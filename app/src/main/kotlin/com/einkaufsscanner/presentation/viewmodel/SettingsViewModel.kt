package com.einkaufsscanner.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.einkaufsscanner.data.preferences.ScannerPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val preferencesRepository = ScannerPreferencesRepository(application.applicationContext)

    // Expose scanner width as state
    val scannerWidth = preferencesRepository.scannerWidthPercent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ScannerPreferencesRepository.DEFAULT_SCANNER_WIDTH,
    )

    // Expose scanner height as state
    val scannerHeight = preferencesRepository.scannerHeightPercent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ScannerPreferencesRepository.DEFAULT_SCANNER_HEIGHT,
    )

    fun loadSettings() {
        // Settings are loaded automatically via Flow
    }

    fun updateScannerWidth(width: Float) {
        viewModelScope.launch {
            preferencesRepository.updateScannerWidth(width)
        }
    }

    fun updateScannerHeight(height: Float) {
        viewModelScope.launch {
            preferencesRepository.updateScannerHeight(height)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }
}
