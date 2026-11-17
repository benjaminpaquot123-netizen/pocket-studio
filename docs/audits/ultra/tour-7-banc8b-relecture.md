# Relecture du lot « banc8b » (tour 7)
Relu : 2895ce102
Portée : `git diff 108b200b7..2895ce102` (HumanTester.kt, ScreenText.kt, BenchAmeneesTest.kt, BenchScreenTextOverlayTest.kt, scripts/human-act.py, scripts/bench/test_ultra_bench.py, rapport et décisions).
Relecteur : l'agent du lot lui-même, pas un sous-agent neuf (aucun outil pour en lancer un dans cette discussion) ; le chef peut la faire refaire par une discussion neuve.

## Points lus
- Défaut 1 (amenée tuto) : le garde de chapitre échoue en clair au lieu de jouer un autre chapitre, il ne masque rien. `drawnAt` retombe sur « dessiné » si la lecture échoue (comportement d'avant). Le défilement systématique ajoute un « attends 0.5 » au journal ; les rejeux restent tenus.
- Défaut 2 (texte sous l'anneau) : l'exception est limitée à deux étiquettes nommées (`tuto_spot_ring`, `tuto_finger`), qui sont dans l'appli des Canvas qui laissent passer les touchers. Une surcouche plein écran inconnue cache toujours ce qu'elle couvre, ce qui est vérifié par le témoin `aPageDrawnOverTheScreenStillHidesWhatIsUnder`. Aucun défaut d'appli n'est masqué.
- Défaut 3 (image du dernier écran) : elle n'est permise que pour l'écran du dernier tour et seulement une fois le siège fini. Le test remet `images.txt` à deux images avant chaque cas, donc aucun « déjà regardé » ne le fait passer par erreur.
- Tests : les trois rouges ont été prouvés rouges sur l'ancien code, avec le bon message d'échec. Le test du doigt et le témoin de la page ne l'ont pas été (même mécanisme que l'anneau).

## Mineur (noté, non corrigé)
- `reachTag` : prenons un élément dont le milieu est au centre de l'écran mais hors de sa partie dessinée, et que `performScrollTo` ne peut pas faire défiler. Le geste de secours glisse alors de presque rien, huit fois, puis touche ce milieu, comme avant. Cela n'arrive pas dans les cas vus.

Verdict : 0 bloquant, 0 grave
