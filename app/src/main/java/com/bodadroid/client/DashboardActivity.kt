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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        MobileAds.initialize(this) {}
        val userId = auth.currentUser?.uid ?: return

        val planText = findViewById<TextView>(R.id.planText)
        val balanceText = findViewById<TextView>(R.id.balanceText)
        val generateBtn = findViewById<Button>(R.id.generatePostBtn)
        val watchAdBtn = findViewById<Button>(R.id.watchAdBtn)
        
        db.collection("Users").document(userId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            currentBalance = snapshot.getLong("remaining_posts") ?: 0
            currentPlan = snapshot.getString("plan_type") ?: "free"
            userApiKey = snapshot.getString("api_key") ?: ""
            balanceText.text = currentBalance.toString()

            if (currentPlan != "free") {
                planText.text = "Plan: PRO 👑"
                planText.setTextColor(android.graphics.Color.parseColor("#C084FC"))
                watchAdBtn.visibility = View.GONE
                findViewById<Button>(R.id.upgradeBtn).visibility = View.GONE
            } else {
                planText.text = "Plan: Free"
                loadRewardedAd()
            }
        }

        findViewById<Button>(R.id.settingsBtn).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<Button>(R.id.logoutBtn).setOnClickListener { auth.signOut(); startActivity(Intent(this, MainActivity::class.java)); finish() }
        
        watchAdBtn.setOnClickListener {
            rewardedAd?.show(this) {
                currentBalance += 1
                db.collection("Users").document(userId).update("remaining_posts", currentBalance)
                Toast.makeText(this, "Credit Added! 🎉", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            } ?: Toast.makeText(this, "Loading ad...", Toast.LENGTH_SHORT).show()
        }

        generateBtn.setOnClickListener {
            if (userApiKey.isEmpty()) {
                Toast.makeText(this, "Save API Key in settings first!", Toast.LENGTH_LONG).show()
            } else if (currentBalance > 0) {
                showCreatePostDialog(userId)
            } else {
                Toast.makeText(this, "Zero Credits! Watch an ad.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadRewardedAd() {
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
        })
    }

    private fun showCreatePostDialog(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.confirmGenerateBtn).setOnClickListener {
            val topic = dialogView.findViewById<EditText>(R.id.topicInput).text.toString()
            val niche = dialogView.findViewById<EditText>(R.id.nicheInput).text.toString()
            val inst = dialogView.findViewById<EditText>(R.id.instructionsInput).text.toString()

            if (topic.isNotEmpty() && niche.isNotEmpty()) {
                dialog.dismiss()
                sendRequest(userId, topic, niche, inst)
            } else {
                Toast.makeText(this, "Fill required fields!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun sendRequest(userId: String, topic: String, niche: String, inst: String) {
        val btn = findViewById<Button>(R.id.generatePostBtn)
        btn.text = "AI IS THINKING..."
        btn.isEnabled = false

        val data = hashMapOf("user_id" to userId, "topic" to topic, "niche" to niche, "instructions" to inst, "status" to "pending", "timestamp" to FieldValue.serverTimestamp())
        db.collection("Requests").add(data).addOnSuccessListener { doc ->
            currentBalance -= 1
            db.collection("Users").document(userId).update("remaining_posts", currentBalance)
            
            doc.addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                val status = snap.getString("status")
                if (status == "completed" || status == "failed") {
                    btn.text = "AI GENERATE POST ✨"
                    btn.isEnabled = true
                    val msg = if (status == "completed") "Published successfully! 🎉" else "Failed to publish. Credit refunded. ❌"
                    AlertDialog.Builder(this).setTitle(status.uppercase()).setMessage(msg).setPositiveButton("OK", null).show()
                }
            }
        }
    }
}
