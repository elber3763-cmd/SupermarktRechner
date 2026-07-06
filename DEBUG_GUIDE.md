# 🔧 Debug Guide: Digit Scanner Troubleshooting

## Quick Start Testing

### 1. Build & Install
```bash
cd O:\SupermarktRechner

# Build
./gradlew assembleDebug

# Install on device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.einkaufsscanner/.MainActivity
```

### 2. Real-Time Logging
Öffnen Sie **zwei Terminal-Fenster**:

**Terminal 1: Logcat Monitor**
```bash
adb logcat | grep -E "DigitScanner|CameraManager|PreviewView|Camera|ML Kit"
```

**Terminal 2: App Control**
```bash
adb shell
```

---

## 🎯 Test Scenario

### Schritt 1: App starten
```
Expected: Main Shopping Cart Screen angezeigt
Buttons sichtbar: "Manuell", "Zahlen", "Kamera"
```

### Schritt 2: "Zahlen" Button klicken
```
Expected Logcat Output:
D/DigitScannerScreen: 📱 PreviewView created, starting camera setup
D/CameraManager: 🔍 Starting live digit scanning with ImageAnalysis
D/CameraManager: ✅ Camera provider obtained, binding camera...
D/CameraManager: ✅ Live digit scanning camera bound successfully
D/DigitScannerScreen: ✅ Camera setup completed
```

✅ Wenn diese Logs erscheinen: **Kamera sollte angezeigt werden**

### Schritt 3: Zahlen vor Kamera halten
```
Expected Logcat Output nach ~500ms:
D/DigitScannerScreen: Detected digits: 299
D/CameraManager: 📊 Digits detected: 299
```

✅ Wenn diese Logs erscheinen: **Ziffern sollten im Display angezeigt werden**

### Schritt 4: "Übernehmen" klicken
```
Expected Logcat Output:
D/ShoppingViewModel: Processing detected digits: 299
D/ShoppingViewModel: Formatted price: 2,99 -> 2.99
```

✅ Wenn diese Logs erscheinen: **Dialog sollte sich öffnen mit Preis 2,99€**

---

## ❌ Fehlerfall: Kamera nicht angezeigt

### Fehlerdiagnose Checkliste

#### 1️⃣ PreviewView wird nicht erstellt?
```bash
adb logcat | grep "PreviewView created"
```

**Lösung wenn nicht vorhanden**:
- Überprüfe: Hat Activity `com.einkaufsscanner.MainActivity` LifecycleOwner?
- Überprüfe: Ist Context vom Type Activity/Fragment?

#### 2️⃣ Camera Provider wird nicht initialisiert?
```bash
adb logcat | grep "Camera provider obtained"
```

**Lösung wenn nicht vorhanden**:
- Überprüfe: Ist `ProcessCameraProvider.getInstance()` aufgerufen worden?
- Überprüfe: Gibt es Fehler beim Instance-Get?

```bash
adb logcat | grep "Error"
```

#### 3️⃣ Camera wird nicht gebunden?
```bash
adb logcat | grep "Live digit scanning camera bound"
```

**Lösung wenn nicht vorhanden**:
- Überprüfe: Permission ist gewährt?
  ```bash
  adb shell pm check-permission android.permission.CAMERA com.einkaufsscanner
  ```
  
  Sollte: `PERMISSION_GRANTED` sein
  
  Wenn nicht:
  ```bash
  adb shell pm grant com.einkaufsscanner android.permission.CAMERA
  ```

#### 4️⃣ Exception beim Binden?
```bash
adb logcat | grep "Error binding\|Exception"
```

**Häufige Fehler**:
- `CameraX failed to bind` → Andere App nutzt Kamera
- `Lifecycle not active` → Activity wurde zerstört
- `Surface not ready` → PreviewView nicht initialisiert

---

## 🔍 Detailliertes Debugging

### Full Logcat mit Timestamps
```bash
adb logcat -v threadtime | grep -E "DigitScanner|CameraManager"
```

### Nur Fehler
```bash
adb logcat "*:E" | grep -E "DigitScanner|CameraManager|Camera"
```

### Memory/Performance
```bash
adb shell dumpsys meminfo com.einkaufsscanner | grep -A5 "Native Heap"
```

### Camera State
```bash
adb shell dumpsys camera | grep -E "Camera|State|Provider"
```

