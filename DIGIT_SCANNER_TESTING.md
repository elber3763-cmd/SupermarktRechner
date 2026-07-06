# 🎯 Digit Scanner Testing Guide

## Feature: Live Handwritten Digit Recognition via CameraX + ML Kit

Dieses Dokument beschreibt, wie Sie die neue **Digit Scanner**-Funktionalität testen können.

---

## 📋 Was wurde implementiert?

### Live-Zahlen-Scanning
- **CameraX ImageAnalysis** für kontinuierliche Frame-Verarbeitung
- **ML Kit Text Recognition v2** (On-Device OCR)
- **Ziffern-Filter** - nur Zahlen und Kommas
- **Preis-Formatierung** - "1299" → "12,99€"

### UI Integration
- **"Zahlen" Button** in der Bottom Action Bar
- **DigitScannerScreen** mit Live-Kamera-Preview
- **Erkannte Zahlen Display** mit Live-Feedback
- **Integration mit ScannerResultDialog**

---

## 🚀 Installation & Start

### 1. App Bauen
```bash
cd O:\SupermarktRechner
./gradlew assembleDebug
```

### 2. App auf Emulator/Gerät installieren
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. App starten
```bash
adb shell am start -n com.einkaufsscanner/.MainActivity
```

---

## ✅ Test-Szenarien

### Test 1: UI Integration
**Ziel**: Überprüfen, dass der "Zahlen"-Button vorhanden ist und die Digit Scanner Screen öffnet

**Schritte**:
1. App starten
2. Zur Shopping-Cart-Liste navigieren
3. **Bottom Action Bar** überprüfen:
   - Sollte 3 Buttons haben: "Manuell", "Zahlen", "Kamera"
4. Auf "Zahlen" Button klicken
5. ✅ **DigitScannerScreen sollte sich öffnen** mit:
   - Top AppBar mit "Zahlen scannen" Titel
   - Live-Kamera-Preview (schwarzer Bereich)
   - Text "Halten Sie Zahlen vor die Kamera"
   - "Abbrechen" und "Übernehmen" Buttons

### Test 2: Kamera-Berechtigung
**Ziel**: Runtime-Berechtigung für Kamera wird abgefragt

**Schritte**:
1. App starten
2. "Zahlen" Button klicken
3. ✅ **Berechtigungsdialog sollte erscheinen** (falls noch nicht gewährt)
4. **"Erlauben" klicken**
5. Kamera-Preview sollte sich aktivieren

### Test 3: Live Digit Detection
**Ziel**: Überprüfen, dass ML Kit Zahlen erkennt

**Schritte**:
1. DigitScannerScreen öffnen
2. Eine **handgeschriebene oder gedruckte Ziffer** vor die Kamera halten (z.B. "2")
3. Warten Sie ~1 Sekunde
4. ✅ **Erkannte Ziffer sollte in der Box angezeigt werden**:
   ```
   Erkannte Zahlen:
   2
   ```

### Test 4: Mehrstellige Zahlen
**Ziel**: Überprüfen, dass mehrstellige Preise erkannt werden

**Schritte**:
1. DigitScannerScreen öffnen
2. Ein Blatt Papier mit "1299" darauf vor die Kamera halten
3. Warten Sie auf Erkennung
4. ✅ **Display sollte zeigen**:
   ```
   Erkannte Zahlen:
   1299
   ```

### Test 5: Ziffern-Filterung
**Ziel**: Überprüfen, dass nur Ziffern und Kommas erkannt werden

**Schritte**:
1. DigitScannerScreen öffnen
2. Ein Blatt mit "PREIS: 12,99€" darauf vor die Kamera halten
3. ✅ **Display sollte nur "1299" oder "12,99" zeigen** (Buchstaben herausgefiltert)

### Test 6: Preis-Integration
**Ziel**: Überprüfen, dass erkannte Zahlen als Preis übernommen werden

**Schritte**:
1. DigitScannerScreen öffnen
2. "299" erkennen (sollte "2,99" anzeigen)
3. "Übernehmen" Button klicken
4. ✅ **ScannerResultDialog sollte sich öffnen** mit:
   - Erkannter Preis: "2,99 EUR"
   - Eingabefelder für Name und Menge
5. Preis überprüfen und "Bestätigen" klicken
6. ✅ **Artikel sollte zur Shopping-List hinzugefügt werden**

### Test 7: Abbrechen-Funktion
**Ziel**: Überprüfen, dass Man den Scanner abbrechen kann

**Schritte**:
1. DigitScannerScreen öffnen
2. "Abbrechen" Button klicken
3. ✅ **Scanner sollte sich schließen**
4. **Zurück zur Hauptoberfläche**

### Test 8: "Übernehmen" deaktiviert (keine Ziffern)
**Ziel**: Überprüfen, dass "Übernehmen"-Button nur aktiv ist, wenn Ziffern erkannt wurden

