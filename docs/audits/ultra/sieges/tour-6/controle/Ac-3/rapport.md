code testé : 359304932c46547f9f53f076e7ba2134957186cb
joué par : sous-agent neuf
siege: a | profil: t6-Ac lyceen-envoie-a-un-ami | lang: fr | device: phone | font: 100 | mode: simple
mission: exports | buts: spotify, club, retrouve
promesse: id=t6-Ac-1 | texte: Partager | ecran: 12 | tenue: oui | pourquoi: depuis le studio, onglet Partager puis bouton Partager puis l'appli : 3 gestes, fichier parti (écran 12)
promesse: id=t6-Ac-2 | texte: Mes fichiers Retrouver et renvoyer : M4A, WAV, vidéo, .zip | ecran: 18 | tenue: oui | pourquoi: en Mode simple, l'onglet Partager et « Voir mes fichiers » ouvrent la liste (elle s'appelle « Mes fichiers », pas « Mes exports »)
promesse: id=t6-Ac-3 | texte: Trap-20261113-171050.pocketstudio.zip | ecran: 32 | tenue: oui | pourquoi: le .zip envoyé depuis Mes projets est listé (écrans 32 et 39) ; une fois la liste est restée vide (écran 26, fiche ci-dessous)
promesse: id=t6-Ac-4 | texte: Durée : 2 min 58 s | ecran: 12 | tenue: oui | pourquoi: 2:58 annoncé avant (écran 8), 2 min 58 s dans le bilan (écrans 12 et 17)
oeil-neuf: q1 | ecran: 49 | image: 49 | tout ce qui a un fond ou un contour se touche ; « Mes fichiers » ressemble à une carte et se touche bien ; rien de trompeur vu
oeil-neuf: q6 | ecran: 49 | image: 49 | après l'export, la preuve est le bilan « C'est prêt » (volume, durée) puis « Mes fichiers » ; ici l'écran ne garde aucune trace du dernier envoi
oeil-neuf: q9 | ecran: 49 | image: 49 | rien de coupé ; seul l'onglet « Mon morceau » passe sur deux lignes, les voisins sur une
hesitations:
  - ecran: 8 | cherchais: « Mes exports » | regarde: la carte s'appelle « Mes fichiers », je l'ai trouvée grâce à « .zip » dans sa description
  - ecran: 26 | cherchais: mes deux morceaux et le .zip | regarde: liste vide, j'ai cru que rien n'était gardé
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: aucun outil hors human-act.py, sauf lecture de mon journal.txt et de mes images (dossier du siège)
defauts:
  - geste: 25 | ecran.vu: 26 | cible: texte « Voir mes fichiers » | geste.type: touche
    attendu: la liste avec les deux .m4a et le .zip qu'on vient d'envoyer | obtenu: « Mes fichiers » vide (seulement le texte d'explication), même après 5 s ; refait par le même chemin écran 39 : liste complète
    symptome: perdu | gravite: finition | revu: oui: ecran 39 | banc?: temps | promesse: t6-Ac-3
  - geste: 43 | ecran.vu: 44 | cible: texte « détails » | geste.type: touche
    attendu: phrase du volume entière, lien « détails » et « ? » après elle | obtenu: en Pro, « détails ? » s'insère au milieu de la phrase : « comme la musique de détails ? club » (image 45)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 45 | banc?: non | promesse: -
defauts-avant:
E1: corrigé (écran 12) streaming = touche « Streaming » + « Partager » (2 gestes, puis le choix de l'appli), club pareil (écran 17) ; le bilan donne « Volume · −14,0 » et « Volume · −8,0 »
E3: autre (la ligne s'appelle maintenant « Mes fichiers / Retrouver et renvoyer : M4A, WAV, vidéo, .zip » avec son « ? » : entière et lisible en Simple écran 8 et en Pro écran 45 ; pas de ligne « Autres fichiers » vue)
E4: corrigé (écran 32) les noms passent à la ligne aux tirets ou avant « .m4a »/« .zip », jamais au milieu d'un mot ni d'un nombre (le banc note quand même « .pocketstudio / .zip » sur deux lignes)
E6: corrigé (écran 23) « Envoyer le projet (.zip) »
N3: corrigé (écran 44) « Fort, comme en club (−8 LUFS), mesuré sur le passage le plus fort… »
