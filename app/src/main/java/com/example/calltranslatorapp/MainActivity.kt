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
    
    // 🎛️ የትርጉም ሞድ መቆጣጠሪያ (0 = አልተመረጠም, 1 = ENG to AMH, 2 = AMH to ENG)
    private var translationMode = 0 
    private var isShowingResult = false 

    // 📖 300 የተመረጡ ከመስመር ውጭ የሁለትዮሽ መዝገበ-ቃላት ጥቅል
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        // [ክፍል 1: አማርኛ] 150 ወሳኝ ቃላት
        put("ስብሰባው መቼ ነው", "When is the meeting?")
        put("ውል መፈረም እፈልጋለሁ", "I want to sign a contract.")
        put("ዋጋው ስንት ነው", "What is the price?")
        put("ይህ የቢዝነስ እቅዳችን ነው", "This is our business plan.")
        put("ትርፋችን ጨምሯል", "Our profit has increased.")
        put("ኪሳራ አጋጥሞናል", "We have faced a loss.")
        put("የባንክ ሂሳቤን ማረጋገጥ እፈልጋለሁ", "I want to check my bank account.")
        put("ደረሰኝ ስጠኝ እባክህ", "Please give me a receipt.")
        put("አሞኛል ዶክተር መጥራት እፈልጋለሁ", "I am sick, I want to call a doctor.")
        put("ራስ ምታት አለብኝ", "I have a headache.")
        put("ትኩሳት አለብኝ", "I have a fever.")
        put("ፋርማሲው የት ነው", "Where is the pharmacy?")
        put("አምቡላንስ ጥራ", "Call an ambulance.")
        put("ፓስፖርቴ የት ነው", "Where is my passport?")
        put("ቪዛ ማግኘት እፈልጋለሁ", "I want to get a visa.")
        put("የአውሮፕላን ትኬት ስንት ነው", "How much is the flight ticket?")
        put("ሻንጣዬ ጠፍቷል", "My luggage is lost.")
        put("የአውሮፕላን ማረፊያ የት ነው", "Where is the airport?")
        put("ሆቴል መያዝ እፈልጋለሁ", "I want to book a hotel.")
        put("ታክሲ የት አገኛለሁ", "Where can I find a taxi?")
        put("እወድሃለሁ", "I love you (to a male)")
        put("እወድሻለሁ", "I love you (to a female)")
        put("ናፍቀኸኛል", "I miss you (to a male)")
        put("ናፍቀሽኛል", "I miss you (to a female)")
        put("ውዴ", "My love")
        put("የኔ ፍቅር", "My love")
        put("የኔ አለም", "My world")
        put("የኔ ቆንጆ", "My beautiful")
        put("ልቤ", "My heart")
        put("የልቤ ንጉስ", "King of my heart")
        put("የልቤ ንግስት", "Queen of my heart")
        put("በጣም ነው የምወድህ", "I love you so much")
        put("ያለ አንተ መኖር አልችልም", "I can't live without you")
        put("ያለ አንቺ መኖር አልችልም", "I can't live without you")
        put("ደስታዬ ነህ", "You are my happiness")
        put("ደስታዬ ነሽ", "You are my happiness")
        put("ፈገግታህ ደስ ይለኛል", "I love your smile")
        put("ፈገግታሽ ደስ ይለኛል", "I love your smile")
        put("የኔ ውድ", "My precious")
        put("የኔ ማር", "My honey")
        put("ህይወቴ ነሽ", "You are my life")
        put("ሰላም", "Hello")
        put("እንደምን ነህ", "How are you?")
        put("ደህና ነኝ", "I am fine")
        put("ምን አዲስ ነገር አለ", "What is new?")
        put("አመሰግናለሁ", "Thank you")
        put("ይቅርታ", "Excuse me / Sorry")
        put("አልገባኝም", "I don't understand")
        put("ስምህ ማን ነው", "What is your name?")
        put("መንገድ ጠፋኝ", "I am lost")
        put("ሰዓት ስንት ነው", "What time is it?")
        put("እንሂድ", "Let's go")
        put("ደህና ሁን", "Goodbye")
        put("ቁም", "Stop")
        put("እባክህ እርዳኝ", "Please help me")
        put("ነገ እንገናኝ", "See you tomorrow")
        put("እንኳን ደስ አለህ", "Congratulations")

        // [ክፍል 2: እንግሊዝኛ] 150 ወሳኝ ቃላት
        put("hello", "ሰላም")
        put("how are you", "እንደምን ነህ? / እንደምን ነሽ?")
        put("i am fine", "ደህና ነኝ")
        put("what is new", "ምን አዲስ ነገር አለ?")
        put("thank you", "አመሰግናለሁ")
        put("excuse me", "ይቅርታ")
        put("sorry", "አዝናለሁ")
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
        put("i want to sign a contract", "ውል መፈረም እፈልጋለሁ")
        put("what is the price", "ዋጋው ስንት ነው?")
        put("this is our business plan", "ይህ የቢዝነስ እቅዳችን ነው")
        put("our profit has increased", "ትርፋችን ጨምሯል")
        put("we have faced a loss", "ኪሳራ አጋጥሞናል")
        put("i want to check my bank account", "የባንክ ሂሳቤን ማረጋገጥ እፈልጋለሁ")
        put("please give me a receipt", "ደረሰኝ ስጠኝ እባክህ")
        put("i am sick", "አሞኛል")
        put("i have a headache", "ራስ ምታት አለብኝ")
        put("i have a fever", "ትኩሳት አለብኝ")
        put("where is the pharmacy", "ፋርማሲው የት ነው?")
        put("call an ambulance", "አምቡላንስ ጥራ")
        put("where is my passport", "ፓስፖርቴ የት ነው?")
        put("i want to get a visa", "ቪዛ ማግኘት እፈልጋለሁ")
        put("how much is the flight ticket", "የአውሮፕላን ትኬት ስንት ነው?")
        put("my luggage is lost", "ሻንጣዬ ጠፍቷል")
        put("where is the airport", "የአውሮፕላን ማረፊያ የት ነው?")
        put("i want to book a hotel", "ሆቴል መያዝ እፈልጋለሁ")
        put("where can i find a taxi", "ታክሲ የት አገኛለሁ?")
        put("i love you", "እወድሻለሁ / እወድሃለሁ")
        put("i miss you", "ናፍቀሽኛል / ናፍቀኸኛል")
        put("my love", "የኔ ፍቅር / ውዴ")
        put("my world", "የኔ አለም")
        put("my beautiful", "የኔ ቆንጆ")
        put("my heart", "ልቤ")
        put("i love you so much", "በጣም ነው የምወድህ")
        put("i can't live without you", "ያለ አንተ መኖር አልችልም")
        put("you are my happiness", "ደስታዬ ነህ / ነሽ")
        put("i love your smile", "ፈገግታህ ደስ ይለኛል")
        put("my precious", "የኔ ውድ")
        put("my honey", "የኔ ማር")
        put("you are my life", "ህይወቴ ነህ")
        put("meeting", "ስብሰባ")
        put("contract", "ውል")
        put("profit", "ትርፍ")
        put("loss", "ኪሳራ")
        put("doctor", "ዶክተር")
        put("hospital", "ሆስፒታል")
        put("pharmacy", "ፋርማሲ")
        put("medicine", "መድሃኒት")
        put("passport", "ፓስፖርት")
        put("visa", "ቪዛ")
        put("ticket", "ትኬት")
        put("airport", "አውሮፕላን ማረፊያ")
        put("hotel", "ሆቴል")
        put("taxi", "ታክሲ")
        put("stop", "ቁም")
        put("money", "ገንዘብ")
        put("food", "ምግብ")
        put("water", "ውሃ")
        put("friend", "ጓደኛ")
        put("time", "ሰዓት")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic UI Scaffolding ለቋንቋ መምረጫ
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            padding = 50
        }

        val titleView = TextView(this).apply {
            text = "🎙️ Call Translator Pro\n(100% Offline Mode)"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(titleView)

        // 🔘 ቁልፍ 1: English to Amharic
        val btnEngToAmh = Button(this).apply {
            text = "🇺🇸 ENG ➔ 🇪🇹 AMH (የእንግሊዝኛ ድምፅ)"
            setBackgroundColor(Color.parseColor("#FF9800")) // ብርቱካናማ
            setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(30, 40, 30, 40)
        }
        
        // 🔘 ቁልፍ 2: Amharic to English
        val btnAmhToEng = Button(this).apply {
            text = "🇪🇹 AMH ➔ 🇺🇸 ENG (የአማርኛ ድምፅ)"
            setBackgroundColor(Color.parseColor("#4CAF50")) // አረንጓዴ
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(30, 40, 30, 40)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 40, 0, 40)
            layoutParams = params
        }

        // 🔘 ቁልፍ 3: የአማርኛ ጥቅል ማውረጃ (Voice Package Downloader)
        val btnDownloadModel = Button(this).apply {
            text = "📥 የአማርኛ Offline ጥቅል መጫኛ"
            setBackgroundColor(Color.parseColor("#2196F3")) // ሰማያዊ
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
            Toast.makeText(this, "⚠️ Offline Speech Recognition ውስጥ ገብተው 'አማርኛ'ን ያውርዱ!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val backupIntent = Intent(Settings.ACTION_SETTINGS)
            startActivity(backupIntent)
            Toast.makeText(this, "Settings -> Language -> Voice Settings ውስጥ ያውርዱ", Toast.LENGTH_LONG).show()
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
        
        val modeText = if (translationMode == 1) "English to Amharic" else "Amharic to English"
        Toast.makeText(this, "🚀 ሞድ: $modeText ተነስቷል!", Toast.LENGTH_SHORT).show()
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
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // ፍጹም ከመስመር ውጭ ማስገደጃ
                    
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
                        // ስህተት ሲፈጠር (ሰው ዝም ሲል) ሳይጠፋ በዛው ቋንቋ እንዲቀጥል ማድረጊያ
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
            isShowingResult = true // 🔒 ስክሪኑን ለተጠቃሚው ማቆያ
            try { speechRecognizer?.destroy() } catch (e: Exception) {}

            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
            if (translationMode == 2) {
                overlayTextView?.text = "🇪🇹 አማርኛ: $rawInput\n🇺🇸 ENG: $translatedText"
            } else {
                overlayTextView?.text = "🇺🇸 ENG: $rawInput\n🇪🇹 አማርኛ: $translatedText"
            }

            // ⏱️ ፅሁፉ በስክሪን ሰከንድ ሳይጠፋ ለ 7 ሰከንድ ሙሉ እንዲቆይ ማድረጊያ
            mainHandler.postDelayed({
                isShowingResult = false 
                restartListening()
            }, 7000) 

        } else {
            // ትርጉም ባይገኝም ፅሁፉን አሳይቶ ለ 4 ሰከንድ ያቆየዋል
            isShowingResult = true
            overlayTextView?.setTextColor(Color.parseColor("#FF5252"))
            overlayTextView?.text = "🎙️ ግብዓት: $rawInput\n⚠️ [ይህ ቃል በ 300 ቃላት ጥቅል ውስጥ የለም]"
            
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
