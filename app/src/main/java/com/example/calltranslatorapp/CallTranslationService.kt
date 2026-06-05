package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.util.ArrayList

class CallTranslationService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    
    // 🎙️ Phase 1፦ የድምፅ ኢንጂን ረዳቶች
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningLoopActive = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getApplicationContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        startForeground(1, createNotification())
        
        forceSpeakerphoneOn()
        setupOverlayWindow()
        
        isListeningLoopActive = true
        // የድምፅ ኢንጂኑን በዋናው መስመር ላይ ማስነሳት
        startSpeechEngine()
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
            text = "🎙️ የድምፅ ኢንጂን በመነሳት ላይ... ይናገሩ"
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

    private fun startSpeechEngine() {
        if (!isListeningLoopActive) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // ለመጀመርያ ፍተሻ በእንግሊዝኛ አድርገነዋል
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                forceSpeakerphoneOn()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsd: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // ስህተት ቢመጣ እንኳ ድምፅ ማዳመጡን ሳያቋርጥ በየ 1 ሴኮንዱ ራሱን ይቀሰቅሳል (Continuous Listening)
                if (isListeningLoopActive) {
                    mainHandler.postDelayed({ restartListening() }, 1000)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val heardText = matches[0]
                    overlayTextView?.text = "HEARD: $heardText"
                }
                if (isListeningLoopActive) restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partialText = matches[0]
                    overlayTextView?.text = "HEARD (💡 እየተናገሩ ነው...)፦\n$partialText"
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(recognitionIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restartListening() {
        if (!isListeningLoopActive) return
        try {
            speechRecognizer?.destroy()
            startSpeechEngine()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            .setContentTitle("Phase 1: የድምፅ ፍተሻ መስመር")
            .setContentText("ድምፅን ወደ ጽሑፍ ለመቀየር እያዳመጠ ነው...")
            .setSmallIcon(android.R.drawable.star_on)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isListeningLoopActive = false
        try {
            speechRecognizer?.destroy()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
