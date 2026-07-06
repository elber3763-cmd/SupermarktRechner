# 🧪 Manueller Test: Digit Scanner (Pragmatischer Ansatz)

## 🎯 Setup

```bash
cd O:\SupermarktRechner

# 1. Build
./gradlew assembleDebug

# 2. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Clear Logs
adb logcat -c

# 4. Start App
adb shell am start -n com.einkaufsscanner/.MainActivity
```

## 📋 Erwartete Logs beim Start

```bash
adb logcat | grep -E "CameraManager|DigitScanner|ML Kit"
```

Expected Output:
```
D/DigitClassifier: ⚠️ TFLite model not found (OK - wir nutzen ML Kit)
D/DigitScannerScreen: 📱 PreviewView created, starting camera setup
D/CameraManager: 🔍 Starting live digit scanning with ImageAnalysis
D/CameraManager: ✅ Camera provider obtained, binding camera...
D/CameraManager: ✅ Live digit scanning camera bound successfully
```

## 🎮 Manueller Test (auf dem Gerät)

1. **App öffnet sich** → Sie sehen Shopping Cart Screen
2. **Schauen Sie auf die Buttons unten:**
   - Links: "Manuell"
   - Mitte: **"Zahlen"** (NEW - mit Live-Kamera)
   - Rechts: "Kamera" oder "Preis"

3. **Klicken Sie auf "Zahlen"**
   - Sie sollten Kamera-Preview sehen
   - Schwarzer Bildschirm mit Bereichs-Rahmen in der Mitte

4. **Halten Sie eine große, klare Ziffer vor die Kamera:**
   - **Gedruckte Ziffer:** Am besten für Tests (z.B. aus einer Zeitschrift)
   - **Handschrift:** Groß, klare Linienstärke
   - **Minimum-Größe:** ~3cm × 3cm

5. **Beobachten Sie die Logs:**

```bash
adb logcat -v brief | grep "CameraManager"
```

### ✅ Erwartete Logs während Erkennung:

```
D/CameraManager: 🔍 ML Kit recognized: '5'
D/CameraManager: 🔤 Raw text: '5' → Cleaned: '5'
D/CameraManager: 📊 Digits extracted: 5
D/DigitScanner: ✅ UI updated with digits: 5
```

### Wenn Text erkannt wird:
- **Display oben sollte "5" anzeigen** (im Bereich "Erkannte Zahlen:")
- **"Übernehmen" Button wird aktiv** (nicht grau)

### Wenn mehrere Ziffern:
```
D/CameraManager: 🔍 ML Kit recognized: 'Price: 12,99€'
D/CameraManager: 🔤 Raw text: 'Price: 12,99€' → Cleaned: '1299'
D/CameraManager: 📊 Digits extracted: 1299
```
→ Display zeigt **"1299"**

## 🔍 Debugging: Wenn nichts erkannt wird

### Check 1: Camera Preview zeigt was?

**Erwartung:** Schwarzer Bildschirm oder Live-Kamera-Vorschau

**Wenn:** Schwarzer Bildschirm bleibt
- Prüfe: Kamera-Berechtigung?
- Settings → Apps → Einkaufs-Scanner → Permissions → Camera: **Allow**?

**Logs:**
```bash
adb logcat | grep "PreviewView\|Camera"
```

### Check 2: ML Kit lädt?

**Logs:**
```bash
adb logcat | grep "ML Kit\|recognized"
```

**Wenn nichts:** ML Kit braucht Internet zum Download der Modelle (beim ersten Mal)

**Lösung:**
1. App beenden (lange Home-Button drücken)
2. WiFi verbinden
3. App neu starten
4. "Zahlen" klicken
5. Warten Sie **30 Sekunden** - ML Kit lädt Modelle herunter
6. Erst danach testen

### Check 3: Frame-Analyse läuft?

**Logs detailliert:**
```bash
adb logcat -v threadtime | grep -E "CameraManager|analyzed|frame"
```

**Erwartung:** Sollte mehrere Zeilen pro Sekunde sehen

**Wenn stille:** Analyzer läuft nicht
- Prüfe: Lifecycle korrekt?
- Prüfe: ImageAnalysis in Camera gebunden?

## 📸 Test mit Bildern

### Test 1: Einzelne Ziffer
```
Zeige: 5
Erwartet: "5" angezeigt
Klick "Übernehmen" → Dialog mit "0,05€"
```

### Test 2: Preis
```
Zeige: 1299 (oder 12,99)
Erwartet: "1299" angezeigt
Klick "Übernehmen" → Dialog mit "12,99€"
```

### Test 3: Mit Text
```
Zeige: "Price: 3,99"
Erwartet: "399" extrahiert
Klick "Übernehmen" → Dialog mit "3,99€"
```

### Test 4: Handschrift
```
Zeige: Handgeschriebene "7"
Erwartet: "7" erkannt (wenn groß + klar)
Klick "Übernehmen" → Dialog mit "0,07€"
```

## 🐛 Häufige Fehler

| Symptom | Ursache | Lösung |
|---------|--------|--------|
| Schwarzer Screen | Camera nicht gerendert | Kamera-Permission prüfen |
| Keine Logs | App startet nicht | `adb logcat -d` vollständig prüfen |
| "Zahlen" nicht sichtbar | 3-Button Layout zu eng | Neustart App |
| Text erkannt aber keine Ziffern | "Price: ..." Text zu viel | Näher rangehen, nur Ziffer |
| Zu viele False Positives | Threshold zu niedrig | Klarerezahlen nötig |
| Throttled log | 100ms reicht nicht | Erhöhen auf 200ms |

## 📊 Performance-Metriken

### Erwartete Werte:
- **Frame Analysis**: ~30fps (= alle ~33ms)
- **ML Kit Processing**: 100-300ms pro Frame
- **UI Update Throttle**: 100ms
- **Recognition Delay**: 1-2 Sekunden nach zeigen

### Überprüfung:
```bash
adb logcat | grep "Throttled" | wc -l
```
Sollte **nicht zu häufig** sein. Wenn oft: Threshold erhöhen.

## ✅ Checklist für erfolgreichen Test

- [ ] App startet ohne Crash
- [ ] "Zahlen" Button sichtbar und klickbar
- [ ] DigitScannerScreen öffnet sich
- [ ] Camera Preview zeigt Kamera-Feed (nicht schwarz)
- [ ] ML Kit "recognized" Logs erscheinen
- [ ] Bei Ziffer in Kamera: "Digits extracted" Log
- [ ] Display aktualisiert sich mit Ziffern
- [ ] "Übernehmen" Button wird aktiv
- [ ] Dialog mit Preis öffnet sich
- [ ] Artikel wird zur Liste hinzugefügt

---

## 🎯 Nächste Schritte

1. **Führe diesen Test durch** und berichte:
   - Welche Logs siehst du?
   - Wo stoppen die Logs?
   - Funktioniert Kamera-Preview?

2. **Wenn Logs zeigen "recognized: ...":**
   - Prima! ML Kit funktioniert
   - Dann prüfen: Warum wird Ziffer nicht extrahiert?

3. **Wenn keine "recognized" Logs:**
   - ML Kit lädt vielleicht noch
   - Oder: ImageAnalyzer wird nicht aufgerufen

Viel Erfolg! 🚀
