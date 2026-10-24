package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

private val DiscordBgDark = Color(0xFF111214)
private val DiscordTileSurface = Color(0xFF1E1F22)
private val DiscordTileSecondary = Color(0xFF2B2D31)
private val DiscordDockBorder = Color(0xFF313338)
private val DiscordGreen = Color(0xFF23A55A)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordRed = Color(0xFFDA373C)
private val DiscordMutedText = Color(0xFF949BA4)

// --- 1. Live Hardware Audio Output Detection & Routing (Always Defaults to Bluetooth/Headset, Else Phone — Never Speaker) ---

enum class CallAudioRoute(val label: String) {
    BLUETOOTH("Bluetooth"),
    HEADSET("Headset"),
    SPEAKER("Speaker"),
    PHONE("Phone")
}

enum class CallMinimizeMode {
    NONE,
    TOP_SWIPE_BAR,
    FLOATING_BOX
}

data class AudioRouteOption(
    val route: CallAudioRoute,
    val isConnected: Boolean,
    val hardwareDeviceName: String? = null
) {
    val displayName: String
        get() = hardwareDeviceName?.takeIf { it.isNotBlank() } ?: route.label
}

@Suppress("DEPRECATION")
fun detectAudioRouteOptions(context: Context): List<AudioRouteOption> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    var bluetoothConnected = false
    var wiredHeadsetConnected = false
    var bluetoothDeviceName: String? = null
    var wiredHeadsetName: String? = null

    if (audioManager != null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (dev in devices) {
                    val prodName = dev.productName?.toString()?.trim()?.takeIf {
                        it.isNotEmpty() && !it.equals(Build.MODEL, ignoreCase = true)
                    }
                    when (dev.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER,
                        AudioDeviceInfo.TYPE_BLE_BROADCAST -> {
                            bluetoothConnected = true
                            if (bluetoothDeviceName == null && prodName != null) {
                                bluetoothDeviceName = prodName
                            }
                        }
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                        AudioDeviceInfo.TYPE_USB_DEVICE -> {
                            wiredHeadsetConnected = true
                            if (wiredHeadsetName == null && prodName != null) {
                                wiredHeadsetName = prodName
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevices = audioManager.availableCommunicationDevices
                for (dev in commDevices) {
                    val prodName = dev.productName?.toString()?.trim()?.takeIf {
                        it.isNotEmpty() && !it.equals(Build.MODEL, ignoreCase = true)
                    }
                    when (dev.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                            bluetoothConnected = true
                            if (bluetoothDeviceName == null && prodName != null) {
                                bluetoothDeviceName = prodName
                            }
                        }
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_USB_HEADSET -> {
                            wiredHeadsetConnected = true
                            if (wiredHeadsetName == null && prodName != null) {
                                wiredHeadsetName = prodName
                            }
                        }
                    }
                }
            }
            if (audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn) {
                bluetoothConnected = true
            }
            if (audioManager.isWiredHeadsetOn) {
                wiredHeadsetConnected = true
            }
        } catch (_: Exception) {
        }
    }

    val availableRoutes = mutableListOf<AudioRouteOption>()
    if (bluetoothConnected) {
        availableRoutes.add(
            AudioRouteOption(
                route = CallAudioRoute.BLUETOOTH,
                isConnected = true,
                hardwareDeviceName = bluetoothDeviceName ?: "Bluetooth Audio"
            )
        )
    }
    if (wiredHeadsetConnected) {
        availableRoutes.add(
            AudioRouteOption(
                route = CallAudioRoute.HEADSET,
                isConnected = true,
                hardwareDeviceName = wiredHeadsetName ?: "Wired Headset"
            )
        )
    }
    availableRoutes.add(AudioRouteOption(route = CallAudioRoute.PHONE, isConnected = true))
    availableRoutes.add(AudioRouteOption(route = CallAudioRoute.SPEAKER, isConnected = true))
    return availableRoutes
}

/**
 * Always defaults to Bluetooth or Headset if connected; otherwise ALWAYS defaults to Phone (never Speaker).
 */
