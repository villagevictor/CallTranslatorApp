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
import java.util.Locale

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    // 📖 የ 50 የዕለት ተዕለት ንግግሮች ከመስመር ውጭ መዝገበ-ቃላት
    private val offlineDictionary = HashMap<String, String>().apply {
        put("hello", "ሰላም")
        put("how are you", "እንደምን ነህ? / እንደምን ነሽ?")
        put("i am fine", "ደህና ነኝ")
        put("what is new", "ምን አዲስ ነገር አለ?")
        put("nothing much", "ምንም አዲስ ነገር የለም")
        put("how old are you", "ዕድሜህ ስንት ነው?")
        put("what is your name", "ስምህ ማን ነው?")
        put("my name is", "ስሜ ... ነው")
        put("nice to meet you", "ስላገኘሁህ ደስ ብሎኛል")
        put("good morning", "እንደምን አደርክ / አደርሽ")
        put("good afternoon", "እንደምን ዋልክ / ዋልሽ")
        put("good evening", "እንደምን አመሸህ / አመሸሽ")
        put("good night", "ደህና እደር / እደሪ")
        put("thank you", "አመሰግናለሁ")
        put("thank you very much", "በጣም አመሰግናለሁ")
        put("you are welcome", "ምንም አይደለም")
        put("where are you from", "ከየት ሀገር ነህ?")
        put("i am from ethiopia", "እኔ ከኢትዮጵያ ነኝ")
        put("where do you live", "የት ነው የምትኖረው?")
        put("what do you do", "ምን ትሰራለህ? (ስራህ ምንድነው?)")
        put("i am a student", "እኔ ተማሪ ነኝ")
        put("can you help me", "ልትረዳኝ ትችላለህ?")
        put("excuse me", "ይቅርታ")
        put("sorry", "አዝናለሁ / አፉ በለኝ")
        put("what time is it", "ሰዓት ስንት ነው?")
        put("where is the bathroom", "መጸዳጃ ቤቱ የት ነው?")
        put("how much is this", "ዋጋው ስንት ነው?")
        put("it is too expensive", "በጣም ውድ ነው")
        put("i don't understand", "አልገባኝም")
        put("do you speak amharic", "አማርኛ ትናገራለህ?")
        put("yes i do", "አዎ እናገራለሁ")
        put("no i don't", "አይ፣ አልናገርም")
        put("please speak slowly", "እባክህ ቀስ ብለህ ተናገር")
        put("what does this mean", "ይህ ማለት ምን ማለት ነው?")
        put("i am hungry", "ርቦኛል")
        put("i am thirsty", "ጠምቶኛል")
        put("i like it", "ወድጄዋለሁ")
        put("i don't like it", "አልወደድኩትም")
        put("where are you going", "የት እየሄድክ ነው?")
        put("i am going home", "ወደ ቤት እየሄድኩ ነው")
        put("what happened", "ምን ተፈጠረ?")
        put("don't worry", "አትጨነቅ / አትጨነቂ")
        put("everything will be fine", "ሁሉም ነገር ጥሩ ይሆናል")
        put("i am tired", "ደክሞኛል")
        put("i am sick", "አሞኛል")
        put("call me later", "በኋላ ደውልልኝ")
        put("i will call you", "እኔ እደውልልሃለሁ")
        put("let's go", "እንሂድ")
        put("wait a minute", "አንድ ደቂቃ ቆይ")
        put("see you later", "በኋላ እንገናኝ")
        put("goodbye", "ደህና ሁን / ሁኚ")
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
        
        Toast.makeText(this, "🚀 የ 50 ንግግሮች ሞተር ተነስቷል!", Toast.LENGTH_SHORT).show()
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
            .setContentText("ከመስመር ውጭ 50 ንግግሮች ዝግጁ ናቸው...")
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
                        if (isListening) mainHandler.postDelayed({ restartListening() }, 300)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            lookupTranslation(matches[0])
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

    private fun lookupTranslation(spokenText: String) {
        val cleanText = spokenText.lowercase(Locale.ROOT).trim()
        var translatedText = ""

        // 🔍 በከፊል (Partial match) ፍለጋ ለማድረግ
        for ((key, value) in offlineDictionary) {
            if (cleanText.contains(key)) {
                translatedText = value
                break
            }
        }

        if (translatedText.isNotEmpty()) {
            overlayTextView?.text = "ENG 🇺🇸: $spokenText\nAMH 🇪🇹: $translatedText"
        } else {
            overlayTextView?.text = "ENG 🇺🇸: $spokenText\n⚠️ [ትርጉም አልተመዘገበም]"
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
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
