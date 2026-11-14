# Audit de la nuit du tour 6 (12 au 13 novembre)

## Le verdict en bref

La méthode trouve bien les défauts et dit la vérité sur le tuto. Mais elle corrige trop, trop vite, sans vérifier que le débutant s'en sort mieux. Huit lots sont entrés, et le tuto n'est pas meilleur. Bonne pour chercher, pas encore pour réparer.

## A. L'audit a-t-il mesuré ce qui compte ?

En grande partie. Dix-neuf testeurs au lieu de huit, quinze langues : tu l'as demandé, et c'est noté. Mais la deuxième vague n'a pas été relue avant son départ.

Le gros raté : cent vingt gestes pour un tuto qui en demande deux cent cinquante. Cinq testeurs ont calé au chapitre sept. Le signal de 20 h 42 a été compris à 21 h 21, après le lancement de trois autres sièges pareils.

Tous n'étaient pas neufs : un siège « œil neuf » joué par un auteur de code, un lecteur et un trieur qui avaient tenu des sièges.

Le tri est sérieux : chaque verdict cite le code. Sa relecture a revérifié vingt-huit verdicts sur quatre-vingt-six, et contesté quatre des cinq « faute du banc » regardés. Le tri excusait trop l'appli. Deux verdicts sur trois restent non revérifiés.

Le son a été jugé avant le lot « kick », puis remesuré. C'est un calcul, pas une oreille.

## B. Les corrections étaient-elles sûres ?

À moitié. Le portail, le script qui garde l'entrée, a fait seize fusions et neuf refus : trois pour un vrai rouge, un conflit, cinq pour la forme. Il a arrêté deux vraies régressions ; les auteurs, deux autres.

Mais il ne rejoue que les tests du banc et ceux que le lot cite. Ni toute la suite, ni les captures. Un test rouge est donc resté quatre heures dans l'intégration, trouvé par hasard. Seize captures de référence sont périmées. Aucune vérification GitHub n'a couvert ces lots avant 5 h 55.

J'ai lu trois corrections rapides. Leurs tests sont vrais : rouges sans la correction. Mais ils prouvent un bouton, pas un parcours. Et leur relecture « neuve » a duré quatre minutes, sans relancer un test.

Le plus grave : la relecture du lot un avait vu les deux défauts qui ont coûté cher, le test rouge et les flèches fausses. Classés « moyens », ils sont passés. Personne ne suit les « moyens ».

## C. Les corrections ont-elles marché ?

Bloquant : oui, avec preuve. Kick : oui, à la mesure.

Tuto : non. Les trois contrôles vont au « Bravo ». Mais douze défauts d'hier sur trente-deux sont corrigés, trois graves sont nouveaux, et les chapitres bloqués passent de un à trois. « Le tuto n'est pas encore bon » est exact.

Cent défauts en lots parallèles, cela a brassé le tuto : trois lots l'ont touché ensemble, sans testeur avant 6 heures.

## D. Le temps perdu

Environ cinq heures évitables : une machine neuve bloquée deux heures, cinq sièges arrêtés un quart d'heure pour une ligne oubliée, trois passages au portail tués par des redémarrages. Et le lot des textes coupés, fini à 5 h 12, n'est toujours pas livré.

Le vrai goulot : un seul portail, une seule machine, sept heures quarante. Les lots, écrits en une heure, faisaient la queue.

## E. T'a-t-on dit la vérité ?

Sur le fond, oui : le point de 6 h 52 dit que le tuto n'est pas au niveau. Mais « le contrôle précédent était vert » datait d'avant toutes les corrections. L'échec sur émulateur, vieux de huit minutes, n'est pas dit. Ni le test resté rouge, ni les captures périmées. Et « redémarré sans rien perdre » oublie une heure de travail.

## G. Le banc est-il toujours branché sur l'appli ?

En partie, sans preuve complète.

Ce qui tient : le code du banc n'a pas bougé. Les deux cents noms de boutons qu'il cherche existent toujours. Les trois contrôles ont joué tout le tuto sans geste refusé.

