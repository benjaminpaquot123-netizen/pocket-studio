# Testeur Amateur 4 (60 ans, tablette, 130 %, pianiste, veut un morceau Chill avec sa mélodie)

Chemins : app/src/main/java/com/pocketstudio/app/ (abrégé ici en `app/`). Lecture seule, rien lancé.

## Parcours réel

1. **Accueil** (onboarding/Welcome.kt:93-120, capture scale130/fr_welcome.png). Bon départ : trois grosses cartes, « Créer mon premier morceau (guidé) » en orange.
   - À 130 % le titre de la carte fait 3 lignes, et « Partir de zéro » est coupé en bas : il faut défiler pour voir qu'il y a un 3e choix.
   - Les boutons de langue (Français / English / Nederlands) passent sur 2 lignes, c'est du bruit pour elle.
   - Le dernier texte, « Le guide des premiers pas reste dans Outils › Aide. », est petit (bodySmall) et gris : elle ne le verra pas. Et « Outils › Aide » ne lui dit rien.
2. **Assistant** « Nouveau morceau, pas à pas » (library/WizardScreen.kt:158). « Choisis un style » : elle cherche « Chill » ou « Lo-fi ». Les styles sont rangés par familles Techno / House / Bass music / Hip-hop / Autres (retour du 22/10) et presque tous sont de la musique électronique. Le Lo-fi est caché dans « Hip-hop » (« Lo-fi hip-hop »), le mot « Chill » n'existe nulle part. Elle ne sait pas dans quelle famille chercher.
   - Phrase d'aide longue : « Touche une carte pour lire sa description, ▶ pour l'écouter. »
   - Les étapes suivantes parlent de « ligne acide (TB-303) », « Coupure », « Résonance », « Densité » (WizardScreen.kt:500-610). Pour un Lo-fi c'est du charabia ; « La ligne qui couine et qui gronde » ne l'aide pas. Elle peut cliquer « Créer tout de suite (choix conseillés) » (l.200), c'est le seul bon réflexe, mais le bouton est long et pas mis en avant.
