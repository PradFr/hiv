package com.example.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import android.os.CancellationSignal
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import com.example.data.local.DuoSpaceDatabase
import com.example.data.local.MessageType
import com.example.data.repository.DuoSpaceRepository
import com.example.ui.CallMode
import com.example.ui.HiveKnowPartnerController
import com.example.ui.HiveNotificationHelper
import com.example.ui.HiveSettingsPersistence
import com.example.ui.TestUserSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class PeerRealtimeSignal(
    val callState: String = "IDLE", // IDLE, RINGING, CONNECTED, ENDED
    val callerSlot: String = "",
    val callMode: String = "VOICE",
    val callSessionId: Long = 0L,
    val isLocalMuted: Boolean = false,
    val isLocalSpeakerOn: Boolean = true,
    val isPartnerVideoOn: Boolean = false,
    val isPartnerFrontCamera: Boolean = true,
    val isPartnerScreenSharing: Boolean = false,
    val partnerScreenShareLabel: String = "Phone Screen",
    val partnerPointerX: Float = 0.5f,
    val partnerPointerY: Float = 0.45f,
    val isBuzzActive: Boolean = false,
    val buzzUntilMs: Long = 0L,
    val requestHearPartner: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val batteryPct: Int = -1,
    val hearConsent: Boolean = true,
    val locConsent: Boolean = true,
    val updatedAtMs: Long = 0L,
    val isFresh: Boolean = false,
    val audioSeq: Long = 0L,
    val audioBase64: String = "",
    val videoSeq: Long = 0L,
    val videoMode: String = "NONE", // NONE, CAM, SCREEN
    val videoBase64: String = ""
)

/**
 * Real-time peer-to-peer signaling, background message notification poller, 8kHz µ-law live audio relay,
 * and live compressed JPEG video/screen-share frame relay over Supabase (`public.spaces`).
 */
object HiveLivePeerRelay {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabaseClient = SupabaseClientManager()

    private var appContext: Context? = null
    private var currentSpaceCode: String = ""
    private var currentMySlot: TestUserSlot = TestUserSlot.USER_1

    private var localCallState: String = "IDLE"
    private var localCallerSlot: String = ""
    private var localCallMode: String = "VOICE"
    private var localCallSessionId: Long = 0L
    private var locallyEndedSessionId: Long = 0L
    private var localMuted: Boolean = false
    private var localSpeakerOn: Boolean = true
    private var localVideoOn: Boolean = false
    private var localFrontCamera: Boolean = true
    private var localScreenSharing: Boolean = false
    private var localScreenShareLabel: String = "Phone Screen"
    private var localPointerX: Float = 0.5f
    private var localPointerY: Float = 0.45f

    private var localBuzzHolding: Boolean = false
    private var localBuzzUntilMs: Long = 0L
    private var localRequestHearPartner: Boolean = false
    private var localLat: Double = 0.0
    private var localLng: Double = 0.0
    private var localAcc: Float = 0f
    private var localHearConsent: Boolean = true
    private var localLocConsent: Boolean = true

    private var outgoingAudioSeq: Long = 0L
    private var latestOutgoingAudioBase64: String = ""
    private var lastPlayedPartnerAudioSeq: Long = -1L
    @Volatile
    var lastRemoteAudioReceivedAtMs: Long = 0L
        private set

    private var outgoingVideoSeq: Long = 0L
    private var outgoingVideoMode: String = "NONE"
    private var latestOutgoingVideoBase64: String = ""
    private var lastDecodedPartnerVideoSeq: Long = -1L

    // Clock-skew-immune partner heartbeat tracking
    private var lastSeenPartnerRemoteTs: Long = -1L
    private var lastPartnerHeartbeatLocalMs: Long = 0L

    // Accumulator buffer for 16kHz -> 8kHz downsampled call audio frames from DiscordCallAudioAndSheet
    private val callAccumulator8k = ShortArray(2400) // 300ms @ 8kHz
    private var callAccumulatorIndex: Int = 0
    private var lastCallDspSubmitMs: Long = 0L

    private val _partnerSignal = MutableStateFlow(PeerRealtimeSignal())
    val partnerSignal: StateFlow<PeerRealtimeSignal> = _partnerSignal.asStateFlow()

    private val _partnerVideoBitmap = MutableStateFlow<Bitmap?>(null)
    val partnerVideoBitmap: StateFlow<Bitmap?> = _partnerVideoBitmap.asStateFlow()

    private val _isPartnerBuzzingMe = MutableStateFlow(false)
    val isPartnerBuzzingMe: StateFlow<Boolean> = _isPartnerBuzzingMe.asStateFlow()

