Relu : 5488ec308 (après fusion de l'intégration par le chef ; relecture d'origine sur 6558eb861)

# Relecture neuve, tour 9 L5a (contenu des cours du tuto)

Diff relu : `git diff b81c56d32..13574782b` (merge-base avec origin/ccr-80af1a1b-mn0wec). Moteur audio non touché, sauvegarde non touchée (seul `Scene.shownLabel`, affichage, change ; le libellé reste enregistré en français).

## Tests lancés (ciblés)
- project-model : `Tour9TutoL5aDottedTest`, `Tour9TutoL5aPartNamesTest`, `Tour6Tuto4TechnoTest`, `TutoAllCoursesTest`, `TutoOneMomentTest` : verts.
- app : `Tour9TutoL5a*` (Course, Notes, Pads, PanelWords, Quotes, Words, KitReady, Shots, KitShots), `Tour9AcidChipNameTest`, `Tour9PlainWordsL5aTest`, `Tour6TutoFlechesRollTest`, `TutoOneMomentShownTest`, `LanguagesScanTest` : 155 tests, verts après la correction ci-dessous.

## Bloquant
Aucun.

## Grave
- Corrigé dans 555d5d640. `Tour9TutoL5aKitShots(LargeText)Test#simple/proReadyStepOffersTheKitLater` (dernier commit du lot) : 4 rouges lancés avec les autres classes du lot (le texte restait « le kit du disco est chargé »), vert seul. Le test lisait le vrai catalogue des packs (réseau, `libraryVm.refreshPacks`) : la carte du kit n'arrivait pas, donc ni « Plus tard » ni le texte F061. Il pose maintenant le catalogue du style dans `TutoTestHooks.packs` (aucun pack sur le téléphone) et le remet à zéro après. Les 8 captures annoncées par le commit 13574782b, absentes, sont ajoutées et regardées (écran entier) : l'étape « prête » dit « Le kit du style n'est pas encore sur tes pads… « Plus tard » suffit », en Simple et en Pro, à 100 % et 130 %.

## Mineur
- F061 : la moitié « un kit déjà chargé n'est plus proposé » (t9-27) n'est pas traitée (carte dans AppRoot, hors lot). Le rapport le dit, mais la ligne de `corrections.tsv` ne le dit pas : le contrôle doit garder F061 ouvert pour cette moitié.
- F061 : le texte de l'étape « prête » se choisit sur l'offre du kit, calculée quand le catalogue est lu. Tant qu'il ne l'est pas, l'ancien texte (« kit chargé ») s'affiche, puis change. Au téléphone : un court éclair au premier affichage, ou l'ancien texte hors ligne. Le texte en Pro devient le même qu'en Facile (le détail des sons de la 909 disparaît tant que la carte est proposée).
- F065 : les lignes en clair ne s'affichent qu'en Mode simple (voulu, décision du lot). Sur la capture `tour9_l5a_acidwords_simple_100`, la carte du kit prend la place et aucune ligne « 303 : … » ne s'affiche sous un texte qui dit « Acid 303 ». La phrase « chaque mot expliqué la première fois » n'est vraie que sans carte du kit. Le Pro arabe garde « accents » et « slides » en lettres latines (avant le lot).
- F137 : l'Intro 2 en Facile dit « sauf ce qui n'arrive qu'au Drop », ce qui est flou pour un débutant (le « Pourquoi ? » nomme le riff). Recette identique aux deux niveaux : la phrase d'acceptation est tenue.
- F064 : la puce dit « Boucle acide » (le rapport et le registre disent bien « Boucle » ; le message du commit 107c9b80 dit encore « Séquence acide »). Le test ne vérifie que le français.
- F058 (`momentWords`) : on retire du libellé tous les signes de `TUTO_OPEN`/`TUTO_CLOSE`, apostrophe typographique ’ comprise. Aucune traduction actuelle d'un libellé de bouton n'en a besoin (seules les marques ‘ ’ du néerlandais, voulues), mais une future traduction avec « l’ » perdrait son apostrophe.
- `Scene.shownLabel` traduit aussi un nom tapé par l'utilisateur de la forme « <section> <lettre> » (« Refrain B ») : cohérent avec « Refrain 2 », sans danger.

## Vérifié sans défaut
- F058 et F137 : plus de guillemets imbriqués dans aucune langue (test sur tous les cours et sur la ligne du moment, 15 langues). En techno, les `textPro` retirés avaient le même contenu que le `text`.
- F059 : `Notes.ghosts` passe par `own()` comme `done` : les pointillés suivent la vérification d'une étape coupée. Test générique sur tous les cours (compte au moins 5 étapes, garde contre un test vide).
- F061 : `kitName` écarte les noms de voix du studio (`PadVoice.NAMES`) et retombe sur l'ancien calcul si tous les pads en sont. `readyText` ne touche que l'étape `ready`, plié comme ouvert.
- F063, F066, F067, F069–F072, F074, F080, F083, F091, F092, F095 : chaque test lit les cours tels que livrés et compare à l'écran ou à la vérification. La fixture `pro-step-ids.txt` suit le nouvel ordre de jungle.
- F079 : la limite de mot Unicode remplace `\b` (accordé, bouclé, accentuée testés) ; la ligne arabe du Break nomme la partie (test sur toutes les langues).
- Textes d'interface en français, avec leurs 14 traductions (READY_PENDING, « Boucle acide », 4 lignes en clair) ; code et commentaires en anglais. Aucun `!!` neuf dans le code de l'appli, aucun risque de plantage repéré.

Note du chef : fusion de l'intégration c2ae90f85 (conflits dans corrections.tsv et 21 fichiers de traduction du tuto). Fusion clé par clé : clés d'un seul côté gardées ; 60 traductions arabes touchées des deux côtés réunies mot à mot (solfège F063 du lot + titre de la feuille F077 et mot « clap » F088 de l'intégration) ; 21 retraductions de l'intégration posées sur les clés françaises renommées par le lot (Si♭ → La#, bouton Montée, Break, −2 → −1 ; F077, F085, F087, F088, F090, F098) ; corrections.tsv : union des lignes, aucun identifiant en double. Tests : :project-model:test et les tests tuto/i18n de l'appli (1268 + 1862) ; rouges : I18nCatalogTest (TrackBar.kt: it), TutoJourneyTest trap Facile (hat_triplet) et TutoReadabilityBudgetTest, les trois aussi rouges sur l'intégration seule, donc pas dus à la fusion.

Verdict : 0 bloquant, 0 grave
