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

audio = np.zeros_like(t)

# ===== DRUMS =====
# Kick drum (bass drum) - strong beat
kick_times = [0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0]  # Beat pattern
for kick_time in kick_times:
    kick_idx = int(kick_time * sample_rate)
    kick_duration = int(0.15 * sample_rate)

    if kick_idx + kick_duration < len(t):
        # Synthesize kick drum - frequency sweep from 150Hz to 50Hz
        kick_t = np.linspace(0, 0.15, kick_duration)
        kick_freq = 150 - (kick_t * 400)  # Frequency decay
        kick_phase = 2 * np.pi * np.cumsum(kick_freq) / sample_rate
        kick = np.sin(kick_phase)

        # Envelope for kick
        kick_env = np.exp(-kick_t * 15)
        kick = kick * kick_env * 1.2

        audio[kick_idx:kick_idx+kick_duration] += kick

# Snare drum - crisp sound
snare_times = [0.5, 1.5, 2.5]
for snare_time in snare_times:
    snare_idx = int(snare_time * sample_rate)
    snare_duration = int(0.1 * sample_rate)

    if snare_idx + snare_duration < len(t):
        # Snare - noise with resonance
        snare_t = np.linspace(0, 0.1, snare_duration)
        snare_noise = np.random.normal(0, 0.5, snare_duration)

        # Resonant frequencies
        freq1, freq2 = 200, 300
        snare_tone = (0.5 * np.sin(2*np.pi*freq1*snare_t) +
                      0.5 * np.sin(2*np.pi*freq2*snare_t))

        snare = (snare_noise * 0.5 + snare_tone * 0.5)

        # Envelope
        snare_env = np.exp(-snare_t * 30)
        snare = snare * snare_env

        audio[snare_idx:snare_idx+snare_duration] += snare

# ===== BASS GUITAR =====
# Power chord progression: E - A - E - B (rock style)
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

    # Bass guitar - slightly distorted sine wave for rock feel
    bass = np.sin(2*np.pi*freq*bass_t)

    # Add harmonics for richness
    bass += 0.3 * np.sin(2*np.pi*freq*2*bass_t)  # Octave
    bass += 0.1 * np.sin(2*np.pi*freq*3*bass_t)  # Fifth

    # Distortion (soft clipping)
    bass = np.tanh(bass * 1.5)

    # Envelope - attack and decay
    bass_env = np.ones_like(bass_t)
    attack_samples = int(0.05 * sample_rate)
    decay_samples = int(0.2 * sample_rate)

    bass_env[:attack_samples] = np.linspace(0, 1, attack_samples)
    bass_env[-decay_samples:] = np.linspace(1, 0.1, decay_samples)

    bass = bass * bass_env * 0.4

    audio[start_idx:end_idx] += bass

# ===== ELECTRIC GUITAR =====
# Rock riff - E5 pentatonic minor scale
guitar_notes = [659.25, 739.99, 659.25, 587.33, 659.25, 739.99, 659.25, 587.33]  # E5, F#5, E5, D5
guitar_start_times = np.linspace(0.2, 3.0, len(guitar_notes))
guitar_note_duration = 0.3

for freq, start_time in zip(guitar_notes, guitar_start_times):
    start_idx = int(start_time * sample_rate)
    end_idx = int((start_time + guitar_note_duration) * sample_rate)

    if end_idx > len(t):
        end_idx = len(t)

    guitar_t = t[start_idx:end_idx]

    # Electric guitar tone with distortion
    guitar = np.sin(2*np.pi*freq*guitar_t)

    # Add harmonics for guitar timbre
    guitar += 0.2 * np.sin(2*np.pi*freq*2*guitar_t)
    guitar += 0.1 * np.sin(2*np.pi*freq*3*guitar_t)

    # Distortion for rock sound
    guitar = np.tanh(guitar * 2.5)

    # Envelope
    guitar_env = np.exp(-guitar_t * 8)
    guitar = guitar * guitar_env * 0.3

    audio[start_idx:end_idx] += guitar

# ===== POWER CHORDS (High notes) =====
# Rock power chord hits
chord_times = [0, 1.0, 2.0]
for chord_time in chord_times:
    chord_idx = int(chord_time * sample_rate)
    chord_duration = int(0.2 * sample_rate)

    if chord_idx + chord_duration < len(t):
        chord_t = t[chord_idx:chord_idx+chord_duration]

        # E major power chord: E5, B5
        chord = (0.5 * np.sin(2*np.pi*659.25*chord_t) +  # E5
                 0.5 * np.sin(2*np.pi*987.77*chord_t))   # B5

        # Distortion
        chord = np.tanh(chord * 2.0)

        # Envelope
        chord_env = np.exp(-chord_t * 10)
        chord = chord * chord_env * 0.4

        audio[chord_idx:chord_idx+chord_duration] += chord

# ===== CYMBALS =====
# High-frequency shimmer for cymbals
cymbal_freqs = [8000, 12000, 15000]
cymbal_noise = np.random.normal(0, 0.3, len(t))

for freq in cymbal_freqs:
    cymbal_filtered = cymbal_noise * np.sin(2*np.pi*freq*t) * 0.1
    audio += cymbal_filtered

# Normalize to prevent clipping
max_amplitude = np.max(np.abs(audio))
if max_amplitude > 0:
    audio = (audio / max_amplitude) * 0.85  # Leave headroom

# Convert to 16-bit PCM
audio_int16 = np.int16(audio * 32767)

# Write WAV file
wavfile.write(output_path, sample_rate, audio_int16)

print("✓ Rock/Pop-Musik erfolgreich erstellt!")
print(f"  📁 Pfad: {output_path}")
print(f"  ⏱️  Dauer: {duration}s")
print(f"  🎚️  Sample-Rate: {sample_rate}Hz")
print(f"  🎸 Stil: AC/DC Rock")
print(f"  🥁 Features:")
print(f"     - Kick Drum & Snare")
print(f"     - Bass Guitar (E-A-E-B Power Chord)")
print(f"     - Electric Guitar Riff (Pentatonic)")
print(f"     - Power Chords")
print(f"     - Distortion & Heavy Sound")
print(f"     - Cymbals")
print("\n✓ Die App wird automatisch diese Rock-Musik beim nächsten Build laden!")
