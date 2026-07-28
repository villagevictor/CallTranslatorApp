package com.example.calltranslatorapp.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TranslationWebSocketClient {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val outgoingAudioChannel = Channel<ByteArray>(Channel.UNLIMITED)

    private val _incomingTranslatedAudio = MutableSharedFlow<ByteArray>()
    val incomingTranslatedAudio = _incomingTranslatedAudio.asSharedFlow()

    private val _incomingSubtitles = MutableSharedFlow<String>()
    val incomingSubtitles = _incomingSubtitles.asSharedFlow()

    fun connectAndStream(serverUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.webSocket(urlString = serverUrl) {
                    session = this

                    // Outgoing 100ms PCM Chunk Streamer
                    val senderJob = launch {
                        for (audioChunk in outgoingAudioChannel) {
                            send(Frame.Binary(true, audioChunk))
                        }
                    }

                    // Incoming Translation Receiver
                    val receiverJob = launch {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Binary -> {
                                    _incomingTranslatedAudio.emit(frame.readBytes())
                                }
                                is Frame.Text -> {
                                    _incomingSubtitles.emit(frame.readText())
                                }
                                else -> {}
                            }
                        }
                    }

                    senderJob.join()
                    receiverJob.join()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        outgoingAudioChannel.trySend(pcmChunk)
    }

    fun disconnect() {
        session = null
        client.close()
    }
}
