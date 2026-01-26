package dev.pranav.applock.features.settings.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
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

    val shizukuPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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

    var autoUnlock by remember {
        mutableStateOf(appLockRepository.isAutoUnlockEnabled())
    }

    var useMaxBrightness by remember {
        mutableStateOf(appLockRepository.shouldUseMaxBrightness())
    }
    var useBiometricAuth by remember {
        mutableStateOf(appLockRepository.isBiometricAuthEnabled())
    }
    var unlockTimeDuration by remember {
        mutableIntStateOf(appLockRepository.getUnlockTimeDuration())
    }

    var antiUninstallEnabled by remember {
        mutableStateOf(appLockRepository.isAntiUninstallEnabled())
    }
    var disableHapticFeedback by remember {
        mutableStateOf(appLockRepository.shouldDisableHaptics())
    }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showDeviceAdminDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val biometricManager = remember { BiometricManager.from(context) }

    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
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
                ) { Text(stringResource(R.string.settings_screen_support_development_donate_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                }) { Text(stringResource(R.string.cancel_button)) }
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
                        context.getString(R.string.main_screen_device_admin_explanation)
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

                // Check if device admin is still needed after accessibility is granted
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
                title = {
                    Text(stringResource(R.string.settings_screen_title))
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                DonateButton()
            }
            item {
                SectionTitle(text = stringResource(R.string.settings_screen_lock_screen_customization_title))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        SettingItem(
                            icon = BrightnessHigh,
                            title = stringResource(R.string.settings_screen_max_brightness_title),
                            description = stringResource(R.string.settings_screen_max_brightness_desc),
                            checked = useMaxBrightness,
                            onCheckedChange = { isChecked ->
                                useMaxBrightness = isChecked
                                appLockRepository.setUseMaxBrightness(isChecked)
                            }
                        )
                        SettingItem(
                            icon = if (useBiometricAuth) Fingerprint else FingerprintOff,
                            title = stringResource(R.string.settings_screen_biometric_auth_title),
                            description = if (isBiometricAvailable) stringResource(R.string.settings_screen_biometric_auth_desc_available) else stringResource(
                                R.string.settings_screen_biometric_auth_desc_unavailable
                            ),
                            checked = useBiometricAuth && isBiometricAvailable,
                            enabled = isBiometricAvailable,
                            onCheckedChange = { isChecked ->
                                useBiometricAuth = isChecked
                                appLockRepository.setBiometricAuthEnabled(isChecked)
                            }
                        )
                        SettingItem(
                            icon = Icons.Default.Vibration,
                            title = stringResource(R.string.settings_screen_haptic_feedback_title),
                            description = stringResource(R.string.settings_screen_haptic_feedback_desc),
                            checked = disableHapticFeedback,
                            onCheckedChange = { isChecked ->
                                disableHapticFeedback = isChecked
                                appLockRepository.setDisableHaptics(isChecked)
                            }
                        )
                        SettingItem(
                            icon = Icons.Default.ShieldMoon,
                            title = stringResource(R.string.settings_screen_auto_unlock_title),
                            description = stringResource(R.string.settings_screen_auto_unlock_desc),
                            checked = autoUnlock,
                            onCheckedChange = { isChecked ->
                                autoUnlock = isChecked
                                appLockRepository.setAutoUnlockEnabled(isChecked)
                            }
                        )
                    }
                }
            }
            item {
                SectionTitle(text = stringResource(R.string.settings_screen_security_title))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        ActionSettingItem(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.settings_screen_change_pin_title),
                            description = stringResource(R.string.settings_screen_change_pin_desc),
                            onClick = {
                                navController.navigate(Screen.ChangePassword.route)
                            }
                        )
                        ActionSettingItem(
                            icon = Timer,
                            title = stringResource(R.string.settings_screen_unlock_duration_title),
                            description = if (unlockTimeDuration > 0) {
                                if (unlockTimeDuration > 10_000) "Until screen off" else stringResource(
                                    R.string.settings_screen_unlock_duration_summary_minutes,
                                    unlockTimeDuration
                                )
                            } else stringResource(R.string.settings_screen_unlock_duration_summary_immediate),
                            onClick = { showUnlockTimeDialog = true }
                        )
                        SettingItem(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.settings_screen_anti_uninstall_title),
                            description = stringResource(R.string.settings_screen_anti_uninstall_desc),
                            checked = antiUninstallEnabled,
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
                                        Intent(
                                            context,
                                            AdminDisableActivity::class.java
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                SectionTitle(text = stringResource(R.string.settings_screen_advanced_title))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        ActionSettingItem(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(R.string.settings_screen_export_logs_title),
                            description = stringResource(R.string.settings_screen_export_logs_desc),
                            onClick = {
                                val uri = LogUtils.exportLogs(context)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            "Share logs"
                                        )
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_screen_export_logs_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
            item {
                BackendSelectionCard(
                    appLockRepository = appLockRepository,
                    context = context,
                    shizukuPermissionLauncher = shizukuPermissionLauncher
                )
            }
            item {
                LinksSection()
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { if (enabled) onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.surfaceTint
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun ActionSettingItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.surfaceTint
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun UnlockTimeDurationDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val durations = listOf(0, 1, 5, 15, 30, 60, Integer.MAX_VALUE)
    var selectedDuration by remember { mutableIntStateOf(currentDuration) }

    // If the current duration is not in our list, default to the closest value
    if (!durations.contains(selectedDuration)) {
        selectedDuration = durations.minByOrNull { abs(it - currentDuration) } ?: 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_screen_unlock_duration_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_screen_unlock_duration_dialog_description_new))

                durations.forEach { duration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDuration = duration }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration }
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
    shizukuPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    var selectedBackend by remember { mutableStateOf(appLockRepository.getBackendImplementation()) }

    Column {
        SectionTitle(text = stringResource(R.string.settings_screen_backend_implementation_title))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column {
                BackendImplementation.entries.forEach { backend ->
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
                                                context.getString(R.string.settings_screen_shizuku_not_running_toast),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        selectedBackend = backend
                                        appLockRepository.setBackendImplementation(
                                            BackendImplementation.SHIZUKU
                                        )
                                        context.startService(
                                            Intent(
                                                context,
                                                ShizukuAppLockService::class.java
                                            )
                                        )
                                    }
                                }

                                BackendImplementation.USAGE_STATS -> {
                                    if (!context.hasUsagePermission()) {
                                        val intent = Intent(
                                            android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_screen_usage_permission_toast),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@BackendSelectionItem
                                    }

                                    selectedBackend = backend

                                    appLockRepository.setBackendImplementation(BackendImplementation.USAGE_STATS)
                                    context.startService(
                                        Intent(
                                            context,
                                            ExperimentalAppLockService::class.java
                                        )
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
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getBackendIcon(backend),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.surfaceTint
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getBackendDisplayName(backend),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                Text(
                    text = getBackendDescription(backend),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
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
                Text(stringResource(R.string.settings_screen_device_admin_dialog_text_1))
                Text(stringResource(R.string.settings_screen_device_admin_dialog_text_2))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.enable_button))
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
                Text(stringResource(R.string.settings_screen_accessibility_dialog_text_1))
                Text(stringResource(R.string.settings_screen_accessibility_dialog_text_2))
                Text(stringResource(R.string.settings_screen_accessibility_dialog_text_3))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.enable_button))
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
fun LinksSection() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Links",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
        )

        LinkCard(
            title = "Discord Community",
            icon = Discord,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://discord.gg/46wCMRVAre")
                )
                context.startActivity(intent)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinkCard(
                title = "Source Code",
                icon = Icons.Outlined.Code,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/PranavPurwar/AppLock")
                    )
                    context.startActivity(intent)
                }
            )

            LinkCard(
                title = "Report Issue",
                icon = Icons.Outlined.BugReport,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/PranavPurwar/AppLock/issues")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(),
        shapes = ButtonDefaults.shapes()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.surfaceTint
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

