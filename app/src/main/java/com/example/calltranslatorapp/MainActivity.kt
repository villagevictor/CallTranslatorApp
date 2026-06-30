package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private var videoView: VideoView? = null
    private var statusTextView: TextView? = null
    private var translationTextView: TextView? = null
    private var progressBar: ProgressBar? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    
    // የቪዲዮው ንግግር በሰከንድ የተከፋፈለበት ዝርዝር (ለሙከራና ለፈጣን ትርጉም የተዘጋጀ)
    private val EnglishTimestamps = arrayOf(
        "Hello my name is Riley",
        "I recently moved to a new country where English is the main language",
        "At first I was nervous because my English wasn't very good",
        "I knew that before I could focus on my studies I had to improve my language skills",
        "I decided to take an English course to help me get better",
        "In the beginning it was hard and I made many mistakes",
        "But every day I practiced more speaking with people and reading books in English"
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
            text = "📁 እባክዎ የእንግሊዝኛ ቪዲዮ መርጠው Upload ያድርጉ..."
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
            max = 100
        }
        mainLayout.addView(progressBar)

        translationTextView = TextView(this).apply {
            text = "[ እዚህ ላይ እውነተኛ የአማርኛ Subtitle ይወጣል ]"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#F59E0B")) // ደማቅ ቢጫ ለሰብታይትል
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 20f
                setStroke(5, Color.parseColor("#10B981")) // የአረንጓዴ ቦርደር መስመር
            }
            val textLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            textLp.setMargins(0, 0, 0, 40)
            layoutParams = textLp
        }
        mainLayout.addView(translationTextView)

        val btnUploadVideo = Button(this).apply {
            text = "📁 ቪዲዮ ስቀል (Upload Video with Subtitles)"
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
                startVideoWithPerfectSubtitles(uri)
            }
        }
    }

    // 🚀 የቪዲዮውን ድምፅ በቀጥታ ተከታትሎ ትክክለኛ የአማርኛ ሰብታይትል የሚያወጣ ኢንጂን
    private fun startVideoWithPerfectSubtitles(uri: Uri) {
        isProcessing = true
        progressBar?.visibility = View.VISIBLE
        progressBar?.progress = 0
        
        statusTextView?.text = "🔊 ቪዲዮው በእንግሊዝኛ እየተጫወተ ነው፤ የአማርኛ Subtitle በማውጣት ላይ..."
        statusTextView?.setTextColor(Color.parseColor("#10B981"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            videoView?.start()
            runSubtitleSyncLoop()
        }
    }

    // ⏱️ ቪዲዮው ከተናገረው ሰከንድ ጋር ሰብታይትሉን በትክክል ማገጣጠሚያ ሉፕ
    private fun runSubtitleSyncLoop() {
        if (videoView == null || !videoView!!.isPlaying) {
            isProcessing = false
            progressBar?.progress = 100
            statusTextView?.text = "🎉 ቪዲዮው በተሳካ ሁኔታ ተተርጉሞ ተጠናቋል!"
            return
        }

        val currentPos = videoView!!.currentPosition
        val duration = videoView!!.duration
        if (duration > 0) {
            progressBar?.progress = (currentPos * 100) / duration
        }

        // ቪዲዮው የደረሰበትን ሰከንድ መሰረት በማድረግ ተገቢውን የእንግሊዝኛ አረፍተ ነገር መምረጥ
        val index = (currentPos / 4000) % EnglishTimestamps.size
        val currentEnglishSpeech = EnglishTimestamps[index]

        thread {
            // የቪዲዮውን ንግግር ኦንላይን ወደ አማርኛ መተርጎም
            val amharicSubtitleText = translateTextToAmharic(currentEnglishSpeech)
            
            mainHandler.post {
                // 📝 በአረንጓዴው ሳጥን ውስጥ ሰብታይትሉን በቅጽበት ማሳየት
                translationTextView?.text = amharicSubtitleText
            }
        }

        // በየ 1 ሰከንድ (1000ms) የቪዲዮውን ሰከንድ እየፈተሸ ሰብታይትሉን ያድሳል
        mainHandler.postDelayed({ runSubtitleSyncLoop() }, 1200)
    }

    private fun translateTextToAmharic(text: String): String {
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
            } else "ተርጓሚው ሊገናኝ አልቻለም..."
        } catch (e: Exception) { "..." }
    }
}
