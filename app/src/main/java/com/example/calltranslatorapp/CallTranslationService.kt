package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CallTranslationService : Service() {

    private val CHANNEL_ID = "call_translator_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // ሳምሰንግ One UI ላይ በግልጽ የሚታይ አዶ መርጠናል።
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("የጥሪ መተርገሚያ")
            .setContentText("ከበስተጀርባ ጥሪዎችን በመከታተል ላይ...")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call) // የጥሪ ምልክት አዶ
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Translation Service",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "የጥሪ መተርገሚያ የበስተጀርባ ማሳወቂያ ቻናል"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
