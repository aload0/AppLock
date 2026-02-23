package dev.pranav.applock.core.broadcast

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class DeviceAdmin : DeviceAdminReceiver() {
    companion object {
        private const val PREFS_NAME = "dev.pranav.applock.admin_prefs"
        private const val KEY_PASSWORD_VERIFIED = "password_verified"
    }

    override fun onEnabled(context: Context, intent: android.content.Intent) {
        super.onEnabled(context, intent)
        context.getSharedPreferences("app_lock_settings", Context.MODE_PRIVATE).edit {
            putBoolean("anti_uninstall", true)
        }

        val component = ComponentName(context, DeviceAdmin::class.java)

        getManager(context).setUninstallBlocked(component, context.packageName, true)
    }

    override fun onDisabled(context: Context, intent: android.content.Intent) {
        super.onDisabled(context, intent)
        context.getSharedPreferences("app_lock_settings", Context.MODE_PRIVATE).edit {
            putBoolean("anti_uninstall", false)
        }
    }

    fun setPasswordVerified(context: Context, verified: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_PASSWORD_VERIFIED, verified) }
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
