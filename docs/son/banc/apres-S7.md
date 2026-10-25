# Banc des morceaux : après S7 son (largeur, téléphone, direct = export)

37 morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` (libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale (Drop, sinon Refrain) : bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.

## Résumé (médiane sur les styles)

| Mesure | Avant | Après | Écart | Min – max (après) | Cible |
|---|---|---|---|---|---|
| LUFS-I (morceau) | −10,2 | −10,2 | +0,0 | −13,8 – −8,4 | cible du genre ±1 (avant S5) |
| LUFS du Drop / Refrain (sections entières) | −8,7 | −8,7 | +0,0 | −12,2 – −7,1 | −8 à −6 en club |
| LUFS de la partie la plus forte (court terme, 20 % du haut) | −8,5 | −8,5 | +0,0 | −12,1 – −7,0 | cible du style ±1 (S5) |
| Écart à la cible (LU) | −0,1 | −0,1 | −0,0 | −2,6 – −0,0 | ±1 |
| True peak (dBTP) | −1,19 | −1,19 | +0,00 | −1,19 – −1,17 | ≤ −1,0 |
| PLR (TP − LUFS-I) | 9,0 | 9,0 | −0,0 | 7,3 – 12,7 | 6 à 10 en club |
| LRA (LU) | 5,4 | 5,3 | −0,1 | 2,9 – 9,7 | 3 à 9 en club |
| Facteur de crête du Drop (dB) | 8,9 | 9,1 | +0,1 | 7,0 – 11,8 | ≥ 6 |
| Air > 6 kHz (dB rel.) | −16,3 | −16,3 | +0,0 | −23,3 – −12,4 | −20 à −16 |
| Pente 0,1–8 kHz (dB/oct) | −4,87 | −4,71 | +0,16 | −5,92 – −3,40 | −4,5 ±0,7 |
| Grave 60–250 Hz au Drop (dB rel.) | −3,4 | −3,6 | −0,2 | −7,3 – −1,3 | ≥ −6 en bass music |
| Mouvement du lead (centroïde, ±%) | 19,3 | 19,5 | +0,2 | 0,0 – 56,8 | ≥ 15 |
| Côté/centre (dB) | −16,2 | −11,0 | +5,2 | −16,6 – −7,9 | −15 à −9 |
| Corrélation L/R | 0,95 | 0,85 | −0,10 | 0,72 – 0,96 | 0,3 à 0,85 |
| Corrélation < 120 Hz | 1,000 | 1,000 | +0,000 | 0,992 – 1,000 | ≥ 0,95 |
| Perte en mono (LU) | −0,15 | −0,54 | −0,39 | −0,96 – −0,18 | ≥ −1,5 |
| Drop − Groove, Couplet ou Montée (LU) | 2,4 | 2,5 | +0,0 | 1,8 – 5,8 | ≥ +2 |
| Drop − Break (LU) | 4,8 | 5,0 | +0,2 | 2,2 – 9,4 | ≥ +4 |
| Drop − Groove avant master (LU) | 4,5 | 4,5 | −0,0 | 2,7 – 8,6 | ≥ +2 |
| Drop − Break avant master (LU) | 7,5 | 7,4 | −0,1 | 4,9 – 12,1 | ≥ +4 |
| True peak du M4A décodé, premier encodage (dBTP) | −1,46 | −1,47 | −0,01 | −1,90 – −1,10 | – |
| True peak du M4A décodé, après la vérification de l'appli (dBTP) | −1,46 | −1,47 | −0,01 | −1,90 – −1,10 | ≤ −1,0 |
| Montée : sa montée sur ses 4 dernières mesures (LU) | 3,7 | 3,5 | −0,2 | 1,8 – 6,0 | ≥ +2 |
| Montée avant master (LU) | 3,7 | 3,5 | −0,2 | 2,2 – 6,0 | ≥ +2 |
| Drop − fin de Montée (LU, le plus petit du morceau) | 2,6 | 2,4 | −0,2 | 1,5 – 4,2 | ≥ +1 (+1,5 visé) |
| Drop − fin de Montée avant master (LU) | 5,0 | 4,8 | −0,2 | 3,3 – 6,3 | – |
| Intro − Drop / Refrain (LU) | −3,2 | −3,0 | +0,2 | −9,3 – −0,3 | ≥ −8 |
| Variation entre Drops (part des mesures qui diffèrent, paire la plus proche) | 0,50 | 0,50 | +0,00 | 0,44 – 1,00 | ≥ 0,25 |
| Continu (moyenne absolue) | 0,0000 | 0,0000 | +0,0000 | 0,0000 – 0,0000 | ≤ 0,001 |
| Gain de la cible (dB) | 6,9 | 6,7 | −0,1 | −1,2 – 11,1 | – |
| Perte dans la chaîne (LU) | 2,7 | 2,8 | +0,1 | 0,1 – 3,0 | ≤ 3 (S5, au Drop ; 6 avant) |
| B1 Basse au téléphone 200 Hz (dB) | −6,5 | −6,1 | +0,4 | −11,5 – −1,7 | ≥ −10 |
| B2 Basse au téléphone 300 Hz (dB) | −10,7 | −10,1 | +0,6 | −15,4 – −2,8 | ≥ −14 |
| B3 Basse dans le mix au téléphone (LU) | −9,1 | −8,5 | +0,5 | −35,4 – −2,4 | ≥ −10 hip-hop et bass music, ≥ −14 club |
| B4 Harmoniques 200–800 / 30–100 Hz (dB) | −6,3 | −5,5 | +0,8 | −15,7 – 12,5 | ≥ −14 |
| B5 Attaque de la basse (dB) | 5,2 | 5,1 | −0,1 | 2,4 – 85,5 | ≥ +2 (808, pluck : +4) |
| B6 Régularité de l'attaque (dB) | 10,75 | 20,88 | +10,13 | 0,23 – 71,18 | ≤ 0,5 |
| B7 Sub 40–60 Hz du mix (dB rel.) | −6,0 | −6,2 | −0,2 | −22,4 – −3,8 | −6 à −3 (le garder) |
| B8 Infra < 30 Hz du mix (dB rel.) | −25,5 | −25,6 | −0,2 | −29,9 – −19,6 | ≤ −20 |
| B9 Creux de la basse sous le kick (dB) | 0,5 | −0,1 | −0,5 | −11,2 – 105,0 | ≤ −6 club, ≤ −3 ailleurs |
| B12 Sonie ajoutée par l'exciteur (LU) | 0,04 | 0,06 | +0,01 | −0,01 – 0,19 | ≤ +1 |

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| True peak du M4A décodé ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 30 sur 37 | dnb (−1,0, plafonné), dubstep (−1,1, plafonné), gabber (−1,0, plafonné), hard_techno (−2,6, plafonné), jump_up (−1,2, plafonné), neurofunk (−1,2, plafonné), riddim (−2,2, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 32 sur 37 | acid_house (+3,8), acidcore (+2,6), gabber (+3,2), hard_techno (+2,8), trance (+2,2) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 36 sur 37 | acidcore (+1,8) |
| Chaque Montée monte d'au moins +2 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 36 sur 37 | acid_techno (+1,9 / +1,7) |
| Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +1 LU (exporté ; sans Montée : –) (bloquant) | 37 sur 37 | – |
| Intro à moins de 8 LU sous le Drop ou le Refrain (exporté) | 35 sur 37 | afrobeats (−8,6), phonk (−9,3) |
| Variation entre Drops ≥ 0,25 (modèle : aucune paire de Drops identique) | 37 sur 37 | – |
| B2 Basse au téléphone (300 Hz) ≥ −16 dB (cible −14) (bloquant) | 37 sur 37 | – |
| B2 Basse au téléphone (300 Hz) ≥ −14 dB (la cible) | 36 sur 37 | industrial_techno (−15,4) |
| B1 Basse au téléphone (200 Hz) ≥ −10 dB | 35 sur 37 | hard_techno (−10,3), industrial_techno (−11,5) |
| B4 Harmoniques de la basse ≥ −14 dB | 36 sur 37 | industrial_techno (−15,7) |
| B5 Attaque de la basse ≥ +2 dB (notes assez longues pour la lire) | 37 sur 37 | – |
| B6 Écart de l'attaque ≤ 0,5 dB | 8 sur 37 | afro_house (10,8), afrobeats (21,9), amapiano (2,7), boom_bap (35,2), deep_house (27,8), detroit (43,2), disco (3,7), dnb (35,5), dubstep (28,4), grime (2,7), house (48,3), jersey_club (0,7), jump_up (37,7), jungle (29,6), liquid_dnb (59,1), lofi (1,5), minimal (8,7), neurofunk (35,5), pop (1,4), reggaeton (0,7), retro_techno (19,9), riddim (25,7), speed_garage (25,0), synthwave (2,9), tech_house (67,9), techno (14,4), trance (0,8), trap (6,0), uk_garage (71,2) |
| B9 La basse s'efface sous le kick (≤ −3 dB) | 5 sur 37 | afrobeats (−0,2), boom_bap (0,1), deep_house (1,1), detroit (−1,3), disco (2,3), dnb (−1,2), drill (0,8), dubstep (−2,0), gabber (5,2), grime (−0,7), hard_techno (4,6), house (−1,4), industrial_techno (4,6), jersey_club (2,2), jump_up (2,0), jungle (−0,9), liquid_dnb (−1,4), lofi (−0,6), minimal (105,0), neurofunk (0,0), phonk (1,1), pop (−0,1), reggaeton (3,5), retro_techno (1,1), riddim (−1,3), speed_garage (−0,9), synthwave (−1,7), tech_house (4,3), techno (0,2), trance (−1,7), trap (0,5), uk_garage (−1,1) |
| B8 Rien sous 30 Hz : infra ≤ −15 dB du Drop (cible −20) (bloquant) | 37 sur 37 | – |
| B8 Rien sous 30 Hz : infra ≤ −20 dB du Drop (la cible) | 36 sur 37 | riddim (−19,6) |
| B12 L'exciteur ajoute au plus +1 LU au morceau (avant master) (bloquant) | 37 sur 37 | – |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Par style

Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).

| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −9,6 | −8,0 (Drop) | +6,7 | 2,5 | −8,1 | −1,19 | 8,4 | 3,8 | −17,5 (+0,1) | −4,45 (+0,17) | −10,0 (+7,5) | 0,82 (−0,15) | 0,996 | 2,0 (−0,1) | 3,8 |
| acid_techno | −9,1 (−0,2) | −7,5 (Drop) | +7,8 | 3,0 | −7,8 (−0,1) | −1,18 | 7,9 (+0,2) | 4,1 (+0,2) | −17,6 (+0,1) | −4,53 (+0,18) | −12,2 (+8,2) | 0,89 (−0,10) | 0,997 | 2,4 | 4,3 |
| acidcore | −9,0 (−0,3) | −7,5 (Drop) | +1,9 | 3,0 | −8,2 (−0,2) | −1,19 | 7,8 (+0,3) | 2,9 (+0,5) | −14,1 (+0,8) | −4,57 (+0,28) | −9,9 (+7,9) | 0,82 (−0,15) | 0,992 | 1,8 (−0,1) | 2,6 (+0,6) |
| afro_house | −10,9 | −9,0 (Drop) | +5,4 | 0,8 | −9,2 | −1,19 | 9,7 | 5,6 (+0,4) | −18,8 (+0,1) | −5,67 (+0,12) | −14,1 (+3,1) | 0,93 | 1,000 | 2,6 | 5,5 (+0,4) |
| afrobeats | −10,6 | −9,0 (Drop) | +4,5 | 2,2 | −9,3 | −1,18 | 9,4 | 6,8 (−0,6) | −23,3 (+0,1) | −5,91 (+0,24) | −11,8 (+4,2) | 0,88 (−0,08) | 1,000 | 2,2 (+0,1) | – |
| amapiano | −11,7 (+0,2) | −9,0 (Drop) | +5,7 | 1,2 | −9,3 | −1,19 | 10,5 (−0,2) | 7,6 (−0,9) | −18,4 (+0,3) | −5,86 (+0,24) | −11,2 (+6,7) | 0,86 (−0,11) | 1,000 | 4,1 (−0,5) | 8,0 (−1,5) |
| boom_bap | −11,8 (+0,1) | −10,0 (Drop) | +2,7 | 1,3 | −10,3 | −1,19 | 10,6 (−0,1) | 5,3 (−1,0) | −19,2 (+0,2) | −5,30 (+0,07) | −15,2 (+5,5) | 0,94 | 0,999 | 2,2 (−0,1) | – |
| deep_house | −11,0 | −9,0 (Drop) | +4,9 | 1,1 | −9,2 | −1,19 | 9,8 | 5,0 (+0,3) | −18,3 | −5,92 | −13,1 (+4,0) | 0,91 (−0,05) | 0,999 | 2,6 | 4,6 (+0,5) |
| detroit | −9,7 (−0,2) | −8,0 (Drop) | +5,2 | 1,7 | −8,2 (−0,1) | −1,19 | 8,5 (+0,2) | 6,3 (+0,2) | −16,3 | −3,79 (+0,12) | −12,2 (+6,3) | 0,89 (−0,08) | 1,000 | 2,4 (+0,2) | 6,5 (+0,1) |
| disco | −11,4 | −9,0 (Drop) | +3,3 | 0,8 | −9,2 | −1,19 | 10,2 | 6,0 (−0,4) | −15,9 (+0,2) | −4,95 (+0,19) | −10,8 (+5,0) | 0,84 (−0,10) | 1,000 | 2,5 (+0,1) | 6,7 (−0,5) |
| dnb | −10,2 (+0,1) | −7,5 (Drop) | +6,8 | 3,0 | −8,6 | −1,19 | 9,0 | 5,4 (+0,1) | −14,8 (+0,1) | −4,45 (+0,14) | −9,1 (+3,1) | 0,78 (−0,11) | 1,000 | 5,7 (−0,2) | 4,2 |
| drill | −10,3 | −8,5 (Drop) | +4,3 | 0,4 | −8,7 | −1,19 | 9,1 (+0,1) | 7,8 (−1,4) | −19,9 (+0,1) | −5,21 (+0,15) | −14,4 (+5,6) | 0,93 (−0,05) | 1,000 | 2,5 | – |
| dubstep | −10,3 (+0,1) | −7,5 (Drop) | +8,6 | 3,0 | −8,8 | −1,19 | 9,2 (−0,1) | 6,3 (−0,1) | −16,3 (+0,1) | −4,17 (+0,14) | −9,4 (+2,9) | 0,80 (−0,09) | 1,000 | 5,4 (−0,2) | 4,3 |
| gabber | −9,0 (−0,2) | −7,0 (Drop) | +8,8 | 3,0 | −8,1 (−0,1) | −1,19 | 7,8 (+0,2) | 3,8 | −14,8 | −4,24 (+0,18) | −7,9 (+4,7) | 0,72 (−0,17) | 0,999 | 4,9 (−0,1) | 3,2 |
| grime | −10,3 | −8,5 (Drop) | +4,2 | 1,3 | −8,9 | −1,19 | 9,1 | 4,5 (−0,9) | −14,3 (+0,7) | −3,63 (+0,23) | −9,2 (+6,3) | 0,79 (−0,16) | 1,000 | 2,5 (+0,2) | – |
| hard_techno | −10,5 (−0,1) | −6,0 (Drop) | +8,4 | 3,0 | −8,8 | −1,19 | 9,3 | 4,4 | −13,2 | −3,77 | −8,8 | 0,77 | 0,997 | 2,8 | 2,8 (+0,5) |
| house | −10,2 | −8,0 (Drop) | +7,4 | 3,0 | −8,5 | −1,18 | 9,0 | 6,1 (−0,4) | −16,3 | −5,33 (+0,09) | −12,5 (+5,7) | 0,89 (−0,08) | 1,000 | 2,3 | 8,3 (−0,8) |
| industrial_techno | −8,7 (+0,1) | −7,5 (Drop) | +4,5 | 0,6 | −7,6 (+0,1) | −1,19 | 7,5 (−0,2) | 4,5 (−0,3) | −17,5 (+0,1) | −5,00 (+0,19) | −9,6 (+6,4) | 0,80 (−0,15) | 0,997 | 2,1 (−0,2) | 6,4 (−1,1) |
| jersey_club | −10,0 | −8,5 (Drop) | +6,3 | 2,3 | −8,7 | −1,18 | 8,8 | 6,5 (−1,1) | −19,0 | −5,26 | −11,5 (+5,7) | 0,87 (−0,09) | 1,000 | 2,1 | – |
| jump_up | −10,7 | −7,5 (Drop) | +8,3 | 3,0 | −8,8 | −1,18 | 9,5 | 6,8 (+0,3) | −12,8 (−0,1) | −3,60 | −8,2 (+1,2) | 0,74 (−0,06) | 0,999 | 5,0 | 6,1 (+0,3) |
| jungle | −10,0 | −7,5 (Drop) | +7,2 | 3,0 | −8,3 | −1,18 | 8,8 | 5,3 (−0,1) | −15,1 (−0,4) | −4,38 | −8,6 (+3,7) | 0,76 (−0,13) | 1,000 | 2,2 (+0,2) | 5,1 (−0,3) |
| liquid_dnb | −10,0 (+0,1) | −8,0 (Drop) | +6,9 | 3,0 | −8,4 | −1,18 | 8,8 (−0,1) | 5,3 (+0,1) | −14,4 (+0,1) | −4,29 (+0,17) | −9,0 (+6,0) | 0,78 (−0,16) | 1,000 | 5,0 (−0,3) | 4,0 (−0,5) |
| lofi | −13,8 (+0,1) | −12,0 (Drop) | −1,2 | 0,1 | −12,2 | −1,19 | 12,7 (−0,1) | 6,1 (−1,9) | −22,2 (+0,3) | −5,59 (+0,15) | −15,1 (+8,4) | 0,94 (−0,05) | 1,000 | 2,6 (−0,1) | – |
| minimal | −9,9 (−0,1) | −8,0 (Drop) | +8,2 | 3,0 | −8,3 | −1,19 | 8,7 (+0,1) | 4,7 (+0,1) | −20,6 | −5,71 (+0,11) | −10,2 (+2,4) | 0,83 (−0,07) | 0,999 | 2,5 (+0,2) | 4,7 (+0,1) |
| neurofunk | −10,2 (+0,1) | −7,5 (Drop) | +9,4 | 3,0 | −8,8 (+0,1) | −1,19 | 9,0 (−0,1) | 9,2 (−0,2) | −12,4 (+0,2) | −3,40 (+0,13) | −11,8 (+3,2) | 0,88 (−0,06) | 1,000 | 5,8 (−0,1) | 7,5 (−0,4) |
| phonk | −8,4 (−0,1) | −7,0 (Drop) | +6,4 | 1,3 | −7,1 | −1,17 | 7,3 (+0,1) | 9,7 (−1,6) | −19,7 (+0,1) | −5,19 (+0,06) | −16,6 (+6,5) | 0,96 | 1,000 | 2,1 (−0,1) | – |
| pop | −10,3 | −9,0 (Drop) | +3,6 | 1,3 | −9,2 | −1,19 | 9,1 | 3,1 | −18,0 (+0,1) | −5,01 (+0,13) | −10,8 (+6,9) | 0,85 (−0,12) | 1,000 | 2,4 | – |
| reggaeton | −10,3 | −9,0 (Drop) | +3,6 | 0,8 | −9,2 | −1,19 | 9,2 | 4,0 (−0,5) | −16,0 | −4,37 | −11,6 (+2,6) | 0,87 (−0,06) | 1,000 | 2,5 | – |
| retro_techno | −9,4 | −8,0 (Drop) | +7,7 | 2,8 | −8,2 | −1,19 | 8,3 | 6,1 (−0,3) | −15,8 (+0,2) | −4,29 (+0,11) | −13,9 (+5,3) | 0,92 (−0,05) | 1,000 | 2,1 (−0,1) | 9,4 (−0,3) |
| riddim | −11,3 | −7,5 (Drop) | +11,1 | 3,0 | −9,8 | −1,17 | 10,2 | 7,5 (+0,1) | −12,5 (+0,1) | −3,62 (+0,08) | −11,0 (+1,5) | 0,85 | 0,999 | 5,8 (−0,2) | 5,0 (+0,6) |
| speed_garage | −10,1 | −8,0 (Drop) | +7,0 | 3,0 | −8,3 (−0,1) | −1,18 | 8,9 | 6,2 (−0,4) | −16,0 | −5,21 (+0,10) | −12,3 (+5,4) | 0,89 (−0,08) | 1,000 | 2,2 (−0,2) | 5,6 (−0,5) |
| synthwave | −9,3 (+0,1) | −8,0 (Drop) | +5,8 | 1,9 | −8,3 (+0,1) | −1,19 | 8,1 (−0,1) | 3,5 (−1,2) | −17,7 (+0,3) | −4,83 (+0,10) | −11,2 (+4,6) | 0,86 (−0,09) | 1,000 | 2,1 (+0,1) | – |
| tech_house | −11,1 | −8,0 (Drop) | +6,7 | 3,0 | −9,0 | −1,17 | 9,9 | 5,3 | −15,2 (−0,1) | −5,02 (+0,11) | −10,6 (+5,7) | 0,84 (−0,12) | 1,000 | 2,1 (−0,1) | 6,4 (+0,6) |
| techno | −10,0 (−0,1) | −7,5 (Drop) | +7,2 | 3,0 | −8,4 | −1,19 | 8,8 (+0,1) | 4,3 (+0,1) | −18,4 | −5,20 (+0,10) | −9,8 (+3,2) | 0,81 (−0,09) | 0,999 | 2,2 (+0,1) | 4,7 |
| trance | −9,8 | −8,0 (Drop) | +8,2 | 3,0 | −8,3 | −1,18 | 8,6 (+0,1) | 3,5 | −14,0 | −4,64 | −9,6 | 0,80 | 0,999 | 2,5 | 2,2 (+0,5) |
| trap | −10,2 | −8,5 (Drop) | +4,6 | 1,0 | −8,7 | −1,19 | 9,0 | 6,2 (−1,2) | −18,6 (+0,3) | −4,71 (+0,16) | −13,3 (+5,8) | 0,91 (−0,06) | 1,000 | 2,7 | – |
| uk_garage | −9,5 (+0,1) | −8,0 (Drop) | +7,3 | 3,0 | −8,4 (+0,1) | −1,18 | 8,3 (−0,1) | 4,4 (+0,1) | −13,8 | −4,60 (+0,25) | −10,1 (+7,9) | 0,82 (−0,15) | 1,000 | 2,2 (+0,1) | – |

## Montées, Drops et Intro (S5b)

Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). Bloquant : Drop − fin de Montée ≥ +1 LU. Intro − Drop : l'Intro du début entière contre les sections Drop (ou Refrain), à titre indicatif.

| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Intro − Drop |
|---|---|---|---|---|
| acid_house | −9,8 / −9,7 | −8,0 / −8,0 | +1,8 / +1,7 (+3,6 / +3,5) | −2,3 (−0,1) |
| acid_techno | −9,2 / −9,3 | −7,6 / −7,5 | +1,6 / +1,8 (+3,3 / +3,3) | −1,6 (−0,2) |
| acidcore | −10,1 / −10,2 / −10,2 | −8,0 / −8,2 / −8,1 | +2,1 / +2,0 / +2,0 (+4,7 / +4,9 / +4,9) | −0,3 (+0,1) |
| afro_house | −12,6 / −12,8 | −9,0 / −9,1 | +3,6 / +3,7 (+4,6 / +4,9) | −2,1 |
| afrobeats | – | – | – | −8,6 (+1,2) |
| amapiano | −13,5 / −13,9 | −9,3 / −9,3 | +4,2 / +4,6 (+5,6 / +6,0) | −3,6 (+0,2) |
| boom_bap | – | – | – | −5,4 (+1,0) |
| deep_house | −12,8 / −13,2 | −9,1 / −9,0 | +3,7 / +4,2 (+4,8 / +5,5) | −2,9 (+0,1) |
| detroit | −11,0 / −11,1 | −8,2 / −8,3 | +2,8 / +2,8 (+4,8 / +4,6) | −1,6 (−0,3) |
| disco | −11,9 / −11,9 | −9,3 / −9,0 | +2,5 / +2,8 (+3,3 / +3,5) | −4,0 (−0,3) |
| dnb | −11,2 / −11,2 / −11,2 | −8,8 / −8,6 / −8,5 | +2,4 / +2,6 / +2,6 (+4,7 / +5,6 / +5,6) | −3,1 |
| drill | – | – | – | −7,4 (+1,4) |
| dubstep | −10,8 / −10,8 / −10,9 | −9,2 / −8,6 / −8,7 | +1,6 / +2,2 / +2,2 (+4,1 / +5,2 / +5,4) | −3,4 |
| gabber | −10,1 / −10,1 / −10,1 | −8,2 / −8,1 / −8,0 | +1,9 / +2,0 / +2,1 (+4,2 / +5,0 / +5,0) | −0,5 (−0,3) |
| grime | – | – | – | −3,1 (+0,8) |
| hard_techno | −12,7 / −12,8 / −12,7 | −9,6 / −8,8 / −8,8 | +3,1 / +4,0 / +3,9 (+5,7 / +7,1 / +7,0) | −3,8 |
| house | −11,5 / −11,8 | −8,5 / −8,4 | +3,1 / +3,4 (+6,2 / +6,6) | −1,9 (−0,1) |
| industrial_techno | −10,1 / −10,2 | −7,6 / −7,6 | +2,5 / +2,6 (+3,3 / +3,5) | −0,9 |
| jersey_club | – | – | – | −4,0 (+1,2) |
| jump_up | −10,8 / −10,8 / −10,8 | −9,3 / −8,7 / −8,7 | +1,5 / +2,1 / +2,1 (+4,6 / +5,2 / +5,3) | −4,8 (−0,1) |
| jungle | −11,5 / −12,3 / −11,5 | −8,1 / −8,3 / −8,3 | +3,4 / +4,0 / +3,2 (+6,1 / +7,0 / +6,3) | −3,9 (−0,1) |
| liquid_dnb | −10,5 / −10,4 / −10,4 | −8,4 / −8,3 / −8,2 | +2,1 / +2,1 / +2,2 (+5,2 / +5,1 / +5,1) | −3,1 (+0,1) |
| lofi | – | – | – | −6,1 (+2,1) |
| minimal | −11,2 / −11,5 | −8,4 / −8,3 | +2,8 / +3,3 (+6,3 / +6,6) | −2,2 (−0,1) |
| neurofunk | −10,8 / −10,8 / −10,8 | −9,0 / −8,7 / −8,7 | +1,8 / +2,0 / +2,1 (+4,6 / +5,2 / +5,3) | −2,4 (−0,1) |
| phonk | – | – | – | −9,3 (+1,6) |
| pop | – | – | – | −2,2 (+0,2) |
| reggaeton | – | – | – | −3,0 (+0,5) |
| retro_techno | −10,5 / −10,6 | −8,2 / −8,3 | +2,3 / +2,3 (+5,4 / +5,4) | −1,2 |
| riddim | −11,7 / −11,7 / −11,7 | −9,7 / −9,7 / −9,7 | +2,0 / +2,0 / +2,1 (+5,7 / +5,6 / +5,7) | −4,2 |
| speed_garage | −10,8 / −10,8 | −8,4 / −8,3 | +2,4 / +2,5 (+5,6 / +5,6) | −2,2 |
| synthwave | – | – | – | −1,9 (+1,3) |
| tech_house | −11,5 / −11,7 | −8,9 / −8,9 | +2,6 / +2,8 (+5,6 / +6,0) | −3,9 (−0,1) |
| techno | −10,6 / −10,5 | −8,4 / −8,4 | +2,2 / +2,1 (+5,4 / +5,3) | −2,7 (−0,1) |
| trance | −10,7 / −10,8 | −8,4 / −8,2 | +2,3 / +2,6 (+5,2 / +5,4) | −2,8 |
| trap | – | – | – | −5,0 (+1,1) |
| uk_garage | – | – | – | −2,3 (−0,1) |

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,5 | −3,3 | −13,6 | −8,1 | −10,8 | −17,5 |
| acid_techno | −5,0 | −3,1 | −13,9 | −11,3 | −12,0 | −17,6 |
| acidcore | −11,6 | −1,9 | −10,3 | −10,8 | −11,4 | −14,1 |
| afro_house | −11,2 | −1,3 | −9,6 | −13,8 | −17,8 | −18,8 |
| afrobeats | −4,1 | −3,8 | −9,1 | −13,4 | −16,9 | −23,3 |
| amapiano | −15,8 | −1,4 | −9,8 | −9,7 | −16,5 | −18,4 |
| boom_bap | −4,2 | −3,2 | −11,4 | −16,2 | −16,0 | −19,2 |
| deep_house | −4,2 | −4,0 | −11,2 | −9,4 | −21,2 | −18,3 |
| detroit | −4,6 | −4,3 | −13,6 | −8,6 | −11,7 | −16,3 |
| disco | −8,8 | −3,3 | −6,7 | −9,7 | −12,7 | −15,9 |
| dnb | −6,0 | −4,4 | −8,0 | −8,7 | −12,6 | −14,8 |
| drill | −4,9 | −3,2 | −10,2 | −11,8 | −16,4 | −19,9 |
| dubstep | −4,3 | −6,0 | −7,7 | −8,3 | −14,9 | −16,3 |
| gabber | −5,8 | −6,0 | −6,5 | −7,4 | −13,5 | −14,8 |
| grime | −5,4 | −5,1 | −11,5 | −7,5 | −9,6 | −14,3 |
| hard_techno | −9,1 | −5,8 | −5,4 | −6,4 | −13,5 | −13,2 |
| house | −5,6 | −3,2 | −9,6 | −10,5 | −17,2 | −16,3 |
| industrial_techno | −6,4 | −3,6 | −9,6 | −7,4 | −15,7 | −17,5 |
| jersey_club | −3,8 | −5,4 | −10,4 | −7,6 | −18,4 | −19,0 |
| jump_up | −6,3 | −7,3 | −7,4 | −5,9 | −10,9 | −12,8 |
| jungle | −6,3 | −3,6 | −9,1 | −9,7 | −12,0 | −15,1 |
| liquid_dnb | −5,6 | −4,1 | −10,2 | −8,2 | −13,2 | −14,4 |
| lofi | −4,3 | −2,9 | −12,5 | −15,0 | −17,8 | −22,2 |
| minimal | −6,0 | −2,9 | −9,1 | −10,6 | −17,7 | −20,6 |
| neurofunk | −3,7 | −6,0 | −9,2 | −12,5 | −10,8 | −12,4 |
| phonk | −4,2 | −4,3 | −9,9 | −9,7 | −16,1 | −19,7 |
| pop | −5,7 | −3,1 | −10,7 | −9,7 | −14,7 | −18,0 |
| reggaeton | −6,6 | −3,5 | −9,4 | −8,7 | −12,4 | −16,0 |
| retro_techno | −5,0 | −3,5 | −11,2 | −11,1 | −12,9 | −15,8 |
| riddim | −4,4 | −6,3 | −9,0 | −7,6 | −14,2 | −12,5 |
| speed_garage | −5,9 | −3,2 | −8,9 | −10,9 | −15,2 | −16,0 |
| synthwave | −6,1 | −3,2 | −10,2 | −9,0 | −13,7 | −17,7 |
| tech_house | −5,4 | −4,1 | −9,8 | −8,1 | −15,6 | −15,2 |
| techno | −4,8 | −4,5 | −7,5 | −9,9 | −16,2 | −18,4 |
| trance | −6,2 | −4,8 | −7,5 | −7,6 | −15,3 | −14,0 |
| trap | −3,3 | −4,8 | −12,7 | −10,2 | −15,3 | −18,6 |
| uk_garage | −7,2 | −3,6 | −10,6 | −7,4 | −12,9 | −13,8 |
| **médiane** | −5,6 | −3,6 | −9,8 | −9,7 | −14,7 | −16,3 |

## Largeur et mouvement (S4)

Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.

| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |
|---|---|---|---|---|---|
| acid_house | −10,0 (+7,5) | 0,82 | 0,996 | – | −3,3 (−0,3) |
| acid_techno | −12,2 (+8,2) | 0,89 | 0,997 | – | −3,1 (−0,2) |
| acidcore | −9,9 (+7,9) | 0,82 | 0,992 | – | −1,9 (−0,3) |
| afro_house | −14,1 (+3,1) | 0,93 | 1,000 | 30,6 | −1,3 (−0,1) |
| afrobeats | −11,8 (+4,2) | 0,88 | 1,000 | 22,3 | −3,8 (−0,1) |
| amapiano | −11,2 (+6,7) | 0,86 | 1,000 | 27,9 | −1,4 (−0,2) |
| boom_bap | −15,2 (+5,5) | 0,94 | 0,999 | 25,1 | −3,2 (−0,1) |
| deep_house | −13,1 (+4,0) | 0,91 | 0,999 | 32,0 | −4,0 (−0,1) |
| detroit | −12,2 (+6,3) | 0,89 | 1,000 | 18,4 | −4,3 (−0,2) |
| disco | −10,8 (+5,0) | 0,84 | 1,000 | 14,3 | −3,3 (−0,2) |
| dnb | −9,1 (+3,1) | 0,78 | 1,000 | 13,7 | −4,4 (−0,2) |
| drill | −14,4 (+5,6) | 0,93 | 1,000 | 24,4 | −3,2 (−0,1) |
| dubstep | −9,4 (+2,9) | 0,80 | 1,000 | 28,2 | −6,0 (−0,2) |
| gabber | −7,9 (+4,7) | 0,72 | 0,999 | 11,1 | −6,0 (−0,3) |
| grime | −9,2 (+6,3) | 0,79 | 1,000 | 56,8 | −5,1 (−0,3) |
| hard_techno | −8,8 | 0,77 | 0,997 | 8,0 | −5,8 |
| house | −12,5 (+5,7) | 0,89 | 1,000 | 28,5 | −3,2 (−0,2) |
| industrial_techno | −9,6 (+6,4) | 0,80 | 0,997 | 16,7 | −3,6 (−0,3) |
| jersey_club | −11,5 (+5,7) | 0,87 | 1,000 | 9,6 | −5,4 (−0,2) |
| jump_up | −8,2 (+1,2) | 0,74 | 0,999 | 28,9 | −7,3 (−0,1) |
| jungle | −8,6 (+3,7) | 0,76 | 1,000 | 13,1 | −3,6 (−0,2) |
| liquid_dnb | −9,0 (+6,0) | 0,78 | 1,000 | 25,5 | −4,1 (−0,3) |
| lofi | −15,1 (+8,4) | 0,94 | 1,000 | 23,3 | −2,9 (−0,1) |
| minimal | −10,2 (+2,4) | 0,83 | 0,999 | – | −2,9 (−0,1) |
| neurofunk | −11,8 (+3,2) | 0,88 | 1,000 | 19,5 | −6,0 (−0,1) |
| phonk | −16,6 (+6,5) | 0,96 | 1,000 | 19,6 | −4,3 (−0,1) |
| pop | −10,8 (+6,9) | 0,85 | 1,000 | 16,5 | −3,1 (−0,2) |
| reggaeton | −11,6 (+2,6) | 0,87 | 1,000 | 17,3 | −3,5 (−0,1) |
| retro_techno | −13,9 (+5,3) | 0,92 | 1,000 | 49,0 | −3,5 (−0,1) |
| riddim | −11,0 (+1,5) | 0,85 | 0,999 | 0,0 | −6,3 |
| speed_garage | −12,3 (+5,4) | 0,89 | 1,000 | 8,1 | −3,2 (−0,2) |
| synthwave | −11,2 (+4,6) | 0,86 | 1,000 | 15,2 | −3,2 (−0,2) |
| tech_house | −10,6 (+5,7) | 0,84 | 1,000 | 5,3 | −4,1 (−0,2) |
| techno | −9,8 (+3,2) | 0,81 | 0,999 | 28,1 | −4,5 (−0,2) |
| trance | −9,6 | 0,80 | 0,999 | 12,4 | −4,8 |
| trap | −13,3 (+5,8) | 0,91 | 1,000 | 34,2 | −4,8 (−0,1) |
| uk_garage | −10,1 (+7,9) | 0,82 | 1,000 | 12,4 | −3,6 (−0,3) |

## Basse au téléphone (docs/hits/basses-qui-tapent.md § 8)

Les basses du morceau (pistes de basse, Sub, Sub drop, pad de la 808 : `META bass`) rendues seules avant la chaîne master, au Drop (ou au Refrain), à travers un « téléphone » : passe-haut de Butterworth d'ordre 4 à 200 Hz (bon téléphone, B1) ou 300 Hz (téléphone moyen, B2), puis la pondération K. Perte de sonie contre la basse en pleine bande : plus proche de 0, mieux elle passe. B3 : la basse filtrée à 300 Hz contre le morceau filtré pareil. B4 : énergie 200–800 Hz contre 30–100 Hz. B5 : RMS 5 ms le plus fort des 40 premières ms d'une note contre la médiane 80–250 ms (notes de 250 ms au moins). B7, B8 : le morceau exporté. B9 : RMS de la basse 0–50 ms après chaque kick contre 100–200 ms après. B12 : le morceau avant master avec l'exciteur contre sans.

| Style | B1 tél. 200 | B2 tél. 300 | B3 place | B4 harm. | B5 attaque (notes) | B6 écart | B7 sub | B8 infra | B9 kick | B12 exciteur |
|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | – | – | – | – | – (0) | – | −6,5 (−0,2) | −24,6 (−0,6) | – | – |
| acid_techno | – | – | – | – | – (0) | – | −5,0 (−0,1) | −27,3 (−0,5) | – | – |
| acidcore | – | – | – | – | – (0) | – | −11,7 (−0,4) | −29,1 (−0,4) | – | – |
| afro_house | −6,4 | −10,0 | −8,7 (−0,5) | −4,9 | +5,8 (115) | 10,75 | −12,4 (−0,1) | −26,1 | −6,0 | +0,06 |
| afrobeats | −6,2 | −10,9 | −7,1 (−1,0) | −6,1 | +4,7 (63) | 21,91 | −4,9 (−0,1) | −28,1 (−0,1) | −0,2 | +0,15 |
| amapiano | −7,3 | −10,8 | −7,7 (−1,1) | −5,9 | +7,8 (118) | 2,73 | −22,4 (−0,4) | −29,9 (−0,3) | −11,2 | +0,19 |
| boom_bap | −6,9 | −12,1 | −7,9 (−0,5) | −6,4 | +4,1 (52) | 35,20 | −5,2 (−0,1) | −25,2 | +0,1 | +0,06 |
| deep_house | −6,8 | −11,3 | −9,8 (−0,5) | −6,2 | +2,7 (108) | 27,75 | −4,3 (−0,1) | −25,3 | +1,1 | +0,10 |
| detroit | −6,1 | −10,5 | −13,6 (−0,6) | −5,3 | +85,4 (189) | 43,25 | −4,9 (−0,2) | −26,6 (−0,3) | −1,3 | +0,02 |
| disco | −2,5 | −5,7 | −5,2 (−0,6) | 12,5 | +5,8 (114) | 3,73 | −8,8 (−0,3) | −25,1 (−0,2) | +2,3 | +0,03 |
| dnb | −4,5 | −7,2 | −7,7 | −3,8 | +3,5 (147) | 35,51 | −7,2 (−0,2) | −26,5 (−0,2) | −1,2 | +0,05 |
| drill | −5,9 | −8,6 | −5,0 (−0,5) | −6,7 | +2,4 (24) | 0,23 | −5,7 (−0,1) | −25,3 (−0,1) | +0,8 | +0,02 |
| dubstep | −3,7 | −6,0 | −6,9 | −4,4 | +3,9 (148) | 28,38 | −4,8 (−0,2) | −25,6 (−0,2) | −2,0 | +0,10 |
| gabber | −9,8 | −12,8 | −34,0 (−0,7) | −13,3 | – (0) | – | −5,9 (−0,4) | −29,2 (−0,4) | +5,2 | +0,00 |
| grime | −4,5 | −6,4 | −9,8 (−0,9) | −6,4 | +5,2 (112) | 2,66 | −6,7 (−0,4) | −20,5 (−0,4) | −0,7 | +0,06 |
| hard_techno | −10,3 | −13,4 | −34,1 | −13,9 | – (0) | – | −9,2 | −26,6 | +4,6 | +0,00 |
| house | −6,0 | −10,4 | −10,1 (−0,6) | −3,6 | +85,5 (108) | 48,26 | −5,8 (−0,2) | −24,7 (−0,1) | −1,4 | +0,10 |
| industrial_techno | −11,5 | −15,4 | −35,4 (−0,8) | −15,7 | – (0) | – | −6,4 (−0,3) | −29,9 (−0,3) | +4,6 | +0,00 |
| jersey_club | −6,6 | −10,6 | −11,7 (−0,6) | −8,4 | +3,8 (36) | 0,70 | −4,9 (−0,2) | −24,8 (−0,2) | +2,2 | +0,02 |
| jump_up | −1,7 | −2,8 | −7,1 | −0,3 | +8,1 (233) | 37,70 | −6,5 (−0,1) | −23,7 (−0,1) | +2,0 | +0,01 |
| jungle | −5,9 | −10,2 | −10,9 | −5,8 | +3,8 (129) | 29,63 | −6,9 (−0,2) | −24,1 (−0,2) | −0,9 | +0,07 |
| liquid_dnb | −6,4 | −11,0 | −11,3 | −5,6 | +4,6 (123) | 59,11 | −5,8 (−0,4) | −27,3 (−0,3) | −1,4 | +0,08 |
| lofi | −7,3 | −12,6 | −6,9 | −7,7 | +5,8 (34) | 1,54 | −4,7 (−0,1) | −24,7 (−0,1) | −0,6 | +0,04 |
| minimal | −6,1 | −9,6 | −8,8 | −5,7 | +6,2 (231) | 8,67 | −6,0 (−0,1) | −27,0 (−0,2) | +105,0 | +0,17 |
| neurofunk | −2,3 | −3,8 | −7,2 | −2,1 | +5,7 (211) | 35,51 | −3,8 (−0,1) | −23,5 | +0,0 | +0,05 |
| phonk | −3,8 | −5,4 | −2,4 | −4,7 | – (0) | – | −4,4 (−0,1) | −21,7 (−0,1) | +1,1 | −0,01 |
| pop | −6,2 | −10,6 | −9,1 | −5,2 | +4,3 (74) | 1,43 | −7,4 (−0,3) | −21,2 (−0,2) | −0,1 | +0,15 |
| reggaeton | −6,1 | −8,5 | −8,4 | −6,6 | +3,4 (85) | 0,65 | −6,7 (−0,1) | −26,6 (−0,1) | +3,5 | −0,01 |
| retro_techno | −5,3 | −7,3 | −7,8 | −5,4 | +6,1 (78) | 19,85 | −6,5 (−0,1) | −22,0 (−0,1) | +1,1 | +0,18 |
| riddim | −2,1 | −2,9 | −4,9 | −2,3 | +5,8 (207) | 25,70 | −7,4 (−0,1) | −19,6 (−0,1) | −1,3 | +0,07 |
| speed_garage | −4,8 | −9,0 | −7,7 | −2,8 | +5,0 (88) | 25,03 | −6,8 (−0,2) | −25,9 (−0,1) | −0,9 | +0,19 |
| synthwave | −5,2 | −7,2 | −6,2 | −4,3 | +4,0 (51) | 2,90 | −6,2 (−0,2) | −27,6 | −1,7 | +0,08 |
| tech_house | −7,2 | −11,5 | −13,2 | −8,6 | +6,4 (69) | 67,93 | −5,6 (−0,2) | −22,5 (−0,2) | +4,3 | +0,06 |
| techno | −4,6 | −6,5 | −6,0 | −5,1 | +6,6 (116) | 14,40 | −4,9 (−0,2) | −26,4 (−0,2) | +0,2 | +0,06 |
| trance | −6,0 | −9,1 | −14,7 | −5,0 | +2,7 (174) | 0,81 | −6,3 | −26,5 | −1,7 | +0,05 |
| trap | −6,7 | −10,4 | −8,7 | −9,2 | +3,1 (57) | 5,96 | −4,9 (−0,1) | −25,0 (−0,1) | +0,5 | +0,02 |
| uk_garage | −5,1 | −10,4 | −11,0 | −0,5 | +5,8 (114) | 71,18 | −7,5 (−0,2) | −26,1 (−0,2) | −1,1 | +0,12 |

## Défauts

| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |
|---|---|---|---|---|---|
| acid_house | 0 | 0 | 0,0000 | −33,3 | 192,0 |
| acid_techno | 0 | 0 | 0,0000 | −37,2 | 306,1 |
| acidcore | 0 | 0 | 0,0000 | −33,0 | 280,2 |
| afro_house | 0 | 0 | 0,0000 | −40,2 | 188,8 |
| afrobeats | 0 | 0 | 0,0000 | −52,2 | 192,0 |
| amapiano | 0 | 0 | 0,0000 | −44,7 | 205,7 |
| boom_bap | 0 | 0 | 0,0000 | −35,0 | 167,0 |
| deep_house | 0 | 0 | 0,0000 | −43,0 | 192,0 |
| detroit | 0 | 0 | 0,0000 | −44,6 | 335,2 |
| disco | 0 | 0 | 0,0000 | −45,7 | 198,6 |
| dnb | 0 | 0 | 0,0000 | −32,9 | 297,9 |
| drill | 0 | 0 | 0,0000 | −39,9 | 175,8 |
| dubstep | 0 | 0 | 0,0000 | −41,6 | 301,7 |
| gabber | 0 | 0 | 0,0000 | −39,0 | 296,2 |
| grime | 0 | 0 | 0,0000 | −38,4 | 178,3 |
| hard_techno | 0 | 0 | 0,0000 | −35,2 | 358,4 |
| house | 0 | 0 | 0,0000 | −44,4 | 188,8 |
| industrial_techno | 0 | 0 | 0,0000 | −36,7 | 310,6 |
| jersey_club | 0 | 0 | 0,0000 | −40,7 | 178,3 |
| jump_up | 0 | 0 | 0,0000 | −32,4 | 297,9 |
| jungle | 0 | 0 | 0,0000 | −37,8 | 325,8 |
| liquid_dnb | 0 | 0 | 0,0000 | −32,8 | 297,9 |
| lofi | 0 | 0 | 0,0000 | −39,8 | 182,9 |
| minimal | 0 | 0 | 0,0000 | −42,8 | 330,0 |
| neurofunk | 0 | 0 | 0,0000 | −33,8 | 297,9 |
| phonk | 0 | 0 | 0,0000 | −40,6 | 192,0 |
| pop | 0 | 0 | 0,0000 | −42,7 | 206,9 |
| reggaeton | 0 | 0 | 0,0000 | −37,7 | 210,0 |
| retro_techno | 0 | 0 | 0,0000 | −38,6 | 330,0 |
| riddim | 0 | 0 | 0,0000 | −40,8 | 308,6 |
| speed_garage | 0 | 0 | 0,0000 | −44,2 | 177,2 |
| synthwave | 0 | 0 | 0,0000 | −40,0 | 192,0 |
| tech_house | 0 | 0 | 0,0000 | −44,7 | 182,9 |
| techno | 0 | 0 | 0,0000 | −41,2 | 320,0 |
| trance | 0 | 0 | 0,0000 | −43,6 | 306,1 |
| trap | 0 | 0 | 0,0000 | −41,1 | 178,3 |
| uk_garage | 0 | 0 | 0,0000 | −35,6 | 196,4 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
375884838d8fdabcaa15e3fe4aa3cdb02d82fa7e0df24b833ae55478d962ba79  acid_house.wav
bca93d19e26d9d256f33f977cde582424a3d4748b2b58872594bad9645bcd255  acid_techno.wav
7759b09a91ff5b277b31935e40e7bc570f1df92f35c0b2328e45158178f50398  acidcore.wav
d69d7c4771ddcd630cdf8616149ed0f55fe10aaa63480119a7d89add6836b1c3  afro_house.wav
eff904d3d2c643b9baa518901d7c5ca820a78db7572391bd02b08f52641472d8  afrobeats.wav
ef1bbaa2523143714c580b81ca9773d1c10909c6d56bf4e34e8c238be64f852d  amapiano.wav
7a07a38f6cfc0b7af1ae315d3c7c7aee87d3f050d5367f7d6dc7f6492cba8c0a  boom_bap.wav
1fa2bea900fac989f72c74024f4c54cc45117d9fec6d781e1f501018280798fc  deep_house.wav
dd1d558ccb5e6c26b12d7d161ba267bea2d274c1708331f850afadf1881c1db0  detroit.wav
35991438fbed0d0f68a5a3e09ecab4860bdd743da653abe3bb50acb0821d87d2  disco.wav
c27d4ba4d6e9244762947ac16b02baab987b96fb6f47efc3fb09c56c82204f59  dnb.wav
fd6cb7873954f6cc9f562ee810a771abc812d8b20c6af26d000933ee645969fd  drill.wav
d2724f93d2a8ed1ba2419da541ae797faa483268b43fd806c0be0297bc183b28  dubstep.wav
943b81d55758a89dc4c959cfee38666717a1df8c34ac69e16e350f53570f90ca  gabber.wav
86b0bc9620bdf09f3b415af12b2e38e8270479c5d8c346b3ee4b3607f35802e4  grime.wav
395b66e2278403d6d659f830c584bd0e29526b3dc57d830b8b1c3f60f24d077e  hard_techno.wav
41d642f7076dd7ae92e126bca28c8c1183281236f6a57304814484b1b13e71a9  house.wav
b22ec2d22987b8425e74421d7269d278276b061b861d1509a0009cb0a9c32805  industrial_techno.wav
ffa03cef3a27e35d7db11041f3f9616bc21133e99d7d94d07ac4fa3a9e6908dd  jersey_club.wav
eb28defdc76bf437137af1ae8a807832e849d7564246f37fbe2699c1b3f313dc  jump_up.wav
e633b641412fd0e7bc7ad0ac4f4d4f205b5e89859b8daafbf63bf9213894731d  jungle.wav
c4d23566dff81a96518b205328875e40eeb7311cb9fd5f03db5e22c09438bcb5  liquid_dnb.wav
5134bd5cbe7d8e7ab7a6f79f67cfc9ad60ba9cad56a69647ea7cbab79200415d  lofi.wav
70acf42fcb4c9169abb40de14a0d769f62b5849d91e542019e94b9e1d4463c1f  minimal.wav
65f4077a0da52fc7c1813f91ab5336032d5e158aab74d9e090fa54c41c56d8ef  neurofunk.wav
e9d1a6000c2be0e4058abc471dff74922d41ff4d567193752c117c243082b7ba  phonk.wav
340b245beebdbfe5d7cb7cf5b87ddd6f0c94b8e296ed66d803d128a3f8c07f38  pop.wav
804d5d21a8a9241c7870de73381a5fb0231efd8288352ba9a2f36b490f312a7a  reggaeton.wav
c4b163343eb0ee04d748efac401fb779922abbe57e32a3803778a4dee2e4b236  retro_techno.wav
e67c6a53cb25a88dfa9732c3a3d5f591fbe7cb5898b030649dc60abf18ef30aa  riddim.wav
337ab9f1238d8ffec77a608514a9d0d5ebf9833b6573cfb7a29ef527ce73a803  speed_garage.wav
1229d786155c0f162da0eb401d4095b69714db99927d1a0514d350c04eea311b  synthwave.wav
a121d7ed342e1b9ee6ca6e3ed4dbcf7a158a660a4e3b9cc593bbaaa9ba5a797a  tech_house.wav
07c40eaadc70631fad9d8231809b4afa203f02cf7925822fe07e54380104099f  techno.wav
8844a72c0138646d3d6d8c99394b818a8d74e5fa080046a2d8907abde8156bd0  trance.wav
81ead9b8d3d851f8ad1aebadfc9b026a01f0a6598c9ce49a352abc6cc6554131  trap.wav
7d1c69153bbddc7ff67b9a0bbfd70d2e8e8ba8097570b580e4c53aeff93ca80f  uk_garage.wav
```
