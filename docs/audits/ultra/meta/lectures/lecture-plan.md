# Lecture du plan de l'audit ultra (méta-audit, lecteur « plan »)

Date : 2026-11-05. Dépôt : `/home/user/fx-int` (branche `fx-int`, poussée comme `ccr-80af1a1b-mn0wec`, 203 commits depuis `d6f307a`). Lecture seule : rien n'a été modifié dans le dépôt.

Fichiers lus en entier : `docs/audits/AUDIT-ULTRA.md` (214 lignes, plan du 2026-11-03 révisé le 2026-11-04), `docs/audits/PLAN-AUDITS-FINAUX.md` (2026-10-29), `docs/audits/AUDIT.md` (2026-11-01), `docs/audits/CARTE-FONCTIONS.md`, `docs/audits/GABARIT-RAPPORT-TESTEUR.md`, `docs/audits/GABARIT-FICHE-DEFAUT.md`, `docs/audits/GABARIT-TABLEAU-DE-BORD.md`, `CLAUDE.md`. Vérifications d'existence (ls, grep) sur les fichiers que le plan nomme : `scripts/bench/*.py`, `scripts/human-replay.sh`, `docs/audits/ultra/base.tsv`, les dossiers de sièges `docs/audits/ultra/sieges/tour-N/`, `docs/audits/ultra/sieges/tour-3/consignes-testeur.md`.

Remarque de chemin : le prompt du propriétaire (`docs/audits/ultra/PROMPT-FABLE-META.md`, lignes 21 à 24) cite `docs/AUDIT-ULTRA.md`, `docs/CARTE-FONCTIONS.md`, `docs/GABARIT-RAPPORT-TESTEUR.md` ; les fichiers sont en réalité sous `docs/audits/`.

## 0. Comment les documents s'emboîtent (à savoir avant de lire les règles)

1. `PLAN-AUDITS-FINAUX.md` (29 oct.) est le plan d'origine. Depuis le 1er nov. il est **remplacé** par `AUDIT.md` (« en cas d'écart, AUDIT fait foi », ligne 3). Il reste la référence pour : les rôles (§2, §2 bis), le lot Master pro (§1 bis), les parcours (§3), les langues par tour (§4) et **la définition des gravités (§6)**.
2. `AUDIT.md` (1er nov.) organise le « premier Audit » en 8 étages (0 à 7). Sa révision du 4 nov. (lignes 190 et 231) dit que **l'étage 6 n'est pas joué à part : il devient le tour 0 de l'audit ultra, et seul `AUDIT-ULTRA.md` §7 porte les critères d'arrêt**.
3. `AUDIT-ULTRA.md` (3 nov., révisé 4 nov.) est la méthode effectivement appliquée depuis le 4 nov. Ses sections : §0 changements, §1 recensement, §2 tutos, §3 foule (3.1 banc 2, 3.2 profils, 3.3 missions, 3.4 rapport), §4 preuve par le rejeu, §5 équipe rouge, §5 bis mise à jour / premier lancement, §6 concurrence, §6 bis son, §6 ter langues et accessibilité, §7 boucle et arrêt, §8 coût et calendrier, §8 bis mode économe, §9 Benjamin, §10 ordre de préparation, §11 ce qui est écarté.
4. Les trois gabarits : `GABARIT-RAPPORT-TESTEUR.md` (4 nov. 10:15, « Audit ultra §3.4 ») ; `GABARIT-FICHE-DEFAUT.md` et `GABARIT-TABLEAU-DE-BORD.md` (3 nov. 10:57) qui renvoient encore à « AUDIT §2, §5, §6, §7 » (le premier Audit) et aux étiquettes `audit-N`, pas à l'ultra ni aux étiquettes `ultra-N`.
5. `CLAUDE.md` fixe les règles d'équipe (deux équipes au plus, serrure Gradle, surveillance, minuterie, revue neuve de fin de phase, règle « partout »).

Conséquence : **trois jeux de critères d'arrêt coexistent** dans les documents (PLAN §7 → AUDIT §6 → AUDIT-ULTRA §7, plus la liste à cocher du gabarit de tableau de bord qui reprend AUDIT §6). Voir §4 ci-dessous.

## 1. Les règles que la méthode prescrit (liste numérotée, avec la référence)

Chaque règle est reformulée en une phrase ; la référence donne le fichier et la section. « ULTRA » = `docs/audits/AUDIT-ULTRA.md`, « AUDIT » = `docs/audits/AUDIT.md`, « PLAN » = `docs/audits/PLAN-AUDITS-FINAUX.md`.

### A. Preuve avant correction

