code testé : 307759b3
joué par : sous-agent neuf

siege: a | profil: t6-Da japonais-debutant-complet | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 188 | tenue: oui | pourquoi: 15/15 chapitres faits, « おめでとう、曲が完成！ » à l'écran 188, aucune impasse
promesse: id=t6-Da-2 | texte: À chaque étape du tuto, le texte d'aide est lisible et l'élément montré est bien à l'écran | ecran: 108 | tenue: non | pourquoi: chap. 5 (écran 80) et surtout chap. 7 (écrans 108-122) : des notes pointillées demandées sont hors de l'écran, il faut faire défiler la grille à l'aveugle
promesse: id=t6-Da-3 | texte: Je peux quitter le tuto puis le reprendre là où je l'avais laissé | ecran: 49 | tenue: oui | pourquoi: Retour à l'accueil (48) puis « 続ける » : reprise exacte chap. 4 étape 1/3 (49)
promesse: id=t6-Da-4 | texte: chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 164 | tenue: non | pourquoi: chap. 12 dit « もうできています！…触るところはありません » et en même temps « あなたの番：…赤い×をそれぞれタップ » ; « Rim » (177) jamais expliqué
promesse: id=t6-Da-5 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 128 | tenue: non | pourquoi: pas de français, mais de l'anglais : bouton « パートに「Groove」と名前を付ける » (128), « ⚠️ 「Rim」が他を覆っています。 » (177)
oeil-neuf: q1 | ecran: 188 | image: 188 | tout ce qui a l'air touchable l'est (onglets, シェア, M4A/WAV, マイファイル, ?, 停止, やり直す, 閉じる) ; rien de trompeur vu
oeil-neuf: q6 | ecran: 188 | image: 188 | preuve claire : trophée + « おめでとう、曲が完成！ » ; suite claire : « チュートリアルをやり直す » ou « チュートリアルを閉じる »
oeil-neuf: q9 | ecran: 188 | image: 188 | le champ 曲名 est coupé par le haut : « マイソング » à moitié caché sous le bord
hesitations:
  - ecran: 35 | cherchais: la pad « 手拍子たち » demandée | regarde: la pad s'appelle « 手拍子 FX », le chapitre s'appelle « クラップ » : j'ai deviné que c'était la même
  - ecran: 108 | cherchais: les 3 notes du 1er小節 et les 4 du 2e | regarde: seules 2 visibles, le reste hors écran (à droite, plus haut, puis « この前に ») ; trouvé grâce aux flèches du compteur, 10 glissés
  - ecran: 164 | cherchais: si je dois toucher quelque chose | regarde: « もうできています！ » et « あなたの番 » sur la même carte ; j'ai fait « 次のステップ »
abandon: jamais
incidents-banc:
defauts:
  - geste: 108 | ecran.vu: 108 | cible: bouton « メロディ · 小節 1/8 点線のノートがあと 5 個、この先に → » | geste.type: glisse
    attendu: les notes pointillées du chapitre 7 (« 1小節目に3つ、2小節目に4つ ») visibles, ou la grille qui se place seule dessus | obtenu: 3 notes hors écran (à droite, une octave plus haut, puis revenir en arrière) ; 10 glissés (#108 à #121) guidés par « この先に → », « もっと上 ↑ », « ← この前に » avant la dernière note (122)
    symptome: cache | gravite: contourne-en-6-gestes | revu: non | banc?: non | promesse: t6-Da-2
mots-faux:
  - ecran: 35 | vu: 手拍子たち | attendu: 手拍子
  - ecran: 128 | vu: Groove | attendu: グルーヴ
  - ecran: 177 | vu: Rim | attendu: 太鼓のふち
  - ecran: 183 | vu: 共有 | attendu: シェア
chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : compris du premier coup
  - chapitre 3 « クラップ » : hésitation (« 「手拍子たち」パッドで » alors que la pad dit « 手拍子 FX » et le titre « クラップ »)
  - chapitre 4 « ハイハット » : compris du premier coup
  - chapitre 5 « ベース » : hésitation (« あと 8 個 » mais 7 visibles ; la 8e au-dessus de l'écran, trouvée par « もっと上 ↑ », 2 glissés)
  - chapitre 6 « スタブ » : compris du premier coup
  - chapitre 7 « リフ » : hésitation (notes pointillées hors écran, 10 glissés pour les trouver)
  - chapitre 8 « 最初のパート：グルーヴ » : hésitation (le texte dit nommer « グルーヴ », le bouton dit « Groove »)
  - chapitre 9 « パートを理解する » : hésitation (« ブレイク：…ドラムが止まります » alors que hi-hat et « 太鼓のふち » restent ; les chiffres 16/32 de la carte ne sont expliqués qu'à l'étape 2)
  - chapitre 10 « パートを作ろう » : hésitation (« 最後の1拍の無音は、チャプター 12 で作るよ » mais le succès dit déjà « 最後の拍は鳴りません »)
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション » : hésitation (« もうできています！ » + « あなたの番：…赤い×をそれぞれタップ », combien de × non dit)
  - chapitre 13 « エフェクト » : compris du premier coup
  - chapitre 14 « ミックス：音量 » : compris du premier coup
  - chapitre 15 « 曲を書き出そう » : hésitation (« 「共有」で… » au-dessus d'un bouton « シェア » ; « （−14） » non expliqué)
defauts-avant:
  - M1 : encore là (écran 35, « 「手拍子たち」パッドで ») ; le titre de grille, lui, est corrigé (écran 40 « 手拍子 », 26 « スタジオのバスドラム »)
  - M2 : encore là (écran 177, « 「Rim」が他を覆っています » sans explication ; écran 92, « スタブ » nommé avant d'être expliqué au 93) ; ベロシティ/ロール/3連符 (35), ベース (4), Groove/グルーヴ (126), ブレイク (134), トランジション (164) expliqués
  - M3 : corrigé (écrans 26, 78, 97, 186 : compteurs justes, 16+16+32+12+4+32+16 = 3:53 cohérent) ; autre : au 80 le compteur dit 8 mais une note est hors écran
  - M4 : encore là (écran 134, « ブレイク：静かになる部分。ドラムが止まります。 » avec « スタブがハイハットと「太鼓のふち」といっしょに残る » ; écran 164, « もうできています！ » avec « あなたの番 »)
  - M5 : encore là (écran 164, chap. 12 « 赤い×をそれぞれタップ » sans nombre ; écran 183, « （−14） » non expliqué) ; chap. 14 corrigé (écran 175 explique « −2 », « −4 », « −∞ »)
  - M7 : encore là (écran 183, « 「共有」でプロジェクト「マイプロジェクト 1」の名前も変わります。 » à côté du bouton « シェア »)
hesitations-total: 8
