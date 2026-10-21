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

## Livraison
`.github/workflows/ci.yml` : tests moteur (avec ASan/UBSan) → tests Kotlin + captures → APK signé → pré-release `preview` (PR) ou Release `v0.<phase>.<run>` (main). Test de fumée sur émulateur x86_64 en parallèle.

## Sons perso (phase 2)
`Sample` (core) : données float 48 kHz immuables, comptées par références. Créé hors fil audio (JNI `sampleCreate`, rééchantillonnage libsamplerate), installé sur un pad par une commande qui porte une référence. Le fil audio ne libère jamais : un son dont le compte tombe à zéro part dans une file SPSC que le fil principal vide (`Engine::collectGarbage`). Le moteur d'export reçoit les mêmes sons par `NativeEngine.captureInto`.

## Styles de l'assistant (recettes)
Les 36 styles de « Nouveau morceau, pas à pas » sont écrits en JSON (`scripts/packs/recipes/*.json`) et convertis en Kotlin (`StyleRecipes.kt`, à ne jamais modifier à la main) par `scripts/packs/recipes_to_kotlin.py`, qui vérifie chaque valeur. Le test `StyleRecipesUpToDateTest` échoue si le Kotlin n'est pas celui du script. La syntaxe complète est en tête du script ; l'essentiel :
- Un caractère par double croche (16 = une mesure, 32 = deux). Batterie : `X` fort, `x` moyen, `o` doux, `.` rien, `2` `3` `4` roulement de 2 à 4 coups dans le pas (`Hit.repeats`). `voice` : la voix du kit intégré (shaker, conga, cowbell, toms…) quand aucun pack n'est là.
- Basse, riff et ligne 303 en degrés de l'accord : `R`, `2`…`7`, `O` (minuscule = plus doux), `-` tient, `.` silence, `/` tient et glisse vers la note suivante (les notes se chevauchent : glissé du lecteur d'échantillons pour une 808, portamento `GLIDE` du synthé si `"glide": true`).
- Sons : préréglage, `pack:<pack>:<sorte>`, `gm:<nom>` (remplaçant intégré), `kit:cowbell` (voix du kit jouée au clavier).
- `defaults` (parties cochées), `default_mood`, `signature` (la partie qui joue dans toutes les sections), `form` (`song` : Intro/Couplet/Refrain ; `club` : Intro/Groove/Drop + Break, par défaut pour House, Techno et Bass music), `bass_tip`/`pad_tip`/`lead_tip`, `fx` (préréglage d'effets, insert, envois réverbe/écho sur une partie, une ligne, l'acide ou la batterie), `references` (toutes affichées, la plus connue d'abord).
- Les listes de sons, de préréglages d'effets et de types d'effets acceptées sont lues dans le code (`SoundLibrary.kt`, `FxPresets.kt`, `Effects.kt`) ; `--names` les affiche, un test les compare au code.
- Ligne 303 (`acid`) : une ou deux mesures (16 ou 32 pas), `sound` (un son 303 de `SoundLibrary.ACID`), `knobs` (réglages par-dessus), `moves` (un bouton tourné au fil du morceau : pour chaque sorte de section, sa valeur au début et à la fin, écrites en courbe d'automation « Morceau » du bouton de la 303, `AutoTarget.ACID` ; l'outro rejoue l'intro à l'envers). Ligne de batterie : `build` (son motif dans la 2e section). `form: rave` (acidcore) : la 303 seule en intro, Groove, Drop, Break, Drop, petit Break, Drop, outro. `fx` sur `master` : inserts seulement, `params` pour régler un insert.
- Construction : `StarterWizard.build` → `planFor`/`scenePlan` (qui joue dans quelle section) → `sectionsOf`/`songOf` (Couplets et Refrains de 16 mesures, Break de 8, morceau de 3 min 30 au plus en raccourcissant l'intro) → `withEffects` (effets de la recette sur les voies utilisées).
