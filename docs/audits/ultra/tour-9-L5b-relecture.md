# Tour 9, lot L5b : relecture neuve (fiche 6)
Relu : c0fa9174f834c8ecf0931b188da9541a13772e52

Relecteur : sous-agent neuf, qui n'a pas écrit le lot. Diff relu : `origin/ccr-80af1a1b-mn0wec..HEAD` (b81c56d3..c0fa9174), le même que c79d3442..d61d3f94 : 92 fichiers.

Contrôles
1. Traductions : clés et virgules gardées, JSON valide, aucune clé perdue ni vide, marqueurs %n$s et {x} identiques à la source.
2. Seules les clés des défauts du lot sont touchées, dans les fichiers permis (hi/ar tuto.json et tr/model.json : voir docs/decisions/tour-9-L5b.md).
3. Tour9L5bTutoWordsTest : 13 tests, un par défaut ; chacun échouerait sur l'ancien texte ; aucun @Ignore. Tour9L5bTutoCapturesTest : rien à redire.
4. corrections.tsv : 13 « tenté » avec leur sha, 8 « pas pris », aucun « corrigé », 12 colonnes partout.
5. Aucun identifiant de modèle dans les commits.

Bloquant : aucun. Grave : aucun.

Mineurs (laissés pour la suite, hors des phrases d'acceptation du lot)
- F068 (de) : environ 35 « Wirbel » restent ailleurs dans les tutos alors que le bouton dit « Roll ».
- F068 (pt-BR) : « depois toque em “×4” em “Rufo” » se lit mal (le second « em » était là avant).
- F075 (ar) : le mot général « المزج » reste à côté de l'onglet « المكس », titre de chapitre compris.
- F076 (fr source) : le titre dit encore « Ta première partie : le refrain » alors que la partie s'appelle « Drop ».
- F073 (hi) : la phrase dit ce qui est enlevé, pas ce qui est gardé ; l'ambiguïté du siège disparaît.
- F088 (ar) : « التصفيق (تصفيقة يد) » répète la même idée ; « ريف » reste dans ar/extract.json (hors lot, noté).
- F090 : la source fr et 12 langues disent encore « croix rouge » (noté).
- F094 (zh) : seule l'étape Rim de techno change, comme le siège t9-68 l'a vu.

Verdict : 0 bloquant, 0 grave
