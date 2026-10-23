package com.example.data.repository

import android.util.Log
import com.example.data.local.DuoSpaceDao
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.SpaceEntity
import com.example.data.remote.SupabaseClientManager
import com.example.data.remote.SupabaseMessageDto
import com.example.data.remote.SupabaseReactionPatchDto
import com.example.data.remote.SupabaseSpaceDto
import com.example.data.remote.SupabaseSpacePatchDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class SpaceReceiptWatermarks(
    val baseTopic: String = "Hive Space",
    val user1DeliveredUpToMs: Long = 0L,
    val user1ReadUpToMs: Long = 0L,
    val user2DeliveredUpToMs: Long = 0L,
    val user2ReadUpToMs: Long = 0L
)

private fun parseWatermarksFromTopic(rawTopic: String?): SpaceReceiptWatermarks {
    val safe = rawTopic?.ifBlank { "Hive Space" } ?: "Hive Space"
    val parts = safe.split("||", limit = 2)
    val base = parts.firstOrNull()?.ifBlank { "Hive Space" } ?: "Hive Space"
    if (parts.size < 2) return SpaceReceiptWatermarks(baseTopic = base)
    var d1 = 0L
    var r1 = 0L
    var d2 = 0L
    var r2 = 0L
    parts[1].split("|").forEach { token ->
        val kv = token.split(":", limit = 2)
        if (kv.size == 2) {
            val v = kv[1].toLongOrNull() ?: 0L
            when (kv[0]) {
                "D1" -> d1 = v
                "R1" -> r1 = v
                "D2" -> d2 = v
                "R2" -> r2 = v
            }
        }
    }
    return SpaceReceiptWatermarks(
        baseTopic = base,
        user1DeliveredUpToMs = d1,
        user1ReadUpToMs = r1,
        user2DeliveredUpToMs = d2,
        user2ReadUpToMs = r2
    )
}

private fun encodeWatermarksIntoTopic(w: SpaceReceiptWatermarks): String {
    return "${w.baseTopic}||D1:${w.user1DeliveredUpToMs}|R1:${w.user1ReadUpToMs}|D2:${w.user2DeliveredUpToMs}|R2:${w.user2ReadUpToMs}"
}

