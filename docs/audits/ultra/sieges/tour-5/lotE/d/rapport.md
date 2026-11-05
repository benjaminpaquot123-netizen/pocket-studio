siege: d | profil: t5-Ed-dj-club-couche | lang: fr | device: phoneLand | font: 100 | mode: pro
mission: padlive | buts: son, stop, prise
promesse: id=t5-Ed-1 | texte: Laser, coup, prêt, pad A6 | ecran: 18 | tenue: non | pourquoi: toucher le haut d'un pad (bande du badge « coup ») ouvre « Pad A6 : Laser » au lieu de le jouer ; au centre ça joue bien (#1)
promesse: id=t5-Ed-2 | texte: EFFETS DE SCÈNE / 124 BPM / REC | ecran: 1 | tenue: oui | pourquoi: effets, tempo et REC sont là dès l'arrivée, sans passer par « Gros pads »
promesse: id=t5-Ed-3 | texte: Tout couper | ecran: 12 | tenue: non | pourquoi: caché derrière « Plus », libellé « Tout cou… », et le morceau continue de jouer après (3 essais)
promesse: id=t5-Ed-4 | texte: Lancer la ligne 1 à la mesure | ecran: 4 | tenue: oui | pourquoi: les boucles 1, 3, 4 partent, le bouton devient « Arrêter la ligne 1 »
hesitations:
  - ecran: 1 | cherchais: un bouton pour tout couper d'un coup | regarde: rien sur l'écran, trouvé seulement en ouvrant « Plus »
  - ecran: 33 | cherchais: la différence entre « REC » en haut et « Enregistrer ce que je joue » | regarde: deux boutons d'enregistrement qui font des choses différentes, sans le dire
  - ecran: 35 | cherchais: pourquoi l'enregistrement ne part pas | regarde: « Prêt : ça part à la mesure » reste figé tant que je n'appuie pas sur Jouer
abandon: jamais
incidents-banc:
defauts:
  - geste: 11 | ecran.vu: 12 | cible: bouton « Tout couper » | geste.type: touche
    attendu: tout s'arrête (pads et morceau), comme un kill sur une table de mix | obtenu: les pads se coupent mais « la musique du projet joue en boucle », le bouton reste « Arrêter », aucun message
    symptome: rien
    gravite: contourne-en-1-gestes | revu: oui: ecran 16 et 30 | banc?: non | promesse: t5-Ed-3
  - geste: 17 | ecran.vu: 18 | cible: bouton « Laser, coup, prêt, pad A6 » | geste.type: touche
    attendu: le haut du pad joue le son comme le reste du pad | obtenu: la fiche « Pad A6 : Laser » s'ouvre (le badge « coup » et toute la bande du haut = Réglages)
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: oui: ecran 20 (pad Sirène, touché à gauche du badge) | banc?: non | promesse: t5-Ed-1
  - geste: 10 | ecran.vu: 40 | cible: bouton « Tout couper » | geste.type: touche
    attendu: « Tout couper » lisible et entier, à portée de pouce | obtenu: libellé « Tout cou… », bouton sous le bandeau du bas (écran 16), il faut ouvrir « Plus » puis faire défiler
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 21 | banc?: non | promesse: t5-Ed-3
  - geste: 45 | ecran.vu: 47 | cible: bouton « 125 BPM » | geste.type: touche
    attendu: taper 128 sur le pavé du Tempo | obtenu: couché, seules les touches 1 à 6 sont visibles, 7 8 9 0 et la validation sont hors écran, le panneau ne défile pas (#46, #49) ; contourné avec Fermer + 3 × « Plus vite »
    symptome: cache
    gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 46 | ecran.vu: 47 | cible: bouton « 5 » | geste.type: glisse
    attendu: un glissé sur la fiche Tempo la fait défiler | obtenu: le glissé tape la touche « 5 » (le tempo affiché change)
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 41 | cible: bouton « Chute · freinage » | geste.type: glisse
    attendu: glisser pour faire défiler la colonne d'effets | obtenu: le glissé déclenche l'effet « Chute · freinage » en plein live, et la colonne ne défile pas
    symptome: incompris
    gravite: finition | revu: non | banc?: son | promesse: -
  - geste: 32 | ecran.vu: 35 | cible: bouton « Prêt : ça part à la mesure » | geste.type: touche
    attendu: l'enregistrement part au premier pad, ou un mot dit d'appuyer sur Jouer | obtenu: reste armé indéfiniment, le pad Kick joue mais rien ne s'enregistre, aucune aide
    symptome: sans-aide
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 54 | ecran.vu: 1 | cible: bouton « Fête » | geste.type: touche
    attendu: le bouton des packs porte un nom ou une icône | obtenu: case vide sans texte entre « D » et « ? », on ne devine pas que c'est le choix du pack
    symptome: sans-nom
    gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 40 | cible: texte « Ta prise est sur le pad B1. » | geste.type: touche
    attendu: message entier | obtenu: la première lettre est rognée par le bord gauche de l'écran (« a prise est sur le pad B1. »)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 16 | cible: texte « Perf gardée · 0:07 » | geste.type: touche
    attendu: les pads gardent leur taille après la prise | obtenu: le bandeau « Perf gardée » rétrécit toute la grille, noms coupés (« Boucl… », « Cri « … », « Charle… »)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 21 (redevient normal après Fermer) | banc?: non | promesse: -
mots-faux:
