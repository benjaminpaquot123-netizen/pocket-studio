# Banc de mesure du son : après le chantier leads, lot 1 (recettes et vraies voix FM)

Point de départ : « vieux synthé amateur » (Benjamin, la mélodie DnB) et l'audit 35 des pros (lead techno à 296 Hz, brillance −19,9 dB, boue +12,6 ; présence au téléphone du lead tech house −18,9 dB ; réponse jump up à 252 Hz, boue +20,3 ; neurofunk à −38,4 dB de brillance). Diagnostic et plan : `docs/hits/leads-pros.md`. Ce lot fait le lot A (« recettes seules ») et la partie simple du lot 2 (« vraies voix FM ») ; le moteur ne change que pour la vélocité des voix FM.

Ce qui change (morceaux de l'assistant ; un projet enregistré avant garde son son, octet pour octet) :
- **Les sons de la bibliothèque** (leads, nappes, plucks, claviers) : plus de sub carré sous eux ; le filtre SVF dès qu'il y a de l'unisson ou deux oscillateurs désaccordés ; coupures ouvertes par rôle (Supersaw 10 kHz, lead 4 kHz, nappe 2,7 kHz, pluck 600 Hz + enveloppe) ; relâchement de 100 à 400 ms ; vibrato à 5 Hz et ±6 à 8 cents (le piano sans vibrato) ; chaque son garde sa sonie (gain recalé, à 0,1 dB près).
- **Les morceaux (WideMix, version de son 3)** : plus de doublure à l'octave −1 quand il y a une piste « Sub » (et jamais sous un pluck, un clavier ou une cloche ; 0,15 ailleurs au lieu de 0,3) ; le cycle de filtre de 8 mesures **ouvert au début de chaque phrase** (une courbe de la coupure, ±0,5 octave sur le lead, ±0,4 sur la nappe) ; la coupure qui suit l'octave de la partie (suivi de clavier fait par la recette) ; un lead ou un contre-chant sous Do5 coupé sous 250 Hz avec +2 dB à 3 kHz, une nappe sous Do4 coupée sous 160 Hz ; un lead à unisson sans le chorus qui le repliait en mono ; envoi de réverbe des leads 0,2 ; une nappe ou un clavier Plaits (mono) élargi par un trémolo stéréo et un chorus ; leads −1 dB et nappes −1,5 dB (plus brillants, ils paraissaient plus forts : sans cela le Break remontait vers le Drop).
- **Les vraies voix FM de Plaits** : « Piano FM » joue enfin « E.PIANO 1 » (il jouait « BASS 1 ») ; « Rhodes FM » (WINTRHODES), « Cloche FM » (BELL C), « Vibraphone FM » (VIBE 1), « Stab métallique » (FM 2 opérateurs) ; en version 3 la vélocité règle leur volume (0,3 + 0,7 v : 4,8 dB entre 0,4 et 1,0, au lieu de 0,2).
- **Par style** : jump up répond avec un hoover à l'octave 4 ; liquid : accords « Rhodes FM » en auto-pan à la croche, une nappe derrière, contre-chant « Cloche FM » ; neurofunk : « Stab métallique » à l'octave 4 ; dubstep, trap, minimal, detroit : « Cloche FM » ; lead techno et rétro techno à l'octave 4, « Lead filtré » plus ouvert ; claviers de deep house, lo-fi, drill, detroit, uk garage, amapiano sur les pianos FM (le boom bap y garde son lead, ses accords et ceux de l'afrobeats restent sur le piano électrique du synthé, réglé : un piano FM, mono, les rendait étroits).

## Méthode

