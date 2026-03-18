package dev.pranav.applock.features.settings.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.BiometricStatus
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.core.utils.getBiometricStatus
import dev.pranav.applock.core.utils.hasUsagePermission
import dev.pranav.applock.core.utils.isAccessibilityServiceEnabled
import dev.pranav.applock.core.utils.openAccessibilitySettings
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.AppThemeMode
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.features.admin.AdminDisableActivity
import dev.pranav.applock.services.ExperimentalAppLockService
import dev.pranav.applock.services.ShizukuAppLockService
import dev.pranav.applock.ui.icons.*
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.File
import kotlin.math.abs

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appLockRepository = remember { AppLockRepository(context) }

    // Pre-fetch strings to avoid LocalContext.current resource querying in lambdas
    val shizukuPermissionGrantedMsg = stringResource(R.string.settings_screen_shizuku_permission_granted)
    val shizukuPermissionRequiredMsg = stringResource(R.string.settings_screen_shizuku_permission_required_desc)
    val deviceAdminExplanation = stringResource(R.string.main_screen_device_admin_explanation)
    val exportLogsError = stringResource(R.string.settings_screen_export_logs_error)
    val shizukuNotRunningMsg = stringResource(R.string.settings_screen_shizuku_not_running_toast)
    val usagePermissionMsg = stringResource(R.string.settings_screen_usage_permission_toast)

    var showUnlockTimeDialog by remember { mutableStateOf(false) }

    val shizukuPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, shizukuPermissionGrantedMsg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, shizukuPermissionRequiredMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // Intruder Selfie states
    var intruderSelfieEnabled by remember { mutableStateOf(appLockRepository.isIntruderSelfieEnabled()) }
    var intruderSelfieAttempts by remember { mutableIntStateOf(appLockRepository.getIntruderSelfieAttempts()) }
    var hasSelfies by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val selfieDir = File(context.filesDir, "intruder_selfies")
        hasSelfies = selfieDir.exists() && (selfieDir.listFiles()?.isNotEmpty() ?: false)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            intruderSelfieEnabled = true
            appLockRepository.setIntruderSelfieEnabled(true)
        } else {
            Toast.makeText(context, "Camera permission is required for Intruder Selfie", Toast.LENGTH_SHORT).show()
        }
    }

    // Reactive states from repository flows
    val amoledModeEnabled by appLockRepository.amoledModeFlow()
        .collectAsState(initial = appLockRepository.isAmoledModeEnabled())
    val dynamicColorEnabled by appLockRepository.dynamicColorFlow()
        .collectAsState(initial = appLockRepository.isDynamicColorEnabled())
    val appThemeMode by appLockRepository.appThemeModeFlow()
        .collectAsState(initial = appLockRepository.getAppThemeMode())

    var autoUnlock by remember { mutableStateOf(appLockRepository.isAutoUnlockEnabled()) }
    var useMaxBrightness by remember { mutableStateOf(appLockRepository.shouldUseMaxBrightness()) }
    var useBiometricAuth by remember { mutableStateOf(appLockRepository.isBiometricAuthEnabled()) }
    var unlockTimeDuration by remember { mutableStateOf(appLockRepository.getUnlockTimeDuration()) }
    var antiUninstallEnabled by remember { mutableStateOf(appLockRepository.isAntiUninstallEnabled()) }
    
    var antiUninstallAdminSettings by remember { mutableStateOf(appLockRepository.isAntiUninstallAdminSettingsEnabled()) }
    var antiUninstallUsageStats by remember { mutableStateOf(appLockRepository.isAntiUninstallUsageStatsEnabled()) }
    var antiUninstallAccessibility by remember { mutableStateOf(appLockRepository.isAntiUninstallAccessibilityEnabled()) }
    var antiUninstallOverlay by remember { mutableStateOf(appLockRepository.isAntiUninstallOverlayEnabled()) }
    var preventAllAppUninstall by remember { mutableStateOf(appLockRepository.isPreventAllAppUninstallEnabled()) }

    var disableHapticFeedback by remember { mutableStateOf(appLockRepository.shouldDisableHaptics()) }
    var loggingEnabled by remember { mutableStateOf(appLockRepository.isLoggingEnabled()) }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showDeviceAdminDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    // Sync other states with repository on resume (non-flow states)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                autoUnlock = appLockRepository.isAutoUnlockEnabled()
                useMaxBrightness = appLockRepository.shouldUseMaxBrightness()
                useBiometricAuth = appLockRepository.isBiometricAuthEnabled()
                unlockTimeDuration = appLockRepository.getUnlockTimeDuration()
                antiUninstallEnabled = appLockRepository.isAntiUninstallEnabled()
                antiUninstallAdminSettings = appLockRepository.isAntiUninstallAdminSettingsEnabled()
                antiUninstallUsageStats = appLockRepository.isAntiUninstallUsageStatsEnabled()
                antiUninstallAccessibility = appLockRepository.isAntiUninstallAccessibilityEnabled()
                antiUninstallOverlay = appLockRepository.isAntiUninstallOverlayEnabled()
                preventAllAppUninstall = appLockRepository.isPreventAllAppUninstallEnabled()
                disableHapticFeedback = appLockRepository.shouldDisableHaptics()
                loggingEnabled = appLockRepository.isLoggingEnabled()
                intruderSelfieEnabled = appLockRepository.isIntruderSelfieEnabled()
                intruderSelfieAttempts = appLockRepository.getIntruderSelfieAttempts()
                
                val selfieDir = File(context.filesDir, "intruder_selfies")
                hasSelfies = selfieDir.exists() && (selfieDir.listFiles()?.isNotEmpty() ?: false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val biometricManager = remember { BiometricManager.from(context) }
    var isSettingsAuthorized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when (val biometricStatus = getBiometricStatus(context)) {
            BiometricStatus.Available -> {
                val activity = context as? androidx.fragment.app.FragmentActivity ?: return@LaunchedEffect
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        isSettingsAuthorized = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!isSettingsAuthorized) navController.popBackStack()
                    }
                })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Settings")
                    .setSubtitle("Biometric authentication required")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Cancel")
                    .build()
                prompt.authenticate(promptInfo)
            }
            is BiometricStatus.Unavailable -> {
                Toast.makeText(context, biometricStatus.message, Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
    }

    if (!isSettingsAuthorized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Authenticate with biometrics to open Settings")
        }
        return
    }

    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Effect to handle anti-uninstall settings reset when disabled
    LaunchedEffect(antiUninstallEnabled) {
        if (!antiUninstallEnabled) {
            antiUninstallAdminSettings = false
            antiUninstallUsageStats = false
            antiUninstallAccessibility = false
            antiUninstallOverlay = false
            appLockRepository.setAntiUninstallAdminSettingsEnabled(false)
            appLockRepository.setAntiUninstallUsageStatsEnabled(false)
            appLockRepository.setAntiUninstallAccessibilityEnabled(false)
            appLockRepository.setAntiUninstallOverlayEnabled(false)
        }
    }

    if (showUnlockTimeDialog) {
        UnlockTimeDurationDialog(
            currentDuration = unlockTimeDuration,
            onDismiss = { showUnlockTimeDialog = false },
            onConfirm = { newDuration ->
                unlockTimeDuration = newDuration
                appLockRepository.setUnlockTimeDuration(newDuration)
                showUnlockTimeDialog = false
            }
        )
    }

    if (showPermissionDialog) {
        PermissionRequiredDialog(
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                showPermissionDialog = false
                showDeviceAdminDialog = true
            }
        )
    }

    if (showDeviceAdminDialog) {
        DeviceAdminDialog(
            onDismiss = { showDeviceAdminDialog = false },
            onConfirm = {
                showDeviceAdminDialog = false
                val component = ComponentName(context, DeviceAdmin::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        deviceAdminExplanation
                    )
                }
                context.startActivity(intent)
            }
        )
    }

    if (showAccessibilityDialog) {
        AccessibilityDialog(
            onDismiss = { showAccessibilityDialog = false },
            onConfirm = {
                showAccessibilityDialog = false
                openAccessibilitySettings(context)
                val dpm =
                    context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val component = ComponentName(context, DeviceAdmin::class.java)
                if (!dpm.isAdminActive(component)) {
                    showDeviceAdminDialog = true
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_screen_back_cd)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SectionTitle(text = stringResource(R.string.settings_screen_security_title))

                SettingsCard(index = 0, listSize = 3) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_auto_unlock_title),
                        summary = stringResource(R.string.settings_screen_auto_unlock_desc),
                        icon = Icons.Default.LockOpen,
                        checked = autoUnlock,
                        onCheckedChange = {
                            autoUnlock = it
                            appLockRepository.setAutoUnlockEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 1, listSize = 3) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_biometric_auth_title),
                        summary = stringResource(R.string.settings_screen_biometric_auth_desc),
                        icon = Icons.Default.Fingerprint,
                        checked = useBiometricAuth,
                        enabled = isBiometricAvailable,
                        onCheckedChange = {
                            useBiometricAuth = it
                            appLockRepository.setBiometricAuthEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 2, listSize = 4) {
                    ClickableItem(
                        title = "Change Lock",
                        summary = when (appLockRepository.getLockType()) {
                            PreferencesRepository.LOCK_TYPE_PATTERN -> "Update your unlock pattern"
                            PreferencesRepository.LOCK_TYPE_PASSWORD -> "Update your unlock password"
                            else -> "Update your unlock PIN"
                        },
                        icon = Icons.Default.Lock,
                        onClick = {
                            navController.navigate(Screen.ChangePassword.route)
                        }
                    )
                }

                SettingsCard(index = 3, listSize = 4) {
                    SwitchItem(
                        title = "Prevent all app uninstall",
                        summary = "Require protection before any app uninstall attempt",
                        icon = Icons.Outlined.Security,
                        checked = preventAllAppUninstall,
                        onCheckedChange = {
                            preventAllAppUninstall = it
                            appLockRepository.setPreventAllAppUninstallEnabled(it)
                        }
                    )
                }

                SectionTitle(text = "Intruder Selfie")

                SettingsCard(index = 0, listSize = 3) {
                    SwitchItem(
                        title = "Enable Intruder Selfie",
                        summary = "Capture a photo after failed unlock attempts",
                        icon = Icons.Default.CameraAlt,
                        checked = intruderSelfieEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    intruderSelfieEnabled = true
                                    appLockRepository.setIntruderSelfieEnabled(true)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                intruderSelfieEnabled = false
                                appLockRepository.setIntruderSelfieEnabled(false)
                            }
                        }
                    )
                }

                SettingsCard(index = 1, listSize = 3) {
                    FailedAttemptsSpinnerItem(
                        selectedAttempts = intruderSelfieAttempts,
                        enabled = intruderSelfieEnabled,
                        onAttemptsSelected = { attempts ->
                            intruderSelfieAttempts = attempts
                            appLockRepository.setIntruderSelfieAttempts(attempts)
                        }
                    )
                }

                SettingsCard(index = 2, listSize = 3) {
                    ClickableItem(
                        title = "Show Intruder Selfies",
                        summary = "View captured photos of intruders",
                        icon = Icons.Default.PhotoLibrary,
                        enabled = hasSelfies,
                        onClick = {
                            navController.navigate(Screen.IntruderSelfies.route)
                        }
                    )
                }

                SectionTitle(text = stringResource(R.string.settings_screen_anti_uninstall_title))

                SettingsCard(index = 0, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_anti_uninstall_title),
                        summary = stringResource(R.string.settings_screen_anti_uninstall_desc),
                        icon = Icons.Outlined.Security,
                        checked = antiUninstallEnabled,
                        onCheckedChange = {
                            antiUninstallEnabled = it
                            appLockRepository.setAntiUninstallEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 1, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.permission_warning_device_admin_title),
                        summary = stringResource(R.string.permission_warning_device_admin_desc),
                        icon = Icons.Default.AdminPanelSettings,
                        checked = antiUninstallAdminSettings,
                        enabled = antiUninstallEnabled,
                        onCheckedChange = {
                            if (it) {
                                val dpm =
                                    context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                val component = ComponentName(context, DeviceAdmin::class.java)
                                if (!dpm.isAdminActive(component)) {
                                    showPermissionDialog = true
                                    return@SwitchItem
                                }
                            }
                            antiUninstallAdminSettings = it
                            appLockRepository.setAntiUninstallAdminSettingsEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 2, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.permission_warning_usage_stats_title),
                        summary = stringResource(R.string.permission_warning_usage_stats_desc),
                        icon = Icons.Default.QueryStats,
                        checked = antiUninstallUsageStats,
                        enabled = antiUninstallEnabled,
                        onCheckedChange = {
                            if (it && !context.hasUsagePermission()) {
                                val intent =
                                    Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                return@SwitchItem
                            }
                            antiUninstallUsageStats = it
                            appLockRepository.setAntiUninstallUsageStatsEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 3, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.permission_warning_accessibility_title),
                        summary = stringResource(R.string.permission_warning_accessibility_desc),
                        icon = Icons.Default.Accessibility,
                        checked = antiUninstallAccessibility,
                        enabled = antiUninstallEnabled,
                        onCheckedChange = {
                            if (it && !context.isAccessibilityServiceEnabled()) {
                                showAccessibilityDialog = true
                                return@SwitchItem
                            }
                            antiUninstallAccessibility = it
                            appLockRepository.setAntiUninstallAccessibilityEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 4, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.permission_warning_overlay_title),
                        summary = stringResource(R.string.permission_warning_overlay_desc),
                        icon = Icons.Default.Layers,
                        checked = antiUninstallOverlay,
                        enabled = antiUninstallEnabled,
                        onCheckedChange = {
                            antiUninstallOverlay = it
                            appLockRepository.setAntiUninstallOverlayEnabled(it)
                        }
                    )
                }

                SectionTitle(text = stringResource(R.string.settings_screen_lock_screen_customization_title))

                SettingsCard(index = 0, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_max_brightness_title),
                        summary = stringResource(R.string.settings_screen_max_brightness_desc),
                        icon = Icons.Default.BrightnessHigh,
                        checked = useMaxBrightness,
                        onCheckedChange = {
                            useMaxBrightness = it
                            appLockRepository.setUseMaxBrightness(it)
                        }
                    )
                }

                SettingsCard(index = 1, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_amoled_mode_title),
                        summary = stringResource(R.string.settings_screen_amoled_mode_desc),
                        icon = Icons.Default.DarkMode,
                        checked = amoledModeEnabled,
                        onCheckedChange = {
                            appLockRepository.setAmoledModeEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 2, listSize = 5) {
                    SwitchItem(
                        title = "Use Dynamic Theme",
                        summary = "Apply dynamic colors from your wallpaper (Android 12+)",
                        icon = Icons.Default.Palette,
                        checked = dynamicColorEnabled,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        onCheckedChange = {
                            appLockRepository.setDynamicColorEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 3, listSize = 5) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_haptic_feedback_title),
                        summary = stringResource(R.string.settings_screen_haptic_feedback_desc),
                        icon = Icons.Default.Vibration,
                        checked = disableHapticFeedback,
                        onCheckedChange = {
                            disableHapticFeedback = it
                            appLockRepository.setDisableHaptics(it)
                        }
                    )
                }

                SettingsCard(index = 4, listSize = 5) {
                    ClickableItem(
                        title = stringResource(R.string.settings_screen_unlock_duration_title),
                        summary = when (unlockTimeDuration) {
                            0 -> stringResource(R.string.settings_screen_unlock_duration_dialog_option_immediate)
                            1 -> stringResource(
                                R.string.settings_screen_unlock_duration_dialog_option_minute,
                                unlockTimeDuration
                            )
                            60 -> stringResource(R.string.settings_screen_unlock_duration_dialog_option_hour)
                            Integer.MAX_VALUE -> "Until Screen Off"
                            else -> stringResource(
                                R.string.settings_screen_unlock_duration_summary_minutes,
                                unlockTimeDuration
                            )
                        },
                        icon = Icons.Default.Timer,
                        onClick = { showUnlockTimeDialog = true }
                    )
                }

                SectionTitle(text = stringResource(R.string.settings_screen_app_theme_title))
                AppThemeModeCard(
                    selectedThemeMode = appThemeMode,
                    onThemeModeSelected = {
                        appLockRepository.setAppThemeMode(it)
                    }
                )

                BackendSelectionCard(
                    appLockRepository = appLockRepository,
                    context = context,
                    shizukuPermissionLauncher = shizukuPermissionLauncher,
                    shizukuNotRunningMsg = shizukuNotRunningMsg,
                    usagePermissionMsg = usagePermissionMsg
                )

                SectionTitle(text = "App Details")

                SettingsCard(index = 0, listSize = 3) {
                    ClickableItem(
                        title = "App Version",
                        summary = try {
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            ).versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        },
                        icon = Icons.Default.Info,
                        onClick = {}
                    )
                }

                SettingsCard(index = 1, listSize = 3) {
                    SwitchItem(
                        title = stringResource(R.string.settings_screen_logging_title),
                        summary = stringResource(R.string.settings_screen_logging_summary),
                        icon = Icons.Outlined.BugReport,
                        checked = loggingEnabled,
                        onCheckedChange = {
                            loggingEnabled = it
                            appLockRepository.setLoggingEnabled(it)
                        }
                    )
                }

                SettingsCard(index = 2, listSize = 3) {
                    ClickableItem(
                        title = "Export Logs",
                        summary = "Save app logs to your Downloads folder",
                        icon = Icons.Default.Download,
                        onClick = {
                            val appName = context.getString(R.string.app_name)
                            val exportedFile = LogUtils.exportLogsToDownloads(appName)
                            if (exportedFile != null) {
                                Toast.makeText(context, "Logs exported: $exportedFile", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, exportLogsError, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                SectionTitle(text = stringResource(R.string.settings_screen_links_section))

                SettingsCard(index = 0, listSize = 3) {
                    LinkItem(
                        title = stringResource(R.string.settings_screen_developer_email_ap),
                        icon = Icons.Default.Email,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_SENDTO,
                                "mailto:ap0803apap@gmail.com".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                SettingsCard(index = 1, listSize = 3) {
                    LinkItem(
                        title = stringResource(R.string.settings_screen_source_code),
                        icon = Icons.Default.Code,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/ap0803apap-sketch".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                SettingsCard(index = 2, listSize = 3) {
                    LinkItem(
                        title = stringResource(R.string.settings_screen_report_issue),
                        icon = Icons.Default.BugReport,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/ap0803apap-sketch".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingsCard(
    index: Int,
    listSize: Int,
    content: @Composable () -> Unit
) {
    val shape = when {
        listSize == 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == listSize - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        content()
    }
}

@Composable
fun SwitchItem(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun ClickableItem(
    title: String,
    summary: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun FailedAttemptsSpinnerItem(
    selectedAttempts: Int,
    enabled: Boolean,
    onAttemptsSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val attemptsOptions = listOf(1, 2, 3, 4, 5)

    Box {
        ListItem(
            modifier = Modifier.clickable(enabled = enabled) { expanded = true },
            headlineContent = {
                Text(
                    text = "Failed attempts",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            },
            supportingContent = {
                Text(
                    text = "Take photo after $selectedAttempts failed attempt(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            attemptsOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text("$option attempt(s)") },
                    onClick = {
                        onAttemptsSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LinkItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun UnlockTimeDurationDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val durations = listOf(0, 1, 5, 10, 30, 60, Integer.MAX_VALUE)
    var selectedDuration by remember { mutableIntStateOf(currentDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_screen_unlock_duration_dialog_title)) },
        text = {
            val durationsScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(durationsScrollState)
                    .selectableGroup()
            ) {
                durations.forEach { duration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedDuration == duration,
                                onClick = { selectedDuration = duration },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDuration == duration,
                            onClick = null
                        )
                        Text(
                            text = when (duration) {
                                0 -> stringResource(R.string.settings_screen_unlock_duration_dialog_option_immediate)
                                1 -> stringResource(
                                    R.string.settings_screen_unlock_duration_dialog_option_minute,
                                    duration
                                )
                                60 -> stringResource(R.string.settings_screen_unlock_duration_dialog_option_hour)
                                Integer.MAX_VALUE -> "Until Screen Off"
                                else -> stringResource(
                                    R.string.settings_screen_unlock_duration_summary_minutes,
                                    duration
                                )
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDuration) }) {
                Text(stringResource(R.string.confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun BackendSelectionCard(
    appLockRepository: AppLockRepository,
    context: Context,
    shizukuPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    shizukuNotRunningMsg: String,
    usagePermissionMsg: String
) {
    var selectedBackend by remember { mutableStateOf(appLockRepository.getBackendImplementation()) }

    Column {
        SectionTitle(text = stringResource(R.string.settings_screen_backend_implementation_title))

        Column {
            BackendImplementation.entries.forEachIndexed { index, backend ->
                SettingsCard(
                    index = index,
                    listSize = BackendImplementation.entries.size
                ) {
                    BackendSelectionItem(
                        backend = backend,
                        isSelected = selectedBackend == backend,
                        onClick = {
                            when (backend) {
                                BackendImplementation.SHIZUKU -> {
                                    if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
                                        if (Shizuku.isPreV11()) {
                                            shizukuPermissionLauncher.launch(ShizukuProvider.PERMISSION)
                                        } else if (Shizuku.pingBinder()) {
                                            Shizuku.requestPermission(423)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                shizukuNotRunningMsg,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        selectedBackend = backend
                                        appLockRepository.setBackendImplementation(
                                            BackendImplementation.SHIZUKU
                                        )
                                        context.startService(
                                            Intent(context, ShizukuAppLockService::class.java)
                                        )
                                    }
                                }
                                BackendImplementation.USAGE_STATS -> {
                                    if (!context.hasUsagePermission()) {
                                        val intent =
                                            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        Toast.makeText(
                                            context,
                                            usagePermissionMsg,
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@BackendSelectionItem
                                    }
                                    selectedBackend = backend
                                    appLockRepository.setBackendImplementation(BackendImplementation.USAGE_STATS)
                                    context.startService(
                                        Intent(context, ExperimentalAppLockService::class.java)
                                    )
                                }
                                BackendImplementation.ACCESSIBILITY -> {
                                    if (!context.isAccessibilityServiceEnabled()) {
                                        openAccessibilitySettings(context)
                                        return@BackendSelectionItem
                                    }
                                    selectedBackend = backend
                                    appLockRepository.setBackendImplementation(BackendImplementation.ACCESSIBILITY)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackendSelectionItem(
    backend: BackendImplementation,
    isSelected: Boolean,
    onClick: (() -> Unit)?
) {
    ListItem(
        modifier = Modifier
            .clickable { onClick?.invoke() }
            .padding(vertical = 2.dp, horizontal = 4.dp),
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getBackendDisplayName(backend),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (backend == BackendImplementation.SHIZUKU) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ) {
                        Text(
                            text = stringResource(R.string.settings_screen_backend_implementation_shizuku_advanced),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = getBackendDescription(backend),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getBackendIcon(backend),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        trailingContent = {
            Box(
                contentAlignment = Alignment.Center
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

private fun getBackendDisplayName(backend: BackendImplementation): String {
    return when (backend) {
        BackendImplementation.ACCESSIBILITY -> "Accessibility Service"
        BackendImplementation.USAGE_STATS -> "Usage Statistics"
        BackendImplementation.SHIZUKU -> "Shizuku Service"
    }
}

private fun getBackendDescription(backend: BackendImplementation): String {
    return when (backend) {
        BackendImplementation.ACCESSIBILITY -> "Standard method that works on most devices"
        BackendImplementation.USAGE_STATS -> "Experimental method using app usage statistics"
        BackendImplementation.SHIZUKU -> "Advanced method using Shizuku and internal APIs"
    }
}

private fun getBackendIcon(backend: BackendImplementation): ImageVector {
    return when (backend) {
        BackendImplementation.ACCESSIBILITY -> Accessibility
        BackendImplementation.USAGE_STATS -> Icons.Default.QueryStats
        BackendImplementation.SHIZUKU -> Icons.Default.AutoAwesome
    }
}


@Composable
fun AppThemeModeCard(
    selectedThemeMode: AppThemeMode,
    onThemeModeSelected: (AppThemeMode) -> Unit
) {
    val modes = listOf(
        AppThemeMode.SYSTEM to stringResource(R.string.settings_screen_theme_mode_system),
        AppThemeMode.LIGHT to stringResource(R.string.settings_screen_theme_mode_light),
        AppThemeMode.DARK to stringResource(R.string.settings_screen_theme_mode_dark)
    )

    SettingsCard(index = 0, listSize = 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            modes.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedThemeMode == mode,
                            onClick = { onThemeModeSelected(mode) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedThemeMode == mode,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_screen_permission_required_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_screen_permission_required_dialog_text_1))
                Text(stringResource(R.string.settings_screen_permission_required_dialog_text_2))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.grant_permission_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun DeviceAdminDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_screen_device_admin_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_screen_device_admin_dialog_text))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.grant_permission_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun AccessibilityDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_screen_accessibility_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_screen_accessibility_dialog_text))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.grant_permission_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