- **R1. Le rejeu d'abord.** Tout défaut passe d'abord par le rejeu déterministe des gestes du siège (`scripts/human-replay.sh <siège> <n>` : « reproduit / divergé au pas k ») ; reproduit = le défaut existe, zéro agent. ULTRA §4.1 et §3.1 (« Rejeu »). Le script existe (`scripts/human-replay.sh`).
- **R2. Test de reproduction obligatoire.** L'équipe de correction écrit un test rouge sur le code courant ; rouge = confirmé. « Un défaut non reproduit n'est pas corrigé. » ULTRA §4.2 ; `GABARIT-FICHE-DEFAUT.md` ligne 3 ; AUDIT §1.2 (« Tout est reproductible »).
- **R3. Un seul juge léger, seulement sans rejeu possible.** Il lit la grappe (≤ 15 lignes) et 2 images, tranche « défaut / voulu / idée » avec la gravité. ULTRA §4.3.
- **R4. Deux sièges = confirmé sans juge.** Une grappe vue par ≥ 2 sièges saute le juge : « la fréquence est la preuve ». ULTRA §4.3 ; `GABARIT-RAPPORT-TESTEUR.md` ligne 21 ; codé dans `scripts/bench/fuse-reports.py` (ligne 136 : `confirmed = … len(c["seats"]) >= 2`).
- **R5. Trois lecteurs seulement si l'équipe de correction conteste.** ULTRA §4.3 ; §11 (« trois contradicteurs lourds » écartés).
- **R6. Un défaut d'ergonomie n'est jamais réfuté par la lecture du code.** Il est confirmé par deux profils différents, ou par un second siège neuf qui échoue au même pas en ≤ 20 gestes. ULTRA §4.4, §0.6, §11.
- **R7. Chaque défaut a son oracle** (règle, mesure, comparaison, référence : FEW HICCUPPS, WCAG, son maître…). AUDIT §1.3 ; `GABARIT-FICHE-DEFAUT.md` section « Oracle ».
- **R8. Règle « partout ».** Un défaut gardé est corrigé à la racine, partout où il peut se reproduire, avec un test qui couvre tous les cas. ULTRA §4.5 ; AUDIT §1.4 ; `CLAUDE.md` (« Chaque erreur signalée par Benjamin… ») ; fiche de défaut, champ « Vérification partout ».
- **R9. Cause racine obligatoire ; trois défauts de même cause = une correction de fond.** AUDIT §5 ; fiche de défaut « Cause racine ».
- **R10. Priorité = gravité × fréquence × persistance, ×2 en zone de priorité 1.** AUDIT §5 ; fiche de défaut ligne 14.
- **R11. Cinq gravités définies** : Bloquant (on ne peut pas finir, plantage, perte de projet, son cassé), Grave (faux, trompeur ou très pénible : un débutant abandonne, un pro n'utiliserait pas), Moyen (gêne réelle avec contournement), Mineur (finition), Idée. PLAN §6 ; AUDIT §5.
- **R12. Verdict « banc » ou « appli » : aucune procédure écrite.** Le plan dit seulement que le banc n'a pas de moteur (ULTRA §3.2), qu'il est « Robolectric en debug, pas une APK » (§0, faits corrigés), que ses verbes chaos ne sont pas écrits (§5) et qu'un contrôle impossible à jouer est « remplacé par le plus proche possible et noté dans DECISIONS » (AUDIT §1.8). Les états de `docs/audits/ultra/base.tsv` (ligne 1 : « nouveau, confirmé, corrigé, voulu, idée ») n'ont pas d'état « banc » ; le mot « banc » n'apparaît que dans la colonne note (ex. ligne 3 : « banc : moteur gardé pendant le travail du tableau (34d56bb) »). Qui tranche, avec quelle preuve, n'est écrit nulle part.

### B. Dédoublonnage et lecture

- **R13. Fusion des doublons par programme, avant qu'un agent lise quoi que ce soit.** Clé = route de l'écran (lue dans le journal au n°) + cible + symptôme ; base de toutes les clés connues `docs/audits/ultra/base.tsv` avec leur état ; `clusters.md` ≤ 60 lignes, seule lecture d'un agent ; « nouveau » = clé absente de la base. ULTRA §3.4 ; gabarit du rapport, ligne 21 ; `scripts/bench/fuse-reports.py` lignes 4 à 6, 118, 128 à 130. (`base.tsv` existe : 31 lignes ; `clusters.md` n'est pas commité, il est dans le scratchpad par lot.)
- **R14. Un rapport qui juge le son est rejeté au dédoublonnage.** ULTRA §3.2, §3.4 ; gabarit du rapport ligne 3 ; `fuse-reports.py` ligne 11.
- **R15. La liste des défauts connus n'est jamais donnée aux testeurs.** ULTRA §3.3.
- **R16. Plafonds de lecture** : rapport de testeur ≤ 40 lignes ; synthèse de tour ≤ 60 lignes ; un juge lit ≤ 2 images ; personne ne lit `journal.txt` brut ni logcat ; le tableau de bord est rempli par le script sauf la ligne « En une ligne ». ULTRA §8 ; §5 (émulateur : résumé de 30 lignes).

### C. Le banc et les sièges (missions bornées)

- **R17. Banc 2 obligatoire avant le tour 1.** `start.txt` porte : `lang=` (15 codes), `device=` (5 profils + pliable), `font=100|130|200`, `mode=simple|pro`, `theme=studio|night|daltonien`, `fresh=reel`, `gestes.max=`, `lit=non`, `main=une|gauche`. Un test refuse un tour où tous les sièges ont la même langue. ULTRA §3.1 ; §0.2. (Constaté : `docs/audits/ultra/sieges/tour-3/lotA/a/start.txt` porte `lang, device, font, mode, fresh, lit, gestes.max, mission, timeout.minutes` ; pas de `theme=` ni `main=` sur ce siège.)
- **R18. L'écran en texte.** `screen-N.txt` = arbre sémantique visible ; l'image (`regarde image`) seulement au premier écran d'une route nouvelle ou quand le texte ne suffit pas ; plafond 1 image pour 5 gestes. ULTRA §3.1 ; repris dans `sieges/tour-3/consignes-testeur.md` (« at most 1 image per 5 gestures… no limit if your brief says you can't read »).
- **R19. `LayoutScan` à chaque écran, journal enrichi, chien de garde.** Route, nœud sous le doigt (ou « rien », « désactivé »), empreinte, retours, touchers sur « ? », ligne de son, gel du fil principal (trou > 700 ms = « gel », avec la pile) ; « rien ne change » après 3 écrans identiques, fin forcée au 5e. ULTRA §3.1.
- **R20. Budget de gestes tenu par le banc** : débutant 60, connaisseur 90, QA 120 ; « regarde image » = ½ geste ; le banc écrit « abandon : budget » et ferme. ULTRA §3.1, §3.3. (Constaté dans `sieges.tsv` tour 3 : colonne budget 60 / 90.)
- **R21. Amenées scriptées** : un script place l'appli au point de départ de la mission avant de rendre le siège à l'agent. ULTRA §3.1.
- **R22. Profils : liste fixe de 16, 8 tirés par tour, jamais deux fois le même.** 6 « n'y connaît rien » (enfant 8 ans, ado TikTok, parent pressé, senior police 200 %, explorateur, anxieux), 5 « connaît un peu » (GarageBand/Koala, DJ amateur, guitariste, fan de remix, MiniLab), 5 « connaît beaucoup » (producteur FL/Ableton, ingénieur du son, beatmaker trap, DJ club, QA pro). ULTRA §3.2.
- **R23. Variantes par réglage du banc, pas par persona** : `lit=non` sur 2 sièges par tour (dont l'enfant), `fresh=reel` sur 2 sièges, 2 sièges non francophones (langues tirées par paires avec appareil et police, `Pairwise.kt`), `main=une|gauche`, `font=200` ; un siège TalkBack au tour 1 et au tour final seulement. ULTRA §3.2, §0.11.
- **R24. Aucun testeur IA ne juge le son.** Les « connaissent beaucoup » reçoivent le rapport chiffré du banc (LUFS, crête, Drop − Montée, kick) et jugent ce que l'appli affiche et promet contre ces chiffres. ULTRA §3.2 ; §2 (« la promesse sonore est tenue par les programmes et l'oreille de Benjamin »).
- **R25. Affectation tirée par paires** : niveau × mission × mode × langue × appareil × zone, écrite dans le tableau de bord ; jamais deux sièges d'un même tour avec mission + mode + zone identiques. ULTRA §3.3.
- **R26. Missions découpées en 2 ou 3 buts courts** ; un abandon au but 2 reste une information. Dix missions générales (premier morceau ≤ 40 gestes en Simple ; remixer et découper la voix ; Pad Live premier son en 1 toucher ; refaire un défi ; enregistrer sa voix ; exporter Spotify et club ; reprendre 3 jours après ; brancher le MiniLab ; tablette au clavier ; tout en Mode simple) ; six missions musicales pour les connaisseurs, au tour 1 seulement puis si leur zone a changé. La mission « brancher le MiniLab » sort de la rotation (personne n'a de clavier). ULTRA §3.3, §9.
- **R27. Oracle de mission par programme** (`scripts/bench/seat-metrics.py`) : « réussie » est lue dans l'état sauvegardé, jamais déclarée par le testeur ; le banc écrit `mission=reussie|partielle|non`. Définitions : premier morceau = ≥ 3 parties, ≥ 2 min, un fichier partagé ; Remix = chanson séparée + un pad « Voix » ; défi = ≥ 1 étoile ; exports = deux fichiers aux cibles ; reprendre = projet rouvert en ≤ 3 gestes ; Mode simple = jamais passé en Pro. ULTRA §3.3 ; code lignes 158 à 297 de `seat-metrics.py` (mesures écrites dans `mesures.json`, 34 fichiers trouvés dans le scratchpad des tours).
- **R28. Mesures calculées par programme, pas par le testeur** : gestes jusqu'au premier son (cible 1 au Pad Live, ≤ 3 depuis l'accueil), gestes jusqu'au premier résultat partageable (≤ 40 en Simple), touchers à vide ou sur contrôle désactivé, retours immédiats, écrans revisités, défilements sans toucher, appels à l'aide, abandons, gels. ULTRA §3.4.
- **R29. Rapport court à champs fermés** : ≤ 40 lignes, 12 défauts au plus, 3 hésitations, 1 surprise, « j'aurais abandonné à … » ; pas de note sur 10, pas de SUS ni de SEQ. ULTRA §3.4 ; gabarit du rapport.
- **R30. Tutos : deux passages d'agent par tour** (1 Facile, 1 Pro, ≤ 5 chapitres), pris dans les sièges du tour, cours choisis par programme (le plus long, le plus manuel, celui qui a cassé au tour d'avant). ULTRA §2.
- **R31. Test des 5 secondes une seule fois**, sur 10 écrans clés, agent léger. ULTRA §3.4.