class DuoSpaceRepository(
    private val dao: DuoSpaceDao,
    val supabaseClient: SupabaseClientManager = SupabaseClientManager()
) {
    companion object {
        const val DEFAULT_SPACE_CODE = "HIVE01"
        const val USER_1_ID = "hive_user_1"
        const val USER_2_ID = "hive_user_2"
    }

    val isSupabaseConfigured: Boolean
        get() = supabaseClient.isConfigured

    fun observeSpace(code: String = DEFAULT_SPACE_CODE): Flow<SpaceEntity?> =
        dao.observeSpace(code)

    fun observeMessages(spaceCode: String = DEFAULT_SPACE_CODE): Flow<List<MessageEntity>> =
        dao.observeMessagesForSpace(spaceCode)

    suspend fun getSpace(code: String): SpaceEntity? = dao.getSpaceByCode(code.trim().uppercase().ifBlank { DEFAULT_SPACE_CODE })

    suspend fun ensureSpaceInitialized(
        spaceCode: String = DEFAULT_SPACE_CODE,
        creatorName: String = "You",
        partnerName: String? = null
    ) {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DEFAULT_SPACE_CODE }
        val existing = dao.getSpaceByCode(cleanCode)
        if (existing == null) {
            val now = System.currentTimeMillis()
            val space = SpaceEntity(
                code = cleanCode,
                topic = "Hive Space",
                creatorId = USER_1_ID,
                creatorName = creatorName.trim().ifBlank { "Partner 1" },
                partnerId = if (!partnerName.isNullOrBlank()) USER_2_ID else null,
                partnerName = partnerName?.trim()?.ifBlank { null },
                extraBottomSpaceDp = 0,
                createdAt = now,
                lastMessagePreview = "Space ready • Share code $cleanCode with your partner",
                lastUpdatedAt = now
            )
            dao.upsertSpace(space)

            supabaseClient.restService?.let { api ->
                try {
                    val remoteList = api.getSpaceByCode(codeFilter = "eq.$cleanCode")
                    val remote = remoteList.firstOrNull()
                    if (remote != null) {
                        dao.upsertSpace(
                            space.copy(
                                topic = remote.topic,
                                creatorName = remote.creatorName.ifBlank { space.creatorName },
                                partnerId = remote.partnerId ?: space.partnerId,
                                partnerName = remote.partnerName ?: space.partnerName,
                                lastMessagePreview = remote.lastMessagePreview ?: space.lastMessagePreview,
                                lastUpdatedAt = remote.lastUpdatedAtMs
                            )
                        )
                    } else {
                        api.upsertSpace(space.toDto())
                    }
                } catch (e: Exception) {
                    Log.w("DuoSpaceRepo", "Initial space cloud sync skipped: ${e.message}")
                }
            }
        } else if (existing.partnerId == null && !partnerName.isNullOrBlank()) {
            val updated = existing.copy(
                partnerId = USER_2_ID,
                partnerName = partnerName.trim(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            dao.upsertSpace(updated)
            supabaseClient.restService?.let { api ->
                try {
                    api.updateSpace(
                        codeFilter = "eq.$cleanCode",
                        patch = SupabaseSpacePatchDto(
                            partnerId = USER_2_ID,
                            partnerName = partnerName.trim(),
                            lastUpdatedAtMs = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.w("DuoSpaceRepo", "Partner connect cloud sync deferred: ${e.message}")
                }
            }
        }
    }

    suspend fun createNewSpace(
        spaceCode: String,
        creatorName: String
    ): SpaceEntity {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DEFAULT_SPACE_CODE }
        val cleanCreator = creatorName.trim().ifBlank { "Creator" }
        val now = System.currentTimeMillis()
        val space = SpaceEntity(
            code = cleanCode,
            topic = encodeWatermarksIntoTopic(
                SpaceReceiptWatermarks(
                    baseTopic = "Hive Space",
                    user1DeliveredUpToMs = now,
                    user1ReadUpToMs = now
                )
            ),
            creatorId = USER_1_ID,
            creatorName = cleanCreator,
            partnerId = null,
            partnerName = null,
            extraBottomSpaceDp = 0,
            createdAt = now,
            lastMessagePreview = "Space created • Invite code: $cleanCode",
            lastUpdatedAt = now
        )
        dao.upsertSpace(space)

        supabaseClient.restService?.let { api ->
            try {
                api.upsertSpace(space.toDto())
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Create space cloud sync deferred: ${e.message}")
            }
        }
        return space
    }

    suspend fun joinExistingSpace(
        spaceCode: String,
        partnerName: String
    ): SpaceEntity {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DEFAULT_SPACE_CODE }
        val cleanPartner = partnerName.trim().ifBlank { "Partner" }
        val now = System.currentTimeMillis()

        var remoteSpace: SupabaseSpaceDto? = null
        supabaseClient.restService?.let { api ->
            try {
                remoteSpace = api.getSpaceByCode(codeFilter = "eq.$cleanCode").firstOrNull()
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Fetch remote space on join failed: ${e.message}")
            }
        }

        val localExisting = dao.getSpaceByCode(cleanCode)
        val existingWatermarks = parseWatermarksFromTopic(remoteSpace?.topic ?: localExisting?.topic)
        val updatedTopic = encodeWatermarksIntoTopic(
            existingWatermarks.copy(
                user2DeliveredUpToMs = maxOf(existingWatermarks.user2DeliveredUpToMs, now),
                user2ReadUpToMs = maxOf(existingWatermarks.user2ReadUpToMs, now)
            )
        )

        val resolvedCreatorName = remoteSpace?.creatorName?.takeIf { it.isNotBlank() }
            ?: localExisting?.creatorName?.takeIf { it.isNotBlank() }
            ?: "Partner"

        val joinedSpace = SpaceEntity(
            code = cleanCode,
            topic = updatedTopic,
            creatorId = remoteSpace?.creatorId ?: localExisting?.creatorId ?: USER_1_ID,
            creatorName = resolvedCreatorName,
            partnerId = USER_2_ID,
            partnerName = cleanPartner,
            pinnedMessageId = localExisting?.pinnedMessageId,
            pinnedNote = localExisting?.pinnedNote,
            screenDoodleData = localExisting?.screenDoodleData,
            extraBottomSpaceDp = localExisting?.extraBottomSpaceDp ?: 0,
            autoRecordCalls = localExisting?.autoRecordCalls ?: false,
            createdAt = remoteSpace?.createdAtMs ?: localExisting?.createdAt ?: now,
            lastMessagePreview = "$cleanPartner joined the Hive Space",
            lastUpdatedAt = now
        )
        dao.upsertSpace(joinedSpace)

        supabaseClient.restService?.let { api ->
            try {
                if (remoteSpace != null) {
                    api.updateSpace(
                        codeFilter = "eq.$cleanCode",
                        patch = SupabaseSpacePatchDto(
                            topic = updatedTopic,
                            creatorName = resolvedCreatorName,
                            partnerId = USER_2_ID,
                            partnerName = cleanPartner,
                            lastMessagePreview = "$cleanPartner joined the Hive Space",
                            lastUpdatedAtMs = now
                        )
                    )
                } else {
                    api.upsertSpace(joinedSpace.toDto())
                }
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Join space cloud sync deferred: ${e.message}")
            }
        }

        syncSpaceFromSupabase(cleanCode, USER_2_ID)
        return joinedSpace
    }

    suspend fun updateMemberNameInSpace(
        spaceCode: String,
        userId: String,
        displayName: String
    ) {
        val cleanName = displayName.trim()
        if (cleanName.isBlank()) return
        val existing = dao.getSpaceByCode(spaceCode) ?: return
        val now = System.currentTimeMillis()
        val updated = if (userId == USER_1_ID) {
            existing.copy(creatorName = cleanName, lastUpdatedAt = now)
        } else {
            existing.copy(partnerId = USER_2_ID, partnerName = cleanName, lastUpdatedAt = now)
        }
        dao.upsertSpace(updated)
        supabaseClient.restService?.let { api ->
            try {
                api.updateSpace(
                    codeFilter = "eq.$spaceCode",
                    patch = SupabaseSpacePatchDto(
                        topic = updated.topic,
                        creatorName = updated.creatorName,
                        partnerId = updated.partnerId,
                        partnerName = updated.partnerName,
                        lastMessagePreview = updated.lastMessagePreview,
                        lastUpdatedAtMs = now
                    )
                )
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Member name cloud sync deferred: ${e.message}")
            }
        }
    }

    suspend fun markAllReadForUser(readerId: String, spaceCode: String = DEFAULT_SPACE_CODE) {
        dao.markMessagesRead(spaceCode, readerId)
        val existing = dao.getSpaceByCode(spaceCode) ?: return
        val now = System.currentTimeMillis()
        val currentWm = parseWatermarksFromTopic(existing.topic)
        val nextWm = if (readerId == USER_1_ID) {
            currentWm.copy(
                user1DeliveredUpToMs = maxOf(currentWm.user1DeliveredUpToMs, now),
                user1ReadUpToMs = maxOf(currentWm.user1ReadUpToMs, now)
            )
        } else {
            currentWm.copy(
                user2DeliveredUpToMs = maxOf(currentWm.user2DeliveredUpToMs, now),
                user2ReadUpToMs = maxOf(currentWm.user2ReadUpToMs, now)
            )
        }
        val nextTopic = encodeWatermarksIntoTopic(nextWm)
        if (nextTopic != existing.topic) {
            val updatedSpace = existing.copy(topic = nextTopic, lastUpdatedAt = now)
            dao.upsertSpace(updatedSpace)
            supabaseClient.restService?.let { api ->
                try {
                    api.updateSpace(
                        codeFilter = "eq.$spaceCode",
                        patch = SupabaseSpacePatchDto(
                            topic = nextTopic,
                            creatorName = updatedSpace.creatorName,
                            partnerId = updatedSpace.partnerId,
                            partnerName = updatedSpace.partnerName,
                            lastMessagePreview = updatedSpace.lastMessagePreview,
                            lastUpdatedAtMs = now
                        )
                    )
                } catch (e: Exception) {
                    Log.w("DuoSpaceRepo", "Read watermark sync deferred: ${e.message}")
                }
            }
        }
    }

    suspend fun updateScreenDoodle(
        doodleData: String?,
        extraBottomSpaceDp: Int,
        spaceCode: String = DEFAULT_SPACE_CODE
    ) {
        if (doodleData.isNullOrBlank()) {
            dao.deleteDoodleSpacersForSpace(spaceCode)
        }
        val existing = dao.getSpaceByCode(spaceCode) ?: return
        dao.upsertSpace(
            existing.copy(
                screenDoodleData = doodleData,
                extraBottomSpaceDp = if (doodleData.isNullOrBlank()) 0 else extraBottomSpaceDp.coerceIn(0, 3200),
                lastUpdatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun sendMessage(
        senderId: String,
        senderName: String,
        content: String,
        messageType: String = MessageType.TEXT,
        mediaMeta: String? = null,
        replyTo: MessageEntity? = null,
        spaceCode: String = DEFAULT_SPACE_CODE
    ): Result<MessageEntity> {
        val trimmed = content.trim()
        if (trimmed.isEmpty() && messageType == MessageType.TEXT) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }

        val space = dao.getSpaceByCode(spaceCode)
        val now = System.currentTimeMillis()

        // If the user drew in bottom doodle space, lock that height into the timeline
        val reservedDoodleSpaceDp = space?.extraBottomSpaceDp ?: 0
        if (reservedDoodleSpaceDp > 0 && !space?.screenDoodleData.isNullOrBlank()) {
            val doodleSpacerMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                spaceCode = spaceCode,
                senderId = senderId,
                senderName = senderName.trim().ifEmpty { "User" },
                content = "Doodle Space",
                messageType = MessageType.DOODLE,
                mediaMeta = reservedDoodleSpaceDp.toString(),
                replyToId = null,
                replyToPreview = null,
                reaction = null,
                isPinned = false,
                isDelivered = true,
                isRead = true,
                createdAt = now - 1L,
                isSystemEvent = true
            )
            dao.insertMessage(doodleSpacerMsg)
        }

        val replyPreviewText = replyTo?.let { target ->
            val summary = when (target.messageType) {
                MessageType.VOICE -> "🎤 Voice message (${target.content})"
                MessageType.STICKER -> "✨ Sticker (${target.content})"
                MessageType.CALL_LOG -> "📞 ${target.content}"
                MessageType.PHOTO -> "📷 Photo (${target.content.ifBlank { "Image" }})"
                MessageType.VIDEO -> "🎬 Video (${target.content.ifBlank { "Clip" }})"
                MessageType.FILE -> "📎 File (${target.content})"
                MessageType.POLL -> "📊 Poll: ${target.content}"
                MessageType.CONTACT -> "👤 Contact: ${target.content}"
                MessageType.LOCATION -> "📍 Location: ${target.content}"
                MessageType.COUPLE_CARD -> "💌 ${target.content}"
                else -> target.content.take(60)
            }
            "${target.senderName}: $summary"
        }

        // Starts at isDelivered = false, isRead = false (1 single gray tick: sent from your side)
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            spaceCode = spaceCode,
            senderId = senderId,
            senderName = senderName.trim().ifEmpty { "User" },
            content = trimmed,
            messageType = messageType,
            mediaMeta = mediaMeta,
            replyToId = replyTo?.id,
            replyToPreview = replyPreviewText,
            reaction = null,
            isPinned = false,
            isDelivered = false,
            isRead = false,
            createdAt = now,
            isSystemEvent = false
        )

        dao.insertMessage(message)
        val isPartnerJoined = !space?.partnerId.isNullOrBlank() || !space?.partnerName.isNullOrBlank()

        if (space != null) {
            val previewLabel = when (messageType) {
                MessageType.PHOTO -> "📷 Sent a photo${if (trimmed.isNotBlank()) ": $trimmed" else ""}"
                MessageType.VIDEO -> "🎬 Sent a video${if (trimmed.isNotBlank()) ": $trimmed" else ""}"
                MessageType.FILE -> "📎 Sent file: $trimmed"
                MessageType.POLL -> "📊 Poll: $trimmed"
                MessageType.CONTACT -> "👤 Contact: $trimmed"
                MessageType.LOCATION -> "📍 Location: $trimmed"
                MessageType.COUPLE_CARD -> "💌 $trimmed"
                else -> trimmed
            }
            val updatedSpace = space.copy(
                extraBottomSpaceDp = 0,
                lastMessagePreview = "${message.senderName}: $previewLabel",
                lastUpdatedAt = now
            )
            dao.upsertSpace(updatedSpace)
        }

        val api = supabaseClient.restService
        if (api != null) {
            try {
                api.insertMessage(message.toDto())
                if (space != null) {
                    api.updateSpace(
                        codeFilter = "eq.$spaceCode",
                        patch = SupabaseSpacePatchDto(
                            topic = space.topic,
                            creatorName = space.creatorName,
                            partnerId = space.partnerId,
                            partnerName = space.partnerName,
                            lastMessagePreview = "${message.senderName}: $trimmed",
                            lastUpdatedAtMs = now
                        )
                    )
                }
                // If partner has joined the space and message reached the cloud relay, mark delivered (2 gray ticks)
                if (isPartnerJoined) {
                    dao.markMessageDelivered(message.id)
                }
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Supabase sendMessage sync deferred: ${e.message}")
            }
        } else if (isPartnerJoined) {
            // Local/offline space with partner present -> mark delivered (2 gray ticks, unread)
            dao.markMessageDelivered(message.id)
        }

        return Result.success(message)
    }

    suspend fun updateMessageMetadata(message: MessageEntity, newContent: String, newMediaMeta: String?) {
        dao.updateMessageContentAndMeta(message.id, newContent, newMediaMeta)
        val updated = message.copy(content = newContent, mediaMeta = newMediaMeta)
        supabaseClient.restService?.let { api ->
            try {
                api.insertMessage(updated.toDto())
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Supabase metadata update deferred: ${e.message}")
            }
        }
    }

    suspend fun setReaction(message: MessageEntity, emoji: String?) {
        dao.updateMessageReaction(message.id, emoji)

        supabaseClient.restService?.let { api ->
            try {
                api.updateMessageReaction(
                    idFilter = "eq.${message.id}",
                    patch = SupabaseReactionPatchDto(reaction = emoji)
                )
            } catch (e: Exception) {
                Log.w("DuoSpaceRepo", "Supabase reaction sync deferred: ${e.message}")
            }
        }
    }

    suspend fun toggleReaction(message: MessageEntity, emoji: String) {
        val newReaction = if (message.reaction == emoji) null else emoji
        setReaction(message, newReaction)
    }

    suspend fun togglePinMessage(
        message: MessageEntity,
        spaceCode: String = DEFAULT_SPACE_CODE
    ) {
        val willPin = !message.isPinned
        dao.setMessagePinned(message.id, willPin)
        val existingSpace = dao.getSpaceByCode(spaceCode) ?: return
        val pinSummary = if (willPin) {
            val label = when (message.messageType) {
                MessageType.VOICE -> "🎤 Voice message (${message.content})"
                MessageType.STICKER -> "✨ Sticker (${message.content})"
                MessageType.CALL_LOG -> "📞 ${message.content}"
                MessageType.PHOTO -> "📷 Photo (${message.content})"
                MessageType.VIDEO -> "🎬 Video (${message.content})"
                MessageType.FILE -> "📎 File (${message.content})"
                MessageType.POLL -> "📊 Poll (${message.content})"
                MessageType.CONTACT -> "👤 Contact (${message.content})"
                MessageType.LOCATION -> "📍 Location (${message.content})"
                MessageType.COUPLE_CARD -> "💌 ${message.content}"
                else -> message.content
            }
            "${message.senderName}: $label"
        } else null
        dao.upsertSpace(
            existingSpace.copy(
                pinnedMessageId = if (willPin) message.id else null,
                pinnedNote = pinSummary
            )
        )
    }

    suspend fun unpinCurrentMessage(spaceCode: String = DEFAULT_SPACE_CODE) {
        dao.clearAllPinnedInSpace(spaceCode)
        val existingSpace = dao.getSpaceByCode(spaceCode) ?: return
        dao.upsertSpace(existingSpace.copy(pinnedMessageId = null, pinnedNote = null))
    }

    suspend fun setAutoRecordCalls(
        enabled: Boolean,
        spaceCode: String = DEFAULT_SPACE_CODE
    ) {
        val existingSpace = dao.getSpaceByCode(spaceCode) ?: return
        dao.upsertSpace(existingSpace.copy(autoRecordCalls = enabled))
    }

    suspend fun updateUserPresenceStatus(
        spaceCode: String = DEFAULT_SPACE_CODE,
        isUser1: Boolean,
        status: String
    ) {
        val cleanCode = spaceCode.trim().uppercase().ifBlank { DEFAULT_SPACE_CODE }
        if (isUser1) {
            dao.updateUser1Status(cleanCode, status)
        } else {
            dao.updateUser2Status(cleanCode, status)
        }
    }

    suspend fun syncSpaceFromSupabase(
        spaceCode: String = DEFAULT_SPACE_CODE,
        activeUserId: String = USER_1_ID
    ) {
        val api = supabaseClient.restService ?: return
        try {
            val now = System.currentTimeMillis()
            // 1. Sync Space metadata & receipt watermarks
            val remoteSpace = api.getSpaceByCode(codeFilter = "eq.$spaceCode").firstOrNull()
            val localSpace = dao.getSpaceByCode(spaceCode)
            var watermarks = parseWatermarksFromTopic(remoteSpace?.topic ?: localSpace?.topic)

            // Update our own delivered watermark so the partner knows their messages reached our device
            watermarks = if (activeUserId == USER_1_ID) {
                watermarks.copy(user1DeliveredUpToMs = maxOf(watermarks.user1DeliveredUpToMs, now))
            } else {
                watermarks.copy(user2DeliveredUpToMs = maxOf(watermarks.user2DeliveredUpToMs, now))
            }

            if (remoteSpace != null && localSpace != null) {
                val mergedTopic = encodeWatermarksIntoTopic(watermarks)
                val mergedSpace = localSpace.copy(
                    topic = mergedTopic,
                    creatorName = remoteSpace.creatorName.ifBlank { localSpace.creatorName },
                    partnerId = remoteSpace.partnerId ?: localSpace.partnerId,
                    partnerName = remoteSpace.partnerName ?: localSpace.partnerName,
                    lastMessagePreview = remoteSpace.lastMessagePreview ?: localSpace.lastMessagePreview,
                    lastUpdatedAt = maxOf(localSpace.lastUpdatedAt, remoteSpace.lastUpdatedAtMs)
                )
                dao.upsertSpace(mergedSpace)
            } else if (remoteSpace != null && localSpace == null) {
                val mergedTopic = encodeWatermarksIntoTopic(watermarks)
                val newSpace = SpaceEntity(
                    code = remoteSpace.code,
                    topic = mergedTopic,
                    creatorId = remoteSpace.creatorId,
                    creatorName = remoteSpace.creatorName,
                    partnerId = remoteSpace.partnerId,
                    partnerName = remoteSpace.partnerName,
                    extraBottomSpaceDp = 0,
                    createdAt = remoteSpace.createdAtMs,
                    lastMessagePreview = remoteSpace.lastMessagePreview,
                    lastUpdatedAt = remoteSpace.lastUpdatedAtMs
                )
                dao.upsertSpace(newSpace)
            }

            // 2. Sync Messages and compute accurate 3-state delivery & read flags
            val remoteMessages = api.getMessagesForSpace(spaceCodeFilter = "eq.$spaceCode")
            if (remoteMessages.isNotEmpty()) {
                val existingById = dao.getMessagesForSpaceList(spaceCode).associateBy { it.id }
                val partnerJoined = !(remoteSpace?.partnerId ?: localSpace?.partnerId).isNullOrBlank()

                val entities = remoteMessages.map { dto ->
                    val prev = existingById[dto.id]
                    val base = dto.toEntity()
                    val isMine = dto.senderId == activeUserId
                    if (isMine) {
                        val partnerDelivTs = if (activeUserId == USER_1_ID) {
                            watermarks.user2DeliveredUpToMs
                        } else {
                            watermarks.user1DeliveredUpToMs
                        }
                        val partnerReadTs = if (activeUserId == USER_1_ID) {
                            watermarks.user2ReadUpToMs
                        } else {
                            watermarks.user1ReadUpToMs
                        }
                        // Also if the partner has sent any message after this message, they have clearly read it
                        val latestPartnerReplyMs = remoteMessages
                            .filter { it.senderId != activeUserId }
                            .maxOfOrNull { it.createdAtMs } ?: 0L

                        val read = (prev?.isRead == true) ||
                            (partnerReadTs >= dto.createdAtMs && partnerReadTs > 0L) ||
                            (latestPartnerReplyMs >= dto.createdAtMs)
                        val delivered = read ||
                            (prev?.isDelivered == true) ||
                            (partnerDelivTs >= dto.createdAtMs && partnerDelivTs > 0L) ||
                            partnerJoined

                        base.copy(
                            isPinned = prev?.isPinned ?: false,
                            isDelivered = delivered,
                            isRead = read
                        )
                    } else {
                        // Incoming message from partner: it is now delivered to us; keep isRead from local DB
                        base.copy(
                            isPinned = prev?.isPinned ?: false,
                            isDelivered = true,
                            isRead = prev?.isRead ?: false
                        )
                    }
                }
                dao.upsertMessages(entities)
            }
        } catch (e: Exception) {
            Log.w("DuoSpaceRepo", "Background sync skipped: ${e.message}")
        }
    }

    fun startRealtimeForSpace(
        scope: CoroutineScope,
        spaceCode: String = DEFAULT_SPACE_CODE,
        onSyncRequested: () -> Unit
    ) {
        supabaseClient.connectRealtime(
            scope = scope,
            spaceCode = spaceCode,
            onRealtimeEvent = onSyncRequested
        )
    }

    fun stopRealtime() {
        supabaseClient.disconnectRealtime()
    }
}

private fun SpaceEntity.toDto() = SupabaseSpaceDto(
    code = code,
    topic = topic,
    creatorId = creatorId,
    creatorName = creatorName,
    partnerId = partnerId,
    partnerName = partnerName,
    createdAtMs = createdAt,
    lastMessagePreview = lastMessagePreview,
    lastUpdatedAtMs = lastUpdatedAt
)

private fun MessageEntity.toDto() = SupabaseMessageDto(
    id = id,
    spaceCode = spaceCode,
    senderId = senderId,
    senderName = senderName,
    content = if (mediaMeta != null) "$messageType::$mediaMeta::$content" else content,
    replyToId = replyToId,
    replyToPreview = replyToPreview,
    reaction = reaction,
    createdAtMs = createdAt,
    isSystemEvent = isSystemEvent
)

private val ENCODED_MEDIA_TYPES = setOf(
    MessageType.VOICE,
    MessageType.DOODLE,
    MessageType.STICKER,
    MessageType.CALL_LOG,
    MessageType.PHOTO,
    MessageType.VIDEO,
    MessageType.FILE,
    MessageType.POLL,
    MessageType.CONTACT,
    MessageType.LOCATION,
    MessageType.COUPLE_CARD
)

private fun SupabaseMessageDto.toEntity(): MessageEntity {
    val parts = content.split("::", limit = 3)
    val isEncodedMedia = parts.size == 3 && parts[0] in ENCODED_MEDIA_TYPES
    return MessageEntity(
        id = id,
        spaceCode = spaceCode,
        senderId = senderId,
        senderName = senderName,
        content = if (isEncodedMedia) parts[2] else content,
        messageType = if (isEncodedMedia) parts[0] else MessageType.TEXT,
        mediaMeta = if (isEncodedMedia) parts[1] else null,
        replyToId = replyToId,
        replyToPreview = replyToPreview,
        reaction = reaction,
        isDelivered = true,
        isRead = false,
        createdAt = createdAtMs,
        isSystemEvent = isSystemEvent
    )
}

