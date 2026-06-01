package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

class CallTranslationService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val CHANNEL_ID = "call_translator_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // በአንድሮይድ ህግ መሰረት ሰርቪሱን ደህንነቱ የተጠበቀ ማድረግ
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("የጥሪ መተርገሚያ")
            .setContentText("ከበስተጀርባ ጥሪዎችን በመከታተል ላይ...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(101, notification)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Translation Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
