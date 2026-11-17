code testé : 11ca024b94945ffae964263f27e8342aa3770316
joué par : sous-agent neuf
siege: c | profil: adulte-coreen | lang: ko | device: etroit-360dp | font: 130 | mode: simple
mission: tuto-facile ch9-12 | buts: ch10, ch11, ch12
promesse: id=texte-coupe | texte: 끝의 한 박 … / 계속 읽기 | ecran: 21 | tenue: non | pourquoi: le « … » recouvre la moitié droite de « 박 » (image 21), et « 계속 읽기 » ne fait que défiler une petite fenêtre : la 1re ligne part en haut, jamais le texte entier d'un coup (écrans 8, 22)
promesse: id=roulement | texte: “손뼉” 패드가 마디마다 빨라지고, 마지막 박은 조용해져요. | ecran: 23 | tenue: non | pourquoi: aucun ×2 vu, mais l'appli dit le dernier temps déjà silencieux alors que la mesure 4 a 16 claps allumés (image 29) et que le ch12 demande de les effacer
promesse: id=ch10-boutons | texte: 잘했어요, 성공이에요! | ecran: 31 | tenue: oui | pourquoi: étapes 1 à 6 : bouton d'action + « 보여 주세요 » puis « 잘했어요, 성공이에요! » ; étape 7 finit sur « 잘했어요, 챕터 완료! » (écran 33)
promesse: id=ch12-crash | texte: 잘했어요, 챕터 완료! | ecran: 54 | tenue: pas vue | pourquoi: la phrase « 여기 점선 칸이 1개 남았어요 » n'est jamais apparue (aucune case en pointillé, image 53) ; toucher la 1re case 심벌 a bien fini le chapitre, sans plantage
oeil-neuf: q1 | ecran: 54 | image: 54 | grille, onglets, carte ⌃ et « 챕터 » se voient touchables ; rien de trompeur
oeil-neuf: q6 | ecran: 54 | image: 54 | preuve : case 심벌 allumée + « 잘했어요, 챕터 완료! » ; mais pas de bouton « 다음 챕터 » visible dans la carte repliée, il faut deviner ⌃
oeil-neuf: q9 | ecran: 54 | image: 54 | « 인트로 1/128마디 » passe sur deux lignes en haut, « 오픈 하… » coupé dans la grille
hesitations:
  - ecran: 3 | cherchais: le chapitre 10 | regarde: la liste des chapitres s'arrête au 8 (coupé), il a fallu glisser
  - ecran: 34 | cherchais: le bouton « 엔딩 » demandé | regarde: il s'appelle « + H · 끝 »
  - ecran: 46 | cherchais: les « 빨간 × 4개 » annoncés | regarde: les 4 claps sont jaunes comme les autres, aucun × rouge
abandon: jamais
incidents-banc:
defauts:
  - geste: 21 | ecran.vu: 21 | cible: 직접 해 보세요: ‘“빌드업” 파트 만들기’를 탭하세요. 킥도 베이스도 없이, 클랩이 마디마다 점점 빨라져요. 끝의 한 박 침묵은 12챕터에서 만들어요. | geste.type: touche
    attendu: le « … » après une syllabe entière | obtenu: « … » dessiné par-dessus la moitié droite de « 박 » (image 21)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: texte-coupe
  - geste: 8 | ecran.vu: 8 | cible: 계속 읽기 | geste.type: touche
    attendu: la consigne longue s'ouvre en entier | obtenu: la fenêtre défile ; la 1re ligne « 직접 해 보세요… » passe sous l'en-tête (coupé), le lien devient un simple ⌄ ; idem écran 22
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: texte-coupe
  - geste: 23 | ecran.vu: 29 | cible: “빌드업” 파트를 만들었어요 (그루브 복사): 스튜디오 큰북, 낮은 울림, 오픈 하이햇, 북 테두리, 베이스, 멜로디 없이. “손뼉” 패드가 마디마다 빨라지고, 마지막 박은 조용해져요. | geste.type: touche
    attendu: le texte dit ce que montre la grille | obtenu: mesure 4 : 16 claps allumés, dernier temps pas silencieux ; le ch12 (écran 45) demande justement de le rendre silencieux
    symptome: texte-faux | gravite: finition | revu: oui: ecran 46 | banc?: non | promesse: roulement
  - geste: 32 | ecran.vu: 32 | cible: 직접 해 보세요: ‘“엔딩” 파트 만들기’를 탭하세요. 인트로의 드럼이에요. DJ가 다음 곡으로 이어 갈 수 있게요. | geste.type: touche
    attendu: même nom dans la consigne et sur le bouton | obtenu: bouton « “끝” 파트 만들기 », partie « H · 끝 » ; ch11 (écrans 34, 43) dit encore « 엔딩 »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 34 | banc?: non | promesse: -
  - geste: 46 | ecran.vu: 46 | cible: 직접 해 보세요: “빌드업” 파트의 마지막 마디에서 마지막 박에 있는 “손뼉” 패드의 히트 4개를 지우세요(빨간 × 4개를 하나씩 탭). | geste.type: touche
    attendu: 4 cases marquées d'un × rouge | obtenu: aucune marque, 4 cases jaunes comme les 12 autres (image 46) ; on devine à l'aide de « 마지막 박 »
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 13 | cible: “인트로 2” 파트를 만들었어요 (그루브 복사): 멜로디 없이. | geste.type: touche
    attendu: « 심벌과 리프 없이 » comme la consigne | obtenu: « 멜로디 없이 » seulement, la cymbale retirée n'est pas citée
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 18 | vu: 스튜디오 큰북 | attendu: 킥 (les consignes disent « 킥 », la liste et la grille « 큰북 »)
  - ecran: 46 | vu: 읽기 ⌃ | attendu: 계속 읽기 (carte repliée, mot tronqué)
defauts-avant:
  - c-V3 | corrigé | ecran: 29 | aucun ×2 / « 2번 반복 » vu sur le Build-up à sa création ; reste un texte qui annonce le silence final déjà fait (fiche geste 23)
  - c-V4 | autre | ecran: 21 | la consigne longue finit bien par « … » + « 계속 읽기 », mais le « … » mange la moitié de « 박 »
fin
