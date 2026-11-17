code testé : ede109d8ada92d293e0cfd3b777f53b38e2e3b68
joué par : sous-agent neuf

siege: d | profil: adulte-fr-curieux | lang: fr | device: telephone | font: 100 | mode: simple
mission: tuto-facile (chapitre 12 « Les transitions ») | buts: gap-mots, cymbale-mots, partie-posee
promesse: id=gap-mots | texte: efface les 4 coups du pad « Claquement de mains » sur le dernier temps (touche les 4 croix rouges) | ecran: 2 | tenue: oui | pourquoi: « Mesure 4/4 · Claquement de mains », exactement 4 cases barrées (les 4 dernières, temps 4), le compteur descend 4→3→2→1 puis « Réussi »
promesse: id=cymbale-mots | texte: sur le pad « Cymbale », touche la toute première case, encore vide | ecran: 9 | tenue: oui | pourquoi: « PAS : CYMBALE · DROP », case 1 « vide, en pointillé : à toucher », la toucher donne « Bravo, chapitre fini ! » (écran 10)
promesse: id=partie-posee | texte: Il n'y a pas de partie « Montée » dans ton morceau : cette étape se fait dedans. / Créer la partie « Montée » | ecran: 22 | tenue: oui | pourquoi: phrase présente (écran 20) ; après le bouton, « I · Montée 4 mes. » est dans la liste du morceau entre « E · Break » et « F · Pont », morceau 128→132 mesures
promesse: id=vide-pas-fait | texte: Il n'y a pas de partie « Montée » dans ton morceau : cette étape se fait dedans. | ecran: 34 | tenue: oui | pourquoi: avec une partie vide « J · Montée », l'étape 1 ne dit ni « Déjà fait ! » ni « rien à toucher » ; le bouton remplit J (écran 35, 4 croix rouges, pas de partie en plus)
oeil-neuf: q1 | ecran: 35 | image: 35 | pads, croix, Mesure 1-4, Suivre, « Lire ^ », onglets touchables ; « Remplir tout 1/2 1/4 » et « Effacer » ont l'air de simple texte orange
oeil-neuf: q6 | ecran: 35 | image: 35 | preuve : « Partie : J · Montée » et « 4 coups en trop ici (barrés) » ; suite claire : toucher les croix
oeil-neuf: q9 | ecran: 35 | image: 35 | consigne du tuto coupée « Claquem… » (Lire pour la suite) ; titre du pas à demi caché sous la barre « Partie » (« DE MAINS · MONTÉE »)
hesitations:
  - ecran: 8 | cherchais: sur quel pad j'étais (la consigne parle de « Cymbale ») | regarde: le nom du pad était hors écran, il a fallu glisser vers le haut (écran 9 « PAS : CYMBALE · DROP »)
  - ecran: 34 | cherchais: si ma partie vide « J · Montée » comptait | regarde: le tuto dit qu'il n'y a pas de partie « Montée », alors qu'elle est dans la liste
abandon: jamais
incidents-banc:
defauts:
  - geste: 34 | ecran.vu: 34 | cible: texte « Il n'y a pas de partie « Montée » dans ton morceau : cette étape se fait dedans. » | geste.type: touche
    attendu: avec une partie « J · Montée » vide dans le morceau, une phrase du genre « ta partie Montée est vide » | obtenu: le tuto affirme qu'il n'y a pas de partie « Montée » alors que « J · Montée × 1 · 4 mes. » est dans la liste (le bouton la remplit bien ensuite)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: vide-pas-fait
  - geste: 34 | ecran.vu: 34 | cible: texte « Partie « Montée » créée (copie de « Drop ») : sans Grosse caisse studio, Grondement grave, Charley ouvert, Bord de caisse, Cymbale, Basse, Mélodie. » | geste.type: touche
    attendu: ce message disparaît (ou suit le nouveau nom) une fois la partie renommée « Groove » | obtenu: le message reste affiché, juste au-dessus de « Il n'y a pas de partie « Montée » » : les deux phrases se contredisent
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: partie-posee
defauts-avant:
  - d-V1 | corrigé | ecran: 20 | après renommage en « Pont », phrase « Il n'y a pas de partie « Montée » … » présente, et le bouton place « I · Montée » dans la liste du morceau (écran 22)
  - d-V2 | corrigé | ecran: 34 | partie vide nommée « Montée » : l'étape 1 ne dit pas « Déjà fait ! » (mais dit à tort qu'il n'y a pas de Montée)
  - d-P1 | corrigé | ecran: 2 | « 4 coups du pad « Claquement de mains » » = 4 croix rouges, toutes sur Claquement de mains, mesure 4, dernier temps
  - d-P2 | corrigé | ecran: 9 | pad « Cymbale » du Drop, toute première case vide et en pointillé, comme dit la consigne
