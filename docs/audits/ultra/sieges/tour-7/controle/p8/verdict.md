# Contrôle p8 (code ede109d8, appli 4170af63) : verdict

code testé : ede109d8ada92d293e0cfd3b777f53b38e2e3b68 (code de l'appli 4170af636f90498838f58c7df92a5e8ea7e6f22d)
joué par : sous-agent neuf (un par siège, sans lecture du code)
banc : `scripts/bench/selftest.sh` vert avant les sièges (204 verts, 0 rouge) ; sortie de siège : 0 fichier hors des sièges.

| Siège | Langue / appareil | Chapitres bloqués | Défauts visés | Graves neufs |
|---|---|---|---|---|
| a | fr, téléphone, house ch. 3-5 | 0 | V1 encore là, V2 corrigé, V4 corrigé | 0 |
| b | ja, téléphone, techno ch. 3-5 | 0 | V1, V2, V3 corrigés (tiennent toujours) | 0 |
| c | ko, phone360 130 %, techno ch. 9-12 | 0 | V3 encore là, V4 corrigé | 0 |
| d | fr, téléphone, **techno** ch. 12 | 0 | V1, V2, P1 (étape 1 Facile), P2 (cymbale) corrigés | 0 |
| e | de, tablette 8 Pro, techno ch. 8-13 | 0 | V1 à V4 corrigés (tiennent toujours) | 0 |

Siège d : joué en techno ch. 12 (et non en house) pour atteindre les deux phrases neuves du ch. 12 (étape 1 Facile, cymbale).

## Défauts visés (9)
- a-V1 « Touche ici » sur deux hauteurs éloignées : **encore là**. Les La aigus restent hors de l'écran après le premier toucher, et il faut toucher deux fois (écrans 67 et 85).
- a-V2 consigne sous la carte du kit : **corrigé** (écran 1).
- a-V4 pointillés d'une étape faite en A qui reviennent en B : **corrigé** (écrans 24 et 78).
- c-V3 roulement du Build-up : **encore là**. Le ×2 est déjà posé à la création et aucune étape ne le demande (écran 47).
- c-V4 consigne longue coupée sous la bannière : **corrigé**. Elle finit par « … » et 계속 읽기 l'ouvre. Reste une finition : le « … » est posé sur une syllabe à moitié dessinée (écran 28).
- d-V1 « Montée » absente : **corrigé**. La phrase le dit et la partie créée entre dans le morceau (écrans 20 et 22).
- d-V2 partie vide « Montée » qui donnait « Déjà fait » : **corrigé** (écran 34).
- d-P1 étape 1 Facile, mots contre écran : **corrigé**. Les 4 croix sont sur Claquement de mains, au dernier temps (écran 2).
- d-P2 cymbale « toute première case, encore vide » : **corrigé**. La case est bien vide et en pointillé (écran 9).

Visés corrigés : 7/9.

## Graves
Aucun « grave » n'a été signalé, donc il n'y a rien à revoir et aucun n'est requalifié en moyen.

## Neufs notables (finitions, pas graves)
- d : avec une partie vide nommée « Montée », le tuto dit « Il n'y a pas de partie « Montée » », ce qui est faux (il devrait dire qu'elle est vide). Après le renommage, le message « Partie « Montée » créée » reste affiché et contredit la phrase.
- a : « Copier sur les mesures suivantes » ne fait rien en partie A quand les notes sont en B, mais l'écran dit « Motif posé ! ». Après l'écoute, l'éditeur reste sur la partie B.
- c : au ch. 10, la consigne dit « 엔딩 » alors que le bouton et la partie disent « 끝 ». Un message affirme que le silence du dernier temps est déjà fait.
- e : au ch. 11, « Ich mach's » ne fait que replier le panneau. Les boutons « Am Ende hinzufügen » se déplacent pendant la saisie. « Snare: danach » apparaît sur la feuille du Clap.

## Critère
Le critère est 0 chapitre bloqué, 0 grave neuf et les défauts visés vus comme corrigés.
- 0 bloqué et 0 grave neuf : atteint.
- Défauts visés : pas atteint, car 2 sur 9 sont encore là (a-V1 et c-V3).

Décision : pas de fusion sur ede109d8.
