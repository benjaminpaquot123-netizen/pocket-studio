# Testeur Amateur 1 (débutant total, téléphone) : créer un morceau de 3 min et le partager

Chemins : app/src/main/java/com/pocketstudio/app/ (abrégé `…/`). Lecture du code et des captures uniquement.

## Parcours réel

1. **Accueil** (welcome.png). « Créer mon premier morceau (guidé) » est en gros, en orange. Bien. Je touche.
2. **Assistant, étape 1 « Choisis un style »** (library/WizardScreen.kt:~262). Des familles repliées (Techno, House, Bass music, Hip-hop, Autres) et 30+ styles : « Acid techno », « Acidcore », « Neurofunk », « Riddim », « Speed garage »… Je veux « de la techno » : je dois ouvrir une famille, toucher une carte pour lire, ▶ pour écouter, puis « Choisir ce style ». Je ne sais pas ce qui sépare « Techno », « Techno de Detroit » et « Hard techno ». Les références (« Pour l'idée : … ») sont des noms d'artistes qui ne me disent rien. Je tiens bon.
3. **Barre d'étapes** : « Étape 2 sur 13 » (wizard_summary.png : « Étape 13 sur 13 »). 13 étapes, c'est long et ennuyeux. Il y a un petit bouton « Créer tout de suite (choix conseillés) » (WizardScreen.kt:200) : c'est ce que je prends dès l'étape 3 parce que les écrans suivants parlent de choses que je ne comprends pas.
4. Si je continue : « Ambiance » (gamme, accords, « verrouille le clavier sur cette gamme »), puis une étape par ligne de batterie (« Utiliser cette ligne », « Choisis le son », « Avec le rythme »), puis « Ligne acide (TB-303) » (wizard_acid.png) : « La ligne qui couine et qui gronde, jouée par une TB-303 ». Une grille « La La La – Sol… » avec « Appui long : accent (couleur pleine), puis slide ⌒, puis les deux ». « Densité 80 % », « Coupure », « Résonance ». Là je décroche : je ne sais pas si je dois y toucher, ni ce qui se passe si j'y touche. La page est trop longue pour un téléphone et le bas est caché par « Créer tout de suite » + « Retour / Suivant ».
5. **Résumé** (wizard_summary.png) : « QUATRE SECTIONS … Intro / Groove / Drop / Break », « Le morceau : Intro → Groove → Drop → Break → Drop → Intro pour finir : environ 2 min 50 ». Bon. Mais « Kick (grosse caisse) », « Charley ouvert », « Montée de souffle » : du jargon sans explication. « Créer le morceau ».
6. **Arrivée** sur l'onglet Morceau (bien, AppRoot.kt:489) avec un guide en 4-5 pages (SongGuide.kt:~50-110). Le texte contient : « Montée », « Drop », « Break », « Acide : ta ligne 303 », « Coupure », « arpégiateur », « Mixeur, Automation », « Impact », « Contre-chant ». Je clique « suivant » sans rien retenir.
7. **Écran Morceau** (session.png) : en haut un gros bouton orange « Live : jouer les sections piste par piste » (je ne sais pas ce que c'est), une carte « Ligne du temps », puis « LE MORCEAU » avec A·Intro, B·Couplet… ×2, − +, ⋮, « Ajouter à la fin : + A + B + C + D + J », puis « RÉGLAGES DU MORCEAU » (« À la fin : s'arrêter / recommencer », « Pendant la lecture : suivre le morceau / rester sur ma scène », « Temps par mesure 2/4 … 7/4 »), puis « SCÈNES » : une grille de 8, 16 ou 24 carrés A à X, presque tous « vide ». Tout cela est sur la même page, très long. Je lance « Jouer le morceau » : ça marche, ça se suit bien (chrono 0:02 / 0:27, mes. 2.3 / 14).
8. **Je veux changer des sons.** Où ? Aucun écran ne me dit « pour changer un son, va là ». Je tente l'onglet « Synthé » (la barre du bas a 7 onglets : Rythme, Acide, Synthé, Mixeur, Audio, Morceau, Outils : des mots de musicien). Je vois un clavier géant (synth.png). Le choix des sons est en dessous : « SONS PRÊTS À JOUER : Basse / Lead / Nappe / Pluck » (SynthScreen.kt:420), puis « RÉGLAGES DU SON : Forme d'onde Scie/Carré/Triangle, Coupure, Résonance, Enveloppe du filtre ». Je ne sais pas laquelle des pistes (Basse, Cordes, Lead, Synthé) je modifie : il faut regarder la petite barre de pistes (synth/TrackBar.kt) qui est un détail.
9. **Je veux changer la mélodie.** Synthé › puce « Notes » (SynthScreen.kt:473) → piano roll (piano_roll.png) : des barres orange sur des lignes, « Do3, Do4 », « Quantifier 50 % », « Basse en fantôme », « Boucle 1 mes. 2 mes. 4 mes. », « Force ». « Touche une case : ajoute une note. Touche une note : l'enlève. Glisse : déplace. Glisse le bout : allonge. Couloir du bas : force. » C'est écrit, c'est faisable, mais je dois comprendre la fenêtre de 1 mesure, ne pas casser l'accord, et surtout je modifie UNE scène : si je change la mélodie du Drop et pas celle de la Montée, je ne vois pas ce qui a changé dans le morceau final. Pas de bouton « jouer ma mélodie en boucle avec le reste » visible depuis le piano roll (le ▶ est dans le haut de l'écran Rythme, et joue « cette section » : voir point 1 ci-dessous).
10. **Je veux que le drop tape plus fort.** Il n'existe aucun bouton « plus fort / plus intense ». Chemins possibles : (a) Ligne du temps : toucher un clip pour couper/remettre une piste dans une partie (ça marche mais je dois l'inventer) ; (b) Mixeur : le volume est général à la piste, pas à la partie ; (c) Mixeur › « Automation et effets » › « Automatiser » : « Boucle / Par scène / Morceau », « Dessiner / Ligne / Zone / Lisser », « Volume · Batterie » (AutomationCard.kt:152-212). Pour un débutant c'est un mur. J'abandonne.
11. **Je veux une fin.** Le morceau généré finit par « Intro pour finir ». Pour une vraie fin : onglet Morceau › dupliquer une scène, la nommer « Fin » (RenameSceneDialog, SessionScreen.kt:~815, la liste contient bien « Fin »), la vider ou l'alléger (« Vider »), « + » dans « Ajouter à la fin ». Rien ne dit que c'est la voie. Le seul « vrai » fondu est à l'export (« Fondu de fin », MixerScreen.kt:604).
12. **Exporter et partager.** Mixeur : tout en haut un gros bouton vert « Partager mon morceau » (mixer.png) avec « Tout le morceau, avec un fondu à la fin, en M4A léger : prêt à envoyer. » Très bien. Dialogue « Nomme ton morceau » → « Je prépare ton morceau… » → choix de l'appli. Le meilleur écran de l'appli pour moi. Mais pour y aller il faut penser au « Mixeur » (une enceinte/égaliseur) : le bouton n'est ni dans la barre du haut ni sur l'écran Morceau.

## Points durs

### P1 · Le bouton ▶ ne joue pas ce que le guide dit (pénible, tourne en rond)
- Où : library/SongGuide.kt:79 (« ▶ Jouer fait entendre tout le morceau, de l'Intro à la fin ») contre rhythm/RhythmViewModel.kt:1544-1580 (`listenSong` = null → sur les onglets d'édition, ▶ joue « cette section » en boucle ; seul l'onglet Morceau joue tout) et rhythm_after_wizard.png (« Écouter : cette section »).
- Pourquoi : je termine l'assistant, on me dit « ▶ joue tout le morceau », je vais dans Rythme, j'appuie ▶, j'entends 2 mesures qui bouclent. Je crois que mon morceau fait 4 secondes ou que l'appli est cassée. Le petit sélecteur « Écouter : ⇅ cette section » est une carte à deux lignes que rien n'explique.
- Fix : après l'assistant, mettre `listenSong = true` par défaut dans le projet créé. Sur chaque écran d'édition afficher dans le bouton lui-même « ▶ Jouer le morceau » / « ▶ Jouer cette partie », avec un bandeau « Tu écoutes seulement la section C · Drop » quand on boucle une section.

### P2 · Aucun moyen simple de « rendre le drop plus fort » (bloquant pour mon objectif)
- Où : pas d'écran ; les outils sont éclatés entre mixer/AutomationCard.kt (:152-212, :386-417), session/TimelineScreen.kt (:1495-1560) et mixer/MixerScreen.kt (volume par piste, global, :539).
- Pourquoi : je pense en « plus de punch / plus de basse / plus d'énergie », l'appli me propose des « courbes d'automation » et des « modes Boucle/Par scène/Morceau ». Le volume d'une piste est le même dans toutes les parties : monter la basse dans le drop monte la basse partout.
- Fix : (1) sur la carte de chaque partie (Morceau) un curseur « Intensité » ou deux boutons « Plus fort / Moins fort » qui règlent le volume de cette partie (une courbe d'automation « Par scène » écrite en coulisses) ; (2) des boutons « Rendre ce drop plus costaud » qui ajoutent une piste (basse, impact), montent le kick, ouvrent le filtre ; (3) un tutoriel d'une page « Faire taper le drop » dans Outils › Aide.

### P3 · L'assistant : 13 étapes, trop de jargon, et on ne sait pas quoi ignorer (pénible)
- Où : library/WizardScreen.kt:160 (« Étape x sur 13 »), :200 (« Créer tout de suite »), acide ~:495-620 (« Coupure », « Résonance », « slide », « accent », « Densité ») ; textes de style (« Acid techno », « Acidcore » sans explication).
- Pourquoi : 13 écrans pour quelqu'un qui n'a jamais fait de musique ; chaque écran demande de choisir entre 8 « Kick » dont les noms ne disent rien. Le bon raccourci est discret (bouton-texte orange).
- Fix : proposer en premier un mode « Rapide » (style → ambiance → créer, 3 étapes, avec écoute) et un mode « Détaillé » ensuite. Remplacer « 303 / acide » par « Basse qui couine ». Mettre « Créer tout de suite » en vrai bouton plein dès l'étape 2. Cacher les grilles de notes derrière « Changer les notes (facultatif) ».

### P4 · L'écran Morceau est une longue page qui mélange tout (pénible)
- Où : session/SessionScreen.kt:245-275 (Live, Ligne du temps), :320-392 (Jouer en direct, Le morceau, Réglages), :563-607 (Scènes A à X).
- Pourquoi : le débutant voit en premier « Live : jouer les sections piste par piste », « Ligne du temps », « LE MORCEAU », « RÉGLAGES (suivre le morceau / rester sur ma scène / temps par mesure 7/4) », puis 16 ou 24 carrés de scènes vides. Les phrases d'aide (« Une scène = la batterie, l'acide et le piano roll » :572, « Touche une partie : elle joue juste après celle en cours » :334) se contredisent presque : « touche une scène pour la modifier » vs « touche une partie, elle joue ».
- Fix : l'écran Morceau = la liste des parties + un grand ▶ + « Ajouter une partie » (Intro, Couplet, Refrain, Drop, Fin, avec boutons). Les scènes vides, « temps par mesure », « Live » descendent dans « Plus d'options ». Remplacer le mot « scène » par « partie » partout (la grille « SCÈNES A–X » reste un détail interne).

### P5 · Pour modifier un son, on ne sait pas dans quel onglet aller (bloquant au début)
- Où : AppRoot.kt:173-186 (7 onglets Rythme, Acide, Synthé, Mixeur, Audio, Morceau, Outils) ; synth/SynthScreen.kt:314 (« Le son de la piste (le même pour toutes ses notes) »), :420 (« Sons prêts à jouer » : Basse/Lead/Nappe/Pluck) ; synth/TrackBar.kt.
- Pourquoi : « Synthé » ressemble à un piano ; mes sons de batterie sont dans « Rythme » (pads), la basse qui couine dans « Acide », la nappe/lead/cordes dans « Synthé ». Je ne vois pas la liste de mes « instruments » avec leur nom d'un coup. Le choix du son est sous un clavier de 2 octaves et plus bas que le fold (synth.png), et la piste modifiée n'est pas clairement indiquée. « Coupure / Résonance / Enveloppe du filtre / Épaisseur (désaccord) / Unisson (supersaw) » sont du vocabulaire de synthé.
- Fix : un écran « Mes sons » (ou, depuis Morceau/Ligne du temps, appui sur le nom d'une piste → « Changer le son » avec une liste de sons nommés, ▶ pour écouter, un bouton « Choisir »). Dans Synthé, mettre « Sons prêts à jouer » avant le clavier, afficher le nom de la piste modifiée en grand. Renommer les réglages : « Coupure » → « Brillant / sourd », « Résonance » → « Piquant ».

### P6 · Piano roll : pas de retour d'écoute sur le morceau, modification limitée à une scène (pénible)
- Où : synth/PianoRoll.kt (piano_roll.png : « Boucle 1 mes. », « Quantifier 50 % », « Basse en fantôme ») ; Synthé › « Notes » (SynthScreen.kt:473).
- Pourquoi : je modifie une mélodie dans la scène en cours (scène B, par exemple) ; la scène C peut avoir sa propre copie (ou la partager ?) : rien ne l'écrit (« Cette mélodie joue dans : Groove, Drop »). Les mots « quantifier », « force », « fantôme » ne me disent rien. Aucune aide pour écrire une bonne mélodie (juste « gamme verrouillée » : OK dans le clavier mais pas dans le piano roll ?).
- Fix : une ligne « Joue dans : Groove, Drop, Break (modifier toutes / seulement celle-ci) » au-dessus du piano roll. Un bouton « Une autre mélodie pour moi » (génération depuis la gamme et l'ambiance). Renommer « Quantifier » → « Remettre en rythme », « Force » → « Intensité de la note ».

### P7 · La « Fin » du morceau n'existe pas vraiment (pénible)
- Où : library/SongGuide.kt:27 (« Intro pour finir »), session/SessionScreen.kt:346-349 (Ajouter à la fin : + A + B…), Scenes.kt:43 (nom « Fin » existe), MixerScreen.kt:604 (fondu seulement à l'export).
- Pourquoi : le morceau se termine en rejouant l'Intro puis s'arrête net (« À la fin : s'arrêter »). Pas de bouton « Ajouter une fin ». « Ajouter à la fin : + A + B + C + D + J » : des lettres.
- Fix : un bouton « Ajouter une fin » qui duplique l'Intro, retire le kick/la basse, ajoute un impact, et pose un fondu ; garder « Fondu de fin » activé par défaut à l'export (c'est déjà le cas pour le M4A : dire « Fondu : 4 s » à côté du bouton Partager).

### P8 · « Partager mon morceau » n'est que dans le Mixeur (détail → pénible)
- Où : mixer/MixerScreen.kt:432 (le bouton), onboarding/NextSteps.kt (étape 8 « Partager » → onglet Mixeur).
- Pourquoi : la fin de mon parcours est cachée derrière un onglet « Mixeur » ; ni la barre du haut, ni l'écran Morceau n'ont de bouton. La carte « Prochaine étape · 6/8 Volumes » (rhythm_after_wizard.png) apparaît en haut de l'écran Rythme, sans lien avec ma volonté « ma fin de morceau ».
- Fix : un bouton « Partager » dans la barre du haut (icône ↗) et sur l'écran Morceau. Quand le morceau est prêt, la carte « Prochaine étape » dit « Ton morceau est prêt : écoute-le en entier, puis partage-le ».

### P9 · Vocabulaire non expliqué partout (pénible, récurrent)
- Où : SongGuide.kt (Montée, Drop, Break, 303, Coupure, arpégiateur, Automation, Impact, Contre-chant), WizardScreen.kt (« Kick (grosse caisse) » est bien : « Charley ouvert », « Percussion 2 », « Ride » non), NextSteps.kt, AutomationCard.kt (« Coupe graves », « Hachoir de voie », « Lancer réverbe »).
- Pourquoi : un mot de plus sur chaque ligne et le débutant ne comprend plus. Le glossaire (help/Glossary.kt) existe : les « ? » sont là, mais seulement sur quelques titres de cartes.
- Fix : les mots du guide (Montée, Drop, Break) deviennent des liens/étiquettes avec une mini explication au toucher. Une phrase de plus à la première apparition : « Drop = le moment où tout démarre fort ».

### P10 · L'écran Live et la ligne du temps sont trop riches pour moi (détail)
- Où : session/SessionScreen.kt:245 (bandeau orange), session/TimelineScreen.kt (timeline_screen.png).
- Pourquoi : la ligne du temps est jolie et lisible (clips colorés, tête de lecture) mais elle suppose que je sache ce qu'est une « partie », un clip, « Suivre », « Boucle 3–4 », M et S. Les actions utiles (couper la piste dans cette partie, changer de scène) sont dans l'appui long : invisible.
- Fix : un petit message au premier passage (« Touche un bloc pour couper/remettre une piste dans cette partie ») et un bouton « ? » ouvert une fois automatiquement.

## Ce qui manque pour finir un morceau complet
- Un chemin « changer un son » simple et nommé (liste de sons, ▶, « Choisir ») au lieu de potards de synthé.
- Un réglage « plus fort / plus doux » par partie, sans automation (le drop).
- Un bouton « Ajouter une fin » et une vraie transition (impact, fondu) visible.
- Une mélodie « proposée » à la demande dans le piano roll (« une autre mélodie »), avec une indication de quelle partie elle concerne.
- Du feedback global : « Ton morceau est complet : 2 min 50, 5 parties » + « Il te manque : une fin » (la carte « Prochaine étape » ne regarde que la fin d'un brouillon).
- Un bouton « Partager » hors du Mixeur.
- Un « Annuler » plus visible quand on a cassé quelque chose (il est dans la barre du haut : bien, mais le débutant ne sait pas qu'il annule aussi dans le piano roll).

## Top 5 des priorités
1. Corriger le ▶ : après l'assistant, il doit jouer tout le morceau partout (ou afficher clairement « cette section ») ; le guide dit le contraire de ce que fait Rythme (RhythmViewModel.kt:1544, SongGuide.kt:79).
2. Ajouter « Plus fort / plus costaud » par partie (Morceau, ou Ligne du temps) pour le drop, sans passer par l'automation.
3. Ajouter « Changer le son » en un geste (depuis le nom d'une piste, liste nommée avec ▶) et « Joue dans : … » dans le piano roll.
4. Simplifier l'assistant et l'écran Morceau : mode rapide en 3 étapes, scènes vides/réglages repliés, « partie » au lieu de « scène », mots du guide expliqués (Drop, Montée, 303, Coupure).
5. Ajouter « Ajouter une fin » et un bouton « Partager » visible hors du Mixeur (barre du haut + écran Morceau).
