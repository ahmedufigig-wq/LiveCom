package com.livecom.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val gold = Color.rgb(212, 175, 55)
    private val background = Color.rgb(12, 12, 14)
    private val panel = Color.rgb(27, 27, 31)
    private val white = Color.WHITE
    private val muted = Color.rgb(185, 185, 190)

    private lateinit var root: LinearLayout
    private lateinit var roomNameInput: EditText
    private lateinit var nicknameInput: EditText
    private lateinit var roomsContainer: LinearLayout
    private lateinit var statusText: TextView
    private var currentRoomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoading()
        if (auth.currentUser != null) {
            showHome()
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { showHome() }
                .addOnFailureListener { error ->
                    showError(authErrorMessage(error))
                }
        }
    }

    private fun showLoading() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(this@MainActivity.background)
        }
        root.addView(label("LiveCom", 36f, gold, true))
        root.addView(label("جارٍ تجهيز حسابك...", 16f, muted))
        setContentView(root)
    }

    private fun showHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 26, 20, 20)
            setBackgroundColor(this@MainActivity.background)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("LiveCom", 34f, gold, true))
        content.addView(label("مساحتك للبث المباشر والتواصل", 16f, white))
        content.addView(label("أنشئ غرفة أو انضم إلى غرفة عامة", 14f, muted).apply {
            setPadding(0, 8, 0, 22)
        })

        content.addView(sectionTitle("إنشاء غرفة جديدة"))
        roomNameInput = input("اسم الغرفة")
        content.addView(roomNameInput)
        nicknameInput = input("اسمك داخل التطبيق")
        content.addView(nicknameInput)
        content.addView(actionButton("إنشاء غرفة عامة") { createRoom() })

        content.addView(sectionTitle("الغرف المتاحة"))
        content.addView(actionButton("تحديث قائمة الغرف") { loadRooms() })
        statusText = label("اضغط تحديث لعرض الغرف العامة.", 13f, muted)
        statusText.gravity = Gravity.START
        content.addView(statusText)
        roomsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 0)
        }
        content.addView(roomsContainer)
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        loadRooms()
    }

    private fun createRoom() {
        val name = roomNameInput.text.toString().trim()
        val nickname = nicknameInput.text.toString().trim().ifBlank { "مستخدم" }
        if (name.length < 3) {
            toast("اكتب اسم غرفة من 3 أحرف على الأقل")
            return
        }
        val uid = auth.currentUser?.uid ?: return showError("الحساب غير جاهز، أعد فتح التطبيق")
        val room = hashMapOf<String, Any>(
            "roomName" to name,
            "ownerUid" to uid,
            "ownerName" to nickname,
            "memberUids" to listOf(uid),
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("rooms").add(room)
            .addOnSuccessListener { document ->
                toast("تم إنشاء الغرفة")
                openRoom(document.id, name, true)
            }
            .addOnFailureListener { error -> toast("فشل إنشاء الغرفة: ${error.localizedMessage}") }
    }

    private fun loadRooms() {
        if (!::roomsContainer.isInitialized) return
        statusText.text = "جارٍ تحميل الغرف..."
        roomsContainer.removeAllViews()
        db.collection("rooms")
            .whereEqualTo("status", "open")
            .limit(25)
            .get()
            .addOnSuccessListener { snapshot ->
                roomsContainer.removeAllViews()
                if (snapshot.isEmpty) {
                    statusText.text = "لا توجد غرف عامة حالياً. أنشئ أول غرفة!"
                    return@addOnSuccessListener
                }
                statusText.text = "الغرف المتاحة: ${snapshot.size()}"
                for (doc in snapshot.documents) {
                    val name = doc.getString("roomName") ?: "غرفة بدون اسم"
                    val owner = doc.getString("ownerName") ?: "مضيف"
                    val members = doc.get("memberUids") as? List<*> ?: emptyList<Any>()
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(14, 12, 14, 12)
                        setBackgroundColor(this@MainActivity.panel)
                    }
                    card.addView(label(name, 18f, gold, true).apply { gravity = Gravity.START })
                    card.addView(label("المضيف: $owner  •  الأعضاء: ${members.size}", 13f, muted).apply {
                        gravity = Gravity.START
                    })
                    card.addView(actionButton("انضم إلى الغرفة") {
                        joinRoom(doc.id, name)
                    })
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    roomsContainer.addView(card, params)
                }
            }
            .addOnFailureListener { error ->
                statusText.text = "تعذر تحميل الغرف. تأكد من إعداد Firestore وقواعده."
                toast(error.localizedMessage ?: "خطأ في تحميل الغرف")
            }
    }

    private fun joinRoom(roomId: String, roomName: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomId)
            .update("memberUids", FieldValue.arrayUnion(uid))
            .addOnSuccessListener {
                toast("تم الانضمام إلى الغرفة")
                openRoom(roomId, roomName, false)
            }
            .addOnFailureListener { error -> toast("تعذر الانضمام: ${error.localizedMessage}") }
    }

    private fun openRoom(roomId: String, roomName: String, isOwner: Boolean) {
        currentRoomId = roomId
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(this@MainActivity.background)
        }
        root.addView(label("LiveCom", 32f, gold, true))
        root.addView(label(roomName, 24f, white, true).apply { setPadding(0, 20, 0, 10) })
        root.addView(label(if (isOwner) "أنت مضيف هذه الغرفة" else "أنت مشارك في هذه الغرفة", 15f, muted))
        root.addView(label("معرّف الغرفة: $roomId", 12f, muted).apply { setPadding(0, 12, 0, 20) })
        root.addView(label("تم تجهيز الانضمام وحفظ الأعضاء عبر Firestore. البث الصوتي والمرئي سيُضاف بعد ربط مزود البث واختباره.", 14f, white))
        root.addView(actionButton("مغادرة الغرفة") { leaveRoom(roomId) })
    }

    private fun leaveRoom(roomId: String) {
        val uid = auth.currentUser?.uid ?: return showHome()
        db.collection("rooms").document(roomId)
            .update("memberUids", FieldValue.arrayRemove(uid))
            .addOnCompleteListener {
                currentRoomId = null
                showHome()
            }
    }

    private fun authErrorMessage(error: Exception): String {
        val authError = error as? FirebaseAuthException
        val code = authError?.errorCode.orEmpty()
        val details = error.localizedMessage.orEmpty()

        return if (
            code.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            code.contains("ADMIN_ONLY_OPERATION", ignoreCase = true) ||
            details.contains("ADMIN_ONLY_OPERATION", ignoreCase = true) ||
            details.contains("administrators only", ignoreCase = true)
        ) {
            "تسجيل الدخول المجهول غير مفعّل في Firebase.\n\n" +
                "الحل: افتح Firebase Console ثم Authentication ثم Sign-in method، " +
                "وفعّل Anonymous واضغط Save.\n\n" +
                "إذا كان Anonymous مفعّلاً بالفعل، فتحقق من صلاحيات المشروع وسياسات Identity Platform.\n" +
                "رمز الخطأ: ${code.ifBlank { "ADMIN_ONLY_OPERATION" }}"
        } else {
            "تعذر الاتصال بخدمة الحسابات.\n" +
                "تحقق من الإنترنت وإعدادات Firebase ثم أعد المحاولة.\n" +
                "رمز الخطأ: ${code.ifBlank { "غير متوفر" }}\n" +
                "التفاصيل: ${details.ifBlank { "لا توجد تفاصيل إضافية" }}"
        }
    }

    private fun showError(message: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(this@MainActivity.background)
        }
        root.addView(label("LiveCom", 34f, gold, true))
        root.addView(label(message, 15f, white))
        root.addView(actionButton("إعادة المحاولة") { recreate() })
        setContentView(root)
    }

    private fun sectionTitle(value: String) = label(value, 18f, gold, true).apply {
        gravity = Gravity.START
        setPadding(0, 20, 0, 8)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(4, 6, 4, 6)
        }

    private fun input(hintText: String) = EditText(this).apply {
        hint = hintText
        setHintTextColor(muted)
        setTextColor(white)
        textSize = 16f
        isSingleLine = true
        setPadding(14, 10, 14, 10)
        setBackgroundColor(this@MainActivity.panel)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
        layoutParams = params
    }

    private fun actionButton(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        setTextColor(Color.BLACK)
        setBackgroundColor(gold)
        isAllCaps = false
        textSize = 15f
        setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8; bottomMargin = 8 }
        layoutParams = params
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (currentRoomId != null) {
            leaveRoom(currentRoomId!!)
        } else {
            super.onBackPressed()
        }
    }
}
