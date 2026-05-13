package dev.pranav.applock.features.lockscreen.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.ui.theme.AppLockTheme

@SuppressLint("ViewConstructor")
class LockScreenOverlayManager(private val context: Context):
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, OnBackPressedDispatcherOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    // Lifecycle setup
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private var isStateRestored = false

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override val onBackPressedDispatcher = OnBackPressedDispatcher {
        removeOverlay()
    }

    fun showOverlay(
        lockedPackageName: String,
        triggeringPackageName: String,
        onUnlock: () -> Unit,
        onExit: () -> Unit
    ) {
        if (composeView != null) return

        if (!isStateRestored) {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            isStateRestored = true
        }

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@LockScreenOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@LockScreenOverlayManager)
            setViewTreeViewModelStoreOwner(this@LockScreenOverlayManager)

            setContent {
                AppLockTheme {
                    val appLockRepository = context.appLockRepository()
                    val appName = try {
                        val pm = context.packageManager
                        pm.getApplicationLabel(pm.getApplicationInfo(lockedPackageName, 0))
                            .toString()
                    } catch (_: Exception) {
                        "App"
                    }

                    val onPinAttemptCallback = { pin: String ->
                        val isValid = appLockRepository.validatePassword(pin)
                        if (isValid) {
                            onUnlock()
                            removeOverlay()
                        }
                        isValid
                    }

                    val onPatternAttemptCallback = { pattern: String ->
                        val isValid = appLockRepository.validatePattern(pattern)
                        if (isValid) {
                            onUnlock()
                            removeOverlay()
                        }
                        isValid
                    }

                    BackHandler {
                        onExit()
                        removeOverlay()
                    }

                    val lockType = appLockRepository.getLockType()

                    if (lockType == PreferencesRepository.LOCK_TYPE_PATTERN) {
                        PatternLockScreen(
                            fromMainActivity = false,
                            lockedAppName = appName,
                            triggeringPackageName = triggeringPackageName,
                            onPatternAttempt = onPatternAttemptCallback
                        )
                    } else {
                        PasswordOverlayScreen(
                            showBiometricButton = appLockRepository.isBiometricAuthEnabled(),
                            fromMainActivity = false,
                            lockedAppName = appName,
                            triggeringPackageName = triggeringPackageName,
                            onAuthSuccess = {
                                onUnlock()
                                removeOverlay()
                            },
                            onBiometricAuth = {
                                val intent = Intent(
                                    context,
                                    TransparentBiometricActivity::class.java
                                ).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                    putExtra("locked_package", lockedPackageName)
                                }
                                context.startActivity(intent)
                            },
                            onPinAttempt = onPinAttemptCallback
                        )
                    }
                }
            }
        }

        // Window Layout Parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Respect brightness setting
            if (context.appLockRepository().shouldUseMaxBrightness()) {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }

        composeView?.isFocusableInTouchMode = true
        composeView?.requestFocus()

        //// Block Back Button
        //composeView?.setOnKeyListener { _, keyCode, event ->
        //    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
        //        val intent = Intent(Intent.ACTION_MAIN).apply {
        //            addCategory(Intent.CATEGORY_HOME)
        //            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //        }
        //        context.startActivity(intent)
        //
        //        removeOverlay()
        //        return@setOnKeyListener true
        //    }
        //    false
        //}

        try {
            windowManager.addView(composeView, params)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeOverlay() {
        composeView?.let {
            try {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
        }
    }
}
