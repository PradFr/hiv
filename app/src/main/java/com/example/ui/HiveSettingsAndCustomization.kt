package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

enum class HiveThemePreset(
    val id: String,
    val label: String,
    val primaryDarkHex: Long,
    val secondaryDarkHex: Long,
    val bgDarkHex: Long,
    val surfaceDarkHex: Long,
    val primaryLightHex: Long,
    val bgLightHex: Long,
    val surfaceLightHex: Long
) {
    CLASSIC_INK(
        id = "classic_ink",
        label = "Minimal Ink",
        primaryDarkHex = 0xFFF4F4F5L,
        secondaryDarkHex = 0xFFA1A1AAL,
        bgDarkHex = 0xFF09090BL,
        surfaceDarkHex = 0xFF121215L,
        primaryLightHex = 0xFF18181BL,
        bgLightHex = 0xFFFAFAF9L,
        surfaceLightHex = 0xFFFFFFFFL
    ),
    OBSIDIAN_HONEY(
        id = "obsidian_honey",
        label = "Hive Honey",
        primaryDarkHex = 0xFFF59E0BL,
        secondaryDarkHex = 0xFFFBBF24L,
        bgDarkHex = 0xFF0B0B0EL,
        surfaceDarkHex = 0xFF16161DL,
        primaryLightHex = 0xFFD97706L,
        bgLightHex = 0xFFFFFBF2L,
        surfaceLightHex = 0xFFFFFFFFL
    ),
    MIDNIGHT_ROSE(
        id = "midnight_rose",
        label = "Midnight Rose",
        primaryDarkHex = 0xFFF43F5EL,
        secondaryDarkHex = 0xFFFB7185L,
        bgDarkHex = 0xFF110A10L,
        surfaceDarkHex = 0xFF1D121BL,
        primaryLightHex = 0xFFE11D48L,
        bgLightHex = 0xFFFFF5F7L,
        surfaceLightHex = 0xFFFFFFFFL
    ),
    EMERALD_FOREST(
        id = "emerald_forest",
        label = "Emerald Woods",
        primaryDarkHex = 0xFF10B981L,
        secondaryDarkHex = 0xFF34D399L,
        bgDarkHex = 0xFF07120EL,
        surfaceDarkHex = 0xFF10201AL,
        primaryLightHex = 0xFF059669L,
        bgLightHex = 0xFFF2FBF7L,
        surfaceLightHex = 0xFFFFFFFFL
    ),
    CYBER_VIOLET(
        id = "cyber_violet",
        label = "Cyber Violet",
        primaryDarkHex = 0xFF8B5CF6L,
        secondaryDarkHex = 0xFFA78BFAL,
        bgDarkHex = 0xFF0B0914L,
        surfaceDarkHex = 0xFF161326L,
        primaryLightHex = 0xFF6D28D9L,
        bgLightHex = 0xFFF7F5FFL,
        surfaceLightHex = 0xFFFFFFFFL
    ),
    OCEAN_SLATE(
        id = "ocean_slate",
        label = "Nordic Ocean",
        primaryDarkHex = 0xFF38BDF8L,
        secondaryDarkHex = 0xFF7DD3FCL,
        bgDarkHex = 0xFF081018L,
        surfaceDarkHex = 0xFF111D2BL,
        primaryLightHex = 0xFF0284C7L,
        bgLightHex = 0xFFF2F9FFL,
        surfaceLightHex = 0xFFFFFFFFL
    );

    companion object {
        fun fromId(id: String?): HiveThemePreset =
            entries.firstOrNull { it.id == id } ?: CLASSIC_INK
    }
}

enum class HiveThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromId(id: String?): HiveThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

data class SavedCouponItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeType: String = "COUPON",
    val senderName: String = "",
    val fromUserName: String = senderName,
    val messageId: String = "",
    val recipientSlot: TestUserSlot = TestUserSlot.USER_1
)

data class HiveAppConfigState(
    val isSpacePaired: Boolean = false,
    val activeSpaceCode: String = "HIVE01",
    val localUserSlot: TestUserSlot = TestUserSlot.USER_1,
    val themePreset: HiveThemePreset = HiveThemePreset.CLASSIC_INK,
    val themeMode: HiveThemeMode = HiveThemeMode.SYSTEM,
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "1234",
    val user1CustomName: String = "",
    val user2CustomName: String = "",
    val user1AvatarBlob: String = "",
    val user2AvatarBlob: String = "",
    val user1LocationConsent: Boolean = false,
    val user2LocationConsent: Boolean = false,
    val user1HearConsent: Boolean = false,
    val user2HearConsent: Boolean = false,
    val user1Lat: Double = 37.7858,
    val user1Lng: Double = -122.4065,
    val user2Lat: Double = 37.7865,
    val user2Lng: Double = -122.4052,
    val user1BatteryPct: Int = 88,
    val user2BatteryPct: Int = 84
) {
    val user1Name: String get() = user1CustomName
    val user2Name: String get() = user2CustomName
    val user1BatteryPercent: Int get() = user1BatteryPct
    val user2BatteryPercent: Int get() = user2BatteryPct
}

object HiveSettingsPersistence {
    private const val PREFS_NAME = "hive_app_settings_prefs_v1"
    private const val COUPON_PREFS_NAME = "duospace_user_coupons_prefs"
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    var changeCounter by mutableLongStateOf(0L)
        private set

