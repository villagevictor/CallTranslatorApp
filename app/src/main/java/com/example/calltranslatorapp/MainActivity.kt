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
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var mpManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // 🎯 የትርጉም መዝገበ-ቃላት
    private val translationDictionary = LinkedHashMap<String, String>().apply {
        put("challenge", "ውድድር / ፈተና 🏆")
        put("winner", "አሸናፊ 🎉")
        put("subscribe", "ሰብስክራይብ ያድርጉ (ይከተሉ)")
        put("amazing", "አስደናቂ! ✨")
        put("money", "ገንዘብ / ዶላር 💵")
        put("dollars", "ዶላር 💵")
        put("friend", "ጓደኛ 🤝")
        put("video", "ቪዲዮ 🎬")
        put("survive", "በህይወት መቆየት / መትረፍ 🏹")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

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
            text = "📺 Ethio Live Translate\n(System Audio Engine V94)"
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
                // 🚀 የሲስተም የውስጥ ድምፅ መፍቀጃ ጥያቄ ማንሳት
                startActivityForResult(mpManager?.createScreenCaptureIntent(), 108)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 108 && resultCode == RESULT_OK && data != null) {
            mediaProjection = mpManager?.getMediaProjection(resultCode, data)
            activateInternalAudioTranslator()
        }
    }

    private fun activateInternalAudioTranslator() {
        stopAudioEngine()
        isRecording = true
        setupOverlayWindow()
        
        overlayTextView?.text = "📺 [System Audio Mode]\n🎵 TikTok ወይም YouTube ቪዲዮ ሲከፍቱ በውስጥ መስመር መስማት ይጀምራል..."
        overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))

        startInternalAudioCapture()
    }

    private fun startInternalAudioCapture() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        try {
            // 🔥 ከማይክሮፎን ውጭ ስልኩ ውስጥ የሚጫወተውን ድምፅ ብቻ የመቅጃ ሚስጥር (REMOTE_SUBMIX)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.REMOTE_SUBMIX,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()

            thread(start = true) {
                val audioBuffer = ShortArray(bufferSize)
                var silentCount = 0

                while (isRecording) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readSize > 0) {
                        // 📊 የውስጥ ድምፅ ሞገዱን መለካት (ቪዲዮው ሲጫወት ብቻ እንዲሰራ)
                        var maxAmplitude = 0
                        for (i in 0 until readSize) {
                            val value = abs(audioBuffer[i].toInt())
                            if (value > maxAmplitude) maxAmplitude = value
                        }

                        mainHandler.post {
                            if (maxAmplitude > 100) { // ቪዲዮው እየተጫወተ ድምፅ ካለው
                                silentCount = 0
                                // 🔄 ለሙከራ ያህል በውስጥ የተሰማውን ድምፅ በመዝገበ ቃላት ፈልገን እናሳይ
                                checkInternalWords("challenge") 
                            } else {
                                silentCount++
                                if (silentCount > 30) {
                                    overlayTextView?.setTextColor(Color.parseColor("#10B981"))
                                    overlayTextView?.text = "🎙️ [የውስጥ መስመር]፡ ቪዲዮ እየተጠበቀ ነው..."
                                }
                            }
                        }
                    }
                    Thread.sleep(100) // ⚡ ስልኩ እና ቪዲዮው እንዳይጨናነቅ/እንዳይቆም ማድረጊያ
                }
            }
        } catch (e: Exception) {
            overlayTextView?.text = "❌ ስህተት፡ የውስጥ ድምፅ ሞተር አልተነሳም"
        }
    }

    private fun checkInternalWords(mockText: String) {
        for ((englishWord, amharicTranslation) in translationDictionary) {
            if (mockText.contains(englishWord)) {
                overlayTextView?.setTextColor(Color.parseColor("#F59E0B"))
                overlayTextView?.text = "🔊 [ቪዲዮው ውስጥ የተሰማ]: \"$englishWord\"\n🔄 [ትርጉም]: $amharicTranslation"
                break
            }
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

    private fun stopAudioEngine() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopAudioEngine()
        try {
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
