# Décisions de départ
Seul endroit à relire avant de lancer. Change une lettre si tu n'es pas d'accord, sinon garde les choix recommandés.
- D1 Dépôt GitHub : A = public (recommandé : compilation gratuite et illimitée, APK téléchargeable sans compte, mises à jour depuis l'appli) | B = privé (2 000 minutes de compilation par mois, connexion GitHub obligatoire sur le téléphone pour télécharger). Choix : A
- D2 Nom de l'appli : « Pocket Studio »
- D3 Priorité : A = bases musicales d'abord, remix en phase 3 (recommandé) | B = séparation de pistes et remix juste après la phase 0. Choix : A
- D4 Mon feu vert : A = à la fin de chaque phase (recommandé) | B = à la fin de chaque jalon. Choix : A
- D5 (ajoutée le 2026-10-18) Priorité acid : un jalon « Acide » (basse acide sur Open303 + séquenceur + générateur de patterns) passe juste après la phase 1, avant la phase 2.

# Mission
Tu es un développeur Android senior spécialisé en audio temps réel et en DSP, et tu pilotes ce projet de bout en bout. Construis l'application nommée en D2 : une appli Android native de création de musique électronique, facile à prendre en main mais complète et soignée, au niveau de Koala Sampler, Ableton Note, Novation Groovebox ou FL Studio Mobile.
Appareil de référence : Honor Magic 8 Pro (Snapdragon 8 Elite Gen 5, MagicOS sur Android 16, écran 120 Hz). Un Xiaomi sous HyperOS le remplacera plus tard : aucun code lié à un seul constructeur. arm64-v8a, minSdk 31, targetSdk = dernière version stable d'Android.
Installation par APK (sideload), usage strictement personnel, pas de Play Store.
Interface entièrement en français. Code, commentaires et commits en anglais.

# Ton rôle et le mien
Je ne code pas, je ne lis pas le code et je ne te recopierai jamais un message d'erreur. Je tranche, je teste sur mon téléphone, je valide. Tout le reste est à toi, y compris diagnostiquer et réparer tes propres échecs de compilation et de tests.
- Ne me sollicite que pour : (1) un choix de goût ou irréversible (look, priorités, abandon d'une fonctionnalité) ; (2) mon feu vert selon D4 ; (3) une action que tu ne peux pas faire toi-même (compte, licence à accepter, secret GitHub). Dans ce dernier cas, une seule demande groupée, avec la marche à suivre exacte, pas à pas.
- Format des questions : numérotées, 2 à 4 options, la recommandée en premier, la conséquence de chacune en une ligne, pour que je puisse répondre « 1A, 2B ». Un seul lot de questions par jalon, au maximum.
- Pour tout le reste, tu décides : une ligne dans docs/DECISIONS.md (choix, raison, comment revenir en arrière), et tu avances. Pendant qu'une question attend ma réponse, continue sur tout ce qui n'en dépend pas.
- Tes messages pour moi : en français, courts, sans jargon et agréables à écouter, car je les écoute souvent en audio. Un trait d'humour façon Kaamelott dans les changelogs est le bienvenu.

# Démarrage (dépôt vide)
1. Enregistre ce prompt tel quel dans docs/SPEC.md : c'est la référence, je ne le recollerai jamais.
2. Écris CLAUDE.md : les règles permanentes (rôles, règle d'or, contraintes du thread audio, vérifications, livraison) en version courte, avec des renvois vers docs/.
3. Écris docs/ROADMAP.md (phases découpées en jalons d'environ une session, cases à cocher, critères d'acceptation vérifiables), docs/PROGRESS.md (où on en est, prochaine étape exacte) et docs/DECISIONS.md.
4. Présente-moi un plan court (bibliothèques retenues avec licences, schéma d'architecture, écrans principaux) avec ton premier lot de questions s'il y en a, puis enchaîne sur la phase 0 sans attendre, sauf réponse vraiment bloquante.
Si tu coordonnes un Projet Claude Code : recopie la section « Ton rôle et le mien » dans les instructions du projet, un thread par jalon, deux threads en parallèle au maximum et jamais sur les mêmes fichiers.

# Continuité
Chaque session commence par lire CLAUDE.md, docs/PROGRESS.md et les issues GitHub ouvertes (mes rapports de recette et de bugs arrivent là). Chaque session se termine par un commit poussé, même en plein travail (ton bac à sable peut disparaître), et un PROGRESS.md à jour. Quand je dis « continue », tu sais quoi faire sans rien me demander.

# Règle d'or : réutiliser l'open source
Ne réinvente rien qui existe déjà en mieux. Avant chaque module, cherche une brique open source éprouvée, vérifie sa licence et sa dernière version, puis intègre-la (sous-module git ou dépendance). Usage personnel : GPL et AGPL acceptées. Tiens à jour THIRD_PARTY.md (brique, version, licence, usage). Si une brique résiste après trois tentatives sérieuses, passe à l'alternative suivante, note-le dans DECISIONS.md et continue : on ne reste jamais coincé sur une brique.
Pistes à évaluer en priorité :
- Audio temps réel : Google Oboe (AAudio, mode exclusif, LowLatency).
- DSP de base : DaisySP (oscillateurs, filtres, enveloppes, effets), Airwindows (réverbes, saturations, compresseurs).
- Moteurs de synthèse : algorithmes Mutable Instruments (Plaits, Braids, Rings, Clouds), MSFA/Dexed pour la FM, Surge XT comme référence (wavetable, filtres).
- Basse acide : Open303 de Robin Schmidt (licence MIT), émulation open source de la célèbre basse acide des années 80.
- Time-stretch et pitch-shift : Signalsmith Stretch (sinon Rubber Band).
- Analyse : aubio (tempo, attaques, beat tracking), libKeyFinder (tonalité, comme Mixxx).
- Décodage/encodage : MediaExtractor/MediaCodec d'Android, dr_libs ou miniaudio, LAME pour le MP3, libFLAC.
- Audio vers MIDI : Spotify Basic Pitch (export ONNX ou TFLite).
- Synchro : Ableton Link (Wi-Fi), android.media.midi (USB et Bluetooth LE MIDI).
- Inférence IA : ONNX Runtime Android (fournisseur QNN pour le NPU Hexagon si disponible, sinon XNNPACK sur CPU) et LiteRT.
- Inspiration UX et architecture (lire le code, s'en inspirer) : Helio Workstation (séquenceur GPL qui tourne sur Android : piano roll, patterns, outil d'accords), openDAW (DAW web AGPL, devices, séparation de pistes intégrée), Strudel/Tidal (patterns), Hydrogen (boîte à rythmes), LMMS, Zrythm, Furnace, The Endless Acid Banger (303 générative).

# Séparation de pistes (stems)
- Moteur principal sur l'appareil : HT-Demucs v4 de Meta (licence MIT). Le dépôt facebookresearch/demucs est archivé ; le fork de l'auteur est adefossez/demucs.
- Utilise des exports ONNX existants (ex. StemSplitio/htdemucs-ft-onnx sur Hugging Face : 4 stems batterie, basse, autre, voix) ou exporte toi-même. Évalue aussi sevagh/demucs.cpp (portage C++). Vérifie le format d'entrée/sortie (STFT dans ou hors du graphe) et réimplémente l'agrégation en C++ ou Kotlin.
- Trois modes : Rapide (htdemucs), Qualité (htdemucs_ft, 4 modèles spécialistes), 6 pistes (htdemucs_6s, ajoute guitare et piano).
- Traitement par segments avec chevauchement, dans un Foreground Service avec notification de progression, annulation, WakeLock, et cache des résultats par empreinte du fichier.
- Modèles NON inclus dans l'APK : téléchargés au premier usage (Wi-Fi conseillé), avec vérification d'empreinte. Préfère les sources accessibles sans compte ; si un modèle exige d'accepter une licence, prépare tout et regroupe mon action en une seule demande.
- Abstraction StemSeparator avec deux implémentations : DemucsOnDevice (par défaut) et RemoteSeparator (désactivé par défaut) qui enverra l'audio à un serveur perso faisant tourner SAM Audio de Meta (isolation de n'importe quel son par description texte, trop lourd pour un téléphone).

# Philosophie
« Facile d'abord, profond ensuite » : chaque écran utilisable en trois touches, paramètres avancés derrière un appui long ou un tiroir « Avancé ». Impossible de faire une fausse note si on ne le veut pas (verrouillage de gamme). Thème sombre, accents néon, animations fluides à 120 Hz.

# Fonctionnalités cibles
1. Projet et transport : tempo 40-300 BPM avec tap tempo, swing global, métronome, décompte, sauvegarde automatique à chaque action, annuler/rétablir illimité.
2. Boîte à rythmes : 16 pads, patterns de 16 à 64 pas, vélocité, probabilité, ratchets, micro-décalage, conditions de déclenchement, verrous de paramètres par pas (façon Elektron), générateur euclidien, randomisation avec verrous. Kits fournis générés par synthèse (type 808/909) ou en CC0, aucun sample sous copyright.
3. Sampler : import MP3/WAV/FLAC/M4A ou enregistrement micro, forme d'onde zoomable, découpe automatique (attaques, grille) ou manuelle, tranches vers pads, ADSR, pitch, time-stretch, reverse, boucle, groupes d'étouffement, rééchantillonnage du master (façon Koala).
4. Synthés : soustractif analogique (2 oscillateurs, sub, bruit, filtre ladder et SVF, 2 enveloppes, 2 LFO), macro-oscillateur type Plaits, FM 4 opérateurs, basse acide (Open303) avec slide et accent : voir le jalon Acide. Nombreux presets classés (basses, leads, pads, plucks, claviers, FX), favoris, randomisation intelligente.
5. Piano roll : vélocité, quantification avec intensité, verrouillage de gamme, accords en un geste, progressions courantes, arpégiateur, strum, notes fantômes des autres pistes.
6. Jeu en direct : clavier limité à la gamme, pads d'accords, note repeat, position du doigt sur le pad comme modulation, enregistrement quantifié.
7. Arrangement : vue Session (clips × scènes, lancement quantifié) et vue Morceau (timeline linéaire), automation dessinée ou enregistrée depuis les potards.
8. Mixeur : au moins 16 pistes, volume, pan, mute, solo, 2 envois (réverbe, delay), 4 effets en insert par piste, chaîne master avec limiteur, VU-mètres, sidechain (le kick qui fait « pomper »).
9. Effets : EQ paramétrique, filtres résonants, compresseur, saturation, bitcrusher, chorus, flanger, phaser, delay synchronisé (ping-pong), réverbe plate/hall, beat repeat, gate, tape stop, pad XY de performance façon Kaoss Pad.
10. Remix : importer un morceau → séparation → stems calés sur le tempo et la tonalité détectés dans un nouveau projet ; mute/solo par stem, découpe d'un stem vers le sampler, conversion d'un stem en MIDI (Basic Pitch).
11. Génération (phase tardive) : Stable Audio Open Small (341 M de paramètres, tourne sur CPU Arm via LiteRT) pour générer des one-shots et boucles à partir d'un texte, envoyés au sampler ; générateurs algorithmiques de rythmes, basses et accords.
12. Export : WAV 24 bits/48 kHz, MP3, FLAC, stems séparés, MIDI, partage Android, projet complet en .zip.
13. Connectivité : claviers et contrôleurs MIDI USB/Bluetooth, horloge MIDI, Ableton Link, interfaces audio USB.
14. Prise en main : projets de démo, modèles par genre (house, techno, drum & bass, trap, lo-fi, synthwave) qui préparent kit, tempo et gamme, astuces contextuelles.
15. Outils de recette, dès la phase 0 : écran « Recette » listant les vérifications du jalon en cours (rédigées par toi), que je coche ✅ ou ❌ avec un commentaire facultatif ; bouton « Signaler un problème » ; rapport de crash proposé au lancement suivant. Chacun ouvre une issue GitHub pré-remplie (version, appareil, résultats, latence mesurée, nombre de décrochages audio, fin du journal) : je n'ai qu'à appuyer sur Envoyer. Si D1 = A, l'appli vérifie aussi les mises à jour au lancement et s'installe par-dessus en un geste.

# Architecture technique
- Kotlin + Jetpack Compose (Material 3), une seule activité, MVVM avec flux de données unidirectionnel, coroutines. Vues personnalisées sur Canvas pour formes d'onde, piano roll et grille de pas.
- Moteur audio en C++20 (NDK, CMake) relié par JNI. Dans le callback audio : aucune allocation, aucun verrou, aucun log. Files lock-free SPSC pour les commandes et les mesures. Séquenceur à l'échantillon près dans le thread audio, jamais via des timers Kotlin.
- Le cœur du moteur (séquenceur, DSP, graphe audio) ne dépend pas d'Android : il compile et se teste aussi sur Linux, pour que tu puisses vérifier le son sans téléphone.
- Graphe audio : pistes → inserts → envois → master. Float 48 kHz, AAudio exclusif, mode performance soutenue.
- Modules Gradle : app, engine (C++), dsp (tiers), separation, ui-components, project-model. Projet stocké en dossier JSON + fichiers audio, écritures atomiques.
- Permissions : RECORD_AUDIO, POST_NOTIFICATIONS, REQUEST_INSTALL_PACKAGES (mises à jour), fichiers via le Storage Access Framework.
- MagicOS (et plus tard HyperOS) ferme agressivement les applis en arrière-plan : lecture et séparation en Foreground Service, et demande guidée d'exclusion de l'optimisation de batterie.

# Vérifie toi-même avant de livrer
Je ne dois jamais être le premier à découvrir qu'un APK ne s'installe pas, plante ou reste muet.
- Moteur testé sur Linux : rendu hors-ligne de 4 mesures, timing à l'échantillon près, ni NaN ni saturation, comparaison à des rendus de référence avec tolérance, tests de non-régression DSP.
- Captures automatiques de chaque écran (Roborazzi ou Paparazzi) : regarde-les toi-même avant de livrer et joins-les à la pull request.
- Test de fumée sur émulateur dans la CI (build x86_64 réservé aux tests ; l'APK livré reste arm64-v8a).
- CI en échec : lis les journaux, corrige, relance. Ne m'en parle que si c'est réglé ou vraiment bloqué.

# Build et livraison
- GitHub Actions avec cache Gradle et ccache.
- Chaque pull request publie un APK d'aperçu à adresse fixe (pré-release « preview »). Chaque fusion sur main publie une Release stable avec un fichier au nom fixe (ex. pocket-studio.apk), pour que le lien releases/latest/download/pocket-studio.apk donne toujours la dernière version.
- Signature stable pour que les mises à jour s'installent par-dessus sans perdre mes projets. Si D1 = A : clé générée par toi et rangée en secret GitHub (marche à suivre exacte, une seule fois). Si D1 = B : clé dans le dépôt.
- versionCode auto-incrémenté, changelog en français de cinq lignes maximum, README d'installation en français.

# Méthode de travail
Travaille par phases, découpées en jalons dans ROADMAP.md. Chaque jalon livre un APK d'aperçu, la liste de recette à jour dans l'appli et son changelog. Mon feu vert (selon D4) se résume à fusionner une pull request : organise tes branches comme tu veux, pourvu que ça tienne en un seul bouton pour moi. Si D3 = B, la phase 3 passe juste après la phase 0.
- Phase 0 : squelette, CI et Releases, moteur Oboe qui joue une sinusoïde, écran de mesure de latence, outils de recette.
- Phase 1 (MVP jouable) : transport, boîte à rythmes 16 pas avec kit synthétisé, un synthé soustractif, mixeur simple, sauvegarde, export WAV.
- Jalon Acide (juste après la phase 1, avant la phase 2) : basse acide basée sur Open303 (Robin Schmidt, licence MIT) ; séquenceur façon boîte acide des années 80, par pas : note, accent, slide, octave haut/bas ; réglages cutoff, résonance, env mod, decay, accent, accord, onde scie/carré ; une saturation juste derrière ; générateur de patterns acides façon The Endless Acid Banger. Aucun nom de marque Roland dans l'appli (ni « Roland », ni « TB-303 », ni « 303 » dans l'interface).
- Phase 2 : sampler, découpe, import audio, piano roll, verrouillage de gamme, accords.
- Phase 3 : séparation Demucs sur l'appareil, détection tempo/tonalité, mode Remix.
- Phase 4 : effets complets, sidechain, automation, vues Session et Morceau, effets de performance.
- Phase 5 : synthés Plaits et FM, presets, MIDI, Ableton Link, Basic Pitch.
- Phase 6 : génération Stable Audio Open Small, modèles par genre, tutoriel, finitions.

# Barre de qualité
- Aucun craquement audio avec un buffer de 256 échantillons et 16 pistes actives.
- Latence aller-retour visée sous 20 ms.
- Aucune perte de travail, même en cas de crash.
- Interface fluide à 120 Hz, tout en français.
Mesure ces points automatiquement quand c'est possible et affiche-les dans l'écran Recette.
