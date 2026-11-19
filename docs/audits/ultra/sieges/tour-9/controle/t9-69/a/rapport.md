code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

```
siege: a | profil: pro | lang: zh-CN | device: phone360 | font: 100 | mode: pro
mission: tuto-pro (tuto techno, chapitres 6 à 10) | buts: chapitres 6-10 finis
promesse: id=- | texte: 已创建段落“高潮”：整个律动都在里面演奏。 | ecran: 71 | tenue: oui | pourquoi: la liste des sections montre bien A à H (律动、前奏…结束) à l'écran 80
promesse: id=- | texte: 和弦 · 第 4/8 小节 所有虚线音符都放好了 | ecran: 9 | tenue: oui | pourquoi: les deux stabs posés, chapitre 6 coché aussitôt après « 复制 »
hesitations:
  - ecran: 40 | cherchais: renommer la section A moi-même (menu « A ▾ », puis appui long sur « A ») | regarde: le menu n'a que « A » et « 新的空段落 », l'appui long ferme le menu ; j'ai pris le bouton du tuto
  - ecran: 26 | cherchais: le bouton « 设置音轨的音色 ↓ » sous « 键盘 »/« 音符 » que décrit la consigne | regarde: l'éditeur montre « 和弦 »/« 幽灵贝斯 », pas « 键盘 »/« 音符 » ; « 我来做 » m'a amené directement aux curseurs
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun ; outils hors human-act.py : lecture des fichiers de mon dossier de siège (start.txt, journal.txt, captures) et écriture de rapport.md
defauts:
  - geste: 13 | ecran.vu: 14 | cible: icône « 下一步  » → | geste.type: touche
    attendu: l'exemple que j'ai lancé (il jouait encore) compte comme écouté | obtenu: « 未收听，已跳过示例。 » alors que je l'avais lancé
    symptome: texte-faux | gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: bouton « 旋律 滤波 Lead » ⋮ | geste.type: glisse
    attendu: la piste garde le nom de son preset (« 滤波 Lead », au pire marqué modifié) quand je monte le 截止 | obtenu: l'onglet devient « 旋律 Plaits », le nom du preset disparaît
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 56 | cible: icône « 继续阅读 » ⌄ | geste.type: touche
    attendu: la suite de la consigne s'affiche | obtenu: le panneau défile vers le bas et la consigne de l'étape (« 听间奏… ») sort de l'écran ; il faut faire glisser pour la revoir
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 57 | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 45 | cible: texte « 结束 » @ 489,493 | geste.type: touche
    attendu: le schéma des sections tient dans la largeur | obtenu: la case « 结束 16 » est coupée au bord droit (« 1… »), la légende du dessous est cachée (« … »), la petite case 渐强段 n'a ni nom ni longueur
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 56 | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: texte « 已创建段落“前奏”（律动的副本）：没有 Rumble, 拍手, 镲片, 贝斯, 和弦, 旋律。 » | geste.type: touche
    attendu: une liste en chinois avec « 、 » | obtenu: virgules anglaises « , » et « Rumble » non traduit (même chose écrans 69 à 79)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: -
mots-faux:
  - ecran: 8 | vu: Pattern 放好了！ | attendu: 节奏型放好了！ (ou 段落型)
  - ecran: 49 | vu: 贝斯和 Stab 进来 | attendu: 贝斯和短促和弦进来 (le chapitre 6 dit « 短促和弦 »)
  - ecran: 57 | vu: stab 和踩镲 | attendu: 短促和弦和踩镲
  - ecran: 67 | vu: Rumble | attendu: 隆隆底鼓 (ou nom traduit de la piste)
  - ecran: 67 | vu: « , » entre les pistes | attendu: « 、 »
chapitres:
  - 6 短促和弦 : compris du premier coup (9 gestes)
  - 7 重复乐句 : compris du premier coup (consigne « 键盘/音符 » ne colle pas à l'écran, mais « 我来做 » y mène)
  - 8 律动 : hésitation (renommer à la main introuvable depuis « A ▾ »)
  - 9 理解段落 : compris du premier coup
  - 10 搭建你的段落 : compris du premier coup ; fin du chapitre 10 atteinte (écran 79, « 第 10/16 章 · 已完成 »)
```

fin-tranche: oui (ROUTE ligne 382)
