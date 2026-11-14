code testé : 1a2d9ea955344a2ef11452258c85f209ae809e37
joué par : sous-agent neuf

siege: a | profil: t6-Da | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: t6-Da-1, t6-Da-2
promesse: id=t6-Da-1 | texte: 曲をまるごと自分で作る、一歩ずつ。説明とチェックはまかせて。 | ecran: 143 | tenue: oui | pourquoi: chapitres 1 à 12 enchaînés avec les seuls boutons du tuto (次へ / 次のステップ / 次の章 / 自分でやる), aucune impasse, « よくできました、章クリア！ » au ch. 12 (écran 143)
promesse: id=t6-Da-2 | texte: あなたの番：… (consigne de chaque étape) | ecran: 31 | tenue: oui | pourquoi: à chaque étape l'élément à toucher (cases pointillées, bouton « ここをタップ », notes pointillées) était visible au-dessus du panneau ; seul accroc : le texte long du ch. 3 se lit dans une petite fenêtre de 3 lignes (image 31), lisible mais à faire défiler
oeil-neuf: q1 | ecran: 143 | image: 143 | grille de cases, boutons 埋める/すべて/1/2/1/4/◀▶, 消去, longueurs 1小節/2/3/4 : tout ce qui a l'air touchable l'est ; « 埋める » et « 消去 » sont du texte orange sans cadre, on hésite à les croire boutons
oeil-neuf: q6 | ecran: 143 | image: 143 | preuve : case 1 allumée en bleu + bandeau vert « よくできました、章クリア！ » ; suite : déplier le bandeau (⌃) pour « 次の章 », pas évident au premier coup d'œil
oeil-neuf: q9 | ecran: 143 | image: 143 | rien de coupé ; en haut « イントロ 小節 1/128 » alors que la partie affichée est « D · ドロップ » (léger décalage, pas coupé)
hesitations:
  - ecran: 31 | cherchais: lire toute la consigne du ch. 3 (clap doux) | regarde: « 続きを読む » touché, rien ne s'est déplié ; le texte reste dans une fenêtre de 3 lignes déjà défilée au milieu (image 31)
  - ecran: 90 | cherchais: la 3e note pointillée de la mesure 1 (ch. 7), coupée au bord droit | regarde: « ここをタップ » m'a emmené à la mesure 2 avant de finir la 1 ; rattrapé par « ← … この前に ここをタップ » (écran 96)
  - ecran: 140 | cherchais: ce que fait l'étape 1 du ch. 12 | regarde: « もうできています！ … 触るところはありません » sans dire ce qui était déjà fait
abandon: jamais
incidents-banc:
  - geste: 129 | attente: 0 | vu: écrans 130-131 rendus en texte presque vides (seule la barre REC/メモ), l'image montrait pourtant l'écran complet ; revenu normal à l'écran 132
defauts:
mots-faux:
  - ecran: 116 | vu: 低いうなり, 手拍子, ベース, コード, メロディ なし（virgules occidentales « , ») | attendu: 低いうなり、手拍子、ベース、コード、メロディ なし
chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : compris du premier coup
  - chapitre 3 « クラップ » : hésitation (consigne longue lisible seulement dans une fenêtre de 3 lignes, « 続きを読む » sans effet visible)
  - chapitre 4 « ハイハット » : compris du premier coup
  - chapitre 5 « ベース » : compris du premier coup (la note « 1段上 » hors vue est signalée par « もっと上 ↑ ここをタップ »)
  - chapitre 6 « スタブ（短いコード） » : compris du premier coup
  - chapitre 7 « リフ » : hésitation (« ここをタップ » saute à la mesure 2 alors qu'il reste une note en mesure 1)
  - chapitre 8 « 最初のパート：グルーヴ、基本のループ » : compris du premier coup
  - chapitre 9 « パートを理解する » : compris du premier coup
  - chapitre 10 « パートを作ろう » : compris du premier coup (petite contradiction : ch. 10 ét. 5 dit que le silence final se fera au ch. 12, le message de réussite dit déjà « 最後の拍は鳴りません »)
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション：パートからパートへのつなぎ » : hésitation (étape 1 « もうできています！ » sans explication)
points:
  - P5 : autre (aucune étape à croix rouges ne s'est présentée : ch. 12 = 2 étapes ; l'étape 1/2 affichait « もうできています！ 曲にはこのステップで必要なものがもうありました：触るところはありません。 » (écran 140), l'étape 2/2 était la cymbale de la ドロップ, chapitre clair à l'écran 143 ; image regardée : 143 ; donc ligne « → pad » et croix au-dessus du panneau pas vues)
hesitations-total: 3
