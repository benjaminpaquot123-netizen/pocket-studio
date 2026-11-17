code testé : ede109d8ada92d293e0cfd3b777f53b38e2e3b68
joué par : sous-agent neuf

siege: b | profil: adulte-japonais-premiere-fois | lang: ja | device: telephone | font: 100 | mode: simple
mission: tuto-facile | buts: chapitre-3, chapitre-4, chapitre-5
promesse: id=repli | texte: icône ⌄ (pas de mot « 閉じる ⌃ ») | ecran: 26 | tenue: oui | pourquoi: ⌄ replie le panneau, les rangées de la grille reviennent (écrans 2 et 26) ; l'icône seule, sans mot
promesse: id=consigne-coupee | texte: « あなたの番：4小節目のノート（最後は1段上がって、上に跳ぶ）をタップして、もう一度「コピ… 読む ⌃ » | ecran: 50 | tenue: oui | pourquoi: le texte replié finit par « … » (images 11, 50)
promesse: id=chaque-etape | texte: « 見せて » / « 自分でやる » / « 次の小節にコピー » | ecran: 48 | tenue: oui | pourquoi: chaque étape a 見せて ou un bouton d'action, et une coche « 成功 » après (écrans 5, 15, 23, 30, 33, 43, 48, 61)
promesse: id=touche-ici | texte: « ベース · 小節 4/8 ↑ タップする点線のノートがあと 8 個 ここをタップ » | ecran: 51 | tenue: oui | pourquoi: le roll bouge et les notes restent visibles, mais elles passent du centre au bord bas (voir défaut 2)
oeil-neuf: q1 | ecran: 64 | image: 64 | tout ce qui a un contour se touche ; « チャプター » (texte + icône) et ⌄ se touchent sans en avoir l'air
oeil-neuf: q6 | ecran: 64 | image: 64 | preuve claire : bandeau vert « よくできました、章クリア！ » + « 点線のノートはすべて置けたよ » ; suite : gros bouton « 次の章 »
oeil-neuf: q9 | ecran: 64 | image: 64 | « + トラ→ » coupé et chevauché par une flèche ; « マイプロジェ… » coupé ; le reste lisible
hesitations:
  - ecran: 50 | cherchais: où sont les notes « au-dessus » (flèche ↑) | regarde: elles étaient déjà toutes au centre du roll
  - ecran: 33 | cherchais: lequel toucher pour avancer | regarde: « 次のステップ » et « 次へ » proposés en même temps
  - ecran: 1 | cherchais: le mot « 閉じる » pour replier | regarde: seulement l'icône ⌄
abandon: jamais
incidents-banc:
defauts:
  - geste: 35 | ecran.vu: 35 | cible: bouton + «  トラック » [coupé] @ 508,186 | geste.type: touche
    attendu: le bouton « + トラック » lisible en entier | obtenu: « + トラ→ » coupé et chevauché par une image → (images 35, 64)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 64 | banc?: non | promesse: -
  - geste: 51 | ecran.vu: 51 | cible: bouton « ベース · 小節 4/8 ↑ タップする点線のノートがあと 8 個 ここをタップ » | geste.type: touche
    attendu: la flèche ↑ et ここをタップ amènent les notes au centre | obtenu: les notes étaient déjà au centre ; elles sont poussées contre le panneau, au bas du roll (image 51)
    symptome: incompris | gravite: finition | revu: oui: ecran 59 | banc?: non | promesse: touche-ici
  - geste: 1 | ecran.vu: 1 | cible: texte « ▸ なぜ？ » | geste.type: touche
    attendu: « ▸ なぜ？ » entier | obtenu: coupé au bord gauche du panneau (images 1, 24)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 24 | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 11 | cible: bandeau « ここに点線のマスがあと N 個 » | geste.type: touche
    attendu: le nom du pad visible | obtenu: « 手拍子 », « 太鼓のふち » cachés ou éteints derrière le bandeau (images 11, 24)
    symptome: cache | gravite: finition | revu: oui: ecran 24 | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 24 | cible: panneau déplié, étape d'écoute | geste.type: touche
    attendu: la grille entière visible pendant l'écoute | obtenu: le panneau coupe la 2e rangée ; il faut replier (1 geste)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 26 | banc?: non | promesse: repli
  - geste: 1 | ecran.vu: 1 | cible: icône ⌄ @ 531,470 | geste.type: touche
    attendu: « 閉じる ⌃ » écrit | obtenu: icône seule, sans mot ; on devine
    symptome: sans-nom | gravite: finition | revu: oui: ecran 26 | banc?: non | promesse: repli
  - geste: 33 | ecran.vu: 33 | cible: « 次のステップ » + icône « 次へ » → | geste.type: touche
    attendu: un seul bouton pour avancer | obtenu: deux boutons pour avancer en même temps
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 47 | vu: キット テクノ をダウンロード | attendu: テクノキットをダウンロード
  - ecran: 17 | vu: 太鼓のふち | attendu: リムショット
  - ecran: 49 | vu: 最後は1段上がって、上に跳ぶ | attendu: 最後は1つ上の音に跳ぶ
  - ecran: 58 | vu: 点線の音があと1個、もっと上 | attendu: 点線のノートがあと1個（もっと上）（« ノート » partout ailleurs）
  - ecran: 5 | vu: 成功 | attendu: できた！
defauts-avant:
  - b-V1 | corrigé | ecran: 26 | ⌄ replie le panneau et les rangées de la grille reviennent
  - b-V2 | corrigé | ecran: 50 | la consigne longue repliée finit par « … »
  - b-V3 | corrigé | ecran: 48 | chaque étape a 見せて ou un bouton d'action, puis une coche « 成功 »