### D. Le recensement (census) et la carte des fonctions

- **R32. Le recensement étend les programmes existants** (LayoutScan, HelpCoverage, NavigationMap, CarteFonctions) au lieu d'un nouveau parcours : marcheur depuis 3 projets de référence (vide, petit, énorme), chaque nœud touchable touché une fois, effet classé (projet, annulation, écran, moteur, réglage, fichier, intent, message, vibration, rien) ; « sans effet » seulement si « rien » depuis les 3 projets, contrôle non désactivé, aucun dialogue ouvert ; liste `Allowed` avec raison. ULTRA §1 (tableau), §0.7.
- **R33. Le recensement complet est généré sous `app/build`, jamais commité** ; seuls le résumé par écran et la liste des acceptés avec raison sont commités ; un test échoue si une étiquette nouvelle apparaît sans fiche ni acceptation, ou si un compteur monte. ULTRA §1 (« Sortie »), §11.
- **R34. Règle des cibles : toucher ≥ 48 dp et visible ≥ 24 dp** (décision du 2026-11-03). ULTRA §1 ; PLAN §1.5 (48 dp, pads 64 dp, texte ≥ 14 sp, contraste ≥ 4,5:1).
- **R35. Ce qui reste aux agents légers** (listes courtes) : un « ? » explique-t-il vraiment ; un bouton « rien partout » est-il un bug ou un choix ; un texte de tuto se comprend-il sans le lire deux fois ; une icône veut-elle dire ce qu'elle montre. ULTRA §1 (fin), §0 (« principe qui a tout changé »).
- **R36. Carte des fonctions : une case vide est un oubli à corriger** ; `CarteFonctionsTest` échoue si un test cité n'existe plus, si une ligne n'a pas ses 13 cases, ou si le nombre de `VIDE` monte ; le plafond baisse à chaque tour. `CARTE-FONCTIONS.md` lignes 3 à 9 ; AUDIT §4 étage 2 ; ULTRA §7.
- **R37. Programmes à écrire pour le recensement** : registre unique des écrans, `AccessibilityScan` sur tous les écrans, `HelpCoverage` généralisé, `NavigationMapProTest`, `IconLexiconTest`, `GESTES.md` avec garde sur `pointerInput`, police 200 % dans la matrice. ULTRA §1, §10 (P0/P1). (`docs/GESTES.md` : absent à ce jour ; les tests : non vérifiés.)

### E. Équipe rouge et robustesse

- **R38. L'équipe rouge est un tableau « cas · où ça tourne · oracle »**, avec des définitions lues par programme : plantage = FATAL / ANR / signal fatal ; projet perdu = changement montré « Sauvegardé » absent à la réouverture ou projet rouvert neuf ; craquement = `xruns`, `restarts`, `nonFinite` > 0 dans `EngineHealth`. ULTRA §5 ; §0.8.
- **R39. L'émulateur d'audit tourne avant le tour 1 puis à chaque fin de tour** sur `ultra-N`, résumé de 30 lignes lu par un agent léger, personne ne lit logcat. ULTRA §5, §0.9. (`.github/workflows/audit-emulator.yml` existe.)
- **R40. StrictMode et LeakCanary** entrent dans le projet ; violations du premier passage acceptées avec raison. ULTRA §5, §0.9.
- **R41. Mise à jour, réinstallation, premier lancement** : `UpgradeFromReleasesTest` (5 étiquettes), `RestoredPhoneTest`, `FirstLaunchTest` ; `fresh=reel` sur 2 sièges par tour. ULTRA §5 bis.
- **R42. Téléphone de Benjamin = `recette.json`**, 7 points de l'équipe rouge, chaque point se termine par un chiffre à recopier. ULTRA §5, §9 ; `CLAUDE.md` Livraison.
- **R43. Version release R8 pour l'audit ; seuls le banc Robolectric et les instruments de debug tournent en debug.** AUDIT §1.5.
- **R44. Un test instable est réparé, jamais mis de côté.** AUDIT §0 (QA lead), §4 étage 1.

