package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnable = findViewById<Button>(R.layout.activity_main) // በተኑን ማገናኘት
        
        // በተኑ ሲነካ የሚሰራው ስራ
        findViewById<Button>(resources.getIdentifier("btn_enable_service", "id", packageName)).setOnClickListener {
            try {
                Toast.makeText(this, "እባክዎ 'CallTranslator' የሚለውን አብሩት", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "ገጹን መክፈት አልተቻለም: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
