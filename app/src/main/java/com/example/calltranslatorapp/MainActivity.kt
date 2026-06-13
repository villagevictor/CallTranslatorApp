package com.example.calltranslatorapp

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognitionIntent: Intent
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    // 📖 700+ ከመስመር ውጭ የሁለትዮሽ (Bi-directional) መዝገበ-ቃላት ጥቅል
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        
        // 💼 === [1] 50 የቢዝነስ (Business) ንግግሮች ===
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
        put("ስራ መልቀቅ እፈልጋለሁ", "I want to resign from work.")
        put("ደሞዝ መቼ ይከፈላል", "When will the salary be paid?")
        put("ማስታወቂያ መስራት አለብን", "We need to create an advertisement.")
        put("ይህ ምርት ጥራት አለው", "This product has high quality.")
        put("ዋጋውን መቀነስ እንችላለን", "Can we reduce the price?")
        put("ሽያጩ ቀንሷል", "Sales have decreased.")
        
        put("የስብሰባው አጀንዳ ምንድነው", "What is the meeting agenda?")
        put("አጋር መሆን እንችላለን", "We can be partners.")
        put("የስራ ልምድ አለህ", "Do you have work experience?")
        put("ይህ ህገወጥ ነው", "This is illegal.")
        put("ግብር መክፈል አለብን", "We must pay taxes.")
        put("የባንክ ብድር እፈልጋለሁ", "I need a bank loan.")
        put("የንግድ ፈቃዴ ጠፍቷል", "My business license is lost.")
        put("ይህ ስራ ከባድ ነው", "This job is difficult.")
        put("የስራ ሰዓት አልቋል", "Work hours are over.")
        put("ነገ ስራ አለን", "We have work tomorrow.")
        put("ስብሰባው ተሰርዟል", "The meeting is canceled.")
        put("አዲስ ስምምነት አድርገናል", "We have made a new agreement.")
        put("የኩባንያው ህግ መከበር አለበት", "The company rules must be respected.")
        put("ይህ የእኔ ፊርማ ነው", "This is my signature.")
        put("ፋይናንስ መምሪያው የት ነው", "Where is the finance department?")
        put("ኢንሹራንስ አለህ", "Do you have insurance?")
        put("ባለሀብት መፈለግ አለብን", "We need to find an investor.")
        put("የስራ ማመልከቻ አስገብቻለሁ", "I have submitted a job application.")
        put("ደሞዝ ጭማሪ እፈልጋለሁ", "I want a salary increment.")
        put("ይህ የእኔ የስራ ድርሻ ነው", "This is my job responsibility.")
        put("በጋራ እንስራ", "Let's work together.")

        // 🏥 === [2] 50 የህክምና (Medical) ንግግሮች ===
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
        
        put("የመድሃኒት ማዘዣ (prescription) አለህ", "Do you have a prescription?")
        put("ትውከት እያጋጠመኝ ነው", "I am experiencing vomiting.")
        put("ጉንፋን ይዞኛል", "I have a cold.")
        put("ሳል አለብኝ", "I have a cough.")
        put("የመተንፈስ ችግር አለብኝ", "I have breathing difficulty.")
        put("እግሬ ተሰብሯል", "My leg is broken.")
        put("የደም ምርመራ ማድረግ እፈልጋለሁ", "I want to do a blood test.")
        put("ጥርሴን እያመመኝ ነው", "My tooth is hurting.")
        put("የጥርስ ዶክተር የት ነው", "Where is the dentist?")
        put("ይህ ህመም ከባድ ነው", "This pain is severe.")
        put("መቼ ነው የምድነው", "When will I recover?")
        put("እረፍት ያስፈልግሃል", "You need rest.")
        put("ይህ መድሃኒት ያሸልማል", "This medicine makes you drowsy.")
        put("አለርጂ አለብኝ", "I have an allergy.")
        put("የአይን ሐኪም ማየት እፈልጋለሁ", "I want to see an ophthalmologist.")
        put("ክኒን መዋጥ አልችልም", "I cannot swallow pills.")
        put("ቁስሉን እጠበው", "Wash the wound.")
        put("ፋሻ ያስፈልገዋል", "It needs a bandage.")
        put("የድንገተኛ አደጋ ክፍል የት ነው", "Where is the emergency room?")
        put("ነፍሰ ጡር ነኝ", "I am pregnant.")
        put("ልጄ አሞታል", "My child is sick.")
        put("መውለጃዋ ደርሷል", "Her delivery time has arrived.")
        put("መርፌ መውጋት እፈራለሁ", "I am afraid of getting an injection.")
        put("ይህ ቫይረስ ተላላፊ ነው", "This virus is contagious.")
        put("ክትባት መውሰድ እፈልጋለሁ", "I want to get a vaccine.")
        put("የደም አይነቴ ምንድነው", "What is my blood type?")
        put("ማዞር ማዞር ይለኛል", "I feel dizzy.")
        put("ጀርባዬን እያመመኝ ነው", "My back is hurting.")
        put("የሕክምና ካርድ አለህ", "Do you have a medical card?")
        put("ሆስፒታሉ በጣም ሩቅ ነው", "The hospital is very far.")
        put("ዶክተሩ መቼ ይመጣል", "When will the doctor come?")
        put("በቀጠሮ ነው የመጣሁት", "I came by appointment.")
        put("ክብደቴ ቀንሷል", "My weight has decreased.")
        put("ስኳር ህመም አለብኝ", "I have diabetes.")
        put("ይህ የጎንዮሽ ጉዳት (side effect) አለው", "Does this have a side effect?")
        put("የኤክስሬይ (X-ray) ምርመራ እፈልጋለሁ", "I need an X-ray examination.")
        put("ጤናዬ እየተሻሻለ ነው", "My health is improving.")
        put("የመጀመሪያ እርዳታ መስጠት አለብን", "We must give first aid.")
        put("ጭንቀት ይሰማኛል", "I feel anxious.")

        // ✈ === [3] 50 የጉዞ (Travel) ንግግሮች ===
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
        put("የጉዞ መስመር (route) የትኛው ነው", "Which one is the travel route?")
        put("ከተማዋን መጎብኘት እፈልጋለሁ", "I want to visit the city.")
        put("ታክሲ የት አገኛለሁ", "Where can I find a taxi?")
        put("የድንበር ቁጥጥር የት ነው", "Where is the border control?")
        put("የባህር ጉዞ ደስ ይለኛል", "I like sea travel.")
        put("የጉዞ ዋስትና አለህ", "Do you have travel insurance?")
        put("የአውቶቡስ መቆሚያው የት ነው", "Where is the bus stop?")
        put("መንገዱ ዝግ ነው", "The road is closed.")
        put("ምርጥ ምግብ ቤት የት አለ", "Where is a good restaurant?")
        put("ይህ ታሪካዊ ቦታ ነው", "This is a historical place.")
        put("ፎቶ ማንሳት እችላለሁ", "Can I take a photo?")
        put("የጉዞ ቦርሳዬ ከባድ ነው", "My travel bag is heavy.")
        put("የውጭ ምንዛሬ የት ይለወጣል", "Where is the foreign currency exchanged?")
        put("ዶላር መለወጥ እፈልጋለሁ", "I want to exchange dollars.")
        put("በረራው ተሰርዟል", "The flight is canceled.")
        put("በረራው ዘግይቷል", "The flight is delayed.")
        put("የመቀመጫ ቁጥሬ ስንት ነው", "What is my seat number?")
        put("መስኮት አጠገብ መቀመጥ እፈልጋለሁ", "I want to sit by the window.")
        put("መታወቂያህን አሳየኝ", "Show me your ID.")
        put("እዚህ ሀገር አዲስ ነኝ", "I am new to this country.")
        put("እንግሊዝኛ አትናገርም", "Don't you speak English?")
        put("ይህ የጉምሩክ (customs) ፍተሻ ነው", "This is the customs check.")
        put("የመኖሪያ አድራሻህ ምንድነው", "What is your residential address?")
        put("ጉዞው አድካሚ ነበር", "The journey was tiring.")
        put("ነገ እመለሳለሁ", "I will return tomorrow.")
        put("የባህር ዳርቻ መሄድ እፈልጋለሁ", "I want to go to the beach.")
        put("መኪና መከራየት እፈልጋለሁ", "I want to rent a car.")
        put("የመኪና ኪራይ ዋጋ ስንት ነው", "How much is the car rental price?")
        put("ይህ መንገድ ደህንነቱ የተጠበቀ ነው", "Is this road safe?")
        put("እባክህ እርዳኝ መንገድ ጠፍቶብኛል", "Please help me, I am lost.")
        put("የጉዞ ትዝታዬ ደስ የሚል ነው", "My travel memory is pleasant.")
        put("አስጎብኚ እፈልጋለሁ", "I need a tour guide.")
        put("የመግቢያ ክፍያ አለው", "Does it have an entrance fee?")
        put("ሙዚየሙ ስንት ሰዓት ይከፈታል", "What time does the museum open?")
        put("ይህ ታላቅ ጉዞ ነው", "This is a great journey.")

        // 📝 === [4] 50 አዳዲስ ወሳኝ ቃላት (Vocabulary Words) ===
        put("meeting", "ስብሰባ")
        put("contract", "ውል / ስምምነት")
        put("investment", "ኢንቨስትመንት")
        put("profit", "ትርፍ")
        put("loss", "ኪሳራ")
        put("employee", "ሰራተኛ")
        put("manager", "ስራ አስኪያጅ")
        put("customer", "ደንበኛ")
        put("office", "ቢሮ")
        put("salary", "ደሞዝ")
        put("advertisement", "ማስታወቂያ")
        put("signature", "ፊርማ")
        put("tax", "ግብር / ቀረጥ")
        put("loan", "ብድር")
        put("insurance", "ኢንሹራንስ")
        put("doctor", "ዶክተር / ሐኪም")
        put("hospital", "ሆስፒታል")
        put("pharmacy", "መድሃኒት ቤት / ፋርማሲ")
        put("medicine", "መድሃኒት")
        put("headache", "ራስ ምታት")
        put("fever", "ትኩሳት")
        put("cough", "ሳል")
        put("injury", "ቁስል / አደጋ")
        put("ambulance", "አምቡላንስ")
        put("surgery", "ቀዶ ጥገና")
        put("prescription", "የመድሃኒት ማዘዣ")
        put("allergy", "አለርጂ")
        put("vaccine", "ክትባት")
        put("virus", "ቫይረስ")
        put("passport", "ፓስፖርት")
        put("visa", "ቪዛ")
        put("ticket", "ትኬት")
        put("luggage", "ሻንጣ / ጓዝ")
        put("airport", "አውሮፕላን ማረፊያ")
        put("flight", "በረራ")
        put("hotel", "ሆቴል")
        put("tourist", "ጎብኝ / ቱሪስት")
        put("map", "ካርታ")
        put("taxi", "ታክሲ")
        put("customs", "ጉምሩክ")
        put("museum", "ሙዚየም")
        put("beach", "የባህር ዳርቻ")
        put("guide", "አስጎብኚ / መሪ")
        put("currency", "የውጭ ምንዛሬ")
        put("border", "ድንበር")
        put("emergency", "ድንገተኛ አደጋ")
        put("blood", "ደም")
        put("appointment", "ቀጠሮ")
        put("agenda", "አጀንዳ")

        // ❤ === [5] የፍቅር እና የሮማንቲክ (Romantic) ንግግሮች ጥቅል ===
        put("እወድሃለሁ", "I love you (to a male)")
        put("እወድሻለሁ", "I love you (to a female)")
        put("ናፍቀኸኛል", "I miss you (to a male)")
        put("ናፍቀሽኛል", "I miss you (to a female)")
        put("ውዴ", "My love / My dear")
        put("የኔ ፍቅር", "My love")
        put("የኔ አለም", "My world")
        put("የኔ ቆንጆ", "My beautiful / My handsome")
        put("ልቤ", "My heart")
        put("የልቤ ንጉስ", "King of my heart")
        put("የልቤ ንግስት", "Queen of my heart")
        put("በጣም ነው የምወድህ", "I love you so much (to a male)")
        put("በጣም ነው የምወድሽ", "I love you so much (to a female)")
        put("ያለ አንተ መኖር አልችልም", "I can't live without you (to a male)")
        put("ያለ አንቺ መኖር አልችልም", "I can't live without you (to a female)")
        put("ሁልጊዜ አስብሃለሁ", "I think of you all the time (to a male)")
        put("ሁልጊዜ አስብሻለሁ", "I think of you all the time (to a female)")
        put("ደስታዬ ነህ", "You are my happiness (to a male)")
        put("ደስታዬ ነሽ", "You are my happiness (to a female)")
        put("ፈገግታህ ደስ ይለኛል", "I love your smile (to a male)")
        put("ፈገግታሽ ደስ ይለኛል", "I love your smile (to a female)")
        put("አይኖችህ በጣም ያምራሉ", "Your eyes are so beautiful (to a male)")
        put("አይኖችሽ በጣም ያምራሉ", "Your eyes are so beautiful (to a female)")
        put("የኔ ጌጥ", "My jewel / My precious")
        put("ህይወቴ ነህ", "You are my life (to a male)")
        put("ህይወቴ ነሽ", "You are my life (to a female)")
        put("ካንተ ጋር መሆን እፈልጋለሁ", "I want to be with you (to a male)")
        put("ካንቺ ጋር መሆን እፈልጋለሁ", "I want to be with you (to a female)")
        put("ስላገኘሁሽ እግዚአብሔርን አመሰግናለሁ", "I thank God for finding you (to a female)")
        put("ስላገኘሁህ እግዚአብሔርን አመሰግናለሁ", "I thank God for finding you (to a male)")
        put("ህልሜ ነህ", "You are my dream (to a male)")
        put("ህልሜ ነሽ", "You are my dream (to a female)")
        put("ማታ በህልሜ አይቼሃለሁ", "I saw you in my dream last night (to a male)")
        put("ማታ በህልሜ አይቼሻለሁ", "I saw you in my dream last night (to a female)")
        put("የኔ ውድ", "My precious")
        put("የኔ ጣፋጭ", "My sweet")
        put("አቅፈኝ", "Hug me (to a male)")
        put("አቅፊኝ", "Hug me (to a female)")
        put("መሳም እፈልጋለሁ", "I want to kiss you")
        put("ሁሌም የኔ ነህ", "You are always mine (to a male)")
        put("ሁሌም የኔ ነሽ", "You are always mine (to a female)")
        put("ልብህን ስጠኝ", "Give me your heart (to a male)")
        put("ልብሽን ስጠኝ", "Give me your heart (to a female)")
        put("ካንተ መለየት አልፈልግም", "I don't want to separate from you (to a male)")
        put("ካንቺ መለየት አልፈልግም", "I don't want to separate from you (to a female)")
        put("ማርሰኛል", "You are my honey (expression)")
        put("የኔ ማር", "My honey")
        put("ብርሃኔ ነህ", "You are my light (to a male)")
        put("ብርሃኔ ነሽ", "You are my light (to a female)")
        put("አንተ ብቻ ነህ ለኔ", "You are the only one for me (to a male)")
        put("አንቺ ብቻ ነሽ ለኔ", "You are the only one for me (to a female)")
        put("ፍቅርህ ማረከኝ", "Your love captured me (to a male)")
        put("ፍቅርሽ ማረከኝ", "Your love captured me (to a female)")
        put("ድምጽህ ደስ ይለኛል", "I love your voice (to a male)")
        put("ድምጽሽ ደስ ይለኛል", "I love your voice (to a female)")
        put("ትዳር እንመስርት", "Let's build a marriage")
        put("ላግባሽ", "Marry me (to a female)")
        put("ላግባህ", "Marry me (to a male)")
        put("እውነተኛ ፍቅሬ ነህ", "You are my true love (to a male)")
        put("እውነተኛ ፍቅሬ ነሽ", "You are my true love (to a female)")
        put("የኔ ፀሐይ", "My sun")
        put("የኔ ጨረቃ", "My moon")
        put("ለዘላለም እወድሃለሁ", "I will love you forever (to a male)")
        put("ለዘላለም እወድሻለሁ", "I will love you forever (to a female)")
        put("አታላይ", "You are charming (romantic way)")
        put("喂ቸኛዬ ነህ", "You are my only one (to a male)")
        put("ብቸኛዬ ነሽ", "You are my only one (to a female)")
        put("አብረን እንሁን", "Let's be together")
        put("ልቤን ሰርቀኸዋል", "You have stolen my heart (to a male)")
        put("ልቤን ሰርቀሽዋል", "You have stolen my heart (to a female)")
        put("ለኔ ልዩ ነህ", "You are special to me (to a male)")
        put("ለኔ ልዩ ነሽ", "You are special to me (to a female)")
        put("የልብ ጓደኛዬ", "My soulmate")
        put("እመካብሃለሁ", "I rely on you / proud of you (to a male)")
        put("እመካብሻለሁ", "I rely on you / proud of you (to a female)")
        put("የኔ ምርጥ", "My best")
        put("አንተ የኔ ስጦታ ነህ", "You are my gift (to a male)")
        put("አንቺ የኔ ስጦታ ነሽ", "You are my gift (to a female)")
        put("ቃላት ያጥሩኛል", "Words fail me (to describe my love)")
        put("ካንተ ጋር ሰላም አለኝ", "I have peace with you (to a male)")
        put("ካንቺ ጋር ሰላም አለኝ", "I have peace with you (to a female)")
        put("ባንተ እኮራለሁ", "I am proud of you (to a male)")
        put("ባንቺ እኮራለሁ", "I am proud of you (to a female)")
        put("מלካም ማታ የኔ ፍቅር", "Good night my love")
        put("መልካም ቀን የኔ ውድ", "Have a nice day my dear")
        put("እንክብካቤህ ደስ ይለኛል", "I love your care (to a male)")
        put("እንክብካቤሽ ደስ ይለኛል", "I love your care (to a female)")
        put("አታዝኚ የኔ ፍቅር", "Don't be sad my love (to a female)")
        put("አታዝን የኔ ፍቅር", "Don't be sad my love (to a male)")
        put("የኔ ታማኝ", "My faithful")
        put("አምንሃለሁ", "I trust you (to a male)")
        put("አምንሻለሁ", "I trust you (to a female)")
        put("የኔ ትንፋሽ", "My breath")
        put("የኔ መጠጊያ", "My refuge")
        put("ሁሌም በአእምሮዬ ነህ", "You are always in my mind (to a male)")
        put("ሁሌም በአእምሮዬ ነሽ", "You are always in my mind (to a female)")
        put("ፍቅር ያሸንፋል", "Love wins")
        put("የኔ ሁነኛ", "My perfect match")
        put("እወድሃለሁ የኔ አለም", "I love you my world (to a male)")
        put("እወድሻለሁ የኔ አለም", "I love you my world (to a female)")

        // የድሮ አጠቃላይ ንግግሮች ማትሪክስ
        put("hello", "ሰላም")
        put("how are you", "እንደምን ነህ? / እንደምን ነሽ?")
        put("i am fine", "ደህና ነኝ")
        put("what is new", "ምን አዲስ ነገር አለ?")
        put("nothing much", "ምንም አዲስ ነገር የለም")
        put("what is your name", "ስምህ ማን ነው?")
        put("my name is", "ስሜ ... ነው")
        put("nice to meet you", "ስላገኘሁህ ደስ ብሎኛል")
        put("good morning", "እንደምን አደርክ / አደርሽ")
        put("good afternoon", "እንደምን ዋልክ / ዋልሽ")
        put("good evening", "እንደምን አመሸህ / አመሸሽ")
        put("good night", "ደህና እደር / እደሪ")
        put("thank you", "አመሰግናለሁ")
        put("thank you very much", "በጣም አመሰግናለሁ")
        put("you are welcome", "ምንም አይደለም")
        put("where are you from", "ከየት ሀገር ነህ?")
        put("i am from ethiopia", "እኔ ከኢትዮጵያ ነኝ")
        put("where do you live", "የት ነው የምትኖረው?")
        put("i am a student", "እኔ ተማሪ ነኝ")
        put("can you help me", "ልትረዳኝ ትችላለህ?")
        put("excuse me", "ይቅርታ")
        put("sorry", "አዝናለሁ / አፉ በለኝ")
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
        put("don't worry", "አትጨነቅ / አትጨነቂ")
        put("everything will be fine", "ሁሉም ነገር ጥሩ ይሆናል")
        put("i am tired", "ደክሞኛል")
        put("i am sick", "አሞኛል")
        put("call me later", "በኋላ ደውልልኝ")
        put("i will call you", "እኔ እደውልልሃለሁ")
        put("let's go", "እንሂድ")
        put("wait a minute", "አንድ ደቂቃ ቆይ")
        put("see you later", "በኋላ እንገናኝ")
        put("goodbye", "ደህና ሁን / ሁኚ")
        put("how was your day", "ውሎህ እንዴት ነበር?")
        put("it was great", "በጣም ጥሩ ነበር")
        put("i am lost", "ጠፍቶብኛል / መንገድ ጠፋኝ")
        put("can you show me the way", "መንገዱን ልታሳየኝ ትችላለህ?")
        put("i need a taxi", "ታክሲ እፈልጋለሁ")
        put("stop here please", "እባክህ እዚህ አቁም")
        put("go straight", "ቀጥታ ሂድ")
        put("turn right", "ወደ ቀኝ ታጠፍ")
        put("turn left", "ወደ ግራ ታጠፍ")
        put("i am busy now", "አሁን ስራ በዝቶብኛል")
        put("call me tomorrow", "ነገ ደውልልኝ")
        put("are you ready", "ተዘጋጅተሃል?")
        put("yes i am ready", "አዎ ተዘጋጅቻለሁ")
        put("i am waiting for you", "እየጠበቅኩህ ነው")
        put("where is the airport", "አውሮፕላን ማረፊያው የት ነው?")
        put("i have a question", "ጥያቄ አለኝ")
        put("can you repeat that", "ልትደግምልኝ ትችላለህ?")
        put("write it down please", "እባክህ ጻፍልኝ")
        put("i forgot", "ረሳሁት")
        put("i remember", "ትዝ ይለኛል")
        put("congratulations", "እንኳን ደስ አለህ / አለሽ")
        put("happy birthday", "መልካም ልደት")
        put("good luck", "መልካም እድል")
        put("have a nice trip", "መልካም ጉዞ")
        put("i am happy", "ደስተኛ ነኝ")
        put("i am sad", "አዝኛለሁ")
        put("don't copy me", "አትቅዳኝ")
        put("listen to me", "ስማኝ / ስሚኝ")
        put("look at this", "ይህንን እይ")
        put("what are you watching", "ምን እያየህ ነው?")
        put("i am watching a movie", "ፊልም እያየሁ ነው")
        put("do you like sports", "ስፖርት ትወዳለህ?")
        put("i play football", "እግር ኳስ እጫወታለሁ")
        put("see you tomorrow", "ነገ እንገናኝ")

        // የትምህርት ቤት ንግግሮች ጥቅል
        put("good morning class", "እንደምን አደራችሁ የክፍሉ ተማሪዎች")
        put("please open your books", "እባካችሁ መጽሐፋችሁን ክፈቱ")
        put("who is absent today", "ዛሬ የቀረ ማን ነው?")
        put("listen carefully please", "እባካችሁ በጥንቃቄ ስሙ")
        put("do you have any questions", "ጥያቄ ያላችሁ አለ?")
        put("raise your hand please", "እባክህ እጅህን አውጣ")
        put("sit down please", "እባካችሁ ተቀመጡ")
        put("stand up please", "እባክህ ቁም / እባክሽ ቁሚ")
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
        put("spell this word please", "እባክህ የዚህን ቃል ፊደላት ቁጠር")
        put("speak louder please", "እባክህ ድምጽህን ከፍ አድርግ")
        put("who wants to read", "ማን ማንበብ ይፈልጋል?")
        put("it is your turn", "የአንተ ተራ ነው")
        put("time is up", "ሰዓት አልቋል")
        put("have a nice weekend", "መልካም ቅዳሜና እሁድ ይሁንላችሁ")
        put("when is the exam", "ፈተናው መቼ ነው?")
        put("the exam is next week", "ፈተናው በሚቀጥለው ሳምንት ነው")
        put("i got a good grade", "ጥሩ ውጤት አገኘሁ")
        put("don't cheat in the exam", "በፈተና ላይ አትኮርጅ")
        put("close your books now", "አሁን መጽሐፋችሁን ዝጉ")
        put("who knows the answer", "መልሱን ማን ያውቃል?")
        put("may i come in", "ልግባ?")
        put("may i go out", "ልውጣ?")
        put("you are late today", "ዛሬ ዘግይተሃል")
        put("sorry for being late", "በመዘግየቴ ይቅርታ")
        put("work in pairs", "ሁለት ሁለት ሆናችሁ ስሩ")
        put("help each other", "እርስ በርሳችሁ ተረዳዱ")
        put("put your pens down", "ስክሪፕቶአችሁን አስቀምጡ")
        put("are you finished", "ጨረሳችሁ?")
        put("not yet teacher", "ገና ነን መምህር")
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
        
        Toast.makeText(this, "🚀 700+ ከመስመር ውጭ የላቀ ትርጉም በንቃት ላይ!", Toast.LENGTH_SHORT).show()
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
            .setContentText("ቢዝነስ፣ ህክምና፣ ጉዞ እና ሮማንቲክ ፓይፕላይን ገባሪ ነው...")
            .setSmallIcon(if (logoId != 0) logoId else android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        manager.notify(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayTextView = TextView(this).apply {
            text = "🎙️ Call Translator: Ready for Multi-Domain Translation..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xEE112200.toInt()) 
            setPadding(40, 30, 40, 30)
            gravity = Gravity.CENTER
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 150
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
                            overlayTextView?.text = "Detected: ${matches[0]}"
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
            if (matchedKey.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())) {
                overlayTextView?.text = "AMH 🇪🇹: $rawInput\nENG 🇺🇸: $translatedText"
            } else {
                overlayTextView?.text = "ENG 🇺🇸: $rawInput\nAMH 🇪🇹: $translatedText"
            }
        } else {
            overlayTextView?.text = "Input: $rawInput\n⚠️ [ትርጉም አልተገኘም]"
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
