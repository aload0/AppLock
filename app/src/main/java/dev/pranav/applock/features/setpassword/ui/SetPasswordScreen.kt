package dev.pranav.applock.features.setpassword.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import dev.pranav.applock.AppLockApplication
import dev.pranav.applock.R
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.RecoveryKeyManager
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.features.lockscreen.ui.KeypadRow
import dev.pranav.applock.features.lockscreen.ui.PasswordIndicators
import dev.pranav.applock.ui.icons.Backspace

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun SetPasswordScreen(
    navController: NavController,
    isFirstTimeSetup: Boolean
) {
    var passwordState by remember { mutableStateOf("") }
    var confirmPasswordState by remember { mutableStateOf("") }
    var isConfirmationMode by remember { mutableStateOf(false) }

    var isVerifyOldPasswordMode by remember { mutableStateOf(!isFirstTimeSetup) }

    var showMismatchError by remember { mutableStateOf(false) }
    var showLengthError by remember { mutableStateOf(false) }
    var showInvalidOldPasswordError by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var generatedRecoveryKey by remember { mutableStateOf("") }
    val minLength = 4

    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val appLockRepository = remember {
        (context.applicationContext as? AppLockApplication)?.appLockRepository
    }

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

    BackHandler {
        if (isFirstTimeSetup) {
            Toast.makeText(context, R.string.set_pin_to_continue_toast, Toast.LENGTH_SHORT).show()
        } else {
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                activity?.finish()
            }
        }
    }

    val fragmentActivity = LocalActivity.current as? androidx.fragment.app.FragmentActivity

    fun launchDeviceCredentialAuth() {
        if (fragmentActivity == null) return
        val executor = ContextCompat.getMainExecutor(context)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.authenticate_to_reset_pin_title))
            .setSubtitle(context.getString(R.string.use_device_pin_pattern_password_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        val biometricPrompt = BiometricPrompt(
            fragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isVerifyOldPasswordMode = false
                    passwordState = ""
                    confirmPasswordState = ""
                    showInvalidOldPasswordError = false
                }

            })
        biometricPrompt.authenticate(promptInfo)
    }

    if (showRecoveryDialog) {
        RecoveryKeyGeneratedDialog(
            recoveryKey = generatedRecoveryKey,
            onDismiss = {
                showRecoveryDialog = false
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.SetPassword.route) { inclusive = true }
                    if (isFirstTimeSetup) {
                        popUpTo(Screen.AppIntro.route) { inclusive = true }
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            if (isFirstTimeSetup && !isLandscape) {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                isVerifyOldPasswordMode -> stringResource(R.string.enter_current_pin_title)
                                isConfirmationMode -> stringResource(R.string.confirm_pin_title)
                                else -> stringResource(R.string.set_new_pin_title)
                            },
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .padding(end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            isVerifyOldPasswordMode -> stringResource(R.string.enter_current_pin_label)
                            isConfirmationMode -> stringResource(R.string.confirm_new_pin_label)
                            else -> stringResource(R.string.create_new_pin_label)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showMismatchError) {
                        Text(
                            text = stringResource(R.string.pins_dont_match_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    if (showLengthError) {
                        Text(
                            text = stringResource(R.string.pin_min_length_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    if (showInvalidOldPasswordError) {
                        Text(
                            text = stringResource(R.string.incorrect_pin_try_again),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    val currentPassword = when {
                        isVerifyOldPasswordMode -> passwordState
                        isConfirmationMode -> confirmPasswordState
                        else -> passwordState
                    }

                    PasswordIndicators(
                        passwordLength = currentPassword.length
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isVerifyOldPasswordMode -> stringResource(R.string.enter_current_pin_label)
                            isConfirmationMode -> stringResource(R.string.re_enter_new_pin_confirm_label)
                            else -> stringResource(R.string.tooltip_create_pin_min_length)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.8f),
                        textAlign = TextAlign.Center
                    )

                    if (isVerifyOldPasswordMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { launchDeviceCredentialAuth() }) {
                            Text(stringResource(R.string.reset_using_device_password_button))
                        }
                    }

                    if (isVerifyOldPasswordMode || isConfirmationMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                if (isVerifyOldPasswordMode) {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    } else {
                                        activity?.finish()
                                    }
                                } else {
                                    isConfirmationMode = false
                                    if (!isFirstTimeSetup) {
                                        isVerifyOldPasswordMode = true
                                    }
                                }
                                passwordState = ""
                                confirmPasswordState = ""
                                showMismatchError = false
                                showLengthError = false
                                showInvalidOldPasswordError = false
                            }
                        ) {
                            Text(
                                if (isVerifyOldPasswordMode) stringResource(R.string.cancel_button) else stringResource(
                                    R.string.start_over_button
                                )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(buttonSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val onKeyClick: (String) -> Unit = { key ->
                        val currentActivePassword = when {
                            isVerifyOldPasswordMode -> passwordState
                            isConfirmationMode -> confirmPasswordState
                            else -> passwordState
                        }
                        val updatePassword: (String) -> Unit = when {
                            isVerifyOldPasswordMode -> { newPass -> passwordState = newPass }
                            isConfirmationMode -> { newPass -> confirmPasswordState = newPass }
                            else -> { newPass -> passwordState = newPass }
                        }

                        when (key) {
                            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                                updatePassword(currentActivePassword + key)
                            }

                            "backspace" -> {
                                if (currentActivePassword.isNotEmpty()) {
                                    updatePassword(currentActivePassword.dropLast(1))
                                }
                                showMismatchError = false
                                showLengthError = false
                                showInvalidOldPasswordError = false
                            }

                            "proceed" -> {
                                if (currentActivePassword.length >= minLength) {
                                    when {
                                        isVerifyOldPasswordMode -> {
                                            if (appLockRepository!!.validatePassword(passwordState)) {
                                                isVerifyOldPasswordMode = false
                                                passwordState = ""
                                                showInvalidOldPasswordError = false
                                            } else {
                                                showInvalidOldPasswordError = true
                                                passwordState = ""
                                            }
                                        }

                                        !isConfirmationMode -> {
                                            isConfirmationMode = true
                                            showLengthError = false
                                        }

                                        else -> {
                                            if (passwordState == confirmPasswordState) {
                                                appLockRepository?.setPassword(passwordState)
                                                appLockRepository?.setLockType(PreferencesRepository.LOCK_TYPE_PIN)
                                                generatedRecoveryKey = RecoveryKeyManager.generateRecoveryKey()
                                                appLockRepository?.setRecoveryKey(generatedRecoveryKey)
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.password_set_successfully_toast),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showRecoveryDialog = true
                                            } else {
                                                showMismatchError = true
                                                confirmPasswordState = ""
                                            }
                                        }
                                    }
                                } else {
                                    showLengthError = true
                                }
                            }
                        }
                    }

                    val disableHaptics = appLockRepository!!.shouldDisableHaptics()

                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("1", "2", "3"),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )
                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("4", "5", "6"),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )
                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("7", "8", "9"),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )

                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("backspace", "0", "proceed"),
                        icons = listOf(
                            Backspace,
                            null,
                            if (isConfirmationMode || isVerifyOldPasswordMode) Icons.Default.Check else Icons.AutoMirrored.Rounded.KeyboardArrowRight
                        ),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            isVerifyOldPasswordMode -> stringResource(R.string.enter_current_pin_label)
                            isConfirmationMode -> stringResource(R.string.confirm_new_pin_label)
                            else -> stringResource(R.string.create_new_pin_label)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                if (showMismatchError) {
                    Text(
                        text = stringResource(R.string.pins_dont_match_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                if (showLengthError) {
                    Text(
                        text = stringResource(R.string.pin_min_length_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                if (showInvalidOldPasswordError) {
                    Text(
                        text = stringResource(R.string.incorrect_pin_try_again),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                val currentPassword = when {
                    isVerifyOldPasswordMode -> passwordState
                    isConfirmationMode -> confirmPasswordState
                    else -> passwordState
                }

                PasswordIndicators(
                    passwordLength = currentPassword.length
                )

                Text(
                    text = when {
                        isVerifyOldPasswordMode -> stringResource(R.string.enter_current_pin_label)
                        isConfirmationMode -> stringResource(R.string.re_enter_new_pin_confirm_label)
                        else -> stringResource(R.string.tooltip_create_pin_min_length)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.alpha(0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isVerifyOldPasswordMode) {
                    TextButton(onClick = { launchDeviceCredentialAuth() }) {
                        Text(stringResource(R.string.reset_using_device_password_button))
                    }
                }

                if (isVerifyOldPasswordMode || isConfirmationMode) {
                    TextButton(
                        onClick = {
                            if (isVerifyOldPasswordMode) {
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                } else {
                                    activity?.finish()
                                }
                            } else {
                                isConfirmationMode = false
                                if (!isFirstTimeSetup) {
                                    isVerifyOldPasswordMode = true
                                }
                            }
                            passwordState = ""
                            confirmPasswordState = ""
                            showMismatchError = false
                            showLengthError = false
                            showInvalidOldPasswordError = false
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            if (isVerifyOldPasswordMode) stringResource(R.string.cancel_button) else stringResource(
                                R.string.start_over_button
                            )
                        )
                    }
                }

                TextButton(
                    onClick = {
                        navController.navigate(Screen.SetPasswordPattern.route)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(stringResource(R.string.use_pattern_button))
                }
                TextButton(
                    onClick = {
                        navController.navigate(Screen.SetPasswordText.route)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Use Password")
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(buttonSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                ) {
                    val onKeyClick: (String) -> Unit = { key ->
                        val currentActivePassword = when {
                            isVerifyOldPasswordMode -> passwordState
                            isConfirmationMode -> confirmPasswordState
                            else -> passwordState
                        }
                        val updatePassword: (String) -> Unit = when {
                            isVerifyOldPasswordMode -> { newPass -> passwordState = newPass }
                            isConfirmationMode -> { newPass -> confirmPasswordState = newPass }
                            else -> { newPass -> passwordState = newPass }
                        }

                        when (key) {
                            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                                updatePassword(currentActivePassword + key)
                            }

                            "backspace" -> {
                                if (currentActivePassword.isNotEmpty()) {
                                    updatePassword(currentActivePassword.dropLast(1))
                                }
                                showMismatchError = false
                                showLengthError = false
                                showInvalidOldPasswordError = false
                            }

                            "proceed" -> {
                                if (currentActivePassword.length >= minLength) {
                                    when {
                                        isVerifyOldPasswordMode -> {
                                            if (appLockRepository!!.validatePassword(passwordState)) {
                                                isVerifyOldPasswordMode = false
                                                passwordState = ""
                                                showInvalidOldPasswordError = false
                                            } else {
                                                showInvalidOldPasswordError = true
                                                passwordState = ""
                                            }
                                        }

                                        !isConfirmationMode -> {
                                            isConfirmationMode = true
                                            showLengthError = false
                                        }

                                        else -> {
                                            if (passwordState == confirmPasswordState) {
                                                appLockRepository?.setLockType(PreferencesRepository.LOCK_TYPE_PIN)
                                                appLockRepository?.setPassword(passwordState)
                                                generatedRecoveryKey = RecoveryKeyManager.generateRecoveryKey()
                                                appLockRepository?.setRecoveryKey(generatedRecoveryKey)
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.password_set_successfully_toast),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showRecoveryDialog = true
                                            } else {
                                                showMismatchError = true
                                                confirmPasswordState = ""
                                            }
                                        }
                                    }
                                } else {
                                    showLengthError = true
                                }
                            }
                        }
                    }

                    val disableHaptics = appLockRepository!!.shouldDisableHaptics()

                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("1", "2", "3"),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )
                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("4", "5", "6"),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )
                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("7", "8", "9"),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )

                    KeypadRow(
                        disableHaptics = disableHaptics,
                        keys = listOf("backspace", "0", "proceed"),
                        icons = listOf(
                            Backspace,
                            null,
                            if (isConfirmationMode || isVerifyOldPasswordMode) Icons.Default.Check else Icons.AutoMirrored.Rounded.KeyboardArrowRight
                        ),
                        onKeyClick = onKeyClick,
                        buttonSize = buttonSize,
                        buttonSpacing = buttonSpacing
                    )
                }
            }
        }
    }
}
