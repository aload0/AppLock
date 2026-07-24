package dev.pranav.applock.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import dev.pranav.applock.R
import dev.pranav.applock.AppLockApplication
import dev.pranav.applock.data.repository.AppLockRepository

/**
 * Provides vibration feedback with proper error handling and API level compatibility.
 */
fun vibrate(context: Context, duration: Long = DEFAULT_VIBRATION_DURATION) {
    try {
        val vibrator = getVibrator(context)
        val vibrationEffect = VibrationEffect.createOneShot(
            duration,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        vibrator.vibrate(vibrationEffect)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to vibrate device", e)
    }
}

private fun getVibrator(context: Context): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

/**
 * Launches battery optimization settings for the app.
 * Falls back to standard settings if specific request intent is not available.
 */
@SuppressLint("BatteryLife")
fun launchBatterySettings(context: Context) {
    try {
        val pm = context.packageManager
        val requestIgnoreIntent = createBatteryOptimizationIntent(context)

        if (requestIgnoreIntent.resolveActivity(pm) != null) {
            context.startActivity(requestIgnoreIntent)
            showBatteryOptimizationToast(context, context.getString(R.string.utils_battery_optimization_sent_toast))
        } else {
            Log.w(
                TAG,
                "Battery optimization intent not available, falling back to general settings"
            )
            launchGeneralBatterySettings(context)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to launch battery settings", e)
        showBatteryOptimizationToast(context, context.getString(R.string.utils_battery_open_failed_toast))
    }
}

/**
 * Checks if the app has usage stats permission.
 */
fun Context.hasUsagePermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = appOps.checkOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName
    )
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

private fun createBatteryOptimizationIntent(context: Context): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
}

private fun launchGeneralBatterySettings(context: Context) {
    try {
        val generalBatteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(generalBatteryIntent)
        showBatteryOptimizationToast(
            context,
            context.getString(R.string.utils_battery_configure_toast)
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to launch general battery settings", e)
    }
}

private fun showBatteryOptimizationToast(context: Context, message: String) {
    try {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to show toast message", e)
    }
}

private const val TAG = "AppLockUtils"
private const val DEFAULT_VIBRATION_DURATION = 500L

/**
 * Extension function to get AppLockRepository from Context
 */
fun Context.appLockRepository(): AppLockRepository =
    (applicationContext as AppLockApplication).appLockRepository
