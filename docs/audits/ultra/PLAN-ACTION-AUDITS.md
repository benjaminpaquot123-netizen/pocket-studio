# Plan d'action des audits : la prochaine étape

Écrit le 2026-11-09 pour Benjamin. **Rien n'est lancé : ce plan attend ton ordre.**

Il part de quatre choses déjà livrées :
- l'audit « banc crédible » : 5 sur 10 (`docs/audits/ultra/AUDIT-BANC-CREDIBLE.md`) ;
- l'audit « prise en main et design » : 6 sur 10 et 7 sur 10 (`docs/audits/AUDIT-PRISE-EN-MAIN.md`) ;
- la synthèse du tour 5 : 3 bloquants, 5 graves (`docs/audits/ultra/tour-5-synthese.md`) ;
- ta recette du 8 novembre : 8 points rouges (`docs/audits/ultra/retours-benjamin.md`).

Le détail technique de chaque ligne reste dans `docs/audits/ultra/PLAN-RESTE.md` (banc) et dans la section 4 de l'audit de prise en main (appli). Ce plan-ci donne l'ordre, les durées et le nombre d'agents.

## 1. En trois lignes

1. Le banc dit vrai sur ses bases, mais son juge accepte encore des verdicts sans preuve, et il ne sait ni enregistrer un potard qu'on tourne, ni faire attendre un pad Live. On répare ça d'abord : une vingtaine d'heures pour passer de 5 à 8.
2. Ensuite on corrige l'appli là où tu t'es perdu : partager ton morceau et le retrouver, puis les trois bloquants du tour 5.
3. Le prochain tour d'audit vient après, en petit : six testeurs au lieu de vingt-quatre, pour vérifier que rien ne revient.

## 2. Les règles du jeu (quota et confiance)

- **Un seul agent à la fois pour les réparations et les corrections** (consigne de Benjamin du 2026-11-09) : l'auteur travaille, puis s'arrête, puis un relecteur neuf qui n'a pas écrit le code prend la suite. **Plusieurs agents pour les audits** (il l'a précisé le même soir) : trois testeurs au plus en même temps pendant un tour. J'annonce le nombre avant chaque lancement.
- **Chaque réparation se prouve** : un test rouge d'abord, la correction ensuite, la relecture, puis la fusion par le script. Pas de « ça devrait marcher ».
- **Une étape finie** : trois lignes de nouvelles, un envoi sur la branche, et je m'arrête. Tu relances avec « go » et le numéro de l'étape, ou « continue ».
- **Pas d'APK et pas d'essai sur ton téléphone** tant que tu ne le demandes pas.
- Une ligne qui prend deux fois sa durée est coupée ou reportée, et je te le dis.
- Les durées sont celles estimées par les audits. Ce sont des ordres de grandeur, pas des promesses.

## 3. Vue d'ensemble

| Étape | Quoi | Durée estimée | Agents en même temps |
|---|---|---|---|
| 0 | Finir ce qui tourne | 1 h | 1 |
| 1 | Banc : le juge, la mémoire, le faux moteur | 20 h | 1 |
| 2 | Appli, première vague : partager et retrouver, les trois bloquants | 30 h | 1 |
| 3 | Banc : pads Live, fin de l'audit, fin du plan | 55 h | 1 |
| 4 | Appli, deuxième vague : le reste des dix chantiers | 70 h | 1 |
| 5 | Tour 6 : le petit tour de contrôle | 6 testeurs | 3 |

Les étapes 1 et 3 valent pour le banc. Les étapes 2 et 4 touchent l'appli : elles ne partent que sur ton go.

## 4. Les étapes en détail

### Étape 0 : finir ce qui tourne (en cours aujourd'hui)

1. Fusion de l'équipe « séance 5a » : faite, contrôles verts, envoyée (4d90302).
2. Équipe « banc 4.5 » (le détecteur de fuites entre tests) : l'auteur finit deux retouches, un relecteur relit le delta, puis fusion.
3. Cinq gardes inscrites dans la mémoire des verdicts. Le juge doit alors afficher zéro écart.
4. État de la CI relevé par programme et noté.
5. Journal et suivi à jour, ateliers finis rangés. Puis arrêt.

### Étape 1 : le banc passe de 5 à 8 (environ 20 h)

