siege: b | profil: DJ club anglais (rejeu tour 3) | lang: en | device: phone | font: 100 | mode: pro
mission: exports | buts atteints: 3 sur 3
hesitations:
  - ecran: 10 | cherchais: la liste de mes exports | regarde: onglet More, puis Mix ; « My exports » n'apparaît qu'en bas de Mix après défilement (écran 13)
  - ecran: 65 | cherchais: pourquoi la feuille Share dit 8 bars / 0:16 / WAV alors que Mix est réglé 16 bars + FLAC | regarde: Mix › Advanced puis la feuille Share
  - ecran: 51 | cherchais: les cibles Apple Music (−16) et Free annoncées par l'aide | regarde: Target loudness puis Advanced, introuvables
promesse: Choisir club/streaming là où on exporte ; liste de ses exports ; rapport honnête ; ancienne version jamais proposée | tenue: en partie | pourquoi: club/streaming au bon endroit, liste et bilan présents, mais fausse alerte « song has changed » juste après le 1er export et la feuille Share ignore longueur/format choisis dans Mix
surprise: un export Mix lancé puis doublé par la feuille Share s'est fini en silence (fichier streaming dans la liste, jamais de bilan affiché)
abandon: jamais
defauts:
  - ecran: 12 | cible: « The song has changed since this export: export again to send the new version. » | geste: touche
    attendu: juste après l'export Club (rien modifié), « Ready to share » | obtenu: alerte « song has changed » ; Undo s'active et annule le choix Club fait avant l'export ; reste après Redo (écran 19)
    symptome: promesse | gravite: grave | bloque: non
  - ecran: 65 | cible: feuille « Ready to share? » (« your part goes out as a loop (8 bars) », 0:16, WAV) | geste: touche
    attendu: même longueur (16 bars) et format (FLAC) que réglés dans Mix, comme pour la cible « same setting as in Mix » | obtenu: exporte 8 bars WAV 16 s (écran 69) ; Mix affiche ensuite « Export 16 bars » à côté d'un bilan « Length: 16 s, WAV »
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 51 | cible: aide « Target loudness » ? | geste: touche
    attendu: les cibles citées existent | obtenu: l'aide cite « Apple Music (−16) and Free », absentes de Mix, Advanced et Share
    symptome: promesse | gravite: moyen | bloque: non
  - ecran: 30 | cible: « −8.0 LUFS integrated » | geste: touche
    attendu: même mesure que l'écran promet | obtenu: Club est dit « measured on the loudest passage » mais le bilan affiche « integrated » (sur tout le morceau)
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 48 | cible: liste « My exports » | geste: touche
    attendu: savoir quel fichier correspond à la version actuelle (ancienne version signalée) | obtenu: tous « Send » sans marque, noms « loop-club » identiques pour 8 et 16 bars, taille « 1 KB » pour un WAV 24-bit de 16 s
    symptome: promesse | gravite: moyen | bloque: non
  - ecran: 26 | cible: « Share my song » pendant « Making the sound · 71% » dans Mix | geste: touche
    attendu: attendre l'export en cours ou le signaler | obtenu: la feuille Share s'ouvre et lance un 2e export ; le 1er (streaming) finit sans bilan ni message
    symptome: perdu | gravite: mineur | bloque: non
  - ecran: 46 | cible: ? « What is “My exports”? » | geste: touche
    attendu: une explication (où sont les fichiers, renvoyer, supprimer) | obtenu: répète seulement le sous-titre « In Music › BenStudio on this phone. Newest first. »
    symptome: sans-aide | gravite: mineur | bloque: non
  - ecran: 15 | cible: « Share the song · 1 » | geste: touche
    attendu: nom du fichier envoyé depuis la liste | obtenu: titre « · 1 » au lieu du nom (alors que le Share du bilan affiche le nom, écran 63)
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 61 | cible: « Length: 33 s » | geste: touche
    attendu: 16 bars à 120 BPM = 32 s | obtenu: 33 s affiché
    symptome: incompris | gravite: mineur | bloque: non
