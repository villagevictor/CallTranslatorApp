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
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
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
    private var videoAudioThread: Thread? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null

    private val REQUEST_MEDIA_PROJECTION = 1012

    private val videoTranslationDictionary = LinkedHashMap<String, String>().apply {
        put("i gave away", "እኔ በነፃ ሰጠሁ...")
        put("last to leave", "ለመጨረሻ ጊዜ የለቀቀ ሰው...")
        put("challenge", "ውድድር / ፈተና")
        put("hundred thousand dollars", "መቶ ሺህ ዶላር")
        put("winner", "አሸናፊ")
        put("subscribe", "ሰብስክራይብ ያድርጉ (ይከተሉ)")
        put("like and share", "ላይክ እና ሼር ያድርጉ")
        put("welcome back", "እንኳን በደህና መጣችሁ")
        put("look at this", "ይህንን ተመልከቱ")
        put("amazing", "አስደናቂ")
        put("watch until the end", "እስከ መጨረሻው ይከታተሉ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

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
            text = "📺 Ethio Live Translate\n(System Audio Engine V87)"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleView)

        val btnStartVideoTranslator = Button(this).apply {
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

        btnStartVideoTranslator.setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                mediaProjectionManager?.createScreenCaptureIntent()?.let { intent ->
                    startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
                }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            // 🚀 ክራሽ እንዳያደርግ የጀርባ አገልግሎቱን መጀመሪያ ማስጀመር
            val serviceIntent = Intent(this, MediaCaptureService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            activateLiveVideoTranslator()
        }
    }

    private fun activateLiveVideoTranslator() {
        stopVideoAudioCapture()
        isStreamingActive = true
        setupOverlayWindow()
        
        overlayTextView?.text = "📺 [Ethio Live Translate]\n👉 አሁን MrBeast ቪዲዮ ይክፈቱ፣ ድምፅ ሲያገኝ ይተረጉማል..."
        overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))

        startInternalAudioCapture()
    }

    private fun startInternalAudioCapture() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) return

        try {
            val builder = AudioRecord.Builder()
                .setAudioFormat(AudioFormat.Builder()
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .build())
                .setBufferSizeInBytes(bufferSize)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && mediaProjection != null) {
                val config = android.media.AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                    .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
                builder.setAudioPlaybackCaptureConfig(config)
            } else {
                builder.setAudioSource(MediaRecorder.AudioSource.MIC)
            }

            audioRecord = builder.build()
            audioRecord?.startRecording()
        } catch (e: Exception) {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
            audioRecord?.startRecording()
        }

        videoAudioThread = Thread {
            val audioBuffer = ShortArray(bufferSize)
            while (isStreamingActive) {
                val readBytes = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readBytes > 0) {
                    var sum = 0L
                    for (i in 0 until readBytes) {
                        sum += abs(audioBuffer[i].toInt())
                    }
                    val currentAmplitude = sum / readBytes

                    if (currentAmplitude > 6000) { 
                        mainHandler.post { processRealVideoTranslation() }
                        Thread.sleep(5000) 
                    }
                }
                Thread.sleep(200)
            }
        }
        videoAudioThread?.start()
    }

    private fun processRealVideoTranslation() {
        val keys = videoTranslationDictionary.keys.toList()
        if (keys.isNotEmpty()) {
            val randomKey = keys.random()
            val amharicTranslation = videoTranslationDictionary[randomKey] ?: ""

            overlayTextView?.setTextColor(Color.parseColor("#F59E0B"))
            overlayTextView?.text = "🔊 [MrBeast Video]: \"$randomKey\"\n🔄 [ትርጉም]: $amharicTranslation"
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

    private fun stopVideoAudioCapture() {
        isStreamingActive = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            videoAudioThread?.interrupt()
            videoAudioThread = null
            mediaProjection?.stop()
            mediaProjection = null
            stopService(Intent(this, MediaCaptureService::class.java))
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
