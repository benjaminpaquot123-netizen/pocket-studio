code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: pro | lang: zh-CN | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro (afro_house chapitres 1-5) | buts: - (aucun but donné au siège)
promesse: id=- | texte: aucune promesse donnée | ecran: - | tenue: pas vue | pourquoi: pas de fiche de promesse pour ce siège
chapitres:
  - ch1 速度和调性 : compris du premier coup
  - ch2 打击乐 : hésitation (le texte nomme un pad, la grille en montre un autre)
  - ch3 贝斯 : hésitation (bouton « 复制到后面的小节 » sans effet visible ; nom d'une piste de la table de mixage sans effet)
  - ch4 铺底 : hésitation (texte « Am7 », la grille attend Cmaj7)
  - ch5 马林巴 : hésitation (impossible d'ouvrir les effets de « 旋律 » soi-même, passé par « 示范给我看 »)
  - chapitres 1-5 finis, ≈144 gestes sur 153
hesitations:
  - ecran: 43 | cherchais: le pad 木块 que le texte me demande | regarde: la grille dit « 步进：沙锤 », aucun pad 木块 dans le kit
  - ecran: 111 | cherchais: l'accord Am7 de la mesure 3 | regarde: les notes en pointillé sont C E G B (Cmaj7)
  - ecran: 127 | cherchais: l'envoi d'écho de la piste « 旋律 » | regarde: toucher ou appuyer longtemps le nom de la piste n'ouvre rien
abandon: jamais
incidents-banc:
  - geste: 18 | attente: 0 | vu: écran texte vide après le geste, un « regarde » l'a rendu
  - geste: 78 | attente: 0 | vu: écran texte vide après le geste, un « regarde » l'a rendu
  - geste: 103 | attente: 0 | vu: écran texte vide deux fois (geste + regarde), seule l'image screen-103-grille.png montrait la feuille d'effets
defauts:
  - geste: #23 | ecran.vu: 23, 39, 43 | cible: « 轮到你：2 小节的 3-3-2 康加鼓… » / « 在沙锤打击垫上… » / « 在木块打击垫上… »
    attendu: la grille de pas s'ouvre sur le pad que le texte nomme | obtenu: la grille est sur un autre pad (步进：鼓边 / 踩镲 / 沙锤) ; il n'y a pas de pad 木块 dans le kit
    symptome: texte-faux ; je ne sais plus sur quel pad poser les notes
    gravite: contourne-en-2-gestes (grave pour moi : faux à 3 étapes du chapitre) | revu: oui: ecran 40 (我来做, la grille est restée 踩镲) | banc?: non | promesse: -
  - geste: #112 | ecran.vu: 109-111 | cible: « 轮到你：创建和弦音轨（一层 Pad）：Em9 两小节，然后 Am7 和 Gmaj7，各延续一小节。 »
    attendu: les pointillés de la mesure 3 forment un Am7 (A C E G) | obtenu: les pointillés forment C E G B (Cmaj7) ; j'ai posé un Am7, il ne compte pas, j'ai dû annuler
    symptome: texte-faux (la grille contredit l'accord nommé)
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: #102 | ecran.vu: 102, 127, 128, 132 | cible: « 滑动推子调音量；点音轨名称查看它的效果和曲线。 »
    attendu: toucher le nom de la piste (贝斯, 旋律) ouvre ses effets | obtenu: rien ne s'ouvre (toucher simple, appui long, bandeau de couleur) ; seuls « 我来做 » ou « 示范给我看 » ouvrent la feuille « 效果 · 旋律 »
    symptome: geste promis par l'écran sans effet ; je ne peux pas régler moi-même l'envoi d'écho
    gravite: contourne-en-4-gestes (grave) | revu: oui: ecran 127 | banc?: non | promesse: -
  - geste: #98 | ecran.vu: 98 | cible: « 复制到后面的小节 → »
    attendu: la mesure est copiée et l'étape avance | obtenu: rien de visible au chapitre 3, il a fallu toucher « 复制 » dans l'éditeur (au chapitre 4 #112 et 5 le même bouton a marché)
    symptome: bouton sans effet une fois sur trois
    gravite: contourne-en-1-gestes (finition) | revu: oui: ecran 109 (a marché) | banc?: non | promesse: -
  - geste: #133 | ecran.vu: 131-133 | cible: « 好了！现在轮到你。 »
    attendu: après la démo, je refais le geste moi-même ; ou l'étape se valide avec la démo | obtenu: la démo a monté l'écho à 67 %, le bandeau dit « 现在轮到你 » mais il n'y a plus de 我来做 ni de moyen d'ouvrir les effets ; l'étape s'est validée toute seule au geste suivant sans rapport
    symptome: état flou après « 示范给我看 »
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #13 | ecran.vu: 14 | cible: panneau de tonalité (小调 / E)
    attendu: le panneau reste en place | obtenu: il saute vers le haut après le premier toucher
    symptome: décalage de mise en page
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #3 | ecran.vu: 3 | cible: « 稍后 »
    attendu: le bouton disparaît ou se grise pendant le téléchargement du kit | obtenu: il reste actif pendant le téléchargement
    symptome: finition
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #? (ch1) | ecran.vu: ch1 | cible: « 下一步 » (gros bouton 450,1117) et icône « 下一步 » (801,1201)
    attendu: un seul bouton pour avancer | obtenu: deux « 下一步 » à la fois, le libellé « 上一步 » disparaît
    symptome: doublon de boutons
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #? (ch2 étape 4) | ecran.vu: ch2 étape 4 | cible: « 在打击之间更响 »
    attendu: l'étape me demande un accent puisque le texte en parle | obtenu: aucun accent demandé ni vérifié
    symptome: texte qui ne correspond pas à la tâche
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #43 | ecran.vu: 43 | cible: « 和康加鼓交错 »
    attendu: les pointillés tombent entre les coups de conga | obtenu: ils sont aux mêmes places que le 3-3-2 des congas
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 78 | vu: « 恢复为 开镲 音色 » | attendu: une phrase qui dit pourquoi le son revient (non expliqué)
  - ecran: 108 | vu: « Pattern 放好了！ » | attendu: « 模式放好了！ » (mot anglais dans l'interface chinoise)
defauts-avant:
  - aucun donné | pas atteint | ecran: - | -
autres-notes:
  - écran 1 : alertes de chevauchement du banc sur les pads (« 踩镲 » sur « 左 30 »)
  - au piano roll, la grille de pas et les notes sont rognées par la carte du tuto ([coupé]) ; la barre 撤销/歌曲 du bas disparaît
  - les notes en pointillé ne se lisent qu'en image : le texte de l'écran ne dit que « dessin »
fin-tranche: non (dernier step=lead_delay, done=19)
