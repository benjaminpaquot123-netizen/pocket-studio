code testé : 4cb88f552031889ab2b4854b9ddd285f2abb6250
joué par : sous-agent neuf

siege: a | profil: t6-Da | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 181 | tenue: oui | pourquoi: 15/15 chapitres faits, « おめでとう、曲が完成！ » atteint ; des détours (ch. 3, 5, 7) mais aucune impasse
promesse: id=t6-Da-2 | texte: À chaque étape du tuto, le texte d'aide est lisible et l'élément montré est bien à l'écran | ecran: 106 | tenue: non | pourquoi: ch. 7, les notes en pointillé restantes sont hors de la grille visible et l'aide « もっと上 ↑ » envoie dans la mauvaise direction ; ch. 2 (écran 18) le texte dit « キットはもうテクノ用 » alors que l'écran montre « キット：スタジオ »
promesse: id=t6-Da-3 | texte: Je peux quitter le tuto puis le reprendre là où je l'avais laissé | ecran: 44 | tenue: oui | pourquoi: ⋮ → ホーム puis « 続ける » : retour exact sur « チャプター 4/15 · ステップ 1/3 »
promesse: id=t6-Da-4 | texte: chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 61 | tenue: non | pourquoi: « ベース » (ch. 5), « スタブ » (titre ch. 6, expliqué seulement au ch. 9), « グルーヴ », « ライド », « トランジション » jamais expliqués ; ch. 3 la consigne « ソフト » est cachée quand le panneau se replie
promesse: id=t6-Da-5 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 171 | tenue: non | pourquoi: « ⚠️ 「Rim」が他を覆っています。 » (anglais) et bouton « パートに「Groove」と名前を付ける » (alphabet latin) ; rien en français
oeil-neuf: q1 | ecran: 181 | image: 181 | tout ce qui a l'air touchable l'est (シェア, M4A/WAV, マイファイル, 停止, やり直す, 閉じる) ; le champ 曲名 à moitié caché ne se devine plus touchable
oeil-neuf: q6 | ecran: 181 | image: 181 | preuve claire (trophée vert « おめでとう、曲が完成！ ») ; suite proposée : やり直す ou 閉じる, rien ne dit où retrouver le fichier sauf マイファイル
oeil-neuf: q9 | ecran: 181 | image: 181 | le champ « 曲名 » et son contenu « マイソング » sont coupés sous le haut du panneau (vraiment coupé à l'image)
hesitations:
  - ecran: 37 | cherchais: pourquoi les 3 cases pointillées remplies ne valident pas l'étape | regarde: la consigne repliée sur 2 lignes cachait « 長押しして、「ソフト」 »
  - ecran: 106 | cherchais: la note pointillée restante | regarde: « 点線の音があと N 個、もっと上 ↑ » alors que la note était à droite / à gauche hors vue ; 12 gestes de recherche
  - ecran: 18 | cherchais: la kit techno annoncée | regarde: « キットはもうテクノ用 » mais pads « スタジオのバスドラム » et « キット：スタジオ »
abandon: jamais
incidents-banc:
  - geste: 20 | attente: 0 | vu: l'écran en texte ne montre plus que la barre « フィードバックモード » (écrans 21, 22, 27, 28, 34, 35, 45, 46, 53, 54, 148, 149, 163, 164, 171, 176) ; j'ai joué sur l'image
  - geste: 44 | attente: 0 | vu: écrans 45-46 : 2 touches « à l'aveugle » (texte vide, mise en page différente de l'image précédente) tombées hors grille, rien écrit
  - geste: 0 | attente: 0 | vu: outils hors human-act.py : seulement Read sur les images du siège, les consignes et ce rapport
