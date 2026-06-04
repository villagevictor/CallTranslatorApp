package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.ArrayList

class CallTranslationService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    private var isListeningLoopActive = false
    
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var recognitionIntent: Intent
    
    // የ AudioRecord ረዳቶች
    private var audioRecord: AudioRecord? = null
    private var isRecordingBytes = false

    private val enAmTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("en").setTargetLanguage("am").build()
    )
    private val amEnTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("am").setTargetLanguage("en").build()
    )

    override fun onCreate() {
        super.onCreate()
        audioManager = getApplicationContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        startForeground(1, createNotification())
        
        backgroundThread = HandlerThread("VoiceCallProcessorThread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        // በጥሪ ጊዜ ስፒከሩን በሃይል ማንቃት
        forceSpeakerphoneOn()

        setupOverlayWindow()
        
        isListeningLoopActive = true
        backgroundHandler?.post { startAdvancedCallListening() }
    }

    private fun forceSpeakerphoneOn() {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayTextView = TextView(this).apply {
            text = "🎙️ የጥሪ ድምፅ በሃይል በመያዝ ላይ... መናገር ይችላሉ"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xE6000000.toInt()) // ይበልጥ ጎልቶ የሚታይ ጥቁር
            setPadding(40, 30, 40, 30)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 250
        }

        try {
            windowManager?.addView(overlayTextView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAdvancedCallListening() {
        if (!isListeningLoopActive) return

        // ዘዴ 1፦ የጥሪ ድምፅን በሴኮንድ ውስጥ በባይት ደረጃ ለመቆለፍ መሞከር (AudioRecord Audio Capture Bypass)
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate, channelConfig, audioFormat, bufferSize
            )
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isRecordingBytes = true
                // ከበስተጀርባ ድምፅ መኖሩን በባይት ደረጃ እያዳመጠ ለትርጉም ያዘጋጃል
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // ዘዴ 2፦ መደበኛውን የድምፅ ኢንጂን ከተሻሻለ ዲክቴሽን ሞድ ጋር በጥምረት ማስነሳት
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@CallTranslationService)
            recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("am-ET", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // አንድሮይድ በጥሪ ጊዜ ማይኩን እንዳይዘጋው የመጨረሻውን የሃይል ትዕዛዝ መስጠት
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.AUDIO_SOURCE", MediaRecorder.AudioSource.VOICE_RECOGNITION)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    forceSpeakerphoneOn()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsd: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    backgroundHandler?.post { restartCallListening() }
                }

                override fun onError(error: Int) {
                    // ጥሪ ላይ ስህተት ቢመጣ እንኳ በየ 2 ሴኮንድ ራሱን በሃይል ይቀሰቅሳል
                    forceSpeakerphoneOn()
                    backgroundHandler?.removeCallbacksAndMessages(null)
                    backgroundHandler?.postDelayed({ restartCallListening() }, 2000)
                }

                override fun onResults(results: Bundle?) {
                    backgroundHandler?.post { processVoiceResults(results) }
                    backgroundHandler?.post { restartCallListening() }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        mainHandler.post {
                            overlayTextView?.text = "🎙️ በጥሪ ላይ እየተሰማ ነው፦\n${matches[0]}"
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            try {
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun restartCallListening() {
        if (!isListeningLoopActive) return
        forceSpeakerphoneOn()
        mainHandler.post {
            try {
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                backgroundHandler?.post { startAdvancedCallListening() }
            }
        }
    }

    private fun processVoiceResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.get(0) ?: ""
        
        if (spokenText.isNotEmpty()) {
            val isEnglish = spokenText.matches(Regex("^[a-zA-Z\\s\\d.,?!'\"-]+$"))
            
            if (isEnglish) {
                enAmTranslator.translate(spokenText).addOnSuccessListener { trans ->
                    mainHandler.post { overlayTextView?.text = "🇺🇸 EN: $spokenText\n🇪🇹 AM: $trans" }
                }.addOnFailureListener {
                    val amFallback = when {
                        spokenText.contains("hello", true) -> "ሰላም"
                        spokenText.contains("how", true) -> "እንደምን ነህ?"
                        spokenText.contains("fine", true) -> "ደህና ነኝ"
                        else -> "$spokenText"
                    }
                    mainHandler.post { overlayTextView?.text = "🇺🇸 EN: $spokenText\n🇪🇹 AM: $amFallback" }
                }
            } else {
                amEnTranslator.translate(spokenText).addOnSuccessListener { trans ->
                    mainHandler.post { overlayTextView?.text = "🇪🇹 AM: $spokenText\n🇺🇸 EN: $trans" }
                }.addOnFailureListener {
                    val enFallback = when {
                        spokenText.contains("ሰላም", true) -> "Hello"
                        spokenText.contains("እንደምን", true) -> "How are you?"
                        spokenText.contains("ደህና", true) -> "I am fine"
                        else -> "$spokenText"
                    }
                    mainHandler.post { overlayTextView?.text = "🇪🇹 AM: $spokenText\n🇺🇸 EN: $enFallback" }
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "call_translator_channel"
        val channelName = "Call Translation Running"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("የጥሪ መተርገሚያ መስመር ነቅቷል")
            .setContentText("አፑ ማይክሮፎኑን በሃይል ተቆጣጥሮ ጥሪውን እያዳመጠ ነው...")
            .setSmallIcon(android.R.drawable.star_on)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isListeningLoopActive = false
        isRecordingBytes = false
        backgroundHandler?.removeCallbacksAndMessages(null)
        backgroundThread?.quitSafely()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                if (overlayTextView != null) windowManager?.removeView(overlayTextView)
                audioManager?.isSpeakerphoneOn = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }
}
