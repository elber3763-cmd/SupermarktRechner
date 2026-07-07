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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShoppingUiState(
    val items: List<CartItem> = emptyList(),
    val total: Float = 0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val priceResult: PriceResult? = null,
    val lastRecognizedText: String? = null,
    val showScannerResultDialog: Boolean = false,
    val detectedPrice: Float? = null,
    val isCameraActive: Boolean = false,
    val showClearCartConfirmation: Boolean = false,
    val editingItemId: Long? = null,
    val editingItemName: String? = null,
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

    val logoSizePercent = preferencesRepository.logoSizePercent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ScannerPreferencesRepository.DEFAULT_LOGO_SIZE,
    )

    val labelSizePercent = preferencesRepository.labelSizePercent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ScannerPreferencesRepository.DEFAULT_LABEL_SIZE,
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
        viewModelScope.launch {
            cartRepository.addItem(price, name)
        }
    }

    fun removeItem(id: Long) {
        Log.d("ShoppingViewModel", "Removing item: id=$id")
        viewModelScope.launch {
            cartRepository.removeItem(id)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }

    fun showClearCartConfirmation() {
        _uiState.value = _uiState.value.copy(showClearCartConfirmation = true)
    }

    fun hideClearCartConfirmation() {
        _uiState.value = _uiState.value.copy(showClearCartConfirmation = false)
    }

    fun startEditingItem(item: CartItem) {
        _uiState.value = _uiState.value.copy(
            showScannerResultDialog = true,
            detectedPrice = item.price,
            editingItemId = item.id,
            editingItemName = item.name,
            isCameraActive = false
        )
    }

    fun cancelEditingItem() {
        _uiState.value = _uiState.value.copy(
            showScannerResultDialog = false,
            detectedPrice = null,
            editingItemId = null,
            editingItemName = null
        )
    }

    /**
     * Process image: extract ROI, preprocess, run OCR, extract price
     * Fully asynchronous with Coroutines
     * DEBUG: Can be set to skip ROI crop for full-image testing
     */
    fun processPriceFromImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    lastRecognizedText = null
                )

                // Run ALL heavy processing on CPU thread to avoid blocking Main Thread
                val startTime = System.currentTimeMillis()
                val result = withContext(Dispatchers.Default) {
                    Log.d("ShoppingViewModel", "========== Image Processing Pipeline ==========")
                    Log.d("ShoppingViewModel", "Original image: ${bitmap.width}x${bitmap.height}")

                    // Downscale huge camera image first (12MP → ~1400px wide for better OCR)
                    val downscaled = ImageProcessing.downscaleBitmap(bitmap)
                    Log.d("ShoppingViewModel", "After downscaling: ${downscaled.width}x${downscaled.height}")

                    // DEBUG MODE: Test full image without crop
                    val imageForOcr = downscaled
                    Log.d("ShoppingViewModel", "⚠️  DEBUG: Using FULL IMAGE (crop disabled for testing)")
                    Log.d("ShoppingViewModel", "Final image sent to OCR: ${imageForOcr.width}x${imageForOcr.height}")

                    // Preprocess for OCR (minimal - just preserve colors)
                    val processed = ImageProcessing.preprocessForOcr(imageForOcr)
                    Log.d("ShoppingViewModel", "After preprocessing: ${processed.width}x${processed.height}")

                    // Run ML Kit OCR asynchronously
                    Log.d("ShoppingViewModel", "Sending to ML Kit OCR...")
                    val recognizedText = OcrProcessor.recognizeText(processed)
                    Log.d("ShoppingViewModel", "OCR returned: '$recognizedText'")

                    // Extract price from text
                    val priceResult = PriceExtractor.extractPrice(recognizedText)
                    Log.d("ShoppingViewModel", "Price extraction result: ${priceResult.price}€")

                    Pair(recognizedText, priceResult)
                }

                val processingTime = System.currentTimeMillis() - startTime
                Log.d("ShoppingViewModel", "✅ Total processing time: ${processingTime}ms")

                val (recognizedText, priceResult) = result

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    priceResult = priceResult,
                    lastRecognizedText = recognizedText
                )

                // Show unified scanner result dialog for ALL cases:
                // - If price found: show with pre-filled price
                // - If price NOT found: show with empty price field for manual entry
                _uiState.value = _uiState.value.copy(
                    detectedPrice = priceResult.price,
                    showScannerResultDialog = true,
                    priceResult = null,
                    isCameraActive = false
                )
            } catch (e: Exception) {
                Log.e("ShoppingViewModel", "❌ Processing failed", e)
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

    fun openManualInputDialog() {
        _uiState.value = _uiState.value.copy(
            showScannerResultDialog = true,
            detectedPrice = null,
            isCameraActive = false
        )
    }

    fun closeScannerResultDialog() {
        _uiState.value = _uiState.value.copy(
            showScannerResultDialog = false,
            detectedPrice = null,
            isCameraActive = false
        )
    }

    fun addItemFromScannerResult(price: Float, name: String, quantity: Int) {
        val editingId = _uiState.value.editingItemId
        val selectedManualId = _selectedManualItemId.value

        when {
            selectedManualId != null -> {
                // Add price to selected manual item
                addPriceToManualItem(price, quantity)
            }
            editingId != null -> {
                // Update existing item
                val totalPrice = price * quantity
                viewModelScope.launch {
                    cartRepository.updateItem(editingId, totalPrice, name)
                    _uiState.value = _uiState.value.copy(
                        showScannerResultDialog = false,
                        detectedPrice = null,
                        editingItemId = null,
                        editingItemName = null,
                        isCameraActive = false
                    )
                }
            }
            else -> {
                // Add new item
                val totalPrice = price * quantity
                addItem(totalPrice, name)
                _uiState.value = _uiState.value.copy(
                    showScannerResultDialog = false,
                    detectedPrice = null,
                    isCameraActive = false
                )
            }
        }
    }

    fun activateCamera() {
        Log.d("ShoppingViewModel", "🎥 activateCamera() called - setting isCameraActive = true")
        _uiState.value = _uiState.value.copy(isCameraActive = true)
        Log.d("ShoppingViewModel", "State updated. isCameraActive is now: ${_uiState.value.isCameraActive}")
    }

    fun deactivateCamera() {
        Log.d("ShoppingViewModel", "📷 deactivateCamera() called - setting isCameraActive = false")
        _uiState.value = _uiState.value.copy(isCameraActive = false)
    }

    /**
     * NEW: Process recognized text directly from native camera ML Kit analysis
     * No bitmap manipulation, pure text extraction
     */
    fun processRecognizedText(recognizedText: String) {
        viewModelScope.launch {
            try {
                Log.d("ShoppingViewModel", "Native ML Kit text: '$recognizedText'")

                if (recognizedText.isEmpty()) {
                    Log.d("ShoppingViewModel", "No text recognized in this frame")
                    return@launch
                }

                // Extract price from the recognized text
                val priceResult = PriceExtractor.extractPrice(recognizedText)

                Log.d("ShoppingViewModel", "Extracted price: ${priceResult.price}€")

                // Only show dialog if a price was found (price is nullable Float?)
                val detectedPrice = priceResult.price ?: 0f
                if (detectedPrice > 0f) {
                    _uiState.value = _uiState.value.copy(
                        detectedPrice = detectedPrice,
                        showScannerResultDialog = true,
                        lastRecognizedText = recognizedText,
                        isCameraActive = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ShoppingViewModel", "Error processing recognized text", e)
            }
        }
    }

    fun formatEur(value: Float): String {
        return String.format("%.2f EUR", value).replace(".", ",")
    }

    // Manual Shopping List Functions
    val cartItems = cartRepository.getManualItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList(),
    )

    val scannedItems = cartRepository.getScannedItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList(),
    )

    fun addManualItem(name: String, price: Float, quantity: Int) {
        viewModelScope.launch {
            cartRepository.addManualItem(name, price, quantity)
        }
    }

    fun updateItem(item: com.einkaufsscanner.data.database.entities.ShoppingItemEntity) {
        viewModelScope.launch {
            cartRepository.updateManualItem(item)
        }
    }

    fun updateItemCheckedStatus(id: Long, isChecked: Boolean) {
        viewModelScope.launch {
            cartRepository.updateItemCheckedStatus(id, isChecked)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            cartRepository.deleteShoppingItem(id)
        }
    }

    private val _selectedManualItemId = MutableStateFlow<Long?>(null)
    val selectedManualItemId = _selectedManualItemId.asStateFlow()

    private val _scannerDialogQuantity = MutableStateFlow("1")
    val scannerDialogQuantity = _scannerDialogQuantity.asStateFlow()

    fun selectManualItem(itemId: Long?) {
        _selectedManualItemId.value = itemId
    }

    fun setScannerDialogQuantity(quantity: String) {
        _scannerDialogQuantity.value = quantity.filter { it.isDigit() }
    }

    fun resetScannerDialogQuantity() {
        _scannerDialogQuantity.value = "1"
    }

    fun addPriceToManualItem(price: Float, quantity: Int = 1) {
        val selectedId = _selectedManualItemId.value ?: return
        viewModelScope.launch {
            cartRepository.convertManualToScanned(selectedId, price * quantity)
            _selectedManualItemId.value = null
            _uiState.value = _uiState.value.copy(
                showScannerResultDialog = false,
                detectedPrice = null,
                isCameraActive = false
            )
        }
    }

}
