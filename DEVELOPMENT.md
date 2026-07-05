# Entwicklungsanleitung

## Setup

### Voraussetzungen
- Android Studio 2023.1 oder höher
- JDK 17+
- Android SDK 28+ (min SDK für App)
- Android SDK 34 (target SDK)

### Projekt öffnen
1. Android Studio starten
2. **File → Open** → `O:\SupermarktRechner`
3. Gradle Sync durchführen (Auto-Prompt bei Öffnen)

## Build

### Debug APK
```bash
./gradlew assembleDebug
```
APK wird erstellt: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew assembleRelease
```

### Install auf Gerät
```bash
./gradlew installDebug
```

## Tests

### Unit Tests ausführen
```bash
./gradlew test
```

### Specific Test Class
```bash
./gradlew test --tests com.einkaufsscanner.domain.model.PriceExtractorTest
```

### Test Reports
Nach dem Run: `app/build/reports/tests/testDebugUnitTest/index.html`

### UI/Instrumentation Tests (auf echtem Gerät/Emulator)
```bash
./gradlew connectedAndroidTest
```

## Architektur-Details

### MVVM-Muster

```
View (Composable)
    ↓
ViewModel (Business Logic)
    ↓
Repository (Data Access)
    ↓
Domain Models (Entities)
```

### Coroutines & Flow

```kotlin
// ViewModel sammelt Daten über Flow
init {
    viewModelScope.launch {
        cartRepository.cartFlow.collect { cart ->
            _uiState.value = _uiState.value.copy(
                items = cart.items,
                total = cart.total,
            )
        }
    }
}

// ViewModel.processPriceFromImage() ist asynchron mit Coroutines
fun processPriceFromImage(bitmap: Bitmap) {
    viewModelScope.launch {
        try {
            // Alle diese Operationen laufen auf IO Dispatcher
            val roi = ImageProcessing.extractRoi(bitmap)
            val processed = ImageProcessing.preprocessForOcr(roi)
            val text = OcrProcessor.recognizeText(processed) // suspend
            // ...
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "...")
        }
    }
}
```

### Hilt Dependency Injection

```kotlin
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val cartRepository: ShoppingCartRepository,
) : ViewModel()
```

Dependencies werden in `di/AppModule.kt` definiert:
```kotlin
@Provides
@Singleton
fun provideShoppingCartRepository(): ShoppingCartRepository {
    return ShoppingCartRepository()
}
```

### Jetpack Compose

Alle UI-Komponenten sind deklarativ:
```kotlin
@Composable
fun CartItemRow(item: CartItem, onRemove: () -> Unit) {
    Card(...) {
        Row(...) {
            Text(item.name)
            Text(String.format("%.2f EUR", item.price))
            IconButton(onClick = onRemove) { ... }
        }
    }
}
```

## Wichtige Dateien

| File | Zweck |
|------|-------|
| `MainActivity.kt` | App Entry Point, Compose setContent |
| `ShoppingViewModel.kt` | Business Logic, State Management |
| `ShoppingCartRepository.kt` | Data Layer mit StateFlow |
| `PriceExtractor.kt` | Geschäftslogik: Preisfilterung |
| `OcrProcessor.kt` | ML Kit Integration (async) |
| `ImageProcessing.kt` | OpenCV Integration |
| `CartScreen.kt` | Main UI mit Compose |
| `build.gradle.kts` | Dependencies & Gradle config |

## Debugging

### Logcat Filter
```bash
./gradlew logcat -v app  # Nur App-Logs
```

### Breakpoints in Android Studio
1. Klick auf Zeilennummer im Editor
2. Debug-Modus starten (Shift+F9)
3. Debugger-Panel verwenden

### Network/ML Kit Debugging
```kotlin
// In OcrProcessor
try {
    val text = OcrProcessor.recognizeText(bitmap)
    Log.d("OCR", "Recognized: $text")
} catch (e: Exception) {
    Log.e("OCR", "Error", e)
}
```

## Performance-Tipps

### Bild-Verarbeitung
- ROI Extraction vor OCR (10%-90% x, 37%-63% y)
- Preprocessing (bilateralFilter, threshold) dauert ~100ms
- OCR async mit Coroutines laufen lassen

### State Management
- Flow/StateFlow für Reactive UI
- viewModelScope für automatisches Cleanup
- Immutable data classes für kartesische CartItems

### Speicher
- Bitmaps in viewModelScope.launch freigeben
- CameraManager.shutdown() bei onDestroy
- Executor in CameraManager.cameraExecutor

## Gradle Tasks

```bash
./gradlew help              # Alle Tasks anzeigen
./gradlew clean             # Build cache löschen
./gradlew build             # Kompilieren & verpacken
./gradlew test              # Unit Tests
./gradlew connectedTest     # Instrumentation Tests
./gradlew lint              # Code-Qualität checken
./gradlew dependencies      # Dependencies visualisieren
```

## Häufige Probleme

### "Unable to load OpenCV"
→ OpenCV ist optional. App funktioniert auch ohne (mit ML Kit OCR)

### "Module not found: androidx.compose..."
→ `./gradlew clean` dann Sync in Android Studio

### "Camera permission denied"
→ Manuell in Settings erlauben oder Emulator-Simulator nutzen

### OCR timeout auf ML Kit
→ `OcrProcessor.recognizeText()` hat 10s Timeout. Timeout anpassen falls nötig.

## CI/CD Integration

Für GitHub Actions (.github/workflows/build.yml):
```yaml
- name: Run tests
  run: ./gradlew test

- name: Build APK
  run: ./gradlew assembleDebug

- name: Upload APK
  uses: actions/upload-artifact@v3
  with:
    name: app-debug.apk
    path: app/build/outputs/apk/debug/app-debug.apk
```

## Releases

### Version Management
Bearbeite in `build.gradle.kts`:
```kotlin
versionCode = 1
versionName = "1.0"
```

### Signed Release Build
```bash
./gradlew bundleRelease  # Android App Bundle für Play Store
```

## Ressourcen

- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [CameraX Guide](https://developer.android.com/training/camerax)
- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition)
- [OpenCV Android](https://docs.opencv.org/4.x/d3/d63/classcv_1_1Mat.html)