Ce qui a dérivé : le juge du chantier comptait huit écarts hier, soixante-huit ce matin, et personne n'a regardé. Un seul rejeu protège l'appli : aucun défaut corrigé cette nuit n'a le sien. Le banc rend encore des écrans vides. Et le seul test qui lance la vraie appli, sur émulateur, est rouge.

La preuve qui manque : ce parcours sur émulateur au vert, puis un siège court par mission.

## F. Les notes sur dix

Qualité de l'audit : six. Tri : six. Sûreté des corrections : cinq. Efficacité des corrections : quatre. Vitesse : six. Maîtrise du coût : trois. Honnêteté : sept. Banc toujours branché sur l'appli : cinq.

## Les cinq règles, par ordre d'effet

1. Un lot du tuto n'est fini que quand un testeur neuf a rejoué ses chapitres, sur sa branche, avec trois cents gestes. Le chef lit « hésitations avant, après » avant le portail. Un seul lot du tuto à la fois.
2. Toutes les trois fusions, une discussion à part lance toute la suite, les captures, et lit le juge. Rien ne s'empile avant le vert. Avant chaque point, le chef lit la vérification GitHub, émulateur compris.
3. Chaque bloquant ou grave corrigé reçoit son rejeu sur le banc, vérifié par le portail.
4. Avant chaque vague, un siège pilote par mission joue vingt minutes avec la fiche exacte. Chaque discussion dit d'abord si elle peut modifier l'appli et pousser une branche neuve.
5. Chaque « moyen » d'une relecture va dans le fichier des revues, avec un responsable. Chaque point finit par « ce qui n'est pas vert ».

## À garder

Le portail. Le gel de version. La poussée après chaque fusion. La relecture du tri. Les tutos joués en entier. Les contrôles après correction. La franchise sur le tuto.

---

## Annexe : preuves

Abréviations : « journal » = `scratchpad/equipe-discussions.md` ; « T6 » = `docs/audits/ultra/sieges/tour-6/` dans `/home/user/fx-int` ; heures UTC prises dans git et dans les journaux du portail (le journal du chef avance de 5 à 10 min).

### A. Couverture, indépendance, budget, tri, son

