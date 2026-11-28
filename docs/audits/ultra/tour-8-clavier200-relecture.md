relu : ce47be95

- léger : app/src/main/java/com/pocketstudio/app/synth/SynthScreen.kt:936-954 — le rétrécissement du nom de touche est dans `Key`, partagé par tous les claviers (Mode Pro, paysage, tablette, clavier flottant). À 100 % rien ne bouge (le « Do » tient, la boucle ne tourne pas), mais en Mode Pro ou en paysage à 200 % le « Do » qui était coupé devient plus petit au lieu d'être coupé : petit écart à la règle « Pro / paysage inchangés », sans risque (c'est une amélioration, et aucun texte n'est changé).
- léger : SynthScreen.kt:944 — à 200 % sur une touche étroite le nom peut descendre jusqu'à environ 6 sp : il tient, mais il est moins lisible que ce que la personne a demandé avec sa grande police. Acceptable pour une étiquette d'aide sur la touche ; à surveiller dans la grille « œil neuf ».
- léger : SynthScreen.kt:425 et 433-434 — `recordTall` vaut 0 à la première image : les touches s'affichent d'abord à 150 dp puis se tassent à environ 122 dp une image plus tard (un petit saut à l'ouverture du Clavier à 200 %). Pas de boucle : la hauteur de la carte de prise ne dépend pas de celle des touches. Si `record` devenait nul en cours de route, `recordTall` resterait à l'ancienne valeur (touches un peu plus basses que possible, jamais sous 110 dp).
- léger : SynthScreen.kt:434 — le calcul de la place ne retire pas la barre des pistes (`TrackBar`) ; aujourd'hui le Clavier reçoit `bar = false` (AppRoot.kt:3245), donc sans effet ; à reprendre si cette barre y revient.
- léger : app/screenshots/scale130/fr_record_bar.png — capture hors des noms `synth*` / `*keyboard*` changée (le « Do » rétrécit), déjà signalée au chef ; le changement vient bien du nom de touche, pas d'un texte.

Vérifié sans remarque :
- condition `simple && !tabletUpright && fontScale >= 1.15` (SynthScreen.kt:424), dans la branche « debout, sans Notes » seulement (le paysage est pris avant, l. 370) : Mode Pro, 100 %, tablette et paysage gardent l'ancien ordre et 150 / 190 dp ; 115 % (« grand ») est aussi concerné, ce qui est voulu et écrit dans le rapport ;
- boucle de mesure : `remember(label, room, base)`, recalculée seulement si la largeur ou le nom change ; au plus ~7 tours (×0,9 jusqu'à 6 sp) ; largeur finie (la touche a une largeur fixe) ; largeur 0 ou taille non précisée (NaN) : la boucle s'arrête, pas de plantage ;
- `keysHigh` borné 110–150 dp, jamais négatif ; `trackKeyboard` déplacé sur les touches, sans effet ici (`follows` est faux en Mode simple sans Notes) ;
- étiquettes gardées : `key_62`, `key_65` (PlayKeys), `record_start` (RecordBar, racine unique dans la Box) ; aucune phrase française ajoutée ou changée ;
- fichiers : seuls SynthScreen.kt, le nouveau test, deux captures scale130, et les docs du lot ; pas de fil audio touché ;
- le test Tour8KeyboardBigFontTest lit la vraie position des touches 62, 65 et du bouton de prise contre la barre du Guide (entiers, sans défiler) ; le commit rouge 9e195d6c le fait échouer sur la base (130 % coupé, 200 % hors écran), 100 % et tablet8 servent de témoins.

Verdict : 0 bloquant, 0 grave
