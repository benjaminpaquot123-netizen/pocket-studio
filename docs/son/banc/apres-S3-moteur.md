# Banc de mesure du son : après S3 (partie moteur)

Chantier S3 de `docs/PLAN-SON-PRO.md`, partie moteur : micro-décalages et roulements à rampe, voix 808 dédiée, kick studio court (et sa variante dure), rumble, charleys et clap plus clairs (kit v3), couches de vélocité et alternance d'échantillons sur les pads, fondus du master, solo des pads.

Méthode : `scripts/song-bench.sh` (enregistrement par `SongBenchExport`, rendu `ps_render`, mesure `ps_measure`), 37 styles, sons intégrés. Les recettes de l'assistant ont été réécrites par S1 en parallèle ; pour juger **le moteur seul**, on compare **les mêmes morceaux** (mêmes `.pscmd`) avec deux kits :

1. **Contrôle** : la branche fusionnée (S1 + interface 34 b + moteur S3), enregistrée telle quelle. L'assistant envoie encore le kit v2 (`SetKitVersion 2`) et n'utilise pas encore la 808 ni les décalages.
2. **Kit v3** : les mêmes commandes, le kit passé en v3 (`C 100 3.0`), rendu par `--no-record`. JSON : `apres-S3-moteur.json`.

Le moteur S3 ne change rien aux morceaux existants : sur les enregistrements d'avant S1, le contrôle redonne `apres-etape2` à l'identique (toutes les médianes à +0,0). Les projets sauvés gardent le kit v1 ou v2, et une rampe absente joue comme avant.

## Kit v3 contre kit v2, mêmes morceaux (médianes sur 37 styles)

| Mesure | Kit v2 (contrôle) | Kit v3 | Écart | Min – max (v3) | Cible |
|---|---|---|---|---|---|
| Longueur du kick (−40 dB) | ≥ 1 s (plafond 1,5 s) | **0,32 s** (dur : 0,27 s) | — | — | ≤ 0,35 s |
| Air > 6 kHz (dB rel.) | −20,1 | **−17,4** | +2,7 | −23,8 – −12,7 | −20 à −16 |
| LUFS du Drop / Refrain | −8,7 | −8,8 | −0,2 | −11,8 – −7,3 | −8 à −6 en club |
| LUFS-I (morceau) | −8,3 | −9,1 | −0,8 | −12,0 – −7,0 | cible du genre ±1 |
| True peak (dBTP) | −1,17 | −1,17 | +0,00 | −1,19 – −1,09 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 7,1 | 7,9 | +0,8 | 5,8 – 10,8 | 6 à 10 en club |
| Facteur de crête du Drop (dB) | 7,7 | 8,7 | +0,9 | 6,9 – 10,8 | ≥ 6 |
| LRA (LU) | 1,8 | 3,9 | +2,1 | 0,6 – 13,8 | 3 à 9 en club |
| Côté/centre (dB) | −23,1 | −20,9 | +2,2 | −38,0 – −11,2 | −15 à −9 |
| Pente 0,1–8 kHz (dB/oct) | −4,91 | −4,94 | −0,03 | −6,22 – −3,29 | −4,5 ±0,7 |
| Drop − Break (LU) | −0,2 | −0,7 | −0,5 | −1,8 – 4,7 | ≥ +4 |
| Gain de la cible (dB) | 8,6 | 10,7 | +2,1 | −0,3 – 14,6 | – |
| Perte dans la chaîne (LU) | 4,5 | 6,1 | +1,6 | 0,1 – 6,8 | ≤ 6 (plafond) |

La longueur du kick n'est pas une mesure du banc (le kick partage la voie Batterie avec les autres pads) : elle vient des tests du moteur (`studio_kicks`, `kit_v3`), un coup seul, −40 dB sous sa crête.

| Critère | Kit v2 (contrôle) | Kit v3 |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | 37 sur 37 |
| LUFS-I à ±1 LU de la cible, ou plafonné (bloquant) | 37 sur 37 | 37 sur 37 |
| LUFS-I à ±1 LU de la cible du style | 29 sur 37 | 18 sur 37 (les 19 autres plafonnés) |
| Continu, NaN, échantillons écrêtés | 37 sur 37 | 37 sur 37 |
| Air entre −20 et −16 dB | 15 sur 37 (2 au-dessus) | 21 sur 37 (9 au-dessus, 7 en dessous) |
| PLR ≥ 6 dB | 35 sur 37 (acidcore, gabber) | 35 sur 37 (acidcore, gabber) |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | 37 sur 37 |

Quelques styles (kit v3, écart au contrôle) :