### F. Concurrence, son, langues

- **R45. Concurrence : un seul agent léger, une fois, avant le tour 1**, ≤ 30 lignes, ≤ 5 questions à Benjamin ; les « ajouts » vont dans le lot « après l'arrêt » (gel), seules les simplifications entrent dans la boucle ; pas de refaite. ULTRA §6.
- **R46. Critère son chiffré, rempli par `scripts/bench/dashboard.py`** : song-bench 37/37, tuto-bench 74/74, live-bench 0 alerte ; direct = export ±1 LU par partie ; banc de clics 0 ; cpu-bench 0 bloc hors budget ; 0 XRun en 30 min sur le Honor ; latence toucher → son ≤ 20 ms, MIDI ≤ 15 ms, gigue ≤ 2 ms ; résidu ≤ −60 dBFS ; kick ≥ +2 dB ; Spotify et club à −14 LUFS avec PLR ≥ 6 et Drop − Break ≥ +4 ; deux restes tranchés (haut-parleur : bloquant > 5 LU, listé 4 à 5 ; Intro riddim et jersey club : bloquant > 2 LU, listé 1 à 2). ULTRA §6 bis. (`scripts/bench/dashboard.py` : absent.)
- **R47. Langues et accessibilité par programmes** : `lang-packet` + un lecteur natif IA par langue, 14, une seule fois ; pluriels polonais et arabes ; icônes miroir ; police 200 % ; thème daltonien + simulation de Machado + « pas seulement la couleur » (ΔE > 10 ou autre signe) ; `ReplayShaky` ; `TalkBackWalk` sur 29 écrans × Simple/Pro ; score de lisibilité (50 pires textes). ULTRA §6 ter, §0.12.

### G. Boucle, coupe-circuit, une seule construction

