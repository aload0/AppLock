package dev.pranav.applock.data.manager

import android.util.Log
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.services.AppLockAccessibilityService
import dev.pranav.applock.services.ShizukuAppLockService
import dev.pranav.applock.services.UsageLockService

/**
 * Manages backend service operations and switching between different implementations.
 * Provides a centralized way to handle service lifecycle and backend selection.
 */
class BackendServiceManager {

    private var activeBackend: BackendImplementation? = null

    fun setActiveBackend(backend: BackendImplementation) {
        activeBackend = backend
        Log.d(TAG, "Active backend set to: ${backend.name}")
    }

    fun shouldStartService(
        serviceClass: Class<*>,
        chosenBackend: BackendImplementation
    ): Boolean {
        Log.d(TAG, "Checking if service ${serviceClass.simpleName} should start")
        Log.d(TAG, "Chosen backend: ${chosenBackend.name}")

        val serviceBackend = getBackendForService(serviceClass)
        if (serviceBackend == null) {
            Log.d(TAG, "Unknown service class: ${serviceClass.simpleName}")
            return false
        }

        if (serviceBackend == chosenBackend) {
            Log.d(TAG, "Service ${serviceClass.simpleName} matches chosen backend")
            return true
        }

        Log.d(TAG, "Service ${serviceClass.simpleName} should not start")
        return false
    }

    private fun getBackendForService(serviceClass: Class<*>): BackendImplementation? {
        return when (serviceClass) {
            AppLockAccessibilityService::class.java -> BackendImplementation.ACCESSIBILITY
            UsageLockService::class.java -> BackendImplementation.USAGE_STATS
            ShizukuAppLockService::class.java -> BackendImplementation.SHIZUKU
            else -> null
        }
    }

    companion object {
        private const val TAG = "BackendServiceManager"
    }
}
