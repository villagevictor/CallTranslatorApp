package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
            text = "⏳ ቪዲዮው ሲጀምር ጎግል AI በራሱ ሰምቶ በሰከንድ ውስጥ ወደ አማርኛ ይተረጉመዋል..."
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val descDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 25f
                setStroke(4, Color.parseColor("#10B981"))
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
        
        statusTextView?.text = "🎬 የ Google AI Cloud የመተርጎም ሁነታ ነቅቷል..."
        statusTextView?.setTextColor(Color.parseColor("#3B82F6"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            videoView?.start()
            isVideoPlaying = true
            
            initSpeechRecognizer()
            startListeningEngine()
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
                        translateWithGoogleAI(matches[0])
                    }
                    if (isVideoPlaying) startListeningEngine()
                }

                override fun onPartialResults(partialResults: AndroidBundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateWithGoogleAI(matches[0])
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

    // 🌐 የ Google Translate ነፃ የ AI ትርጉም ኤፒአይ (API)
    private fun translateWithGoogleAI(textToTranslate: String) {
        if (textToTranslate.isEmpty()) return

        thread {
            try {
                val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=am&dt=t&q=" +
                        URLEncoder.encode(textToTranslate, "UTF-8")
                val url = URL(urlString)
                val con = url.openConnection() as HttpURLConnection
                con.requestMethod = "GET"
                con.setRequestProperty("User-Agent", "Mozilla/5.0")

                val responseCode = con.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(con.inputStream))
                    val response = StringBuilder()
                    var inputLine: String?
                    while (reader.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    reader.close()

                    // የጉግልን የ JSON ምላሽ ሰባብሮ ትክክለኛውን የአማርኛ ዓረፍተ ነገር ማውጫ ዘዴ
                    val rawResponse = response.toString()
                    if (rawResponse.contains("\"")) {
                        val firstIndex = rawResponse.indexOf("\"") + 1
                        val secondIndex = rawResponse.indexOf("\"", firstIndex)
                        val amharicResult = rawResponse.substring(firstIndex, secondIndex)

                        mainHandler.post {
                            translationTextView?.setTextColor(Color.parseColor("#F59E0B")) // ወደ ቢጫ ይቀይራል
                            translationTextView?.text = "🎙️ [የሰማው እንግሊዝኛ]:\n\"$textToTranslate\"\n\n🤖 [Google AI አውቶማቲክ ትርጉም]:\n$amharicResult"
                        }
                    }
                }
            } catch (e: Exception) {
                // ኢንተርኔት ከሌለ የሰማውን እንግሊዝኛ ብቻ በአረንጓዴ ያሳያል
                mainHandler.post {
                    translationTextView?.setTextColor(Color.parseColor("#10B981"))
                    translationTextView?.text = "🎙️ [የሰማው እንግሊዝኛ]:\n\"$textToTranslate\"\n⚠️ (ትርጉም ለመቀበል ኢንተርኔት ያብሩ)"
                }
            }
        }
    }

    private fun stopSpeechEngine() {
        isVideoPlaying = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            videoView?.stopPlayback()
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopSpeechEngine()
        super.onDestroy()
    }
}
