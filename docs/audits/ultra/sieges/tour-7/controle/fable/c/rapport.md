code testé : 258441802eda97ec42ca0967188dfc83c7aeb29f
joué par : sous-agent neuf

siege: c | profil: controle-ko-petit-tel-130 | lang: ko | device: phone360 | font: 130 | mode: simple
mission: tuto-facile (chapitres 9 à 12, contrôle fable-repar) | buts: -
promesse: id=ch10-boutons | texte: 직접 해 보세요: ‘“인트로” 파트 만들기’를 탭하세요. | ecran: 19 | tenue: oui | pourquoi: le bouton « “인트로” 파트 만들기 » et « 보여 주세요 » sont là, l'étape se coche (écran 20)
promesse: id=ch12-crash | texte: “드롭” 파트에서 “심벌” 패드의 맨 첫 칸을 탭하세요 | ecran: 55 | tenue: oui | pourquoi: la case 1 du cymbale du Drop est vide et pointillée, je la pose, chapitre fini (écran 56)
hesitations:
  - ecran: 49 | cherchais: le pad « 하이햇 » dont parle la consigne | regarde: l'écran montre « 4/4마디 · 손뼉 » avec 4 croix rouges, pas 3
  - ecran: 35 | cherchais: le bouton « 엔딩 » de la consigne | regarde: le bouton dit « “끝” 파트 만들기 »
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 30 | vu: premier « regarde » : « le banc ne tourne pas pour ce lot », bon au 2e essai
defauts:
  - geste: 30 | ecran.vu: 31 | cible: 직접 해 보세요: ‘“빌드업” 파트 만들기’를 탭하세요. 킥도 베이스도 없이, 클랩이 마디마다 점점 빨라져요. 끝의 한 박 침묵은 12챕터에서 만들어요. | geste.type: touche
    attendu: la consigne entière visible à 130 % | obtenu: coupée net au milieu de « 끝의 한 박 », sans points de suspension, sous le bandeau « Techno 킷 다운로드 » (aussi écrans 21, 33, 37, 46, 48)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: -
  - geste: 31 | ecran.vu: 32 | cible: “빌드업” 파트를 만들었어요 (그루브 복사): 스튜디오 큰북, 낮은 울림, 오픈 하이햇, 북 테두리, 베이스, 멜로디 없이. “손뼉” 패드가 마디마다 빨라지고, 마지막 박은 조용해져요. | geste.type: touche
    attendu: la partie neuve sans roulement, une étape suivante me le demande | obtenu: le roulement (×2 partout) est déjà posé (écran 49) et aucune étape ne le demande ; le message dit aussi « 마지막 박은 조용해져요 » alors que la consigne disait « 12챕터에서 만들어요 »
    symptome: incompris | gravite: finition | revu: oui: ecran 49 | banc?: non | promesse: -
  - geste: 48 | ecran.vu: 49 | cible: 4/4마디 · 손뼉 | geste.type: touche
    attendu: le pad « 하이햇 » avec 3 croix rouges, comme dit la consigne | obtenu: le pad affiché est « 손뼉 » avec 4 croix ; il faut en toucher 4 pour que l'étape se coche
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 34 | ecran.vu: 35 | cible: bouton ⊕ «   “끝” 파트 만들기 » | geste.type: touche
    attendu: le même nom que la consigne (« 엔딩 ») | obtenu: la consigne dit « 엔딩 », le bouton et la partie disent « 끝 » (« + H · 끝 », écran 38)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 38 | banc?: non | promesse: -
mots-faux:
  - ecran: 35 | vu: 끝 | attendu: 엔딩
defauts-avant:
  - V1 | corrigé | ecran: 19 | étape 1/7 du chapitre 10 : gros bouton « “인트로” 파트 만들기 » + « 보여 주세요 », je touche et ça coche (écran 20)
  - V2 | corrigé | ecran: 36 | chaque étape des ch. 10 à 12 avait un bouton qui agit (ou « 직접 할게요 ») et « 보여 주세요 », chacune s'est cochée ; chapitre 10 fini en 7/7
  - V3 | autre | ecran: 49 | le crash est bien demandé à part (case vide, écran 55), mais le roulement du « 빌드업 » est déjà là à la création et aucune étape ne le demande
  - V4 | encore là | ecran: 31 | flèches « 이전 / 다음 → » entières, mais la consigne longue est coupée en plein mot sous le bandeau de téléchargement (aussi écran 21)
