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
- [x] 1.1 Modèle de projet (module project-model, JSON, écritures atomiques, sauvegarde auto, annuler/rétablir) : branché dans l'appli avec 1.2/1.3
- [x] 1.2 Transport : tempo 40-300 BPM, tap tempo, swing, métronome, décompte ; séquenceur à l'échantillon près (moteur + tests ; écran avec 1.3)
- [x] 1.3 Boîte à rythmes 16 pas, 16 pads, kit 808/909 synthétisé (DaisySP), vélocité
### Jalon Acide (priorité de Benjamin : fait juste après 1.3, avant 1.4)
- [x] A.1 Intégrer Open303 (Robin Schmidt, MIT) dans `dsp/`, voix monophonique dans le graphe audio ; tests Linux (pas de NaN, slide continu, accent plus fort)
- [x] A.2 Séquenceur acide par pas (16 pas, jusqu'à 64) : note, accent, slide, octave haut/bas, silence ; à l'échantillon près dans le thread audio
- [x] A.3 Réglages : cutoff, résonance, env mod, decay, accent, accord, onde scie/carré ; saturation juste derrière la voix
- [x] A.4 Générateur de patterns acides façon The Endless Acid Banger (gamme, densité, accents, slides), avec verrous
- [x] A.5 Écran « Acide » : grille de pas lisible au doigt, potards, bouton « Génère-moi un pattern »
- Critères : un pattern généré tourne en boucle avec la boîte à rythmes, sans craquement à 256 échantillons ; slide et accent audibles ; aucun nom de marque Roland dans l'interface.
- [x] 1.4 Synthé soustractif (2 osc, sub, bruit, ladder + SVF, 2 env, 2 LFO), clavier simple
- [x] 1.5 Mixeur simple (volume, pan, mute, solo, VU-mètres, limiteur master)
- [x] 1.6 Export WAV 24 bits/48 kHz + partage ; lecture en Foreground Service + exclusion batterie guidée
- Critères : un beat de 4 mesures composé, sauvegardé, rouvert et exporté ; 0 craquement à 256 échantillons.

## Phase 2 : sampler et notes
- [x] 2.1 Import MP3/WAV/FLAC/M4A, enregistrement micro, forme d'onde zoomable
- [x] 2.2 Découpe (attaques avec aubio, grille, manuelle), tranches vers pads, ADSR, pitch, reverse, boucle, groupes d'étouffement
- [x] 2.3 Time-stretch (Signalsmith Stretch), rééchantillonnage du master
- [x] 2.4 Piano roll : vélocité, quantification avec intensité, notes fantômes
- [x] 2.5 Verrouillage de gamme, accords en un geste, progressions, arpégiateur, strum
- [x] 2.6 Bibliothèque d'instruments : banque General MIDI GeneralUser GS (128 instruments + kits) jouée par TinySoundFont (MIT), téléchargée au premier usage depuis notre propre Release (empreinte vérifiée) ; navigateur de sons par famille ; import de packs de samples (zip/dossier) et packs libres CC0

## Phase 3 : remix
- [x] 3.1 StemSeparator + DemucsOnDevice (ONNX Runtime), téléchargement des modèles avec empreinte
- [x] 3.2 Foreground Service de séparation (progression, annulation, WakeLock, cache)
- [x] 3.3 Détection tempo (aubio) et tonalité (libKeyFinder)
- [x] 3.4 Mode Remix : stems calés dans un nouveau projet, mute/solo, stem vers sampler

## Phase 4 : production
- [x] 4.1 Effets complets (EQ, compresseur, saturation, delay, réverbes…), 4 inserts + 2 envois
- [x] 4.2 Sidechain, automation dessinée/enregistrée
- [x] 4.3 Vue Session (clips × scènes) et vue Morceau
- [x] 4.4 Effets de performance, pad XY

## Phase 5 : synthèse avancée et connectivité
- [x] 5.1 Plaits, FM 4 opérateurs (FM 6 op. de Plaits)
- [x] 5.2 Presets classés, favoris, randomisation intelligente
- [~] 5.3 MIDI USB/BLE, horloge MIDI, Ableton Link
- [x] 5.4 Basic Pitch (audio vers MIDI)

## Phase 6 : complétude, à hauteur de FL Studio Mobile (voir `COMPARAISON.md`)
- [x] 6.1 Projets et exports : plusieurs projets (fait), enregistrement en direct (fait), boucle de batterie 1-4 mesures (fait) ; export du morceau entier, des pistes séparées, M4A et FLAC, normalisation ; fichiers MIDI (export, import) ; projet complet en .zip (export, import)
- [x] 6.2 Réglages, aide, démos : écran Réglages (thème dont daltonien, taille de l'interface, notation, Do central, vibrations, métronome, mode audio) ; premier pas guidé et astuces ; projets de démonstration et modèles
- [x] 6.3 Piano roll complet : sélection, copier/coller, dupliquer, transposer, couloir de vélocité, zoom, grille (triolets, 1/32), legato, humaniser, deviner la gamme
- [x] 6.4 Batterie complète (pan et envois par pad : avec les pistes, 7.1) : grille de tous les pads, pan/hauteur/roulement par pas, remplir tous les N pas, décaler, faire tourner ; pan et envois par pad

## Phase 7 : pistes
- [x] 7.1 Pistes d'instruments ajoutées à volonté (synthé, Plaits, General MIDI, sampler multi-échantillons), nommées et colorées, chacune avec sa voie de mixeur
- [x] 7.2 Pistes audio : enregistrer voix et instruments (latence compensée), clips avec fondus, normalisation, inversion, découpe
- [x] 7.3 Ligne de temps (clips posés sur des pistes) à côté des scènes ; mesures autres que 4/4

## Phase 8 : effets et contrôle
- [x] 8.1 Nouveaux effets (flanger, phaser, gate, trémolo, élargisseur, bitcrusher, EQ paramétrique visuel), analyseur, accordeur
- [x] 8.2 Automation en clips, par scène et sur toute la longueur
- [x] 8.3 Clavier expressif (vélocité selon l'appui, pitch bend, molette, sustain, arpège en direct), MIDI learn, horloge MIDI sortante (Ableton Link reporté, voir DECISIONS.md)

## Phase 9 : génération
- [x] 9.1 Stable Audio Open Small (ONNX Runtime, voir DECISIONS.md)
- [x] 9.2 Modèles par genre

## Phase 10 : débutants et terrain de jeu (demandes de recette)
- [x] 10.1 Batterie : pan par pad, pan et hauteur par coup, sons en stéréo
- [x] 10.2 Assistant « Nouveau morceau, pas à pas » : style, ambiance, une étape par ligne (rôle expliqué, son de la bibliothèque), basse / nappe / lead, rythme et accords de départ
- [x] 10.3 Mélodie sans fausse note : verrouillage de gamme (clavier, MIDI), pads d'accords, progressions en accords / ligne de basse / arpège
- [x] 10.4 Effets créatifs : 21 ambiances toutes prêtes, 4 macros par piste, pad XY, bouton Effets sur chaque piste
- [x] 10.5 Packs de sons par style et sous-style (téléchargement à la demande, suppression) : fabriqués en phase 11

## Phase 11 : bibliothèques de sons
- [x] 11.1 Packs par style et sous-style fabriqués et hébergés (kicks, caisses, claps, charleys, percus, 808, basses)
- [x] 11.2 Rayon de sons dans le pad (parcourir, écouter, poser), gestion des packs (taille, supprimer)
- [ ] 11.3 Ableton Link (reporté)

## Phase 12 : jouer les sons et comprendre les mots (demandes de recette)
- [x] 12.1 Son échantillonné : n'importe quel son (808, basse, stab d'un pack, son importé) joué sur tout le clavier, note d'origine détectée, glisse façon 808, boucle, enveloppe
- [x] 12.2 Petits « ? » à côté des termes (explication en une ou deux phrases) et Lexique dans Outils › Aide
- [x] 12.3 Pads live : « + » ouvre la bibliothèque de sons, la génération par phrase, puis le téléphone ; export « La scène » au lieu de « La boucle »
- [x] 12.4 Assistant : 808 et basses des packs installés proposées pour la basse, stabs pour le lead

## Phase 13 : instruments classiques et sons actuels (demande de Benjamin)
- [x] 13.1 Packs « machines » : les boîtes à rythmes et synthés qui ont fait l'histoire, au-delà de la 808 (909, 707, 606, CR-78, LinnDrum, SP-1200, MPC, Juno, Jupiter, DX7, Minimoog…), un pack par machine, jouables sur les pads et au clavier
- [x] 13.2 Packs « sons actuels » : 808 modernes (saturées, longues, à glisser), Reese, hoover, supersaw, plucks, wavetable, voix découpées, montées et impacts (FX), log drum, cloche phonk…
- [x] 13.3 Synthé : unisson (jusqu'à 7 voix désaccordées et écartées) pour les supersaws de la future bass et de la trance et les Reese de la drum & bass ; préréglages « actuels »
- [x] 13.4 Générer : phrases d'exemple pour les genres récents (hyperpop, future bass, afro house, amapiano, jersey club, drill, phonk, UK garage…)

## Phase 14 : un look pro et l'écoute partout (demandes de Benjamin)
- [x] 14.1 Nouveau look sur tout le programme : police Inter, panneaux en relief, puces en pilule, curseurs fins au pouce lumineux, vumètres à LED, pads et touches en relief qui s'allument, barre du haut et onglets redessinés
- [x] 14.2 Chaque bloc d'un écran se replie en touchant son titre (petite flèche discrète), et le reste d'une fois sur l'autre
- [x] 14.3 Sons en vignettes (icône et couleur de famille, une ligne d'explication) : sons prêts à jouer, 24 modèles Plaits, instruments, bibliothèque
- [x] 14.4 Clavier qui suit : un clavier compact glisse du bas quand le vrai est hors de portée pendant le choix d'un son (modes Clavier et Notes), se range d'un geste
- [x] 14.5 Assistant : écouter chaque ambiance (ses accords sur le rythme), chaque ligne de batterie avec les lignes déjà choisies, chaque partie avec la batterie ; sources sur une ligne, sons en vignettes juste dessous

## Phase 15 : des styles qui sonnent vrai (demandes de Benjamin)
- [x] 15.1 Recettes de style : pour chaque style, ses morceaux de référence, son tempo, sa batterie, ses accords (rythme et disposition), sa ligne de basse, son riff et, quand c'est sa signature, sa ligne 303
- [x] 15.2 Les meilleurs sons d'abord : chaque son des packs mesuré (attaque, grave, justesse, saturation) et classé ; l'assistant prend la machine du style (909, 808…) et les sons de son pack, et propose de les télécharger
- [x] 15.3 Packs Acid techno et TB-303, et le style Acid techno
- [x] 15.4 Claviers au choix : 2 octaves, 1 octave avec le nom des notes, ou seulement les notes de la gamme (façon GarageBand)
- [x] 15.5 Sample au clavier : le son d'un pad (ou un fichier) joué sur tout le clavier, plus grave à gauche, plus aigu à droite
- [x] 15.6 Effets par pad : la même boîte d'effets agit sur le pad choisi ou sur toute la batterie

## Phase 16 : l'audit « en production » (demande de Benjamin)
- [x] 16.1 Deux audits indépendants (le moteur audio, l'usage réel de l'appli), chaque problème montré par un essai ou un test
- [x] 16.2 Perte de travail : prise micro jamais perdue (Annuler, autre projet, son en cours de pose), historique qui ne garde que ce qui change
- [x] 16.3 Son : solo fantôme, filtre du son général qui se taisait (NaN), coups perdus en Bluetooth, notes bloquées (pédale, clavier MIDI), notes coupées par Annuler
- [x] 16.4 Comme un vrai lecteur : arrêt quand le casque est débranché ou qu'un appel arrive
- [x] 16.5 Calage : prise micro compensée selon la sortie (Bluetooth), note jouée juste avant le « 1 »
- [x] 16.6 Exports : toutes les mesures (3/4…), stems sans les solos du mixage, sans les coupures de la partie qui joue, après le chargement des sons, fin en fondu
