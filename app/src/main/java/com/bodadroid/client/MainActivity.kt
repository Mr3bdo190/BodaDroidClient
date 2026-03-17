package com.bodadroid.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // إذا كان المستخدم مسجل دخول مسبقاً، انقله فوراً للوحة التحكم
        if (auth.currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passInput = findViewById<EditText>(R.id.passInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            if (email.isNotEmpty() && pass.isNotEmpty()) loginUser(email, pass)
        }

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            if (email.isNotEmpty() && pass.isNotEmpty()) registerNewUser(email, pass)
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "تم الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "بيانات خاطئة!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerNewUser(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                val userMap = hashMapOf(
                    "email" to email, "blogger_id" to "", "plan_type" to "free",
                    "remaining_posts" to 0, "is_active" to true, "join_date" to Date().toString()
                )
                db.collection("Users").document(userId!!).set(userMap).addOnSuccessListener {
                    Toast.makeText(this, "تم إنشاء الحساب! شاهد الإعلانات لجمع رصيد.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(this, "فشل إنشاء الحساب!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
