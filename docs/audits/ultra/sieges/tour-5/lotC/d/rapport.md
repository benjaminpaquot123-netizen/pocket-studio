siege: d | profil: t5-Cd musicien-qui-echange | lang: fr | device: phone | font: 100 | mode: simple
mission: exports | buts: spotify, club, retrouve
promesse: id=t5-Cd-1 | texte: Reprendre ton morceau sur un autre appareil : « Partager » › « Envoyer le projet » (un .zip avec tous ses sons) / « Autres exports Piste par piste, MIDI, FLAC… dans Mix » | ecran: 30 | tenue: oui | pourquoi: le .zip part bien depuis Partager › Envoyer le projet (écran 23), le MIDI est dans Mix, mais caché sous « Avancé » replié (écran 26)
promesse: id=t5-Cd-2 | texte: Mes exports : retrouver et renvoyer un fichier | ecran: 69 | tenue: non | pourquoi: le .mid, le .wav club et le .m4a streaming y sont, mais aucun des 3 .zip du projet envoyés n'y apparaît
promesse: id=t5-Cd-3 | texte: Cet appareil ne gère pas le MIDI. / Aucun contrôleur branché. Branche ton clavier en USB : il est reconnu tout seul. | ecran: 47 | tenue: non | pourquoi: les deux phrases se suivent sur la même page et se contredisent
promesse: id=- | texte: C'est prêt : Démo-Acid-house-morceau-streaming-20261105-211501.m4a, une copie est aussi dans Musique › BenStudio. | ecran: 10 | tenue: oui | pourquoi: le fichier est bien listé ensuite dans Mes exports (écran 69)
hesitations:
  - ecran: 25 | cherchais: le MIDI annoncé « dans Mix » | regarde: Mix ne montre aucun MIDI tant qu'on n'ouvre pas « Avancé » (trouvé écran 26)
  - ecran: 32 | cherchais: « Mes exports » | regarde: Mes projets, Accueil, Plus, Réglages, Ma bibliothèque, Remix : rien ; trouvé seulement tout en bas de la feuille Partager, sous la ligne de flottaison (écran 68)
  - ecran: 70 | cherchais: quel fichier je renvoie | regarde: le menu dit « Partager le morceau · 2 » au lieu du nom du fichier
abandon: jamais
incidents-banc:
  - aucun
defauts:
  - geste: 46 | ecran.vu: 47 | cible: Cet appareil ne gère pas le MIDI. | geste.type: touche
    attendu: une seule info claire (le MIDI marche ou pas) | obtenu: « ne gère pas le MIDI » juste au-dessus de « Branche ton clavier en USB : il est reconnu tout seul. »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 49 | banc?: non | promesse: t5-Cd-3
  - geste: 50 | ecran.vu: 51 | cible: ↓ la suite plus bas (glisse vers le haut pour la voir) | geste.type: glisse
    attendu: l'onglet Envoyer (mode Simple) défile et montre « Mes exports » comme en Pro | obtenu: rien ne bouge (3 glisses), « Mes exports » est inatteignable en Simple ; il faut passer en Pro (pastille, Pro, Partager, glisse, Mes exports)
    symptome: cache
    gravite: contourne-en-5-gestes | revu: oui: ecran 102 | banc?: non | promesse: t5-Cd-2
  - geste: 49 | ecran.vu: 52 | cible: Envoyer le projet Pour le reprendre sur un autre appareil : un fichier .zip avec tous ses sons, à ouvrir là-bas dans Mes projets › Importer. | geste.type: touche
    attendu: la carte « Envoyer le projet » visible dans l'onglet Envoyer (Simple) | obtenu: bande vide entre « Autres exports » et M4A/WAV ; le bouton existe (un toucher à l'aveugle lance le .zip) mais on ne le voit pas
    symptome: cache
    gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: t5-Cd-1
  - geste: 69 | ecran.vu: 70 | cible: Partager le morceau · 2 | geste.type: touche
    attendu: le nom du fichier renvoyé (ex. Démo-Acid-house-morceau-club-….wav) comme lors du premier envoi | obtenu: « · 2 » puis « Ton morceau « 2 » part avec WhatsApp » ; « · 3 » pour le .mid (et « morceau » pour un MIDI)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 68 | ecran.vu: 69 | cible: Dans Musique › BenStudio sur ce téléphone. Les plus récents d'abord. | geste.type: touche
    attendu: les .zip du projet envoyés (3 fois) listés pour les renvoyer | obtenu: seulement .mid, .wav, .m4a ; pour renvoyer le projet il faut repasser par Mes projets › Options › Envoyer / sauvegarder (.zip)
    symptome: perdu
    gravite: contourne-en-3-gestes | revu: oui: ecran 96 | banc?: non | promesse: t5-Cd-2
  - geste: 83 | ecran.vu: 84 | cible: Dans Musique › BenStudio sur ce téléphone. Les plus récents d'abord. | geste.type: touche
    attendu: la liste des 3 exports | obtenu: liste vide, sans aucun message, toujours vide après 5 s (ouvert via Mes projets › Partager mon morceau juste après un envoi .zip) ; non reproduit ensuite (écran 96)
    symptome: lent
    gravite: contourne-en-3-gestes | revu: oui: ecran 96 | banc?: temps | promesse: t5-Cd-2
  - geste: 31 | ecran.vu: 32 | cible: Mes projets | geste.type: touche
    attendu: « Mes exports » accessible aussi depuis Mes projets ou Plus | obtenu: seulement en bas de la feuille Partager, sous le pli ; une trentaine de gestes pour le trouver
    symptome: cache
    gravite: finition | revu: oui: ecran 61 | banc?: non | promesse: t5-Cd-2
mots-faux:
  - ecran: 70 | vu: Partager le morceau · 2 | attendu: Partager Démo-Acid-house-morceau-club-20261105-211523.wav
