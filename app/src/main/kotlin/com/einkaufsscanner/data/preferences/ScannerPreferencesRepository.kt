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
        private val LOGO_SIZE_KEY = floatPreferencesKey("logo_size_percent")
        private val LABEL_SIZE_KEY = floatPreferencesKey("label_size_percent")

        const val DEFAULT_SCANNER_WIDTH = 0.98f  // 98% of width
        const val DEFAULT_SCANNER_HEIGHT = 0.84f // 84% of height
        const val MIN_SCANNER_SIZE = 0.10f       // 10% minimum (relaxed from 20%)
        const val MAX_SCANNER_SIZE = 1.0f        // 100% maximum (FULL SCREEN!)

        const val DEFAULT_LOGO_SIZE = 1.0f       // 100% (normal size)
        const val MIN_LOGO_SIZE = 0.5f           // 50% minimum
        const val MAX_LOGO_SIZE = 2.0f           // 200% maximum

        const val DEFAULT_LABEL_SIZE = 1.0f      // 100% (normal size)
        const val MIN_LABEL_SIZE = 0.6f          // 60% minimum
        const val MAX_LABEL_SIZE = 1.8f          // 180% maximum
    }

    // Read scanner width as Flow
    val scannerWidthPercent: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SCANNER_WIDTH_KEY] ?: DEFAULT_SCANNER_WIDTH
    }

    // Read scanner height as Flow
    val scannerHeightPercent: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SCANNER_HEIGHT_KEY] ?: DEFAULT_SCANNER_HEIGHT
    }

    // Read logo size as Flow
    val logoSizePercent: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[LOGO_SIZE_KEY] ?: DEFAULT_LOGO_SIZE
    }

    // Read label size as Flow
    val labelSizePercent: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[LABEL_SIZE_KEY] ?: DEFAULT_LABEL_SIZE
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

    // Update logo size
    suspend fun updateLogoSize(size: Float) {
        val validSize = size.coerceIn(MIN_LOGO_SIZE, MAX_LOGO_SIZE)
        context.dataStore.edit { preferences ->
            preferences[LOGO_SIZE_KEY] = validSize
        }
    }

    // Update label size
    suspend fun updateLabelSize(size: Float) {
        val validSize = size.coerceIn(MIN_LABEL_SIZE, MAX_LABEL_SIZE)
        context.dataStore.edit { preferences ->
            preferences[LABEL_SIZE_KEY] = validSize
        }
    }

    // Reset to defaults
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences[SCANNER_WIDTH_KEY] = DEFAULT_SCANNER_WIDTH
            preferences[SCANNER_HEIGHT_KEY] = DEFAULT_SCANNER_HEIGHT
            preferences[LOGO_SIZE_KEY] = DEFAULT_LOGO_SIZE
            preferences[LABEL_SIZE_KEY] = DEFAULT_LABEL_SIZE
        }
    }
}
