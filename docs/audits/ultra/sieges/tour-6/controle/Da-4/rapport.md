code testé : e4286b40
joué par : sous-agent neuf
siege: a | profil: t6-Da japonais-debutant-complet | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: « チュートリアル完了：おめでとう！ » | ecran: 166 | tenue: oui | pourquoi: les 15 chapitres se suivent sans impasse jusqu'à « おめでとう、曲が完成！ » (écran 167)
promesse: id=t6-Da-2 | texte: « ここに点線のマスがあと 2 個 » | ecran: 22 | tenue: non | pourquoi: chap. 2, les cases pointillées restantes sont cachées sous le panneau du tuto, la ligne n'a pas de flèche et la toucher ne fait rien ; chap. 12 la ligne nomme le mauvais pad (écran 149)
promesse: id=t6-Da-3 | texte: « ハイハット チャプター 4/15 · ステップ 2/3 · かんたん » | ecran: 52 | tenue: oui | pourquoi: Retour vers l'accueil puis « 続ける » : le tuto reprend exactement à l'étape quittée
promesse: id=t6-Da-4 | texte: « もうできています！ » | ecran: 147 | tenue: non | pourquoi: l'écran dit « rien à toucher » et en même temps « あなたの番：…消そう » ; mots « ベロシティ / ロール / 3連符 » (écran 41) et le titre « スタブ » (écran 84) jamais expliqués au moment voulu
promesse: id=t6-Da-5 | texte: « パートに「Groove」と名前を付ける » | ecran: 112 | tenue: non | pourquoi: mots anglais non traduits « Groove » et « Rim » (écran 157) au milieu du japonais
oeil-neuf: q1 | ecran: 167 | image: 167 | boutons pleins et à contour bien lisibles ; « マイファイル » et « ? » touchables comme attendu ; rien de trompeur
oeil-neuf: q6 | ecran: 167 | image: 167 | preuve claire : trophée + « おめでとう、曲が完成！ » ; suite proposée : « チュートリアルを閉じる » / « やり直す »
oeil-neuf: q9 | ecran: 167 | image: 167 | le champ « 曲名 » est à moitié recouvert par les boutons M4A/WAV (texte « マイソング » coupé)
hesitations:
  - ecran: 22 | cherchais: les 2 cases pointillées restantes (« あと 2 個 ») | regarde: aucune flèche, toucher la ligne ne fait rien ; il a fallu replier le panneau (⌄)
  - ecran: 38 | cherchais: pourquoi l'étape n'est pas réussie alors que toutes les cases sont posées | regarde: aucun message « il manque ソフト » ; seulement « 手拍子 » et la consigne répétée
  - ecran: 147 | cherchais: quoi faire : « もうできています／触るところはありません » mais « あなたの番：…消そう » | regarde: j'ai fait « 次のステップ » au hasard
abandon: jamais
incidents-banc:
  - geste: 52 | attente: 0 | vu: écran 53 en texte vide (seul le pied « REC/メモ ») ; l'image montrait la grille, joué sur l'image
  - geste: 136 | attente: 0 | vu: idem écrans 137, 138, 157, 162 (texte vide, image correcte)
  - geste: - | attente: 0 | vu: outils hors human-act.py : Read des consignes (demandé), Read des images du banc, Read/Write de rapport.md
