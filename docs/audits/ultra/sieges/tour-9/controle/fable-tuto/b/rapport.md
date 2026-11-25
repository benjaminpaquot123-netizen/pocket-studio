code testé : d87dea73ff18887bc55b2ac5e0987f5e17be4c21
joué par : sous-agent neuf
siege: b | profil: debutant-motive | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=- | texte: ベース · 小節 4/8 ↑ タップする点線のノートがあと 1 個 ここをタップ | ecran: 48 | tenue: non | pourquoi: il restait en fait 2 notes; après « ここをタップ » le compteur repasse à « あと 2 個 » (écran 49)
promesse: id=- | texte: よくできました、章クリア！ | ecran: 10, 52 | tenue: oui | pourquoi: chapitres 3, 4 et 5 finis, « 次の章 » mène bien au suivant
hesitations:
  - ecran: 7 | cherchais: comment rendre la frappe « ソフト » | regarde: la consigne ne dit pas « 長押し »; seule l'aide de « 自分でやる » le dit
  - ecran: 42 | cherchais: le « ここをタップ » sur la note | regarde: c'est un bouton dans le bandeau au-dessus de la grille, avec une flèche ↑, alors que l'anneau est sur une note en bas
  - ecran: 54 | cherchais: la note « du dessous » de chaque accord | regarde: l'anneau est sur la note du haut (C4); j'ai suivi l'anneau, ça a marché
abandon: jamais
incidents-banc:
defauts:
  - geste: 2 | ecran.vu: 2 | cible: icône « 続きを読む » ⌄ @ 294,741
    attendu: en lisant toute la consigne, les cases à toucher restent visibles | obtenu: le panneau grandit et coupe la 2e rangée de la grille: les cases « 4 » et la dernière case pointillée sont cachées
    symptome: cache
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 41 | cible: bouton « 次のステップ  » → @ 472,978
    attendu: à l'étape 4/4 de la basse, les notes à toucher sont visibles au-dessus du panneau | obtenu: le panneau ouvert couvre tout le piano roll, seul « ベース · 小節 4/8 » dépasse; il faut toucher « 自分でやる » pour voir les notes
    symptome: cache
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 49 | ecran.vu: 49 | cible: bouton « ベース · 小節 4/8 ↑ タップする点線のノートがあと 1 個 ここをタップ » @ 294,336
    attendu: le compteur dit le vrai nombre de notes restantes et toutes les notes à mettre sont en vue | obtenu: une note (l'octave du haut) était hors de l'écran; après « ここをタップ » la grille défile et le compteur passe de 1 à 2
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 30 | cible: icône « あなたの番：1小節目の点線のノートをタップ：キックのあとに小さな2音ずつ。 読む » ⌃ @ 294,977
    attendu: une consigne = deux gestes au plus | obtenu: une seule consigne demande 8 notes (aussi 3 cases écran 1, 4 cases écran 11 et 16, 7 notes + « コピー » écran 42)
    symptome: incompris
    gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: -
  - geste: 54 | ecran.vu: 54 | cible: icône « あなたの番：1小節目の2つのコードそれぞれの下の点線のノートをタップ：コード全体が置ける。2つのスタブ（短く乾いたコード）。 読む » ⌃ @ 294,977
    attendu: la consigne et l'anneau montrent la même note | obtenu: la consigne dit la note « du dessous » (下), l'anneau est sur la note du haut (C4)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 10 | ecran.vu: 11 | cible: texte « ハイハット » @ 207,470
    attendu: le haut du panneau dit chapitre et étape comme au chapitre 3 | obtenu: au chapitre 4 la ligne « チャプター 4/15 · ステップ… » et la barre de progression manquent; le bandeau au-dessus de la grille montre seulement « 個 »
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 16 | banc?: non | promesse: -
  - geste: 6 | ecran.vu: 7 | cible: texte « あなたの番：「手拍子」パッドのこの最後のヒットを「ソフト」にしよう。 » @ 294,569
    attendu: la consigne dit comment faire (appui long sur la frappe) | obtenu: rien n'indique l'appui long; il faut ouvrir « 自分でやる » pour lire « ヒットを長押し »
    symptome: sans-aide
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 0 | ecran.vu: 1 | cible: texte « ▸ なぜ？ » @ 294,692
    attendu: le lien « なぜ？ » entier, aligné avec le texte | obtenu: le ▸ est coupé par le bord gauche du panneau
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 2 | banc?: non | promesse: -
mots-faux:
  - ecran: 30 | vu: ベースの影 | attendu: ベースを表示 (« ombre de la basse » ne veut rien dire pour moi)
  - ecran: 41 | vu: 最後は1段上がって、上に跳ぶ | attendu: 最後の音は1つ上、そのあと1オクターブ上へ (phrase confuse)
  - ecran: 16 | vu: 拍からずれた位置 | attendu: 裏拍 (comme au chapitre précédent)
defauts-avant:
  - a-V1 | autre | ecran: 42 | la note à mettre est bien au-dessus du panneau avec un anneau et une seule touche suffit (écrans 30, 42, 54), mais « ここをタップ » n'est pas sur l'anneau: c'est un bouton dans le bandeau du haut; et à l'écran 48 la dernière note était hors de vue (il a fallu toucher ce bouton, écran 49); panneau ouvert, écran 41, aucune note visible
  - D3 | corrigé | ecran: 1 | la première ligne est entière et finit par « … », « 続きを読む » montre le reste (écran 2); repliée, « 読む » avec « … » (écran 42); la carte du kit est sous la consigne, elle ne la cache pas
  - D4 | encore là | ecran: 30 | une seule consigne demande 8 notes; aussi 3 cases (écran 1), 4 cases (écran 11), 7 notes puis « コピー » (écran 42)
  - b-grille | encore là | ecran: 41 | panneau ouvert à l'étape 4/4 de la basse: toute la grille est cachée; aussi écran 2 (après « 続きを読む ») les cases 4 et la dernière sont coupées; à l'écran 1 et 11 c'était bon