    fun notifyUpdated() {
        changeCounter += 1L
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureInitialized(context: Context) {
        prefs(context)
    }

    fun generateSpaceInviteCode(): String {
        return buildString(6) {
            repeat(6) {
                append(CODE_CHARS.random())
            }
        }
    }

    fun refreshLiveDeviceBattery(context: Context, activeUser: TestUserSlot) {
        val liveBattery = readDeviceBatteryPercentage(context)
        val editor = prefs(context).edit()
        if (activeUser == TestUserSlot.USER_1) {
            editor.putInt("user1_battery_pct", liveBattery)
        } else {
            editor.putInt("user2_battery_pct", liveBattery)
        }
        editor.apply()
    }

    fun readDeviceBatteryPercentage(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (level in 1..100) level else 86
        } catch (_: Exception) {
            86
        }
    }

    fun loadConfig(context: Context): HiveAppConfigState {
        val p = prefs(context)
        val liveBattery = readDeviceBatteryPercentage(context)
        val slotStr = p.getString("local_user_slot", TestUserSlot.USER_1.name)
        val slot = if (slotStr == TestUserSlot.USER_2.name) TestUserSlot.USER_2 else TestUserSlot.USER_1
        return HiveAppConfigState(
            isSpacePaired = p.getBoolean("is_space_paired", false),
            activeSpaceCode = p.getString("active_space_code", "HIVE01")?.trim()?.uppercase()?.ifBlank { "HIVE01" } ?: "HIVE01",
            localUserSlot = slot,
            themePreset = HiveThemePreset.fromId(p.getString("theme_preset", HiveThemePreset.CLASSIC_INK.id)),
            themeMode = HiveThemeMode.fromId(p.getString("theme_mode", HiveThemeMode.SYSTEM.id)),
            pinLockEnabled = p.getBoolean("pin_lock_enabled", false),
            pinCode = p.getString("pin_code", "1234")?.ifBlank { "1234" } ?: "1234",
            user1CustomName = p.getString("user1_custom_name", "").orEmpty(),
            user2CustomName = p.getString("user2_custom_name", "").orEmpty(),
            user1AvatarBlob = p.getString("user1_avatar_blob", "").orEmpty(),
            user2AvatarBlob = p.getString("user2_avatar_blob", "").orEmpty(),
            user1LocationConsent = p.getBoolean("user1_location_consent", false),
            user2LocationConsent = p.getBoolean("user2_location_consent", false),
            user1HearConsent = p.getBoolean("user1_hear_consent", false),
            user2HearConsent = p.getBoolean("user2_hear_consent", false),
            user1Lat = p.getString("user1_lat", "37.7858")?.toDoubleOrNull() ?: 37.7858,
            user1Lng = p.getString("user1_lng", "-122.4065")?.toDoubleOrNull() ?: -122.4065,
            user2Lat = p.getString("user2_lat", "37.7865")?.toDoubleOrNull() ?: 37.7865,
            user2Lng = p.getString("user2_lng", "-122.4052")?.toDoubleOrNull() ?: -122.4052,
            user1BatteryPct = p.getInt("user1_battery_pct", liveBattery),
            user2BatteryPct = p.getInt("user2_battery_pct", liveBattery)
        )
    }

    fun completeSpacePairing(
        context: Context,
        spaceCode: String,
        slot: TestUserSlot,
        displayName: String,
        avatarBlobName: String? = null
    ) {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { generateSpaceInviteCode() }
        val cleanName = displayName.trim().ifBlank { "You" }
        val liveBattery = readDeviceBatteryPercentage(context)
        val editor = prefs(context).edit()
            .putBoolean("is_space_paired", true)
            .putString("active_space_code", cleanCode)
            .putString("local_user_slot", slot.name)
        if (slot == TestUserSlot.USER_1) {
            editor.putString("user1_custom_name", cleanName)
            if (!avatarBlobName.isNullOrBlank()) {
                editor.putString("user1_avatar_blob", avatarBlobName)
            }
            editor.putInt("user1_battery_pct", liveBattery)
        } else {
            editor.putString("user2_custom_name", cleanName)
            if (!avatarBlobName.isNullOrBlank()) {
                editor.putString("user2_avatar_blob", avatarBlobName)
            }
            editor.putInt("user2_battery_pct", liveBattery)
        }
        editor.apply()
        notifyUpdated()
    }

    fun setLocalUserSlot(context: Context, slot: TestUserSlot) {
        prefs(context).edit()
            .putString("local_user_slot", slot.name)
            .apply()
        notifyUpdated()
    }

    fun leaveOrResetSpace(context: Context) {
        prefs(context).edit()
            .putBoolean("is_space_paired", false)
            .apply()
        notifyUpdated()
    }

    fun updateTheme(context: Context, preset: HiveThemePreset, mode: HiveThemeMode) {
        prefs(context).edit()
            .putString("theme_preset", preset.id)
            .putString("theme_mode", mode.id)
            .apply()
        notifyUpdated()
    }

    fun updatePinSettings(context: Context, enabled: Boolean, pinCode: String = "1234") {
        val cleanPin = pinCode.filter { it.isDigit() }.take(6).ifEmpty { "1234" }
        prefs(context).edit()
            .putBoolean("pin_lock_enabled", enabled)
            .putString("pin_code", cleanPin)
            .apply()
        notifyUpdated()
    }

    fun updateUserProfile(
        context: Context,
        slot: TestUserSlot,
        displayName: String,
        avatarBlobName: String? = null
    ) {
        val editor = prefs(context).edit()
        val liveBattery = readDeviceBatteryPercentage(context)
        if (slot == TestUserSlot.USER_1) {
            editor.putString("user1_custom_name", displayName.trim())
            if (avatarBlobName != null) {
                editor.putString("user1_avatar_blob", avatarBlobName)
            }
            editor.putInt("user1_battery_pct", liveBattery)
        } else {
            editor.putString("user2_custom_name", displayName.trim())
            if (avatarBlobName != null) {
                editor.putString("user2_avatar_blob", avatarBlobName)
            }
            editor.putInt("user2_battery_pct", liveBattery)
        }
        editor.apply()
        notifyUpdated()
    }

