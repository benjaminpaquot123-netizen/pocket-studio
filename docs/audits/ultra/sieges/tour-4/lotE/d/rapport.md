siege: d | profil: Étudiante en son chinoise (rejeu tour 3) | lang: zh-CN | device: foldable | font: 100 | mode: pro
mission: synthe | buts atteints: 3 sur 3
hesitations:
  - ecran: 10 | cherchais: le bouton pour enregistrer mon réglage dans l'éditeur | regarde: haut du panneau (seulement le texte « “保留这个声音”会把它存进你的收藏 »), puis tout en bas
  - ecran: 43 | cherchais: « mettre à jour 我的低音 » au lieu d'en créer un nouveau | regarde: dossier 收藏, appui-long sur la tuile, menu de la piste
  - ecran: 56 | cherchais: « 添加音轨 » avec un son de 收藏 | regarde: toute la liste 添加音轨 (aucune entrée favoris), puis 更换音色 après coup
promesse: Éditeur de synthé utilisable sur pliable, preset mis à jour, piste avec son favori | tenue: en partie | pourquoi: éditeur et favori sur nouvelle piste OK, mais aucune mise à jour d'un favori : il faut le supprimer puis le recréer
surprise: un appui-long sur le nom d'un curseur ouvre « 自动化 » (courbes), rien ne l'annonce
abandon: jamais
defauts:
  - ecran: 43 | cible: 保留 (nom « 我的低音 » déjà pris) | geste: touche
    attendu: proposer « 更新/覆盖 » le favori existant | obtenu: « 已有名为“我的低音”的保存音色：请换个名字。 », le nom tapé est perdu après 知道了 (écran 44)
    symptome: promesse | gravite: grave | bloque: non
  - ecran: 33 | cible: « 点声音上的 ☆，就能在“收藏”里找到它。 » | geste: touche
    attendu: une étoile ☆ sur chaque tuile de son | obtenu: aucune ☆ sur les tuiles (écran 74, dossier Lead), seul le gros bouton 保留这个声音
    symptome: promesse | gravite: moyen | bloque: non
  - ecran: 62 | cible: puces de piste « 贝斯 合成器 » / « 现代 Reese 合成器 » | geste: touche
    attendu: voir le son 我的低音 sur la piste qui l'utilise | obtenu: la puce dit « 合成器 » ; la 1re piste garde « 现代 Reese » alors que son son est devenu 我的低音
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 12 | cible: panneau 音色设置 au-dessus du clavier | geste: glisse
    attendu: sur pliable ouvert, réglages et clavier côte à côte | obtenu: fenêtre de ~350 px, 6 glissés pour tout voir, barre transport en double (haut et bas)
    symptome: coupe | gravite: moyen | bloque: non
  - ecran: 35 | cible: 我的低音 (appui-long dans 收藏) | geste: appui-long
    attendu: un menu (mettre à jour, renommer…) | obtenu: le son est appliqué tout de suite et remplace mes réglages non sauvés (取消 les rend, heureusement)
    symptome: mauvais-ecran | gravite: mineur | bloque: non
  - ecran: 88 | cible: 录下我的旋律 | geste: touche
    attendu: lancer l'enregistrement | obtenu: le bouton s'est déplacé (apparition de « 保留我刚弹的 », le bouton A du haut disparaît), touche dans le vide ; puis écran 89 « 没有可保留的内容 » alors que j'avais joué
    symptome: rien | gravite: moyen | bloque: non
  - ecran: 10 | cible: touchable sans nom à côté de 截止/共振/释放… ; libellé « Sub 贝斯 » | geste: appui-long
    attendu: noms parlants (« 副振荡器 » pour l'oscillateur sub) | obtenu: zones sans nom pour TalkBack ; « Sub 贝斯 » se lit comme un preset de basse, pas un réglage
    symptome: sans-nom | gravite: mineur | bloque: non