Exactement celle de l'« avant » (`docs/audit-35/mesures.md`), sur le même ordinateur :
- **Enregistrement** : `SongBenchExport` (export), `LiveSongRecorder` (le direct, 150 s) et `PreviewLevelBench` en un seul passage, avec deux lignes de plus dans les enregistrements (`META pad`, `META counter` : la voie de la nappe et du contre-chant). L'« avant » a été réenregistré sur le code de départ (e0ff315) : ses `.pscmd` sont ceux de l'audit 35 à la ligne près (hors `META`), et rejoués par le moteur de ce lot ils donnent **les mêmes WAV octet pour octet** (empreintes SHA-256 de l'audit, vérifié sur amapiano, house et liquid_dnb, voix FM en version 2 comprises).
- **Voix seules** : chaque lead, nappe et contre-chant rendu seul (`ps_render --solo <voie>`, chaîne master en place, comme `audit-35/tools/lead.py`), mesuré sur le premier Drop (ou Refrain), mesures où la voix joue : centroïde, brillance (4–10 kHz contre 1–4 kHz), boue (150–500 Hz contre 1–4 kHz), présence au haut-parleur de téléphone (part de 1–4 kHz après un passe-haut d'ordre 4 à 300 Hz), côté/centre ; sur tout le morceau : mouvement sur 8 mesures (écart-type de la forme du spectre sur 18 bandes) et centroïde de la mesure 1 de la phrase contre sa médiane.
- **Sons seuls** (sonde dans le vrai `ps::Synth` / `PlaitsSynth`) : non-harmoniques et repliement d'une note tenue à Do6 et Do7 (suréchantillonné, vibrato, balayage et cycle coupés), vélocité, centroïde / f0 de Do3 à Do7, attaque.
- **Mix** : `scripts/song-bench.sh --no-record --premaster --m4a --before audit-35/bench/bench.json` sur ces enregistrements ; haut-parleur de téléphone (`audit-35/tools/phone.py`, 8 premières mesures du premier Drop) ; `scripts/live-bench.sh` (37 styles, 90 s, puis les 9 styles longs à 150 s) ; et quelques voix seules **en direct** (`ps_live`, la voie en solo, master coupé, 150 s, méthode de `leads-pros.md` § 1).
- **Extraits** : même méthode que l'« avant » (`audit-35/tools/extracts.py`) : 30 s depuis 4 mesures avant le premier Drop (trap : Refrain), AAC 192 kbit/s, true peak décodé ≤ −1 dBTP, plus la version « haut-parleur de téléphone » au même gain.

## En bref (médianes des styles où la voix joue au premier Drop, avant → après)

| Voix | Mesure | Avant | Après | Cible (`leads-pros.md` § 8) | Styles dans la cible |
|---|---|---|---|---|---|
| Lead | Centroïde (Hz) | 612 | **1166** | 1 500 à 3 000 | 1/32 → **7/32** |
| Lead | Brillance 4–10 k / 1–4 k (dB) | −18,4 | **−8,4** | −8 à −14 (± 6 de la référence : ≥ −20) | 19/32 → **25/32** |
| Lead | Boue 150–500 / 1–4 k (dB) | 6,6 | **−2,8** | ≤ +4 | 11/32 → **22/32** |
| Lead | Présence téléphone 1–4 kHz (dB) | −8,3 | **−4,8** | ≥ −8 | 15/32 → **26/32** |
| Lead | Mouvement sur 8 mesures (dB) | 4,78 | **4,43** | ≥ 2,5 | 25/32 → **26/32** |
| Lead | Centroïde mesure 1 / médiane de la phrase | 0,90 | **1,01** | ≥ 0,9 | 14/32 → **27/32** |
| Nappe / accords | Centroïde (Hz) | 442 | **692** | – | – |
| Nappe / accords | Brillance (dB) | −24,6 | **−10,9** | −12 à −20 (≥ −20) | 12/30 → **30/30** |
| Nappe / accords | Boue (dB) | 12,1 | **5,8** | ≤ +8 | 8/30 → **22/30** |
| Nappe / accords | Présence téléphone (dB) | −11,0 | **−6,9** | ≥ −12 | 18/30 → **30/30** |
| Nappe / accords | Mouvement sur 8 mesures (dB) | 2,28 | **2,10** | ≥ 2,5 | 10/30 → **10/30** |
| Nappe / accords | Mesure 1 / médiane | 0,93 | **1,04** | ≥ 0,9 | 20/30 → **30/30** |
| Contre-chant | Centroïde (Hz) | 524 | **820** | 1 500 à 3 000 | 0/6 → **1/6** |
| Contre-chant | Brillance 4–10 k / 1–4 k (dB) | −18,6 | **−14,0** | −8 à −14 (± 6 de la référence : ≥ −20) | 4/6 → **4/6** |
| Contre-chant | Boue 150–500 / 1–4 k (dB) | 11,0 | **3,4** | ≤ +4 | 1/6 → **4/6** |
| Contre-chant | Présence téléphone 1–4 kHz (dB) | −10,4 | **−5,8** | ≥ −8 | 1/6 → **5/6** |
| Contre-chant | Mouvement sur 8 mesures (dB) | 1,73 | **0,76** | ≥ 2,5 | 0/6 → **0/6** |
| Contre-chant | Centroïde mesure 1 / médiane de la phrase | 1,00 | **1,00** | ≥ 0,9 | 1/6 → **1/6** |

Bloquants du banc (aucun ne doit reculer) : régressions relevées : **techno (Drop − Groove)**. True peak ≤ −1 dBTP : 37/37 ; M4A décodé ≤ −1 dBTP : 37/37 ; Drop − Break ≥ +4 là où il l'était : 12/12 (et 16/37 au total) ; Drop − fin de Montée ≥ +1 : 25/25.

Ce qu'on en lit :
- **Les voix ne sont plus sombres ni boueuses.** Le lead a doublé son centre de gravité (médiane 612 → 1 170 Hz) et gagné 10 dB de brillance ; sa boue passe de +6,6 à −2,8 dB, sa présence au téléphone de −8,3 à −4,8 dB. Les cas cités par les pros : lead techno 296 → 1 100 Hz (brillance −19,9 → −13,9, boue +12,6 → −2,6, présence −8,9 → −3,8) ; réponse jump up 252 → 1 141 Hz (boue +20,3 → +4,2, présence −18,2 → −7,5) ; neurofunk −38,4 → +3,2 dB de brillance (le stab FM est métallique par nature : au-dessus de la fourchette, voulu) ; liquid 618 → 1 902 Hz. Les nappes : brillance −24,6 → −10,9 dB, boue +12,1 → +5,8, présence 30 styles sur 30 dans la cible (18 avant).
- **Le cycle de 8 mesures s'ouvre au début de la phrase** : centroïde de la mesure 1 sur la médiane de la phrase 0,90 → 1,01 (lead), 0,93 → 1,04 (nappe) ; le mouvement reste au-dessus de 2,5 dB sur le lead (4,8 → 4,4 dB : ±0,5 octave au lieu de ±1,2).
- **Le Supersaw est propre** : −69 dB de non-harmoniques à Do6 (−33 avant). Les autres sons gagnent 7 à 35 dB mais restent entre −39 et −56 dB (cible −60) : le reste viendra du moteur (lot C).
- **Le mix ne recule pas sur les bloquants** : true peak 37/37, M4A décodé 37/37, sonie dans la cible ou plafonnée 37/37, Drop − fin de Montée ≥ +1 sur les 25 morceaux qui en ont une, Drop − Break ≥ +4 gardé partout où il l'était (12 styles à Break sur 12 ; dnb, jungle et liquid y repassent grâce au −1 / −1,5 dB des leads et des nappes). Au téléphone, la présence 1–4 kHz du Drop monte dans 23 styles (dnb −10,0 → −7,3, dubstep −11,7 → −8,1, liquid −8,1 → −4,8, afrobeats −10,0 → −8,5), la perte baisse dans 20.
- **Ce qui recule un peu** : Drop − Groove (ou Montée) de 0,2 à 0,3 LU en techno (+1,76 → +1,45), neurofunk (+5,99 → +5,72), pop (+2,27 → +2,03) et minimal (+1,92 → +1,69), la médiane montant de +1,83 à +1,87 ; essayé : ±1 à 2 dB sur le lead ou la nappe de ces morceaux ne le change que de 0,05 LU (le Groove est plus brillant, et la chaîne master travaille au plafond sur le Drop). Le côté/centre du Drop perd 0,6 dB en médiane (−18,3 → −18,9) : les pianos FM sont mono (le piano du synthé ouvre ses deux oscillateurs à gauche et à droite), le trémolo stéréo et le chorus ne compensent qu'en partie. En boom bap et afrobeats, où l'écart était le plus grand (−19,2 → −27,2 et −16,4 → −23,3 dB avec le piano FM), les accords sont revenus sur le piano électrique du synthé, réglé : −20,4 et −16,8 dB, et leur nappe garde une bonne partie du gain (présence −13,3 → −8,7 en boom bap, −13,4 → −9,5 en afrobeats ; boue +16,3 → +11,2 et +13,9 → +9,8). La présence au téléphone recule de 0,3 à 0,4 dB en jungle et synthwave.
- Les voix seules ont été mesurées sur l'enregistrement d'avant le dernier réglage de niveau (−1 / −1,5 dB) : le rendu seul est ramené à la même sonie, les rapports de bandes bougent de 0,2 dB au plus (vérifié sur la nappe dnb).

## Le mix

| Mesure (médiane, 37 styles) | Avant (audit 35) | Après | Cible |
|---|---|---|---|
| Partie la plus forte (LUFS) | −8,5 | **−8,5** | cible ±1 |
| True peak export (dBTP) | −1,18 | **−1,18** | ≤ −1,0 |
| M4A décodé (dBTP) | −1,8 | **−2,0** | ≤ −1,0 |
| M4A, sonie (LUFS-I) | −12,9 | **−13,1** | – |
| Drop − Break (LU) | 4,0 | **4,3** | ≥ +4 |
| Drop − Groove (LU) | 1,8 | **1,8** | ne diminue pas |
| Drop − fin de Montée, le plus petit (LU) | 2,3 | **2,4** | ≥ +1 (bloquant) |
| Perte dans la chaîne (LU) | 3,0 | **3,0** | ≤ 3 |
| Perte au haut-parleur de téléphone (LU) | 4,1 | **4,1** | plus petit = mieux |
| Présence 1–4 kHz du Drop au téléphone (dB) | −6,5 | **−6,0** | – |
| 300–800 Hz au téléphone (dB) | −4,0 | **−4,5** | – |
| Côté/centre du Drop (dB) | −18,3 | **−18,9** | −15 à −9 |

| Style | Partie forte (LUFS) | True peak | M4A décodé | Drop − Break | Drop − Groove | Drop − Montée (min) | Perte tél. (LU) | Présence tél. | Côté/centre |
|---|---|---|---|---|---|---|---|---|---|
| acid_techno | −7,6 → −7,6 | −1,18 → −1,18 | −1,8 → −1,8 | +3,0 → +3,0 | +0,9 → +0,9 | +2,1 → +2,1 | 4,6 → 4,6 | −3,0 → −3,0 | −30,1 → −30,1 |
| acidcore | −7,9 → −7,9 | −1,16 → −1,16 | −1,0 → −1,0 | +0,5 → +0,5 | +0,7 → +0,7 | +2,3 → +2,3 | 3,9 → 3,9 | −6,3 → −6,3 | −35,8 → −35,8 |
| detroit | −8,0 → −8,0 | −1,18 → −1,17 | −2,6 → −4,1 | +4,5 → +5,5 | +0,7 → +0,9 | +2,2 → +2,2 | 3,1 → 3,5 | −2,4 → −2,3 | −20,2 → −22,1 |
| gabber | −7,9 → −7,8 | −1,19 → −1,19 | −1,3 → −2,5 | +2,6 → +3,3 | +4,8 → +4,9 | +2,1 → +1,8 | 3,4 → 3,4 | −7,3 → −7,5 | −14,3 → −15,4 |
| hard_techno | −8,4 → −8,5 | −1,19 → −1,19 | −1,4 → −2,3 | +2,2 → +2,3 | +2,6 → +2,8 | +2,4 → +2,9 | 2,4 → 2,3 | −6,0 → −6,1 | −12,0 → −12,8 |
| industrial_techno | −7,6 → −7,5 | −1,19 → −1,19 | −2,6 → −3,4 | +6,4 → +7,3 | +1,7 → +1,8 | +2,9 → +3,2 | 3,5 → 3,6 | −8,4 → −8,5 | −16,6 → −16,9 |
| minimal | −8,8 → −8,7 | −1,19 → −1,18 | −1,4 → −2,1 | +2,9 → +3,5 | +1,9 → +1,7 | +3,1 → +2,8 | 5,8 → 5,6 | −8,6 → −6,9 | −11,8 → −12,8 |
| retro_techno | −8,2 → −8,2 | −1,17 → −1,17 | −3,0 → −2,1 | +7,0 → +9,4 | +1,2 → +1,2 | +2,2 → +2,3 | 4,4 → 4,1 | −4,9 → −4,7 | −20,5 → −20,5 |
| techno | −8,6 → −8,3 | −1,17 → −1,17 | −1,8 → −1,6 | +4,0 → +4,2 | +1,8 → +1,4 | +3,0 → +2,4 | 5,0 → 4,8 | −6,0 → −6,0 | −14,1 → −14,9 |
| trance | −8,4 → −8,4 | −1,18 → −1,18 | −1,3 → −1,3 | +1,6 → +1,6 | +1,9 → +2,0 | +2,2 → +2,7 | 3,2 → 2,8 | −7,5 → −7,0 | −10,9 → −11,0 |
| acid_house | −8,1 → −8,1 | −1,17 → −1,17 | −2,2 → −2,2 | +2,3 → +2,3 | +0,5 → +0,5 | +1,8 → +1,8 | 3,6 → 3,6 | −2,7 → −2,7 | −24,3 → −24,3 |
| afro_house | −9,0 → −9,0 | −1,19 → −1,19 | −3,1 → −2,6 | +4,6 → +4,9 | +2,1 → +2,0 | +3,4 → +3,2 | 7,1 → 6,7 | −6,5 → −6,0 | −17,5 → −18,2 |
| amapiano | −9,0 → −9,0 | −1,19 → −1,19 | −1,5 → −2,2 | +10,4 → +10,6 | +5,0 → +5,0 | +6,1 → +6,0 | 7,1 → 6,4 | −4,8 → −4,5 | −19,7 → −21,5 |
| deep_house | −9,0 → −9,0 | −1,18 → −1,18 | −1,9 → −1,2 | +3,6 → +3,8 | +2,2 → +2,2 | +3,7 → +4,0 | 6,1 → 6,2 | −8,1 → −7,7 | −19,8 → −21,7 |
| disco | −9,0 → −9,0 | −1,18 → −1,19 | −1,6 → −1,8 | +5,2 → +6,7 | +1,8 → +1,9 | +2,5 → +2,6 | 4,1 → 4,1 | −9,6 → −8,4 | −17,0 → −19,4 |
| house | −8,4 → −8,5 | −1,18 → −1,17 | −2,3 → −4,3 | +3,9 → +6,7 | +0,6 → +0,7 | +2,3 → +3,4 | 4,8 → 5,5 | −7,8 → −6,3 | −23,3 → −22,4 |
| speed_garage | −8,2 → −8,2 | −1,18 → −1,18 | −1,9 → −3,1 | +6,1 → +6,2 | +2,0 → +2,2 | +2,4 → +2,3 | 5,2 → 5,1 | −7,0 → −7,1 | −20,9 → −20,3 |
| tech_house | −8,9 → −8,8 | −1,16 → −1,16 | −2,5 → −1,1 | +3,9 → +4,1 | +1,0 → +1,1 | +2,9 → +2,9 | 3,9 → 3,9 | −5,7 → −5,7 | −18,9 → −18,3 |
| uk_garage | −8,4 → −8,3 | −1,18 → −1,18 | −2,9 → −2,4 | – → – | +1,0 → +1,1 | – → – | 3,2 → 3,2 | −5,9 → −5,0 | −18,5 → −20,1 |
| dnb | −8,5 → −8,6 | −1,19 → −1,19 | −1,4 → −2,1 | +4,4 → +4,2 | +5,4 → +5,9 | +2,3 → +2,6 | 3,1 → 3,0 | −10,0 → −7,4 | −12,5 → −13,6 |
| dubstep | −8,8 → −8,7 | −1,19 → −1,18 | −1,3 → −2,4 | +3,5 → +4,3 | +5,2 → +5,5 | +2,3 → +2,0 | 2,8 → 2,8 | −11,7 → −8,3 | −11,3 → −12,5 |
| jump_up | −8,7 → −8,7 | −1,18 → −1,18 | −2,1 → −1,9 | +4,7 → +5,8 | +4,6 → +5,1 | +1,9 → +2,1 | 1,2 → 1,1 | −6,5 → −5,2 | −7,4 → −8,2 |
| jungle | −8,0 → −8,2 | −1,18 → −1,18 | −1,1 → −1,7 | +6,2 → +4,5 | +1,3 → +1,4 | +3,4 → +3,3 | 4,3 → 3,7 | −6,4 → −6,7 | −16,9 → −13,8 |
| liquid_dnb | −8,4 → −8,3 | −1,19 → −1,18 | −1,1 → −4,4 | +5,8 → +4,4 | +5,2 → +5,2 | +2,0 → +2,1 | 4,4 → 3,5 | −8,1 → −4,9 | −14,7 → −15,5 |
| neurofunk | −8,8 → −8,8 | −1,19 → −1,19 | −3,5 → −3,1 | +5,6 → +7,7 | +6,0 → +5,7 | +2,0 → +1,8 | 3,5 → 3,5 | −6,7 → −6,7 | −23,7 → −23,7 |
| riddim | −9,8 → −9,8 | −1,18 → −1,18 | −1,8 → −1,8 | +4,0 → +4,0 | +6,2 → +6,2 | +2,4 → +2,4 | 2,7 → 2,7 | −6,6 → −6,6 | −16,4 → −16,4 |
| boom_bap | −10,1 → −10,1 | −1,18 → −1,18 | −1,6 → −1,3 | – → – | +1,8 → +1,8 | – → – | 7,0 → 6,9 | −7,4 → −6,4 | −19,2 → −20,4 |
| drill | −8,6 → −8,6 | −1,19 → −1,19 | −1,9 → −2,0 | – → – | +2,0 → +2,0 | – → – | 6,5 → 6,3 | −5,9 → −5,1 | −21,0 → −21,2 |
| grime | −8,5 → −8,5 | −1,17 → −1,17 | −1,5 → −1,4 | – → – | +2,2 → +2,3 | – → – | 2,9 → 2,7 | −4,2 → −4,1 | −16,9 → −16,7 |
| jersey_club | −8,5 → −8,5 | −1,18 → −1,18 | −1,4 → −3,1 | – → – | +1,3 → +1,2 | – → – | 4,1 → 4,3 | −7,1 → −6,7 | −18,3 → −18,9 |
| lofi | −12,1 → −12,1 | −1,19 → −1,20 | −1,6 → −1,0 | – → – | +2,7 → +2,8 | – → – | 9,0 → 8,4 | −7,9 → −6,0 | −25,6 → −27,5 |
| phonk | −7,0 → −7,0 | −1,16 → −1,16 | −2,2 → −1,4 | – → – | +0,8 → +0,8 | – → – | 5,0 → 4,9 | −5,1 → −4,7 | −22,8 → −24,7 |
| trap | −8,5 → −8,5 | −1,18 → −1,19 | −1,9 → −1,8 | – → – | +1,5 → +1,8 | – → – | 5,7 → 5,2 | −4,1 → −2,7 | −19,5 → −21,7 |
| afrobeats | −9,2 → −9,2 | −1,13 → −1,13 | −1,1 → −1,3 | – → – | +0,9 → +0,9 | – → – | 6,1 → 6,5 | −10,0 → −8,5 | −16,4 → −16,8 |
| pop | −9,0 → −9,0 | −1,18 → −1,17 | −1,7 → −2,1 | – → – | +2,3 → +2,0 | – → – | 5,5 → 5,4 | −7,0 → −5,6 | −19,7 → −20,9 |
| reggaeton | −9,0 → −9,1 | −1,18 → −1,18 | −1,4 → −1,5 | – → – | +1,0 → +1,4 | – → – | 3,3 → 3,2 | −5,9 → −4,8 | −15,5 → −15,8 |
| synthwave | −8,1 → −8,1 | −1,18 → −1,18 | −2,4 → −1,2 | – → – | +0,9 → +0,9 | – → – | 3,7 → 4,2 | −4,4 → −4,8 | −15,4 → −16,3 |

## Chaque voix, style par style (premier Drop, piste seule)

Centroïde en Hz, autres colonnes en dB ; « mouv. » = mouvement sur 8 mesures (dB) · centroïde mesure 1 / médiane de la phrase.

| Style | Voix | Centroïde | Brillance | Boue | Présence tél. | Côté/centre | Mouv. · mesure 1 |
|---|---|---|---|---|---|---|---|
| detroit | lead | 584 → **1423** | −20,4 → **−9,9** | +5,4 → **−7,6** | −7,2 → **−2,9** | −7,5 → **−10,3** | 4,80 → **6,36** · 0,97 → **0,93** |
| detroit | contre-chant | 327 → **607** | −18,5 → **−28,2** | +12,7 → **+3,3** | −9,6 → **−5,0** | −9,1 → **−11,4** | – → **–** · – → **–** |
| detroit | nappe / accords | 1826 → **1826** | −13,1 → **−13,1** | −11,0 → **−11,0** | −0,8 → **−0,8** | −16,7 → **−16,7** | 0,39 → **0,39** · 1,00 → **1,00** |
| gabber | lead | 547 → **860** | −10,3 → **−4,4** | +9,3 → **+7,4** | −9,9 → **−9,4** | −8,8 → **−8,2** | 3,94 → **2,68** · 0,84 → **1,03** |
| gabber | nappe / accords | 481 → **672** | −16,0 → **−4,8** | +7,7 → **+7,9** | −7,3 → **−7,9** | −7,2 → **−6,8** | 3,25 → **2,36** · 1,00 → **1,12** |
| hard_techno | lead | 594 → **1051** | −12,8 → **−6,3** | +7,5 → **+4,8** | −8,5 → **−7,3** | −6,4 → **−5,2** | 5,84 → **3,32** · 0,89 → **0,98** |
| hard_techno | nappe / accords | 516 → **727** | −15,8 → **−4,5** | +7,2 → **+7,7** | −7,0 → **−7,7** | −7,1 → **−6,7** | 2,96 → **1,86** · 0,95 → **1,07** |
| industrial_techno | lead | 322 → **678** | −2,6 → **−4,2** | +17,2 → **+11,1** | −13,5 → **−9,7** | −7,2 → **−5,4** | 2,28 → **3,03** · 1,05 → **1,04** |
| industrial_techno | nappe / accords | 705 → **705** | −9,5 → **−9,5** | +6,5 → **+6,5** | −7,0 → **−7,0** | −11,5 → **−11,5** | 0,42 → **0,42** · 1,00 → **1,00** |
| minimal | lead | – | – | – | – | – | (la voie ne joue pas au premier Drop) |
| minimal | contre-chant | 588 → **679** | −18,6 → **−22,1** | +7,4 → **+5,1** | −9,0 → **−8,2** | −8,2 → **−7,8** | 1,73 → **0,76** · 1,00 → **1,00** |
| minimal | nappe / accords | 340 → **531** | −24,1 → **−9,9** | +14,0 → **+9,1** | −10,9 → **−7,6** | −5,0 → **−4,8** | 1,49 → **1,33** · 0,97 → **1,08** |
| retro_techno | lead | 455 → **1401** | −12,9 → **−6,8** | +6,2 → **−0,9** | −6,2 → **−3,9** | −8,8 → **−6,6** | 4,62 → **2,69** · 0,85 → **1,06** |
| techno | lead | 296 → **1100** | −19,9 → **−13,9** | +12,6 → **−2,6** | −8,9 → **−3,8** | −13,0 → **−11,1** | 5,86 → **4,61** · 1,05 → **0,98** |
| techno | nappe / accords | 362 → **426** | −26,8 → **−11,8** | +13,1 → **+11,9** | −10,3 → **−9,7** | −4,8 → **−5,0** | 1,94 → **1,88** · 0,97 → **1,03** |
| trance | lead | 608 → **1252** | −12,3 → **−4,2** | +7,2 → **+3,5** | −9,1 → **−7,7** | −6,3 → **−5,3** | 6,53 → **4,44** · 0,83 → **1,00** |
| trance | contre-chant | 378 → **596** | −17,1 → **−8,7** | +14,9 → **+8,3** | −12,8 → **−7,4** | −6,2 → **−7,7** | – → **–** · – → **–** |
| trance | nappe / accords | 488 → **679** | −14,6 → **−5,8** | +9,1 → **+8,9** | −8,4 → **−8,4** | −4,6 → **−4,6** | 1,58 → **0,81** · 0,94 → **0,98** |
| afro_house | lead | 2713 → **2713** | −7,6 → **−7,6** | −18,0 → **−18,0** | −1,3 → **−1,3** | −5,7 → **−5,7** | – → **–** · – → **–** |
| afro_house | nappe / accords | 238 → **496** | −34,7 → **−18,8** | +37,7 → **+8,5** | −31,5 → **−6,9** | −8,1 → **−7,3** | 1,85 → **2,10** · 0,87 → **1,07** |
| amapiano | lead | 845 → **1028** | −9,6 → **−11,6** | +1,8 → **−6,3** | −6,8 → **−3,5** | −10,2 → **−9,3** | – → **–** · – → **–** |
| amapiano | nappe / accords | 322 → **996** | −27,9 → **−9,5** | +16,0 → **+3,6** | −12,6 → **−5,6** | −7,5 → **−11,2** | 2,01 → **2,29** · 0,94 → **1,36** |
| deep_house | lead | 845 → **845** | −26,1 → **−26,1** | −3,0 → **−3,0** | −5,2 → **−5,2** | −5,9 → **−5,9** | – → **–** · – → **–** |
| deep_house | nappe / accords | 378 → **494** | −26,1 → **−18,4** | +15,0 → **+7,9** | −13,4 → **−7,8** | −7,2 → **−11,1** | 1,56 → **1,79** · 0,92 → **1,01** |
| disco | lead | 687 → **1782** | −22,8 → **−5,4** | +6,3 → **−3,3** | −8,8 → **−3,9** | −5,9 → **−5,6** | 8,43 → **6,19** · 0,67 → **1,19** |
| disco | nappe / accords | 433 → **988** | −25,6 → **−7,1** | +11,6 → **+4,4** | −11,1 → **−6,2** | −7,7 → **−8,0** | 2,48 → **2,13** · 0,84 → **0,98** |
| house | lead | 563 → **948** | −18,1 → **−13,1** | +7,1 → **+0,7** | −8,2 → **−5,4** | −7,1 → **−5,9** | 4,29 → **4,06** · 0,76 → **0,98** |
| house | nappe / accords | 608 → **665** | −5,9 → **−17,5** | +11,0 → **+5,2** | −11,1 → **−7,0** | −26,2 → **−17,6** | 3,44 → **4,89** · 0,90 → **0,93** |
| speed_garage | lead | 562 → **601** | −29,7 → **−28,9** | +8,6 → **+7,2** | −11,0 → **−10,2** | −10,7 → **−10,2** | 2,91 → **2,85** · 0,99 → **1,00** |
| speed_garage | nappe / accords | 638 → **660** | −14,7 → **−16,2** | +5,2 → **+5,2** | −6,4 → **−6,7** | −7,8 → **−7,1** | 2,34 → **3,10** · 0,93 → **1,05** |
| tech_house | lead | 525 → **546** | −23,8 → **−23,8** | +17,4 → **+16,0** | −18,9 → **−17,9** | −12,4 → **−12,7** | 3,27 → **3,10** · 0,98 → **0,99** |
| tech_house | nappe / accords | 961 → **1099** | −15,1 → **−12,4** | −0,6 → **−2,4** | −4,5 → **−4,4** | −7,3 → **−6,6** | 1,42 → **1,01** · 0,93 → **1,05** |
| uk_garage | lead | 646 → **646** | −23,0 → **−23,0** | +11,7 → **+11,7** | −15,0 → **−15,0** | −11,3 → **−11,1** | – → **–** · – → **–** |
| uk_garage | nappe / accords | 748 → **2596** | −25,0 → **−5,2** | +0,3 → **−25,0** | −10,9 → **−3,6** | −9,3 → **−13,3** | 1,79 → **1,54** · 1,06 → **1,19** |
| dnb | lead | 602 → **1119** | −11,8 → **−5,8** | +6,4 → **+2,7** | −8,4 → **−6,8** | −6,2 → **−4,7** | 5,88 → **3,57** · 0,94 → **1,01** |
| dnb | nappe / accords | 451 → **843** | −36,1 → **−8,9** | +17,9 → **+3,0** | −19,0 → **−6,9** | −5,9 → **−6,1** | 2,97 → **2,57** · 0,90 → **1,09** |
| dubstep | lead | 644 → **1543** | −17,4 → **−10,7** | +6,8 → **−34,3** | −9,3 → **−2,4** | −5,4 → **−5,8** | 3,19 → **4,33** · 1,00 → **1,03** |
| dubstep | nappe / accords | 426 → **767** | −36,4 → **−8,5** | +20,0 → **+5,9** | −20,0 → **−7,8** | −4,5 → **−4,6** | 2,14 → **1,78** · 0,85 → **1,05** |
| jump_up | lead | 252 → **1141** | −21,5 → **−3,6** | +20,3 → **+4,2** | −18,2 → **−7,5** | −10,4 → **−6,1** | 3,89 → **5,76** · 0,70 → **1,38** |
| jump_up | nappe / accords | 450 → **830** | −36,7 → **−9,5** | +17,2 → **+3,1** | −18,4 → **−6,8** | −2,7 → **−3,0** | 2,45 → **1,81** · 0,87 → **1,03** |
| jungle | lead | 537 → **1057** | −12,1 → **−4,4** | +8,2 → **+5,0** | −7,9 → **−6,8** | −7,4 → **−5,4** | 7,73 → **4,99** · 0,85 → **1,15** |
| jungle | nappe / accords | 344 → **640** | −33,5 → **−14,7** | +29,8 → **+6,7** | −27,9 → **−9,8** | −4,1 → **−2,9** | 2,80 → **2,09** · 0,87 → **1,06** |
| liquid_dnb | lead | 618 → **1902** | −19,6 → **−7,2** | +4,3 → **−8,5** | −7,8 → **−3,1** | −8,0 → **−7,5** | 5,53 → **6,54** · 0,82 → **1,03** |
| liquid_dnb | contre-chant | 651 → **1561** | −20,7 → **−9,4** | +0,4 → **−40,4** | −6,2 → **−0,5** | −7,4 → **−10,6** | – → **–** · – → **–** |
| liquid_dnb | nappe / accords | 427 → **1082** | −23,7 → **−10,1** | +16,5 → **+2,6** | −15,9 → **−5,1** | −5,4 → **−11,0** | 1,74 → **1,50** · 0,87 → **0,96** |
| neurofunk | lead | 409 → **3405** | −38,4 → **3,2** | +12,2 → **−3,6** | −11,5 → **−6,9** | −5,5 → **−7,6** | 4,78 → **4,55** · 0,81 → **0,95** |
| riddim | lead | – | – | – | – | – | (la voie ne joue pas au premier Drop) |
| boom_bap | lead | 837 → **1191** | −25,3 → **−10,7** | −2,7 → **−33,0** | −6,7 → **−4,4** | −7,3 → **−10,7** | – → **–** · – → **–** |
| boom_bap | nappe / accords | 297 → **394** | −29,1 → **−18,8** | +16,3 → **+11,2** | −13,3 → **−8,7** | −8,8 → **−8,1** | 2,53 → **3,88** · 0,86 → **0,91** |
| drill | lead | 897 → **1218** | −27,5 → **−13,2** | −2,8 → **−7,6** | −4,6 → **−3,6** | −6,7 → **−8,4** | 6,12 → **5,31** · 0,69 → **0,92** |
| drill | nappe / accords | 462 → **783** | −31,4 → **−8,6** | +9,2 → **+5,8** | −8,5 → **−5,8** | −8,5 → **−8,3** | 2,87 → **3,38** · 0,82 → **0,93** |
| grime | lead | 3174 → **3164** | −3,3 → **−3,2** | −13,4 → **−13,4** | −2,8 → **−2,8** | −9,0 → **−8,6** | 8,37 → **8,35** · 1,49 → **1,50** |
| grime | nappe / accords | 453 → **575** | −12,3 → **−8,0** | +9,6 → **+8,9** | −7,5 → **−7,4** | −10,1 → **−9,7** | 4,51 → **5,64** · 0,90 → **1,07** |
| jersey_club | lead | 615 → **655** | −25,9 → **−25,4** | +9,3 → **+7,4** | −12,8 → **−11,9** | −8,6 → **−8,5** | 4,41 → **4,43** · 1,03 → **1,00** |
| lofi | lead | 501 → **793** | −23,6 → **−20,4** | +13,7 → **+4,2** | −14,1 → **−6,4** | −10,3 → **−11,3** | 8,49 → **7,31** · 1,16 → **0,99** |
| lofi | nappe / accords | 370 → **765** | −20,0 → **−14,5** | +15,0 → **+3,0** | −13,4 → **−5,0** | −11,1 → **−15,1** | 2,02 → **2,62** · 1,23 → **0,99** |
| phonk | lead | 892 → **934** | −23,8 → **−23,6** | −7,2 → **−8,6** | −3,2 → **−2,8** | −9,5 → **−9,4** | 2,35 → **2,40** · 1,02 → **1,02** |
| phonk | nappe / accords | 293 → **674** | −36,5 → **−12,9** | +22,2 → **+5,4** | −18,6 → **−4,8** | −8,9 → **−8,6** | 2,30 → **2,02** · 0,90 → **1,04** |
| trap | lead | 1278 → **2030** | −18,6 → **−12,3** | −10,0 → **−42,2** | −2,1 → **−0,9** | −5,4 → **−8,8** | 4,85 → **7,88** · 1,04 → **1,17** |
| trap | contre-chant | 547 → **960** | −39,4 → **−18,6** | +16,6 → **+3,5** | −17,2 → **−5,8** | −10,8 → **−9,6** | – → **–** · – → **–** |
| trap | nappe / accords | 384 → **705** | −31,0 → **−10,0** | +9,7 → **+5,7** | −7,6 → **−5,0** | −9,0 → **−8,7** | 2,26 → **2,10** · 0,97 → **1,07** |
| afrobeats | lead | 3303 → **3303** | −3,6 → **−3,6** | −25,1 → **−25,1** | −1,7 → **−1,7** | −9,9 → **−9,9** | 3,80 → **3,80** · 0,96 → **0,96** |
| afrobeats | contre-chant | 500 → **1178** | −17,9 → **−8,5** | +9,3 → **−24,5** | −11,3 → **−5,7** | −8,1 → **−7,2** | – → **–** · – → **–** |
| afrobeats | nappe / accords | 423 → **512** | −25,8 → **−15,6** | +13,9 → **+9,8** | −13,4 → **−9,5** | −7,0 → **−6,2** | 2,71 → **3,42** · 0,86 → **0,91** |
| pop | lead | 507 → **1395** | −16,3 → **−6,2** | +11,9 → **−2,1** | −12,2 → **−6,1** | −8,1 → **−8,4** | 4,11 → **4,35** · 0,86 → **1,07** |
| pop | nappe / accords | 404 → **563** | −9,5 → **−19,9** | +12,7 → **+6,7** | −9,3 → **−6,7** | −13,6 → **−13,0** | 1,52 → **2,18** · 0,87 → **1,00** |
| reggaeton | lead | 679 → **1913** | −18,1 → **−8,3** | +3,9 → **−9,4** | −6,6 → **−2,5** | −8,3 → **−8,0** | 4,63 → **4,38** · 0,90 → **1,16** |
| reggaeton | nappe / accords | 679 → **748** | −17,5 → **−15,7** | +5,1 → **+4,6** | −6,9 → **−6,9** | −8,0 → **−7,6** | 2,56 → **2,67** · 0,96 → **1,09** |
| synthwave | lead | 963 → **1705** | −15,9 → **−8,5** | −3,4 → **−8,6** | −3,4 → **−3,0** | −9,3 → **−7,5** | 5,63 → **4,74** · 0,83 → **0,94** |
| synthwave | nappe / accords | 451 → **554** | −21,1 → **−15,5** | +11,4 → **+8,2** | −11,2 → **−9,0** | −7,2 → **−6,9** | 2,32 → **2,81** · 0,99 → **1,07** |

## Les sons seuls (sonde)

Non-harmoniques et repliement en dB (cible ≤ −60) ; vélocité : écart de niveau entre 0,4 et 1,0 (cible ≥ 4 dB pour le FM) ; centroïde / f0 à Do3 / Do5 / Do7 (cible : dans un facteur 2) ; attaque en dB.

| Son | Do6 | Do7 | Vélocité (dB) | Centroïde / f0 | Attaque |
|---|---|---|---|---|---|
| Supersaw | −33,1 → **−69,3** | −29,8 → **−64,4** | 4,7 → **4,7** | 2,16/1,55/1,06 → **2,93/2,19/1,62** | 1,7 → **2,5** |
| Hoover | −32,0 → **−44,6** | −25,9 → **−38,7** | 4,7 → **4,7** | 1,60/1,45/0,82 → **2,43/2,02/1,21** | 7,4 → **9,1** |
| Lead | −35,9 → **−53,5** | −33,8 → **−49,5** | 4,7 → **4,7** | 2,44/1,79/0,99 → **3,14/2,49/1,52** | 11,6 → **12,0** |
| Lead trance | −36,9 → **−55,5** | −33,1 → **−50,6** | 4,7 → **4,7** | 1,81/1,99/0,98 → **2,45/3,74/1,62** | 9,8 → **12,2** |
| Nappe | −38,4 → **−39,2** | −31,4 → **−36,8** | 4,7 → **4,7** | 1,91/0,96/0,53 → **4,43/2,22/1,15** | −3,2 → **−0,5** |
| Nappe chaude | −15,4 → **−47,2** | −0,2 → **−36,5** | 4,7 → **4,7** | 1,13/0,59/0,04 → **2,85/1,50/1,01** | −12,8 → **−11,9** |
| Pluck | −20,5 → **−56,3** | −0,2 → **−38,5** | 4,7 → **4,7** | 1,00/0,53/0,01 → **2,08/1,69/1,00** | 22,6 → **13,7** |
| Cloche | −13,7 → **−71,2** | −20,0 → **−41,0** | 4,7 → **4,7** | 1,35/1,33/1,33 → **1,36/1,37/1,39** | 29,0 → **12,0** |
| Piano électrique | −47,4 → **−54,2** | −24,1 → **−33,7** | 4,7 → **4,7** | 1,35/1,05/0,59 → **1,58/1,32/1,01** | 18,2 → **18,6** |
| Lead filtré | – → **−44,4** | – → **−40,8** | – → **1,3** | –/–/– → **3,02/2,87/1,82** | – → **0,3** |
| Piano FM | – → **−56,1** | – → **−24,4** | – → **4,9** | –/–/– → **2,38/1,63/1,51** | – → **7,0** |
| Rhodes FM | – → **−70,6** | – → **−78,7** | – → **4,8** | –/–/– → **3,82/2,70/1,21** | – → **7,1** |
| Vibraphone FM | – → **−89,3** | – → **−85,0** | – → **4,8** | –/–/– → **1,01/1,01/1,02** | – → **14,6** |
| Cloche FM | – → **−67,7** | – → **−72,7** | – → **4,8** | –/–/– → **3,80/2,50/2,18** | – → **4,2** |
| Stab métallique | – → **−3,2** | – → **−4,2** | – → **2,1** | –/–/– → **6,80/6,22/1,60** | – → **3,3** |
| Piano FM (avant : DX BASS 1) | −62,5 → **–** | −38,4 → **–** | −0,1 → **–** | 1,34/1,08/0,70 → **–/–/–** | 9,5 → **–** |

## Écoute en direct (comme le téléphone)

- 90 s, 37 styles : 37/37 styles sans alerte. Gain live = la cible de loudness (dB) ; niveau = court terme (3 s) pendant la lecture ; courbe = la courbe du morceau sur la sortie générale (1 = pleine).
- 150 s, les 9 styles qui n'atteignent pas leur Drop en 90 s : 9/9 styles sans alerte. Gain live = la cible de loudness (dB) ; niveau = court terme (3 s) pendant la lecture ; courbe = la courbe du morceau sur la sortie générale (1 = pleine).

| Style | Gain live max (dB) | Limiteur le plus profond (dB) | Drop en direct, 4 s après (dBFS c.t.) |
|---|---|---|---|
| acid_techno | 7,2 → 7,2 | −4,8 → −4,8 | −9,2 → −9,2 |
| acidcore | 2,7 → 2,7 | −6,3 → −6,3 | −10,5 → −10,5 |
| detroit | 6,8 → 6,8 | −3,8 → −4,4 | −9,7 → −9,5 |
| gabber | 10,5 → 10,5 | −8,5 → −8,2 | −9,3 → −9,3 |
| hard_techno | 11,1 → 11,1 | −3,7 → −2,7 | −11,9 → −12,1 |
| industrial_techno | 5,1 → 5,1 | −4,2 → −4,2 | −9,2 → −9,1 |
| minimal | 11,5 → 11,5 | −5,0 → −5,0 | −10,1 → −10,2 |
| retro_techno | 9,2 → 9,0 | −6,1 → −5,5 | −9,5 → −9,6 |
| techno | 11,8 → 11,8 | −7,9 → −6,3 | −9,6 → −9,2 |
| trance | 12,3 → 12,3 | −5,7 → −5,3 | −10,2 → −10,3 |
| acid_house | 9,4 → 9,4 | −1,6 → −1,6 | −10,0 → −10,0 |
| afro_house | 8,1 → 8,1 | −6,4 → −6,4 | −9,3 → −9,4 |
| amapiano | 12,6 → 12,2 | −7,3 → −7,7 | −9,5 → −9,5 |
| deep_house | 9,7 → 9,8 | −4,7 → −4,6 | −9,6 → −9,5 |
| disco | 8,4 → 8,4 | −6,1 → −5,0 | −11,4 → −11,5 |
| house | 9,4 → 9,4 | −7,0 → −7,5 | −9,6 → −9,6 |
| speed_garage | 10,4 → 10,4 | −5,4 → −5,4 | −9,9 → −10,0 |
| tech_house | 8,7 → 8,6 | −8,3 → −8,3 | −10,7 → −10,6 |
| uk_garage | 9,5 → 9,7 | −6,1 → −6,0 | −10,2 → −10,2 |
| dnb | 13,9 → 13,9 | −5,4 → −5,8 | −10,2 → −10,1 |
| dubstep | 15,1 → 15,1 | −5,6 → −6,9 | −10,1 → −10,6 |
| jump_up | 11,1 → 9,5 | −5,7 → −6,6 | −11,6 → −11,8 |
| jungle | 11,6 → 11,6 | −4,7 → −4,6 | −9,2 → −9,5 |
| liquid_dnb | 13,5 → 13,5 | −4,9 → −4,6 | −9,9 → −10,2 |
| neurofunk | 14,0 → 14,0 | −5,8 → −5,8 | −10,9 → −10,9 |
| riddim | 15,5 → 15,5 | −6,0 → −6,0 | −11,7 → −11,7 |
| boom_bap | 6,1 → 6,1 | −6,1 → −6,6 | −11,0 → −10,9 |
| drill | 9,0 → 9,1 | −4,9 → −4,8 | −7,7 → −7,7 |
| grime | 8,3 → 9,1 | −4,7 → −4,6 | −10,0 → −9,7 |
| jersey_club | 9,1 → 9,1 | −6,1 → −6,2 | −8,9 → −8,9 |
| lofi | 7,1 → 7,1 | −4,3 → −3,9 | −8,8 → −9,0 |
| phonk | 10,1 → 10,1 | −7,8 → −8,1 | −7,7 → −7,7 |
| trap | 9,1 → 9,1 | −6,8 → −6,6 | −7,9 → −7,6 |
| afrobeats | 7,1 → 7,1 | −4,8 → −4,9 | −9,3 → −9,2 |
| pop | 5,5 → 5,4 | −4,9 → −4,5 | −9,2 → −9,1 |
| reggaeton | 6,9 → 7,1 | −6,1 → −5,3 | −9,5 → −9,5 |
| synthwave | 7,2 → 7,4 | −4,6 → −5,3 | −9,5 → −9,5 |

Voix seules en direct (`ps_live`, master coupé, 150 s, mesures où la voix joue) :

| Style | Voix | Centroïde (Hz) | Brillance | Boue | Présence tél. | Mouvement 8 mesures |
|---|---|---|---|---|---|---|
| dnb | lead | 666 → **1134** | −19,8 → **−7,8** | +6,2 → **+2,3** | −7,1 → **−5,2** | 6,44 → 3,97 |
| dnb | nappe / accords | 433 → **1073** | −37,0 → **−11,0** | +20,9 → **+0,6** | −21,9 → **−4,9** | 2,81 → 2,60 |
| dubstep | lead | 749 → **1886** | −19,1 → **−11,9** | +2,5 → **−48,9** | −6,8 → **−0,4** | 4,25 → 4,35 |
| house | lead | 591 → **1036** | −21,0 → **−12,3** | +7,7 → **−1,9** | −9,5 → **−5,3** | 4,93 → 3,50 |
| house | nappe / accords | 563 → **622** | −7,3 → **−18,6** | +11,2 → **+5,6** | −10,8 → **−7,2** | 1,30 → 2,29 |
| jump_up | lead | 213 → **1145** | −23,6 → **−6,0** | +20,6 → **+1,8** | −17,4 → **−5,8** | – → 5,81 |
| liquid_dnb | lead | 708 → **1992** | −22,3 → **−8,2** | +2,7 → **−38,3** | −7,8 → **−2,1** | 6,21 → 5,20 |
| liquid_dnb | nappe / accords | 416 → **1333** | −25,9 → **−10,2** | +15,9 → **+0,7** | −14,6 → **−3,8** | 1,71 → 1,37 |
| neurofunk | lead | 338 → **3967** | −40,1 → **2,2** | +23,5 → **−46,4** | −21,7 → **−5,2** | 5,54 → 4,82 |
| techno | lead | 382 → **1370** | −19,5 → **−13,3** | +10,3 → **−5,5** | −7,9 → **−2,2** | – → – |
| techno | nappe / accords | 356 → **421** | −33,3 → **−26,9** | +13,9 → **+11,2** | −11,0 → **−10,0** | 1,98 → 1,68 |
| trance | nappe / accords | 535 → **718** | −16,6 → **−7,7** | +7,5 → **+7,0** | −6,8 → **−6,5** | 1,53 → 0,71 |
| trap | lead | 1309 → **1980** | −17,7 → **−12,6** | −12,2 → **−50,4** | −2,0 → **−0,5** | 5,05 → 6,91 |

## Extraits à écouter (l'« après »)

| Style | Départ (s) | Drop à (s de l'extrait) | Gain (dB) | LUFS-I / TP | Téléphone : LUFS-I (écart) |
|---|---|---|---|---|---|
| techno | 109,1 | 7,3 (Drop) | −2,9 (avant −3,7) | −12,1 / −1,2 (avant −13,3 / −2,3) | −16,3 (−4,2 ; avant −4,5) |
| house | 55,1 | 7,9 (Drop) | −1,1 (avant −1,4) | −10,7 / −1,5 (avant −10,6 / −1,5) | −16,6 (−5,9 ; avant −4,7) |
| trap | 6,9 | 6,9 (Refrain) | −1,7 (avant −1,6) | −11,3 / −1,7 (avant −11,2 / −1,6) | −17,8 (−6,5 ; avant −6,3) |
| jump_up | 49,7 | 5,5 (Drop) | −2,9 (avant −4,0) | −12,5 / −1,2 (avant −13,6 / −3,1) | −14,8 (−2,3 ; avant −1,1) |
| acid_techno | 104,3 | 7,0 (Drop) | −2,9 (avant −2,9) | −11,3 / −2,3 (avant −11,3 / −2,3) | −16,1 (−4,8 ; avant −4,8) |
| dubstep | 61,7 | 6,9 (Drop) | −2,6 (avant −2,8) | −12,4 / −2,2 (avant −12,4 / −2,5) | −14,8 (−2,4 ; avant −2,5) |

Fichiers : `scratchpad/ab-leads/<style>-apres.m4a` et `<style>-apres-telephone.m4a`, à comparer à `scratchpad/audit-35/<style>-avant.m4a` (mêmes styles, mêmes mesures, même méthode).

## Ce que les recettes ne peuvent pas faire (lots moteur)

Mesuré ici, à reprendre dans le moteur (`leads-pros.md` § 6 et § 8) :
- **Lot B, le toucher et l'attaque** : la vélocité ne change toujours pas le timbre du synthé (rapport de centroïde 1,00 sur tous les sons du synthé ; seules les voix FM ont 1,01 à 1,34) : vélocité → coupure et → enveloppe ; le vrai suivi de clavier (ici fait une fois par partie, d'après sa note médiane : un pluck qui monte de deux octaves dans la même phrase s'assombrit encore) ; une enveloppe de hauteur et un clic de bruit pour les plucks et les stabs (attaque du Supersaw 2,5 dB, cible ≥ 3 ; du « Lead filtré » 0,3 dB) ; le vibrato par voix retardé (`Lfo1Delay` : ici 5 Hz ±8 cents mais dès l'attaque, et un seul LFO pour toutes les voix) ; le cycle de 8 mesures déphasable dans le moteur (ici une courbe de morceau : il ne joue pas quand une partie tourne seule en boucle, et il prend une des 16 courbes du moteur).
- **Lot C, l'unisson et le filtre** : la supersaw façon JP-8000 (rapports de Szabo, phases libres à chaque note, dérive, centre/côtés, passe-haut sur f0) : l'unisson reste linéaire et repart des mêmes phases ; le ladder vraiment suréchantillonné (ou le SVF partout) : le Hoover reste à −45 dB de non-harmoniques à Do6, le Lead à −54, la nappe à −39 (cible ≤ −60 : seul le Supersaw y est, −69) ; le chorus stéréo en entrée (`StereoChorus` somme encore en mono : les leads à unisson s'en passent désormais, les autres voix non) ; un drive avant le filtre.
- **Lot D, couches et nouvelles voix** : `Track.layers` (supersaw + carré à l'octave + clic, pluck + nappe, piano FM + air : ici la nappe de liquid est une piste à part) ; PWM et pulse-scie pour un vrai hoover (le hoover du jump up et de la jungle est encore 7 scies + un carré, sans PWM ni chorus de Juno) ; une FM 2 opérateurs par voix dans le synthé (le « Stab métallique » vient de Plaits, mono et sans réglage d'indice dans le temps).
- **Plaits** : un sélecteur de patch DX nommé dans le modèle (aujourd'hui une valeur de « harmonics ») ; la vélocité → volume est faite pour les modèles 2 à 4 seulement.
