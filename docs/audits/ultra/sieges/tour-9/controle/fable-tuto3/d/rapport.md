code testé : 257267dba5ec6396b700731001861e3779756397
joué par : sous-agent neuf

siege: d | profil: debutant-motive | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=partage-niveau | texte: Niveau : comme la musique en streaming (−14) · Pas de saturation. | ecran: 42 | tenue: oui | pourquoi: le chapitre 15 annonçait « −14 : aussi fort que le streaming. Pas de saturation : rien ne grésille. » et l'écran « C'est prêt » le montre (chiffre du banc, banc?: son)
hesitations:
  - ecran: 26 | cherchais: un moyen de vider la partie « Montée » | regarde: menu de partie et menu ⋮, aucun « vider » ; j'ai fini par « Recommencer le tuto »
  - ecran: 46 | cherchais: effacer plusieurs cases d'un coup dans « Tous les pads » | regarde: glisser sur la grille ne fait rien, il faut toucher case par case
  - ecran: 64 | cherchais: pourquoi l'étape 1/3 ne passait pas après avoir effacé les croix visibles | regarde: il restait une croix plus bas (Bord de caisse), cachée sous le panneau du tuto ; trouvée en faisant défiler (écran 65)
abandon: jamais
incidents-banc:
defauts:
  - geste: 53 | ecran.vu: 53 | cible: Chapitres | geste.type: touche
    attendu: la liste des chapitres s'ouvre | obtenu: le panneau du tuto a bougé (de ~573 à ~537) juste avant, la touche tombe à côté ; refaite à l'écran 54
    symptome: rien
    gravite: finition | revu: oui: ecran 54 | banc?: non | promesse: -
  - geste: 64 | ecran.vu: 64 | cible: À toi : dans la partie « Montée », dernière mesure, efface les coups du dernier temps (t… | geste.type: glisse
    attendu: voir toutes les croix à effacer sans faire défiler, ou un mot « il en reste N » | obtenu: une croix sur Bord de caisse restait sous le panneau du tuto, l'étape semblait refuser alors qu'il restait un coup
    symptome: cache
    gravite: contourne-en-2-gestes | revu: oui: ecran 65 | banc?: non | promesse: -
  - geste: 46 | ecran.vu: 49 | cible: Tous les pads | geste.type: touche
    attendu: le texte d'écran dit quelles cases sont pleines | obtenu: les cases pleines et vides de la vue « Tous les pads » n'ont pas d'état lisible (bouton sans [allumé])
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - d-fin | corrigé | ecran: 16 | étape 1 validée seulement après la dernière croix (écran 7, et de nouveau écran 66 dans un projet neuf), cymbale du Drop validée au toucher (écran 10), variation validée au 4e coup comme écrit, pas avant (écrans 13-15 non, 16 « Bravo, chapitre fini ! »)
  - montee-vide | pas atteint | ecran: 57 | je n'ai trouvé aucun moyen de vider la Montée ; celle créée par « Créer la partie Montée » est une copie du Groove avec des coups, et l'étape 1/3 n'y était pas pré-validée (pas de « Déjà fait »)
  - cible-autre-partie | corrigé | ecran: 9 | en-tête « Partie : C · Montée » avec les croix dans la Montée (écran 2), puis « Partie : D · Drop » avec la case en pointillé de la cymbale dans le Drop (écran 9)
