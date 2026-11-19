code testé : c9d48c1855fdfd2943731ecdf1d4a7322b9b8474
joué par : sous-agent neuf
siege: a | profil: t8-td | lang: ja | device: foldable | font: 200 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t8-td-1 | texte: 「次の小節にコピー」、次に4小節目を G♭ メジャー（♭II）で書いて、もう一度「コ… 読む | ecran: 1 | tenue: oui | pourquoi: la consigne repliée finit par « … » et 続きを読む/読む montre tout (écrans 2, 104)
promesse: id=t8-td-2 | texte: (panneau du tuto déplié) あなたの番：… 自分でやる | ecran: 42 | tenue: non | pourquoi: déplier la consigne recouvre toute la grille (écrans 2, 42, 104) ; il faut 自分でやる pour la revoir
promesse: id=t8-td-3 | texte: « 4拍目に » rendu « 4拍目l… » | ecran: 39 | tenue: non | pourquoi: tout est en japonais, mais des textes sont coupés (dernier caractère avant « … » à moitié dessiné, noms de touches du piano roll tronqués, compteur « あと … »)
hesitations:
  - ecran: 43 | cherchais: le pad シンバル demandé par la consigne (ch. 4) | regarde: il était sous le panneau, j'ai dû deviner et faire défiler la zone des pads
  - ecran: 95 | cherchais: la bonne ligne de note du piano roll (ch. 6) | regarde: la grille défile et zoome toute seule après chaque toucher, mes touchers suivants tombent sur une autre ligne
  - ecran: 106 | cherchais: le bouton « コピー » de la consigne | regarde: invisible tant que l'accord n'est pas complet ; il apparaît seulement dans le bandeau (écran 108)
abandon: jamais
incidents-banc:
defauts:
  - geste: 42 | ecran.vu: 42 | cible: あなたの番：…  読む | geste.type: touche
    attendu: consigne dépliée avec la grille encore visible | obtenu: le panneau prend toute la zone de travail, plus aucune case visible (idem écrans 2 et 104)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 104 | banc?: non | promesse: t8-td-2
  - geste: 90 | ecran.vu: 95 | cible: texte « B♭ » @ 84,522 | geste.type: touche
    attendu: la note se pose et la grille reste en place | obtenu: le piano roll défile/zoome tout seul ; le toucher suivant (#95) a posé une note B♭ longue parasite au pas 11 (refait écrans 99, 103, 107)
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: oui: ecran 103 | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 43 | cible: シンバル | geste.type: glisse
    attendu: le pad demandé visible au-dessus du panneau | obtenu: le pad est sous le panneau, il faut faire défiler sans indice
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t8-td-2
  - geste: 38 | ecran.vu: 39 | cible: consigne repliée « …ド » rendue « ト… » | geste.type: touche
    attendu: le dernier caractère avant « … » entier | obtenu: caractère à moitié dessiné (« に » vu « l… » écran 1, « ド » vu « ト… » écran 39)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 1 | banc?: non | promesse: t8-td-3
  - geste: 95 | ecran.vu: 95 | cible: texte « C3 » @ 84,759 | geste.type: touche
    attendu: noms de touches du clavier lisibles à 200 % | obtenu: B♭, A♭, F, E, C3 se chevauchent et sont coupés à moitié
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 102 | banc?: non | promesse: t8-td-3
  - geste: 72 | ecran.vu: 73 | cible: ↑ タップする点線のノートがあと … | geste.type: touche
    attendu: le compteur de notes restantes lisible | obtenu: le nombre est remplacé par « … », on ne sait pas combien il en reste
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t8-td-3
  - geste: 81 | ecran.vu: 81 | cible: texte « F » | geste.type: touche
    attendu: la ligne F aiguë distinguable (numéro d'octave) | obtenu: F aigu et F grave portent le même nom, sans octave
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 4 | cible: ここに点線のマスが… | geste.type: touche
    attendu: l'aide au-dessus de la grille de batterie lisible | obtenu: coupée par le haut
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t8-td-3
  - geste: 30 | ecran.vu: 31 | cible: 埋める すべて 1/2 1/4 | geste.type: touche
    attendu: la ligne de remplissage entière | obtenu: à moitié cachée sous le panneau du tuto
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: t8-td-2
  - geste: 30 | ecran.vu: 31 | cible: キックの下では弱めに | geste.type: touche
    attendu: l'étape vérifie la vélocité douce demandée | obtenu: étape validée sans aucune note adoucie
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
defauts-avant:
  - P6-texte | corrigé | ecran: 1 | consigne repliée qui finit par « … » + 続きを読む, dépliée elle se lit en entier (le caractère juste avant « … » reste à moitié dessiné, fiche à part)
  - P6-grille | encore là | ecran: 42 | dépliée, la consigne recouvre toute la grille (aussi écrans 2 et 104) ; seul 自分でやる la rend
  - T-ch3 | corrigé | ecran: 8 | chapitre 3 fini sans blocage, « 完了 » affiché
chapitres: ch3 compris du premier coup ; ch4 hésitation (pad シンバル caché) ; ch5 compris du premier coup ; ch6 hésitation (piano roll qui bouge sous le doigt) ; bravo pas atteint (budget)
