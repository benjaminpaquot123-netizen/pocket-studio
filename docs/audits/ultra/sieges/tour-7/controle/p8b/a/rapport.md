code testé : 11ca024b94945ffae964263f27e8342aa3770316
joué par : sous-agent neuf
siege: a | profil: adulte-fr-tuto-house-ch3 | lang: fr | device: téléphone | font: 100 | mode: simple
mission: tuto-facile | buts: chapitre-3, chapitre-4, chapitre-5
promesse: id=consigne-kit | texte: À toi : sur le pad « Claquement de mains », touche les cases en pointillé des 2 mesures, sur les temps 2 et 4. | ecran: 1 | tenue: non | pourquoi: la consigne est bien au-dessus de la carte du kit, mais coupée à une ligne « À toi : sur le pad « Claquement de mains », … » : « temps 2 et 4 » ne se lit qu'en ouvrant « Lire la suite »
promesse: id=touche-ici | texte: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | ecran: 66 | tenue: non | pourquoi: mesures 1 (écran 66) et 4 (écran 76) : 1er toucher montre seulement 2 notes aiguës sur 6, la flèche passe à ↓ ; il faut un 2e toucher pour les graves
promesse: id=pointilles-partie | texte: Basse · mesure 4/8 Toutes les notes en pointillé sont posées | ecran: 86 | tenue: oui | pourquoi: partie B créée (copie de A) : aucun pointillé redemandé, ni au Clavier (écran 86) ni au Rythme (écran 87)
oeil-neuf: q1 | ecran: 87 | image: 87 | tout ce qui semble touchable l'est ; « Remplir » ressemble à un mot touchable mais c'est une étiquette, et le titre « Mesure 2/2 · Bord de caisse » est à moitié caché sous « Partie : B »
oeil-neuf: q6 | ecran: 87 | image: 87 | preuve claire : « Bravo, chapitre fini ! » + cases roses ; mais la suite (chapitre 6) n'est pas proposée sans rouvrir le panneau
oeil-neuf: q9 | ecran: 87 | image: 87 | titre de la grille « Mesure 2/2 · Bord de caisse » coupé/estompé en haut ; rien d'autre coupé
defauts-avant:
  - a-V1 | encore là | écran 66 et 76 | « Touche ici » sur deux hauteurs : le 1er toucher montre les aiguës, les graves restent hors écran (flèche ↓), 2e toucher nécessaire, aux mesures 1 et 4
  - a-V2 | corrigé | écran 1 | la consigne est au-dessus de la carte du kit (mais tronquée à une ligne : voir défaut 1)
  - a-V4 | corrigé | écran 86 | rien de fait en A n'est redemandé en pointillé en B
hesitations:
  - ecran: 1 | cherchais: sur quels temps toucher | regarde: consigne coupée, je me fie aux cases en pointillé
  - ecran: 83 | cherchais: lequel des deux « Copier » toucher | regarde: « Copier » dans la grille et « Copier sur les mesures suivantes » dans le panneau font la même chose
abandon: jamais
incidents-banc:
  - geste: 6 | attente: 0 | vu: j'ai écrit par erreur un fichier temporaire (a-tmp) dans le dossier parent du siège, supprimé aussitôt ; outils hors human-act.py : Bash (sed, grep, cat, rm) pour filtrer l'affichage et écrire ce rapport, Read pour les images
defauts:
  - geste: 1 | ecran.vu: 1 | cible: À toi : sur le pad « Claquement de mains », touche les cases en pointillé des 2 mesures, sur les temps 2 et 4. | geste.type: touche
    attendu: consigne entière lisible au-dessus du kit | obtenu: une seule ligne visible, « sur les temps 2 et 4 » caché derrière « Lire la suite »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 3 | banc?: non | promesse: consigne-kit
  - geste: 65 | ecran.vu: 66 | cible: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: les 6 notes visées à l'écran | obtenu: 2 notes aiguës visibles, 4 graves hors écran, bandeau « ↓ … plus bas », 2e « Touche ici » nécessaire
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 76 | banc?: non | promesse: touche-ici
  - geste: 75 | ecran.vu: 76 | cible: Basse · mesure 4/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: les 6 notes de la mesure 4 à l'écran | obtenu: 2 notes visibles (une aiguë, une plus bas), 4 hors écran, 2e toucher nécessaire
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 79 | banc?: non | promesse: touche-ici
  - geste: 85 | ecran.vu: 86 | cible: A mesure 1/16 | geste.type: touche
    attendu: en haut, le repère suit la partie choisie (B) | obtenu: « A mesure 1/16 » affiché alors que « Partie : B » est sélectionnée
    symptome: texte-faux | gravite: finition | revu: oui: ecran 87 | banc?: non | promesse: -
  - geste: 86 | ecran.vu: 87 | cible: Mesure 2/2 · Bord de caisse | geste.type: touche
    attendu: titre de la grille lisible | obtenu: titre estompé et à moitié sous la ligne « Partie : B »
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
fin
