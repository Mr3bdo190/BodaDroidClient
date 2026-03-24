package com.bodadroid.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 9001
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var bloggerStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val connectGoogleBtn = findViewById<Button>(R.id.connectGoogleBtn)
        bloggerStatusText = findViewById<TextView>(R.id.bloggerStatusText)
        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val saveBtn = findViewById<Button>(R.id.saveSettingsBtn)

        // جلب الإعدادات الحالية من Firebase
        db.collection("Users").document(userId).get().addOnSuccessListener { doc ->
            if (doc != null) {
                apiKeyInput.setText(doc.getString("api_key") ?: "")
                val bloggerToken = doc.getString("blogger_token")
                if (!bloggerToken.isNullOrEmpty()) {
                    bloggerStatusText.text = "Status: Connected ✅"
                    bloggerStatusText.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                }
            }
        }

        // إعدادات زر تسجيل الدخول بجوجل
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(getString(R.string.default_web_client_id)) 
            .requestScopes(Scope("https://www.googleapis.com/auth/blogger")) 
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        connectGoogleBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        saveBtn.setOnClickListener {
            val apiK = apiKeyInput.text.toString().trim()
            if (apiK.isEmpty()) {
                Toast.makeText(this, "Please insert Gemini API Key!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("Users").document(userId).update("api_key", apiK).addOnSuccessListener {
                Toast.makeText(this, "Configuration Saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // هنا كان الخطأ وتم تصحيحه
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) 
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account?.serverAuthCode 
                
                if (authCode != null) {
                    val userId = auth.currentUser?.uid ?: return
                    db.collection("Users").document(userId).update("blogger_token", authCode)
                    
                    bloggerStatusText.text = "Status: Connected ✅"
                    bloggerStatusText.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                    Toast.makeText(this, "Blogger Connected Successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Failed to connect: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
