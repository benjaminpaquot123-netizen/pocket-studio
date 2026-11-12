# Le grand audit final : tester comme de vrais utilisateurs

Demandes de Benjamin :
> 2026-10-27 : « Prévois un énorme audit avec un beau panel. L'objectif est de trouver toutes les imperfections du programme, quel que soit le type. » — « N'hésite pas à tout mettre en même temps, pas de limite. »
> 2026-11-12 : « Fais un plan d'audit, car actuellement il n'est pas optimisé pour tester le programme comme de vrais utilisateurs. Débutant, moyen, pro, etc. »

Ce plan remplace la version du 27/10. Le panel d'experts est gardé (partie 6) ; ce qui change, c'est tout le volet « utilisateurs ».

## 1. Pourquoi l'ancien plan ne testait pas « pour de vrai »

| Ce que faisait l'ancien audit | Ce que fait un vrai utilisateur | Ce qu'on change |
|---|---|---|
| Les testeurs lisaient le code et la doc avant de toucher l'appli : ils savaient où tout se trouve. | Il ne voit que l'écran. | **Test à l'aveugle** : captures et touches seulement, code interdit pendant la mission. |
| Les touches visaient des étiquettes cachées du code (`tap("wizard_create_now")`). | Il touche ce qu'il lit ou ce qu'il voit. | Le pilote touche **par le texte affiché** ou par l'endroit sur la capture. |
| Le banc était **sourd** : pas de moteur, rien ne sonne, l'export s'arrête (audit 35). | Il écoute tout le temps, et juge à l'oreille. | Le projet du testeur est **joué par le vrai moteur** à chaque étape (partie 3). |
| Des profils par style de musique, une mission vague. | Des niveaux très différents, un but précis. | **Cinq niveaux**, une mission avec un résultat vérifiable (partie 2). |
| Un seul passage, d'un trait, sans erreur. | Il se trompe, change d'avis, revient trois jours après, est interrompu. | Deux séances, erreurs imposées, interruptions, un testeur « casse-tout ». |
| Des constats en prose. | Il réussit, il galère, ou il abandonne. | Un **tableau de scores** par mission (partie 4). |

## 2. Le panel d'utilisateurs : cinq niveaux, seize missions

Chaque fiche dit : qui, ce qu'il sait déjà, l'appareil, le but **dans ses mots**, et ce qui prouve la réussite. L'aide permise est celle de l'appli seulement (« ? », Guide, missions, Les mots de la musique).

### Niveau 0 : le curieux (n'a jamais fait de musique)
| # | Qui | Mission | Réussi si |
|---|---|---|---|
| 1 | Léa, 16 ans, Mode simple, téléphone | « Un son pour ma story, vite. » | Un fichier envoyé, sans aide, en moins de 15 gestes après l'installation. |
| 2 | Le pressé : ne lit rien, « Plus tard » partout, Mode complet par erreur | « Je veux juste entendre un truc. » | Du son en moins de 5 gestes ; il n'est jamais coincé sur un écran. |
| 3 | Marc, 55 ans, taille 130 %, néerlandais | « Faire un petit morceau calme pour ma fille. » | Morceau lo-fi fini et envoyé ; aucun texte coupé, aucun mot anglais incompris. |

### Niveau 1 : le débutant motivé (veut SON morceau)
| # | Qui | Mission | Réussi si |
|---|---|---|---|
| 4 | Fan de jump up (le cas de Benjamin), suit le Guide jusqu'au bout | « Un morceau de 3 min à moi : mes sons, ma mélodie, un Drop qui tape, une vraie fin. » | Morceau exporté, différent de celui de l'assistant (sons, mélodie, structure), Drop plus fort que le Break. |
| 5 | Ado trap, veut poser sa voix | « Une instru avec une 808, et ma voix dessus. » | Une prise de voix calée, gardée, présente dans le fichier exporté. |
| 6 | Débutante, veut remixer une chanson | « Enlever la voix d'une chanson et rejouer par-dessus. » | Remix trouvé en 2 gestes, pistes séparées, rejouées avec le projet. |
| 7 | Débutante lo-fi qui **revient trois jours après** et change d'avis | « Reprendre mon morceau, changer le piano, puis finalement revenir à l'ancien. » | Projet retrouvé intact, son changé, retour en arrière réussi (historique, couches). |

### Niveau 2 : le moyen (vient de BandLab, GarageBand, FL Studio Mobile)
| # | Qui | Mission | Réussi si |
|---|---|---|---|
| 8 | Habitué d'une autre appli : cherche avec SES mots (piano roll, quantize, sidechain, BPM) | « Refaire cette boucle de 8 mesures » : un rythme, une basse et des accords imposés, écrits sur une fiche. | La boucle exportée contient exactement les notes de la fiche (vérifié dans le projet). |
| 9 | Beatmaker : sample importé, découpe, roulements de charleston, 808 qui glisse | « Un beat trap de 1 min 30 à partir de mon sample. » | Sample découpé sur les pads, beat exporté, 808 audible « au téléphone ». |
| 10 | Bidouilleur d'effets : automation, effets par piste, pad XY | « Faire monter le filtre avant le Drop et mettre de l'écho sur la fin. » | Les courbes existent, s'entendent en direct **et** dans l'export. |

