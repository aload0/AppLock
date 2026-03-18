package dev.pranav.applock.features.lockscreen.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.ui.shapes
import dev.pranav.applock.core.utils.IntruderSelfieManager
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.vibrate
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.services.AppLockManager
import dev.pranav.applock.ui.icons.Backspace
import dev.pranav.applock.ui.icons.Fingerprint
import dev.pranav.applock.ui.theme.AppLockTheme
import dev.pranav.applock.features.setpassword.ui.RecoveryKeyDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

class PasswordOverlayActivity : FragmentActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var appLockRepository: AppLockRepository
    internal var lockedPackageNameFromIntent: String? = null
    internal var triggeringPackageNameFromIntent: String? = null

    private var isBiometricPromptShowingLocal = false
    private var movedToBackground = false
    private val isGoingHome = AtomicBoolean(false)
    
    private var appName by mutableStateOf("")
    private var appIcon by mutableStateOf<Drawable?>(null)

    private val TAG = "PasswordOverlayActivity"

    private val systemDialogsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra("reason")
                // Handle button presses
                when (reason) {
                    "recentapps", "homekey" -> {
                        // Home or Recents button pressed - trigger HOME action
                        Log.d(TAG, "System dialog action detected ($reason) - triggering HOME action")
                        goHome()
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lockedPackageNameFromIntent = intent.getStringExtra("locked_package")
        triggeringPackageNameFromIntent = intent.getStringExtra("triggering_package")
        if (lockedPackageNameFromIntent == null) {
            Log.e(TAG, "No locked_package name provided in intent. Finishing.")
            finishAffinity()
            return
        }

        enableEdgeToEdge()

        appLockRepository = AppLockRepository(applicationContext)

        // Override back button to do nothing
        onBackPressedDispatcher.addCallback(this) {
            Log.d(TAG, "Back button pressed - ignoring on lock screen")
            // Do nothing - swallow the back press
        }

        setupWindow()
        loadAppDetailsAndSetupUI()

        val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemDialogsReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(systemDialogsReceiver, filter)
        }
    }

    private fun goHome() {
        if (isGoingHome.getAndSet(true)) return // Prevent multiple calls

        try {
            // Record that we are leaving the locked app to ensure grace period triggers correctly
            lockedPackageNameFromIntent?.let {
                AppLockManager.setRecentlyLeftApp(it)
            }

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            
            // Finish activity after starting home intent to clear overlay
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error going home: ${e.message}")
            isGoingHome.set(false)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setupBiometricPromptInternal()
    }

    override fun onPostResume() {
        super.onPostResume()
        setupBiometricPromptInternal()
        if (appLockRepository.isBiometricAuthEnabled()) {
            triggerBiometricPrompt()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed - orientation: ${newConfig.orientation}")
    }

    private fun setupWindow() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SECURE or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        // Disable system gestures and navigation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            window.addFlags(flags)
        }

        val layoutParams = window.attributes
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        if (appLockRepository.shouldUseMaxBrightness()) {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }
        window.attributes = layoutParams

        // Immersive mode to hide system UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    private fun loadAppDetailsAndSetupUI() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pkgName = lockedPackageNameFromIntent!!
                val info = packageManager.getApplicationInfo(pkgName, 0)
                val label = packageManager.getApplicationLabel(info).toString()
                val icon = packageManager.getApplicationIcon(info)
                
                withContext(Dispatchers.Main) {
                    appName = label
                    appIcon = icon
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading app details: ${e.message}")
                withContext(Dispatchers.Main) {
                    appName = getString(R.string.default_app_name)
                }
            }
        }
        setupUI()
    }

    private fun setupUI() {
        val onPinAttemptCallback = { pin: String, isFinal: Boolean ->
            val correctPassword = appLockRepository.getPassword() ?: ""
            val isValid = pin == correctPassword
            
            if (isValid) {
                IntruderSelfieManager.resetFailedAttempts()
                lockedPackageNameFromIntent?.let { pkgName ->
                    AppLockManager.unlockApp(pkgName)
                    finishAfterTransition()
                }
            } else {
                // Proper failed attempt count:
                // 1. If it's a "Proceed" button click, it's always a failed attempt.
                // 2. If it's an auto-unlock check, only count if the length matches or exceeds the correct password length.
                if (isFinal || (pin.length >= correctPassword.length && correctPassword.isNotEmpty())) {
                    IntruderSelfieManager.recordFailedAttempt(this@PasswordOverlayActivity)
                }
            }
            isValid
        }

        val onPatternAttemptCallback = { pattern: String ->
            val isValid = appLockRepository.validatePattern(pattern)
            if (isValid) {
                IntruderSelfieManager.resetFailedAttempts()
                lockedPackageNameFromIntent?.let { pkgName ->
                    AppLockManager.unlockApp(pkgName)

                    finishAfterTransition()
                }
            } else {
                IntruderSelfieManager.recordFailedAttempt(this@PasswordOverlayActivity)
            }
            isValid
        }

        setContent {
            AppLockTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) { innerPadding ->
                    val lockType = remember { appLockRepository.getLockType() }
                    when (lockType) {
                        PreferencesRepository.LOCK_TYPE_PATTERN -> {
                            PatternLockScreen(
                                modifier = Modifier.padding(innerPadding),
                                fromMainActivity = false,
                                lockedAppName = appName,
                                lockedAppIcon = appIcon,
                                triggeringPackageName = triggeringPackageNameFromIntent,
                                onPatternAttempt = onPatternAttemptCallback,
                                onBiometricAuth = { triggerBiometricPrompt() },
                                onForgotPasscodeReset = { launchResetFlow() }
                            )
                        }

                        PreferencesRepository.LOCK_TYPE_PASSWORD -> {
                            PasswordOverlayScreen(
                                modifier = Modifier.padding(innerPadding),
                                showBiometricButton = appLockRepository.isBiometricAuthEnabled(),
                                fromMainActivity = false,
                                useTextPassword = true,
                                onBiometricAuth = { triggerBiometricPrompt() },
                                onAuthSuccess = { IntruderSelfieManager.resetFailedAttempts() },
                                lockedAppName = appName,
                                lockedAppIcon = appIcon,
                                triggeringPackageName = triggeringPackageNameFromIntent,
                                onForgotPasscodeReset = { launchResetFlow() },
                                onPinAttempt = onPinAttemptCallback
                            )
                        }

                        else -> {
                            PasswordOverlayScreen(
                                modifier = Modifier.padding(innerPadding),
                                showBiometricButton = appLockRepository.isBiometricAuthEnabled(),
                                fromMainActivity = false,
                                onBiometricAuth = { triggerBiometricPrompt() },
                                onAuthSuccess = { IntruderSelfieManager.resetFailedAttempts() },
                                lockedAppName = appName,
                                lockedAppIcon = appIcon,
                                triggeringPackageName = triggeringPackageNameFromIntent,
                                onForgotPasscodeReset = { launchResetFlow() },
                                onPinAttempt = onPinAttemptCallback
                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchResetFlow() {
        startActivity(Intent(this, dev.pranav.applock.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
        finish()
    }

    private fun setupBiometricPromptInternal() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt =
            BiometricPrompt(this@PasswordOverlayActivity, executor, authenticationCallbackInternal)

        val appNameForPrompt = appName.ifEmpty { getString(R.string.this_app) }
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_app_title, appNameForPrompt))
            .setSubtitle(getString(R.string.confirm_biometric_subtitle))
            .setNegativeButtonText(getString(R.string.use_pin_button))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .setConfirmationRequired(false)
            .build()
    }

    private val authenticationCallbackInternal =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricPromptShowingLocal = false
                AppLockManager.reportBiometricAuthFinished()
                
                // Only log unexpected errors, ignore cancellation during navigation
                if (errorCode != BiometricPrompt.ERROR_CANCELED && 
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Log.w(TAG, "Authentication error: $errString ($errorCode)")
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptShowingLocal = false
                IntruderSelfieManager.resetFailedAttempts()
                lockedPackageNameFromIntent?.let { pkgName ->
                    AppLockManager.temporarilyUnlockAppWithBiometrics(pkgName)
                }
                finishAfterTransition()
            }
        }

    override fun onResume() {
        super.onResume()
        movedToBackground = false
        isGoingHome.set(false)
        AppLockManager.isLockScreenShown.set(true)
        lifecycleScope.launch {
            applyUserPreferences()
        }
    }

    private fun applyUserPreferences() {
        if (appLockRepository.shouldUseMaxBrightness()) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            if (window.decorView.isAttachedToWindow) {
                windowManager.updateViewLayout(window.decorView, window.attributes)
            }
        }
    }

    fun triggerBiometricPrompt() {
        if (appLockRepository.isBiometricAuthEnabled() && !isGoingHome.get()) {
            AppLockManager.reportBiometricAuthStarted()
            isBiometricPromptShowingLocal = true
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Error calling biometricPrompt.authenticate: ${e.message}", e)
                isBiometricPromptShowingLocal = false
                AppLockManager.reportBiometricAuthFinished()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "User leave hint - going home from lock screen")
        // Allow going home when user presses home/recents
        goHome()
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations() && !isBiometricPromptShowingLocal && !movedToBackground && !isGoingHome.get()) {
            AppLockManager.isLockScreenShown.set(false)
            AppLockManager.reportBiometricAuthFinished()
            finish()
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        AppLockManager.isLockScreenShown.set(true)
    }

    override fun onStop() {
        super.onStop()
        movedToBackground = true
        AppLockManager.isLockScreenShown.set(false)
        if (!isChangingConfigurations() && !isFinishing && !isDestroyed) {
            AppLockManager.reportBiometricAuthFinished()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(systemDialogsReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        AppLockManager.isLockScreenShown.set(false)
        AppLockManager.reportBiometricAuthFinished()
        Log.d(TAG, "PasswordOverlayActivity onDestroy for $lockedPackageNameFromIntent")
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun PasswordOverlayScreen(
    modifier: Modifier = Modifier,
    showBiometricButton: Boolean = false,
    fromMainActivity: Boolean = false,
    onBiometricAuth: () -> Unit = {},
    onAuthSuccess: () -> Unit,
    lockedAppName: String? = null,
    lockedAppIcon: Drawable? = null,
    triggeringPackageName: String? = null,
    useTextPassword: Boolean = false,
    onForgotPasscodeReset: (() -> Unit)? = null,
    onPinAttempt: ((pin: String, isFinal: Boolean) -> Boolean)? = null
) {
    val appLockRepository = LocalContext.current.appLockRepository()
    val windowInfo = LocalWindowInfo.current

    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val isLandscape = screenWidth > screenHeight

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val passwordState = remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        var showRecoveryDialog by remember { mutableStateOf(false) }
        val minLength = 4

        if (showRecoveryDialog) {
            RecoveryKeyDialog(
                onDismiss = { showRecoveryDialog = false },
                onValidated = { onForgotPasscodeReset?.invoke() }
            )
        }

        if (useTextPassword) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppHeader(
                    fromMainActivity = fromMainActivity,
                    lockedAppName = lockedAppName,
                    lockedAppIcon = lockedAppIcon,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = passwordState.value,
                    onValueChange = {
                        passwordState.value = it
                        showError = false
                    },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.incorrect_password_error), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val valid = onPinAttempt?.invoke(passwordState.value, true) ?: false
                    if (valid) {
                        onAuthSuccess()
                    } else {
                        showError = true
                        passwordState.value = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
                TextButton(onClick = { showRecoveryDialog = true }) { Text("Forgot passcode?") }
                if (showBiometricButton) {
                    TextButton(onClick = onBiometricAuth) { Text("Use biometric") }
                }
            }
        } else if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppHeader(
                        fromMainActivity = fromMainActivity,
                        lockedAppName = lockedAppName,
                        lockedAppIcon = lockedAppIcon,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordIndicators(
                        passwordLength = passwordState.value.length,
                    )

                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.incorrect_pin_try_again),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    KeypadSection(
                        passwordState = passwordState,
                        minLength = minLength,
                        showBiometricButton = showBiometricButton,
                        fromMainActivity = fromMainActivity,
                        onBiometricAuth = onBiometricAuth,
                        onAuthSuccess = onAuthSuccess,
                        onPinAttempt = onPinAttempt,
                        onPasswordChange = {
                            showError = false

                            if (appLockRepository.isAutoUnlockEnabled()) {
                                onPinAttempt?.invoke(passwordState.value, false)
                            }
                        },
                        onPinIncorrect = { showError = true }
                    )
                    TextButton(onClick = { showRecoveryDialog = true }) { Text("Forgot passcode?") }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = if (fromMainActivity) 24.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Dynamic spacer for small screens
                val topSpacerHeight = if (screenHeightDp < 600.dp) 12.dp else 48.dp
                Spacer(modifier = Modifier.height(topSpacerHeight))

                AppHeader(
                    fromMainActivity = fromMainActivity,
                    lockedAppName = lockedAppName,
                    lockedAppIcon = lockedAppIcon,
                    style = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                        MaterialTheme.typography.titleLargeEmphasized
                    else
                        MaterialTheme.typography.headlineMediumEmphasized
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordIndicators(
                    passwordLength = passwordState.value.length,
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.incorrect_pin_try_again),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                KeypadSection(
                    passwordState = passwordState,
                    minLength = minLength,
                    showBiometricButton = showBiometricButton,
                    fromMainActivity = fromMainActivity,
                    onBiometricAuth = onBiometricAuth,
                    onAuthSuccess = onAuthSuccess,
                    onPinAttempt = onPinAttempt,
                    onPasswordChange = {
                        showError = false

                        if (appLockRepository.isAutoUnlockEnabled()) {
                            onPinAttempt?.invoke(passwordState.value, false)
                        }
                    },
                    onPinIncorrect = { showError = true }
                )
                TextButton(onClick = { showRecoveryDialog = true }) { Text("Forgot passcode?") }
            }
        }
    }

    if (fromMainActivity) {
        BackHandler {}
    }
}

@Composable
fun AppHeader(
    fromMainActivity: Boolean,
    lockedAppName: String?,
    lockedAppIcon: Drawable?,
    style: androidx.compose.ui.text.TextStyle
) {
    if (!fromMainActivity && !lockedAppName.isNullOrEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Continue to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (lockedAppIcon != null) {
                    val bitmap = remember(lockedAppIcon) {
                        val b = Bitmap.createBitmap(
                            lockedAppIcon.intrinsicWidth.coerceAtLeast(1),
                            lockedAppIcon.intrinsicHeight.coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(b)
                        lockedAppIcon.setBounds(0, 0, canvas.width, canvas.height)
                        lockedAppIcon.draw(canvas)
                        b.asImageBitmap()
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                
                Text(
                    text = lockedAppName,
                    style = style.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.enter_password_to_continue),
            style = style,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun PasswordIndicators(
    passwordLength: Int
) {
    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current

    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val screenWidthDp = configuration.screenWidthDp.dp
    val isLandscape = screenWidth > screenHeight

    val indicatorSize = remember(screenWidthDp) {
        when {
            screenWidthDp >= 900.dp -> 32.dp
            screenWidthDp >= 600.dp -> 28.dp
            isLandscape -> 26.dp
            else -> 22.dp
        }
    }

    val indicatorSpacing = remember(screenWidthDp) {
        when {
            screenWidthDp >= 900.dp -> 16.dp
            screenWidthDp >= 600.dp -> 14.dp
            isLandscape -> 12.dp
            else -> 8.dp
        }
    }

    val maxWidth = if (isLandscape) {
        minOf(screenWidthDp * 0.5f, 500.dp)
    } else {
        screenWidthDp * 0.85f
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(passwordLength) {
        if (passwordLength > 0) {
            lazyListState.animateScrollToItem(
                index = passwordLength - 1,
                scrollOffset = 0
            )
        }
    }

    Box(
        modifier = Modifier
            .width(maxWidth)
            .height(indicatorSize + 32.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(
                indicatorSpacing,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(passwordLength) { index ->
                key("digit_$index") {
                    val isNewest = index == passwordLength - 1
                    var animationTarget by remember { mutableStateOf(0f) }

                    LaunchedEffect(Unit) {
                        animationTarget = 1f
                    }

                    val animationProgress by animateFloatAsState(
                        targetValue = animationTarget,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        ),
                        label = "indicatorProgress"
                    )

                    val scale = if (isNewest && animationProgress < 1f) {
                        when {
                            animationProgress < 0.6f -> 1.1f + (1f - animationProgress) * 0.4f
                            animationProgress < 0.9f -> 1.1f + (1f - animationProgress) * 0.2f
                            else -> 1f
                        }
                    } else {
                        1f
                    }

                    val shape = when {
                        isNewest && animationProgress < 1f -> shapes[index % shapes.size].toShape()
                        else -> CircleShape
                    }

                    val color = MaterialTheme.colorScheme.primary

                    val collapseProgress = if (isNewest && animationProgress > 0.6f) {
                        ((animationProgress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val originalShapeScale = 1f - collapseProgress

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .size(indicatorSize)
                    ) {
                        if (collapseProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = color, shape = CircleShape)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = originalShapeScale
                                    scaleY = originalShapeScale
                                }
                                .background(color = color, shape = shape)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun KeypadSection(
    passwordState: MutableState<String>,
    minLength: Int,
    showBiometricButton: Boolean,
    fromMainActivity: Boolean = false,
    onBiometricAuth: () -> Unit,
    onAuthSuccess: () -> Unit,
    onPinAttempt: ((pin: String, isFinal: Boolean) -> Boolean)? = null,
    onPasswordChange: () -> Unit,
    onPinIncorrect: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val windowInfo = LocalWindowInfo.current

    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val isLandscape = screenWidth > screenHeight

    val horizontalPadding = remember(screenWidthDp, isLandscape) {
        if (isLandscape) {
            0.dp
        } else {
            screenWidthDp * 0.12f
        }
    }

    val buttonSpacing = remember(screenWidthDp, screenHeightDp, isLandscape) {
        if (isLandscape) {
            screenHeightDp * 0.015f
        } else {
            screenWidthDp * 0.02f
        }
    }

    val estimatedTopContentHeight = 220.dp
    val availableHeight = screenHeightDp - estimatedTopContentHeight

    val buttonSize =
        remember(
            screenWidthDp,
            screenHeightDp,
            isLandscape,
            buttonSpacing,
            horizontalPadding,
            showBiometricButton
        ) {
            val rows = if (showBiometricButton) 5f else 4f
            if (isLandscape) {
                val availableLandscapeHeight = screenHeightDp * 0.8f
                val totalVerticalSpacing = buttonSpacing * (rows - 1)
                val heightBasedSize = (availableLandscapeHeight - totalVerticalSpacing) / rows

                val availableWidth = (screenWidthDp * 0.45f)
                val totalHorizontalSpacing = buttonSpacing * 2
                val widthBasedSize = (availableWidth - totalHorizontalSpacing) / 3f

                minOf(heightBasedSize, widthBasedSize)
            } else {
                val availableWidth = screenWidthDp - (horizontalPadding * 2)
                val totalHorizontalSpacing = buttonSpacing * 2
                val widthBasedSize = (availableWidth - totalHorizontalSpacing) / 3.5f

                val totalVerticalSpacing = buttonSpacing * (rows - 1)
                val heightBasedSize =
                    (availableHeight - totalVerticalSpacing) / rows

                minOf(widthBasedSize, heightBasedSize)
            }
        }

    val onDigitKeyClick = remember(passwordState, minLength, onPasswordChange) {
        { key: String ->
            addDigitToPassword(
                passwordState,
                key,
                onPasswordChange
            )
        }
    }

    val disableHaptics = context.appLockRepository().shouldDisableHaptics()

    val onSpecialKeyClick = remember(
        passwordState,
        minLength,
        fromMainActivity,
        onAuthSuccess,
        onPinAttempt,
        context,
        onPasswordChange,
        onPinIncorrect
    ) {
        { key: String ->
            handleKeypadSpecialButtonLogic(
                key = key,
                passwordState = passwordState,
                minLength = minLength,
                fromMainActivity = fromMainActivity,
                onAuthSuccess = onAuthSuccess,
                onPinAttempt = onPinAttempt,
                context = context,
                onPasswordChange = onPasswordChange,
                onPinIncorrect = onPinIncorrect
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(buttonSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (isLandscape) {
            Modifier
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        } else {
            Modifier
                .padding(horizontal = horizontalPadding)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        }
    ) {
        KeypadRow(
            disableHaptics = disableHaptics,
            keys = listOf("1", "2", "3"),
            onKeyClick = onDigitKeyClick,
            buttonSize = buttonSize,
            buttonSpacing = buttonSpacing
        )
        KeypadRow(
            disableHaptics = disableHaptics,
            keys = listOf("4", "5", "6"),
            onKeyClick = onDigitKeyClick,
            buttonSize = buttonSize,
            buttonSpacing = buttonSpacing
        )
        KeypadRow(
            disableHaptics = disableHaptics,
            keys = listOf("7", "8", "9"),
            onKeyClick = onDigitKeyClick,
            buttonSize = buttonSize,
            buttonSpacing = buttonSpacing
        )
        KeypadRow(
            disableHaptics = disableHaptics,
            keys = listOf("backspace", "0", "proceed"),
            icons = listOf(Backspace, null, Icons.AutoMirrored.Rounded.KeyboardArrowRight),
            onKeyClick = onSpecialKeyClick,
            buttonSize = buttonSize,
            buttonSpacing = buttonSpacing
        )
        
        if (showBiometricButton) {
            FilledTonalIconButton(
                onClick = onBiometricAuth,
                modifier = Modifier.size(buttonSize),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = Fingerprint,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(buttonSize * 0.25f),
                    contentDescription = stringResource(R.string.biometric_authentication_cd),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private fun addDigitToPassword(
    passwordState: MutableState<String>,
    digit: String,
    onPasswordChange: () -> Unit
) {
    passwordState.value += digit
    onPasswordChange()
}

private fun handleKeypadSpecialButtonLogic(
    key: String,
    passwordState: MutableState<String>,
    minLength: Int,
    fromMainActivity: Boolean,
    onAuthSuccess: () -> Unit,
    onPinAttempt: ((pin: String, isFinal: Boolean) -> Boolean)?,
    context: Context,
    onPasswordChange: () -> Unit,
    onPinIncorrect: () -> Unit
) {
    val appLockRepository = context.appLockRepository()

    when (key) {
        "0" -> addDigitToPassword(passwordState, key, onPasswordChange)
        "backspace" -> {
            if (passwordState.value.isNotEmpty()) {
                passwordState.value = passwordState.value.dropLast(1)
                onPasswordChange()
            }
        }

        "proceed" -> {
            if (passwordState.value.length < minLength) {
                if (!appLockRepository.shouldDisableHaptics()) {
                    vibrate(context, 100)
                }
                passwordState.value = ""
                return
            }
            if (passwordState.value.length >= minLength) {
                if (fromMainActivity) {
                    val correctPassword = appLockRepository.getPassword() ?: ""
                    if (passwordState.value == correctPassword) {
                        IntruderSelfieManager.resetFailedAttempts()
                        onAuthSuccess()
                    } else {
                        IntruderSelfieManager.recordFailedAttempt(context)
                        passwordState.value = ""
                        if (!appLockRepository.shouldDisableHaptics()) {
                            vibrate(context, 100)
                        }
                        onPinIncorrect()
                    }
                } else {
                    onPinAttempt?.let { attempt ->
                        val pinWasCorrectAndProcessed = attempt(passwordState.value, true)
                        if (!pinWasCorrectAndProcessed) {
                            passwordState.value = ""
                            if (!appLockRepository.shouldDisableHaptics()) {
                                vibrate(context, 100)
                            }
                        }
                    } ?: run {
                        Log.e(
                            "PasswordOverlayScreen",
                            "onPinAttempt callback is null for app unlock path."
                        )
                        passwordState.value = ""
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KeypadRow(
    disableHaptics: Boolean = false,
    keys: List<String>,
    icons: List<ImageVector?> = emptyList(),
    onKeyClick: (String) -> Unit,
    buttonSize: Dp,
    buttonSpacing: Dp
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEachIndexed { index, key ->
            val interactionSource = remember { MutableInteractionSource() }

            val isPressed by interactionSource.collectIsPressedAsState()

            val targetColor = if (isPressed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                if (icons.isNotEmpty() && index < icons.size && icons[index] != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceBright
            }

            val animatedContainerColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 150),
                label = "ButtonContainerColorAnimation"
            )

            val normalTextSize = MaterialTheme.typography.headlineLargeEmphasized.fontSize

            val targetFontSize = if (isPressed) normalTextSize * 1.2f else normalTextSize

            val animatedFontSize by animateFloatAsState(
                targetValue = targetFontSize.value,
                animationSpec = tween(durationMillis = 100),
                label = "ButtonTextSizeAnimation"
            )

            FilledTonalButton(
                onClick = {
                    if (!disableHaptics) vibrate(context, 100)
                    onKeyClick(key)
                },
                modifier = Modifier.size(buttonSize),
                interactionSource = interactionSource,
                shapes = ButtonShapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(25),
                ),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = animatedContainerColor,
                ),
                elevation = ButtonDefaults.filledTonalButtonElevation()
            ) {
                val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

                if (icons.isNotEmpty() && index < icons.size && icons[index] != null) {
                    Icon(
                        imageVector = icons[index]!!,
                        contentDescription = key,
                        modifier = Modifier.size(buttonSize * 0.45f),
                        tint = contentColor
                    )
                } else {
                    Text(
                        text = key,
                        color = contentColor,
                        style = MaterialTheme.typography.headlineLargeEmphasized.copy(
                            fontSize = animatedFontSize.sp
                        ),
                    )
                }
            }
        }
    }
}
