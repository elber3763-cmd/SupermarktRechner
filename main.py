"""
============================================================
 Einkaufs-Scanner  -  Vollstaendige main.py (Desktop + Android)
============================================================
 Technik:  Kivy 2.3.x + KivyMD 1.2.0
           Desktop : OpenCV (cv2.VideoCapture) + pytesseract
           Android : camera4kivy (Kamera) + ML Kit (OCR, Schritt 5)

 Eigenschaften:
   - Plattform-abhaengige Kamera (Desktop / Android) mit gleicher
     Schnittstelle: get_roi(), start(), stop().
   - Plattform-abhaengige OCR (run_ocr): Desktop = pytesseract,
     Android = ML-Kit-Platzhalter (faellt auf manuelle Eingabe zurueck).
   - Bildverarbeitung mit NumPy-Fallback, falls OpenCV fehlt.
   - Robuste RegEx-Preisfilterung inkl. Warnung bei mehreren Preisen.
   - Manuelle Schnell-Eingabe als Fallback.

 Stilregeln dieser Datei:
   - Alle KV-Attribute haben gueltige Werte (Farben/Radien als Listen).
   - Jede Python-Anweisung steht in einer eigenen Zeile.
   - Dialog-Buttons werden erzeugt und per .bind(on_release=...) verbunden
     (keine Lambdas im Konstruktor).
============================================================
"""

import os
import re
from collections import namedtuple
from functools import partial

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty
from kivy.clock import Clock
from kivy.graphics.texture import Texture
from kivy.uix.image import Image
from kivy.utils import platform

from kivymd.app import MDApp
from kivymd.uix.card import MDCard
from kivymd.uix.dialog import MDDialog
from kivymd.uix.button import MDFlatButton, MDRaisedButton
from kivymd.uix.textfield import MDTextField


# --- NumPy (auf beiden Plattformen verfuegbar) ---
try:
    import numpy as np
    HAS_NUMPY = True
except Exception as e:
    HAS_NUMPY = False
    print("NumPy nicht verfuegbar:", e)

# --- OpenCV (Desktop sicher; Android nur mit p4a-Recipe) ---
try:
    import cv2
    HAS_CV2 = True
except Exception as e:
    HAS_CV2 = False
    print("OpenCV nicht verfuegbar (Desktop: pip install opencv-python):", e)

# --- pytesseract (nur Desktop sinnvoll) ---
try:
    import pytesseract
    HAS_TESSERACT = True
    if os.name == "nt":
        _win_path = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
        if os.path.exists(_win_path):
            pytesseract.pytesseract.tesseract_cmd = _win_path
except Exception as e:
    HAS_TESSERACT = False
    print("pytesseract nicht verfuegbar (Desktop: pip install pytesseract):", e)

# --- camera4kivy (nur auf Android importieren) ---
Preview = None
if platform == "android":
    try:
        from camera4kivy import Preview
    except Exception as e:
        print("camera4kivy nicht verfuegbar:", e)


# ----------------------------------------------------------------------
#  Konstanten
# ----------------------------------------------------------------------
ROI_X = (0.10, 0.90)
ROI_Y = (0.37, 0.63)

OCR_CONFIG = r"--oem 3 --psm 7 -c tessedit_char_whitelist=0123456789.,"


# ======================================================================
#  BILDVERARBEITUNG (OpenCV mit NumPy-Fallback)
# ======================================================================
def preprocess_for_ocr(bgr):
    if HAS_CV2:
        gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
        gray = cv2.resize(gray, None, fx=2.0, fy=2.0, interpolation=cv2.INTER_CUBIC)
        gray = cv2.bilateralFilter(gray, 11, 17, 17)
        _, th = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        return th
    if HAS_NUMPY and getattr(bgr, "ndim", 0) == 3:
        gray = (0.114 * bgr[..., 0] + 0.587 * bgr[..., 1] + 0.299 * bgr[..., 2])
        gray = gray.astype("uint8")
        threshold_value = gray.mean()
        binary = (gray > threshold_value) * 255
        return binary.astype("uint8")
    return bgr


# ======================================================================
#  OCR (plattform-abhaengig)
# ======================================================================
def run_ocr_desktop(image):
    if not HAS_TESSERACT:
        return ""
    try:
        return pytesseract.image_to_string(image, config=OCR_CONFIG)
    except Exception as e:
        print("OCR-Fehler:", e)
        return ""