### Niveau 3 : le confirmé (home-studio, sans assistant)
| # | Qui | Mission | Réussi si |
|---|---|---|---|
| 11 | Producteur techno : part d'un **projet vide**, refuse l'assistant | « Un morceau de 4 min : mes sons, mon arrangement, mon mix. » | 6 pistes ou plus, 8 parties ou plus, export à ±1 LU du volume visé, zéro saturation. |
| 12 | Compositeur : clavier, accords, gammes, piano roll complet, MIDI | « Écrire une suite d'accords et une mélodie, les exporter en MIDI. » | Fichier MIDI relu : mêmes notes, même tempo. |

### Niveau 4 : le pro (cherche les limites)
| # | Qui | Mission | Réussi si |
|---|---|---|---|
| 13 | Ingénieur du son | « Sortir les pistes séparées pour finir dans mon logiciel. » | Les pistes additionnées redonnent le mix (écart mesuré), true peak et LUFS tenus, 44,1 kHz / 16 bits corrects. |
| 14 | Producteur live | « Un set de 20 min : pads, effets, enregistrer la performance. » | 20 min sans alerte ni trou, prise gardée en « Nouvelle version », rien d'écrasé. |
| 15 | Le projet monstre : 8 pistes d'instruments, 8 pistes audio, 24 parties, des courbes partout, 7 min | « Est-ce que ça tient ? » | Ouverture, lecture, sauvegarde et export sans erreur ; « petit téléphone » pas « à risque ». |

### Hors niveau
| # | Qui | Mission | Réussi si |
|---|---|---|---|
| 16 | Le casse-tout : touches au hasard, doubles touches, rotation, retour en plein export, appli tuée, permissions refusées, stockage plein | « Rien, je touche à tout. » | Zéro plantage, zéro projet perdu, « ↶ » ramène toujours l'état d'avant. |

**Appareils** : chaque mission sur le téléphone (400 dp) ; les missions 1, 4, 8 et 11 refaites sur la Galaxy Tab A11 debout et couchée ; les missions 3 et 7 à 130 %.
**Couverture** : avant de lancer, une grille « fonction × mission » vérifie que chaque grande fonction (assistant, Guide, missions, Mode simple, batterie, 303, synthé, couches, voix, Remix, Générer, packs, Live, automation, mix, master, exports, Mes projets, zip, MIDI, réglages) est touchée par au moins deux missions de niveaux différents. Un trou = une mission de plus.

## 3. Les règles du jeu (ce qui rend le test « réel »)

