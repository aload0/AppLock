package dev.pranav.applock.features.applist.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import dev.pranav.applock.ui.components.DonateModalBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

@SuppressLint("LocalContextGetResourceValueCall")
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

    val isLoading by mainViewModel.isLoading.collectAsState()
    val lockedApps by mainViewModel.lockedAppsFlow.collectAsState()
    val unlockedApps by mainViewModel.unlockedAppsFlow.collectAsState()

    var showAddAppsSheet by remember { mutableStateOf(false) }

    var applockEnabled by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var firstMissingPermission by remember { mutableStateOf<MissingPermission?>(null) }

    LaunchedEffect(Unit) {
        val appLockRepository = context.appLockRepository()
        applockEnabled = appLockRepository.isProtectEnabled()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val appLockRepository = context.appLockRepository()
                val backend = appLockRepository.getBackendImplementation()
                val isAntiUninstallEnabled = appLockRepository.isAntiUninstallEnabled()
                val dpm =
                    context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val component = ComponentName(context, DeviceAdmin::class.java)

                firstMissingPermission = when {
                    !Settings.canDrawOverlays(context) -> MissingPermission.OVERLAY
                    backend == BackendImplementation.ACCESSIBILITY && !context.isAccessibilityServiceEnabled() -> MissingPermission.ACCESSIBILITY
                    backend == BackendImplementation.USAGE_STATS && !context.hasUsagePermission() -> MissingPermission.USAGE_STATS
                    backend == BackendImplementation.SHIZUKU && (runCatching { !Shizuku.pingBinder() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED }.getOrDefault(
                        true
                    )) -> MissingPermission.SHIZUKU

                    isAntiUninstallEnabled && !context.isAccessibilityServiceEnabled() -> MissingPermission.ACCESSIBILITY
                    isAntiUninstallEnabled && !dpm.isAdminActive(component) -> MissingPermission.DEVICE_ADMIN
                    else -> null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appLockRepository = context.appLockRepository()

    var showCommunityLink by remember { mutableStateOf(appLockRepository.isShowCommunityLink()) }

    if (showCommunityLink) {
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
    if (showDonateDialog && !showCommunityLink) {
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
                    Surface(
                        onClick = {
                            appLockRepository.setProtectEnabled(!applockEnabled)
                            applockEnabled = !applockEnabled
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (applockEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (applockEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                                contentDescription = stringResource(R.string.main_screen_app_protection_cd),
                                modifier = Modifier.size(18.dp),
                                tint = if (applockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (applockEnabled) "ON" else "OFF",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (applockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.TriggerExclusions.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Trigger exclusions",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.AntiUninstall.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Anti-Uninstall",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = { showAddAppsSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.main_screen_search_cd) // Update ideally
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            firstMissingPermission?.let { missingPerm ->
                PermissionWarningBanner(
                    missingPermission = missingPerm,
                    onClick = {
                        when (missingPerm) {
                            MissingPermission.OVERLAY -> {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = "package:${context.packageName}".toUri()
                                })
                            }

                            MissingPermission.ACCESSIBILITY -> {
                                openAccessibilitySettings(context)
                            }

                            MissingPermission.USAGE_STATS -> {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }

                            MissingPermission.SHIZUKU -> {
                                try {
                                    if (Shizuku.isPreV11()) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.main_screen_shizuku_manual_permission_toast),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Shizuku.requestPermission(423)
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.main_screen_shizuku_not_available_toast),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            MissingPermission.DEVICE_ADMIN -> {
                                val component = ComponentName(context, DeviceAdmin::class.java)
                                val intent =
                                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                                        putExtra(
                                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                            context.getString(R.string.main_screen_device_admin_explanation)
                                        )
                                    }
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }
            if (isLoading) {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                ProtectedAppsDashboard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    lockedApps = lockedApps,
                    onUnlockApp = { appInfo ->
                        mainViewModel.unlockApp(appInfo.packageName)
                    }
                )
            }
        }
    }

    if (showAddAppsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val selectedPackages = remember { mutableStateListOf<String>() }
        var bottomSheetSearchQuery by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = {
                bottomSheetSearchQuery = ""
                showAddAppsSheet = false
            },
            sheetState = sheetState
        ) {
            AddProtectedAppsSheetContent(
                unlockedApps = unlockedApps,
                searchQuery = bottomSheetSearchQuery,
                onSearchQueryChanged = { bottomSheetSearchQuery = it },
                selectedPackages = selectedPackages,
                onToggleSelection = { packageName ->
                    if (selectedPackages.contains(packageName)) {
                        selectedPackages.remove(packageName)
                    } else {
                        selectedPackages.add(packageName)
                    }
                },
                onSave = {
                    mainViewModel.lockApps(selectedPackages)
                    bottomSheetSearchQuery = ""
                    showAddAppsSheet = false
                },
                onCancel = {
                    bottomSheetSearchQuery = ""
                    showAddAppsSheet = false
                }
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

@Composable
private fun ProtectedAppsDashboard(
    modifier: Modifier = Modifier,
    lockedApps: List<ApplicationInfo>,
    onUnlockApp: (ApplicationInfo) -> Unit
) {
    if (lockedApps.isEmpty()) {
        EmptyDashboardState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp), // Extra padding for FAB
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lockedApps, key = { it.packageName }) { appInfo ->
                ProtectedAppItem(
                    appInfo = appInfo,
                    onUnlock = { onUnlockApp(appInfo) }
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboardState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Protected Apps",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to selectively secure your apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProtectedAppsSheetContent(
    unlockedApps: List<ApplicationInfo>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedPackages: List<String>,
    onToggleSelection: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val filteredApps by produceState(
        initialValue = unlockedApps,
        unlockedApps,
        searchQuery
    ) {
        value = if (searchQuery.isBlank()) {
            unlockedApps
        } else {
            withContext(Dispatchers.Default) {
                val lowerQuery = searchQuery.lowercase()
                unlockedApps.filter { appInfo ->
                    val label = AppIconCache.getLabel(context, appInfo).lowercase()
                    label.contains(lowerQuery) || appInfo.packageName.contains(
                        lowerQuery,
                        ignoreCase = true
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Apps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onSave,
                enabled = selectedPackages.isNotEmpty()
            ) {
                Text("Protect (${selectedPackages.size})")
            }
        }

        // Search Bar
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

        // List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { appInfo ->
                val isSelected = selectedPackages.contains(appInfo.packageName)
                SelectableAppItem(
                    appInfo = appInfo,
                    isSelected = isSelected,
                    onClick = { onToggleSelection(appInfo.packageName) }
                )
            }
        }
    }
}

@Composable
private fun ProtectedAppItem(
    appInfo: ApplicationInfo,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current

    var appName by remember(appInfo) { mutableStateOf<String?>(null) }
    var icon by remember(appInfo) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(appInfo) {
        withContext(Dispatchers.IO) {
            appName = AppIconCache.getLabel(context, appInfo)
            icon = AppIconCache.getIcon(context, appInfo)
        }
    }

    ListItem(
        headlineContent = {
            if (appName != null) {
                Text(
                    text = appName!!,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingContent = {
            Text(
                text = "Protected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Image(
                            bitmap = icon!!,
                            contentDescription = appName,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onUnlock) {
                Icon(
                    imageVector = Icons.Outlined.LockOpen,
                    contentDescription = "Unlock ${appName ?: "app"}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
private fun SelectableAppItem(
    appInfo: ApplicationInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    var appName by remember(appInfo) { mutableStateOf<String?>(null) }
    var icon by remember(appInfo) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(appInfo) {
        withContext(Dispatchers.IO) {
            appName = AppIconCache.getLabel(context, appInfo)
            icon = AppIconCache.getIcon(context, appInfo)
        }
    }

    ListItem(
        headlineContent = {
            if (appName != null) {
                Text(
                    text = appName!!,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingContent = {
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Image(
                            bitmap = icon!!,
                            contentDescription = appName,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        },
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    )
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

private enum class MissingPermission(val titleResId: Int, val descriptionResId: Int) {
    OVERLAY(
        titleResId = R.string.permission_warning_overlay_title,
        descriptionResId = R.string.permission_warning_overlay_desc
    ),
    ACCESSIBILITY(
        titleResId = R.string.permission_warning_accessibility_title,
        descriptionResId = R.string.permission_warning_accessibility_desc
    ),
    USAGE_STATS(
        titleResId = R.string.permission_warning_usage_stats_title,
        descriptionResId = R.string.permission_warning_usage_stats_desc
    ),
    SHIZUKU(
        titleResId = R.string.permission_warning_shizuku_title,
        descriptionResId = R.string.permission_warning_shizuku_desc
    ),
    DEVICE_ADMIN(
        titleResId = R.string.permission_warning_device_admin_title,
        descriptionResId = R.string.permission_warning_device_admin_desc
    )
}

@Composable
private fun PermissionWarningBanner(
    missingPermission: MissingPermission,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${stringResource(missingPermission.titleResId)} ${stringResource(R.string.permission_warning_title_suffix)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(missingPermission.descriptionResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
