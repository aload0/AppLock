package dev.pranav.applock

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.data.repository.AppLockRepository
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui

class AppLockApplication : Application() {

    lateinit var appLockRepository: AppLockRepository
        private set

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initializeHiddenApiBypass()
    }

    override fun onCreate() {
        super.onCreate()
        initializeComponents()

        LogUtils.initialize(this)
        checkAndHandleAppUpdate()
    }

    private fun initializeHiddenApiBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")
                Log.d(TAG, "Hidden API bypass initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize hidden API bypass", e)
            }
        }
    }

    private fun initializeComponents() {
        try {
            appLockRepository = AppLockRepository(this)
            initializeSui()
            Log.d(TAG, "Application components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize application components", e)
        }
    }

    private fun initializeSui() {
        try {
            Sui.init(packageName)
            Log.d(TAG, "Sui initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Sui", e)
        }
    }

    /**
     * Check if the app has been updated and handle accordingly.
     * On update: clear all old logs
     * On every start: purge logs older than 3 days
     */
    private fun checkAndHandleAppUpdate() {
        try {
            val prefsRepo = appLockRepository.preferencesRepository
            val lastVersionCode = prefsRepo.getLastVersionCode()
            val currentVersionCode = BuildConfig.VERSION_CODE

            // Always purge old logs (older than 3 days) on app start
            LogUtils.purgeOldLogs()

            // If app was updated, clear all logs and update version code
            if (lastVersionCode != 0 && lastVersionCode < currentVersionCode) {
                Log.d(TAG, "App updated from version $lastVersionCode to $currentVersionCode. Clearing logs.")
                LogUtils.clearAllLogs()
                prefsRepo.setLastVersionCode(currentVersionCode)
            } else if (lastVersionCode == 0) {
                // First time app is running, just save the version code
                Log.d(TAG, "First app launch. Setting version code to $currentVersionCode")
                prefsRepo.setLastVersionCode(currentVersionCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app update", e)
        }
    }

    companion object {
        private const val TAG = "AppLockApplication"
    }
}
