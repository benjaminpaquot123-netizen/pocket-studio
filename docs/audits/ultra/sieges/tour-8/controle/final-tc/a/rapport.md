code testé : dd43d00f5e4c43d57a40995d84e8048989bf1b7b
joué par : sous-agent neuf
siege: tc | profil: leo-debutant-presse | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chapitre 12 house jusqu'au « Bravo »
arrivee: écran 1 Rythme, « Partie : G · Fin », carte « Chapitre 12/15 · Étape 1/3 » ; parties existantes (écran 2) : A · Groove, B · Intro, C · Montée, D · Drop, E · Break, F · Drop 2, G · Fin
etapes: étape 1 (Montée, effacer les coups barrés) compris du premier coup, validée écran 12 | étape 2 (cymbale dans le Drop) compris du premier coup, validée écran 15 | étape 3 (4 maracas dans le Drop 2) compris, mais « Bravo » dès le 1er coup (écran 21)
chapitres: chapitre 12 | compris
promesse: id=t8-tc-1 | texte: Une étape n'est validée (« Bravo », « Déjà fait », barre qui avance) que quand j'ai vraiment fait ce qu'elle demande | ecran: 21 | tenue: non | pourquoi: étape 3 demande 4 maracas, « Bravo, chapitre fini ! » après 1 seul coup (écran 21) ; revu écran 30 et 36 : « Déjà fait ! » avec 1 coup sur 4
promesse: id=t8-tc-2 | texte: « Suivant » ne saute jamais une étape sans dire ce qui reste | ecran: 7 | tenue: oui | pourquoi: « Cette étape n'est pas encore faite. Touche encore « Suivant » pour la passer quand même. » et la grille dit « 2 coups en trop ici (barrés) »
promesse: id=t8-tc-3 | texte: Les parties et pistes que le tuto cite existent à l'écran sous le même nom, ou le tuto me propose de les créer | ecran: 17 | tenue: oui | pourquoi: Montée, Drop, Drop 2, Charley, Cymbale, Maracas trouvés sous ces noms (écrans 4, 14, 17)
promesse: id=t8-tc-4 | texte: tous les textes vus sont en français, aucun coupé sans « … » | ecran: 39 | tenue: oui | pourquoi: tout en français ; la consigne longue a « Lire la suite » (écran 6) ; seuls des boutons cachés sous la carte (« Suivre », « Longueur »), pas de mot coupé
oeil-neuf: q1 | ecran: 39 | image: 39 | boutons nets (Chapitre suivant, ←, Écouter mon morceau, Écouter l'exemple, Télécharger le kit) ; « Plus tard » et « Pourquoi ? » sont du texte seul, on ne devine pas qu'ils se touchent
oeil-neuf: q6 | ecran: 39 | image: 39 | la preuve contredit : la grille montre 1 seul coup de maracas, la carte dit « Déjà fait ! … rien à toucher » et propose « Chapitre suivant »
oeil-neuf: q9 | ecran: 39 | image: 39 | aucun mot coupé ; la carte recouvre la moitié de la grille (rangée « 3 » et « 4 » cachée)
defauts-avant:
  - D8 | pas atteint | ecran: - | la Montée existait déjà en arrivant (« C · Montée » dans la liste des parties, écran 2) : condition non réunie
  - G12-1 | corrigé | ecran: 12 | « Aller dans la partie « Montée » » m'amène dans C · Montée (écran 4) sans valider ; Suivant refusé (écran 7) ; « Réussi Étape suivante » seulement après avoir effacé les 5 coups barrés (charley, charley ouvert, bord de caisse), écran 12
  - G12-2 | corrigé | ecran: 14 | la case en pointillé « Encore 1 case en pointillé ici » est dans « Partie : D · Drop », pas dans A
  - G12-3 | autre | ecran: 21 | plus de « pas encore faite », mais c'est l'inverse : « Bravo, chapitre fini ! » dès le 1er coup de maracas sur les 4 demandés ; « Montre-moi » (écrans 19-20) n'a laissé aucun coup visible, je n'ai pas vu où il les pose
  - M2 | pas atteint | ecran: - | « Poser ce qui manque » jamais apparu : cherché dans la carte aux étapes 1 à 3 (deux passages, écrans 1-36, même avec 3 maracas manquantes), « Pourquoi ? » (écran 37), menu Plus (écran 38), liste des chapitres (écrans 32-33)
hesitations:
  - ecran: 1 | cherchais: si la Montée existe | regarde: le bouton « Aller dans la partie « Montée » » a un ⊕, j'ai cru qu'il en créait une nouvelle alors que C · Montée existe
  - ecran: 14 | cherchais: le pad de la cymbale | regarde: le titre du pad est remplacé par « Encore 1 case en pointillé ici » ; le nom « Cymbale » n'apparaît qu'après avoir touché (écran 15)
  - ecran: 20 | cherchais: où « Montre-moi » pose les maracas | regarde: la démo finit avec une grille vide, j'ai deviné les temps 1 à 4
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun incident ; outils hors human-act.py : lecture du brief, cat/grep/sed sur les fichiers du siège (screen-N.txt, journal.txt) et un fichier temporaire dans le scratchpad, Read des images -grille
defauts:
  - geste: 20 | ecran.vu: 21 | cible: bouton « 1 » [état « vide »] @ 70,380 | geste.type: touche
    attendu: l'étape 3 (« touche 4 cases, une par temps de la mesure 1 ») attend les 4 coups | obtenu: « Bravo, chapitre fini ! » après 1 seul coup
    symptome: incompris
    gravite: contourne-en-4-gestes | revu: oui: ecran 36 | banc?: non | promesse: t8-tc-1
  - geste: 29 | ecran.vu: 30 | cible: bouton « 4 » [état « coup, force 100 % »] @ 325,460 | geste.type: touche
    attendu: en enlevant 3 maracas sur 4, l'étape redit ce qui manque | obtenu: « Déjà fait ! » « Ton morceau avait déjà ce que demande cette étape : rien à toucher. » avec 1 coup sur 4
    symptome: texte-faux
    gravite: contourne-en-3-gestes | revu: oui: ecran 36 | banc?: non | promesse: t8-tc-1
  - geste: 25 | ecran.vu: 26 | cible: icône ← @ 57,975 | geste.type: touche
    attendu: ← depuis le « Bravo » revient à l'étape 3 | obtenu: saute à « Étape 2/3 » (Drop 2, pad Cymbale vide, « Déjà fait ! »)
    symptome: mauvais-ecran
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: bouton « 12 Les transitions » (rond coché) @ 294,711 | geste.type: touche
    attendu: étape 1 rouverte dit que je l'ai déjà faite | obtenu: « Ton morceau avait déjà ce que demande cette étape : rien à toucher. » alors que c'est moi qui ai effacé les coups (écran 12)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
fin
