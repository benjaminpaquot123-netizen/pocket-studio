# Tour 6 — lot 7 « restes » : rapport

Source : consigne de la discussion principale (après le lot 6) ; défauts laissés par le lot 6 (« Boum boum », T2 hors
fr/en/de, U1 part PlayStrip). Base : tête de `ccr-80af1a1b-mn0wec` (5957fac5). Fichiers : `padlive/Challenge.kt`,
`model/DefaultNames.kt`, `ui/PlayStrip.kt`, une clé neuve dans les 14 `i18n/*/padlive.json`, tests et une capture.

## Corrigé (rouge puis vert)
- **« Boum boum »** (candidat du banc, confirmé au lot 6) : avant et pendant l'essai, le panneau du défi nomme les pads
  à taper, « À taper : A9 · Klaxon » (place et son ; plusieurs pads séparés par des virgules). Une clé neuve
  « À taper : %s », traduite dans les 14 langues (ajoutée seule en fin de chaque `padlive.json`). Capture
  `padlive_challenge` réenregistrée et regardée : la ligne tient ; les pads perdent un peu de hauteur (une ligne de plus
  dans le panneau).
- **T2 hors français, anglais, allemand** : les noms des voix intégrées (`PadVoice.NAMES` : 808, Kick studio, Kick dur,
  Rumble) sont des noms de l'appli dans `DefaultNames` : affichés dans la langue de l'interface (럼블, ランブル), comme
  la consigne du tuto, et reconnus dans n'importe quelle langue. Tous les tests de `project-model` passent.
- **U1, part PlayStrip** : la position porte la partie en attente et la bande dit « Refrain : ensuite » à la place de
  la mesure jusqu'à ce qu'elle arrive (mots déjà traduits).

## Limite
- U1 n'est visible dans l'appli qu'une fois `SessionUi.queued` passé à `playPosition` depuis `ui/AppRoot.kt` (lot 3,
  hors de ce lot) : d'ici là rien ne change à l'écran. Le test prouve la bande et la position, pas le câblage.
- T2 : la part `loadTutoKit` (le pad garde « Rumble ») est au lot 6 ; ce lot rend ce nom traduit.

tests: *Lot7ChallengePadTest *Lot7PlayStripNextTest *PadLiveChallenge* *PlayStrip*

chemin: Pad Live › Défi « Boum boum » › avant l'essai | test: Lot7ChallengePadTest#boumBoumNamesThePadToTapBeforeAndDuringTheTry
chemin: Pad Live › Défi à deux pads | test: Lot7ChallengePadTest#twoPadsAreBothNamed
chemin: Bande de lecture › partie en attente | test: Lot7PlayStripNextTest#theStripSaysThePartComingNext
chemin: Bande de lecture › position calculée | test: Lot7PlayStripNextTest#theQueuedPartIsInThePosition
