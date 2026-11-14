# Auto-audit : plan test APK et corrections (2026-11-12, après « Tout tester » v5)

Question de Benjamin : « est-ce qu'on tourne en rond ? » Premier jet par l'agent principal, puis contre-audit par un auditeur neuf (Benjamin : « Recommence l'audit »). Ce fichier garde la version corrigée.

## Verdict
On ne tourne pas tout à fait en rond, mais la convergence est mince et en partie illusoire.

## Chiffres (rapport v4 = rapport-tout-1, v5 = rapport-tout-2)
| Point | v4 | v5 | Statut |
|---|---|---|---|
| Images lentes Sons / Mes projets | 31 % / 25 % | 0 % / 4 % | corrigé, prouvé (lot jank) |
| Images lentes Mix | 48 % | 42 % | en cours (lot mixwake) |
| Hard techno, décrochages | 25 | 0 | NON EXPLIQUÉ : moteur inchangé, seule la mesure a changé ; peut-être lié au robot sorti de l'appli en v4 (non prouvé) |
| Robot coincé dans « Mes fichiers » | 3 | 0 | corrigé |
| Écran éteint | non concluant | lecture tenue, 4 décrochages au rallumage | en cours (lot mixwake) |
| Feux vert / orange / rouge | 22 / 26 / 2 | 20 / 28 / 3 | pas de progrès global (un rouge vient du robot : Liquid DnB) |

## Ce qui n'allait pas
1. **Environ 70 % de l'effort sur le robot**, 30 % sur l'appli (13 des 17 commits locaux du test APK). Rentable pour 2 vrais défauts trouvés, mais 5 passages téléphone au lieu des 5-6 sessions prévues, T2 et T3 non faits.
2. **Le « pire bloc » du rapport est aveugle.** Le robot lit encore la charge lissée et plafonnée ; il ne lit pas le vrai pire bloc ni le plus long écart que le moteur expose depuis le lot hardtechno. Les 28 oranges ne valent rien, et recaler un seuil sur ce capteur (premier jet : « orange dès 70 % ») était une erreur. Correction : brancher le vrai pire bloc ; rouge s'il y a un décrochage, un vrai bloc au-delà de 100 % ou un écart de plus de 2 blocs ; orange entre 70 et 100 % ou un écart de plus d'un bloc.
3. **« Crête 0.87 » partout** : c'est le plafond du limiteur, aucune détection de saturation. À remplacer par le niveau réel.
4. **Travail non poussé** : 17 commits du test APK sur aucune branche distante. Une fin de conteneur les perdrait. À faire passer par le portail en priorité.
5. **Règle d'arrêt du premier jet mal réglée** : « Mix sous 15 % » était inventé (le plan dit vert sous 5 %, orange de 5 à 15 %) ; « Liquid DnB passé » ne teste que le robot ; Hard techno, Mix et écran éteint sont déjà à leur 2e essai.

## Règle d'arrêt corrigée
- Les points déjà à 2 essais (Hard techno, Mix, écran éteint) n'ont plus qu'**un seul** retest (v6). Ce qui reste rouge part dans la liste du tour 6, avec ses chiffres et « non expliqué » si la cause manque.
- Fin du plan test APK : le « Retester les corrections » de la v6 (Mix, écran éteint, Hard techno, Techno, Liquid DnB) lu avec le vrai pire bloc. Seuils du plan : Mix vert sous 5 %, acceptable jusqu'à 15 %.
- Pas de nouveau « Tout tester » complet avant le tour 6. T3 (questions à l'oreille) en un seul passage.

## Chemin vers le tour 6
1. Faire passer eq/tour-5-testapk par le portail (relecture neuve), puis pousser.
2. Brancher le vrai pire bloc et le plus long écart dans le rapport et les feux.
3. Finir mixwake (relecture neuve, portail), puis Liquid DnB et « Retester les corrections ».
4. Une seule v6 : Benjamin lance « Retester les corrections ».
5. Le reste rouge va dans la liste du tour 6 ; T3 en un passage.
6. Étape 5 (tour 6), les auditeurs partent du rapport v6.
