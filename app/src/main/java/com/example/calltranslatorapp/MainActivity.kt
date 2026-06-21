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
import java.util.Locale

class MainActivity : Activity() {

    private var videoView: VideoView? = null
    private var statusTextView: TextView? = null
    private var translationTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isVideoPlaying = false

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
            setPadding(30, 30, 30, 30)
        }

        // 📺 1. የቪዲዮ ማጫወቻ መስኮት
        videoView = VideoView(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                750
            )
            lp.setMargins(0, 0, 0, 30)
            layoutParams = lp
        }
        mainLayout.addView(videoView)

        // 📝 2. የሁኔታ መግለጫ
        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ ቪዲዮ መርጠው Upload ያድርጉ..."
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusTextView)

        // 🔄 3. የትርጉም እና የተሰማ ጽሑፍ ማሳያ ሰሌዳ
        translationTextView = TextView(this).apply {
            text = "⏳ ቪዲዮው ሲጀምር የተሰማው እንግሊዝኛ እና ትርጉሙ እዚህ ይጻፋል..."
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val descDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 25f
                setStroke(3, Color.parseColor("#3B82F6"))
            }
            background = descDrawable
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }
        mainLayout.addView(translationTextView)

        // 📤 4. ቪዲዮ መምረጫ በተን
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
        
        statusTextView?.text = "🎬 ቪዲዮው እየተጫወተ ነው... ድምፅ እየተሰማ ነው..."
        statusTextView?.setTextColor(Color.parseColor("#3B82F6"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView?.start()
            isVideoPlaying = true
            // 🚀 ቪዲዮው ልክ እንደጀመረ የድምፅ መለዮውን ማንቃት
            startSpeechRecognitionLoop()
        }
    }

    private fun startSpeechRecognitionLoop() {
        if (!isVideoPlaying) return

        mainHandler.post {
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
                        // 🔄 ድምፅ መሃል ላይ ቢቋረጥ እንኳ በራሱ መልሶ እንዲቀሰቅስ
                        if (isVideoPlaying) {
                            mainHandler.postDelayed({ startSpeechRecognitionLoop() }, 500)
                        }
                    }

                    override fun onResults(results: AndroidBundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            processSpokenText(matches[0])
                        }
                        if (isVideoPlaying) startSpeechRecognitionLoop()
                    }

                    override fun onPartialResults(partialResults: AndroidBundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            processSpokenText(matches[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: AndroidBundle?) {}
                })

                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                translationTextView?.text = "❌ የድምፅ መለዮውን ማንሳት አልተቻለም"
            }
        }
    }

    private fun processSpokenText(text: String) {
        val lowerText = text.lowercase()
        var translatedWord = ""
        var amharicMeaning = ""

        // 🔍 መዝገበ ቃላቱን መፈለግ
        for ((englishWord, amharicTranslation) in translationDictionary) {
            if (lowerText.contains(englishWord)) {
                translatedWord = englishWord
                amharicMeaning = amharicTranslation
                break
            }
        }

        mainHandler.post {
            if (amharicMeaning.isNotEmpty()) {
                // 🌟 የተረጎመውን በግልፅ በቢጫ ያሳያል
                translationTextView?.setTextColor(Color.parseColor("#F59E0B"))
                translationTextView?.text = "🔊 [የተሰማ ቃል]: \"$translatedWord\"\n🔄 [ትርጉም]: $amharicMeaning"
            } else {
                // 🎙️ የሰማውን ሙሉ የእንግሊዝኛ ዓረፍተ ነገር በስክሪኑ ላይ በቀጥታ ይጽፋል!
                translationTextView?.setTextColor(Color.parseColor("#10B981"))
                translationTextView?.text = "🎙️ [የተሰማ እንግሊዝኛ]:\n\"$text\""
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
