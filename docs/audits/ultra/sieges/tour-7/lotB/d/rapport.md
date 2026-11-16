code testé : 28f1260707fe98b9322b638b5eb0f4db44c66028
joué par : sous-agent neuf
siege: d | profil: t7-2d | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t7-2d-1 | texte: « Simple ou Pro ? » | ecran: 2 | tenue: non | pourquoi: titre en haut puis grand vide sombre de y≈80 à y≈650 (image 2), identique écrans 25 et 34 (image 25), même après « attends 5 » (écran 26)
promesse: id=t7-2d-2 | texte: « Plus tard » | ecran: 7 | tenue: non | pourquoi: au chapitre 1 la carte du kit et « Plus tard » n'apparaissent qu'après deux touches sur « Lire la suite » (écrans 4→6→7)
promesse: id=t7-2d-3 | texte: « Quitter le tuto (tu pourras le reprendre) » | ecran: 21 | tenue: non | pourquoi: aucun 2e Guide vu (écrans 4 à 56), mais la sortie du tuto est un texte gris sans contour, pas un bouton (image 21)
promesse: id=t7-2d-4 | texte: « • Drop : le moment fort du morceau, quand tout revient d'un coup. » [coupé] | ecran: 4 | tenue: non | pourquoi: tout est en français, mais des lignes du tuto sont coupées par le bas du panneau (écrans 4, 6, 7, 19)
oeil-neuf: q1 | ecran: 56 | image: 56 | les cases en pointillé, Remplir tout/1/2/1/4, « Lire ^ » et les onglets ont l'air touchables ; « Remplir » et « Effacer » (gris) sont du texte, on hésite
oeil-neuf: q6 | ecran: 56 | image: 56 | preuve claire : « Encore 3 cases en pointillé ici » et la barre de progression ; la suite du geste (appui long, Doux) est cachée sous « Lire ^ »
oeil-neuf: q9 | ecran: 56 | image: 56 | la consigne du clap est coupée à deux lignes « touche les cases en pointillé : les temps 2 et … »
hesitations:
  - ecran: 19 | cherchais: « Fermer le tuto » dans le panneau | regarde: rien dans le panneau ni sous « Lire la suite » ; trouvé dans « Chapitres », en bas, écrit « Quitter le tuto »
  - ecran: 23 | cherchais: « Que veux-tu faire ? » | regarde: le dossier mène à « Mes projets » ; trouvé seulement par Retour depuis « Mes projets » (écran 32)
  - ecran: 4 | cherchais: la fin de la consigne | regarde: « Lire la suite » fait défiler le texte au lieu de l'ouvrir en entier
abandon: ecran 60 | parce que budget de 60 gestes atteint au milieu du chapitre 3 (il restait « Doux »)
incidents-banc:
  - geste: 42 | attente: 0 | vu: écran 43 en texte vide (seulement la barre « Mode retours »), l'image montre bien la grille ; idem gestes 48 et 55
  - geste: - | attente: 0 | vu: outils hors human-act : lecture des images et de cmd-N.txt / rapport.md de mon dossier de siège (ls, cat, Read), rien d'autre
defauts:
  - geste: 1 | ecran.vu: 2 | cible: texte « Simple ou Pro ? » | geste.type: touche
    attendu: titre, texte et cartes rangés en haut | obtenu: grand vide sombre sur la moitié haute, je crois que l'écran charge encore
    symptome: incompris | gravite: finition | revu: oui: ecran 25 et 34 | banc?: non | promesse: t7-2d-1
  - geste: 5 | ecran.vu: 7 | cible: texte « Plus tard » [coupé] | geste.type: touche
    attendu: carte du kit et « Plus tard » visibles dès l'étape 1 | obtenu: il faut toucher deux fois « Lire la suite », et « Plus tard » reste coupé
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 20 | banc?: non | promesse: t7-2d-2
  - geste: 20 | ecran.vu: 21 | cible: texte « Quitter le tuto (tu pourras le reprendre) » | geste.type: touche
    attendu: un bouton « Fermer le tuto » dans le panneau | obtenu: texte gris sans contour, caché dans « Chapitres »
    symptome: sans-nom | gravite: finition | revu: oui: ecran 21 (image) | banc?: non | promesse: t7-2d-3
  - geste: 3 | ecran.vu: 4 | cible: texte « • Drop : le moment fort du morceau, quand tout revient d'un coup. » [coupé] | geste.type: touche
    attendu: ligne entière | obtenu: ligne coupée par le bas du panneau (image 4)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 19 (bouton du kit coupé) | banc?: non | promesse: t7-2d-4
  - geste: 55 | ecran.vu: 56 | cible: texte « Lire ^ » / « touche les cases en pointillé : les temps 2 et … » | geste.type: touche
    attendu: consigne du clap lisible pendant que je touche les cases (appui long, « Doux ») | obtenu: réduite à deux lignes, la partie « appui long… Doux » est cachée
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: bouton « Phrygienne » | geste.type: touche
    attendu: le bouton se montre choisi | obtenu: pas marqué [sélectionné] ; seule la ligne « Tonalité du morceau : Do · Phrygienne » change
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: bouton « 11 Range tes parties 0/2 » [coupé] | geste.type: touche
    attendu: un signe que la liste continue (15 chapitres) | obtenu: liste coupée au chapitre 11, rien n'indique la suite
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 32 | ecran.vu: 33 | cible: texte « Nouveau projet » | geste.type: touche
    attendu: même mot que le bouton touché « Nouveau morceau » | obtenu: l'écran s'appelle « Nouveau projet »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 23 | cible: texte « Chargement de la liste… » | geste.type: touche
    attendu: mon projet dans la liste | obtenu: « Chargement de la liste… » reste (écrans 23, 30, 31)
    symptome: lent | gravite: finition | revu: oui: ecran 31 | banc?: temps | promesse: -
defauts-avant:
  - retour Benjamin 2026-11-14 (grand vide) | pas corrigé (écran 2) : titre puis grand vide sombre jusqu'à y≈650, cartes en bas ; idem écrans 25 et 34
  - T7 | pas corrigé (écran 7) : carte du kit et « Plus tard » visibles seulement après deux « Lire la suite »
  - T13 | corrigé (écran 18) : du chapitre 1 au chapitre 3 (écrans 4 à 56), aucun Guide ne s'ouvre par-dessus le tuto
  - N13 | pas corrigé (écran 21) : « Quitter le tuto (tu pourras le reprendre) » est un texte gris sans contour
  - G1 | pas vu
  - T6 | corrigé (écran 14) : kick, drop, temps, tempo/BPM, phrygienne, gamme expliqués ; seul « Tonique » (écran 15) n'a pas d'explication sur la fiche
