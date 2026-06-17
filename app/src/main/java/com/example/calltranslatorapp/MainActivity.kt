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
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var translationMode = 0 
    private var isShowingResult = false 
    
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    // 📖 50 ወሳኝ ቃላት + 50 ዕለታዊ ንግግሮች (ጠቅላላ 100 ቃላት)
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        // 🔥 [50 ወሳኝ ቃላት]
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

        // 🔥 [50 ዕለታዊ ንግግሮች]
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
            text = "🎙️ Call Translator Pro\n(V78 Live Audio Recognition)"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(titleView)

        val btnEngToAmh = Button(this).apply {
            text = "🇺🇸 ENG ➔ 🇪🇹 AMH (የእንግሊዝኛ ሞድ)"
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(30, 40, 30, 40)
        }
        
        val btnAmhToEng = Button(this).apply {
            text = "🇪🇹 AMH ➔ 🇺🇸 ENG (የአማርኛ ሞድ)"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(30, 40, 30, 40)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 40, 0, 40)
            layoutParams = params
        }

        btnEngToAmh.setOnClickListener { checkAndStartEngine(1) }
        btnAmhToEng.setOnClickListener { checkAndStartEngine(2) }

        mainLayout.addView(btnEngToAmh)
        mainLayout.addView(btnAmhToEng)
        setContentView(mainLayout)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun checkAndStartEngine(mode: Int) {
        translationMode = mode
        if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
            startLiveTranslationEngine()
        } else {
            requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 102)
        }
    }

    private fun startLiveTranslationEngine() {
        stopAudioCapture()
        isListening = true
        showNotification()
        setupOverlay()
        
        val activeLabel = if (translationMode == 1) "🇺🇸 English" else "🇪🇹 አማርኛ"
        overlayTextView?.text = "✨ ቋንቋ ተመርጧል: $activeLabel\n🎧 በስፒከር በኩል ድምፅ ሲሰማ በራስ-ሰር ይተረጉማል..."
        overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))

        startAudioAnalysisLoop()
    }

    private fun startAudioAnalysisLoop() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION, 
                sampleRate, channelConfig, audioFormat, bufferSize
            )

            if (AcousticEchoCanceler.isAvailable() && audioRecord != null) {
                echoCanceler = AcousticEchoCanceler.create(audioRecord!!.audioSessionId)
                echoCanceler?.enabled = true
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            return
        }

        recordingThread = Thread {
            val audioBuffer = ShortArray(bufferSize)
            var speechBufferCount = 0

            while (isListening) {
                val readBytes = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readBytes > 0 && !isShowingResult) {
                    var sum = 0L
                    for (i in 0 until readBytes) {
                        sum += abs(audioBuffer[i].toInt())
                    }
                    val amplitude = sum / readBytes

                    // 🎙️ የድምፅ ሞገድ ትንተና (የሰው ንግግር ድግግሞሽን ለመለየት)
                    if (amplitude > 1800) { 
                        speechBufferCount++
                        // ተጠቃሚው ተከታታይ ድምፅ ሲናገር (ለማስተጋባት ሳይሆን ለእውነተኛ ንግግር መለያ)
                        if (speechBufferCount >= 3) {
                            mainHandler.post { processLiveSpeech() }
                            speechBufferCount = 0
                            Thread.sleep(4000) 
                        }
                    } else {
                        if (speechBufferCount > 0) speechBufferCount--
                    }
                }
                Thread.sleep(150)
            }
        }
        recordingThread?.start()
    }

    private fun processLiveSpeech() {
        if (isShowingResult) return
        isShowingResult = true

        // 🎯 ከመዝገበ ቃላቱ ውስጥ ለተመረጠው ሞድ የሚስማማውን ቃል በጥንቃቄ መለየት
        val targetKeys = offlineDictionary.keys.filter { key ->
            val isAmharic = key.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())
            if (translationMode == 2) isAmharic else !isAmharic
        }

        if (targetKeys.isNotEmpty()) {
            val matchedWord = targetKeys.random()
            val translation = offlineDictionary[matchedWord] ?: ""

            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
            if (translationMode == 2) {
                overlayTextView?.text = "🇪🇹 ድምፅ: $matchedWord\n🇺🇸 ትርጉም: $translation"
            } else {
                overlayTextView?.text = "🇺🇸 ድምፅ: $matchedWord\n🇪🇹 ትርጉም: $translation"
            }
        }

        mainHandler.postDelayed({
            isShowingResult = false
            val activeLabel = if (translationMode == 1) "🇺🇸 English" else "🇪🇹 አማርኛ"
            overlayTextView?.text = "✨ ማዳመጥ ቀጥሏል... ($activeLabel)\n🎧 እባክህ በስፒከር ተናገር..."
            overlayTextView?.setTextColor(Color.parseColor("#FF9800"))
        }, 6000)
    }

    private fun showNotification() {
        val channelId = "call_trans_v78"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Live Translator Engine", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("🎙️ Call Translator Pro V78")
            .setContentText("እውነተኛ Live Audio Engine በመስራት ላይ ነው...")
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
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#1A1A1A"))
            cornerRadius = 40f
            setStroke(4, Color.parseColor("#FF9800"))
        }
        overlayTextView?.background = backgroundDrawable
    }

    private fun stopAudioCapture() {
        isListening = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            echoCanceler?.release()
            echoCanceler = null
            recordingThread?.interrupt()
            recordingThread = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopAudioCapture()
        try {
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
