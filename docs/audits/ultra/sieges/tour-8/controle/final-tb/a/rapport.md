code testé : 80e7f93e7bef6f5434f4bec78b5129e96b9b1bcf
joué par : sous-agent neuf
siege: a | profil: debutante-coreenne-petit-ecran | lang: ko | device: phone360 | font: 130 | mode: simple
mission: tuto-facile | buts: finir-chapitre-9, finir-chapitre-10, -
chapitres: ch9=compris du premier coup ; ch10=compris du premier coup
promesse: id=amenee-ch9 | texte: 파트 이해하기 9/15장 · 1/7단계 · 쉬움 | ecran: 1 | tenue: oui | pourquoi: le premier écran du tuto est le chapitre 9 (9/15장), pas le chapitre 1
promesse: id=ch10-etape1 | texte: 내 차례: ““인트로” 파트 만들기”를 누르세요. / “인트로” 파트 만들기 | ecran: 20 | tenue: oui | pourquoi: le bouton porte le même mot « 인트로 » ; un toucher (#20) donne « 잘했어요, 성공이에요! » puis 다음 단계, sans « 다음 »
promesse: id=ch10-fin | texte: 파트를 만들어 보세요 10/15장 · 완료 · 쉬움 / 잘했어요, 챕터 완료! | ecran: 32 | tenue: oui | pourquoi: les 7 étapes (인트로, 인트로 2, 드롭, 브레이크, 빌드업, 드롭 2, 끝) validées une à une, jamais bloquée
oeil-neuf: q1 | ecran: 34 | image: 34 | les puces « + A · 그루브 » … « + H · 끝 » et « + 새 파트 » se lisent comme des boutons ; le tuto replié (⌃) aussi ; rien de trompeur
oeil-neuf: q6 | ecran: 34 | image: 34 | preuve : les 8 parties A à H listées, « 잘했어요, 챕터 완료! » ; la suite (다음 챕터) n'est visible qu'en dépliant la carte
oeil-neuf: q9 | ecran: 34 | image: 34 | « 2/8마디 » sous « 끝 » déborde en bas de la barre du haut ; le nom du projet « 내 프로젝... » est coupé
hesitations:
  - ecran: 17 | cherchais: les cases de la « 맵 » avec leur longueur en mesures | regarde: seulement la courbe 에너지, les cases sont cachées sous le texte à 130 %
  - ecran: 19 | cherchais: le mot exact entre les guillemets doubles « ““인트로” 파트 만들기” » | regarde: le gros bouton orange « “인트로” 파트 만들기 », trouvé tout de suite
abandon: jamais
incidents-banc:
  - geste: 34 | attente: 0 | vu: le banc a écrit en tête « code testé : 80e7f93e7bef6f5434f4bec78b5129e96b9b1bcf », alors que le brief annonçait c9d48c1855fdfd2943731ecdf1d4a7322b9b8474 : ligne du banc gardée, écart à vérifier par le chef
defauts:
  - geste: 17 | ecran.vu: 17 | cible: 맵을 보세요: 칸마다 파트 하나이고, 마디 단위 길이가 적혀 있어요. 곡선은 에너지를 보여 줘요. | geste.type: touche
    attendu: voir la carte avec une case par partie et sa longueur en mesures | obtenu: à 130 % le texte pousse la carte : seule la courbe 에너지 se voit ; sur les étapes 2 à 6 les longueurs (16, 32, 12…) sont coupées
    symptome: cache | gravite: finition | revu: oui: ecran 14 | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 19 | cible: 내 차례: ““인트로” 파트 만들기”를 누르세요. | geste.type: touche
    attendu: un seul jeu de guillemets autour du nom du bouton (« “인트로” 파트 만들기 »를 누르세요) | obtenu: guillemets doublés « ““ … ”” », lourds à lire (même chose aux étapes 2 à 7)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 31 | banc?: non | promesse: -
  - geste: 34 | ecran.vu: 34 | cible: 끝 2/8마디 | geste.type: touche
    attendu: le compteur de mesures de la partie jouée entier dans la barre du haut | obtenu: « 2/8마디 » dessiné sous le bord de la barre (coupé en bas)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: 내 프로젝트 1 저장됨 | geste.type: touche
    attendu: le nom du projet lisible | obtenu: « 내 프로젝... » coupé sur petit écran à 130 % (tous les écrans)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 34 | banc?: non | promesse: -
defauts-avant:
  - D2 | corrigé | ecran: 20 | la consigne nomme « “인트로” 파트 만들기 », le bouton porte le même texte, le toucher donne « 잘했어요, 성공이에요! »
  - ctrl-c-ch10 | corrigé | ecran: 32 | chapitre 10 fini jusqu'au bout : « 10/15장 · 완료 · 쉬움 », « 잘했어요, 챕터 완료! », sans « 다음 »
  - amenee-ch9 | corrigé | ecran: 1 | l'amenée tuto:techno:9 ouvre directement « 파트 이해하기 9/15장 · 1/7단계 »
