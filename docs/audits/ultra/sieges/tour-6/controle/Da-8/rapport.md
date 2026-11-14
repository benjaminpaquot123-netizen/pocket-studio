code testé : 16098b62
joué par : sous-agent neuf

siege: a | profil: t6-Da japonais-debutant-tuto-facile | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: おめでとう、曲が完成！ 全部聴き直して、まだ変えていけるよ。 | ecran: 174 | tenue: oui | pourquoi: les 15 chapitres se suivent jusqu'au « Bravo » sans impasse
promesse: id=t6-Da-2 | texte: タップする点線のノートがあと 2 個 | ecran: 83 | tenue: non | pourquoi: ch. 5 (et ch. 7, écran 105) la note pointillée annoncée est hors de la grille visible ; il faut le bouton « ここをタップ » pour la voir (2 gestes de plus)
promesse: id=t6-Da-3 | texte: 続ける シンプル マイプロジェクト 1 | ecran: 47 | tenue: oui | pourquoi: parti à l'Accueil au ch. 4, « 続ける » me remet au ch. 4 étape 1/3
promesse: id=t6-Da-4 | texte: ゴーストベース / リズム補正 50% | ecran: 67 | tenue: non | pourquoi: mots affichés pendant le tuto sans explication (aussi « Plaits », et « 小節 » utilisé dès l'écran 21 avant d'être défini au ch. 5)
promesse: id=t6-Da-5 | texte: パート「イントロ」を作りました（グルーヴのコピー）：低いうなり, 手拍子, ベース, コード, メロディ なし。 | ecran: 129 | tenue: oui | pourquoi: tout en japonais, rien en français, rien de coupé ; seulement des virgules latines (voir mots-faux)
oeil-neuf: q1 | ecran: 174 | image: 174 | touchable : シェア, M4A/WAV, マイファイル, 停止, やり直す, 閉じる, onglets ; le champ 曲名 à moitié caché sous M4A/WAV ne paraît plus touchable
oeil-neuf: q6 | ecran: 174 | image: 174 | preuve : coupe + « おめでとう、曲が完成！ », suite claire : « チュートリアルを閉じる » ou « やり直す »
oeil-neuf: q9 | ecran: 174 | image: 174 | le champ « 曲名 » (« マイソング ») est coupé, recouvert par la rangée M4A/WAV
hesitations:
  - ecran: 21 | cherchais: ce que veut dire « 小節 » dans « 拍：小節の中の「1、2、3、4」それぞれ » | regarde: pas défini avant le ch. 5 (écran 66)
  - ecran: 83 | cherchais: la 2e note pointillée annoncée (« あと 2 個 ») | regarde: une seule visible ; glisser puis « ここをタップ » (écrans 84-86)
  - ecran: 153 | cherchais: ce que demandait l'étape 1 du ch. 12 | regarde: « もうできています！…触るところはありません » sans dire quoi
abandon: jamais
incidents-banc:
  - geste: 22 | attente: 0 | vu: l'écran texte ne montre que la barre « フィードバックモード » (aussi écrans 29, 36, 48, 56, 143, 156, 163, 169) ; image lue à la place
defauts:
mots-faux:
  - ecran: 129 | vu: 低いうなり, 手拍子, ベース | attendu: 低いうなり、手拍子、ベース
  - ecran: 137 | vu: 最後の拍は鳴りません（alors que l'écran 136 disait 最後の1拍の無音は、チャプター 12 で作るよ） | attendu: une seule version
chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : hésitation (« 小節 », « ループ » pas encore expliqués)
  - chapitre 3 « クラップ » : compris du premier coup
  - chapitre 4 « ハイハット » : hésitation (après la reprise, la grille est plus bas : 2 touches à côté, écrans 48-49)
  - chapitre 5 « ベース » : hésitation (« ゴーストベース », « リズム補正 », « Plaits » ; note du saut hors écran)
  - chapitre 6 « スタブ（短いコード） » : compris du premier coup
  - chapitre 7 « リフ » : hésitation (notes hors écran, « この先に → ここをタップ » deux fois, écrans 105-111)
  - chapitre 8 « 最初のパート：グルーヴ、基本のループ » : compris du premier coup
  - chapitre 9 « パートを理解する » : compris du premier coup
  - chapitre 10 « パートを作ろう » : hésitation (« チャプター 12 で作るよ » puis « 最後の拍は鳴りません » tout de suite)
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション：パートからパートへのつなぎ » : hésitation (« もうできています！ » sans dire quoi ; bandeau « → スタジオのバスドラム » sur la grille de la cymbale)
  - chapitre 13 « エフェクト » : compris du premier coup
  - chapitre 14 « ミックス：音量 » : compris du premier coup
  - chapitre 15 « 曲をシェアしよう » : compris du premier coup
points:
  - P1 : corrigé (écran 163, « ⚠️ 「太鼓のふち」が他を覆っています。 ») ; un seul avertissement sur la Mix (écran 166), aucun pour la cymbale, les maracas ou le laser
  - P2 : corrigé (écran 118, « パートに「グルーヴ」と名前を付ける »)
  - P3 : encore là (écran 155, « → スタジオのバスドラム » au-dessus de la grille, qui est celle de « シンバル » : écran 157)
hesitations-total: 6
