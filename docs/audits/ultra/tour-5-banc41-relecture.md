# Relecture neuve : tour 5, banc 4.1 (P3 « tape », défi Pad Live)
Relu : 4575c0d

Relecteur neuf (n'a pas écrit le code). Branche `eq/tour-5-banc41`, commits 31a4690, d00af4c (rouge), 0f5efde (vert),
d2909cc (rapport). Aucun code principal touché (`git diff --raw 75b1534 d2909cc` : 2 fichiers de test modifiés, 1 ajouté,
`scripts/human-act.py`, le rapport). Les coutures utilisées (`ChallengeController.now`, `exampleForTest`,
`PadLiveController.toggleForTest`, `livePadsSoundingForTest`) existaient déjà.

## Ce qui a été vérifié

- `ChallengeControllerTest` : 7/7 vert (2 gardes @KnownDefect vertes « encore là »).
- Mutations locales (annulées par `git checkout`, arbre propre) :
  - `TOLERANCE_BEATS` 0,25 → 0,2 : `fiveHundredMsTapsAreScoredOnTheProjectsTempo` rouge (1 bonne au lieu de 3). Bien.
  - `TOLERANCE_BEATS` 0,25 → 0,5 : **aucun test normal rouge** ; seule la garde T5-DEFI-TENDANCE passe au rouge (« no longer fails »).
  - `TIGHT_BEATS` 0,12 → 0,03 : `aHumanHandOnASoundingPadStillGetsThreeStars` rouge (2 étoiles). Bien.
  - `PadLiveController.touch` qui ignore un pad qui sonne : les deux tests « pad qui sonne » rouges. Bien.
- Gardes : T5-DEFI-TENDANCE échoue sur `assertEquals("four touches 500 ms apart … are said too slow (« … »)", -1, r.tendency)` :
  le message de l'annotation est bien une partie exacte du message ; calcul refait à la main (touchers à 0 ; 0,75 ; 1,5 ;
  2,25 temps ; coups 1-3 appariés aux touchers 1 et 4 : empan 2,25 contre 2 → `tendency = 1`, « trop lent ») : la cause
  est la bonne et le bon comportement (-1) est le bon. T5-DEFI-ECOUTE : écoute à 95 % du temps de résonance (126 369 trames
  sur 128 000), le toucher tombe en `Listening`, puis `YourTurn(c, 0)` : échoue pour la bonne raison, message exact.
- Banc : `BenchAmeneesTest.aTap*` 2/2 vert (4/4, 3 étoiles, les deux à 120 BPM / 500 ms).
- Moteur : `ClipLauncher::toggle` lu (`engine/src/main/cpp/core/ClipLauncher.cpp`) ; explication **confirmée**, voir plus bas.

## Explication moteur : confirmée, avec précisions

- l. 112-113 : seul un pad **boucle** qui joue s'arrête ; un « coup » qui joue tombe dans le `else` (l. 117-124).
- l. 119 : l'horloge `beat_` ne repart à 0 que si **tous** les emplacements sont `Off` ; le clap qui sonne est `Playing`,
  donc pas de remise à zéro.
- l. 120-121 : `startBeat = ceil(beat_ / grid) * grid`, `gridOf` (ClipLauncher.h l. 134) = `min(beats, beatsPerBar)` = 1 temps
  pour un « coup » : le nouveau coup attend la **prochaine ligne de temps du projet** (horloge partie au premier toucher).
- l. 115-116 : un toucher pendant `Waiting` met `Off` : le coup en attente est annulé (l'ancien clap continue de sonner :
  `idle()` l. 119 du .h reste faux tant que `voice.sample`).
- Précision absente du rapport : après l'annulation, l'emplacement est `Off`, donc le toucher **suivant** remet l'horloge à 0
  (l. 119) et part tout de suite : d'où « 0, (annulé), 999 » de la sonde, cohérent.
- Précision absente du rapport : la grille suit le **tempo du projet**, le défi le tempo **borné 80-120** ; sur un projet à
  140 BPM la grille (429 ms) ne coïncide même pas avec la pulsation du défi (500 ms).
- Chemin vérifié : `PadLiveController.touch` → `PadLive.touch` (« coup » : `TOGGLE` à chaque appui) → `RhythmViewModel.toggleLive`
  → `NativeEngine.toggleClip` → `Engine.cpp` l. 340 `clips_.toggle(a, !sequencer_.running(), value)`.

