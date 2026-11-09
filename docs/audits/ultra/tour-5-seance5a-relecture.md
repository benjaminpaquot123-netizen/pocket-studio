# Tour 5, séance 5a : relecture critique (contrôles 6, 7, 8, 9 du selftest et import d'un .mid)
Relu : 4330452

Relecteur neuf (n'a pas écrit le code). Périmètre : `bebe0c9..4330452` (0f0b05b partage, 859d514 micro, 4027fb1 pads,
fcd6e4c Live › Pads et .mid, 4330452 rapport) : 4 fichiers de test du banc + le rapport, aucun code principal de l'appli,
`HumanTester.kt`, `BenchEngine.kt` et `BenchEngineTest.kt` non touchés (vérifié sur `git diff --stat`).

## Ce qui a été vérifié, en le faisant tourner (3 passages, `scripts/bench/gradle-file.sh`, verdicts lus dans les XML JUnit)
- **Passage 1, banc cassé exprès comme décrit dans les commits** (`BenchEngine.exportRender` en `IoError` à tout rendu après le
  premier ; condition `audio/midi` du `picker` de `HumanTester` rendue fausse ; édits jetables, restaurés par `git checkout`) :
  `BenchShareTest` 7 tests, **3 rouges** (`twoStemsLeaveAsTwoFiles`, `fourStemsLeaveAsFourFiles` : « no error expected null,
  but was:<L'écriture du fichier a échoué (stockage plein ?).> », plus le collatéral annoncé `annulerStopsAndClosesAtOnce`) ;
  `BenchAmeneesTest` 12 tests, **1 rouge** (`aMidImportedPutsItsNotesIn` : « the picker gave a .mid: Son : rien ne joue. Le
  sélecteur de fichiers s'est ouvert, mais sur ce banc il ne propose une chanson que pour le Remix… »). Messages identiques
  à ceux des commits et du rapport.
- **Passage 2, appli cassée exprès (banc intact), pour tester ce que les commits ne prouvaient pas** : `WavExporter.share`
  n'envoyant que le premier fichier dans `ACTION_SEND_MULTIPLE` → `twoStemsLeaveAsTwoFiles` et `fourStemsLeaveAsFourFiles`
  **rouges sur l'assertion de comptage** (« 2 files → 2 EXTRA_STREAM … expected:<2> but was:<1> », « … expected:<4> but
  was:<1> ») : le contrôle 6 compte vraiment (le vieux `twoFilesLeaveTogether` ne l'aurait pas vu). Et `fillLiveStarter`
  mourant après `make()` → les trois ouvertures de `BenchPlaceAndPadLiveTest` **rouges** (« Pad Live's pads … never came »),
  mais ce rouge traverse aussi le remplaçant du banc (même `fillLiveStarter`) : voir constat 1 pour ce qu'il ne prouve pas.
- **Passage 3, vrai banc, les 4 classes** : `BenchShareTest` 7/7, `BenchTour1Test` 12 (1 sauté `drive`), `BenchPlaceAndPadLiveTest` 5 (1 sauté), `BenchAmeneesTest` 12 (1 sauté) : **36 tests, 0 rouge, 3 sautés**, 4 min 25 avec Gradle. Les trois ouvertures de Pad Live à 16/16 pads, « PADLIVE … » : 3 610 ms (« Que veux-tu faire ? »), 3 595 ms (`fresh=reel`), 3 580 ms (après un morceau) d'horloge simulée, contre 1 805 à 3 625 ms dans le rapport : sous le seuil, mais déjà à un tiers de celui-ci (constat 4).
- Toutes les cassures défaites (`git checkout`), `git status --short | grep -v '^ T'` vide.

## Lecture des tests
- **Par l'écran** : les cinq contrôles touchent des tags (`tap`/`act("touche …")`, `reachTag`) et lisent l'état de l'appli
  (`ExportUi.files`/`error`, l'intent du menu Android, `audioRecording`, les clips des pistes audio, `project.live.at(i)`,
  `padLive.home`, `synthNotes`). Les seuls raccourcis par le view model sont de la **mise en place** (le `build` de
  `openWithStems`, `vm.addTrackOf`/`vm.addNote` pour fabriquer la ligne de basse à exporter) : acceptable, l'action jugée
  est faite au doigt.
- **Pas de sommeil réel comme assertion** : les attentes sont des bornes hautes (30 à 120 s réelles) autour de l'horloge
  simulée du banc ; pas de dépendance à l'ordre (état par instance : `padsAsOnASeat`, `BenchEngine()` par test ; `I18n.lang`
  remis à FR par `AppHarness.finish`, `NativeEngine.sink` remis à null dans les `@After`, `FileProvider` purgé).
- **`padLiveAwaitsItsSet` / `standInForTheStarter` « code mort »** : confirmé **pour l'ouverture par le testeur**, avec une
  nuance (constat 1) : ce n'est pas du code mort au sens strict. (a) Dans l'amenée `AMENEE_PADLIVE` (`HumanTester.kt`
  l. 650), `standInForTheStarter()` est appelé sans condition : si `live.at(4)` est encore vide à cet instant (le cas
  courant, le jeu de l'appli met 2 à 4 s), il **annule le travail de l'appli** (`liveStarterJob.cancel()`) et pose ses
  propres tonalités. (b) Dans `engineDone` (l. 826), `padLiveAwaitsItsSet` ne peut être vrai qu'après un travail de
  l'appli fini sans `starterOffered` (une exception qui s'échappe du corps de `fillLiveStarter` avant `applied`) ; or
  `SoundLibraryViewModel.liveStarter` attrape pad par pad et `applied` marque le jeu offert même partiel (d'où les 11/16 et
  8/16 observés) : en pratique inatteignable aujourd'hui, donc l'observation du rapport est juste.

## Constats

| Gravité | Constat | Suite |
|---|---|---|
| Moyen | **Le contrôle 8 dit couvrir « chaque ouverture », l'amenée comprise (« déjà couverts par `padLiveStartsWithItsPadsReady` ») ; or sur l'amenée, c'est le remplaçant du banc qui remplit les pads** (il annule le travail de l'appli dès que `live.at(4)` est vide, voir ci-dessus) : cette ouverture-là ne prouve rien du jeu de l'appli. Le rapport le dit à demi (« n'intervient qu'en amenée ») mais la ligne 5.7 est présentée comme close pour l'amenée. | Dans `HumanTester.amenee(AMENEE_PADLIVE)` (équipe du fichier) : attendre d'abord le travail de l'appli (`liveStarting` → false) et ne poser le remplaçant que s'il n'a rien donné, en le disant « BANC » ; ou retirer le remplaçant et laisser `waitFor("les pads prêts")`. D'ici là, noter dans PLAN-RESTE 5.7 : « amenée = tonalités du banc ». |
| Moyen | **La preuve « rouge sur banc cassé » du contrôle 6 ne touche pas l'assertion qui compte.** La cassure décrite (`exportRender` qui échoue) rend le test rouge sur `assertNull("no error")`, avant tout menu : elle prouve la lecture de l'erreur, pas le comptage des `EXTRA_STREAM`, qui était justement le trou C7. Mon passage 2 (premier fichier seul) le prouve ; le rapport, non. | Ajouter au rapport (ou au message de commit suivant) la cassure côté appli ou côté lecture (`menusOpened` ne gardant que `uris.first()`) et son rouge « expected:<2> but was:<1> ». Pas de code à changer. |
| Mineur | **Contrôle 9 (Live › Pads) : « allumé » et « ligne de son » sont écrits par le banc lui-même** : `vm.liveStates` est posé par `HumanTester.lightPads` (réflexion sur `_liveStates`), la phrase « joue en boucle » par `padFollowed`. Ce que le test prouve de l'appli : qu'un toucher sur `live_full_pad_N` atteint `RhythmViewModel.toggleLive(N)` et qu'un second l'éteint : c'est la garde voulue par la ligne 5.8 (le banc doit suivre Live › Pads, tour 3 zone 1 n° 5), mais le rapport la présente comme un contrôle de l'appli. | Une phrase dans le rapport et le KDoc du test : « garde du remplaçant du banc : ce qui est vérifié de l'appli, c'est le câblage Live › Pads → `toggleLive` ». |
| Mineur | **Le seuil « 10 s simulées » du contrôle 8 n'est pas indépendant de la machine** : chaque tour de `waitFor` ajoute 0,5 s d'horloge simulée pendant que le vrai travail (16 `padSamples.add` sur IO, les rendus) avance en temps réel ; `backgroundQuiet` absorbe jusqu'à 5 s réelles par tour, ce qui explique les 4 à 7 tours mesurés (1,8 à 3,6 s). Une machine chargée (4 forks du banc) fait plus de tours : marge ×3 environ. Conforme à la ligne 5.7 (« < 10 s simulées ») et à la règle du §2 (pas de seuil réel sec), donc acceptable, mais à surveiller. | Garder le `PADLIVE …` dans la sortie ; si un rouge « the pads took … ms » apparaît en selftest, appliquer la règle du §2 (médiane de 3 passages) avant de toucher au seuil. |
| Mineur | `BenchPlaceAndPadLiveTest.allowIfAsked()` n'est appelé qu'une fois, juste après le toucher ; la question système de `fresh=reel` est dessinée par le banc dans `settle → permissions()`, donc présente à ce moment-là si elle vient avec le toucher, mais une question qui viendrait à un geste suivant resterait sans réponse (sans effet sur les pads aujourd'hui : Pad Live ne demande pas le micro à l'ouverture). `viewText` redouble aussi une marche de `BenchWindows` qui pourrait vivre dans `HumanTester` (fichier réservé). | Rien avant fusion ; à déplacer dans `HumanTester` (helper « réponds à la question système ») quand le fichier sera libre. |
| Mineur | `aMidImportedPutsItsNotesIn` : le `.mid` est fabriqué par le banc **depuis le projet ouvert** (`MidiFile.export`), puis réimporté dans le même projet : l'appli exporte et relit son propre fichier (3 notes → canal 1 → synthé). Prouve le chemin picker → `importMidi` → `importInto`, pas la lecture d'un MIDI d'un autre logiciel (ce n'est pas ce que demande 5.13, miroir du .zip). | Noter dans le KDoc que le fichier est « maison » ; un `.mid` fixe de deux pistes/canaux en ressource de test serait un plus (`project-model` a déjà des tests de `MidiFile`). |
| Info | Les durées : `BenchAmeneesTest` 12 tests ≈ 1 min sur le vrai banc, mais ≈ 14 min quand les pads ne viennent pas (attentes de 60 s + 120 s par test : 7 tests en cascade) : un banc cassé coûte cher en selftest, c'est le prix voulu des gardes. | Rien. |

Verdict : 0 bloquant, 0 grave — fusion possible
