package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
    private var speechRecognizer: SpeechRecognizer? = null
    private var isProcessing = false
    private var speechIntent: Intent? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // የስልኩን ሚዲያ ድምፅ በራስ-ሰር ወደ ከፍተኛ ደረጃ ከፍ ማድረግ (ማይክሮፎኑ በደንብ እንዲሰማው)
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {}

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
            max = 100
        }
        mainLayout.addView(progressBar)

        translationTextView = TextView(this).apply {
            text = "[ እዚህ ላይ የቪዲዮው እውነተኛ የአማርኛ Subtitle በቅጽበት ይወጣል ]"
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

        // የድምፅ መፈለጊያ ኢንቴንትን ማዘጋጀት
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "video/*" }
        startActivityForResult(intent, 110)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 110 && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                startUniversalSubtitleEngine(uri)
            }
        }
    }

    private fun startUniversalSubtitleEngine(uri: Uri) {
        isProcessing = true
        progressBar?.visibility = View.VISIBLE
        progressBar?.progress = 0
        
        statusTextView?.text = "📢 የቪዲዮውን ድምፅ በቀጥታ እየሰማሁ ወደ አማርኛ Subtitle እየቀየርኩ ነው..."
        statusTextView?.setTextColor(Color.parseColor("#10B981"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            videoView?.start()
            startContinuousListeningLoop()
            runProgressBarUpdateLoop()
        }
    }

    // 🎙️ የትኛውንም የቪዲዮ ድምፅ በቀጥታ የሚሰማ እና ያለማቋረጥ የሚሰራል ሉፕ
    private fun startContinuousListeningLoop() {
        if (videoView == null || !videoView!!.isPlaying) return

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    
                    override fun onError(error: Int) {
                        // ስህተት ቢፈጠርም እንኳ ሳይቆም የትኛውንም ቪዲዮ ማዳመጡን ይቀጥላል
                        mainHandler.postDelayed({ startContinuousListeningLoop() }, 400)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val liveEnglishSpeech = matches[0]
                            
                            thread {
                                // የተሰማውን ማንኛውንም አዲስ ንግግር ወዲያውኑ መተርጎም
                                val dynamicSubtitle = translateOnlineDynamic(liveEnglishSpeech)
                                mainHandler.post {
                                    if (videoView != null && videoView!!.isPlaying) {
                                        translationTextView?.text = "📝 [የአማርኛ Subtitle]:\n$dynamicSubtitle"
                                    }
                                }
                            }
                        }
                        startContinuousListeningLoop()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // ለበለጠ ፍጥነት በከፊል የተሰሙትንም ጭምር በቅጽበት ያሳያል
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partialEnglish = matches[0]
                            thread {
                                val dynamicSubtitle = translateOnlineDynamic(partialEnglish)
                                mainHandler.post {
                                    if (videoView != null && videoView!!.isPlaying) {
                                        translationTextView?.text = "📝 [የአማርኛ Subtitle...]:\n$dynamicSubtitle"
                                    }
                                }
                            }
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(speechIntent)
            } catch (e: Exception) {
                mainHandler.postDelayed({ startContinuousListeningLoop() }, 400)
            }
        }
    }

    private fun runProgressBarUpdateLoop() {
        if (videoView != null && videoView!!.isPlaying) {
            val currentPos = videoView!!.currentPosition
            val duration = videoView!!.duration
            if (duration > 0) {
                progressBar?.progress = (currentPos * 100) / duration
            }
            mainHandler.postDelayed({ runProgressBarUpdateLoop() }, 1000)
        } else if (videoView != null && !videoView!!.isPlaying && isProcessing) {
            isProcessing = false
            progressBar?.progress = 100
            statusTextView?.text = "🎉 ቪዲዮው ተጠናቋል!"
            statusTextView?.setTextColor(Color.parseColor("#94A3B8"))
        }
    }

    private fun translateOnlineDynamic(text: String): String {
        if (text.trim().isEmpty()) return "..."
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

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
