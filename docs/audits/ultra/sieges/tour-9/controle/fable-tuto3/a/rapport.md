code testé : 257267dba5ec6396b700731001861e3779756397
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1 (oui, écran 20), chap3 (oui, chapitres 5, 6, 7 finis, écran 63 ; chapitre 8 fini aussi, écran 74), bravo (oui, « Bravo, chapitre fini ! » écrans 20, 42, 63, 74)
promesse: id=chap6-accord | texte: « À toi : touche la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. Il y a 4 coups. » | ecran: 25 | tenue: oui | pourquoi: en touchant la note du bas, l'accord entier se pose, 4 coups suffisent (écrans 26 à 29)
promesse: id=compteur-pointilles | texte: « Encore 1 note en pointillé à toucher » | ecran: 53 | tenue: non | pourquoi: au début de l'étape 2 du chapitre 7 la ligne dit « Encore 1 note » alors que la consigne dit « écris les 8 notes » ; après « Je le fais » elle dit « Encore 8 notes » (écran 54)
hesitations:
  - ecran: 25 | cherchais: quelle note toucher : la consigne dit « la note du bas » | regarde: le rond jaune est autour de la note du HAUT du premier accord ; j'ai suivi le texte (note du bas), ça a marché
  - ecran: 35 | cherchais: où est la mesure 4 (la consigne dit « mesures 3 et 4 ») | regarde: la ligne du haut s'est changée en « … à toucher : 5 → Touche ici », je l'ai touchée et la mesure 4 est apparue (écran 36)
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: plafond d'images atteint une fois après l'écran 27 (règle du banc, pas une panne) ; aucun outil utilisé hors human-act.py (sauf lecture des images du dossier du siège et écriture de ce rapport)
defauts:
  - geste: 25 | ecran.vu: 26 | cible: bouton « Accords · mesure 1/8 Accords en pointillé à toucher : 3 » | geste.type: touche
    attendu: après avoir posé le 1er accord, le rond « Touche ici » passe sur l'accord suivant encore en pointillé | obtenu: le rond reste autour de la note du haut du 1er accord, déjà posé (orange plein) ; au départ (écran 25) il était déjà sur la note du haut alors que la consigne demande la note du bas
    symptome: incompris | gravite: contourne-en-0-gestes | revu: non | banc?: non | promesse: a-anneau
  - geste: 52 | ecran.vu: 53 | cible: bouton « Mélodie · mesure 2/8 Encore 1 note en pointillé à toucher » | geste.type: touche
    attendu: au début de l'étape « écris les 8 notes de la mesure 2 », la ligne dit 8 notes à toucher | obtenu: elle dit « Encore 1 note » jusqu'à « Je le fais », puis « Encore 8 notes » (écran 54)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 26 | cible: bouton « Accords · mesure 1/8 Accords en pointillé à toucher : 3 » | geste.type: touche
    attendu: un retour de son quand un accord se pose (la basse et la mélodie font « 1 note du synthé ») | obtenu: « Son : rien ne joue » à chaque accord posé (écrans 26 à 29, 32 à 41)
    symptome: rien | gravite: finition | revu: oui: ecran 32 | banc?: son | promesse: -
defauts-avant:
  - a-V1 | corrigé | ecran: 5, 13, 25, 31, 36, 44, 54 | à chaque mesure demandée, dès l'arrivée sur la grille (avant tout « Touche ici »), toutes les notes en pointillé étaient visibles au-dessus du panneau du tuto, sans flèche ↑/↓ ; chaque note posée d'une seule touche : basse mes. 1 (6 notes, écran 5), basse mes. 4 (6 notes, écran 13), accords mes. 1 (4 accords, écran 25), accords mes. 3 (4 accords, écran 31), accords mes. 4 (5 accords, écran 36, atteinte par la ligne « → Touche ici » puisque l'étape demande deux mesures), mélodie mes. 1 (8 notes, écran 44), mélodie mes. 2 (8 notes, écran 54, juste après « Je le fais »)
  - D3 | corrigé | ecran: 25 | la consigne se lit toujours : première ligne entière ; quand elle est trop longue elle finit par « se p… » et « Lire » à droite ; la carte « Télécharger le kit House » est sous la consigne et ne la cache jamais (écrans 1, 4, 24, 43)
  - a-anneau | encore là | ecran: 26 | sur les accords le rond est sur la note du HAUT du 1er accord (la consigne dit la note du bas, écran 25), puis il reste sur cet accord déjà posé au lieu de passer au suivant (écran 26) ; aux accords mes. 3 et 4 il est sur la note du bas (écrans 31, 36) ; basse et mélodie : rond sur une note encore à poser (écrans 5, 13, 44, 54)
