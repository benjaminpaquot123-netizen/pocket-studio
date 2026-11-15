code testé : 8a8542fe1bffe24594417d7416cac7b13662451b
joué par : sous-agent neuf
siege: a | profil: t7-1a | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t7-1a-1 | texte: Les chapitres 4 à 6 se font sans note cachée ni consigne que je ne comprends pas | ecran: 45 | tenue: non | pourquoi: notes pointillées à moitié sous le panneau (45, 59) et « 8 cases » demandées pour 4 visibles (21)
promesse: id=t7-1a-2 | texte: Le compteur « Encore N notes » baisse d'un à chaque note que je pose | ecran: 66 | tenue: oui | pourquoi: -1 par touche partout (4→0, 12→0, 9→1 accords)
promesse: id=t7-1a-3 | texte: Chaque consigne nomme un pad, un bouton ou une mesure que je trouve à l'écran | ecran: 21 | tenue: non | pourquoi: « 8 cases (4 par mesure) » alors que la grille fait 1 mesure de 4 cases
promesse: id=t7-1a-4 | texte: tous les textes vus sont en français, aucun coupé | ecran: 26 | tenue: non | pourquoi: consigne et bouton « Télécharger le kit House (4 Mo) » [coupé], chevauchés par « Pourquoi ? »
oeil-neuf: q1 | ecran: 114 | image: 114 | cases, Remplir tout/1/2/1/4, longueurs, « Étape suivante » : tout semble touchable et l'est ; « Effacer » et « Remplir » sans contour mais touchables
oeil-neuf: q6 | ecran: 114 | image: 114 | preuve claire : « 122 Tempo » en haut et bandeau vert « Réussi » + « Étape suivante → »
oeil-neuf: q9 | ecran: 114 | image: 114 | rien de coupé ni chevauché sur cet écran
tuto:
  - chapitre 4 (charleys et bord de caisse) : hésitation (consigne coupée, 8 cases pour 4)
  - chapitre 5 (la basse) : hésitation (notes sous le panneau, mesure 3 sautée)
  - chapitre 6 (le piano) : hésitation (« note du bas » des accords sous le panneau)
  - chapitre 7 (la petite boucle) : hésitation (16 notes, 5 visibles, flèches seulement à la fin)
  - chapitre 3 (le clap) : compris du premier coup (sauf « 2 mesures » pour 1)
  - chapitre 1 (vitesse et couleur) : compris du premier coup, étapes 2 et 3 réussies, budget fini avant « Revenir au tuto »
hesitations:
  - ecran: 21 | cherchais: les 8 cases en pointillé | regarde: 1 seule mesure, 4 cases, « Encore 4 »
  - ecran: 43 | cherchais: la mesure 3 (Ré) | regarde: l'appli passe de la mesure 1 à la 4
  - ecran: 59 | cherchais: la note du bas de l'accord | regarde: cachée sous le panneau, j'ai touché la plus basse visible
abandon: ecran 118 | parce que budget de 120 gestes atteint (chapitre 1 étape 3 réussie, pas revenu au tuto)
incidents-banc:
  - geste: 3 | attente: 0 | vu: écran sans texte (seule la barre de retours) après « Je le fais », lu sur l'image
  - geste: 102 | attente: 0 | vu: même écran vide après « Je le fais » (chapitre 3)
  - geste: 0 | attente: 0 | vu: le brief annonce le code 98e83c21, le banc a écrit 8a8542fe en tête ; outils hors human-act : Read du brief, des consignes et des images
defauts-avant:
  - T4/N4 | pas corrigé (écran 45) : 3 notes pointillées à moitié sous le panneau ; « Encore 6 » sans flèche à 35 et 43 (flèche seulement après les notes visibles)
  - N8 | corrigé (écran 66) : accords, 9 au départ, -1 par touche
  - T11 | corrigé (écran 35) : « Créer la piste Basse » ouvre la grille sur « mesure 1/8 » comme la consigne
  - T2 | corrigé (écran 21) : la consigne nomme « Bord de caisse », pad trouvé sous ce nom
  - T8 | pas corrigé (écran 34) : étape 2 de la basse = créer la piste, poser la mesure 1, copier, Ré mesure 3, écrire mesure 4 (5 actions)
  - T6 | corrigé (écran 57) : kick, charley, accord, mesure expliqués en mots simples
defauts:
  - geste: 45 | ecran.vu: 45 | cible: Encore 6 notes | geste.type: touche
    attendu: chaque note pointillée en entier au-dessus du panneau, ou une flèche | obtenu: 3 notes à moitié sous le panneau du tuto
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 59 | banc?: non | promesse: t7-1a-1
  - geste: 35 | ecran.vu: 35 | cible: Encore 6 notes | geste.type: touche
    attendu: flèche ↑/↓ vers les notes hors écran dès le début | obtenu: 4 notes visibles, aucune flèche avant de les avoir posées
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: oui: ecran 43 | banc?: non | promesse: t7-1a-1
  - geste: 59 | ecran.vu: 59 | cible: toucher la note du bas | geste.type: touche
    attendu: la note du bas de chaque accord visible | obtenu: cachée sous le panneau, la plus basse visible marche
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t7-1a-1
  - geste: 21 | ecran.vu: 21 | cible: touche les 8 cases en pointillé (4 par mesure) | geste.type: touche
    attendu: autant de cases que la consigne en annonce | obtenu: grille d'1 mesure, 4 cases, « Encore 4 » (même écart chap. 3, écran 101 : « 2 mesures » pour 1)
    symptome: incompris | gravite: finition | revu: oui: ecran 101 | banc?: non | promesse: t7-1a-3
  - geste: 1 | ecran.vu: 1 | cible: À toi : sur le pad « Charley ouvert »… [coupé] | geste.type: touche
    attendu: consigne lisible en entier | obtenu: dernière ligne coupée et chevauchée par « ▸ Pourquoi ? » (aussi écrans 8, 21)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: t7-1a-4
  - geste: 26 | ecran.vu: 26 | cible: Télécharger le kit House (4 Mo) [coupé] | geste.type: touche
    attendu: bouton entier | obtenu: mot du bouton coupé, chevauché par « Pourquoi ? » (aussi 33, 53 ; « Plus tard » coupé à 30)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 53 | banc?: non | promesse: t7-1a-4
  - geste: 34 | ecran.vu: 34 | cible: À toi : … Basse | geste.type: touche
    attendu: au plus 3 actions par consigne | obtenu: 5 actions enchaînées, l'appli saute la mesure 3 annoncée
    symptome: incompris | gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 85 | ecran.vu: 85 | cible: Encore 16 notes | geste.type: touche
    attendu: consigne « 8 notes de la mesure 1 » cohérente avec le compteur, notes entières | obtenu: compteur 16, 5 visibles, une note coupée au bord droit
    symptome: cache | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
