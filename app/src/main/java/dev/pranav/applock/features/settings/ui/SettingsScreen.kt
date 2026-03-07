package dev.pranav.applock.features.settings.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.LogUtils
import dev.pranav.applock.core.utils.hasUsagePermission
import dev.pranav.applock.core.utils.isAccessibilityServiceEnabled
import dev.pranav.applock.core.utils.openAccessibilitySettings
import dev.pranav.applock.data.repository.AppLockRepository
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.features.admin.AdminDisableActivity
import dev.pranav.applock.services.ExperimentalAppLockService
import dev.pranav.applock.services.ShizukuAppLockService
import dev.pranav.applock.ui.components.DonateButton
import dev.pranav.applock.ui.icons.*
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val appLockRepository = remember { AppLockRepository(context) }

    var showDialog by remember { mutableStateOf(false) }
    var showUnlockTimeDialog by remember { mutableStateOf(false) }

    val shizukuPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_screen_shizuku_permission_granted),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_screen_shizuku_permission_required_desc),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var autoUnlock by remember { mutableStateOf(appLockRepository.isAutoUnlockEnabled()) }
    var useMaxBrightness by remember { mutableStateOf(appLockRepository.shouldUseMaxBrightness()) }
    var useBiometricAuth by remember { mutableStateOf(appLockRepository.isBiometricAuthEnabled()) }
    var unlockTimeDuration by remember { mutableIntStateOf(appLockRepository.getUnlockTimeDuration()) }
    var antiUninstallEnabled by remember { mutableStateOf(appLockRepository.isAntiUninstallEnabled()) }
    var disableHapticFeedback by remember { mutableStateOf(appLockRepository.shouldDisableHaptics()) }
    var loggingEnabled by remember { mutableStateOf(appLockRepository.isLoggingEnabled()) }

    // NEW: System Settings Restriction State Variables
    var restrictDrawOverAppsEnabled by remember { 
        mutableStateOf(appLockRepository.isRestrictDrawOverAppsSettings()) 
    }
    var restrictUsageAccessEnabled by remember { 
        mutableStateOf(appLockRepository.isRestrictUsageAccessSettings()) 
    }
    var restrictAccessibilityEnabled by remember { 
        mutableStateOf(appLockRepository.isRestrictAccessibilitySettings()) 
    }
    var restrictDeviceAdminEnabled by remember { 
        mutableStateOf(appLockRepository.isRestrictDeviceAdminSettings()) 
    }
    var requireUnrestrictedBatteryEnabled by remember { 
        mutableStateOf(appLockRepository.isRequireUnrestrictedBattery()) 
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showDeviceAdminDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val biometricManager = remember { BiometricManager.from(context) }
    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_screen_support_development_dialog_title)) },
            text = { Text(stringResource(R.string.support_development_text)) },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://pranavpurwar.github.io/donate.html".toUri()
                            )
                        )
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.settings_screen_support_development_donate_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                showDeviceAdminDialog = true
                showAccessibilityDialog = true
                showPermissionDialog = false
            }
        )
    }

    if (showDeviceAdminDialog) {
        DeviceAdminPermissionDialog(
            onDismiss = { showDeviceAdminDialog = false },
            onConfirm = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(context, DeviceAdmin::class.java)
                )
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.device_admin_explanation)
                )
                context.startActivity(intent)
                showDeviceAdminDialog = false
            }
        )
    }

    if (showAccessibilityDialog) {
        AntiUninstallAccessibilityPermissionDialog(
            onDismiss = { showAccessibilityDialog = false },
            onConfirm = {
                openAccessibilitySettings(context)
                showAccessibilityDialog = false
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_screen_back_cd)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val packageInfo = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                }
                val versionName = packageInfo?.versionName ?: "Unknown"
                Text(
                    text = stringResource(R.string.settings_screen_version_template, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            item {
                DonateButton()
            }

            item {
                SectionTitle(text = stringResource(R.string.settings_screen_lock_screen_customization_title))
            }

            item {
                SettingsGroup(
                    items = listOf(
                        ToggleSettingItem(
                            icon = BrightnessHigh,
                            title = stringResource(R.string.settings_screen_max_brightness_title),
                            subtitle = stringResource(R.string.settings_screen_max_brightness_desc),
                            checked = useMaxBrightness,
                            enabled = true,
                            onCheckedChange = { isChecked ->
                                useMaxBrightness = isChecked
                                appLockRepository.setUseMaxBrightness(isChecked)
                            }
                        ),
                        ToggleSettingItem(
                            icon = if (useBiometricAuth) Fingerprint else FingerprintOff,
                            title = stringResource(R.string.settings_screen_biometric_auth_title),
                            subtitle = if (isBiometricAvailable)
                                stringResource(R.string.settings_screen_biometric_auth_desc_available)
                            else
                                stringResource(R.string.settings_screen_biometric_auth_desc_unavailable),
                            checked = useBiometricAuth && isBiometricAvailable,
                            enabled = isBiometricAvailable,
                            onCheckedChange = { isChecked ->
                                useBiometricAuth = isChecked
                                appLockRepository.setBiometricAuthEnabled(isChecked)
                            }
                        ),
                        ToggleSettingItem(
                            icon = Icons.Default.Vibration,
                            title = stringResource(R.string.settings_screen_haptic_feedback_title),
                            subtitle = stringResource(R.string.settings_screen_haptic_feedback_desc),
                            checked = disableHapticFeedback,
                            enabled = true,
                            onCheckedChange = { isChecked ->
                                disableHapticFeedback = isChecked
                                appLockRepository.setDisableHaptics(isChecked)
                            }
                        ),
                        ToggleSettingItem(
                            icon = Icons.Default.ShieldMoon,
                            title = stringResource(R.string.settings_screen_auto_unlock_title),
                            subtitle = stringResource(R.string.settings_screen_auto_unlock_desc),
                            checked = autoUnlock,
                            enabled = true,
                            onCheckedChange = { isChecked ->
                                autoUnlock = isChecked
                                appLockRepository.setAutoUnlockEnabled(isChecked)
                            }
                        )
                    )
                )
            }

            item {
                SectionTitle(text = stringResource(R.string.settings_screen_security_title))
            }

            item {
                SettingsGroup(
                    items = listOf(
                        ActionSettingItem(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.settings_screen_change_pin_title),
                            subtitle = stringResource(R.string.settings_screen_change_pin_desc),
                            onClick = { navController.navigate(Screen.ChangePassword.route) }
                        ),
                        ActionSettingItem(
                            icon = Timer,
                            title = stringResource(R.string.settings_screen_unlock_duration_title),
                            subtitle = if (unlockTimeDuration > 0) {
                                if (unlockTimeDuration > 10_000) "Until screen off"
                                else stringResource(
                                    R.string.settings_screen_unlock_duration_summary_minutes,
                                    unlockTimeDuration
                                )
                            } else stringResource(R.string.settings_screen_unlock_duration_summary_immediate),
                            onClick = { showUnlockTimeDialog = true }
                        )
                    )
                )
            }

            // NEW: ANTI-UNINSTALL WITH SYSTEM SETTINGS RESTRICTIONS
            item {
                Column {
                    // Main Anti-Uninstall Toggle
                    SettingsGroup(
                        items = listOf(
                            ToggleSettingItem(
                                icon = Icons.Default.Lock,
                                title = stringResource(R.string.settings_screen_anti_uninstall_title),
                                subtitle = stringResource(R.string.settings_screen_anti_uninstall_desc),
                                checked = antiUninstallEnabled,
                                enabled = true,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        val dpm =
                                            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                        val component = ComponentName(context, DeviceAdmin::class.java)
                                        val hasDeviceAdmin = dpm.isAdminActive(component)
                                        val hasAccessibility = context.isAccessibilityServiceEnabled()

                                        when {
                                            !hasDeviceAdmin && !hasAccessibility -> {
                                                showPermissionDialog = true
                                            }
                                            !hasDeviceAdmin -> {
                                                showDeviceAdminDialog = true
                                            }
                                            !hasAccessibility -> {
                                                showAccessibilityDialog = true
                                            }
                                            else -> {
                                                antiUninstallEnabled = true
                                                appLockRepository.setAntiUninstallEnabled(true)
                                            }
                                        }
                                    } else {
                                        context.startActivity(
                                            Intent(context, AdminDisableActivity::class.java)
                                        )
                                    }
                                }
                            )
                        )
                    )

                    // Sub-Switches - Only visible when Anti-Uninstall is enabled
                    AnimatedVisibility(
                        visible = antiUninstallEnabled,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 100f
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 100f
                            )
                        ) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Restrict System Settings Access",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Disable "Draw Over Other Apps" Settings Page
                            SettingsGroup(
                                items = listOf(
                                    ToggleSettingItem(
                                        icon = Icons.Default.Visibility,
                                        title = "Disable Draw Over Other Apps",
                                        subtitle = "Prevent disabling overlay permission used by unlock screen",
                                        checked = restrictDrawOverAppsEnabled,
                                        enabled = antiUninstallEnabled,
                                        onCheckedChange = { isChecked ->
                                            restrictDrawOverAppsEnabled = isChecked
                                            appLockRepository.setRestrictDrawOverAppsSettings(isChecked)
                                        }
                                    )
                                )
                            )

                            // Disable "Usage Access" Settings Page
                            SettingsGroup(
                                items = listOf(
                                    ToggleSettingItem(
                                        icon = Icons.Default.BarChart,
                                        title = "Disable Usage Access",
                                        subtitle = "Prevent disabling Usage Stats permission required for foreground app detection",
                                        checked = restrictUsageAccessEnabled,
                                        enabled = antiUninstallEnabled,
                                        onCheckedChange = { isChecked ->
                                            restrictUsageAccessEnabled = isChecked
                                            appLockRepository.setRestrictUsageAccessSettings(isChecked)
                                        }
                                    )
                                )
                            )

                            // Disable "Accessibility Settings" Page
                            SettingsGroup(
                                items = listOf(
                                    ToggleSettingItem(
                                        icon = Accessibility,
                                        title = "Disable Accessibility Settings",
                                        subtitle = "Prevent disabling Accessibility Service used for app lock monitoring",
                                        checked = restrictAccessibilityEnabled,
                                        enabled = antiUninstallEnabled,
                                        onCheckedChange = { isChecked ->
                                            restrictAccessibilityEnabled = isChecked
                                            appLockRepository.setRestrictAccessibilitySettings(isChecked)
                                        }
                                    )
                                )
                            )

                            // Disable "Device Administrator Settings" Page
                            SettingsGroup(
                                items = listOf(
                                    ToggleSettingItem(
                                        icon = Icons.Outlined.Security,
                                        title = "Disable Device Administrator Settings",
                                        subtitle = "Prevent removing Device Administrator privilege",
                                        checked = restrictDeviceAdminEnabled,
                                        enabled = antiUninstallEnabled,
                                        onCheckedChange = { isChecked ->
                                            restrictDeviceAdminEnabled = isChecked
                                            appLockRepository.setRestrictDeviceAdminSettings(isChecked)
                                        }
                                    )
                                )
                            )

                            // Require "Unrestricted Battery Usage"
                            SettingsGroup(
                                items = listOf(
                                    ToggleSettingItem(
                                        icon = BatterySaver,
                                        title = "Require Unrestricted Battery Usage",
                                        subtitle = "Ensure app is not restricted by battery optimization",
                                        checked = requireUnrestrictedBatteryEnabled,
                                        enabled = antiUninstallEnabled,
                                        onCheckedChange = { isChecked ->
                                            requireUnrestrictedBatteryEnabled = isChecked
                                            appLockRepository.setRequireUnrestrictedBattery(isChecked)
                                        }
                                    )
                                )
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(text = stringResource(R.string.settings_screen_advanced_title))
            }

            item {
                SettingsGroup(
                    items = listOf(
                        ActionSettingItem(
                            icon = Icons.Outlined.Security,
                            title = stringResource(R.string.settings_Screen_export_audit),
                            subtitle = stringResource(R.string.settings_screen_export_audit_desc),
                            onClick = {
                                val uri = LogUtils.exportAuditLogs()
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share audit logs")
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_screen_export_logs_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ),
                        ActionSettingItem(
                            icon = Icons.Outlined.Code,
                            title = stringResource(R.string.settings_screen_backend_title),
                            subtitle = when (appLockRepository.getBackendImplementation()) {
                                BackendImplementation.ACCESSIBILITY -> stringResource(R.string.settings_screen_backend_accessibility)
                                BackendImplementation.USAGE_STATS -> stringResource(R.string.settings_screen_backend_usage_stats)
                                BackendImplementation.SHIZUKU -> stringResource(R.string.settings_screen_backend_shizuku)
                            },
                            onClick = {
                                navController.navigate(Screen.ChooseBackend.route)
                            }
                        ),
                        ActionSettingItem(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(R.string.settings_screen_logging_title),
                            subtitle = stringResource(R.string.settings_screen_logging_desc),
                            onClick = {
                                loggingEnabled = !loggingEnabled
                                appLockRepository.setLoggingEnabled(loggingEnabled)
                            }
                        )
                    )
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = { showDialog = true }) {
                        Text(stringResource(R.string.settings_screen_support_development_button))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.navigate("https://github.com/PranavPurwar/AppLock") }) {
                        Icon(
                            imageVector = Github,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_screen_github_button))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(items: List<SettingItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items.forEachIndexed { index, item ->
            when (item) {
                is ToggleSettingItem -> ToggleSettingItemComposable(item)
                is ActionSettingItem -> ActionSettingItemComposable(item)
            }
            if (index < items.lastIndex) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingItemComposable(item: ToggleSettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) { item.onCheckedChange(!item.checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (item.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (item.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = item.checked,
            onCheckedChange = { item.onCheckedChange(it) },
            enabled = item.enabled
        )
    }
}

@Composable
private fun ActionSettingItemComposable(item: ActionSettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

sealed class SettingItem
data class ToggleSettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit
) : SettingItem()

data class ActionSettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
) : SettingItem()

@Composable
private fun UnlockTimeDurationDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var duration by remember { mutableIntStateOf(currentDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Unlock Duration") },
        text = {
            Column {
                Text("Duration in seconds (0 = immediately, 10000+ = until screen off)")
                OutlinedTextField(
                    value = duration.toString(),
                    onValueChange = { duration = it.toIntOrNull() ?: 0 },
                    label = { Text("Seconds") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionRequiredDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = { Text("Device Administrator and Accessibility Service permissions are required for Anti-Uninstall protection.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeviceAdminPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device Administrator Required") },
        text = { Text("Grant Device Administrator permission to enable Anti-Uninstall protection.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AntiUninstallAccessibilityPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibility Service Required") },
        text = { Text("Enable Accessibility Service for App Lock to provide Anti-Uninstall protection.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
