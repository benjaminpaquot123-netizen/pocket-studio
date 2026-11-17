# Relecture du lot « banc8b » (tour 7)
Relu : 2895ce102
Portée : `git diff 108b200b7..2895ce102` (HumanTester.kt, ScreenText.kt, BenchAmeneesTest.kt, BenchScreenTextOverlayTest.kt, scripts/human-act.py, scripts/bench/test_ultra_bench.py, rapport et décisions).
Relecteur : un sous-agent neuf, qui n'a pas écrit le code (remplace l'auto-relecture du commit 27ebab671). Tests relancés par lui : BenchScreenTextOverlayTest, BenchAmeneesTest, test_ultra_bench.py, tous verts ; relancés aussi après la fusion de l'intégration (5aecbbeda), toujours verts.

## Défauts
- Siège c, chapitre 1 au lieu du 9 (`HumanTester.kt:901-922`) : vraie cause visée (ligne coupée par le bord de la liste, milieu caché touché sur « Recommencer le tuto »). Le garde de chapitre (`HumanTester.kt:848-851`) fait échouer l'amenée en clair. Rien de masqué côté appli.
- Siège d, texte d'écran vide (`ScreenText.kt`) : `TutoSpotRing` et `TutoFinger` sont de simples dessins sans toucher ; liste fermée de deux noms, toute autre surcouche plein écran cache toujours (test Pad Live). Aucun défaut de l'appli masqué.
- Siège e, image du dernier écran (`human-act.py:132`) : accordée seulement pour le dernier écran, siège fini ; pas de contournement du plafond.
- Sécurité : aucun binaire, image ni lien symbolique ; aucun identifiant de modèle.

## Remarques (non bloquantes, laissées)
- moyen · `HumanTester.kt:906-912` : cible au milieu non dessiné sans conteneur défilable : 8 glissés verticaux pour rien (environ 12 s), puis toucher du milieu caché ; mieux : viser le centre de la partie dessinée.
- mineur · `HumanTester.kt:904` : défilement et « attends 0.5 » pour toute cible pas entièrement visible ; d'anciens rejeux peuvent ne plus coller.
- mineur · `HumanTester.kt:919` : `drawnAt` renvoie « vrai » en cas d'échec, sans le signaler.
- mineur · `ScreenText.kt:118-128` : le nouveau KDoc s'intercale entre celui de `mark()` et `mark()`.
- mineur · `human-act.py:132` : `n == turn(seat)` toujours vrai, sans effet.
- mineur · `BenchAmeneesTest.kt:75` : un seul cas couvert (ko, phone360, 130 %, chapitre 9).

Verdict : 0 bloquant, 0 grave
