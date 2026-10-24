# Banc de mesure du son : après S3 (partie générateur)

Chantier S3 de `docs/PLAN-SON-PRO.md`, partie générateur (l'assistant), sur le moteur S3 et S4 :

- **Kit v3** pour les morceaux de l'assistant (kick studio court, charleys clairs, clap en quatre claquements), les projets sauvés gardent leur kit.
- **Voix studio sur les pads** (`Project.padVoices`, enregistrées dans le projet) : la **808** joue la basse en trap, drill, phonk et jersey club (accordée sur la tonalité, chaque coup sur la note de l'accord, glissés de 110 à 180 ms où la ligne glisse, 1,5 dB plus bas hors Refrain) ; **kick studio accordé** (fondamentale ou quinte, 43 à 66 Hz) en house et en techno, **kick dur** en hard techno, gabber, acidcore et techno industrielle ; **rumble** accordé avec le kick (techno : coché par défaut, comme le dit l'étude des hits).
- **Batterie jouée à la main** (graine fixe : style et ambiance) : charleys en cycle 100/70/85/60, chaque coup un peu différent, clap 3 à 5 ms en avance, charleys et percussions 1 à 6 ms en retard, caisse « laid-back » (lo-fi +15 ms, boom bap +10 ms), notes fantômes, swing sur les charleys seuls en drill ; fin de phrase de 4 mesures prise parmi 3 ou 4 fins par famille ; version « B » du beat dans la 2e scène du Drop ; **fills plus gros aux 8 et aux 16** et **crash sur le 1 de chaque 16** du Drop (et au début d'un faux drop, d'un Break) en parties d'une mesure dont seule la batterie change (comme « Fill à la fin »), jamais à chaque boucle.
- Vérifié par les tests (`DrumFeelTest`, 37 styles) : σ des forces de charley ≥ 0,05 et au moins 6 mesures de batterie différentes par morceau, sur 37 styles sur 37.

Méthode : `scripts/song-bench.sh` (enregistrement par `SongBenchExport`, rendu `ps_render`, mesure `ps_measure`), 37 styles, sons intégrés ; « avant master » : les mêmes `.pscmd` rendus avec `ps_render --no-limiter` (comme pour S2). **Avant** : `apres-S2.json` (kit v2, sans S4). **S4 + kit v3** : `apres-S4-kit3.json` (les morceaux de S4, le kit passé en v3, sans le générateur S3) : la différence avec « Après » est ce que fait le générateur S3 seul. **Après** : la branche S3 générateur avec S4 fusionné. JSON : `apres-S3-generateur.json`.

La longueur du kick : le kick de chaque morceau (pad 0, sa voix et ses réglages tels que le morceau les envoie) rendu seul, un coup, −40 dB sous sa crête. La montée de la Montée : la sonie court terme (3 s) à la fin de ses 4 dernières mesures (le temps avant son silence) moins celle de leur début (+3 s), par Montée.

## Médianes sur 37 styles

| Mesure | Avant (S2) | S4 + kit v3 | Après | Cible |
|---|---|---|---|---|
| Longueur du kick (−40 dB, s) | 1,49 (plafond du rendu) | 0,32 (kit v3) | 0,32 | ≤ 0,35 |
| Grave 60–250 Hz au Drop, styles 808 (dB rel.) | −7,8 | −6,5 | −5,7 | ≥ −6 |
| Grave 60–250 Hz au Drop, 37 styles (dB rel.) | −6,1 | −4,2 | −4,1 | ≥ −6 en bass music |
| Air > 6 kHz (dB rel.) | −20,6 | −17,3 | −17,8 | −20 à −16 |
| Drop − Groove exporté (LU) | −0,2 | −0,4 | +0,1 | ≥ +2 |
| Drop − Break exporté (LU) | +1,0 | −0,7 | +0,5 | ≥ +4 |
| Drop − Groove avant master (LU) | +0,8 | – | +0,8 | ≥ +2 |
| Drop − Break avant master (LU) | +3,4 | – | +2,7 | ≥ +4 |
| Drop − Montée avant master (LU) | +0,8 | – | +0,7 | > 0 |
| LRA avant master (LU) | 3,8 | – | 4,7 | 3 à 6 |
| Montée : montée sur ses 4 dernières mesures, avant master (LU) | – | – | −0,1 | +3 |
| Montée : montée sur ses 4 dernières mesures, exporté (LU) | – | – | +0,6 | +3 |
| LUFS-I exporté | −8,3 | – | −9,0 | cible ±1 |
| Perte dans la chaîne (LU) | 4,0 | – | 6,0 | ≤ 6 |

| Critère | Avant (S2) | Après |
|---|---|---|
| Kick ≤ 0,35 s | 0 sur 37 | 37 sur 37 |
| Air entre −20 et −16 dB | 15 sur 37 | 19 sur 37 |
| Grave 60–250 Hz ≥ −6 dB, styles 808 | 1 sur 4 | 2 sur 4 (drill −3,5, phonk −5,5 ; trap −6,0, jersey −7,3) |
| Drop − Groove ≥ +2 LU avant master | 3 sur 37 | 3 sur 37 |
| Drop − Break ≥ +4 LU avant master | 9 sur 25 | 7 sur 25 |
| Drop − Break ≥ +4 LU exporté | 2 sur 25 | 0 sur 25 |
| Montée qui monte de +3 LU sur ses 4 dernières mesures | – | 0 sur 59 |
| True peak ≤ −1 dBTP, NaN, continu (bloquants) | 37 sur 37 | 37 sur 37 |

## Par style

Kick : sa longueur (s) ; Grave : 60–250 Hz au Drop (dB rel.) ; D − G, D − B : Drop − Groove, Drop − Break (LU) ; Montée : sa montée sur ses 4 dernières mesures, avant master, par Montée (LU).

| Style | Kick | Grave | Air | D − G avant master | D − B avant master | D − B exporté | Montée |
|---|---|---|---|---|---|---|---|
| acid_house | 0,32 | −6,7 → −3,4 | −20,9 → −17,9 | +0,2 → +0,2 | +2,1 → +1,2 | +0,9 → +0,1 | −1,5 / −1,9 |
| acid_techno | 0,32 | −6,6 → −3,2 | −20,6 → −17,4 | +0,5 → +0,8 | +2,4 → +1,6 | +0,2 → −0,1 | −1,5 / −1,5 |
| acidcore | 0,25 | −1,6 → −1,3 | −15,9 → −15,7 | −0,0 → −0,0 | +0,5 → +0,3 | −0,5 → −0,6 | −4,2 / −3,8 / −3,8 |
| afro_house | 0,27 | −6,4 → −2,1 | −21,9 → −18,4 | +0,6 → +1,1 | +3,4 → +1,8 | +1,6 → +0,4 | +0,0 / +0,1 |
| afrobeats | 0,33 | −6,1 → −5,0 | −24,6 → −24,3 | +0,8 → +0,7 | – → – | – → – | – |
| amapiano | 0,31 | −3,4 → −1,1 | −21,3 → −18,9 | +3,4 → +4,4 | +9,0 → +8,0 | +4,5 → +4,0 | +0,8 / +1,2 |
| boom_bap | 0,33 | −5,1 → −4,1 | −22,5 → −20,7 | +0,8 → +0,8 | – → – | – → – | – |
| deep_house | 0,27 | −6,2 → −4,3 | −21,2 → −18,6 | +0,6 → +1,0 | +2,5 → +1,5 | +1,6 → +0,9 | −0,9 / −0,9 |
| detroit | 0,32 | −7,9 → −4,9 | −19,3 → −16,8 | +0,4 → +0,4 | +4,7 → +4,2 | +2,0 → −0,5 | −0,1 / −0,1 |
| disco | 0,32 | −5,8 → −4,3 | −20,1 → −18,9 | +0,3 → +0,3 | +3,2 → +2,6 | +1,0 → +0,5 | −0,1 / −0,4 |
| dnb | 0,33 | −6,7 → −5,5 | −18,1 → −15,8 | +2,4 → +1,7 | +3,8 → +3,6 | −1,4 → −1,1 | +0,8 / +0,6 / +0,7 |
| drill | 0,33 | −7,0 → −3,5 | −21,4 → −21,0 | +0,7 → +0,2 | – → – | – → – | – |
| dubstep | 0,33 | −8,9 → −7,7 | −18,8 → −17,4 | +1,5 → +0,8 | +3,4 → +2,7 | −0,9 → −1,2 | +1,1 / +1,1 / +1,1 |
| gabber | 0,25 | −7,0 → −5,7 | −19,4 → −16,6 | +1,9 → +0,8 | +2,3 → +1,8 | −0,8 → −0,6 | +0,7 / +0,7 / +0,7 |
| grime | 0,33 | −5,7 → −3,2 | −14,7 → −14,0 | +0,9 → +1,0 | – → – | – → – | – |
| hard_techno | 0,24 | −6,0 → −5,8 | −19,0 → −14,5 | +1,8 → +1,7 | +2,3 → +2,1 | −0,8 → −0,5 | −0,3 / −0,2 / −0,2 |
| house | 0,32 | −6,0 → −3,9 | −20,8 → −17,8 | +0,5 → +0,3 | +3,1 → +2,7 | +1,6 → +1,0 | −1,0 / −0,9 |
| industrial_techno | 0,27 | −3,9 → −3,5 | −18,7 → −17,9 | +0,0 → +0,1 | +1,2 → +3,5 | +0,3 → +2,2 | −0,9 / −0,8 |
| jersey_club | 0,33 | −10,2 → −7,3 | −23,8 → −19,3 | +0,5 → +0,5 | – → – | – → – | – |
| jump_up | 0,33 | −10,0 → −9,0 | −19,2 → −14,5 | +1,7 → +1,3 | +4,7 → +3,9 | −0,8 → −0,8 | +0,8 / +0,6 / +0,7 |
| jungle | 0,33 | −5,0 → −3,9 | −17,5 → −15,0 | +1,0 → +0,8 | +4,8 → +5,1 | +1,0 → +0,9 | −0,2 / −1,0 / +0,1 |
| liquid_dnb | 0,33 | −5,4 → −4,8 | −16,9 → −15,4 | +2,4 → +1,9 | +4,5 → +4,1 | +1,3 → +0,5 | +0,8 / +0,7 / +0,9 |
| lofi | 0,33 | −4,4 → −3,2 | −25,5 → −23,2 | +0,8 → +0,8 | – → – | – → – | – |
| minimal | 0,32 | −5,9 → −4,1 | −26,7 → −22,5 | +1,3 → +2,6 | +2,2 → +1,4 | +1,4 → +0,1 | −0,8 / −0,2 |
| neurofunk | 0,33 | −8,6 → −7,2 | −18,2 → −14,3 | +1,8 → +0,7 | +7,2 → +5,7 | +2,1 → +1,6 | +1,0 / +1,0 / +1,0 |
| phonk | 0,33 | −6,0 → −5,5 | −23,0 → −19,8 | +0,5 → +0,1 | – → – | – → – | – |
| pop | 0,33 | −4,6 → −3,3 | −22,6 → −20,5 | +0,9 → +0,9 | – → – | – → – | – |
| reggaeton | 0,33 | −6,7 → −5,5 | −20,6 → −18,6 | +0,4 → +0,5 | – → – | – → – | – |
| retro_techno | 0,32 | −6,1 → −3,7 | −20,6 → −17,4 | +1,1 → +1,6 | +9,3 → +6,9 | +5,2 → +1,4 | +0,0 / −0,1 |
| riddim | 0,33 | −8,8 → −7,8 | −16,8 → −13,4 | −0,1 → −0,8 | +5,1 → +3,7 | +1,7 → +1,1 | +1,4 / +1,4 |
| speed_garage | 0,32 | −6,0 → −3,4 | −19,1 → −16,2 | +1,3 → +2,4 | +5,2 → +4,9 | +2,3 → +1,9 | +1,0 / +1,0 |
| synthwave | 0,33 | −5,5 → −3,9 | −21,0 → −19,4 | +0,5 → +0,6 | – → – | – → – | – |
| tech_house | 0,32 | −7,3 → −5,3 | −19,9 → −15,9 | +0,5 → +0,7 | +3,8 → +3,0 | +2,1 → +1,2 | −0,5 / −0,3 |
| techno | 0,27 | −4,9 → −3,9 | −22,4 → −19,9 | +1,2 → +1,7 | +2,5 → +2,1 | +0,5 → +1,0 | −0,5 / −0,6 |
| trance | 0,32 | −6,8 → −5,1 | −18,9 → −17,0 | +1,6 → +1,9 | +1,7 → +1,1 | −0,7 → −0,3 | −0,4 / −0,7 |
| trap | 0,33 | −8,5 → −6,0 | −22,8 → −20,2 | +0,7 → +0,4 | – → – | – → – | – |
| uk_garage | 0,32 | −5,4 → −3,6 | −17,3 → −13,9 | +0,8 → +1,0 | – → – | – → – | – |

## Ce que ça dit

- **Le kick ne traîne plus** (0,32 s au lieu de 1,5 s) et chaque kick de house et de techno sonne sur la tonalité du morceau ; l'**air** reste dans la cible (−17,8 dB, 19 styles sur 37 entre −20 et −16 au lieu de 15).
- **La 808** : le grave 60–250 Hz des styles 808 passe de −7,8 à −5,7 dB (médiane ; −6,5 avec S4 et le kit v3 seuls) : la 808 dédiée (harmoniques, clic, saturation) remplit le grave que le sinus « Basse sub » laissait creux. Elle est à la sonie de l'ancienne basse en trap (−15,6 contre −15,7 LUFS, piste seule), drill (+1,1) et phonk (−1,2) ; en jersey club elle est 4 dB sous le kick qu'elle double.
- **Drop − Groove avant master ne bouge pas** (+0,8 LU) : le kick court du kit v3 retire au Drop la queue grave qui faisait sa sonie, la 808 relevée de 1,5 dB au Refrain et le rumble (qui joue partout où joue le kick, intro DJ comprise) ne la rendent pas. **Drop − Break avant master baisse** (+3,4 → +2,7) pour la même raison (le Break n'a pas de kick). Exporté, le générateur rattrape une partie de ce que le kit v3 seul coûte (Drop − Break +0,5 contre −0,7 pour S4 + kit v3), mais reste sous S2 (+1,0). Réserver le rumble et la 808 au seul Drop gagnerait du contraste, mais l'intro DJ veut son kick et son rumble (Futureproof) : c'est un réglage de S5 (cible de sonie qui vise le Drop, bus basse) plus qu'un arrangement.
- **La Montée ne monte pas** sur ses 4 dernières mesures (−0,1 LU avant master, aucune à +3) : S2 la tient sous le Drop (roulement ≤ 0,7, accords à 75 %, kick et basse coupés sur 2 mesures). Pour +3 LU il faudrait que le roulement et le souffle montent davantage sur ces 4 mesures tout en coupant le grave : à reprendre avec S5.
- Sonie exportée −0,7 LU (−9,0) et 18 styles plafonnés : l'effet du kit v3 déjà mesuré par `apres-S3-moteur.md` ; la chaîne bute sur son plafond de 6 LU (S5).
