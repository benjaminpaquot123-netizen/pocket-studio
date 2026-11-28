code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: ko | device: tablet8 | font: 200 | mode: simple
mission: tuto industrial_techno chapitres 1-6 | buts: - (le brief n'en donne pas)
promesse: id=vue-3 | texte: 그리고 2박과 4박 앞에 약한 히트 두 개. | ecran: 24 | tenue: non | pourquoi: les deux coups « avant 2 et 4 » sont posés à « 세기 100 % », pas faibles
promesse: id=vue-4 | texte: 시작하기 전에: 킷은 이미 인더스트리얼 테크노 킷이에요. | ecran: 16 | tenue: non | pourquoi: le bouton du kit affiche « 킷: 커스텀 », pas Industrial techno
hesitations:
  - ecran: 65 | cherchais: si l'étape est finie | regarde: « 잘했어요, 성공이에요! » mais en haut « 탭할 점선 코드: 1 여기를 탭 »
  - ecran: 73 | cherchais: s'il reste une note à poser | regarde: « 성공 다음 단계 » et en même temps « 점선 노트가 1개 더 있어요, 뒤쪽에 → 여기를 탭 »
  - ecran: 76 | cherchais: pourquoi pas de succès après la note de la mesure 2 | regarde: seul « 다음 마디에 복사 » proposé, pas écrit dans la consigne
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 0 | vu: l'écran texte 1 listait « Industrial techno 킷 다운로드 (3 MB) » et « 계속 읽기 » que l'image 1 ne montrait pas (texte et image décalés)
defauts:
  - geste: 2 | ecran.vu: 3 | cible: 예시를 듣지 않고 건너뛰었어요. | geste.type: touche
    attendu: l'exemple a été lancé (« 듣기 정지 » affiché) avant « 다음 », pas de reproche | obtenu: l'appli dit que je n'ai pas écouté l'exemple
    symptome: texte-faux
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: bouton « 오픈 하이햇 R 30 » @ 559,408 | geste.type: touche
    attendu: noms des pads lisibles | obtenu: les pastilles « R 30 », « R 45 », « R 50 », « L 30 » recouvrent le début du nom (오픈, 미드, 북 테두리)
    symptome: cache
    gravite: finition | revu: oui: ecran 1 | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: bouton « 킷: 커스텀 » ▾ @ 139,660 | geste.type: touche
    attendu: après le téléchargement, le kit s'appelle Industrial techno comme le dit le tuto | obtenu: « 킷: 커스텀 » alors que le texte dit « 킷은 이미 인더스트리얼 테크노 킷이에요 »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 71 | banc?: non | promesse: vue-4
  - geste: 19 | ecran.vu: 24 | cible: bouton [état « 히트, 세기 100 % »] @ 397,434 | geste.type: touche
    attendu: les deux coups avant 2 et 4 sont « 약한 히트 » (faibles) | obtenu: posés à 100 %, rien ne dit de les baisser
    symptome: incompris
    gravite: finition | revu: oui: ecran 24 | banc?: non | promesse: vue-3
  - geste: 45 | ecran.vu: 46 | cible: texte « 직접 해 보세요: “하이햇” 패드에서 킥 사이의 점선 칸을 탭하세요. » [coupé] @ 450,777 | geste.type: touche
    attendu: une consigne d'une ligne lue en entier | obtenu: coupée « 킥 사이의 ㅈ… », il faut « 계속 읽기 »
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 17 | banc?: non | promesse: -
  - geste: 61 | ecran.vu: 62 | cible: onglet « Acid 303 베이스 라인 » @ 133,266 | geste.type: touche
    attendu: dans mon projet industrial techno, seulement les pistes que j'ai créées | obtenu: une piste « Acid 303 베이스 라인 » que je n'ai jamais faite, sans explication
    symptome: incompris
    gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 64 | ecran.vu: 65 | cible: bouton « 코드 · 4/8마디 ↑ 탭할 점선 코드: 1 여기를 탭 » @ 450,550 | geste.type: touche
    attendu: au succès, plus d'invitation à taper | obtenu: « 성공이에요! » et en même temps « 탭할 점선 코드: 1 여기를 탭 » (même chose écran 73 après #72)
    symptome: incompris
    gravite: finition | revu: oui: ecran 73 | banc?: non | promesse: -
  - geste: 71 | ecran.vu: 72 | cible: bouton + «  트랙 » @ 697,265 | geste.type: touche
    attendu: bouton « + 트랙 » entier | obtenu: coupé au bord droit « + 트→ » quand il y a 3 pistes
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 75 | banc?: non | promesse: -
  - geste: 75 | ecran.vu: 76 | cible: texte « 직접 해 보세요: 2마디 끝의 노트를 쓰세요. » | geste.type: touche
    attendu: poser la note suffit, comme écrit | obtenu: pas de succès, il faut aussi « 다음 마디에 복사 », que la consigne ne dit pas
    symptome: sans-aide
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 74 | vu: 노트를 쓰세요 | attendu: 노트를 놓으세요
  - ecran: 84 | vu: “파트 이름을 “그루브”로 짓기” | attendu: ‘파트 이름을 “그루브”로 짓기’ (guillemets imbriqués)
chapitres: 1 hésitation (« 듣지 않고 건너뛰었어요 » alors que j'écoutais) | 2 compris du premier coup | 3 hésitation (écoute validée seulement après l'exemple) | 4 hésitation (succès + « 탭할 점선 코드: 1 ») | 5 hésitation (copie non écrite) | 6 compris du premier coup — mission finie en 85 gestes sur 150
fin-tranche: non (dernier step=groove_name, done=17)
