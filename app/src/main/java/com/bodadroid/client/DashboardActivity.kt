package com.bodadroid.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // تهيئة إعلانات جوجل
        MobileAds.initialize(this) {}
        loadRewardedAd()

        val balanceText = findViewById<TextView>(R.id.balanceText)
        val watchAdBtn = findViewById<Button>(R.id.watchAdBtn)
        val generatePostBtn = findViewById<Button>(R.id.generatePostBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        val userId = auth.currentUser?.uid

        if (userId != null) {
            // الاستماع المباشر للرصيد من قاعدة البيانات
            db.collection("Users").document(userId).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                currentBalance = snapshot.getLong("remaining_posts") ?: 0
                balanceText.text = "رصيد المقالات: $currentBalance"
            }
        }

        // زر مشاهدة الإعلان
        watchAdBtn.setOnClickListener {
            if (rewardedAd != null) {
                rewardedAd?.show(this) { rewardItem ->
                    // العميل شاهد الإعلان بالكامل! نضيف له مقال في قاعدة البيانات
                    currentBalance += 1
                    db.collection("Users").document(userId!!).update("remaining_posts", currentBalance)
                    Toast.makeText(this, "مبروك! حصلت على مقال مجاني.", Toast.LENGTH_LONG).show()
                    loadRewardedAd() // تحميل إعلان جديد للمرة القادمة
                }
            } else {
                Toast.makeText(this, "جاري تحميل الإعلان.. حاول مجدداً بعد ثوانٍ.", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            }
        }

        // زر توليد المقال (مغلق مؤقتاً حتى نربط السكربت)
        generatePostBtn.setOnClickListener {
            if (currentBalance > 0) {
                Toast.makeText(this, "هنا سيتم تشغيل سكربت التوليد عبر السيرفر", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "رصيدك غير كافٍ! شاهد إعلاناً لتربح رصيداً.", Toast.LENGTH_LONG).show()
            }
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // دالة تحميل الإعلان بمكافأة (Test Ad Unit ID)
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        })
    }
}
