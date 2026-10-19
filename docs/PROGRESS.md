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

## Prochaine étape exacte
1. Revue de fin de phase 2 (sous-agent neuf), corriger le grave, REVIEW.md, résumé en 3 lignes, PR #6 prête, feu vert.
2. Reste noté : lever la limite de 60 s par son ; kits de batterie GM (banque 128) sur les pads ; import de packs .zip.
2. Reste du 2.3 : lever la limite de 60 s par son (stockage 16 bits ou lecture en flux), utile pour découper des morceaux entiers.
