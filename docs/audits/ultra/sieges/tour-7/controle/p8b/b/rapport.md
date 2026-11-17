code testé : 11ca024b94945ffae964263f27e8342aa3770316
joué par : sous-agent neuf

siege: b | profil: adulte-japonais-premiere-fois | lang: ja | device: telephone | font: 100 | mode: simple
mission: tuto-facile | buts: chapitre-3, chapitre-4, chapitre-5
promesse: id=repli | texte: icône ⌄ | ecran: 2 | tenue: oui | pourquoi: ⌄ replie le panneau, la grille (埋める, 消去, 長さ) revient en entier, consigne réduite en bas
promesse: id=consigne-coupee | texte: あなたの番：4小節目のノート（最後は1段上がって、上に跳ぶ）をタップして、もう一度「コピ… 読む ⌃ | ecran: 46 | tenue: oui | pourquoi: repliée, la consigne finit par « … » (vu aussi écran 8 « 開いたメニ… »)
promesse: id=chaque-etape | texte: 見せて / 自分でやる / 次の小節にコピー / 結果を聴く → 成功 次のステップ | ecran: 43 | tenue: oui | pourquoi: les 9 étapes des chapitres 3-5 ont 見せて ou un bouton d'action, puis une coche (成功 ou 章クリア)
promesse: id=touche-ici | texte: ベース · 小節 4/8 ↑ タップする点線のノートがあと 8 個 ここをタップ | ecran: 47 | tenue: non | pourquoi: le 1er toucher a fait défiler vers C3 : les 7 notes visées sont sorties de l'écran, flèche passée à ↓ ; il a fallu 2 touchers de plus
oeil-neuf: q1 | ecran: 59 | image: 59 | tout ce qui a l'air touchable l'est ; la bande « よくできました、章クリア！ » avec ⌃ ne dit pas qu'elle s'ouvre
oeil-neuf: q6 | ecran: 59 | image: 59 | preuve claire (章クリア, 完了, barre pleine, « すべて置けたよ ») mais pas de « 次の章 » visible tant qu'on n'ouvre pas le panneau
oeil-neuf: q9 | ecran: 59 | image: 59 | puce « + トラ→ » coupée, la flèche → mord le mot トラック
hesitations:
  - ecran: 19 | cherchais: la consigne de l'étape après avoir ouvert « なぜ？ » | regarde: l'explication a pris sa place, la consigne a disparu
  - ecran: 47 | cherchais: les notes pointillées après « ここをタップ » | regarde: grille vide autour de C3, une seule note coupée en haut à droite
abandon: jamais
incidents-banc:
  - geste: 4 | attente: 0 | vu: aucun incident du banc ; outils hors human-act : Read des images de mon siège, et head/sed/grep pour raccourcir la sortie (un BrokenPipe local dû à head, sans effet sur le banc)
defauts:
  - geste: 47 | ecran.vu: 47 | cible: ベース · 小節 4/8 ↑ タップする点線のノートがあと 8 個 ここをタップ | geste.type: touche
    attendu: les notes pointillées visées viennent à l'écran | obtenu: la grille défile vers C3, les 7 notes visibles avant disparaissent, flèche ↓ ; revenues au 3e toucher (écran 49)
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 57 | banc?: non | promesse: touche-ici
  - geste: 19 | ecran.vu: 19 | cible: 毎小節同じように戻ってくるずれた音：これが催眠効果を生む。 | geste.type: touche
    attendu: l'explication « なぜ？ » s'ajoute sous la consigne | obtenu: elle remplace la consigne, 2e ligne « 生む » coupée à mi-hauteur, « … » posé de travers
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: consigne-coupee
  - geste: 19 | ecran.vu: 1 | cible: ▸ なぜ？ | geste.type: touche
    attendu: « ▸ なぜ？ » aligné avec la marge du panneau | obtenu: le triangle colle au bord gauche de l'écran, à moitié coupé
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 19 | banc?: non | promesse: -
  - geste: 34 | ecran.vu: 34 | cible: bouton + «  トラック » [coupé] | geste.type: touche
    attendu: puce « + トラック » lisible | obtenu: « + トラ » puis la flèche → par-dessus le mot
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 59 | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 46 | cible: ベース · 小節 4/8 ↑ タップする点線のノートがあと 8 個 ここをタップ | geste.type: touche
    attendu: le compteur « あと 8 個 » correspond aux notes visibles | obtenu: 7 notes visibles, la 8e est très haut hors grille, la flèche ↑ ne dit pas qu'il en manque une seule
    symptome: incompris | gravite: finition | revu: oui: ecran 56 | banc?: non | promesse: touche-ici
  - geste: 29 | ecran.vu: 32 | cible: image (rond coché) | geste.type: attends
    attendu: coche avec le mot « 成功 » comme aux autres étapes | obtenu: étapes d'écoute (ch.4 étape 3, ch.5 étape 1) : rond coché seul, sans mot
    symptome: sans-nom | gravite: finition | revu: oui: ecran 32 | banc?: non | promesse: -
mots-faux:
  - ecran: 1 | vu: キット テクノ をダウンロード | attendu: テクノキットをダウンロード
  - ecran: 18 | vu: 太鼓のふち | attendu: リムショット（太鼓のふち）
  - ecran: 34 | vu: ゴーストベース | attendu: 薄く表示されたお手本（未翻訳の印象）
defauts-avant:
  - b-V1 (panneau repli) | corrigé | ecran: 2 | ⌄ replie le panneau et rend la grille entière
  - b-V2 (consigne coupée sans « … ») | corrigé | ecran: 46 | repliée, la consigne finit par « … » ; seul le texte « なぜ？ » est mal coupé (écran 19)
  - b-V3 (étape sans bouton ni 見せて) | corrigé | ecran: 43 | toutes les étapes vues ont 見せて ou un bouton d'action
fin
