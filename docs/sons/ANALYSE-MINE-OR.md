# Drum one-shot reference targets ("mine d'or" pack)

Date: 2026-11-26. Source: one commercial drum/sample pack (Google Drive zip, 16.1 GB, 13 800 entries).
Only aggregated statistics are published here: no audio, no file names, no per-file numbers.
Tool: `scripts/sons/mesure.py` (selftest 140/140). Classification and statistics: `scripts/sons/mine_or.py`.
Format of every number: **median [q25–q75]**.

## 1. What could be measured

### Source corruption (not recoverable from this source)
- The zip served by Drive contains three runs of zero bytes (about 6.1–7.2 GB, 10.3–12.3 GB, 13.6–14.8 GB of the file; 4.28 GB in total).
  A direct byte-range request to Drive returns the same zeros, so the file stored on Drive is itself damaged (not a download error).
- 3 537 entries are unreadable (bad local header / CRC). By pack: Riddim Riot 756, Deathstep 661, Neo Rave 583, Exclusive Vocals 495,
  Mainstage DnB 406, Afro House 398, Trap Melody Kits 169, Atmospheres 69.
- Drum one-shots found: 2 380 (+3 drum-looking files that matched no family). **Kept 1 649, lost 731.**

| family | kept | lost | lost by style |
|---|---|---|---|
| kick | 352 | 145 | dubstep 80, rave 30, dnb 25, afro-house 10 |
| kick_808 | 21 | 0 | |
| snare | 302 | 105 | dubstep 60, dnb 35, afro-house 10 |
| clap | 149 | 130 | dubstep 80, dnb 20, rave 20, afro-house 10 |
| hat (unlabelled) | 136 | 75 | dnb 30, dubstep 20, rave 15, afro-house 10 |
| hat_closed | 69 | 0 | |
| hat_open | 80 | 41 | dubstep 20, rave 11, afro-house 10 |
| ride | 75 | 30 | dubstep 20, dnb 10 |
| crash | 26 | 35 | dubstep 20, dnb 10, rave 5 |
| cymbal (china etc.) | 20 | 0 | |
| tom | 44 | 10 | afro-house 10 |
| perc | 225 | 120 | dubstep 60, dnb 30, afro-house 30 |
| shaker / tambourine | 66 | 40 | dubstep 30, afro-house 10 |
| cowbell | 67 | 0 | |
| snap | 12 | 0 | |
| rim | 5 | 0 | |

Consequences: **dubstep and afro-house have no drum one-shot left**; dnb is mostly the liquid kit; rave is partial;
the pack has **no trap / hip-hop drum one-shots** at all (only a set of 808 one-shots). Style labels come from the kit names:
techno, house, amapiano, dnb, rave, lofi, drift-phonk, and "multi" (a large multi-genre construction set with acoustic,
synth, foley, lofi, distorted sub-folders).

### Trust rules applied to every column
- `t20` is dropped when the file ends above −35 dB (end_level_db), `t40` above −50 dB, `t60` above −60 dB: the tail is cut
  or noisy and the decay fit is not trustworthy. Many techno files (100 / 400 ms long) and the rave rides are cut this way.
- `pitch_*`, `h2`, `h3` are dropped when `tonal_db < −10` (noise-like sound: the pitch read is meaningless; e.g. acoustic hats
  read ~340 Hz "pitch").
- Fixed lengths (120, 387, 429, 480, 500, 531 ms…) are sample padding with digital silence (end ≈ −140 dB): `dur_ms` is
  **not** a target, decays are.
- `t20_ms` is the time at which the Schroeder curve crosses −20 dB (it includes the 0…−5 dB body plateau). It is **not** an ISO T20
  slope: a −5…−25 dB slope fit gives ~35 % shorter values on kicks (82 vs 126 ms on a sample). Synth targets must use the same
  definition (`mesure.py`).
- `t60_ms` tracks the file length (log-correlation 0.81 kicks, 0.92 snares, 0.98–1.00 hats; t60 ≈ 0.76–0.97 × length): it mostly
  measures where the pack trimmed or faded the file. **Use t20 as the decay target; t60 only as a loose upper bound.**
