# Plan test APK : « BenStudio Test » (demande de Benjamin, 2026-11-11)

Statut : **plan seulement, rien n'est construit**. Version 2, après audit du plan (voir la fin). À lancer quand Benjamin dit « lance le plan test apk ». Il passe **avant** le grand audit final (`docs/PLAN-AUDIT-FINAL.md`), pour que les auditeurs partent de mesures prises sur le vrai téléphone.

## Le but

Une appli à part, **BenStudio Test**, qui teste presque tout **toute seule** sur le téléphone et la tablette. Benjamin en fait le moins possible : il lance, il pose le téléphone, il répond à 8 questions, il renvoie un fichier.

Le « banc parfait », c'est trois preuves que seul un vrai appareil peut donner :
1. le téléphone tient la charge (pas de craquement, pas de fondu, pas de plantage) ;
2. le téléphone sonne comme le banc de l'agent (sinon tout ce que l'agent mesure chez lui ne vaut rien) ;
3. les écrans tiennent sur ses deux appareils, dans toutes les positions.

## Ce que Benjamin aura à faire, en tout

1. Installer BenStudio Test (elle se met **à côté** de la vraie appli, ses projets ne risquent rien). Sur l'écran de préparation : accepter le micro et les notifications (2 touchers).
2. Une seule fois, au tout début du chantier : l'« essai éclair » de 5 min, puis « Envoyer le rapport ». Il prouve que le robot marche sur son téléphone avant qu'on construise le reste.
3. Le grand passage : chargeur branché, pièce calme, « Ne pas déranger », toucher **« Tout tester »**, laisser faire environ 70 min.
4. Répondre aux **8 questions** (10 min, casque et enceinte Bluetooth à portée de main).
5. Toucher **« Envoyer le rapport »** et le donner à l'agent.
6. Sur la tablette : les points 1, 3 et 5 seulement (aucune question).

## Comment ça marche

### Le robot est dans l'appli (plus besoin d'ordinateur ni de mode test d'Android)
- Le robot vit dans BenStudio Test et touche les vrais boutons par leurs étiquettes de test, comme le robot des parcours existant (`app/src/sharedTest/.../journey/Journey.kt`). Les parcours déjà écrits sont repris : `Journey` ne se sert que de six gestes (existe, attendre, toucher, défiler, patienter, lister), qu'on met derrière une petite interface avec deux pilotes : celui des tests actuels et le nouveau pilote intégré.
- Le pilote intégré lit l'écran par l'arbre d'accessibilité de Compose (`ViewRootForTest.semanticsOwner`, toutes les fenêtres par `WindowInspector`) et envoie de vrais touchers à la vue. **Il se prouve sur la machine de l'agent, dans Robolectric**, sans émulateur.
- Le code du robot n'est compilé que dans BenStudio Test (option de compilation `-PbenTest` : autre identifiant `com.pocketstudio.test`, autre nom, autre couleur d'icône, dossier de sources en plus). Pas de « saveurs » Gradle : les commandes et la CI actuelles ne changent pas. La vraie appli ne contient aucune ligne du robot (contrôle `dexdump` déjà en place).
- Compilée **optimisée comme la vraie** (pas en mode débogage), sinon les mesures de charge et de fluidité sont fausses. Le rapport porte la version et le commit, pour comparer avant / après.

### Le passage tient debout tout seul
- Carnet de bord écrit sur le disque après chaque étape. Si l'appli plante, la relancer reprend à l'étape suivante et le plantage est noté avec le nom de l'étape.
- Chaque étape a une durée maximale ; une étape ratée n'arrête pas les autres.
- Dérangement (appel, notification qui baisse le son) : repéré par la perte du « focus audio », l'étape est refaite à la fin.
- Température et bridage du téléphone notés tout du long. Les deux styles les plus lourds (jump up, dnb) sont joués au début (à froid) et à la fin (à chaud).
- Ses propres mesures, 4 par seconde, sur un fil à lui : celles de l'appli s'arrêtent quand l'écran s'éteint.

## Niveau 1 : le robot mesure (0 geste)

| Mesure | Comment | Durée |
|---|---|---|
| Charge et craquements | Les 37 styles, morceau de l'assistant, 45 s en direct chacun : décrochages, charge du fil audio (moyenne, pire bloc), niveau par seconde, alerte fondu / silence / saut (règles de `live-bench`) | 30 min |
| Même son que le banc | Chaque style rendu hors ligne sur le téléphone, réduit à une empreinte (niveau par seconde dans 8 bandes), comparée à celle du banc Linux | 8 min |
| Exports | M4A, WAV, FLAC, vidéo 9:16 sur jump up : étapes, fichier relu, durée, crête, temps pris, « Annuler » | 5 min |
| Latence | 3 mesures aller-retour, tampon | 1 min |
| Le téléphone s'écoute | D'abord un balayage de sons : la **courbe du haut-parleur** de l'appareil. Puis 8 styles joués et écoutés par le micro : trous de son, craquements, ce qui reste de la basse | 6 min |
| Guide | Suivi jusqu'au bout sur 3 styles ; durée réelle des mesures du Guide | 5 min |
| Remix | Séparation d'un mélange fabriqué par l'appli (pistes connues, donc qualité chiffrée) : temps, mémoire | 5 min |
| Vie de l'appli | Mémoire, fluidité (images lentes par écran), rotation, zéro projet, corbeille, Signaler un bug, « Que veux-tu faire ? » | 5 min |
| Écran éteint | **En dernier** : jump up joue, l'écran s'éteint tout seul, 3 min de mesures, puis le robot arrête tout. Benjamin retrouve le rapport prêt | 4 min |

