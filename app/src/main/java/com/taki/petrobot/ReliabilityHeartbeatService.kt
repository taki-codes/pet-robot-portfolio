package com.taki.petrobot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class ReliabilityHeartbeatService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationManager: NotificationManager
    private var startedAtElapsedRealtime = 0L
    private var heartbeatCount = 0L

    private val heartbeat = object : Runnable {
        override fun run() {
            heartbeatCount += 1
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(SystemClock.elapsedRealtime())
            )
            handler.postDelayed(this, HEARTBEAT_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        heartbeatCount = 1
        startForeground(
            NOTIFICATION_ID,
            buildNotification(startedAtElapsedRealtime),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        handler.postDelayed(heartbeat, HEARTBEAT_INTERVAL_MILLIS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.heartbeat_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.heartbeat_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(nowElapsedRealtime: Long): Notification {
        val elapsed = HeartbeatDurationFormatter.format(
            nowElapsedRealtime - startedAtElapsedRealtime
        )
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.heartbeat_notification_title))
            .setContentText(
                getString(
                    R.string.heartbeat_notification_text,
                    elapsed,
                    heartbeatCount
                )
            )
            .setContentIntent(openAppIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "reliability_heartbeat"
        const val NOTIFICATION_ID = 1001
        const val HEARTBEAT_INTERVAL_MILLIS = 60_000L
    }
}