    private var syncJob: Job? = null
    private var endCallResetJob: Job? = null
    private var micStreamJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var isRemoteVibrating: Boolean = false
    private var lastLocationRefreshMs: Long = 0L
    private var lastBgMessageSyncMs: Long = 0L

    // Callback into ViewModel for synchronized call state transitions
    var onRemoteCallSignalChanged: ((PeerRealtimeSignal) -> Unit)? = null

    fun attach(
        context: Context,
        spaceCode: String,
        mySlot: TestUserSlot,
        hearConsent: Boolean = true,
        locConsent: Boolean = true
    ) {
        appContext = context.applicationContext
        val cleanCode = spaceCode.trim().uppercase()
        val changed = cleanCode != currentSpaceCode || mySlot != currentMySlot
        currentSpaceCode = cleanCode
        currentMySlot = mySlot
        localHearConsent = hearConsent
        localLocConsent = locConsent

        if (cleanCode.length >= 4 && (changed || syncJob?.isActive != true)) {
            startSyncLoop()
        }
    }

    fun updateConsents(hearConsent: Boolean, locConsent: Boolean) {
        localHearConsent = hearConsent
        localLocConsent = locConsent
        triggerImmediatePush()
    }

    fun requestImmediateLocationRefresh() {
        refreshLocalCoordinates()
        triggerImmediatePush()
    }

    fun getLocalCoordinates(): Triple<Double, Double, Float> {
        if (localLat == 0.0 && localLng == 0.0) {
            refreshLocalCoordinates()
        }
        return Triple(localLat, localLng, localAcc)
    }

    fun setLocalBuzzActive(holding: Boolean) {
        localBuzzHolding = holding
        localBuzzUntilMs = if (holding) System.currentTimeMillis() + 4500L else 0L
        triggerImmediatePush()
    }

    fun refreshLocalBuzzHold() {
        localBuzzHolding = true
        localBuzzUntilMs = System.currentTimeMillis() + 4500L
        triggerImmediatePush()
    }

    fun setRequestHearPartner(requesting: Boolean) {
        localRequestHearPartner = requesting
        if (!requesting) {
            HiveKnowPartnerController.updateRemoteAudioStats(0.0f, "-∞ dB")
        }
        updateAudioPipelineState()
        triggerImmediatePush()
    }

    fun setCallVideoState(isVideoOn: Boolean, isFrontCamera: Boolean) {
        localVideoOn = isVideoOn
        localFrontCamera = isFrontCamera
        if (isVideoOn) {
            localCallMode = CallMode.VIDEO.name
        } else if (!localScreenSharing) {
            localCallMode = CallMode.VOICE.name
            if (outgoingVideoMode == "CAM") {
                outgoingVideoMode = "NONE"
                latestOutgoingVideoBase64 = ""
            }
        }
        triggerImmediatePush()
    }

    fun setCallScreenShareState(
        isScreenSharing: Boolean,
        label: String = "Phone Screen",
        pointerX: Float = localPointerX,
        pointerY: Float = localPointerY
    ) {
        localScreenSharing = isScreenSharing
        localScreenShareLabel = label
        localPointerX = pointerX
        localPointerY = pointerY
        if (isScreenSharing) {
            localCallMode = CallMode.VIDEO.name
        } else if (!localVideoOn) {
            localCallMode = CallMode.VOICE.name
            if (outgoingVideoMode == "SCREEN") {
                outgoingVideoMode = "NONE"
                latestOutgoingVideoBase64 = ""
            }
        }
        triggerImmediatePush()
    }

    fun updateScreenSharePointer(pointerX: Float, pointerY: Float) {
        localPointerX = pointerX.coerceIn(0.05f, 0.95f)
        localPointerY = pointerY.coerceIn(0.05f, 0.95f)
    }

    private var lastVideoFrameSubmitMs: Long = 0L

    /**
     * Compresses a live Camera or Screen Share Bitmap into a compact JPEG Base64 packet
     * and pushes it to the partner's device in real time.
     */
    fun submitLocalVideoBitmap(bitmap: Bitmap, mode: String) {
        if (localCallState != "CONNECTED" && localCallState != "RINGING" && !localVideoOn && !localScreenSharing) return
        val now = System.currentTimeMillis()
        if (now - lastVideoFrameSubmitMs < 280L) return
        lastVideoFrameSubmitMs = now
        scope.launch {
            runCatching {
                val targetW = if (mode == "SCREEN") 260 else 200
                val ratio = bitmap.height.toFloat() / bitmap.width.coerceAtLeast(1).toFloat()
                val targetH = (targetW * ratio).toInt().coerceIn(140, 480)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                val baos = ByteArrayOutputStream()
                val quality = if (mode == "SCREEN") 44 else 42
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val bytes = baos.toByteArray()
                if (bytes.isNotEmpty()) {
                    outgoingVideoSeq += 1L
                    outgoingVideoMode = mode
                    latestOutgoingVideoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    pushLocalSignalSnapshot()
                }
            }
        }
    }