## Constats

| Gravité | Constat | Suite |
|---|---|---|
| Grave → **corrigé** | **Le vrai défaut de Benjamin (moteur) n'est suivi nulle part.** Ni id, ni ligne `verdicts.tsv`, ni garde `engine/host-tests` (la sonde est hors dépôt, seulement recopiée dans le rapport), ni ligne `PLAN-RESTE.md`. De même T5-DEFI-TENDANCE et T5-DEFI-ECOUTE n'ont pas de ligne `verdicts.tsv`, alors que `KnownDefectRule` renvoie explicitement à « its verdicts.tsv line ». `T5-Ca-32` reste « ouvert » avec l'ancienne cause (« c' »), alors que le critère d'acceptation de 4.1 est « T5-Ca-32 tranché ». Risque : la correction moteur se perd et tel-17 revient. | Avant fusion : 3 lignes `verdicts.tsv` (T5-DEFI-TENDANCE, T5-DEFI-ECOUTE, un id moteur type T5-COUP-GRILLE avec `ClipLauncher.cpp` l. 115-124), T5-Ca-32 mis à jour (banc + appli + moteur), une ligne `PLAN-RESTE.md` « coup relancé tout de suite lecture arrêtée, garde host-tests rouge/verte ». Si le fichier partagé a un propriétaire unique, le lui demander. |
| Moyen → **corrigé** | **Le banc tape désormais comme un métronome** : `passUntil` place chaque toucher à ±8 ms de `début + i × intervalle`, et la ligne de son lui donne la pulsation en millisecondes. Un humain n'a ni l'un ni l'autre (l'écran **cache** même le « ♩ N » quand il diffère du tempo affiché, `TempoLight`). Le banc ne peut plus révéler un défi trop sévère pour une vraie main ni un défi dont on ne trouve pas la pulsation à l'oreille. | Ajouter une gigue humaine reproductible (±30-40 ms, graine fixe) à `tape`, et ne dire la pulsation que comme l'écran (BPM seulement quand il est affiché). |
| Moyen → **corrigé** | **`tape` pendant l'exemple attend « À toi ! » tout seul** : c'est le banc qui décide pour le testeur. Cela masque côté banc T5-DEFI-ECOUTE (un testeur qui enchaîne sur l'exemple, comme Benjamin, ne perd plus rien). Défaut gardé par le test unitaire, mais les sièges ne peuvent plus le rencontrer. | Ne pas attendre : dire « l'exemple joue encore, ce toucher ne compte pas » et taper quand même ; ou au moins le noter comme limite du banc. |
| Moyen → **corrigé** | **Le banc ne passe jamais par la cause (d)** : les deux tests du banc tournent à 120 BPM, où `tempo` = 500 ms = l'ancienne valeur fixe. Le rouge de d00af4c ne prouvait que l'absence de la ligne « pulsation » et du mot `tempo`, pas qu'un testeur à 90 BPM s'en sort. | Un cas banc sur un projet à 90 BPM (pulsation 667 ms) avec `tape … tempo`, 3 étoiles. |
| Moyen → **corrigé en partie** (reste mineur) | **Tolérance non bornée par le haut dans les tests normaux** : élargir `TOLERANCE_BEATS` à 0,5 ne fait rougir que la garde T5-DEFI-TENDANCE ; quand elle sera corrigée et retirée, plus rien. Et `fiveHundredMs…` repose sur deux comparaisons pile à la tolérance (0,75 et 2,25 temps, flottants à la limite) : déterministe mais fragile au moindre arrondi d'horloge. | Ajouter un cas net hors tolérance (toucher à 0,4 temps de sa place : non compté) et un net dedans (0,1) ; ne pas faire dépendre « 3 sur 4 » d'une égalité flottante. |
| Mineur → suivi (PLAN-RESTE R.7) | `Challenge.kt` (`example()`) dit « a hit tapped again starts over, as on the engine » : c'est faux d'après `ClipLauncher::toggle` (le coup attend la ligne, ou est annulé). L'exemple entendu et le retour du pad diffèrent donc. | À corriger avec la correction moteur (commentaire ou comportement). |
| Mineur → ouvert | `pass(ms)` déborde toujours ailleurs (`attends`, `touche`, `appui-long`) ; noté dans le rapport, non corrigé. | Ligne au journal du banc fiable ; mesurer l'écart sur `appui-long`. |
| Mineur → ouvert (la garde moteur existe désormais) | Les tests « pad qui sonne » (`touchesCloserThanTheClap…`, `aHumanHand…`) ne prouvent que le côté Kotlin (le moteur est remplacé par `toggleForTest`) ; leur nom et leur KDoc laissent croire qu'ils couvrent la remarque de Benjamin. | Nommer « …AreAllCountedByTheChallenge » et renvoyer à la future garde host-tests. |
| Info | Aucun code principal modifié ; pas de nouvelle couture d'horloge ; arbre propre après expériences ; pas de dépendance au temps réel dans `ChallengeControllerTest` (horloge injectée, `idleFor`). | — |

## Suite de la relecture (delta c034963..4575c0d)

Commits relus : 63a2f8f (fusion de fx-int, fichiers égaux à fx-int ignorés), b91b3a8, eb14aaa, bc02b31, f7f5002, 4575c0d.
Toujours aucun code principal modifié (`app/src/main`, `engine/src`, `project-model/src` : diff vide).

- **Grave, corrigé.** `verdicts.tsv` : T5-DEFI-TENDANCE, T5-DEFI-ECOUTE, T5-PADRETRIG (moteur, `ClipLauncher::toggle`
  l. 112-124, recette tel-17) avec leurs gardes ; T5-Ca-32 tranché (preuves `ChallengeControllerTest` + `BenchAmeneesTest`).
  `PLAN-RESTE.md` R.7 (correction moteur + commentaire `example()`). Garde `engine/host-tests`
  `padlive_known_defect_t5_padretrig` : lancée, **OK** ; mutation locale (un « coup » relancé tout de suite :
  `startBeat = beat_` pour un one-shot) : **3 échecs** (l. 426, 439, 454), annulée. Les trois cas (90 BPM clap long,
  croches avec annulation puis remise à zéro, grille à 429 ms sur un projet à 140 BPM) collent à la lecture du code.
  Garde « à l'envers » (vérifie le comportement d'aujourd'hui) : acceptable, le commentaire et la ligne R.7 disent de l'inverser.
- **Banc, corrigé.** `tape` : gigue ±30 ms tirée d'un `Random` fixé par le siège et le numéro du geste (rejouable) ;
  plus d'attente de « À toi ! » ; la ligne de son dit « pulsation ~N BPM » (+ « exemple en cours »), plus de millisecondes.
  `BenchAmeneesTest` 9/9 (+1 `drive` ignoré) : 120 BPM 3 étoiles avec gigue, **90 BPM** (`tempo` = 666 ms) 3 étoiles,
  touchers pendant l'exemple : `YourTurn(c, 0)` puis 3 étoiles une fois « À toi ! ».
- **Tolérance, corrigé.** `aTouchFarFromItsPlaceIsNotCountedANearOneIs` : refait la mutation `TOLERANCE_BEATS` 0,5 :
  ce test rougit lui-même (plus seulement la garde). `ChallengeControllerTest` 8/8.

## Restes (mineurs, non bloquants)

| Gravité | Constat | Suite |
|---|---|---|
| Mineur | `fiveHundredMsTapsAreScoredOnTheProjectsTempo` dépend toujours de deux comparaisons flottantes pile à la tolérance (0,75 et 2,25 temps). Déterministe, mais un arrondi d'horloge le fera basculer. | Le décaler de quelques ms (par ex. 490 ms) ou le documenter comme cas limite voulu. |
| Mineur | `aTappedRhythmAtNinetyBpmIsScored` règle le tempo par `rhythm()!!.setTempo(90.0)` (appel direct, pas un geste). Acceptable pour une mise en place de test, mais ce n'est pas le chemin d'un siège. | Passer par le réglage de tempo à l'écran si un geste existe. |
| Mineur | La ligne de son donne « ~N BPM » même quand l'écran cache le « ♩ N » (`TempoLight`, tempo du projet hors 80-120). Information un peu plus riche que celle d'un humain. | Ne la dire que quand l'écran l'affiche, sinon « suis l'exemple ». |
| Mineur | `pass(ms)` déborde toujours ailleurs ; noms des tests « pad qui sonne » inchangés. | Voir le tableau plus haut. |

Verdict : 0 bloquant, 0 grave — fusion possible