| Style | LUFS-I | Drop | PLR | LRA | Air |
|---|---|---|---|---|---|
| techno | −9,1 (−1,5) | −8,4 | 7,9 (+1,5) | 4,0 (+1,8) | −19,5 (+2,4) |
| house | −9,0 (−0,8) | −8,6 (−0,1) | 7,8 (+0,8) | 3,9 (+2,5) | −17,5 (+3,2) |
| hard_techno | −9,1 (−0,6) | −8,9 (−0,4) | 8,0 (+0,6) | 3,0 (+1,5) | −16,3 (+2,5) |
| gabber | −7,0 (−0,3) | −7,4 (−0,2) | 5,8 (+0,3) | 1,6 (−0,2) | −16,5 (+2,0) |
| jump_up | −9,1 (−0,9) | −8,9 (−0,2) | 7,9 (+1,0) | 6,8 (+4,4) | −14,9 (+3,7) |
| trap | −9,9 (−0,2) | −9,7 (−0,2) | 8,7 (+0,2) | 5,3 (−0,5) | −20,9 (+1,7) |
| drill | −9,3 (+0,1) | −9,2 | 8,2 (−0,1) | 7,0 (−0,5) | −19,8 (+1,4) |
| dnb | −8,2 | −8,7 (+0,1) | 7,0 | 2,3 | −16,0 (+1,6) |

## Ce que ça dit

- **Le kick ne traîne plus** : 0,32 s au lieu de 1,5 s, avec une attaque à 2–5 kHz (−19 dB dans les 20 premières ms, cible ≥ −25) et un corps à 40–80 Hz. Il ne recouvre plus le kick suivant ni la basse.
- **L'air arrive dans la cible** : +2,7 dB de médiane (−17,4), grâce aux charleys (couche de grésillement au-dessus de 7 kHz, charley ouvert de 0,44 s au lieu de 0,05 s) et au clap (quatre claquements, de l'air au-dessus de 3,5 kHz). Chaque voix garde la sonie de son rôle du kit v2 (±0,5 dB, mesuré pondéré K sur un temps à 128 BPM).
- **Plus de dynamique** : facteur de crête du Drop +0,9 dB, PLR +0,8, LRA +2,1, image un peu plus large (+2,2 dB côté/centre).
- **Le prix : 0,8 LU de sonie intégrée** (le Drop ne perd que 0,2), et 19 styles plafonnés au lieu de 8. Ce ne sont pas les crêtes du kick : la même expérience avec les deux kicks du kit v3 à −2 dB donne le même résultat (LUFS-I −9,0, perte 6,1 LU). C'est la queue de 1,5 s du kick v2, une énergie grave continue, qui apportait cette sonie. Le gain de la cible monte de 2 dB pour la remplacer, et la chaîne bute sur son plafond. Il faut remplir ce grave autrement : la 808 en trap, drill et phonk, le rumble en techno, et une basse plus dense (S4). C'est le travail du générateur, pas un réglage du kit.
- **Drop − Break** ne bouge pas (−0,7) : c'est l'étape « courbe d'énergie » (S5), pas la batterie.

## Ce que le générateur et l'interface doivent appeler

- Kit v3 : `NativeEngine.setKitVersion(3)` pour les morceaux de l'assistant (jamais pour un projet sauvé avant).
- Roulements : `setDrumHit(lane, step, velocity, repeats 1..8, ramp)` avec `Hit.ramp` (0 comme avant, 1 montant depuis la moitié, −1 descendant jusqu'à la moitié), puis au besoin `setDrumStepRoll(lane, step, hits 1..16, span 1..4, ROLL_RAMP_*, amount, triplet)` (rafale de 1/32 sur un temps : 8 coups sur 4 pas).
- Micro-timing : `setDrumStepOffset(lane, step, ms)` après le coup (−50..50 ms ; 0 sur le kick, ±2 à 5 ms sur charleys et percussions, caisse en retard en lo-fi et boom bap).
- 808 : `setPadVoice(lane, PAD_VOICE_808)`, puis `setPadVoiceParam(lane, VOICE_TUNE, note MIDI)` (et `VOICE_DECAY`, `VOICE_DRIVE`, `VOICE_TONE`, `VOICE_CLICK`, `VOICE_GLIDE`). La note de chaque coup : `setDrumStepDetail(lane, step, 0f, demi-tons)`. Le glissé : `setDrumStepGlide(lane, step, ms)` (60 à 200 ms).
- Kick studio, kick dur, rumble sur un autre pad : `setPadVoice(lane, PAD_VOICE_KICK_STUDIO | PAD_VOICE_KICK_HARD | PAD_VOICE_RUMBLE)` et `VOICE_TUNE` en Hz, `VOICE_SUB`, `VOICE_CLICK`, `VOICE_DRIVE`, `VOICE_DECAY`, `VOICE_TONE`.
- Fondus du master : la courbe `AutoTarget.masterVolume()` est maintenant entendue (après le limiteur, jamais au-dessus de l'unité).
- Kits échantillonnés : `setPadSample(pad, son de base)` puis `setPadLayer(pad, couche 0..7, vélocité basse, haute 0..127, son)`.
