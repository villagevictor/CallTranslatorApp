package com.example.calltranslatorapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calltranslatorapp.service.AudioCallService

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val statusText = TextView(this).apply {
            text = "Real-Time Call Translator"
            textSize = 20f
            setPadding(0, 0, 0, 40)
        }

        val startButton = Button(this).apply {
            text = "Start Translation Service"
            setOnClickListener {
                if (checkPermissions()) {
                    startTranslationService()
                    statusText.text = "Service Status: RUNNING"
                } else {
                    requestPermissions()
                }
            }
        }

        val stopButton = Button(this).apply {
            text = "Stop Translation Service"
            setOnClickListener {
                stopTranslationService()
                statusText.text = "Service Status: STOPPED"
            }
        }

        layout.addView(statusText)
        layout.addView(startButton)
        layout.addView(stopButton)
        setContentView(layout)
    }

    private fun startTranslationService() {
        val serviceIntent = Intent(this, AudioCallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopTranslationService() {
        val serviceIntent = Intent(this, AudioCallService::class.java)
        stopService(serviceIntent)
    }

    private fun checkPermissions(): Boolean {
        val micPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return micPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            ),
            PERMISSION_REQUEST_CODE
        )
    }
}
