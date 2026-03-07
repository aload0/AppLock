package dev.pranav.applock.features.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.pranav.applock.core.security.RestrictSetting
import dev.pranav.applock.core.security.RestrictSettingsState

@Composable
fun RestrictSystemSettingsSection(

    antiUninstallEnabled: Boolean,

    state: RestrictSettingsState,

    onToggle: (RestrictSetting, Boolean) -> Unit
) {

    if (!antiUninstallEnabled) return

    Column {

        Text(
            text = "Restrict System Settings Access",
            style = MaterialTheme.typography.titleMedium
        )

        SwitchPreference(
            title = "Disable Draw Over Other Apps Settings",
            checked = state.blockOverlaySettings,
            onCheckedChange = { onToggle(RestrictSetting.OVERLAY, it) }
        )

        SwitchPreference(
            title = "Disable Usage Access Settings",
            checked = state.blockUsageAccessSettings,
            onCheckedChange = { onToggle(RestrictSetting.USAGE, it) }
        )

        SwitchPreference(
            title = "Disable Accessibility Settings",
            checked = state.blockAccessibilitySettings,
            onCheckedChange = { onToggle(RestrictSetting.ACCESSIBILITY, it) }
        )

        SwitchPreference(
            title = "Disable Device Admin Settings",
            checked = state.blockDeviceAdminSettings,
            onCheckedChange = { onToggle(RestrictSetting.DEVICE_ADMIN, it) }
        )

        SwitchPreference(
            title = "Require Unrestricted Battery Usage",
            checked = state.requireBatteryExemption,
            onCheckedChange = { onToggle(RestrictSetting.BATTERY, it) }
        )
    }
}
