code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: moyen | lang: fr | device: phone360 | font: 100 | mode: simple
mission: tuto-facile | buts: chapitres 1-8 du tuto afrobeats (aucun id donné par le brief)
chapitres:
  - ch1 La vitesse et la couleur du morceau : hésitation (le contour des boutons « entourés » ne se voit que sur l'image, écran 10)
  - ch2 Le shaker d'abord : compris du premier coup
  - ch3 Le kick et le rim : hésitation (la feuille dit « Caisse » alors que le texte dit « Bord de caisse », écran 34)
  - ch4 Les congas : compris du premier coup, mais mes notes sont parties sur le mauvais pad (écran 54)
  - ch5 La basse : hésitation (« 5 notes » contre « Encore 3 », et Copier à toucher deux fois)
  - ch6 Les accords : hésitation (Copier à toucher deux fois, encore)
  - ch7 La mélodie : hésitation (le compteur de notes se trompe, écrans 98 et 100)
  - ch8 Ta première partie : le Refrain : compris du premier coup (Fait, écran 116)
hesitations:
  - ecran: 10 | cherchais: les « deux boutons entourés » | regarde: le texte de l'écran ne montre aucun contour, il a fallu l'image
  - ecran: 34 | cherchais: le pad « Bord de caisse » demandé | regarde: la feuille ouverte s'appelle « Mesure 1/2 · Caisse »
  - ecran: 74 | cherchais: pourquoi Copier n'a rien fait | regarde: j'ai attendu, puis retouché
abandon: jamais
incidents-banc:
defauts:
  - geste: 45 | ecran.vu: 54 | cible: « Pas 13 » | geste.type: touche
    attendu: le texte dit pad « Conga », mes notes vont sur Conga | obtenu: la feuille est « Bord de caisse » ; sur la grille, Conga n'a aucune note
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: « Pas 13 » | geste.type: touche
    attendu: le texte dit pad « Bord de caisse », la feuille ouverte est ce pad | obtenu: la feuille ouverte s'appelle « Mesure 1/2 · Caisse », mes notes vont sur Caisse
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 52 | ecran.vu: 53 | cible: « Mesure 2/2 · Bord de caisse » | geste.type: retour
    attendu: Retour ferme la feuille des pas et me laisse dans le Rythme | obtenu: je me retrouve à l'accueil « Que veux-tu faire ? », il faut « Continuer »
    symptome: mauvais-ecran | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 73 | ecran.vu: 74 | cible: «   Copier sur les mesures suivantes » | geste.type: touche
    attendu: un toucher copie la basse sur les mesures suivantes | obtenu: rien ne change ; il faut toucher une deuxième fois
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 89 | banc?: non | promesse: -
  - geste: 97 | ecran.vu: 98 | cible: « Encore 5 notes en pointillé, plus haut ↑ » | geste.type: touche
    attendu: après 4 notes posées sur 5, le bandeau dit « Encore 1 note » | obtenu: le bandeau dit « Encore 5 notes en pointillé »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 99 | ecran.vu: 100 | cible: « Encore 4 notes en pointillé, plus loin → » | geste.type: touche
    attendu: l'étape affiche « Réussi », plus rien à poser | obtenu: le bandeau réclame encore 4 notes plus loin
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 65 | ecran.vu: 66 | cible: «   Créer la piste Basse » | geste.type: touche
    attendu: le texte « touche les 5 notes en pointillé » et le bandeau donnent le même nombre | obtenu: le bandeau dit « Encore 3 notes », et des notes sont hors de vue (« plus haut ↑ »)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: « Suivant  » | geste.type: touche
    attendu: j'ai écouté presque tout l'exemple, je passe sans reproche | obtenu: « Exemple passé sans l'écouter. » (revu au geste 77, écran 78)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 78 | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 56 | cible: « Télécharger le kit Afrobeats (2 Mo) » | geste.type: touche
    attendu: le kit se télécharge et le kit devient Afrobeats | obtenu: « Téléchargement du kit… 0 % » puis disparaît sans message, le kit reste « Studio »
    symptome: perdu | gravite: finition | revu: non | banc?: temps | promesse: -
mots-faux:
  - ecran: 18 | vu: Charley (feuille ouverte par « Je le fais ») | attendu: shaker / Maracas
  - ecran: 15 | vu: le kit de l'afrobeats est déjà chargé | attendu: Kit : Studio (le kit Afrobeats n'est pas chargé)
  - ecran: 62 | vu: Kick (expliqué seulement au chapitre 5) | attendu: Grosse caisse, ou Kick expliqué dès le chapitre 3
defauts-avant:
fin-tranche: non (dernier step=groove_name, done=20)
