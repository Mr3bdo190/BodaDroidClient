package com.bodadroid.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        upgradeBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("الترقية للباقة الاحترافية 🚀")
                .setMessage("باقة Pro تمنحك سرعة توليد مضاعفة، وتلغي الإعلانات تماماً!\n\nتواصل مع إدارة Boda Droid للترقية.")
                .setPositiveButton("حسناً", null)
                .show()
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
                Toast.makeText(this, "جاري تحميل الإعلان.. انتظر ثوانٍ واضغط مجدداً.", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            }
        }

        // الزر السحري (إرسال الطلب للسيرفر)
        generatePostBtn.setOnClickListener {
            if (userApiKey.isEmpty() || userBloggerId.isEmpty()) {
                Toast.makeText(this, "⚠️ يرجى ضبط إعدادات المفاتيح أولاً من ⚙️ الإعدادات!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (currentBalance > 0) {
                generatePostBtn.isEnabled = false
                generatePostBtn.text = "⏳ جاري إرسال الطلب..."
                
                // إنشاء الأوردر في قاعدة البيانات
                val requestMap = hashMapOf(
                    "user_id" to userId,
                    "api_key" to userApiKey,
                    "blogger_id" to userBloggerId,
                    "status" to "pending", // الحالة: قيد الانتظار
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                db.collection("Requests").add(requestMap).addOnSuccessListener {
                    // خصم الرصيد بعد نجاح الإرسال
                    currentBalance -= 1
                    db.collection("Users").document(userId).update("remaining_posts", currentBalance)
                    
                    Toast.makeText(this, "✅ تم إرسال الطلب للسيرفر! سيتم النشر قريباً.", Toast.LENGTH_LONG).show()
                    generatePostBtn.isEnabled = true
                    generatePostBtn.text = "توليد ونشر مقال جديد"
                }
            } else {
                Toast.makeText(this, "رصيدك 0! شاهد إعلاناً أو قم بالترقية.", Toast.LENGTH_LONG).show()
            }
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
}
