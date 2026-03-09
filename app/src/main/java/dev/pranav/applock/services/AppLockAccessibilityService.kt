package dev.pranav.applock.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.enableAccessibilityServiceWithShizuku
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.features.lockscreen.ui.PasswordOverlayActivity
import dev.pranav.applock.services.AppLockConstants.ACCESSIBILITY_SETTINGS_CLASSES
import dev.pranav.applock.services.AppLockConstants.DEVICE_ADMIN_SETTINGS_CLASSES
import dev.pranav.applock.services.AppLockConstants.USAGE_ACCESS_SETTINGS_CLASSES
import dev.pranav.applock.services.AppLockConstants.OVERLAY_SETTINGS_CLASSES
import dev.pranav.applock.services.AppLockConstants.EXCLUDED_APPS
import rikka.shizuku.Shizuku

@SuppressLint("AccessibilityPolicy")
class AppLockAccessibilityService : AccessibilityService() {
    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }
    private val keyboardPackages: List<String> by lazy { getKeyboardPackageNames() }
    private var launcherPackages: Set<String> = emptySet()

    private var recentsOpen = false
    private var lastForegroundPackage = ""

    private val NOTIFICATION_ID = 114
    private val CHANNEL_ID = "AppLockAccessibilityServiceChannel"
    private val notificationManager: NotificationManager by lazy { getSystemService(NotificationManager::class.java)!! }

    enum class BiometricState {
        IDLE, AUTH_STARTED
    }

    companion object {
        private const val TAG = "AppLockAccessibility"
        private const val APP_PACKAGE_PREFIX = "dev.pranav.applock"

        @Volatile
        var isServiceRunning = false
    }

    private val screenStateReceiver = object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            try {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    LogUtils.d(TAG, "Screen off detected. Resetting AppLock state.")
                    AppLockManager.isLockScreenShown.set(false)
                    AppLockManager.clearTemporarilyUnlockedApp()
                    AppLockManager.appUnlockTimes.clear()
                }
            } catch (e: Exception) {
                logError("Error in screenStateReceiver", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            isServiceRunning = true
            AppLockManager.currentBiometricState = BiometricState.IDLE
            AppLockManager.isLockScreenShown.set(false)
            startPrimaryBackendService()
            startForegroundService()
            updateLauncherPackages()

            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        } catch (e: Exception) {
            logError("Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                packageNames = null
            }

            Log.d(TAG, "Accessibility service connected")
            AppLockManager.resetRestartAttempts(TAG)
            appLockRepository.setActiveBackend(BackendImplementation.ACCESSIBILITY)
            startForegroundService()
            updateLauncherPackages()
        } catch (e: Exception) {
            logError("Error in onServiceConnected", e)
        }
    }

    private fun updateLauncherPackages() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            launcherPackages = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
                .toSet()
            Log.d(TAG, "Launcher packages updated: $launcherPackages")
        } catch (e: Exception) {
            logError("Error updating launcher packages", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            // Block Recents button if lock screen is active
            if (AppLockManager.isLockScreenShown.get()) {
                if (isRecentsEvent(event)) {
                    LogUtils.d(TAG, "Blocking Recents access while lock screen is active. Triggering BACK action to stay on overlay.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
            }

            handleAccessibilityEvent(event)
        } catch (e: Exception) {
            logError("Unhandled error in onAccessibilityEvent", e)
        }
    }

    private fun isRecentsEvent(event: AccessibilityEvent): Boolean {
        val packageName = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""
        val text = event.text.toString().lowercase()
        
        if (packageName == applicationContext.packageName) return false

        return className in AppLockConstants.KNOWN_RECENTS_CLASSES ||
               (packageName == "com.android.systemui" && className.contains("recents", ignoreCase = true)) ||
               text.contains("recent apps") || 
               text.contains("overview")
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        if (appLockRepository.isAntiUninstallEnabled()) {
            handleAntiUninstallBlocking(event)
        }

        if (!appLockRepository.isProtectEnabled() || !isServiceRunning) {
            return
        }

        // Only react to major window state changes to avoid triggering on notifications or keyboards
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val eventPackage = event.packageName?.toString() ?: return
            
            // 1. Get the current actual active package from the window manager if possible
            val activePackageName = rootInActiveWindow?.packageName?.toString() ?: eventPackage

            // 2. Detect if we are on any Launcher/Home screen
            if (launcherPackages.contains(eventPackage) || launcherPackages.contains(activePackageName)) {
                val unlockedApp = AppLockManager.temporarilyUnlockedApp
                if (unlockedApp.isNotEmpty()) {
                    // Record that we left this app to handle stray exit events via grace period
                    AppLockManager.setRecentlyLeftApp(unlockedApp)
                    AppLockManager.clearTemporarilyUnlockedApp()
                }
                recentsOpen = false
                lastForegroundPackage = if (launcherPackages.contains(activePackageName)) activePackageName else eventPackage
                return
            }

            // 3. Handle Recents and focus transitions
            handleWindowStateChanged(event)

            if (recentsOpen) {
                return
            }

            // 4. Check if it's a valid app package for locking
            if (!isValidPackageForLocking(eventPackage)) {
                return
            }

            // 5. Process locking logic
            try {
                processPackageLocking(eventPackage)
            } catch (e: Exception) {
                logError("Error processing package locking for $eventPackage", e)
            }
        }
    }

    private fun handleAntiUninstallBlocking(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        val isSettings = packageName.contains("settings") || 
                         packageName.contains("packageinstaller") || 
                         packageName.contains("permissioncontroller") ||
                         packageName == "android"

        if (!isSettings) return

        val className = event.className?.toString() ?: ""
        val root = rootInActiveWindow
        
        if (root != null && (containsTextRecursive(root, "dev.pranav.applock") || containsTextRecursive(root, "APP Lock by AP"))) {
            if (className.contains("AppDetails") || className.contains("InstalledAppDetails") ||
                className.contains("Uninstaller") || className.contains("PackageInstaller") ||
                className.contains("Settings\$AppDetailsActivity")) {
                blockAccess("APP Lock by AP protection is active.")
                return
            }
        }

        if (appLockRepository.isAntiUninstallAdminSettingsEnabled()) {
            if (className in DEVICE_ADMIN_SETTINGS_CLASSES || 
                className.contains("DeviceAdminSettings") || 
                className.contains("DeviceAdminAdd")) {
                blockAccess("Device Admin settings are protected.")
                return
            }
            if (root != null && (containsTextRecursive(root, "Device admin") || containsTextRecursive(root, "Device administrator"))) {
                 if (className.contains("SubSettings") || className.contains("SettingsActivity")) {
                     blockAccess("Device Admin settings are protected.")
                     return
                 }
            }
        }

        if (appLockRepository.isAntiUninstallUsageStatsEnabled()) {
            if (className in USAGE_ACCESS_SETTINGS_CLASSES || 
                className.contains("UsageAccessSettings") || 
                className.contains("UsageStats")) {
                blockAccess("Usage access settings are protected.")
                return
            }
            if (root != null && (containsTextRecursive(root, "Usage access") || containsTextRecursive(root, "Usage stats"))) {
                if (className.contains("SubSettings") || className.contains("SettingsActivity")) {
                    blockAccess("Usage access settings are protected.")
                    return
                }
            }
        }

        if (appLockRepository.isAntiUninstallAccessibilityEnabled()) {
            if (className in ACCESSIBILITY_SETTINGS_CLASSES || 
                className.contains("AccessibilitySettings") || 
                className.contains("AccessibilityServiceWarning")) {
                blockAccess("Accessibility settings are protected.")
                return
            }
            
            val accessibilityKeywords = listOf("Accessibility", "Installed apps", "Downloaded apps", "Installed services", "Downloaded services")
            if (root != null && accessibilityKeywords.any { containsTextRecursive(root, it) }) {
                if (containsTextRecursive(root, "APP Lock by AP")) {
                    blockAccess("Accessibility settings for APP Lock by AP are protected.")
                    return
                }
            }
        }

        if (appLockRepository.isAntiUninstallOverlayEnabled()) {
            if (className in OVERLAY_SETTINGS_CLASSES || 
                className.contains("DrawOverlayDetails") || 
                className.contains("OverlaySettings")) {
                blockAccess("Overlay settings are protected.")
                return
            }
            if (root != null && (containsTextRecursive(root, "Display over other apps") || containsTextRecursive(root, "Appear on top"))) {
                if (className.contains("SubSettings") || className.contains("SettingsActivity") || className.contains("DrawOverlayDetails")) {
                    blockAccess("Overlay settings are protected.")
                    return
                }
            }
        }
    }

    private fun containsTextRecursive(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        
        val nodeText = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        
        if (nodeText.contains(text, ignoreCase = true) || contentDescription.contains(text, ignoreCase = true)) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (containsTextRecursive(child, text)) {
                return true
            }
        }
        return false
    }

    private fun blockAccess(message: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        if (isRecentlyOpened(event)) {
            recentsOpen = true
        } else if (isAppSwitchedFromRecents(event)) {
            recentsOpen = false
            clearTemporarilyUnlockedAppIfNeeded(event.packageName?.toString())
        }
    }

    @SuppressLint("InlinedApi")
    private fun isRecentlyOpened(event: AccessibilityEvent): Boolean {
        val packageName = event.packageName?.toString() ?: ""
        return (launcherPackages.contains(packageName) &&
                event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED) ||
                (event.text.toString().lowercase().contains("recent apps"))
    }

    private fun isAppSwitchedFromRecents(event: AccessibilityEvent): Boolean {
        val packageName = event.packageName?.toString() ?: ""
        return !launcherPackages.contains(packageName) && recentsOpen
    }

    private fun clearTemporarilyUnlockedAppIfNeeded(newPackage: String? = null) {
        val shouldClear = newPackage == null ||
                (newPackage != AppLockManager.temporarilyUnlockedApp &&
                        newPackage !in appLockRepository.getTriggerExcludedApps())

        if (shouldClear) {
            AppLockManager.clearTemporarilyUnlockedApp()
        }
    }

    private fun isValidPackageForLocking(packageName: String): Boolean {
        if (applicationContext.isDeviceLocked()) {
            AppLockManager.appUnlockTimes.clear()
            AppLockManager.clearTemporarilyUnlockedApp()
            return false
        }

        if (!shouldAccessibilityHandleLocking()) {
            return false
        }

        // Don't lock our own app, keyboards, system apps, or launchers
        if (packageName == applicationContext.packageName ||
            packageName in keyboardPackages ||
            packageName in EXCLUDED_APPS ||
            launcherPackages.contains(packageName)
        ) {
            return false
        }

        return true
    }

    private fun processPackageLocking(packageName: String) {
        val currentForegroundPackage = packageName
        
        // Attempt to restore state during transitions (handles stray "exit" events)
        if (AppLockManager.checkAndRestoreRecentlyLeftApp(currentForegroundPackage)) {
            LogUtils.d(TAG, "Restored state for $currentForegroundPackage via grace period")
        }

        val triggeringPackage = lastForegroundPackage
        lastForegroundPackage = currentForegroundPackage

        // "One-way" Entry-based Detection: Only trigger if the package actually changed
        if (currentForegroundPackage == triggeringPackage) {
            return
        }

        if (triggeringPackage in appLockRepository.getTriggerExcludedApps()) {
            return
        }

        val unlockedApp = AppLockManager.temporarilyUnlockedApp
        if (unlockedApp.isNotEmpty() &&
            unlockedApp != currentForegroundPackage &&
            currentForegroundPackage !in appLockRepository.getTriggerExcludedApps()
        ) {
            AppLockManager.setRecentlyLeftApp(unlockedApp)
            AppLockManager.clearTemporarilyUnlockedApp()
        }

        checkAndLockApp(currentForegroundPackage, triggeringPackage, System.currentTimeMillis())
    }

    private fun shouldAccessibilityHandleLocking(): Boolean {
        return when (appLockRepository.getBackendImplementation()) {
            BackendImplementation.ACCESSIBILITY -> true
            BackendImplementation.SHIZUKU -> !applicationContext.isServiceRunning(
                ShizukuAppLockService::class.java
            )

            BackendImplementation.USAGE_STATS -> !applicationContext.isServiceRunning(
                ExperimentalAppLockService::class.java
            )
        }
    }

    private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == BiometricState.AUTH_STARTED
        ) {
            return
        }

        if (packageName !in appLockRepository.getLockedApps()) {
            return
        }

        if (AppLockManager.isAppTemporarilyUnlocked(packageName)) {
            return
        }

        AppLockManager.clearTemporarilyUnlockedApp()

        val unlockDurationMinutes = appLockRepository.getUnlockTimeDuration()
        val unlockTimestamp = AppLockManager.appUnlockTimes[packageName] ?: 0L

        if (unlockDurationMinutes > 0 && unlockTimestamp > 0) {
            if (unlockDurationMinutes >= 10_000) {
                return
            }

            val durationMillis = unlockDurationMinutes.toLong() * 60L * 1000L
            val elapsedMillis = currentTime - unlockTimestamp

            if (elapsedMillis < durationMillis) {
                return
            }

            AppLockManager.appUnlockTimes.remove(packageName)
            AppLockManager.clearTemporarilyUnlockedApp()
        }

        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == BiometricState.AUTH_STARTED
        ) {
            return
        }

        showLockScreenOverlay(packageName, triggeringPackage)
    }

    private fun showLockScreenOverlay(packageName: String, triggeringPackage: String) {
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
            logError("Failed to start password overlay", e)
            AppLockManager.isLockScreenShown.set(false)
        }
    }

    private fun getKeyboardPackageNames(): List<String> {
        return try {
            getSystemService<InputMethodManager>()?.enabledInputMethodList?.map { it.packageName }
                ?: emptyList()
        } catch (e: Exception) {
            logError("Error getting keyboard package names", e)
            emptyList()
        }
    }

    private fun startPrimaryBackendService() {
        try {
            AppLockManager.stopAllOtherServices(this, AppLockAccessibilityService::class.java)

            when (appLockRepository.getBackendImplementation()) {
                BackendImplementation.SHIZUKU -> {
                    startService(Intent(this, ShizukuAppLockService::class.java))
                }

                BackendImplementation.USAGE_STATS -> {
                    startService(Intent(this, ExperimentalAppLockService::class.java))
                }

                else -> {
                }
            }
        } catch (e: Exception) {
            logError("Error starting primary backend service", e)
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = createNotification()

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                determineForegroundServiceType()
            } else 0

            try {
                if (type != 0) {
                    startForeground(NOTIFICATION_ID, notification, type)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }
        }
    }

    private fun determineForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val dpm = getSystemService(DevicePolicyManager::class.java)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "APP Lock by AP Accessibility Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("APP Lock by AP")
            .setContentText("Accessibility service is protecting your apps")
            .setSmallIcon(R.drawable.baseline_shield_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onInterrupt() {
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return try {
            isServiceRunning = false
            AppLockManager.startFallbackServices(this, AppLockAccessibilityService::class.java)

            if (Shizuku.pingBinder() && appLockRepository.isAntiUninstallEnabled()) {
                enableAccessibilityServiceWithShizuku(ComponentName(packageName, javaClass.name))
            }

            super.onUnbind(intent)
        } catch (e: Exception) {
            logError("Error in onUnbind", e)
            super.onUnbind(intent)
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            isServiceRunning = false

            try {
                unregisterReceiver(screenStateReceiver)
            } catch (_: IllegalArgumentException) {
            }

            AppLockManager.isLockScreenShown.set(false)
            AppLockManager.startFallbackServices(this, AppLockAccessibilityService::class.java)
        } catch (e: Exception) {
            logError("Error in onDestroy", e)
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
