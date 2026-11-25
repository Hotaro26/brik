package com.hotaro.brik.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hotaro.brik.MainActivity
import com.hotaro.brik.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeBlockService : Service() {

    companion object {
        private const val CHANNEL_ID = "brik_time_block_channel"
        private const val NOTIFICATION_ID = 2746
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_ENDS_AT = "extra_ends_at"

        fun start(context: Context, title: String, endsAt: Long) {
            val intent = Intent(context, TimeBlockService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ENDS_AT, endsAt)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimeBlockService::class.java)
            context.stopService(intent)
        }
    }

    private var countDownTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Active Time Block"
        val endsAt = intent?.getLongExtra(EXTRA_ENDS_AT, System.currentTimeMillis() + 3600000L) ?: (System.currentTimeMillis() + 3600000L)

        startForeground(NOTIFICATION_ID, buildNotification(title, endsAt))
        startCountdown(title, endsAt)

        return START_STICKY
    }

    private fun startCountdown(title: String, endsAt: Long) {
        countDownTimer?.cancel()
        val duration = endsAt - System.currentTimeMillis()
        if (duration <= 0) return

        countDownTimer = object : CountDownTimer(duration, 60000L) {
            override fun onTick(millisUntilFinished: Long) {
                val notification = buildNotification(title, endsAt)
                val nm = getSystemService(NotificationManager::class.java)
                nm?.notify(NOTIFICATION_ID, notification)
            }

            override fun onFinish() {
                stopSelf()
            }
        }.start()
    }

    private fun buildNotification(title: String, endsAt: Long): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMins = ((endsAt - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(endsAt))
        val body = "Active • Ends at $timeStr ($remainingMins min left) • Key required to unlock"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mode_fill0)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Time Block Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and time remaining for active Focus Modes and Routines"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
