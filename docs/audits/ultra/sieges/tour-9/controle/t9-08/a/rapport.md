code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: pro | lang: zh-CN | device: phone | font: 200 | mode: pro
mission: tuto-pro (finir les chapitres 6-14 du tuto afro_house) | buts: chapitres 6-14
promesse: id=- | texte: 太棒了，成功了！ 未收听，已跳过示例。 | ecran: 11 | tenue: non | pourquoi: j'avais lancé l'exemple (il jouait encore), l'écran dit « non écouté »
chapitres:
  - ch6 你的第一段：高潮 : hésitation (j'ai cherché à renommer la section moi-même par le menu « A », pas d'option ; fini avec le bouton du tuto)
  - ch7 理解段落 : compris du premier coup
  - ch8 搭建你的段落 : bloqué à l'étape 4/6 (« 我来做 » : la grille et les pads restent sous le panneau du tuto ; le banc a clos le siège)
  - ch9 à ch14 : pas atteints (siège clos par le banc au geste #44, budget non épuisé : 37 gestes comptés sur 160)
hesitations:
  - ecran: 7 | cherchais: renommer la section « A » en « 高潮 » à la main, comme dans un DAW | regarde: le menu « 要编辑的段落 » n'offre que « A » et « 新的空段落 », rien pour renommer
  - ecran: 26 | cherchais: voir comment créer le « 前奏 » avec « 示范给我看 » | regarde: le doigt montre seulement l'onglet « 段落 » puis « 好了！现在轮到你。 », le bouton d'ajout reste sous le panneau
  - ecran: 44 | cherchais: les pads conga / woodblock / open hat pour ajouter des coups | regarde: zone de travail d'environ 190 px entre « 段落：D · 高潮 2 » et le panneau replié, je ne retrouve plus la grille
abandon: ecran 44 | parce que le banc a clos le siège (5 écrans identiques) pendant que je cherchais à remonter vers les pads
incidents-banc:
  - geste: 44 | attente: 0 | vu: « Fin du siège : rien ne change (5 écrans identiques) » ; mes glissés vers le bas partaient de l'en-tête fixe « 段落：D · 高潮 2 », siège fermé à 37 gestes sur 160
defauts:
  - geste: 34 | ecran.vu: 44 | cible: bouton (doigt qui touche) «   我来做 » | geste.type: touche
    attendu: arriver sur les pads et la grille pour ajouter quelques coups de conga / woodblock / open hat | obtenu: l'éditeur s'ouvre sur la ligne « 鼓边 », la grille et le choix du pad sont coupés ou cachés sous le panneau du tuto (même replié), zone utile ~190 px ; je ne retrouve pas les pads
    symptome: cache | gravite: mission-impossible | revu: non | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 26 | cible: bouton (œil) «  示范给我看 » | geste.type: touche
    attendu: la démo montre comment créer le « 前奏 » | obtenu: le doigt pointe l'onglet « 段落 » puis « 好了！现在轮到你。 » ; le bouton d'ajout « + A · 高潮 » [coupé] reste sous le panneau, rien n'est montré
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 12 | cible: icône « 下一步  » → | geste.type: touche
    attendu: passer à l'étape suivante, l'exemple ayant été écouté | obtenu: « 未收听，已跳过示例。 » alors que l'exemple jouait encore (encore 2 s)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 6 | ecran.vu: 7 | cible: bouton « A » ▾ | geste.type: touche
    attendu: renommer la section depuis son menu | obtenu: le menu ne propose que choisir « A » ou « 新的空段落 » ; appui long sur « A » ferme le menu
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: oui: ecran 8 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 3 | cible: bouton « A » ▾ | geste.type: touche
    attendu: lire le nom de la section en cours dans la barre du haut | obtenu: la puce affiche « .. » (nom coupé), alors qu'elle n'a qu'une lettre
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 32 | ecran.vu: 33 | cible: icône « 继续阅读 » ⌄ | geste.type: touche
    attendu: lire tout le message « 已创建段落“高潮 2”… » | obtenu: la boîte défile de quelques lignes seulement, lignes tranchées en haut et en bas, jamais lisible en entier
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 32 | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 14 | cible: texte « 16 » | geste.type: touche
    attendu: voir les blocs de sections (noms + longueur) sous le graphe d'énergie | obtenu: une rangée de « 16 » sans nom de section, cartes tranchées en bas, la dernière coupée « 1… »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 16 | banc?: non | promesse: -
mots-faux:
  - ecran: 28 | vu: 没有拍手, 开镲, 鼓边, 贝斯, 和弦, 旋律 (virgules latines « , ») | attendu: 没有拍手、开镲、鼓边、贝斯、和弦、旋律
  - ecran: 32 | vu: 录音棚底鼓, 拍手, 踩镲 (virgules latines) | attendu: 录音棚底鼓、拍手、踩镲

fin-tranche: non (dernier step=variation, done=24)
