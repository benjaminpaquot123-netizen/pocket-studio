# Audit ultra, tour 1 : l'équipe du banc

Zone : le banc du « testeur humain » (`app/src/test/.../human/`, `scripts/bench/`). Aucun fichier de l'appli touché.
Règle §4 : banc = le banc ment là où un téléphone a son moteur ; la preuve est le code de l'appli qui attend le moteur, ou le journal du siège.

## Corrigé (banc)
| Synthèse | Verdict, preuve | Gravité | Test |
|---|---|---|---|
| Banc : amenée remix (B c, Pro + main=gauche) | banc : en Pro l'appli ouvre « Que veux-tu faire ? » (guide vu), l'amenée ne cherchait que l'accueil (`welcome_remix`). Elle prend maintenant `welcome_*` ou `launch_*` (remix, nouveau morceau › « Je pars de zéro », Pad Live) | bloquant | `BenchAmeneesProfilesTest` (2 sièges ratés reproduits rouges) |
| Banc : amenée padlive (E c, tablette paysage + lit=non) | banc : le banc glissait sur la marge gauche, hors de la colonne centrée de l'accueil ; « Pad Live » touché sous l'écran (y = 1222 pour 1200). Il fait défiler le conteneur jusqu'à la cible, puis glisse sur la colonne de la cible | bloquant | idem : 3 amenées × 10 profils du tour (7 appareils, Simple/Pro, 130/200 %, lit=non, une main, arabe, japonais) |
| 29 Micro « ne fait rien » (A d, C c, E b) | banc : `recordAudio` sort sans rien dire sans moteur ; l'AudioRecord de Robolectric rend du silence d'un coup. Faux micro `BenchMic` (une voix 220 Hz en syllabes, au rythme de l'horloge du banc) pour « Enregistrer ma voix », pads live, notes chantées (le son pris devient un échantillon du moteur : le moteur du banc vient pendant la prise) ; et le moteur du banc répond au micro des pistes audio (`micStart/micStatus/micFinish`, la voie ouverte par l'équipe zone 4) avec décompte puis la même voix | grave | `BenchTour1Test.theMicrophoneHearsAVoice`, `anAudioTrackTakeRecords` |
| 16 « Exporter » du Mix sans effet (A c, E d, F c) | banc : `startExport` sort à `!NativeEngine.isAvailable` ; journal F c #30, #33, #63 : DOIGT export, même empreinte. Le banc donne son moteur au toucher (la zone 2 a aussi ramené le résultat à la vue : les deux étaient vrais) | grave | `theMixTabsExportRenders` (rouge sans la correction) |
| 4 « Écouter le bout » muet (F b) | banc : l'appli jouait (écran 54 : « Stop 1 bar · 0:02.0 ») par le lecteur Android de secours ; la ligne de son du banc ne le disait pas. Elle dit maintenant « un bout de la chanson joue : la batterie seule, 2,0 s » | moyen | pas de test dédié (la page Découpe demande une séparation : coûteux) |
| 10 « Écouter » de la performance muet (E a) | banc : `listenPerformance` → `playSongFrom` sort sans moteur ; journal E a #45 → #46 « rien ne joue ». Moteur donné au toucher | moyen | `aKeptPerformanceIsHeard` |
| 40 « Créer la vidéo » : « moteur pas prêt » (C a) | banc : `VideoExportViewModel.start` exige le moteur. Moteur donné au toucher et gardé pendant la vidéo, encodeur vidéo du téléphone simulé | moyen | `theVideoIsMade` |
| 19 Mesures en direct : que des tirets (E d, F c) | banc : `pollMeters` sort sans moteur natif. Pendant la lecture le banc affiche ce qu'un moteur lirait sur un mix au niveau streaming (pistes qui jouent, −14 LUFS court terme, −1,2 dBTP), valeurs fixes pour le rejeu | moyen | `theLiveMetersRead` |
| 34 « Montre-moi » silencieux (B d) | banc : `tutoDemoSound` sort sans moteur. Moteur donné au toucher de « Montre-moi », gardé tant que le doigt de la démo est là ; la ligne de son dit les pads et notes envoyés au moteur | moyen | `padsPlayedThroughTheEngineAreHeard` |
| 27 Changer le son d'une piste : le morceau tourne en boucle sur l'Intro (D b, C a ; signalé par l'équipe zone 3) | banc : à tout geste qui n'est pas dans ses listes, le banc arrêtait le morceau, lâchait son moteur et relançait ▶ (la boucle de la partie, seule possible sans moteur). Le moteur reste tant que le morceau joue | moyen | `theSongGoesOnAfterAnotherGesture` (rouge sans la correction : « la musique du projet joue en boucle ») |
| Banc : « RIEN NE CHANGE » | « regarde » écrit comme geste était une erreur de geste (plantage du siège) : c'est maintenant un regard, ni compté ni budgété ; le compteur repart à zéro tant qu'un long travail tourne (séparation, export, vidéo, pack, prise, kit du tuto, anneau qui tourne) | moyen | `nothingChangesIgnoresLooksAndLongWork` |
| Banc : oracles manquants | `seat-metrics.py` : `tuto-facile`, `tuto-pro` (chapitres entiers au bon niveau, lus dans `etat/prefs/tuto.xml` et le cours), `synthe` (un même son sur deux pistes avec des notes ; `SeatState` écrit désormais `synth_tracks` avec « son retouché »), `libre` (fini par le testeur, ≥ 3 écrans, sans plantage ni gel), `defi` « partielle » quand un défi a été ouvert sans étoile | — | `scripts/bench/test_ultra_bench.py` |

Sièges du tour relus par les nouveaux oracles : B d tuto-pro réussie (5 chapitres), A b tuto-facile réussie (3), D c synthe réussie, F d libre réussie, F e défi partielle (était « aucun défi joué »).

## Pour les équipes de l'appli (pas corrigé ici)
- Zone 4 (n° 29) : sans moteur, `recordAudio` et `startExport` sortent en silence ; un téléphone a toujours son moteur, mais si l'ouverture échoue l'appli doit le dire (la même phrase que « Le micro n'a pas pu être ouvert… »). Pas de test : seul le banc n'a pas de moteur.
- Zone 5 (n° 34) : « Montre-moi » du tempo laisse 174 BPM posé (B d) : appli, pas le banc (la démo restaure le projet, pas le tempo).
- Zone 2 (n° 19) : limiteur sans GR, release « ×2,0 » sans ms, curseurs master sans nom : appli (vus avec les tirets, toujours vrais avec les mesures du banc).
- Le moteur du banc n'est donné que pour les gestes listés (`ENGINE_TAGS`, micro, ▶ du morceau, Partager) : tout autre bouton qui exige `NativeEngine.isAvailable` reste muet sur le banc. Les pads de batterie touchés par le testeur ne s'entendent toujours pas (seuls ceux de « Montre-moi » et du Pad Live).

## Tests lancés
`BenchTour1Test` (10), `BenchAmeneesProfilesTest` (30), `BenchAmeneesTest`, `BenchEngineTest`, `BenchShareTest`, `BenchSeatOptionsTest` : verts ; `scripts/bench/test_ultra_bench.py` : vert.
Rouges, et aussi rouges sans la correction n° 27 (donc pas ce lot) : `BenchPlayThenTabsTest` × 2 (« Retour » mène maintenant à « Que veux-tu faire ? », plus de ▶ à toucher : à revoir par la zone 3) et `BenchAssistantTest` (l'écoute d'une ambiance de l'assistant : « rien ne joue », après les changements de la zone 5).

## Restes (banc)
- `fresh=reel` : le micro accordé à la question d'Android, le banc redonne son moteur pour la prise qui suit (non testé : pas de siège `fresh=reel` en mission voix ce tour).
- La vidéo se fabrique sur le son du banc (3 s de la tonalité, puis du silence).
