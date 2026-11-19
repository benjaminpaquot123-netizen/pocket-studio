code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: moyen | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto trance chapitres 7-13 | buts: -
promesse: id=- | texte: Bravo, ton morceau est fini ! Tu peux tout réécouter et continuer à le changer. | ecran: 82 | tenue: oui | pourquoi: les 13 chapitres finis, morceau exporté en M4A et renommé « Trance du soir »
chapitres:
  - 7 Comprendre les parties : compris du premier coup
  - 8 Construis tes parties : hésitation (le « Montre-moi » de l'étape 3 m'a envoyé sur le mauvais bouton)
  - 9 Range tes parties : compris du premier coup
  - 10 Les transitions : compris du premier coup
  - 11 Les effets : compris du premier coup (écoute seulement)
  - 12 Le mix : les volumes : compris du premier coup
  - 13 Exporte ton morceau : compris du premier coup
hesitations:
  - ecran: 22 | cherchais: comment créer la Montée « à ma façon » après la démo | regarde: le doigt de la démo montre « + Nouvelle partie (copie de …) », pas le gros bouton orange du tuto
  - ecran: 5 | cherchais: les durées sous la carte (Intro 16, Drop 32…) | regarde: les nombres sont mangés par le bas du bandeau quand la consigne fait 3 lignes
abandon: jamais
incidents-banc:
  - geste: 66 | attente: 0 | vu: écran 67 en texte sans la consigne du chapitre 12 (« À toi : le mix… ») alors qu'elle est bien dessinée sur l'image
defauts:
  - geste: 22 | ecran.vu: 23 | cible: bouton « + Nouvelle partie (copie de C · Intro 2) » @ 272,588 | geste.type: touche
    attendu: juste après « Voilà ! À toi maintenant. », je touche le bouton que la démo vient de montrer et une partie est créée | obtenu: rien n'est créé, le panneau du tuto se rouvre par-dessus ; il faut le replier et toucher une deuxième fois
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 25 | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 25 | cible: bouton « + Nouvelle partie (copie de C · Intro 2) » | geste.type: touche
    attendu: refaire ce que « Montre-moi » a montré (la démo a fait apparaître « + D · Montée ») crée la partie « Montée » et valide l'étape 3/7 | obtenu: une partie « D » sans nom est créée, l'étape reste 3/7 ; j'ai dû Annuler puis toucher « Créer la partie « Montée » »
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 26 | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: texte « 16 » [coupé] @ 51,648 | geste.type: touche
    attendu: voir les durées en mesures sous chaque partie de la carte, comme à l'étape 2 | obtenu: quand la consigne fait 3 lignes, les nombres (16, 32, 24) et la ligne des noms sont coupés par le bas de la carte
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 8 | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: texte « 8 » [coupé] @ 100,642 | geste.type: touche
    attendu: savoir quelle partie est le bloc « 8 » (l'étape 6 parle de « deux Montées ») | obtenu: les blocs Intro 2 et Montée n'ont que « 8 », sans nom, en gris peu lisible ; j'ai dû deviner que c'étaient les Montées
    symptome: sans-nom | gravite: finition | revu: oui: ecran 14 | banc?: non | promesse: -
  - geste: 83 | ecran.vu: 84 | cible: texte « Reprendre le tuto » @ 147,990 | geste.type: touche
    attendu: le tuto est fini (« Tuto fini : bravo ! »), donc plus de « Reprendre » | obtenu: « Reprendre le tuto » est encore proposé à côté de « Recommencer le tuto », comme s'il restait quelque chose à faire
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 80 | ecran.vu: 81 | cible: texte « Exporte ton morceau Chapitre 13/13 · Étape 1/1 · Facile » @ 207,879 | geste.type: touche
    attendu: l'en-tête dit « Fait » comme aux chapitres 7 à 12 une fois fini | obtenu: l'en-tête reste « Étape 1/1 » alors que le bas dit « Tuto fini : bravo ! »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 82 | banc?: non | promesse: -
mots-faux:
defauts-avant:
