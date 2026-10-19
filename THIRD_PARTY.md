# Briques tierces

| Brique | Version | Licence | Usage |
| --- | --- | --- | --- |
| [Oboe](https://github.com/google/oboe) | 1.11.0 | Apache-2.0 | Flux audio temps réel (AAudio, exclusif, basse latence) |
| [DaisySP](https://github.com/electro-smith/DaisySP) | commit 2c72eaf (2026-09-28, après V1.0.0) | MIT | Voix de batterie (grosses caisses, caisses claires, charlestons), filtres SVF/ladder, oscillateurs ; sous-module `engine/third_party/DaisySP` |
| [libsamplerate](https://github.com/libsndfile/libsamplerate) | 0.2.2 (dernière version) | BSD-2-Clause | Rééchantillonnage haute qualité des sons importés vers 48 kHz (hors fil audio) ; sous-module `engine/third_party/libsamplerate` |
| [Open303](https://github.com/RobinSchmidt/Open303) | commit 313bf0d (2024-03-29) | MIT | Basse acide (jalon Acide) ; sous-module `engine/third_party/Open303`, une correction de débordement appliquée sur une copie à la compilation (`open303.cmake`) |
| [The Endless Acid Banger](https://github.com/vitling/acid-banger) (David Whiting) | 2021 | CC BY 4.0 | Méthode et jeux de notes du générateur de patterns acides, réécrits en Kotlin (`AcidGenerator`) ; attribution obligatoire |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM) | 2026.09.00 | Apache-2.0 | Interface |
| AndroidX Activity / Lifecycle / Core | 1.13.0 / 2.11.0 / 1.19.1 | Apache-2.0 | Socle Android, ViewModel |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | 1.11.0 | Apache-2.0 | Format JSON des projets |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | 1.11.0 | Apache-2.0 | Tâches asynchrones |
| [Robolectric](https://github.com/robolectric/robolectric) | 4.17 | MIT | Tests sur JVM (tests uniquement) |
| [Roborazzi](https://github.com/takahirom/roborazzi) | 1.76.0 | Apache-2.0 | Captures d'écran automatiques (tests uniquement) |

Prévu dans les phases suivantes (à vérifier au moment de l'intégration) : Airwindows (MIT), Mutable Instruments (MIT/GPL), MSFA/Dexed (Apache-2.0/GPL), Signalsmith Stretch (MIT), aubio (GPL-3.0), libKeyFinder (GPL-3.0), ONNX Runtime (MIT), HT-Demucs (MIT), Basic Pitch (Apache-2.0), Ableton Link (GPL-2.0), LiteRT (Apache-2.0).
