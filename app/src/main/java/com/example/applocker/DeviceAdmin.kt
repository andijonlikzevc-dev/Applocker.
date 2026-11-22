package com.example.applocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class DeviceAdmin : DeviceAdminReceiver() {
    // Foydalanuvchi "Deactivate" bosganda standart ogohlantirish ko'rsatiladi.
    // Android buni to'liq bloklashga imkon bermaydi, lekin Settings allaqachon
    // parol bilan bloklangani uchun bu ekranga yetib borish o'zi parol talab qiladi.
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Diqqat: bu dastur o'chirilsa, ilovalarni bloklash to'xtaydi."
    }
}
