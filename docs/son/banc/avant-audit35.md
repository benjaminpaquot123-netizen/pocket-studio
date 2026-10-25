# Audit 35 : banc d'export (avant les chantiers basses et leads)

37 morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` (libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale (Drop, sinon Refrain) : bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.

## Résumé (médiane sur les styles)

| Mesure | Avant | Après | Écart | Min – max (après) | Cible |
|---|---|---|---|---|---|
| LUFS-I (morceau) | −10,0 | −10,1 | −0,0 | −13,9 – −8,2 | cible du genre ±1 (avant S5) |
| LUFS du Drop / Refrain (sections entières) | −8,6 | −8,7 | −0,0 | −12,3 – −7,1 | −8 à −6 en club |
| LUFS de la partie la plus forte (court terme, 20 % du haut) | −8,5 | −8,5 | −0,0 | −12,1 – −7,0 | cible du style ±1 (S5) |
| Écart à la cible (LU) | −0,1 | −0,1 | −0,0 | −2,4 – −0,0 | ±1 |
| True peak (dBTP) | −1,18 | −1,18 | +0,00 | −1,19 – −1,13 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 8,8 | 8,9 | +0,0 | 7,0 – 12,7 | 6 à 10 en club |
| LRA (LU) | 5,2 | 4,9 | −0,3 | 1,9 – 9,5 | 3 à 9 en club |
| Facteur de crête du Drop (dB) | 8,7 | 8,8 | +0,0 | 6,2 – 11,6 | ≥ 6 |
| Air > 6 kHz (dB rel.) | −17,4 | −17,4 | −0,0 | −24,2 – −13,0 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −5,03 | −5,03 | +0,00 | −6,43 – −3,60 | −4,5 ±0,7 |
| Grave 60–250 Hz au Drop (dB rel.) | −3,8 | −3,7 | +0,1 | −7,1 – −0,9 | ≥ −6 en bass music |
| Mouvement du lead (centroïde, ±%) | 22,1 | 22,0 | −0,1 | 0,1 – 53,9 | ≥ 15 |
| Côté/centre (dB) | −16,9 | −16,8 | +0,1 | −37,2 – −8,3 | −15 à −9 |
| Corrélation L/R | 0,96 | 0,96 | −0,00 | 0,74 – 1,00 | 0,3 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | +0,000 | 0,997 – 1,000 | ≥ 0,95 |
| Perte en mono (LU) | −0,13 | −0,13 | +0,00 | −0,62 – 0,00 | ≥ −1,5 |
| Drop − Groove, Couplet ou Montée (LU) | 1,8 | 1,8 | +0,0 | 0,5 – 6,2 | ≥ +2 |
| Drop − Break (LU) | 4,3 | 4,0 | −0,3 | 0,5 – 10,4 | ≥ +4 |
| Drop − Groove avant master (LU) | 3,2 | 3,1 | −0,1 | 1,9 – 8,9 | ≥ +2 |
| Drop − Break avant master (LU) | 6,4 | 6,3 | −0,1 | 2,3 – 11,2 | ≥ +4 |
| True peak du M4A décodé, premier encodage (dBTP) | 0,66 | 0,38 | −0,28 | −1,13 – 2,86 | – |
| True peak du M4A décodé, après la vérification de l'appli (dBTP) | −1,72 | −1,77 | −0,05 | −3,46 – −1,00 | ≤ −1,0 |
| Montée : sa montée sur ses 4 dernières mesures (LU) | 3,6 | 3,5 | −0,2 | 2,0 – 5,4 | ≥ +2 |
| Montée avant master (LU) | 3,6 | 3,5 | −0,1 | 2,2 – 5,4 | ≥ +2 |
| Drop − fin de Montée (LU, le plus petit du morceau) | 2,3 | 2,3 | +0,0 | 1,8 – 6,1 | ≥ +1 (+1,5 visé) |
| Drop − fin de Montée avant master (LU) | 5,0 | 5,1 | +0,1 | 3,6 – 7,1 | – |
| Intro − Drop / Refrain (LU) | −2,9 | −2,8 | +0,1 | −10,2 – 0,3 | ≥ −8 |
| Continu (moyenne absolue) | 0,0000 | 0,0000 | +0,0000 | 0,0000 – 0,0000 | ≤ 0,001 |
| Gain de la cible (dB) | 7,3 | 7,0 | −0,3 | −1,1 – 10,8 | – |
| Perte dans la chaîne (LU) | 3,0 | 3,0 | +0,0 | 0,1 – 3,0 | ≤ 3 (S5, au Drop ; 6 avant) |

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| True peak du M4A décodé ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 30 sur 37 | dnb (−1,0, plafonné), dubstep (−1,3, plafonné), hard_techno (−2,4, plafonné), jump_up (−1,2, plafonné), neurofunk (−1,3, plafonné), riddim (−2,3, plafonné), techno (−1,1, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 24 sur 37 | acid_house (+2,3), acid_techno (+3,0), acidcore (+0,5), deep_house (+3,6), dubstep (+3,5), gabber (+2,6), hard_techno (+2,2), house (+3,9), minimal (+2,9), riddim (+4,0), tech_house (+3,9), techno (+4,0), trance (+1,6) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 15 sur 37 | acid_house (+0,5), acid_techno (+0,9), acidcore (+0,7), afrobeats (+0,9), boom_bap (+1,8), detroit (+0,7), disco (+1,8), house (+0,6), industrial_techno (+1,7), jersey_club (+1,3), jungle (+1,3), minimal (+1,9), phonk (+0,8), reggaeton (+1,0), retro_techno (+1,2), speed_garage (+2,0), synthwave (+0,9), tech_house (+1,0), techno (+1,8), trance (+1,9), trap (+1,5), uk_garage (+1,0) |
| Chaque Montée monte d'au moins +2 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 36 sur 37 | acid_techno (+1,9 / +2,1) |
| Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +1 LU (exporté ; sans Montée : –) (bloquant) | 37 sur 37 | – |
| Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 34 sur 37 | drill (−8,9), lofi (−10,2), phonk (−8,1) |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −9,1 | −8,0 (Drop) | +7,4 | 2,8 | −8,1 | −1,17 | 7,9 | 2,4 | −17,6 | −4,62 | −24,4 | 0,99 | 1,000 | 0,5 | 2,3 |
| acid_techno | −8,3 | −7,5 (Drop) | +7,8 | 2,5 | −7,8 | −1,18 | 7,1 | 3,1 | −17,8 | −4,71 | −31,2 | 1,00 | 1,000 | 0,9 | 3,0 |
| acidcore | −8,6 | −7,5 (Drop) | +2,6 | 3,0 | −8,2 | −1,16 | 7,4 | 2,5 | −15,0 | −4,84 | −37,2 | 1,00 | 1,000 | 0,7 | 0,5 |
| afro_house | −10,6 | −9,0 (Drop) | +5,6 | 0,8 | −9,2 | −1,19 | 9,4 | 4,8 (−0,2) | −19,0 (−0,4) | −5,51 | −17,2 (−0,2) | 0,96 | 1,000 | 2,1 | 4,6 (−0,2) |
| afrobeats | −10,3 | −9,0 (Drop) | +5,6 | 3,0 | −9,4 | −1,13 | 9,2 (+0,1) | 4,9 (−0,2) | −24,2 (−0,1) | −5,97 | −16,2 (+0,2) | 0,95 | 1,000 | 0,9 | – |
| amapiano | −11,9 (+0,1) | −9,0 (Drop) | +5,8 | 0,8 | −9,4 | −1,19 | 10,7 (−0,1) | 8,9 (−0,5) | −19,4 (−0,1) | −6,32 | −19,3 (+0,6) | 0,98 | 1,000 | 5,0 (−0,1) | 10,4 (−1,0) |
| boom_bap | −11,7 | −10,0 (Drop) | +3,1 | 1,6 | −10,4 | −1,18 | 10,5 | 5,3 | −20,0 | −5,41 | −19,3 | 0,98 | 1,000 | 1,8 | – |
| deep_house | −10,8 (+0,1) | −9,0 (Drop) | +4,9 | 1,1 | −9,1 | −1,18 | 9,6 (−0,1) | 4,1 (−0,2) | −18,7 (−0,4) | −6,06 (−0,11) | −16,6 (+0,2) | 0,96 | 0,999 | 2,2 (−0,2) | 3,6 (−0,3) |
| detroit | −9,0 (+0,1) | −8,0 (Drop) | +5,9 | 2,1 | −8,3 | −1,18 | 7,8 (−0,1) | 4,7 (−0,4) | −16,6 (−0,2) | −3,96 (−0,08) | −19,6 (+0,3) | 0,98 | 1,000 | 0,7 (−0,2) | 4,5 (−0,5) |
| disco | −11,1 | −9,0 (Drop) | +3,8 | 1,1 | −9,3 | −1,18 | 9,9 | 5,4 | −17,2 | −5,54 | −14,3 | 0,93 | 1,000 | 1,8 | 5,2 |
| dnb | −10,2 | −7,5 (Drop) | +7,3 | 3,0 | −8,6 | −1,19 | 9,0 | 5,3 (−0,1) | −15,6 | −4,93 | −12,2 | 0,89 | 1,000 | 5,4 | 4,4 |
| drill | −10,2 | −8,5 (Drop) | +4,5 | 0,5 | −8,7 | −1,19 | 9,0 | 9,5 (−0,5) | −20,1 | −5,20 | −20,4 (+0,5) | 0,98 | 1,000 | 2,0 | – |
| dubstep | −10,4 | −7,5 (Drop) | +8,4 | 3,0 | −8,9 | −1,19 | 9,2 | 6,2 (+0,1) | −17,4 | −4,79 | −10,6 (+0,1) | 0,84 | 1,000 | 5,2 (+0,2) | 3,5 (−0,1) |
| gabber | −8,8 | −7,0 (Drop) | +8,8 | 3,0 | −8,0 | −1,18 | 7,6 | 3,2 (−0,1) | −16,6 (−0,1) | −4,88 | −12,3 (+0,1) | 0,89 | 0,999 | 4,8 (−0,1) | 2,6 (−0,1) |
| grime | −10,2 | −8,5 (Drop) | +4,2 | 1,1 | −8,9 | −1,17 | 9,1 | 5,0 (−0,5) | −14,0 (−0,1) | −3,70 (−0,05) | −15,1 (+0,1) | 0,94 | 1,000 | 2,2 (−0,1) | – |
| hard_techno | −10,2 | −6,0 (Drop) | +9,0 | 3,0 | −8,6 | −1,19 | 9,0 | 4,3 | −14,5 | −4,60 | −10,0 (+0,1) | 0,82 | 0,997 | 2,6 | 2,2 (−0,1) |
| house | −9,4 (−0,2) | −8,0 (Drop) | +7,1 | 3,0 | −8,5 (−0,2) | −1,17 | 8,2 (+0,2) | 3,9 | −16,9 (−0,4) | −5,56 | −20,0 | 0,98 | 1,000 | 0,6 | 3,9 (−0,3) |
| industrial_techno | −8,6 | −7,5 (Drop) | +4,4 | 0,4 | −7,7 | −1,19 | 7,4 | 4,4 | −18,1 | −5,26 | −16,8 (+0,1) | 0,96 | 0,997 | 1,7 | 6,4 (−0,1) |
| jersey_club | −9,8 | −8,5 (Drop) | +6,1 | 2,1 | −8,6 | −1,18 | 8,6 | 7,0 (−0,1) | −19,2 | −5,03 | −17,4 | 0,96 | 1,000 | 1,3 | – |
| jump_up | −10,6 | −7,5 (Drop) | +8,4 | 3,0 | −8,8 (−0,1) | −1,18 | 9,4 | 6,0 | −13,5 | −4,04 | −8,3 | 0,74 | 0,999 | 4,6 | 4,7 |
| jungle | −9,8 | −7,5 (Drop) | +7,8 | 3,0 | −8,2 | −1,17 | 8,6 | 5,7 (−0,1) | −14,3 | −4,38 | −14,9 | 0,94 | 1,000 | 1,3 | 6,2 (−0,2) |
| liquid_dnb | −10,3 | −8,0 (Drop) | +7,5 | 3,0 | −8,6 | −1,18 | 9,2 | 6,7 | −15,4 (−0,1) | −4,99 | −14,4 | 0,93 | 1,000 | 5,2 (+0,1) | 5,8 (−0,1) |
| lofi | −13,9 | −12,0 (Drop) | −1,1 | 0,1 | −12,3 | −1,19 | 12,7 | 9,3 | −22,8 | −5,83 | −25,6 | 0,99 | 1,000 | 2,7 | – |
| minimal | −10,0 (−0,1) | −8,0 (Drop) | +7,5 | 3,0 | −8,9 (−0,2) | −1,18 | 8,9 (+0,2) | 3,4 (+0,2) | −22,3 (−0,6) | −6,43 (−0,12) | −11,7 (−0,4) | 0,87 | 0,998 | 1,9 (−0,1) | 2,9 (+0,1) |
| neurofunk | −10,3 (−0,1) | −7,5 (Drop) | +9,3 | 3,0 | −9,0 | −1,19 | 9,1 | 8,2 (−0,5) | −13,6 | −4,02 | −13,0 (+0,3) | 0,90 | 0,999 | 6,0 (+0,1) | 5,6 (−0,5) |
| phonk | −8,2 | −7,0 (Drop) | +8,1 | 2,8 | −7,1 | −1,16 | 7,0 | 8,5 (−0,3) | −19,7 | −4,91 | −22,6 (+0,1) | 0,99 | 1,000 | 0,8 | – |
| pop | −10,3 | −9,0 (Drop) | +3,4 | 1,2 | −9,2 | −1,18 | 9,1 | 3,2 | −19,6 | −5,52 | −19,0 (+0,2) | 0,97 | 1,000 | 2,3 | – |
| reggaeton | −9,9 | −9,0 (Drop) | +4,9 | 2,1 | −9,2 | −1,18 | 8,7 | 3,1 (−0,2) | −17,5 (−0,1) | −4,29 | −15,3 | 0,94 | 1,000 | 1,0 (−0,1) | – |
| retro_techno | −9,2 | −8,0 (Drop) | +7,9 | 3,0 | −8,3 | −1,17 | 8,1 | 5,7 | −16,4 | −4,37 | −19,3 (+0,1) | 0,98 | 0,999 | 1,2 | 7,0 (−0,2) |
| riddim | −11,4 (−0,1) | −7,5 (Drop) | +10,8 | 3,0 | −10,1 (−0,1) | −1,18 | 10,3 (+0,1) | 7,4 (+0,1) | −13,0 (−0,1) | −3,60 | −13,0 | 0,90 | 0,999 | 6,2 (+0,2) | 4,0 |
| speed_garage | −10,1 | −8,0 (Drop) | +7,4 | 3,0 | −8,3 (−0,1) | −1,18 | 8,9 | 6,3 (−0,2) | −15,9 (−0,2) | −5,14 | −19,3 (+0,7) | 0,98 | 1,000 | 2,0 (−0,2) | 6,1 (−0,6) |
| synthwave | −8,8 | −8,0 (Drop) | +6,4 | 2,6 | −8,2 | −1,17 | 7,7 | 1,9 (−0,2) | −19,1 (−0,1) | −4,94 | −15,1 (+0,2) | 0,94 | 1,000 | 0,9 (−0,1) | – |
| tech_house | −10,6 | −8,0 (Drop) | +7,0 | 3,0 | −9,0 | −1,16 | 9,4 | 4,1 (−0,2) | −15,4 (−0,4) | −5,30 (−0,10) | −18,2 (+0,5) | 0,97 | 1,000 | 1,0 (−0,1) | 3,9 (−0,2) |
| techno | −10,0 (−0,3) | −7,5 (Drop) | +6,8 | 3,0 | −8,7 (−0,3) | −1,17 | 8,8 (+0,3) | 3,8 | −19,1 (−0,4) | −5,60 (−0,10) | −13,9 (+1,2) | 0,92 | 0,999 | 1,8 (+0,1) | 4,0 (−0,1) |
| trance | −9,8 (−0,1) | −8,0 (Drop) | +7,9 | 3,0 | −8,5 (−0,1) | −1,18 | 8,6 (+0,1) | 3,1 (+0,1) | −16,0 (−0,3) | −5,30 (−0,11) | −10,9 (−0,3) | 0,85 | 0,999 | 1,9 | 1,6 (+0,1) |
| trap | −10,0 | −8,5 (Drop) | +5,4 | 1,4 | −8,7 | −1,19 | 8,8 | 6,9 (−0,1) | −19,7 | −5,03 | −18,7 | 0,97 | 1,000 | 1,5 | – |
| uk_garage | −9,3 (−0,1) | −8,0 (Drop) | +7,6 | 3,0 | −8,6 (−0,1) | −1,18 | 8,1 (+0,1) | 3,5 (+0,1) | −14,1 (−0,2) | −5,12 (−0,07) | −18,3 (+0,6) | 0,97 | 1,000 | 1,0 | – |

## Montées, Drops et Intro (S5b)

Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). Bloquant : Drop − fin de Montée ≥ +1 LU. Intro − Drop : l'Intro du début entière contre les sections Drop (ou Refrain), à titre indicatif.

| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Intro − Drop |
|---|---|---|---|---|
| acid_house | −9,8 / −9,8 | −8,0 / −8,0 | +1,9 / +1,8 (+3,9 / +3,8) | −1,5 |
| acid_techno | −9,6 / −9,6 | −7,5 / −7,5 | +2,1 / +2,1 (+3,6 / +3,6) | +0,1 |
| acidcore | −10,5 / −10,5 / −10,5 | −8,1 / −8,1 / −8,2 | +2,4 / +2,3 / +2,3 (+4,8 / +4,8 / +4,8) | +0,3 |
| afro_house | −12,3 / −12,4 | −9,0 / −8,9 | +3,4 / +3,4 (+4,4 / +4,5) | −1,4 (−0,1) |
| afrobeats | – | – | – | −7,8 (+0,1) |
| amapiano | −15,5 / −15,6 | −9,4 / −9,4 | +6,1 / +6,3 (+7,1 / +7,3) | −4,0 (+0,3) |
| boom_bap | – | – | – | −5,4 |
| deep_house | −12,9 / −13,0 | −9,2 / −9,0 | +3,7 / +4,0 (+4,9 / +5,3) | −2,5 (+0,1) |
| detroit | −10,5 / −10,6 | −8,4 / −8,3 | +2,2 / +2,2 (+4,7 / +4,7) | −0,2 (+0,1) |
| disco | −11,8 / −11,8 | −9,3 / −9,1 | +2,5 / +2,7 (+3,6 / +3,8) | −3,0 |
| dnb | −10,9 / −11,0 / −11,1 | −8,7 / −8,6 / −8,7 | +2,3 / +2,4 / +2,4 (+5,1 / +5,5 / +5,6) | −2,8 (+0,1) |
| drill | – | – | – | −8,9 (+0,6) |
| dubstep | −11,1 / −11,1 / −11,3 | −8,8 / −8,7 / −8,7 | +2,3 / +2,4 / +2,5 (+5,1 / +5,4 / +5,7) | −3,1 |
| gabber | −10,2 / −10,2 / −10,2 | −8,2 / −8,1 / −7,9 | +2,1 / +2,2 / +2,3 (+4,4 / +5,2 / +5,2) | −0,6 |
| grime | – | – | – | −3,6 (+0,3) |
| hard_techno | −11,7 / −11,7 / −11,7 | −9,3 / −8,6 / −8,7 | +2,4 / +3,1 / +3,0 (+5,5 / +6,4 / +6,4) | −3,8 |
| house | −11,0 / −10,8 | −8,3 / −8,5 | +2,6 / +2,3 (+5,2 / +5,2) | −0,9 (+0,1) |
| industrial_techno | −10,6 / −10,5 | −7,6 / −7,7 | +3,0 / +2,9 (+3,6 / +3,6) | −0,3 |
| jersey_club | – | – | – | −5,1 (+0,1) |
| jump_up | −10,7 / −10,8 / −10,8 | −8,8 / −8,8 / −8,6 | +1,9 / +2,0 / +2,2 (+5,1 / +5,2 / +5,4) | −4,8 (−0,1) |
| jungle | −11,6 / −12,3 / −11,6 | −8,1 / −8,2 / −8,2 | +3,5 / +4,1 / +3,4 (+6,3 / +7,0 / +6,5) | −3,6 (−0,1) |
| liquid_dnb | −10,6 / −10,6 / −10,6 | −8,5 / −8,4 / −8,5 | +2,0 / +2,1 / +2,1 (+5,1 / +5,2 / +5,0) | −3,1 |
| lofi | – | – | – | −10,2 |
| minimal | −11,9 / −12,0 | −8,8 / −8,8 | +3,1 / +3,2 (+6,2 / +6,6) | −1,5 (−0,1) |
| neurofunk | −11,2 / −11,1 / −11,1 | −9,1 / −8,8 / −8,7 | +2,0 / +2,4 / +2,4 (+4,6 / +5,6 / +5,6) | −2,3 |
| phonk | – | – | – | −8,1 (+0,3) |
| pop | – | – | – | −3,1 |
| reggaeton | – | – | – | −2,4 (+0,2) |
| retro_techno | −10,6 / −10,6 | −8,3 / −8,4 | +2,2 / +2,2 (+5,5 / +5,5) | −0,7 |
| riddim | −12,2 / −12,3 / −12,3 | −9,9 / −9,7 / −9,8 | +2,4 / +2,5 / +2,5 (+6,0 / +5,9 / +6,0) | −4,0 (−0,1) |
| speed_garage | −10,8 / −10,7 | −8,2 / −8,3 | +2,6 / +2,4 (+5,7 / +5,7) | −2,2 |
| synthwave | – | – | – | −1,1 (+0,2) |
| tech_house | −11,9 / −11,9 | −9,0 / −8,9 | +2,9 / +3,1 (+6,1 / +6,3) | −3,1 (−0,1) |
| techno | −11,7 / −11,8 | −8,7 / −8,6 | +3,0 / +3,3 (+5,2 / +5,4) | −2,1 |
| trance | −10,7 / −10,7 | −8,5 / −8,5 | +2,2 / +2,2 (+5,2 / +5,3) | −2,4 (−0,1) |
| trap | – | – | – | −6,1 (+0,1) |
| uk_garage | – | – | – | −1,5 (−0,1) |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,1 | −3,0 | −13,8 | −8,9 | −12,2 | −17,6 |
| acid_techno | −4,8 | −2,9 | −14,1 | −12,4 | −13,6 | −17,8 |
| acidcore | −10,6 | −1,9 | −10,4 | −11,6 | −12,6 | −15,0 |
| afro_house | −6,9 | −1,8 | −11,2 | −16,4 | −18,8 | −19,0 |
| afrobeats | −2,9 | −4,7 | −10,1 | −14,3 | −18,9 | −24,2 |
| amapiano | −14,9 | −0,9 | −11,1 | −13,0 | −19,8 | −19,4 |
| boom_bap | −3,2 | −3,8 | −12,3 | −18,0 | −17,4 | −20,0 |
| deep_house | −3,9 | −3,9 | −11,8 | −10,3 | −22,1 | −18,7 |
| detroit | −4,0 | −4,5 | −12,8 | −9,6 | −12,9 | −16,6 |
| disco | −8,9 | −3,7 | −5,9 | −10,7 | −16,1 | −17,2 |
| dnb | −5,7 | −4,3 | −7,2 | −9,8 | −14,8 | −15,6 |
| drill | −4,0 | −3,0 | −13,8 | −14,1 | −18,5 | −20,1 |
| dubstep | −4,3 | −6,4 | −5,8 | −10,0 | −17,7 | −17,4 |
| gabber | −6,0 | −5,2 | −6,3 | −7,8 | −16,2 | −16,6 |
| grime | −8,7 | −3,1 | −11,9 | −7,8 | −10,0 | −14,0 |
| hard_techno | −9,1 | −5,0 | −5,5 | −6,7 | −16,4 | −14,5 |
| house | −5,5 | −3,4 | −8,3 | −11,5 | −18,0 | −16,9 |
| industrial_techno | −6,1 | −3,3 | −10,1 | −8,1 | −17,0 | −18,1 |
| jersey_club | −3,1 | −5,7 | −11,4 | −8,4 | −18,8 | −19,2 |
| jump_up | −6,5 | −7,1 | −5,9 | −6,6 | −12,7 | −13,5 |
| jungle | −5,4 | −3,2 | −10,2 | −13,8 | −12,3 | −14,3 |
| liquid_dnb | −4,7 | −3,6 | −9,8 | −12,3 | −15,7 | −15,4 |
| lofi | −3,6 | −3,0 | −13,7 | −21,1 | −19,4 | −22,8 |
| minimal | −4,3 | −3,7 | −8,6 | −12,7 | −22,9 | −22,3 |
| neurofunk | −3,9 | −5,9 | −7,2 | −12,7 | −13,4 | −13,6 |
| phonk | −2,7 | −5,0 | −13,0 | −11,6 | −17,7 | −19,7 |
| pop | −4,7 | −3,1 | −9,9 | −14,2 | −17,1 | −19,6 |
| reggaeton | −3,4 | −5,0 | −11,1 | −10,5 | −14,4 | −17,5 |
| retro_techno | −4,3 | −3,5 | −11,9 | −12,2 | −14,7 | −16,4 |
| riddim | −4,1 | −6,4 | −9,8 | −7,9 | −14,8 | −13,0 |
| speed_garage | −5,3 | −3,1 | −10,3 | −12,4 | −15,3 | −15,9 |
| synthwave | −5,9 | −3,5 | −10,3 | −8,1 | −14,6 | −19,1 |
| tech_house | −5,2 | −3,7 | −10,1 | −9,4 | −16,9 | −15,4 |
| techno | −4,4 | −3,9 | −8,5 | −11,9 | −19,1 | −19,1 |
| trance | −5,5 | −4,3 | −7,7 | −8,7 | −19,2 | −16,0 |
| trap | −2,3 | −5,3 | −15,0 | −12,3 | −18,3 | −19,7 |
| uk_garage | −6,8 | −3,5 | −10,7 | −7,4 | −16,3 | −14,1 |
| **médiane** | −4,8 | −3,7 | −10,3 | −11,5 | −16,4 | −17,4 |

## Largeur et mouvement (S4)

Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.

| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |
|---|---|---|---|---|---|
| acid_house | −24,4 | 0,99 | 1,000 | – | −3,0 |
| acid_techno | −31,2 | 1,00 | 1,000 | – | −2,9 |
| acidcore | −37,2 | 1,00 | 1,000 | – | −1,9 |
| afro_house | −17,2 (−0,2) | 0,96 | 1,000 | 31,1 | −1,8 (−0,1) |
| afrobeats | −16,2 (+0,2) | 0,95 | 1,000 | 22,0 | −4,7 |
| amapiano | −19,3 (+0,6) | 0,98 | 1,000 | 28,5 | −0,9 |
| boom_bap | −19,3 | 0,98 | 1,000 | 21,1 | −3,8 |
| deep_house | −16,6 (+0,2) | 0,96 | 0,999 | 32,6 | −3,9 (+0,1) |
| detroit | −19,6 (+0,3) | 0,98 | 1,000 | 23,9 | −4,5 |
| disco | −14,3 | 0,93 | 1,000 | 34,5 | −3,7 |
| dnb | −12,2 | 0,89 | 1,000 | 18,2 | −4,3 |
| drill | −20,4 (+0,5) | 0,98 | 1,000 | 31,0 | −3,0 |
| dubstep | −10,6 (+0,1) | 0,84 | 1,000 | 16,8 | −6,4 |
| gabber | −12,3 (+0,1) | 0,89 | 0,999 | 14,0 | −5,2 |
| grime | −15,1 (+0,1) | 0,94 | 1,000 | 53,9 | −3,1 |
| hard_techno | −10,0 (+0,1) | 0,82 | 0,997 | 14,5 | −5,0 |
| house | −20,0 | 0,98 | 1,000 | 34,0 | −3,4 |
| industrial_techno | −16,8 (+0,1) | 0,96 | 0,997 | 15,1 | −3,3 |
| jersey_club | −17,4 | 0,96 | 1,000 | 8,9 | −5,7 |
| jump_up | −8,3 | 0,74 | 0,999 | 18,3 | −7,1 |
| jungle | −14,9 | 0,94 | 1,000 | 16,1 | −3,2 |
| liquid_dnb | −14,4 | 0,93 | 1,000 | 22,4 | −3,6 |
| lofi | −25,6 | 0,99 | 1,000 | 23,8 | −3,0 |
| minimal | −11,7 (−0,4) | 0,87 | 0,998 | – | −3,7 (+0,1) |
| neurofunk | −13,0 (+0,3) | 0,90 | 0,999 | 22,4 | −5,9 (−0,1) |
| phonk | −22,6 (+0,1) | 0,99 | 1,000 | 19,7 | −5,0 |
| pop | −19,0 (+0,2) | 0,97 | 1,000 | 23,2 | −3,1 |
| reggaeton | −15,3 | 0,94 | 1,000 | 30,1 | −5,0 |
| retro_techno | −19,3 (+0,1) | 0,98 | 0,999 | 37,5 | −3,5 |
| riddim | −13,0 | 0,90 | 0,999 | 0,1 | −6,4 |
| speed_garage | −19,3 (+0,7) | 0,98 | 1,000 | 7,0 | −3,1 |
| synthwave | −15,1 (+0,2) | 0,94 | 1,000 | 19,4 | −3,5 |
| tech_house | −18,2 (+0,5) | 0,97 | 1,000 | 4,8 | −3,7 (+0,2) |
| techno | −13,9 (+1,2) | 0,92 | 0,999 | 31,2 | −3,9 (−0,1) |
| trance | −10,9 (−0,3) | 0,85 | 0,999 | 14,1 | −4,3 (+0,2) |
| trap | −18,7 | 0,97 | 1,000 | 24,4 | −5,3 |
| uk_garage | −18,3 (+0,6) | 0,97 | 1,000 | 13,8 | −3,5 |

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −32,8 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −35,6 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,0 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −39,4 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −50,9 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −44,8 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −37,4 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −42,6 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −42,5 | 335,2 |
| disco | 0 | 0 | 0,0000 | −44,7 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −32,9 | 297,9 |
| drill | 0 | 0 | 0,0000 | −41,9 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −41,7 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −38,9 | 296,2 |
| grime | 0 | 0 | 0,0000 | −43,8 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −34,5 | 358,4 |
| house | 0 | 0 | 0,0000 | −42,2 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −36,3 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −42,2 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −32,4 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −37,4 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −32,6 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −41,9 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −42,5 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −34,1 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −40,0 | 192,0 |
| pop | 0 | 0 | 0,0000 | −42,6 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −36,1 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −37,4 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −41,0 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −43,8 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −38,4 | 192,0 |
| tech_house | 0 | 0 | 0,0000 | −42,5 | 182,9 |
| techno | 0 | 0 | 0,0000 | −40,6 | 320,0 |
| trance | 0 | 0 | 0,0000 | −43,4 | 306,1 |
| trap | 0 | 0 | 0,0000 | −43,2 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −34,7 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
0e35ca7cee91dbd40d7f188f2d65dac8fc0d3fd37806caed5fe1455c39f7f92f  acid_house.wav
a926a9f364214651125a7fd25455b6532fab5ced7c3aa05261c15155050c245c  acid_techno.wav
83436e47474c84339dc1400690072587bf1698be180c29a68226e509f6221fde  acidcore.wav
b2b65f65ef32ef1cf4254cea6254e747134676817584d9b95b808ee6b85a4b0f  afro_house.wav
b1859d25c84e67086a9d52451e7d4338633a0e25084f6caef35805f83a433029  afrobeats.wav
1754cefc608f1d8910efc5013c8f2d9151636c2ee2b61ed5abed2d168ed3307b  amapiano.wav
4e948323b21256b7c1e917c32a909fc46798b5fef21dcef2b15954c08bc7af39  boom_bap.wav
dfdddc0a8cb1808f9a42c0eb944834f52f1e5268aa4727ae2fe329ff8f01fde3  deep_house.wav
1b82e4ffe8234f9235071e4d0b5a22d72b8d2d3f0d35b86046ec3e5fca410bc0  detroit.wav
2b9dfdcdbfdef0a134a47febeebea38c429394146a9577d3f2b0c8a2b11600da  disco.wav
53fc9a2ba09c46072c141a834081d74f139ec7eb7f50cfde5d6fb3665200baf8  dnb.wav
c25a6b72eab824affe9a12bff642bdde2f830bf762ce42bb78bb9f73ab9de9c8  drill.wav
07b46d9522353c97ba1b9cdb3778bac7594f3b376c2dfdec9639c68b23e21910  dubstep.wav
d0a31b2ff524ccbc913e85c334feb01c92a281a3a2b0f7748bb3442b8937e8ad  gabber.wav
80070c0caa2008349a6823598ace2929a2e2c6af59f25f791f2e101737052de8  grime.wav
28b3c034e41a0fdc1034c9108918882a7e619e344d3e40dcc532fa6477946c30  hard_techno.wav
b66cd7d061e19585f4b69e6e1478083e0ef529b739a9bbb4b508994d3ff9d5c7  house.wav
99ec1f3e7dd737225df873d8be1d164a0c31ae580a7c5be7593fea9745f19362  industrial_techno.wav
7ca347f4bab6bcc5b187b5b2fcb8115776c0d1202c10d488b2a232b4c5698a07  jersey_club.wav
07b4e6696b3543933eb0db3e14229432099954e13f17d0f56baf5b108b087174  jump_up.wav
b29d15c1f5da2ac8bddd651900e61e64ea298e0c4766903631b888a08b2d5a3e  jungle.wav
d352f5a1dc189a879e97839a2a74af3c3504b25c5f9827b6877ff6522d868785  liquid_dnb.wav
1a03680281b636c5952902dafd7e4c0366d04682c369cbeff8b7a7a55b8ab86e  lofi.wav
434a26e96a9f2ff1d866236f0409331ae4cb1ce1dffd73df60714f6afb8daf23  minimal.wav
6a3b60f86502e8ba4e0a79c997b3d5a923e640e698ca47c3c4d2c83216cd95a8  neurofunk.wav
6931fcc4b8c5a7feab3077b09f1fa481a254b00be96b5ade25c5722b04700b37  phonk.wav
848a4e104cbab2c5f44ad36d4f04a09ce9d214ea45ee2f044f13c6d9e4faed01  pop.wav
310afa064426abe18f94e2bb15f7bc6ab77e9c085a667c4cc98d30bf10e800ee  reggaeton.wav
c4c7d5396c5cd717d738a7a9844f774fd2019c30ab71bc7119da1ee77e8c81d6  retro_techno.wav
f081802e892d65468a4cbc61ca98b6091e378b224089dd4972f5b63c6f614fa7  riddim.wav
397d11866c98a2fcdb151acbabed9afd9c6fec713faaa9fd5719e63e24d38907  speed_garage.wav
553208642cbe13f5ed699159195a04bd55c3b79de8cbcd0ac9ec8008f4314a3d  synthwave.wav
da50277612f617909c21190ad87027716f97691df90af061aec091e50e41ea03  tech_house.wav
7dda29575e70209f1b7e4afcfd8ca10af6997f50fcfdb61b49782c634af30343  techno.wav
6dabda5ae727702512cd9b995357c261ec440afdb97dcb892de0ca9fd679c7f6  trance.wav
13e18966e72b4dedd8604d764b98ab3a35657ff2522028a9215a1c0c6b6c949c  trap.wav
5bb7c7bd63330e55c16814a7fb5a480f8b2adda336bf3ac7b97af545d92b0380  uk_garage.wav
```
