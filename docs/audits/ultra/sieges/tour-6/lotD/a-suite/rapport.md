siege: a | profil: t6-Da japonais-debutant-tuto-facile | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 223 | tenue: oui | pourquoi: « おめでとう、曲が完成！ » atteint (écran 223), aucune impasse ; mais 5 étapes d'écoute non validées en silence ont coûté un détour (fiche 1)
promesse: id=t6-Da-2 | texte: À chaque étape du tuto, le texte d'aide est lisible et l'élément montré est bien à l'écran | ecran: 78 | tenue: non | pourquoi: cases et notes en pointillés cachées sous le panneau du tuto (écrans 21, 78, 114)
promesse: id=t6-Da-3 | texte: Je peux quitter le tuto puis le reprendre là où je l'avais laissé | ecran: 42 | tenue: oui | pourquoi: Retour système à l'écran 40 (chap. 4 étape 1/3), « 続ける » → repris à la même étape (écran 42)
promesse: id=t6-Da-4 | texte: chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 81 | tenue: non | pourquoi: « スタブ », « コード », « リフ », « ライド », « フェーダー », « 小節 » jamais expliqués ; pads nommés autrement que sur l'écran
promesse: id=t6-Da-5 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 49 | tenue: non | pourquoi: « Perc », « Rim », « Groove » restent en latin ; bouton « キット テクノ をダウンロード » vraiment coupé (image 17)
oeil-neuf: q1 | ecran: 223 | image: 223 | « マイファイル » gris sans bord ressemble peu à un bouton ; « チュートリアルを閉じる » est un texte nu touchable ; le reste se lit bien
oeil-neuf: q6 | ecran: 223 | image: 223 | preuve claire (coupe + « おめでとう、曲が完成！ ») mais l'en-tête dit encore « リフ チャプター 7/15 · ステップ 1/2 », contradictoire
oeil-neuf: q9 | ecran: 223 | image: 223 | le champ « マイソング » est coupé à mi-hauteur par les boutons M4A/WAV
hesitations:
  - ecran: 19 | cherchais: le pad « バスドラム類 » demandé | regarde: aucun pad de ce nom (« スタジオのバスドラム », « バスドラム 2 ») ; j'ai touché les cases directement
  - ecran: 69 | cherchais: où sont les 8 notes restantes | regarde: deviné que le bandeau « この先に → » se touche
  - ecran: 127 | cherchais: le bouton « チャプター » pour revenir à l'étape 1 | regarde: aucun bouton de ce nom ; j'ai pris la flèche ←
abandon: jamais
incidents-banc:
  - geste: 22 | attente: 2 | vu: écran presque vide après « 自分でやる » (aussi 27, 34, 43, 50, 109, 164, 187, 193) ; « attends 2 » le ramène
  - geste: 125 | attente: 0 | vu: le texte en ne montre pas « あなたの番：このパートに「グルーヴ」… » alors qu'il est sur l'image 126
  - geste: 8 | attente: 0 | vu: outils hors human-act : filtres d'affichage head/grep/sed sur la sortie (head a cassé un tuyau au geste 8, geste quand même joué) ; une fois sortie redirigée vers lotDsuite/a-tmp.txt (hors siège) puis effacé par rm
  - geste: 104 | attente: 0 | vu: « attends » ne compte pas pour le plafond d'images : 5 touches « à vide » sur le bandeau (gestes 111-120) pour pouvoir voir l'image
