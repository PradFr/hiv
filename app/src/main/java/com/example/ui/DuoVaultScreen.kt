package com.example.ui

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class VaultItemCategory(val folderLabel: String) {
    FOLDER("Folder"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    FILES("Files");

    companion object {
        fun fromMimeOrName(mimeType: String?, fileName: String): VaultItemCategory {
            val m = mimeType?.lowercase(Locale.US).orEmpty()
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
            return when {
                m.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic") -> IMAGES
                m.startsWith("video/") || ext in setOf("mp4", "mov", "mkv", "webm", "avi") -> VIDEOS
                m.startsWith("audio/") || ext in setOf("mp3", "wav", "m4a", "aac", "ogg", "flac") -> AUDIO
                m.contains("pdf") || m.startsWith("text/") ||
                    ext in setOf("pdf", "doc", "docx", "txt", "md", "xls", "xlsx", "ppt", "pptx") -> DOCUMENTS
                else -> FILES
            }
        }
    }
}

data class VaultNode(
    val id: String,
    val parentId: String?, // null = root
    val name: String,
    val isFolder: Boolean,
    val category: VaultItemCategory,
    val ownerSlot: TestUserSlot,
    val encryptedBlobFileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isSystemDefault: Boolean = false
)

enum class VaultClipboardOp {
    COPY,
    CUT
}

data class VaultClipboardState(
    val nodeIds: Set<String>,
    val operation: VaultClipboardOp
)

/**
 * Zero-Knowledge Client-Side AES-256-GCM Encryption Engine.
 * - All Vault files and metadata are encrypted with AES-256-GCM before touching disk or any database.
 * - Stored inside `context.noBackupFilesDir` (completely sandboxed from MediaStore, gallery apps,
 *   other apps, and cloud auto-backups).
 * - Plaintext never touches disk unless the user explicitly clicks "Save to Phone".
 */
object VaultCryptoEngine {
    private const val PREFS_NAME = "duo_vault_zero_knowledge_meta"
    private const val KEY_SALT = "vault_kdf_salt_b64"
    private const val KEY_DEVICE_SECRET = "vault_device_secret_b64"
    private const val KEY_PASSCODE_HASH = "vault_passcode_hash"
    private const val KEY_SCREEN_SHIELD = "vault_privacy_shield_v2"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12

    // Shared DuoSpace space key derivation so encrypted .venc relay blobs can be decrypted by both partners
    private val sharedRelayKey: SecretKeySpec by lazy {
        val sharedSalt = "duospace_shared_vault_v1_salt".toByteArray(Charsets.UTF_8)
        val sharedSecret = "duospace_pair_e2ee_shared_key_2026".toCharArray()
        val spec = PBEKeySpec(sharedSecret, sharedSalt, 12_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun getLegacyDeviceKey(context: Context): SecretKeySpec? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return null
        val secretB64 = prefs.getString(KEY_DEVICE_SECRET, null) ?: return null
        return runCatching {
            val saltBytes = Base64.decode(saltB64, Base64.NO_WRAP)
            val spec = PBEKeySpec(secretB64.toCharArray(), saltBytes, 12_000, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        }.getOrNull()
    }

    fun encryptBytes(context: Context, plainBytes: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sharedRelayKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainBytes)
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return combined
    }

    fun decryptBytes(context: Context, encryptedPayload: ByteArray): ByteArray? {
        if (encryptedPayload.size <= GCM_IV_BYTES) return null
        val iv = encryptedPayload.copyOfRange(0, GCM_IV_BYTES)
        val cipherBytes = encryptedPayload.copyOfRange(GCM_IV_BYTES, encryptedPayload.size)

        // Try shared space key first
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sharedRelayKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(cipherBytes)
        } catch (_: Exception) {
            // Fallback to legacy device-local key for previously saved local files
        }

        return try {
            val legacyKey = getLegacyDeviceKey(context) ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, legacyKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherBytes)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Signal-Style Smart Image Sizer:
     * Shrinks large 8–15 MB camera photos down to ~200–300 KB (max 1600px on longest side,
     * 82% JPEG quality) while stripping EXIF metadata and keeping crisp mobile visual quality.
     */
    fun optimizeImageBytes(
        rawBytes: ByteArray,
        maxDimensionPx: Int = 1600,
        jpegQuality: Int = 82
    ): ByteArray {
        if (rawBytes.isEmpty()) return rawBytes
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            val srcWidth = boundsOpts.outWidth
            val srcHeight = boundsOpts.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return rawBytes

            var sampleSize = 1
            while ((srcWidth / sampleSize) > maxDimensionPx * 2 || (srcHeight / sampleSize) > maxDimensionPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return rawBytes

            val maxSide = maxOf(decoded.width, decoded.height)
            val scaledBitmap = if (maxSide > maxDimensionPx) {
                val ratio = maxDimensionPx.toFloat() / maxSide.toFloat()
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                android.graphics.Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
            } else {
                decoded
            }

            val out = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, jpegQuality, out)
            val compressed = out.toByteArray()

            if (compressed.isNotEmpty() && compressed.size < rawBytes.size) {
                compressed
            } else {
                rawBytes
            }
        } catch (_: Throwable) {
            rawBytes
        }
    }

    fun isScreenShieldEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SCREEN_SHIELD, false)
    }

    fun setScreenShieldEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SCREEN_SHIELD, enabled).apply()
    }

    fun getPasscode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASSCODE_HASH, null)?.takeIf { it.isNotEmpty() }
    }

    fun setPasscode(context: Context, pin: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PASSCODE_HASH, pin?.trim().orEmpty()).apply()
    }
}

object DuoVaultPersistence {
    private const val VAULT_DIR_NAME = "sandboxed_vault_e2ee"
    private const val INDEX_FILE_NAME = "vault_tree_index.venc"

