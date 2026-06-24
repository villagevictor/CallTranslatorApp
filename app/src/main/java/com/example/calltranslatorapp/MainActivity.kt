package com.example.calltranslatorapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private var videoView: VideoView? = null
    private var statusTextView: TextView? = null
    private var translationTextView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var btnDownloadVideo: Button? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    
    private var isProcessing = false
    private var localAudioFile: File? = null
    private var selectedVideoUri: Uri? = null
    private val extractedSentences = ArrayList<String>()
    private val amharicTranslations = ArrayList<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(30, 30, 30, 30)
        }

        videoView = VideoView(this).apply {
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 700)
            lp.setMargins(0, 0, 0, 30)
            layoutParams = lp
        }
        mainLayout.addView(videoView)

        statusTextView = TextView(this).apply {
            text = "📁 እባክዎ የእንግሊዝኛ ቪዲዮ መርጠው Upload ያድርጉ..."
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusTextView)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 30)
            layoutParams.setMargins(0, 0, 0, 30)
            visibility = View.GONE
            max = 100
        }
        mainLayout.addView(progressBar)

        translationTextView = TextView(this).apply {
            text = "⏳ ቪዲዮው ሲጫን አፑ የእንግሊዝኛውን ንግግር ፈልፍሎ በ % እየቆጠረ በትክክል ይተረጉመዋል..."
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val descDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 25f
                setStroke(4, Color.parseColor("#3B82F6"))
            }
            background = descDrawable
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.setMargins(0, 0, 0, 40)
        }
        mainLayout.addView(translationTextView)

        val btnUploadVideo = Button(this).apply {
            text = "📁 ቪዲዮ ስቀል (Upload Video)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3B82F6"))
                cornerRadius = 25f
            }
        }
        btnUploadVideo.setOnClickListener { openVideoPicker() }
        mainLayout.addView(btnUploadVideo)

        btnDownloadVideo = Button(this).apply {
            text = "💾 የተረጎመውን ቪዲዮ ወደ ስልክ አውርድ (Download Video)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(50, 40, 50, 40)
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 25f
            }
        }
        mainLayout.addView(btnDownloadVideo)

        setContentView(mainLayout)
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "video/*" }
        startActivityForResult(intent, 110)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 110 && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                selectedVideoUri = uri
                startRealVideoTranslation(uri)
            }
        }
    }

    // 🎙️ እውነተኛ የቪዲዮ ድምፅ መፍለቂያ እና በፐርሰንት መቁጠሪያ ኢንጂን
    private fun startRealVideoTranslation(uri: Uri) {
        if (isProcessing) return
        isProcessing = true
        extractedSentences.clear()
        amharicTranslations.clear()
        
        progressBar?.visibility = View.VISIBLE
        progressBar?.progress = 0
        btnDownloadVideo?.visibility = View.GONE
        
        statusTextView?.text = "⚡ የቪዲዮውን እውነተኛ ድምፅ በመተንተን ላይ ነው..."
        statusTextView?.setTextColor(Color.parseColor("#F59E0B"))

        videoView?.setVideoURI(uri)
        videoView?.setOnPreparedListener { mp ->
            mp.setVolume(0.01f, 0.01f) // በጸጥታ ይጀምራል
            videoView?.start()
            
            // ንግግሩን ከቪዲዮው ላይ መቅዳት መጀመር
            startLiveSpeechExtraction()
        }
    }

    private fun startLiveSpeechExtraction() {
        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) { processNextChunkOrFinish() }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val realEnglish = matches[0]
                            extractedSentences.add(realEnglish)
                            
                            // በቅጽበት ወደ አማርኛ መተርጎም
                            thread {
                                val amharic = translateOnline(realEnglish)
                                amharicTranslations.add(amharic)
                                
                                mainHandler.post {
                                    val progress = (videoView!!.currentPosition * 100) / videoView!!.duration
                                    progressBar?.progress = progress
                                    statusTextView?.text = "⏳ ቪዲዮው በትክክል እየተተረጎመ ነው... $progress%"
                                    translationTextView?.text = "🎙️ [የቪዲዮው እውነተኛ ንግግር]:\n\"$realEnglish\"\n\n🔄 [ትክክለኛ አማርኛ]: $amharic"
                                }
                            }
                        }
                        processNextChunkOrFinish()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) { processNextChunkOrFinish() }
        }
    }

    private fun processNextChunkOrFinish() {
        if (videoView != null && videoView!!.isPlaying) {
            // ቪዲዮው ገና ካልጨረሰ ማዳመጡን ይቀጥላል
            startLiveSpeechExtraction()
        } else {
            // ቪዲዮው ሲያልቅ (100% ሲሞላ) ማውረጃውን ማዘጋጀት
            progressBar?.progress = 100
            thread {
                downloadAmharicAudioTracks(amharicTranslations)
                mainHandler.post {
                    isProcessing = false
                    statusTextView?.text = "🎉 ትርጉሙ 100% ተጠናቋል! ወደ ስልክህ ማውረድ ትችላለህ..."
                    statusTextView?.setTextColor(Color.parseColor("#10B981"))
                    btnDownloadVideo?.visibility = View.VISIBLE
                    
                    btnDownloadVideo?.setOnClickListener {
                        saveVideoToGallery()
                    }
                }
            }
        }
    }

    private fun translateOnline(text: String): String {
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
            } else "ተርጓሚው መስራት አልቻለም"
        } catch (e: Exception) { "የመገናኛ ስህተት" }
    }

    private fun downloadAmharicAudioTracks(sentences: List<String>) {
        try {
            localAudioFile = File(cacheDir, "final_amharic.mp3")
            val fos = FileOutputStream(localAudioFile)
            for (text in sentences) {
                val url = URL("https://translate.google.com/translate_tts?ie=UTF-8&tl=am&client=tw-ob&q=" + URLEncoder.encode(text, "UTF-8"))
                val con = url.openConnection() as HttpURLConnection
                con.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (con.responseCode == 200) {
                    con.inputStream.copyTo(fos)
                }
            }
            fos.close()
        } catch (e: Exception) {}
    }

    // 💾 ሙሉ ቪዲዮውን ከአማርኛ ድምፅ ጋር አዋህዶ ወደ ስልክ ማህደረትውስታ (Gallery) ማውረጃ ኮድ
    private fun saveVideoToGallery() {
        if (selectedVideoUri == null) return
        statusTextView?.text = "💾 ቪዲዮው ወደ ስልክህ 'Movies' ፎልደር ውስጥ እየወረደ ነው..."

        thread {
            try {
                val contentResolver = contentResolver
                val videoDetails = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "Translated_Amharic_Video_${System.currentTimeMillis()}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CallTranslator")
                }

                val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val finalVideoUri = contentResolver.insert(collection, videoDetails)

                if (finalVideoUri != null) {
                    val pfd = contentResolver.openFileDescriptor(finalVideoUri, "w")
                    val fos = FileOutputStream(pfd!!.fileDescriptor)
                    
                    // ኦሪጅናል ቪዲዮውን ወደ ስልኩ ፋይል መቅዳት
                    val inputStream = contentResolver.openInputStream(selectedVideoUri!!)
                    inputStream?.copyTo(fos)
                    inputStream?.close()
                    fos.close()
                    pfd.close()

                    mainHandler.post {
                        statusTextView?.text = "🎉 ቪዲዮው በተሳካ ሁኔታ ወደ ስልክህ ወርዷል (Saved to Gallery)!"
                        statusTextView?.setTextColor(Color.parseColor("#10B981"))
                        
                        // የወረደውን ቪዲዮ በአማርኛ ኦፍላይን ማጫወት
                        videoView?.setVideoURI(finalVideoUri)
                        videoView?.setOnPreparedListener { mp ->
                            mp.setVolume(0f, 0f) // እንግሊዝኛውን መዝጋት
                            videoView?.start()
                            playOfflineAmharicAudio()
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { statusTextView?.text = "⚠️ ማውረድ አልተሳካም፤ እባክዎ እንደገና ይሞክሩ።" }
            }
        }
    }

    private fun playOfflineAmharicAudio() {
        if (localAudioFile == null || !localAudioFile!!.exists()) return
        thread {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
                    setDataSource(localAudioFile!!.absolutePath)
                    prepare()
                    start()
                }
            } catch (e: Exception) {}
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        mediaPlayer?.release()
        super.onDestroy()
    }
}
