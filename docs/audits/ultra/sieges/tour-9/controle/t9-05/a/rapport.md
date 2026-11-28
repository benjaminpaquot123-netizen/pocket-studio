code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: ko | device: phone360 | font: 200 | mode: pro
mission: tuto-facile (acidcore, chapitres 1 à 5) | buts: -
promesse: id=- | texte: “보여 주세요”를 탭하면 앱이 “Acid 303” 트랙에 대신 써 줘요. 그다음 “내 결과 듣기”로 들어 보세요. | ecran: 47 | tenue: non | pourquoi: 5 essais (simple et pro, onglets 리듬/건반/구성/사운드), l'étape ne se valide jamais et « 내 결과 듣기 » n'apparaît pas
promesse: id=- | texte: 맵을 보세요… 곡선은 에너지를 보여 줘요 | ecran: 81 | tenue: oui | pourquoi: la carte n'apparaît qu'après « 계속 읽기 » (écran 85)
chapitres:
  - ch1 곡의 빠르기와 색깔 : hésitation (« 계속 읽기 » sans effet à l'étape 1, bouton de téléchargement caché par le texte)
  - ch2 드럼 : compris du premier coup
  - ch3 애시드 라인 (303) : bloqué à l'étape 2/2 (passé avec 다음, la liste des chapitres affiche 1/2)
  - ch4 첫 번째 파트: 그루브 : compris du premier coup
  - ch5 파트 이해하기 : hésitation (la carte annoncée était cachée)
hesitations:
  - ecran: 47 | cherchais: le « 프로듀서 화면 » dont parle le tuto | regarde: les onglets 리듬/건반, le « ? » du 건반, la feuille des modes, le mode 프로 : ce nom n'existe nulle part
  - ecran: 1 | cherchais: la suite du texte coupé de l'étape 1 | regarde: « 계속 읽기 » touché 2 fois, rien ne s'ouvre
  - ecran: 81 | cherchais: la carte et la courbe d'énergie | regarde: seulement « 에너지 » et « … » ; j'ai dû ouvrir « 계속 읽기 »
abandon: ecran 103 | parce que le chapitre 3 ne se valide pas malgré 5 « 보여 주세요 » ; chapitres 1, 2, 4, 5 finis
incidents-banc:
defauts:
  - geste: #46 | ecran.vu: 47 | cible: bouton (œil) «  보여 주세요 » @ 270,701 | geste.type: touche
    attendu: l'appli écrit la ligne acide sur « Acid 303 », l'étape se valide, « 내 결과 듣기 » apparaît | obtenu: la démo montre la grille de la batterie (손뼉) ou rien, dit « 원래대로 돌려놓는 중… » puis « 이제 직접 해 보세요 » ; l'étape reste 2/2, pas de « 내 결과 듣기 »
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 102 (gestes #48, #55, #92 en pro, #101 sur l'onglet Acid 303) | banc?: non | promesse: -
  - geste: #46 | ecran.vu: 47 | cible: texte « 애시드 라인은 프로듀서 화면에서 한 칸씩 써요. » | geste.type: touche
    attendu: savoir où est le « 프로듀서 화면 » pour écrire moi-même | obtenu: aucun écran, onglet ni aide ne porte ce nom (ni en simple ni en pro) ; je ne peux pas le faire à la main non plus
    symptome: incompris | gravite: mission-impossible | revu: oui: ecran 93 | banc?: non | promesse: -
  - geste: #6 | ecran.vu: 7 | cible: texte « 예시를 듣지 않고 건너뛰었어요. » @ 270,297 | geste.type: touche
    attendu: pas de reproche, j'avais lancé « 예시 듣기 » et il jouait encore (7 s restantes) | obtenu: on me dit que j'ai sauté sans écouter
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #1 | ecran.vu: 2 | cible: icône « 계속 읽기 » | geste.type: touche
    attendu: lire la suite du texte coupé de l'étape 1 | obtenu: rien ne s'ouvre (2 essais, #1 et #2) ; à l'étape 3 le même bouton marche
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 3 | banc?: non | promesse: -
  - geste: #3 | ecran.vu: 1 | cible: bouton « Acid 425 킷 다운로드 (4 MB) » @ 219,454 | geste.type: touche
    attendu: voir le bouton de téléchargement du kit | obtenu: sur l'image le bouton est couvert par le texte coupé ; je l'ai touché à l'aveugle et ça a marché
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #16 | ecran.vu: 17 | cible: bouton « 튜토리얼로 돌아가기 » [coupé] @ 270,1149 | geste.type: touche
    attendu: un bouton de retour bien visible | obtenu: coupé tout en bas de l'écran, au ras du bord (il marche quand même)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #84 | ecran.vu: 81 | cible: texte « 에너지 » | geste.type: touche
    attendu: voir la carte et la courbe que le tuto me dit de regarder | obtenu: la carte est cachée sous le panneau ; elle apparaît seulement après « 계속 읽기 », et panneau replié la frise est vide
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: #21 | ecran.vu: 22 | cible: texte « 여기 점선 칸이 4개 남았어요 » [coupé] @ 274,294 | geste.type: touche
    attendu: lire l'aide en entier | obtenu: la phrase d'aide est coupée à la police 200 %
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #52 | ecran.vu: 54 | cible: bouton [coupé] @ 53,525 | geste.type: touche
    attendu: un clavier entier pour jouer | obtenu: le clavier est coupé à la police 200 %, et son « ? » ne parle que d'enregistrer une mélodie
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 21 | vu: 손뼉 FX (nom du pad) | attendu: 손뼉 (mot du tuto)
  - ecran: 22 | vu: Roland TR-909 · 하이햇 L 30 | attendu: 하이햇 (noms anglais et chiffres mélangés pour un débutant)
defauts-avant:

fin-tranche: non (dernier step=acid_show, done=16)
