package dev.pranav.applock.data.repository

import android.content.Context
import dev.pranav.applock.data.manager.BackendServiceManager

/**
 * Main repository that coordinates between different specialized repositories and managers.
 * Provides a unified interface for all app lock functionality.
 * Updated to include system settings restriction methods.
 */
class AppLockRepository(private val context: Context) {

    private val preferencesRepository = PreferencesRepository(context)
    private val lockedAppsRepository = LockedAppsRepository(context)
    private val backendServiceManager = BackendServiceManager()

    // ============= LOCKED APPS =============
    fun getLockedApps(): Set<String> = lockedAppsRepository.getLockedApps()
    fun addLockedApp(packageName: String) = lockedAppsRepository.addLockedApp(packageName)
    fun addMultipleLockedApps(packageNames: Set<String>) =
        lockedAppsRepository.addMultipleLockedApps(packageNames)
    fun removeLockedApp(packageName: String) = lockedAppsRepository.removeLockedApp(packageName)
    fun isAppLocked(packageName: String): Boolean = lockedAppsRepository.isAppLocked(packageName)

    // ============= TRIGGER EXCLUSIONS =============
    fun getTriggerExcludedApps(): Set<String> = lockedAppsRepository.getTriggerExcludedApps()
    fun addTriggerExcludedApp(packageName: String) =
        lockedAppsRepository.addTriggerExcludedApp(packageName)

    fun removeTriggerExcludedApp(packageName: String) =
        lockedAppsRepository.removeTriggerExcludedApp(packageName)

    fun isAppTriggerExcluded(packageName: String): Boolean =
        lockedAppsRepository.isAppTriggerExcluded(packageName)

    // ============= ANTI-UNINSTALL APPS =============
    fun getAntiUninstallApps(): Set<String> = lockedAppsRepository.getAntiUninstallApps()
    fun addAntiUninstallApp(packageName: String) =
        lockedAppsRepository.addAntiUninstallApp(packageName)

    fun removeAntiUninstallApp(packageName: String) =
        lockedAppsRepository.removeAntiUninstallApp(packageName)

    fun isAppAntiUninstall(packageName: String): Boolean =
        lockedAppsRepository.isAppAntiUninstall(packageName)

    // ============= AUTHENTICATION =============
    fun getPassword(): String? = preferencesRepository.getPassword()
    fun setPassword(password: String) = preferencesRepository.setPassword(password)
    fun validatePassword(inputPassword: String): Boolean =
        preferencesRepository.validatePassword(inputPassword)

    fun getPattern(): String? = preferencesRepository.getPattern()
    fun setPattern(pattern: String) = preferencesRepository.setPattern(pattern)
    fun validatePattern(inputPattern: String): Boolean =
        preferencesRepository.validatePattern(inputPattern)

    // ============= LOCK TYPE & BIOMETRIC =============
    fun setLockType(lockType: String) = preferencesRepository.setLockType(lockType)
    fun getLockType(): String = preferencesRepository.getLockType()

    fun setBiometricAuthEnabled(enabled: Boolean) =
        preferencesRepository.setBiometricAuthEnabled(enabled)

    fun isBiometricAuthEnabled(): Boolean = preferencesRepository.isBiometricAuthEnabled()

    // ============= DISPLAY SETTINGS =============
    fun setUseMaxBrightness(enabled: Boolean) = preferencesRepository.setUseMaxBrightness(enabled)
    fun shouldUseMaxBrightness(): Boolean = preferencesRepository.shouldUseMaxBrightness()
    fun setDisableHaptics(enabled: Boolean) = preferencesRepository.setDisableHaptics(enabled)
    fun shouldDisableHaptics(): Boolean = preferencesRepository.shouldDisableHaptics()
    fun setShowSystemApps(enabled: Boolean) = preferencesRepository.setShowSystemApps(enabled)
    fun shouldShowSystemApps(): Boolean = preferencesRepository.shouldShowSystemApps()

    // ============= ANTI-UNINSTALL & PROTECTION =============
    fun setAntiUninstallEnabled(enabled: Boolean) {
        preferencesRepository.setAntiUninstallEnabled(enabled)
        // When disabling Anti-Uninstall, also disable all system settings restrictions
        if (!enabled) {
            preferencesRepository.disableAllSystemSettingsRestrictions()
        }
    }

    fun isAntiUninstallEnabled(): Boolean = preferencesRepository.isAntiUninstallEnabled()
    fun setProtectEnabled(enabled: Boolean) = preferencesRepository.setProtectEnabled(enabled)
    fun isProtectEnabled(): Boolean = preferencesRepository.isProtectEnabled()

