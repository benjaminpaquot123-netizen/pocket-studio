code testé : d7e98a3e46c4e53c7a049db6fec96b6316c5a91f
joué par : sous-agent neuf
chapitre 3 : compris du premier coup — 3 cases en pointillé touchées une fois chacune, appui long puis ソフト, « よくできました、章クリア！ » écran 9
chapitre 4 : compris du premier coup — 4 cases en pointillé, 見せて clair ; fin avec seulement « 完了 », sans Bravo (écran 29)
chapitre 1 : compris du premier coup — ouvert par la liste, étapes 2 et 3 « もうできています！ » (écrans 30-35)
chapitre 2 : hésitation — consigne très longue (écran 36), « キックだけを聴こう » alors que la grille montre 太鼓のふち (écran 37)
chapitre 5 : hésitation — après コピー, bandeau « あと 8 個 ここをタップ » pendant que le panneau dit « 成功！ » (écran 58) ; le panneau cache la grille à l'étape 4 jusqu'à 自分でやる (écran 59) ; Bravo écran 70
chapitre 6 : compris du premier coup — même contradiction après コピー (écran 77) ; Bravo écran 82
siege: b | profil: t8-ct0a-b debutant-ja-telephone | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
oeil-neuf: q1 | ecran: 82 | image: 82 | コード et ゴーストベース sont tous deux pleins orange : on dirait deux boutons allumés ; le bandeau « 点線のノートはすべて置けたよ » ressemble à un bouton
oeil-neuf: q6 | ecran: 82 | image: 82 | preuve claire (notes pleines + « よくできました、章クリア！ ») mais aucun bouton « 次の章 » visible sans ouvrir le ^ du panneau
oeil-neuf: q9 | ecran: 82 | image: 82 | « マイプロジェ… » coupé ; « + トラ » coupé par la flèche → ; pas d'autre texte coupé
hesitations:
  - ecran: 42 | cherchais: le chapitre suivant après le 2 | regarde: 次の章 m'a ramené au chapitre 3 déjà fini, je suis passé par la liste チャプター
  - ecran: 58 | cherchais: si j'avais réussi | regarde: panneau « 成功！ » mais bandeau « あと 8 個 ここをタップ »
  - ecran: 59 | cherchais: les notes du 4e小節 | regarde: le panneau couvrait la grille, j'ai touché 自分でやる
abandon: jamais
incidents-banc:
  - geste: fin | attente: 0 | vu: le banc a écrit en tête « code testé : d7e98a3e… », le brief annonçait 8eabd655 ; j'ai gardé les lignes du banc
  - geste: - | attente: 0 | vu: outils hors human-act.py : Read sur mes images screen-*.png du siège, cat/heredoc sur rapport.md seulement
defauts:
  - geste: 58 | ecran.vu: 58 | cible: ベース · 小節 4/8 ↑ タップする点線のノートがあと 8 個 ここをタップ | geste.type: touche
    attendu: après « よくできました、成功！ », plus rien à faire tant que je n'ai pas lu l'étape suivante | obtenu: le bandeau me demande déjà 8 notes, il contredit le panneau
    symptome: incompris | gravite: finition | revu: oui: ecran 77 | banc?: non | promesse: -
  - geste: 58 | ecran.vu: 58 | cible: タップする点線のノートがあ… | geste.type: touche
    attendu: le compteur entier à 100 % | obtenu: texte coupé avant le nombre (image 58)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 42 | ecran.vu: 42 | cible: 次の章 | geste.type: touche
    attendu: aller au chapitre 3 non fait suivant (ici 5) | obtenu: retour au chapitre 3 déjà fini
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 30 | cible: チャプター | geste.type: touche
    attendu: le chapitre 1 marqué 2/3 reprend à l'étape 2 | obtenu: il s'ouvre à l'étape 1/3
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 37 | cible: キックだけを聴こう | geste.type: touche
    attendu: la grille montre la キック | obtenu: la grille montre 太鼓のふち
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 29 | cible: 完了 | geste.type: touche
    attendu: « よくできました、章クリア！ » comme aux chapitres 3, 5, 6 | obtenu: seulement 完了, pas de Bravo
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: ここに点線のマスがあと 1 個 | geste.type: touche
    attendu: le nom du pad (手拍子) lisible | obtenu: le nom est caché sous le bandeau compteur (image 6), même sur toutes les grilles rythme
    symptome: cache | gravite: finition | revu: oui: ecran 14 | banc?: non | promesse: -
  - geste: 72 | ecran.vu: 73 | cible: ゴーストベース | geste.type: touche
    attendu: un seul mode allumé (コード) | obtenu: コード et ゴーストベース tous deux [sélectionné], même orange (image 73)
    symptome: incompris | gravite: finition | revu: oui: ecran 82 | banc?: non | promesse: -
mots-faux:
  - ecran: 8 | vu: ベロシティ | attendu: 強さ (déjà utilisé ailleurs)
  - ecran: 48 | vu: ゴーストベース | attendu: mot japonais simple
  - ecran: 48 | vu: リズム補正 50% | attendu: phrase simple, sans chiffre
defauts-avant:
  - a-V1-ja | autre | ecran: 5 | chaque case en vue au-dessus du panneau et posée d'un seul toucher (écrans 1-5, images 1 et 6), mais aucun rond « ここをタップ » sur la note au chapitre 3 ; le mot n'apparaît que dans le bandeau basse (écran 58)
  - compteur-ja | autre | ecran: 58 | juste aux écrans 1, 3, 11, 14, 48, 53, 61, 68 ; mais écran 58 « あと 8 個 » pour ~7 pointillés visibles (une note hors de la grille) ; au chapitre 6 le compteur devient « 点線のコード：2 » (6 notes en pointillé)
