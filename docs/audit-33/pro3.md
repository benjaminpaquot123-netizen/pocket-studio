# Testeur Pro 3 : architecture de l'information et parcours « morceau complet »
(Lecture du code + captures session.png, rhythm.png, mixer.png, top_bar.png, projects_new.png ; pas d'exécution.)

## Diagnostic en une phrase
Les 7 onglets sont classés par OUTIL (rythme, 303, synthé, mixeur, micro, morceau, divers), alors que l'utilisateur pense par ÉTAPE (je pose des sons, je construis des sections, j'arrange, je mixe, je partage). Résultat : « finir un morceau » oblige à faire des allers-retours sur 5 onglets et à comprendre 6 concepts (scène, partie, morceau, section, boucle, ligne du temps).

## Constats

1. [bloquant] Le « morceau » est éclaté sur 3 vues qui se recouvrent
   - Où : onglet Morceau, SessionScreen.kt:210-233 (LiveEntry, TimelineEntry, SongCard, ScenesCard, LiveSection empilées) ; Ligne du temps plein écran (TimelineScreen.kt) ; grille Scènes (SessionScreen.kt:563) ; liste « Le morceau » (SessionScreen.kt:330).
   - Pourquoi : la même chose (l'ordre des sections) s'édite à 3 endroits (liste des parties, mini-carte/ligne du temps, scènes). Sur la capture session.png, il faut ~2,5 écrans de défilement pour voir « Le morceau », ses réglages, puis les 16 scènes ; la ligne du temps (mini-carte) et les pads live sont encore en dessous/au-dessus. On ne sait pas où « on construit » le morceau.
   - Fix : une seule vue « Morceau » = la ligne du temps (arrangement), avec en bas une barre de scènes (A B C D…) à glisser sur la timeline. La liste de parties devient la vue « Liste » d'un bouton bascule. Retirer la grille de 16 scènes de la page (la garder dans un tiroir/feuille « Mes scènes »).

2. [bloquant] Trois « Live » différents, et le plus gros bouton de Morceau n'est pas de la création
   - Où : SessionScreen.kt:236-250 (bouton cyan « Live : jouer les sections piste par piste », plein de texte), SessionScreen.kt:320 (« Jouer en direct » = pads live + LivePad), LivePadsFullScreen.kt (pads plein écran), LiveScreen.kt (grille scènes x pistes + Effets/303/Pads). Dans Outils aussi : « Fond », « Latence ».
   - Pourquoi : « Live page », « Pads live », « Live pads plein écran » ne se distinguent pas par le nom ; le premier bouton de l'onglet Morceau est une fonction de PERFORMANCE alors que l'utilisateur vient FINIR un morceau. Charge cognitive énorme pour un débutant.
   - Fix : sortir le Live du flux de création : un seul bouton « Jouer en live » dans le menu (⋯) de la barre du haut ou dans un 8e espace « Live » hors des onglets de création. Fusionner pads live carte + plein écran en une seule entrée (carte = aperçu qui ouvre le plein écran). Nommer clairement : « Live » (scènes) vs « Pads » (échantillons).

3. [bloquant] Pas de chemin de création guidé unique après la génération ; la « Prochaine étape » est cachée dans l'onglet Rythme
   - Où : onboarding/NextSteps.kt:88-105 (8 étapes : Rythme, Basse, Mélodie, Sections, Morceau, Voix, Volumes, Partager) ; carte affichée dans AppRoot.kt:1120 (onglet Rythme seulement, `showHere = showGrid`) ; GuideDialog.kt:60-95.
   - Pourquoi : la checklist est la meilleure idée de l'app, mais elle ne vit que dans Rythme, se rouvre via Outils › Aide (AppRoot.kt:1738), et ne suit pas l'utilisateur. Le guide d'accueil décrit les onglets, pas la démarche. Les étapes envoient vers 5 onglets (Rythme → Acide → Synthé → Morceau → Mixeur).
   - Fix : une barre « Étape suivante » persistante (sous la barre du haut, 1 ligne : « 3/6 · Ajoute une mélodie → ») sur tous les onglets de création, repliable ; ou mieux, le « chemin » en tête de page (voir Parcours cible).

4. [pénible] Scène vs tout le morceau : deux niveaux d'édition mélangés, et le lien n'est visible que par une pastille
   - Où : TopBar.kt:295-340 (puce « C · Refrain » = scène à modifier, visible seulement sur Rythme/Acide/Synthé/Audio p.0, AppRoot.kt:843 et 1879 SCENE_TABS) ; Mixeur et Morceau sans puce ; réglage « Pendant la lecture : suivre le morceau / rester sur ma scène » (SessionScreen.kt:374).
   - Pourquoi : on édite « la scène C » sans savoir que ça change aussi les parties B/C répétées du morceau ; le mixeur, lui, est global, mais l'automation a un mode Boucle (scène) et Morceau. Sur top_bar.png la puce est tronquée (« C ▾ » sans nom) et le nom du projet aussi (« Da… »).
   - Fix : afficher en permanence « Section C · Refrain (utilisée 2×) » et un avertissement doux à la première modification d'une scène réutilisée ; mettre la puce dans TOUS les onglets de création ; garantir le nom de scène plutôt que « C » seul quand l'espace manque (raccourcir le nom du projet, pas la scène).

5. [pénible] Vocabulaire non stabilisé : scène / partie / section / morceau / ligne du temps / boucle
   - Où : SessionScreen.kt:330 (« Le morceau »), 563 (« Scènes »), 509 (« joue/ensuite »), 399 (« Insérer après la partie %d »), 572 (« Une scène = la batterie, l'acide et le piano roll »), NextSteps.kt:93 (« section »), TimelineScreen.kt:556/861/1495 (« Parties », « Partie %d »), onglet « Morceau » qui contient aussi « Le morceau », projet = « morceau » (Mixeur « Partager mon morceau ») ; « Mes projets » vs « Nouveau morceau guidé ».
   - Pourquoi : un débutant lit « scène », « section » et « partie » comme 3 choses. « Morceau » désigne à la fois l'onglet, la liste d'arrangement et le projet entier.
   - Fix : 2 mots seulement : « Section » (A Intro, B Couplet… la brique) et « Morceau » (l'enchaînement = l'arrangement). Renommer l'onglet « Morceau » en « Arrangement » (ou « Structure »), et le projet = « Projet » partout (Mes projets) ; « Partie » disparaît ; « scène » devient « section ». Mettre à jour Glossary.kt et les catalogues i18n d'un coup.

6. [pénible] Le mixage et l'automation sont loin de ce qu'ils servent ; le Mixeur est une longue pile de cartes
   - Où : MixerScreen.kt:206-262 (sortie générale, analyseur, limiteur), 231-248 (carte Automation et effets, boutons « Automatiser » / « Ce qui joue »), 432 (« Partager mon morceau » tout en haut), 464-560 (une carte par voie : niveau, volume, pan, effets repliés) ; mixer.png : 2 pistes = 1 écran entier, sans voir les autres pistes ; Solo resté actif sur « Basse acide » sans rappel.
   - Pourquoi : on ne voit jamais toutes les pistes ensemble avec leurs faders (comme une console) ; il faut défiler. Le Solo oublié rend le morceau « cassé » à l'export sans alerte visible (voir constat 9). « Ce qui joue » (Mixeur:248, Morceau:370, sous-menu de partie) est présent à 3 endroits.
   - Fix : Mixeur = vue console horizontale (faders verticaux côte à côte, M/S, niveau, un seul bouton « Effets »), détails (pan, effets) dans une feuille ouverte au toucher d'une voie ; l'automation en feuille depuis la voie ; un bandeau « Solo actif sur Basse acide : tout désactiver » persistant en haut ; « Ce qui joue » dans un seul endroit (bouton « ? Pourquoi ce son » de la barre du haut).

7. [pénible] Onglets Audio et Outils : rassemblent des fonctions disparates derrière un 2e niveau d'onglets
   - Où : AppRoot.kt:1506-1510 (Audio : Pistes audio / Remix / Accordeur / Générer), 1721-1735 (Outils : Aide, Réglages, Recette, MIDI, Fond, Latence, Son = 7 sous-onglets défilants) ; GuideDialog.kt:64-65.
   - Pourquoi : « Générer » (créer des sons par IA) et « Remix » (séparer un morceau) sont des fonctions de CONTENU cachées sous « Audio » à côté d'un Accordeur ; « Recette » (liste de test développeur) apparaît dans les outils d'un utilisateur ; l'Aide est le premier sous-onglet d'Outils (le 7e onglet de la barre) : peu de gens iront l'y chercher.
   - Fix : « Audio » → « Enregistrer / Importer » ; déplacer Remix et Générer dans la bibliothèque de sons / feuille « + Ajouter » ; Accordeur, Latence, Fond, Son → Réglages ; Recette → Réglages › Avancé (ou n'apparaît qu'en mode test) ; « ? » aide contextuel dans la barre du haut plutôt qu'un onglet.

8. [pénible] Pages longues et cartes empilées sur tous les onglets de création
   - Où : session.png (2,5 écrans pour Morceau) ; rhythm.png (transport + 16 pads + éditeur de pas + 4 sous-onglets Coups/Roulements/Pan/Hauteur + Remplir/Effacer + Longueur + encore d'autres cartes sous le pli : beaucoup de contrôles dès l'arrivée) ; SessionScreen.kt:372 (réglages du morceau, tempo/mesure/fin/suivi dans la carte de la liste) ; FoldSection partout (DESIGN.md « chaque carte à titre se replie »).
   - Pourquoi : le repli par carte déplace le problème : on ne sait pas ce qui est caché. « Temps par mesure 2/4…7/4 » prend 3 lignes dans l'écran principal du morceau alors que 99 % des morceaux sont en 4/4.
   - Fix : « Réglages du morceau » (fin, suivi, mesure, tempo) → feuille/engrenage ; garder au plus 2 cartes visibles par écran ; défaut « essentiel » avec bouton « Plus d'options ».

9. [pénible] Feedback : « où suis-je », « ça joue quoi », « sauvegardé ? » sont incomplets
   - Où : TopBar.kt:206 (« Sauvegardé » seul état, affiché ou masqué selon la largeur ; absent en top_bar.png étroit) ; barre du haut sans temps/mesure (SessionScreen.kt:426-432 : chrono et « mes. 2.3 / 14 » seulement dans l'onglet Morceau) ; transport Rythme.kt:251 « Écouter : cette section » (même réglage que Morceau:340, deux endroits pour le même choix).
   - Pourquoi : sur Rythme/Acide/Synthé on ne sait pas si ▶ joue la section en boucle ou le morceau (il faut lire un petit libellé gris « Écouter : ⇅ cette section » en capitale pâle) ; pas d'indicateur « modifications non sauvegardées / enregistrement en cours » autre que « Sauvegardé » ; la position dans le morceau n'est visible que dans l'onglet Morceau.
   - Fix : barre du haut : ▶ avec double état explicite (« ▶ Section » / « ▶ Morceau »), mini-position « mes. 14/60 » toujours visible pendant la lecture ; un seul réglage « Écouter » (barre du haut), retiré de la carte Morceau ; états sauvegarde : « Enregistrement… / Sauvegardé / Hors-ligne ».

10. [pénible] Partage : bien présent mais en 3 endroits et sans étape de vérification
   - Où : MixerScreen.kt:432 (bouton vert en tête du Mixeur), ProjectsScreen.kt:272, NextStepCard.kt:160, ShareSongUi.kt:53 ; « Autres exports » (Mixeur:435-436) qui ouvre une grosse carte Exporter avec Quoi/Format/Fondu/Avancé (MixerScreen.kt:564-726).
   - Pourquoi : pas de page « Fini ? » qui vérifie : morceau non vide, volume OK (le limiteur « travaille fort » est enfoui en MixerScreen.kt:346), Solo oublié, fin du morceau (fondu), durée. Le partage est dans le Mixeur, alors que la dernière chose faite est l'arrangement.
   - Fix : bouton « Partager » toujours dans la barre du haut (icône ⤴) → feuille « Prêt à partager » : durée, avertissements (Solo actif, saturation, morceau vide), choix « Écouter une dernière fois », format M4A/WAV, bouton principal.

11. [pénible] Entrée de l'app : « Nouveau projet » est plus simple que le reste, mais la fin du parcours n'est pas reliée
   - Où : projects_new.png (assistant guidé, 5 familles, rythme de départ, démos) ; AppRoot.kt:489/535 (atterrissage sur Morceau après l'assistant).
   - Pourquoi : bon départ (« Une touche, un morceau complet »), mais « Rythme de départ » (gris pâle, peu lisible) et « Écouter une démo » sont des cartes de poids égal au choix principal ; après la génération on arrive dans Morceau et on voit d'un coup Live, ligne du temps, liste, scènes : on ne sait pas quoi faire ensuite.
   - Fix : après la génération, écran de bienvenue : « Ton morceau est prêt ▶ Écouter / Modifier la structure / Changer un son / Partager » (4 gros boutons), puis le bandeau d'étape suivante.

12. [détail] Icônes / puces / couleurs : cohérence partielle
   - Où : AppRoot.kt:173-181 (icônes d'onglets : GridView pour Rythme, GraphicEq pour Acide, Equalizer pour Mixeur : GraphicEq et Equalizer se ressemblent ; Mic pour Audio ; QueueMusic pour Morceau ; Tune pour Outils) ; vert = « joue » mais aussi bouton Partager et Stop (mixer.png / rhythm.png) ; orange = accent ET « sélectionné » ET alerte ; jaune pour Automation (mixer.png) ; boutons « Écouter : cette section » est un chip dans Rythme, une ChoiceRow dans Morceau.
   - Fix : icônes distinctes (Acide = vague/robot ; Mixeur = faders) ; vert uniquement pour l'état de lecture ; un composant unique de bascule 2 états.

13. [détail] Chips et menus « … » cachent des fonctions clés
   - Où : SessionScreen.kt:527-551 (menu ⋮ d'une partie : Monter/Descendre/Dupliquer/Insérer/Retirer/Ce qui joue) ; Mixeur « Autres exports » ; Rythme sous-onglets Coups/Roulements/Pan/Hauteur (coupé « Hauteur » à droite sur rhythm.png) ; la bascule « Écouter ⇅ » ; MixerScreen.kt:436.
   - Fix : actions fréquentes (dupliquer, réordonner par glisser) à même la ligne ; chips à largeur adaptée (passage à la ligne ou défilement avec indice).

14. [détail] Guide et aide : correct mais passif
   - Où : GuideDialog.kt (3 pages : onglets, 3 gestes, ensuite) ; Glossary.kt (« ? » sur les titres) ; Outils › Aide.
   - Pourquoi : le guide décrit les onglets alors que le but est de finir un morceau ; il recommande « touche ▶, puis les pads de Rythme » mais l'assistant a déjà tout fait. Les « ? » sont dispersés (petits, pâles).
   - Fix : guide = « Comment finir ton morceau en 5 étapes » cliquable (même contenu que NextSteps) ; coach-marks sur la 1re visite d'un écran (une bulle).

## Parcours cible proposé : « Nouveau projet → Partager » en 5 étapes

Principe : un seul fil rouge « Chemin du morceau » visible en permanence, et 5 espaces qui correspondent aux 5 intentions, au lieu de 7 outils.

Barre du bas proposée (5 onglets + feuilles) :
1. **Sons** (ex Rythme + Acide + Synthé + Audio) : une liste de pistes verticale (Batterie, Basse acide, Synthé, Voix, +). Un toucher sur une piste ouvre son éditeur (grille de pas / pattern 303 / piano roll / enregistreur) dans la même page ; « + » ajoute (instrument, échantillon, micro, générer, remix).
2. **Structure** (ex Morceau) : la ligne du temps = LA vue d'arrangement ; barre de sections A B C… à glisser ; liste de parties en bascule ; réglages du morceau dans un engrenage.
3. **Mix** (ex Mixeur) : console horizontale + automation par voie.
4. **Live** (ex pads live + Live + Live plein écran) : un seul espace, hors du flux de création.
5. **Plus** (ex Outils) : projets, réglages, aide, MIDI, latence, recette (avancé).
Barre du haut commune : projet (⇒ Mes projets), section en cours « C · Refrain », ▶ (Section/Morceau), position mesure, ↶, ●, ⤴ Partager, ? aide.

Parcours (de l'écran « Nouveau projet » au partage) :
1. **Nouveau projet** : « Nouveau morceau guidé » ou un style (inchangé) → 1 à 3 touches.
2. **Écran « Ton morceau est prêt »** (nouveau) : ▶ Écouter, « Changer un son », « Modifier la structure », « Partager ». Le chemin (barre fine) : Sons ▸ Structure ▸ Mix ▸ Partager, avec coche sur les étapes faites.
3. **Sons** : changer/ajouter une piste (sons prêts, générer, micro) en restant dans la même page.
4. **Structure** : ajuster l'ordre des sections et les répétitions sur la ligne du temps ; ajouter intro/fin.
5. **Mix** : équilibrer (faders côte à côte), ajouter 1 effet/automation si voulu ; alerte si Solo actif ou saturation.
6. **Partager** (⤴ barre du haut) : feuille de vérification + bouton principal.
Total : 1 écran de départ + 4 espaces au lieu de 7 onglets et 3 vues d'arrangement, avec un seul vocabulaire : Projet, Section, Morceau.

Étape de migration à coût réduit (sans tout refaire) :
- (a) afficher la « Prochaine étape » en bandeau fin sur tous les onglets ;
- (b) renommer onglet « Morceau » → « Structure » et unifier le vocabulaire (section/morceau) ;
- (c) déplacer le bouton Live hors du haut de Structure ;
- (d) bouton Partager dans la barre du haut avec vérification ;
- (e) Remix/Générer sous « + » dans Sons ; Recette hors d'Outils ;
- (f) Mixeur en console.

## Top 5
1. Remplacer 3 vues d'arrangement (liste, scènes, ligne du temps) par une seule « Structure » = ligne du temps + barre de sections.
2. Sortir le Live (3 versions) du flux de création : une seule entrée « Live », pas le gros bouton en tête de l'onglet Morceau.
3. Unifier le vocabulaire : scène/partie/section/morceau/projet → « Section », « Morceau », « Projet » (onglet « Morceau » renommé « Structure »).
4. Rendre le « Chemin du morceau » (NextSteps) persistant sur tous les onglets + écran « Ton morceau est prêt » après l'assistant.
5. Partager dans la barre du haut avec vérification (Solo actif, saturation, morceau vide) ; Mixeur en console et « Ce qui joue » en un seul endroit.
