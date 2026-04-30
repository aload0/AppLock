package dev.pranav.applock.core.broadcast

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.services.AppLockAccessibilityService
import dev.pranav.applock.services.ShizukuAppLockService
import dev.pranav.applock.services.UsageLockService
import dev.pranav.applock.services.isServiceRunning

object AppLockServiceStarter {
    private const val TAG = "AppLockServiceStarter"

    fun startAppropriateServices(
        context: Context,
        repository: dev.pranav.applock.data.repository.AppLockRepository
    ) {
        if (repository.isAntiUninstallEnabled()) {
            startService(context, AppLockAccessibilityService::class.java)
        }

        when (repository.getBackendImplementation()) {
            BackendImplementation.SHIZUKU -> {
                startService(context, ShizukuAppLockService::class.java)
            }

            BackendImplementation.ACCESSIBILITY -> {
                startService(context, AppLockAccessibilityService::class.java)
            }

            BackendImplementation.USAGE_STATS -> {
                startService(context, UsageLockService::class.java)
            }
        }
    }

    private fun startService(context: Context, serviceClass: Class<*>) {
        try {
            if (context.isServiceRunning(serviceClass)) {
                Log.d(TAG, "Service already running: ${serviceClass.simpleName}")
                return
            }

            val serviceIntent = Intent(context, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                serviceClass != AppLockAccessibilityService::class.java
            ) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Started service: ${serviceClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service: ${serviceClass.simpleName}", e)
        }
    }

}
