code testé : b3617da2
joué par : sous-agent neuf
siege: a | profil: t6-Ba gaucher-espagnol-une-main | lang: es | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t6-Ba-1 | texte: « Remix » (menu Más) / « Escuchar el trozo 6 tiempos · 0:03.0 » | ecran: 33 | tenue: non | pourquoi: « Remix » du menu Más et le centre de « Escuchar el trozo » refusés « trop loin du pouce » ; chips Voz/Batería en haut aussi
promesse: id=t6-Ba-2 | texte: « Cortar lo que quiero » (seule carte) ; « Recomendado Hacer un tema… » | ecran: 8 | tenue: oui | pourquoi: après la séparation une seule action conseillée (et un seul « Recomendado » écran 38)
promesse: id=t6-Ba-3 | texte: « A compás 1/1 » / « Bucle de 1 comp. » | ecran: 28 | tenue: non | pourquoi: chanson de 0:40 jouée dans un projet d'1 mesure, aucun « la fin será cortada » nulle part (écrans 8, 9, 28, 40)
promesse: id=t6-Ba-4 | texte: « bouton ✂ ▾ « Frases » [coupé] » | ecran: 18 | tenue: non | pourquoi: en mode Frases, la rangée « Frases / En el tiempo » disparaît sous la grande forme d'onde (image 18)
oeil-neuf: q1 | ecran: 42 | image: 42 | Reproducir, 2× Cortar, Abrir el tablero, Inicio, onglets ont l'air touchables ; « Efectos » est un simple mot orange, on devine ; la barre verte (déplaçable) n'a pas l'air d'un bouton
oeil-neuf: q6 | ecran: 42 | image: 42 | preuve : « La canción entra en el compás 1 » + barre verte + « compás 1/20 » en haut ; suite évidente : « Cortar la canción »
oeil-neuf: q9 | ecran: 42 | image: 42 | la puce « Inicio » est coupée par le bouton flottant « Cortar la canción » ; « 0:00 / 0:40 · » finit sur un point orphelin
hesitations:
  - ecran: 13 | cherchais: comment marquer une phrase de la voix en mode Frases | regarde: forme d'onde avec badge « 1 », boutons pads désactivés, aucun outil visible
  - ecran: 32 | cherchais: revenir au Remix | regarde: « Remix » dans Más est trop haut ; je suis passé par Retour ×2 puis l'accueil
  - ecran: 9 | cherchais: Inicio/Fin/Escuchar | regarde: rangée coupée sous « Guía » ; il faut faire glisser la page pour la voir entière
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun ; séparation rapide : 1 geste d'attente pour le modèle (attends 20) + 1 pour la séparation (attends 30), banc?: temps ; aucun outil utilisé hors human-act.py (sauf Read sur les images du banc)
defauts:
  - geste: 32 | ecran.vu: 33 | cible: bouton (disque) « Remix » @ 107,472 | geste.type: touche
    attendu: ouvrir le Remix d'une main gauche | obtenu: « trop loin de ton pouce », la grille Más ne descend pas (glisse #33 sans effet)
    symptome: hors-pouce | gravite: contourne-en-3-gestes | revu: oui: ecran 37 | banc?: non | promesse: t6-Ba-1
  - geste: 23 | ecran.vu: 24 | cible: bouton (casque) « Escuchar el trozo 6 tiempos · 0:03.0 » @ 436,666 | geste.type: touche
    attendu: écouter le bout | obtenu: refusé hors pouce ; marche seulement sur le bord gauche du bouton (#24)
    symptome: hors-pouce | gravite: contourne-en-1-gestes | revu: oui: ecran 25 | banc?: non | promesse: t6-Ba-1
  - geste: 9 | ecran.vu: 10 | cible: bouton « Voz » [sélectionné] @ 69,366 | geste.type: glisse
    attendu: atteindre la rangée Voz/Batería/Bajo/Resto au pouce | obtenu: « trop loin de ton pouce » ; la page glisse mais ces puces partent vers le haut
    symptome: hors-pouce | gravite: finition | revu: non | banc?: non | promesse: t6-Ba-1
  - geste: 17 | ecran.vu: 18 | cible: bouton ✂ ▾ « Frases » [coupé] @ 117,753 | geste.type: touche
    attendu: outils de découpe visibles en mode Frases | obtenu: « Frases » et « En el tiempo » cachés sous la forme d'onde / la carte « Marca trozos » (image 18), touchables à l'aveugle seulement
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 22 | banc?: non | promesse: t6-Ba-4
  - geste: 19 | ecran.vu: 20 | cible: texte « Marca trozos para ponerlos en un pad o una pista. » | geste.type: touche
    attendu: toucher une phrase de voix la marque et active « En los pads » | obtenu: rien ne change (3 essais #14 #15 #19), seul un badge « 1 », boutons toujours [désactivé]
    symptome: rien | gravite: contourne-en-3-gestes | revu: oui: ecran 20 | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: icône (flèches en boucle) « A compás 1/1 » [désactivé] | geste.type: touche
    attendu: avertissement « la fin será cortada » (chanson 0:40, projet 1 mesure) | obtenu: aucun message ; le banc dit « toutes ses parties sont coupées »
    symptome: sans-aide | gravite: finition | revu: oui: ecran 41 | banc?: non | promesse: t6-Ba-3
  - geste: 41 | ecran.vu: 42 | cible: bouton ✂ «  Cortar la canción » @ 294,903 | geste.type: touche
    attendu: un seul bouton par action | obtenu: deux « Cortar la canción » sur le même panneau (aussi 2× « Descargar el modelo (174 MB) » écran 3, 2× « Elegir una canción » écran 6)
    symptome: incompris | gravite: finition | revu: oui: ecran 6 | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 42 | cible: bouton « Inicio » [coupé, sélectionné] @ 95,832 | geste.type: touche
    attendu: puce « Inicio » entière | obtenu: coupée par le bouton flottant « Cortar la canción »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 26 | cible: bouton « En los pads » (quatre carrés) [coupé] @ 96,934 | geste.type: touche
    attendu: les 4 boutons restent visibles après « “Voz 1” está en el pad live 1. » | obtenu: le message les pousse sous « Guía », on n'en voit que le haut (image 26)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 26 | vu: en el pad live 1 | attendu: en el pad 1 del Live
defauts-avant:
  - A1 : corrigé
  - A2 : autre (la page glisse maintenant, mais les puces Voz/Batería/Bajo/Resto restent hors pouce)
  - A3 : autre (✂ « Cortar lo que quiero » se touche au pouce ; sa phrase d'aide « Elige trozos… » [coupé] reste sous le flottant « Elegir una canción »)
  - A4 : encore là
  - A5 : autre (coupée à l'arrivée sous « Guía », entière après avoir fait glisser la page)
  - A6 : encore là
  - A7 : encore là
  - A8 : encore là
