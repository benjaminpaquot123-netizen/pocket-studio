# Tour 5, séance 5 (équipe 5a) : contrôles 6, 7, 8, 9 du selftest et import d'un .mid

Branche `eq/tour-5-seance5a`. Lignes **5.5, 5.6, 5.7, 5.8 et 5.13** de `docs/audits/ultra/PLAN-RESTE.md` §2.
Aucun code principal de l'appli modifié ; `HumanTester.kt` et `BenchEngineTest.kt` non modifiés (une autre équipe y travaillait) ;
`verdicts.tsv` non touché.

## En bref

- **5 contrôles écrits, chacun rouge sur le banc cassé exprès, puis vert sur le vrai banc** : partage de N fichiers et feuille
  dans la langue du siège (6), micro par la carte et par le ● en Simple et en Pro (7), pads de Pad Live remplis à chaque
  ouverture en moins de 10 s d'horloge (8), touchers Live › Pads (9), import d'un .mid (P1).
- **Aucun défaut de l'appli trouvé** par ces contrôles : tout ce qui a été rouge l'a été par la cassure voulue du banc.
- **Une découverte sur le banc** : depuis la méthode v2 (moteur toujours là), ce n'est plus le « remplaçant » du banc
  (`HumanTester.standInForTheStarter` / `padLiveAwaitsItsSet`) qui remplit les pads de Pad Live quand le testeur l'ouvre
  lui-même : **c'est l'appli, avec le moteur du banc** (`BenchEngine.exportRenderFloat` pour les boucles, le kit du banc,
  `BenchPartySounds` pour les sons de fête). Casser le remplaçant ne change rien (vert) ; casser le rendu du moteur du banc
  rend le contrôle rouge. Le commentaire du remplaçant (« le moteur du banc ne sait pas le rendre, il attend pour toujours »)
  est périmé : à simplifier dans `HumanTester.kt` (pas fait ici, fichier réservé).

## Méthode

