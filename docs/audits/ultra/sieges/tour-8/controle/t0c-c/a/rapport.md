code testé : dee3519df1694386b61d84269762b368f08029e8
joué par : sous-agent neuf
siege: a | profil: t8-t0c-c debutante-coreenne-360dp | lang: ko | device: phone360 | font: 130 | mode: simple
mission: tuto-facile | buts: finir-chapitre-9, chapitre-10-etape-2, -
chapitre: 9 | compris du premier coup | 7 étapes d'écoute (예시 듣기 puis 다음 단계), fin « 잘했어요, 챕터 완료! » écran 20
chapitre: 10 | hésitation | 7/7 étapes faites (écran 44), mais les consignes citent « 리프 », « 심벌 », « 킥 », « 클랩 » que je ne retrouve pas sur l'écran : je suis allée vérifier 리듬 et 건반 (3 gestes)
promesse: id=t0c-c-1 | texte: 버튼: “빌드업” 파트 만들기. 킥이 없고 클랩이 폭주해서 긴장이 올라가요. | ecran: 37 | tenue: non | pourquoi: les pads s'appellent « 스튜디오 큰북 FX » et « 손뼉 FX » (écran 27), pas 킥/클랩 ; « 리프 » et « 심벌 » (écrans 24, 31) n'existent ni dans les pads ni dans la liste du Groove (écran 32)
promesse: id=t0c-c-2 | texte: 내 프로젝... / Techno 킷 다운로드 (4 MB) | ecran: 45 | tenue: non | pourquoi: le nom du projet est coupé en haut (« 내 프로젝... », image 45), « 추가: » coupé, et « Techno », « Acid 303 » restent en anglais
oeil-neuf: q1 | ecran: 45 | image: 45 | les puces « + A · 그루브 » … « + H · 끝 » ont l'air touchables et le sont ; la carte « 잘했어요, 챕터 완료! » avec ⌃ ne montre pas qu'il faut la rouvrir pour « 다음 챕터 »
oeil-neuf: q6 | ecran: 45 | image: 45 | preuve = les 8 puces A à H ; mais la ligne du temps du morceau (l'ordre) n'est pas visible, et le bouton « 다음 챕터 » est caché dans le panneau replié
oeil-neuf: q9 | ecran: 45 | image: 45 | « 내 프로젝... » coupé en haut ; « 추가: » collé au bord, la partie au-dessus est coupée
hesitations:
  - ecran: 25 | cherchais: le « 리프 » dont parle la consigne d'인트로 2 | regarde: pads (écran 27) et 건반 (écran 28) : aucun « 리프 », seulement 베이스 라인, 베이스, 코드, 멜로디
  - ecran: 31 | cherchais: la « 심벌 » qui entre au drop | regarde: la liste du drop (écran 32) ne contient ni 심벌 ni 리프
  - ecran: 21 | cherchais: le sens des doubles guillemets ““인트로” 파트 만들기” | regarde: le bouton orange plus bas porte le même texte, j'ai compris
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun outil hors human-act.py (seulement lecture des consignes, du brief et de mon dossier de siège, et écriture de rapport.md)
defauts:
  - geste: 30 | ecran.vu: 31 | cible: 버튼: “드롭” 파트 만들기. 심벌과 리프가 동시에 들어와요: 가장 강한 순간이에요. | geste.type: touche
    attendu: la consigne nomme ce qui entre vraiment au drop | obtenu: « 심벌 » et « 리프 » absents de mon Groove (écran 32 : 스튜디오 큰북, 낮은 울림, 손뼉, 오픈 하이햇, 북 테두리, 베이스, 코드, 멜로디)
    symptome: texte-faux | gravite: contourne-en-3-gestes | revu: oui: ecran 32 | banc?: non | promesse: t0c-c-1
  - geste: 23 | ecran.vu: 24 | cible: 버튼: “인트로 2” 파트 만들기. 리프는 드롭을 위해 남겨 둬요. 그래야 효과가 더 커요. | geste.type: touche
    attendu: la consigne d'인트로 2 parle de ce qui est retiré de mon Groove | obtenu: elle parle d'un « 리프 » que je n'ai pas ; le résultat dit « 멜로디 없이 » (écran 25), aucun mot sur la cymbale
    symptome: texte-faux | gravite: contourne-en-3-gestes | revu: oui: ecran 28 | banc?: non | promesse: t0c-c-1
  - geste: 36 | ecran.vu: 37 | cible: 버튼: “빌드업” 파트 만들기. 킥이 없고 클랩이 폭주해서 긴장이 올라가요. 끝의 한 박 침묵은 12챕터에서 만들어요. | geste.type: touche
    attendu: les mots des pads de l'écran (큰북, 손뼉) | obtenu: « 킥 », « 클랩 » ; le résultat juste après dit « “손뼉” 패드 » (écran 38) : deux mots pour le même pad
    symptome: texte-faux | gravite: finition | revu: oui: ecran 38 | banc?: non | promesse: t0c-c-1
  - geste: 20 | ecran.vu: 21 | cible: 내 차례: ““인트로” 파트 만들기”를 누르세요. | geste.type: touche
    attendu: un seul jeu de guillemets autour du nom du bouton | obtenu: guillemets doublés ““…”…” à chaque étape du chapitre 10 (écrans 21, 23, 30, 33, 36, 39, 42)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: t0c-c-2
  - geste: 44 | ecran.vu: 45 | cible: icône (dossier ouvert) ✓ « 내 프로젝트 1 저장됨 » | geste.type: touche
    attendu: le nom du projet lisible à 130 % | obtenu: « 내 프로젝... » coupé en haut sur tous les écrans (images 1, 21, 43, 45)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 1 | banc?: non | promesse: t0c-c-2
  - geste: 0 | ecran.vu: 1 | cible: bouton « Techno 킷 다운로드 (4 MB) » | geste.type: touche
    attendu: tout en coréen | obtenu: « Techno » en anglais (et « Acid 303 베이스 라인 » écran 28) ; ce bandeau reste en plus à chaque étape des chapitres 9 et 10 et pousse la consigne
    symptome: texte-faux | gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: t0c-c-2
mots-faux:
  - ecran: 13 | vu: “북 테두리” | attendu: 림샷
  - ecran: 37 | vu: 클랩 | attendu: 손뼉 (mot du pad)
  - ecran: 37 | vu: 킥 | attendu: 큰북 (mot du pad)
defauts-avant:
  - t0c-ko-ending | corrigé | ecran: 45 | la partie s'appelle « + H · 끝 » et la consigne dit « “끝” 파트 만들기 » (écran 43), même mot que l'écran ; le bouton de fin de chapitre dit « ✓ 완료 » (écran 20) et l'en-tête « 완료 », plus « 끝 »
  - t0c-intro2-facile | encore là | ecran: 24 | « 리프는 드롭을 위해 남겨 둬요 » : un 리프 que mon Groove n'a pas (liste écran 32) ; résultat « 멜로디 없이 » (écran 25) ; aucune mention de la cymbale retirée
  - t0c-c-V3-creation-montee | autre | ecran: 37 | ne dit plus que le dernier temps se tait maintenant, mais dit encore « 끝의 한 박 침묵은 12챕터에서 만들어요 » ; le résultat (écran 38) dit « “손뼉” 패드가 마디마다 빨라져요 », sans silence
  - c-V3 | corrigé | ecran: 38 | en Facile, la montée avec le roulement se fait en un seul toucher « “빌드업” 파트 만들기 » ; consigne courte, résultat en une phrase
