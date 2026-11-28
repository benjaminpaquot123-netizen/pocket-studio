code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

```
siege: a | profil: debutant-motive | lang: ko | device: foldable | font: 100 | mode: simple
mission: tuto-facile (acidcore, chapitres 6-11) | buts: chapitres 6 à 11 finis
promesse: id=- | texte: 잘했어요, 챕터 완료! | ecran: 65 | tenue: oui | pourquoi: la liste des chapitres coche 6, 7, 8, 9, 10 et 11 ; partage M4A fini (écran 62)
chapitres:
  - 6 파트를 만들어 보세요 : compris du premier coup (7 fois le même bouton orange)
  - 7 파트를 정리하세요 : hésitation (les boutons « + » ont sauté sous mon doigt après le premier ajout, une partie en trop, annulée)
  - 8 트랜지션 : compris du premier coup (X rouges et case pointillée bien visibles)
  - 9 필터 움직이기 : compris du premier coup (juste écouter)
  - 10 믹스: 볼륨 : compris du premier coup (il faut ouvrir « 드럼 ▸ » pour trouver 손뼉, trouvé seul)
  - 11 곡을 내보내세요 : compris du premier coup
hesitations:
  - ecran: 20 | cherchais: « + C · 인트로 2 » là où il était juste avant | regarde: la liste « 추가 » est descendue d'un cran quand la frise est apparue
  - ecran: 35 | cherchais: ce que veut dire « 여기 4번 더 쳤어요(줄 그음) » | regarde: les cases avec X rouges ; j'ai compris par l'image, pas par la phrase
  - ecran: 41 | cherchais: le nom du pad dont je vois la grille (titre « 스텝: … » sorti en haut) | regarde: le contour bleu du pad 심벌 à gauche
abandon: jamais
incidents-banc:
  - geste: 64 | attente: 0 | vu: fin du tuto « 거의 다 됐어요! 10단계 남았어요 » : les étapes restantes sont dans les chapitres 1-5 que le banc a construits à ma place (amenée), pas un oubli de ma part
defauts:
  - geste: 20 | ecran.vu: 20 | cible: bouton « + C · 인트로 2 » | geste.type: touche
    attendu: après « + B · 인트로 », les autres boutons « + » restent où ils étaient pour taper la suite dans l'ordre | obtenu: toute la liste descend (la frise apparaît au-dessus) ; mon doigt tape dans le vide, puis le tap suivant ajoute « A · 그루브 » par erreur
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 44 | cible: bouton ▶ «  내 곡 듣기 » | geste.type: touche
    attendu: pendant que mon morceau joue, le bouton devient « 정지 » à la même place (comme « 내 결과 듣기 » → « ■ 정지 » au chapitre 6) | obtenu: le bouton disparaît, « 예시 듣기 » glisse à sa place ; en voulant arrêter, j'ai lancé l'exemple
    symptome: perdu | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: texte « 내 차례: ““인트로 2” 파트 만들기”를 누르세요. » | geste.type: touche
    attendu: un seul jeu de guillemets autour du nom du bouton | obtenu: deux guillemets ouvrants collés « ““ » ; pareil à chaque étape des chapitres 6 et 8 (드롭, 브레이크, 빌드업, 끝, “빌드업” 파트로 가기)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 34 | banc?: non | promesse: -
  - geste: 34 | ecran.vu: 35 | cible: texte « 여기 4번 더 쳤어요(줄 그음) » | geste.type: touche
    attendu: une phrase qui dit d'enlever 4 coups (« 지울 히트가 4개 남았어요 ») | obtenu: « ici tu as tapé 4 fois de plus (barré) » : on dirait que j'ai fait une faute, alors que c'est la consigne du tuto
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: bouton ▶ «  예시 듣기 » @ 558,943 | geste.type: touche
    attendu: après chaque partie créée, « 내 결과 듣기 » pour écouter ce que je viens de faire (présent aux étapes 2, 3, 5, 6) | obtenu: absent après « 인트로 » et « 브레이크 » : seul « 예시 듣기 » reste
    symptome: rien | gravite: finition | revu: oui: ecran 2 | banc?: non | promesse: -
mots-faux:
  - ecran: 35 | vu: 4번 더 쳤어요 | attendu: 지울 히트 4개
```

fin-tranche: non (dernier step=share, done=21 ; attendu Bravo done=31 ; chapitres 6-11 cochés, 10 étapes restantes dans les chapitres 1-5 de l amenée)
