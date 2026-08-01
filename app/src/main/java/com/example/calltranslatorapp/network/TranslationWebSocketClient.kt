package com.example.calltranslatorapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class TranslationWebSocketClient(private val serverUrl: String) {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(onTextReceived: (String) -> Unit) {
        scope.launch {
            try {
                session = client.webSocketSession(serverUrl)
                session?.incoming?.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        onTextReceived(text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendAudioChunk(audioBytes: ByteArray) {
        scope.launch {
            try {
                session?.send(Frame.Binary(true, audioBytes))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