Chaque contrôle est un test `Bench*` lancé par `scripts/bench/selftest.sh`. Pour chacun : le banc a été **cassé exprès**
(modification locale jetable du fichier du banc concerné, jamais commitée, restaurée par `git checkout`), le test lancé
**rouge**, le message rouge exact noté ci-dessous et dans le message du commit, puis le banc restauré et le test **vert**.
Passages : `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests … -Ptest.results.dir=…` (file d'attente partagée),
verdicts lus dans les XML JUnit :

| Passage | Banc | Classes | Résultat |
|---|---|---|---|
| 1 | vrai | les 4 classes | 36 tests, 2 rouges (deux erreurs de navigation de mes nouveaux tests, corrigées : « Plus » devant « Briques », vue « Notes » devant « Importer un fichier MIDI ») |
| 2 | cassé exprès (6 cassures) | les 4 classes | rouges attendus pour 5.5, 5.6, 5.8, 5.13 ; **5.7 resté vert** (le remplaçant du banc n'est pas ce qui remplit les pads : voir ci-dessus) |
| 3 | cassé exprès (rendu du moteur du banc) | `BenchPlaceAndPadLiveTest` | 5 tests, **3 rouges** (les trois ouvertures : 11, 8 et 8 pads sur 16), 1 sauté |
| 4 | vrai | `BenchPlaceAndPadLiveTest`, `BenchAmeneesTest` | 17 tests, **0 rouge**, 2 sautés (`drive`) |

## Les contrôles

### 5.5 : contrôle 6, le partage liste tous les fichiers, la feuille parle la langue du siège

Fichier : `BenchShareTest.kt` (commit `0f0b05b`). Le test lit **tous** les `EXTRA_STREAM` d'un menu de partage
(`ACTION_SEND_MULTIPLE`), une entrée par menu (C7 du méta-audit : `HumanTester.kt` ne lisait que `firstOrNull()` et l'ancien
`twoFilesLeaveTogether` était vert quand même, `ci-242.md` n° 7).

| Test | Ce qu'il prouve |
|---|---|
| `twoStemsLeaveAsTwoFiles` | projet Pro à 2 pistes qui sonnent (batterie, synthé), Mix › Avancé › Piste par piste › Exporter, « Partager les 2 fichiers » : **1 menu, `ACTION_SEND_MULTIPLE`, 2 `EXTRA_STREAM` distincts**, les fichiers fabriqués |
| `fourStemsLeaveAsFourFiles` | idem avec 4 pistes (batterie, synthé, basse, mélodie) : **4 `EXTRA_STREAM`** |
| `theShareMenuSpeaksTheSeatsLanguage` | siège `lang=ja` (C5 : la feuille dessinée en français pour tous les sièges) : le bouton de « Prêt à partager ? » et le titre du menu (`EXTRA_TITLE`) sont `tr(…)` en japonais (`シェア`, `曲をシェア`), jamais les mots français |

Cassures et rouges :
- `BenchEngine.exportRender` échoue à tout rendu après le premier → `java.lang.AssertionError: no error expected null, but was:<L'écriture du fichier a échoué (stockage plein ?).>` (les deux cas 2 et 4 ; dommage collatéral attendu : `annulerStopsAndClosesAtOnce`, qui rend deux fois).
- `AppHarness.start` ignore la langue du siège (`language = Lang.FR`) → `java.lang.AssertionError: the sheet's button in the seat's language (ja): «   Partager »`.

Vert (passage 1) : **7 tests, 0 rouge**.

### 5.6 : contrôle 7, le micro par la carte et par le ● du haut, Simple et Pro

Fichier : `BenchTour1Test.kt` (commit `859d514`). Les prises sont lancées **par des touchers** (plus par le view model) :

| Test | Parcours |
|---|---|
| `theMicrophoneRecordsFromTheCardAndTheTopDotInPro` | Sons › Voix ; « Enregistrer » de la carte, arrêt par « Arrêter » de la carte ; puis le ● du haut (c'est le micro sur cette page), arrêt par le ● |
| `theMicrophoneRecordsFromTheTopDotAndTheCardInSimple` | le ● demande quoi enregistrer, « Le micro » ouvre la page du micro et lance la prise, le ● l'arrête ; puis « Nouvelle prise » de la carte |

À chaque prise : le niveau d'entrée monte (> 0,1), la prise finit sur une piste audio comme **nouveau clip**, le micro du banc
compte une prise de plus (`BenchEngine.micTakes`).

Cassure et rouges : `BenchEngine.micStatus` ne sort jamais du décompte (`else -> 0f`) →
`java.lang.AssertionError: the card's « Enregistrer » (Pro): the input level shows` et
`java.lang.AssertionError: the top ● › « Le micro » (Simple): the input level shows` (collatéral attendu : `anAudioTrackTakeRecords`).
Remarque banc : les secondes de la prise avancent même pendant le décompte (`micStatus` rend les trames quel que soit l'état), d'où
un rouge sur le niveau plutôt que sur les secondes ; sans conséquence pour les sièges.

Vert (passage 1) : **12 tests (1 sauté : `drive`), 0 rouge**.

### 5.7 : contrôle 8, les pads remplis à chaque ouverture en moins de 10 s d'horloge

Fichier : `BenchPlaceAndPadLiveTest.kt` (commit `4027fb1`). « Remplis » = **les 16 pads du jeu prêt à jouer** (`LiveStarter.PADS` :
3 boucles rendues par le moteur, le clap, 4 sons de fête, 8 coups du kit), mesurés à l'horloge du banc (`SystemClock.uptimeMillis`,
celle de l'appli) entre le toucher d'ouverture et les pads prêts, le temps écrit dans la sortie du test (`PADLIVE …`).

| Test | Ouverture | Pads (passage 4) | Temps mesuré (passage 1 / passage 4) |
|---|---|---|---|
| `padLiveTouchedFromTheWelcomeOpensOnItsPads` (existant, durci) | « Que veux-tu faire ? », tablette paysage | 16/16 | 2 715 ms / 1 805 ms |
| `padLiveOpenedAfterASongHasItsPads` (nouveau) | un morceau fait à l'assistant (Pro), joué, quitté par Retour, puis Pad Live depuis « Que veux-tu faire ? » : son projet « Live » à lui, pas le morceau | 16/16 | 3 580 ms / 3 550 ms |
| `padLiveOpenedOnAFreshPhoneHasItsPads` (nouveau) | `fresh=reel` (rien de réglé d'avance), la carte Pad Live de l'accueil | 16/16 | 3 625 ms / 2 715 ms |

L'amenée et le pack sont déjà couverts par `BenchAmeneesTest#padLiveStartsWithItsPadsReady` et `#aPackFillsThePageShown`.
Aucun seuil sec en secondes réelles : le seuil (10 s) porte sur l'horloge simulée, déterministe ; les trois mesures sont sous 4 s.

Cassures et rouges :
- `HumanTester.padLiveAwaitsItsSet` à `false` (le remplaçant du banc ne vient plus) : **vert quand même** (passage 2) : ce n'est pas
  lui qui remplit les pads (voir « En bref »).
- `BenchEngine.exportRenderFloat` rend un tableau vide (le moteur du banc ne sait plus rendre un son en mémoire) → `java.lang.AssertionError: from « Que veux-tu faire ? » (seat A c, #1): every pad of the ready-to-play set is filled (11 of 16) expected:<16> but was:<11>`, `… fresh=reel, from the welcome: … (8 of 16) …`, `… after a song: … (8 of 16) …` (passage 3).

Vert (passage 4) : **5 tests (1 sauté : `drive`), 0 rouge** ; 16/16 pads aux trois ouvertures.

### 5.8 : contrôle 9, touchers Pad Live **et** Live › Pads : allume, éteint, ligne de son

Fichier : `BenchAmeneesTest.kt` (commit `fcd6e4c`). Pad Live était déjà couvert (`padLiveLightsStopsAndRecordsAsOnAPhone`) ; cas ajouté :

| Test | Parcours |
|---|---|
| `livePadsLightGoOutAndAreHeardAsPadLivesDo` | amenée Pad Live ; « Plus » › « Briques » ouvre la vue Live ; onglet « Pads » (plein écran sur un téléphone, `live_full_pad_N`) ; un pad boucle touché : état 2 (allumé) et la ligne de son dit « le pad live N joue en boucle » ; retouché : état 0 et la ligne de son ne le dit plus |

Cassure et rouges : `HumanTester.kt` l. 476, le banc ne suit plus `toggleLive` (`v.liveToggledForTest` non branché) →
`java.lang.AssertionError: the sound line says the pad plays: Son : rien ne joue.` (Live › Pads) et
`java.lang.AssertionError: Son : rien ne joue.` (Pad Live, le test existant : la même cassure couvre les deux chemins).

Vert (passage 4) : **12 tests (1 sauté : `drive`), 0 rouge**.

### 5.13 : P1, import d'un .mid (miroir du test .zip)

Fichier : `BenchAmeneesTest.kt` (même commit). `aMidImportedPutsItsNotesIn` : projet vide en Pro, une ligne de basse de 3 notes
sur une piste ajoutée (le picker du banc fabrique son .mid **depuis le projet ouvert**) ; Sons › puce de la piste › vue « Notes » ›
« Importer un fichier MIDI » : le picker répond « Le-morceau-de-Sam.mid » (dit dans la ligne de son), l'import ne finit pas en
`SampleTask.Failed`, le premier canal se pose sur le synthé (3 notes de plus : 3 → 6).

Cassure et rouge : `HumanTester.picker` ne répond plus de .mid (condition inversée) →
`java.lang.AssertionError: the picker gave a .mid: Son : rien ne joue.` suivi de la phrase du banc « Le sélecteur de fichiers
s'est ouvert, mais sur ce banc il ne propose une chanson que pour le Remix, et un projet (.zip) ou un MIDI (.mid) quand l'appli
les demande : rien n'a été choisi. »

## Défauts de l'appli

Aucun trouvé par ces cinq contrôles. Deux observations, **pas des défauts** :
- Le bouton « Briques » (vue Live) et les autres de « Plus » ne sont visibles qu'après le toucher de « Plus » : voulu (deux rangées
  repliées, lot B3).
- « Importer un fichier MIDI » n'est que dans la vue « Notes » de la page du clavier, pas dans la vue « Clavier » : voulu.

## Changements de `HumanTester.kt` qui auraient été utiles (non faits : fichier réservé à une autre équipe)

1. `standInForTheStarter` / `padLiveAwaitsItsSet` : commentaire et rôle périmés depuis la méthode v2 (l'appli remplit les pads
   avec le moteur du banc ; le remplaçant n'intervient qu'en amenée, et seulement si `live.at(4)` est encore vide à cet
   instant). À réécrire en garde (« si l'appli n'a rien posé au bout de N s, dire BANC ») ou à retirer, après le passage de
   `BenchPlaceAndPadLiveTest` qui le prouve.
2. Aucun autre changement nécessaire : les cinq contrôles passent avec le banc d'aujourd'hui.

## Comptes verts (vrai banc)

| Classe | Tests | Rouges | Sautés | Passage |
|---|---|---|---|---|
| `BenchShareTest` | 7 | 0 | 0 | 1 |
| `BenchTour1Test` | 12 | 0 | 1 (`drive`) | 1 |
| `BenchPlaceAndPadLiveTest` | 5 | 0 | 1 (`drive`) | 4 |
| `BenchAmeneesTest` | 12 | 0 | 1 (`drive`) | 4 |
