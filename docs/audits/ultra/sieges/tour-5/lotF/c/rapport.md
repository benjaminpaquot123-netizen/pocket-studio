siege: c | profil: t5-Fc-debutante-japonaise-tablette | lang: ja | device: tablet8 | font: 100 | mode: simple
mission: simple | buts: son, partie, jamais-pro
promesse: id=t5-Fc-1 | texte: (aucun message « デモの準備ができました » ou équivalent vu ; seul « デモを聴く 完成済みの曲。何ができるか体験しよう。 ») | ecran: 6 | tenue: pas vue | pourquoi: après « 作って聴く » le morceau joue mais la guide affiche seulement « ガイドが曲を読んでいます… », aucun « ta démo est prête » ni écran 6, 7, 22, 33
promesse: id=t5-Fc-2 | texte: このプロジェクトをどう表示する？上のバッジでいつでも変えられます。 | ecran: 14 | tenue: oui | pourquoi: la pastille « シンプル » en haut ouvre « このプロジェクトのモード » avec Simple/Pro, fermée sans rien changer
promesse: id=t5-Fc-3 | texte: シンプル はじめての人に：5つのタブ、大きなボタン、ふだんの言葉。 | ecran: 2 | tenue: oui | pourquoi: son qui joue et 2e partie ajoutée deux fois (Lo-fi et projet vide) sans jamais passer en Pro
promesse: id=t5-Fc-ecran-1 | texte: ふだんの言葉 | ecran: 11 | tenue: non | pourquoi: en Simple, « ミックス » sur les parties, « 後にブレイク »/« 前にビルドアップ »/« フィル », « 未決定 18 件 » : pas du japonais de tous les jours pour une débutante
hesitations:
  - ecran: 6 | cherchais: un message « ta démo est prête » ou que faire maintenant | regarde: une guide « ガイドが曲を読んでいます… » puis un gros panneau qui couvre la moitié de l'écran
  - ecran: 10 | cherchais: comment ajouter une 2e partie | regarde: aucun bouton « ajouter » visible dans la liste, trouvé via « その他の操作 » puis « 後ろに挿入 »
  - ecran: 16 | cherchais: « デモを聴く » de l'accueil | regarde: disparu de l'accueil, retrouvé seulement dans 新しい曲 › ほかの始め方 en bas
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 300 | vu: le premier « regarde » a mis plusieurs minutes avant le premier écran (démarrage du banc)
defauts:
  - geste: 5 | ecran.vu: 6 | cible: ガイドが曲を読んでいます… | geste.type: touche
    attendu: phrase entière lisible dans le panneau Guide | obtenu: le premier caractère « ガ » est rogné au bord gauche de l'écran (revu sur le projet Trap)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 7 | cible: 現在の状況 未決定 18 件 | geste.type: touche
    attendu: la ligne « 現在の状況 » lisible | obtenu: la ligne est coupée à moitié derrière la barre du bas (元に戻す / 停止), seul le haut des lettres se voit
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: 言葉：スケール 曲の中で相性のいい音を、低い順に並べたもの。ここでは A マイナー。 | geste.type: touche
    attendu: la gamme qui va avec les accords affichés (B♭ F Dm Gm) | obtenu: la guide dit « A マイナー » alors que les accords du morceau contiennent B♭ : contradiction à l'écran
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 10 | ecran.vu: 11 | cible: 曲中で 2 回再生。編集する範囲： | geste.type: touche
    attendu: même nombre de lectures que dans la liste | obtenu: la liste dit « A · イントロ × 1 » mais la fiche dit « 4 小節 · 2 回再生 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: C · サビ 1小節  ·  ミックス | geste.type: glisse
    attendu: des parties claires (une サビ) | obtenu: trois lignes « C · サビ » (1, 6, 1 小節) marquées « ミックス » sans + ni − actifs, incompréhensible pour une débutante
    symptome: incompris | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: t5-Fc-ecran-1
  - geste: 12 | ecran.vu: 13 | cible: M × 1  ·  4 小節 | geste.type: touche
    attendu: « 新しい空のパート » = une partie vide avec un nom lisible | obtenu: partie nommée seulement « M », accords « F F F F » donc pas vide, et la boucle « ループ » s'est allumée toute seule
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: イントロ 小節 1/64 | geste.type: touche
    attendu: le compteur du haut avance comme celui du bloc 曲 | obtenu: en haut « 小節 1/64 » figé pendant que le bloc 曲 affiche « 小節 4.3 / 64 »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 24 | banc?: son | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: 新しい曲 ワンタップでスタイルを、またはアシスタントとステップごとに。 | geste.type: retour
    attendu: « デモを聴く » toujours sur l'accueil | obtenu: disparu après le 1er projet ; il faut 新しい曲 › ほかの始め方 › glisser › 試聴
    symptome: perdu | gravite: contourne-en-3-gestes | revu: oui: ecran 29 | banc?: non | promesse: t5-Fc-1
  - geste: 30 | ecran.vu: 31 | cible: チル | geste.type: touche
    attendu: ouvrir la démo pour « 何ができるか体験しよう » (accueil) | obtenu: la ligne démo ne s'ouvre pas, seul « 試聴 » existe
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: t5-Fc-1
  - geste: 31 | ecran.vu: 32 | cible: シンプル 前回の選択 はじめての人に：5つのタブ、大きなボタン、ふだんの言葉。 | geste.type: touche
    attendu: l'écoute de la démo s'arrête quand on quitte la liste | obtenu: le son de démo continue sur l'écran « シンプル？プロ？ » (s'arrête à l'ouverture du projet)
    symptome: rien | gravite: finition | revu: non | banc?: son | promesse: -
mots-faux:
  - ecran: 33 | vu: 太鼓のふち | attendu: リムショット
  - ecran: 16 | vu: Lo-fi ヒップホップ Lo-fi ヒップホップ | attendu: Lo-fi ヒップホップ (titre répété deux fois sur la carte 続ける)
  - ecran: 5 | vu: 参考： Nekfeu – On verra (2015) | attendu: une référence new-yorkaise (la carte dit « ニューヨークのオールドスクール »)
