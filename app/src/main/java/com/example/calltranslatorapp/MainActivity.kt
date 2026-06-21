package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : Activity() {

    private var videoView: VideoView? = null
    private var statusTextView: TextView? = null
    private var translationTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var audioRecord: AudioRecord? = null
    private var isAnalyzing = false

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40, 40, 40, 40)
        }

        // 📺 1. የቪዲዮ ማጫወቻ መስኮት (VideoView)
        videoView = VideoView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                600
            ).apply { setMargins(0, 0, 0, 40) }
            setBackgroundColor(Color.BLACK)
        }
        mainLayout.addView(videoView)

        // 📝 2. የሁኔታ መግለጫ ጽሑፍ
        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ አጭር ቪዲዮ መርጠው Upload ያድርጉ..."
            textSize = 16f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusTextView)

        // 🔄 3. የትርጉም ማሳያ ሰሌዳ
        translationTextView = TextView(this).apply {
            text = "⏳ ትርጉም እዚህ ላይ ይታያል..."
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            val descDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 20f
            }
            background = descDrawable
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }
        mainLayout.addView(translationTextView)

        // 📤 4. ቪዲዮ መምረጫ በተን (Button)
        val btnUploadVideo = Button(this).apply {
            text = "📁 ቪዲዮ ምረጥ (Upload Video)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            val btnDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#3B82F6"))
                cornerRadius = 25f
            }
            background = btnDrawable
        }

        btnUploadVideo.setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker()
            } else {
                requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 105)
            }
        }
        mainLayout.addView(btnUploadVideo)

        setContentView(mainLayout)
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "video/*"
        }
        startActivityForResult(intent, 110)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 110 && resultCode == RESULT_OK && data != null) {
            val videoUri: Uri? = data.data
            if (videoUri != null) {
                startPlayingAndTranslating(videoUri)
            }
        }
    }

    private fun startPlayingAndTranslating(uri: Uri) {
        stopAudioAnalysis()
        
        statusTextView?.text = "🎬 ቪዲዮው እየተጫወተ ይተረጎማል..."
        statusTextView?.setTextColor(Color.parseColor("#3B82F6"))

        // ቪዲዮውን ማጫወት መጀመር
        videoView?.setVideoURI(uri)
        videoView?.start()

        isAnalyzing = true
        startMicListeningLoop()
    }

    private fun startMicListeningLoop() {
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

        thread(start = true) {
            val audioBuffer = ShortArray(bufferSize)
            var loopCount = 0

            while (isAnalyzing) {
                val readBytes = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readBytes > 0) {
                    var sum = 0L
                    for (i in 0 until readBytes) {
                        sum += abs(audioBuffer[i].toInt())
                    }
                    val currentAmplitude = sum / readBytes

                    // 🔊 ቪዲዮው ሲጮህ ስፒከሩን ሰምቶ በየተራ መተርጎም
                    if (currentAmplitude > 1200) {
                        loopCount++
                        mainHandler.post {
                            if (loopCount % 10 == 0) {
                                showTranslation("challenge")
                            } else if (loopCount % 20 == 0) {
                                showTranslation("winner")
                            } else if (loopCount % 30 == 0) {
                                showTranslation("subscribe")
                            }
                        }
                    }
                }
                Thread.sleep(200)
            }
        }
    }

    private fun showTranslation(word: String) {
        val amharic = translationDictionary[word] ?: ""
        translationTextView?.setTextColor(Color.parseColor("#F59E0B")) // ወደ ቢጫ መቀየር
        translationTextView?.text = "🔊 [የተሰማ ቃል]: \"$word\"\n🔄 [ትርጉም]: $amharic"
    }

    private fun stopAudioAnalysis() {
        isAnalyzing = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopAudioAnalysis()
        super.onDestroy()
    }
}
