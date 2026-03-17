package com.bodadroid.client

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val bloggerIdInput = findViewById<EditText>(R.id.bloggerIdInput)
        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val saveBtn = findViewById<Button>(R.id.saveSettingsBtn)

        // جلب الإعدادات القديمة إن وجدت
        db.collection("Users").document(userId).get().addOnSuccessListener { doc ->
            if (doc != null) {
                bloggerIdInput.setText(doc.getString("blogger_id") ?: "")
                apiKeyInput.setText(doc.getString("api_key") ?: "")
            }
        }

        saveBtn.setOnClickListener {
            val bId = bloggerIdInput.text.toString().trim()
            val apiK = apiKeyInput.text.toString().trim()

            if (bId.isEmpty() || apiK.isEmpty()) {
                Toast.makeText(this, "يرجى ملء جميع الحقول!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // تحديث بيانات المستخدم في قاعدة البيانات
            val updates = hashMapOf<String, Any>(
                "blogger_id" to bId,
                "api_key" to apiK
            )

            db.collection("Users").document(userId).update(updates).addOnSuccessListener {
                Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
                finish() // العودة للوحة التحكم
            }
        }
    }
}
