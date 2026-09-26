package dev.pranav.applock.features.antiuninstall.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.pranav.applock.R
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.core.utils.blockUninstallForUser
import dev.pranav.applock.core.utils.unblockUninstallForUser
import dev.pranav.applock.features.applist.domain.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING, // Binder hasn't been received
    PERMISSION_DENIED,
    READY
}

class AntiUninstallViewModel: ViewModel() {
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _protectedApps = MutableStateFlow<Set<String>>(emptySet())
    val protectedApps: StateFlow<Set<String>> = _protectedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _manualPackageName = MutableStateFlow("")
    val manualPackageName: StateFlow<String> = _manualPackageName.asStateFlow()

    fun loadApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true

            val repository = context.appLockRepository()
            _protectedApps.value = repository.getAntiUninstallApps()

            val apps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }

            _allApps.value = apps
            _filteredApps.value = apps
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps(query)
    }

    fun updateManualPackageName(packageName: String) {
        _manualPackageName.value = packageName
    }

    private fun filterApps(query: String) {
        _filteredApps.value = if (query.isEmpty()) {
            _allApps.value
        } else {
            _allApps.value.filter { app ->
                app.name.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    fun toggleAppProtection(context: Context, packageName: String) {
        val repository = context.appLockRepository()
        val currentProtected = _protectedApps.value.toMutableSet()

        if (currentProtected.contains(packageName)) {
            repository.removeAntiUninstallApp(packageName)
            currentProtected.remove(packageName)
            unblockUninstallForUser(packageName)
        } else {
            repository.addAntiUninstallApp(packageName)
            currentProtected.add(packageName)
            blockUninstallForUser(packageName)
        }

        _protectedApps.value = currentProtected
    }

    fun addManualPackage(context: Context, packageName: String) {
        if (packageName.isNotBlank()) {
            val repository = context.appLockRepository()
            repository.addAntiUninstallApp(packageName.trim())

            val currentProtected = _protectedApps.value.toMutableSet()
            currentProtected.add(packageName.trim())
            _protectedApps.value = currentProtected

            _manualPackageName.value = ""
        }
    }

    private fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || isImportantSystemApp(it) }
            .map { appInfo ->
                AppInfo(
                    name = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun isImportantSystemApp(appInfo: ApplicationInfo): Boolean {
        val importantSystemApps = setOf(
            "com.android.chrome",
            "com.android.vending",
            "com.google.android.gms",
            "com.android.settings",
            "com.android.systemui",
            "com.android.launcher3"
        )
        return appInfo.packageName in importantSystemApps
    }
}

fun isShizukuInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun checkShizukuState(context: Context): ShizukuState {
    if (!isShizukuInstalled(context)) return ShizukuState.NOT_INSTALLED
    if (!Shizuku.pingBinder()) return ShizukuState.NOT_RUNNING
    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) return ShizukuState.PERMISSION_DENIED
    return ShizukuState.READY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiUninstallScreen(
    navController: NavController,
    viewModel: AntiUninstallViewModel = viewModel()
) {
    val context = LocalContext.current

    val allApps by viewModel.allApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val protectedApps by viewModel.protectedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val manualPackageName by viewModel.manualPackageName.collectAsState()

    val showManualAddDialog = remember { mutableStateOf(false) }

    var shizukuState by remember { mutableStateOf(checkShizukuState(context)) }

    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }

    DisposableEffect(Unit) {
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            shizukuState = checkShizukuState(context)
        }
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            shizukuState = checkShizukuState(context)
        }
        val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
            shizukuState = checkShizukuState(context)
        }

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        onDispose {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SearchTopBar(
                title = stringResource(R.string.anti_uninstall_screen_title),
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onBack = { navController.navigateUp() },
                onAdd = { showManualAddDialog.value = true }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = stringResource(R.string.anti_uninstall_screen_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                    }
                }
            } else {
                val protectedNotInList = protectedApps.filter { pkg ->
                    allApps.none { it.packageName == pkg }
                }

                if (protectedNotInList.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.anti_uninstall_manual_packages_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(protectedNotInList) { packageName ->
                        ManualPackageItem(
                            packageName = packageName,
                            onToggle = { viewModel.toggleAppProtection(context, packageName) }
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.anti_uninstall_installed_apps_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }

                items(filteredApps) { app ->
                    AppProtectionItem(
                        app = app,
                        isProtected = protectedApps.contains(app.packageName),
                        onToggle = { viewModel.toggleAppProtection(context, app.packageName) }
                    )
                }
            }
        }
    }

    if (showManualAddDialog.value) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog.value = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.anti_uninstall_manual_add_dialog_title)) },
            text = {
                Column(Modifier.fillMaxWidth(0.8f)) {
                    Text(
                        text = stringResource(R.string.anti_uninstall_manual_add_dialog_text),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = manualPackageName,
                        onValueChange = viewModel::updateManualPackageName,
                        label = { Text(stringResource(R.string.anti_uninstall_package_name_label)) },
                        placeholder = { Text(stringResource(R.string.anti_uninstall_package_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addManualPackage(context, manualPackageName)
                        showManualAddDialog.value = false
                    },
                    enabled = manualPackageName.isNotBlank()
                ) { Text(stringResource(R.string.add_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog.value = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    if (shizukuState != ShizukuState.READY) {
        AlertDialog(
            onDismissRequest = { /* Force response to use feature */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text(
                    text = when (shizukuState) {
                        ShizukuState.NOT_INSTALLED -> stringResource(R.string.anti_uninstall_shizuku_not_installed_title)
                        ShizukuState.NOT_RUNNING -> stringResource(R.string.anti_uninstall_shizuku_not_running_title)
                        ShizukuState.PERMISSION_DENIED -> stringResource(R.string.anti_uninstall_shizuku_permission_required_title)
                        else -> stringResource(R.string.anti_uninstall_shizuku_setup_title)
                    }
                )
            },
            text = {
                Column(Modifier.fillMaxWidth(0.8f)) {
                    Text(
                        text = when (shizukuState) {
                            ShizukuState.NOT_INSTALLED -> stringResource(R.string.anti_uninstall_shizuku_not_installed_text)
                            ShizukuState.NOT_RUNNING -> stringResource(R.string.anti_uninstall_shizuku_not_running_text)
                            ShizukuState.PERMISSION_DENIED -> stringResource(R.string.anti_uninstall_shizuku_permission_required_text)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (shizukuState) {
                            ShizukuState.NOT_INSTALLED -> {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://play.google.com/store/apps/details?id=${ShizukuProvider.MANAGER_APPLICATION_ID}".toUri()
                                )
                                context.startActivity(intent)
                            }

                            ShizukuState.NOT_RUNNING -> {
                                val launchIntent =
                                    context.packageManager.getLaunchIntentForPackage(ShizukuProvider.MANAGER_APPLICATION_ID)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            }

                            ShizukuState.PERMISSION_DENIED -> {
                                Shizuku.requestPermission(0)
                            }

                            else -> {}
                        }
                    }
                ) {
                    Text(
                        text = when (shizukuState) {
                            ShizukuState.NOT_INSTALLED -> stringResource(R.string.anti_uninstall_install_shizuku_button)
                            ShizukuState.NOT_RUNNING -> stringResource(R.string.anti_uninstall_open_shizuku_button)
                            ShizukuState.PERMISSION_DENIED -> stringResource(R.string.grant_permission_button)
                            else -> stringResource(R.string.confirm_button)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    navController.popBackStack()
                }) { Text(stringResource(R.string.go_back_button)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.anti_uninstall_add_manual_cd))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.anti_uninstall_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp)
                )
            }
        }
    }
}

@Composable
private fun AppProtectionItem(app: AppInfo, isProtected: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = isProtected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun ManualPackageItem(packageName: String, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.anti_uninstall_manually_added_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = true, onCheckedChange = { onToggle() })
        }
    }
}