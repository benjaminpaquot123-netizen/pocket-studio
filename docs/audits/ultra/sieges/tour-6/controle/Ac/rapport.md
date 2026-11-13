code testé : b3617da2
joué par : sous-agent neuf
siege: a | profil: t6-Ac lyceen-envoie-a-un-ami | lang: fr | device: phone | font: 100 | mode: simple
mission: exports | buts: spotify, club, retrouve
promesse: id=t6-Ac-1 | texte: Partager un morceau se fait en 3 gestes au plus depuis le studio | ecran: 10 | tenue: oui | pourquoi: onglet « Partager » (#6), « Partager » (#7), « WhatsApp » (#9) : 3 gestes, « C'est prêt » écran 10
promesse: id=t6-Ac-2 | texte: Mes fichiers Retrouver et renvoyer : M4A, WAV, vidéo, .zip | ecran: 42 | tenue: oui | pourquoi: atteint en Mode simple depuis l'onglet Partager (#41) ; l'appli l'appelle « Mes fichiers », pas « Mes exports »
promesse: id=t6-Ac-3 | texte: Trap-20261113-122439.pocketstudio.zip | ecran: 42 | tenue: oui | pourquoi: le .zip envoyé à l'écran 32 est en tête de « Mes fichiers » (« projet · 13 nov. 2026 12:24 »)
promesse: id=t6-Ac-4 | texte: Durée : 2 min 58 s | ecran: 10 | tenue: oui | pourquoi: annoncé « 2:58 » écran 7, fichiers M4A, streaming et club tous « 2 min 58 s » (écrans 10, 20, 24, 48)
oeil-neuf: q1 | ecran: 49 | image: 49 | tout ce qui a un contour se touche (chips, Partager, Mes fichiers, Écouter, onglets) ; la ligne « Autres fichiers » à moitié cachée sous les chips M4A/WAV ressemble à du texte délavé, pas à un bouton
oeil-neuf: q6 | ecran: 49 | image: 49 | après l'envoi, la preuve n'est que dans la fenêtre « C'est prêt » ; ici rien ne rappelle le dernier envoi, il faut ouvrir « Mes fichiers » (bien visible) pour le retrouver
oeil-neuf: q9 | ecran: 49 | image: 49 | « Autres fichiers » est recouvert par la rangée M4A/WAV : sa 2e ligne et son « ? » sont coupés
mix: écran 13, vu, rien à juger
hesitations:
  - ecran: 14 | cherchais: un fichier « streaming » ou « club » en Mode simple | regarde: touché la ligne « Niveau : très fort » (rien) et le Mix ; trouvé seulement via « Autres fichiers (passe en mode Pro) »
  - ecran: 29 | cherchais: comment envoyer le projet en .zip | regarde: menu Plus, puis Mes projets, puis le ⋮ du projet : « Sauvegarder le morceau en .zip »
  - ecran: 37 | cherchais: « Mes exports » sur l'accueil | regarde: rien sur l'accueil ni dans Plus ; seulement « Mes fichiers » dans l'onglet Partager
abandon: jamais
incidents-banc:
defauts:
  - geste: 6 | ecran.vu: 7 | cible: bouton (notes sur une pile) › « Autres fichiers Piste par piste, MIDI, FLAC… (passe en mode Pro) » [coupé] | geste.type: touche
    attendu: la ligne « Autres fichiers » entière et lisible | obtenu: recouverte par les chips « M4A · pour partager » / « WAV · qualité max », 2e ligne et « ? » coupés (images 7 et 49)
    symptome: cache
    gravite: finition | revu: oui: ecran 49 | banc?: non | promesse: -
  - geste: 16 | ecran.vu: 18 | cible: texte « Le morceau a changé depuis cet export : exporte à nouveau pour envoyer la nouvelle version. » | geste.type: touche
    attendu: aucun avertissement, je n'ai rien changé au morceau, seulement choisi « Streaming » | obtenu: message qui dit que le morceau a changé
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 47 | cible: texte « Niveau : très fort (les plateformes le baisseront) » | geste.type: touche
    attendu: en Simple, le Partager dit quel volume sera utilisé | obtenu: le fichier sort « Trap-morceau-club-… .m4a » (réglage Club pris en Pro), rien ne l'annonce sur l'écran Simple
    symptome: sans-aide
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 42 | ecran.vu: 43 | cible: texte « Partager le morceau · 3 » | geste.type: touche
    attendu: le nom du fichier renvoyé (Trap-morceau-club-….wav) | obtenu: titre « Partager le morceau · 3 », et l'envoi annonce « Ton morceau « 3 » part avec Gmail »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: fichier | promesse: -
  - geste: 31 | ecran.vu: 32 | cible: texte « Sauvegarder le morceau en .zip » | geste.type: touche
    attendu: le mot dit qu'on envoie le projet (on choisit ensuite WhatsApp, Gmail…) | obtenu: « Sauvegarder » ouvre un choix d'appli pour envoyer ; fichier nommé « .pocketstudio.zip » alors que l'appli s'appelle BenStudio
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ac-3
  - geste: 34 | ecran.vu: 35 | cible: icône ← @ 54,54 | geste.type: touche
    attendu: retour au studio d'où j'avais ouvert Plus › Mes projets | obtenu: retour à l'accueil « Que veux-tu faire ? », morceau arrêté
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 42 | cible: texte « Trap-morceau-streaming-20261113-122406.wav » | geste.type: touche
    attendu: nom de fichier coupé entre deux mots | obtenu: coupé au milieu du nombre (« …-12 / 2406.wav »), tous les noms passent sur deux lignes
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 42 | cible: texte « 13 nov. 2026 12:24 · 1 Ko » | geste.type: touche
    attendu: la même taille que celle annoncée à l'envoi (34230,9 Ko) | obtenu: chaque fichier affiche « 1 Ko »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: son | promesse: -
mots-faux:
  - ecran: 42 | vu: Mes fichiers | attendu: Mes exports

## Contrôle après corrections (ajouté par Testeur 3, hors gabarit testeur)
lot joué : controle/lotAc/a (le banc refuse un dossier qui ne s'appelle pas « lot… ») ; selftest 176 verts, 0 rouge ; buts 3/3 (spotify, club, retrouve) en 44 gestes
defauts-avant (tour 6, lotA/c/rapport.md) :
  - « Mes fichiers » vide après 3 exports | corrigé | les fichiers et le .zip sont listés (écran 42)
  - Simple annonce « streaming (−14) », Pro dit « Auto (Trap) −8,5 » | autre | plus de contradiction vue, mais le Simple n'annonce plus le volume du fichier sorti en « club » (#45)
  - « Le morceau a changé depuis cet export » sans changement | encore là | #16, écran 18
  - « Autres fichiers » recouvert par les chips M4A/WAV | encore là | #6, images 7 et 49
  - en Pro, WAV choisi au lieu du M4A du Simple | pas revu | non commenté (Mes fichiers montre un streaming .wav et un club .m4a)
  - noms de fichiers coupés au milieu d'un mot | encore là | #41 (« …-12 / 2406.wav »)
  - « Annuler » coupé sous la barre Guide pendant la fabrication | pas revu | non signalé
compte : défauts d'avant corrigés 1/7 (3 encore là, 1 autre, 2 pas revus) ; neufs 4 (#42 titre « · 3 », #31 « Sauvegarder » qui envoie, #34 retour vers l'accueil, #41 taille « 1 Ko ») dont 0 bloquant / 0 grave
