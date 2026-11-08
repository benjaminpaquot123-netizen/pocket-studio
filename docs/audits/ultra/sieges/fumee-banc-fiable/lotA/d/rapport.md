siege: d | profil: t5-Ec | lang: ko | device: tablet8 | font: 100 | mode: pro
mission: exports | buts: spotify, club, retrouve
promesse: id=t5-Ec-1 | texte: 리믹스 · 보컬 (1/4) … Ma-chanson-préférée-04-리믹스-나머지-20261108-145257.wav | ecran: 44 | tenue: oui | pourquoi: « 트랙별로 » sur le remix a donné 4 fichiers (보컬, 드럼, 베이스, 나머지), les pistes vides sont annoncées sautées
promesse: id=t5-Ec-2 | texte: 뮤트되어 파일에서 빠져요: 보컬 (Ma chanson préférée). | ecran: 24 | tenue: oui | pourquoi: après avoir coupé la voix du remix, le bilan « 준비 » le dit et propose « 모두 다시 듣기 », qui remet « 준비 완료 »
promesse: id=t5-Ec-3 | texte: - | ecran: 46 | tenue: pas vue | pourquoi: aucun bouton « 2개 파일 공유 » vu (ni après 2 exports, ni dans 내 내보내기, l'appui long ne sélectionne rien)
promesse: id=t5-Ec-4 | texte: 내-최애곡-리믹스-곡-스트리밍-20261108-145132.wav | ecran: 17 | tenue: oui | pourquoi: nom coréen tapé conservé tel quel dans le fichier, le bilan et 내 내보내기
promesse: id=t5-Ec-5 | texte: 프로젝트 이름은 “Ma chanson préférée” 그대로예요. | ecran: 19 | tenue: oui | pourquoi: après l'export le projet s'appelle toujours « Ma chanson préférée »
hesitations:
  - ecran: 20 | cherchais: où couper une partie du remix (je croyais que « 곡 자르기 » coupait le morceau) | regarde: c'est un éditeur de tranches pour pads ; la coupure est l'interrupteur « 보컬 소리 » plus bas
  - ecran: 38 | cherchais: le bouton d'export après avoir coché « 트랙별로 » | regarde: le bouton a remonté de 1009 à 795, mon toucher est tombé dans le vide
  - ecran: 46 | cherchais: envoyer plusieurs fichiers d'un coup (les 4 pistes) | regarde: seulement un « 보내기 » par ligne
abandon: jamais
incidents-banc:
  - geste: 7 | attente: 15 | vu: écran 8 vide (aucun élément) juste après le choix « 프로 », revenu normal à l'écran 9
  - geste: 39 | attente: 240 | vu: export « 트랙별로 » de 4 fichiers de 40 s a pris environ 4 minutes (fichiers de 1 KB dans 내 내보내기), durées du banc
defauts:
  - geste: 37 | ecran.vu: 38 | cible: bouton « 트랙 내보내기 » @ 450,795 | geste.type: touche
    attendu: le bouton d'export reste à sa place quand on coche « 트랙별로 » | obtenu: le bouton « 곡 내보내기 » (1009) devient « 트랙 내보내기 » et remonte à 795 ; mon toucher suivant (#38) tombe dans le vide
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 24 | cible: champ « 보낼 곡 이름 » contenu « Ma chanson préférée » | geste.type: touche
    attendu: le nom que je venais de taper (« 내 최애곡 리믹스 ») reste proposé pour le 2e export | obtenu: le champ revient au nom du projet, il faut effacer et retaper (3 gestes) à chaque export
    symptome: perdu
    gravite: contourne-en-3-gestes | revu: oui: ecran 25 | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 46 | cible: texte « Ma-chanson-préférée-01-리믹스-보컬-20261108-145246.wav » | geste.type: touche
    attendu: les 4 pistes d'un même export « 트랙별로 » regroupées ou envoyables ensemble | obtenu: 4 lignes séparées, un « 보내기 » chacune, aucune sélection multiple (appui long #46 sans effet)
    symptome: sans-aide
    gravite: finition | revu: non | banc?: non | promesse: t5-Ec-3
mots-faux:
  - ecran: 17 | vu: 내-최애곡-리믹스-곡-스트리밍 | attendu: 내-최애곡-리믹스-스트리밍 (le mot « 곡 » est ajouté au nom tapé, sans qu'on l'ait demandé)
