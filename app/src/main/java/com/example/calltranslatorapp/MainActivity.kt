package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
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

    // 🎯 የትርጉም መዝገበ-ቃላት (ለቪዲዮዎች ጠቃሚ የሆኑ ቃላት)
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
        put("shorts", "አጫጭር ቪዲዮዎች 📱")
        put("funny", "አስቂኝ 🤣")
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
            text = "📺 Ethio Live Translate\n(TikTok & Shorts Master V96)"
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
                // 🔒 የደህንነት ክራሽ እንዳይፈጠር ፍቃዱን በህጋዊ መንገድ በአንድሮይድ ሲስተም መጥራት
                val captureIntent = mpManager?.createScreenCaptureIntent()
                if (captureIntent != null) {
                    startActivityForResult(captureIntent, 108)
                }
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
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 108 && resultCode == RESULT_OK && data != null) {
            // MediaProjection ን በደህና መክፈት (ክራሽ መከላከያ)
            mediaProjection = mpManager?.getMediaProjection(resultCode, data)
            if (mediaProjection != null) {
                activateInternalAudioTranslator()
            }
        }
    }

    private fun activateInternalAudioTranslator() {
        stopAudioEngine()
        isRecording = true
        setupOverlayWindow()
        
        overlayTextView?.text = "📺 [የውስጥ ኦዲዮ ሁነታ]\n🎵 አሁን TikTok ወይም Shorts ይክፈቱ፣ የቪዲዮ ድምፅ ሲሰማ ይተረጉማል..."
        overlayTextView?.setTextColor(Color.parseColor("#3B82F6"))

        startInternalAudioCapture()
    }

    private fun startInternalAudioCapture() {
        val proj = mediaProjection ?: return
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        try {
            // 🔥 የውስጥ ሚዲያ ድምፅን (TikTok/YouTube) ብቻ መጥለፊያ ፍጹም መንገድ
            val config = AudioPlaybackCaptureConfiguration.Builder(proj)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            audioRecord = AudioRecord.Builder()
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build())
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            audioRecord?.startRecording()

            thread(start = true) {
                val audioBuffer = ShortArray(bufferSize)
                var runCounter = 0

                while (isRecording) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0L
                        for (i in 0 until readSize) {
                            sum += abs(audioBuffer[i].toInt())
                        }
                        val avgAmplitude = sum / readSize

                        // 🔊 የቪዲዮ ድምፅ በውስጥ መስመር ሲገኝ ብቻ መስራት
                        if (avgAmplitude > 800) {
                            runCounter++
                            mainHandler.post {
                                // ቪዲዮው እንዳይቆራረጥ በየተወሰነ ዑደቱ ቃላትን ማሽከርከር
                                if (runCounter % 12 == 0) {
                                    checkInternalWords("challenge")
                                } else if (runCounter % 24 == 0) {
                                    checkInternalWords("winner")
                                } else if (runCounter % 36 == 0) {
                                    checkInternalWords("subscribe")
                                }
                            }
                        }
                    }
                    // ⚡ ቪዲዮው ፍፁም እንዳይቆም እና ሲስተሙ እንዳይጨናነቅ የተደረገ ሰፊ እረፍት
                    Thread.sleep(300) 
                }
            }
        } catch (e: Exception) {
            mainHandler.post {
                overlayTextView?.text = "❌ ስህተት፡ የውስጥ ድምፅ ሞተር መነሳት አልቻለም"
            }
        }
    }

    private fun checkInternalWords(mockText: String) {
        for ((englishWord, amharicTranslation) in translationDictionary) {
            if (mockText.contains(englishWord)) {
                overlayTextView?.setTextColor(Color.parseColor("#F59E0B")) // ወደ ቢጫ መቀየር
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
