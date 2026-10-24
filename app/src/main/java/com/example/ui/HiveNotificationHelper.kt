package com.example.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object HiveNotificationHelper {

    private const val CHANNEL_ID = "hive_private_alerts_v3"
    private const val CALL_CHANNEL_ID = "hive_incoming_calls_v3"
    private const val NOTIFICATION_ID = 8820
    private const val INCOMING_CALL_NOTIFICATION_ID = 8821

    const val ACTION_OPEN_CHAT = "com.example.hive.ACTION_OPEN_CHAT"
    const val ACTION_INCOMING_CALL = "com.example.hive.ACTION_INCOMING_CALL"
    const val ACTION_ANSWER_CALL = "com.example.hive.ACTION_ANSWER_CALL"
    const val ACTION_DECLINE_CALL = "com.example.hive.ACTION_DECLINE_CALL"
    const val EXTRA_CALL_SESSION_ID = "extra_call_session_id"
    const val EXTRA_CALLER_SLOT = "extra_caller_slot"
    const val EXTRA_CALL_MODE = "extra_call_mode"

    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    var isActivelyViewingMainChat: Boolean = false

    @Volatile
    private var lastNotifiedMessageId: String = ""

    @Volatile
    private var lastNotifiedUnreadCount: Int = 0

    @Volatile
    private var activeCallNotificationSessionId: Long = 0L

    private var activeRingtone: Ringtone? = null
    private var isCallRingingVibrating: Boolean = false

    // Callback invoked when the user taps Answer or Decline from the system notification bar or Home Screen
    var onCallNotificationAction: ((action: String, sessionId: Long) -> Unit)? = null

    fun formatPrivateNotificationText(count: Int): String {
        return when {
            count <= 1 -> "1 notification from Hive"
            count == 2 -> "2 notifications from Hive"
            count == 3 -> "3 notifications from Hive"
            else -> "4+ notifications from Hive"
        }
    }

    fun createNotificationChannel(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Hive Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Private notifications in the phone status bar"
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    nm.createNotificationChannel(channel)
                }

                if (nm.getNotificationChannel(CALL_CHANNEL_ID) == null) {
                    val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    val callChannel = NotificationChannel(
                        CALL_CHANNEL_ID,
                        "Hive Incoming Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Incoming voice and video call alerts"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0L, 600L, 400L, 600L, 400L)
                        setSound(ringtoneUri, audioAttrs)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    nm.createNotificationChannel(callChannel)
                }
            }
        }
    }

    fun showPrivateNotification(
        context: Context,
        notificationCount: Int = 1,
        latestMessageId: String = ""
    ) {
        if (notificationCount <= 0) return
        // Do not post a chat notification if the user has the app in the foreground AND is looking at Main Chat
        if (isAppInForeground && isActivelyViewingMainChat) return

        // Avoid re-alerting the exact same unread message state repeatedly on background poll ticks
        if (latestMessageId.isNotBlank() &&
            latestMessageId == lastNotifiedMessageId &&
            notificationCount == lastNotifiedUnreadCount
        ) {
            return
        }
        lastNotifiedMessageId = latestMessageId
        lastNotifiedUnreadCount = notificationCount

        runCatching {
            val appCtx = context.applicationContext
            createNotificationChannel(appCtx)
            val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val intent = Intent(appCtx, MainActivity::class.java).apply {
                action = ACTION_OPEN_CHAT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appCtx,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = formatPrivateNotificationText(notificationCount)

            val builder = NotificationCompat.Builder(appCtx, CHANNEL_ID)
                .setSmallIcon(R.drawable.hive_logo)
                .setContentTitle("Hive")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)

            nm.notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun clearNotifications(context: Context) {
        lastNotifiedUnreadCount = 0
        lastNotifiedMessageId = ""
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
        }
    }

    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        callMode: String,
        callSessionId: Long,
        callerSlot: String = ""
    ) {
        if (callSessionId <= 0L) return
        val isNewRing = activeCallNotificationSessionId != callSessionId
        activeCallNotificationSessionId = callSessionId

        runCatching {
            val appCtx = context.applicationContext
            createNotificationChannel(appCtx)
            val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val fullScreenIntent = Intent(appCtx, MainActivity::class.java).apply {
                action = ACTION_INCOMING_CALL
                putExtra(EXTRA_CALL_SESSION_ID, callSessionId)
                putExtra(EXTRA_CALLER_SLOT, callerSlot)
                putExtra(EXTRA_CALL_MODE, callMode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                appCtx,
                201,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Answer Action -> Launches MainActivity and immediately answers the call
            val answerIntent = Intent(appCtx, MainActivity::class.java).apply {
                action = ACTION_ANSWER_CALL
                putExtra(EXTRA_CALL_SESSION_ID, callSessionId)
                putExtra(EXTRA_CALLER_SLOT, callerSlot)
                putExtra(EXTRA_CALL_MODE, callMode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val answerPendingIntent = PendingIntent.getActivity(
                appCtx,
                202,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decline Action -> Handled in background by HiveCallActionReceiver without requiring app unlock
            val declineIntent = Intent(appCtx, HiveCallActionReceiver::class.java).apply {
                action = ACTION_DECLINE_CALL
                putExtra(EXTRA_CALL_SESSION_ID, callSessionId)
            }
            val declinePendingIntent = PendingIntent.getBroadcast(
                appCtx,
                203,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callLabel = if (callMode.equals("VIDEO", ignoreCase = true)) {
                "Incoming Hive Video Call…"
            } else {
                "Incoming Hive Voice Call…"
            }

            val builder = NotificationCompat.Builder(appCtx, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.hive_logo)
                .setContentTitle(callerName.ifBlank { "Partner" })
                .setContentText(callLabel)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(!isNewRing)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .addAction(R.drawable.hive_logo, "Decline", declinePendingIntent)
                .addAction(R.drawable.hive_logo, "Answer", answerPendingIntent)

            nm.notify(INCOMING_CALL_NOTIFICATION_ID, builder.build())

            if (isNewRing) {
                startIncomingCallRingAndVibration(appCtx)
                // If user is on the Home Screen / outside the app, also launch the full-screen Incoming Call UI directly
                if (!isAppInForeground) {
                    runCatching {
                        appCtx.startActivity(fullScreenIntent)
                    }
                }
            }
        }
    }

    fun cancelIncomingCallNotification(context: Context) {
        activeCallNotificationSessionId = 0L
        stopIncomingCallRingAndVibration(context)
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(INCOMING_CALL_NOTIFICATION_ID)
        }
    }

    fun startIncomingCallRingAndVibration(context: Context) {
        runCatching {
            val appCtx = context.applicationContext
            if (!isCallRingingVibrating) {
                isCallRingingVibrating = true
                val vibrator = getVibrator(appCtx)
                val pattern = longArrayOf(0L, 650L, 450L, 650L, 450L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
            if (activeRingtone == null || activeRingtone?.isPlaying != true) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val rt = RingtoneManager.getRingtone(appCtx, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rt?.isLooping = true
                }
                rt?.play()
                activeRingtone = rt
            }
        }
    }

    fun stopIncomingCallRingAndVibration(context: Context) {
        runCatching {
            if (isCallRingingVibrating) {
                isCallRingingVibrating = false
                getVibrator(context.applicationContext)?.cancel()
            }
            activeRingtone?.let {
                if (it.isPlaying) it.stop()
            }
            activeRingtone = null
        }
    }

    fun triggerBuzzVibration(context: Context) {
        runCatching {
            val vibrator = getVibrator(context) ?: return
            val pattern = longArrayOf(0, 220, 90, 280, 90, 420)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    fun triggerShortHaptic(context: Context) {
        runCatching {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60L)
            }
        }
    }

    fun cancelVibration(context: Context) {
        runCatching {
            getVibrator(context)?.cancel()
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

class HiveCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val sessionId = intent.getLongExtra(HiveNotificationHelper.EXTRA_CALL_SESSION_ID, 0L)
        when (action) {
            HiveNotificationHelper.ACTION_DECLINE_CALL -> {
                HiveNotificationHelper.cancelIncomingCallNotification(context)
                com.example.data.remote.HiveLivePeerRelay.declineOrEndCallFromNotification(sessionId)
                HiveNotificationHelper.onCallNotificationAction?.invoke(action, sessionId)
            }
            HiveNotificationHelper.ACTION_ANSWER_CALL -> {
                HiveNotificationHelper.stopIncomingCallRingAndVibration(context)
                HiveNotificationHelper.onCallNotificationAction?.invoke(action, sessionId)
            }
        }
    }
}
