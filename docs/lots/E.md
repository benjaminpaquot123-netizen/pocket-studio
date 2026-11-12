# Lot E, « Master pro » (PLAN-AUDITS-FINAUX §1 bis, points 1 à 6) : note de suivi

Branche `fx-e` (2026-11-03). Commandes moteur 190 et 191 (plage E : 190 à 209).

## Fait
1. **Cible de volume** (Pro › Mix › Sortie générale › « Avancé : master pro ») : Spotify/YouTube −14, Apple Music −16, club −9, « comme le style » (la cible d'avant, sur le Drop), valeur libre de −23 à −6 LUFS (curseur). Une cible pro vise le **morceau entier** en LUFS intégré, quelle que soit la version du mix (`MasterChain::setForceSong`, commande 190 n° 2). Plafond −1, −0,5 ou −0,3 dBTP, tenu **après l'encodage** : WAV/FLAC directement (limiteur 0,2 dB sous le plafond), M4A avec le limiteur 1 dB plus bas et `M4aGuard` vérifiant la crête décodée sous le plafond choisi (n'importe lequel, plus seulement −1). Sans limiteur : « Sans limiteur » existant (WAV 32 bits flottants).
2. **Chaîne du master visible et réglable**, dans l'ordre : égaliseur, compresseur de bus (glue), largeur stéréo + grave en mono sous 80/120/160 Hz, saturation douce (effets d'insert du master, réglés dans « Effets » ; bouton « Mettre la chaîne pro » qui les pose dans les cases libres), limiteur : look-ahead 1 ms (fixe : la latence ne bouge jamais), attaque (20 à 100 % du look-ahead) et relâchement (×0,25 à ×4), commandes 190 n° 3 et 4, reprises par l'export (`OfflineRenderer::masterPass`).
   **A/B au même volume** (`C/MasterCompare.h`) : A avec le master, B sans (le mix avant tout le traitement du master, retardé comme la chaîne), Référence ; armé, chaque côté est ramené au volume du plus calme (jamais monté : rien ne sature), mesure par blocs de 400 ms sur ~20 s, glissé de 20 ms, sortie bornée à 0 dBFS pendant la comparaison. Rien n'est mesuré quand c'est désarmé (une branche par bloc). Fermer le panneau remet le master.
   **Morceau de référence** importé (90 premières secondes, décodé, mesuré en Kotlin, gardé dans `filesDir/references/<projet>.wav`, chargé dans le moteur par la commande 191), écouté au même volume, spectre en octaves comparé dans le rapport.
3. **Mesures de pro** (`C/LoudnessRange.h`, rapport du rendu hors ligne) : LUFS intégré, court terme max, momentané max, LRA (EBU Tech 3342), crête vraie (après compression pour le M4A), PLR, corrélation stéréo, perte en mono ; avertissement « en mono une partie du son s'annule ». Affichées sous « Mesures pro » après un export, et **rapport** `<nom>-rapport.txt` à côté du fichier (exports de la carte, pas « Partager »), avec le spectre en 10 octaves contre la référence.
4. **Fichier final** (carte Exporter › Avancé › « Fichier final ») : 44,1 / 48 / 96 kHz (96 : rééchantillonné après le master), 16 / 24 / 32 bits flottants (32 aussi avec limiteur), dither TPDF (déjà) + mise en forme du bruit en option en 16 bits (filtre de Lipshitz à 5 coefficients, `WavWriter`), fondu de début, « Couper les silences » (début au premier son, fin au dernier, queue de réverbe gardée), silence de fin 0,5 ou 2 s, plage exportée (morceau, une partie, une sélection de mesures : la chanson rendue jusqu'à la fin de la plage, gardée à partir de son début, à l'échantillon près), métadonnées titre (nom du projet), artiste, BPM, tonalité, pochette (WAV : LIST/INFO + chunk « id3 » ID3v2.4 ; FLAC : VORBIS_COMMENT + PICTURE ; M4A : moov/udta/meta/ilst, décalages stco/co64 corrigés si le moov est avant l'audio). Les puces « 44,1 kHz » et « 16 bits » d'avant sont remplacées par « Fichier final » (mêmes valeurs par défaut).
5. **Stems pour pros** : avec/sans le traitement du master (existant), **avec/sans les envois** (« Sans les envois » : chaque piste sèche + un fichier « Réverbe » et un « Écho » des retours seuls, mode de stem du mixeur, commande 190 n° 5), **tous de la même longueur depuis 0** (la plus longue ; les autres complétées de silence, `WavLength`), noms numérotés `<projet>-01-batterie.wav`… ; **nullité** : la somme égale le mix sans le master.
6. **Autres sorties** : MIDI et projet complet existaient (export MIDI, partage du projet en zip) ; vidéo de partage : la vidéo 9:16 existait (phase 39). « Vidéo de l'écran » : non faite (voir Idées).

## Matrice des exports (sortie de E)
`scripts/export-matrix.sh` : 30 cas couvrant toutes les paires (format × fréquence × bits × cible × plafond × 5 styles, 208 paires, générateur maison `scripts/bench/pairwise.py --check`), chaque cas = la chanson du style enregistrée par `SongBenchExport` et rejouée par `ps_render` avec les options du cas, FLAC par le `FlacEncoder` de l'appli et étiquettes par `Metadata` (`ExportPairsTest`), M4A par FDK AAC avec la boucle de `M4aGuard`, puis vérifié par ffmpeg (`scripts/bench/matrix_check.py`) : LUFS ±0,5, crête vraie sous le plafond après encodage, ffprobe (codec, fréquence, bits, durée, titre, artiste), décodage sans erreur ; stems de chaque style (avec envois / secs + retours) sommés contre le mix sans master sous −60 dB. Les 100 premières secondes de chaque chanson (disque). Résultats : voir plus bas.

## Tests
- Hôte (`engine/host-tests/masterpro_tests.inc`, 9 tests) : LRA/maxima/corrélation/mono sur signaux connus (LRA 10,0 pour 20 s puis 20 s à −10 LU), A/B et référence au même volume mesurés sur la sortie (±0,5 LU ; −9,0 / −9,0 LUFS, référence −26,9 / −26,9), stems avec envois et secs + retours (résidu −94 et −91 dB), fondu/coupes/silence, plage à l'échantillon près, 96 kHz 16 bits avec dither mis en forme (bruit sous 2 kHz −77 dB contre −60), attaque/relâchement sous le plafond, cible intégrée sur un morceau visé au Drop (−16,0 et −9,0 LUFS, plafond −0,3), et tout cela sans allocation dans le thread audio. Suite complète verte.
- Appli : `T/export/ExportPairsTest` (étiquettes WAV/FLAC/M4A relues par ffprobe, audio inchangé, moov avant/après), `T/export/StemNullTest` (fichiers numérotés, même longueur, modes de stem demandés, somme = mix), `T/export/AbSameLevelTest`, `T/mixer/SimpleModeUnchangedTest` (Partager : M4A, WAV, vidéo et rien d'autre ; options par défaut = formats d'avant ; projet sans master pro enregistré comme avant), `T/export/MasterProScreenshotTest` (360 dp, FR et DE).

## Règle « partout »
- Le plafond −1 dBTP était codé en dur à 4 endroits (limiteur, M4A −2, `M4aGuard` −1, plafond du gain) : tous suivent maintenant `MasterPro.ceilingDb` (`RhythmViewModel.startExport`, `M4aGuard.run(maxDecodedDb, firstCeiling)`, `pushMasterPro`) ; couvert par la matrice (les trois plafonds, M4A compris). Reste : `renderForVideo` garde −2/−1 (vidéo de partage, volontaire).
- Les réglages du master pro passent par un seul point, `loudnessLufs(p)` (appelé par `syncEngine` en direct et dans chaque export décrit) → `pushMasterPro` : direct = export.

## Décisions à recopier dans DECISIONS
- 2026-11-03 · A/B « au même volume » : chaque côté ramené au volume du plus calme, jamais monté. Raison : monter le mix sans limiteur au volume du master saturerait. Retour : `MasterCompare::measure` (cible = celle du master).
- 2026-11-03 · Look-ahead du limiteur fixe (1 ms), réglables : attaque (part du look-ahead) et relâchement. Raison : changer le look-ahead changerait la latence de tout le moteur (pads, MIDI, enregistrement). Retour : `MasterChain::setLimiterAttack`.
- 2026-11-03 · Une cible pro (Spotify, Apple, club −9, libre) vise le morceau entier (LUFS intégré, ce que mesurent les plateformes), « comme le style » reste sur le Drop. Retour : `ProForceSong` à 0.
- 2026-11-03 · Les puces « 44,1 kHz » et « 16 bits » de l'export deviennent « Fichier final » (44,1/48/96 kHz, 16/24/32 bits). Retour : remettre les deux puces dans `ExportCard`.
- 2026-11-03 · Tous les exports reçoivent titre, BPM, tonalité (et artiste, pochette si donnés) ; le rapport texte seulement pour les exports de la carte (pas « Partager »). Raison : le Mode simple et le partage ne changent pas. Retour : `tagAndReport`.
- 2026-11-03 · Référence : 90 premières secondes gardées (mémoire), mesurée en Kotlin. Retour : `MasterProController.REFERENCE_SECONDS`.
- 2026-11-03 · Matrice en paires sur les 100 premières secondes de chaque chanson (disque du conteneur : 3 Go libres) ; la matrice complète sur toute la chanson tourne à l'étage 1 de l'Audit (`MATRIX_SECONDS=1000`).
- 2026-11-03 · Vidéo de l'écran (capture d'écran du téléphone) : non tentée, « Idée » (la vidéo 9:16 de partage existe ; une capture d'écran demande `MediaProjection` et une permission, hors de la demi-journée avec la matrice).

## Fichiers touchés hors liste stricte (à savoir au rangement)
- Neuf `engine/src/main/cpp/core/MasterCompare.h` (A/B et référence, entête seul).
- `engine/src/main/cpp/core/Engine.h` : `collectGarbage` libère aussi la référence.
- `RhythmViewModel.kt` : seulement `ExportUi` (à côté d'`ExportFormat` : champs `pro`, `proReport`, `parts`, `songBars`), sections Export (dont `describe`) ; `loudnessLufs` (section Export, appelé par la section Mixer) envoie aussi les réglages du master pro.
- `A/rhythm/StemExports.kt` : `WavLength` (longueurs des stems).

## Outils tiers (pour THIRD_PARTY)
- FDK AAC via `fdkaac` (paquet système, licence FDK AAC de Fraunhofer) et ffmpeg 6.1.1 (LGPL/GPL) : outils de banc seulement, rien dans l'appli. Générateur de paires : maison (pas de PICT ni d'allpairspy).

## Idées (non faites)
- Vidéo de l'écran (voir décision).
- Spectre de la référence superposé à l'analyseur en direct (aujourd'hui : dans le rapport).
- Mise en forme du dither propre à 96 kHz (le filtre de Lipshitz est calculé pour 44,1 ; à 96 kHz le bruit part au-dessus de 20 kHz, mieux encore, mais non optimisé).

## Résultats (2026-11-03)
- **Matrice en paires : 30 cas sur 30 OK, stems des 5 styles OK** (`build/matrix/report.md`, jamais commité). LUFS à ±0,2 de la cible pour toutes les cibles pro ; crête vraie sous le plafond dans tous les cas après encodage (M4A décodé compris) ; fichiers lus par ffmpeg (codec, fréquence, bits, durée, titre, artiste). Stems : résidu −103 (dnb), −92 (house, sans envois), −75 (lofi), −237 (techno), −97 dB (trap, sans envois).
- Notés, pas échoués : « comme le style » visé au Drop sur 3 cas où la chaîne prend déjà ses 3 LU (Drop à −8,0 pour −7,5 en dnb, −8,2 pour −7,5 en techno, −9,3 pour −8 en house M4A) ; club −9 en M4A sur la dnb : −9,8 (la chaîne prend son maximum, 6,9 LU : on ne tasse pas plus).
- **Défaut trouvé par la matrice et corrigé** : un fichier rééchantillonné (44,1 ou 96 kHz) d'un mix limité à 48 kHz dépassait le plafond de 0,1 à 0,3 dB (c01, c03, c05, c09, c24, c29) ; un limiteur true peak repasse maintenant à la fréquence du fichier (`OfflineRenderer::Output`, plafond du master). Règle « partout » : tous les exports rééchantillonnés passent par cet `Output` (WAV, FLAC, M4A, stems avec limiteur) ; les rendus en mémoire restent à 48 kHz. Matrice = le test qui couvre.
- **Banc CPU `m17_worst_case_live_cpu_bench`** (avant = fx-int, après = fx-e, en alternance, 3 fois) : moyenne avant 29,6 / 25,0 / 26,0 %, après 23,5 / 22,7 / 24,5 % ; p99 avant 46,8 / 36,5 / 39,3 %, après 37,1 / 37,2 / 38,3 % : pas de recul (le chemin A/B ne coûte qu'une branche quand il est désarmé). Marge non regagnée : rien dans ce lot ne touchait le coût du direct ; à mesurer sur le téléphone. `master_cpu_budget` (1,9 à 2,0 % pour 2 %) et `drive_effects_cpu` échouent parfois, avant comme après, sur ce conteneur chargé (instables, déjà là).

## Reste
- `master_cpu_budget` et `drive_effects_cpu` à la limite sur ce conteneur (instables avant le lot aussi) : à regarder avant G.
- La matrice complète (toute la chanson, `MATRIX_SECONDS=1000`, toutes les combinaisons) : étage 1 de l'Audit.
- L'écoute A/B et la référence ne sont testées que sur le banc hôte et par la logique de l'appli : à écouter sur le téléphone (liste de Benjamin, moment 1 : « master Spotify et master club »).
