code testé : 57297b698f1773789eb00fab1bb71671db9e8b9d
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
chapitres:
  - chapitre 5 « La basse » : fini (écran 20, « Bravo, chapitre fini ! »)
  - chapitre 6 « Le piano » : fini (écran 44, « Bravo, chapitre fini ! »)
  - chapitre 7 « La petite boucle » : fini (écran 67, « Bravo, chapitre fini ! »)
buts-atteints: chap1 oui (écran 20) | chap3 oui (écran 67) | bravo oui (écran 20, 44, 67 : « Bravo, chapitre fini ! »)
promesse: id=a-V1 | texte: « À toi : écris les 8 notes de la mesure 2. » | ecran: 57 | tenue: non | pourquoi: juste après « Je le fais », 7 notes sur 8 sont visibles, la ligne dit « ↓ Encore 7 notes en pointillé à … » avec « Touche ici » ; la 8e est hors de vue plus bas
promesse: id=D3 | texte: « À toi : touche la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se p… » | ecran: 25 | tenue: oui | pourquoi: première ligne entière, coupe finie par « … », le bouton « Lire » montre tout (écran 26) ; la carte du kit est sous la consigne, jamais dessus
promesse: id=a-anneau | texte: « À toi : touche la note du bas de chaque accord … » | ecran: 25 | tenue: non | pourquoi: à l'écran 25 l'anneau est sur la note du HAUT de l'accord alors que la consigne dit la note du bas ; après « Je le fais » (écran 27) il est bien sur la note du bas
hesitations:
  - ecran: 46 | cherchais: pourquoi « Encore 6 notes » alors que la consigne dit « les 8 notes » | regarde: deux notes étaient cachées plus bas, le compteur ne les comptait pas encore
  - ecran: 52 | cherchais: le compteur passe de « Encore 1 note » à « Encore 2 notes en pointillé, plus bas ↓ » | regarde: j'ai touché « Touche ici » pour descendre
  - ecran: 30 | cherchais: où toucher ensuite, l'anneau avait disparu | regarde: les pointillés restants seuls
abandon: jamais
incidents-banc:
  - geste: 37 | attente: 0 | vu: plafond d'images atteint (7 pour 34 gestes), j'ai joué la mesure 4 des accords à l'aveugle d'après la mesure 3
defauts:
  - geste: 56 | ecran.vu: 57 | cible: bouton « Mélodie · mesure 2/8 ↓ Encore 7 notes en pointillé à toucher Touche ici » | geste.type: touche
    attendu: après « Je le fais », les 8 notes en pointillé de la mesure 2 toutes visibles au-dessus du panneau, sans flèche
    obtenu: 7 visibles, 1 cachée plus bas ; flèche ↓ et « Touche ici » sur la ligne ; il faut toucher « Touche ici » pour la voir (même chose à l'écran 46 pour la mesure 1, 2 notes cachées)
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 46 | banc?: non | promesse: a-V1
  - geste: 46 | ecran.vu: 52 | cible: bouton « Mélodie · mesure 1/8 Encore 2 notes en pointillé, plus bas ↓ Touche ici » | geste.type: touche
    attendu: le compteur compte toutes les notes demandées (« les 8 notes ») dès le début
    obtenu: « Encore 6 notes » au début, puis « Encore 1 note », puis remonte à « Encore 2 notes … plus bas » ; idem mesure 2 (« Encore 1 » → « Encore 1 … plus bas »), et à l'écran 56 la ligne disait « Encore 1 note » avant même « Je le fais » pour 8 notes demandées
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 64 | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 25 | cible: icône « À toi : touche la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. Il y a 4 coups. Lire » ⌃ | geste.type: touche
    attendu: l'anneau sur la note du bas du premier accord, comme dit la consigne
    obtenu: l'anneau est sur la note du haut (près de Do4) ; il ne passe sur la note du bas qu'après « Lire » puis « Je le fais » (écran 27)
    symptome: incompris
    gravite: finition | revu: oui: ecran 27 | banc?: non | promesse: a-anneau
  - geste: 46 | ecran.vu: 46 | cible: bouton « Mélodie · mesure 1/8 ↓ Encore 6 notes en pointillé à toucher Touche ici » | geste.type: touche
    attendu: la ligne d'aide lisible en entier
    obtenu: à l'image, la ligne est coupée « ↓ Encore 6 notes en pointillé à … » à cause du bouton « Touche ici » (même chose écran 57)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 57 | banc?: non | promesse: -
  - geste: 29 | ecran.vu: 30 | cible: bouton « Accords · mesure 1/8 Accords en pointillé à toucher : 1 » | geste.type: touche
    attendu: l'anneau « Touche ici » suit sur l'accord suivant à poser
    obtenu: après le premier accord posé, plus aucun anneau sur les accords restants (écrans 30 et 40)
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: a-anneau
  - geste: 27 | ecran.vu: 28 | cible: bouton « Accords · mesure 1/8 Accords en pointillé à toucher : 3 » | geste.type: touche
    attendu: entendre l'accord posé, comme les notes de basse (« 1 note du synthé »)
    obtenu: « Son : rien ne joue » à chaque accord posé (écrans 28 à 43)
    symptome: rien
    gravite: finition | revu: oui: ecran 41 | banc?: son | promesse: -
defauts-avant:
  - a-V1 | encore là | ecran: 57 | juste après « Je le fais » (mesure 2 de la mélodie), une note sur 8 est cachée sous la grille, la ligne porte « ↓ … Touche ici » ; pareil mesure 1 (écran 46, 2 cachées). Pour la basse (écrans 5, 13) et les accords (écrans 27, 33, 40) tout était visible et une touche suffisait
  - D3 | corrigé | ecran: 25 | consigne lisible : première ligne entière, coupe finie par « … », « Lire » (pas « Lire la suite ») ouvre tout le texte (écran 26) ; la carte du kit House est sous la consigne, ne la cache pas (écrans 1, 21, 26)
  - a-anneau | autre | ecran: 25 | l'anneau est bien sur une note à poser, mais à l'écran 25 sur la note du HAUT de l'accord alors que la consigne dit la note du bas ; après « Je le fais » (écrans 27, 33) il est sur la note du bas ; après le 1er accord il disparaît (écrans 30, 40)
