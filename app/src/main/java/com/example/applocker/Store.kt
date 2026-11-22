package com.example.applocker

import android.content.ComponentName
import android.content.Context
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Store {
    @Volatile var unlocked: String? = null
    @Volatile var recoveryCode: String? = null
    @Volatile var recoverySentAt: Long = 0L

    private fun p(c: Context) = c.getSharedPreferences("lock", Context.MODE_PRIVATE)

    private fun hash(pw: String, salt: String): String {
        val spec = PBEKeySpec(pw.toCharArray(), salt.toByteArray(), 100_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hasPw(c: Context) = p(c).contains("hash")

    fun setPw(c: Context, pw: String) {
        val salt = UUID.randomUUID().toString()
        p(c).edit().putString("salt", salt).putString("hash", hash(pw, salt)).apply()
    }

    fun check(c: Context, pw: String): Boolean {
        val salt = p(c).getString("salt", null) ?: return false
        return hash(pw, salt) == p(c).getString("hash", null)
    }

    fun locked(c: Context): Set<String> = p(c).getStringSet("apps", emptySet())!!.toSet()
    fun setLocked(c: Context, s: Set<String>) = p(c).edit().putStringSet("apps", s).apply()

    fun botToken(c: Context): String = p(c).getString("bot_token", "") ?: ""
    fun setBotToken(c: Context, v: String) = p(c).edit().putString("bot_token", v).apply()
    fun chatId(c: Context): String = p(c).getString("chat_id", "") ?: ""
    fun setChatId(c: Context, v: String) = p(c).edit().putString("chat_id", v).apply()

    /** Brauzerda qidirib bloklanadigan matnlar, masalan "youtube.com" */
    fun urlKeywords(c: Context): List<String> =
        p(c).getString("url_keywords", "youtube.com")!!
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
    fun setUrlKeywords(c: Context, v: String) = p(c).edit().putString("url_keywords", v).apply()

    /** Settings ichida qidirib bloklanadigan ekran nomlari, masalan "Apps, Ilovalar" */
    fun settingsKeywords(c: Context): List<String> =
        p(c).getString("settings_keywords", "")!!
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
    fun setSettingsKeywords(c: Context, v: String) = p(c).edit().putString("settings_keywords", v).apply()

    fun setIconHidden(c: Context, v: Boolean) = p(c).edit().putBoolean("icon_hidden", v).apply()
    fun adminComponent(c: Context) = ComponentName(c, DeviceAdmin::class.java)
}
