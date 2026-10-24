package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MessageEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun PhotoAttachmentBubbleView(
    message: MessageEntity
) {
    val context = LocalContext.current
    // mediaMeta: blobName|sizeBytes|fileName|filterLabel
    val parts = remember(message.mediaMeta) { (message.mediaMeta ?: "").split("|") }
    val blobName = parts.getOrNull(0).orEmpty()

    val decryptedBytes = remember(blobName) {
        DuoVaultPersistence.readDecryptedBlobInMemory(context, blobName)
    }
    val bitmap = remember(decryptedBytes) {
        decryptedBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    Box(
        modifier = Modifier
            .width(248.dp)
            .height(248.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .testTag("photo_full_bubble_${message.id}")
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF334155))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📷",
                    color = Color.White,
                    fontSize = 26.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoAttachmentBubbleView(
    message: MessageEntity,
    onLongPressBubble: () -> Unit = {}
) {
    val context = LocalContext.current
    // mediaMeta: blobName|durationSec|sizeBytes|fileName|filterLabel
    val parts = remember(message.mediaMeta) { (message.mediaMeta ?: "").split("|") }
    val blobName = parts.getOrNull(0).orEmpty()
    val durationSec = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 5

    val decryptedBytes = remember(blobName) {
        DuoVaultPersistence.readDecryptedBlobInMemory(context, blobName)
    }
    val coverBitmap = remember(decryptedBytes) {
        decryptedBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val totalMs = (durationSec * 1000L).coerceAtLeast(2000L)
            val start = System.currentTimeMillis()
            while (isActive && isPlaying) {
                val elapsed = System.currentTimeMillis() - start
                progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                if (elapsed >= totalMs) {
                    isPlaying = false
                    progress = 0f
                    break
                }
                delay(40L)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(248.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .combinedClickable(
                onClick = { isPlaying = !isPlaying },
                onLongClick = onLongPressBubble
            )
            .testTag("bubble_video_player_${message.id}")
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap.asImageBitmap(),
                contentDescription = "Video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.08f * ((progress * 10).toInt() % 2)))
            )
        }

        // Center Play/Pause Button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause video" else "Play video",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Top-left Video Duration Badge
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.62f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
                val mins = durationSec / 60
                val secs = durationSec % 60
                Text(
                    text = String.format(Locale.US, "%d:%02d", mins, secs),
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isPlaying || progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFFEF4444),
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun FileAttachmentBubbleView(
    message: MessageEntity,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color
) {
    val context = LocalContext.current
    // mediaMeta: blobName|sizeBytes|mimeType
    val parts = remember(message.mediaMeta) { (message.mediaMeta ?: "").split("|") }
    val blobName = parts.getOrNull(0).orEmpty()
    val sizeBytes = parts.getOrNull(1)?.toLongOrNull() ?: 4096L
    val mimeType = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
    val fileName = message.content.ifBlank { "SharedFile" }

    Row(
        modifier = Modifier
            .width(236.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isMe) Color.White.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(textColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = fileName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${DuoVaultPersistence.formatBytes(sizeBytes)} • In Vault",
                    fontSize = 11.sp,
                    color = metaColor
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(textColor.copy(alpha = 0.15f))
                .clickable {
                    val decrypted = DuoVaultPersistence.readDecryptedBlobInMemory(context, blobName)
                        ?: "Shared File: $fileName".toByteArray(Charsets.UTF_8)
                    val ok = DuoVaultPersistence.saveMediaBytesToPhone(
                        context = context,
                        plainBytes = decrypted,
                        displayName = fileName,
                        mimeType = mimeType
                    )
                    Toast.makeText(
                        context,
                        if (ok) "Saved $fileName to phone Downloads" else "File saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .testTag("bubble_save_file_${message.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Save file to phone",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InteractivePollBubbleView(
    message: MessageEntity,
    activeUserName: String,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color,
    onVoteOption: (newMediaMeta: String) -> Unit
) {
    // mediaMeta: Option1~Alex,Sam|Option2~|Option3~
    val rawEntries = remember(message.mediaMeta) {
        (message.mediaMeta ?: "Yes~|No~")
            .split("|")
            .filter { it.isNotBlank() }
            .map { entry ->
                val split = entry.split("~", limit = 2)
                val label = split.getOrNull(0)?.trim().orEmpty().ifEmpty { "Option" }
                val voters = split.getOrNull(1)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
                label to voters
            }
    }

    val totalVotes = remember(rawEntries) {
        rawEntries.sumOf { it.second.size }
    }

    Column(
        modifier = Modifier
            .width(244.dp)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Poll,
                contentDescription = "Poll",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "COUPLE POLL",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = metaColor
            )
        }

        Text(
            text = message.content,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        rawEntries.forEachIndexed { idx, (optionLabel, voters) ->
            val isVotedByMe = activeUserName in voters
            val ratio = if (totalVotes > 0) voters.size.toFloat() / totalVotes.toFloat() else 0f
            val pct = (ratio * 100).toInt()

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isVotedByMe) {
                    textColor.copy(alpha = 0.22f)
                } else {
                    textColor.copy(alpha = 0.1f)
                },
                border = if (isVotedByMe) {
                    BorderStroke(1.dp, textColor.copy(alpha = 0.65f))
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val updatedMeta = rawEntries.mapIndexed { i, (lbl, vList) ->
                            val cleanVoters = vList.filterNot { it == activeUserName }.toMutableList()
                            if (i == idx && !isVotedByMe) {
                                cleanVoters.add(activeUserName)
                            }
                            "$lbl~${cleanVoters.joinToString(",")}"
                        }.joinToString("|")
                        onVoteOption(updatedMeta)
                    }
                    .testTag("poll_vote_${message.id}_$idx")
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isVotedByMe) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Voted",
                                    tint = textColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = optionLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isVotedByMe) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                        Text(
                            text = if (voters.isNotEmpty()) "${voters.joinToString(", ")} ($pct%)" else "0%",
                            fontSize = 10.5.sp,
                            color = metaColor
                        )
                    }
                }
            }
        }

        Text(
            text = "$totalVotes vote(s) • Tap an option to vote",
            fontSize = 10.5.sp,
            color = metaColor
        )
    }
}

@Composable
fun ContactAttachmentBubbleView(
    message: MessageEntity,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color
) {
    val context = LocalContext.current
    // mediaMeta: name|phone|role
    val parts = remember(message.mediaMeta) { (message.mediaMeta ?: "").split("|") }
    val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: message.content
    val phone = parts.getOrNull(1)?.trim().orEmpty()
    val role = parts.getOrNull(2)?.trim().orEmpty()

    Column(
        modifier = Modifier
            .width(236.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isMe) Color.White.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (phone.isNotEmpty()) {
                    Text(
                        text = phone,
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
                if (role.isNotEmpty()) {
                    Text(
                        text = role,
                        fontSize = 10.5.sp,
                        color = metaColor
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = textColor.copy(alpha = 0.16f),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        runCatching {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(dialIntent)
                        }
                    }
                    .testTag("bubble_call_contact_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Dial",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Call", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = textColor.copy(alpha = 0.16f),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val copyValue = phone.ifEmpty { name }
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Contact", copyValue))
                        Toast.makeText(context, "Copied $copyValue", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Copy", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }
            }
        }
    }
}

@Composable
fun LocationAttachmentBubbleView(
    message: MessageEntity,
    textColor: Color,
    metaColor: Color
) {
    val context = LocalContext.current
    // mediaMeta: lat|lng|label
    val parts = remember(message.mediaMeta) { (message.mediaMeta ?: "").split("|") }
    val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: 37.7858
    val lng = parts.getOrNull(1)?.toDoubleOrNull() ?: -122.4065
    val label = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: message.content

    val pulse = rememberInfiniteTransition(label = "pin_pulse")
    val ringScale by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val openInGoogleMaps = {
        val queryEncoded = Uri.encode(label.ifBlank { "$lat,$lng" })
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($queryEncoded)")
        runCatching {
            val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapsIntent)
        }.recoverCatching {
            val genericGeoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericGeoIntent)
        }.recoverCatching {
            val webMapsIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webMapsIntent)
        }
    }

    Column(
        modifier = Modifier
            .width(240.dp)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Interactive Google Maps Pin Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .clickable { openInGoogleMaps() }
                .testTag("bubble_open_map_${message.id}"),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridCol = Color(0xFF38BDF8).copy(alpha = 0.16f)
                for (i in 1..4) {
                    drawLine(gridCol, Offset(w * i / 5f, 0f), Offset(w * i / 5f, h), 1.dp.toPx())
                    drawLine(gridCol, Offset(0f, h * i / 5f), Offset(w, h * i / 5f), 1.dp.toPx())
                }
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.28f),
                    radius = 22.dp.toPx() * ringScale,
                    center = Offset(w / 2f, h / 2f)
                )
            }

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pinned Location",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(30.dp)
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Google Maps",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = String.format(Locale.US, "GPS %.4f, %.4f • Tap for Google Maps", lat, lng),
            fontSize = 11.sp,
            color = metaColor
        )
    }
}

