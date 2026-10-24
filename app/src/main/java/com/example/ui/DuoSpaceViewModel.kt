package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DuoSpaceDatabase
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.SpaceEntity
import com.example.data.repository.DuoSpaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class TestUserSlot(val userId: String, val slotNumber: Int) {
    USER_1(DuoSpaceRepository.USER_1_ID, 1),
    USER_2(DuoSpaceRepository.USER_2_ID, 2)
}

enum class CallMode {
    VOICE,
    VIDEO
}

data class ActiveCallSession(
    val mode: CallMode,
    val callerSlot: TestUserSlot,
    val callSessionId: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isScreenSharing: Boolean = false,
    val screenShareLabel: String = "Phone Screen",
    val isRecording: Boolean = false,
    val wasEverRecorded: Boolean = false,
    val startedAtMs: Long = System.currentTimeMillis()
)

data class DuoUiState(
    val activeUser: TestUserSlot = TestUserSlot.USER_1,
    val activeSpaceCode: String = DuoSpaceRepository.DEFAULT_SPACE_CODE,
    val messageInput: String = "",
    val replyingTo: MessageEntity? = null,
    val isDoodleModeActive: Boolean = false,
    val isStickerTrayOpen: Boolean = false,
    val isAttachmentTrayOpen: Boolean = false,
    val isCameraStudioOpen: Boolean = false,
    val activeCall: ActiveCallSession? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DuoSpaceViewModel(
    private val repository: DuoSpaceRepository,
    initialSpaceCode: String = DuoSpaceRepository.DEFAULT_SPACE_CODE,
    initialUserSlot: TestUserSlot = TestUserSlot.USER_1,
    initialUserName: String = "You"
) : ViewModel() {

    private val _activeSpaceCode = MutableStateFlow(
        initialSpaceCode.trim().uppercase().ifBlank { DuoSpaceRepository.DEFAULT_SPACE_CODE }
    )

    private val _uiState = MutableStateFlow(
        DuoUiState(
            activeUser = initialUserSlot,
            activeSpaceCode = _activeSpaceCode.value
        )
    )
    val uiState: StateFlow<DuoUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    val spaceState: StateFlow<SpaceEntity?> = _activeSpaceCode
        .flatMapLatest { code -> repository.observeSpace(code) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val messagesState: StateFlow<List<MessageEntity>> = _activeSpaceCode
        .flatMapLatest { code -> repository.observeMessages(code) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private var lastHandledRemoteCallSessionId: Long = 0L
    private var locallyEndedCallSessionId: Long = 0L

    init {
        com.example.data.remote.HiveLivePeerRelay.onRemoteCallSignalChanged = { signal ->
            handleRemoteCallSignal(signal)
        }
        HiveNotificationHelper.onCallNotificationAction = { action, sessionId ->
            when (action) {
                HiveNotificationHelper.ACTION_ANSWER_CALL -> {
                    val cur = _uiState.value.activeCall
                    if (cur != null && !cur.isConnected) {
                        answerCall()
                    }
                }
                HiveNotificationHelper.ACTION_DECLINE_CALL -> {
                    if (sessionId > 0L) {
                        locallyEndedCallSessionId = sessionId
                    }
                    _uiState.update { it.copy(activeCall = null) }
                }
            }
        }
        viewModelScope.launch {
            val code = _activeSpaceCode.value
            val creator = if (initialUserSlot == TestUserSlot.USER_1) initialUserName else "Partner"
            val partner = if (initialUserSlot == TestUserSlot.USER_2) initialUserName else null
            repository.ensureSpaceInitialized(
                spaceCode = code,
                creatorName = creator,
                partnerName = partner
            )
        }
        startBackgroundSync(_activeSpaceCode.value)
    }

    fun handleIncomingCallIntent(
        action: String,
        sessionId: Long,
        callerSlotRaw: String,
        callModeRaw: String
    ) {
        if (sessionId <= 0L || sessionId == locallyEndedCallSessionId) return
        val callerSlot = if (callerSlotRaw == TestUserSlot.USER_2.name) {
            TestUserSlot.USER_2
        } else if (callerSlotRaw == TestUserSlot.USER_1.name) {
            TestUserSlot.USER_1
        } else {
            if (_uiState.value.activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
        }
        val mode = if (callModeRaw.equals(CallMode.VIDEO.name, ignoreCase = true)) {
            CallMode.VIDEO
        } else {
            CallMode.VOICE
        }
        val autoRec = spaceState.value?.autoRecordCalls == true
        lastHandledRemoteCallSessionId = sessionId

        if (_uiState.value.activeCall == null || _uiState.value.activeCall?.callSessionId != sessionId) {
            _uiState.update {
                it.copy(
                    activeCall = ActiveCallSession(
                        mode = mode,
                        callerSlot = callerSlot,
                        callSessionId = sessionId,
                        isConnected = false,
                        isVideoEnabled = mode == CallMode.VIDEO,
                        isFrontCamera = true,
                        isRecording = autoRec,
                        wasEverRecorded = autoRec,
                        startedAtMs = System.currentTimeMillis()
                    ),
                    isStickerTrayOpen = false,
                    isDoodleModeActive = false
                )
            }
        }

        if (action == HiveNotificationHelper.ACTION_ANSWER_CALL) {
            answerCall()
        }
    }

    private fun handleRemoteCallSignal(signal: com.example.data.remote.PeerRealtimeSignal) {
        val now = System.currentTimeMillis()
        val currentCall = _uiState.value.activeCall
        val callerSlot = if (signal.callerSlot == TestUserSlot.USER_2.name) {
            TestUserSlot.USER_2
        } else {
            TestUserSlot.USER_1
        }
        val mode = if (signal.callMode == CallMode.VIDEO.name) CallMode.VIDEO else CallMode.VOICE

        when (signal.callState) {
            "RINGING" -> {
                if (!signal.isFresh || signal.callSessionId <= 0L) return
                if (signal.callSessionId == locallyEndedCallSessionId) return
                // Only open an incoming ringing call when the OTHER user is the caller
                if (callerSlot == _uiState.value.activeUser) return

                if (currentCall == null || (currentCall.callSessionId != signal.callSessionId && !currentCall.isConnected)) {
                    lastHandledRemoteCallSessionId = signal.callSessionId
                    val autoRec = spaceState.value?.autoRecordCalls == true
                    _uiState.update {
                        it.copy(
                            activeCall = ActiveCallSession(
                                mode = mode,
                                callerSlot = callerSlot,
                                callSessionId = signal.callSessionId,
                                isConnected = false,
                                isVideoEnabled = mode == CallMode.VIDEO,
                                isFrontCamera = true,
                                isRecording = autoRec,
                                wasEverRecorded = autoRec,
                                startedAtMs = now
                            ),
                            isStickerTrayOpen = false,
                            isDoodleModeActive = false
                        )
                    }
                    com.example.data.remote.HiveLivePeerRelay.acknowledgeIncomingCallRinging(
                        mode = mode,
                        callerSlot = callerSlot.name,
                        sessionId = signal.callSessionId
                    )
                }
            }
            "CONNECTED" -> {
                if (!signal.isFresh || signal.callSessionId <= 0L) return
                if (signal.callSessionId == locallyEndedCallSessionId) return

                if (currentCall == null) {
                    lastHandledRemoteCallSessionId = signal.callSessionId
                    val autoRec = spaceState.value?.autoRecordCalls == true
                    _uiState.update {
                        it.copy(
                            activeCall = ActiveCallSession(
                                mode = mode,
                                callerSlot = callerSlot,
                                callSessionId = signal.callSessionId,
                                isConnected = true,
                                isVideoEnabled = mode == CallMode.VIDEO,
                                isFrontCamera = true,
                                isRecording = autoRec,
                                wasEverRecorded = autoRec,
                                startedAtMs = now
                            ),
                            isStickerTrayOpen = false,
                            isDoodleModeActive = false
                        )
                    }
                    com.example.data.remote.HiveLivePeerRelay.publishCallConnected(
                        mode = mode,
                        callerSlot = callerSlot.name,
                        sessionId = signal.callSessionId
                    )
                } else if (!currentCall.isConnected && currentCall.callSessionId == signal.callSessionId) {
                    _uiState.update { state ->
                        val cur = state.activeCall ?: return@update state
                        state.copy(
                            activeCall = cur.copy(
                                isConnected = true,
                                startedAtMs = now
                            )
                        )
                    }
                    com.example.data.remote.HiveLivePeerRelay.publishCallConnected(
                        mode = currentCall.mode,
                        callerSlot = currentCall.callerSlot.name,
                        sessionId = signal.callSessionId
                    )
                }
            }
            "ENDED" -> {
                // Strictly verify that ENDED matches the active call's callSessionId so stale ENDED states
                // from previous calls can NEVER cut a newly started call!
                if (currentCall != null &&
                    signal.callSessionId > 0L &&
                    signal.callSessionId == currentCall.callSessionId
                ) {
                    locallyEndedCallSessionId = currentCall.callSessionId
                    _uiState.update { it.copy(activeCall = null) }
                    com.example.data.remote.HiveLivePeerRelay.publishCallEnded(currentCall.callSessionId)
                }
            }
        }
    }

    private fun startBackgroundSync(spaceCode: String) {
        repository.startRealtimeForSpace(viewModelScope, spaceCode) {
            viewModelScope.launch {
                repository.syncSpaceFromSupabase(
                    spaceCode = _activeSpaceCode.value,
                    activeUserId = _uiState.value.activeUser.userId
                )
            }
        }
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.syncSpaceFromSupabase(
                spaceCode = spaceCode,
                activeUserId = _uiState.value.activeUser.userId
            )
            while (isActive && repository.isSupabaseConfigured) {
                delay(3000L)
                repository.syncSpaceFromSupabase(
                    spaceCode = _activeSpaceCode.value,
                    activeUserId = _uiState.value.activeUser.userId
                )
            }
        }
    }

    fun bindSpaceAndUser(spaceCode: String, slot: TestUserSlot, displayName: String = "") {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DuoSpaceRepository.DEFAULT_SPACE_CODE }
        val codeChanged = _activeSpaceCode.value != cleanCode
        _activeSpaceCode.value = cleanCode
        _uiState.update {
            it.copy(
                activeUser = slot,
                activeSpaceCode = cleanCode,
                replyingTo = null
            )
        }
        viewModelScope.launch {
            if (displayName.isNotBlank()) {
                repository.updateMemberNameInSpace(cleanCode, slot.userId, displayName)
            }
        }
        if (codeChanged) {
            startBackgroundSync(cleanCode)
        }
    }

    fun createNewSpace(
        spaceCode: String,
        creatorName: String,
        onComplete: (SpaceEntity) -> Unit = {}
    ) {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DuoSpaceRepository.DEFAULT_SPACE_CODE }
        _activeSpaceCode.value = cleanCode
        _uiState.update {
            it.copy(
                activeUser = TestUserSlot.USER_1,
                activeSpaceCode = cleanCode,
                replyingTo = null
            )
        }
        viewModelScope.launch {
            val space = repository.createNewSpace(cleanCode, creatorName)
            startBackgroundSync(cleanCode)
            onComplete(space)
        }
    }

    fun joinExistingSpace(
        spaceCode: String,
        partnerName: String,
        onComplete: (SpaceEntity) -> Unit = {}
    ) {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DuoSpaceRepository.DEFAULT_SPACE_CODE }
        val joinerSlot = TestUserSlot.USER_2
        _activeSpaceCode.value = cleanCode
        _uiState.update {
            it.copy(
                activeUser = joinerSlot,
                activeSpaceCode = cleanCode,
                replyingTo = null
            )
        }
        viewModelScope.launch {
            val space = repository.joinExistingSpace(cleanCode, partnerName)
            startBackgroundSync(cleanCode)
            onComplete(space)
        }
    }

    fun markAllReadForActiveUser() {
        val code = _activeSpaceCode.value
        val uid = _uiState.value.activeUser.userId
        viewModelScope.launch {
            repository.markAllReadForUser(uid, code)
        }
    }

    fun switchActiveUser(target: TestUserSlot) {
        _uiState.update { it.copy(activeUser = target, replyingTo = null) }
        viewModelScope.launch {
            val code = _activeSpaceCode.value
            val currentSpace = repository.getSpace(code)
            if (target == TestUserSlot.USER_2 && currentSpace != null && currentSpace.partnerId == null) {
                repository.joinExistingSpace(code, currentSpace.partnerName ?: "Partner")
            }
        }
    }

    private var typingResetJob: Job? = null

    fun updateMessageInput(text: String) {
        _uiState.update { it.copy(messageInput = text) }
        val code = _activeSpaceCode.value
        val isUser1 = _uiState.value.activeUser == TestUserSlot.USER_1
        typingResetJob?.cancel()
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.updateUserPresenceStatus(code, isUser1, "typing")
            }
            typingResetJob = viewModelScope.launch {
                delay(3000L)
                repository.updateUserPresenceStatus(code, isUser1, "Online")
            }
        } else {
            viewModelScope.launch {
                repository.updateUserPresenceStatus(code, isUser1, "Online")
            }
        }
    }

    fun setVoiceRecordingActive(isRecording: Boolean) {
        val code = _activeSpaceCode.value
        val isUser1 = _uiState.value.activeUser == TestUserSlot.USER_1
        typingResetJob?.cancel()
        viewModelScope.launch {
            repository.updateUserPresenceStatus(code, isUser1, if (isRecording) "recording_voice" else "Online")
        }
    }

    fun setReplyTarget(message: MessageEntity?) {
        _uiState.update { it.copy(replyingTo = message) }
    }

    fun toggleDoodleMode(active: Boolean) {
        _uiState.update {
            it.copy(
                isDoodleModeActive = active,
                isStickerTrayOpen = if (active) false else it.isStickerTrayOpen,
                isAttachmentTrayOpen = if (active) false else it.isAttachmentTrayOpen
            )
        }
    }

    fun toggleStickerTray(open: Boolean) {
        _uiState.update {
            it.copy(
                isStickerTrayOpen = open,
                isDoodleModeActive = if (open) false else it.isDoodleModeActive,
                isAttachmentTrayOpen = if (open) false else it.isAttachmentTrayOpen
            )
        }
    }

    fun toggleAttachmentTray(open: Boolean) {
        _uiState.update {
            it.copy(
                isAttachmentTrayOpen = open,
                isStickerTrayOpen = if (open) false else it.isStickerTrayOpen,
                isDoodleModeActive = if (open) false else it.isDoodleModeActive
            )
        }
    }

    fun toggleCameraStudio(open: Boolean) {
        _uiState.update {
            it.copy(
                isCameraStudioOpen = open,
                isStickerTrayOpen = if (open) false else it.isStickerTrayOpen,
                isAttachmentTrayOpen = if (open) false else it.isAttachmentTrayOpen,
                isDoodleModeActive = if (open) false else it.isDoodleModeActive
            )
        }
    }

    fun updateScreenDoodle(doodleData: String?, extraBottomSpaceDp: Int) {
        viewModelScope.launch {
            repository.updateScreenDoodle(doodleData, extraBottomSpaceDp, _activeSpaceCode.value)
        }
    }

    fun setAutoRecordCalls(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoRecordCalls(enabled, _activeSpaceCode.value)
        }
    }

    private fun resolveActiveSenderName(): String {
        val space = spaceState.value
        return when (_uiState.value.activeUser) {
            TestUserSlot.USER_1 -> space?.creatorName?.ifBlank { "Partner 1" } ?: "Partner 1"
            TestUserSlot.USER_2 -> space?.partnerName?.ifBlank { "Partner 2" } ?: "Partner 2"
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val content = state.messageInput.trim()
        if (content.isEmpty()) return

        val senderId = state.activeUser.userId
        val senderName = resolveActiveSenderName()
        val replyTarget = state.replyingTo
        val code = _activeSpaceCode.value

        _uiState.update { it.copy(messageInput = "", replyingTo = null) }
        val isUser1 = state.activeUser == TestUserSlot.USER_1
        typingResetJob?.cancel()

        viewModelScope.launch {
            repository.updateUserPresenceStatus(code, isUser1, "Online")
            repository.sendMessage(
                senderId = senderId,
                senderName = senderName,
                content = content,
                messageType = MessageType.TEXT,
                replyTo = replyTarget,
                spaceCode = code
            )
        }
    }

    fun sendVoiceMessage(filePath: String, durationSec: Int, waveformCsv: String) {
        val state = _uiState.value
        val senderId = state.activeUser.userId
        val senderName = resolveActiveSenderName()
        val replyTarget = state.replyingTo
        val code = _activeSpaceCode.value
        val mins = durationSec / 60
        val secs = durationSec % 60
        val durationLabel = String.format(Locale.getDefault(), "%d:%02d", mins, secs)
        val meta = "$filePath|$durationSec|$waveformCsv"

        _uiState.update { it.copy(replyingTo = null) }

        viewModelScope.launch {
            repository.sendMessage(
                senderId = senderId,
                senderName = senderName,
                content = durationLabel,
                messageType = MessageType.VOICE,
                mediaMeta = meta,
                replyTo = replyTarget,
                spaceCode = code
            )
        }
    }

    fun sendStickerMessage(stickerId: String, emoji: String, caption: String, bgColorHex: Long) {
        val state = _uiState.value
        val senderId = state.activeUser.userId
        val senderName = resolveActiveSenderName()
        val replyTarget = state.replyingTo
        val code = _activeSpaceCode.value
        val meta = "$stickerId|$emoji|$caption|$bgColorHex"

        _uiState.update { it.copy(replyingTo = null) }

        viewModelScope.launch {
            repository.sendMessage(
                senderId = senderId,
                senderName = senderName,
                content = if (caption.isNotBlank()) "$emoji $caption" else emoji,
                messageType = MessageType.STICKER,
                mediaMeta = meta,
                replyTo = replyTarget,
                spaceCode = code
            )
        }
    }

    fun sendRichAttachmentMessage(
        content: String,
        messageType: String,
        mediaMeta: String
    ) {
        val state = _uiState.value
        val senderId = state.activeUser.userId
        val senderName = resolveActiveSenderName()
        val replyTarget = state.replyingTo
        val code = _activeSpaceCode.value

        _uiState.update {
            it.copy(
                replyingTo = null,
                isAttachmentTrayOpen = false,
                isCameraStudioOpen = false
            )
        }

        viewModelScope.launch {
            repository.sendMessage(
                senderId = senderId,
                senderName = senderName,
                content = content,
                messageType = messageType,
                mediaMeta = mediaMeta,
                replyTo = replyTarget,
                spaceCode = code
            )
        }
    }

    fun updateInteractiveMessage(
        message: MessageEntity,
        newContent: String,
        newMediaMeta: String?
    ) {
        viewModelScope.launch {
            repository.updateMessageMetadata(message, newContent, newMediaMeta)
        }
    }

    fun startCall(mode: CallMode = CallMode.VOICE) {
        val caller = _uiState.value.activeUser
        val autoRec = spaceState.value?.autoRecordCalls == true
        val sessionId = System.currentTimeMillis()
        lastHandledRemoteCallSessionId = sessionId
        _uiState.update {
            it.copy(
                activeCall = ActiveCallSession(
                    mode = mode,
                    callerSlot = caller,
                    callSessionId = sessionId,
                    isConnected = false,
                    isVideoEnabled = mode == CallMode.VIDEO,
                    isFrontCamera = true,
                    isRecording = autoRec,
                    wasEverRecorded = autoRec,
                    startedAtMs = sessionId
                ),
                isStickerTrayOpen = false,
                isDoodleModeActive = false
            )
        }
        com.example.data.remote.HiveLivePeerRelay.publishCallStart(
            mode = mode,
            callerSlot = caller,
            sessionId = sessionId
        )
    }

    fun answerCall() {
        val autoRec = spaceState.value?.autoRecordCalls == true
        var answeredMode = CallMode.VOICE
        var answeredCaller = _uiState.value.activeUser.name
        var answeredSessionId = lastHandledRemoteCallSessionId
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            answeredMode = current.mode
            answeredCaller = current.callerSlot.name
            answeredSessionId = current.callSessionId
            val recActive = current.isRecording || autoRec
            state.copy(
                activeCall = current.copy(
                    isConnected = true,
                    isRecording = recActive,
                    wasEverRecorded = current.wasEverRecorded || recActive,
                    startedAtMs = System.currentTimeMillis()
                )
            )
        }
        com.example.data.remote.HiveLivePeerRelay.publishCallAnswered(
            mode = answeredMode,
            callerSlot = answeredCaller,
            sessionId = answeredSessionId
        )
    }

    fun toggleCallMute() {
        var nextMuted = false
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            nextMuted = !current.isMuted
            state.copy(activeCall = current.copy(isMuted = nextMuted))
        }
        com.example.data.remote.HiveLivePeerRelay.setCallMuted(nextMuted)
    }

    fun toggleCallSpeaker() {
        var nextSpeaker = false
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            nextSpeaker = !current.isSpeakerOn
            state.copy(activeCall = current.copy(isSpeakerOn = nextSpeaker))
        }
        com.example.data.remote.HiveLivePeerRelay.setCallSpeakerOn(nextSpeaker)
    }

    fun toggleCallVideo() {
        var nextVideo = false
        var frontCam = true
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            nextVideo = !current.isVideoEnabled
            frontCam = current.isFrontCamera
            state.copy(
                activeCall = current.copy(
                    isVideoEnabled = nextVideo,
                    mode = if (nextVideo || current.isScreenSharing) CallMode.VIDEO else CallMode.VOICE
                )
            )
        }
        com.example.data.remote.HiveLivePeerRelay.setCallVideoState(
            isVideoOn = nextVideo,
            isFrontCamera = frontCam
        )
    }

    fun flipCallCamera() {
        var isVid = false
        var nextFront = true
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            isVid = current.isVideoEnabled
            nextFront = !current.isFrontCamera
            state.copy(
                activeCall = current.copy(
                    isFrontCamera = nextFront
                )
            )
        }
        com.example.data.remote.HiveLivePeerRelay.setCallVideoState(
            isVideoOn = isVid,
            isFrontCamera = nextFront
        )
    }

    fun toggleCallScreenShare(label: String = "Phone Screen") {
        var nextSharing = false
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            nextSharing = !current.isScreenSharing
            state.copy(
                activeCall = current.copy(
                    isScreenSharing = nextSharing,
                    screenShareLabel = label,
                    mode = if (nextSharing || current.isVideoEnabled) CallMode.VIDEO else CallMode.VOICE
                )
            )
        }
        com.example.data.remote.HiveLivePeerRelay.setCallScreenShareState(
            isScreenSharing = nextSharing,
            label = label
        )
    }

    fun toggleCallRecording() {
        _uiState.update { state ->
            val current = state.activeCall ?: return@update state
            val nextRec = !current.isRecording
            state.copy(
                activeCall = current.copy(
                    isRecording = nextRec,
                    wasEverRecorded = current.wasEverRecorded || nextRec
                )
            )
        }
    }

    fun endCall(
        elapsedSeconds: Int,
        recordingFilePath: String? = null,
        recordingWaveformCsv: String? = null
    ) {
        val current = _uiState.value.activeCall ?: return
        locallyEndedCallSessionId = current.callSessionId
        _uiState.update { it.copy(activeCall = null) }
        com.example.data.remote.HiveLivePeerRelay.publishCallEnded(current.callSessionId)

        val space = spaceState.value
        val code = _activeSpaceCode.value
        val senderId = current.callerSlot.userId
        val senderName = when (current.callerSlot) {
            TestUserSlot.USER_1 -> space?.creatorName?.ifBlank { "Partner 1" } ?: "Partner 1"
            TestUserSlot.USER_2 -> space?.partnerName?.ifBlank { "Partner 2" } ?: "Partner 2"
        }
        val callLabel = if (current.mode == CallMode.VIDEO) "Video call" else "Voice call"
        val recBadge = if (current.wasEverRecorded || !recordingFilePath.isNullOrBlank()) " • Recorded" else ""
        val summary = if (current.isConnected && elapsedSeconds > 0) {
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            "$callLabel • ${String.format(Locale.getDefault(), "%d:%02d", mins, secs)}$recBadge"
        } else {
            "Missed ${callLabel.lowercase()}"
        }

        val encodedMeta = listOf(
            current.mode.name,
            elapsedSeconds.toString(),
            recordingFilePath.orEmpty(),
            recordingWaveformCsv.orEmpty()
        ).joinToString("|")

        viewModelScope.launch {
            repository.sendMessage(
                senderId = senderId,
                senderName = senderName,
                content = summary,
                messageType = MessageType.CALL_LOG,
                mediaMeta = encodedMeta,
                spaceCode = code
            )
        }
    }

    fun toggleReaction(message: MessageEntity, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(message, emoji)
        }
    }

    fun replaceReaction(message: MessageEntity, emoji: String) {
        viewModelScope.launch {
            repository.setReaction(message, emoji)
        }
    }

    fun togglePinMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.togglePinMessage(message, _activeSpaceCode.value)
        }
    }

    fun unpinMessage() {
        viewModelScope.launch {
            repository.unpinCurrentMessage(_activeSpaceCode.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        repository.stopRealtime()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = DuoSpaceDatabase.getInstance(appCtx)
            val repo = DuoSpaceRepository(db.duoSpaceDao())
            val cfg = HiveSettingsPersistence.loadConfig(appCtx)
            val slot = cfg.localUserSlot
            val code = cfg.activeSpaceCode.ifBlank { DuoSpaceRepository.DEFAULT_SPACE_CODE }
            val myName = if (slot == TestUserSlot.USER_1) {
                cfg.user1Name.ifBlank { "You" }
            } else {
                cfg.user2Name.ifBlank { "You" }
            }
            return DuoSpaceViewModel(
                repository = repo,
                initialSpaceCode = code,
                initialUserSlot = slot,
                initialUserName = myName
            ) as T
        }
    }
}
