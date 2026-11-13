siege: b | profil: t6-Db coreenne-debutante-petit-tel | lang: ko | device: phone360 | font: 130 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Db-1 | texte: Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 70 | tenue: pas vue | pourquoi: arrivée au chapitre 5/15 sans impasse, puis le banc a clos le siège (pas de « Bravo » vu)
promesse: id=t6-Db-2 | texte: Chaque étape du tuto se valide quand je fais le geste demandé | ecran: 57 | tenue: non | pourquoi: étape « 내 결과 듣기 » écoutée puis arrêtée, aucun « 성공 », il a fallu « 다음 » ; l'étape claps validée sans le « 2마디로 바꾸기 » annoncé
promesse: id=t6-Db-3 | texte: Le bouton du tuto ne cache aucun contrôle dont j'ai besoin | ecran: 51 | tenue: non | pourquoi: le panneau du tuto couvre la 2e rangée de cases pointillées (퍼크) et les notes pointillées de la basse (écran 60)
promesse: id=t6-Db-4 | texte: chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 43 | tenue: non | pourquoi: « 업비트 » non expliqué (la puce explique « 엇박 »), « 마디 », « 노트 », « 루프 », « 퍼크 » jamais expliqués
promesse: id=t6-Db-5 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 18 | tenue: non | pourquoi: tout en coréen, mais l'aide des pads est coupée par le panneau (« 길게 누… »)
oeil-neuf: q1 | ecran: 70 | image: 70 | clavier, grille et pastilles de pistes touchables ; rien n'indique combien de notes pointillées restent (le compteur est parti en haut)
oeil-neuf: q6 | ecran: 70 | image: 70 | preuve = notes orange dans la grille ; le compteur « 탭할 점선 노트가 N개 남았어요 » n'est plus visible après défilement
oeil-neuf: q9 | ecran: 70 | image: 70 | pastille « + 트랙 » coupée au bord droit, libellé « 마디 » coupé en haut de la grille
hesitations:
  - ecran: 49 | cherchais: le pad « 퍼크 » demandé | regarde: aucun pad ne porte ce nom, j'ai tapé les pointillés au hasard
  - ecran: 60 | cherchais: les « 점선 노트 » à taper | regarde: grille vide au-dessus du panneau, il a fallu faire défiler 3 fois
  - ecran: 37 | cherchais: le bouton « 2마디로 바꾸기 » | regarde: il n'est jamais apparu, une seule rangée de 16 cases
abandon: ecran 70 | parce que le banc a clos le siège (« rien ne change ») alors que les notes s'ajoutaient à l'image
incidents-banc:
  - geste: 26 | attente: 0 | vu: écrans 26, 27, 32, 33, 38, 39, 44, 45 (anneau du tuto) rendus en texte vide, seule l'image montrait la grille
  - geste: 70 | attente: 0 | vu: siège clos « 5 écrans identiques » : les notes ajoutées au piano-roll n'apparaissent pas dans le texte du banc
defauts:
  - geste: 18 | ecran.vu: 19 | cible: 시작하기 전에: 킷은 이미 테크노 킷(909 드럼 머신)이고, 이펙트도 들어 있어요. 패드에 있는 “FX”는 이펙트가 이미 걸려 있다는 뜻이에요. 설정할 건 없어요. | geste.type: touche
    attendu: le kit affiché est Techno, rien à régler | obtenu: l'écran montre « 킷: 스튜디오 » ; le téléchargement du kit Techno est caché sous « 계속 읽기 »
    symptome: incompris | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t6-Db-4
  - geste: 31 | ecran.vu: 36 | cible: 직접 해 보세요: “낮은 울림” 패드(으르렁거림)에서 킥과 같은 칸을 탭하세요. | geste.type: touche
    attendu: la grille du pad « 낮은 울림 » | obtenu: la grille ouverte s'appelle « Techno · 스네어 2 » (après le kit Techno, plus de pad « 낮은 울림 »)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Db-4
  - geste: 37 | ecran.vu: 38 | cible: “2마디로 바꾸기”가 나오면 먼저 탭하세요. | geste.type: touche
    attendu: le bouton « 2마디로 바꾸기 » et une 2e mesure, un « 약한 히트» | obtenu: jamais de bouton, une seule mesure, dernier coup à 100 %, étape validée quand même
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Db-2
  - geste: 42 | ecran.vu: 43 | cible: 직접 해 보세요: “Roland TR-909 · 오픈 하이햇” 패드(더 긴 “츠으”)에서 점선 칸을 탭하세요. 킥 바로 사이, 업비트에요. | geste.type: touche
    attendu: un mot expliqué et le nom du pad visible | obtenu: « 업비트 » non expliqué (la puce dit « 엇박 ») ; les pads s'appellent « Techno · … », pas « Roland TR-909 »
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Db-4
  - geste: 48 | ecran.vu: 51 | cible: 직접 해 보세요: “퍼크” 패드(작은 “똑”)에서 점선 칸을 탭하세요. | geste.type: touche
    attendu: toutes les cases pointillées visibles | obtenu: le panneau du tuto couvre la 2e rangée (2 cases), il faut le replier avec ⌄
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Db-3
  - geste: 59 | ecran.vu: 60 | cible: 베이스 · 1/8마디 탭할 점선 노트가 8개 남았어요 | geste.type: touche
    attendu: les 8 notes pointillées visibles au-dessus du panneau | obtenu: grille vide, notes sous le panneau ; « 직접 할게요 » n'aide pas, 3 défilements avant de les voir
    symptome: cache | gravite: contourne-en-5-gestes | revu: oui: ecran 63 | banc?: non | promesse: t6-Db-3
  - geste: 55 | ecran.vu: 57 | cible: 내 드럼 전체를 몇 초 들어 보고, 예시의 드럼도 들어 보세요. | geste.type: touche
    attendu: « 성공 » après avoir écouté mon résultat | obtenu: rien ne se valide, j'ai dû deviner « 다음 »
    symptome: rien | gravite: contourne-en-1-gestes | revu: non | banc?: temps | promesse: t6-Db-2
  - geste: 17 | ecran.vu: 18 | cible: 패드는 소리를 내고, 칸은 히트를 저장해요. 길게 누르기: 사운드 바꾸기. | geste.type: touche
    attendu: l'aide des pads lisible en entier | obtenu: coupée à « 길게 누 » par le panneau du tuto (police 130 %)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Db-5
  - geste: 58 | ecran.vu: 59 | cible: 직접 해 보세요: “베이스 트랙 만들기”(트랙 = 곡의 악기 하나)를 탭하고, 1마디의 점선 노트를 탭하세요. | geste.type: touche
    attendu: une consigne courte, mots expliqués | obtenu: 5 actions dans une phrase, « 마디 », « 노트 », « 복사 » vers 2-3-4마디 non expliqués
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Db-4
mots-faux:
  - ecran: 49 | vu: 퍼크 | attendu: 퍼커션 (ou le nom réel du pad)
  - ecran: 43 | vu: 업비트 | attendu: 엇박 (le mot expliqué juste en dessous)
