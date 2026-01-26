package dev.pranav.applock.services

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dev.pranav.applock.data.repository.BackendImplementation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object AppLockConstants {
    val KNOWN_RECENTS_CLASSES = setOf(
        "com.android.systemui.recents.RecentsActivity",
        "com.android.quickstep.RecentsActivity",
        "com.android.systemui.recents.RecentsView",
        "com.android.systemui.recents.RecentsPanelView",
    )

    val EXCLUDED_APPS = setOf(
        "com.android.systemui",
        "com.android.intentresolver",
        "com.google.android.permissioncontroller",
        "android.uid.system:1000",
        "com.google.android.googlequicksearchbox"
    )

    val ADMIN_CONFIG_CLASSES = setOf(
        "com.android.settings.deviceadmin.DeviceAdminAdd",
        "com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd",
        "com.android.settings.deviceadmin.DeviceAdminSettings",
    )

    val ACCESSIBILITY_SETTINGS_CLASSES = setOf(
        "com.android.settings.accessibility.AccessibilitySettings",
        "com.android.settings.accessibility.AccessibilityMenuActivity",
        "com.android.settings.accessibility.AccessibilityShortcutActivity",
        "com.android.settings.Settings\$AccessibilitySettingsActivity"
    )

    const val MAX_RESTART_ATTEMPTS = 3
    const val RESTART_COOLDOWN_MS = 30000L
    const val RESTART_INTERVAL_MS = 5000L
}

fun Context.isDeviceLocked(): Boolean {
    val keyguardManager = getSystemService(KeyguardManager::class.java)
    return keyguardManager?.isKeyguardLocked ?: false
}

@Suppress("DEPRECATION")
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(ActivityManager::class.java) ?: return false
    return manager.getRunningServices(Int.MAX_VALUE)
        .any { serviceClass.name == it.service.className }
}

object AppLockManager {
    private const val TAG = "AppLockManager"

    var temporarilyUnlockedApp: String = ""
    val appUnlockTimes = ConcurrentHashMap<String, Long>()
    val isLockScreenShown = AtomicBoolean(false)
    var currentBiometricState: Any? = null

    private val serviceRestartAttempts = ConcurrentHashMap<String, Int>()
    private val lastRestartTime = ConcurrentHashMap<String, Long>()

    private val ALL_APP_LOCK_SERVICES = setOf(
        ShizukuAppLockService::class.java,
        ExperimentalAppLockService::class.java
    )

    fun unlockApp(packageName: String) {
        temporarilyUnlockedApp = packageName
        appUnlockTimes[packageName] = System.currentTimeMillis()
        Log.d(
            TAG,
            "App $packageName unlocked at timestamp: ${appUnlockTimes[packageName]}, current time: ${System.currentTimeMillis()}"
        )
    }

    fun temporarilyUnlockAppWithBiometrics(packageName: String) {
        unlockApp(packageName)
        reportBiometricAuthFinished()
    }

    fun reportBiometricAuthStarted() {}
    fun reportBiometricAuthFinished() {}

    fun isAppTemporarilyUnlocked(packageName: String): Boolean =
        temporarilyUnlockedApp == packageName

    fun clearTemporarilyUnlockedApp() {
        temporarilyUnlockedApp = ""
    }

    fun startFallbackServices(context: Context, failedService: Class<*>) {
        val serviceName = failedService.simpleName

        if (!shouldAttemptRestart(serviceName)) return

        val targetBackend = when (failedService) {
            ShizukuAppLockService::class.java -> BackendImplementation.USAGE_STATS
            ExperimentalAppLockService::class.java -> BackendImplementation.ACCESSIBILITY
            AppLockAccessibilityService::class.java -> null
            else -> null
        }

        when (targetBackend) {
            BackendImplementation.ACCESSIBILITY -> {
                if (AppLockAccessibilityService.isServiceRunning) return
                // Check if accessibility service is enabled in system settings
                // But don't show toast if we are just restarting or if it's not enabled yet
                // The user needs to enable it manually anyway if they chose this backend.
                Log.d(
                    TAG,
                    "Attempting ACCESSIBILITY backend restart. (Manually enabled in settings)"
                )
            }

            BackendImplementation.USAGE_STATS, BackendImplementation.SHIZUKU -> {
                startServiceByBackend(context, targetBackend)
            }

            null -> {
                if (failedService == AppLockAccessibilityService::class.java) {
                    Log.d(
                        TAG,
                        "Accessibility Service stopped. Waiting for system restart or manual re-enable."
                    )
                    return
                }
                showNoPermissionsToast(context)
                return
            }
        }
        recordRestartAttempt(serviceName)
    }

    fun stopAllOtherServices(context: Context, excludeService: Class<*>) {
        ALL_APP_LOCK_SERVICES
            .filter { it != excludeService }
            .forEach {
                context.stopService(Intent(context, it))
            }
        Log.d(TAG, "Stopped all main app lock services except ${excludeService.simpleName}.")
    }

    fun resetRestartAttempts(serviceName: String) {
        serviceRestartAttempts.remove(serviceName)
        lastRestartTime.remove(serviceName)
        Log.d(TAG, "Reset restart attempts for $serviceName")
    }

    private fun showNoPermissionsToast(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Selected backend has insufficient permissions. Please provide necessary permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun shouldAttemptRestart(serviceName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val attempts = serviceRestartAttempts[serviceName] ?: 0
        val lastRestart = lastRestartTime[serviceName] ?: 0L

        if (currentTime - lastRestart < AppLockConstants.RESTART_INTERVAL_MS) {
            Log.d(TAG, "Service $serviceName restart too recent, skipping")
            return false
        }

        if (attempts >= AppLockConstants.MAX_RESTART_ATTEMPTS) {
            if (currentTime - lastRestart > AppLockConstants.RESTART_COOLDOWN_MS) {
                Log.d(TAG, "Cooldown expired for $serviceName, resetting attempts")
                serviceRestartAttempts[serviceName] = 0
                return true
            }
            Log.d(TAG, "Max restart attempts reached for $serviceName, in cooldown")
            return false
        }

        return true
    }

    private fun recordRestartAttempt(serviceName: String) {
        val currentTime = System.currentTimeMillis()
        serviceRestartAttempts.compute(serviceName) { _, attempts -> (attempts ?: 0) + 1 }
        lastRestartTime[serviceName] = currentTime
        Log.d(
            TAG,
            "Recorded restart attempt ${serviceRestartAttempts[serviceName]} for $serviceName"
        )
    }

    private fun startServiceByBackend(context: Context, backend: BackendImplementation) {
        try {
            stopAllOtherServices(context, Nothing::class.java)

            val serviceClass = when (backend) {
                BackendImplementation.SHIZUKU -> ShizukuAppLockService::class.java
                BackendImplementation.USAGE_STATS -> ExperimentalAppLockService::class.java
                else -> return
            }

            Log.d(TAG, "Starting $backend service as fallback.")
            context.startService(Intent(context, serviceClass))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start fallback service for backend: $backend", e)
        }
    }
}
