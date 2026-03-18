@file:OptIn(ExperimentalMaterial3Api::class)

package dev.pranav.applock.features.setpassword.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import dev.pranav.applock.AppLockApplication
import dev.pranav.applock.R
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.RecoveryKeyManager
import dev.pranav.applock.data.repository.PreferencesRepository

private fun isStrongPassword(password: String): Boolean {
    return password.length >= 8 &&
        password.any { it.isUpperCase() } &&
        password.any { it.isLowerCase() } &&
        password.any { it.isDigit() } &&
        password.any { !it.isLetterOrDigit() }
}

@Composable
fun SetPasswordTextScreen(navController: NavController, isFirstTimeSetup: Boolean) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val repo = remember { (context.applicationContext as? AppLockApplication)?.appLockRepository }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

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
                    // Reset current password requirement or navigate directly
                    // In this context, we just allow them to set a new password without current one
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    BackHandler {
        if (isFirstTimeSetup) {
            Toast.makeText(context, R.string.set_pin_to_continue_toast, Toast.LENGTH_SHORT).show()
        } else if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            activity?.finish()
        }
    }

    if (showRecoveryDialog) {
        RecoveryKeyGeneratedDialog(
            recoveryKey = repo?.getRecoveryKey().orEmpty(),
            onDismiss = {
                showRecoveryDialog = false
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.SetPasswordText.route) { inclusive = true }
                    if (isFirstTimeSetup) {
                        popUpTo(Screen.AppIntro.route) { inclusive = true }
                    }
                }
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (isFirstTimeSetup) "Set Password" else "Change Password") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Password must contain at least 8 characters, 1 uppercase, 1 lowercase, 1 number and 1 symbol.")
            
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; error = null },
                label = { Text("New password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                when {
                    !isStrongPassword(newPassword) -> error = "Password does not meet complexity requirements"
                    newPassword != confirmPassword -> error = "Passwords do not match"
                    else -> {
                        repo?.setLockType(PreferencesRepository.LOCK_TYPE_PASSWORD)
                        repo?.setPassword(newPassword)
                        val recoveryKey = RecoveryKeyManager.generateRecoveryKey()
                        repo?.setRecoveryKey(recoveryKey)
                        showRecoveryDialog = true
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Save password")
            }

            if (!isFirstTimeSetup) {
                TextButton(onClick = { launchDeviceCredentialAuth() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reset_using_device_password_button))
                }
            }

            TextButton(onClick = {
                navController.navigate(if (isFirstTimeSetup) Screen.SetPassword.route else Screen.ChangePasswordPin.route)
            }) { Text("Use PIN") }
            TextButton(onClick = {
                navController.navigate(if (isFirstTimeSetup) Screen.SetPasswordPattern.route else Screen.ChangePasswordPattern.route)
            }) { Text("Use Pattern") }
        }
    }
}
