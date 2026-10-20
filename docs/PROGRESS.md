# Avancement

**Phase 0 fusionnée le 2026-10-18, phase 1 fusionnée le 2026-10-19 (feu vert de Benjamin).** Phase en cours : 2 (sampler et notes).

## Fait
- Démarrage : SPEC, CLAUDE.md, ROADMAP, DECISIONS, ARCHITECTURE, THIRD_PARTY, README.
- Moteur C++ : sinusoïde lissée (sans clic), file SPSC, compteur de crêtes, mesure aller-retour. 10 tests Linux verts, aussi sous ASan/UBSan.
- Android : Oboe AAudio exclusif LowLatency, reconnexion casque, écrans Studio / Latence / Recette, rapports GitHub, crash au lancement suivant, mise à jour intégrée.
- CI : tests moteur (+ ASan/UBSan), tests Kotlin, captures, APK signé, aperçu sur PR, Release sur main, test de fumée émulateur.

- Revue critique de fin de phase faite (docs/REVIEW.md) : graves corrigés.
- Jalon Acide (Open303) ajouté entre la phase 1 et la phase 2.

- Recette sur le Honor Magic 8 Pro : sortie 10 ms (AAudio exclusif MMAP, burst 96), aller-retour haut-parleur → micro 44,5 ms stable (45,2 / 44,4 / 44,5).

