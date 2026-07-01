package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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

    private var statusTextView: TextView? = null
    private var logTextView: TextView? = null
    private var btnToggleService: Button? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isBackgroundListening = false
    private var speechIntent: Intent? = null
    private var audioIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ከፍተኛ የድምፅ ማስተካከያ (ማይክሮፎኑ የስልኩን ድምፅ በጀርባ እንዲሰማው)
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {}

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40, 40, 40, 40)
        }

        statusTextView = TextView(this).apply {
            text = "🔄 የጀርባ ድምፅ ተርጓሚ (Background Voice Dubber)"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#3B82F6"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        mainLayout.addView(statusTextView)

        logTextView = TextView(this).apply {
            text = "አፑን አስጀምረውና ወደ ሌላ ቪዲዮ ማጫወቻ (YouTube / Gallery) ሂድ። አፑ በጀርባ ሆኖ እየሰማ በአማርኛ ይናገራል..."
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 20f
                setStroke(4, Color.parseColor("#10B981"))
            }
            val textLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            textLp.setMargins(0, 0, 0, 40)
            layoutParams = textLp
        }
        mainLayout.addView(logTextView)

        btnToggleService = Button(this).apply {
            text = "🚀 የጀርባ ትርጉም አስነሳ (Start Background Translator)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
        }
        btnToggleService!!.setOnClickListener { toggleBackgroundService() }
        mainLayout.addView(btnToggleService)

        setContentView(mainLayout)

        // የንግግር ማወቂያ ዝግጅት
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    private fun toggleBackgroundService() {
        if (!isBackgroundListening) {
            isBackgroundListening = true
            btnToggleService?.text = "🛑 የጀርባ ትርጉም አቁም (Stop Background Translator)"
            btnToggleService?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444"))
                cornerRadius = 25f
            }
            logTextView?.text = "📢 አፑ በጀርባ መስራት ጀምሯል! አሁን ይህንን አፕ ዘግተህ የትኛውንም ቪዲዮ በስልክህ ላይ ስታጫውት በጀርባ እየሰማ በአማርኛ ድምፅ ይተረጉማል..."
            logTextView?.setTextColor(Color.parseColor("#F59E0B"))
            
            startBackgroundListeningLoop()
        } else {
            isBackgroundListening = false
            btnToggleService?.text = "🚀 የጀርባ ትርጉም አስነሳ (Start Background Translator)"
            btnToggleService?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
            logTextView?.text = "ተወግዷል። አፑ ቆሟል።"
            logTextView?.setTextColor(Color.parseColor("#94A3B8"))
            speechRecognizer?.destroy()
            mediaPlayer?.release()
        }
    }

    // 🔄 አፑ በጀርባ ሆኖ የስልኩን ማይክሮፎን በመጠቀም የቪዲዮውን ድምፅ የሚሰማበት ማለቂያ የሌለው ሉፕ
    private fun startBackgroundListeningLoop() {
        if (!isBackgroundListening) return

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
                        // ቪዲዮው እስኪጀምር ወይም ዝምታ ቢኖርም ሉፑ እንዳይቋረጥ ወዲያውኑ ይቀጥላል
                        if (isBackgroundListening) {
                            mainHandler.postDelayed({ startBackgroundListeningLoop() }, 400)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val englishSpeech = matches[0]
                            
                            thread {
                                // 1. በጀርባ የተሰማውን የእንግሊዝኛ ንግግር መተርጎም
                                val amharicTranslation = translateOnlineBackground(englishSpeech)
                                
                                // 2. የአማርኛ ድምፅ ማመንጨት
                                val audioFile = downloadVoiceTrack(amharicTranslation)
                                
                                mainHandler.post {
                                    logTextView?.text = "🎙️ [የተሰማው ንግግር]: \"$englishSpeech\"\n\n🔄 [በጀርባ በአማርኛ መናገር]: $amharicTranslation"
                                    // 3. በአማርኛ በድምፅ ማውራት!
                                    playAmharicVoiceTrack(audioFile)
                                }
                            }
                        }
                        startBackgroundListeningLoop()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(speechIntent)
            } catch (e: Exception) {
                mainHandler.postDelayed({ startBackgroundListeningLoop() }, 400)
            }
        }
    }

    private fun translateOnlineBackground(text: String): String {
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

    private fun downloadVoiceTrack(text: String): File? {
        return try {
            audioIndex++
            val file = File(cacheDir, "bg_voice_$audioIndex.mp3")
            val url = URL("https://translate.google.com/translate_tts?ie=UTF-8&tl=am&client=tw-ob&q=" + URLEncoder.encode(text, "UTF-8"))
            val con = url.openConnection() as HttpURLConnection
            con.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (con.responseCode == 200) {
                val fos = FileOutputStream(file)
                con.inputStream.copyTo(fos)
                fos.close()
                file
            } else null
        } catch (e: Exception) { null }
    }

    private fun playAmharicVoiceTrack(file: File?) {
        if (file == null || !file.exists() || !isBackgroundListening) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(file.absolutePath)
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