- `centroid_hz` is the **power-weighted** centroid of `mesure.py`. Never compare it with a magnitude-weighted centroid.
- Steady pitch of gliding kicks: the median is right (re-measured 48 vs 51 Hz) but single files are noisy (58 % differ by > 10 %;
  the lowest kicks read too high, e.g. 43 instead of 29 Hz, the window keeps the end of the glide). Attack pitch ±5–15 %, attack pitch ±5–15 %; h2/h3 were not cross-checked on real files.

### Excluded outliers: 35 of 1 649 (2.1 %)
| reason | n |
|---|---|
| kick not low/tonal (centroid > 1.5 kHz or tonal < −10 dB) | 2 |
| hat/shaker with centroid < 2.5 kHz (a kick filed as a hat, low perc) | 4 |
| snare with centroid < 500 Hz | 5 |
| snap with centroid < 1 kHz | 3 |
| > 3 IQR (log scale) from its family × style median, centroid or t20 | 21 |

## 2. Targets by family (after exclusions and trust rules)

| family | n | t20 ms | t60 ms | pitch attack → steady Hz | tonal dB | centroid Hz | hf dB | transient/body dB |
|---|---|---|---|---|---|---|---|---|
| kick | 345 | 168 [122–237] | 283 [210–408] | 216 [147–264] → 48.8 [43.5–55.5] | −0.1 | 109 [83–185] | −40 [−54…−31] | −12.1 [−14.8…−8.4] |
| kick_808 | 21 | 1430 [1104–1784] | 1788 [1522–2200] | 214 → 36.9 [36.6–55.0] | 0.0 | 46 [39–61] | −88 | −23.4 [−30…−20] |
| snare | 297 | 118 [78–150] | 273 [187–589] | 666 → 277 [202–476] | −8.2 | 2044 [1403–2882] | −15 | −4.2 [−7.0…−1.9] |
| clap | 147 | 91 [75–118] | 203 [164–236] | (noise) | −11.1 | 4288 [2954–6555] | −11 | −6.3 [−8.0…−4.2] |
| hat (unlabelled) | 131 | 68 [45–90] | 154 [89–491] | (noise) | −25 | 7369 [6614–8288] | −6.8 | −5.4 |
| hat_closed | 69 | 55 [44–61] | (see 3) | (noise) | −25 | 9381 [8135–10554] | −3.7 | −0.5 |
| hat_open | 79 | 133 [85–680] | 1151 [261–1962] | (noise) | −28 | 8487 [7572–10595] | −5.1 | −8.3 |
| ride | 74 | 309 [262–388] | 645 [520–778] | (noise) | −22 | 9514 [8077–10644] | −3.7 | −8.6 |
| crash | 25 | 1002 [519–1387] | 1791 [1260–2104] | (noise) | −20 | 6805 [6337–7380] | −8.5 | −20.1 |
| cymbal | 19 | 552 [456–715] | 984 [912–1045] | (noise) | −15 | 5689 [5184–6174] | −10.6 | −12.1 |
| tom | 44 | 377 [207–491] | 946 [472–1150] | 225 → 78 [65–115] | −1.7 | 697 [238–1087] | −34 | −4.9 |
| perc | 219 | 87 [57–137] | 215 [160–344] | 823 → 470 [266–872] | −4.7 | 1258 [665–2136] | −31 | −3.3 |
| shaker | 65 | 48 [33–82] | 557 [490–667] | (noise) | −20 | 8935 [6750–10189] | −5.4 | −0.4 |
| cowbell | 65 | 362 [64–592] | 1481 [885–1830] | 797 → 625 [402–764] | −2.5 | 834 [727–1092] | −43 | −9.6 |
| snap | 9 | 230 [208–292] | 818 [740–1632] | (noise) | −10 | 3201 [2945–4212] | −15 | −7.9 |
| rim | 5 | 56 [32–61] | 323 | 1500 → 1018 | −8.0 | 1479 [1231–1905] | −34 | +3.7 |

Mono loss is negligible for kicks/808/snares (≈ 0 dB); hats, rides, crashes and shakers are stereo (−0.5 to −3 dB).

## 3. By style and sub-type (groups with n ≥ 9)

