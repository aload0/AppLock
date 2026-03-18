package dev.pranav.applock.core.utils

import android.content.Context
import androidx.biometric.BiometricManager

sealed class BiometricStatus {
    data object Available : BiometricStatus()
    data class Unavailable(val message: String) : BiometricStatus()
}

fun getBiometricStatus(context: Context): BiometricStatus {
    val biometricManager = BiometricManager.from(context)
    return when (
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.Unavailable(
            "Sorry this app cant run on this device because of security limitation: no biometric hardware."
        )
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.Unavailable(
            "Sorry this app cant run on this device because of security limitation: biometric not enabled."
        )
        else -> BiometricStatus.Unavailable(
            "Sorry this app cant run on this device because of security limitation: biometric authentication unavailable."
        )
    }
}