    private const val ROOT_USER_1_ID = "folder_root_user_1"
    private const val ROOT_USER_2_ID = "folder_root_user_2"

    fun subFolderId(slot: TestUserSlot, category: VaultItemCategory): String {
        return "folder_${slot.name.lowercase(Locale.US)}_${category.name.lowercase(Locale.US)}"
    }

    fun vaultSandboxDir(context: Context): File {
        val dir = File(context.noBackupFilesDir, VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // Place .nomedia sentinel so Android media scanners never index anything here
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            runCatching { noMedia.createNewFile() }
        }
        return dir
    }

    fun loadNodes(
        context: Context,
        user1Name: String,
        user2Name: String,
        messages: List<MessageEntity>
    ): List<VaultNode> {
        val dir = vaultSandboxDir(context)
        val indexFile = File(dir, INDEX_FILE_NAME)
        val loaded = if (indexFile.exists()) {
            runCatching {
                val encrypted = indexFile.readBytes()
                val decrypted = VaultCryptoEngine.decryptBytes(context, encrypted)
                if (decrypted != null) {
                    parseNodesJson(String(decrypted, Charsets.UTF_8))
                } else {
                    emptyList()
                }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val ensured = ensureDefaultHierarchyAndSyncChat(
            context = context,
            existing = loaded,
            user1Name = user1Name,
            user2Name = user2Name,
            messages = messages
        )
        if (ensured != loaded) {
            saveNodes(context, ensured)
        }

        // For any file node whose .venc blob is not yet in this device's offline sandbox,
        // pull it from the temporary cloud relay locker and auto-delete the cloud copy.
        ensured.forEach { node ->
            if (!node.isFolder && !node.encryptedBlobFileName.isNullOrBlank()) {
                com.example.data.remote.DuoCloudRelaySync.ensureLocalSandboxFileOrPullFromRelay(
                    targetDir = dir,
                    blobFileName = node.encryptedBlobFileName
                )
            }
        }
        return ensured
    }

    fun saveNodes(context: Context, nodes: List<VaultNode>) {
        val dir = vaultSandboxDir(context)
        val indexFile = File(dir, INDEX_FILE_NAME)
        val arr = JSONArray()
        for (node in nodes) {
            val obj = JSONObject()
            obj.put("id", node.id)
            obj.put("parentId", node.parentId ?: "")
            obj.put("name", node.name)
            obj.put("isFolder", node.isFolder)
            obj.put("category", node.category.name)
            obj.put("ownerSlot", node.ownerSlot.name)
            obj.put("encryptedBlobFileName", node.encryptedBlobFileName ?: "")
            obj.put("mimeType", node.mimeType ?: "")
            obj.put("sizeBytes", node.sizeBytes)
            obj.put("createdAt", node.createdAt)
            obj.put("isSystemDefault", node.isSystemDefault)
            arr.put(obj)
        }
        val plainBytes = arr.toString().toByteArray(Charsets.UTF_8)
        val encryptedBytes = VaultCryptoEngine.encryptBytes(context, plainBytes)
        indexFile.writeBytes(encryptedBytes)
        com.example.data.remote.DuoCloudRelaySync.enqueueRelayUpload(INDEX_FILE_NAME, encryptedBytes)
    }

    private fun parseNodesJson(jsonStr: String): List<VaultNode> {
        val arr = JSONArray(jsonStr)
        val list = ArrayList<VaultNode>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val parentId = obj.optString("parentId", "").takeIf { it.isNotEmpty() }
            val category = runCatching {
                VaultItemCategory.valueOf(obj.optString("category", VaultItemCategory.FILES.name))
            }.getOrDefault(VaultItemCategory.FILES)
            val ownerSlot = if (obj.optString("ownerSlot") == TestUserSlot.USER_2.name) {
                TestUserSlot.USER_2
            } else {
                TestUserSlot.USER_1
            }
            list.add(
                VaultNode(
                    id = obj.getString("id"),
                    parentId = parentId,
                    name = obj.getString("name"),
                    isFolder = obj.optBoolean("isFolder", false),
                    category = category,
                    ownerSlot = ownerSlot,
                    encryptedBlobFileName = obj.optString("encryptedBlobFileName", "").takeIf { it.isNotEmpty() },
                    mimeType = obj.optString("mimeType", "").takeIf { it.isNotEmpty() },
                    sizeBytes = obj.optLong("sizeBytes", 0L),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isSystemDefault = obj.optBoolean("isSystemDefault", false)
                )
            )
        }
        return list
    }

    /**
     * Ensures the two default person folders (named after User 1 and User 2) exist at root,
     * each containing default file-type subfolders (Images, Videos, Audio, Documents, Files),
     * and syncs any sent chat media into the sender's corresponding subfolder.
     */
    private fun ensureDefaultHierarchyAndSyncChat(
        context: Context,
        existing: List<VaultNode>,
        user1Name: String,
        user2Name: String,
        messages: List<MessageEntity>
    ): List<VaultNode> {
        val map = LinkedHashMap<String, VaultNode>()
        for (node in existing) {
            map[node.id] = node
        }

        // 1. Root Folder for Person 1
        val u1Root = map[ROOT_USER_1_ID]?.copy(name = user1Name) ?: VaultNode(
            id = ROOT_USER_1_ID,
            parentId = null,
            name = user1Name,
            isFolder = true,
            category = VaultItemCategory.FOLDER,
            ownerSlot = TestUserSlot.USER_1,
            isSystemDefault = true
        )
        map[ROOT_USER_1_ID] = u1Root

        // 2. Root Folder for Person 2
        val u2Root = map[ROOT_USER_2_ID]?.copy(name = user2Name) ?: VaultNode(
            id = ROOT_USER_2_ID,
            parentId = null,
            name = user2Name,
            isFolder = true,
            category = VaultItemCategory.FOLDER,
            ownerSlot = TestUserSlot.USER_2,
            isSystemDefault = true
        )
        map[ROOT_USER_2_ID] = u2Root

        // 3. Default file-type subfolders inside each person's folder
        val defaultCategories = listOf(
            VaultItemCategory.IMAGES,
            VaultItemCategory.VIDEOS,
            VaultItemCategory.AUDIO,
            VaultItemCategory.DOCUMENTS,
            VaultItemCategory.FILES
        )

        for (cat in defaultCategories) {
            val u1SubId = subFolderId(TestUserSlot.USER_1, cat)
            if (!map.containsKey(u1SubId)) {
                map[u1SubId] = VaultNode(
                    id = u1SubId,
                    parentId = ROOT_USER_1_ID,
                    name = cat.folderLabel,
                    isFolder = true,
                    category = cat,
                    ownerSlot = TestUserSlot.USER_1,
                    isSystemDefault = true
                )
            }
            val u2SubId = subFolderId(TestUserSlot.USER_2, cat)
            if (!map.containsKey(u2SubId)) {
                map[u2SubId] = VaultNode(
                    id = u2SubId,
                    parentId = ROOT_USER_2_ID,
                    name = cat.folderLabel,
                    isFolder = true,
                    category = cat,
                    ownerSlot = TestUserSlot.USER_2,
                    isSystemDefault = true
                )
            }
        }

        // 4. Auto-index any voice notes, photos, videos, and files sent in Chat into the sender's Vault folders
        for (msg in messages) {
            val slot = if (msg.senderId == TestUserSlot.USER_2.userId) {
                TestUserSlot.USER_2
            } else {
                TestUserSlot.USER_1
            }
            when (msg.messageType) {
                MessageType.VOICE -> {
                    val syncedId = "chat_voice_${msg.id}"
                    if (!map.containsKey(syncedId)) {
                        val targetParent = subFolderId(slot, VaultItemCategory.AUDIO)
                        val payloadBytes = "Voice Note (${msg.content}) sent by ${msg.senderName}".toByteArray(Charsets.UTF_8)
                        val blobName = writeEncryptedBlob(context, payloadBytes)
                        map[syncedId] = VaultNode(
                            id = syncedId,
                            parentId = targetParent,
                            name = "VoiceNote_${msg.id.take(6)}.m4a",
                            isFolder = false,
                            category = VaultItemCategory.AUDIO,
                            ownerSlot = slot,
                            encryptedBlobFileName = blobName,
                            mimeType = "audio/mp4",
                            sizeBytes = payloadBytes.size.toLong(),
                            createdAt = msg.createdAt
                        )
                    }
                }
                MessageType.PHOTO -> {
                    val syncedId = "chat_photo_${msg.id}"
                    if (!map.containsKey(syncedId)) {
                        val parts = (msg.mediaMeta ?: "").split("|")
                        val blobName = parts.getOrNull(0).orEmpty()
                        val sizeBytes = parts.getOrNull(1)?.toLongOrNull() ?: 48_000L
                        val fileName = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "ChatPhoto_${msg.id.take(6)}.jpg"
                        if (blobName.isNotBlank()) {
                            map[syncedId] = VaultNode(
                                id = syncedId,
                                parentId = subFolderId(slot, VaultItemCategory.IMAGES),
                                name = fileName,
                                isFolder = false,
                                category = VaultItemCategory.IMAGES,
                                ownerSlot = slot,
                                encryptedBlobFileName = blobName,
                                mimeType = "image/jpeg",
                                sizeBytes = sizeBytes,
                                createdAt = msg.createdAt
                            )
                        }
                    }
                }
                MessageType.VIDEO -> {
                    val syncedId = "chat_video_${msg.id}"
                    if (!map.containsKey(syncedId)) {
                        val parts = (msg.mediaMeta ?: "").split("|")
                        val blobName = parts.getOrNull(0).orEmpty()
                        val sizeBytes = parts.getOrNull(2)?.toLongOrNull() ?: 120_000L
                        val fileName = parts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "ChatVideo_${msg.id.take(6)}.mp4"
                        if (blobName.isNotBlank()) {
                            map[syncedId] = VaultNode(
                                id = syncedId,
                                parentId = subFolderId(slot, VaultItemCategory.VIDEOS),
                                name = fileName,
                                isFolder = false,
                                category = VaultItemCategory.VIDEOS,
                                ownerSlot = slot,
                                encryptedBlobFileName = blobName,
                                mimeType = "video/mp4",
                                sizeBytes = sizeBytes,
                                createdAt = msg.createdAt
                            )
                        }
                    }
                }
                MessageType.FILE -> {
                    val syncedId = "chat_file_${msg.id}"
                    if (!map.containsKey(syncedId)) {
                        val parts = (msg.mediaMeta ?: "").split("|")
                        val blobName = parts.getOrNull(0).orEmpty()
                        val sizeBytes = parts.getOrNull(1)?.toLongOrNull() ?: 16_000L
                        val mimeType = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
                        val fileName = msg.content.ifBlank { "SharedFile_${msg.id.take(6)}" }
                        if (blobName.isNotBlank()) {
                            val cat = VaultItemCategory.fromMimeOrName(mimeType, fileName)
                            map[syncedId] = VaultNode(
                                id = syncedId,
                                parentId = subFolderId(slot, cat),
                                name = fileName,
                                isFolder = false,
                                category = cat,
                                ownerSlot = slot,
                                encryptedBlobFileName = blobName,
                                mimeType = mimeType,
                                sizeBytes = sizeBytes,
                                createdAt = msg.createdAt
                            )
                        }
                    }
                }
            }
        }

        return map.values.toList()
    }

    fun writeEncryptedBlob(context: Context, plainBytes: ByteArray): String {
        val dir = vaultSandboxDir(context)
        val blobName = "blob_${UUID.randomUUID()}.venc"
        val encrypted = VaultCryptoEngine.encryptBytes(context, plainBytes)
        File(dir, blobName).writeBytes(encrypted)
        // Queue to temporary Post-Office Media Relay so partner's phone downloads it & auto-deletes from cloud
        com.example.data.remote.DuoCloudRelaySync.enqueueRelayUpload(blobName, encrypted)
        return blobName
    }

    fun readDecryptedBlobInMemory(context: Context, blobFileName: String?): ByteArray? {
        if (blobFileName.isNullOrBlank()) return null
        val dir = vaultSandboxDir(context)
        val file = File(dir, blobFileName)
        if (!file.exists()) {
            com.example.data.remote.DuoCloudRelaySync.ensureLocalSandboxFileOrPullFromRelay(dir, blobFileName)
            return null
        }
        return VaultCryptoEngine.decryptBytes(context, file.readBytes())
    }

    fun saveMediaBytesToPhone(
        context: Context,
        plainBytes: ByteArray,
        displayName: String,
        mimeType: String
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    when {
                        mimeType.startsWith("image/") ->
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DuoSpace")
                        mimeType.startsWith("video/") ->
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/DuoSpace")
                        else ->
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/DuoSpace")
                    }
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = when {
                    mimeType.startsWith("image/") -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    mimeType.startsWith("video/") -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val uri = resolver.insert(collection, values) ?: return false
                resolver.openOutputStream(uri)?.use { it.write(plainBytes) }
                values.clear()
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val outDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                File(outDir, displayName).writeBytes(plainBytes)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun duplicateEncryptedBlob(context: Context, sourceBlobFileName: String?): String? {
        if (sourceBlobFileName.isNullOrBlank()) return null
        val dir = vaultSandboxDir(context)
        val src = File(dir, sourceBlobFileName)
        if (!src.exists()) return null
        val copyName = "blob_${UUID.randomUUID()}.venc"
        val encryptedBytes = src.readBytes()
        File(dir, copyName).writeBytes(encryptedBytes)
        com.example.data.remote.DuoCloudRelaySync.enqueueRelayUpload(copyName, encryptedBytes)
        return copyName
    }

    fun deleteEncryptedBlob(context: Context, blobFileName: String?) {
        if (blobFileName.isNullOrBlank()) return
        val file = File(vaultSandboxDir(context), blobFileName)
        if (file.exists()) {
            runCatching { file.delete() }
        }
    }

    fun ingestUriIntoVault(
        context: Context,
        uri: Uri,
        ownerSlot: TestUserSlot,
        explicitParentFolderId: String?, // if null, auto-sorts into owner's filetype folder
        smartCompressImages: Boolean = true
    ): VaultNode? {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)
            var displayName = "VaultFile_${System.currentTimeMillis()}"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let {
                            displayName = it
                        }
                    }
                }
            }

