package com.example.applocker

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Telefon ilovasida *#*#25667#*#* terilsa ishga tushadi (faqat ba'zi
 * standart "Phone" ilovalarida ishlaydi, hammasida emas).
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alias = ComponentName(context, "com.example.applocker.MainActivityAlias")
        context.packageManager.setComponentEnabledSetting(
            alias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
        Store.setIconHidden(context, false)
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
