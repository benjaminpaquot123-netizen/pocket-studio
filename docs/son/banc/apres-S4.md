# Banc de mesure du son : après S4 (synthés larges et vivants)

Chantier S4 de `docs/PLAN-SON-PRO.md` : unisson stéréo du synthé (compatible mono), doublure à l'octave basse, saturation suréchantillonnée, cycle de filtre sur 8 mesures, balayage carré, formant, couche médium des basses de bass music, suréchantillonnage des oscillateurs ; instruments General MIDI et échantillons en stéréo (version de son 2) ; plaque (salle en hip-hop) avec retours égalisés, graves du mélange en mono sous 120 Hz ; potards du synthé et de Plaits automatisables. Côté assistant, `WideMix.apply` (dernière étape de `StarterWizard.build`) règle tout cela par rôle et par style, avec trois courbes : réverbe du lead montée sur la dernière mesure de chaque phrase de 8, vitesse du wobble 1/8 → 1/4 → 1/16 par bloc de 8 (dubstep, riddim), voyelle par bloc (neurofunk).

Méthode : `scripts/song-bench.sh`, 37 styles, sons intégrés.

1. **Avant** : les enregistrements de S1 (`build/bench-s1/cmds`, l'assistant avant S4, kit v2) rejoués par le moteur S4, avec la voie du lead ajoutée (`META lead`, la même piste que dans les morceaux d'après). Les WAV sont **identiques octet pour octet** à ceux du moteur S3 (mêmes SHA-256) : le moteur S4 rend les morceaux d'avant comme avant.
2. **Après** : les morceaux de l'assistant S4 enregistrés à nouveau (`SongBenchExport`), kit v2 comme l'assistant l'envoie aujourd'hui. JSON : `apres-S4.json`.
3. **Après, kit v3** : les mêmes commandes, le kit passé en v3 (`C 100 3.0`), contre le passage kit v3 de S3 (`apres-S3-moteur.json`). JSON : `apres-S4-kit3.json`.

Nouvelle mesure : le **mouvement du lead**. Sa piste rendue seule (`ps_render --solo`), le centroïde spectral (60 Hz–16 kHz) de chaque mesure où il joue (à 30 dB de sa mesure la plus forte), l'écart entre les 10e et 90e centiles en ±% autour de leur milieu (`ps_measure --lead`). Mesure par mesure, les notes d'une phrase se moyennent : il reste le mouvement du filtre, des courbes et de l'arrangement.

## Résumé (médianes sur 37 styles)

| Mesure | Avant (kit v2) | Après (kit v2) | Avant (kit v3, S3) | Après (kit v3) | Cible |
|---|---|---|---|---|---|
| Côté/centre (dB) | −23,1 | **−19,5** | −20,9 | **−16,5** | −15 à −9 |
| Corrélation L/R | 0,99 | 0,98 | 0,98 | 0,96 | 0,4 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | 1,000 | 1,000 | ≥ 0,95 |
| Perte en mono (LU, la pire) | −0,29 | −0,58 | −0,41 | −0,83 | ≥ −1,5 |
| Mouvement du lead (±%) | 16,0 | **19,9** | – | 19,9 | ≥ 15 |
| Grave 60–250 Hz au Drop (dB rel.) | −6,0 | −6,0 | −4,1 | −4,2 | ≥ −6 en bass music |
| LUFS-I | −8,3 | −8,5 | −9,1 | −9,2 | cible ±1 |
| LUFS du Drop | −8,7 | −8,8 | −8,8 | −8,8 | −8 à −6 en club |
| PLR | 7,1 | 7,3 | 7,9 | 8,0 | 6 à 10 |
| Perte dans la chaîne (LU) | 4,5 | 4,2 | 6,1 | 6,0 | ≤ 6 |

| Critère | Avant (v2) | Après (v2) | Après (v3) |
|---|---|---|---|
| Côté/centre entre −15 et −9 dB | 1 sur 37 | 8 sur 37 | 13 sur 37 |
| Corrélation ≤ 0,85 | 0 sur 37 | 1 sur 37 | 4 sur 37 |
| Corrélation < 120 Hz ≥ 0,95 | 37 sur 37 | 37 sur 37 | 37 sur 37 |
| Lead qui bouge d'au moins ±15 % | 17 sur 32 | 18 sur 32 | 18 sur 32 |
| True peak ≤ −1 dBTP, aucun NaN, continu ≤ 0,001 (bloquants) | 37 sur 37 | 37 sur 37 | 37 sur 37 |