### Kicks
| group | n | t20 ms | t60 ms | centroid Hz | hf dB | transient/body dB |
|---|---|---|---|---|---|---|
| house | 20 | 235 [218–250] | 310 [289–365] | 105 | −48 | −13.9 |
| techno | 30 | 215 [199–219] | 253 (few, most cut) | 135 | −38 | −14.5 |
| dnb | 28 | 125 [110–148] | 211 [194–272] | 98 | −38 | −9.4 |
| drift-phonk | 39 | 97 [85–113] | 155 [142–173] | 67 | −80 | −14.3 |
| lofi | 10 | 147 | 227 | 86 | −83 | −12.2 |
| multi: synth hard | 69 | 197 [157–347] | 341 [228–699] | 90 | −38 | −14.8 |
| multi: synth soft | 45 | 166 [126–195] | 246 [194–295] | 199 | −31 | −11.6 |
| multi: distorted | 43 | 247 [228–272] | 370 [332–431] | 278 | −27 | −11.2 |
| multi: acoustic | 20 | 169 [131–293] | 682 [494–826] | 125 | −47 | −5.1 |
| multi: foley | 16 | 142 | 485 | 239 | −38 | −4.2 |
| multi: lofi | 18 | 122 | 379 | 105 | −46 | −6.7 |

Two clear sub-types: **electronic punchy kick** (t20 100–250 ms, centroid 70–140 Hz, transient/body −12 to −15 dB, almost
no air) and **acoustic/foley kick** (longer low ring t60 500–700 ms, brighter attack: transient/body −4 to −5 dB).
Distorted kicks: centroid ~280 Hz and hf −27 dB. Drift-phonk/lofi kicks are short and dark (hf ≈ −80 dB).

### Snares
| group | n | t20 ms | t60 ms | centroid Hz | hf dB | transient/body dB |
|---|---|---|---|---|---|---|
| dnb | 30 | 57 [50–69] | 173 [140–199] | 3464 | −12.5 | −3.2 |
| rave | 20 | 159 | 267 | 1626 | −13.9 | −8.5 |
| drift-phonk | 20 | 124 | 242 | 2479 | −16.7 | −6.9 |
| lofi | 10 | 37 | 169 | 1392 | −26.6 | −1.5 |
| techno | 9 | 63 | 186 | 804 | −20.6 | +0.1 |
| multi: hard | 45 | 130 [101–156] | 265 [206–307] | 1882 | −13.8 | −9.1 |
| multi: soft | 35 | 100 [85–116] | 187 [155–213] | 2446 | −13.6 | −6.2 |
| multi: neuro | 41 | 119 | 616 [557–654] | 2404 | −12.2 | −3.2 |
| multi: acoustic | 24 | 168 | 734 [545–883] | 1057 | −25.9 | −1.0 |
| multi: foley | 33 | 95 | 237 | 2101 | −15.0 | −1.8 |
| multi: lofi | 24 | 170 | 406 | 1564 | −21.3 | −4.4 |

Electronic snares: t60 170–270 ms; acoustic snares ring ~3× longer and are an octave darker.

### Claps
House 2.9 kHz, techno 3.8 kHz, dnb 3.3 kHz, drift-phonk 2.7 kHz centroid; the multi set is much brighter (6.9 kHz, hf −6 dB).
Decay is consistent everywhere: t20 70–120 ms, t60 150–215 ms.

### Hats and cymbals
- Closed hat: t20 40–55 ms is the perceptual decay. In the multi set only, the t60 reads ~410 ms because of a long low-level
  **high-frequency** tail (level vs peak −25/−45/−55/−61 dB at 50/100/200/300 ms; unchanged by a 2 kHz high-pass): a fade or
  ambience, not part of the hit (dnb closed hats: t60 92 ms). **Use t20, not t60, as the closed-hat target.**
- Unlabelled hats behave like closed hats in house/techno/drift-phonk (t60 74–175 ms) but the multi acoustic hats are long
  (t60 ~700 ms): treat them as a mixed bag.
- Open hat is bimodal: drift-phonk open hats are long (t60 ~1.8 s on the 17/30 files with a clean tail, ~1.1 s over all files,
  P10–P90 0.6–2.2 s; centroid 10.5 kHz); the multi set ones are short
  (t60 ~200 ms) and often cut (end ≈ −52 dB).
