package com.example.calltranslatorapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calltranslatorapp.data.ContactFetcher
import com.example.calltranslatorapp.service.AudioCallService

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 102
    private lateinit var contactsContainer: LinearLayout
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40, 60, 40, 40)
        }

        val headerTitle = TextView(this).apply {
            text = "imo Translator Pro"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, 0, 0, 10)
        }

        statusTextView = TextView(this).apply {
            text = "● ዝግጁ ነው (Ready for HD Real-Time Call)"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 30)
        }

        val sectionTitle = TextView(this).apply {
            text = "የቅርብ ጊዜ ጥሪዎች / Contacts List"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 20)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        contactsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(contactsContainer)

        val startCallButton = Button(this).apply {
            text = "📞 HD ጥሪ ጀምር (Start Live Translated Call)"
            setBackgroundColor(Color.parseColor("#059669"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                startTranslationService()
            }
        }

        mainLayout.addView(headerTitle)
        mainLayout.addView(statusTextView)
        mainLayout.addView(sectionTitle)
        mainLayout.addView(scrollView)
        mainLayout.addView(startCallButton)

        setContentView(mainLayout)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            loadDeviceContactsUI()
        }
    }

    private fun loadDeviceContactsUI() {
        val fetcher = ContactFetcher(this)
        val contacts = fetcher.getDeviceContacts()

        contactsContainer.removeAllViews()

        if (contacts.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "ምንም የContacts ዝርዝር አልተገኘም"
                setTextColor(Color.GRAY)
                setPadding(0, 20, 0, 20)
            }
            contactsContainer.addView(emptyText)
            return
        }

        contacts.take(10).forEach { contact ->
            val cardView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(30, 30, 30, 30)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 20)
                layoutParams = params
            }

            val contactName = TextView(this).apply {
                text = contact.name
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }

            val contactPhone = TextView(this).apply {
                text = "🇺🇸 እንግሊዝኛ ⇆ 🇪🇹 አማርኛ | " + contact.phoneNumber
                textSize = 13f
                setTextColor(Color.parseColor("#38BDF8"))
            }

            cardView.addView(contactName)
            cardView.addView(contactPhone)

            cardView.setOnClickListener {
                statusTextView.text = "🔴 ጥሪ በዝግጅት ላይ ነው: " + contact.name
                startTranslationService()
            }

            contactsContainer.addView(cardView)
        }
    }

    private fun startTranslationService() {
        val serviceIntent = Intent(this, AudioCallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        statusTextView.text = "🔴 ጥሪ በሂደት ላይ ነው (Live Translation Active)"
        statusTextView.setTextColor(Color.RED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            loadDeviceContactsUI()
        }
    }
}
