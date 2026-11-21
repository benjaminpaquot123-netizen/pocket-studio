code testé : dee3519df1694386b61d84269762b368f08029e8
joué par : sous-agent neuf
siege: a | profil: t8-t0c-a-debutant-motive | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap8, montee, bravo
promesse: id=t0c-a-1 | texte: Chaque consigne décrit ce que je vois et ce que j'entends à l'écran | ecran: 97 | tenue: non | pourquoi: la consigne du partage parle de « Niveau », « Pas de saturation », « aucun solo oublié », l'écran dit « Volume : −13,7 » ; E30 « avec sa durée en mesures » mais les durées de la carte sont coupées
promesse: id=t0c-a-2 | texte: Aucune consigne ne demande plus de 3 actions à la fois | ecran: 49 | tenue: non | pourquoi: « touche dans l'ordre Intro, Intro 2, Drop, Break, Montée, Drop 2 et Fin » = 7 touches d'un coup
promesse: id=t0c-a-3 | texte: tous les textes vus sont en français correct, aucun coupé | ecran: 7 | tenue: non | pourquoi: phrase « La ligne du temps… » coupée (E7 à E59), carte coupée (E30), début de la consigne coupé après « Lire la suite » (E96)
oeil-neuf: q1 | ecran: 102 | image: 102 | touchables clairs (M4A, WAV, Partager, cartes de chapitre avec →) ; « Mes fichiers » est écrasé en bande fine sous Partager, on doute que ce soit un bouton
oeil-neuf: q6 | ecran: 102 | image: 102 | preuve : « Presque fini ! » et barre presque pleine ; suite : « Il te reste 6 étapes », mais « Chapitre 2/15 » apparaît deux fois, on ne sait pas laquelle prendre
oeil-neuf: q9 | ecran: 102 | image: 102 | titres et sous-titres des cartes coupés en « … » (« La vitesse et la couleur du mo… »), « Mes fichiers » rogné, « Revenir à l'étape » coupé en bas
hesitations:
  - ecran: 30 | cherchais: la durée de chaque partie dont parle la consigne | regarde: carte coupée, seuls Intro/Drop/Drop 2/Fin lisibles, chiffres cachés
  - ecran: 63 | cherchais: pourquoi tous les − / + sont gris après « Donner les durées conseillées » | regarde: la seule aide parle du Live, je n'ai rien joué en Live
  - ecran: 75 | cherchais: pourquoi la grille est passée sur « Cymbale » | regarde: aucune phrase ne le dit, il faut toucher « Aller dans la partie « Drop » »
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun gel ; outils hors human-act.py : sed/awk/grep/cat sur mon seul dossier de siège (écrans, notes.txt, journal.txt pour les numéros de geste)
defauts:
  - geste: 48 | ecran.vu: 49 | cible: À toi : dans « Mon morceau », sous « Ajouter », touche dans l'ordre Intro, Intro 2, Drop, Break, Montée, Drop 2 et Fin. | geste.type: touche
    attendu: une consigne de 3 actions au plus | obtenu: 7 touches dans une seule consigne, et le titre devient « Ajouter à la fin : » au lieu de « Ajouter »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t0c-a-2
  - geste: 62 | ecran.vu: 63 | cible: − / + : répétitions de la partie (une partie jouée en Live garde sa longueur). | geste.type: touche
    attendu: une phrase dit pourquoi le + du Drop est grisé | obtenu: rien ne se passe, aucun message ; l'aide ne parle que du Live
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 62 | banc?: non | promesse: -
  - geste: 29 | ecran.vu: 30 | cible: Regarde la carte : chaque case est une partie, avec sa durée en mesures. La courbe montre l'énergie. | geste.type: touche
    attendu: la carte montre chaque partie et sa durée | obtenu: durées « 16 » [coupé], « Fi… » coupé, la case de 4 mesures n'est qu'un trait, 4 parties seulement nommées
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t0c-a-1
  - geste: 6 | ecran.vu: 7 | cible: La ligne du temps met tes parties dans l'ordre, chacune avec sa durée en mesures. Ajoute-les dans l'ordre. | geste.type: touche
    attendu: la phrase entière lisible | obtenu: [coupé] sous la barre du haut, de l'écran 7 à 59
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 49 | banc?: non | promesse: t0c-a-3
  - geste: 95 | ecran.vu: 96 | cible: Lire la suite | geste.type: touche
    attendu: la consigne du partage s'affiche en entier | obtenu: le 1er toucher (#94) ne fait rien, le 2e déplie mais le début glisse sous la barre de progrès (on lit « morceau sonne fort ou doux (« −14 »… »)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t0c-a-3
  - geste: 96 | ecran.vu: 97 | cible: Volume : −13,7, comme la musique en streaming (−14) | geste.type: touche
    attendu: les mots « Niveau », « Pas de saturation », « aucun solo oublié » de la consigne sur cet écran | obtenu: « Volume » ici ; « Niveau » et « Pas de saturation » seulement après l'envoi (E100), « aucun solo oublié » jamais vu
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t0c-a-1
  - geste: 84 | ecran.vu: 86 | cible: Exemple passé sans l'écouter. | geste.type: touche
    attendu: le message disparaît une fois l'exemple écouté | obtenu: après « Écouter l'exemple » (#84) et 8 s d'attente, chapitre « Fait », le message reste
    symptome: texte-faux
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 74 | ecran.vu: 75 | cible: Le pad « Cymbale » au premier temps marque l'entrée ; ailleurs, il se répéterait toutes les mesures. | geste.type: touche
    attendu: une phrase dit qu'on passe au pad Cymbale | obtenu: la grille passe de « Claquement de mains » à « Mesure 4/4 · Cymbale » sans explication
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t0c-a-1
  - geste: 101 | ecran.vu: 102 | cible: Chapitre 2/15 · Le kick et le grondement | geste.type: touche
    attendu: une carte par étape restante, nom différent | obtenu: deux cartes « Chapitre 2/15 · Le kick et le grondement » l'une sous l'autre, titres coupés en « … »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t0c-a-3
mots-faux:
  - ecran: 15 | vu: stab (pas expliqué avant l'écran 82) | attendu: accord court
defauts-avant:
  - t0c-derniere-rafale | autre | ecran: 68 | aucune consigne des chapitres 8 à 15 ne dit « rafale » ni « la dernière rafale » ; l'étape de la Montée dit « À toi : à la fin de la Montée, efface les 4 coups du pad « Claquement de mains » du dernier temps. » et je vois bien 4 coups barrés, mais on ne m'a jamais fait poser de rafale ni de roulement
  - t0c-c-V3-creation-montee | corrigé | ecran: 42 | « Partie « Montée » créée (copie du Groove) : sans Grosse caisse studio, Grondement grave, Charley ouvert, Bord de caisse, Basse, Mélodie. Son pad « Claquement de mains » accélère mesure après mesure. » : plus rien sur un dernier temps silencieux
  - t0c-3a14-moins-plus-grises | encore là | ecran: 62 | après « Donner les durées conseillées » tous les − / + sont gris ; la seule ligne près d'eux est « − / + : répétitions de la partie (une partie jouée en Live garde sa longueur). », qui ne dit pas pourquoi (je n'ai rien joué en Live)
