code testé : a4b719dfddb2bdf446ce5eb44f8836cd41b62cf4
joué par : sous-agent neuf
siege: a | profil: t6-Ba gaucher-espagnol-une-main | lang: es | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t6-Ba-1 | texte: « Ce point est trop loin de ton pouce » sur « 2 » (morceau) et « + » de « Empezar desde el compás 1 de la canción » | ecran: 21 | tenue: non | pourquoi: pastilles des morceaux (Frases/Compases) et « + » du compás de départ hors du pouce gauche (#20, #26, #38)
promesse: id=t6-Ba-2 | texte: « ✂ Cortar lo que quiero » (seule carte « Ma chanson préférée » après la séparation) | ecran: 8 | tenue: oui | pourquoi: une seule carte de résultat, un seul bouton plein conseillé dedans
promesse: id=t6-Ba-3 | texte: - | ecran: - | tenue: pas vue | pourquoi: aucun avertissement « el final se cortará » rencontré ; impossible d'amener la fin au bout du morceau à une main (défilement de l'onde ~2 s par geste)
promesse: id=t6-Ba-4 | texte: « bouton ✂ ▾ « Frases » [coupé] » / « (aimant) ▾ « En el tiempo » [coupé] » | ecran: 19 | tenue: non | pourquoi: en mode Frases/Compases les puces de découpe disparaissent entre l'onde et la carte « Marca trozos… » (image 19)
oeil-neuf: q1 | ecran: 56 | image: 56 | touchable vu : ←, ▶ orange, ?, bande d'aperçu, puces, Inicio/Fin/Escuchar, Voz…Resto ; l'onde ne ressemble pas à une zone touchable ; la carte vide sous « Voz » (bord à y 930) cache les boutons de destination
oeil-neuf: q6 | ecran: 56 | image: 56 | la preuve « “Voz 1” está en una nueva pista de audio » est coupée sous « Guía » ; rien n'indique où aller ensuite (pista ? pads ?)
oeil-neuf: q9 | ecran: 56 | image: 56 | mots entiers ; seule la carte du bas (message + destinations) est tranchée par la barre « Guía »
hesitations:
  - ecran: 9 | cherchais: où poser le trozo une fois Inicio/Fin placés | regarde: « Coloca Inicio y Fin… » [coupé] sous « Guía », boutons de destination invisibles sans glisser
  - ecran: 19 | cherchais: comment choisir une frase de voz | regarde: les blocs verts de l'onde, la pastille « 1 » hors du pouce
  - ecran: 7 | cherchais: si la séparation était lente | regarde: 1 seul geste d'attente (#7, attends 60) : séparation rapide
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: outils hors human-act.py : Read sur mes images de siège (screen-*-grille.png) et cat/grep/ls du journal de MON dossier de siège pour les numéros de gestes
defauts:
  - geste: 18 | ecran.vu: 19 | cible: titre « COMP. 2 A 5 » | geste.type: touche
    attendu: toucher un morceau de voix (Frases) le marque comme choisi et active « En los pads » | obtenu: rien ne se passe (la tête de lecture bouge), boutons de destination restent [désactivé] ; idem en Compases (#25, #27, #28)
    symptome: rien
    gravite: contourne-en-3-gestes | revu: oui: ecran 29 | banc?: non | promesse: t6-Ba-1
  - geste: 20 | ecran.vu: 21 | cible: titre « COMP. 2 A 5 » | geste.type: touche
    attendu: la pastille numérotée du morceau (« 2 ») à portée du pouce gauche | obtenu: « Ce point est trop loin de ton pouce » (aussi #26 en Compases)
    symptome: hors-pouce
    gravite: contourne-en-3-gestes | revu: oui: ecran 27 | banc?: non | promesse: t6-Ba-1
  - geste: 16 | ecran.vu: 19 | cible: bouton ✂ ▾ « Frases » [coupé] @ 117,753 | geste.type: touche
    attendu: les puces « Frases » / « En el tiempo » restent visibles sous l'onde | obtenu: cachées entre l'onde et la carte « Marca trozos para ponerlos en un pad o una pista. » (invisibles sur l'image 19)
    symptome: cache
    gravite: finition | revu: oui: ecran 25 | banc?: non | promesse: t6-Ba-4
  - geste: 8 | ecran.vu: 9 | cible: texte « Coloca Inicio y Fin, y luego pon este trozo en un pad o una pista. » [coupé] @ 294,949 | geste.type: touche
    attendu: à l'arrivée sur la découpe, voir « En los pads / En el Live / Guardar / En una pista » | obtenu: carte coupée sous « Guía », il faut glisser (#12) pour les trouver
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 32 | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: bouton ✂ « Un trozo » ▾ @ 131,474 | geste.type: glisse
    attendu: après avoir fait monter la page pour voir les destinations, les outils restent au pouce | obtenu: « Un trozo » remonte hors du pouce (#13 refusé), il faut redescendre (#14)
    symptome: hors-pouce
    gravite: contourne-en-2-gestes | revu: oui: ecran 33 | banc?: non | promesse: t6-Ba-1
  - geste: 33 | ecran.vu: 34 | cible: bouton « En los pads » (quatre carrés) [coupé] @ 96,934 | geste.type: touche
    attendu: après « En los pads », les destinations restent visibles | obtenu: le message « “Voz 1” está en el pad live 1. » pousse les 4 boutons sous « Guía » (image 34)
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 39 | cible: icône + @ 510,549 | geste.type: touche
    attendu: changer « Empezar desde el compás 1 de la canción » à une main gauche | obtenu: « + » hors du pouce, rien ne change
    symptome: hors-pouce
    gravite: finition | revu: non | banc?: non | promesse: t6-Ba-1
  - geste: 51 | ecran.vu: 52 | cible: bouton ✂ «  Cortar la canción » @ 294,903 | geste.type: glisse
    attendu: un seul bouton ✂ pour découper sur la page Remix | obtenu: « ✂ Cortar lo que quiero » et « ✂ Cortar la canción », même forme orange, même icône, l'un au-dessus de l'autre (image 52)
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: bouton « Descargar el modelo (174 MB) » @ 294,903 | geste.type: touche
    attendu: un seul bouton par action | obtenu: « Descargar el modelo (174 MB) » deux fois à l'écran 3, puis « Elegir una canción » deux fois (écrans 6 et 8)
    symptome: incompris
    gravite: finition | revu: oui: ecran 8 | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: bouton ✂ « Un trozo » ▾ @ 131,660 | geste.type: touche
    attendu: le mot « Un trozo » et « 6 tiempos · 0:03.0 » lisibles | obtenu: sur l'image 9 la puce affiche « ✂ … » et « Escuchar el trozo » « … », onde vide ; tout est normal plus tard (image 56)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 56 | banc?: temps | promesse: -
mots-faux:
  - ecran: 53 | vu: ordre « Batería, Bajo, Resto, Voz » | attendu: même ordre que la découpe « Voz, Batería, Bajo, Resto »
defauts-avant:
  - A2 | corrigé (écran 9) : « Voz », « Batería », « Bajo », « Resto » à y 864, touchés au pouce gauche sans glisser (#9, #10) ; mais en mode Frases/Compases ils remontent en haut (y 366, écran 17)
  - A3 | corrigé (écran 8)
  - A5 | corrigé (écran 9) : « Inicio aquí », « Fin aquí », « Escuchar el trozo » entiers au-dessus de « Guía » (seule la ligne de durée s'affiche « … » sur l'image 9)
  - A7 | autre : la puce « Voz » est nettement marquée (fond orange), mais un morceau de voix (Frases/Compases) ne se choisit pas : le toucher ne fait que déplacer la tête de lecture, aucun marquage (écrans 19 à 29)
  - A8 | autre : un seul « Cortar la canción », mais sur la même page un second bouton ✂ identique « Cortar lo que quiero » juste au-dessus (écran 52)
