code testé : b4ac25f2
joué par : sous-agent neuf

siege: a | profil: t6-Da | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Da-1 | texte: Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 176 | tenue: oui | pourquoi: 15 chapitres faits, « おめでとう、曲が完成！ » atteint en ~175 gestes, aucune impasse
promesse: id=t6-Da-2 | texte: À chaque étape du tuto, le texte d'aide est lisible et l'élément montré est bien à l'écran | ecran: 20 | tenue: non | pourquoi: ch.2 cases pointillées à moitié sous le panneau ; ch.14 « 少し下げる » sous le panneau (écran 165)
promesse: id=t6-Da-3 | texte: Je peux quitter le tuto puis le reprendre là où je l'avais laissé | ecran: 37 | tenue: oui | pourquoi: Retour à l'accueil au ch.3, « 続ける » → même étape « クラップ 3/15 · ステップ 1/1 »
promesse: id=t6-Da-4 | texte: chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 35 | tenue: non | pourquoi: pad « 手拍子たち » introuvable ; ベロシティ, ロール, ライド, グルーヴ, Fm/G♭ jamais expliqués
promesse: id=t6-Da-5 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 165 | tenue: non | pourquoi: « Groove », « Rim », « Plaits » en lettres latines ; « あとで » et « 少し下げる » coupés
oeil-neuf: q1 | ecran: 176 | image: 176 | touchables clairs (自分の曲を聴く, やり直す, 閉じる, シェア) ; le champ du nom du morceau, à moitié caché, ne semble plus touchable
oeil-neuf: q6 | ecran: 176 | image: 176 | preuve nette (coupe + « おめでとう、曲が完成！ ») et suite proposée (écouter, refaire, fermer)
oeil-neuf: q9 | ecran: 176 | image: 176 | champ « 曲名 » : contenu « マイソング » coupé/recouvert par les boutons M4A/WAV
hesitations:   (3 au plus)
  - ecran: 20 | cherchais: les 4 cases pointillées à taper | regarde: seules 2 cases visibles, à moitié sous le panneau du tuto
  - ecran: 35 | cherchais: la pad « 手拍子たち » | regarde: aucune pad de ce nom, seulement « 手拍子 »
  - ecran: 157 | cherchais: la pad シンバル | regarde: la grille titrée « → スタジオのバスドラム » alors qu'on parle de cymbale
abandon: jamais
incidents-banc:
  - geste: 20 | attente: 2 | vu: après « 自分でやる », écran vide (seule la pastille REC) un geste, puis l'écran revient (idem gestes 27, 37, 46, 53, 143, 163, 169)
defauts:   (10 au plus)
  - geste: 34 | ecran.vu: 35 | cible: texte « あなたの番：「手拍子たち」パッドで、点線のマスをタップしよう… » | geste.type: touche
    attendu: le nom de la pad tel qu'à l'écran (« 手拍子 ») | obtenu: « 手拍子たち », aucune pad de ce nom
    symptome: texte-faux | gravite: finition | revu: oui: ecran 37 | banc?: non | promesse: t6-Da-4
  - geste: 17 | ecran.vu: 18 | cible: texte « 始める前に：キットはもうテクノ用（909 のドラムマシン）で… » | geste.type: touche
    attendu: le texte colle à l'écran | obtenu: l'écran montre « キット：スタジオ » et « スタジオのバスドラム » (kit Techno pas téléchargé)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-2
  - geste: 19 | ecran.vu: 20 | cible: bouton « 1 » [coupé, état « 空、点線：タップしてね »] | geste.type: touche
    attendu: les 4 cases pointillées visibles au-dessus du panneau | obtenu: 1re rangée coupée à mi-hauteur, 2e rangée cachée ; il faut « 自分でやる »
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 27 | banc?: non | promesse: t6-Da-2
  - geste: 16 | ecran.vu: 17 | cible: texte « あとで » [coupé] | geste.type: touche
    attendu: « あとで » lisible sous le bouton de téléchargement | obtenu: masqué par le fondu « 続きを読む » (idem écrans 87, 98, 120…)
    symptome: cache | gravite: finition | revu: oui: ecran 87 | banc?: non | promesse: t6-Da-5
  - geste: 118 | ecran.vu: 119 | cible: bouton ⊕ «   パートに「Groove」と名前を付ける » | geste.type: touche
    attendu: « グルーヴ » comme dans la consigne juste au-dessus | obtenu: « Groove » en lettres latines
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 163 | ecran.vu: 165 | cible: texte « ⚠️ 「Rim」が他を覆っています。 » | geste.type: touche
    attendu: le nom de la pad en japonais (« 太鼓のふち », comme au ch.4) | obtenu: « Rim »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Da-5
  - geste: 163 | ecran.vu: 165 | cible: texte « 少し下げる » [coupé] | geste.type: touche
    attendu: le bouton de l'alerte visible | obtenu: seul le haut orange dépasse, le reste est sous le panneau du tuto
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: t6-Da-2
  - geste: 103 | ecran.vu: 111 | cible: bouton « メロディ · 小節 1/8 点線の音があと5個、もっと上 ↑ » | geste.type: touche
    attendu: la note pointillée du bord droit (ch.7) entière, et la toucher la pose | obtenu: un liseré seulement ; le toucher pose une note parasite en fin de mesure 1, compteur inchangé
    symptome: perdu | gravite: contourne-en-6-gestes | revu: non | banc?: non | promesse: t6-Da-2
  - geste: 156 | ecran.vu: 157 | cible: texte « → スタジオのバスドラム » | geste.type: touche
    attendu: le titre de la grille dit « シンバル » (la consigne parle de la pad cymbale) | obtenu: « → スタジオのバスドラム » ; « シンバル » n'apparaît qu'après la frappe
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
  - geste: 42 | ecran.vu: 43 | cible: texte « ベロシティ » | geste.type: appui-long
    attendu: un mot simple ou expliqué (la consigne dit « ソフト ») | obtenu: « ベロシティ », « ロール », « 3連符 » jamais expliqués
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Da-4
mots-faux:
  - ecran: 65 | vu: ベース Plaits | attendu: un nom japonais
  - ecran: 124 | vu: ライド（non expliqué） | attendu: mot expliqué
  - ecran: 146 | vu: Fm / G♭（non expliqués） | attendu: mot expliqué
  - ecran: 65 | vu: ゴーストベース / リズム補正 50%（non expliqués） | attendu: mot expliqué
  - ecran: 88 | vu: titre « スタブ » (ch.6), expliqué seulement au ch.9 | attendu: expliqué dès le ch.6

