package dev.pranav.applock.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Repository for managing locked applications and trigger exclusions.
 * Handles all app-related locking functionality.
 */
class LockedAppsRepository(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Locked Apps Management
    fun getLockedApps(): Set<String> {
        return preferences.getStringSet(KEY_LOCKED_APPS, emptySet())?.toSet() ?: emptySet()
    }

    fun addLockedApp(packageName: String) {
        if (packageName.isBlank()) return
        val updated = getLockedApps() + packageName
        preferences.edit { putStringSet(KEY_LOCKED_APPS, updated) }
    }

    fun removeLockedApp(packageName: String) {
        val updated = getLockedApps() - packageName
        preferences.edit { putStringSet(KEY_LOCKED_APPS, updated) }
    }

    fun isAppLocked(packageName: String): Boolean {
        return getLockedApps().contains(packageName)
    }

    fun clearAllLockedApps() {
        preferences.edit { putStringSet(KEY_LOCKED_APPS, emptySet()) }
    }

    // Trigger Exclusions Management
    fun getTriggerExcludedApps(): Set<String> {
        return preferences.getStringSet(KEY_TRIGGER_EXCLUDED_APPS, emptySet())?.toSet()
            ?: emptySet()
    }

    fun addTriggerExcludedApp(packageName: String) {
        if (packageName.isBlank()) return
        val updated = getTriggerExcludedApps() + packageName
        preferences.edit { putStringSet(KEY_TRIGGER_EXCLUDED_APPS, updated) }
    }

    fun removeTriggerExcludedApp(packageName: String) {
        val updated = getTriggerExcludedApps() - packageName
        preferences.edit { putStringSet(KEY_TRIGGER_EXCLUDED_APPS, updated) }
    }

    fun isAppTriggerExcluded(packageName: String): Boolean {
        return getTriggerExcludedApps().contains(packageName)
    }

    fun clearAllTriggerExclusions() {
        preferences.edit { putStringSet(KEY_TRIGGER_EXCLUDED_APPS, emptySet()) }
    }

    // Anti-Uninstall Apps Management
    fun getAntiUninstallApps(): Set<String> {
        return preferences.getStringSet(KEY_ANTI_UNINSTALL_APPS, emptySet())?.toSet() ?: emptySet()
    }

    fun addAntiUninstallApp(packageName: String) {
        if (packageName.isBlank()) return
        val updated = getAntiUninstallApps() + packageName
        preferences.edit { putStringSet(KEY_ANTI_UNINSTALL_APPS, updated) }
    }

    fun removeAntiUninstallApp(packageName: String) {
        val updated = getAntiUninstallApps() - packageName
        preferences.edit { putStringSet(KEY_ANTI_UNINSTALL_APPS, updated) }
    }

    fun isAppAntiUninstall(packageName: String): Boolean {
        return getAntiUninstallApps().contains(packageName)
    }

    fun clearAllAntiUninstallApps() {
        preferences.edit { putStringSet(KEY_ANTI_UNINSTALL_APPS, emptySet()) }
    }

    // Bulk operations
    fun addMultipleLockedApps(packageNames: Set<String>) {
        val validPackageNames = packageNames.filter { it.isNotBlank() }.toSet()
        if (validPackageNames.isEmpty()) return
        val updated = getLockedApps() + validPackageNames
        preferences.edit { putStringSet(KEY_LOCKED_APPS, updated) }
    }

    fun removeMultipleLockedApps(packageNames: Set<String>) {
        val updated = getLockedApps() - packageNames
        preferences.edit { putStringSet(KEY_LOCKED_APPS, updated) }
    }

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_LOCKED_APPS = "locked_apps"
        private const val KEY_TRIGGER_EXCLUDED_APPS = "trigger_excluded_apps"
        private const val KEY_ANTI_UNINSTALL_APPS = "anti_uninstall_apps"
    }
}
