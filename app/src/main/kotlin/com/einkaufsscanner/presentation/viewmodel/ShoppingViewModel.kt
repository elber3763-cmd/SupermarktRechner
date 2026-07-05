package com.einkaufsscanner.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.einkaufsscanner.data.repository.ShoppingCartRepository
import com.einkaufsscanner.data.preferences.ScannerPreferencesRepository
import com.einkaufsscanner.domain.model.CartItem
import com.einkaufsscanner.domain.model.PriceExtractor
import com.einkaufsscanner.domain.model.PriceResult
import com.einkaufsscanner.domain.usecase.ImageProcessing
import com.einkaufsscanner.domain.usecase.OcrProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val items: List<CartItem> = emptyList(),
    val total: Float = 0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val priceResult: PriceResult? = null,
    val lastRecognizedText: String? = null,
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val cartRepository: ShoppingCartRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val preferencesRepository = ScannerPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState = _uiState.asStateFlow()

    // Expose scanner dimensions from preferences
    val scannerWidthPercent = preferencesRepository.scannerWidthPercent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ScannerPreferencesRepository.DEFAULT_SCANNER_WIDTH,
    )

    val scannerHeightPercent = preferencesRepository.scannerHeightPercent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ScannerPreferencesRepository.DEFAULT_SCANNER_HEIGHT,
    )

    private val _cartFlow: Flow<com.einkaufsscanner.domain.model.ShoppingCart> = cartRepository.cartFlow

    init {
        viewModelScope.launch {
            _cartFlow.collect { cart ->
                _uiState.value = _uiState.value.copy(
                    items = cart.items,
                    total = cart.total,
                )
            }
        }
    }

    fun addItem(price: Float, name: String? = null) {
        Log.d("ShoppingViewModel", "Adding item: price=$price, name=$name")
        cartRepository.addItem(price, name)
    }

    fun removeItem(id: Long) {
        Log.d("ShoppingViewModel", "Removing item: id=$id")
        cartRepository.removeItem(id)
    }

    fun clearCart() {
        cartRepository.clearCart()
    }

    /**
     * Process image: extract ROI, preprocess, run OCR, extract price
     * Fully asynchronous with Coroutines
     */
    fun processPriceFromImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    lastRecognizedText = null
                )

                // Extract Region of Interest with dynamic dimensions
                val widthPercent = scannerWidthPercent.value
                val heightPercent = scannerHeightPercent.value
                val roi = ImageProcessing.extractRoi(bitmap, widthPercent, heightPercent)
                Log.d("ShoppingViewModel", "Extracted ROI with dimensions: ${widthPercent*100}% x ${heightPercent*100}%")

                // Preprocess for OCR
                val processed = ImageProcessing.preprocessForOcr(roi)

                // Run ML Kit OCR asynchronously
                val recognizedText = OcrProcessor.recognizeText(processed)
                Log.d("ShoppingViewModel", "OCR Result: '$recognizedText'")

                // Extract price from text
                val priceResult = PriceExtractor.extractPrice(recognizedText)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    priceResult = priceResult,
                    lastRecognizedText = recognizedText
                )

                if (priceResult.price == null) {
                    val msg = if (recognizedText.isBlank()) 
                        "Kein Text erkannt. Bitte näher ran oder besser beleuchten." 
                      else 
                        "Text erkannt, aber kein Preis gefunden. Probiere es noch einmal!"
                    
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                    return@launch
                }

                // Auto-add if unambiguous
                if (!priceResult.ambiguous) {
                    addItem(priceResult.price)
                    _uiState.value = _uiState.value.copy(priceResult = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fehler bei der Verarbeitung: ${e.message}",
                )
            }
        }
    }

    fun selectPrice(price: Float) {
        addItem(price)
        _uiState.value = _uiState.value.copy(priceResult = null)
    }

    fun clearPriceResult() {
        _uiState.value = _uiState.value.copy(priceResult = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun formatEur(value: Float): String {
        return String.format("%.2f EUR", value).replace(".", ",")
    }
}
