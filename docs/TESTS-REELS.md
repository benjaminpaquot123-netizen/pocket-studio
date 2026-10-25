# Tests d'usage réel

Des tests qui font ce que fait un utilisateur, avec la vraie appli et le vrai moteur, plutôt que des morceaux de code isolés. Quatre familles : les **parcours** (un débutant de l'installation au partage), l'**endurance** (jouer longtemps), le **petit téléphone** (le fil audio contre son budget) et les **projets réels** (ceux de Benjamin).

| Test | Où | Quand | Durée |
|---|---|---|---|
| `JourneyTest` (2 parcours) | émulateur (androidTest) | chaque PR (job « Emulator smoke test ») et la nuit | ~10 min |
| `JourneyPathsTest` (les mêmes touches, sans moteur) | Robolectric (`:app:testDebugUnitTest`) | chaque PR, en local | ~1 min |
| `EnduranceTest` | émulateur | PR : 2 min ; nuit : 10 min ; à la main : 30 min | |
| `scripts/soak.sh` (endurance sur Linux) | Linux, `ps_live` | PR : morceau de 2 min bouclé 10 min ; nuit : 6 min bouclé 30 min | 3 à 8 min |
| `scripts/cpu-bench.sh` (petit téléphone) | Linux, `ps_live` | la nuit (37 styles) ; en local | ~25 min |
| `RealProjectsTest` + `scripts/real-projects.sh` | Robolectric + `ps_live` / `ps_render` | chaque PR et la nuit | ~2 min par projet |

Un échec de la nuit envoie un e-mail au propriétaire du dépôt (GitHub prévient le compte qui a posé le planning, c'est-à-dire celui de Benjamin, notifications par défaut) ; l'agent doit les regarder au début de chaque session (ligne proposée pour CLAUDE.md, à ajouter par Benjamin ou la session principale). La nuit : `.github/workflows/nightly.yml` (« Nightly real use »), chaque nuit sur main, ou à la main (Actions › Nightly real use › Run workflow, durées réglables). Les rapports sont dans les artefacts `nightly-linux` et `nightly-emulator`. Une PR reste sous ~45 min : les longs passages n'y sont qu'en version courte.

## Parcours (émulateur)

`app/src/androidTest/.../JourneyTest.kt`, sur un téléphone « tout juste installé » (fichiers et réglages effacés avant et après : le test suivant, `SmokeTest` compris, trouve un premier lancement).

1. **Toutes les fonctions** : premier lancement › « Complet » › « Plus tard » ; nouveau démarrage › « Que veux-tu faire ? » › Continuer ; nouveau démarrage › Nouveau morceau › l'assistant › jump up › Créer maintenant ; « Ton morceau est prêt ! » › Écouter **60 s** (crête de chaque seconde au-dessus de −40 dBFS après les 3 premières, position du morceau qui avance chaque seconde) ; Structure › Parties › ⋮ › « Fill à la fin » puis « Montée avant » sur une autre partie (« Fait : … ») ; Sons › la piste Lead › ● Enregistrer ma mélodie (une mesure de décompte vérifiée), quatre touches du clavier à l'écran, ■, « Remettre en rythme » (les notes sur les doubles croches), Garder ; Mix › Exporter le morceau en **M4A** (décodé par le téléphone : durée = le morceau, +10 s de queue au plus ; true peak ≤ −1 dBTP mesuré par le test, suréchantillonné 4×) puis en **WAV** (durée lue dans l'en-tête) ; ⤴ Partager › Partager : la feuille de partage d'Android est demandée avec le fichier (intercepté avant de s'afficher, son type et sa taille vérifiés).
2. **Mode simple** : premier lancement › « Simple » › « Créer mon premier morceau (guidé) » › style lo-fi (famille Calme) › Créer ; missions arrêtées ; Écouter 20 s ; onglet Clavier › une prise remise en rythme ; onglet Envoyer › WAV › Partager.

Les touches passent par `app/src/sharedTest/.../journey/Journey.kt`, compilé dans les deux jeux de tests : `JourneyPathsTest` (Robolectric) refait les mêmes chemins sans moteur, si bien qu'une étiquette renommée casse en une minute en local, pas seulement sur l'émulateur. Aucune étiquette n'a dû être ajoutée à l'appli : toutes existaient.

**Émulateur de la CI** (x86_64, API 35, google_apis, swiftshader) : le flux n'est pas celui d'un téléphone (pas de MMAP ni de mode exclusif, rafales et latence plus grandes, quelques sous-alimentations). Chaque test écrit la ligne `stream:` de `Metrics.streamInfo` (fréquence, rafale, tampon, aaudio, mmap, exclusif, latence, sous-alimentations) dans le journal ; `scripts/emulator-tests.sh` écrit au fil de l'eau les lignes `RealUse` dans `app/build/realuse-logcat.txt` (artefact `smoke-test-report` : elles restent même si le job est coupé) et limite chaque test à 15 min (l'endurance : sa durée plus 15 min). Les seuils en tiennent compte : −40 dBFS par seconde (comme `LivePlaybackTest`) ; une « tempête » de sous-alimentations (plus d'un rappel sur 20 en retard dans un tour, au moins 50) fait échouer sur un téléphone, mais n'est qu'écrite dans le journal sur l'émulateur (`Build.HARDWARE` « ranchu »), dont l'audio de l'hôte en produit beaucoup et au hasard. Les autorisations micro et notifications (Android 13+) sont données d'avance : la demande de notifications, à la première prise, resterait sinon par-dessus l'appli.

## Endurance

**Émulateur** (`EnduranceTest`, argument `enduranceMinutes`) : deux projets (le jump up de l'assistant, une démo), puis des tours jusqu'à la fin du temps : Mes projets › l'autre projet ; ▶ 8 s ; boucler la partie 4 s ; la page Live et trois pads ; l'appli en arrière-plan (bouton Accueil) 4 s, la musique continue, puis retour ; rotation paysage puis portrait ; ■. À chaque tour : flux en marche, rappels audio qui avancent (aussi en arrière-plan), fil principal qui répond en moins de 5 s (une ANR), pas de NaN, pas de tempête de sous-alimentations, de la musique entendue. À la fin : tas Java (+64 Mo au plus) et tas natif (+128 Mo au plus) par rapport au 2ᵉ tour, et sur un long passage le dernier tiers pas au-dessus du deuxième (+24 / +48 Mo). Un plantage tue le processus de test : le test échoue.

**Linux** (`scripts/soak.sh [--minutes 30] [--song 360] [--style jump_up]`) : le morceau de l'assistant, coupé après la partie qui atteint `--song` secondes ou rallongé de ses propres parties jusque-là, bouclé (« le morceau reboucle ») pendant `--minutes`, enregistré par `LiveSongRecorder` (`-Pbench.loop`) comme l'appli le joue (chaque partie envoyée après la bascule du moteur, tour après tour) et joué par `ps_live`. Échoue sur : n'importe quelle alerte de l'écoute en direct, **ALLOC** (un appel au tas dans le rappel audio : `ps_live` compte les `malloc`, `calloc`, `realloc`, allocations alignées et `free` du fil audio pendant `Engine::process`, donc aussi tous les `new`/`delete` ; seuls `valloc`/`pvalloc`, que le moteur n'emploie pas, y échappent), **DRIFT** (un tour 1 dB plus fort ou plus faible que le 2ᵉ, ou son gain live à 1,5 dB), moins de 3 tours entiers, des rappels manquants. Puis il vérifie l'alerte elle-même : la même prise, batterie muette dès le 3ᵉ tour, doit sortir DRIFT. Rapport : `build/soak/soak.md` (un tableau par tour).


## Petit téléphone

`ps_live` mesure le temps CPU du fil audio à chaque rappel (temps du fil, pas l'horloge : un autre processus ne compte pas) contre son budget (96 trames = 2 ms, 192 trames = 4 ms à 48 kHz) et modélise un téléphone `--cpu-scale` fois plus lent (3 par défaut) : blocs hors budget, et craquements entendus avec un tampon de 2 rappels (les blocs en retard mangent la marge du tampon ; quand elle est vide, le suivant s'entend).

`scripts/cpu-bench.sh [--styles a,b] [--seconds 60] [--runs 2] [--scale 3] [--cmds DIR] [--strict]` : les 37 styles, 96 et 192 trames, plusieurs passages dont il garde le plus rapide de chaque bloc (le rejeu est identique, un autre processus ne fait que ralentir). Verdict par style : « à risque » si le téléphone 3× plus lent craque même à 192 trames, « limite » s'il ne craque qu'à 96, « ok » sinon. Rapport `build/cpu-bench/report.md`. `--cmds` rejoue les `.pslive` d'un autre banc (par exemple sur une machine ARM). Jamais bloquant en CI : une machine partagée est bruitée ; `scripts/bench/test_cpu_report.py` vérifie le calcul (PR).

Limite : ce n'est pas un téléphone. Un cœur ARM de milieu de gamme n'est pas « un x86 divisé par 3 » (caches, NEON contre AVX, fréquence qui baisse à chaud). Le facteur 3 est une marge prudente pour un Snapdragon 6xx contre ces Xeon ; le vrai juge reste le compteur de sous-alimentations sur le téléphone de Benjamin.

## Projets réels

`app/src/test/resources/real-projects/` : un dossier par projet (`project.json` et `samples/`, comme dans le .zip que partage l'appli) ou directement un `.pocketstudio.zip`. `RealProjectsTest` prend chacun et fait ce que fait l'appli : importer le .zip (`ProjectCatalog.importZip`, comme « Mes projets › Importer ») ; l'ouvrir (`RhythmViewModel`, sons chargés) ; ▶ sur le morceau et 60 s de ce que reçoit le moteur du direct (chaque partie après sa bascule, comme `LiveSongRecorder`) ; « Exporter › Morceau ». Avec `scripts/real-projects.sh`, les 60 s sont jouées par `ps_live` (aucune alerte : fondu, silence, clic, NaN, allocation dans le rappel) et l'export rendu par `ps_render` (pas de NaN, true peak ≤ −1 dBTP, la durée du morceau, jamais 4 s de silence dedans). Dans `:app:testDebugUnitTest` seul, l'import, l'ouverture, la lecture et l'export tournent quand même (sans les binaires). Un dossier vide passe.

**Ajouter un projet** : dans l'appli, Mes projets › ⋮ › Envoyer le projet ; copier le .zip dans `app/src/test/resources/real-projects/` (ou le dézipper dans un sous-dossier). S'il dépasse ~5 Mo : garder `project.json` et les sons que le morceau joue, lister les autres dans `absents.txt` (un nom de fichier par ligne, une raison après). Le dépôt est **public** : rien de personnel (voix, enregistrement au micro) sans l'accord de Benjamin. Le test exige que les sons absents soient exactement ceux de `absents.txt` : un son perdu par erreur le fait échouer.

**Premier projet** : `jumpup-dnb-20261024` (jump up de Benjamin, 46 parties, 174 BPM, 4 min 58). Le .zip fait 13 Mo, dont 12,8 Mo pour les six clips de la page Live, que le morceau ne joue pas : ils sont listés dans `absents.txt`, le dossier fait 1,6 Mo.

**Projet complet** : à activer seulement avec l'accord de Benjamin, sur un hébergement privé (le dépôt et ses releases sont publics ; le .zip complet contient ses clips Live, dont « Voix », peut-être sa voix). Le mécanisme est prêt : `complets.txt` donne pour chacun la release, le fichier et son SHA-256 (la ligne du jump up y est, en commentaire) ; `scripts/real-projects.sh --fetch` les télécharge (gh, sinon curl) dans `build/real-projects/complets/` et les teste avec les autres ; un projet absent ou pas hébergé est signalé et sauté, jamais un échec. Aujourd'hui aucun ne l'est : la nuit ne teste que le dossier du dépôt.

## Lancer en local

- `./scripts/real-projects.sh` (avec `TOOLS=build/live-bench/tools` si le banc d'écoute a déjà construit `ps_live`).
- `./scripts/soak.sh --minutes 10 --song 120` (court) ou sans option (30 min, morceau de 6 min).
- `./scripts/cpu-bench.sh` (37 styles) ou `--styles jump_up,techno --runs 1`.
- Émulateur ou téléphone branché : `./scripts/emulator-tests.sh --endurance 30 --class com.pocketstudio.app.EnduranceTest`.

## Résultats

Mesurés le 2026-10-24 sur la machine de développement (x86_64, 4 cœurs Xeon 2,1 GHz partagés avec d'autres tâches, mémoire juste).

- **Écoute en direct, 37 styles, 60 s** : 37 sur 37 sans alerte, aucun appel au tas dans le rappel audio (30 000 rappels chacun).
- **Endurance Linux, PR** (jump up coupé à 2 min, bouclé 10 min) : 4 tours entiers, niveaux de −13,58 à −13,73 dBFS, gain live de +7,35 à +7,64 dB, 0 alerte, 0 allocation en 300 000 rappels ; la batterie muette au 3ᵉ tour est bien signalée (DRIFT, −2,0 dB).
- **Endurance Linux, nuit** (jump up rallongé à 6 min, bouclé 30 min) : 4 tours entiers de 363 s, niveaux à ±0,02 dB (−13,33 à −13,36 dBFS), gain live à ±0,01 dB, 0 alerte, 0 allocation en 900 000 rappels ; contrôle de la dérive ok (−1,95 dB).
- **Petit téléphone** ([rapport](son/banc/petit-telephone.md), 2 passages, 60 s par style) : 37 sur 37 « ok » pour un téléphone 3× plus lent. Le plus lourd est jump up (moyenne 14 %, p99,9 29 %, pire bloc 38 % du budget à 96 trames), puis dnb, phonk, reggaeton, trap, neurofunk (moyenne 10 à 11 %, pire 25 à 31 %). En poussant le modèle : à 4× plus lent, **dnb et jump up** passent « à risque » ; à 5×, aussi drill, dubstep, liquid dnb, trap (et disco, neurofunk, phonk, reggaeton « limite »). Ce sont les styles à surveiller sur un petit téléphone. Un passage seul sur cette machine montre des pointes isolées (jusqu'à plusieurs centaines de % sur un bloc, la mémoire étant saturée par d'autres tâches) que le passage suivant n'a pas : d'où le plus rapide de chaque bloc.
- **Projets réels** : `jumpup-dnb-20261024` importé (6 sons absents, ceux attendus), 60 s en direct sans alerte, export de 297,9 s à −11,0 LUFS et −1,19 dBTP.
- **Parcours dans Robolectric** (`JourneyPathsTest`) : 9 sur 9.
- **Émulateur** (`JourneyTest`, `EnduranceTest`) : pas d'émulateur ici ; compilés, leurs chemins vérifiés par `JourneyPathsTest`. Premier vrai passage : la CI de la PR.
