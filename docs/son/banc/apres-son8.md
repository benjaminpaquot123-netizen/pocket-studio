# Banc de mesure du son : son 8 (leads lots B à D, restes de S7 et des relectures)

Point de départ : la branche `g4` (main + Guide G4), dont le son est celui de S7 (vérifié : ses morceaux donnent au banc les mêmes mesures que `apres-S7.json`). « Avant » = S7 (`apres-S7.json`, `S7-telephone.json`, `S7-direct.json`) ; les voix seules et la sonde ont été mesurées de nouveau sur les enregistrements et les sons d'avant. Plan : `docs/hits/leads-pros.md` § 6 et § 8 (lots B, C, D) ; restes : `docs/REVIEW.md` (« S6 son », « S7 son »).

## 1. Ce que fait son 8

- **Moteur, lot B (le toucher et l'attaque)** : le filtre suit la note (`KeyTrack`, 0 à 1 octave par octave autour de Do4) ; la vélocité règle l'enveloppe du filtre (`VelocityEnv`, en plus de `VelocityCutoff` déjà là) ; un clic de bruit de 4 ms passé au passe-haut à 2 kHz par-dessus le filtre (`Click`) ; le vibrato de chaque note attend puis arrive (`VibratoDelay`) ; le cycle de 8 mesures peut être décalé (`CycleOffset` 0,5 : ouvert au début de la phrase) ; l'enveloppe de hauteur des basses sert au « tick » des plucks.
- **Moteur, lot C (l'unisson et le filtre)** : la supersaw façon JP-8000 (`Supersaw`) : écarts de Szabo, phases tirées à chaque note d'une graine que le transport relance (un export est le même deux fois), dérive de ±3 cents au plus par copie, centre 0,72 et copies 0,50, passe-haut 12 dB sur la note ; un ladder 4 pôles linéaire (`CleanLadder`, Zavalishin) ; un drive avant le filtre (`PreDrive`, anti-repliement par primitive) ; le chorus prend une entrée stéréo (5ᵉ réglage « Entrée » : le centre passe dans le chorus, le côté passe tel quel ; avant, tout était sommé en mono).
- **Moteur, lot D** : PWM des ondes carrées (`Pwm`, triangle à 0,7 Hz, sans continu) ; une paire FM 2 opérateurs (`Fm`, `FmRatio`) dont l'indice suit l'enveloppe du filtre.
- **Rien ne change pour les morceaux d'avant** : les onze réglages sont ajoutés en fin de liste ; au repos le synthé prend son code de rendu d'avant, tel quel (`renderVoices`), le nouveau (`renderLeads`) seulement si l'un d'eux bouge. Vérifié : les enregistrements d'avant (house, liquid DnB, dubstep, synthwave, techno) rendent **les mêmes WAV octet pour octet** avec le moteur de son 8 et celui de départ. Un chorus enregistré avec 4 réglages reste mono et ne repart pas avec le transport (le relancer changeait l'export de la techno).
- **La bibliothèque** (sons choisis maintenant et morceaux de l'assistant) : leads avec suivi de clavier 0,5, vélocité sur la coupure et l'enveloppe, vibrato retardé de 250 ms, petit clic ; Supersaw, Lead trance, Hoover et nappes à unisson en JP-8000 (gain rendu à la sonie d'avant : +1,3 à +2,1 dB) ; le Hoover avec PWM et une plongée de 3 demi-tons ; plucks suivis à 100 %, clic, tick de 12 demi-tons en 4 ms, « aboiement » à la vélocité ; Arpège, Cloche, Orgue et Piano électrique sur le ladder propre. Un projet fait avant garde ses sons (et leurs noms).
- **Les morceaux de l'assistant** : le suivi de clavier du moteur remplace celui de la recette ; couches du lot D : lead Supersaw + « Lead carré » une octave au-dessus à −12 dB (la morsure), lead Pluck + « Nappe » à −14 dB, piano ou clavier FM + « Nappe » une octave au-dessus à −18 dB (l'air) ; chorus des leads et des claviers FM en entrée stéréo ; courbe de phrase des nappes ±0,6 octave (±0,4 avant) ; un son de lead joué en basse (le grime) garde son réglage d'avant.
- **Restes de S7** : deep house, dubstep, neurofunk et phonk plus larges (leads 1,0, nappes 1,0 même en bass music, clap ou caisse claire 0,8) ; le reggaeton sans étalement (la nappe sous son pluck l'élargit déjà) ; Break de dubstep et liquid 0,4 et 0,3 dB plus bas, Couplet de synthwave 0,3 dB plus bas (leurs contrastes avaient reculé de 0,1 à 0,4 LU).
- **Restes des relectures** : l'« Étalement » glisse sur un bloc quand on le tourne ; en version de mix 4 le clic du métronome repasse sous le fader du master ; le chorus stéréo repart avec le transport (direct = export) ; le gain du direct est estimé de nouveau 1,5 s après le dernier changement de son ou de mix.

## 2. Méthode

- Export : `scripts/song-bench.sh --premaster --m4a --before apres-S7.json` sur les 37 styles (`apres-son8.json`) ; dubstep, liquid DnB, synthwave et reggaeton enregistrés et rendus de nouveau après leurs derniers réglages, puis fusionnés (rapport refait par `scripts/bench/report.py`).
- Voix seules : chaque lead, nappe et contre-chant (`META lead/pad/counter`) rendu seul (`ps_render --solo`), chaîne master en place, premier Drop (ou Refrain), méthode de `apres-leads-1.md` (scratchpad `son8/tools/voices.py`, qui reprend `leads/py/stemstats.py`).
- Sons seuls : la sonde des leads (scratchpad `leads/probe/probe.cpp`, ici à 59 réglages) sur les sons de la bibliothèque d'avant (lot 1) et d'après.
- Largeur et téléphone : `docs/son/banc/audit35/outils/phone.py` (8 premières mesures du premier Drop).
- Direct : `scripts/live-bench.sh` (150 s), comparé à l'export sur la même portion (`son8-direct.json`).
- Tests hôtes : `engine/host-tests/leads2_tests.inc` (les tests nommés au § 8 du plan, plus PWM, FM, drive, chorus, étalement, métronome).

## 3. Le mix (médianes, 37 styles)

| Mesure | Après S7 | Son 8 | Cible |
|---|---|---|---|
| Partie la plus forte (LUFS) / true peak WAV | −8,5 / −1,19 | −8,5 / −1,19 | inchangés |
| True peak du M4A décodé (dBTP) | −1,47 | −1,51 | ≤ −1 (37/37) |
| Côté/centre du Drop (dB, phone.py) | −11,7 (33/37) | **−11,2 (37/37** ; −14,9 à −9,2) | −15 à −9 |
| Corrélation G/D sous 120 Hz | 1,00 | 1,00 (pire 0,993) | ≈ 1 |
| Perte en mono, morceau (LU) | −0,54 | −0,60 (pire −1,25) | ≥ −1,5 |
| Perte au haut-parleur de téléphone (LU) | 3,8 (23/37 ≤ 4) | **3,6** (24/37) | ≤ 4 |
| Drop − Break (LU) | +5,0 (32/37 ≥ +4) | +5,0 (32/37, les mêmes) | ≥ +4 |
| Drop − Groove / Couplet (LU) | +2,5 (36/37) | +2,4 (36/37, acidcore déjà dessous) | ≥ +2 |
| Drop − fin de Montée (LU) | ≥ +1 partout | ≥ +1 partout (37/37) | bloquant |
| Direct : Intro − export (LU) | 0,0 (35/37) | 0,0 (35/37 ; riddim −1,56, jersey −1,27) | 0 ± 1 |
| Direct : Drop − export (LU) | 0,0 (37/37) | 0,0 (37/37) | 0 ± 1 |
| Direct : alertes | 0 | 0 (37/37) | 0 |

Les bloquants du banc tiennent tous (true peak, M4A, sonie ou plafond, perte, continu, NaN, Drop − fin de Montée, basse au téléphone, infra).

Par style (après S7 → son 8) :

| Style | Côté/centre du Drop (dB) | Perte tél. (LU) | Corr. < 120 Hz | Présence tél. (dB) |
|---|---|---|---|---|
| acid_house | −10,2 → **−10,2** | 3,0 → **3,0** | 1,00 | −2,7 → **−2,7** |
| acid_techno | −12,2 → **−12,2** | 4,0 → **4,0** | 1,00 | −3,0 → **−3,0** |
| acidcore | −9,8 → **−9,8** | 3,3 → **3,3** | 0,99 | −6,3 → **−6,3** |
| afro_house | −14,7 → **−13,3** | 6,1 → **6,0** | 1,00 | −6,8 → **−6,8** |
| afrobeats | −11,2 → **−11,1** | 5,4 → **5,3** | 1,00 | −9,2 → **−9,1** |
| amapiano | −11,4 → **−11,7** | 5,0 → **5,2** | 1,00 | −5,4 → **−5,4** |
| boom_bap | −13,6 → **−13,5** | 6,2 → **6,1** | 1,00 | −7,1 → **−7,0** |
| deep_house | −15,5 → **−14,9** | 5,6 → **5,5** | 1,00 | −8,0 → **−8,0** |
| detroit | −12,1 → **−12,1** | 2,8 → **2,8** | 1,00 | −2,2 → **−2,2** |
| disco | −12,4 → **−11,1** | 3,7 → **3,7** | 1,00 | −8,6 → **−8,6** |
| dnb | −14,6 → **−14,6** | 3,8 → **3,8** | 1,00 | −8,4 → **−8,4** |
| drill | −14,8 → **−13,8** | 5,0 → **5,0** | 1,00 | −6,9 → **−6,8** |
| dubstep | −16,0 → **−13,4** | 3,9 → **3,6** | 1,00 | −9,5 → **−9,6** |
| gabber | −10,3 → **−9,3** | 3,0 → **2,6** | 1,00 | −7,4 → **−7,0** |
| grime | −10,8 → **−9,6** | 2,4 → **2,3** | 1,00 | −4,4 → **−4,4** |
| hard_techno | −11,7 → **−12,0** | 2,2 → **1,9** | 1,00 | −6,0 → **−5,6** |
| house | −14,3 → **−13,6** | 4,9 → **4,8** | 1,00 | −6,6 → **−6,5** |
| industrial_techno | −9,8 → **−9,8** | 3,0 → **3,0** | 1,00 | −8,5 → **−8,5** |
| jersey_club | −12,5 → **−11,2** | 3,8 → **3,7** | 1,00 | −7,1 → **−7,1** |
| jump_up | −10,0 → **−10,0** | 1,3 → **1,3** | 1,00 | −4,8 → **−4,8** |
| jungle | −10,3 → **−10,3** | 3,5 → **3,5** | 1,00 | −6,9 → **−6,9** |
| liquid_dnb | −9,6 → **−9,3** | 3,0 → **3,0** | 1,00 | −5,0 → **−5,0** |
| lofi | −13,6 → **−13,2** | 6,6 → **6,5** | 1,00 | −6,6 → **−6,5** |
| minimal | −10,6 → **−11,0** | 4,9 → **4,7** | 1,00 | −6,8 → **−6,9** |
| neurofunk | −16,8 → **−12,2** | 3,3 → **3,0** | 1,00 | −7,0 → **−7,0** |
| phonk | −16,9 → **−13,9** | 4,1 → **3,9** | 1,00 | −5,6 → **−5,6** |
| pop | −11,3 → **−10,9** | 4,2 → **4,1** | 1,00 | −6,2 → **−6,0** |
| reggaeton | −11,0 → **−9,2** | 2,7 → **2,6** | 1,00 | −5,6 → **−5,5** |
| retro_techno | −14,6 → **−13,0** | 3,9 → **3,5** | 1,00 | −5,0 → **−4,8** |
| riddim | −13,6 → **−13,6** | 2,6 → **2,6** | 1,00 | −6,9 → **−6,9** |
| speed_garage | −13,8 → **−13,6** | 4,5 → **4,5** | 1,00 | −7,9 → **−7,8** |
| synthwave | −11,2 → **−9,8** | 3,7 → **3,5** | 1,00 | −5,1 → **−5,0** |
| tech_house | −11,5 → **−11,2** | 3,3 → **3,3** | 1,00 | −5,8 → **−5,8** |
| techno | −10,2 → **−10,2** | 4,2 → **4,2** | 1,00 | −6,6 → **−6,4** |
| trance | −9,8 → **−10,5** | 2,5 → **2,4** | 1,00 | −7,1 → **−6,3** |
| trap | −14,4 → **−13,1** | 4,4 → **4,3** | 1,00 | −3,7 → **−3,7** |
| uk_garage | −10,4 → **−10,8** | 2,8 → **2,9** | 1,00 | −5,3 → **−5,3** |

## 4. Les voix seules (premier Drop)

| Voix | Mesure | Avant | Après | Cible | Styles dans la cible |
|---|---|---|---|---|---|
| Lead | Centroïde (Hz) | 1105 | **1240** | 1500 à 3000 | 8/34 → **10/34** |
| Lead | Brillance 4–10 k / 1–4 k (dB) | −9,2 | **−9,2** | ≥ -20 | 26/34 → **26/34** |
| Lead | Boue 150–500 / 1–4 k (dB) | −1,8 | **−3,3** | ≤ 4 | 22/34 → **25/34** |
| Lead | Présence téléphone 1–4 kHz (dB) | −5,2 | **−5,1** | ≥ -8 | 26/34 → **26/34** |
| Lead | Mouvement sur 8 mesures (dB) | 4,87 | **4,26** | ≥ 2,5 | 27/34 → **24/34** |
| Lead | Centroïde mesure 1 / médiane | 1,00 | **1,00** | ≥ 0,9 | 31/34 → **32/34** |
| Lead | Attaque (dB) | 2,7 | **2,6** | – | – |
| Lead | Côté/centre (dB) | −2,7 | **−2,1** | – | – |
| Nappe / accords | Centroïde (Hz) | 700 | **750** | 1500 à 3000 | 2/30 → **2/30** |
| Nappe / accords | Brillance 4–10 k / 1–4 k (dB) | −10,8 | **−10,4** | ≥ -20 | 29/30 → **30/30** |
| Nappe / accords | Boue 150–500 / 1–4 k (dB) | 6,3 | **5,8** | ≤ 4 | 8/30 → **10/30** |
| Nappe / accords | Présence téléphone 1–4 kHz (dB) | −6,9 | **−6,7** | ≥ -8 | 24/30 → **25/30** |
| Nappe / accords | Mouvement sur 8 mesures (dB) | 2,12 | **2,27** | ≥ 2,5 | 9/30 → **12/30** |
| Nappe / accords | Centroïde mesure 1 / médiane | 1,05 | **1,08** | ≥ 0,9 | 29/30 → **29/30** |
| Nappe / accords | Attaque (dB) | 4,0 | **4,3** | – | – |
| Nappe / accords | Côté/centre (dB) | −1,6 | **−0,3** | – | – |
| Contre-chant | Centroïde (Hz) | 826 | **849** | 1500 à 3000 | 1/6 → **1/6** |
| Contre-chant | Brillance 4–10 k / 1–4 k (dB) | −14,1 | **−14,5** | ≥ -20 | 4/6 → **4/6** |
| Contre-chant | Boue 150–500 / 1–4 k (dB) | 4,1 | **3,6** | ≤ 4 | 3/6 → **3/6** |
| Contre-chant | Présence téléphone 1–4 kHz (dB) | −5,7 | **−6,0** | ≥ -8 | 6/6 → **6/6** |
| Contre-chant | Mouvement sur 8 mesures (dB) | 0,82 | **1,06** | ≥ 2,5 | 0/6 → **0/6** |
| Contre-chant | Centroïde mesure 1 / médiane | 0,98 | **0,97** | ≥ 0,9 | 4/6 → **5/6** |
| Contre-chant | Attaque (dB) | 3,2 | **2,5** | – | – |
| Contre-chant | Côté/centre (dB) | −3,6 | **−3,1** | – | – |

| Style | Voix | Centroïde | Brillance | Boue | Présence tél. | Côté/centre | Attaque | Mouv. · mesure 1 |
|---|---|---|---|---|---|---|---|---|
| afro_house | lead | 2751 → **2780** | −7,4 → **−7,3** | −18,4 → **−18,3** | −1,3 → **−1,3** | −1,0 → **−1,0** | 6,2 → **6,3** | – → **–** · 0,95 → **0,96** |
| afro_house | pad | 509 → **513** | −18,6 → **−15,6** | 8,2 → **8,3** | −6,4 → **−6,4** | −1,6 → **0,6** | −4,0 → **5,9** | 2,14 → **2,68** · 1,10 → **1,08** |
| afrobeats | counter | 1160 → **996** | −9,2 → **−10,5** | −24,9 → **−35,6** | −5,5 → **−6,4** | −1,4 → **−0,9** | 3,2 → **2,6** | – → **–** · 0,99 → **0,91** |
| afrobeats | lead | 3317 → **3313** | −3,6 → **−3,7** | −24,9 → **−24,6** | −1,7 → **−1,7** | −2,0 → **−2,0** | 5,9 → **5,9** | 4,12 → **4,13** · 0,98 → **0,99** |
| afrobeats | pad | 533 → **563** | −15,5 → **−11,7** | 9,2 → **9,1** | −9,1 → **−9,0** | −0,9 → **−0,9** | 6,6 → **6,6** | 3,04 → **3,32** · 0,92 → **0,93** |
| amapiano | lead | 1046 → **1066** | −11,8 → **−12,3** | −5,8 → **−6,0** | −3,7 → **−3,4** | −2,7 → **−2,6** | 7,7 → **7,6** | – → **–** · 0,99 → **1,00** |
| amapiano | pad | 1028 → **998** | −9,3 → **−9,2** | 3,9 → **3,6** | −5,6 → **−5,5** | 0,9 → **1,4** | 4,3 → **3,2** | 2,18 → **2,06** · 1,37 → **1,35** |
| boom_bap | lead | 1224 → **1204** | −11,1 → **−11,4** | −32,0 → **−32,1** | −4,4 → **−4,5** | −1,9 → **−1,9** | 2,5 → **2,3** | – → **–** · 1,16 → **1,18** |
| boom_bap | pad | 351 → **370** | −18,6 → **−13,7** | 11,5 → **11,2** | −8,6 → **−8,3** | −2,9 → **−2,9** | 9,2 → **9,3** | 3,61 → **3,95** · 0,82 → **0,81** |
| deep_house | lead | 862 → **859** | −26,3 → **−26,2** | −3,5 → **−3,4** | −4,9 → **−4,9** | −1,4 → **0,2** | 1,0 → **0,8** | – → **–** · 0,70 → **0,68** |
| deep_house | pad | 522 → **650** | −18,1 → **−13,4** | 7,8 → **5,7** | −7,8 → **−6,7** | 0,5 → **1,4** | 5,2 → **4,8** | 1,73 → **1,74** · 1,05 → **1,04** |
| detroit | counter | 546 → **566** | −28,0 → **−27,6** | 5,1 → **4,5** | −5,5 → **−5,5** | −7,7 → **−7,6** | 4,3 → **4,8** | – → **–** · 0,83 → **0,96** |
| detroit | lead | 1526 → **1528** | −9,8 → **−9,7** | −8,6 → **−8,8** | −2,6 → **−2,6** | −6,4 → **−6,4** | 2,2 → **2,3** | 6,37 → **6,35** · 0,88 → **0,90** |
| detroit | pad | 1799 → **1799** | −13,8 → **−13,8** | −12,3 → **−12,3** | −0,6 → **−0,6** | −3,3 → **−3,3** | −0,9 → **−0,9** | 0,39 → **0,39** · 1,00 → **1,00** |
| disco | lead | 1771 → **1724** | −5,4 → **−6,1** | −3,2 → **−3,3** | −3,9 → **−3,8** | −1,4 → **−1,0** | −1,9 → **−2,1** | 6,20 → **6,20** · 1,22 → **1,20** |
| disco | pad | 972 → **999** | −7,2 → **−9,8** | 4,1 → **3,2** | −6,4 → **−5,7** | −0,7 → **1,7** | 4,1 → **3,9** | 2,27 → **1,98** · 0,92 → **0,99** |
| dnb | lead | 1045 → **1307** | −5,8 → **−6,6** | 3,7 → **−0,2** | −7,0 → **−5,2** | −1,7 → **−1,8** | −0,6 → **3,9** | 3,59 → **3,45** · 1,00 → **1,00** |
| dnb | pad | 840 → **856** | −8,7 → **−8,2** | 3,1 → **3,1** | −7,0 → **−7,0** | −2,5 → **−2,5** | −0,2 → **−0,2** | 2,56 → **2,93** · 1,11 → **1,19** |
| drill | lead | 1264 → **1265** | −13,0 → **−13,1** | −8,0 → **−8,1** | −3,5 → **−3,4** | −2,3 → **−2,4** | 3,4 → **3,2** | 5,89 → **5,91** · 0,97 → **0,95** |
| drill | pad | 711 → **695** | −8,5 → **−8,8** | 6,8 → **6,9** | −6,2 → **−6,1** | −1,7 → **0,2** | 1,2 → **3,8** | 3,52 → **3,67** · 0,93 → **0,92** |
| dubstep | lead | 1542 → **1565** | −10,8 → **−10,9** | −33,6 → **−33,9** | −2,4 → **−2,3** | −1,4 → **0,3** | 1,7 → **2,1** | 4,20 → **4,20** · 1,04 → **1,04** |
| dubstep | pad | 759 → **778** | −8,4 → **−8,1** | 6,1 → **6,0** | −8,0 → **−7,9** | −1,9 → **−0,2** | 3,3 → **3,3** | 1,82 → **2,20** · 1,07 → **1,13** |
| gabber | lead | 803 → **819** | −4,3 → **−4,7** | 8,3 → **8,1** | −9,7 → **−9,5** | −3,3 → **−2,4** | 4,1 → **3,5** | 2,54 → **2,42** · 1,01 → **1,01** |
| gabber | pad | 701 → **899** | −4,6 → **−5,9** | 7,9 → **4,8** | −8,0 → **−6,8** | −1,6 → **−0,6** | 2,9 → **4,4** | 2,43 → **2,79** · 1,13 → **1,14** |
| grime | lead | 3180 → **3157** | −3,1 → **−3,1** | −13,2 → **−12,8** | −2,9 → **−2,9** | −2,0 → **−0,9** | 0,4 → **0,7** | 8,69 → **8,71** · 1,46 → **1,48** |
| grime | pad | 560 → **547** | −7,7 → **−6,9** | 9,5 → **10,2** | −7,9 → **−8,3** | −2,4 → **−1,0** | 3,9 → **3,9** | 5,47 → **5,32** · 1,06 → **1,05** |
| hard_techno | lead | 945 → **1140** | −6,3 → **−6,0** | 6,0 → **3,6** | −7,7 → **−6,9** | −5,3 → **−6,6** | 4,2 → **2,5** | 3,60 → **3,89** · 0,99 → **1,00** |
| hard_techno | pad | 779 → **937** | −4,4 → **−6,5** | 7,6 → **4,4** | −7,8 → **−6,6** | −5,3 → **−5,6** | 3,5 → **4,2** | 1,84 → **1,94** · 1,06 → **1,09** |
| house | lead | 957 → **1069** | −12,6 → **−10,9** | 0,8 → **1,0** | −5,4 → **−5,6** | −1,5 → **−1,1** | 9,8 → **9,2** | 5,00 → **5,07** · 0,98 → **0,95** |
| house | pad | 699 → **768** | −17,1 → **−14,0** | 5,1 → **4,2** | −6,8 → **−6,4** | 0,9 → **1,9** | 2,7 → **3,2** | 4,49 → **3,88** · 0,91 → **0,92** |
| industrial_techno | lead | 442 → **440** | −4,8 → **−4,8** | 13,2 → **13,3** | −9,8 → **−9,9** | −4,2 → **−4,2** | 5,5 → **5,6** | 3,23 → **3,24** · 1,01 → **1,01** |
| industrial_techno | pad | 733 → **733** | −9,5 → **−9,5** | 6,1 → **6,1** | −6,9 → **−6,9** | 0,6 → **0,6** | 7,3 → **7,3** | 0,44 → **0,44** · 0,98 → **0,98** |
| jersey_club | lead | 637 → **640** | −25,5 → **−25,5** | 8,2 → **8,1** | −12,2 → **−12,2** | −2,8 → **−1,4** | 0,4 → **0,5** | 5,00 → **4,99** · 1,04 → **1,04** |
| jump_up | lead | 1154 → **1682** | −3,6 → **−4,1** | 4,0 → **−4,3** | −7,5 → **−5,2** | −4,6 → **−4,2** | 4,1 → **3,9** | 5,53 → **5,25** · 1,40 → **1,46** |
| jump_up | pad | 826 → **837** | −9,2 → **−8,9** | 3,2 → **3,2** | −6,9 → **−6,9** | −1,7 → **−1,7** | 0,7 → **0,6** | 1,88 → **2,24** · 1,10 → **1,16** |
| jungle | lead | 878 → **1216** | −4,4 → **−5,1** | 6,7 → **2,7** | −7,1 → **−5,7** | −2,7 → **−2,2** | 3,1 → **3,2** | 4,21 → **3,77** · 1,17 → **1,14** |
| jungle | pad | 647 → **656** | −14,6 → **−12,4** | 6,5 → **6,5** | −9,6 → **−9,6** | 1,0 → **1,0** | 6,5 → **6,6** | 2,06 → **2,60** · 1,06 → **1,10** |
| liquid_dnb | counter | 1569 → **1601** | −9,5 → **−9,2** | −40,5 → **−40,3** | −0,5 → **−0,5** | −3,0 → **−3,1** | 2,5 → **2,0** | – → **–** · 0,98 → **0,98** |
| liquid_dnb | lead | 1915 → **1970** | −7,3 → **−7,5** | −8,8 → **−8,5** | −3,0 → **−3,2** | −1,8 → **−1,2** | 3,5 → **3,1** | 6,36 → **4,70** · 1,04 → **1,02** |
| liquid_dnb | pad | 1160 → **1177** | −9,9 → **−9,8** | 2,4 → **2,1** | −5,1 → **−5,0** | −1,9 → **−1,6** | 2,0 → **−0,0** | 1,47 → **1,41** · 0,97 → **0,97** |
| lofi | lead | 769 → **768** | −20,4 → **−20,4** | 4,7 → **4,7** | −6,7 → **−6,7** | −3,3 → **−3,3** | 2,0 → **2,3** | 7,05 → **–** · 0,99 → **0,98** |
| lofi | pad | 779 → **788** | −14,5 → **−14,2** | 2,9 → **2,8** | −5,0 → **−5,0** | 0,8 → **1,2** | 12,2 → **13,6** | 2,37 → **2,40** · 0,98 → **0,99** |
| minimal | counter | 690 → **711** | −22,3 → **−22,3** | 4,8 → **4,1** | −7,9 → **−7,5** | −4,2 → **−3,1** | 6,2 → **6,1** | 0,82 → **1,06** · 1,00 → **1,00** |
| minimal | lead | 78 → **78** | −24,6 → **−24,6** | 35,1 → **35,1** | −29,7 → **−29,7** | −200,0 → **−200,0** | 12,7 → **12,7** | – → **–** · 1,00 → **1,00** |
| minimal | pad | 560 → **638** | −9,7 → **−8,7** | 8,6 → **8,5** | −7,4 → **−7,5** | −2,6 → **−2,8** | 5,1 → **4,6** | 1,37 → **1,04** · 1,04 → **1,08** |
| neurofunk | lead | 3434 → **3437** | 3,1 → **3,0** | −3,8 → **−3,8** | −6,9 → **−6,7** | −1,9 → **0,0** | 2,1 → **2,2** | 4,34 → **4,29** · 0,90 → **0,90** |
| phonk | lead | 933 → **936** | −23,3 → **−23,1** | −8,0 → **−8,2** | −2,9 → **−2,8** | −2,7 → **−0,7** | 10,0 → **10,2** | 3,09 → **3,06** · 1,04 → **1,04** |
| phonk | pad | 628 → **615** | −12,7 → **−11,8** | 6,1 → **6,3** | −4,8 → **−4,8** | −1,9 → **0,1** | 0,7 → **4,5** | 2,01 → **2,30** · 1,07 → **1,10** |
| pop | lead | 1401 → **1486** | −6,1 → **−5,9** | −2,0 → **−2,1** | −6,1 → **−5,9** | −2,3 → **−1,7** | 2,9 → **2,9** | 4,87 → **3,86** · 1,03 → **0,99** |
| pop | pad | 524 → **549** | −20,1 → **−17,1** | 7,5 → **7,1** | −6,9 → **−6,7** | −0,3 → **0,4** | 4,1 → **3,7** | 2,24 → **2,10** · 0,94 → **0,94** |
| reggaeton | lead | 1902 → **1955** | −8,4 → **−8,0** | −9,6 → **−8,9** | −2,5 → **−2,7** | −5,0 → **−5,7** | 3,2 → **3,1** | 5,41 → **5,17** · 1,15 → **1,03** |
| reggaeton | pad | 770 → **823** | −15,3 → **−12,6** | 4,1 → **3,5** | −7,0 → **−6,6** | −3,4 → **−1,1** | 4,0 → **2,9** | 2,58 → **3,09** · 1,06 → **1,07** |
| retro_techno | lead | 1183 → **1817** | −6,8 → **−6,0** | 0,9 → **−4,5** | −4,2 → **−3,3** | −2,7 → **−0,4** | 2,9 → **2,5** | 2,77 → **2,17** · 1,06 → **1,06** |
| riddim | lead | 53 → **53** | −13,4 → **−13,4** | 29,3 → **29,3** | −25,0 → **−25,0** | −200,0 → **−200,0** | 16,5 → **16,5** | – → **–** · – → **–** |
| speed_garage | lead | 580 → **591** | −29,0 → **−29,0** | 7,8 → **7,4** | −10,4 → **−10,3** | −3,1 → **−3,0** | 2,5 → **2,8** | 2,85 → **2,86** · 0,99 → **1,00** |
| speed_garage | pad | 667 → **703** | −16,0 → **−14,1** | 5,2 → **4,8** | −6,8 → **−6,6** | −0,4 → **−0,4** | 6,0 → **6,0** | 2,92 → **3,28** · 1,06 → **1,10** |
| synthwave | lead | 1704 → **1656** | −8,5 → **−8,8** | −8,8 → **−9,2** | −3,0 → **−2,9** | −1,9 → **−1,2** | 1,5 → **1,8** | 5,62 → **5,57** · 0,92 → **0,94** |
| synthwave | pad | 560 → **663** | −15,0 → **−12,9** | 8,1 → **6,6** | −8,7 → **−7,5** | −1,2 → **1,1** | 7,4 → **7,2** | 2,56 → **2,70** · 1,13 → **1,09** |
| tech_house | lead | 532 → **538** | −23,9 → **−23,9** | 16,6 → **16,5** | −18,3 → **−18,3** | −3,7 → **−3,7** | 1,9 → **1,6** | 3,75 → **3,72** · 0,98 → **0,98** |
| tech_house | pad | 1103 → **1174** | −12,5 → **−11,1** | −2,6 → **−2,9** | −4,3 → **−4,2** | 0,6 → **0,6** | 4,8 → **4,7** | 1,00 → **1,41** · 1,05 → **1,11** |
| techno | lead | 1056 → **1054** | −14,0 → **−14,1** | −1,5 → **−1,6** | −3,8 → **−3,8** | −2,9 → **−3,0** | 2,3 → **2,1** | 4,90 → **4,95** · 1,00 → **0,98** |
| techno | pad | 440 → **456** | −11,7 → **−11,9** | 11,3 → **10,8** | −9,2 → **−8,8** | −1,0 → **−1,0** | 4,5 → **4,5** | 1,78 → **1,84** · 1,03 → **1,09** |
| trance | counter | 509 → **585** | −8,4 → **−9,1** | 10,2 → **9,5** | −8,0 → **−7,8** | −7,2 → **−5,5** | 3,1 → **2,5** | – → **–** · 0,89 → **0,80** |
| trance | lead | 1165 → **1380** | −4,3 → **−5,3** | 4,4 → **1,6** | −7,9 → **−6,1** | −5,3 → **−4,8** | 1,2 → **1,4** | 4,61 → **4,23** · 1,01 → **1,01** |
| trance | pad | 697 → **868** | −5,7 → **−6,1** | 8,7 → **6,0** | −8,5 → **−7,6** | −4,0 → **−4,4** | 5,7 → **2,9** | 0,77 → **0,84** · 0,98 → **1,00** |
| trap | counter | 962 → **987** | −18,7 → **−18,5** | 3,4 → **3,1** | −5,8 → **−5,6** | −2,6 → **−1,6** | −5,2 → **−4,5** | – → **–** · 1,00 → **0,98** |
| trap | lead | 2042 → **2051** | −12,3 → **−12,3** | −42,2 → **−42,6** | −0,9 → **−0,8** | −2,0 → **−0,8** | 0,8 → **1,0** | 8,19 → **8,19** · 1,18 → **1,18** |
| trap | pad | 603 → **570** | −9,7 → **−9,7** | 7,3 → **7,7** | −5,4 → **−5,4** | −2,5 → **−0,5** | 3,1 → **5,0** | 2,11 → **2,40** · 1,05 → **1,11** |
| uk_garage | lead | 659 → **660** | −22,8 → **−22,6** | 10,7 → **10,7** | −14,1 → **−14,2** | −3,6 → **−3,6** | 1,1 → **0,9** | – → **–** · 1,00 → **0,99** |
| uk_garage | pad | 2902 → **2734** | −4,7 → **−5,3** | −25,0 → **−22,8** | −3,8 → **−3,3** | 2,3 → **2,6** | 4,0 → **3,3** | 1,45 → **1,25** · 1,20 → **1,16** |

## 5. Les sons seuls (sonde)

Non-harmoniques et repliement d'une note tenue (suréchantillonnée) ; vélocité : centroïde des 250 premières ms à 1,0 sur 0,4 ; centroïde / f0 à Do3, Do5, Do7 ; corrélation de deux attaques de la même note ; attaque (crête des 20 premières ms contre 100–300 ms).

| Son | Non-harm. Do6 (dB) | Do7 | Vélocité (centroïde 1,0 / 0,4) | Centroïde / f0 Do3 / Do5 / Do7 | Même note rejouée (corr.) | Attaque (dB) |
|---|---|---|---|---|---|---|
| Lead | −53,5 → **−65,7** | −49,2 → **−62,7** | 1,00 → **1,12** | 3,06/2,43/1,49 → **3,05/2,50/1,58** | 0,63 → **0,63** | 11,4 → **10,8** |
| Lead carré | −59,4 → **−67,8** | −57,8 → **−66,1** | 1,00 → **1,13** | 2,58/2,05/1,31 → **2,57/2,11/1,36** | 0,67 → **0,69** | 8,9 → **9,1** |
| Lead acide | −0,7 → **−0,7** | −0,0 → **−0,0** | 1,00 → **1,00** | 1,00/1,00/0,06 → **1,00/1,00/0,06** | 0,20 → **0,20** | 30,4 → **30,4** |
| Supersaw | −69,3 → **−73,8** | −64,4 → **−63,2** | 1,00 → **0,96** | 2,83/2,11/1,54 → **4,76/3,14/1,80** | −0,10 → **0,10** | 1,3 → **10,9** |
| Lead trance | −55,5 → **−69,8** | −50,6 → **−60,4** | 1,00 → **1,07** | 2,39/3,61/1,58 → **6,99/2,91/2,04** | 0,15 → **−0,03** | 11,6 → **9,6** |
| Hoover | −44,6 → **−42,6** | −38,7 → **−42,8** | 1,00 → **1,00** | 2,40/1,99/1,20 → **4,94/3,42/2,22** | 0,22 → **0,17** | 7,9 → **15,4** |
| Nappe | −41,2 → **−46,6** | −36,9 → **−45,3** | 1,00 → **1,00** | 4,67/2,40/1,39 → **4,47/2,53/1,78** | 0,25 → **0,25** | −0,7 → **−0,7** |
| Nappe chaude | −47,2 → **−54,6** | −36,5 → **−43,6** | 1,00 → **1,00** | 2,84/1,49/1,01 → **2,70/1,58/1,19** | −0,15 → **−0,15** | −11,8 → **−11,8** |
| Accords future bass | −37,5 → **−52,4** | −33,7 → **−47,3** | 1,00 → **1,25** | 3,04/2,46/1,36 → **5,60/3,12/2,22** | 0,49 → **0,28** | 5,0 → **12,7** |
| Cordes | −41,2 → **−46,6** | −37,0 → **−45,5** | 1,00 → **1,00** | 4,63/2,39/1,35 → **4,42/2,53/1,74** | 0,09 → **0,12** | 6,2 → **6,2** |
| Pluck | −56,3 → **−49,6** | −38,5 → **−42,9** | 1,00 → **1,49** | 2,07/1,68/1,00 → **1,73/2,72/1,97** | 0,07 → **0,40** | 13,3 → **12,6** |
| Pluck future | −31,1 → **−33,5** | −33,6 → **−36,9** | 1,00 → **1,56** | 1,79/1,03/1,01 → **1,41/1,54/1,57** | −0,08 → **0,15** | 13,8 → **14,3** |
| Arpège | −13,1 → **−14,4** | −6,0 → **−16,5** | 1,00 → **1,21** | 1,37/1,02/0,72 → **1,37/1,58/1,16** | 0,21 → **0,23** | 26,5 → **28,3** |
| Cloche | −71,2 → **−92,0** | −41,0 → **−78,5** | 1,00 → **0,99** | 1,36/1,37/1,39 → **1,36/1,37/1,36** | −0,17 → **−0,17** | 11,9 → **12,1** |
| Orgue | −41,4 → **−98,2** | −25,2 → **−94,5** | 1,00 → **1,00** | 2,80/1,87/1,07 → **2,92/1,95/1,09** | 0,26 → **0,25** | 5,1 → **6,0** |
| Piano électrique | −54,2 → **−86,7** | −33,7 → **−76,0** | 1,00 → **1,16** | 1,58/1,31/1,01 → **1,60/1,44/1,26** | 0,08 → **0,08** | 18,5 → **19,6** |

## 6. Les tests du moteur (`leads2_tests.inc`)

| Test | Mesure |
|---|---|
| suivi de clavier | centroïde / f0 de Do3 à Do7 : ×1,37 → **×1,05** ; Do7 30,6 dB sous Do3 sans, 1,6 dB avec |
| vélocité → filtre | centroïde 1,0 / 0,4 : 1,00 → **1,43** (coupure), **2,52** (enveloppe) |
| enveloppe de hauteur | 873 Hz sur 1–5 ms pour un Do5 (523 Hz), à 0,5 cent après 50 ms |
| clic | au-dessus de 2 kHz : **+39,6 dB** sur ses 5 premières ms contre 50–100 ms |
| vibrato retardé | 0,00 cent sur 0–150 ms, ±4,6 ensuite |
| cycle décalé | mesure 1 / mesure 3 : 0,72 → **1,39** |
| unisson, phases | deux attaques : corrélation 1,00 → **−0,05** ; deux exports identiques |
| unisson, rapports | 7 pics sur 7 à 2 cents des rapports de Szabo |
| unisson, battement | auto-corrélation de l'enveloppe à 0,3–5 s : 0,98 → **0,20** |
| ladder dense | 7 scies à Do6 : −35,9 → **−82,6 dB** de non-harmoniques |
| dérive | ≤ 2,9 cents sur 60 s, une copie bouge de 5,8 cents |
| chorus stéréo | côté d'une entrée large : −4,4 dB (mono) → **+0,0 dB** |
| étalement | plus grande marche du côté au bord d'un bloc 0,033 (ailleurs 0,035) ; au repos exactement 0 |
| métronome | fader −12 dB : clic −12,0 dB en version 2 et en version 4 |
| temps réel | tout allumé : 0 allocation dans le rendu |

## 7. Le direct, style par style

| Style | Intro direct − export (LU) | Drop direct − export (LU) |
|---|---|---|
| acid_house | 0,26 → **0,26** | 0,06 → **0,06** |
| acid_techno | −0,59 → **−0,59** | −0,09 → **−0,09** |
| acidcore | −0,02 → **−0,02** | 0,01 → **0,01** |
| afro_house | 0,40 → **0,35** | 0,08 → **0,08** |
| afrobeats | 0,75 → **0,83** | 0,29 → **0,28** |
| amapiano | −0,40 → **−0,42** | 0,03 → **0,02** |
| boom_bap | 0,55 → **0,52** | 0,22 → **0,21** |
| deep_house | 0,35 → **0,41** | 0,09 → **0,11** |
| detroit | −0,17 → **−0,16** | −0,14 → **−0,13** |
| disco | −0,07 → **−0,04** | 0,02 → **0,03** |
| dnb | −0,84 → **−0,79** | 0,08 → **0,10** |
| drill | −0,11 → **−0,09** | 0,12 → **0,15** |
| dubstep | −0,92 → **−0,94** | 0,16 → **0,16** |
| gabber | 0,11 → **0,23** | 0,09 → **0,08** |
| grime | −0,98 → **−0,99** | −0,64 → **−0,64** |
| hard_techno | 0,30 → **0,24** | −0,09 → **−0,09** |
| house | 0,22 → **0,24** | −0,05 → **−0,05** |
| industrial_techno | −0,08 → **−0,08** | −0,03 → **−0,03** |
| jersey_club | −1,30 → **−1,27** | −0,32 → **−0,28** |
| jump_up | −0,62 → **−0,60** | −0,02 → **−0,01** |
| jungle | −0,33 → **−0,31** | −0,04 → **−0,04** |
| liquid_dnb | −0,72 → **−0,67** | −0,03 → **0,03** |
| lofi | 0,31 → **0,20** | 0,44 → **0,33** |
| minimal | 0,09 → **0,04** | −0,02 → **−0,04** |
| neurofunk | −0,38 → **−0,39** | 0,20 → **0,19** |
| phonk | 0,01 → **0,05** | 0,04 → **0,06** |
| pop | 0,50 → **0,36** | 0,17 → **0,13** |
| reggaeton | 0,01 → **−0,01** | −0,02 → **−0,07** |
| retro_techno | 0,28 → **0,32** | 0,07 → **0,11** |
| riddim | −1,56 → **−1,56** | −0,59 → **−0,59** |
| speed_garage | 0,13 → **0,13** | 0,01 → **0,02** |
| synthwave | 0,63 → **0,53** | 0,20 → **0,11** |
| tech_house | 0,36 → **0,36** | −0,13 → **−0,13** |
| techno | 0,12 → **0,06** | −0,09 → **−0,06** |
| trance | 0,33 → **0,74** | 0,06 → **0,08** |
| trap | −0,31 → **−0,24** | 0,20 → **0,21** |
| uk_garage | −0,93 → **−0,89** | −0,07 → **−0,04** |

## 8. Extraits à écouter

30 s depuis 4 mesures avant le premier Drop (trap : Refrain), AAC 192 kbit/s, true peak décodé ≤ −1 dBTP (méthode de `audit-35/tools/extracts.py`). Fichiers : scratchpad `final/<style>-final.m4a` (hors du dépôt), à comparer à `ab-leads/<style>-apres.m4a` (lot 1) et `audit-35/<style>-avant.m4a`.

| Style | Départ (s) | Partie visée à (s de l'extrait) | Gain (dB) | LUFS-I / TP |
|---|---|---|---|---|
| techno | 109,1 | 7,3 (Drop) | −3,0 | −12,2 / −1,4 |
| house | 55,1 | 7,9 (Drop) | −1,3 | −10,8 / −1,1 |
| trap | 6,9 | 6,9 (Refrain) | −1,1 | −10,6 / −1,4 |
| jump_up | 49,7 | 5,5 (Drop) | −3,7 | −13,6 / −3,2 |
| acid_techno | 104,3 | 7,0 (Drop) | −2,0 | −10,4 / −1,4 |
| dubstep | 61,7 | 6,9 (Drop) | −1,2 | −10,9 / −1,3 |

## 9. Ce qui n'est pas atteint

- Vélocité des leads : 1,07 à 1,13 (cible 1,15) ; le Supersaw, ouvert à 10 kHz, ne s'éclaircit pas (0,96 : seuls le niveau et le clic changent).
- Mouvement des nappes sur 8 mesures : médiane 2,12 → 2,27 dB (12/30 au-dessus de 2,5) avec une courbe à ±0,6 octave.
- Centroïde des leads entre 1,5 et 3 kHz : 10/34 (8 avant) ; l'attaque des leads dans les morceaux ne bouge pas (beaucoup sont des voix Plaits ; à la sonde le Supersaw passe de 1,3 à 10,9 dB).
- Le Pluck suivi à 100 % ouvrait son SVF très haut sur ses notes aiguës (non-harmoniques à Do6 −56 → −50 dB) : après la relecture, suivi à 70 % : −64 dB (centroïde / f0 1,96 / 2,47 / 1,78). Les mesures de mix et de voix ci-dessus ont été prises avec 100 %.
- Lot D : pas de couche médium sous un stab reese ou wobble, la paire FM n'est dans aucune recette (le stab neuro reste celui de Plaits), pas de piano VCSL.
- Direct : riddim (−1,56) et jersey club (−1,27) gardent leur Intro à plus de 1 LU de l'export (gain estimé sur le Refrain seul : jersey 5,17 dB contre 6,22 à l'export).
