package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import java.io.File
import java.io.FileOutputStream
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
    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    
    private var isProcessing = false
    private var audioIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(30, 30, 30, 30)
        }

        // 🎬 ኦንላይን በቀጥታ የሚጫወትበት የቪዲዮ ማሳያ
        videoView = VideoView(this).apply {
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 700)
            lp.setMargins(0, 0, 0, 30)
            layoutParams = lp
        }
        mainLayout.addView(videoView)

        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ የእንግሊዝኛ ቪዲዮ ይምረጡ..."
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

        // 🎙️ የቀጥታ ትርጉም ማሳያ ሰሌዳ
        translationTextView = TextView(this).apply {
            text = "⚡ ቪዲዮው ሲጀምር አፑ ንግግሩን እየተከተለ በቀጥታ በአማርኛ ድምፅ መናገር ይጀምራል..."
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 25f
                setStroke(4, Color.parseColor("#3B82F6"))
            }
            val textLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            textLp.setMargins(0, 0, 0, 40)
            layoutParams = textLp
        }
        mainLayout.addView(translationTextView)

        val btnUploadVideo = Button(this).apply {
            text = "📁 ቪዲዮ ስቀልና በቀጥታ በአማርኛ አጫውት"
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
                startLiveStreamingTranslation(uri)
            }
        }
    }

    // 🚀 ቪዲዮው ሲጫን የእንግሊዝኛውን አጥፍቶ (Mute) ኦንላይን መተርጎም ይጀምራል
    private fun startLiveStreamingTranslation(uri: Uri) {
        isProcessing = true
        audioIndex = 0
        progressBar?.visibility = View.VISIBLE
        progressBar?.progress = 0
        
        statusTextView?.text = "📢 የእንግሊዝኛውን ድምፅ አጥፍቼ በአማርኛ ድምፅ እየተካሁት ነው..."
        statusTextView?.setTextColor(Color.parseColor("#F59E0B"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            mp.setVolume(0f, 0f) // 🔇 የቪዲዮውን የእንግሊዝኛ ድምፅ ሙሉ በሙሉ ያጠፋዋል!
            videoView?.start()
            startRealtimeSpeechListening()
        }
    }

    // 🎙️ ቪዲዮው ሲጫወት ንግግሩን በሪልታይም መስማት
    private fun startRealtimeSpeechListening() {
        if (videoView == null || !videoView!!.isPlaying) return
        
        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) { checkNextLiveChunk() }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val englishChunk = matches[0]
                            
                            thread {
                                // 1. ወዲያውኑ ኦንላይን ይተረጉመዋል
                                val amharicTranslation = translateOnline(englishChunk)
                                
                                // 2. የአማርኛውን ድምፅ ወዲያውኑ ያመነጫል
                                val liveAudio = downloadSingleAudioTrack(amharicTranslation)
                                
                                mainHandler.post {
                                    if (videoView != null && videoView!!.isPlaying) {
                                        val progress = (videoView!!.currentPosition * 100) / videoView!!.duration
                                        progressBar?.progress = progress
                                        
                                        translationTextView?.text = "🎙️ [ኦሪጅናል ንግግር]: \"$englishChunk\"\n\n🔄 [የቀጥታ አማርኛ ትርጉም]: $amharicTranslation"
                                        
                                        // 3. ቪዲዮውን እየተከተለ በአማርኛ ይናገራል!
                                        playLiveAmharicVoice(liveAudio)
                                    }
                                }
                            }
                        }
                        checkNextLiveChunk()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) { checkNextLiveChunk() }
        }
    }

    private fun checkNextLiveChunk() {
        if (videoView != null && videoView!!.isPlaying) {
            startRealtimeSpeechListening()
        } else {
            progressBar?.progress = 100
            statusTextView?.text = "🎉 የቪዲዮው ቀጥታ የአማርኛ ትርጉም በተሳካ ሁኔታ ተጠናቋል!"
            statusTextView?.setTextColor(Color.parseColor("#10B981"))
            isProcessing = false
        }
    }

    private fun translateOnline(text: String): String {
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

    private fun downloadSingleAudioTrack(text: String): File? {
        return try {
            audioIndex++
            val audioFile = File(cacheDir, "live_track_$audioIndex.mp3")
            val url = URL("https://translate.google.com/translate_tts?ie=UTF-8&tl=am&client=tw-ob&q=" + URLEncoder.encode(text, "UTF-8"))
            val con = url.openConnection() as HttpURLConnection
            con.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (con.responseCode == 200) {
                val fos = FileOutputStream(audioFile)
                con.inputStream.copyTo(fos)
                fos.close()
                audioFile
            } else null
        } catch (e: Exception) { null }
    }

    // 🔊 ቪዲዮውን እየተከተለ በአማርኛ ድምፅ እንዲናገር ማድረጊያ ሞተር
    private fun playLiveAmharicVoice(audioFile: File?) {
        if (audioFile == null || !audioFile.exists()) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        mediaPlayer?.release()
        super.onDestroy()
    }
}
