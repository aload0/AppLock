package dev.pranav.applock.core.security

data class RestrictSettingsState(

    val blockOverlaySettings: Boolean = false,

    val blockUsageAccessSettings: Boolean = false,

    val blockAccessibilitySettings: Boolean = false,

    val blockDeviceAdminSettings: Boolean = false,

    val requireBatteryExemption: Boolean = false
)

enum class RestrictSetting {

    OVERLAY,

    USAGE,

    ACCESSIBILITY,

    DEVICE_ADMIN,

    BATTERY
}