Moteur seul (tests `s4_*` d'`engine/host-tests`) :

| Mesure | Avant | Après | Cible |
|---|---|---|---|
| Repliement d'une scie sur Do6 (composante la plus forte, sous le fondamental) | −51,7 dB | **−73,9 dB** (oscillateurs ×2) | ≤ −60 dB |
| Repliement, Do5 suréchantillonné et saturé (×4, anti-repliement) | – | −87,1 dB | ≤ −60 dB |
| Supersaw 7 copies, largeur 1 : côté/centre, corrélation, sous 100 Hz | mono | −5,7 dB ; 0,58 ; 0,985 | −15 à −3 ; 0,3–0,9 ; ≥ 0,95 |
| Somme mono d'un son élargi contre le même sans largeur | – | −0,01 dB | ±1 dB |
| Cycle de 8 mesures (profondeur 0,5) : centroïde | fixe | ±39 % | ≥ ±15 % |
| Couche médium sur un La1 : côté/centre, corrélation sous 120 Hz | mono | −11,4 dB ; 1,000 | −15 à −3 ; ≥ 0,95 |
| CPU, 8 voix × 7 copies, tout activé (largeur, ×2, saturation, formant, couche médium) | 2,5 % (8 voix simples) | 10,1 % du temps réel | ≤ 15 % (budget du synthé) |
| Allocations dans le callback, tout S4 activé (300 blocs) | – | 0 | 0 |

## Bass music (Drop)

| Style | Grave 60–250 v2 | Grave 60–250 v3 | Côté/centre v2 | Côté/centre v3 | Corr. v3 |
|---|---|---|---|---|---|
| dnb | −6,8 → −6,6 | −5,7 → −5,5 | −25,1 → −13,6 | −12,2 | 0,89 |
| liquid_dnb | −5,4 → −5,8 | −4,1 → −4,6 | −22,0 → −15,4 | −14,2 | 0,93 |
| neurofunk | −8,9 → −9,2 | −7,2 → −7,6 | −25,6 → −17,1 | −13,7 | 0,92 |
| jungle | −5,0 → −5,2 | −3,9 → −4,2 | −31,8 → −17,2 | −15,3 | 0,94 |
| jump_up | −10,4 → −10,5 | −9,2 → −9,4 | −21,3 → −10,7 | −7,4 | 0,69 |
| dubstep | −9,0 → −8,8 | −8,2 → −7,9 | −18,2 → −12,1 | −10,7 | 0,84 |
| riddim | −9,0 → −10,1 | −6,5 → −8,4 | −18,6 → −15,8 | −13,0 | 0,91 |

## Ce que ça dit

- **L'image s'ouvre** : +3,6 dB de côté/centre en médiane (+4,4 avec le kit v3), jusqu'à +11 à +14 dB en bass music et en dnb, +8,7 en trance. Huit styles entrent dans la cible avec le kit v2, treize avec le kit v3. Le grave reste mono (corrélation sous 120 Hz ≥ 0,994 partout) et la perte en mono reste petite (−0,8 LU au pire).
- **La médiane reste sous −15 dB et la corrélation au-dessus de 0,85** : ce qui domine un mélange de l'assistant (kick, caisse, basse, sub) est au centre, et doit le rester. Les styles encore étroits (house −23, trap −21, lofi −26, acid −31 à −40) ont un lead Plaits, échantillonné ou une 303 : S4 ne les élargit pas (seul le synthé a l'unisson stéréo ; le chorus du lead les aide un peu). Pistes pour S5 : percussions et charleys plus écartés, réverbe plus présente au Break, chorus sur les nappes Plaits.
- **Le lead bouge** : 19,9 % en médiane (16,0 avant), 18 leads sur 32 au-delà de ±15 %. Les leads qui ne bougent pas sont des sons sans aigus à filtrer (« Cloche » en triangle) ou des sources qui n'ont pas le cycle (Plaits, échantillons, instruments GM).
- **Le grave 60–250 Hz ne remonte pas** (−6,0 en médiane, rien de gagné en bass music) : la doublure à l'octave et la coupure à 80 Hz ajoutent surtout sous 60 Hz, la couche médium ajoute au-dessus de 250 Hz, et la queue du kick v2 (47 Hz, 1,5 s) pèse dans le total. Le kit v3 le remonte de 2 dB à lui seul. Pour la cible, c'est un équilibre de mélange (S5) : la basse plus forte que ses médiums, un sub sinus moins dominant ; riddim perd 1 à 2 dB (sa couche médium sans sub dessous).
- **Les 0,8 LU que le kit v3 a coûtés ne reviennent pas** (−9,2 contre −9,1) : la largeur ne fait pas de sonie, et les basses de bass music gardent la leur. C'est aussi pour S5 (cible de sonie sur le Drop, saturation de bus).
- **Rien ne change pour les projets sauvés** : WAV identiques à l'octet près sur les 37 morceaux d'avant.

## Ce que le générateur et l'interface doivent appeler

- Le générateur : `WideMix.apply(project, style, partOfTrack)` en dernière étape de `StarterWizard.build`, juste avant `sanitized()` (déjà en place). Il pose `Project.soundVersion = SOUND_V2`, `Mixer.monoBassHz = 120`, les réglages S4 des synthés par rôle, le chorus et l'écho ping-pong des leads, la plaque et l'égaliseur des retours, et ajoute ses courbes (au plus 16 en tout).
- L'appli : `NativeEngine.setSoundVersion(p.soundVersion)` et `NativeEngine.setMonoBass(p.mixer.monoBassHz)` (dans `syncEngine` / `pushMixer`), les bus avec 10 réglages (type de réverbe en 4 : `REVERB_HALL`, `REVERB_PLATE`, `REVERB_ROOM` ; égaliseur en 8 et 9 : `BUS_LOW_CUT`, `BUS_HIGH_CUT`).
- Les courbes : `AutoTarget.synth(piste, paramètre)` et `AutoTarget.plaits(piste, paramètre)` (types 8 et 9, canal = la voie de la piste).
- Le banc : la voie du lead dans l'enregistrement (`META lead`, `Project.leadTrackId()`).