defauts:
  - geste: 105 | ecran.vu: 106 | cible: bouton « メロディ · 小節 1/8 点線の音があと5個、もっと上 ↑ » | geste.type: touche
    attendu: l'aide montre où est la note pointillée suivante (ou la grille y défile) | obtenu: « もっと上 ↑ » alors que les notes étaient dans la mesure suivante / au bord gauche ; puis « ← …この前に » ; 12 gestes de glisse et 見せて/自分でやる
    symptome: incompris | gravite: contourne-en-12-gestes | revu: oui: ecran 84 (même chose au ch. 5) | banc?: non | promesse: t6-Da-2
  - geste: 36 | ecran.vu: 37 | cible: icône « あなたの番：「手拍子たち」パッドで、点線のマスをタップしよう：2拍目と4拍目、そして次の周の直前の最後のマス。この最後のヒットは弱く：長押しして、「ソフト」。 読む » ⌃ | geste.type: touche
    attendu: si les cases sont remplies mais pas en « ソフト », un mot le dit | obtenu: toutes les cases remplies, rien ne valide, aucun message ; la consigne « ソフト » est dans la partie repliée
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 17 | ecran.vu: 18 | cible: texte « 始める前に：キットはもうテクノ用（909 のドラムマシン）で、エフェクトも入っている。… » | geste.type: touche
    attendu: la kit affichée est la kit techno | obtenu: l'écran montre « キット：スタジオ » et « スタジオのバスドラム » ; le texte contredit l'écran
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-2
  - geste: 60 | ecran.vu: 61 | cible: texte « お手本のベースをキックといっしょに聴こう：ヒットの間にすべり込む。 » | geste.type: touche
    attendu: « ベース » expliqué dans la liste • comme キック, 拍… | obtenu: aucun « • ベース： » ; idem « ライド » (écran 128), « グルーヴ » (écran 122), « トランジション » (écran 160), « スタブ » au ch. 6 (écran 91)
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 170 | ecran.vu: 172 | cible: texte « ⚠️ 「Rim」が他を覆っています。 » | geste.type: touche
    attendu: le nom japonais du pad (« 太鼓のふち ») | obtenu: « Rim » en anglais
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 122 | ecran.vu: 123 | cible: bouton ⊕ «   パートに「Groove」と名前を付ける » | geste.type: touche
    attendu: « グルーヴ » comme dans la phrase au-dessus | obtenu: « Groove » en alphabet latin (le nom créé est bien « グルーヴ »)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 32 | ecran.vu: 33 | cible: texte « あなたの番：「手拍子たち」パッドで、点線のマスをタップしよう：… » | geste.type: touche
    attendu: le nom du pad tel qu'à l'écran | obtenu: « 手拍子たち » alors que le pad s'appelle « 手拍子 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-2
  - geste: 175 | ecran.vu: 176 | cible: texte « 「共有」でプロジェクト「マイプロジェクト 1」の名前も変わります。 » | geste.type: touche
    attendu: le même mot que le bouton | obtenu: le texte dit « 共有 », le bouton dit « シェア »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 138 | ecran.vu: 139 | cible: texte « • ブレイク：静かになる部分。ドラムが止まります。 » | geste.type: touche
    attendu: définition cohérente avec la consigne | obtenu: la même étape garde ハイハット et « コッ » (des batteries) : « ドラムが止まります » contredit
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
mots-faux:
  - ecran: 134 | vu: 低いうなり, 手拍子, ベース | attendu: 低いうなり、手拍子、ベース (virgule japonaise)
  - ecran: 149 | vu: 最後に追加： | attendu: 追加： (le tuto dit « 「追加」の下で »)

chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : hésitation (texte « キットはもうテクノ用 » mais écran « キット：スタジオ »)
  - chapitre 3 « クラップ » : hésitation (3 cases remplies sans succès ; « ソフト » caché dans la consigne repliée)
  - chapitre 4 « ハイハット » : compris du premier coup
  - chapitre 5 « ベース » : hésitation (« ベース » non expliqué ; « もっと上 ↑ » alors que la dernière note était au bord droit)
  - chapitre 6 « スタブ » : hésitation (« スタブ » non expliqué dans ce chapitre)
  - chapitre 7 « リフ » : hésitation (notes hors vue, aide « もっと上 ↑ » trompeuse, 12 gestes)
  - chapitre 8 « 最初のパート：グルーヴ » : hésitation (« グルーヴ » non expliqué, bouton « Groove » en latin)
  - chapitre 9 « パートを理解する » : compris du premier coup (« ライド » non expliqué)
  - chapitre 10 « パートを作ろう » : compris du premier coup
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション » : compris du premier coup (étape 1 déjà faite, bien signalé)
  - chapitre 13 « エフェクト » : compris du premier coup
  - chapitre 14 « ミックス：音量 » : compris du premier coup (« Rim » en anglais)
  - chapitre 15 « 曲を書き出そう » : compris du premier coup
coupe-verifiee: ecran 12 | image 12 | • キー：曲の始まりの音と、その明るさや暗さ。 | vraiment coupé (glyphes à moitié effacés sous « なぜ？ », lisible via 続きを読む)
coupe-verifiee: ecran 17 | image 17 | あとで | vraiment coupé (sous le pli, invisible sans défiler)
coupe-verifiee: ecran 181 | image 181 | 曲名 / マイソング | vraiment coupé (champ à moitié sous le haut)
defauts-avant:
  - T2 : corrigé (écran 107) : note pointillée au bord droit (fin contour), touchée à x=540 : cette note-là est écrite (compteur 5→4), aucune note parasite ; idem ch. 5 (écran 88)
  - T7a : corrigé (écran 6) : « 新しい曲は 120 BPM から始まる » et l'écran montre « 120 テンポ »
  - T7c : corrigé (écran 74) : コード allumé, passage sur Acid 303 puis retour sur ベース : « コード » toujours [sélectionné]
  - T8 : corrigé (écran 158) : retour sur une étape déjà faite : « もうできています！ 曲にはこのステップで必要なものがもうありました » (aussi écran 160, ch. 12)
hesitations-total: 8
