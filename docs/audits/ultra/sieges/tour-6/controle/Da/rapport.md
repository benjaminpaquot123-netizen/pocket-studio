siege: a | profil: t6-Da-japonais-debutant-tuto | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: 1. Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 189 | tenue: oui | pourquoi: 15 chapitres finis, « おめでとう、曲が完成！ » atteint, malgré deux longs détours (ch. 5 et 7)
promesse: id=t6-Da-2 | texte: 2. À chaque étape du tuto, le texte d'aide est lisible et l'élément montré est bien à l'écran | ecran: 79 | tenue: non | pourquoi: note pointillée introuvable (« もっと上 ↑ » trompeur), « 少し下げる » sous le panneau du tuto, « あとで » sous le fondu
promesse: id=t6-Da-3 | texte: 3. Je peux quitter le tuto puis le reprendre là où je l'avais laissé | ecran: 44 | tenue: oui | pourquoi: « チュートリアルをやめる » puis « チュートリアルを再開 » : retour exact à la fin du chapitre 3
promesse: id=t6-Da-4 | texte: 4. chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 156 | tenue: non | pourquoi: résultats qui contredisent la consigne (ch. 10), flèches contradictoires (ch. 7), mots non expliqués (ライド, ロール, ベロシティ, グルーヴ, Fm/G♭)
promesse: id=t6-Da-5 | texte: 5. tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 133 | tenue: non | pourquoi: pas de français, mais du latin : « Groove », « Roland TR-909 », « Plaits », « Rim » ; « あとで » et « 少し下げる » coupés
oeil-neuf: q1 | ecran: 191 | image: 191 | les ▶ devant chaque partie et les −/+ ont l'air touchables, mais tous les −/+ sont grisés sans raison dite ; « チュートリアルを再開 » s'affiche alors que le tuto est fini
oeil-neuf: q6 | ecran: 191 | image: 191 | preuve claire (7 parties avec 16/16/32/12/4/32/16 小節, fichier m4a prêt), mais rien ne dit quoi faire ensuite à part « 再開 », trompeur
oeil-neuf: q9 | ecran: 191 | image: 191 | rien de coupé sauf les boutons « + A · グルーヴ » / « + B · イントロ » mangés par la barre du bas (on peut faire défiler)
hesitations:
  - ecran: 79 | cherchais: la dernière note pointillée de la mesure 4 (basse) | regarde: le bandeau « 点線の音があと1個、もっと上 ↑ » ; rien au-dessus, ni à droite ; « 見せて » repart à la mesure 1 sans finir
  - ecran: 4 | cherchais: pourquoi la tempo affiche 120 alors que j'ai choisi « テクノ 132 BPM » | regarde: le bouton « 120  テンポ »
  - ecran: 135 | cherchais: ce que veulent dire 16, 32, 12, 4 sous « エネルギー » | regarde: l'explication n'arrive qu'à l'étape suivante (« 数字はパートの長さ（小節数）。 »)
abandon: jamais
incidents-banc:
  - geste: 20 | attente: 0 | vu: après « 自分でやる » l'écran texte ne contient que la pastille REC (écrans 21, 22, 27, 28, 36, 37, 46, 47, 160, 161, 179, 184) ; l'image, elle, est normale ; outils hors human-act : Read sur les images du banc seulement
