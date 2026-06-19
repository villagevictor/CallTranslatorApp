package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Bundle as AndroidBundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStreamingActive = false
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    // 🎯 የትርጉም መዝገበ-ቃላት
    private val translationDictionary = LinkedHashMap<String, String>().apply {
        put("challenge", "ውድድር / ፈተና 🏆")
        put("winner", "አሸናፊ 🎉")
        put("subscribe", "ሰብስክራይብ ያድርጉ (ይከተሉ)")
        put("amazing", "አስደናቂ! ✨")
        put("money", "ገንዘብ / ዶላር 💵")
        put("friend", "ጓደኛ 🤝")
        put("video", "ቪዲዮ 🎬")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(60, 60, 60, 60)
        }

        val logoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#3B82F6"))
                cornerRadius = 40f
            }
            background = drawable
            layoutParams = LinearLayout.LayoutParams(220, 220).apply { setMargins(0, 0, 0, 50) }
        }
        val logoText = TextView(this).apply {
            text = "🇪🇹"
            textSize = 36f
            gravity = Gravity.CENTER
        }
        logoLayout.addView(logoText)
        mainLayout.addView(logoLayout)

        val titleView = TextView(this).apply {
            text = "📺 Ethio Live Translate\n(Google Speech Engine V91)"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleView)

        val btnStartTranslator = Button(this).apply {
            text = "🚀 የቀጥታ ትርጉም አስነሳ"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 45, 50, 45)
            val btnDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
            background = btnDrawable
        }

        btnStartTranslator.setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                activateLiveTranslator()
            } else {
                requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 105)
            }
        }

        mainLayout.addView(btnStartTranslator)
        setContentView(mainLayout)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun activateLiveTranslator() {
        stopSpeechEngine()
        isStreamingActive = true
        setupOverlayWindow()
        
        overlayTextView?.text = "📺 [Google Speech Engine]\n🎙️ ድምፅ እየጠበቅኩ ነው... MrBeast ቪዲዮ ይክፈቱ"
        overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))

        startGoogleSpeechRecognition()
    }

    private fun startGoogleSpeechRecognition() {
        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: AndroidBundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // ስህተት ሲፈጠር ወይም ድምፅ ሲጠፋ ራሱን በራሱ እንዲቀሰቅስ (Auto-Restart)
                        if (isStreamingActive) {
                            mainHandler.postDelayed({ startGoogleSpeechRecognition() }, 1000)
                        }
                    }
                    override fun onResults(results: AndroidBundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val spokenText = matches[0].lowercase()
                            processSpokenWords(spokenText)
                        }
                        if (isStreamingActive) startGoogleSpeechRecognition()
                    }
                    override fun onPartialResults(partialResults: AndroidBundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val spokenText = matches[0].lowercase()
                            processSpokenWords(spokenText)
                        }
                    }
                    override fun onEvent(eventType: Int, params: AndroidBundle?) {}
                })

                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                overlayTextView?.text = "❌ ስህተት፡ Google Speech አልተነሳም"
            }
        }
    }

    private fun processSpokenWords(text: String) {
        var foundTranslation = false
        for ((englishWord, amharicTranslation) in translationDictionary) {
            if (text.contains(englishWord)) {
                overlayTextView?.setTextColor(Color.parseColor("#F59E0B"))
                overlayTextView?.text = "🔊 [የተሰማ ቃል]: \"$englishWord\"\n🔄 [ትርጉም]: $amharicTranslation"
                foundTranslation = true
                break
            }
        }
        
        if (!foundTranslation) {
            overlayTextView?.setTextColor(Color.parseColor("#10B981"))
            overlayTextView?.text = "🎙️ [ድምፅ ይሰማል]..."
        }
    }

    private fun setupOverlayWindow() {
        if (overlayTextView == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayTextView = TextView(this).apply {
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(45, 45, 45, 45)
                gravity = Gravity.CENTER
                elevation = 25f
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
                horizontalMargin = 0.05f
            }
            try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
        }
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B")) 
            cornerRadius = 35f
            setStroke(5, Color.parseColor("#10B981")) 
        }
        overlayTextView?.background = backgroundDrawable
    }

    private fun stopSpeechEngine() {
        isStreamingActive = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopSpeechEngine()
        try {
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
