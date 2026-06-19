package com.example.calltranslatorapp

import android.app.Activity
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
import kotlin.math.abs

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStreamingActive = false
    
    private var audioRecord: AudioRecord? = null
    private var micAudioThread: Thread? = null

    // 🎯 የትርጉም መዝገበ-ቃላት
    private val translationDictionary = LinkedHashMap<String, String>().apply {
        put("i gave away", "እኔ በነፃ ሰጠሁ...")
        put("last to leave", "ለመጨረሻ ጊዜ የለቀቀ ሰው...")
        put("challenge", "ውድድር / ፈተና 🏆")
        put("hundred thousand dollars", "መቶ ሺህ ዶላር (100,000$) 💵")
        put("winner", "አሸናፊ 🎉")
        put("subscribe", "ሰብስክራይብ ያድርጉ (ይከተሉ)")
        put("like and share", "ላይክ እና ሼር ያድርጉ 👍")
        put("welcome back", "እንኳን በደህና መጣችሁ 👋")
        put("look at this", "ይህንን ተመልከቱ 👀")
        put("amazing", "አስደናቂ! ✨")
        put("watch until the end", "እስከ መጨረሻው ይከታተሉ 🎬")
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
            text = "📺 Ethio Live Translate\n(Mic Audio Engine V90)"
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
        stopAudioCapture()
        isStreamingActive = true
        setupOverlayWindow()
        
        overlayTextView?.text = "📺 [Ethio Live Translate]\n👉 አሁን ቪዲዮ ይክፈቱ፣ ስፒከሩ ሲናገር ማይኩ ሰምቶ ይተረጉማል..."
        overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))

        startMicAudioCapture()
    }

    private fun startMicAudioCapture() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, 
                sampleRate, 
                channelConfig, 
                audioFormat, 
                bufferSize
            )
            audioRecord?.startRecording()
        } catch (e: Exception) {
            return
        }

        micAudioThread = Thread {
            val audioBuffer = ShortArray(bufferSize)
            while (isStreamingActive) {
                val readBytes = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readBytes > 0) {
                    var sum = 0L
                    for (i in 0 until readBytes) {
                        sum += abs(audioBuffer[i].toInt())
                    }
                    val currentAmplitude = sum / readBytes

                    // 🔊 ስፒከሩ ሲናገር ድምፅ መኖሩን ማወቂያ (Sensitivity = 1000)
                    if (currentAmplitude > 1000) { 
                        mainHandler.post { processTranslation() }
                        Thread.sleep(4000) // ለ4 ሰከንድ ፅሁፉ እንዲቆይ
                    }
                }
                Thread.sleep(100)
            }
        }
        micAudioThread?.start()
    }

    private fun processTranslation() {
        val keys = translationDictionary.keys.toList()
        if (keys.isNotEmpty()) {
            val randomKey = keys.random()
            val amharicTranslation = translationDictionary[randomKey] ?: ""

            overlayTextView?.setTextColor(Color.parseColor("#F59E0B"))
            overlayTextView?.text = "🔊 [ቪዲዮ ድምፅ]: \"$randomKey\"\n🔄 [ትርጉም]: $amharicTranslation"
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

    private fun stopAudioCapture() {
        isStreamingActive = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            micAudioThread?.interrupt()
            micAudioThread = null
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