    fun publishCallStart(
        mode: CallMode,
        callerSlot: TestUserSlot,
        sessionId: Long = System.currentTimeMillis()
    ) {
        endCallResetJob?.cancel()
        localCallState = "RINGING"
        localCallerSlot = callerSlot.name
        localCallMode = mode.name
        localCallSessionId = sessionId
        localVideoOn = mode == CallMode.VIDEO
        localScreenSharing = false
        updateAudioPipelineState()
        scope.launch {
            pushLocalSignalSnapshot()
            delay(300L)
            if (localCallState == "RINGING" && localCallSessionId == sessionId) {
                pushLocalSignalSnapshot()
            }
        }
    }

    fun acknowledgeIncomingCallRinging(
        mode: CallMode,
        callerSlot: String,
        sessionId: Long
    ) {
        if (sessionId <= 0L || sessionId == locallyEndedSessionId) return
        if (localCallState == "CONNECTED" && localCallSessionId == sessionId) return
        endCallResetJob?.cancel()
        localCallState = "RINGING"
        localCallerSlot = callerSlot
        localCallMode = mode.name
        localCallSessionId = sessionId
        triggerImmediatePush()
    }

    fun publishCallAnswered(mode: CallMode, callerSlot: String, sessionId: Long) {
        endCallResetJob?.cancel()
        val resolvedSessionId = if (sessionId > 0L) sessionId else localCallSessionId.takeIf { it > 0L } ?: System.currentTimeMillis()
        localCallState = "CONNECTED"
        localCallerSlot = callerSlot.ifBlank { currentMySlot.name }
        localCallMode = mode.name
        localCallSessionId = resolvedSessionId
        localVideoOn = mode == CallMode.VIDEO
        appContext?.let { HiveNotificationHelper.cancelIncomingCallNotification(it) }
        updateAudioPipelineState()
        scope.launch {
            pushLocalSignalSnapshot()
            delay(300L)
            if (localCallState == "CONNECTED" && localCallSessionId == resolvedSessionId) {
                pushLocalSignalSnapshot()
            }
        }
    }

    fun publishCallConnected(mode: CallMode, callerSlot: String, sessionId: Long) {
        endCallResetJob?.cancel()
        val resolvedSessionId = if (sessionId > 0L) sessionId else localCallSessionId
        localCallState = "CONNECTED"
        localCallerSlot = callerSlot.ifBlank { currentMySlot.name }
        localCallMode = mode.name
        localCallSessionId = resolvedSessionId
        appContext?.let { HiveNotificationHelper.cancelIncomingCallNotification(it) }
        updateAudioPipelineState()
        triggerImmediatePush()
    }

    fun publishCallEnded(sessionId: Long = localCallSessionId) {
        val endedSession = if (sessionId > 0L) sessionId else localCallSessionId
        if (endedSession > 0L) {
            locallyEndedSessionId = endedSession
        }
        localCallState = "ENDED"
        localCallSessionId = endedSession
        localVideoOn = false
        localScreenSharing = false
        outgoingVideoMode = "NONE"
        latestOutgoingVideoBase64 = ""
        latestOutgoingAudioBase64 = ""
        callAccumulatorIndex = 0
        _partnerVideoBitmap.value = null
        appContext?.let { HiveNotificationHelper.cancelIncomingCallNotification(it) }
        updateAudioPipelineState()

        endCallResetJob?.cancel()
        endCallResetJob = scope.launch {
            pushLocalSignalSnapshot()
            delay(350L)
            pushLocalSignalSnapshot()
            delay(4500L)
            if (localCallState == "ENDED" && localCallSessionId == endedSession) {
                localCallState = "IDLE"
                pushLocalSignalSnapshot()
            }
        }
    }

    fun declineOrEndCallFromNotification(sessionId: Long) {
        publishCallEnded(sessionId)
        val endedSignal = _partnerSignal.value.copy(
            callState = "ENDED",
            callSessionId = if (sessionId > 0L) sessionId else localCallSessionId,
            isFresh = true,
            updatedAtMs = System.currentTimeMillis()
        )
        onRemoteCallSignalChanged?.invoke(endedSignal)
    }

    fun setCallMuted(muted: Boolean) {
        localMuted = muted
    }

    fun setCallSpeakerOn(speakerOn: Boolean) {
        localSpeakerOn = speakerOn
    }

