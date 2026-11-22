package com.example.applocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LockService : AccessibilityService() {

    private val installers = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller"
    )

    // Manzil satrini o'qish mumkin bo'lgan brauzerlar (Chrome eng ishonchli ishlaydi)
    private val browsers = setOf(
        "com.android.chrome", "com.google.android.apps.chrome", "com.chrome.beta",
        "org.mozilla.firefox", "com.sec.android.app.sbrowser", "com.android.browser",
        "com.brave.browser", "com.microsoft.emmx", "com.opera.browser", "com.UCMobile.intl",
        "com.kiwibrowser.browser"
    )

    private var lastScan = 0L

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return

        // Allaqachon shu ilova uchun parol kiritilgan bo'lsa, tarkibni qayta tekshirmaymiz
        if (pkg == Store.unlocked) return

        if (pkg in Store.locked(this) || pkg in installers) {
            lock(pkg); return
        }

        val now = System.currentTimeMillis()
        if (now - lastScan < 400) return   // ortiqcha tekshirishni oldini olish
        lastScan = now

        when {
            pkg in browsers -> {
                val kws = Store.urlKeywords(this)
                if (kws.isNotEmpty() && treeContains(kws)) lock(pkg)
            }
            pkg.contains("settings") -> {
                val kws = Store.settingsKeywords(this)
                if (kws.isNotEmpty() && treeContains(kws)) lock(pkg)
            }
            !pkg.contains("systemui") && !pkg.contains("inputmethod") && !pkg.contains("keyboard") -> {
                Store.unlocked = null
            }
        }
    }

    private fun lock(pkg: String) {
        startActivity(
            Intent(this, LockActivity::class.java)
                .putExtra("pkg", pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    }

    /** Ekrandagi matn orasida kalit so'zlardan birortasi bormi, tekshiradi (max 250 ta node) */
    private fun treeContains(keywords: List<String>): Boolean {
        val root = rootInActiveWindow ?: return false
        val lower = keywords.map { it.lowercase() }
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var checked = 0
        while (stack.isNotEmpty() && checked < 250) {
            val node = stack.removeLast()
            checked++
            val t = node.text?.toString()?.lowercase()
            val d = node.contentDescription?.toString()?.lowercase()
            if ((t != null && lower.any { t.contains(it) }) ||
                (d != null && lower.any { d.contains(it) })
            ) return true
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.addLast(it) }
        }
        return false
    }

    override fun onInterrupt() {}
}
