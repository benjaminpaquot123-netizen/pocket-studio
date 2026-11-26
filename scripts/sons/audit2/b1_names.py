"""Batch 1: type / subtype / role / style from trap names."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *

K, SN, HC, HO, CL = kick(), snare(), hat(0.08, 0.012), hat(0.6, 0.15), clap()
B8 = b808()
DL = lambda bpm=120, bars=2: drum_loop(bpm, bars)
ML = lambda bpm=120, bars=4: chord_loop(bpm, bars)
PAD = tone(220, 3.0, 0.3)
cases = [
    # melodic words in folder or name kick a drum out of the drum table
    ("Drums/Kicks/Synth Hard/Kick 01.wav", K), ("Drums/Kicks/Kick 01.wav", K),
    ("Drums/Kicks/Synth Kick 01.wav", K), ("Drums/Snares/Synth Snare 01.wav", SN),
    ("Drums/Kicks/Bass Drum 01.wav", K), ("Drums/Kicks/Kick Punchy Keyed.wav", K),
    ("Drums/Kicks/Kick Bassy 01.wav", K), ("Drums/Snares/Snare Padded 01.wav", SN),
    ("Drums/Claps/Clap Leader.wav", CL), ("Drums/Hats/Hat Plucky 01.wav", HC),
    ("Drums/Kicks/Kickdrum 01.wav", K), ("Drums/Kicks/Kick_Drum_01.wav", K), ("Drums/Kicks/BD 01.wav", K),
    ("Drums/Snares/SN 01.wav", SN), ("Drums/Snares/SD 01.wav", SN), ("Drums/Claps/CP 01.wav", CL),
    ("Drums/Snares/Rimshot 01.wav", SN), ("Drums/Perc/Cowbell 01.wav", HC), ("Drums/Perc/Tambourine 01.wav", HC),
    ("Drums/Perc/Conga High 01.wav", SN), ("Drums/Toms/Tom Low 01.wav", K), ("Drums/Perc/Perc Mid 01.wav", SN),
    ("Drums/Perc/Mid/Perc 01.wav", SN), ("Drums/Perc/High/Perc 01.wav", SN),
    # an ancestor folder with a loop word turns one-shots into loops
    ("Loops & One Shots/Kicks/Kick 01.wav", K), ("Trap Beats/One Shots/Snare 01.wav", SN),
    ("Drum Fills & Hits/Hits/Clap 01.wav", CL), ("Beat Makers Pack/Hats/Hat 01.wav", HC),
    ("Stems & Shots/Shots/Kick 01.wav", K),
    # open / closed hats
    ("Drums/Hats/Open/Hat 01.wav", HO), ("Drums/Hats/Closed/Hat 01.wav", HC),
    ("Drums/Open Hats/Hat 01.wav", HO), ("Drums/Closed Hats/Hat 01.wav", HC),
    ("Drums/Hats/Open/Open 01.wav", HO), ("Drums/Hats/Open/01.wav", HO), ("Drums/Hats/Closed/01.wav", HC),
    ("Drums/Hats/Hat Open 01.wav", HO), ("Drums/Hats/Hat Closed 01.wav", HC),
    ("Drums/Hats/Hat 01.wav", HO), ("Drums/Hats/Hat 02.wav", HC),
    ("Drums/Hats/OH 01.wav", HO), ("Drums/Hats/CH 01.wav", HC), ("Drums/Hats/Hat Op 01.wav", HO),
    ("Drums/Hats/Hat Cl 01.wav", HC), ("Drums/Hats/Hi-Hat Open 01.wav", HO), ("Drums/Hats/HiHat_Closed_01.wav", HC),
    ("Drums/Hats/Hat - Open - 01.wav", HO), ("Drums/Hats/Charley Ouvert 01.wav", HO),
    ("Drums/Hats/Hat Semi Open 01.wav", HO), ("Drums/Hats/Hat Half-Open 01.wav", HO),
    ("Drums/Hats/Hat Pedal 01.wav", HC), ("Drums/Hats/Opened Hat 01.wav", HO),
    # 808
    ("Drums/808 Kit/Snare 01.wav", SN), ("Drums/Snares/808 Snare 01.wav", SN), ("Drums/Claps/808 Clap.wav", CL),
    ("Drums/Hats/TR-808 Hat Closed.wav", HC), ("Drums/TR-808/Kick 01.wav", K), ("Drums/808s/808 C.wav", B8),
    ("Drums/909/Kick 01.wav", K), ("Drums/Kicks/Kick 808 Style.wav", K),
    # full / master words
    ("Drum Loops/Full Drum Loop 128.wav", DL(128)), ("Drums/Kicks/Kick Mastered 01.wav", K),
    ("Drums/Kicks/Kick Full 01.wav", K), ("Drums/Snares/Snare Mix 01.wav", SN), ("Drums/Kicks/Kick Song 01.wav", K),
    # fx words
    ("FX/Reverse Crash 01.wav", HO), ("Loops/Reversed Piano Loop 120 Am.wav", ML()),
    ("Loops/Noisey Bass Loop 140 F.wav", ML(140)), ("Loops/Texture Pad Loop 01.wav", PAD),
    ("Drums/Kicks/Kick Impact 01.wav", K), ("Drums/Snares/Snare Noise 01.wav", SN),
    ("Bass/Bass Sweep C2.wav", B8), ("Synth/Atmosphere Pad C3.wav", PAD), ("Drums/Kicks/Kick Zapper.wav", K),
    ("Bass/Reese Drop 01.wav", B8), ("Drums/Snares/Snare Swell.wav", SN),
    # vocal words
    ("Loops/Guitar Phrase 120 Am.wav", ML()), ("Synth/Vox Lead C3.wav", PAD), ("Synth/Voice Pad C3.wav", PAD),
    ("Synth/Vocoder Chord Am.wav", PAD), ("Drums/Perc/Vocal Perc 01.wav", SN), ("Loops/Chanting Monks Loop 90.wav", ML(90)),
    # loops without the word loop
    ("Tops/Top 128 01.wav", DL(128, 4)), ("Hi Hats/Hats 128 01.wav", DL(128, 2)),
    ("Melodies/Melody 01 Cm.wav", ML(160, 4)), ("Drums/Full Drums 01 120.wav", DL(120, 4)),
    ("Percussion/Perc 124 01.wav", DL(124, 1)), ("Bass/Bassline 01 124 Gm.wav", ML(124, 2)),
    ("Breaks/Break 170 01.wav", DL(170, 2)), ("Kick Loops/Kick 01.wav", drum_loop(125, 2, hats="none")),
    # melodic substrings in drum loops, drum substrings in melodic loops
    ("Loops/Organic Drum Loop 120.wav", DL()), ("Loops/Sharp Hat Loop 140.wav", DL(140)),
    ("Loops/Monkey Drum Loop 100.wav", DL(100)), ("Loops/Padded Kick Loop 125.wav", DL(125)),
    ("Loops/What Loop 120 Am.wav", ML()), ("Loops/Custom Loop 01 120 Am.wav", ML()),
    ("Loops/Upbeat Piano Loop 120 C.wav", ML()), ("Loops/Breakdown Synth Loop 128 Am.wav", ML(128)),
    ("Loops/Chat Lead Loop 120 Am.wav", ML()), ("Loops/Atomic Bass Loop 120 Am.wav", ML()),
    ("Loops/Superclap Loop 120.wav", DL()), ("Loops/Loop 01 120.wav", DL()), ("Loops/Loop 02 120 Am.wav", ML()),
    ("Loops/Drum & Bass Loop 174.wav", DL(174)), ("Loops/Bass & Drums Loop 120 Am.wav", chord_loop(120, 4, with_drums=True)),
    # kits
    ("Kits/Kit A/Bass C2.wav", B8), ("Kits/Kit A/Kick.wav", K), ("Drum Kits/Kit 01/Snare 01.wav", SN),
    ("Kit 808/Kick 01.wav", K), ("Kits/Trap Kit 140 Am/Drums.wav", DL(140, 4)),
    # long 808 with bpm in the name
    ("Drums/808s/808 Long 140bpm C.wav", b808(65.4, 2.0)), ("Drums/808s/808 Long C.wav", b808(65.4, 2.0)),
    ("Drums/808s/808 Very Long C.wav", b808(65.4, 11.0, 3.0)), ("Drums/Cymbals/Crash Long.wav", hat(11.0, 2.5)),
    ("Bass/Sub Bass C1.wav", b808(32.7, 2.0)), ("Bass/Bass Shot 01.wav", b808(55, 0.8, 0.3)),
    ("One Shots/Stab Am.wav", PAD), ("One Shots/Chord Stab 01.wav", PAD),
    ("Synth/Long Pad Cm.wav", tone(261.6, 9.0, 0.3)), ("Synth/Drone Dark.wav", tone(65.4, 12.0, 0.3)),
]
# Expected rows (docs/sons/AUDIT-TOUT-2.md, defects 9, 18, 23 to 26); empty fields omitted.
WANT = {
    'Drums/Kicks/Synth Hard/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/Synth Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Snares/Synth Snare 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Drums/Kicks/Bass Drum 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick Punchy Keyed.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick Bassy 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Snares/Snare Padded 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Drums/Claps/Clap Leader.wav': 'type=drum_oneshot, subtype=clap, role=snare, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Plucky 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kickdrum 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick_Drum_01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/BD 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Snares/SN 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Drums/Snares/SD 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Drums/Claps/CP 01.wav': 'type=drum_oneshot, subtype=clap, role=snare, style=unlabelled, end_cut=0',
    'Drums/Snares/Rimshot 01.wav': 'type=drum_oneshot, subtype=rim, role=snare, style=unlabelled, end_cut=0',
    'Drums/Perc/Cowbell 01.wav': 'type=drum_oneshot, subtype=cowbell, role=perc, style=unlabelled, end_cut=0',
    'Drums/Perc/Tambourine 01.wav': 'type=drum_oneshot, subtype=shaker, role=perc, style=unlabelled, end_cut=0',
    'Drums/Perc/Conga High 01.wav': 'type=drum_oneshot, subtype=perc_high, role=perc, style=unlabelled, end_cut=0',
    'Drums/Toms/Tom Low 01.wav': 'type=drum_oneshot, subtype=tom, role=perc, style=unlabelled, end_cut=0',
    'Drums/Perc/Perc Mid 01.wav': 'type=drum_oneshot, subtype=perc_mid, role=perc, style=unlabelled, end_cut=0',
    'Drums/Perc/Mid/Perc 01.wav': 'type=drum_oneshot, subtype=perc_mid, role=perc, style=unlabelled, end_cut=0',
    'Drums/Perc/High/Perc 01.wav': 'type=drum_oneshot, subtype=perc_high, role=perc, style=unlabelled, end_cut=0',
    'Loops & One Shots/Kicks/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Trap Beats/One Shots/Snare 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=trap, end_cut=0',
    'Drum Fills & Hits/Hits/Clap 01.wav': 'type=drum_oneshot, subtype=clap, role=snare, style=unlabelled, end_cut=0',
    'Beat Makers Pack/Hats/Hat 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Stems & Shots/Shots/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Hats/Open/Hat 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Closed/Hat 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Open Hats/Hat 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Closed Hats/Hat 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Open/Open 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Open/01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Closed/01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Open 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Closed 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat 02.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/OH 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/CH 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Op 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Cl 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hi-Hat Open 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/HiHat_Closed_01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat - Open - 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Charley Ouvert 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Semi Open 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Half-Open 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Hat Pedal 01.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/Hats/Opened Hat 01.wav': 'type=drum_oneshot, subtype=hat_open, role=hats, style=unlabelled, end_cut=0',
    'Drums/808 Kit/Snare 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Drums/Snares/808 Snare 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Drums/Claps/808 Clap.wav': 'type=drum_oneshot, subtype=clap, role=snare, style=unlabelled, end_cut=0',
    'Drums/Hats/TR-808 Hat Closed.wav': 'type=drum_oneshot, subtype=hat_closed, role=hats, style=unlabelled, end_cut=0',
    'Drums/TR-808/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/808s/808 C.wav': 'type=drum_oneshot, subtype=kick_808, role=808, style=unlabelled, end_cut=0',
    'Drums/909/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick 808 Style.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drum Loops/Full Drum Loop 128.wav': 'type=drum_loop, subtype=-, role=full, style=unlabelled, bpm_tag=128',
    'Drums/Kicks/Kick Mastered 01.wav': 'type=drum_oneshot, subtype=kick, role=full, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick Full 01.wav': 'type=drum_oneshot, subtype=kick, role=full, style=unlabelled, end_cut=0',
    'Drums/Snares/Snare Mix 01.wav': 'type=drum_oneshot, subtype=snare, role=full, style=unlabelled, end_cut=0',
    'Drums/Kicks/Kick Song 01.wav': 'type=drum_oneshot, subtype=kick, role=full, style=unlabelled, end_cut=0',
    'FX/Reverse Crash 01.wav': 'type=fx, subtype=-, role=fx, style=unlabelled',  # defect 9: a riser, not a hat
    'Loops/Reversed Piano Loop 120 Am.wav': 'type=melodic_loop, subtype=-, role=keys, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Loops/Noisey Bass Loop 140 F.wav': 'type=melodic_loop, subtype=-, role=bass, style=unlabelled, bpm_tag=140, key_tag=F major',
    'Loops/Texture Pad Loop 01.wav': 'type=melodic_loop, subtype=-, role=pad, style=unlabelled',
    'Drums/Kicks/Kick Impact 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drums/Snares/Snare Noise 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Bass/Bass Sweep C2.wav': 'type=oneshot_melodic, subtype=-, role=bass, style=unlabelled, note_tag=C, end_cut=0',
    'Synth/Atmosphere Pad C3.wav': 'type=oneshot_melodic, subtype=-, role=pad, style=unlabelled, note_tag=C, end_cut=1',
    'Drums/Kicks/Kick Zapper.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Bass/Reese Drop 01.wav': 'type=oneshot_melodic, subtype=-, role=bass, style=unlabelled, end_cut=0',
    'Drums/Snares/Snare Swell.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Loops/Guitar Phrase 120 Am.wav': 'type=melodic_loop, subtype=-, role=guitar, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Synth/Vox Lead C3.wav': 'type=oneshot_melodic, subtype=-, role=lead, style=unlabelled, note_tag=C, end_cut=1',
    'Synth/Voice Pad C3.wav': 'type=oneshot_melodic, subtype=-, role=pad, style=unlabelled, note_tag=C, end_cut=1',
    'Synth/Vocoder Chord Am.wav': 'type=oneshot_melodic, subtype=-, role=chords, style=unlabelled, end_cut=1',
    'Drums/Perc/Vocal Perc 01.wav': 'type=drum_oneshot, subtype=perc, role=perc, style=unlabelled, end_cut=0',
    'Loops/Chanting Monks Loop 90.wav': 'type=melodic_loop, subtype=by_sound, role=vocal, style=unlabelled, bpm_tag=90',
    'Tops/Top 128 01.wav': 'type=drum_loop, subtype=by_sound, role=drums, style=unlabelled, bpm_tag=128',
    'Hi Hats/Hats 128 01.wav': 'type=drum_loop, subtype=-, role=hats, style=unlabelled, bpm_tag=128',
    'Melodies/Melody 01 Cm.wav': 'type=melodic_loop, subtype=-, role=lead, style=unlabelled, key_tag=C minor',
    'Drums/Full Drums 01 120.wav': 'type=drum_loop, subtype=-, role=full, style=unlabelled, bpm_tag=120',
    'Percussion/Perc 124 01.wav': 'type=drum_loop, subtype=-, role=perc, style=unlabelled, bpm_tag=124',
    'Bass/Bassline 01 124 Gm.wav': 'type=melodic_loop, subtype=-, role=bass, style=unlabelled, bpm_tag=124, key_tag=G minor',
    'Breaks/Break 170 01.wav': 'type=drum_loop, subtype=-, role=drums, style=unlabelled, bpm_tag=170',
    'Kick Loops/Kick 01.wav': 'type=drum_loop, subtype=-, role=kick, style=unlabelled',
    'Loops/Organic Drum Loop 120.wav': 'type=drum_loop, subtype=-, role=drums, style=unlabelled, bpm_tag=120',
    'Loops/Sharp Hat Loop 140.wav': 'type=drum_loop, subtype=-, role=hats, style=unlabelled, bpm_tag=140',
    'Loops/Monkey Drum Loop 100.wav': 'type=drum_loop, subtype=-, role=drums, style=unlabelled, bpm_tag=100',
    'Loops/Padded Kick Loop 125.wav': 'type=drum_loop, subtype=-, role=kick, style=unlabelled, bpm_tag=125',
    'Loops/What Loop 120 Am.wav': 'type=melodic_loop, subtype=by_sound, role=-, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Loops/Custom Loop 01 120 Am.wav': 'type=melodic_loop, subtype=by_sound, role=-, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Loops/Upbeat Piano Loop 120 C.wav': 'type=melodic_loop, subtype=-, role=keys, style=unlabelled, bpm_tag=120, key_tag=C major',
    'Loops/Breakdown Synth Loop 128 Am.wav': 'type=melodic_loop, subtype=-, role=synth, style=unlabelled, bpm_tag=128, key_tag=A minor',
    'Loops/Chat Lead Loop 120 Am.wav': 'type=melodic_loop, subtype=-, role=lead, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Loops/Atomic Bass Loop 120 Am.wav': 'type=melodic_loop, subtype=-, role=bass, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Loops/Superclap Loop 120.wav': 'type=drum_loop, subtype=-, role=snare, style=unlabelled, bpm_tag=120',
    'Loops/Loop 01 120.wav': 'type=drum_loop, subtype=by_sound, role=-, style=unlabelled, bpm_tag=120',
    'Loops/Loop 02 120 Am.wav': 'type=melodic_loop, subtype=by_sound, role=-, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Loops/Drum & Bass Loop 174.wav': 'type=drum_loop, subtype=-, role=drums, style=dnb, bpm_tag=174',
    'Loops/Bass & Drums Loop 120 Am.wav': 'type=melodic_loop, subtype=-, role=bass, style=unlabelled, bpm_tag=120, key_tag=A minor',
    'Kits/Kit A/Bass C2.wav': 'type=oneshot_melodic, subtype=-, role=bass, style=unlabelled, note_tag=C, end_cut=0',
    'Kits/Kit A/Kick.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Drum Kits/Kit 01/Snare 01.wav': 'type=drum_oneshot, subtype=snare, role=snare, style=unlabelled, end_cut=0',
    'Kit 808/Kick 01.wav': 'type=drum_oneshot, subtype=kick, role=kick, style=unlabelled, end_cut=0',
    'Kits/Trap Kit 140 Am/Drums.wav': ['type=kit, subtype=-, role=-, style=trap, bpm_tag=140, key_tag=A minor', 'type=kit_stem, subtype=-, role=drums, style=trap, bpm_tag=140'],  # a drum stem carries no key (repair 7)
    'Drums/808s/808 Long 140bpm C.wav': 'type=drum_oneshot, subtype=kick_808, role=808, style=unlabelled',  # end_cut blank = no verdict (repair 18)
    'Drums/808s/808 Long C.wav': 'type=drum_oneshot, subtype=kick_808, role=808, style=unlabelled, end_cut=0',
    'Drums/808s/808 Very Long C.wav': 'type=drum_oneshot, subtype=kick_808, role=808, style=unlabelled, end_cut=0',
    'Drums/Cymbals/Crash Long.wav': 'type=drum_oneshot, subtype=crash, role=hats, style=unlabelled, end_cut=0',
    'Bass/Sub Bass C1.wav': 'type=oneshot_melodic, subtype=-, role=bass, style=unlabelled, note_tag=C, end_cut=0',
    'Bass/Bass Shot 01.wav': 'type=oneshot_melodic, subtype=-, role=bass, style=unlabelled, end_cut=1',
    'One Shots/Stab Am.wav': 'type=oneshot_melodic, subtype=-, role=chords, style=unlabelled, end_cut=1',
    'One Shots/Chord Stab 01.wav': 'type=oneshot_melodic, subtype=-, role=chords, style=unlabelled, end_cut=1',
    'Synth/Long Pad Cm.wav': 'type=oneshot_melodic, subtype=-, role=pad, style=unlabelled, end_cut=1',
    'Synth/Drone Dark.wav': 'type=oneshot_melodic, subtype=-, role=pad, style=unlabelled, end_cut=1',
}
COLS = ["type", "subtype", "role", "style", "bpm_tag", "key_tag", "note_tag", "end_cut"]
chk = Check("b1_names")
for i, (rel, x) in enumerate(cases):
    rows, con, code = one("b1", i, rel, x)
    show(rel, rows, COLS, con, code)
    if not chk.run(rel, rows, code):
        continue
    want = WANT[rel] if isinstance(WANT[rel], list) else [WANT[rel]]
    if not chk.eq(rel + " rows", len(rows), len(want)):
        continue
    for r, w in zip(rows, want):
        w = dict(kv.split("=", 1) for kv in w.split(", "))
        for c in COLS:
            chk.eq("%s %s" % (rel, c), r.get(c, ""), w.get(c, ""))
chk.clean_repo()
chk.done()