**Schritte**:
1. DigitScannerScreen öffnen (noch keine Erkennung)
2. ✅ **"Übernehmen"-Button sollte grau/deaktiviert sein**
3. Ziffern erkennen
4. ✅ **"Übernehmen"-Button sollte aktiv werden**

---

## 🔍 Code Integration - Was wurde geändert?

### Neue Dateien:
```
✅ app/src/main/kotlin/com/einkaufsscanner/util/PermissionRequester.kt
✅ app/src/main/kotlin/com/einkaufsscanner/presentation/ui/composables/DigitScannerScreen.kt
```

### Erweiterte Dateien:
```
✅ app/src/main/kotlin/com/einkaufsscanner/data/camera/CameraManager.kt
   - startLiveDigitScanning()
   - analyzeFrameForDigits()
   - onLiveDigitDetected Callback

✅ app/src/main/kotlin/com/einkaufsscanner/presentation/viewmodel/ShoppingViewModel.kt
   - showDigitScanner State
   - openDigitScanner()
   - closeDigitScanner()
   - processDetectedDigits()

✅ app/src/main/kotlin/com/einkaufsscanner/domain/model/PriceExtractor.kt
   - extractDigitsOnly()
   - formatLiveDigitsAsPrice()

✅ app/src/main/kotlin/com/einkaufsscanner/presentation/ui/composables/CartScreen.kt
   - DigitScannerScreen Integration
   - 3-Button BottomActionBar
   - PermissionRequester Check

✅ app/build.gradle.kts
   - camera-extensions:1.3.0 Added

✅ app/src/main/AndroidManifest.xml
   - Already has CAMERA permission
```

---

## 🧪 Unit Tests für neue Funktionen

### Ziffern-Filterung testen:
```kotlin
@Test
fun testExtractDigitsOnly() {
    val input = "PREIS: 12,99€"
    val result = PriceExtractor.extractDigitsOnly(input)
    assertEquals("1299", result)
}
```

### Preis-Formatierung testen:
```kotlin
@Test
fun testFormatLiveDigitsAsPrice() {
    val digits = "1299"
    val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
    assertEquals("12,99", result)
}
```

### Edge Cases:
```kotlin
@Test
fun testFormatLiveDigitsAsPrice_TooSmall() {
    val digits = "1" // 0,01€ - unter Minimum
    val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
    assertNull(result)
}

@Test
fun testFormatLiveDigitsAsPrice_Valid() {
    val digits = "5" // 0,05€ - Minimum
    val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
    assertEquals("0,05", result)
}
```

---

## 🐛 Häufige Fehler & Lösungen

| Problem | Lösung |
|---------|--------|
| Kamera-Preview schwarz/blau | Kamera-Berechtigung erteilen |
| Keine Zahlen erkannt | Zahlen näher zur Kamera oder besser beleuchten |
| "Übernehmen" ist grau | ML Kit hat noch keine Zahlen erkannt |
| Scanner öffnet sich nicht | Überprüfen Sie Logcat auf Fehler |
| Preis wird nicht übernommen | Überprüfen Sie, ob Preis im gültigen Bereich ist (0,05€ - 9999,99€) |

---

## 📊 Performance-Hinweise

- **Frame-Drosselung**: 500ms zwischen Updates
- **Backpressure Strategy**: `STRATEGY_KEEP_ONLY_LATEST` (neueste Frames)
- **Executor**: Single-threaded für Sequential Processing
- **ML Kit**: On-Device (keine Cloud-Latenz)

---

## 🎥 Logcat Debugging

Zum Debuggen können Sie folgende Logs überprüfen:

```bash
adb logcat | grep -E "DigitScanner|CameraManager|OcrProcessor|ShoppingViewModel"
```

**Erwartete Logs**:
```
D/DigitScanner: Detected digits: 1299
D/CameraManager: 📊 Digits detected: 1299
D/ShoppingViewModel: Processing detected digits: 1299
D/ShoppingViewModel: Formatted price: 12,99 -> 12.99
```

---

## ✨ Features zum Testen

- ✅ Live-Kamera-Preview
- ✅ Continuous Frame Analysis
- ✅ Ziffern-Filterung
- ✅ Preis-Formatierung
- ✅ Drosselung/Throttling
- ✅ Runtime Permissions
- ✅ UI-Integration
- ✅ Dialog-Workflow
- ✅ Validierung (Min/Max Preis)

---

## 📝 Notizen

- Die **Digit Scanner** ist optimiert für **handgeschriebene Zahlen**
- ML Kit hat bereits **aggressive Handwriting-Normalisierung** in `cleanOcrTextForHandwriting()`
- Der Scanner ist **auf Deutsch** ("Zahlen scannen", "Übernehmen", etc.)
- Die **Kamera bleibt offen**, bis der User "Abbrechen" oder "Übernehmen" klickt

---

**Viel Erfolg beim Testen!** 🚀