- Ride: t20 300–340 ms, t60 630–950 ms; rave rides are cut at 387 ms (no decay value). Crash: t60 1.3 s (dnb) to 2.0 s (multi).

### Other
- Toms (acoustic): t60 ~1.0 s, steady pitch ~80 Hz.
- Perc: very heterogeneous (centroid 400 Hz amapiano to 2.4 kHz techno); use per-instrument references, not the family median.
- Cowbell is bimodal: drift-phonk (long 808-style, t60 1.5 s, pitch ~420 Hz) vs multi (short, t60 ~200 ms, pitch ~700 Hz).

## 4. Blind critique (fresh sub-agent, own numpy code, 25 random files per family)
| check | its value | tool value | verdict |
|---|---|---|---|
| kick −20 dB crossing | 126 ms | 126 ms | identical; but a slope fit gives 82 ms → definition clarified (§1) |
| kick steady pitch (FFT peak 100–300 ms) | 48 Hz | 51 Hz | median OK, per-file noisy → caveat added |
| clap power centroid | 3383 Hz | 3383 Hz | identical |
| closed hat t20 / t60 | 59 / 409 ms | 59 / 409 ms | identical; tail is HF, not low → wording fixed |
| misclassified (spectral) | 1–3 % per family | 35 flags | mostly caught |
| its own 3-IQR outliers | 48 files | 35 | same order; 1.5 IQR → 178 (too many) |

What changed after the critique:
1. t60 demoted to an upper bound (it follows the file length); t20 is the decay target, with its exact definition.
2. Closed-hat tail described as a high-frequency fade, not a low tail.
3. Drift-phonk open hat: 1.8 s only on clean tails, 1.1 s over all files.
4. Kicks are not one target: by style t20 ranges 97 ms (drift-phonk) to 235 ms (house); 217 of 352 kicks come from the multi set.
5. 808 "37 Hz" is the low edge of a 37–55 Hz range (21 files, one set).
6. Acoustic snares' long t60 partly follows their ~1 s file length; compare t20 (168 vs 118 ms) instead.
7. Outlier rule kept, but 17 family × style groups have < 8 files: there, the outlier test is skipped (no fallback yet), so small
   groups may still hold a stray file.


## 5. Limits
- Corruption removed 31 % of the drum one-shots and whole styles (dubstep, afro-house); no trap/hip-hop kit.
- One pack only (a few producers' taste): use these numbers as ranges, not as exact targets.
- Small groups (rim n 5, snap n 9, 808 n 21, cymbal n 19) are indicative only.
- t60 is meaningless for cut or padded files: always read end_level_db first.

## Deuxième passe (12 RAR)

État : 12 RAR sur 12 réunis. Source : `docs/sons/passe2/tout/drum_oneshot.csv` (2 426 coups), tableau complet dans
`docs/sons/passe2/tout/chiffres.md`, inventaire dans `docs/sons/passe2/INVENTAIRE.md`.
Mêmes règles de confiance qu'au §1 ; **pas** de retrait des valeurs aberrantes (le §2 en retirait 35). Médiane [q25–q75].

**Non lu** : le RAR 12 a 22 fichiers de moins qu'en première passe (786 contre 808) : ils sont dans une archive
imbriquée du pack 3 que la deuxième passe n'ouvre pas. Archives imbriquées non lues, par RAR : RAR 11 → 2 (non lues
non plus en première passe, écart 0), RAR 12 → 1, tous les autres → 0. Tous les autres RAR ont le même nombre de
fichiers dans les deux passes.

