package com.bodadroid.client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var rewardedAd: RewardedAd? = null
    private var currentBalance: Long = 0
    private var currentPlan: String = "free"
    private var userApiKey: String = ""
    private var userBloggerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        MobileAds.initialize(this) {}
        
        val planText = findViewById<TextView>(R.id.planText)
        val balanceText = findViewById<TextView>(R.id.balanceText)
        val watchAdBtn = findViewById<Button>(R.id.watchAdBtn)
        val upgradeBtn = findViewById<Button>(R.id.upgradeBtn)
        val settingsBtn = findViewById<Button>(R.id.settingsBtn)
        val generatePostBtn = findViewById<Button>(R.id.generatePostBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        val userId = auth.currentUser?.uid ?: return

        db.collection("Users").document(userId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            currentBalance = snapshot.getLong("remaining_posts") ?: 0
            currentPlan = snapshot.getString("plan_type") ?: "free"
            userApiKey = snapshot.getString("api_key") ?: ""
            userBloggerId = snapshot.getString("blogger_id") ?: ""
            balanceText.text = "رصيد المقالات: $currentBalance"

            if (currentPlan == "pro" || currentPlan == "lifetime") {
                planText.text = "الباقة: 👑 $currentPlan (بدون إعلانات)"
                planText.setTextColor(android.graphics.Color.parseColor("#fbbf24"))
                watchAdBtn.visibility = View.GONE
                upgradeBtn.visibility = View.GONE
            } else {
                planText.text = "الباقة: مجانية (تتضمن إعلانات)"
                watchAdBtn.visibility = View.VISIBLE
                upgradeBtn.visibility = View.VISIBLE
                loadRewardedAd()
            }
        }

        settingsBtn.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        upgradeBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("الترقية للباقة الاحترافية 🚀")
                .setMessage("باقة Pro تمنحك سرعة توليد مضاعفة وتلغي الإعلانات!\nتواصل مع الدعم.")
                .setPositiveButton("حسناً", null).show()
        }

        watchAdBtn.setOnClickListener {
            if (rewardedAd != null) {
                rewardedAd?.show(this) {
                    currentBalance += 1
                    db.collection("Users").document(userId).update("remaining_posts", currentBalance)
                    Toast.makeText(this, "🎉 ربحت مقال! رصيدك الآن: $currentBalance", Toast.LENGTH_LONG).show()
                    loadRewardedAd()
                }
            } else {
                Toast.makeText(this, "جاري تحميل الإعلان.. انتظر ثوانٍ.", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            }
        }

        generatePostBtn.setOnClickListener {
            if (userApiKey.isEmpty() || userBloggerId.isEmpty()) {
                Toast.makeText(this, "⚠️ يرجى ضبط المفاتيح من ⚙️ الإعدادات!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (currentBalance > 0) showCreatePostDialog(userId)
            else Toast.makeText(this, "رصيدك 0! شاهد إعلاناً أو قم بالترقية.", Toast.LENGTH_LONG).show()
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun loadRewardedAd() {
        if (currentPlan != "free") return
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        })
    }

    private fun showCreatePostDialog(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.setCancelable(false)

        val topicInput = dialogView.findViewById<EditText>(R.id.topicInput)
        val nicheInput = dialogView.findViewById<EditText>(R.id.nicheInput)
        val instructionsInput = dialogView.findViewById<EditText>(R.id.instructionsInput)
        val confirmBtn = dialogView.findViewById<Button>(R.id.confirmGenerateBtn)

        confirmBtn.setOnClickListener {
            val topic = topicInput.text.toString().trim()
            val niche = nicheInput.text.toString().trim()
            val instructions = instructionsInput.text.toString().trim()

            if (topic.isEmpty() || niche.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة الفكرة والمجال!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            sendPostRequestToServer(userId, topic, niche, instructions)
        }
        dialog.show()
    }

    private fun sendPostRequestToServer(userId: String, topic: String, niche: String, instructions: String) {
        val generatePostBtn = findViewById<Button>(R.id.generatePostBtn)
        generatePostBtn.isEnabled = false
        generatePostBtn.text = "⏳ جاري التوليد والنشر... يرجى الانتظار"

        val requestMap = hashMapOf(
            "user_id" to userId,
            "api_key" to userApiKey,
            "blogger_id" to userBloggerId,
            "topic" to topic,
            "niche" to niche,
            "instructions" to instructions,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("Requests").add(requestMap).addOnSuccessListener { documentReference ->
            // خصم الرصيد مبدئياً
            currentBalance -= 1
            db.collection("Users").document(userId).update("remaining_posts", currentBalance)
            
            // مراقبة هذا الطلب تحديداً لمعرفة نتيجته (نجاح أم فشل)
            documentReference.addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val status = snapshot.getString("status")
                val errorMsg = snapshot.getString("error")

                if (status == "completed") {
                    generatePostBtn.isEnabled = true
                    generatePostBtn.text = "توليد ونشر مقال جديد"
                    showResultDialog("🎉 نجاح عظيم!", "تم كتابة المقال ونشره بنجاح على مدونتك! اذهب لتفقده الآن.")
                } else if (status == "failed") {
                    generatePostBtn.isEnabled = true
                    generatePostBtn.text = "توليد ونشر مقال جديد"
                    showResultDialog("❌ فشل النشر!", "لم يتم نشر المقال وتم استرجاع رصيدك بأمان.\n\nالسبب:\n$errorMsg")
                }
            }
        }
    }

    private fun showResultDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }
}
