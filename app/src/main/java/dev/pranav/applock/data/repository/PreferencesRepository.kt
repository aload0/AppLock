package dev.pranav.applock.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

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
        settingsPrefs.edit { putString(KEY_BACKEND_IMPLEMENTATION, backend.name) }
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

    companion object {
        private const val PREFS_NAME_APP_LOCK = "app_lock_prefs"
        private const val PREFS_NAME_SETTINGS = "app_lock_settings"

        private const val KEY_PASSWORD = "password"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_BIOMETRIC_AUTH_ENABLED = "use_biometric_auth"
        private const val KEY_DISABLE_HAPTICS = "disable_haptics"
        private const val KEY_USE_MAX_BRIGHTNESS = "use_max_brightness"
        private const val KEY_ANTI_UNINSTALL = "anti_uninstall"
        private const val KEY_UNLOCK_TIME_DURATION = "unlock_time_duration"
        private const val KEY_BACKEND_IMPLEMENTATION = "backend_implementation"
        private const val KEY_COMMUNITY_LINK_SHOWN = "community_link_shown"
        private const val KEY_SHOW_DONATE_LINK = "show_donate_link"
        private const val LAST_VERSION_CODE = "last_version_code"
        private const val KEY_APPLOCK_ENABLED = "applock_enabled"
        private const val KEY_AUTO_UNLOCK = "auto_unlock"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_LOCK_TYPE = "lock_type"

        private const val DEFAULT_PROTECT_ENABLED = true
        private const val DEFAULT_UNLOCK_DURATION = 0

        const val LOCK_TYPE_PIN = "pin"
        const val LOCK_TYPE_PATTERN = "pattern"
    }
}
