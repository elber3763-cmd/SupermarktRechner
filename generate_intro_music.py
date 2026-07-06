#!/usr/bin/env python3
import numpy as np
from scipy.io import wavfile
import os

# Parameters
sample_rate = 44100
duration = 3.5  # seconds
output_path = "app/src/main/res/raw/intro_music.wav"

# Create output directory if it doesn't exist
os.makedirs(os.path.dirname(output_path), exist_ok=True)

# Generate time array
t = np.linspace(0, duration, int(sample_rate * duration))

# Main melody (ascending tech-inspired notes)
# Notes: E4, G4, B4, E5 (ascending pattern - elegant and modern)
frequencies = [329.63, 392.00, 493.88, 659.25]  # Hz
note_duration = 0.6  # seconds per note

audio = np.zeros_like(t)

# Create ascending notes with smooth envelope
for i, freq in enumerate(frequencies):
    start_time = i * note_duration
    end_time = start_time + note_duration

    # Find indices for this note
    start_idx = int(start_time * sample_rate)
    end_idx = int(end_time * sample_rate)

    if end_idx > len(t):
        end_idx = len(t)

    # Generate sine wave for this note
    note_t = t[start_idx:end_idx]
    note = np.sin(2 * np.pi * freq * note_t)

    # Apply envelope (fade in/out for smooth transitions)
    envelope = np.hanning(len(note))
    note = note * envelope

    audio[start_idx:end_idx] += note

# Add background harmonics (subtle pad sound)
pad_freq = 164.81  # E3 (lower octave for warmth)
pad = 0.3 * np.sin(2 * np.pi * pad_freq * t)
pad_envelope = np.hanning(len(t))
pad = pad * pad_envelope
audio += pad

# Add high frequency shimmer (modern tech sound)
shimmer_freq = 880  # A5
shimmer = 0.15 * np.sin(2 * np.pi * shimmer_freq * t * 1.5)
shimmer_envelope = np.hanning(len(t))
shimmer = shimmer * shimmer_envelope
audio += shimmer

# Add subtle reverb effect (echo)
echo_delay = 0.15  # seconds
echo_samples = int(echo_delay * sample_rate)
echo = np.zeros_like(t)
if echo_samples < len(t):
    echo[echo_samples:] += audio[:-echo_samples] * 0.2
audio += echo

# Normalize to prevent clipping
max_amplitude = np.max(np.abs(audio))
if max_amplitude > 0:
    audio = (audio / max_amplitude) * 0.9  # Leave headroom

# Convert to 16-bit PCM
audio_int16 = np.int16(audio * 32767)

# Write WAV file
wavfile.write(output_path, sample_rate, audio_int16)

print("✓ Intro-Musik erfolgreich erstellt!")
print(f"  📁 Pfad: {output_path}")
print(f"  ⏱️  Dauer: {duration}s")
print(f"  🎚️  Sample-Rate: {sample_rate}Hz")
print(f"  🎵 Melodie: E4 → G4 → B4 → E5 (aufsteigend)")
print(f"  ✨ Features: Harmonische Pads + moderner Shimmer-Effekt + Echo")
print("\n✓ Die App wird automatisch diese Musik beim nächsten Build laden!")
