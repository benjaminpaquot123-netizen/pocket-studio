siege: b | profil: guitariste coréen | lang: ko | device: tablet8 | font: 100 | mode: simple
mission: voix | buts atteints: 3 sur 3
hesitations:
  - ecran: 1 | cherchais: un bouton « enregistrer ma voix/guitare » sur l'accueil | regarde: les 6 cartes, aucune ne parle d'enregistrer ; j'ai pris « 나중에: 시작 비트 보기 » au hasard
  - ecran: 10 | cherchais: laquelle des deux prises est la première | regarde: les puces « 테이크 03:37 · 7 s » et « 테이크 03:37 · 6 s », même heure, seule la durée diffère
  - ecran: 14 | cherchais: le volume de ma prise | regarde: la tranche « Audio 1 » (nom anglais générique, pas « 내 목소리 »)
surprise: en touchant une prise, les puces échangent leur place (la prise choisie saute en premier), j'ai cru avoir touché la mauvaise
abandon: jamais
defauts:
  - ecran: 1 | cible: cartes de l'accueil | geste: touche
    attendu: un choix « 녹음하기 / 내 목소리·기타 녹음 » | obtenu: rien sur l'enregistrement, il faut passer par le beat puis l'onglet 내 곡
    symptome: sans-aide | gravite: moyen | bloque: non
  - ecran: 1 | cible: « 아직 음표가 없어요: 드럼, Acid 303, 신스가 당신의 음표를 기다려요. » | geste: touche
    attendu: coréen naturel | obtenu: « 당신의 » sonne traduit mot à mot ; « 나중에: 시작 비트 보기 » aussi peu naturel
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 2 | cible: « 패드로 연주하는 것 녹음 » (bouton rond du haut, onglet 리듬) | geste: touche
    attendu: même bouton d'enregistrement partout | obtenu: dans 리듬 il n'enregistre que les pads, le micro n'apparaît qu'en 내 곡 (« 녹음: 마이크, 건반 또는 패드 »)
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 5 | cible: piste « Audio 1 », « Audio 1 · 파트 A에서 », « Audio 1 옵션 » | geste: touche
    attendu: nom en coréen (ex. « 오디오 1 » ou « 내 목소리 ») | obtenu: nom de piste laissé en anglais partout (mix, 내 곡, enregistrement)
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 5 | cible: barre de niveau d'entrée sous « Audio 1 · 파트 A에서 » | geste: touche
    attendu: vumètre bien visible pendant le décompte | obtenu: trait vert très fin, sans libellé ni nom TalkBack
    symptome: sans-nom | gravite: mineur | bloque: non
  - ecran: 7 | cible: « 테이크 03:37 · 5.5 s » vs « 녹음: 5.0초 » | geste: touche
    attendu: même unité en coréen (초) | obtenu: « s » anglais dans la liste des prises, « 초 » pendant l'enregistrement
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 10 | cible: puces « 테이크 03:37 · 7 s » / « 테이크 03:37 · 6 s » | geste: touche
    attendu: prises numérotées (테이크 1, 테이크 2) et ordre stable | obtenu: même heure pour les deux, ordre qui change à chaque sélection
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 10 | cible: « 테이크 03:37 · 6 s 빼기 » | geste: touche
    attendu: « 삭제 » ou « 지우기 » | obtenu: « 빼기 » (soustraction), peu clair pour supprimer une prise
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 7 | cible: « 노멀라이즈 », « 리버스 », « 선택 부분 남기기 » | geste: touche
    attendu: mots simples en mode 간단 | obtenu: jargon anglais translittéré sans aide
    symptome: sans-aide | gravite: mineur | bloque: non
  - ecran: 14 | cible: « 네 차례: 들려야 할 소리는 올리고, 방해되는 소리는 내려. ▶ 를 눌러 트랙을 혼자 들어봐. » | geste: touche
    attendu: même politesse (해요체) que le reste de l'app | obtenu: phrase en 반말 ; espace en trop dans « ▶ 를 »
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 14 | cible: interrupteur « 사운드 » (« Audio 1 소리 끄기 », état « 꺼짐 ») | geste: touche
    attendu: état clair (son actif) | obtenu: « 꺼짐 » (éteint) affiché alors que la piste sonne, on croit qu'elle est coupée
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 18 | cible: « “Audio 1” 따로 반복해서 듣기 » (désactiver le solo) | geste: touche
    attendu: retour au mix complet qui continue de jouer | obtenu: toute la lecture s'arrête (« 재생 » réapparaît), il faut relancer
    symptome: incompris | gravite: moyen | bloque: non
