package com.fyp.rebarcountingapp

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}

// In a Repository or ViewModel:
suspend fun setOnboardingCompleted(context: Context) {
    context.dataStore.edit { prefs ->
        prefs[PreferencesKeys.ONBOARDING_COMPLETED] = true
    }
}

suspend fun hasOnboardingCompleted(context: Context): Boolean {
    val prefs = context.dataStore.data.first()
    return prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
}