Le « téléphone qui s'écoute » demande une pièce calme : le robot mesure d'abord le bruit ambiant et le dit s'il y en a trop. Il règle le volume lui-même et le remet. L'analyse se fait **sur le téléphone** : seuls des chiffres sortent, plus 2 secondes de son autour d'une anomalie (affiché clairement avant l'envoi). Aucun enregistrement de la pièce ne va dans le dépôt public.

## Niveau 2 : le robot vérifie les écrans (0 geste)

Le robot visite tous les écrans en Mode simple et complet, à 100 % et 130 %, debout et couché, et **contrôle lui-même** : texte coupé, bouton hors de l'écran, cible plus petite que 48 dp, boutons qui se chevauchent, fenêtre plus grande que l'écran. Il ne garde une capture que des écrans en défaut, plus un jeu de référence, rangés en planches. L'agent regarde une vingtaine de planches, pas mille images. Même passage sur la tablette.

## Niveau 3 : les 8 questions de Benjamin (10 min)

Un bandeau flottant, une question à la fois, ✅ ou ❌. Le robot a déjà mis le bon morceau sur le bon écran et lancé la lecture. Un ❌ prend seul la capture, le journal et les mesures ; un mot d'explication est facultatif.

1. à 4. « Ça tape ? » sur le haut-parleur : jump up, house, trap (les 808), techno, 30 s chacun.
5. Casque filaire : la basse est là, le son est large.
6. Bluetooth : brancher, puis couper en pleine lecture. Le son suit, sans plantage.
7. Jouer avec les doigts : pads à trois doigts et clavier. Pas de retard gênant.
8. Trois gestes retour depuis le bord gauche sur Sons › Batterie. Le robot lit lui-même si le kick est resté au centre : aucune réponse à donner.

En option, s'il a envie : séparer une vraie chanson et écouter la voix seule.

