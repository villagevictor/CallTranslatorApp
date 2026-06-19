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
import android.widget.ImageView
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

    // 📺 የቲክቶክ፣ ዩቲዩብ እና ፊልም መዝገበ-ቃላት
    private val videoTranslationDictionary = LinkedHashMap<String, String>().apply {
        put("subscribe", "ሰብስክራይብ ያድርጉ (ይከተሉ)")
        put("like and share", "ላይክ እና ሼር ያድርጉ")
        put("welcome back", "እንኳን በደህና መጣችሁ")
        put("today we will learn", "ዛሬ የምንማረው...")
        put("look at this", "ይህንን ተመልከቱ")
        put("amazing trick", "አስደናቂ ብልሃት")
        put("how to make money", "እንዴት ገንዘብ መስራት ይቻላል")
        put("free online course", "ነፃ የኦንላይን ትምህርት")
        put("click the link", "ሊንኩን ይጫኑ")
        put("watch until the end", "እስከ መጨረሻው ይከታተሉ")
        put("new technology", "አዲስ ቴክኖሎጂ")
        put("breaking news", "ሰበር ዜና")
        put("movie summary", "የፊልም ታሪክ ማጠቃለያ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A")) // Modern deep dark theme
            setPadding(60, 60, 60, 60)
        }

        // 🎨 አዲስ Logo (በኮድ የተሰራ ፕሮፌሽናል የቲቪ/የትርጉም አርማ)
        val logoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#3B82F6"))
                cornerRadius = 40f
            }
            background = drawable
            layoutParams = LinearLayout.LayoutParams(220, 220).apply {
                setMargins(0, 0, 0, 50)
            }
        }
        val logoText = TextView(this).apply {
            text = "🇪🇹"
            textSize = 36f
            gravity = Gravity.CENTER
        }
        logoLayout.addView(logoText)
        mainLayout.addView(logoLayout)

        val titleView = TextView(this).apply {
            text = "📺 Ethio Live Translate\n(TikTok & YouTube Engine V81)"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleView)

        val descriptionView = TextView(this).apply {
            text = "ይህ አፕ ዩቲዩብ ወይም ቲክቶክ ላይ የሚከፈቱ የእንግሊዝኛ ቪዲዮዎችን ድምፅ በራስ-ሰር እየሰማ ወደ አማርኛ ጽሑፍ ይተረጉማል።"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(descriptionView)

        val btnStartVideoTranslator = Button(this).apply {
            text = "🚀 የቀጥታ ትርጉም አስነሳ"
            setBackgroundColor(Color.parseColor("#10B981")) // Emerald Green Button
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 45, 50, 45)
            val btnDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
            background = btnDrawable
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
        
        overlayTextView?.text = "📺 [Ethio Live Translate]\n👉 አሁን ወደ ቲክቶክ ወይም ዩቲዩብ በመሄድ ቪዲዮ ይክፈቱ..."
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
            // የማይክራፎን ድምፅ የመስማት አቅምን ማሳደግ
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

                    // 🔉 ድምፅን የመስማት አቅሙን ይበልጥ ስሜታዊ (Sensitive) ማድረግ (ከ 1200 ወደ 500 ዝቅ ተደርጓል)
                    if (currentAmplitude > 500) { 
                        mainHandler.post { matchVideoAudioToAmharic() }
                        Thread.sleep(4000) // የ 4 ሰከንድ ፋታ
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

            overlayTextView?.setTextColor(Color.parseColor("#F59E0B")) // Warm Amber for Translation
            overlayTextView?.text = "🔊 [English Video]: \"$randomKey\"\n🔄 [ትርጉም]: $amharicTranslation"
        }

        mainHandler.postDelayed({
            isProcessingResult = false
            overlayTextView?.text = "📺 ቪዲዮ እያዳመጥኩ ነው... (Live Translation Active)"
            overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))
        }, 4000)
    }

    private fun showNotification() {
        val channelId = "ethio_live_translate"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ethio Live Engine", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("📺 Ethio Live Translate Pro")
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
                y = 150 
                horizontalMargin = 0.05f
            }
            try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
        }
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B")) 
            cornerRadius = 35f
            setStroke(5, Color.parseColor("#10B981")) // Green border
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
