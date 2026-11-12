siege: d | profil: testeur QA | lang: fr | device: phone | font: 130 | mode: simple
mission: libre | buts atteints: 3 sur 3
hesitations:
  - ecran: 4 | cherchais: renommer le projet | regarde: le titre en haut ; le toucher ouvre Mes projets, il faut un appui long sans indice
  - ecran: 27 | cherchais: le bouton Jouer | regarde: la barre du haut, cachée par la bulle « REC / Noter / ⋯ »
surprise: Mes projets dit « Aucun projet pour l'instant » alors que « Mon premier rythme » est ouvert et marqué Sauvegardé
abandon: jamais
defauts:
  - ecran: 5 | cible: Mon premier rythme (nom « Sauvegardé ») | geste: touche
    attendu: Mes projets liste le projet ouvert | obtenu: « Aucun projet pour l'instant » (liste visible seulement après une modif, écran 30)
    symptome: perdu
    gravite: grave | bloque: non
  - ecran: 27 | cible: Jouer | geste: touche
    attendu: la lecture démarre | obtenu: la bulle Noter s'ouvre ; Annuler lance REC (écran 12), resté « Patiente… » jusqu'à l'écran 47
    symptome: mauvais-ecran
    gravite: grave | bloque: non
  - ecran: 28 | cible: Écrire une note | geste: retour
    attendu: fermer la fenêtre de note seulement | obtenu: le studio se ferme derrière, Accueil affiché, la fenêtre de note reste ouverte
    symptome: mauvais-ecran
    gravite: moyen | bloque: non
  - ecran: 10 | cible: champ « Nom du projet » | geste: ecris
    attendu: refus des caractères en trop ou message | obtenu: fin du nom coupée sans prévenir (« rythme » → « rythm », « (copie) (copie) » → « (copie) », écran 40)
    symptome: perdu
    gravite: moyen | bloque: non
  - ecran: 78 | cible: Mon premier ryth…me (copie) OUVERT | geste: touche
    attendu: même durée partout (6 mesures, 0:36) | obtenu: Mes projets « 1 mes. » ; Accueil « 0:36 · 1 partie » ; studio « 6 mesures · 0:36 »
    symptome: incompris
    gravite: moyen | bloque: non
  - ecran: 14 | cible: Annuler la dernière action | geste: touche
    attendu: un bouton Rétablir après Annuler | obtenu: aucun Rétablir nulle part (barre du haut, onglet Plus)
    symptome: sans-aide
    gravite: moyen | bloque: non
  - ecran: 65 | cible: Guide · Ajoute une mélodie | geste: touche
    attendu: même progression en Simple et en Pro | obtenu: 23 % en Pro (écran 74), 33 % en Simple pour le même projet
    symptome: incompris
    gravite: mineur | bloque: non
  - ecran: 102 | cible: Continuer … 0:05 · 1 partie | geste: retour
    attendu: même durée que le studio | obtenu: studio « 6 mesures · 0:04 » (écran 101) à 300 BPM, Accueil « 0:05 »
    symptome: incompris
    gravite: mineur | bloque: non
