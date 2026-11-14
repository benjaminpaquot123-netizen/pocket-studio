# Tour 6, lot « padlive2 » : Pad Live (phrases du contrôle A b)

Branche `eq/tour-6-padlive2`, partie de `ccr-80af1a1b-mn0wec` (bdb777a9). Zone : `app/…/padlive/` et ses tests (plus une ligne d'un test du banc, `BenchAmeneesTest`, qui suivait l'ancien ▶). Décisions : `docs/decisions/tour-6-padlive2.md`. Source : `sieges/tour-6/lotA/b/rapport.md` et le contrôle `controle/Ab` (branche `ccr-a548b78b-controle-ab`, code b3617da2).

| Phrase | État | Cause | Correctif | Preuve (rouge / vert) |
|---|---|---|---|---|
| P1 « Notes » montre tout ce que je joue | pas pris | `synth/SynthScreen.kt` (puce « Notes », l. 667) ouvre la grille sur la mesure 1 sans cadrer les notes ; cadrage « Tout voir » dans `synth/PianoRoll.kt` (zone tuto) | — (lot des fichiers partagés) | — |
| P2 rien ne joue à l'ouverture | preuve rejouée | La carte de l'accueil demandait le « coup de bienvenue » (`PadLiveController.welcome`, décision du 2026-11-03) | Plus de coup à l'ouverture ; premier son à 2 touchers | 9102528f / 275d62c8 (`PadLiveSilentOpenTest`) ; banc : écrans 5-6 « Son : rien ne joue. » |
| P3 une 2e prise courte garde la longueur | preuve rejouée | L'appli, pas le banc : la prise de Pad Live suivait le choix par défaut de la page Live, « Nouvelle version » (`writeTake` remplace le morceau) | Dans Pad Live, une prise après une prise déjà écrite va à la suite ; la première remplace le morceau vide | 9e25d659 / 0e027730 (`PadLiveTakeAfterTest`) ; banc : « Live … 9 mes. · 0:17 · 2 parties » (écran 23) |
| P4 le ▶ du haut montre l'état réel | preuve rejouée | Le ▶ ne lisait que le transport, pas les pads lancés seuls | ■ dès qu'un pad sonne ou attend ; un toucher : ■ du transport, sinon « Tout couper » ; pareil dans « Fête » | 9601fd80 / bf73dc50 (`PadLivePlayShowsTest`) ; banc : écrans 10-11 |
| P5 le projet Live rouvre sur Pad Live | pas pris | `ui/AppRoot.kt` : après `switchProject` (reconstruction de l'activité), seul « Continuer » met `showPadLive` (l. 1588) ; `OpeningTab.of` ne connaît pas Pad Live | — (lot des fichiers partagés) | banc : écran 24, rouvert sur « Mon morceau » |

Preuve rejouée : siège `sieges/tour-6/padlive2/lotP/a` (fr, téléphone, 100 %, Simple, mission padlive), joué sur ee4c2a92 par le correcteur du lot, appli entière par le banc, selftest vert (176 verts, 0 rouge). Ce n'est pas le contrôle neuf : un testeur neuf rejoue la branche avant le portail.

## Aller-retour 1 (contrôle Ab-2, code ee4c2a92, branche `ccr-a548b78b-controle-ab2`)

- P2 : corrigé selon le siège (écran 9).
- P3, autre chemin : le siège a enregistré par « Enregistrer ce que je joue » puis « Dans une brique du Live » (pas le REC du haut, celui rejoué ici). Les prises vont dans des briques audio ; le projet reste « Boucle de 1 mes. », lecture « A mesure 1/1 ». Cause : la longueur de la boucle ne compte pas les briques audio, `project-model/…/Scenes.kt` `Scene.stepsFor` (batterie, 303, notes seulement) lu par `SongLength.of` (`project-model/…/SongLength.kt`) et par le transport. **Pas pris** (modèle partagé, hors des fichiers du lot). Le chemin REC reste prouvé (écran 23 du siège lotP/a).
- P4 dans « Plus » : `live/LiveScreen.kt` (Briques, écran 41) tenté : ■ quand les boucles des pads jouent seules, un toucher : « Tout couper » (rouge / vert `PadLivePlayShowsTest#theBricksPageSaysTheSame`). La vue plein écran « Pads » (écran 45, étiquette `live_pads_full_play`) est dans `session/LivePadsFullScreen.kt` (`StageButton` l. 405, lit `playing` seul) : **pas pris** (hors des fichiers du lot).
- Anneau pâle (gestes 21-23) : voulu. Un pad de boucle touché transport arrêté part tout de suite (décision du 2026-11-11, T5-PADRETRIG) ; le 2e et le 3e attendent leur ligne, mais un geste du banc dure plus que la ligne (124 BPM) : le siège Ab l'avait vu à son écran 20. Le banc, pas l'appli.

## Aller-retour 2 (contrôle Ab-3, code ffbcb886, branche `ccr-a548b78b-controle-ab3`)

Le chef a accordé `session/LivePadsFullScreen.kt` et la longueur affichée (`project-model`) à ce lot.
- P2 : corrigé selon le siège (écrans 7 et 31).
- P4 vue « Pads » plein écran (écrans 20, 24) : tenté. Le ▶ de `StageBar` lisait `playing` seul ; il dit ■ quand les boucles des pads jouent seules et un toucher les coupe (« Tout couper »), sans lancer la musique du projet (rouge / vert `PadLivePlayShowsTest#thePadsFullScreenSaysTheSame`). Briques : déjà tenté à l'aller-retour 1 (le siège Ab-3 jouait ffbcb886, d'avant).
- P3 par les briques (écrans 30 et 39, « Boucle de 1 mes. ») : tenté dans `SongLength.of` (`project-model/…/SongLength.kt`), pas dans `Scene.stepsFor` (celle-là sert au transport, à l'export et au mode morceau, et ne voit pas les pistes audio) : sans morceau, la longueur montrée est la plus longue du contenu de la scène et de ses briques audio (clips de la scène affichée, hors clips du morceau). Affichage seul : carte d'accueil et « Mes projets ». Suite `project-model` entière verte ; tests export, projets, accueil, morceau de l'appli verts (un test du remix, `RemixSongEverywhereTest`, rouge une fois en charge, vert seul : sans lien). Rouge / vert `SongLengthBricksTest#aLoopLastsAsLongAsItsLongestBrick`.
- Condition du chef (la longueur affichée doit être celle qu'on entend) : vérifiée par lecture du moteur, pas à l'oreille. Une brique audio part de `local = step - slot.start` (`engine/…/core/Engine.h` `localOf`, `AudioTracks.cpp` `onStep`) : `step` est le pas du transport, qui ne revient pas à 0 à chaque tour de la scène, et `slot.start` n'est remis qu'à un changement de scène (`Engine.cpp` `swapScene`, `resetSlots`), que l'appli ne demande qu'en changeant de scène ou de partie (`RhythmViewModel.stageScene`), pas à chaque tour d'une scène jouée seule. La brique rejoue donc tous les `loopSteps` (8 mesures pour une prise de 6 : `LiveCells.loopBars` arrondit à 1, 2, 4, 8…) pendant que la scène de 1 mesure tourne : on entend 8 mesures, l'étiquette « Boucle de 8 mes. » dit vrai. Ce qui ment, c'est le compteur « A mesure 1/1 » du transport (donné à coquille2).
- Reste hors du lot : « A mesure 1/1 » de Briques (transport, `RhythmViewModel`), « Ton morceau est vide » alors que les prises sont listées (`session/SessionScreen.kt`).

## Contrôle Ab-4 (siège neuf, code 82cf7318)

controle: Ab-4 | code testé: 82cf7318 | bloqués: 0 | graves neufs: 0

P2, P3 et P4 constatés corrigés par le siège (accueil écran 27 et Mes projets écran 28 « Boucle de 8 mes. » après 7 + 2 mesures ; Briques écran 20, Pads écran 23). Rapport du siège : `origin/ccr-a548b78b-controle-ab4` (ea8b6158), copie texte `sieges/tour-6/padlive2/controle-Ab-4-rapport.md`.

Relecture neuve (`tour-6-padlive2-relecture.md`) : 0 bloquant, 0 grave. Moyen pris : un « coup » qui sonne ne fait plus passer le ▶ en ■ (rouge / vert `PadLivePlayShowsTest#aHitRingingKeepsPlay`). Moyens laissés, hors des fichiers du lot : le bouton « Record » d'un contrôleur MIDI (`midi/AutoKnobsVm.kt`, appelle `togglePerformance` sans passer par `PadLiveController.record`) remplace encore le morceau ; morceau plein ou au-delà de la mesure des effets, le message de `RhythmViewModel` parle de « Nouvelle version », absent de Pad Live.

Restent hors du lot : la carte « Pad Live » de l'accueil dit encore « Ça sonne tout de suite. » (`onboarding/`, textes) ; l'effet de bord de P3 (le choix « Nouvelle version / Ajouter à la suite » de la page Live suit la dernière prise de Pad Live).

Juge du chantier : 109 écarts (tête : 110).

tests: PadLivePlayShowsTest PadLiveSilentOpenTest PadLiveTakeAfterTest PadLiveTouchCountTest PadLiveSaveUndoTest BenchAmeneesTest LivePadsFullScreenTest SongLengthBricksTest

## Liste des chemins

- chemin: Pad Live bouton ▶ du haut avec des boucles seules | test: PadLivePlayShowsTest#loopsOnTheirOwnShowTheSquareAndOneTouchStopsThem
- chemin: Pad Live bouton ▶ du haut avec un pad en attente | test: PadLivePlayShowsTest#aPadWaitingForItsLineAlreadyShowsTheSquare
- chemin: Pad Live bouton ▶ du haut pendant qu'un coup sonne | test: PadLivePlayShowsTest#aHitRingingKeepsPlay
- chemin: Plus › Briques bouton ▶ du haut avec des boucles seules | test: PadLivePlayShowsTest#theBricksPageSaysTheSame
- chemin: Plus › Briques › Pads plein écran bouton ▶ avec des boucles seules | test: PadLivePlayShowsTest#thePadsFullScreenSaysTheSame
- chemin: Pad Live plein écran « Fête » bouton ▶ | test: PadLivePlayShowsTest#thePartyScreenSaysTheSame
- chemin: Pad Live ■ sur le banc (moteur, pads éteints) | test: BenchAmeneesTest#padLiveLightsStopsAndRecordsAsOnAPhone
- chemin: accueil carte « Pad Live » ouverture | test: PadLiveSilentOpenTest#openingFromTheCardPlaysNothing
- chemin: accueil carte « Pad Live » puis premier pad | test: PadLiveTouchCountTest#theFirstSoundIsTwoTouchesAway
- chemin: Pad Live REC deuxième prise | test: PadLiveTakeAfterTest#aSecondTakeGoesAfterTheFirst
- chemin: Pad Live REC première prise d'un projet Live neuf | test: PadLiveTakeAfterTest#theFirstTakeStillReplacesTheEmptySong
