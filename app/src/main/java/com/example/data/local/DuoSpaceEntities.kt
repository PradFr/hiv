package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

object MessageType {
    const val TEXT = "TEXT"
    const val VOICE = "VOICE"
    const val DOODLE = "DOODLE"
    const val STICKER = "STICKER"
    const val CALL_LOG = "CALL_LOG"
    const val PHOTO = "PHOTO"
    const val VIDEO = "VIDEO"
    const val FILE = "FILE"
    const val POLL = "POLL"
    const val CONTACT = "CONTACT"
    const val LOCATION = "LOCATION"
    const val COUPLE_CARD = "COUPLE_CARD"
}

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val code: String,
    val topic: String,
    val creatorId: String,
    val creatorName: String,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val pinnedMessageId: String? = null,
    val pinnedNote: String? = null,
    val screenDoodleData: String? = null,
    val extraBottomSpaceDp: Int = 0,
    val autoRecordCalls: Boolean = false,
    val user1Status: String = "Online",
    val user2Status: String = "Online",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val spaceCode: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val messageType: String = MessageType.TEXT,
    val mediaMeta: String? = null,
    val replyToId: String? = null,
    val replyToPreview: String? = null,
    val reaction: String? = null,
    val isPinned: Boolean = false,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isSystemEvent: Boolean = false
)

@Dao
interface DuoSpaceDao {
    @Query("SELECT * FROM spaces WHERE code = :code LIMIT 1")
    fun observeSpace(code: String): Flow<SpaceEntity?>

    @Query("SELECT * FROM spaces WHERE code = :code LIMIT 1")
    suspend fun getSpaceByCode(code: String): SpaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpace(space: SpaceEntity)

    @Query("SELECT * FROM messages WHERE spaceCode = :spaceCode ORDER BY createdAt ASC")
    fun observeMessagesForSpace(spaceCode: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE spaceCode = :spaceCode ORDER BY createdAt ASC")
    suspend fun getMessagesForSpaceList(spaceCode: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE spaceCode = :spaceCode")
    suspend fun getMessageCount(spaceCode: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, reaction: String?)

    @Query("UPDATE messages SET content = :content, mediaMeta = :mediaMeta WHERE id = :messageId")
    suspend fun updateMessageContentAndMeta(messageId: String, content: String, mediaMeta: String?)

    @Query("UPDATE messages SET isPinned = 0 WHERE spaceCode = :spaceCode")
    suspend fun clearAllPinnedInSpace(spaceCode: String)

    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :messageId")
    suspend fun setMessagePinned(messageId: String, isPinned: Boolean)

    @Query("UPDATE messages SET isDelivered = 1 WHERE id = :messageId")
    suspend fun markMessageDelivered(messageId: String)

    @Query("UPDATE messages SET isDelivered = 1 WHERE spaceCode = :spaceCode AND senderId = :senderId AND createdAt <= :upToTimestampMs AND isDelivered = 0")
    suspend fun markOwnMessagesDeliveredUpTo(spaceCode: String, senderId: String, upToTimestampMs: Long)

    @Query("UPDATE messages SET isDelivered = 1, isRead = 1 WHERE spaceCode = :spaceCode AND senderId = :senderId AND createdAt <= :upToTimestampMs AND (isRead = 0 OR isDelivered = 0)")
    suspend fun markOwnMessagesReadUpTo(spaceCode: String, senderId: String, upToTimestampMs: Long)

    @Query("UPDATE messages SET isDelivered = 1, isRead = 1 WHERE spaceCode = :spaceCode AND senderId != :readerId AND (isRead = 0 OR isDelivered = 0)")
    suspend fun markMessagesRead(spaceCode: String, readerId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE spaceCode = :spaceCode AND messageType = 'DOODLE'")
    suspend fun deleteDoodleSpacersForSpace(spaceCode: String)

    @Query("DELETE FROM messages WHERE spaceCode = :spaceCode")
    suspend fun deleteMessagesForSpace(spaceCode: String)
}

@Database(
    entities = [SpaceEntity::class, MessageEntity::class],
    version = 7,
    exportSchema = false
)
abstract class DuoSpaceDatabase : RoomDatabase() {
    abstract fun duoSpaceDao(): DuoSpaceDao

    companion object {
        @Volatile
        private var INSTANCE: DuoSpaceDatabase? = null

        fun getInstance(context: Context): DuoSpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val sandboxDbDir = java.io.File(context.applicationContext.noBackupFilesDir, "sandboxed_db").apply {
                    if (!exists()) mkdirs()
                    val noMedia = java.io.File(this, ".nomedia")
                    if (!noMedia.exists()) runCatching { noMedia.createNewFile() }
                }
                val dbFile = java.io.File(sandboxDbDir, "duospace_sandboxed.db")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DuoSpaceDatabase::class.java,
                    dbFile.absolutePath
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
