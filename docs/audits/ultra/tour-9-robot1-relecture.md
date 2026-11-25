# Tour 9 : lot robot1 (relecture neuve)
Relu : 27888dbf4

Relecteur : sous-agent neuf, qui n'a pas écrit le code. Périmètre : `app/src/benTest` et `app/src/test/.../bentest` (C8, C11, C10, C5, C2, C3), de 0f4b261c5 à la tête ; rien ne touche les écrans, le moteur, le callback audio ni `project-model`.

## Premier passage (252a1a515) : 0 bloquant, 1 grave
- Grave, corrigé (96797a8d3) : sans bouton « Annuler » (réglage de l'appli), les scénarios d'effets donnaient un faux rouge ; maintenant « non mesuré ».
- Mineurs corrigés : « Effets » et « Trois effets » retirent l'effet avant les contrôles du son ; le texte de la latence nomme l'écran de BenStudio Test.
- Mineurs laissés (notés dans `docs/decisions/tour-9-robot1.md`) : Live strict sur chaque lecture, message des pads « pas à l'écran », oracle de montée par nombre de points, ligne d'écart des indices redondante, tampon arrondi à l'entier.

## Deuxième passage (96797a8d3) : 0 bloquant, 1 grave
- Grave, corrigé (27888dbf4) : le « non mesuré » passait avant les contrôles NaN et silence ; un mauvais son reste maintenant rouge (test : `undoShown = false` et NaN donnent ÉCHOUÉ).

## Troisième passage (27888dbf4)
- Rien de neuf ; mineur de rangement laissé (place de la constante `LAISSE`).
- Suite `com.pocketstudio.bentest.*` : 161 tests, 0 échec (téléphone et tablette compris).

Verdict : 0 bloquant, 0 grave