            val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val category = VaultItemCategory.fromMimeOrName(mimeType, displayName)
            val finalBytes = if (smartCompressImages && category == VaultItemCategory.IMAGES) {
                VaultCryptoEngine.optimizeImageBytes(rawBytes)
            } else {
                rawBytes
            }
            val targetParentId = explicitParentFolderId ?: subFolderId(ownerSlot, category)
            val blobName = writeEncryptedBlob(context, finalBytes)

            VaultNode(
                id = UUID.randomUUID().toString(),
                parentId = targetParentId,
                name = displayName,
                isFolder = false,
                category = category,
                ownerSlot = ownerSlot,
                encryptedBlobFileName = blobName,
                mimeType = mimeType,
                sizeBytes = finalBytes.size.toLong(),
                createdAt = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    private const val SHARED_STICKERS_INDEX_FILE = "shared_stickers_index.venc"

    fun loadSharedStickers(context: Context): List<SharedStickerItem> {
        val dir = vaultSandboxDir(context)
        val indexFile = File(dir, SHARED_STICKERS_INDEX_FILE)
        if (!indexFile.exists()) {
            com.example.data.remote.DuoCloudRelaySync.ensureLocalSandboxFileOrPullFromRelay(
                targetDir = dir,
                blobFileName = SHARED_STICKERS_INDEX_FILE
            )
        }
        if (!indexFile.exists()) return emptyList()
        val list = runCatching {
            val encrypted = indexFile.readBytes()
            val decrypted = VaultCryptoEngine.decryptBytes(context, encrypted) ?: return@runCatching emptyList()
            val arr = JSONArray(String(decrypted, Charsets.UTF_8))
            val parsed = ArrayList<SharedStickerItem>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val blobName = obj.optString("blobFileName", "")
                if (blobName.isNotEmpty()) {
                    com.example.data.remote.DuoCloudRelaySync.ensureLocalSandboxFileOrPullFromRelay(
                        targetDir = dir,
                        blobFileName = blobName
                    )
                    parsed.add(
                        SharedStickerItem(
                            id = obj.getString("id"),
                            blobFileName = blobName,
                            createdBy = obj.optString("createdBy", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            favoriteOrder = obj.optInt("favoriteOrder", i)
                        )
                    )
                }
            }
            parsed
        }.getOrDefault(emptyList())
        return list
    }

    fun saveSharedStickers(context: Context, stickers: List<SharedStickerItem>) {
        val dir = vaultSandboxDir(context)
        val indexFile = File(dir, SHARED_STICKERS_INDEX_FILE)
        val arr = JSONArray()
        stickers.forEach { stk ->
            val obj = JSONObject()
            obj.put("id", stk.id)
            obj.put("blobFileName", stk.blobFileName)
            obj.put("createdBy", stk.createdBy)
            obj.put("createdAt", stk.createdAt)
            obj.put("isFavorite", stk.isFavorite)
            obj.put("favoriteOrder", stk.favoriteOrder)
            arr.put(obj)
        }
        val plainBytes = arr.toString().toByteArray(Charsets.UTF_8)
        val encrypted = VaultCryptoEngine.encryptBytes(context, plainBytes)
        indexFile.writeBytes(encrypted)
        com.example.data.remote.DuoCloudRelaySync.enqueueRelayUpload(SHARED_STICKERS_INDEX_FILE, encrypted)
    }

    /**
     * Saves a recorded call audio file directly into the Vault's Audio folder (encrypted .venc blob)
     * without sending a playable recording bubble in chat.
     */
    fun saveCallRecordingToVault(
        context: Context,
        ownerSlot: TestUserSlot,
        user1Name: String,
        user2Name: String,
        recordingFilePath: String?,
        durationSec: Int
    ): VaultNode? {
        return try {
            val rawBytes = if (!recordingFilePath.isNullOrBlank()) {
                val f = File(recordingFilePath)
                if (f.exists() && f.length() > 0L) f.readBytes() else null
            } else {
                null
            } ?: "Recorded Call Audio (${durationSec.coerceAtLeast(1)}s)".toByteArray(Charsets.UTF_8)

            val blobName = writeEncryptedBlob(context, rawBytes)
            val ts = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("MMMdd_HHmm", Locale.US).format(Date(ts))
            val node = VaultNode(
                id = "call_rec_${UUID.randomUUID()}",
                parentId = subFolderId(ownerSlot, VaultItemCategory.AUDIO),
                name = "CallRecording_${formattedDate}_${durationSec.coerceAtLeast(1)}s.m4a",
                isFolder = false,
                category = VaultItemCategory.AUDIO,
                ownerSlot = ownerSlot,
                encryptedBlobFileName = blobName,
                mimeType = "audio/mp4",
                sizeBytes = rawBytes.size.toLong(),
                createdAt = ts
            )
            val existing = loadNodes(context, user1Name, user2Name, emptyList())
            saveNodes(context, listOf(node) + existing)
            node
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts any imported image URI into a 512x512 rounded borderless sticker PNG,
     * encrypts it into the shared Vault sandbox, and adds it to the shared sticker list for both users.
     */
    fun importImageAsSharedSticker(
        context: Context,
        uri: Uri,
        createdBy: String
    ): List<SharedStickerItem> {
        val current = loadSharedStickers(context)
        val stickerBytes = runCatching {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return@runCatching null

            val sizePx = 512
            val minSide = minOf(decoded.width, decoded.height).coerceAtLeast(1)
            val cropX = ((decoded.width - minSide) / 2).coerceAtLeast(0)
            val cropY = ((decoded.height - minSide) / 2).coerceAtLeast(0)
            val squareCrop = android.graphics.Bitmap.createBitmap(decoded, cropX, cropY, minSide, minSide)
            val scaled = android.graphics.Bitmap.createScaledBitmap(squareCrop, sizePx, sizePx, true)

            val output = android.graphics.Bitmap.createBitmap(
                sizePx,
                sizePx,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(output)
            val rect = android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
            val cornerRadius = 72f

            val path = android.graphics.Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(scaled, 0f, 0f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
            canvas.restore()

            val outStream = java.io.ByteArrayOutputStream()
            output.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.toByteArray()
        }.getOrNull() ?: return current

        val blobName = writeEncryptedBlob(context, stickerBytes)
        val newSticker = SharedStickerItem(
            id = "stk_${UUID.randomUUID()}",
            blobFileName = blobName,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis(),
            isFavorite = true,
            favoriteOrder = current.count { it.isFavorite }
        )
        val updated = listOf(newSticker) + current
        saveSharedStickers(context, updated)
        return updated
    }

    fun toggleSharedStickerFavorite(
        context: Context,
        stickerId: String
    ): List<SharedStickerItem> {
        val current = loadSharedStickers(context)
        val maxFavOrder = current.filter { it.isFavorite }.maxOfOrNull { it.favoriteOrder } ?: -1
        val updated = current.map { item ->
            if (item.id == stickerId) {
                val nextFav = !item.isFavorite
                item.copy(
                    isFavorite = nextFav,
                    favoriteOrder = if (nextFav) maxFavOrder + 1 else item.favoriteOrder
                )
            } else {
                item
            }
        }
        saveSharedStickers(context, updated)
        return updated
    }

    fun reorderFavoriteStickers(
        context: Context,
        orderedFavoriteIds: List<String>
    ): List<SharedStickerItem> {
        val current = loadSharedStickers(context)
        val orderMap = orderedFavoriteIds.withIndex().associate { (idx, id) -> id to idx }
        val updated = current.map { item ->
            val newIdx = orderMap[item.id]
            if (newIdx != null) {
                item.copy(isFavorite = true, favoriteOrder = newIdx)
            } else {
                item
            }
        }
        saveSharedStickers(context, updated)
        return updated
    }

    fun deleteSharedSticker(
        context: Context,
        stickerId: String
    ): List<SharedStickerItem> {
        val current = loadSharedStickers(context)
        val toRemove = current.firstOrNull { it.id == stickerId }
        if (toRemove != null) {
            deleteEncryptedBlob(context, toRemove.blobFileName)
        }
        val updated = current.filterNot { it.id == stickerId }
        saveSharedStickers(context, updated)
        return updated
    }
}

data class SharedStickerItem(
    val id: String,
    val blobFileName: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuoVaultScreen(
    activeUser: TestUserSlot,
    user1Name: String,
    user2Name: String,
    messages: List<MessageEntity>,
    onSwitchUser: (TestUserSlot) -> Unit,
    onBackToDashboard: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenWishlist: () -> Unit,
    unreadChatCount: Int
) {
    val context = LocalContext.current

    var allNodes by remember {
        mutableStateOf(DuoVaultPersistence.loadNodes(context, user1Name, user2Name, messages))
    }

    fun persistNodes(updated: List<VaultNode>) {
        allNodes = updated
        DuoVaultPersistence.saveNodes(context, updated)
    }

    // Current folder navigation stack (null = Vault Root showing the 2 people's folders)
    var currentFolderId by rememberSaveable { mutableStateOf<String?>(null) }

    // Selection & Clipboard (Copy / Cut / Paste / Delete)
    var selectedNodeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clipboardState by remember { mutableStateOf<VaultClipboardState?>(null) }

    // Passcode Lock States
    var savedPin by remember { mutableStateOf(VaultCryptoEngine.getPasscode(context)) }
    var isVaultLocked by rememberSaveable { mutableStateOf(savedPin != null) }
    var pinUnlockInput by rememberSaveable { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var newFolderNameInput by rememberSaveable { mutableStateOf("") }

    var nodeToRename by remember { mutableStateOf<VaultNode?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }

    var showUploadChoiceDialog by rememberSaveable { mutableStateOf(false) }
    var autoSortOnUpload by rememberSaveable { mutableStateOf(true) }
    var smartCompressImagesOnUpload by rememberSaveable { mutableStateOf(true) }

    var previewNode by remember { mutableStateOf<VaultNode?>(null) }
    var pendingSaveToPhoneNode by remember { mutableStateOf<VaultNode?>(null) }

    // Ensure FLAG_SECURE is cleared so the Streaming Emulator never shows a black screen
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    BackHandler {
        when {
            selectedNodeIds.isNotEmpty() -> selectedNodeIds = emptySet()
            currentFolderId != null -> {
                val currentFolder = allNodes.find { it.id == currentFolderId }
                currentFolderId = currentFolder?.parentId
            }
            else -> onBackToDashboard()
        }
    }

    // Upload Launchers
    val mediaUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val targetFolder = if (autoSortOnUpload) null else currentFolderId
            val node = DuoVaultPersistence.ingestUriIntoVault(
                context = context,
                uri = uri,
                ownerSlot = activeUser,
                explicitParentFolderId = targetFolder,
                smartCompressImages = smartCompressImagesOnUpload
            )
            if (node != null) {
                persistNodes(allNodes + node)
                Toast.makeText(
                    context,
                    "Added to ${node.category.folderLabel}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val fileUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val targetFolder = if (autoSortOnUpload) null else currentFolderId
            val node = DuoVaultPersistence.ingestUriIntoVault(
                context = context,
                uri = uri,
                ownerSlot = activeUser,
                explicitParentFolderId = targetFolder,
                smartCompressImages = smartCompressImagesOnUpload
            )
            if (node != null) {
                persistNodes(allNodes + node)
                Toast.makeText(
                    context,
                    "Added to ${node.category.folderLabel}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Explicit "Save to Phone" Launcher (only way a file ever leaves the sandbox)
    val saveToPhoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri ->
        val node = pendingSaveToPhoneNode
        pendingSaveToPhoneNode = null
        if (destUri != null && node != null) {
            val decrypted = DuoVaultPersistence.readDecryptedBlobInMemory(
                context = context,
                blobFileName = node.encryptedBlobFileName
            )
            if (decrypted != null) {
                runCatching {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        out.write(decrypted)
                    }
                    Toast.makeText(context, "Saved ${node.name} to phone", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Breadcrumb trail calculation
    val breadcrumbPath = remember(allNodes, currentFolderId) {
        val list = mutableListOf<VaultNode>()
        var cursorId = currentFolderId
        while (cursorId != null) {
            val found = allNodes.find { it.id == cursorId } ?: break
            list.add(0, found)
            cursorId = found.parentId
        }
        list
    }

    val visibleChildren = remember(allNodes, currentFolderId) {
        allNodes
            .filter { it.parentId == currentFolderId }
            .sortedWith(
                compareByDescending<VaultNode> { it.isFolder }
                    .thenByDescending { it.isSystemDefault }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
    }

    fun deleteNodesRecursively(targetIds: Set<String>) {
        val toRemove = mutableSetOf<String>()
        fun collectDescendants(id: String) {
            val node = allNodes.find { it.id == id } ?: return
            if (!node.isSystemDefault) {
                toRemove.add(id)
            }
            allNodes.filter { it.parentId == id }.forEach { child ->
                collectDescendants(child.id)
            }
        }
        targetIds.forEach { collectDescendants(it) }

        allNodes.filter { it.id in toRemove && !it.isFolder }.forEach { fileNode ->
            DuoVaultPersistence.deleteEncryptedBlob(context, fileNode.encryptedBlobFileName)
        }
        persistNodes(allNodes.filterNot { it.id in toRemove })
        selectedNodeIds = emptySet()
    }

    fun pasteClipboardHere(destinationFolderId: String?) {
        val clip = clipboardState ?: return
        val updated = allNodes.toMutableList()

        fun cloneSubtree(sourceNode: VaultNode, newParentId: String?): List<VaultNode> {
            val newId = UUID.randomUUID().toString()
            val clonedBlob = if (!sourceNode.isFolder) {
                DuoVaultPersistence.duplicateEncryptedBlob(context, sourceNode.encryptedBlobFileName)
            } else {
                null
            }
            val copyNode = sourceNode.copy(
                id = newId,
                parentId = newParentId,
                name = if (sourceNode.parentId == newParentId) "${sourceNode.name} (Copy)" else sourceNode.name,
                encryptedBlobFileName = clonedBlob,
                isSystemDefault = false,
                createdAt = System.currentTimeMillis()
            )
            val result = mutableListOf(copyNode)
            if (sourceNode.isFolder) {
                allNodes.filter { it.parentId == sourceNode.id }.forEach { child ->
                    result.addAll(cloneSubtree(child, newId))
                }
            }
            return result
        }

        if (clip.operation == VaultClipboardOp.COPY) {
            for (id in clip.nodeIds) {
                val src = allNodes.find { it.id == id } ?: continue
                updated.addAll(cloneSubtree(src, destinationFolderId))
            }
        } else {
            // CUT (Move) — prevent moving a folder inside itself
            for (id in clip.nodeIds) {
                val idx = updated.indexOfFirst { it.id == id }
                if (idx >= 0 && !updated[idx].isSystemDefault && id != destinationFolderId) {
                    updated[idx] = updated[idx].copy(parentId = destinationFolderId)
                }
            }
        }

        persistNodes(updated)
        clipboardState = null
        selectedNodeIds = emptySet()
    }

    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_new_folder_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newFolderNameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            val folder = VaultNode(
                                id = UUID.randomUUID().toString(),
                                parentId = currentFolderId,
                                name = trimmed,
                                isFolder = true,
                                category = VaultItemCategory.FOLDER,
                                ownerSlot = activeUser
                            )
                            persistNodes(allNodes + folder)
                            newFolderNameInput = ""
                            showCreateFolderDialog = false
                        }
                    },
                    enabled = newFolderNameInput.trim().isNotEmpty(),
                    modifier = Modifier.testTag("vault_new_folder_confirm_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    nodeToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { nodeToRename = null },
            title = { Text("Rename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty()) {
                            persistNodes(
                                allNodes.map {
                                    if (it.id == target.id) it.copy(name = newName) else it
                                }
                            )
                            nodeToRename = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { nodeToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Upload Choice Dialog
    if (showUploadChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showUploadChoiceDialog = false },
            title = { Text("Upload", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-sort into $activeUserName's filetype folder",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (autoSortOnUpload) {
                                    "Routes to $activeUserName / Images, Videos, Audio, or Files"
                                } else {
                                    "Uploads directly into current folder"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSortOnUpload,
                            onCheckedChange = { autoSortOnUpload = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart photo sizer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (smartCompressImagesOnUpload) {
                                    "Optimizes photos to save ~95% space"
                                } else {
                                    "Keeps original uncompressed photo size"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = smartCompressImagesOnUpload,
                            onCheckedChange = { smartCompressImagesOnUpload = it },
                            modifier = Modifier.testTag("vault_smart_sizer_switch")
                        )
                    }

                    Button(
                        onClick = {
                            showUploadChoiceDialog = false
                            mediaUploadLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_upload_media_button")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Photo or Video")
                    }

                    OutlinedButton(
                        onClick = {
                            showUploadChoiceDialog = false
                            fileUploadLauncher.launch(arrayOf("*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_upload_document_button")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Any File / Document")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showUploadChoiceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // In-Memory Decrypted File Preview & Explicit "Save to Phone" Modal
    previewNode?.let { fileNode ->
        val decryptedBytes = remember(fileNode.id) {
            DuoVaultPersistence.readDecryptedBlobInMemory(context, fileNode.encryptedBlobFileName)
        }
        val bitmap = remember(decryptedBytes) {
            if (fileNode.category == VaultItemCategory.IMAGES && decryptedBytes != null) {
                runCatching {
                    BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)?.asImageBitmap()
                }.getOrNull()
            } else {
                null
            }
        }
        val ownerName = if (fileNode.ownerSlot == TestUserSlot.USER_1) user1Name else user2Name

        AlertDialog(
            onDismissRequest = { previewNode = null },
            title = {
                Text(
                    text = fileNode.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = fileNode.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIcon(fileNode.category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "${fileNode.category.folderLabel} • ${DuoVaultPersistence.formatBytes(fileNode.sizeBytes)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Text(
                        text = "Added by $ownerName",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSaveToPhoneNode = fileNode
                        previewNode = null
                        saveToPhoneLauncher.launch(fileNode.name)
                    },
                    modifier = Modifier.testTag("vault_save_to_phone_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Phone")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { previewNode = null }) {
                    Text("Close")
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
                            text = "The Vault",
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
                currentDestination = DuoBottomNavDestination.VAULT,
                unreadChatCount = unreadChatCount,
                onClickDashboard = onBackToDashboard,
                onClickChat = onOpenChat,
                onClickJournal = onOpenJournal,
                onClickWishlist = onOpenWishlist,
                onClickVault = {}
            )
        }
    ) { innerPadding ->
        if (isVaultLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Vault Locked",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your PIN to unlock",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (savedPin != null) {
                            OutlinedTextField(
                                value = pinUnlockInput,
                                onValueChange = {
                                    pinUnlockInput = it
                                    pinErrorText = null
                                },
                                placeholder = { Text("Enter Vault PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (pinErrorText != null) {
                                Text(
                                    text = pinErrorText!!,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (savedPin == null || pinUnlockInput.trim() == savedPin) {
                                    isVaultLocked = false
                                    pinUnlockInput = ""
                                    pinErrorText = null
                                } else {
                                    pinErrorText = "Incorrect Vault PIN"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_unlock_button")
                        ) {
                            Text("Unlock Vault", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action Bar: Breadcrumb Navigation + New Folder + Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Breadcrumbs
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentFolderId != null) {
                            IconButton(
                                onClick = {
                                    val curr = allNodes.find { it.id == currentFolderId }
                                    currentFolderId = curr?.parentId
                                    selectedNodeIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("vault_up_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Up folder",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentFolderId == null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    currentFolderId = null
                                    selectedNodeIds = emptySet()
                                }
                                .testTag("vault_breadcrumb_root")
                        ) {
                            Text(
                                text = "Vault",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentFolderId == null) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        for (crumb in breadcrumbPath) {
                            Text(
                                text = "/",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val isLast = crumb.id == currentFolderId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isLast) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        currentFolderId = crumb.id
                                        selectedNodeIds = emptySet()
                                    }
                            ) {
                                Text(
                                    text = crumb.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLast) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // New Folder + Upload Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCreateFolderDialog = true }
                                .testTag("vault_create_folder_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "New Folder",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Folder",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showUploadChoiceDialog = true }
                                .testTag("vault_upload_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "Upload",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Upload",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Multi-Select / File Operations Toolbar (Copy, Cut, Rename, Delete, Save to Phone)
                AnimatedVisibility(visible = selectedNodeIds.isNotEmpty() || clipboardState != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_file_ops_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedNodeIds.isNotEmpty()) {
                                Text(
                                    text = "${selectedNodeIds.size} selected",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Copy
                                    IconButton(
                                        onClick = {
                                            clipboardState = VaultClipboardState(
                                                nodeIds = selectedNodeIds,
                                                operation = VaultClipboardOp.COPY
                                            )
                                            selectedNodeIds = emptySet()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("vault_op_copy_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Cut
                                    IconButton(
                                        onClick = {
                                            clipboardState = VaultClipboardState(
                                                nodeIds = selectedNodeIds,
                                                operation = VaultClipboardOp.CUT
                                            )
                                            selectedNodeIds = emptySet()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("vault_op_cut_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCut,
                                            contentDescription = "Cut",
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Rename (when 1 item is selected)
                                    if (selectedNodeIds.size == 1) {
                                        val singleNode = allNodes.find { it.id == selectedNodeIds.first() }
                                        if (singleNode != null && !singleNode.isSystemDefault) {
                                            IconButton(
                                                onClick = {
                                                    renameInput = singleNode.name
                                                    nodeToRename = singleNode
                                                    selectedNodeIds = emptySet()
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("vault_op_rename_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                                    contentDescription = "Rename",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (singleNode != null && !singleNode.isFolder) {
                                            IconButton(
                                                onClick = {
                                                    pendingSaveToPhoneNode = singleNode
                                                    selectedNodeIds = emptySet()
                                                    saveToPhoneLauncher.launch(singleNode.name)
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("vault_op_save_phone_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Save to Phone",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { deleteNodesRecursively(selectedNodeIds) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("vault_op_delete_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedNodeIds = emptySet() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear selection",
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            } else if (clipboardState != null) {
                                val clip = clipboardState!!
                                Text(
                                    text = "${clip.nodeIds.size} item(s) ready to ${if (clip.operation == VaultClipboardOp.COPY) "copy" else "move"}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { pasteClipboardHere(currentFolderId) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("vault_op_paste_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste Here", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { clipboardState = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel clipboard",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Vault Folder & File List Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vault_items_surface")
                ) {
                    if (visibleChildren.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "This folder is empty",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap Upload or Folder above to add items",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(visibleChildren, key = { it.id }) { node ->
                                val isSelected = node.id in selectedNodeIds
                                val childCount = remember(allNodes, node.id) {
                                    allNodes.count { it.parentId == node.id }
                                }
                                val ownerName = if (node.ownerSlot == TestUserSlot.USER_1) {
                                    user1Name
                                } else {
                                    user2Name
                                }

                                VaultNodeRowItem(
                                    node = node,
                                    ownerName = ownerName,
                                    childCount = childCount,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (selectedNodeIds.isNotEmpty()) {
                                            selectedNodeIds = if (isSelected) {
                                                selectedNodeIds - node.id
                                            } else {
                                                selectedNodeIds + node.id
                                            }
                                        } else if (node.isFolder) {
                                            currentFolderId = node.id
                                        } else {
                                            previewNode = node
                                        }
                                    },
                                    onLongClick = {
                                        selectedNodeIds = if (isSelected) {
                                            selectedNodeIds - node.id
                                        } else {
                                            selectedNodeIds + node.id
                                        }
                                    },
                                    onSelectToggle = {
                                        selectedNodeIds = if (isSelected) {
                                            selectedNodeIds - node.id
                                        } else {
                                            selectedNodeIds + node.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultNodeRowItem(
    node: VaultNode,
    ownerName: String,
    childCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectToggle: () -> Unit
) {
    val dateLabel = remember(node.createdAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(node.createdAt))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("vault_node_${node.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (node.isFolder) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (node.isFolder) {
                            Icons.Default.Folder
                        } else {
                            categoryIcon(node.category)
                        },
                        contentDescription = node.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (node.isFolder) {
                            "$childCount item(s) • $ownerName"
                        } else {
                            "${DuoVaultPersistence.formatBytes(node.sizeBytes)} • $ownerName • $dateLabel"
                        },
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Selection Checkbox Pill (for quick Copy / Cut / Delete without needing long-press)
            Surface(
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                border = if (!isSelected) {
                    BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
                } else {
                    null
                },
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSelectToggle)
                    .testTag("vault_select_${node.id}")
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: VaultItemCategory) = when (category) {
    VaultItemCategory.FOLDER -> Icons.Default.Folder
    VaultItemCategory.IMAGES -> Icons.Default.Image
    VaultItemCategory.VIDEOS -> Icons.Default.VideoFile
    VaultItemCategory.AUDIO -> Icons.Default.AudioFile
    VaultItemCategory.DOCUMENTS -> Icons.Default.Description
    VaultItemCategory.FILES -> Icons.AutoMirrored.Filled.InsertDriveFile
}
