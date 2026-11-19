code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: confirmé | lang: ja | device: phone | font: 130 | mode: pro
mission: tuto-pro | buts: -
promesse: id=- | texte: 「メロディトラックを作り、フックを書こう」 | ecran: 117 | tenue: oui | pourquoi: la piste mélodie a été créée et les 2 mesures du hook validées (écran 129)
hesitations:
  - ecran: 67 | cherchais: pourquoi l'étape ne valide pas après les 4 cases annoncées | regarde: petit indice « 2 小節目 · 4 マス → » en bas
  - ecran: 104 | cherchais: l'accord de la mineur annoncé | regarde: la case pointillée sur la ligne « C3 »
  - ecran: 141 | cherchais: « ノート間のグライド » dans les réglages avancés | regarde: la liste s'arrête à « 音量：サステイン » sous le panneau du tuto
abandon: jamais
incidents-banc:
defauts:
  - geste: 56 | ecran.vu: 57 | cible: 小節 2/2 · リム | geste.type: touche
    attendu: travailler sur le pad tom comme dit 「タムのパッドで」 | obtenu: la grille est sur « リム » et l'étape valide quand même
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: ここに点線のマスがあと 4 個 | geste.type: touche
    attendu: étape validée après les 4 cases entre les temps | obtenu: rien ne valide, il faut aussi remplir la mesure 2 (indice minuscule)
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 106 | ecran.vu: 107 | cible: 次の小節にコピー | geste.type: touche
    attendu: la mesure 1 copiée sur la mesure 2 | obtenu: écran identique (basse #88 et accords #106) ; au ch.7 (#128) le même bouton marche
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 107 | banc?: non | promesse: -
  - geste: 96 | ecran.vu: 97 | cible: ポンプ | geste.type: touche
    attendu: voir l'effet ajouté dans la liste | obtenu: la liste remonte en haut, l'effet ajouté reste hors écran, aucune confirmation
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 140 | ecran.vu: 141 | cible: 音量：サステイン | geste.type: glisse
    attendu: descendre jusqu'à « ノート間のグライド » | obtenu: le défilement s'arrête, la suite reste sous le panneau du tuto ; atteinte seulement via « 自分でやる »
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 141 | banc?: non | promesse: -
  - geste: 116 | ecran.vu: 117 | cible: お手本を聴かずにスキップしました。 | geste.type: touche
    attendu: rien, j'avais écouté l'exemple (il jouait encore) | obtenu: le tuto dit que j'ai sauté sans écouter
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 29 | cible: 少し小さめに | geste.type: touche
    attendu: devoir baisser la force du clap | obtenu: clap à 100 % validé sans rien baisser
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: 120 | geste.type: ecris
    attendu: le tempo tapé (105) seul en évidence | obtenu: la pastille « 120 » reste [sélectionné]
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 57 | vu: リム | attendu: タム
defauts-avant:
chapitres:
  - ch1 テンポとキー : compris du premier coup
  - ch2 キック : compris du premier coup
  - ch3 スネアとクラップ : compris du premier coup (doute sur « 少し小さめに »)
  - ch4 ハイハットとタム : hésitation (rim au lieu de tom, 4 cases puis 4 de plus)
  - ch5 ベース : hésitation (« 次の小節にコピー » sans effet)
  - ch6 コード : hésitation (même bouton de copie, accord sur « C3 »)
  - ch7 フック : hésitation (glide caché sous le panneau)
  - ch8 最初のパート：サビ : compris du premier coup (le bouton nomme la partie à ma place)
8 défauts (0 bloquant / 0 grave) ; 155 gestes utilisés sur 188 ; chapitres hésités : 4, 5, 6, 7 ; bloqués : aucun.

fin-tranche: oui (ROUTE ligne 841)
