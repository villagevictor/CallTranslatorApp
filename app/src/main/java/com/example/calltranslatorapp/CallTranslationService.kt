package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.media.AudioManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat

class CallTranslationService : Service() {

    private val CHANNEL_ID = "call_translator_channel"
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // ሳምሰንግ ሲስተም እንዳይዘጋው ቋሚ ማሳወቂያ መስቀል
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("የጥሪ መተርገሚያ")
            .setContentText("የጥሪ ድምፅ ጥራት መስመር ተስተካክሏል...")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(101, notification)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // ሚስጥሩ እዚህ ጋር ነው! የድምፅ ረብሻውን ለማጥፋት የስልኩን Audio Mode ወደ VOICE_COMMUNICATION መቀየር
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    audioManager.isSpeakerphoneOn = true // ድምፅ በንፅህና እንዲሰማ ስፒከር ማገዝ
                    Log.d("CallTranslator", "የኦዲዮ ድምፅ ረብሻ ተወግዷል")
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // ስልኩ ሲዘጋ ወደ መደበኛ ሁነታ መመለስ
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Translation Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "የጥሪ መተርገሚያ ቻናል"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        audioManager.mode = AudioManager.MODE_NORMAL
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
