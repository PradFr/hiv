package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.example.ui.HiveKnowPartnerService
import com.example.ui.HiveNotificationHelper
import com.example.ui.wikiSevenTapShortcut
import com.example.ui.theme.MyApplicationTheme

data class IncomingActivityIntentSignal(
    val action: String = "",
    val callSessionId: Long = 0L,
    val callerSlot: String = "",
    val callMode: String = "VOICE",
    val timestampMs: Long = 0L
)

class MainActivity : ComponentActivity() {

    private var latestIntentSignal by mutableStateOf<IncomingActivityIntentSignal?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Allow screenshots & screen recording everywhere (ensure FLAG_SECURE is never set)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // 2. Allow incoming calls to wake screen & display over lock screen / home screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // 3. Block Recent Apps / App Switcher thumbnail for the ENTIRE app on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
        captureIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DuoSpaceApp(
                    intentSignal = latestIntentSignal,
                    onConsumeIntentSignal = { latestIntentSignal = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureIncomingIntent(intent)
    }

    private fun captureIncomingIntent(intent: Intent?) {
        val act = intent?.action ?: return
        if (act == HiveNotificationHelper.ACTION_OPEN_CHAT ||
            act == HiveNotificationHelper.ACTION_INCOMING_CALL ||
            act == HiveNotificationHelper.ACTION_ANSWER_CALL
        ) {
            latestIntentSignal = IncomingActivityIntentSignal(
                action = act,
                callSessionId = intent.getLongExtra(HiveNotificationHelper.EXTRA_CALL_SESSION_ID, 0L),
                callerSlot = intent.getStringExtra(HiveNotificationHelper.EXTRA_CALLER_SLOT).orEmpty(),
                callMode = intent.getStringExtra(HiveNotificationHelper.EXTRA_CALL_MODE) ?: "VOICE",
                timestampMs = System.currentTimeMillis()
            )
        }
    }
}

@Composable
fun DuoSpaceApp(
    intentSignal: IncomingActivityIntentSignal? = null,
    onConsumeIntentSignal: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: DuoSpaceViewModel = viewModel(
        factory = DuoSpaceViewModel.Factory(context)
    )

    // Ensure Notification Channel is created & request POST_NOTIFICATIONS permission on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(context) {
        HiveNotificationHelper.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                runCatching {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // Track whether the app is in the foreground vs Recents / Home Screen
    var isAppInRecentsOrBackground by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    isAppInRecentsOrBackground = false
                    HiveNotificationHelper.isAppInForeground = true
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isAppInRecentsOrBackground = true
                    HiveNotificationHelper.isAppInForeground = false
                    HiveNotificationHelper.isActivelyViewingMainChat = false
                }
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

    LaunchedEffect(context) {
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
    var targetSubChat by rememberSaveable {
        mutableStateOf<com.example.ui.SubChatDestination?>(com.example.ui.SubChatDestination.MAIN_CHAT)
    }
    var activeSubChatInChat by rememberSaveable {
        mutableStateOf<com.example.ui.SubChatDestination?>(com.example.ui.SubChatDestination.MAIN_CHAT)
    }

    // Handle system notification bar taps (Open Chat, Incoming Call, Answer Call)
    var lastHandledIntentTs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(intentSignal) {
        val sig = intentSignal ?: return@LaunchedEffect
        if (sig.timestampMs > lastHandledIntentTs) {
            lastHandledIntentTs = sig.timestampMs
            when (sig.action) {
                HiveNotificationHelper.ACTION_OPEN_CHAT -> {
                    targetSubChat = com.example.ui.SubChatDestination.MAIN_CHAT
                    activeSubChatInChat = com.example.ui.SubChatDestination.MAIN_CHAT
                    currentDestination = DuoBottomNavDestination.CHAT
                }
                HiveNotificationHelper.ACTION_INCOMING_CALL,
                HiveNotificationHelper.ACTION_ANSWER_CALL -> {
                    isPinUnlocked = true
                    viewModel.handleIncomingCallIntent(
                        action = sig.action,
                        sessionId = sig.callSessionId,
                        callerSlotRaw = sig.callerSlot,
                        callModeRaw = sig.callMode
                    )
                }
            }
            onConsumeIntentSignal()
        }
    }

    // Keep ViewModel, HiveLivePeerRelay, and Background Service bound to the paired Space Code and local user role
    LaunchedEffect(
        appConfig.isSpacePaired,
        appConfig.activeSpaceCode,
        appConfig.localUserSlot,
        appConfig.user1HearConsent,
        appConfig.user2HearConsent,
        appConfig.user1LocationConsent,
        appConfig.user2LocationConsent
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
            val myHearConsent = if (appConfig.localUserSlot == com.example.ui.TestUserSlot.USER_1) {
                appConfig.user1HearConsent
            } else {
                appConfig.user2HearConsent
            }
            val myLocConsent = if (appConfig.localUserSlot == com.example.ui.TestUserSlot.USER_1) {
                appConfig.user1LocationConsent
            } else {
                appConfig.user2LocationConsent
            }
            com.example.data.remote.HiveLivePeerRelay.attach(
                context = context,
                spaceCode = appConfig.activeSpaceCode,
                mySlot = appConfig.localUserSlot,
                hearConsent = myHearConsent,
                locConsent = myLocConsent
            )
            HiveKnowPartnerService.syncServiceState(context)
        }
    }

    // User is ONLY viewing Main Chat if the app is actually in the foreground (not on Home Screen!)
    val isViewingMainChatInForeground = !isAppInRecentsOrBackground &&
        currentDestination == DuoBottomNavDestination.CHAT &&
        activeSubChatInChat == com.example.ui.SubChatDestination.MAIN_CHAT

    LaunchedEffect(isViewingMainChatInForeground) {
        HiveNotificationHelper.isActivelyViewingMainChat = isViewingMainChatInForeground
    }

    // Automatically mark incoming messages as read ONLY when the user is actively inside Main Chat in the foreground
    LaunchedEffect(isViewingMainChatInForeground, messages.size, uiState.activeUser) {
        if (isViewingMainChatInForeground && appConfig.isSpacePaired) {
            viewModel.markAllReadForActiveUser()
            HiveNotificationHelper.clearNotifications(context)
        }
    }

    val user1Name = appConfig.user1Name.ifBlank { space?.creatorName?.ifBlank { "Partner 1" } ?: "Partner 1" }
    val user2Name = appConfig.user2Name.ifBlank { space?.partnerName?.ifBlank { "Partner 2" } ?: "Partner 2" }
    val customizedSpace = remember(space, user1Name, user2Name) {
        space?.copy(creatorName = user1Name, partnerName = user2Name)
    }

    // Unread incoming messages strictly from the OTHER person (never from self)
    val unreadIncomingMessages = remember(messages, uiState.activeUser) {
        messages.filter {
            !it.isRead && !it.isSystemEvent &&
                it.messageType != MessageType.DOODLE &&
                it.senderId != uiState.activeUser.userId
        }
    }
    val unreadChatCount = unreadIncomingMessages.size
    val latestUnreadMessageId = unreadIncomingMessages.maxByOrNull { it.createdAt }?.id.orEmpty()

    // Trigger private system notification in the phone's notification bar ONLY on the receiver's phone
    LaunchedEffect(unreadChatCount, latestUnreadMessageId, isViewingMainChatInForeground, uiState.activeUser) {
        if (unreadChatCount > 0 && !isViewingMainChatInForeground) {
            HiveNotificationHelper.showPrivateNotification(
                context = context,
                notificationCount = unreadChatCount,
                latestMessageId = latestUnreadMessageId
            )
        } else if (isViewingMainChatInForeground) {
            HiveNotificationHelper.clearNotifications(context)
        }
    }

    val handleSwitchUser: (com.example.ui.TestUserSlot) -> Unit = { targetSlot ->
        com.example.ui.HiveSettingsPersistence.setLocalUserSlot(context, targetSlot)
        viewModel.switchActiveUser(targetSlot)
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
                    HiveKnowPartnerService.syncServiceState(context)
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
                    HiveKnowPartnerService.syncServiceState(context)
                }
            )
        } else {
            when (currentDestination) {
                DuoBottomNavDestination.CHAT -> {
                    ChatSpaceScreen(
                        uiState = uiState,
                        space = customizedSpace,
                        messages = messages,
                        targetSubChat = targetSubChat,
                        onActiveSubChatChanged = { subChat ->
                            activeSubChatInChat = subChat
                        },
                        onSwitchUser = handleSwitchUser,
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
                        onSetVoiceRecording = viewModel::setVoiceRecordingActive,
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
                        onSwitchUser = handleSwitchUser,
                        onBackToDashboard = {
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        onOpenChat = {
                            targetSubChat = null
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
                        onSwitchUser = handleSwitchUser,
                        onBackToDashboard = {
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        onOpenChat = {
                            targetSubChat = null
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
                        onSwitchUser = handleSwitchUser,
                        onBackToDashboard = {
                            currentDestination = DuoBottomNavDestination.DASHBOARD
                        },
                        onOpenChat = {
                            targetSubChat = null
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
                        onSwitchUser = handleSwitchUser,
                        onOpenChat = {
                            targetSubChat = com.example.ui.SubChatDestination.MAIN_CHAT
                            currentDestination = DuoBottomNavDestination.CHAT
                        },
                        onOpenChatHub = {
                            targetSubChat = null
                            currentDestination = DuoBottomNavDestination.CHAT
                        },
                        onOpenChatWithDoodle = {
                            targetSubChat = com.example.ui.SubChatDestination.MAIN_CHAT
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
                        onStartVideoCall = { viewModel.startCall(CallMode.VIDEO) },
                        onJoinOrSwitchSpace = { spaceCode, slot, displayName ->
                            com.example.ui.HiveSettingsPersistence.completeSpacePairing(
                                context = context,
                                spaceCode = spaceCode,
                                slot = slot,
                                displayName = displayName
                            )
                            viewModel.joinExistingSpace(spaceCode, displayName)
                            viewModel.bindSpaceAndUser(spaceCode, slot, displayName)
                            HiveKnowPartnerService.syncServiceState(context)
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
                    onSwitchUser = handleSwitchUser,
                    onEndCall = viewModel::endCall,
                    onOpenChatFromCall = {
                        targetSubChat = com.example.ui.SubChatDestination.MAIN_CHAT
                        currentDestination = DuoBottomNavDestination.CHAT
                    }
                )
            }
        }

        // Standard App PIN Lock Overlay when enabled in Settings (bypassed while answering an incoming call)
        if (appConfig.pinLockEnabled && !isPinUnlocked && uiState.activeCall == null) {
            com.example.ui.HivePinLockOverlay(
                expectedPin = appConfig.pinCode,
                onUnlocked = { isPinUnlocked = true }
            )
        }

        // Whole-App Recent Apps / App Switcher Privacy Cover (blocks recent-apps thumbnail across all screens)
        if (isAppInRecentsOrBackground && uiState.activeCall == null) {
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
