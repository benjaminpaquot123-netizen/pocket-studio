# Lot C « Remix » : note de suivi (branche fx-c)

Consigne : `docs/PLAN-REPRISE.md` étape C, fichiers `docs/CARTE-CODE.md` §2 C. Maquette validée `docs/maquettes/decoupe-deux.png`.

## Fait
- **Nouvelle découpe (n° 24)** : écran « Découper la chanson » (`remix/CutScreen.kt`, `CutWave.kt`, `CutModel.kt`), ouvert par le gros bouton « Découper la chanson » de la carte Remix (le tableau des pistes reste en dessous). Comme la maquette : puces Voix/Batterie/Basse/Reste, vue d'ensemble avec le passage encadré (toucher = y aller), zone zoomée (« Pince pour zoomer ») avec bouts numérotés et colorés, lignes de coupe et poignées, tête de lecture orange ; coupes auto Phrases/Attaques/Mesures (sur le passage affiché, toute la chanson si rien n'est zoomé ; la voix est coupée en phrases dès la première ouverture) ; aimant Au temps / À la mesure / Libre ; « Couper ici » (à la tête de lecture, ou là où ça joue pendant l'écoute), Début, Fin, Enlever (les bouts cochés, sinon celui sous la tête), Boucle (bout calé sur un nombre entier de temps, écouté en boucle) ; coupe déplacée au doigt avec loupe (« 0:29.20 · sur le temps ») et aimant ; Annuler (50 pas, un déplacement = un pas) ; liste « Tes bouts » à cocher, écoute d'un toucher, « Tout choisir » ; barre « N bouts choisis · durée » → Sur les pads (un seul pas d'annulation, pads libres seulement) / Dans le Live / Garder / Sur une piste (une nouvelle piste audio, les bouts à leur place dans la chanson, silence entre eux).
- Tout passe par `StemBoardActions` / `StemDestination` (méthodes ajoutées avec valeur par défaut : `play(sel, loop)`, `savedCuts`, `saveCuts`, `autoCuts`, `sendPieces`). La branche `StemDestination.Live` du routeur n'est pas modifiée : plusieurs bouts vers le Live = la même route appelée bout par bout (`route()` = l'ancien `send` sans le contrôle « occupé »).
- **Modèle** : `model/SongCuts.kt` (règles pures : couper, début, fin, enlever, lignes partagées, aimant, mesures, attaques, boucle) ; champ `Project.songCuts` à la fin, valeur par défaut, absent du fichier quand vide, pas de nouveau numéro de format. Enregistré sans pas d'annulation du projet (`session.editQuietly`, section Stem board de `RhythmViewModel`) : l'écran a son propre Annuler.
- **« Garder » dans le navigateur unique (reste 7d)** : `SoundShelfBrowser.kt` range « Ma bibliothèque » en dossiers par origine (« Ma bibliothèque › Boys »), seulement quand il y a un choix (un seul dossier = la liste telle quelle) ; branché dans `LibraryScreen.kt`. Message après « Garder » plusieurs bouts : « 3 bouts gardés dans Ma bibliothèque, dans le dossier Boys. »
- **Rappel des droits** : `remix/RightsNotice.kt` (`RightsNotice.line(project)`, composable `RightsLine(project)`), rien sans chanson.
- **Voix en notes sur la vraie chanson (reste n° 23)** : `scripts/separation-tests.sh --file chanson.mp3 --out DOSSIER` (MP3 décodé par ffmpeg, séparé par notre code, `separation/host-tests/separate_file.cpp`) puis `scripts/voice-notes-check.py --stems DOSSIER/… --work … --bpm 113` (nos notes `SongMatter.voiceNotes` via le test `VoiceNotesFileCheck`, ignoré sans fichier, comparées à Basic Pitch). Sorties dans le scratchpad uniquement, rien de la chanson dans le dépôt.
- Textes : `i18n/<14 langues>/remixc.json`, une ligne dans `I18n.AREAS`.

## Tests (ciblés, tous verts)
- `MT/SongCutsTest` (9), `T/remix/CutModelTest` (7 : coupes, annuler, déplacement en un pas, coches, sauvegarde, coupes auto sur son synthétique, rappel des droits), `T/remix/CutJourneyTest` (2 : bout en bout sur son synthétique : phrases auto, Couper ici, coupe déplacée avec aimant, 3 bouts → pads en un pas, Annuler, sauvegarde → réouverture ; plusieurs bouts vers Live, piste et bibliothèque), `T/remix/KeepInBrowserTest` (2), `CutScreenshotTest`. Relancés verts : `remix.*`, `library.*`, `StemBoardScreenshotTest`, `*Everywhere*`, `*AppTexts*`, `*Remix*`, `i18n.*`.
- Captures regardées : `cut_screen`, `cut_screen_magnifier`, `library_mine_folders` (neuves), `remix_board_button` (réenregistrée : bouton « Découper la chanson » ajouté).

## Contrôle sur la vraie chanson (ADÉLA - Boys, une fois, 2026-11-03)
Séparation complète sur Linux (~3 min). 416 notes de voix sur toute la chanson (0,9 s à 140,6 s), tempo 113 :
- **Hauteur image par image de Basic Pitch : 364/416 = 87,5 % à ±1 demi-ton** (objectif 80 % : atteint ; tiers de la chanson 93 %, 90 %, 81 %).
- Notes de Basic Pitch (seuils par défaut) : 317/416 de nos notes en recouvrent une, dont 86 % à ±1 demi-ton (65,6 % sur toutes nos notes) ; 21 notes à l'octave. Basic Pitch ne fait pas de note sur les sons courts chantés, d'où la mesure image par image retenue comme objectif.

## Décisions à recopier (DECISIONS)
- 2026-11-03 · Découpe : coupes auto sur le passage affiché (toute la chanson si rien n'est zoomé) ; la voix coupée en phrases à la première ouverture, les autres pistes en mesures (attaques sans tempo). Raison : la maquette montre un passage ; 60+ bouts d'un coup illisibles. Retour : `CutScreen.autoCut(whole = true)`.
- 2026-11-03 · Annuler de la découpe = historique propre à l'écran, coupes enregistrées sans pas d'annulation du projet. Raison : un geste de doigt ne doit pas remplir l'annulation générale. Retour : `saveCuts` avec `session.edit`.
- 2026-11-03 · « Sur une piste » avec plusieurs bouts = une seule nouvelle piste audio, chaque bout à sa place dans la chanson. Raison : une piste n'a qu'un clip ; garde le calage. Retour : `sendPieces` branche Track.
- 2026-11-03 · « Dans le Live » avec plusieurs bouts = la route Live existante, bout par bout (un pas d'annulation par bout). Raison : la branche Live est à B. Retour : un `putOnLivePads` groupé quand B le propose.
- 2026-11-03 · Aimant : « Au temps » / « À la mesure » / « Libre » (« Temps » se traduit déjà « Time »), « Coupes : » au lieu de « Coupes auto : » et Annuler en flèche seule : sinon les menus sont coupés sur un téléphone de 400 dp.
- 2026-11-03 · Voix en notes : objectif mesuré contre la hauteur image par image de Basic Pitch (les notes de Basic Pitch, seuils par défaut, manquent des notes courtes) ; les deux chiffres sont affichés.

## « Règle partout »
- Envois depuis la découpe : une seule porte (`StemDestination` via `StemBoardRouter`), mêmes règles que le tableau des pistes (pads libres seulement, longueurs max, message court) : `CutJourneyTest`, `StemBoardRouterTest` (inchangé, vert).
- « Garder » : tous les écrans qui gardent un son (pads, clips, pads live, Remix) écrivent `MySound.origin` ; le classement par dossier est dans le navigateur unique, donc partout où il montre « Ma bibliothèque » (`KeepInBrowserTest`).
- Voix en notes : même outil possible pour la basse (`bassNotes`) — pas lancé (la consigne visait la voix) : à faire à l'étage 1 de l'Audit si voulu.

## Demandes à l'orchestrateur
- Brancher `RightsLine(project)` d'une ligne sous les boutons de `export/ShareSheet.kt` (fichier de B/E).
- Pluriels polonais/arabe (`|few`, `|many`) des nouveaux `trCount` (« bouts ») : dans `plurals.json` (fichier partagé) si on veut la grammaire exacte ; sinon la forme « other » s'affiche.

## Idées (pas faites)
- Dupliquer, inverser, transposer, étirer une piste de la chanson dans Structure : l'écran Structure (`session/`) n'est pas à C et étirer demande `StemPlayer`/`TimeStretch` (moteur) → lot D ou Audit.
- Séparation coupée qui reprend là où elle s'est arrêtée : aujourd'hui l'appli le dit et propose de relancer ; reprendre à mi-chemin demande des points de reprise dans le séparateur natif.
- Découpe côte à côte avec Structure sur tablette : lot D (prévu).
- Corriger les 21 notes de voix à l'octave (YIN) : pas nécessaire pour l'objectif, à revoir si Benjamin l'entend.

## Prochaine sous-étape
Rien de bloquant : lot C prêt à ranger. Au rangement : réenregistrer `remix_board_button` si conflit, recopier les décisions ci-dessus.
