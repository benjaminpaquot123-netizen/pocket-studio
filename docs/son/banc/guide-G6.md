# Banc G6 : morceaux suivis jusqu'au bout du Guide

37 morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` (libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale (Drop, sinon Refrain) : bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.

G6 : chaque morceau est d'abord suivi jusqu'au bout de son parcours du Guide (`GuideFollow`, politique « accept » : la première option de chaque carte qui change le morceau ; sans mesures, la balance n'est pas appliquée). « Avant » : le morceau de l'assistant intact (`apres-S7.json`). Rendu principal seulement (pas de passe lead ni basse pour 32 styles). Relancer : `scripts/song-bench.sh --guide accept`.

## Résumé (médiane sur les styles)

| Mesure | Avant | Après | Écart | Min – max (après) | Cible |
|---|---|---|---|---|---|
| LUFS-I (morceau) | −10,2 | −10,1 | +0,1 | −13,9 – −8,5 | cible du genre ±1 (avant S5) |
| LUFS du Drop / Refrain (sections entières) | −8,7 | −8,6 | +0,0 | −12,2 – −7,1 | −8 à −6 en club |
| LUFS de la partie la plus forte (court terme, 20 % du haut) | −8,5 | −8,4 | +0,1 | −12,1 – −7,0 | cible du style ±1 (S5) |
| Écart à la cible (LU) | −0,1 | −0,1 | +0,0 | −2,5 – 0,0 | ±1 |
| True peak (dBTP) | −1,19 | −1,19 | +0,00 | −1,20 – −1,17 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 9,0 | 8,9 | −0,1 | 7,3 – 12,7 | 6 à 10 en club |
| LRA (LU) | 5,3 | 5,6 | +0,3 | 3,1 – 10,9 | 3 à 9 en club |
| Facteur de crête du Drop (dB) | 9,1 | 9,0 | −0,1 | 7,0 – 11,9 | ≥ 6 |
| Air > 6 kHz (dB rel.) | −16,3 | −16,4 | −0,1 | −23,4 – −12,5 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −4,71 | −4,89 | −0,18 | −5,98 – −3,39 | −4,5 ±0,7 |
| Grave 60–250 Hz au Drop (dB rel.) | −3,6 | −3,4 | +0,2 | −6,7 – −1,2 | ≥ −6 en bass music |
| Mouvement du lead (centroïde, ±%) | 19,5 | 26,2 | +6,7 | 21,6 – 30,8 | ≥ 15 |
| Côté/centre (dB) | −11,0 | −11,8 | −0,7 | −17,9 – −8,9 | −15 à −9 |
| Corrélation L/R | 0,85 | 0,88 | +0,02 | 0,77 – 0,97 | 0,3 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | +0,000 | 0,997 – 1,000 | ≥ 0,95 |
| Perte en mono (LU) | −0,54 | −0,47 | +0,07 | −0,95 – −0,13 | ≥ −1,5 |
| Drop − Groove, Couplet ou Montée (LU) | 2,5 | 2,5 | +0,0 | 1,9 – 6,0 | ≥ +2 |
| Drop − Break (LU) | 5,0 | 5,7 | +0,7 | 2,3 – 20,9 | ≥ +4 |
| Drop − Groove avant master (LU) | 4,5 | – | – | – – – | ≥ +2 |
| Drop − Break avant master (LU) | 7,4 | – | – | – – – | ≥ +4 |
| True peak du M4A décodé, premier encodage (dBTP) | −1,47 | – | – | – – – | – |
| True peak du M4A décodé, après la vérification de l'appli (dBTP) | −1,47 | – | – | – – – | ≤ −1,0 |
| Montée : sa montée sur ses 4 dernières mesures (LU) | 3,5 | 4,0 | +0,5 | 2,3 – 6,0 | ≥ +2 |
| Montée avant master (LU) | 3,5 | – | – | – – – | ≥ +2 |
| Drop − fin de Montée (LU, le plus petit du morceau) | 2,4 | 2,7 | +0,3 | 1,9 – 16,3 | ≥ +1 (+1,5 visé) |
| Drop − fin de Montée avant master (LU) | 4,8 | – | – | – – – | – |
| Intro − Drop / Refrain (LU) | −3,0 | −2,9 | +0,1 | −10,5 – −0,1 | ≥ −8 |
| Variation entre Drops (part des mesures qui diffèrent, paire la plus proche) | 0,50 | 0,50 | +0,00 | 0,44 – 1,00 | ≥ 0,25 |
| Continu (moyenne absolue) | 0,0000 | 0,0000 | +0,0000 | 0,0000 – 0,0000 | ≤ 0,001 |
| Gain de la cible (dB) | 6,7 | 6,1 | −0,6 | −1,2 – 10,9 | – |
| Perte dans la chaîne (LU) | 2,8 | 2,5 | −0,3 | 0,1 – 3,0 | ≤ 3 (S5, au Drop ; 6 avant) |
| B1 Basse au téléphone 200 Hz (dB) | −6,1 | – | – | – – – | ≥ −10 |
| B2 Basse au téléphone 300 Hz (dB) | −10,1 | – | – | – – – | ≥ −14 |
| B3 Basse dans le mix au téléphone (LU) | −8,5 | – | – | – – – | ≥ −10 hip-hop et bass music, ≥ −14 club |
| B4 Harmoniques 200–800 / 30–100 Hz (dB) | −5,5 | – | – | – – – | ≥ −14 |
| B5 Attaque de la basse (dB) | 5,1 | – | – | – – – | ≥ +2 (808, pluck : +4) |
| B6 Régularité de l'attaque (dB) | 20,88 | – | – | – – – | ≤ 0,5 |
| B7 Sub 40–60 Hz du mix (dB rel.) | −6,2 | −6,0 | +0,2 | −22,5 – −3,8 | −6 à −3 (le garder) |
| B8 Infra < 30 Hz du mix (dB rel.) | −25,6 | −25,8 | −0,2 | −29,8 – −19,9 | ≤ −20 |
| B9 Creux de la basse sous le kick (dB) | −0,1 | – | – | – – – | ≤ −6 club, ≤ −3 ailleurs |
| B12 Sonie ajoutée par l'exciteur (LU) | 0,06 | – | – | – – – | ≤ +1 |

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 32 sur 37 | dubstep (−1,1, plafonné), hard_techno (−2,4, plafonné), jump_up (−1,1, plafonné), neurofunk (−1,0, plafonné), riddim (−2,1, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 33 sur 37 | acidcore (+2,7), gabber (+3,3), hard_techno (+2,8), trance (+2,3) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 36 sur 37 | acidcore (+1,9) |
| Chaque Montée monte d'au moins +2 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 37 sur 37 | – |
| Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +1 LU (exporté ; sans Montée : –) (bloquant) | 37 sur 37 | – |
| Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 34 sur 37 | afrobeats (−8,4), drill (−8,1), phonk (−10,5) |
| Variation entre Drops ≥ 0,25 (modèle : aucune paire de Drops identique) | 37 sur 37 | – |
| B8 Rien sous 30 Hz : infra ≤ −15 dB du Drop (cible −20) (bloquant) | 37 sur 37 | – |
| B8 Rien sous 30 Hz : infra ≤ −20 dB du Drop (la cible) | 36 sur 37 | riddim (−19,9) |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −9,8 (−0,2) | −8,0 (Drop) | +6,1 | 2,2 | −8,1 | −1,19 | 8,6 (+0,2) | 10,2 (+6,5) | −17,6 (−0,1) | −4,44 | −10,6 (−0,6) | 0,84 | 0,999 | 2,2 (+0,2) | 16,9 (+13,1) |
| acid_techno | −9,0 (+0,1) | −7,5 (Drop) | +7,0 | 2,5 | −7,7 (+0,1) | −1,18 | 7,8 (−0,1) | 4,7 (+0,6) | −17,9 (−0,3) | −4,55 | −12,8 (−0,7) | 0,90 | 0,999 | 2,6 (+0,2) | 20,9 (+16,7) |
| acidcore | −8,9 (+0,1) | −7,5 (Drop) | +1,8 | 3,0 | −8,3 (−0,1) | −1,19 | 7,7 (−0,1) | 5,6 (+2,8) | −14,2 (−0,2) | −4,57 | −10,4 (−0,4) | 0,83 | 0,997 | 1,9 | 2,7 (+0,1) |
| afro_house | −10,9 | −9,0 (Drop) | +4,7 | 0,6 | −9,2 | −1,19 | 9,7 | 6,0 (+0,4) | −19,2 (−0,4) | −5,75 (−0,08) | −14,8 (−0,7) | 0,94 | 1,000 | 2,6 | 6,0 (+0,5) |
| afrobeats | −10,7 (−0,1) | −9,0 (Drop) | +3,8 | 1,8 | −9,4 (−0,1) | −1,18 | 9,5 (+0,2) | 7,3 (+0,4) | −23,4 (−0,2) | −5,98 (−0,07) | −11,8 (−0,1) | 0,88 | 1,000 | 2,4 (+0,2) | – |
| amapiano | −11,8 (−0,1) | −9,0 (Drop) | +5,5 | 1,1 | −9,3 | −1,19 | 10,6 (+0,1) | 8,1 (+0,5) | −18,4 (+0,1) | −5,93 (−0,07) | −12,9 (−1,7) | 0,90 | 1,000 | 4,2 (+0,1) | 8,9 (+0,9) |
| boom_bap | −11,8 | −10,0 (Drop) | +2,6 | 1,3 | −10,3 | −1,19 | 10,6 | 5,5 (+0,2) | −19,1 | −5,34 | −15,6 (−0,4) | 0,95 | 1,000 | 2,3 | – |
| deep_house | −11,0 | −9,0 (Drop) | +4,5 | 1,0 | −9,1 | −1,19 | 9,8 | 5,3 (+0,3) | −18,5 (−0,3) | −5,98 (−0,06) | −14,3 (−1,2) | 0,93 | 1,000 | 2,6 | 4,9 (+0,3) |
| detroit | −9,7 | −8,0 (Drop) | +5,2 | 1,8 | −8,2 | −1,19 | 8,5 | 6,1 (−0,1) | −16,4 (−0,1) | −3,88 (−0,09) | −13,5 (−1,3) | 0,92 | 1,000 | 2,5 | 6,5 |
| disco | −11,3 (+0,1) | −9,0 (Drop) | +3,1 | 0,8 | −9,2 | −1,19 | 10,1 (−0,1) | 6,1 (+0,1) | −16,0 (−0,1) | −4,96 | −11,8 (−1,0) | 0,88 | 1,000 | 2,4 (−0,1) | 6,7 |
| dnb | −10,1 (+0,1) | −7,5 (Drop) | +6,6 | 3,0 | −8,6 (+0,1) | −1,19 | 8,9 (−0,1) | 5,4 | −14,8 | −4,45 | −9,1 | 0,78 | 1,000 | 5,9 (+0,1) | 4,2 |
| drill | −10,1 (+0,1) | −8,5 (Drop) | +3,5 | 0,2 | −8,7 | −1,19 | 8,9 (−0,1) | 8,6 (+0,8) | −20,0 (−0,1) | −5,39 (−0,18) | −15,6 (−1,2) | 0,95 | 1,000 | 2,6 (+0,1) | – |
| dubstep | −10,2 (+0,1) | −7,5 (Drop) | +8,3 | 3,0 | −8,7 (+0,1) | −1,19 | 9,0 (−0,1) | 6,3 | −16,4 (−0,1) | −4,21 | −9,0 (+0,4) | 0,78 | 1,000 | 5,5 (+0,1) | 4,0 (−0,3) |
| gabber | −8,7 (+0,3) | −7,0 (Drop) | +8,4 | 3,0 | −8,0 (+0,1) | −1,19 | 7,5 (−0,3) | 4,2 (+0,4) | −14,8 | −4,29 | −8,9 (−1,0) | 0,77 (+0,05) | 0,999 | 5,2 (+0,4) | 3,3 (+0,1) |
| grime | −10,2 | −8,5 (Drop) | +3,9 | 1,4 | −8,9 | −1,18 | 9,1 | 4,7 (+0,1) | −14,4 (−0,1) | −3,69 (−0,06) | −9,3 (−0,1) | 0,79 | 1,000 | 2,4 (−0,1) | – |
| hard_techno | −10,4 (+0,1) | −6,0 (Drop) | +8,2 | 3,0 | −8,9 | −1,18 | 9,2 (−0,1) | 4,3 (−0,1) | −13,1 (+0,1) | −3,89 (−0,12) | −8,9 (−0,2) | 0,78 | 0,999 | 2,5 (−0,3) | 2,8 (−0,1) |
| house | −10,0 (+0,2) | −8,0 (Drop) | +7,0 | 3,0 | −8,4 (+0,1) | −1,18 | 8,8 (−0,2) | 6,3 (+0,2) | −16,5 (−0,1) | −5,36 | −13,9 (−1,4) | 0,92 | 1,000 | 2,2 (−0,1) | 8,5 (+0,2) |
| industrial_techno | −8,7 | −7,5 (Drop) | +4,6 | 0,5 | −7,6 | −1,19 | 7,5 | 4,6 (+0,1) | −17,3 (+0,2) | −5,09 (−0,09) | −12,5 (−2,9) | 0,89 (+0,09) | 0,999 | 2,1 | 6,8 (+0,4) |
| jersey_club | −9,8 (+0,1) | −8,5 (Drop) | +5,4 | 1,9 | −8,6 | −1,18 | 8,7 (−0,1) | 4,4 (−2,1) | −19,1 (−0,1) | −5,31 | −12,3 (−0,8) | 0,89 | 1,000 | 2,3 (+0,2) | – |
| jump_up | −10,7 (+0,1) | −7,5 (Drop) | +8,1 | 3,0 | −8,8 (+0,1) | −1,18 | 9,5 | 7,3 (+0,6) | −12,6 (+0,1) | −3,63 | −10,2 (−2,0) | 0,82 (+0,09) | 1,000 | 5,3 (+0,3) | 6,7 (+0,6) |
| jungle | −9,8 (+0,2) | −7,5 (Drop) | +7,2 | 3,0 | −8,1 (+0,1) | −1,18 | 8,6 (−0,2) | 5,5 (+0,2) | −15,0 (+0,1) | −4,43 | −10,6 (−1,9) | 0,84 (+0,08) | 1,000 | 2,0 (−0,2) | 5,7 (+0,5) |
| liquid_dnb | −9,9 (+0,1) | −8,0 (Drop) | +6,7 | 3,0 | −8,3 | −1,19 | 8,7 | 5,1 (−0,2) | −14,5 (−0,1) | −4,37 (−0,08) | −9,8 (−0,7) | 0,81 | 1,000 | 5,2 (+0,1) | 4,2 (+0,2) |
| lofi | −13,9 | −12,0 (Drop) | −1,2 | 0,1 | −12,2 | −1,20 | 12,7 | 6,5 (+0,3) | −22,2 | −5,71 (−0,12) | −16,8 (−1,6) | 0,96 | 1,000 | 2,7 (+0,1) | – |
| minimal | −9,7 (+0,1) | −8,0 (Drop) | +7,9 | 2,6 | −8,2 (+0,2) | −1,18 | 8,6 (−0,1) | 5,0 (+0,3) | −20,8 (−0,2) | −5,86 (−0,15) | −11,9 (−1,7) | 0,88 (+0,05) | 0,999 | 2,5 (+0,1) | 5,0 (+0,3) |
| neurofunk | −10,1 (+0,2) | −7,5 (Drop) | +9,2 | 3,0 | −8,7 (+0,1) | −1,19 | 8,9 (−0,2) | 9,3 (+0,1) | −12,5 (−0,1) | −3,39 | −11,8 | 0,88 | 1,000 | 6,0 (+0,2) | 7,5 (−0,1) |
| phonk | −8,5 | −7,0 (Drop) | +5,2 | 0,9 | −7,1 | −1,17 | 7,3 | 10,9 (+1,3) | −19,7 | −5,25 (−0,06) | −17,9 (−1,3) | 0,97 | 1,000 | 2,4 (+0,2) | – |
| pop | −10,3 | −9,0 (Drop) | +3,2 | 1,1 | −9,2 | −1,19 | 9,2 | 3,1 | −17,9 (+0,1) | −5,04 | −11,7 (−0,9) | 0,87 | 1,000 | 2,5 (+0,1) | – |
| reggaeton | −10,7 (−0,3) | −9,0 (Drop) | +2,8 | 0,6 | −9,2 | −1,19 | 9,5 (+0,3) | 7,7 (+3,7) | −16,2 (−0,2) | −4,52 (−0,15) | −12,5 (−0,9) | 0,89 | 1,000 | 2,7 (+0,2) | – |
| retro_techno | −9,4 | −8,0 (Drop) | +7,0 | 2,6 | −8,2 | −1,19 | 8,2 | 6,2 (+0,1) | −15,9 (−0,2) | −4,40 (−0,11) | −14,0 (−0,1) | 0,92 | 1,000 | 2,1 | 9,6 (+0,1) |
| riddim | −11,2 (+0,1) | −7,5 (Drop) | +10,9 | 3,0 | −9,8 (+0,1) | −1,18 | 10,1 (−0,1) | 7,6 (+0,2) | −12,5 | −3,61 | −11,2 (−0,2) | 0,86 | 1,000 | 6,0 (+0,2) | 5,1 (+0,1) |
| speed_garage | −10,0 (+0,1) | −8,0 (Drop) | +6,8 | 3,0 | −8,3 (+0,1) | −1,18 | 8,9 (−0,1) | 6,2 | −16,1 (−0,1) | −5,24 | −13,1 (−0,8) | 0,91 | 1,000 | 2,1 (−0,1) | 5,6 |
| synthwave | −9,5 (−0,2) | −8,0 (Drop) | +5,2 | 1,6 | −8,3 (−0,1) | −1,19 | 8,3 (+0,2) | 4,4 (+1,0) | −17,7 (−0,1) | −4,89 (−0,06) | −11,5 (−0,3) | 0,87 | 1,000 | 2,4 (+0,2) | – |
| tech_house | −10,9 (+0,2) | −8,0 (Drop) | +6,7 | 3,0 | −8,9 (+0,1) | −1,17 | 9,8 (−0,2) | 5,2 (−0,1) | −15,3 (−0,1) | −5,14 (−0,12) | −11,6 (−1,0) | 0,87 | 1,000 | 2,0 (−0,1) | 6,2 (−0,2) |
| techno | −9,9 (+0,1) | −7,5 (Drop) | +7,1 | 3,0 | −8,3 (+0,1) | −1,18 | 8,7 (−0,1) | 4,2 (−0,1) | −18,4 | −5,24 | −11,3 (−1,5) | 0,86 (+0,05) | 1,000 | 2,1 (−0,1) | 4,8 |
| trance | −9,6 (+0,2) | −8,0 (Drop) | +7,9 | 3,0 | −8,2 (+0,1) | −1,18 | 8,4 (−0,2) | 3,2 (−0,3) | −14,2 (−0,2) | −4,69 (−0,05) | −10,2 (−0,6) | 0,82 | 1,000 | 2,1 (−0,3) | 2,3 (+0,1) |
| trap | −10,3 (−0,1) | −8,5 (Drop) | +3,9 | 0,8 | −8,7 | −1,18 | 9,1 (+0,1) | 7,0 (+0,8) | −18,9 (−0,2) | −4,89 (−0,18) | −14,4 (−1,1) | 0,93 | 1,000 | 2,8 (+0,1) | – |
| uk_garage | −9,5 | −8,0 (Drop) | +6,9 | 3,0 | −8,5 (−0,1) | −1,18 | 8,3 | 4,2 (−0,3) | −14,1 (−0,3) | −4,76 (−0,16) | −11,5 (−1,4) | 0,87 | 1,000 | 2,1 (−0,1) | – |

## Montées, Drops et Intro (S5b)

Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). Bloquant : Drop − fin de Montée ≥ +1 LU. Intro − Drop : l'Intro du début entière contre les sections Drop (ou Refrain), à titre indicatif.

| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Intro − Drop |
|---|---|---|---|---|
| acid_house | −14,5 / −14,5 | −8,0 / −8,0 | +6,5 / +6,5 | −2,4 (−0,1) |
| acid_techno | −13,2 / −13,3 | −7,4 / −7,4 | +5,8 / +5,9 | −1,8 (−0,2) |
| acidcore | −24,5 / −24,6 / −24,5 | −8,2 / −8,2 / −8,2 | +16,3 / +16,4 / +16,3 | −0,3 |
| afro_house | −13,3 / −13,5 | −9,0 / −9,1 | +4,2 / +4,4 | −2,0 (+0,1) |
| afrobeats | – | – | – | −8,4 (+0,2) |
| amapiano | −14,2 / −14,6 | −9,4 / −9,3 | +4,8 / +5,2 | −3,8 (−0,2) |
| boom_bap | – | – | – | −5,7 (−0,3) |
| deep_house | −13,3 / −13,6 | −9,1 / −9,0 | +4,2 / +4,7 | −2,7 (+0,1) |
| detroit | −11,0 / −11,1 | −8,2 / −8,3 | +2,8 / +2,8 | −1,6 |
| disco | −12,0 / −12,0 | −9,5 / −9,1 | +2,5 / +2,9 | −3,5 (+0,6) |
| dnb | −11,2 / −11,2 / −11,2 | −8,6 / −8,5 / −8,5 | +2,6 / +2,6 / +2,7 | −3,1 |
| drill | – | – | – | −8,1 (−0,7) |
| dubstep | −10,9 / −10,9 / −11,0 | −9,1 / −8,6 / −8,6 | +1,9 / +2,4 / +2,4 | −3,4 |
| gabber | −10,4 / −10,3 / −10,4 | −7,8 / −8,1 / −7,9 | +2,5 / +2,3 / +2,5 | −0,1 (+0,5) |
| grime | – | – | – | −2,9 (+0,1) |
| hard_techno | −13,0 / −13,0 / −13,0 | −10,1 / −8,8 / −8,7 | +2,9 / +4,2 / +4,3 | −3,4 (+0,4) |
| house | −12,0 / −12,2 | −8,4 / −8,3 | +3,5 / +3,9 | −1,6 (+0,3) |
| industrial_techno | −10,2 / −10,3 | −7,5 / −7,6 | +2,7 / +2,7 | −0,8 |
| jersey_club | – | – | – | −4,6 (−0,6) |
| jump_up | −11,0 / −10,9 / −11,0 | −9,1 / −8,6 / −8,6 | +1,9 / +2,4 / +2,4 | −4,7 (+0,2) |
| jungle | −11,4 / −12,2 / −11,4 | −7,9 / −8,1 / −8,2 | +3,5 / +4,1 / +3,2 | −3,5 (+0,4) |
| liquid_dnb | −10,5 / −10,4 / −10,4 | −8,3 / −8,3 / −8,2 | +2,2 / +2,1 / +2,2 | −3,0 (+0,1) |
| lofi | – | – | – | −6,6 (−0,5) |
| minimal | −11,5 / −11,9 | −8,0 / −8,3 | +3,5 / +3,6 | −2,4 (−0,1) |
| neurofunk | −10,8 / −10,8 / −10,8 | −8,9 / −8,7 / −8,7 | +1,9 / +2,1 / +2,1 | −2,4 (+0,1) |
| phonk | – | – | – | −10,5 (−1,2) |
| pop | – | – | – | −2,2 |
| reggaeton | – | – | – | −3,3 (−0,3) |
| retro_techno | −11,0 / −11,1 | −8,2 / −8,2 | +2,9 / +2,8 | −1,2 |
| riddim | −11,8 / −11,8 / −11,8 | −9,6 / −9,7 / −9,7 | +2,2 / +2,1 / +2,2 | −4,2 |
| speed_garage | −10,9 / −10,8 | −8,2 / −8,3 | +2,6 / +2,6 | −2,2 (+0,1) |
| synthwave | – | – | – | −2,2 (−0,3) |
| tech_house | −11,6 / −11,8 | −8,9 / −8,9 | +2,7 / +2,9 | −3,8 (+0,1) |
| techno | −10,7 / −10,7 | −8,2 / −8,3 | +2,5 / +2,4 | −2,6 (+0,1) |
| trance | −10,8 / −10,9 | −8,4 / −8,1 | +2,4 / +2,8 | −2,4 (+0,3) |
| trap | – | – | – | −5,6 (−0,6) |
| uk_garage | – | – | – | −1,8 (+0,5) |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,1 | −3,3 | −13,9 | −8,3 | −11,2 | −17,6 |
| acid_techno | −4,7 | −3,2 | −14,1 | −11,7 | −12,4 | −17,9 |
| acidcore | −10,6 | −2,0 | −10,4 | −10,9 | −11,5 | −14,2 |
| afro_house | −11,8 | −1,2 | −9,4 | −14,1 | −18,2 | −19,2 |
| afrobeats | −4,3 | −3,7 | −8,7 | −13,5 | −17,0 | −23,4 |
| amapiano | −15,9 | −1,4 | −9,4 | −9,9 | −16,8 | −18,4 |
| boom_bap | −4,3 | −3,1 | −11,0 | −16,1 | −16,0 | −19,1 |
| deep_house | −3,9 | −4,1 | −11,0 | −9,9 | −21,5 | −18,5 |
| detroit | −4,7 | −4,2 | −13,1 | −8,4 | −12,0 | −16,4 |
| disco | −8,0 | −3,2 | −7,1 | −10,4 | −13,1 | −16,0 |
| dnb | −6,0 | −4,3 | −8,1 | −8,7 | −12,5 | −14,8 |
| drill | −5,3 | −2,9 | −9,9 | −12,4 | −16,8 | −20,0 |
| dubstep | −4,5 | −5,9 | −7,4 | −8,2 | −14,9 | −16,4 |
| gabber | −5,4 | −5,6 | −7,0 | −7,9 | −13,8 | −14,8 |
| grime | −5,5 | −4,9 | −11,3 | −7,7 | −9,6 | −14,4 |
| hard_techno | −8,6 | −5,5 | −5,8 | −6,5 | −13,7 | −13,1 |
| house | −5,5 | −3,1 | −9,7 | −11,0 | −17,5 | −16,5 |
| industrial_techno | −6,1 | −3,4 | −10,3 | −7,8 | −16,4 | −17,3 |
| jersey_club | −3,8 | −4,9 | −10,9 | −8,3 | −18,5 | −19,1 |
| jump_up | −6,2 | −6,7 | −7,6 | −6,4 | −10,9 | −12,6 |
| jungle | −6,2 | −3,4 | −9,4 | −10,8 | −12,0 | −15,0 |
| liquid_dnb | −5,7 | −3,9 | −10,0 | −8,6 | −13,3 | −14,5 |
| lofi | −4,5 | −2,8 | −11,7 | −15,5 | −18,0 | −22,2 |
| minimal | −5,6 | −2,7 | −9,9 | −11,7 | −18,8 | −20,8 |
| neurofunk | −3,7 | −5,8 | −9,4 | −12,6 | −10,8 | −12,5 |
| phonk | −4,5 | −4,0 | −9,8 | −10,0 | −16,2 | −19,7 |
| pop | −5,8 | −3,1 | −10,4 | −10,0 | −14,7 | −17,9 |
| reggaeton | −6,6 | −3,2 | −9,3 | −9,8 | −12,9 | −16,2 |
| retro_techno | −5,1 | −3,4 | −11,0 | −11,4 | −13,1 | −15,9 |
| riddim | −4,5 | −6,2 | −8,8 | −7,7 | −14,1 | −12,5 |
| speed_garage | −5,9 | −3,1 | −8,8 | −11,6 | −15,2 | −16,1 |
| synthwave | −6,1 | −3,2 | −9,8 | −9,3 | −13,9 | −17,7 |
| tech_house | −5,4 | −3,9 | −9,5 | −8,8 | −16,1 | −15,3 |
| techno | −4,6 | −4,5 | −7,9 | −10,3 | −16,5 | −18,4 |
| trance | −5,9 | −4,5 | −8,0 | −8,0 | −15,8 | −14,2 |
| trap | −3,5 | −4,3 | −12,5 | −10,8 | −16,0 | −18,9 |
| uk_garage | −7,1 | −3,4 | −10,3 | −7,8 | −13,7 | −14,1 |
| **médiane** | −5,5 | −3,4 | −9,8 | −9,9 | −14,7 | −16,4 |

## Largeur et mouvement (S4)

Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.

| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |
|---|---|---|---|---|---|
| acid_house | −10,6 (−0,6) | 0,84 | 0,999 | – | −3,3 |
| acid_techno | −12,8 (−0,7) | 0,90 | 0,999 | – | −3,2 (−0,1) |
| acidcore | −10,4 (−0,4) | 0,83 | 0,997 | – | −2,0 (−0,1) |
| afro_house | −14,8 (−0,7) | 0,94 | 1,000 | 30,8 | −1,2 (+0,1) |
| afrobeats | −11,8 (−0,1) | 0,88 | 1,000 | 21,6 | −3,7 (+0,1) |
| amapiano | −12,9 (−1,7) | 0,90 | 1,000 | – | −1,4 |
| boom_bap | −15,6 (−0,4) | 0,95 | 1,000 | – | −3,1 (+0,1) |
| deep_house | −14,3 (−1,2) | 0,93 | 1,000 | – | −4,1 (−0,1) |
| detroit | −13,5 (−1,3) | 0,92 | 1,000 | – | −4,2 |
| disco | −11,8 (−1,0) | 0,88 | 1,000 | – | −3,2 (+0,1) |
| dnb | −9,1 | 0,78 | 1,000 | – | −4,3 (+0,1) |
| drill | −15,6 (−1,2) | 0,95 | 1,000 | – | −2,9 (+0,2) |
| dubstep | −9,0 (+0,4) | 0,78 | 1,000 | – | −5,9 (+0,1) |
| gabber | −8,9 (−1,0) | 0,77 | 0,999 | – | −5,6 (+0,4) |
| grime | −9,3 (−0,1) | 0,79 | 1,000 | – | −4,9 (+0,2) |
| hard_techno | −8,9 (−0,2) | 0,78 | 0,999 | – | −5,5 (+0,3) |
| house | −13,9 (−1,4) | 0,92 | 1,000 | – | −3,1 (+0,1) |
| industrial_techno | −12,5 (−2,9) | 0,89 | 0,999 | – | −3,4 (+0,2) |
| jersey_club | −12,3 (−0,8) | 0,89 | 1,000 | – | −4,9 (+0,5) |
| jump_up | −10,2 (−2,0) | 0,82 | 1,000 | – | −6,7 (+0,6) |
| jungle | −10,6 (−1,9) | 0,84 | 1,000 | – | −3,4 (+0,3) |
| liquid_dnb | −9,8 (−0,7) | 0,81 | 1,000 | – | −3,9 (+0,2) |
| lofi | −16,8 (−1,6) | 0,96 | 1,000 | – | −2,8 (+0,1) |
| minimal | −11,9 (−1,7) | 0,88 | 0,999 | – | −2,7 (+0,2) |
| neurofunk | −11,8 | 0,88 | 1,000 | – | −5,8 (+0,2) |
| phonk | −17,9 (−1,3) | 0,97 | 1,000 | – | −4,0 (+0,3) |
| pop | −11,7 (−0,9) | 0,87 | 1,000 | – | −3,1 |
| reggaeton | −12,5 (−0,9) | 0,89 | 1,000 | – | −3,2 (+0,3) |
| retro_techno | −14,0 (−0,1) | 0,92 | 1,000 | – | −3,4 (+0,1) |
| riddim | −11,2 (−0,2) | 0,86 | 1,000 | – | −6,2 |
| speed_garage | −13,1 (−0,8) | 0,91 | 1,000 | – | −3,1 (+0,1) |
| synthwave | −11,5 (−0,3) | 0,87 | 1,000 | – | −3,2 |
| tech_house | −11,6 (−1,0) | 0,87 | 1,000 | – | −3,9 (+0,1) |
| techno | −11,3 (−1,5) | 0,86 | 1,000 | – | −4,5 |
| trance | −10,2 (−0,6) | 0,82 | 1,000 | – | −4,5 (+0,2) |
| trap | −14,4 (−1,1) | 0,93 | 1,000 | – | −4,3 (+0,5) |
| uk_garage | −11,5 (−1,4) | 0,87 | 1,000 | – | −3,4 (+0,1) |

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −33,8 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −37,5 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,0 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −40,2 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −52,7 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −44,9 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −35,7 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −43,7 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −44,6 | 335,2 |
| disco | 0 | 0 | 0,0000 | −46,2 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −32,9 | 297,9 |
| drill | 0 | 0 | 0,0000 | −40,4 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −41,8 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −38,7 | 296,2 |
| grime | 0 | 0 | 0,0000 | −37,7 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −35,4 | 358,4 |
| house | 0 | 0 | 0,0000 | −44,8 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −36,7 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −41,5 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −32,4 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −36,8 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −32,9 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −40,3 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −43,0 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −33,7 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −42,5 | 192,0 |
| pop | 0 | 0 | 0,0000 | −42,9 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −38,5 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −38,9 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −40,9 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −44,4 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −41,5 | 192,0 |
| tech_house | 0 | 0 | 0,0000 | −44,7 | 182,9 |
| techno | 0 | 0 | 0,0000 | −41,0 | 320,0 |
| trance | 0 | 0 | 0,0000 | −43,9 | 306,1 |
| trap | 0 | 0 | 0,0000 | −41,8 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −35,9 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
40dfab4ce7084161632b3efdffc0dd832e937a8b5ccbabc728f593b6a07a60b5  acid_house.wav
ed872ac7457507bef6b693c5058455086f05d491bd775a36279fdf94c5bd0f98  acid_techno.wav
1e93385ff2be2fd9002e2fca12385df77e1f1bea7389a879b4447a34d092fcfc  acidcore.wav
0d4bc41fda0ea69775d94a87513dea220a2fc5ef9cf06dbbfe72002e742a862c  afro_house.wav
063cb7e48833e27bdd274a7c54a6077cba45f035ecdec00ca739816e9d9d2227  afrobeats.wav
cfb3a1cdfb58c9eae034ee7b8d15f99d1a7a1439d6039cee93dd33d70f239bc7  amapiano.wav
6e2f499f6dcdd816edd0ecfa8e8b4f3e8d5c67ab20aea440aec7116520a3389c  boom_bap.wav
d59e308daa916512eb986b95d3ec28e5dc0a1ab95ba1b5c7aa410b8642b654a5  deep_house.wav
8965ada56e20a359d0a06f2f6ad51632bab5daf1410fbd2996e7930106cb7106  detroit.wav
0f13118972cc38836d7951421e560201a4c094dff963e7f089d9b35f6d1be8c2  disco.wav
f47f4986778eafbf0805954eb338bca6351266a69233d509153eba8fef03be2c  dnb.wav
6649a0a5c6f6b3c772750fe167c2dc617e87cc3a52c83f5bc0aefd837063e623  drill.wav
742cda21812e14b76914d3ae745b406c3b0fc178a13e6c304deee1878c89e49a  dubstep.wav
eb3fb4e03b04ebfc9c825c5b7828ade7991cdb49c9f2b55120b619647cbd5959  gabber.wav
50e1794cdaf4a942e2351aff98b6dd52b6af0a69d001d1837b7986766b3697f7  grime.wav
8b9e9c2abe2fb1c8f4c601d76036ee450be13d84a9a1117bdb9911c691a7673a  hard_techno.wav
1f12cdc9cd9ea12013e8cb9f43d4b58fcb436be654cbc9ffb9115b56ad5a0b60  house.wav
ebb89e0bc47947643d8be04605557a9172b60a0321f386d260c9ef9627474328  industrial_techno.wav
d18993767c067fed8f3fb62fe43ebb6a52ebce6cdf30f8efdc41e40b6f386b87  jersey_club.wav
bc44ef1d92a878535c64a408f3ab9bdeb3b6fdf4a616cb1110db9004172e78b8  jump_up.wav
026f84d451709ee13d941262fc0dbc6e84f327c1fe2ed7874e99c37bd01f050d  jungle.wav
e5a6b3292cad8fe1b89036a4a57fb87a71f4f72758310dcddcf1be62dc1e7b51  liquid_dnb.wav
c8dbc282493830d06e88ecc337e27192b7dbce8e3978464e8b50bc7384c695a2  lofi.wav
3dbd4eebf4600f3a21163a43b0fd944ead3df41b0ad776e75e685b2eb061d219  minimal.wav
4dc198a12fd955ef5fd0d438e92df642f96b8896fa49e93e8b8110c31d2faba5  neurofunk.wav
fdfc082a37be3eb7681029788ac2cd4a16d89106d86ff528b85142aff9a584ce  phonk.wav
070a3abda31770afd1d819218cb7bc77d8f792b87caf369bea072751e8855fad  pop.wav
dc908a7e681999134881d52f63273ca126aef3c68a0a30d482a4790477c14071  reggaeton.wav
4e932eaef7ebd8c56833e53d0dc24b763a486aee47aba5c5534acb422768e8fe  retro_techno.wav
95b264e598293572607e039d0b7309d3d38ab951f255b67cdac048bafdaaf019  riddim.wav
8a79769d79336724154822e72b0700ee9be23705b887e4af3f63421d2f4abb3e  speed_garage.wav
6e9334edbfb5557b0951fcbe5a41161a946e2984998ed1d54d1d260e92a72bd1  synthwave.wav
76b2bf2d12546e5c22ee43ac8be9ecae8a72cf760fbb55d4c58959ae617d12ab  tech_house.wav
cb9001448ce948a6b96470fc8105bab243232cd0a008b1fef8fbfd02c48539a1  techno.wav
fa789c49a4769b8f0c1d87d2f2fd8486ad4e9e137862bca74d973f79a08c80e6  trance.wav
94181e74cd7d87d0c53f3cc3ccf4a4e5b05280ef4040070030097f7d02220570  trap.wav
875bbc903bcb1ce3d973a635a57be492f8a3cb1dbd227bbfcf07d6db62ddec94  uk_garage.wav
```
