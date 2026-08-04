package com.example.calltranslatorapp.network

import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString

class TranslationWebSocketClient(private val serverUrl: String) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connection opened
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Incoming audio bytes from AI streaming server (Phase 4)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
            }
        })
    }

    fun sendAudio(audioBytes: ByteArray) {
        webSocket?.send(audioBytes.toByteString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnecting")
    }
}
