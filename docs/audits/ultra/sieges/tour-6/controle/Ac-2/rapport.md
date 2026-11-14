code testé : d946d916
joué par : sous-agent neuf
siege: a | profil: t6-Ac lyceen-envoie-a-un-ami | lang: fr | device: phone | font: 100 | mode: simple
mission: exports | buts: spotify, club, retrouve
promesse: id=t6-Ac-1 | texte: Fini ? Partage ton morceau | ecran: 11 | tenue: oui | pourquoi: depuis le studio, onglet « Partager » puis bouton « Partager » = 2 gestes jusqu'au choix de l'appli (écran 8 puis 11)
promesse: id=t6-Ac-2 | texte: Mes fichiers Retrouver et renvoyer : M4A, WAV, vidéo, .zip | ecran: 19 | tenue: oui | pourquoi: atteint en Mode simple depuis l'onglet Partager ou « Voir mes fichiers » (l'appli dit « Mes fichiers », pas « Mes exports »)
promesse: id=t6-Ac-3 | texte: Trap-20261113-144539.pocketstudio.zip | ecran: 27 | tenue: oui | pourquoi: le .zip sauvegardé (Mes projets › ⋮ › Sauvegarder le morceau en .zip) est en tête de « Mes fichiers »
promesse: id=t6-Ac-4 | texte: Durée : 2 min 58 s | ecran: 18 | tenue: oui | pourquoi: 2:58 annoncé avant (écran 8) = « Durée : 2 min 58 s » après export M4A (écran 12) et WAV (écran 18)
oeil-neuf: q1 | ecran: 47 | image: 47 | tout ce qui a l'air touchable l'est (champ, ✕, Écouter, puces volume et format, Partager, carte Mes fichiers, ?, onglets) ; rien de trompeur
oeil-neuf: q6 | ecran: 47 | image: 47 | aucune trace ici des deux envois déjà faits ; la preuve n'est que dans la carte « Mes fichiers » à ouvrir
oeil-neuf: q9 | ecran: 47 | image: 47 | rien de coupé ni de chevauché à 100 %
mix: vu, rien à juger (écran 7)
hesitations:
  - ecran: 22 | cherchais: où envoyer le projet en .zip | regarde: rien dans « Plus » ; trouvé dans Mes projets › ⋮ › « Sauvegarder le morceau en .zip »
  - ecran: 28 | cherchais: quel fichier je renvoie | regarde: le titre dit « Partager le morceau · 1 » au lieu du nom du fichier
abandon: jamais
incidents-banc:
defauts:
  - geste: 39 | ecran.vu: 40 | cible: Autres fichiers Piste par piste, MIDI, FLAC… dans Mix | geste.type: touche
    attendu: ligne « Autres fichiers » entière avec ses deux lignes et son « ? » | obtenu: en Pro, les puces « M4A · pour partager » / « WAV · qualité max » recouvrent la 2e ligne et le « ? »
    symptome: cache
    gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 19 | cible: Trap-morceau-club-20261113-144507.wav | geste.type: touche
    attendu: nom du fichier lisible ou coupé en fin | obtenu: « Trap-morceau-clu…61113-144507.wav » coupé au milieu du mot « club » et de la date (idem « str… », et « Trap-20261113-14....pocketstudio.zip » écran 27)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 27 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 19 | cible: 13 nov. 2026 14:45 · 1 Ko | geste.type: touche
    attendu: la vraie taille du fichier | obtenu: « 1 Ko » pour le WAV, le M4A et le .zip, alors que l'envoi annonçait 68461,9 Ko
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 27 | banc?: son | promesse: -
  - geste: 39 | ecran.vu: 40 | cible: Volume : −13,0, comme la musique en streaming (−14) | geste.type: touche
    attendu: même niveau qu'en Simple pour le même réglage Club (−8,0, écran 47) | obtenu: en Pro « −13,0, comme la musique en streaming » avec « Club · −8 LUFS » sélectionné
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 43 | banc?: son | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: Partager le morceau · 1 | geste.type: touche
    attendu: le nom du fichier renvoyé (Trap-morceau-streaming-….m4a) | obtenu: « Partager le morceau · 1 », et le partage part sous le nom « 1 »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: fichier | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: Mes projets | geste.type: touche
    attendu: la flèche ← de Mes projets (ouvert depuis le studio) me ramène au studio | obtenu: écran d'accueil « Que veux-tu faire ? », le morceau s'arrête
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
defauts-avant:
  - E1: corrigé (écran 8) : puces « Streaming » et « Club » à 1 geste dans l'onglet Partager en Simple ; après l'export, l'écran dit « Niveau : à la cible (Streaming) » / « (Club) » (écrans 12 et 18), sans chiffre en LUFS dans le bilan (le chiffre −14,0 / −8,0 est sur l'onglet avant export)
  - E3: encore là (écran 40) : en Pro, « Autres fichiers » est recouvert par les puces M4A / WAV (2e ligne et « ? » cachés) ; en Simple la ligne n'existe pas
  - E4: encore là (écran 19) : « Trap-morceau-clu…61113-144507.wav », « Trap-morceau-str…1113-144445.m4a », « Trap-20261113-14....pocketstudio.zip » (écran 27)
  - E6: autre (« Sauvegarder le morceau en .zip » ouvre la liste d'applis d'envoi ; après « Files » : « Projet sauvegardé », copie dans Documents › BenStudio, listé dans Mes fichiers écran 27 ; le bouton qui envoie s'appelle « Partager », et « Renvoyer » dans Mes fichiers)
  - E2: pas atteint (aucun message « Le morceau a changé depuis cet export » vu)
  - E5: encore là (écran 19) : « 1 Ko » sur les trois fichiers
  - E7: autre (← de Mes projets, ouvert après l'export du .zip, mène à l'accueil écran 31 ; le Retour depuis « Mes fichiers » de l'onglet Partager reste bien dans le studio écran 35)
