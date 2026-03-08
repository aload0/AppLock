package dev.pranav.applock.features.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
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
                                    
                                    devicePolicyManager.removeActiveAdmin(deviceAdminComponentName)
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

                                    devicePolicyManager.removeActiveAdmin(deviceAdminComponentName)
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
        }
    }
}