| Lot | En clair | Lignes du plan | Durée |
|---|---|---|---|
| 1. Le juge | Le programme qui juge le chantier refuse un verdict sans preuve. Le contrôle du banc devient rouge si une classe de tests est sautée en entier, et il joue toutes les classes qui portent un défaut connu. La preuve moteur du pad qui redémarre devient lisible par programme. | A.10 (le seul bloquant), A.3, A.4, A.12 | 6 h |
| 2. La mémoire | Trois verdicts mal rangés sont corrigés. Tes 16 essais du 8 novembre sont inscrits sur leurs lignes. La limite du banc sur les pads est écrite noir sur blanc. | A.2, A.5, A.6, A.14, A.11 | 4 h |
| 3. Le faux moteur | Une prise de voix reste quand le morceau finit tout seul. Un potard tourné pendant la lecture s'enregistre. Une seule horloge, décompte compris. Les positions sont comptées depuis le début de la partie. Deux gardes sont branchées sur le vrai code de l'appli. | A.7, A.8, A.9, A.15, A.13 | 9 h 30 |

Sortie de l'étape 1 :
- le juge strict a ses propres tests, et il liste les vrais écarts. On en attend une vingtaine : les verdicts encore sans preuve. Chacun devient une ligne nommée de l'étape 3, aucun n'est caché ;
- le contrôle complet du banc est vert, sans test sauté sans raison ;
- chaque réparation a sa paire rouge puis vert dans l'historique.

### Étape 2 : l'appli, première vague (environ 30 h, sur ton go)

« On en a gros » : c'est là que tu as été bloqué le 8 novembre.

| Quoi | Source | Durée |
|---|---|---|
| Partager et retrouver en 3 gestes : la longueur dans « Prêt à partager ? », « Mes fichiers » sous le bouton Partager, « C'est prêt » au retour, le .zip listé. Le mot « Partager » partout (ta réponse 1A). | Chantier 1 | 6 h |
| Trois composants partagés : la ligne-lien qui a l'air d'un lien, le repli qui résume ce qu'il cache, la pastille de piste dont le texte n'est plus coupé. Une seule icône Partager. Les règles 12 à 22 ajoutées à `docs/DESIGN.md`. | Chantier 2 | 12 h |
| Les trois bloquants du tour 5 : changer de partie en Mode simple ; la mélodie perdue après un Pad Live ; l'étape Swing du tuto Pro (à vérifier d'abord, elle n'a pas été contre-expertisée). | Synthèse du tour 5 | à chiffrer le premier jour, ordre de grandeur 8 h |
| Deux bugs d'export : 16 mesures demandées, 8 exportées ; le .zip absent de la liste. | T5-8MES, T5-ZIP | ordre de grandeur 4 h |

Preuve attendue : les tests et les captures d'écran nommés dans l'audit, regardées une par une, et une CI verte.

### Étape 3 : le banc jusqu'au bout (environ 55 h)

| Lot | En clair | Lignes du plan | Durée |
|---|---|---|---|
| 4. Les pads Live | Sur le banc, un pad Live attend la mesure comme sur ton téléphone, et le banc écoute les ordres reçus par le moteur, plus ce qu'il a écrit lui-même. | A.16 | 8 h |
| 5. Finir l'audit du banc | Rejouer ce que l'audit n'a pas pu lancer. Auditer les quatre dimensions laissées de côté : le portail et les scripts, le réalisme du testeur, le dépôt, la méthode. Deux agents, pas douze. | A.1, A.17 | 7 h 30 |
| 6. La fin du plan | « attends » qui ne dure plus deux minutes, l'écran vide au démarrage, la taille d'un export (5 h 30). Les contrôles restants de la séance 5 (9 h 30). Les verdicts encore à trancher et le tableau de bord complet (13 h). Les équipes de correction aussi fiables que le banc, avec un essai à blanc (7 h 30). Dix contrôles complets de suite et un tour de fumée à 4 sièges (4 h 30). | 4.4, 4.6, 4.7 ; 5.1 à 5.4, 5.10, 5.11 ; 6.2, 6.3, 6.5 ; 7.3 à 7.7 ; critères 1 à 8 | 40 h |

Sortie de l'étape 3 : les huit critères de sortie de `PLAN-RESTE.md` section 5 sont tenus, et le juge strict affiche zéro écart. Le banc est bon pour le service.

### Étape 4 : l'appli, deuxième vague (environ 70 h, sur ton go)

Dans l'ordre conseillé par l'audit :

