code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege controle/t9-68/a | profil pro (niveau 4) | lang zh-CN | device phone | font 200 | mode pro
mission tuto-pro : finir les chapitres 1 à 5 du tuto techno | buts : ch1 ✓, ch2 ✓, ch3 ✓, ch4 ✓, ch5 bloqué à l'étape 2/4
promesse: tenue sur les chapitres 1 à 4 ; rompue au chapitre 5 (le piano roll de la basse reste caché derrière le panneau du tuto)
chapitre 1 « 速度和调性 » : compris du premier coup
chapitre 2 « 底鼓和轰鸣 » : hésitation (lecture par 3 lignes à 200 %, carte de téléchargement évoquée mais absente)
chapitre 3 « 拍手 » : compris du premier coup
chapitre 4 « 踩镲和打击乐 » : compris du premier coup
chapitre 5 « 贝斯 » : bloqué à l'étape 2/4
hesitations: écran 18-23 (carte Techno 鼓组 introuvable) ; écrans 85-101 (où est le piano roll de la basse ?)
abandon: écran 101, chapitre 5 étape 2/4, après 17 gestes à chercher le piano roll (gestes 102 et fin pour fermer)
incidents-banc: aucun

defauts:
- geste #84 touche « 创建贝斯音轨 » | ecran.vu 84, 87, 91, 95, 98 | cible piano roll de la basse (« 贝斯 · 第 1/8 小节 还有 1 个虚线音符要点 ») | geste.type touche
  attendu: le piano roll s'ouvre avec ses notes en pointillés sur F, assez grand pour les toucher | obtenu: seule une bande d'environ 30 px du clavier est visible sous l'en-tête ; le panneau du tuto la recouvre ; défiler, replier le panneau, le glisser, le bouton « 1 », « 我来做 », le menu ⋮ de la piste et le changement d'onglet ne la libèrent pas ; « 示范给我看 » tourne sans finir l'étape (« 还有 1 个虚线音符要点 » reste)
  symptome cache
  gravite mission-impossible (BLOQUANT)
  revu oui (écrans 84 à 101) | banc? non (le reste de l'écran est rendu normalement) | promesse rompue
- geste #2 touche « 下一步 » pendant l'écoute de l'exemple | ecran.vu 3 | cible message « 未收听，已跳过示例。 » | geste.type touche
  attendu: l'exemple compte comme écouté | obtenu: « 未收听，已跳过示例。 » alors que l'exemple jouait
  symptome texte-faux
  gravite finition
  revu oui | banc? non | promesse tenue
- geste #1 regarde | ecran.vu 1 et suivants | cible bouton du motif « A » ▾ @ 238,119 | geste.type regarde
  attendu: « A » lisible | obtenu: dessiné « .. » (tronqué) à 200 %
  symptome texte-coupe
  gravite finition
  revu oui (écran 95) | banc? non | promesse tenue
- geste #18 regarde | ecran.vu 18 | cible consigne « Techno 鼓组的卡片 … 稍后 » | geste.type regarde
  attendu: la carte de téléchargement nommée est à l'écran | obtenu: absente sur cette étape, elle réapparaît plus tard (fermée par « 稍后 » à #23)
  symptome incompris
  gravite finition
  revu oui | banc? non | promesse tenue
- geste #28 touche « 继续阅读 » | ecran.vu 26-30 | cible texte de l'étape | geste.type touche
  attendu: lire la consigne en une ou deux pages | obtenu: à 200 % le texte avance de 3 lignes à la fois, il faut plusieurs touches
  symptome lent
  gravite contourne-en-3-gestes
  revu oui | banc? non | promesse tenue
- geste #32 touche une case | ecran.vu 37 | cible grille des pas (Rumble ou grosse caisse ?) | geste.type touche
  attendu: le nom du pad édité visible au-dessus de la grille | obtenu: rien n'indique si on écrit le Rumble ou le kick
  symptome sans-nom
  gravite finition
  revu oui | banc? non | promesse tenue
- geste #42 regarde | ecran.vu 42 | cible dernière case « 空，虚线：点一下 » | geste.type regarde
  attendu: l'état dit « 长按 » comme la consigne | obtenu: « 点一下 » alors que la consigne demande un appui long
  symptome texte-faux
  gravite finition
  revu oui | banc? non | promesse tenue
- geste #44 appui-long puis « 柔和 » | ecran.vu 44 | cible carte de réussite « 太棒了，本章完成！ » | geste.type regarde
  attendu: carte entière avec le trophée | obtenu: carte coupée à droite et en bas, trophée absent
  symptome texte-coupe
  gravite finition
  revu oui | banc? non (le reste de l'écran est rendu) | promesse tenue
- geste #60 regarde | ecran.vu ~60 | cible consigne ch4 étape 3 « 那个错位的一下 » | geste.type regarde
  attendu: le pluriel, 4 cases en pointillés | obtenu: le singulier pour 4 cases
  symptome texte-faux
  gravite finition
  revu oui | banc? non | promesse tenue

mots-faux:
- écran 83 « 抽吸 » (sidechain) : attendu « 泵感 » ; le tuto disait « 泵压 » plus tôt, trois mots pour une même idée

bilan: 1 bloquant, 0 grave, 8 finitions ; budget non épuisé (103 gestes sur 150), arrêt au chapitre 5 étape 2/4

fin-tranche: non (dernier step=bass_do, done=15, chapters=1,2,3,4)
BENCH-VERDICT (journal l.564) : #102 BENCH-VERDICT ok where=seat.fin
