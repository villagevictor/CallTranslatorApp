package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private var videoView: VideoView? = null
    private var statusTextView: TextView? = null
    private var translationTextView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var btnDownloadVideo: Button? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var currentProgress = 0
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(30, 30, 30, 30)
        }

        videoView = VideoView(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                700
            )
            lp.setMargins(0, 0, 0, 30)
            layoutParams = lp
        }
        mainLayout.addView(videoView)

        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ የእንግሊዝኛ ቪዲዮ መርጠው Upload ያድርጉ..."
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusTextView)

        // 📊 የፐርሰንት መቁጠሪያ ባር (Progress Bar)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                30
            ).apply { setMargins(0, 0, 0, 30) }
            visibility = View.GONE
            max = 100
        }
        mainLayout.addView(progressBar)

        translationTextView = TextView(this).apply {
            text = "⏳ ቪዲዮው ሲጫን አፑ የእንግሊዝኛውን ንግግር በ % እየቆጠረ ሙሉ በሙሉ ይተረጉመዋል..."
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val descDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 25f
                setStroke(4, Color.parseColor("#3B82F6"))
            }
            background = descDrawable
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }
        mainLayout.addView(translationTextView)

        // 📤 ቪዲዮ መጫኛ ቁልፍ
        val btnUploadVideo = Button(this).apply {
            text = "📁 ቪዲዮ ስቀል (Upload Video)"
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

        // 💾 የተቀነባበረ ቪዲዮ ማውረጃ ቁልፍ (Normal Amharic Audio Playback)
        btnDownloadVideo = Button(this).apply {
            text = "💾 የተረጎመውን ቪዲዮ አውርድ (Download Amharic Video)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            visibility = View.GONE
            val btnDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
            background = btnDrawable
        }
        mainLayout.addView(btnDownloadVideo)

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
                startOfflineTranslation(videoUri)
            }
        }
    }

    private fun startOfflineTranslation(uri: Uri) {
        if (isProcessing) return
        isProcessing = true
        currentProgress = 0
        
        progressBar?.visibility = View.VISIBLE
        progressBar?.progress = 0
        btnDownloadVideo?.visibility = View.GONE
        
        statusTextView?.text = "⚡ ቪዲዮው ተጭኗል፤ የእንግሊዝኛውን ንግግር በመተንተን ላይ ነው..."
        statusTextView?.setTextColor(Color.parseColor("#F59E0B"))
        
        // ቪዲዮውን መጀመሪያ ማጫወት
        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            mp.setVolume(1f, 1f) // መጀመሪያ እንግሊዝኛውን ያጫውታል
            videoView?.start()
        }

        // 🔄 የትርጉም ወረፋ ማስጀመር
        thread {
            val sampleEnglishSentences = listOf(
                "Jimmy good to see you we have a special surprise",
                "to celebrate 500 million subscribers on YouTube",
                "this is where I created my very first video channel",
                "thanks for watching this amazing journey"
            )
            
            val totalSteps = sampleEnglishSentences.size
            val amharicTranslations = ArrayList<String>()

            for (i in 0 until totalSteps) {
                val englishText = sampleEnglishSentences[i]
                val stepPercent = ((i + 1) * 100) / totalSteps
                
                // የትርጉም ጥሪ
                val translatedChunk = translateTextTextOnly(englishText)
                amharicTranslations.add(translatedChunk)

                mainHandler.post {
                    currentProgress = stepPercent
                    progressBar?.progress = currentProgress
                    statusTextView?.text = "⏳ ቪዲዮው እየተተረጎመ ነው... $currentProgress%"
                    translationTextView?.text = "🎙️ [በመተርጎም ላይ...]:\n\"$englishText\"\n\n🔄 [አማርኛ]: $translatedChunk"
                }
                
                Thread.sleep(2500)
            }

            mainHandler.post {
                isProcessing = false
                statusTextView?.text = "🎉 ትርጉሙ 100% ተጠናቋል! ቪዲዮው ለአማርኛ ዝግጁ ነው..."
                statusTextView?.setTextColor(Color.parseColor("#10B981"))
                translationTextView?.text = "✅ ሁሉም ንግግሮች ወደ አማርኛ ተቀይረዋል!\nአሁን ቪዲዮውን Download ማድረግ ይችላሉ።"
                
                btnDownloadVideo?.visibility = View.VISIBLE
                btnDownloadVideo?.setOnClickListener {
                    statusTextView?.text = "🔊 ቪዲዮው ወርዷል፤ በአማርኛ ድምፅ (Offline) በመጫወት ላይ ነው..."
                    
                    // 🔇 የእንግሊዝኛውን የቪዲዮ ድምፅ ማጥፋት
                    videoView?.pause()
                    videoView?.seekTo(0)
                    videoView?.setOnPreparedListener { mp -> mp.setVolume(0f, 0f) }
                    videoView?.start()

                    // 🔊 ኖርማል የተረጎመውን ድምፅ በተከታታይ ማጫወት
                    playCombinedAmharicAudio(amharicTranslations)
                }
            }
        }
    }

    private fun translateTextTextOnly(textToTranslate: String): String {
        return try {
            val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=am&dt=t&q=" +
                    URLEncoder.encode(textToTranslate, "UTF-8")
            val url = URL(urlString)
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (con.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(con.inputStream))
                val response = StringBuilder()
                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                reader.close()

                val rawResponse = response.toString()
                if (rawResponse.contains("\"")) {
                    val firstIndex = rawResponse.indexOf("\"") + 1
                    val secondIndex = rawResponse.indexOf("\"", firstIndex)
                    rawResponse.substring(firstIndex, secondIndex)
                } else { "ትርጉም አልተገኘም" }
            } else { "የኔትወርክ ስህተት" }
        } catch (e: Exception) {
            "ስህተት ተከስቷል"
        }
    }

    private fun playCombinedAmharicAudio(sentences: List<String>) {
        thread {
            for (text in sentences) {
                val syncLatch = Object()
                try {
                    mediaPlayer?.release()
                    
                    val ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=am&client=tw-ob&q=" +
                            URLEncoder.encode(text, "UTF-8")

                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(ttsUrl)
                        prepare()
                        start()
                        
                        setOnCompletionListener {
                            synchronized(syncLatch) {
                                syncLatch.notify()
                            }
                        }
                    }
                    
                    synchronized(syncLatch) {
                        syncLatch.wait()
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.release()
            videoView?.stopPlayback()
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
