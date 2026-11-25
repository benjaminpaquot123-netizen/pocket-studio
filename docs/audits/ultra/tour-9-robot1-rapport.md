# Tour 9 : lot robot1 (rapport)

Base : `ccr-80af1a1b-mn0wec`. Branche : `eq/tour-9-robot1`. Décisions : `docs/decisions/tour-9-robot1.md`. Tout est « tenté » (aucun testeur neuf ne l'a encore constaté).

## Commits du robot déjà sur la branche (19, avant la fiche A1)

| Sha | Ce qui change |
|---|---|
| e296fc445 | BenStudio Test : un passage « Scénarios », neuf scénarios d'utilisateur avec chacun son oracle |
| cb985ca2a | preuve sur les vrais écrans, contrôle de l'APK, note sur le temps face à un tour de banc |
| f10cdd2c2 | relecture neuve : feuilles fermées après un rouge, une puce de plus, longue liste de projets, nombres entiers dans le JSON |
| 60adcd600 / 1ec894c4d | rouge / vert : les scénarios partent de l'écran de BenStudio Test (tab_Structure) |
| 93bb15cda / 4fa533bd2 | rouge / vert : indices à l'échec d'un scénario (capture et tags dans le rapport) |
| 96ee2e889 / 20905cfd1 | rouge / vert : son coupé, la latence n'est pas mesurée |
| 37518c93d / 678db446d | rouge / vert : indices et seuil dépassé pour tout échec par mesure |
| b443a2932 | test : RetestApresEclairTest remet le studio devant |
| 3ee079b48 / 81f2b9c9c | rouge / vert : un plantage dans un scénario est son échec, avec ses indices, et le passage continue |
| aa3607ecd | liste des projets, arrière-plan, relance du studio et saisie pour le robot |
| f1512cac9 | 31 scénarios sur quatre niveaux, chacun avec son oracle, moins de 20 min |
| bf6564287 | 100 scénarios en table et choix du paquet rapide ou complet |
| 36728a4ea | débloque les cascades du robot vues au passage complet du téléphone |
| 2c0cab99c | paquet « Refaire les ratés » : rejoue les 78 scénarios ratés ou non mesurés |

## Points de la fiche A1 (tous « tenté »)

| Défaut | Ce qui change | Rouge | Vert |
|---|---|---|---|
| C8 | l'oracle de la montée lit la courbe ajoutée, plus une partie | 3e649c3c5 | fad075264 |
| C11 | les pads du Live se trouvent par leur vrai nom | bb128826f | c61403671 |
| C10 | les scénarios d'effets retirent leurs effets par « Annuler » et disent rouge quand l'effet reste ; toutes pistes pleines = non mesuré | 05ef0c3e6 | 9e67d2bce |
| C5 | sans permission du micro, la latence est « non mesurée » (« Autoriser le micro ») sans essai ; micro indisponible = non mesuré | 47c35f28b | 20df9ea7d |
| C2 | le Live écoute 30 s ; rouge s'il s'arrête tout seul avant le second ▶ du robot ; la phrase de réussite dit « puis le robot l'arrête » | 0b34dbea2 | 1529b6b08 |
| C3 | plus long écart au-delà de 4 blocs : rouge seulement avec un décrochage compté, sinon orange ; lu aussi en ms à côté du tampon | 9821c60db | cbcb90f56 |

tests: com.pocketstudio.bentest.*

## Chemins

chemin: Structure, montée (robot) | test: ScenariosVraisEcransTest#aBuildUpAddsItsCurveNotAPartThenUndoTakesItBack
chemin: Live, pads (robot) | test: ScenariosVraisEcransTest#aPadOfTheLiveIsFoundByItsRealNameThenTheSecondOne
chemin: Mix, effets des pistes (robot) | test: ScenariosVraisEcransTest#theEffectsScenariosLeaveTheStripsAsTheyFoundThem
chemin: Studio, mesure de latence (robot) | test: ScenariosRemiseTest#withoutTheMicrophonePermissionTheLatencyIsNotMeasuredAndNotTried
chemin: Studio, mesure de latence (robot) | test: ScenariosRemiseTest#aMicrophoneTheEngineCannotOpenIsNotMeasuredRatherThanFailed
chemin: Live, lecture 30 s (robot) | test: ScenariosTest#aLiveThatStopsByItselfBeforeTheRobotTouchesItFails
chemin: BenStudio Test, indices d'un rouge par mesure | test: ScenariosRemiseTest#aRedByMeasureLeavesAPictureTheTagsAndTheThresholdPassed

Les vrais écrans (téléphone et tablette, `ScenariosVraisEcransTabletteTest`) passent avec toute la suite `com.pocketstudio.bentest.*`.

## Captures

Aucun écran de l'appli n'est touché (banc seul) : rien à réenregistrer.
