# Tour 6 — lot 1 « tuto-grille » : rapport

Source : `docs/audits/ultra/sieges/tour-6/lots-correction.md` (lot 1), `verdicts.tsv` (T4, N4, N5, T11, N8, T15, U6,
R21, T16) et `lecture-recurrents.md` (complément 2 : T4 et N4 confirmés par B b-suite, T11 par B b-suite et D a-suite).
Base : tête de `ccr-80af1a1b-mn0wec` + lots « langues » (`ccr-6f42e003-295715`) et « gaucher » (`ccr-4cc0bb93-xljmti`).
Fichiers touchés : `PianoRoll.kt`, `RhythmScreen.kt` seulement (plus les tests et 8 captures).

## Corrigé (rouge puis vert)
- **T4 / N4 (graves)** : le piano roll sait quelles rangées sont vraiment visibles (ni coupées par la page, ni sous le
  panneau du tuto : `LocalSpots.coverTop`). Les notes en pointillé d'une étape y sont placées au milieu ; un toucher sur
  la ligne du tuto y amène la suivante ; la ligne dit ↑ / ↓ quand une note de la mesure visible n'est pas entière
  au-dessus du panneau. Mesuré : téléphone 360 dp à 130 %, il reste 129 px de rangées (615 → 744) ; avant, les basses
  à 825 px (sous le panneau) et la ligne disait « à toucher ».
- **N5 (grave)** : la ligne nomme la mesure de la prochaine note en pointillé quand elle est visible (avant :
  « mesure 3/8 » pour une note de la mesure 2).
- **T11** : une étape qui commence ouvre le piano roll sur la mesure de sa première note (seulement pendant un tuto) et
  la grille de pas sur la mesure de sa première case, même si la mesure laissée par l'étape d'avant en contient.
- **N8** : « Accords » allumé, « Encore N » compte les touches (les accords), pas leurs notes (16 → 4 en house).
- **U6** : une case touchée (ou peinte) pendant la lecture coupe « Suivre » sur la mesure visible : le coup posé reste
  à l'écran.
- **R21** : les pastilles « FX » et « G 30 / D 45 » disent ce qu'elles sont (« Effets du pad », « Gauche 30 »), avec
  des textes déjà traduits dans les 14 langues.
- **T16 (part piano roll), v2** : « Basse en fantôme » vient juste après « Accords » dans la rangée d'outils, entière
  à l'écran (360 et 392 dp). La v1 faisait passer la rangée à la ligne : la ligne de plus poussait la grille hors de
  l'écran d'un téléphone en Simple, et le portail l'a refusée (self-test : `BenchTour1Test#notesAddedInThePianoRollAreNotNothingChanges`,
  les touchers tombaient au-dessus d'une grille invisible). La rangée défile de nouveau ; 8 captures réenregistrées et
  regardées.

## Déjà tenu sur cette base (test garde-fou, sans rouge)
- **T4 (pads) / T15** : à 360 dp et 130 %, panneau ouvert, les deux rangées de cases sont au-dessus du panneau et
  l'aide des pads est visible ou défilée hors de vue, jamais coupée par lui (Pro et Simple).

## Laissé de côté
- Rien des 9 défauts. Reste, hors lot : « En rythme 50 % », « 100 % » et « Effacer » sont au bout de la rangée qui défile
  (famille R19). Vu en passant : la 1re rangée d'outils du piano roll (« Tout v… ») défile encore au
  bord (famille R19) ; T16 pour « Plaits » est dans `TrackSheet.kt` (pas dans ce lot).
- Le piano roll garde ~130 px de rangées à 130 % sur 360 dp : on y voit 4 rangées ; la flèche dit où aller, mais
  agrandir le rouleau demanderait de changer la mise en page de l'écran (hors des deux fichiers).

## Écarts au mot près
- T11 (piano roll) : « la mesure que la consigne nomme » est lue comme la mesure de la première note en pointillé.
- Les tests s'appuient sur la mise en page de Robolectric (pas sur un vrai téléphone) ; T16 est mesuré en rendu natif.

tests: com.pocketstudio.app.tuto.TutoGrilleTest com.pocketstudio.app.rhythm.RhythmGrilleTest com.pocketstudio.app.synth.RollToolsWordsTest com.pocketstudio.app.tuto.TutoScreenTest

chemin: Tuto › étape de notes › piano roll (Pro) | test: TutoGrilleTest#proBassNotesAreAboveThePanelOrPointedAt
chemin: Tuto › étape de notes › piano roll (Simple) | test: TutoGrilleTest#simpleBassNotesAreAboveThePanelOrPointedAt
chemin: Tuto › dernière note de la mélodie › ligne « mesure » (Pro) | test: TutoGrilleTest#proTheLastLeadNoteIsShownWithItsBar
chemin: Tuto › dernière note de la mélodie › ligne « mesure » (Simple) | test: TutoGrilleTest#simpleTheLastLeadNoteIsShownWithItsBar
chemin: Tuto › accords › « Encore N » (Pro) | test: TutoGrilleTest#proChordsLeftCountTheTouches
chemin: Tuto › accords › « Encore N » (Simple) | test: TutoGrilleTest#simpleChordsLeftCountTheTouches
chemin: Tuto › étape qui commence › piano roll (Pro) | test: TutoGrilleTest#proANotesStepOpensOnTheBarOfItsFirstNote
chemin: Tuto › étape qui commence › piano roll (Simple) | test: TutoGrilleTest#simpleANotesStepOpensOnTheBarOfItsFirstNote
chemin: Tuto › étape qui commence › grille de pas | test: RhythmGrilleTest#aStepThatBeginsShowsTheBarOfItsFirstCell
chemin: Tuto › grille de pas au-dessus du panneau (Pro) | test: TutoGrilleTest#proTheCellsAreAboveThePanel
chemin: Tuto › grille de pas au-dessus du panneau (Simple) | test: TutoGrilleTest#simpleTheCellsAreAboveThePanel
chemin: Rythme / Sons › case touchée pendant la lecture | test: RhythmGrilleTest#aCellTouchedWhilePlayingKeepsItsBar
chemin: Rythme / Sons › pastille d'un pad | test: RhythmGrilleTest#aPadsPanBadgeSaysWhatItIs
chemin: Clavier / Notes › rangée d'outils (petit téléphone) | test: RollToolsWordsTest#theBassAsAGhostChipIsWholeOnASmallPhone
chemin: Clavier / Notes › rangée d'outils (téléphone) | test: RollToolsWordsTest#theBassAsAGhostChipIsWholeOnAPhone
