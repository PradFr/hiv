package com.example.data.local

import android.content.Context
import com.example.ui.TestUserSlot
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class MonthlyMomentItem(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String = "TEXT", // "TEXT", "IMAGE", "DOODLE", "COUPON"
    val mediaUriOrBlob: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

object MonthlyMomentsPersistence {
    private const val PREFS_NAME = "duo_monthly_moments_store"

    private fun momentsKey(spaceCode: String, slot: TestUserSlot): String {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        return "moments_${safeCode}_${slot.name}"
    }

    private fun lockedKey(spaceCode: String, slot: TestUserSlot): String {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        return "explicit_locked_${safeCode}_${slot.name}"
    }

    private fun testUnlockedKey(spaceCode: String, slot: TestUserSlot): String {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        return "test_unlocked_${safeCode}_${slot.name}"
    }

    fun daysUntilEndOfMonth(): Int {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (maxDay - currentDay).coerceAtLeast(0)
    }

    fun isCalendarEndOfMonth(): Boolean {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Considered end of month in the final 3-4 days of the calendar month
        return currentDay >= 28 || currentDay == maxDay
    }

    /**
     * Rule:
     * - Only opens at the end of the month.
     * - Once opened and the user clears it using the 3-dot option, it becomes locked again.
     * - Only locked state depends on whether it was cleared, not automatically by date.
     */
    fun isPartnerMomentsUnlocked(context: Context, spaceCode: String, partnerSlot: TestUserSlot): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val testUnlocked = prefs.getBoolean(testUnlockedKey(spaceCode, partnerSlot), false)
        if (testUnlocked) return true

        val isExplicitlyLocked = prefs.getBoolean(lockedKey(spaceCode, partnerSlot), false)
        if (isExplicitlyLocked) {
            // User explicitly cleared it, so it remains locked!
            return false
        }

        return isCalendarEndOfMonth()
    }

    fun setTestUnlocked(context: Context, spaceCode: String, partnerSlot: TestUserSlot, unlocked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(testUnlockedKey(spaceCode, partnerSlot), unlocked)
            .putBoolean(lockedKey(spaceCode, partnerSlot), !unlocked)
            .apply()
    }

    private fun doodleKey(spaceCode: String, slot: TestUserSlot): String {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        return "moments_doodle_${safeCode}_${slot.name}"
    }

    private fun doodleSpaceKey(spaceCode: String, slot: TestUserSlot): String {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        return "moments_doodle_space_${safeCode}_${slot.name}"
    }

    fun loadMomentMessages(context: Context, spaceCode: String, slot: TestUserSlot): List<MessageEntity> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(momentsKey(spaceCode, slot), null)
        if (rawJson.isNullOrBlank()) return emptyList()

        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<MessageEntity>()
            val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val legacyType = obj.optString("type", "TEXT")
                val msgType = obj.optString("message_type", "").ifBlank {
                    when (legacyType) {
                        "IMAGE" -> MessageType.PHOTO
                        "COUPON" -> MessageType.COUPLE_CARD
                        else -> legacyType
                    }
                }
                val legacyMedia = obj.optString("media", "").takeIf { it.isNotBlank() }
                val content = obj.optString("content", "")
                val mediaMeta = obj.optString("media_meta", "").takeIf { it.isNotBlank() }
                    ?: when {
                        legacyType == "IMAGE" && legacyMedia != null -> "$legacyMedia|$content|0"
                        legacyType == "COUPON" -> "COUPON|$content| Prepared in Monthly Moments|For You"
                        else -> legacyMedia
                    }

                list.add(
                    MessageEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        spaceCode = safeCode,
                        senderId = obj.optString("sender_id", ""),
                        senderName = obj.optString("sender_name", ""),
                        content = content,
                        messageType = msgType,
                        mediaMeta = mediaMeta,
                        replyToId = obj.optString("reply_to_id", "").takeIf { it.isNotBlank() },
                        replyToPreview = obj.optString("reply_to_preview", "").takeIf { it.isNotBlank() },
                        reaction = obj.optString("reaction", "").takeIf { it.isNotBlank() },
                        isPinned = obj.optBoolean("is_pinned", false),
                        isDelivered = obj.optBoolean("is_delivered", true),
                        isRead = obj.optBoolean("is_read", true),
                        createdAt = obj.optLong("created_at_ms", System.currentTimeMillis()),
                        isSystemEvent = obj.optBoolean("is_system_event", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAllMomentMessages(context: Context, spaceCode: String, slot: TestUserSlot, messages: List<MessageEntity>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        messages.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("sender_id", m.senderId)
            obj.put("sender_name", m.senderName)
            obj.put("content", m.content)
            obj.put("message_type", m.messageType)
            obj.put("type", m.messageType)
            obj.put("media_meta", m.mediaMeta ?: "")
            obj.put("media", m.mediaMeta ?: "")
            obj.put("reply_to_id", m.replyToId ?: "")
            obj.put("reply_to_preview", m.replyToPreview ?: "")
            obj.put("reaction", m.reaction ?: "")
            obj.put("is_pinned", m.isPinned)
            obj.put("is_delivered", m.isDelivered)
            obj.put("is_read", m.isRead)
            obj.put("created_at_ms", m.createdAt)
            obj.put("is_system_event", m.isSystemEvent)
            arr.put(obj)
        }
        prefs.edit()
            .putString(momentsKey(spaceCode, slot), arr.toString())
            .remove(lockedKey(spaceCode, slot))
            .apply()
    }

    fun saveMomentMessage(context: Context, spaceCode: String, slot: TestUserSlot, message: MessageEntity) {
        val existing = loadMomentMessages(context, spaceCode, slot).toMutableList()
        existing.add(message)
        saveAllMomentMessages(context, spaceCode, slot, existing)
    }

    fun loadMomentDoodle(context: Context, spaceCode: String, slot: TestUserSlot): Pair<String?, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(doodleKey(spaceCode, slot), null)?.takeIf { it.isNotBlank() }
        val extraSpace = prefs.getInt(doodleSpaceKey(spaceCode, slot), 0)
        return data to extraSpace
    }

    fun saveMomentDoodle(context: Context, spaceCode: String, slot: TestUserSlot, doodleData: String?, extraBottomSpaceDp: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(doodleKey(spaceCode, slot), doodleData ?: "")
            .putInt(doodleSpaceKey(spaceCode, slot), extraBottomSpaceDp)
            .apply()
    }

    fun loadMoments(context: Context, spaceCode: String, slot: TestUserSlot): List<MonthlyMomentItem> {
        val msgs = loadMomentMessages(context, spaceCode, slot)
        return msgs.filter { it.messageType != MessageType.DOODLE }.map { m ->
            MonthlyMomentItem(
                id = m.id,
                senderId = m.senderId,
                senderName = m.senderName,
                content = m.content,
                type = m.messageType,
                mediaUriOrBlob = m.mediaMeta,
                createdAtMs = m.createdAt
            )
        }
    }

    fun saveMoment(context: Context, spaceCode: String, slot: TestUserSlot, item: MonthlyMomentItem) {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        val msg = MessageEntity(
            id = item.id,
            spaceCode = safeCode,
            senderId = item.senderId,
            senderName = item.senderName,
            content = item.content,
            messageType = when (item.type) {
                "IMAGE" -> MessageType.PHOTO
                "COUPON" -> MessageType.COUPLE_CARD
                else -> item.type
            },
            mediaMeta = when {
                item.type == "IMAGE" && !item.mediaUriOrBlob.isNullOrBlank() -> "${item.mediaUriOrBlob}|${item.content}|0"
                item.type == "COUPON" -> "COUPON|${item.content}|Prepared in Monthly Moments|For You"
                else -> item.mediaUriOrBlob
            },
            isDelivered = true,
            isRead = true,
            createdAt = item.createdAtMs
        )
        saveMomentMessage(context, spaceCode, slot, msg)
    }

    /**
     * Clears the chat and immediately locks it again, fulfilling:
     * "once the chat is cleared it will become locked again and only if the chat is locked, not by date."
     */
    fun clearMomentsAndRelock(context: Context, spaceCode: String, slot: TestUserSlot) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(momentsKey(spaceCode, slot))
            .remove(doodleKey(spaceCode, slot))
            .remove(doodleSpaceKey(spaceCode, slot))
            .putBoolean(lockedKey(spaceCode, slot), true)
            .putBoolean(testUnlockedKey(spaceCode, slot), false)
            .apply()
    }
}
