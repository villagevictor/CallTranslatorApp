package com.example.calltranslatorapp

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.ArrayList

class CallTranslationService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private var translator: Translator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningLoopActive = false

    // የሃርድዌር የድምፅ ማጣሪያዎች
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            forceSpeakerAndCleanAudio()
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            setupOverlayWindow()
            initializeTranslator()
            forceSpeakerAndCleanAudio()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forceSpeakerAndCleanAudio() {
        try {
            audioManager?.mode = AudioManager.MODE_IN_CALL
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {}
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayTextView = TextView(this).apply {
            text = "🎙️ የጥሪ መተርገሚያ (ልዩ አገልግሎት) ዝግጁ ነው..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(35, 25, 35, 25)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 200
        }

        try {
            windowManager?.addView(overlayTextView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeTranslator() {
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag("am")!!)
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
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        forceSpeakerAndCleanAudio()
                        activateHardwareFilters() // ማይኩ ሲከፈት ማጣሪያዎቹን አንቃ
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
                            translateWithMLKit(originalText)
                        }
                        if (isListeningLoopActive) restartListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partialText = matches[0]
                            overlayTextView?.text = "የሚሰማው ድምፅ: $partialText\n⏳ በመተርጎም ላይ..."
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

    // 🚀 የድምፅ ማስተጋባትን እና ጫጫታን በሃርድዌር ደረጃ መቁረጫ ዘዴ
    private fun activateHardwareFilters() {
        try {
            // በአንድሮይድ ውስጥ የነቃ የኦዲዮ መቅረጫ (Audio Session ID) ካለ ፈልጎ ማጣሪያዎችን ማገናኘት
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(0) // 0 የሲስተሙ መደበኛ የጥሪ ሴሽን ነው
                echoCanceler?.enabled = true
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(0)
                noiseSuppressor?.enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun translateWithMLKit(textToTranslate: String) {
        if (translator == null) {
            overlayTextView?.text = "ENG 🇺🇸: $textToTranslate"
            return
        }

        translator?.translate(textToTranslate)
            ?.addOnSuccessListener { translatedText ->
                overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: $translatedText"
            }
            ?.addOnFailureListener { e ->
                val lower = textToTranslate.lowercase()
                if (lower.contains("hello")) {
                    overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: ሰላም"
                } else if (lower.contains("how are you")) {
                    overlayTextView?.text = "ENG 🇺🇸: $textToTranslate\nAMH 🇪🇹: እንደምን ነህ?"
                } else {
                    overlayTextView?.text = "ENG 🇺🇸: $textToTranslate"
                }
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

    override fun onDestroy() {
        isListeningLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
            speechRecognizer?.destroy()
            translator?.close()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
