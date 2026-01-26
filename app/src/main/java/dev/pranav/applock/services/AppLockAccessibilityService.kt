package dev.pranav.applock.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.getSystemService
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.enableAccessibilityServiceWithShizuku
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.features.lockscreen.ui.PasswordOverlayActivity
import dev.pranav.applock.services.AppLockConstants.ACCESSIBILITY_SETTINGS_CLASSES
import dev.pranav.applock.services.AppLockConstants.ADMIN_CONFIG_CLASSES
import dev.pranav.applock.services.AppLockConstants.EXCLUDED_APPS
import rikka.shizuku.Shizuku

@SuppressLint("AccessibilityPolicy")
class AppLockAccessibilityService : AccessibilityService() {
    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }
    private val keyboardPackages: List<String> by lazy { getKeyboardPackageNames() }

    private var recentsOpen = false
    private var lastForegroundPackage = ""

    enum class BiometricState {
        IDLE, AUTH_STARTED
    }

    companion object {
        private const val TAG = "AppLockAccessibility"
        private const val DEVICE_ADMIN_SETTINGS_PACKAGE = "com.android.settings"
        private const val APP_PACKAGE_PREFIX = "dev.pranav.applock"

        @Volatile
        var isServiceRunning = false
    }

    private val screenStateReceiver = object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            try {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    Log.d(TAG, "Screen off detected. Resetting AppLock state.")
                    AppLockManager.isLockScreenShown.set(false)
                    AppLockManager.clearTemporarilyUnlockedApp()
                    // Optional: Clear all unlock timestamps to force re-lock on next unlock
                    AppLockManager.appUnlockTimes.clear()
                } else if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    Log.d(TAG, "User present.")
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

            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        } catch (e: Exception) {
            logError("Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                packageNames = null
            }

            Log.d(TAG, "Accessibility service connected")
            AppLockManager.resetRestartAttempts(TAG)
            appLockRepository.setActiveBackend(BackendImplementation.ACCESSIBILITY)
        } catch (e: Exception) {
            logError("Error in onServiceConnected", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            handleAccessibilityEvent(event)
        } catch (e: Exception) {
            logError("Unhandled error in onAccessibilityEvent", e)
        }
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        // Check for device admin deactivation (anti-uninstall feature)
        if (appLockRepository.isAntiUninstallEnabled() &&
            event.packageName == DEVICE_ADMIN_SETTINGS_PACKAGE
        ) {
            try {
                checkForDeviceAdminDeactivation(event)
            } catch (e: Exception) {
                logError("Error checking device admin deactivation", e)
            }
        }

        // Early return if protection is disabled or service is not running
        if (!appLockRepository.isProtectEnabled() || !isServiceRunning) {
            return
        }

        // Handle window state changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                handleWindowStateChanged(event)
            } catch (e: Exception) {
                logError("Error handling window state change", e)
                return
            }
        }

        // Skip processing if recents are open
        if (recentsOpen) {
            Log.d(TAG, "Recents opened, ignoring accessibility event")
            return
        }

        // Extract and validate package name
        val packageName = event.packageName?.toString() ?: return

        // Skip if device is locked or app is excluded
        if (!isValidPackageForLocking(packageName)) {
            return
        }

        try {
            processPackageLocking(packageName, event)
        } catch (e: Exception) {
            logError("Error processing package locking for $packageName", e)
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val isRecentlyOpened = isRecentlyOpened(event)
        val isHomeScreen = isHomeScreen(event)

        when {
            isRecentlyOpened -> {
                Log.d(TAG, "Entering recents")
                recentsOpen = true
            }

            isHomeScreenTransition(event) && recentsOpen -> {
                Log.d(TAG, "Transitioning to home screen from recents")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded()
            }

            isHomeScreen -> {
                Log.d(TAG, "On home screen")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded()
            }

            isAppSwitchedFromRecents(event) -> {
                Log.d(TAG, "App switched from recents")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded(event.packageName?.toString())
            }
        }
    }

    private fun isRecentlyOpened(event: AccessibilityEvent): Boolean {
        return (event.packageName == getSystemDefaultLauncherPackageName() &&
                event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED) ||
                (event.text.toString().lowercase().contains("recent apps"))
    }

    private fun isHomeScreen(event: AccessibilityEvent): Boolean {
        return event.packageName == getSystemDefaultLauncherPackageName() &&
                event.className == "com.android.launcher3.uioverrides.QuickstepLauncher" &&
                event.text.toString().lowercase().contains("home screen")
    }

    private fun isHomeScreenTransition(event: AccessibilityEvent): Boolean {
        return event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED &&
                event.packageName == getSystemDefaultLauncherPackageName()
    }

    private fun isAppSwitchedFromRecents(event: AccessibilityEvent): Boolean {
        return event.packageName != getSystemDefaultLauncherPackageName() && recentsOpen
    }

    private fun clearTemporarilyUnlockedAppIfNeeded(newPackage: String? = null) {
        val shouldClear = newPackage == null ||
                (newPackage != AppLockManager.temporarilyUnlockedApp &&
                        newPackage !in appLockRepository.getTriggerExcludedApps())

        if (shouldClear) {
            Log.d(TAG, "Clearing temporarily unlocked app")
            AppLockManager.clearTemporarilyUnlockedApp()
        }
    }

    private fun isValidPackageForLocking(packageName: String): Boolean {
        // Check if device is locked
        if (applicationContext.isDeviceLocked()) {
            AppLockManager.appUnlockTimes.clear()
            AppLockManager.clearTemporarilyUnlockedApp()
            return false
        }

        // Check if accessibility should handle locking
        if (!shouldAccessibilityHandleLocking()) {
            return false
        }

        // Skip excluded packages
        if (packageName == APP_PACKAGE_PREFIX ||
            packageName in keyboardPackages ||
            packageName in EXCLUDED_APPS
        ) {
            return false
        }

        // Skip known recents classes
        return true
    }

    private fun processPackageLocking(packageName: String, event: AccessibilityEvent) {
        // Check for grace period first
        if (AppLockManager.checkAndRestoreRecentlyLeftApp(packageName)) {
            // If restored, we don't need to do anything else, it's efficiently unlocked
            // But we continue to letting the logic flow, checkAndLockApp will handle "temporarilyUnlocked" check
        }

        val currentForegroundPackage = packageName
        val triggeringPackage = lastForegroundPackage
        lastForegroundPackage = currentForegroundPackage

        // Skip if triggering package is excluded
        if (triggeringPackage in appLockRepository.getTriggerExcludedApps()) {
            return
        }

        // Fix for "Lock Immediately" not working when switching between apps
        val unlockedApp = AppLockManager.temporarilyUnlockedApp
        if (unlockedApp.isNotEmpty() &&
            unlockedApp != currentForegroundPackage &&
            currentForegroundPackage !in appLockRepository.getTriggerExcludedApps()
        ) {
            Log.d(
                TAG,
                "Switched from unlocked app $unlockedApp to $currentForegroundPackage. Setting grace period tracking."
            )
            AppLockManager.setRecentlyLeftApp(unlockedApp)
            AppLockManager.clearTemporarilyUnlockedApp()
        }

        Log.d(TAG, event.toString())
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
        // Return early if lock screen is already shown or biometric auth is in progress
        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == BiometricState.AUTH_STARTED
        ) {
            return
        }

        // Return if package is not locked
        if (packageName !in appLockRepository.getLockedApps()) {
            return
        }

        // Return if app is temporarily unlocked
        if (AppLockManager.isAppTemporarilyUnlocked(packageName)) {
            return
        }

        AppLockManager.clearTemporarilyUnlockedApp()

        val unlockDurationMinutes = appLockRepository.getUnlockTimeDuration()
        val unlockTimestamp = AppLockManager.appUnlockTimes[packageName] ?: 0L

        Log.d(
            TAG,
            "checkAndLockApp: pkg=$packageName, duration=$unlockDurationMinutes min, unlockTime=$unlockTimestamp, currentTime=$currentTime, isLockScreenShown=${AppLockManager.isLockScreenShown.get()}"
        )

        if (unlockDurationMinutes > 0 && unlockTimestamp > 0) {
            if (unlockDurationMinutes >= 10_000) {
                return
            }

            val durationMillis = unlockDurationMinutes.toLong() * 60L * 1000L

            val elapsedMillis = currentTime - unlockTimestamp

            Log.d(
                TAG,
                "Grace period check: elapsed=${elapsedMillis}ms (${elapsedMillis / 1000}s), duration=${durationMillis}ms (${durationMillis / 1000}s)"
            )

            if (elapsedMillis < durationMillis) {
                return
            }

            Log.d(TAG, "Unlock grace period expired for $packageName. Clearing timestamp.")
            AppLockManager.appUnlockTimes.remove(packageName)
            AppLockManager.clearTemporarilyUnlockedApp()
        }

        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == BiometricState.AUTH_STARTED
        ) {
            Log.d(TAG, "Lock screen already shown or biometric auth in progress, skipping")
            return
        }

        showLockScreenOverlay(packageName, triggeringPackage)
    }

    private fun showLockScreenOverlay(packageName: String, triggeringPackage: String) {
        Log.d(TAG, "Locked app detected: $packageName. Showing overlay.")
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

    private fun checkForDeviceAdminDeactivation(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return

        Log.d(TAG, "Checking for device admin deactivation for event: $event")

        // Check if user is trying to deactivate the accessibility service
        if (isDeactivationAttempt(event)) {
            Log.d(TAG, "Blocking accessibility service deactivation")
            blockDeactivationAttempt()
            return
        }

        // Check if on device admin page and our app is visible
        val isDeviceAdminPage = isDeviceAdminPage(event)
        val isOurAppVisible = findNodeWithTextContaining(rootNode, "App Lock") != null ||
                findNodeWithTextContaining(rootNode, "AppLock") != null

        if (!isDeviceAdminPage || !isOurAppVisible) {
            return
        }

        blockDeviceAdminDeactivation()
    }

    private fun isDeactivationAttempt(event: AccessibilityEvent): Boolean {
        val isAccessibilitySettings = event.className in ACCESSIBILITY_SETTINGS_CLASSES &&
                event.text.any { it.contains("App Lock") }
        val isSubSettings = event.className == "com.android.settings.SubSettings" &&
                event.text.any { it.contains("App Lock") }
        val isAlertDialog = event.className == "android.app.AlertDialog" &&
                event.text.isNotEmpty() && event.text.first().contains("App Lock")

        return isAccessibilitySettings || isSubSettings || isAlertDialog
    }

    private fun blockDeactivationAttempt() {
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } catch (e: Exception) {
            logError("Error blocking deactivation attempt", e)
        }
    }

    private fun isDeviceAdminPage(event: AccessibilityEvent): Boolean {
        val hasDeviceAdminDescription = event.contentDescription?.contains("Device admin app") == true &&
                event.className == "android.widget.FrameLayout"
        val isAdminConfigClass = event.className in ADMIN_CONFIG_CLASSES

        return hasDeviceAdminDescription || isAdminConfigClass
    }

    private fun blockDeviceAdminDeactivation() {
        try {
            val dpm: DevicePolicyManager? = getSystemService()
            val component = ComponentName(this, DeviceAdmin::class.java)

            if (dpm?.isAdminActive(component) == true) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                Thread.sleep(100)
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Toast.makeText(
                    this,
                    "Disable anti-uninstall from AppLock settings to remove this restriction.",
                    Toast.LENGTH_LONG
                ).show()
                Log.w(TAG, "Blocked device admin deactivation attempt.")
            }
        } catch (e: Exception) {
            logError("Error blocking device admin deactivation", e)
        }
    }

    private fun findNodeWithTextContaining(
        node: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        return try {
            if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
                return node
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val result = findNodeWithTextContaining(child, text)
                if (result != null) return result
            }
            null
        } catch (e: Exception) {
            logError("Error finding node with text: $text", e)
            null
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

    fun getSystemDefaultLauncherPackageName(): String {
        return try {
            val packageManager = packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            val systemLauncher = resolveInfoList.find { resolveInfo ->
                val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isOurApp = resolveInfo.activityInfo.packageName == packageName

                isSystemApp && !isOurApp
            }

            systemLauncher?.activityInfo?.packageName?.also {
                if (it.isEmpty()) {
                    Log.w(TAG, "Could not find a clear system launcher package name.")
                }
            } ?: ""
        } catch (e: Exception) {
            logError("Error getting system default launcher package", e)
            ""
        }
    }

    private fun startPrimaryBackendService() {
        try {
            AppLockManager.stopAllOtherServices(this, AppLockAccessibilityService::class.java)

            when (appLockRepository.getBackendImplementation()) {
                BackendImplementation.SHIZUKU -> {
                    Log.d(TAG, "Starting Shizuku service as primary backend")
                    startService(Intent(this, ShizukuAppLockService::class.java))
                }

                BackendImplementation.USAGE_STATS -> {
                    Log.d(TAG, "Starting Experimental service as primary backend")
                    startService(Intent(this, ExperimentalAppLockService::class.java))
                }

                else -> {
                    Log.d(TAG, "Accessibility service is the primary backend.")
                }
            }
        } catch (e: Exception) {
            logError("Error starting primary backend service", e)
        }
    }

    override fun onInterrupt() {
        try {
            Log.d(TAG, "Accessibility service interrupted")
        } catch (e: Exception) {
            logError("Error in onInterrupt", e)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return try {
            Log.d(TAG, "Accessibility service unbound")
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
            Log.d(TAG, "Accessibility service destroyed")

            try {
                unregisterReceiver(screenStateReceiver)
            } catch (e: IllegalArgumentException) {
                // Ignore if not registered
                Log.w(TAG, "Receiver not registered or already unregistered")
            }

            AppLockManager.isLockScreenShown.set(false)
            AppLockManager.startFallbackServices(this, AppLockAccessibilityService::class.java)
        } catch (e: Exception) {
            logError("Error in onDestroy", e)
        }
    }

    /**
     * Logs errors silently without crashing the service.
     * Only logs to debug level to avoid unnecessary noise in production.
     */
    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