| famille | n | t20 ms | t60 ms | hauteur attaque → stable Hz | tonal dB | centroïde Hz | hf dB | transitoire/corps dB |
|---|---|---|---|---|---|---|---|---|
| kick | 512 | 194 [134–238] | 290 [223–369] | 222 → 48.2 [42.7–54.5] | 0.0 | 108 [83–177] | −38 | −12.5 [−15.5…−9.8] |
| kick_808 | 23 | 1430 [1084–1802] | 1788 [1499–2267] | 223 → 41.3 [36.6–63.8] | 0.0 | 46 [39–67] | −88 | −23.7 [−30.0…−20.3] |
| snare | 409 | 124 [85–167] | 283 [199–605] | 633 → 259 [201–446] | −7.7 | 1986 [1259–2797] | −15 | −5.7 [−8.7…−2.7] |
| clap | 298 | 112 [84–166] | 229 [174–333] | (bruit) | −9.9 | 3185 [2514–4392] | −14 | −7.7 [−10.4…−5.3] |
| hat_closed | 246 | 55 [44–68] | 140 [92–277] | (bruit) | −26.5 | 8090 [7106–9668] | −6.0 | −2.6 |
| hat_open | 160 | 137 [93–224] | 288 [197–825] | (bruit) | −22.5 | 7620 [6446–9541] | −6.1 | −7.0 |
| ride | 105 | 291 [240–377] | 614 [459–768] | (bruit) | −23.6 | 9029 [7521–10321] | −4.4 | −8.6 |
| crash | 65 | 1087 [772–1379] | 1884 [1280–2308] | (bruit) | −17.6 | 6169 [5012–6848] | −11.0 | −20.1 |
| cymbal | 20 | 550 [446–716] | 981 [891–1066] | (bruit) | −15.4 | 5723 [5163–6362] | −10.2 | −12.2 |
| tom | 54 | 262 [157–470] | 704 [407–1086] | 215 → 97 [67–122] | −1.4 | 566 [166–1037] | −36 | −6.9 |
| perc | 344 | 108 [66–182] | 263 [177–416] | 714 → 351 [181–594] | −4.0 | 1073 [572–1918] | −30 | −4.1 |
| shaker | 106 | 63 [42–95] | 463 [412–584] | (bruit) | −20.7 | 8465 [6280–9629] | −6.2 | −4.8 |
| cowbell | 67 | 345 [63–591] | 1481 [848–1839] | 798 → 625 [402–767] | −2.4 | 837 [727–1120] | −43 | −9.5 |
| snap | 12 | 220 [199–289] | 768 [617–1611] | (bruit) | −4.9 | 3041 [1013–4203] | −15.6 | −8.1 |
| rim | 5 | 56 [31–64] | 323 | 1501 → 1018 | −8.0 | 1479 [999–2354] | −34 | +3.7 |

Familles : préfixe du sous-type avant « : » (les couches « kick:attack », « kick:tail »… vont dans leur famille) ; perc_high,
perc_mid et perc_low vont dans perc. Coups par style et par famille : `docs/sons/passe2/tout/chiffres.md`.

### Ce qui change nettement par rapport à la première passe
- **Plus de « hat » sans étiquette** : tout est hat_closed (69 → 246) ou hat_open (79 → 160). Les cibles hat_closed
  (t20 55 ms, centroïde 8,1 kHz) et hat_open (t20 137 ms) tiennent ; la queue des hat_open est bien plus courte
  (t60 288 contre 1151 ms).
- **Tom** : t20 262 ms contre 377, centroïde 566 Hz contre 697, hauteur stable 97 Hz contre 78 (54 toms au lieu de 44).
- **Clap** : centroïde 3,2 kHz contre 4,3, t20 112 ms contre 91 ; le tonal médian est à −9,9 dB, à la limite de la règle.
- **Perc** : 344 au lieu de 219, t60 263 ms contre 215, centroïde 1,1 kHz contre 1,3, hauteur stable 351 Hz.
- **Snare** : centroïde 2,0 kHz, inchangé ; t20 124 ms contre 118.
- **Kick** : t20 194 ms contre 168 (une partie de l'écart vient de l'absence de retrait des valeurs aberrantes) ; hauteur
  stable presque inchangée (48 Hz contre 49).
- **Shaker** : t20 63 ms, t60 463 ms, 106 coups.
- **Crash** : t20 1087 ms contre 1002. **Kick_808, ride, cymbal, cowbell, snap, rim** : pas d'écart net.
- Nouveaux styles par rapport à la première lecture par style : synthwave (RAR 9, forcé), trance (33 MIDI, par dossier),
  et rave bien plus fourni (157 coups au lieu de 22).
