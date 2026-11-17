code testé : f1b9fdb1fdb38b1824bbf4bb43318cc6f9291277
joué par : sous-agent neuf

siege: b | profil: haruto-debutant-ja | lang: ja | device: telephone | font: 100 | mode: simple
mission: tuto-facile (techno, chapitre 3 puis suivant) | buts: chapitre-3-fini, chapitre-4-commence, chapitre-4-etape-1
promesse: id=ecran-compteur | texte: ここに点線のマスがあと 3 個 | ecran: 3 | tenue: oui | pourquoi: le compteur descend 3→2→1 puis « よくできました、成功！ »
promesse: id=ecran-chapitre | texte: よくできました、章クリア！ | ecran: 8 | tenue: oui | pourquoi: chapitre 3 coché dans la liste (écran 10), « 次の章 » ouvre ハイハット 4/15
promesse: id=ecran-consigne | texte: あなたの番：「ベーストラックを作る」をタップ（トラック＝曲の楽器1つ）、次に1小節目の点線のノート | ecran: 22 | tenue: non | pourquoi: la consigne demande encore de toucher « ベーストラックを作る » alors que la piste est déjà créée
oeil-neuf: q1 | ecran: 27 | image: 22 | cases pointillées, onglets et pistes se voient touchables ; le bandeau « ベース · 小節 1/8 » a l'air d'un bouton mais ne fait rien
oeil-neuf: q6 | ecran: 27 | image: 22 | preuve = notes pleines + compteur « あと 4 個 » ; la suite est claire (les pointillés)
oeil-neuf: q9 | ecran: 27 | image: 22 | « ＋ トラ→ » coupé par la flèche en haut à droite ; la consigne repliée finit par « … » sur deux lignes, propre
hesitations:
  - ecran: 1 | cherchais: la consigne de l'étape | regarde: le bouton de téléchargement du kit prend sa place, je n'ai lu qu'après avoir tapé une case
  - ecran: 14 | cherchais: où est passé le panneau après « 自分でやる » | regarde: écran 13 presque vide un instant, puis barre repliée en bas
abandon: jamais
incidents-banc:
  - geste: 13 | attente: 2 | vu: écran presque vide (seule la barre フィードバックモード) juste après « 自分でやる », revenu après « attends 2 »
defauts:
  - geste: 12 | ecran.vu: 12 | cible: icône « 続きを読む » ⌄ @ 294,741 | geste.type: touche
    attendu: la consigne s'ouvre et je vois encore la grille des cases | obtenu: le panneau monte jusqu'à y=300 et cache les 16 cases en entier (seul « ここに点線のマスがあと 4 個 » dépasse, coupé)
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 12 | cible: texte « ▸ なぜ？ » @ 294,692 | geste.type: touche
    attendu: « ▸ なぜ？ » lisible sous la liste | obtenu: consigne dépliée : la dernière puce « パッド：さわると音が鳴る四角。 » chevauche « ▸ なぜ? », le ▸ est rogné
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 12 | cible: icône « 続きを読む » ⌄ @ 294,741 | geste.type: touche
    attendu: une fois déplié, le lien dit « 閉じる » ou équivalent | obtenu: il dit toujours « 続きを読む ⌄ » alors que tout est déjà déplié
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 22 | cible: bouton + «  トラック » [coupé] @ 508,186 | geste.type: touche
    attendu: bouton « ＋ トラック » entier | obtenu: « ＋ トラ » puis une flèche → posée dessus
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 22 | cible: icône « あなたの番：「ベーストラックを作る」をタップ（トラック＝曲の楽器1つ）、次に1小節目の点線のノート：キックのあとに小さな2音ずつ。 読む » ⌃ @ 294,978 | geste.type: touche
    attendu: la consigne passe à la partie notes une fois la piste créée | obtenu: elle demande toujours de toucher « ベーストラックを作る », bouton qui n'existe plus à l'écran
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: ecran-consigne
  - geste: 2 | ecran.vu: 2 | cible: bouton « キット テクノ をダウンロード（4 MB） » @ 247,647 | geste.type: touche
    attendu: la consigne de l'étape visible avec l'offre de kit | obtenu: au début du chapitre 3 le bandeau de téléchargement remplace la consigne ; on ne sait quoi faire qu'en lisant la grille
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - D6 | pas atteint | ecran: 10 | le chapitre 3 (クラップ, 2 étapes) se joue entièrement à la Rythmique, jamais au Clavier ; au Clavier du chapitre 5 (écran 22-27) aucun 「ここをタップ」 ne s'est montré
  - D1 | pas atteint | ecran: 10 | pas de grille de notes au chapitre 3 ; pour info au chapitre 5 (écran 22) la grille des notes reste visible au-dessus du panneau replié
  - D3 | autre | ecran: 6 | plus de chevauchement par « ▸ なぜ? » (il est sur sa ligne), mais la consigne repliée s'arrête en plein mot « …長押しして、開い » sans « … » : elle ne se lit pas entière ; dans la barre repliée (écran 17) deux lignes propres finies par « … » + « 読む »
