package com.example.calltranslatorapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.pm.PackageManager
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var textView: TextView
    private val SPEECH_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ገጽታውን በኮድ መፍጠር
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        textView = TextView(this).apply {
            text = "የጥሪ መተርገሚያ አፕሊኬሽን\n(Google Speech to Text)"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "ማዳመጥ ጀምር"
            setOnClickListener {
                // መጀመሪያ የማይክሮፎን ፈቃድ መኖሩን ማረጋገጥ
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startSpeechToText()
                } else {
                    // ፈቃድ ከሌለ ተጠቃሚውን መጠየቅ
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
                }
            }
        }

        layout.addView(textView)
        layout.addView(button)
        setContentView(layout)
    }

    // የGoogle Speech to Text ማዳመጫ መስኮት መክፈቻ
    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // የስልክህን ቋንቋ ይወስዳል
            putExtra(RecognizerIntent.EXTRA_PROMPT, "እየሰማሁ ነው... ይናገሩ")
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
            textView.text = "ድምጽ በማዳመጥ ላይ..."
        } catch (e: Exception) {
            Toast.makeText(this, "የGoogle ድምጽ መለዮ ስልክዎ ላይ አልተጫነም", Toast.LENGTH_SHORT).show()
        }
    }

    // ድምጹ ተሰምቶ ሲያበቃ ውጤቱን የምንቀበልበት ክፍል
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: "ድምጽ መለየት አልተቻለም"
            
            // የተናገርከውን ድምጽ ወደ ጽሑፍ ቀይሮ ስክሪኑ ላይ ያሳየዋል
            textView.text = "የተሰማው ጽሑፍ፦\n$spokenText"
        }
    }
}
