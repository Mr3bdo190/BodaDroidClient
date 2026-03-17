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
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var rewardedAd: RewardedAd? = null
    private var currentBalance: Long = 0
    private var currentPlan: String = "free"
    private var adLoadErrorMsg: String = ""

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

        // مراقبة بيانات العميل لحظياً من Firestore
        db.collection("Users").document(userId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            
            currentBalance = snapshot.getLong("remaining_posts") ?: 0
            currentPlan = snapshot.getString("plan_type") ?: "free"
            
            balanceText.text = "رصيد المقالات: $currentBalance"

            // الذكاء التجاري: إخفاء الإعلانات وزر الترقية إذا كان المشترك Pro
            if (currentPlan == "pro" || currentPlan == "lifetime") {
                planText.text = "الباقة: 👑 $currentPlan (بدون إعلانات)"
                planText.setTextColor(android.graphics.Color.parseColor("#fbbf24"))
                watchAdBtn.visibility = View.GONE
                upgradeBtn.visibility = View.GONE
            } else {
                planText.text = "الباقة: مجانية (تتضمن إعلانات)"
                watchAdBtn.visibility = View.VISIBLE
                upgradeBtn.visibility = View.VISIBLE
                loadRewardedAd() // تحميل الإعلان للمجاني فقط
            }
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        upgradeBtn.setOnClickListener {
            // رسالة الترقية (يمكنك لاحقاً جلب الأسعار من قاعدة البيانات)
            AlertDialog.Builder(this)
                .setTitle("الترقية للباقة الاحترافية 🚀")
                .setMessage("باقة Pro تمنحك سرعة توليد مضاعفة، وتلغي الإعلانات تماماً!\n\nالسعر: 10$ شهرياً\nتواصل مع الدعم الفني للترقية.")
                .setPositiveButton("حسناً", null)
                .show()
        }

        watchAdBtn.setOnClickListener {
            if (rewardedAd != null) {
                rewardedAd?.show(this) { rewardItem ->
                    currentBalance += 1
                    db.collection("Users").document(userId).update("remaining_posts", currentBalance)
                    Toast.makeText(this, "🎉 ربحت مقال! رصيدك الآن: $currentBalance", Toast.LENGTH_LONG).show()
                    loadRewardedAd()
                }
            } else {
                // إظهار سبب عدم ظهور الإعلان للمستخدم
                if (adLoadErrorMsg.isNotEmpty()) {
                    Toast.makeText(this, "الإعلانات غير متوفرة حالياً (كود: $adLoadErrorMsg)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "جاري تحميل الإعلان.. انتظر ثوانٍ واضغط مجدداً.", Toast.LENGTH_SHORT).show()
                }
                loadRewardedAd()
            }
        }

        generatePostBtn.setOnClickListener {
            if (currentBalance > 0) {
                Toast.makeText(this, "سيتم تنفيذ طلب التوليد قريباً!", Toast.LENGTH_SHORT).show()
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
        if (currentPlan != "free") return // لا تحمل إعلانات للمشتركين المدفوعين
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                adLoadErrorMsg = adError.code.toString() // حفظ كود الخطأ لعرضه
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                adLoadErrorMsg = ""
            }
        })
    }
}