defauts:
  - geste: 22 | ecran.vu: 23 | cible: texte « ここに点線のマスがあと 2 個 » | geste.type: touche
    attendu: la ligne indique où sont les cases restantes et la toucher m'y amène | obtenu: pas de flèche, rien ne bouge, cases cachées sous le panneau du tuto
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 29 | banc?: non | promesse: t6-Da-2
  - geste: 37 | ecran.vu: 40 | cible: texte « 手拍子 » | geste.type: touche
    attendu: cases posées mais pas « ソフト » : l'écran dit ce qui manque (T3) | obtenu: aucun message, panneau rouvert sur la même consigne
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: oui: ecran 40 | banc?: non | promesse: t6-Da-4
  - geste: 146 | ecran.vu: 147 | cible: texte « もうできています！ » | geste.type: touche
    attendu: une seule consigne claire | obtenu: « rien à toucher » + « あなたの番：赤い×をタップ » sur le même écran
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 148 | ecran.vu: 149 | cible: texte « → スタジオのバスドラム » | geste.type: touche
    attendu: la ligne nomme le pad シンバル demandé | obtenu: elle nomme la grosse caisse ; c'était bien la grille シンバル (écran 151)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 151 | banc?: non | promesse: t6-Da-2
  - geste: 17 | ecran.vu: 18 | cible: texte « 始める前に：キットはもうテクノ用（909 のドラムマシン）… » | geste.type: touche
    attendu: le kit affiché correspond | obtenu: le bouton dit « キット：スタジオ » et la carte propose de télécharger « キット テクノ »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 40 | ecran.vu: 41 | cible: texte « ベロシティ » | geste.type: appui-long
    attendu: mots simples en Mode simple | obtenu: « ベロシティ », « ロール », « 3連符 » sans explication
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 83 | ecran.vu: 84 | cible: texte « スタブ チャプター 6/15 · ステップ 1/2 · かんたん » | geste.type: touche
    attendu: le titre est expliqué dans le chapitre | obtenu: « スタブ » n'est expliqué qu'au chapitre 9 (écran 116)
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 111 | ecran.vu: 112 | cible: bouton « パートに「Groove」と名前を付ける » | geste.type: touche
    attendu: « グルーヴ » comme dans la consigne | obtenu: mot anglais « Groove »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 156 | ecran.vu: 157 | cible: texte « ⚠️ 「Rim」が他を覆っています。 » | geste.type: touche
    attendu: « 太鼓のふち » | obtenu: mot anglais « Rim »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 158 | banc?: non | promesse: t6-Da-5
  - geste: 5 | ecran.vu: 6 | cible: texte « キット テクノ » | geste.type: touche
    attendu: carte lisible | obtenu: la carte « キット テクノ » est coupée et recouverte par « ▸ なぜ？ »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Da-2
mots-faux:
  - ecran: 112 | vu: Groove | attendu: グルーヴ
  - ecran: 157 | vu: Rim | attendu: 太鼓のふち
  - ecran: 162 | vu: 「共有」で | attendu: 「シェア」で (le bouton dit シェア)
  - ecran: 123 | vu: 低いうなり, 手拍子, ベース | attendu: 低いうなり、手拍子、ベース
chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : hésitation (cases restantes cachées sous le panneau, ligne sans flèche ; « キット：スタジオ » contredit « キットはもうテクノ用 »)
  - chapitre 3 « クラップ » : hésitation (rien ne dit qu'il manque « ソフト » ; « 手拍子たち » vs pad « 手拍子 »)
  - chapitre 4 « ハイハット » : compris du premier coup
  - chapitre 5 « ベース » : hésitation (« コピー : 2小節目と3小節目が埋まる » avec un seul toucher, pas clair ; « ゴーストベース », « リズム補正 » inexpliqués)
  - chapitre 6 « スタブ » : hésitation (titre « スタブ » inexpliqué)
  - chapitre 7 « リフ » : compris du premier coup (flèches → ↑ ← justes)
  - chapitre 8 « 最初のパート：グルーヴ » : compris du premier coup (bouton « Groove » en anglais)
  - chapitre 9 « パートを理解する » : compris du premier coup (« ライド » inexpliqué)
  - chapitre 10 « パートを作ろう » : compris du premier coup
  - chapitre 11 « パートを並べよう » : compris du premier coup (phrase « エンディングで終わるボタンを順に » lourde)
  - chapitre 12 « トランジション » : hésitation (« もうできています » + « あなたの番 » ; ligne « → スタジオのバスドラム » au lieu de シンバル)
  - chapitre 13 « エフェクト » : compris du premier coup (écoute seule)
  - chapitre 14 « ミックス：音量 » : compris du premier coup (« Rim » en anglais)
  - chapitre 15 « 曲を書き出そう » : compris du premier coup
coupe-verifiee: ecran 6 | image 6 | キット テクノ | vraiment coupé
coupe-verifiee: ecran 12 | image 12 | • キー：曲の始まりの音と、その明るさや暗さ。 | vraiment coupé (ligne à demi masquée sous « ▸ なぜ？ », « 続きを読む » présent)
coupe-verifiee: ecran 167 | image 167 | 曲名 « マイソング » | vraiment coupé
defauts-avant:
  - T4 : corrigé (écrans 80, 100, 104, 106) : chap. 5 « もっと上 ↑ » juste (note une octave plus haut), 1 geste de recherche ; chap. 7 « この先に → », « もっと上 ↑ », « ← この前に » justes, toucher la ligne m'y amène chaque fois, 3 gestes de recherche au total (1 par note). Hors chap. 5/7 : au chap. 2 (écran 22) la ligne n'a ni flèche ni effet au toucher, 2 gestes de recherche
  - T3 : encore là (écrans 38 et 40 : aucun message sur « ソフト » manquant)
hesitations-total: 9
