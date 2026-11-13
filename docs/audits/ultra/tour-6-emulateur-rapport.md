# Tour 6 — lot « emulateur » : rapport

Fichier d'appli touché : `app/src/main/java/com/pocketstudio/app/library/PlainWords.kt` (une ligne). Un test neuf : `PhoneRegexFlagsTest`.

## Le défaut (grave : plantage sur un vrai téléphone)
- CI 43375199284, « Emulator smoke test » rouge : `JourneyTest.aFirstSongInTheModeSimple`, « nothing tagged wizard after 15000 ms ».
- **Cause** : le commit `b621715da` (lot « tutoconsignes ») a écrit le motif `(?iU)\btonalités?\b`. Le moteur de motifs du téléphone (ICU) refuse le drapeau « U » (`U_REGEX_INVALID_FLAG`, vérifié avec libicui18n 74) ; la JVM des tests et du banc l'accepte. `PlainWords` plante à son premier usage : l'assistant et le panneau du tuto en Mode simple.
- **Correction** : `(?iu)\btonalités?(?![\p{L}\p{N}])`. Le « U » servait à voir « é » comme une lettre en fin de mot ; le regard en avant fait de même sur les deux moteurs.
- **Garde** : `PhoneRegexFlagsTest#noPatternUsesAFlagThePhoneRefuses` lit toutes les sources Kotlin et JSON livrées (`app`, `project-model`, `ui-components`, `engine`) et refuse tout drapeau en ligne hors de i, m, s, x, u, d, ainsi que `UNICODE_CHARACTER_CLASS`. Il ne couvre pas les autres différences entre les deux moteurs (classes de caractères rares, regards en arrière sans borne) ni un motif assemblé à l'exécution : le test sur émulateur reste le juge.
- rejeu impossible : le banc tourne sur la JVM, qui accepte le drapeau ; remplacé par le test de garde `PhoneRegexFlagsTest#noPatternUsesAFlagThePhoneRefuses`.

## Paires
- `rouge: no pattern uses a flag the phone refuses` → `vert: no pattern uses a flag the phone refuses`.

## Non fait
- Le test sur émulateur avale encore l'exception d'origine (son journal ne nomme pas la cause) : noté pour un lot du banc.

tests: com.pocketstudio.app.library.PhoneRegexFlagsTest com.pocketstudio.app.library.Tour6PlainWordsTest com.pocketstudio.app.library.WizardPlainWordsTest

chemin: Premier lancement › Mode simple › assistant, première étape | test: PhoneRegexFlagsTest#noPatternUsesAFlagThePhoneRefuses
chemin: Mode simple › mots expliqués › « tonalité » | test: PhoneRegexFlagsTest#tonaliteIsStillExplainedWithItsAccent
