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
Sequencer ──(pas, à l'échantillon près)──► DrumKit ─► tampon Batterie ─┐
                                      └──► AcidTrack (Open303) ─► tampon Acide ──┤
Clavier (file d'événements) ─────────────► Synth 8 voix ─► tampon Synthé ─┤
                                                                       ▼
Metronome + son de test ─► tampon direct ──► Mixer (volume, pan, muet, solo,
                                              limiteur master) ─► sortie stéréo
```

## Livraison
`.github/workflows/ci.yml` : tests moteur (avec ASan/UBSan) → tests Kotlin + captures → APK signé → pré-release `preview` (PR) ou Release `v0.<phase>.<run>` (main). Test de fumée sur émulateur x86_64 en parallèle.
