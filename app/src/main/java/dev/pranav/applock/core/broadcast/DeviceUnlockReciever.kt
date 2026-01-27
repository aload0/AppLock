package dev.pranav.applock.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.services.AppLockManager

class DeviceUnlockReceiver(private val onDeviceUnlocked: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> {
                LogUtils.d("DeviceUnlockReceiver", "Device unlocked (ACTION_USER_PRESENT)")
                onDeviceUnlocked()
            }

            Intent.ACTION_SCREEN_OFF -> {
                AppLockManager.clearTemporarilyUnlockedApp()
                AppLockManager.appUnlockTimes.clear()
                LogUtils.d("DeviceUnlockReceiver", "Screen turned OFF (locked)")
            }
        }
    }
}
