package dev.pranav.applock.shizuku

import android.app.ActivityManager
import android.app.ActivityManagerNative
import android.app.IActivityManager
import android.app.IActivityTaskManager
import android.content.*
import android.content.Context.RECEIVER_EXPORTED
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.IWindowManager
import dev.pranav.applock.core.broadcast.DeviceUnlockReceiver
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.services.AppLockManager
import dev.pranav.applock.services.isDeviceLocked
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ShizukuActivityManager(
    private val context: Context,
    private val appLockRepository: AppLockRepository,
    private val onForegroundAppChanged: (String, String, Long) -> Unit
) {
    private val TAG = "ShizukuActivityManager"
    private var lastForegroundApp = ""
    private var deviceUnlockReceiver: DeviceUnlockReceiver? = null
    private var shouldLockAppsOnReturn = false

    private val handler = Handler(Looper.getMainLooper())
    private val checkForegroundRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            // Schedule itself again after 500ms
            handler.postDelayed(this, 500)
        }
    }

    private val homeButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra("reason")
                    LogUtils.d(TAG, "System dialog closed, reason: $reason")
                    if (lastForegroundApp == topActivity?.packageName && topActivity?.className == "com.android.launcher3.uioverrides.QuickstepLauncher") {
                        AppLockManager.clearTemporarilyUnlockedApp()
                    }

                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned off, will lock apps on return")
                    shouldLockAppsOnReturn = true
                    lastForegroundApp = ""
                }

                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Device unlocked, will lock apps on return")
                    shouldLockAppsOnReturn = true
                }
            }
        }
    }

    fun start(): Boolean {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
            Log.e(TAG, "Shizuku is not available")
            return false
        }

        try {
            registerEventReceivers()
            startForegroundAppMonitoring()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun registerEventReceivers() {
        // Register home button and system events receiver
        val homeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        SystemServiceHelper.getSystemService("activity").let {
            if (Build.VERSION.SDK_INT >= 26) {
                IActivityManager.Stub.asInterface(it)
            } else {
                ActivityManagerNative.asInterface(it)
            }
        }

        context.registerReceiver(homeButtonReceiver, homeFilter, RECEIVER_EXPORTED)

        // Keep the device unlock receiver for compatibility
        val unlockFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        deviceUnlockReceiver = DeviceUnlockReceiver {
            shouldLockAppsOnReturn = true
        }
        context.registerReceiver(deviceUnlockReceiver, unlockFilter)
    }

    val windowManager: IWindowManager
        get() = SystemServiceHelper.getSystemService("window")
            .let(::ShizukuBinderWrapper)
            .let(IWindowManager.Stub::asInterface)

    private fun startForegroundAppMonitoring() {
        handler.removeCallbacks(checkForegroundRunnable)
        handler.post(checkForegroundRunnable)
        Log.d(TAG, "Foreground app monitoring started")
    }

    private fun checkForegroundApp() {
        if (!appLockRepository.isProtectEnabled()) return
        if (appLockRepository.getBackendImplementation() != BackendImplementation.SHIZUKU) {
            handler.removeCallbacks(checkForegroundRunnable)
            return
        }
        if (context.isDeviceLocked()) return

        val activity = topActivity ?: return
        val packageName = activity.packageName
        val className = activity.className

        // Skip our own app and known recents classes
        if (packageName == context.packageName) return

        // Skip if app is temporarily unlocked
        if (packageName == lastForegroundApp && AppLockManager.isAppTemporarilyUnlocked(packageName)) return

        // If we should lock apps on return (home button pressed, device locked, etc.)
        // then trigger app lock for any new foreground app
        if (shouldLockAppsOnReturn && packageName != lastForegroundApp) {
            LogUtils.d(TAG, "Should lock apps on return - triggering for: $packageName")
            shouldLockAppsOnReturn = false // Reset the flag

            val timeMillis = System.currentTimeMillis()
            lastForegroundApp = packageName
            onForegroundAppChanged(packageName, className, timeMillis)
            return
        }

        // Normal app switching - only trigger if current app has changed
        if (packageName != lastForegroundApp) {
            val triggerExclusions = appLockRepository.getTriggerExcludedApps()

            // Check if previous app was in trigger exclusions
            if (lastForegroundApp in triggerExclusions) {
                LogUtils.d(
                    TAG,
                    "Previous app $lastForegroundApp is excluded, skipping app lock for $packageName"
                )
                lastForegroundApp = packageName
                return
            }

            val timeMillis = System.currentTimeMillis()
            LogUtils.d(TAG, "Foreground app changed to: $packageName, class: $className")

            lastForegroundApp = packageName
            onForegroundAppChanged(packageName, className, timeMillis)
        }
    }

    fun stop() {
        homeButtonReceiver.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
                Log.d(TAG, "Home button receiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering home button receiver", e)
            }
        }

        deviceUnlockReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
                deviceUnlockReceiver = null
                Log.d(TAG, "Device unlock receiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering device unlock receiver", e)
            }
        }

        handler.removeCallbacks(checkForegroundRunnable)
        Log.d(TAG, "ShizukuActivityManager stopped")
    }
}

val topActivity: ComponentName?
    get() = getTasksWrapper().first().topActivity

private val activityTaskManager: IActivityTaskManager =
    SystemServiceHelper.getSystemService("activity_task")
        .let(::ShizukuBinderWrapper)
        .let(IActivityTaskManager.Stub::asInterface)

private fun getTasksWrapper(): List<ActivityManager.RunningTaskInfo> = when {
    Build.VERSION.SDK_INT < 31 -> activityTaskManager.getTasks(1)
    else -> runCatching { activityTaskManager.getTasks(1, false, false, Display.INVALID_DISPLAY) }
        .getOrElse { activityTaskManager.getTasks(1, false, false) }
}