_android_ocr_warned = False


def run_ocr_android(image):
    global _android_ocr_warned
    if not _android_ocr_warned:
        print("Android-OCR (ML Kit) noch nicht eingebunden -> manueller Fallback.")
        _android_ocr_warned = True
    return ""


def run_ocr(image):
    if platform == "android":
        return run_ocr_android(image)
    return run_ocr_desktop(image)


# ======================================================================
#  ROBUSTE PREIS-FILTERUNG (RegEx)
# ======================================================================
PriceResult = namedtuple("PriceResult", ["price", "candidates", "ambiguous"])

PRICE_RE = re.compile(r"(?<!\d)(\d{1,4})[ \t]*[.,][ \t]*(\d{2})(?!\d)")
UNIT_TAIL_RE = re.compile(r"^\s*/")


def find_price_candidates(text):
    candidates = []
    seen_cents = set()
    for match in PRICE_RE.finditer(text):
        euros = match.group(1)
        cents = match.group(2)
        value = round(int(euros) + int(cents) / 100.0, 2)
        if value <= 0 or value > 9999:
            continue
        key = int(round(value * 100))
        tail = text[match.end():match.end() + 8]
        is_unit = bool(UNIT_TAIL_RE.match(tail))
        if key not in seen_cents:
            seen_cents.add(key)
            candidates.append((value, is_unit))
    return candidates


def extract_price(text):
    if not text:
        return PriceResult(None, [], False)
    candidates = find_price_candidates(text)
    if not candidates:
        return PriceResult(None, [], False)
    real_prices = [value for value, is_unit in candidates if not is_unit]
    if real_prices:
        values = real_prices
    else:
        values = [value for value, is_unit in candidates]
    distinct = []
    for value in values:
        if value not in distinct:
            distinct.append(value)
    ambiguous = len(distinct) > 1
    return PriceResult(distinct[0], distinct, ambiguous)


# ======================================================================
#  KAMERA (Desktop) - cv2.VideoCapture
# ======================================================================
class KivyCamera(Image):
    def __init__(self, capture_index=0, fps=30, **kwargs):
        super().__init__(**kwargs)
        self.allow_stretch = True
        self.keep_ratio = False
        self.latest_frame = None
        self.capture = cv2.VideoCapture(capture_index)
        if self.capture.isOpened():
            Clock.schedule_interval(self.update, 1.0 / fps)
        else:
            print("Webcam konnte nicht geoeffnet werden (Index", capture_index, ")")

    def update(self, dt):
        ret, frame = self.capture.read()
        if not ret:
            return
        self.latest_frame = frame.copy()
        flipped = cv2.flip(frame, 0)
        buf = flipped.tobytes()
        height, width = frame.shape[:2]
        if self.texture is None or self.texture.size != (width, height):
            self.texture = Texture.create(size=(width, height), colorfmt="bgr")
        self.texture.blit_buffer(buf, colorfmt="bgr", bufferfmt="ubyte")
        self.canvas.ask_update()

    def get_roi(self):
        if self.latest_frame is None:
            return None
        height, width = self.latest_frame.shape[:2]
        x1 = int(ROI_X[0] * width)
        x2 = int(ROI_X[1] * width)
        y1 = int(ROI_Y[0] * height)
        y2 = int(ROI_Y[1] * height)
        return self.latest_frame[y1:y2, x1:x2].copy()

    def start(self):
        return

    def stop(self):
        Clock.unschedule(self.update)
        if self.capture is not None:
            self.capture.release()


