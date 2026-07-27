package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var statusText: TextView? = null
    private var btnCall: Button? = null
    private var isCallRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(50, 50, 50, 50)
        }

        statusText = TextView(this).apply {
            text = "🌐 Offline Amharic - Low Latency Calling Engine"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }
        rootLayout.addView(statusText)

        btnCall = Button(this).apply {
            text = "🟢 ጥሪ ጀምር (Start Dual-Stream Call)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(40, 30, 40, 30)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 20f
            }
        }
        btnCall?.setOnClickListener { toggleVoIPCall() }
        rootLayout.addView(btnCall)

        setContentView(rootLayout)
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MANAGE_OWN_CALLS
        )
        val missingPermissions = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), 200)
        }
    }

    private fun toggleVoIPCall() {
        val serviceIntent = Intent(this, CallTranslationService::class.java)
        if (!isCallRunning) {
            startService(serviceIntent)
            isCallRunning = true
            statusText?.text = "📞 በጥሪ ላይ ነዎት... (የቀጥታ ድምፅ ትርጉም በንቃት ላይ)"
            statusText?.setTextColor(Color.parseColor("#10B981"))
            btnCall?.text = "🔴 ጥሪውን አቁም (End Call)"
            btnCall?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444"))
                cornerRadius = 20f
            }
            Toast.makeText(this, "የድምፅ መስመሩ በቅጽበት ተከፍቷል!", Toast.LENGTH_SHORT).show()
        } else {
            stopService(serviceIntent)
            isCallRunning = false
            statusText?.text = "🌐 Offline Amharic - Low Latency Calling Engine"
            statusText?.setTextColor(Color.WHITE)
            btnCall?.text = "🟢 ጥሪ ጀምር (Start Dual-Stream Call)"
            btnCall?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 20f
            }
        }
    }
}