chapitres:
  - chapitre 1 « 曲の速さと雰囲気 » : compris du premier coup
  - chapitre 2 « キックとうなり » : hésitation (cases à moitié sous le panneau ; le texte dit kit Techno, l'écran montre スタジオ)
  - chapitre 3 « クラップ » : hésitation (pad « 手拍子たち » introuvable)
  - chapitre 4 « ハイハット » : compris du premier coup
  - chapitre 5 « ベース » : hésitation (dernière note très au-dessus, « もっと上 » juste mais 2 glissés sans repère ; le texte dit « 1段上がって »)
  - chapitre 6 « スタブ » : hésitation (mesure 4 : quel pointillé est « 下の » ? titre « スタブ » non expliqué)
  - chapitre 7 « リフ » : hésitation (note du bord droit en liseré, note parasite posée, puis 上/→/下 à suivre)
  - chapitre 8 « 最初のパート：グルーヴ » : hésitation (bouton « Groove » latin, グルーヴ non expliqué)
  - chapitre 9 « パートを理解する » : hésitation (« ライド » non expliqué)
  - chapitre 10 « パートを作ろう » : compris du premier coup
  - chapitre 11 « パートを並べよう » : compris du premier coup
  - chapitre 12 « トランジション » : hésitation (étape 1 « もうできています » suivi d'un « あなたの番 » ; grille cymbale titrée バスドラム)
  - chapitre 13 « エフェクト » : compris du premier coup
  - chapitre 14 « ミックス：音量 » : hésitation (alerte « Rim » en latin, « 少し下げる » caché)
  - chapitre 15 « 曲を書き出そう » : compris du premier coup
coupe-verifiee: ecran 17 | image 17 | あとで | vraiment coupé
coupe-verifiee: ecran 20 | image 20 | cases pointillées de la 1re rangée | vraiment coupé
coupe-verifiee: ecran 6 | image 6 | carte « キット テクノ » sous le fondu | vraiment coupé
coupe-verifiee: ecran 101 | image 101 | note pointillée au bord droit de la grille mélodie | vraiment coupé
coupe-verifiee: ecran 165 | image 165 | 少し下げる | vraiment coupé
coupe-verifiee: ecran 176 | image 176 | champ « マイソング » | vraiment coupé
defauts-avant:
  - C1 : corrigé (écran 82 « もっと上 ↑ » juste : la note était au-dessus, trouvée en 2 glissés)
  - C2 : autre (les flèches 上 / → / 下 étaient justes pour chaque note ; mais la note du bord droit n'est qu'un liseré et la toucher pose une note parasite)
  - C3 : corrigé (écrans 130 à 142 : les messages collent aux consignes, ex. « ドロップ 2 … 太鼓のふち なし »)
  - C4 : encore là (écran 35)
  - C5 : encore là (écran 119)
  - C6 : encore là (écran 165)
  - C7 : encore là (écrans 17, 87, 120 ; visible seulement écran 61)
  - C8 : corrigé (écran 6 : « 新しい曲は 120 BPM から始まる。スタイルの速さにするのはあなた。 »)
  - C9 : encore là (écran 18 : « キットはもうテクノ用 » ; écran 11 « キット：スタジオ »)
  - C10 : corrigé (écran 150 : ビルドアップ entre avec 4 小節, comme la carte ; consigne « 長さはまだ仮のもの »)
  - C11 : encore là (écran 20)
  - C12 : autre (Rim traduit « 太鼓のふち » dans le tuto mais « Rim » reste dans l'alerte de la Mixe ; ライド, ロール, ベロシティ, グルーヴ, Fm/G♭, Plaits encore là ; 909 expliqué ; TR-909 pas vu)
verifications:
  - après « 次の小節にコピー » (écran 75) : l'écran se pose sur « 小節 4/8 タップする点線のノートがあと 8 個 » : on sait quoi faire
  - note au bord droit : batterie et basse entières (écrans 44, 84) ; mélodie : liseré seulement (écran 101)
  - « コード » reste allumé pour la piste コード (écran 94) et s'éteint pour la mélodie (écran 101) : logique
  - étape tempo (écran 6) : claire, BPM expliqué, 120 de départ justifié ; « 1分間に132拍 » bien dit
hesitations-total: 7
