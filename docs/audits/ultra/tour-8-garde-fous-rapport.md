# Tour 8 : lot « garde-fous », partie A (rapport)

Base : `eq/tour-8-paquet4` (d27fd0244 = intégration 4987c3a58 + T0c + N). Branche : `eq/tour-8-garde-fous`.
Fiche : `docs/audits/ultra/tour-8-fiches/fiche-garde-fous.md`, partie A (la partie B est réglée par le chef).

## Points et preuves

| Point | Test | Rouge d'abord |
|---|---|---|
| MOYEN, exceptions périmées (moments) | déjà couvert par `TutoValidationMomentTest.everyExceptionStillBreaksTheRuleAndTheListOnlyShrinks` (la revue du tour 7 l'avait manqué) ; son plafond `FROZEN` passe de 368 au compte, 52. Le doublon ajouté dans `TutoOneMomentTest` au commit rouge est retiré | fausse entrée `techno/tempo engine` : les deux tests rouges à la CI 387 (94221107f) |
| MOYEN, exceptions périmées (lisibilité Facile) | `TutoReadabilityBudgetTest.everyFacileExceptionStillMisses` ; `FACILE_FROZEN` reste 409, déjà le compte exact (410 avec la fausse entrée) | fausse entrée `techno/tempo fr OPEN 100% 360dp` : rouge à la CI 387, seule entrée nommée |
| Mineur, repli du panneau | `Tour7TutoPanelFoldTest` : compteur de cas coupés (« Lire la suite »), rouge s'il vaut 0 | — (garde ; vert à la CI 387) |
| Mineur, étapes de volume | `Tour6TutoMixWordsTest.everyVolumeStepNamesItsTracksInEveryLanguage` : les 36 étapes, 14 langues, traduites et nommant chaque piste à baisser | règle rejouée hors Gradle (Python, mêmes fichiers) : 0 écart ; une piste retirée d'un texte la fait rougir ; vert à la CI 387 |

tests: com.pocketstudio.app.tuto.TutoReadabilityBudgetTest com.pocketstudio.app.tuto.Tour7TutoPanelFoldTest com.pocketstudio.model.tuto.TutoValidationMomentTest com.pocketstudio.model.tuto.Tour6TutoMixWordsTest

## Chemins

chemin: panneau du tuto Facile, toutes leçons, langues, tailles et largeurs | test: TutoReadabilityBudgetTest#everyFacileExceptionStillMisses
chemin: panneau du tuto, texte replié « Lire la suite » | test: Tour7TutoPanelFoldTest#theBlocksFoldInOrderTheKitStaysAndWhyIsTouchable
chemin: validation des consignes (un moment par consigne) | test: TutoValidationMomentTest#everyExceptionStillBreaksTheRuleAndTheListOnlyShrinks
chemin: étapes de volume du tuto Facile, 14 langues | test: Tour6TutoMixWordsTest#everyVolumeStepNamesItsTracksInEveryLanguage

## Entrées retirées

Aucune : à la CI 387, les deux tests ne nomment que la fausse entrée. Après T0c, aucune entrée réelle n'est périmée (52 moments, 409 cas de lisibilité).

Effet pour l'élève : aucun. `tuto-moment-exceptions.txt` n'est lu que par la validation (`TutoValidation`) ; une entrée périmée n'excuse plus rien, la retirer ne change aucun texte ni aucun écran.

## Mesure

CI 387 (run 44427624462) sur le commit rouge : seuls les rouges attendus. Aucun changement de `build.gradle.kts` : le test déjà présent était vert à la base, donc la seule entrée périmée était la fausse.

## Pas pris (clôture du tour 8)

- house, étape `levels` (Facile) : « (+4 fort, −∞ muet) », alors que le curseur du Mode simple écrit « Muet », pas « −∞ » (`MixerScreen.levelWords`). Texte du tuto : hors de ce lot.
