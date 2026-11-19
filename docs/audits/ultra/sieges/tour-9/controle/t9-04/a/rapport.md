code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: confirme | lang: ja | device: phone | font: 130 | mode: pro
mission: tuto acid_techno, chapitres 6 a 12 | buts: - (aucun id donne par le brief)
promesse: id=- | texte: （「おすすめの長さにする」で一気にできる） | ecran: 34 | tenue: oui | pourquoi: un toucher a donne 16/16/32/12/4/32/16 小節, total 128
hesitations:
  - ecran: 25 | cherchais: « + F / G / H » pour ajouter la suite | regarde: la liste repart en haut apres chaque ajout, il faut refaire glisser
  - ecran: 42 | cherchais: le nom de la pad シンバル | regarde: la grille de la ドロップ est a moitie sous le haut d'ecran, sans nom de pad
  - ecran: 52 | cherchais: la zone de dessin de la カーブ | regarde: elle est sous le panneau du tuto, il faut faire defiler deux fois
abandon: ecran 62 | parce que les curseurs de la ミックス ne prennent pas le doigt, puis le banc a arrete le siege (5 ecrans identiques)
incidents-banc:
  - geste: 62 | attente: 0 | vu: « Fin du siège : rien ne change (5 écrans identiques) » ; le changement d'onglet n'a pas ete joue
  - geste: 59 | attente: 0 | vu: « doigt couvert : ドラム couvre Acid 303 の音量 » alors que l'image ne montre rien par-dessus (gestes 58 a 61)
  - geste: - | attente: 0 | vu: outils hors human-act : cat/ls/grep/tail dans mon dossier de siege seulement, plus lecture des deux gabarits donnes par le chef
defauts:
  - geste: 58 | ecran.vu: 59 | cible: curseur [état « +0.0 dB », à 80 %] @ 204,516 | geste.type: glisse
    attendu: le curseur Acid 303 descend un peu (etape 10/1) | obtenu: rien ne bouge ; le doigt est pris par « ドラム », de meme sur le curseur ドラム et sur le nom « Acid 303 »
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 61 | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 45 | cible: bouton (doigt qui touche) «   自分でやる » | geste.type: touche
    attendu: « je le fais moi-meme » : le panneau s'ecarte et je dessine la カーブ du 303 | obtenu: 成功 tout de suite, カットオフ deja en パートごと, sans que j'aie dessine (au ch. 10 le meme bouton ne fait rien a ma place)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 25 | cible: bouton « + E · ブレイク » | geste.type: touche
    attendu: apres un ajout, la liste « 最後に追加 » reste ou j'etais | obtenu: elle repart en haut, E/F/G/H sont coupes ou caches, un glisse a chaque ajout (5 en tout)
    symptome: cache | gravite: contourne-en-5-gestes | revu: oui: ecran 30 | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: icône « 続きを読む » ⌄ | geste.type: touche
    attendu: le message de la ビルドアップ s'ouvre en entier (je cherchais le « 1拍の無音 ») | obtenu: la boite defile, « よくできました » disparait, le texte reste coupe avec « … » par-dessus
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 48 | ecran.vu: 49 | cible: bouton ⊕ «   パート「イントロ 2」へ移動 » | geste.type: touche
    attendu: le haut d'ecran dit イントロ 2 | obtenu: il dit toujours « ドロップ 小節 33/128 », seule une ligne plus bas dit « パート C »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 54 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: titre « 曲 » [coupé, état « 開いた状態 »] | geste.type: touche
    attendu: voir la partie que je viens de creer | obtenu: le panneau du tuto couvre tout l'ecran de travail et se rouvre a chaque etape (ecrans 6, 9, 11)
    symptome: cache | gravite: finition | revu: oui: ecran 6 | banc?: non | promesse: -
mots-faux:
  - ecran: 15 | vu: ライド | attendu: シンバル (le resultat ecran 16 et la pad disent シンバル)
  - ecran: 36 | vu: 赤い× | attendu: 取り消し線 (l'ecran parle de 取り消し線, pas de croix rouge)
defauts-avant:
  - (aucun defaut vise donne par le brief)
chapitres:
  - 6 パートを作ろう : compris du premier coup
  - 7 パートを並べよう : hesitation (liste qui remonte apres chaque ajout)
  - 8 トランジション : compris du premier coup
  - 9 フィルターの動き : hesitation (自分でやる fait l'etape 1 a ma place ; zone de dessin cachee)
  - 10 ミックス : bloque (curseurs qui ne prennent pas le doigt, siege arrete par le banc au geste 62)
  - 11, 12 : pas atteints (budget restant 91 gestes)
fin-tranche: non (dernier step=levels, done=22)
