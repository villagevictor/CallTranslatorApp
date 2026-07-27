package com.example.calltranslatorapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var statusTextView: TextView? = null
    private var liveSubtitleView: TextView? = null
    private var btnStartCall: Button? = null
    private var isCallActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Main Container (Dark Theme IMO Style Layout)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B141B")) // Deep IMO Dark Blue
            setPadding(30, 40, 30, 40)
        }

        // Header Section
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 10, 10, 30)
        }

        val appTitle = TextView(this).apply {
            text = "imo Translator Pro"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00A884")) // IMO Brand Accent Color
        }
        headerLayout.addView(appTitle)
        mainLayout.addView(headerLayout)

        // Status Card
        statusTextView = TextView(this).apply {
            text = "● ዝግጁ ነው (Ready for HD Live Translation Call)"
            textSize = 14f
            setTextColor(Color.parseColor("#8696A0"))
            setPadding(20, 20, 20, 20)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111B21"))
                cornerRadius = 16f
            }
        }
        mainLayout.addView(statusTextView)

        // Recent Calls / Chat Section Title
        val sectionTitle = TextView(this).apply {
            text = "የቅርብ ጊዜ ጥሪዎች (Recent Translated Calls)"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E9EDEF"))
            setPadding(10, 30, 10, 15)
        }
        mainLayout.addView(sectionTitle)

        // Scrollable Call Log List (IMO Style Contact Items)
        val scrollContainer = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        val callListLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Sample IMO Contact 1
        callListLayout.addView(createContactItem("አሸናፊ (Ashenafi)", "🇺🇸 እንግሊዘኛ ⇆ 🇪🇹 አማርኛ", "10:45 AM"))
        callListLayout.addView(createContactItem("Abebe Kebede", "🇪🇹 አማርኛ ⇆ 🇺🇸 English", "Yesterday"))
        callListLayout.addView(createContactItem("John Doe (US Office)", "🇺🇸 English ⇆ 🇪🇹 Amharic", "July 24"))

        scrollContainer.addView(callListLayout)
        mainLayout.addView(scrollContainer)

        // Live Subtitle Overlay Box (የቀጥታ ትርጉም ማሳያ)[span_5](start_span)[span_5](end_span)
        liveSubtitleView = TextView(this).apply {
            text = "🎙️ ጥሪ ሲጀምር የተተረጎመው ጽሁፍ እዚህ ጋር በቅጽበት ይታያል..."
            textSize = 15f
            setTextColor(Color.parseColor("#00E676"))
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#182229"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#00A884"))
            }
        }
        mainLayout.addView(liveSubtitleView)

        // Call Control Button Section (IMO Style Bottom Action Bar)
        btnStartCall = Button(this).apply {
            text = "📞 HD ጥሪ ጀምር (Start Live Translated Call)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(30, 25, 30, 25)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00A884"))
                cornerRadius = 30f
            }
        }
        btnStartCall?.setOnClickListener { toggleVoIPCall() }

        val buttonContainer = LinearLayout(this).apply {
            setPadding(10, 20, 10, 10)
            addView(btnStartCall, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        mainLayout.addView(buttonContainer)

        setContentView(mainLayout)
        checkPermissions()
    }

    private fun createContactItem(name: String, languagePair: String, time: String): View {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 25, 20, 25)
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111B21"))
                cornerRadius = 12f
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 15)
            layoutParams = params
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val nameView = TextView(this).apply {
            text = name
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E9EDEF"))
        }

        val subView = TextView(this).apply {
            text = languagePair
            textSize = 13f
            setTextColor(Color.parseColor("#8696A0"))
        }

        textLayout.addView(nameView)
        textLayout.addView(subView)

        val timeView = TextView(this).apply {
            text = time
            textSize = 12f
            setTextColor(Color.parseColor("#8696A0"))
        }

        itemLayout.addView(textLayout)
        itemLayout.addView(timeView)

        return itemLayout
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.MANAGE_OWN_CALLS
        )
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 101)
        }
    }

    private fun toggleVoIPCall() {
        val serviceIntent = Intent(this, CallTranslationService::class.java)
        if (!isCallActive) {
            startService(serviceIntent)
            isCallActive = true
            statusTextView?.text = "🔴 ጥሪ በንቃት ላይ ነው (HD VoIP + Real-Time Translation)"
            statusTextView?.setTextColor(Color.parseColor("#00E676"))
            
            liveSubtitleView?.visibility = View.VISIBLE
            liveSubtitleView?.text = "🎙️ [ENG 🇺🇸]: Hello, how are you?\n🇪🇹 [AMH]: ሰላም፣ እንዴት ነህ?"

            btnStartCall?.text = "🔴 ጥሪውን አቁም (End Call)"
            btnStartCall?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#EA0038"))
                cornerRadius = 30f
            }
            Toast.makeText(this, "የ HD ጥሪ እና የቀጥታ ትርጉሙ ተጀምሯል!", Toast.LENGTH_SHORT).show()
        } else {
            stopService(serviceIntent)
            isCallActive = false
            statusTextView?.text = "● ዝግጁ ነው (Ready for HD Live Translation Call)"
            statusTextView?.setTextColor(Color.parseColor("#8696A0"))
            
            liveSubtitleView?.visibility = View.GONE

            btnStartCall?.text = "📞 HD ጥሪ ጀምር (Start Live Translated Call)"
            btnStartCall?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#00A884"))
                cornerRadius = 30f
            }
        }
    }
}
