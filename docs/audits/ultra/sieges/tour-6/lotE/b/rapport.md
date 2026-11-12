siege: b | profil: t6-Eb producteur-nl-petit-tel | lang: nl | device: phone360 | font: 100 | mode: pro
mission: exports | buts: spotify, club, retrouve
promesse: id=t6-Eb-1 | texte: Streaming · −14 LUFS → Mijn-eerste-beat-loop-streaming-20261112-213547.m4a | ecran: 16 | tenue: oui | pourquoi: export M4A « streaming » fait (écran 7), présent dans Mijn bestanden (écran 16)
promesse: id=t6-Eb-2 | texte: Club · −8 LUFS → Mijn-eerste-beat-loop-club-20261112-213615.wav | ecran: 16 | tenue: oui | pourquoi: export WAV « club » fait (écran 15), présent dans Mijn bestanden (écran 16)
promesse: id=t6-Eb-3 | texte: 0:16 een deel als loop / 0:32 een deel als loop | ecran: 15 | tenue: oui | pourquoi: annoncé 0:16 → « M4A, 48 kHz, 256 kb/s · 16 s » (écran 7), annoncé 0:32 → « WAV, 48 kHz, 24 bits · 32 s » (écran 15)
promesse: id=t6-Eb-4 | texte: Luid, zoals in een club (−8 LUFS), gemeten op het luidste stuk. … [coupé] | ecran: 11 | tenue: non | pourquoi: tout est en néerlandais, mais le texte d'aide Club est coupé et « Ongedaan… » est tronqué dans la barre du bas
oeil-neuf: q1 | ecran: 24 | image: 24 | cartes Meer, onglets, barre Ongedaan/Opnieuw/het nummer/Tempo/Delen : tout ce qui semble touchable l'est ; deux ↶ et deux icônes partager (haut et bas) font la même chose
oeil-neuf: q6 | ecran: 24 | image: 24 | pas de preuve des exports ici : aucune entrée « Mijn bestanden » dans Meer, il faut repasser par Delen pour retrouver mes fichiers
oeil-neuf: q9 | ecran: 24 | image: 24 | « Ongedaan… » coupé sous l'icône ↶ ; carte Instellingen coupée par le bord (la liste continue, sans signe visible)
hesitations:
  - ecran: 24 | cherchais: mes exports dans le menu Meer | regarde: pas d'entrée Mijn bestanden, seulement via Delen › Mijn bestanden ou le bouton après export
  - ecran: 16 | cherchais: lequel des deux fichiers est le WAV | regarde: noms coupés par « … » avant l'extension, je devine par « club »/« streaming »
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun outil hors human-act.py (Read seulement sur brief, consignes et mes images de siège)
defauts:
  - geste: 11 | ecran.vu: 11 | cible: texte « Luid, zoals in een club (−8 LUFS), gemeten op het luidste stuk. De uitgang gaat zacht omhoog om daar te komen; de export komt zo dichtbij als het kan zonder het geluid te pletten. Dezelfde instelling als in Mix. » [coupé] | geste.type: touche
    attendu: l'explication Club entière (4 lignes) | obtenu: coupée après « zo dichtbij als het kan », le bouton M4A recouvre la suite (image écran 11)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: t6-Eb-4
  - geste: 16 | ecran.vu: 16 | cible: texte « Mijn-eerste-beat-loop-club-20261112-213615.wav » | geste.type: touche
    attendu: nom de fichier lisible avec son format (.wav / .m4a) | obtenu: « Mijn-eerste-beat-loop-club-20261112-21361… », extension et format cachés, ni durée affichée
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 18 | banc?: non | promesse: t6-Eb-4
  - geste: 8 | ecran.vu: 24 | cible: icône ↶ « Ongedaan maken » @ 67,954 | geste.type: touche
    attendu: « Ongedaan maken » lisible | obtenu: « Ongedaan… » tronqué sous l'icône (petit téléphone, police 100 %)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 16 | banc?: non | promesse: t6-Eb-4
  - geste: 17 | ecran.vu: 17 | cible: texte « Opnieuw versturen » @ 388,564 | geste.type: touche
    attendu: renvoyer UN fichier (le WAV club) | obtenu: feuille « Nummer delen · 2 » (2 éléments ?), alors que la ligne streaming donne « · 1 »
    symptome: incompris
    gravite: finition | revu: oui: ecran 21 | banc?: fichier | promesse: -
  - geste: 24 | ecran.vu: 24 | cible: onglet ⋯ « Meer » [sélectionné] | geste.type: touche
    attendu: un accès à Mijn bestanden (mes exports) dans Meer | obtenu: Start, Mijn projecten, Hulp, Gids, Muziekwoorden, Instellingen : pas de Mijn bestanden
    symptome: sans-nom
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
