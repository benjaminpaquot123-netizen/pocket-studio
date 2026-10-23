# Audit des gestes : faire, finir et partager un morceau complet

Lecture du code de `/home/user/ps-p22` (version 0.22.173, après la phase 32). Rien n'a été lancé. Point de départ : les 8 rapports de `docs/audit-33/`. Ce document ne refait pas leur diagnostic : il le chiffre en gestes.

## Méthode

- **Point de départ** : un projet ouvert sur l'onglet Morceau, comme juste après l'assistant (`ui/AppRoot.kt:489` et `:535`). Le morceau a 6 parties (Intro, Groove, Drop, Break, Drop, Intro) et environ 8 pistes. Téléphone en portrait, 360 à 412 dp. Aucune carte repliée : le `MemoryFoldStore` part tout ouvert (`ui-components/.../Components.kt:90`).
- **Ce qui compte pour 1 geste** : une touche, un défilement (« défil. »), un appui long, un glisser ou une saisie de texte. Le chiffre en tête de case est le total. Quand l'action se fait déjà dans le bon éditeur, le compte « sur place » est indiqué à part.
- **Chemins de fichiers** : relatifs à `app/src/main/java/com/pocketstudio/app/`, sauf mention contraire.
- **Références d'autres applis** : quand le chiffre vient d'un manuel, la source est citée en bas. Le signe « ≈ » marque un ordre de grandeur tiré de l'usage courant, non revérifié sur l'appareil. « — » veut dire qu'on ne connaît pas d'équivalent fiable.
- **Cible** : le nombre de gestes visé avec le changement proposé et la navigation cible (voir la partie Navigation).

Rappel de ce qui structure les comptes d'aujourd'hui :
- **7 onglets** : Rythme, Acide, Synthé, Mixeur, Audio, Morceau, Outils (`ui/AppRoot.kt:173-180`).
- **▶ en haut** : il joue le morceau sur l'onglet Morceau, mais seulement la section affichée sur les autres onglets tant que l'utilisateur n'a rien choisi (`rhythm/RhythmViewModel.kt:1578-1581`, `:5513`). La bascule « Écouter » n'existe que dans Rythme et Morceau.
- **Pas d'actions contextuelles** : une partie n'a qu'un menu ⋮ (`session/SessionScreen.kt:527-553`). La ligne du temps est une vue à part, avec des appuis longs cachés derrière « ? » (`session/TimelineScreen.kt:738-742`).
- **Portée d'une scène** : les éditeurs agissent sur la scène affichée, qu'on choisit avec la puce de la barre du haut (2 gestes, `ui/TopBar.kt:284-347`).

## Tableau : 41 actions

### A. Démarrer et écouter

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 1 | Créer un morceau complet à partir d'un style (projet déjà ouvert) | **5**, +1 pour l'écouter. Chemin : Projet (`ui/TopBar.kt:180`) › « Nouveau projet : style, démo ou vide » (`projects/ProjectsScreen.kt:150`) › déplier la famille, toutes repliées au départ (`projects/NewProjectPage.kt:98`) › carte du style (`:212`) › guide de 5 pages, « Passer » (`settings/GuideDialog.kt:111`) | BandLab SongStarter ≈ 3-4, mais pour une idée courte, pas un morceau entier. FL Mobile et GarageBand n'ont rien d'équivalent. | **3**, et le morceau joue déjà | « + Nouveau » directement dans Mes projets. Styles à plat : 6 récents ou populaires, plus une recherche, sans famille repliée. À l'arrivée, écran « Ton morceau est prêt » où le morceau joue déjà, avec 4 gros boutons (Écouter, Changer un son, Structure, Partager) à la place du guide de 5 pages. |
| 2 | Écouter tout le morceau | **1** sur Morceau. **2** depuis Rythme : bascule « Écouter » (`rhythm/RhythmScreen.kt:252`) puis ▶. **2** depuis Synthé, Acide ou Audio : aller sur Morceau puis ▶, car ces écrans n'ont pas la bascule et leur ▶ boucle 1 à 4 mesures (`RhythmViewModel.kt:1581`) | 1 partout (FL Mobile, GarageBand, BandLab) | **1** partout | ▶ joue le morceau dès qu'il en existe un (`listenSong = true` à la création). Une puce « ⟳ Section » à côté de ▶ sert à boucler la partie qu'on édite. Le bouton dit ce qu'il joue. |
| 3 | Jouer à partir d'une partie | **1-2** : toucher la ligne de la partie (`session/SessionScreen.kt:496`), +1 défil. dès la 4e partie, rangée sous les blocs Live et Ligne du temps. **3** depuis un autre onglet. | ≈ 2 : toucher la règle, puis ▶ | **1** sur Structure, **2** ailleurs | Structure s'ouvre sur les parties, sans défilement. Un appui long sur ▶ affiche la liste des parties. |

