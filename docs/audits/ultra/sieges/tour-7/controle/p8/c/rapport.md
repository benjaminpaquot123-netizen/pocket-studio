code testé : ede109d8ada92d293e0cfd3b777f53b38e2e3b68
joué par : sous-agent neuf

siege: c | profil: adulte-coreen | lang: ko | device: 360dp | font: 130 | mode: simple
mission: tuto-facile ch9-12 | buts: ch10, ch11, ch12
promesse: id=texte-coupe | texte: 끝의 한 ㅂ… / 계속 읽기 | ecran: 28 | tenue: oui | pourquoi: les longues consignes finissent par « … » et 계속 읽기 ouvre tout (écrans 1, 12, 28), mais à 28 le « … » recouvre une syllabe à moitié dessinée
promesse: id=roulement | texte: 2번 반복 (×2 sur les 12 claps de la mesure 4) | ecran: 47 | tenue: non | pourquoi: le roulement du Build-up est déjà là à la création et aucune étape ne le demande ; seul le silence du dernier temps est bien demandé au ch12
promesse: id=ch10-boutons | texte: 보여 주세요 / 잘했어요, 성공이에요! | ecran: 16-34 | tenue: oui | pourquoi: chaque étape du ch10 a un bouton d'action et 보여 주세요, et chaque étape se coche
promesse: id=ch12-crash | texte: 여기 점선 칸이 1개 남았어요 / 잘했어요, 챕터 완료! | ecran: 53-54 | tenue: oui | pourquoi: dans 드롭, la première case du pad 심벌 est vide et en pointillé ; la toucher termine le chapitre
oeil-neuf: q1 | ecran: 54 | image: 54 | 16 cases, 채우기/전체/1/2/1/4/◀▶, 지우기, onglets, 챕터 et ⌃ ont l'air touchables et le sont ; « 인트로 1/128마디 ▾ » ressemble à un titre mais c'est un menu
oeil-neuf: q6 | ecran: 54 | image: 54 | preuve : la case 1 est bleue et la carte verte dit « 잘했어요, 챕터 완료! », mais rien n'indique le chapitre suivant (il faut deviner « 챕터 »)
oeil-neuf: q9 | ecran: 54 | image: 54 | le nom du pad « 심벌 » est caché sous le bandeau du haut, l'aide à 600 est coupée par la carte du tuto, et le haut affiche « 인트로 » alors qu'on est dans « D · 드롭 »
hesitations:
  - ecran: 53 | cherchais: la rangée du pad « 심벌 » demandée par la consigne | regarde: aucun nom de pad visible, seulement la case en pointillé
  - ecran: 33 | cherchais: le bouton « 엔딩 » nommé par la consigne | regarde: le bouton dit « “끝” 파트 만들기 »
  - ecran: 47 | cherchais: quel est le roulement que j'aurais dû faire | regarde: les « ×2 » y étaient déjà
abandon: jamais
incidents-banc:
  - geste: 18 | attente: 0 | vu: plafond d'images atteint ; j'ai continué avec le texte
  - geste: 0 | attente: 0 | vu: outil hors human-act.py : Read, seulement pour la consigne testeur et les images PNG du siège
defauts:
  - geste: 30 | ecran.vu: 47 | cible: 2번 반복 | geste.type: touche
    attendu: Build-up créé sans roulement ; une étape suivante le demande | obtenu: les 12 claps sont déjà en ×2 et aucune étape ne demande le roulement
    symptome: texte-faux | gravite: finition | revu: oui: ecran 47 | banc?: non | promesse: roulement
  - geste: 30 | ecran.vu: 30 | cible: “손뼉” 패드가 마디마다 빨라지고, 마지막 박은 조용해져요. | geste.type: touche
    attendu: le message ne dit pas que le silence est déjà fait (il est demandé au ch12) | obtenu: il dit que le dernier temps est déjà silencieux
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 33 | cible: “끝” 파트 만들기 | geste.type: touche
    attendu: la consigne et le bouton disent le même nom | obtenu: la consigne dit « 엔딩 », le bouton et la partie disent « 끝 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: 끝의 한 ㅂ… | geste.type: touche
    attendu: la coupure tombe avant une syllabe entière, puis « … » | obtenu: le « … » est dessiné sur la syllabe « 박 » à moitié visible
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 29 | banc?: non | promesse: texte-coupe
  - geste: 53 | ecran.vu: 53 | cible: 여기 점선 칸이 1개 남았어요 | geste.type: touche
    attendu: le nom du pad « 심벌 » est visible au-dessus de sa rangée | obtenu: « 심벌 » est caché sous le bandeau [coupé], donc rien ne relie la case au pad demandé
    symptome: cache | gravite: finition | revu: oui: ecran 54 | banc?: non | promesse: -
  - geste: 53 | ecran.vu: 54 | cible: 인트로 1/128마디 | geste.type: touche
    attendu: le haut de l'écran dit la partie ouverte (드롭) | obtenu: il dit « 인트로 » pendant que l'éditeur montre « 파트: D · 드롭 »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 54 | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 47 | cible: 여기 4번 더 쳤어요 | geste.type: touche
    attendu: une phrase coréenne naturelle, et des numéros de temps lisibles | obtenu: tournure peu naturelle, et les numéros 1/2/3 passent sur les « ×2 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 25 | vu: 손뼉 | attendu: 클랩 (le mot du ch9)
  - ecran: 25 | vu: 스튜디오 큰북 | attendu: 킥 (le mot du ch9)
  - ecran: 25 | vu: 낮은 울림 | attendu: 베이스 / 저음 (sens peu clair)
  - ecran: 16 | vu: 를 (seul en début de ligne après la citation) | attendu: particule gardée sur la même ligne
  - ecran: 36 | vu: 엔딩으로 끝나는 버튼을 | attendu: 끝 파트 버튼을 (tournure lourde, et la partie s'appelle 끝)
defauts-avant:
  - c-V3 | encore là | ecran: 47 | le roulement ×2 du Build-up est présent dès la création et aucune étape ne le demande (seul le silence est demandé au ch12)
  - c-V4 | corrigé | ecran: 28 | la consigne longue sous le bandeau finit par « … » et 계속 읽기 l'ouvre ; reste un « … » posé sur une syllabe à moitié dessinée (finition)