@Suppress("UNUSED_PARAMETER")
fun pickDefaultAudioRoute(context: Context, fallbackSpeaker: Boolean = false): CallAudioRoute {
    val options = detectAudioRouteOptions(context)
    if (options.any { it.route == CallAudioRoute.BLUETOOTH && it.isConnected }) {
        return CallAudioRoute.BLUETOOTH
    }
    if (options.any { it.route == CallAudioRoute.HEADSET && it.isConnected }) {
        return CallAudioRoute.HEADSET
    }
    return CallAudioRoute.PHONE
}

@Composable
fun rememberLiveAudioRouteState(
    context: Context,
    onRouteAutoChanged: (CallAudioRoute) -> Unit
): List<AudioRouteOption> {
    var options by remember { mutableStateOf(detectAudioRouteOptions(context)) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    val updated = detectAudioRouteOptions(context)
                    options = updated
                    val bt = updated.any { it.route == CallAudioRoute.BLUETOOTH && it.isConnected }
                    val hs = updated.any { it.route == CallAudioRoute.HEADSET && it.isConnected }
                    if (bt) {
                        onRouteAutoChanged(CallAudioRoute.BLUETOOTH)
                    } else if (hs) {
                        onRouteAutoChanged(CallAudioRoute.HEADSET)
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    val updated = detectAudioRouteOptions(context)
                    options = updated
                    val bt = updated.any { it.route == CallAudioRoute.BLUETOOTH && it.isConnected }
                    val hs = updated.any { it.route == CallAudioRoute.HEADSET && it.isConnected }
                    if (bt) {
                        onRouteAutoChanged(CallAudioRoute.BLUETOOTH)
                    } else if (hs) {
                        onRouteAutoChanged(CallAudioRoute.HEADSET)
                    } else {
                        // Never auto-switch to Speaker; always fall back to Phone
                        onRouteAutoChanged(CallAudioRoute.PHONE)
                    }
                }
            }
        } else null

        if (audioManager != null && callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                audioManager.registerAudioDeviceCallback(callback, null)
            } catch (_: Exception) {
            }
        }

        onDispose {
            if (audioManager != null && callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    audioManager.unregisterAudioDeviceCallback(callback)
                } catch (_: Exception) {
                }
            }
        }
    }

    return options
}

fun applyAudioOutputRoute(
    context: Context,
    route: CallAudioRoute,
    outputVolumePercent: Float = 100f,
    muteIncomingAudio: Boolean = false
) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    try {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val effectivePercent = if (muteIncomingAudio) 0f else outputVolumePercent
        val streams = listOf(AudioManager.STREAM_VOICE_CALL, AudioManager.STREAM_MUSIC)
        for (stream in streams) {
            try {
                val maxVol = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
                val normalized = (effectivePercent.coerceIn(0f, 100f) / 100f)
                val targetVol = (normalized * maxVol).roundToInt().coerceIn(0, maxVol)
                audioManager.setStreamVolume(stream, targetVol, 0)
            } catch (_: Exception) {
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            val target = when (route) {
                CallAudioRoute.BLUETOOTH -> devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                }
                CallAudioRoute.HEADSET -> devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
                CallAudioRoute.SPEAKER -> devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                CallAudioRoute.PHONE -> devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
            }
            if (target != null) {
                audioManager.setCommunicationDevice(target)
            } else if (route == CallAudioRoute.PHONE) {
                audioManager.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            when (route) {
                CallAudioRoute.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
                CallAudioRoute.HEADSET -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = false
                }
                CallAudioRoute.SPEAKER -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = true
                }
                CallAudioRoute.PHONE -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = false
                }
            }
        }
    } catch (_: Exception) {
    }
}

fun iconForAudioRoute(route: CallAudioRoute): ImageVector = when (route) {
    CallAudioRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
    CallAudioRoute.HEADSET -> Icons.Default.Headset
    CallAudioRoute.SPEAKER -> Icons.Default.VolumeUp
    CallAudioRoute.PHONE -> Icons.Default.PhoneInTalk
}

