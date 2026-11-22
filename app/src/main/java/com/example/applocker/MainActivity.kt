package com.example.applocker

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Store.hasPw(this)) askPassword() else askNewPassword()
    }

    private fun pwField() = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun askNewPassword() {
        val et = pwField()
        AlertDialog.Builder(this).setTitle("Yangi parol o'rnating").setView(et)
            .setCancelable(false)
            .setPositiveButton("Saqlash") { _, _ ->
                val pw = et.text.toString()
                if (pw.length < 4) {
                    Toast.makeText(this, "Kamida 4 ta belgi", Toast.LENGTH_SHORT).show()
                    askNewPassword()
                } else { Store.setPw(this, pw); showList() }
            }
            .setNegativeButton("Bekor") { _, _ -> finish() }.show()
    }

    private fun askPassword() {
        val et = pwField()
        AlertDialog.Builder(this).setTitle("Parol").setView(et).setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                if (Store.check(this, et.text.toString())) showList()
                else { Toast.makeText(this, "Noto'g'ri parol", Toast.LENGTH_SHORT).show(); finish() }
            }
            .setNegativeButton("Bekor") { _, _ -> finish() }.show()
    }

    private fun showList() {
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launch, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        val locked = Store.locked(this).toMutableSet()
        val lv = ListView(this).apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            adapter = ArrayAdapter(context, android.R.layout.simple_list_item_multiple_choice,
                apps.map { it.second })
        }
        apps.forEachIndexed { i, a -> lv.setItemChecked(i, a.first in locked) }
        lv.setOnItemClickListener { _, _, i, _ ->
            if (lv.isItemChecked(i)) locked.add(apps[i].first) else locked.remove(apps[i].first)
            Store.setLocked(this, locked)
        }

        val accessBtn = Button(this).apply {
            text = "1) Accessibility xizmatini yoqish"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val adminBtn = Button(this).apply {
            text = "2) Admin huquqini yoqish (o'chirishni qiyinlashtiradi)"
            setOnClickListener {
                startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, Store.adminComponent(this@MainActivity))
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Ilovani tasodifan o'chirib yuborishdan himoya qiladi"))
            }
        }

        val tokenEt = EditText(this).apply {
            hint = "Telegram bot token (@BotFather dan)"
            setText(Store.botToken(context))
        }
        val chatEt = EditText(this).apply {
            hint = "Chat ID(lar), vergul bilan: 111,222"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Store.chatId(context))
        }
        val saveTgBtn = Button(this).apply {
            text = "Telegram sozlamasini saqlash"
            setOnClickListener {
                Store.setBotToken(this@MainActivity, tokenEt.text.toString().trim())
                Store.setChatId(this@MainActivity, chatEt.text.toString().trim())
                Toast.makeText(this@MainActivity, "Saqlandi", Toast.LENGTH_SHORT).show()
            }
        }

        val urlEt = EditText(this).apply {
            hint = "Brauzerda bloklanadigan saytlar, vergul bilan (masalan: youtube.com)"
            setText(Store.urlKeywords(context).joinToString(", "))
        }
        val settingsEt = EditText(this).apply {
            hint = "Settings'da bloklanadigan bo'limlar (masalan: Apps, Ilovalar)"
            setText(Store.settingsKeywords(context).joinToString(", "))
        }
        val saveFiltersBtn = Button(this).apply {
            text = "Saytlar / Settings filtrini saqlash"
            setOnClickListener {
                Store.setUrlKeywords(this@MainActivity, urlEt.text.toString())
                Store.setSettingsKeywords(this@MainActivity, settingsEt.text.toString())
                Toast.makeText(this@MainActivity, "Saqlandi", Toast.LENGTH_SHORT).show()
            }
        }

        val hideBtn = Button(this).apply {
            text = "3) Ikonkani yashirish"
            setOnClickListener {
                if (Store.botToken(this@MainActivity).isBlank() || Store.chatId(this@MainActivity).isBlank()) {
                    Toast.makeText(this@MainActivity, "Avval Telegram sozlamasini saqlang", Toast.LENGTH_LONG).show()
                } else {
                    packageManager.setComponentEnabledSetting(
                        ComponentName(this@MainActivity, "com.example.applocker.MainActivityAlias"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                    )
                    Store.setIconHidden(this@MainActivity, true)
                    Toast.makeText(
                        this@MainActivity,
                        "Ikonka yashirildi. Qayta ochish: telefon ilovasida *#*#25667#*#* tering",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(accessBtn); addView(adminBtn)
            addView(tokenEt); addView(chatEt); addView(saveTgBtn)
            addView(urlEt); addView(settingsEt); addView(saveFiltersBtn)
            addView(hideBtn)
            addView(TextView(context).apply { text = "4) Bloklanadigan ilovalarni belgilang:"; setPadding(24, 16, 24, 8) })
            addView(lv, LinearLayout.LayoutParams(-1, 1200))
        }
        scroll.addView(col)
        setContentView(scroll)
    }
}
