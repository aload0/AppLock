package dev.pranav.applock.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.hasUsagePermission
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.AppLockRepository.Companion.shouldStartService
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.features.lockscreen.ui.PasswordOverlayActivity
import java.util.Timer
import kotlin.concurrent.timerTask

class ExperimentalAppLockService : Service() {
    private val TAG = "ExperimentalAppLockService"
    private val NOTIFICATION_ID = 113
    private val CHANNEL_ID = "ExperimentalAppLockServiceChannel"

    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }
    private val usageStatsManager: UsageStatsManager by lazy { getSystemService()!! }
    private val notificationManager: NotificationManager by lazy { getSystemService()!! }
    private val biometricAuthStarted by lazy { AppLockAccessibilityService.BiometricState.AUTH_STARTED.toString() }

    private var timer: Timer? = null
    private var previousForegroundPackage = ""

    private val screenStateReceiver = object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                LogUtils.d(
                    TAG,
                    "Screen off detected in Usage Stats fallback. Resetting AppLock state."
                )
                AppLockManager.isLockScreenShown.set(false)
                AppLockManager.clearTemporarilyUnlockedApp()
                previousForegroundPackage = ""
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!shouldStartService(appLockRepository, this::class.java) || !hasUsagePermission()) {
            Log.e(TAG, "Permissions missing or service not needed. Falling back.")
            AppLockManager.startFallbackServices(this, this::class.java)
            stopSelf()
            return START_NOT_STICKY
        }

        AppLockManager.resetRestartAttempts(TAG)
        appLockRepository.setActiveBackend(BackendImplementation.USAGE_STATS)
        AppLockManager.stopAllOtherServices(this, this::class.java)
        AppLockManager.isLockScreenShown.set(false)

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)

        startMonitoringTimer()
        startForegroundService()

        return START_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        LogUtils.d(TAG, "Service destroyed. Checking for fallback.")

        if (shouldStartService(appLockRepository, this::class.java)) {
            AppLockManager.startFallbackServices(this, this::class.java)
        }

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered or already unregistered")
        }

        AppLockManager.isLockScreenShown.set(false)
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Monitoring ---

    private fun startMonitoringTimer() {
        timer?.cancel()
        timer = Timer()
        timer?.schedule(timerTask {
            if (!appLockRepository.isProtectEnabled() || applicationContext.isDeviceLocked()) {
                if (applicationContext.isDeviceLocked()) {
                    AppLockManager.appUnlockTimes.clear()
                    previousForegroundPackage = ""
                }
                return@timerTask
            }

            val foregroundApp = getCurrentForegroundAppPackage() ?: return@timerTask
            val currentPackage = foregroundApp.first
            val triggeringPackage = previousForegroundPackage
            previousForegroundPackage = currentPackage

            if (isExclusionApp(currentPackage)) return@timerTask

            if (triggeringPackage in appLockRepository.getTriggerExcludedApps()) {
                return@timerTask
            }

            if (currentPackage == triggeringPackage) return@timerTask

            checkAndLockApp(currentPackage, triggeringPackage, System.currentTimeMillis())
        }, 0, 250)
    }

    private fun isExclusionApp(packageName: String): Boolean {
        val keyboardPackages = getSystemService<InputMethodManager>()
            ?.enabledInputMethodList
            ?.map { it.packageName }
            ?: emptyList()

        return packageName == this.packageName ||
                packageName in keyboardPackages ||
                packageName in AppLockConstants.EXCLUDED_APPS
    }

    /**
     * Returns the foreground package name and class name, or null if filtered.
     */
    private fun getCurrentForegroundAppPackage(): Pair<String, String>? {
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000 * 100, time)
        val event = UsageEvents.Event()
        var recentApp: Pair<String, String>? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
            if (event.className == "dev.pranav.applock.features.lockscreen.ui.PasswordOverlayActivity") continue

            if (event.className in AppLockConstants.KNOWN_RECENTS_CLASSES
            ) {
                continue
            }

            recentApp = Pair(event.packageName, event.className)
        }
        return recentApp
    }

    private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
        val lockedApps = appLockRepository.getLockedApps()
        if (packageName !in lockedApps) return

        val unlockDurationMinutes = appLockRepository.getUnlockTimeDuration()
        val unlockTimestamp = AppLockManager.appUnlockTimes[packageName] ?: 0L

        LogUtils.d(
            TAG,
            "checkAndLockApp: pkg=$packageName, duration=$unlockDurationMinutes min, unlockTime=$unlockTimestamp, currentTime=$currentTime, isLockScreenShown=${AppLockManager.isLockScreenShown.get()}"
        )

        if (unlockDurationMinutes > 0 && unlockTimestamp > 0) {
            if (unlockDurationMinutes >= 10_000) {
                return
            }

            val durationMillis = unlockDurationMinutes.toLong() * 60_000L

            val elapsedMillis = currentTime - unlockTimestamp

            LogUtils.d(
                TAG,
                "Grace period check: elapsed=${elapsedMillis}ms (${elapsedMillis / 1000}s), duration=${durationMillis}ms (${durationMillis / 1000}s)"
            )

            if (elapsedMillis < durationMillis) {
                return
            }

            LogUtils.d(TAG, "Unlock grace period expired for $packageName. Clearing timestamp.")
            AppLockManager.appUnlockTimes.remove(packageName)
        }

        if (AppLockManager.isLockScreenShown.get() || AppLockManager.currentBiometricState.toString() == biometricAuthStarted) {
            LogUtils.d(TAG, "Lock screen already shown or biometric auth in progress, skipping")
            return
        }

        LogUtils.d(TAG, "Locked app: $packageName. Showing overlay.")
        AppLockManager.isLockScreenShown.set(true)

        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_FROM_BACKGROUND or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("locked_package", packageName)
            putExtra("triggering_package", triggeringPackage)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting overlay for: $packageName", e)
            AppLockManager.isLockScreenShown.set(false)
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createNotification()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            determineForegroundServiceType()
        } else 0

        if (type != 0) {
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun determineForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val dpm: DevicePolicyManager? = getSystemService()
            val component = ComponentName(this, DeviceAdmin::class.java)

            return if (dpm?.isAdminActive(component) == true) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        }
        return 0
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "AppLock Service (Usage Stats)",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Lock")
            .setContentText("Protecting your apps")
            .setSmallIcon(R.drawable.baseline_shield_24)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}
