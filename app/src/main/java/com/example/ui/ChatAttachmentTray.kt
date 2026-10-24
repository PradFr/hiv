package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import java.io.ByteArrayOutputStream
import java.util.Locale

enum class AttachmentTrayCategory(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    GALLERY("gallery", "Gallery", Icons.Default.PhotoLibrary),
    FILE("file", "File", Icons.AutoMirrored.Filled.InsertDriveFile),
    POLL("poll", "Poll", Icons.Default.Poll),
    CONTACT("contact", "Contact", Icons.Default.ContactPage),
    LOCATION("location", "Location", Icons.Default.LocationOn),
    EXTRAS("extras", "Coupons", Icons.Default.AutoAwesome)
}

data class QuickRecentMediaItem(
    val id: String,
    val title: String,
    val isVideo: Boolean,
    val durationSec: Int = 0,
    val filterPreset: CameraFilterPreset = CameraFilterPreset.NATURAL,
    val existingBlobName: String? = null,
    val contentUri: Uri? = null,
    val sizeBytes: Long = 0L
)

private fun queryDeviceRecentMedia(context: Context): List<QuickRecentMediaItem> {
    val result = mutableListOf<QuickRecentMediaItem>()
    runCatching {
        val resolver = context.contentResolver
        val imgProjection = arrayOf(
            android.provider.MediaStore.Images.Media._ID,
            android.provider.MediaStore.Images.Media.DISPLAY_NAME,
            android.provider.MediaStore.Images.Media.SIZE
        )
        resolver.query(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imgProjection,
            null,
            null,
            "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.SIZE)
            var count = 0
            while (cursor.moveToNext() && count < 10) {
                if (idCol >= 0) {
                    val mediaId = cursor.getLong(idCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else "Photo_$mediaId.jpg"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    result.add(
                        QuickRecentMediaItem(
                            id = "dev_img_$mediaId",
                            title = name.ifBlank { "Photo_$mediaId.jpg" },
                            isVideo = false,
                            contentUri = uri,
                            sizeBytes = size
                        )
                    )
                    count++
                }
            }
        }
    }
    return result
}

/**
 * Keyboard-Replacing Plus (+) Attachment Tray.
 * Shows Recent Images & Videos at the top when Gallery is selected, and has a bottom bar with
 * Gallery, File, Poll, Contact, Location, and Couple Extras (Love Coupons / Dares / Mood).
 */
@Composable
fun ChatPlusAttachmentTray(
    activeUser: TestUserSlot,
    user1Name: String,
    user2Name: String,
    messages: List<MessageEntity>,
    onOpenCameraStudio: (initialBitmapToEdit: Bitmap?, initialCaption: String) -> Unit,
    onSendAttachment: (content: String, messageType: String, mediaMeta: String) -> Unit,
    onUpdateMessageMetaById: (messageId: String, newMediaMeta: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name
    val partnerName = if (activeUser == TestUserSlot.USER_1) user2Name else user1Name

    var selectedCategory by remember { mutableStateOf(AttachmentTrayCategory.GALLERY) }

    // Load actual user Vault media nodes + device photos/videos (no fake presets)
    val vaultNodes = remember(user1Name, user2Name, messages) {
        DuoVaultPersistence.loadNodes(context, user1Name, user2Name, messages)
    }

    val recentMediaItems = remember(vaultNodes) {
        val fromDevice = queryDeviceRecentMedia(context)
        val fromVault = vaultNodes
            .filter { !it.isFolder && (it.category == VaultItemCategory.IMAGES || it.category == VaultItemCategory.VIDEOS) }
            .sortedByDescending { it.createdAt }
            .take(12)
            .map { node ->
                QuickRecentMediaItem(
                    id = node.id,
                    title = node.name,
                    isVideo = node.category == VaultItemCategory.VIDEOS,
                    durationSec = if (node.category == VaultItemCategory.VIDEOS) 6 else 0,
                    filterPreset = CameraFilterPreset.NATURAL,
                    existingBlobName = node.encryptedBlobFileName,
                    sizeBytes = node.sizeBytes
                )
            }
        fromDevice + fromVault
    }

    // 1. System Photo & Video Picker (Direct Send as full photo/video)
    val galleryDirectSendLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri).orEmpty()
                val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val isVideo = mime.startsWith("video/")
                val finalBytes = if (!isVideo) {
                    VaultCryptoEngine.optimizeImageBytes(rawBytes)
                } else {
                    rawBytes
                }
                val blobName = DuoVaultPersistence.writeEncryptedBlob(context, finalBytes)
                val ts = System.currentTimeMillis()
                if (isVideo) {
                    val fileName = "GalleryVideo_$ts.mp4"
                    val meta = "$blobName|6|${finalBytes.size}|$fileName|Natural"
                    onSendAttachment("", MessageType.VIDEO, meta)
                } else {
                    val fileName = "GalleryPhoto_$ts.jpg"
                    val meta = "$blobName|${finalBytes.size}|$fileName|Natural"
                    onSendAttachment("", MessageType.PHOTO, meta)
                }
            }
        }
    }

    // 2. System Document / File Picker (Strictly Non-Images and Non-Videos)
    val nonMediaMimeTypes = remember {
        arrayOf(
            "application/pdf",
            "text/*",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/x-rar-compressed",
            "application/json",
            "application/xml",
            "audio/*",
            "application/octet-stream"
        )
    }

    val documentFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                var displayName = "Document_${System.currentTimeMillis()}"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            c.getString(idx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                        }
                    }
                }
                val lowerMime = mimeType.lowercase(Locale.US)
                val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)
                val isImageOrVideo = lowerMime.startsWith("image/") ||
                    lowerMime.startsWith("video/") ||
                    ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "mp4", "mov", "mkv", "webm", "avi")
                if (isImageOrVideo) {
                    Toast.makeText(
                        context,
                        "Files tab is for non-image and non-video documents only.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@runCatching
                }
                val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val blobName = DuoVaultPersistence.writeEncryptedBlob(context, rawBytes)
                // mediaMeta format for FILE: blobName|sizeBytes|mimeType
                val meta = "$blobName|${rawBytes.size}|$mimeType"
                onSendAttachment(displayName, MessageType.FILE, meta)
            }
        }
    }

    // 3. Zero-Permission System Phone Contact Picker (Picks real Contact Name + Phone Number)
    var contactNameInput by remember { mutableStateOf("") }
    var contactPhoneInput by remember { mutableStateOf("") }
    var contactNoteInput by remember { mutableStateOf("") }

    val phoneContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contactUri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && contactUri != null) {
            var pickedName = ""
            var pickedPhone = ""
            runCatching {
                context.contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nameIdx >= 0) {
                            pickedName = cursor.getString(nameIdx).orEmpty().trim()
                        }
                        if (phoneIdx >= 0) {
                            pickedPhone = cursor.getString(phoneIdx).orEmpty().trim()
                        }
                    }
                }
            }
            if (pickedName.isNotEmpty() || pickedPhone.isNotEmpty()) {
                val finalName = pickedName.ifEmpty { pickedPhone }
                val finalPhone = pickedPhone.ifEmpty { contactPhoneInput.trim() }
                val finalNote = contactNoteInput.trim()
                contactNameInput = finalName
                contactPhoneInput = finalPhone
                val meta = "$finalName|$finalPhone|$finalNote"
                onSendAttachment(finalName, MessageType.CONTACT, meta)
            }
        }
    }

    val fallbackContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            var pickedName = ""
            runCatching {
                context.contentResolver.query(
                    contactUri,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (idx >= 0) {
                            pickedName = cursor.getString(idx).orEmpty().trim()
                        }
                    }
                }
            }
            if (pickedName.isNotEmpty()) {
                contactNameInput = pickedName
                if (contactPhoneInput.isNotBlank()) {
                    val meta = "$pickedName|${contactPhoneInput.trim()}|${contactNoteInput.trim()}"
                    onSendAttachment(pickedName, MessageType.CONTACT, meta)
                }
            }
        }
    }

    // 4. Runtime Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val (lat, lng) = resolveDeviceOrFallbackCoordinates(context)
        val label = "Current Location ($activeUserName)"
        val meta = "$lat|$lng|$label"
        onSendAttachment(label, MessageType.LOCATION, meta)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp)
            .testTag("chat_attachment_tray")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // TOP / MAIN TRAY AREA (Switches by Selected Bottom Option)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (selectedCategory) {
                    AttachmentTrayCategory.GALLERY -> {
                        GalleryRecentMediaTraySection(
                            context = context,
                            recentItems = recentMediaItems,
                            onOpenCamera = { onOpenCameraStudio(null, "") },
                            onOpenSystemGallerySend = {
                                galleryDirectSendLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onSendRecentItem = { item ->
                                val (blobName, byteSize) = when {
                                    !item.existingBlobName.isNullOrBlank() -> {
                                        item.existingBlobName to item.sizeBytes.coerceAtLeast(32_000L)
                                    }
                                    item.contentUri != null -> {
                                        val raw = runCatching {
                                            context.contentResolver.openInputStream(item.contentUri)?.use { it.readBytes() }
                                        }.getOrNull()
                                        if (raw != null && raw.isNotEmpty()) {
                                            val optimized = if (!item.isVideo) VaultCryptoEngine.optimizeImageBytes(raw) else raw
                                            DuoVaultPersistence.writeEncryptedBlob(context, optimized) to optimized.size.toLong()
                                        } else {
                                            return@GalleryRecentMediaTraySection
                                        }
                                    }
                                    else -> return@GalleryRecentMediaTraySection
                                }
                                if (item.isVideo) {
                                    val fileName = "${item.title.replace(" ", "_")}.mp4"
                                    val meta = "$blobName|${item.durationSec.coerceAtLeast(5)}|$byteSize|$fileName|Natural"
                                    onSendAttachment("", MessageType.VIDEO, meta)
                                } else {
                                    val fileName = "${item.title.replace(" ", "_")}.jpg"
                                    val meta = "$blobName|$byteSize|$fileName|Natural"
                                    onSendAttachment("", MessageType.PHOTO, meta)
                                }
                            }
                        )
                    }

                    AttachmentTrayCategory.FILE -> {
                        val nonMediaVaultFiles = remember(vaultNodes) {
                            vaultNodes.filter { node ->
                                !node.isFolder &&
                                    node.category != VaultItemCategory.IMAGES &&
                                    node.category != VaultItemCategory.VIDEOS &&
                                    !(node.mimeType?.startsWith("image/", ignoreCase = true) == true) &&
                                    !(node.mimeType?.startsWith("video/", ignoreCase = true) == true)
                            }
                        }
                        FileAttachmentTraySection(
                            vaultFiles = nonMediaVaultFiles,
                            onPickDeviceFile = { documentFileLauncher.launch(nonMediaMimeTypes) },
                            onSendVaultFile = { node ->
                                val blob = node.encryptedBlobFileName ?: DuoVaultPersistence.writeEncryptedBlob(
                                    context,
                                    "Shared Vault File: ${node.name}".toByteArray(Charsets.UTF_8)
                                )
                                val mime = node.mimeType ?: "application/octet-stream"
                                val meta = "$blob|${node.sizeBytes.coerceAtLeast(1024L)}|$mime"
                                onSendAttachment(node.name, MessageType.FILE, meta)
                            }
                        )
                    }

                    AttachmentTrayCategory.POLL -> {
                        PollCreatorTraySection(
                            onSendPoll = { question, options ->
                                // mediaMeta format for POLL: opt1~votesCsv|opt2~votesCsv|...
                                val encodedOptions = options.joinToString("|") { opt ->
                                    "${opt.replace("|", " ").replace("~", " ")}~"
                                }
                                onSendAttachment(question, MessageType.POLL, encodedOptions)
                            }
                        )
                    }

                    AttachmentTrayCategory.CONTACT -> {
                        ContactAttachmentTraySection(
                            nameInput = contactNameInput,
                            phoneInput = contactPhoneInput,
                            noteInput = contactNoteInput,
                            onNameChange = { contactNameInput = it },
                            onPhoneChange = { contactPhoneInput = it },
                            onNoteChange = { contactNoteInput = it },
                            onPickPhoneContact = {
                                runCatching {
                                    val pickPhoneIntent = Intent(
                                        Intent.ACTION_PICK,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                    )
                                    phoneContactPickerLauncher.launch(pickPhoneIntent)
                                }.onFailure {
                                    fallbackContactPickerLauncher.launch(null)
                                }
                            },
                            onSendContact = { name, phone, role ->
                                val meta = "$name|$phone|$role"
                                onSendAttachment(name, MessageType.CONTACT, meta)
                                contactNameInput = ""
                                contactPhoneInput = ""
                                contactNoteInput = ""
                            }
                        )
                    }

                    AttachmentTrayCategory.LOCATION -> {
                        LocationAttachmentTraySection(
                            context = context,
                            activeUserName = activeUserName,
                            onShareLiveGps = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    val (lat, lng) = resolveDeviceOrFallbackCoordinates(context)
                                    val label = "Current Location ($activeUserName)"
                                    onSendAttachment(label, MessageType.LOCATION, "$lat|$lng|$label")
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            onSendPinnedPlace = { title, lat, lng ->
                                onSendAttachment(title, MessageType.LOCATION, "$lat|$lng|$title")
                            }
                        )
                    }

                    AttachmentTrayCategory.EXTRAS -> {
                        CoupleExtrasTraySection(
                            activeUser = activeUser,
                            activeUserName = activeUserName,
                            partnerName = partnerName,
                            onSendCoupleCard = { title, subtitle, badgeType ->
                                // mediaMeta format for COUPLE_CARD: badgeType||subtitle|ACTIVE
                                val meta = "$badgeType||$subtitle|ACTIVE"
                                onSendAttachment(title, MessageType.COUPLE_CARD, meta)
                            },
                            onRedeemReceivedCoupon = { savedCoupon ->
                                val meta = "${savedCoupon.badgeType}|${savedCoupon.messageId}|${savedCoupon.subtitle}|SHARED_TO_REDEEM"
                                onSendAttachment(savedCoupon.title, MessageType.COUPLE_CARD, meta)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            // =========================================================================
            // BOTTOM CATEGORY SELECTOR BAR (Gallery • File • Poll • Contact • Location • Coupons)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentTrayCategory.entries.forEach { cat ->
                    val selected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedCategory = cat }
                            .testTag("attachment_tab_${cat.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.label,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = cat.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryRecentMediaTraySection(
    context: Context,
    recentItems: List<QuickRecentMediaItem>,
    onOpenCamera: () -> Unit,
    onOpenSystemGallerySend: () -> Unit,
    onSendRecentItem: (QuickRecentMediaItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Images & Videos",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenSystemGallerySend)
                    .testTag("tray_open_full_gallery_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "All Photos/Videos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Scrollable Strip of Camera Tile + Recent Images & Videos
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("recent_media_strip"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 6.dp)
        ) {
            // 1. Live Camera Quick Tile
            item(key = "camera_quick_tile") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .width(114.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenCamera)
                        .testTag("tray_camera_tile")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Open Camera",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Photo & Video",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // 2. Actual User Recent Images & Videos Cards (or Pick from Device tile when none recorded yet)
            if (recentItems.isEmpty()) {
                item(key = "pick_device_media_tile") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .width(210.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenSystemGallerySend)
                            .testTag("tray_pick_device_photos_tile")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Browse Photos & Videos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select from Phone Gallery",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your recent photos & videos appear here",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentItems, key = { it.id }) { item ->
                    val thumbBitmap = remember(item.id, item.existingBlobName, item.contentUri) {
                        when {
                            !item.existingBlobName.isNullOrBlank() -> {
                                val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, item.existingBlobName)
                                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                            }
                            item.contentUri != null -> {
                                runCatching {
                                    context.contentResolver.openInputStream(item.contentUri)?.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }
                                }.getOrNull()
                            }
                            else -> null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(132.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSendRecentItem(item) }
                            .testTag("recent_media_item_${item.id}")
                    ) {
                        if (thumbBitmap != null) {
                            Image(
                                bitmap = thumbBitmap.asImageBitmap(),
                                contentDescription = "Recent media",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (item.isVideo) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.58f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "0:${String.format(Locale.US, "%02d", item.durationSec.coerceAtLeast(5))}",
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
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

private fun resolveBitmapForRecentItem(
    context: Context,
    item: QuickRecentMediaItem,
    activeUserName: String,
    partnerName: String
): Bitmap {
    if (!item.existingBlobName.isNullOrBlank()) {
        val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, item.existingBlobName)
        if (bytes != null) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
        }
    }
    return generateStudioSceneBitmap(
        width = 960,
        height = 1280,
        isFrontLens = false,
        isVideo = item.isVideo,
        durationSec = item.durationSec,
        activeUserName = activeUserName,
        partnerName = partnerName,
        filter = item.filterPreset
    )
}

@Composable
private fun FileAttachmentTraySection(
    vaultFiles: List<VaultNode>,
    onPickDeviceFile: () -> Unit,
    onSendVaultFile: (VaultNode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPickDeviceFile,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tray_pick_device_file_button")
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Browse Phone Documents & Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            text = "Non-media files in your Vault (${vaultFiles.size}):",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (vaultFiles.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Documents, PDFs, audio, and non-media files in your Vault will appear here.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            vaultFiles.take(6).forEach { node ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSendVaultFile(node) }
                        .testTag("tray_vault_file_${node.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = node.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${DuoVaultPersistence.formatBytes(node.sizeBytes)} • Tap to send",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Vault File",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PollCreatorTraySection(
    onSendPoll: (question: String, options: List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Templates Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                Triple("What's for dinner tonight?", "Sushi 🍣", "Tacos 🌮") to "Thai Curry 🍜",
                Triple("Movie night pick?", "Sci-Fi Thriller 🎬", "Rom-Com 🍿") to "Horror 👻",
                Triple("Weekend plan?", "Cozy Stay-In 🏠", "Road Trip 🚗") to "Brunch Date 🥞"
            )
            presets.forEachIndexed { idx, (qAnd2, opt3) ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            question = qAnd2.first
                            option1 = qAnd2.second
                            option2 = qAnd2.third
                            option3 = opt3
                        }
                        .testTag("poll_preset_$idx")
                ) {
                    Text(
                        text = qAnd2.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        TrayCompactInputField(
            value = question,
            onValueChange = { question = it },
            placeholder = "Poll question (e.g. What should we eat tonight?)",
            testTag = "poll_question_input"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrayCompactInputField(
                value = option1,
                onValueChange = { option1 = it },
                placeholder = "Option 1",
                modifier = Modifier.weight(1f),
                testTag = "poll_option_1_input"
            )
            TrayCompactInputField(
                value = option2,
                onValueChange = { option2 = it },
                placeholder = "Option 2",
                modifier = Modifier.weight(1f),
                testTag = "poll_option_2_input"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrayCompactInputField(
                value = option3,
                onValueChange = { option3 = it },
                placeholder = "Option 3 (optional)",
                modifier = Modifier.weight(1f),
                testTag = "poll_option_3_input"
            )

            Button(
                onClick = {
                    val q = question.trim().ifEmpty { "What's our pick?" }
                    val opts = listOf(
                        option1.trim().ifEmpty { "Option A" },
                        option2.trim().ifEmpty { "Option B" },
                        option3.trim()
                    ).filter { it.isNotEmpty() }
                    onSendPoll(q, opts)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("send_poll_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Poll", fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun ContactAttachmentTraySection(
    nameInput: String,
    phoneInput: String,
    noteInput: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPickPhoneContact: () -> Unit,
    onSendContact: (name: String, phone: String, role: String) -> Unit
) {
    val canSend = nameInput.isNotBlank() || phoneInput.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Button(
            onClick = onPickPhoneContact,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pick_phone_contact_button")
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick from Phone Contacts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrayCompactInputField(
                value = nameInput,
                onValueChange = onNameChange,
                placeholder = "Contact Name",
                modifier = Modifier.weight(1f),
                testTag = "contact_name_input"
            )
            TrayCompactInputField(
                value = phoneInput,
                onValueChange = onPhoneChange,
                placeholder = "Phone Number",
                modifier = Modifier.weight(1f),
                testTag = "contact_phone_input"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrayCompactInputField(
                value = noteInput,
                onValueChange = onNoteChange,
                placeholder = "Label / Note (optional)",
                modifier = Modifier.weight(1f),
                testTag = "contact_note_input"
            )

            Button(
                onClick = {
                    val finalName = nameInput.trim().ifEmpty { phoneInput.trim() }
                    val finalPhone = phoneInput.trim()
                    val finalRole = noteInput.trim()
                    if (finalName.isNotEmpty() || finalPhone.isNotEmpty()) {
                        onSendContact(finalName, finalPhone, finalRole)
                    }
                },
                enabled = canSend,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("send_contact_button")
            ) {
                Text("Send Contact", fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun LocationAttachmentTraySection(
    context: Context,
    activeUserName: String,
    onShareLiveGps: () -> Unit,
    onSendPinnedPlace: (title: String, lat: Double, lng: Double) -> Unit
) {
    var searchPlaceInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onShareLiveGps,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_live_gps_button")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Current Live GPS Location", fontWeight = FontWeight.SemiBold)
        }

        TrayCompactInputField(
            value = searchPlaceInput,
            onValueChange = { searchPlaceInput = it },
            placeholder = "Search place or address for Google Maps…",
            modifier = Modifier.fillMaxWidth(),
            testTag = "custom_location_input"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val query = searchPlaceInput.trim()
                    val geoUri = if (query.isNotEmpty()) {
                        Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                    } else {
                        val (lat, lng) = resolveDeviceOrFallbackCoordinates(context)
                        Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                    }
                    runCatching {
                        val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                            setPackage("com.google.android.apps.maps")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(mapsIntent)
                    }.recoverCatching {
                        val fallbackIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                if (query.isNotEmpty()) {
                                    "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
                                } else {
                                    val (lat, lng) = resolveDeviceOrFallbackCoordinates(context)
                                    "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                                }
                            )
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallbackIntent)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_google_maps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Google Maps", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val query = searchPlaceInput.trim()
                    if (query.isNotEmpty()) {
                        val (lat, lng) = resolveCoordinatesForPlaceQuery(context, query)
                        onSendPinnedPlace(query, lat, lng)
                        searchPlaceInput = ""
                    } else {
                        onShareLiveGps()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("send_custom_location_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Maps Pin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun resolveCoordinatesForPlaceQuery(context: Context, query: String): Pair<Double, Double> {
    return try {
        if (Geocoder.isPresent()) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(query, 1)
            val first = results?.firstOrNull()
            if (first != null) {
                return first.latitude to first.longitude
            }
        }
        resolveDeviceOrFallbackCoordinates(context)
    } catch (_: Exception) {
        resolveDeviceOrFallbackCoordinates(context)
    }
}

@Composable
private fun CoupleExtrasTraySection(
    activeUser: TestUserSlot,
    activeUserName: String,
    partnerName: String,
    onSendCoupleCard: (title: String, subtitle: String, badgeType: String) -> Unit,
    onRedeemReceivedCoupon: (SavedCouponItem) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("duospace_user_coupons_prefs", Context.MODE_PRIVATE)
    }

    var selectedCouponSubTab by remember { mutableStateOf("CREATED") } // "CREATED" or "RECEIVED"

    var savedCoupons by remember {
        val raw = prefs.getString("user_coupons_v1", "").orEmpty()
        val parsed = raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t")
                when {
                    parts.size >= 4 -> listOf(parts[0], parts[1], parts[3])
                    parts.size >= 2 -> listOf(parts[0], parts[1], parts.getOrElse(2) { "COUPON" })
                    else -> null
                }
            }
        mutableStateOf(parsed)
    }

    var receivedCoupons by remember(activeUser, selectedCouponSubTab) {
        mutableStateOf(HiveSettingsPersistence.loadReceivedCoupons(context, activeUser))
    }

    fun persistCoupons(updated: List<List<String>>) {
        savedCoupons = updated
        val serialized = updated.joinToString("\n") { item ->
            item.joinToString("\t") { it.replace("\t", " ").replace("\n", " ") }
        }
        prefs.edit().putString("user_coupons_v1", serialized).apply()
    }

    var customTitle by remember { mutableStateOf("") }
    var customSubtitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sub-tabs: Created Coupons vs Received Coupons (saved from partner)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isCreated = selectedCouponSubTab == "CREATED"
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isCreated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable { selectedCouponSubTab = "CREATED" }
                    .testTag("coupon_subtab_created")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create & Saved (${savedCoupons.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCreated) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isReceived = selectedCouponSubTab == "RECEIVED"
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isReceived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        receivedCoupons = HiveSettingsPersistence.loadReceivedCoupons(context, activeUser)
                        selectedCouponSubTab = "RECEIVED"
                    }
                    .testTag("coupon_subtab_received")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Received (${receivedCoupons.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceived) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (selectedCouponSubTab == "RECEIVED") {
            if (receivedCoupons.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "When $partnerName sends you a coupon, tap Save on the coupon bubble to keep it here. Share it in chat whenever you want to redeem it!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                receivedCoupons.forEachIndexed { idx, coupon ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("received_coupon_card_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = coupon.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${coupon.subtitle} • From ${coupon.fromUserName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    onRedeemReceivedCoupon(coupon)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("share_received_coupon_$idx")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Share coupon in chat",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Share", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            TrayCompactInputField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = "Coupon title (e.g., 1 Free Back Massage)…",
                modifier = Modifier.fillMaxWidth(),
                testTag = "coupon_title_input"
            )

            TrayCompactInputField(
                value = customSubtitle,
                onValueChange = { customSubtitle = it },
                placeholder = "Details or terms for $partnerName (optional)…",
                modifier = Modifier.fillMaxWidth(),
                testTag = "coupon_subtitle_input"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cleanTitle = customTitle.trim()
                        if (cleanTitle.isNotEmpty()) {
                            val cleanSub = customSubtitle.trim().ifEmpty { "Redeemable by $partnerName" }
                            val entry = listOf(cleanTitle, cleanSub, "COUPON")
                            persistCoupons(listOf(entry) + savedCoupons)
                            customTitle = ""
                            customSubtitle = ""
                        }
                    },
                    enabled = customTitle.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_custom_coupon_button")
                ) {
                    Text("Save Coupon", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val cleanTitle = customTitle.trim()
                        if (cleanTitle.isNotEmpty()) {
                            val cleanSub = customSubtitle.trim().ifEmpty { "Redeemable by $partnerName" }
                            onSendCoupleCard(cleanTitle, cleanSub, "COUPON")
                            customTitle = ""
                            customSubtitle = ""
                        }
                    },
                    enabled = customTitle.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_custom_coupon_button")
                ) {
                    Text("Send Coupon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (savedCoupons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Saved Templates (${savedCoupons.size})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                savedCoupons.forEachIndexed { idx, item ->
                    val title = item[0]
                    val subtitle = item[1]
                    val badgeType = item.getOrElse(2) { "COUPON" }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSendCoupleCard(title, subtitle, badgeType)
                            }
                            .testTag("couple_extra_card_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val next = savedCoupons.toMutableList().also { it.removeAt(idx) }
                                            persistCoupons(next)
                                        }
                                        .testTag("delete_saved_coupon_$idx")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Coupon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
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
private fun TrayCompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            )
        }
    }
}

private fun resolveDeviceOrFallbackCoordinates(context: Context): Pair<Double, Double> {
    return try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            loc.latitude to loc.longitude
        } else {
            37.7858 to -122.4065
        }
    } catch (_: SecurityException) {
        37.7858 to -122.4065
    } catch (_: Exception) {
        37.7858 to -122.4065
    }
}
