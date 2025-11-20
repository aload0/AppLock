package com.pranavpurwar.applock.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.pranavpurwar.applock.data.datastore.DatastoreRepository // Import the repository you created
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Utility class to handle Biometric authentication. 
 * Requires the DatastoreRepository to check the user's preferred authentication method.
 */
class BiometricUtils @Inject constructor(
    // We inject the repository here to access the 'Fingerprint Only' setting
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val datastoreRepository: DatastoreRepository
) {

    /**
     * Checks if biometric authentication is available on the device.
     */
    fun isBiometricReady(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    /**
     * Determines the correct set of authenticators based on the user's preference 
     * read from the DatastoreRepository.
     */
    private suspend fun getAllowedAuthenticators(): Int {
        // CORE LOGIC: Read the 'Fingerprint Only' setting
        val isFingerprintOnly = datastoreRepository.isFingerprintOnly.first()

        // ALWAYS allow the Device Credential (PIN/Pattern/Password) as a secure fallback
        val deviceCredential = BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        return if (isFingerprintOnly) {
            // Fingerprint Only: allows fingerprint or PIN/Pattern
            BiometricManager.Authenticators.BIOMETRIC_FINGERPRINT or deviceCredential
        } else {
            // Default: Allows all strong biometrics (Finger/Face/Iris) + Device Credential
            BiometricManager.Authenticators.BIOMETRIC_STRONG or deviceCredential
        }
    }


    /**
     * Builds the authentication prompt info, incorporating the user's biometric preference.
     * This function must be 'suspend' because it reads a Flow from the repository.
     */
    suspend fun getPromptInfo(
        title: String,
        subtitle: String
    ): BiometricPrompt.PromptInfo {
        // Use the function to get the dynamic allowed authenticators flag
        val authenticators = getAllowedAuthenticators() 

        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators) 
            .build()
    }

    /**
     * Creates and starts the biometric authentication process.
     */
    fun startBiometric(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        callback: BiometricPrompt.AuthenticationCallback
    ) {
        val biometricPrompt = BiometricPrompt(activity, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}