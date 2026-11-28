code testé : 694768c6c37e495221d99ca07c16e2408b8f629c
joué par : sous-agent neuf
siege: d | profil: leo-debutant | lang: fr | device: telephone | font: 100 | mode: simple
mission: tuto-facile | buts: -
chapitres: 12=hésitation, 3=hésitation
chapitres bloqués: 0 | graves neufs: 0
oeil-neuf: q1 | ecran: 47 | image: 47 | touchables vus : pads du haut, « Kit : Studio », « Tous les pads », Mesure 1/2, « Suivre », cases, « Chapitres », barre « Bravo » ⌃, Annuler/Rétablir, ▶, onglets ; le titre « PAS : TOUS LES PADS ⌄ » se replie mais ressemble à un simple titre ; les cases de la grille sont minuscules
oeil-neuf: q6 | ecran: 47 | image: 47 | preuve : case jaune du clap + « Bravo, chapitre fini ! » ; mais après Annuler une case du temps 4 (mesure 2) est vide et le Bravo reste ; pas de « Chapitre suivant » visible sans ouvrir la barre
oeil-neuf: q9 | ecran: 47 | image: 47 | noms de pads coupés « Grosse c… », « Claquem… », « Charley … », « Tom méd… » ; titre « PAS : TOUS LES PADS » sur deux lignes
hesitations:
  - ecran: 11 | cherchais: pourquoi l'étape « efface les coups du dernier temps » ne passait pas alors que les 3 croix du Charley étaient enlevées | regarde: « Cette étape n'est pas encore faite » sans dire où ; les 2 autres croix étaient sous le panneau du tuto
  - ecran: 26 | cherchais: jouer le ch.3 « Le clap » | regarde: « Déjà fait ! … rien à toucher » : j'ai dû faire « Recommencer le tuto (nouveau projet vide) » pour pouvoir le jouer
  - ecran: 21 | cherchais: toucher 4 cases de Maracas | regarde: « Bravo, chapitre fini ! » dès la 1re case touchée (je ne sais pas si 3 cases y étaient déjà)
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: le brief annonce « code testé : a0d928cdb7be2961a852750dca8721b975ee8473 », le banc a écrit 694768c6c37e495221d99ca07c16e2408b8f629c : j'ai gardé la ligne du banc
defauts:
  - geste: 9 | ecran.vu: 13 | cible: texte « 3 coups en trop ici (barrés) » | geste.type: touche
    attendu: les croix rouges de la dernière mesure visibles, ou l'appli dit où sont les autres | obtenu: 2 croix (Charley ouvert, Bord de caisse) cachées sous le panneau du tuto dans « Tous les pads », la vue d'un pad ne parle que du Charley ; l'étape ne passe pas et ne dit pas pourquoi
    symptome: cache | gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 30 | cible: icône (liste) « Chapitres » @ 444,573 | geste.type: touche
    attendu: la liste des chapitres s'ouvre | obtenu: rien ne change ; mon geste suivant, prévu pour « 3 Le clap », a mis un coup sur « Charley ouvert » dans la grille
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 32 | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 6 | cible: texte « Grosse caisse 1 » | geste.type: touche
    attendu: noms des pads lisibles dans « Tous les pads » | obtenu: « Grosse c… », « Claquem… », « Charley … », « Bord de … », « Tom méd… »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 47 | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 47 | cible: icône (coupe) « Bravo, chapitre fini ! » ⌃ | geste.type: touche
    attendu: après Annuler, l'appli montre qu'il manque de nouveau une case | obtenu: « Encore 1 case en pointillé ici » et « Bravo, chapitre fini ! » / « Fait » en même temps, barre de progression presque vide
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - a-V1-anneau | corrigé | ecran: 41 et 47 | vue d'un pad (écran 41) : la case à toucher du clap a un anneau orange autour ; « Tous les pads » (écrans 34 et 47) : la case à toucher a aussi un anneau orange ; je n'ai vu nulle part les mots « Touche ici » écrits
  - d-V2 | pas atteint | ecran: 2 | la partie « Montée » existait mais n'était pas vide ; le message « Il n'y a pas de partie « Montée » » ne s'est jamais affiché
  - c-croix | corrigé | ecran: 2 et 6 | vue d'un pad (écran 2) : 3 croix rouges dans des cases en pointillé ; « Tous les pads » (écrans 6 et 13) : croix rouges en pointillé, petites, sur Charley puis Charley ouvert et Bord de caisse

note du siège : 694768c6 = a0d928cdb7be2961a852750dca8721b975ee8473 + les seuls documents du passage a (0 fichier hors docs/audits/ultra/sieges/tour-8/controle/t0b-d2/a/) ; même code d'appli.
