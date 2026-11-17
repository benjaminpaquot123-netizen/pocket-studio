code testé : 258441802eda97ec42ca0967188dfc83c7aeb29f
joué par : sous-agent neuf

siege: d | profil: controle-adulte | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile (chapitre 12 « Les transitions ») | buts: V1, V2, V3, V4
promesse: id=- | texte: Ton morceau avait déjà ce que demande cette étape : rien à toucher. | ecran: 53 | tenue: non | pourquoi: la partie « I · Montée » est vide (écran 54), il n'y a rien à effacer, l'étape est cochée quand même
promesse: id=- | texte: Créer la partie « Montée » | ecran: 21 | tenue: oui | pourquoi: le geste #22 crée « H · Montée » (écran 23), avec les croix rouges à effacer
promesse: id=- | texte: Bravo, chapitre fini ! | ecran: 38 | tenue: non | pourquoi: la Montée travaillée (H) n'est pas dans le morceau (écran 40, « + H · Montée » dans « Ajouter à la fin ») : le silence avant le Drop ne s'entend pas
oeil-neuf: q1 | ecran: 53 | image: 53 | « Étape suivante » (vert) et « Suivant → » (texte orange) côte à côte : deux façons d'avancer, on ne sait laquelle compte
oeil-neuf: q6 | ecran: 53 | image: 53 | « Déjà fait ! » sans montrer quelle partie a été regardée : aucune preuve à l'écran, on part sans avoir rien fait
oeil-neuf: q9 | ecran: 53 | image: 53 | rien de coupé dans le panneau ; la liste du morceau derrière est coupée en haut (« H · Fill »)
hesitations:
  - ecran: 21 | cherchais: une phrase qui dise que la partie « Montée » manque | regarde: seul le bouton « Créer la partie « Montée » » le laisse deviner
  - ecran: 23 | cherchais: où se trouve la nouvelle partie dans le morceau | regarde: l'en-tête dit toujours « Pont mesure 17/96 », rien ne dit qu'elle n'est pas posée
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 120 | vu: « Le banc ne tourne pas pour ce lot » au premier regarde, puis réponse normale au 2e essai (60 s plus tard)
defauts:
  - geste: 22 | ecran.vu: 40 | cible: bouton ⊕ «   Créer la partie « Montée » » | geste.type: touche
    attendu: la partie Montée est créée ET posée dans le morceau, juste avant le Drop | obtenu: « H · Montée » créée mais absente du morceau ; le chapitre se finit (« Bravo ») sur une partie qui ne joue jamais
    symptome: perdu
    gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
defauts-avant:
  - V1 | autre | ecran: 21 | quand « Montée » n'existe plus (renommée en Pont), seul un bouton « Créer la partie « Montée » » apparaît, aucune phrase ne dit qu'elle manque, et la partie créée (H) n'est pas posée dans le morceau (écran 40)
  - V2 | encore là | ecran: 53 | partie « I · Montée » neuve et vide : l'étape 1/3 dit « Déjà fait ! » sans rien faire (vide vérifié écran 54)
  - V3 | corrigé | ecran: 28 | étape 2/3 : la case en pointillé est dans « Partie : D · Drop », première case, « Encore 1 case en pointillé ici » ; croix de l'étape 1 dans « H · Montée » (écran 23), jamais dans A
  - V4 | corrigé | ecran: 35 | étape 3/3 : après les 4 cases maracas dans « F · Drop 2 », « Fait » et « Bravo, chapitre fini ! »
