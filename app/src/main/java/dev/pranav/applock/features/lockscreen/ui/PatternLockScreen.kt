package dev.pranav.applock.features.lockscreen.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.vibrate
import dev.pranav.applock.ui.icons.Fingerprint

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun PatternLockScreen(
    modifier: Modifier = Modifier,
    fromMainActivity: Boolean = false,
    showCloseButton: Boolean = false,
    onClose: () -> Unit = {},
    lockedAppName: String? = null,
    triggeringPackageName: String? = null,
    onPatternAttempt: ((pattern: String) -> Boolean)? = null,
    onBiometricAuth: (() -> Unit)? = null
) {
    val appLockRepository = LocalContext.current.appLockRepository()
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current

    val screenWidth = windowInfo.containerSize.width
    val screenHeight = windowInfo.containerSize.height
    val isLandscape = screenWidth > screenHeight

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var showError by remember { mutableStateOf(false) }

            @Suppress("ASSIGNED_BUT_NOT_ACCESSED_WARNING")
            var errorShakeOffset by remember { mutableStateOf(0f) }

            val shakeAnimation by animateFloatAsState(
                targetValue = errorShakeOffset,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "shake"
            )

            val patternIds = remember { mutableStateOf<List<Int>>(emptyList()) }

            val lockCallback = object: LockCallback {
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
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                                    stringResource(R.string.pattern_lock_continue_to_app, lockedAppName)
                                else
                                    stringResource(R.string.enter_pattern_to_continue),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
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
                        }

                        if (appLockRepository.isBiometricAuthEnabled() && onBiometricAuth != null) {
                            FilledTonalIconButton(
                                onClick = { onBiometricAuth() },
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
                        Text(
                            text = if (!fromMainActivity && !lockedAppName.isNullOrEmpty())
                                stringResource(R.string.pattern_lock_continue_to_app, lockedAppName)
                            else
                                stringResource(R.string.enter_pattern_to_continue),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (appLockRepository.isBiometricAuthEnabled() && onBiometricAuth != null) {
                            FilledTonalIconButton(
                                onClick = { onBiometricAuth() },
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

                            Spacer(modifier = Modifier.height(16.dp))
                        }

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
                    }
                }
            }

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
                        contentDescription = stringResource(R.string.pattern_lock_close_cd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