### B. Sons

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 4 | Changer tout le kit de batterie | **≈ 9**, plus un téléchargement, par un détour dans Synthé : Synthé › défil. › « Instruments » (`instruments/InstrumentPanel.kt:100`) › télécharger la banque (1re fois) › défil. horizontal › « Kits de batterie » (`:163`) › un kit › « Mettre sur les 16 pads » (`:181`) › remettre « Synthé » sur la piste, que le kit a remplacé. Pad par pad, c'est **≈ 80** : Rythme › pad › défil. › « Changer le son » (`rhythm/RhythmScreen.kt:710`) › catégorie › « Poser », le tout 16 fois. | FL Mobile ≈ 3 (changer le kit de la piste batterie). GarageBand ≈ 2. | **2** depuis la batterie, **3** d'ailleurs | Puce « Kit : 909 ▾ » en tête de la batterie. Elle ouvre une feuille de kits à écouter (intégrés, packs machines, kits GM) ; un toucher remplace les 16 pads sans toucher au son de la piste synthé. Réutilise `loadKit` (`RhythmViewModel.kt:3971`). |
| 5 | Changer le son de la basse pour tout le morceau | **3-5**. Si c'est la basse acide : Acide › défil. 1-2 › tuile (`acid/AcidScreen.kt:392`). Si c'est la basse synthé : Synthé › piste « Basse » (`synth/TrackBar.kt`) › défil. sous le clavier › famille › tuile. Avant tout, il faut deviner laquelle des 3 « Basse » est la bonne : celle du pad, du synthé ou de l'acide. | GarageBand ≈ 2-3. BandLab ≈ 3. | **2** | Toucher le nom de la piste (dans Structure ou Sons) ouvre la feuille « Changer le son ». Elle propose les sons du même rôle (basse, 808, sub, acide 303), avec ▶ sur chaque tuile. |
| 6 | Changer un instrument (piano GM…) pour tout le morceau | **6**, plus un téléchargement. Chemin : Synthé › piste › défil. › « Instruments » (`InstrumentPanel.kt:100`) › famille › instrument | FL Mobile ≈ 3 | **2** | La même feuille « Changer le son », avec un onglet « Instruments » qui propose le téléchargement sur place. |
| 7 | Changer le son d'un pad | **5-6**. Chemin : Rythme › pad › défil. › « Changer le son » (`RhythmScreen.kt:710`) › catégorie › « Poser » (`library/LibraryScreen.kt:165`) | FL Mobile ≈ 2-3 (toucher le son dans le rack) | **2** | Appui long sur le pad : une feuille Son, Volume, Pan, avec les sons du même type en tuiles. |
| 8 | Ajouter une piste d'instrument | **3**. Chemin : Synthé › « + Piste » (`synth/TrackBar.kt:144`) › type (`synth/TrackAdd.kt:64`). Pour une 808 avec glisse, **≈ 8** : Synthé › Instrument › Sample › Bibliothèque › chercher › Poser › cocher « Glisse » (`instruments/SampledPanel.kt:80`). | GarageBand et BandLab ≈ 2-3 | **2** | Sons › « + », avec les choix Mélodie, Accords, Basse, 808 (glisse déjà active), Voix (micro), Échantillon, Générer. |

### C. Batterie

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 9 | Ajouter un coup | **3** depuis Morceau : Rythme › pad › case (`RhythmScreen.kt:561`). **1-2** sur place, selon que le pad est déjà choisi. Si la partie voulue n'est pas affichée, +2 pour la puce de scène. | 1 (FL Mobile : toucher le pas) | **1** sur place, **2** depuis Structure | Sur téléphone, la grille « tous les pads » devient la vue par défaut (une rangée par pad, qui défile). Double-toucher le clip Batterie d'une partie ouvre cette grille. |
| 10 | Peindre des charleys | **3** pour des croches régulières : Rythme › pad › « Remplir 1/2 » (`RhythmScreen.kt:626`). **8 à 16** pour un motif libre, une touche par case, car les cases ne gèrent que le toucher et l'appui long (`combinedClickable`, `RhythmScreen.kt:556`). | GarageBand, séquenceur pas à pas : 1 glisser ≈. FL Mobile : une touche par pas (manuel). | **1** glisser | Glisser le doigt allume les cases traversées, ou les éteint si la première case était allumée. |
| 11 | Ajouter un roulement | **5-7**. Chemin : Rythme › pad › puce « Roulements », dans une rangée qui défile (`RhythmScreen.kt:612`) › 1 à 3 touches sur la case (×2, ×3, ×4) › revenir à « Coups ». Le roulement ne fait que décroître et s'arrête à ×4. | FL Mobile : « Note repeat », jusqu'à 8 répétitions par pas, dans l'éditeur de graphes (≈ 2-3, manuel) | **2** | Appui long sur un coup : menu rond avec ×2 ×3 ×4 ×6 ×8, « montant » et la force. Aujourd'hui, l'appui long fait seulement tourner la force (`RhythmScreen.kt:567`). |
| 12 | Volume d'un pad (le kick plus fort) | **6**, et seulement si le pad porte un échantillon. Chemin : Rythme › pad › défil. › « Régler le son » (`RhythmScreen.kt:748`) › « Enveloppe, volume… » (`:888`) › curseur (`:893`). Le Mixeur n'a qu'une voie pour toute la batterie. | FL Mobile ≈ 2 | **2** | La même feuille d'appui long sur le pad. Les pads deviennent aussi des sous-voies dépliables dans la console Mix. |

