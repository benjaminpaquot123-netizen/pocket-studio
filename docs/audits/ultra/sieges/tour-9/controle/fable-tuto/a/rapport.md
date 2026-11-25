code testé : d87dea73ff18887bc55b2ac5e0987f5e17be4c21
joué par : sous-agent neuf
siege: a | profil: debutant-motive | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=chap1 | texte: Bravo, chapitre fini ! | ecran: 8 | tenue: oui | pourquoi: chapitre 3 fini en suivant la consigne mot à mot
promesse: id=chap3 | texte: Bravo, chapitre fini ! | ecran: 83 | tenue: oui | pourquoi: chapitres 3, 4 et 5 finis (écrans 8, 62, 83), puis 6, 7 et 8 aussi (écrans 104, 127, 133)
promesse: id=bravo | texte: Bravo, chapitre fini ! | ecran: 133 | tenue: pas vue | pourquoi: j'ai vu le « Bravo » de chaque chapitre, pas celui de la fin du tuto ; budget fini au chapitre 9, étape 1/5 (écran 134)
hesitations:
  - ecran: 13 | cherchais: comment aller à la mesure 2 | regarde: le lien « Mesure 2 · N cases → » a disparu quand la grille a défilé ; j'ai glissé pour le retrouver
  - ecran: 71 | cherchais: où sont les notes La aigu demandées | regarde: rien dans la grille, seulement la bande « Encore 2 notes en pointillé, plus haut ↑ Touche ici »
  - ecran: 87 | cherchais: la « note du bas » de l'accord | regarde: l'anneau « Touche ici » était sur la note du haut
abandon: jamais
incidents-banc:
  - geste: 20 | attente: 0 | vu: six touches (gestes 20 à 25) envoyées d'un coup dans une boucle de commandes, pas une à la fois comme demandé (faute de ma part, aucun outil autre que human-act.py)
  - geste: 131 | attente: 4 | vu: « attends 4 » et une touche envoyés dans un même appel (faute de ma part)
  - geste: 0 | attente: 0 | vu: plusieurs sorties de human-act.py raccourcies avec sed/head (filtre d'affichage seulement, aucun fichier du dépôt lu)
defauts:
  - geste: #13 | ecran.vu: 13 | cible: Mesure 2 · 8 cases → | geste.type: touche
    attendu: après la dernière case de la mesure 1, le lien vers la mesure 2 reste visible | obtenu: la grille défile, le lien disparaît ; il faut glisser puis toucher Mesure 2
    symptome: sans-aide
    gravite: contourne-en-2-gestes | revu: oui: ecran 32, 51 | banc?: non | promesse: -
  - geste: #71 | ecran.vu: 71 | cible: Encore 2 notes en pointillé, plus haut ↑ Touche ici | geste.type: touche
    attendu: les notes à poser sont visibles au-dessus du panneau, avec l'anneau | obtenu: notes hors de l'écran (en haut au chapitre 5, sous le panneau du tuto aux écrans 106 et 117) ; il faut toucher « Touche ici » pour les voir
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 106, 117 | banc?: non | promesse: -
  - geste: #111 | ecran.vu: 111 | cible: Encore 3 notes en pointillé, plus bas ↓ | geste.type: touche
    attendu: le compteur descend régulièrement (5, 4, 3…) | obtenu: le compteur est descendu à « Encore 1 » (écran 110) puis remonte d'un coup à « Encore 3 notes »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 124 | banc?: non | promesse: -
  - geste: #116 | ecran.vu: 116 | cible: écris les 8 notes de la mesure 2 | geste.type: touche
    attendu: la consigne et la bande disent le même nombre de notes | obtenu: la consigne demande 8 notes alors que la bande dit « Encore 1 note »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #87 | ecran.vu: 87 | cible: touche la note du bas de chaque accord … Il y a 4 coups | geste.type: touche
    attendu: l'anneau « Touche ici » est sur la note du bas, comme dit le texte | obtenu: l'anneau est sur la note du haut (ligne Do4) ; la toucher pose quand même l'accord
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #86 | ecran.vu: 86 | cible: Exemple passé sans l'écouter. | geste.type: touche
    attendu: pas de reproche, j'avais écouté l'exemple (il jouait encore, 3 s restantes) | obtenu: message « Exemple passé sans l'écouter. »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: #97 | ecran.vu: 97 | cible: Accords en pointillé à toucher :… Touche ici | geste.type: touche
    attendu: la bande dit en entier ce qu'il reste à faire | obtenu: texte coupé par « … », pas de « Lire la suite » ; même chose écran 106 (« ↓ Encore 5 notes en pointillé à … »)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 106 | banc?: non | promesse: -
  - geste: #9 | ecran.vu: 9 | cible: Chapitre 4/15 | geste.type: touche
    attendu: la ligne « Chapitre N/15 · Étape x/y » reste affichée | obtenu: la ligne manque à l'écran 9, et disparaît pendant que l'exemple joue (écran 64)
    symptome: perdu
    gravite: finition | revu: oui: ecran 64 | banc?: non | promesse: -
mots-faux:
defauts-avant:
  - a-V1 | encore là | ecran: 71 | au chapitre 5 les La aigu sont hors de l'écran en haut, aux écrans 106 et 117 des notes sont cachées sous le panneau du tuto ; il faut toucher « Touche ici » pour les voir. Les notes visibles ont bien l'anneau et entrent en une touche (écrans 76 à 82, 98)
  - D3 | corrigé | ecran: 1 | la première ligne de la consigne est entière, finit par « … » quand elle est coupée, « Lire la suite » montre le reste (écrans 1, 9, 47) ; la carte du kit ne l'a jamais cachée
  - D4 | encore là | ecran: 20 | une étape demande 24 touches sur 2 mesures (écran 20), une autre 9 accords (écran 93), une autre « Créer la piste » puis 8 notes (écran 106)
  - a-V4 | pas atteint | ecran: 134 | toujours « Partie : A » jusqu'au chapitre 9 étape 1/5, budget fini avant d'aller sur la partie B
