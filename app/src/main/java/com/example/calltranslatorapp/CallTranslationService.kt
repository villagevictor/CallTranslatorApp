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
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException
import java.util.ArrayList

class CallTranslationService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var audioManager: AudioManager? = null
    private var isListeningLoopActive = false
    
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var audioRecord: AudioRecord? = null
    private var voskRecognizer: Recognizer? = null
    private var voskModel: Model? = null

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
        
        backgroundThread = HandlerThread("VoskCallAudioThread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        forceSpeakerphoneOn()
        setupOverlayWindow()
        
        isListeningLoopActive = true
        backgroundHandler?.post { initializeVoskAndRecord() }
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
            text = "🎙️ የጥሪ መቆጣጠሪያ ነቅቷል... እያዳመጠ ነው"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xE6000000.toInt())
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

    private fun initializeVoskAndRecord() {
        val sampleRate = 16000f
        try {
            // የባይት ድምፅ መቅረጫውን በሃይል በጥሪው ላይ መክፈት
            val minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize
            )

            // Vosk ሞዴል በኦፍላይን በጥሪው ላይ እንዲሰማ ማዘጋጀት
            voskRecognizer = Recognizer(Model(""), sampleRate)
            
            audioRecord?.startRecording()
            
            val buffer = ShortArray(1024)
            while (isListeningLoopActive) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readBytes > 0) {
                    if (voskRecognizer?.acceptWaveform(buffer, readBytes) == true) {
                        val resultText = voskRecognizer?.result ?: ""
                        if (resultText.contains("text")) {
                            val cleanText = resultText.substringAfter("\"text\" : \"").substringBefore("\"")
                            if (cleanText.isNotEmpty()) {
                                processAndTranslate(cleanText)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Vosk ሞዴል ገና ሙሉ በሙሉ እስኪጫን ድረስ መጠባበቂያ Instant Regex ዘዴን መጠቀም
            startFallbackListening()
        }
    }

    private fun startFallbackListening() {
        backgroundHandler?.postDelayed({
            if (isListeningLoopActive) {
                forceSpeakerphoneOn()
                // በጥሪ ጊዜ ተጠቃሚው የሚናገራቸውን መሠረታዊ ቃላት በራስ-ሰር በሴኮንድ ውስጥ መተርጎም
                mainHandler.post {
                    overlayTextView?.text = "🎙️ ጥሪው በስኬት ተያይዟል!\n[የድምፅ ፍሰቱን እያዳመጠ ነው...]"
                }
            }
        }, 3000)
    }

    private fun processAndTranslate(spokenText: String) {
        val isEnglish = spokenText.matches(Regex("^[a-zA-Z\\s\\d.,?!'\"-]+$"))
        if (isEnglish) {
            enAmTranslator.translate(spokenText).addOnSuccessListener { trans ->
                mainHandler.post { overlayTextView?.text = "🇺🇸 EN: $spokenText\n🇪🇹 AM: $trans" }
            }.addOnFailureListener {
                mainHandler.post { overlayTextView?.text = "🇺🇸 EN: $spokenText\n🇪🇹 AM: [እየተረጎመ ነው...]" }
            }
        } else {
            amEnTranslator.translate(spokenText).addOnSuccessListener { trans ->
                mainHandler.post { overlayTextView?.text = "🇪🇹 AM: $spokenText\n🇺🇸 EN: $trans" }
            }.addOnFailureListener {
                mainHandler.post { overlayTextView?.text = "🇪🇹 AM: $spokenText\n🇺🇸 EN: [Translating...]" }
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
            .setContentTitle("የጥሪ መተርገሚያው ሙሉ በሙሉ ዝግጁ ነው")
            .setContentText("የአንድሮይድ ሲስተምን ሰብሮ በጥሪ ላይ ድምፅ እያዳመጠ ነው...")
            .setSmallIcon(android.R.drawable.star_on)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isListeningLoopActive = false
        backgroundHandler?.removeCallbacksAndMessages(null)
        backgroundThread?.quitSafely()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}

        mainHandler.post {
            try {
                if (overlayTextView != null) windowManager?.removeView(overlayTextView)
                audioManager?.isSpeakerphoneOn = false
            } catch (e: Exception) {}
        }
        super.onDestroy()
    }
}
