siege: c | profil: t6-Dc producteur-chinois-tablette | lang: zh-CN | device: tablet8 | font: 100 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t6-Dc-1 | texte: 1. Le tuto Pro se suit sur au moins 3 chapitres sans impasse | ecran: 84 | tenue: oui | pourquoi: chapitres 1 à 4 finis et 5 entamé sans impasse (« 4 踩镲和打击乐 » coché)
promesse: id=t6-Dc-2 | texte: 2. Chaque étape se valide quand je fais le geste demandé | ecran: 47 | tenue: non | pourquoi: j'ai écouté l'exemple puis « 下一步 » : l'appli dit « 太棒了，本章完成！ » mais la liste montre « 1 速度和调性 3/4 »
promesse: id=t6-Dc-3 | texte: 3. Le Guide s'ouvre depuis le studio et son texte est complet | ecran: 56 | tenue: non | pourquoi: « 打开我这首歌的向导 » ouvre juste l'onglet 结构, sans aucun texte de guide ; l'autre « 向导 » ouvre « 帮我做一首 » (nouvelle chanson)
promesse: id=t6-Dc-4 | texte: 4. chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 34 | tenue: non | pourquoi: 混响、失真、滤到 200 Hz、抽吸、弗里几亚、幽灵音、十六分音符、钢琴卷帘、小二度、截止 : rien n'est expliqué
promesse: id=t6-Dc-5 | texte: 5. tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 28 | tenue: non | pourquoi: « four-on-the-floor », « Pattern », « Ride », « Drop » restent en anglais ; le bouton « 效果 » est coupé (écran 89)
oeil-neuf: q1 | ecran: 110 | image: 110 | on voit deux ▶ en haut (« 这个段落 », « 歌曲 ») et le grand « 我来做 » ; « 效果 » coupé à droite ; en piano roll la barre 撤销/歌曲/速度 du bas disparaît
oeil-neuf: q6 | ecran: 110 | image: 110 | la preuve est la barre verte (3e segment) et « 第 3/3 步 » ; pas de « 成功 » visible pour l'étape précédente une fois passée
oeil-neuf: q9 | ecran: 110 | image: 110 | « 效果 » coupé au bord droit ; la bande « 小节 3 4 » ne montre que la mesure 4 sans signe que d'autres mesures suivent
hesitations:
  - ecran: 14 | cherchais: où régler la tonalité « F 弗里几亚 » : aucun bouton de tonalité visible au studio | regarde: il a fallu « 我来做 » pour ouvrir « 歌曲调性 »
  - ecran: 19 | cherchais: où est le réglage « Swing » | regarde: caché derrière le panneau du tuto, trouvé seulement avec « 示范给我看 »
  - ecran: 55 | cherchais: le Guide | regarde: deux entrées « 我的第一首歌，跟着向导做 » et « 打开我这首歌的向导 », je ne sais laquelle est le guide
abandon: ecran 110 | parce que budget de 120 gestes presque épuisé au chapitre 5/16 (pas un blocage) ; « bravo » hors de portée en 120 gestes
incidents-banc:
  - geste: 20 | attente: 0 | vu: écran 21 texte vide alors que l'image montre le studio (aussi écrans 35, 36, 43, 62, 63, 70, 77, juste après « 我来做 » ou « 示范给我看 »)
  - geste: 0 | attente: 0 | vu: outils hors human-act : filtres shell (head/sed/grep) sur la sortie du banc et un fichier temporaire last.txt dans mon siège (supprimé) ; une fois trois touches dans un même appel (#29 à #31)
defauts:
  - geste: 46 | ecran.vu: 47 | cible: bouton « 1 速度和调性 3/4 » | geste.type: touche
    attendu: un chapitre annoncé « 本章完成 » apparaît coché dans la liste | obtenu: chapitres 1 et 2 restent « 3/4 » (étape d'écoute quittée avant la fin non comptée, sans le dire)
    symptome: incompris | gravite: finition | revu: oui: ecran 84 | banc?: temps | promesse: t6-Dc-2
  - geste: 55 | ecran.vu: 56 | cible: bouton « 打开我这首歌的向导 » | geste.type: touche
    attendu: le guide de ma chanson s'ouvre avec son texte | obtenu: onglet 结构 seul, aucun texte de guide, le panneau du tuto reste
    symptome: mauvais-ecran | gravite: contourne-en-3-gestes | revu: oui: ecran 59 | banc?: non | promesse: t6-Dc-3
  - geste: 45 | ecran.vu: 46 | cible: icône (coupe) « 太棒了，本章完成！ » | geste.type: touche
    attendu: le fantôme doit être « 柔和 » : l'étape refuse un coup fort ou explique | obtenu: validé avec 力度 100%, je ne comprends pas pourquoi c'est réussi (pareil « 轻一点 » au #67)
    symptome: incompris | gravite: finition | revu: oui: ecran 68 | banc?: non | promesse: t6-Dc-2
  - geste: 33 | ecran.vu: 34 | cible: texte « 轮到你：Rumble 每拍一下：第二个长长的底鼓，泡在混响里并且失真，滤到 200 Hz 以下，每次底鼓时被抽吸压低。 » | geste.type: touche
    attendu: une consigne simple « 点 Rumble 的虚线格子 » | obtenu: phrase pleine de jargon, rien ne dit quoi toucher
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Dc-4
  - geste: 87 | ecran.vu: 88 | cible: texte « 轮到你：创建贝斯音轨（“模拟贝斯”音色，抽吸已经加好）。钢琴卷帘里的第 1 小节：每两个底鼓之间两个十六分音符，全在 F 上。 » | geste.type: touche
    attendu: étape courte, mots expliqués | obtenu: 5 actions et 6 mots techniques dans une seule étape, tronquée par « … » une fois repliée
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Dc-4
  - geste: 68 | ecran.vu: 69 | cible: texte « 轮到你：错位的鼓边，循环里的小节奏钩子。 » | geste.type: touche
    attendu: un verbe : quoi faire | obtenu: pas d'action dite, j'ai dû toucher « 我来做 » pour voir les cases
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Dc-4
  - geste: 18 | ecran.vu: 19 | cible: texte « 轮到你：加一点点 Swing（5%）：小踩镲稍微拖后一点，律动在呼吸，但不会瘸。 » | geste.type: touche
    attendu: le réglage Swing visible ou indiqué | obtenu: curseur caché sous le panneau du tuto ; la démo le pousse à 7 % au lieu de 5 %
    symptome: cache | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t6-Dc-4
  - geste: 88 | ecran.vu: 89 | cible: bouton « 效果 » [coupé] | geste.type: touche
    attendu: bouton entier | obtenu: « 效果 » coupé au bord droit dans la rangée des pistes
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 110 | banc?: non | promesse: t6-Dc-5
  - geste: 4 | ecran.vu: 5 | cible: bouton « 120  BPM » | geste.type: touche
    attendu: j'ai choisi « Techno 132 BPM », le studio affiche 132 | obtenu: 120 BPM, puis le tuto me demande de mettre 132 moi-même
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 28 | vu: four-on-the-floor | attendu: 四拍底鼓（每拍一下）
  - ecran: 97 | vu: Pattern 放好了！ | attendu: 节奏型放好了！
  - ecran: 76 | vu: Ride | attendu: 叮叮镲（打击垫上写的是「镲片」）
  - ecran: 5 | vu: Drop | attendu: 高潮段（Drop）
