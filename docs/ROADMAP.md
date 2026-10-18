# Feuille de route

Chaque jalon ≈ une session de travail. Chaque jalon livre un APK d'aperçu, la liste de recette à jour dans l'appli et son changelog.
Feu vert de Benjamin (D4 = A) : fusion de la pull request en fin de phase.

## Phase 0 : fondations
### Jalon 0.1 : squelette et CI
- [x] Projet Gradle (app, engine, ui-components), Kotlin + Compose, minSdk 31, targetSdk 37, arm64-v8a
- [x] Cœur moteur C++20 indépendant d'Android + tests Linux
- [x] GitHub Actions : tests moteur, build, captures, aperçu sur PR, Release sur main
- [x] Signature stable (keystore chiffré + secret)
- Critères : la CI est verte ; `releases/download/preview/pocket-studio-preview.apk` s'installe sur le Honor.

### Jalon 0.2 : son et latence
- [x] Oboe (AAudio, exclusif, LowLatency, performance soutenue) qui joue une sinusoïde réglable
- [x] Compteur de décrochages audio (xruns) et infos du flux (fréquence, taille de buffer, mode)
- [x] Écran Latence : latence de sortie estimée + mesure aller-retour (haut-parleur → micro)
- Critères : sinusoïde audible, sans craquement, 0 décrochage en 1 minute ; latence affichée.

### Jalon 0.3 : outils de recette
- [x] Écran Recette (✅/❌ + commentaire) qui ouvre une issue GitHub pré-remplie
- [x] Bouton « Signaler un problème »
- [x] Rapport de crash proposé au lancement suivant
- [x] Vérification de mise à jour au lancement + installation par-dessus en un geste
- Critères : un rapport envoyé depuis le téléphone arrive en issue avec version, appareil, latence, décrochages et journal.

## Phase 1 : MVP jouable
- [ ] 1.1 Modèle de projet (module project-model, JSON, écritures atomiques, sauvegarde auto, annuler/rétablir)
- [ ] 1.2 Transport : tempo 40-300 BPM, tap tempo, swing, métronome, décompte ; séquenceur à l'échantillon près
- [ ] 1.3 Boîte à rythmes 16 pas, 16 pads, kit 808/909 synthétisé (DaisySP), vélocité
- [ ] 1.4 Synthé soustractif (2 osc, sub, bruit, ladder + SVF, 2 env, 2 LFO), clavier simple
- [ ] 1.5 Mixeur simple (volume, pan, mute, solo, VU-mètres, limiteur master)
- [ ] 1.6 Export WAV 24 bits/48 kHz + partage ; lecture en Foreground Service + exclusion batterie guidée
- Critères : un beat de 4 mesures composé, sauvegardé, rouvert et exporté ; 0 craquement à 256 échantillons.

## Phase 2 : sampler et notes
- [ ] 2.1 Import MP3/WAV/FLAC/M4A, enregistrement micro, forme d'onde zoomable
- [ ] 2.2 Découpe (attaques avec aubio, grille, manuelle), tranches vers pads, ADSR, pitch, reverse, boucle, groupes d'étouffement
- [ ] 2.3 Time-stretch (Signalsmith Stretch), rééchantillonnage du master
- [ ] 2.4 Piano roll : vélocité, quantification avec intensité, notes fantômes
- [ ] 2.5 Verrouillage de gamme, accords en un geste, progressions, arpégiateur, strum

## Phase 3 : remix
- [ ] 3.1 StemSeparator + DemucsOnDevice (ONNX Runtime), téléchargement des modèles avec empreinte
- [ ] 3.2 Foreground Service de séparation (progression, annulation, WakeLock, cache)
- [ ] 3.3 Détection tempo (aubio) et tonalité (libKeyFinder)
- [ ] 3.4 Mode Remix : stems calés dans un nouveau projet, mute/solo, stem vers sampler

## Phase 4 : production
- [ ] 4.1 Effets complets (EQ, compresseur, saturation, delay, réverbes…), 4 inserts + 2 envois
- [ ] 4.2 Sidechain, automation dessinée/enregistrée
- [ ] 4.3 Vue Session (clips × scènes) et vue Morceau
- [ ] 4.4 Effets de performance, pad XY

## Phase 5 : synthèse avancée et connectivité
- [ ] 5.1 Plaits, FM 4 opérateurs, basse 303
- [ ] 5.2 Presets classés, favoris, randomisation intelligente
- [ ] 5.3 MIDI USB/BLE, horloge MIDI, Ableton Link
- [ ] 5.4 Basic Pitch (audio vers MIDI)

## Phase 6 : génération et finitions
- [ ] 6.1 Stable Audio Open Small (LiteRT)
- [ ] 6.2 Modèles par genre, projets de démo, tutoriel, astuces
- [ ] 6.3 Exports MP3/FLAC/stems/MIDI/zip, finitions
