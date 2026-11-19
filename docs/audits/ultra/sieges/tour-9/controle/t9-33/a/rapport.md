code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: confirmé | lang: ja | device: phone360 | font: 100 | mode: pro
mission: tuto-pro (tuto house, chapitres 1 à 9) | buts: -
promesse: id=- | texte: 2小節目のステップ 15 には弱いヒット | ecran: 28 | tenue: non | pourquoi: une frappe à 100 % valide l'étape, la force n'est pas vérifiée
hesitations:
  - ecran: 37 | cherchais: le sélecteur 小節 1/2 pour aller à la mesure 2 | regarde: caché sous « パート：A », il a fallu glisser vers le haut
  - ecran: 107 | cherchais: où mettre la ポンプ sur ベース dans ミックス | regarde: le nom de piste ne réagit pas, passé par « 自分でやる »
  - ecran: 131 | cherchais: comment écrire le D9 des mesures 3-4 | regarde: « 次の小節にコピー » ne copie pas, il propose des accords pointillés à toucher
abandon: jamais
incidents-banc:
  - geste: 27-172 | attente: 0 | vu: j'ai lu journal.txt du siège pour comprendre des touches refusées (pas d'autre outil hors human-act.py)
  - geste: 167 | attente: 0 | vu: carte du tuto déjà dépliée, ma touche sur ⌃ a pris « 見せて » (positions qui bougent)
defauts:
  - geste: 27 | ecran.vu: 28 | cible: 2小節目のステップ 15 には弱いヒット | geste.type: touche
    attendu: l'étape attend une frappe faible | obtenu: frappe à 100 % acceptée « 完了 » ; idem ch. 4 (拍の上は強く、まわりは弱く), écran 67
    symptome: incompris | gravite: finition | revu: oui: ecran 67 | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 37 | cible: パート：A | geste.type: touche
    attendu: voir « 小節 1/2 · クラップ » et le sélecteur de mesure | obtenu: caché sous « パート：A », il faut glisser
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 48 | banc?: non | promesse: -
  - geste: 73 | ecran.vu: 73 | cible: スウィングを 33% にしよう…シカゴ・ハウス（55〜60 %）とニューヨーク・ハウス（70〜80 %）の間 | geste.type: touche
    attendu: une valeur cohérente avec la phrase | obtenu: 33 % n'est pas entre 55 et 80 %, incohérent pour un pro
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 107 | ecran.vu: 108 | cible: トラック名をタップでエフェクトとカーブ | geste.type: touche
    attendu: toucher « ベース » ouvre ses effets | obtenu: rien ; le ⋮ de ベース dans サウンド n'a pas d'effets ; seul « 自分でやる » ouvre « エフェクト · ベース »
    symptome: rien | gravite: contourne-en-6-gestes | revu: oui: ecran 110 | banc?: non | promesse: -
  - geste: 105 | ecran.vu: 105 | cible: 自分でやる | geste.type: touche
    attendu: la feuille d'effets vide, je pose la ポンプ moi-même | obtenu: « 1. ポンプ » déjà ajoutée par l'appli (Sub, puis Bass écran 116)
    symptome: incompris | gravite: finition | revu: oui: ecran 116 | banc?: non | promesse: -
  - geste: 152 | ecran.vu: 153 | cible: タップする点線のノートがあと 2 個 | geste.type: touche
    attendu: le compteur reste à 10 après « ここをタップ » | obtenu: il passe de « あと 10 個 » à « あと 2 個 » sans note posée, puis « あと 8 個 » mesure 2
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 172 | ecran.vu: 173 | cible: エコー センド | geste.type: glisse
    attendu: voir la carte du tuto confirmer l'étape | obtenu: la feuille d'effets couvre la carte, aucun retour visible (écho monté à 41 %)
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 84 | ecran.vu: 84 | cible: ここをタップ | geste.type: touche
    attendu: voir les notes de l'octave dans le piano roll de la basse | obtenu: rouleau très petit, notes hors vue, 2 touches (#84, #85) sans effet avant « ここをタップ »
    symptome: cache | gravite: finition | revu: oui: ecran 153 | banc?: non | promesse: -
mots-faux:
  - ecran: 57 | vu: ここに4回多い（取り消し線） | attendu: ここに4個多い
chapitres:
  - ch1 tempo et tonalité : compris du premier coup
  - ch2 kick : compris du premier coup
  - ch3 clap : hésitation (sélecteur de mesure caché)
  - ch4 hats, rim, swing : hésitation (mesure cachée, texte du swing)
  - ch5 basse : hésitation (rouleau petit, comportement de la copie)
  - ch6 sub et pompe : hésitation (nom de piste mort dans ミックス)
  - ch7 stabs de piano : hésitation (« 次の小節にコピー » ne copie pas)
  - ch8 arpège : étape 1 faite, étape 2 non confirmée
  - ch9 : non atteint
budget épuisé au chapitre 8
defauts-avant:
  - aucun défaut ciblé donné par le brief

fin-tranche: non (dernier step=lead_delay, done=20)
