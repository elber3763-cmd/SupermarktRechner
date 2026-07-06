#!/usr/bin/env python3
import numpy as np
from scipy.io import wavfile
import os

# Parameters
sample_rate = 44100
duration = 3.5
output_path = "app/src/main/res/raw/intro_music.wav"

os.makedirs(os.path.dirname(output_path), exist_ok=True)

# Generate time array
t = np.linspace(0, duration, int(sample_rate * duration))

audio = np.zeros_like(t)

# ===== KICK DRUM (Clean) =====
kick_times = [0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0]
for kick_time in kick_times:
    kick_idx = int(kick_time * sample_rate)
    kick_duration = int(0.15 * sample_rate)

    if kick_idx + kick_duration < len(t):
        kick_t = np.linspace(0, 0.15, kick_duration)
        # Frequency sweep: 150Hz -> 50Hz
        kick_freq_start = 150
        kick_freq_end = 50
        kick_freq = kick_freq_start + (kick_freq_end - kick_freq_start) * (kick_t / 0.15)

        # Generate clean sine wave
        kick_phase = 2 * np.pi * np.cumsum(kick_freq) / sample_rate
        kick = np.sin(kick_phase)

        # Smooth envelope (exponential decay)
        kick_env = np.exp(-kick_t * 15)
        kick = kick * kick_env * 1.0

        audio[kick_idx:kick_idx+kick_duration] += kick

# ===== SNARE DRUM (Clean - no noise) =====
snare_times = [0.5, 1.5, 2.5]
for snare_time in snare_times:
    snare_idx = int(snare_time * sample_rate)
    snare_duration = int(0.08 * sample_rate)

    if snare_idx + snare_duration < len(t):
        snare_t = np.linspace(0, 0.08, snare_duration)

        # Clean sine waves instead of noise
        freq1, freq2 = 200, 320
        snare = (0.6 * np.sin(2*np.pi*freq1*snare_t) +
                 0.4 * np.sin(2*np.pi*freq2*snare_t))

        # Envelope
        snare_env = np.exp(-snare_t * 35)
        snare = snare * snare_env * 0.8

        audio[snare_idx:snare_idx+snare_duration] += snare

# ===== BASS GUITAR (Clean) =====
bass_freqs = [82.41, 110.00, 82.41, 123.47]  # E2, A2, E2, B2
bass_timing = [0, 1.0, 2.0, 2.8]

for i, (freq, start_time) in enumerate(zip(bass_freqs, bass_timing)):
    if i < len(bass_freqs) - 1:
        duration_bass = bass_timing[i+1] - start_time
    else:
        duration_bass = 0.6

    start_idx = int(start_time * sample_rate)
    end_idx = int((start_time + duration_bass) * sample_rate)

    if end_idx > len(t):
        end_idx = len(t)

    bass_t = t[start_idx:end_idx]

    # Clean bass - multiple harmonics
    bass = np.sin(2*np.pi*freq*bass_t)
    bass += 0.25 * np.sin(2*np.pi*freq*2*bass_t)  # Octave
    bass += 0.1 * np.sin(2*np.pi*freq*3*bass_t)   # Fifth

    # Soft clipping (less aggressive)
    bass = np.tanh(bass * 1.2)

    # Smooth envelope
    bass_env = np.ones_like(bass_t)
    attack_samples = int(0.05 * sample_rate)
    decay_samples = int(0.15 * sample_rate)

    if attack_samples > 0:
        bass_env[:attack_samples] = np.linspace(0, 1, attack_samples)
    if decay_samples > 0:
        bass_env[-decay_samples:] = np.linspace(1, 0.2, decay_samples)

    bass = bass * bass_env * 0.35

    audio[start_idx:end_idx] += bass

# ===== ELECTRIC GUITAR (Clean) =====
guitar_notes = [659.25, 739.99, 659.25, 587.33, 659.25, 739.99, 659.25, 587.33]
guitar_start_times = np.linspace(0.2, 3.0, len(guitar_notes))
guitar_note_duration = 0.3

