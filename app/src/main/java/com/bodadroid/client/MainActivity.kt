package com.bodadroid.client

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passInput = findViewById<EditText>(R.id.passInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                loginUser(email, pass)
            } else {
                Toast.makeText(this, "يرجى إدخال البيانات", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "مرحباً بك في Boda Droid!", Toast.LENGTH_SHORT).show()
                    // سيتم إضافة كود الانتقال للوحة التحكم والتحقق من الباقة هنا في الخطوة القادمة
                } else {
                    Toast.makeText(this, "فشل تسجيل الدخول: بيانات خاطئة أو لا يوجد اشتراك", Toast.LENGTH_LONG).show()
                }
            }
    }
}
