# Tour 6, lot « tuto4 » : les phrases du tuto (2026-11-13)

Consigne du chef : réparer les phrases d'acceptation M1 à M7 (sièges de contrôle Ha-2/3/4 en français, Da-2/3/4 en japonais, Bb-2/3/4 en allemand), dans toutes les langues, sans code Kotlin.
Branche : `eq/tour-6-tuto4`, partie de `eq/tour-6-tuto3` (dcc06432c) avec la tête de `ccr-80af1a1b-mn0wec` fusionnée. Décisions : `docs/decisions/tour-6-tuto4.md`.
Fichiers : `project-model/src/main/resources/tutos/house.json`, `tutos/techno.json`, `i18n/*/tuto_house.json`, `i18n/*/tuto_techno.json` (14 langues) et les tests.

tests: *Tour6TutoPadNamesTest *Tour6TutoPanelPadQuotesTest *Tour6TutoPanelPartMadeTest

## Les phrases, point par point (toutes « tentées » : seul le siège de contrôle peut les dire corrigées)
| point | house (Ha, fr) | techno (Da ja, Bb de) | pas pris (code ou fichier d'un autre) |
|---|---|---|---|
| M1 pad nommé comme écrit | « Rim » (« Bord de caisse » en Simple), plus de « tambourin » | « Rumble », « Clap », « Charley ouvert », « Rim », « Cymbale » nommés comme sur le pad | ja « 手拍子たち » : `i18n/ja/ultra2z6.json` ou `I18n.kt` ; titre de grille « → スタジオのバスドラム » : `AppRoot.kt` vers 2037 ou `TutoPanel.kt` |
| M2 mots expliqués à leur première vue | kick, Groove, Tonique, Break, relances, Mix, solo | kick, basse, Drop, mesure (Takt), Force/Roulement/Triolet (ベロシティ, ロール, 3連符 au ch. 3), stab, Groove, transition | |
| M3 chiffres justes | « les 8 cases (4 par mesure) » ; la durée des parties annoncée au ch. 11 | mesures de chaque partie au ch. 11 (8, la Montée 4) ; plus d'« Offbeats » faux | « Encore 9 notes » : `TutoPanel.kt:288` |
| M4 pas de contradiction | Break : kick et basse se taisent, piano et charleys continuent ; Montée sans silence promis ; Drop 2 par « Créer la partie » | Break garde charleys et « Rim » ; Montée idem ; chaque étape du ch. 10 nomme « Créer la partie « X » » | définition « Break : … la batterie se tait » : `library/PlainWords.kt:55` |
| M5 combien, et ce que disent les chiffres | ch. 12 : 4 cases du « Shaker » ; ch. 14 : +4 … −∞ en une phrase ; ch. 15 : −14, « Pas de saturation », « aucun solo oublié » | ch. 14 et 15 : même chose, avec les libellés de l'écran | les étiquettes elles-mêmes : `SimpleMix.kt:316`, `Loudness.kt:31`, `ExportCheck.kt:292` |
| M6 envoi réverbe du Clap | grille › pad « Clap » › « Effets du pad » › « Envoi réverbe » | même chemin | en Mode simple, « Effets du pad » est caché : seul « Je le fais » y mène |
| M7 un mot par chose | « Partage ton morceau », « Partager » partout | « Partager » (シェア), « partie » au lieu de « scène », kit Techno dit tel quel | ja 共有 dans « « Partager » renomme aussi le projet » : `i18n/ja/finish.json` |

ne corrige pas: N19 : grave du lot 2 (TutoController.kt), hors des phrases de ce lot

## Liste des chemins
Les phrases sont prouvées par `Tour6Tuto4HouseTest` et `Tour6Tuto4TechnoTest` (project-model, 7 méthodes chacun, une paire rouge/vert par méthode). Les écrans de l'appli qui lisent ces mêmes phrases :
chemin: Tuto › chaque pad cité entre guillemets est un pad affiché | test: Tour6TutoPadNamesTest#aPadNamedIsAPadShown
chemin: Tuto en japonais › le pad cité prend le nom du kit | test: Tour6TutoPanelPadQuotesTest#japanese
chemin: Tuto en coréen › le pad cité prend le nom du kit | test: Tour6TutoPanelPadQuotesTest#korean
chemin: Tuto › Créer la partie › ce que dit le bouton suit l'étape | test: Tour6TutoPanelPartMadeTest#aPartThatLeavesBricksOutIsNotSaidTheWholeGroove
chemin: Tuto › Créer la partie › ce qui manque se lit sur la partie créée | test: Tour6TutoPanelPartMadeTest#whatAPartLeavesOutIsReadFromThePartMadeNotFromTheOneShown

## Preuves
- Chaque méthode vue rouge sur son assertion avant la correction, puis verte (paires « rouge: » / « vert: » par méthode ; M1, M2 et M3 techno ont un deuxième « vert: » du même sujet, expliqué dans les décisions).
- Sur la tête fusionnée : `:project-model:test` 1147 tests, 0 échec ; `:app:testDebugUnitTest --tests '*Tuto*'` 372 tests, 0 échec sur chaque équipe.
- Le contrôle (siège neuf sur la branche poussée, fr, ja, de) est à faire avant le portail : ses lignes `controle:` s'ajoutent ici.

## Hors des points, vu en passant (code)
- ja : le bouton « Nommer la partie « Groove » » reste en lettres latines ; l'alerte du Mix dit « Rim » au lieu du nom japonais du pad.
- Le swing est validé à 4 % alors que 7 % est demandé.
- `LexiconWordsTest` exige au moins 90 « À toi : dans Mix, » : il en reste 91.
