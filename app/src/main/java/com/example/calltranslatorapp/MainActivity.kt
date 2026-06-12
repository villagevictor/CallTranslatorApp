package com.example.calltranslatorapp

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var translator: Translator? = null
    private var isModelDownloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. የኦቨርሌይ ፍቃድ መጠየቅ
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // 2. የኖቲፊኬሽን ፍቃድ መጠየቅ
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
            }
        }

        // 3. የ AI መዝገበ-ቃላት (ML Kit) ሞዴልን ማዘጋጀት
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag("am")!!)
            .build()
        translator = Translation.getClient(options)

        // ሙሉውን ዲክሽነሪ በአንድ ጊዜ በጀርባ ማውረድ (ለመጀመሪያ ጊዜ ኢንተርኔት ይፈልጋል)
        val conditions = DownloadConditions.Builder().build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                isModelDownloaded = true
                Toast.makeText(this, "📚 ሙሉ የአማርኛ ዲክሽነሪ ዝግጁ ሆኗል (Offline)!", Toast.LENGTH_LONG).show()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, "ዲክሽነሪውን ለማውረድ ኢንተርኔት ያብሩ: ${e.message}", Toast.LENGTH_LONG).show()
            }

        val btnId = resources.getIdentifier("btn_enable_service", "id", packageName)
        findViewById<Button>(btnId).setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                startTranslationEngine()
            } else {
                requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 102)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 102 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTranslationEngine()
        }
    }

    private fun startTranslationEngine() {
        if (isListening) return
        isListening = true
        
        showNotification()
        setupOverlay()
        startListeningLoop()
        
        Toast.makeText(this, "የትርጉም ሞተር ተነስቷል!", Toast.LENGTH_SHORT).show()
    }

    private fun showNotification() {
        val channelId = "call_trans_fast"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Fast Translation", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("🎙️ Call Translator Pro")
            .setContentText("ከመስመር ውጭ ሙሉ መዝገበ-ቃላት ገብቷል...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        manager.notify(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayTextView = TextView(this).apply {
            text = "🎙️ Call Translator: እያዳመጥኩ ነው..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xEE6200EE.toInt())
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
            y = 150
        }
        try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
    }

    private fun startListeningLoop() {
        if (!isListening) return
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
                        if (isListening) mainHandler.postDelayed({ restartListening() }, 400)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            translateWithAI(matches[0])
                        }
                        if (isListening) restartListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            overlayTextView?.text = "የሚሰማው: ${matches[0]}"
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                if (isListening) mainHandler.postDelayed({ restartListening() }, 1000)
            }
        }
    }

    // 🚀 በሺዎች የሚቆጠሩ ቃላትን በአንድ ጊዜ የሚተረጉመው የ AI ሞተር
    private fun translateWithAI(text: String) {
        if (translator != null && isModelDownloaded) {
            translator?.translate(text)
                ?.addOnSuccessListener { translatedText ->
                    overlayTextView?.text = "ENG 🇺🇸: $text\nAMH 🇪🇹: $translatedText"
                }
                ?.addOnFailureListener {
                    overlayTextView?.text = "ENG 🇺🇸: $text\nAMH: [በትርጉም ላይ ስህተት]"
                }
        } else {
            // ሞዴሉ ገና ካልወረደ መሰረታዊ የውስጥ ቃላትን መጠቀም
            val clean = text.lowercase().trim()
            var local = "እየተተረጎመ ነው..."
            if (clean.contains("hello")) local = "ሰላም"
            else if (clean.contains("good morning")) local = "እንደምን አደርክ/ሽ"
            overlayTextView?.text = "ENG: $text\nAMH (Base): $local"
        }
    }

    private fun restartListening() {
        if (!isListening) return
        try { speechRecognizer?.destroy(); startListeningLoop() } catch (e: Exception) {}
    }

    override fun onDestroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
            translator?.close()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
