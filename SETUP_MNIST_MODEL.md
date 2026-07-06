# 🤖 TensorFlow Lite MNIST Model Setup

Die Digit-Scanner-Funktion benötigt ein trainiertes **MNIST Digit-Klassifizierungsmodell**.

## 🚀 Schnelle Setup (3 Minuten)

### Option 1: Python-Skript verwenden (Empfohlen)

```bash
# 1. Python + TensorFlow installieren
pip install tensorflow numpy

# 2. Model generieren
cd O:\SupermarktRechner
python3 generate_mnist_model.py

# 3. Model wird automatisch in app/src/main/assets/ platziert

# 4. App neu kompilieren
./gradlew assembleDebug
```

### Option 2: Google Colab (Kein Setup nötig)

1. Öffne: https://colab.research.google.com
2. Kopiere diesen Code in eine Zelle:

```python
# TensorFlow Lite MNIST Model Generator
import tensorflow as tf
from tensorflow import keras
import numpy as np

# Load MNIST
(x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()
x_train = x_train.astype('float32') / 255.0
x_test = x_test.astype('float32') / 255.0
x_train = x_train.reshape(-1, 28, 28, 1)
x_test = x_test.reshape(-1, 28, 28, 1)

# Build model
model = keras.Sequential([
    keras.layers.Input(shape=(28, 28, 1)),
    keras.layers.Conv2D(32, (3, 3), activation='relu'),
    keras.layers.MaxPooling2D((2, 2)),
    keras.layers.Conv2D(64, (3, 3), activation='relu'),
    keras.layers.MaxPooling2D((2, 2)),
    keras.layers.Flatten(),
    keras.layers.Dense(128, activation='relu'),
    keras.layers.Dropout(0.5),
    keras.layers.Dense(10, activation='softmax')
])

model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(x_train, y_train, batch_size=128, epochs=5, validation_split=0.1)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Download
from google.colab import files
with open('digit_classifier.tflite', 'wb') as f:
    f.write(tflite_model)
files.download('digit_classifier.tflite')
```

3. Klick auf **Download** wenn fertig
4. Verschiebe `digit_classifier.tflite` zu: `app/src/main/assets/`
5. App neu bauen

### Option 3: Fertig-Modell downloaden

Wenn Python nicht verfügbar ist:

1. Gehe zu: https://storage.googleapis.com/download.tensorflow.org/models/tflite/mnist_2021_05_26.zip
2. Download und entpacke ZIP
3. Kopiere die `.tflite` Datei nach `app/src/main/assets/`
4. Benenne sie zu `digit_classifier.tflite` um

---

## ✅ Verifikation

Nach dem Setup:

```bash
# 1. App bauen
./gradlew assembleDebug

# 2. Logs überprüfen
adb logcat | grep "DigitClassifier"

# Expected output:
# ✅ TFLite model loaded successfully
```

---

## 🎯 Model-Spezifikationen

- **Input**: 28×28 Grayscale Image
- **Output**: 10 Klassen (Ziffern 0-9)
- **Format**: TensorFlow Lite (.tflite)
- **Größe**: ~2-5 MB
- **Genauigkeit**: ~98% (MNIST Test-Set)

---

## 🐛 Fehlerbehebung

### "Model not initialized" in Logs

→ Modell fehlt oder ist nicht im richtigen Format

**Lösung:**
1. Überprüfe: `app/src/main/assets/digit_classifier.tflite` existiert?
2. Modell ist >1MB groß?
3. Ja zu beidem? App neu bauen (Clean Build)

### "Image already closed" Error

→ Das sollte mit TensorFlow Lite nicht mehr vorkommen

**Lösung:** App neu starten und Logs überprüfen

---

## 📊 Training-Details

Das generierte Modell wird trainiert auf:
- **MNIST Datensatz**: 60.000 Trainings-Bilder + 10.000 Test-Bilder
- **Architektur**: 2× Conv2D + MaxPooling + Dense Layers
- **Epochs**: 5 (optimiert für Balance zwischen Genauigkeit und Dateigröße)
- **Batch Size**: 128

---

Fragen? Siehe `DEBUG_GUIDE.md` für weitere Optionen!
