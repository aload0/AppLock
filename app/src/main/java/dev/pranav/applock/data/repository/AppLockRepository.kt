package dev.pranav.applock.data.repository

import android.content.Context
import dev.pranav.applock.data.manager.BackendServiceManager
import kotlinx.coroutines.flow.Flow

/**
 * Main repository that coordinates between different specialized repositories and managers.
 * Provides a unified interface for all app lock functionality.
 */
class AppLockRepository(private val context: Context) {

    private val preferencesRepository = PreferencesRepository(context)
    private val lockedAppsRepository = LockedAppsRepository(context)
    private val backendServiceManager = BackendServiceManager()

    fun getLockedApps(): Set<String> = lockedAppsRepository.getLockedApps()
    fun addLockedApp(packageName: String) = lockedAppsRepository.addLockedApp(packageName)
    fun addMultipleLockedApps(packageNames: Set<String>) =
        lockedAppsRepository.addMultipleLockedApps(packageNames)
    fun removeLockedApp(packageName: String) = lockedAppsRepository.removeLockedApp(packageName)
    fun isAppLocked(packageName: String): Boolean = lockedAppsRepository.isAppLocked(packageName)

    fun getTriggerExcludedApps(): Set<String> = lockedAppsRepository.getTriggerExcludedApps()
    fun addTriggerExcludedApp(packageName: String) =
        lockedAppsRepository.addTriggerExcludedApp(packageName)

    fun removeTriggerExcludedApp(packageName: String) =
        lockedAppsRepository.removeTriggerExcludedApp(packageName)

    fun isAppTriggerExcluded(packageName: String): Boolean =
        lockedAppsRepository.isAppTriggerExcluded(packageName)

    fun getAntiUninstallApps(): Set<String> = lockedAppsRepository.getAntiUninstallApps()
    fun addAntiUninstallApp(packageName: String) =
        lockedAppsRepository.addAntiUninstallApp(packageName)

    fun removeAntiUninstallApp(packageName: String) =
        lockedAppsRepository.removeAntiUninstallApp(packageName)

    fun isAppAntiUninstall(packageName: String): Boolean =
        lockedAppsRepository.isAppAntiUninstall(packageName)

    fun getPassword(): String? = preferencesRepository.getPassword()
    fun setPassword(password: String) = preferencesRepository.setPassword(password)
    fun validatePassword(inputPassword: String): Boolean =
        preferencesRepository.validatePassword(inputPassword)

    fun getPattern(): String? = preferencesRepository.getPattern()
    fun setPattern(pattern: String) = preferencesRepository.setPattern(pattern)
    fun validatePattern(inputPattern: String): Boolean =
        preferencesRepository.validatePattern(inputPattern)

    fun setLockType(lockType: String) = preferencesRepository.setLockType(lockType)
    fun getLockType(): String = preferencesRepository.getLockType()

    fun setBiometricAuthEnabled(enabled: Boolean) =
        preferencesRepository.setBiometricAuthEnabled(enabled)

    fun isBiometricAuthEnabled(): Boolean = preferencesRepository.isBiometricAuthEnabled()

    fun setUseMaxBrightness(enabled: Boolean) = preferencesRepository.setUseMaxBrightness(enabled)
    fun shouldUseMaxBrightness(): Boolean = preferencesRepository.shouldUseMaxBrightness()
    fun setAmoledModeEnabled(enabled: Boolean) = preferencesRepository.setAmoledModeEnabled(enabled)
    fun isAmoledModeEnabled(): Boolean = preferencesRepository.isAmoledModeEnabled()
    fun amoledModeFlow(): Flow<Boolean> = preferencesRepository.amoledModeFlow

    fun setDynamicColorEnabled(enabled: Boolean) = preferencesRepository.setDynamicColorEnabled(enabled)
    fun isDynamicColorEnabled(): Boolean = preferencesRepository.isDynamicColorEnabled()
    fun dynamicColorFlow(): Flow<Boolean> = preferencesRepository.dynamicColorFlow

