# Tour 8 : lot « garde-fous », partie A (rapport)

Base : `eq/tour-8-paquet4` (d27fd0244 = intégration 4987c3a58 + T0c + N). Branche : `eq/tour-8-garde-fous`.
Fiche : `docs/audits/ultra/tour-8-fiches/fiche-garde-fous.md`, partie A (la partie B est réglée par le chef).

## Points et preuves

| Point | Test | Rouge d'abord |
|---|---|---|
| MOYEN, exceptions périmées (moments) | `TutoOneMomentTest.everyExceptionStillBreaksTheRuleItExcuses` : chaque entrée doit encore être un problème mesuré ; plafond `CEILING` | fausse entrée `techno/tempo engine` (commit rouge 94221107f, CI 387) |
| MOYEN, exceptions périmées (lisibilité Facile) | `TutoReadabilityBudgetTest.everyFacileExceptionStillMisses` ; plafond `FACILE_FROZEN` baissé au compte mesuré | fausse entrée `techno/tempo fr OPEN 100% 360dp` (même CI) |
| Mineur, repli du panneau | `Tour7TutoPanelFoldTest` : compteur de cas coupés (« Lire la suite »), rouge s'il vaut 0 | — (garde ; vert attendu) |
| Mineur, étapes de volume | `Tour6TutoMixWordsTest.everyVolumeStepNamesItsTracksInEveryLanguage` : les 36 étapes, 14 langues, traduites et nommant chaque piste à baisser | règle rejouée hors Gradle (Python, mêmes fichiers) : 0 écart ; une piste retirée d'un texte la fait rougir |

## Entrées retirées

À remplir avec les listes de la CI 387.

Effet pour l'élève : aucun. `tuto-moment-exceptions.txt` n'est lu que par la validation (`TutoValidation`) ; une entrée périmée n'excuse plus rien, la retirer ne change aucun texte ni aucun écran.

## Pas pris (clôture du tour 8)

- house, étape `levels` (Facile) : « (+4 fort, −∞ muet) », alors que le curseur du Mode simple écrit « Muet », pas « −∞ » (`MixerScreen.levelWords`). Texte du tuto : hors de ce lot.
