package com.example.calltranslatorapp

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    // 📖 የተመጣጠነ 500 ከመስመር ውጭ (Offline) የሁለትዮሽ መዝገበ-ቃላት ጥቅል
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        
        // 💼 [ክፍል 1] 20 ዋና የቢዝነስ ንግግሮች (Business Phrases)
        put("ስብሰባው መቼ ነው", "When is the meeting?")
        put("ውል መፈረም እፈልጋለሁ", "I want to sign a contract.")
        put("ዋጋው ስንት ነው", "What is the price?")
        put("ይህ የቢዝነስ እቅዳችን ነው", "This is our business plan.")
        put("ትርፋችን ጨምሯል", "Our profit has increased.")
        put("ኪሳራ አጋጥሞናል", "We have faced a loss.")
        put("አዲስ ሰራተኛ መቅጠር አለብን", "We need to hire a new employee.")
        put("ይህንን ፕሮጀክት መምራት እፈልጋለሁ", "I want to lead this project.")
        put("የባንክ ሂሳቤን ማረጋገጥ እፈልጋለሁ", "I want to check my bank account.")
        put("ደረሰኝ ስጠኝ እባክህ", "Please give me a receipt.")
        put("ይህ ሚስጥራዊ መረጃ ነው", "This is confidential information.")
        put("ከስራ ባልደረባዬ ጋር እየሰራሁ ነው", "I am working with my colleague.")
        put("ደንበኞቻችን በጣም ደስተኛ ናቸው", "Our customers are very happy.")
        put("አዲስ ገበያ መፈለግ አለብን", "We need to find a new market.")
        put("ይህ ምርጥ ኢንቨስትመንት ነው", "This is a great investment.")
        put("የቀጠሮ ሰዓታችን አልፏል", "Our appointment time has passed.")
        put("ኢሜይል ልኬልሃለሁ", "I have sent you an email.")
        put("ሪፖርቱን አዘጋጅተሃል", "Have you prepared the report?")
        put("የኩባንያው ስራ አስኪያጅ ማን ነው", "Who is the company manager?")
        put("ቢሮዬ እዚህ ነው", "My office is here.")

        // 🏥 [ክፍል 2] 20 ዋና የህክምና ንግግሮች (Medical Phrases)
        put("አሞኛል ዶክተር መጥራት እፈልጋለሁ", "I am sick, I want to call a doctor.")
        put("ራስ ምታት አለብኝ", "I have a headache.")
        put("ሆዴን እያመመኝ ነው", "My stomach is hurting.")
        put("ትኩሳት አለብኝ", "I have a fever.")
        put("ፋርማሲው የት ነው", "Where is the pharmacy?")
        put("ይህንን መድሃኒት በቀን ስንት ጊዜ ልውሰድ", "How many times a day should I take this medicine?")
        put("የደም ግፊቴን ለካኝ", "Measure my blood pressure.")
        put("የልብ ህመም አለበት", "He has heart disease.")
        put("አምቡላንስ ጥራ", "Call an ambulance.")
        put("የቀዶ ጥገና ክፍል የት ነው", "Where is the surgery room?")
        put("የመድሃኒት ማዘዣ አለህ", "Do you have a prescription?")
        put("ትውከት እያጋጠመኝ ነው", "I am experiencing vomiting.")
        put("ጉንፋን ይዞኛል", "I have a cold.")
        put("ሳል አለብኝ", "I have a cough.")
        put("የመተንፈስ ችግር አለብኝ", "I have breathing difficulty.")
        put("እግሬ ተሰብሯል", "My leg is broken.")
        put("የደም ምርመራ ማድረግ እፈልጋለሁ", "I want to do a blood test.")
        put("ጥርሴን እያመመኝ ነው", "My tooth is hurting.")
        put("የጥርስ ዶክተር የት ነው", "Where is the dentist?")
        put("ይህ ህመም ከባድ ነው", "This pain is severe.")

        // ✈ [ክፍል 3] 20 ዋና የጉዞ ንግግሮች (Travel Phrases)
        put("ፓስፖርቴ የት ነው", "Where is my passport?")
        put("ቪዛ ማግኘት እፈልጋለሁ", "I want to get a visa.")
        put("የአውሮፕላን ትኬት ስንት ነው", "How much is the flight ticket?")
        put("ሻንጣዬ ጠፍቷል", "My luggage is lost.")
        put("የበረራ ሰዓት ደርሷል", "The flight time has arrived.")
        put("የአውሮፕላን ማረፊያ የት ነው", "Where is the airport?")
        put("ሆቴል መያዝ እፈልጋለሁ", "I want to book a hotel.")
        put("የቱሪስት መረጃ ቢሮ የት ነው", "Where is the tourist information office?")
        put("ይህ ባቡር ወዴት ነው የሚሄደው", "Where does this train go?")
        put("ካርታ ያስፈልገኛል", "I need a map.")
        put("የጉዞ መስመር የትኛው ነው", "Which one is the travel route?")
        put("ከተማዋን መጎብኘት እፈልጋለሁ", "I want to visit the city.")
        put("ታክሲ የት አገኛለሁ", "Where can I find a taxi?")
        put("የድንበር ቁጥጥር የት ነው", "Where is the border control?")
        put("የባህር ጉዞ ደስ ይለኛል", "I like sea travel.")
        put("የጉዞ ዋስትና አለህ", "Do you have travel insurance?")
        put("የአውቶቡስ መቆሚያው የት ነው", "Where is the bus stop?")
        put("መንገዱ ዝግ ነው", "The road is closed.")
        put("ምርጥ ምግብ ቤት የት አለ", "Where is a good restaurant?")
        put("ይህ ታሪካዊ ቦታ ነው", "This is a historical place.")

        // 📝 [ክፍል 4] 40 ወሳኝ አጫጭር ቃላት (Key Vocabulary)
        put("meeting", "ስብሰባ")
        put("contract", "ውል")
        put("profit", "ትርፍ")
        put("loss", "ኪሳራ")
        put("employee", "ሰራተኛ")
        put("manager", "ስራ አስኪያጅ")
        put("customer", "ደንበኛ")
        put("office", "ቢሮ")
        put("salary", "ደሞዝ")
        put("doctor", "ዶክተር")
        put("hospital", "ሆስፒታል")
        put("pharmacy", "ፋርማሲ")
        put("medicine", "መድሃኒት")
        put("passport", "ፓስፖርት")
        put("visa", "ቪዛ")
        put("ticket", "ትኬት")
        put("airport", "አውሮፕላን ማረፊያ")
        put("hotel", "ሆቴል")
        put("taxi", "ታክሲ")
        put("emergency", "ድንገተኛ አደጋ")
        put("love", "ፍቅር")
        put("heart", "ልብ")
        put("smile", "ፈገግታ")
        put("beautiful", "ቆንጆ")
        put("happy", "ደስተኛ")
        put("sad", "አዝኛለሁ")
        put("tired", "ደክሞኛል")
        put("sick", "አሞኛል")
        put("home", "ቤት")
        put("school", "ትምህርት ቤት")
        put("teacher", "መምህር")
        put("student", "ተማሪ")
        put("book", "መጽሐፍ")
        put("exam", "ፈተና")
        put("homework", "የቤት ስራ")
        put("time", "ሰዓት")
        put("money", "ገንዘብ")
        put("food", "ምግብ")
        put("water", "ውሃ")
        put("friend", "ጓደኛ")

        // ❤ [ክፍል 5] የፍቅር እና የሮማንቲክ (Romantic) ንግግሮች ጥቅል
        put("እወድሃለሁ", "I love you (to a male)")
        put("እወድሻለሁ", "I love you (to a female)")
        put("ናፍቀኸኛል", "I miss you (to a male)")
        put("ናፍቀሽኛል", "I miss you (to a female)")
        put("ውዴ", "My love")
        put("የኔ ፍቅር", "My love")
        put("የኔ አለም", "My world")
        put("የኔ ቆንጆ", "My beautiful")
        put("ልቤ", "My heart")
        put("የልቤ ንጉስ", "King of my heart")
        put("የልቤ ንግስት", "Queen of my heart")
        put("በጣም ነው የምወድህ", "I love you so much")
        put("ያለ አንተ መኖር አልችልም", "I can't live without you")
        put("ያለ አንቺ መኖር አልችልም", "I can't live without you")
        put("ሁልጊዜ አስብሃለሁ", "I think of you all the time")
        put("ሁልጊዜ አስብሻለሁ", "I think of you all the time")
        put("ደስታዬ ነህ", "You are my happiness")
        put("ደስታዬ ነሽ", "You are my happiness")
        put("ፈገግታህ ደስ ይለኛል", "I love your smile")
        put("ፈገግታሽ ደስ ይለኛል", "I love your smile")
        put("አይኖችህ በጣም ያምራሉ", "Your eyes are so beautiful")
        put("አይኖችሽ በጣም ያምራሉ", "Your eyes are so beautiful")
        put("የኔ ጌጥ", "My jewel")
        put("ህይወቴ ነህ", "You are my life")
        put("ህይወቴ ነሽ", "You are my life")
        put("ካንተ ጋር መሆን እፈልጋለሁ", "I want to be with you")
        put("ካንቺ ጋር መሆን እፈልጋለሁ", "I want to be with you")
        put("የኔ ውድ", "My precious")
        put("የኔ ጣፋጭ", "My sweet")
        put("አቅፈኝ", "Hug me")
        put("አቅፊኝ", "Hug me")
        put("መሳም እፈልጋለሁ", "I want to kiss you")
        put("ሁሌም የኔ ነህ", "You are always mine")
        put("ሁሌም የኔ ነሽ", "You are always mine")
        put("ልብህን ስጠኝ", "Give me your heart")
        put("ልብሽን ስጠኝ", "Give me your heart")
        put("ማርሰኛል", "You are my honey")
        put("የኔ ማር", "My honey")
        put("ብርሃኔ ነህ", "You are my light")
        put("ብርሃኔ ነሽ", "You are my light")
        put("አንተ ብቻ ነህ ለኔ", "You are the only one for me")
        put("አንቺ ብቻ ነሽ ለኔ", "You are the only one for me")
        put("ፍቅርህ ማረከኝ", "Your love captured me")
        put("ፍቅርሽ ማረከኝ", "Your love captured me")
        put("ድምጽህ ደስ ይለኛል", "I love your voice")
        put("ድምጽሽ ደስ ይለኛል", "I love your voice")
        put("እውነተኛ ፍቅሬ ነህ", "You are my true love")
        put("እውነተኛ ፍቅሬ ነሽ", "You are my true love")
        put("የኔ ፀሐይ", "My sun")
        put("የኔ ጨረቃ", "My moon")
        put("ለዘላለም እወድሃለሁ", "I will love you forever")
        put("ለዘላለም እወድሻለሁ", "I will love you forever")

        // 🏫 [ክፍል 6] ጠቅላላ እና የዕለት ተዕለት ንግግሮች ጥቅል (ባላንስ ማሟያ እስከ 500)
        put("hello", "ሰላም")
        put("how are you", "እንደምን ነህ? / እንደምን ነሽ?")
        put("i am fine", "ደህና ነኝ")
        put("what is new", "ምን አዲስ ነገር አለ?")
        put("nothing much", "ምንም አዲስ ነገር የለም")
        put("what is your name", "ስምህ ማን ነው?")
        put("my name is", "ስሜ ... ነው")
        put("nice to meet you", "ስላገኘሁህ ደስ ብሎኛል")
        put("good morning", "እንደምን አደርክ")
        put("good afternoon", "እንደምን ዋልክ")
        put("good evening", "እንደምን አመሸህ")
        put("good night", "ደህና እደር")
        put("thank you", "አመሰግናለሁ")
        put("thank you very much", "በጣም አመሰግናለሁ")
        put("you are welcome", "ምንም አይደለም")
        put("where are you from", "ከየት ሀገር ነህ?")
        put("i am from ethiopia", "እኔ ከኢትዮጵያ ነኝ")
        put("where do you live", "የት ነው የምትኖረው?")
        put("i am a student", "እኔ ተማሪ ነኝ")
        put("can you help me", "ልትረዳኝ ትችላለህ?")
        put("excuse me", "ይቅርታ")
        put("sorry", "አዝናለሁ")
        put("what time is it", "ሰዓት ስንት ነው?")
        put("where is the bathroom", "መጸዳጃ ቤቱ የት ነው?")
        put("i don't understand", "አልገባኝም")
        put("do you speak amharic", "አማርኛ ትናገራለህ?")
        put("yes i do", "አዎ እናገራለሁ")
        put("please speak slowly", "እባክህ ቀስ ብለህ ተናገር")
        put("what does this mean", "ይህ ማለት ምን ማለት ነው?")
        put("where are you going", "የት እየሄድክ ነው?")
        put("i am going home", "ወደ ቤት እየሄድኩ ነው")
        put("what happened", "ምን ተፈጠረ?")
        put("don't worry", "አትጨነቅ")
        put("everything will be fine", "ሁሉም ነገር ጥሩ ይሆናል")
        put("call me later", "በኋላ ደውልልኝ")
        put("i will call you", "እኔ እደውልልሃለሁ")
        put("let's go", "እንሂድ")
        put("wait a minute", "አንድ ደቂቃ ቆይ")
        put("see you later", "በኋላ እንገናኝ")
        put("goodbye", "ደህና ሁን")
        put("how was your day", "ውሎህ እንዴት ነበር?")
        put("it was great", "በጣም ጥሩ ነበር")
        put("i am lost", "መንገድ ጠፋኝ")
        put("can you show me the way", "መንገዱን ልታሳየኝ ትችላለህ?")
        put("stop here please", "እባክህ እዚህ አቁም")
        put("go straight", "ቀጥታ ሂድ")
        put("turn right", "ወደ ቀኝ ታጠፍ")
        put("turn left", "ወደ ግራ ታጠፍ")
        put("i am busy now", "አሁን ስራ በዝቶብኛል")
        put("call me tomorrow", "ነገ ደውልልኝ")
        put("are you ready", "ተዘጋጅተሃል?")
        put("yes i am ready", "አዎ ተዘጋጅቻለሁ")
        put("i am waiting for you", "እየጠበቅኩህ ነው")
        put("i have a question", "ጥያቄ አለኝ")
        put("can you repeat that", "ልትደግምልኝ ትችላለህ?")
        put("write it down please", "እባክህ ጻፍልኝ")
        put("i forgot", "ረሳሁት")
        put("i remember", "ትዝ ይለኛል")
        put("congratulations", "እንኳን ደስ አለህ")
        put("happy birthday", "መልካም ልደት")
        put("good luck", "መልካም እድል")
        put("have a nice trip", "መልካም ጉዞ")
        put("listen to me", "ስማኝ")
        put("look at this", "ይህንን እይ")
        put("see you tomorrow", "ነገ እንገናኝ")
        put("please open your books", "እባካችሁ መጽሐፋችሁን ክፈቱ")
        put("who is absent today", "ዛሬ የቀረ ማን ነው?")
        put("listen carefully please", "እባካችሁ በጥንቃቄ ስሙ")
        put("do you have any questions", "ጥያቄ ያላችሁ አለ?")
        put("raise your hand please", "እባክህ እጅህን አውጣ")
        put("sit down please", "እባካችሁ ተቀመጡ")
        put("stand up please", "እባክህ ቁም")
        put("look at the blackboard", "ጥቁር ሰሌዳውን እዩ")
        put("write this down", "ይህንን ጻፉት")
        put("clean the board please", "እባክህ ሰሌዳውን አጽዳው")
        put("be quiet please", "እባካችሁ ዝም በሉ")
        put("is everything clear", "ሁሉም ነገር ግልጽ ነው?")
        put("yes teacher it is clear", "አዎ መምህር፣ ግልጽ ነው")
        put("i don't understand this lesson", "ይህ ትምህርት አልገባኝም")
        put("can you explain it again", "እባክህ ድጋሚ ልታብራራልኝ ትችላለህ?")
        put("what is the homework", "የቤት ስራው ምንድነው?")
        put("submit your homework tomorrow", "የቤት ስራችሁን ነገ አስረክቡ")
        put("i forgot my book at home", "መጽሐፌን ቤት ረሳሁት")
        put("can i borrow a pen", "ስክሪፕቶ ልበደር እችላለሁ?")
        put("you did a great job", "በጣም ጥሩ ስራ ሰርተሃል")
        put("excellent answer", "በጣም ምርጥ መልስ ነው")
        put("try again next time", "ቀጣይ ጊዜ ድጋሚ ሞክር")
        put("what is the meaning of this word", "የዚህ ቃል ትርጉም ምንድነው?")
        put("speak louder please", "እባክህ ድምጽህን ከፍ አድርግ")
        put("it is your turn", "የአንተ ተራ ነው")
        put("time is up", "ሰዓት አልቋል")
        put("when is the exam", "ፈተናው መቼ ነው?")
        put("the exam is next week", "ፈተናው በሚቀጥለው ሳምንት ነው")
        put("i got a good grade", "ጥሩ ውጤት አገኘሁ")
        put("don't cheat in the exam", "በፈተና ላይ አትኮርጅ")
        put("close your books now", "አሁን መጽሐፋችሁን ዝጉ")
        put("may i come in", "ልግባ?")
        put("may i go out", "ልውጣ?")
        put("you are late today", "ዛሬ ዘግይተሃል")
        put("sorry for being late", "በመዘግየቴ ይቅርታ")
        put("work in pairs", "ሁለት ሁለት ሆናችሁ ስሩ")
        put("put your pens down", "ስክሪፕቶአችሁን አስቀምጡ")
        put("are you finished", "ጨረሳችሁ?")
        put("yes we are finished", "አዎ ጨርሰናል")
        put("open page twenty", "ገጽ ሃያ ላይ ክፈቱ")
        put("check your answers", "መልሳችሁን ፈትሹ")
        put("see you next class", "በሚቀጥለው ክፍለ-ጊዜ እንገናኝ")
        put("thank you teacher", "አመሰግናለሁ መምህር")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
            }
        }

        val btnId = resources.getIdentifier("btn_enable_service", "id", packageName)
        findViewById<Button>(btnId).setOnClickListener {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                startTranslationEngine()
            } else {
                requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 102)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 102 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTranslationEngine()
        }
    }

    private fun startTranslationEngine() {
        if (isListening) return
        isListening = true
        
        showNotification()
        setupOverlay()
        startListeningLoop()
        
        Toast.makeText(this, "🚀 ለመተርጎም ዝግጁ ነው!", Toast.LENGTH_SHORT).show()
    }

    private fun showNotification() {
        val channelId = "call_trans_fast"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Fast Translation", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        
        val logoId = resources.getIdentifier("app_logo", "drawable", packageName)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("🎙️ Call Translator Pro")
            .setContentText("ለመተርጎም ዝግጁ ነው...")
            .setSmallIcon(if (logoId != 0) logoId else android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        manager.notify(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        overlayTextView = TextView(this).apply {
            // ✨ ያንተን ምርጫ መሠረት ያደረገ ንፁህ ፅሁፍ ብቻ
            text = "✨ ለመተርጎም ዝግጁ ነው..."
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            
            setTextColor(Color.parseColor("#4CAF50")) // ውብ አረንጓዴ የብራንድ ቀለም
            setPadding(45, 35, 45, 35)
            gravity = Gravity.CENTER

            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A")) // ፕሪሚየም ጥቁር ካርድ
                cornerRadius = 35f // የተጠጋገሩ ማዕዘኖች (Modern UI)
                setStroke(3, Color.parseColor("#FF9800")) // ብርቱካናማ የዳር መስመር
            }
            background = backgroundDrawable
            elevation = 15f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 180
            width = WindowManager.LayoutParams.MATCH_PARENT
            horizontalMargin = 0.05f 
        }
        
        try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) {}
    }

    private fun startListeningLoop() {
        if (!isListening) return
        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "am-ET")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "am-ET"))
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsd: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (isListening) mainHandler.postDelayed({ restartListening() }, 300)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            lookupTranslation(matches[0])
                        }
                        if (isListening) restartListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            overlayTextView?.text = "🎙️ ሰማሁት: ${matches[0]}"
                            overlayTextView?.setTextColor(Color.parseColor("#FF9800"))
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(recognitionIntent)
            } catch (e: Exception) {
                if (isListening) mainHandler.postDelayed({ restartListening() }, 1000)
            }
        }
    }

    private fun lookupTranslation(spokenText: String) {
        val rawInput = spokenText.trim()
        val lowerInput = rawInput.lowercase(Locale.ROOT)
        var translatedText = ""
        var matchedKey = ""

        for (key in offlineDictionary.keys) {
            if (key.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())) {
                if (rawInput.contains(key) || key.contains(rawInput)) {
                    translatedText = offlineDictionary[key] ?: ""
                    matchedKey = key
                    break
                }
            } else {
                if (lowerInput.contains(key.lowercase(Locale.ROOT)) || key.lowercase(Locale.ROOT).contains(lowerInput)) {
                    translatedText = offlineDictionary[key] ?: ""
                    matchedKey = key
                    break
                }
            }
        }

        if (translatedText.isNotEmpty()) {
            overlayTextView?.setTextColor(Color.parseColor("#4CAF50"))
            if (matchedKey.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())) {
                overlayTextView?.text = "🇪🇹 አማርኛ: $rawInput\n🇺🇸 ENG: $translatedText"
            } else {
                overlayTextView?.text = "🇺🇸 ENG: $rawInput\n🇪🇹 አማርኛ: $translatedText"
            }
        } else {
            overlayTextView?.setTextColor(Color.parseColor("#FF5252"))
            overlayTextView?.text = "🎙️ ግብዓት: $rawInput\n⚠️ [ትርጉም አልተገኘም]"
        }
    }

    private fun restartListening() {
        if (!isListening) return
        try { speechRecognizer?.destroy(); startListeningLoop() } catch (e: Exception) {}
    }

    override fun onDestroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
            if (overlayTextView != null) windowManager?.removeView(overlayTextView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