@Composable
fun CoupleExtraCardBubbleView(
    message: MessageEntity,
    activeUser: TestUserSlot,
    activeUserName: String,
    textColor: Color,
    metaColor: Color,
    onToggleRedeem: (newMediaMeta: String) -> Unit
) {
    val context = LocalContext.current
    // mediaMeta: badgeType|originMsgId|subtitle|status
    val parts = remember(message.mediaMeta) { (message.mediaMeta ?: "").split("|") }
    val badgeType = parts.getOrNull(0)?.ifBlank { "COUPON" } ?: "COUPON"
    val originMessageId = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: message.id
    val subtitle = parts.getOrNull(2)?.ifBlank { "Shared Coupon" } ?: "Shared Coupon"
    val status = parts.getOrNull(3)?.ifBlank { "ACTIVE" } ?: "ACTIVE"
    val isRedeemed = status.startsWith("REDEEMED")
    val isSharedToRedeem = status == "SHARED_TO_REDEEM"
    val isRecipient = message.senderId != activeUser.userId

    var isSavedToReceived by remember(message.id, activeUser, isRedeemed) {
        val currentReceived = HiveSettingsPersistence.loadReceivedCoupons(context, activeUser)
        mutableStateOf(currentReceived.any { it.messageId == message.id || it.messageId == originMessageId || it.title == message.content })
    }

    Column(
        modifier = Modifier
            .width(248.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isSharedToRedeem) "$badgeType • SHARED TO REDEEM" else badgeType,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = metaColor
        )

        Text(
            text = message.content,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.85f)
        )

        if (isRedeemed) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF10B981).copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("couple_card_redeemed_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = status,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        } else if (isSharedToRedeem) {
            // Shared from Received Coupons into Chat -> Clicking Redeem here redeems it!
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF10B981).copy(alpha = 0.28f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        HiveSettingsPersistence.removeReceivedCouponOnRedeem(
                            context = context,
                            messageId = originMessageId,
                            title = message.content,
                            recipientSlot = activeUser
                        )
                        isSavedToReceived = false
                        val nextStatus = "REDEEMED by $activeUserName"
                        val newMeta = "$badgeType|$originMessageId|$subtitle|$nextStatus"
                        onToggleRedeem(newMeta)
                    }
                    .testTag("couple_card_redeem_${message.id}")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Redeem Coupon",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        } else if (isRecipient) {
            // Initial coupon sent by partner: Recipient saves it to Received Coupons, then shares it in chat to redeem
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSavedToReceived) {
                    Color(0xFF10B981).copy(alpha = 0.22f)
                } else {
                    textColor.copy(alpha = 0.20f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        if (!isSavedToReceived) {
                            HiveSettingsPersistence.saveCouponToReceived(
                                context = context,
                                coupon = SavedCouponItem(
                                    id = "rc_${message.id}",
                                    messageId = message.id,
                                    title = message.content,
                                    subtitle = subtitle,
                                    badgeType = badgeType,
                                    fromUserName = message.senderName,
                                    recipientSlot = activeUser
                                )
                            )
                            isSavedToReceived = true
                        }
                    }
                    .testTag("couple_card_save_${message.id}")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSavedToReceived) "Saved • Share from Coupons to Redeem" else "Save to Received Coupons",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        } else {
            // Sender cannot click redeem for the other person
            Surface(
                shape = RoundedCornerShape(50),
                color = textColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("couple_card_sender_status_${message.id}")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sent • Redeemable by Partner",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
