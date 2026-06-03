package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.ArrayList

class CallTranslationService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var textViewSubtitle: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var audioManager: AudioManager? = null

    // የትርጉም ሞተሮች
    private val enAmTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("en").setTargetLanguage("am").build()
    )
    private val amEnTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("am").setTargetLanguage("en").build()
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getOrCreateAudioManager()
        startForegroundServiceNotification()
        showOverlayWindow()
        setupPhoneCallListener()
    }

    private fun getOrCreateAudioManager(): AudioManager {
        return getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun startForegroundServiceNotification() {
        val channelId = "call_translator_channel"
        val channel = NotificationChannel(channelId, "Call Translation", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("የጥሪ መተርገሚያው በጀርባ እየሰራ ነው")
            .setContentText("በጥሪ ጊዜ ድምፅን በራስ-ሰር ያዳምጣል...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun showOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // ቀለል ያለ ተንሳፋፊ ጽሑፍ ማሳያ ማዘጋጀት
        textViewSubtitle = TextView(this).apply {
            text = "🎙️ ጥሪ ሲጀመር ትርጉሙ እዚህ ላይ ይታያል..."
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000")) // ከፊል ግልፅ ጥቁር
            setPadding(30, 20, 30, 20)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 150 // ከላይ ትንሽ ዝቅ ብሎ እንዲቀመጥ
        }

        windowManager?.addView(textViewSubtitle, params)
    }

    private fun setupPhoneCallListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_MANAGER) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        // ጥሪው ሲጀመር ስፒከር ማብራት እና ማዳመጥ መጀመር
                        activateSpeakerPhone()
                        startContinuousSpeechRecognition()
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // ጥሪው ሲዘጋ ማዳመጥ ማቆም
                        stopSpeechRecognition()
                    }
                }
            }
        }
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun activateSpeakerPhone() {
        try {
            audioManager?.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                isSpeakerphoneOn = true
            }
        } catch (e: Exception) {
            updateSubtitle("⚠️ ስፒከር ማብራት አልተቻለም")
        }
    }

    private fun startContinuousSpeechRecognition() {
        if (speechRecognizer != null) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    updateSubtitle("🎙️ እያዳመጥኩ ነው... ይናገሩ")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    // ስህተት ሲፈጠር (ለምሳሌ ዝምታ ከተፈጠረ) ራሱን በራሱ መልሶ እንዲቀሰቅስ ማድረግ (Continuous Loop)
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                    startContinuousSpeechRecognition()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.get(0) ?: ""
                    
                    if (spokenText.isNotEmpty()) {
                        processAndTranslate(spokenText)
                    }
                    
                    // ውጤቱን ካሳየ በኋላ ወዲያውኑ ማዳመጡን በቋሚነት መቀጠል
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                    startContinuousSpeechRecognition()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("am-ET", "en-US"))
        }
        speechRecognizer?.startListening(intent)
    }

    private fun processAndTranslate(text: String) {
        val isEnglish = text.matches(Regex("^[a-zA-Z\\s\\d.,?!'\"-]+$"))
        
        if (isEnglish) {
            enAmTranslator.translate(text)
                .addOnSuccessListener { translated ->
                    updateSubtitle("🇺🇸 EN: $text\n🇪🇹 AM: $translated")
                }
                .addOnFailureListener {
                    updateSubtitle("🇺🇸 EN: $text (ለመተርጎም አልተቻለም)")
                }
        } else {
            amEnTranslator.translate(text)
                .addOnSuccessListener { translated ->
                    updateSubtitle("🇪🇹 AM: $text\n🇺🇸 EN: $translated")
                }
                .addOnFailureListener {
                    updateSubtitle("🇪🇹 AM: $text (ለመተርጎም አልተቻለም)")
                }
        }
    }

    private fun updateSubtitle(text: String) {
        textViewSubtitle?.text = text
    }

    private fun stopSpeechRecognition() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        updateSubtitle("📞 ጥሪው ተዘግቷል")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpeechRecognition()
        if (textViewSubtitle != null && windowManager != null) {
            windowManager?.removeView(textViewSubtitle)
        }
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }
}
