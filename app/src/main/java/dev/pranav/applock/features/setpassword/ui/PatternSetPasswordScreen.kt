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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.mrhwsn.composelock.Dot
import com.mrhwsn.composelock.LockCallback
import com.mrhwsn.composelock.PatternLock
import dev.pranav.applock.AppLockApplication
import dev.pranav.applock.R
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.RecoveryKeyManager
import dev.pranav.applock.core.utils.vibrate
import dev.pranav.applock.data.repository.PreferencesRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatternSetPasswordScreen(
    navController: NavController,
    isFirstTimeSetup: Boolean
) {
    var patternState by remember { mutableStateOf("") }
    var confirmPatternState by remember { mutableStateOf("") }
    var isConfirmationMode by remember { mutableStateOf(false) }

    var showMismatchError by remember { mutableStateOf(false) }
    var showMinLengthError by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var generatedRecoveryKey by remember { mutableStateOf("") }

    val minLength = 4
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val appLockRepository = remember {
        (context.applicationContext as? AppLockApplication)?.appLockRepository
    }

    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val isLandscape = screenWidth > screenHeight

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
                    // In this context, we just allow them to set a new pattern
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    BackHandler {
        if (isFirstTimeSetup) {
            if (isConfirmationMode) {
                isConfirmationMode = false
            } else {
                Toast.makeText(context, R.string.set_pin_to_continue_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                activity?.finish()
            }
        }
    }

    fun switchToPinMethod() {
        navController.navigate(Screen.SetPassword.route) {
            popUpTo(Screen.SetPasswordPattern.route) { inclusive = true }
        }
    }

    fun submitPattern() {
        val currentPattern = when {
            isConfirmationMode -> confirmPatternState
            else -> patternState
        }

        if (currentPattern.length < minLength) {
            showMinLengthError = true
            return
        }

        when {
            !isConfirmationMode -> {
                isConfirmationMode = true
                showMinLengthError = false
            }

            else -> {
                if (patternState == confirmPatternState) {
                    appLockRepository?.setLockType(PreferencesRepository.LOCK_TYPE_PATTERN)
                    appLockRepository?.setPattern(patternState)
                    Toast.makeText(
                        context,
                        context.getString(R.string.password_set_successfully_toast),
                        Toast.LENGTH_SHORT
                    ).show()

                    generatedRecoveryKey = RecoveryKeyManager.generateRecoveryKey()
                    appLockRepository?.setRecoveryKey(generatedRecoveryKey)
                    showRecoveryDialog = true
                } else {
                    showMismatchError = true
                    confirmPatternState = ""
                }
            }
        }
    }

    if (showRecoveryDialog) {
        RecoveryKeyGeneratedDialog(
            recoveryKey = generatedRecoveryKey,
            onDismiss = {
                showRecoveryDialog = false
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.SetPasswordPattern.route) { inclusive = true }
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
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            isConfirmationMode -> "Confirm pattern"
                            else -> "Create a pattern"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (showMismatchError) {
                        Text(
                            text = "Incorrect Pattern",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (showMinLengthError) {
                        Text(
                            text = "Patter length should be at least 4",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.enter_pattern_to_continue),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.8f),
                        textAlign = TextAlign.Center
                    )

                    if (!isFirstTimeSetup) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { launchDeviceCredentialAuth() }) {
                            Text(stringResource(R.string.reset_using_device_password_button))
                        }
                    }

                    if (isFirstTimeSetup && !isConfirmationMode) {
                        TextButton(onClick = { switchToPinMethod() }) {
                            Text("Use PIN")
                        }
                        TextButton(onClick = {
                            navController.navigate(Screen.SetPasswordText.route) {
                                popUpTo(Screen.SetPasswordPattern.route) { inclusive = true }
                            }
                        }) {
                            Text("Use Password")
                        }
                    }

                    if (isConfirmationMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                isConfirmationMode = false
                                patternState = ""
                                confirmPatternState = ""
                                showMismatchError = false
                                showMinLengthError = false
                            }
                        ) {
                            Text(stringResource(R.string.start_over_button))
                        }
                    }
                }

                PatternLock(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    dimension = 3,
                    sensitivity = 50f,
                    dotsColor = MaterialTheme.colorScheme.primary,
                    dotsSize = 14f,
                    linesColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    linesStroke = 6f,
                    animationDuration = 120,
                    callback = object : LockCallback {
                        override fun onStart(dot: Dot) {
                            if (!appLockRepository!!.shouldDisableHaptics()) {
                                vibrate(context, 10)
                            }
                        }

                        override fun onDotConnected(dot: Dot) {
                            if (!appLockRepository!!.shouldDisableHaptics()) {
                                vibrate(context, 10)
                            }
                        }

                        override fun onResult(result: List<Dot>) {
                            val patternString = result.joinToString("") { it.id.toString() }
                            if (isConfirmationMode) {
                                confirmPatternState = patternString
                            } else {
                                patternState = patternString
                            }
                            submitPattern()
                        }
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            isConfirmationMode -> stringResource(R.string.confirm_new_pin_label)
                            else -> stringResource(R.string.create_new_pin_label)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    if (showMismatchError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.pins_dont_match_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (showMinLengthError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Patter length should be at least 4",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.enter_pattern_to_continue),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.alpha(0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PatternLock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        dimension = 3,
                        sensitivity = 50f,
                        dotsColor = MaterialTheme.colorScheme.primary,
                        dotsSize = 16f,
                        linesColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        linesStroke = 7f,
                        animationDuration = 120,
                        callback = object : LockCallback {
                            override fun onStart(dot: Dot) {
                                if (!appLockRepository!!.shouldDisableHaptics()) {
                                    vibrate(context, 10)
                                }
                            }

                            override fun onDotConnected(dot: Dot) {
                                if (!appLockRepository!!.shouldDisableHaptics()) {
                                    vibrate(context, 10)
                                }
                            }

                            override fun onResult(result: List<Dot>) {
                                val patternString = result.joinToString("") { it.id.toString() }
                                if (isConfirmationMode) {
                                    confirmPatternState = patternString
                                } else {
                                    patternState = patternString
                                }
                                submitPattern()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isConfirmationMode) {
                        TextButton(onClick = { switchToPinMethod() }) {
                            Text("Use PIN")
                        }
                        TextButton(onClick = {
                            navController.navigate(Screen.SetPasswordText.route)
                        }) {
                            Text("Use Password")
                        }
                    }

                    if (!isFirstTimeSetup) {
                        TextButton(onClick = { launchDeviceCredentialAuth() }) {
                            Text(stringResource(R.string.reset_using_device_password_button))
                        }
                    }

                    if (isConfirmationMode) {
                        TextButton(
                            onClick = {
                                isConfirmationMode = false
                                patternState = ""
                                confirmPatternState = ""
                                showMismatchError = false
                                showMinLengthError = false
                            }
                        ) {
                            Text(stringResource(R.string.start_over_button))
                        }
                    }
                }
            }
        }
    }
}
