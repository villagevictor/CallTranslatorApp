package com.example.calltranslatorapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var textView: TextView
    private lateinit var button: Button
    private val SPEECH_REQUEST_CODE = 100

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage("en")
        .setTargetLanguage("am")
        .build()
    private val englishAmharicTranslator = Translation.getClient(options)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        textView = TextView(this).apply {
            text = "የትርጉም ማሽን ከጉግል ላይ በመውረድ ላይ ነው...\nእባክዎ ኢንተርኔት ያብሩ እና ጥቂት ሰከንድ ይጠብቁ።"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
        }

        button = Button(this).apply {
            text = "ማዳመጥ እና መተርጎም ጀምር"
            isEnabled = false // ፋይሉ እስኪወርድ በተኑ እንዳይሰራ እናግደዋለን
            setOnClickListener {
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startSpeechToText()
                } else {
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
                }
            }
        }

        layout.addView(textView)
        layout.addView(button)
        setContentView(layout)

        // ያለምንም ቅድመ ሁኔታ በዳታም በዋይፋይም እንዲያወርድ ማድረግ
        val conditions = DownloadConditions.Builder().build()
        englishAmharicTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // ፋይሉ ወርዶ ሲያበቃ አፑ ዝግጁ ይሆናል
                textView.text = "የጥሪ መተርገሚያ አፕሊኬሽን\n(እንግሊዘኛ ➡️ አማርኛ)\n\nአፑ አሁን ዝግጁ ነው!"
                button.isEnabled = true
            }
            .addOnFailureListener { e ->
                textView.text = "የትርጉም ፋይሉን ማውረድ አልተቻለም።\nኢንተርኔት መኖሩን ያረጋግጡ!\nስህተት፦ ${e.message}"
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
            Toast.makeText(this, "ስህተት አጋጥሟል", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""

            if (spokenText.isNotEmpty()) {
                textView.text = "የተሰማው (EN)፦ $spokenText\n\nእየተተረጎመ ነው..."
                
                englishAmharicTranslator.translate(spokenText)
                    .addOnSuccessListener { translatedText ->
                        textView.text = "የተሰማው (EN)፦ $spokenText\n\nትርጉም (AM)፦ $translatedText"
                    }
                    .addOnFailureListener { e ->
                        textView.text = "ትርጉም አልተሳካም!\nምክንያት፦ ${e.message}"
                    }
            }
        }
    }
}