@Composable
fun SpeakerConfirmationDialog(
    onConfirmSpeaker: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DiscordTileSurface,
            border = BorderStroke(1.dp, DiscordDockBorder),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .testTag("speaker_confirm_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiscordBlurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speaker",
                            tint = DiscordBlurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Switch to Speaker?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Are you sure you want to play the call through the loudspeaker?",
                    fontSize = 13.sp,
                    color = DiscordMutedText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DiscordTileSecondary,
                        border = BorderStroke(1.dp, DiscordDockBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .testTag("cancel_speaker_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 11.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DiscordBlurple,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onConfirmSpeaker()
                                onDismiss()
                            }
                            .testTag("confirm_speaker_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 11.dp)
                        ) {
                            Text(
                                text = "Yes, Speaker",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioOutputSelectorDialog(
    selectedRoute: CallAudioRoute,
    options: List<AudioRouteOption>,
    onSelectRoute: (CallAudioRoute) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DiscordTileSurface,
            border = BorderStroke(1.dp, DiscordDockBorder),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .testTag("audio_output_selector_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Output",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DiscordMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                options.forEach { option ->
                    val isSelected = option.route == selectedRoute
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) DiscordBlurple.copy(alpha = 0.22f) else DiscordTileSecondary,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) DiscordBlurple else DiscordDockBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectRoute(option.route)
                                onDismiss()
                            }
                            .testTag("audio_route_${option.route.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (isSelected) DiscordBlurple else Color.White.copy(alpha = 0.08f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconForAudioRoute(option.route),
                                        contentDescription = option.route.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = option.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (option.isConnected || isSelected) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.55f)
                                    }
                                )
                                if (option.isConnected &&
                                    (option.route == CallAudioRoute.BLUETOOTH || option.route == CallAudioRoute.HEADSET)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(DiscordGreen)
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = DiscordBlurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Real Live PCM Voice DSP Engine ---

enum class NoiseSuppressionMode(val label: String) {
    KRISP("High"),
    STANDARD("Standard"),
    OFF("Off")
}

enum class AudioInputMode(val label: String) {
    VOICE_ACTIVITY("Voice Activity"),
    PUSH_TO_TALK("Push to Talk")
}

data class LiveDspTelemetry(
    val inputDb: Float = -72f,
    val ambientFloorDb: Float = -62f,
    val effectiveThresholdDb: Float = -42f,
    val isVoiceGateOpen: Boolean = false,
    val hasMicPermission: Boolean = false
)

@Composable
fun rememberRealCallVoiceDspEngine(
    enabled: Boolean,
    suppressionMode: NoiseSuppressionMode,
    echoCancelEnabled: Boolean,
    autoGainEnabled: Boolean,
    autoSensitivity: Boolean,
    manualSensitivityDb: Float,
    outputVolumePercent: Float,
    micLoopbackTest: Boolean
): State<LiveDspTelemetry> {
    val context = LocalContext.current
    val telemetryState = remember { mutableStateOf(LiveDspTelemetry()) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    LaunchedEffect(enabled) {
        if (enabled && !hasMicPermission) {
            try {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (_: Exception) {
            }
        }
    }

    val currentSuppressionMode by remember { mutableStateOf(suppressionMode) }.apply { value = suppressionMode }
    val currentEchoCancel by remember { mutableStateOf(echoCancelEnabled) }.apply { value = echoCancelEnabled }
    val currentAutoGain by remember { mutableStateOf(autoGainEnabled) }.apply { value = autoGainEnabled }
    val currentAutoSens by remember { mutableStateOf(autoSensitivity) }.apply { value = autoSensitivity }
    val currentManualSensDb by remember { mutableFloatStateOf(manualSensitivityDb) }.apply { value = manualSensitivityDb }
    val currentOutputVol by remember { mutableFloatStateOf(outputVolumePercent) }.apply { value = outputVolumePercent }
    val currentLoopback by remember { mutableStateOf(micLoopbackTest) }.apply { value = micLoopbackTest }

    LaunchedEffect(enabled, hasMicPermission) {
        if (!enabled || !hasMicPermission) {
            telemetryState.value = LiveDspTelemetry(
                inputDb = -75f,
                effectiveThresholdDb = if (currentAutoSens) -45f else currentManualSensDb,
                isVoiceGateOpen = false,
                hasMicPermission = hasMicPermission
            )
            return@LaunchedEffect
        }

        kotlinx.coroutines.withContext(Dispatchers.IO) {
            var audioRecord: AudioRecord? = null
            var loopbackTrack: AudioTrack? = null
            var hwNs: NoiseSuppressor? = null
            var hwAec: AcousticEchoCanceler? = null
            var hwAgc: AutomaticGainControl? = null

            try {
                val sampleRate = 16000
                val frameSamples = 320
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(frameSamples * 4)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    return@withContext
                }

                val sessionId = audioRecord.audioSessionId
                if (NoiseSuppressor.isAvailable()) {
                    hwNs = NoiseSuppressor.create(sessionId)
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    hwAec = AcousticEchoCanceler.create(sessionId)
                }
                if (AutomaticGainControl.isAvailable()) {
                    hwAgc = AutomaticGainControl.create(sessionId)
                }

                val minOutBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(frameSamples * 4)

                loopbackTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(minOutBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioRecord.startRecording()
                loopbackTrack.play()

                val pcmBuffer = ShortArray(frameSamples)
                val outBuffer = ShortArray(frameSamples)

                var prevX = 0f
                var prevY = 0f
                val hpfAlpha = 0.967f
                var noiseFloorEnv = 180f
                var agcGain = 1.0f
                var smoothedDb = -70f

                while (isActive) {
                    try {
                        hwNs?.enabled = (currentSuppressionMode != NoiseSuppressionMode.OFF)
                        hwAec?.enabled = currentEchoCancel
                        hwAgc?.enabled = currentAutoGain
                    } catch (_: Exception) {
                    }

                    val readCount = audioRecord.read(pcmBuffer, 0, frameSamples)
                    if (readCount <= 0) continue

                    val nsMode = currentSuppressionMode
                    val agcOn = currentAutoGain
                    val aecOn = currentEchoCancel

                    var sumSquares = 0.0
                    val floatFrame = FloatArray(readCount)
                    for (i in 0 until readCount) {
                        val x = pcmBuffer[i].toFloat()
                        val y = hpfAlpha * (prevY + x - prevX)
                        prevX = x
                        prevY = y
                        floatFrame[i] = y
                        sumSquares += (y * y).toDouble()
                    }

                    val rawRms = sqrt(sumSquares / readCount).toFloat().coerceAtLeast(1f)

                    noiseFloorEnv = if (rawRms < noiseFloorEnv) {
                        0.85f * noiseFloorEnv + 0.15f * rawRms
                    } else {
                        0.995f * noiseFloorEnv + 0.005f * rawRms
                    }.coerceIn(40f, 4000f)

                    val suppressionFactor = when (nsMode) {
                        NoiseSuppressionMode.KRISP -> {
                            val snr = (rawRms / (noiseFloorEnv * 1.6f)).coerceIn(0f, 4f)
                            if (snr < 1f) (snr * snr * 0.12f) else 1f
                        }
                        NoiseSuppressionMode.STANDARD -> {
                            val snr = (rawRms / (noiseFloorEnv * 1.25f)).coerceIn(0f, 4f)
                            if (snr < 1f) (snr * 0.35f) else 1f
                        }
                        NoiseSuppressionMode.OFF -> 1f
                    }

                    val echoDuckFactor = if (aecOn && !currentLoopback && rawRms < noiseFloorEnv * 1.35f) {
                        0.5f
                    } else {
                        1.0f
                    }

                    val postNsRms = (rawRms * suppressionFactor * echoDuckFactor).coerceAtLeast(1f)
                    if (agcOn && postNsRms > noiseFloorEnv * 1.3f) {
                        val desiredGain = (3800f / postNsRms).coerceIn(0.6f, 4.2f)
                        agcGain = 0.90f * agcGain + 0.10f * desiredGain
                    } else if (!agcOn) {
                        agcGain = 1.0f
                    }

                    var processedSquares = 0.0
                    for (i in 0 until readCount) {
                        val s = floatFrame[i] * suppressionFactor * echoDuckFactor * agcGain
                        processedSquares += (s * s).toDouble()
                    }
                    val processedRms = sqrt(processedSquares / readCount).toFloat().coerceAtLeast(1f)
                    val frameDb = (20.0 * log10(processedRms / 32768.0)).toFloat().coerceIn(-80f, 0f)
                    smoothedDb = (0.65f * smoothedDb + 0.35f * frameDb).coerceIn(-80f, 0f)

                    val ambientDb = (20.0 * log10(noiseFloorEnv / 32768.0)).toFloat().coerceIn(-78f, -25f)
                    val effectiveThresholdDb = if (currentAutoSens) {
                        (ambientDb + 11f).coerceIn(-62f, -24f)
                    } else {
                        currentManualSensDb.coerceIn(-80f, 0f)
                    }

                    val gateOpen = smoothedDb >= effectiveThresholdDb

                    telemetryState.value = LiveDspTelemetry(
                        inputDb = smoothedDb,
                        ambientFloorDb = ambientDb,
                        effectiveThresholdDb = effectiveThresholdDb,
                        isVoiceGateOpen = gateOpen,
                        hasMicPermission = true
                    )

                    if (gateOpen) {
                        val outGain = (currentOutputVol / 100f).coerceIn(0f, 2f)
                        for (i in 0 until readCount) {
                            val rawSample = floatFrame[i] * suppressionFactor * agcGain * outGain
                            val normalized = rawSample / 32768f
                            val limited = tanh(normalized.toDouble()).toFloat() * 32760f
                            outBuffer[i] = limited.roundToInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        }
                        com.example.data.remote.HiveLivePeerRelay.submitCallDspPcm16(outBuffer, readCount)
                        if (currentLoopback) {
                            loopbackTrack.write(outBuffer, 0, readCount)
                        }
                    }
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            } finally {
                try {
                    hwNs?.release()
                } catch (_: Exception) {
                }
                try {
                    hwAec?.release()
                } catch (_: Exception) {
                }
                try {
                    hwAgc?.release()
                } catch (_: Exception) {
                }
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {
                }
                try {
                    loopbackTrack?.stop()
                    loopbackTrack?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    return telemetryState
}

// --- 3. Custom User Soundboard (No Default Clips — Record or Import Audio) ---

data class SoundboardClip(
    val id: String,
    val title: String,
    val filePath: String,
    val accentHex: Long = 0xFF5865F2L,
    val ownerUserId: String = "user_1",
    val isShared: Boolean = true,
    val durationSec: Int = 3
)

fun playCustomSoundboardClip(
    clip: SoundboardClip,
    soundboardVolume: Float,
    outputVolume: Float,
    isDeafened: Boolean,
    scope: CoroutineScope
) {
    if (isDeafened || soundboardVolume <= 0.01f || outputVolume <= 0.01f) return
    val gain = (soundboardVolume * (outputVolume / 100f)).coerceIn(0f, 1f)
    scope.launch(Dispatchers.IO) {
        try {
            val file = File(clip.filePath)
            if (!file.exists()) return@launch
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setVolume(gain, gain)
            player.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (_: Exception) {
                }
            }
            player.prepare()
            player.start()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun DiscordSoundboardPanel(
    customClips: List<SoundboardClip>,
    onDeleteClip: (SoundboardClip) -> Unit,
    onPlayClip: (SoundboardClip) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .testTag("call_soundboard_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = DiscordBlurple,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Soundboard",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close soundboard",
                    tint = DiscordMutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (customClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sounds yet. Add or record sounds (up to 5s) in Dashboard Settings.",
                    fontSize = 11.5.sp,
                    color = DiscordMutedText
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 128.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(customClips, key = { it.id }) { clip ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DiscordTileSecondary,
                        border = BorderStroke(1.dp, DiscordDockBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPlayClip(clip) }
                            .testTag("soundboard_clip_${clip.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clip.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (clip.isShared) "Shared" else "Personal",
                                    fontSize = 9.5.sp,
                                    color = DiscordMutedText
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete sound",
                                tint = DiscordMutedText,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onDeleteClip(clip) }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = DiscordDockBorder)
    }
}

// --- 4. App-Matched Voice & Audio Settings Section ---

@Composable
private fun CompactPillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = DiscordGreen,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else DiscordTileSurface,
        label = "switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, if (checked) activeColor else DiscordDockBorder, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Matches the exact Bento Card style of Speaker / Record Call / Front Camera / Audio Mute in the dock.
 */
@Composable
private fun DockStyleToggleCard(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) activeColor.copy(alpha = 0.22f) else DiscordTileSecondary,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) activeColor.copy(alpha = 0.7f) else DiscordDockBorder
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) activeColor else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DiscordFullAudioSettingsSection(
    isDeafened: Boolean,
    onToggleDeafen: () -> Unit,
    noiseSuppressionMode: NoiseSuppressionMode,
    onSelectNoiseSuppression: (NoiseSuppressionMode) -> Unit,
    echoCancelEnabled: Boolean,
    onToggleEchoCancel: (Boolean) -> Unit,
    autoGainEnabled: Boolean,
    onToggleAutoGain: (Boolean) -> Unit,
    inputMode: AudioInputMode,
    onSelectInputMode: (AudioInputMode) -> Unit,
    autoSensitivity: Boolean,
    onToggleAutoSensitivity: (Boolean) -> Unit,
    inputSensitivityDb: Float,
    onChangeSensitivityDb: (Float) -> Unit,
    outputVolumePercent: Float,
    onChangeOutputVolume: (Float) -> Unit,
    soundboardVolumePercent: Float,
    onChangeSoundboardVolume: (Float) -> Unit,
    liveInputDb: Float,
    effectiveThresholdDb: Float,
    isVoiceGateOpen: Boolean,
    micLoopbackTest: Boolean = false,
    onToggleMicLoopbackTest: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 310.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 2.dp, bottom = 6.dp)
            .testTag("discord_full_audio_settings"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Echo Cancel & Auto Gain (Exact same 2-column Bento Card style as the 4 quick-menu tiles above)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DockStyleToggleCard(
                icon = Icons.Default.GraphicEq,
                title = "Echo Cancel",
                isActive = echoCancelEnabled,
                activeColor = DiscordGreen,
                onClick = { onToggleEchoCancel(!echoCancelEnabled) },
                testTag = "toggle_echo_cancel",
                modifier = Modifier.weight(1f)
            )
            DockStyleToggleCard(
                icon = Icons.Default.Tune,
                title = "Auto Gain",
                isActive = autoGainEnabled,
                activeColor = DiscordGreen,
                onClick = { onToggleAutoGain(!autoGainEnabled) },
                testTag = "toggle_auto_gain",
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Deafen & Noise Suppression Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DiscordTileSecondary,
            border = BorderStroke(1.dp, DiscordDockBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
                            contentDescription = "Deafen",
                            tint = if (isDeafened) DiscordRed else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Deafen",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    CompactPillSwitch(
                        checked = isDeafened,
                        onCheckedChange = { onToggleDeafen() },
                        activeColor = DiscordRed,
                        modifier = Modifier.testTag("deafen_switch")
                    )
                }

                HorizontalDivider(color = DiscordDockBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Noise Suppression",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiscordTileSurface)
                            .border(1.dp, DiscordDockBorder, RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        NoiseSuppressionMode.entries.forEach { mode ->
                            val selected = mode == noiseSuppressionMode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) DiscordBlurple else Color.Transparent)
                                    .clickable { onSelectNoiseSuppression(mode) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else DiscordMutedText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Audio Input Method & Sensitivity Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DiscordTileSecondary,
            border = BorderStroke(1.dp, DiscordDockBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AudioInputMode.entries.forEach { mode ->
                        val selected = mode == inputMode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) DiscordBlurple else DiscordTileSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) DiscordBlurple else DiscordDockBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectInputMode(mode) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensitivity (${effectiveThresholdDb.roundToInt()} dB)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (micLoopbackTest) DiscordGreen.copy(alpha = 0.22f) else DiscordTileSurface,
                            border = BorderStroke(
                                1.dp,
                                if (micLoopbackTest) DiscordGreen else DiscordDockBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleMicLoopbackTest(!micLoopbackTest) }
                                .testTag("mic_loopback_test_button")
                        ) {
                            Text(
                                text = if (micLoopbackTest) "Stop Test" else "Test Mic",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (micLoopbackTest) DiscordGreen else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Auto",
                            fontSize = 11.sp,
                            color = DiscordMutedText
                        )
                        CompactPillSwitch(
                            checked = autoSensitivity,
                            onCheckedChange = onToggleAutoSensitivity,
                            activeColor = DiscordGreen
                        )
                    }
                }

                // Clean App-Matched Voice Gate Meter Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DiscordTileSurface)
                ) {
                    val normLevel = ((liveInputDb + 80f) / 80f).coerceIn(0.03f, 1f)
                    val normThreshold = ((effectiveThresholdDb + 80f) / 80f).coerceIn(0.05f, 0.95f)
                    val activeWidth = size.width * normLevel
                    drawRect(
                        color = if (isVoiceGateOpen) DiscordGreen else Color(0xFF4E5058),
                        size = size.copy(width = activeWidth)
                    )
                    val markerX = size.width * normThreshold
                    drawLine(
                        color = Color.White,
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, size.height),
                        strokeWidth = 4f
                    )
                }

                if (!autoSensitivity) {
                    Slider(
                        value = inputSensitivityDb,
                        onValueChange = onChangeSensitivityDb,
                        valueRange = -80f..0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = DiscordGreen,
                            inactiveTrackColor = DiscordTileSurface
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }

        // 4. Output Volume & Soundboard Volume Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DiscordTileSecondary,
            border = BorderStroke(1.dp, DiscordDockBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Output Volume",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${outputVolumePercent.roundToInt()}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiscordBlurple
                    )
                }
                Slider(
                    value = outputVolumePercent,
                    onValueChange = onChangeOutputVolume,
                    valueRange = 0f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DiscordBlurple,
                        inactiveTrackColor = DiscordTileSurface
                    ),
                    modifier = Modifier.height(24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Soundboard Volume",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${(soundboardVolumePercent * 100f).roundToInt()}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiscordGreen
                    )
                }
                Slider(
                    value = soundboardVolumePercent,
                    onValueChange = onChangeSoundboardVolume,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DiscordGreen,
                        inactiveTrackColor = DiscordTileSurface
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

// --- 5. Minimized Call Overlay (Shows ONLY Top Pull-Down Bar OR ONLY Floating Box — Never Both) ---

@Composable
fun MinimizedDiscordCallOverlay(
    minimizeMode: CallMinimizeMode,
    partnerName: String,
    formattedTimer: String,
    isConnected: Boolean,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isDeafened: Boolean,
    isVideoEnabled: Boolean,
    isScreenSharing: Boolean,
    onExpandCall: () -> Unit,
    onToggleMute: () -> Unit,
    onEndCall: () -> Unit
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val pipWidthDp = 132.dp
    val pipHeightDp = 168.dp
    val edgeMarginDp = 12.dp
    val topReservedDp = 76.dp
    val bottomReservedDp = 84.dp

    val pipWidthPx = with(density) { pipWidthDp.toPx() }
    val pipHeightPx = with(density) { pipHeightDp.toPx() }
    val marginPx = with(density) { edgeMarginDp.toPx() }
    val topMinPx = with(density) { topReservedDp.toPx() }
    val bottomMarginPx = with(density) { bottomReservedDp.toPx() }

    var initializedPosition by remember { mutableStateOf(false) }
    var pipX by remember { mutableFloatStateOf(0f) }
    var pipY by remember { mutableFloatStateOf(0f) }

    var pullDownPx by remember { mutableFloatStateOf(0f) }
    val animatedPullDownPx by animateFloatAsState(
        targetValue = pullDownPx,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pull_down_sheet"
    )

    val maxX = (containerSize.width - pipWidthPx - marginPx).coerceAtLeast(marginPx)
    val maxY = (containerSize.height - pipHeightPx - bottomMarginPx).coerceAtLeast(topMinPx)

    LaunchedEffect(containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            if (!initializedPosition) {
                pipX = maxX
                pipY = maxY
                initializedPosition = true
            } else {
                pipX = pipX.coerceIn(marginPx, maxX)
                pipY = pipY.coerceIn(topMinPx, maxY)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        if (minimizeMode == CallMinimizeMode.TOP_SWIPE_BAR) {
            // Mode 1: Clicked Chat button inside Call UI -> ONLY Top Pull-Down Bar is shown
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .offset { IntOffset(0, animatedPullDownPx.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (pullDownPx > 56f) {
                                    pullDownPx = 0f
                                    onExpandCall()
                                } else {
                                    pullDownPx = 0f
                                }
                            },
                            onDragCancel = { pullDownPx = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                pullDownPx = (pullDownPx + dragAmount).coerceIn(0f, 220f)
                            }
                        )
                    }
                    .testTag("call_minimized_top_bar"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DiscordTileSurface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, DiscordDockBorder),
                    tonalElevation = 10.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onExpandCall)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) DiscordGreen else Color(0xFFFBBF24))
                                )
                                Text(
                                    text = partnerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = formattedTimer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DiscordGreen
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand call",
                                tint = DiscordMutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                        )
                    }
                }
            }
        } else if (minimizeMode == CallMinimizeMode.FLOATING_BOX) {
            // Mode 2: Closed via mobile Back button -> ONLY Screen-Clamped Draggable Floating Call Box is shown
            val borderColor by animateColorAsState(
                targetValue = if (isSpeaking) DiscordGreen else DiscordDockBorder,
                label = "floating_call_border"
            )

            val clampedX = if (initializedPosition) pipX.coerceIn(marginPx, maxX) else maxX
            val clampedY = if (initializedPosition) pipY.coerceIn(topMinPx, maxY) else maxY

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = DiscordTileSurface,
                tonalElevation = 14.dp,
                shadowElevation = 18.dp,
                border = BorderStroke(if (isSpeaking) 2.dp else 1.dp, borderColor),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = clampedX.roundToInt(),
                            y = clampedY.roundToInt()
                        )
                    }
                    .size(width = pipWidthDp, height = pipHeightDp)
                    .clip(RoundedCornerShape(22.dp))
                    .pointerInput(maxX, maxY) {
                        detectDragGestures(
                            onDragEnd = {
                                val midX = (marginPx + maxX) / 2f
                                pipX = if (pipX < midX) marginPx else maxX
                                pipY = pipY.coerceIn(topMinPx, maxY)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            pipX = (pipX + dragAmount.x).coerceIn(marginPx, maxX)
                            pipY = (pipY + dragAmount.y).coerceIn(topMinPx, maxY)
                        }
                    }
                    .clickable(onClick = onExpandCall)
                    .testTag("floating_call_pip_box")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isVideoEnabled || isScreenSharing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isScreenSharing) {
                                            listOf(Color(0xFF1E293B), Color(0xFF312E81))
                                        } else {
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        }
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(DiscordTileSecondary)
                                .border(
                                    width = if (isSpeaking) 2.dp else 1.dp,
                                    color = if (isSpeaking) DiscordGreen else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = partnerName.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = partnerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formattedTimer,
                            fontSize = 10.sp,
                            color = DiscordMutedText
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isMuted || isDeafened) DiscordRed else DiscordTileSecondary,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleMute)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDeafened) {
                                        Icons.Default.VolumeOff
                                    } else if (isMuted) {
                                        Icons.Default.MicOff
                                    } else {
                                        Icons.Default.Mic
                                    },
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = DiscordRed,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onEndCall)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End call",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PushToTalkHoldBar(
    isHoldingPtt: Boolean,
    onHoldChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isHoldingPtt) DiscordGreen else DiscordBlurple,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHoldChange(true)
                        tryAwaitRelease()
                        onHoldChange(false)
                    }
                )
            }
            .testTag("push_to_talk_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Push to Talk",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isHoldingPtt) "Transmitting…" else "Hold to Talk",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
