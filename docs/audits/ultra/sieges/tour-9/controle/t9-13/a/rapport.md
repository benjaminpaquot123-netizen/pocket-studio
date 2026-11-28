code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: confirme | lang: nl | device: phone360 | font: 130 | mode: pro
mission: tuto-pro boom_bap ch1-4 | buts: ch1 klaar, ch2 klaar (stap 5 overgeslagen), ch3 klaar, ch4 klaar (132 gestes)
chapitres: ch1 compris du premier coup | ch2 bloqué (stap 5/8 impossible, sautée avec Volgende ×2) | ch3 compris du premier coup | ch4 hésitation (1er ‘Kopiëren’ sans effet)
promesse: id=p1 | texte: Goed gedaan, het is gelukt! | ecran: 9 | tenue: oui | pourquoi: le kit téléchargé, l'étape a validé
promesse: id=p2 | texte: de twee (zachte) ghost notes van maat 2 | ecran: 35 | tenue: non | pourquoi: les cases pointillées tapées deviennent « slag, kracht 100 % », pas douces
promesse: id=p3 | texte: dan ‘Verschuiving van slag’ op +10 ms | ecran: 75 | tenue: non | pourquoi: la feuille du coup n'a pas ce réglage
promesse: id=p4 | texte: Kopiëren naar de volgende maten | ecran: 119 | tenue: oui | pourquoi: maten 2-3 de la basse écrites, chapitre 3 klaar
hesitations:
  - ecran: 22 | cherchais: aller à la maat 2 | regarde: la rangée « Maat 1 2 » à moitié cachée, trouvé le texte « Maat 2 · 4 vakjes → »
  - ecran: 65 | cherchais: le pad Snare demandé | regarde: la grille montrait le Hi-hat, il a fallu remonter et toucher le pad
  - ecran: 61 | cherchais: mettre le swing à 27 % | regarde: le curseur ne glisse pas, deux touches pour tomber sur 28 %
abandon: jamais (stap 5 du ch2 sautée, pas abandonnée)
incidents-banc:
  - geste: 3 | attente: 0 | vu: « regarde image » refusée (quota d'images)
  - geste: 61 | attente: 0 | vu: glisse sur le curseur swing sans effet (touche marche) ; idem glisse #65 #71 #72 #76 #77 #82 pour défiler
  - geste: - | attente: 0 | vu: hors banc j'ai lu les 2 gabarits (consigne), ls/cat du dossier du siège et ouvert les PNG grille du siège
defauts:
  - geste: 74 | ecran.vu: 75 | cible: Kans om te spelen | geste.type: appui-long
    attendu: la feuille du coup de snare propose ‘Verschuiving van slag’ pour régler +10 ms | obtenu: seulement Kracht / Roll / Triool / Kans om te spelen, la feuille ne défile pas, « Laat zien » ne montre rien de plus ; étape impossible, sautée
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 82 | banc?: non | promesse: p3
  - geste: 64 | ecran.vu: 65 | cible: Maat 2/2 · Akai MPC60 · Hi-hat | geste.type: touche
    attendu: « Ik doe het » ouvre la grille du pad ‘E-mu SP-1200 · Snare’ nommé dans la carte | obtenu: la grille du Hi-hat ; remonter, toucher le pad Snare, redescendre, redemander ≈ 8 gestes
    symptome: mauvais-ecran | gravite: contourne-en-8-gestes | revu: oui: ecran 74 | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 35 | cible: slag, kracht 100 % | geste.type: touche
    attendu: les ghost notes « (zachte) » entrent douces | obtenu: pleine force 100 %, comme les coups normaux (idem petites notes du hi-hat)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: p2
  - geste: 18 | ecran.vu: 26 | cible: Maat 1 2 Volgen | geste.type: touche
    attendu: le choix des mesures bien visible | obtenu: la rangée est coupée, presque cachée sous « Deel: A » (police 130 %)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: -
  - geste: 108 | ecran.vu: 109 | cible: Bas · maat 1/8 Nog 3 gestippelde noten om aan te tikken | geste.type: touche
    attendu: un piano roll où l'on voit les notes | obtenu: bande d'environ 50 px entre l'en-tête et la carte du tuto, notes du haut/bas coupées (pareil pour les accords, écran 126)
    symptome: cache | gravite: finition | revu: oui: ecran 126 | banc?: non | promesse: -
  - geste: 130 | ecran.vu: 131 | cible: Kopiëren naar de volgende maten | geste.type: touche
    attendu: les accords copiés et l'étape validée au premier toucher | obtenu: rien ne change ; il faut toucher une deuxième fois pour « Klaar »
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 132 | banc?: non | promesse: -
  - geste: 113 | ecran.vu: 114 | cible: Bas · maat 4/8 ↑ Nog 2 gestippelde noten om aan te tikken Tik hier | geste.type: touche
    attendu: « de 3 noten van maat 4 » comme dit la carte | obtenu: le bandeau annonce 2 notes (la 3e hors vue) jusqu'à « Tik hier »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 115 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 26 | cible: Ongedaan maken | geste.type: touche
    attendu: étiquette de la barre du bas à la taille de ses voisines | obtenu: « Ongedaan maken » (et « Hoofdstukken » de la carte) écrits beaucoup plus petit que « Opnieuw », « Tempo »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 109 | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: 92  BPM | geste.type: touche
    attendu: voir la puce BPM pendant l'étape tempo | obtenu: carte du tuto dépliée la cache
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 35 | vu: ghost notes | attendu: spooknoten
  - ecran: 65 | vu: Druk op de pad … lang op elke harde slag | attendu: Houd op de pad … elke harde slag ingedrukt
fin-tranche: non (dernier step=pad_end, done=16)
