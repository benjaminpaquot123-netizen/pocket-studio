code testé : dee3519df1694386b61d84269762b368f08029e8
joué par : sous-agent neuf
siege: a | profil: t8-t0c-b | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chapitre-3-fini, chapitre-4-commence, chapitre-4-etape-1
promesse: id=ecran-compteur | texte: ここに点線のマスがあと 3 個 | ecran: 5 | tenue: oui | pourquoi: 3→2→1 (écrans 5, 6) puis le bandeau disparaît à la dernière case ; idem chapitre 4 : 4→3→2→1 (écrans 15-17)
promesse: id=ecran-chapitre | texte: よくできました、章クリア！ | ecran: 11 | tenue: oui | pourquoi: « 完了 » + « 次の章 » ; un toucher ouvre « ハイハット チャプター 4/15 · ステップ 1/3 » (écran 13)
promesse: id=ecran-consigne | texte: あなたの番：「手拍子」パッドのこの最後のヒットを「ソフト」にしよう。 | ecran: 8 | tenue: non | pourquoi: aucun « ソフト » à l'écran ; le geste (長押し) n'est dit que dans « なぜ？ » (écran 9)
oeil-neuf: q1 | ecran: 72 | image: 72 | boutons pleins clairs (例を聴く, ダウンロード) ; « あとで », « ▸なぜ? » et « 次へ/前へ » ressemblent à du texte mais se touchent ; la grille grisée derrière le panneau a l'air inactive
oeil-neuf: q6 | ecran: 72 | image: 72 | preuve = barre de progression verte et « ステップ 1/2 » ; suite évidente : le gros bouton « 例を聴く »
oeil-neuf: q9 | ecran: 72 | image: 72 | « ▸なぜ? » et « • スタブ… » rognés au bord gauche du panneau ; « マイプロジェ… » coupé ; étiquette de piste « 小節 4/4 · シンバル » illisible sous le voile
hesitations:
  - ecran: 8 | cherchais: le bouton « ソフト » | regarde: rien à l'écran ; trouvé dans « なぜ？ » (écran 9) : « マスを長押しするとメニューが開く »
  - ecran: 20 | cherchais: le chapitre « ロール » / « ビルドアップ » | regarde: aucun titre ne le dit ; essayé 12, 10, 9, 11, 13 sans trouver une étape où je place la ロール
  - ecran: 41 | cherchais: ce que demandait l'étape « もうできています！ » | regarde: aucune consigne affichée, seulement « 触るところはありません »
abandon: ecran 72 | parce que la partie bonus (étape ロール dans ビルドアップ) est introuvable par les titres de chapitres ; les 3 buts sont atteints
incidents-banc:
  - geste: - | attente: 0 | vu: aucun ; aucun outil hors human-act.py sauf lecture des images et de mon rapport/journal dans mon dossier de siège
defauts:
  - geste: 7 | ecran.vu: 8 | cible: あなたの番：「手拍子」パッドのこの最後のヒットを「ソフト」にしよう。 | geste.type: touche
    attendu: la consigne dit le geste (長押し) qui fait apparaître « ソフト » | obtenu: « ソフト » absent de l'écran, geste caché dans « なぜ？ »
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: ecran-consigne
  - geste: 24 | ecran.vu: 25 | cible: ここに4回多い（取り消し線） | geste.type: touche
    attendu: phrase naturelle, ex. « 消すヒットがあと 4 個（取り消し線） » | obtenu: « 4回多い » n'a pas de sens clair
    symptome: texte-faux | gravite: finition | revu: oui: ecran 26 (« ここに3回多い ») | banc?: non | promesse: -
  - geste: 0 | ecran.vu: 1 | cible: クラップ チャプター 3/15 · ステップ 1/2 · かんたん | geste.type: touche
    attendu: un seul nom pour le même son | obtenu: chapitre et « なぜ？ » disent « クラップ », la pad et la consigne disent « 手拍子 » (écrans 1, 41, 61)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 61 | banc?: non | promesse: ecran-consigne
  - geste: 0 | ecran.vu: 1 | cible: texte « 手拍子 » [coupé] @ 86,218 | geste.type: touche
    attendu: le nom de la pad lisible au-dessus de la grille | obtenu: caché sous le bandeau « ここに点線のマスがあと 3 個 » (image 1)
    symptome: cache | gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 0 | ecran.vu: 1 | cible: ▸ なぜ？ | geste.type: touche
    attendu: « ▸ なぜ？ » entier | obtenu: la flèche et le début sont rognés par le bord gauche du panneau (images 1 et 72)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: 前の章で作るものがまだありません。 | geste.type: touche
    attendu: dire clairement ce qui manque et pourquoi | obtenu: phrase bancale, « 第4章へ » (et « 章 » ailleurs) au lieu de « チャプター » ; la « なぜ？ » parle d'un autre bouton : « ボタン：パート「イントロ 2」を作る » alors que le bouton dit « 足りないものを置く »
    symptome: incompris | gravite: finition | revu: oui: ecran 69 | banc?: non | promesse: ecran-consigne
  - geste: 22 | ecran.vu: 23 | cible: パート「ビルドアップ」を作りました（「A」のコピー）：スタジオのバスドラム, 低いうなり, オープンハイハットなし。 | geste.type: touche
    attendu: « 、 » japonais et des mots qui correspondent aux pads (pas de « スタジオのバスドラム ») | obtenu: virgules occidentales, mots qui ne sont sur aucune pad
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: あなたの番：「パート「ビルドアップ」を作る」をタップ。 | geste.type: touche
    attendu: guillemets imbriqués 『パート「ビルドアップ」を作る』 | obtenu: 「…「…」…」 difficile à lire (idem イントロ, ドロップ, エンディング)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 64 | banc?: non | promesse: -
mots-faux:
  - ecran: 3 | vu: ループを押し出す | attendu: ループに勢いをつける
  - ecran: 33 | vu: もう一度「次へ」をタッチ | attendu: もう一度「次へ」をタップ（ailleurs toujours « タップ »）
  - ecran: 11 | vu: 章クリア / 次の章 | attendu: チャプタークリア / 次のチャプター（l'en-tête dit « チャプター »）
  - ecran: 1 | vu: テクノ キットをダウンロード | attendu: テクノキットをダウンロード（sans espace）
defauts-avant:
  - t0c-ja-mots | corrigé | ecran: 1 | « テクノ キットをダウンロード（4 MB） » (ordre naturel, reste une espace) ; écran 57 « 「リムショット」 » à la place de « 太鼓のふち » ; écrans 1-17 « 点線のマス » (plus de « 点線の音 ») ; « ゴーストベース » : son ancien endroit non vu
  - t0c-derniere-rafale | pas atteint | ecran: - | jamais vu d'étape où je place moi-même la ロール ; vu seulement écran 60 « ビルドアップを聴こう：クラップがどんどん速くなり、最後に1拍だけ全部が黙る。 » (sans « 最後の連打 ») et écran 25 « 最後の拍にある「手拍子」パッドの4つのヒットを消そう » (correct)
