code testé : f1b9fdb1fdb38b1824bbf4bb43318cc6f9291277
joué par : sous-agent neuf

siege: d | profil: leo-debutant-presse | lang: fr | device: telephone | font: 100 | mode: simple
mission: tuto-facile (chapitre 12 du tuto House sans 10 ni 11) | buts: D8
promesse: id=- | texte: Bravo, c'est réussi ! | ecran: 28 | tenue: non | pourquoi: l'étape 1 (effacer les coups du dernier temps de la Montée) est validée dès la création d'une Montée vide, rien n'a été effacé
promesse: id=- | texte: Cette étape n'est pas encore faite. Touche encore « Suivant » pour la passer quand même. | ecran: 38 | tenue: non | pourquoi: les 4 maracas sont posées sur les temps 1 à 4 de la mesure 1 de « Drop 2 » (là où Montre-moi touche) et l'étape reste « pas faite »
oeil-neuf: q1 | ecran: 44 | image: 44 | les 4 cases roses, Remplir/Effacer, Suivre, la carte du tuto se touchent ; « Lire ^ » ressemble à un lien, rien n'indique la case à toucher ensuite
oeil-neuf: q6 | ecran: 44 | image: 44 | les 4 coups sont posés (preuve : cases roses) mais la barre de progression ne bouge pas, aucune suite proposée
oeil-neuf: q9 | ecran: 44 | image: 44 | consigne coupée « une sur chaque... » dans la carte repliée ; la rangée « Mesure 1 2 3 4 » est coupée sous la barre « Partie »
hesitations:
  - ecran: 4 | cherchais: où ouvrir le tuto House depuis mon projet vide | regarde: rien dans l'écran ; trouvé par ⋮ › Aide › J'apprends, qui crée un AUTRE projet (Mon projet 3)
  - ecran: 16 | cherchais: quoi faire après « Je le fais » à l'étape 1 | regarde: la carte se replie sur la partie A vide, aucune croix rouge, aucune Montée
  - ecran: 24 | cherchais: la consigne de l'étape 2 | regarde: la carte ne montre que « Télécharger le kit House » et « Je le fais », la consigne n'apparaît qu'avec « Lire la suite »
abandon: jamais
incidents-banc:
  - geste: 16 | attente: 0 | vu: écran texte vide (seulement la barre Mode retours), l'image montrait l'écran normal
  - geste: 39 | attente: 0 | vu: idem pendant « Montre-moi »
defauts:
  - geste: 14 | ecran.vu: 14 | cible: bouton (doigt qui touche) «   Je le fais » | geste.type: touche
    attendu: projet vide, chapitre 12 : on me dit que la partie « Montée » manque, avec un bouton pour la créer | obtenu: seulement « Je le fais », qui replie la carte sur la partie A ; le bouton « Créer la partie « Montée » » n'apparaît qu'après avoir posé un coup ailleurs (écran 27)
    symptome: sans-aide
    gravite: contourne-en-13-gestes | revu: oui: ecran 21 | banc?: non | promesse: D8
  - geste: 26 | ecran.vu: 26 | cible: bouton « 1 » [coupé, état « vide, en pointillé : à toucher »] | geste.type: touche
    attendu: étape 2 « dans le Drop » : la case en pointillé est dans le Drop ou on me dit de créer le Drop d'abord | obtenu: la case en pointillé est dans la partie A ; je pose la cymbale dans A, ensuite seulement « Créer la partie « Drop » » apparaît
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 28 | cible: texte « Bravo, c'est réussi ! » | geste.type: touche
    attendu: créer la Montée, puis effacer les coups du dernier temps pour réussir l'étape | obtenu: « Bravo » dès la création ; la Montée est vide, rien n'a été effacé
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 28 | cible: texte « Partie « Montée » créée (copie du Groove) : sans Cymbale. » | geste.type: touche
    attendu: le nom d'une partie qui existe | obtenu: « copie du Groove » alors que le projet n'a qu'une partie « A », jamais appelée Groove
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 30 | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 38 | cible: bouton « 4 » [état « coup, force 100 % »] | geste.type: touche
    attendu: 4 maracas sur les temps 1 à 4 de la mesure 1 de « Drop 2 » = étape 3 réussie | obtenu: rien ne bouge ; Suivant dit « Cette étape n'est pas encore faite » ; « Montre-moi » touche exactement les mêmes cases
    symptome: rien
    gravite: contourne-en-2-gestes | revu: oui: ecran 44 | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 11 | cible: bouton « House 122 BPM » → | geste.type: touche
    attendu: le tuto s'ouvre dans le projet vide que je venais de créer (Mon projet 2) | obtenu: un troisième projet « Mon projet 3 » est créé, Mon projet 2 reste vide à côté
    symptome: mauvais-ecran
    gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - D8 | autre | ecran: 14 | à l'arrivée sur le chapitre 12 (projet vide, revu écran 21) rien ne dit que la Montée manque, juste « Je le fais » ; le bouton « Créer la partie « Montée » » n'apparaît qu'après un coup posé dans A (écran 27), et il marche (écran 28)
