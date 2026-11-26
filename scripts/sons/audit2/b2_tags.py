"""Batch 2: tempo / key / note tags and style from trap names."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *

K, SN = kick(), snare()
ML = chord_loop(120, 2)
DL = drum_loop(120, 2)
PAD = fade(tone(220, 2.0, 0.3), 30)
loops = """Loops/Bass Loop Am 24Bit 96k.wav
Loops/Bass Loop 96 kHz Am.wav
Loops/24Bit 96k/Bass Loop Am.wav
Loops/Bass Loop Am 96kHz24bit.wav
Loops/Bass Loop Am 96-kHz.wav
Loops/Bass Loop Am 96KHZ 24BIT.wav
Loops/Bass Loop Am 96khz_24bit.wav
Loops/Bass Loop Am 24-Bit 96K.wav
Loops/Bass Loop Am 96000Hz.wav
Loops/Bass Loop Am 88.2k.wav
Loops/Bass Loop Am (96k).wav
Loops/Bass Loop Am 96 k.wav
Loops/Bass Loop Am 96 Khz Stereo.wav
Loops/Bass Loop Am 16b 48.wav
Loops/Juno 106 Chords Loop Am.wav
Loops/SH-101 Bass Loop Am.wav
Loops/CS-80 Pad Loop Dm.wav
Loops/Rhodes 73 Loop Cm.wav
Loops/MPC 60 Keys Loop Am.wav
Loops/90's Piano Loop Cm.wav
Loops/80s Synth Loop F#m.wav
Loops/Route 66 Guitar Loop Am.wav
Loops/Loop 100% Wet Am 120.wav
Loops/Loop 85 Am 140.wav
Loops/Bass Loop 64 Am 140.wav
Loops/Loop 140 bpm.wav
Loops/Loop 140bpm Am.wav
Loops/Loop 140-bpm Am.wav
Loops/Loop_140BPM_Am.wav
Loops/Loop BPM 140.wav
Loops/Loop bpm140.wav
Loops/Loop 140 BPM/Loop 01.wav
Loops/140/Loop 01.wav
Loops/140 Am/Loop 01.wav
Loops/Bass Loop 120.wav
Loops/Bass Loop 120 01.wav
Loops/Piano Loop 120 C.wav
Loops/Piano Loop C 120.wav
Loops/Pad Loop F.wav
Loops/Pad Loop Fm.wav
Loops/Pad Loop F#.wav
Loops/Pad Loop - F.wav
Loops/Pad Loop [F].wav
Loops/Pad Loop Key F.wav
Loops/Pad Loop in F.wav
Loops/bass_loop_120_f#m.wav
Loops/bass loop 120 am.wav
Loops/BASS LOOP 120 AM.wav
Loops/BASS LOOP 120 A MIN.wav
Loops/BassLoop120Am.wav
Loops/Bass_Loop_120_Am.wav
Loops/Bass-Loop-120-Am.wav
Loops/120-Am-Bass-Loop.wav
Loops/Loop.120.Am.wav
Loops/Bass Loop 120 CM.wav
Loops/Bass Loop 120 Cmaj.wav
Loops/Bass Loop 120 Bb.wav
Loops/Bass Loop 120 Bbm.wav
Loops/Bass Loop 120 B♭m.wav
Loops/Bass Loop 120 A#m.wav
Loops/Bass Loop 120 A♯m.wav
Loops/Bass Loop 120 Db.wav
Loops/Bass Loop 120 Hm.wav
Loops/Bass Loop – 120 – Am.wav
Loops/Bass Loop 120 Am (2).wav
Loops/Mélodie Loop 120 Am.wav
Loops/Mélodie Loop 120 Am.wav
Loops/Mélodie 120 Am Réverbérée.wav
Boucles/Mélodie 120 Am.wav
Loops/Fm Synth Loop 120.wav
Loops/Bb Trumpet Loop 120.wav
Loops/E Piano Loop 120.wav
Loops/Synth Loop 120 A.wav
Loops/Synth Loop 120 B.wav
Loops/Blues in A 120.wav
Loops/Loop 120 Am 4 Bars.wav
Loops/Loop 210 Am.wav
Loops/Loop 58 Am.wav
Loops/Loop 174 F#m.wav
Loops/Trap Loop 01 [140] [Am].wav
Loops/Am/Loop 120.wav
Loops/Loop 120 A minor.wav
Loops/Loop 120 Aminor.wav
Loops/Loop 120 A-Minor.wav
Loops/Loop 120 Am7.wav
Loops/Loop 120 Asus4.wav
Loops/Loop 120 Dmaj.wav
Loops/Loop 120 D Major.wav
Loops/Loop 120 Dbmaj7.wav
Loops/Loop 120 C#.wav
Loops/Loop 120 C# m.wav
Loops/Loop 120 Cm_Dry.wav
Loops/Loop 120 G.wav
Loops/Loop 120 Gm Wet.wav""".split("\n")
dloops = """Drum Loops/Drum Loop 128.wav
Drum Loops/Drum Loop 128 01.wav
Drum Loops/Drum Loop 128 bpm.wav
Drum Loops/128 Drum Loop.wav
Drum Loops/Top Loop 128 A.wav
Drum Loops/Drum Loop 120 B.wav
Drum Loops/Drum Loop BPM 140.wav
Drum Loops/140/Loop 01.wav
Drum Loops/140 BPM/Loop 01.wav
Drum Loops/Loop 01 - 140.wav
Drum Loops/DrumLoop_140.wav
Drum Loops/Hat Loop 140 C.wav
Drum Loops/Kick Loop 125 Gm.wav""".split("\n")
shots = """Keys/Keys E Piano.wav
Keys/E Piano 01.wav
Bass/E Bass 01.wav
Bass/A Bass 01.wav
Synth/G Funk Lead 01.wav
Synth/Stab A1.wav
Synth/Lead B2.wav
Synth/Pluck F#3.wav
Synth/Pluck Gb2.wav
Synth/Lead c3.wav
Synth/Lead C-1.wav
Synth/Chord Am7.wav
Synth/Chord Am.wav
Synth/Pad Am.wav
Synth/Pad A min.wav
Synth/Pad C3 Wet.wav
Synth/Pad Soft F.wav
Synth/Bass C.wav
Synth/Bass A#.wav
Synth/Bass 120 C.wav
Synth/Bass 01 A.wav
Synth/Plan B Lead.wav
Synth/Synth 1 C.wav
Synth/Lead 1 C3.wav
Synth/Lead C 3.wav
Synth/Lead_C_3.wav
Synth/Lead C3 127.wav
Synth/Lead C3 v2.wav
Synth/Pluck Db4.wav
Synth/Pluck D#4.wav
Synth/Pluck Ds4.wav
Synth/Pluck D4 Dry.wav""".split("\n")
dshots = """Kicks/Kick Hard A.wav
Kicks/Kick A.wav
Kicks/Kick C.wav
Kicks/Kick F#.wav
Kicks/Kick 01 G.wav
Snares/Snare D.wav
Snares/Snare B 2.wav
Kicks/Kick E Dry.wav""".split("\n")
styles = """Drums/Snares/Gravel Snare 01.wav
Synth/Brave Lead C3.wav
Synth/Horn Blast C3.wav
Synth/Modern Bass C2.wav
Drums/Fills/Solo Fill 01.wav
FX/Entrance FX 01.wav
Drums/Kicks/Warehouse Kick 01.wav
Drums/Kicks/Strap Kick 01.wav
Synth/Technology Lead C3.wav
Drums/Kicks/Popcorn Kick 01.wav
Electronic Drums/Kick 01.wav
Jungle Percussion/Conga 01.wav
Drums/Kicks/Garage Door Kick 01.wav
Drums/Snares/Soulful Snare 01.wav
Drums/Kicks/Trapdoor Kick 01.wav
Drums/Kicks/Contrapunto Kick 01.wav
Drums/Kicks/Extra Punch Kick 01.wav
Drums/Kicks/Travel Kick 01.wav
Drums/Kicks/Ultra Phat Kick 01.wav
Drums/Kicks/Pop Kick 01.wav
Deep House/Kicks/Kick 01.wav
Techno/House Kicks/Kick 01.wav
Hip-Hop & Trap/Kicks/Kick 01.wav
Lo-Fi House/Kicks/Kick 01.wav
UK Garage/Kicks/Kick 01.wav
Drum & Bass/Kicks/Kick 01.wav
DnB/Kicks/Kick 01.wav
RnB/Kicks/Kick 01.wav
Rock/Kicks/Kick 01.wav
Reggaeton/Kicks/Kick 01.wav
Drill/Kicks/Kick 01.wav
Disco/Kicks/Kick 01.wav
Funk/Kicks/Kick 01.wav
Hardstyle/Kicks/Kick 01.wav""".split("\n")
# Expected cells in play order (rel, 'k=v | k=v'); a column not listed must be empty.
WANT = [
    ('Loops/Bass Loop Am 24Bit 96k.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop 96 kHz Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/24Bit 96k/Bass Loop Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96kHz24bit.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96-kHz.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96KHZ 24BIT.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96khz_24bit.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 24-Bit 96K.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96000Hz.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 88.2k.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am (96k).wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96 k.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 96 Khz Stereo.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop Am 16b 48.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Juno 106 Chords Loop Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/SH-101 Bass Loop Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/CS-80 Pad Loop Dm.wav', 'type=melodic_loop | key_tag=D minor'),
    ('Loops/Rhodes 73 Loop Cm.wav', 'type=melodic_loop | key_tag=C minor'),
    ('Loops/MPC 60 Keys Loop Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ("Loops/90's Piano Loop Cm.wav", 'type=melodic_loop | key_tag=C minor'),
    ('Loops/80s Synth Loop F#m.wav', 'type=melodic_loop | key_tag=F# minor'),
    ('Loops/Route 66 Guitar Loop Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Loop 100% Wet Am 120.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 85 Am 140.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Bass Loop 64 Am 140.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Loop 140 bpm.wav', 'type=melodic_loop | bpm_tag=140'),
    ('Loops/Loop 140bpm Am.wav', 'type=melodic_loop | bpm_tag=140 | key_tag=A minor'),
    ('Loops/Loop 140-bpm Am.wav', 'type=melodic_loop | bpm_tag=140 | key_tag=A minor'),
    ('Loops/Loop_140BPM_Am.wav', 'type=melodic_loop | bpm_tag=140 | key_tag=A minor'),
    ('Loops/Loop BPM 140.wav', 'type=melodic_loop | bpm_tag=140'),
    ('Loops/Loop bpm140.wav', 'type=melodic_loop | bpm_tag=140'),
    ('Loops/Loop 140 BPM/Loop 01.wav', 'type=melodic_loop | bpm_tag=140'),
    ('Loops/140/Loop 01.wav', 'type=melodic_loop | bpm_tag=140'),
    ('Loops/140 Am/Loop 01.wav', 'type=melodic_loop | bpm_tag=140 | key_tag=A minor'),
    ('Loops/Bass Loop 120.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Bass Loop 120 01.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Piano Loop 120 C.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=C major'),
    ('Loops/Piano Loop C 120.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Pad Loop F.wav', 'type=melodic_loop'),
    ('Loops/Pad Loop Fm.wav', 'type=melodic_loop | key_tag=F minor'),
    ('Loops/Pad Loop F#.wav', 'type=melodic_loop | key_tag=F# major'),
    ('Loops/Pad Loop - F.wav', 'type=melodic_loop'),
    ('Loops/Pad Loop [F].wav', 'type=melodic_loop'),
    ('Loops/Pad Loop Key F.wav', 'type=melodic_loop'),
    ('Loops/Pad Loop in F.wav', 'type=melodic_loop'),
    ('Loops/bass_loop_120_f#m.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=F# minor'),
    ('Loops/bass loop 120 am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/BASS LOOP 120 AM.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/BASS LOOP 120 A MIN.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/BassLoop120Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Bass_Loop_120_Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Bass-Loop-120-Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/120-Am-Bass-Loop.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop.120.Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Bass Loop 120 CM.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Bass Loop 120 Cmaj.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=C major'),
    ('Loops/Bass Loop 120 Bb.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=Bb major'),
    ('Loops/Bass Loop 120 Bbm.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=Bb minor'),
    ('Loops/Bass Loop 120 B♭m.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=Bb minor'),
    ('Loops/Bass Loop 120 A#m.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A# minor'),
    ('Loops/Bass Loop 120 A♯m.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A# minor'),
    ('Loops/Bass Loop 120 Db.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=Db major'),
    ('Loops/Bass Loop 120 Hm.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Bass Loop – 120 – Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Bass Loop 120 Am (2).wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Mélodie Loop 120 Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Mélodie Loop 120 Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Mélodie 120 Am Réverbérée.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Boucles/Mélodie 120 Am.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Fm Synth Loop 120.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Bb Trumpet Loop 120.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/E Piano Loop 120.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Synth Loop 120 A.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Synth Loop 120 B.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Blues in A 120.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 120 Am 4 Bars.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 210 Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Loop 58 Am.wav', 'type=melodic_loop | key_tag=A minor'),
    ('Loops/Loop 174 F#m.wav', 'type=melodic_loop | bpm_tag=174 | key_tag=F# minor'),
    ('Loops/Trap Loop 01 [140] [Am].wav', 'type=melodic_loop | bpm_tag=140 | key_tag=A minor'),
    ('Loops/Am/Loop 120.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 120 A minor.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 120 Aminor.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 120 A-Minor.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 120 Am7.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=A minor'),
    ('Loops/Loop 120 Asus4.wav', 'type=melodic_loop | bpm_tag=120'),
    ('Loops/Loop 120 Dmaj.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=D major'),
    ('Loops/Loop 120 D Major.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=D major'),
    ('Loops/Loop 120 Dbmaj7.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=Db major'),
    ('Loops/Loop 120 C#.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=C# major'),
    ('Loops/Loop 120 C# m.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=C# minor'),
    ('Loops/Loop 120 Cm_Dry.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=C minor'),
    ('Loops/Loop 120 G.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=G major'),
    ('Loops/Loop 120 Gm Wet.wav', 'type=melodic_loop | bpm_tag=120 | key_tag=G minor'),
    ('Drum Loops/Drum Loop 128.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/Drum Loop 128 01.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/Drum Loop 128 bpm.wav', 'type=drum_loop | bpm_tag=128 | bpm_est=120'),
    ('Drum Loops/128 Drum Loop.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/Top Loop 128 A.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/Drum Loop 120 B.wav', 'type=drum_loop | bpm_tag=120 | bpm_est=120'),
    ('Drum Loops/Drum Loop BPM 140.wav', 'type=drum_loop | bpm_tag=140 | bpm_est=120'),
    ('Drum Loops/140/Loop 01.wav', 'type=drum_loop | bpm_tag=140 | bpm_est=120'),
    ('Drum Loops/140 BPM/Loop 01.wav', 'type=drum_loop | bpm_tag=140 | bpm_est=120'),
    ('Drum Loops/Loop 01 - 140.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/DrumLoop_140.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/Hat Loop 140 C.wav', 'type=drum_loop | bpm_est=120'),
    ('Drum Loops/Kick Loop 125 Gm.wav', 'type=drum_loop | bpm_tag=125 | key_tag=G minor | bpm_est=120'),
    ('Keys/Keys E Piano.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Keys/E Piano 01.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Bass/E Bass 01.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Bass/A Bass 01.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/G Funk Lead 01.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Stab A1.wav', 'type=oneshot_melodic | note_tag=A | note_measured=A3'),
    ('Synth/Lead B2.wav', 'type=oneshot_melodic | note_tag=B | note_measured=A3'),
    ('Synth/Pluck F#3.wav', 'type=oneshot_melodic | note_tag=F# | note_measured=A3'),
    ('Synth/Pluck Gb2.wav', 'type=oneshot_melodic | note_tag=Gb | note_measured=A3'),
    ('Synth/Lead c3.wav', 'type=oneshot_melodic | note_tag=C | note_measured=A3'),
    ('Synth/Lead C-1.wav', 'type=oneshot_melodic | note_tag=C | note_measured=A3'),
    ('Synth/Chord Am7.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Chord Am.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Pad Am.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Pad A min.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Pad C3 Wet.wav', 'type=oneshot_melodic | note_tag=C | note_measured=A3'),
    ('Synth/Pad Soft F.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Bass C.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Bass A#.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Bass 120 C.wav', 'type=oneshot_melodic | note_measured=A3'),  # one-shot with a tempo (repair 2): a note, no key; a lone letter may be a variation, so no note_tag
    ('Synth/Bass 01 A.wav', 'type=oneshot_melodic | note_tag=A | note_measured=A3'),
    ('Synth/Plan B Lead.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Synth 1 C.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Lead 1 C3.wav', 'type=oneshot_melodic | note_tag=C | note_measured=A3'),
    ('Synth/Lead C 3.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Lead_C_3.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Lead C3 127.wav', 'type=oneshot_melodic | note_tag=C | note_measured=A3'),
    ('Synth/Lead C3 v2.wav', 'type=oneshot_melodic | note_tag=C | note_measured=A3'),
    ('Synth/Pluck Db4.wav', 'type=oneshot_melodic | note_tag=Db | note_measured=A3'),
    ('Synth/Pluck D#4.wav', 'type=oneshot_melodic | note_tag=D# | note_measured=A3'),
    ('Synth/Pluck Ds4.wav', 'type=oneshot_melodic | note_measured=A3'),
    ('Synth/Pluck D4 Dry.wav', 'type=oneshot_melodic | note_tag=D | note_measured=A3'),
    ('Kicks/Kick Hard A.wav', 'type=drum_oneshot | subtype=kick | note_measured=G#1'),
    ('Kicks/Kick A.wav', 'type=drum_oneshot | subtype=kick | note_measured=G#1'),
    ('Kicks/Kick C.wav', 'type=drum_oneshot | subtype=kick | note_measured=G#1'),
    ('Kicks/Kick F#.wav', 'type=drum_oneshot | subtype=kick | note_measured=G#1'),
    ('Kicks/Kick 01 G.wav', 'type=drum_oneshot | subtype=kick | note_measured=G#1'),
    ('Snares/Snare D.wav', 'type=drum_oneshot | subtype=snare | note_measured=G#1'),
    ('Snares/Snare B 2.wav', 'type=drum_oneshot | subtype=snare | note_measured=G#1'),
    ('Kicks/Kick E Dry.wav', 'type=drum_oneshot | subtype=kick | note_measured=G#1'),
    ('Drums/Snares/Gravel Snare 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Synth/Brave Lead C3.wav', 'type=oneshot_melodic | style=unlabelled'),
    ('Synth/Horn Blast C3.wav', 'type=oneshot_melodic | style=unlabelled'),
    ('Synth/Modern Bass C2.wav', 'type=oneshot_melodic | style=unlabelled'),
    ('Drums/Fills/Solo Fill 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('FX/Entrance FX 01.wav', 'type=fx | style=unlabelled'),
    ('Drums/Kicks/Warehouse Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Strap Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Synth/Technology Lead C3.wav', 'type=oneshot_melodic | style=unlabelled'),
    ('Drums/Kicks/Popcorn Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Electronic Drums/Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Jungle Percussion/Conga 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Garage Door Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Snares/Soulful Snare 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Trapdoor Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Contrapunto Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Extra Punch Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Travel Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Ultra Phat Kick 01.wav', 'type=drum_oneshot | style=unlabelled'),
    ('Drums/Kicks/Pop Kick 01.wav', 'type=drum_oneshot | style=pop'),
    ('Deep House/Kicks/Kick 01.wav', 'type=drum_oneshot | style=house'),
    ('Techno/House Kicks/Kick 01.wav', 'type=drum_oneshot | style=house'),
    ('Hip-Hop & Trap/Kicks/Kick 01.wav', 'type=drum_oneshot | style=trap'),
    ('Lo-Fi House/Kicks/Kick 01.wav', 'type=drum_oneshot | style=house'),
    ('UK Garage/Kicks/Kick 01.wav', 'type=drum_oneshot | style=garage'),
    ('Drum & Bass/Kicks/Kick 01.wav', 'type=drum_oneshot | style=dnb'),
    ('DnB/Kicks/Kick 01.wav', 'type=drum_oneshot | style=dnb'),
    ('RnB/Kicks/Kick 01.wav', 'type=drum_oneshot | style=rnb'),
    ('Rock/Kicks/Kick 01.wav', 'type=drum_oneshot | style=rock'),
    ('Reggaeton/Kicks/Kick 01.wav', 'type=drum_oneshot | style=reggaeton'),
    ('Drill/Kicks/Kick 01.wav', 'type=drum_oneshot | style=drill'),
    ('Disco/Kicks/Kick 01.wav', 'type=drum_oneshot | style=disco'),
    ('Funk/Kicks/Kick 01.wav', 'type=drum_oneshot | style=funk'),
    ('Hardstyle/Kicks/Kick 01.wav', 'type=drum_oneshot | style=hardstyle'),
]
# Defects still in the tool (expected red, see docs/sons/REJEUX-OUTIL2.md): the true value. None left:
# 'Kick Loop 125 Gm' keeps the key written in full on its own name (a tuned kick loop; defect 8 was the
# lone variation letter, 'Hat Loop 140 C', which stays blank).
TRUE = {}
chk = Check("b2_tags")
i = 0
for group, snd, cols in ((loops, ML, ["type", "bpm_tag", "key_tag"]), (dloops, DL, ["type", "bpm_tag", "key_tag", "bpm_est"]),
                         (shots, PAD, ["type", "note_tag", "note_measured", "bpm_tag", "key_tag"]),
                         (dshots, K, ["type", "subtype", "note", "note_measured", "key_tag"]),
                         (styles, K, ["type", "style"])):
    for rel in group:
        rel = rel.encode().decode("unicode_escape").encode("latin-1").decode("utf-8") if "\\u" in rel else rel
        i += 1
        rows, con, code = one("b2", i, rel, snd)
        show(rel, rows, cols, con, code)
        wrel, wtxt = WANT[i - 1]
        if not chk.run(rel, rows, code):
            continue
        if not chk.eq(rel + " rows", len(rows), 1):
            continue
        w = dict(kv.split("=", 1) for kv in wtxt.split(" | "))
        w.update(TRUE.get(rel, {}))
        for c in cols:
            chk.eq("%s %s" % (rel, c), rows[0].get(c, ""), w.get(c, ""))
chk.clean_repo()
chk.done()
