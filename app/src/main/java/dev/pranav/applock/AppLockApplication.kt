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

    companion object {
        private const val TAG = "AppLockApplication"
    }
}
