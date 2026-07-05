# API Reference

## Domain Models

### ShoppingCart
Immutable data class für den Einkaufswagen-Zustand.

```kotlin
data class ShoppingCart(
    val items: List<CartItem> = emptyList(),
)

val total: Float  // Berechnet: Summe aller Artikel
```

**Methoden:**
```kotlin
fun addItem(price: Float, name: String? = null): ShoppingCart
fun removeItem(id: Long): ShoppingCart
```

**Beispiel:**
```kotlin
var cart = ShoppingCart()
cart = cart.addItem(2.99f, "Milch")
println(cart.total) // 2.99
```

---

### CartItem
Einzelner Artikel im Warenkorb.

```kotlin
data class CartItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val price: Float,
)
```

---

### PriceResult
Resultat der Preisextraktion aus OCR-Text.

```kotlin
data class PriceResult(
    val price: Float? = null,           // Erkannter Preis (null wenn nicht gefunden)
    val candidates: List<Float> = emptyList(),  // Alle erkannten Preise
    val ambiguous: Boolean = false,     // True wenn mehrere unterschiedliche Preise
)
```

---

## PriceExtractor

Extrahiert Preise aus OCR-erkanntem Text mittels Regex.

```kotlin
object PriceExtractor {
    fun extractPrice(text: String?): PriceResult
}
```

**Pattern:**
- Erkennt: "12,99", "2.50", "10 , 50"
- Filtert Unit-Preise: "2,50/kg" werden erkannt aber deprioritisiert
- Validiert: 0 < price ≤ 9999

**Beispiel:**
```kotlin
val result = PriceExtractor.extractPrice("Milch 1L: 2,49 EUR")
// result.price = 2.49f
// result.ambiguous = false

val ambiguous = PriceExtractor.extractPrice("Alt: 5,99, Neu: 3,49")
// ambiguous.ambiguous = true
// ambiguous.candidates = [5.99f, 3.49f]
```

---

## ImageProcessing

OpenCV-basierte Bildverarbeitung für OCR-Vorbereitung.

```kotlin
object ImageProcessing {
    const val ROI_X_START = 0.10f  // 10%-90% horizontal
    const val ROI_X_END = 0.90f
    const val ROI_Y_START = 0.37f  // 37%-63% vertikal
    const val ROI_Y_END = 0.63f

    fun preprocessForOcr(bitmap: Bitmap): Bitmap
    fun extractRoi(bitmap: Bitmap): Bitmap
}
```

**Preprocessing-Schritte:**
1. Grayscale conversion
2. 2x upscaling (INTER_CUBIC)
3. Bilateral filter (11, 17, 17)
4. Binary threshold (OTSU)

**Beispiel:**
```kotlin
val original = camera.getFrame()
val roi = ImageProcessing.extractRoi(original)           // 10%-90% x, 37%-63% y
val preprocessed = ImageProcessing.preprocessForOcr(roi) // Optimiert für OCR
```

---

## OcrProcessor

ML Kit Text Recognition Integration mit Coroutines.

```kotlin
object OcrProcessor {
    suspend fun recognizeText(bitmap: Bitmap): String
    fun recognizeTextSync(bitmap: Bitmap, timeoutMs: Long = 10000): String?
}
```

**Async variant (empfohlen):**
```kotlin
viewModelScope.launch {
    val text = OcrProcessor.recognizeText(bitmap)
    val result = PriceExtractor.extractPrice(text)
}
```

**Sync variant (für Tests):**
```kotlin
val text = OcrProcessor.recognizeTextSync(bitmap)
```

---

## ShoppingCartRepository

Verwaltung des Einkaufswagen-Zustands mit Flow.

```kotlin
@Singleton
class ShoppingCartRepository {
    val cartFlow: Flow<ShoppingCart>  // Observable state
    
    fun getCurrentCart(): ShoppingCart
    fun addItem(price: Float, name: String? = null)
    fun removeItem(id: Long)
    fun clearCart()
}
```

**Beispiel:**
```kotlin
@Inject lateinit var repo: ShoppingCartRepository

fun addProduct(price: Float) {
    repo.addItem(price, "Artikel ${System.currentTimeMillis()}")
}

// In ViewModel:
init {
    viewModelScope.launch {
        repo.cartFlow.collect { cart ->
            _uiState.value = _uiState.value.copy(
                items = cart.items,
                total = cart.total,
            )
        }
    }
}
```

---

## ShoppingViewModel

MVVM ViewModel mit Business Logic.

```kotlin
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val cartRepository: ShoppingCartRepository,
) : ViewModel()

val uiState: StateFlow<ShoppingUiState>
```

**State:**
```kotlin
data class ShoppingUiState(
    val items: List<CartItem> = emptyList(),
    val total: Float = 0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val priceResult: PriceResult? = null,
)
```

