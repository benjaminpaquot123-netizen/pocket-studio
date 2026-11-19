code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: confirmé | lang: ja | device: phone360 | font: 200 | mode: pro
mission: tuto-pro (tuto synthwave, chapitres 9-16, départ chapitre 9) | buts: -
chapitres:
  - 9 パートを理解する : compris du premier coup
  - 10 パートを作ろう : compris du premier coup (boutons « パートを作る »)
  - 11 パートを並べよう : hésitation (11 ajouts, la liste saute après chaque ajout)
  - 12 トランジション : bloqué à l'étape 1/3 (grille de pas introuvable)
  - 13 à 16 : pas atteints
promesse: id=- | texte: パート「プレサビ」へ移動 | ecran: 70 | tenue: non | pourquoi: on arrive sur la partie, mais la grille où taper la tom n'est pas à l'écran
hesitations:
  - ecran: 67 | cherchais: comment mettre la プレサビ à 4 小節 à la main | regarde: − grisé « 灰色：長さ固定 », j'ai pris « おすすめの長さにする »
  - ecran: 29 | cherchais: comment créer une partie moi-même | regarde: la démo « 見せて » montre un doigt sur une zone vide puis revient
  - ecran: 37 | cherchais: les boutons « + » après chaque ajout | regarde: il faut redescendre deux fois à chaque partie
abandon: ecran 73 | parce que la grille de pas de la プレサビ n'apparaît pas et rien ne défile
incidents-banc:
  - geste: 74 | attente: 0 | vu: le banc a fini le siège tout seul (« rien ne change, 5 écrans identiques ») pendant mes essais de défilement ; je n'ai pas pu tenter ⌃ / 見せて / 次へ
defauts:
  - geste: 69 | ecran.vu: 73 | cible: texte « パート：D · プレサビ » | geste.type: touche
    attendu: voir la grille de la tom pour taper les 3 cases avant le dernier temps | obtenu: en-têtes + panneau du tuto remplissent l'écran, la grille n'est nulle part et les glissés ne font rien (#70-#73)
    symptome: cache | gravite: mission-impossible | revu: non | banc?: non | promesse: -
  - geste: 37 | ecran.vu: 38 | cible: bouton « + B · イントロ » | geste.type: touche
    attendu: ajouter les 11 parties d'affilée sous « 追加 » | obtenu: après chaque ajout la liste remonte sur la ligne ajoutée, les « + » sortent de l'écran : 2 glissés par partie (~20 gestes en plus)
    symptome: perdu | gravite: contourne-en-20-gestes | revu: oui: ecran 66 | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 68 | cible: onglet (ligne brisée et points) « 構成 » [sélectionné] | geste.type: touche
    attendu: lire les onglets du bas (構成, サウンド, ミックス…) | obtenu: la barre « フィードバックモード / REC / メモ » mange le bas des onglets, les noms sont coupés à moitié
    symptome: cache | gravite: finition | revu: oui: ecran 1 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 2 | cible: icône « 続きを読む » ⌄ | geste.type: touche
    attendu: lire la phrase entière de la consigne | obtenu: la phrase reste coupée « … », seul « エネルギー » apparaît dessous ; il faut glisser dans la carte
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 36 | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 29 | cible: bouton (œil) «  見せて » | geste.type: touche
    attendu: voir comment créer la partie à la main | obtenu: « 見てて、やってみせるね… », un doigt sur une zone vide entre les onglets, puis retour au panneau sans rien montrer
    symptome: incompris | gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 22 | ecran.vu: 23 | cible: texte « パート「イントロ」を作りました（「サビ」のコピー）：キック, スネア, クラップ, ハイハット, オープンハイハット, リム, ベースなし。 » | geste.type: touche
    attendu: liste japonaise avec « 、 » | obtenu: virgules latines « , » dans la phrase japonaise (même chose écrans 31 et 35)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: bouton ⊕ «   パート「ブリッジ」を作る » | geste.type: touche
    attendu: le bouton d'action reste à la même place d'une étape à l'autre | obtenu: il saute (y 588, 693, 622, 651…) et le panneau se rouvre seul après chaque étape
    symptome: perdu | gravite: finition | revu: oui: ecran 34 | banc?: non | promesse: -
mots-faux:
  - ecran: 23 | vu: キック, スネア | attendu: キック、スネア
  - ecran: 40 | vu: 最後に追加： (la consigne dit「追加」) | attendu: même mot dans la consigne et à l'écran
  - ecran: 15 | vu: ポストサビ | attendu: 後サビ / アウトロ前 (« ポストサビ » sonne traduit)
budget: 74 gestes sur 181 utilisés ; arrêt par le banc au chapitre 12, pas par épuisement

fin-tranche: non (dernier step=fill, done=37, chapters=1,2,3,4,5,6,7,8,9,10,11)
BENCH-FLAG (journal #74) : where=seat.rien-ne-change want="un écran qui change" saw="5 écrans identiques" waited=196799 screen=tab_Sons
BENCH-VERDICT (journal #74) : fail where=seat.rien-ne-change why="rien ne change (5 écrans identiques)"