for freq, start_time in zip(guitar_notes, guitar_start_times):
    start_idx = int(start_time * sample_rate)
    end_idx = int((start_time + guitar_note_duration) * sample_rate)

    if end_idx > len(t):
        end_idx = len(t)

    guitar_t = t[start_idx:end_idx]

    # Clean guitar tone
    guitar = np.sin(2*np.pi*freq*guitar_t)
    guitar += 0.15 * np.sin(2*np.pi*freq*2*guitar_t)
    guitar += 0.08 * np.sin(2*np.pi*freq*3*guitar_t)

    # Light distortion
    guitar = np.tanh(guitar * 1.5)

    # Smooth envelope
    guitar_env = np.exp(-guitar_t * 6)
    guitar = guitar * guitar_env * 0.25

    audio[start_idx:end_idx] += guitar

# ===== POWER CHORDS =====
chord_times = [0, 1.0, 2.0]
for chord_time in chord_times:
    chord_idx = int(chord_time * sample_rate)
    chord_duration = int(0.2 * sample_rate)

    if chord_idx + chord_duration < len(t):
        chord_t = t[chord_idx:chord_idx+chord_duration]

        # Clean power chord
        chord = (0.6 * np.sin(2*np.pi*659.25*chord_t) +   # E5
                 0.4 * np.sin(2*np.pi*987.77*chord_t))     # B5

        # Light distortion
        chord = np.tanh(chord * 1.3)

        # Smooth envelope
        chord_env = np.exp(-chord_t * 8)
        chord = chord * chord_env * 0.35

        audio[chord_idx:chord_idx+chord_duration] += chord

# ===== HIGH NOTES (Clean shimmer - no noise) =====
# Add clean high-frequency tones instead of noise
high_freq = 1200  # Clean high tone
high_notes = []
for i in range(int(duration * 4)):
    note_start = i * 0.875
    note_duration = 0.3
    if note_start + note_duration <= duration:
        high_notes.append((note_start, note_duration))

for note_start, note_duration in high_notes:
    note_idx = int(note_start * sample_rate)
    note_end_idx = int((note_start + note_duration) * sample_rate)

    if note_end_idx > len(t):
        note_end_idx = len(t)

    note_t = t[note_idx:note_end_idx]

    # Clean high note
    high_note = 0.15 * np.sin(2*np.pi*high_freq*note_t)

    # Smooth envelope
    note_env = np.exp(-note_t * 5)
    high_note = high_note * note_env

    audio[note_idx:note_end_idx] += high_note

# ===== NORMALIZATION & COMPRESSION =====
# Apply gentle compression to reduce peaks
threshold = 0.8
ratio = 4.0

for i in range(len(audio)):
    if abs(audio[i]) > threshold:
        sign = np.sign(audio[i])
        excess = abs(audio[i]) - threshold
        audio[i] = sign * (threshold + excess / ratio)

# Final normalization
max_amplitude = np.max(np.abs(audio))
if max_amplitude > 0:
    audio = (audio / max_amplitude) * 0.9

# Convert to 16-bit PCM
audio_int16 = np.int16(audio * 32767)

# Write WAV file
wavfile.write(output_path, sample_rate, audio_int16)

print("✓ Clean Rock-Musik erfolgreich erstellt!")
print(f"  📁 Pfad: {output_path}")
print(f"  ⏱️  Dauer: {duration}s")
print(f"  🎚️  Sample-Rate: {sample_rate}Hz")
print(f"  🎸 Stil: AC/DC Rock (SAUBER)")
print(f"  ✨ Optimierungen:")
print(f"     ✓ Kein störendes Rauschen")
print(f"     ✓ Saubere Sine-Wave Signale")
print(f"     ✓ Sanfte Kompression")
print(f"     ✓ Glatte Envelope")
print(f"     ✓ Optimales Mixing")
print("\n✓ Die App wird automatisch diese saubere Rock-Musik laden!")
