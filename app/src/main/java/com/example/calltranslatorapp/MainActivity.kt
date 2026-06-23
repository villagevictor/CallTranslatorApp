package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private var statusTextView: TextView? = null
    private var translationTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isListeningActive = false

    private var mediaPlayer: MediaPlayer? = null
    private var lastSpokenText = ""
    
    // 🎛️ የሲስተም ድምፅ መቆጣጠሪያ (AudioManager)
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40, 40, 40, 40)
        }

        statusTextView = TextView(this).apply {
            text = "🎙️ አፑን አስነስቶ በሌላ ማጫወቻ ቪዲዮ ለመተርጎም ዝግጁ ነው..."
            textSize = 16f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(statusTextView)

        translationTextView = TextView(this).apply {
            text = "🔊 ሌላ አፕ ላይ ቪዲዮ ሲከፍቱ የእንግሊዝኛው ድemጽ ቀንሶ አማርኛው እዚህ በከፍተኛ ድምፅ ይጮኻል።.."
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
            ).apply { setMargins(0, 0, 0, 50) }
        }
        mainLayout.addView(translationTextView)

        val btnToggleListen = Button(this).apply {
            text = "▶️ መተርጎም ጀምር (Start Translation System)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            val btnDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
            background = btnDrawable
        }

        btnToggleListen.setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                if (!isListeningActive) {
                    isListeningActive = true
                    btnToggleListen.text = "⏹️ አቁም (Stop Translation System)"
                    btnToggleListen.background = GradientDrawable().apply {
                        setColor(Color.parseColor("#EF4444"))
                        cornerRadius = 25f
                    }
                    statusTextView?.text = "🟢 አፑ በጀርባ እያዳመጠ ነው፤ አሁን ወደ ፈለጉት ቪዲዮ ማጫወቻ ይሂዱ..."
                    statusTextView?.setTextColor(Color.parseColor("#10B981"))
                    
                    initSpeechRecognizer()
                    startListeningEngine()
                } else {
                    isListeningActive = false
                    btnToggleListen.text = "▶️ መተርጎም ጀምር (Start Translation System)"
                    btnToggleListen.background = GradientDrawable().apply {
                        setColor(Color.parseColor("#10B981"))
                        cornerRadius = 25f
                    }
                    statusTextView?.text = "🛑 የአስተርጓሚው ሲስተም ቆሟል..."
                    statusTextView?.setTextColor(Color.parseColor("#94A3B8"))
                    stopSpeechEngine()
                }
            } else {
                requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 105)
            }
        }
        mainLayout.addView(btnToggleListen)

        setContentView(mainLayout)
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
                    if (isListeningActive) {
                        mainHandler.postDelayed({ startListeningEngine() }, 10)
                    }
                }

                override fun onResults(results: AndroidBundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndSpeakOnline(matches[0], shouldSpeak = true)
                    }
                    if (isListeningActive) startListeningEngine()
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
        if (isListeningActive) {
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
                                translationTextView?.text = "🎙️ [የሰማው እንግሊዝኛ]: \"$textToTranslate\"\n\n🔊 [አማርኛ Voice]:\n$amharicResult"
                                
                                if (amharicResult != lastSpokenText) {
                                    lastSpokenText = amharicResult
                                    requestAudioFocusAndSpeak(amharicResult)
                                }
                            } else {
                                translationTextView?.setTextColor(Color.parseColor("#10B981"))
                                translationTextView?.text = "🎙️ [እያዳመጠ ነው...]: \"$textToTranslate\""
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    // 🎛️ ሌላው አፕ ላይ ያለው የቪዲዮ ድምፅ እንዲቀንስ (Duck እንዲያደርግ) ማዘዣ ዘዴ
    private fun requestAudioFocusAndSpeak(textToSpeak: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        mediaPlayer?.stop()
                    }
                }
                .build()
            
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }

        playAmharicAudioFromCloud(textToSpeak)
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
                    
                    // 🔄 የአማርኛው ድምፅ ተናግሮ ሲጨርስ የሌላውን አፕ (የእንግሊዝኛውን ቪዲዮ) ድምፅ በራስ-ሰር መልሶ ከፍ ያደርገዋል
                    setOnCompletionListener {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
                        } else {
                            @Suppress("DEPRECATION")
                            audioManager?.abandonAudioFocus(null)
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun stopSpeechEngine() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopSpeechEngine()
        super.onDestroy()
    }
}
