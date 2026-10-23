# Testeur Pro 1 : producteur techno mélodique, 124 BPM, morceau de 3-4 min

Méthode : lecture du code (app + project-model), des chaînes tr(), des captures et de docs/RETOURS.md. Rien lancé. Les numéros de ligne sont ceux relevés à la lecture, à ±10 lignes près. Ce qui est déjà noté « traité » dans RETOURS.md (ligne du temps buggée, échelles d'automation, pads live vides, etc.) n'est pas repris.

## Parcours réel

1. Nouveau projet › assistant. Un style, une ambiance, une ligne par étape : bonne base. Le morceau sort avec des scènes A Intro / B Couplet / C Refrain et une liste de parties (capture session.png). Le plafond est MAX_SONG_SECONDS = 210 s (StarterWizard.kt:1199) : un morceau de 3:30 se génère donc seul. Pour un pro, c'est le point fort de l'appli.
2. Onglet Morceau (SessionScreen). Chaque ligne affiche « A · Intro, × 2, 2 mes. ». Deux chiffres seulement : des répétitions et la longueur de la scène. Impossible d'écrire « 16 mesures » : il faut calculer scène × répétitions. Une partie à longueur libre (`SongPart.bars`) n'existe que si elle vient de « Enregistrer la performance » ou de « Couper la partie ».
3. Je veux un break de 8 mesures sans kick et une mesure de fill avant le drop. Parcours en taps :
   - Scènes › « Dupliquer » (1 tap) crée D, copie de B. Elle copie TOUTES les pistes : batterie, 303, synthé et pistes d'instruments.
   - Scène D › onglet Rythme › vider le kick : 1 tap par pad ou « Effacer » après avoir choisi le pad (2 taps).
   - Ligne du temps › appui long sur le clip › « Changer de scène… » › choisir D : 3 taps, par piste et par partie (TimelineScreen.kt:1512, 1560).
   - Pour un fill sur la dernière mesure d'un couplet de 8 mesures : « Couper la partie à la mesure N » (il faut viser la bonne mesure avec l'appui long), puis une nouvelle scène « fill », puis « Changer de scène… » pour la batterie seule, soit 6 taps ou plus et une scène de plus. Dans Ableton ou FL, c'est 1 clip de 1 mesure glissé sur la piste.
