package dev.pranav.applock.features.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.utils.LocaleManager
import dev.pranav.applock.core.utils.SecurityUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.features.lockscreen.ui.KeypadSection
import dev.pranav.applock.features.lockscreen.ui.PasswordIndicators
import dev.pranav.applock.features.lockscreen.ui.PatternLockScreen
import dev.pranav.applock.ui.theme.AppLockTheme

class AdminDisableActivity : ComponentActivity() {
    private lateinit var appLockRepository: AppLockRepository
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var deviceAdminComponentName: ComponentName

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.wrap(newBase ?: return))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        deviceAdminComponentName = ComponentName(this, DeviceAdmin::class.java)

        appLockRepository = appLockRepository()

        // Set up back press callback to prevent admin disabling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val deviceAdmin = DeviceAdmin()
                deviceAdmin.setPasswordVerified(this@AdminDisableActivity, false)
                finish()
            }
        })

        setContent {
            AppLockTheme {
                Scaffold { padding ->
                    val lockType = appLockRepository.getLockType()
                    when (lockType) {
                        PreferencesRepository.LOCK_TYPE_PATTERN -> {
                            AdminDisablePatternScreen(
                                modifier = Modifier.padding(padding),
                                onPatternVerified = {
                                    val deviceAdmin = DeviceAdmin()
                                    deviceAdmin.setPasswordVerified(this@AdminDisableActivity, true)

                                    Toast.makeText(
                                        this@AdminDisableActivity,
                                        R.string.password_verified_admin,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    appLockRepository.setAntiUninstallEnabled(false)
                                    finish()
                                },
                                validatePattern = { inputPattern ->
                                    appLockRepository.validatePattern(inputPattern)
                                        .also { isValid ->
                                            if (!isValid) {
                                                Toast.makeText(
                                                    this@AdminDisableActivity,
                                                    R.string.incorrect_pattern_try_again,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                },
                                onCancel = {
                                    val deviceAdmin = DeviceAdmin()
                                    deviceAdmin.setPasswordVerified(
                                        this@AdminDisableActivity,
                                        false
                                    )
                                    finish()
                                }
                            )
                        }

                        PreferencesRepository.LOCK_TYPE_PASSWORD -> {
                            AdminDisablePasswordScreen(
                                modifier = Modifier.padding(padding),
                                onPasswordVerified = {
                                    val deviceAdmin = DeviceAdmin()
                                    deviceAdmin.setPasswordVerified(this@AdminDisableActivity, true)

                                    Toast.makeText(
                                        this@AdminDisableActivity,
                                        R.string.password_verified_admin,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    appLockRepository.setAntiUninstallEnabled(false)
                                    finish()
                                },
                                onCancel = {
                                    val deviceAdmin = DeviceAdmin()
                                    deviceAdmin.setPasswordVerified(
                                        this@AdminDisableActivity,
                                        false
                                    )
                                    finish()
                                },
                                validatePassword = { inputPassword ->
                                    appLockRepository.validatePassword(inputPassword)
                                        .also { isValid ->
                                            if (!isValid) {
                                                Toast.makeText(
                                                    this@AdminDisableActivity,
                                                    R.string.incorrect_password_try_again,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }
                            )
                        }

                        else -> {
                            AdminDisableScreen(
                                modifier = Modifier.padding(padding),
                                onPasswordVerified = {
                                    val deviceAdmin = DeviceAdmin()
                                    deviceAdmin.setPasswordVerified(this@AdminDisableActivity, true)

                                    Toast.makeText(
                                        this@AdminDisableActivity,
                                        R.string.password_verified_admin,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    appLockRepository.setAntiUninstallEnabled(false)
                                    finish()
                                },
                                onCancel = {
                                    val deviceAdmin = DeviceAdmin()
                                    deviceAdmin.setPasswordVerified(
                                        this@AdminDisableActivity,
                                        false
                                    )
                                    finish()
                                },
                                validatePassword = { inputPassword ->
                                    appLockRepository.validatePassword(inputPassword)
                                        .also { isValid ->
                                            if (!isValid) {
                                                Toast.makeText(
                                                    this@AdminDisableActivity,
                                                    R.string.incorrect_pin_try_again,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDisableScreen(
    modifier: Modifier = Modifier,
    onPasswordVerified: () -> Unit,
    onCancel: () -> Unit,
    validatePassword: (String) -> Boolean
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val passwordState = remember { mutableStateOf("") }
        val showError = remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.unlock_to_disable_admin),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordIndicators(
                passwordLength = passwordState.value.length
            )

            if (showError.value) {
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
                minLength = 4,
                showBiometricButton = false,
                fromMainActivity = false,
                onBiometricAuth = {},
                onAuthSuccess = {},
                onPinAttempt = { pin ->
                    val isValid = validatePassword(pin)
                    if (isValid) {
                        onPasswordVerified()
                    } else {
                        onCancel()
                    }
                    isValid
                },
                onPasswordChange = { showError.value = false },
                onPinIncorrect = { showError.value = true }
            )
        }
    }
}

@Composable
fun AdminDisablePasswordScreen(
    modifier: Modifier = Modifier,
    onPasswordVerified: () -> Unit,
    onCancel: () -> Unit,
    validatePassword: (String) -> Boolean
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var passwordState by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        var passwordVisible by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.unlock_to_disable_admin),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = passwordState,
                onValueChange = { input ->
                    passwordState = SecurityUtils.sanitizePassword(input)
                    showError = false
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text(stringResource(R.string.password_hint)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                isError = showError,
                singleLine = true
            )

            if (showError) {
                Text(
                    text = stringResource(R.string.incorrect_password_try_again),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel_button))
                }

                Button(
                    onClick = {
                        if (validatePassword(passwordState)) {
                            onPasswordVerified()
                        } else {
                            showError = true
                            passwordState = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.verify_button))
                }
            }
        }
    }
}

@Composable
fun AdminDisablePatternScreen(
    modifier: Modifier = Modifier,
    onPatternVerified: () -> Unit,
    onCancel: () -> Unit,
    validatePattern: (String) -> Boolean
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.unlock_to_disable_admin),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            PatternLockScreen(
                modifier = Modifier.weight(1f),
                fromMainActivity = false,
                lockedAppName = null,
                triggeringPackageName = null,
                onPatternAttempt = { pattern ->
                    val isValid = validatePattern(pattern)
                    if (isValid) {
                        onPatternVerified()
                    }
                    isValid
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    }
}
