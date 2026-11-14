code testé : 10830ce2d4befd58cf3b89e815e0e25cf59965fb
joué par : sous-agent neuf

siege: a | profil: t6-Ba gaucher-espagnol-une-main | lang: es | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t6-Ba-1 | texte: Tout le Remix et la découpe se font à la main gauche, sans bouton hors du pouce | ecran: 57 | tenue: non | pourquoi: « + » de « Empezar desde el compás » (510,549) et « Entendido » (écran 71) hors du pouce, page déjà en haut ; aussi minicarte (écran 22) et morceau 2 (écran 24)
promesse: id=t6-Ba-2 | texte: ✂ « Cortar lo que quiero » (seul bouton plein de la carte « Ma chanson préférée ») | ecran: 8 | tenue: oui | pourquoi: une seule action pleine dans la carte, « Tocar con el proyecto » est en contour
promesse: id=t6-Ba-3 | texte: « La canción pasa 0:38 del final del tema: su final se cortará. » | ecran: 70 | tenue: oui | pourquoi: affiché à « Poner en mi proyecto » sur un projet d'1 mesure
promesse: id=t6-Ba-4 | texte: « ✂ ▾ Frases » / « En el tiempo » [coupé] | ecran: 20 | tenue: non | pourquoi: en Frases/Compases les puces de mode passent sous le panneau « Marca trozos », invisibles (images 20, 23, 37) ; en « Un trozo » tout est visible (écran 14)
oeil-neuf: q1 | ecran: 72 | image: 72 | Reproducir, Abrir el tablero, Inicio, −/+, Cortar la canción ont l'air touchables ; « Efectos » orange a l'air d'un lien mais l'écran le dit texte ; la barre verte (à glisser) n'a pas l'air touchable
oeil-neuf: q6 | ecran: 72 | image: 72 | preuve : « La canción entra en el compás 1, al inicio de “A” » + barre verte ; mais plus aucune trace de « su final se cortará » et pas de suite indiquée sauf « Cortar la canción »
oeil-neuf: q9 | ecran: 72 | image: 72 | « 0:00 / 0:40 · inicio » coupé : « inicio » disparaît après le « · » contre « Efectos »
hesitations:
  - ecran: 37 | cherchais: le bouton « Quitar » pour enlever un des 2 morceaux choisis | regarde: retoucher le morceau, appui long, toucher « 2 trozos elegidos » : rien ; c'est son numéro rond qui l'enlève (écran 40)
  - ecran: 34 | cherchais: le choix « Frases / Compases » pour changer de mode | regarde: invisible, touché à l'aveugle sous la forme d'onde (#34), ça a ouvert le menu
  - ecran: 8 | cherchais: lequel des deux « Elegir una canción » toucher | regarde: les deux sont orange pleins, un dans la carte, un flottant
abandon: jamais
incidents-banc:
  - geste: 7 | attente: 60 | vu: séparation finie en un seul « attends 60 » (1 geste d'attente, pas lente) ; banc?: temps
  - geste: 30 | attente: 0 | vu: j'ai enchaîné deux commandes dans un seul appel (#30 attends et #31 touche), sans effet sur le test ; aucun outil hors human-act.py sauf Read des images et ls/cat/grep de mon dossier de siège (start.txt, rapport.md, fin.txt, journal.txt)
defauts:
  - geste: 56 | ecran.vu: 57 | cible: icône + @ 510,549 | geste.type: touche
    attendu: avancer le compas de départ au pouce gauche | obtenu: hors de portée, page déjà en haut, impossible de le descendre
    symptome: hors-pouce | gravite: finition | revu: non | banc?: non | promesse: t6-Ba-1
  - geste: 70 | ecran.vu: 71 | cible: texte « Entendido » @ 481,687 | geste.type: touche
    attendu: fermer l'avertissement « su final se cortará » | obtenu: hors du pouce, fermé par Retour (#71)
    symptome: hors-pouce | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Ba-1
  - geste: 23 | ecran.vu: 24 | cible: dessin morceau 2 de la voix (forme d'onde, droite) | geste.type: touche
    attendu: choisir la 2e phrase de voix | obtenu: hors du pouce ; il faut faire glisser la forme d'onde puis viser plus bas (#24, #25)
    symptome: hors-pouce | gravite: contourne-en-2-gestes | revu: oui: ecran 26 | banc?: non | promesse: t6-Ba-1
  - geste: 21 | ecran.vu: 22 | cible: minicarte de la chanson au-dessus de « COMP. 2 A 5 » | geste.type: touche
    attendu: sauter ailleurs dans la chanson | obtenu: hors du pouce ; glisser la forme d'onde à la place (#22)
    symptome: hors-pouce | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Ba-1
  - geste: 19 | ecran.vu: 20 | cible: bouton ✂ ▾ « Frases » [coupé] @ 117,753 | geste.type: touche
    attendu: le choix de mode reste visible après avoir choisi Frases | obtenu: puces « Frases » et « En el tiempo » cachées sous le panneau des actions (images 20, 23, 37) ; retrouvé à l'aveugle (#34)
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 37 | banc?: non | promesse: t6-Ba-4
  - geste: 27 | ecran.vu: 28 | cible: texte « 2 trozos elegidos · 0:07 en total » | geste.type: touche
    attendu: un bouton « Quitar » ou retoucher le morceau pour l'enlever | obtenu: aucun « Quitar » ; retoucher/appui long ne fait rien ; seul le rond numéroté l'enlève (#39), sans aide
    symptome: sans-aide | gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 31 | ecran.vu: 33 | cible: texte « OK » @ 508,741 | geste.type: touche
    attendu: le message « “Voz 1” está en el pad live 1 » part seul | obtenu: il reste jusqu'à « OK » et couvre le choix de mode (attendu 18 s, #30-#32)
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: temps | promesse: t6-Ba-4
  - geste: 6 | ecran.vu: 8 | cible: bouton + «  Elegir una canción » @ 294,903 | geste.type: touche
    attendu: un seul « Elegir una canción » | obtenu: deux boutons orange pleins identiques (carte + flottant) ; idem deux « Descargar el modelo (174 MB) » écran 3
    symptome: incompris | gravite: finition | revu: oui: ecran 3 | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 57 | cible: texte « 0:00 / 0:40 · inicio » @ 350,342 | geste.type: retour
    attendu: « inicio » lisible | obtenu: l'image s'arrête à « 0:00 / 0:40 · », « inicio » mangé par « Efectos »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 69 | ecran.vu: 70 | cible: choix « Ma chanson préférée se ajusta a mi tempo (120 BPM) » [sélectionné] | geste.type: touche
    attendu: message qui confirme que la chanson suit mon tempo | obtenu: « Tu canción mantiene su tempo (120 BPM): el tema se ajusta a él sin cambiar de tono. » : dit l'inverse de mon choix
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - A7 | corrigé (écran 21 : phrase touchée → « 1 trozo elegido », « En los pads » activé)
  - A8 | corrigé (écran 8 : un seul ✂ Cortar lo que quiero ; écrans 41-51, chanson dans le projet : un seul Cortar la canción, plus de Cortar lo que quiero)
  - A9 | autre : aucun bouton « Quitar » vu ; en touchant le rond numéroté d'un des 2 morceaux, seul celui-là part (écran 40, 2 → 1 trozo)
  - A2 | corrigé (écrans 15-18 : Resto, Batería, Bajo, Voz touchés au pouce gauche, sans glisser)
  - A3 | corrigé (écran 8 : bouton ✂ et sa phrase entiers au-dessus du flottant)
  - A5 | corrigé (écran 14 : Inicio aquí, Fin aquí, Escuchar el trozo entiers, au-dessus de Guía)
