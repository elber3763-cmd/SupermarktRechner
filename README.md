# Einkaufs-Scanner - Android App

Eine native Android-App in Kotlin für das Scannen und Verwalten von Produktpreisen mit OCR-Unterstützung.

## Architektur

```
┌─────────────────────────────────────────────────┐
│              UI Layer (Jetpack Compose)          │
│  ShoppingCartScreen, CartItemRow, Dialogs       │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│          ViewModel Layer (Coroutines)            │
│         ShoppingViewModel, UiState              │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│        Repository & UseCase Layer                │
│  ShoppingCartRepository, OcrProcessor,          │
│  ImageProcessing, PriceExtractor                │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│          Domain Layer (Business Logic)           │
│  ShoppingCart, CartItem, PriceResult            │
└─────────────────────────────────────────────────┘
```

## Technologie-Stack

- **UI Framework**: Jetpack Compose (deklarativ, modern)
- **Architektur**: MVVM mit Coroutines
- **Dependency Injection**: Hilt
- **Kamera**: CameraX
- **OCR**: ML Kit Text Recognition (Google)
- **Bildverarbeitung**: OpenCV (optional)
- **Asynchronität**: Kotlin Coroutines & Flow

## Kernfeatures (von Python übertragen)

### 1. **Bildverarbeitung**
```kotlin
// Equivalent to Python's preprocess_for_ocr()
- Grayscale conversion
- 2x upscaling für bessere OCR
- Bilateral filtering
- Binary threshold (OTSU method)
```

### 2. **OCR mit ML Kit**
```kotlin
// Asynchron mit Coroutines (statt Python threading)
suspend fun recognizeText(bitmap: Bitmap): String
```

### 3. **Robuste Preisfilterung**
```kotlin
// Equivalent to Python's PriceExtractor.extract_price()
// Regex-Muster: "12,99" oder "2.50"
// Filtert Unit-Preise (z.B. "2,50/kg")
// Erkennt und behandelt mehrere erkannte Preise
```

### 4. **Einkaufswagen-Verwaltung**
```kotlin
// State Management mit Flow & StateFlow
// Immutable data model (CartItem, ShoppingCart)
// Real-time updates
```

## Projektstruktur

```
app/src/main/
├── kotlin/com/einkaufsscanner/
│   ├── MainActivity.kt                    # Entry point
│   ├── EinkaufsScannerApp.kt             # Hilt App
│   ├── presentation/
│   │   ├── ui/composables/
│   │   │   ├── CartScreen.kt             # Haupt-UI
│   │   │   ├── Dialogs.kt                # Dialog-Komponenten
│   │   └── viewmodel/
│   │       └── ShoppingViewModel.kt      # MVVM ViewModel
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ShoppingCart.kt           # Data Models
│   │   │   └── PriceExtractor.kt         # Geschäftslogik
│   │   └── usecase/
│   │       ├── ImageProcessing.kt        # Image processing
│   │       └── OcrProcessor.kt           # OCR integration
│   ├── data/
│   │   ├── repository/
│   │   │   └── ShoppingCartRepository.kt # Data layer
│   │   └── camera/
│   │       └── CameraManager.kt          # CameraX wrapper
│   └── di/
│       └── AppModule.kt                  # Hilt module
├── res/
│   ├── values/
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── AndroidManifest.xml
└── java/com/einkaufsscanner/
    └── [Tests]
```

## Konvertierung: Python → Kotlin

| Python | Kotlin | Zweck |
|--------|--------|-------|
| `cv2.VideoCapture` | CameraX | Kamera-Zugriff |
| `pytesseract` | ML Kit | OCR |
| `numpy` + manuelle Bildverarbeitung | OpenCV für Android | Bildverarbeitung |
| `Kivy/KivyMD` | Jetpack Compose | UI |
| `threading/asyncio` | Coroutines & Flow | Asynchronität |
| `StringProperty` | StateFlow | Reactive state |
| `namedtuple` | data class | Datenmodelle |

## Abhängigkeiten

```gradle
// Core
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.6.2

// Compose
androidx.compose.material3:material3
androidx.activity:activity-compose:1.8.0

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3

// ML Kit
com.google.mlkit:text-recognition:16.0.0

// CameraX
androidx.camera:camera-core:1.3.0
androidx.camera:camera-lifecycle:1.3.0

// Hilt
com.google.dagger:hilt-android:2.48

// OpenCV (optional)
org.opencv:opencv-android:4.8.0
```

## Build & Run

```bash
# Android Studio: File → Open → O:\SupermarktRechner
# Or command line:
./gradlew assembleDebug      # Build APK
./gradlew installDebug       # Install on device
./gradlew tasks              # List available tasks
```

## Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## Testing

Unit tests sind in `app/src/test/kotlin/`:

```kotlin
// Example: Price extraction tests
class PriceExtractorTest {
    @Test
    fun testExtractSimplePrice() { ... }
    
    @Test
    fun testDetectAmbiguousPrices() { ... }
}
```

## Nächste Schritte (Optional)

- [ ] Kamera-Integration mit CameraX vollständig testen
- [ ] OCR-Fehlerbehandlung erweitern
- [ ] UI-Tests mit Compose Test Framework hinzufügen
- [ ] Performance-Optimierungen für große Einkaufslisten
- [ ] Persistenz (Room Database) für Einkaufsverlauf
- [ ] Dunkelmodus-Unterstützung

## Lizenz

Privates Projekt
