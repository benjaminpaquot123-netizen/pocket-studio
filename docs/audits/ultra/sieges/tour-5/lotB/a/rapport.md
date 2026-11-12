siege: a | profil: t5-Ba retraitee-70-ans-200 | lang: fr | device: phone | font: 200 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t5-Ba-1 | texte: Aucun bandeau « Nouveau son / C'est fait » ni panneau du Guide ne cache le clavier ou les cases | ecran: 84 | tenue: non | pourquoi: pas de bandeau « Nouveau son / C'est fait » vu, mais le panneau du tuto (même replié) laisse 2 à 4 rangées de la grille des notes à 200 % et a caché la dernière note en pointillé (écrans 73, 84, 90)
promesse: id=t5-Ba-2 | texte: À toi : sur le pad « Perc » (un petit « toc »), touche les cases en pointillé : il tombe à côté des temps, décalé. | ecran: 49 | tenue: non | pourquoi: aucun pad « Perc » à l'écran ; la grille ouverte est « PAS : BORD DE CAISSE » (pad « Bord de caisse FX D 50 ») ; les autres pads (Grosse caisse studio, Grondement grave, Claquement de mains, Charley ouvert) collent à l'écran
promesse: id=t5-Ba-3 | texte: Bravo, ton morceau est fini ! (non vu ; vu à la place : « Bravo, chapitre fini ! ») | ecran: 36 | tenue: pas vue | pourquoi: budget épuisé au chapitre 7/15 ; seuls « Bravo, chapitre fini ! » et « Écouter mon morceau » vus, lisibles à 200 % ; « Bravo, ton morceau est fini ! » jamais atteint
hesitations:
  - ecran: 20 | cherchais: sur quel pad je pose les cases | regarde: la grille s'ouvre sans le nom du pad, le haut « Encore 4 cases en poi… » est coupé
  - ecran: 73 | cherchais: la dernière note « plus haut ↑ » de la mesure 4 | regarde: après la flèche, la note est sortie de la grille, 4 glissements et un essai au hasard
  - ecran: 97 | cherchais: l'aide (« Aide et premiers pas ») | regarde: rien ne s'ouvre, je retombe sur le tuto
abandon: ecran 109 | parce que le budget de 120 gestes est presque épuisé au chapitre 7/15 : la fin du tuto (« Bravo ») est hors d'atteinte à ce rythme (chaque chapitre Clavier coûte 15 à 25 gestes à 200 %)
incidents-banc:
  - geste: 19 | attente: 0 | vu: écrans 20-21, 26, 32 : le texte ne donne que la barre du bas (anneau du tuto), il a fallu l'image pour voir les cases
  - geste: 75 | attente: 0 | vu: plafond d'images atteint, note posée à l'aveugle (#77)
defauts:
  - geste: 43 | ecran.vu: 49 | cible: À toi : sur le pad « Perc » (un petit « toc »), touche les cases en pointillé : il tombe à côté des temps, décalé. | geste.type: touche
    attendu: le tuto nomme le pad comme l'écran (« Bord de caisse ») | obtenu: « Perc » n'existe nulle part ; la grille ouverte s'appelle « PAS : BORD DE CAISSE »
    symptome: texte-faux
    gravite: contourne-en-1-gestes | revu: oui: ecran 103 | banc?: non | promesse: t5-Ba-2
  - geste: 72 | ecran.vu: 73 | cible: Basse · mesure 4/8 Encore 1 note en pointillé, plus haut ↑ | geste.type: touche
    attendu: la flèche amène la note en pointillé à la vue | obtenu: la grille défile mais la note reste invisible (sous l'en-tête ou le panneau du tuto) ; 4 glissements + 1 essai au hasard
    symptome: cache
    gravite: contourne-en-5-gestes | revu: non | banc?: non | promesse: t5-Ba-1
  - geste: 83 | ecran.vu: 84 | cible: Accords · mesure 1/8 Encore 6 notes en pointillé à toucher | geste.type: touche
    attendu: à 200 % la grille des notes reste lisible au-dessus du panneau du tuto replié | obtenu: 4 rangées seulement, les notes en pointillé collées au bord du panneau « Le stab »
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 55 | banc?: non | promesse: t5-Ba-1
  - geste: 89 | ecran.vu: 90 | cible: Annuler la dernière action | geste.type: touche
    attendu: annuler ne bouge pas l'écran | obtenu: une 2e ligne d'en-tête apparaît (« Mon projet 1 », « Rétablir »), la grille descend sous le panneau du tuto (1 rangée visible), puis remonte au geste suivant : mes doigts tapent à côté
    symptome: cache
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t5-Ba-1
  - geste: 96 | ecran.vu: 97 | cible: Aide et premiers pas | geste.type: touche
    attendu: une aide s'ouvre | obtenu: le menu se ferme, rien d'autre (tuto en cours)
    symptome: rien
    gravite: finition | revu: oui: ecran 101 | banc?: non | promesse: -
  - geste: 80 | ecran.vu: 81 | cible: Créer la piste Accords | geste.type: touche
    attendu: la grille s'ouvre là où sont les notes à poser (mesure 1) | obtenu: elle s'ouvre mesure 4 : « ← Encore 6 notes en pointillé, plus tôt »
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: oui: ecran 109 | banc?: non | promesse: -
  - geste: 31 | ecran.vu: 35 | cible: Touche d'abord « Passer à 2 mesures » si on te le propose. | geste.type: touche
    attendu: les cases des 2 mesures, ou rien sur 2 mesures | obtenu: une seule mesure de 16 cases, chapitre validé ; le texte parle de 2 mesures pour rien
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 4 | cible: Mon morceau | geste.type: touche
    attendu: le nom de l'onglet en entier | obtenu: « morceau » coupé par le bas à 200 %
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 109 | banc?: non | promesse: -
mots-faux:
  - ecran: 98 | vu: Batterie | attendu: Rythme
  - ecran: 98 | vu: Volumes | attendu: Mix
