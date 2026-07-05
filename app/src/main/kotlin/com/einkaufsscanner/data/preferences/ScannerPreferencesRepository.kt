package com.einkaufsscanner.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scanner_settings")

class ScannerPreferencesRepository(private val context: Context) {

    companion object {
        private val SCANNER_WIDTH_KEY = floatPreferencesKey("scanner_width_percent")
        private val SCANNER_HEIGHT_KEY = floatPreferencesKey("scanner_height_percent")

        const val DEFAULT_SCANNER_WIDTH = 0.98f  // 98% of width
        const val DEFAULT_SCANNER_HEIGHT = 0.84f // 84% of height
        const val MIN_SCANNER_SIZE = 0.10f       // 10% minimum (relaxed from 20%)
        const val MAX_SCANNER_SIZE = 1.0f        // 100% maximum (FULL SCREEN!)
    }

    // Read scanner width as Flow
    val scannerWidthPercent: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SCANNER_WIDTH_KEY] ?: DEFAULT_SCANNER_WIDTH
    }

    // Read scanner height as Flow
    val scannerHeightPercent: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SCANNER_HEIGHT_KEY] ?: DEFAULT_SCANNER_HEIGHT
    }

    // Update scanner width
    suspend fun updateScannerWidth(width: Float) {
        val validWidth = width.coerceIn(MIN_SCANNER_SIZE, MAX_SCANNER_SIZE)
        context.dataStore.edit { preferences ->
            preferences[SCANNER_WIDTH_KEY] = validWidth
        }
    }

    // Update scanner height
    suspend fun updateScannerHeight(height: Float) {
        val validHeight = height.coerceIn(MIN_SCANNER_SIZE, MAX_SCANNER_SIZE)
        context.dataStore.edit { preferences ->
            preferences[SCANNER_HEIGHT_KEY] = validHeight
        }
    }

    // Reset to defaults
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences[SCANNER_WIDTH_KEY] = DEFAULT_SCANNER_WIDTH
            preferences[SCANNER_HEIGHT_KEY] = DEFAULT_SCANNER_HEIGHT
        }
    }
}
