siege: d | profil: t5-Ad | lang: ja | device: tablet11Land | font: 100 | mode: pro
mission: padlive | buts: son, stop, prise
promesse: id=t5-Ad-1 | texte: ■ 全部 | ecran: 28 | tenue: oui | pourquoi: en pleine prise, ■ 全部 arrête tous les blocs (prise continue, voulu : « ブレイクには ■を使ってね ») ; ■ ドラム n'arrête que la piste batterie (écran 27)
promesse: id=t5-Ad-2 | texte: 1 行目を小節頭でスタート | ecran: 3 | tenue: oui | pourquoi: rien ne tournait, la ligne 1 est partie tout de suite (pads 1, 3, 4 en boucle)
promesse: id=t5-Ad-3 | texte: Pad Live | ecran: 44 | tenue: non | pourquoi: en rouvrant Pad Live depuis l'accueil, « Le pad 2 (Clap) joue une fois » sans que je touche rien (revu écran 47) ; à la toute 1re ouverture, rien
promesse: id=t5-Ad-4 | texte: キャンセル | ecran: 32 | tenue: non | pourquoi: « キャンセル » du bandeau « 曲を書き込みました » efface la prise (piste Pads live disparue) sans aucune question
hesitations:
  - ecran: 6 | cherchais: un bouton pour tout arrêter dans Pad Live | regarde: il n'y a que ▶ en haut ; « 全部止める » est caché dans « その他 »
  - ecran: 10 | cherchais: la différence entre les deux REC | regarde: REC du haut (曲) et « 演奏を録音 » (パッド) tournent en même temps, deux compteurs différents
  - ecran: 27 | cherchais: arrêter la boucle de kick avec ■ ドラム | regarde: la colonne ドラム passe à 空 mais « le pad live 1 joue en boucle » continue
abandon: jamais
incidents-banc:
defauts:
  - geste: 10 | ecran.vu: 11 | cible: bouton « 全部止める » @ 1334,484 | geste.type: touche
    attendu: « tout arrêter » coupe la musique qui joue | obtenu: « la musique du projet joue en boucle » continue, les deux prises continuent (10 小節)
    symptome: incompris | gravite: contourne-en-1-gestes | revu: oui: ecran 16 | banc?: non | promesse: -
  - geste: 16 | ecran.vu: 17 | cible: bouton « 元に戻す » @ 1563,484 | geste.type: touche
    attendu: un avertissement avant d'effacer la prise | obtenu: le bandeau « パフォーマンス保存 · 0:03 » disparaît, prise effacée sans question
    symptome: perdu | gravite: contourne-en-2-gestes | revu: oui: ecran 59 | banc?: non | promesse: t5-Ad-4
  - geste: 31 | ecran.vu: 32 | cible: bouton « キャンセル » @ 1650,174 | geste.type: touche
    attendu: « キャンセル » demande confirmation avant d'effacer la prise | obtenu: piste « Pads live » effacée d'un coup ; récupérée seulement via 構成 → « やり直す »
    symptome: perdu | gravite: contourne-en-2-gestes | revu: oui: ecran 39 | banc?: non | promesse: t5-Ad-4
  - geste: 43 | ecran.vu: 44 | cible: bouton « Pad Live NEW すぐ弾けるパッド、自分の音、ステージエフェクト。すぐに鳴ります。 » | geste.type: touche
    attendu: Pad Live s'ouvre en silence | obtenu: « Le pad 2 (« Clap ») joue une fois » à l'ouverture, sans toucher de pad
    symptome: incompris | gravite: finition | revu: oui: ecran 47 | banc?: son | promesse: t5-Ad-3
  - geste: 58 | ecran.vu: 59 | cible: texte « テイクはパッド B1 にあります。 » | geste.type: touche
    attendu: après « 元に戻す », le message ne dit plus que la prise est sur B1 | obtenu: message toujours affiché alors que « ページ B · 空 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t5-Ad-4
  - geste: 13 | ecran.vu: 14 | cible: bouton « 試聴 » @ 983,129 | geste.type: touche
    attendu: écouter la prise | obtenu: le doigt a touché « シートを閉じる » (feuille de rangement de la prise pads, 11 小節, invisible) : rien ne joue, prise pads jamais proposée ensuite
    symptome: perdu | gravite: contourne-en-1-gestes | revu: non | banc?: temps | promesse: -
  - geste: 49 | ecran.vu: 53 | cible: bouton « 準備完了: 小節頭でスタート » @ 1445,385 | geste.type: touche
    attendu: la prise démarre quand je tape un pad | obtenu: kick et caisse jouent mais la prise reste « 準備完了 » ; elle ne part qu'avec une ligne de boucles
    symptome: incompris | gravite: contourne-en-1-gestes | revu: oui: ecran 53 | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: touchable « Acid 303 のループ 2 小節 » @ 1627,706 | geste.type: touche
    attendu: le pad sous mon doigt se lance au début de la prise | obtenu: au REC tout descend de ~84 px (bandeau « REC 0:00 »), le toucher tombe dans le vide (DOIGT rien)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 36 | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 22 | cible: touchable « Live オープン たった今 · 124 BPM · 1小節 のループ » | geste.type: touche
    attendu: « はじめてのビート » attend dans マイプロジェクト (message écran 1) | obtenu: seul le projet « Live » est listé
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 0 | ecran.vu: 1 | cible: bouton « パック: パーティー » @ 1573,132 | geste.type: touche
    attendu: le nom du pack lisible | obtenu: affiché « ... ▾ » seulement (texte coupé)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 31 | vu: 0 個の設定 | attendu: 設定なし (« 0 個の設定 » sonne machine)
  - ecran: 24 | vu: オープンハイハッ/ト | attendu: coupure de ligne au milieu du mot sur le pad « オープンハイハット »
