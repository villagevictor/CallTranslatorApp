package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.calltranslatorapp.network.TranslationWebSocketClient

class CallTranslationService : Service() {

    private val CHANNEL_ID = "CallTranslationChannel"
    private lateinit var webSocketClient: TranslationWebSocketClient

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        webSocketClient = TranslationWebSocketClient("wss://echo.websocket.org")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Real-Time Call Translator")
            .setContentText("Live Translation Service Running")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .build()

        startForeground(1, notification)
        webSocketClient.connect()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Translation Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
