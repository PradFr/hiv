package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.MessageType
import com.example.ui.CallMode
import com.example.ui.ChatSpaceScreen
import com.example.ui.DuoBottomNavDestination
import com.example.ui.DuoCallOverlay
import com.example.ui.DuoDashboardScreen
import com.example.ui.DuoJournalScreen
import com.example.ui.DuoSpaceViewModel
import com.example.ui.DuoVaultScreen
import com.example.ui.DuoWishlistScreen
import com.example.ui.wikiSevenTapShortcut
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Allow screenshots & screen recording everywhere (ensure FLAG_SECURE is never set)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // 2. Block Recent Apps / App Switcher thumbnail for the ENTIRE app on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DuoSpaceApp()
            }
        }
    }
}

@Composable
fun DuoSpaceApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: DuoSpaceViewModel = viewModel(
        factory = DuoSpaceViewModel.Factory(context)
    )

    // Whole-App Recent Apps Thumbnail Privacy Scrim (activates when app goes to Recents / background)
    var isAppInRecentsOrBackground by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppInRecentsOrBackground = false
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isAppInRecentsOrBackground = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val space by viewModel.spaceState.collectAsStateWithLifecycle()
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(context) {
        com.example.ui.HiveSettingsPersistence.ensureInitialized(context)
    }
    val appConfig = remember(context, com.example.ui.HiveSettingsPersistence.changeCounter) {
        com.example.ui.HiveSettingsPersistence.loadConfig(context)
    }
    var isPinUnlocked by rememberSaveable { mutableStateOf(false) }

    // Re-lock PIN when app goes to Recents / background if PIN Lock is enabled
    DisposableEffect(lifecycleOwner, appConfig.pinLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && appConfig.pinLockEnabled) {
                isPinUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentDestination by rememberSaveable {
        mutableStateOf(DuoBottomNavDestination.DASHBOARD)
    }

    // Keep ViewModel bound to the paired Space Code and local user role
    androidx.compose.runtime.LaunchedEffect(
        appConfig.isSpacePaired,
        appConfig.activeSpaceCode,
        appConfig.localUserSlot
    ) {
        if (appConfig.isSpacePaired && appConfig.activeSpaceCode.isNotBlank()) {
            val myName = if (appConfig.localUserSlot == com.example.ui.TestUserSlot.USER_1) {
                appConfig.user1Name
            } else {
                appConfig.user2Name
            }
            viewModel.bindSpaceAndUser(
                spaceCode = appConfig.activeSpaceCode,
                slot = appConfig.localUserSlot,
                displayName = myName
            )
        }
    }

    // Automatically mark incoming messages as read when the user is viewing the Chat screen
    androidx.compose.runtime.LaunchedEffect(currentDestination, messages.size, uiState.activeUser) {
        if (currentDestination == DuoBottomNavDestination.CHAT && appConfig.isSpacePaired) {
            viewModel.markAllReadForActiveUser()
        }
    }

    val user1Name = appConfig.user1Name.ifBlank { space?.creatorName?.ifBlank { "Partner 1" } ?: "Partner 1" }
    val user2Name = appConfig.user2Name.ifBlank { space?.partnerName?.ifBlank { "Partner 2" } ?: "Partner 2" }
    val customizedSpace = remember(space, user1Name, user2Name) {
        space?.copy(creatorName = user1Name, partnerName = user2Name)
    }

    val unreadChatCount = remember(messages, uiState.activeUser) {
        messages.count {
            !it.isRead && !it.isSystemEvent &&
                it.messageType != MessageType.DOODLE &&
                it.senderId != uiState.activeUser.userId
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wikiSevenTapShortcut(context)
    ) {
        if (!appConfig.isSpacePaired) {
            com.example.ui.HiveSpaceOnboardingScreen(
                config = appConfig,
                onCreateSpace = { code, creatorName, avatarBlob ->
                    com.example.ui.HiveSettingsPersistence.completeSpacePairing(
                        context = context,
                        spaceCode = code,
                        slot = com.example.ui.TestUserSlot.USER_1,
                        displayName = creatorName,
                        avatarBlobName = avatarBlob
                    )
                    viewModel.createNewSpace(
                        spaceCode = code,
                        creatorName = creatorName
                    )
                },
                onJoinSpace = { code, partnerName, avatarBlob ->
                    com.example.ui.HiveSettingsPersistence.completeSpacePairing(
                        context = context,
                        spaceCode = code,
                        slot = com.example.ui.TestUserSlot.USER_2,
                        displayName = partnerName,
                        avatarBlobName = avatarBlob
                    )
                    viewModel.joinExistingSpace(
                        spaceCode = code,
                        partnerName = partnerName
                    )
                }
            )
        } else {
            when (currentDestination) {
                DuoBottomNavDestination.CHAT -> {
                    ChatSpaceScreen(
                        uiState = uiState,
                        space = customizedSpace,
                        messages = messages,
                        onSwitchUser = viewModel::switchActiveUser,
                        onUpdateMessageInput = viewModel::updateMessageInput,
                        onSendMessage = viewModel::sendMessage,
                        onSendVoiceMessage = viewModel::sendVoiceMessage,
                        onSendStickerMessage = viewModel::sendStickerMessage,
                        onToggleDoodleMode = viewModel::toggleDoodleMode,
                        onToggleStickerTray = viewModel::toggleStickerTray,
                        onUpdateScreenDoodle = viewModel::updateScreenDoodle,
                        onSelectReply = viewModel::setReplyTarget,
                        onToggleReaction = viewModel::toggleReaction,
                        onReplaceReaction = viewModel::replaceReaction,
                        onTogglePinMessage = viewModel::togglePinMessage,
                        onUnpinMessage = viewModel::unpinMessage,
                        onSetAutoRecordCalls = viewModel::setAutoRecordCalls,
                        onStartCall = viewModel::startCall,
                        onAnswerCall = viewModel::answerCall,
                        onToggleCallMute = viewModel::toggleCallMute,
                        onToggleCallSpeaker = viewModel::toggleCallSpeaker,
                        onToggleCallVideo = viewModel::toggleCallVideo,
                        onFlipCallCamera = viewModel::flipCallCamera,
                        onToggleCallScreenShare = viewModel::toggleCallScreenShare,
                        onToggleCallRecording = viewModel::toggleCallRecording,
                        onEndCall = viewModel::endCall,
                        onToggleAttachmentTray = viewModel::toggleAttachmentTray,
                        onToggleCameraStudio = viewModel::toggleCameraStudio,
                        onSendRichAttachment = viewModel::sendRichAttachmentMessage,
                        onUpdateInteractiveMessage = viewModel::updateInteractiveMessage,
                        onBackToDashboard = {
                            viewModel.toggleDoodleMode(false)
                            viewModel.toggleStickerTray(false)
                            viewModel.toggleAttachmentTray(false)
                            viewModel.toggleCameraStudio(false)
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        renderCallOverlay = false
                    )
                }
                DuoBottomNavDestination.JOURNAL -> {
                    DuoJournalScreen(
                        activeUser = uiState.activeUser,
                        user1Name = user1Name,
                        user2Name = user2Name,
                        onSwitchUser = viewModel::switchActiveUser,
                        onBackToDashboard = {
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        onOpenChat = {
                            currentDestination = DuoBottomNavDestination.CHAT
                        },
                        onOpenWishlist = {
                            currentDestination = DuoBottomNavDestination.WISHLIST
                        },
                        onOpenVault = {
                            currentDestination = DuoBottomNavDestination.VAULT
                        },
                        unreadChatCount = unreadChatCount
                    )
                }
                DuoBottomNavDestination.WISHLIST -> {
                    DuoWishlistScreen(
                        activeUser = uiState.activeUser,
                        user1Name = user1Name,
                        user2Name = user2Name,
                        onSwitchUser = viewModel::switchActiveUser,
                        onBackToDashboard = {
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        onOpenChat = {
                            currentDestination = DuoBottomNavDestination.CHAT
                        },
                        onOpenJournal = {
                            currentDestination = DuoBottomNavDestination.JOURNAL
                        },
                        onOpenVault = {
                            currentDestination = DuoBottomNavDestination.VAULT
                        },
                        unreadChatCount = unreadChatCount
                    )
                }
                DuoBottomNavDestination.VAULT -> {
                    DuoVaultScreen(
                        activeUser = uiState.activeUser,
                        user1Name = user1Name,
                        user2Name = user2Name,
                        messages = messages,
                        onSwitchUser = viewModel::switchActiveUser,
                        onBackToDashboard = {
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        onOpenChat = {
                            currentDestination = DuoBottomNavDestination.CHAT
                        },
                        onOpenJournal = {
                            currentDestination = DuoBottomNavDestination.JOURNAL
                        },
                        onOpenWishlist = {
                            currentDestination = DuoBottomNavDestination.WISHLIST
                        },
                        unreadChatCount = unreadChatCount
                    )
                }
                DuoBottomNavDestination.DASHBOARD -> {
                    DuoDashboardScreen(
                        uiState = uiState,
                        space = customizedSpace,
                        messages = messages,
                        onSwitchUser = viewModel::switchActiveUser,
                        onOpenChat = {
                            currentDestination = DuoBottomNavDestination.CHAT
                        },
                        onOpenChatWithDoodle = {
                            currentDestination = DuoBottomNavDestination.CHAT
                            viewModel.toggleDoodleMode(true)
                        },
                        onOpenJournal = {
                            currentDestination = DuoBottomNavDestination.JOURNAL
                        },
                        onOpenWishlist = {
                            currentDestination = DuoBottomNavDestination.WISHLIST
                        },
                        onOpenVault = {
                            currentDestination = DuoBottomNavDestination.VAULT
                        },
                        onStartCall = { viewModel.startCall(CallMode.VOICE) },
                        onJoinOrSwitchSpace = { spaceCode, slot, displayName ->
                            if (slot == com.example.ui.TestUserSlot.USER_2 && spaceCode != uiState.activeSpaceCode) {
                                viewModel.joinExistingSpace(spaceCode, displayName)
                            } else {
                                viewModel.bindSpaceAndUser(spaceCode, slot, displayName)
                            }
                        }
                    )
                }
            }

            // Persistent Voice / Video Call Overlay across Dashboard, Chat, and Journal
            uiState.activeCall?.let { callSession ->
                DuoCallOverlay(
                    callSession = callSession,
                    activeUser = uiState.activeUser,
                    user1Name = user1Name,
                    user2Name = user2Name,
                    onAnswerCall = viewModel::answerCall,
                    onToggleMute = viewModel::toggleCallMute,
                    onToggleSpeaker = viewModel::toggleCallSpeaker,
                    onToggleVideo = viewModel::toggleCallVideo,
                    onFlipCamera = viewModel::flipCallCamera,
                    onToggleScreenShare = viewModel::toggleCallScreenShare,
                    onToggleRecordCall = viewModel::toggleCallRecording,
                    onSwitchUser = viewModel::switchActiveUser,
                    onEndCall = viewModel::endCall,
                    onOpenChatFromCall = {
                        currentDestination = DuoBottomNavDestination.CHAT
                    }
                )
            }
        }

        // Standard App PIN Lock Overlay when enabled in Settings
        if (appConfig.pinLockEnabled && !isPinUnlocked) {
            com.example.ui.HivePinLockOverlay(
                expectedPin = appConfig.pinCode,
                onUnlocked = { isPinUnlocked = true }
            )
        }

        // Whole-App Recent Apps / App Switcher Privacy Cover (blocks recent-apps thumbnail across all screens)
        if (isAppInRecentsOrBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hive",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