defauts:
  - geste: 78 | ecran.vu: 79 | cible: bouton « ベース · 小節 4/8 点線の音があと1個、もっと上 ↑ » | geste.type: touche
    attendu: la note pointillée restante visible, ou une flèche qui montre où elle est | obtenu: rien au-dessus (vérifié en faisant défiler), « 見せて » rejoue depuis la mesure 1 et s'arrête sans la poser ; seul un 2e « 自分でやる » l'a montrée (écran 93)
    symptome: perdu
    gravite: contourne-en-14-gestes | revu: oui: ecran 112 | banc?: non | promesse: t6-Da-2
  - geste: 111 | ecran.vu: 125 | cible: bouton « メロディ · 小節 1/8 点線の音があと1個、もっと下 ↓ » | geste.type: touche
    attendu: une seule direction juste pour la note restante | obtenu: pour les mêmes notes, « もっと上 ↑ » (112), puis « この先に → » (124), puis « もっと下 ↓ » (125) ; la note était en fait à droite
    symptome: texte-faux
    gravite: contourne-en-12-gestes | revu: oui: ecran 119 | banc?: non | promesse: t6-Da-4
  - geste: 155 | ecran.vu: 156 | cible: texte « パート「ドロップ 2」を作りました（グルーヴのコピー）：スタジオのバスドラム, 低いうなり, Roland TR-909 · オープンハイハット, テクノ · 太鼓のふち, ベース, メロディ なし。 » | geste.type: touche
    attendu: comme la consigne « 「コッ」以外は全部鳴る » | obtenu: le message dit que kick, basse, charleston… sont retirés ; même décalage aux étapes イントロ 2 (148 : « ベース, コード… なし » contre « ベースとスタブが入る ») et ビルドアップ (154 : clap « なし » puis clap qui accélère)
    symptome: incompris
    gravite: finition | revu: oui: ecran 154 | banc?: non | promesse: t6-Da-4
  - geste: 34 | ecran.vu: 35 | cible: texte « あなたの番：「手拍子たち」パッドで、点線のマスをタップしよう… » | geste.type: touche
    attendu: le nom d'un pad qui existe | obtenu: aucun pad « 手拍子たち » ; le pad s'appelle « テクノ · 手拍子 »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 132 | ecran.vu: 133 | cible: bouton « パートに「Groove」と名前を付ける » | geste.type: touche
    attendu: « グルーヴ » comme dans la consigne au-dessus | obtenu: « Groove » en lettres latines (la partie est ensuite bien nommée « グルーヴ »)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 179 | ecran.vu: 181 | cible: texte « 少し下げる » [coupé] | geste.type: glisse
    attendu: le bouton du conseil « 「Rim」が他を覆っています。 » visible | obtenu: le bouton orange est caché sous le panneau du tuto, seul son haut dépasse
    symptome: cache
    gravite: finition | revu: oui: ecran 181 | banc?: non | promesse: t6-Da-2
  - geste: 15 | ecran.vu: 16 | cible: texte « あとで » [coupé] | geste.type: touche
    attendu: le choix « あとで » lisible sous le bouton de téléchargement | obtenu: caché sous le fondu « 続きを読む »
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t6-Da-2
  - geste: 3 | ecran.vu: 4 | cible: bouton « 120  テンポ » | geste.type: touche
    attendu: 132 après avoir choisi « テクノ 132 BPM » | obtenu: projet à 120, le tuto me fait ensuite taper 132 moi-même
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 18 | cible: texte « 始める前に：キットはもうテクノ用（909 のドラムマシン）で… » | geste.type: touche
    attendu: des pads de la kit Techno | obtenu: le premier pad reste « スタジオのバスドラム », la kit affichée « キット：スタジオ » ; mélange « テクノ · », « Roland TR-909 · »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 164 | ecran.vu: 165 | cible: icône ▶ « F · ビルドアップ × 1  ·  1小節 » | geste.type: touche
    attendu: « どのパートも8小節で入る » comme le dit la consigne | obtenu: la ビルドアップ entre avec 1 小節
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
mots-faux:
  - ecran: 45 | vu: Roland TR-909 · オープンハイハット | attendu: オープンハイハット（909）
  - ecran: 62 | vu: Plaits | attendu: un nom en japonais
  - ecran: 180 | vu: Rim | attendu: 太鼓のふち
  - ecran: 148 | vu: « , » (virgule latine entre les noms) | attendu: 、

chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : hésitation (テンポ 120 alors que j'avais choisi 132 ; écoute sautée avec « 次へ », rien dit, chapitre compté)
  - chapitre 2 « キックとうなり » : hésitation (cases pointillées à moitié sous le panneau ; le texte dit kit Techno, l'écran dit スタジオ)
  - chapitre 3 « クラップ » : hésitation (pad « 手拍子たち » introuvable)
  - chapitre 4 « ハイハット » : hésitation (nom latin « Roland TR-909 » dans la consigne)
  - chapitre 5 « ベース » : bloqué (note restante introuvable malgré « もっと上 ↑ », 14 gestes de détour)
  - chapitre 6 « スタブ » : hésitation (titre « スタブ » expliqué seulement au chapitre 9 ; quelle note est « 下の »)
  - chapitre 7 « リフ » : bloqué (flèches ↑ → ↓ contradictoires pour la même note, 12 gestes de détour)
  - chapitre 8 « 最初のパート：グルーヴ » : hésitation (bouton « Groove » en latin ; l'écoute tourne en boucle sans fin)
  - chapitre 9 « パートを理解する » : hésitation (chiffres 16/32 expliqués une étape trop tard ; « ライド » jamais expliqué)
  - chapitre 10 « パートを作ろう » : hésitation (les messages de réussite disent l'inverse de la consigne)
  - chapitre 11 « パートを並べよう » : hésitation (Fm, G♭ non expliqués ; « 8小節 » mais ビルドアップ à 1)
  - chapitre 12 « トランジション » : compris du premier coup (étape 1 déjà faite, dite « もうできています！ »)
  - chapitre 13 « エフェクト » : compris du premier coup (écoutes sautées, comptées)
  - chapitre 14 « ミックス：音量 » : hésitation (bouton « 少し下げる » caché, « Rim »)
  - chapitre 15 « 曲を書き出そう » : compris du premier coup
coupe-verifiee: ecran 16 | image 16 | あとで | vraiment coupé
coupe-verifiee: ecran 20 | image 20 | cases pointillées 1 à 4 (ch. 2) | vraiment coupé
coupe-verifiee: ecran 181 | image 181 | 少し下げる | vraiment coupé
coupe-verifiee: ecran 21 | image 21 | écran texte vide après « 自分でやる » | rendu banc
defauts-hier:
  - H1 : corrigé (écoutes sautées aux ch. 1, 2, 4, 7, 9, 13 ; « チュートリアル完了：おめでとう！ » puis « おめでとう、曲が完成！ », écrans 188-189)
  - H2 : corrigé (liste des chapitres : 1 à 9 tous cochés, écran 143)
  - H3 : encore là (autre forme : « もっと上 ↑ » alors que la note est à droite ou hors vue, ch. 5 et 7 ; cases du ch. 2 à moitié sous le panneau)
  - H4 : encore là (ch. 2 corrigé « スタジオのバスドラム », ch. 3 toujours « 手拍子たち »)
  - H5 : autre (le tuto dit « テクノ · 太鼓のふち », mais « Rim » reste dans l'avertissement de la Mix)
  - H6 : corrigé (ch. 3 une seule étape, plus de « 2 小節にする »)
  - H7 : corrigé (ch. 6 et 7 ouverts sur 小節 1/8)
  - H8 : autre (le bouton de téléchargement est visible, « あとで » reste sous le fondu)
  - H9 : autre (ch. 5 en 3 étapes ; l'étape 3 garde 3 actions dans un paragraphe)
  - H10 : encore là (テクノ 132 → projet à 120)
  - H11 : corrigé (aucun 2e guide après avoir quitté puis fini le tuto)
  - H12 : autre (キー, 小節, 拍, リフ, スタブ, コード expliqués ; restent ライド, ロール, ベロシティ, グルーヴ, ゴーストベース, Fm/G♭)
  - H13 : autre (Perc disparu ; restent « Groove » sur un bouton, « Plaits », « Rim », et nouveau « Roland TR-909 »)
  - H14 : autre (909 expliqué ; mais pad « スタジオのバスドラム » et « キット：スタジオ » alors que le texte dit Techno)
  - H15 : autre (ch. 12 étape 1 toujours faite d'avance mais expliquée ; ch. 14 dit bien « ふつう » ; « 少し下げる » toujours coupé)
  - H16 : corrigé (en-tête du Bravo : « 曲を書き出そう チャプター 15/15 »)
hesitations-total: 17
