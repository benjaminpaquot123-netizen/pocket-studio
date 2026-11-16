code testé : 28f1260707fe98b9322b638b5eb0f4db44c66028
joué par : sous-agent neuf
siege: c | profil: t7-2c gauchere-es-pouce-gauche | lang: es | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t7-2c-1 | texte: Simple Para empezar: 5 pestañas, botones grandes y palabras de todos los días. | ecran: 3 | tenue: oui | pourquoi: touchée au pouce gauche (90,950), la barre du haut dit « Simple » ensuite
promesse: id=t7-2c-2 | texte: Voz | ecran: 21 | tenue: non | pourquoi: « Cortar lo que quiero » et les puces se touchent en mode Un trozo (écrans 9, 14), mais en Frases/Compases les puces montent à y=366, hors du pouce
promesse: id=t7-2c-3 | texte: “Voz 1” está en una nueva pista de audio, en el compás 3. | ecran: 24 | tenue: oui | pourquoi: pad (« está en el pad live 1 », écran 17) et piste nommés
promesse: id=t7-2c-4 | texte: Coloca Inicio y Fin, y luego pon este trozo en un pad o una pista. | ecran: 10 | tenue: non | pourquoi: phrase coupée sous la barre Guía ; « pad live 1 » sonne mal en espagnol
oeil-neuf: q1 | ecran: 42 | image: 42 | tout ce qui a un contour se touche ; « MIS PISTAS » et « LA CANCIÓN » avec ⌄ ne ressemblent pas à des boutons pliables
oeil-neuf: q6 | ecran: 42 | image: 42 | la piste « Voz 1 » est la preuve ; mais « Tu canción está vacía » juste dessous fait douter
oeil-neuf: q9 | ecran: 42 | image: 42 | rien de coupé sur cet écran
hesitations:
  - ecran: 4 | cherchais: pourquoi « Elegir una canción » ne répond pas | regarde: le même bouton en bas « + Elegir una canción » marche
  - ecran: 17 | cherchais: où est le « pad live 1 » | regarde: Ritmo n'a pas de pad Voz ; Live est dans Más, hors du pouce
  - ecran: 34 | cherchais: comment allonger le trozo | regarde: les poignées sont hors du pouce
abandon: jamais
incidents-banc:
defauts:
  - geste: 3 | ecran.vu: 4 | cible: bouton « Elegir una canción » @ 294,517 | geste.type: touche
    attendu: le bouton principal se touche du pouce gauche | obtenu: hors de portée ; un doublon plus bas fait la même chose
    symptome: hors-pouce | gravite: contourne-en-1-gestes | revu: oui: ecran 8 | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: bouton « Voz » [sélectionné] @ 69,366 | geste.type: touche
    attendu: en mode Frases les puces restent sous le pouce comme en Un trozo | obtenu: elles montent en haut, hors de portée
    symptome: hors-pouce | gravite: contourne-en-2-gestes | revu: oui: ecran 22 | banc?: non | promesse: t7-2c-2
  - geste: 15 | ecran.vu: 16 | cible: bouton ✂ « Un trozo » ▾ @ 131,474 | geste.type: touche
    attendu: le choix du mode se touche après avoir fait défiler | obtenu: hors du pouce, il faut redescendre
    symptome: hors-pouce | gravite: contourne-en-1-gestes | revu: oui: ecran 19 | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: dessin [état « 0:07.00 »] @ 450,522 | geste.type: glisse
    attendu: tirer la poignée Fin du pouce gauche | obtenu: hors de portée, trozo impossible à allonger à la main
    symptome: hors-pouce | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t7-2c-2
  - geste: 39 | ecran.vu: 41 | cible: bouton (neuf points) « Live: tocar en directo » @ 481,409 | geste.type: touche
    attendu: aller voir le pad live où est ma voix | obtenu: hors du pouce gauche (2 essais)
    symptome: hors-pouce | gravite: contourne-en-2-gestes | revu: oui: ecran 41 | banc?: non | promesse: t7-2c-3
  - geste: 9 | ecran.vu: 10 | cible: texte « Coloca Inicio y Fin, y luego pon este trozo en un pad o una pista. » [coupé] | geste.type: touche
    attendu: la consigne et les boutons « En los pads… » visibles | obtenu: coupés sous la barre Guía, il faut deviner qu'on fait défiler
    symptome: texte-coupe | gravite: contourne-en-1-gestes | revu: oui: ecran 12 | banc?: non | promesse: t7-2c-4
  - geste: 16 | ecran.vu: 17 | cible: bouton « En los pads » (quatre carrés) [coupé] | geste.type: touche
    attendu: les boutons restent en place après le message | obtenu: le message les pousse sous la barre Guía
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: texte « Este fragmento de la pista está en silencio: no se colocó nada. “Voz 2” está en el pad live 2. » | geste.type: touche
    attendu: un message clair | obtenu: « no se colocó nada » puis « está en el pad » dans la même bulle
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: icône ↶ [désactivé] @ 534,660 | geste.type: touche
    attendu: un seul bouton par action | obtenu: deux Deshacer, trois ▶ (528,267 ; « la canción » ; « esta parte »), deux ←
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 42 | cible: texte « Tu canción está vacía: añade tus partes en el orden en que suenan (Intro, Estrofa, Estribillo…). » | geste.type: touche
    attendu: pas « vacía » quand la piste Voz 1 vient d'être posée | obtenu: contredit ce que je viens de faire
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 17 | vu: el pad live 1 | attendu: el pad 1 del Live
defauts-avant:
  - R6 | corrigé (écran 24) : « “Voz 1” está en una nueva pista de audio, en el compás 3. » ; pad : « está en el pad live 1 » (écran 17)
  - R7 | pas corrigé (écran 21) : carte Simple et « Cortar lo que quiero » au pouce, mais en mode Frases les puces (y=366) sont hors du pouce
  - R3 | corrigé (écran 22) : en Frases la rangée « Frases / En el tiempo / ? / ↶ » est entière, rien sous un bouton flottant
  - R4 | pas vu : poignées hors du pouce, je n'ai pas pu faire un trozo trop long
  - R5 | pas corrigé (écran 10) : deux Deshacer (↶ 534,660 et 71,1062), trois ▶
  - R16 | corrigé (écran 20) : « COMPASES 2 A 5 » en entier
