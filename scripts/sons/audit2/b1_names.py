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
sel = sys.argv[1:] 
for i, (rel, x) in enumerate(cases):
    rows, con, code = one("b1", i, rel, x)
    show(rel, rows, ["type", "subtype", "role", "style", "bpm_tag", "key_tag", "note_tag", "end_cut"], con, code)