    fun updateLocationConsent(
        context: Context,
        slot: TestUserSlot,
        enabled: Boolean,
        lat: Double? = null,
        lng: Double? = null
    ) {
        val editor = prefs(context).edit()
        if (slot == TestUserSlot.USER_1) {
            editor.putBoolean("user1_location_consent", enabled)
            if (lat != null && lng != null) {
                editor.putString("user1_lat", lat.toString())
                editor.putString("user1_lng", lng.toString())
            }
        } else {
            editor.putBoolean("user2_location_consent", enabled)
            if (lat != null && lng != null) {
                editor.putString("user2_lat", lat.toString())
                editor.putString("user2_lng", lng.toString())
            }
        }
        editor.apply()
        notifyUpdated()
    }

    fun updateHearConsent(
        context: Context,
        slot: TestUserSlot,
        enabled: Boolean
    ) {
        val editor = prefs(context).edit()
        if (slot == TestUserSlot.USER_1) {
            editor.putBoolean("user1_hear_consent", enabled)
        } else {
            editor.putBoolean("user2_hear_consent", enabled)
        }
        editor.apply()
        notifyUpdated()
    }

    // --- Soundboard Persistence (Shared vs Personal, max 5 seconds) ---

    fun loadSoundboardClips(context: Context, activeUser: TestUserSlot): List<SoundboardClip> {
        val raw = prefs(context).getString("soundboard_clips_json_v1", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val result = ArrayList<SoundboardClip>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ownerId = obj.optString("ownerUserId", TestUserSlot.USER_1.userId)
                val isShared = obj.optBoolean("isShared", true)
                if (isShared || ownerId == activeUser.userId) {
                    result.add(
                        SoundboardClip(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            filePath = obj.getString("filePath"),
                            accentHex = obj.optLong("accentHex", 0xFF5865F2L),
                            ownerUserId = ownerId,
                            isShared = isShared,
                            durationSec = obj.optInt("durationSec", 3)
                        )
                    )
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun loadAllSoundboardClipsRaw(context: Context): List<SoundboardClip> {
        val raw = prefs(context).getString("soundboard_clips_json_v1", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val result = ArrayList<SoundboardClip>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    SoundboardClip(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        filePath = obj.getString("filePath"),
                        accentHex = obj.optLong("accentHex", 0xFF5865F2L),
                        ownerUserId = obj.optString("ownerUserId", TestUserSlot.USER_1.userId),
                        isShared = obj.optBoolean("isShared", true),
                        durationSec = obj.optInt("durationSec", 3)
                    )
                )
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun saveAllSoundboardClipsRaw(context: Context, clips: List<SoundboardClip>) {
        val arr = JSONArray()
        clips.forEach { clip ->
            val obj = JSONObject()
            obj.put("id", clip.id)
            obj.put("title", clip.title)
            obj.put("filePath", clip.filePath)
            obj.put("accentHex", clip.accentHex)
            obj.put("ownerUserId", clip.ownerUserId)
            obj.put("isShared", clip.isShared)
            obj.put("durationSec", clip.durationSec)
            arr.put(obj)
        }
        prefs(context).edit().putString("soundboard_clips_json_v1", arr.toString()).apply()
        notifyUpdated()
    }

    fun addSoundboardClip(context: Context, clip: SoundboardClip) {
        val all = loadAllSoundboardClipsRaw(context)
        saveAllSoundboardClipsRaw(context, listOf(clip) + all)
    }

    fun deleteSoundboardClip(
        context: Context,
        clipId: String,
        activeUser: TestUserSlot = TestUserSlot.USER_1
    ): List<SoundboardClip> {
        val all = loadAllSoundboardClipsRaw(context)
        val target = all.firstOrNull { it.id == clipId }
        if (target != null) {
            runCatching { File(target.filePath).delete() }
        }
        saveAllSoundboardClipsRaw(context, all.filterNot { it.id == clipId })
        return loadSoundboardClips(context, activeUser)
    }

    // --- Received Coupons Persistence (Per-Recipient Saved Coupons, Removed on Redeem) ---

    fun loadReceivedCoupons(context: Context, activeUser: TestUserSlot): List<SavedCouponItem> {
        val cp = context.getSharedPreferences(COUPON_PREFS_NAME, Context.MODE_PRIVATE)
        val key = "received_coupons_${activeUser.userId}"
        val raw = cp.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = ArrayList<SavedCouponItem>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sender = obj.optString("senderName", obj.optString("fromUserName", ""))
                list.add(
                    SavedCouponItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subtitle = obj.optString("subtitle", ""),
                        badgeType = obj.optString("badgeType", "COUPON"),
                        senderName = sender,
                        fromUserName = sender,
                        messageId = obj.optString("messageId", ""),
                        recipientSlot = activeUser
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun saveReceivedCouponsList(
        context: Context,
        activeUser: TestUserSlot,
        items: List<SavedCouponItem>
    ) {
        val cp = context.getSharedPreferences(COUPON_PREFS_NAME, Context.MODE_PRIVATE)
        val key = "received_coupons_${activeUser.userId}"
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("subtitle", item.subtitle)
            obj.put("badgeType", item.badgeType)
            obj.put("senderName", item.fromUserName.ifBlank { item.senderName })
            obj.put("fromUserName", item.fromUserName.ifBlank { item.senderName })
            obj.put("messageId", item.messageId)
            arr.put(obj)
        }
        cp.edit().putString(key, arr.toString()).apply()
        notifyUpdated()
    }

    fun isCouponSavedByRecipient(
        context: Context,
        activeUser: TestUserSlot,
        messageId: String,
        title: String
    ): Boolean {
        return loadReceivedCoupons(context, activeUser).any {
            (messageId.isNotBlank() && it.messageId == messageId) || it.title.equals(title, ignoreCase = true)
        }
    }

    fun saveCouponToReceived(
        context: Context,
        coupon: SavedCouponItem
    ) {
        val activeUser = coupon.recipientSlot
        val current = loadReceivedCoupons(context, activeUser)
        if (current.any {
                (coupon.messageId.isNotBlank() && it.messageId == coupon.messageId) ||
                    it.title.equals(coupon.title, ignoreCase = true)
            }
        ) {
            return
        }
        saveReceivedCouponsList(context, activeUser, listOf(coupon) + current)
    }

    fun removeReceivedCouponOnRedeem(
        context: Context,
        messageId: String,
        title: String,
        recipientSlot: TestUserSlot = TestUserSlot.USER_1
    ): List<SavedCouponItem> {
        val current = loadReceivedCoupons(context, recipientSlot)
        val filtered = current.filterNot {
            (messageId.isNotBlank() && it.messageId == messageId) || it.title.equals(title, ignoreCase = true)
        }
        saveReceivedCouponsList(context, recipientSlot, filtered)
        return filtered
    }
}

@Composable
fun UserAvatarCircle(
    name: String,
    avatarBlobName: String,
    sizeDp: Int = 48,
    size: androidx.compose.ui.unit.Dp = sizeDp.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    backgroundColor: Color = containerColor,
    textColor: Color = contentColor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarBitmap = remember(avatarBlobName) {
        if (avatarBlobName.isNotBlank()) {
            val bytes = DuoVaultPersistence.readDecryptedBlobInMemory(context, avatarBlobName)
            if (bytes != null && bytes.isNotEmpty()) {
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            } else null
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = "$name profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * Magic Touch Privacy Lock Overlay when enabled in Settings.
 * Normal PIN codes are easily shoulder-surfed or guessed; Magic Touch requires
 * double-tapping the Hive logo (top-left or center) and then tapping Enter.
 */
@Composable
fun HivePinLockOverlay(
    expectedPin: String,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var errorShakeText by remember { mutableStateOf<String?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("hive_pin_lock_overlay")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Top-left Hive logo for Magic Touch (double-tap logo + Enter)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        errorShakeText = null
                        logoTapCount += 1
                    }
                    .padding(6.dp)
                    .testTag("magic_touch_top_left_logo"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hive_logo),
                    contentDescription = "Hive Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
                )
                Text(
                    text = "Hive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hive_logo),
                    contentDescription = "Hive Magic Touch Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        .clickable {
                            errorShakeText = null
                            logoTapCount += 1
                        }
                        .testTag("magic_touch_center_logo")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Hive Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = errorShakeText ?: "Magic Touch Protected",
                    fontSize = 13.sp,
                    color = if (errorShakeText != null) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Decoy / Visual Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val targetLen = 4
                    for (i in 0 until targetLen) {
                        val filled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Keypad with ENTER button (Unlocks when user tapped the Hive logo twice + clicks Enter)
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLR", "0", "ENTER")
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { key ->
                                val isEnter = key == "ENTER"
                                Surface(
                                    shape = CircleShape,
                                    color = if (isEnter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            errorShakeText = null
                                            when (key) {
                                                "CLR" -> {
                                                    enteredPin = ""
                                                    logoTapCount = 0
                                                }
                                                "ENTER" -> {
                                                    if (enteredPin.isEmpty() && logoTapCount == 2) {
                                                        onUnlocked()
                                                    } else if (enteredPin.isNotEmpty() && enteredPin == expectedPin) {
                                                        onUnlocked()
                                                    } else {
                                                        errorShakeText = "Try again"
                                                        enteredPin = ""
                                                        logoTapCount = 0
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                    }
                                                }
                                            }
                                        }
                                        .testTag(if (isEnter) "pin_key_OK" else "pin_key_$key")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontSize = if (key.length > 1) 11.5.sp else 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEnter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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

/**
 * Full Settings & Customization Modal accessible from the top-right Settings icon on the Dashboard.
 * Organized into separated, elevated visual cards with spacious internal layout:
 * 1) Your Profile (Display Name & Profile Picture)
 * 2) Theme Customization (Minimal Ink default, Mode & Color Presets)
 * 3) Soundboard Studio (Import/Record max 5 seconds, Named, Shared vs Personal)
 * 4) Know [Partner] Permissions (Hear & On-Demand Location toggles)
 * 5) Magic Touch Lock (Double-tap Hive logo + Enter)
 */
@Composable
fun HiveDashboardSettingsDialog(
    activeUser: TestUserSlot,
    config: HiveAppConfigState,
    currentDisplayName: String = if (activeUser == TestUserSlot.USER_1) {
        config.user1Name.ifBlank { "You" }
    } else {
        config.user2Name.ifBlank { "You" }
    },
    partnerDisplayName: String = if (activeUser == TestUserSlot.USER_1) {
        config.user2Name.ifBlank { "Partner" }
    } else {
        config.user1Name.ifBlank { "Partner" }
    },
    onUpdateDisplayName: (String) -> Unit = {},
    onJoinOrSwitchSpace: (spaceCode: String, slot: TestUserSlot, displayName: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var joinSpaceCodeInput by remember { mutableStateOf("") }

    var nameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var currentAvatarBlob by remember(activeUser, config) {
        mutableStateOf(
            if (activeUser == TestUserSlot.USER_1) config.user1AvatarBlob else config.user2AvatarBlob
        )
    }

    var selectedPreset by remember(config.themePreset) { mutableStateOf(config.themePreset) }
    var selectedMode by remember(config.themeMode) { mutableStateOf(config.themeMode) }

    var pinEnabled by remember(config.pinLockEnabled) { mutableStateOf(config.pinLockEnabled) }

    val myLocationConsent = if (activeUser == TestUserSlot.USER_1) {
        config.user1LocationConsent
    } else {
        config.user2LocationConsent
    }
    var locationConsentToggle by remember(myLocationConsent) { mutableStateOf(myLocationConsent) }

    val myHearConsent = if (activeUser == TestUserSlot.USER_1) {
        config.user1HearConsent
    } else {
        config.user2HearConsent
    }
    var hearConsentToggle by remember(myHearConsent) { mutableStateOf(myHearConsent) }

    // Soundboard State (Max 5 seconds per clip, Shared vs Personal)
    var soundboardClips by remember(activeUser, HiveSettingsPersistence.changeCounter) {
        mutableStateOf(HiveSettingsPersistence.loadSoundboardClips(context, activeUser))
    }
    var newSoundName by remember { mutableStateOf("") }
    var newSoundIsShared by remember { mutableStateOf(true) }
    var isRecordingSound by remember { mutableStateOf(false) }
    var recordingSecondsElapsed by remember { mutableIntStateOf(0) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var pendingRecordFile by remember { mutableStateOf<File?>(null) }

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPerm = granted
    }

    val hearPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hearConsentToggle = true
        HiveSettingsPersistence.updateHearConsent(context, activeUser, true)
        HiveKnowPartnerService.syncServiceState(context)
    }

    val backgroundLocationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        HiveKnowPartnerService.syncServiceState(context)
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        HiveSettingsPersistence.updateLocationConsent(context, activeUser, true)
        locationConsentToggle = true
        HiveKnowPartnerService.syncServiceState(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasBg) {
                runCatching {
                    backgroundLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val optimized = VaultCryptoEngine.optimizeImageBytes(raw)
                val blobName = DuoVaultPersistence.writeEncryptedBlob(context, optimized)
                currentAvatarBlob = blobName
                HiveSettingsPersistence.updateUserProfile(
                    context = context,
                    slot = activeUser,
                    displayName = nameInput.trim().ifEmpty { currentDisplayName },
                    avatarBlobName = blobName
                )
            }
        }
    }

    val importSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                var durationMs = 3000L
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    retriever.release()
                    if (durStr != null) {
                        durationMs = durStr.toLongOrNull() ?: 3000L
                    }
                }
                if (durationMs > 5500L) {
                    Toast.makeText(
                        context,
                        "Soundboard clips cannot exceed 5 seconds.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@runCatching
                }

                val sbDir = File(context.filesDir, "soundboard").apply { mkdirs() }
                val destFile = File(sbDir, "sb_imp_${System.currentTimeMillis()}.m4a")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val clipLabel = newSoundName.trim().ifEmpty { "Sound ${soundboardClips.size + 1}" }
                val newClip = SoundboardClip(
                    id = "sb_${System.currentTimeMillis()}",
                    title = clipLabel,
                    filePath = destFile.absolutePath,
                    ownerUserId = activeUser.userId,
                    isShared = newSoundIsShared,
                    durationSec = (durationMs / 1000L).toInt().coerceIn(1, 5)
                )
                HiveSettingsPersistence.addSoundboardClip(context, newClip)
                soundboardClips = HiveSettingsPersistence.loadSoundboardClips(context, activeUser)
                newSoundName = ""
                Toast.makeText(
                    context,
                    "Added \"${newClip.title}\" (${if (newSoundIsShared) "Shared" else "Personal"})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun finishSoundRecording() {
        if (!isRecordingSound) return
        try {
            activeRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            activeRecorder?.release()
        } catch (_: Exception) {
        }
        activeRecorder = null
        isRecordingSound = false
        val savedFile = pendingRecordFile
        if (savedFile != null && savedFile.exists()) {
            val clipLabel = newSoundName.trim().ifEmpty { "Clip ${soundboardClips.size + 1}" }
            val newClip = SoundboardClip(
                id = "sb_${System.currentTimeMillis()}",
                title = clipLabel,
                filePath = savedFile.absolutePath,
                ownerUserId = activeUser.userId,
                isShared = newSoundIsShared,
                durationSec = recordingSecondsElapsed.coerceIn(1, 5)
            )
            HiveSettingsPersistence.addSoundboardClip(context, newClip)
            soundboardClips = HiveSettingsPersistence.loadSoundboardClips(context, activeUser)
            newSoundName = ""
            Toast.makeText(
                context,
                "Saved ${newClip.durationSec}s sound (${if (newSoundIsShared) "Shared" else "Personal"})",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Enforce 5-second max recording duration
    LaunchedEffect(isRecordingSound) {
        if (isRecordingSound) {
            recordingSecondsElapsed = 0
            while (isActive && isRecordingSound && recordingSecondsElapsed < 5) {
                delay(1000L)
                recordingSecondsElapsed += 1
            }
            if (isRecordingSound && recordingSecondsElapsed >= 5) {
                finishSoundRecording()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeRecorder?.stop()
                activeRecorder?.release()
            } catch (_: Exception) {
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val settingsGrayBackground = if (isDark) Color(0xFF1E1F22) else Color(0xFFE5E7EB)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            shape = RectangleShape,
            color = settingsGrayBackground,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxSize()
                .wikiSevenTapShortcut(context)
                .testTag("hive_dashboard_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Full-width Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hive_logo),
                            contentDescription = "Hive Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Hive Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Profile, Themes, Soundboard & Privacy",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    thickness = 1.dp
                )

                // 1. PROFILE BLOCK (Full-Width, No Box Wrapper)
                HiveSettingsGroupCard(
                    icon = Icons.Default.Person,
                    title = "Your Profile ($currentDisplayName)",
                    subtitle = "Customize your display name and avatar"
                ) {
                    HiveSettingsSubCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            UserAvatarCircle(
                                name = nameInput.ifBlank { currentDisplayName },
                                avatarBlobName = currentAvatarBlob,
                                sizeDp = 64,
                                modifier = Modifier
                                    .clickable {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("settings_pick_avatar_button")
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Display Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_profile_name_input")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            avatarPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Photo", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val clean = nameInput.trim().ifEmpty { currentDisplayName }
                                            HiveSettingsPersistence.updateUserProfile(
                                                context = context,
                                                slot = activeUser,
                                                displayName = clean,
                                                avatarBlobName = currentAvatarBlob
                                            )
                                            onUpdateDisplayName(clean)
                                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_save_profile_button")
                                    ) {
                                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 1B. HIVE SPACE INVITE CODE & JOIN SPACE BLOCK
                HiveSettingsGroupCard(
                    icon = Icons.Default.Check,
                    title = "Hive Space (${config.activeSpaceCode})",
                    subtitle = "Share your invite code with your partner or join their space"
                ) {
                    HiveSettingsSubCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "ACTIVE SPACE CODE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = config.activeSpaceCode,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            cm?.setPrimaryClip(
                                                android.content.ClipData.newPlainText("Hive Space Code", config.activeSpaceCode)
                                            )
                                            Toast.makeText(context, "Copied Space Code: ${config.activeSpaceCode}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("settings_copy_space_code_button")
                                    ) {
                                        Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            HiveSettingsPersistence.leaveOrResetSpace(context)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("settings_reopen_space_setup_button")
                                    ) {
                                        Text("Switch Space", fontSize = 12.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = joinSpaceCodeInput,
                                    onValueChange = { joinSpaceCodeInput = it.uppercase().take(8) },
                                    placeholder = { Text("Enter partner's Space Code…") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_join_space_code_input")
                                )
                                Button(
                                    onClick = {
                                        val code = joinSpaceCodeInput.trim().uppercase()
                                        if (code.length >= 4) {
                                            val myName = nameInput.trim().ifEmpty { currentDisplayName }
                                            HiveSettingsPersistence.completeSpacePairing(
                                                context = context,
                                                spaceCode = code,
                                                slot = TestUserSlot.USER_2,
                                                displayName = myName,
                                                avatarBlobName = currentAvatarBlob
                                            )
                                            onJoinOrSwitchSpace(code, TestUserSlot.USER_2, myName)
                                            joinSpaceCodeInput = ""
                                            Toast.makeText(context, "Joined Space $code!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = joinSpaceCodeInput.trim().length >= 4,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("settings_join_space_button")
                                ) {
                                    Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // One-tap copy Supabase SQL setup script
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Supabase Database Setup",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Run this in your Supabase SQL Editor if cloud sync shows PGRST205 table errors",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            cm?.setPrimaryClip(
                                                android.content.ClipData.newPlainText(
                                                    "Supabase SQL Schema",
                                                    com.example.data.remote.SupabaseClientManager.SUPABASE_SQL_SCHEMA
                                                )
                                            )
                                            Toast.makeText(context, "Copied Supabase SQL Setup to Clipboard!", Toast.LENGTH_LONG).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Copy SQL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. THEME CUSTOMIZATION BLOCK (Full-Width, No Box Wrapper)
                HiveSettingsGroupCard(
                    icon = Icons.Default.Palette,
                    title = "Theme & Appearance",
                    subtitle = "Minimal Ink is default • Choose mode and palette"
                ) {
                    // Sub-area: Theme Mode (System / Dark / Light)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "APPEARANCE MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HiveThemeMode.entries.forEach { mode ->
                                val isSelected = selectedMode == mode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedMode = mode
                                            HiveSettingsPersistence.updateTheme(context, selectedPreset, mode)
                                        }
                                        .testTag("theme_mode_${mode.id}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 11.dp)
                                    ) {
                                        Text(
                                            text = mode.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sub-area: Theme Color Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "COLOR PALETTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HiveThemePreset.entries.forEach { preset ->
                                val isSelected = selectedPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(preset.surfaceDarkHex),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(preset.primaryDarkHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedPreset = preset
                                            HiveSettingsPersistence.updateTheme(context, preset, selectedMode)
                                        }
                                        .testTag("theme_preset_${preset.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(preset.primaryDarkHex))
                                        )
                                        Text(
                                            text = preset.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. SOUNDBOARD STUDIO BLOCK (Full-Width, No Box Wrapper)
                HiveSettingsGroupCard(
                    icon = Icons.Default.MusicNote,
                    title = "Soundboard Studio",
                    subtitle = "Import or record clips up to 5 seconds (Shared or Personal)"
                ) {
                    HiveSettingsSubCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value = newSoundName,
                                onValueChange = { newSoundName = it },
                                placeholder = { Text("Name your sound clip…") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_soundboard_name_input")
                            )

                            // Sub-area: Shared vs Personal Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(true to "Shared (Both Users)", false to "Personal (Only Me)").forEach { (isSharedOpt, label) ->
                                    val selected = newSoundIsShared == isSharedOpt
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { newSoundIsShared = isSharedOpt }
                                            .testTag(if (isSharedOpt) "soundboard_scope_shared" else "soundboard_scope_personal")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Record (<= 5s) + Import Audio Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!hasAudioPerm) {
                                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@Button
                                        }
                                        if (!isRecordingSound) {
                                            try {
                                                val sbDir = File(context.filesDir, "soundboard").apply { mkdirs() }
                                                val outFile = File(sbDir, "sb_rec_${System.currentTimeMillis()}.m4a")
                                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    MediaRecorder(context)
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    MediaRecorder()
                                                }
                                                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                                                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                recorder.setAudioSamplingRate(44100)
                                                recorder.setAudioEncodingBitRate(96000)
                                                recorder.setMaxDuration(5000)
                                                recorder.setOutputFile(outFile.absolutePath)
                                                recorder.prepare()
                                                recorder.start()
                                                activeRecorder = recorder
                                                pendingRecordFile = outFile
                                                isRecordingSound = true
                                            } catch (_: Exception) {
                                                isRecordingSound = false
                                            }
                                        } else {
                                            finishSoundRecording()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_soundboard_record_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingSound) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRecordingSound) "Stop (${5 - recordingSecondsElapsed}s)" else "Record (Max 5s)",
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        runCatching { importSoundLauncher.launch("audio/*") }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_soundboard_import_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import (≤5s)", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Existing Soundboard Clips List
                    if (soundboardClips.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "SAVED CLIPS (${soundboardClips.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            soundboardClips.forEach { clip ->
                                HiveSettingsSubCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = clip.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${if (clip.isShared) "Shared" else "Personal"} • ${clip.durationSec}s",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    playCustomSoundboardClip(
                                                        clip = clip,
                                                        soundboardVolume = 1f,
                                                        outputVolume = 100f,
                                                        isDeafened = false,
                                                        scope = scope
                                                    )
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Preview sound",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    HiveSettingsPersistence.deleteSoundboardClip(context, clip.id)
                                                    soundboardClips = HiveSettingsPersistence.loadSoundboardClips(context, activeUser)
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete sound",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

                // 4. KNOW [PARTNER] SETTINGS BLOCK (Hear & On-Demand See/Location)
                HiveSettingsGroupCard(
                    icon = Icons.Default.LocationOn,
                    title = "Know $partnerDisplayName",
                    subtitle = "On-demand Hear and See access"
                ) {
                    // Sub-area 1: Hear toggle
                    HiveSettingsSubCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Hear",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "On-demand audio check-in",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hearConsentToggle,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (hasAudioPerm) {
                                            hearConsentToggle = true
                                            HiveSettingsPersistence.updateHearConsent(context, activeUser, true)
                                            HiveKnowPartnerService.syncServiceState(context)
                                        } else {
                                            hearPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        hearConsentToggle = false
                                        HiveSettingsPersistence.updateHearConsent(context, activeUser, false)
                                        HiveKnowPartnerService.syncServiceState(context)
                                    }
                                },
                                modifier = Modifier.testTag("settings_hear_consent_switch")
                            )
                        }
                    }

                    // Sub-area 2: See (On-demand Location) toggle
                    HiveSettingsSubCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "See (Location)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fetches location only on-demand when requested",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = locationConsentToggle,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED ||
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            locationConsentToggle = true
                                            HiveSettingsPersistence.updateLocationConsent(context, activeUser, true)
                                            HiveKnowPartnerService.syncServiceState(context)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                val hasBg = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (!hasBg) {
                                                    runCatching {
                                                        backgroundLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                                    }
                                                }
                                            }
                                        } else {
                                            locationPermLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    } else {
                                        locationConsentToggle = false
                                        HiveSettingsPersistence.updateLocationConsent(context, activeUser, false)
                                        HiveKnowPartnerService.syncServiceState(context)
                                    }
                                },
                                modifier = Modifier.testTag("settings_location_consent_switch")
                            )
                        }
                    }
                }

                // 5. MAGIC TOUCH PRIVACY LOCK BLOCK
                HiveSettingsGroupCard(
                    icon = Icons.Default.Lock,
                    title = "Magic Touch Lock",
                    subtitle = "Discrete gesture lock on app open & Recents"
                ) {
                    HiveSettingsSubCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Enable Magic Touch",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap the Hive logo twice and press Enter to unlock on open & Recents",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pinEnabled,
                                onCheckedChange = {
                                    pinEnabled = it
                                    HiveSettingsPersistence.updatePinSettings(context, it)
                                },
                                modifier = Modifier.testTag("settings_pin_lock_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width borderless settings section (no outer box container), with consistent vertical spacing
 * and a clean bottom divider separating sections on the gray Settings background.
 */
@Composable
private fun HiveSettingsGroupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/**
 * Full-width borderless sub-section container (removes the box-like card border while preserving spacing).
 */
@Composable
private fun HiveSettingsSubCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/**
 * Opens a shortcut to Wikipedia in Chrome (or default browser fallback).
 */
fun openWikipediaShortcut(context: Context) {
    val wikiUri = Uri.parse("https://www.wikipedia.org")
    runCatching {
        val chromeIntent = Intent(Intent.ACTION_VIEW, wikiUri).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chromeIntent)
    }.recoverCatching {
        val browserIntent = Intent(Intent.ACTION_VIEW, wikiUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }
}

/**
 * Non-consuming pointer detector that watches for 7+ consecutive taps on the same area
 * (within 72.dp radius and 750ms between taps) and opens the Wikipedia shortcut.
 */
@Composable
fun Modifier.wikiSevenTapShortcut(context: Context): Modifier {
    val density = LocalDensity.current
    val maxDistancePx = remember(density) { with(density) { 72.dp.toPx() } }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf<Offset?>(null) }

    return this.pointerInput(context, maxDistancePx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downTime = System.currentTimeMillis()
            val pos = down.position
            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (up != null) {
                val now = System.currentTimeMillis()
                val prevPos = lastTapPosition
                val isQuickPress = (now - downTime) <= 450L
                val isConsecutiveTime = (now - lastTapTimeMs) <= 750L
                val isSameArea = prevPos != null &&
                    hypot((pos.x - prevPos.x).toDouble(), (pos.y - prevPos.y).toDouble()) <= maxDistancePx

                if (isQuickPress) {
                    if (tapCount == 0 || (isConsecutiveTime && isSameArea)) {
                        tapCount += 1
                    } else {
                        tapCount = 1
                    }
                    lastTapTimeMs = now
                    lastTapPosition = pos

                    if (tapCount >= 7) {
                        tapCount = 0
                        lastTapPosition = null
                        openWikipediaShortcut(context)
                    }
                } else {
                    tapCount = 0
                    lastTapPosition = null
                }
            }
        }
    }
}

/**
 * Production Space Setup & Pairing Screen for Hive:
 * Splits the two users into distinct identities:
 * - Mode 0 ("Create a Space"): Generates a unique 6-character Space Code as USER_1 (Creator) to share with your partner.
 * - Mode 1 ("Join Partner's Space"): Enters the partner's 6-character Space Code as USER_2 (Partner) to link into the same space.
 */
@Composable
fun HiveSpaceOnboardingScreen(
    config: HiveAppConfigState,
    onCreateSpace: (spaceCode: String, creatorName: String, avatarBlob: String?) -> Unit,
    onJoinSpace: (spaceCode: String, partnerName: String, avatarBlob: String?) -> Unit
) {
    val context = LocalContext.current
    var displayName by remember {
        val existingName = if (config.localUserSlot == TestUserSlot.USER_1) {
            config.user1Name
        } else {
            config.user2Name
        }
        mutableStateOf(existingName)
    }
    var avatarBlob by remember {
        val existingAvatar = if (config.localUserSlot == TestUserSlot.USER_1) {
            config.user1AvatarBlob
        } else {
            config.user2AvatarBlob
        }
        mutableStateOf(existingAvatar)
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Create a Space, 1 = Join a Space
    var generatedCode by remember {
        mutableStateOf(
            config.activeSpaceCode.takeIf { it.isNotBlank() && it != "HIVE01" }
                ?: HiveSettingsPersistence.generateSpaceInviteCode()
        )
    }
    var joinCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val optimized = VaultCryptoEngine.optimizeImageBytes(raw)
                avatarBlob = DuoVaultPersistence.writeEncryptedBlob(context, optimized)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("hive_space_onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Brand Header
            Image(
                painter = painterResource(id = R.drawable.hive_logo),
                contentDescription = "Hive Logo",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Welcome to Hive",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Set up your profile and create or join your private 2-person space.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Step 1: Your Profile Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. YOUR IDENTITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UserAvatarCircle(
                            name = displayName.ifBlank { "You" },
                            avatarBlobName = avatarBlob,
                            sizeDp = 58,
                            modifier = Modifier
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("onboarding_avatar_picker")
                        )

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                errorMessage = null
                            },
                            label = { Text("Your Display Name") },
                            placeholder = { Text("Enter your name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_name_input")
                        )
                    }
                }
            }

            // Step 2: Create Space vs Join Space Tabs
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "2. PAIR YOUR SPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(0 to "Create Space", 1 to "Join with Code").forEach { (idx, label) ->
                            val isSelected = selectedTab == idx
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedTab = idx
                                        errorMessage = null
                                    }
                                    .testTag(if (idx == 0) "onboarding_tab_create" else "onboarding_tab_join")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        // Create Space Mode
                        Text(
                            text = "Share this 6-character invite code with your partner so they can join your private Hive space:",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = generatedCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            generatedCode = HiveSettingsPersistence.generateSpaceInviteCode()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("New", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            cm?.setPrimaryClip(
                                                android.content.ClipData.newPlainText("Hive Space Code", generatedCode)
                                            )
                                            Toast.makeText(context, "Copied code $generatedCode", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("onboarding_copy_code_button")
                                    ) {
                                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                val cleanName = displayName.trim()
                                if (cleanName.isEmpty()) {
                                    errorMessage = "Please enter your display name first."
                                    return@Button
                                }
                                onCreateSpace(generatedCode, cleanName, avatarBlob.ifBlank { null })
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("onboarding_start_created_space_button")
                        ) {
                            Text(
                                text = "Create & Enter Hive Space",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Join Space Mode
                        Text(
                            text = "Enter the 6-character Space Invite Code your partner shared with you:",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = {
                                joinCodeInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(8)
                                errorMessage = null
                            },
                            label = { Text("Partner's Space Code") },
                            placeholder = { Text("e.g. HIVE84") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_join_code_input")
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                val cleanName = displayName.trim()
                                val cleanCode = joinCodeInput.trim().uppercase()
                                if (cleanName.isEmpty()) {
                                    errorMessage = "Please enter your display name first."
                                    return@Button
                                }
                                if (cleanCode.length < 4) {
                                    errorMessage = "Please enter a valid Space Code."
                                    return@Button
                                }
                                onJoinSpace(cleanCode, cleanName, avatarBlob.ifBlank { null })
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("onboarding_join_space_confirm_button")
                        ) {
                            Text(
                                text = "Join Partner's Space",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Step 3: Cloud Database Sync Helper
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Supabase Cloud Sync Setup",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "If you encounter table sync errors (PGRST205), copy and execute the SQL schema in your Supabase project's SQL Editor.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            cm?.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "Supabase SQL Schema",
                                    com.example.data.remote.SupabaseClientManager.SUPABASE_SQL_SCHEMA
                                )
                            )
                            Toast.makeText(context, "Copied Supabase SQL Setup Script!", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Copy SQL", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}




