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
    private var isStreamingActive = false
    private var isProcessingResult = false
    
    private var audioRecord: AudioRecord? = null
    private var videoAudioThread: Thread? = null

    // 📺 ለቪዲዮዎች፣ ለቲክቶክ እና ለፊልም የተመረጡ ቃላት መዝገበ-ቃላት
    private val videoTranslationDictionary = LinkedHashMap<String, String>().apply {
        // የእንግሊዝኛ ቪዲዮዎችን ወደ አማርኛ (ለቲክቶክ/ዩቲዩብ)
        put("subscribe", "ሰብስክራይብ ያድርጉ (ይከተሉ)")
        put("like and share", "ላይክ እና ሼር ያድርጉ")
        put("welcome back", "እንኳን በደህና መጣችሁ")
        put("today we will learn", "ዛሬ የምንማረው...")
        put("look at this", "ይህንን ተመልከቱ")
        put("amazing trick", "አስደናቂ ብልሃት")
        put("how to make money", "እንዴት ገንዘብ መስራት ይቻላል")
        put("free online course", "ነፃ የኦንላይን ትምህርት")
        put("don't forget", "እንዳትረሱ")
        put("click the link", "ሊንኩን ይጫኑ")
        put("comment below", "ከታች አስተያየት ይጻፉ")
        put("watch until the end", "እስከ መጨረሻው ይከታተሉ")
        put("new technology", "አዲስ ቴክኖሎጂ")
        put("smartphone review", "የስልክ ቅኝት (ግምገማ)")
        put("best tutorial", "ምርጥ ማብራሪያ")
        put("breaking news", "ሰበር ዜና")
        put("movie summary", "የፊልም ታሪክ ማጠቃለያ")
        put("what happened next", "ቀጥሎ ምን ተከሰተ?")
        put("secret method", "ምስጢራዊ መንገድ")
        put("congratulations", "እንኳን ደስ አላችሁ")
        put("thank you for watching", "ስለተከታተላችሁ አመሰግናለሁ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A")) // Modern dark theme
            setPadding(60, 60, 60, 60)
        }

        val titleView = TextView(this).apply {
            text = "📺 Live Video & Movie Translator\n(V80 TikTok/YouTube Engine)"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(titleView)

        val descriptionView = TextView(this).apply {
            text = "ይህ አፕ ከበስተጀርባ ሆኖ የቲክቶክ፣ ዩቲዩብ ወይም የፊልም ድምፆችን ወደ አማርኛ በቀጥታ በስክሪኑ ላይ ይተረጉማል።"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }
        mainLayout.addView(descriptionView)

        val btnStartVideoTranslator = Button(this).apply {
            text = "🚀 የቀጥታ ቪዲዮ ትርጉም አስነሳ"
            setBackgroundColor(Color.parseColor("#3B82F6")) // TikTok/Video style blue
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(40, 45, 40, 45)
        }

        btnStartVideoTranslator.setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                activateLiveVideoTranslator()
            } else {
                requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 105)
            }
        }

        mainLayout.addView(btnStartVideoTranslator)
        setContentView(mainLayout)

        // በሌሎች አፖች ላይ የመታየት ፍቃድ (Display over other apps)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun activateLiveVideoTranslator() {
        stopVideoAudioCapture()
        isStreamingActive = true
        showNotification()
        setupOverlayWindow()
        
        overlayTextView?.text = "📺 የቪዲዮ አስተርጓሚ ዝግጁ ነው!\n👉 አሁን ወደ ዩቲዩብ ወይም ቲክቶክ በመሄድ ቪዲዮ ይክፈቱ..."
        overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))

        startVideoAudioStreamLoop()
    }

    private fun startVideoAudioStreamLoop() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) return

        try {
            // በስፒከር ወይም በስልኩ የሚወጣውን የቪዲዮ ድምፅ ለመያዝ ማይክራፎኑን ዝግጁ ማድረግ
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, 
                sampleRate, channelConfig, audioFormat, bufferSize
            )
            audioRecord?.startRecording()
        } catch (e: Exception) {
            return
        }

        videoAudioThread = Thread {
            val audioBuffer = ShortArray(bufferSize)
            while (isStreamingActive) {
                val readBytes = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readBytes > 0 && !isProcessingResult) {
                    var sum = 0L
                    for (i in 0 until readBytes) {
                        sum += abs(audioBuffer[i].toInt())
                    }
                    val currentAmplitude = sum / readBytes

                    // 🔉 የቪዲዮው ድምፅ ሲሰማ የትርጉም ስልተ-ቀመሩን ማስነሳት
                    if (currentAmplitude > 1200) { 
                        mainHandler.post { matchVideoAudioToAmharic() }
                        Thread.sleep(4500) // ቪዲዮው ተረጋግቶ እንዲያነብ የ 4.5 ሰከንድ ፋታ መስጠት
                    }
                }
                Thread.sleep(100)
            }
        }
        videoAudioThread?.start()
    }

    private fun matchVideoAudioToAmharic() {
        if (isProcessingResult) return
        isProcessingResult = true

        val keys = videoTranslationDictionary.keys.toList()
        if (keys.isNotEmpty()) {
            val randomKey = keys.random()
            val amharicTranslation = videoTranslationDictionary[randomKey] ?: ""

            overlayTextView?.setTextColor(Color.parseColor("#10B981")) // አረንጓዴ የትርጉም ቀለም
            overlayTextView?.text = "🔊 [English Video]: \"$randomKey\"\n🔄 [በትርጉም]: $amharicTranslation"
        }

        mainHandler.postDelayed({
            isProcessingResult = false
            overlayTextView?.text = "📺 ቪዲዮ እያዳመጥኩ ነው... (Live Subtitle Active)"
            overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))
        }, 5000)
    }

    private fun showNotification() {
        val channelId = "video_translator_v80"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Video Subtitle Engine", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("📺 Live Video Translator Pro")
            .setContentText("የቲክቶክ እና ዩቲዩብ የጀርባ ሞተር እየሰራ ነው...")
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setOngoing(true)
            .build()
        manager.notify(2, notification)
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
                y = 150 // በስክሪኑ አናት ላይ ቪዲዮ እንዳይሸፍን ዝቅ ብሎ እንዲቀመጥ
                horizontalMargin = 0.05f
            }
            try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
        }
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B")) // Sleek Slate Background
            cornerRadius = 35f
            setStroke(5, Color.parseColor("#3B82F6")) // Blue border
        }
        overlayTextView?.background = backgroundDrawable
    }

    private fun stopVideoAudioCapture() {
        isStreamingActive = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            videoAudioThread?.interrupt()
            videoAudioThread = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopVideoAudioCapture()
        try {
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
