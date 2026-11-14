siege: c | profil: t5-Ec | lang: ko | device: tablet8 | font: 100 | mode: pro
mission: exports | buts: spotify, club, retrouve
promesse: id=t5-Ec-1 | texte: 리믹스 · 보컬 (1/4) … 파일 4개 공유 | ecran: 52 | tenue: oui | pourquoi: « 트랙별로 » sur le remix sort 4 fichiers (보컬, 드럼, 베이스, 나머지) et propose « 파일 4개 공유 »
promesse: id=t5-Ec-2 | texte: 준비 완료: 켜진 솔로도, 뮤트된 트랙도 없어요. | ecran: 29 | tenue: non | pourquoi: chanson de 0:40 décalée en mesure 2 (« 곡이 2마디, “A” 중간에서 들어와요. »), export toujours 40초 et « 준비 완료 » sans dire que la fin est coupée
promesse: id=t5-Ec-3 | texte: - | ecran: - | tenue: pas vue | pourquoi: aucun bouton « 2개 파일 공유 » trouvé (partage, Mes exports, Mix, vidéo SNS) ; seulement « 파일 4개 공유 » et des envois un par un
promesse: id=t5-Ec-4 | texte: 내 곡 공유 · 봄밤-리믹스-스트리밍-곡-20261105-213323.m4a | ecran: 25 | tenue: oui | pourquoi: le coréen tapé reste intact (espaces → tirets), idem 봄밤-리믹스-클럽-곡 dans 내 내보내기
promesse: id=t5-Ec-5 | texte: 프로젝트 이름은 “Ma chanson préférée” 그대로예요. | ecran: 20 | tenue: oui | pourquoi: après deux exports au nom coréen, l'en-tête reste « Ma chanson préférée 저장됨 » (écrans 27, 38)
hesitations:
  - ecran: 1 | cherchais: par où commencer, deux boutons « 모델 다운로드 » et « 노래 고르기 » orange alors que le modèle manque | regarde: la ligne « 먼저 분리 모델이 필요해요 »
  - ecran: 43 | cherchais: l'export piste par piste | regarde: « 고급 » sans état ouvert/fermé, a fallu deviner qu'il avait déplié les cases
  - ecran: 56 | cherchais: « Partager les 2 fichiers » (streaming + club ensemble) | regarde: la feuille de partage, puis Mes exports : un « 보내기 » par fichier seulement
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 0 | vu: un « retour » que je n'ai pas fait (journal « #1 > retour »), remis sur l'accueil ; refait Remix
  - geste: 13 | attente: 300 | vu: pendant la séparation, un « retour » non demandé (journal « #13 > retour ») ramène à l'accueil ; la séparation a quand même fini
  - geste: 12 | attente: 300 | vu: séparation lente (5 % → 35 % en plusieurs attentes), banc?: temps
defauts:
  - geste: 28 | ecran.vu: 29 | cible: 준비 완료: 켜진 솔로도, 뮤트된 트랙도 없어요. | geste.type: touche
    attendu: avec la chanson (0:40) placée en mesure 2 d'un projet de 20 mesures, « 준비 » signale que la fin du remix est coupée | obtenu: « 준비 완료 » vert, durée 0:40, puis « 준비됐어요 … 40초 » (écran 37) sans aucun avertissement
    symptome: perdu
    gravite: contourne-en-1-gestes | revu: oui: ecran 80 | banc?: son | promesse: t5-Ec-2
  - geste: 61 | ecran.vu: 62 | cible: 이 내보내기 뒤에 곡이 바뀌었어요: 새 버전을 보내려면 다시 내보내세요. | geste.type: touche
    attendu: juste après un export tout neuf (MIDI), pas d'alerte « le morceau a changé » | obtenu: l'alerte s'affiche sous le fichier qui vient d'être exporté, sans que j'aie rien modifié (seulement ouvert puis fermé « 곡 자르기 »)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 74 | banc?: non | promesse: -
mots-faux:
  - ecran: 9 | vu: 휴대폰 (« 모델이 휴대폰에 있어요 ») | attendu: 기기 (je suis sur tablette)
  - ecran: 40 | vu: 이 휴대폰의 음악 › BenStudio | attendu: 이 기기의 음악 › BenStudio
  - ecran: 71 | vu: Ma-chanson-préférée-video-…mp4 | attendu: …-영상-… (comme « -곡- » pour l'audio)
