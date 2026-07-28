package com.example.calltranslatorapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import kotlin.concurrent.thread

class CallTranslationService : Service() {

    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = Notification.Builder(this, "CALL_TRANSLATION_CHANNEL")
            .setContentTitle("imo Live Translation Active")
            .setContentText("በቅጽበት የድምፅ ትርጉም እና HD ጥሪ እየሰራ ይገኛል...")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .build()

        startForeground(1, notification)
        startAudioCapturePipeline()
        return START_STICKY
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
                val buffer = ByteArray(bufferSize)

                while (isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        // Raw PCM 16bit Audio Stream
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
