package com.pranavpurwar.applock.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository to handle all reading and writing operations for DataStore preferences.
 * This class is injected using Hilt and manages keys defined in PreferencesKeys.kt.
 */
class DatastoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {

    // --- Existing PIN Logic (Inferred from app's core function) ---

    /** Retrieves the saved PIN as a Flow. */
    val getPin: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KEY_PIN]
        }

    /** Saves a new PIN to DataStore. */
    suspend fun setPin(pin: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_PIN] = pin
        }
    }
    
    // --- NEW BIOMETRIC TOGGLE LOGIC ---

    /**
     * Flow of the current state of the 'Fingerprint Only' setting. 
     * Defaults to false (Face and Finger allowed).
     */
    val isFingerprintOnly: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FINGERPRINT_ONLY_BIOMETRICS] ?: false
        }

    /**
     * Saves the user's choice for the biometric mode.
     */
    suspend fun setFingerprintOnly(isOnlyFingerprint: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FINGERPRINT_ONLY_BIOMETRICS] = isOnlyFingerprint
        }
    }
    // --- END NEW BIOMETRIC LOGIC ---
}