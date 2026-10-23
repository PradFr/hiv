package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class SupabaseSpaceDto(
    @Json(name = "code") val code: String,
    @Json(name = "topic") val topic: String,
    @Json(name = "creator_id") val creatorId: String,
    @Json(name = "creator_name") val creatorName: String,
    @Json(name = "partner_id") val partnerId: String? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
    @Json(name = "created_at_ms") val createdAtMs: Long,
    @Json(name = "last_message_preview") val lastMessagePreview: String? = null,
    @Json(name = "last_updated_at_ms") val lastUpdatedAtMs: Long
)

data class SupabaseSpacePatchDto(
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "creator_name") val creatorName: String? = null,
    @Json(name = "partner_id") val partnerId: String? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
    @Json(name = "last_message_preview") val lastMessagePreview: String? = null,
    @Json(name = "last_updated_at_ms") val lastUpdatedAtMs: Long
)

data class SupabaseMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "space_code") val spaceCode: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "content") val content: String,
    @Json(name = "reply_to_id") val replyToId: String? = null,
    @Json(name = "reply_to_preview") val replyToPreview: String? = null,
    @Json(name = "reaction") val reaction: String? = null,
    @Json(name = "created_at_ms") val createdAtMs: Long,
    @Json(name = "is_system_event") val isSystemEvent: Boolean = false
)

data class SupabaseReactionPatchDto(
    @Json(name = "reaction") val reaction: String?
)

interface SupabaseRestService {
    @GET("rest/v1/spaces")
    suspend fun getSpaceByCode(
        @Query("code") codeFilter: String,
        @Query("select") select: String = "*"
    ): List<SupabaseSpaceDto>

    @GET("rest/v1/spaces")
    suspend fun getAllSpaces(
        @Query("select") select: String = "*",
        @Query("order") order: String = "last_updated_at_ms.desc",
        @Query("limit") limit: Int = 50
    ): List<SupabaseSpaceDto>

    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/spaces")
    suspend fun upsertSpace(
        @Body space: SupabaseSpaceDto
    ): List<SupabaseSpaceDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/spaces")
    suspend fun updateSpace(
        @Query("code") codeFilter: String,
        @Body patch: SupabaseSpacePatchDto
    ): List<SupabaseSpaceDto>

    @GET("rest/v1/messages")
    suspend fun getMessagesForSpace(
        @Query("space_code") spaceCodeFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at_ms.asc"
    ): List<SupabaseMessageDto>

    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/messages")
    suspend fun insertMessage(
        @Body message: SupabaseMessageDto
    ): List<SupabaseMessageDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/messages")
    suspend fun updateMessageReaction(
        @Query("id") idFilter: String,
        @Body patch: SupabaseReactionPatchDto
    ): List<SupabaseMessageDto>
}

class SupabaseClientManager {
    val rawUrl: String = listOf(
        BuildConfig.SUPABASE_URL.trim(),
        BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim()
    ).firstOrNull {
        it.startsWith("https://") && it != "YOUR_SUPABASE_PROJECT_URL"
    }?.removeSuffix("/") ?: ""

    val rawAnonKey: String = listOf(
        BuildConfig.SUPABASE_ANON_KEY.trim(),
        BuildConfig.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY.trim()
    ).firstOrNull {
        it.isNotEmpty() &&
            it != "YOUR_SUPABASE_ANON_KEY" &&
            it != "YOUR_SUPABASE_PUBLISHABLE_KEY"
    } ?: ""

