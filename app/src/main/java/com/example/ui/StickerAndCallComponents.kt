package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.SpaceEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- Shared Custom Image Stickers + Unlimited Draggable Favorites ---

private enum class StickerTrayTab {
    ALL_SHARED,
    FAVORITES
}

@Composable
fun StickerTrayPanel(
    activeUserName: String = "Alex",
    onSendSticker: (stickerId: String, emoji: String, caption: String, bgColorHex: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(StickerTrayTab.FAVORITES) }
    var sharedStickers by remember { mutableStateOf(DuoVaultPersistence.loadSharedStickers(context)) }

    // Drag-and-drop state for rearranging Favorite stickers
    var draggingStickerId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cellWidthPx by remember { mutableFloatStateOf(200f) }
    var cellHeightPx by remember { mutableFloatStateOf(200f) }

    val favoriteStickers = remember(sharedStickers) {
        sharedStickers
            .filter { it.isFavorite }
            .sortedBy { it.favoriteOrder }
    }

    val stickerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            sharedStickers = DuoVaultPersistence.importImageAsSharedSticker(
                context = context,
                uri = uri,
                createdBy = activeUserName
            )
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .testTag("sticker_tray_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top bar: Favorites (Default First) / Shared Stickers Tabs + Import Image Button + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1 (Default): Unlimited Favorites (Drag to rearrange)
                    val favSelected = selectedTab == StickerTrayTab.FAVORITES
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (favSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedTab = StickerTrayTab.FAVORITES }
                            .testTag("sticker_tab_favorites")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (favSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    Color(0xFFF59E0B)
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Favorites (${favoriteStickers.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (favSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // Tab 2: All Shared Stickers
                    val allSelected = selectedTab == StickerTrayTab.ALL_SHARED
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (allSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedTab = StickerTrayTab.ALL_SHARED }
                            .testTag("sticker_tab_all")
                    ) {
                        Text(
                            text = "Stickers (${sharedStickers.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (allSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Import Image -> Convert to Shared Sticker Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                stickerImagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("import_shared_sticker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import sticker",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close stickers",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            if (selectedTab == StickerTrayTab.FAVORITES && favoriteStickers.size > 1) {
                Text(
                    text = "Drag any favorite sticker to rearrange your order",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val displayedList = if (selectedTab == StickerTrayTab.ALL_SHARED) {
                sharedStickers
            } else {
                favoriteStickers
            }

            if (displayedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable {
                            stickerImagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedTab == StickerTrayTab.ALL_SHARED) {
                                "No shared stickers yet"
                            } else {
                                "No favorite stickers yet"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTab == StickerTrayTab.ALL_SHARED) {
                                "Tap Import to turn any photo into a borderless shared sticker for both of you"
                            } else {
                                "Tap Import to add a sticker to Favorites, or star any sticker & drag to reorder"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("sticker_grid"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(displayedList, key = { _, item -> item.id }) { _, sticker ->
                        val isBeingDragged = draggingStickerId == sticker.id
                        val isDraggableFavTab = selectedTab == StickerTrayTab.FAVORITES

                        SharedStickerTile(
                            sticker = sticker,
                            isBeingDragged = isBeingDragged,
                            dragOffset = if (isBeingDragged) dragOffset else Offset.Zero,
                            enableDragReorder = isDraggableFavTab,
                            onMeasuredSize = { w, h ->
                                if (w > 0) cellWidthPx = w.toFloat()
                                if (h > 0) cellHeightPx = h.toFloat()
                            },
                            onClick = {
                                onSendSticker(
                                    sticker.id,
                                    sticker.blobFileName,
                                    "",
                                    0xFF3B82F6L
                                )
                            },
                            onToggleFavorite = {
                                sharedStickers = DuoVaultPersistence.toggleSharedStickerFavorite(
                                    context = context,
                                    stickerId = sticker.id
                                )
                            },
                            onDeleteSticker = {
                                sharedStickers = DuoVaultPersistence.deleteSharedSticker(
                                    context = context,
                                    stickerId = sticker.id
                                )
                            },
                            onDragStart = {
                                draggingStickerId = sticker.id
                                dragOffset = Offset.Zero
                            },
                            onDragDelta = { delta ->
                                dragOffset += delta
                                val colShift = (dragOffset.x / cellWidthPx.coerceAtLeast(60f)).roundToInt()
                                val rowShift = (dragOffset.y / cellHeightPx.coerceAtLeast(60f)).roundToInt()
                                val slotShift = rowShift * 3 + colShift
                                if (slotShift != 0) {
                                    val currentFavIds = favoriteStickers.map { it.id }.toMutableList()
                                    val fromIdx = currentFavIds.indexOf(sticker.id)
                                    if (fromIdx != -1) {
                                        val targetIdx = (fromIdx + slotShift).coerceIn(0, currentFavIds.lastIndex)
                                        if (targetIdx != fromIdx) {
                                            val moved = currentFavIds.removeAt(fromIdx)
                                            currentFavIds.add(targetIdx, moved)
                                            sharedStickers = DuoVaultPersistence.reorderFavoriteStickers(
                                                context = context,
                                                orderedFavoriteIds = currentFavIds
                                            )
                                            dragOffset = Offset.Zero
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingStickerId = null
                                dragOffset = Offset.Zero
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedStickerTile(
    sticker: SharedStickerItem,
    isBeingDragged: Boolean,
    dragOffset: Offset,
    enableDragReorder: Boolean,
    onMeasuredSize: (Int, Int) -> Unit,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteSticker: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val stickerBitmap = remember(sticker.blobFileName) {
        val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, sticker.blobFileName)
        if (bytes != null && bytes.isNotEmpty()) {
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .zIndex(if (isBeingDragged) 10f else 0f)
            .offset {
                if (isBeingDragged) {
                    IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                } else {
                    IntOffset.Zero
                }
            }
            .scale(if (isBeingDragged) 1.08f else 1f)
            .onSizeChanged { size -> onMeasuredSize(size.width, size.height) }
            .height(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .let { mod ->
                if (isBeingDragged) {
                    mod.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                } else {
                    mod
                }
            }
            .let { baseMod ->
                if (enableDragReorder) {
                    baseMod.pointerInput(sticker.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount)
                            }
                        )
                    }
                } else {
                    baseMod
                }
            }
            .clickable(onClick = onClick)
            .testTag("sticker_tile_${sticker.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (stickerBitmap != null) {
            Image(
                bitmap = stickerBitmap.asImageBitmap(),
                contentDescription = "Shared sticker",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        } else {
            Text(
                text = "Sticker",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Top-Left Delete Sticker Button
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onDeleteSticker)
                .testTag("delete_sticker_${sticker.id}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete sticker",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Top-Right Favorite Toggle Star Button (No limit on favorites)
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleFavorite)
                .testTag("fav_sticker_${sticker.id}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (sticker.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite sticker",
                    tint = if (sticker.isFavorite) Color(0xFFFBBF24) else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun MessageReceiptTicks(
    isDelivered: Boolean,
    isRead: Boolean,
    onMediaOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val readColor = Color(0xFF10B981) // Vibrant Emerald Green Read Indicator
    val deliveredColor = if (onMediaOverlay) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8)
    val sentColor = if (onMediaOverlay) Color.White.copy(alpha = 0.55f) else Color(0xFF64748B)

    val tickColor = when {
        isRead -> readColor
        isDelivered -> deliveredColor
        else -> sentColor
    }

    val icon = if (isRead || isDelivered) Icons.Default.DoneAll else Icons.Default.Done
    val tag = if (isRead) "message_tick_read" else if (isDelivered) "message_tick_delivered" else "message_tick_sent"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when {
                isRead -> "Read"
                isDelivered -> "Delivered"
                else -> "Sent"
            },
            tint = tickColor,
            modifier = Modifier
                .size(13.dp)
                .testTag(tag)
        )
        if (isRead) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(readColor)
            )
        }
    }
}

@Composable
fun StickerMessageView(
    mediaMeta: String?,
    fallbackContent: String,
    timeLabel: String,
    isMe: Boolean,
    isDelivered: Boolean = false,
    isRead: Boolean,
    isPinned: Boolean
) {
    val context = LocalContext.current
    val parts = remember(mediaMeta) { (mediaMeta ?: "").split("|") }
    val blobOrRef = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: fallbackContent
    val caption = parts.getOrNull(2).orEmpty()
    val colorHex = parts.getOrNull(3)?.toLongOrNull() ?: 0xFF3B82F6L
    val badgeColor = Color(colorHex)

    val customStickerBitmap = remember(blobOrRef) {
        if (blobOrRef.endsWith(".venc")) {
            val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, blobOrRef)
            if (bytes != null && bytes.isNotEmpty()) {
                try {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sticker_pop"
    )

    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (customStickerBitmap != null) {
                Image(
                    bitmap = customStickerBitmap.asImageBitmap(),
                    contentDescription = "Shared Sticker",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    badgeColor.copy(alpha = 0.28f),
                                    badgeColor.copy(alpha = 0.0f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = blobOrRef.take(2),
                        fontSize = 48.sp,
                        modifier = Modifier.rotate(if (isMe) -4f else 4f)
                    )
                }
            }

            if (caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    modifier = Modifier.offset(y = 4.dp)
                ) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = buildString {
                    if (isPinned) append("📌 ")
                    append(timeLabel)
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isMe) {
                MessageReceiptTicks(
                    isDelivered = isDelivered,
                    isRead = isRead
                )
            }
        }
    }
}

// --- Discord-Inspired 1-on-1 Voice, Video & Screen Share Call UI ---

private val DiscordBgDark = Color(0xFF111214)
private val DiscordTileSurface = Color(0xFF1E1F22)
private val DiscordTileSecondary = Color(0xFF2B2D31)
private val DiscordDockBorder = Color(0xFF313338)
private val DiscordGreen = Color(0xFF23A55A)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordRed = Color(0xFFDA373C)

private enum class CallStageFocus {
    SPLIT_GRID,
    PARTNER_FULL,
    SELF_FULL,
    SCREEN_SHARE_FULL
}

private enum class DockSwipeStage {
    COLLAPSED,
    QUICK_MENU,
    FULL_SETTINGS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuoCallOverlay(
    callSession: ActiveCallSession,
    activeUser: TestUserSlot,
    user1Name: String,
    user2Name: String,
    onAnswerCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleScreenShare: (String) -> Unit,
    onToggleRecordCall: () -> Unit,
    onSwitchUser: (TestUserSlot) -> Unit,
    onEndCall: (elapsedSeconds: Int, recordingFilePath: String?, recordingWaveformCsv: String?) -> Unit,
    onOpenChatFromCall: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val partnerName = if (activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name
    var elapsedSec by remember { mutableIntStateOf(0) }
    var selectedShareSource by remember { mutableStateOf(callSession.screenShareLabel) }

    // Minimize mode:
    // - Clicking the top Chat button -> CallMinimizeMode.TOP_SWIPE_BAR (shows ONLY the top pull-down bar)
    // - Pressing mobile Back / Close button -> CallMinimizeMode.FLOATING_BOX (shows ONLY the floating PiP box)
    var minimizeMode by remember { mutableStateOf(CallMinimizeMode.NONE) }

    BackHandler(enabled = minimizeMode == CallMinimizeMode.NONE) {
        minimizeMode = CallMinimizeMode.FLOATING_BOX
    }

    // Stage focus state:
    // - Starts in SPLIT_GRID so your live camera is large and visible right away;
    //   automatically switches to PARTNER_FULL once partner's live video frame arrives.
    // - Double-tapping any participant block (or Screen Share stage) toggles full-stage focus.
    var stageFocus by remember {
        mutableStateOf(CallStageFocus.SPLIT_GRID)
    }

    // Progressive 2-stage swipe-up dock state (COLLAPSED -> QUICK_MENU -> FULL_SETTINGS)
    var dockSwipeStage by remember { mutableStateOf(DockSwipeStage.COLLAPSED) }

    // Soundboard panel toggle + user clips configured in Settings (Shared + Personal)
    var isSoundboardOpen by remember { mutableStateOf(false) }

    // Auto-hide call controls: hides after 4.5 seconds of inactivity and reappears on tap
    var isCallUiControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isCallUiControlsVisible, dockSwipeStage, isSoundboardOpen) {
        if (isCallUiControlsVisible && dockSwipeStage == DockSwipeStage.COLLAPSED && !isSoundboardOpen) {
            delay(4500L)
            isCallUiControlsVisible = false
        }
    }

    var customSoundboardClips by remember(activeUser, isSoundboardOpen) {
        mutableStateOf(HiveSettingsPersistence.loadSoundboardClips(context, activeUser))
    }
    var activeSoundboardBanner by remember { mutableStateOf<SoundboardClip?>(null) }
    var isSharingInScreenAudio by remember { mutableStateOf(false) }

    // Audio Output Route state — ALWAYS defaults to Bluetooth/Headset if connected, else Phone (NEVER Speaker)
    var selectedAudioRoute by remember {
        mutableStateOf(pickDefaultAudioRoute(context))
    }
    val audioRouteOptions = rememberLiveAudioRouteState(
        context = context,
        onRouteAutoChanged = { detectedRoute ->
            selectedAudioRoute = detectedRoute
        }
    )
    var showAudioOutputDialog by remember { mutableStateOf(false) }
    var showSpeakerConfirmDialog by remember { mutableStateOf(false) }

    // Voice & Audio DSP Settings State:
    // - isPartnerAudioMuted: mutes ONLY the other person's audio (4th tile "Audio Mute" in quick menu)
    // - isDeafened: deafens everything including self mic ("Deafen" switch in full settings)
    var isPartnerAudioMuted by remember { mutableStateOf(false) }
    var isDeafened by remember { mutableStateOf(false) }
    var noiseSuppressionMode by remember { mutableStateOf(NoiseSuppressionMode.KRISP) }
    var echoCancelEnabled by remember { mutableStateOf(true) }
    var autoGainEnabled by remember { mutableStateOf(true) }
    var audioInputMode by remember { mutableStateOf(AudioInputMode.VOICE_ACTIVITY) }
    var isHoldingPtt by remember { mutableStateOf(false) }
    var autoSensitivity by remember { mutableStateOf(true) }
    var inputSensitivityDb by remember { mutableFloatStateOf(-42f) }
    var outputVolumePercent by remember { mutableFloatStateOf(100f) }
    var soundboardVolumePercent by remember { mutableFloatStateOf(0.85f) }
    var micLoopbackTest by remember { mutableStateOf(false) }

    LaunchedEffect(selectedAudioRoute, outputVolumePercent, isPartnerAudioMuted, isDeafened) {
        applyAudioOutputRoute(
            context = context,
            route = selectedAudioRoute,
            outputVolumePercent = outputVolumePercent,
            muteIncomingAudio = isPartnerAudioMuted || isDeafened
        )
    }

    // Real 16kHz PCM Voice DSP Engine (Hardware + Software Noise Suppression, Echo Cancel, Auto Gain, Sensitivity Gate)
    val liveDsp by rememberRealCallVoiceDspEngine(
        enabled = callSession.isConnected && !callSession.isMuted && !isDeafened,
        suppressionMode = noiseSuppressionMode,
        echoCancelEnabled = echoCancelEnabled,
        autoGainEnabled = autoGainEnabled,
        autoSensitivity = autoSensitivity,
        manualSensitivityDb = inputSensitivityDb,
        outputVolumePercent = outputVolumePercent,
        micLoopbackTest = micLoopbackTest
    )

    // Clear soundboard floating pill after 1.6s
    LaunchedEffect(activeSoundboardBanner) {
        if (activeSoundboardBanner != null) {
            delay(1600L)
            activeSoundboardBanner = null
        }
    }

    val partnerSignal by com.example.data.remote.HiveLivePeerRelay.partnerSignal.collectAsState()
    val partnerVideoBitmap by com.example.data.remote.HiveLivePeerRelay.partnerVideoBitmap.collectAsState()
    val isPartnerVideoOn = partnerSignal.isFresh &&
        (partnerSignal.isPartnerVideoOn || (partnerSignal.videoMode == "CAM" && partnerVideoBitmap != null))
    val isPartnerScreenSharing = partnerSignal.isFresh &&
        (partnerSignal.isPartnerScreenSharing || (partnerSignal.videoMode == "SCREEN" && partnerVideoBitmap != null))
    val anyScreenSharing = callSession.isScreenSharing || isPartnerScreenSharing

    var hasCallCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val callCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCallCameraPermission = granted
        if (granted && !callSession.isVideoEnabled) {
            onToggleVideo()
        }
    }

    val handleToggleVideo: () -> Unit = {
        val permGrantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasCallCameraPermission = permGrantedNow
        onToggleVideo()
    }

    val bluetoothConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(callSession.isConnected) {
        if (callSession.isConnected && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val hasBtConnect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasBtConnect) {
                runCatching {
                    bluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        }
    }

    LaunchedEffect(callSession.isVideoEnabled, callSession.isFrontCamera) {
        com.example.data.remote.HiveLivePeerRelay.setCallVideoState(
            isVideoOn = callSession.isVideoEnabled,
            isFrontCamera = callSession.isFrontCamera
        )
        if (callSession.isVideoEnabled) {
            val permNow = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            hasCallCameraPermission = permNow
            if (!permNow) {
                try {
                    callCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } catch (_: Exception) {
                }
            }
            if (!anyScreenSharing) {
                stageFocus = if (isPartnerVideoOn && partnerVideoBitmap != null) {
                    CallStageFocus.PARTNER_FULL
                } else {
                    CallStageFocus.SPLIT_GRID
                }
            }
        }
    }

    LaunchedEffect(isPartnerVideoOn, partnerVideoBitmap != null) {
        if (isPartnerVideoOn && partnerVideoBitmap != null && !anyScreenSharing && stageFocus == CallStageFocus.SPLIT_GRID && callSession.isVideoEnabled) {
            stageFocus = CallStageFocus.PARTNER_FULL
        }
    }

    // If screen sharing ends while in SCREEN_SHARE_FULL, return to appropriate stage
    LaunchedEffect(anyScreenSharing) {
        if (!anyScreenSharing && stageFocus == CallStageFocus.SCREEN_SHARE_FULL) {
            stageFocus = CallStageFocus.SPLIT_GRID
        }
    }

    // Interactive laser pointer offset + live drawing points on the shared screen stage
    var pointerNormalized by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    var sharedCanvasTrail by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var localScreenPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastAppWindowBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val localRootView = LocalView.current

    // Live Screen Share Frame Capture & Relay Loop (continues even when call is minimized to top bar!)
    LaunchedEffect(callSession.isScreenSharing, selectedShareSource, minimizeMode) {
        com.example.data.remote.HiveLivePeerRelay.setCallScreenShareState(
            isScreenSharing = callSession.isScreenSharing,
            label = selectedShareSource,
            pointerX = pointerNormalized.x,
            pointerY = pointerNormalized.y
        )
        if (callSession.isScreenSharing) {
            var frameTick = 0
            while (isActive && callSession.isScreenSharing) {
                frameTick += 1
                runCatching {
                    val root = (context as? Activity)?.window?.decorView?.rootView ?: localRootView.rootView
                    val rw = root.width.coerceAtLeast(270)
                    val rh = root.height.coerceAtLeast(420)
                    val outW = 270
                    val outH = ((outW.toFloat() * rh.toFloat()) / rw.toFloat()).toInt().coerceIn(200, 520)
                    val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    val canvas = AndroidCanvas(bmp)

                    if (minimizeMode != CallMinimizeMode.NONE && root.width > 10 && root.height > 10) {
                        // User minimized the call to browse the app live: capture live app window directly!
                        val sx = outW.toFloat() / root.width.toFloat()
                        val sy = outH.toFloat() / root.height.toFloat()
                        canvas.save()
                        canvas.scale(sx, sy)
                        root.draw(canvas)
                        canvas.restore()
                        lastAppWindowBitmap = bmp.copy(Bitmap.Config.ARGB_8888, false)
                    } else if (lastAppWindowBitmap != null && selectedShareSource != "DuoSpace Canvas") {
                        // Draw latest captured app window frame as base when back in Call overlay
                        val prev = lastAppWindowBitmap!!
                        val srcRect = android.graphics.Rect(0, 0, prev.width, prev.height)
                        val dstRect = android.graphics.Rect(0, 0, outW, outH)
                        canvas.drawBitmap(prev, srcRect, dstRect, null)
                    } else {
                        // Render live interactive workspace canvas frame
                        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(22, 25, 34)
                            style = android.graphics.Paint.Style.FILL
                        }
                        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), bgPaint)

                        val cardPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(35, 39, 52)
                            style = android.graphics.Paint.Style.FILL
                        }
                        val pad = 16f
                        canvas.drawRoundRect(
                            pad,
                            pad,
                            outW - pad,
                            outH - pad,
                            20f,
                            20f,
                            cardPaint
                        )

                        val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(48, 54, 72)
                        }
                        canvas.drawRoundRect(
                            pad + 12f,
                            pad + 12f,
                            outW - pad - 12f,
                            pad + 44f,
                            10f,
                            10f,
                            headerPaint
                        )

                        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            textSize = 11.5f
                            isFakeBoldText = true
                        }
                        canvas.drawText(
                            "LIVE • $selectedShareSource",
                            pad + 22f,
                            pad + 32f,
                            textPaint
                        )

                        val pulseShift = (kotlin.math.sin(frameTick * 0.45) * 8.0).toFloat()
                        val accentPaint1 = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(115, 88, 101, 242)
                        }
                        canvas.drawRoundRect(
                            pad + 16f,
                            pad + 60f + pulseShift,
                            outW * 0.68f,
                            pad + 96f + pulseShift,
                            12f,
                            12f,
                            accentPaint1
                        )

                        val accentPaint2 = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(105, 35, 165, 90)
                        }
                        canvas.drawRoundRect(
                            outW * 0.30f,
                            pad + 112f - pulseShift * 0.6f,
                            outW - pad - 16f,
                            pad + 150f - pulseShift * 0.6f,
                            12f,
                            12f,
                            accentPaint2
                        )

                        val subTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(190, 220, 225, 240)
                            textSize = 10f
                        }
                        canvas.drawText(
                            "Drag here to draw & point live • Tap 'Browse App Live' to stream app",
                            pad + 16f,
                            outH - pad - 16f,
                            subTextPaint
                        )
                    }

                    // Draw interactive laser pointer & trail onto transmitted screen frame
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(230, 88, 101, 242)
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4.5f
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    for (i in 1 until sharedCanvasTrail.size) {
                        val p0 = sharedCanvasTrail[i - 1]
                        val p1 = sharedCanvasTrail[i]
                        canvas.drawLine(p0.x * outW, p0.y * outH, p1.x * outW, p1.y * outH, paint)
                    }
                    val px = pointerNormalized.x * outW
                    val py = pointerNormalized.y * outH
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.argb(110, 88, 101, 242)
                    canvas.drawCircle(px, py, 14f, paint)
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(px, py, 5f, paint)

                    localScreenPreviewBitmap = bmp
                    com.example.data.remote.HiveLivePeerRelay.updateScreenSharePointer(
                        pointerNormalized.x,
                        pointerNormalized.y
                    )
                    com.example.data.remote.HiveLivePeerRelay.submitLocalVideoBitmap(bmp, mode = "SCREEN")
                }
                delay(360L)
            }
        } else {
            localScreenPreviewBitmap = null
        }
    }

    // Optional Android MediaProjection screen capture intent launcher
    val screenShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (!callSession.isScreenSharing) {
            onToggleScreenShare(selectedShareSource)
        }
    }

    // Real audio recorder for call recording
    val callRecorder = remember(context) { VoiceRecorderController(context) }
    var isRecorderRunning by remember { mutableStateOf(false) }
    var lastRecordedPath by remember { mutableStateOf<String?>(null) }
    var lastRecordedWaveform by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(callSession.isConnected, callSession.isRecording) {
        if (callSession.isConnected && callSession.isRecording) {
            if (!isRecorderRunning) {
                callRecorder.startRecording()
                isRecorderRunning = true
            }
            while (isActive && callSession.isRecording) {
                callRecorder.sampleAmplitude()
                delay(220L)
            }
        } else if (isRecorderRunning) {
            val saved = callRecorder.stopRecording(cancel = false)
            isRecorderRunning = false
            if (saved != null) {
                lastRecordedPath = saved.first
                lastRecordedWaveform = saved.third
            }
        }
    }

    DisposableEffect(callSession.isSpeakerOn, callSession.isMuted, isDeafened) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.isMicrophoneMute = callSession.isMuted || isDeafened
        } catch (_: Exception) {
        }
        onDispose {
            try {
                audioManager?.isMicrophoneMute = false
            } catch (_: Exception) {
            }
        }
    }

    val isIncomingRinging = !callSession.isConnected && callSession.callerSlot != activeUser

    val micPermLauncherForCall = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(callSession.isConnected, isIncomingRinging) {
        if (isIncomingRinging) {
            HiveNotificationHelper.startIncomingCallRingAndVibration(context)
        } else {
            HiveNotificationHelper.cancelIncomingCallNotification(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            HiveNotificationHelper.stopIncomingCallRingAndVibration(context)
        }
    }

    LaunchedEffect(callSession.isConnected, callSession.callSessionId) {
        if (!callSession.isConnected) {
            elapsedSec = 0
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic && callSession.callerSlot == activeUser) {
                runCatching {
                    micPermLauncherForCall.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            // Wait up to 45 seconds for partner to answer before timing out an unanswered call
            delay(45_000L)
            if (!callSession.isConnected) {
                HiveNotificationHelper.cancelIncomingCallNotification(context)
                onEndCall(0, null, null)
            }
        } else {
            HiveNotificationHelper.cancelIncomingCallNotification(context)
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) {
                runCatching {
                    micPermLauncherForCall.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            val startMs = System.currentTimeMillis()
            while (isActive) {
                elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt()
                delay(500L)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "discord_call_anim")
    val voiceWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_wave"
    )

    val mins = elapsedSec / 60
    val secs = elapsedSec % 60
    val formattedTimer = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    val isPartnerSpeaking = callSession.isConnected && !isDeafened && !isPartnerAudioMuted &&
        outputVolumePercent > 1f && voiceWavePhase > 0.35f

    val fallbackLevelDb = -70f + (1f - voiceWavePhase) * 52f
    val currentLiveInputDb = if (liveDsp.hasMicPermission) liveDsp.inputDb else fallbackLevelDb
    val currentEffectiveThresholdDb = if (liveDsp.hasMicPermission) {
        liveDsp.effectiveThresholdDb
    } else if (autoSensitivity) {
        -42f
    } else {
        inputSensitivityDb
    }
    val isVoiceGateOpen = if (liveDsp.hasMicPermission) {
        liveDsp.isVoiceGateOpen
    } else {
        currentLiveInputDb >= currentEffectiveThresholdDb
    }

    val isSelfSpeaking = callSession.isConnected && !callSession.isMuted && !isDeafened &&
        when (audioInputMode) {
            AudioInputMode.VOICE_ACTIVITY -> isVoiceGateOpen
            AudioInputMode.PUSH_TO_TALK -> isHoldingPtt
        }

    val handleEndCall = {
        HiveNotificationHelper.cancelIncomingCallNotification(context)
        if (isRecorderRunning) {
            val saved = callRecorder.stopRecording(cancel = false)
            isRecorderRunning = false
            if (saved != null) {
                lastRecordedPath = saved.first
                lastRecordedWaveform = saved.third
            }
        }
        if (callSession.wasEverRecorded || callSession.isRecording || !lastRecordedPath.isNullOrBlank()) {
            DuoVaultPersistence.saveCallRecordingToVault(
                context = context,
                ownerSlot = activeUser,
                user1Name = user1Name,
                user2Name = user2Name,
                recordingFilePath = lastRecordedPath,
                durationSec = elapsedSec
            )
        }
        // Recorded call audio is saved directly in the Vault (Audio folder) and NOT sent as a chat audio player
        onEndCall(elapsedSec, null, null)
    }

    // Dedicated Full-Screen Incoming Call Interface with Answer & Decline when receiving a call
    if (isIncomingRinging) {
        val cfg = remember(context, HiveSettingsPersistence.changeCounter) {
            HiveSettingsPersistence.loadConfig(context)
        }
        val callerAvatarBlob = if (callSession.callerSlot == TestUserSlot.USER_1) {
            cfg.user1AvatarBlob
        } else {
            cfg.user2AvatarBlob
        }
        HiveIncomingCallOverlay(
            callerName = partnerName,
            callerAvatarBlob = callerAvatarBlob,
            callMode = callSession.mode,
            pulseFraction = voiceWavePhase,
            onAnswer = {
                HiveNotificationHelper.cancelIncomingCallNotification(context)
                val hasMic = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasMic) {
                    runCatching {
                        micPermLauncherForCall.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                onAnswerCall()
            },
            onDecline = {
                HiveNotificationHelper.cancelIncomingCallNotification(context)
                handleEndCall()
            }
        )
        return
    }

    // Show ONLY the top swipe-down bar (if Chat icon was clicked) OR ONLY the floating box (if mobile Back was pressed)
    if (minimizeMode != CallMinimizeMode.NONE) {
        MinimizedDiscordCallOverlay(
            minimizeMode = minimizeMode,
            partnerName = partnerName,
            formattedTimer = formattedTimer,
            isConnected = callSession.isConnected,
            isSpeaking = isPartnerSpeaking,
            isMuted = callSession.isMuted,
            isDeafened = isDeafened,
            isVideoEnabled = callSession.isVideoEnabled,
            isScreenSharing = callSession.isScreenSharing,
            onExpandCall = { minimizeMode = CallMinimizeMode.NONE },
            onToggleMute = onToggleMute,
            onEndCall = handleEndCall
        )
        return
    }

    val triggerScreenShare = {
        onToggleScreenShare(selectedShareSource)
    }

    Surface(
        color = DiscordBgDark,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isCallUiControlsVisible = !isCallUiControlsVisible
            }
            .testTag("active_call_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Discord-Style Top Stage Header Bar (Auto-hides, tap screen to toggle)
            AnimatedVisibility(
                visible = isCallUiControlsVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
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
                        // Connection & Timer Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DiscordTileSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDockBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (callSession.isConnected) DiscordGreen else Color(0xFFFBBF24)
                                        )
                                )
                                Text(
                                    text = if (callSession.isConnected) {
                                        "Voice Connected • $formattedTimer"
                                    } else {
                                        "Ringing $partnerName…"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        // Discord LIVE Pill when Screen Sharing (self or partner)
                        if (anyScreenSharing) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DiscordRed
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Recording Pill
                        if (callSession.isRecording) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DiscordRed.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordRed)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(DiscordRed)
                                    )
                                    Text(
                                        text = "REC",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }
                        }
                    }

                    // Top-Right Action Icons:
                    // 1) Audio Output Selector (defaults to Bluetooth / Headset)
                    // 2) Open Chat (minimizes call to floating box + top swipe-down banner)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Audio Output Route Selector Pill Button (Displays Bluetooth / Headset / Speaker / Phone)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DiscordTileSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDockBorder),
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAudioOutputDialog = true }
                                .testTag("call_audio_output_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = iconForAudioRoute(selectedAudioRoute),
                                    contentDescription = selectedAudioRoute.label,
                                    tint = if (selectedAudioRoute == CallAudioRoute.BLUETOOTH ||
                                        selectedAudioRoute == CallAudioRoute.HEADSET
                                    ) {
                                        DiscordBlurple
                                    } else {
                                        Color.White
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                val activeRouteDisplayName = audioRouteOptions
                                    .firstOrNull { it.route == selectedAudioRoute }
                                    ?.displayName
                                    ?: selectedAudioRoute.label
                                Text(
                                    text = activeRouteDisplayName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Open Chat Icon Button -> Minimizes call to ONLY the top pull-down bar & opens Chat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DiscordTileSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDockBorder),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onOpenChatFromCall()
                                    minimizeMode = CallMinimizeMode.TOP_SWIPE_BAR
                                }
                                .testTag("call_minimize_to_chat_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Open chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Center Discord-Style Stage Area (Tap anywhere to toggle controls, Double-Tap Any Block for Full Stage)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = if (isCallUiControlsVisible) 10.dp else 0.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isCallUiControlsVisible = !isCallUiControlsVisible
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (anyScreenSharing &&
                        stageFocus != CallStageFocus.PARTNER_FULL &&
                        stageFocus != CallStageFocus.SELF_FULL
                    ) {
                        // --- DISCORD SCREEN SHARE STAGE (Supports both Local Sharing & Partner Incoming Screen Share!) ---
                        val isShareFull = stageFocus == CallStageFocus.SCREEN_SHARE_FULL
                        val isSelfSharingNow = callSession.isScreenSharing
                        val shareHeaderTitle = if (isSelfSharingNow) {
                            "$activeUserName's Screen • $selectedShareSource"
                        } else {
                            "$partnerName's Screen • ${partnerSignal.partnerScreenShareLabel}"
                        }
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = DiscordTileSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                DiscordBlurple.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .combinedClickable(
                                    onClick = { isCallUiControlsVisible = !isCallUiControlsVisible },
                                    onDoubleClick = {
                                        stageFocus = if (isShareFull) {
                                            CallStageFocus.SPLIT_GRID
                                        } else {
                                            CallStageFocus.SCREEN_SHARE_FULL
                                        }
                                    }
                                )
                                .testTag("screen_share_stage_card")
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF18191C))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenShare,
                                            contentDescription = null,
                                            tint = DiscordGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = shareHeaderTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isSelfSharingNow) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = DiscordGreen.copy(alpha = 0.25f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        runCatching {
                                                            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                                                            if (mpm != null) {
                                                                screenShareLauncher.launch(mpm.createScreenCaptureIntent())
                                                            }
                                                        }
                                                    }
                                                    .testTag("screen_share_os_cast_button")
                                            ) {
                                                Text(
                                                    text = "Cast OS",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DiscordGreen,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = DiscordBlurple.copy(alpha = 0.28f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        onOpenChatFromCall()
                                                        minimizeMode = CallMinimizeMode.TOP_SWIPE_BAR
                                                    }
                                                    .testTag("screen_share_browse_app_button")
                                            ) {
                                                Text(
                                                    text = "Browse App Live",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (isSharingInScreenAudio) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = DiscordGreen.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = "AUDIO",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DiscordGreen,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DiscordRed
                                        ) {
                                            Text(
                                                text = "LIVE",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF161922), Color(0xFF0D1017))
                                            )
                                        )
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                isCallUiControlsVisible = !isCallUiControlsVisible
                                            },
                                            onDoubleClick = {
                                                stageFocus = if (isShareFull) {
                                                    CallStageFocus.SPLIT_GRID
                                                } else {
                                                    CallStageFocus.SCREEN_SHARE_FULL
                                                }
                                            }
                                        )
                                        .pointerInput(isSelfSharingNow) {
                                            if (isSelfSharingNow) {
                                                detectDragGestures { change, _ ->
                                                    change.consume()
                                                    val nx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                                                    val ny = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                                                    val pt = Offset(nx, ny)
                                                    pointerNormalized = pt
                                                    sharedCanvasTrail = (sharedCanvasTrail + pt).takeLast(90)
                                                    com.example.data.remote.HiveLivePeerRelay.updateScreenSharePointer(nx, ny)
                                                }
                                            }
                                        }
                                ) {
                                    // Display Partner's incoming live screen share bitmap OR Local live screen preview
                                    val displayBitmap = if (!isSelfSharingNow) {
                                        partnerVideoBitmap
                                    } else {
                                        localScreenPreviewBitmap
                                    }

                                    if (displayBitmap != null) {
                                        Image(
                                            bitmap = displayBitmap.asImageBitmap(),
                                            contentDescription = "Live Shared Screen",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height

                                        if (displayBitmap == null) {
                                            val frameW = w * 0.84f
                                            val frameH = h * 0.78f
                                            val left = (w - frameW) / 2f
                                            val top = (h - frameH) / 2f

                                            drawRoundRect(
                                                color = Color(0xFF232733),
                                                topLeft = Offset(left, top),
                                                size = Size(frameW, frameH),
                                                cornerRadius = CornerRadius(24f, 24f)
                                            )

                                            drawRoundRect(
                                                color = Color(0xFF2E3344),
                                                topLeft = Offset(left + 18f, top + 18f),
                                                size = Size(frameW - 36f, 34f),
                                                cornerRadius = CornerRadius(10f, 10f)
                                            )

                                            val shift = voiceWavePhase * 10f
                                            drawRoundRect(
                                                color = DiscordBlurple.copy(alpha = 0.35f),
                                                topLeft = Offset(left + 22f, top + 68f + shift),
                                                size = Size(frameW * 0.56f, 40f),
                                                cornerRadius = CornerRadius(14f, 14f)
                                            )
                                            drawRoundRect(
                                                color = DiscordGreen.copy(alpha = 0.3f),
                                                topLeft = Offset(left + frameW * 0.32f, top + 124f - shift * 0.5f),
                                                size = Size(frameW * 0.58f, 44f),
                                                cornerRadius = CornerRadius(14f, 14f)
                                            )
                                        }

                                        // Render interactive whiteboard trail
                                        for (i in 1 until sharedCanvasTrail.size) {
                                            val p0 = sharedCanvasTrail[i - 1]
                                            val p1 = sharedCanvasTrail[i]
                                            drawLine(
                                                color = DiscordBlurple,
                                                start = Offset(p0.x * w, p0.y * h),
                                                end = Offset(p1.x * w, p1.y * h),
                                                strokeWidth = 6f
                                            )
                                        }

                                        val activePtr = if (isSelfSharingNow) {
                                            pointerNormalized
                                        } else {
                                            Offset(partnerSignal.partnerPointerX, partnerSignal.partnerPointerY)
                                        }
                                        val px = w * activePtr.x
                                        val py = h * activePtr.y
                                        drawCircle(
                                            color = DiscordBlurple.copy(alpha = 0.35f),
                                            radius = 24f,
                                            center = Offset(px, py)
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 7f,
                                            center = Offset(px, py)
                                        )
                                    }

                                    if (isSelfSharingNow) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(10.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf("Phone Screen", "DuoSpace Canvas", "App Window").forEach { src ->
                                                val isSelected = selectedShareSource == src
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (isSelected) DiscordBlurple else Color.Black.copy(alpha = 0.6f),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        Color.White.copy(alpha = if (isSelected) 0.4f else 0.15f)
                                                    ),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable {
                                                            selectedShareSource = src
                                                            com.example.data.remote.HiveLivePeerRelay.setCallScreenShareState(
                                                                isScreenSharing = true,
                                                                label = src,
                                                                pointerX = pointerNormalized.x,
                                                                pointerY = pointerNormalized.y
                                                            )
                                                        }
                                                ) {
                                                    Text(
                                                        text = src,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                            if (sharedCanvasTrail.isNotEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = DiscordRed.copy(alpha = 0.85f),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable { sharedCanvasTrail = emptyList() }
                                                ) {
                                                    Text(
                                                        text = "Clear Ink",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isShareFull) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DiscordParticipantTile(
                                    name = partnerName,
                                    isSpeaking = isPartnerSpeaking,
                                    isMuted = false,
                                    isDeafened = isPartnerAudioMuted || isDeafened,
                                    isVideoOn = isPartnerVideoOn,
                                    isFrontCamera = partnerSignal.isPartnerFrontCamera,
                                    isSelfTile = false,
                                    hasCameraPermission = hasCallCameraPermission,
                                    remoteVideoBitmap = if (!isPartnerScreenSharing) partnerVideoBitmap else null,
                                    onRequestCameraPermission = {},
                                    isFullStage = false,
                                    showFlipButton = false,
                                    onFlipCamera = {},
                                    onSingleTapTile = { isCallUiControlsVisible = !isCallUiControlsVisible },
                                    onDoubleTapTile = { stageFocus = CallStageFocus.PARTNER_FULL },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("call_partner_tile")
                                )
                                DiscordParticipantTile(
                                    name = "$activeUserName (You)",
                                    isSpeaking = isSelfSpeaking,
                                    isMuted = callSession.isMuted,
                                    isDeafened = isDeafened,
                                    isVideoOn = callSession.isVideoEnabled,
                                    isFrontCamera = callSession.isFrontCamera,
                                    isSelfTile = true,
                                    hasCameraPermission = hasCallCameraPermission,
                                    remoteVideoBitmap = null,
                                    onRequestCameraPermission = {
                                        runCatching { callCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                                    },
                                    isFullStage = false,
                                    showFlipButton = callSession.isVideoEnabled,
                                    onFlipCamera = onFlipCamera,
                                    onSingleTapTile = { isCallUiControlsVisible = !isCallUiControlsVisible },
                                    onDoubleTapTile = { stageFocus = CallStageFocus.SELF_FULL },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("call_self_tile")
                                )
                            }
                        }
                    } else if (stageFocus == CallStageFocus.PARTNER_FULL || stageFocus == CallStageFocus.SELF_FULL) {
                        // --- FULL STAGE FOCUS MODE (Default on Video Call, or Double-Tap Any Person's Block) ---
                        val isPartnerFocused = stageFocus == CallStageFocus.PARTNER_FULL

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            DiscordParticipantTile(
                                name = if (isPartnerFocused) partnerName else "$activeUserName (You)",
                                isSpeaking = if (isPartnerFocused) isPartnerSpeaking else isSelfSpeaking,
                                isMuted = if (isPartnerFocused) false else callSession.isMuted,
                                isDeafened = if (isPartnerFocused) (isPartnerAudioMuted || isDeafened) else isDeafened,
                                isVideoOn = if (isPartnerFocused) isPartnerVideoOn else callSession.isVideoEnabled,
                                isFrontCamera = if (isPartnerFocused) partnerSignal.isPartnerFrontCamera else callSession.isFrontCamera,
                                isSelfTile = !isPartnerFocused,
                                hasCameraPermission = hasCallCameraPermission,
                                remoteVideoBitmap = if (isPartnerFocused && !isPartnerScreenSharing) partnerVideoBitmap else null,
                                onRequestCameraPermission = {
                                    runCatching { callCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                                },
                                isFullStage = true,
                                showFlipButton = !isPartnerFocused && callSession.isVideoEnabled,
                                onFlipCamera = onFlipCamera,
                                onSingleTapTile = { isCallUiControlsVisible = !isCallUiControlsVisible },
                                onDoubleTapTile = { stageFocus = CallStageFocus.SPLIT_GRID },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(if (isPartnerFocused) "call_partner_tile" else "call_self_tile")
                            )

                            DiscordPipCornerTile(
                                name = if (isPartnerFocused) "You" else partnerName,
                                isSpeaking = if (isPartnerFocused) isSelfSpeaking else isPartnerSpeaking,
                                isMuted = if (isPartnerFocused) callSession.isMuted else (isPartnerAudioMuted || isDeafened),
                                isVideoOn = if (isPartnerFocused) callSession.isVideoEnabled else isPartnerVideoOn,
                                isFrontCamera = if (isPartnerFocused) callSession.isFrontCamera else partnerSignal.isPartnerFrontCamera,
                                isSelfTile = isPartnerFocused,
                                hasCameraPermission = hasCallCameraPermission,
                                remoteVideoBitmap = if (!isPartnerFocused && !isPartnerScreenSharing) partnerVideoBitmap else null,
                                onRequestCameraPermission = {
                                    runCatching { callCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                                },
                                showFlipButton = isPartnerFocused && callSession.isVideoEnabled,
                                onFlipCamera = onFlipCamera,
                                onSwapFocus = {
                                    stageFocus = if (isPartnerFocused) {
                                        CallStageFocus.SELF_FULL
                                    } else {
                                        CallStageFocus.PARTNER_FULL
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(14.dp)
                                    .size(width = 110.dp, height = 148.dp)
                                    .testTag(if (isPartnerFocused) "call_self_tile" else "call_partner_tile")
                            )
                        }
                    } else {
                        // --- STANDARD 2-TILE SPLIT GRID STAGE (Double-tap either block to expand Full Stage!) ---
                        DiscordParticipantTile(
                            name = partnerName,
                            isSpeaking = isPartnerSpeaking,
                            isMuted = false,
                            isDeafened = isPartnerAudioMuted || isDeafened,
                            isVideoOn = isPartnerVideoOn,
                            isFrontCamera = partnerSignal.isPartnerFrontCamera,
                            isSelfTile = false,
                            hasCameraPermission = hasCallCameraPermission,
                            remoteVideoBitmap = if (!isPartnerScreenSharing) partnerVideoBitmap else null,
                            onRequestCameraPermission = {},
                            isFullStage = false,
                            showFlipButton = false,
                            onFlipCamera = {},
                            onSingleTapTile = { isCallUiControlsVisible = !isCallUiControlsVisible },
                            onDoubleTapTile = { stageFocus = CallStageFocus.PARTNER_FULL },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("call_partner_tile")
                        )

                        DiscordParticipantTile(
                            name = "$activeUserName (You)",
                            isSpeaking = isSelfSpeaking,
                            isMuted = callSession.isMuted,
                            isDeafened = isDeafened,
                            isVideoOn = callSession.isVideoEnabled,
                            isFrontCamera = callSession.isFrontCamera,
                            isSelfTile = true,
                            hasCameraPermission = hasCallCameraPermission,
                            remoteVideoBitmap = null,
                            onRequestCameraPermission = {
                                runCatching { callCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                            },
                            isFullStage = false,
                            showFlipButton = callSession.isVideoEnabled,
                            onFlipCamera = onFlipCamera,
                            onSingleTapTile = { isCallUiControlsVisible = !isCallUiControlsVisible },
                            onDoubleTapTile = { stageFocus = CallStageFocus.SELF_FULL },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("call_self_tile")
                        )
                    }

                    // Push-To-Talk Hold Bar when Input Mode is PUSH_TO_TALK
                    if (audioInputMode == AudioInputMode.PUSH_TO_TALK) {
                        PushToTalkHoldBar(
                            isHoldingPtt = isHoldingPtt,
                            onHoldChange = { isHoldingPtt = it }
                        )
                    }

                    // Quick "Join Voice Now" pill while ringing
                    if (!callSession.isConnected) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DiscordGreen,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onAnswerCall)
                                    .testTag("answer_call_now_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Answer call",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Join Voice Now",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Soundboard Reaction Pill when a Soundboard clip is played
                activeSoundboardBanner?.let { clip ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(clip.accentHex),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$activeUserName played ${clip.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Progressive 2-Stage Swipe-Up Discord Control Dock + Soundboard (Auto-hides, tap screen to reveal)
            AnimatedVisibility(
                visible = isCallUiControlsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                shape = RoundedCornerShape(28.dp),
                color = DiscordTileSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDockBorder),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(dockSwipeStage) {
                        var accumulatedDy = 0f
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDy = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDy += dragAmount
                                if (accumulatedDy < -120f) {
                                    dockSwipeStage = DockSwipeStage.FULL_SETTINGS
                                    accumulatedDy = 0f
                                } else if (accumulatedDy < -28f && dockSwipeStage == DockSwipeStage.COLLAPSED) {
                                    dockSwipeStage = DockSwipeStage.QUICK_MENU
                                    accumulatedDy = 0f
                                } else if (accumulatedDy < -36f && dockSwipeStage == DockSwipeStage.QUICK_MENU) {
                                    dockSwipeStage = DockSwipeStage.FULL_SETTINGS
                                    accumulatedDy = 0f
                                } else if (accumulatedDy > 28f && dockSwipeStage == DockSwipeStage.FULL_SETTINGS) {
                                    dockSwipeStage = DockSwipeStage.QUICK_MENU
                                    accumulatedDy = 0f
                                } else if (accumulatedDy > 28f && dockSwipeStage == DockSwipeStage.QUICK_MENU) {
                                    dockSwipeStage = DockSwipeStage.COLLAPSED
                                    accumulatedDy = 0f
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clean Minimal Grab Handle (Swipe up for Quick Menu, Swipe all the way up for Full Audio Settings)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                dockSwipeStage = when (dockSwipeStage) {
                                    DockSwipeStage.COLLAPSED -> DockSwipeStage.QUICK_MENU
                                    DockSwipeStage.QUICK_MENU -> DockSwipeStage.FULL_SETTINGS
                                    DockSwipeStage.FULL_SETTINGS -> DockSwipeStage.COLLAPSED
                                }
                            }
                            .padding(vertical = 4.dp)
                            .testTag("call_dock_swipe_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                        )
                    }

                    // Soundboard Drawer (when Soundboard button is tapped; clips managed in Settings)
                    AnimatedVisibility(
                        visible = isSoundboardOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DiscordSoundboardPanel(
                            customClips = customSoundboardClips,
                            onDeleteClip = { clip ->
                                customSoundboardClips = HiveSettingsPersistence.deleteSoundboardClip(
                                    context = context,
                                    clipId = clip.id,
                                    activeUser = activeUser
                                )
                            },
                            onPlayClip = { clip ->
                                activeSoundboardBanner = clip
                                playCustomSoundboardClip(
                                    clip = clip,
                                    soundboardVolume = soundboardVolumePercent,
                                    outputVolume = outputVolumePercent,
                                    isDeafened = isDeafened,
                                    scope = scope
                                )
                            },
                            onClose = { isSoundboardOpen = false }
                        )
                    }

                    // Stage 1 Swipe-Up: Clean 4-Tile Quick Menu (1st tile is Share Audio for in-screen audio)
                    AnimatedVisibility(
                        visible = dockSwipeStage != DockSwipeStage.COLLAPSED,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Share Audio (Shares in-screen audio during call / screen share)
                                DiscordSecondaryBentoCard(
                                    icon = Icons.Default.GraphicEq,
                                    title = if (isSharingInScreenAudio) "Sharing Audio" else "Share Audio",
                                    isActive = isSharingInScreenAudio,
                                    activeColor = DiscordGreen,
                                    onClick = {
                                        isSharingInScreenAudio = !isSharingInScreenAudio
                                    },
                                    testTag = "call_share_audio_button",
                                    modifier = Modifier.weight(1f)
                                )

                                // 2. Record Call
                                DiscordSecondaryBentoCard(
                                    icon = Icons.Default.FiberManualRecord,
                                    title = if (callSession.isRecording) "Recording" else "Record Call",
                                    isActive = callSession.isRecording,
                                    activeColor = DiscordRed,
                                    onClick = onToggleRecordCall,
                                    testTag = "call_record_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 3. Flip Camera
                                DiscordSecondaryBentoCard(
                                    icon = Icons.Default.SwapHoriz,
                                    title = if (callSession.isFrontCamera) "Front Camera" else "Back Camera",
                                    isActive = !callSession.isFrontCamera && callSession.isVideoEnabled,
                                    activeColor = DiscordGreen,
                                    onClick = {
                                        if (!callSession.isVideoEnabled) {
                                            handleToggleVideo()
                                        }
                                        onFlipCamera()
                                    },
                                    testTag = "call_flip_camera_button",
                                    modifier = Modifier.weight(1f)
                                )

                                // 4. Audio Mute (Mutes the OTHER person's incoming audio only)
                                DiscordSecondaryBentoCard(
                                    icon = Icons.Default.VolumeOff,
                                    title = "Audio Mute",
                                    isActive = isPartnerAudioMuted,
                                    activeColor = DiscordRed,
                                    onClick = { isPartnerAudioMuted = !isPartnerAudioMuted },
                                    testTag = "call_deafen_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (dockSwipeStage == DockSwipeStage.QUICK_MENU) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { dockSwipeStage = DockSwipeStage.FULL_SETTINGS }
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "More voice settings",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Swipe up for Voice & Audio Settings",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = DiscordDockBorder,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Stage 2 Swipe All The Way Up: Full Discord Voice & Audio Settings
                    AnimatedVisibility(
                        visible = dockSwipeStage == DockSwipeStage.FULL_SETTINGS,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DiscordFullAudioSettingsSection(
                            isDeafened = isDeafened,
                            onToggleDeafen = { isDeafened = !isDeafened },
                            noiseSuppressionMode = noiseSuppressionMode,
                            onSelectNoiseSuppression = { noiseSuppressionMode = it },
                            echoCancelEnabled = echoCancelEnabled,
                            onToggleEchoCancel = { echoCancelEnabled = it },
                            autoGainEnabled = autoGainEnabled,
                            onToggleAutoGain = { autoGainEnabled = it },
                            inputMode = audioInputMode,
                            onSelectInputMode = { audioInputMode = it },
                            autoSensitivity = autoSensitivity,
                            onToggleAutoSensitivity = { autoSensitivity = it },
                            inputSensitivityDb = inputSensitivityDb,
                            onChangeSensitivityDb = { inputSensitivityDb = it },
                            outputVolumePercent = outputVolumePercent,
                            onChangeOutputVolume = { outputVolumePercent = it },
                            soundboardVolumePercent = soundboardVolumePercent,
                            onChangeSoundboardVolume = { soundboardVolumePercent = it },
                            liveInputDb = currentLiveInputDb,
                            effectiveThresholdDb = currentEffectiveThresholdDb,
                            isVoiceGateOpen = isVoiceGateOpen,
                            micLoopbackTest = micLoopbackTest,
                            onToggleMicLoopbackTest = { micLoopbackTest = it }
                        )
                    }

                    // Primary 5-Pill Row: Mic, Video, Share, Soundboard, Leave
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Mute / Unmute Mic
                        DiscordDockButton(
                            icon = if (callSession.isMuted || isDeafened) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (callSession.isMuted || isDeafened) "Muted" else "Mic",
                            containerColor = if (callSession.isMuted || isDeafened) DiscordRed else DiscordTileSecondary,
                            contentColor = Color.White,
                            onClick = onToggleMute,
                            testTag = "call_mute_button"
                        )

                        // 2. Video On / Off
                        DiscordDockButton(
                            icon = if (callSession.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = if (callSession.isVideoEnabled) "Cam On" else "Video",
                            containerColor = if (callSession.isVideoEnabled) DiscordBlurple else DiscordTileSecondary,
                            contentColor = Color.White,
                            onClick = handleToggleVideo,
                            testTag = "call_video_toggle_button"
                        )

                        // 3. Screen Share Toggle
                        DiscordDockButton(
                            icon = if (callSession.isScreenSharing) {
                                Icons.Default.StopScreenShare
                            } else {
                                Icons.Default.ScreenShare
                            },
                            label = if (callSession.isScreenSharing) "Stop" else "Share",
                            containerColor = if (callSession.isScreenSharing) DiscordGreen else DiscordTileSecondary,
                            contentColor = Color.White,
                            onClick = triggerScreenShare,
                            testTag = "call_screen_share_button"
                        )

                        // 4. Soundboard Button (Replaces Options button — swipe up opens menu)
                        DiscordDockButton(
                            icon = Icons.Default.MusicNote,
                            label = "Soundboard",
                            containerColor = if (isSoundboardOpen) DiscordBlurple else DiscordTileSecondary,
                            contentColor = Color.White,
                            badgeDotColor = if (callSession.isRecording) DiscordRed else null,
                            onClick = { isSoundboardOpen = !isSoundboardOpen },
                            testTag = "call_soundboard_button"
                        )

                        // 5. Disconnect / End Call
                        DiscordDockButton(
                            icon = Icons.Default.CallEnd,
                            label = "Leave",
                            containerColor = DiscordRed,
                            contentColor = Color.White,
                            onClick = handleEndCall,
                            testTag = "end_call_button"
                        )
                    }
                }
            }
        }
    }
    }

    if (showAudioOutputDialog) {
        AudioOutputSelectorDialog(
            selectedRoute = selectedAudioRoute,
            options = audioRouteOptions,
            onSelectRoute = { route ->
                if (route == CallAudioRoute.SPEAKER && selectedAudioRoute != CallAudioRoute.SPEAKER) {
                    showSpeakerConfirmDialog = true
                } else {
                    selectedAudioRoute = route
                    if (callSession.isSpeakerOn) {
                        onToggleSpeaker()
                    }
                }
            },
            onDismiss = { showAudioOutputDialog = false }
        )
    }

    if (showSpeakerConfirmDialog) {
        SpeakerConfirmationDialog(
            onConfirmSpeaker = {
                selectedAudioRoute = CallAudioRoute.SPEAKER
                if (!callSession.isSpeakerOn) {
                    onToggleSpeaker()
                }
            },
            onDismiss = { showSpeakerConfirmDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscordParticipantTile(
    name: String,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isDeafened: Boolean,
    isVideoOn: Boolean,
    isFrontCamera: Boolean,
    isSelfTile: Boolean = false,
    hasCameraPermission: Boolean = true,
    remoteVideoBitmap: Bitmap? = null,
    onRequestCameraPermission: () -> Unit = {},
    isFullStage: Boolean,
    showFlipButton: Boolean,
    onFlipCamera: () -> Unit,
    onSingleTapTile: () -> Unit = {},
    onDoubleTapTile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSpeaking && !isMuted && !isDeafened) DiscordGreen else DiscordDockBorder,
        label = "speaking_border"
    )
    val borderWidth = if (isSpeaking && !isMuted && !isDeafened) 2.dp else 1.dp
    val avatarSize = if (isFullStage) 96.dp else 70.dp
    val ringSize = if (isFullStage) 114.dp else 86.dp

    val isShowingLiveVideo = (isSelfTile && isVideoOn && hasCameraPermission) ||
        (!isSelfTile && (isVideoOn || remoteVideoBitmap != null) && remoteVideoBitmap != null)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DiscordTileSurface,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = onSingleTapTile,
                onDoubleClick = onDoubleTapTile
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelfTile && isVideoOn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (isFrontCamera) {
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                } else {
                                    listOf(Color(0xFF0F172A), Color(0xFF064E3B))
                                }
                            )
                        )
                ) {
                    CallLiveCameraFeed(
                        isFrontCamera = isFrontCamera,
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = onRequestCameraPermission,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (!isSelfTile && remoteVideoBitmap != null && isVideoOn) {
                Image(
                    bitmap = remoteVideoBitmap.asImageBitmap(),
                    contentDescription = "$name live video",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Center Avatar with Discord Speaking Ring (hidden when live camera/video is actively rendering)
            if (!isShowingLiveVideo) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSpeaking && !isMuted && !isDeafened) {
                        Box(
                            modifier = Modifier
                                .size(ringSize)
                                .clip(CircleShape)
                                .background(DiscordGreen.copy(alpha = 0.22f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(DiscordTileSecondary)
                            .border(
                                width = if (isSpeaking && !isMuted && !isDeafened) 2.5.dp else 1.dp,
                                color = if (isSpeaking && !isMuted && !isDeafened) DiscordGreen else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            fontSize = if (isFullStage) 34.sp else 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Top-Right Flip Camera Pill on Self Video Tile
            if (showFlipButton) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onFlipCamera)
                        .testTag("video_viewport_flip_camera_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Flip camera",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isFrontCamera) "Front Cam" else "Back Cam",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom-Left Participant Name Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isDeafened) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Audio Muted",
                            tint = DiscordRed,
                            modifier = Modifier.size(13.dp)
                        )
                    } else if (isMuted) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Muted",
                            tint = DiscordRed,
                            modifier = Modifier.size(13.dp)
                        )
                    } else if (isSpeaking) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Speaking",
                            tint = DiscordGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CallLiveCameraFeed(
    isFrontCamera: Boolean,
    hasCameraPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permGranted = hasCameraPermission || remember(context, hasCameraPermission) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var hasHardwareBitmap by remember { mutableStateOf(false) }
    var fallbackCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }

    if (permGranted) {
        Box(modifier = modifier) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewRef = this
                    }
                },
                update = { view ->
                    view.scaleX = if (isFrontCamera) -1f else 1f
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!hasHardwareBitmap && fallbackCameraBitmap != null) {
                Image(
                    bitmap = fallbackCameraBitmap!!.asImageBitmap(),
                    contentDescription = "Live Camera Stream",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LaunchedEffect(previewViewRef, isFrontCamera, permGranted) {
            val view = previewViewRef ?: return@LaunchedEffect
            try {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener({
                    try {
                        val cameraProvider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }
                        val desiredSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        val fallbackSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        val activeSelector = when {
                            runCatching { cameraProvider.hasCamera(desiredSelector) }.getOrDefault(false) -> desiredSelector
                            runCatching { cameraProvider.hasCamera(fallbackSelector) }.getOrDefault(false) -> fallbackSelector
                            else -> desiredSelector
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, activeSelector, preview)
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (_: Exception) {
            }
        }

        // Periodically capture compressed live camera frames and transmit to partner via HiveLivePeerRelay
        LaunchedEffect(previewViewRef, isFrontCamera, permGranted) {
            val view = previewViewRef ?: return@LaunchedEffect
            delay(350L)
            var tick = 0
            while (isActive) {
                tick += 1
                runCatching {
                    val bmp = view.bitmap
                    if (bmp != null && bmp.width > 16 && bmp.height > 16) {
                        hasHardwareBitmap = true
                        com.example.data.remote.HiveLivePeerRelay.submitLocalVideoBitmap(bmp, mode = "CAM")
                    } else {
                        hasHardwareBitmap = false
                        val outW = 200
                        val outH = 260
                        val gen = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                        val c = AndroidCanvas(gen)
                        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        p.shader = android.graphics.LinearGradient(
                            0f,
                            0f,
                            outW.toFloat(),
                            outH.toFloat(),
                            if (isFrontCamera) intArrayOf(0xFF1E293B.toInt(), 0xFF0F172A.toInt())
                            else intArrayOf(0xFF064E3B.toInt(), 0xFF0F172A.toInt()),
                            null,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                        c.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), p)
                        p.shader = null
                        val pulse = (kotlin.math.sin(tick * 0.5) * 10.0).toFloat()
                        p.color = android.graphics.Color.argb(65, 88, 101, 242)
                        c.drawCircle(outW / 2f, outH / 2f, 48f + pulse, p)
                        p.color = android.graphics.Color.argb(210, 255, 255, 255)
                        p.textSize = 12f
                        p.isFakeBoldText = true
                        p.textAlign = android.graphics.Paint.Align.CENTER
                        c.drawText(
                            if (isFrontCamera) "LIVE FRONT CAM" else "LIVE BACK CAM",
                            outW / 2f,
                            outH / 2f + 4f,
                            p
                        )
                        fallbackCameraBitmap = gen
                        com.example.data.remote.HiveLivePeerRelay.submitLocalVideoBitmap(gen, mode = "CAM")
                    }
                }
                delay(380L)
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = DiscordBlurple,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRequestPermission)
                    .testTag("call_allow_camera_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Allow Camera",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tap to Allow Camera",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscordPipCornerTile(
    name: String,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isVideoOn: Boolean,
    isFrontCamera: Boolean,
    isSelfTile: Boolean = true,
    hasCameraPermission: Boolean = true,
    remoteVideoBitmap: Bitmap? = null,
    onRequestCameraPermission: () -> Unit = {},
    showFlipButton: Boolean,
    onFlipCamera: () -> Unit,
    onSwapFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSpeaking && !isMuted) DiscordGreen else Color.White.copy(alpha = 0.28f),
        label = "pip_speaking_border"
    )
    val isShowingLiveVideo = (isSelfTile && isVideoOn && hasCameraPermission) ||
        (!isSelfTile && (isVideoOn || remoteVideoBitmap != null) && remoteVideoBitmap != null)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DiscordTileSecondary,
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSpeaking && !isMuted) 2.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onSwapFocus,
                onDoubleClick = onSwapFocus
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelfTile && isVideoOn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (isFrontCamera) {
                                    listOf(Color(0xFF26334D), Color(0xFF111827))
                                } else {
                                    listOf(Color(0xFF064E3B), Color(0xFF0F172A))
                                }
                            )
                        )
                ) {
                    CallLiveCameraFeed(
                        isFrontCamera = isFrontCamera,
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = onRequestCameraPermission,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (!isSelfTile && remoteVideoBitmap != null && isVideoOn) {
                Image(
                    bitmap = remoteVideoBitmap.asImageBitmap(),
                    contentDescription = "$name video",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Mini Avatar in PiP (only when live video is off)
            if (!isShowingLiveVideo) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(DiscordTileSurface)
                        .border(
                            width = if (isSpeaking && !isMuted) 2.dp else 1.dp,
                            color = if (isSpeaking && !isMuted) DiscordGreen else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Flip camera button in top-right of PiP if self video is on
            if (showFlipButton) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFlipCamera)
                        .testTag("video_viewport_flip_camera_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Flip camera",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Bottom label pill inside PiP
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Muted",
                            tint = DiscordRed,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text(
                        text = name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscordSecondaryBentoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        border = androidx.compose.foundation.BorderStroke(
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
private fun DiscordDockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    badgeDotColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = containerColor,
                modifier = Modifier
                    .size(width = 56.dp, height = 48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onClick)
                    .testTag(testTag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (badgeDotColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeDotColor)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

// --- Three-Dot Menu Modals: Pinned Messages, Call History & Recordings, Export as .md ---

@Composable
fun PinnedMessagesDialog(
    pinnedMessages: List<MessageEntity>,
    onUnpinMessage: (MessageEntity) -> Unit,
    onUnpinAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("pinned_messages_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pinned Messages (${pinnedMessages.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                if (pinnedMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pinned messages yet.\nLong-press any message in chat to pin it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pinnedMessages, key = { it.id }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = msg.senderName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = timeFormatter.format(Date(msg.createdAt)),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        val previewText = when (msg.messageType) {
                                            MessageType.VOICE -> "🎤 Voice message (${msg.content})"
                                            MessageType.STICKER -> "✨ Sticker (${msg.content})"
                                            MessageType.CALL_LOG -> "📞 ${msg.content}"
                                            else -> msg.content
                                        }
                                        Text(
                                            text = previewText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onUnpinMessage(msg) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Unpin message",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    onUnpinAll()
                                    onDismiss()
                                }
                        ) {
                            Text(
                                text = "Unpin All",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryAndRecordingsDialog(
    callLogs: List<MessageEntity>,
    autoRecordAllCalls: Boolean,
    onToggleAutoRecordAllCalls: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("call_history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Call History & Recordings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Record all calls automatically",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Or tap 'Record' during any individual call",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoRecordAllCalls,
                            onCheckedChange = onToggleAutoRecordAllCalls,
                            modifier = Modifier.testTag("auto_record_calls_switch")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                if (callLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No calls yet.\nTap the Call icon in the header to start a call.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(callLogs.reversed(), key = { it.id }) { callMsg ->
                            val metaParts = (callMsg.mediaMeta ?: "").split("|")
                            val modeName = metaParts.getOrNull(0) ?: CallMode.VOICE.name
                            val durationSec = metaParts.getOrNull(1)?.toIntOrNull() ?: 0
                            val recordingPath = metaParts.getOrNull(2).orEmpty()
                            val waveformCsv = metaParts.getOrNull(3).orEmpty()
                            val hasRecording = recordingPath.isNotBlank() || callMsg.content.contains("Recorded")

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                imageVector = if (modeName == CallMode.VIDEO.name) {
                                                    Icons.Default.Videocam
                                                } else {
                                                    Icons.Default.Call
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "${callMsg.senderName} • ${callMsg.content}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = timeFormatter.format(Date(callMsg.createdAt)),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (hasRecording) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFEF4444).copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = "● Recorded",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFEF4444),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (hasRecording) {
                                        val mins = durationSec.coerceAtLeast(1) / 60
                                        val secs = durationSec.coerceAtLeast(1) % 60
                                        val durLabel = String.format(Locale.getDefault(), "%d:%02d", mins, secs)
                                        val voiceMeta = "$recordingPath|${durationSec.coerceAtLeast(1)}|${waveformCsv.ifEmpty { "0.35,0.65,0.50,0.80,0.45,0.70,0.60,0.40" }}"
                                        VoiceMessageBubbleView(
                                            durationLabel = durLabel,
                                            mediaMeta = voiceMeta,
                                            isMe = false,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            accentColor = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportChatMarkdownDialog(
    space: SpaceEntity?,
    messages: List<MessageEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var copiedToast by remember { mutableStateOf(false) }

    val markdownText = remember(space, messages) {
        buildChatMarkdown(space, messages)
    }

    remember(markdownText) {
        try {
            val file = File(context.filesDir, "DuoSpace_Chat_Export.md")
            file.writeText(markdownText)
            file.absolutePath
        } catch (_: Exception) {
            "DuoSpace_Chat_Export.md"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("export_markdown_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export Chat (.md)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Saved as DuoSpace_Chat_Export.md",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = markdownText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("DuoSpace Chat Markdown", markdownText))
                                copiedToast = true
                            }
                            .testTag("copy_markdown_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (copiedToast) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Markdown",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copiedToast) "Copied .md!" else "Copy .md",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/markdown"
                                        putExtra(Intent.EXTRA_SUBJECT, "DuoSpace Chat Export (.md)")
                                        putExtra(Intent.EXTRA_TEXT, markdownText)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Export Chat Markdown (.md)")
                                    )
                                } catch (_: Exception) {
                                }
                            }
                            .testTag("share_markdown_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Markdown",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share .md",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildChatMarkdown(space: SpaceEntity?, messages: List<MessageEntity>): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val user1 = space?.creatorName ?: "Alex"
    val user2 = space?.partnerName ?: "Sam"
    return buildString {
        appendLine("# DuoSpace Chat Export")
        appendLine("- **Participants:** $user1 & $user2")
        appendLine("- **Exported At:** ${dateFormat.format(Date())}")
        appendLine()
        appendLine("---")
        appendLine()
        messages.filter { it.messageType != MessageType.DOODLE }.forEach { msg ->
            val timeStr = dateFormat.format(Date(msg.createdAt))
            val pinTag = if (msg.isPinned) " 📌 *(Pinned)*" else ""
            appendLine("### **${msg.senderName}** ($timeStr)$pinTag")
            if (!msg.replyToPreview.isNullOrBlank()) {
                appendLine("> ↪ *Replying to:* ${msg.replyToPreview}")
            }
            when (msg.messageType) {
                MessageType.VOICE -> appendLine("🎤 *Voice Message* (${msg.content})")
                MessageType.STICKER -> appendLine("✨ *Sticker:* ${msg.content}")
                MessageType.CALL_LOG -> appendLine("📞 *Call:* ${msg.content}")
                else -> appendLine(msg.content)
            }
            if (!msg.reaction.isNullOrBlank()) {
                appendLine("- **Reaction:** ${msg.reaction}")
            }
            appendLine()
        }
    }
}

@Composable
fun HiveIncomingCallOverlay(
    callerName: String,
    callerAvatarBlob: String,
    callMode: CallMode,
    pulseFraction: Float,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val displayCaller = callerName.ifBlank { "Partner" }
    val callTypeText = if (callMode == CallMode.VIDEO) {
        "Incoming Hive Video Call…"
    } else {
        "Incoming Hive Voice Call…"
    }

    Surface(
        color = Color(0xFF0A0B0E),
        modifier = Modifier
            .fillMaxSize()
            .testTag("incoming_call_overlay")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141824),
                            Color(0xFF0B0C10),
                            Color(0xFF08090C)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            // Top Status Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF1E2230).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF10B981).copy(alpha = 0.45f)
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .scale(0.85f + 0.3f * pulseFraction)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "HIVE PRIVATE CALL",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Center Caller Avatar + Pulsing Rings + Name
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(196.dp)
                ) {
                    // Outer pulsing ring
                    Box(
                        modifier = Modifier
                            .size(186.dp)
                            .scale(0.88f + 0.18f * pulseFraction)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.10f * (1f - 0.4f * pulseFraction)))
                            .border(
                                1.5.dp,
                                Color(0xFF10B981).copy(alpha = 0.28f * (1f - 0.4f * pulseFraction)),
                                CircleShape
                            )
                    )
                    // Inner pulsing ring
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .scale(0.92f + 0.12f * pulseFraction)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.16f))
                            .border(
                                1.5.dp,
                                Color(0xFF10B981).copy(alpha = 0.45f),
                                CircleShape
                            )
                    )

                    UserAvatarCircle(
                        name = displayCaller,
                        avatarBlobName = callerAvatarBlob,
                        sizeDp = 112,
                        containerColor = Color(0xFF252836),
                        contentColor = Color.White
                    )
                }

                Text(
                    text = displayCaller,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = callTypeText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA1A1AA),
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Decline & Answer Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDecline)
                            .testTag("incoming_call_decline_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "Decline",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFCA5A5)
                    )
                }

                // Answer Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(74.dp)
                            .scale(0.96f + 0.06f * pulseFraction)
                            .clip(CircleShape)
                            .clickable(onClick = onAnswer)
                            .testTag("incoming_call_answer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (callMode == CallMode.VIDEO) {
                                    Icons.Default.Videocam
                                } else {
                                    Icons.Default.Call
                                },
                                contentDescription = "Answer Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "Answer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6EE7B7)
                    )
                }
            }
        }
    }
}

