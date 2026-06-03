package com.example.calltranslatorapp

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.widget.Toast
import android.Manifest

class MainActivity : Activity() {

    private lateinit var textView: TextView
    private lateinit var bgButton: Button
    private val PERMISSION_REQUEST_CODE = 200
    private val OVERLAY_REQUEST_CODE = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        textView = TextView(this).apply {
            text = "የጥሪ መተርገሚያ አፕሊኬሽን\n🔄 ሙሉ በሙሉ አውቶማቲክ ስሪት\n\n✅ የትርጉም ሞተር ዝግጁ ነው!"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        bgButton = Button(this).apply {
            text = "የበስተጀርባ የጥሪ መተርገሚያ አስጀምር"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    // ተንሳፋፊ ፈቃድ ከሌለ ወደ ሳምሰንግ ሲስተም ሴቲንግ መውሰድ
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, OVERLAY_REQUEST_CODE)
                } else {
                    startTranslationService()
                }
            }
        }

        layout.addView(textView)
        layout.addView(bgButton)
        setContentView(layout)

        checkAndRequestPermissions()
    }

    private fun startTranslationService() {
        try {
            val intent = Intent(this, CallTranslationService::class.java)
            startForegroundService(intent)
            Toast.makeText(this, "🚀 አውቶማቲክ የጥሪ መከታተያ ተነስቷል!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "ማስነሳት አልተቻለም፦ ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add("android.permission.POST_NOTIFICATIONS")
            }
        }
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissions(permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startTranslationService()
            } else {
                Toast.makeText(this, "❌ ተንሳፋፊ መስኮት ፈቃድ አልተሰጠም!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
