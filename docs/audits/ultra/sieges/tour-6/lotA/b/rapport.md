siege: b | profil: t6-Ab musicienne-pressee | lang: fr | device: phone | font: 100 | mode: simple
mission: padlive | buts: son, stop, prise
promesse: id=t6-Ab-1 | texte: « Ma mélodie Mélodie » | ecran: 49 | tenue: oui | pourquoi: prise au Clavier (3 notes), Retour pendant la prise (#9), Pad Live, retour par Mes projets : la piste « Ma mélodie » est là, « Vider » actif, notes visibles en « Tout voir » (Do4 mesure 4, une autre fin de mesure 3)
promesse: id=t6-Ab-2 | texte: « Tout couper » | ecran: 56 | tenue: oui | pourquoi: pads et ▶ lancés : après #28 et #55, « Son : rien ne joue. », ▶ revient, tous les pads éteints ; aucun compteur affiché dans Pad Live
promesse: id=t6-Ab-3 | texte: « EFFETS LIVE » | ecran: 60 | tenue: oui | pourquoi: 4 glissés (#18, #19, #58, #59), aucun effet allumé ni annoncé ; mais un appui long sur « Écho » (#60) n'annonce rien non plus, la preuve est faible
promesse: id=t6-Ab-4 | texte: « boucle » | ecran: 55 | tenue: non | pourquoi: 5 essais (#13, #14, #16, #27, #54), musique en route : le pad s'allume et joue dès l'écran suivant, jamais d'[anneau pâle]
oeil-neuf: q1 | ecran: 64 | image: 64 | pads, ▶, − / +, Pack, ★, ?, effets, Tout couper, Enregistrer, Plus ont l'air touchables ; « REC » en haut (rond rouge) a l'air d'un bouton mais l'écran le dit « texte »
oeil-neuf: q6 | ecran: 64 | image: 64 | après la prise : « Ta prise est sur le pad B1. » (écran 26) et le pad « Ma prise 1 » : preuve claire, la suite aussi (toucher le pad)
oeil-neuf: q9 | ecran: 64 | image: 64 | rien de coupé sur cet écran (« Pack… » est un nom d'attente, pas une coupure)
hesitations:
  - ecran: 35 | cherchais: mes notes de mélodie après le retour au projet | regarde: la grille « Notes » montre seulement la mesure 1, vide ; j'ai fait défiler haut, bas, côté avant de trouver « Tout voir »
  - ecran: 11 | cherchais: mon projet en ouvrant Pad Live | regarde: message « Pad Live a son propre projet « Live » : « Mon projet 1 » t'attend dans Mes projets. » ; « Continuer » ouvre ensuite « Live », pas mon projet (écran 30)
  - ecran: 21 | cherchais: combien de temps dure ma prise | regarde: « 1 mesure · touche pour finir » monte d'une mesure à chaque geste (écran 24 : 4 mesures)
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 600 | vu: le premier « regarde » attendait sans banc lancé ; banc lancé par moi (scripts/human-tester.sh) après le selftest vert du lot (scripts/bench/selftest.sh), sur consigne de la discussion principale ; outils hors human-act : bash (ls, cat, grep, sed sur la sortie du banc et mon dossier de siège), Read sur mes images
  - geste: 60 | attente: 0 | vu: l'appui long sur « Écho » ne donne aucune trace d'effet dans la ligne « Son » : le banc ne semble pas dire les effets (la promesse 3 ne se prouve donc qu'à l'écran)
defauts:
  - geste: 13 | ecran.vu: 14 | cible: bouton @ 333,256 | geste.type: touche
    attendu: le pad « Boucle Acid 303 » attend la mesure suivante (anneau pâle) puisque la boucle de kick joue déjà | obtenu: allumé et « les pads live 1, 3 jouent en boucle » tout de suite ; pareil aux #14, #16, #27, #54 (écran 55, ▶ en route)
    symptome: incompris
    gravite: finition | revu: oui: ecran 55 | banc?: temps | promesse: t6-Ab-4
  - geste: 34 | ecran.vu: 43 | cible: bouton « Notes » @ 203,354 | geste.type: touche
    attendu: voir ma mélodie enregistrée en ouvrant « Notes » | obtenu: grille agrandie sur la mesure 1 seule, aucune note visible : on croit la mélodie perdue ; il faut « Tout voir » puis faire défiler (écran 49)
    symptome: cache
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 29 | cible: texte « EFFETS LIVE » @ 79,704 | geste.type: touche
    attendu: le titre « EFFETS LIVE » entier quand « Plus » est ouvert | obtenu: sur l'image (écrans 25 et 29), le titre est collé sous la dernière rangée de pads et son « E » de début paraît rogné
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 29 | banc?: non | promesse: -
