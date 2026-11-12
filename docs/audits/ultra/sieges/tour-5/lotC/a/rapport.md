siege: a | profil: t5-Ca enfant-6-ne-lit-pas | lang: fr | device: tablet11Land | font: 100 | mode: simple
mission: defi | buts: ouvert, tente, compris
promesse: id=t5-Ca-1 | texte: (je ne lis pas) cadre « Écoute le rythme, puis refais-le sur les pads. » | ecran: 3 | tenue: oui | pourquoi: à la 1re ouverture (écran 2) pads vides « + » et cadre presque vide quelques secondes, puis tout rempli seul sans rien toucher (écran 3) ; 2e ouverture remplie tout de suite (écran 39)
promesse: id=t5-Ca-2 | texte: (je ne lis pas) points jaunes et rouges dans le cadre du niveau 2 | ecran: 15 | tenue: non | pourquoi: les ronds rouges = le pad 2 qui s'allume dans l'exemple, mais les ronds jaunes ne disent pas quel pad ; aucun pad n'est marqué hors exemple, 4 pads sont jaunes
promesse: id=t5-Ca-3 | texte: (je ne lis pas) cadre du défi | ecran: 3 | tenue: pas vue | pourquoi: aucun chiffre visible dans le cadre ; le seul « 124 » est en haut, hors du cadre ; textes du cadre masqués pour moi
promesse: id=t5-Ca-4 | texte: (je ne lis pas) ☆☆☆ sous les ronds | ecran: 37 | tenue: pas vue | pourquoi: 3 essais, au mieux 1 rond vert sur 4 et 0 étoile ; le banc ne tape pas en vrai rythme (banc rythme)
hesitations:
  - ecran: 5 | cherchais: le bouton pour « à moi de jouer » après l'exemple | regarde: le gros bouton jaune avec ✓ (c'est « J'ai fini »), alors que les pads ne s'allument pas pour dire « tape maintenant »
  - ecran: 15 | cherchais: quel pad taper sur les ronds jaunes | regarde: les pads jaunes (4 pareils) ; exemple réécouté 2 fois, seul le pad 2 s'allume sur les ronds rouges
  - ecran: 2 | cherchais: les dessins des pads | regarde: 16 cases vides avec « + », j'ai failli taper un « + »
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 5 | vu: pads vides puis remplis après « attends 5 » (chargement « Préparation des pads prêts à jouer… »)
defauts:
  - geste: 5 | ecran.vu: 6 | cible: bouton « ▭▭▭ » @ 1726,585 | geste.type: touche
    attendu: le gros bouton jaune après l'exemple me laisse jouer | obtenu: l'essai se termine tout de suite, 4 ronds rouges, 0 étoile, sans que j'aie tapé
    symptome: incompris
    gravite: contourne-en-2-gestes | revu: oui: ecran 19 | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 15 | cible: texte (icône) @ 1587,441 | geste.type: touche
    attendu: au niveau 2, voir quel pad va avec les ronds jaunes (pad qui brille ou même couleur unique) | obtenu: ronds jaunes sans pad désigné ; 4 pads jaunes ; l'exemple trop court pour voir lequel s'allume
    symptome: sans-aide
    gravite: contourne-en-2-gestes | revu: oui: ecran 26 | banc?: temps | promesse: t5-Ca-2
  - geste: 1 | ecran.vu: 2 | cible: texte « + » @ 157,255 | geste.type: touche
    attendu: pads et cadre du défi remplis dès l'arrivée | obtenu: 16 pads vides « + » et cadre sans niveaux ni ronds pendant quelques secondes
    symptome: lent
    gravite: finition | revu: oui: ecran 39 | banc?: temps | promesse: t5-Ca-1
  - geste: 32 | ecran.vu: 37 | cible: texte (icône) @ 1563,510 | geste.type: touche
    attendu: au moins une étoile en tapant le pad 2 sur les 4 ronds | obtenu: 1 rond vert sur 4, 0 étoile (3 essais)
    symptome: rien
    gravite: finition | revu: oui: ecran 14 | banc?: rythme | promesse: t5-Ca-4
mots-faux:
