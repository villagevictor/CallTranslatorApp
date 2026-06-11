package com.example.calltranslatorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. ለአንድሮይድ 13+ የኖቲፊኬሽን ፍቃድ መጠየቅ
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 102)
            }
        }

        // 2. በስክሪን ላይ የመሳል (Display over other apps) ፍቃድ መጠየቅ
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "እባክዎ ለ CallTranslator በስክሪን ላይ የመታየት ፍቃድ ይስጡ", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        val btnId = resources.getIdentifier("btn_enable_service", "id", packageName)
        findViewById<Button>(btnId).setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "ገጹን መክፈት አልተቻለም: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
