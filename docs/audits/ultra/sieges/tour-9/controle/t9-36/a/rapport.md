code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: ko | device: phone | font: 100 | mode: simple
mission: tuto-facile (industrial_techno, chapitres 7-13) | buts: - (brief : finir les chapitres 7 à 13)
promesse: id=- | texte: 지금 상태의 곡을 들어 보고 다음 챕터로 넘어가세요 | ecran: 12 | tenue: oui | pourquoi: chaque fin de chapitre (7 à 12) mène bien au chapitre suivant par « 다음 챕터 »
promesse: id=- | texte: 내 차례: ““드롭 2” 파트로 가기”를 누르세요. | ecran: 44 | tenue: non | pourquoi: le premier toucher change de partie mais le tuto ne réagit pas ; il faut toucher une 2e fois
chapitres:
  - ch7 파트 이해하기 : hésitation (la carte ne nomme pas « 브레이크 » ni « 드롭 B », légende cachée)
  - ch8 파트를 만들어 보세요 : compris du premier coup
  - ch9 파트를 정리하세요 : compris du premier coup
  - ch10 트랜지션 : hésitation (bouton « 드롭 2 파트로 가기 » à toucher deux fois)
  - ch11 이펙트 : compris du premier coup
  - ch12 믹스: 볼륨 : compris du premier coup
  - ch13 곡을 내보내세요 : compris du premier coup (export M4A, écran « 준비됐어요 »)
hesitations:
  - ecran: 8 | cherchais: où est la « 브레이크 » sur la carte | regarde: les cases 16 / 8 / 8 / 16 du milieu n'ont pas de nom, la légende est coupée derrière « … »
  - ecran: 44 | cherchais: la consigne suivante après avoir touché « 드롭 2 파트로 가기 » | regarde: la partie a changé mais le tuto répète la même consigne
  - ecran: 63 | cherchais: si j'ai fini le tuto | regarde: « 4단계 남았어요 » renvoie aux chapitres 1, 2, 3, 6 (écoutes des chapitres préparés par l'amenée, hors mission)
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun outil hors human-act.py ; chapitres 1-6 construits par l'amenée, d'où « 4단계 남았어요 » à l'écran 63
defauts:
  - geste: 43 | ecran.vu: 44 | cible: bouton ⊕ «   “드롭 2” 파트로 가기 » | geste.type: touche
    attendu: la partie « 드롭 2 » s'ouvre et le tuto passe à la consigne « 심벌 패드의 맨 첫 칸을 탭하세요 » (comme au chapitre 10 étape 1) | obtenu: la partie change, mais la carte du tuto reste sur « 내 차례: … 파트로 가기를 누르세요 » ; il faut retoucher le même bouton (geste 44)
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: bouton ⊕ «   “Drop B” 파트 만들기 » | geste.type: touche
    attendu: nom de partie en coréen « 드롭 B », comme dans la consigne du chapitre 7 et 9 | obtenu: « Drop B » en anglais sur le bouton, la partie et la légende de la carte
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 29 | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 15 | cible: texte « 타임라인은 파트를 순서대로 놓아요. 각각 마디 단위 길이가 있어요. 순서대로 추가하세요. » [coupé] | geste.type: touche
    attendu: lire l'explication de la ligne du temps au-dessus des boutons « 추가 » | obtenu: le texte est entièrement caché sous la barre du haut (invisible sur l'image)
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: texte « 내 차례: ““인트로” 파트 만들기”를 누르세요. » | geste.type: touche
    attendu: un seul jeu de guillemets autour du nom du bouton | obtenu: guillemets doublés « ““인트로” 파트 만들기” », pareil à chaque étape des chapitres 8 et 10
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: texte « 브레이크를 들어 보세요: 킥 없이 으르렁거림과 드론이 남아요. » | geste.type: touche
    attendu: voir sur la carte où est la « 브레이크 » (et au pas 5 le « 드롭 B ») | obtenu: la carte n'écrit que 인트로 / 드롭 / 드롭 2 / 끝 ; les cases du milieu n'ont qu'un chiffre, la légende est coupée
    symptome: incompris
    gravite: finition | revu: oui: ecran 11 | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 30 | cible: texte « 끝에 추가: » | geste.type: touche
    attendu: la consigne dit « “추가” 아래에서 … 탭하세요 » et le titre reste « 추가: » | obtenu: après le 1er ajout le titre devient « 끝에 추가: » et tous les boutons descendent d'environ 125 points sous le doigt
    symptome: mauvais-ecran
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 20 | vu: Drop B | attendu: 드롭 B
defauts-avant:

fin-tranche: non (dernier step=share, done=32)
