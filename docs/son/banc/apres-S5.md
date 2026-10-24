# Banc de mesure du son : après S5 (mixage et master honnêtes)

Chantier S5 de `docs/PLAN-SON-PRO.md` : la cible de volume vise le Drop, l'écrêteur et le limiteur n'y retirent jamais plus de 3 LU, un vrai sidechain et un bus « Musique », un seul étage sur l'attaque du kick, des écrêteurs doux au mixage, les sections à leur niveau, une Montée qui monte, le M4A vérifié après encodage. Tout cela n'existe que pour les morceaux de l'assistant (version de mix 2, `Project.mixVersion`) : les projets sauvés gardent leur son (version 1).

Méthode : `scripts/song-bench.sh --premaster --m4a`, 37 styles, sons intégrés, enregistrés par `SongBenchExport` sur la branche S5 qui contient le générateur S3 (kit v3, 808, batterie jouée à la main). **Avant** : `apres-S3-generateur.json` (même générateur, master de la phase 33, sans S5) ; pour la largeur, le PLR et le gain de la cible, que ce rapport-là ne garde pas : `apres-S4-kit3.json`. Deux passages complets sur le même code donnent les mêmes WAV, octet pour octet (empreintes plus bas). JSON : `apres-S5.json`.

Nouvelles mesures :
- **La partie la plus forte** (`loud_lufs`) : la sonie court terme (fenêtres de 3 s toutes les 100 ms), moyenne en puissance des 20 % les plus fortes, au-dessus des portes −70 LUFS et −20 LU. C'est ce que la cible vise en version 2 (`LoudPart` du moteur), relu sur le fichier par `ps_measure`. Elle lit 0,3 à 1 LU au-dessus de la sonie des sections « Drop » entières (`main_lufs`), qui comptent aussi leurs mesures plus calmes.
- **La montée d'une Montée** : la sonie court terme juste avant son temps de silence moins celle des 3 premières secondes de ses 4 dernières mesures (comme le rapport S3 ; les mesures de fill et de crash comptent avec leur section).
- **Le M4A** : le morceau rendu comme l'export M4A de l'appli (plafond −2 dBTP), encodé en AAC 256 kbit/s par FFmpeg (codeur « fast »), décodé et relu ; au-dessus de −1 dBTP encodé à nouveau plus bas, comme le fait maintenant l'appli (`AacEncoder.encodeChecked`).

## Résumé

| Mesure (médiane, 37 styles) | Avant (S3 générateur) | Après S5 | Cible |
|---|---|---|---|
| Sonie de la partie la plus forte (LUFS court terme, 20 % du haut) | – | −8,2 | cible du style ±1 |
| Écart à la cible (LU) | – | −0,1 | ±1 |
| LUFS-I (morceau) | −9,0 | −9,7 | sous la cible du Drop |
| LUFS des sections Drop / Refrain | −8,8 | −8,7 | – |
| Perte dans la chaîne (LU) | 6,0 | 2,4 | ≤ 3 |
| Gain de la cible (dB) | 10,2 | 6,6 | – |
| PLR (dB) | 8,0 | 8,6 | ≥ 6 |
| True peak (dBTP) | −1,2 | −1,2 | ≤ −1,0 |
| M4A décodé, premier encodage (dBTP) | – | 0,4 | – |
| M4A décodé, après la vérification (dBTP) | – | −1,8 | ≤ −1,0 |
| Drop − Break exporté (LU) | 0,5 | 4,4 | ≥ +4 |
| Drop − Break avant master (LU) | 2,7 | 6,4 | ≥ +4 |
| Drop − Groove exporté (LU) | 0,1 | 1,4 | ≥ +2 |
| Drop − Groove avant master (LU) | 0,8 | 2,6 | ≥ +2 |
| Montée sur ses 4 dernières mesures, exporté (LU) | 0,7 | 2,4 | ≥ +3 |
| Montée, avant master (LU) | −0,2 | 3,1 | ≥ +3 |
| LRA (LU) | 3,4 | 4,2 | 3 à 9 |
| Côté/centre (dB) | −16,5 | −16,9 | −15 à −9 |
| Corrélation L/R | 0,96 | 0,96 | 0,4 à 0,85 |
| Grave 60–250 Hz au Drop (dB rel.) | −4,1 | −3,8 | ≥ −6 |
| Air > 6 kHz (dB rel.) | −17,8 | −17,3 | −20 à −16 |

