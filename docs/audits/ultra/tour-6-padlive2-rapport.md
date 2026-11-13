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

Restent hors du lot : la carte « Pad Live » de l'accueil dit encore « Ça sonne tout de suite. » (`onboarding/`, textes) ; l'effet de bord de P3 (le choix « Nouvelle version / Ajouter à la suite » de la page Live suit la dernière prise de Pad Live).

Juge du chantier : 109 écarts (tête : 110).

tests: com.pocketstudio.app.padlive.PadLivePlayShowsTest com.pocketstudio.app.padlive.PadLiveSilentOpenTest com.pocketstudio.app.padlive.PadLiveTakeAfterTest com.pocketstudio.app.padlive.PadLiveTouchCountTest com.pocketstudio.app.padlive.PadLiveSaveUndoTest com.pocketstudio.app.human.BenchAmeneesTest

## Liste des chemins

- chemin: Pad Live bouton ▶ du haut avec des boucles seules | test: PadLivePlayShowsTest#loopsOnTheirOwnShowTheSquareAndOneTouchStopsThem
- chemin: Pad Live bouton ▶ du haut avec un pad en attente | test: PadLivePlayShowsTest#aPadWaitingForItsLineAlreadyShowsTheSquare
- chemin: Pad Live plein écran « Fête » bouton ▶ | test: PadLivePlayShowsTest#thePartyScreenSaysTheSame
- chemin: Pad Live ■ sur le banc (moteur, pads éteints) | test: BenchAmeneesTest#padLiveLightsStopsAndRecordsAsOnAPhone
- chemin: accueil carte « Pad Live » ouverture | test: PadLiveSilentOpenTest#openingFromTheCardPlaysNothing
- chemin: accueil carte « Pad Live » puis premier pad | test: PadLiveTouchCountTest#theFirstSoundIsTwoTouchesAway
- chemin: Pad Live REC deuxième prise | test: PadLiveTakeAfterTest#aSecondTakeGoesAfterTheFirst
- chemin: Pad Live REC première prise d'un projet Live neuf | test: PadLiveTakeAfterTest#theFirstTakeStillReplacesTheEmptySong
