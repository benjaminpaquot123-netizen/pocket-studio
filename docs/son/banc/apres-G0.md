# Banc après G0 (Drops variés, Drop 1A, chaînes et pans par rôle, revue)

37 morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` (libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale (Drop, sinon Refrain) : bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.

## Résumé (médiane sur les styles)

| Mesure | Avant | Après | Écart | Min – max (après) | Cible |
|---|---|---|---|---|---|
| LUFS-I (morceau) | −10,1 | −10,1 | −0,0 | −13,9 – −8,2 | cible du genre ±1 (avant S5) |
| LUFS du Drop / Refrain (sections entières) | −8,7 | −8,7 | +0,0 | −12,3 – −7,1 | −8 à −6 en club |
| LUFS de la partie la plus forte (court terme, 20 % du haut) | −8,5 | −8,5 | +0,0 | −12,1 – −7,0 | cible du style ±1 (S5) |
| Écart à la cible (LU) | −0,1 | −0,1 | +0,1 | −2,3 – −0,0 | ±1 |
| True peak (dBTP) | −1,18 | −1,19 | −0,01 | −1,19 – −1,16 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 8,9 | 8,9 | +0,0 | 7,0 – 12,7 | 6 à 10 en club |
| LRA (LU) | 4,9 | 4,8 | −0,0 | 2,1 – 9,6 | 3 à 9 en club |
| Facteur de crête du Drop (dB) | 8,8 | 8,8 | +0,1 | 6,2 – 11,7 | ≥ 6 |
| Air > 6 kHz (dB rel.) | −17,4 | −16,9 | +0,5 | −22,4 – −12,8 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −5,03 | −5,00 | +0,03 | −6,27 – −3,70 | −4,5 ±0,7 |
| Grave 60–250 Hz au Drop (dB rel.) | −3,7 | −3,7 | +0,0 | −7,2 – −0,9 | ≥ −6 en bass music |
| Mouvement du lead (centroïde, ±%) | 22,2 | 22,9 | +0,7 | 0,1 – 57,5 | ≥ 15 |
| Côté/centre (dB) | −16,8 | −16,7 | +0,1 | −36,0 – −8,6 | −15 à −9 |
| Corrélation L/R | 0,96 | 0,96 | −0,00 | 0,76 – 1,00 | 0,3 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | +0,000 | 0,997 – 1,000 | ≥ 0,95 |
| Perte en mono (LU) | −0,13 | −0,14 | −0,01 | −0,58 – 0,00 | ≥ −1,5 |
| Drop − Groove, Couplet ou Montée (LU) | 1,8 | 1,9 | +0,0 | 0,5 – 6,2 | ≥ +2 |
| Drop − Break (LU) | 4,0 | 4,1 | +0,1 | 0,6 – 10,5 | ≥ +4 |
| Drop − Groove avant master (LU) | – | – | – | – – – | ≥ +2 |
| Drop − Break avant master (LU) | – | – | – | – – – | ≥ +4 |
| True peak du M4A décodé, premier encodage (dBTP) | – | – | – | – – – | – |
| True peak du M4A décodé, après la vérification de l'appli (dBTP) | – | – | – | – – – | ≤ −1,0 |
| Montée : sa montée sur ses 4 dernières mesures (LU) | 3,5 | 3,5 | +0,0 | 2,1 – 5,4 | ≥ +2 |
| Montée avant master (LU) | – | – | – | – – – | ≥ +2 |
| Drop − fin de Montée (LU, le plus petit du morceau) | 2,3 | 2,3 | +0,0 | 1,4 – 6,2 | ≥ +1 (+1,5 visé) |
| Drop − fin de Montée avant master (LU) | – | – | – | – – – | – |
| Intro − Drop / Refrain (LU) | −2,8 | −2,8 | +0,0 | −10,2 – 0,4 | ≥ −8 |
| Variation entre Drops (part des mesures qui diffèrent, paire la plus proche) | 0,06 | 0,50 | +0,44 | 0,44 – 1,00 | ≥ 0,25 |
| Continu (moyenne absolue) | 0,0000 | 0,0000 | +0,0000 | 0,0000 – 0,0000 | ≤ 0,001 |
| Gain de la cible (dB) | 7,0 | 7,0 | +0,0 | −1,0 – 11,0 | – |
| Perte dans la chaîne (LU) | 3,0 | 2,7 | −0,2 | 0,1 – 3,0 | ≤ 3 (S5, au Drop ; 6 avant) |

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 30 sur 37 | dnb (−1,1, plafonné), dubstep (−1,3, plafonné), hard_techno (−2,3, plafonné), jump_up (−1,2, plafonné), neurofunk (−1,3, plafonné), riddim (−2,2, plafonné), techno (−1,0, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 25 sur 37 | acid_house (+2,4), acid_techno (+3,0), acidcore (+0,6), deep_house (+3,7), dubstep (+3,5), gabber (+2,7), hard_techno (+2,3), house (+4,0), minimal (+2,8), tech_house (+3,9), techno (+4,0), trance (+1,5) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 16 sur 37 | acid_house (+0,7), acid_techno (+1,0), acidcore (+0,9), afrobeats (+1,1), boom_bap (+1,4), detroit (+0,8), disco (+1,9), house (+0,5), industrial_techno (+1,7), jersey_club (+1,3), jungle (+1,2), minimal (+1,9), phonk (+0,7), reggaeton (+1,2), retro_techno (+1,5), synthwave (+1,0), tech_house (+1,1), techno (+1,8), trance (+1,9), trap (+1,5), uk_garage (+1,0) |
| Chaque Montée monte d'au moins +2 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 37 sur 37 | – |
| Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +1 LU (exporté ; sans Montée : –) (bloquant) | 37 sur 37 | – |
| Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 34 sur 37 | drill (−9,0), lofi (−10,2), phonk (−8,4) |
| Variation entre Drops ≥ 0,25 (modèle : aucune paire de Drops identique) | 37 sur 37 | – |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −9,2 (−0,1) | −8,0 (Drop) | +7,0 | 2,3 | −8,1 | −1,18 | 8,0 (+0,1) | 2,7 (+0,3) | −17,5 (+0,1) | −4,62 | −24,3 (+0,1) | 0,99 | 1,000 | 0,7 (+0,1) | 2,4 (+0,1) |
| acid_techno | −8,2 (+0,1) | −7,5 (Drop) | +7,8 | 2,4 | −7,7 (+0,1) | −1,18 | 7,0 (−0,1) | 3,1 | −17,7 (+0,1) | −4,73 | −30,6 (+0,5) | 1,00 | 1,000 | 1,0 | 3,0 |
| acidcore | −8,3 (+0,2) | −7,5 (Drop) | +2,7 | 2,7 | −8,0 (+0,2) | −1,19 | 7,2 (−0,2) | 2,8 (+0,4) | −14,8 (+0,2) | −4,83 | −36,0 (+1,2) | 1,00 | 1,000 | 0,9 (+0,2) | 0,6 (+0,1) |
| afro_house | −10,5 (+0,1) | −9,0 (Drop) | +5,8 | 0,9 | −9,2 | −1,19 | 9,3 | 4,8 | −18,7 (+0,3) | −5,48 | −17,0 (+0,2) | 0,96 | 1,000 | 2,0 (−0,1) | 4,7 |
| afrobeats | −10,5 (−0,2) | −9,0 (Drop) | +6,1 | 3,0 | −9,6 (−0,2) | −1,18 (−0,05) | 9,3 (+0,1) | 4,7 (−0,2) | −22,4 (+1,8) | −5,72 (+0,25) | −15,7 (+0,5) | 0,95 | 1,000 | 1,1 (+0,1) | – |
| amapiano | −11,9 | −9,0 (Drop) | +5,8 | 0,8 | −9,4 | −1,19 | 10,7 | 9,0 (+0,1) | −19,1 (+0,3) | −6,26 (+0,06) | −19,4 (−0,1) | 0,98 | 1,000 | 5,0 | 10,5 (+0,1) |
| boom_bap | −11,5 (+0,2) | −10,0 (Drop) | +3,9 | 2,2 | −10,4 | −1,19 | 10,3 (−0,2) | 4,9 (−0,5) | −19,1 (+0,9) | −5,24 (+0,17) | −20,2 (−0,9) | 0,98 | 1,000 | 1,4 (−0,5) | – |
| deep_house | −10,8 | −9,0 (Drop) | +5,0 | 1,1 | −9,1 | −1,19 | 9,6 | 4,3 (+0,2) | −18,2 (+0,5) | −5,95 (+0,11) | −16,3 (+0,4) | 0,95 | 0,999 | 2,2 | 3,7 (+0,1) |
| detroit | −9,1 (−0,1) | −8,0 (Drop) | +5,7 | 1,8 | −8,4 (−0,1) | −1,19 | 7,9 (+0,1) | 4,8 (+0,1) | −16,1 (+0,5) | −3,86 (+0,10) | −19,1 (+0,5) | 0,98 | 1,000 | 0,8 (+0,1) | 4,6 (+0,1) |
| disco | −11,2 (−0,1) | −9,0 (Drop) | +3,6 | 0,9 | −9,2 | −1,19 | 10,0 | 5,6 (+0,2) | −16,9 (+0,3) | −5,54 | −14,1 (+0,2) | 0,93 | 1,000 | 1,9 (+0,1) | 5,5 (+0,3) |
| dnb | −10,2 | −7,5 (Drop) | +7,3 | 3,0 | −8,7 | −1,19 | 9,0 | 5,4 (+0,1) | −15,3 (+0,2) | −4,90 | −12,3 (−0,1) | 0,89 | 1,000 | 5,4 | 4,5 |
| drill | −10,2 | −8,5 (Drop) | +4,5 | 0,5 | −8,7 | −1,19 | 9,0 | 9,6 (+0,1) | −19,8 (+0,3) | −5,18 | −20,0 (+0,4) | 0,98 | 1,000 | 2,0 | – |
| dubstep | −10,4 | −7,5 (Drop) | +8,5 | 3,0 | −9,0 (−0,1) | −1,19 | 9,2 | 6,2 (−0,1) | −17,2 (+0,2) | −4,77 | −10,9 (−0,3) | 0,85 | 1,000 | 5,1 (−0,1) | 3,5 |
| gabber | −8,8 | −7,0 (Drop) | +8,9 | 3,0 | −8,0 | −1,19 | 7,6 | 3,3 (+0,2) | −16,2 (+0,4) | −4,83 | −12,1 (+0,2) | 0,89 | 0,999 | 4,8 | 2,7 (+0,1) |
| grime | −10,3 | −8,5 (Drop) | +4,0 | 0,9 | −8,8 | −1,19 | 9,1 | 5,3 (+0,2) | −14,3 (−0,3) | −3,70 | −15,3 (−0,2) | 0,94 | 1,000 | 2,4 (+0,2) | – |
| hard_techno | −10,1 | −6,0 (Drop) | +9,0 | 3,0 | −8,6 | −1,19 | 8,9 | 4,3 | −14,3 (+0,2) | −4,42 (+0,18) | −9,9 (+0,1) | 0,81 | 0,997 | 2,7 (+0,1) | 2,3 (+0,1) |
| house | −9,4 | −8,0 (Drop) | +7,3 | 3,0 | −8,5 | −1,18 | 8,2 (−0,1) | 3,8 | −16,7 (+0,2) | −5,52 | −19,8 (+0,1) | 0,98 | 1,000 | 0,5 | 4,0 |
| industrial_techno | −8,6 | −7,5 (Drop) | +4,4 | 0,4 | −7,7 | −1,19 | 7,4 | 4,4 (+0,1) | −17,6 (+0,4) | −5,19 (+0,07) | −16,7 (+0,1) | 0,96 | 0,997 | 1,7 | 6,5 (+0,1) |
| jersey_club | −9,8 | −8,5 (Drop) | +6,1 | 2,1 | −8,7 | −1,18 | 8,6 | 7,0 (+0,1) | −19,0 (+0,1) | −5,05 | −17,0 (+0,4) | 0,96 | 1,000 | 1,3 | – |
| jump_up | −10,7 | −7,5 (Drop) | +8,4 | 3,0 | −8,9 | −1,18 | 9,5 | 6,0 | −13,1 (+0,4) | −3,93 (+0,11) | −8,6 (−0,3) | 0,76 | 0,999 | 4,6 | 4,8 |
| jungle | −9,8 | −7,5 (Drop) | +7,8 | 3,0 | −8,2 | −1,17 | 8,6 | 5,7 | −14,4 | −4,41 | −14,8 (+0,1) | 0,94 | 1,000 | 1,2 | 6,3 |
| liquid_dnb | −10,4 (−0,1) | −8,0 (Drop) | +7,4 | 3,0 | −8,7 | −1,19 | 9,2 (+0,1) | 6,9 (+0,1) | −15,1 (+0,3) | −4,93 (+0,06) | −14,5 | 0,93 | 1,000 | 5,3 (+0,1) | 6,0 (+0,2) |
| lofi | −13,9 | −12,0 (Drop) | −1,0 | 0,1 | −12,3 | −1,19 | 12,7 (−0,1) | 9,3 (−0,1) | −22,3 (+0,4) | −5,78 | −26,2 (−0,6) | 0,99 | 1,000 | 2,7 | – |
| minimal | −9,9 (+0,1) | −8,0 (Drop) | +7,8 | 3,0 | −8,8 (+0,1) | −1,19 | 8,8 (−0,1) | 3,3 (−0,1) | −21,5 (+0,8) | −6,27 (+0,16) | −11,5 (+0,1) | 0,87 | 0,998 | 1,9 | 2,8 (−0,1) |
| neurofunk | −10,3 | −7,5 (Drop) | +9,3 | 3,0 | −9,0 | −1,19 | 9,1 | 8,3 (+0,1) | −13,4 (+0,2) | −3,99 | −12,9 | 0,90 | 0,999 | 6,0 (+0,1) | 5,7 |
| phonk | −8,2 | −7,0 (Drop) | +8,2 | 2,9 | −7,1 | −1,16 | 7,0 | 8,8 (+0,3) | −19,5 (+0,1) | −4,88 | −22,8 (−0,2) | 0,99 | 1,000 | 0,7 | – |
| pop | −10,3 | −9,0 (Drop) | +3,7 | 1,4 | −9,3 (−0,1) | −1,18 | 9,1 | 3,1 (−0,1) | −19,0 (+0,5) | −5,48 | −17,7 (+1,4) | 0,97 | 1,000 | 2,1 (−0,1) | – |
| reggaeton | −10,1 (−0,2) | −9,0 (Drop) | +4,6 | 1,7 | −9,3 (−0,1) | −1,19 | 8,9 (+0,2) | 3,5 (+0,5) | −16,5 (+1,0) | −4,15 (+0,14) | −15,7 (−0,4) | 0,95 | 1,000 | 1,2 (+0,2) | – |
| retro_techno | −9,2 | −8,0 (Drop) | +7,6 | 2,4 | −8,2 (+0,2) | −1,18 | 8,1 | 6,2 (+0,6) | −16,0 (+0,4) | −4,34 | −19,1 (+0,3) | 0,97 | 0,999 | 1,5 (+0,3) | 7,7 (+0,7) |
| riddim | −11,3 (+0,1) | −7,5 (Drop) | +11,0 | 3,0 | −9,9 (+0,1) | −1,17 | 10,2 (−0,1) | 7,3 | −12,8 (+0,2) | −3,72 (−0,12) | −12,8 (+0,2) | 0,90 | 0,999 | 6,2 | 4,1 (+0,1) |
| speed_garage | −10,1 | −8,0 (Drop) | +7,4 | 3,0 | −8,3 | −1,18 | 8,9 | 6,3 | −15,8 (+0,1) | −5,14 | −19,1 (+0,1) | 0,98 | 1,000 | 2,0 | 6,2 (+0,1) |
| synthwave | −8,8 | −8,0 (Drop) | +6,4 | 2,5 | −8,2 | −1,19 | 7,6 | 2,1 (+0,2) | −18,7 (+0,3) | −4,88 (+0,06) | −15,3 (−0,2) | 0,94 | 1,000 | 1,0 (+0,1) | – |
| tech_house | −10,6 | −8,0 (Drop) | +7,0 | 3,0 | −9,0 | −1,16 | 9,4 | 4,1 | −15,4 (+0,1) | −5,29 | −18,2 | 0,97 | 1,000 | 1,1 | 3,9 |
| techno | −9,9 (+0,1) | −7,5 (Drop) | +6,9 | 3,0 | −8,6 (+0,1) | −1,19 | 8,8 (−0,1) | 3,7 (−0,1) | −18,8 (+0,3) | −5,58 | −13,8 (+0,1) | 0,92 | 0,999 | 1,8 | 4,0 |
| trance | −9,7 (+0,1) | −8,0 (Drop) | +8,2 | 3,0 | −8,4 (+0,1) | −1,18 | 8,5 (−0,1) | 3,0 (−0,1) | −15,6 (+0,4) | −5,21 (+0,09) | −10,6 (+0,3) | 0,84 | 0,999 | 1,9 | 1,5 (−0,1) |
| trap | −10,0 | −8,5 (Drop) | +5,4 | 1,5 | −8,7 | −1,18 | 8,8 | 7,0 (+0,1) | −19,5 (+0,2) | −5,00 | −18,7 | 0,97 | 1,000 | 1,5 | – |
| uk_garage | −9,3 | −8,0 (Drop) | +7,6 | 3,0 | −8,7 | −1,18 | 8,1 | 3,5 | −14,1 (+0,1) | −5,13 | −18,4 (−0,1) | 0,97 | 1,000 | 1,0 | – |

## Montées, Drops et Intro (S5b)

Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). Bloquant : Drop − fin de Montée ≥ +1 LU. Intro − Drop : l'Intro du début entière contre les sections Drop (ou Refrain), à titre indicatif.

| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Intro − Drop |
|---|---|---|---|---|
| acid_house | −10,0 / −9,9 | −8,0 / −8,0 | +2,0 / +2,0 | −1,5 (−0,1) |
| acid_techno | −9,5 / −9,5 | −7,4 / −7,4 | +2,1 / +2,1 | −0,1 (−0,1) |
| acidcore | −10,4 / −10,4 / −10,4 | −7,9 / −8,0 / −7,9 | +2,5 / +2,4 / +2,5 | +0,4 (+0,1) |
| afro_house | −12,2 / −12,2 | −9,0 / −8,9 | +3,2 / +3,3 | −1,3 (+0,1) |
| afrobeats | – | – | – | −7,3 (+0,5) |
| amapiano | −15,5 / −15,7 | −9,4 / −9,4 | +6,2 / +6,3 | −4,0 |
| boom_bap | – | – | – | −4,8 (+0,6) |
| deep_house | −12,7 / −12,8 | −9,2 / −9,0 | +3,6 / +3,8 | −2,4 (+0,2) |
| detroit | −10,7 / −10,7 | −8,3 / −8,4 | +2,3 / +2,4 | −0,2 |
| disco | −11,9 / −11,9 | −9,3 / −9,1 | +2,7 / +2,9 | −3,2 (−0,2) |
| dnb | −11,0 / −11,1 / −11,1 | −8,8 / −8,5 / −8,6 | +2,2 / +2,5 / +2,6 | −2,8 |
| drill | – | – | – | −9,0 (−0,1) |
| dubstep | −11,1 / −11,1 / −11,2 | −9,4 / −8,7 / −8,8 | +1,7 / +2,4 / +2,4 | −3,0 (+0,1) |
| gabber | −10,3 / −10,2 / −10,3 | −8,2 / −8,1 / −8,0 | +2,1 / +2,2 / +2,3 | −0,6 (+0,1) |
| grime | – | – | – | −3,9 (−0,3) |
| hard_techno | −11,7 / −11,7 / −11,7 | −9,4 / −8,6 / −8,6 | +2,3 / +3,2 / +3,1 | −3,8 |
| house | −10,7 / −10,7 | −8,3 / −8,4 | +2,5 / +2,3 | −0,9 |
| industrial_techno | −10,6 / −10,6 | −7,6 / −7,7 | +3,0 / +2,9 | −0,3 |
| jersey_club | – | – | – | −5,1 |
| jump_up | −10,7 / −10,8 / −10,9 | −9,3 / −8,7 / −8,7 | +1,4 / +2,1 / +2,2 | −4,8 |
| jungle | −11,6 / −12,3 / −11,6 | −8,1 / −8,2 / −8,2 | +3,5 / +4,1 / +3,4 | −3,6 |
| liquid_dnb | −10,7 / −10,7 / −10,7 | −8,6 / −8,4 / −8,6 | +2,1 / +2,2 / +2,1 | −3,1 |
| lofi | – | – | – | −10,2 |
| minimal | −11,6 / −11,7 | −8,7 / −8,6 | +2,9 / +3,1 | −1,4 (+0,1) |
| neurofunk | −11,2 / −11,2 / −11,2 | −9,2 / −8,8 / −8,7 | +2,0 / +2,4 / +2,5 | −2,3 |
| phonk | – | – | – | −8,4 (−0,3) |
| pop | – | – | – | −2,9 (+0,2) |
| reggaeton | – | – | – | −2,8 (−0,4) |
| retro_techno | −10,8 / −10,9 | −8,2 / −8,2 | +2,6 / +2,7 | −0,9 (−0,2) |
| riddim | −12,1 / −12,1 / −12,1 | −9,7 / −9,8 / −9,8 | +2,4 / +2,3 / +2,3 | −4,1 (−0,1) |
| speed_garage | −10,8 / −10,8 | −8,2 / −8,3 | +2,6 / +2,5 | −2,2 |
| synthwave | – | – | – | −1,3 (−0,1) |
| tech_house | −11,9 / −11,9 | −9,0 / −8,9 | +3,0 / +3,1 | −3,1 |
| techno | −11,6 / −11,7 | −8,6 / −8,5 | +3,0 / +3,2 | −2,1 |
| trance | −10,6 / −10,6 | −8,5 / −8,5 | +2,1 / +2,1 | −2,4 (+0,1) |
| trap | – | – | – | −6,2 (−0,1) |
| uk_garage | – | – | – | −1,5 |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,2 | −3,0 | −14,0 | −8,9 | −12,2 | −17,5 |
| acid_techno | −4,9 | −2,9 | −14,2 | −12,5 | −13,6 | −17,7 |
| acidcore | −10,7 | −1,8 | −10,3 | −11,6 | −12,5 | −14,8 |
| afro_house | −7,5 | −1,6 | −11,0 | −16,2 | −18,6 | −18,7 |
| afrobeats | −3,6 | −4,1 | −9,6 | −13,7 | −17,3 | −22,4 |
| amapiano | −14,8 | −0,9 | −11,1 | −12,9 | −19,7 | −19,1 |
| boom_bap | −3,7 | −3,3 | −12,5 | −17,5 | −16,6 | −19,1 |
| deep_house | −3,9 | −4,0 | −11,6 | −9,9 | −21,5 | −18,2 |
| detroit | −4,3 | −4,4 | −12,7 | −9,1 | −12,5 | −16,1 |
| disco | −8,8 | −3,4 | −5,7 | −10,4 | −15,9 | −16,9 |
| dnb | −5,7 | −4,2 | −7,3 | −9,9 | −14,6 | −15,3 |
| drill | −4,0 | −3,1 | −13,7 | −13,8 | −18,5 | −19,8 |
| dubstep | −4,2 | −6,4 | −5,9 | −10,2 | −17,6 | −17,2 |
| gabber | −6,0 | −5,4 | −6,2 | −7,7 | −16,2 | −16,2 |
| grime | −9,0 | −3,2 | −11,9 | −7,4 | −9,8 | −14,3 |
| hard_techno | −9,0 | −5,3 | −5,3 | −6,5 | −16,2 | −14,3 |
| house | −5,6 | −3,4 | −8,2 | −11,4 | −17,9 | −16,7 |
| industrial_techno | −6,1 | −3,4 | −10,1 | −7,9 | −16,9 | −17,6 |
| jersey_club | −3,2 | −5,8 | −11,1 | −8,3 | −18,9 | −19,0 |
| jump_up | −6,4 | −7,2 | −6,1 | −6,6 | −12,3 | −13,1 |
| jungle | −5,5 | −3,1 | −10,2 | −13,7 | −12,4 | −14,4 |
| liquid_dnb | −4,7 | −3,7 | −9,9 | −11,9 | −15,4 | −15,1 |
| lofi | −3,9 | −2,8 | −13,7 | −21,5 | −19,1 | −22,3 |
| minimal | −4,5 | −3,8 | −8,2 | −12,1 | −22,2 | −21,5 |
| neurofunk | −3,9 | −5,9 | −7,3 | −12,6 | −13,2 | −13,4 |
| phonk | −2,7 | −5,1 | −13,0 | −11,5 | −17,6 | −19,5 |
| pop | −5,5 | −2,8 | −9,2 | −13,8 | −16,6 | −19,0 |
| reggaeton | −3,9 | −4,4 | −11,3 | −10,7 | −13,8 | −16,5 |
| retro_techno | −4,7 | −3,3 | −11,8 | −12,2 | −14,4 | −16,0 |
| riddim | −4,2 | −6,1 | −9,4 | −7,7 | −14,8 | −12,8 |
| speed_garage | −5,3 | −3,1 | −10,4 | −12,2 | −15,4 | −15,8 |
| synthwave | −6,0 | −3,4 | −10,6 | −8,1 | −14,3 | −18,7 |
| tech_house | −5,2 | −3,7 | −10,1 | −9,4 | −16,9 | −15,4 |
| techno | −4,5 | −3,9 | −8,3 | −11,8 | −19,0 | −18,8 |
| trance | −5,7 | −4,4 | −7,5 | −8,4 | −18,9 | −15,6 |
| trap | −2,3 | −5,3 | −15,1 | −12,2 | −18,5 | −19,5 |
| uk_garage | −6,8 | −3,4 | −10,7 | −7,5 | −16,3 | −14,1 |
| **médiane** | −5,2 | −3,7 | −10,3 | −11,4 | −16,3 | −16,9 |

## Largeur et mouvement (S4)

Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.

| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |
|---|---|---|---|---|---|
| acid_house | −24,3 (+0,1) | 0,99 | 1,000 | – | −3,0 (+0,1) |
| acid_techno | −30,6 (+0,5) | 1,00 | 1,000 | – | −2,9 (+0,1) |
| acidcore | −36,0 (+1,2) | 1,00 | 1,000 | – | −1,8 (+0,1) |
| afro_house | −17,0 (+0,2) | 0,96 | 1,000 | 31,2 | −1,6 (+0,1) |
| afrobeats | −15,7 (+0,5) | 0,95 | 1,000 | 22,9 | −4,1 (+0,6) |
| amapiano | −19,4 (−0,1) | 0,98 | 1,000 | 27,0 | −0,9 |
| boom_bap | −20,2 (−0,9) | 0,98 | 1,000 | 26,5 | −3,3 (+0,5) |
| deep_house | −16,3 (+0,4) | 0,95 | 0,999 | 32,3 | −4,0 (−0,1) |
| detroit | −19,1 (+0,5) | 0,98 | 1,000 | 23,0 | −4,4 (+0,1) |
| disco | −14,1 (+0,2) | 0,93 | 1,000 | 34,5 | −3,4 (+0,3) |
| dnb | −12,3 (−0,1) | 0,89 | 1,000 | 17,8 | −4,2 |
| drill | −20,0 (+0,4) | 0,98 | 1,000 | 30,8 | −3,1 |
| dubstep | −10,9 (−0,3) | 0,85 | 1,000 | 16,6 | −6,4 (+0,1) |
| gabber | −12,1 (+0,2) | 0,89 | 0,999 | 13,0 | −5,4 (−0,2) |
| grime | −15,3 (−0,2) | 0,94 | 1,000 | 57,5 | −3,2 (−0,1) |
| hard_techno | −9,9 (+0,1) | 0,81 | 0,997 | 14,9 | −5,3 (−0,3) |
| house | −19,8 (+0,1) | 0,98 | 1,000 | 33,3 | −3,4 |
| industrial_techno | −16,7 (+0,1) | 0,96 | 0,997 | 15,2 | −3,4 (−0,1) |
| jersey_club | −17,0 (+0,4) | 0,96 | 1,000 | 11,3 | −5,8 |
| jump_up | −8,6 (−0,3) | 0,76 | 0,999 | 20,4 | −7,2 (−0,1) |
| jungle | −14,8 (+0,1) | 0,94 | 1,000 | 16,4 | −3,1 (+0,1) |
| liquid_dnb | −14,5 | 0,93 | 1,000 | 23,6 | −3,7 |
| lofi | −26,2 (−0,6) | 0,99 | 1,000 | 23,3 | −2,8 (+0,2) |
| minimal | −11,5 (+0,1) | 0,87 | 0,998 | – | −3,8 (−0,1) |
| neurofunk | −12,9 | 0,90 | 0,999 | 25,5 | −5,9 |
| phonk | −22,8 (−0,2) | 0,99 | 1,000 | 20,7 | −5,1 |
| pop | −17,7 (+1,4) | 0,97 | 1,000 | 25,2 | −2,8 (+0,3) |
| reggaeton | −15,7 (−0,4) | 0,95 | 1,000 | 29,9 | −4,4 (+0,6) |
| retro_techno | −19,1 (+0,3) | 0,97 | 0,999 | 35,5 | −3,3 (+0,2) |
| riddim | −12,8 (+0,2) | 0,90 | 0,999 | 0,1 | −6,1 (+0,3) |
| speed_garage | −19,1 (+0,1) | 0,98 | 1,000 | 6,9 | −3,1 |
| synthwave | −15,3 (−0,2) | 0,94 | 1,000 | 19,2 | −3,4 (+0,1) |
| tech_house | −18,2 | 0,97 | 1,000 | 4,8 | −3,7 |
| techno | −13,8 (+0,1) | 0,92 | 0,999 | 30,9 | −3,9 |
| trance | −10,6 (+0,3) | 0,84 | 0,999 | 13,9 | −4,4 (−0,1) |
| trap | −18,7 | 0,97 | 1,000 | 21,8 | −5,3 |
| uk_garage | −18,4 (−0,1) | 0,97 | 1,000 | 14,3 | −3,4 |

## Lecture (G0)

- Avant : la branche `main` au commit f53a4a5 (même banc, mêmes 37 styles) ; après : la branche `g0` avec les corrections de la relecture (fills partagés quand les scènes manquent, une seule scène pour le dernier Drop) : les 24 styles dont le morceau change ont été repassés, les 13 autres (morceau identique) gardent leur mesure du premier passage. La « Variation entre Drops » d'avant vient du modèle (`DropVariationTest`, mêmes morceaux, l'ancien générateur) : les enregistrements d'avant ne portaient pas encore la ligne `META drop_variation`.
- Variation entre Drops : médiane 0,06 → 0,50, minimum 0 → 0,44 ; 27 styles sur 37 avaient deux Drops presque identiques (< 0,25), plus aucun (sur les 4 ambiances, le modèle donne au moins 0,31).
- Sonie, true peak, contrastes (Drop − Break, Drop − fin de Montée) : inchangés à ±0,1 LU ; aucune limite bloquante franchie.
- Largeur (côté/centre) : médiane −16,8 → −16,7 dB, quasi inchangée. Les pans par rôle (lead +12 %, contre-chant −15 %, voix hachées −12 %) ajoutent peu face à un Drop dominé par le kick et la basse (mono) ; le Drop 1A sans nappe des bass music et les refrains de 8 mesures qui respirent sans accords (boom bap, lofi) en retirent un peu. Élargir vraiment demandera des nappes et des charleys plus larges (chantier son, pas G0).
- Écoute en direct (`scripts/live-bench.sh`, 90 s) : 37 styles sur 37 sans alerte au premier passage, 24 sur 24 sur les styles repassés après la relecture (ni fondu, ni silence, ni clic, ni NaN).

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −32,8 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −35,4 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,0 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −39,2 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −50,5 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −44,5 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −37,1 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −42,4 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −42,7 | 335,2 |
| disco | 0 | 0 | 0,0000 | −44,9 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −32,9 | 297,9 |
| drill | 0 | 0 | 0,0000 | −41,0 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −41,7 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −38,9 | 296,2 |
| grime | 0 | 0 | 0,0000 | −40,4 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −34,6 | 358,4 |
| house | 0 | 0 | 0,0000 | −42,1 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −36,3 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −42,2 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −32,4 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −37,4 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −32,6 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −41,7 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −42,2 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −33,8 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −41,5 | 192,0 |
| pop | 0 | 0 | 0,0000 | −42,8 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −37,1 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −37,6 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −40,9 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −43,8 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −39,4 | 192,0 |
| tech_house | 0 | 0 | 0,0000 | −42,5 | 182,9 |
| techno | 0 | 0 | 0,0000 | −40,5 | 320,0 |
| trance | 0 | 0 | 0,0000 | −43,1 | 306,1 |
| trap | 0 | 0 | 0,0000 | −43,1 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −34,4 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
aaa3256a48127d5317caa27aed3fca642a6cc6e852adf597b696956c3c1afeec  acid_house.wav
5f4d0792186843c85752e39f0d82b29f9a3630c19dde4eb35c73a40187ab08bd  acid_techno.wav
e5523dcde9e13955885dd426a669992644f633d3d6d7a493ccad42be522040f6  acidcore.wav
5763470640f17e4437b4f46db6158925e1c6a38adeae0cec685589899be78369  afro_house.wav
f57df1bb77cf5da2a3239e1d768bdcb342ee2624be9148e8f6cf74ed57490488  afrobeats.wav
6d559c03c18b6eccfcededc821bcbecd14548a860993dd82869f73ead06fff99  amapiano.wav
7e568b7ff7fc2f7a6b448fffcb5a816bec030e7a7e23ef008e3a4eecdbd53246  boom_bap.wav
f700b5c8429cfe57f5bd7c6a1f8c5ce86e5606f188fa79958d2ce95d6ccf18c0  deep_house.wav
cb57a137314d16d13a66f0f31479da0523ce39524c5809e6cc0bc3a3b8721467  detroit.wav
ba83c84691f42f687cbce23d95abf81bc6f7a39a2084e3c0d51f4fdc94d86bcc  disco.wav
5ed32a4524d8b81ee3097abd519e52688cf7913a55be8999021fb613db6089a0  dnb.wav
cb3920ee313ef4c8f7fc1b218c6ec6e04b535c8e41eef927a72c71b910834485  drill.wav
3d2e1478d1dd7085a0133aaf163285106ac38ce0992bc278605e6244b271b1f6  dubstep.wav
d233c04c71218c21c7a02bb0c64c62e80825c8b9d118d5325b3e51234c518cb4  gabber.wav
39646dbfb9191d06dad7a4084e382e1d0d5c821a7735ab5c2230a92fd30f25b7  grime.wav
ce1ec953c3759fc3208b4bc9313e04abaee07cca1541d6fb3bb98b3bf3d32177  hard_techno.wav
8511820f2559f888be48d30fbb1bb5e2bf8c2997ed264a38a120d54e1283cd02  house.wav
1203b31a20260ff2aa3145485ad95f876e0e2abbad5c4f107bde983fad3fa402  industrial_techno.wav
2f2e2d5c847b226ba477dedb6fd2c1648b07c4ae736dc297a85736721c831e1c  jersey_club.wav
e7304a46b6126fe9fa28b37723b0e09f6c78dd4d58a5bacd17e9e40412437e91  jump_up.wav
108621af463c295dde903480851d8471546e84bbb1343206d28d26064a057b71  jungle.wav
e60babcbe0fbe69c53d0a55c32b0ef9ae13de232a3281d08ce725b07c94f3c50  liquid_dnb.wav
34e9d94a419f2d0a9dffde39842b9f372d519e43778340dbb9fc71d80ba03276  lofi.wav
15e4414875f34e68bfb63a5352e03008a411219fa7ce825665767980f883012a  minimal.wav
574527f8e5ffd1e420aa6e06039e9d2e670871a1e66845b86a0ce25298a073c7  neurofunk.wav
1a04f087bb313da2745f54a6b304b6486be1f312ecd3b65191427f8e8ddf8f6a  phonk.wav
67ce8cda30d3a0d2079537aa2f4a030055af579f6459aaaf3a5da53955f3270d  pop.wav
3d9409e4176d4486c8486ee1e09da81ef36766784177172c3a8ca6b2f28d89d0  reggaeton.wav
1c3f0878342505a02416b4aaa81ebdf23e3476ecb0da37f37396a0cb223c2319  retro_techno.wav
b7aeba50c061d89bee02050442cb00a0ca2827f031fb435aa7505da0fba9fd8a  riddim.wav
738c9253adb1203c591b43482d4bcfb8f217d2b06d811a4c5c6f241703ed6d60  speed_garage.wav
e52c391a12a1f38b6cc82881a08a4bdf2d0378e3bfc58e8ae2280f848a0a284d  synthwave.wav
c2e962c27827db27baeec7183f28da2de1c397fd8ee50c8f4b966f6f4bc43c7f  tech_house.wav
97617fc69d119cd3ab145397198caea7a3bcde88da69a3a19433e890a0ffb59f  techno.wav
7ea15dd727d83dc6247d2e5faa2abb01af6eddd307d1fb41a10a6a05a668fa52  trance.wav
4d302cb80716915a3765914b0baaf13ca2d628fe4fd292de09a8c06971536b34  trap.wav
c4911d8741f6f5cefed38dd28e43483a6a67b4df72bd52b7fe5d0da2ca0b216f  uk_garage.wav
```
