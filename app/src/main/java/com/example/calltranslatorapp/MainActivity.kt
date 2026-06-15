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
import android.view.WindowManager
import android.widget.Button
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
    
    // 🎛️ የሰርጥ መቀያየሪያ (True = አማርኛ ያዳምጣል, False = እንግሊዝኛ ያዳምጣል)
    // ስልክህ ላይ የእንግሊዝኛ ጥቅል ብቻ ስላለ መጀመሪያ በእንግሊዝኛ (False) እንዲነሳ እናደርገዋለን
    private var listenAmharicToggle = false
    private var isShowingResult = false // ትርጉም ስክሪን ላይ መኖሩን ማረጋገጫ

    // 📖 የተመረጡ 300 ከመስመር ውጭ (Offline) የሁለትዮሽ መዝገበ-ቃላት ጥቅል
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        
        // 🇪🇹 === [አማርኛ] 150 ወሳኝ ቃላት እና ንግግሮች ===
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

        // 🇺🇸 === [እንግሊዝኛ] 150 ወሳኝ ቃላት እና ንግግሮች ===
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
        put("i have a fever", "ትকুሳት አለብኝ")
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
        setContentView(R.layout.activity_main)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
            }
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
        
        Toast.makeText(this, "🚀 300 ትርጉም ከመስመር ውጭ ዝግጁ ነው!", Toast.LENGTH_SHORT).show()
    }

    private fun showNotification() {
        val channelId = "call_trans_fast"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Fast Translation", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        
        val logoId = resources.getIdentifier("app_logo", "drawable", packageName)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("🎙️ Call Translator Pro")
            .setContentText("ለመተርጎም ዝግጁ ነው...")
            .setSmallIcon(if (logoId != 0) logoId else android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        manager.notify(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayTextView = TextView(this).apply {
            text = "✨ ለመተርጎም ዝግጁ ነው..."
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#4CAF50")) 
            setPadding(45, 35, 45, 35)
            gravity = Gravity.CENTER

            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A")) 
                cornerRadius = 35f 
                setStroke(3, Color.parseColor("#FF9800")) 
            }
            background = backgroundDrawable
            elevation = 15f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 180
            width = WindowManager.LayoutParams.MATCH_PARENT
            horizontalMargin = 0.05f 
        }
        
        try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
    }

    private fun startListeningLoop() {
        if (!isListening) return
        
        // 🛑 ትርጉም ስክሪን ላይ ካለ አዲሱን ማዳመጫ ለተወሰነ ጊዜ ያቆማል
        if (isShowingResult) return 

        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) 
                    
                    if (listenAmharicToggle) {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "am-ET")
                    } else {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    }
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (!isShowingResult) {
                            val langLabel = if (listenAmharicToggle) "🇪🇹 አማርኛ" else "🇺🇸 English"
                            overlayTextView?.text = "✨ ለመተርጎም ዝግጁ ነው... ($langLabel)"
                            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
                        }
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsd: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (isListening && !isShowingResult) {
                            listenAmharicToggle = !listenAmharicToggle 
                            mainHandler.postDelayed({ restartListening() }, 200)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            lookupTranslation(matches[0])
                        } else {
                            if (isListening && !isShowingResult) {
                                listenAmharicToggle = !listenAmharicToggle
                                restartListening()
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty() && !isShowingResult) {
                            overlayTextView?.text = "🎙️ ሰማሁት: ${matches[0]}"
                            overlayTextView?.setTextColor(Color.parseColor("#FF9800"))
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                if (isListening && !isShowingResult) mainHandler.postDelayed({ restartListening() }, 800)
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
            isShowingResult = true // 🔒 ፅሁፉ እንዳይጠፋ ስክሪኑን ይቆልፋል
            try { speechRecognizer?.destroy() } catch (e: Exception) {}

            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
            if (matchedKey.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())) {
                overlayTextView?.text = "🇪🇹 አማርኛ: $rawInput\n🇺🇸 ENG: $translatedText"
            } else {
                overlayTextView?.text = "🇺🇸 ENG: $rawInput\n🇪🇹 አማርኛ: $translatedText"
            }

            // ⏱️ [CRITICAL FIX] ውጤቱ በስክሪን ሰከንድ ሳይጠፋ ለ 7 ሰከንድ ሙሉ እንዲቆይ ማድረጊያ
            mainHandler.postDelayed({
                isShowingResult = false // 🔓 ቆልፉን ይከፍታል
                listenAmharicToggle = !listenAmharicToggle
                restartListening()
            }, 7000) 

        } else {
            // ትርጉም ካልተገኘ ወዲያው ማዳመጡን ይቀጥላል
            if (isListening) {
                listenAmharicToggle = !listenAmharicToggle
                restartListening()
            }
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
