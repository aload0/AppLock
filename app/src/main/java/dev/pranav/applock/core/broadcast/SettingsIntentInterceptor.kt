package dev.pranav.applock.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.systemSettingsRestrictionManager

/**
 * COMPLETE FILE - Copy to app/src/main/java/dev/pranav/applock/core/broadcast/
 * 
 * Broadcast receiver that intercepts attempts to open restricted system settings pages.
 * Register in AndroidManifest.xml with the provided intent filters.
 */
class SettingsIntentInterceptor : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) {
            return
        }

        try {
            val repository = context.appLockRepository()
            
            if (!repository.isAntiUninstallEnabled()) {
                return
            }

            val restrictionManager = context.systemSettingsRestrictionManager()
            
            if (restrictionManager.isIntentRestricted(intent)) {
                blockAndShowLockScreen(context, intent)
                abortBroadcast()
            }
        } catch (e: Exception) {
            LogUtils.logError("Error in SettingsIntentInterceptor", e)
        }
    }

    /**
     * Block the settings intent and show lock screen instead.
     */
    private fun blockAndShowLockScreen(context: Context, intent: Intent) {
        try {
            val action = intent.action ?: "Unknown"
            LogUtils.logSecurityEvent("Blocked attempt to access settings: $action")

            val lockScreenIntent = Intent(context, PasswordOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("isRestrictedSettings", true)
                putExtra("restrictedAction", intent.action)
            }
            context.startActivity(lockScreenIntent)

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            LogUtils.logError("Failed to block and show lock screen", e)
        }
    }

    companion object {
        private const val TAG = "SettingsIntentInterceptor"
    }
}

/**
 * Alternative implementation: Activity that intercepts settings intents.
 * Can be used as a bridge activity to intercept intents before they reach system settings.
 * Optional - only use if BroadcastReceiver approach doesn't work on your device.
 */
class SettingsIntentInterceptorActivity : android.app.Activity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val intent = intent
            val repository = applicationContext.appLockRepository()
            
            if (repository.isAntiUninstallEnabled()) {
                val restrictionManager = applicationContext.systemSettingsRestrictionManager()
                
                if (restrictionManager.isIntentRestricted(intent)) {
                    LogUtils.logSecurityEvent("Blocked attempt to access settings: ${intent.action}")
                    
                    val lockScreenIntent = Intent(applicationContext, PasswordOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        putExtra("isRestrictedSettings", true)
                    }
                    startActivity(lockScreenIntent)
                    
                    finish()
                    return
                }
            }
            
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            LogUtils.logError("Error in SettingsIntentInterceptorActivity", e)
            finish()
        }
    }
}