    val isConfigured: Boolean = rawUrl.startsWith("https://") && rawAnonKey.isNotEmpty()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val reqBuilder = original.newBuilder()
                    .header("apikey", rawAnonKey)
                    .header("Content-Type", "application/json")
                // Legacy anon keys are JWTs (start with eyJ) and use Authorization: Bearer.
                // Newer sb_publishable_ keys authenticate via the apikey header at the Supabase gateway.
                if (rawAnonKey.startsWith("eyJ")) {
                    reqBuilder.header("Authorization", "Bearer $rawAnonKey")
                }
                chain.proceed(reqBuilder.build())
            })
            .build()
    }

    val restService: SupabaseRestService? by lazy {
        if (!isConfigured) null
        else {
            try {
                Retrofit.Builder()
                    .baseUrl("$rawUrl/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseRestService::class.java)
            } catch (e: Exception) {
                Log.e("SupabaseClient", "Invalid SUPABASE_URL: ${e.message}")
                null
            }
        }
    }

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null

    fun connectRealtime(
        scope: CoroutineScope,
        spaceCode: String,
        onRealtimeEvent: () -> Unit
    ) {
        if (!isConfigured) return
        disconnectRealtime()

        val wsBaseUrl = rawUrl.replaceFirst("https://", "wss://")
        val wsUrl = "$wsBaseUrl/realtime/v1/websocket?apikey=$rawAnonKey&vsn=1.0.0"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val joinPayload = """
                    {
                      "topic": "realtime:public:messages:space_code=eq.$spaceCode",
                      "event": "phx_join",
                      "payload": {
                        "config": {
                          "postgres_changes": [
                            {"event": "*", "schema": "public", "table": "messages", "filter": "space_code=eq.$spaceCode"},
                            {"event": "*", "schema": "public", "table": "spaces", "filter": "code=eq.$spaceCode"}
                          ]
                        }
                      },
                      "ref": "1"
                    }
                """.trimIndent()
                webSocket.send(joinPayload)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("postgres_changes") || text.contains("INSERT") || text.contains("UPDATE")) {
                    onRealtimeEvent()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("SupabaseRealtime", "Realtime socket closed/fallback to polling: ${t.message}")
            }
        })

        heartbeatJob = scope.launch(Dispatchers.IO) {
            var refCounter = 2
            while (isActive) {
                delay(25_000L)
                webSocket?.send("""{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${refCounter++}"}""")
            }
        }
    }

    fun disconnectRealtime() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Leaving room")
        webSocket = null
    }

    companion object {
        val SUPABASE_SQL_SCHEMA = """
            -- Run this once in your Supabase SQL Editor to enable DuoSpace Cloud Sync:
            CREATE TABLE IF NOT EXISTS public.spaces (
                code TEXT PRIMARY KEY,
                topic TEXT NOT NULL,
                creator_id TEXT NOT NULL,
                creator_name TEXT NOT NULL,
                partner_id TEXT,
                partner_name TEXT,
                created_at_ms BIGINT NOT NULL,
                last_message_preview TEXT,
                last_updated_at_ms BIGINT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS public.messages (
                id TEXT PRIMARY KEY,
                space_code TEXT NOT NULL REFERENCES public.spaces(code) ON DELETE CASCADE,
                sender_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                content TEXT NOT NULL,
                reply_to_id TEXT,
                reply_to_preview TEXT,
                reaction TEXT,
                created_at_ms BIGINT NOT NULL,
                is_system_event BOOLEAN NOT NULL DEFAULT FALSE
            );

            ALTER TABLE public.spaces ENABLE ROW LEVEL SECURITY;
            ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

            CREATE POLICY "Allow anon access to spaces" ON public.spaces
                FOR ALL USING (true) WITH CHECK (true);

            CREATE POLICY "Allow anon access to messages" ON public.messages
                FOR ALL USING (true) WITH CHECK (true);

            ALTER PUBLICATION supabase_realtime ADD TABLE public.spaces, public.messages;

            -- Create the temporary Post-Office Media Relay bucket (auto-emptied after partner downloads):
            INSERT INTO storage.buckets (id, name, public)
            VALUES ('duospace-relay', 'duospace-relay', false)
            ON CONFLICT (id) DO NOTHING;
        """.trimIndent()
    }

    /**
     * Post-Office Media Relay:
     * 1. Uploads an already-encrypted .venc file to the temporary Supabase Storage bucket (`duospace-relay`).
     * 2. Downloads a pending .venc file into the partner's private offline sandbox (`noBackupFilesDir`).
     * 3. Immediately deletes the .venc file from the Supabase Storage bucket once downloaded so cloud usage stays ~0 MB.
     */
    fun uploadEncryptedRelayBlob(blobFileName: String, encryptedBytes: ByteArray): Boolean {
        if (!isConfigured || blobFileName.isBlank() || encryptedBytes.isEmpty()) return false
        return try {
            val url = "$rawUrl/storage/v1/object/duospace-relay/$blobFileName"
            val mediaType = okhttp3.MediaType.Companion.run { "application/octet-stream".toMediaTypeOrNull() }
            val body = okhttp3.RequestBody.Companion.run { encryptedBytes.toRequestBody(mediaType) }
            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .header("apikey", rawAnonKey)
                .header("x-upsert", "true")
            if (rawAnonKey.startsWith("eyJ")) {
                requestBuilder.header("Authorization", "Bearer $rawAnonKey")
            }
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("SupabaseRelay", "Relay upload skipped/offline: ${e.message}")
            false
        }
    }

    fun downloadAndDeleteRelayBlob(blobFileName: String): ByteArray? {
        if (!isConfigured || blobFileName.isBlank()) return null
        return try {
            val url = "$rawUrl/storage/v1/object/duospace-relay/$blobFileName"
            val getBuilder = Request.Builder()
                .url(url)
                .get()
                .header("apikey", rawAnonKey)
            if (rawAnonKey.startsWith("eyJ")) {
                getBuilder.header("Authorization", "Bearer $rawAnonKey")
            }
            val downloadedBytes = okHttpClient.newCall(getBuilder.build()).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }

            if (downloadedBytes != null && downloadedBytes.isNotEmpty()) {
                // Immediately delete from cloud relay locker so the 1 GB free tier stays near 0 MB
                deleteRelayBlobFromCloud(blobFileName)
            }
            downloadedBytes
        } catch (e: Exception) {
            Log.w("SupabaseRelay", "Relay download skipped/offline: ${e.message}")
            null
        }
    }

    fun deleteRelayBlobFromCloud(blobFileName: String): Boolean {
        if (!isConfigured || blobFileName.isBlank()) return false
        return try {
            val url = "$rawUrl/storage/v1/object/duospace-relay/$blobFileName"
            val deleteBuilder = Request.Builder()
                .url(url)
                .delete()
                .header("apikey", rawAnonKey)
            if (rawAnonKey.startsWith("eyJ")) {
                deleteBuilder.header("Authorization", "Bearer $rawAnonKey")
            }
            okHttpClient.newCall(deleteBuilder.build()).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }
}

