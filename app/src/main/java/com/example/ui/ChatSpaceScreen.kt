package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.MonthlyMomentsPersistence
import com.example.data.local.SpaceEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private fun computeUsedBottomSpaceDp(
    strokes: List<DoodleStroke>,
    messagesContentHeightDp: Float
): Int {
    val maxY = strokes.flatMap { it.pointsDp }.maxOfOrNull { it.y } ?: 0f
    val overflowBelowMessages = maxY - (messagesContentHeightDp + 16f)
    return if (overflowBelowMessages > 0f) {
        ceil(overflowBelowMessages + 36f).toInt().coerceAtMost(3200)
    } else {
        0
    }
}

@Composable
fun ChatSpaceScreen(
    uiState: DuoUiState,
    space: SpaceEntity?,
    messages: List<MessageEntity>,
    onSwitchUser: (TestUserSlot) -> Unit,
    onUpdateMessageInput: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendVoiceMessage: (filePath: String, durationSec: Int, waveformCsv: String) -> Unit,
    onSendStickerMessage: (stickerId: String, emoji: String, caption: String, bgColorHex: Long) -> Unit,
    onToggleDoodleMode: (Boolean) -> Unit,
    onToggleStickerTray: (Boolean) -> Unit,
    onUpdateScreenDoodle: (doodleData: String?, extraBottomSpaceDp: Int) -> Unit,
    onSelectReply: (MessageEntity?) -> Unit,
    onToggleReaction: (MessageEntity, String) -> Unit,
    onReplaceReaction: (MessageEntity, String) -> Unit,
    onTogglePinMessage: (MessageEntity) -> Unit,
    onUnpinMessage: () -> Unit,
    onSetAutoRecordCalls: (Boolean) -> Unit,
    onStartCall: (CallMode) -> Unit,
    onAnswerCall: () -> Unit,
    onToggleCallMute: () -> Unit,
    onToggleCallSpeaker: () -> Unit,
    onToggleCallVideo: () -> Unit,
    onFlipCallCamera: () -> Unit,
    onToggleCallScreenShare: (String) -> Unit,
    onToggleCallRecording: () -> Unit,
    onEndCall: (Int, String?, String?) -> Unit,
    onToggleAttachmentTray: (Boolean) -> Unit = {},
    onToggleCameraStudio: (Boolean) -> Unit = {},
    onSendRichAttachment: (content: String, messageType: String, mediaMeta: String) -> Unit = { _, _, _ -> },
    onUpdateInteractiveMessage: (MessageEntity, String, String?) -> Unit = { _, _, _ -> },
    onBackToDashboard: (() -> Unit)? = null,
    renderCallOverlay: Boolean = true
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var studioInitialBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var studioInitialCaption by remember { mutableStateOf("") }

    var activeSubChat by remember { mutableStateOf<SubChatDestination?>(null) }

    BackHandler(
        enabled = activeSubChat != null &&
            uiState.activeCall == null &&
            !uiState.isDoodleModeActive &&
            !uiState.isStickerTrayOpen &&
            !uiState.isAttachmentTrayOpen &&
            !uiState.isCameraStudioOpen &&
            studioInitialBitmap == null
    ) {
        activeSubChat = null
    }

    BackHandler(
        enabled = activeSubChat == null &&
            onBackToDashboard != null &&
            uiState.activeCall == null &&
            !uiState.isDoodleModeActive &&
            !uiState.isStickerTrayOpen &&
            !uiState.isAttachmentTrayOpen &&
            !uiState.isCameraStudioOpen &&
            studioInitialBitmap == null
    ) {
        onBackToDashboard?.invoke()
    }

    val context = LocalContext.current
    val spaceCode = space?.code ?: "HIVE01"
    val user1Name = space?.creatorName ?: "Alex"
    val user2Name = space?.partnerName ?: "Sam"
    val activeUserName = if (uiState.activeUser == TestUserSlot.USER_1) user1Name else user2Name
    val partnerName = if (uiState.activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val partnerSlot = if (uiState.activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1

    val isMonthlyMoments = activeSubChat == SubChatDestination.MY_MONTHLY_MOMENTS ||
        activeSubChat == SubChatDestination.PARTNER_MONTHLY_MOMENTS
    val momentsTargetSlot = if (activeSubChat == SubChatDestination.PARTNER_MONTHLY_MOMENTS) {
        partnerSlot
    } else {
        uiState.activeUser
    }

    var momentMessagesState by remember(spaceCode, momentsTargetSlot, activeSubChat) {
        mutableStateOf(
            if (isMonthlyMoments) {
                MonthlyMomentsPersistence.loadMomentMessages(context, spaceCode, momentsTargetSlot)
            } else {
                emptyList()
            }
        )
    }
    var showClearMonthlyMomentsDialog by remember { mutableStateOf(false) }

    val effectiveMessages = if (isMonthlyMoments) momentMessagesState else messages

    val effectiveOnSendMessage: () -> Unit = {
        if (isMonthlyMoments) {
            val content = uiState.messageInput.trim()
            if (content.isNotEmpty()) {
                val replyTarget = uiState.replyingTo
                val newMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    spaceCode = spaceCode,
                    senderId = uiState.activeUser.userId,
                    senderName = activeUserName,
                    content = content,
                    messageType = MessageType.TEXT,
                    replyToId = replyTarget?.id,
                    replyToPreview = replyTarget?.let { "${it.senderName}: ${it.content.take(48)}" },
                    isDelivered = true,
                    isRead = true,
                    createdAt = System.currentTimeMillis()
                )
                MonthlyMomentsPersistence.saveMomentMessage(context, spaceCode, momentsTargetSlot, newMsg)
                momentMessagesState = MonthlyMomentsPersistence.loadMomentMessages(context, spaceCode, momentsTargetSlot)
                onUpdateMessageInput("")
                onSelectReply(null)
            }
        } else {
            onSendMessage()
        }
    }

    val effectiveOnSendVoiceMessage: (String, Int, String) -> Unit = { filePath, durationSec, waveformCsv ->
        if (isMonthlyMoments) {
            val mins = durationSec / 60
            val secs = durationSec % 60
            val durationLabel = String.format(Locale.getDefault(), "%d:%02d", mins, secs)
            val meta = "$filePath|$durationSec|$waveformCsv"
            val replyTarget = uiState.replyingTo
            val newMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                spaceCode = spaceCode,
                senderId = uiState.activeUser.userId,
                senderName = activeUserName,
                content = durationLabel,
                messageType = MessageType.VOICE,
                mediaMeta = meta,
                replyToId = replyTarget?.id,
                replyToPreview = replyTarget?.let { "${it.senderName}: ${it.content.take(48)}" },
                isDelivered = true,
                isRead = true,
                createdAt = System.currentTimeMillis()
            )
            MonthlyMomentsPersistence.saveMomentMessage(context, spaceCode, momentsTargetSlot, newMsg)
            momentMessagesState = MonthlyMomentsPersistence.loadMomentMessages(context, spaceCode, momentsTargetSlot)
            onSelectReply(null)
        } else {
            onSendVoiceMessage(filePath, durationSec, waveformCsv)
        }
    }

    val effectiveOnSendStickerMessage: (String, String, String, Long) -> Unit = { stickerId, emoji, caption, bgColorHex ->
        if (isMonthlyMoments) {
            val meta = "$stickerId|$emoji|$caption|$bgColorHex"
            val replyTarget = uiState.replyingTo
            val newMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                spaceCode = spaceCode,
                senderId = uiState.activeUser.userId,
                senderName = activeUserName,
                content = if (caption.isNotBlank()) "$emoji $caption" else emoji,
                messageType = MessageType.STICKER,
                mediaMeta = meta,
                replyToId = replyTarget?.id,
                replyToPreview = replyTarget?.let { "${it.senderName}: ${it.content.take(48)}" },
                isDelivered = true,
                isRead = true,
                createdAt = System.currentTimeMillis()
            )
            MonthlyMomentsPersistence.saveMomentMessage(context, spaceCode, momentsTargetSlot, newMsg)
            momentMessagesState = MonthlyMomentsPersistence.loadMomentMessages(context, spaceCode, momentsTargetSlot)
            onSelectReply(null)
        } else {
            onSendStickerMessage(stickerId, emoji, caption, bgColorHex)
        }
    }

    val effectiveOnSendRichAttachment: (String, String, String) -> Unit = { content, msgType, mediaMeta ->
        if (isMonthlyMoments) {
            val replyTarget = uiState.replyingTo
            val newMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                spaceCode = spaceCode,
                senderId = uiState.activeUser.userId,
                senderName = activeUserName,
                content = content,
                messageType = msgType,
                mediaMeta = mediaMeta,
                replyToId = replyTarget?.id,
                replyToPreview = replyTarget?.let { "${it.senderName}: ${it.content.take(48)}" },
                isDelivered = true,
                isRead = true,
                createdAt = System.currentTimeMillis()
            )
            MonthlyMomentsPersistence.saveMomentMessage(context, spaceCode, momentsTargetSlot, newMsg)
            momentMessagesState = MonthlyMomentsPersistence.loadMomentMessages(context, spaceCode, momentsTargetSlot)
            onSelectReply(null)
            onToggleAttachmentTray(false)
            onToggleCameraStudio(false)
        } else {
            onSendRichAttachment(content, msgType, mediaMeta)
        }
    }

    val effectiveOnUpdateInteractiveMessage: (MessageEntity, String, String?) -> Unit = { msg, newContent, newMeta ->
        if (isMonthlyMoments) {
            val updated = momentMessagesState.map {
                if (it.id == msg.id) it.copy(content = newContent, mediaMeta = newMeta) else it
            }
            MonthlyMomentsPersistence.saveAllMomentMessages(context, spaceCode, momentsTargetSlot, updated)
            momentMessagesState = updated
        } else {
            onUpdateInteractiveMessage(msg, newContent, newMeta)
        }
    }

    val effectiveOnToggleReaction: (MessageEntity, String) -> Unit = { msg, emoji ->
        if (isMonthlyMoments) {
            val currentList = msg.reaction.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (currentList.contains(emoji)) currentList.remove(emoji) else currentList.add(emoji)
            val newReaction = currentList.joinToString(",").ifEmpty { null }
            val updated = momentMessagesState.map {
                if (it.id == msg.id) it.copy(reaction = newReaction) else it
            }
            MonthlyMomentsPersistence.saveAllMomentMessages(context, spaceCode, momentsTargetSlot, updated)
            momentMessagesState = updated
        } else {
            onToggleReaction(msg, emoji)
        }
    }

    val effectiveOnReplaceReaction: (MessageEntity, String) -> Unit = { msg, emoji ->
        if (isMonthlyMoments) {
            val updated = momentMessagesState.map {
                if (it.id == msg.id) it.copy(reaction = emoji) else it
            }
            MonthlyMomentsPersistence.saveAllMomentMessages(context, spaceCode, momentsTargetSlot, updated)
            momentMessagesState = updated
        } else {
            onReplaceReaction(msg, emoji)
        }
    }

    val effectiveOnTogglePinMessage: (MessageEntity) -> Unit = { msg ->
        if (isMonthlyMoments) {
            val updated = momentMessagesState.map {
                if (it.id == msg.id) it.copy(isPinned = !it.isPinned) else it
            }
            MonthlyMomentsPersistence.saveAllMomentMessages(context, spaceCode, momentsTargetSlot, updated)
            momentMessagesState = updated
        } else {
            onTogglePinMessage(msg)
        }
    }

    val effectiveOnUnpinMessage: () -> Unit = {
        if (isMonthlyMoments) {
            val updated = momentMessagesState.map { it.copy(isPinned = false) }
            MonthlyMomentsPersistence.saveAllMomentMessages(context, spaceCode, momentsTargetSlot, updated)
            momentMessagesState = updated
        } else {
            onUnpinMessage()
        }
    }

    val effectiveOnUpdateScreenDoodle: (String?, Int) -> Unit = { doodleData, extraBottomSpaceDp ->
        if (isMonthlyMoments) {
            MonthlyMomentsPersistence.saveMomentDoodle(context, spaceCode, momentsTargetSlot, doodleData, extraBottomSpaceDp)
        } else {
            onUpdateScreenDoodle(doodleData, extraBottomSpaceDp)
        }
    }

    val unreadForUser1 = remember(effectiveMessages) {
        effectiveMessages.count { !it.isRead && !it.isSystemEvent && it.senderId != TestUserSlot.USER_1.userId }
    }
    val unreadForUser2 = remember(effectiveMessages) {
        effectiveMessages.count { !it.isRead && !it.isSystemEvent && it.senderId != TestUserSlot.USER_2.userId }
    }

    val allPinnedMessages = remember(effectiveMessages) {
        effectiveMessages.filter { it.isPinned && it.messageType != MessageType.DOODLE }
    }

    val latestPinnedMessage = remember(allPinnedMessages, space?.pinnedMessageId, isMonthlyMoments) {
        allPinnedMessages.lastOrNull()
            ?: if (!isMonthlyMoments) {
                effectiveMessages.firstOrNull { it.id == space?.pinnedMessageId && it.messageType != MessageType.DOODLE }
            } else null
    }

    val callHistoryMessages = remember(effectiveMessages) {
        effectiveMessages.filter { it.messageType == MessageType.CALL_LOG }
    }

    // Three-dot menu states
    var isSearchBarVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showPinnedDialog by remember { mutableStateOf(false) }
    var showCallHistoryDialog by remember { mutableStateOf(false) }
    var showExportMarkdownDialog by remember { mutableStateOf(false) }

    // Filtered messages when search bar is open
    val displayedMessages = remember(effectiveMessages, isSearchBarVisible, searchQuery) {
        val q = searchQuery.trim()
        if (!isSearchBarVisible || q.isEmpty()) {
            effectiveMessages
        } else {
            effectiveMessages.filter { msg ->
                msg.messageType != MessageType.DOODLE &&
                    (msg.content.contains(q, ignoreCase = true) ||
                        msg.senderName.contains(q, ignoreCase = true))
            }
        }
    }

    // Stable in-memory strokes list that loads per chat destination (Main Chat vs Monthly Moments)
    val strokes = remember { mutableStateListOf<DoodleStroke>() }
    var loadedDoodleSubChat by remember { mutableStateOf<SubChatDestination?>(null) }
    LaunchedEffect(activeSubChat, space?.screenDoodleData, momentsTargetSlot) {
        if (activeSubChat != null && loadedDoodleSubChat != activeSubChat) {
            loadedDoodleSubChat = activeSubChat
            strokes.clear()
            if (isMonthlyMoments) {
                val (doodleRaw, _) = MonthlyMomentsPersistence.loadMomentDoodle(context, spaceCode, momentsTargetSlot)
                val decoded = DoodleSerializer.decode(doodleRaw)
                if (decoded.isNotEmpty()) strokes.addAll(decoded)
            } else if (space != null) {
                val decoded = DoodleSerializer.decode(space.screenDoodleData)
                if (decoded.isNotEmpty()) strokes.addAll(decoded)
            }
        }
    }

    val activeStrokePointsDp = remember { mutableStateListOf<Offset>() }
    var selectedDoodleColor by remember { mutableStateOf(DOODLE_PALETTE[0]) }
    val currentColorState by rememberUpdatedState(selectedDoodleColor)
    var strokeThicknessDp by remember { mutableFloatStateOf(3.5f) }
    var isEraserMode by remember { mutableStateOf(false) }

    var messagesContentHeightDp by remember { mutableFloatStateOf(0f) }
    val currentMessagesHeightState by rememberUpdatedState(messagesContentHeightDp)
    var tempDoodleAddedSpaceDp by remember { mutableIntStateOf(0) }

    val usedDoodleBottomSpaceDp = remember(strokes.size, messagesContentHeightDp) {
        computeUsedBottomSpaceDp(strokes, messagesContentHeightDp)
    }

    val effectiveBottomSpaceDp = if (uiState.isDoodleModeActive) {
        max(usedDoodleBottomSpaceDp, tempDoodleAddedSpaceDp)
    } else {
        usedDoodleBottomSpaceDp
    }

    LaunchedEffect(uiState.isDoodleModeActive) {
        if (!uiState.isDoodleModeActive) {
            val updatedSpace = computeUsedBottomSpaceDp(strokes, messagesContentHeightDp)
            tempDoodleAddedSpaceDp = updatedSpace
            effectiveOnUpdateScreenDoodle(
                DoodleSerializer.encode(strokes).ifEmpty { null },
                updatedSpace
            )
        } else {
            val updatedSpace = computeUsedBottomSpaceDp(strokes, messagesContentHeightDp)
            tempDoodleAddedSpaceDp = max(updatedSpace, 64)
        }
    }

    var activeContextMessageId by remember { mutableStateOf<String?>(null) }
    var openEmojiPickerDirectly by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveMessages.size, activeSubChat) {
        if (effectiveMessages.isNotEmpty() && !uiState.isDoodleModeActive) {
            delay(60)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // --- Sub-Chat Navigation Router ---
    if (activeSubChat == null) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                MinimalHeaderBar(
                    activeUser = uiState.activeUser,
                    user1Name = user1Name,
                    user2Name = user2Name,
                    unreadForUser1 = unreadForUser1,
                    unreadForUser2 = unreadForUser2,
                    pinnedCount = allPinnedMessages.size,
                    callCount = callHistoryMessages.size,
                    autoRecordCalls = space?.autoRecordCalls == true,
                    onSwitchUser = onSwitchUser,
                    onStartCall = { onStartCall(CallMode.VOICE) },
                    onOpenSearch = {
                        isSearchBarVisible = !isSearchBarVisible
                        if (!isSearchBarVisible) searchQuery = ""
                    },
                    onOpenPinnedMessages = { showPinnedDialog = true },
                    onOpenCallHistory = { showCallHistoryDialog = true },
                    onToggleAutoRecordCalls = {
                        onSetAutoRecordCalls(!(space?.autoRecordCalls == true))
                    },
                    onOpenExportMarkdown = { showExportMarkdownDialog = true },
                    onBackToDashboard = onBackToDashboard
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                HiveChatNavigationHub(
                    activeUser = uiState.activeUser,
                    space = space,
                    messages = messages,
                    onSelectChat = { dest -> activeSubChat = dest },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MinimalHeaderBar(
                        activeUser = uiState.activeUser,
                        user1Name = user1Name,
                        user2Name = user2Name,
                        unreadForUser1 = unreadForUser1,
                        unreadForUser2 = unreadForUser2,
                        pinnedCount = allPinnedMessages.size,
                        callCount = callHistoryMessages.size,
                        autoRecordCalls = space?.autoRecordCalls == true,
                        onSwitchUser = onSwitchUser,
                        onStartCall = { onStartCall(CallMode.VOICE) },
                        onOpenSearch = {
                            isSearchBarVisible = !isSearchBarVisible
                            if (!isSearchBarVisible) searchQuery = ""
                        },
                        onOpenPinnedMessages = { showPinnedDialog = true },
                        onOpenCallHistory = { showCallHistoryDialog = true },
                        onToggleAutoRecordCalls = {
                            onSetAutoRecordCalls(!(space?.autoRecordCalls == true))
                        },
                        onOpenExportMarkdown = { showExportMarkdownDialog = true },
                        onBackToDashboard = { activeSubChat = null },
                        headerTitleOverride = when (activeSubChat) {
                            SubChatDestination.MY_MONTHLY_MOMENTS -> "My Monthly Moments"
                            SubChatDestination.PARTNER_MONTHLY_MOMENTS -> "$partnerName's Monthly Moments"
                            else -> null
                        },
                        headerSubtitleOverride = when (activeSubChat) {
                            SubChatDestination.MY_MONTHLY_MOMENTS -> "Private vault for $partnerName • Unlocks month-end"
                            SubChatDestination.PARTNER_MONTHLY_MOMENTS -> "Unwrapped monthly moments for you ✨"
                            else -> null
                        },
                        showCallButton = !isMonthlyMoments,
                        onClearMonthlyMoments = if (isMonthlyMoments) {
                            { showClearMonthlyMomentsDialog = true }
                        } else null
                    )

                    // Inline Search Bar (opened from 3-dot menu)
                    AnimatedVisibility(visible = isSearchBarVisible) {
                        MinimalChatSearchBar(
                            query = searchQuery,
                            matchCount = if (searchQuery.isBlank()) 0 else displayedMessages.size,
                            onQueryChange = { searchQuery = it },
                            onClose = {
                                isSearchBarVisible = false
                                searchQuery = ""
                            }
                        )
                    }

                    // Pinned Message Banner (click to view all pinned messages)
                    AnimatedVisibility(visible = latestPinnedMessage != null || (!isMonthlyMoments && !space?.pinnedNote.isNullOrBlank())) {
                        val textToShow = latestPinnedMessage?.let { msg ->
                            val preview = when (msg.messageType) {
                                MessageType.VOICE -> "🎤 Voice message (${msg.content})"
                                MessageType.STICKER -> "✨ Sticker (${msg.content})"
                                MessageType.CALL_LOG -> "📞 ${msg.content}"
                                else -> msg.content
                            }
                            "${msg.senderName}: $preview"
                        } ?: space?.pinnedNote.orEmpty()

                        MinimalPinnedBar(
                            text = textToShow,
                            pinnedCount = allPinnedMessages.size,
                            onOpenAllPinned = { showPinnedDialog = true },
                            onUnpin = effectiveOnUnpinMessage
                        )
                    }
                }
            },
            bottomBar = {
                MinimalBottomArea(
                    uiState = uiState,
                    activeUserName = activeUserName,
                    user1Name = user1Name,
                    user2Name = user2Name,
                    messages = effectiveMessages,
                    onUpdateMessageInput = onUpdateMessageInput,
                    onSendMessage = effectiveOnSendMessage,
                    onSendVoiceMessage = effectiveOnSendVoiceMessage,
                    onSendStickerMessage = effectiveOnSendStickerMessage,
                    onSendRichAttachment = effectiveOnSendRichAttachment,
                    onToggleDoodleMode = onToggleDoodleMode,
                    onToggleStickerTray = onToggleStickerTray,
                    onToggleAttachmentTray = onToggleAttachmentTray,
                    onOpenCameraStudio = { editBitmap, caption ->
                        studioInitialBitmap = editBitmap
                        studioInitialCaption = caption
                        onToggleCameraStudio(true)
                    },
                    onUpdateInteractiveMessage = { msg, newMeta ->
                        effectiveOnUpdateInteractiveMessage(msg, msg.content, newMeta)
                    },
                    onClearReply = { onSelectReply(null) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Scrollable Chat Content + Rendered On-Screen Doodles
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            state = scrollState,
                            enabled = !uiState.isDoodleModeActive
                        )
                        .drawWithContent {
                            drawContent()

                            fun renderStroke(ptsDp: List<Offset>, color: Color, widthDp: Float) {
                                if (ptsDp.isEmpty()) return
                                val strokePx = widthDp.dp.toPx()
                                if (ptsDp.size == 1) {
                                    val p = ptsDp.first()
                                    drawCircle(
                                        color = color,
                                        radius = strokePx / 2f,
                                        center = Offset(p.x.dp.toPx(), p.y.dp.toPx())
                                    )
                                    return
                                }
                                val path = Path().apply {
                                    moveTo(ptsDp[0].x.dp.toPx(), ptsDp[0].y.dp.toPx())
                                    for (i in 1 until ptsDp.size) {
                                        lineTo(ptsDp[i].x.dp.toPx(), ptsDp[i].y.dp.toPx())
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(
                                        width = strokePx,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            strokes.forEach { s ->
                                renderStroke(s.pointsDp, Color(s.colorArgb), s.strokeWidthDp)
                            }
                            if (activeStrokePointsDp.isNotEmpty()) {
                                renderStroke(activeStrokePointsDp, Color(currentColorState), strokeThicknessDp)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("chat_message_list"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .onSizeChanged { size ->
                                messagesContentHeightDp = with(density) { size.height.toDp().value }
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subtle conversation date chip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = when (activeSubChat) {
                                        SubChatDestination.MY_MONTHLY_MOMENTS -> "🔒 Private Monthly Moments • Hidden from $partnerName until Month-End"
                                        SubChatDestination.PARTNER_MONTHLY_MOMENTS -> "✨ $partnerName's Monthly Moments • Unwrapped"
                                        else -> "Today • End-to-End DuoSpace"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        displayedMessages.forEachIndexed { index, message ->
                            key(message.id) {
                                if (message.messageType == MessageType.DOODLE) {
                                    // Reserved inline doodle space so messages sent AFTER a doodle
                                    // appear AFTER the doodle space instead of overlapping it!
                                    val reservedHeightDp = message.mediaMeta?.toIntOrNull()?.coerceIn(24, 3200) ?: 120
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(reservedHeightDp.dp)
                                            .testTag("doodle_reserved_space_${message.id}")
                                    )
                                } else {
                                    val isMe = message.senderId == uiState.activeUser.userId
                                    val prevMsg = displayedMessages.getOrNull(index - 1)
                                    val isGroupedWithPrev = prevMsg != null &&
                                        prevMsg.messageType != MessageType.DOODLE &&
                                        prevMsg.senderId == message.senderId
                                    val isMenuVisible = activeContextMessageId == message.id

                                    SwipeAndLongPressMessageItem(
                                        message = message,
                                        isMe = isMe,
                                        activeUser = uiState.activeUser,
                                        activeUserName = activeUserName,
                                        isGroupedWithPrev = isGroupedWithPrev,
                                        searchHighlight = searchQuery.trim(),
                                        isMenuVisible = isMenuVisible,
                                        initialExpandEmojiPicker = openEmojiPickerDirectly,
                                        onSwipeToReply = { onSelectReply(message) },
                                        onLongPressBubble = {
                                            openEmojiPickerDirectly = false
                                            activeContextMessageId = message.id
                                        },
                                        onLongPressReactionBadge = {
                                            openEmojiPickerDirectly = true
                                            activeContextMessageId = message.id
                                        },
                                        onTapReactionBadge = { emoji ->
                                            effectiveOnToggleReaction(message, emoji)
                                        },
                                        onDismissMenu = {
                                            activeContextMessageId = null
                                            openEmojiPickerDirectly = false
                                        },
                                        onSelectReactionFromMenu = { emoji ->
                                            if (openEmojiPickerDirectly) {
                                                effectiveOnReplaceReaction(message, emoji)
                                            } else {
                                                effectiveOnToggleReaction(message, emoji)
                                            }
                                            activeContextMessageId = null
                                            openEmojiPickerDirectly = false
                                        },
                                        onReplyFromMenu = {
                                            onSelectReply(message)
                                            activeContextMessageId = null
                                        },
                                        onTogglePinFromMenu = {
                                            effectiveOnTogglePinMessage(message)
                                            activeContextMessageId = null
                                        },
                                        onEditAndDrawMedia = { bmp, cap ->
                                            studioInitialBitmap = bmp
                                            studioInitialCaption = cap
                                            onToggleCameraStudio(true)
                                        },
                                        onUpdateInteractiveMeta = { updatedMeta ->
                                            effectiveOnUpdateInteractiveMessage(message, message.content, updatedMeta)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (effectiveBottomSpaceDp > 0 || uiState.isDoodleModeActive) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(effectiveBottomSpaceDp.dp)
                                .testTag("chat_bottom_draw_space")
                        )
                    }
                }

                // 2. Dedicated Full-Screen Touch Overlay when Doodle Mode is active
                if (uiState.isDoodleModeActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isEraserMode, strokeThicknessDp) {
                                val horizontalPadPx = 16.dp.toPx()
                                val verticalPadPx = 12.dp.toPx()
                                detectDragGestures(
                                    onDragStart = { offsetPx ->
                                        activeStrokePointsDp.clear()
                                        val contentX = offsetPx.x - horizontalPadPx
                                        val contentY = offsetPx.y + scrollState.value - verticalPadPx
                                        val ptDp = with(density) {
                                            Offset(contentX.toDp().value, contentY.toDp().value)
                                        }
                                        if (isEraserMode) {
                                            val eraseRadius = strokeThicknessDp * 2.5f + 16f
                                            val removed = strokes.removeAll { stroke ->
                                                stroke.pointsDp.any { pt ->
                                                    val dx = pt.x - ptDp.x
                                                    val dy = pt.y - ptDp.y
                                                    (dx * dx + dy * dy) <= (eraseRadius * eraseRadius)
                                                }
                                            }
                                            if (removed) {
                                                val newUsedBottomDp = computeUsedBottomSpaceDp(
                                                    strokes,
                                                    currentMessagesHeightState
                                                )
                                                effectiveOnUpdateScreenDoodle(
                                                    DoodleSerializer.encode(strokes).ifEmpty { null },
                                                    newUsedBottomDp
                                                )
                                            }
                                        } else {
                                            activeStrokePointsDp.add(ptDp)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val pos = change.position
                                        val contentX = pos.x - horizontalPadPx
                                        val contentY = pos.y + scrollState.value - verticalPadPx
                                        val ptDp = with(density) {
                                            Offset(contentX.toDp().value, contentY.toDp().value)
                                        }
                                        if (isEraserMode) {
                                            val eraseRadius = strokeThicknessDp * 2.5f + 16f
                                            val removed = strokes.removeAll { stroke ->
                                                stroke.pointsDp.any { pt ->
                                                    val dx = pt.x - ptDp.x
                                                    val dy = pt.y - ptDp.y
                                                    (dx * dx + dy * dy) <= (eraseRadius * eraseRadius)
                                                }
                                            }
                                            if (removed) {
                                                val newUsedBottomDp = computeUsedBottomSpaceDp(
                                                    strokes,
                                                    currentMessagesHeightState
                                                )
                                                effectiveOnUpdateScreenDoodle(
                                                    DoodleSerializer.encode(strokes).ifEmpty { null },
                                                    newUsedBottomDp
                                                )
                                            }
                                        } else {
                                            activeStrokePointsDp.add(ptDp)
                                        }
                                    },
                                    onDragEnd = {
                                        if (!isEraserMode && activeStrokePointsDp.isNotEmpty()) {
                                            strokes.add(
                                                DoodleStroke(
                                                    pointsDp = activeStrokePointsDp.toList(),
                                                    colorArgb = currentColorState,
                                                    strokeWidthDp = strokeThicknessDp
                                                )
                                            )
                                            activeStrokePointsDp.clear()
                                            val newUsedBottomDp = computeUsedBottomSpaceDp(
                                                strokes,
                                                currentMessagesHeightState
                                            )
                                            effectiveOnUpdateScreenDoodle(
                                                DoodleSerializer.encode(strokes),
                                                newUsedBottomDp
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        if (!isEraserMode && activeStrokePointsDp.isNotEmpty()) {
                                            strokes.add(
                                                DoodleStroke(
                                                    pointsDp = activeStrokePointsDp.toList(),
                                                    colorArgb = currentColorState,
                                                    strokeWidthDp = strokeThicknessDp
                                                )
                                            )
                                            activeStrokePointsDp.clear()
                                        }
                                    }
                                )
                            }
                            .testTag("doodle_touch_overlay")
                    )
                }

                // 3. Static Fixed "+ Add Space" Button pinned at bottom-center in Doodle mode
                AnimatedVisibility(
                    visible = uiState.isDoodleModeActive,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                val nextSpace = (effectiveBottomSpaceDp + 180).coerceAtMost(3200)
                                tempDoodleAddedSpaceDp = nextSpace
                                coroutineScope.launch {
                                    delay(50)
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                            .testTag("static_add_space_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add bottom space to draw",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Add Space",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 4. Minimal Floating Doodle Controls Pill at top-center when Doodle mode is active
                AnimatedVisibility(
                    visible = uiState.isDoodleModeActive,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    FloatingDoodleToolbar(
                        selectedColor = selectedDoodleColor,
                        canUndo = strokes.isNotEmpty(),
                        canClear = strokes.isNotEmpty(),
                        onSelectColor = {
                            selectedDoodleColor = it
                            isEraserMode = false
                        },
                        onUndo = {
                            if (strokes.isNotEmpty()) {
                                strokes.removeAt(strokes.lastIndex)
                                val updatedBottom = computeUsedBottomSpaceDp(strokes, messagesContentHeightDp)
                                effectiveOnUpdateScreenDoodle(
                                    DoodleSerializer.encode(strokes).ifEmpty { null },
                                    updatedBottom
                                )
                            }
                        },
                        onClear = {
                            strokes.clear()
                            activeStrokePointsDp.clear()
                            tempDoodleAddedSpaceDp = 64
                            effectiveOnUpdateScreenDoodle(null, 0)
                        },
                        onAddBottomSpace = {},
                        onDone = { onToggleDoodleMode(false) },
                        isEraser = isEraserMode,
                        onToggleEraser = { isEraserMode = !isEraserMode },
                        strokeThicknessDp = strokeThicknessDp,
                        onSelectThickness = {
                            strokeThicknessDp = it
                            isEraserMode = false
                        }
                    )
                }
            }
        }

        // Three-Dot Menu Dialogs
        if (showPinnedDialog) {
            PinnedMessagesDialog(
                pinnedMessages = allPinnedMessages,
                onUnpinMessage = effectiveOnTogglePinMessage,
                onUnpinAll = effectiveOnUnpinMessage,
                onDismiss = { showPinnedDialog = false }
            )
        }

        if (showCallHistoryDialog) {
            CallHistoryAndRecordingsDialog(
                callLogs = callHistoryMessages,
                autoRecordAllCalls = space?.autoRecordCalls == true,
                onToggleAutoRecordAllCalls = onSetAutoRecordCalls,
                onDismiss = { showCallHistoryDialog = false }
            )
        }

        if (showExportMarkdownDialog) {
            ExportChatMarkdownDialog(
                space = space,
                messages = effectiveMessages,
                onDismiss = { showExportMarkdownDialog = false }
            )
        }

        if (showClearMonthlyMomentsDialog) {
            AlertDialog(
                onDismissRequest = { showClearMonthlyMomentsDialog = false },
                title = { Text("Clear Monthly Moments & Relock?") },
                text = {
                    Text(
                        "Clearing this chat will wipe all messages, media, and doodles in this Monthly Moments vault and immediately lock it again."
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            MonthlyMomentsPersistence.clearMomentsAndRelock(context, spaceCode, momentsTargetSlot)
                            momentMessagesState = emptyList()
                            strokes.clear()
                            showClearMonthlyMomentsDialog = false
                            Toast.makeText(context, "Monthly Moments cleared and locked 🔒", Toast.LENGTH_SHORT).show()
                            activeSubChat = null
                        }
                    ) {
                        Text("Clear & Relock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearMonthlyMomentsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Full-Screen In-App Camera & Edit/Draw Studio Modal (Photos & Videos)
        if (uiState.isCameraStudioOpen || studioInitialBitmap != null) {
            ChatCameraStudioModal(
                activeUserName = activeUserName,
                partnerName = partnerName,
                initialEditBitmap = studioInitialBitmap,
                initialCaption = studioInitialCaption,
                onClose = {
                    studioInitialBitmap = null
                    studioInitialCaption = ""
                    onToggleCameraStudio(false)
                },
                onSendMediaToChat = { content, msgType, mediaMeta ->
                    studioInitialBitmap = null
                    studioInitialCaption = ""
                    effectiveOnSendRichAttachment(content, msgType, mediaMeta)
                }
            )
        }

        // Full-Screen Voice / Video Call Overlay when a call is active
        if (renderCallOverlay) {
            uiState.activeCall?.let { callSession ->
                DuoCallOverlay(
                    callSession = callSession,
                    activeUser = uiState.activeUser,
                    user1Name = user1Name,
                    user2Name = user2Name,
                    onAnswerCall = onAnswerCall,
                    onToggleMute = onToggleCallMute,
                    onToggleSpeaker = onToggleCallSpeaker,
                    onToggleVideo = onToggleCallVideo,
                    onFlipCamera = onFlipCallCamera,
                    onToggleScreenShare = onToggleCallScreenShare,
                    onToggleRecordCall = onToggleCallRecording,
                    onSwitchUser = onSwitchUser,
                    onEndCall = onEndCall
                )
            }
        }
    }
}

@Composable
private fun MinimalHeaderBar(
    activeUser: TestUserSlot,
    user1Name: String,
    user2Name: String,
    unreadForUser1: Int,
    unreadForUser2: Int,
    pinnedCount: Int,
    callCount: Int,
    autoRecordCalls: Boolean,
    onSwitchUser: (TestUserSlot) -> Unit,
    onStartCall: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPinnedMessages: () -> Unit,
    onOpenCallHistory: () -> Unit,
    onToggleAutoRecordCalls: () -> Unit,
    onOpenExportMarkdown: () -> Unit,
    onBackToDashboard: (() -> Unit)? = null,
    headerTitleOverride: String? = null,
    headerSubtitleOverride: String? = null,
    showCallButton: Boolean = true,
    onClearMonthlyMoments: (() -> Unit)? = null
) {
    val partnerName = if (activeUser == TestUserSlot.USER_1) user2Name else user1Name
    val displayTitle = headerTitleOverride ?: partnerName
    val displaySubtitle = headerSubtitleOverride ?: "Active now"
    var isMoreMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_header")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back Button (if onBackToDashboard provided) + Partner Avatar + Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBackToDashboard != null) {
                        IconButton(
                            onClick = onBackToDashboard,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("back_to_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to dashboard",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!showCallButton) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = partnerName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (showCallButton) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = displaySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right: User Switcher Pill + Single Call Button (only in Main Chat) + 3-Dot More Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Profile Switcher Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val nextSlot = if (activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
                                onSwitchUser(nextSlot)
                            }
                            .testTag("header_switch_user_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Switch profile slot",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (activeUser == TestUserSlot.USER_1) "User 1" else "User 2",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Single Call Button (Hidden in Monthly Moments per user request)
                    if (showCallButton) {
                        IconButton(
                            onClick = onStartCall,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("voice_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Top-Right Three-Dot Menu Button
                    Box {
                        IconButton(
                            onClick = { isMoreMenuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("header_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isMoreMenuExpanded,
                            onDismissRequest = { isMoreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Switch to ${if (activeUser == TestUserSlot.USER_1) user2Name else user1Name}")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    val nextSlot = if (activeUser == TestUserSlot.USER_1) TestUserSlot.USER_2 else TestUserSlot.USER_1
                                    onSwitchUser(nextSlot)
                                },
                                modifier = Modifier.testTag("menu_item_switch_user")
                            )

                            DropdownMenuItem(
                                text = { Text("Search") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onOpenSearch()
                                },
                                modifier = Modifier.testTag("menu_item_search")
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (pinnedCount > 0) "Pinned Messages ($pinnedCount)"
                                        else "Pinned Messages"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onOpenPinnedMessages()
                                },
                                modifier = Modifier.testTag("menu_item_pinned")
                            )

                            if (showCallButton) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (callCount > 0) "Call History ($callCount)"
                                            else "Call History & Recordings"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        isMoreMenuExpanded = false
                                        onOpenCallHistory()
                                    },
                                    modifier = Modifier.testTag("menu_item_call_history")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (autoRecordCalls) "Auto-Record Calls: ON"
                                            else "Auto-Record Calls: OFF"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (autoRecordCalls) {
                                                Icons.Default.Check
                                            } else {
                                                Icons.Default.FiberManualRecord
                                            },
                                            contentDescription = null,
                                            tint = if (autoRecordCalls) {
                                                Color(0xFFEF4444)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        onToggleAutoRecordCalls()
                                    },
                                    modifier = Modifier.testTag("menu_item_auto_record_calls")
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )

                            DropdownMenuItem(
                                text = { Text("Export Chat (.md)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onOpenExportMarkdown()
                                },
                                modifier = Modifier.testTag("menu_item_export_md")
                            )

                            if (onClearMonthlyMoments != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Clear Chat & Lock",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        isMoreMenuExpanded = false
                                        onClearMonthlyMoments()
                                    },
                                    modifier = Modifier.testTag("menu_item_clear_monthly_moments")
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun MinimalChatSearchBar(
    query: String,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_search_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search messages in chat…",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input")
                        )
                    }
                    if (query.isNotBlank()) {
                        Text(
                            text = "$matchCount found",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun MinimalPinnedBar(
    text: String,
    pinnedCount: Int,
    onOpenAllPinned: () -> Unit,
    onUnpin: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAllPinned)
            .testTag("pinned_message_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (pinnedCount > 1) "($pinnedCount) $text" else text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unpin message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun MinimalUserTab(
    name: String,
    selected: Boolean,
    hasUnread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "tab_bg"
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
            if (hasUnread && !selected) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeAndLongPressMessageItem(
    message: MessageEntity,
    isMe: Boolean,
    activeUser: TestUserSlot,
    activeUserName: String,
    isGroupedWithPrev: Boolean,
    searchHighlight: String,
    isMenuVisible: Boolean,
    initialExpandEmojiPicker: Boolean,
    onSwipeToReply: () -> Unit,
    onLongPressBubble: () -> Unit,
    onLongPressReactionBadge: () -> Unit,
    onTapReactionBadge: (String) -> Unit,
    onDismissMenu: () -> Unit,
    onSelectReactionFromMenu: (String) -> Unit,
    onReplyFromMenu: () -> Unit,
    onTogglePinFromMenu: () -> Unit,
    onEditAndDrawMedia: (Bitmap, String) -> Unit,
    onUpdateInteractiveMeta: (String) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val swipeThresholdPx = 110f

    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = if (isGroupedWithPrev) 8.dp else 20.dp,
            bottomEnd = 4.dp,
            bottomStart = 20.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (isGroupedWithPrev) 8.dp else 20.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 4.dp
        )
    }

    val isHighlighted = searchHighlight.isNotEmpty() &&
        message.content.contains(searchHighlight, ignoreCase = true)

    val bubbleColor = if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val metaColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val next = (offsetX.value + dragAmount * 0.65f).coerceIn(-150f, 150f)
                        coroutineScope.launch {
                            offsetX.snapTo(next)
                        }
                    },
                    onDragEnd = {
                        if (abs(offsetX.value) >= swipeThresholdPx) {
                            onSwipeToReply()
                        }
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                )
            }
    ) {
        if (abs(offsetX.value) > 12f) {
            val progress = (abs(offsetX.value) / swipeThresholdPx).coerceIn(0f, 1f)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Swipe to reply",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(if (offsetX.value > 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 8.dp)
                    .size(18.dp)
                    .alpha(progress)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box {
                if (message.messageType == MessageType.STICKER) {
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {},
                                onLongClick = onLongPressBubble
                            )
                            .testTag("message_bubble_${message.id}")
                    ) {
                        StickerMessageView(
                            mediaMeta = message.mediaMeta,
                            fallbackContent = message.content,
                            timeLabel = timeFormatter.format(Date(message.createdAt)),
                            isMe = isMe,
                            isDelivered = message.isDelivered,
                            isRead = message.isRead,
                            isPinned = message.isPinned
                        )
                    }
                } else {
                    val isFullMediaBubble = message.messageType == MessageType.PHOTO || message.messageType == MessageType.VIDEO
                    Surface(
                        shape = bubbleShape,
                        color = bubbleColor,
                        border = if (isHighlighted) {
                            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6))
                        } else if (!isMe) {
                            androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        } else null,
                        modifier = Modifier
                            .widthIn(min = 84.dp, max = 304.dp)
                            .clip(bubbleShape)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = onLongPressBubble
                            )
                            .testTag("message_bubble_${message.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = if (isFullMediaBubble) 3.dp else 14.dp,
                                vertical = if (isFullMediaBubble) 3.dp else 9.dp
                            )
                        ) {
                            if (!message.replyToPreview.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            start = if (isFullMediaBubble) 6.dp else 0.dp,
                                            end = if (isFullMediaBubble) 6.dp else 0.dp,
                                            top = if (isFullMediaBubble) 4.dp else 0.dp,
                                            bottom = 6.dp
                                        )
                                        .height(IntrinsicSize.Min)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isMe) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
                                            } else {
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(metaColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.replyToPreview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 11.5.sp,
                                        color = metaColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box {
                                when (message.messageType) {
                                    MessageType.VOICE -> {
                                        VoiceMessageBubbleView(
                                            durationLabel = message.content,
                                            mediaMeta = message.mediaMeta,
                                            isMe = isMe,
                                            contentColor = textColor,
                                            accentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    MessageType.CALL_LOG -> {
                                        val metaParts = (message.mediaMeta ?: "").split("|")
                                        val modeName = metaParts.getOrNull(0) ?: CallMode.VOICE.name
                                        val durationSec = metaParts.getOrNull(1)?.toIntOrNull() ?: 0
                                        val recPath = metaParts.getOrNull(2).orEmpty()
                                        val recWave = metaParts.getOrNull(3).orEmpty()
                                        val hasRec = recPath.isNotBlank() || message.content.contains("Recorded")

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                    contentDescription = "Call log",
                                                    tint = textColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = message.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = textColor
                                                )
                                            }
                                            if (hasRec) {
                                                val mins = durationSec.coerceAtLeast(1) / 60
                                                val secs = durationSec.coerceAtLeast(1) % 60
                                                val durLabel = String.format(Locale.getDefault(), "%d:%02d", mins, secs)
                                                val voiceMeta = "$recPath|${durationSec.coerceAtLeast(1)}|${recWave.ifEmpty { "0.4,0.7,0.5,0.8,0.6,0.4" }}"
                                                VoiceMessageBubbleView(
                                                    durationLabel = durLabel,
                                                    mediaMeta = voiceMeta,
                                                    isMe = isMe,
                                                    contentColor = textColor,
                                                    accentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    MessageType.PHOTO -> {
                                        PhotoAttachmentBubbleView(
                                            message = message
                                        )
                                    }

                                    MessageType.VIDEO -> {
                                        VideoAttachmentBubbleView(
                                            message = message,
                                            onLongPressBubble = onLongPressBubble
                                        )
                                    }

                                    MessageType.FILE -> {
                                        FileAttachmentBubbleView(
                                            message = message,
                                            isMe = isMe,
                                            textColor = textColor,
                                            metaColor = metaColor
                                        )
                                    }

                                    MessageType.POLL -> {
                                        InteractivePollBubbleView(
                                            message = message,
                                            activeUserName = activeUserName,
                                            isMe = isMe,
                                            textColor = textColor,
                                            metaColor = metaColor,
                                            onVoteOption = onUpdateInteractiveMeta
                                        )
                                    }

                                    MessageType.CONTACT -> {
                                        ContactAttachmentBubbleView(
                                            message = message,
                                            isMe = isMe,
                                            textColor = textColor,
                                            metaColor = metaColor
                                        )
                                    }

                                    MessageType.LOCATION -> {
                                        LocationAttachmentBubbleView(
                                            message = message,
                                            textColor = textColor,
                                            metaColor = metaColor
                                        )
                                    }

                                    MessageType.COUPLE_CARD -> {
                                        CoupleExtraCardBubbleView(
                                            message = message,
                                            activeUser = activeUser,
                                            activeUserName = activeUserName,
                                            textColor = textColor,
                                            metaColor = metaColor,
                                            onToggleRedeem = onUpdateInteractiveMeta
                                        )
                                    }

                                    else -> {
                                        Text(
                                            text = message.content,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            color = textColor
                                        )
                                    }
                                }

                                if (isFullMediaBubble) {
                                    // Subtle corner timestamp pill overlaid on the full photo/video
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.52f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            if (message.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            Text(
                                                text = timeFormatter.format(Date(message.createdAt)),
                                                fontSize = 9.5.sp,
                                                color = Color.White
                                            )
                                            if (isMe) {
                                                MessageReceiptTicks(
                                                    isDelivered = message.isDelivered,
                                                    isRead = message.isRead,
                                                    onMediaOverlay = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (!isFullMediaBubble) {
                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (message.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = metaColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Text(
                                        text = timeFormatter.format(Date(message.createdAt)),
                                        fontSize = 10.sp,
                                        color = metaColor
                                    )
                                    if (isMe) {
                                        MessageReceiptTicks(
                                            isDelivered = message.isDelivered,
                                            isRead = message.isRead,
                                            onMediaOverlay = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact Floating Context Box anchored directly to this message
                if (isMenuVisible) {
                    val context = LocalContext.current
                    val isMediaMessage = message.messageType == MessageType.PHOTO || message.messageType == MessageType.VIDEO
                    Popup(
                        alignment = if (isMe) Alignment.TopEnd else Alignment.TopStart,
                        offset = IntOffset(0, -16),
                        onDismissRequest = onDismissMenu,
                        properties = PopupProperties(focusable = true)
                    ) {
                        CompactMessageContextBox(
                            message = message,
                            initialExpandEmojiPicker = initialExpandEmojiPicker,
                            onSelectReaction = onSelectReactionFromMenu,
                            onReply = onReplyFromMenu,
                            onTogglePin = onTogglePinFromMenu,
                            onDrawMedia = if (isMediaMessage) {
                                {
                                    val parts = (message.mediaMeta ?: "").split("|")
                                    val blobName = parts.getOrNull(0).orEmpty()
                                    val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, blobName)
                                    val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                                    onDismissMenu()
                                    if (bmp != null) {
                                        onEditAndDrawMedia(bmp, "")
                                    }
                                }
                            } else null,
                            onSaveMedia = if (isMediaMessage) {
                                {
                                    val parts = (message.mediaMeta ?: "").split("|")
                                    val blobName = parts.getOrNull(0).orEmpty()
                                    val isVideo = message.messageType == MessageType.VIDEO
                                    val fileName = if (isVideo) {
                                        parts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "DuoVideo_${message.id.take(6)}.mp4"
                                    } else {
                                        parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "DuoPhoto_${message.id.take(6)}.jpg"
                                    }
                                    val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, blobName)
                                        ?: message.content.toByteArray(Charsets.UTF_8)
                                    val ok = DuoVaultPersistence.saveMediaBytesToPhone(
                                        context = context,
                                        plainBytes = bytes,
                                        displayName = fileName,
                                        mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                                    )
                                    Toast.makeText(
                                        context,
                                        if (ok) "Saved to phone" else "Saved copy",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismissMenu()
                                }
                            } else null
                        )
                    }
                }
            }

            // Reaction Pill: Tap to toggle, Long-Press to open searchable/scrollable emoji list and swap
            if (!message.reaction.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier
                        .padding(top = 2.dp, start = 6.dp, end = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .combinedClickable(
                            onClick = { onTapReactionBadge(message.reaction) },
                            onLongClick = onLongPressReactionBadge
                        )
                        .testTag("reaction_badge_${message.id}")
                ) {
                    Text(
                        text = message.reaction,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MinimalBottomArea(
    uiState: DuoUiState,
    activeUserName: String,
    user1Name: String,
    user2Name: String,
    messages: List<MessageEntity>,
    onUpdateMessageInput: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendVoiceMessage: (filePath: String, durationSec: Int, waveformCsv: String) -> Unit,
    onSendStickerMessage: (stickerId: String, emoji: String, caption: String, bgColorHex: Long) -> Unit,
    onSendRichAttachment: (content: String, messageType: String, mediaMeta: String) -> Unit,
    onToggleDoodleMode: (Boolean) -> Unit,
    onToggleStickerTray: (Boolean) -> Unit,
    onToggleAttachmentTray: (Boolean) -> Unit,
    onOpenCameraStudio: (initialBitmapToEdit: Bitmap?, initialCaption: String) -> Unit,
    onUpdateInteractiveMessage: (MessageEntity, String) -> Unit = { _, _ -> },
    onClearReply: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val messageInputFocusRequester = remember { FocusRequester() }

    val recorderController = remember(context) { VoiceRecorderController(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // Close sticker tray or attachment tray on system Back press just like dismissing a keyboard
    BackHandler(enabled = uiState.isStickerTrayOpen || uiState.isAttachmentTrayOpen) {
        if (uiState.isAttachmentTrayOpen) {
            onToggleAttachmentTray(false)
        } else {
            onToggleStickerTray(false)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        recorderController.startRecording()
        recordingSeconds = 0
        isRecordingVoice = true
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            val startMs = System.currentTimeMillis()
            while (isActive && isRecordingVoice) {
                recorderController.sampleAmplitude()
                recordingSeconds = ((System.currentTimeMillis() - startMs) / 1000L).toInt()
                delay(200L)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )

            // Reply Quote Bar
            AnimatedVisibility(visible = uiState.replyingTo != null) {
                uiState.replyingTo?.let { reply ->
                    val summary = when (reply.messageType) {
                        MessageType.VOICE -> "🎤 Voice message (${reply.content})"
                        MessageType.STICKER -> "✨ Sticker (${reply.content})"
                        MessageType.CALL_LOG -> "📞 ${reply.content}"
                        MessageType.PHOTO -> "📷 Photo (${reply.content})"
                        MessageType.VIDEO -> "🎬 Video (${reply.content})"
                        MessageType.FILE -> "📎 File (${reply.content})"
                        MessageType.POLL -> "📊 Poll (${reply.content})"
                        MessageType.CONTACT -> "👤 Contact (${reply.content})"
                        MessageType.LOCATION -> "📍 Location (${reply.content})"
                        MessageType.COUPLE_CARD -> "Coupon (${reply.content})"
                        else -> reply.content
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Replying to ${reply.senderName}: $summary",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearReply,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 1. Composer Input Bar (ALWAYS stays above the keyboard OR above the Plus / Sticker Tray!)
            if (isRecordingVoice) {
                RecordingStatusRow(
                    elapsedSec = recordingSeconds,
                    onCancel = {
                        recorderController.stopRecording(cancel = true)
                        isRecordingVoice = false
                    },
                    onSendVoice = {
                        val result = recorderController.stopRecording(cancel = false)
                        isRecordingVoice = false
                        if (result != null) {
                            onSendVoiceMessage(result.first, result.second, result.third)
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Plus (+) Button: Replaces Keyboard with Recent Images/Videos & Attachment Tray!
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isAttachmentTrayOpen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (uiState.isAttachmentTrayOpen) {
                                    onToggleAttachmentTray(false)
                                    messageInputFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } else {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onToggleAttachmentTray(true)
                                }
                            }
                            .testTag("open_attachment_tray_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isAttachmentTrayOpen) {
                                    Icons.Default.Keyboard
                                } else {
                                    Icons.Default.Add
                                },
                                contentDescription = if (uiState.isAttachmentTrayOpen) {
                                    "Switch to keyboard"
                                } else {
                                    "Open attachments tray"
                                },
                                tint = if (uiState.isAttachmentTrayOpen) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // In-App Camera Studio Button (Photos & Videos + Edit/Draw + Save/Send)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onOpenCameraStudio(null, "")
                            }
                            .testTag("open_camera_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Open Camera Studio",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Toggle On-Screen Doodle Mode Button
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isDoodleModeActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!uiState.isDoodleModeActive) {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                                onToggleDoodleMode(!uiState.isDoodleModeActive)
                            }
                            .testTag("open_doodle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Draw on chat screen",
                                tint = if (uiState.isDoodleModeActive) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Toggle Sticker Tray <-> Keyboard Button
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isStickerTrayOpen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (uiState.isStickerTrayOpen) {
                                    onToggleStickerTray(false)
                                    messageInputFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } else {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onToggleStickerTray(true)
                                }
                            }
                            .testTag("open_sticker_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isStickerTrayOpen) {
                                    Icons.Default.Keyboard
                                } else {
                                    Icons.Default.EmojiEmotions
                                },
                                contentDescription = if (uiState.isStickerTrayOpen) {
                                    "Switch to keyboard"
                                } else {
                                    "Open Stickers"
                                },
                                tint = if (uiState.isStickerTrayOpen) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Extended Text Input Field with Microphone Icon Inside the Text Box!
                    val hasText = uiState.messageInput.isNotBlank()
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.messageInput.isEmpty()) {
                                    Text(
                                        text = "Message as $activeUserName…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = uiState.messageInput,
                                    onValueChange = onUpdateMessageInput,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (uiState.messageInput.isNotBlank()) onSendMessage()
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(messageInputFocusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                if (uiState.isStickerTrayOpen) onToggleStickerTray(false)
                                                if (uiState.isAttachmentTrayOpen) onToggleAttachmentTray(false)
                                            }
                                        }
                                        .testTag("chat_message_input")
                                )
                            }

                            // Microphone Icon INSIDE the Text Input Box
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            recorderController.startRecording()
                                            recordingSeconds = 0
                                            isRecordingVoice = true
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    .testTag("record_voice_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (hasText) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onSendMessage)
                                        .testTag("send_message_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Keyboard-Replacing Plus (+) Attachment Tray BELOW the Composer Input Bar
            AnimatedVisibility(visible = uiState.isAttachmentTrayOpen) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    ChatPlusAttachmentTray(
                        activeUser = uiState.activeUser,
                        user1Name = user1Name,
                        user2Name = user2Name,
                        messages = messages,
                        onOpenCameraStudio = { editBmp, cap ->
                            onToggleAttachmentTray(false)
                            onOpenCameraStudio(editBmp, cap)
                        },
                        onSendAttachment = onSendRichAttachment,
                        onUpdateMessageMetaById = { msgId, newMeta ->
                            messages.firstOrNull { it.id == msgId }?.let { targetMsg ->
                                onUpdateInteractiveMessage(targetMsg, newMeta)
                            }
                        },
                        onDismiss = { onToggleAttachmentTray(false) }
                    )
                }
            }

            // 3. Sticker Tray Panel BELOW the Composer Input Bar (Replaces the Keyboard area!)
            AnimatedVisibility(visible = uiState.isStickerTrayOpen) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    StickerTrayPanel(
                        activeUserName = activeUserName,
                        onSendSticker = onSendStickerMessage,
                        onDismiss = { onToggleStickerTray(false) }
                    )
                }
            }
        }
    }
}
