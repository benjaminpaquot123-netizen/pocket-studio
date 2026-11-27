# Drum one-shot reference targets ("mine d'or" pack)

Date: 2026-11-26. Source: one commercial drum/sample pack (Google Drive zip, 16.1 GB, 13 800 entries).
Only aggregated statistics are published here: no audio, no file names, no per-file numbers.
Tool: `scripts/sons/mesure.py` (selftest 140/140). Classification and statistics: `scripts/sons/mine_or.py`.
Format of every number: **median [q25–q75]**.

## 1. What could be measured

### Source corruption (not recoverable from this source)
- The zip served by Drive contains three runs of zero bytes (about 6.1–7.2 GB, 10.3–12.3 GB, 13.6–14.8 GB of the file; 4.28 GB in total).
  A direct byte-range request to Drive returns the same zeros, so the file stored on Drive is itself damaged (not a download error).
- 3 537 entries are unreadable (bad local header / CRC). By pack: Pack A (dubstep) 756, Pack B (deathstep) 661, Pack C (rave) 583, Pack D (vocals) 495,
  Pack E (dnb) 406, Pack F (afro house) 398, Pack G (trap melody) 169, Pack H (atmospheres) 69.
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

## Deuxième passe (12 RAR, corrigée le 2026-11-27 après critique)

Source : `docs/sons/passe2/tout/` (12 449 lignes, dont 2 439 coups), tous les chiffres et leur n dans
`docs/sons/passe2/tout/chiffres.md` (rejouable : `python3 -I scripts/sons/passe2/fin/tables.py`), inventaire dans
`docs/sons/passe2/INVENTAIRE.md`, liste des fautes corrigées dans `docs/sons/passe2/CORRECTIONS-CRITIQUE.md`.
Règle de cible : un chiffre n'est une cible que s'il porte au moins 20 sons **et** 2 packs distincts ; sinon « pas de cible ».
Mêmes règles de confiance qu'au §1 ; aberrants retirés par une règle écrite (hauteur hors 20–5 000 Hz, durée de
décroissance plus longue que le fichier, centroïde hors 20 Hz–20 kHz) : seule la grosse caisse en perd (3 et 8 valeurs).

**Archives imbriquées** : lues. RAR 12 : 808 fichiers dans les deux passes (écart 0) ; RAR 11 : +2 sons que la première
passe n'avait pas lus. Le « −22 » de la première version venait de l'archive non ouverte.

| famille | n (packs) | t20 ms | t60 ms | hauteur stable Hz | centroïde Hz | transitoire/corps dB |
|---|---|---|---|---|---|---|
| kick | 515 (14) | 194 [134–238] | 290 [223–369] | 48.5 [43.0–54.7] | 108 [83–177] | −12.5 |
| snare | 412 (12) | 124 [86–167] | 281 [200–603] | 259 [201–446] (fourchette) | 1 988 | −5.7 |
| clap | 299 (13) | 113 [84–167] | 229 [175–332] | sans objet (bruit) | 3 183 | −7.7 |
| perc | 348 (14) | 107 [66–182] | 261 [175–416] | 347 [180–594] (fourchette) | 1 073 | −4.1 |
| hat fermé (mesure) | 290 (14) | — | 134 [92–208] | sans objet | — | — |
| hat ouvert (mesure) | 118 (7) | — | 440 [295–848] | sans objet | — | — |

Ce qui change par rapport à la première version de cette section :
- **Grosse caisse par style**, plus de chiffre global unique : dubstep t20 226 / t60 319 / 47.7 Hz (2 packs), dnb t20 159,
  rave t20 208, lofi t20 140 (2 packs chacun). house, drift-phonk, multi, afro-house, amapiano : un seul pack, pas de cible ;
  techno : t20 et t60 d'un seul pack, pas de cible.
- **kick_808** : 21 lignes sur 23 viennent d'un seul pack : aucune valeur n'est une cible.
- **Charlestons** : 23 % des étiquettes « fermé/ouvert » sont contredites par la mesure ; on lit la mesure (fermé 134 ms,
  ouvert 440 ms de t60), pas l'étiquette (140 et 288 ms).
- **Hauteur** retirée pour clap, hats, shaker, ride, crash, cymbal, snap (bruit). Le remesurage à l'aveugle ne retrouve
  la hauteur que sur la grosse caisse : snare, perc et tom donnent une fourchette, pas une cible.
- **Tonalité estimée inutilisable** : 18 % d'accord exact avec l'étiquette, 5 % de plus avec la même tonique, 0 % sur
  les RAR 5 et 12. Seule l'étiquette compte.
- **Tempo** : il faut le replier (moitié/double). Boucles de batterie : 69 % d'accord brut, **97 %** replié ;
  boucles mélodiques : 55 % → 72 % (RAR 7 : 24 % seulement). Tempo cible par style, pris de l'étiquette :
  dnb 174, dubstep 140, techno 150, house 125, lofi 77 (au moins 2 packs) ; rave 155, amapiano 113, drift-phonk 130 :
  un seul pack, pas de cible.
- 2 040 lignes n'ont aucun style ; les 5 packs sans étiquette restent sans style (aucune preuve suffisante).
