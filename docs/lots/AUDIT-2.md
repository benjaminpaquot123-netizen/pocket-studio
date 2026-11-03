# Audit, étage 2 « tout est connecté et prévu comme il faut » : note de suivi (équipe étage 2, 2026-11-03)

Branche `fx-s2` (worktree `/home/user/fx-s2`), depuis l'étiquette `audit-1`. Rien de poussé, pas d'APK. Référence : `docs/audits/AUDIT.md` §4 étage 2, §5, §6. L'étage 1 (captures, langues, accessibilité, son, exports, tests instables) est fait par l'autre équipe : ses colonnes de la carte lui sont renvoyées.

## Ce qui a été livré
| Point de l'AUDIT | Livré | Test |
|---|---|---|
| 1. Carte des fonctions | `docs/audits/CARTE-FONCTIONS.md` : 23 fonctions × 13 branchements | `CarteFonctionsTest` (chaque test cité existe ; les cases vides ne peuvent pas augmenter ; chaque fonction a son mot dans le Lexique) |
| 2. Machine à états de la navigation | 22 chemins depuis « Que veux-tu faire ? » en Mode simple, petit téléphone | `NavigationMapTest` (≤ 3 touchers, l'écran promis s'affiche, Retour ramène à l'accueil sans quitter l'appli) |
| 3. Interactions deux à deux | générateur « toutes les paires » (glouton, façon AETG) : état × mode × langue × événement, environ 27 cas (le minimum possible est 9 × 3) au lieu de 162 | `PairwiseInteractionsTest`, `PairwiseOthersTest` (appel pendant un export, couverture des paires) |
| 4. Allers-retours des données | fichier sans un de ses champs (chaque champ de chaque classe atteinte par les projets de référence), sauvegarde et réouverture | `FormatMigrationTest` ; zip et réouverture dans `WeekInTheLifeTest` |
| 5. Contrats interface ↔ moteur | 4 commandes sans test moteur (303 en direct, tonalité de la 303, volume du métronome, annulation du test de latence) testées à travers `Engine::process` | `contract_tests.inc` (hôte) ; `AppContractsTest` (chaque commande a un test hôte, chaque fonction de `NativeEngine` est appelée ou listée avec sa raison) |
| 6. Règles d'architecture | un navigateur de sons, un réglage du tempo, un chemin d'annulation, nombres dans la langue de l'appli, réglages observés à l'écran | `ArchitectureRulesTest` (+ existants `MuteSoloEverywhereTest`, `StillPadsEverywhereTest`, `LiveGridStyleTest`) |
| 7. Rien de mort | réglage sans effet (aucun), commande morte `setDrumRepeat` retirée, identifiants de commandes orphelins retirés | `ArchitectureRulesTest::noSettingWithoutEffect`, `AppContractsTest` ; textes inutilisés : `I18nCatalogTest::noTranslationIsLeftUnused` (existant) |
| 8. Une semaine dans la vie | tuto, morceau de l'assistant, remix (chanson synthétique), cases du Live, performance au clavier MIDI, réouverture, zip, mise à jour | `WeekInTheLifeTest` |
| MiniLab et profils | chaque profil de `Controllers.PROFILES` reconnu sous les noms réels des appareils ; clavier inconnu en générique, accueilli par son vrai nom | `ControllerNamesTest` |

ADÉLA - Boys : non utilisée (la semaine tourne sur une chanson synthétique ; rien de tiré d'ADÉLA n'est commité).

## Fiches de défaut

