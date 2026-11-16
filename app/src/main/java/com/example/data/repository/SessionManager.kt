package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linkup_session_prefs")

class SessionManager(private val context: Context) {
    private val currentUserIdKey = stringPreferencesKey("current_user_id")
    private val lowDataModeKey = booleanPreferencesKey("low_data_mode")
    private val languageCodeKey = stringPreferencesKey("language_code")
    private val hasSeenIntroKey = booleanPreferencesKey("has_seen_intro")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[currentUserIdKey]?.takeIf { it.isNotBlank() }
    }

    val lowDataMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[lowDataModeKey] ?: false
    }

    val languageCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[languageCodeKey] ?: "en"
    }

    val hasSeenIntro: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[hasSeenIntroKey] ?: false
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "SYSTEM"
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(currentUserIdKey)
            } else {
                prefs[currentUserIdKey] = userId
            }
        }
    }

    suspend fun setLowDataMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[lowDataModeKey] = enabled
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[languageCodeKey] = code
        }
    }

    suspend fun setHasSeenIntro(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[hasSeenIntroKey] = seen
        }
    }

    suspend fun setThemeMode(modeCode: String) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = modeCode
        }
    }
}
