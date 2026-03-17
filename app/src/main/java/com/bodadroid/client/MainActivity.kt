package com.bodadroid.client

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

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passInput = findViewById<EditText>(R.id.passInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        // زر تسجيل الدخول
        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                loginUser(email, pass)
            } else {
                Toast.makeText(this, "يرجى إدخال البيانات", Toast.LENGTH_SHORT).show()
            }
        }

        // زر إنشاء الحساب
        registerBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if (pass.length < 6) {
                    Toast.makeText(this, "كلمة المرور يجب أن تكون 6 أحرف على الأقل", Toast.LENGTH_SHORT).show()
                } else {
                    registerNewUser(email, pass)
                }
            } else {
                Toast.makeText(this, "يرجى كتابة إيميل وباسورد لإنشاء الحساب", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "تم الدخول! جاري التحقق من الباقة...", Toast.LENGTH_SHORT).show()
                    // سيتم توجيهه للوحة التحكم في الخطوات القادمة
                } else {
                    Toast.makeText(this, "بيانات خاطئة أو الحساب غير موجود", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerNewUser(email: String, pass: String) {
        Toast.makeText(this, "جاري إنشاء الحساب...", Toast.LENGTH_SHORT).show()
        
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    
                    // إنشاء ملف العميل في قاعدة البيانات برصيد 0
                    val userMap = hashMapOf(
                        "email" to email,
                        "blogger_id" to "",
                        "plan_type" to "free",
                        "remaining_posts" to 0,
                        "is_active" to false,
                        "join_date" to Date().toString()
                    )

                    db.collection("Users").document(userId!!).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "🎉 تم إنشاء الحساب بنجاح! تواصل مع الإدارة لشحن رصيدك.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "حدث خطأ أثناء حفظ البيانات", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "فشل إنشاء الحساب: قد يكون الإيميل مستخدم مسبقاً", Toast.LENGTH_LONG).show()
                }
            }
    }
}