- **Prévu contre joué.** `PLAN-TOUR-6.md` § 4 et § 6 : 8 sièges, « 10 agents en tout, 3 au plus en même temps ». Joué : 19 sièges + 3 suites (22 dossiers `T6/lot*/…`), un balayage automatique, un siège son ; jusqu'à 14 discussions. La 2e vague est notée en une ligne dans `docs/decisions/tour-6-depart.md` (« hors du plan audité à 8 »). Elle vient de tes messages de 19 h 58 à 20 h 00 (« croisements pour les langues », « les tutos… modèle phare »). `AUDIT-PLAN-TOUR-6.md` n'a audité que le plan à 8 ; il ne dit rien du budget d'un tuto entier.
- **Départ avec la liste de lancement non tenue.** Commit `1585bb467` « launch checklist: stopped at step 10 (judge: 12 gaps, 7 accepted) » puis `d12ad4a8d` 17 min après ; 2 écarts notés dans `tour-6-depart.md`. La vérification GitHub du départ (run 43279184052) est sortie rouge à 20 h 21 ; le plan disait « rouge : on arrête les sièges » ; décision de continuer (journal 20 h 27, cause dans le banc, confirmée par le lot ci2). Tu as été prévenu à 20 h 27 avec la promesse « je le note dans le journal des décisions » : cette ligne n'existe pas dans `tour-6-depart.md` (2 lignes seulement).
- **Écarts non écrits dans le dépôt.** `T6/hors-banc.tsv` : 9 lignes sur 10 encore à « ? » (ci_build, ci_emulateur, sieges_coupes_par_le_banc, corrections_sans_test…). Non notés : C a joué par KICK lui-même (journal 20 h 44 : « à noter dans hors-banc / tour-6-depart », pas fait) ; les 3 suites à 300 gestes ; lecteur, rédacteur et trieur non neufs ; 9 lots de correction puis un 10e alors que le plan § 8 dit « au plus 6 » ; § 9 « pas de boucle » contre « je boucle jusqu'à un passage propre » (point de 6 h 52). `docs/REVIEW.md` n'a aucune entrée du tour 6.
- **Pièces manquantes.** 10 dossiers de siège sur 22 n'ont pas de `mesures.json` (A a, A c, C b, D a, D a-suite, E c, G a, G b, H a, H a-suite) alors que le plan § 5 l'exige. Les images des écrans ne sont pas dans l'intégration (`T6/LISEZ-MOI.md`). Aucun rapport ne dit qui a joué (discussion ou sous-agent) : les « sous-agents neufs » du journal ne se prouvent pas.
- **Indépendance.** Journal 20 h 44 : « KICK a joué C a LUI-MÊME (avait lu le code moteur du kick) », rapport gardé « indicatif ». S2 a tenu 5 sièges (A b, B a hôte, B c, F a, F b) puis a été lecteur et rédacteur de la synthèse (journal 21 h 42, 23 h 48) ; GAUCHER a hébergé E c et G a, écrit le lot « gaucher », fait le tri (21 h 52 à 22 h 14), puis corrigé le lot 4, dont le bloquant vu par son siège E c. Le plan § 6 : « 1 lecteur neuf », « 1 rédacteur neuf ».
- **Budget de gestes.** Fiches `start.txt` : `gestes.max=120` pour B b, D a, D b, D c, H a (tutos). Journal 20 h 42 : B b « pas le bravo final (80 gestes) » ; 21 h 00 : 2e vague lancée avec D a, D b, D c, H a à 120 ; 21 h 21 : « AUCUN siège n'a vu le Bravo… 120 gestes ne suffisent pas » ; 21 h 39 confirmé. Suites à 300 : H a-suite (fini 21 h 43, 246 gestes), D a-suite (22 h 07, 223), B b-suite (22 h 11). Derniers sièges rentrés à 22 h 11 au lieu de 21 h 54.
- **Tri.** `T6/verdicts.tsv` : 98 lignes (77 réel, 11 voulu, 3 artefact, 5 doublons, 1 déjà corrigé ; 1 bloquant, 5 graves). `T6/tri-relecture.md` : 28 verdicts revérifiés sur 86 au moment de la relecture ; 7 désaccords dont 3 « graves » (U23, N15, N16) ; sur les 5 « artefact du banc » relus, un seul confirmé (U12), R8, U22, U23 et N15 contestés ; les 10 « réel » relus sont confirmés. Les 3 désaccords graves sont repris dans `verdicts.tsv` (U23, N15, N16 = réel). N19 à N28 jugés « tous réels » en 4 minutes (journal 22 h 32 à 22 h 36), sans relecture. N29 à N31 (balayage) et S1 à S6 (son) n'ont pas de ligne dans `verdicts.tsv` (synthèse : 105 défauts ; verdicts : 96 numéros).
- **Mesure « hésitation ».** Un seul testeur par langue ; le contrôle B b écrit lui-même « testeur neuf plus strict » (`eq/tour-6-controle-Bb`, rapport l. 108). Le nombre de chapitres avec hésitation dépend donc du testeur ; les chiffres solides sont « corrigés X sur Y », « bloqué », « graves neufs » et les gestes.
- **Son.** `T6/son/rapport-son.md` l. 3 : version jugée 1585bb46 ; journal 23 h 45 : « MAIS jugé… AVANT le lot kick » ; `rapport-son-apres-kick.md` : tête 845c8beb, 28 → 3 styles sous 3 dB sur le modèle de téléphone, 26 → 12 sur le pire haut-parleur ; mesure par filtres, pas d'écoute humaine. La synthèse garde pourtant le kick comme 8e grave et n° 10 du « top 10 », avec la mention « nouvelle mesure en cours ».
- **Version auditée.** Les sièges ont jugé 1585bb46 alors que 5 lots déjà écrits attendaient (gaucher, langues, kick, moyens2, ecoute ; journal « gel de version », 20 h 05). Effet mesuré faible côté écrans (1 verdict « déjà corrigé », R2), fort côté son.

### B. Sûreté des corrections