| Critère | Avant | Après S5 |
|---|---|---|
| Partie la plus forte à ±1 LU de la cible | – | 36 sur 37 |
| Perte ≤ 3 LU (+0,2) | 9 sur 37 | 37 sur 37 |
| PLR ≥ 6 dB | 36 sur 37 | 37 sur 37 |
| True peak ≤ −1 dBTP | – | 37 sur 37 |
| M4A décodé ≤ −1 dBTP (premier encodage) | – | 2 sur 37 |
| M4A décodé ≤ −1 dBTP (après la vérification de l'appli) | – | 37 sur 37 |
| Drop − Break ≥ +4 LU exporté | 0 sur 25 | 16 sur 25 |
| Drop − Groove ≥ +2 LU exporté | 1 sur 37 | 9 sur 37 |
| Montées à +3 LU sur leurs 4 dernières mesures (exporté) | 0 sur 59 | 6 sur 36 |
| Côté/centre entre −15 et −9 dB | 13 sur 37 | 11 sur 37 |
| Grave 60–250 Hz ≥ −6 dB, bass music | 3 sur 7 | 4 sur 7 |

## Ce que ça dit

- **La sonie est gagnée, pas achetée.** La chaîne master retire 2,4 LU en médiane (6,0 avant), jamais plus de 3 ; son gain passe de +10,2 à +6,6 dB. 36 styles sur 37 ont leur partie la plus forte à ±1 LU de leur cible ; le morceau entier est 0,7 LU plus doux (−9,7 LUFS-I), parce que les Grooves, Breaks et Couplets sont maintenant sous le Drop.
- **Le Drop tombe enfin, après le master aussi.** Drop − Break exporté : +0,5 → **+4,4 LU** (16 styles sur 25 au-dessus de +4, aucun avant) ; Drop − Groove exporté : +0,1 → +1,4 (9 sur 37, un seul avant) ; avant master, +6,4 et +2,6. Les Montées montent : +2,4 LU exporté en médiane (+0,7 avant), +3,1 avant master ; 6 sur 36 passent +3 LU exporté.
- **Le M4A** : le premier encodage dépasse son plafond de −2 dBTP de 2,4 dB en médiane (+0,4 dBTP décodé, 2 morceaux sur 37 sous −1), plus qu'en phase 33 (+1 dB) : les charleys brillants du kit v3 et des mixes moins écrasés donnent à l'AAC des attaques qu'il reconstruit trop haut. La vérification après encodage ramène les 37 sous −1 dBTP (−1,8 en médiane), en 4 encodages au plus. Le dépassement de l'AAC ne suit pas le gain (sur trap : 0,7 dB plus bas retire 0,6 dB, 1 dB plus bas 0,1 seulement), d'où les pas croissants. Le téléphone encode avec son propre codeur (MediaCodec), pas FFmpeg : la vérification est la même.
- **Le grave** : 60–250 Hz au Drop −3,8 dB en médiane, 4 styles de bass music sur 7 au-dessus de −6 (3 avant) ; restent jump up (−7,1), dubstep et riddim (−6,4).

## Ce qui n'est pas atteint, et pourquoi

- **Hard techno à −6** (consigne) : sa partie la plus forte plafonne à −8,2 avec 3 LU de perte. Ses sons (kick dur, accords, lead) ne sont pas assez denses pour −6 sans écraser ; plus de gain ne donnerait que de la perte. Gabber (−7) et le reste de la techno et de la bass music (−7,5) arrivent à 0,2 à 1,0 LU de leur cible, plafonnés. Les −4 à −6 des hits viennent de productions bien plus denses et écrêtées : c'est un travail de sons (saturation, couches), pas de master.
- **Drop − Groove ≥ +2** (9 sur 37) : avant master on y est (+2,6), mais la chaîne prend 2 à 3 LU au Drop et moins ailleurs. Les styles qui pompent le plus (bass music) restent près de 0 : leur Groove a déjà le kick et la basse du Drop.
- **Montée +3 LU** (6 sur 36 exporté) : même raison, la fin d'une Montée est plafonnée comme le Drop qu'elle annonce.
- **Largeur** (côté/centre −16,9 dB, 11 styles sur 37 dans −15 à −9, 13 avec le S4 kit v3) : S5 n'a élargi que les charleys, percussions et ride ; les styles étroits (acid −31 à −37, lo-fi −26, house −20) ont un lead ou une 303 mono, ce qu'un mixage ne change pas.

## Moteur seul (tests `s5_*` d'`engine/host-tests`)

| Mesure | Résultat | Cible |
|---|---|---|
| Pompe sur le son du kick (pad 0 ou 5, avec ou sans voie à lui), profondeur 0,5 | −6,0 dB à 5 et 10 ms du coup, −0,2 dB 450 ms après | 4 à 8 dB en ≤ 10 ms |
| Clé sur un pad muet | 0,0 dB | rien |
| Bus « Musique » au repos (profondeur 0) | identique au mix sans bus | rien |
| Bus « Musique » (0,2 / graves 0,8), 10 ms après le kick | graves −14,0 dB, aigus −2,2 dB | une baisse, les graves plus profond |
| Écrêteur doux : sinus sous sa marge | différence −55 dB | transparent |
| Écrêteur doux : pointes de +12 dB sur un niveau de 0,1 (marge 6 dB) | crête 0,17 (plafond 0,16) | sous le plafond |
| Export en mode Drop, cible −9 (intro et fin calmes, Drop 3 fois plus fort) | Drop −9,08, partie forte −9,04, morceau −13,0, perte 0,16 LU, TP −1,19 | ±0,5 ; perte ≤ 3 |
| Export en mode Drop, cible −5 sur un Drop trop dense | −7,73, perte 2,97 LU | perte ≤ 3,1 |
| Export en mode morceau (projets d'avant), cible −9 | −8,7, comme avant | ±0,5 |
| Gain en direct (mode Drop, cible −9) : mix baissé de 6 dB 30 s | +1,4 → +1,4 dB (ne remonte pas) | aucune dérive |
| … puis plus fort que tout ce qui a été entendu | +1,4 → −0,8 dB | baisse |
| Gain d'un export repris en direct, puis mix plus doux | tenu à 0,01 dB | tenu |
| Allocations dans le callback, tout S5 (clé, pompes, bus, écrêteurs, mode Drop) | 0 en 300 blocs | 0 |

## Par style (exporté)

Cible : celle du Drop (version 2) ; Partie forte : la sonie de la partie la plus forte ; Drop : les sections « Drop » (ou « Refrain ») entières ; D − B, D − G : Drop − Break, Drop − Groove, exporté puis avant master ; Montée : sa montée sur ses 4 dernières mesures, exporté (avant master).

| Style | Cible | Partie forte | Écart | Perte | Drop | LUFS-I | PLR | M4A | D − B exp. / avant | D − G exp. / avant | Montée exp. / avant | Côté/centre | Grave 60–250 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| acid_house | −8,0 | −8,0 | −0,0 | 2,1 | −8,3 | −9,0 | 7,8 | −1,20 | +2,5 / +4,0 | +0,8 / +2,3 | +1,6 / +1,8 (+1,8 / +2,3) | −24,5 | −3,0 |
| acid_techno | −7,5 | −7,6 | −0,1 | 2,1 | −7,9 | −8,2 | 7,0 | −1,90 | +3,1 / +4,3 | +1,1 / +2,6 | +1,3 / +1,6 (+1,4 / +2,0) | −31,2 | −2,9 |
| acidcore | −7,5 | −7,9 | −0,4 | 3,0 | −8,2 | −8,4 | 7,2 | −2,03 | +0,4 / +2,3 | +0,7 / +1,9 | – (–) | −37,2 | −1,9 |
| afro_house | −9,0 | −9,0 | −0,0 | 0,6 | −9,6 | −10,6 | 9,4 | −1,55 | +5,1 / +5,5 | +2,4 / +2,8 | +2,5 / +3,1 (+2,8 / +3,5) | −17,0 | −1,7 |
| afrobeats | −9,0 | −9,1 | −0,1 | 3,0 | −9,4 | −10,2 | 9,1 | −1,24 | – / – | +0,9 / +3,2 | – (–) | −16,4 | −4,7 |
| amapiano | −9,0 | −9,0 | −0,0 | 0,7 | −9,4 | −11,7 | 10,5 | −1,18 | +11,4 / +12,0 | +5,2 / +5,8 | +3,9 / +4,5 (+4,0 / +4,6) | −19,9 | −0,9 |
| boom_bap | −10,0 | −10,1 | −0,1 | 1,6 | −10,4 | −11,7 | 10,5 | −1,88 | – / – | +1,8 / +2,7 | – (–) | −19,3 | −3,8 |
| deep_house | −9,0 | −9,0 | −0,0 | 0,7 | −9,3 | −10,8 | 9,6 | −2,52 | +4,4 / +5,0 | +2,6 / +3,2 | +2,0 / +2,8 (+2,2 / +2,9) | −16,9 | −4,0 |
| detroit | −8,0 | −8,0 | −0,0 | 1,1 | −8,6 | −9,2 | 8,1 | −2,92 | +5,5 / +6,7 | +1,4 / +2,5 | +2,2 / +2,6 (+2,8 / +3,5) | −20,0 | −4,4 |
| disco | −9,0 | −9,0 | −0,0 | 0,7 | −9,6 | −11,1 | 9,9 | −1,43 | +5,5 / +6,3 | +1,9 / +2,6 | +3,3 / +3,6 (+3,3 / +3,6) | −14,4 | −3,7 |
| dnb | −7,5 | −8,1 | −0,6 | 3,0 | −8,6 | −9,7 | 8,6 | −2,83 | +4,3 / +7,4 | +0,5 / +2,4 | – (–) | −12,2 | −4,3 |
| drill | −8,5 | −8,6 | −0,1 | 0,5 | −8,7 | −9,6 | 8,4 | −1,05 | – / – | +2,0 / +2,4 | – (–) | −20,7 | −3,0 |
| dubstep | −7,5 | −8,2 | −0,7 | 3,0 | −8,9 | −9,8 | 8,6 | −1,27 | +3,6 / +6,4 | −0,1 / +1,0 | +1,4 / +2,4 / +2,4 (+3,0 / +4,8 / +4,7) | −10,7 | −6,4 |
| gabber | −7,0 | −7,6 | −0,6 | 3,0 | −8,0 | −8,5 | 7,3 | −2,26 | +2,7 / +5,3 | +0,5 / +1,3 | – (–) | −12,4 | −5,2 |
| grime | −8,5 | −8,5 | −0,0 | 1,0 | −8,8 | −10,2 | 9,0 | −1,77 | – / – | +2,3 / +3,0 | – (–) | −15,1 | −3,1 |
| hard_techno | −6,0 | −8,2 | −2,2 | 3,0 | −8,6 | −9,8 | 8,7 | −1,50 | +2,4 / +5,0 | +2,6 / +4,2 | – (–) | −10,1 | −5,0 |
| house | −8,0 | −8,0 | −0,0 | 2,6 | −8,4 | −9,0 | 7,9 | −1,09 | +4,5 / +6,9 | +0,6 / +2,4 | +1,2 / +1,7 (+2,0 / +2,8) | −20,0 | −3,4 |
| industrial_techno | −7,5 | −7,6 | −0,1 | 0,4 | −7,8 | −8,6 | 7,4 | −1,50 | +6,5 / +6,8 | +1,8 / +2,1 | +1,1 / +1,7 (+1,6 / +2,2) | −16,9 | −3,3 |
| jersey_club | −8,5 | −8,6 | −0,1 | 1,8 | −8,8 | −9,7 | 8,5 | −1,70 | – / – | +1,4 / +2,5 | – (–) | −17,4 | −5,7 |
| jump_up | −7,5 | −8,1 | −0,6 | 3,0 | −8,8 | −10,0 | 8,8 | −3,19 | +4,6 / +7,6 | −0,1 / +1,6 | – (–) | −8,3 | −7,1 |
| jungle | −7,5 | −7,8 | −0,2 | 3,0 | −8,2 | −9,4 | 8,2 | −1,56 | +6,3 / +9,3 | +1,2 / +3,5 | – (–) | −15,0 | −3,2 |
| liquid_dnb | −8,0 | −8,1 | −0,1 | 2,8 | −8,6 | −10,0 | 8,8 | −2,11 | +6,0 / +8,8 | +0,7 / +2,4 | – (–) | −14,5 | −3,7 |
| lofi | −12,0 | −12,1 | −0,1 | 0,1 | −12,3 | −13,9 | 12,7 | −1,57 | – / – | +2,7 / +2,8 | – (–) | −25,6 | −3,0 |
| minimal | −8,0 | −8,3 | −0,3 | 3,0 | −8,7 | −9,6 | 8,4 | −2,35 | +2,7 / +4,9 | +1,9 / +4,8 | +2,1 / +2,5 (+2,8 / +3,8) | −11,3 | −3,9 |
| neurofunk | −7,5 | −8,0 | −0,5 | 3,0 | −9,0 | −9,8 | 8,6 | −3,14 | +6,0 / +8,8 | −0,1 / +0,0 | – (–) | −13,2 | −5,8 |
| phonk | −7,0 | −7,0 | −0,0 | 2,8 | −7,1 | −7,6 | 6,5 | −1,99 | – / – | +0,7 / +2,3 | – (–) | −22,7 | −5,0 |
| pop | −9,0 | −9,0 | −0,0 | 1,1 | −9,2 | −10,2 | 9,0 | −2,81 | – / – | +2,3 / +3,2 | – (–) | −19,3 | −3,1 |
| reggaeton | −9,0 | −9,1 | −0,1 | 1,6 | −9,3 | −10,1 | 8,9 | −2,48 | – / – | +1,3 / +2,5 | – (–) | −15,4 | −5,0 |
| retro_techno | −8,0 | −8,0 | −0,0 | 2,0 | −8,5 | −9,3 | 8,1 | −1,72 | +8,3 / +10,1 | +1,9 / +3,5 | +2,3 / +2,5 (+3,2 / +3,8) | −19,7 | −3,4 |
| riddim | −7,5 | −8,5 | −1,0 | 3,0 | −10,1 | −10,7 | 9,6 | −1,29 | +4,3 / +6,4 | −1,2 / −2,2 | +1,3 / +2,2 / +2,3 (+3,6 / +5,3 / +5,3) | −13,0 | −6,4 |
| speed_garage | −8,0 | −8,1 | −0,1 | 1,4 | −8,8 | −10,1 | 9,0 | −1,99 | +8,4 / +9,8 | +3,3 / +4,5 | +2,7 / +3,3 (+3,6 / +4,3) | −20,1 | −3,0 |
| synthwave | −8,0 | −8,0 | −0,0 | 2,4 | −8,2 | −8,8 | 7,7 | −1,76 | – / – | +1,0 / +2,7 | – (–) | −15,4 | −3,4 |
| tech_house | −8,0 | −8,4 | −0,4 | 3,0 | −8,9 | −10,1 | 8,9 | −2,13 | +3,9 / +6,8 | +1,2 / +2,8 | +2,4 / +2,7 (+2,3 / +3,0) | −18,7 | −4,0 |
| techno | −7,5 | −8,2 | −0,7 | 3,0 | −8,4 | −9,5 | 8,3 | −1,34 | +4,0 / +6,1 | +1,6 / +3,8 | +1,6 / +2,0 (+2,0 / +2,7) | −15,1 | −3,8 |
| trance | −8,0 | −8,3 | −0,3 | 3,0 | −8,4 | −9,5 | 8,3 | −1,62 | +1,4 / +4,1 | +1,9 / +4,3 | +1,2 / +1,5 (+2,2 / +2,5) | −10,7 | −4,8 |
| trap | −8,5 | −8,6 | −0,1 | 1,5 | −8,7 | −9,7 | 8,6 | −2,26 | – / – | +1,5 / +2,6 | – (–) | −18,7 | −5,3 |
| uk_garage | −8,0 | −8,1 | −0,1 | 3,0 | −8,5 | −9,0 | 7,8 | −1,72 | – / – | +1,0 / +3,2 | – (–) | −18,9 | −3,5 |

## Ce que le générateur et l'interface appellent

- Le générateur : `WizardMix.honest(project, style, partOfTrack)` dans `StarterWizard.build`, juste avant `WideMix.apply` (qui reste la dernière étape). Il pose `Project.mixVersion = MIX_V2`, `Mixer.sidechainKey` (le pad du kick) et `Mixer.musicBus`, retire les pompes « kick » des pistes du bus, met la pompe des pads sur le son du kick (4e réglage de la Pompe), remplace les colles du bus batterie et du master, règle le Punch, pose les écrêteurs, les courbes de la Montée et du volume général par section.
- L'appli : `NativeEngine.setMixVersion`, `setSidechainKey`, `setMusicBusDuck`, `setMusicBusMember`, `setMusicBusReturn` (dans `syncEngine` / `pushMixer`), `setKnownDrive` après l'export du morceau ; `LOUDNESS_LOSS` et `REPORT_LOSS`, `REPORT_LOUD` pour la ligne « les crêtes sont tassées de … dB » ; `AacEncoder.encodeChecked` pour le M4A.
- Le moteur : commandes `SetSidechain` (102 : a 0 clé, 1–3 profondeur, graves, relâche du bus, 4 voie dans le bus, 5 retour dans le bus) et `SetMixVersion` (103), `SetMaster` a 8 (gain connu) ; effet 28 « Écrêteur doux ».

## Le banc complet (exporté)

## Critères (docs/plan/son.md § 2.3 et étape 2)

| Critère | Morceaux conformes | Hors critère |
|---|---|---|
| True peak ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| True peak du M4A décodé ≤ −1,0 dBTP (bloquant) | 37 sur 37 | – |
| Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement | 36 sur 37 | hard_techno (−2,2, plafonné) |
| Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5) (bloquant) | 37 sur 37 | – |
| Perte dans la chaîne sous son plafond (bloquant) | 37 sur 37 | – |
| Continu ≤ 0,001 (bloquant) | 37 sur 37 | – |
| Aucun NaN (bloquant) | 37 sur 37 | – |
| Aucun échantillon écrêté (≥ 0,999) | 37 sur 37 | – |
| PLR ≥ 6 dB | 37 sur 37 | – |
| Facteur de crête du Drop ≥ 6 dB | 37 sur 37 | – |
| Drop − Break ≥ +4 LU (exporté ; sans Break : –) | 28 sur 37 | acid_house (+2,5), acid_techno (+3,1), acidcore (+0,4), dubstep (+3,6), gabber (+2,7), hard_techno (+2,4), minimal (+2,7), tech_house (+3,9), trance (+1,4) |
| Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté) | 9 sur 37 | acid_house (+0,8), acid_techno (+1,1), acidcore (+0,7), afrobeats (+0,9), boom_bap (+1,8), detroit (+1,4), disco (+1,9), dnb (+0,5), dubstep (−0,1), gabber (+0,5), house (+0,6), industrial_techno (+1,8), jersey_club (+1,4), jump_up (−0,1), jungle (+1,2), liquid_dnb (+0,7), minimal (+1,9), neurofunk (−0,1), phonk (+0,7), reggaeton (+1,3), retro_techno (+1,9), riddim (−1,2), synthwave (+1,0), tech_house (+1,2), techno (+1,6), trance (+1,9), trap (+1,5), uk_garage (+1,0) |
| Chaque Montée monte d'au moins +3 LU sur ses 4 dernières mesures (exporté ; sans Montée : –) | 22 sur 37 | acid_house (+1,6 / +1,8), acid_techno (+1,3 / +1,6), afro_house (+2,5 / +3,1), deep_house (+2,0 / +2,8), detroit (+2,2 / +2,6), dubstep (+1,4 / +2,4 / +2,4), house (+1,2 / +1,7), industrial_techno (+1,1 / +1,7), minimal (+2,1 / +2,5), retro_techno (+2,3 / +2,5), riddim (+1,3 / +2,2 / +2,3), speed_garage (+2,7 / +3,3), tech_house (+2,4 / +2,7), techno (+1,6 / +2,0), trance (+1,2 / +1,5) |

Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.

