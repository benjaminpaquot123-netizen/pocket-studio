siege: b | profil: t5-Db etudiante-son-chinoise-pliable | lang: zh-CN | device: foldable | font: 100 | mode: pro
mission: synthe | buts: son, regle, garde
promesse: id=t5-Db-1 | texte: 已有名为“明亮Reese”的保存音色：请换个名字。 | ecran: 35 | tenue: non | pourquoi: même nom refusé (pas de « mettre à jour »), et le nom tapé disparaît (retour à « 我的声音 1 », écran 37)
promesse: id=t5-Db-2 | texte: 点声音上的 ☆，就能在“收藏”里找到它。 | ecran: 39 | tenue: non | pourquoi: aucune ☆ sur les tuiles des dossiers 贝斯 (écran 4) et Lead (écran 40)
promesse: id=t5-Db-3 | texte: 明亮Reese 合成器 | ecran: 59 | tenue: non | pourquoi: la puce suit un son choisi dans la liste (écran 5, 45), mais pas un son qu'on vient de garder : « 明亮Reese 2 » coché, la puce dit « 明亮Reese »
promesse: id=t5-Db-4 | texte: 播放 | ecran: 9 | tenue: non | pourquoi: dans l'éditeur, transport en double (en haut 撤销/重做/播放/分享/BPM, en bas encore 撤销/重做/播放/速度/分享) ; il reste environ 300 px pour les réglages, 2 rangées de curseurs
hesitations:
  - ecran: 9 | cherchais: le bouton pour garder le son depuis l'éditeur | regarde: la phrase « “保留这个声音”会把它存进你的收藏 » est là, mais pas le bouton ; il faut rouvrir 更换音色
  - ecran: 23 | cherchais: l'éditeur de son après avoir fermé la liste | regarde: le panneau montre les ambiances d'effets (肥厚贝斯, Growl…) au lieu des curseurs ; il faut repasser par le menu ⋮
  - ecran: 31 | cherchais: le nom de mon son déjà gardé, pour le mettre à jour | regarde: le champ propose « 我的声音 1 »
abandon: jamais
incidents-banc:
  - geste: 7 | attente: 3 | vu: écran 8 presque vide (route tuto_spot_ring), l'éditeur arrive après « attends » ; pareil au geste 24
defauts:
  - geste: 34 | ecran.vu: 35 | cible: 已有名为“明亮Reese”的保存音色：请换个名字。 | geste.type: touche
    attendu: proposer de remplacer / mettre à jour « 明亮Reese » | obtenu: refus, il faut un autre nom (j'ai dû créer « 明亮Reese 2 »)
    symptome: incompris
    gravite: contourne-en-5-gestes | revu: non | banc?: non | promesse: t5-Db-1
  - geste: 35 | ecran.vu: 37 | cible: champ « 音色名称 » contenu « 我的声音 1 » @ 517,606 | geste.type: touche
    attendu: après « 知道了 », revenir au champ avec le nom que j'ai tapé | obtenu: la fenêtre se ferme ; en la rouvrant, le nom tapé est perdu (« 我的声音 1 »)
    symptome: perdu
    gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: t5-Db-1
  - geste: 39 | ecran.vu: 40 | cible: bouton « Supersaw 锯齿波 · 7 个声部 · 持续 » @ 749,398 | geste.type: touche
    attendu: une ☆ sur chaque tuile, comme le dit « 点声音上的 ☆ » | obtenu: aucune ☆ (ni en texte ni sur l'image) ; l'appui long choisit juste le son
    symptome: cache
    gravite: contourne-en-4-gestes | revu: oui: ecran 4 | banc?: non | promesse: t5-Db-2
  - geste: 56 | ecran.vu: 59 | cible: touchable « 明亮Reese 合成器 » (nom « 明亮Reese 的选项 ») @ 676,186 | geste.type: touche
    attendu: la puce montre « 明亮Reese 2 », le son coché dans 收藏 | obtenu: la puce garde l'ancien nom « 明亮Reese »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 23 | banc?: non | promesse: t5-Db-3
  - geste: 7 | ecran.vu: 9 | cible: bouton « 播放 » @ 512,1024 | geste.type: touche
    attendu: un seul transport, l'éditeur de synthé visible en entier sur le pliable | obtenu: transport en haut et en bas, clavier ouvert ; 3 glissés pour atteindre 音量
    symptome: cache
    gravite: contourne-en-3-gestes | revu: oui: ecran 27 | banc?: non | promesse: t5-Db-4
  - geste: 48 | ecran.vu: 49 | cible: curseur « 音量 » [état « 60% », à 60 %] @ 761,469 | geste.type: glisse
    attendu: le curseur bouge et la liste reste où elle était | obtenu: la valeur change (82 %) mais la liste remonte tout en haut, le curseur disparaît de l'écran
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: oui: ecran 27 | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 15 | cible: texte « 音色：合成器 » @ 161,97 | geste.type: touche
    attendu: le titre montre le son actuel (Reese 贝斯, puis 明亮Reese) | obtenu: toujours « 音色：合成器 »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 52 | banc?: non | promesse: -
  - geste: 52 | ecran.vu: 53 | cible: champ « 音色名称 » contenu « 我的声音 1 » @ 517,606 | geste.type: touche
    attendu: le nom proposé est celui du son en cours (« 明亮Reese ») | obtenu: « 我的声音 1 », il faut effacer et retaper
    symptome: incompris
    gravite: finition | revu: oui: ecran 31 | banc?: non | promesse: t5-Db-1
  - geste: 16 | ecran.vu: 17 | cible: bouton « 清除 » @ 865,613 | geste.type: touche
    attendu: le champ vidé reste prêt à écrire | obtenu: le champ perd le curseur, il faut le retoucher avant d'écrire
    symptome: perdu
    gravite: finition | revu: oui: ecran 54 | banc?: clavier | promesse: -
mots-faux:
  - ecran: 4 | vu: 上一个文件夹 | attendu: 返回上级文件夹（或 返回）
  - ecran: 13 | vu: Sub 贝斯（curseur de niveau） | attendu: 副振荡器 / Sub 振荡器
