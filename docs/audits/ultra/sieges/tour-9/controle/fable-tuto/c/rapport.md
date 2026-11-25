code testé : d87dea73ff18887bc55b2ac5e0987f5e17be4c21
joué par : sous-agent neuf
siege: c | profil: debutant-motive | lang: ko | device: small-360 | font: 130 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=- | texte: 잘했어요, 챕터 완료! | ecran: 83 | tenue: oui | pourquoi: les chapitres 9, 10, 11, 12 et 14 se sont terminés avec cet écran ; le chapitre 13 est passé au 14 sans écran de fin
hesitations:
  - ecran: 35 | cherchais: « 계속 읽기 » pour lire la fin de la consigne | regarde: le bouton s'appelle seulement « 읽기 ^ », j'ai deviné qu'il ouvrait le texte
  - ecran: 41 | cherchais: le bouton de la partie suivante sous « 추가 » | regarde: la liste avait sauté, le bouton suivant était passé sous la barre du tuto
  - ecran: 81 | cherchais: « 인트로 1/128마디 » / « 빌드업 77/128마디 » en haut | regarde: l'en-tête garde le nom d'une partie même quand je suis ailleurs, pas clair
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: outil utilisé hors human-act.py : Read sur brief.md et sur des captures PNG du dossier du siège (écrans 1, 12, 30, 35, 52, 69) ; chaque réponse portait bien « Siège controle/fable-tuto/c »
defauts:
  - geste: 34 | ecran.vu: 34 | cible: 직접 해 보세요: “내 곡”의 “추가” 아래에서 인트로, 인트로 2, 드롭, 브레이크, 빌드업, 드롭 2, 끝을 순서대로 탭하세요. | geste.type: touche
    attendu: au plus deux gestes par consigne, la suite à l'étape suivante | obtenu: une seule consigne demande 7 touches de suite
    symptome: incompris
    gravite: contourne-en-7-gestes | revu: oui: ecran 52 | banc?: non | promesse: -
  - geste: 52 | ecran.vu: 52 | cible: 빌드업 끝에서 마지막 박에 있는 “손뼉” 패드의 히트 4개를 지우세요 | geste.type: touche
    attendu: au plus deux gestes par consigne | obtenu: une seule consigne demande d'effacer 4 coups
    symptome: incompris
    gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 42 | ecran.vu: 41 | cible: “내 곡”의 “추가” | geste.type: touche
    attendu: après chaque partie ajoutée, le bouton suivant reste visible au même endroit | obtenu: la liste revient en arrière, le bouton suivant sort de l'écran ou passe sous la barre du tuto ; il faut glisser (gestes 42, 44, 47)
    symptome: cache
    gravite: contourne-en-3-gestes | revu: oui: ecran 46 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 18 | cible: Techno 킷 다운로드 (4 MB) | geste.type: touche
    attendu: l'étape « 맵을 보세요 » montre toute la carte des parties | obtenu: le bandeau de téléchargement du kit recouvre la carte, les noms de parties et les numéros de mesure sont coupés (vu aussi aux écrans 1 et 12)
    symptome: cache
    gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: -
  - geste: 35 | ecran.vu: 35 | cible: 읽기 ^ | geste.type: touche
    attendu: le bouton de lecture s'appelle « 계속 읽기 » comme ailleurs | obtenu: il s'appelle seulement « 읽기 » ; à l'écran 1, au contraire, un « 계속 읽기 » sous un texte court ouvre la carte d'énergie et pas le texte
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 81 | banc?: non | promesse: -
mots-faux:
  - ecran: 62 | vu: 클랩 | attendu: 손뼉 (le même pad s'appelle 손뼉 partout ailleurs)
  - ecran: 12 | vu: 북 테두리 | attendu: 림
  - ecran: 21 | vu: (그루브 복사) | attendu: 그루브 복사하기 / 같은 그루브로
defauts-avant:
  - c-V3 | corrigé | ecran: 79 | dans la partie 빌드업, les coups du pad 손뼉 (mesure 1 coup 1, mesure 4 coup 12) ont le roll ×1 sélectionné, aucun ×2 posé d'avance ; la leçon ne m'a jamais demandé d'en mettre un
  - c-V4 | corrigé | ecran: 36 | à l'écran 35 la consigne longue finit par « 드롭,… » sur un mot entier ; le bouton (nommé « 읽기 », pas « 계속 읽기 ») a montré tout le texte à l'écran 36
  - D4 | encore là | ecran: 34 | une consigne demande 7 touches de suite (7 parties) ; l'écran 52 en demande encore 4 (effacer 4 coups)
  - D2 | corrigé | ecran: 20 | l'étape nomme « 인트로 » et le bouton « “인트로” 파트 만들기 » est bien là ; pareil pour 빌드업 à l'écran 28
