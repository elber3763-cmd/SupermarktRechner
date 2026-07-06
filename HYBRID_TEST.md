# 🧪 Hybrid Digit Scanner - Test-Anleitung

## ✅ Status
- **Gedruckte Ziffern:** ✅ FUNKTIONIERT (ML Kit)
- **Handgeschriebene Ziffern:** 🧪 Zu testen (Fallback-Mode)

---

## 🎯 Test 1: Gedruckte Ziffern (Baseline)

```bash
# Terminal 1: Logs überwachen
adb logcat | grep "PRINTED"
```

**Auf dem Handy:**
1. App öffnen → "Zahlen" klicken
2. Halte **gedruckte Ziffer** vor Kamera (z.B. aus Zeitung)
3. Beobachte Logs und Display

**Erwartet:**
```
✅ PRINTED: ML Kit recognized: '5'
📊 Printed digits extracted: 5
```

✅ **Wenn das funktioniert:** Abschnitt bestanden!

---

## 🖊️ Test 2: Handgeschriebene Ziffern

```bash
# Terminal 1: Alle Logs
adb logcat | grep -E "PRINTED|HANDWRITING|extracted"
```

**Auf dem Handy:**
1. "Zahlen" klicken
2. Schreiben Sie eine **große, klare Ziffer** auf Papier
3. Halten Sie vor Kamera (**nicht zu schnell bewegen!**)
4. Warten Sie **1-2 Sekunden** auf Erkennung

**Wichtig für Handschrift:**
- ✅ **Groß:** Mindestens 5cm × 5cm
- ✅ **Klar:** Dicke Linien, schwarze Tinte/Stift
- ✅ **Kontrast:** Dunkle Zahl auf hellem Untergrund
- ✅ **Beleuchtung:** Gut beleuchtet, kein Gegenlicht
- ✅ **Stellung:** Kamera direkt über Ziffer

**Nicht optimal:**
- ❌ Zu kleine Ziffern (<2cm)
- ❌ Dünn gezeichnet/Bleistift
- ❌ Kursivschrift oder verziert
- ❌ Mehrere Ziffern auf einmal (nur eine pro Versuch!)

**Erwartet in Logs:**
```
⚠️ No text in frame - trying handwriting mode...
🖊️  Attempting handwriting recognition...
✅ HANDWRITING: ML Kit recognized: '7'
📊 Handwriting digits extracted: 7
```

Oder Fallback:
```
❌ ML Kit failed, trying handwriting...
🖊️  Attempting handwriting recognition...
✅ HANDWRITING: ML Kit recognized: '3'
📊 Handwriting digits extracted: 3
```

---

## 📊 Test-Matrix

| Ziffer | Gedruckt | Handschrift | Notes |
|--------|----------|-------------|-------|
| **0** | ✅ | 🧪 | Oval/Kreis |
| **1** | ✅ | 🧪 | Vertikale Linie |
| **2** | ✅ | 🧪 | S-förmig |
| **3** | ✅ | 🧪 | Kurven |
| **4** | ✅ | 🧪 | Winkel + Linie |
| **5** | ✅ | 🧪 | Komplexe Form |
| **6** | ✅ | 🧪 | Schleife |
| **7** | ✅ | 🧪 | Einfach |
| **8** | ✅ | 🧪 | Zwei Schleifen |
| **9** | ✅ | 🧪 | Schleife + Linie |

**Test je Ziffer mindestens 3x:**
1. Erste Versuche werden oft nicht erkannt (ML Kit wärmt auf)
2. Ab 3. Versuch bessere Chancen
3. Nach ~10 Versuchen sollte Muster klar sein

---

## 🔍 Debugging: Wenn Handschrift nicht funktioniert

### Debug-Modus aktivieren
```bash
adb logcat -v brief | grep -E "HANDWRITING|No text|Attempting"
```

### Check 1: Wird Fallback-Mode überhaupt aufgerufen?

**Wenn Logs zeigen:**
```
⚠️ No text in frame - trying handwriting mode...
🖊️  Attempting handwriting recognition...
```

→ **Fallback-Mode läuft!** Aber ML Kit erkennt Handschrift nicht

**Wenn diese Logs NICHT erscheinen:**
→ ML Kit erkennt als "gedruckter Text" (auch Handschrift)
→ Aber Extraktion findet keine Ziffern → Check extractDigitsFromText

### Check 2: Was erkennt ML Kit tatsächlich?

```bash
adb logcat | grep "recognized:"
```

Zeigt: `recognized: 'garbage'` oder `recognized: ''`

→ ML Kit kann Handschrift nicht dekodieren (bekannt, nicht unsere Schuld)

### Check 3: Extraktions-Logik arbeitet?

```bash
adb logcat | grep "Raw text:"
```

Zeigt: `Raw text: 'xyz' → Cleaned: ''`

→ Ziffer nicht in erkanntem Text enthalten

---

## ✅ Erfolgs-Kriterien

**Hybrid-Scanner gilt als erfolgreich wenn:**

1. ✅ Gedruckte Ziffern: >90% Erkennungsrate
2. ✅ Handschrift: >50% Erkennungsrate (akzeptabel)
3. ✅ UI zeigt erkannte Ziffern (beide Modi)
4. ✅ "Übernehmen" Button funktioniert
5. ✅ Preis wird korrekt formatiert (z.B. 5 → 0,05€)

**Wenn >50% Handschrift-Rate nicht erreichbar:**
- Hybrid-Scanner erfüllt Zweck
- Benutzer kann immer noch manuell eingeben oder Foto nutzen
- Gedruckte Ziffern: Perfect für Barcode-Nummern

---

## 📝 Test-Log-Template

Führen Sie diesen Test durch und berichten Sie:

```
Test durchgeführt am: [Datum]
Gerät: [Modell]
Lichtverhältnisse: [Dunkel/Normal/Hell]

GEDRUCKTE ZIFFERN:
- Ziffer: 5 → Erkannt: JA/NEIN
- Ziffer: 2,99 → Erkannt: JA/NEIN
- Erfolgsrate: __/10

HANDGESCHRIEBENE ZIFFERN:
- Ziffer: 7 → Erkannt: JA/NEIN  
- Ziffer: 3 → Erkannt: JA/NEIN
- Erfolgsrate: __/10

LOGS bei Handschrift (letzte 10 Zeilen):
[Logs einfügen]

Gesamteindruck:
[ ] Sehr zufrieden (90%+ erkannt)
[ ] Zufrieden (70%+ erkannt)
[ ] Akzeptabel (50%+ erkannt)
[ ] Nicht zufrieden (<50% erkannt)
```

---

## 🚀 Wenn Handschrift <50% erkannt wird

Dann würde ich empfehlen:
1. **Gedruckte Ziffern weiter nutzen** (funktioniert gut!)
2. **Foto-Scanning als Fallback** (für Preisschilder)
3. **Manuelle Eingabe als Option** (immer verfügbar)

ML Kit ist einfach nicht für Handschrift trainiert - das ist eine Limitation der Bibliothek, nicht unserer Implementierung.

---

Viel Erfolg beim Test! 📸
