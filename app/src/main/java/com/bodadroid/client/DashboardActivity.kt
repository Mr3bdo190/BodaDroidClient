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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentBalance: Long = 0
    private var currentPlan: String = "free"
    private var userApiKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val planText = findViewById<TextView>(R.id.planText)
        val balanceText = findViewById<TextView>(R.id.balanceText)
        val generatePostBtn = findViewById<Button>(R.id.generatePostBtn)
        val settingsBtn = findViewById<Button>(R.id.settingsBtn)
        val watchAdBtn = findViewById<Button>(R.id.watchAdBtn)
        val upgradeBtn = findViewById<Button>(R.id.upgradeBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        db.collection("Users").document(userId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            currentBalance = snapshot.getLong("remaining_posts") ?: 0
            currentPlan = snapshot.getString("plan_type") ?: "free"
            userApiKey = snapshot.getString("api_key") ?: ""
            balanceText.text = currentBalance.toString()

            if (currentPlan == "pro" || currentPlan == "lifetime") {
                planText.text = "Plan: PRO (No Ads)"
                planText.setTextColor(android.graphics.Color.parseColor("#C084FC"))
                watchAdBtn.visibility = View.GONE
                upgradeBtn.visibility = View.GONE
            } else {
                planText.text = "Plan: Free (Contains Ads)"
                watchAdBtn.visibility = View.VISIBLE
                upgradeBtn.visibility = View.VISIBLE
            }
        }

        settingsBtn.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        watchAdBtn.setOnClickListener { Toast.makeText(this, "Loading Ad...", Toast.LENGTH_SHORT).show() }
        upgradeBtn.setOnClickListener { Toast.makeText(this, "Upgrade feature coming soon!", Toast.LENGTH_SHORT).show() }

        generatePostBtn.setOnClickListener {
            if (userApiKey.isEmpty()) {
                Toast.makeText(this, "Please save your Gemini API Key in Settings first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (currentBalance > 0) {
                showCreatePostDialog(userId)
            } else {
                Toast.makeText(this, "Zero Credits! Watch an ad to earn more.", Toast.LENGTH_LONG).show()
            }
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showCreatePostDialog(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val topicInput = dialogView.findViewById<EditText>(R.id.topicInput)
        val nicheInput = dialogView.findViewById<EditText>(R.id.nicheInput)
        val instructionsInput = dialogView.findViewById<EditText>(R.id.instructionsInput)
        val confirmBtn = dialogView.findViewById<Button>(R.id.confirmGenerateBtn)

        confirmBtn.setOnClickListener {
            val topic = topicInput.text.toString().trim()
            val niche = nicheInput.text.toString().trim()
            val instructions = instructionsInput.text.toString().trim()

            if (topic.isEmpty() || niche.isEmpty()) {
                Toast.makeText(this, "Topic and Niche are required!", Toast.LENGTH_SHORT).show()
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
        generatePostBtn.text = "AI IS THINKING..."

        val requestMap = hashMapOf(
            "user_id" to userId,
            "topic" to topic,
            "niche" to niche,
            "instructions" to instructions,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("Requests").add(requestMap).addOnSuccessListener { docRef ->
            currentBalance -= 1
            db.collection("Users").document(userId).update("remaining_posts", currentBalance)
            
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val status = snapshot.getString("status")
                val errorMsg = snapshot.getString("error")

                if (status == "completed") {
                    generatePostBtn.isEnabled = true
                    generatePostBtn.text = "AI GENERATE POST"
                    showResultDialog("SUCCESS! ✨", "Your article has been generated and published to your Blogger.")
                } else if (status == "failed") {
                    generatePostBtn.isEnabled = true
                    generatePostBtn.text = "AI GENERATE POST"
                    showResultDialog("FAILED ❌", "Failed to publish. Your credit was refunded.\nReason: $errorMsg")
                }
            }
        }
    }

    private fun showResultDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
