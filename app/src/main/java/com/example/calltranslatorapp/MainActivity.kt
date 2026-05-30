package com.example.calltranslatorapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // የአፑን ገጽታ በኮድ እንፍጠር
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            padding = 50
        }

        val textView = TextView(this).apply {
            text = "የጥሪ መተርገሚያ አፕሊኬሽን"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "ትርጉም ጀምር"
            setOnClickListener {
                textView.text = "ድምጽ በማዳመጥ ላይ..."
            }
        }

        layout.addView(textView)
        layout.addView(button)
        setContentView(layout)
    }
}
