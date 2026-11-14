code testé : 0e250438b189b35c647ee6a0a3f2357099d79c77
joué par : sous-agent neuf

siege: a | profil: t6-Da | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: t6-Da-1, t6-Da-2
promesse: id=t6-Da-1 | texte: 「よくできました、章クリア！」 (chapitre 12/15 · 完了) | ecran: 141 | tenue: oui | pourquoi: chapitres 1 à 12 enchaînés avec les seuls boutons du tuto (次へ / 次のステップ / 次の章 / 自分でやる), jamais la liste des chapitres, aucune impasse
promesse: id=t6-Da-2 | texte: 「ここに点線のマスがあと 1 個」 / 「タップする点線のノートがあと 8 個」 | ecran: 140 | tenue: oui | pourquoi: à chaque étape le texte d'aide est lisible et la case/note pointillée ou le bouton montré est à l'écran ; quand la note est hors champ, le bandeau dit 「もっと上 ↑ ここをタップ」 / 「この先に →」 et y amène (écrans 69, 89, 93, 95)
oeil-neuf: q1 | ecran: 141 | image: 141 | touchable clair : case cochée, 埋める/すべて/1/2/1/4, 消去, longueurs 1小節–4, onglets ; 「よくできました、章クリア！」 avec ⌃ se voit touchable (dépliable)
oeil-neuf: q6 | ecran: 141 | image: 141 | preuve : case 1 allumée en bleu + bandeau vert « 章クリア » + barre de progression pleine ; suite : dérouler le bandeau (次の章)
oeil-neuf: q9 | ecran: 141 | image: 141 | rien de coupé ni chevauché à 100 % ; en haut on lit « イントロ 小節 1/128 » alors que la grille est « パート：D · ドロップ » (lecture de position du morceau, pas une coupe)
hesitations:
  - ecran: 89 | cherchais: la 3e note du 1er小節 (au bord droit, à moitié visible) | regarde: 「ここをタップ」 m'a envoyé au 小節 2 avant de finir le 1er, puis m'a ramené au 1 (écran 95-96) : on s'y retrouve mais l'ordre surprend
  - ecran: 128 | cherchais: les boutons 「+ B · イントロ」 etc. | regarde: le texte de l'écran était vide (banc), l'image montrait bien la liste encadrée ; suivi à l'image
  - ecran: 122 | cherchais: si le silence du dernier temps de la ビルドアップ restait à faire | regarde: ch.10 dit 「最後の1拍の無音は、チャプター 12 で作るよ」 mais le succès dit déjà 「最後の拍は鳴りません」 et le ch.12 étape 1 dit 「もうできています！」 : léger doute, pas bloquant
abandon: jamais
incidents-banc:
  - geste: 127 | attente: 0 | vu: écrans 128 et 129 rendus en texte sans aucun élément sauf la pastille de retours ; l'image était normale ; revenu seul à l'écran 130
defauts:
mots-faux:
  - ecran: 114 | vu: 「低いうなり, 手拍子, ベース, コード, メロディ なし」 (virgules ASCII) | attendu: 「低いうなり、手拍子、ベース、コード、メロディ なし」
chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : compris du premier coup
  - chapitre 3 « クラップ » : compris du premier coup
  - chapitre 4 « ハイハット » : compris du premier coup
  - chapitre 5 « ベース » : hésitation (dernière note hors champ au-dessus ; résolue par 「もっと上 ↑ ここをタップ」)
  - chapitre 6 « スタブ（短いコード） » : compris du premier coup
  - chapitre 7 « リフ » : hésitation (la note au bord droit du 小節 1, le guide saute au 小節 2 puis revient)
  - chapitre 8 « 最初のパート：グルーヴ、基本のループ » : compris du premier coup
  - chapitre 9 « パートを理解する » : compris du premier coup
  - chapitre 10 « パートを作ろう » : compris du premier coup
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション：パートからパートへのつなぎ » : compris du premier coup
points:
  - P3 : corrigé (écran 140, le bandeau au-dessus de la grille dit 「シンバル」「ここに点線のマスがあと 1 個」, plus « → スタジオのバスドラム »)
  - P4 : oui (écran 33, appui-long #32 sur la dernière case vide 「空、点線：タップしてね」 : le menu 「ヒット 16」 avec 「ベロシティ」 強め/ふつう/ソフト s'ouvre tout de suite ; 「ソフト」 → 「ヒット、強さ 20 %」 et chapitre clair, écran 34)
hesitations-total: 3
