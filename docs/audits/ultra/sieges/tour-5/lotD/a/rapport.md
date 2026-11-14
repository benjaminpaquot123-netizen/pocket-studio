siege: a | profil: t5-Da compositeur-piano-roll | lang: en | device: tablet11Land | font: 100 | mode: pro
mission: libre | buts: but1, but2, but3
promesse: id=t5-Da-1 | texte: « Note grid: 1 note » / « Son : 1 note du synthé » | ecran: 6 | tenue: oui | pourquoi: note posée = un son part tout de suite, et en tirant son bord droit elle s'allonge (vu sur l'image écran 6)
promesse: id=t5-Da-1 | texte: « ✏ Draw » | ecran: 88 | tenue: non | pourquoi: en Draw, glisser le doigt sur une ligne vide ne trace rien (3 essais, 3 lignes), seul le toucher pose une note
promesse: id=t5-Da-1 | texte: « Paste bar 1 » | ecran: 42 | tenue: non | pourquoi: le collage se pose par-dessus des notes de même hauteur et en supprime une sans rien dire
promesse: id=t5-Da-1 | texte: « Scale » / « Chords » | ecran: 60 | tenue: oui | pourquoi: Chords pose des accords de la gamme (C-E-G, E-G-B en do majeur), Scale n'affiche que les notes de la gamme
promesse: id=t5-Da-1 | texte: « Loop − 16 bars + » | ecran: 56 | tenue: non | pourquoi: le « + » ajoute une mesure par toucher (appui long aussi) : 1 → 16 mesures = 15 touchers, plus 2 pour ouvrir la grille
hesitations:
  - ecran: 4 | cherchais: plus de hauteurs de notes dans la grille | regarde: le gros bouton « Record my melody » et les barres d'outils qui mangent l'écran
  - ecran: 7 | cherchais: comment tracer une note longue d'un geste | regarde: « ✏ Draw » sélectionné, rien ne se passe
  - ecran: 27 | cherchais: la tonalité de la chanson (je veux la mineur) | regarde: Scale, More, Structure, menu de la piste : rien
abandon: jamais
incidents-banc:
defauts:
  - geste: 6 | ecran.vu: 7 | cible: case « ✏ Draw » [sélectionné] @ 221,567 | geste.type: glisse
    attendu: glisser en mode Draw trace une note (ou une suite de notes) | obtenu: rien, « Note grid: 1 note » inchangé, aucun son
    symptome: rien
    gravite: contourne-en-2-gestes | revu: oui: ecran 88 | banc?: non | promesse: t5-Da-1
  - geste: 40 | ecran.vu: 42 | cible: bouton « Paste bar 1 » @ 1808,729 | geste.type: touche
    attendu: collage sans chevauchement, ou un message | obtenu: les 5 notes collées (vertes) se posent à cheval sur les notes orange de même hauteur (C5, D5, accord)
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: oui: ecran 44 | banc?: non | promesse: t5-Da-1
  - geste: 43 | ecran.vu: 44 | cible: bouton « Paste bar 2 » @ 1808,729 | geste.type: touche
    attendu: 5 notes collées en plus, rien d'effacé sans prévenir | obtenu: « 4 notes » sélectionnées, 16 → 20 notes : la longue C5 de la mesure 3 a disparu, aucun message (idem #40 : 16+5 → 20)
    symptome: perdu
    gravite: contourne-en-1-gestes | revu: oui: ecran 42 | banc?: non | promesse: t5-Da-1
  - geste: 78 | ecran.vu: 84 | cible: bouton «   Write a melody for me » @ 960,126 | geste.type: touche
    attendu: mélodie « in the song's key » sans toucher à ma gamme | obtenu: la gamme de Scale passe sans rien dire de do majeur à mi majeur (E F♯ G♯…) sur toutes les pistes ; mes accords C-E-G sont hors gamme ; Undo remet do majeur (écran 84)
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: oui: ecran 84 | banc?: non | promesse: t5-Da-1
  - geste: 26 | ecran.vu: 27 | cible: case « Scale » [sélectionné] @ 890,270 | geste.type: appui-long
    attendu: voir et choisir la tonalité de « Scale » | obtenu: aucune tonalité affichée ni réglable (cherché aussi dans More, Structure et le menu de piste écran 85)
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 85 | banc?: non | promesse: t5-Da-1
  - geste: 9 | ecran.vu: 10 | cible: bouton « + » @ 391,648 | geste.type: touche
    attendu: 16 mesures en moins de 15 touchers | obtenu: +1 mesure par toucher, appui long = +1 aussi (écran 12), 16 bars au 15e toucher
    symptome: lent
    gravite: finition | revu: oui: ecran 56 | banc?: non | promesse: t5-Da-1
  - geste: 3 | ecran.vu: 4 | cible: texte « Note grid: 0 notes » @ 1008,985 | geste.type: touche
    attendu: une grande grille sur une tablette 11" | obtenu: 5 à 7 hauteurs visibles en bas de l'écran ; « Record my melody » et les barres d'outils prennent les 2/3 ; elle rétrécit encore quand la barre de sélection s'ouvre
    symptome: cache
    gravite: finition | revu: oui: ecran 81 | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: bouton « Keep going Pro My first beat Loop of 6 bars Saved just now » @ 633,649 | geste.type: touche
    attendu: revenir sur la piste Synth, vue Notes, où j'étais | obtenu: rouvre sur Drums 16 pads ; il faut toucher Synth puis Notes (notes bien gardées)
    symptome: mauvais-ecran
    gravite: contourne-en-2-gestes | revu: oui: ecran 75 | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 40 | cible: bouton « C5 » @ 198,1101 | geste.type: touche
    attendu: une note posée sur une note de même hauteur la coupe ou la fusionne | obtenu: les deux C5 se chevauchent sur la grille, sans avertissement
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t5-Da-1
  - geste: 72 | ecran.vu: 73 | cible: case « Notes » [sélectionné] @ 343,270 | geste.type: retour
    attendu: Retour ramène à l'accueil (comme au geste 29) | obtenu: rien ne change, il faut un 2e Retour
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: -
