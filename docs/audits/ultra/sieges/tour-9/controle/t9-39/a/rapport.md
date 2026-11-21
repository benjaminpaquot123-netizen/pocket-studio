code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: moyen | lang: fr | device: tablet8 | font: 100 | mode: simple
mission: tuto jump_up ch1-8 | buts: -
chapitres:
  - ch1 La vitesse et la couleur du morceau : compris du premier coup
  - ch2 Le kick : compris du premier coup
  - ch3 Caisse claire et clap : compris du premier coup
  - ch4 Les charleys : compris du premier coup
  - ch5 La basse : hésitation (note en pointillé cachée plus haut, hors de l'écran)
  - ch6 Les accords : compris du premier coup
  - ch7 La mélodie : hésitation (consigne « 2 notes », il en restait une 3e en mesure 3)
  - ch8 Ta première partie : le Groove : compris du premier coup
hesitations:
  - ecran: 58 | cherchais: la note en pointillé qui manquait | regarde: elle était une octave plus haut, hors de l'écran
  - ecran: 62 | cherchais: si l'étape était finie | regarde: « Bravo, c'est réussi ! » sous un bandeau « Encore 2 notes en pointillé à toucher »
  - ecran: 89 | cherchais: pourquoi encore une note après les 2 demandées | regarde: bandeau « plus loin → Touche ici », note en mesure 3
abandon: jamais
incidents-banc:
defauts:
  - geste: 58 | ecran.vu: 58 | cible: Encore 1 note en pointillé, plus haut ↑ | geste.type: touche
    attendu: les 3 notes demandées sont toutes visibles sur l'écran | obtenu: après les 3 notes visibles, il en reste une, cachée une octave au-dessus
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 66 | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 62 | cible: Basse · mesure 4/8 Encore 2 notes en pointillé à toucher | geste.type: touche
    attendu: « Bravo, c'est réussi ! » seulement quand il ne reste plus de note à toucher | obtenu: le tuto dit Bravo et le bandeau juste au-dessus dit qu'il reste 2 notes
    symptome: incompris | gravite: finition | revu: oui: ecran 75 (accords) et 89 (mélodie, « Réussi » avec 1 note restante) | banc?: non | promesse: -
  - geste: 88 | ecran.vu: 89 | cible: À toi : touche les 2 notes en pointillé de la mesure 1 : elles répondent à la basse. | geste.type: touche
    attendu: après les 2 notes de la mesure 1, plus rien à toucher | obtenu: une 3e note en pointillé attend en mesure 3, que la consigne ne mentionne pas
    symptome: texte-faux | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 3 | ecran.vu: 4 | cible: Exemple passé sans l'écouter. | geste.type: touche
    attendu: pas de reproche, j'avais écouté une bonne partie de l'exemple | obtenu: le tuto dit que je l'ai passé sans l'écouter (il restait 7 s)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 53 | ecran.vu: 53 | cible: Étape suivante | geste.type: touche
    attendu: un seul bouton pour avancer | obtenu: « Étape suivante » et « Suivant → » en même temps, je ne sais pas lequel prendre (revu écran 99, ch8)
    symptome: incompris | gravite: finition | revu: oui: ecran 99 | banc?: non | promesse: -
  - geste: 52 | ecran.vu: 53 | cible: Acid 303 Ligne de basse | geste.type: touche
    attendu: une seule piste basse après « Créer la piste Basse » | obtenu: une piste « Acid 303 Ligne de basse » existait déjà, j'ai deux basses sans savoir pourquoi
    symptome: incompris | gravite: finition | revu: oui: ecran 86 | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 20 | cible: Drum & bass · Caisse FX | geste.type: touche
    attendu: des noms de sons écrits de la même façon | obtenu: « Drum & bass · Caisse FX » à côté de « Claquement de mains FX » (préfixe sur l'un, pas sur l'autre)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 72 | ecran.vu: 72 | cible: À toi : touche les 2 notes en pointillé de la mesure 1 : elles répondent à la | geste.type: touche
    attendu: le texte du tuto a de la marge en bas | obtenu: la 2e ligne du texte touche presque le bord bas du panneau (revu écrans 78, 86)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 86 | banc?: non | promesse: -
mots-faux:
defauts-avant:
fin: chapitres 1 à 8 finis, environ 102 gestes sur 174

fin-tranche: non (dernier step=groove_name, done=22 ; ROUTE ligne 493 ; écran 101 montre « Chapitre 8/15 · Fait » et « Bravo, chapitre fini ! », mais aucune ligne step=brick : « Chapitre suivant » non touché)
