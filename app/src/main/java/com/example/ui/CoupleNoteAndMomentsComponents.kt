package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.local.CoupleNoteBoard
import com.example.data.local.CoupleNoteImage
import com.example.data.local.CoupleNotePersistence
import com.example.data.local.CoupleNoteTextBox
import com.example.data.local.MessageEntity
import com.example.data.local.MonthlyMomentItem
import com.example.data.local.MonthlyMomentsPersistence
import com.example.data.local.SpaceEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun DecryptedBlobImage(
    blobName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap: androidx.compose.ui.graphics.ImageBitmap? = remember(blobName) {
        val bytes: ByteArray? = DuoVaultPersistence.readDecryptedBlobInMemory(context, blobName)
        if (bytes != null && bytes.isNotEmpty()) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// Helper for partial/segment stroke erasing so rubbing the eraser removes touched segments smoothly
fun eraseStrokesNearPoint(
    strokes: MutableList<DoodleStroke>,
    targetPtDp: Offset,
    eraseRadiusDp: Float
): Boolean {
    val rSq = eraseRadiusDp * eraseRadiusDp
    var modified = false
    val result = mutableListOf<DoodleStroke>()
    for (stroke in strokes) {
        val anyHit = stroke.pointsDp.any { pt ->
            val dx = pt.x - targetPtDp.x
            val dy = pt.y - targetPtDp.y
            (dx * dx + dy * dy) <= rSq
        }
        if (!anyHit) {
            result.add(stroke)
        } else {
            modified = true
            var currentSegment = mutableListOf<Offset>()
            for (pt in stroke.pointsDp) {
                val dx = pt.x - targetPtDp.x
                val dy = pt.y - targetPtDp.y
                if ((dx * dx + dy * dy) > rSq) {
                    currentSegment.add(pt)
                } else {
                    if (currentSegment.size >= 2) {
                        result.add(stroke.copy(pointsDp = currentSegment.toList()))
                    }
                    currentSegment = mutableListOf()
                }
            }
            if (currentSegment.size >= 2) {
                result.add(stroke.copy(pointsDp = currentSegment.toList()))
            }
        }
    }
    if (modified) {
        strokes.clear()
        strokes.addAll(result)
    }
    return modified
}

// --- Couple's Interactive Note Box Component ---

@Composable
fun CoupleNoteBoxView(
    activeUser: TestUserSlot,
    spaceCode: String,
    user1Name: String,
    user2Name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val partnerSlot = if (activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
    val partnerName = if (activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name

    // By default, you see THEIR note left for you!
    // Flipping/rotating shows the note YOU left for them.
    var isViewingMyNote by remember { mutableStateOf(false) }

    // Loaded notes
    var partnerNote by remember(spaceCode, activeUser, isViewingMyNote) {
        mutableStateOf(CoupleNotePersistence.loadNote(context, spaceCode, partnerSlot, partnerName))
    }
    var myNote by remember(spaceCode, activeUser, isViewingMyNote) {
        mutableStateOf(CoupleNotePersistence.loadNote(context, spaceCode, activeUser, activeUserName))
    }

    val currentNote = if (isViewingMyNote) myNote else partnerNote

    // Flip rotation animation
    var flipRotationTarget by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = flipRotationTarget,
        animationSpec = tween(durationMillis = 420),
        label = "note_flip_anim"
    )

    // In "My Note", creation dialogs & transformation states
    var showAddTextDialog by remember { mutableStateOf(false) }
    var editingTextBoxId by remember { mutableStateOf<String?>(null) }
    var newTextInput by remember { mutableStateOf("") }
    var selectedNoteColor by remember { mutableStateOf(0x00000000L) } // Default Transparent
    var selectedTextColor by remember { mutableStateOf(0xFFFFFFFFL) } // Default White text
    var isDoodlingOnNote by remember { mutableStateOf(false) }
    var isNoteEraserMode by remember { mutableStateOf(false) }
    var isTransformingDoodle by remember { mutableStateOf(false) }
    var selectedDoodleColorHex by remember { mutableStateOf(0xFFEC4899L) }
    var selectedDoodleStrokeWidth by remember { mutableFloatStateOf(4.5f) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    val noteDoodleStrokes = remember { mutableStateListOf<DoodleStroke>() }
    val activeDoodleStroke = remember { mutableStateListOf<Offset>() }
    var showCleanConfirmDialog by remember { mutableStateOf(false) }
    var canvasWidthDp by remember { mutableFloatStateOf(340f) }
    var canvasHeightDp by remember { mutableFloatStateOf(310f) }

    val stickyPalette = remember {
        listOf(
            0x00000000L, // Transparent (Default / Normal centered text)
            0xFFFFF3B0L, // Warm Butter
            0xFFFFD6E0L, // Soft Rose
            0xFFC8F7DCL, // Mint Sage
            0xFFD0EBFFL, // Sky Mist
            0xFFE5DBFFL, // Lavender
            0xFFFFE5D9L  // Peach Cream
        )
    }

    val textColorPalette = remember {
        listOf(
            0xFFFFFFFFL, // Pure White
            0xFFEC4899L, // Vibrant Pink
            0xFF8B5CF6L, // Bright Purple
            0xFF0EA5E9L, // Sky Blue
            0xFF10B981L, // Mint Green
            0xFFF59E0BL, // Golden Amber
            0xFF0F172AL  // Charcoal
        )
    }

    val doodlePenPalette = remember {
        listOf(
            0xFFEC4899L, // Rose Pink
            0xFF8B5CF6L, // Violet
            0xFF0EA5E9L, // Ocean Blue
            0xFF10B981L, // Emerald
            0xFFF59E0BL, // Amber
            0xFFEF4444L, // Coral Red
            0xFFFFFFFFL  // Pure White Ink
        )
    }

    LaunchedEffect(myNote.doodleStrokes) {
        noteDoodleStrokes.clear()
        noteDoodleStrokes.addAll(myNote.doodleStrokes)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (raw != null) {
                    val optimized = VaultCryptoEngine.optimizeImageBytes(raw)
                    val blobName = DuoVaultPersistence.writeEncryptedBlob(context, optimized)
                    val newImg = CoupleNoteImage(
                        id = UUID.randomUUID().toString(),
                        imageBlobOrUri = blobName,
                        xOffsetDp = 36f + (myNote.images.size * 28f) % 140f,
                        yOffsetDp = 36f + (myNote.images.size * 22f) % 95f,
                        widthDp = 104f,
                        heightDp = 104f,
                        rotationDeg = 0f
                    )
                    val updatedImages = myNote.images + newImg
                    val updatedNote = myNote.copy(images = updatedImages, lastUpdatedAtMs = System.currentTimeMillis())
                    myNote = updatedNote
                    selectedItemId = newImg.id
                    CoupleNotePersistence.saveNote(context, spaceCode, updatedNote)
                    Toast.makeText(context, "Photo added! Pinch to zoom/rotate, or drag out of note to delete.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag("couple_note_box_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- DISTINCT CANVAS SURFACE (Visually distinct from the #141416 Chat Background) ---
        val dotColor = Color(0xFFA5B4FC).copy(alpha = 0.16f)
        val boardBgColor = if (isViewingMyNote) {
            Color(0xFF282438) // Warm Twilight Plum-Slate for My Note
        } else {
            Color(0xFF212738) // Deep Midnight Indigo-Slate for Partner's Note
        }
        val boardBorderColor = if (isViewingMyNote) {
            Color(0xFF4C446B)
        } else {
            Color(0xFF3B4561)
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = boardBgColor,
            border = BorderStroke(1.2.dp, boardBorderColor),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(22.dp))
                .onSizeChanged { sz ->
                    if (sz.width > 0 && sz.height > 0) {
                        canvasWidthDp = sz.width / density.density
                        canvasHeightDp = sz.height / density.density
                    }
                }
                .testTag("note_canvas_surface")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = isViewingMyNote && !isDoodlingOnNote) {
                        selectedItemId = null
                    }
            ) {
                // Subtle architectural dot-grid texture
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepPx = 18.dp.toPx()
                    val dotRadius = 1.1.dp.toPx()
                    var x = stepPx
                    while (x < size.width) {
                        var y = stepPx
                        while (y < size.height) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += stepPx
                        }
                        x += stepPx
                    }
                }

                // Subtle Corner Controls: Clean note button (only in My Note) & Rotate/Flip button
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isViewingMyNote) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { showCleanConfirmDialog = true }
                                .testTag("clean_my_note_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clean note",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                flipRotationTarget += 180f
                                isViewingMyNote = !isViewingMyNote
                                isDoodlingOnNote = false
                                isNoteEraserMode = false
                                isTransformingDoodle = false
                                selectedItemId = null
                            }
                            .testTag("rotate_note_box_symbol")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Rotate note to flip side",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(17.dp)
                                    .rotate(animatedRotation)
                            )
                        }
                    }
                }

                // Blank canvas tranquil notice when empty
                if (currentNote.textItems.isEmpty() && currentNote.images.isEmpty() && currentNote.doodleStrokes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isViewingMyNote) "Blank note • Add text, photo, or doodle below" else "Blank note from $partnerName",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.48f)
                        )
                    }
                }

                // 1. Render Placed Images (Frameless, Unlimited Zoom, Two-Finger Rotate, Drag Out of Note to Delete)
                val imagesToRender = if (isViewingMyNote) myNote.images else currentNote.images
                imagesToRender.forEach { img ->
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (img.xOffsetDp * density.density).roundToInt(),
                                    (img.yOffsetDp * density.density).roundToInt()
                                )
                            }
                            .size(width = img.widthDp.dp, height = img.heightDp.dp)
                            .graphicsLayer {
                                rotationZ = img.rotationDeg
                            }
                            .then(
                                if (isViewingMyNote && !isDoodlingOnNote) {
                                    Modifier.pointerInput(img.id, canvasWidthDp, canvasHeightDp) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            val currentImg = myNote.images.firstOrNull { it.id == img.id } ?: return@detectTransformGestures
                                            val newW = (currentImg.widthDp * zoom).coerceAtLeast(24f)
                                            val newH = (currentImg.heightDp * zoom).coerceAtLeast(24f)
                                            val newRot = (currentImg.rotationDeg + rotation) % 360f
                                            val newX = currentImg.xOffsetDp + pan.x / density.density
                                            val newY = currentImg.yOffsetDp + pan.y / density.density

                                            val centerX = newX + newW * 0.5f
                                            val centerY = newY + newH * 0.5f
                                            val isDraggedOutside = centerX < -12f ||
                                                centerX > canvasWidthDp + 12f ||
                                                centerY < -12f ||
                                                centerY > canvasHeightDp + 12f

                                            if (isDraggedOutside) {
                                                val updated = myNote.copy(
                                                    images = myNote.images.filterNot { it.id == img.id },
                                                    lastUpdatedAtMs = System.currentTimeMillis()
                                                )
                                                myNote = updated
                                                CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                            } else {
                                                val updated = myNote.copy(
                                                    images = myNote.images.map {
                                                        if (it.id == img.id) {
                                                            it.copy(
                                                                xOffsetDp = newX,
                                                                yOffsetDp = newY,
                                                                widthDp = newW,
                                                                heightDp = newH,
                                                                rotationDeg = newRot
                                                            )
                                                        } else it
                                                    },
                                                    lastUpdatedAtMs = System.currentTimeMillis()
                                                )
                                                myNote = updated
                                                CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                            }
                                        }
                                    }
                                } else Modifier
                            )
                    ) {
                        DecryptedBlobImage(
                            blobName = img.imageBlobOrUri,
                            contentDescription = "Note attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }

                // 2. Render Placed Text Items (Frameless — NO box/border, Unlimited Zoom, Two-Finger Rotate, Drag Out of Note to Delete)
                val textsToRender = if (isViewingMyNote) myNote.textItems else currentNote.textItems
                textsToRender.forEach { textBox ->
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (textBox.xOffsetDp * density.density).roundToInt(),
                                    (textBox.yOffsetDp * density.density).roundToInt()
                                )
                            }
                            .size(width = textBox.widthDp.dp, height = textBox.heightDp.dp)
                            .graphicsLayer {
                                rotationZ = textBox.rotationDeg
                            }
                            .then(
                                if (isViewingMyNote && !isDoodlingOnNote) {
                                    Modifier.pointerInput(textBox.id, canvasWidthDp, canvasHeightDp) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            val currentBox = myNote.textItems.firstOrNull { it.id == textBox.id } ?: return@detectTransformGestures
                                            val newW = (currentBox.widthDp * zoom).coerceAtLeast(36f)
                                            val newH = (currentBox.heightDp * zoom).coerceAtLeast(24f)
                                            val newFontSize = (currentBox.fontSizeSp * zoom).coerceAtLeast(6f)
                                            val newRot = (currentBox.rotationDeg + rotation) % 360f
                                            val newX = currentBox.xOffsetDp + pan.x / density.density
                                            val newY = currentBox.yOffsetDp + pan.y / density.density

                                            val centerX = newX + newW * 0.5f
                                            val centerY = newY + newH * 0.5f
                                            val isDraggedOutside = centerX < -12f ||
                                                centerX > canvasWidthDp + 12f ||
                                                centerY < -12f ||
                                                centerY > canvasHeightDp + 12f

                                            if (isDraggedOutside) {
                                                val updated = myNote.copy(
                                                    textItems = myNote.textItems.filterNot { it.id == textBox.id },
                                                    lastUpdatedAtMs = System.currentTimeMillis()
                                                )
                                                myNote = updated
                                                CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                            } else {
                                                val updated = myNote.copy(
                                                    textItems = myNote.textItems.map {
                                                        if (it.id == textBox.id) {
                                                            it.copy(
                                                                xOffsetDp = newX,
                                                                yOffsetDp = newY,
                                                                widthDp = newW,
                                                                heightDp = newH,
                                                                fontSizeSp = newFontSize,
                                                                rotationDeg = newRot
                                                            )
                                                        } else it
                                                    },
                                                    lastUpdatedAtMs = System.currentTimeMillis()
                                                )
                                                myNote = updated
                                                CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                            }
                                        }
                                    }
                                } else Modifier
                            )
                            .clickable(enabled = isViewingMyNote && !isDoodlingOnNote) {
                                editingTextBoxId = textBox.id
                                newTextInput = textBox.text
                                selectedNoteColor = textBox.colorArgb
                                selectedTextColor = textBox.textColorArgb
                                showAddTextDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = textBox.text,
                            fontSize = textBox.fontSizeSp.sp,
                            lineHeight = (textBox.fontSizeSp * 1.22f).sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color(textBox.textColorArgb)
                        )
                    }
                }

                // 3. Render Doodles on Note (With Translate & Scale for Movable & Resizable Doodle)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeList = if (isViewingMyNote) noteDoodleStrokes else currentNote.doodleStrokes
                    val offX = if (isViewingMyNote) myNote.doodleOffsetXDp else currentNote.doodleOffsetXDp
                    val offY = if (isViewingMyNote) myNote.doodleOffsetYDp else currentNote.doodleOffsetYDp
                    val scaleFactor = if (isViewingMyNote) myNote.doodleScale else currentNote.doodleScale

                    drawContext.canvas.save()
                    drawContext.canvas.translate(offX * density.density, offY * density.density)
                    drawContext.canvas.translate(size.width / 2f, size.height / 2f)
                    drawContext.canvas.scale(scaleFactor, scaleFactor)
                    drawContext.canvas.translate(-size.width / 2f, -size.height / 2f)

                    strokeList.forEach { stroke ->
                        if (stroke.pointsDp.size > 1) {
                            val path = Path()
                            val first = stroke.pointsDp[0]
                            path.moveTo(first.x * density.density, first.y * density.density)
                            for (i in 1 until stroke.pointsDp.size) {
                                val pt = stroke.pointsDp[i]
                                path.lineTo(pt.x * density.density, pt.y * density.density)
                            }
                            drawPath(
                                path = path,
                                color = Color(stroke.colorArgb),
                                style = Stroke(
                                    width = stroke.strokeWidthDp * density.density,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Active live stroke while drawing on note
                    if (isDoodlingOnNote && !isNoteEraserMode && activeDoodleStroke.size > 1) {
                        val path = Path()
                        path.moveTo(activeDoodleStroke[0].x * density.density, activeDoodleStroke[0].y * density.density)
                        for (i in 1 until activeDoodleStroke.size) {
                            path.lineTo(activeDoodleStroke[i].x * density.density, activeDoodleStroke[i].y * density.density)
                        }
                        drawPath(
                            path = path,
                            color = Color(selectedDoodleColorHex),
                            style = Stroke(
                                width = selectedDoodleStrokeWidth * density.density,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    drawContext.canvas.restore()
                }

                // Doodle Touch Handler (Drawing & Eraser mode)
                if (isViewingMyNote && isDoodlingOnNote && !isTransformingDoodle) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedDoodleColorHex, selectedDoodleStrokeWidth, isNoteEraserMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        activeDoodleStroke.clear()
                                        val ptDp = with(density) { Offset(offset.x.toDp().value, offset.y.toDp().value) }
                                        if (isNoteEraserMode) {
                                            val eraseRadius = (selectedDoodleStrokeWidth * 1.8f + 10f)
                                            if (eraseStrokesNearPoint(noteDoodleStrokes, ptDp, eraseRadius)) {
                                                val updatedNote = myNote.copy(
                                                    doodleStrokes = noteDoodleStrokes.toList(),
                                                    lastUpdatedAtMs = System.currentTimeMillis()
                                                )
                                                myNote = updatedNote
                                                CoupleNotePersistence.saveNote(context, spaceCode, updatedNote)
                                            }
                                        } else {
                                            activeDoodleStroke.add(ptDp)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val ptDp = with(density) { Offset(change.position.x.toDp().value, change.position.y.toDp().value) }
                                        if (isNoteEraserMode) {
                                            val eraseRadius = (selectedDoodleStrokeWidth * 1.8f + 10f)
                                            if (eraseStrokesNearPoint(noteDoodleStrokes, ptDp, eraseRadius)) {
                                                val updatedNote = myNote.copy(
                                                    doodleStrokes = noteDoodleStrokes.toList(),
                                                    lastUpdatedAtMs = System.currentTimeMillis()
                                                )
                                                myNote = updatedNote
                                                CoupleNotePersistence.saveNote(context, spaceCode, updatedNote)
                                            }
                                        } else {
                                            activeDoodleStroke.add(ptDp)
                                        }
                                    },
                                    onDragEnd = {
                                        if (!isNoteEraserMode && activeDoodleStroke.isNotEmpty()) {
                                            val newStroke = DoodleStroke(
                                                pointsDp = activeDoodleStroke.toList(),
                                                colorArgb = selectedDoodleColorHex,
                                                strokeWidthDp = selectedDoodleStrokeWidth
                                            )
                                            noteDoodleStrokes.add(newStroke)
                                            activeDoodleStroke.clear()
                                            val updatedNote = myNote.copy(
                                                doodleStrokes = noteDoodleStrokes.toList(),
                                                lastUpdatedAtMs = System.currentTimeMillis()
                                            )
                                            myNote = updatedNote
                                            CoupleNotePersistence.saveNote(context, spaceCode, updatedNote)
                                        }
                                    },
                                    onDragCancel = {
                                        activeDoodleStroke.clear()
                                    }
                                )
                            }
                    )
                }

                // Doodle Move & Resize Overlay Frame (When isTransformingDoodle == true)
                if (isViewingMyNote && isTransformingDoodle && noteDoodleStrokes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .border(
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                                RoundedCornerShape(16.dp)
                            )
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    ) {
                        // Top Pill with scale info & zoom controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Art Scale ${(myNote.doodleScale * 100).toInt()}% • Drag center",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val newScale = (myNote.doodleScale - 0.15f).coerceIn(0.35f, 3f)
                                            val updated = myNote.copy(doodleScale = newScale)
                                            myNote = updated
                                            CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Shrink doodle", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val newScale = (myNote.doodleScale + 0.15f).coerceIn(0.35f, 3f)
                                            val updated = myNote.copy(doodleScale = newScale)
                                            myNote = updated
                                            CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Enlarge doodle", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Button(
                                    onClick = { isTransformingDoodle = false },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Center Drag Move Handle for Doodle
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newX = (myNote.doodleOffsetXDp + dragAmount.x / density.density).coerceIn(-140f, 140f)
                                            val newY = (myNote.doodleOffsetYDp + dragAmount.y / density.density).coerceIn(-110f, 110f)
                                            myNote = myNote.copy(doodleOffsetXDp = newX, doodleOffsetYDp = newY)
                                        },
                                        onDragEnd = {
                                            CoupleNotePersistence.saveNote(context, spaceCode, myNote)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = "Drag to move doodle across note board",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Corner Resize Handle for Doodle
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 5.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val delta = (dragAmount.x + dragAmount.y) / 100f
                                            val newScale = (myNote.doodleScale + delta).coerceIn(0.35f, 3.0f)
                                            myNote = myNote.copy(doodleScale = newScale)
                                        },
                                        onDragEnd = {
                                            CoupleNotePersistence.saveNote(context, spaceCode, myNote)
                                        }
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Drag to resize doodle",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- INLINE DOODLE PALETTE + BRUSH SIZE SLIDER + ERASER BAR (Visible while drawing on note) ---
        AnimatedVisibility(visible = isViewingMyNote && isDoodlingOnNote) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            doodlePenPalette.forEach { colorHex ->
                                val isChosen = !isNoteEraserMode && selectedDoodleColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(if (isChosen) 24.dp else 19.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(
                                            width = if (isChosen) 2.5.dp else 1.dp,
                                            color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.7f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedDoodleColorHex = colorHex
                                            isNoteEraserMode = false
                                        }
                                )
                            }

                            // Eraser Toggle Button for Note Doodle
                            Surface(
                                shape = CircleShape,
                                color = if (isNoteEraserMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                                border = if (isNoteEraserMode) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { isNoteEraserMode = !isNoteEraserMode }
                                    .testTag("note_doodle_eraser_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = "Note Eraser",
                                        tint = if (isNoteEraserMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Undo last stroke
                            if (noteDoodleStrokes.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            noteDoodleStrokes.removeAt(noteDoodleStrokes.lastIndex)
                                            val updated = myNote.copy(doodleStrokes = noteDoodleStrokes.toList())
                                            myNote = updated
                                            CoupleNotePersistence.saveNote(context, spaceCode, updated)
                                        }
                                ) {
                                    Text(
                                        text = "Undo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        isDoodlingOnNote = false
                                        isNoteEraserMode = false
                                    }
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Controllable Brush / Eraser Size Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isNoteEraserMode) "Eraser Size" else "Brush Size",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = selectedDoodleStrokeWidth,
                            onValueChange = { selectedDoodleStrokeWidth = it },
                            valueRange = 1.5f..28f,
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("note_doodle_size_slider")
                        )
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(selectedDoodleStrokeWidth.coerceIn(3f, 24f).dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isNoteEraserMode) MaterialTheme.colorScheme.error
                                        else Color(selectedDoodleColorHex)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // --- MINIMAL ICON-ONLY ACTION BAR ---
        if (isViewingMyNote) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Text icon
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    editingTextBoxId = null
                                    newTextInput = ""
                                    selectedNoteColor = 0x00000000L
                                    showAddTextDialog = true
                                }
                                .testTag("note_add_text_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Add text box",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 2. Photo icon
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("note_add_photo_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 3. Doodle icon
                        Surface(
                            shape = CircleShape,
                            color = if (isDoodlingOnNote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isDoodlingOnNote = !isDoodlingOnNote
                                    if (isDoodlingOnNote) {
                                        isTransformingDoodle = false
                                        selectedItemId = null
                                    }
                                }
                                .testTag("note_toggle_doodle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gesture,
                                    contentDescription = "Doodle",
                                    tint = if (isDoodlingOnNote) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 4. Move/Resize Doodle icon
                        if (noteDoodleStrokes.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = if (isTransformingDoodle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        isTransformingDoodle = !isTransformingDoodle
                                        if (isTransformingDoodle) {
                                            isDoodlingOnNote = false
                                            selectedItemId = null
                                        }
                                    }
                                    .testTag("note_move_resize_doodle_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.OpenWith,
                                        contentDescription = "Move/Resize doodle",
                                        tint = if (isTransformingDoodle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Frameless Note Text Dialog
    if (showAddTextDialog) {
        val isEditing = editingTextBoxId != null
        AlertDialog(
            onDismissRequest = {
                showAddTextDialog = false
                editingTextBoxId = null
            },
            title = { Text(if (isEditing) "Edit Text" else "Add Text") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTextInput,
                        onValueChange = { newTextInput = it },
                        placeholder = { Text("Write a note, text or reminder…") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Text Color options
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Text Color",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            textColorPalette.forEach { colorHex ->
                                val isSelected = selectedTextColor == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedTextColor = colorHex }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Tip: Pinch with 2 fingers to resize or rotate freely. Drag outside the note to delete.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = newTextInput.trim()
                        if (text.isNotEmpty()) {
                            val editId = editingTextBoxId
                            if (editId != null) {
                                val updated = myNote.copy(
                                    textItems = myNote.textItems.map {
                                        if (it.id == editId) it.copy(text = text, colorArgb = 0L, textColorArgb = selectedTextColor) else it
                                    },
                                    lastUpdatedAtMs = System.currentTimeMillis()
                                )
                                myNote = updated
                                CoupleNotePersistence.saveNote(context, spaceCode, updated)
                            } else {
                                val newBox = CoupleNoteTextBox(
                                    id = UUID.randomUUID().toString(),
                                    text = text,
                                    xOffsetDp = 28f + (myNote.textItems.size * 26f) % 140f,
                                    yOffsetDp = 28f + (myNote.textItems.size * 24f) % 100f,
                                    widthDp = 160f,
                                    heightDp = 76f,
                                    fontSizeSp = 18f,
                                    colorArgb = 0L,
                                    textColorArgb = selectedTextColor,
                                    rotationDeg = 0f
                                )
                                val updated = myNote.copy(
                                    textItems = myNote.textItems + newBox,
                                    lastUpdatedAtMs = System.currentTimeMillis()
                                )
                                myNote = updated
                                selectedItemId = newBox.id
                                CoupleNotePersistence.saveNote(context, spaceCode, updated)
                            }
                        }
                        showAddTextDialog = false
                        editingTextBoxId = null
                    }
                ) {
                    Text(if (isEditing) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTextDialog = false
                        editingTextBoxId = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clean My Note Confirmation Dialog
    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            title = { Text("Clear Your Note Board?") },
            text = { Text("This will remove all sticky notes, Polaroids, and doodles from your board for $partnerName.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        noteDoodleStrokes.clear()
                        CoupleNotePersistence.clearNote(context, spaceCode, activeUser)
                        myNote = CoupleNotePersistence.loadNote(context, spaceCode, activeUser, activeUserName)
                        selectedItemId = null
                        isTransformingDoodle = false
                        showCleanConfirmDialog = false
                        Toast.makeText(context, "Your note board has been cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear Board")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Chat Tab Navigation Hub (3 Chats List + Rotating Note Box) ---

enum class SubChatDestination {
    MAIN_CHAT,
    MY_MONTHLY_MOMENTS,
    PARTNER_MONTHLY_MOMENTS
}

@Composable
fun HiveChatNavigationHub(
    activeUser: TestUserSlot,
    space: SpaceEntity?,
    messages: List<MessageEntity>,
    onSelectChat: (SubChatDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val spaceCode = space?.code ?: "HIVE01"
    val user1Name = space?.creatorName ?: "Alex"
    val user2Name = space?.partnerName ?: "Sam"
    val partnerSlot = if (activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
    val partnerName = if (activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name

    val partnerMomentsUnlocked = remember(spaceCode, partnerSlot) {
        MonthlyMomentsPersistence.isPartnerMomentsUnlocked(context, spaceCode, partnerSlot)
    }
    val myMomentsCount = remember(spaceCode, activeUser) {
        MonthlyMomentsPersistence.loadMoments(context, spaceCode, activeUser).size
    }
    val partnerMomentsCount = remember(spaceCode, partnerSlot) {
        MonthlyMomentsPersistence.loadMoments(context, spaceCode, partnerSlot).size
    }
    val daysUntilEndOfMonth = remember { MonthlyMomentsPersistence.daysUntilEndOfMonth() }

    var showLockedMomentsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141416))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 3 CHATS LIST ---
        Text(
            text = "CONVERSATIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Direct Conversation Tile with Partner ($partnerName)
        val lastMessage = messages.lastOrNull { !it.isSystemEvent }
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09090B)),
            border = BorderStroke(0.8.dp, Color(0xFF27272A)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectChat(SubChatDestination.MAIN_CHAT)
                }
                .testTag("chat_tile_main")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular surface matching partner's pfp avatar
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF27272A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = partnerName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = partnerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (lastMessage != null) {
                            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastMessage.createdAt))
                            Text(
                                text = timeStr,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = lastMessage?.content ?: "Everyday shared messages, voice & calls",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. My Monthly Moments Tile (Openable anytime)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09090B)),
            border = BorderStroke(0.8.dp, Color(0xFF27272A)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectChat(SubChatDestination.MY_MONTHLY_MOMENTS)
                }
                .testTag("chat_tile_my_moments")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF27272A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "My Monthly Moments",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Monthly Moments",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Prepare secret messages, photos, doodles & coupons",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 3. Partner's Monthly Moments Tile (Locked until Month-End, or Unlocked)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (partnerMomentsUnlocked) Color(0xFF09090B)
                else Color(0xFF111113)
            ),
            border = BorderStroke(
                0.8.dp,
                if (partnerMomentsUnlocked) Color(0xFF10B981).copy(alpha = 0.35f)
                else Color(0xFF27272A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (partnerMomentsUnlocked) {
                        onSelectChat(SubChatDestination.PARTNER_MONTHLY_MOMENTS)
                    } else {
                        showLockedMomentsDialog = true
                    }
                }
                .testTag("chat_tile_partner_moments")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF27272A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (partnerMomentsUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Partner Monthly Moments",
                            tint = if (partnerMomentsUnlocked) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$partnerName's Monthly Moments",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (partnerMomentsUnlocked) {
                            "Ready to open"
                        } else {
                            "Locked until month-end ($daysUntilEndOfMonth days left)"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (partnerMomentsUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (partnerMomentsUnlocked) Color(0xFF10B981) else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- COUPLE'S INTERACTIVE ROTATING NOTE BOX (Fills remaining space) ---
        Text(
            text = "SHARED NOTE BOARD",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CoupleNoteBoxView(
            activeUser = activeUser,
            spaceCode = spaceCode,
            user1Name = user1Name,
            user2Name = user2Name,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Locked Moments Dialog
    if (showLockedMomentsDialog) {
        AlertDialog(
            onDismissRequest = { showLockedMomentsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("$partnerName's Monthly Moments are Locked") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This chat is locked throughout the month while $partnerName prepares secret messages, photos, doodles, and coupons for you.\n\nIt will unlock automatically at the end of the month ($daysUntilEndOfMonth days remaining) for you to open!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Quick test unlock option so developer/user can verify unlock immediately
                        MonthlyMomentsPersistence.setTestUnlocked(context, spaceCode, partnerSlot, true)
                        showLockedMomentsDialog = false
                        onSelectChat(SubChatDestination.PARTNER_MONTHLY_MOMENTS)
                    }
                ) {
                    Text("Unlock for Testing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockedMomentsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// --- Monthly Moments Dedicated Conversation Screen ---

@Composable
fun MonthlyMomentsChatScreen(
    activeUser: TestUserSlot,
    space: SpaceEntity?,
    isViewingPartnerMoments: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spaceCode = space?.code ?: "HIVE01"
    val user1Name = space?.creatorName ?: "Alex"
    val user2Name = space?.partnerName ?: "Sam"
    val partnerSlot = if (activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
    val partnerName = if (activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name

    // The target slot whose moments we are looking at:
    // If isViewingPartnerMoments == true: we are looking at partnerSlot's moments for us.
    // If isViewingPartnerMoments == false: we are looking at activeUser's moments for partner.
    val targetSlot = if (isViewingPartnerMoments) partnerSlot else activeUser

    var momentsList by remember(spaceCode, targetSlot) {
        mutableStateOf(MonthlyMomentsPersistence.loadMoments(context, spaceCode, targetSlot))
    }

    var messageInput by remember { mutableStateOf("") }
    var showCouponDialog by remember { mutableStateOf(false) }
    var customCouponText by remember { mutableStateOf("") }
    var show3DotMenu by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (raw != null) {
                    val optimized = VaultCryptoEngine.optimizeImageBytes(raw)
                    val blobName = DuoVaultPersistence.writeEncryptedBlob(context, optimized)
                    val newMoment = MonthlyMomentItem(
                        senderId = activeUser.userId,
                        senderName = activeUserName,
                        content = "Shared a photo for end of month",
                        type = "IMAGE",
                        mediaUriOrBlob = blobName
                    )
                    MonthlyMomentsPersistence.saveMoment(context, spaceCode, activeUser, newMoment)
                    momentsList = MonthlyMomentsPersistence.loadMoments(context, spaceCode, activeUser)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chats")
                    }
                    Column {
                        Text(
                            text = if (isViewingPartnerMoments) "$partnerName's Monthly Moments" else "My Monthly Moments",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isViewingPartnerMoments) "Unwrapped moments for you" else "Secrets prepared for $partnerName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { show3DotMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = show3DotMenu,
                        onDismissRequest = { show3DotMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Clear Monthly Moments",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                show3DotMenu = false
                                showClearConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Moments Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (momentsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = if (isViewingPartnerMoments) "$partnerName hasn't added moments yet."
                                else "No moments added yet.\nSend messages, photos, and love coupons below!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(momentsList, key = { it.id }) { item ->
                val isMe = item.senderId == activeUser.userId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (item.type) {
                            "COUPON" -> Color(0xFFFDE047)
                            else -> if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        },
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(0.82f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Coupon Badge
                            if (item.type == "COUPON") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(16.dp))
                                    Text(text = "COUPLE COUPON", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF854D0E))
                                }
                            }

                            // Image if attached
                            if (item.type == "IMAGE" && !item.mediaUriOrBlob.isNullOrBlank()) {
                                DecryptedBlobImage(
                                    blobName = item.mediaUriOrBlob,
                                    contentDescription = "Monthly moment photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }

                            Text(
                                text = item.content,
                                fontSize = 14.sp,
                                color = when (item.type) {
                                    "COUPON" -> Color(0xFF1E293B)
                                    else -> if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            val timeStr = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(item.createdAtMs))
                            Text(
                                text = timeStr,
                                fontSize = 10.sp,
                                color = when (item.type) {
                                    "COUPON" -> Color(0xFF713F12).copy(alpha = 0.8f)
                                    else -> if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Area (Available in "My Monthly Moments" to prepare moments)
        if (!isViewingPartnerMoments) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Action Pills (+ Photo, + Coupon)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Photo", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = { showCouponDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDE047), contentColor = Color(0xFF854D0E)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Coupon", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Text Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Write a secret monthly moment…") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val text = messageInput.trim()
                                    if (text.isNotEmpty()) {
                                        val newMoment = MonthlyMomentItem(
                                            senderId = activeUser.userId,
                                            senderName = activeUserName,
                                            content = text,
                                            type = "TEXT"
                                        )
                                        MonthlyMomentsPersistence.saveMoment(context, spaceCode, activeUser, newMoment)
                                        momentsList = MonthlyMomentsPersistence.loadMoments(context, spaceCode, activeUser)
                                        messageInput = ""
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send moment",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Coupon Creation Dialog
    if (showCouponDialog) {
        val presetCoupons = listOf(
            "💆 30-Minute Massage Anytime",
            "🍽️ Dinner Date (Your Choice, On Me)",
            "🎬 Movie Night Pick with Unlimited Popcorn",
            "☕ Breakfast in Bed this Weekend",
            "🧺 Free Pass from 1 Chore"
        )
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text("Add Love Coupon") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose a preset coupon or write your own:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    presetCoupons.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newMoment = MonthlyMomentItem(
                                        senderId = activeUser.userId,
                                        senderName = activeUserName,
                                        content = preset,
                                        type = "COUPON"
                                    )
                                    MonthlyMomentsPersistence.saveMoment(context, spaceCode, activeUser, newMoment)
                                    momentsList = MonthlyMomentsPersistence.loadMoments(context, spaceCode, activeUser)
                                    showCouponDialog = false
                                }
                        ) {
                            Text(text = preset, fontSize = 12.5.sp, modifier = Modifier.padding(10.dp))
                        }
                    }

                    OutlinedTextField(
                        value = customCouponText,
                        onValueChange = { customCouponText = it },
                        placeholder = { Text("Or type a custom coupon…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = customCouponText.trim()
                        if (text.isNotEmpty()) {
                            val newMoment = MonthlyMomentItem(
                                senderId = activeUser.userId,
                                senderName = activeUserName,
                                content = "🎟️ $text",
                                type = "COUPON"
                            )
                            MonthlyMomentsPersistence.saveMoment(context, spaceCode, activeUser, newMoment)
                            momentsList = MonthlyMomentsPersistence.loadMoments(context, spaceCode, activeUser)
                        }
                        showCouponDialog = false
                    }
                ) {
                    Text("Add Custom Coupon")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCouponDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Monthly Moments Confirmation Dialog
    // Fulfilling: "once the chat is cleared it will become locked again and only if the chat is locked, not by date."
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Monthly Moments?") },
            text = {
                Text("Clearing this chat will wipe all moments and immediately lock this chat again until next month.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        MonthlyMomentsPersistence.clearMomentsAndRelock(context, spaceCode, targetSlot)
                        momentsList = emptyList()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Monthly moments cleared and locked", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                ) {
                    Text("Clear & Relock Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
