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

    // 🎯 ከቪዲዮው ስክሪፕት የተወሰዱ ሁሉንም ቃላት ያካተተ ሰፊ መዝገበ-ቃላት
    private val translationDictionary = LinkedHashMap<String, String>().apply {
        put("boom", "ቡም! (ድንገተኛ ድምፅ) 💥")
        put("look", "ማየት / ተመልከት 👀")
        put("badboy", "ጎበዝ / አስደናቂ ነገር 😎")
        put("cool", "ግሩም / በጣም አሪፍ 😎")
        put("playbutton", "የዩቲዩብ ፕሌይ በተን (የሽልማት ቁልፍ) 🥇")
        put("wall", "ግድግዳ 🧱")
        put("good", "ጥሩ / መልካም 👍")
        put("see", "ማየት 👀")
        put("special", "ልዩ ✨")
        put("surprise", "ድንገተኛ ስጦታ / ሰርፕራይዝ 🎉")
        put("celebrate", "ማክበር / ደስታን መግለጽ 🥳")
        put("subscribers", "ሰብስክራይበሮች (ተከታዮች) 👥")
        put("say", "መናገር / ማለት 🗣️")
        put("something", "አንድ ነገር 📝")
        put("childhood", "የልጅነት ጊዜ 👶")
        put("bedroom", "የመኝታ ክፍል 🛏️")
        put("created", "የፈጠርኩት / የሰራሁት 🛠️")
        put("channel", "የዩቲዩብ ቻናል 📺")
        put("blessed", "የተባረክኩ / የታደልኩ 🙏")
        put("viewers", "ተመልካቾች 👁️‍🗨️")
        put("closing", "ለማጠቃለል / መጨረሻ ላይ 🏁")
        put("never", "በፍጹም ❌")
        put("guys", "ጓደኞቼ / እናንተን 👥")
        put("granted", "እንደ ቀላል ነገር ማየት (ለእናንተ ያለኝን ክብር አልቀንስም) 🤝")
        put("thanks", "ምስጋና / አመሰግናለሁ 🙏")
        put("watching", "ስለተመለከታችሁ 📺")
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
            text = "⏳ ቪዲዮው ሲጀምር እያንዳንዱ ቃል እና ትርጉም እዚህ ያለምንም መቆራረጥ ይጻፋል..."
            textSize = 18f
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
        
        statusTextView?.text = "🎬 ቪዲዮው እየተጫወተ ነው... የማያቋርጥ የትርጉም ዥረት ነቅቷል!"
        statusTextView?.setTextColor(Color.parseColor("#3B82F6"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView?.start()
            isVideoPlaying = true
            
            // 🚀 ቀጣይነት ያለው የድምፅ መለዮ ሞተር ማስጀመር
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
                // 🔥 ድምፅ መሃል ላይ ለሰከንድ ቢቆም እንኳ ሞተሩ እንዳይዘጋ የሚከለክሉ የጉግል ሚስጥራዊ ቁልፎች
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: AndroidBundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    // 🔄 የድሮ ጽሑፍ ሳይጠፋ ያለምንም መቆራረጥ ሞተሩን በቅጽበት መልሶ ማስነሻ
                    if (isVideoPlaying) {
                        mainHandler.postDelayed({ startListeningEngine() }, 50)
                    }
                }

                override fun onResults(results: AndroidBundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processSpokenText(matches[0])
                    }
                    if (isVideoPlaying) startListeningEngine()
                }

                override fun onPartialResults(partialResults: AndroidBundle?) {
                    // ⚡ ቪዲዮው እየተናገረ እያለ በየሚሊሰከንዱ ጽሑፉን በቅጽበት ማውጫ
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processSpokenText(matches[0])
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

    private fun processSpokenText(text: String) {
        val lowerText = text.lowercase()
        val foundTranslations = ArrayList<String>()

        // 🔍 በቪዲዮው ንግግር ውስጥ ያሉትን ቃላት በሙሉ ከመዝገበ-ቃላቱ ጋር ማመሳከር
        for ((englishWord, amharicTranslation) in translationDictionary) {
            if (lowerText.contains(englishWord)) {
                foundTranslations.add("• \"$englishWord\" ➡️ $amharicTranslation")
            }
        }

        mainHandler.post {
            if (foundTranslations.isNotEmpty()) {
                translationTextView?.setTextColor(Color.parseColor("#F59E0B")) // ወደ ደማቅ ቢጫ መቀየር
                val output = StringBuilder("🎙️ [የተሰማ ሙሉ እንግሊዝኛ]:\n\"$text\"\n\n🔄 [የቃላት ትርጉም]:\n")
                for (trans in foundTranslations) {
                    output.append("$trans\n")
                }
                translationTextView?.text = output.toString()
            } else {
                // መዝገበ ቃላት ውስጥ የሌለ ቃል ቢሆንም እንኳ የሰማውን በሙሉ በቅጽበት በአረንጓዴ ይጽፈዋል
                translationTextView?.setTextColor(Color.parseColor("#10B981"))
                translationTextView?.text = "🎙️ [የሰማው እንግሊዝኛ]:\n\"$text\""
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
