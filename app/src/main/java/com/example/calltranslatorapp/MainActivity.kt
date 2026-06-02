package com.example.calltranslatorapp

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.pm.PackageManager
import android.widget.Toast
import android.Manifest
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : Activity() {

    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var bgButton: Button
    private val SPEECH_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 200
    private var isModelDownloaded = false

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage("en")
        .setTargetLanguage("am")
        .build()
    private val englishAmharicTranslator = Translation.getClient(options)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        textView = TextView(this).apply {
            text = "የጥሪ መተርገሚያ አፕሊኬሽን\n(እንግሊዘኛ ➡️ አማርኛ)\n\nየትርጉም ፋይል በመጫን ላይ..."
            textSize = 22f
            gravity = Gravity.CENTER
        }

        button = Button(this).apply {
            text = "ማዳመጥ እና መተርጎም ጀምር"
            isEnabled = false
            setOnClickListener { startSpeechToText() }
        }

        bgButton = Button(this).apply {
            text = "በጥሪ ጊዜ ከበስተጀርባ አስጀምር"
            isEnabled = false
            setOnClickListener {
                try {
                    val intent = Intent(this@MainActivity, CallTranslationService::class.java)
                    startForegroundService(intent)
                    Toast.makeText(this@MainActivity, "የጥሪ መከታተያ በስኬት ተነስቷል!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "ማስነሳት አልተቻለም፦ ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        layout.addView(textView)
        layout.addView(button)
        layout.addView(bgButton)
        setContentView(layout)

        checkAndRequestPermissions()
        forceDownloadGoogleModel()
    }

    private fun forceDownloadGoogleModel() {
        // የሲስተሙን የኔትወርክ ገደብ ሙሉ በሙሉ ሰብሮ እንዲያወርድ ማዘዝ
        val conditions = DownloadConditions.Builder()
            .build()

        textView.text = "🔄 ሙሉ የአማርኛ መዝገበ-ቃላት (100MB) ከሰርቨር ላይ በሃይል እየወረደ ነው...\n\n⚠️ እባክዎ ጠንካራ ኢንተርኔት ወይም VPN አብርተው ይህ ገጽ ሳይዘጋ 1 ወይም 2 ደቂቃ ይጠብቁ! ይህ አንድ ጊዜ ብቻ ነው የሚደረገው።"

        englishAmharicTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                isModelDownloaded = true
                textView.text = "የጥሪ መተርገሚያ አፕሊኬሽን\n(እንግሊዘኛ ➡️ አማርኛ)\n\n✅ ሙሉ መዝገበ-ቃላቱ 100% ወርዶ አልቋል!\nአሁን ሁሉንም ቃል መተርጎም ይችላሉ።"
                button.isEnabled = true
                bgButton.isEnabled = true
                Toast.makeText(this, "ስኬት! ሙሉ የቋንቋ ማህደሩ ተጭኗል።", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                // የመጀመሪያው መስመር ካልሰራ ሁለተኛ የግዳጅ ሙከራ
                englishAmharicTranslator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        isModelDownloaded = true
                        textView.text = "የጥሪ መተርገሚያ አፕሊኬሽን\n(እንግሊዘኛ ➡️ አማርኛ)\n\n✅ ሙሉ መዝገበ-ቃላቱ በስኬት ተጭኗል!"
                        button.isEnabled = true
                        bgButton.isEnabled = true
                    }
                    .addOnFailureListener { secondaryError ->
                        textView.text = "❌ ማውረድ አልተቻለም፦ ${secondaryError.message}\n\n💡 መፍትሄ፦ እባክዎ VPN አብርተው አፑን መልሰው ይክፈቱት!"
                    }
            }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add("android.permission.POST_NOTIFICATIONS")
            }
        }
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissions(permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "እባክዎ በእንግሊዘኛ ይናገሩ...")
        }
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "ስህተት", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                textView.text = "የተሰማው (EN)፦ $spokenText\n\nእየተተረጎመ ነው..."
                
                if (isModelDownloaded) {
                    englishAmharicTranslator.translate(spokenText)
                        .addOnSuccessListener { translatedText ->
                            textView.text = "የተሰማው (EN)፦ $spokenText\n\nትርጉም (AM)፦ $translatedText"
                        }
                        .addOnFailureListener {
                            textView.text = "የተሰማው (EN)፦ $spokenText\n\n❌ በትክክል መተርጎም አልተቻለም።"
                        }
                } else {
                    textView.text = "⚠️ ፋይሉ ገና ስላልወረደ መተርጎም አይችልም!"
                }
            }
        }
    }
}
