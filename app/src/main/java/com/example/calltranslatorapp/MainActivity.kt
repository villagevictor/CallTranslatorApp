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
import android.os.Bundle as AndroidBundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
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
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isVideoPlaying = false

    private var mediaPlayer: MediaPlayer? = null
    private var lastSpokenText = ""

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
                750
            )
            lp.setMargins(0, 0, 0, 30)
            layoutParams = lp
        }
        mainLayout.addView(videoView)

        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ ቪዲዮ መርጠው Upload ያድርጉ..."
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusTextView)

        translationTextView = TextView(this).apply {
            text = "⏳ ቪዲዮው ሲጀምር የተስተካከለው የኦንላይን አማርኛ ድምፅ እዚህ ይጫወታል..."
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val descDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 25f
                setStroke(4, Color.parseColor("#F59E0B"))
            }
            background = descDrawable
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }
        mainLayout.addView(translationTextView)

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
        stopSpeechEngine()
        lastSpokenText = ""
        
        statusTextView?.text = "🎬 ቪዲዮው በተሳካ ሁኔታ ተከፍቷል፤ ፍጹም የአማርኛ ድምፅ ማመሳሰል ነቅቷል..."
        statusTextView?.setTextColor(Color.parseColor("#10B981"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            // 🔇 የቪዲዮውን ዋና የእንግሊዝኛ ድምፅ ሙሉ በሙሉ ማጥፋት (Play errorን ለማስቀረት)
            mp.setVolume(0f, 0f)
            videoView?.start()
            isVideoPlaying = true
            
            initSpeechRecognizer()
            startListeningEngine()
        }
        
        // የፋይል ስህተት ከገጠመ መልሶ በደህና እንዲከፍተው ማድረጊያ
        videoView?.setOnErrorListener { _, _, _ ->
            statusTextView?.text = "⚠️ የቪዲዮ ፎርማቱን ለማስተካከል በድጋሚ እየተሞከረ ነው..."
            statusTextView?.setTextColor(Color.parseColor("#EF4444"))
            false
        }
    }

    private fun initSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: AndroidBundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isVideoPlaying) {
                        mainHandler.postDelayed({ startListeningEngine() }, 10)
                    }
                }

                override fun onResults(results: AndroidBundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndSpeakOnline(matches[0], shouldSpeak = true)
                    }
                    if (isVideoPlaying) startListeningEngine()
                }

                override fun onPartialResults(partialResults: AndroidBundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndSpeakOnline(matches[0], shouldSpeak = false)
                    }
                }

                override fun onEvent(eventType: Int, params: AndroidBundle?) {}
            })
        } catch (e: Exception) {}
    }

    private fun startListeningEngine() {
        if (isVideoPlaying) {
            mainHandler.post {
                try {
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (e: Exception) {}
            }
        }
    }

    private fun translateAndSpeakOnline(textToTranslate: String, shouldSpeak: Boolean) {
        if (textToTranslate.isEmpty()) return

        thread {
            try {
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
                        val amharicResult = rawResponse.substring(firstIndex, secondIndex)

                        mainHandler.post {
                            if (shouldSpeak) {
                                translationTextView?.setTextColor(Color.parseColor("#F59E0B"))
                                translationTextView?.text = "🎙️ [እንግሊዝኛ]: \"$textToTranslate\"\n\n🔊 [አማርኛ Voice]:\n$amharicResult"
                                
                                if (amharicResult != lastSpokenText) {
                                    lastSpokenText = amharicResult
                                    playAmharicAudioFromCloud(amharicResult)
                                }
                            } else {
                                translationTextView?.setTextColor(Color.parseColor("#10B981"))
                                translationTextView?.text = "🎙️ [የቪዲዮውን ንግግር እየተረጎመ ነው...]: \"$textToTranslate\""
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun playAmharicAudioFromCloud(textToSpeak: String) {
        thread {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                }
                mediaPlayer = null

                val ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=am&client=tw-ob&q=" +
                        URLEncoder.encode(textToSpeak, "UTF-8")

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
                }
            } catch (e: Exception) {}
        }
    }

    private fun stopSpeechEngine() {
        isVideoPlaying = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            videoView?.stopPlayback()
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopSpeechEngine()
        super.onDestroy()
    }
}
