package dev.pranav.applock.core.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

fun enforceBatteryExemption(context: Context) {

    val pm = context.getSystemService(PowerManager::class.java)

    if (pm.isIgnoringBatteryOptimizations(context.packageName)) return

    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

    intent.data = Uri.parse("package:${context.packageName}")

    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    context.startActivity(intent)
}
