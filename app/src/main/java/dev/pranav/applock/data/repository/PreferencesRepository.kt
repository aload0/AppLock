package dev.pranav.applock.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Repository for managing application preferences and settings.
 * Handles all SharedPreferences operations with proper separation of concerns.
 * COMPLETE FILE - Copy and paste to replace your existing PreferencesRepository.kt
 */
class PreferencesRepository(context: Context) {

    private val appLockPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_APP_LOCK, Context.MODE_PRIVATE)

    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)

    // ============= AUTHENTICATION =============
    fun setPassword(password: String) {
        appLockPrefs.edit { putString(KEY_PASSWORD, password) }
    }

    fun getPassword(): String? {
        return appLockPrefs.getString(KEY_PASSWORD, null)
    }

    fun validatePassword(inputPassword: String): Boolean {
        val storedPassword = getPassword()
        return storedPassword != null && inputPassword == storedPassword
    }

    fun setPattern(pattern: String) {
        appLockPrefs.edit { putString(KEY_PATTERN, pattern) }
    }

    fun getPattern(): String? {
        return appLockPrefs.getString(KEY_PATTERN, null)
    }

    fun validatePattern(inputPattern: String): Boolean {
        val storedPattern = getPattern()
        return storedPattern != null && inputPattern == storedPattern
    }

    // ============= LOCK TYPE & BIOMETRIC =============
    fun setLockType(lockType: String) {
        settingsPrefs.edit { putString(KEY_LOCK_TYPE, lockType) }
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

    // ============= DISPLAY SETTINGS =============
    fun setUseMaxBrightness(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_USE_MAX_BRIGHTNESS, enabled) }
    }

    fun shouldUseMaxBrightness(): Boolean {
        return settingsPrefs.getBoolean(KEY_USE_MAX_BRIGHTNESS, false)
    }

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

    // ============= ANTI-UNINSTALL & PROTECTION =============
    fun setAntiUninstallEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_ANTI_UNINSTALL, enabled) }
    }

    fun isAntiUninstallEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL, false)
    }

    fun setProtectEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_APPLOCK_ENABLED, enabled) }
    }

    fun isProtectEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_APPLOCK_ENABLED, DEFAULT_PROTECT_ENABLED)
    }

    // ============= SYSTEM SETTINGS RESTRICTIONS (NEW) =============
    fun setRestrictDrawOverAppsSettings(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_RESTRICT_DRAW_OVER_APPS, enabled) }
    }

    fun isRestrictDrawOverAppsSettings(): Boolean {
        return settingsPrefs.getBoolean(KEY_RESTRICT_DRAW_OVER_APPS, false)
    }

    fun setRestrictUsageAccessSettings(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_RESTRICT_USAGE_ACCESS, enabled) }
    }

    fun isRestrictUsageAccessSettings(): Boolean {
        return settingsPrefs.getBoolean(KEY_RESTRICT_USAGE_ACCESS, false)
    }

    fun setRestrictAccessibilitySettings(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_RESTRICT_ACCESSIBILITY_SETTINGS, enabled) }
    }

    fun isRestrictAccessibilitySettings(): Boolean {
        return settingsPrefs.getBoolean(KEY_RESTRICT_ACCESSIBILITY_SETTINGS, false)
    }

    fun setRestrictDeviceAdminSettings(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_RESTRICT_DEVICE_ADMIN_SETTINGS, enabled) }
    }

    fun isRestrictDeviceAdminSettings(): Boolean {
        return settingsPrefs.getBoolean(KEY_RESTRICT_DEVICE_ADMIN_SETTINGS, false)
    }

    fun setRequireUnrestrictedBattery(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_REQUIRE_UNRESTRICTED_BATTERY, enabled) }
    }

    fun isRequireUnrestrictedBattery(): Boolean {
        return settingsPrefs.getBoolean(KEY_REQUIRE_UNRESTRICTED_BATTERY, false)
    }

    fun disableAllSystemSettingsRestrictions() {
        settingsPrefs.edit {
            putBoolean(KEY_RESTRICT_DRAW_OVER_APPS, false)
            putBoolean(KEY_RESTRICT_USAGE_ACCESS, false)
            putBoolean(KEY_RESTRICT_ACCESSIBILITY_SETTINGS, false)
            putBoolean(KEY_RESTRICT_DEVICE_ADMIN_SETTINGS, false)
            putBoolean(KEY_REQUIRE_UNRESTRICTED_BATTERY, false)
        }
    }

    // ============= UNLOCK DURATION & AUTO-UNLOCK =============
    fun setUnlockTimeDuration(minutes: Int) {
        settingsPrefs.edit { putInt(KEY_UNLOCK_TIME_DURATION, minutes) }
    }

    fun getUnlockTimeDuration(): Int {
        return settingsPrefs.getInt(KEY_UNLOCK_TIME_DURATION, 0)
    }

    fun setAutoUnlockEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_AUTO_UNLOCK_ENABLED, enabled) }
    }

    fun isAutoUnlockEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_AUTO_UNLOCK_ENABLED, false)
    }

    // ============= BACKEND IMPLEMENTATION =============
    fun setBackendImplementation(backend: BackendImplementation) {
        settingsPrefs.edit { putString(KEY_BACKEND_IMPLEMENTATION, backend.name) }
    }

    fun getBackendImplementation(): BackendImplementation {
        val value = settingsPrefs.getString(KEY_BACKEND_IMPLEMENTATION, null)
        return if (value != null) BackendImplementation.valueOf(value)
        else BackendImplementation.ACCESSIBILITY
    }

    // ============= LINKS =============
    fun isShowCommunityLink(): Boolean {
        return settingsPrefs.getBoolean(KEY_SHOW_COMMUNITY_LINK, true)
    }

    fun setCommunityLinkShown(shown: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_COMMUNITY_LINK, shown) }
    }

    fun isShowDonateLink(): Boolean {
        return settingsPrefs.getBoolean(KEY_SHOW_DONATE_LINK, true)
    }

    fun setShowDonateLink(show: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_DONATE_LINK, show) }
    }

    fun isShowDonateLink(context: Context): Boolean {
        return settingsPrefs.getBoolean(KEY_SHOW_DONATE_LINK, true)
    }

    fun setShowDonateLink(context: Context, show: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_DONATE_LINK, show) }
    }

    // ============= LOGGING =============
    fun isLoggingEnabled(): Boolean {
        return settingsPrefs.getBoolean(KEY_LOGGING_ENABLED, false)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_LOGGING_ENABLED, enabled) }
    }

    companion object {
        private const val PREFS_NAME_APP_LOCK = "applock_prefs"
        private const val PREFS_NAME_SETTINGS = "settings_prefs"

        private const val KEY_PASSWORD = "password"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_BIOMETRIC_AUTH_ENABLED = "biometric_auth_enabled"
        private const val KEY_USE_MAX_BRIGHTNESS = "use_max_brightness"
        private const val KEY_DISABLE_HAPTICS = "disable_haptics"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_ANTI_UNINSTALL = "anti_uninstall"
        private const val KEY_APPLOCK_ENABLED = "applock_enabled"

        // System Settings Restriction Keys
        private const val KEY_RESTRICT_DRAW_OVER_APPS = "restrict_draw_over_apps"
        private const val KEY_RESTRICT_USAGE_ACCESS = "restrict_usage_access"
        private const val KEY_RESTRICT_ACCESSIBILITY_SETTINGS = "restrict_accessibility_settings"
        private const val KEY_RESTRICT_DEVICE_ADMIN_SETTINGS = "restrict_device_admin_settings"
        private const val KEY_REQUIRE_UNRESTRICTED_BATTERY = "require_unrestricted_battery"

        private const val KEY_UNLOCK_TIME_DURATION = "unlock_time_duration"
        private const val KEY_AUTO_UNLOCK_ENABLED = "auto_unlock_enabled"
        private const val KEY_BACKEND_IMPLEMENTATION = "backend_implementation"
        private const val KEY_SHOW_COMMUNITY_LINK = "show_community_link"
        private const val KEY_SHOW_DONATE_LINK = "show_donate_link"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"

        const val LOCK_TYPE_PIN = "pin"
        const val LOCK_TYPE_PATTERN = "pattern"
        const val LOCK_TYPE_PASSWORD = "password"

        const val DEFAULT_PROTECT_ENABLED = true
    }
}

enum class BackendImplementation {
    ACCESSIBILITY,
    USAGE_STATS,
    SHIZUKU
}
