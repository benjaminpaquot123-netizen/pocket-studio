code testé : f1b9fdb1fdb38b1824bbf4bb43318cc6f9291277
joué par : sous-agent neuf

siege: a | profil: camille-debutante-fr | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile (tuto House, du chapitre 3 jusqu'au chapitre 4 au moins ; fini jusqu'au chapitre 5) | buts: chapitre-3, chapitre-4, chapitre-5-basse
promesse: id=D5 | texte: À toi : sur le pad « Claquement de mains », touche les cases en pointillé des 2 mesures, sur les temps 2 et 4. | ecran: 5 | tenue: oui | pourquoi: la grille dit « Mesure 1/2 », Longueur 2, deux cases pointillées par mesure
promesse: id=D3 | texte: Lire la suite | ecran: 12 | tenue: non | pourquoi: au début du chapitre 4 (et du 3, écran 1) la consigne repliée n'apparaît pas du tout, seuls kit, « Plus tard » et « ▸ Pourquoi ? » se voient
promesse: id=D4 | texte: À toi : touche « Créer la piste Basse », puis les notes en pointillé de la mesure 1 (La grave, La aigu). | ecran: 73 | tenue: non | pourquoi: 1 bouton + 6 notes dans la même consigne ; l'étape 3/3 (écran 84) demande copier + écrire toute la mesure 4
promesse: id=D1 | texte: Basse · mesure 4/8 Encore 6 notes en pointillé à toucher | ecran: 86 | tenue: non | pourquoi: après le toucher, 5 notes sur 6 visibles, la 6e cachée plus haut sans flèche ; la flèche ne vient qu'en dernier (écran 91)
oeil-neuf: q1 | ecran: 94 | image: 94 | les onglets, pistes, notes et « Bravo, chapitre fini ! » ont l'air touchables ; « En rythme » coupé à droite ressemble à un bouton sans contour
oeil-neuf: q6 | ecran: 94 | image: 94 | preuve : bandeau vert « Toutes les notes en pointillé sont posées » et « Bravo, chapitre fini ! » ; la suite est dans la carte repliée, il faut l'ouvrir pour voir « Chapitre suivant »
oeil-neuf: q9 | ecran: 94 | image: 94 | « En rythme 50 % » coupé par le bord droit ; « + Piste » coupé en haut à droite
hesitations:
  - ecran: 1 | cherchais: la consigne du chapitre | regarde: seulement le titre, le kit et « Lire la suite », il faut deviner que la consigne est derrière
  - ecran: 74 | cherchais: les 2 notes « La aigu » annoncées | regarde: 4 notes visibles alors que le bandeau dit 6, rien n'indique où sont les 2 autres
  - ecran: 84 | cherchais: la fin de la consigne de l'étape 3/3 | regarde: coupée après « pour revenir », il faut « Lire la suite »
abandon: jamais
incidents-banc:
  - geste: 4 | attente: 2 | vu: écran presque vide (seule la barre du bas) juste après « Je le fais », revenu au geste suivant
defauts:
  - geste: 11 | ecran.vu: 12 | cible: icône « Lire la suite » ⌄ @ 294,744 | geste.type: touche
    attendu: au début du chapitre, la consigne repliée lisible au-dessus du kit | obtenu: aucune ligne de consigne, seulement kit, « Plus tard », « ▸ Pourquoi ? » et « Lire la suite »
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 1 | banc?: non | promesse: D3
  - geste: 1 | ecran.vu: 2 | cible: icône « Lire la suite » ⌄ @ 294,744 | geste.type: touche
    attendu: la carte s'ouvre et le lien devient « Réduire » | obtenu: la carte recouvre la grille, le lien dit encore « Lire la suite », au 2e toucher la 1re ligne de consigne est coupée (écran 3)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 72 | ecran.vu: 73 | cible: texte « À toi : touche « Créer la piste Basse », puis les notes en pointillé de la mesure 1 (La grave, La aigu). » | geste.type: touche
    attendu: une consigne = au plus deux gestes | obtenu: créer la piste puis 6 notes dans la même consigne
    symptome: incompris | gravite: finition | revu: oui: ecran 84 | banc?: non | promesse: D4
  - geste: 85 | ecran.vu: 86 | cible: bouton « Basse · mesure 4/8 Encore 6 notes en pointillé à toucher » | geste.type: touche
    attendu: les 6 notes visibles ou une flèche vers celle qui manque | obtenu: rien ne bouge, 5 notes visibles, la 6e hors champ sans flèche (même chose écran 75, mesure 1)
    symptome: cache | gravite: contourne-en-6-gestes | revu: oui: ecran 75 | banc?: non | promesse: D1
  - geste: 90 | ecran.vu: 91 | cible: bouton « Basse · mesure 4/8 Encore 1 note en pointillé, plus haut ↑ Touche ici » | geste.type: touche
    attendu: « plus haut ↑ » lisible | obtenu: l'image montre « Encore 1 note en pointillé, plus … » : la flèche ↑ est coupée par « Touche ici » (idem écran 79)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: D1
  - geste: 83 | ecran.vu: 84 | cible: texte « À toi : touche « Copier sur les mesures suivantes » : il fait la mesure 2, et la même chose sur Ré en mesure 3. Écris enfin la mesure 4 : elle finit sur Mi et Sol pour revenir vers La. » [coupé] | geste.type: touche
    attendu: consigne entière | obtenu: coupée après « pour revenir », la fin est derrière « Lire la suite »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - D3 | autre | ecran: 12 | kit, « Plus tard » et « ▸ Pourquoi ? » se voient sans « Lire la suite », mais aux débuts des chapitres 3 et 4 (écrans 1 et 12) la consigne repliée n'est pas affichée du tout ; seul le début du chapitre 5 (consigne d'une ligne, écran 70) tient la phrase en entier
  - D5 | corrigé | ecran: 5 | consigne « des 2 mesures », grille « Mesure 1/2 » puis « Mesure 2/2 », Longueur 2 ; même chose au chapitre 4 (écran 12)
  - D4 | encore là | ecran: 73 | étape 2/3 : créer la piste + 6 notes dans une consigne ; étape 3/3 (écran 84) : copier + écrire la mesure 4 (6 notes)
  - D1 | autre | ecran: 86 | toucher « Basse · mesure 4/8 » ne change rien : 5 notes entières, la 6e cachée sans flèche ; la flèche « plus haut ↑ Touche ici » vient seulement quand les visibles sont posées (écran 91), avec ↑ coupé ; « Touche ici » montre bien la note entière (écran 92)
