package com.example.calltranslatorapp

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var translationMode = 0 
    private var isShowingResult = false 

    // 📖 50 ወሳኝ ቃላት + 50 ዕለታዊ ንግግሮች (በአጠቃላይ 100 ከመስመር ውጭ መዝገበ-ቃላት)
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        // 🔥 [50 ወሳኝ ቃላት - 25 English / 25 Amharic]
        put("hello", "ሰላም")
        put("thank you", "አመሰግናለሁ")
        put("sorry", "አዝናለሁ")
        put("please", "እባክህ")
        put("yes", "አዎ")
        put("no", "አይ")
        put("money", "ገንዘብ")
        put("food", "ምግብ")
        put("water", "ውሃ")
        put("doctor", "ዶክተር")
        put("hospital", "ሆስፒታል")
        put("pharmacy", "ፋርማሲ")
        put("passport", "ፓስፖርት")
        put("visa", "ቪዛ")
        put("ticket", "ትኬት")
        put("airport", "አውሮፕላን ማረፊያ")
        put("hotel", "ሆቴል")
        put("taxi", "ታክሲ")
        put("stop", "ቁም")
        put("friend", "ጓደኛ")
        put("time", "ሰዓት")
        put("today", "ዛሬ")
        put("tomorrow", "ነገ")
        put("house", "ቤት")
        put("name", "ስም")
        
        put("ሰላም", "Hello")
        put("አመሰግናለሁ", "Thank you")
        put("ይቅርታ", "Sorry")
        put("እባክህ", "Please")
        put("ገንዘብ", "Money")
        put("ምግብ", "Food")
        put("ውሃ", "Water")
        put("ዶክተር", "Doctor")
        put("ሆስፒታል", "Hospital")
        put("ፋርማሲ", "Pharmacy")
        put("ፓስፖርት", "Passport")
        put("ቪዛ", "Visa")
        put("ትኬት", "Ticket")
        put("ሆቴል", "Hotel")
        put("ታክሲ", "Taxi")
        put("ቁም", "Stop")
        put("ጓደኛ", "Friend")
        put("ሰዓት", "Time")
        put("ዛሬ", "Today")
        put("ነገ", "Tomorrow")
        put("ስም", "Name")
        put("ቢሮ", "Office")
        put("ስልክ", "Phone")
        put("ዋጋ", "Price")
        put("ትልቅ", "Big")

        // 🔥 [50 ዕለታዊ ንግግሮች - 25 English / 25 Amharic]
        put("how are you", "እንደምን ነህ? / እንደምን ነሽ?")
        put("i am fine", "ደህና ነኝ")
        put("what is new", "ምን አዲስ ነገር አለ?")
        put("i don't understand", "አልገባኝም")
        put("what is your name", "ስምህ ማን ነው?")
        put("where is the bathroom", "መጸዳጃ ቤቱ የት ነው?")
        put("what time is it", "ሰዓት ስንት ነው?")
        put("let's go", "እንሂድ")
        put("goodbye", "ደህና ሁን")
        put("i am lost", "መንገድ ጠፋኝ")
        put("please help me", "እባክህ እርዳኝ")
        put("see you tomorrow", "ነገ እንገናኝ")
        put("congratulations", "እንኳን ደስ አለህ")
        put("when is the meeting", "ስብሰባው መቼ ነው?")
        put("what is the price", "ዋጋው ስንት ነው?")
        put("where is the airport", "የአውሮፕላን ማረፊያ የት ነው?")
        put("i want to book a hotel", "ሆቴል መያዝ እፈልጋለሁ")
        put("where can i find a taxi", "ታክሲ የት አገኛለሁ?")
        put("i love you", "እወድሻለሁ / እወድሃለሁ")
        put("i miss you", "ናፍቀሽኛል / ናፍቀኸኛል")
        put("my love", "የኔ ፍቅር")
        put("i can't live without you", "ያለ አንተ መኖር አልችልም")
        put("you are my happiness", "ደስታዬ ነህ")
        put("i love your smile", "ፈገግታህ ደስ ይለኛል")
        put("you are my life", "ህይወቴ ነህ")

        put("ስብሰባው መቼ ነው", "When is the meeting?")
        put("ውል መፈረም እፈልጋለሁ", "I want to sign a contract.")
        put("ዋጋው ስንት ነው", "What is the price?")
        put("እባክህ ደረሰኝ ስጠኝ", "Please give me a receipt.")
        put("አሞኛል ዶክተር ጥራ", "I am sick, call a doctor.")
        put("ራስ ምታት አለብኝ", "I have a headache.")
        put("ትኩሳት አለብኝ", "I have a fever.")
        put("መንገድ ጠፋኝ እባክህ እርዳኝ", "I am lost, please help me.")
        put("ቪዛ ማግኘት እፈልጋለሁ", "I want to get a visa.")
        put("የአውሮፕላን ትኬት ስንት ነው", "How much is the flight ticket?")
        put("ሻንጣዬ ጠፍቷል", "My luggage is lost.")
        put("እንደምን ነህ", "How are you?")
        put("ደህና ነኝ", "I am fine")
        put("ምን አዲስ ነገር አለ", "What is new?")
        put("እወድሃለሁ", "I love you")
        put("እወድሻለሁ", "I love you")
        put("ናፍቀኸኛል", "I miss you")
        put("ናፍቀሽኛል", "I miss you")
        put("የኔ ፍቅር", "My love")
        put("በጣም ነው የምወድህ", "I love you so much")
        put("ደስታዬ ነህ", "You are my happiness")
        put("ደስታዬ ነሽ", "You are my happiness")
        put("ፈገግታሽ ደስ ይለኛል", "I love your smile")
        put("የኔ ማር", "My honey")
        put("ነገ እንገናኝ", "See you tomorrow")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(50, 50, 50, 50)
        }

        val titleView = TextView(this).apply {
            text = "🎙️ Call Translator Pro\n(Optimized 100 Words)"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(titleView)

        val btnEngToAmh = Button(this).apply {
            text = "🇺🇸 ENG ➔ 🇪🇹 AMH (የእንግሊዝኛ ድምፅ)"
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(30, 40, 30, 40)
        }
        
        val btnAmhToEng = Button(this).apply {
            text = "🇪🇹 AMH ➔ 🇺🇸 ENG (የአማርኛ ድምፅ)"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(30, 40, 30, 40)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 40, 0, 40)
            layoutParams = params
        }

        val btnDownloadModel = Button(this).apply {
            text = "📥 የአማርኛ Offline ጥቅል መጫኛ"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            textSize = 14f
        }

        btnEngToAmh.setOnClickListener { checkAndStartEngine(1) }
        btnAmhToEng.setOnClickListener { checkAndStartEngine(2) }
        btnDownloadModel.setOnClickListener { triggerGoogleVoiceSettings() }

        mainLayout.addView(btnEngToAmh)
        mainLayout.addView(btnAmhToEng)
        mainLayout.addView(btnDownloadModel)
        setContentView(mainLayout)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun checkAndStartEngine(mode: Int) {
        translationMode = mode
        if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
            startTranslationEngine()
        } else {
            requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 102)
        }
    }

    private fun triggerGoogleVoiceSettings() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.settingsui.VoiceSearchPreferences")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
            Toast.makeText(this, "⚠️ Offline Speech Recognition -> All ውስጥ ገብተው 'አማርኛ'ን ያውርዱ!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val backupIntent = Intent(Settings.ACTION_SETTINGS)
            startActivity(backupIntent)
        }
    }

    private fun startTranslationEngine() {
        if (isListening) {
            try { speechRecognizer?.destroy() } catch(e: Exception){}
        }
        isListening = true
        showNotification()
        setupOverlay()
        startListeningLoop()
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
            .setContentText("ከመስመር ውጭ ለመተርጎም ዝግጁ ነው...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        manager.notify(1, notification)
    }

    private fun setupOverlay() {
        if (overlayTextView == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayTextView = TextView(this).apply {
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(45, 45, 45, 45)
                gravity = Gravity.CENTER
                elevation = 20f
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = 200
                horizontalMargin = 0.05f
            }
            try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
        }
        
        overlayTextView?.text = "✨ ለመተርጎም ዝግጁ ነው..."
        overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#1A1A1A"))
            cornerRadius = 40f
            setStroke(4, Color.parseColor("#FF9800"))
        }
        overlayTextView?.background = backgroundDrawable
    }

    private fun startListeningLoop() {
        if (!isListening || isShowingResult) return

        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) 
                    
                    if (translationMode == 1) {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    } else {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
                    }
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (!isShowingResult) {
                            val activeLabel = if (translationMode == 1) "🎙️ ማዳመጥ: 🇺🇸 English" else "🎙️ ማዳመጥ: 🇪🇹 አማርኛ"
                            overlayTextView?.text = "✨ ዝግጁ ነው... ($activeLabel)"
                            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
                        }
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsd: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (isListening && !isShowingResult) {
                            mainHandler.postDelayed({ restartListening() }, 250)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            lookupTranslation(matches[0])
                        } else {
                            if (isListening && !isShowingResult) restartListening()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty() && !isShowingResult) {
                            overlayTextView?.text = "🎧 እየሰማሁ ነው: ${matches[0]}"
                            overlayTextView?.setTextColor(Color.parseColor("#FF9800"))
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                if (isListening && !isShowingResult) mainHandler.postDelayed({ restartListening() }, 1000)
            }
        }
    }

    private fun lookupTranslation(spokenText: String) {
        val rawInput = spokenText.trim()
        val lowerInput = rawInput.lowercase(Locale.ROOT)
        var translatedText = ""
        var matchedKey = ""

        for (key in offlineDictionary.keys) {
            if (key.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())) {
                if (rawInput.contains(key) || key.contains(rawInput)) {
                    translatedText = offlineDictionary[key] ?: ""
                    matchedKey = key
                    break
                }
            } else {
                if (lowerInput.contains(key.lowercase(Locale.ROOT)) || key.lowercase(Locale.ROOT).contains(lowerInput)) {
                    translatedText = offlineDictionary[key] ?: ""
                    matchedKey = key
                    break
                }
            }
        }

        if (translatedText.isNotEmpty()) {
            isShowingResult = true 
            try { speechRecognizer?.destroy() } catch (e: Exception) {}

            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
            if (translationMode == 2) {
                overlayTextView?.text = "🇪🇹 አማርኛ: $rawInput\n🇺🇸 ENG: $translatedText"
            } else {
                overlayTextView?.text = "🇺🇸 ENG: $rawInput\n🇪🇹 አማርኛ: $translatedText"
            }

            mainHandler.postDelayed({
                isShowingResult = false 
                restartListening()
            }, 7000) 

        } else {
            isShowingResult = true
            overlayTextView?.setTextColor(Color.parseColor("#FF5252"))
            overlayTextView?.text = "🎙️ ግብዓት: $rawInput\n⚠️ [ይህ ቃል በ 100 ቃላት ጥቅል ውስጥ የለም]"
            
            mainHandler.postDelayed({
                isShowingResult = false
                restartListening()
            }, 4000)
        }
    }

    private fun restartListening() {
        if (!isListening || isShowingResult) return
        try { speechRecognizer?.destroy(); startListeningLoop() } catch (e: Exception) {}
    }

    override fun onDestroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
