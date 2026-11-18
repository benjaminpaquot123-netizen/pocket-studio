code testé : 258441802eda97ec42ca0967188dfc83c7aeb29f
joué par : sous-agent neuf

siege: b | profil: controle-ja-tuto | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: V1, V2, V3 (siège de contrôle)
oeil-neuf: q1 | ecran: 72 | image: 72 | bandeau vert « よくできました、章クリア！ » et chevron ⌃ touchables ; « + トラ→ » en haut à droite on devine un bouton mais le mot est mangé
oeil-neuf: q6 | ecran: 72 | image: 72 | preuve = « 点線のノートはすべて置けたよ » + « 完了 » ; mais une seule note visible dans la mesure 4, mes 6 autres notes ne se voient plus
oeil-neuf: q9 | ecran: 72 | image: 72 | « + トラック » coupé par la flèche → (« + トラ→ ») ; le reste lisible
hesitations:
  - ecran: 1 | cherchais: comment replier le grand panneau qui cache la grille | regarde: seulement un chevron ⌄ en haut à droite, pas de mot
  - ecran: 69 | cherchais: la dernière note « もっと上 ↑ » | regarde: elle était à droite sur la même ligne après « ここをタップ »
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 60 | vu: le banc ne tournait pas au premier « regarde », reparti après un essai
defauts:
  - geste: 3 | ecran.vu: 4 | cible: texte « あなたの番：「手拍子」パッドで、点線のマスをタップしよう：2拍目と4拍目、そして次の周の直前の最後のマス。 » [coupé] | geste.type: touche
    attendu: consigne dépliée lisible en entier, ou finie par « … » | obtenu: coupée net après « 次の周の直 », sans « … » ; « 続きを読む » reste affiché à côté de « 閉じる » et n'ajoute qu'une puce, coupée aussi (écran 7)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 7 | banc?: non | promesse: -
  - geste: 49 | ecran.vu: 50 | cible: bouton + «  トラック » [coupé] | geste.type: touche
    attendu: le mot du bouton « + トラック » en entier | obtenu: « + トラ » recouvert par la flèche → de défilement
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 69 | ecran.vu: 70 | cible: bouton « ベース · 小節 4/8 タップする点線のノートがあと 1 個 » | geste.type: touche
    attendu: après « ここをタップ », je vois encore les notes déjà posées dans la mesure 4 | obtenu: la vue saute (C2 → C3), mes 6 notes posées ne se voient plus, seule la dernière reste visible à la fin (écran 72)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - V1 | corrigé | ecran: 5 | panneau déplié par-dessus la grille (écran 4, rangée 2 cachée) ; le lien « 閉じる ⌃ » le replie et les deux rangées de cases reviennent
  - V2 | corrigé | ecran: 12 | consigne repliée sur deux lignes, finit par « 次の周の… » ; pareil écran 29 « タップしよう… » et écran 62 « 「コピ… »
  - V3 | corrigé | ecran: 60 | chaque étape vue (ch. 3 à 5, 9 étapes) avait « 見せて » ou un bouton d'action (自分でやる, 結果を聴く, ベーストラックを作る, 次の小節にコピー) ; l'étape se coche après (écrans 14, 26, 45, 48, 58, 60, 72) ; « 見せて » montre, remet à zéro, puis « 今度はあなたの番 »
