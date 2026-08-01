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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallTranslationService : Service() {

    private val CHANNEL_ID = "CallTranslationChannel"
    private lateinit var webSocketClient: TranslationWebSocketClient
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Default WebSocket Server URL (Phase 4 Orchestration Engine)
        val serverUrl = "wss://echo.websocket.org" 
        webSocketClient = TranslationWebSocketClient(serverUrl)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("imo Translator Pro")
            .setContentText("Real-Time Call Translation is Active...")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)

        // Connect to Cloud AI Stream
        webSocketClient.connectAndStream()

        // Listen for translated audio stream
        scope.launch {
            webSocketClient.incomingTranslatedAudio.collect { audioBytes ->
                // Process audio frames in NetEQ playback loop
            }
        }

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
