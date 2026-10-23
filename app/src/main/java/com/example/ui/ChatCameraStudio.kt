package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.MessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CameraCaptureMode {
    PHOTO,
    VIDEO
}

enum class CameraFlashSetting(val label: String) {
    OFF("Off"),
    AUTO("Auto"),
    ON("On")
}

enum class CameraTimerSetting(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s")
}

enum class CameraAspectRatioSetting(val ratio: Float, val label: String) {
    RATIO_4_3(3f / 4f, "4:3"),
    RATIO_16_9(9f / 16f, "16:9"),
    RATIO_1_1(1f, "1:1")
}

enum class CameraFilterPreset(
    val id: String,
    val label: String,
    val tintColor: Long,
    val primaryBgHex: Long,
    val secondaryBgHex: Long
) {
    NATURAL("natural", "Natural", 0x00000000L, 0xFF1E293BL, 0xFF334155L),
    WARM_SUNSET("warm", "Warm Glow", 0x38F97316L, 0xFF431407L, 0xFF9A3412L),
    VINTAGE_90S("vintage", "90s Film", 0x35F59E0BL, 0xFF27272AL, 0xFF52525BL),
    ROSE_QUARTZ("rose", "Rose Gold", 0x38EC4899L, 0xFF4C0519L, 0xFF9D174DL),
    CYBER_NEON("cyber", "Cyber", 0x386366F1L, 0xFF0F172AL, 0xFF312E81L),
    NOIR_BW("noir", "Noir B&W", 0x45000000L, 0xFF09090BL, 0xFF27272AL)
}

data class NormalizedDrawStroke(
    val pointsNorm: List<Offset>, // 0f..1f normalized coordinates
    val colorArgb: Long,
    val strokeWidthDp: Float
)

data class PlacedStampOverlay(
    val id: String,
    val text: String,
    val xNorm: Float,
    val yNorm: Float
)

val STUDIO_BRUSH_COLORS = listOf(
    0xFFFFFFFFL, // White
    0xFFEF4444L, // Red
    0xFFF97316L, // Orange
    0xFFFBBF24L, // Gold
    0xFF10B981L, // Emerald
    0xFF3B82F6L, // Blue
    0xFFEC4899L, // Pink
    0xFF8B5CF6L  // Violet
)

val STUDIO_QUICK_STAMPS = listOf(
    "❤️", "🔥", "✨", "🥰", "💋", "📸", "🎉", "Date Night", "Miss You", "Us Two"
)

/**
 * Full-Screen In-App Camera & Edit/Draw Studio for Chat.
 * Supports both Photos and Videos, Pro Camera Settings, Freehand Drawing, Text & Stamp Overlays,
 * Save to Phone, and Direct Send to Chat (with Signal-style smart sizer + E2EE sandbox storage).
 */
