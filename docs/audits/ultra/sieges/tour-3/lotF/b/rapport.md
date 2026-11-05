siege: b | profil: producteur coréen qui remixe puis exporte | lang: ko | device: tablet8 | font: 100 | mode: pro
mission: exports | buts atteints: 3 sur 3
hesitations:
  - ecran: 28 | cherchais: où régler la cible de volume (목표: 없음) | regarde: carte 내보내기, « 고급 », aide LUFS, puis défilé jusqu'à 마스터 볼륨 (écran 37)
  - ecran: 53 | cherchais: mes 4 pistes séparées (보컬/드럼/베이스/나머지) après 트랙 내보내기 | regarde: ligne « 저장됨 », bouton 공유, onglet 구성
  - ecran: 15 | cherchais: renommer le projet en coréen | regarde: titre en haut, puis menu « 옵션 » de 내 프로젝트
promesse: Remix exporté proprement, nom de fichier en coréen intact, cible de volume tenue | tenue: en partie | pourquoi: coréen intact et −14 LUFS tenu, mais cible éteinte par défaut et cachée loin de l'export, export par pistes ne sort pas les pistes du remix
surprise: le champ « 곡 이름 » du partage renomme tout le projet à chaque export de version
abandon: jamais
defauts:
  - ecran: 58 | cible: « 준비 완료: 켜진 솔로도, 뮤트된 트랙도 없어요. » | geste: touche
    attendu: avertir que la piste 보컬 du remix est coupée (version MR) | obtenu: dit qu'aucune piste n'est muette
    symptome: promesse | gravite: grave | bloque: non
  - ecran: 53 | cible: « 트랙 내보내기 » (트랙마다 파일 하나: 드럼, 애시드, 신스, 리믹스) | geste: touche
    attendu: un fichier par piste séparée (보컬, 드럼, 베이스, 나머지) ou au moins les 4 annoncés | obtenu: un seul fichier « …-01-remix-….wav », sans dire pourquoi
    symptome: promesse | gravite: grave | bloque: non
  - ecran: 26 | cible: « 목표: 없음 (목표 음량 꺼짐) » | geste: touche
    attendu: cible réglable depuis la feuille de partage / la carte export | obtenu: éteinte par défaut, réglage seulement dans 믹스 sous 마스터 볼륨, après les réverbes
    symptome: sans-aide | gravite: moyen | bloque: non
  - ecran: 41 | cible: champ « 곡 이름 (프로젝트 이름도) » | geste: ecris
    attendu: nommer une version exportée (ex. 봄밤 인스트 (MR)) sans toucher au projet | obtenu: le projet est renommé à chaque export
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 45 | cible: « 봄밤-리믹스-두번째-완성-곡-streaming-….m4a », « …-01-remix-….wav » | geste: touche
    attendu: nom tout en coréen | obtenu: mots anglais « streaming », « remix » collés au nom ; « remix » aussi dans la liste projets (écran 21)
    symptome: coupe | gravite: mineur | bloque: non
  - ecran: 29 | cible: « “최종 파일”란 뭘까요? », « “목표 음량”란 뭘까요? » (écran 37) | geste: touche
    attendu: « 최종 파일이란 », « 목표 음량이란 » | obtenu: particule fautive ; aussi « 을(를) » (écran 13), « 은(는) » (écran 22)
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 70 | cible: « 이미지와 압축… » (SNS용 영상) | geste: attends
    attendu: phrase complète type « 이미지 만들고 압축하는 중… » avec un pourcentage | obtenu: phrase tronquée, pas d'avancement chiffré
    symptome: coupe | gravite: mineur | bloque: non
  - ecran: 22 | cible: touchable sans nom @ 450,686 (poignée de la feuille de partage) | geste: touche
    attendu: un nom lu par TalkBack | obtenu: élément sans nom en doublon de « 드래그 핸들 »
    symptome: sans-nom | gravite: mineur | bloque: non
  - ecran: 7 | cible: « “Ma chanson préférée” 분리 중… 15% » | geste: attends
    attendu: « 몇 분 걸려요 » pour un morceau de 0:40 | obtenu: environ 25 min d'attente (écrans 6 à 12), sans temps restant
    symptome: lent | gravite: moyen | bloque: non
