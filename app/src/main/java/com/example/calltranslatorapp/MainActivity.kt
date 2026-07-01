package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Camera
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : Activity(), SurfaceHolder.Callback {

    private var camera: Camera? = null
    private var surfaceView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    
    private var statusTitle: TextView? = null
    private var subtitleText: TextView? = null
    private var btnCallAction: Button? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var isCallActive = false
    private var callDurationSec = 0
    private var audioIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. ዋናው የቪዲዮ መደወያ ስክሪን (Root Layout)
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        // 2. የቪዲ视频 ካሜራ እይታ (Local/Remote Video Feed Layer)
        surfaceView = SurfaceView(this)
        surfaceHolder = surfaceView!!.holder
        surfaceHolder!!.addCallback(this)
        rootLayout.addView(surfaceView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // 3. የቁጥጥር እና የትርጉም ሌየር (Overlay Control UI Layer)
        val overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(40, 40, 40, 60)
        }

        statusTitle = TextView(this).apply {
            text = "🌐 IMO-Style Real-Time AI Translator Call"
            textSize = 18f
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        overlayLayout.addView(statusTitle)

        // 🔄 በቅጽበት የተተረጎመውን የአማርኛ ንግግር የሚያሳየው ሰሌዳ
        subtitleText = TextView(this).apply {
            text = "የእውነተኛ ጊዜ የድምፅ ትርጉም ዝግጁ ነው..."
            textSize = 16f
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B30F172A")) // ከፊል ግልፅ ጥቁር ባክግራውንድ
                cornerRadius = 15f
                setStroke(2, Color.parseColor("#10B981"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(20, 0, 20, 40) }
        }
        overlayLayout.addView(subtitleText)

        // 📞 ጥሪ መጀመርያ እና መዝጊያ ቁልፍ
        btnCallAction = Button(this).apply {
            text = "🟢 ስልክ ጥራ (Start Online Video Call)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(60, 40, 60, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 30f
            }
        }
        btnCallAction!!.setOnClickListener { toggleInAppVideoCall() }
        overlayLayout.addView(btnCallAction)

        rootLayout.addView(overlayLayout)
        setContentView(rootLayout)
    }

    private fun toggleInAppVideoCall() {
        if (!isCallActive) {
            isCallActive = true
            callDurationSec = 0
            statusTitle?.text = "📞 በጥሪ ላይ ነዎት... (00:00)"
            statusTitle?.setTextColor(Color.parseColor("#EF4444"))
            btnCallAction?.text = "🔴 ጥሪውን ዝጋ (Disconnect)"
            btnCallAction?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444"))
                cornerRadius = 30f
            }
            // የውስጥ ድምፅ ማስተላለፊያ እና የትርጉም ሉፕ አስነሳ
            startRealTimeInAppTranslationLoop()
        } else {
            isCallActive = false
            statusTitle?.text = "🌐 IMO-Style Real-Time AI Translator Call"
            statusTitle?.setTextColor(Color.WHITE)
            subtitleText?.text = "ጥሪው ተቋርጧል።"
            btnCallAction?.text = "🟢 ስልክ ጥራ (Start Online Video Call)"
            btnCallAction?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 30f
            }
            mediaPlayer?.release()
        }
    }

    // 🎙️ በጥሪው መስመር ውስጥ የሚመጣውን የኦዲዮ ፍሰት በቀጥታ የሚሰማ እና በድምፅ የሚተረጉም ዋና ሞተር
    private fun startRealTimeInAppTranslationLoop() {
        if (!isCallActive) return

        thread {
            try {
                // አስመሳይ የተደዋዋጭ የእንግሊዝኛ ንግግሮች (በእውነተኛ አፕ ላይ ከዌብ ሶኬት/WebRTC የኦዲዮ ስትሪም የሚተካ)
                val incomingEnglishVoiceStream = when (callDurationSec) {
                    0 -> "Hello my friend, can you hear me clearly over this app?"
                    1 -> "This is amazing, we are talking directly with no speaker restrictions."
                    2 -> "The server is processing my speech and converting it instantly."
                    3 -> "Let us meet tomorrow to finish our system configuration."
                    else -> ""
                }

                if (incomingEnglishVoiceStream.isNotEmpty()) {
                    // 1. የመስመር ላይ ፈጣን የጽሑፍ ትርጉም
                    val amharicText = translateLiveStream(incomingEnglishVoiceStream)
                    
                    // 2. የአማርኛ ድምፅ ማመንጨት (In-App Audio Dubbing)
                    val voiceFile = downloadLiveTtsTrack(amharicText)

                    mainHandler.post {
                        if (isCallActive) {
                            subtitleText?.text = "🗣️ [እንግሊዝኛ]: \"$incomingEnglishVoiceStream\"\n\n🇪🇹 [አማርኛ]: $amharicText"
                            // 3. ድምፅን በቀጥታ በጆሮ ማዳመጫ ወይም በስልክ መስመር ማጫወት
                            playLiveAmharicStream(voiceFile)
                        }
                    }
                }

                callDurationSec++
                val minutes = callDurationSec * 5 / 60
                val seconds = callDurationSec * 5 % 60
                mainHandler.post {
                    statusTitle?.text = String.format("📞 በጥሪ ላይ ነዎት... (%02d:%02d)", minutes, seconds)
                }

                // በየ 5 ሰከንዱ የሚመጣውን አዲስ የድምፅ ፍሰት ተከታትሎ ይተረጉማል
                mainHandler.postDelayed({ startRealTimeInAppTranslationLoop() }, 5000)

            } catch (e: Exception) {
                mainHandler.postDelayed({ startRealTimeInAppTranslationLoop() }, 2000)
            }
        }
    }

    private fun translateLiveStream(text: String): String {
        return try {
            val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=am&dt=t&q=" + URLEncoder.encode(text, "UTF-8"))
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (con.responseCode == 200) {
                val res = BufferedReader(InputStreamReader(con.inputStream)).readText()
                val start = res.indexOf("\"") + 1
                val end = res.indexOf("\"", start)
                res.substring(start, end)
            } else "..."
        } catch (e: Exception) { "..." }
    }

    private fun downloadLiveTtsTrack(text: String): File? {
        return try {
            audioIndex++
            val file = File(cacheDir, "live_stream_$audioIndex.mp3")
            val url = URL("https://translate.google.com/translate_tts?ie=UTF-8&tl=am&client=tw-ob&q=" + URLEncoder.encode(text, "UTF-8"))
            val con = url.openConnection() as HttpURLConnection
            con.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (con.responseCode == 200) {
                val fos = FileOutputStream(file)
                con.inputStream.copyTo(fos)
                fos.close()
                file
            } else null
        } catch (e: Exception) { null }
    }

    private fun playLiveAmharicStream(file: File?) {
        if (file == null || !file.exists() || !isCallActive) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION) // በቀጥታ ለጥሪ መስመር የተመደበ የኦዲዮ ቻናል
                    .build())
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {}
    }

    // የካሜራ አስተዳደር (SurfaceHolder Callbacks)
    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera = Camera.open(1) // የፊት ካሜራ (Front Camera) ለመክፈት
            camera?.setPreviewDisplay(holder)
            camera?.setDisplayOrientation(90) // ምስሉ ቀጥ ብሎ እንዲታይ
            camera?.startPreview()
        } catch (e: Exception) {}
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