4. Éditer la mélodie du lead (Synthé › Notes, PianoRoll.kt:150-213) :
   - « Boucle » propose 1, 2, 4 ou 8 mesures (`LENGTHS_IN_STEPS = 16, 32, 64, 128`, Notes.kt:152). Pas de 3, 6 ou 12 mesures.
   - Une note touchée est supprimée (« Touche une note : l'enlève »). Une erreur de doigt efface la note au lieu de la sélectionner : l'inverse de FL Mobile, où le tap sélectionne et l'appui long supprime.
   - Copier / Coller / Dupliquer n'existent qu'en mode « ⬚ Sélectionner » (2 taps pour y entrer). Le presse-papiers est un `remember` local (PianoRoll.kt:135) : il est perdu en quittant l'écran. Je ne peux donc pas copier un motif de la piste Lead vers la piste Pad, ni vers une autre scène.
   - Quantifier 50 % / 100 % sont des boutons texte sans effet visible sur le doigt.
5. Batterie (Rythme, rhythm.png). 8 cases par ligne, 16 cases = 2 lignes sur téléphone. Un pad à la fois, ou « Tous les pads ». Il n'y a pas de peinture au glissé : `combinedClickable` (RhythmScreen.kt:555) ne gère que le tap et l'appui long pour la force. Remplir un charley en croches = « Remplir 1/2 » (1 tap), ce qui est bien. Une ghost snare irrégulière = autant de taps que de coups. La longueur est limitée à 1 à 4 mesures (`MAX_STEPS = 64`, Project.kt:214) et commune à tous les pads : polyrythme et motifs de longueurs différentes par pad sont impossibles. Un pattern « 3 contre 4 » ou un perc de 3 temps sur un kick 4/4 ne se fait pas.
6. Automation (Mixeur › Automation, capture automation.png). Trois modes : Boucle, Par scène, Morceau. La courbe Morceau a un point par mesure (« Une valeur par mesure », glisse entre les points) : le balayage de filtre sur 16 mesures est possible. Mais les cibles (AutoTarget, Automation.kt:36-46) sont : volume, pan, envoi, réglage d'un effet d'insert, bus, 303, macros, Live. Aucun paramètre de synthé : la coupure, la résonance, l'enveloppe ou les LFO du Lead et du Pad ne sont pas automatisables. Il faut insérer un effet « Filtre » sur la piste, ce qu'aucune aide ne dit. Pas d'automation de tempo.
7. Mixage. 32 voies, 8 inserts par voie, 2 envois (réverbe, écho) avec un seul réglage global de réverbe et un seul écho (`Buses.COUNT = 2`, Effects.kt:364). Effets : Égaliseur (3 réglages graves / médiums / aigus dans l'enum, Effects.kt:66) plus un EQ paramétrique visuel (param_eq.png). Compresseur : seuil, ratio, attaque, relâche, sans gain de compensation, sans genou, sans sidechain externe (Effects.kt:73-82). « Pompe » (Effects.kt:105) est un ducker maison avec « Déclenche : Grosse caisse / Chaque temps » : il fait le pompage house, mais ce n'est pas un vrai sidechain pilotable (pas de choix de la piste source, pas de courbe, pas d'entrée externe pour un compresseur). Pas de groupes ni de bus de sous-mix (batterie, synthés).
8. Export (Mixeur › Exporter, MixerScreen.kt:564-726). Le choix est bon : morceau entier ou scène, WAV 24 bits / FLAC / M4A, fondu de fin, mesure LUFS, pistes séparées, « Sans limiteur (pour le mastering) » en WAV 32 bits flottants, MIDI. Pour sortir un morceau fini, c'est le plus complet de l'appli.

## Points durs

### 1. Arrangement : on ne peut pas faire de variations locales sans créer une scène par variation
- Gravité : **bloquant** pour un morceau de 3-4 min soigné.
- Où : modèle `Scene` / `SongPart` (Scenes.kt:13-130) ; menu de clip dans TimelineScreen.kt:1477-1570 ; scènes dans SessionScreen.kt (« Dupliquer »).
- Pourquoi : une partie = une scène × répétitions. Modifier un pattern change toutes les parties qui l'utilisent. Un fill sur la dernière mesure, un kick qui sort sur 4 mesures, un riser qui entre à la mesure 13 imposent de dupliquer la scène complète (toutes les pistes), de la renommer, puis de la réaffecter piste par piste avec des menus. Il n'y a ni clip indépendant par piste, ni copie d'un clip sur place, ni « rendre unique ». La limite de 24 scènes (`Scene.MAX`, Scenes.kt:36) devient un vrai plafond à 3-4 minutes avec 6 à 8 variations par section.
- Piste de correction : ajouter à « Dupliquer la partie » une option « Dupliquer en rendant unique ». Elle copierait la scène, pour cette seule piste, dans une scène cachée (ou un nouveau champ `override: Map<piste, NoteClip>` sur `SongPart`) et ouvrirait l'éditeur dessus. Ajouter « Fill de fin de partie » en 1 tap : découper + mettre la batterie sur la scène « fill ». Ableton : chaque clip est indépendant, Ctrl+D le duplique. FL Mobile : les patterns sont placés comme des clips que l'on peut « rendre uniques ».

### 2. Pas de longueur de partie directe
- Gravité : pénible.
- Où : SessionScreen.kt (lignes de parties, boutons « − / + » et « Une fois de plus »), TimelineScreen.kt:1520-1531.
- Pourquoi : on ne règle que × répétitions. Pour 12 mesures avec une scène de 8, il faut couper une partie répétée 2 fois. Il n'y a pas de poignée de bord de clip pour étirer.
- Piste de correction : sur la ligne du temps, glisser le bord droit d'un clip étire la partie (champ `bars` existe déjà sur `SongPart`) ; saisie d'un nombre de mesures. Ableton : on tire le bord ou on tape la longueur dans le Clip View. FL : on étire le clip dans la playlist.

### 3. Batterie : une seule longueur pour tous les pads, pas de peinture au glissé, 4 mesures au plus
- Gravité : pénible, bloquant pour le polyrythme.
- Où : Project.kt:203-214 (`DrumPattern`, `MAX_STEPS = 64`, `MIN_STEPS = 8`) ; RhythmScreen.kt:540-570 (`combinedClickable` seulement).
- Pourquoi : pas de longueur par pad (percussion en 12 pas sur un kick en 16), pas de glisser pour peindre des cases, pas de probabilité ni de condition par pas, pas de copie / coller d'une mesure à l'autre dans une boucle de 4 mesures.
- Piste de correction : longueur de boucle par pad (`DrumLane.steps`), glisser pour peindre ou effacer comme FL Mobile, copier / coller des mesures en mode « Tous les pads », option « 1 mesure sur N » (probabilité). Ableton : chaque piste d'un Drum Rack peut avoir son propre clip et sa propre longueur de boucle.

### 4. Piano roll : gestes risqués et copier / coller limité
- Gravité : pénible.
- Où : PianoRoll.kt:135 (presse-papiers local), 150-213 (barre d'outils), 212-213 (aide : « Touche une note : l'enlève »), 244-277 (sélection).
- Pourquoi :
  - un tap supprime la note ;
  - il faut basculer en « Sélectionner » pour copier ;
  - le presse-papiers ne survit pas à un changement d'écran ou de piste ;
  - boucles limitées à 1, 2, 4 ou 8 mesures ;
  - une seule longueur de note à poser (« Note »), pas d'étirement multiple ;
  - pas de CC, de pitch bend ni de probabilité par note ;
  - pas de bouton « Dupliquer la mesure » en un tap.
- Piste de correction : tap = sélectionner, appui long = supprimer, double-tap = ajouter. Presse-papiers dans le ViewModel, partagé entre pistes et scènes. Boucles de 1 à 8 mesures au pas d'une mesure. Glisser les poignées de vélocité sur plusieurs notes. FL Mobile : sélection multiple, copier / coller d'une piste à l'autre, longueur en barres libre.

### 5. Automation : pas de paramètres de synthé, pas de tempo, pas d'automation de clip par piste
- Gravité : bloquant pour un lead de techno mélodique (le filtre qui s'ouvre sur 16 mesures est le geste principal du genre).
- Où : Automation.kt:36-46 (`AutoTarget.isValid`), MixerScreen.kt:231-250 (« Automatiser un réglage »).
- Pourquoi : les cibles se limitent à volume, pan, envoi, effets d'insert, 303, macros et Live. La coupure du synthé (SynthPatch) ou du Plaits n'est pas ciblable. Le contournement (un effet « Filtre » en insert) n'est dit nulle part dans l'écran Synthé. Pas de courbe de tempo (ralentir avant le drop). Le mode Morceau n'a qu'un point par mesure : impossible de faire une montée en 1 temps ou un coup de filtre sur la dernière noire.
- Piste de correction : exposer chaque potard du synthé (« Automatiser » en appui long sur un potard, comme dans Live ou FL). Résolution en 1/4 de mesure. Piste de tempo. Ableton : clic droit › « Automatiser » sur n'importe quel potard, courbes par clip ou par morceau.

### 6. Pas de vrai sidechain, pas de groupes, 2 envois seulement
- Gravité : pénible.
- Où : Effects.kt:73-82 (`COMPRESSOR`), 105 (`PUMP`), 364 (`Buses`).
- Pourquoi : « Pompe » ne se règle que sur « Grosse caisse » ou « Chaque temps ». Le compresseur n'a pas d'entrée sidechain. Pas de gain de compensation, pas de bus de sous-mix. Seulement une réverbe commune et un écho commun, donc pas de réverbe courte (ambiance) à côté d'une longue (pad). Master : limiteur fixe, sans seuil réglable ni mesure de gain réduit détaillée.
- Piste de correction : ajouter un champ « Source : piste » au compresseur et à Pompe ; 4 envois au lieu de 2 ; bus de groupe. Ableton : « Audio From » dans le compresseur. FL : Fruity Limiter / Peak Controller et routage de pistes de mixeur vers un autre.

### 7. Pas de gel ni de rééchantillonnage d'une piste
- Gravité : pénible.
- Où : RhythmScreen.kt « Capturer tout le morceau » (rééchantillonne le master vers un pad) ; AudioTracks.kt:31 (`MAX = 4` pistes audio).
- Pourquoi : on peut capturer toute la sortie dans un pad, mais pas bouncer une seule piste sur une piste audio (basse resamplée avec sa distorsion, puis coupée / réversée pour un riser). 4 pistes audio seulement.
- Piste de correction : « Geler la piste » : rendu hors-ligne d'une piste + inserts en clip audio, avec Annuler. Ableton « Freeze / Flatten » ; FL « Render to audio clip ».

### 8. Transitions : peu d'outils (risers, fills, impacts) hors Live
- Gravité : pénible.
- Où : AutoTarget.LIVE_RISER / LIVE_DOWNLIFTER / LIVE_BUILD_UP (Automation.kt:78-100, libellé « Souffle montant » AutomationCard.kt:208) ; SongGuide.kt:49 (piste de riser de l'assistant).
- Pourquoi : les risers et montées existent comme gestes Live enregistrables dans une courbe Morceau, mais pas comme un clip posé à la mesure 8 d'une partie. Pas de bibliothèque « fills » (roulements de caisse claire, reverse crash) posable en 1 tap sur la dernière mesure d'une partie. Les sons FX des packs 13.2 existent, mais il faut les mettre sur un pad ou une piste et les déclencher soi-même.
- Piste de correction : menu de partie « Transition de fin » avec Montée / Fill / Reverse crash / Silence d'une mesure. Ableton : clips d'effets et automation sur le début de la scène suivante.

### 9. Lisibilité de la structure sur téléphone
- Gravité : détail.
- Où : session.png, timeline.png.
- Pourquoi : la liste Morceau affiche le total en mesures et les temps, ce qui est bien. Mais pas de mesure de départ de chaque partie (« mes. 17 »), et sur une scène de 8 mesures × 4 le texte « × 4 · 4 mes. » se lit mal. Les boutons « + A + B + C + D + J » en bas ne montrent que les premières scènes.
- Piste de correction : afficher « mes. 17-24 » sur chaque ligne et un résumé « 6 parties · 96 mes. · 3:05 ».

### 10. Modification d'une scène partagée : risque de casser une autre partie
- Gravité : pénible.
- Où : Scenes.kt:13-45 ; SessionScreen.kt (« Une scène = la batterie, l'acide et le piano roll »).
- Pourquoi : éditer le refrain C met à jour tous les refrains, ce qui est voulu, mais rien n'avertit qu'une scène est utilisée dans N parties, ni ne propose « modifier seulement cette partie ».
- Piste de correction : bandeau « Utilisée dans 4 parties » + bouton « Rendre unique ».

## Fonctions manquantes pour finir un morceau complet, classées

1. Clips indépendants par piste et par partie : variations, fills et sorties d'instrument sans dupliquer toute une scène.
2. Automation des paramètres du synthé et de Plaits (filtre, résonance, désaccord), plus une résolution plus fine que la mesure.
3. Longueur de partie libre (étirer le bord d'un clip) et copier / coller de clips entre pistes et scènes.
4. Sidechain réel (source au choix), gain de compensation du compresseur, bus de groupes, plus de 2 envois.
5. Gel / bounce d'une piste vers une piste audio, plus de 4 pistes audio.
6. Batterie : longueur par pad (polyrythme), peinture au glissé, probabilité par pas.
7. Piano roll : sélection par tap, presse-papiers global, boucles à la mesure près, CC et pitch bend.
8. Outils de transition en 1 tap (riser, fill, reverse crash) posés à la mesure.
9. Automation de tempo et changements de tonalité par partie (transposition de la section).
10. Mastering intégré : limiteur réglable, analyse du spectre sur le master, courbe de loudness (la mesure LUFS existe à l'export).

## Top 5 des priorités

1. Variations par piste (clip rendu unique + fill en 1 tap) : sans cela, 3-4 min exigent 10 à 20 scènes et 5 à 7 taps par fill.
2. Automation des potards du synthé (filtre du lead) et résolution inférieure à la mesure.
3. Étirer la longueur d'une partie au bord du clip et copier / coller des clips.
4. Sidechain réel avec source et bus de groupes (plus d'envois).
5. Piano roll : tap = sélection, presse-papiers global, durée de boucle à la mesure ; batterie : peinture au glissé et longueur par pad.
