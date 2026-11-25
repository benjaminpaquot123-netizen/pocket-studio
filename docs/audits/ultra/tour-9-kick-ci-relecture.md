# Tour 9, lot kick-ci : relecture
relu : eb7c360fa415aad3bb408e8578141f11346740bc

Relecteur neuf (sous-agent qui n'a pas écrit ce code). Lu : `git diff 0ed59078a..eb7c360fa415aad3bb408e8578141f11346740bc` (rouge a8ff9c23f, vert a59db829e, docs f5aaa509a et eb7c360fa).

## Contrôles

- Un seul fichier de code : `app/src/test/java/com/pocketstudio/app/tuto/TutoKickSoundTest.kt`. Rien dans l'appli.
- Diagnostic plausible et prouvé : avec `TutoTestHooks.packs` nul, AppRoot appelle `libraryVm.refreshPacks()` (réseau) ; avec un catalogue posé, plus d'appel réseau. Le commit rouge a8ff9c23f fait échouer `karimsGesturesWithTheKitCardOffered` avec le message de la CI (« the kick's sheet: not in time »).
- Test pas affaibli : aucune assertion retirée, pas d'`@Ignore`, pas d'`assume`, attente toujours 120 s. Le nouveau test ajoute une assertion (« the kit card offered ») qui prouve que la carte vient bien dans le cas CI et pas dans l'autre. Le repli du panneau est un geste de personne, pas un contournement.
- État remis à zéro : `TutoTestHooks.packs = null` dans `@After` (avec `player`) ; `BenchPristine` le vérifie et le remet aussi. `kitOffer` n'est pas touché. Pas de fuite vers les classes suivantes.
- Rejoué ici : `gradle-file.sh :app:testDebugUnitTest --tests ...TutoKickSoundTest` : 4 tests, 0 échec.

## Constats

| Point | Gravité | Constat |
|---|---|---|
| 1 | mineur | `theKickStepNowSaysShowsAndTicks` laisse encore le catalogue au réseau. Il range la carte par `putTheKitCardAway()` une seule fois, après la démo : si le catalogue arrivait plus tard (réseau lent), la carte pourrait cacher « J'ai compris ». Il ne touche pas « Réglages du pad » et était vert sur la CI : pas grave. Piste : lui poser `catalogue(true)`. `theDrumAndBassKitIsBuiltIn` n'utilise pas le panneau : sans risque. |
| 2 | mineur | KDoc de `catalogue()` : la carte « takes the panel's place » ; en fait elle grandit le panneau. |
| 3 | moyen, design hors lot | En Pro sur 360 × 780, la carte du pad n'a pas de place sous le panneau ouvert ; après « Plus tard », bande vide de 66 dp. À trier par le chef. |

Verdict : 0 bloquant, 0 grave
