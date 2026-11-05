# Tour 3 : intégration

Base `ff9b9c3`, six équipes et le banc en parallèle, index partagé.
## Restes non commités
- `RhythmViewModel.kt`, `AppRoot.kt`, `HumanTester.kt` : c'étaient des morceaux mal placés par l'index partagé
  (rendu d'une partie du remix après la description du projet, résultat d'export lu au mauvais endroit,
  message d'annulation effacé dans « Rétablir », `sendProject` collé entre deux instructions, constantes du banc
  entre `@RunWith` et la classe). Remis à leur place (`5d59c88`).
- `AppRoot.kt.orig` effacé, jamais commité ; 3 captures `padlive_*` gardées (conformes au rendu).
- Item 7 de la zone 1 (coin des pads « Réglages du pad ») et le geste « ● du haut = micro » du banc : déjà commités.

## Corrections demandées
1. Intro figée sur l'émulateur : en mouvement réduit, la pause se compte en images (plus de `delay`) ;
   `Journey.idle` avance l'horloge d'une image. BrandTest vert.
2. `ForegroundSoundTest` : rouge seul après un autre test du même processus. Cause : la lecture du projet
   revenait sur le fil principal et ce retour ne passait jamais. L'ouverture tourne maintenant entièrement
   hors du fil principal, les attentes de sauvegarde aussi (temps réel, 10 s au plus). Vert.
3. `BenchAmeneesTest` : attend la fin des pads prêts et chaque étiquette ; une absente est nommée.
4. Banc : attente bornée par images quand un dialogue avec champ de texte est ouvert (limite de Robolectric),
   « ecris » y insère au curseur ; `Tour3RenameIdleTest` / `Tour3NameFieldIdleTest` réécrits : ils vérifient
   l'appli (nom tapé, « OK », projet renommé), plus de `@Ignore`. Position de lecture du faux moteur qui avance,
   tous les fichiers d'un partage lus, pads du banc qui suivent Live › Pads et `stopLive`.
5. « ♩ 120 » contre 124 : le défi joue au tempo du projet borné à 80–120 (un enfant suit). Le chiffre ne
   s'affiche que s'il est celui de l'écran ; la lumière bat ce qui est joué, TalkBack le dit.
6. `HelpCoverageTest` : `LIVE_GRID` utilisé quand la grille a une colonne Remix. `ScreenScanTest` : vrais « ? »
   sur la carte d'une piste audio, « Envoyer le projet » et « Mes exports » (textes déjà traduits).
   `I18nCatalogTest` : déjà vert (1074 tests de `project-model`, 0 échec).
## Relecture du diff et suite complète
Deux KDoc empilées fusionnées. La suite complète a montré 10 tests rouges : décisions des zones non suivies par
d'anciens tests (pièce sur un pad live vide, compte de la prise pendant l'enregistrement, dossier du kit, nom de
la poignée « Replier », durée réelle de l'extrait du banc), « Pas » simple doublé dans le lexique (`stepFor`), fuzz
bloqué sur une fenêtre « Renommer » (fermée par Retour). Tous corrigés ; 2e passage : 7984 tests, 0 échec hors captures.
## Vérifications
- Captures : 175 ré-enregistrées (44 classes), échantillon regardé (piste audio, partage, défi, guide).
- `project-model` : 1074 tests verts. `assembleRelease` : vert.
- Tests hôte : `master_cpu_budget` rouge par la charge de la machine (2,4 % pour 2 %), aucun code moteur
  touché ce tour ; les deux autres budgets CPU rouges au 1er passage sont verts au 2e.
## Reste
- Toucher la courbe pour poser le curseur coche aussi le bout touché (zone 1, n° 9) : effet de bord à juger.
- Sur téléphone : renommer un projet et une piste (liste de recette), pour confirmer que le gel était le banc.
