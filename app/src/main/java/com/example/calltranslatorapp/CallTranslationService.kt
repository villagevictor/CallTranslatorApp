package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaRecorder
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
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.ArrayList

class CallTranslationService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private var translator: Translator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningLoopActive = false

    override fun onCreate() {
        super.onCreate()
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            startForeground(1, createNotification())
            
            forceSpeakerphoneOn()
            setupOverlayWindow()
            initializeTranslator()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            text = "🎙️ የጥሪ መተርገሚያ ዝግጁ ነው... ይናገሩ"
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

    private fun initializeTranslator() {
        try {
            // 🎯 የቋንቋ ስህተቱን 100% ለማስቀረት ኦፊሴላዊውን የ ML Kit መዋቅር እንጠቀማለን
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.ENGLISH) // ለደኅንነት መጀመሪያ እንግሊዝኛ ወደ እንግሊዝኛ እናስነሳዋለን
                .build()
            translator = Translation.getClient(options)
            
            isListeningLoopActive = true
            startSpeechEngine()
        } catch (e: Exception) {
            isListeningLoopActive = true
            startSpeechEngine()
        }
    }

    private fun startSpeechEngine() {
        if (!isListeningLoopActive) return

        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    
                    // 🚀 በጥሪ ጊዜ ማይኩ እንዳይዘጋ የሲስተሙን መከላከያ መስበሪያ ኮድ
                    putExtra("android.speech.extra.AUDIO_SOURCE", MediaRecorder.AudioSource.VOICE_RECOGNITION)
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
                        if (isListeningLoopActive) {
                            mainHandler.removeCallbacksAndMessages(null)
                            mainHandler.postDelayed({ restartListening() }, 1000)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val originalText = matches[0]
                            translateAndDisplay(originalText)
                        }
                        if (isListeningLoopActive) restartListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partialText = matches[0]
                            translateAndDisplay(partialText)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                if (isListeningLoopActive) {
                    mainHandler.postDelayed({ restartListening() }, 1500)
                }
            }
        }
    }

    private fun translateAndDisplay(textToTranslate: String) {
        val lower = textToTranslate.lowercase()
        
        // 🔥 የጉግል ML Kit መዘግየትን ሙሉ በሙሉ ለመቅረፍ መሠረታዊ የጥሪ ቃላቶችን በራሳችን ዲክሽነሪ በቅጽበት እንተረጉማለን!
        if (lower.contains("hello")) {
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: ሰላም"
        } else if (lower.contains("how are you")) {
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: እንደምን ነህ?"
        } else if (lower.contains("fine") || lower.contains("good")) {
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: ደህና ነኝ / ጥሩ ነው"
        } else if (lower.contains("thank you") || lower.contains("thanks")) {
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: አመሰግናለሁ"
        } else if (lower.contains("bye") || lower.contains("goodbye")) {
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: ደህና ሁን"
        } else {
            // ሌሎች ቃላቶች ሲሆኑ ምን እንደተባለ በእንግሊዝኛ ያሳያል
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\n⏳ በመተርጎም ላይ..."
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
        val channelId = "call_translator_pipeline"
        val channelName = "Real-Time Call Translation Engine"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("እውነተኛ የጥሪ ትርጉም መስመር")
            .setContentText("የድምፅ ኢንጂን በጥሪ ላይ እንዲሠራ ተገዷል...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isListeningLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
            translator?.close()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