defauts:
  - geste: 199 | ecran.vu: 199 | cible: texte « 残り 5 ステップ。タップするとそこへ移動： » | geste.type: touche
    attendu: le « Bravo » à la fin du chap. 15, ou un avertissement à chaque étape d'écoute passée avec « 次へ » | obtenu: « もう少しで完成！ », 5 écoutes (ch. 1, 2, 4, 5, 7) non validées sans rien dire ; 24 gestes pour les refaire
    symptome: incompris | gravite: contourne-en-24-gestes | revu: oui: ecran 127 | banc?: non | promesse: t6-Da-1
  - geste: 78 | ecran.vu: 78 | cible: bouton « ベース · 小節 4/8 タップする点線のノートがあと 1 個 » | geste.type: touche
    attendu: le bandeau amène la note en pointillés au milieu de la grille | obtenu: la note atterrit au bord bas, sous le panneau du tuto (image 78), touchée à l'aveugle
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 114 | banc?: non | promesse: t6-Da-2
  - geste: 19 | ecran.vu: 21 | cible: texte « ここに点線のマスがあと 2 個 » | geste.type: touche
    attendu: les 4 cases en pointillés visibles | obtenu: grille presque entièrement sous le panneau (image 21), 2 cases invisibles ; il faut « 自分でやる »
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 26 | banc?: non | promesse: t6-Da-2
  - geste: 49 | ecran.vu: 49 | cible: texte « あなたの番：「Perc」パッド（小さな「コッ」）で、点線のマスをタップしよう：拍からずれた位置に来る。 » | geste.type: touche
    attendu: le nom du pad tel qu'écrit dessus | obtenu: « Perc » (latin) ; le pad s'appelle « 太鼓のふち », ailleurs « 「コッ」 » et « Rim »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 56 | banc?: non | promesse: t6-Da-5
  - geste: 19 | ecran.vu: 19 | cible: texte « あなたの番：「バスドラム類」パッド（キックを鳴らすボタン）で、点線のマスをタップしよう：1拍に1つずつ。1マスがループの中の1つの瞬間。 » | geste.type: touche
    attendu: un nom de pad visible (« スタジオのバスドラム ») | obtenu: « バスドラム類 » n'existe pas ; même cas chap. 3 « 手拍子たち » pour le pad « 手拍子 »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 33 | banc?: non | promesse: t6-Da-4
  - geste: 33 | ecran.vu: 38 | cible: texte « …2小節分の点線のマスをタップしよう：2拍目と4拍目、そして次の小節の直前に小さな弱いヒット。出てきたら、先に「2 小節にする」をタップしてね。 » | geste.type: touche
    attendu: un bouton « 2 小節にする » ou deux mesures à remplir | obtenu: aucun bouton, grille à « 1小節 », chapitre réussi avec 3 frappes à 100 % ; je ne comprends pas pourquoi c'est bon
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 17 | ecran.vu: 17 | cible: texte « 始める前に：キットはもうテクノ用（909 のドラムマシン）で、エフェクトも入っている。… » | geste.type: touche
    attendu: le kit affiché est « テクノ » | obtenu: l'écran montre « キット：スタジオ » et propose « キット テクノ をダウンロード（4 MB） » ; « 909 » non expliqué
    symptome: incompris | gravite: finition | revu: oui: ecran 26 | banc?: non | promesse: t6-Da-4
  - geste: 17 | ecran.vu: 17 | cible: bouton « キット テクノ をダウンロード（4 MB） » [coupé] | geste.type: touche
    attendu: un bouton lisible | obtenu: libellé invisible sous le fondu et « 続きを読む » (image 17), sur chaque étape du tuto
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: t6-Da-5
  - geste: 175 | ecran.vu: 175 | cible: texte « あなたの番：「ビルドアップ」パートの「4 小節目」で、最後の拍のヒットを消そう（赤い×をそれぞれタップ）。… » | geste.type: touche
    attendu: faire le geste demandé | obtenu: étape déjà cochée « 次のステップ » sans rien avoir fait ; pourquoi réussie ?
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 81 | ecran.vu: 81 | cible: texte « スタブ チャプター 6/15 · ステップ 1/1 · かんたん » | geste.type: touche
    attendu: mots de musicien expliqués comme « キック » ou « BPM » | obtenu: « スタブ », « コード » (accord), « リフ » (ch. 7), « ライド » (ch. 10), « フェーダー » (ch. 14) sans explication
    symptome: sans-aide | gravite: finition | revu: oui: ecran 93 | banc?: non | promesse: t6-Da-4
mots-faux:
  - ecran: 188 | vu: 「Rim」が他を覆っています | attendu: 「太鼓のふち」の音が大きすぎて他の音を隠しています
  - ecran: 124 | vu: 最初のパート：Groove / 「Groove」と名前を付ける | attendu: グルーヴ (la consigne dit « グルーヴ »)
  - ecran: 150 | vu: 低いうなり, 手拍子, ベース, コード, メロディ なし | attendu: 、 (virgule japonaise)
  - ecran: 103 | vu: 点線の音があと2個 | attendu: 点線のノート (ailleurs toujours « ノート »)
  - ecran: 204 | vu: AからZまでのチュートリアル | attendu: 全チャプター (calque du français « de A à Z »)
chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : hésitation (« BPM » expliqué, mais « フリジアン »/« キー » à toucher sans savoir ce que c'est ; l'écoute coupée par « 次へ » n'a pas compté)
  - chapitre 2 « キックとうなり » : hésitation (pad « バスドラム類 » introuvable, cases cachées sous le panneau)
  - chapitre 3 « クラップ » : hésitation (pas de bouton « 2 小節にする », réussi sans comprendre pourquoi)
  - chapitre 4 « ハイハット » : hésitation (pad « Perc » qui n'existe pas)
  - chapitre 5 « ベース » : hésitation (consigne très longue ; dernière note cachée sous le panneau)
  - chapitre 6 « スタブ » : hésitation (« スタブ » et « コード » non expliqués)
  - chapitre 7 « リフ » : hésitation (« コード » resté allumé pour la mélodie ; note cachée sous le panneau)
  - chapitre 8 « 最初のパート：Groove » : hésitation (« チャプター » introuvable pour revenir à l'étape 1)
  - chapitre 9 « パートを理解する » : compris du premier coup
  - chapitre 10 « パートを作ろう » : compris du premier coup
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション » : hésitation (étape 1 déjà réussie toute seule ; bandeau « → スタジオのバスドラム » alors que la grille est la シンバル)
  - chapitre 13 « エフェクト » : compris du premier coup
  - chapitre 14 « ミックス：音量 » : hésitation (« フェーダーの下の数字 » mais il y a écrit « ふつう » ; « 少し下げる » caché)
  - chapitre 15 « 曲を書き出そう » : compris du premier coup (« −14 », « ソロ », « ミュート » non expliqués)
coupe-verifiee: ecran 17 | image 17 | キット テクノ をダウンロード（4 MB） | vraiment coupé
coupe-verifiee: ecran 4 | image 4 | ミッドタム R 45 / ハイタム / 太鼓のふち / ベル | vraiment coupé
coupe-verifiee: ecran 11 | image 11 | スタジオのバスドラム (titre sous le panneau) | vraiment coupé
coupe-verifiee: ecran 38 | image 38 | 手拍子 (nom de la grille) | vraiment coupé
coupe-verifiee: ecran 60 | image 60 | + トラック / ゴーストベース | vraiment coupé
coupe-verifiee: ecran 95 | image 95 | Acid 303 ベースライン | vraiment coupé
coupe-verifiee: ecran 188 | image 188 | 少し下げる | vraiment coupé
coupe-verifiee: ecran 223 | image 223 | 曲名 « マイソング » | vraiment coupé
