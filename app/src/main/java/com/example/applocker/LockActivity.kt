package com.example.applocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread
import kotlin.random.Random

class LockActivity : Activity() {
    private var pkg = ""
    private var tries = 0
    private var mode = "password"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        pkg = intent.getStringExtra("pkg") ?: ""
        render("Ilova bloklangan", "Parol")
    }

    private fun render(title: String, hint: String) {
        val et = EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val btn = Button(this).apply { text = "Tasdiqlash" }
        btn.setOnClickListener { onSubmit(et.text.toString()) }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 80, 80, 80)
            addView(TextView(context).apply { text = title; textSize = 22f; gravity = Gravity.CENTER })
            addView(et)
            addView(btn)
        })
    }

    private fun onSubmit(input: String) {
        if (mode == "code") { checkCode(input); return }

        if (Store.check(this, input)) {
            Store.unlocked = pkg
            finish()
            return
        }

        tries++
        Toast.makeText(this, "Parol noto'g'ri ($tries/6)", Toast.LENGTH_SHORT).show()
        when {
            tries == 3 -> Toast.makeText(this, "Yana xato qilinsa Telegram'ga kod yuboriladi", Toast.LENGTH_LONG).show()
            tries >= 6 -> sendRecoveryCode()
        }
        if (tries < 6) render("Ilova bloklangan", "Parol")
    }

    private fun sendRecoveryCode() {
        val token = Store.botToken(this)
        val chatId = Store.chatId(this)
        if (token.isBlank() || chatId.isBlank()) {
            Toast.makeText(this, "Telegram sozlanmagan", Toast.LENGTH_LONG).show()
            goHome(); return
        }
        val code = (10000..99999).random(Random).toString()
        Store.recoveryCode = code
        Store.recoverySentAt = System.currentTimeMillis()
        val ids = chatId.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        thread {
            for (id in ids) {
                try {
                    val text = URLEncoder.encode("App Locker kod: $code", "UTF-8")
                    val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$id&text=$text")
                    (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
                        inputStream.close()
                        disconnect()
                    }
                } catch (_: Exception) { }
            }
        }
        Toast.makeText(this, "Kod Telegram'ga yuborildi (${ids.size} ta akkaunt)", Toast.LENGTH_LONG).show()
        mode = "code"; tries = 0
        render("Telegram'dan kelgan kodni kiriting", "5 xonali kod")
    }

    private fun checkCode(input: String) {
        val valid = Store.recoveryCode != null &&
            System.currentTimeMillis() - Store.recoverySentAt < 10 * 60 * 1000
        if (valid && input.trim() == Store.recoveryCode) {
            Store.recoveryCode = null
            Store.unlocked = pkg
            finish()
            return
        }
        tries++
        Toast.makeText(this, "Kod noto'g'ri", Toast.LENGTH_SHORT).show()
        if (tries >= 3) goHome() else render("Telegram'dan kelgan kodni kiriting", "5 xonali kod")
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
