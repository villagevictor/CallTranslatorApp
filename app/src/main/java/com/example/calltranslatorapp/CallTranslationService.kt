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
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.ArrayList

class CallTranslationService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    private var isListeningLoopActive = false
    private lateinit var recognitionIntent: Intent

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
        
        enAmTranslator.downloadModelIfNeeded()
        amEnTranslator.downloadModelIfNeeded()

        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupOverlayWindow()
        
        isListeningLoopActive = true
        startContinuousListening()
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayTextView = TextView(this).apply {
            text = "🎙️ ጥሪ በመተንተን ላይ... መናገር ይችላሉ"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(40, 25, 40, 25)
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

    private fun startContinuousListening() {
        if (!isListeningLoopActive) return

        speechRecognizer?.destroy()
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("am-ET", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                audioManager?.isSpeakerphoneOn = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsd: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                restartListening()
            }

            override fun onError(error: Int) {
                overlayTextView?.postDelayed({ restartListening() }, 1000)
            }

            override fun onResults(results: Bundle?) {
                processVoiceResults(results)
                restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    overlayTextView?.text = "🎙️ እየተሰማ ነው፦ ${matches[0]}"
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
        if (isListeningLoopActive) {
            try {
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                startContinuousListening()
            }
        }
    }

    private fun processVoiceResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.get(0) ?: ""
        
        if (spokenText.isNotEmpty()) {
            val isEnglish = spokenText.matches(Regex("^[a-zA-Z\\s\\d.,?!'\"-]+$"))
            
            if (isEnglish) {
                enAmTranslator.translate(spokenText).addOnSuccessListener { trans ->
                    overlayTextView?.text = "🇺🇸 EN: $spokenText\n🇪🇹 AM: $trans"
                }.addOnFailureListener {
                    val amFallback = when {
                        spokenText.contains("hello", true) -> "ሰላም"
                        spokenText.contains("how are you", true) -> "እንደምን ነህ?"
                        spokenText.contains("morning", true) -> "እንደምን አደርክ"
                        spokenText.contains("fine", true) -> "ደህና ነኝ"
                        else -> "$spokenText"
                    }
                    overlayTextView?.text = "🇺🇸 EN: $spokenText\n🇪🇹 AM: $amFallback"
                }
            } else {
                amEnTranslator.translate(spokenText).addOnSuccessListener { trans ->
                    overlayTextView?.text = "🇪🇹 AM: $spokenText\n🇺🇸 EN: $trans"
                }.addOnFailureListener {
                    val enFallback = when {
                        spokenText.contains("ሰላም", true) -> "Hello"
                        spokenText.contains("እንደምን ነህ", true) -> "How are you?"
                        spokenText.contains("ደህና ነኝ", true) -> "I am fine"
                        else -> "$spokenText"
                    }
                    overlayTextView?.text = "🇪🇹 AM: $spokenText\n🇺🇸 EN: $enFallback"
                }
            }
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
            .setContentTitle("የጥሪ መተርገሚያ መስመር")
            .setContentText("አፑ ከጀርባ ሆኖ ጥሪውን በጥልቀት እያዳመጠ ነው...")
            .setSmallIcon(android.R.drawable.star_on)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isListeningLoopActive = false
        try {
            speechRecognizer?.destroy()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
            audioManager?.isSpeakerphoneOn = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