    fun setAppThemeMode(themeMode: AppThemeMode) = preferencesRepository.setAppThemeMode(themeMode)
    fun getAppThemeMode(): AppThemeMode = preferencesRepository.getAppThemeMode()
    fun appThemeModeFlow(): Flow<AppThemeMode> = preferencesRepository.appThemeModeFlow

    fun setDisableHaptics(enabled: Boolean) = preferencesRepository.setDisableHaptics(enabled)
    fun shouldDisableHaptics(): Boolean = preferencesRepository.shouldDisableHaptics()
    fun setShowSystemApps(enabled: Boolean) = preferencesRepository.setShowSystemApps(enabled)
    fun shouldShowSystemApps(): Boolean = preferencesRepository.shouldShowSystemApps()

    fun setAntiUninstallEnabled(enabled: Boolean) =
        preferencesRepository.setAntiUninstallEnabled(enabled)

    fun isAntiUninstallEnabled(): Boolean = preferencesRepository.isAntiUninstallEnabled()

    fun setAntiUninstallAdminSettingsEnabled(enabled: Boolean) =
        preferencesRepository.setAntiUninstallAdminSettingsEnabled(enabled)

    fun isAntiUninstallAdminSettingsEnabled(): Boolean =
        preferencesRepository.isAntiUninstallAdminSettingsEnabled()

    fun setAntiUninstallUsageStatsEnabled(enabled: Boolean) =
        preferencesRepository.setAntiUninstallUsageStatsEnabled(enabled)

    fun isAntiUninstallUsageStatsEnabled(): Boolean =
        preferencesRepository.isAntiUninstallUsageStatsEnabled()

    fun setAntiUninstallAccessibilityEnabled(enabled: Boolean) =
        preferencesRepository.setAntiUninstallAccessibilityEnabled(enabled)

    fun isAntiUninstallAccessibilityEnabled(): Boolean =
        preferencesRepository.isAntiUninstallAccessibilityEnabled()

    fun setAntiUninstallOverlayEnabled(enabled: Boolean) =
        preferencesRepository.setAntiUninstallOverlayEnabled(enabled)

    fun isAntiUninstallOverlayEnabled(): Boolean =
        preferencesRepository.isAntiUninstallOverlayEnabled()

    fun setProtectEnabled(enabled: Boolean) = preferencesRepository.setProtectEnabled(enabled)
    fun isProtectEnabled(): Boolean = preferencesRepository.isProtectEnabled()

    fun setUnlockTimeDuration(minutes: Int) = preferencesRepository.setUnlockTimeDuration(minutes)
    fun getUnlockTimeDuration(): Int = preferencesRepository.getUnlockTimeDuration()
    fun setAutoUnlockEnabled(enabled: Boolean) = preferencesRepository.setAutoUnlockEnabled(enabled)
    fun isAutoUnlockEnabled(): Boolean = preferencesRepository.isAutoUnlockEnabled()

    fun setBackendImplementation(backend: BackendImplementation) =
        preferencesRepository.setBackendImplementation(backend)

    fun getBackendImplementation(): BackendImplementation =
        preferencesRepository.getBackendImplementation()

    fun isShowCommunityLink(): Boolean = preferencesRepository.isShowCommunityLink()
    fun setCommunityLinkShown(shown: Boolean) = preferencesRepository.setCommunityLinkShown(shown)
    fun isShowDonateLink(): Boolean = preferencesRepository.isShowDonateLink(context)
    fun setShowDonateLink(show: Boolean) = preferencesRepository.setShowDonateLink(context, show)

    fun isLoggingEnabled(): Boolean = preferencesRepository.isLoggingEnabled()
    fun setLoggingEnabled(enabled: Boolean) = preferencesRepository.setLoggingEnabled(enabled)

    fun setIntruderSelfieEnabled(enabled: Boolean) = preferencesRepository.setIntruderSelfieEnabled(enabled)
    fun isIntruderSelfieEnabled(): Boolean = preferencesRepository.isIntruderSelfieEnabled()
    fun setIntruderSelfieAttempts(attempts: Int) = preferencesRepository.setIntruderSelfieAttempts(attempts)
    fun getIntruderSelfieAttempts(): Int = preferencesRepository.getIntruderSelfieAttempts()

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
