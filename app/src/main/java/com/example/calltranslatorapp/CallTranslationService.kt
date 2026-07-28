package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.IBinder
import com.example.calltranslatorapp.network.TranslationWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class CallTranslationService : Service() {

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val webSocketClient = TranslationWebSocketClient()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = Notification.Builder(this, "CALL_TRANSLATION_CHANNEL")
            .setContentTitle("imo Live Translation Active")
            .setContentText("በቅጽበት የድምፅ ትርጉም እና HD ጥሪ እየሰራ ይገኛል...")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .build()

        startForeground(1, notification)
        
        // 1. Connect to AI Cloud Gateway (WebSocket)
        // ማስታወሻ፦ እዚህ ጋር የምንጠቀመውን የ Cloud AI Gateway URL በኋላ እናስገባለን
        webSocketClient.connectAndStream("wss://your-ai-gateway-url.com/translate")

        // 2. Start Audio Playback Engine (ከሰርቨሩ የሚመጣውን የተተረጎመ ድምፅ ማሰሚያ)
        initAudioTrack()

        // 3. Start Audio Capture Pipeline (የራሳችንን ድምፅ መቅረጫ)
        startAudioCapturePipeline()

        return START_STICKY
    }

    private fun initAudioTrack() {
        val sampleRate = 16000
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioTrack?.play()

        // ከ WebSocket የሚመጣውን የተተረጎመ ድምፅ በጆሮ ማሰማት
        CoroutineScope(Dispatchers.IO).launch {
            webSocketClient.incomingTranslatedAudio.collect { audioBytes ->
                audioTrack?.write(audioBytes, 0, audioBytes.size)
            }
        }
    }

    private fun startAudioCapturePipeline() {
        isRecording = true
        thread {
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                audioRecord?.startRecording()
                
                // 100ms Chunk Size for Low Latency (16000 samples/sec * 2 bytes/sample * 0.1 sec = 3200 bytes)
                val chunkSize = 3200
                val buffer = ByteArray(chunkSize)

                while (isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        // Raw 100ms PCM Chunk ወደ AI Cloud በ WebSocket መላክ
                        webSocketClient.sendAudioChunk(buffer.copyOf(readBytes))
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioTrack?.stop()
        audioTrack?.release()
        webSocketClient.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "CALL_TRANSLATION_CHANNEL",
            "Call Translation Engine",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