### D. Notes et harmonie

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 13 | Ajouter une note dans une partie précise | **3** depuis Morceau : Synthé › « Notes » › case. +1 s'il faut changer de piste, +2 pour la bonne section avec la puce de scène (`ui/TopBar.kt:284`). **1** sur place. | FL Mobile : 2 (double-toucher le clip ouvre l'éditeur, puis toucher la grille) | **2** | Double-toucher le clip d'une piste dans une partie, dans Structure, ouvre le piano roll de cette partie. |
| 14 | Modifier la batterie ou une piste d'une partie précise (ex. le refrain) | **3** : puce de scène (2) + onglet. Ou bien : Ligne du temps › appui long sur le clip › « Ouvrir dans Rythme » (`session/TimelineScreen.kt:1502`). | 1 (FL Mobile : double-toucher le clip) | **1** | Double-toucher le clip dans Structure. |
| 15 | Enregistrer une mélodie avec décompte | **3-4** : Synthé › (piste) › ● dans la barre du haut (`ui/TopBar.kt:228`) › ● pour finir. Le décompte est d'une mesure par défaut (`settings/Settings.kt:51`). Pour entendre le métronome pendant la prise : +4 (Rythme › défil. ×2 › « Métronome », `RhythmScreen.kt:669`). Pour viser la bonne section : +2. | GarageBand : 2 (● avec décompte, puis ■) ≈ | **2** | Gros bouton « ● Enregistrer » à côté du clavier. Décompte et métronome par défaut pendant la prise. La prise va dans la partie choisie dans Structure. |
| 16 | Remettre la prise en rythme (quantifier) | **2-3** : « Notes » › « 100 % », dans une barre qui défile (`synth/PianoRoll.kt:198`). Un débutant ne reconnaît pas le mot « Quantifier ». | GarageBand ≈ 3-4 (Commandes de piste › Quantification, manuel Apple). FL Mobile ≈ 4 (sélection › menu › Snap, manuel). | **1** | Carte affichée après la prise : « Garder · Remettre en rythme · Refaire ». Pour les pros, la force (50, 75 ou 100 %) se règle sur le même bouton. |
| 17 | Changer les accords de tout le morceau | **≈ 30**. Pour une scène et une piste, 7 gestes : Synthé › piste › Notes › défil. ×2 › progression (`PianoRoll.kt:744`) › « Accords » (`:752`). Puis ≈ 3 par autre scène (puce de scène ×2, puis « Accords »), et à nouveau 4 + 3 par scène pour la basse. Changer la tonalité ne déplace pas les notes des autres pistes (`PianoRoll.kt:733`). | Pas d'équivalent mobile connu. Sur ordinateur, une piste d'accords (Cubase, Studio One) se règle en un seul geste global. | **3** | « Accords du morceau » : une bande d'accords sous la règle de Structure, avec les noms (Am F C G). On choisit une progression, on l'écoute, puis « Appliquer à tout le morceau » réécrit accords, basse et arpège dans toutes les sections, en suivant la tonalité. |
| 18 | Copier des notes vers une autre piste ou une autre partie | **Impossible** : le presse-papiers ne vit que dans l'écran ouvert (`PianoRoll.kt:135`), il faut tout retaper | FL Mobile ≈ 4 (Copier, puis appui long › Coller, manuel) | **4** | Presse-papiers gardé dans le ViewModel et partagé entre pistes et sections. Comme dans FL Mobile : toucher sélectionne, appui long supprime. |

