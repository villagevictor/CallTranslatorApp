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
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import okhttp3.*
import java.io.*
import java.util.zip.ZipInputStream

class MainActivity : Activity() {

    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var bgButton: Button
    private val SPEECH_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 200

    // 1. እንግሊዘኛ ➡️ አማርኛ ተርጓሚ
    private val enAmOptions = TranslatorOptions.Builder()
        .setSourceLanguage("en")
        .setTargetLanguage("am")
        .build()
    private val enAmTranslator = Translation.getClient(enAmOptions)

    // 2. አማርኛ ➡️ እንግሊዘኛ ተርጓሚ
    private val amEnOptions = TranslatorOptions.Builder()
        .setSourceLanguage("am")
        .setTargetLanguage("en")
        .build()
    private val amEnTranslator = Translation.getClient(amEnOptions)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        textView = TextView(this).apply {
            text = "የጥሪ መተርገሚያ አፕሊኬሽን\n🔄 ባለሁለት አቅጣጫ (EN ↔️ AM)\n\nየመዝገበ-ቃላት ፋይሎችን በመፈተሽ ላይ..."
            textSize = 20f
            gravity = Gravity.CENTER
        }

        button = Button(this).apply {
            text = "መናገር እና መተርጎም ጀምር"
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
        checkAndSetupOfflineModels()
    }

    private fun checkAndSetupOfflineModels() {
        // ሁለቱም የአማርኛ እና የእንግሊዘኛ ኦፍላይን ማውረጃ ፎልደሮች መኖራቸውን ማረጋገጫ
        val amDir = File(noBackupFilesDir, ".com.google.firebase.ml.translate.models/am")
        val enDir = File(noBackupFilesDir, ".com.google.firebase.ml.translate.models/en")
        
        if (amDir.exists() && amDir.list()?.isNotEmpty() == true) {
            activateTranslationFeatures()
            return
        }

        textView.text = "🚀 የመዝገበ-ቃላት ውቅረትን በማዘጋጀት ላይ...\n\nእባክዎ 1 ደቂቃ ያህል በትዕግስት ይጠብቁ።"

        // የዚፕ ፋይል ማውረጃ (ከራሳችን ፈጣን R2 ሲስተም)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://pub-c2a4ef99df3d463cb967be2f067468de.r2.dev/am_en.zip")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // የመጀመሪያው ካልሰራ ወደ ML Kit ይፋዊ ሰርቨር ቀይር (Fallback)
                runOnUiThread { triggerGoogleOfficialDownload() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread { triggerGoogleOfficialDownload() }
                    return
                }

                try {
                    val body = response.body ?: return
                    if (!amDir.exists()) amDir.mkdirs()

                    ZipInputStream(body.byteStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val outFile = File(amDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                    runOnUiThread { activateTranslationFeatures() }
                } catch (e: Exception) {
                    runOnUiThread { triggerGoogleOfficialDownload() }
                }
            }
        })
    }

    // ከሰርቨር ማውረድ ካልተቻለ በጎግል በኩል በደኅንነት እንዲያወርድ የሚያደርግ መከላከያ ኢንጂን
    private fun triggerGoogleOfficialDownload() {
        textView.text = "🔄 ከዋናው ሰርቨር ጋር በመገናኘት ላይ...\nእባክዎ ከመተግበሪያው ሳይወጡ ይጠብቁ።"
        val modelManager = RemoteModelManager.getInstance()
        val amModel = TranslateRemoteModel.Builder("am").build()
        
        modelManager.download(amModel, com.google.mlkit.common.model.DownloadConditions.Builder().build())
            .addOnSuccessListener {
                activateTranslationFeatures()
            }
            .addOnFailureListener { e ->
                textView.text = "❌ ፋይሉን ማዘጋጀት አልተቻለም፦ ${e.message}\nእባክዎ የኢንተርኔት ግንኙነትዎን ፈትሸው እንደገና ይክፈቱት።"
            }
    }

    private fun activateTranslationFeatures() {
        textView.text = "የጥሪ መተርገሚያ አፕሊኬሽን\n🔄 ባለሁለት አቅጣጫ (EN ↔️ AM)\n\n✅ የትርጉም መዝገበ-ቃላት 100% ዝግጁ ነው!\n(አሁን ያለ ኢንተርኔት በነፃ መተርጎም ይችላሉ)"
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
            // ሁለቱንም ቋንቋዎች በአንድ ላይ እንዲያዳምጥ እናዝዘዋለን
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("am-ET", "en-US"))
        }
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "የድምፅ ኢንጂን አልተገኘም", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            
            if (spokenText.isNotEmpty()) {
                // የተነገረው ቋንቋ እንግሊዘኛ መሆኑን በፊደላቱ መለየት (Regex)
                val isEnglish = spokenText.matches(Regex("^[a-zA-Z\\s\\d.,?!'\"-]+$"))

                if (isEnglish) {
                    // እንግሊዘኛ ከሆነ ➡️ ወደ አማርኛ ተርጉም
                    textView.text = "🇺🇸 የተሰማው (EN)፦ $spokenText\n\n🔄 ወደ አማርኛ እየተተረጎመ ነው..."
                    enAmTranslator.translate(spokenText)
                        .addOnSuccessListener { translatedText ->
                            textView.text = "🇺🇸 የተሰማው (EN)፦ $spokenText\n\n🇪🇹 ትርጉም (AM)፦ $translatedText"
                        }
                        .addOnFailureListener {
                            textView.text = "🇺🇸 የተሰማው (EN)፦ $spokenText\n\n❌ የመተርጎም ስህተት አጋጠመ።"
                        }
                } else {
                    // አማርኛ ከሆነ ➡️ ወደ እንግሊዘኛ ተርጉም
                    textView.text = "🇪🇹 የተሰማው (AM)፦ $spokenText\n\n🔄 ወደ እንግሊዘኛ እየተተረጎመ ነው..."
                    amEnTranslator.translate(spokenText)
                        .addOnSuccessListener { translatedText ->
                            textView.text = "🇪🇹 የተሰማው (AM)፦ $spokenText\n\n🇺🇸 ትርጉም (EN)፦ $translatedText"
                        }
                        .addOnFailureListener {
                            textView.text = "🇪🇹 የተሰማው (AM)፦ $spokenText\n\n❌ የመተርጎም ስህተት አጋጠመ።"
                        }
                }
            }
        }
    }
}
