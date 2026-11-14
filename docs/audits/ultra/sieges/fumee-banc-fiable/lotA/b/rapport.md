siege: b | profil: t5-Ad | lang: ja | device: tablet11Land | font: 100 | mode: pro
mission: padlive | buts: son, stop, prise
promesse: id=t5-Ad-1 | texte: 全部止める / 1 行目を停止 | ecran: 7 | tenue: non | pourquoi: « 全部止める » (et « 1 行目を停止 », écran 17) coupent le son mais la prise continue de compter (5 → 8 小節 en silence), revu écran 27
promesse: id=t5-Ad-2 | texte: 1 行目を小節頭でスタート | ecran: 14 | tenue: oui | pourquoi: rien ne jouait, la ligne 1 (B1-B3) est partie seule en boucle
promesse: id=t5-Ad-3 | texte: Pad Live | ecran: 1 | tenue: oui | pourquoi: à l'ouverture « rien ne joue » jusqu'à mon premier toucher
promesse: id=t5-Ad-4 | texte: 元に戻す | ecran: 11 | tenue: non | pourquoi: un toucher sur « 元に戻す » efface la prise posée sur le pad sans aucun avertissement, revu écran 31
hesitations:
  - ecran: 4 | cherchais: un bouton ■ « tout arrêter » visible pendant la performance | regarde: seulement ▶ « 再生 » en haut ; « 全部止める » est caché dans « その他 »
  - ecran: 13 | cherchais: ma prise sur B1 comme l'annonce le bandeau | regarde: B1 = « ドラム » du pack House, la prise a disparu
abandon: jamais
incidents-banc:
defauts:
  - geste: 5 | ecran.vu: 7 | cible: bouton « 全部止める » | geste.type: touche
    attendu: tout s'arrête, prise comprise (la prise se termine) | obtenu: le son s'arrête mais « 8 小節 · タップで終了 » continue, prise de 8 mesures avec la fin vide
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 27 | banc?: non | promesse: t5-Ad-1
  - geste: 10 | ecran.vu: 11 | cible: bouton « 元に戻す » | geste.type: touche
    attendu: un avertissement avant d'effacer la prise | obtenu: la prise sur B1 est effacée d'un coup (« ページ B · 空 »), aucun message, pas de « refaire »
    symptome: perdu | gravite: contourne-en-6-gestes | revu: oui: ecran 31 | banc?: non | promesse: t5-Ad-4
  - geste: 12 | ecran.vu: 13 | cible: texte « テイクはパッド B1 にあります。 » | geste.type: touche
    attendu: le bandeau disparaît ou se corrige quand la prise est annulée | obtenu: il affiche toujours « B1 » alors que B1 joue « ドラム » (même chose pour B8, écran 31, « 空, パッド B8 »)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 31 | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: bouton « 再生 » | geste.type: touche
    attendu: pendant que des pads tournent, le bouton du haut sert à arrêter (ou montre que ça joue) | obtenu: il affichait « 再生 » et a lancé en plus la musique du projet par-dessus mon pad
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 3 | ecran.vu: 4 | cible: bouton « パック: パーティー » | geste.type: touche
    attendu: le nom du pack lisible sur le bouton | obtenu: le bouton affiche seulement « … ▾ » (capture écran 4)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 8 | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: bouton « 小節頭で終わります… » (absent de la lecture d'écran) | geste.type: touche
    attendu: la prise se termine tout de suite puisque plus rien ne joue | obtenu: attente « 小節頭で終わります… » plusieurs secondes, bouton sans nom pour le lecteur d'écran
    symptome: sans-nom | gravite: finition | revu: oui: ecran 28 | banc?: temps | promesse: -
mots-faux:
  - ecran: 3 | vu: 1小節 · タップで終了 | attendu: 1 小節 · タップで終了 (même espacement que « 3 小節 »)
