code testé : 28f1260707fe98b9322b638b5eb0f4db44c66028
joué par : sous-agent neuf
siege: a | profil: t7-2a | lang: ko | device: phone360 | font: 130 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t7-2a-1 | texte: Toutes les notes à poser sont visibles sans faire défiler sous le panneau | ecran: 39 | tenue: non | pourquoi: basse OK avec flèches (écran 22), mais mélodie 1마디 : 2 notes sur 3 au premier regard, la 3e n'apparaît qu'après un aller-retour du bandeau
promesse: id=t7-2a-2 | texte: L'aide des pads est lisible en entier | ecran: 81 | tenue: non | pourquoi: « …길게 누 » coupé au bord, la 2e rangée de cases n'est pas visible au-dessus du panneau
promesse: id=t7-2a-3 | texte: Aucune consigne ne demande plus de 3 actions à la fois | ecran: 26 | tenue: non | pourquoi: chapitre 6 étape 1 : créer la piste, allumer 코드, toucher les notes pointillées, faire 2 accords (4 actions)
promesse: id=t7-2a-4 | texte: tous les textes vus sont en coréen, aucun coupé | ecran: 3 | tenue: non | pourquoi: puces du glossaire coupées (écrans 3, 13, 31, 59, 76), « Techno 킷 » en anglais (écran 63)
oeil-neuf: q1 | ecran: 81 | image: 81 | pads, puce « 킷: 스튜디오 », bouton jaune du panneau, 챕터, ⌃, onglets, ? : tout a l'air touchable ; la phrase d'aide à côté du kit a l'air d'un simple texte, normal
oeil-neuf: q6 | ecran: 81 | image: 81 | la preuve « 파트: B · 인트로 » est en haut, mais le panneau redemande « “인트로” 파트 만들기 » : je ne sais pas où aller ensuite
oeil-neuf: q9 | ecran: 81 | image: 81 | « …길게 누 » coupé à droite, « 내 프로젝… » tronqué (dernière image prise ; écran final 82 vu en texte)
chapitres:
  - ch5 basse : compris du premier coup (Bravo écran 24)
  - ch6 스탭 : hésitation (accords pointillés cherchés, image nécessaire)
  - ch7 리프 : hésitation (3e note de 1마디 cachée, « 노트 »/« 음 » mélangés)
  - ch8 그루브 : compris du premier coup
  - ch9 파트 이해하기 : hésitation (longueurs de la carte cachées par le panneau)
  - ch10 파트를 만들어 보세요 : bloqué (étape 1/7 ne passe pas)
defauts-avant:
  - T4 | corrigé (écran 22) : notes de basse au-dessus du panneau, sinon bandeau « 더 위 ↑ »
  - N4 | corrigé (écran 22) : toucher « 점선 음이 1개 남았어요, 더 위 ↑ » amène la note à l'écran
  - T15 | pas corrigé (écran 81) : « 패드는 소리를 내고, 칸은 히트를 저장해요. 길게 누 » coupé, cases de la 2e rangée invisibles
  - T8 | pas corrigé (écran 26) : 4 actions dans une consigne « 직접 해 보세요 »
  - T6 | corrigé (écran 59) : « 그루브 » expliqué en mots simples (mais « 빌드업 » non expliqué, écran 70)
  - R19 | corrigé (écran 28) : une flèche « → » au bout de la rangée de pistes signale la suite
hesitations:
  - ecran: 39 | cherchais: la 3e note pointillée de 1마디 | regarde: le bandeau, qui m'a envoyé en 2마디 puis « ← … 앞쪽에 »
  - ecran: 71 | cherchais: les longueurs en 마디 que la consigne dit de lire | regarde: replié le panneau pour les voir
  - ecran: 78 | cherchais: pourquoi l'étape ne passe pas alors que la partie existe | regarde: « 다음 » répond « 이 단계는 아직 안 끝났어요 »
abandon: ecran 82 | parce que le chapitre 10 reste à l'étape 1/7 malgré trois essais (et budget presque fini)
incidents-banc:
  - geste: - | attente: 0 | vu: outils hors human-act.py : Read sur les 2 fichiers de consignes, Read sur mes propres images screen-N-grille.png, grep sur journal.txt de mon siège après « fin » pour les numéros de geste
  - geste: 16-20, 50, 53-55 | attente: 0 | vu: plusieurs gestes enchaînés dans un seul appel bash (dont des « attends »), une commande à la fois quand même
defauts:
  - geste: 75 | ecran.vu: 82 | cible: bouton « “인트로” 파트 만들기 » → @ 270,851 | geste.type: touche
    attendu: la partie créée, l'étape passe à 2/7 | obtenu: « “인트로” 파트를 만들었어요 », « + B · 인트로 » existe, mais toujours 1/7 et « 이 단계는 아직 안 끝났어요 »
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 82 | banc?: non | promesse: -
  - geste: 80 | ecran.vu: 81 | cible: texte « 패드는 소리를 내고, 칸은 히트를 저장해요. 길게 누르기: 사운드 바꾸기. » [coupé] @ 387,649 | geste.type: touche
    attendu: aide du pad lisible en entier | obtenu: coupée à « 길게 누 », 2e rangée de cases cachée par le panneau
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-2a-2
  - geste: 25 | ecran.vu: 26 | cible: consigne « 직접 해 보세요 » du chapitre 6, étape 1 (스탭) | geste.type: touche
    attendu: au plus 3 actions | obtenu: créer la piste, allumer 코드, toucher les notes, faire 2 accords
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t7-2a-3
  - geste: 38 | ecran.vu: 39 | cible: « 직접 해 보세요: “멜로디 트랙 만들기”를 탭하고 점선 노트를 탭하세요. 1마디에 3개, 2마디에 4개. » | geste.type: touche
    attendu: les 3 notes de 1마디 visibles | obtenu: 2 visibles, la 3e seulement après un aller-retour du bandeau
    symptome: cache | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t7-2a-1
  - geste: 2 | ecran.vu: 3 | cible: texte « • 마디: 리듬의 한 바퀴… » [coupé] | geste.type: touche
    attendu: la ligne du glossaire entière | obtenu: coupée en deux au-dessus de « ▸ 왜요? » (aussi écrans 13, 31, 38, 59, 69, 76) ; aussi légende « 빌드업 / 드롭 2 / 끝 » coupée (écran 65) et « 추가: » coupé derrière le panneau (63-82)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 31 | banc?: non | promesse: t7-2a-4
  - geste: 62 | ecran.vu: 63 | cible: bouton « Techno 킷 다운로드 (4 MB) » | geste.type: touche
    attendu: le bouton lisible | obtenu: seul le haut du bouton jaune dépasse, mot caché (aussi écran 74)
    symptome: cache | gravite: finition | revu: oui: ecran 74 | banc?: non | promesse: t7-2a-4
  - geste: 70 | ecran.vu: 71 | cible: « 맵을 보세요 … 마디 단위 길이가 적혀 있어요 » | geste.type: touche
    attendu: les longueurs de la carte visibles | obtenu: cases de longueur cachées par le bas du panneau
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
    attendu: mot entier ou signe de suite | obtenu: coupé au bord droit sans signe
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: t7-2a-4
  - ecran: 63 | vu: Techno 킷 | attendu: 테크노 킷
  - ecran: 76 | vu: ““인트로” 파트 만들기” | attendu: “인트로” 파트 만들기
  - ecran: 39 | vu: 노트 / 음 mélangés | attendu: un seul mot partout
