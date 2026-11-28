code testé : ab7fbf4e8
joué par : sous-agent neuf

siege: a | profil: moyen | lang: fr | device: phone | font: 130 | mode: simple
mission: tuto-facile jump_up, chapitres 9 à 15 | buts: finir ch.9 à ch.15
chapitres:
  - ch.9 : compris du premier coup
  - ch.10 : hésitation (notes en pointillé hors de vue, compteur « Encore N notes » qui remonte)
  - ch.11 : hésitation (les boutons d'ajout bougent après le premier ajout, mauvais ordre, 3 Annuler)
  - ch.12 : compris du premier coup
  - ch.13 : compris du premier coup
  - ch.14 : compris du premier coup (il a fallu « Lire la suite »)
  - ch.15 : compris du premier coup (export M4A, « C'est prêt », chapitre réussi écran 93)
hesitations:
  - ecran: 46 | cherchais: ajouter les parties dans l'ordre demandé | regarde: les boutons « + X · nom » avaient changé de place après mon premier ajout
  - ecran: 28 | cherchais: la 4e note en pointillé | regarde: elle était au-dessus, hors de l'écran (« plus haut ↑ »), trouvée via « Touche ici »
  - ecran: 37 | cherchais: combien de notes restaient | regarde: « Encore 2 notes » est devenu « Encore 3 notes » après un toucher
abandon: jamais
incidents-banc:
  - geste: 81 | attente: 0 | vu: le texte de l'écran 81 n'avait pas la consigne « À toi » que l'image montre
defauts:
  - geste: 47 | ecran.vu: 47 | cible: bouton « + C · Montée » @ 166,327 | geste.type: touche
    attendu: ajouter « C · Montée » juste après « B · Intro », boutons restés à leur place | obtenu: après le 1er ajout les boutons d'ajout descendent d'environ 185 px ; mon doigt tombe sur le texte « − / + : répétitions… », les touchers suivants ajoutent les mauvaises parties, le tuto ne dit rien de l'ordre faux
    symptome: perdu
    gravite: contourne-en-9-gestes | revu: oui: ecran 62 | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 12 | cible: texte « Exemple passé sans l'écouter. » @ 222,429 | geste.type: touche
    attendu: le message concerne seulement l'étape passée | obtenu: il reste affiché à l'étape suivante même après avoir écouté son exemple
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 38 | cible: « ↑ Encore 2 notes en pointillé à toucher » | geste.type: touche
    attendu: le compteur baisse ou reste | obtenu: il passe à « Encore 3 notes » après « Touche ici », on ne sait plus où on en est
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 28 | cible: « Encore 1 note en pointillé, plus haut ↑ » | geste.type: touche
    attendu: les notes à toucher visibles dans la grille | obtenu: une note est hors de l'écran, il faut deviner qu'il faut monter ou toucher « Touche ici »
    symptome: cache
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 26 | cible: texte « Bravo, c'est réussi ! » [coupé] @ 224,622 | geste.type: touche
    attendu: le panneau de réussite lisible en entier | obtenu: le texte est coupé, l'explication sous « Bravo » manque
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 8 | cible: bouton « Télécharger le kit Jump up DnB (2 Mo) » | geste.type: touche
    attendu: proposé une fois | obtenu: revient à chaque étape jusqu'à « Plus tard » ; « Étape suivante » et « Suivant → » affichés en même temps (doublon)
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 84 | ecran.vu: 85 | cible: texte « Sortie » [coupé] @ 564,232 | geste.type: glisse
    attendu: toutes les colonnes du Mix alignées et visibles | obtenu: « Sortie » coupée au bord droit, carte Batterie plus haute que ses voisines, consigne « À toi : monte… » coupée sous l'en-tête
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 92 | ecran.vu: 92 | cible: texte « C'est prêt » @ 127,370 | geste.type: touche
    attendu: un titre puis le détail | obtenu: « C'est prêt » répété (titre et début de phrase)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -

fin-tranche: non (dernier step=share, done=38)