3. **Après la création** : arrivée sur l'écran Morceau (bien). Barre du bas : sept onglets Rythme / Acide / Synthé / Mixeur / Audio / Morceau / Outils (ui/AppRoot.kt:173-190 et 898-925). « Acide », « Synthé », « Mixeur » : aucun sens pour elle. L'onglet piano est « Synthé » (icône piano, c'est le seul indice).
   - Sur l'écran Morceau (capture scale130/fr_session.png) : bouton orange énorme « Live : jouer les sections piste par piste / Pads de toutes les pistes, effets et 303 sous les doigts » (ui/TopBar.kt:245). Du jargon à tous les mots. Puis « LE MORCEAU », les parties « B · Co… » et « C · Ref… » (coupées), des « − + ⋮ » sur chaque ligne, « Ajouter à la fin : + J », « Réglages du morceau / suivre le morceau / rester sur ma scène / Temps par mesure ». Ça fait 5 blocs de commandes avant de savoir quel bouton lance la lecture (« Jouer le morceau » est en bas, session/SessionScreen.kt:361, sous les parties).
   - Le « Jouer » du haut (rond orange) existe aussi : deux boutons Play différents (un qui joue la scène, un qui joue le morceau). Elle ne sait pas lequel entend « tout du début à la fin ».
4. **Jouer sa mélodie** : onglet Synthé, bloc « CLAVIER » (synth/SynthScreen.kt:367, scale130/fr_synth.png). Le clavier est joli et assez grand.
   - Au-dessus : puces « Accords », « Pads », « 2 octaves », « 1 octave », « Gamme », et « Octave 3 − + ». Sept réglages avant la première touche. « Pads » et « Gamme » sont incompréhensibles pour elle.
   - Pas de bouton « Enregistrer » à côté du clavier. L'enregistrement est le petit rond rouge du bandeau du haut : 30 dp, avec un point de 14 dp, sans mot (ui/TopBar.kt:228-250). Le texte d'aide « Enregistrer ce que je joue » n'est qu'une description pour lecteur d'écran, jamais lu à l'écran. À 130 % le bandeau tronque même le nom du projet : « Da F… » (scale130/fr_top_bar.png).
   - Quand on enregistre, l'application joue en boucle sur 1 mesure (le réglage « Boucle 1 mes. » est choisi par défaut dans le piano roll, scale tablet/piano_roll.png). Elle joue une phrase de 8 mesures et le message qui apparaît dit « La boucle passe à 4 mesures pour ta prise… ↶ en haut pour revenir à 1 mesure » (ui/AppRoot.kt:886-887). Une personne de 60 ans ne comprend pas « boucle », « prise », ni « ↶ ». Et le décompte et le métronome sont cachés dans l'onglet Rythme (rhythm/RhythmScreen.kt:669-670 : puces « Métronome » et « Décompte »). Sans décompte, elle appuie sur ● et doit jouer tout de suite : elle rate le début.
   - Elle joue donc ses notes un peu « à la pianiste » (pas pile sur le temps). Pour corriger : bascule « Clavier / Notes » (SynthScreen.kt:471-473) puis dans le piano roll : « Quantifier 50 % » et « 100 % » (synth/PianoRoll.kt:197-198). « Quantifier » et un « 100 % » tout seul : aucun sens. Il y a aussi « Grille 1/8 · 1/16 · 1/16 t · 1/32 » (l.~170), « Note 1/16 1/8 1/4 1/2 », « Legato », « Humaniser », « Force » : c'est un tableau de bord de studio.
5. **Écouter du début à la fin** : onglet Morceau, « Jouer le morceau » (SessionScreen.kt:361). Si sa mélodie est dans une seule scène, il faut penser à dupliquer la scène, l'ajouter à la liste « Ajouter à la fin : + A · Intro », etc. Le mot « scène » est expliqué sur place (l.572 : « Une scène = la batterie, l'acide et le piano roll ») mais en 20 mots de jargon. Sa mélodie dans « le morceau » n'est pas automatique.
6. **Savoir qu'elle a fini** : carte « PROCHAINE ÉTAPE · 2/8 » (onboarding/NextStepCard.kt, scale130/fr_next_steps.png) : huit étapes (Rythme, Basse, Mélodie, Sections, Morceau, Voix, Volumes, Partager). Elle pense : « il faut faire huit choses ? ». La « Basse » dit « génère une ligne dans l'onglet Acide » : mauvais pour du Lo-fi. Les étapes « Voix » et « Volumes » se cochent à la main (« C'est fait »). Il n'y a jamais de message « Ton morceau est terminé, bravo ». La carte se replie en bandeau et est facile à fermer par erreur (croix de 18 dp, NextStepCard.kt:147).
7. **Envoyer à ses petits-enfants** : le bouton vert « Partager mon morceau » est en haut de l'onglet **Mixeur** (mixer/MixerScreen.kt:191 et 420-441, scale130/fr_mixer.png). Bonne idée (M4A, « prêt à envoyer »), mais personne ne cherche « partager » dans « Mixeur ». Le bouton s'affiche sur 2 lignes à 130 %. À côté « Autres exports » avec une flèche. Plus bas, « WAV · qualité max », « FLAC », « Sans limiteur (pour le mastering) » : à ne pas voir. Dialogue « Nomme ton morceau » puis « Je prépare ton morceau… » puis la feuille de partage Android : ça, c'est bien. Seule la carte « Prochaine étape › Partager mon morceau » (NextStepCard.kt:160) l'y amène, à la toute dernière étape.
   - Pas d'« Enregistrer le projet » à voir : « Sauvegardé » (TopBar.kt:206) est une petite écriture grise de 11 sp avec une coche verte. Elle ne saura pas si son travail est gardé.

Elle abandonne probablement vers l'étape 4 (clavier / enregistrement) ou l'étape 5 (assembler le morceau).

## Points durs

**1. Bloquant : l'enregistrement au clavier est un petit rond rouge sans mot**
- Écran : bandeau du haut, toutes pages. ui/TopBar.kt:228-250 (rond 30 dp, icône 14 dp).
- Pourquoi : à 60 ans, sur tablette, le rond rouge avec contour gris ressemble à un voyant, pas à un bouton. Aucune bulle, aucun texte « Enregistrer ». Dans l'onglet Synthé, aucun bouton d'enregistrement à côté du clavier.
- Fix : ajouter, dans le bloc « CLAVIER » du Synthé, un gros bouton rouge « ● Enregistrer ma mélodie » (≥ 56 dp, texte visible) qui lance décompte + enregistrement, et devient « ■ Terminer ». Mettre le même mot dans le bandeau haut quand il y a la place.

**2. Bloquant : décompte et métronome introuvables, boucle de 1 mesure par défaut**
- Écran : Rythme › carte « Groove » (rhythm/RhythmScreen.kt:669-670), piano roll « Boucle » (synth/PianoRoll.kt, tablet/piano_roll.png), message ui/AppRoot.kt:886-887.
- Pourquoi : une pianiste a besoin de 4 temps de décompte et d'un tic-tac, et d'une phrase de plusieurs mesures. Ici tout est caché ou technique (« boucle passe à 4 mesures pour ta prise »).
- Fix : au lancement de ● sur le Synthé, décompte de 4 temps automatique (affiché en grand « 4 · 3 · 2 · 1 »), métronome audible par défaut pendant la prise, longueur de la prise = nombre de mesures du morceau/de la section, et un seul texte clair : « Joue ta mélodie, j'écoute. Appuie sur Terminer quand tu as fini. »

**3. Pénible : après la prise, rien ne dit « voilà ta mélodie, tu la gardes ? » et la correction du rythme est cachée**
- Écran : Synthé › « Notes » › piano roll (PianoRoll.kt:197-198, 273). Boutons « Quantifier 50 % » / « 100 % » / « Legato » / « Humaniser ».
- Pourquoi : « quantifier » est du jargon, « 100 % » seul n'a pas de sens, et elle doit d'abord trouver l'onglet « Notes ».
- Fix : à la fin de la prise, fenêtre : « Ta mélodie est enregistrée. [Écouter] [Remettre les notes en rythme] [Refaire] [Garder] ». « Remettre en rythme » = quantifier 1/16 à 75 % ; garder « Quantifier » et « Humaniser » dans « Avancé ». Renommer en « Remettre les notes en rythme » (doux / complet).

**4. Bloquant : le partage est dans le Mixeur**
- Écran : Mixeur (mixer/MixerScreen.kt:191, 420-441).
- Pourquoi : elle cherche « Envoyer », « Partager » ou une icône de partage dans le bandeau du haut. Le bouton « Partager mon morceau » n'est qu'en haut de l'onglet Mixeur, sur deux lignes à 130 %, accompagné d'« Autres exports » (WAV, FLAC, MIDI, limiteur…).
- Fix : mettre une icône Partager (carré avec flèche) dans le bandeau du haut et sur l'écran Morceau (« Envoyer mon morceau »). Dans le Mixeur garder le bouton, mais replier tout le reste dans « Pour les experts ». Masquer WAV / FLAC / « Sans limiteur (pour le mastering) » (MixerScreen.kt:564-676) par défaut.

**5. Pénible : trop de choix sur l'écran Morceau et deux boutons Lecture**
- Écran : Morceau (session/SessionScreen.kt:330-395 ; ui/TopBar.kt:245-275 ; capture scale130/fr_session.png).
- Pourquoi : « Live : jouer les sections piste par piste / Pads… 303 sous les doigts », « Ligne du temps », « LE MORCEAU », « Scènes », « Écouter : cette section / tout le morceau », « À la fin : s'arrêter / recommencer », « suivre le morceau / rester sur ma scène », « Temps par mesure ». Le bouton qui joue tout le morceau est tout en bas de la liste.
- Fix : en haut un seul grand bouton « ▶ Écouter mon morceau en entier ». Replier « Live » et « Ligne du temps » sous « Pour aller plus loin ». Remplacer « Réglages du morceau » par « Options » replié. Texte de l'écran Morceau limité à une phrase.

**6. Pénible : jargon et mots anglais partout**
- Acide, TB-303, 303, Synthé, Lead, Pluck, Pads, Pad, Nappe (Synthé, SynthScreen.kt:420 « Sons prêts à jouer » : Basse / Lead / Nappe / Pluck), Mixeur, Panoramique, dB, « Solo », « Muet », « BPM », « Taper » (tempo), « Quantifier », « Legato », « Humaniser », « Automatise », « Ce qui joue », « Live », « Fondu », « M4A », « WAV », « FLAC », « limiteur », « mastering », « Unisson (supersaw) », « Ladder » (SynthScreen.kt:126, seul mot non traduit dans un choix), « Forme d'onde : Scie / Carré / Triangle ».
- Pourquoi : lue par une personne qui lit tout, chaque mot inconnu fait douter.
- Fix : un « Mode simple » par défaut (à proposer à l'accueil : « Je débute ») qui renomme : Synthé → « Clavier », Acide → « Basse acide » (masqué si style calme), Mixeur → « Volumes et envoi », Morceau → « Mon morceau », Outils → « Réglages et aide », Lead → « Mélodie », Nappe → « Ambiance », Pluck → « Notes courtes », « Taper » → « Frapper le tempo ». Les « ? » d'aide (Glossary.kt) existent déjà : les rendre plus gros (aujourd'hui ≈ 18 dp).

**7. Pénible : barre du bas à sept onglets et sans consigne d'ordre**
- Écran : barre du bas (ui/AppRoot.kt:898-925 ; scale130/fr_top_bar.png pour le haut).
- Pourquoi : sept icônes dont l'ordre (Rythme, Acide, Synthé, Mixeur, Audio, Morceau, Outils) ne suit pas ce qu'elle fait : jouer → monter le morceau → envoyer. À 130 % le code réduit les étiquettes (« tight », l.898-911) donc encore plus petites, justement pour elle. L'icône d'« Acide » (barres de son) et de « Synthé » (piano) ne se distinguent pas du Mixeur.
- Fix : en mode simple, 4 onglets : « Clavier » · « Rythme » · « Mon morceau » · « Envoyer ». Garder les autres sous « Plus ». Garder une taille d'étiquette ≥ 12 sp même quand « tight » (passer à 2 lignes ou à un rail latéral sur tablette).

**8. Pénible : « Chill / Lo-fi » introuvable et assistant tourné vers l'électro**
- Écran : Assistant, étape « Choisis un style » (library/WizardScreen.kt:241-304), StyleFamilies.kt ; project-model StyleRecipes.kt.
- Pourquoi : pas de famille « Calme / Détente ». « Lo-fi hip-hop » est rangé dans « Autres » ou « Hip-hop ». Ensuite des étapes d'acide (WizardScreen.kt:500-621) sans rapport.
- Fix : ajouter une famille « Calme (Chill, Lo-fi, Piano) » en tête avec description « doux, lent, pour se détendre », et une étape « Joue ta mélodie » à la fin de l'assistant (aller sur le clavier avec le décompte). Cacher l'étape 303 quand le style n'est pas acide.

**9. Pénible : « Prochaine étape » : 8 étapes, pas de « Terminé »**
- Écran : carte flottante en haut (onboarding/NextStepCard.kt, NextSteps.kt:70-95).
- Pourquoi : huit marches, « Basse » (acide), « Voix » et « Volumes » à cocher à la main avec « C'est fait ». Elle ne sait pas si elle peut s'arrêter. Pas de message final.
- Fix : réduire à 4 étapes visibles : 1. Choisir un style, 2. Jouer ma mélodie, 3. Écouter en entier, 4. Envoyer. Quand les 4 sont faites, grande carte « Ton morceau est prêt. [Envoyer à quelqu'un] ». « Voix », « Volumes » et « Basse » passent en « Pour aller plus loin ».

**10. Pénible : textes petits ou coupés à 130 % et sur tablette**
- Écrans : bandeau haut (« Da F… », scale130/fr_top_bar.png) ; parties du morceau (« B · Co… », « C · Ref… », fr_session.png) ; pistes du Mixeur (« B… », « Bass… », fr_mixer.png) ; carte d'accueil (« Partir de zéro » coupé, fr_welcome.png).
- Pourquoi : les noms que le morceau lui-même a donnés sont tronqués : « Couplet » devient « Co… » ; « Basse acide » devient « B… ». Elle ne peut pas les relire. La mention « Sauvegardé » fait 11 sp (TopBar.kt:206). Les textes d'aide grises (bodySmall, Neon.TextSecondary) sont de faible contraste.
- Fix : noms sur 2 lignes (maxLines = 2) au lieu de « … » ; sous 130 % passer le bandeau du haut sur deux rangées (nom du projet seul sur la 1re) ; aider en 14 sp minimum avec un gris plus clair ; vérifier les captures scale130 avant livraison en y mettant « Couplet », « Refrain ».

**11. Détail : icônes sans texte**
- Écran : bandeau haut (dossier orange = « Mes projets », flèche courbe = Annuler, rond rouge = enregistrer, rond orange = jouer) ; ⋮ de chaque partie (SessionScreen.kt:527) ; croix de fermeture de la carte (NextStepCard.kt:147).
- Pourquoi : le dossier orange n'est pas assez clair pour « mes morceaux ». « ↶ » cité dans un texte (AppRoot.kt:887) n'est pas le même dessin que l'icône.
- Fix : étiquettes de 2-3 mots sous ou à côté des quatre icônes principales du bandeau (« Projets », « Enregistrer », « Jouer ») quand l'écran fait plus de 600 dp ou à 130 %.

**12. Détail : sauvegarde et nom du morceau**
- Écran : bandeau haut, Mes projets. Le nom par défaut est un nom de style (« Da F… » / « Mon projet 1 »). La première demande de nom n'arrive qu'à l'envoi (ShareSongUi.kt:97 « Nomme ton morceau »).
- Fix : demander le nom à la fin de l'assistant (« Comment s'appelle ton morceau ? ») et afficher « Ton travail est gardé automatiquement » une fois.

## Ce qui manque

- Un bouton « Enregistrer ma mélodie » clair, avec décompte et métronome automatiques, sur l'écran du clavier.
- Un bouton « Écouter » qui joue vraiment tout le morceau depuis le début, bien visible sur chaque écran (aujourd'hui « Jouer » du haut ≠ « Jouer le morceau » de Morceau).
- Un écran de fin : « Ton morceau est prêt » avec « Envoyer à quelqu'un » en gros, sans passer par le Mixeur.
- Un « Mode simple / Je débute » qui cache Acide, 303, Mixeur avancé, WAV/FLAC, Live, et change les mots.
- Un style « Chill / Lo-fi / Piano calme » facile à trouver, et un morceau Chill qui s'arrête de parler de 303.
- La possibilité de jouer sa mélodie directement dans une section (« Couplet ») et de la voir apparaître dans le morceau, sans devoir comprendre scène / duplication / ajout à la liste.
- Une confirmation visible de sauvegarde (« Enregistré à 14 h 32 ») et un moyen de retrouver le morceau envoyé.
- Une « visite » parlée ou en images de 30 secondes (la phrase « Le guide des premiers pas reste dans Outils › Aide » est trop discrète).
- Des textes plus grands par défaut sur tablette (aujourd'hui le réglage 130 % est à trouver soi-même dans Outils › Réglages › Affichage, et les captures montrent encore des coupures).

## Top 5 des priorités

1. Gros bouton « ● Enregistrer ma mélodie » sur le clavier, avec décompte de 4 temps et métronome automatiques, et une prise sur plusieurs mesures (TopBar.kt:228, AppRoot.kt:886, RhythmScreen.kt:669).
2. Après la prise : « Écouter / Remettre en rythme / Refaire / Garder » en français simple, à la place de « Quantifier 50 % / 100 % » cachés dans l'onglet Notes (PianoRoll.kt:197).
3. « Envoyer mon morceau » visible partout (icône dans le bandeau + fin de l'assistant + écran Morceau), plus le message « Ton morceau est prêt », au lieu d'être enterré dans le Mixeur (MixerScreen.kt:191).
4. Un « Mode simple » : 4 onglets (Clavier, Rythme, Mon morceau, Envoyer), mots français (Mélodie, Ambiance, pas de Lead / Pad / 303 / Quantifier / Mixeur), famille de styles « Calme (Chill, Lo-fi) ».
5. Alléger l'écran Morceau (un seul grand « ▶ Écouter mon morceau en entier », le reste replié) et corriger les textes tronqués à 130 % (« Co… », « B… », « Da F… ») (SessionScreen.kt:330-395, ui/TopBar.kt, fr_session.png).
