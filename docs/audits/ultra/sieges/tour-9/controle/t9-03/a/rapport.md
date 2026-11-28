code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: confirme | lang: ja | device: tablet8 | font: 100 | mode: pro
mission: tuto acid_techno chapitres 1 à 5 | buts: non donnés par le brief
promesse: id=- | texte: あなたの番：「アシッド」タブを開いて、2小節のラインを1マスずつ書こう | ecran: 58 | tenue: non | pourquoi: ligne écrite marche par marche, étape jamais validée, aucun indice
hesitations:
  - ecran: 53 | cherchais: le bouton pour passer à l'étape suivante | regarde: le bouton sous mon doigt devient 試聴を停止, 次のステップ n'arrive qu'à la fin de l'exemple
  - ecran: 58 | cherchais: quelles notes sont déjà écrites dans la grille 303 | regarde: notes guides en trait plein, seul l'éditeur de pas dit 休符
  - ecran: 110 | cherchais: ce qui manque dans ma ligne 303 | regarde: 見せて rejoue la démo puis revient au même état, sans dire quoi corriger
abandon: ecran 114 | parce que l'étape 2 du chapitre 3 ne se valide pas, je saute avec 次へ ×2
incidents-banc:
defauts:
  - geste: #111 | ecran.vu: 111 | cible: 303 チャプター 3/12 · ステップ 2/2 · プロ | geste.type: touche
    attendu: ligne de 2 mesures écrite comme demandé (A, octave, C, G, E, accents, slides, B♭, dernière case vide) → étape cochée | obtenu: rien ne se coche, aucun message sur ce qui ne va pas ; seul 次へ ×2 permet de sauter, le chapitre 3 reste « 1/2 »
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: oui: ecran 110 (見せて puis réessai) | banc?: non | promesse: -
  - geste: #56 | ecran.vu: 57 | cible: Acid 303 | geste.type: touche
    attendu: un onglet « アシッド » comme dit le tuto | obtenu: l'onglet s'appelle « Acid 303 ベースライン »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #105 | ecran.vu: 105 | cible: A# | geste.type: touche
    attendu: une touche B♭ comme dit le tuto | obtenu: le clavier n'affiche que A#, il faut savoir que c'est la même note
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #43 | ecran.vu: 43 | cible: ステップ：シンバル | geste.type: touche
    attendu: la piste « ライド » annoncée par le tuto (ドロップ用のライド) | obtenu: elle s'appelle « シンバル »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #20 | ecran.vu: 20 | cible: わかった | geste.type: touche
    attendu: un kick « crunch » 909 et un 303 vide, comme dit l'intro du chapitre 2 | obtenu: le pad dit « スタジオキック », et la grille 303 montre déjà des notes en trait plein (休符 seulement dans l'éditeur)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #60 | ecran.vu: 60 | cible: ステップ 29 | geste.type: glisse
    attendu: octave et interrupteurs ノート/アクセント/スライド visibles avec la grille | obtenu: cachés sous le panneau du tuto, il faut faire défiler à chaque pas ; impossible de voir grille et octave ensemble
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 65 | banc?: non | promesse: -
  - geste: #58 | ecran.vu: 58 | cible: あなたの番：「アシッド」タブを開いて、2小節のラインを1マスずつ書こう | geste.type: touche
    attendu: la consigne lisible en entier | obtenu: coupée dans le panneau replié, il faut l'ouvrir pour lire la fin (B♭, dernière case vide)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #32 | ecran.vu: 32 | cible: ステップ：ROLAND TR-909 · オープンハイハット | geste.type: touche
    attendu: titre de piste lisible, écrit comme le pad (Roland TR-909) | obtenu: majuscules « ROLAND », texte très pâle sur le fond (contraste faible), idem « ステップ：シンバル »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #4 | ecran.vu: 13 | cible: ベースライン · ルート A | geste.type: touche
    attendu: titre de la grille ouverte visible | obtenu: titre coupé derrière le panneau du tuto
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 57 | vu: Acid 303 | attendu: アシッド
  - ecran: 105 | vu: A# | attendu: B♭
  - ecran: 43 | vu: シンバル | attendu: ライド
defauts-avant:
  - aucun défaut visé donné par le brief | pas atteint | ecran: - | rien à contrôler
chapitres:
  - 1 テンポとキー : compris du premier coup
  - 2 ドラム : compris du premier coup
  - 3 303 : bloqué (étape 2 jamais validée, sautée par 次へ ; reste « 1/2 » dans la liste)
  - 4 最初のパート：グルーヴ : compris du premier coup
  - 5 パートを理解する : compris du premier coup
gestes: 136/153, budget non épuisé

fin-tranche: non (dernier step=map_phrases, done=18)
