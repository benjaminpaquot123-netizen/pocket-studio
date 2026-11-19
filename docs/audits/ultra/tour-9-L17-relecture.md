# Tour 9, lot L17 : relecture neuve
Relu : cbdfd5d7

## Diff lu
`git diff origin/ccr-80af1a1b-mn0wec...cbdfd5d7` : AcidScreen.kt, RhythmScreen.kt, TutoController.kt, TutoPanel.kt, TutoChecks.kt, TutoKind.kt, TripletsTest.kt, les 8 tests T9L17*, rapport et décisions du lot. Rien sous `engine/` : thread audio intact. Aucun test sauté ni désactivé (aucun `@Ignore`, aucun `assume`). Pas de changement du chemin de sauvegarde (le seul `store.save` nouveau passe par `tick`, déjà existant).

## Test relancé
- `:project-model:test --tests '*TripletsTest'` sur cbdfd5d7 (tâche de test réellement exécutée) : 8 tests, 0 échec, 0 sauté.

## Le grave de la relecture précédente (6832498e)
**Corrigé.** Le commit 25f33189 change `TripletsTest#theTutorialDetectsAndShowsATriplet` selon la règle de F032, sans l'affaiblir :
- en trap, un triolet vrai sur la case 5 (hors des cases 13 et 15 de la ligne du style) donne maintenant « pas fait », ce qui est exactement le défaut F032 ; `tripletsDone` lit les triolets de la ligne trap (`X.x.X.x.X.x.t.t.…` : cases 12 et 14 en index) et les exige tous ;
- en house (ligne sans triolet), n'importe quel triolet suffit (`hasTriplet`), et le test le prouve sur un projet house ;
- « Montre-moi » en trap pose toujours 12 et 14 et l'étape est alors faite ; en house, le triolet de la fin de mesure 1 valide.
Le test vérifie donc les deux branches de `tripletsDone` et reste vert. La décision est écrite dans `docs/decisions/tour-9-L17.md`.

## Bloquant
Aucun.

## Grave
Aucun.

## Moyen
- F032, `tripletsDone` : la ligne du style est répétée sur toute la grille (`StarterWizard.hits(line, steps)`). Sur une grille trap de 4 mesures, il faudrait aussi les triolets des cases 44 et 46, alors que la consigne ne parle que de la mesure 1. « Montre-moi » fait pareil (cohérent), mais un débutant qui suit le texte à la lettre resterait bloqué. Non vu dans le parcours Facile (grille de 1 ou 2 mesures à cette étape) ; à faire jouer par un testeur.
- F040, Acid : toucher une case muette y pose une note ; deux touchers sur une note la rendent muette. Changement de geste pour tous, hors tuto aussi (verrouiller un silence ou choisir une hauteur demande un toucher de plus). Voulu par F040, à valider par un testeur.
- F039, `TutoPadEar.heard` : lien global vers le contrôleur, rendu à null seulement à « Quitter ». Un contrôleur abandonné sans `quit()` reste tenu jusqu'au tuto suivant ; un pad joué entre-temps peut cocher l'étape d'écoute de l'ancien projet. Petite fuite, sans perte de données.
- F046, TutoGridLine : « Mesure 2 · N cases → » s'affiche maintenant à côté de « Encore N cases… » ; sur un écran étroit, le statut (une ligne) peut être coupé. Aucune capture ne montre ce cas.
- F036, `Crash.shown` rend null quand rien ne change (crash déjà posé partout) ; sans effet tant que l'étape est alors faite.
- Seul `TripletsTest` a été relancé ici ; le reste de la suite du modèle n'est pas rejoué dans cette relecture (la grande vérification de fin le fera).

## Points vérifiés sans constat
- F036 `endDemoNow` : la démo est arrêtée et le projet remis avant le geste ; le `finally` de la démo ne vole plus `demoJob` grâce au test `===`.
- F034 `rolls()` : la consigne qui dit « Roulement » demande et montre un roulement ×4 ; les fills de toms restent comme avant.
- F044 : les notes fantômes de la sous-basse viennent de `subOf`, la même ligne que le contrôle.
- F042 : le conseil lit `TutoChecks.sendChannels(p, bus, step)`, la même liste que le contrôle.
- Les tests globaux (`TutoHints.acid`, `TutoPadEar.heard`) sont remis à null en `@After`.

Verdict : 0 bloquant, 0 grave