object DuoCloudRelaySync {
    private val client by lazy { SupabaseClientManager() }
    private val ioScope = CoroutineScope(Dispatchers.IO)

    val isCloudConfigured: Boolean
        get() = client.isConfigured

    /**
     * Queues an encrypted .venc blob to the temporary cloud locker in the background.
     */
    fun enqueueRelayUpload(blobFileName: String, encryptedBytes: ByteArray) {
        if (!client.isConfigured) return
        ioScope.launch {
            client.uploadEncryptedRelayBlob(blobFileName, encryptedBytes)
        }
    }

    /**
     * Checks if a .venc file is missing from the local offline sandbox (`targetDir`).
     * If missing and cloud relay is configured, downloads it into `targetDir` for permanent
     * offline access and immediately deletes it from the cloud bucket.
     */
    fun ensureLocalSandboxFileOrPullFromRelay(
        targetDir: java.io.File,
        blobFileName: String?,
        onDownloaded: (() -> Unit)? = null
    ) {
        if (blobFileName.isNullOrBlank()) return
        val localFile = java.io.File(targetDir, blobFileName)
        if (localFile.exists() && localFile.length() > 0L) return
        if (!client.isConfigured) return

        ioScope.launch {
            val bytes = client.downloadAndDeleteRelayBlob(blobFileName)
            if (bytes != null && bytes.isNotEmpty()) {
                runCatching {
                    localFile.writeBytes(bytes)
                }
                onDownloaded?.invoke()
            }
        }
    }
}

