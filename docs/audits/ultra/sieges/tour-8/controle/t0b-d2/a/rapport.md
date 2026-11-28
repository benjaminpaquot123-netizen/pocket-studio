code testé : a0d928cdb7be2961a852750dca8721b975ee8473
joué par : sous-agent neuf

siege: d | profil: leo-debutant | lang: fr | device: telephone | font: 100 | mode: simple
mission: tuto-facile | buts: -
chapitres: 12=hésitation (étape 1/3 en cours, siège arrêté par le banc au geste 13), 3=pas atteint
chapitres bloqués: 0 | graves neufs: 0
oeil-neuf: q1 | ecran: 13 | image: 13 | cases de la grille, Mesure 1-4, « Suivre », « Chapitres », « Lire », onglets : touchables et ont l'air touchables ; les noms de lignes (« Grosse c… ») ressemblent à du texte, je ne sais pas s'ils se touchent
oeil-neuf: q6 | ecran: 13 | image: 13 | après avoir effacé 2 coups, la vue « Tous les pads » ne dit pas combien de croix il reste (la vue d'un pad disait « 1 coup en trop ici ») ; la barre du tuto ne bouge pas, je ne sais pas si j'avance
oeil-neuf: q9 | ecran: 13 | image: 13 | noms de lignes coupés : « Grosse c… », « Claquem… », « Charley … », « Tom méd… », « Bord de … »
hesitations:
  - ecran: 7 | cherchais: la vue avec tous les pads | regarde: il a fallu remonter la page au-dessus de « Suivre » pour trouver « Tous les pads » (caché sous les pads au départ)
  - ecran: 13 | cherchais: quelles croix rouges il reste | regarde: dans « Tous les pads » les croix sont aussi sur « Charley ouvert » et « Bord de caisse », la consigne ne parle que de « la dernière mesure »
abandon: jamais
incidents-banc:
  - geste: 13 | attente: 0 | vu: le banc a fermé le siège « rien ne change (5 écrans identiques) » ; c'est ma faute : j'ai fait des « attends » et des petits glissés sans effet pour regagner le droit à une image (plafond 1 image / 5 gestes) ; mission coupée au ch.12 étape 1/3
  - geste: 13 | attente: 0 | vu: outil hors banc utilisé une fois : agrandissement (recadrage) de l'image 13 pour voir les croix, fichier écrit dans le dossier brouillon de la session (hors siège)
defauts:
  - geste: 1 | ecran.vu: 2 | cible: icône « Intro mesure 1/96 » ▾ | geste.type: touche
    attendu: le bandeau du haut dit où je suis (partie Montée) | obtenu: il dit « Intro mesure 1/96 » alors que juste en dessous c'est « Partie : C · Montée » (et « Partie : G · Fin » à l'écran 1)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 13 | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 13 | cible: texte « Grosse caisse 1 » @ 96,722 | geste.type: touche
    attendu: le nom de chaque son en entier dans la vue « Tous les pads » | obtenu: « Grosse c… », « Claquem… », « Charley … », « Bord de … » : deux lignes « Grosse c… » impossibles à distinguer
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 8 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 2 | cible: texte « Longueur » [coupé] @ 89,819 | geste.type: touche
    attendu: les réglages du pad visibles | obtenu: la ligne « Longueur » (1 mesure / 2 / 3 / 4) passe sous la carte du tuto, coupée
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 13 | cible: texte « Charley » @ 96,566 | geste.type: touche
    attendu: une croix rouge nette sur chaque case à effacer | obtenu: dans les cases étroites de « Tous les pads », la croix est écrasée en petit dessin rouge (arcs + barres), on devine « à enlever » mais on ne reconnaît pas bien une croix
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - a-V1-anneau | pas atteint | ecran: 13 | siège arrêté au ch.12, chapitre 3 jamais ouvert
  - d-V2 | pas atteint | ecran: 2 | la partie « Montée » existe et a des coups (pas vide) ; le message « Il n'y a pas de partie « Montée » » n'est pas apparu, mais le cas « existe et vide » n'a pas été rencontré
  - c-croix | pas atteint | ecran: 13 | vue « Tous les pads » (image 13) : croix rouges visibles sur Charley, Charley ouvert, Bord de caisse (petites, écrasées) ; cases en pointillé pas vues ; vue d'un pad : texte « barré : à enlever » (écran 2-3) mais pas d'image prise, donc pas jugée