## Bandes du Drop (dB relatifs à l'énergie totale)

| Style | Sub < 60 | Grave 60–250 | Bas-méd. 250–500 | Médium 0,5–2 k | Haut-méd. 2–6 k | Air > 6 k |
|---|---|---|---|---|---|---|
| acid_house | −6,2 | −3,0 | −13,8 | −8,9 | −12,2 | −17,7 |
| acid_techno | −4,8 | −2,9 | −14,0 | −12,4 | −13,6 | −17,8 |
| acidcore | −10,6 | −1,9 | −10,4 | −11,6 | −12,6 | −15,0 |
| afro_house | −7,3 | −1,7 | −10,9 | −16,3 | −18,9 | −18,6 |
| afrobeats | −2,9 | −4,7 | −10,2 | −14,4 | −19,0 | −24,1 |
| amapiano | −14,7 | −0,9 | −11,4 | −13,1 | −19,7 | −19,2 |
| boom_bap | −3,2 | −3,8 | −12,3 | −18,0 | −17,4 | −20,0 |
| deep_house | −3,8 | −4,0 | −11,9 | −10,2 | −21,7 | −18,3 |
| detroit | −4,0 | −4,4 | −13,0 | −9,6 | −12,8 | −16,4 |
| disco | −9,0 | −3,7 | −5,8 | −10,7 | −16,2 | −17,3 |
| dnb | −5,7 | −4,3 | −7,3 | −9,8 | −14,7 | −15,6 |
| drill | −4,0 | −3,0 | −13,8 | −14,4 | −18,6 | −20,1 |
| dubstep | −4,3 | −6,4 | −5,8 | −10,2 | −17,6 | −17,4 |
| gabber | −6,0 | −5,2 | −6,3 | −7,9 | −16,3 | −16,5 |
| grime | −8,6 | −3,1 | −12,6 | −7,8 | −9,7 | −14,0 |
| hard_techno | −9,0 | −5,0 | −5,6 | −6,7 | −16,4 | −14,5 |
| house | −5,4 | −3,4 | −8,5 | −11,8 | −18,2 | −16,6 |
| industrial_techno | −6,1 | −3,3 | −10,1 | −8,1 | −17,0 | −18,1 |
| jersey_club | −3,2 | −5,7 | −10,9 | −8,6 | −18,8 | −19,1 |
| jump_up | −6,5 | −7,1 | −5,9 | −6,6 | −12,6 | −13,5 |
| jungle | −5,4 | −3,2 | −10,3 | −14,0 | −12,3 | −14,3 |
| liquid_dnb | −4,6 | −3,7 | −9,8 | −12,4 | −15,7 | −15,3 |
| lofi | −3,6 | −3,0 | −13,7 | −21,1 | −19,4 | −22,8 |
| minimal | −4,4 | −3,9 | −8,2 | −12,3 | −22,4 | −21,6 |
| neurofunk | −3,9 | −5,8 | −7,5 | −12,8 | −13,4 | −13,6 |
| phonk | −2,7 | −5,0 | −13,0 | −11,7 | −17,6 | −19,7 |
| pop | −4,7 | −3,1 | −10,0 | −14,3 | −17,2 | −19,5 |
| reggaeton | −3,4 | −5,0 | −11,1 | −10,6 | −14,3 | −17,3 |
| retro_techno | −4,3 | −3,4 | −12,0 | −12,4 | −14,9 | −16,6 |
| riddim | −4,1 | −6,4 | −9,8 | −7,9 | −14,8 | −12,9 |
| speed_garage | −5,2 | −3,0 | −10,5 | −13,0 | −15,3 | −15,8 |
| synthwave | −5,9 | −3,4 | −10,5 | −8,2 | −14,5 | −19,0 |
| tech_house | −4,8 | −4,0 | −10,1 | −9,7 | −17,0 | −15,0 |
| techno | −4,3 | −3,8 | −9,1 | −12,3 | −18,8 | −18,7 |
| trance | −5,1 | −4,8 | −7,6 | −8,5 | −19,1 | −15,8 |
| trap | −2,3 | −5,3 | −14,8 | −12,3 | −18,3 | −19,7 |
| uk_garage | −6,5 | −3,5 | −10,8 | −7,6 | −16,1 | −13,9 |
| **médiane** | −4,8 | −3,8 | −10,4 | −11,6 | −16,4 | −17,3 |

