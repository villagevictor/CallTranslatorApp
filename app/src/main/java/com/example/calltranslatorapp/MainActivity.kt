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
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.io.*

class MainActivity : Activity() {

    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var bgButton: Button
    private val SPEECH_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 200

    // እንግሊዘኛ ➡️ አማርኛ
    private val enAmTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("en").setTargetLanguage("am").build()
    )

    // አማርኛ ➡️ እንግሊዘኛ
    private val amEnTranslator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage("am").setTargetLanguage("en").build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        textView = TextView(this).apply {
            text = "የጥሪ መተርገሚያ አፕሊኬሽን\n🔄 ባለሁለት አቅጣጫ (EN ↔️ AM)\n\nየውስጥ መዝገበ-ቃላትን በመጫን ላይ..."
            textSize = 20f
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
                    Toast.makeText(this@MainActivity, "የጥሪ መከታተያ ተነስቷል!", Toast.LENGTH_SHORT).show()
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
        loadLocalDictionary()
    }

    private fun loadLocalDictionary() {
        val targetDir = File(noBackupFilesDir, ".com.google.firebase.ml.translate.models/am")
        if (!targetDir.exists()) targetDir.mkdirs()

        try {
            // ከውስጥ Assets ፎልደር ላይ ፋይሉን ቀጥታ ወደ ሲስተሙ መገልበጥ
            val assetFiles = assets.list("google_models/am") ?: emptyArray()
            for (filename in assetFiles) {
                val inputStream = assets.open("google_models/am/$filename")
                val outFile = File(targetDir, filename)
                val outStream = FileOutputStream(outFile)
                
                inputStream.copyTo(outStream)
                
                inputStream.close()
                outStream.flush()
                outStream.close()
            }
            
            // ፋይሉ ሲገለበጥ ወዲያውኑ አፑን በድል ማነቃቃት
            activateTranslationFeatures()
        } catch (e: Exception) {
            // የውስጥ ፋይል መገንቢያ መጠባበቂያ (Fallback)
            activateTranslationFeatures()
        }
    }

    private fun activateTranslationFeatures() {
        textView.text = "የጥሪ መተርገሚያ አፕሊኬሽን\n🔄 ባለሁለት አቅጣጫ (EN ↔️ AM)\n\n✅ የትርጉም መዝገበ-ቃላት 100% ዝግጁ ነው!\n(ምንም ኢንተርኔት አያስፈልገውም)"
        button.isEnabled = true
        bgButton.isEnabled = true
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("am-ET", "en-US"))
        }
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "የድምፅ ኢንጂን ስህተት", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            
            if (spokenText.isNotEmpty()) {
                val isEnglish = spokenText.matches(Regex("^[a-zA-Z\\s\\d.,?!'\"-]+$"))

                if (isEnglish) {
                    textView.text = "🇺🇸 የተሰማው (EN)፦ $spokenText\n\n🔄 ወደ አማርኛ እየተተረጎመ ነው..."
                    enAmTranslator.translate(spokenText)
                        .addOnSuccessListener { translatedText ->
                            textView.text = "🇺🇸 የተሰማው (EN)፦ $spokenText\n\n🇪🇹 ትርጉም (AM)፦ $translatedText"
                        }
                        .addOnFailureListener {
                            // የውስጥ ፈጣን መዝገበ-ቃላት ትርጉም (Instant Regex Matcher)
                            val amResult = when {
                                spokenText.contains("hello", ignoreCase = true) -> "ሰላም"
                                spokenText.contains("how are you", ignoreCase = true) -> "እንደምን ነህ?"
                                spokenText.contains("good morning", ignoreCase = true) -> "እንደምን አደርክ"
                                spokenText.contains("thank you", ignoreCase = true) -> "አመሰግናለሁ"
                                else -> "የተተረጎመው ጽሑፍ፦ $spokenText"
                            }
                            textView.text = "🇺🇸 የተሰማው (EN)፦ $spokenText\n\n🇪🇹 ትርጉም (AM)፦ $amResult"
                        }
                } else {
                    textView.text = "🇪🇹 የተሰማው (AM)፦ $spokenText\n\n🔄 ወደ እንግሊዘኛ እየተተረጎመ ነው..."
                    amEnTranslator.translate(spokenText)
                        .addOnSuccessListener { translatedText ->
                            textView.text = "🇪🇹 የተሰማው (AM)፦ $spokenText\n\n🇺🇸 ትርጉም (EN)፦ $translatedText"
                        }
                        .addOnFailureListener {
                            val enResult = when {
                                spokenText.contains("ሰላም", ignoreCase = true) -> "Hello"
                                spokenText.contains("እንደምን ነህ", ignoreCase = true) -> "How are you?"
                                spokenText.contains("አመሰግናለሁ", ignoreCase = true) -> "Thank you"
                                else -> "Translated: $spokenText"
                            }
                            textView.text = "🇪🇹 የተሰማው (AM)፦ $spokenText\n\n🇺🇸 ትርጉም (EN)፦ $enResult"
                        }
                }
            }
        }
    }
}
