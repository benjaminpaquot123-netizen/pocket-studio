# Tour 9 : lot robot1b (rapport)

Base : `ccr-80af1a1b-mn0wec`. Branche : `eq/tour-9-robot1b`. Décisions : `docs/decisions/tour-9-robot1b.md`.

Lot robot1 rejoué (option B du chef) : le portail avait refusé `eq/tour-9-robot1` (tête e468982bd), son juge passant de 166 à 172 écarts, parce que les verts de C8, C11 et C10 n'avaient ni le sujet de leur rouge ni le nom de leur test. Ici, même contenu (`app`, `scripts`), commits repris dans l'ordre sur l'intégration 3d0bb823e ; chaque rouge et son vert portent le même sujet et le vert nomme son test ; pour C8, le correctif (ancien commit « wip: » 70b0edeb5) et la note (fad075264) forment un seul vert. Les fichiers `tour-9-robot1-rapport.md` et `tour-9-robot1-relecture.md` restent comme trace du premier passage ; les décisions passent dans `docs/decisions/tour-9-robot1b.md`. Tout est « tenté » (aucun testeur neuf ne l'a encore constaté).

## Commits du robot repris sur la branche (19, avant la fiche A1)

| Sha | Ce qui change |
|---|---|
| 6ecaf05f1 | BenStudio Test : un passage « Scénarios », neuf scénarios d'utilisateur avec chacun son oracle |
| e441abaed | preuve sur les vrais écrans, contrôle de l'APK, note sur le temps face à un tour de banc |
| f31295901 | relecture neuve : feuilles fermées après un rouge, une puce de plus, longue liste de projets, nombres entiers dans le JSON |
| e352f1200 / a332b51b3 | rouge / vert : les scénarios partent de l'écran de BenStudio Test (tab_Structure) |
| c7e687c60 / 17e31c413 | rouge / vert : indices à l'échec d'un scénario (capture et tags dans le rapport) |
| a126f5c65 / b0636eab5 | rouge / vert : son coupé, la latence n'est pas mesurée |
| 3374bcb34 / e0e3ee43c | rouge / vert : indices et seuil dépassé pour tout échec par mesure |
| 0ba53d0df | test : RetestApresEclairTest remet le studio devant |
| 59c15ecf1 / bdf8fc47c | rouge / vert : un plantage dans un scénario est son échec, avec ses indices, et le passage continue |
| 07b25d8bf | liste des projets, arrière-plan, relance du studio et saisie pour le robot |
| fea0efadc | 31 scénarios sur quatre niveaux, chacun avec son oracle, moins de 20 min |
| 31af9f083 | 100 scénarios en table et choix du paquet rapide ou complet |
| d64ea8da5 | débloque les cascades du robot vues au passage complet du téléphone |
| 46b563122 | paquet « Refaire les ratés » : rejoue les 78 scénarios ratés ou non mesurés |

## Points de la fiche A1 (tous « tenté »)

| Défaut | Ce qui change | Rouge | Vert |
|---|---|---|---|
| C8 | l'oracle de la montée lit la courbe ajoutée, plus une partie | 260f27e89 | 58602e555 |
| C11 | les pads du Live se trouvent par leur vrai nom | ad0800add | 51f142ca2 |
| C10 | les scénarios d'effets retirent leurs effets par « Annuler » et disent rouge quand l'effet reste ; toutes pistes pleines = non mesuré | da31bab02 | d58db3f1f |
| C5 | sans permission du micro, la latence est « non mesurée » (« Autoriser le micro ») sans essai ; micro indisponible = non mesuré | 160e5e7fa | e9025f6d7 |
| C2 | le Live écoute 30 s ; rouge s'il s'arrête tout seul avant le second ▶ du robot ; la phrase de réussite dit « puis le robot l'arrête » | 2dc53b96f | 9e1dca107 |
| C3 | plus long écart au-delà de 4 blocs : rouge seulement avec un décrochage compté, sinon orange ; lu aussi en ms à côté du tampon | 1b3703627 | fca49817e |

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