1. **À l'aveugle.** Pendant sa mission, le testeur n'ouvre ni le code, ni `docs/NAVIGATION.md`, ni les anciens audits. Il ne reçoit que sa fiche et un petit pilote : capture d'écran, liste des textes visibles, toucher / appui long / glisser sur un texte ou un point.
2. **Penser tout haut.** Avant chaque geste : ce que je vois, ce que je crois qu'il va se passer. Après : ce qui s'est passé. **Un écart entre les deux est un constat**, même si la mission continue.
3. **Règle des trois essais.** Trois tentatives ratées → il cherche dans l'aide de l'appli. Si l'aide ne suffit pas → « bloqué » noté, puis un indice du meneur (compté) pour continuer la mission.
4. **Écouter comme sur le téléphone.** À chaque étape clé (premier son, après un changement, avant l'envoi), le projet du testeur est sauvé et joué par le vrai moteur (`ps_live`, le chemin de `scripts/live-bench.sh`) : niveau seconde par seconde, alertes fondu / silence / saut, basse « au téléphone », puis l'export mesuré (LUFS, true peak, durée). Le testeur « entend » donc ce qu'il a fait.
5. **Deux séances.** Niveaux 1 et 2 : une séance, puis l'appli tuée et rouverte « trois jours après ». Tous : une interruption en pleine action (arrière-plan, rotation).
6. **Erreurs imposées.** Chaque fiche contient deux maladresses à commettre (mauvais bouton, effacer par erreur, refuser le micro) : on regarde si l'appli aide à s'en sortir.
7. **Le code vient après.** Un enquêteur séparé (qui, lui, lit le code) reprend chaque blocage ou surprise et trouve la cause. Le testeur ne propose pas de correction : il raconte.

## 4. Ce qu'on mesure : le tableau de scores

Une ligne par mission, dans `docs/AUDIT-FINAL.md` :

| Mesure | Comment |
|---|---|
| **Réussite** | oui · avec l'aide de l'appli · avec un indice · non (abandon) |
| Gestes faits / gestes du chemin idéal | compteur du pilote ; le chemin idéal est rejoué par un expert |
| Gestes avant le premier son | compteur |
| Impasses (touche sans effet, écran sans issue) et retours arrière | compteur |
| Mots incompris, appels au « ? » | journal du testeur |
| Le résultat sonne-t-il ? | mesures de la règle 4 (alertes, volume, contraste Drop / Break, basse au téléphone) |
| **Le moment où j'ai failli abandonner** | une phrase du testeur |
| Fierté du résultat, envie de recommander | note sur 5, une phrase |

**Gravité, vue de l'utilisateur** : *bloquant* = mission ratée, plantage ou perte de données ; *grave* = indice nécessaire, ou résultat qui sonne mal sans que l'appli prévienne ; *moyen* = détour de plus du double du chemin idéal, mot incompris, surprise ; *mineur* = cosmétique. Un blocage au niveau 0 ou 1 pèse plus lourd qu'au niveau 4.

## 5. Étape 0 : les outils à préparer (avant de lancer le panel)

1. **Le pilote à l'aveugle** : au-dessus du harnais existant (`AppHarness`, appli entière dans Robolectric), des gestes par texte visible et par point, la capture et la liste des textes à chaque pas, le journal automatique (gestes, écrans, impasses).
2. **Le banc qui entend** : à la demande du testeur, sauver son projet et le faire jouer par `ps_live` puis `ps_render` (la mécanique de `RealProjectsTest` et de `LiveSongRecorder`), et lui rendre un petit rapport d'écoute.
3. **Le casse-tout automatique** : gestes au hasard (graine fixe, rejouable) sur trois projets (vide, assistant, le jump up réel de Benjamin) ; vérifie à chaque tour : pas de plantage, projet relisible, ↶ fidèle.
4. **Les fiches de mission** (`docs/audit-final/missions/`), avec la boucle imposée de la mission 8 et le sample de la mission 9.
5. **La grille de couverture** fonction × mission.

Limites connues, dites d'avance : pas d'émulateur ni de téléphone sur la machine de l'agent. Le toucher réel, la latence, le haut-parleur, la chaleur et le micro restent pour la recette de Benjamin (partie 7). Remix et Générer demandent leurs gros modèles : testés par `scripts/separation-tests.sh` côté moteur, et par le parcours d'écran jusqu'au téléchargement.

## 6. Le panel d'experts (gardé de l'ancien plan)

Ils lisent le code et font tourner les bancs, en même temps que les utilisateurs :
- **Son (3)** : bass music ; techno / house / trance / acid ; rap / trap / lo-fi / afro / pop. Toujours export **et** écoute en direct.
- **Technique (7)** : moteur temps réel ; sauvegarde et données ; plantages et robustesse ; performance ; qualité du code ; qualité des tests ; sécurité et vie privée.
- **Forme (4)** : design et ergonomie ; textes français / anglais / néerlandais ; accessibilité (TalkBack, contrastes, cibles) ; le Guide (aide-t-il vraiment ?).

## 7. Le déroulé

1. **Étape 0** : les outils (un agent, puis un essai à blanc avec la mission 1).
2. **Vague A, en même temps** : les 16 missions à l'aveugle + les 14 experts.
3. **Vague B** : les enquêteurs remontent chaque blocage jusqu'au code ; un sceptique essaie de réfuter chaque constat grave ou moyen ; doublons fusionnés.
4. **Synthèse** : `docs/AUDIT-FINAL.md` = le tableau de scores, puis les constats par gravité avec leur preuve (capture, journal, mesure).
5. **Corrections** : tout le bloquant, le grave et le moyen, par lots ; le mineur dans `docs/REVIEW.md`.
6. **Le match retour** : les missions ratées ou aidées sont **rejouées par un testeur neuf** sur la version corrigée. But : 16 réussites sur 16, sans indice pour les niveaux 0 et 1.
7. **Relecture neuve** des corrections, tests complets, captures regardées.
8. **Fin** : un seul APK, un résumé de trois lignes, les extraits audio, et la **recette de Benjamin** (`recette.json`, 15 minutes) : ce que seul un vrai téléphone dit (toucher, latence, haut-parleur, micro, geste retour, écran éteint).

## 8. Contraintes
- La CI de GitHub reste en pause (consigne de Benjamin) : tout tourne sur la machine de l'agent, fusion avec « [skip ci] ». Option à sa main : un seul passage « Nightly real use » sur émulateur à la fin de l'audit.
- La vague A tourne en parallèle (consigne « pas de limite » du 27/10, pour cet audit seulement) ; tout le reste en mode économie.
- Le chantier Play Store n'est pas dans cet audit.