---

## 🎬 Step-by-Step Manual Test

### Test 1: Permission Check
```
1. App starten
2. Settings → Apps → Einkaufs-Scanner → Permissions
3. Camera: sollte "Allowed" sein
4. Falls "Denied": Erlauben
5. App neu starten
6. "Zahlen" klicken
```

### Test 2: Single Digit
```
1. "Zahlen" klicken → Kamera angezeigt
2. Papier mit "5" hochhalten
3. Warte 1 Sekunde
4. ✅ Display sollte "5" anzeigen
5. "Übernehmen" klicken
6. ✅ Dialog mit "0,05 EUR" sollte angezeigt werden
```

### Test 3: Multi-Digit Price
```
1. "Zahlen" klicken
2. Papier mit "1299" hochhalten
3. ✅ Display sollte "1299" anzeigen
4. "Übernehmen" klicken
5. ✅ Dialog mit "12,99 EUR" sollte angezeigt werden
```

### Test 4: Invalid Price (too small)
```
1. "Zahlen" klicken
2. Papier mit "1" hochhalten
3. "Übernehmen" klicken
4. ✅ Nichts sollte passieren (0,01€ zu klein)
```

### Test 5: Cancel
```
1. "Zahlen" klicken
2. "Abbrechen" klicken
3. ✅ Zurück zur Hauptoberfläche
```

---

## 📊 Performance Metrics

### Erwartete Werte
- **First Frame Render**: < 500ms
- **Text Recognition**: 100-300ms per frame
- **Update Throttle**: 500ms (verhinderung von UI-Überlastung)
- **Memory Usage**: ~80-120MB (Camera + ML Kit)

### Überprüfung
```bash
adb shell ps -eo PID,VSIZ,RSS,ARGS | grep einkaufsscanner
```

---

## 🐛 Häufige Fehler & Lösungen

| Fehler | Ursache | Lösung |
|--------|--------|--------|
| Schwarzer Bildschirm | PreviewView nicht angezeigt | Überprüfe scaleType, implementationMode |
| Kamera friert | ImageProxy nicht geschlossen | Überprüfe finally-Block in analyzeFrameForDigits |
| Keine Erkennung | ML Kit Models nicht heruntergeladen | Erste Ausführung: warte 30 Sekunden |
| Langsam | TextRecognition.getClient() zu oft aufgerufen | Sollte gecacht werden |
| Abstürz | Memory leak in CameraManager | Überprüfe stopLiveDigitScanning() |
| Kamera geht an, Ziffern nicht erkannt | Schlechte Lichtverhältnisse | Bessere Beleuchtung |

---

## 📱 Android Studio Debugger

### Breakpoints setzen
1. DigitScannerScreen.kt → Zeile 60: `LaunchedEffect(Unit)`
2. CameraManager.kt → Zeile 80: `startLiveDigitScanning()`
3. CameraManager.kt → Zeile 146: `analyzeFrameForDigits()`

### Variablen inspizieren
- `detectedDigits` State
- `lifecycleOwner` Typ
- `cameraProvider` null check

### Profiler
```
Android Profiler → CPU → Recording starten
"Zahlen" klicken → Beobachte CPU Usage
```

---

## ✅ Verification Checklist

- [ ] App startet ohne Crash
- [ ] "Zahlen" Button vorhanden
- [ ] Logcat zeigt "PreviewView created"
- [ ] Kamera-Berechtigung erteilt
- [ ] Logcat zeigt "Camera provider obtained"
- [ ] Kamera-Preview sichtbar (nicht schwarz/blau)
- [ ] Ziffern vor Kamera → erkannt
- [ ] "Übernehmen" aktiviert wenn Ziffern vorhanden
- [ ] Dialog mit Preis angezeigt
- [ ] Artikel zur Liste hinzugefügt

---

## 🎯 Nächste Schritte

Wenn nach dieser Checkliste die Kamera noch nicht funktioniert:

1. Poste die **vollständige Logcat-Ausgabe**:
   ```bash
   adb logcat -d > logcat.txt
   ```

2. Überprüfe **Emulator vs. Device**:
   - Welcher API Level?
   - Welches Gerät/Emulator?

3. Überprüfe **Android Version**:
   ```bash
   adb shell getprop ro.build.version.release
   ```

---

**Viel Erfolg beim Debugging!** 🚀