**Öffentliche Methoden:**
```kotlin
fun addItem(price: Float, name: String? = null)
fun removeItem(id: Long)
fun clearCart()
fun processPriceFromImage(bitmap: Bitmap)  // Async: ROI → OCR → ExtractPrice
fun selectPrice(price: Float)
fun clearPriceResult()
fun clearError()
fun formatEur(value: Float): String
```

**Beispiel:**
```kotlin
val viewModel: ShoppingViewModel = hiltViewModel()
val uiState by viewModel.uiState.collectAsState()

// User scannt Foto
viewModel.processPriceFromImage(bitmap)

// Bei mehreren Preisen erkannt: Dialog zeigen
if (uiState.priceResult?.ambiguous == true) {
    PriceSelectionDialog(uiState.priceResult!!.candidates)
}

// User wählt Preis
viewModel.selectPrice(3.49f)
```

---

## CameraManager

CameraX Wrapper für Foto-Aufnahmen.

```kotlin
class CameraManager(private val context: Context) : LifecycleObserver {
    fun setUp(previewView: PreviewView, lifecycle: Lifecycle)
    suspend fun takePhoto(): Bitmap
}
```

**Beispiel:**
```kotlin
@Inject lateinit var cameraManager: CameraManager

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    cameraManager.setUp(previewView, lifecycle)
}

fun onScanPrice() {
    viewModelScope.launch {
        try {
            val bitmap = cameraManager.takePhoto()
            viewModel.processPriceFromImage(bitmap)
        } catch (e: Exception) {
            viewModel.clearError()
        }
    }
}
```

---

## UI Components (Compose)

### ShoppingCartScreen
Main-UI mit Kamera-Platzhalter, Warenliste und Bottom-Action-Bar.

```kotlin
@Composable
fun ShoppingCartScreen(
    viewModel: ShoppingViewModel = hiltViewModel(),
    onScanPrice: () -> Unit = {},
    onManualEntry: (String) -> Unit = {},
)
```

### CartItemRow
Einzelner Artikel in der Liste.

```kotlin
@Composable
fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
)
```

### ManualPriceEntryDialog
Dialog für manuelle Preiseingabe.

```kotlin
@Composable
fun ManualPriceEntryDialog(
    viewModel: ShoppingViewModel,
    onDismiss: () -> Unit,
    prefill: String = "",
)
```

### PriceSelectionDialog
Dialog bei mehreren erkannten Preisen.

```kotlin
@Composable
fun PriceSelectionDialog(
    candidates: List<Float>,
    onSelectPrice: (Float) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
)
```

---

## Dependency Injection (Hilt)

### AppModule
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideShoppingCartRepository(): ShoppingCartRepository
    
    @Singleton
    @Provides
    fun provideCameraManager(@ApplicationContext context: Context): CameraManager
}
```

Alle `@Inject` Dependency Injection wird automatisch resolved.

---

## Coroutines & Threading

Alle lange laufenden Operationen verwenden `viewModelScope.launch`:

```kotlin
viewModelScope.launch {
    try {
        // Suspend function - läuft auf IO Dispatcher
        val text = OcrProcessor.recognizeText(bitmap)
        
        // Zurück auf Main Thread für UI-Update
        _uiState.value = _uiState.value.copy(result = text)
    } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message)
    }
}
```

- `viewModelScope.launch` → Automatisches Cleanup bei ViewModel.onCleared()
- `suspend fun` → Nicht-blocking, mit Coroutine-support
- Default Dispatcher → IO Dispatcher für Disk/Network/OCR

---

## Error Handling

Alle Fehler werden im ViewModel gefangen:

```kotlin
fun processPriceFromImage(bitmap: Bitmap) {
    viewModelScope.launch {
        try {
            val roi = ImageProcessing.extractRoi(bitmap)
            val processed = ImageProcessing.preprocessForOcr(roi)
            val text = OcrProcessor.recognizeText(processed)
            val result = PriceExtractor.extractPrice(text)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                priceResult = result,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Fehler: ${e.message}",
            )
        }
    }
}
```

---

## Constants

```kotlin
// ImageProcessing
ROI_X = (0.10, 0.90)          // 10%-90% horizontal
ROI_Y = (0.37, 0.63)          // 37%-63% vertikal

// PriceExtractor
PRICE_RE = /(?<!\d)(\d{1,4})[ \t]*[.,][ \t]*(\d{2})(?!\d)/

// Validation
MIN_PRICE = 0.01f
MAX_PRICE = 9999f
```

---

## Gradle Dependencies

Siehe `build.gradle.kts` für aktuelle Versionen:

```gradle
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-ktx
androidx.compose.material3:material3
androidx.camera:camera-core
com.google.mlkit:text-recognition
org.opencv:opencv-android
com.google.dagger:hilt-android
org.jetbrains.kotlinx:kotlinx-coroutines-android
```
