# Architecture

```
┌──────────────────────────── app (Kotlin, Compose) ────────────────────────────┐
│  Écrans : Studio · Latence · Recette      ViewModels (état → écran, flux unidirectionnel) │
│  Rapports GitHub (recette, bug, crash)    Mises à jour (GitHub Releases + PackageInstaller) │
└───────────────┬───────────────────────────────────────────────▲──────────────┘
                │ commandes (JNI → file SPSC lock-free)          │ mesures (atomiques)
┌───────────────▼──────────────── engine (C++20) ───────────────┴──────────────┐
│  android/  AudioHost : flux Oboe (AAudio exclusif, LowLatency), reconnexion,   │
│            entrée micro pour la mesure aller-retour, pont JNI                  │
│  core/     Engine : graphe audio, lissage des paramètres, compteur de crêtes   │
│            RoundTripMeter : salve de bruit + corrélation croisée               │
│            SpscQueue : file sans verrou                                        │
│            → indépendant d'Android, testé sur Linux (engine/host-tests)        │
└──────────────────────────────────────────────────────────────────────────────┘
ui-components : thème sombre néon, cartes, VU-mètre (réutilisés par tous les écrans)
```

## Modules Gradle
| Module | Rôle | Statut |
| --- | --- | --- |
| `app` | Activité unique, écrans, rapports, mises à jour | Phase 0 |
| `engine` | Moteur C++ + JNI | Phase 0 |
| `ui-components` | Thème et composants | Phase 0 |
| `project-model` | Projet JSON (Kotlin pur), écritures atomiques + secours, annuler/rétablir, sauvegarde auto | Phase 1 (1.1 fait) |
| `dsp` | Briques DSP tierces (DaisySP, Airwindows…) | Phase 1 |
| `separation` | StemSeparator, Demucs ONNX | Phase 3 |

## Règles du thread audio
Aucune allocation, aucun verrou, aucun log dans `Engine::process`. Tout ce qui vient de l'interface passe par `SpscQueue<Command>`.
Le séquenceur vit dans `core/` et avance à l'échantillon près dans le callback.

## Graphe audio (phase 1)
```
Sequencer ──(pas, à l'échantillon près)──► DrumKit ou Sampler (son perso du pad) ─► tampon Batterie ─┐
                                      └──► AcidTrack (Open303) ─► tampon Acide ──┤
Clavier (file d'événements) ─────────────► Synth 8 voix ─► tampon Synthé ─┤
                                                                       ▼
Metronome + son de test ─► tampon direct ──► Mixer (volume, pan, muet, solo,
                                              limiteur master) ─► sortie stéréo
```

