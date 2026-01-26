package dev.pranav.applock.features.applist.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.pranav.applock.R
import dev.pranav.applock.core.broadcast.DeviceAdmin
import dev.pranav.applock.core.navigation.Screen
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.hasUsagePermission
import dev.pranav.applock.core.utils.isAccessibilityServiceEnabled
import dev.pranav.applock.core.utils.openAccessibilitySettings
import dev.pranav.applock.data.repository.BackendImplementation
import dev.pranav.applock.ui.components.*
import rikka.shizuku.Shizuku

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MainScreen(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()
    val filteredApps by mainViewModel.filteredApps.collectAsState()

    var showOverlayDialog by remember { mutableStateOf(!Settings.canDrawOverlays(context)) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showShizukuDialog by remember { mutableStateOf(false) }
    var showUsageStatsDialog by remember { mutableStateOf(false) }
    var showAntiUninstallAccessibilityDialog by remember { mutableStateOf(false) }
    var showAntiUninstallDeviceAdminDialog by remember { mutableStateOf(false) }
    var applockEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val appLockRepository = context.appLockRepository()
        val selectedBackend = appLockRepository.getBackendImplementation()
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, DeviceAdmin::class.java)

        applockEnabled = appLockRepository.isProtectEnabled()

        if (appLockRepository.isAntiUninstallEnabled()) {
            Log.d("MainScreen", context.getString(R.string.main_screen_anti_uninstall_log))
            if (!context.isAccessibilityServiceEnabled()) {
                showAntiUninstallAccessibilityDialog = true
            } else if (!dpm.isAdminActive(component)) {
                showAntiUninstallDeviceAdminDialog = true
            }
        }

        when (selectedBackend) {
            BackendImplementation.ACCESSIBILITY -> {
                if (!context.isAccessibilityServiceEnabled()) {
                    showAccessibilityDialog = true
                }
            }

            BackendImplementation.USAGE_STATS -> {
                if (!context.hasUsagePermission()) {
                    showUsageStatsDialog = true
                }
            }

            BackendImplementation.SHIZUKU -> {
                try {
                    if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
                        showShizukuDialog = true
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.main_screen_shizuku_not_available_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = { Text(stringResource(R.string.main_screen_overlay_permission_dialog_title)) },
            text = { Text(stringResource(R.string.main_screen_overlay_permission_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = "package:${context.packageName}".toUri()
                    })
                    showOverlayDialog = false
                }) {
                    Text(stringResource(R.string.main_screen_overlay_permission_open_settings_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Show accessibility service guide dialog if needed
    if (showAccessibilityDialog && !showOverlayDialog && !showAntiUninstallAccessibilityDialog && !showAntiUninstallDeviceAdminDialog && !context.isAccessibilityServiceEnabled()) {
        AccessibilityServiceGuideDialog(
            onOpenSettings = {
                openAccessibilitySettings(context)
                showAccessibilityDialog = false
            },
            onDismiss = {
                showAccessibilityDialog = false
            }
        )
    }

    if (showShizukuDialog && !showOverlayDialog && !showAntiUninstallAccessibilityDialog && !showAntiUninstallDeviceAdminDialog && Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
        ShizukuPermissionDialog(
            onOpenSettings = {
                try {
                    if (Shizuku.isPreV11()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.main_screen_shizuku_manual_permission_toast),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        showShizukuDialog = false
                        Shizuku.requestPermission(423)
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.main_screen_shizuku_not_available_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onDismiss = {
                showShizukuDialog = false
            }
        )
    }

    if (showUsageStatsDialog && !showOverlayDialog && !showAntiUninstallAccessibilityDialog && !showAntiUninstallDeviceAdminDialog && !context.hasUsagePermission()) {
        UsageStatsPermission(
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                showUsageStatsDialog = false
            },
            onDismiss = {
                showUsageStatsDialog = false
            }
        )
    }

    if (showAntiUninstallAccessibilityDialog && !showOverlayDialog && !showShizukuDialog && !showUsageStatsDialog && !showAccessibilityDialog) {
        AntiUninstallAccessibilityPermissionDialog(
            onOpenSettings = {
                openAccessibilitySettings(context)
                showAntiUninstallAccessibilityDialog = false
            },
            onDismiss = {
                showAntiUninstallAccessibilityDialog = false
            }
        )
    }

    if (showAntiUninstallDeviceAdminDialog && !showOverlayDialog && !showShizukuDialog && !showUsageStatsDialog && !showAccessibilityDialog && !showAntiUninstallAccessibilityDialog) {
        AntiUninstallAccessibilityPermissionDialog(
            onOpenSettings = {
                val component = ComponentName(context, DeviceAdmin::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.main_screen_device_admin_explanation)
                    )
                }
                context.startActivity(intent)
                showAntiUninstallDeviceAdminDialog = false
            },
            onDismiss = {
                showAntiUninstallDeviceAdminDialog = false
            }
        )
    }

    val appLockRepository = context.appLockRepository()

    var showCommunityLink by remember { mutableStateOf(appLockRepository.isShowCommunityLink()) }

    if (showCommunityLink && !showAccessibilityDialog && !showShizukuDialog && !showUsageStatsDialog && !showAntiUninstallAccessibilityDialog && !showAntiUninstallDeviceAdminDialog && !showOverlayDialog) {
        CommunityDialog(
            onDismiss = {
                appLockRepository.setCommunityLinkShown(true)
                showCommunityLink = false
            },
            onJoin = {
                appLockRepository.setCommunityLinkShown(true)
                showCommunityLink = false
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://discord.gg/46wCMRVAre".toUri()
                    )
                )
            }
        )
    }

    var showDonateDialog by remember { mutableStateOf(appLockRepository.isShowDonateLink()) }
    if (showDonateDialog && !showAccessibilityDialog && !showShizukuDialog && !showUsageStatsDialog && !showAntiUninstallAccessibilityDialog && !showAntiUninstallDeviceAdminDialog && !showCommunityLink && !showOverlayDialog) {
        DonateModalBottomSheet { showDonateDialog = false }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            appLockRepository.setProtectEnabled(!applockEnabled)
                            applockEnabled = !applockEnabled
                        }
                    ) {
                        Icon(
                            imageVector = if (applockEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                            contentDescription = stringResource(R.string.main_screen_app_protection_cd),
                            tint = if (applockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.main_screen_settings_cd),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter and Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val showSystemApps by mainViewModel.showSystemApps.collectAsState()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Show system apps",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                Switch(
                                    checked = showSystemApps,
                                    onCheckedChange = {
                                        mainViewModel.toggleShowSystemApps()
                                    }
                                )
                            },
                            onClick = {
                                mainViewModel.toggleShowSystemApps()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Trigger exclusions",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                expanded = false
                                navController.navigate(Screen.TriggerExclusions.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            MainContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                searchQuery = searchQuery,
                filteredApps = filteredApps,
                onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) },
                onAppToggle = { appInfo, isChecked ->
                    mainViewModel.toggleAppLock(appInfo, isChecked)
                },
                viewModel = mainViewModel
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.main_screen_loading_applications_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    searchQuery: String,
    filteredApps: Set<ApplicationInfo>,
    onSearchQueryChanged: (String) -> Unit,
    onAppToggle: (ApplicationInfo, Boolean) -> Unit,
    viewModel: MainViewModel
) {
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChanged,
                        onSearch = { focusManager.clearFocus() },
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = {
                            Text(
                                stringResource(R.string.main_screen_search_apps_placeholder),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.main_screen_search_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                content = {},
            )
        }

        if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
            item {
                EmptySearchState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        } else {
            items(filteredApps.toList(), key = { it.packageName }) { appInfo ->
                AppItem(
                    appInfo = appInfo,
                    viewModel = viewModel,
                    onClick = { isChecked ->
                        onAppToggle(appInfo, isChecked)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.main_screen_empty_search_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.main_screen_empty_search_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppItem(
    appInfo: ApplicationInfo,
    viewModel: MainViewModel,
    onClick: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appName = remember(appInfo) { appInfo.loadLabel(packageManager).toString() }
    val icon = remember(appInfo) { appInfo.loadIcon(packageManager)?.toBitmap()?.asImageBitmap() }

    val isChecked = remember(appInfo) {
        mutableStateOf(viewModel.isAppLocked(appInfo.packageName))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isChecked.value = !isChecked.value
                onClick(isChecked.value)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(30),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = icon ?: ImageBitmap.imageResource(R.drawable.ic_notification),
                    contentDescription = appName,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = isChecked.value,
            onCheckedChange = { isCheckedValue ->
                isChecked.value = isCheckedValue
                onClick(isCheckedValue)
            }
        )
    }
}

@Composable
private fun CommunityDialog(
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Groups,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.join_community),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.join_community_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onJoin) {
                Icon(
                    Icons.Rounded.Forum,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.join_discord))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.maybe_later))
            }
        }
    )
}
