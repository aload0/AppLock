package dev.pranav.applock.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.systemSettingsRestrictionManager
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.features.lockscreen.ui.PasswordOverlayActivity

@SuppressLint("AccessibilityPolicy")
class AppLockAccessibilityService : AccessibilityService() {

    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }

    private val keyboardPackages: List<String> by lazy {
        getKeyboardPackageNames()
    }

    // NEW: System Settings Restriction Manager
    private val restrictionManager by lazy { 
        applicationContext.systemSettingsRestrictionManager() 
    }

    private var lastForegroundPackage = ""

    companion object {

        private const val TAG = "AppLockAccessibility"

        @Volatile
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()

        isServiceRunning = true

        AppLockManager.isLockScreenShown.set(false)

        startPrimaryBackendService()
    }

    override fun onServiceConnected() {

        serviceInfo = serviceInfo.apply {

            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            packageNames = null
        }

        Log.d(TAG, "Accessibility service connected")

        appLockRepository.setActiveBackend(BackendImplementation.ACCESSIBILITY)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        try {

            handleAccessibilityEvent(event)

        } catch (e: Exception) {

            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent) {

        if (!appLockRepository.isProtectEnabled()) return

        val packageName = event.packageName?.toString() ?: return

        val className = event.className?.toString()

        /* ---------------------------------------------------------
           🔒 NEW: Restrict System Settings Access (Anti-Uninstall)
        ---------------------------------------------------------- */

        if (isSettingsPackage(packageName)) {
            handleSettingsPackageOpening(event)
        }

        /* ---------------------------------------------------------
           🔒 ORIGINAL: Restrict System Settings Access
        ---------------------------------------------------------- */

        if (isRestrictedSettings(packageName, className)) {

            LogUtils.d(TAG, "Blocked restricted settings page")

            performGlobalAction(GLOBAL_ACTION_BACK)

            showSecurityLock()

            return
        }

        if (!isValidPackage(packageName)) return

        processPackageLocking(packageName)
    }

    /* ---------------------------------------------------------
       NEW METHODS: SYSTEM SETTINGS RESTRICTION (Anti-Uninstall)
    ---------------------------------------------------------- */

    /**
     * Determine if a package is a system settings app.
     */
    private fun isSettingsPackage(packageName: String): Boolean {
        return packageName in listOf(
            "com.android.settings",
            "com.sec.android.app.personalpage",
            "com.oppo.safe",
            "com.vivo.settings",
            "com.huawei.systemmanager",
            "com.xiaomi.misettings"
        )
    }

    /**
     * Handle when settings package is being opened.
     * Check if it's trying to access a restricted settings page.
     */
    private fun handleSettingsPackageOpening(event: AccessibilityEvent) {
        try {
            val appLockRepo = applicationContext.appLockRepository()
            
            if (!appLockRepo.isAntiUninstallEnabled()) {
                return
            }

            if (!appLockRepo.hasAnySystemSettingsRestriction()) {
                return
            }

            val sourceNode = event.source ?: return
            
            if (detectRestrictedSettingsActivity(sourceNode, appLockRepo)) {
                showLockScreenForRestrictedSettings()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            sourceNode.recycle()
        } catch (e: Exception) {
            LogUtils.logError("Error handling settings package opening", e)
        }
    }

    /**
     * Detect if a restricted settings activity is being accessed.
     */
    private fun detectRestrictedSettingsActivity(
        sourceNode: AccessibilityNodeInfo,
        repository: AppLockRepository
    ): Boolean {
        try {
            val text = sourceNode.text?.toString() ?: ""
            val contentDescription = sourceNode.contentDescription?.toString() ?: ""
            
            return when {
                repository.isRestrictDrawOverAppsSettings() &&
                    (text.contains("overlay", ignoreCase = true) || 
                     text.contains("draw", ignoreCase = true)) -> {
                    LogUtils.d(TAG, "Detected restricted Draw Over Apps settings")
                    true
                }
                
                repository.isRestrictUsageAccessSettings() &&
                    (text.contains("usage", ignoreCase = true) || 
                     text.contains("data usage", ignoreCase = true)) -> {
                    LogUtils.d(TAG, "Detected restricted Usage Access settings")
                    true
                }
                
                repository.isRestrictAccessibilitySettings() &&
                    (text.contains("accessibility", ignoreCase = true) || 
                     contentDescription.contains("accessibility", ignoreCase = true)) -> {
                    LogUtils.d(TAG, "Detected restricted Accessibility settings")
                    true
                }
                
                repository.isRestrictDeviceAdminSettings() &&
                    (text.contains("admin", ignoreCase = true) || 
                     text.contains("device administrator", ignoreCase = true)) -> {
                    LogUtils.d(TAG, "Detected restricted Device Admin settings")
                    true
                }
                
                repository.isRequireUnrestrictedBattery() &&
                    (text.contains("battery", ignoreCase = true) || 
                     text.contains("power saving", ignoreCase = true)) -> {
                    LogUtils.d(TAG, "Detected restricted Battery Optimization settings")
                    true
                }
                
                else -> false
            }
        } catch (e: Exception) {
            LogUtils.logError("Error detecting restricted settings activity", e)
            return false
        }
    }

    /**
     * Create and show the lock screen when user tries to access restricted settings.
     */
    private fun showLockScreenForRestrictedSettings() {
        try {
            if (AppLockManager.isLockScreenShown.get()) return

            AppLockManager.isLockScreenShown.set(true)

            val lockScreenIntent = Intent(applicationContext, PasswordOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("isRestrictedSettings", true)
                putExtra("locked_package", "com.android.settings")
            }
            applicationContext.startActivity(lockScreenIntent)

            LogUtils.d(TAG, "Showed lock screen for restricted settings")
        } catch (e: Exception) {
            LogUtils.logError("Failed to show lock screen for restricted settings", e)
        }
    }

    /**
     * Optional: Called from broadcast receiver if needed
     */
    protected open fun onSettingsIntentIntercepted(action: String) {
        try {
            val restrictMgr = applicationContext.systemSettingsRestrictionManager()
            val intent = Intent(action)
            
            if (restrictMgr.isIntentRestricted(intent)) {
                showLockScreenForRestrictedSettings()
                LogUtils.d(TAG, "Settings intent intercepted for action: $action")
            }
        } catch (e: Exception) {
            LogUtils.logError("Error in onSettingsIntentIntercepted", e)
        }
    }

    /* ---------------------------------------------------------
       ORIGINAL METHODS: SETTINGS PROTECTION
    ---------------------------------------------------------- */

    private fun isRestrictedSettings(pkg: String, cls: String?): Boolean {

        val restrictState = appLockRepository.getRestrictSettings()

        if (pkg != "com.android.settings") return false

        if (restrictState.blockOverlaySettings &&
            cls?.contains("Overlay", true) == true
        ) return true

        if (restrictState.blockUsageAccessSettings &&
            cls?.contains("UsageAccess", true) == true
        ) return true

        if (restrictState.blockAccessibilitySettings &&
            cls?.contains("Accessibility", true) == true
        ) return true

        if (restrictState.blockDeviceAdminSettings &&
            cls?.contains("DeviceAdmin", true) == true
        ) return true

        return false
    }

    private fun showSecurityLock() {

        if (AppLockManager.isLockScreenShown.get()) return

        AppLockManager.isLockScreenShown.set(true)

        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            putExtra("locked_package", "com.android.settings")
        }

        startActivity(intent)
    }

    /* ---------------------------------------------------------
       ORIGINAL METHODS: NORMAL APP LOCK LOGIC
    ---------------------------------------------------------- */

    private fun processPackageLocking(packageName: String) {

        val triggeringPackage = lastForegroundPackage

        lastForegroundPackage = packageName

        if (triggeringPackage in appLockRepository.getTriggerExcludedApps()) return

        checkAndLockApp(packageName, triggeringPackage)
    }

    private fun checkAndLockApp(packageName: String, triggeringPackage: String) {

        if (AppLockManager.isLockScreenShown.get()) return

        if (packageName !in appLockRepository.getLockedApps()) return

        if (AppLockManager.isAppTemporarilyUnlocked(packageName)) return

        LogUtils.d(TAG, "Locked app detected: $packageName")

        AppLockManager.isLockScreenShown.set(true)

        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION

            putExtra("locked_package", packageName)

            putExtra("triggering_package", triggeringPackage)
        }

        startActivity(intent)
    }

    /* ---------------------------------------------------------
       ORIGINAL METHODS: VALIDATION
    ---------------------------------------------------------- */

    private fun isValidPackage(packageName: String): Boolean {

        if (applicationContext.isDeviceLocked()) {

            AppLockManager.appUnlockTimes.clear()

            return false
        }

        if (packageName == this.packageName) return false

        if (packageName in keyboardPackages) return false

        if (packageName in AppLockConstants.EXCLUDED_APPS) return false

        return true
    }

    private fun getKeyboardPackageNames(): List<String> {

        return try {

            getSystemService<InputMethodManager>()
                ?.enabledInputMethodList
                ?.map { it.packageName }
                ?: emptyList()

        } catch (e: Exception) {

            emptyList()
        }
    }

    /* ---------------------------------------------------------
       ORIGINAL METHODS: BACKEND CONTROL
    ---------------------------------------------------------- */

    private fun startPrimaryBackendService() {

        try {

            AppLockManager.stopAllOtherServices(this, AppLockAccessibilityService::class.java)

            when (appLockRepository.getBackendImplementation()) {

                BackendImplementation.USAGE_STATS -> {

                    startService(Intent(this, ExperimentalAppLockService::class.java))
                }

                BackendImplementation.SHIZUKU -> {

                    startService(Intent(this, ShizukuAppLockService::class.java))
                }

                else -> {}
            }

        } catch (e: Exception) {

            Log.e(TAG, "Error starting backend service", e)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {

        super.onDestroy()

        isServiceRunning = false

        AppLockManager.startFallbackServices(this, AppLockAccessibilityService::class.java)
    }
}
