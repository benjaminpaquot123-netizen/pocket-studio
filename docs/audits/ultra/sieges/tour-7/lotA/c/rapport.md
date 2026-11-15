code testé : 8a8542fe1bffe24594417d7416cac7b13662451b
joué par : sous-agent neuf

siege: c | profil: t7-1c | lang: ja | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t7-1c-1 | texte: 例を聴く | ecran: 25 | tenue: oui | pourquoi: chap. 5, après écoute l'étape se coche seule et 次のステップ apparaît sans toucher 次へ
promesse: id=t7-1c-2 | texte: この先に → ここをタップ | ecran: 67 | tenue: non | pourquoi: chap. 7, il manquait 1 note au 小節 1 et le bouton m'a mis au 小節 2 (au chap. 5, écran 47, c'était juste)
promesse: id=t7-1c-3 | texte: 3つのマスをタップ…長押し…ソフト | ecran: 1 | tenue: non | pourquoi: chap. 3 demande 5 gestes d'un coup ; chap. 6 (écran 51) en demande 4
promesse: id=t7-1c-4 | texte: Acid 303 / Plaits / フィルターのか… | ecran: 27 | tenue: non | pourquoi: noms de pistes en anglais et noms coupés (écrans 27, 64)
oeil-neuf: q1 | ecran: 75 | image: 75 | boutons pleins/contour clairs ; la bannière verte « 点線のノートはすべて置けたよ » est un bouton mais ressemble à un simple message ; les flèches ←/→ des pistes sont des images
oeil-neuf: q6 | ecran: 75 | image: 75 | preuve claire (« よくできました、章クリア！ », barre verte, 完了) mais aucun bouton « 次の章 » visible : je ne sais pas où aller ensuite sans rouvrir ⌃
oeil-neuf: q9 | ecran: 75 | image: 75 | « フィルターのか… » et « マイプロジェ… » coupés, onglet « 303 …イン » coupé sous la flèche ←, « C4 » coupé en bas de la grille
tuto:
  - chap 3 クラップ (écrans 1-8) : hésitation (consigne de 5 gestes, texte replié qui chevauche « ▸ なぜ？ ») ; Bravo écran 8
  - chap 4 ハイハット (écrans 9-20) : compris du premier coup ; mais 次へ sur l'écoute a sauté à 章クリア sans écouter
  - chap 5 ベース (écrans 23-49) : hésitation (panneau ouvert, grille cachée) ; fini écran 49
  - chap 6 スタブ (écrans 51-60) : hésitation (« スタブ » / « コード 短い音 » / bouton « コード » : trois noms) ; fini écran 60
  - chap 7 リフ (écrans 62-75) : hésitation (ここをタップ m'envoie au mauvais 小節) ; fini écran 75
hesitations:
  - ecran: 35 | cherchais: les notes en pointillé de la basse | regarde: le panneau ouvert couvre toute la grille, sans flèche ↑/↓
  - ecran: 67 | cherchais: la note qui manque au 小節 1 | regarde: on m'a mis au 小節 2, j'ai dû trouver « ← この前に » plus tard
  - ecran: 52 | cherchais: la piste « スタブ » | regarde: elle s'appelle « コード 短い音 »
abandon: jamais
incidents-banc:
  - geste: 3 | attente: 0 | vu: écran en texte réduit à la barre « フィードバックモード » (l'image, elle, était normale)
  - geste: 4 | attente: 0 | vu: idem
  - geste: 75 | attente: 0 | vu: le banc a écrit « code testé : 8a8542fe » en tête alors que la fiche dit 98e83c21 ; lignes du banc gardées
  - geste: - | attente: 0 | vu: outils hors human-act : Read des images et de last.txt dans mon dossier de siège, grep sur last.txt (sortie redirigée après un BrokenPipe au geste 10)
defauts-avant:
  - T4 / N4 | pas corrigé (écran 35) : panneau ouvert, grille entièrement cachée, aucune note pointillée ni flèche ↑/↓
  - N19 | corrigé (écran 63) : 次へ sur l'écoute non faite → étape 2 qui dit quoi faire, pas de もうすぐ完成
  - T11 | pas corrigé (écran 67) : « この先に → ここをタップ » m'amène au 小節 2 alors qu'il manque une note au 小節 1
  - T3 | pas corrigé (écran 1) : puces « 1小節 / 2 / 3 / 4 » visibles mais la consigne ne parle pas de 2 小節 et aucun bouton « 2 小節にする »
  - T8 | corrigé (écran 38) : chap. 5, étape 2 = 2 actions, étape 3 = 3 actions
  - N21 | pas corrigé (écran 52) : « スタブ » (chapitre), « コード 短い音 » (piste), « コード » (interrupteur) pour la même chose
  - T16 | pas corrigé (écran 27) : « Acid 303 », « Plaits » en anglais, « フィルターのか… » coupé (écran 64)
defauts:
  - geste: 67 | ecran.vu: 67 | cible: この先に → ここをタップ | geste.type: touche
    attendu: grille sur le 小節 1 où manque la note | obtenu: grille sur le 小節 2
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: oui: ecran 73 | banc?: non | promesse: t7-1c-2
  - geste: 35 | ecran.vu: 35 | cible: ベース · 小節 1/4 | geste.type: touche
    attendu: notes pointillées visibles au-dessus du panneau ou flèche ↑/↓ | obtenu: grille entièrement cachée par le panneau
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: 続きを読む | geste.type: touche
    attendu: 3 actions au plus | obtenu: 3 cases + appui long + ソフト (5 actions) ; chap. 6 écran 51 : 4 actions
    symptome: incompris | gravite: finition | revu: oui: ecran 51 | banc?: non | promesse: t7-1c-3
  - geste: 27 | ecran.vu: 27 | cible: ベース Plaits | geste.type: touche
    attendu: noms en japonais | obtenu: « Plaits », « Acid 303 » en anglais
    symptome: texte-faux | gravite: finition | revu: oui: ecran 75 | banc?: non | promesse: t7-1c-4
  - geste: 64 | ecran.vu: 64 | cible: メロディ フィルターのかかったメロディ | geste.type: touche
    attendu: nom lisible en entier | obtenu: « フィルターのか… » coupé ; « + トラ→ » coupé par la flèche (écran 27)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 75 | banc?: non | promesse: t7-1c-4
  - geste: 52 | ecran.vu: 52 | cible: コード 短い音 | geste.type: touche
    attendu: un seul mot pour la stab | obtenu: スタブ / コード 短い音 / コード (interrupteur)
    symptome: incompris | gravite: finition | revu: oui: ecran 60 | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 20 | cible: 次へ | geste.type: touche
    attendu: l'étape d'écoute me dit d'écouter d'abord | obtenu: saut direct à « 章クリア » sans avoir écouté ; puis « 次の章 » et « 次へ » côte à côte
    symptome: incompris | gravite: finition | revu: oui: ecran 63 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: ▸ なぜ？ | geste.type: touche
    attendu: consigne repliée lisible | obtenu: le texte replié chevauche « ▸ なぜ？ » ; déplié, le bouton reste « 続きを読む »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 35 | banc?: non | promesse: t7-1c-4
