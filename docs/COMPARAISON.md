# Pocket Studio face à FL Studio Mobile (et aux autres)

Comparaison faite le 2026-10-20 avec FL Studio Mobile 5.0.1 (manuel en ligne Image-Line, notes de version, fiche Play), Koala Sampler, Caustic 3, BandLab, n-Track, Zenbeats et Audio Evolution.
Légende : ✅ présent · 🟡 partiel · ❌ absent · ➖ volontairement non prévu. La dernière colonne dit où c'est prévu (voir `ROADMAP.md`).

## 1. Projets

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Plusieurs projets : ouvrir, nouveau, renommer, dupliquer, supprimer | ✅ | ✅ « Mes projets » (barre du haut), corbeille 30 jours | fait |
| Sauvegarde automatique, copie de secours, rien de perdu | 🟡 (plaintes d'utilisateurs) | ✅ toutes les 0,4 s, écriture atomique + .bak | fait |
| Annuler / rétablir sur tous les écrans | ✅ | ✅ barre du haut | fait |
| Projets de démonstration, modèles de départ | ✅ | 🟡 « Projet vide » et « Rythme de départ » | 6.2 |
| Export / import du projet complet (.zip avec ses sons) | ✅ | ❌ | 6.1 |
| Informations du projet (durée, dates, tempo) | ✅ | 🟡 dans la liste des projets | 6.1 |
| Synchro cloud, partage Wi-Fi | ✅ | ➖ (partage par le menu Android avec le .zip) | — |

## 2. Arrangement

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Morceau construit à partir de motifs | ✅ playlist à clips | 🟡 24 scènes + morceau (scènes × répétitions) | 6.1 export du morceau entier ; 7.3 ligne de temps |
| Pistes ajoutées à volonté, nommées, colorées | ✅ | ❌ 4 pistes fixes (batterie, acide, synthé, remix) | 7.1 |
| Pistes audio (voix, guitare) | ✅ | ❌ | 7.2 |
| Tempo réglable, tap tempo | ✅ | ✅ | fait |
| Changements de tempo / de mesure en cours de morceau | ❌ | ❌ | ➖ |
| Mesures autres que 4/4 | ✅ à la création | ❌ | 7.3 |

## 3. Batterie

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| 16 pads, grille de pas, vélocité | ✅ | ✅ | fait |
| Longueur de la boucle | ✅ | ✅ 1 à 4 mesures (pages, suivi) | fait |
| Pan / hauteur / répétition (roulement) par pas | ✅ | ❌ (vélocité seule) | 6.4 |
| Remplir tous les 1/2/4 pas, décaler, faire tourner | ✅ | ❌ | 6.4 |
| Grille de tous les pads d'un coup | ✅ | ❌ (un pad à la fois) | 6.4 |
| Swing | ✅ | ✅ | fait |
| Sample par pad : début/fin, hauteur, inverse, ADSR, étouffement | ✅ | ✅ (+ calage au tempo, découpe auto) | fait |
| Pan / envoi par pad | ✅ | ❌ | 6.4 |
| Enregistrer les pads joués en direct | ✅ | ✅ bouton ● | fait |

## 4. Piano roll et clavier

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Ajouter, déplacer, allonger des notes | ✅ | ✅ | fait |
| Sélection, copier/coller, dupliquer, transposer | ✅ | ❌ | 6.3 |
| Couloir de vélocité, zoom | ✅ | ❌ (vélocité en 3 niveaux à l'appui long) | 6.3 |
| Grille (triolets, 1/32…), quantification d'une sélection | ✅ | 🟡 quantification 1/16 de tout | 6.3 |
| Legato, humaniser, deviner la gamme, longueur fixe | ✅ | ❌ | 6.3 |
| Gamme, accords, arpégiateur, strum | ✅ | ✅ (arpégiateur sur les accords du piano roll) | fait ; arpège au clavier en 8.3 |
| Enregistrer ce qu'on joue au clavier / au MIDI | ✅ | ✅ bouton ●, calé à la double-croche | fait |
| Décompte, métronome | ✅ | ✅ | fait |
| Clavier : vélocité selon l'appui, pitch bend, molette, sustain | ✅ | ❌ (vélocité fixe) | 8.3 |
| Audio vers notes (chanter une mélodie) | ❌ | ✅ Basic Pitch | fait |

## 5. Instruments

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Synthé soustractif | ✅ 3x Osc, MiniSynth, SuperSaw | ✅ 2 osc + sub + bruit, 2 filtres, 2 env, 2 LFO | fait |
| Basse acide 303 | ✅ Transistor Bass | ✅ Open303 + générateur de motifs | fait |
| Synthé à modèles (FM, cordes, percussions…) | 🟡 GMS | ✅ Plaits, 24 modèles | fait |
| Banques General MIDI (SoundFont) | ✅ | ✅ 3 banques au choix | fait |
| Sampler multi-échantillons, découpeur | ✅ DirectWave, Slicer | 🟡 découpe vers les pads | 7.1 |
| Sons rangés par familles, favoris, hasard | 🟡 | ✅ | fait |
| Plusieurs instruments à la fois (couches, partages) | ✅ | ❌ une source à la fois sur la piste Synthé | 7.1 |

## 6. Audio

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Importer WAV/MP3/FLAC/OGG/M4A | ✅ | ✅ (sons jusqu'à 2 min, packs dossier/.zip) | fait |
| Enregistrer au micro | ✅ piste audio | 🟡 vers un pad (30 s) | 7.2 |
| Éditer un clip : fondus, normaliser, inverser, couper | ✅ | 🟡 début/fin, inverse | 7.2 |
| Séparer un morceau en 4 pistes | ❌ | ✅ HT-Demucs | fait |
| Étirer sans changer la hauteur | ✅ | ✅ pads calés, Remix qui suit le tempo | fait |

## 7. Mixeur et effets

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Volume, pan, mute, solo, VU | ✅ | ✅ | fait |
| Effets d'insert à la chaîne | ✅ illimités | ✅ 4 par voie + 4 sur la sortie | fait |
| Envois (réverbe, écho) | ✅ | ✅ 2 bus | fait |
| EQ, compresseur, saturation, chorus, filtre, pompe | ✅ | ✅ | fait |
| Flanger, phaser, gate, trémolo, élargisseur, bitcrusher | ✅ | ❌ | 8.1 |
| Analyseur, accordeur, loudness | ✅ | ❌ | 8.1 |
| Automation dessinée / enregistrée | ✅ clips | 🟡 8 couloirs sur la boucle de batterie | 8.2 |
| Effets live (pad XY, répéteur, stop bande) | ✅ Multi FX | ✅ | fait |

## 8. Export et partage

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| WAV | ✅ | ✅ 24 bits / 48 kHz | fait |
| Morceau entier (toutes les scènes) | ✅ | ❌ (la boucle en cours) | 6.1 |
| Pistes séparées (stems) | ✅ | ❌ | 6.1 |
| M4A / AAC, FLAC | ✅ | ❌ | 6.1 |
| MP3 | ✅ | ❌ (pas d'encodeur MP3 dans Android) | ➖ M4A à la place |
| Fichier MIDI : export / import | ✅ | ❌ | 6.1 |
| Normaliser, garder / couper la queue | ✅ | 🟡 queue de 2 s | 6.1 |

## 9. MIDI et liaison

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Claviers USB et Bluetooth | ✅ | ✅ | fait |
| Suivre l'horloge MIDI | ❌ | ✅ | fait |
| Associer un bouton MIDI à un réglage (MIDI learn) | ✅ | ❌ | 8.3 |
| Ableton Link, horloge sortante | ❌ | ❌ | 8.3 |

## 10. Interface et réglages

| Fonction | FL Mobile | Pocket Studio | Prévu |
| --- | --- | --- | --- |
| Portrait et paysage | ✅ (portrait depuis 5.0) | ✅ | fait |
| Écran de réglages (thème, notation, Do central, son…) | ✅ | ❌ (2 préférences cachées) | 6.2 |
| Thèmes, dont daltonien | ✅ | ❌ | 6.2 |
| Taille de l'interface | ✅ | ❌ (suit la taille de texte d'Android) | 6.2 |
| Aide, premier pas guidé, astuces | ✅ manuel, vidéos | 🟡 phrases d'aide sur les cartes | 6.2 |
| Mode audio (latence / sûreté) | ✅ | ❌ (réglé automatiquement) | 6.2 |
| Rapport de bug intégré, mise à jour en un geste | ❌ | ✅ | fait |

## Ce que Pocket Studio a en plus
Séparation de morceaux en 4 pistes, Remix calé au tempo et à la tonalité, audio vers notes, générateur de lignes acides, Plaits, pads live façon lanceur de boucles, scènes qui s'enchaînent sur la mesure, horloge MIDI suivie, sauvegarde qui ne perd rien.
