code testé : f1b9fdb1fdb38b1824bbf4bb43318cc6f9291277
joué par : sous-agent neuf

siege: c | profil: debutante-coreenne-petit-ecran | lang: ko | device: phone360 | font: 130 | mode: simple
mission: tuto-facile | buts: finir-chapitre-9, chapitre-10-etape-2, -
promesse: id=ch10-etape1 | texte: 직접 해 보세요: ““인트로” 파트 만들기”를 탭하세요. | ecran: 29 | tenue: non | pourquoi: aucun bouton « 인트로 » sur 내 곡 ; « + 새 파트 (A 복사) » crée B et l'étape reste 1/7
promesse: id=ch9-carte | texte: 맵을 보세요: 칸마다 파트 하나이고, 마디 단위 길이가 적혀 있어요. | ecran: 20 | tenue: non | pourquoi: aucune carte à l'écran, le panneau du tuto couvre tout
oeil-neuf: q1 | ecran: 43 | image: 43 | + A / + 새 파트 / + 빈 새 파트 se touchent, mais rien ne ressemble au « 인트로 파트 만들기 » demandé
oeil-neuf: q6 | ecran: 43 | image: 43 | après « 직접 할게요 » je suis sur 내 곡 sans savoir quoi toucher ; pas de preuve d'étape réussie
oeil-neuf: q9 | ecran: 43 | image: 43 | consigne du tuto coupée en « … » (panneau replié), guillemets doublés « ““인트로” »
hesitations:
  - ecran: 1 | cherchais: le chapitre 9 promis | regarde: l'appli ouvre le chapitre 1/15, il faut passer par 챕터 et faire défiler la liste
  - ecran: 24 | cherchais: le bouton « “인트로” 파트 만들기 » | regarde: seulement + A, + 새 파트 (A 복사), + 빈 새 파트 ; ni sur 리듬 ni dans 파트: A ▾
  - ecran: 39 | cherchais: un menu dans 더보기 | regarde: rien ne s'ouvre, l'onglet 리듬 perd juste sa sélection
abandon: jamais (étape 2/7 atteinte en sautant l'étape 1 par « 다음 » deux fois)
incidents-banc:
  - geste: 23 | attente: 2 | vu: écran presque vide juste après « 직접 할게요 », revenu au geste suivant (idem geste 41)
defauts:
  - geste: 22 | ecran.vu: 29 | cible: bouton (doigt qui touche) «   직접 할게요 » | geste.type: touche
    attendu: un bouton « “인트로” 파트 만들기 » sur 내 곡, puis 성공 et étape 2/7 | obtenu: aucun bouton de ce nom ; « + 새 파트 (A 복사) » (geste 30) crée B, étape reste 1/7
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 42 | banc?: non | promesse: ch10-etape1
  - geste: 19 | ecran.vu: 20 | cible: texte « 맵을 보세요: 칸마다 파트 하나이고, 마디 단위 길이가 적혀 있어요. 곡선은 에너지를 보여 줘요. » | geste.type: touche
    attendu: la carte des parties visible à côté de la consigne | obtenu: le panneau du tuto couvre tout l'écran, aucune carte
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: ch9-carte
  - geste: 36 | ecran.vu: 37 | cible: icône ⌃ @ 483,738 | geste.type: touche
    attendu: pads, aide des pads et 2e rangée de la grille lisibles avec le panneau ouvert | obtenu: le panneau ouvert couvre les pads et toute la grille (y 170 à 910)
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 20 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 1 | cible: texte « 곡의 빠르기와 색깔 1/15장 · 1/3단계 · 쉬움 » | geste.type: touche
    attendu: arriver au chapitre 9 comme annoncé | obtenu: chapitre 1/15 affiché, 3 gestes pour atteindre le 9
    symptome: mauvais-ecran | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 39 | cible: icône ⋮ « 더보기 » @ 479,950 | geste.type: touche
    attendu: un menu s'ouvre | obtenu: rien ne s'ouvre, l'onglet 리듬 n'est plus sélectionné
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 22 | vu: ““인트로” 파트 만들기” | attendu: “인트로” 파트 만들기 (guillemets doublés)
  - ecran: 1 | vu: 큰북 (pour la grosse caisse/kick) | attendu: 킥 (le tuto dit 킥 partout, le pad dit 큰북)
defauts-avant:
  - D2 | encore là | ecran: 29 | sur 내 곡, étape 1/7, aucun bouton « “인트로” 파트 만들기 » ; seul « 다음 » ×2 fait passer à 2/7, sans 성공 ; même chose à l'étape 2 (écran 42)
  - D1 | encore là | ecran: 37 | panneau ouvert = pads et grille entièrement cachés ; replié, l'aide est coupée (écran 32) et il faut faire défiler pour la 2e rangée (écran 36)
