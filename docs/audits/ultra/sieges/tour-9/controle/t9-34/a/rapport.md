code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: confirme | lang: ja | device: pliable | font: 130 | mode: pro
mission: tuto-pro (chapitres 10 à 17) | buts: -
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: aucune promesse donnée dans la fiche
hesitations:
  - ecran: 33 | cherchais: le bouton « + C ビルドアップ » | regarde: la liste des « 追加 » qui saute de ~235 px après chaque ajout
  - ecran: 77 | cherchais: la tranche « コード » de la ミックス | regarde: 4 tranches seulement (ドラム, Acid 303, ベース, サブ), aucun signe qu'il y en a d'autres
  - ecran: 45 | cherchais: pourquoi l'intro fait 8 小節 alors que la carte du ch.10 dit 16 | regarde: la frise à 48 小節 avant « おすすめの長さにする »
abandon: ecran 105 | parce que la fader « コード » ne bouge ni au glisser, ni au toucher, ni à l'appui long (ch.15 étape 1 sautée par « 次へ » ×2)
incidents-banc:
defauts:
  - geste: #104 | ecran.vu: 97 | cible: curseur [état « +0.0 dB », à 80 %] @ 451,516 (« コード ») | geste.type: glisse
    attendu: baisser un peu la fader コード (ch.15 « コードとメロディのフェーダーを少し下げよう ») | obtenu: reste à +0.0 dB après 4 glisser, 1 toucher, 1 appui long ; seul « 見せて » la bouge, puis remet tout
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 125 | banc?: geste ? (le curseur horizontal リバーブ センド a bien glissé écran 74) | promesse: -
  - geste: #34 | ecran.vu: 33 | cible: bouton « + C ビルドアップ » (liste « 追加 ») | geste.type: touche
    attendu: ajouter les parties dans l'ordre (ch.12 étape 1) | obtenu: après le 1er ajout la frise apparaît au-dessus, les boutons descendent ; mes touches tombent sur A puis B (frise B, A, B) ; 4 gestes de « ⋮ → 外す » + 1 défilement pour réparer, puis un défilement avant chaque E, F, G
    symptome: perdu | gravite: contourne-en-5-gestes | revu: oui: ecran 49 | banc?: non | promesse: -
  - geste: #77 | ecran.vu: 76 | cible: texte « サブ » @ 444,380 (tranches de la ミックス) | geste.type: glisse
    attendu: faire défiler la table de mixage jusqu'à « コード » (ch.14 « ミックスで「コード」トラックにも同じことをしよう ») | obtenu: deux glisser latéraux sans effet, aucune flèche ni bord coupé visible ; atteint seulement via « 見せて » (4 gestes de plus)
    symptome: cache | gravite: contourne-en-4-gestes | revu: oui: ecran 78 | banc?: non | promesse: -
  - geste: #112 | ecran.vu: 111 | cible: bouton « クラブ · −8 LUFS » [coupé] @ 687,753 | geste.type: touche
    attendu: choisir クラブ (ch.16) | obtenu: le bouton est à moitié sous le panneau du tuto, la touche ne fait rien ; il faut défiler
    symptome: cache | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: #2 | ecran.vu: 1 | cible: bannière « ハウス キットをダウンロード（4 MB） » / « あとで » | geste.type: touche
    attendu: lire la carte d'énergie du ch.10 | obtenu: la bannière jaune couvre le bas de la carte (nombres de mesures coupés, noms de sections cachés) jusqu'à « あとで »
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #67 | ecran.vu: 67 | cible: bouton « 1 » [état « 空、点線：タップしてね »] (D · ドロップ) | geste.type: touche
    attendu: poser la シンバル sur ドロップ ET ドロップ 2 (consigne ch.13 étape 3) | obtenu: « 成功 » dès la seule ドロップ ; ドロップ 2 jamais demandée
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #73 | ecran.vu: 73 | cible: texte « コード：ここで調整 · スネア：次 » (feuille « エフェクト · パッド 5（クラップ） ») | geste.type: touche
    attendu: une aide qui parle de la クラップ que je règle | obtenu: parle de コード et de スネア ; je ne sais pas ce que je règle
    symptome: incompris | gravite: finition | revu: oui: ecran 81 (« コード ✓ · スネア：次 ») | banc?: non | promesse: -
  - geste: #15 | ecran.vu: 15 | cible: texte « ドラム全部、ベース、ピアノ、ループはなし » | geste.type: touche
    attendu: comprendre ce qui joue | obtenu: la phrase se lit « toute la batterie, la basse, le piano » alors qu'il n'y a que la batterie
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #60 | ecran.vu: 60 | cible: consigne « 赤い×をそれぞれタップ » | geste.type: touche
    attendu: des × rouges à toucher (ch.13 étape 2) | obtenu: les cases sont « 取り消し線：消そう » (barrées), pas de × ; ça marche mais le mot ne colle pas
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #13 | ecran.vu: 13 | cible: légende イントロ…エンディング de la carte (ch.10 étape 5) | geste.type: touche
    attendu: lire les noms des sections | obtenu: tous [coupé] à 130 % ; aussi « 数字はパートの長さ（小節数）。 » coupé écran 3
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 3 | banc?: non | promesse: -
mots-faux:
  - ecran: 16 | vu: « ベース, サブ, コード, メロディ なし » | attendu: « ベース、サブ、コード、メロディなし » (virgules japonaises ; aussi écrans 18, 24, 28, 30)
  - ecran: 108 | vu: « ピアノ » / « ループ » | attendu: « コード » / « メロディ » (noms des pistes de la ミックス)
  - ecran: 86 | vu: « イントロ 小節 1/96 » en tête pendant que je travaille la ビルドアップ | attendu: la partie en cours
chapitres:
  - ch10 : compris du premier coup
  - ch11 : compris du premier coup (petit doute : « 4小節、フィル » jamais confirmé)
  - ch12 : hésitation (boutons « 追加 » qui sautent ; 8 vs 16 小節)
  - ch13 : compris du premier coup
  - ch14 : hésitation (piste コード introuvable dans la ミックス, passé par « 見せて »)
  - ch15 : bloqué (étape 1 sautée, fader immobile)
  - ch16 : compris du premier coup (1 touche perdue sous le panneau)
  - ch17 : compris du premier coup (partage M4A 3:09 vers Drive)
fin : chapitre 17 atteint ; le tuto annonce « 残り 2 ステップ » (ch.2, hors mission, et ch.15 étape 1) ; 125 gestes sur 181.
defauts-avant:
fin-tranche: non (dernier step=levels, done=38)