### E. Structure

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 19 | Ajouter une partie | **2** à la fin : défil. › « + C » (`SessionScreen.kt:349`). **3-4** au milieu : ⋮ › « Insérer une scène après » (`:547`) › choix. **≈ 6** pour une partie au contenu neuf : grille Scènes › « Nouvelle » (`:605`), puis l'insérer. | GarageBand (sections) ≈ 2-3 | **2** | Un « + » entre deux parties ouvre les choix Intro, Couplet, Refrain, Break, Drop, Fin, « Copie de… », Vide. |
| 20 | Dupliquer une partie | **2-3** : défil. › ⋮ (`:527`) › « Dupliquer » (`:543`) | FL Mobile : 4 (Copier › appui long › Coller, manuel) | **2** | Toucher l'étiquette de la partie ouvre sa feuille, avec « Dupliquer » en premier. |
| 21 | Faire une variation (une copie modifiable sans toucher l'original) | **≈ 9** : grille Scènes (défil. ×2) › scène › « Dupliquer » (`:606`) › ⋮ › « Insérer une scène après » › choix › retirer l'ancienne partie (2). Autre chemin : « Changer de scène… » piste par piste (`TimelineScreen.kt:1512`), 3 gestes par piste. | Sur ordinateur, Ableton et FL Studio rendent un clip unique en 1-2 gestes | **2** | « Variation » dans la feuille de partie : copie la section, la pose à la place et ouvre l'éditeur. Un bandeau « Section jouée 3 fois : modifier toutes / seulement celle-ci » prévient avant de modifier. |
| 22 | Déplacer une partie de 3 places | **6** : ⋮ › « Monter », trois fois (`:535`). Ou **2**, mais caché : Ligne du temps › appui long et glisser sur un drapeau (`TimelineScreen.kt:1004`). | 1 glisser (GarageBand, BandLab, FL Mobile) | **1** | Poignée ≡ ou appui long et glisser dans Structure, avec une bulle d'aide la première fois. |
| 23 | Supprimer une partie | **2-3** : ⋮ › « Retirer du morceau » (`:551`) | 2 | **2** | Dans la feuille de partie, suivi d'un message « Annuler ». |
| 24 | Allonger une partie (de 8 à 16 mesures) | **2-4** touches sur « + », selon la longueur de la scène (`:524`). On règle un nombre de répétitions, jamais une longueur en mesures. | FL Mobile : 1, en tirant le bord du clip (manuel) | **1** | Tirer le bord droit de la partie, aimanté à la mesure. Une puce « 16 mes. » dans la feuille de partie fait la même chose. |
| 25 | Faire un break | **6**, mais en coupant aussi les charleys : Ligne du temps (`SessionScreen.kt:256`) › appui long sur un clip › « Dupliquer la partie » (`TimelineScreen.kt:1536`) › toucher les clips Batterie, Basse et Acide (`:1171`). La batterie n'est qu'une seule rangée. Pour garder les charleys, **≈ 15** : nouvelle scène, effacer le kick et la caisse claire pad par pad, réaffecter. | — | **2** | « Break après » dans la feuille de partie : une copie sans kick ni basse, qui garde charleys et nappe, avec un filtre doux. |
| 26 | Ajouter un fill en fin de partie | **≈ 16** : appui long sur la bonne mesure › « Couper la partie à la mesure N » (`TimelineScreen.kt:1531`) › fermer › Scènes › Dupliquer › Rythme › éditer (≈ 6) › Morceau › Ligne du temps › appui long sur le clip Batterie › « Changer de scène… » › choix | GarageBand Drummer ≈ 2 (réglage « Fills » de la région) | **2** | « Fill » dans la feuille de partie : caisse claire, roulement montant, toms ou silence d'une mesure. Il remplace seulement la dernière mesure de cette partie. |
| 27 | Ajouter une fin | **≈ 6** : défil. › « + A » (`SessionScreen.kt:349`) › Ligne du temps › couper 2-3 pistes. Le seul fondu existe à l'export (`mixer/MixerScreen.kt:604`). | — | **2** | Un « + » après la dernière partie propose « Fin » : l'intro allégée, un impact et un fondu écrit dans le morceau. |
| 28 | Couper une piste dans une seule partie | **2** : Ligne du temps › toucher le clip (`TimelineScreen.kt:1171`). +1 défil. horizontal si la partie est hors de l'écran. | Ableton : 1 (désactiver le clip) | **1** | Structure devient la ligne du temps : le toucher est direct. |

### F. Transitions et automation

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 29 | Montée de filtre avant le drop | **≈ 11** : Mixeur › « Automatiser » (`MixerScreen.kt:244`) › « Automatiser un réglage » (`mixer/AutomationCard.kt:405`) › piste › « Filtre (balayage) » (`:996`) › mode « Morceau », car le filtre démarre en mode Boucle (`RhythmViewModel.kt:3487`) › outil « Ligne » (`AutomationCard.kt:668`) › 2 touches › retrouver les bonnes mesures (zoom ou défil. 1-2), alors que l'axe ne porte aucun nom de partie. Le filtre propre du synthé n'est pas automatisable. | FL Mobile ≈ 4 plus le dessin (CTRL › ajouter une piste d'automation › clip, manuel) | **2** | « Montée avant » dans la feuille de la partie Drop : filtre et souffle sur 4 ou 8 mesures, réglables ensuite. Les noms des parties apparaissent sur l'axe des courbes. |
| 30 | Fondu d'entrée de l'intro | **8 par piste** : Mixeur › Automatiser › Automatiser un réglage › piste › Volume › Ligne › 2 touches. Le volume général ne s'automatise pas, donc il faut **≈ 30** gestes pour tout le mix. L'export ne propose qu'un fondu de fin. | GarageBand ≈ 3 plus les points (automation de la piste master) | **2** | « Fondu d'entrée » dans la feuille de l'Intro, et « Fondu de sortie » sur la dernière partie. Ils écrivent une courbe de volume général sur le morceau. |

### G. Mixage

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 31 | Régler le volume d'une piste | **2-4** : Mixeur › défil. 0-2, car chaque piste occupe une carte (`MixerScreen.kt:448`) › glisser | FL Mobile : 2 (onglet mixeur, puis fader) ≈ | **2** | Console horizontale où toutes les voies sont visibles, faders verticaux. Ou appui long sur l'en-tête de la piste dans Structure. |
| 32 | Couper une piste ou l'écouter seule | **2-4** : Mixeur › défil. › « Muet » ou « Solo » (`MixerScreen.kt:476-483`). Ou Ligne du temps › M ou S (`TimelineScreen.kt:893`). | 1 partout | **1** sur Structure et Mix | M et S sur chaque en-tête de piste de Structure. Un bandeau « Solo actif » s'affiche sur tous les écrans. |
| 33 | Rééquilibrer le mix après un changement | **Aucune fonction** : ≈ 10 gestes, un fader après l'autre à l'oreille | Sur ordinateur, Logic « Mix Assistant » : 1 ≈ | **1-2** | Mix › « Équilibrer » : refait le calage de l'assistant (`WizardMix`), avec Annuler. |

### H. Enregistrer

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 34 | Enregistrer la voix sur tout le morceau | **5-6**, plus l'autorisation du micro la première fois. Chemin : Audio › « + Piste audio » (`audiotracks/AudioTracksScreen.kt:168`) › « Tout le morceau » (`:213`) › (partie de départ) › « Enregistrer » (`:298`) › « Arrêter et garder » (`:251`) | BandLab ≈ 3 (+ › Voix › ●). GarageBand ≈ 4. | **3** | Sons › « + Voix » crée la piste, armée sur le morceau à partir de la partie choisie › ● › ■. Pendant la prise, un compteur « mes. 5/16 · Couplet ». |
| 35 | Enregistrer une performance live | **3-4** : Morceau › « Live » (`SessionScreen.kt:237`) › (le choix par défaut est « Remplacer le morceau ») › REC (`live/LiveScreen.kt:529`) › « ■ Fin » (`:580`). Les pads live ne sont pas enregistrés. | GarageBand Live Loops : 2 (●, puis ■) ≈ | **3** (onglet Live › ● › ■) | Un onglet Live à part. Par défaut, la prise crée une nouvelle version (copie) au lieu d'écraser le morceau. |

### I. Finir, partager, projets

| # | Action | Aujourd'hui (gestes, chemin) | Référence autre appli | Cible | Changement proposé |
|---|---|---|---|---|---|
| 36 | Annuler | **1** (`ui/TopBar.kt:217`). Inactif pendant une prise. | 1 | **1** | On garde. Un message « Annulé : … » dit ce qui a été défait. |
| 37 | Partager le morceau en M4A | **4**, plus une saisie : Mixeur › « Partager mon morceau » (`MixerScreen.kt:424`) › nom (`export/ShareSongUi.kt`, `NameSongDialog`) › appli. Autre chemin : Projet › « Partager mon morceau » (`projects/ProjectsScreen.kt:270`). | BandLab ≈ 3. GarageBand ≈ 5. | **2** | ⤴ dans la barre du haut, sur tous les écrans. Il ouvre une feuille « Prêt à partager » : durée, alertes (solo actif, saturation), nom déjà rempli. Puis le choix de l'appli. |
| 38 | Exporter en WAV | **4** : Mixeur › « Autres exports » (`MixerScreen.kt:434`) › défil. › « Exporter le morceau » (`:692`). Le WAV est déjà le format par défaut de cette carte (`RhythmViewModel.kt:238`). | FL Mobile ≈ 3-4 | **3** | ⤴ › puce « WAV » › « Exporter ». Le dernier format choisi est mémorisé. |
| 39 | Renommer le projet | **4**, plus une saisie : Projet › ⋮ (`ProjectsScreen.kt:277`) › « Renommer » (`:281`) › OK | GarageBand ≈ 3 | **2** | Appui long sur le nom dans la barre du haut : on le modifie sur place. Le nom est aussi demandé à la fin de l'assistant. |
| 40 | Ouvrir un autre projet | **2** : Projet › projet | 2-3 | **2** | On garde. |
| 41 | Changer le tempo (de 124 à 140) | **2 à 17** : Rythme › « + » maintenu, ou 16 touches (`RhythmScreen.kt:268-279`). Aucun champ de saisie, et le BPM de la barre du haut ne réagit pas au toucher (`TopBar.kt:249`). | FL Mobile ≈ 2 (toucher le tempo) | **2** | Toucher le BPM dans la barre du haut ouvre un pavé numérique, des puces 70, 90, 120, 140, 174, et ½ / ×2. |

**Déjà au mieux, à garder** : Annuler (1), jouer depuis une partie sur Morceau (1), ouvrir un projet (2), dupliquer ou supprimer une partie (2).

## Navigation : ce que changent les 5 onglets de Pro 3 et le Mode simple

Pro 3 propose 5 onglets : Sons / Structure / Mix / Live / Plus. Sa barre du haut contient le projet, la section, ▶ (Section ou Morceau), ↶, ●, ⤴ et l'aide.

Pour comparer, on prend 15 actions-clés. Les colonnes donnent le nombre de gestes dans chaque cas :
- **Aujourd'hui** : les 7 onglets actuels.
- **Pro 3, navigation seule** : on change les onglets, sans ajouter les feuilles contextuelles.
- **Pro 3 + contexte** : la cible complète, avec les feuilles de partie, de piste et de pad.
- **Mode simple** : 4 onglets (Mon morceau · Rythme · Clavier · Envoyer).

| Action | Aujourd'hui | Pro 3, navigation seule | Pro 3 + contexte (cible) | Mode simple |
|---|---|---|---|---|
| Écouter tout le morceau | 1-2 | 1 | 1 | 1 |
| Jouer depuis une partie | 1-3 | 1-2 | 1 | 1 |
| Changer le son de la basse | 3-5 | 3-4 (Sons › Basse › défil. › son) | 2 | 2 (Mon morceau › nom de piste › son) |
| Changer le kit | 9 | 9 | 2 | 2 |
| Ouvrir la batterie du refrain | 3 | 2 | 1 | 1 |
| Couper une piste dans une partie | 2 | 1 | 1 | 1 |
| Déplacer une partie | 2-6 | 1 | 1 | 1 |
| Volume d'une piste | 2-4 | 2 (console) | 2 | 2 (feuille de piste) |
| Enregistrer une mélodie | 3-4 | 3 | 2 | 2 (Clavier › ● Enregistrer ma mélodie) |
| Enregistrer la voix | 5-6 | 4 | 3 | 3 |
| Montée de filtre avant le drop | 11 | 10 | 2 | 2 |
| Fill en fin de partie | 16 | 14 | 2 | 2 |
| Partager | 4 | 3 | 2 | 2 |
| Exporter en WAV | 4 | 3 | 3 | menu ⋯ |
| Ouvrir le Live | 1-2 | 1 | 1 | menu ⋯ |
| **Total (valeurs médianes)** | **≈ 75** | **≈ 59 (−21 %)** | **≈ 26 (−65 %)** | **≈ 22 sur 13 actions** |

**Ce que la navigation seule apporte.**
- Elle fait gagner au plus 1 geste sur environ un tiers des actions, et retire la plupart des défilements : une seule vue Structure, une console de mixage, ⤴ dans la barre du haut.
- Les gros gains, 5 gestes ou plus, viennent tous des intentions placées dans le contexte (Fill, Montée, Kit, Accords, Variation), pas des onglets.
- Il faut faire les deux : la navigation donne un point central, Structure, où les feuilles de partie et de piste ont leur place.

**Points de vigilance sur les 5 onglets.**
- **Le risque de « Sons »** : fusionner Rythme, Acide, Synthé et Audio en une liste peut ajouter un geste (Sons › piste › éditeur), alors qu'aller sur Rythme en coûte un seul aujourd'hui. Il faut que Sons rouvre directement la dernière piste éditée et garde une rangée de puces de pistes en haut, comme `TrackBar` aujourd'hui. Avec le double-toucher sur les clips de Structure, on garde 1 geste.
- **La largeur de la barre du haut** : il y a la place sur 360 dp pour [Projet ▾] [Section ▾] ↶ ● ▶ ⤴. Le calcul : 4 boutons de 48 dp, plus 92 dp pour le projet, plus 52 dp pour la lettre de section, plus 12 dp de marge, soit ≈ 348 dp. Il faut ajouter le 4e bouton à `ALWAYS_SHOWN` (`TopBar.kt:151`). Le tempo et la position passent dans l'en-tête de Structure. L'aide « ? » va dans Plus.
- **Live** : en faire un onglet ne change pas le compte (1 geste), mais cela le sort du chemin de création, comme le demandent Pro 3 et l'amateur 1.

**Le Mode simple.** Le candidat de l'amateur 4 est Clavier · Rythme · Mon morceau · Envoyer.
- Il atteint les mêmes comptes que la cible pour un débutant, à deux conditions : « Mon morceau » est l'onglet d'arrivée, et il a les feuilles de partie et de piste.
- Il cache sans perte ce qui coûte cher aujourd'hui : l'automation et la console restent accessibles, mais les intentions « Fondu », « Montée », « Fill », « Break » et « Fin » donnent déjà le résultat sans passer par ces outils.
- Ajustements recommandés : mettre « Mon morceau » en premier, et ne pas doubler ⤴ dans la barre du haut quand l'onglet Envoyer existe. Cet onglet devient la page « Fini ? » : durée, vérifications, Partager, WAV.

## Les 15 plus gros gains

Le score est le nombre de gestes gagnés multiplié par la fréquence. La fréquence est estimée pour un morceau de 3 minutes, avec 6 à 8 parties et 8 pistes ; ce sont des hypothèses à confirmer par les tests de Benjamin.

| # | Action | Aujourd'hui → cible | Gestes gagnés | Fréquence par morceau | Score | Changement |
|---|---|---|---|---|---|---|
| 1 | Fill en fin de partie | 16 → 2 | 14 | 3 | 42 | « Fill » dans la feuille de partie, sur la dernière mesure seulement |
| 2 | Peindre des pas (charleys, motifs) | ≈ 10 → 1 | 9 | 4 | 36 | Glisser le doigt pour peindre ou effacer |
| 3 | Accords de tout le morceau | ≈ 30 → 3 | 27 | 1 | 27 | « Accords du morceau » appliqués à toutes les sections et pistes, tonalité suivie |
| 4 | Roulement | 6 → 2 | 4 | 6 | 24 | Appui long sur un coup : ×2 à ×8, montant |
| 5 | Changer le kit | 9 → 2 (80 → 2 pour qui le fait pad par pad) | 7 | 3 essais | 21 | Puce « Kit ▾ » en tête de la batterie |
| 6 | Variation d'une partie | 9 → 2 | 7 | 3 | 21 | « Variation » dans la feuille de partie, avec l'avertissement « jouée 3 fois » |
| 7 | Son d'un pad | 5,5 → 2 | 3,5 | 6 | 21 | Appui long sur le pad : feuille Son, Volume, Pan |
| 8 | Éditer la bonne partie | 3 → 1 | 2 | 10 | 20 | Double-toucher le clip dans Structure ouvre son éditeur |
| 9 | Montée de filtre avant le drop | 11 → 2 | 9 | 2 | 18 | « Montée avant » dans la feuille de partie, noms des parties sur les courbes |
| 10 | Rééquilibrer le mix | ≈ 10 → 1 | 9 | 2 | 18 | Mix › « Équilibrer », avec Annuler |
| 11 | Couper ou écouter seule une piste | 3 → 1 | 2 | 8 | 16 | M et S sur les en-têtes de piste de Structure, bandeau « Solo actif » |
| 12 | Écouter tout le morceau | 2 → 1 | 1 | 15 | 15 | ▶ joue le morceau partout, puce « ⟳ Section » pour boucler |
| 13 | Fondu d'entrée (tout le mix) | ≈ 30 → 2 | 28 | 0,5 | 14 | « Fondu d'entrée » et « Fondu de sortie » dans la feuille de partie |
| 14 | Enregistrer une mélodie puis la remettre en rythme | ≈ 7,5 → 3 | 4,5 | 3 | 13,5 | « ● Enregistrer » à côté du clavier (décompte et métronome), puis la carte « Garder · Remettre en rythme · Refaire » |
| 15 | Volume d'un pad | 6 → 2 | 4 | 3 | 12 | Feuille d'appui long sur le pad, pads dans la console |

Juste derrière (score de 10 environ) :
- déplacer une partie en la glissant ;
- couper une piste dans une partie d'un toucher ;
- volume d'une piste dans la console ;
- feuille « Changer le son » d'une piste ;
- copier-coller de notes entre pistes et sections.

Hors classement, car le gain en gestes est faible, mais décisifs pour finir un morceau :
- **⤴ Partager dans la barre du haut** (4 → 2). Aucun testeur débutant ne trouvait ce bouton dans le Mixeur.
- **« Fin » et « Break » en 2 gestes**. Aujourd'hui, il faut 6 à 15 gestes, et aucun chemin n'est indiqué.
- **Écran « Ton morceau est prêt »** après l'assistant.

## Principes de design

1. **Une action fréquente, faite 5 fois ou plus par morceau, se fait en 2 gestes au plus depuis l'écran où l'on est, et en 3 au plus depuis n'importe où.** Une action rare se fait en 4 gestes au plus. Jamais un défilement, un menu et un repli à la suite.
2. **Toucher l'objet ouvre ses actions.** Une partie, une piste, un pad, un pas ou une note ouvre sa feuille ou son menu là où on le regarde, pas dans un onglet lointain. L'appui long veut dire la même chose partout : « options de cet objet ». Une bulle l'explique la première fois.
3. **Une intention, un bouton.** Break, Fill, Fin, Montée, Fondu, Kit, Accords du morceau, Équilibrer. Les outils techniques (automation, scènes, inserts) restent dessous pour les pros, et ce que les boutons écrivent reste visible et modifiable dans ces outils.
4. **La portée par défaut est la plus large utile, et elle est dite.** Un son, un kit, des accords, un tempo valent pour tout le morceau. « Seulement cette partie » est une option visible. Modifier une section jouée plusieurs fois affiche un avertissement : « jouée 3 fois : toutes / celle-ci ».
5. **▶ joue ce qu'on attend.** C'est le morceau, à partir de la position. Boucler une section est un choix visible (« ⟳ Section »), et le bouton affiche ce qu'il joue.
6. **Glisser plutôt que toucher N fois.** Pour peindre des pas, déplacer une partie, étirer une longueur, régler un volume ou dessiner une rampe.
7. **Zéro défilement pour l'action principale d'un écran.** La grille de pas, les parties, les faders et le clavier sont visibles à l'arrivée. Les réglages rares vont dans une feuille « ⚙ » : mesure 2/4 à 7/4, suivi, fin du morceau.
8. **Un seul endroit pour éditer une même chose.** Structure est la seule vue d'arrangement. La liste des parties devient un mode d'affichage de cette vue, pas un deuxième éditeur.
9. **Après chaque création ou prise, le choix suivant en 1 geste.** Garder, Remettre en rythme, Refaire, Écouter. Le bon outil vient à l'utilisateur au lieu qu'il le cherche.
10. **Les actions de fin sont toujours visibles.** ⤴ Partager ou Exporter se trouve dans la barre du haut, avec une vérification : solo oublié, saturation, morceau vide, durée.
11. **Pas de mot qu'on ne peut pas toucher.** Un bouton dit ce qu'il fait (« Remettre en rythme », pas « Quantifier 100 % »). Un bouton fait d'une seule icône porte un libellé quand la place le permet.
12. **Un budget de gestes vérifié par la CI.** Pour les 15 actions du classement, un test Compose compte les touches par leurs `testTag` et échoue si un chemin s'allonge. Le budget de chaque action est la colonne « Cible ».

## Sources des références

- FL Studio Mobile, manuel en ligne, éditeurs (pas, note repeat jusqu'à 8, gestes du piano roll, copier-coller) : [FL Studio Mobile, Editors](https://www.image-line.com/support/flstudio_mobile_online_manual/html/plugins/FL%20Studio%20Mobile_Editors.htm)
- FL Studio Mobile, manuel en ligne, playlist (copier puis coller par appui long, redimensionner un clip en tirant son bord, double-toucher pour ouvrir l'éditeur, automation par CTRL) : [FL Studio Mobile, Playlist](https://www.image-line.com/support/flstudio_mobile_online_manual/html/plugins/FL%20Studio%20Mobile_Playlist.htm)
- GarageBand pour iPhone, quantification dans les commandes de piste : [Apple, GarageBand iPhone](https://support.apple.com/guide/garageband-iphone/chs175dc411/ios)
- Les autres chiffres marqués « ≈ » (GarageBand, BandLab, Ableton, Logic) sont des ordres de grandeur d'usage courant, non revérifiés sur l'appareil.
