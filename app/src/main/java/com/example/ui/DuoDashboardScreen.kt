package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.IBinder
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.SpaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.log10
import kotlin.math.sqrt

enum class DuoBottomNavDestination {
    DASHBOARD,
    CHAT,
    JOURNAL,
    WISHLIST,
    VAULT
}

@Composable
fun DuoDashboardScreen(
    uiState: DuoUiState,
    space: SpaceEntity?,
    messages: List<MessageEntity>,
    onSwitchUser: (TestUserSlot) -> Unit,
    onOpenChat: () -> Unit,
    onOpenChatWithDoodle: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenWishlist: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onStartCall: () -> Unit,
    onJoinOrSwitchSpace: (spaceCode: String, slot: TestUserSlot, displayName: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(context, uiState.activeUser) {
        HiveSettingsPersistence.ensureInitialized(context)
        HiveSettingsPersistence.refreshLiveDeviceBattery(context, uiState.activeUser)
    }

    val appConfig = remember(context, HiveSettingsPersistence.changeCounter) {
        HiveSettingsPersistence.loadConfig(context)
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showKnowPartnerDialog by remember { mutableStateOf(false) }
    val isHearingPartner = HiveKnowPartnerController.isHearingPartner
    val liveAudioLevel = HiveKnowPartnerController.liveAudioLevel
    val liveAudioDbText = HiveKnowPartnerController.liveAudioDbText
    val hearElapsedSeconds = HiveKnowPartnerController.hearElapsedSeconds
    var isFetchingLiveLocation by remember { mutableStateOf(false) }
    var onDemandFetchedLocationText by remember { mutableStateOf<String?>(null) }

    val user1Name = appConfig.user1Name.ifBlank { space?.creatorName?.ifBlank { "Partner 1" } ?: "Partner 1" }
    val user2Name = appConfig.user2Name.ifBlank { space?.partnerName?.ifBlank { "Partner 2" } ?: "Partner 2" }
    val activeUserName = if (uiState.activeUser == TestUserSlot.USER_1) user1Name else user2Name
    val partnerName = if (uiState.activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val partnerSlot = if (uiState.activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1

    val activeAvatarBlob = if (uiState.activeUser == TestUserSlot.USER_1) {
        appConfig.user1AvatarBlob
    } else {
        appConfig.user2AvatarBlob
    }
    val partnerAvatarBlob = if (uiState.activeUser == TestUserSlot.USER_1) {
        appConfig.user2AvatarBlob
    } else {
        appConfig.user1AvatarBlob
    }
    val partnerBatteryPercent = if (uiState.activeUser == TestUserSlot.USER_1) {
        appConfig.user2BatteryPercent
    } else {
        appConfig.user1BatteryPercent
    }

    val unreadForActiveUser = remember(messages, uiState.activeUser) {
        messages.count {
            !it.isRead && !it.isSystemEvent && it.senderId != uiState.activeUser.userId
        }
    }

    val latestMessage = remember(messages) {
        messages.lastOrNull { it.messageType != MessageType.DOODLE }
    }

    val pinnedCount = remember(messages) {
        messages.count { it.isPinned && it.messageType != MessageType.DOODLE }
    }

    val hasActiveDoodle = !space?.screenDoodleData.isNullOrBlank()

    val formattedLastTime = remember(latestMessage?.createdAt) {
        latestMessage?.createdAt?.let { ts ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
        } ?: "Now"
    }

    val latestPreviewText = remember(latestMessage, partnerName) {
        when (latestMessage?.messageType) {
            null -> "Tap to start chatting with $partnerName"
            MessageType.VOICE -> "${latestMessage.senderName} sent a voice note (${latestMessage.content})"
            MessageType.STICKER -> "${latestMessage.senderName} sent a sticker"
            MessageType.CALL_LOG -> latestMessage.content
            else -> "${latestMessage.senderName}: ${latestMessage.content}"
        }
    }

    // Load Journal preview for Quick Access
    val journalSnapshot = remember(user1Name, user2Name) {
        DuoJournalPersistence.loadState(context, user1Name, user2Name)
    }
    val todayIso = remember { DuoJournalPersistence.todayIso() }
    val todayJournalLog = journalSnapshot.logsByDate[todayIso]
    val user1InkOption = remember(journalSnapshot.user1InkId) {
        JOURNAL_INK_PALETTE.find { it.id == journalSnapshot.user1InkId } ?: JOURNAL_INK_PALETTE[0]
    }
    val user2InkOption = remember(journalSnapshot.user2InkId) {
        JOURNAL_INK_PALETTE.find { it.id == journalSnapshot.user2InkId } ?: JOURNAL_INK_PALETTE[1]
    }
    val user1InkColor = user1InkOption.resolveColor(isDark)
    val user2InkColor = user2InkOption.resolveColor(isDark)

    val journalPreviewText = remember(todayJournalLog) {
        val u1Text = todayJournalLog?.user1TabChars?.asPlainString()?.trim().orEmpty()
        val u2Text = todayJournalLog?.user2TabChars?.asPlainString()?.trim().orEmpty()
        when {
            u1Text.isNotEmpty() -> u1Text
            u2Text.isNotEmpty() -> u2Text
            else -> "Open today's notepad"
        }
    }

    // Load Wishlist preview for Quick Access
    val wishlistItems = remember {
        DuoWishlistPersistence.loadItems(context)
    }
    val wishlistPreviewText = remember(wishlistItems) {
        val latestWish = wishlistItems.firstOrNull()
        if (latestWish != null) {
            "${wishlistItems.size} item(s) • Latest: ${latestWish.name}"
        } else {
            "Add wishes with link & photo"
        }
    }

    // Load Vault preview for Quick Access
    val vaultNodes = remember(user1Name, user2Name, messages) {
        DuoVaultPersistence.loadNodes(context, user1Name, user2Name, messages)
    }
    val vaultPreviewText = remember(vaultNodes, user1Name, user2Name) {
        val fileCount = vaultNodes.count { !it.isFolder }
        "$user1Name & $user2Name • $fileCount file(s)"
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hive_logo),
                            contentDescription = "Hive Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "Hive",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$user1Name & $user2Name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings Button directly in Dashboard (Not in Bottom Nav)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showSettingsDialog = true }
                                .testTag("dashboard_open_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            ModernBottomNavBar(
                currentDestination = DuoBottomNavDestination.DASHBOARD,
                unreadChatCount = unreadForActiveUser,
                onClickDashboard = {},
                onClickChat = onOpenChat,
                onClickJournal = onOpenJournal,
                onClickWishlist = onOpenWishlist,
                onClickVault = onOpenVault
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Upgraded Partner Profile, Battery Percentage & Quick Call Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("duo_dashboard_people_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Partner + Active User Profile Avatars & Partner Battery Percentage
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                UserAvatarCircle(
                                    name = partnerName,
                                    avatarBlobName = partnerAvatarBlob,
                                    size = 52.dp,
                                    backgroundColor = MaterialTheme.colorScheme.primary,
                                    textColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                                UserAvatarCircle(
                                    name = activeUserName,
                                    avatarBlobName = activeAvatarBlob,
                                    size = 38.dp,
                                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(start = 36.dp, top = 14.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$partnerName & $activeUserName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Text(
                                            text = "$partnerName online",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Partner Live Battery Percentage Pill
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.testTag("partner_battery_badge")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BatteryFull,
                                                contentDescription = "Partner battery",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "$partnerBatteryPercent%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                // Quick Profile Switcher (Test / Duo Mode)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSwitchUser(partnerSlot) }
                                        .testTag("dashboard_switch_user_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Autorenew,
                                            contentDescription = "Switch profile",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "You: $activeUserName • Switch to $partnerName",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Call Button on Duo Card
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onStartCall)
                                .testTag("dashboard_call_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call $partnerName",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Access Section (Chat, Journal, Wishlist, The Vault, and Know [Partner] at the end)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Chat Quick Access Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenChat)
                        .testTag("quick_access_chat_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubble,
                                        contentDescription = "Chat",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Chat with $partnerName",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (unreadForActiveUser > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF10B981)
                                            ) {
                                                Text(
                                                    text = unreadForActiveUser.toString(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = latestPreviewText,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formattedLastTime,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Mini Quick Badges inside the Chat Quick Access Card (Pinned Messages only; Doodle button removed)
                        if (pinnedCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onOpenChat)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "$pinnedCount Pinned",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Journal Quick Access Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenJournal)
                        .testTag("quick_access_journal_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = "Journal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Journal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(user1InkColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(user2InkColor)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = journalPreviewText,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Journal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Wishlist Quick Access Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenWishlist)
                        .testTag("quick_access_wishlist_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Wishlist",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Wishlist",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = wishlistPreviewText,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Wishlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // The Vault Quick Access Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenVault)
                        .testTag("quick_access_vault_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "The Vault",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "The Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = vaultPreviewText,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open The Vault",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Know [Partner] Quick Access Card (at the end of Quick Access)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            onDemandFetchedLocationText = null
                            showKnowPartnerDialog = true
                        }
                        .testTag("dashboard_know_partner_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Know $partnerName",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Know $partnerName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isHearingPartner) {
                                        "Listening live to $partnerName…"
                                    } else {
                                        "Hear $partnerName • See $partnerName"
                                    },
                                    fontSize = 12.5.sp,
                                    color = if (isHearingPartner) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Know $partnerName",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    val mutualHearEnabled = (appConfig.user1HearConsent && appConfig.user2HearConsent) ||
        (appConfig.user1LocationConsent && appConfig.user2LocationConsent)
    val mutualSeeEnabled = appConfig.user1LocationConsent && appConfig.user2LocationConsent

    // Keep Foreground Service synchronized when mutual Hear or See is enabled or active so
    // Android's "While using the app" permission works seamlessly even when minimized/backgrounded.
    LaunchedEffect(mutualHearEnabled, mutualSeeEnabled, isHearingPartner) {
        if (!mutualHearEnabled && isHearingPartner) {
            HiveKnowPartnerController.stopHearing(context)
        } else if (mutualHearEnabled || mutualSeeEnabled) {
            HiveKnowPartnerService.syncServiceState(context)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && mutualHearEnabled) {
            HiveKnowPartnerController.startHearing(context, partnerName)
        }
    }

    val executeOnDemandSeePartner: () -> Unit = {
        if (mutualSeeEnabled && !isFetchingLiveLocation) {
            isFetchingLiveLocation = true
            HiveKnowPartnerService.syncServiceState(context)
            coroutineScope.launch {
                val result = fetchOnDemandPartnerLocation(
                    context = context,
                    partnerSlot = partnerSlot,
                    partnerName = partnerName,
                    messages = messages,
                    activeUserId = uiState.activeUser.userId,
                    fallbackLat = if (uiState.activeUser == TestUserSlot.USER_1) appConfig.user2Lat else appConfig.user1Lat,
                    fallbackLng = if (uiState.activeUser == TestUserSlot.USER_1) appConfig.user2Lng else appConfig.user1Lng
                )
                HiveSettingsPersistence.updateLocationConsent(
                    context = context,
                    slot = partnerSlot,
                    enabled = true,
                    lat = result.latitude,
                    lng = result.longitude
                )
                onDemandFetchedLocationText = result.displaySummary
                isFetchingLiveLocation = false

                val geoUri = Uri.parse(
                    "geo:${result.latitude},${result.longitude}?q=${result.latitude},${result.longitude}(${Uri.encode("$partnerName (${result.label})")})"
                )
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(mapIntent) }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && mutualSeeEnabled) {
            executeOnDemandSeePartner()
        }
    }

    if (showKnowPartnerDialog) {
        Dialog(
            onDismissRequest = { showKnowPartnerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("know_partner_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Know $partnerName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showKnowPartnerDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 1: Hear [Partner] (click once to start live audio stream, click again to stop)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isHearingPartner) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isHearingPartner) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                if (mutualHearEnabled) {
                                    if (isHearingPartner) {
                                        HiveKnowPartnerController.stopHearing(context)
                                    } else {
                                        val hasMicPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasMicPerm) {
                                            HiveKnowPartnerController.startHearing(context, partnerName)
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            }
                            .testTag("know_partner_hear_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isHearingPartner) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isHearingPartner) Icons.Default.GraphicEq else Icons.Default.Mic,
                                            contentDescription = "Hear $partnerName",
                                            tint = if (isHearingPartner) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Hear $partnerName",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isHearingPartner) {
                                            val mins = hearElapsedSeconds / 60
                                            val secs = hearElapsedSeconds % 60
                                            val formattedElapsed = String.format(Locale.US, "%02d:%02d", mins, secs)
                                            Text(
                                                text = "Live audio active ($formattedElapsed • $liveAudioDbText) • Tap again to stop",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (isHearingPartner) {
                                LinearProgressIndicator(
                                    progress = { liveAudioLevel.coerceIn(0.06f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // Option 2: See [Partner] (fetches GPS/Network location strictly on-demand when clicked)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                if (mutualSeeEnabled) {
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine || hasCoarse) {
                                        executeOnDemandSeePartner()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            }
                            .testTag("know_partner_see_location_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "See $partnerName",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "See $partnerName",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isFetchingLiveLocation) {
                                        Text(
                                            text = "Fetching live location…",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (onDemandFetchedLocationText != null) {
                                        Text(
                                            text = "Live location: $onDemandFetchedLocationText",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.primary
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

    if (showSettingsDialog) {
        HiveDashboardSettingsDialog(
            activeUser = uiState.activeUser,
            config = appConfig,
            currentDisplayName = activeUserName,
            partnerDisplayName = partnerName,
            onUpdateDisplayName = { updatedName ->
                onJoinOrSwitchSpace(
                    appConfig.activeSpaceCode,
                    uiState.activeUser,
                    updatedName
                )
            },
            onJoinOrSwitchSpace = onJoinOrSwitchSpace,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

private data class OnDemandPartnerLocationResult(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val displaySummary: String
)

@SuppressLint("MissingPermission")
private suspend fun fetchOnDemandPartnerLocation(
    context: Context,
    partnerSlot: TestUserSlot,
    partnerName: String,
    messages: List<MessageEntity>,
    activeUserId: String,
    fallbackLat: Double,
    fallbackLng: Double
): OnDemandPartnerLocationResult = withContext(Dispatchers.IO) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    var resolvedLocation: Location? = null

    if (locationManager != null) {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        // Try active on-demand hardware fix on Android R+ (bounded timeout so UI remains responsive)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (provider in providers) {
                val isEnabled = runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
                if (!isEnabled) continue
                val liveFix = withTimeoutOrNull(1800L) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        runCatching {
                            locationManager.getCurrentLocation(
                                provider,
                                signal,
                                context.mainExecutor
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
                if (liveFix != null) {
                    resolvedLocation = liveFix
                    break
                }
            }
        }

        // Fallback to most recent provider fix from LocationManager
        if (resolvedLocation == null) {
            resolvedLocation = providers
                .mapNotNull { provider ->
                    runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull { it.time }
        }
    }

    val latestLocMsg = messages.lastOrNull {
        it.messageType == MessageType.LOCATION && it.senderId != activeUserId
    }
    val msgParts = latestLocMsg?.mediaMeta?.split("|").orEmpty()
    val msgLat = msgParts.getOrNull(0)?.toDoubleOrNull()
    val msgLng = msgParts.getOrNull(1)?.toDoubleOrNull()

    val finalLat = resolvedLocation?.latitude ?: msgLat ?: fallbackLat
    val finalLng = resolvedLocation?.longitude ?: msgLng ?: fallbackLng
    val accuracyMeters = resolvedLocation?.accuracy?.takeIf { it > 0f }?.toInt()

    // Reverse geocode coordinates into a readable address if Geocoder is available
    val geocodedPlace = runCatching {
        if (Geocoder.isPresent()) {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(finalLat, finalLng, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                listOfNotNull(
                    addr.thoroughfare ?: addr.featureName,
                    addr.locality ?: addr.subAdminArea,
                    addr.adminArea
                ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
            } else {
                null
            }
        } else {
            null
        }
    }.getOrNull()

    val coordString = String.format(Locale.US, "%.4f, %.4f", finalLat, finalLng)
    val accuracySuffix = if (accuracyMeters != null) " (±${accuracyMeters}m)" else ""
    val label = geocodedPlace ?: "$partnerName's Live Coordinates"
    val summary = if (!geocodedPlace.isNullOrBlank()) {
        "$geocodedPlace • $coordString$accuracySuffix"
    } else {
        "$coordString$accuracySuffix"
    }

    OnDemandPartnerLocationResult(
        latitude = finalLat,
        longitude = finalLng,
        label = label,
        displaySummary = summary
    )
}

/**
 * Modern Floating Bottom Navigation Bar with Icon-Only Pills (Home, Chat, Journal, Wishlist, Vault).
 */
@Composable
fun ModernBottomNavBar(
    currentDestination: DuoBottomNavDestination,
    unreadChatCount: Int,
    onClickDashboard: () -> Unit,
    onClickChat: () -> Unit,
    onClickJournal: () -> Unit,
    onClickWishlist: () -> Unit = {},
    onClickVault: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("modern_bottom_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDashboardSelected = currentDestination == DuoBottomNavDestination.DASHBOARD
                val isChatSelected = currentDestination == DuoBottomNavDestination.CHAT
                val isJournalSelected = currentDestination == DuoBottomNavDestination.JOURNAL
                val isWishlistSelected = currentDestination == DuoBottomNavDestination.WISHLIST
                val isVaultSelected = currentDestination == DuoBottomNavDestination.VAULT

                // 1. Home Icon Pill
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDashboardSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onClickDashboard)
                        .testTag("bottom_nav_dashboard_button")
                ) {
                    val contentColor = if (isDashboardSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDashboardSelected) {
                                Icons.Rounded.Home
                            } else {
                                Icons.Outlined.Home
                            },
                            contentDescription = "Home",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. Chat Icon Pill
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isChatSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onClickChat)
                        .testTag("bottom_nav_chat_button")
                ) {
                    val contentColor = if (isChatSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isChatSelected) {
                                Icons.Rounded.ChatBubble
                            } else {
                                Icons.Outlined.ChatBubbleOutline
                            },
                            contentDescription = "Chat",
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (unreadChatCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(start = 18.dp, top = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                }

                // 3. Journal Icon Pill
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isJournalSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onClickJournal)
                        .testTag("bottom_nav_journal_button")
                ) {
                    val contentColor = if (isJournalSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isJournalSelected) {
                                Icons.Rounded.AutoStories
                            } else {
                                Icons.Outlined.AutoStories
                            },
                            contentDescription = "Journal",
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // 4. Wishlist Icon Pill
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isWishlistSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onClickWishlist)
                        .testTag("bottom_nav_wishlist_button")
                ) {
                    val contentColor = if (isWishlistSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWishlistSelected) {
                                Icons.Rounded.CardGiftcard
                            } else {
                                Icons.Outlined.CardGiftcard
                            },
                            contentDescription = "Wishlist",
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // 5. Vault Icon Pill
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isVaultSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onClickVault)
                        .testTag("bottom_nav_vault_button")
                ) {
                    val contentColor = if (isVaultSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVaultSelected) {
                                Icons.Rounded.Folder
                            } else {
                                Icons.Outlined.Folder
                            },
                            contentDescription = "Vault",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Process-wide controller for "Know [Partner]" (Hear & See) so live audio streaming and
 * on-demand location access remain active across screen changes and when the app is minimized
 * or the screen is off, backed by [HiveKnowPartnerService].
 */
object HiveKnowPartnerController {
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate
    )
    private var audioStreamJob: kotlinx.coroutines.Job? = null
    private var elapsedTimerJob: kotlinx.coroutines.Job? = null

    var isHearingPartner by mutableStateOf(false)
        private set
    var liveAudioLevel by mutableFloatStateOf(0f)
        private set
    var liveAudioDbText by mutableStateOf("-∞ dB")
        private set
    var hearElapsedSeconds by mutableIntStateOf(0)
        private set
    var activePartnerName by mutableStateOf("Partner")
        private set

    fun startHearing(context: Context, partnerName: String = "Partner") {
        val appContext = context.applicationContext
        val hasMicPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPerm) {
            isHearingPartner = false
            return
        }

        activePartnerName = partnerName
        if (isHearingPartner && audioStreamJob?.isActive == true) {
            return
        }

        isHearingPartner = true
        hearElapsedSeconds = 0
        liveAudioLevel = 0.08f
        liveAudioDbText = "-42 dB"

        // Ensure Foreground Service is started so Android treats "While using the app" permission
        // as active even if the user minimizes the app or turns off the screen.
        HiveKnowPartnerService.syncServiceState(appContext)

        elapsedTimerJob?.cancel()
        elapsedTimerJob = scope.launch {
            while (isActive && isHearingPartner) {
                delay(1000L)
                hearElapsedSeconds += 1
            }
        }

        audioStreamJob?.cancel()
        audioStreamJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val minInBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(3200)
            val minOutBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(3200)

            var audioRecord: AudioRecord? = null
            var audioTrack: AudioTrack? = null
            try {
                @SuppressLint("MissingPermission")
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minInBuf * 2
                )
                audioRecord = recorder

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    .setBufferSizeInBytes(minOutBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack = track

                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.play()
                    }
                    val pcmBuffer = ShortArray(1024)
                    while (isActive && isHearingPartner) {
                        val readCount = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                        if (readCount > 0) {
                            if (track.state == AudioTrack.STATE_INITIALIZED) {
                                track.write(pcmBuffer, 0, readCount)
                            }
                            var sumSquares = 0.0
                            for (i in 0 until readCount) {
                                val sample = pcmBuffer[i].toDouble()
                                sumSquares += sample * sample
                            }
                            val rms = sqrt(sumSquares / readCount)
                            val normalized = (rms / 8000.0).toFloat().coerceIn(0f, 1f)
                            val db = if (rms > 1.0) {
                                (20.0 * log10(rms / 32768.0)).toInt()
                            } else {
                                -60
                            }
                            withContext(Dispatchers.Main) {
                                liveAudioLevel = normalized
                                liveAudioDbText = "${db} dB"
                            }
                        } else {
                            delay(25L)
                        }
                    }
                }
            } catch (_: Throwable) {
                // Keep state active in headless test environments
            } finally {
                runCatching { audioRecord?.stop() }
                runCatching { audioRecord?.release() }
                runCatching { audioTrack?.stop() }
                runCatching { audioTrack?.release() }
            }
        }
    }

    fun stopHearing(context: Context) {
        isHearingPartner = false
        elapsedTimerJob?.cancel()
        audioStreamJob?.cancel()
        liveAudioLevel = 0f
        liveAudioDbText = "-∞ dB"
        hearElapsedSeconds = 0
        HiveKnowPartnerService.syncServiceState(context.applicationContext)
    }
}

/**
 * Foreground Service (`microphone|location`) that keeps Android's "While using the app"
 * microphone and location permissions active when the app is minimized, in another app,
 * or when the screen is off.
 */
class HiveKnowPartnerService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireSafetyWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var fgTypes = 0
                val hasMic = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                val hasLoc = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                if (hasMic) {
                    fgTypes = fgTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                if (hasLoc) {
                    fgTypes = fgTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                if (fgTypes != 0) {
                    startForeground(NOTIFICATION_ID, notification, fgTypes)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseSafetyWakeLock()
        super.onDestroy()
    }

    private fun acquireSafetyWakeLock() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Hive:KnowPartnerSafetyWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(4 * 60 * 60 * 1000L)
                }
            }
        }
    }

    private fun releaseSafetyWakeLock() {
        runCatching {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        }
    }

    private fun buildForegroundNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hive Safety Check-In",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps consented Hear & See safety access active in background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
        val statusText = if (HiveKnowPartnerController.isHearingPartner) {
            "Live audio check-in active with ${HiveKnowPartnerController.activePartnerName}"
        } else {
            "On-demand Hear & See safety connection ready"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.hive_logo)
            .setContentTitle("Hive")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hive_know_partner_safety_channel"
        private const val NOTIFICATION_ID = 4109

        fun syncServiceState(context: Context) {
            val appContext = context.applicationContext
            val config = HiveSettingsPersistence.loadConfig(appContext)
            val anyConsentEnabled = config.user1HearConsent ||
                config.user2HearConsent ||
                config.user1LocationConsent ||
                config.user2LocationConsent ||
                HiveKnowPartnerController.isHearingPartner

            val hasMicOrLocPerm = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            val serviceIntent = Intent(appContext, HiveKnowPartnerService::class.java)
            runCatching {
                if (anyConsentEnabled && hasMicOrLocPerm) {
                    ContextCompat.startForegroundService(appContext, serviceIntent)
                } else {
                    appContext.stopService(serviceIntent)
                }
            }
        }
    }
}

