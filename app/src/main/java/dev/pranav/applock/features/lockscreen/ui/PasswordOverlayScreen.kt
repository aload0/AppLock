package dev.pranav.applock.features.lockscreen.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.pranav.applock.R
import dev.pranav.applock.core.ui.shapes
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.vibrate
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.services.AppLockManager
import dev.pranav.applock.ui.icons.Backspace
import dev.pranav.applock.ui.icons.Fingerprint
import dev.pranav.applock.ui.theme.AppLockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class PasswordOverlayActivity : FragmentActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var appLockRepository: AppLockRepository
    internal var lockedPackageNameFromIntent: String? = null
    internal var triggeringPackageNameFromIntent: String? = null

    private var isBiometricPromptShowingLocal = false
    private var movedToBackground = false
    private var appName: String = ""

    private val TAG = "PasswordOverlayActivity"

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

        onBackPressedDispatcher.addCallback(this) {
            // Prevent back navigation to maintain security
        }

        setupWindow()
        loadAppNameAndSetupUI()
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

    
    private fun setupWindow() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SECURE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }


        val layoutParams = window.attributes
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
        
        if (appLockRepository.shouldUseMaxBrightness()) {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }
        window.attributes = layoutParams
    }

    private fun loadAppNameAndSetupUI() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(lockedPackageNameFromIntent!!, 0)
                ).toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading app name: ${e.message}")
                appName = getString(R.string.default_app_name)
            }
        }
        setupUI()
    }

    private fun setupUI() {
        val onPinAttemptCallback = { pin: String ->
            val isValid = appLockRepository.validatePassword(pin)
            if (isValid) {
                lockedPackageNameFromIntent?.let { pkgName ->
                    AppLockManager.unlockApp(pkgName)

                    finishAfterTransition()
                }
            }
            isValid
        }

        val onPatternAttemptCallback = { pattern: String ->
            val isValid = appLockRepository.validatePattern(pattern)
            if (isValid) {
                lockedPackageNameFromIntent?.let { pkgName ->
                    AppLockManager.unlockApp(pkgName)

                    finishAfterTransition()
                }
            }
            isValid
        }

        setContent {
            AppLockTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                ) { innerPadding ->
                    val lockType = remember { appLockRepository.getLockType() }
                    when (lockType) {
                        PreferencesRepository.LOCK_TYPE_PATTERN -> {
                            PatternLockScreen(
                                modifier = Modifier.padding(innerPadding),
                                fromMainActivity = false,
                                lockedAppName = appName,
                                triggeringPackageName = triggeringPackageNameFromIntent,
                                onPatternAttempt = onPatternAttemptCallback
                            )
                        }

                        else -> {
                            PasswordOverlayScreen(
                                modifier = Modifier.padding(innerPadding),
                                showBiometricButton = appLockRepository.isBiometricAuthEnabled(),
                                fromMainActivity = false,
                                onBiometricAuth = { triggerBiometricPrompt() },
                                onAuthSuccess = {},
                                lockedAppName = appName,
                                triggeringPackageName = triggeringPackageNameFromIntent,
                                onPinAttempt = onPinAttemptCallback
                            )
                        }
                    }
                }
            }
        }
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
                Log.w(TAG, "Authentication error: $errString ($errorCode)")
                
                // If user cancels authentication, finish the activity to remove the overlay
                // This prevents the overlay from staying visible over other apps
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptShowingLocal = false
                lockedPackageNameFromIntent?.let { pkgName ->
                    AppLockManager.temporarilyUnlockAppWithBiometrics(pkgName)
                    val intent = packageManager.getLaunchIntentForPackage(pkgName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "No launch intent found for package: $pkgName")
                    }
                }
                finishAfterTransition()
            }
        }

    override fun onResume() {
        super.onResume()
        movedToBackground = false
        AppLockManager.isLockScreenShown.set(true) // Set to true when activity is visible
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
        if (appLockRepository.isBiometricAuthEnabled()) {
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

    override fun onPause() {
        super.onPause()
        movedToBackground = true
        AppLockManager.isLockScreenShown.set(false)
        
        // Cancel biometric prompt if it's showing
        if (isBiometricPromptShowingLocal) {
            try {
                biometricPrompt.cancelAuthentication()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error canceling biometric prompt: ${e.message}")
            }
            isBiometricPromptShowingLocal = false
        }
        
        if (!isFinishing && !isDestroyed) {
            AppLockManager.reportBiometricAuthFinished()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
    triggeringPackageName: String? = null,
    onPinAttempt: ((pin: String) -> Boolean)? = null
) {
    val appLockRepository = LocalContext.current.appLockRepository()
    val windowInfo = LocalWindowInfo.current

    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val isLandscape = screenWidth > screenHeight

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val passwordState = remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        val minLength = 4

        if (isLandscape) {
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
                    Text(
                        text = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                            "Continue to $lockedAppName"
                        else
                            stringResource(R.string.enter_password_to_continue),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

//                    if (!fromMainActivity && !triggeringPackageName.isNullOrEmpty()) {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = triggeringPackageName,
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
//                            textAlign = TextAlign.Center
//                        )
//                    }

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
                                onPinAttempt?.invoke(passwordState.value)
                            }
                        },
                        onPinIncorrect = { showError = true }
                    )
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
                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                        "Continue to $lockedAppName"
                    else
                        stringResource(R.string.enter_password_to_continue),
                    style = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                        MaterialTheme.typography.titleLargeEmphasized
                    else
                        MaterialTheme.typography.headlineMediumEmphasized,
                    textAlign = TextAlign.Center
                )

//                if (!fromMainActivity && !triggeringPackageName.isNullOrEmpty()) {
//                    Text(
//                        text = triggeringPackageName,
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
//                        textAlign = TextAlign.Center
//                    )
//                }

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
                            onPinAttempt?.invoke(passwordState.value)
                        }
                    },
                    onPinIncorrect = { showError = true }
                )
            }
        }
    }

    if (fromMainActivity) {
        BackHandler {}
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
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
    onPinAttempt: ((pin: String) -> Boolean)? = null,
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

    val buttonSize =
        remember(screenWidthDp, screenHeightDp, isLandscape, buttonSpacing, horizontalPadding) {
            if (isLandscape) {
                val availableHeight = screenHeightDp * 0.8f
                val totalVerticalSpacing = buttonSpacing * 3
                val heightBasedSize = (availableHeight - totalVerticalSpacing) / 4f

                val availableWidth = (screenWidthDp * 0.45f)
                val totalHorizontalSpacing = buttonSpacing * 2
                val widthBasedSize = (availableWidth - totalHorizontalSpacing) / 3f

                minOf(heightBasedSize, widthBasedSize)
            } else {
                val availableWidth = screenWidthDp - (horizontalPadding * 2)
                val totalSpacing = buttonSpacing * 2
                (availableWidth - totalSpacing) / 3.5f
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
        }
    ) {
        if (showBiometricButton) {
            FilledTonalIconButton(
                onClick = onBiometricAuth,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(40),
            ) {
                Icon(
                    imageVector = Fingerprint,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentDescription = stringResource(R.string.biometric_authentication_cd),
                    tint = MaterialTheme.colorScheme.surfaceTint
                )
            }
        }
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
    onPinAttempt: ((pin: String) -> Boolean)?,
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
                    if (appLockRepository.validatePassword(passwordState.value)) {
                        onAuthSuccess()
                    } else {
                        passwordState.value = ""
                        if (!appLockRepository.shouldDisableHaptics()) {
                            vibrate(context, 100)
                        }
                        onPinIncorrect()
                    }
                } else {
                    onPinAttempt?.let { attempt ->
                        val pinWasCorrectAndProcessed = attempt(passwordState.value)
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
                        style = MaterialTheme.typography.headlineLargeEmphasized.copy(
                            fontSize = animatedFontSize.sp
                        ),
                    )
                }
            }
        }
    }
}
