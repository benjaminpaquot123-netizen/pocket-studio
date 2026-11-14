siege: a | profil: t6-Ca-oeil-neuf-tablette-130 | lang: fr | device: tablet8 | font: 130 | mode: pro
mission: libre | buts: son, trace, partage
oeil-neuf: 14 écrans, 3 graves
hesitations:
  - ecran: 7 | cherchais: comment couper la batterie | regarde: le carré haut-parleur « Son » sans mot clair ; il coupe la piste, je l'ai su en le touchant
  - ecran: 12 | cherchais: ce que veulent dire les pastilles des pads | regarde: « FX », « G 30 », « D 45 », « D 50 » sans aucune explication à l'écran
  - ecran: 25 | cherchais: les curseurs de volume des pistes en Pro | regarde: l'onglet Mix s'ouvre sur l'export ; les tranches sont plus haut, rien ne le signale
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 900 | vu: « L'appli ne s'est pas ouverte (15 min) » deux fois : le banc du lot (human-tester.sh lotC) n'était pas lancé sur ma machine ; relancé par le coordinateur, puis tout a répondu
  - geste: 0 | attente: 0 | vu: outils hors human-act.py : lecture des images et de mes fichiers dans mon dossier de siège, écriture de oeil-neuf.tsv et rapport.md (printf), ps et free une fois pour voir si le banc tournait
defauts:
  - geste: 12 | ecran.vu: 13 | cible: bouton [état « vide »] @ 192,1065 | geste.type: touche
    attendu: la case touchée s'allume tout de suite à l'endroit où j'ai touché | obtenu: la case reste vide à l'écran, la grille est passée à la mesure 3 (« Suivre » allumé) ; le coup n'apparaît qu'en coupant « Suivre » et en revenant à la mesure 1
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 16 | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 25 | cible: bouton « Auto (House) · −8 LUFS » [sélectionné] | geste.type: touche
    attendu: l'onglet Mix (Pro) montre les tranches de volume, comme en Simple | obtenu: la page s'ouvre en bas, sur le volume visé et l'export ; aucune tranche visible, aucun signe qu'il faut remonter ; un glissé ne suffit pas
    symptome: cache | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: -
  - geste: 16 | ecran.vu: 17 | cible: bouton « Batterie ▸ » [état « fermé »] | geste.type: touche
    attendu: les noms des pistes lisibles en entier à 130 % | obtenu: noms coupés au milieu du mot : « Batteri / e ▸ », « Mélodi / e », « Souffl / e mo… »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 16 | ecran.vu: 17 | cible: texte « Chute grave » | geste.type: touche
    attendu: un signe que la rangée de tranches continue à droite | obtenu: la dernière tranche est coupée par le bord, rien n'indique qu'on peut faire glisser
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: bouton ▶ « cette partie » | geste.type: touche
    attendu: le gros bouton du haut montre que ça joue | obtenu: il montre ▶ (lecture) pendant que le morceau joue ; seul le ■ vert du bas dit que ça joue
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 24 | cible: texte « Lead » | geste.type: touche
    attendu: la même piste garde son nom en passant en Pro | obtenu: « Mélodie » en Simple devient « Lead » en Pro ; « Chute grave » devient « Sub drop » (écran 27)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 27 | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: bouton « Kick studio FX » | geste.type: touche
    attendu: les mêmes pads gardent leur nom entre Simple et Pro | obtenu: « Grosse caisse studio », « Claquement de mains », « Cloche », « Laser » (Simple, écran 12) deviennent « Kick studio », « Clap », « Cowbell », « Zap »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 12 | cible: bouton « Charley G 30 » | geste.type: touche
    attendu: savoir ce que veulent dire « G 30 », « D 45 », « FX » sur les pads | obtenu: aucune explication à l'écran
    symptome: sans-aide | gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: bouton (haut-parleur barré) [état « désactivé »] | geste.type: touche
    attendu: un mot qui dit que le bouton coupe la piste | obtenu: un carré haut-parleur marqué « Son », à côté de « Écouter seule » ; on comprend seulement après l'avoir touché
    symptome: sans-nom | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 26 | ecran.vu: 27 | cible: texte « 30.2 » | geste.type: touche
    attendu: savoir ce que compte le cercle « 30.2 » en haut de Live | obtenu: un nombre sans nom ni unité
    symptome: sans-nom | gravite: finition | revu: non | banc?: non | promesse: -