Sortis de la liste de Benjamin, car le robot ou l'agent s'en charge : les 17 points de la recette actuelle sauf le son à l'oreille, le casque et le geste retour ; l'enregistrement au micro (le robot enregistre et vérifie qu'il y a du son) ; le partage (envoyer le rapport passe par le même chemin, et chaque fichier exporté est relu).

## Les feux (valeurs de départ, recalées après le premier passage)

| Mesure | Vert | Orange | Rouge |
|---|---|---|---|
| Décrochages par style | 0 | 1 ou 2 | 3 et plus |
| Pire bloc du fil audio | sous 50 % | 50 à 80 % | plus de 80 % |
| Niveau en direct, écart avec le banc | 1 dB au plus | 2 dB au plus | plus de 2 dB |
| Empreinte de l'export, écart par bande | 0,5 dB au plus | 1,5 dB au plus | au-delà |
| Export | relu, durée à 0,1 s près, crête sous −1 dBTP | plus de 2 fois le temps prévu | échec ou blocage |
| Latence aller-retour | 50 ms au plus (44,5 mesurés en phase 0) | 70 ms au plus | au-delà |
| Images lentes par écran | sous 5 % | 5 à 15 % | au-delà |
| Défauts de mise en page | 0 | cibles trop petites | texte coupé, bouton hors écran |
| Plantage, valeur invalide (NaN) | aucun | | un seul |

L'écran de fin dit le verdict en clair (« 34 verts, 2 oranges, 1 rouge »), sans attendre l'agent.

## Le rapport

Un zip de moins de 15 Mo, bouton « Envoyer le rapport » (rien ne part tout seul) : `rapport.md`, `mesures.json`, les planches de captures, le journal, la courbe du haut-parleur. L'agent en tire :
- le tableau **téléphone contre banc Linux** par style : le vrai facteur de lenteur remplace le « 3 fois plus lent » supposé de `cpu-bench` (la tablette, plus modeste, devient le vrai juge du « petit téléphone ») ;
- la **courbe du haut-parleur** du Honor et de la tablette, mise dans le banc Linux : l'agent écoute désormais chaque morceau « comme sur le téléphone de Benjamin », à la place du filtre 300 Hz deviné ;
- les défauts d'écran et les ❌ de Benjamin, avec leur contexte.

Les chiffres (pas les captures ni le son) sont rangés dans `docs/tests-telephone/`. Ces listes sont l'entrée du grand audit. BenStudio Test se relance telle quelle après les corrections : comparaison avant / après automatique.

## Étapes

| Étape | Contenu | Preuve |
|---|---|---|
| T0 | Option `-PbenTest`, pilote intégré, carnet de bord, zip, **essai éclair** (3 styles, 1 export, 5 écrans, 1 question) | Robolectric ici, puis l'essai de 5 min de Benjamin. On ne construit rien d'autre avant son retour. |
| T1 | Niveau 1 complet, feux | Tests Linux des analyses (empreinte, courbe, trous de son) sur des sons fabriqués |
| T2 | Niveau 2 : visite et contrôles de mise en page | Robolectric : un écran volontairement cassé doit être attrapé |
| T3 | Niveau 3 : bandeau, 8 questions, écran de fin | Captures Roborazzi |
| T4 | Relecture neuve (une seule), APK complet, mode d'emploi en 5 lignes | |
| T5 | Lecture des rapports (téléphone, tablette), tableau, courbe dans le banc, seuils recalés. Puis le grand audit. | |

Cinq à six sessions, un agent à la fois (mode économie).

## Risques et ce qui reste à prouver

- **Livrer l'APK** (48 Mo) depuis la machine de l'agent n'a jamais été fait : à prouver en T0. Repli : une compilation GitHub manuelle, une fois par version de BenStudio Test, qui ne se lance que si Benjamin dit oui (sa consigne « plus de compilation sur GitHub » tient).
- Le pilote intégré sous une appli optimisée (R8) : les étiquettes de test doivent survivre. Prouvé par l'essai éclair.
- MagicOS peut tuer l'appli écran éteint : un toucher de plus sur l'écran de préparation (« Lancement d'applications »), facultatif ; sans lui, l'étape « écran éteint » est marquée « non concluante », pas rouge.
- Le micro du téléphone a sa propre couleur : la courbe mesurée est celle du haut-parleur et du micro ensemble. Assez bonne pour comparer des styles entre eux, pas une mesure de laboratoire.
- Deux constantes d'action des services portent le nom du paquet en dur : à vérifier qu'elles ne se croisent pas avec la vraie appli installée à côté.
- Hors du plan : générer un son (modèle de 858 Mo, sauf demande), le Play Store, d'autres téléphones que les siens, le goût (c'est le travail des testeurs de l'audit).

## Audit du plan (2026-11-11) : ce qui a été corrigé

| Dans la version 1 | Problème trouvé | Version 2 |
|---|---|---|
| Robot lancé par le « mode test » d'Android depuis l'appli | Impossible à prouver ici (pas d'émulateur : la machine n'a pas de KVM) ; sans ordinateur ce mode n'a ni capture système ni autorisations automatiques | Robot intégré à l'appli, prouvé dans Robolectric |
| Captures par l'outil système | Indisponible sans ordinateur | Capture de toutes les fenêtres de l'appli, par l'appli |
| « L'agent regarde 120 captures » | Avec 2 modes, 2 tailles, 2 sens et 2 appareils : près de 1 000 images, hors de prix et peu fiable à l'œil | Le robot contrôle la mise en page, l'agent regarde 20 planches |
| « 30 min » | Le calcul donne 70 min | Essai éclair de 5 min, grand passage de 70 min sur chargeur |
| Écran éteint mesuré au passage | Une appli ne peut ni éteindre ni rallumer l'écran, et les mesures de l'appli s'arrêtent écran éteint | Étape finale, mesures sur un fil du robot |
| Basse au haut-parleur entre 100 et 300 Hz | Contredit le banc (au-dessus de 300 Hz) ; un téléphone ne sort presque rien sous 200 Hz | Courbe réelle du haut-parleur, réutilisée dans le banc |
| Extraits du micro dans le zip | 30 min de la pièce de Benjamin enregistrées | Analyse sur l'appareil, 2 s autour d'une anomalie seulement |
| « Banc parfait » = charge du processeur | Rien ne prouvait que le téléphone sonne comme le banc | Empreinte sonore téléphone contre Linux |
| Pas de seuils | Il fallait l'agent pour dire si c'est bon | Feux vert / orange / rouge à l'écran |
| Un plantage en route | Tout le passage perdu | Carnet de bord, reprise, plantage noté |
| 12 questions | Trop pour « le moins possible » | 8 questions, dont une sans réponse à donner |
| Autorisations | Passées sous silence : le robot ne peut pas les accepter | Écran de préparation, 2 touchers |
| « Le bandeau est à moitié écrit » | Faux : un début de modèle, mis de côté et non gardé | Tout est à écrire, compté en T3 |
| « Pocket Studio Test » | | **BenStudio Test** |