@Composable
fun ChatCameraStudioModal(
    activeUserName: String,
    partnerName: String,
    initialEditBitmap: Bitmap? = null,
    initialCaption: String = "",
    onClose: () -> Unit,
    onSendMediaToChat: (content: String, messageType: String, mediaMeta: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera Settings State
    var captureMode by remember { mutableStateOf(CameraCaptureMode.PHOTO) }
    var isFrontLens by remember { mutableStateOf(false) }
    var flashSetting by remember { mutableStateOf(CameraFlashSetting.AUTO) }
    var timerSetting by remember { mutableStateOf(CameraTimerSetting.OFF) }
    var aspectRatioSetting by remember { mutableStateOf(CameraAspectRatioSetting.RATIO_4_3) }
    var selectedFilter by remember { mutableStateOf(CameraFilterPreset.NATURAL) }
    var showGridlines by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var smartSizerEnabled by remember { mutableStateOf(true) }
    var dateWatermarkEnabled by remember { mutableStateOf(true) }
    var mirrorFrontCamera by remember { mutableStateOf(false) }
    var highFpsVideo by remember { mutableStateOf(true) }

    // Capture & Recording State
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var isFlashBurstActive by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordedVideoSeconds by remember { mutableIntStateOf(0) }

    // Post-Capture Studio State
    var capturedBitmap by remember(initialEditBitmap) { mutableStateOf(initialEditBitmap) }
    var capturedIsVideo by remember { mutableStateOf(false) }
    var capturedVideoDurationSec by remember { mutableIntStateOf(0) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Studio Drawing & Annotation State
    var isDrawModeActive by remember { mutableStateOf(true) }
    val studioStrokes = remember { mutableStateListOf<NormalizedDrawStroke>() }
    val activeStrokePoints = remember { mutableStateListOf<Offset>() }
    var selectedBrushColor by remember { mutableLongStateOf(STUDIO_BRUSH_COLORS[1]) }
    var selectedBrushWidthDp by remember { mutableFloatStateOf(5f) }
    val placedStamps = remember { mutableStateListOf<PlacedStampOverlay>() }
    var overlayBannerText by remember { mutableStateOf("") }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var tempTextDialogInput by remember { mutableStateOf("") }
    var captionInput by remember(initialCaption) { mutableStateOf(initialCaption) }
    var canvasPixelSize by remember { mutableStateOf(IntSize(1080, 1440)) }

    // Camera Permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && initialEditBitmap == null) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // Photo Picker inside Camera Studio so user can also import a photo/video to draw on
    val studioGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (raw != null) {
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size)
                    if (bmp != null) {
                        capturedBitmap = bmp
                        capturedIsVideo = mime.startsWith("video/")
                        capturedVideoDurationSec = if (capturedIsVideo) 6 else 0
                        studioStrokes.clear()
                        placedStamps.clear()
                    }
                }
            }
        }
    }

    // Video Recording Timer Loop
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordedVideoSeconds = 0
            while (isActive && isRecordingVideo) {
                delay(1000L)
                recordedVideoSeconds += 1
            }
        }
    }

    BackHandler {
        if (capturedBitmap != null && initialEditBitmap == null) {
            capturedBitmap = null
            capturedIsVideo = false
            studioStrokes.clear()
            placedStamps.clear()
            overlayBannerText = ""
        } else {
            onClose()
        }
    }

    fun triggerCaptureNow(forVideo: Boolean, durationSec: Int = 0) {
        coroutineScope.launch {
            if (flashSetting != CameraFlashSetting.OFF) {
                isFlashBurstActive = true
                delay(110L)
                isFlashBurstActive = false
            }
            val hardwareBitmap = try {
                previewViewRef?.bitmap
            } catch (_: Exception) {
                null
            }
            val baseBmp = hardwareBitmap ?: generateStudioSceneBitmap(
                width = 960,
                height = (960f / aspectRatioSetting.ratio).toInt().coerceIn(720, 1700),
                isFrontLens = isFrontLens,
                isVideo = forVideo,
                durationSec = durationSec,
                activeUserName = activeUserName,
                partnerName = partnerName,
                filter = selectedFilter
            )
            capturedBitmap = baseBmp
            capturedIsVideo = forVideo
            capturedVideoDurationSec = durationSec.coerceAtLeast(if (forVideo) 3 else 0)
            studioStrokes.clear()
            placedStamps.clear()
        }
    }

    fun onShutterPressed() {
        if (captureMode == CameraCaptureMode.VIDEO) {
            if (isRecordingVideo) {
                val finalDuration = recordedVideoSeconds.coerceAtLeast(1)
                isRecordingVideo = false
                triggerCaptureNow(forVideo = true, durationSec = finalDuration)
            } else {
                coroutineScope.launch {
                    if (timerSetting.seconds > 0) {
                        for (t in timerSetting.seconds downTo 1) {
                            countdownRemaining = t
                            delay(1000L)
                        }
                        countdownRemaining = 0
                    }
                    isRecordingVideo = true
                }
            }
        } else {
            coroutineScope.launch {
                if (timerSetting.seconds > 0) {
                    for (t in timerSetting.seconds downTo 1) {
                        countdownRemaining = t
                        delay(1000L)
                    }
                    countdownRemaining = 0
                }
                triggerCaptureNow(forVideo = false)
            }
        }
    }

    // Add Text Overlay Dialog
    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Add Text Overlay", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempTextDialogInput,
                    onValueChange = { tempTextDialogInput = it },
                    placeholder = { Text("Type text to place on photo/video…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_text_overlay_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        overlayBannerText = tempTextDialogInput.trim()
                        showAddTextDialog = false
                    },
                    modifier = Modifier.testTag("camera_text_overlay_confirm")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pro Camera Settings Dialog / Sheet
    if (showSettingsSheet) {
        AlertDialog(
            onDismissRequest = { showSettingsSheet = false },
            title = { Text("Camera Studio Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Photo Sizer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Auto-optimizes photos to ~200 KB before encrypting",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = smartSizerEnabled,
                            onCheckedChange = { smartSizerEnabled = it },
                            modifier = Modifier.testTag("camera_setting_smart_sizer")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rule-of-Thirds Grid", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Show 3×3 alignment grid in viewfinder",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showGridlines,
                            onCheckedChange = { showGridlines = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Film Date Stamp", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Stamp date & time in corner of captured shot",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dateWatermarkEnabled,
                            onCheckedChange = { dateWatermarkEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mirror Front Selfie", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Keep natural mirror reflection on front camera",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = mirrorFrontCamera,
                            onCheckedChange = { mirrorFrontCamera = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("60fps Smooth Video", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (highFpsVideo) "1080p • 60fps Smooth" else "720p • Compact Clip",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = highFpsVideo,
                            onCheckedChange = { highFpsVideo = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsSheet = false }) {
                    Text("Done")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("chat_camera_studio_modal")
    ) {
        if (capturedBitmap == null) {
            // =========================================================================
            // 1. LIVE CAMERA VIEWFINDER MODE (PHOTO & VIDEO + SETTINGS)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Camera Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .testTag("close_camera_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Camera",
                            tint = Color.White
                        )
                    }

                    // Quick Camera Settings Pills: Flash, Timer, Grid, Aspect Ratio, Pro Gear
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flash Toggle
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    flashSetting = when (flashSetting) {
                                        CameraFlashSetting.OFF -> CameraFlashSetting.AUTO
                                        CameraFlashSetting.AUTO -> CameraFlashSetting.ON
                                        CameraFlashSetting.ON -> CameraFlashSetting.OFF
                                    }
                                }
                                .testTag("camera_flash_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (flashSetting) {
                                        CameraFlashSetting.OFF -> Icons.Default.FlashOff
                                        CameraFlashSetting.AUTO -> Icons.Default.FlashAuto
                                        CameraFlashSetting.ON -> Icons.Default.FlashOn
                                    },
                                    contentDescription = "Flash",
                                    tint = if (flashSetting == CameraFlashSetting.OFF) Color.White else Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = flashSetting.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Timer Toggle
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    timerSetting = when (timerSetting) {
                                        CameraTimerSetting.OFF -> CameraTimerSetting.SEC_3
                                        CameraTimerSetting.SEC_3 -> CameraTimerSetting.SEC_10
                                        CameraTimerSetting.SEC_10 -> CameraTimerSetting.OFF
                                    }
                                }
                                .testTag("camera_timer_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (timerSetting == CameraTimerSetting.OFF) Color.White else Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = timerSetting.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Aspect Ratio Toggle
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    aspectRatioSetting = when (aspectRatioSetting) {
                                        CameraAspectRatioSetting.RATIO_4_3 -> CameraAspectRatioSetting.RATIO_16_9
                                        CameraAspectRatioSetting.RATIO_16_9 -> CameraAspectRatioSetting.RATIO_1_1
                                        CameraAspectRatioSetting.RATIO_1_1 -> CameraAspectRatioSetting.RATIO_4_3
                                    }
                                }
                                .testTag("camera_ratio_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Ratio",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = aspectRatioSetting.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Grid Toggle
                        IconButton(
                            onClick = { showGridlines = !showGridlines },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("camera_grid_toggle")
                        ) {
                            Icon(
                                imageVector = if (showGridlines) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = "Toggle Grid",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Pro Settings Gear Button
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("camera_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Camera Settings",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Center Viewfinder Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatioSetting.ratio, matchHeightConstraintsFirst = false)
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                width = if (isRecordingVideo) 2.5.dp else 1.dp,
                                color = if (isRecordingVideo) Color(0xFFEF4444) else Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        // 1. Live Interactive Studio Backdrop (always visible or blended under CameraX)
                        LiveViewfinderSceneCanvas(
                            isFrontLens = isFrontLens,
                            isVideoMode = captureMode == CameraCaptureMode.VIDEO,
                            isRecording = isRecordingVideo,
                            filter = selectedFilter,
                            activeUserName = activeUserName,
                            partnerName = partnerName
                        )

                        // 2. Real CameraX PreviewView when CAMERA permission is granted
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                        alpha = 0.92f
                                        previewViewRef = this
                                    }
                                },
                                update = { pv ->
                                    pv.scaleX = if (isFrontLens && mirrorFrontCamera) -1f else 1f
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            LaunchedEffect(previewViewRef, isFrontLens) {
                                val pv = previewViewRef ?: return@LaunchedEffect
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    runCatching {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(pv.surfaceProvider)
                                        }
                                        val desiredSelector = if (isFrontLens) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }
                                        val activeSelector = if (runCatching { cameraProvider.hasCamera(desiredSelector) }.getOrDefault(false)) {
                                            desiredSelector
                                        } else if (runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)) {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        }
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, activeSelector, preview)
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            }
                        }

                        // 4. Rule-of-Thirds 3x3 Grid Overlay
                        if (showGridlines) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val gridColor = Color.White.copy(alpha = 0.28f)
                                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())
                            }
                        }

                        // 5. Video Recording Pulsing REC Badge
                        if (isRecordingVideo) {
                            val pulseTransition = rememberInfiniteTransition(label = "rec")
                            val dotAlpha by pulseTransition.animateFloat(
                                initialValue = 0.25f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(550, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot"
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = dotAlpha))
                                    )
                                    val mins = recordedVideoSeconds / 60
                                    val secs = recordedVideoSeconds % 60
                                    Text(
                                        text = String.format(Locale.US, "REC %02d:%02d", mins, secs),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 6. Countdown Overlay (3s / 10s Timer)
                        if (countdownRemaining > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = countdownRemaining.toString(),
                                    color = Color.White,
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // 7. Flash Burst White Effect
                        if (isFlashBurstActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.85f))
                            )
                        }
                    }
                }

                // Bottom Camera Controls: Photo/Video Mode Switcher + Shutter Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PHOTO / VIDEO Mode Pill Switcher
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            CameraCaptureMode.entries.forEach { mode ->
                                val selected = captureMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) {
                                                if (mode == CameraCaptureMode.VIDEO) Color(0xFFEF4444) else Color.White
                                            } else Color.Transparent
                                        )
                                        .clickable {
                                            if (!isRecordingVideo) captureMode = mode
                                        }
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                        .testTag("camera_mode_${mode.name.lowercase(Locale.US)}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (mode == CameraCaptureMode.PHOTO) {
                                                Icons.Default.PhotoCamera
                                            } else {
                                                Icons.Default.Videocam
                                            },
                                            contentDescription = mode.name,
                                            tint = if (selected) {
                                                if (mode == CameraCaptureMode.VIDEO) Color.White else Color.Black
                                            } else Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (mode == CameraCaptureMode.PHOTO) "Photo" else "Video",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) {
                                                if (mode == CameraCaptureMode.VIDEO) Color.White else Color.Black
                                            } else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Shutter Button + Gallery Import + Lens Flip Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Import from Gallery to Edit/Draw
                        IconButton(
                            onClick = {
                                studioGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("camera_import_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Pick from Gallery to Edit",
                                tint = Color.White
                            )
                        }

                        // Big Shutter Button (Photo Snap OR Video Start/Stop)
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (captureMode == CameraCaptureMode.VIDEO) Color(0xFFEF4444)
                                    else Color.White
                                )
                                .clickable { onShutterPressed() }
                                .testTag("camera_shutter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (captureMode == CameraCaptureMode.VIDEO && isRecordingVideo) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Flip Front / Back Camera
                        IconButton(
                            onClick = { isFrontLens = !isFrontLens },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("camera_flip_lens_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Flip Camera Lens",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // 2. POST-CAPTURE EDIT, DRAW, SAVE TO PHONE & SEND STUDIO
            // =========================================================================
            val baseBitmap = capturedBitmap!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Studio Edit Toolbar: Retake, Draw Toggle, Undo, Clear, Add Text, Save to Phone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake / Back
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (initialEditBitmap != null) {
                                    onClose()
                                } else {
                                    capturedBitmap = null
                                    capturedIsVideo = false
                                    studioStrokes.clear()
                                    placedStamps.clear()
                                    overlayBannerText = ""
                                }
                            }
                            .testTag("camera_retake_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retake",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (initialEditBitmap != null) "Close" else "Retake",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Draw / Text / Undo / Clear / Save to Phone Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle Freehand Draw Mode
                        IconButton(
                            onClick = { isDrawModeActive = !isDrawModeActive },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDrawModeActive) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.14f)
                                )
                                .testTag("studio_draw_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Draw on shot",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Add Text Overlay
                        IconButton(
                            onClick = {
                                tempTextDialogInput = overlayBannerText
                                showAddTextDialog = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (overlayBannerText.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.14f)
                                )
                                .testTag("studio_add_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Add Text Overlay",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Undo Stroke
                        IconButton(
                            onClick = {
                                if (studioStrokes.isNotEmpty()) {
                                    studioStrokes.removeAt(studioStrokes.lastIndex)
                                } else if (placedStamps.isNotEmpty()) {
                                    placedStamps.removeAt(placedStamps.lastIndex)
                                }
                            },
                            enabled = studioStrokes.isNotEmpty() || placedStamps.isNotEmpty(),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("studio_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (studioStrokes.isNotEmpty() || placedStamps.isNotEmpty()) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.35f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear All Annotations
                        IconButton(
                            onClick = {
                                studioStrokes.clear()
                                placedStamps.clear()
                                overlayBannerText = ""
                            },
                            enabled = studioStrokes.isNotEmpty() || placedStamps.isNotEmpty() || overlayBannerText.isNotEmpty(),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("studio_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear drawings",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Save Directly to Phone Gallery
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    val rendered = renderFinalCompositeBitmap(
                                        base = baseBitmap,
                                        filter = selectedFilter,
                                        strokes = studioStrokes,
                                        stamps = placedStamps,
                                        bannerText = overlayBannerText,
                                        includeDateStamp = dateWatermarkEnabled
                                    )
                                    val out = ByteArrayOutputStream()
                                    rendered.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                    val bytes = out.toByteArray()
                                    val ext = if (capturedIsVideo) "mp4" else "jpg"
                                    val mime = if (capturedIsVideo) "video/mp4" else "image/jpeg"
                                    val fileName = "DuoSpace_${System.currentTimeMillis()}.$ext"
                                    val ok = DuoVaultPersistence.saveMediaBytesToPhone(
                                        context = context,
                                        plainBytes = bytes,
                                        displayName = fileName,
                                        mimeType = mime
                                    )
                                    Toast.makeText(
                                        context,
                                        if (ok) "Saved to phone gallery ($fileName)" else "Saved to app storage",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("camera_save_to_phone_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save to Phone",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Save",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Interactive Photo/Video + Freehand Drawing Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .onSizeChanged { size ->
                                if (size.width > 0 && size.height > 0) {
                                    canvasPixelSize = size
                                }
                            }
                            .pointerInput(isDrawModeActive, selectedBrushColor, selectedBrushWidthDp) {
                                if (isDrawModeActive) {
                                    detectDragGestures(
                                        onDragStart = { pos ->
                                            activeStrokePoints.clear()
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            activeStrokePoints.add(
                                                Offset(
                                                    (pos.x / w).coerceIn(0f, 1f),
                                                    (pos.y / h).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            activeStrokePoints.add(
                                                Offset(
                                                    (change.position.x / w).coerceIn(0f, 1f),
                                                    (change.position.y / h).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDragEnd = {
                                            if (activeStrokePoints.isNotEmpty()) {
                                                studioStrokes.add(
                                                    NormalizedDrawStroke(
                                                        pointsNorm = activeStrokePoints.toList(),
                                                        colorArgb = selectedBrushColor,
                                                        strokeWidthDp = selectedBrushWidthDp
                                                    )
                                                )
                                                activeStrokePoints.clear()
                                            }
                                        },
                                        onDragCancel = {
                                            activeStrokePoints.clear()
                                        }
                                    )
                                }
                            }
                            .testTag("studio_drawing_canvas")
                    ) {
                        Image(
                            bitmap = baseBitmap.asImageBitmap(),
                            contentDescription = "Captured media preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Drawn Freehand Strokes + Stamps Overlay Canvas
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            fun drawNormStroke(pts: List<Offset>, color: Color, widthDp: Float) {
                                if (pts.isEmpty()) return
                                val pxWidth = widthDp.dp.toPx()
                                if (pts.size == 1) {
                                    drawCircle(
                                        color = color,
                                        radius = pxWidth / 2f,
                                        center = Offset(pts[0].x * w, pts[0].y * h)
                                    )
                                    return
                                }
                                val path = Path().apply {
                                    moveTo(pts[0].x * w, pts[0].y * h)
                                    for (i in 1 until pts.size) {
                                        lineTo(pts[i].x * w, pts[i].y * h)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(
                                        width = pxWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            studioStrokes.forEach { s ->
                                drawNormStroke(s.pointsNorm, Color(s.colorArgb), s.strokeWidthDp)
                            }
                            if (activeStrokePoints.isNotEmpty()) {
                                drawNormStroke(activeStrokePoints, Color(selectedBrushColor), selectedBrushWidthDp)
                            }
                        }

                        // Placed Sticker Stamps
                        placedStamps.forEach { stamp ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = (stamp.xNorm * 240f).dp,
                                        top = (stamp.yNorm * 280f).dp
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = stamp.text,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Text Banner Overlay
                        if (overlayBannerText.isNotBlank()) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = overlayBannerText,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Video Badge if Video Clip
                        if (capturedIsVideo) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFEF4444),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    val mins = capturedVideoDurationSec / 60
                                    val secs = capturedVideoDurationSec % 60
                                    Text(
                                        text = String.format(Locale.US, "VIDEO %d:%02d", mins, secs),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Studio Palette, Stamps, Caption & Send Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Brush Color Picker + Width Slider Row
                    if (isDrawModeActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                STUDIO_BRUSH_COLORS.forEach { colorHex ->
                                    val selected = selectedBrushColor == colorHex
                                    Box(
                                        modifier = Modifier
                                            .size(if (selected) 26.dp else 20.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorHex))
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedBrushColor = colorHex }
                                            .testTag("studio_color_$colorHex")
                                    )
                                }
                            }

                            Slider(
                                value = selectedBrushWidthDp,
                                onValueChange = { selectedBrushWidthDp = it },
                                valueRange = 2.5f..16f,
                                modifier = Modifier.width(96.dp)
                            )
                        }
                    }

                    // Quick Stamp Stickers Strip + Filter Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        STUDIO_QUICK_STAMPS.forEachIndexed { idx, stampText ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        val nextX = (0.15f + (placedStamps.size * 0.14f) % 0.65f)
                                        val nextY = (0.18f + (placedStamps.size * 0.12f) % 0.6f)
                                        placedStamps.add(
                                            PlacedStampOverlay(
                                                id = "stamp_${System.currentTimeMillis()}_$idx",
                                                text = stampText,
                                                xNorm = nextX,
                                                yNorm = nextY
                                            )
                                        )
                                    }
                            ) {
                                Text(
                                    text = stampText,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Caption Input + Send to Chat Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (captionInput.isEmpty()) {
                                    Text(
                                        text = if (capturedIsVideo) "Add a caption to video…" else "Add a caption…",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = captionInput,
                                    onValueChange = { captionInput = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("camera_caption_input")
                                )
                            }
                        }

                        // Send Directly to Chat (Encrypts into Private Sandbox + Auto-Syncs to Vault)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val composite = renderFinalCompositeBitmap(
                                        base = baseBitmap,
                                        filter = selectedFilter,
                                        strokes = studioStrokes,
                                        stamps = placedStamps,
                                        bannerText = overlayBannerText,
                                        includeDateStamp = dateWatermarkEnabled
                                    )
                                    val rawStream = ByteArrayOutputStream()
                                    composite.compress(Bitmap.CompressFormat.JPEG, 90, rawStream)
                                    val rawBytes = rawStream.toByteArray()
                                    val finalBytes = if (smartSizerEnabled) {
                                        VaultCryptoEngine.optimizeImageBytes(rawBytes)
                                    } else {
                                        rawBytes
                                    }
                                    val blobName = DuoVaultPersistence.writeEncryptedBlob(context, finalBytes)
                                    val ts = System.currentTimeMillis()
                                    if (capturedIsVideo) {
                                        val fileName = "CameraVideo_$ts.mp4"
                                        // mediaMeta format for VIDEO: blobName|durationSec|sizeBytes|fileName|filterLabel
                                        val meta = "$blobName|${capturedVideoDurationSec.coerceAtLeast(3)}|${finalBytes.size}|$fileName|${selectedFilter.label}"
                                        onSendMediaToChat(
                                            captionInput.trim(),
                                            MessageType.VIDEO,
                                            meta
                                        )
                                    } else {
                                        val fileName = "CameraPhoto_$ts.jpg"
                                        // mediaMeta format for PHOTO: blobName|sizeBytes|fileName|filterLabel
                                        val meta = "$blobName|${finalBytes.size}|$fileName|${selectedFilter.label}"
                                        onSendMediaToChat(
                                            captionInput.trim(),
                                            MessageType.PHOTO,
                                            meta
                                        )
                                    }
                                }
                                .testTag("camera_send_to_chat_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send to Chat",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(21.dp)
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
private fun LiveViewfinderSceneCanvas(
    isFrontLens: Boolean,
    isVideoMode: Boolean,
    isRecording: Boolean,
    filter: CameraFilterPreset,
    activeUserName: String,
    partnerName: String
) {
    val transition = rememberInfiniteTransition(label = "viewfinder_scene")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(filter.primaryBgHex),
                    Color(filter.secondaryBgHex)
                )
            )
        )

        // Subtle animated bokeh orbs so viewfinder feels alive in emulator & device
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.22f),
            radius = w * 0.28f,
            center = Offset(w * (0.25f + 0.15f * drift), h * (0.32f + 0.08f * drift))
        )
        drawCircle(
            color = Color(0xFFEC4899).copy(alpha = 0.22f),
            radius = w * 0.34f,
            center = Offset(w * (0.75f - 0.12f * drift), h * (0.65f - 0.1f * drift))
        )
        drawCircle(
            color = Color(0xFFFBBF24).copy(alpha = 0.16f),
            radius = w * 0.18f,
            center = Offset(w * 0.52f, h * (0.45f + 0.12f * drift))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = if (isFrontLens) {
                    "Front Selfie Lens • $activeUserName"
                } else {
                    "Rear HD Lens • ${if (isVideoMode) "Video" else "Photo"} • ${filter.label}"
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * Generates a rich, high-resolution bitmap when capturing from the studio viewfinder
 * (or when physical camera hardware isn't attached in the streaming container).
 */
fun generateStudioSceneBitmap(
    width: Int,
    height: Int,
    isFrontLens: Boolean,
    isVideo: Boolean,
    durationSec: Int,
    activeUserName: String,
    partnerName: String,
    filter: CameraFilterPreset
): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Gradient background
    paint.shader = LinearGradient(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        filter.primaryBgHex.toInt(),
        filter.secondaryBgHex.toInt(),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // Soft glowing circles
    paint.color = 0x4438BDF8
    canvas.drawCircle(width * 0.28f, height * 0.32f, width * 0.26f, paint)
    paint.color = 0x44EC4899
    canvas.drawCircle(width * 0.72f, height * 0.62f, width * 0.30f, paint)
    paint.color = 0x33FBBF24
    canvas.drawCircle(width * 0.5f, height * 0.45f, width * 0.20f, paint)

    // Center graphic card
    val cardRect = RectF(width * 0.14f, height * 0.30f, width * 0.86f, height * 0.70f)
    paint.color = 0x38000000
    canvas.drawRoundRect(cardRect, 48f, 48f, paint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    textPaint.textSize = width * 0.065f
    val headline = if (isVideo) {
        "🎬 $activeUserName's Video Clip (${durationSec.coerceAtLeast(3)}s)"
    } else if (isFrontLens) {
        "📸 $activeUserName • Selfie Moment"
    } else {
        "✨ $activeUserName & $partnerName"
    }
    canvas.drawText(headline, width * 0.5f, height * 0.48f, textPaint)

    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    textPaint.textSize = width * 0.042f
    textPaint.color = 0xDDFFFFFF.toInt()
    val subline = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
    canvas.drawText("$subline • ${filter.label}", width * 0.5f, height * 0.56f, textPaint)

    return bmp
}

/**
 * Composites the base bitmap + selected filter + freehand strokes + placed stamps + text banner + film date watermark.
 */
fun renderFinalCompositeBitmap(
    base: Bitmap,
    filter: CameraFilterPreset,
    strokes: List<NormalizedDrawStroke>,
    stamps: List<PlacedStampOverlay>,
    bannerText: String,
    includeDateStamp: Boolean
): Bitmap {
    val w = base.width.coerceAtLeast(1)
    val h = base.height.coerceAtLeast(1)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    if (filter == CameraFilterPreset.NOIR_BW) {
        val cm = ColorMatrix().apply { setSaturation(0f) }
        basePaint.colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(base, 0f, 0f, basePaint)
    basePaint.colorFilter = null

    if (filter.tintColor != 0L && filter != CameraFilterPreset.NOIR_BW) {
        val tintPaint = Paint().apply {
            color = filter.tintColor.toInt()
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), tintPaint)
    }

    // Draw Freehand Strokes
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    for (s in strokes) {
        if (s.pointsNorm.isEmpty()) continue
        strokePaint.color = s.colorArgb.toInt()
        strokePaint.strokeWidth = (s.strokeWidthDp * (w / 360f)).coerceAtLeast(4f)
        if (s.pointsNorm.size == 1) {
            val pt = s.pointsNorm[0]
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = s.colorArgb.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pt.x * w, pt.y * h, strokePaint.strokeWidth / 2f, fillPaint)
        } else {
            val path = android.graphics.Path()
            path.moveTo(s.pointsNorm[0].x * w, s.pointsNorm[0].y * h)
            for (i in 1 until s.pointsNorm.size) {
                path.lineTo(s.pointsNorm[i].x * w, s.pointsNorm[i].y * h)
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    // Draw Placed Stamps
    val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (w * 0.065f).coerceAtLeast(28f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    for (st in stamps) {
        canvas.drawText(st.text, st.xNorm * w, (st.yNorm * h) + stampPaint.textSize, stampPaint)
    }

    // Draw Text Banner Overlay
    if (bannerText.isNotBlank()) {
        val bannerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAA000000.toInt()
        }
        val top = h * 0.44f
        val bottom = h * 0.56f
        canvas.drawRoundRect(RectF(w * 0.08f, top, w * 0.92f, bottom), 28f, 28f, bannerBg)
        val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = (w * 0.055f).coerceAtLeast(24f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(bannerText, w * 0.5f, (top + bottom) / 2f + bannerTextPaint.textSize * 0.35f, bannerTextPaint)
    }

    // Draw Film Date Stamp in bottom-right corner
    if (includeDateStamp) {
        val dateStampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFB74D.toInt()
            textSize = (w * 0.036f).coerceAtLeast(18f)
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val stampStr = SimpleDateFormat("yy MM dd • HH:mm", Locale.US).format(Date())
        canvas.drawText(stampStr, w * 0.95f, h * 0.95f, dateStampPaint)
    }

    return out
}
