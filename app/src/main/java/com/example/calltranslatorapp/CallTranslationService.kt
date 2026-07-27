package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import kotlin.concurrent.thread

class CallTranslationService : Service() {

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "CALL_TRANSLATOR_CHANNEL")
                .setContentTitle("Offline Amharic Translator")
                .setContentText("Real-Time VoIP Call Dubbing Engine Active...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        } else {
            Notification()
        }
        
        startForeground(1001, notification)
        startAudioStreamProcessingLoop()
        return START_STICKY
    }

    private fun startAudioStreamProcessingLoop() {
        if (isRecording) return
        isRecording = true

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()

            thread {
                val pcmBuffer = ByteArray(1600) // 100ms Chunk ለዝቅተኛ መዘግየት (Low Latency)
                while (isRecording) {
                    val bytesRead = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                    if (bytesRead > 0) {
                        // ⚡ እዚህ ጋር የወጣው የ 100ms raw PCM አሬይ በቀጥታ ወደ WebSocket Streaming STT ይላካል
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CALL_TRANSLATOR_CHANNEL",
                "Call Translation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
