# Tour 6 — lot 8 « cablage » : rapport

Base : tête poussée de `ccr-80af1a1b-mn0wec` (854a097c). Fichiers : `ui/UnderTopBar.kt`, `report/ReportWiring.kt`, un test du lot 4 (`CutBoutBigTextTest`) et un test neuf (`Lot8QueuedWiringTest`). Décisions : `docs/decisions/tour-6-cablage.md`.

tests: com.pocketstudio.app.ui.Lot8QueuedWiringTest com.pocketstudio.app.remix.CutBoutBigTextTest *Lot7PlayStripNextTest

## Fait
- **U1, le câblage** (A a #7) : la partie mise en attente par « Jouer depuis… » pendant que le morceau joue arrive maintenant jusqu'à la bande de lecture sous la barre du haut (« Refrain : ensuite ») et jusqu'au rapport de bug (« … · ensuite Refrain »). Rouge ae8fb738, vert 7eafb997 (deux petites fonctions sorties avant, sans changement de comportement : ddf5d728). Le test prouve les deux calculs que l'écran et le rapport appellent ; l'écran lui-même n'est pas rejoué en entier.
- **U13, la preuve** (E c #24) : avec le message « “Voz 1” está numa nova faixa de áudio », à 200 % en portugais, les puces et les mots des quatre boutons d'envoi restent. Le test est rouge sur 0992f9ac (base d'avant le lot 4 : la puce « Bateria » disparaît) et vert depuis le lot 4 : aucun code changé (7233e2dc).
- **R7** : rien de refait ; ce qu'il faudrait changer au prochain tour est dans les décisions (3 lignes).

chemin: Bande de lecture sous la barre du haut › partie en attente | test: Lot8QueuedWiringTest#theStripUnderTheTopBarSaysThePartQueued
chemin: Signaler un bug › rapport › position du morceau | test: Lot8QueuedWiringTest#theBugReportSaysThePartQueued
chemin: Remix › Découper la chanson › Un bout, message affiché (pt-BR, 200 %) | test: CutBoutBigTextTest#theChipsAndTheSendButtonsStayWithTheMessageInPortugueseAtTwiceTheText
