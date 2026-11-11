# Relecture du chantier 7 « Pad Live » (tour 5, étape 4)
Relu : c927f7def7eef922cabc9cec64d5d48aefe02596

Relecteur neuf (ni auteur du code). Diff lu : `git diff bb4f1a26...HEAD` (moteur `ClipLauncher`, `RhythmViewModel`, `PadLiveScreen`, `AppRoot`, aides 14 langues, tests, captures), le rapport de l'auteur et les 6 lignes de DECISIONS.

## Moteur (priorité, lu ligne à ligne)
- `ClipLauncher::toggle` tourne dans le fil audio (vidage de la file, `Engine.cpp` `ToggleClip`). Ajout : un `std::none_of` sur les 64 cases avec un lambda sans capture allouée, des affectations de champs, un `store` relaxé sur l'atomique d'état déjà publiée. Ni allocation, ni verrou, ni log, ni JNI. Aucune nouvelle commande ni file : l'UI parle toujours par la SpscQueue (`post` → `nativePost`, 100 ms d'attente puis `droppedCommands`, inchangé).
- « Coup » seul, lecture arrêtée : `Off` → l'horloge repart à 0 (comme avant) ; `Playing` → `startBeat = beat_`, `Waiting`, et `render()` le relance au bloc suivant (`restart` fait fondre l'ancien passage, pas de clic). L'horloge des mesures n'est pas remise à zéro sur un retoucher : les lignes de mesure de « Enregistrer ce que je joue » restent justes.
- Boucles allumées, lecture arrêtée : un coup en attente n'est plus annulé (branche vide, état republié tel quel). Lecture en marche (`idleClock` faux) : rien ne change, donc les prises de perf (`LivePadTake`, sur le temps du transport) restent fidèles au moteur.
- `stopAll` (« Tout couper ») n'est pas touché ; toutes les cases repassent à `Off`, la case suivante relance l'horloge à 0. État remis à zéro : oui.
- `./scripts/host-tests.sh` : ALL PASSED (0 failures), 6 min 39.

## « Tout couper » coupe-t-il tout ?
- `cutEverything` = ■ du transport (s'il joue) puis `stopLive`. Le ■ du transport finit proprement ce qui enregistre : prise ● des notes (`stopRecordingTake`, gardée, un pas d'annulation), prise de perf (`endPerformanceOnStop` → `finishPerformance`, gardée), prise micro (le suivi voit le transport arrêté → `stopAudioRecording`, écrite par `TakeVault`). Aucune perte de prise.
- « Enregistrer ce que je joue » (rééchantillonnage moteur) : le transport arrêté ne le termine pas ; la prise continue, et se termine au toucher suivant sur une ligne de mesure (`clips_.render` tourne à chaque bloc, l'horloge des pads aussi) ou quand le tampon est plein. Rien n'est perdu (voir M1).

## Gestes
- `HeldEffect` : chaque chemin qui a allumé un effet l'éteint (glissé précoce : `holdNow(false)` ; fin du geste ou annulation : `finally` avec `on`). Montée ne part qu'au lâcher sans glissé. Aucun effet bloqué trouvé, y compris quand le parent consomme le défilement.

## Mutations
- Moteur M1 : `if (s.oneShot && alone && s.state != Playing)` (un coup qui sonne retouché ne repart plus tout de suite) → `padlive_hit_retouched_sounds_at_once` rouge (8 échecs). Tuée, restaurée.
- Moteur M2 : la branche « jamais annulé » retirée (`else if (false)`) → même test rouge (2 échecs, l. 478 et 480). Tuée, restaurée.
- Kotlin M3 : `cutEverything` sans le ■ du transport → `BenchPadLiveStopTest#padLivesStopAllIsInSightAndStopsTheTempoToo` rouge (« la musique du projet joue en boucle »). Tuée, restaurée.
- Kotlin M4 : glissé précoce sans `holdNow(false)` → `PadLiveGlideTest#aHeldEffectLetsGoAsSoonAsTheFingerGlides` rouge (`[brake true]`). Tuée, restaurée.

## Tests JVM ciblés (via `scripts/bench/gradle-file.sh`)
PadLiveGlideTest, BenchPadLiveStopTest, BenchPadLiveTest, Tour5AppProof3Test, Tour4AppProofTest, BenchTour4Test, PadLiveSceneTest, PadLiveTouchCountTest, HelpCoverageTest : 57 cas, 0 échec (2 « drive » du banc ignorés comme prévu).

## Captures regardées
padlive, padlive_more, padlive_small, padlive_recording, sizes/expanded_padlive : carré rouge « Tout couper » entier sur deux lignes, « Enregistrer ce que je joue » sur deux lignes sans coupure, « Plus » sans « Tout couper ». « F… » de padlive_small déjà présent à la base (pas de ce lot).

## Points restants (aucun grave)
| n° | gravité | constat | suite |
|---|---|---|---|
| M1 | moyen | Pendant « Enregistrer ce que je joue » (bouton voisin), « Tout couper » laisse la prise tourner : des mesures de silence s'ajoutent jusqu'au toucher suivant ou au plein. Rien n'est perdu. | Faire finir la prise sur sa ligne (`played.press()` si elle enregistre, `cancel()` si armée) dans `cutEverything`. |
| M2 | mineur | DECISIONS dit « ■ blanc » ; le code et les captures montrent un ■ noir (contraste 2,8:1 du blanc). | Corriger la ligne. |
| M3 | mineur | `Tour4AppProofTest` : la phrase « musique arrêtée » n'est vérifiée que par l'absence de mots ; c'est l'assertion sur le transport qui porte la preuve. | Chercher une phrase positive. |
| M4 | mineur | Montée s'allume dès le doigt posé alors qu'elle ne part qu'au lâcher (elle s'éteint sur un glissé). | Allumer au lâcher seulement. |
| M5 | mineur | Lecture arrêtée, une boucle allumée : un coup en attente ne peut plus être annulé en le retouchant (choix R.7 assumé). | Rien, noté. |

## Tests à rejouer sur téléphone
tel-08, tel-11, tel-12, tel-17 (comme le rapport), plus : « Enregistrer ce que je joue » lancé, puis « Tout couper », puis toucher le bouton : la prise doit se poser (M1).

Verdict : 0 bloquant, 0 grave — fusion possible
