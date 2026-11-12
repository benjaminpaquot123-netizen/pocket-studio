# Relecture neuve du lot « fuzz » (tour 5) : GestureFuzzTest rouge en CI 43229551237
Relu : a3621ddc23443158bb6d56611441f30ef2286cb4

Lu : `git diff db66a3ce..a3621ddc` (96083cfd rouge, b934b4c2 vert, a3621ddc docs). Quatre fichiers, tous dans la zone permise : `GestureFuzzTest.kt`, `BenchWindows.kt` (sous `app/src/test`), le rapport et le fichier de décisions du lot. Ni lien symbolique, ni image, ni `app/src/benTest`, `app/src/journey`, `scripts/bench/` ou rejeu touché. Aucune ligne de l'appli ne change.

rouge constaté : b934b4c2 : rejoué sur 96083cfd : `GestureFuzzTest#aTextWindowUnderAPopupIsClosedWithRetour` échoue sur son assertion, `AssertionError: the window with the text field is still open` (GestureFuzzTest.kt:254) ; le même test est vert sur a3621ddc.

## Bloquant
Aucun.

## Grave
Aucun.

## Moyen
Aucun.

## Mineur
1. Le correctif est juste et n'affaiblit pas le doigt : avant, Retour partait à la première fenêtre au-dessus de l'écran, quelle qu'elle soit (la bulle) ; maintenant il part à la fenêtre qui tient le champ. Même nombre de tours (3), mêmes graines, mêmes gestes, mêmes invariants. Le seul cas changé : si `BenchWindows.windows()` lève, l'ancien `closeTextWindows()` laissait remonter l'exception (le test tombait sur elle) ; le nouveau `runCatching { textFieldDialog() }.getOrNull() ?: return` se tait, et un dialogue à champ resté ouvert finirait en `AppNotIdleException` 60 s plus tard, moins lisible. Le repli `lastFieldWindow` de `textFieldInADialog()` n'a jamais servi à ce chemin (l'ancien code appelait `windows()` d'abord) : aucun cas de détection perdu. `windows()` ne lit que `WindowManagerGlobal` par réflexion ; je ne l'ai jamais vu lever.
2. `textFieldInADialog()` garde exactement sa réponse (même lecture, même repli quand `windows()` lève, même mise à jour de `lastFieldWindow`) : son autre utilisateur, `HumanTester.kt:1592`, ne change pas de comportement.
3. Le test rouge prouve le bon point (Retour atteint la fenêtre du champ sous une bulle), pas le gel lui-même : sur 96083cfd l'attente revient (le champ n'a pas le curseur), c'est la seconde assertion qui rougit. Sa première assertion (« the wait never came back ») ne rougit donc pas sans le correctif ; elle reste un garde-fou, pas une preuve. Le rapport le dit correctement (« échoue sur « the window with the text field is still open » »).
4. Rapport, une phrase plus sûre que sa preuve : « les lots mixwake, testapk et oreille l'ont déplacé vers un bouton « Garder ce son » » est une explication plausible, non montrée. Le titre « La cause, prouvée » repose sur une sonde biaisée qui reproduit le blocage avec une bulle au-dessus du dialogue, pas sur les fenêtres de la CI elle-même ; la section « Ce dont je ne suis pas sûr » tempère assez. L'écart de mixwake est bien appuyé (sonde isolée et note du 10 novembre antérieure à `Changes.kt`).
5. Si le test neuf échoue, l'activité et ses fenêtres ne sont pas détruites (`controller.destroy()` est après les assertions) et restent pour les classes suivantes de la même JVM. Sans effet quand il est vert.

Non rejoué ici : `aRandomFingerNeverCrashesTheApp` et `anotherRandomFingerNeverCrashesTheApp`, `PlayingRecomposeTest`, `ClipHoldTest`, `Zone3DialogDiagTest` (annoncés verts sur b934b4c2 par l'auteur ; la CI tranchera) ; la suite complète non plus.

Verdict : 0 bloquant, 0 grave — fusion possible
