package com.example.calltranslatorapp

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

class CallAudioPipeline {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000 // Optimized 16kHz Mono PCM for AI processing[span_2](start_span)[span_2](end_span)
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun startCapture(onAudioFrameAvailable: (ByteArray) -> Unit) {
        if (isRecording) return
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioPipeline", "Failed to initialize AudioRecord Structure.")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            // Dedicated background processing thread to prevent UI lags[span_3](start_span)[span_3](end_span)
            Thread {
                val audioBuffer = ByteArray(bufferSize)
                while (isRecording) {
                    val readBytes = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readBytes > 0) {
                        val frameData = audioBuffer.copyOf(readBytes)
                        onAudioFrameAvailable(frameData)
                    }
                }
            }.start()
            
            Log.d("AudioPipeline", "VoIP Audio Pipeline initiated successfully.")
        } catch (e: Exception) {
            Log.e("AudioPipeline", "Exception in Audio Pipeline Initialization: ${e.message}")
        }
    }

    fun stopCapture() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {}
    }
}