### Grave
1. **Annuler (ou toucher) pendant « Montre-moi » mélangeait la démonstration et le projet** (zone : tutos ; cause racine : état affiché pris pour l'état réel). La démonstration remplace l'état courant de l'historique (`showQuietly`) ; un « Annuler » pendant qu'elle joue reculait depuis l'image de démonstration, puis la fin de la démonstration réécrivait l'état d'avant : l'annulation était perdue, et « Rétablir » remettait la démonstration comme si l'utilisateur l'avait faite. Un toucher pendant la démonstration s'appliquait sur l'image puis était effacé. **Corrigé** à la racine : `ProjectSession.beforeChange` (appelé avant chaque changement annulable, annuler et rétablir) retire d'abord ce qui n'est que montré (`RhythmViewModel.endTutoDemoShown`). Test : `PairwiseInteractionsTest` (événements « annuler / toucher pendant Montre-moi », dans chaque état × mode × langue tiré).

### Moyen
2. **Fiche de piste : la case d'une partie ne changeait pas à l'écran** (signalé par la relecture F). Elle suivait l'analyse du Guide (300 ms plus tard, parfois bien plus) au lieu du projet. **Corrigé** : les cases « Quand » lisent le projet (`TrackSheetRoute`). Test : `TrackSheetTest::aChainIsCopiedAndAPartSwitchedFromTheSheet` (état coché vérifié après chaque toucher ; échouait avant).
3. **Réglages lus sans être observés** (lint `StateFlowValueCalledInComposition`) : Mode simple et noms de notes (Do Ré Mi / C D E) restaient anciens sur l'écran ouvert (nouveau projet, assistant, mots simples, touches du clavier, vidéo). **Corrigé** : `observedSettings()` ; `ArchitectureRulesTest::settingsReadOnScreenAreObserved`.
4. **Bibliothèque de sons à 4 gestes en Mode simple** sur petit téléphone (bouton sous le pli de Rythme). **Corrigé** : « Bibliothèque de sons » dans le menu ⋯ du Mode simple (3 touchers depuis l'accueil). `NavigationMapTest`, capture `nav/simple_more.png` regardée.
5. **Clavier MIDI inconnu accueilli « Clavier MIDI connecté »** au lieu de son vrai nom (consigne de Benjamin du 2026-11-03). **Corrigé** : `MidiViewModel.greeting`. `ControllerNamesTest`.

### Mineur
6. Chiffres formatés dans la langue du téléphone au lieu de celle de l'appli (3 endroits : chrono d'enregistrement des pads, durée d'attaque, fréquence du filtre tenu ; lint `NonObservableLocale`). **Corrigé** ; règle `ArchitectureRulesTest::numbersFollowTheInterfacesLanguage`.
7. Code mort : `NativeEngine.setDrumRepeat` (jamais appelé), constantes `CMD_RESAMPLE_ARM/STOP` jamais envoyées. **Retirés** ; `AppContractsTest`.
8. Un arrêt du transport ne terminait la prise en direct que si le moteur était là (sans effet sur le téléphone, mais le modèle restait « en enregistrement » sous test). **Corrigé** (`toggleTransport`) ; `PairwiseInteractionsTest` (appel pendant un enregistrement).
9. Lexique : pas de mot pour la page Live ni pour l'enregistrement de la performance. **Ajoutés** (14 langues). `CarteFonctionsTest::everyFunctionHasItsWordInTheLexicon`.

## Lint (tri demandé par REVIEW)
- 25 `StateFlowValueCalledInComposition` : 8 vraies lectures non observées dans l'appli → corrigées (n° 3) ; le reste : valeurs initiales de `collectAsStateWithLifecycle` (sans danger), lectures dans des rappels (`LaunchedEffect`, `onClick`), 4 dans des tests. Non corrigés : `rememberLaunchGate(firstLaunch = …)` (lu une fois au démarrage, voulu), `keyName` de l'assistant (fonction non composable, appelée dans un texte recomposé avec la carte).
- 2 `RememberReturnType` (`AppRoot.kt`, `remember(knobsVm, rhythmVm) { attach }` et `remember(rhythmVm.projectId) { onglet après reconstruction }`) : **gardés volontairement**. Le second doit s'exécuter pendant la composition, avant la lecture de `tab` : un `LaunchedEffect` montrerait une image de l'ancien onglet (le clignotement du n° 2 de Benjamin). Le premier est idempotent. Faux positifs à documenter.
- 2 lectures de langue non observées : `LivePadsFullScreen` → corrigé (n° 6) ; `RhythmScreen` (`LocalContextConfiguration`, tablette « par l'appareil ») : voulu.

## Décisions à recopier dans DECISIONS
- 2026-11-03 · Ce qui n'est que montré (« Montre-moi ») est retiré avant tout changement, annulation ou rétablissement (`ProjectSession.beforeChange`). Raison : défaut grave n° 1. Retour : retirer le crochet.
- 2026-11-03 · « Bibliothèque de sons » dans le menu ⋯ du Mode simple. Raison : 3 touchers depuis l'accueil. Retour : retirer l'entrée (et le chemin de `NavigationMapTest`).
- 2026-11-03 · Un clavier MIDI inconnu est accueilli par le nom que le téléphone lui donne. Raison : consigne de Benjamin. Retour : `MidiViewModel.greeting`.
- 2026-11-03 · Carte des fonctions tenue par un test (`CarteFonctionsTest`) avec un plafond de cases vides (40) qui ne peut que baisser. Raison : AUDIT §6 « aucune case vide ». Retour : supprimer le test.
- 2026-11-03 · Générateur de paires en Kotlin dans les tests (glouton sur le produit complet, petit) plutôt que PICT/allpairspy. Raison : rien à installer, déterministe. Retour : `Pairwise.cases`.

## Ce qui reste
- **40 cases vides** de la carte (liste en bas de `CARTE-FONCTIONS.md`) : surtout accessibilité et tailles d'écran (étage 1, autre équipe), « mêmes mots » (étage 5), et la ligne « Accords » (export, direct = export, performance, ≤ 3 touchers : à tester au prochain passage).
- ≤ 3 touchers non vérifiés pour : copier une brique, découpe du Remix (il faut une chanson séparée), cases Remix du Live, accords.
- Paires des autres écrans testées ailleurs : Remix + Live (`LiveRemixCellTest`), rotation pendant la découpe (`StemBoardModelTest::turningThePhoneKeepsTheSamePartOfTheSong`, au niveau du modèle seulement : pas de test d'écran avec rotation).
- Pad Live depuis l'accueil : le harnais Robolectric ne survit pas à la reconstruction de l'activité ; on vérifie que le projet Live s'ouvre (le reste : `PadLiveTouchCountTest`).

## Pour l'agent principal ou Benjamin
- **Capacité du moteur sans bouton** : les couches de vélocité d'un pad de batterie (`NativeEngine.setPadLayer`, plusieurs sons selon la force de frappe), construites en phase 34, jamais branchées. Choix : A) un réglage « couches » dans la fiche du pad (Pro), B) retirer le code moteur. Recommandé : A plus tard (utile avec le MiniLab sensible à la force) ; listé dans `AppContractsTest` en attendant.
- Capture `learn_glossary` : les deux nouveaux mots sont sous le pli, la capture n'a pas changé.
