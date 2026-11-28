code testé : c9d48c185
joué par : sous-agent neuf
siege: a | profil: t8-ta-debutante-fr | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=ta-1 | texte: À toi : sur le pad « Bord de caisse », touche les 8 cases en pointillé (4 par mesure), entre les temps. | ecran: 41 | tenue: non | pourquoi: ch.4 ét.3 demande les notes de 2 mesures dans une consigne ; au ch.5 chaque consigne = un bouton OU une mesure (écrans 61, 62, 70, 71)
promesse: id=ta-2 | texte: À toi : sur le pad « Charley ouvert » (le « tchi »), touche les cases en pointillé : une entre chaq… | ecran: 6 | tenue: non | pourquoi: repliée, la 1re phrase est coupée en plein mot « chaq… » ; au ch.5 (écrans 62, 72) elle est entière
promesse: id=ta-3 | texte: Basse · mesure 1/8 Encore 2 notes en pointillé, plus haut ↑ Touche ici | ecran: 66 | tenue: oui | pourquoi: notes hors champ annoncées par ↑ + « Touche ici » qui m'y amène (aussi écran 71)
promesse: id=ta-4 | texte: ↑ Encore 4 notes en pointillé à … | ecran: 62 | tenue: non | pourquoi: tout en français, mais bandeau coupé (62), « Mesure 1/2 · Charley ouvert » coupé (6), « Motif posé ! La suite se copie : » à moitié sous la carte (70)
oeil-neuf: q1 | ecran: 79 | image: 79 | pistes, onglets, grille, bandeau « Bravo » ⌃ : tout ce qui a l'air touchable l'est ; « Acid 303 » déjà là à côté de « Basse » m'intrigue
oeil-neuf: q6 | ecran: 79 | image: 79 | preuve claire : « Toutes les notes en pointillé sont posées » + « Bravo, chapitre fini ! » ; la suite (chapitre 6) n'est pas montrée sans déplier ⌃
oeil-neuf: q9 | ecran: 79 | image: 79 | « En rythme » coupé au bord droit, « Piste » coupé derrière la flèche → en haut à droite
tuto: ch.4 | hésitation | écran 6 | j'ai pris ▶ pour « mesure suivante », il a décalé mes coups ; Annuler puis « Mesure 2 · 4 cases → » m'a remise sur les rails
tuto: ch.5 | compris | écran 72 | « Créer la piste Basse » seul, puis une mesure, puis Copier ; seul le compteur de notes m'a fait douter
defauts-avant:
  - D4/P7 | autre | ecran: 61 | au ch.5 « À toi : touche « Créer la piste Basse ». » est seul (61), puis « touche les notes en pointillé de la mesure 1 » (62) : séparé ; mais au ch.4 écran 41 une consigne demande 8 cases sur 2 mesures
  - D3 | autre | ecran: 6 | la carte du kit est sous la consigne dépliée (écran 1), la consigne repliée est lisible sur 2 lignes mais au ch.4 coupée en plein mot « une entre chaq… » ; au ch.5 (62, 72) entière
  - CTRL-a | corrigé | ecran: 66 | les notes hors champ ont « plus haut ↑ » et « Touche ici » qui fait défiler ; les autres sont à l'écran en entier (62, 67, 72)
hesitations:
  - ecran: 6 | cherchais: comment aller à la mesure 2 | regarde: ▶ à côté de « Remplir », il décale tout au lieu de changer de mesure
  - ecran: 62 | cherchais: où est le « La aigu » | regarde: le ↑ du bandeau, puis « Touche ici » (écran 66)
  - ecran: 72 | cherchais: combien de notes il reste | regarde: « Encore 5 » devenu « Encore 6 » sans que j'aie posé de note
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: le banc avait pré-écrit « code testé : e61e09917ddc3141f71d6b1f8723bb0978053175 » ; en-tête remplacé par celui du brief (c9d48c185)
defauts:
  - geste: 6 | ecran.vu: 7 | cible: texte « ▶ » @ 452,471 | geste.type: touche
    attendu: passer à la mesure 2 (je croyais à une flèche de mesure) | obtenu: tous mes coups décalés d'une case, devenus « barré : à enlever »
    symptome: incompris | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 41 | cible: texte « À toi : sur le pad « Bord de caisse », touche les 8 cases en pointillé (4 par mesure), entre les temps. » [coupé] | geste.type: touche
    attendu: une consigne = une mesure de notes | obtenu: 8 cases sur 2 mesures dans une seule consigne (idem ch.4 ét.1 et 2)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: ta-1
  - geste: 1 | ecran.vu: 6 | cible: icône « À toi : sur le pad « Charley ouvert » (le « tchi »), touche les cases en pointillé : une entre chaque coup de kick. Lire » ⌃ | geste.type: touche
    attendu: consigne repliée lisible, au moins sa 1re phrase entière | obtenu: coupée en plein mot « une entre chaq… »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: ta-2
  - geste: 61 | ecran.vu: 62 | cible: bouton « Basse · mesure 1/8 ↑ Encore 4 notes en pointillé à toucher Touche ici » | geste.type: touche
    attendu: bandeau lu en entier | obtenu: « ↑ Encore 4 notes en pointillé à … » coupé (aussi écran 71)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 71 | banc?: non | promesse: ta-4
  - geste: 71 | ecran.vu: 72 | cible: bouton « Basse · mesure 4/8 Encore 6 notes en pointillé à toucher » | geste.type: touche
    attendu: même nombre de notes restantes avant et après « Touche ici » | obtenu: « Encore 5 » (71) puis « Encore 6 » (72) ; au ch.5 ét.2 « Encore 4 » (62) alors qu'il en fallait 6
    symptome: texte-faux | gravite: finition | revu: oui: ecran 66 | banc?: non | promesse: -
  - geste: 69 | ecran.vu: 70 | cible: texte « Motif posé ! La suite se copie : » [coupé] | geste.type: touche
    attendu: message et bouton « Copier » visibles | obtenu: à moitié cachés sous la carte du tuto dépliée
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: ta-4
  - geste: 5 | ecran.vu: 6 | cible: texte « Mesure 1/2 · Charley ouvert » [coupé] | geste.type: touche
    attendu: titre de la mesure lisible | obtenu: coupé « Charley … » et glissé à moitié sous « Partie : A », très pâle
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 13 | banc?: non | promesse: ta-4
