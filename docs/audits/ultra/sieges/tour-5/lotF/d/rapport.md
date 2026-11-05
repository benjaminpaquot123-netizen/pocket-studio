siege: d | profil: t5-Fd gaucher-une-main | lang: fr | device: phone411 | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t5-Fd-1 | texte: Remix et découpe faisables au pouce gauche | ecran: 50 | tenue: non | pourquoi: séparé, découpé et posé, mais Simple/Pro, « Faire un morceau… », « Utiliser cette chanson », poignées du bout et Remix du menu Plus sont hors du pouce : il a fallu passer par Pad Live, Retour et refaire l'import
promesse: id=t5-Fd-2 | texte: → Pad jamais sur un son que ton rythme joue | ecran: 85 | tenue: pas vue | pourquoi: phrase jamais affichée ; « Sur quel pad ? » propose les 16 pads sans marque, dont « 1 Grosse caisse 1 » alors que j'avais mis un coup au pas 1 (#69), et les rangées du haut sont hors du pouce
promesse: id=t5-Fd-3 | texte: Garder / Sur une piste | ecran: 68 | tenue: oui | pourquoi: « est dans ta bibliothèque (Voix) » (écran 67) puis « « Voix 1 » est sur une nouvelle piste audio, à la mesure 1. » (écran 68)
hesitations:
  - ecran: 3 | cherchais: comment choisir Simple ou Pro d'une main | regarde: deux cartes en haut, tout le bas de l'écran vide
  - ecran: 50 | cherchais: comment déplacer le début et la fin du bout | regarde: poignées sur la règle, puis Début ici / Fin ici
  - ecran: 74 | cherchais: comment revenir au Remix depuis mon morceau | regarde: menu Plus collé à droite en haut
abandon: jamais
incidents-banc:
  - geste: 26 | attente: 1100 | vu: séparation qui avance d'environ 15 % par geste, quelle que soit l'attente (#26 à #31)
defauts:
  - geste: 2 | ecran.vu: 3 | cible: bouton « Simple Pour débuter : 5 onglets, de gros boutons et des mots de tous les jours. » | geste.type: touche
    attendu: choisir le mode au pouce gauche | obtenu: Simple et Pro tout en haut, hors de portée ; aucun choix possible, seul Retour sort
    symptome: hors-pouce
    gravite: contourne-en-8-gestes | revu: oui: ecran 10 | banc?: non | promesse: t5-Fd-1
  - geste: 34 | ecran.vu: 35 | cible: touchable « Dans un nouveau projet Un projet vide ; la chanson t'attend dans le Remix pour en prendre des morceaux. » | geste.type: touche
    attendu: un nouveau projet avec la chanson | obtenu: retombe sur « Simple ou Pro ? » inaccessible ; Retour obligatoire
    symptome: hors-pouce
    gravite: contourne-en-11-gestes | revu: oui: ecran 10 | banc?: non | promesse: t5-Fd-1
  - geste: 33 | ecran.vu: 34 | cible: touchable « Faire un morceau à partir de cette chanson Un nouveau projet à son tempo et dans sa tonalité (120 BPM, La mineur), avec ses pistes prêtes. » | geste.type: touche
    attendu: le choix recommandé (en premier) atteignable | obtenu: hors de portée du pouce, rien ne se passe
    symptome: hors-pouce
    gravite: contourne-en-11-gestes | revu: non | banc?: non | promesse: t5-Fd-1
  - geste: 37 | ecran.vu: 38 | cible: bouton « Utiliser cette chanson » | geste.type: touche
    attendu: réutiliser la chanson déjà séparée | obtenu: hors de portée ; seule issue : rechoisir la même chanson avec le bouton du bas
    symptome: hors-pouce
    gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: t5-Fd-1
  - geste: 51 | ecran.vu: 52 | cible: texte « Fin du bout » [état « 0:07.00 »] | geste.type: glisse
    attendu: tirer la poignée de fin du bout | obtenu: poignées et forme d'onde hors du pouce ; découpe seulement avec Début ici / Fin ici après avoir fait défiler la vue
    symptome: hors-pouce
    gravite: contourne-en-5-gestes | revu: oui: ecran 65 | banc?: non | promesse: t5-Fd-1
  - geste: 52 | ecran.vu: 55 | cible: bouton « Fin ici » | geste.type: touche
    attendu: seule la fin bouge, la durée affichée suit | obtenu: le début saute aussi (0:01 à 0:04), « Écouter le bout 6 temps · 0:03.0 » ne change pas, puis « Ce morceau de la piste est muet : rien n'a été posé. »
    symptome: incompris
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 56 | cible: bouton « Annuler » (nom « Annuler la dernière action ») | geste.type: touche
    attendu: annuler seulement le dernier réglage du bout | obtenu: tout l'import de la chanson est annulé (tempo 124, Refrain parti, retour au Remix) ; le bandeau dit encore « Tempo calé sur Ma chanson préférée : 120 »
    symptome: perdu
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 58 | ecran.vu: 63 | cible: texte « Coche des bouts pour les envoyer. » | geste.type: touche
    attendu: voir et cocher les phrases découpées | obtenu: la rangée Couper ici / Début / Fin / Enlever / Boucle et la liste des bouts sont sous le panneau d'envoi ; la page ne défile pas (#62)
    symptome: cache
    gravite: contourne-en-2-gestes | revu: oui: ecran 63 | banc?: non | promesse: t5-Fd-1
  - geste: 74 | ecran.vu: 75 | cible: touchable « Remix » | geste.type: touche
    attendu: rouvrir le Remix depuis le menu Plus | obtenu: menu entier collé à droite en haut, hors du pouce ; il a fallu Accueil, Remix, rechoisir la chanson et la remettre dans le projet
    symptome: hors-pouce
    gravite: contourne-en-10-gestes | revu: non | banc?: non | promesse: t5-Fd-1
  - geste: 83 | ecran.vu: 84 | cible: texte « « Voix 1 » est sur une nouvelle piste audio, à la mesure 1. » | geste.type: touche
    attendu: un écran de découpe neuf après le nouvel import | obtenu: le message d'avant reste affiché, comme si je venais de poser une piste
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
