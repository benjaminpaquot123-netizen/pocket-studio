siege: c | profil: producteur FL Studio | lang: fr | device: tablet8 | font: 100 | mode: simple
mission: padlive | buts atteints: 1 sur 3
hesitations:
  - ecran: 5 | cherchais: quand la prise REC de la perf demarre vraiment | regarde: bouton REC en haut a droite, reste sur « Prêt » sans decompte
  - ecran: 37 | cherchais: ou retrouver la perf enregistree dans le projet | regarde: accueil, Continuer, Mes projets, Options de Live
  - ecran: 1 | cherchais: lequel des trois REC enregistre la perf | regarde: REC en haut, « Enregistrer ce que je joue » en bas, bulle « ● REC » flottante
surprise: « Arrêter » coupe la musique du projet mais les boucles des pads continuent, meme apres retour a l'accueil
abandon: j'aurais abandonné à l'écran 31 parce que la prise REC reste bloquee sur « Prêt » et aucune perf ne peut etre gardee
defauts:
  - ecran: 5 | cible: REC (Enregistrer la performance) | geste: touche
    attendu: decompte puis enregistrement et bouton Fin | obtenu: reste « Prêt » indefiniment (ecrans 5 a 31, 15 s d'attente, pads joues)
    symptome: bloque | gravite: bloquant | bloque: oui
  - ecran: 23 | cible: Enregistrer ce que je joue | geste: touche
    attendu: enregistrement des pads | obtenu: « Pas assez de mémoire pour enregistrer », puis plus rien au second essai (ecran 25)
    symptome: bloque | gravite: bloquant | bloque: oui
  - ecran: 2 | cible: Boucle de kick, pad A1 | geste: touche
    attendu: pad marque « joue » et visuellement allume | obtenu: son annonce mais le pad reste « prêt » et identique a l'image (ecran 7)
    symptome: incompris | gravite: grave | bloque: non
  - ecran: 2 | cible: Jouer (transport) | geste: touche
    attendu: transport affiche lecture quand des boucles jouent | obtenu: reste « Jouer » alors que 3 boucles tournent
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 34 | cible: Pack… > Techno (page B) | geste: touche
    attendu: la page B se remplit du pack Techno | obtenu: page B reste « vide », 16 pads vides
    symptome: rien | gravite: grave | bloque: non
  - ecran: 12 | cible: Filtre · grave ↔ aigu | geste: appui-long
    attendu: effet actif signale a l'ecran | obtenu: aucun changement visible ni annonce (idem glisse ecran 13, Montée ecran 19)
    symptome: rien | gravite: moyen | bloque: non
  - ecran: 17 | cible: Boucle de kick, pad A1 | geste: appui-long
    attendu: menu pad (muet, solo, volume, quantif) | obtenu: rien, aucun muet/solo par pad ni reglage de quantification du lancement
    symptome: sans-aide | gravite: moyen | bloque: non
  - ecran: 28 | cible: Arrêter | geste: touche
    attendu: tout s'arrete | obtenu: seules les boucles des pads continuent, il faut « Tout couper » cache dans Plus
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 43 | cible: Autres exports (passe en mode Pro) | geste: touche
    attendu: Mode Pro deja actif, exports avances disponibles | obtenu: l'appli est en mode Simple (ecran 46) alors que le profil est Pro
    symptome: mauvais-ecran | gravite: moyen | bloque: non
  - ecran: 43 | cible: Partager mon morceau | geste: touche
    attendu: partager la perf Pad Live | obtenu: « Rien à entendre » puis export d'un m4a du projet vide (ecran 45), la perf n'est pas dans le projet
    symptome: perdu | gravite: grave | bloque: oui