- **R48. Forme des tours** : tour 0 = programmes verts + 3 éclaireurs (un par niveau) sur les 3 missions les plus fréquentes, Bloquants corrigés avant la foule ; tours 1 et 2 = 8 sièges neufs en 2 lots de 4 ; tour 3 et suivants = 6 sièges ; 3 tours au minimum, 5 au maximum puis point avec Benjamin (A : continuer, B : accepter les Moyens restants, étage 7). ULTRA §7, §0.4 ; AUDIT §6 (garde-fou après 5 tours).
- **R49. Coupe-circuit par lot (programme)** : après le lot A, 0 clé nouvelle et 0 clé corrigée qui réapparaît → lot B réduit à 2 sièges ciblés ; que des Mineurs → le lot B saute ; un Bloquant ou un Grave → la mission sort du tour jusqu'à correction ; une mission sans nouveauté sur 3 passages sort de la rotation. ULTRA §7.
- **R50. Zones changées = diff depuis l'étiquette du tour précédent → zone de `CARTE-CODE` → missions** : c'est le script qui tire les missions du tour suivant. ULTRA §7. (Script non nommé, non vérifié.)
- **R51. Une seule compilation partagée par tour** (l'atelier `fx-int`, que les équipes de correction ne touchent pas) et **une seule suite complète pour le tour**, puis étiquette `ultra-N` et émulateur ; jamais la suite complète « par correction ». ULTRA §8 (calendrier [6]), §8 bis, §0 (faits corrigés).
- **R52. Serrure Gradle** : un seul Gradle à la fois ; un lot de sièges le prend 45 à 60 min ; fenêtre Gradle en série (équipe 1 puis équipe 2) ; ateliers de correction sans Gradle pendant la lecture ; au plus 2 ateliers de correction vivants ; `gradle clean` entre deux tours ; ≥ 2 Go libres avant chaque lot. ULTRA §8 ; `CLAUDE.md` (« un verrou `flock` par groupe d'équipes… au moins deux en parallèle quand la mémoire le permet » : les deux textes ne disent pas la même chose, voir §4).
- **R53. Deux équipes de correction au plus, une par zone, jamais un agent par défaut** ; consignes fermées (fichiers exacts via `CARTE-CODE.md`, tests qui prouvent la fin, ce qu'il ne faut pas toucher) ; tests ciblés seulement ; commit à chaque morceau vert ; résumé des journaux jamais le journal entier. ULTRA §8 bis, §8 ; AUDIT §8 ; `CLAUDE.md` Méthode de travail.
- **R54. Relecture neuve** : un agent qui n'a pas écrit le code lit le diff (calendrier [5]) ; en fin de phase, revue par sous-agent neuf sur les zones sensibles, grave corrigé, reste dans `docs/REVIEW.md`, 3 lignes à Benjamin ; une seule relecture par version. ULTRA §8 ; `CLAUDE.md` « Revue de fin de phase » et « Mode économie ».
- **R55. Gel** : pendant un tour aucune nouvelle fonction, seulement des corrections sur branches à part ; les ajouts attendent le lot « après l'arrêt ». AUDIT §1.6 ; ULTRA §5 bis, §6, §10.
- **R56. Plafond de quota : 60 agents lourds et 40 légers tout compris**, compteur tenu dans le tableau de bord de chaque tour ; au-delà, arrêt et point avec Benjamin. ULTRA §8.
- **R57. Surveillance automatique** : disque < 2 Go, test Gradle figé > 20 min, démon bloqué, équipe sans commit ni note depuis 45 min, même échec relancé 2 fois → l'orchestrateur agit ; une minuterie (tâche planifiée toutes les 2 h) par audit, par lot et par étape. `CLAUDE.md` (Travail en équipes, Méthode de travail, Rôles).
- **R58. Mode économe** : toujours le modèle le plus fort ; un agent ne lit que les lignes en échec ; captures signalées par `LayoutScan`, jamais toutes ; tours qui rapetissent ; arrêt dès les critères, pas de tour « pour voir ». ULTRA §8 bis.

### H. Critères d'arrêt (ULTRA §7) et tableau de bord

- **R59. On s'arrête quand tout est vrai en même temps, « sur deux tours cumulés »** :
  1. 0 Bloquant, 0 Grave ; ≤ 3 Moyens nouveaux par tour et aucun vu par deux profils ; courbe des découvertes < 20 % du tour 1 ;
  2. missions (oracle programme) : débutants ≥ 9 passages réussis sur 10, aucune mission échouée deux fois, aucun abandon avant le premier son ; connaisseurs : tous, ou 1 échec expliqué par un défaut déjà fiché et corrigé ;
  3. recensement : 0 « rien partout » non accepté, 0 « sans ? » non accepté, 0 « sans nom » ; carte des fonctions : 0 case vide ;
  4. tutos : `TutoJourneyTest` (FR, DE, AR), « le panneau tient », cibles visibles, oracles de geste, `tuto-bench` 74/74, `TutoSoundsExistTest`, `GuideTipsReachTest` verts ;
  5. son : la ligne du §6 bis (R46) ;
  6. langues et accessibilité : 14 paquets traités, pluriels, icônes miroir, 200 %, daltonisme, `ReplayShaky`, `TalkBackWalk` verts ;
  7. mise à jour depuis chaque étiquette, réinstallation, premier lancement : verts ;
  8. émulateur : résumé `ultra-N` sans plantage ni ANR, démarrage à froid < 1,5 s, 0 fuite, StrictMode propre ; les 7 chiffres du téléphone relevés une fois.
  SUS, SEQ, note sur 10, plaisir : à l'étage 7 seulement, à 3 smileys ; l'étage 7 ne rouvre la boucle que pour un Bloquant ou un Grave. ULTRA §7.
- **R60. Anciens critères encore écrits ailleurs** (remplacés par la révision du 4 nov. mais toujours dans les fichiers) : « 2 tours de suite sans Bloquant ni Grave et au plus 5 Moyens ; courbe < 20 % ; carte sans case vide ; captures propres ; 0 erreur ATF et WCAG AA ; son ; démarrage < 1,5 s ; 90 % de réussite par tâche, SUS ≥ 80, Pad Live en 1 toucher, 8/10 sur chaque écran ». AUDIT §6 (lignes 238 à 250) ; `GABARIT-TABLEAU-DE-BORD.md` lignes 74 à 82 ; PLAN §2 bis (8/10 sur 6 critères, « un écran sous 8/10 bloque la fin des audits comme un Grave »).
- **R61. Un tableau de bord par tour** (`docs/audits/tour-N/tableau-de-bord.md`), rempli par le script sauf « En une ligne » : compteurs par gravité (trouvés, corrigés, restants, tour précédent), courbe des découvertes en % du tour 1, causes racines (top 5), cases vides de la carte, état des étages, son, performance, utilisabilité, accessibilité et langues, critères d'arrêt à cocher, prochain tour. `GABARIT-TABLEAU-DE-BORD.md` ; AUDIT §7 ; ULTRA §8 (compteur d'agents) et §3.3 (affectation des sièges écrite dedans). (Aucun dossier `docs/audits/tour-N/` n'existe : le gabarit n'a pas été rempli pour les tours ultra, d'après `ls docs/audits/`.)
- **R62. Une fiche de défaut par défaut** dans `docs/audits/tour-N/defauts.md`, avec son état (ouvert, reproduit, corrigé, vérifié au tour suivant, refusé). `GABARIT-FICHE-DEFAUT.md`. (Même constat : dossier absent ; les défauts des tours ultra vivent dans `docs/audits/ultra/tour-N-*.md` et `sieges/tour-N/synthese.md`.)
- **R63. Ce que chaque tour livre** : tableau de bord d'une page, une fiche par défaut (capture annotée ou son, cause racine, test, vérification partout), captures « après » sans la pastille de retours, extraits sonores avant/après. AUDIT §7 ; `CLAUDE.md` (captures sans la pastille « ● REC · Noter · ⋯ »).

### I. Règles de rôle (CLAUDE.md) qui pèsent sur l'audit

- **R64.** Benjamin ne lit pas le code ; on ne le sollicite que pour ce qu'il est seul à faire (téléphone, compte, secret) ; tout le reste se décide et se note dans `docs/DECISIONS.md`. `CLAUDE.md` Rôles.
- **R65.** Retours de Benjamin : rangés dans `docs/RETOURS.md`, analysés d'un coup sur demande, puis règle « partout ». `CLAUDE.md` ; AUDIT étage 7.
- **R66.** Fin de session : commit poussé et `docs/PROGRESS.md` à jour ; « continue » = prochaine case non cochée de `docs/PLAN-REPRISE.md`. `CLAUDE.md`.

## 2. La carte des fonctions, à plat

Source : `docs/audits/CARTE-FONCTIONS.md` (23 fonctions × 13 branchements = 299 cases ; bilan du 2026-11-03 : 221 avec un test, 38 sans objet, 40 vides ; après P0 équipe B du 2026-11-04 : 15 cases remplies, **25 vides**, ce que confirme le compte des `VIDE` dans le tableau actuel).

### 2.1 Les 23 fonctions (lignes)

1. Pad Live
2. Cases du Live (lancer, case vide, copier)
3. Briques et parties (Structure)
4. Copier une brique
5. Remix (séparation, 4 pistes)
6. Découpe du Remix
7. Cases Remix du Live
8. Tempo
9. Effets (pistes, live, ambiances)
10. Acid 303
11. Notes (clavier, piano roll)
12. Accords
13. Batterie (pads, pas)
14. Mix (volumes, muet, solo)
15. Enregistrer (voix, micro)
16. Enregistrer la performance (prise du Live)
17. Bibliothèque de sons (Ma bibliothèque)
18. Export et Master pro
19. Nouveau morceau (assistant)
20. Tutos (« Montre-moi »)
21. Guide
22. MIDI et MiniLab (profils, générique)
23. Tablette, clavier physique, dix doigts

### 2.2 Les 13 branchements (colonnes)

1. S/P : visible en Mode simple et en Pro
2. Tailles : chaque classe de taille (téléphone, couché, tablette, pliable)
3. Langues : les 15 langues, rien de coupé
4. Annul. : annulable en une étape
5. Sauvé : sauvegardé et rouvert
6. Exporté : présent dans l'export
7. Direct = export
8. MIDI : pilotable au MiniLab 3, profil reconnu, clavier inconnu en générique, vrai nom affiché
9. Perf. : pris dans l'enregistrement de la performance
10. Access. : TalkBack, cibles, contrastes
11. Aide : Lexique, « ? », tuto ou Guide
12. Mots : mêmes mots et mêmes icônes qu'ailleurs
13. ≤ 3 : atteignable depuis l'accueil en 3 touchers en Mode simple, retour jusqu'à l'accueil

### 2.3 Les 25 cases encore vides (lues dans le tableau)

- Accords : 6 (Tailles, Exporté, Direct = export, Perf., Mots, ≤ 3) ;
- Découpe du Remix : 2 (Mots, ≤ 3) ; Enregistrer (voix) : 2 (Tailles, Mots) ; Enregistrer la performance : 2 (Tailles, Mots) ; Bibliothèque : 2 (Tailles, Exporté) ; Export et Master pro : 2 (Tailles, Mots) ; Nouveau morceau : 2 (Tailles, Mots) ;
- Copier une brique : 1 (≤ 3) ; Cases Remix du Live : 1 (≤ 3) ; Acid 303 : 1 (Tailles) ; Notes : 1 (Mots) ; Tutos : 1 (Mots) ; Guide : 1 (Mots) ; MIDI et MiniLab : 1 (Tailles).
- Les 14 cases « Access. » vides du 3 nov. pointent désormais vers `ScreenScanTest::census`, présenté comme « une garde (rien ne s'aggrave), pas encore “accessible” » (section « Audit ultra, P0 équipe B »). La section « Cases vides et qui s'en charge » (lignes 63 à 70) liste encore ces 14 cases « Access. » : elle n'a pas été mise à jour après P0.

### 2.4 Les zones de risque du premier Audit (AUDIT §2) et les zones de correction

Zones de la carte des risques, avec leur priorité : Z1 sauvegarde, réouverture, migration, import de zip (1) ; Z2 moteur audio en direct (1) ; Z3 export et master (1) ; Z4 Remix (1) ; Z5 Live et Pad Live (1) ; Z6 tutos, Guide, « Montre-moi » (2) ; Z7 navigation, Mode simple, accueil (2) ; Z8 15 langues, tailles d'écran, tablette (2) ; Z9 réglages, aide, rapport de bug (3).

Les « zones 1 à 6 » des tours (fichiers `tour-N-zone1..6.md`) **ne sont définies dans aucun des documents de méthode lus** ; elles viennent des synthèses de tour (ex. `docs/audits/ultra/tour-1-synthese.md` ligne 6 : « Zone 1 — Remix → pads, Pad Live, défis ») et de `docs/CARTE-CODE.md` (non lu ici). Le plan parle de « 2 équipes de correction » (R53) alors que les tours ont 6 fichiers de zone : la correspondance zones ↔ équipes n'est pas dans le plan.

### 2.5 Dimensions de couverture prescrites (pour le lecteur « couverture »)

- 16 profils (R22), 10 missions générales + 6 musicales (R26), 15 langues (`lang=`), 5 appareils + pliable (`device=`), 3 polices (100, 130, 200), 2 modes, 3 thèmes, `fresh=reel`, `lit=non`, `main=une|gauche`, TalkBack (tour 1 et final). ULTRA §3.1 à §3.3.
- Les parcours du premier Audit (PLAN §3, 11 parcours ; AUDIT étage 6 : + Pad Live, découpe, « Piocher dans la chanson », tutos, tablette, scène de 45 min) et les langues par tour (PLAN §4 : tour 1 FR EN JA AR DE ES ; tour 2 NL PT-BR IT PL TR ZH-CN ; tour 3 ID HI KO + langues à défaut) restent écrits mais sont remplacés dans l'ultra par le tirage par paires (R23, R25) : rien ne dit si la rotation des 15 langues de PLAN §4 s'applique encore.

## 3. Les gabarits : champs du rapport de testeur et de la fiche de défaut

### 3.1 Rapport de testeur (`GABARIT-RAPPORT-TESTEUR.md`, 4 nov.)

Un bloc par siège, ≤ 40 lignes, champs fermés, lu tel quel par `fuse-reports.py` :

1. `siege` (lettre) · `profil` (un des 16) · `lang` · `device` · `font` (100|130|200) · `mode` (simple|pro)
2. `mission` · `buts atteints` (n sur m)
3. `hesitations` (3) : `ecran` n° · `cherchais` · `regarde`
4. `surprise` (une ligne ou « aucune »)
5. `abandon` : « j'aurais abandonné à l'écran n° parce que … » ou « jamais »
6. `defauts` (12 au plus), chacun : `ecran` n° · `cible` (texte ou nom TalkBack vu) · `geste` (touche|appui-long|glisse|ecris|retour) · `attendu` · `obtenu` · `symptome` (rien|mauvais-ecran|plantage|incompris|coupe|lent|perdu|bloque|sans-nom|sans-aide) · `gravite` (bloquant|grave|moyen|mineur) · `bloque` (oui|non)

Ce qui n'est **pas** dans le rapport et vient du banc (`mesures.json`) : gestes, retours, « ? », premier son, abandon, mission réussie. Interdits : récit, note sur 10, jugement sonore (rapport rejeté).

Écart constaté avec la consigne réellement donnée au tour 3 (`docs/audits/ultra/sieges/tour-3/consignes-testeur.md`, lignes 20 et 26) : une ligne `promesse: <la promesse du brief> | tenue: <oui|non|en partie> | pourquoi:` et un symptôme supplémentaire `promesse` ont été ajoutés ; le gabarit du dépôt ne les a pas. La colonne « promesse » existe aussi dans `sieges.tsv` (tours 3 et 4).

### 3.2 Fiche de défaut (`GABARIT-FICHE-DEFAUT.md`, 3 nov.)

Un bloc `## D-<tour>-<numéro> : <titre>` par défaut, dans `docs/audits/tour-N/defauts.md` :

1. Étage / siège (étage 0 à 7 ; personnalité, langue, appareil, orientation, police)
2. Version : étiquette `audit-N` (release R8) · commit
3. Zone (carte des risques AUDIT §2) · priorité de zone 1, 2 ou 3
4. Gravité (Bloquant | Grave | Moyen | Mineur | Idée)
5. Fréquence (sièges ou cas sur le total)
6. Persistance (contournable, comment | non contournable)
7. Priorité = gravité × fréquence × persistance (×2 en zone 1)
8. Reproduire : projet de départ ; graine du hasard ; étapes ; attendu ; obtenu ; preuve (capture annotée, vidéo, son, extrait du journal)
9. Oracle (règle, mesure, comparaison, référence)
10. Cause racine : famille (oubli de branchement | état non sauvegardé | texte | mise en page | son | performance | plateforme | autre) ; explication ; même cause déjà vue (D-x-y)
11. Correction : test qui échoue avant ; commit, branche ; vérification « partout » ; relance ciblée ; captures ou extrait sonore « après »
12. État : ouvert | reproduit | corrigé | vérifié au tour suivant | refusé (raison)

À noter : la fiche n'a **aucun champ « banc / appli »** ni « reproduit par rejeu (`human-replay.sh`) oui/non » ; elle parle d'étages, d'étiquettes `audit-N` et de « AUDIT §2/§5 », pas de l'ultra.

### 3.3 Tableau de bord (`GABARIT-TABLEAU-DE-BORD.md`, 3 nov.), pour mémoire

Sections : En une ligne · Compteurs par gravité (trouvés, corrigés, restants, tour précédent) · Courbe des découvertes (% du tour 1, critère < 20 %) · Causes racines top 5 · Carte des fonctions (cases vides, critère 0) · Étages 0 à 6 (vert / rouge / pas lancé) · Son (banc 37 styles, rendu maître, direct = export, XRuns) · Performance (démarrage < 1,5 s, images > 16 ms, fuites LeakCanary, PSS après 30 min) · Utilisabilité (réussite ≥ 90 %, SUS ≥ 80, Pad Live 1 toucher, designers ≥ 8/10) · Accessibilité et langues (ATF, WCAG, captures signalées) · Critères d'arrêt à cocher (ceux d'AUDIT §6, pas d'ULTRA §7) · Prochain tour. Pas de ligne pour : défauts qui reviennent, fausses alertes du banc, promesses tenues, compteur d'agents lourds/légers (que ULTRA §8 exige pourtant « dans le tableau de bord de chaque tour »).

## 4. Ce que le plan prescrit et qui paraît non mesurable, ambigu ou non appliqué

### 4.1 Le critère d'arrêt existe en trois versions, et aucune n'est celle du propriétaire

1. **La règle énoncée par le propriétaire** pour ce méta-audit (prompt, ligne 16 : « deux tours de suite sans défaut bloquant ou grave confirmé côté appli, sans défaut qui revient, et au moins 90 % des promesses tenues ») **n'est écrite dans aucun document de méthode**. ULTRA §7 ne parle ni de « défaut qui revient » (le mot n'apparaît que dans le coupe-circuit : « clé corrigée qui réapparaît ») ni de « promesses tenues » (grep « promess » dans AUDIT-ULTRA.md : une seule occurrence, ligne 63, « la promesse sonore ») ; la notion de promesse n'entre dans la méthode qu'au tour 3, par la consigne du testeur et `sieges.tsv`, sans seuil ni mode de calcul.
2. **« Deux tours cumulés » (ULTRA §7) contre « deux tours de suite » (AUDIT §6, gabarit du tableau de bord, prompt)** : « cumulés » n'est pas défini (somme des deux tours ? chacun des deux ?).
3. ULTRA §7 pose des conditions qui dépendent de programmes **qui n'existent pas encore ou que je n'ai pas pu vérifier** : `dashboard.py` (absent), `tuto-bench` 74/74, `TalkBackWalk`, `ReplayShaky`, `GuideTipsReachTest`, `TutoSoundsExistTest`, `UpgradeFromReleasesTest`, 14 paquets de lecteurs natifs (non vérifiés). Tant qu'ils manquent, la liste de §7 ne peut pas être « toute vraie en même temps » : le plan ne dit pas si l'on s'arrête quand même.
4. **« Courbe des découvertes < 20 % du tour 1 »** : l'unité n'est pas dite (clés nouvelles de `base.tsv` ? grappes ? défauts après juge ? défauts gardés après correction ?), et le nombre de sièges a changé d'un tour à l'autre (tour 1 : 24 sièges, lots A à F, `tour-1-synthese.md` ligne 1 ; tour 2 : lots A et B seulement ; tours 3 et 4 : 24 lignes dans `sieges.tsv`). Sans normalisation par siège, le pourcentage n'est pas comparable.
5. **« 0 Bloquant, 0 Grave »** : trouvés, confirmés, ou restants après correction ? Et avant ou après le tri banc/appli ? Le tableau de bord distingue trouvés / corrigés / restants ; §7 non.
6. **« ≤ 3 Moyens nouveaux par tour et aucun vu par deux profils »** : « nouveau » suppose `base.tsv` tenue à jour à chaque lot (`--maj-base`) ; les 31 lignes actuelles n'en donnent pas la preuve (non vérifié tour par tour). « Deux profils » ≠ « deux sièges » (deux sièges du même profil comptent-ils ?).
7. **« Débutants ≥ 9 passages réussis sur 10 »** : avec 6 à 8 sièges par tour dont la moitié de débutants, il faut cumuler plusieurs tours ; `partielle` compte-t-elle comme échec ? Non dit.
8. **« Aucun abandon avant le premier son »** : deux notions coexistent, l'abandon réel (budget épuisé, écrit par le banc : « abandon : budget ») et l'abandon déclaré (« j'aurais abandonné à l'écran n° »). Laquelle compte ?

### 4.2 Le verdict « banc » ou « appli » n'a pas de procédure

9. Rien ne dit **qui** classe un défaut « banc », **sur quelle preuve**, ni où ce verdict est consigné (R12). Le rejeu (R1) reproduit sur le même banc : un artefact du banc est « reproduit » aussi bien qu'un vrai défaut. Le test de reproduction (R2) ne distingue pas non plus (un test Robolectric partage les limites du banc). Le seul garde-fou écrit est AUDIT §1.8 (« remplacé par le plus proche possible et noté dans DECISIONS »), qui vise les contrôles, pas les défauts.
10. **« Confirmé côté appli »** (prompt) n'a donc pas de définition dans le plan ; c'est précisément le point que les tours 3 ont tranché à la main (voir les lecteurs des tours).

### 4.3 La clé de fusion rend « défaut qui revient » fragile

11. Clé = route + cible + symptôme (R13). La cible est le texte vu, normalisé (`fuse-reports.py` ligne 69) : le même défaut vu en français et en anglais, ou nommé autrement (« Tout couper » / « ■ de ligne » / « Wszystko »), donne des clés différentes ; `base.tsv` montre déjà une route inconnue (« ? », ligne 7 : `?|vocals remix simple mode|sans-aide`). Le coupe-circuit (R49) et le critère « clé corrigée qui réapparaît » reposent sur cette clé : un défaut qui revient sous un autre libellé passe pour « nouveau ».
12. La gravité d'une grappe est celle du siège le plus sévère (tri par `gravity` minimale, ligne 130 ; non vérifié au-delà) : un seul siège qui surcote entraîne la grappe. Le champ `bloque: oui|non` fait double emploi avec `gravite: bloquant` et peut le contredire ; aucune règle de réconciliation.
13. Les gravités (R11) sont définies par leur **conséquence humaine** (« un débutant abandonne », « un pro n'utiliserait pas »), que le testeur attribue lui-même alors que le plan lui interdit de juger (R24 pour le son) et que son budget l'empêche d'abandonner vraiment.

### 4.4 Règles déclaratives, sans mécanisme qui les fasse respecter

14. « Personne ne lit `journal.txt` brut ni logcat », « un juge lit ≤ 2 images », « 1 image pour 5 gestes », « la liste des défauts connus n'est jamais donnée aux testeurs » (R15, R16, R18) : aucune garde dans le banc ou les scripts n'est nommée (la consigne du tour 3 lève même le plafond d'images « si ton brief dit que tu ne lis pas »).
15. « Second siège neuf qui échoue au même pas en ≤ 20 gestes » (R6) : « même pas » n'est pas défini (même route ? même clé ? même geste ?).
16. « Zones changées → `CARTE-CODE` → missions, c'est le script qui tire » (R50) : aucun script nommé, non vérifié.
17. Le chien de garde (trou > 700 ms = « gel ») mesure le fil principal **du banc** : un gel du banc et un gel de l'appli se ressemblent par construction.

### 4.5 Le plan et la pratique ne coïncident pas (le plan ne dit pas lequel gouverne)

18. **Sièges** : 8 par tour (6 dès le tour 3), 16 profils « jamais deux fois le même » (R22, R48) ; constaté : 24 sièges aux tours 1, 3 et 4 (tour 4 : 20 joués, pause après le lot E). Avec 24 sièges par tour, « jamais deux fois le même profil » est impossible dès le tour 1, et les volumes de §8 (≈ 55 lourds, 1 750 images) sont dépassés par construction.
19. **Équipes** : « deux équipes de correction au plus » (R53, ULTRA §8, AUDIT §8, CLAUDE.md) ; constaté : six fichiers de zone par tour (`tour-2-zone1..6.md`, `tour-3-zone1..6.md`) et des « 6 équipes » dans le suivi. Sur Gradle, ULTRA §8 dit « un seul Gradle à la fois », CLAUDE.md dit « au moins deux en parallèle quand la mémoire le permet » : contradiction écrite.
20. **Tableau de bord et fiches** : prescrits par tour (R61, R62) ; aucun `docs/audits/tour-N/` n'existe. Le compteur d'agents (R56) n'est donc tenu nulle part de vérifiable. Les tours ont produit à la place `synthese.md`, `tour-N-zoneK.md`, `tour-N-integration.md`, `tour-N-relecture.md`, `notes-banc.txt`.
21. **Lots** : « 2 lots de 4 » puis coupe-circuit après le lot A (R49) ; constaté : lots A à F. Le coupe-circuit, défini pour deux lots, n'a pas de règle pour six.
22. **Fausses alertes du banc** : le plan n'a ni compteur, ni état, ni colonne pour elles (R12, 3.3), alors que les notes des tours en consignent (`base.tsv` lignes 3 à 8 : « banc : … »).

### 4.6 Artefacts nommés par le plan : état constaté

| Artefact | Prescrit où | Constat (ls du 2026-11-05) |
|---|---|---|
| `scripts/human-replay.sh` | ULTRA §3.1, §4.1 | existe |
| `scripts/bench/seat-metrics.py` (`mesures.json`) | §3.3, §3.4 | existe ; 34 `mesures.json` dans le scratchpad des tours ; aucun dans les copies texte `docs/audits/ultra/sieges/` |
| `scripts/bench/fuse-reports.py`, `clusters.md` | §3.4 | script existe ; `clusters.md` par lot dans le scratchpad (ex. `ultra/tour-1/lotB/clusters.md`), non commité |
| `docs/audits/ultra/base.tsv` | §3.4 | existe, 31 lignes, états sans « banc » |
| `scripts/bench/dashboard.py` | §6 bis, §7 | absent |
| `docs/GESTES.md` | §1 | absent |
| `.github/workflows/audit-emulator.yml` | §5 | existe (passages par tour : non vérifiés) |
| `app/src/main/assets/recette.json` | §9 | existe (contenu non vérifié) |
| `docs/audits/tour-N/tableau-de-bord.md`, `defauts.md` | gabarits | aucun dossier `tour-N` sous `docs/audits/` |
| tests `TalkBackWalk`, `ReplayShaky`, `tuto-bench`, `UpgradeFromReleasesTest`, `FirstLaunchTest`, `IconLexiconTest`, `NavigationMapProTest` | §1, §2, §5 bis, §6 ter | non vérifié |

### 4.7 Petites ambiguïtés de gabarit

23. `device: <profil>` : les noms des 5 profils + pliable ne sont listés nulle part dans les gabarits (les tours utilisent `phone360`, `phone`, `tablet11Land`…, `sieges.tsv`).
24. `theme=` et `main=` sont prescrits dans `start.txt` (R17) ; le siège A/a du tour 3 n'en porte aucun ; le test « refuse un tour où tous les sièges ont la même langue » n'est satisfait que par un seul siège non francophone.
25. Le champ `promesse … tenue: oui|non|en partie` (tour 3) est un jugement du testeur, pas un oracle ; « 90 % des promesses tenues » n'a ni dénominateur (par siège ? par promesse ? par tour ?) ni règle pour « en partie ».
26. Le gabarit de la fiche exige une « capture annotée » : le banc rend l'écran en texte, l'image est l'exception (R18) ; la preuve attendue et la preuve disponible ne sont pas les mêmes.
