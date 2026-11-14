siege: a | profil: t6-Aa debutant-indien-petit-tel | lang: hi | device: phone360 | font: 130 | mode: simple
mission: simple | buts: son, partie, jamais-pro
promesse: id=t6-Aa-1 | texte: Je change de partie et c'est bien la partie touchée qui s'affiche et se joue | ecran: 39 | tenue: oui | pourquoi: à l'arrêt « C · मुखड़ा » touché → en-tête « मुखड़ा », partie C joue (écrans 14 et 39) ; en lecture, la partie choisie passe « अगला » puis joue et s'affiche (écran 45)
promesse: id=t6-Aa-2 | texte: « ▶ यह पार्ट » / « ▶ गाना » | ecran: 40 | tenue: oui | pourquoi: « यह पार्ट » boucle la partie affichée (« मुखड़ा बार 2/8 », écrans 15 et 40) ; « गाना » relance le morceau entier (« बार 25/104 », écran 16)
promesse: id=t6-Aa-3 | texte: नया गाना → Pop | ecran: 36 | tenue: oui | pourquoi: le 2e morceau s'ouvre directement en « सिंपल », sans l'écran « सिंपल या प्रो? »
promesse: id=t6-Aa-4 | texte: टेस्ट चेकलिस्ट | ecran: 19 | tenue: oui | pourquoi: la tuile « टेस्ट चेकलिस्ट » est dans « और » en Mode simple et ouvre la Recette (écran 20), mais seulement après défilement
oeil-neuf: q1 | ecran: 45 | image: 45 | les lignes de partie (▶, −, +, ⋮) et les onglets se voient touchables ; les « + » grisés des lignes « मिक्स » ressemblent aux « + » actifs, sans raison dite
oeil-neuf: q6 | ecran: 45 | image: 45 | preuve claire : la ligne jouée est verte « बज रहा है », la suivante jaune « अगला », l'en-tête dit « अंतरा बार 5/100 » ; la suite (le guide) est repliée en bas
oeil-neuf: q9 | ecran: 45 | image: 45 | à 130 % « F · प्री-कोरस 2 » passe sur deux lignes (le « 2 » seul) ; la ligne jouée est coupée en haut par l'en-tête
hesitations:
  - ecran: 7 | cherchais: comment changer de partie | regarde: le menu « यहाँ से बजाओ… » sous « इंट्रो ▾ » ; rien ne bouge tout de suite si le morceau joue
  - ecran: 17 | cherchais: la Recette dans « और » | regarde: le guide ouvert cache la moitié des tuiles ; il faut le replier puis faire défiler
  - ecran: 31 | cherchais: un bouton pour ajouter une partie | regarde: la liste des parties, je n'en ai pas trouvé, je me suis contenté de changer de partie
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: avant le jeu, pour mettre le banc en route sur cette machine : selftest.sh du lotA relancé (vert, 173 tests), human-tester.sh lancé, fiches start.txt de lotA/b et lotA/c mises de côté (consigne de la discussion principale) ; Read des images et lecture de journal.txt dans mon dossier de siège seulement
defauts:
  - geste: 7 | ecran.vu: 11 | cible: texte « 7 · अंतरा » | geste.type: touche
    attendu: « यहाँ से बजाओ… » (joue à partir d'ici) : saut immédiat à « अंतरा », comme quand le morceau est arrêté (écran 14) | obtenu: l'en-tête reste « इंट्रो बार 3/104 » puis « बार 6/104 » ; le saut n'arrive qu'à la fin de la partie, signalé seulement par « अगला » dans la liste, hors de vue
    symptome: incompris | gravite: finition | revu: oui: ecran 44 | banc?: non | promesse: t6-Aa-1
  - geste: 16 | ecran.vu: 17 | cible: bouton (boussole) « गाइड  · एक साउंड बदलो 15 % » ⌄ [état « खुला »] | geste.type: touche
    attendu: le panneau « और » montre toutes ses tuiles | obtenu: le guide ouvert recouvre le bas du panneau (Live et Remix coupés, le reste caché) ; il faut le replier et faire défiler pour trouver « टेस्ट चेकलिस्ट »
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 19 | banc?: non | promesse: t6-Aa-4
  - geste: 19 | ecran.vu: 20 | cible: texte « Banc fiable : les 10 essais restants (8 faits sur le téléphone, merci). Chaque point dit le mode, les gestes un par un, et la réponse à recopier. » | geste.type: touche
    attendu: la Recette en hindi | obtenu: le texte et « नया क्या है » sont en français (« Recette, deuxième fournée… ») dans une appli en हिन्दी
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 43 | vu: Post-chorus | attendu: पोस्ट-कोरस
  - ecran: 29 | vu: Fade in / Fade out / Ending जोड़ो | attendu: mots hindi (ou au moins translittérés)
