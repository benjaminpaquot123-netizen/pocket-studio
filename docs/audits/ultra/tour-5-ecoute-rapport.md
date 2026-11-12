# Tour 5, lot « écoute » : BenStudio Test écoute le vrai son du téléphone (T1b, 2026-11-12)

Plan : `docs/PLAN-TEST-APK.md` (étape T1b, remise à plus tard en T1). Décisions : `docs/decisions/tour-5-ecoute.md`.

## Ce que fait le lot
- Un bouton « Écoute au micro » dans BenStudio Test (build `-PbenTest` seulement) : environ 5 min, téléphone posé, pièce calme. Une phrase dit ce qui va se passer avant que le micro soit demandé ; refusé, chaque étape est « passée » (feu non concluant).
- Courbe du haut-parleur : un balayage de 12 s joué par le moteur, écouté au micro, analysé sur le téléphone par tiers d'octave (26 bandes, 50 Hz à 16 kHz), bruit de la pièce retiré ; un feu ; la courbe dans `mesures.json` (« micro » › « courbe_db ») et `rapport.md`.
- Empreinte du son : jump up, house, trap, techno, 30 s chacun au micro, énergie par octave (8 bandes) ; rien n'est enregistré sauf 2 s autour d'un trou ou d'une saturation (`sons/` du zip, dit à l'écran avant l'envoi).
- Côté Linux (`scripts/bentest/`) : `empreinte.py` (même analyse à l'identique ; « comparer » le banc entendu à travers la courbe du téléphone contre le micro, « filtrer » un WAV comme sur le haut-parleur du téléphone, « courbe ») et `comparer-telephone.sh` (les 4 morceaux joués par le banc en direct, puis la comparaison). Le filtre 300 Hz deviné du banc n'est pas touché : la courbe s'utilise à côté.
- Rien dans la vraie appli : aucun fichier de `app/src/main` touché ; `check-apk.sh` exige les nouvelles classes dans BenStudio Test ; `HorsVraieAppliTest` le vérifie.

## Preuves
- Sons fabriqués, valeurs attendues calculées depuis le filtre imposé : courbe retrouvée à 1 dB près de 100 Hz à 12 kHz (`CourbeTest`), empreinte à 1 dB près (`EmpreinteTest`), passage complet avec un faux téléphone (`EcouteMicroTest`), 2 s gardées autour d'un trou (`GuetTest`), bouton et phrase (`BoutonMicroTest`), côté Linux 12 tests (`python3 -I scripts/bentest/test_empreinte.py`).
- Chaque fonction : un commit « rouge: » (test seul, échec sur assertion, après un commit « squelette » de signatures) puis « vert: ».

## Corrections de la relecture
Les 7 points moyens et les mineurs de la première relecture sont corrigés (détail dans les décisions) : chaque bande du balayage jugée sur ses propres trames, octave non comparable plutôt que basse absente côté Linux, trou de 160 ms (les silences d'un rythme ne comptent plus), micro avant le balayage, fenêtre du banc placée par les pas du moteur, réponse du micro gardée hors de l'écran, phrase « au plus 4 extraits », test croisé Kotlin et Python.

## Restes (non prouvé ici)
- Rien n'a tourné sur un vrai téléphone : AudioRecord, la source « non traitée », le lecteur d'aperçu pour le balayage, le volume à 60 %. À l'essai.
- `comparer-telephone.sh` n'a été joué qu'avec un faux banc (`--live`, faux `ps_live`) : le vrai `live-bench.sh` n'a pas été lancé (Gradle, quota).
- Les seuils des feux sont des valeurs de départ, à recaler après le premier passage (T5).

## Chemins (endroits qui lisent ou montrent les mêmes données)
- chemin: Écoute au micro › courbe du haut-parleur | test: CourbeTest#theSweepThroughAKnownSpeakerGivesBackItsCurve
- chemin: Écoute au micro › bande sous le bruit de la pièce | test: CourbeTest#aBandUnderTheRoomsNoiseIsNotMadeUp
- chemin: Écoute au micro › feu de la courbe | test: CourbeTest#nothingHeardIsRedAndANoisyRoomOrange
- chemin: Écoute au micro › empreinte par octave | test: EmpreinteTest#filteredPinkNoiseReadsItsFilterOctaveByOctave
- chemin: Écoute au micro › empreinte sans le bruit de la pièce | test: EmpreinteTest#theRoomsNoiseIsTakenOutAndWhatItHidesIsNotMadeUp
- chemin: Écoute au micro › feu de l'empreinte | test: EmpreinteTest#nothingHeardIsRed
- chemin: Écoute au micro › extrait de 2 s autour d'un trou | test: GuetTest#aHoleIsFoundAndOnlyTheTwoSecondsAroundItAreKept
- chemin: Écoute au micro › rien gardé sans anomalie | test: GuetTest#aSteadySongKeepsNothing
- chemin: Écoute au micro › micro saturé | test: GuetTest#aSaturatedMicrophoneIsSaidAndOneExtractAtMost
- chemin: Rapport › mesures.json et rapport.md du micro | test: EcouteMicroTest#theRunFindsTheSpeakersCurveAndEachSongsFingerprint
- chemin: Écoute au micro › micro refusé | test: EcouteMicroTest#aRefusedMicrophoneSkipsEveryStep
- chemin: Écoute au micro › permission au moment voulu | test: EcouteMicroTest#theMicrophoneIsAskedForWhenItIsNeeded
- chemin: Écoute au micro › bande jugée sur ses propres trames | test: CourbeTest#aBandIsJudgedAgainstTheNoiseOfItsOwnFrames
- chemin: Écoute au micro › silences d'un rythme | test: GuetTest#theSilencesOfARhythmAreNotHoles
- chemin: Écoute au micro › micro avant le balayage | test: EcouteMicroTest#theMicrophoneListensBeforeTheSweepStarts
- chemin: Rapport › début de la fenêtre comparée | test: EcouteMicroTest#theWindowComparedStartsWhereTheSongIs
- chemin: Téléphone › pas joués par le moteur pour la fenêtre du banc | test: HautParleurReelTest#theStepsPlayedAreTheEngines
- chemin: Écoute au micro › réponse du micro après une rotation | test: EcouteMicroTest#theMicrophonesAnswerStartsTheRunOnce
- chemin: Banc Linux › même analyse que le téléphone | test: MemeAnalyseTest#theFixedSignalReadsTheSameNumbersAsOnLinux
- chemin: Écran de BenStudio Test › bouton et phrase | test: BoutonMicroTest#theButtonSaysWhatWillHappenBeforeTheMicrophoneIsAskedFor
- chemin: Vraie appli › sans le micro du robot | test: HorsVraieAppliTest#theMicrophonesCodeIsInBenStudioTestOnly
- chemin: Compilation › sources du robot pour -PbenTest seulement | test: HorsVraieAppliTest#theBuildTakesItForBenStudioTestOnly
- chemin: Contrôles d'APK › classes du micro | test: HorsVraieAppliTest#theApksChecksKnowTheMicrophone

tests: com.pocketstudio.bentest.*
