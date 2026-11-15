code testé : 8a8542fe1bffe24594417d7416cac7b13662451b
joué par : sous-agent neuf
code testé (fiche) : 98e83c21
siege: b | profil: t7-1b debutante-fr-tuto-facile | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t7-1b-1 | texte: Les chapitres 12 à 15 se finissent jusqu'au dernier « Bravo » | ecran: 50 | tenue: non | pourquoi: 12 et 14 non finis (parties absentes), fin du 15 = « Réussi Étape suivante » puis « Presque fini ! Il te reste 35 étapes », aucun Bravo
promesse: id=t7-1b-2 | texte: « Suivant » ne saute jamais une étape sans me dire ce qui reste | ecran: 10 | tenue: non | pourquoi: 12.1 (écran 10) et l'écoute 13.1 (écran 31) passées par Suivant sans aucun message
promesse: id=t7-1b-3 | texte: Niveau : comme la musique en streaming (−14) | ecran: 48 | tenue: oui | pourquoi: l'export dit en mots simples « comme la musique en streaming (−14) » et « Pas de saturation »
promesse: id=t7-1b-4 | texte: tous les textes vus sont en français, aucun coupé | ecran: 44 | tenue: non | pourquoi: puces Longueur de la boucle coupées (image 44), « Écouter mon morc… » coupé (image 33)
oeil-neuf: q1 | ecran: 101 | image: 101 | pads, Remplir, Longueur, carte du tuto touchables ; l'étiquette du pad/mesure en haut est cachée sous l'en-tête
oeil-neuf: q6 | ecran: 101 | image: 101 | aucune preuve d'étape faite ; la consigne cite la partie « Montée » qui n'existe pas, rien ne dit où aller
oeil-neuf: q9 | ecran: 101 | image: 101 | « Mesure 2/2 · Caisse » coupé en haut sous l'en-tête
tuto: chap12 bloqué | chap13 bloqué (Montée absente, « Fait » sans Bravo) | chap14 bloqué (pas de pistes Accords/Mélodie) | chap15 hésitation (fait, sans Bravo) | chap1 hésitation (démo rejouée après coup) | chap2 compris du premier coup | chap3 compris du premier coup
defauts-avant:
  - N19 | corrigé (écran 31) : pas de « Presque fini ! » après Suivant sur l'écoute, mais rien ne dit ce qui reste (voir défaut promesse 2)
  - N20 | pas vu
  - N28 | pas corrigé (écran 101) : la consigne cite « Montée » / « dernière mesure », le projet n'a que la partie A
  - N23 | pas corrigé (écran 50) : dernière étape = « Réussi Étape suivante », en-tête « Étape 1/1 », puis « Presque fini ! »
  - U2 | pas corrigé (écran 48) : −14 affiché comme annoncé, mais aucun niveau à choisir en Simple
  - N12 | corrigé (écran 44) : champ et projet nommés « Mon morceau », comme le tuto
  - N26 | pas vu
hesitations:
  - ecran: 3 | cherchais: la partie « Montée » et ses croix rouges | regarde: menu Partie : seulement « A » et « + Nouvelle partie »
  - ecran: 16 | cherchais: pourquoi l'étape n'est pas validée | regarde: « Cette étape n'est pas encore faite » sans dire quoi
  - ecran: 34 | cherchais: la consigne du chapitre 14 | regarde: carte ouverte déjà défilée
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: le banc écrit code testé 8a8542fe, la fiche dit 98e83c21 (écart à vérifier)
  - geste: 0 | attente: 0 | vu: outils hors human-act.py : Read des images du siège et des 2 fichiers de consignes, ls/cat du dossier du siège
defauts:
  - geste: 3 | ecran.vu: 101 | cible: Partie : A | geste.type: touche
    attendu: une partie « Montée » (puis Drop, Drop 2) comme la consigne le dit | obtenu: seule la partie A, aucun avertissement ; 12.1 à 12.3 impossibles
    symptome: incompris | gravite: mission-impossible | revu: oui: ecran 101 | banc?: non | promesse: t7-1b-1
  - geste: 9 | ecran.vu: 10 | cible: Suivant | geste.type: touche
    attendu: un message disant ce qui reste à faire | obtenu: passe à l'étape 2/3 sans rien dire
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t7-1b-2
  - geste: 30 | ecran.vu: 31 | cible: Suivant | geste.type: touche
    attendu: dire que l'écoute n'est pas faite | obtenu: l'étape d'écoute 13.1 sautée sans message
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t7-1b-2
  - geste: 15 | ecran.vu: 16 | cible: Cette étape n'est pas encore faite. Touche encore « Suivant » pour la passer quand même. | geste.type: touche
    attendu: dire ce qui manque (la partie Drop n'existe pas) | obtenu: message flou, la case touchée dans A ne compte pas
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 29 | banc?: non | promesse: -
  - geste: 35 | ecran.vu: 37 | cible: Je le fais | geste.type: touche
    attendu: ouvrir l'onglet Mix avec les pistes Accords et Mélodie | obtenu: ouvre « Mon morceau » ; Mix n'a que Batterie, Synthé, Sortie
    symptome: mauvais-ecran | gravite: mission-impossible | revu: non | banc?: non | promesse: t7-1b-1
  - geste: 48 | ecran.vu: 50 | cible: Réussi Étape suivante | geste.type: touche
    attendu: « Bravo », tuto fini | obtenu: en-tête « Étape 1/1 », puis « Presque fini ! … Il te reste 35 étapes »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t7-1b-1
  - geste: 43 | ecran.vu: 44 | cible: Longueur de la boucle | geste.type: touche
    attendu: puces 4/8/16/32 lisibles | obtenu: seul le haut des ronds visible, chiffres coupés
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-1b-4
  - geste: 32 | ecran.vu: 33 | cible: Écouter mon morc… | geste.type: touche
    attendu: bouton lisible en entier | obtenu: mot coupé
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-1b-4
  - geste: 33 | ecran.vu: 34 | cible: Je le fais | geste.type: touche
    attendu: carte du chapitre 14 ouverte en haut, consigne visible | obtenu: carte déjà défilée, consigne hors de vue
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 14 | cible: Mesure 2/2 · Caisse | geste.type: touche
    attendu: nom du pad et de la mesure visibles | obtenu: cachés sous l'en-tête du tuto (revu image 101)
    symptome: cache | gravite: finition | revu: oui: ecran 101 | banc?: non | promesse: t7-1b-4
