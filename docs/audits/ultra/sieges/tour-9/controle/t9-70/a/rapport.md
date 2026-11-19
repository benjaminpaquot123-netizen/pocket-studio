code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: pro | lang: zh-CN | device: foldable | font: 130 | mode: pro
mission: tuto techno, chapitres 11 à 16 | buts: ch11-16 finis (tous atteints, chapters=1..16 au geste #95)
chapitres:
  - ch11 排列你的段落 : hésitation (le premier « + B » change d'onglet, boutons d'ajout cachés sous le pli)
  - ch12 过渡 : compris du premier coup
  - ch13 效果 : hésitation (curseur d'envoi réverb raté, trait de courbe qui fait défiler)
  - ch14 混音 : hésitation (tranches de la table sans nom une fois défilées)
  - ch15 母带处理 : compris du premier coup (liste des cibles sous le panneau du tuto, un glisser)
  - ch16 分享你的曲子 : compris du premier coup (WAV, −8.0 LUFS, fichier nommé)
promesse: id=- | texte: 太棒了，你的曲子完成了！ | ecran: 97 | tenue: oui | pourquoi: les 6 chapitres finis, export WAV club partagé
hesitations:
  - ecran: 4 | cherchais: ajouter les sections B à H dans 段落 | regarde: l'appli m'a envoyé sur 时间线, j'ai dû revenir à 段落 et défiler
  - ecran: 74 | cherchais: la tranche 旋律 pour baisser son fader | regarde: après défilement, plus aucun nom de piste, deviné à l'icône courbe
  - ecran: 82 | cherchais: le choix « 俱乐部 » de 目标响度 | regarde: le bouton s'allume mais la liste est cachée sous le panneau du tuto, il faut défiler
abandon: jamais
incidents-banc:
  - geste: 67 | attente: 0 | vu: journal « DOIGT rien » alors que le trait a bien dessiné la courbe (idem #68)
defauts:
  - geste: 3 | ecran.vu: 4 | cible: + B · 前奏 | geste.type: touche
    attendu: la section B s'ajoute et je reste dans la liste 段落 | obtenu: la section s'ajoute mais l'écran bascule sur l'onglet 时间线
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 54 | ecran.vu: 55 | cible: 混响 发送 | geste.type: glisse
    attendu: tirer l'envoi réverb depuis 0 % en partant du début de la piste | obtenu: rien ne bouge : à 0 % aucune piste n'est dessinée, il faut viser le bouton
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 56 | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 66 | cible: 用手指画；快速点两下删除一个点。“平滑”可以抚平抖动的线条，“撤销”可以撤回。 | geste.type: glisse
    attendu: dessiner la courbe de filtre en diagonale | obtenu: la page défile d'environ 80 px au lieu de dessiner
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 67 | banc?: non | promesse: -
  - geste: 63 | ecran.vu: 63 | cible: 在鼓上 · 1 小节 | geste.type: touche
    attendu: un libellé qui parle de la piste 旋律 que j'automatise | obtenu: l'option dit « 在鼓上 » (sur la batterie) pour une piste mélodique
    symptome: texte-faux | gravite: finition | revu: oui: ecran 65 | banc?: non | promesse: -
  - geste: 74 | ecran.vu: 74 | cible: 旋律 的音量 | geste.type: glisse
    attendu: voir le nom de chaque tranche de la table de mixage | obtenu: une fois la table défilée, les tranches n'ont plus de nom (seulement icônes et dB)
    symptome: sans-nom | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 81 | ecran.vu: 82 | cible: 自动（按风格） · −9 LUFS | geste.type: touche
    attendu: la liste des cibles de volume s'ouvre sous mes yeux | obtenu: elle s'ouvre sous le panneau du tuto, invisible ; il faut glisser pour la trouver
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 83 | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 66 | cible: 18.0 kHz | geste.type: glisse
    attendu: lire les fréquences de l'axe de la courbe | obtenu: « 18.0 kHz » et « 849 Hz » coupés au bord
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 30 | cible: 32 小节 | geste.type: touche
    attendu: lire la longueur des sections | obtenu: « 小 » et « 节 » sur deux lignes dans les rangées de la chanson
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: 高潮 小节 33/128 | geste.type: touche
    attendu: lire la position dans la barre du haut | obtenu: la ligne « 小节 …/128 » est rognée en bas
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 82 | banc?: non | promesse: -
  - geste: 94 | ecran.vu: 94 | cible: 取消 | geste.type: touche
    attendu: un bouton Annuler lisible pendant l'export | obtenu: « 取消 » gris très pâle sur fond sombre, à peine lisible
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 63 | vu: 在鼓上 | attendu: 在旋律上
defauts-avant:
  - aucun défaut visé donné par le brief
gestes: 98 sur 150 (fin au geste #98)
fin-tranche: oui (ROUTE ligne 541)