# ======================================================================
#  KAMERA (Android) - camera4kivy.Preview mit Pixel-Analyse
# ======================================================================
if Preview is not None:

    class AndroidCamera(Preview):
        def __init__(self, **kwargs):
            super().__init__(**kwargs)
            self.latest_frame = None
            self._first_frame = True
            self._connected = False

        def analyze_pixels_callback(self, pixels, image_size, image_pos, scale, mirror):
            if not HAS_NUMPY:
                return
            try:
                width, height = image_size
                arr = np.frombuffer(pixels, np.uint8).reshape(height, width, 4)
                if HAS_CV2:
                    self.latest_frame = cv2.cvtColor(arr, cv2.COLOR_RGBA2BGR)
                else:
                    self.latest_frame = arr[:, :, [2, 1, 0]].copy()
                if self._first_frame:
                    self._first_frame = False
                    app = MDApp.get_running_app()
                    if app is not None:
                        app.report_status("")
            except Exception as e:
                print("analyze_pixels_callback-Fehler:", e)
                app = MDApp.get_running_app()
                if app is not None:
                    app.report_status("analyze-Fehler: " + str(e))

        def get_roi(self):
            if self.latest_frame is None:
                return None
            height, width = self.latest_frame.shape[:2]
            x1 = int(ROI_X[0] * width)
            x2 = int(ROI_X[1] * width)
            y1 = int(ROI_Y[0] * height)
            y2 = int(ROI_Y[1] * height)
            return self.latest_frame[y1:y2, x1:x2].copy()

        def start(self):
            if self._connected:
                return
            app = MDApp.get_running_app()
            try:
                self.connect_camera(filepath_callback=self._on_capture_path)
                self._connected = True
                if app is not None:
                    app.report_status("")
            except Exception as e:
                print("connect_camera-Fehler:", e)
                if app is not None:
                    app.report_status("connect_camera-FEHLER:\n" + str(e))

        def stop(self):
            if not self._connected:
                return
            self._connected = False
            self._first_frame = True
            self.latest_frame = None
            try:
                self.disconnect_camera()
            except Exception as e:
                print("disconnect_camera-Fehler:", e)

        def take_photo(self):
            app = MDApp.get_running_app()
            try:
                self.capture_photo(location="private")
            except Exception as e:
                print("capture_photo-Fehler:", e)
                if app is not None:
                    app._show_debug("Foto-Aufnahme-Fehler:\n" + str(e))

        def capture_photo_callback(self, path):
            # Fallback, falls diese Version den Rueckruf so liefert
            app = MDApp.get_running_app()
            if app is not None:
                app.on_photo_captured(path)

        def _on_capture_path(self, path):
            app = MDApp.get_running_app()
            if app is not None:
                app.on_photo_captured(path)


# ======================================================================
#  KV-Layout (alle Attribute mit gueltigen Werten)
# ======================================================================
KV = '''
#:import dp kivy.metrics.dp

<CartRow>:
    orientation: 'horizontal'
    size_hint_y: None
    height: dp(60)
    padding: dp(12), dp(6)
    spacing: dp(8)
    radius: [10, 10, 10, 10]
    elevation: 1
    md_bg_color: app.theme_cls.bg_light

    MDLabel:
        text: root.item_name
        font_style: "Subtitle1"
        valign: "center"

    MDLabel:
        text: root.price_text
        halign: "right"
        valign: "center"
        size_hint_x: 0.45
        font_style: "Subtitle1"
        bold: True

    MDIconButton:
        icon: "trash-can-outline"
        theme_text_color: "Custom"
        text_color: [1, 0.3, 0.3, 1]
        pos_hint: {"center_y": 0.5}
        on_release: app.remove_item(root.index)


MDScreen:

    MDBoxLayout:
        orientation: "vertical"

        MDTopAppBar:
            title: "Einkaufs-Scanner v19"
            elevation: 2

        FloatLayout:
            id: camera_holder
            size_hint_y: 0.45

            canvas.after:
                Color:
                    rgba: [0.1, 0.9, 0.3, 0.9]
                Line:
                    width: dp(2)
                    rectangle: (self.center_x - self.width * 0.40, self.center_y - self.height * 0.13, self.width * 0.80, self.height * 0.26)

            Image:
                id: photo_view
                allow_stretch: True
                keep_ratio: True
                size_hint: 1, 1
                pos_hint: {"x": 0, "y": 0}

            MDLabel:
                id: camera_placeholder
                text: "Kamera bereit\\nTippe auf 'Preis scannen' fuer ein Foto"
                halign: "center"
                valign: "center"
                bold: True
                font_size: "17sp"
                theme_text_color: "Custom"
                text_color: [0, 0, 0, 1]

        ScrollView:
            MDList:
                id: cart_list
                padding: dp(8), dp(8)
                spacing: dp(8)

        MDCard:
            orientation: "vertical"
            size_hint_y: None
            height: dp(140)
            padding: dp(16)
            spacing: dp(8)
            elevation: 4
            md_bg_color: app.theme_cls.primary_color
            radius: [20, 20, 0, 0]

            MDLabel:
                text: app.total_text
                halign: "center"
                font_style: "H4"
                bold: True
                theme_text_color: "Custom"
                text_color: [1, 1, 1, 1]

            MDBoxLayout:
                spacing: dp(12)
                adaptive_height: True

                MDRaisedButton:
                    text: "Manuell"
                    icon: "keyboard"
                    md_bg_color: [1, 1, 1, 1]
                    theme_text_color: "Custom"
                    text_color: app.theme_cls.primary_color
                    on_release: app.open_manual_entry()

                MDRaisedButton:
                    text: "Preis scannen"
                    icon: "camera"
                    md_bg_color: [1, 1, 1, 1]
                    theme_text_color: "Custom"
                    text_color: app.theme_cls.primary_color
                    on_release: app.scan_price()
'''


