# Tour 6 — lot « réparations » : rapport

Deux restes du tour 6, sur la tête d'intégration validée `91192b8a`.
Fichiers touchés : `PianoRoll.kt` seulement côté appli (`RhythmScreen.kt` inchangé), trois tests, une capture.

## 1. Test rouge de l'intégration : `GestureBudgetTest#aStrokeCutShortIsStillItsOwnUndoStep` (attendu 11, obtenu 10)
- **Cause** : le commit `5c72d686` du lot « tutogrille » (U6, « une case touchée ou peinte pendant la lecture coupe
  Suivre »). Depuis lui, un doigt posé sur les cases éteint « Suivre » : la tête de lecture ne tourne plus la page sous
  le doigt, donc elle ne coupe plus le trait. Le test attendait justement que la page tourne et coupe le trait.
- **Preuve** : le bloc de quatre lignes du chemin « peindre » de `5c72d686` retiré (essai, non commité), la page tourne
  de nouveau sous le doigt (la case `step_0` disparaît) ; remis, elle reste et le trait reste ouvert.
- **Verdict** : aucune étape d'annulation n'est perdue. Le trait se ferme quand le doigt se lève, en une étape. C'est
  l'attente du test qui n'était plus vraie, pas l'appli. `RhythmScreen.kt` n'est pas modifié, U6 est gardé.
- **Test adapté, pas affaibli** : il vérifie maintenant en plus que la page reste sur la mesure peinte quand la tête de
  lecture passe à la mesure 2 (U6), puis il coupe le trait par le moyen qui existe encore (un autre doigt sur le numéro
  de mesure « 2 »), et garde toutes ses vérifications d'avant : le trait coupé vaut une étape d'annulation à lui seul,
  l'édition suivante ne s'y fond pas, deux annulations rendent la grille vide.

## 2. Tuto : « plus haut ↑ » alors que les notes sont à l'écran (capture `tuto_app_bass`)
Deux causes, trouvées en traçant le rouleau (traces retirées) :
- **La flèche** s'affichait dès qu'UNE note de la mesure visible était cachée. Jump up : trois notes en pointillé à
  l'écran, la quatrième plus haut, et la ligne disait « Encore 4 notes en pointillé, plus haut ↑ ». Maintenant la
  flèche ne vient que si AUCUNE note de la mesure visible n'est entière au-dessus du panneau (même règle que
  « plus loin → » pour les mesures). Les trois notes posées, la ligne dit « Encore 1 note en pointillé, plus haut ↑ ».
- **Le placement** : « Je le fais » plaçait le rouleau et demandait sa prochaine note pendant que le rouleau était
  encore sous le panneau du tuto (zéro rangée visible). La note finissait sur le bord haut du rouleau, coupée à moitié
  (house, 360 dp : note dessinée de 519 à 549 px, rouleau visible à partir de 534). Maintenant la demande attend qu'il y
  ait des rangées visibles, et le placement est refait une fois quand elles ont fini de bouger (0,2 s), sauf si
  l'utilisateur a fait défiler le rouleau entre-temps.
- Capture `tuto_app_bass` réenregistrée et regardée : les trois notes au milieu du rouleau, « Encore 4 notes en
  pointillé à toucher ».

## Tests du lot 1 gardés verts
`TutoGrilleTest`, `RhythmGrilleTest`, `RollToolsWordsTest`, `TutoScreenTest` : verts. Une seule ligne de `TutoGrilleTest`
change : une note entière visible suffit pour ne pas devoir de flèche (avant : toute note cachée en devait une). C'est
la règle que ce lot change, écrite dans les décisions ; le défaut d'origine (T4 / N4 : notes sous le panneau et « à
toucher ») reste couvert, puisqu'il n'y avait alors aucune note visible.

## Laissé de côté
- La capture `folded_cards` diffère de sa référence sur cette base (déjà notée par le lot « tutoconsignes » comme
  venant de l'intégration) : hors des deux fichiers, non réenregistrée.
- Quand aucune note à poser n'est dans la mesure visible et qu'elles sont aussi plus haut, la ligne dit « plus haut ↑ »
  avant « plus loin → » (ordre des textes dans `TutoPanel.kt`, hors lot).
- La mesure des rangées visibles reste lue en composition (moyen ouvert de la relecture du lot 1) ; les rangées
  « stables » ajoutent une recomposition du rouleau chaque fois qu'elles se stabilisent à une nouvelle valeur (0,2 s
  après un mouvement de la page), tuto ou non.
- Les rangées ne sont attendues « stables » qu'en sortant de sous le panneau (zéro rangée visible). Une étape qui
  commence pendant que le panneau change de hauteur place encore le rouleau sur les rangées du moment, comme avant.
- Vu par la relecture dans `RhythmScreen.kt` (commit `5c72d686`, hors de ce lot) : un trait peint à l'arrêt éteint
  « Suivre », une case touchée à l'arrêt non. À trancher avec le lot « tutogrille ».

## Vérifié
- Tests ciblés : 63 tests verts (les six classes de la ligne « tests: »), dont `GestureBudgetTest` en entier.
- Rouge constaté avant correction, sur l'assertion : les quatre tests de `TutoSightHintTest`. Jump up (100 et 130 %)
  rougissait sur la flèche (« a dotted note is in sight, no arrow expected ») ; house (100 et 130 %) sur le placement
  (« the roll opens with a dotted note in sight » : la note 45 du premier temps, celle que « Je le fais » demande,
  dessinée de 519 à 549 px pour un rouleau visible à partir de 534). Après la relecture, cette assertion nomme la note
  demandée elle-même.
- Non couvert par un test : le second placement abandonné quand l'utilisateur a fait défiler le rouleau, et la demande
  en attente servie une seule fois (lus par la relecture, sans défaut trouvé).
- Captures du rouleau et du tuto vérifiées (165 tests) : seule `tuto_app_bass` change à cause du lot.
- Self-test du banc (`scripts/bench/selftest.sh`, lancé comme le portail le lance, par `scripts/bench/rejeu-defauts.sh`) : 0 rouge ; rejeu `t4-26-prise-clavier : oracle tenu`.

tests: com.pocketstudio.app.ui.GestureBudgetTest com.pocketstudio.app.tuto.TutoSightHintTest com.pocketstudio.app.tuto.TutoGrilleTest com.pocketstudio.app.rhythm.RhythmGrilleTest com.pocketstudio.app.synth.RollToolsWordsTest com.pocketstudio.app.tuto.TutoScreenTest

chemin: Rythme / Sons › trait peint pendant la lecture | test: GestureBudgetTest#aStrokeCutShortIsStillItsOwnUndoStep
chemin: Tuto jump up › basse › ligne du rouleau (100 %) | test: TutoSightHintTest#jumpUpBassTheArrowIsOnlyForNotesOutOfSight
chemin: Tuto jump up › basse › ligne du rouleau (130 %) | test: TutoSightHintTest#jumpUpBassTheArrowIsOnlyForNotesOutOfSightAt130
chemin: Tuto house › basse › rouleau après « Je le fais » (100 %) | test: TutoSightHintTest#houseBassTheArrowIsOnlyForNotesOutOfSight
chemin: Tuto house › basse › rouleau après « Je le fais » (130 %) | test: TutoSightHintTest#houseBassTheArrowIsOnlyForNotesOutOfSightAt130
chemin: Tuto › étape de notes › piano roll (Pro) | test: TutoGrilleTest#proBassNotesAreAboveThePanelOrPointedAt
chemin: Tuto › étape de notes › piano roll (Simple) | test: TutoGrilleTest#simpleBassNotesAreAboveThePanelOrPointedAt
