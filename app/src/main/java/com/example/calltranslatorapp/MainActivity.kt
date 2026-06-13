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

    // 📖 500+ ከመስመር ውጭ የሁለትዮሽ (Bi-directional) መዝገበ-ቃላት ጥቅል
    private val offlineDictionary = LinkedHashMap<String, String>().apply {
        
        // ❤ === [1] 100 የፍቅር እና የሮማንቲክ (Romantic) ንግግሮች (Amharic ⇄ English) ===
        // የአማርኛ ቁልፎች መጀመሪያ ይቀመጣሉ (ለፈጣን ማዛመድ)
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
        put("ብቸኛዬ ነህ", "You are my only one (to a male)")
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
        put("መልካም ማታ የኔ ፍቅር", "Good night my love")
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

        // የእንግሊዝኛ ተገላቢጦሽ ለሮማንቲክ
        put("i love you", "እወድሻለሁ / እወድሃለሁ")
        put("i miss you", "ናፍቀሽኛል / ናፍቀኸኛል")
        put("my love", "የኔ ፍቅር")
        put("my heart", "ልቤ / የኔ ልብ")
        put("marry me", "ላግባሽ / ላግባህ")

        // === [2] የድሮ ንግግሮች ማትሪክስ (400 Phrases - English & Amharic) ===
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

        // የገበያ ንግግሮች
        put("where is the market", "ገበያው የት ነው?")
        put("how much is this", "ዋጋው ስንት ነው?")
        put("it is too expensive", "በጣም ውድ ነው")
        put("is there a discount", "ቅናሽ አለ?")
        put("what is the final price", "መጨረሻው ስንት ነው?")
        put("i want to buy this", "ይህንን መግዛት እፈልጋለሁ")
        put("where can i pay", "የት ነው የምከፍለው?")
        put("cash or card", "በጥሬ ገንዘብ ወይስ በካርድ?")
        put("i have cash", "ጥሬ ገንዘብ አለኝ")
        put("keep the change", "መልሱ ይቅርብህ")
        put("where is the supermarket", "ሱፐርማርኬቱ የት ነው?")
        put("i am just looking", "ለማየት ብቻ ነው")
        put("do you have a smaller size", "ከዚህ ያነሰ መጠን አለህ?")
        put("do you have a bigger size", "ከዚህ በትልቅ መጠን አለህ?")
        put("can i try it on", "ልልካው እችላለሁ?")
        put("where is the changing room", "ልብስ መቀየሪያ ክፍሉ የት ነው?")
        put("it fits perfectly", "ልክ ልኬ ነው")
        put("do you have other colors", "ሌላ ቀለም አለህ?")
        put("i want the blue one", "ሰማያዊውን እፈልጋለሁ")
        put("i want the black one", "ጥቁሩን እፈልጋለሁ")
        put("is this on sale", "ይህ ቅናሽ ተደርጎለታል?")
        put("it is very cheap", "በጣም ርካሽ ነው")
        put("do you sell clothes", "ልብስ ትሸጣላችሁ?")
        put("where are the shoes", "ጫማዎቹ የት ናቸው?")
        put("i need a shopping bag", "የገበያ ፌስታል እፈልጋለሁ")
        put("is it fresh", "ትኩስ ነው?")
        put("give me one kilo", "አንድ ኪሎ ስጠኝ")
        put("give me two kilos", "ሁለት ኪሎ ስጠኝ")
        put("where are the fruits", "ፍራፍሬዎቹ የት ናቸው?")
        put("i want to buy onions", "ሽንኩርት መግዛት እፈልጋለሁ")
        put("do you have tomatoes", "ቲማቲም አለህ?")
        put("this is broken", "ይህ ተበላሽቷል / ተሰብሯል")
        put("i want to return this", "ይህንን መመለስ እፈልጋለሁ")
        put("can i get a receipt", "ደረሰኝ ማግኘት እችላለሁ?")
        put("where is the bakery", "ዳቦ ቤቱ የት ነው?")
        put("i want fresh bread", "ትኩስ ዳቦ እፈልጋለሁ")
        put("do you have eggs", "እንቁላል አለህ?")
        put("where is the milk", "ወተቱ የት ነው?")
        put("is this open", "ይህ ክፍት ነው?")
        put("when do you close", "መቼ ነው የምትዘጉት?")
        put("i will come back later", "በኋላ እመለሳለሁ")
        put("do you accept credit cards", "ክሬዲት ካርድ ትቀበላላችሁ?")
        put("the price is good", "ዋጋው ጥሩ ነው")
        put("where is the pharmacy", "መድሃኒት ቤቱ የት ነው?")
        put("i need medicine", "መድሃኒት እፈልጋለሁ")
        put("do you have soap", "ሳሙና አለህ?")
        put("i want sugar", "ስኳር እፈልጋለሁ")
        put("where is the salt", "ጨው የት ነው?")
        put("this is high quality", "ጥራቱ በጣም ከፍተኛ ነው")
        put("thank you for the discount", "ለቅናሹ አመሰግናለሁ")

        // የምግብ እና የቤት ንግግሮች
        put("what do you want to eat", "ምን መብላት ትፈልጋለህ?")
        put("i want coffee", "ቡና እፈልጋለሁ")
        put("give me water please", "እባክህ ውሃ ስጠኝ")
        put("the food is delicious", "ምግቡ በጣም ጣፋጭ ነው")
        put("where is the hotel", "ሆቴሉ የት ነው?")
        put("where is the restaurant", "ምግብ ቤቱ የት ነው?")
        put("i need a room", "ክፍል እፈልጋለሁ")
        put("i am hungry", "ርቦኛል")
        put("i am thirsty", "ጠምቶኛል")
        put("i want breakfast", "ቁርስ እፈልጋለሁ")
        put("i want lunch", "ምሳ እፈልጋለሁ")
        put("i want dinner", "እራት እፈልጋለሁ")
        put("what is the special today", "ዛሬ ልዩ ምግብ ምንድነው?")
        put("give me the menu please", "እባክህ የምግብ ዝርዝሩን ስጠኝ")
        put("i want meat", "ስጋ እፈልጋለሁ")
        put("i am a vegetarian", "እኔ ስጋ አልበላም")
        put("i want chicken", "የዶሮ ስጋ እፈልጋለሁ")
        put("do you have fish", "ዓሳ አለህ?")
        put("i want traditional food", "የባህል ምግብ እፈልጋለሁ")
        put("give me injera", "እንጀራ ስጠኝ")
        put("i want doro wat", "ዶሮ ወጥ እፈልጋለሁ")
        put("is the food spicy", "ምግቡ ያቃጥላል?")
        put("please bring the bill", "እባክህ ሂሳቡን አምጣው")
        put("welcome to our home", "ወደ ቤታችን እንኳን ደህና መጡ")
        put("please sit down", "እባክህ ተቀመጥ")
        put("make yourself at home", "እንደ ቤትህ እይው")
        put("where is the kitchen", "ወጥ ቤት የት ነው?")
        put("i am cooking dinner", "እራት እየሰራሁ ነው")
        put("can you wash the dishes", "እቃዎቹን ማጠብ ትችላለህ?")
        put("clean the room please", "እባክህ ክፍሉን አጽዳው")
        put("where is my phone", "ስልኬ የት ነው?")
        put("turn on the light", "መብራቱን አብራው")
        put("turn off the light", "መብራቱን አጥፋው")
        put("open the window", "መስኮቱን ክፈተው")
        put("close the door", "በሩን ዝጋው")
        put("i am going to sleep", "ልተኛ ነው")
        put("wake me up early", "ጠዋት ማልደህ ቀስቅሰኝ")
        put("did you sleep well", "ደህና አደርክ?")
        put("i want to take a shower", "መታጠብ እፈልጋለሁ")
        put("where is the towel", "ፎጣው የት ነው?")
        put("we have guests today", "ዛሬ እንግዶች አሉን")
        put("prepare the tea", "ሻይ አዘጋጅ")
        put("i want more food", "ተጨማሪ ምግብ እፈልጋለሁ")
        put("i am full", "ጠግቤያለሁ")
        put("the water is cold", "ውሃው ቀዝቃዛ ነው")
        put("the water is hot", "ውሃው ትኩስ ነው")
        put("where is the key", "ቁልፉ የት ነው?")
        put("i lost my keys", "ቁልፎቼ ጠፉብኝ")
        put("lock the door", "በሩን ቁልፈው")
        put("i love my family", "ቤተሰቦቼን እወዳለሁ")

        // የትራንስፖርት ንግግሮች
        put("where is the bus station", "የባስ ተራው የት ነው?")
        put("where is the train station", "የባቡር ጣቢያው የት ነው?")
        put("i want to go to the city center", "ወደ ከተማው መሀል መሄድ እፈልጋለሁ")
        put("how much is the ticket", "ትኬቱ ስንት ነው?")
        put("i want to buy a ticket", "ትኬት መግዛት እፈልጋለሁ")
        put("when does the bus leave", "አውቶቡሱ መቼ ነው የሚነሳው?")
        put("when does the train arrive", "ባቡሩ መቼ ነው የሚደርሰው?")
        put("is this seat free", "ይህ ወንበር ክፍት ነው?")
        put("this is my seat", "ይህ የእኔ ወንበር ነው")
        put("where does this bus go", "ይህ አውቶቡስ ወዴት ነው የሚሄደው?")
        put("does this train stop at", "ይህ ባቡር ... ይቆማል?")
        put("how long does it take", "ምን ያህል ጊዜ ይፈጃል?")
        put("it takes one hour", "አንድ ሰዓት ይፈጃል")
        put("is it far away", "ሩቅ ነው?")
        put("no it is near", "አይ፣ ቅርብ ነው")
        put("please tell me where to get off", "እባክህ የት መውረድ እንዳለብኝ ንገረኝ")
        put("i want to get off here", "እዚህ መውረድ እፈልጋለሁ")
        put("you missed your stop", "የመውረጃ ቦታህ አምልጦሃል")
        put("where can i find a gas station", "የነዳጅ ማደያ የት አገኛለሁ?")
        put("my car is broken", "መኪናዬ ተበላሽታለች")
        put("i need a mechanic", "መካኒክ እፈልጋለሁ")
        put("where is the ticket office", "የትኬት መቁረጫው የት ነው?")
        put("can i sit here", "እዚህ መቀመጥ እችላለሁ?")
        put("the bus is late", "አውቶቡሱ ዘግይቷል")
        put("is there a traffic jam", "የትራፊክ መጨናነቅ አለ?")
        put("drive slowly please", "እባክህ ቀስ ብለህ እዳው")
        put("drive faster please", "እባክህ ፈጠን አድርገው")
        put("where is the parking lot", "መኪና ማቆሚያው የት ነው?")
        put("i have a lot of luggage", "ብዙ ሻንጣ አለኝ")
        put("put it in the trunk", "በጀርባው ውስጥ አስቀምጠው")
        put("fasten your seatbelt", "ቀበቶህን እሰር")
        put("show me your license", "ፈቃድህን አሳየኝ")
        put("i am a tourist", "እኔ ጎብኝ ነኝ")
        put("where is the nearest hotel", "በጣም ቅርቡ ሆቴል የት ነው?")
        put("can you order a taxi for me", "ታክሲ ልታዝልኝ ትችላለህ?")
        put("how much to the airport", "እስከ አውሮፕላን ማረፊያ ስንት ነው?")
        put("keep moving", "ቀጥል ሂድ")
        put("the road is blocked", "መንገዱ ተዘግቷል")
        put("is there another way", "ሌላ መንገድ አለ?")
        put("follow that car", "ያቺን መኪና ተከተላት")
        put("where is the entrance", "መግቢያው የት ነው?")
        put("where is the exit", "መውጫው የት ነው?")
        put("i am waiting for the bus", "አውቶቡስ እየጠበቅኩ ነው")
        put("the train is fast", "ባቡሩ ፈጣን ነው")
        put("i missed the bus", "አውቶቡሱ አመለጠኝ")
        put("is it safe to walk here", "እዚህ በእግር መሄድ ሰላም ነው?")
        put("where is the police station", "ፖሊስ ጣቢያው የት ነው?")
        put("look at the map", "ካርታውን እይ")
        put("have a safe journey", "መልካም ጉዞ ይሁንልህ")
        put("we have arrived", "ደርሰናል")

        // ቃላት
        put("house", "ቤት")
        put("car", "መኪና")
        put("money", "ገንዘብ")
        put("water", "ውሃ")
        put("food", "ምግብ")
        put("man", "ወንድ")
        put("woman", "ሴት")
        put("child", "ልጅ")
        put("family", "ቤተሰብ")
        put("friend", "ጓደኛ")
        put("book", "መጽሐፍ")
        put("school", "ትምህርት ቤት")
        put("teacher", "አስተማሪ")
        put("doctor", "ዶክተር")
        put("hospital", "ሆስፒታል")
        put("city", "ከተማ")
        put("country", "ሀገር")
        put("road", "መንገድ")
        put("time", "ሰዓት / ጊዜ")
        put("day", "ቀን")
        put("night", "ማታ / ሌሊት")
        put("today", "ዛሬ")
        put("tomorrow", "ነገ")
        put("yesterday", "ትላንት")
        put("work", "ስራ")
        put("life", "ህይወት")
        put("love", "ፍቅር")
        put("peace", "ሰላም")
        put("health", "ጤና")
        put("sun", "ፀሐይ")
        put("moon", "ጨረቃ")
        put("sky", "ሰማይ")
        put("rain", "ዝናብ")
        put("wind", "ንፋስ")
        put("fire", "እሳት")
        put("earth", "መሬት")
        put("sea", "ባህር")
        put("river", "ወንዝ")
        put("fish", "ዓሳ")
        put("bird", "ወፍ")
        put("animal", "እንስሳ")
        put("dog", "ውሻ")
        put("cat", "ድመት")
        put("tree", "ዛፍ")
        put("flower", "አበባ")
        put("fruit", "ፍራፍሬ")
        put("bread", "ዳቦ")
        put("milk", "ወተት")
        put("tea", "ሻይ")
        put("coffee", "ቡና")
        put("meat", "ስጋ")
        put("salt", "ጨው")
        put("sugar", "ስኳር")
        put("clothing", "ልብስ")
        put("shoes", "ጫማ")
        put("shirt", "ሸሚዝ")
        put("phone", "ስልክ")
        put("computer", "ኮምፒውተር")
        put("watch", "ሰዓት")
        put("key", "ቁልፍ")
        put("bag", "ፌስታል / ቦርሳ")
        put("bed", "አልጋ")
        put("chair", "ወንበር")
        put("table", "ጠረጴዛ")
        put("door", "በር")
        put("window", "መስኮት")
        put("light", "መብራት")
        put("paper", "ወረቀት")
        put("pen", "ስክሪፕቶ")
        put("eye", "አይን")
        put("ear", "ጆሮ")
        put("hand", "እጅ")
        put("foot", "እግር")
        put("head", "ራስ / ጭንቅላት")
        put("heart", "ልብ")
        put("blood", "ደም")
        put("voice", "ድምጽ")
        put("word", "ቃል")
        put("name", "ስም")
        put("big", "ትልቅ")
        put("small", "ትንሽ")
        put("good", "ጥሩ / መልካም")
        put("bad", "መጥፎ")
        put("hot", "ትኩስ")
        put("cold", "ቀዝቃዛ")
        put("new", "አዲስ")
        put("old", "አሮጌ")
        put("young", "ወጣት")
        put("happy", "ደስተኛ")
        put("sad", "አዘንተኛ")
        put("fast", "ፈጣን")
        put("slow", "ቀስተኛ")
        put("beautiful", "ቆንጆ")
        put("heavy", "ከባድ")
        put("lightweight", "ቀላል")
        put("clean", "ንጹህ")
        put("dirty", "ቆሻሻ")
        put("rich", "ሀብታም")
        put("poor", "ደሀ")

        // የትምህርት ንግግሮች
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
        
        // የአማርኛ ቀጥታ ማዛመጃዎች (For quick reverse lookup)
        put("ሰላም", "Hello")
        put("እንደምን ነህ", "How are you?")
        put("ደህና ነኝ", "I am fine")
        put("አመሰግናለሁ", "Thank you")
        put("ጥያቄ አለኝ", "I have a question")
        put("አልገባኝም", "I don't understand")
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
        
        Toast.makeText(this, "🚀 500+ የሮማንቲክ እና የሁለትዮሽ ትርጉም በንቃት ላይ!", Toast.LENGTH_SHORT).show()
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
            .setContentText("አማርኛ ⇄ እንግሊዝኛ ሙሉ የሁለትዮሽ ፓይፕላይን ገባሪ ነው...")
            .setSmallIcon(if (logoId != 0) logoId else android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        manager.notify(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayTextView = TextView(this).apply {
            text = "🎙️ Call Translator: Listening For Amharic & English..."
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
                    
                    // ⚡ ቋንቋን የመለየት መፍትሔ (The Absolute Fix for Amharic Speech Recognition)
                    // በአንድሮይድ ላይ አማርኛ በደንብ እንዲሰማ ዋናውን ቋንቋ "am-ET" እናደርገዋለን፣ ተጨማሪውን ደግሞ "en-US"
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
                            // በቅጽበት በስክሪኑ ላይ የሚነገረውን ቋንቋ የመፃፍ ተግባር[span_1](start_span)[span_1](end_span)
                            overlayTextView?.text = "የሚሰማው (Detected): ${matches[0]}"
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

        // የሕብረቁምፊ ማዛመጃ ማሻሻያ (Advanced String Matching Matrix)
        for (key in offlineDictionary.keys) {
            // የአማርኛ ቁምፊ ከሆነ ቀጥታ ማዛመድ (ካለማሻሻያ)፣ የእንግሊዝኛ ከሆነ በ lowercase ማዛመድ
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

        // ውጤቱን በቪዥዋል ቅርጽ ስክሪኑ ላይ ማሳየት[span_2](start_span)[span_2](end_span)
        if (translatedText.isNotEmpty()) {
            if (matchedKey.matches("^[\\u1200-\\u137F\\s,?.!]+$".toRegex())) {
                // አማርኛ ሰምቶ ወደ እንግሊዝኛ ሲተረጉም
                overlayTextView?.text = "AMH 🇪🇹: $rawInput\nENG 🇺🇸: $translatedText"
            } else {
                // እንግሊዝኛ ሰምቶ ወደ አማርኛ ሲተረጉም
                overlayTextView?.text = "ENG 🇺🇸: $rawInput\nAMH 🇪🇹: $translatedText"
            }
        } else {
            overlayTextView?.text = "Detected Input: $rawInput\n⚠️ [ትርጉም አልተገኘም]"
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
