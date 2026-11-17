# Contrôle p8b (code 11ca024b) : verdict

code testé : 11ca024b94945ffae964263f27e8342aa3770316
joué par : sous-agent neuf (un par siège, sans lecture du code)
banc : `scripts/bench/selftest.sh` vert avant les sièges (204 verts, 0 rouge, 26 sautés) ; sortie de siège : 0 fichier hors des sièges.

| Siège | Langue / appareil | Chapitres bloqués | Défauts visés | Graves neufs |
|---|---|---|---|---|
| a | fr, téléphone, house ch. 3-5 | 0 | V1 encore là, V2 corrigé, V4 corrigé | 0 |
| b | ja, téléphone, techno ch. 3-5 | 0 | V1, V2, V3 corrigés (tiennent toujours) | 0 |
| c | ko, phone360 130 %, techno ch. 9-12 | 0 | V3 corrigé, V4 corrigé (finition du « … » toujours là) | 0 |
| d | fr, téléphone, techno ch. 12 | 0 | V1, V2, P1, P2 corrigés | 0 |
| e | de, tablette 8 Pro, techno ch. 8-13 | 0 | V2, V3 corrigés ; V1, V4 « autre » (voir neufs) | 0 |

Hors des 9 visés, les sièges b et e ont rejoué leurs anciens défauts : b-V1 à b-V3 tiennent ; e-V2 et e-V3 tiennent ; e-V1 et e-V4 sont classés « autre » (finitions neuves, pas un retour du défaut).

## Défauts visés (9)
- a-V1 « Touche ici » sur deux hauteurs éloignées : **encore là**. Mesures 1 et 4 : le premier toucher ne montre que 2 notes sur 6, la flèche passe à ↓ et il faut toucher une deuxième fois (écrans 66 et 76). Même chose en japonais au siège b (« ここをタップ », mesure 4, écran 47 : 3 touchers).
- a-V2 consigne sous la carte du kit : **corrigé** (écran 1). Reste une finition : la consigne est coupée à une ligne, et « temps 2 et 4 » ne se lit qu'avec « Lire la suite ».
- a-V4 pointillés d'une étape faite en A qui reviennent en B : **corrigé** (écran 86).
- c-V3 roulement du Build-up : **corrigé**. Aucun ×2 ni « 2번 반복 » à la création (écran 29). Le siège e le voit aussi en allemand (écran 62). Reste une finition : le texte de création dit que le dernier temps est déjà silencieux, alors que la mesure 4 a ses 16 claps et que le ch. 12 demande de les effacer.
- c-V4 consigne longue coupée sous la bannière : **corrigé**. Elle finit par « … » et 계속 읽기 l'ouvre. La finition est toujours là : le « … » mord la moitié de « 박 » (écran 21), et 계속 읽기 fait défiler une petite fenêtre au lieu de tout montrer.
- d-V1 « Montée » absente : **corrigé**. La phrase le dit après le renommage, et la partie créée entre dans le morceau (écrans 11 et 47).
- d-V2 partie vide « Montée » qui donnait « Déjà fait » : **corrigé**. Plus de « Déjà fait » (écran 22). La phrase fausse « pas de partie Montée » est toujours là (déjà notée en neuf au p8).
- d-P1 étape 1 Facile, mots contre écran : **corrigé**. Exactement 4 croix, toutes sur Claquement de mains, au temps 4 de la mesure 4 (écran 23).
- d-P2 cymbale « toute première case, encore vide » : **corrigé**. La case 1 est vide et en pointillé ; la toucher finit le chapitre (écrans 30 et 31).

Visés corrigés : 8/9.

## Graves
Aucun « grave » n'a été signalé, donc il n'y a rien à revoir et aucun n'est requalifié en moyen.

## Neufs notables (finitions, pas graves)
- e : au ch. 13 Pro, « Ich mach's » ouvre seulement la feuille d'effet (Reverb-Send reste à 0 %, la courbe n'est pas tracée) : 2 gestes de contournement. C'est le même genre de défaut que celui du ch. 11 au p8. Au ch. 12, l'éditeur reste sur l'onglet « Rolls » de l'étape 1, donc toucher une croix pose un ×2 au lieu d'effacer. En haut, on lit « Drop 2 Takt 81/128 » alors qu'on travaille dans un autre morceau. Pendant le chargement du kit, le texte de l'étape est raccourci. Dix mots allemands sont faux ou maladroits (voir le rapport).
- d : la Montée créée se place après le Break, avant « F · Pont ». Elle n'est donc pas « juste avant le Drop 2 », comme le dit la consigne.
- c : en coréen, aucune croix rouge n'est visible au ch. 12 sur les 4 claps à effacer. Ils sont jaunes comme les autres, et la phrase « 여기 점선 칸이 1개 남았어요 » n'est jamais apparue. La consigne dit « 엔딩 » alors que le bouton dit « 끝 », comme au p8. « 인트로 2 » ne cite pas la cymbale retirée.
- b : « なぜ？ » remplace la consigne au lieu de s'y ajouter. Le triangle ▸ est collé au bord. La puce « + トラ→ » est coupée. Les étapes d'écoute n'ont pas le mot « 成功 ».
- a : en partie B, le repère du haut affiche « A mesure 1/16 ». Le titre de la grille est à moitié caché sous « Partie : B ».

## Critère
Le critère est 0 chapitre bloqué, 0 grave neuf et les défauts visés vus comme corrigés.
- 0 bloqué et 0 grave neuf : atteint.
- Défauts visés : pas atteint, car 1 sur 9 est encore là (a-V1, prioritaire ; il se reproduit aussi en japonais).

Décision : pas de fusion sur 11ca024b.
