package com.pranavpurwar.applock.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Defines all keys used for storing user preferences in DataStore.
 */
object PreferencesKeys {
    // Existing key for the PIN (Required for app function)
    val KEY_PIN = stringPreferencesKey("pin_key")

    // The new key for the biometric toggle
    val FINGERPRINT_ONLY_BIOMETRICS = booleanPreferencesKey("fingerprint_only_biometrics")
}