- **Ce que le portail vérifie** (`scripts/bench/fusion-equipes.sh` l. 31 à 113) : relecture commitée qui nomme le commit lu, rien changé après (sauf fichiers égaux à l'intégration), verdict « 0 bloquant, 0 grave », un seul fichier de décisions, rapport de zone, pas de lien symbolique, fusion, puis self-test du banc + rejeux + les seuls filtres de tests cités par le lot (l. 99 à 110). Il ne lance ni toute la suite `:app:testDebugUnitTest`, ni `verifyRoborazziDebug`, ni `assembleRelease`. Les « moyens » et « mineurs » d'une relecture ne bloquent rien.
- **Passages au portail cette nuit** (journaux `portail-*.log`, `chaine*.log`, `file*.log`) : 16 fusions réussies (juge7, ci2, gaucher, langues, kick, banc7, remixdecoupe, moyens2, restes, tutogrille, morceauexport, cablage, ecoute, tutoconsignes, guidecoquille, reparations). 9 refus : 3 pour un vrai rouge (ci2 v1, `BenchNetworkEndTest#drive` ; tutogrille v1, `chaine3.log` l. 30, 23 h 58 ; guidecoquille v1, rejeu t4-26, `file9.log` l. 214, 3 h 45) ; 1 conflit (moyens2, captures ré-enregistrées des deux côtés, journal 23 h 05) ; 5 pour la forme (gaucher : fusion d'une tête ancienne, journal 22 h 20 à 22 h 26 ; ecoute : fichiers arrivés après la relecture, `file-portails.log` l. 127 ; morceauexport : `RhythmViewModel.kt` changé après la relecture, `file3.log` l. 60 ; ecoute : décisions dans `tour-6-son.md`, `file5.log` l. 4 ; tutoconsignes : décisions dans `-a.md` et `-b.md`, `file7.log` l. 4). Plus 3 passages tués par un redémarrage (remixdecoupe deux fois, ecoute une fois).
- **« Quatre vraies régressions arrêtées »** (PROGRESS, point de 6 h 52) : 2 par le portail (grille du piano roll sortie de la zone, bouton « Arrêter » de la prise) ; 2 par le self-test relancé par l'auteur à la demande du chef, avant le portail (« Fin » qui cassait « Écouter », icône inconnue du banc ; commits `69e09546f`, `7c36fe670`, 1 h 04 à 1 h 11). La première relecture neuve du lot 6 avait dit « 0 bloquant, 0 grave » (23 h 41) alors que ce self-test avait 3 rouges.
- **Ce que le portail a manqué.**
  - `GestureBudgetTest#aStrokeCutShortIsStillItsOwnUndoStep` rouge dans l'intégration depuis la fusion du lot 1 (1 h 45), signalé par l'agent du lot 3 vers 3 h 05, confirmé à 4 h 30 (journal), réparé par le lot 9 à 5 h 55 (`1f41ed8ca` : le test est adapté, pas l'appli). Cause : le correctif U6 (`5c72d6867`, `RhythmScreen.kt`) éteint « Suivre » sur le chemin « peindre ». La relecture du lot 1 l'avait écrit : « Moyen… `RhythmScreen.kt:717` (U6, chemin peindre) éteint Suivre sans condition de lecture » (`tour-6-tutogrille-relecture.md`).
  - 16 captures de référence périmées depuis le lot 1, signalées par FUZZ à 5 h 12 (journal) ; une seule ré-enregistrée (`2badb011d`). Encore ouvert dans PROGRESS.
  - Les flèches du tuto : même relecture, « Mineur… `PianoRoll.kt:1331` (flèche ↑/↓ sur la 1re note cachée d'une liste non triée) » ; le contrôle japonais trouve ensuite « flèches ↑ → ↓ contradictoires pour la même note, 12 gestes » (grave neuf), après le lot 9 qui avait retouché cette règle (`71704bef9`).
  - Le commit de fusion refusé `ea294587a` (tutogrille v1, « défait » par le portail) est dans l'historique de la tête : il est entré par la branche du lot 3, dont l'agent avait fusionné l'intégration locale pendant le portail (journal 0 h 15, « PIÈGE TROUVÉ »). Le contenu est recouvert par la v2 ; la trace reste.
- **Vérification GitHub** (`gh run list`) : 43324587513 verte sur `5957fac5` (créée 22 h 57, finie 1 h 42) : elle couvre gaucher, langues, kick, pas un seul lot du tour 6. Aucune autre avant 43375199284 sur `9f26c244` (5 h 55). Entre les deux : 12 fusions. État à 7 h 08 : « Emulator smoke test » en échec à 6 h 44 (`JourneyTest.aFirstSongInTheModeSimple` : « nothing tagged wizard after 15000 ms ») ; « Build, unit tests, screenshots » encore en cours. Une vérification complète prend 1 h 30 à 2 h 45 : elle ne peut pas servir de garde entre deux lots.
- **Trois lots rapides, tests lus.**
  - Lot 1 (tuto-grille, 8 défauts dont 3 graves) : premiers commits 22 h 22, rapport 22 h 52, relecture neuve 22 h 56 (4 min après le rapport, « Aucun test relancé »). Échantillon U6 : test `RhythmGrilleTest#aCellTouchedWhilePlayingKeepsItsBar` (`88723fc03`) puis correctif `5c72d6867` : le test échouerait sans le correctif (la page tournerait). Vrai test. Mais la classe voisine `GestureBudgetTest` n'a pas été lancée.
  - Lot 6 (morceau-export-padlive, 21 sur 24) : confié vers 23 h, relecture 23 h 41, « Tests non relancés (lecture seule) » (35 lignes). Les 8 commits « rouge: » sont tous à 23 h 18 min 09 s et les « vert: » 30 à 40 secondes après : l'ordre rouge puis vert est rangé après coup, pas une preuve que le rouge a été vu. Échantillons U23 (`ae6b47099` / `78141a15d`, attente dans « Mes projets ») et R23 (`beee68bcd` / `a0e82cf08`, le Mix s'ouvre en haut) : les deux tests échoueraient sans le correctif. Vrais tests, au niveau d'un écran isolé.
  - Lot 7 (restes) : 23 h 44 à 0 h 05, relecture « Aucun test relancé ». Échantillon « Boum boum » (`b71c5571c` / `2b584b390`) : le test lit une ligne nouvelle, absente avant ; il échouerait sans le correctif.
  - À l'inverse, les relectures des lots 4, 8 et 3 ont rejoué les rouges (« rouge constaté : … rejoué sur 0992f9ac », `tour-6-remixdecoupe-relecture.md` l. 12 à 17). C'est le bon modèle.

### C. Efficacité

- **Avant, après** (rapports `T6/lotH/a-suite`, `lotD/a-suite`, `lotB/b-suite` contre `controle/Ha`, `Da`, `Bb` sur les branches `eq/tour-6-controle-*`, version 9f26c244) :
  - Français Facile : Bravo, 246 → 259 gestes ; chapitres avec hésitation 11 → 13 sur 15 (1 bloqué hier, 0 aujourd'hui) ; corrigés 2 sur 6 ; 5 défauts neufs dont 1 grave.
  - Japonais Facile : Bravo, 223 → 191 gestes ; hésitation 10 → 12 sur 15, dont 2 bloqués (chapitres 5 et 7) contre 0 ; corrigés 6 sur 16 (7 « autre », 3 encore là) ; 3 neufs dont 1 grave.
  - Allemand Pro : Bravo, environ 226 → 214 gestes ; hésitation 7 → 12 sur 16, dont 1 bloqué (chapitre 13) contre 0 ; corrigés 4 sur 10 ; 10 neufs dont 1 grave, et un sur-correctif (swing validé à 4 % pour 7 % demandés).
  - Total : 12 corrigés sur 32 ; 3 graves neufs ; bloqués 1 → 3.
- **Bloquant** : lot 4, rouge rejoué par le relecteur (`CutBoutBigTextTest`, « the curve is 0.0.dp high at 200 % »), fusionné `70d2d8de`. Non rejoué par un siège à 200 %.
- **Lot 5 « langues »** (textes coupés et mots anglais, la famille la plus répandue de la synthèse) : pas dans l'intégration à 7 h.
- **Brassage du tuto** : lots 1, 2, 3 puis 9 touchent le tuto (PianoRoll, RhythmScreen, consignes, panneau, Guide) ; le lot 2 seul porte 50 décisions (`docs/DECISIONS.md` l. 1492). Premier testeur sur l'ensemble : 6 h. Le lot 10 est déjà lancé sur les mêmes fichiers.

### D. Temps perdu (minutes, d'après les horodatages)

- Sièges qui attendent le banc non lancé : `T6/incidents-banc.tsv`, colonne « attente » : A b 600 s, A c 600 s, B b 900 s, C a 900 s, C b 900 s = 65 min ; leçon écrite à 20 h 37, R3 encore arrêté pour la même raison de 21 h 29 à 21 h 36.
- Discussions bloquées sur une question à Benjamin : GAUCHER 34 min (19 h 29 à 20 h 05), LANGUES 16 min.
- Machines neuves (Maven 429) : R1 de 19 h 45 à 21 h 49, soit 124 min, aucun livrable ; R3 rend C b à 21 h 54, 68 min après les autres sièges de la vague.
- Budget du tuto : 5 sièges coupés au chapitre 7, 3 suites (environ 72 min de sièges), 17 min sur le chemin critique.
- Lot 2 donné à S3 qui n'a pas le droit de modifier l'appli : 13 min (22 h 17 à 22 h 30). Règle découverte à 22 h 30 ; elle retire 8 discussions de la correction.
- Refus de forme : gaucher environ 12 min ; morceauexport 17 min ; tutoconsignes 47 min (2 h 59 à 3 h 46) ; ecoute 79 min entre le premier refus et une version acceptable (1 h 16 à 2 h 35). Total environ 155 min de retard de lots.
- Redémarrages : remixdecoupe relancé à 0 h 41 au lieu de 23 h 58 (43 min) ; ecoute 12 min ; agent du lot 3 mis en pause de 0 h 40 à 1 h 47 (67 min), avec 16 fichiers non commités au 2e redémarrage. Cause inconnue. Avant la règle « pousser après chaque portail » (0 h 15), la fusion de banc7 n'était que sur le disque.
- Vraies régressions : tutogrille entre à 1 h 45 au lieu de 23 h 58 ; guidecoquille à 5 h 41 au lieu de 3 h 45.
- Lot 5 chez FUZZ : limite de branche connue à 23 h 18 (journal), lot confié quand même à 23 h 16 ; fini à 5 h 12, non livré à 7 h (au moins 108 min, et toi non prévenu à cause de la consigne de silence).
- Branche des documents de 286 Mo (1 010 binaires) : découverte au moment de fusionner, vers 4 h 20, dépôt public ; refaite en texte seul (4,6 Mo, `2bb86bc3a`). Coût faible, risque évité de justesse. S2 gardée ouverte de 23 h 48 à 4 h 35 pour ça.
- **Chemin critique.** Sièges 19 h 30 à 22 h 11 ; tri 22 h 14 ; puis portail en file sur une seule machine de 22 h 14 à 5 h 55, soit 461 min : 268 min de passages réussis, 103 min de passages perdus (tutogrille v1 35, remixdecoupe 43, ecoute 12, guidecoquille v1 13), 90 min à vide dont 68 à attendre le lot 3 (4 h 19 à 5 h 27). Le lot 3, écrit par un agent sur la machine du portail, va de 23 h 06 à 5 h 41.

### E. Ce qui t'a été dit

- 20 h 03 : aveu clair, « le monitoring n'était pas parfait, deux discussions étaient bloquées sans que je le sache ». 20 h 27 : la vérification rouge du départ et l'écart au plan sont dits. Bien.
- 21 h 15 : « Tout roule » ; à cette heure R1 est bloqué depuis 1 h 30, KICK a joué un siège « œil neuf » lui-même, et le problème de budget du tuto est déjà visible (20 h 42). Enjolivé.
- 6 h 52, point du matin :
  - « Huit lots fusionnés », « bloquant réparé », « 105 défauts, 1 bloquant, 8 graves », « 28 → 3 styles » : exacts (le tri ne retient que 5 graves ; le 12 sur le pire haut-parleur n'est pas dit).
  - « Le dernier contrôle complet tourne encore ; le précédent était vert » : le précédent (`5957fac5`) ne contient aucun lot du tour 6 ; la tâche émulateur était rouge depuis 6 h 44.
  - « Il a arrêté quatre vraies régressions… Rien de cela n'est entré » : vrai pour ces quatre ; le test rouge entré avec le lot 1 et les 16 captures périmées ne sont pas dits.
  - « Trois débutants neufs » : ce sont les trois mêmes discussions qu'hier ; sous-agents neufs non prouvés.
  - Tuto : « pas encore au niveau », 12 ou 13 chapitres, 3 graves neufs : exact et net. Manque : 12 corrigés sur 32, et les chapitres bloqués 1 → 3.
  - « J'ai fermé onze discussions » puis, à 7 h 03, « les neuf que j'ai fermées » : le journal en liste 9.
  - « Redémarré trois fois… sans rien perdre » : trois passages au portail perdus et un agent mis en pause.

### G. Le banc et l'appli après les corrections

- **Le banc n'a pas changé depuis banc7.** `git diff --stat 23ee5dd02 HEAD` sur `app/src/test/java/com/pocketstudio/app/human`, `scripts/human-act.py`, `scripts/human-tester.sh`, `scripts/bench`, `docs/audits/ultra/rejeux` : vide. Les 9 lots de l'appli n'ont donc rien adapté dans le banc ; quand le banc a rougi, c'est l'appli qui a été changée (icône « ‹ » retournée en « › », `tour-6-guidecoquille-rapport.md` l. 129 ; flèche de la rangée des pistes, `7c36fe670` ; N15 retiré après le rejeu t4-26, `5fadb52b3`). À savoir : c'est le banc qui a ainsi décidé de deux choix d'écran.
- **Noms cherchés par le banc.** 205 mots de type étiquette cités dans les 60 fichiers du banc ; tous retrouvés dans `app/src/main` ou `ui-components`, sauf 8 qui n'en sont pas (4 clés de l'état du siège dans `SeatState.kt`, 1 faux nom voulu dans `BenchSelfTest`, 3 touches `key_60/61/72` de `Tour5BloquantsTest`, classe passée verte au portail du lot gaucher). Les icônes sont relues dans les sources à chaque self-test (`BenchSelfTest#everyIconOfTheAppHasItsShape`, l. 88 à 103). L'oracle des tutos lit les mêmes fichiers `tutos/*.json` que l'appli (`seat-metrics.py` l. 250 à 284) : les étapes ajoutées par le lot 2 (37 fichiers changés) sont suivies sans retouche. Lecture statique seulement : je n'ai rien lancé.
- **Les deux totaux du self-test.** Portail, de banc7 à reparations : « 176 verts, 0 rouges, 17 sautés » à chaque fusion (`docs/audits/ultra/portail/2026-11-1*/selftest-resume.txt`). Les 17 sautés sont tous des `#drive` « only with -Phuman.dir » (points d'entrée des sièges, listés dans `selftest-sauts-connus.txt`). L'écart avec « 203 verts, 44 sautés » vaut 27 et 27 : chaque profil de siège ajoute un test vert « amenée prête » et un `#drive` sauté. Preuve : lot H 3 profils = 173/16, lot A 6 profils = 176/19, lot D 9 profils = 179/22. 203/44 est donc un passage avec 30 profils. Le portail ne vérifie l'amenée que pour 3 profils (français, téléphone, 200 %) ; les mêmes tests du banc tournent des deux côtés. Aucun des deux ne « saute » un contrôle de l'appli, mais le portail ne prouve pas l'amenée dans les 14 autres langues ni sur tablette.
- **Rejeux.** `docs/audits/ultra/rejeux/` : un seul, `t4-26-prise-clavier` (« oracle tenu » dans chaque `fusion.txt`). `rejeux-attente/` : les 8 du tour 5, non joués par le portail. Aucun rejeu créé pour le tour 6, alors que `PLAN-TOUR-6.md` § 5 en demande un par bloquant ou grave. Les corrections de la nuit ne sont gardées que par des tests d'écran isolé. Ce que le banc ne verrait pas : tout écran que ni t4-26 (prise au clavier en Simple) ni les tests `Bench*` ne traversent, par exemple la découpe à 200 %, « Mes projets », le Mix Pro, les chapitres 4 à 15 des tutos.
- **Les trois contrôles.** Aucun geste refusé dans les journaux (0 ligne « refus », « introuvable », « erreur »). Incident connu toujours là : écran rendu vide en texte après « Je le fais », image normale (H a : 6 écrans ; D a : 12 ; B b : 4 et plus). Il existait avant (B b : 8 écrans ; D a : 9) et le lot banc7 l'a laissé de côté (« E3 », `tour-6-banc7-rapport.md` l. 26). Le testeur passe alors par l'image : le banc pilote, mais il est aveugle par moments.
- **Le juge** (`verifie-chantier.py`, dernière ligne de `selftest-chantier.txt` de chaque trace) : 8 écarts après juge7, ci2 et gaucher ; 18 après langues ; 19 après tutogrille ; 22 après ecoute ; 40 après tutoconsignes ; 67 après guidecoquille ; 68 après reparations. Sur les 68 : 35 « vert sans rouge du même test avant lui », 32 « rouge sans vert », 1 fusion sans trace (`ea294587a`, le tutogrille refusé). Les lots écrivent des « rouge: » et « vert: » aux titres différents ou groupés. Le portail ne bloque pas sur ce compte ; le journal n'en parle plus après 22 h. Le départ du tour exigeait 7.
- **Vérification GitHub de la tête** (run 43375199284, état à 7 h 15) : contrôles rapides, séparation et moteur verts ; « Emulator smoke test » rouge (`JourneyTest.aFirstSongInTheModeSimple`, « nothing tagged wizard after 15000 ms », l'étiquette existe toujours dans `WizardScreen.kt` l. 173) ; « Build, unit tests, screenshots » encore en cours. `app/src/androidTest` n'a pas changé depuis la dernière verte (`git diff --stat 5957fac56 HEAD` vide) alors que les lots 3 et 6 ont touché l'écran d'accueil et la coquille : régression ou test capricieux, non tranché.
- **Conclusion.** Branché pour piloter (étiquettes, icônes, tutos) ; dérivé pour juger (68 écarts, 1 seul rejeu, écrans vides) ; non vérifié sur la vraie appli (émulateur rouge). La vérification unique qui le prouverait : `JourneyTest` vert sur émulateur pour la tête, complété par un siège de 20 gestes par mission.

### Ce que je n'ai pas pu vérifier

- Qui a vraiment joué chaque siège (discussion ou sous-agent neuf) : aucun rapport ne le dit.
- Les messages envoyés aux autres discussions et leurs états « bloqué » (je n'ai que le journal du chef).
- La cause des 4 redémarrages.
- Le coût en quota de la nuit.
- Le résultat final de la vérification 43375199284 (encore en cours à 7 h 08) et si l'échec sur émulateur est une vraie régression ou un test capricieux.
- Que les tests des lots 1, 6 et 7 ont bien été vus rouges avant le correctif : je l'ai déduit en lisant le code, sans lancer Gradle.
- Le contenu du lot 5 (pas sur une branche que j'ai lue) et le lot 10 (en cours).
- Les images des sièges (rangées sur une autre branche) et le détail des 1 110 parcours du balayage.
- Les heures exactes du journal entre 23 h et 3 h (horloge décalée) ; j'ai pris git et les journaux du portail.
- Pour la question G : que le banc pilote bien chaque mission sur la tête (seuls trois tutos ont été joués après les corrections) ; pourquoi le parcours sur émulateur échoue ; l'origine exacte du total « 203 verts, 44 sautés » (déduite des écarts entre lots, pas lue dans une trace).
