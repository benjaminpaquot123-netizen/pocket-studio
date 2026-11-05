siege: b | profil: compositeur au piano roll | lang: en | device: tablet11Land | font: 100 | mode: pro
mission: libre | buts atteints: 2 sur 3
hesitations:
  - ecran: 6 | cherchais: comment écrire sur 16 mesures | regarde: « Loop 1 bar + », 15 touchers un par un (l'appui-long n'accélère pas)
  - ecran: 58 | cherchais: la vélocité | regarde: rien au-dessus de la grille, trouvée en faisant défiler sous la grille (« Velocity », bande coupée en bas)
  - ecran: 81 | cherchais: allonger une note déjà posée | regarde: bord droit de la note, puis la puce « Note 1/2 » (écran 84), puis « Legato »
promesse: Le piano roll permet d'écrire 16 mesures proprement, d'annuler, de copier, de quantifier | tenue: en partie | pourquoi: 16 mesures, Undo/Redo, Copy/Paste et « Put in time » (+ « As played ») marchent, mais impossible de changer la durée d'une note posée et le collage est imprévisible
surprise: « Paste bar 3 → 5 → 7… » enchaîne tout seul, 16 mesures remplies en 8 touchers ; mais aucune note n'est jouée quand on la pose
abandon: j'aurais abandonné à l'écran 84 parce que je ne trouve aucun moyen de rallonger une note déjà écrite
defauts:
  - ecran: 81 | cible: une note posée (bord droit) | geste: glisse
    attendu: allonger la note | obtenu: la note est déplacée ; « Note 1/2 » avec la note sélectionnée ne change rien non plus (écran 84)
    symptome: promesse
    gravite: grave | bloque: oui
  - ecran: 24 | cible: grille en mode « ✏ Draw » | geste: glisse
    attendu: dessiner une note de la longueur du glissé | obtenu: rien, aucune note créée
    symptome: rien
    gravite: moyen | bloque: non
  - ecran: 64 | cible: « Paste bar 2 » | geste: touche
    attendu: coller là où je veux / où l'étiquette suit la règle | obtenu: colle mesure 2 par-dessus les notes existantes (chevauchements), règle surligne la mesure 4, étiquette figée ; « Paste bar 16 » (écran 40) coupe la 2e mesure sans prévenir
    symptome: incompris
    gravite: moyen | bloque: non
  - ecran: 41 | cible: « See all » | geste: touche
    attendu: voir les 16 mesures et la mélodie | obtenu: 16 mesures mais 4 rangées seulement, la mélodie (C5) hors vue ; sans cela la grille n'a que 5 à 7 rangées sous « Record my melody » et 3 barres d'outils (écrans 6, 30)
    symptome: coupe
    gravite: moyen | bloque: non
  - ecran: 62 | cible: grille du piano roll | geste: glisse
    attendu: remonter la page vers les outils (Draw, Note, Loop) | obtenu: c'est la grille qui défile ; seule la marge gauche (x≈145) fait remonter la page
    symptome: perdu
    gravite: moyen | bloque: non
  - ecran: 71 | cible: « Scale » | geste: appui-long
    attendu: choisir tonalité et mode (ex. la mineur) | obtenu: rien, Do majeur imposé, pas de gamme surlignée dans la grille ; « Chords » pose un accord sur C# sans tenir compte de la gamme (écran 98)
    symptome: rien
    gravite: moyen | bloque: non
  - ecran: 94 | cible: « In time 50% », « Legato », « Humanize », « Paste bar N » | geste: touche
    attendu: un « ? » ou une étiquette qui explique | obtenu: applique sans message, « ? Label the screen » (écran 95) n'étiquette pas ces outils
    symptome: sans-aide
    gravite: mineur | bloque: non