    /**
     * Called directly by `rememberRealCallVoiceDspEngine` in `DiscordCallAudioAndSheet.kt`
     * with 16kHz DSP-processed voice frames so we never open two competing `AudioRecord` instances
     * during an active call.
     */
    fun submitCallDspPcm16(pcm16k: ShortArray, readCount: Int) {
        if (localMuted || localCallState != "CONNECTED") return
        lastCallDspSubmitMs = System.currentTimeMillis()
        var i = 0
        while (i < readCount) {
            callAccumulator8k[callAccumulatorIndex++] = pcm16k[i]
            if (callAccumulatorIndex >= callAccumulator8k.size) {
                val b64 = encodePcm16ToUlawBase64(callAccumulator8k, callAccumulator8k.size)
                callAccumulatorIndex = 0
                outgoingAudioSeq += 1L
                latestOutgoingAudioBase64 = b64
                triggerImmediatePush()
            }
            i += 2 // 16kHz -> 8kHz decimation
        }
    }

    suspend fun fetchFreshPartnerSignal(): PeerRealtimeSignal = withContext(Dispatchers.IO) {
        pullPartnerSignalSnapshot()
        _partnerSignal.value
    }

    private fun triggerImmediatePush() {
        scope.launch {
            pushLocalSignalSnapshot()
        }
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                val ctx = appContext
                if (ctx != null && currentSpaceCode.length < 4) {
                    val cfg = HiveSettingsPersistence.loadConfig(ctx)
                    if (cfg.isSpacePaired && cfg.activeSpaceCode.length >= 4) {
                        currentSpaceCode = cfg.activeSpaceCode.trim().uppercase()
                        currentMySlot = cfg.localUserSlot
                    }
                }
                val code = currentSpaceCode
                if (code.length >= 4 && supabaseClient.isConfigured) {
                    val now = System.currentTimeMillis()
                    if (now - lastLocationRefreshMs > 10_000L) {
                        lastLocationRefreshMs = now
                        refreshLocalCoordinates()
                    }
                    pushLocalSignalSnapshot()
                    pullPartnerSignalSnapshot()

                    if (ctx != null && (now - lastBgMessageSyncMs) >= 2200L) {
                        lastBgMessageSyncMs = now
                        syncBackgroundMessagesAndNotifications(ctx, code)
                    }
                }
                val nowAfter = System.currentTimeMillis()
                val fastMode =
                    localCallState == "RINGING" ||
                        localCallState == "CONNECTED" ||
                        localVideoOn ||
                        localScreenSharing ||
                        localRequestHearPartner ||
                        localBuzzHolding ||
                        localBuzzUntilMs > nowAfter ||
                        _partnerSignal.value.callState == "RINGING" ||
                        _partnerSignal.value.callState == "CONNECTED" ||
                        _partnerSignal.value.isPartnerVideoOn ||
                        _partnerSignal.value.isPartnerScreenSharing ||
                        _partnerSignal.value.requestHearPartner ||
                        _partnerSignal.value.isBuzzActive

                delay(if (fastMode) 320L else 1000L)
            }
        }
    }

    private suspend fun syncBackgroundMessagesAndNotifications(context: Context, spaceCode: String) {
        runCatching {
            val db = DuoSpaceDatabase.getInstance(context)
            val dao = db.duoSpaceDao()
            val repo = DuoSpaceRepository(dao, supabaseClient)
            val myUserId = currentMySlot.userId
            repo.syncSpaceFromSupabase(spaceCode = spaceCode, activeUserId = myUserId)

            val allMessages = dao.getMessagesForSpaceList(spaceCode)
            val unreadIncoming = allMessages.filter {
                !it.isRead &&
                    !it.isSystemEvent &&
                    it.messageType != MessageType.DOODLE &&
                    it.senderId != myUserId
            }
            if (unreadIncoming.isNotEmpty()) {
                val latestId = unreadIncoming.maxByOrNull { it.createdAt }?.id.orEmpty()
                HiveNotificationHelper.showPrivateNotification(
                    context = context,
                    notificationCount = unreadIncoming.size,
                    latestMessageId = latestId
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshLocalCoordinates() {
        val ctx = appContext ?: return
        try {
            if (localLocConsent) {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                var bestLoc: Location? = null
                for (p in providers) {
                    val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
                    if (bestLoc == null || loc.time > bestLoc.time) {
                        bestLoc = loc
                    }
                }
                if (bestLoc != null) {
                    localLat = bestLoc.latitude
                    localLng = bestLoc.longitude
                    localAcc = bestLoc.accuracy
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val activeProvider = providers.firstOrNull {
                        runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
                    }
                    if (activeProvider != null) {
                        runCatching {
                            lm.getCurrentLocation(
                                activeProvider,
                                CancellationSignal(),
                                ctx.mainExecutor
                            ) { fresh ->
                                if (fresh != null) {
                                    localLat = fresh.latitude
                                    localLng = fresh.longitude
                                    localAcc = fresh.accuracy
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun getLocalBatteryPct(): Int {
        val ctx = appContext ?: return -1
        return try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (pct in 1..100) pct else {
                val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) (level * 100) / scale else -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    private suspend fun pushLocalSignalSnapshot() {
        val code = currentSpaceCode
        val rest = supabaseClient.restService
        if (code.length < 4 || !supabaseClient.isConfigured || rest == null) return
        val rowKey = "${code}__${currentMySlot.name}"
        val now = System.currentTimeMillis()
        val bat = getLocalBatteryPct()
        val buzzActiveNow = localBuzzHolding && localBuzzUntilMs > now
        if (!buzzActiveNow && localBuzzHolding) {
            localBuzzHolding = false
        }

        val cleanShareLabel = localScreenShareLabel.replace("|", "").replace(",", "").ifBlank { "Phone Screen" }
        val topicPayload = buildString {
            append("CALL:").append(localCallState).append(":")
                .append(localCallerSlot).append(":")
                .append(localCallMode).append(":")
                .append(localCallSessionId)
            append("|VID_STATE:")
                .append(if (localVideoOn) "1" else "0").append(",")
                .append(if (localFrontCamera) "1" else "0").append(",")
                .append(if (localScreenSharing) "1" else "0").append(",")
                .append(cleanShareLabel).append(",")
                .append(localPointerX).append(",")
                .append(localPointerY)
            append("|BUZZ:").append(if (buzzActiveNow) localBuzzUntilMs else 0L)
            append("|BUZZ_ACT:").append(if (buzzActiveNow) "1" else "0")
            append("|HEAR_REQ:").append(if (localRequestHearPartner) "1" else "0")
            append("|LOC:").append(localLat).append(",").append(localLng).append(",").append(localAcc)
            append("|BAT:").append(bat)
            append("|CONSENT:").append(if (localHearConsent) "1" else "0")
                .append(",").append(if (localLocConsent) "1" else "0")
            append("|TS:").append(now)
        }

        val audioPreview = if (latestOutgoingAudioBase64.isNotBlank()) {
            "AUD:$outgoingAudioSeq:$latestOutgoingAudioBase64"
        } else {
            "AUD:0:"
        }

        val videoPayload = if ((localVideoOn || localScreenSharing) && latestOutgoingVideoBase64.isNotBlank()) {
            "VID:$outgoingVideoSeq:$outgoingVideoMode:$latestOutgoingVideoBase64"
        } else {
            ""
        }

        runCatching {
            rest.upsertSpace(
                SupabaseSpaceDto(
                    code = rowKey,
                    topic = topicPayload,
                    creatorId = currentMySlot.userId,
                    creatorName = currentMySlot.name,
                    partnerId = null,
                    partnerName = videoPayload,
                    createdAtMs = now,
                    lastMessagePreview = audioPreview,
                    lastUpdatedAtMs = now
                )
            )
        }
    }

    private suspend fun pullPartnerSignalSnapshot() {
        val code = currentSpaceCode
        val rest = supabaseClient.restService
        if (code.length < 4 || !supabaseClient.isConfigured || rest == null) return
        val partnerSlot = if (currentMySlot == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
        val partnerRowKey = "${code}__${partnerSlot.name}"

        val dto = runCatching {
            rest.getSpaceByCode(codeFilter = "eq.$partnerRowKey").firstOrNull()
        }.getOrNull() ?: return

        val now = System.currentTimeMillis()
        val rawParsed = parseSignalFromDto(dto)

        // Clock-skew-immune freshness check:
        // Whenever the partner's row timestamp changes, record our local clock time.
        if (rawParsed.updatedAtMs != lastSeenPartnerRemoteTs) {
            if (lastSeenPartnerRemoteTs != -1L || abs(now - rawParsed.updatedAtMs) < 180_000L) {
                lastPartnerHeartbeatLocalMs = now
            }
            lastSeenPartnerRemoteTs = rawParsed.updatedAtMs
        }
        val partnerRowFresh =
            (now - lastPartnerHeartbeatLocalMs) < 22_000L ||
                abs(now - rawParsed.updatedAtMs) < 18_000L

        val parsed = rawParsed.copy(isFresh = partnerRowFresh)
        _partnerSignal.value = parsed

        // Decode partner video / screen share frame if active
        val hasIncomingVisualStream =
            parsed.isPartnerVideoOn ||
                parsed.isPartnerScreenSharing ||
                parsed.videoMode == "CAM" ||
                parsed.videoMode == "SCREEN"
        if (partnerRowFresh && hasIncomingVisualStream && parsed.videoBase64.isNotBlank()) {
            if (parsed.videoSeq != lastDecodedPartnerVideoSeq || _partnerVideoBitmap.value == null) {
                lastDecodedPartnerVideoSeq = parsed.videoSeq
                runCatching {
                    val bytes = Base64.decode(parsed.videoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        _partnerVideoBitmap.value = bmp
                    }
                }
            }
        } else if (!hasIncomingVisualStream) {
            if (_partnerVideoBitmap.value != null) {
                _partnerVideoBitmap.value = null
            }
        }

        // 1. Check remote buzz -> vibrate MY phone if partner is holding their buzz button!
        val shouldBuzzMe = partnerRowFresh && parsed.isBuzzActive
        if (shouldBuzzMe != _isPartnerBuzzingMe.value) {
            _isPartnerBuzzingMe.value = shouldBuzzMe
        }
        handleIncomingPartnerBuzz(shouldBuzzMe)

        // 2. Handle Incoming Call system notification & full-screen wake even when on Home Screen
        val ctx = appContext
        if (ctx != null) {
            val isIncomingRingingForMe =
                partnerRowFresh &&
                    parsed.callState == "RINGING" &&
                    parsed.callerSlot.isNotBlank() &&
                    parsed.callerSlot != currentMySlot.name &&
                    parsed.callSessionId > 0L &&
                    parsed.callSessionId != locallyEndedSessionId &&
                    !(localCallState == "CONNECTED" && localCallSessionId == parsed.callSessionId)

            if (isIncomingRingingForMe) {
                val cfg = HiveSettingsPersistence.loadConfig(ctx)
                val callerName = if (currentMySlot == TestUserSlot.USER_1) {
                    cfg.user2Name.ifBlank { "Partner" }
                } else {
                    cfg.user1Name.ifBlank { "Partner" }
                }
                HiveNotificationHelper.showIncomingCallNotification(
                    context = ctx,
                    callerName = callerName,
                    callMode = parsed.callMode,
                    callSessionId = parsed.callSessionId,
                    callerSlot = parsed.callerSlot
                )
            } else if (parsed.callState == "ENDED" || parsed.callState == "IDLE" || localCallState == "CONNECTED") {
                HiveNotificationHelper.cancelIncomingCallNotification(ctx)
            }
        }

        // 3. Notify ViewModel of call state changes from partner
        if (partnerRowFresh || parsed.callState == "ENDED") {
            onRemoteCallSignalChanged?.invoke(parsed)
        }

        // 4. Update microphone capture if partner is requesting "Hear Partner" or call is connected
        updateAudioPipelineState()

        // 5. Play incoming audio chunk if we are in a CONNECTED call or actively hearing partner
        val shouldPlayAudio =
            (localCallState == "CONNECTED" || (partnerRowFresh && parsed.callState == "CONNECTED") || localRequestHearPartner) &&
                parsed.audioSeq > lastPlayedPartnerAudioSeq &&
                parsed.audioBase64.isNotBlank() &&
                partnerRowFresh

        if (shouldPlayAudio) {
            lastPlayedPartnerAudioSeq = parsed.audioSeq
            lastRemoteAudioReceivedAtMs = now
            playIncomingUlawChunk(parsed.audioBase64)
        }
    }

    private fun parseSignalFromDto(dto: SupabaseSpaceDto): PeerRealtimeSignal {
        var callState = "IDLE"
        var callerSlot = ""
        var callMode = "VOICE"
        var callSessionId = 0L
        var isPartnerVideoOn = false
        var isPartnerFrontCam = true
        var isPartnerScreenSharing = false
        var partnerShareLabel = "Phone Screen"
        var partnerPointerX = 0.5f
        var partnerPointerY = 0.45f
        var buzzUntilMs = 0L
        var buzzActFlag: Boolean? = null
        var requestHear = false
        var lat = 0.0
        var lng = 0.0
        var acc = 0f
        var bat = -1
        var hearConsent = true
        var locConsent = true
        var ts = dto.lastUpdatedAtMs

        val segments = dto.topic.split("|")
        for (seg in segments) {
            when {
                seg.startsWith("CALL:") -> {
                    val p = seg.removePrefix("CALL:").split(":")
                    callState = p.getOrNull(0) ?: "IDLE"
                    callerSlot = p.getOrNull(1) ?: ""
                    callMode = p.getOrNull(2) ?: "VOICE"
                    callSessionId = p.getOrNull(3)?.toLongOrNull() ?: 0L
                }
                seg.startsWith("VID_STATE:") -> {
                    val p = seg.removePrefix("VID_STATE:").split(",")
                    isPartnerVideoOn = (p.getOrNull(0) ?: "0") == "1"
                    isPartnerFrontCam = (p.getOrNull(1) ?: "1") == "1"
                    isPartnerScreenSharing = (p.getOrNull(2) ?: "0") == "1"
                    partnerShareLabel = p.getOrNull(3)?.ifBlank { "Phone Screen" } ?: "Phone Screen"
                    partnerPointerX = p.getOrNull(4)?.toFloatOrNull() ?: 0.5f
                    partnerPointerY = p.getOrNull(5)?.toFloatOrNull() ?: 0.45f
                }
                seg.startsWith("BUZZ:") -> {
                    buzzUntilMs = seg.removePrefix("BUZZ:").toLongOrNull() ?: 0L
                }
                seg.startsWith("BUZZ_ACT:") -> {
                    buzzActFlag = seg.removePrefix("BUZZ_ACT:") == "1"
                }
                seg.startsWith("HEAR_REQ:") -> {
                    requestHear = seg.removePrefix("HEAR_REQ:") == "1"
                }
                seg.startsWith("LOC:") -> {
                    val p = seg.removePrefix("LOC:").split(",")
                    lat = p.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    lng = p.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                    acc = p.getOrNull(2)?.toFloatOrNull() ?: 0f
                }
                seg.startsWith("BAT:") -> {
                    bat = seg.removePrefix("BAT:").toIntOrNull() ?: -1
                }
                seg.startsWith("CONSENT:") -> {
                    val p = seg.removePrefix("CONSENT:").split(",")
                    hearConsent = (p.getOrNull(0) ?: "1") == "1"
                    locConsent = (p.getOrNull(1) ?: "1") == "1"
                }
                seg.startsWith("TS:") -> {
                    ts = seg.removePrefix("TS:").toLongOrNull() ?: dto.lastUpdatedAtMs
                }
            }
        }

        val isBuzzActive = buzzActFlag ?: (buzzUntilMs > ts)

        var audioSeq = 0L
        var audioB64 = ""
        val preview = dto.lastMessagePreview.orEmpty()
        if (preview.startsWith("AUD:")) {
            val p = preview.removePrefix("AUD:").split(":", limit = 2)
            audioSeq = p.getOrNull(0)?.toLongOrNull() ?: 0L
            audioB64 = p.getOrNull(1) ?: ""
        }

        var videoSeq = 0L
        var videoMode = "NONE"
        var videoB64 = ""
        val rawPartnerCol = dto.partnerName.orEmpty()
        if (rawPartnerCol.startsWith("VID:")) {
            val p = rawPartnerCol.removePrefix("VID:").split(":", limit = 3)
            videoSeq = p.getOrNull(0)?.toLongOrNull() ?: 0L
            videoMode = p.getOrNull(1) ?: "NONE"
            videoB64 = p.getOrNull(2) ?: ""
        }

        return PeerRealtimeSignal(
            callState = callState,
            callerSlot = callerSlot,
            callMode = callMode,
            callSessionId = callSessionId,
            isPartnerVideoOn = isPartnerVideoOn,
            isPartnerFrontCamera = isPartnerFrontCam,
            isPartnerScreenSharing = isPartnerScreenSharing,
            partnerScreenShareLabel = partnerShareLabel,
            partnerPointerX = partnerPointerX,
            partnerPointerY = partnerPointerY,
            isBuzzActive = isBuzzActive,
            buzzUntilMs = buzzUntilMs,
            requestHearPartner = requestHear,
            lat = lat,
            lng = lng,
            accuracyMeters = acc,
            batteryPct = bat,
            hearConsent = hearConsent,
            locConsent = locConsent,
            updatedAtMs = ts,
            audioSeq = audioSeq,
            audioBase64 = audioB64,
            videoSeq = videoSeq,
            videoMode = videoMode,
            videoBase64 = videoB64
        )
    }

    @SuppressLint("MissingPermission")
    private fun handleIncomingPartnerBuzz(shouldVibrate: Boolean) {
        val ctx = appContext ?: return
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (shouldVibrate && !isRemoteVibrating) {
                isRemoteVibrating = true
                val pattern = longArrayOf(0L, 180L, 60L, 220L, 60L, 260L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } else if (!shouldVibrate && isRemoteVibrating) {
                isRemoteVibrating = false
                vibrator?.cancel()
            }
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateAudioPipelineState() {
        val partner = _partnerSignal.value
        val now = System.currentTimeMillis()
        val partnerSignalRecent = partner.isFresh

        val inConnectedCall = localCallState == "CONNECTED" || (partnerSignalRecent && partner.callState == "CONNECTED")
        val callDspActive = (now - lastCallDspSubmitMs) < 2_500L
        val partnerListeningToMe = partnerSignalRecent && partner.requestHearPartner && localHearConsent

        // Use fallback background AudioRecord when partner uses "Hear [Partner]" (or if call DSP loop isn't active)
        val shouldCaptureBackgroundMic = partnerListeningToMe || (inConnectedCall && !localMuted && !callDspActive)

        if (shouldCaptureBackgroundMic && micStreamJob?.isActive != true) {
            micStreamJob = scope.launch {
                val sampleRate = 8000
                val chunkSamples = 2400 // 300ms of 8kHz audio per packet
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(chunkSamples * 2)

                var recorder: AudioRecord? = null
                try {
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                    )
                    if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                        recorder.startRecording()
                        val pcmBuf = ShortArray(chunkSamples)
                        while (isActive) {
                            if ((System.currentTimeMillis() - lastCallDspSubmitMs) < 1500L) {
                                // Yield mic to foreground call DSP engine
                                break
                            }
                            val read = recorder.read(pcmBuf, 0, pcmBuf.size)
                            if (read > 0 && (!inConnectedCall || !localMuted)) {
                                val b64 = encodePcm16ToUlawBase64(pcmBuf, read)
                                outgoingAudioSeq += 1L
                                latestOutgoingAudioBase64 = b64
                                pushLocalSignalSnapshot()
                            } else {
                                delay(100L)
                            }
                            delay(50L)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { recorder?.stop() }
                    runCatching { recorder?.release() }
                }
            }
        } else if (!shouldCaptureBackgroundMic && micStreamJob?.isActive == true) {
            micStreamJob?.cancel()
            micStreamJob = null
        }
    }

    private fun ensureAudioTrack(): AudioTrack? {
        val existing = audioTrack
        if (existing != null && existing.state == AudioTrack.STATE_INITIALIZED) {
            return existing
        }
        return try {
            val sampleRate = 8000
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(8000)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            audioTrack = track
            track
        } catch (_: Exception) {
            null
        }
    }

    private fun playIncomingUlawChunk(base64Ulaw: String) {
        try {
            val pcm = decodeUlawBase64ToPcm16(base64Ulaw)
            if (pcm.isEmpty()) return

            var sumSq = 0.0
            for (s in pcm) {
                sumSq += (s.toDouble() * s.toDouble())
            }
            val rms = sqrt(sumSq / pcm.size.coerceAtLeast(1))
            val normalized = (rms / 7500.0).toFloat().coerceIn(0.05f, 1.0f)
            val db = if (rms > 1.0) (20.0 * log10(rms / 32768.0)).toInt().coerceIn(-65, 0) else -58

            if (localRequestHearPartner) {
                HiveKnowPartnerController.updateRemoteAudioStats(normalized, "$db dB • Partner Mic Live")
            }

            val track = ensureAudioTrack() ?: return
            track.write(pcm, 0, pcm.size)
        } catch (_: Exception) {
        }
    }

    private fun encodePcm16ToUlawBase64(pcm: ShortArray, length: Int): String {
        val out = ByteArray(length)
        for (i in 0 until length) {
            out[i] = linearToUlaw(pcm[i].toInt())
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decodeUlawBase64ToPcm16(b64: String): ShortArray {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val out = ShortArray(bytes.size)
        for (i in bytes.indices) {
            out[i] = ulawToLinear(bytes[i])
        }
        return out
    }

    private fun linearToUlaw(sampleIn: Int): Byte {
        var sample = sampleIn
        val sign = (sample shr 8) and 0x80
        if (sign != 0) sample = -sample
        if (sample > 32635) sample = 32635
        sample += 0x84
        var exponent = 7
        var expMask = 0x4000
        while ((sample and expMask) == 0 && exponent > 0) {
            exponent--
            expMask = expMask shr 1
        }
        val mantissa = (sample shr (exponent + 3)) and 0x0F
        val ulawByte = (sign or (exponent shl 4) or mantissa).inv()
        return (ulawByte and 0xFF).toByte()
    }

    private fun ulawToLinear(ulawByteIn: Byte): Short {
        val ulaw = ulawByteIn.toInt().inv() and 0xFF
        val sign = ulaw and 0x80
        val exponent = (ulaw shr 4) and 0x07
        val mantissa = ulaw and 0x0F
        var sample = ((mantissa shl 3) + 0x84) shl exponent
        sample -= 0x84
        return (if (sign != 0) -sample else sample).coerceIn(-32768, 32767).toShort()
    }
}
