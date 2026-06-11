package com.example.calltranslatorapp

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
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

class CallTranslationService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private var translator: Translator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningLoopActive = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            showPersistentNotification() // 🚀 ጠንካራ ኖቲፊኬሽን እዚህ ይነሳል
            setupOverlayWindow()
            initializeTranslator()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPersistentNotification() {
        val channelId = "call_translator_notif_channel"
        val channelName = "Call Translator Active Engine"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "በትርጉም ጊዜ እንዳይዘጋ የሚከላከል"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Call Translator Pro")
            .setContentText("አፑ ንግግርን ለመተርጎም በጀርባ ዝግጁ ነው...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayTextView = TextView(this).apply {
            text = "🎙️ Call Translator: ለመስማት ዝግጁ ነው..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xDD6200EE.toInt()) // 🎨 ይበልጥ ማራኪ ፐርፕል ከለር
            setPadding(40, 30, 40, 30)
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
            y = 150
        }
        try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
    }

    private fun initializeTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag("am")!!)
            .build()
        translator = Translation.getClient(options)
        isListeningLoopActive = true
        startSpeechEngine()
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
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsd: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (isListeningLoopActive) mainHandler.postDelayed({ restartListening() }, 1000)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            processAndTranslate(matches[0])
                        }
                        if (isListeningLoopActive) restartListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            overlayTextView?.text = "የሚሰማው ቃል: ${matches[0]}"
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                if (isListeningLoopActive) mainHandler.postDelayed({ restartListening() }, 1500)
            }
        }
    }

    // 🚀 መዝገበ-ቃላቱን እና ትርጉሙን 100% የማገናኘት ዋና ዘዴ
    private fun processAndTranslate(text: String) {
        val cleanText = text.lowercase().trim()
        
        // መጀመሪያ ከውስጥ ዲክሽነሪ ፈልግ
        var localTranslation = ""
        if (cleanText.contains("hello")) localTranslation = getString(resources.getIdentifier("dic_hello", "string", packageName))
        else if (cleanText.contains("how are you")) localTranslation = getString(resources.getIdentifier("dic_how_are_you", "string", packageName))
        else if (cleanText.contains("fine")) localTranslation = getString(resources.getIdentifier("dic_fine", "string", packageName))
        else if (cleanText.contains("good")) localTranslation = getString(resources.getIdentifier("dic_good", "string", packageName))
        else if (cleanText.contains("thank you")) localTranslation = getString(resources.getIdentifier("dic_thank_you", "string", packageName))
        else if (cleanText.contains("where are you")) localTranslation = getString(resources.getIdentifier("dic_where_are_you", "string", packageName))

        if (localTranslation.isNotEmpty()) {
            overlayTextView?.text = "ENG: $text\nAMH (ዲክሽነሪ): $localTranslation"
            return
        }

        // በዲክሽነሪው ውስጥ ከሌለ በ AI ማሽን ተርጉም
        translator?.translate(text)
            ?.addOnSuccessListener { translatedText ->
                overlayTextView?.text = "ENG: $text\nAMH (AI): $translatedText"
            }
            ?.addOnFailureListener {
                overlayTextView?.text = "ENG: $text\n[ማሳሰቢያ: ትርጉም አልተገኘም]"
            }
    }

    private fun restartListening() {
        if (!isListeningLoopActive) return
        try { speechRecognizer?.destroy(); startSpeechEngine() } catch (e: Exception) {}
    }

    override fun onDestroy() {
        isListeningLoopActive = false
        try { speechRecognizer?.destroy(); translator?.close() } catch (e: Exception) {}
        super.onDestroy()
    }
}
