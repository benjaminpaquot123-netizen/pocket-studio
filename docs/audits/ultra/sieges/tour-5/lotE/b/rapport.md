siege: b | profil: t5-Eb-lit-mal-icones | lang: en | device: phone | font: 100 | mode: simple
mission: premier-morceau | buts: son, partie, partage
promesse: id=t5-Eb-1 | texte: Chaque bouton a un nom (barre du haut, ▶, réglages de piste, touches, coins des pads) | ecran: 4 | tenue: non | pourquoi: le ▶ « écouter le style » de l'assistant (#4) et le ⋮ d'une partie (#25) n'ont aucun nom ; les touches du clavier, elles, en ont un (« F 4 », #36)
promesse: id=t5-Eb-2 | texte: un premier morceau possible sans lire les petits textes | ecran: 32 | tenue: oui | pourquoi: son, 2e partie et partage faits aux icônes et aux gros titres (étoiles, éclair, ▶ orange, +, avion), mais la 2e partie a demandé de deviner longtemps
hesitations:
  - ecran: 3 | cherchais: quel style choisir | regarde: six lignes toutes en barres grises, j'ai choisi à l'icône (éclair)
  - ecran: 7 | cherchais: où ajouter une 2e partie | regarde: menus déroulants pleins de mots illisibles (#7, #13), deux boutons qui ouvrent tous les deux le tempo (#9, #15), « Write a note » (#17)
  - ecran: 26 | cherchais: le bouton « ajouter une partie » | regarde: une feuille de 13 boutons, trouvé le + en bas après avoir essayé le + de la ligne (#24)
abandon: jamais
incidents-banc:
defauts:
  - geste: 4 | ecran.vu: 4 | cible: bouton (icône) @ 57,483 | geste.type: touche
    attendu: le ▶ d'écoute du style a un nom (« Listen to Techno ») | obtenu: bouton sans nom (journal : wizard_style_listen_techno, aucun libellé)
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: t5-Eb-1
  - geste: 25 | ecran.vu: 24 | cible: bouton (icône) @ 510,372 | geste.type: touche
    attendu: le ⋮ d'une partie a un nom (« Part options ») | obtenu: bouton sans nom (journal : part_menu_8, aucun libellé)
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: t5-Eb-1
  - geste: 24 | ecran.vu: 25 | cible: bouton (icône) @ 438,372 | geste.type: touche
    attendu: le + à côté d'une partie ajoute une partie (c'est ce que dit l'icône pour qui ne lit pas) | obtenu: aucune partie en plus, juste le − qui s'allume ; il faut passer par ⋮ > + « Insert after » > choisir un nom
    symptome: incompris
    gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: t5-Eb-2
  - geste: 13 | ecran.vu: 14 | cible: bouton « ▭▭▭ » @ 118,180 | geste.type: touche
    attendu: le choix de partie (« Part: I · Build-up ▾ ») montre les parties avec un repère visuel et un « + nouvelle partie » | obtenu: une liste de 17 lignes de texte seul, illisible pour moi, sans icône ni couleur
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t5-Eb-2
  - geste: 7 | ecran.vu: 8 | cible: bouton « ▭▭▭ ▭▭▭ » @ 248,114 | geste.type: touche
    attendu: le bouton de position « Build-up ▾ » en haut se comprend sans lire | obtenu: une longue liste de lignes de texte seul (play_from_menu), j'ai fait retour sans comprendre
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t5-Eb-2
  - geste: 3 | ecran.vu: 3 | cible: bouton « ▭▭▭ ▭▭▭ » @ 294,384 | geste.type: touche
    attendu: les noms des familles de styles (« House », « Techno »…) lisibles | obtenu: noms en gris très pâle (contraste 1,7 à 2,2:1 relevé par le banc), seules les icônes guident
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t5-Eb-2
  - geste: 34 | ecran.vu: 35 | cible: interrupteur « ▭▭▭ » [non coché] @ 543,114 | geste.type: touche
    attendu: le « ? » (« Label the screen ») aide quelqu'un qui lit mal | obtenu: il ajoute encore plus de petites étiquettes de texte, rien d'autre (ni son, ni image)
    symptome: sans-aide
    gravite: finition | revu: non | banc?: non | promesse: t5-Eb-2
mots-faux:
