package com.wififtp.server.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import com.wififtp.server.MainActivity
import com.wififtp.server.R
import com.wififtp.server.data.ServerConfig
import com.wififtp.server.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class FtpForegroundService : Service() {

    @Inject lateinit var ftpEngine: FtpEngine
    @Inject lateinit var settingsRepo: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_START  = "com.wififtp.server.START"
        const val ACTION_STOP   = "com.wififtp.server.STOP"
        const val CHANNEL_ID    = "ftp_server_channel"
        const val NOTIF_ID      = 1001

        fun startIntent(context: Context) =
            Intent(context, FtpForegroundService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, FtpForegroundService::class.java).apply { action = ACTION_STOP }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startFtp()
            ACTION_STOP  -> stopFtp()
        }
        return START_STICKY
    }

    private fun startFtp() {
        startForeground(
            NOTIF_ID,
            buildNotification("Starting FTP server…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        scope.launch {
            val config = settingsRepo.config.first()
            val result = ftpEngine.start(config)
            val notifText = if (result.isSuccess)
                "Running on port ${config.port}"
            else
                "Failed: ${result.exceptionOrNull()?.message}"
            updateNotification(notifText)
        }
    }

    private fun stopFtp() {
        ftpEngine.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        ftpEngine.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notifications ─────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FTP Server",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "WiFi FTP Server running status"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi FTP Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notifManager = getSystemService(NotificationManager::class.java)
        notifManager.notify(NOTIF_ID, buildNotification(text))
    }
}

// ─── Boot Receiver ────────────────────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Auto-start handled by ViewModel observing DataStore
        }
    }
}
