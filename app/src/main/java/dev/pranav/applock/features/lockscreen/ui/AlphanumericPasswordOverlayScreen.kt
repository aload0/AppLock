package dev.pranav.applock.features.lockscreen.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.pranav.applock.R
import dev.pranav.applock.core.utils.SecurityUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.ui.icons.Fingerprint

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlphanumericPasswordOverlayScreen(
    modifier: Modifier = Modifier,
    showBiometricButton: Boolean = false,
    fromMainActivity: Boolean = false,
    showCloseButton: Boolean = false,
    onClose: () -> Unit = {},
    onBiometricAuth: () -> Unit = {},
    onAuthSuccess: () -> Unit,
    lockedAppName: String? = null,
    triggeringPackageName: String? = null,
    onPasswordAttempt: ((password: String) -> Boolean)? = null
) {
    val appLockRepository = LocalContext.current.appLockRepository()
    var passwordState by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val minLength = 4

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showCloseButton) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.alphanumeric_password_overlay_close_cd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                        stringResource(R.string.alphanumeric_overlay_continue_to_app, lockedAppName ?: "")
                    else
                        stringResource(R.string.enter_password_to_continue),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = passwordState,
                    onValueChange = { input ->
                        passwordState = SecurityUtils.sanitizePassword(input)
                        showError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.password_hint)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (passwordState.length >= minLength) {
                                performVerification(
                                    passwordState,
                                    fromMainActivity,
                                    appLockRepository,
                                    onAuthSuccess,
                                    onPasswordAttempt,
                                    onIncorrect = {
                                        passwordState = ""
                                        showError = true
                                    }
                                )
                            }
                        }
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
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBiometricButton) {
                        FilledTonalIconButton(
                            onClick = onBiometricAuth,
                            modifier = Modifier.size(56.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(
                                imageVector = Fingerprint,
                                modifier = Modifier.size(24.dp),
                                contentDescription = stringResource(R.string.biometric_authentication_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (passwordState.length >= minLength) {
                                performVerification(
                                    passwordState,
                                    fromMainActivity,
                                    appLockRepository,
                                    onAuthSuccess,
                                    onPasswordAttempt,
                                    onIncorrect = {
                                        passwordState = ""
                                        showError = true
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.verify_button))
                    }
                }
            }
        }
    }

    BackHandler { }
}

private fun performVerification(
    passwordState: String,
    fromMainActivity: Boolean,
    appLockRepository: dev.pranav.applock.data.repository.AppLockRepository,
    onAuthSuccess: () -> Unit,
    onPasswordAttempt: ((password: String) -> Boolean)?,
    onIncorrect: () -> Unit
) {
    if (fromMainActivity) {
        if (appLockRepository.validatePassword(passwordState)) {
            onAuthSuccess()
        } else {
            onIncorrect()
        }
    } else {
        onPasswordAttempt?.let { attempt ->
            if (attempt(passwordState)) {
                onAuthSuccess()
            } else {
                onIncorrect()
            }
        }
    }
}