## Empreintes des rendus (SHA-256, WAV float 32 bits)

Deux passages du banc sur le même code donnent les mêmes octets.

```
692c03df4be06c6fae72bb4b4fb9948d9d6e1625beab5a492805351d6ae8d7bc  acid_house.wav
60a73f229d781064c2d29fcaec5661a1bcfe9d2e96fb44c1b1ac5f624fb47db7  acid_techno.wav
0ad3faac5a1bb99735a838d03e5264dd3a36174a971e018e974a8389f410515c  acidcore.wav
e02737d5c0534d265c7643b13505aa19c4ac5271f90f5636a6c16d058bd7590b  afro_house.wav
77037c8cd7341471efed01e4f57f90f6839567b4df1d4a88f60b59db39e0f915  afrobeats.wav
af71562d57f358865ed79fb56b00d7d3835159fc42cc11141d5fd85f30cbceae  amapiano.wav
f92d17f703feb6ed10b674caa93bc25082f4adb1e9b54494ce02752380cd71f6  boom_bap.wav
035c6ebedfe0e01f713e1a6d0d972746c869504789c1d061da9e3d0ac4db5a1d  deep_house.wav
23780adc98ec0f8d9ab49a7af6969d998ba7779c731931fd51982e709c7c57e7  detroit.wav
7aa50fc8f3e0ac8d0efa974366581eb4f3ee643bfdde6eeaef525f050974c86f  disco.wav
f4b856e08f6ed63cc035e91416fc84a6c111ccfb2f81e96c8fa0972678258135  dnb.wav
55ed8ce6f6b2da2de21343a487c15c11d85e6fe577aaa4f7bf28d425e0cd7fc0  drill.wav
a12bb9dc96c230523f8d24d4a0bd53173e7ecd292a12407d01d3478fa85262b1  dubstep.wav
1bb3996568c250636b7abd4f2ac8d182103765017e702a8c5bf66cd473f7db5b  gabber.wav
1fb265623bc9b3380d6f009779f0ea3ad97896bedc755e3801f01cc706a0cbb1  grime.wav
4f4996129a7c02e2bd1e3997ced380989516ba09974197d3b4818eb77a0eb528  hard_techno.wav
8ef093ab30a01afcf66ab2f45acb9fc04e7acac6f25e68233de121150f79ebda  house.wav
838d2775d62a8ce9f99d317144317a2f133976ae20c9f1552dbfb1995f670d8c  industrial_techno.wav
6a650342734d56d134796ce1e22db5ec7f9a419c65ceff104cc689e69a5f4180  jersey_club.wav
54478249353901f312de96c83d527c8c86d95f65e305be992c2a8fe235a56e5d  jump_up.wav
9bab67d6df1245c542e446dbe7a21360d8d9328b0e406ff4a6f50a2cc781b513  jungle.wav
1093d5c6f901e2bcf61ae28717a2f8978d96d3c90043e4eeb8d288edb3978461  liquid_dnb.wav
5ec4fe556523250c1631d1cf44d97928dcd66a3e587b94a12368a9d88673ac03  lofi.wav
6e4043de2e85e1962ed348c0a009b3a06bed9d518c142cd62c389ac16e1d7048  minimal.wav
ce09b918b8d66dcbbafa50ee766a58aa1971478377de089537a8f66a888d33fa  neurofunk.wav
eea9679362305c412223e81bc063a815f413dd150ae5ef1544cf61ceeeea3a27  phonk.wav
43c6208c9026c2f47133febcb71c4854c80a1f581048c848336fed948f07fa79  pop.wav
1d3feefd6678f051e2f154e6d1669e576d38a6a2fb3fccfe1a844f58caf7f307  reggaeton.wav
365388e8ef9d0b39fdea3e636756bd391e1c5e4a666f2ee131619843dcf1a38f  retro_techno.wav
bc5d176d8c1b63963935f437d98e7f1a37fab976de1227fd70bc647d54a902cd  riddim.wav
45985e8eecf22d471258d19080d2278cdf28068581ac64012c5d67e77ac0c929  speed_garage.wav
5eaa20170ae2ecc7cb00c857cacaebee95bc50448e73468765968f69e2cd808a  synthwave.wav
098944525c17a16df4c3d9d2379c47e05c3de46c82d3d5b05af52030f922131c  tech_house.wav
8e0cc7a394d15d33e830e5f0fbba6614f6d3f0493cf1bd00a1f1195a6eff329d  techno.wav
f50b5e7ba91eaf2971176830847fc27311c63b9e5db12e4bac30dbe927d85712  trance.wav
32596fda59b721235549fdd206983b40add630c75b2cd785cf6ce5d05bcea9ec  trap.wav
d15171eff5c9d213c2590747b0c6de8d490e097b603ebe1d6cf861b9bd907e92  uk_garage.wav
```
