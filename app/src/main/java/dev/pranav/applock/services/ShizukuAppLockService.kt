package dev.pranav.applock.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.AppLockRepository.Companion.shouldStartService
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.features.lockscreen.ui.PasswordOverlayActivity
import dev.pranav.applock.shizuku.ShizukuActivityManager
import rikka.shizuku.Shizuku

class ShizukuAppLockService : Service() {
    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }
    private var shizukuActivityManager: ShizukuActivityManager? = null
    private var previousForegroundPackage = ""

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    companion object {
        private const val TAG = "ShizukuAppLockService"
        private const val NOTIFICATION_ID = 112
        private const val CHANNEL_ID = "ShizukuAppLockServiceChannel"

        @Volatile
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ShizukuAppLockService created")
        AppLockManager.isLockScreenShown.set(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ShizukuAppLockService started. Running: $isServiceRunning")

        if (isServiceRunning) return START_STICKY
        isServiceRunning = true

        if (!shouldStartService(appLockRepository, this::class.java) || !isShizukuAvailable()) {
            Log.e(TAG, "Service not needed or Shizuku not ready. Triggering fallback if necessary.")
            isServiceRunning = false
            AppLockManager.startFallbackServices(this, ShizukuAppLockService::class.java)
            stopSelf()
            return START_NOT_STICKY
        }

        AppLockManager.resetRestartAttempts(TAG)
        appLockRepository.setActiveBackend(BackendImplementation.SHIZUKU)
        AppLockManager.stopAllOtherServices(this, this::class.java)

        setupShizukuActivityManager()

        val shizukuStarted = shizukuActivityManager?.start() == true
        if (!shizukuStarted) {
            Log.e(TAG, "Shizuku failed to start, triggering fallback")
            isServiceRunning = false
            AppLockManager.startFallbackServices(this, ShizukuAppLockService::class.java)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "ShizukuAppLockService destroyed.")

        shizukuActivityManager?.stop()

        if (isServiceRunning && shouldStartService(appLockRepository, this::class.java)) {
            Log.d(TAG, "Service destroyed unexpectedly, starting fallback")
            AppLockManager.startFallbackServices(this, ShizukuAppLockService::class.java)
        }

        isServiceRunning = false
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "ShizukuAppLockService unbound. Checking for necessary restart.")
        if (shouldStartService(appLockRepository, this::class.java)) {
            AppLockManager.startFallbackServices(this, ShizukuAppLockService::class.java)
        }
        return super.onUnbind(intent)
    }

    private fun isShizukuAvailable(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val type = determineForegroundServiceType()
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun determineForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(this, DeviceAdmin::class.java)

            return if (dpm.isAdminActive(component)) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        }
        return 0
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "AppLock Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AppLock")
            .setContentText("Protecting your apps with Shizuku")
            .setSmallIcon(R.drawable.baseline_shield_24)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun setupShizukuActivityManager() {
        shizukuActivityManager =
            ShizukuActivityManager(this, appLockRepository) { packageName, _, timeMillis ->
                try {
                    val triggeringPackage = previousForegroundPackage
                    previousForegroundPackage = packageName

                    if (AppLockManager.isLockScreenShown.get() || packageName == this.packageName) {
                        return@ShizukuActivityManager
                    }

                    val triggerExclusions = appLockRepository.getTriggerExcludedApps()
                    if (triggeringPackage in triggerExclusions) {
                        Log.d(
                            TAG,
                            "Trigger app $triggeringPackage is excluded, skipping lock for $packageName"
                        )
                        return@ShizukuActivityManager
                    }

                    Log.d(TAG, "Current package=$packageName, trigger=$triggeringPackage")
                    checkAndLockApp(packageName, triggeringPackage, timeMillis)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ShizukuActivityManager callback", e)
                }
            }
    }

    private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
        val lockedApps = appLockRepository.getLockedApps()

        if (packageName !in lockedApps) return

        val unlockDurationMinutes = appLockRepository.getUnlockTimeDuration()
        val unlockTimestamp = AppLockManager.appUnlockTimes[packageName] ?: 0L

        Log.d(
            TAG,
            "checkAndLockApp: pkg=$packageName, duration=$unlockDurationMinutes min, unlockTime=$unlockTimestamp, currentTime=$currentTime, isLockScreenShown=${AppLockManager.isLockScreenShown.get()}"
        )

        if (unlockDurationMinutes > 0 && unlockTimestamp > 0) {
            if (unlockDurationMinutes >= 10_000) {
                return
            }

            val durationMillis = unlockDurationMinutes.toLong() * 60_000L

            val elapsedMillis = currentTime - unlockTimestamp

            Log.d(
                TAG,
                "Grace period check: elapsed=${elapsedMillis}ms (${elapsedMillis / 1000}s), duration=${durationMillis}ms (${durationMillis / 1000}s)"
            )

            if (elapsedMillis < durationMillis) {
                return
            }

            Log.d(TAG, "Unlock grace period expired for $packageName. Clearing timestamp.")
            AppLockManager.appUnlockTimes.remove(packageName)
        }

        if (AppLockManager.isLockScreenShown.get()) {
            Log.d(TAG, "Lock screen already shown, skipping")
            return
        }

        Log.d(TAG, "Locked app detected: $packageName. Showing overlay.")
        AppLockManager.isLockScreenShown.set(true)

        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_FROM_BACKGROUND or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("locked_package", packageName)
            putExtra("triggering_package", triggeringPackage)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            AppLockManager.isLockScreenShown.set(false)
            Log.e(TAG, "Failed to start password overlay: ${e.message}", e)
        }
    }
}
