# Build Instructions - Einkaufs-Scanner Android App

## Schnellstart mit Android Studio

### 1. Projekt öffnen
```
1. Android Studio öffnen
2. File → Open
3. O:\SupermarktRechner wählen
4. OK klicken
5. Gradle Sync abwarten (automatisch)
```

### 2. APK bauen
```
Build → Make Project
oder Ctrl+F9
```

### 3. Auf Gerät/Emulator installieren
```
Run → Run 'app'
oder Shift+F10
```

---

## CLI Build (Falls Gradle korrekt konfiguriert ist)

```bash
cd O:\SupermarktRechner

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Auf Emulator/Gerät installieren
./gradlew installDebug

# Tests ausführen
./gradlew test
```

---

## Build-Artefakte

Nach erfolgreichem Build:

### Debug APK
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK
```
app/build/outputs/apk/release/app-release.apk
```

### Test Reports
```
app/build/reports/tests/testDebugUnitTest/index.html
app/build/reports/tests/testReleaseUnitTest/index.html
```

---

## Systemanforderungen

- ✅ Android Studio 2023.1+ (vorhanden: C:\Program Files\Android\Android Studio)
- ✅ JDK 17+ (benötigt)
- ✅ Android SDK (benötigt):
  - Min SDK: 28 (Android 9)
  - Target SDK: 34 (Android 14)
- ✅ Emulator oder echtes Android-Gerät

---

## Troubleshooting

### "Gradle sync failed"
→ File → Sync Now
→ oder File → Invalidate Caches → Restart

### "SDK nicht gefunden"
→ Tools → SDK Manager
→ Min SDK 28 + Target SDK 34 installieren

### "Java version not supported"
→ File → Project Structure → JDK Location
→ JDK 17+ auswählen

### "Gradle 8.4 not found"
→ Gradle wird beim ersten Build heruntergeladen
→ Internetverbindung prüfen

---

## Projektstruktur validiert ✅

```
O:\SupermarktRechner/
├── ✅ build.gradle.kts (Dependencies konfiguriert)
├── ✅ settings.gradle.kts (Projektstruktur)
├── ✅ gradlew.bat (Gradle Wrapper)
├── ✅ gradle/wrapper/gradle-wrapper.properties
├── ✅ app/
│   ├── ✅ build.gradle.kts
│   ├── ✅ proguard-rules.pro
│   ├── ✅ src/main/
│   │   ├── ✅ kotlin/ (vollständiger Quellcode)
│   │   ├── ✅ res/ (Ressourcen)
│   │   └── ✅ AndroidManifest.xml
│   └── ✅ src/test/ (Unit Tests)
├── ✅ README.md
├── ✅ DEVELOPMENT.md
├── ✅ MIGRATION.md
└── ✅ API_REFERENCE.md
```

---

## Build Sequence

1. **Gradle Sync**
   - Lädt alle Dependencies herunter (ML Kit, CameraX, Compose, etc.)
   - Generiert R.java und BuildConfig

2. **Kotlin Compilation**
   - Kompiliert alle .kt Dateien
   - Type Checking

3. **Resource Processing**
   - Packt Strings, Themes, Icons
   - Generiert Android Resources

4. **APK Assembly**
   - Kombiniert Dex + Resources + Manifest
   - Signiert (Debug-Zertifikat)

5. **Output**
   - app/build/outputs/apk/debug/app-debug.apk

---

## Größe & Performance

### APK Größe (Debug)
- Expected: ~50-80 MB (mit ML Kit, CameraX, OpenCV)
- Reduzierbar mit ProGuard in Release-Build

### Build Zeit
- Clean Build: 2-3 Minuten (erste Konfiguration)
- Incremental Build: 30-60 Sekunden

### Min SDK
- SDK 28 = Android 9.0 (2018)
- ~99% der aktiven Android-Geräte unterstützt

---

## Nächste Schritte nach erfolgreichem Build

1. ✅ APK auf Emulator/Gerät testen
2. ✅ Unit Tests ausführen: `./gradlew test`
3. ✅ UI testen: Kamera, OCR, Preiseingabe
4. ✅ Permissions checken (Kamera, Storage)
5. ✅ Release APK bauen für App Store

---

## Support

- Probleme? → Siehe DEVELOPMENT.md
- API-Fragen? → Siehe API_REFERENCE.md
- Konvertierungs-Details? → Siehe MIGRATION.md