| Chantier | En clair | Durée |
|---|---|---|
| 3. Textes | « C'est pas faux » : un mot par chose. Un seul nom pour l'assistant, plus de dB en Mode simple. | 8 h |
| 4. Navigation | Les deux ▶ gardés mais nommés : « ▶ cette partie » en haut, « ▶ le morceau » en bas (ta réponse 2B). La barre du pouce ne disparaît plus quand le texte est agrandi. | 9 h |
| 5. Navigation | Le même « Plus » dans les deux modes, Recette comprise. « Simple ou Pro ? » n'est plus demandé au deuxième morceau (ta réponse 3A). | 12 h |
| 6. Accueil | Les accueils montrent qu'ils défilent. « Un style en une touche » tient sa promesse. | 7 h |
| 7. Pad Live | « Tout couper » visible, et il coupe tout, tempo compris (4A). Un glissé ne déclenche plus un effet. Avec le défaut moteur du pad qui redémarre (R.7, 3 h). | 12 h |
| 8. Courbes | La nouvelle courbe se montre, l'option reste visible, « Voir le réglage » y mène. | 5 h |
| 9. Accessibilité | Les feuilles défilent en paysage. Trois fiches « ? » là où un débutant bloque. | 5 h |
| 10. Remix | Une seule carte conseillée, un bouton clair, l'alerte « la fin sera coupée ». | 4 h |

S'y ajoutent les 5 graves du tour 5 et les points rouges de ta recette qui ne sont pas déjà dans un chantier. Le chantier 7 vient après le lot 4 du banc : on ne corrige pas les pads avec un banc qui ne sait pas les jouer.

### Étape 5 : le tour 6, petit tour de contrôle

- **Condition** : l'étape 3 est finie, et au moins l'étape 2 est fusionnée. Auditer avant, ce serait retrouver les mêmes défauts en payant deux fois.
- **Qui** : 6 testeurs simulés, un par zone de l'appli. Fiches de route fabriquées par programme, plus la liste « œil neuf » de l'audit de prise en main (section 7). Tablette à 130 % et téléphone à 100 %.
- **Déroulé** : deux lots de 3 testeurs, puis un lecteur pour les défauts qui reviennent, puis une synthèse. Huit agents en tout, trois au plus en même temps.
- **Ce que tu reçois** : un tableau de bord, chaque défaut avec sa preuve, et les deux notes refaites. Objectif : aucun bloquant, aucun grave sur le siège « œil neuf ».

## 5. Ta part, quand tu auras le temps (rien d'urgent)

Cinq essais sur ton téléphone, proposés par l'audit du banc. Je te les demanderai ici, un à la fois, avec les gestes exacts :
1. Mode simple, Mon morceau, Enregistrer ma voix, et laisser le morceau finir tout seul.
2. Mode Pro, le rond rouge de la carte Automation pendant que tu tournes un volume de piste.
3. La démo « Le beat de départ », puis attendre 10 secondes à « Simple ou Pro ? ».
4. Un export MIDI, puis revenir : l'alerte « le morceau a changé depuis cet export » apparaît-elle ?
5. Mode simple, Clavier : enregistrer sur la partie A pendant que la partie B joue.

## 6. Trois questions (réponse du type « 1A, 2A, 3A »)

1. **Par quoi on redémarre ?**
   - A. (conseillé) Dans l'ordre du plan : étapes 1, 2, 3, 4, 5. Conséquence : le partage est réparé après 50 h environ, et jugé par un juge sérieux.
   - B. Tout le banc d'abord : étapes 1, 3, puis 2, 4, 5. Conséquence : le banc est fini plus tôt, l'appli attend 75 h.
   - C. L'appli d'abord : étapes 2, 1, 3, 4, 5. Conséquence : tu vois le partage réparé plus vite, mais jugé par un juge encore trop gentil.
2. **La taille du tour 6 ?**
   - A. (conseillé) 6 testeurs. Conséquence : quota léger, on vérifie surtout ce qui a été corrigé.
   - B. 12 testeurs. Conséquence : deux fois le quota, plus d'écrans couverts.
   - C. 24 testeurs, comme le tour 5. Conséquence : le plus complet, le plus cher.
3. **Les deux vieux ateliers gardés de côté ?** L'un contient le battement de cœur d'ouverture fait avec BenStudio, l'autre tes notes de retours du 27 octobre, jamais enregistrées.
   - A. (conseillé) Je range les notes dans le dépôt, sans la ligne sur ton statut, et je laisse le battement de cœur de côté. Conséquence : les notes sont sauvées, le son reste privé et disparaît quand la session se ferme.
   - B. Je garde les deux tels quels. Conséquence : tout disparaît quand la session se ferme.
   - C. Je jette les deux tout de suite.
