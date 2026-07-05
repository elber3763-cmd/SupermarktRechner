# ⚡ Quick Start Guide

## 🎯 In 3 Schritten zur fertigen App

### 1️⃣ Android Studio öffnen
```
C:\Program Files\Android\Android Studio\bin\studio.exe
```

### 2️⃣ Projekt öffnen
```
File → Open → O:\SupermarktRechner → OK
Warte auf "Gradle Sync" (auto)
```

### 3️⃣ App bauen
```
Build → Make Project
oder Ctrl+F9
```

---

## ✅ Validierung vor dem Build

```bash
cd O:\SupermarktRechner

# Core-Logik testen
python validate_logic.py

# Ergebnis: ✅ ALL VALIDATION TESTS PASSED!
```

---

## 📱 Auf Gerät installieren

### Mit Android Studio
```
Run → Run 'app'
oder Shift+F10
```

### Via CLI (nach erfolgreichem Build)
```bash
./gradlew installDebug
```

---

## 🧪 Unit Tests ausführen

```bash
./gradlew test

# Ergebnis:
# ✅ PriceExtractorTest (10 tests)
# ✅ ShoppingCartTest (10 tests)
```

---

## 📦 Build Artefakte

Nach erfolgreichem Build findest du:

```
app/build/outputs/apk/debug/app-debug.apk
├── Size: ~50-80 MB
├── Signature: Debug certificate
└── Ready to install on device
```

---

## 🐛 Debugging

### Logs anschauen
```
View → Tool Windows → Logcat (oder Alt+6)
```

### Breakpoints setzen
```
Klick auf Zeilennummer im Editor
Debug Mode: Shift+F9
```

### Device Status
```
View → Tool Windows → Device Manager
```

---

## 📚 Dokumentation

| Datei | Inhalt |
|-------|--------|
| **README.md** | Architektur-Übersicht |
| **BUILD_INSTRUCTIONS.md** | Ausführliche Build-Anleitung |
| **DEVELOPMENT.md** | Debugging & Testing Guide |
| **API_REFERENCE.md** | Vollständige API-Doku |
| **MIGRATION.md** | Python→Kotlin Details |

---

## 🆘 Häufige Probleme

### ❌ "Gradle sync failed"
→ **Lösung:** File → Sync Now

### ❌ "SDK nicht gefunden"
→ **Lösung:** Tools → SDK Manager → SDK 28 & 34 installieren

### ❌ "Java version not supported"
→ **Lösung:** File → Project Structure → JDK 17+ auswählen

### ❌ "Gradle wrapper nicht gefunden"
→ **Lösung:** Ist enthalten (gradlew.bat + gradle/wrapper/)

---

## 💡 Tipps

✅ **Erste Build dauert länger** (Dependencies werden heruntergeladen)  
✅ **Nutze einen Emulator oder echtes Gerät**  
✅ **Kamera-Permission manuell in Emulator erlauben**  
✅ **Internet-Verbindung für Gradle nötig**  

---

## 🚀 Du bist fertig wenn...

- [ ] Android Studio ist geöffnet
- [ ] Projekt ist geladen
- [ ] Gradle Sync erfolgreich
- [ ] Build erfolgreich (grünes Häkchen)
- [ ] APK unter `app/build/outputs/apk/debug/`
- [ ] App läuft auf Emulator/Gerät

---

## 📊 Was wurde erstellt

| Kategorie | Anzahl |
|-----------|---------|
| Kotlin Source Files | 16 |
| Test Files | 2 |
| Gradle Config | 3 |
| Resources | 3 |
| Documentation | 6 |
| **TOTAL** | **30 Files** |

---

## 🎯 Projekt-Facts

- **Architektur:** MVVM
- **UI Framework:** Jetpack Compose
- **Async:** Kotlin Coroutines
- **DI:** Hilt
- **OCR:** ML Kit (Google)
- **Kamera:** CameraX
- **Min SDK:** 28 (Android 9)
- **Target SDK:** 34 (Android 14)

---

**Status:** ✅ Ready to Build  
**Next:** Open Android Studio & Build!
