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
    private val SPEECH_REQUEST_CODE = 100

    // እዚህ ላይ በቋንቋ ታግ (am) በቀጥታ እንዲለይ ተደርጓል፤ ስህተት አይፈጥርም
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
            text = "የጥሪ መተርገሚያ አፕሊኬሽን\n(እንግሊዘኛ ➡️ አማርኛ)"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "ማዳመጥ እና መተርጎም ጀምር"
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

        textView.text = "የትርጉም ማሽን በመዘጋጀት ላይ..."
        val conditions = DownloadConditions.Builder().build()
        englishAmharicTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                textView.text = "የጥሪ መተርገሚያ አፕሊኬሽን\nዝግጁ ነው!"
            }
            .addOnFailureListener {
                textView.text = "የትርጉም ሞዴል ማውረድ አልተቻለም። ኢንተርኔት ያብሩ!"
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
                    .addOnFailureListener {
                        textView.text = "ትርጉም አልተሳካም!"
                    }
            }
        }
    }
}
