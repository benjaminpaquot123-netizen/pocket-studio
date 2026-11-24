# Relecture neuve : lot fable6 (tour 9)

Relu : a31f7f9c9

Relecteur : sous-agent neuf, n'a pas écrit le code. Diff relu : `git diff a1a447c90 a6b936b1e`, puis `git diff a6b936b1e a31f7f9c9` (test durci après sa première remarque). Gradle non lancé par le relecteur : le rouge, le vert et les chiffres du rapport ne sont pas revérifiés par lui.

- `TutoRollHint` (`along`, `atBar`, `brought`, `folded`, `lit`) : les rangées sont jugées sur la vue amenée à la mesure, sur toutes les notes en pointillé de la mesure ; pas de plantage neuf, pas d'allocation dans le thread audio (code d'écran seulement).
- `PianoRoll.kt` : le défilement vers la mesure passe avant le pliage ; un toucher sur la ligne suffit.
- Tests : le test du toucher échouait avant le correctif (7 cas sur 43) ; les deux balayages du modèle sont verts sur la base, comme annoncé.

Moyens (notés, pas bloquants) :
- En mode accords, le cas « rien touché » compte maintenant aussi le haut des accords ; aucun test ne le fixe.
- Un cas sauté (ligne entière à l'arrivée) ne vérifiait pas que l'étape est cochée : réglé dans a31f7f9c9 (`TutoPlay.done` exigé avant le saut).

Mineurs :
- `folding` est posé avant l'attente de `hScroll.scrollTo`.
- `at.left` peut dépasser `hScroll.maxValue` à la dernière mesure (sans effet : borné par le défilement).
- Écriture de droite à gauche non couverte.
- L'assertion `touches <= total` ne pouvait pas échouer : retirée dans a31f7f9c9.
- Le test ne force pas le mode accords et ne vérifie pas la place de l'anneau « Touche ici ».
- Test joué sur 4 styles sur 15 (`PS_FABLE6_STYLES=all` pour tous, plus de 20 minutes).

Verdict : 0 bloquant, 0 grave — fusion possible
