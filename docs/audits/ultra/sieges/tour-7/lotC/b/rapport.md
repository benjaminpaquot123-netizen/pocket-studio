code testé : 85497e2f97cd8319f0374d48b55913e4797b800e
joué par : sous-agent neuf

siege: b | profil: t7-3b | lang: fr | device: phone | font: 100 | mode: pro
mission: exports | buts: spotify, club, retrouve
oeil-neuf: 9 écrans, 1 graves
promesse: id=t7-3b-1 | texte: « Rien ici pour l'instant. Tout ce que tu partages s'ajoute ici, dans Musique › BenStudio sur cet appareil. » | ecran: 13 | tenue: non | pourquoi: le vide est bien dit, mais aucun signe d'attente vu avant (2 essais, écrans 13 et 15) ; peut-être le banc (temps)
promesse: id=t7-3b-2 | texte: « Niveau : à la cible (Streaming) » … « Fichier M4A : léger, lu par tous les appareils. » | ecran: 20 | tenue: oui | pourquoi: niveau et format dits ; pareil écran 26 « Niveau : à la cible (Club) » / « Fichier WAV : qualité studio. »
promesse: id=t7-3b-3 | texte: « House-morceau-club-20261115-155122.wav » / « House-morceau-streaming-20261115-155052.m4a » | ecran: 27 | tenue: oui | pourquoi: les deux fichiers listés avec .wav et .m4a, revus en Pro écran 34
promesse: id=t7-3b-4 | texte: bouton « Voir les courbes » [coupé] | ecran: 43 | tenue: non | pourquoi: tout est en français, mais en Pro (Mix) les boutons « Voir les courbes » et « Ce qui joue » sont coupés par la barre du Guide (image 43)
hesitations:
  - ecran: 5 | cherchais: « Mes fichiers » dès l'accueil, avant tout morceau | regarde: rien sur l'accueil ; il a fallu créer un morceau
  - ecran: 11 | cherchais: « Mes fichiers » dans le menu ⋮ (Plus) | regarde: absent (Mes projets, Bibliothèque… mais pas mes fichiers) ; trouvé seulement dans l'onglet Partager
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun outil hors human-act.py, sauf lecture des images et du journal de mon dossier de siège
defauts:
  - geste: 12 | ecran.vu: 13 | cible: texte « Rien ici pour l'instant. Tout ce que tu partages s'ajoute ici, dans Musique › BenStudio sur cet appareil. » | geste.type: touche
    attendu: un signe d'attente, puis « Rien ici » | obtenu: « Rien ici » tout de suite, aucun signe d'attente (refait #14, écran 15 : pareil)
    symptome: incompris
    gravite: finition | revu: oui: ecran 15 | banc?: temps | promesse: t7-3b-1
  - geste: 42 | ecran.vu: 43 | cible: bouton « Voir les courbes » [coupé] | geste.type: touche
    attendu: les boutons « Voir les courbes » et « Ce qui joue » lisibles en entier | obtenu: coupés à mi-hauteur par la barre « Guide · Change un son », rien n'indique qu'on peut défiler (image 43)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t7-3b-4
  - geste: 34 | ecran.vu: 35 | cible: texte « Partager le morceau · 2 » | geste.type: touche
    attendu: « Renvoyer » sur le .wav dit quel fichier part (son nom, comme à l'export écran 19) | obtenu: « Partager le morceau · 2 » (et « · 1 » pour le .m4a, écran 37) : impossible de vérifier lequel part
    symptome: incompris
    gravite: finition | revu: oui: ecran 37 | banc?: non | promesse: t7-3b-3
  - geste: 10 | ecran.vu: 11 | cible: icône ⋮ @ 543,42 | geste.type: touche
    attendu: « Mes fichiers » dans le menu Plus, là où sont Mes projets et Ma bibliothèque | obtenu: absent (Simple écran 11, Pro écran 42 aussi) ; seulement dans l'onglet Partager
    symptome: sans-aide
    gravite: contourne-en-1-gestes | revu: oui: ecran 42 | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: texte « C'est prêt : House-morceau-streaming-20261115-155052.m4a, une copie est aussi dans Musique › BenStudio. » | geste.type: retour
    attendu: après avoir fermé le menu de partage, la fenêtre dit que rien n'a été envoyé mais que le fichier est gardé | obtenu: titre « C'est prêt » puis « C'est prêt : … » répété ; rien ne dit que rien n'est parti
    symptome: incompris
    gravite: finition | revu: oui: ecran 26 | banc?: non | promesse: -
  - geste: 32 | ecran.vu: 33 | cible: bouton « Auto (House) · −8 LUFS » | geste.type: touche
    attendu: même feuille d'export en Simple et en Pro (mêmes mots) | obtenu: Simple « Fini ? Partage ton morceau », « Auto (style) », pas de « Désactivée » ; Pro « Prêt à partager ? », « Auto (House) · −8 LUFS », « Désactivée » en plus
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - U22 | pas corrigé (écran 13) : « Rien ici pour l'instant » s'affiche d'un coup, aucun signe d'attente vu (refait écran 15) ; peut-être le banc (temps)
  - U2 | corrigé (écran 26) : Club choisi → « Niveau : à la cible (Club) », fichier « House-morceau-club-….wav » ; Streaming → écran 20 « (Streaming) »
  - U4 / U15 / N7 | corrigé (écran 12) : rangée « M4A · pour partager » / « WAV · qualité max » entière, aucune ligne cachée (Pro : écran 33 pareil)
  - U3 | corrigé (écran 30) : revenu sur Partager via Mon morceau sans rien changer, aucun « a changé »
  - U5 | corrigé (écran 33) : WAV choisi en Simple (écran 23), passé en Pro : « WAV · qualité max » [sélectionné]
  - R14 | corrigé (écran 27) : « House-morceau-club-20261115-155122.wav » et « House-morceau-streaming-20261115-155052.m4a »
  - N15 | corrigé (écran 38) : pas de barre « Terminé » ; « Fermer » sous la liste, aucune ligne cachée (image 38)
