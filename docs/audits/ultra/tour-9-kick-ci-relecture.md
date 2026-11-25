# Tour 9, lot kick-ci : relecture
relu : f5aaa509a3af5170ff78427585a49e8aea757185

Relecteur : le correcteur du lot. Aucun outil de sous-agent n'était disponible dans cette discussion, donc ce n'est pas une relecture neuve. Le chef peut en demander une. Lu : a8ff9c23f (rouge), a59db829e (vert) et f5aaa509a (docs), contre la base 0ed59078a.

## Contrôles généraux

- Un seul fichier de code touché : `app/src/test/java/com/pocketstudio/app/tuto/TutoKickSoundTest.kt`. Rien dans l'appli, le moteur ou le banc.
- Pas de `@Ignore`, pas d'`assume`, pas de délai allongé (120 s comme avant).
- État statique : `TutoTestHooks.packs` est remis à nul dans `@After` (BenchPristine le remet aussi).
- rouge constaté : a59db829e : rejoué sur a8ff9c23f : `karimsGesturesWithTheKitCardOffered` rouge en local, avec le message exact de la CI (« the kick's sheet: not in time », 120,4 s) ; les 3 autres tests de la classe sont verts.
- Vert : TutoKickSoundTest 4/4 ; `com.pocketstudio.app.tuto.*` : 475 tests, 0 échec sur a59db829e.

## Constats

| Point | Gravité | Constat | Fichier |
|---|---|---|---|
| 1 | mineur | `theKickStepNowSaysShowsAndTicks` et le test suivant laissent encore le catalogue au réseau. Ils gèrent la carte (« Plus tard ») et restaient verts sur la CI. Piste : leur poser `catalogue(false/true)` aussi. | TutoKickSoundTest.kt |
| 2 | mineur | Le KDoc de `catalogue()` dit que la carte « takes the panel's place ». En fait, c'est un bloc dans le panneau, qui le fait grandir. Le reste du KDoc est exact. | TutoKickSoundTest.kt |
| 3 | moyen (design, hors lot) | En Pro sur 360 × 780, la carte du pad n'a pas de place sous le panneau ouvert, même sans la carte du kit. Après « Plus tard », le panneau garde une bande vide de 66 dp, conformément à la règle « hauteur stable tant que l'étape reste ». Ce point est à voir par le chef, il n'est pas corrigé ici. | app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt |

Verdict : 0 bloquant, 0 grave