# ======================================================================
#  Widget-Klassen
# ======================================================================
class CartRow(MDCard):
    item_name = StringProperty("")
    price_text = StringProperty("")
    index = NumericProperty(0)


# ======================================================================
#  Haupt-App
# ======================================================================
class ShoppingScannerApp(MDApp):

    total_text = StringProperty("0,00 EUR")

    def build(self):
        self.theme_cls.theme_style = "Light"
        self.theme_cls.primary_palette = "Teal"
        self.cart_items = []
        self._dialog = None
        self._choice_dialog = None
        self.price_field = None
        self.camera = None
        self._capture_uri = None
        self._awaiting_capture = False
        self._capture_handled = False
        root = Builder.load_string(KV)
        Clock.schedule_once(self._setup_camera, 0)
        return root

    # ------------------------------------------------------------------
    #  Kamera-Auswahl je nach Plattform
    # ------------------------------------------------------------------
    def _setup_camera(self, dt):
        holder = self.root.ids.camera_holder
        if platform == "android":
            self._setup_android_camera(holder)
        else:
            self._setup_desktop_camera(holder)

    def _setup_desktop_camera(self, holder):
        if not HAS_CV2:
            self.root.ids.camera_placeholder.text = "OpenCV fehlt\n(pip install opencv-python)"
            return
        try:
            cam = KivyCamera(capture_index=0, fps=30)
            cam.size_hint = (1, 1)
            cam.pos_hint = {"x": 0, "y": 0}
            holder.add_widget(cam, index=len(holder.children))
            self.camera = cam
            if cam.capture.isOpened():
                self.root.ids.camera_placeholder.text = ""
            else:
                self.root.ids.camera_placeholder.text = "Keine Webcam gefunden\n(Index 0)"
        except Exception as e:
            self.root.ids.camera_placeholder.text = "Kamera-Fehler"
            print("Kamera-Fehler:", e)

    def _setup_android_camera(self, holder):
        # camera4kivy-Kamera verbinden; das Foto kommt spaeter ueber
        # capture_photo(location="private") - direkt in app-eigenen Speicher.
        if Preview is None:
            self.root.ids.camera_placeholder.text = "camera4kivy fehlt im Build"
            return
        try:
            cam = AndroidCamera()
            cam.size_hint = (1, 1)
            cam.pos_hint = {"x": 0, "y": 0}
            holder.add_widget(cam, index=len(holder.children))
            self.camera = cam
            self.root.ids.camera_placeholder.text = (
                "Kamera auf das Preisschild halten,\ndann 'Preis scannen'.")
            from android.permissions import request_permissions, Permission

            def after_perm(perms, grants):
                if grants and all(grants):
                    Clock.schedule_once(self._connect_android_camera, 1.0)
                else:
                    self.report_status("Kamera-Berechtigung wurde abgelehnt.")

            request_permissions([Permission.CAMERA], after_perm)
        except Exception as e:
            self._show_debug("Kamera-Setup-Fehler:\n" + str(e))

    def _connect_android_camera(self, dt):
        if self.camera is not None:
            self.camera.start()

    def report_status(self, message):
        def _apply(dt):
            if self.root is not None:
                self.root.ids.camera_placeholder.text = message
        Clock.schedule_once(_apply, 0)

    def _open_native_camera(self):
        try:
            from jnius import autoclass, cast
            Intent = autoclass("android.content.Intent")
            MediaStore = autoclass("android.provider.MediaStore")
            ContentValues = autoclass("android.content.ContentValues")
            Images = autoclass("android.provider.MediaStore$Images$Media")
            PythonActivity = autoclass("org.kivy.android.PythonActivity")
            activity_ = PythonActivity.mActivity
            resolver = activity_.getContentResolver()

            values = ContentValues()
            values.put("_display_name", "einkaufsscanner_temp.jpg")
            values.put("mime_type", "image/jpeg")
            VERSION = autoclass("android.os.Build$VERSION")
            if VERSION.SDK_INT >= 29:
                values.put("relative_path", "Pictures")
            uri = resolver.insert(Images.EXTERNAL_CONTENT_URI, values)
            if uri is None:
                self._show_debug("Konnte Foto-Ort nicht anlegen.")
                return
            self._capture_uri = uri

            intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cast("android.os.Parcelable", uri))
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            self._capture_handled = False
            self._awaiting_capture = True
            self.report_status("Kamera-App wird geoeffnet ...")
            activity_.startActivityForResult(intent, 0x4321)
        except Exception as e:
            self._show_debug("Kamera-App-Fehler:\n" + str(e))

    def _cleanup_capture_uri(self, uri):
        try:
            if uri is not None:
                from jnius import autoclass
                PythonActivity = autoclass("org.kivy.android.PythonActivity")
                resolver = PythonActivity.mActivity.getContentResolver()
                resolver.delete(uri, None, None)
        except Exception as e:
            print("cleanup-Fehler:", e)
        self._capture_uri = None

    def _show_debug(self, msg):
        def _open(dt):
            from kivymd.uix.dialog import MDDialog
            from kivymd.uix.button import MDFlatButton
            old = getattr(self, "_debug_dialog", None)
            if old is not None:
                try:
                    old.dismiss()
                except Exception:
                    pass
            btn = MDFlatButton(text="OK")
            self._debug_dialog = MDDialog(title="Status", text=str(msg), buttons=[btn])
            btn.bind(on_release=lambda *a: self._debug_dialog.dismiss())
            self._debug_dialog.open()
        Clock.schedule_once(_open, 0)

    # Weg 1: das (oft fehlende) Kamera-Signal
    def _on_activity_result(self, request_code, result_code, intent):
        if request_code != 0x4321:
            return
        Clock.schedule_once(lambda dt: self._process_captured_photo(), 0.3)

    # Weg 2: beim Zurueckkehren in den Vordergrund (zuverlaessiger)
    def on_pause(self):
        return True

    def on_resume(self):
        if getattr(self, "_awaiting_capture", False):
            Clock.schedule_once(lambda dt: self._process_captured_photo(), 1.0)
        return True

    # Gemeinsame Verarbeitung; nur EINMAL pro Aufnahme
    def _process_captured_photo(self):
        if getattr(self, "_capture_handled", False):
            return
        self._capture_handled = True
        self._awaiting_capture = False
        uri = getattr(self, "_capture_uri", None)
        try:
            if uri is None:
                self._show_debug("Kein Foto-Ort vorhanden.")
                return
            from jnius import autoclass
            PythonActivity = autoclass("org.kivy.android.PythonActivity")
            resolver = PythonActivity.mActivity.getContentResolver()
            istream = resolver.openInputStream(uri)
            BitmapFactory = autoclass("android.graphics.BitmapFactory")
            bmp = BitmapFactory.decodeStream(istream)
            istream.close()
            if bmp is None:
                self._show_debug("Kein Foto gespeichert\n(abgebrochen, oder die Kamera-App\nspeichert nicht an unseren Ort).")
                self._cleanup_capture_uri(uri)
                return
            path = PythonActivity.mActivity.getFilesDir().getAbsolutePath() + "/capture.jpg"
            FileOutputStream = autoclass("java.io.FileOutputStream")
            CompressFormat = autoclass("android.graphics.Bitmap$CompressFormat")
            fos = FileOutputStream(path)
            bmp.compress(CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            self._cleanup_capture_uri(uri)
            Clock.schedule_once(lambda dt: self._display_photo(path), 0)
            self.report_status("Lese Zahl aus dem Foto ...")
            self._run_mlkit_ocr(bmp)
        except Exception as e:
            self._show_debug("Fehler bei der Verarbeitung:\n" + str(e))

    # ------------------------------------------------------------------
    #  Automatische Texterkennung (ML Kit, offline) auf dem aufgenommenen Foto
    # ------------------------------------------------------------------
    def _run_mlkit_ocr(self, bmp):
        try:
            from jnius import autoclass
            InputImage = autoclass("com.google.mlkit.vision.common.InputImage")
            TextRecognition = autoclass("com.google.mlkit.vision.text.TextRecognition")
            Options = autoclass("com.google.mlkit.vision.text.latin.TextRecognizerOptions")
            recognizer = TextRecognition.getClient(Options.DEFAULT_OPTIONS)
            image = InputImage.fromBitmap(bmp, 0)
            self._ocr_recognizer = recognizer  # am Leben halten
            self._ocr_task = recognizer.process(image)
            self._ocr_polls = 0
            Clock.schedule_interval(self._poll_ocr, 0.25)
        except Exception as e:
            self._show_debug("OCR-Start-Fehler:\n" + str(e))

    def _poll_ocr(self, dt):
        task = getattr(self, "_ocr_task", None)
        if task is None:
            return False
        self._ocr_polls += 1
        try:
            if not task.isComplete():
                if self._ocr_polls > 40:  # ~10 s Zeitlimit
                    self._ocr_task = None
                    self._show_debug("Texterkennung dauert zu lange.")
                    return False
                return True
            if task.isSuccessful():
                result = task.getResult()
                text = result.getText() if result is not None else ""
                self._ocr_task = None
                self._handle_ocr_text(text)
            else:
                exc = task.getException()
                msg = exc.getMessage() if exc is not None else "unbekannt"
                self._ocr_task = None
                self._show_debug("Texterkennung fehlgeschlagen:\n" + str(msg))
            return False
        except Exception as e:
            self._ocr_task = None
            self._show_debug("OCR-Poll-Fehler:\n" + str(e))
            return False

    def _handle_ocr_text(self, text):
        result = extract_price(text or "")
        if result.price is None:
            self._show_debug("Erkannter Text:\n%s\n\nKein Preis gefunden - bitte manuell." % (text or "(leer)"))
            self.open_manual_entry(prefill=(text or "").strip())
        elif result.ambiguous:
            self._show_price_choice(result.candidates)
        else:
            self.add_item(result.price)
            self._show_debug("Erkannt: %.2f EUR\nautomatisch hinzugefuegt." % result.price)

    def on_photo_captured(self, path):
        # Rueckruf kommt evtl. aus einem anderen Thread -> auf Main-Thread
        # Doppel-Ausloesung (zwei Rueckruf-Wege) abfangen:
        import time
        now = time.time()
        if now - getattr(self, "_last_capture_time", 0.0) < 1.5:
            return
        self._last_capture_time = now
        Clock.schedule_once(lambda dt: self._after_capture(path), 0)

    def _after_capture(self, path):
        try:
            self._display_photo(path)
            self.report_status("Lese Zahl aus dem Foto ...")
            from jnius import autoclass
            BitmapFactory = autoclass("android.graphics.BitmapFactory")
            bmp = BitmapFactory.decodeFile(path)
            if bmp is None:
                self._show_debug("Foto gespeichert, aber nicht lesbar:\n" + str(path))
                return
            self._run_mlkit_ocr(bmp)
        except Exception as e:
            self._show_debug("Fehler nach Aufnahme:\n" + str(e))

    def _display_photo(self, path):
        try:
            photo_view = self.root.ids.photo_view
            photo_view.source = path
            photo_view.reload()
        except Exception as e:
            self._show_debug("Anzeige-Fehler:\n" + str(e))

    # ------------------------------------------------------------------
    #  "Preis scannen":
    #    Android  -> Einzelfoto aufnehmen und anzeigen (Live-Vorschau umgangen)
    #    Desktop  -> ROI -> optimieren -> OCR -> Preisfilter
    # ------------------------------------------------------------------
    def scan_price(self):
        if platform == "android":
            if self.camera is not None:
                self.report_status("Foto wird aufgenommen ...")
                self.camera.take_photo()
            else:
                self._show_debug("Kamera nicht bereit.")
            return
        if self.camera is not None:
            roi = self.camera.get_roi()
        else:
            roi = None
        if roi is None:
            self.open_manual_entry()
            return
        processed = preprocess_for_ocr(roi)
        raw_text = run_ocr(processed)
        result = extract_price(raw_text)
        print("OCR-Rohtext:", repr(raw_text), "->", result)
        if result.price is None:
            self.open_manual_entry(prefill=raw_text.strip())
        elif result.ambiguous:
            self._show_price_choice(result.candidates)
        else:
            self.add_item(result.price)

    # ------------------------------------------------------------------
    #  Auswahl-Dialog bei mehreren erkannten Preisen
    # ------------------------------------------------------------------
    def _show_price_choice(self, candidates):
        buttons = []
        for value in candidates:
            price_button = MDRaisedButton(text=self._format_eur(value))
            price_button.bind(on_release=partial(self._choose_price, value))
            buttons.append(price_button)
        manual_button = MDFlatButton(text="Manuell")
        manual_button.bind(on_release=self._on_manual_from_choice)
        buttons.append(manual_button)
        self._choice_dialog = MDDialog(
            title="Mehrere Preise erkannt",
            text="Im Sucher wurden unterschiedliche Preise gefunden. Bitte den richtigen waehlen:",
            buttons=buttons,
        )
        self._choice_dialog.open()

    def _choose_price(self, value, instance):
        self.add_item(value)
        if self._choice_dialog is not None:
            self._choice_dialog.dismiss()

    def _on_manual_from_choice(self, instance):
        if self._choice_dialog is not None:
            self._choice_dialog.dismiss()
        self.open_manual_entry()

    # ------------------------------------------------------------------
    #  Artikel-Verwaltung
    # ------------------------------------------------------------------
    def add_item(self, price, name=None):
        if name is None:
            name = "Artikel " + str(len(self.cart_items) + 1)
        self.cart_items.append({"name": name, "price": float(price)})
        self._refresh_list()
        self._update_total()

    def remove_item(self, index):
        if 0 <= index < len(self.cart_items):
            del self.cart_items[index]
            self._refresh_list()
            self._update_total()

    def _refresh_list(self):
        cart_list = self.root.ids.cart_list
        cart_list.clear_widgets()
        for i, item in enumerate(self.cart_items):
            row = CartRow()
            row.item_name = item["name"]
            row.price_text = self._format_eur(item["price"])
            row.index = i
            cart_list.add_widget(row)

    def _update_total(self):
        total = 0.0
        for item in self.cart_items:
            total += item["price"]
        self.total_text = self._format_eur(total)

    @staticmethod
    def _format_eur(value):
        s = "{:,.2f}".format(value)
        s = s.replace(",", "X").replace(".", ",").replace("X", ".")
        return s + " EUR"

    # ------------------------------------------------------------------
    #  Manuelle Schnell-Eingabe (Fallback)
    # ------------------------------------------------------------------
    def open_manual_entry(self, prefill=""):
        self.price_field = MDTextField(
            text=prefill,
            hint_text="Preis in EUR (z.B. 2,49)",
            helper_text="Komma oder Punkt erlaubt",
            helper_text_mode="on_focus",
            input_filter=self._number_filter,
        )
        cancel_button = MDFlatButton(text="Abbrechen")
        cancel_button.bind(on_release=self._on_cancel_manual)
        confirm_button = MDRaisedButton(text="Hinzufuegen")
        confirm_button.bind(on_release=self._on_confirm_manual)
        self._dialog = MDDialog(
            title="Preis manuell eingeben",
            type="custom",
            content_cls=self.price_field,
            buttons=[cancel_button, confirm_button],
        )
        self._dialog.open()
        Clock.schedule_once(self._focus_price_field, 0.3)

    def _focus_price_field(self, dt):
        if self.price_field is not None:
            self.price_field.focus = True

    def _number_filter(self, substring, from_undo):
        result = ""
        for char in substring:
            if char.isdigit() or char in ",.":
                result += char
        return result

    def _on_cancel_manual(self, instance):
        if self._dialog is not None:
            self._dialog.dismiss()

    def _on_confirm_manual(self, instance):
        raw = (self.price_field.text or "").strip().replace(",", ".")
        try:
            price = float(raw)
        except ValueError:
            self.price_field.error = True
            return
        if price > 0:
            self.add_item(price)
            self._dialog.dismiss()
        else:
            self.price_field.error = True

    # ------------------------------------------------------------------
    #  Aufraeumen
    # ------------------------------------------------------------------
    def on_stop(self):
        if self.camera is not None:
            self.camera.stop()


if __name__ == "__main__":
    ShoppingScannerApp().run()
