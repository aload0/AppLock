package dev.pranav.applock.core.utils

import android.content.Context
import android.content.res.Configuration
import dev.pranav.applock.data.repository.PreferencesRepository
import java.util.Locale

object LocaleManager {

    fun wrap(context: Context): Context {
        val prefs = context.getSharedPreferences("app_lock_settings", Context.MODE_PRIVATE)
        val language = prefs.getString(PreferencesRepository.KEY_LANGUAGE, PreferencesRepository.LANGUAGE_SYSTEM)
            ?: PreferencesRepository.LANGUAGE_SYSTEM
        if (language == PreferencesRepository.LANGUAGE_SYSTEM) {
            return context
        }

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
