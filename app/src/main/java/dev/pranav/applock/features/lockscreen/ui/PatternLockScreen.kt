package dev.pranav.applock.features.lockscreen.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrhwsn.composelock.Dot
import com.mrhwsn.composelock.LockCallback
import com.mrhwsn.composelock.PatternLock
import dev.pranav.applock.R
import dev.pranav.applock.core.utils.IntruderSelfieManager
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.vibrate
import dev.pranav.applock.ui.icons.Fingerprint

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun PatternLockScreen(
    modifier: Modifier = Modifier,
    fromMainActivity: Boolean = false,
    lockedAppName: String? = null,
    lockedAppIcon: Drawable? = null,
    triggeringPackageName: String? = null,
    onPatternAttempt: ((pattern: String) -> Boolean)? = null,
    onBiometricAuth: (() -> Unit)? = null,
    onForgotPasscodeReset: (() -> Unit)? = null
) {
    val appLockRepository = LocalContext.current.appLockRepository()
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current

    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val isLandscape = screenWidth > screenHeight

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var showError by remember { mutableStateOf(false) }

        @Suppress("ASSIGNED_BUT_NOT_ACCESSED_WARNING")
        var errorShakeOffset by remember { mutableStateOf(0f) }

        val shakeAnimation by animateFloatAsState(
            targetValue = errorShakeOffset,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "shake"
        )

        val patternIds = remember { mutableStateOf<List<Int>>(emptyList()) }

        val lockCallback = object : LockCallback {
            override fun onStart(dot: Dot) {
                showError = false
                if (!appLockRepository.shouldDisableHaptics()) {
                    vibrate(context, 10)
                }
            }

            override fun onDotConnected(dot: Dot) {
                if (!appLockRepository.shouldDisableHaptics()) {
                    vibrate(context, 10)
                }
            }

            override fun onResult(result: List<Dot>) {
                patternIds.value = result.map { it.id }
                val patternString = result.joinToString("") { it.id.toString() }

                val isValid = onPatternAttempt?.invoke(patternString) ?: false
                if (!isValid) {
                    showError = true
                    errorShakeOffset = 10f
                    // recordFailedAttempt is already handled in PasswordOverlayActivity's onPatternAttemptCallback
                } else {
                    IntruderSelfieManager.resetFailedAttempts()
                }
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppHeader(
                        fromMainActivity = fromMainActivity,
                        lockedAppName = lockedAppName,
                        lockedAppIcon = lockedAppIcon,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.incorrect_pattern_try_again),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    TextButton(onClick = { onForgotPasscodeReset?.invoke() }) { Text("Forgot passcode?") }

                    if (appLockRepository.isBiometricAuthEnabled() && onBiometricAuth != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        FilledTonalIconButton(
                            onClick = { onBiometricAuth() },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Fingerprint,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentDescription = stringResource(R.string.biometric_authentication_cd),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                PatternLock(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .graphicsLayer(translationX = shakeAnimation),
                    dimension = 3,
                    sensitivity = 50f,
                    dotsColor = MaterialTheme.colorScheme.primary,
                    dotsSize = 14f,
                    linesColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    linesStroke = 6f,
                    animationDuration = 120,
                    callback = lockCallback
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LaunchedEffect(Unit) {
                    if (appLockRepository.isBiometricAuthEnabled() && onBiometricAuth != null) {
                        onBiometricAuth()
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppHeader(
                        fromMainActivity = fromMainActivity,
                        lockedAppName = lockedAppName,
                        lockedAppIcon = lockedAppIcon,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.incorrect_pattern_try_again),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    PatternLock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer(translationX = shakeAnimation),
                        dimension = 3,
                        sensitivity = 50f,
                        dotsColor = MaterialTheme.colorScheme.primary,
                        dotsSize = 16f,
                        linesColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        linesStroke = 7f,
                        animationDuration = 120,
                        callback = lockCallback
                    )

                    TextButton(onClick = { onForgotPasscodeReset?.invoke() }) { Text("Forgot passcode?") }

                    if (appLockRepository.isBiometricAuthEnabled() && onBiometricAuth != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        FilledTonalIconButton(
                            onClick = { onBiometricAuth() },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Fingerprint,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                contentDescription = stringResource(R.string.biometric_authentication_cd),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
