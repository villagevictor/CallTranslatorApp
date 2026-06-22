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

    // 🎯 ፍጹም የዓረፍተ ነገር መዝገበ-ቃላት
    private val fullSentenceDictionary = LinkedHashMap<String, String>().apply {
        put("boom i look at that badboy", "ቡም! ያንን አስደናቂ ነገር ተመልከቱት 💥")
        put("it actually looks really cool", "በእውነት በጣም ያምራል/ደስ ይላል 😎")
        put("so i am going to put this playbutton on my wall", "Template-ስለዚህ ይህንን የዩቲዩብ ፕሌይ በተን ግድግዳዬ ላይ እሰቅለዋለሁ 🥇")
        put("jemmy good to see you", "ጄሚ በማየትህ ደስ ብሎኛል 🤝")
        put("we have a special surprise to celebrate 500m subscribers", "የ 500 ሚሊዮን ተከታዮችን (Subscribers) ለማክበር ልዩ ድንገተኛ ስጦታ/ሰርፕራይዝ አዘጋጅተናል 🎉")
        put("we have a special surprise to celebrate 500 million subscribers", "የ 500 ሚሊዮን ተከታዮችን (Subscribers) ለማክበር ልዩ ድንገተኛ ስጦታ/ሰርፕራይዝ አዘጋጅተናል 🎉")
        put("i want to say ssomething from my childbood bedroom", "ከእኔ የልጅነት መኝታ ክፍል ሆኜ አንድ ነገር መናገር እፈልጋለሁ 👶")
        put("i want to say something from my childhood bedroom", "ከእኔ የልጅነት መኝታ ክፍል ሆኜ አንድ ነገር መናገር እፈልጋለሁ 👶")
        put("i actually one day had 0 subscribers", "እኔ በአንድ ወቅት 0 ተከታይ (Subscriber) ነበረኝ 📉")
        put("this is where created my channel", "ቻናሌን የፈጠርኩት/የጀመርኩት እዚህ ቦታ ላይ ነው 🛠️")
        put("i am blessed to have this many viewers", "ይህን ያህል ብዙ ተመልካች በማግኘቴ የታደልኩ ነኝ 🙏")
        put("in closing i just want i will never take you guys for granted", "ለማጠቃለል ያህል፥ እናንተን (ተከታዮቼን) መቼም ቢሆን እንደ ቀላል ነገር አልቆጥራችሁም (ትልቅ ክብር አለኝ) 🤝")
        put("and thanks for watching", "ስለተመለከታችሁም በጣም አመሰግናለሁ! 🙏📺")
    }

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
            text = "⏳ ቪዲዮው ሲጀምር ንግግሩና ትክክለኛው የአማርኛ ትርጉም እዚህ በቅጽበት ይጻፋል..."
            textSize = 17f
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
        
        statusTextView?.text = "🎬 ሙሉ ቪዲዮውን በዓረፍተ ነገር የመተርጎም ሁነታ ነቅቷል..."
        statusTextView?.setTextColor(Color.parseColor("#3B82F6"))

        videoView?.setVideoURI(uri)
        // 🛠️ እዚህ መስመር ላይ የነበረው 'mp =' ስህተት ተስተካክሏል
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
                        processSentenceText(matches[0])
                    }
                    if (isVideoPlaying) startListeningEngine()
                }

                override fun onPartialResults(partialResults: AndroidBundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processSentenceText(matches[0])
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

    private fun processSentenceText(currentText: String) {
        val lowerText = currentText.lowercase().trim()
        var matchedAmharicTranslation = ""

        for ((englishSentence, amharicTranslation) in fullSentenceDictionary) {
            if (lowerText.contains(englishSentence) || englishSentence.contains(lowerText)) {
                matchedAmharicTranslation = amharicTranslation
                break
            }
        }

        mainHandler.post {
            if (matchedAmharicTranslation.isNotEmpty()) {
                translationTextView?.setTextColor(Color.parseColor("#F59E0B"))
                translationTextView?.text = "🎙️ [የተሰማ እንግሊዝኛ]:\n\"$currentText\"\n\n🔄 [ትክክለኛ ትርጉም]:\n$matchedAmharicTranslation"
            } else {
                translationTextView?.setTextColor(Color.parseColor("#10B981"))
                translationTextView?.text = "🎙️ [የተሰማ እንግሊዝኛ]:\n\"$currentText\""
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