## Mode Live (phase 27)
- Moteur : 15 « slots », un par piste (Batterie 0, Acide 1, pistes instrument 2..9, pistes audio 10..13, Remix 14). Chaque slot compte depuis son propre départ (`slots_[k].start`) : la batterie, la 303, chaque piano roll et chaque piste audio peuvent jouer des scènes différentes en même temps.
- `ArmLaunch` (masque de slots, quantum en pas, scène ou -1 = arrêt) : la part du slot est d'abord envoyée dans la scène en attente (`SetStaging`), puis prend la main sur le premier pas multiple du quantum, avant que ce pas sonne. Arrêter = vider le contenu du slot (même son, échantillon pour échantillon, que lancer une part vide) ; le slot Remix coupe le chant séparé sans perdre sa place. Le masque complet = une ligne (la scène change, `sceneSwaps`). `ArmSceneSwap` (morceau, choix de scène) annule les lancements armés. `CancelLaunch`, `SetSlotOffset` (départ en cours de part, pour la prise de la phase 27.3), `SetBusReturn` (retours réverbe/écho du Live).
- Moteur → interface : `SpscQueue<LaunchEvent, 256>` (slot, scène, pas) lue par le ViewModel ~30 fois/s, et des atomiques par slot (`slotStates`, `transportStep`) pour se recaler si la file a débordé. Sans lancement, tous les slots partent avec la scène : export et lecture inchangés.
- App : `model/Live.kt` (réducteurs purs `LiveState`, contenu des cases, quantum sauvé `Project.liveQuantum`), `RhythmViewModel` section « Live grid » (tous les gestes passent par lui : lancement, arrêt, muet M non écrit, gestes 303 et retours non écrits jusqu'au Stop ; `waitsFor(slot)` : une piste qui joue une autre scène que celle éditée ne reçoit pas les modifications), `app/live/` (LiveScreen, LiveFx, LiveAcid), ouverte depuis Morceau en plein écran.

## Livraison
`.github/workflows/ci.yml` : tests moteur (avec ASan/UBSan) → tests Kotlin + captures → APK signé → pré-release `preview` (PR) ou Release `v0.<phase>.<run>` (main). Test de fumée sur émulateur x86_64 en parallèle.

## Sons perso (phase 2)
`Sample` (core) : données float 48 kHz immuables, comptées par références. Créé hors fil audio (JNI `sampleCreate`, rééchantillonnage libsamplerate), installé sur un pad par une commande qui porte une référence. Le fil audio ne libère jamais : un son dont le compte tombe à zéro part dans une file SPSC que le fil principal vide (`Engine::collectGarbage`). Le moteur d'export reçoit les mêmes sons par `NativeEngine.captureInto`.

## Styles de l'assistant (recettes)
Les 37 styles de « Nouveau morceau, pas à pas » sont écrits en JSON (`scripts/packs/recipes/*.json`) et convertis en Kotlin (`StyleRecipes.kt`, à ne jamais modifier à la main) par `scripts/packs/recipes_to_kotlin.py`, qui vérifie chaque valeur. Le test `StyleRecipesUpToDateTest` échoue si le Kotlin n'est pas celui du script. La syntaxe complète est en tête du script ; l'essentiel :
- Un caractère par double croche (16 = une mesure, 32 = deux). Batterie : `X` fort, `x` moyen, `o` doux, `.` rien, `2` `3` `4` roulement de 2 à 4 coups dans le pas (`Hit.repeats`). `voice` : la voix du kit intégré (shaker, conga, cowbell, toms…) quand aucun pack n'est là.
- Basse, riff et ligne 303 en degrés de l'accord : `R`, `2`…`7`, `O` (minuscule = plus doux), `-` tient, `.` silence, `/` tient et glisse vers la note suivante (les notes se chevauchent : glissé du lecteur d'échantillons pour une 808, portamento `GLIDE` du synthé si `"glide": true`). `"sub": true` sur la basse (jump up) : une piste « Sub » (`StarterWizard.SUB_TRACK`, son « Sub pur », pompe sur le kick) rejoue les notes de la basse une octave plus bas, là où joue la basse : le growl au milieu, le sinus propre dessous.
- Sons : préréglage, `pack:<pack>:<sorte>` (sans le pack : un remplaçant intégré, « Basse ronde » pour une basse), `pack:<pack>:<sorte>|<préréglage>` (le son du pack d'abord quand il est là, sinon ce préréglage), `gm:<nom>` (remplaçant intégré), `kit:cowbell` (voix du kit jouée au clavier). Un son de pack enregistré en accord (`stab`, `pad`, `keys`) joue une seule note par accord (`PartSound.chord`). Le lead glisse aussi (`"glide": true` sur `lead`, avec `/` dans son riff).
- `defaults` (parties cochées), `default_mood`, `signature` (la partie qui joue dans toutes les sections), `form` (`song` : Intro/Couplet/Refrain ; `club` : Intro/Groove/Drop + Break, par défaut pour House, Techno et Bass music), `bass_tip`/`pad_tip`/`lead_tip`, `fx` (préréglage d'effets, insert, envois réverbe/écho sur une partie, une ligne, l'acide ou la batterie), `references` (toutes affichées, la plus connue d'abord).
- Les listes de sons, de préréglages d'effets et de types d'effets acceptées sont lues dans le code (`SoundLibrary.kt`, `FxPresets.kt`, `Effects.kt`) ; `--names` les affiche, un test les compare au code.
- Ligne 303 (`acid`) : une ou deux mesures (16 ou 32 pas), `sound` (un son 303 de `SoundLibrary.ACID`), `knobs` (réglages par-dessus), `moves` (un bouton tourné au fil du morceau : pour chaque sorte de section, sa valeur au début et à la fin, écrites en courbe d'automation « Morceau » du bouton de la 303, `AutoTarget.ACID` ; l'outro rejoue l'intro à l'envers). Ligne de batterie : `build` (son motif dans la 2e section). `form: rave` (acidcore) : la 303 seule en intro, Groove, Drop, Break, Drop, petit Break, Drop, outro. `build: true` (forme `SongForm.Build`, bass music et gabber) : le Groove devient une Montée de 8 mesures (scène de 4 mesures, 64 pas) sans kick ni basse où la caisse claire, ou le clap, roule de plus en plus vite (`rollHits`), avant chaque Drop : Intro, Montée, Drop, Break, Montée, Drop, Intro. `drop` : les couches gardées pour le Drop (la basse du speed garage, le charley ouvert de la techno) ; `break` : ce que joue le Break (la tech house garde basse et percus ; deep et afro house : le Drop sans kick ; jamais le kick). `fx` sur `master` : inserts seulement, `params` pour régler un insert. Lignes de batterie en plus : `tom` (pad 8), `ride` (pad 12, la cymbale du kit : aucun pack n'a de ride), `perc2` (pad 13, shaker par défaut), `rumble` (pad 3 : le kick rejoué, qui joue là où joue le kick, pour le préréglage d'effets `rumble` : réverbe seule, distorsion, passe-bas vers 150 Hz, pompe, mono) ; `off` : ligne proposée décochée. `riser: true` : une piste « Montée » (souffle de bruit blanc, `RISER_SOUND`) dont une courbe « Morceau » ouvre le filtre passe-bas sur les 4 mesures avant chaque Drop (toute la Montée en forme Build), grand ouvert sur la première mesure du Drop (l'impact) puis refermé ; filtre fermé hors du morceau, donc muette quand on boucle une section. `sweeps` : un passe-bas sur une couche (basse, accords, lead, 303, master) dont la coupure suit une courbe « Morceau » par section, comme les `moves` de la 303 ; le filtre est réglé comme au Drop quand on boucle une section. Ordre des courbes (8 au plus) : boutons de la 303, montée, filtres.
- Construction : `StarterWizard.build` → `planFor`/`scenePlan` (qui joue dans quelle section) → `sectionsOf`/`songOf` (Couplets et Refrains de 16 mesures, Break de 8, morceau de 3 min 30 au plus en raccourcissant l'intro) → `withEffects` (effets de la recette sur les voies utilisées).
- Enregistrer la performance (27.3) : `SongPart` porte en option `play` (scène par piste), `from` (mesures déjà jouées par piste) et `bars` (longueur propre) ; `Project.partSteps(part)` remplace partout « longueur de scène × répétitions » (lecture, export, MIDI, courbes et prises qui suivent l'arrangement). `partView` compose la part de chaque piste ; `pushPart` envoie un `SetSlotOffset` par slot (0 sans `from`, aussi au départ du transport) : la lecture du morceau et l'export reprennent chaque piste là où elle en était. `MarkSong` (commande 75) fixe la position du morceau au début de la prise (sur le quantum) : les courbes du morceau entendues pendant la prise sont celles du morceau écrit. `model/Performance.kt` (`PerformanceTake`, `Project.writeTake`) découpe la prise à chaque lancement (parties identiques qui se suivent fusionnées) et écrit les gestes en courbes « Morceau » à un point par temps (`songPerBeat`, 1024 points ; `hold` : marches pour Répéter/Freinage/Hachoir), 16 lignes d'automation, sortes 6 MACRO (macros + hachoir de voie, `Mixer::kChopMacro`) et 7 LIVE (PerformanceFx + retours), appliquées sur la trame exacte du pas (`Mixer::scheduleSetting`). ViewModel : section « Enregistrer la performance » (● Prêt → Armé → REC → Écriture → Fait, une seule étape d'annulation, Annuler bloqué pendant la prise, latence de sortie retirée des gestes). Test hôte `performance_song_matches_the_take` : le morceau écrit sonne échantillon pour échantillon comme la prise.

## Ligne du temps (phase 31)
- Modèle : `model/SongTimeline.kt` (pur) : `clipPreview` (ce qu'une piste joue dans une scène, en petites marques), `partSpans`/`partAtStep`, `splitSongPart`/`splitBar`, `songNextIndexLooped` (boucle sur des parties), `songLanesOf` (courbes « Morceau » par piste), `channelOfKey`/`keyOfChannel`.
- App : `session/TimelineScreen.kt`, ouverte en plein écran depuis Morceau (`showTimeline` dans `AppRoot`, `TimelineRoute`). Règle et grille dessinées dans des `Canvas` qui partagent `TimelineView` (zoom en px par mesure, défilement) ; la tête de lecture et le halo sont sur un calque à part, lus au dessin. Phase 32 : `TimelineScroller` est le seul à faire défiler (doigt, zoom, suivi par pages via `followScroll`) ; hauteur des pistes par `timelineRowHeight`, couleurs par `timelineColors`, aperçus typés (`ClipPreview.kind`). ViewModel : `splitSongPart`, `setSongLoop`/`songLoop` (non sauvé), `showSceneToEdit`.

## Banc de mesure du son (phase 33)
- `NativeEngine.sink` (null dans l'appli) reçoit les appels au moteur à la place de la JNI. Le test `SongBenchExport` (app) enregistre ainsi l'export réel de chaque style en `.pscmd`. `engine/bench/ps_render` le rejoue dans le cœur sur Linux, `ps_measure` le mesure (libebur128). Tout se lance par `scripts/song-bench.sh`. Détails et rapports : `docs/son/BANC.md`.
- Gain staging (étape 1) : réduction de gain de chaque compresseur par atomiques (`Mixer::takeInsertReduction`, JNI `insertReductions`), lue avec les vu-mètres ; version du kit par projet (`Project.kitVersion`, commande `SetKitVersion`).

## Chaîne master et sonie (phase 33)
- `core/MasterChain` (dans le `Mixer`, après le gain du master ; le clic du métronome entre après le gain de la cible) : gain de la cible de sonie → écrêteur ClipOnly2 à 4× (HIIR, `core/Oversampling.h`) qui ne prend que l'attaque des crêtes → limiteur true peak à anticipation (plafond −1 dBTP) → mesure (`core/Loudness.h` : LUFS instantané, 3 s, intégré depuis ▶, true peak, publiés par atomiques). Retard fixe `latency()` (68 trames à 48 kHz, 1,42 ms), retiré par l'export ; sous le plafond et sans cible, la sortie est l'entrée retardée à l'échantillon près.
- Cible : commande `SetMaster` 4 (LUFS), 5 (plafond dBTP), 6 (oublier l'estimation). En direct le gain suit une estimation lente ; l'export (`OfflineRenderer`) rend la musique sans la chaîne (`Mixer::setMasterChainBypass`) dans un fichier temporaire ou le tampon, puis la masterise au gain exact (deux passes de la chaîne seule). Sortie : fondus, 44,1 kHz (libsamplerate), 16/24 bits TPDF ou flottant, rapport (`report()`).
- Saturation et Distorsion : `core/DriveShaper.h` (4× HIIR + antidérivée).
- App : `Mixer.target` / `Project.style` (modèle), `LoudnessTargets` (cibles par style), `mixer/MasterLoudness.kt` (lecture « −8,1 LUFS · TP −1,2 », puces « Volume visé », rapport d'export), `NativeEngine.loudnessMeters`.
