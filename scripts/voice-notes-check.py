#!/usr/bin/env python3
"""Real-song check of « La mélodie de la voix en notes » (lot C, Benjamin's n° 23).

The app's voice notes (SongMatter.voiceNotes, run through the JVM by the skipped-by-default test
VoiceNotesFileCheck) on a really separated voice, over the whole song, compared with Basic Pitch on
the same voice, two ways (an octave off fails both):
- frames (the goal): Basic Pitch's most likely pitch over each note's time (its note activations
  summed over the note's frames); a note where Basic Pitch hears no pitch at all fails;
- notes (for information): the Basic Pitch note that overlaps ours most; Basic Pitch's note
  thresholds miss short sung notes, so a note it did not make fails here.
Goal: at least 80 % of our notes within ±1 semitone.

    scripts/separation-tests.sh --file song.mp3 --out SCRATCH/stems
    scripts/voice-notes-check.py --stems SCRATCH/stems --work SCRATCH/notes --bpm 113 [--first-beat 0.42]

Everything it writes goes to --work, which must be outside the repository: the song, its stems
and its notes are copyrighted and never committed. Exit 0 when the goal is met, 1 otherwise.
"""
import argparse
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def overlap(a0, a1, b0, b1):
    return max(0.0, min(a1, b1) - max(a0, b0))


def compare(ours, theirs, tolerance=1):
    """(passed, matched, total, octave_slips): our notes against the reference's, as described above."""
    theirs = sorted(theirs, key=lambda n: n[0])
    passed = matched = slips = 0
    j0 = 0
    for start, end, pitch in ours:
        while j0 < len(theirs) and theirs[j0][1] <= start - 5.0:
            j0 += 1
        best, best_ov = None, 0.0
        j = j0
        while j < len(theirs) and theirs[j][0] < end:
            ov = overlap(start, end, theirs[j][0], theirs[j][1])
            if ov > best_ov:
                best, best_ov = theirs[j], ov
            j += 1
        if best is None:
            continue
        matched += 1
        d = abs(pitch - best[2])
        if d <= tolerance:
            passed += 1
        elif abs(d - 12) <= tolerance:
            slips += 1
    return passed, matched, len(ours), slips


def frame_pitches(ours, notes, times, floor=0.15):
    """(passed, heard, total): our notes against Basic Pitch's activation frames ([frames, 88], MIDI 21 up)."""
    import numpy as np
    passed = heard = 0
    for start, end, pitch in ours:
        a = np.searchsorted(times, start)
        z = max(a + 1, np.searchsorted(times, end))
        act = notes[a:z]
        if len(act) == 0:
            continue
        mean = act.mean(axis=0)
        if mean.max() < floor:
            continue
        heard += 1
        if abs(int(mean.argmax()) + 21 - pitch) <= 1:
            passed += 1
    return passed, heard, len(ours)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--stems", required=True, help="folder holding vocals.wav (separation-tests.sh --file)")
    ap.add_argument("--work", required=True, help="scratch folder outside the repository")
    ap.add_argument("--bpm", type=float, default=120.0, help="the song's tempo (the notes' sixteenths)")
    ap.add_argument("--first-beat", type=float, default=0.0, help="the song's first beat, seconds")
    ap.add_argument("--goal", type=float, default=0.80, help="share of our notes within ±1 semitone")
    args = ap.parse_args()

    work = os.path.realpath(args.work)
    if (work + os.sep).startswith(ROOT + os.sep):
        sys.exit("--work must be outside the repository (copyrighted material)")
    os.makedirs(work, exist_ok=True)
    voice = os.path.join(args.stems, "vocals.wav")
    if not os.path.isfile(voice):
        sys.exit(f"no voice: {voice}")

    # 1. The voice as the app reads it: mono, 32-bit float, 48 kHz.
    raw = os.path.join(work, "voice48.raw")
    subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", voice, "-f", "f32le",
                    "-acodec", "pcm_f32le", "-ac", "1", "-ar", "48000", raw], check=True)

    # 2. Our notes, by the app's own code.
    ours_path = os.path.join(work, "ours.json")
    env = dict(os.environ, VOICE_NOTES_RAW=raw, VOICE_NOTES_OUT=ours_path,
               VOICE_NOTES_BPM=str(args.bpm), VOICE_NOTES_FIRST_BEAT=str(args.first_beat))
    if os.path.exists(ours_path):
        os.remove(ours_path)
    subprocess.run([os.path.join(ROOT, "gradlew"), "--max-workers=2", "--no-configuration-cache", "-q",
                    ":project-model:test", "--tests", "com.pocketstudio.model.VoiceNotesFileCheck", "--rerun"],
                   cwd=ROOT, env=env, check=True)
    with open(ours_path) as f:
        ours = [tuple(n) for n in json.load(f)]

    # 3. Basic Pitch's notes on the same voice (its own resampling).
    from basic_pitch import ICASSP_2022_MODEL_PATH
    from basic_pitch.inference import predict
    from basic_pitch.note_creation import model_frames_to_time
    output, _, events = predict(voice, ICASSP_2022_MODEL_PATH)
    theirs = [(float(e[0]), float(e[1]), int(e[2])) for e in events]
    with open(os.path.join(work, "basic-pitch.json"), "w") as f:
        json.dump(theirs, f)
    activations = output["note"]
    times = model_frames_to_time(activations.shape[0])

    passed, heard, total = frame_pitches(ours, activations, times)
    n_passed, matched, _, slips = compare(ours, theirs)
    span = (min(n[0] for n in ours), max(n[1] for n in ours)) if ours else (0, 0)
    score = passed / total if total else 0.0
    print(f"VOICE NOTES: {total} notes of ours ({span[0]:.1f} s to {span[1]:.1f} s), {len(theirs)} of Basic Pitch")
    print(f"  frames: Basic Pitch hears a pitch under {heard}/{total}; within ±1 semitone: {passed}/{total} = {100 * score:.1f} % (goal {100 * args.goal:.0f} %)")
    print(f"  notes: overlapping a Basic Pitch note {matched}/{total}; within ±1 semitone {n_passed}/{total} = {100 * n_passed / max(1, total):.1f} %"
          f" ({100 * n_passed / max(1, matched):.1f} % of the overlapping ones); an octave off: {slips}")
    # Thirds of the song: the notes must hold all along, not only at its start.
    if ours:
        third = (span[1] - span[0]) / 3 or 1
        for k in range(3):
            part = [n for n in ours if span[0] + k * third <= n[0] < span[0] + (k + 1) * third]
            p, _, t = frame_pitches(part, activations, times)
            print(f"  third {k + 1}: {p}/{t} = {100 * p / t if t else 0:.1f} %")
    with open(os.path.join(work, "score.txt"), "w") as f:
        f.write(f"frames {passed}/{total} {score:.4f}; notes {n_passed}/{total}\n")
    print("VOICE NOTES OK" if score >= args.goal else "VOICE NOTES BELOW GOAL")
    return 0 if score >= args.goal else 1


if __name__ == "__main__":
    sys.exit(main())
