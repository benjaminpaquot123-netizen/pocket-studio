# Banc de mesure du son : après S6

37 morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` (libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale (Drop, sinon Refrain) : bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.

## Résumé (médiane sur les styles)

| Mesure | Avant | Après | Écart | Min – max (après) | Cible |
|---|---|---|---|---|---|
| LUFS-I (morceau) | −10,1 | −10,2 | −0,2 | −14,0 – −8,4 | cible du genre ±1 (avant S5) |
| LUFS du Drop / Refrain (sections entières) | −8,7 | −8,7 | +0,0 | −12,2 – −7,1 | −8 à −6 en club |
| LUFS de la partie la plus forte (court terme, 20 % du haut) | −8,5 | −8,5 | +0,0 | −12,1 – −7,0 | cible du style ±1 (S5) |
| Écart à la cible (LU) | −0,1 | −0,1 | −0,0 | −2,6 – −0,0 | ±1 |
| True peak (dBTP) | −1,18 | −1,19 | −0,01 | −1,19 – −1,16 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 8,9 | 9,0 | +0,1 | 7,2 – 12,8 | 6 à 10 en club |
| LRA (LU) | 5,3 | 5,4 | +0,1 | 2,4 – 11,2 | 3 à 9 en club |
| Facteur de crête du Drop (dB) | 8,9 | 8,9 | +0,0 | 6,9 – 11,8 | ≥ 6 |
| Air > 6 kHz (dB rel.) | −16,3 | −16,3 | −0,0 | −23,4 – −12,6 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −4,86 | −4,87 | −0,01 | −6,15 – −3,53 | −4,5 ±0,7 |
| Grave 60–250 Hz au Drop (dB rel.) | −3,4 | −3,4 | +0,0 | −7,2 – −1,2 | ≥ −6 en bass music |
| Mouvement du lead (centroïde, ±%) | 19,3 | 19,3 | +0,0 | 0,0 – 57,1 | ≥ 15 |
| Côté/centre (dB) | −17,1 | −16,2 | +0,9 | −23,5 – −8,8 | −15 à −9 |
| Corrélation L/R | 0,96 | 0,95 | −0,01 | 0,77 – 0,99 | 0,3 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | +0,000 | 0,995 – 1,000 | ≥ 0,95 |
| Perte en mono (LU) | −0,13 | −0,15 | −0,02 | −0,61 – −0,03 | ≥ −1,5 |
| Drop − Groove, Couplet ou Montée (LU) | 1,9 | 2,4 | +0,5 | 2,0 – 6,0 | ≥ +2 |
| Drop − Break (LU) | 4,3 | 4,8 | +0,4 | 1,7 – 9,7 | ≥ +4 |
| Drop − Groove avant master (LU) | 3,0 | 4,5 | +1,5 | 2,6 – 8,8 | ≥ +2 |
| Drop − Break avant master (LU) | 7,1 | 7,5 | +0,4 | 4,3 – 12,3 | ≥ +4 |
| True peak du M4A décodé, premier encodage (dBTP) | 0,48 | −1,46 | −1,94 | −2,01 – −1,19 | – |
| True peak du M4A décodé, après la vérification de l'appli (dBTP) | −1,81 | −1,46 | +0,35 | −2,01 – −1,19 | ≤ −1,0 |
| Montée : sa montée sur ses 4 dernières mesures (LU) | 3,8 | 3,7 | −0,1 | 1,9 – 5,8 | ≥ +2 |
| Montée avant master (LU) | 3,8 | 3,7 | −0,1 | 2,3 – 5,8 | ≥ +2 |
| Drop − fin de Montée (LU, le plus petit du morceau) | 2,5 | 2,6 | +0,1 | 1,6 – 5,5 | ≥ +1 (+1,5 visé) |
| Drop − fin de Montée avant master (LU) | 5,0 | 5,0 | −0,0 | 3,5 – 7,0 | – |
| Intro − Drop / Refrain (LU) | −3,1 | −3,2 | −0,1 | −10,9 – −0,2 | ≥ −8 |
| Variation entre Drops (part des mesures qui diffèrent, paire la plus proche) | 0,50 | 0,50 | +0,00 | 0,44 – 1,00 | ≥ 0,25 |
| Continu (moyenne absolue) | 0,0000 | 0,0000 | −0,0000 | 0,0000 – 0,0000 | ≤ 0,001 |
| Gain de la cible (dB) | 7,0 | 6,9 | −0,1 | −1,1 – 11,2 | – |
| Perte dans la chaîne (LU) | 2,4 | 2,7 | +0,4 | 0,1 – 3,0 | ≤ 3 (S5, au Drop ; 6 avant) |
| B1 Basse au téléphone 200 Hz (dB) | −6,2 | −6,5 | −0,3 | −11,5 – −2,5 | ≥ −10 |
| B2 Basse au téléphone 300 Hz (dB) | −10,4 | −10,7 | −0,2 | −15,4 – −5,7 | ≥ −14 |
| B3 Basse dans le mix au téléphone (LU) | −8,1 | −9,1 | −0,9 | −34,6 – −4,5 | ≥ −10 hip-hop et bass music, ≥ −14 club |
| B4 Harmoniques 200–800 / 30–100 Hz (dB) | −6,3 | −6,3 | +0,0 | −15,7 – 12,5 | ≥ −14 |
| B5 Attaque de la basse (dB) | 5,0 | 5,2 | +0,2 | 2,4 – 85,5 | ≥ +2 (808, pluck : +4) |
| B6 Régularité de l'attaque (dB) | 18,48 | 10,75 | −7,73 | 0,23 – 48,26 | ≤ 0,5 |
| B7 Sub 40–60 Hz du mix (dB rel.) | −5,8 | −6,0 | −0,1 | −21,9 – −3,6 | −6 à −3 (le garder) |
| B8 Infra < 30 Hz du mix (dB rel.) | −25,5 | −25,5 | +0,0 | −29,6 – −19,6 | ≤ −20 |
| B9 Creux de la basse sous le kick (dB) | −0,1 | 0,5 | +0,5 | −11,2 – 5,2 | ≤ −6 club, ≤ −3 ailleurs |
| B12 Sonie ajoutée par l'exciteur (LU) | 0,06 | 0,04 | −0,01 | 0,00 – 0,22 | ≤ +1 |

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| True peak du M4A décodé ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 30 sur 37 | dnb (−1,1, plafonné), dubstep (−1,2, plafonné), hard_techno (−2,6, plafonné), jump_up (−1,2, plafonné), neurofunk (−1,3, plafonné), riddim (−2,2, plafonné), tech_house (−1,0, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 32 sur 37 | acid_house (+3,8), acidcore (+2,0), gabber (+3,2), hard_techno (+2,3), trance (+1,7) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 36 sur 37 | acidcore (+2,0) |
| Chaque Montée monte d'au moins +2 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 36 sur 37 | acid_techno (+1,9 / +1,9) |
| Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +1 LU (exporté ; sans Montée : –) (bloquant) | 37 sur 37 | – |
| Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 33 sur 37 | afrobeats (−9,8), drill (−8,7), lofi (−8,2), phonk (−10,9) |
| Variation entre Drops ≥ 0,25 (modèle : aucune paire de Drops identique) | 37 sur 37 | – |
| B2 Basse au téléphone (300 Hz) ≥ −16 dB (cible −14) (bloquant) | 37 sur 37 | – |
| B2 Basse au téléphone (300 Hz) ≥ −14 dB (la cible) | 36 sur 37 | industrial_techno (−15,4) |
| B1 Basse au téléphone (200 Hz) ≥ −10 dB | 35 sur 37 | hard_techno (−10,3), industrial_techno (−11,5) |
| B4 Harmoniques de la basse ≥ −14 dB | 36 sur 37 | industrial_techno (−15,7) |
| B5 Attaque de la basse ≥ +2 dB (notes assez longues pour la lire) | 37 sur 37 | – |
| B6 Écart de l'attaque ≤ 0,5 dB | 27 sur 37 | afro_house (10,8), afrobeats (21,9), amapiano (2,7), boom_bap (35,2), deep_house (27,8), detroit (43,2), disco (3,7), grime (2,7), house (48,3), jersey_club (0,7) |
| B9 La basse s'efface sous le kick (≤ −3 dB) | 25 sur 37 | afrobeats (−0,2), boom_bap (0,1), deep_house (1,1), detroit (−1,3), disco (2,3), drill (0,8), gabber (5,2), grime (−0,7), hard_techno (4,6), house (−1,4), industrial_techno (4,6), jersey_club (2,2) |
| B8 Rien sous 30 Hz : infra ≤ −15 dB du Drop (cible −20) (bloquant) | 37 sur 37 | – |
| B8 Rien sous 30 Hz : infra ≤ −20 dB du Drop (la cible) | 36 sur 37 | riddim (−19,6) |
| B12 L'exciteur ajoute au plus +1 LU au morceau (avant master) (bloquant) | 37 sur 37 | – |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −9,6 (−0,4) | −8,0 (Drop) | +6,9 | 2,2 | −8,1 | −1,19 | 8,4 (+0,4) | 3,8 (+1,0) | −17,6 | −4,62 | −17,5 (+6,8) | 0,96 | 0,998 | 2,1 (+1,5) | 3,8 (+1,4) |
| acid_techno | −8,9 (−0,7) | −7,5 (Drop) | +8,1 | 2,8 | −7,7 | −1,18 | 7,7 (+0,7) | 4,0 (+0,9) | −17,7 | −4,71 | −20,4 (+10,3) | 0,98 | 0,998 | 2,4 (+1,4) | 4,2 (+1,2) |
| acidcore | −8,7 (−0,4) | −7,5 (Drop) | +2,8 | 2,9 | −8,0 | −1,19 | 7,5 (+0,4) | 2,4 (−0,4) | −14,9 (−0,1) | −4,85 | −17,8 (+18,2) | 0,97 | 0,995 | 2,0 (+1,1) | 2,0 (+1,4) |
| afro_house | −10,8 (−0,2) | −9,0 (Drop) | +5,4 | 0,7 | −9,2 | −1,19 | 9,6 (+0,2) | 5,2 (+0,4) | −18,9 (−0,1) | −5,79 | −17,1 (+0,8) | 0,96 | 1,000 | 2,5 (+0,5) | 5,1 (+0,4) |
| afrobeats | −10,6 (−0,2) | −9,0 (Drop) | +4,6 | 2,1 | −9,4 | −1,18 | 9,4 (+0,2) | 7,4 (+0,6) | −23,4 (−0,1) | −6,15 (−0,06) | −16,0 (+1,1) | 0,95 | 1,000 | 2,0 (+0,8) | – |
| amapiano | −11,8 | −9,0 (Drop) | +5,9 | 1,1 | −9,4 | −1,19 | 10,6 | 8,4 (−0,3) | −18,7 | −6,10 | −17,9 (+2,5) | 0,97 | 1,000 | 4,6 (−0,1) | 9,5 (−0,6) |
| boom_bap | −11,9 (−0,2) | −10,0 (Drop) | +2,7 | 1,3 | −10,4 | −1,19 | 10,7 (+0,2) | 6,3 (−0,1) | −19,4 | −5,37 | −20,8 (+0,8) | 0,98 | 0,999 | 2,3 (+0,4) | – |
| deep_house | −11,0 (−0,2) | −9,0 (Drop) | +5,0 | 1,1 | −9,1 | −1,19 | 9,8 (+0,2) | 4,7 (+0,3) | −18,2 (−0,1) | −5,97 | −17,1 (+0,4) | 0,96 | 0,999 | 2,6 (+0,4) | 4,2 (+0,4) |
| detroit | −9,5 (−0,4) | −8,0 (Drop) | +5,8 | 2,0 | −8,1 | −1,19 | 8,3 (+0,4) | 6,0 (+0,5) | −16,3 | −3,91 | −18,5 (+2,4) | 0,97 | 1,000 | 2,2 (+1,2) | 6,5 (+1,0) |
| disco | −11,4 (−0,2) | −9,0 (Drop) | +3,6 | 0,8 | −9,2 | −1,19 | 10,2 (+0,2) | 6,5 (+0,1) | −16,0 (−0,1) | −5,14 | −15,7 (+0,5) | 0,95 | 1,000 | 2,4 (+0,4) | 7,2 (+0,2) |
| dnb | −10,2 | −7,5 (Drop) | +7,0 | 3,0 | −8,7 | −1,18 | 9,1 | 5,3 | −14,9 | −4,59 | −12,2 | 0,89 | 1,000 | 6,0 | 4,3 |
| drill | −10,2 (−0,2) | −8,5 (Drop) | +4,4 | 0,4 | −8,7 | −1,19 | 9,0 (+0,2) | 9,3 (−0,4) | −20,0 (−0,1) | −5,36 (−0,08) | −19,9 (+0,8) | 0,98 | 1,000 | 2,5 (+0,5) | – |
| dubstep | −10,4 | −7,5 (Drop) | +8,8 | 3,0 | −8,8 | −1,18 | 9,2 | 6,4 | −16,4 | −4,31 | −12,3 | 0,89 | 1,000 | 5,5 | 4,3 |
| gabber | −8,8 | −7,0 (Drop) | +9,3 | 3,0 | −8,1 | −1,19 | 7,6 | 3,9 (−0,1) | −14,8 | −4,42 | −12,6 (+0,4) | 0,90 | 0,999 | 5,0 | 3,2 (−0,1) |
| grime | −10,3 (−0,1) | −8,5 (Drop) | +4,8 | 1,5 | −8,9 | −1,18 | 9,1 (+0,1) | 5,4 (−0,1) | −15,0 | −3,86 | −15,5 (+0,3) | 0,94 | 1,000 | 2,3 (+0,4) | – |
| hard_techno | −10,4 | −6,0 (Drop) | +8,4 | 3,0 | −8,8 | −1,19 | 9,2 | 4,4 | −13,2 (−0,1) | −3,77 | −8,8 (+0,5) | 0,77 | 0,997 | 2,8 | 2,3 (−0,1) |
| house | −10,1 (−0,6) | −8,0 (Drop) | +7,5 | 3,0 | −8,5 | −1,18 | 9,0 (+0,6) | 6,4 (+1,0) | −16,3 (−0,1) | −5,42 | −18,2 (+0,7) | 0,97 | 1,000 | 2,2 (+1,6) | 9,1 (+2,4) |
| industrial_techno | −8,9 (−0,2) | −7,5 (Drop) | +4,4 | 0,3 | −7,7 | −1,19 | 7,7 (+0,2) | 4,8 | −17,6 (−0,1) | −5,19 | −16,0 (+0,9) | 0,95 | 0,997 | 2,3 (+0,5) | 7,5 (+0,2) |
| jersey_club | −10,0 (−0,3) | −8,5 (Drop) | +6,4 | 2,2 | −8,6 | −1,18 | 8,8 (+0,3) | 7,6 (+0,4) | −19,0 (−0,1) | −5,30 | −17,2 | 0,96 | 1,000 | 2,1 (+1,0) | – |
| jump_up | −10,7 | −7,5 (Drop) | +8,4 | 3,0 | −8,8 | −1,18 | 9,5 | 6,5 | −12,7 | −3,64 | −9,4 | 0,79 | 0,999 | 5,0 | 5,8 |
| jungle | −10,0 (−0,1) | −7,5 (Drop) | +7,4 | 3,0 | −8,3 | −1,17 | 8,8 (+0,1) | 5,4 (+0,6) | −14,7 | −4,41 | −12,4 | 0,89 | 1,000 | 2,0 (+0,7) | 5,4 (+1,0) |
| liquid_dnb | −10,1 | −8,0 (Drop) | +7,2 | 3,0 | −8,4 | −1,18 | 8,9 | 5,2 | −14,6 | −4,46 | −15,1 | 0,94 | 1,000 | 5,4 | 4,5 |
| lofi | −14,0 | −12,0 (Drop) | −1,1 | 0,1 | −12,2 | −1,19 | 12,8 | 8,0 (−0,3) | −22,4 (−0,1) | −5,74 (−0,06) | −23,5 (+3,2) | 0,99 | 1,000 | 2,7 | – |
| minimal | −9,8 (−0,3) | −8,0 (Drop) | +8,4 | 3,0 | −8,3 | −1,19 | 8,6 (+0,3) | 4,7 (+1,1) | −20,6 (−0,1) | −5,82 | −12,7 (−0,1) | 0,90 | 0,999 | 2,3 (+0,9) | 4,6 (+1,1) |
| neurofunk | −10,3 | −7,5 (Drop) | +9,6 | 3,0 | −8,9 | −1,19 | 9,1 | 9,4 | −12,6 | −3,53 | −15,0 | 0,94 | 1,000 | 5,9 | 7,9 |
| phonk | −8,4 (−0,4) | −7,0 (Drop) | +6,3 | 1,1 | −7,1 | −1,17 | 7,2 (+0,4) | 11,2 (−0,3) | −19,8 | −5,25 | −23,1 (+1,1) | 0,99 | 1,000 | 2,2 (+0,8) | – |
| pop | −10,3 (−0,1) | −9,0 (Drop) | +3,8 | 1,2 | −9,2 | −1,19 | 9,2 (+0,1) | 3,1 (+0,3) | −18,1 (−0,1) | −5,14 (−0,09) | −17,7 (+1,2) | 0,97 | 1,000 | 2,4 (+0,4) | – |
| reggaeton | −10,4 (−0,2) | −9,0 (Drop) | +3,7 | 0,8 | −9,2 | −1,19 | 9,2 (+0,2) | 4,5 (−0,1) | −16,0 (−0,1) | −4,42 | −14,1 (+1,4) | 0,93 | 1,000 | 2,5 (+0,9) | – |
| retro_techno | −9,4 (−0,2) | −8,0 (Drop) | +7,7 | 2,7 | −8,2 | −1,19 | 8,3 (+0,2) | 6,5 (+0,3) | −15,9 (−0,1) | −4,40 (−0,09) | −19,3 (−0,1) | 0,98 | 1,000 | 2,1 (+0,7) | 9,7 (+1,0) |
| riddim | −11,3 | −7,5 (Drop) | +11,2 | 3,0 | −9,9 | −1,16 | 10,1 | 7,4 | −12,6 | −3,70 | −12,6 | 0,90 | 0,999 | 6,0 | 4,3 |
| speed_garage | −10,1 | −8,0 (Drop) | +7,2 | 3,0 | −8,3 | −1,18 | 8,9 | 6,6 | −16,0 (−0,2) | −5,31 (−0,07) | −17,7 (+1,2) | 0,97 | 1,000 | 2,4 | 6,2 (−0,2) |
| synthwave | −9,4 (−0,2) | −8,0 (Drop) | +5,5 | 1,5 | −8,3 | −1,19 | 8,2 (+0,2) | 4,6 (−0,1) | −18,0 (−0,1) | −4,93 (−0,07) | −15,8 (+0,6) | 0,95 | 1,000 | 2,1 (+0,7) | – |
| tech_house | −11,1 (−0,4) | −8,0 (Drop) | +6,9 | 3,0 | −9,1 | −1,17 | 9,9 (+0,4) | 5,2 (+1,2) | −15,1 | −5,13 | −16,2 (+1,4) | 0,95 | 1,000 | 2,2 (+1,1) | 5,8 (+1,9) |
| techno | −9,9 (−0,3) | −7,5 (Drop) | +7,4 | 3,0 | −8,4 | −1,19 | 8,7 (+0,3) | 4,2 (+0,8) | −18,4 (−0,1) | −5,30 (−0,06) | −13,1 (+1,4) | 0,91 | 1,000 | 2,1 (+0,7) | 4,8 (+0,7) |
| trance | −9,8 (−0,1) | −8,0 (Drop) | +8,2 | 3,0 | −8,3 | −1,18 | 8,6 (+0,1) | 3,5 (+0,3) | −14,0 (−0,1) | −4,64 | −9,6 (+0,4) | 0,80 | 0,999 | 2,5 (+0,3) | 1,7 (+0,3) |
| trap | −10,3 (−0,2) | −8,5 (Drop) | +4,8 | 0,9 | −8,7 | −1,19 | 9,1 (+0,2) | 7,4 (−0,1) | −18,9 (−0,1) | −4,87 (−0,06) | −19,2 (+0,5) | 0,98 | 1,000 | 2,6 (+1,0) | – |
| uk_garage | −9,6 (−0,3) | −8,0 (Drop) | +7,4 | 3,0 | −8,6 | −1,19 | 8,4 (+0,3) | 4,3 (+0,7) | −13,8 | −4,85 (+0,05) | −18,0 (+1,6) | 0,97 | 1,000 | 2,1 (+1,1) | – |

## Montées, Drops et Intro (S5b)

Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). Bloquant : Drop − fin de Montée ≥ +1 LU. Intro − Drop : l'Intro du début entière contre les sections Drop (ou Refrain), à titre indicatif.

| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Intro − Drop |
|---|---|---|---|---|
| acid_house | −9,9 / −9,9 | −8,0 / −7,9 | +2,0 / +1,9 (+3,8 / +3,7) | −2,2 (−0,7) |
| acid_techno | −9,2 / −9,3 | −7,4 / −7,4 | +1,8 / +1,9 (+3,5 / +3,5) | −1,4 (−1,3) |
| acidcore | −9,8 / −9,8 / −9,8 | −7,9 / −7,9 / −7,9 | +1,9 / +1,9 / +1,9 (+4,6 / +4,7 / +4,7) | −0,4 (−0,8) |
| afro_house | −12,6 / −12,7 | −9,0 / −9,0 | +3,6 / +3,6 (+4,6 / +4,7) | −2,1 (−0,5) |
| afrobeats | – | – | – | −9,8 (−0,1) |
| amapiano | −14,9 / −15,1 | −9,4 / −9,3 | +5,5 / +5,8 (+7,0 / +7,1) | −3,7 (+0,1) |
| boom_bap | – | – | – | −6,4 (+0,2) |
| deep_house | −13,0 / −13,2 | −9,1 / −8,9 | +3,9 / +4,3 (+5,0 / +5,6) | −2,9 (−0,5) |
| detroit | −10,6 / −10,7 | −8,1 / −8,2 | +2,5 / +2,5 (+4,8 / +4,7) | −1,3 (−0,8) |
| disco | −12,0 / −12,1 | −9,3 / −9,0 | +2,8 / +3,0 (+3,5 / +3,8) | −3,8 (−0,5) |
| dnb | −11,3 / −11,3 / −11,3 | −8,7 / −8,6 / −8,5 | +2,6 / +2,7 / +2,8 (+4,9 / +5,7 / +5,7) | −3,1 |
| drill | – | – | – | −8,7 (+0,4) |
| dubstep | −11,0 / −11,0 / −11,0 | −9,2 / −8,7 / −8,7 | +1,8 / +2,3 / +2,3 (+4,4 / +5,3 / +5,5) | −3,4 |
| gabber | −10,0 / −9,9 / −10,0 | −8,1 / −8,0 / −7,9 | +1,9 / +2,0 / +2,0 (+4,3 / +5,0 / +5,0) | −0,2 |
| grime | – | – | – | −3,9 (+0,1) |
| hard_techno | −12,7 / −12,8 / −12,7 | −9,6 / −8,8 / −8,8 | +3,1 / +4,0 / +3,9 (+5,7 / +7,1 / +7,0) | −3,8 |
| house | −11,8 / −12,0 | −8,5 / −8,4 | +3,4 / +3,5 (+6,5 / +6,7) | −1,8 (−1,1) |
| industrial_techno | −10,8 / −10,7 | −7,6 / −7,6 | +3,1 / +3,1 (+3,7 / +3,7) | −0,9 (−0,5) |
| jersey_club | – | – | – | −5,2 (−0,1) |
| jump_up | −10,8 / −10,8 / −10,8 | −9,3 / −8,7 / −8,8 | +1,6 / +2,1 / +2,1 (+4,7 / +5,2 / +5,3) | −4,8 |
| jungle | −11,7 / −12,4 / −11,6 | −8,1 / −8,3 / −8,2 | +3,5 / +4,1 / +3,4 (+6,3 / +7,1 / +6,5) | −3,8 |
| liquid_dnb | −10,7 / −10,6 / −10,6 | −8,4 / −8,3 / −8,3 | +2,3 / +2,3 / +2,3 (+5,3 / +5,3 / +5,2) | −3,2 |
| lofi | – | – | – | −8,2 (+0,3) |
| minimal | −11,1 / −11,3 | −8,3 / −8,2 | +2,7 / +3,0 (+6,2 / +6,4) | −2,1 (−0,5) |
| neurofunk | −10,9 / −10,9 / −10,9 | −9,0 / −8,8 / −8,7 | +1,9 / +2,1 / +2,2 (+4,8 / +5,3 / +5,3) | −2,3 |
| phonk | – | – | – | −10,9 (+0,3) |
| pop | – | – | – | −2,4 (−0,1) |
| reggaeton | – | – | – | −3,5 (+0,3) |
| retro_techno | −10,8 / −10,9 | −8,2 / −8,2 | +2,6 / +2,6 (+5,7 / +5,7) | −1,2 (−0,3) |
| riddim | −11,9 / −11,9 / −11,9 | −9,7 / −9,7 / −9,7 | +2,2 / +2,2 / +2,2 (+5,9 / +5,8 / +5,9) | −4,2 |
| speed_garage | −11,0 / −10,9 | −8,3 / −8,2 | +2,6 / +2,7 (+5,8 / +5,9) | −2,3 |
| synthwave | – | – | – | −3,2 (+0,2) |
| tech_house | −11,8 / −11,9 | −9,0 / −8,9 | +2,9 / +3,0 (+6,0 / +6,2) | −3,8 (−0,7) |
| techno | −10,8 / −10,6 | −8,3 / −8,3 | +2,5 / +2,3 (+5,7 / +5,5) | −2,6 (−0,5) |
| trance | −10,7 / −10,8 | −8,4 / −8,2 | +2,3 / +2,6 (+5,2 / +5,4) | −2,8 (−0,4) |
| trap | – | – | – | −6,1 (+0,1) |
| uk_garage | – | – | – | −2,2 (−0,5) |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,3 | −3,0 | −13,9 | −8,8 | −12,0 | −17,6 |
| acid_techno | −4,9 | −2,9 | −14,1 | −12,3 | −13,4 | −17,7 |
| acidcore | −11,2 | −1,6 | −10,7 | −11,8 | −12,7 | −14,9 |
| afro_house | −11,1 | −1,2 | −9,8 | −14,5 | −18,7 | −18,9 |
| afrobeats | −3,9 | −3,7 | −9,4 | −14,8 | −18,4 | −23,4 |
| amapiano | −15,5 | −1,2 | −9,9 | −11,1 | −17,9 | −18,7 |
| boom_bap | −4,1 | −3,1 | −11,7 | −17,4 | −16,5 | −19,4 |
| deep_house | −4,1 | −3,8 | −11,5 | −10,1 | −21,5 | −18,2 |
| detroit | −4,4 | −4,1 | −13,5 | −9,2 | −12,4 | −16,3 |
| disco | −8,5 | −3,1 | −6,8 | −10,7 | −13,6 | −16,0 |
| dnb | −5,8 | −4,2 | −8,0 | −9,4 | −13,3 | −14,9 |
| drill | −4,8 | −3,1 | −10,4 | −12,7 | −17,2 | −20,0 |
| dubstep | −4,1 | −5,8 | −7,7 | −8,9 | −15,6 | −16,4 |
| gabber | −5,4 | −5,6 | −6,7 | −8,1 | −14,4 | −14,8 |
| grime | −5,0 | −4,8 | −11,6 | −8,1 | −10,6 | −15,0 |
| hard_techno | −9,1 | −5,8 | −5,4 | −6,4 | −13,5 | −13,2 |
| house | −5,4 | −3,0 | −9,8 | −11,6 | −17,9 | −16,3 |
| industrial_techno | −6,1 | −3,3 | −10,5 | −8,0 | −16,9 | −17,6 |
| jersey_club | −3,6 | −5,2 | −10,9 | −8,4 | −18,7 | −19,0 |
| jump_up | −6,2 | −7,2 | −7,4 | −6,1 | −11,1 | −12,7 |
| jungle | −6,1 | −3,4 | −9,4 | −10,8 | −12,1 | −14,7 |
| liquid_dnb | −5,2 | −3,8 | −10,5 | −9,5 | −14,0 | −14,6 |
| lofi | −4,2 | −2,8 | −12,9 | −17,4 | −18,7 | −22,4 |
| minimal | −5,8 | −2,8 | −9,4 | −11,3 | −18,4 | −20,6 |
| neurofunk | −3,6 | −5,9 | −9,1 | −12,9 | −11,5 | −12,6 |
| phonk | −4,2 | −4,2 | −10,0 | −10,0 | −16,4 | −19,8 |
| pop | −5,4 | −2,9 | −11,2 | −11,1 | −15,4 | −18,1 |
| reggaeton | −6,5 | −3,4 | −9,5 | −9,2 | −12,7 | −16,0 |
| retro_techno | −4,9 | −3,3 | −11,3 | −11,7 | −13,6 | −15,9 |
| riddim | −4,4 | −6,2 | −9,0 | −7,7 | −14,6 | −12,6 |
| speed_garage | −5,8 | −3,1 | −9,2 | −11,9 | −15,7 | −16,0 |
| synthwave | −5,9 | −3,0 | −10,4 | −10,0 | −14,2 | −18,0 |
| tech_house | −5,2 | −3,8 | −10,1 | −9,0 | −16,2 | −15,1 |
| techno | −4,7 | −4,4 | −7,7 | −10,7 | −16,9 | −18,4 |
| trance | −6,2 | −4,8 | −7,5 | −7,6 | −15,3 | −14,0 |
| trap | −3,1 | −4,7 | −12,8 | −11,2 | −16,4 | −18,9 |
| uk_garage | −6,9 | −3,3 | −10,6 | −8,2 | −14,3 | −13,8 |
| **médiane** | −5,4 | −3,4 | −10,0 | −10,1 | −15,3 | −16,3 |

## Largeur et mouvement (S4)

Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.

| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |
|---|---|---|---|---|---|
| acid_house | −17,5 (+6,8) | 0,96 | 0,998 | – | −3,0 |
| acid_techno | −20,4 (+10,3) | 0,98 | 0,998 | – | −2,9 |
| acidcore | −17,8 (+18,2) | 0,97 | 0,995 | – | −1,6 (+0,2) |
| afro_house | −17,1 (+0,8) | 0,96 | 1,000 | 31,2 | −1,2 |
| afrobeats | −16,0 (+1,1) | 0,95 | 1,000 | 22,9 | −3,7 (+0,1) |
| amapiano | −17,9 (+2,5) | 0,97 | 1,000 | 30,2 | −1,2 (−0,1) |
| boom_bap | −20,8 (+0,8) | 0,98 | 0,999 | 23,8 | −3,1 |
| deep_house | −17,1 (+0,4) | 0,96 | 0,999 | 32,3 | −3,8 |
| detroit | −18,5 (+2,4) | 0,97 | 1,000 | 19,3 | −4,1 (−0,1) |
| disco | −15,7 (+0,5) | 0,95 | 1,000 | 14,7 | −3,1 |
| dnb | −12,2 | 0,89 | 1,000 | 13,8 | −4,2 |
| drill | −19,9 (+0,8) | 0,98 | 1,000 | 25,1 | −3,1 |
| dubstep | −12,3 | 0,89 | 1,000 | 28,7 | −5,8 |
| gabber | −12,6 (+0,4) | 0,90 | 0,999 | 10,5 | −5,6 |
| grime | −15,5 (+0,3) | 0,94 | 1,000 | 57,1 | −4,8 |
| hard_techno | −8,8 (+0,5) | 0,77 | 0,997 | 8,0 | −5,8 |
| house | −18,2 (+0,7) | 0,97 | 1,000 | 31,2 | −3,0 |
| industrial_techno | −16,0 (+0,9) | 0,95 | 0,997 | 14,5 | −3,3 |
| jersey_club | −17,2 | 0,96 | 1,000 | 11,1 | −5,2 (+0,1) |
| jump_up | −9,4 | 0,79 | 0,999 | 29,1 | −7,2 |
| jungle | −12,4 | 0,89 | 1,000 | 13,2 | −3,4 |
| liquid_dnb | −15,1 | 0,94 | 1,000 | 26,3 | −3,8 |
| lofi | −23,5 (+3,2) | 0,99 | 1,000 | 23,4 | −2,8 |
| minimal | −12,7 (−0,1) | 0,90 | 0,999 | – | −2,8 |
| neurofunk | −15,0 | 0,94 | 1,000 | 19,0 | −5,9 |
| phonk | −23,1 (+1,1) | 0,99 | 1,000 | 20,0 | −4,2 |
| pop | −17,7 (+1,2) | 0,97 | 1,000 | 16,7 | −2,9 |
| reggaeton | −14,1 (+1,4) | 0,93 | 1,000 | 18,8 | −3,4 |
| retro_techno | −19,3 (−0,1) | 0,98 | 1,000 | 50,3 | −3,3 (+0,1) |
| riddim | −12,6 | 0,90 | 0,999 | 0,0 | −6,2 |
| speed_garage | −17,7 (+1,2) | 0,97 | 1,000 | 7,9 | −3,1 |
| synthwave | −15,8 (+0,6) | 0,95 | 1,000 | 16,4 | −3,0 |
| tech_house | −16,2 (+1,4) | 0,95 | 1,000 | 5,4 | −3,8 (−0,1) |
| techno | −13,1 (+1,4) | 0,91 | 1,000 | 29,2 | −4,4 (−0,1) |
| trance | −9,6 (+0,4) | 0,80 | 0,999 | 12,4 | −4,8 (−0,1) |
| trap | −19,2 (+0,5) | 0,98 | 1,000 | 34,7 | −4,7 (+0,2) |
| uk_garage | −18,0 (+1,6) | 0,97 | 1,000 | 13,9 | −3,3 |

## Basse au téléphone (docs/hits/basses-qui-tapent.md § 8)

Les basses du morceau (pistes de basse, Sub, Sub drop, pad de la 808 : `META bass`) rendues seules avant la chaîne master, au Drop (ou au Refrain), à travers un « téléphone » : passe-haut de Butterworth d'ordre 4 à 200 Hz (bon téléphone, B1) ou 300 Hz (téléphone moyen, B2), puis la pondération K. Perte de sonie contre la basse en pleine bande : plus proche de 0, mieux elle passe. B3 : la basse filtrée à 300 Hz contre le morceau filtré pareil. B4 : énergie 200–800 Hz contre 30–100 Hz. B5 : RMS 5 ms le plus fort des 40 premières ms d'une note contre la médiane 80–250 ms (notes de 250 ms au moins). B7, B8 : le morceau exporté. B9 : RMS de la basse 0–50 ms après chaque kick contre 100–200 ms après. B12 : le morceau avant master avec l'exciteur contre sans.

| Style | B1 tél. 200 | B2 tél. 300 | B3 place | B4 harm. | B5 attaque (notes) | B6 écart | B7 sub | B8 infra | B9 kick | B12 exciteur |
|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | – | – | – | – | – (0) | – | −6,3 (−0,1) | −24,0 (−0,2) | – | – |
| acid_techno | – | – | – | – | – (0) | – | −5,0 (−0,1) | −26,8 (−0,1) | – | – |
| acidcore | – | – | – | – | – (0) | – | −11,4 (−0,5) | −28,7 (+0,2) | – | – |
| afro_house | −6,4 (+0,8) | −10,0 (+0,9) | −8,2 (+1,0) | −4,9 (+1,4) | +5,8 (115) | 10,75 (−1,10) | −12,3 (−0,1) | −26,0 | −6,0 (+0,5) | +0,06 |
| afrobeats | −6,2 (+0,5) | −10,9 (+0,7) | −6,1 (+0,6) | −6,1 (+0,9) | +4,7 (+0,1) (63) | 21,91 (+8,20) | −4,7 (−0,2) | −27,9 (−0,1) | −0,2 | +0,16 (+0,07) |
| amapiano | −7,3 (+0,6) | −10,8 (+0,8) | −6,7 (+0,6) | −5,9 (+1,2) | +7,8 (+0,1) (118) | 2,73 (−0,11) | −21,9 (−0,2) | −29,5 (−0,1) | −11,2 (+0,4) | +0,22 (+0,07) |
| boom_bap | −6,9 (+0,2) | −12,1 (+0,5) | −7,4 (+0,5) | −6,4 (+0,4) | +4,1 (+0,2) (52) | 35,20 (+6,27) | −5,2 (−0,2) | −25,2 | +0,1 (+0,1) | +0,06 |
| deep_house | −6,8 (+0,5) | −11,3 (+0,8) | −9,3 (+0,9) | −6,2 (+0,9) | +2,7 (−0,1) (108) | 27,75 (+7,89) | −4,1 (−0,1) | −25,3 (−0,1) | +1,1 (+0,1) | +0,11 (+0,05) |
| detroit | −6,1 | −10,5 | −13,0 (−0,3) | −5,3 | +85,4 (189) | 43,25 | −4,8 (−0,1) | −26,3 (−0,1) | −1,3 | +0,03 |
| disco | −2,5 | −5,7 | −4,5 | 12,5 (+0,1) | +5,8 (−0,1) (114) | 3,73 | −8,6 (−0,1) | −24,9 (−0,1) | +2,3 (−0,1) | +0,03 |
| dnb | – | – | – | – | – (0) | – | −7,0 | −26,4 | – | – |
| drill | −5,9 (+0,3) | −8,6 (+0,2) | −4,5 (+0,2) | −6,7 (+0,6) | +2,4 (−0,1) (24) | 0,23 (−0,07) | −5,6 (−0,2) | −25,2 | +0,8 (+0,1) | +0,03 |
| dubstep | – | – | – | – | – (0) | – | −4,6 | −25,5 | – | – |
| gabber | −9,8 | −12,8 | −33,2 | −13,3 | – (0) | – | −5,5 (−0,1) | −28,8 (−0,1) | +5,2 | +0,00 |
| grime | −4,5 | −6,4 | −8,8 | −6,4 | +5,2 (112) | 2,66 | −6,3 | −20,1 | −0,7 | +0,07 |
| hard_techno | −10,3 | −13,4 | −34,1 (−0,1) | −13,9 | – (0) | – | −9,2 (−0,2) | −26,6 (−0,1) | +4,6 | +0,00 |
| house | −6,0 (+0,3) | −10,4 (+0,6) | −9,4 (+0,8) | −3,6 (+0,6) | +85,5 (+0,1) (108) | 48,26 (+0,47) | −5,6 (−0,1) | −24,6 | −1,4 (+0,1) | +0,10 |
| industrial_techno | −11,5 | −15,4 | −34,6 (−0,1) | −15,7 | – (0) | – | −6,1 | −29,6 | +4,6 | +0,00 |
| jersey_club | −6,6 (+0,2) | −10,6 (+0,2) | −11,1 (+0,4) | −8,4 (+0,4) | +3,8 (−0,1) (36) | 0,70 (−0,56) | −4,7 (−0,1) | −24,6 (−0,1) | +2,2 | +0,02 |
| jump_up | – | – | – | – | – (0) | – | −6,4 | −23,6 | – | – |
| jungle | – | – | – | – | – (0) | – | −6,7 | −23,8 | – | – |
| liquid_dnb | – | – | – | – | – (0) | – | −5,4 | −27,0 | – | – |
| lofi | – | – | – | – | – (0) | – | −4,6 (−0,1) | −24,6 (−0,1) | – | – |
| minimal | – | – | – | – | – (0) | – | −5,9 | −26,8 (−0,1) | – | – |
| neurofunk | – | – | – | – | – (0) | – | −3,6 | −23,5 | – | – |
| phonk | – | – | – | – | – (0) | – | −4,3 (−0,1) | −21,7 (+0,1) | – | – |
| pop | – | – | – | – | – (0) | – | −7,1 (−0,2) | −21,0 (−0,2) | – | – |
| reggaeton | – | – | – | – | – (0) | – | −6,5 | −26,5 (−0,1) | – | – |
| retro_techno | – | – | – | – | – (0) | – | −6,3 (−0,1) | −21,9 (−0,2) | – | – |
| riddim | – | – | – | – | – (0) | – | −7,3 | −19,6 | – | – |
| speed_garage | – | – | – | – | – (0) | – | −6,6 (−0,2) | −25,8 (−0,2) | – | – |
| synthwave | – | – | – | – | – (0) | – | −6,0 (−0,1) | −27,6 (−0,3) | – | – |
| tech_house | – | – | – | – | – (0) | – | −5,3 (−0,1) | −22,3 | – | – |
| techno | – | – | – | – | – (0) | – | −4,7 (−0,2) | −26,1 (−0,2) | – | – |
| trance | – | – | – | – | – (0) | – | −6,3 (−0,1) | −26,5 (−0,1) | – | – |
| trap | – | – | – | – | – (0) | – | −4,8 (−0,2) | −24,9 (−0,1) | – | – |
| uk_garage | – | – | – | – | – (0) | – | −7,2 | −25,9 (−0,1) | – | – |

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −33,3 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −36,9 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,0 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −40,1 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −52,0 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −44,5 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −37,0 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −43,0 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −44,0 | 335,2 |
| disco | 0 | 0 | 0,0000 | −45,5 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −32,9 | 297,9 |
| drill | 0 | 0 | 0,0000 | −41,2 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −41,4 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −38,5 | 296,2 |
| grime | 0 | 0 | 0,0000 | −35,5 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −35,2 | 358,4 |
| house | 0 | 0 | 0,0000 | −44,3 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −36,8 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −40,9 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −32,4 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −37,7 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −32,6 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −39,9 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −42,6 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −33,7 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −42,3 | 192,0 |
| pop | 0 | 0 | 0,0000 | −42,5 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −38,2 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −38,5 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −40,7 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −44,0 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −40,4 | 192,0 |
| tech_house | 0 | 0 | 0,0000 | −44,5 | 182,9 |
| techno | 0 | 0 | 0,0000 | −41,0 | 320,0 |
| trance | 0 | 0 | 0,0000 | −43,6 | 306,1 |
| trap | 0 | 0 | 0,0000 | −42,7 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −35,5 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
f6f325198dd3ea066d9fc213bdf26547dd8b75b724857caa1b59d1816d49194a  acid_house.wav
5ec801209f0dc78f185ec22ec9cbea92620ff127111e9e2493f27084ba415d08  acid_techno.wav
457ac81eb53c22d317841d6b5d98f97b409e233991fe8f3ba0983f6b9e2c340d  acidcore.wav
8b090693fd99075fb94284bc9c2934f968e2bf620dc84a75c0e11730df8579bf  afro_house.wav
988828be129feb7da63b72e734a613b6ec5a2e620cc6ff6f5692ef7689619b5a  afrobeats.wav
c714a37d623c4d5e2606aff440fe45857e81318db4fd4522f16d6bee7ca6ceb9  amapiano.wav
b8915c2b0dd166e1033081172276d1f7fd356b09beda1aa1e6e40ff417c9661c  boom_bap.wav
dc5594e5c24cf921bfd0b93d9cfb128b4ed28f2e9eb46dd31170c0ee3b20741d  deep_house.wav
16300117aed30187c7fc6785142095ba5d5b86a7d60dceb0583ea560a12c485f  detroit.wav
e03be2474e1e887fdce1e993d89d17ed429cbf452a69ce1ee7b5d0d8dc23c04c  disco.wav
01b27c53d02a6074d97fdbb83d3dc764dfcf72e9b5ef90b838cbb6708cde1f4a  dnb.wav
611d31f27b40f4c93ba4fa67814234102da92001002c3acfca753ce4e553efa8  drill.wav
b702b2afcc63f0457ee6b15cfab447d7ba7709c66ffdfd4b422f1a10b38790eb  dubstep.wav
e249e571d0dd78ad4935349b8a91847ff2e9ee3ed4b657e749f0c72a9e318549  gabber.wav
5a79f0af619dab33bc1d82891bb2f40b1920ac6cc3a96c0ce51e8acc9820c5db  grime.wav
763e671448dfe1131ea9cf12468abbbfa43481f24ad5a0b9171b9de34b6d86e5  hard_techno.wav
3ab65add249a6f2fcc6bc1749b70e585a3de566449cad9e864e87ee220b9e551  house.wav
899a7a23269788a4ba0099ff89a1dc40c791130ecb84fed309d6af41c4ef11cc  industrial_techno.wav
85ed3d551d87f003647aad414ff739f21fce15825d8fc433e22365413bd92304  jersey_club.wav
e05283f550f30acb2c17836c8242b286a1f8791aa232d610250c08a993bd4467  jump_up.wav
c08e41efe152e2f7598ae568bf25412801d7b37f118722adb9ee1dc018db1bb0  jungle.wav
9b5c89dab3860e31ddbea77fe927271c38e696518955fdf55147bad4734974c0  liquid_dnb.wav
60dfbd2eb5f29c9fb1a5bc933ff0ec6907e5dcefb8d5ed32ab7220669a27a621  lofi.wav
01f0610ececa334452fde38c79cee69f5baab23f33d7299aa310650075f0b870  minimal.wav
c3266933564ca09cbf833752dc746b2aeee2e6b24130eecd452e29ad920f332e  neurofunk.wav
fab240680181c58ac6cee7497606cb3dff589ff7f512b9606ccca8bfc629a047  phonk.wav
02b186120d4b603c8900bb16c4a15d6a6df7726f94ebaad4cae1fce2340fa7ef  pop.wav
56c89e09d646cb9d0b146ea0366c680d786638234963a395655767b7a2060c14  reggaeton.wav
58aacd4fac4a72540ef4250f0cd2ed7cf8b67e69f379dc75655c9d7a79005ad3  retro_techno.wav
eec1a5bc11371e9aca49731fa0cc3e43d94658a59aaf1dc367f797dd3ef4b49f  riddim.wav
f19b6face918bc7ced6c2a359b91f495ad84a68010d05756ec5be51e8f55261b  speed_garage.wav
51b10c1daf63b25eff0d01677622bb9ed3509f3d9f6d4a5579fe6812f1932429  synthwave.wav
e9618fa9aa1bda787104d61679ae1f9c86577219029fd92c537fcf6eb145579a  tech_house.wav
be07ebd6b746f92881e8bcd3d4e44f120ad4e459ba75d783a9fd392c7e6c4361  techno.wav
34e30823490d60e9144e2040808f031fd6bf3149ef36d33bb6786b1c9a651d3c  trance.wav
3d94697e8229c584ec5307c6c66fdd1a4734a1d615fcac6ad14b0c4d0a3daf6e  trap.wav
e2bb2c2feed319241361ec496b209522cfcb1ae1d986c0674f8779e78c950e75  uk_garage.wav
```
