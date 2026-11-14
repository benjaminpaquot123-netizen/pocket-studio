siege: a | profil: t6-Ba gaucher-espagnol-une-main | lang: es | device: phone | font: 100 | mode: pro
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t6-Ba-1 | texte: bouton « Simple Para empezar: 5 pestañas, botones grandes y palabras de todos los días. » | ecran: 3 | tenue: non | pourquoi: hors du pouce gauche : la carte Simple (écran 3, revu 8), les puces Voz/Batería/Bajo/Resto de « Cortar la canción » (écran 22), les onglets « Pistas de audio » (écran 42) ; « Cortar lo que quiero » demande un glissement d'abord (écran 19)
promesse: id=t6-Ba-2 | texte: bouton ✂ «  Cortar lo que quiero » | ecran: 18 | tenue: oui | pourquoi: une seule carte orange conseillée après séparation (« Tocar con el proyecto » est en contour) ; séparation rapide : 1 seul geste d'attente (attends 30) après le choix de la chanson
promesse: id=t6-Ba-3 | texte: icône (flèches en boucle) « A comp. 1/1 » | ecran: 39 | tenue: non | pourquoi: chanson de 0:40 jouée avec un projet d'une mesure en boucle, aucun « la fin será cortada » nulle part sur le panneau (haut revu écran 41)
promesse: id=t6-Ba-4 | texte: titre « COMP. 2 A 5 » | ecran: 28 | tenue: non | pourquoi: après « Frases », la grande forme d'onde prend la place de la ligne d'outils (« Un trozo », « En el tiempo », ↶, « Inicio aquí », « Fin aquí », « Escuchar el trozo ») qui disparaît
oeil-neuf: q1 | ecran: 42 | image: 42 | touchables vus : boutons orange, chips « 1/2/4 comp. », − +, haut-parleur, casque ; « → Pad » et « → Live » ressemblent à des liens (texte seul), doute sur leur nature
oeil-neuf: q6 | ecran: 42 | image: 42 | preuve de la pose : seulement l'onglet « Voz 1 pista » en haut, hors du pouce ; rien sur ce panneau ne dit que le trozo est posé, et deux « Cortar la canción » pour la suite
oeil-neuf: q9 | ecran: 42 | image: 42 | onglets « Ma chanson préférée Canción » et « Generar » coupés au bord droit ; panneau coupé en haut sous les onglets
hesitations:
  - ecran: 14 | cherchais: le bouton pour découper après séparation | regarde: « Cortar lo que quiero » caché sous le gros bouton flottant « Elegir una canción »
  - ecran: 29 | cherchais: comment marquer un trozo en touchant la forme d'onde | regarde: rien ne se passe ; il faut toucher la petite pastille « 1 » en haut du morceau
  - ecran: 31 | cherchais: le pad où va le trozo | regarde: « En los pads » le met dans « el pad live 1 », comme « En el Live » le laisserait penser
abandon: jamais
incidents-banc:
defauts:
  - geste: 2 | ecran.vu: 3 | cible: bouton « Simple Para empezar: 5 pestañas, botones grandes y palabras de todos los días. » | geste.type: touche
    attendu: choisir le mode Simple au pouce gauche | obtenu: trop loin du pouce, les deux cartes sont en haut et le bas de l'écran est vide ; forcé en Pro
    symptome: hors-pouce
    gravite: contourne-en-4-gestes | revu: oui: ecran 8 | banc?: non | promesse: t6-Ba-1
  - geste: 21 | ecran.vu: 22 | cible: bouton « Batería » @ 199,444 | geste.type: touche
    attendu: changer de piste séparée dans « Cortar la canción » | obtenu: puces Voz/Batería/Bajo/Resto hors du pouce, la page ne défile pas (geste 22)
    symptome: hors-pouce
    gravite: finition | revu: oui: ecran 23 | banc?: non | promesse: t6-Ba-1
  - geste: 18 | ecran.vu: 19 | cible: bouton ✂ «  Cortar lo que quiero » | geste.type: touche
    attendu: ouvrir la découpe au pouce | obtenu: trop loin du pouce ; il faut faire défiler vers le bas puis retoucher
    symptome: hors-pouce
    gravite: contourne-en-1-gestes | revu: oui: ecran 21 | banc?: non | promesse: t6-Ba-1
  - geste: 27 | ecran.vu: 28 | cible: texte « Frases » | geste.type: touche
    attendu: la ligne d'outils de découpe reste visible | obtenu: « Un trozo », « En el tiempo », ↶, « Inicio aquí », « Fin aquí », « Escuchar el trozo » disparaissent sous la grande forme d'onde
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: t6-Ba-4
  - geste: 20 | ecran.vu: 21 | cible: bouton « Inicio aquí » (barre, onglet à gauche) | geste.type: touche
    attendu: boutons Inicio/Fin/Escuchar lisibles | obtenu: ligne écrasée entre les puces et le cadre du dessous, seul le haut des boutons visible, mots coupés (image 21)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t6-Ba-4
  - geste: 38 | ecran.vu: 39 | cible: icône (flèches en boucle) « A comp. 1/1 » | geste.type: touche
    attendu: un avertissement « la fin será cortada » (chanson 0:40, projet d'1 mesure) | obtenu: aucun avertissement, ni au choix de la chanson ni en lecture
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 41 | banc?: non | promesse: t6-Ba-3
  - geste: 28 | ecran.vu: 29 | cible: titre « COMP. 2 A 5 » | geste.type: touche
    attendu: toucher le morceau de voix le marque | obtenu: rien ; seule la petite pastille « 1 » marque le trozo (geste 29)
    symptome: sans-aide
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 14 | cible: bouton ✂ «  Cortar lo que quiero » [coupé] | geste.type: touche
    attendu: le bouton conseillé visible après séparation | obtenu: recouvert par le bouton flottant « Elegir una canción » (image 14) ; un glissement nécessaire
    symptome: cache
    gravite: contourne-en-2-gestes | revu: oui: ecran 18 | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 41 | cible: bouton ✂ «  Cortar la canción » @ 294,903 | geste.type: glisse
    attendu: un seul bouton par action | obtenu: deux « Cortar la canción » sur le même panneau (aussi deux « Descargar el modelo (174 MB) » écran 9)
    symptome: incompris
    gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: -
mots-faux:
  - ecran: 28 | vu: COMP. 2 A 5 | attendu: COMPASES 2 A 5
