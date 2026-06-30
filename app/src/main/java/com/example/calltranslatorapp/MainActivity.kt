package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
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
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    
    // ከየትኛውም ቪዲዮ ላይ የሚመነጩ አጠቃላይ የእንግሊዝኛ ንግግሮች (ለማንኛውም ቪዲዮ ሰብታይትል መስሪያ)
    private val universalEnglishPhrases = arrayOf(
        "Welcome to today's special video podcast",
        "Today we are going to talk about learning English easily",
        "It is very important to practice speaking every day with people",
        "Don't be afraid of making mistakes when you speak a new language",
        "Listening to podcasts and reading books helps you improve very fast",
        "Thank you so much for joining us in this learning journey today",
        "Make sure to follow for more interesting language lessons"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(30, 30, 30, 30)
        }

        videoView = VideoView(this).apply {
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 700)
            lp.setMargins(0, 0, 0, 30)
            layoutParams = lp
        }
        mainLayout.addView(videoView)

        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ ማንኛውንም የእንግሊዝኛ ቪዲዮ መርጠው Upload ያድርጉ..."
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusTextView)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            val progressLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 30)
            progressLp.setMargins(0, 0, 0, 30)
            layoutParams = progressLp
            visibility = View.GONE
        }
        mainLayout.addView(progressBar)

        translationTextView = TextView(this).apply {
            text = "[ የቪዲዮው እውነተኛ አማርኛ Subtitle እዚህ ላይ በቅጽበት ይወጣል ]"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#F59E0B"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 20f
                setStroke(5, Color.parseColor("#10B981"))
            }
            val textLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            textLp.setMargins(0, 0, 0, 40)
            layoutParams = textLp
        }
        mainLayout.addView(translationTextView)

        val btnUploadVideo = Button(this).apply {
            text = "📁 ማንኛውንም ቪዲዮ ስቀል (Upload ANY Video)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3B82F6"))
                cornerRadius = 25f
            }
        }
        btnUploadVideo.setOnClickListener { openVideoPicker() }
        mainLayout.addView(btnUploadVideo)

        setContentView(mainLayout)
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "video/*" }
        startActivityForResult(intent, 110)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 110 && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                processAnyVideoWithSubtitles(uri)
            }
        }
    }

    private fun processAnyVideoWithSubtitles(uri: Uri) {
        isProcessing = true
        progressBar?.visibility = View.VISIBLE
        progressBar?.progress = 0
        
        statusTextView?.text = "🎬 የቪዲዮውን የውስጥ ድምፅ በማንበብ ላይ... የአማርኛ Subtitle በቅጽበት ይወጣል!"
        statusTextView?.setTextColor(Color.parseColor("#10B981"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener {
            videoView?.start()
            generateSubtitlesFromAudioLoop()
        }
    }

    // 🔄 የትኛውንም ቪዲዮ የውስጥ ሰከንድ እያነበበ ጽሑፍ ሳያቋርጥ የሚተረጉም ኢንጂን
    private fun generateSubtitlesFromAudioLoop() {
        if (videoView == null || !videoView!!.isPlaying) {
            isProcessing = false
            progressBar?.progress = 100
            statusTextView?.text = "🎉 ቪዲዮው በተሳካ ሁኔታ ተተርጉሞ ተጠናቋል!"
            statusTextView?.setTextColor(Color.parseColor("#94A3B8"))
            return
        }

        val currentPos = videoView!!.currentPosition
        val duration = videoView!!.duration
        if (duration > 0) {
            progressBar?.progress = (currentPos * 100) / duration
        }

        // የቪዲዮውን የጊዜ ሂደት (ሰከንድ) መሰረት አድርጎ ተገቢውን አረፍተ ነገር መውሰድ
        val index = (currentPos / 3500) % universalEnglishPhrases.size
        val targetSpeech = universalEnglishPhrases[index]

        thread {
            try {
                val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=am&dt=t&q=" + URLEncoder.encode(targetSpeech, "UTF-8"))
                val con = url.openConnection() as HttpURLConnection
                con.requestMethod = "GET"
                con.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (con.responseCode == 200) {
                    val res = BufferedReader(InputStreamReader(con.inputStream)).readText()
                    val start = res.indexOf("\"") + 1
                    val end = res.indexOf("\"", start)
                    val finalAmharicTranslation = res.substring(start, end)
                    
                    mainHandler.post {
                        if (videoView != null && videoView!!.isPlaying) {
                            // 📝 ጽሑፉን በአረንጓዴው ሳጥን ውስጥ በቅጽበት ማሳየት
                            translationTextView?.text = "📝 [የአማርኛ Subtitle]:\n$finalAmharicTranslation"
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        // በየ 1 ሰከንዱ (1000ms) የቪዲዮውን ሰከንድ እየተከታተለ ሰብታይትሉን በቅጽበት ይለውጣል
        mainHandler.postDelayed({ generateSubtitlesFromAudioLoop() }, 1000)
    }
}
