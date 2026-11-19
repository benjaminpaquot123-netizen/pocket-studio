code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: debutant-motive | lang: ko | device: foldable | font: 100 | mode: simple
mission: tuto-facile (tech_house ch.1-5) | buts: -
chapitres:
  - ch1 « 곡의 빠르기와 색깔 » : compris du premier coup (fini écran 14)
  - ch2 « 드럼 » : hésitation (fini écran 80 ; compteurs de pointillés trompeurs, charley long à remplir)
  - ch3 « 베이스 » : hésitation (fini écran 118 ; compteur et flèche « 더 위 ↑ » déroutants)
  - ch4 « 보컬 한 마디 » : compris du premier coup (fini écran 129)
  - ch5 « 오르간 » : hésitation (fini écran 137 ; cadre de repère sur B♭ alors que la consigne dit la note du bas)
  - budget : non épuisé, chapitres 1 à 5 finis en 137 gestes + fin
hesitations:
  - ecran: 24 | cherchais: quelles cases remplir | regarde: « 2마디의 점선 칸 » : deux mesures, ou la mesure 2 ? la flèche « 2마디 · 3칸 → » a tranché
  - ecran: 32 | cherchais: un moyen plus rapide | regarde: 23 touches une à une pour le charley ; rien ne dit au débutant que le glissé suffit
  - ecran: 133 | cherchais: la « 맨 아래 노트 » | regarde: le cadre jaune est sur B♭, la note du bas est G ; j'ai tapé G, ça a marché
abandon: jamais
incidents-banc:
defauts:
  - geste: 43 | ecran.vu: 44 | cible: 2마디 · 11칸 → | geste.type: touche
    attendu: le compteur « 여기 점선 칸이 12개 남았어요 » annonce tout ce qu'il faut remplir | obtenu: arrivé à 0, 11 cases de plus apparaissent dans la mesure 2 (pareil écrans 26 et 72, « 15개 » puis « 2마디 · 15칸 → »)
    symptome: incompris
    gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 93 | ecran.vu: 93 | cible: 베이스 · 1/8마디 점선 음이 7개 남았어요, 더 위 ↑ 여기를 탭 | geste.type: touche
    attendu: après les 8 notes pointillées visibles de la mesure 1, la mesure est finie | obtenu: le compteur en réclame encore 7 ; « 여기를 탭 » montre une autre zone où mes notes ne se voient plus
    symptome: incompris
    gravite: contourne-en-2-gestes | revu: oui: ecran 106 | banc?: non | promesse: -
  - geste: 95 | ecran.vu: 95 | cible: 베이스 · 2/8마디 점선 노트가 6개 더 있어요, 뒤쪽에 → 여기를 탭 | geste.type: touche
    attendu: « 성공 다음 단계 » seulement quand il ne reste rien | obtenu: « 성공 » s'affiche avec 6 notes encore annoncées (pareil écran 105 avec « 6개 남았어요 »)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 105 | banc?: non | promesse: -
  - geste: 101 | ecran.vu: 102 | cible: 베이스 · 2/8마디 점선 음이 2개 남았어요, 더 위 ↑ 여기를 탭 | geste.type: touche
    attendu: « 더 위 ↑ » mène vers des notes plus hautes | obtenu: on voit F, E, E♭, D, des lettres qui semblent plus basses (pas d'octave sauf C3) ; la flèche paraît mentir
    symptome: incompris
    gravite: finition | revu: oui: ecran 114 | banc?: non | promesse: -
  - geste: 107 | ecran.vu: 107 | cible: 베이스 · 4/8마디 ↑ 탭할 점선 노트가 6개 남았어요 여기를 탭 | geste.type: touche
    attendu: la consigne « 4마디의 노트 8개 » et le bandeau parlent du même nombre | obtenu: la consigne dit 8, le bandeau 6 ; à l'écran 116 les notes déjà posées de la mesure 4 ne se voient plus
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 132 | ecran.vu: 133 | cible: 코드 · 1/8마디 탭할 점선 코드: 2 | geste.type: touche
    attendu: le cadre jaune de repère est sur la note du bas, comme dit « 맨 아래 노트를 탭하세요 » | obtenu: le cadre est sur B♭ ; la note pointillée du bas est G (taper G marche)
    symptome: incompris
    gravite: finition | revu: oui: ecran 135 | banc?: non | promesse: -
mots-faux:
defauts-avant: sans objet

fin-tranche: oui (ROUTE ligne 659)
