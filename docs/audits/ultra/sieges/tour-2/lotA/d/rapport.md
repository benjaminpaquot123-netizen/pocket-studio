siege: d | profil: DJ club | lang: en | device: phone | font: 100 | mode: pro
mission: exports | buts atteints: 2 sur 3
hesitations:
  - ecran: 9 | cherchais: the master / loudness target | regarde: Mix tab top, only track faders; target is 5 swipes down under "Master volume"
  - ecran: 11 | cherchais: loudness target in the Share sheet | regarde: Share sheet, no target shown, only M4A/WAV and "More exports"
  - ecran: 21 | cherchais: Export button after setting the target | regarde: long scroll through pro master, A/B, reverb, echo (5 swipes) before EXPORT
surprise: club and streaming exports came out with the exact same numbers (−12.9 LUFS, −12.0 dBTP), only the "gap" line changed
abandon: j'aurais abandonné à l'écran 47 parce que the target changes nothing in the file, so I can't trust a "club version"
defauts:
  - ecran: 31 | cible: report « −12.9 LUFS integrated · peak −12.0 dBTP » / « Gap from the target: −5.4 LU » | geste: touche
    attendu: club export near −7.5 LUFS, as promised "gets as close as it can" (11 dB of peak room left) | obtenu: −12.9 LUFS, 5.4 LU short, same as streaming export
    symptome: incompris | gravite: grave | bloque: oui
  - ecran: 31 | cible: « Length: 3 s » | geste: touche
    attendu: about 5:20 (whole song chosen, Share sheet says 5:20) | obtenu: 3 s on all three exports (31, 47, 55)
    symptome: coupe | gravite: grave | bloque: non
  - ecran: 31 | cible: « Level: like streaming music (−14) » | geste: touche
    attendu: club level wording for a club export | obtenu: streaming wording, while the line below says target Club −7.5 and measure −12.9
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 47 | cible: « Gap from the target: +1.1 LU » (Streaming −14) | geste: touche
    attendu: output brought down to −14 ("rises or falls gently") | obtenu: left 1.1 LU too loud, file identical to club one
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 31 | cible: « peak −12.0 dBTP » with « −12.9 LUFS integrated » | geste: touche
    attendu: true peak well above integrated loudness for techno | obtenu: peak only 0.9 dB above LUFS, numbers not believable
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 17 | cible: help « Loudness target » « Club (−8 LUFS) » | geste: touche
    attendu: same value as the chip | obtenu: chip says « Club · −7.5 LUFS », help says −8: two values for one target
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 31 | cible: « ✓ Ready to share » (green) | geste: touche
    attendu: warning when 5.4 LU off the target | obtenu: green "ready" right above a yellow gap warning
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 45 | cible: « Cancel » during export | geste: attends
    attendu: progress or "exporting…" text | obtenu: only greyed options and Cancel for 30–120 s, no progress
    symptome: sans-aide | gravite: mineur | bloque: non
  - ecran: 55 | cible: « Saved in Music › BenStudio: Techno-song-streaming-…m4a » | geste: touche
    attendu: file name and folder readable by TalkBack, same on WAV | obtenu: line visible but absent from screen text; WAV reports (31, 47) say "Ready: name" with no folder
    symptome: sans-nom | gravite: mineur | bloque: non
  - ecran: 41 | cible: Share sheet « M4A · for sharing » [selected] | geste: touche
    attendu: same format as the Mix export (WAV) and the active target shown | obtenu: M4A preselected here, WAV in Mix; no loudness target mentioned
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 50 | cible: « Final file: 48 kHz · 16 bits » with M4A | geste: touche
    attendu: bitrate for M4A (bits per sample means nothing for AAC) | obtenu: "16 bits"; report says only "M4A, 48 kHz"
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 16 | cible: « Auto (style) · −7.5 LUFS » and « Club · −7.5 LUFS » | geste: touche
    attendu: clear difference between the two chips | obtenu: same value, only the hint text differs
    symptome: incompris | gravite: mineur | bloque: non
