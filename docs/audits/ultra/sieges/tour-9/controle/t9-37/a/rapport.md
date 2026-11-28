code testé : ab7fbf4e8
joué par : sous-agent neuf

siege: a | profil: pro | lang: zh-CN | device: phone360 | font: 130 | mode: pro
mission: tuto-pro (jersey_club ch.1-7) | buts: -
chapitres:
  - ch.1 速度和调性 : hésitation (texte d'intro coupé, il faut « 继续阅读 » ; message « 未收听 » faux)
  - ch.2 五下底鼓 : hésitation (consigne cachée sous « 继续阅读 », nom du pad différent de la consigne)
  - ch.3 拍手 : compris du premier coup
  - ch.4 踩镲和打击乐 : compris du premier coup
  - ch.5 808 : hésitation (compteur de notes incohérent, nom de piste « 808 » intouchable dans le mixage)
  - ch.6 人声片段 : hésitation (compteur ↑/↓ qui se contredit)
  - ch.7 你的第一段：副歌 : hésitation (je n'ai trouvé nulle part où renommer la section moi-même)
hesitations:
  - ecran: 72 | cherchais: la note restante annoncée | regarde: « ↑ 还有 4 个 » devenu « ↓ 还有 1 个 » sans que j'aie posé de note
  - ecran: 84 | cherchais: les effets de la piste 808 (la bulle dit « 点音轨名称 ») | regarde: deux touchers sur « 808 » sans effet
  - ecran: 116 | cherchais: renommer la section A en « 副歌 » | regarde: menu « A ▾ », appui long, onglet 结构 : aucun « 重命名 »
abandon: jamais
incidents-banc: aucun
defauts:
  - geste: 83 | ecran.vu: 84 | cible: bouton « 808 808 » | geste.type: touche
    attendu: le nom de piste ouvre ses effets, comme l'annonce « 点音轨名称查看它的效果和曲线。 » | obtenu: rien, un autre élément (« 鼓 ») recouvre le nom ; j'ai dû passer par « 我来做 », qui a posé « 抽吸 » à ma place
    symptome: cache
    gravite: contourne-en-3-gestes | revu: oui: ecran 85 | banc?: non | promesse: -
  - geste: 115 | ecran.vu: 119 | cible: 轮到你：把这个段落命名为“副歌”：它是你的起点段落，所有东西都在里面播放。 | geste.type: touche
    attendu: un endroit pour renommer la section moi-même (menu A ▾ ou onglet 结构) | obtenu: menu « 要编辑的段落 » sans renommer, appui long sans effet, 结构 sans renommer ; seul le bouton du tuto le fait
    symptome: sans-aide
    gravite: contourne-en-5-gestes | revu: oui: ecran 119 | banc?: non | promesse: -
  - geste: 71 | ecran.vu: 72 | cible: bouton « 808 · 第 1/8 小节 ↑ 还有 4 个虚线音符要点 » | geste.type: touche
    attendu: le guide m'amène aux notes, le compte reste 4 | obtenu: « ↓ 还有 1 个 » sans note posée ; au ch.6 « 还剩 4 个…在更上面 ↑ » devient « ↓ 还有 1 个 » (écran 102)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 102 | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: 未收听，已跳过示例。 | geste.type: touche
    attendu: le tuto voit que j'ai écouté l'exemple (il jouait encore 7 s) | obtenu: « 未收听，已跳过示例。 », revu aux écrans 9, 17, 26, 69
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 69 | banc?: son | promesse: -
  - geste: 17 | ecran.vu: 18 | cible: 轮到你：在“录音棚底鼓”打击垫上 | geste.type: touche
    attendu: un pad « 录音棚底鼓 » comme dit la consigne | obtenu: le pad s'appelle « Jersey club · 底鼓 »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 24 | banc?: non | promesse: -
  - geste: 32 | ecran.vu: 33 | cible: 第 11 步 | geste.type: touche
    attendu: la consigne demande un coup « 轻 », le tuto vérifie la vélocité | obtenu: accepté à 100 % sans régler ; idem pour la syllabe légère du ch.6
    symptome: incompris
    gravite: finition | revu: oui: ecran 107 | banc?: non | promesse: -
  - geste: 78 | ecran.vu: 79 | cible: bouton « 复制 » | geste.type: touche
    attendu: le pattern est copié | obtenu: aucune réaction visible au ch.5 ; seul « 复制到后面的小节 » a marché (au ch.6 le même « 复制 » a marché, écran 108)
    symptome: rien
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 34 | ecran.vu: 34 | cible: 小节 1 2 跟随 | geste.type: touche
    attendu: la rangée des mesures entière | obtenu: à moitié cachée sous l'en-tête ; « 第 1/2 小节 · Jersey club · 拍手 » coupé en « … »
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 92 | ecran.vu: 92 | cible: bouton « 幽灵贝斯 » [sélectionné] | geste.type: touche
    attendu: une piste de chant/voix, réglage voix | obtenu: la piste mélodie s'ouvre sur « 幽灵贝斯 » (ghost bass) sélectionné
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 107 | banc?: non | promesse: -
  - geste: 78 | ecran.vu: 79 | cible: 808 · 第 1/8 小节 | geste.type: touche
    attendu: un piano roll lisible pour poser F2/F3 | obtenu: la bande de notes fait ~70 px, le reste est caché par le panneau du tuto
    symptome: cache
    gravite: finition | revu: oui: ecran 104 | banc?: non | promesse: -
mots-faux:
  - ecran: 18 | vu: 录音棚底鼓 | attendu: Jersey club · 底鼓

fin-tranche: non (dernier step=groove_name, done=16)
