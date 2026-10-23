package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class WishItem(
    val id: String,
    val name: String,
    val link: String?,
    val photoPath: String?,
    val addedBy: TestUserSlot,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object DuoWishlistPersistence {
    private const val LEGACY_PREFS_NAME = "duo_space_wishlist_prefs_v1"
    private const val KEY_JSON = "wishlist_items_json"
    private const val SANDBOX_DIR = "sandboxed_wishlist_e2ee"
    private const val INDEX_FILE = "wishlist_index.venc"

    private fun wishlistSandboxDir(context: Context): File {
        val dir = File(context.noBackupFilesDir, SANDBOX_DIR)
        if (!dir.exists()) dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) runCatching { noMedia.createNewFile() }
        return dir
    }

    fun loadItems(context: Context): List<WishItem> {
        val dir = wishlistSandboxDir(context)
        val indexFile = File(dir, INDEX_FILE)
        var raw: String? = null

        if (indexFile.exists()) {
            val decrypted = VaultCryptoEngine.decryptBytes(
                context,
                runCatching { indexFile.readBytes() }.getOrNull() ?: ByteArray(0)
            )
            if (decrypted != null) {
                raw = String(decrypted, Charsets.UTF_8)
            }
        }

        if (raw.isNullOrBlank()) {
            val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            val legacyRaw = prefs.getString(KEY_JSON, null)
            if (!legacyRaw.isNullOrBlank()) {
                raw = legacyRaw
                prefs.edit().clear().apply()
            }
        }

        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<WishItem>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val addedBy = if (obj.optString("addedBy") == TestUserSlot.USER_2.name) {
                    TestUserSlot.USER_2
                } else {
                    TestUserSlot.USER_1
                }
                val link = obj.optString("link", "").takeIf { it.isNotBlank() }
                val photoPath = obj.optString("photoPath", "").takeIf { it.isNotBlank() }
                list.add(
                    WishItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        link = link,
                        photoPath = photoPath,
                        addedBy = addedBy,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveItems(context: Context, items: List<WishItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("link", item.link ?: "")
            obj.put("photoPath", item.photoPath ?: "")
            obj.put("addedBy", item.addedBy.name)
            obj.put("isCompleted", item.isCompleted)
            obj.put("createdAt", item.createdAt)
            arr.put(obj)
        }
        val encrypted = VaultCryptoEngine.encryptBytes(context, arr.toString().toByteArray(Charsets.UTF_8))
        val indexFile = File(wishlistSandboxDir(context), INDEX_FILE)
        runCatching { indexFile.writeBytes(encrypted) }
        com.example.data.remote.DuoCloudRelaySync.enqueueRelayUpload(INDEX_FILE, encrypted)
    }

    fun copyPickedPhotoToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return null
            // Signal-style Smart Image Sizer: shrinks 10 MB camera photos down to ~250 KB before encrypting
            val optimizedBytes = VaultCryptoEngine.optimizeImageBytes(rawBytes)
            val encryptedBytes = VaultCryptoEngine.encryptBytes(context, optimizedBytes)
            val fileName = "wish_${System.currentTimeMillis()}.venc"
            val destFile = File(wishlistSandboxDir(context), fileName)
            destFile.writeBytes(encryptedBytes)
            com.example.data.remote.DuoCloudRelaySync.enqueueRelayUpload(fileName, encryptedBytes)
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun decodePhotoInMemory(context: Context, path: String?): ByteArray? {
        if (path.isNullOrBlank()) return null
        return try {
            val file = File(path)
            if (!file.exists()) {
                val fileName = file.name
                com.example.data.remote.DuoCloudRelaySync.ensureLocalSandboxFileOrPullFromRelay(
                    targetDir = wishlistSandboxDir(context),
                    blobFileName = fileName
                )
                return null
            }
            val bytes = file.readBytes()
            if (path.endsWith(".venc")) {
                VaultCryptoEngine.decryptBytes(context, bytes)
            } else {
                bytes
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun DuoWishlistScreen(
    activeUser: TestUserSlot,
    user1Name: String,
    user2Name: String,
    onSwitchUser: (TestUserSlot) -> Unit,
    onBackToDashboard: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenVault: () -> Unit = {},
    unreadChatCount: Int
) {
    BackHandler(onBack = onBackToDashboard)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var wishItems by remember {
        mutableStateOf(DuoWishlistPersistence.loadItems(context))
    }

    fun updateWishItems(updated: List<WishItem>) {
        wishItems = updated
        DuoWishlistPersistence.saveItems(context, updated)
    }

    // Add Wish Modal State (Closed by default; opens when user clicks "Add Wish")
    var showAddWishDialog by rememberSaveable { mutableStateOf(false) }
    var wishNameInput by rememberSaveable { mutableStateOf("") }
    var wishLinkInput by rememberSaveable { mutableStateOf("") }
    var wishPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    // 0 = All, 1 = Person 1, 2 = Person 2
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = DuoWishlistPersistence.copyPickedPhotoToInternalStorage(context, uri)
            if (savedPath != null) {
                wishPhotoPath = savedPath
            }
        }
    }

    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name

    val filteredItems = remember(wishItems, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> wishItems.filter { it.addedBy == TestUserSlot.USER_1 }
            2 -> wishItems.filter { it.addedBy == TestUserSlot.USER_2 }
            else -> wishItems
        }
    }

    if (showAddWishDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddWishDialog = false },
            title = {
                Text(
                    text = "Add Wish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Name of the wish
                    OutlinedTextField(
                        value = wishNameInput,
                        onValueChange = { wishNameInput = it },
                        placeholder = {
                            Text(text = "Name of the wish", fontSize = 13.5.sp)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wishlist_name_input")
                    )

                    // 2. Optional link
                    OutlinedTextField(
                        value = wishLinkInput,
                        onValueChange = { wishLinkInput = it },
                        placeholder = {
                            Text(text = "Optional link (https://...)", fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Link",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wishlist_link_input")
                    )

                    // 3. Optional photo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .testTag("wishlist_photo_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (wishPhotoPath != null) {
                                        Icons.Default.Image
                                    } else {
                                        Icons.Default.AddPhotoAlternate
                                    },
                                    contentDescription = "Optional photo",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = if (wishPhotoPath != null) "Photo added" else "Optional photo",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (wishPhotoPath != null) {
                            IconButton(
                                onClick = { wishPhotoPath = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = wishNameInput.trim()
                        if (trimmedName.isNotEmpty()) {
                            val normalizedLink = wishLinkInput.trim().takeIf { it.isNotEmpty() }?.let { raw ->
                                if (raw.startsWith("http://") || raw.startsWith("https://")) {
                                    raw
                                } else {
                                    "https://$raw"
                                }
                            }
                            val newItem = WishItem(
                                id = UUID.randomUUID().toString(),
                                name = trimmedName,
                                link = normalizedLink,
                                photoPath = wishPhotoPath,
                                addedBy = activeUser
                            )
                            updateWishItems(listOf(newItem) + wishItems)
                            wishNameInput = ""
                            wishLinkInput = ""
                            wishPhotoPath = null
                            showAddWishDialog = false
                        }
                    },
                    enabled = wishNameInput.trim().isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("wishlist_add_submit_button")
                ) {
                    Text(text = "Add Wish", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showAddWishDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
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
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wishlist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hive • $user1Name & $user2Name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            ModernBottomNavBar(
                currentDestination = DuoBottomNavDestination.WISHLIST,
                unreadChatCount = unreadChatCount,
                onClickDashboard = onBackToDashboard,
                onClickChat = onOpenChat,
                onClickJournal = onOpenJournal,
                onClickWishlist = {},
                onClickVault = onOpenVault
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Single "Add Wish" Button in the Wishlist Tab
            Button(
                onClick = { showAddWishDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("wishlist_open_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Wish",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Filter Tabs (All | Person 1 | Person 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WishlistFilterChip(
                    modifier = Modifier.weight(1f),
                    label = "All (${wishItems.size})",
                    isSelected = selectedFilterIndex == 0,
                    testTag = "wishlist_filter_all",
                    onClick = { selectedFilterIndex = 0 }
                )
                WishlistFilterChip(
                    modifier = Modifier.weight(1f),
                    label = "$user1Name (${wishItems.count { it.addedBy == TestUserSlot.USER_1 }})",
                    isSelected = selectedFilterIndex == 1,
                    testTag = "wishlist_filter_user_1",
                    onClick = { selectedFilterIndex = 1 }
                )
                WishlistFilterChip(
                    modifier = Modifier.weight(1f),
                    label = "$user2Name (${wishItems.count { it.addedBy == TestUserSlot.USER_2 }})",
                    isSelected = selectedFilterIndex == 2,
                    testTag = "wishlist_filter_user_2",
                    onClick = { selectedFilterIndex = 2 }
                )
            }

            // Wishlist Items List
            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No wishes yet. Tap Add Wish above.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val ownerName = if (item.addedBy == TestUserSlot.USER_1) user1Name else user2Name
                        WishItemCard(
                            item = item,
                            ownerName = ownerName,
                            onToggleComplete = {
                                updateWishItems(
                                    wishItems.map {
                                        if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                                    }
                                )
                            },
                            onDelete = {
                                updateWishItems(wishItems.filterNot { it.id == item.id })
                            },
                            onOpenLink = { url ->
                                runCatching { uriHandler.openUri(url) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistFilterChip(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (!isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        } else {
            null
        },
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WishItemCard(
    item: WishItem,
    ownerName: String,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val context = LocalContext.current
    val decodedBitmap = remember(item.photoPath) {
        DuoWishlistPersistence.decodePhotoInMemory(context, item.photoPath)?.let { plainBytes ->
            runCatching {
                BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wishlist_item_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Check / Complete Circle
                    Surface(
                        shape = CircleShape,
                        color = if (item.isCompleted) {
                            Color(0xFF10B981)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (!item.isCompleted) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleComplete)
                    ) {
                        if (item.isCompleted) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (item.isCompleted) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            }
                        )
                        Text(
                            text = "Added by $ownerName",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete wish",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Optional Photo Preview
            if (decodedBitmap != null) {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            // Optional Link Pill
            if (!item.link.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenLink(item.link) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Open link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.link,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
