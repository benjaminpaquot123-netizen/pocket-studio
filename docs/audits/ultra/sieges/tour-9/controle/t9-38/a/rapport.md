code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: controle/t9-38/a
profil: pro
lang: zh-CN · device: foldable · font: 200 · mode: pro
mission: tuto-pro — finir les chapitres 8 à 15 du tuto jersey_club (amené au chapitre 8)
buts: ch8 compris du premier coup · ch9 hésitation · ch10 hésitation · ch11 compris du premier coup · ch12 hésitation · ch13 hésitation · ch14 compris du premier coup · ch15 compris du premier coup
promesse: tenue — les 8 chapitres finis (« 太棒了，你的曲子完成了！ » écran 97), WAV exporté et partagé, 108 gestes sur 150
hesitations: écran 14 (le menu « A · 副歌 ▾ » n'offre que « 新的空段落 », pas de copie) · écran 54 (toucher une ligne de 时间线 lance la lecture au lieu d'ouvrir « 长度 ») · écran 76 (noms de pistes du 混音 cachés, 4e fader deviné)
abandon: aucun
incidents-banc: aucun (aucun outil hors human-act.py ; seuls fichiers lus : ceux du siège) ; geste #56 « ecris 16 » sans effet après ✕ sur le champ 小节, réussi après un toucher du champ (#58-59) : banc? clavier

defauts:
- geste #19 · ecran.vu 20 · cible « 已创建段落“主歌”（“副歌”的副本）：没有拍手, 开镲。 » · geste.type touche · attendu: une confirmation claire en chinois · obtenu: virgules anglaises « , » et phrase ambiguë (« 没有…» : copie sans ces sons ?), idem écran 24 « 没有录音棚底鼓, 拍手, 踩镲… » · symptome texte-faux · gravite finition · revu oui · banc? non · promesse non
- geste #18 · ecran.vu 18-19 · cible panneau du tuto (⌄) · geste.type touche · attendu: replier le tuto pour voir la grille 积木 · obtenu: à 200 % le panneau couvre presque tout 结构, même replié, et se redéplie à chaque étape · symptome cache · gravite finition · revu oui · banc? non · promesse non
- geste #29-49 · ecran.vu 29-49 · cible « 在末尾添加 » · geste.type touche · attendu: ajouter 10 entrées d'affilée · obtenu: après chaque ajout la liste saute sur la nouvelle entrée, les boutons passent sous le panneau, un glisser de plus à chaque fois (≈9 gestes perdus) · symptome cache · gravite contourne-en-9-gestes · revu oui · banc? non · promesse non
- geste #54 · ecran.vu 54-55 · cible « A · 副歌 × 1 · 8 小节 » · geste.type touche · attendu: le tuto dit « 点一个段落，然后在“长度”下面输入小节数 » → éditeur de longueur · obtenu: la ligne lance « 播放中 » ; la longueur est dans ⋮ · symptome incompris · gravite contourne-en-1-geste · revu oui · banc? non · promesse non
- geste #21 · ecran.vu 22-23 · cible « 示范给我看 » · geste.type touche · attendu: voir la démonstration du geste · obtenu: « 看，我来做… » puis rien de visible, étape non faite · symptome rien · gravite finition · revu oui · banc? non · promesse non
- geste #7 · ecran.vu 8 · cible « 下载 Jersey club 鼓组（1 MB） » · geste.type regarde · attendu: lire le graphe 能量 en entier · obtenu: la bannière jaune couvre le bas du graphe (blocs et numéros de mesures coupés) ; elle revient à chaque étape de chaque chapitre · symptome cache · gravite finition · revu oui · banc? non · promesse non
- geste #71 · ecran.vu 71 · cible « 旋律 » · geste.type touche · attendu: une feuille titrée « 旋律 » · obtenu: feuille sans nom de piste, titre « XY 打击垫，上下： » coupé en haut · symptome texte-coupe · gravite finition · revu oui · banc? non · promesse non
- geste #76 · ecran.vu 76 · cible fader « -3.2 » · geste.type glisse · attendu: savoir quel fader est 旋律 · obtenu: panneau replié, le 混音 est défilé et les noms (鼓/Acid/808/旋律) sont hors écran ; idem 镲片 invisible au ch. 11 · symptome sans-nom · gravite finition · revu oui · banc? non · promesse non
- geste #84-88 · ecran.vu 84, 87, 100, 106 · cible « 约 −8.5 LUFS » / « 自动（按风格） · −9 LUFS » · geste.type regarde · attendu: les mêmes chiffres partout · obtenu: tuto −8.5, bouton −9 ; feuille 分享 « 音量：−9.0 » et 详情 « −13.6 LUFS » côte à côte sans dire avant/après ; « 5 个段落 » (分享) contre « 10 个段落 » (我的项目) · symptome texte-faux · gravite finition · revu oui · banc? non · promesse non
- geste #90-107 · ecran.vu 90, 99, 107 · cible « 分轨给混音师 » · geste.type touche · attendu: une sortie 分轨 (stems) pour un mixeur, comme l'annonce le chapitre 15 · obtenu: ni la feuille 分享 (M4A/WAV/视频) ni le ⋮ du projet (.zip) n'offrent de 分轨 · symptome perdu · gravite contourne-en-N-gestes (non trouvé en 4 gestes) · revu oui · banc? non · promesse non

mots-faux: « 按顺序点结尾是 前奏、副歌…的按钮 » (écran 27, tournure bancale) ; virgules anglaises dans les confirmations (écrans 20, 24)
defauts-avant: aucun défaut visé dans le brief

self-test : vert au code testé 2b18313c4 (324 verts, 0 rouge) ; au 2929a866e il était 324/1 rouge (BenchFlagTest#drive non listé dans les sauts connus), le chef a fait passer le siège sur 2b18313c4 (seule différence : cette ligne du banc, appli identique).
BENCH-VERDICT : #108 BENCH-VERDICT ok where=seat.fin
fin-tranche: oui (ROUTE ligne 602)
