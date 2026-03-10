package dev.pranav.applock.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing application preferences and settings.
 * Handles all SharedPreferences operations with proper separation of concerns.
 */
class PreferencesRepository(context: Context) {

    private val appLockPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_APP_LOCK, Context.MODE_PRIVATE)

    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)

    fun setPassword(password: String) {
        appLockPrefs.edit(commit = true) { putString(KEY_PASSWORD, password) }
    }

    fun getPassword(): String? {
        return appLockPrefs.getString(KEY_PASSWORD, null)
    }

    fun validatePassword(inputPassword: String): Boolean {
        val storedPassword = getPassword()
        return storedPassword != null && inputPassword == storedPassword
    }

    fun setPattern(pattern: String) {
        appLockPrefs.edit(commit = true) { putString(KEY_PATTERN, pattern) }
    }

    fun getPattern(): String? {
        return appLockPrefs.getString(KEY_PATTERN, null)
    }

    fun validatePattern(inputPattern: String): Boolean {
        val storedPattern = getPattern()
        return storedPattern != null && inputPattern == storedPattern
    }

    fun setLockType(lockType: String) {
        settingsPrefs.edit(commit = true) { putString(KEY_LOCK_TYPE, lockType) }
    }

    fun getLockType(): String {
        return settingsPrefs.getString(KEY_LOCK_TYPE, LOCK_TYPE_PIN) ?: LOCK_TYPE_PIN
    }

    fun setBiometricAuthEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_BIOMETRIC_AUTH_ENABLED, enabled) }
    }

    fun isBiometricAuthEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_BIOMETRIC_AUTH_ENABLED, false)
    }

    fun setUseMaxBrightness(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_USE_MAX_BRIGHTNESS, enabled) }
    }

    fun shouldUseMaxBrightness(): Boolean {
        return settingsPrefs.getBoolean(KEY_USE_MAX_BRIGHTNESS, false)
    }

    fun setAmoledModeEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_AMOLED_MODE_ENABLED, enabled) }
        _amoledModeFlow.value = enabled
    }

    fun isAmoledModeEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_AMOLED_MODE_ENABLED, false)
    }

    val amoledModeFlow: Flow<Boolean> = _amoledModeFlow.asStateFlow()

    fun setDynamicColorEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
        _dynamicColorFlow.value = enabled
    }

    fun isDynamicColorEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_DYNAMIC_COLOR, false)
    }

    val dynamicColorFlow: Flow<Boolean> = _dynamicColorFlow.asStateFlow()

    fun setAppThemeMode(themeMode: AppThemeMode) {
        settingsPrefs.edit { putString(KEY_APP_THEME_MODE, themeMode.name) }
        _appThemeModeFlow.value = themeMode
    }

    fun getAppThemeMode(): AppThemeMode {
        val mode = settingsPrefs.getString(KEY_APP_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(mode ?: AppThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            AppThemeMode.SYSTEM
        }
    }

    val appThemeModeFlow: Flow<AppThemeMode> = _appThemeModeFlow.asStateFlow()

    fun setDisableHaptics(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_DISABLE_HAPTICS, enabled) }
    }

    fun shouldDisableHaptics(): Boolean {
        return settingsPrefs.getBoolean(KEY_DISABLE_HAPTICS, false)
    }

    fun setShowSystemApps(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_SYSTEM_APPS, enabled) }
    }

    fun shouldShowSystemApps(): Boolean {
        return settingsPrefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
    }

    fun setAntiUninstallEnabled(enabled: Boolean) {
        settingsPrefs.edit(commit = true) { putBoolean(KEY_ANTI_UNINSTALL, enabled) }
    }

    fun isAntiUninstallEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL, false)
    }

    fun setAntiUninstallAdminSettingsEnabled(enabled: Boolean) {
        settingsPrefs.edit(commit = true) { putBoolean(KEY_ANTI_UNINSTALL_ADMIN_SETTINGS, enabled) }
    }

    fun isAntiUninstallAdminSettingsEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL_ADMIN_SETTINGS, false)
    }

    fun setAntiUninstallUsageStatsEnabled(enabled: Boolean) {
        settingsPrefs.edit(commit = true) { putBoolean(KEY_ANTI_UNINSTALL_USAGE_STATS, enabled) }
    }

    fun isAntiUninstallUsageStatsEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL_USAGE_STATS, false)
    }

    fun setAntiUninstallAccessibilityEnabled(enabled: Boolean) {
        settingsPrefs.edit(commit = true) { putBoolean(KEY_ANTI_UNINSTALL_ACCESSIBILITY, enabled) }
    }

    fun isAntiUninstallAccessibilityEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL_ACCESSIBILITY, false)
    }

    fun setAntiUninstallOverlayEnabled(enabled: Boolean) {
        settingsPrefs.edit(commit = true) { putBoolean(KEY_ANTI_UNINSTALL_OVERLAY, enabled) }
    }

    fun isAntiUninstallOverlayEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL_OVERLAY, false)
    }

    fun setProtectEnabled(enabled: Boolean) {
        settingsPrefs.edit(commit = true) { putBoolean(KEY_APPLOCK_ENABLED, enabled) }
    }

    fun isProtectEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_APPLOCK_ENABLED, DEFAULT_PROTECT_ENABLED)
    }

    fun setUnlockTimeDuration(minutes: Int) {
        settingsPrefs.edit { putInt(KEY_UNLOCK_TIME_DURATION, minutes) }
    }

    fun getUnlockTimeDuration(): Int {
        return settingsPrefs.getInt(KEY_UNLOCK_TIME_DURATION, DEFAULT_UNLOCK_DURATION)
    }

    fun setAutoUnlockEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_AUTO_UNLOCK, enabled) }
    }

    fun isAutoUnlockEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_AUTO_UNLOCK, false)
    }

    fun setBackendImplementation(backend: BackendImplementation) {
        settingsPrefs.edit(commit = true) { putString(KEY_BACKEND_IMPLEMENTATION, backend.name) }
    }

    fun getBackendImplementation(): BackendImplementation {
        val backend = settingsPrefs.getString(
            KEY_BACKEND_IMPLEMENTATION,
            BackendImplementation.ACCESSIBILITY.name
        )
        return try {
            BackendImplementation.valueOf(backend ?: BackendImplementation.ACCESSIBILITY.name)
        } catch (_: IllegalArgumentException) {
            BackendImplementation.ACCESSIBILITY
        }
    }

    fun isShowCommunityLink(): Boolean {
        return !settingsPrefs.getBoolean(KEY_COMMUNITY_LINK_SHOWN, false)
    }

    fun setCommunityLinkShown(shown: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_COMMUNITY_LINK_SHOWN, shown) }
    }

    fun isShowDonateLink(context: Context): Boolean {
        return settingsPrefs.getBoolean(KEY_SHOW_DONATE_LINK, false)
    }

    fun setShowDonateLink(context: Context, show: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_DONATE_LINK, show) }
    }

    fun isLoggingEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_LOGGING_ENABLED, false)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_LOGGING_ENABLED, enabled) }
    }

    fun setIntruderSelfieEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_INTRUDER_SELFIE_ENABLED, enabled) }
    }

    fun isIntruderSelfieEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_INTRUDER_SELFIE_ENABLED, false)
    }

    fun setIntruderSelfieAttempts(attempts: Int) {
        settingsPrefs.edit { putInt(KEY_INTRUDER_SELFIE_ATTEMPTS, attempts) }
    }

    fun getIntruderSelfieAttempts(): Int {
        return settingsPrefs.getInt(KEY_INTRUDER_SELFIE_ATTEMPTS, 3)
    }

    companion object {
        private const val PREFS_NAME_APP_LOCK = "app_lock_prefs"
        private const val PREFS_NAME_SETTINGS = "app_lock_settings"

        private const val KEY_PASSWORD = "password"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_BIOMETRIC_AUTH_ENABLED = "use_biometric_auth"
        private const val KEY_DISABLE_HAPTICS = "disable_haptics"
        private const val KEY_USE_MAX_BRIGHTNESS = "use_max_brightness"
        private const val KEY_AMOLED_MODE_ENABLED = "amoled_mode_enabled"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_ANTI_UNINSTALL = "anti_uninstall"
        private const val KEY_ANTI_UNINSTALL_ADMIN_SETTINGS = "anti_uninstall_admin_settings"
        private const val KEY_ANTI_UNINSTALL_USAGE_STATS = "anti_uninstall_usage_stats"
        private const val KEY_ANTI_UNINSTALL_ACCESSIBILITY = "anti_uninstall_accessibility"
        private const val KEY_ANTI_UNINSTALL_OVERLAY = "anti_uninstall_overlay"
        private const val KEY_UNLOCK_TIME_DURATION = "unlock_time_duration"
        private const val KEY_BACKEND_IMPLEMENTATION = "backend_implementation"
        private const val KEY_COMMUNITY_LINK_SHOWN = "community_link_shown"
        private const val KEY_SHOW_DONATE_LINK = "show_donate_link"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val LAST_VERSION_CODE = "last_version_code"
        private const val KEY_APPLOCK_ENABLED = "applock_enabled"
        private const val KEY_AUTO_UNLOCK = "auto_unlock"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_INTRUDER_SELFIE_ENABLED = "intruder_selfie_enabled"
        private const val KEY_INTRUDER_SELFIE_ATTEMPTS = "intruder_selfie_attempts"

        private const val DEFAULT_PROTECT_ENABLED = true
        private const val DEFAULT_UNLOCK_DURATION = 0

        const val LOCK_TYPE_PIN = "pin"
        const val LOCK_TYPE_PATTERN = "pattern"

        // Static flows to ensure all repository instances share the same state
        private val _amoledModeFlow = MutableStateFlow(false)
        private val _dynamicColorFlow = MutableStateFlow(false)
        private val _appThemeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)

        // Initialize static flows with actual values on first creation
        private var initialized = false
    }

    init {
        if (!initialized) {
            _amoledModeFlow.value = isAmoledModeEnabled()
            _dynamicColorFlow.value = isDynamicColorEnabled()
            _appThemeModeFlow.value = getAppThemeMode()
            initialized = true
        }
    }
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
