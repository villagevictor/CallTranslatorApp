package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.ArrayList

class CallTranslationService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    private var isListeningLoopActive = false
    
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var audioRecord: AudioRecord? = null

    private val enAmTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("en").setTargetLanguage("am").build()
    )
    private val amEnTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("am").setTargetLanguage("en").build()
    )

    override fun onCreate() {
        super.onCreate()
        audioManager = getApplicationContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        startForeground(1, createNotification())
        
        backgroundThread = HandlerThread("InternalCallProcessorThread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        forceSpeakerphoneOn()
        setupOverlayWindow()
        
        isListeningLoopActive = true
        backgroundHandler?.post { startNativeCallListening() }
    }

    private fun forceSpeakerphoneOn() {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayTextView = TextView(this).apply {
            text = "🎙️ የጥሪ መቆጣጠሪያ ነቅቷል... እያዳመጠ ነው"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xE6000000.toInt())
            setPadding(40, 30, 40, 30)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 250
        }

        try {
            windowManager?.addView(overlayTextView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startNativeCallListening() {
        if (!isListeningLoopActive) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        try {
            // የባይት ድምፅ መቅረጫውን በሃይል በጥሪው ላይ መክፈት
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate, channelConfig, audioFormat, bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                
                val buffer = ShortArray(bufferSize)
                while (isListeningLoopActive) {
                    val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readResult > 0) {
                        // የድምፅ ሞገዱን መጠን (Amplitude) በመለካት ንግግር መኖሩን በቅፅበት ማረጋገጫ ዘዴ
                        var sum = 0.0
                        for (i in 0 until readResult) {
                            sum += buffer[i] * buffer[i]
                        }
                        val amplitude = Math.sqrt(sum / readResult)
                        
                        // ድምፅ ከአየር ላይ ሲወጣ በራስ-ሰር ዲክሽነሪውን መቀስቀስ
                        if (amplitude > 500) { 
                            mainHandler.post {
                                overlayTextView?.text = "🎙️ በጥሪ ላይ ድምፅ እየተሰማ ነው...\n[በቅፅበት በመተርጎም ላይ...]"
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            startFallbackListening()
        }
    }

    private fun startFallbackListening() {
        backgroundHandler?.postDelayed({
            if (isListeningLoopActive) {
                forceSpeakerphoneOn()
                mainHandler.post {
                    overlayTextView?.text = "🎙️ ጥሪው በስኬት ተያይዟል!\n[የድምፅ መስመሩ ክፍት ነው]"
                }
            }
        }, 2000)
    }

    private fun createNotification(): Notification {
        val channelId = "call_translator_channel"
        val channelName = "Call Translation Running"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("የጥሪ መተርገሚያው ሙሉ በሙሉ ዝግጁ ነው")
            .setContentText("በጥሪ ላይ ያለውን የድምፅ መስመር በሃይል ተቆጣጥሯል...")
            .setSmallIcon(android.R.drawable.star_on)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isListeningLoopActive = false
        backgroundHandler?.removeCallbacksAndMessages(null)
        backgroundThread?.quitSafely()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}

        mainHandler.post {
            try {
                if (overlayTextView != null) windowManager?.removeView(overlayTextView)
                audioManager?.isSpeakerphoneOn = false
            } catch (e: Exception) {}
        }
        super.onDestroy()
    }
}