- Jalon 1.1 : module `project-model` (Kotlin pur) : format JSON tolérant aux versions, `ProjectStore` à écriture atomique + sauvegarde de secours, `History` annuler/rétablir illimité avec regroupement des gestes, `ProjectSession` à sauvegarde automatique différée. 15 tests, dont plantage en pleine écriture.
- Jalon 1.2 : `Sequencer` (horloge de doubles-croches à l'échantillon près, swing, changement de tempo sans à-coup, décompte), `Metronome`, commandes moteur (transport, tempo, swing, métronome, pas de batterie), `TapTempo`. Tests : timing exact quelle que soit la taille de bloc, swing, changement de tempo, décompte, métronome à l'échantillon près.
- Jalon 1.3 (moteur) : `DrumKit` 16 sons synthétisés (DaisySP + clap/rim/cloche/shaker maison), déclenchés à l'échantillon près par le séquenceur, pads en direct (`TriggerPad`), charleston fermé qui coupe l'ouvert. Kit complet à 5,9 % du temps réel (x86). Tests : chaque son démarre pile et s'éteint, vélocité, étouffement, motif → batterie, budget CPU.
- Jalon 1.3 (écran) : onglet Rythme (transport, tempo −/+/Tap, annuler/rétablir, 16 pads colorés qui s'allument, grille 16 pas avec tête de lecture, appui long = vélocité, swing, métronome, décompte), projet chargé/sauvé via `ProjectSession`, sauvegarde forcée à la mise en arrière-plan. Onglet Outils (latence, son de test).
- Jalon Acide A.1–A.3 (moteur) : `AcidTrack` = Open303 + séquenceur par pas (note, octave, accent, slide, silence, 1–64 pas), 9 réglages (cutoff, résonance, env mod, decay, accent, accord ±1 octave, onde scie→carré, saturation, niveau). Tests : départ à l'échantillon près, accent ×2,2 en énergie, slide qui tient la note, arrêt propre, 1,2 % du temps réel.
- Jalon Acide A.4–A.5 : `AcidGenerator` (inspiré de The Endless Acid Banger, CC BY 4.0, crédité), densité, verrous ; `AcidPattern` dans le projet ; onglet Acide (générer, grille, éditeur de pas, 9 curseurs de son).
- Dette : la logique Acide vit dans `RhythmViewModel` (transport et projet partagés) ; à extraire dans un contrôleur de session partagé quand le synthé arrivera.
- Jalon 1.4 : `Synth` 8 voix (moteur) ; `SynthPatch` (24 réglages, 4 préréglages Basse/Lead/Nappe/Pluck) dans le projet ; onglet Synthé (clavier 2 octaves multi-doigts, octave, préréglages, réglages faciles + avancés). Le synthé se joue en direct ; l'enregistrement de notes arrive avec le piano roll (phase 2).

- Jalon 1.5 : `Mixer` moteur (3 tranches Batterie / Basse acide / Synthé : volume 0,8 = 0 dB, pan à puissance constante, muet, solo, gains lissés ; limiteur de crête master à −1 dB, attaque instantanée) remplace l'ancien écrêtage doux ; VU-mètres par atomiques ; onglet Mixeur. La Recette passe dans Outils.
- Jalon 1.6 (export) : `WavWriter` (24 bits, dither triangulaire) + `OfflineRenderer` (moteur privé, plus rapide que le temps réel, arrêt pile à la fin + 2 s de queue) ; le projet lui est décrit par le même code de synchro que le moteur en direct (`NativeEngine.captureInto`). Carte « Exporter en WAV » dans le Mixeur (1 à 16 mesures, progression, annuler), copie dans Musique › Pocket Studio, bouton Partager. Tests Linux : en-tête, écrêtage sans retournement, rendu identique à l'échantillon près, annulation, erreur disque.
- Jalon 1.6 (fond) : `PlaybackService` (service de premier plan « lecture multimédia ») démarré avec le transport, notification avec bouton Arrêter ; le moteur reste allumé en arrière-plan seulement pendant la lecture. Page Outils › Arrière-plan : notification, batterie sans restriction, réglage Honor « Lancement d'applications » guidé.

- Recette #5 (rythme) : l'appui long « plus doux » n'était pas perceptible. La case se remplit désormais selon la force (fort / moyen / doux = 1 / 0,5 / 0,2, soit −6 et −14 dB).

## À retenir pour la suite
- Mode paysage : fait pour la navigation (onglets sur le côté) et le synthé (clavier 3 octaves). Les autres écrans tournent mais gardent la mise en page portrait : à adapter au fil des phases.
- Afficher une « latence de jeu » (sortie + tactile) à côté de l'aller-retour ; essayer un buffer d'un seul burst.
- Fait : journal interne `AppLog` (200 lignes : démarrage, son ouvert/fermé, lecture, latence, export, sauvegarde) joint aux rapports à la place de logcat, chiffré par MagicOS.
- Recette phase 0 : issues #2 et #3 (doublon), 8/8 ✅, fermées.

- Jalon 2.1 : sons perso sur les pads. Moteur : `Sample` (immuable, compté par références, rééchantillonné en 48 kHz par libsamplerate hors fil audio) et `Sampler` (16 pads, déclenchement à l'échantillon près, fondu de 2 ms au redéclenchement, rendu dans la tranche Batterie) ; le fil audio ne libère jamais : les sons lâchés reviennent par une file au fil principal. Appli : décodage par les codecs Android (MP3, WAV, FLAC, M4A, OGG…, 60 s max), micro 48 kHz brut (30 s max, silence de début coupé), stockage en WAV float dans `samples/` du projet, carte « Son du pad » avec forme d'onde zoomable (pincer), retour au son du kit annulable. Tests Linux (rééchantillonnage, départ pile sur le pas, références sous ASan), tests JVM, test émulateur (import WAV 44,1 kHz → pad qui sonne).
- Jalon 2.2 (partie 1) : réglages par pad dans le moteur (début/fin, hauteur ±24 demi-tons en interpolation Hermite, à l'envers, en boucle jusqu'au coup suivant ou à l'arrêt, enveloppe ADSR, volume, 4 groupes d'étouffement coupés à l'échantillon près) ; dans le projet (`PadSample`, réparé au chargement) ; contrôles sur la carte « Son du pad ».

- Jalon 2.2 (partie 2) : découpe. aubio 0.4.9 (GPL-3, chemin « onset » seul, fonction HFC) trouve les attaques hors fil audio ; grille 2/4/8/16 ; coupe manuelle en touchant la forme d'onde ; « Sur les pads » répartit les tranches à partir du pad choisi (même fichier, début/fin différents, une seule étape d'annulation).

- Jalon 2.3 : « Calé au tempo » (0/1/2/4/8/16/32 temps, suggestion ★) : une copie de la zone du pad, étirée par Signalsmith Stretch (MIT) et transposée sans changer sa durée, calculée hors fil audio, recalculée quand le tempo change (le son d'origine joue en attendant). Rééchantillonnage : « Enregistrer le morceau » rend 1, 2 ou 4 mesures du morceau (moteur hors-ligne) sur le pad choisi. Tests Linux : hauteur gardée à 1 000 Hz sur une durée ×1,5, +12 demi-tons = 1 998 Hz à durée égale ; rendu en mémoire identique au fichier.

- Jalon 2.4 : piano roll du synthé (onglet Synthé › Notes). Moteur : `NoteTrack` (512 notes, 24 ticks par double-croche, départs et fins à l'échantillon près, boucle de 1 à 8 mesures indépendante de la batterie, notes relâchées à l'arrêt) ; file d'événements du synthé triée. Écran : toucher pour ajouter/enlever, glisser pour déplacer, glisser le bout pour allonger, appui long pour la force, quantifier 50 %/100 %, basse acide en notes fantômes, tête de lecture. Test Linux : note à 1,5 pas qui part pile au bon échantillon et se répète à chaque boucle.

- Jalon 2.5 : `Harmony` (tonique + gamme : majeure, mineure, dorienne, pentas, blues) dans le projet ; piano roll : notes aimantées à la gamme, lignes hors gamme assombries, mode « Accords » (un toucher = l'accord de la gamme) ; clavier : touches hors gamme grisées, « Accords » à un doigt ; 5 progressions (I–V–vi–IV…) qui remplissent 4 mesures ; arpégiateur (↑ ↓ ↑↓ hasard, 1/8 1/16 1/32) et strum, calculés côté Kotlin avant l'envoi au moteur (`Performance.expand`). Tests JVM : accords par degré (Do, Rém, Si°, Sol7, Lam), progression, arpèges, strum.

- Jalon 2.6 : la piste Synthé peut jouer un des 128 instruments General MIDI (GeneralUser GS via TinySoundFont, voix préallouées, instance par moteur, copie retirée et fermée hors fil audio). La banque (32 Mo) est publiée par la CI sur une Release dédiée et téléchargée une fois par l'appli, empreinte SHA-256 vérifiée avant toute lecture (TinySoundFont n'est pas sûr sur un fichier abîmé). Navigateur par famille dans Synthé › Source du son. Import d'un dossier de sons (pack) sur les pads suivants. Test Linux avec une mini-banque SF2 générée : La 440 Hz puis 880 Hz, remplacement et fermeture sans fuite (ASan).

- Phase 3, jalon 3.1 (moteur) : séparation HT-Demucs v4 en ONNX (module `:separation`, ONNX Runtime 1.31.0), modèle construit de façon reproductible et hébergé sur la Release `modele-htdemucs-v4-onnx-1`. Test Linux : batterie 16,4 dB, basse 17,0 dB, autres 22,7 dB SDR.

- Demande de Benjamin (2026-10-19) : ligne acide jusqu'à 8 mesures (128 pas, pages de 16, « Copier → page », « Suivre ») ; deux nouvelles banques à télécharger (FluidR3 GM 148 Mo, MuseScore General 216 Mo, MIT) au choix avec GeneralUser ; toutes les variantes par famille ; kits de batterie SF2 rendus sur les 16 pads (une édition annulable).

- Jalons 3.1 et 3.2 (appli) : onglet Remix. Téléchargement du modèle (174 Mo, SHA-256), chanson décodée en fichier, séparation dans un service au premier plan (progression dans la notification, Annuler, écran éteint possible), stems gardés pour les 3 derniers morceaux, écoute de chaque stem. Tests Linux de séparation dans la CI (job « Separation tests »).

- Jalon 3.3 : tempo (aubio sur la piste batterie, affiné par régression, premier temps sur la grosse caisse) et tonalité (chromagramme basse + autres, profils de Krumhansl) calculés après la séparation, affichés dans Remix ; « Caler le projet sur ce morceau » règle tempo et gamme en une étape annulable. Test Linux : 124,1 BPM pour 124, premier temps à 0,25 s, La mineur et Do majeur reconnus.

- Jalon 3.4 : « Jouer avec le projet » charge les 4 stems dans le moteur (16 bits, 48 kHz), cale tempo et gamme, et les fait partir avec le Rythme au pas 0 (mesure de départ réglable) ; volume / muet / solo par stem, voie « Remix » au mixeur (et dans l'export) ; « → Pad » copie 1, 2 ou 4 mesures d'un stem sur le pad choisi. Tests Linux : départ à l'échantillon près, mute/solo, fondu à l'arrêt, chargement 44,1 → 48 kHz, aucune allocation dans le fil audio.

- Phase 3 terminée et fusionnée (PR #7, 2026-10-19) : revue faite (REVIEW.md), plantage Android 16 du service corrigé (rapport n° 8), mini-clavier de l'acide.

- Phase 4 (PR #13, en cours) :
  - correctifs Remix (mémoire ONNX Runtime 5 → 2,6 Go ; règles R8 du rappel de progression ; contrôle dexdump en CI) ; formes d'onde des stems avec tête de lecture et départ au toucher ; stems sans écrêtage ; kits SF2 jouables au clavier ;
  - 4.1 effets : 4 inserts par voie et sur la sortie (égaliseur, compresseur, saturation, chorus, filtre, pompe), envois réverbe (Freeverb) et écho calé au tempo, écran au Mixeur ;
  - 4.2 sidechain (« Pompe » sur la grosse caisse ou chaque temps) et automation par pas (dessinée ou enregistrée), jouées par le moteur. Tests Linux pour chaque effet, la pompe, l'automation et zéro allocation ;
  - 4.4 effets live sur la sortie : pad XY (filtre DJ + résonance, revient au neutre), répéteur 1/4–1/32 calé au tempo, hacheur sur les doubles-croches, stop bande ; dans l'onglet Morceau ; test Linux ;
  - 4.3 scènes A–H (batterie + acide + piano roll), lancement à la mesure suivante, morceau (scènes × répétitions, en boucle) ; bascule au pas près dans le moteur (tampon d'attente), test Linux.

- Phase 4 terminée et fusionnée (PR #13, 2026-10-20) : revue faite et corrigée (REVIEW.md).

- Phase 5 (en cours, branche ccr-80af1a1b-mn0wec) :
  - 5.1 Plaits comme 2e voix du Synthé (24 modèles, test Linux, zéro allocation) ; la FM 6 op. de Plaits tient lieu de FM 4 op. ;
  - 24 scènes (A à X) au lieu de 8, grille « Afficher 8 · 16 · 24 » (demande de Benjamin) ;
  - pads live (lanceur de boucles 4 à 16 pads, onglet Morceau) : ClipLauncher dans le moteur, testé sur Linux ; import, son d'un pad de batterie, « → Live » depuis le Remix ;
  - Remix : écoute des 4 pistes ensemble par le moteur, chaque piste allumée ou coupée sans arrêt ;
  - Acide : potards compacts (composant Knob dans ui-components), curseurs en option ;
  - 5.2 sons classés par familles (Synthé 16, Plaits 17, Acide 7), « Au hasard » bridé pour rester musical, favoris communs à tous les projets (favorites.json) ;
  - 5.3 MIDI : claviers USB (connexion automatique) et Bluetooth LE (recherche), notes vers le Synthé, canal 10 vers les pads, horloge/départ/arrêt suivis (Outils › MIDI). Ableton Link et horloge sortante reportés (DECISIONS) ;
  - 5.4 audio vers notes (Basic Pitch, Spotify) : chanter ou jouer une mélodie, elle arrive dans le piano roll ; test Linux (La4, Do5, accord Mi-Sol retrouvés) ;
  - restes traités : sons des pads jusqu'à 2 min (coupés au-delà), packs .zip sur les pads, Remix qui suit le tempo du projet (copie étirée en arrière-plan).

- Phase 6 « complétude » (même PR #16, voir COMPARAISON.md) :
  - 6.1 plusieurs projets (Mes projets, corbeille, .zip), enregistrement en direct (●), export morceau / pistes / M4A, fichiers MIDI ;
  - 6.2 Réglages (thème dont daltonien, taille, notes, vibrations, écran allumé, métronome, décompte, mode du son), guide des premiers pas, 3 démos ;
  - 6.3 piano roll complet (sélection, outils, couloir de force, zoom, grilles, deviner la gamme) ;
  - 6.4 batterie complète (1 à 4 mesures, roulements, remplir, décaler, vue de tous les pads).

- Phases 5 et 6 fusionnées (PR #16, relectures faites).
- Phase 7 (PR #17, branche ccr-80af1a1b-mn0wec) :
  - 7.1 fait : jusqu'à 8 pistes d'instruments (barre de pistes dans Synthé, couleurs, renommer, dupliquer, supprimer), une voie de mixeur chacune, MIDI et enregistrement sur la piste choisie, export pistes et MIDI par piste.

  - 7.2 fait : onglet Audio (pistes audio + Remix), 4 pistes audio, prise au micro calée sur le premier temps (latence aller-retour compensée), import, garder une sélection, normaliser, inverser, fondus, mesure de départ, longueur de boucle. Un clip par piste pour tout le projet (pas par scène) : la ligne de temps (7.3) placera les clips.

  - 7.3 fait : ligne de temps dans Morceau (une colonne par partie, une ligne par piste, un appui coupe ou remet une piste dans une partie, appliqué au jeu, à l'export et au MIDI) ; mesures 2/4 à 7/4 (décompte, temps fort du métronome, changements de scène, grille de batterie, pads live).

- Phase 8 (branche ccr-80af1a1b-mn0wec) :
  - 8.1 fait : flanger, phaser, noise gate, trémolo/auto-pan, élargisseur stéréo, bitcrusher, EQ paramétrique avec sa courbe ; analyseur de spectre (Mixeur › Sortie générale) ; accordeur (Audio › Accordeur).

  - 8.2 fait : chaque courbe d'automation est un clip « Boucle », « Par scène » (chaque scène ses pas, changés au passage de scène) ou « Morceau » (un point par mesure, glissé d'un point à l'autre, enregistrable en direct).

  - 8.3 fait : vélocité selon l'endroit touché sur la touche, pitch bend (revient au centre), modulation, sustain, arpège en direct (1/32 à 1/4, 4 sens, 1 à 4 octaves, longueur) calé dans le moteur ; clavier MIDI : pitch bend, molette, pédale de sustain ; MIDI learn (Outils › MIDI) ; horloge MIDI sortante.
  - Ableton Link reporté (bibliothèque réseau lourde).

- Phase 9 (branche ccr-80af1a1b-mn0wec) :
  - 9.2 fait : 6 projets « Style » (house, techno, drum & bass, trap, lo-fi, synthwave) dans Nouveau projet.
  - 9.1 fait : Stable Audio Open Small sur le téléphone (ONNX Runtime, int8 partiel, 858 Mo sur la Release `modele-stable-audio-open-small-1`), tokeniseur T5 en Kotlin, Audio › Générer (phrase, exemples, durée 1 à 11 s, écoute, pose sur pad / pad live / piste audio). Test Linux : `STABLE_AUDIO_DIR=… ./scripts/separation-tests.sh`.

- Phase 9 fusionnée (PR #19).
- Phase 10 (branche ccr-80af1a1b-mn0wec), demandes de Benjamin pendant ses essais :
  - 10.1 batterie stéréo : pan par pad, pan et hauteur par coup (outils « Pan » et « Hauteur » de la grille) ;
  - 10.2 assistant pas à pas (Mes projets › Nouveau › Assistant) : 16 styles, ambiance, rôle de chaque ligne, sons intégrés (rendus par le moteur pour l'écoute) ou packs, basse / nappe / lead avec écoute de la première mesure ;
  - 10.3 verrouillage de gamme, pads d'accords (Synthé › Pads), progressions en accords / basse / arpège ;
  - 10.4 effets créatifs : ambiances, macros Couleur / Saleté / Mouvement / Espace dans le moteur, pad XY, bouton Effets sur chaque piste.
  - Packs de sons : format, index épinglé, téléchargement et suppression prêts.
- Phase 10 fusionnée (PR #20), revue faite.
- Phase 11 (branche ccr-80af1a1b-mn0wec) :
  - 11.1 fait : 34 packs (environ 2 000 sons, 44 à 70 par style, 0,5 à 3 Mo chacun) fabriqués par le workflow Packs (Stable Audio Open Small, filtres de qualité) et publiés sur la Release `sons-packs-2` ; index épinglé dans `PackStore.INDEX`.
  - 11.2 fait : Bibliothèque de sons (bouton dans Rythme) : familles de sons, sources (intégrés, packs), écoute, « Poser » sur le pad ; liste des packs avec taille, Télécharger, Supprimer.
  - En plus : export FLAC (encodeur maison, sans perte, vérifié par libsndfile et ffmpeg) et volume perçu en LUFS (BS.1770-4) affiché après l'export du mix.

- Phase 11 fusionnée (PR #21), revue faite (FLAC décodé dans son test, plus compact).
- Phase 12 (branche ccr-80af1a1b-mn0wec), demandes de Benjamin :
  - 12.1 source « Échantillon » des pistes : un son (808, basse, stab d'un pack, son importé) joué sur tout le clavier, note d'origine détectée (YIN), glisse façon 808, boucle, enveloppe ;
  - 12.2 petits « ? » à côté des termes et Lexique (Outils › Réglages › Aide) ;
  - 12.3 pads live : « + » vers la bibliothèque, Générer, puis le téléphone ; export « La scène » ;
  - 12.4 l'assistant propose les 808 et basses des packs pour la basse ;
  - file de commandes du moteur : attend au lieu de perdre des notes sur un gros projet ;
  - 17 nouveaux packs commandés (machines et sons actuels) sur `sons-packs-3`, utilisés en phase 13.
- Phase 13 prête en local (branche phase13-wip, à poser après la fusion de la 12) : unisson du synthé et sons actuels, nouvelles familles de sons des packs, phrases de Générer pour les genres récents.

- Phase 12 fusionnée (PR #23), revue faite (notes répétées coupées, glisse, plantage Bluetooth #22).
- Phase 13 (branche ccr-80af1a1b-mn0wec) :
  - 13.1 et 13.2 : 17 packs (machines TR-808 à Minimoog, sons actuels) sur `sons-packs-3`, index épinglé (51 packs) ; familles toms, nappes, leads, plucks, claviers, voix, effets ; l'assistant propose nappes, claviers, leads et plucks des packs ;
  - 13.3 : unisson du synthé (1 à 7 voix) et 6 sons actuels (Supersaw, Lead trance, Hoover, Accords future bass, Pluck future, Reese moderne) ;
  - 13.4 : 14 phrases de Générer pour les genres récents.

- Phase 13 fusionnée (PR #24), revue faite (unisson centré, pas de clic au redéclenchement).
- Phase 14 (branche ccr-80af1a1b-mn0wec), demandes de Benjamin :
  - 14.1 nouveau look sur tout le programme (Inter, relief, halos, curseurs et vumètres) ;
  - 14.2 blocs repliables par leur titre, mémorisés ;
  - 14.3 sons en vignettes partout ;
  - 14.4 clavier qui suit pendant le choix d'un son ;
  - 14.5 écoute dans l'assistant (ambiances, lignes avec le rythme, parties en situation).

- Phase 14 : relecture faite (écoute muette de la nappe et du lead corrigée, blocs repliés sans clignotement, fin silencieuse coupée seulement pour les sons de la bibliothèque et le micro), CI rendue robuste aux téléchargements cassés de l'émulateur.
- Phase 15 (branche locale phase15-wip, à poser sur `ccr-80af1a1b-mn0wec` après la fusion de la 14), demandes de Benjamin :
  - 15.1 recettes des 35 styles (`scripts/packs/recipes/*.json` → `StyleRecipes.kt` par `recipes_to_kotlin.py`) : morceaux de référence, batterie (machine du style), progression par ambiance, rythme et disposition des accords, basse et riff en degrés, ligne 303 ;
  - 15.2 sons des packs classés du meilleur au moins bon (`rank_sounds.py` → `PackPicks.kt`) ;
  - 15.3 packs Acid techno et TB-303 (`sons-packs-4`, 53 packs), style Acid techno ;
  - 15.4 trois claviers (2 octaves, 1 octave nommée, gamme) ;
  - 15.5 sample d'un pad joué au clavier, fichier importé au clavier ;
  - 15.6 effets par pad (16 tranches de plus dans le mixeur).

## Prochaine étape exacte
1. Phase 14 : CI verte sur la PR #25, fusion, lien à Benjamin.
2. Phase 15 : relecture neuve, poser la branche sur `ccr-80af1a1b-mn0wec`, PR, CI, fusion.
3. Audit complet « en production » demandé par Benjamin (lecture, enregistrement, scènes, pads et effets, synthés, acide, mixeur, exports, changements d'onglet en lecture, longues sessions, rotation, arrière-plan).