    // ============= SYSTEM SETTINGS RESTRICTIONS (NEW) =============
    // NEW: Restrict Draw Over Other Apps Settings Page
    fun setRestrictDrawOverAppsSettings(enabled: Boolean) =
        preferencesRepository.setRestrictDrawOverAppsSettings(enabled)

    fun isRestrictDrawOverAppsSettings(): Boolean =
        preferencesRepository.isRestrictDrawOverAppsSettings()

    // NEW: Restrict Usage Access Settings Page
    fun setRestrictUsageAccessSettings(enabled: Boolean) =
        preferencesRepository.setRestrictUsageAccessSettings(enabled)

    fun isRestrictUsageAccessSettings(): Boolean =
        preferencesRepository.isRestrictUsageAccessSettings()

    // NEW: Restrict Accessibility Settings Page
    fun setRestrictAccessibilitySettings(enabled: Boolean) =
        preferencesRepository.setRestrictAccessibilitySettings(enabled)

    fun isRestrictAccessibilitySettings(): Boolean =
        preferencesRepository.isRestrictAccessibilitySettings()

    // NEW: Restrict Device Administrator Settings Page
    fun setRestrictDeviceAdminSettings(enabled: Boolean) =
        preferencesRepository.setRestrictDeviceAdminSettings(enabled)

    fun isRestrictDeviceAdminSettings(): Boolean =
        preferencesRepository.isRestrictDeviceAdminSettings()

    // NEW: Require Unrestricted Battery Usage
    fun setRequireUnrestrictedBattery(enabled: Boolean) =
        preferencesRepository.setRequireUnrestrictedBattery(enabled)

    fun isRequireUnrestrictedBattery(): Boolean =
        preferencesRepository.isRequireUnrestrictedBattery()

    // NEW: Check if any system settings restriction is enabled
    fun hasAnySystemSettingsRestriction(): Boolean {
        return isRestrictDrawOverAppsSettings() ||
                isRestrictUsageAccessSettings() ||
                isRestrictAccessibilitySettings() ||
                isRestrictDeviceAdminSettings() ||
                isRequireUnrestrictedBattery()
    }

    // ============= UNLOCK DURATION & AUTO-UNLOCK =============
    fun setUnlockTimeDuration(minutes: Int) = preferencesRepository.setUnlockTimeDuration(minutes)
    fun getUnlockTimeDuration(): Int = preferencesRepository.getUnlockTimeDuration()
    fun setAutoUnlockEnabled(enabled: Boolean) = preferencesRepository.setAutoUnlockEnabled(enabled)
    fun isAutoUnlockEnabled(): Boolean = preferencesRepository.isAutoUnlockEnabled()

    // ============= BACKEND IMPLEMENTATION =============
    fun setBackendImplementation(backend: BackendImplementation) =
        preferencesRepository.setBackendImplementation(backend)

    fun getBackendImplementation(): BackendImplementation =
        preferencesRepository.getBackendImplementation()

    // ============= LINKS =============
    fun isShowCommunityLink(): Boolean = preferencesRepository.isShowCommunityLink()
    fun setCommunityLinkShown(shown: Boolean) = preferencesRepository.setCommunityLinkShown(shown)
    fun isShowDonateLink(): Boolean = preferencesRepository.isShowDonateLink()
    fun setShowDonateLink(show: Boolean) = preferencesRepository.setShowDonateLink(show)
    
    // Overloaded versions for context parameter (for backward compatibility)
    fun isShowDonateLink(context: Context): Boolean = preferencesRepository.isShowDonateLink(context)
    fun setShowDonateLink(context: Context, show: Boolean) = preferencesRepository.setShowDonateLink(context, show)

    // ============= LOGGING =============
    fun isLoggingEnabled(): Boolean = preferencesRepository.isLoggingEnabled()
    fun setLoggingEnabled(enabled: Boolean) = preferencesRepository.setLoggingEnabled(enabled)

    fun setActiveBackend(backend: BackendImplementation) =
        backendServiceManager.setActiveBackend(backend)

    companion object {
        private const val TAG = "AppLockRepository"

        fun shouldStartService(repository: AppLockRepository, serviceClass: Class<*>): Boolean {
            return repository.backendServiceManager.shouldStartService(
                serviceClass,
                repository.getBackendImplementation()
            )
        }
    }
}

enum class BackendImplementation {
    ACCESSIBILITY,
    USAGE_STATS,
    SHIZUKU
}
