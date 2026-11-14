# Plan du tour 6 : le petit tour de contrôle (étape 5)

Écrit le 2026-11-12, revu le même jour après l'audit `AUDIT-PLAN-TOUR-6.md` et tes réponses 1A, 2A, 3A, 4B. **Rien n'est lancé.**

## En trois lignes
1. Huit testeurs simulés rejouent l'appli : cinq vérifient les défauts du tour 5, un regarde le Mode Pro (Mix, piano roll, réglages, premier lancement), deux regardent seulement si les écrans sont clairs, un sur tablette, un sur téléphone.
2. On part de ton dernier essai sur le téléphone : ce qui reste rouge est noté à part, avec ses chiffres, sans le faire chercher par les testeurs.
3. On s'arrête quand il n'y a plus de bloquant, que tous les rejeux sont tenus avec leur preuve, et que ta recette n'a plus de point rouge. Les notes sont refaites ensuite, ailleurs, par un agent neuf.

## 1. But, règle de sortie, et ce que le tour ne regarde pas
**Règle de sortie de l'audit (réponse 2A, écrite avant le tour).** Le grand audit est fini quand les trois sont vrais :
- **aucun bloquant** après le tri des « banc? » (§ 5) ;
- **tous les rejeux tenus**, chacun avec la preuve que le bon bouton a été touché (§ 3) ;
- **ta recette sans point rouge** (la page Recette du téléphone, après le dernier lot de correction).
Le tableau de bord de l'audit ultra reste une information. L'ancienne règle (16 sièges, deux tours de suite) est abandonnée.

**Critères du tour lui-même :**
- **0 bloquant, compté après le tri.** Une fiche « banc? » pas encore tranchée ne disparaît pas : tant qu'elle n'a pas sa ligne dans `verdicts.tsv`, elle compte avec sa gravité.
- **0 grave sur les deux sièges œil neuf**, avec leur propre règle (§ 4). La règle des autres sièges compte des gestes de détour dans une mission ; ces sièges n'en ont pas.
- **Les 3 bloquants et 5 graves du tour 5 tranchés** : rejeu tenu avec sa preuve, ou défaut encore là avec sa preuve.
- **Défauts cachés : toujours deux chiffres.** Aujourd'hui 85 % montrés par le banc (`sieges/tour-5/tableau-de-bord.md` l. 103), dont des tests écrits après coup ; par de vrais sièges, 9 sur 23, soit 39 % (`defauts-caches.tsv`, colonne « vu »). Le seuil de 80 % n'est pas réussi s'il ne tient que grâce aux tests écrits après coup.
- **Interdit** : comparer « graves par siège » entre les 24 sièges tirés au hasard du tour 5 et les 8 sièges ciblés du tour 6.

**Ce que le tour 6 ne regarde pas** (dit d'avance) :
- le paysage, la police à 200 %, le thème sombre ;
- les langues autres que les cinq du § 4 (la zone « langues et lisibilité » était la pire du tour 5 : 0 promesse tenue sur 8) ;
- l'enregistrement de la voix (aucune mission « voix ») ;
- le son, la vitesse, les décrochages : le banc ne les sait pas, ils restent sur la liste du téléphone ;
- les écrans qu'aucun siège n'atteint : listés « non vus » à la fin, jamais devinés.
- Le tableau de bord dira « non » ou « ? » sur les lignes D, E, H et I quoi qu'il arrive (promesses vues par 2 sièges, missions des œil neuf sans son, émulateur, 16 sièges). C'est attendu.

## 2. Condition d'entrée (toutes, sinon on ne lance pas)
Chaque ligne a un contrôle qui existe ; son résultat est recopié dans `sieges/tour-6/depart.txt`.
1. **Lot `mixwake`** relu et fusionné par le portail, ou abandonné et noté dans la liste du téléphone. **Fait** : fusionné (`f2051def`, traces `db66a3ce`).
2. **Retest v6 fait** : tu as lancé « Retester les corrections » ; le rapport est rangé dans `docs/tests-telephone/<date>-retest-v6/` (des chiffres seulement). Aujourd'hui : pas encore dans le dépôt.
3. **Les 8 rejeux du tour 5 visent par étiquette.** Un autre agent les convertit en ce moment (atelier `eq-tour-5-rejeux6`). Contrôle : son lot est fusionné, et aucun `cmd-N.txt` de `rejeux-attente/` ne touche par pixels (`test_socle.py` ne lit que `rejeux/` : on étend ce contrôle à `rejeux-attente/`, ou on le fait à la main et on le note).
4. **Les rejeux gardent leur journal, tenus compris.** Aujourd'hui `rejeu-defauts.sh` efface tout à la fin et ne garde que les échecs de `rejeux/`. Sans journal, pas de preuve d'un « tenu ». À ajouter au lot rejeux6, sinon un petit lot banc avant le tour.
5. **Le juge du chantier** : `verifie-chantier.py --resume <selftest-resume.txt>` écrit « chantier : N écarts ». **Il faut N ≤ 7, et ce sont exactement les 7 écarts acceptés** (liste de lancement, ci-dessous ; décision dans `docs/decisions/tour-5-plan6.md`), chaque écart nommé dans `depart.txt` avec sa raison. Où on en est : 6 avant le lot testapk, **18 sur le commit testapk** (`portail/2026-11-12-tour-5-testapk/selftest-chantier.txt`), 23 aujourd'hui sur la tête de fx-int (`9be3e12d`, sans résumé de selftest, donc pas tout à fait le même compte). **Il faut le faire redescendre avant de lancer** ; la cible du plan d'action reste 0.
6. **Pré-vol** : `pre-vol.sh --ci` doit finir par « **PRÉ-VOL : PRÊT** ». Il parle toujours : les lignes « À VOIR » sont lues et recopiées dans `depart.txt`.
7. **CI sur le commit exact.** Sur cette branche, la CI ne part pas seule (elle part sur une PR, sur `main` ou à la main, `ci.yml` l. 3-7). **L'agent principal la lance.** Il faut **les deux verts** : la construction **et** la fumée sur émulateur (rouge sur `c55ffc1` au tour 5). `ci-etat.sh <sha>` en donne le résultat, recopié dans `sieges/tour-6/hors-banc.tsv` (lignes `ci_build`, `ci_emulateur`).
8. **Commit de départ** choisi et écrit. Aujourd'hui aucun candidat n'est final (mixwake et rejeux6 pas fusionnés, retest absent).

**Colonnes de `depart.txt`** (une ligne « clé, tabulation, valeur ») : `sha`, `date`, `branche`, `ci_build`, `ci_emulateur`, `chantier_ecarts` (le N, puis la liste), `prevol` (la dernière ligne, puis les « À VOIR »), `rejeux6` (sha de la fusion), `retest_v6` (dossier), `mixwake` (fusionné ou abandonné).

## 3. Avant les testeurs (environ 1 h de machine, plus un agent si un rejeu diverge)
- **Le selftest se joue par lot**, juste après la fabrication des fiches de ce lot et avant ses testeurs : `selftest.sh sieges/tour-6/lot<X>`, sans `PS_SKIP_SELFTEST`. Seuls les sauts de `selftest-sauts-connus.txt` (14 lignes) sont permis ; la quarantaine est vide et le reste.
- **Rejouer les 9 rejeux en attente** (`rejeux-attente/t5-01` à `t5-08`, plus `rejeux/t4-26`) avec `rejeu-defauts.sh`.
  - **Un « tenu » ne compte qu'avec sa preuve** : son journal montre que le dernier toucher a visé la bonne étiquette (ligne « DOIGT ») et sur le bon écran. Quatre rejeux attendent qu'un texte soit **absent** (t5-02, 04, 05, 06) : sans cette preuve, un doigt tombé ailleurs dirait « tenu » à tort.
  - « Divergé » (étiquette introuvable) n'est ni tenu ni non tenu : un agent regarde, corrige la cible, rejoue. Deux essais, puis le défaut entre dans la liste du tour 6.
  - Non tenu avec la bonne cible : le défaut entre d'office dans la liste du tour 6.
- **La liste du téléphone** (`sieges/tour-6/telephone.tsv`) : chaque point encore rouge ou orange après la v6, plus les ❌ de T3 s'il a été fait, plus les points rouges encore ouverts de la recette du 8 novembre.
  - **Colonnes** : `id` (T6-T01…), `source` (retest-v6, T3, recette-08-11), `ecran`, `chiffres`, `etat` (rouge, orange), `cause` (ou « non expliqué »), `cache_id` (son id dans `defauts-caches.tsv`), `banc` (oui, partiel, non).
  - **Lien avec les calculs** : chaque ligne qui est un défaut de l'appli reçoit sa ligne dans `defauts-caches.tsv` (ids B24 et suivants ; le fichier s'arrête au 8 novembre, on y ajoute les essais v4 à v6) ; le nombre de lignes rouges va dans `hors-banc.tsv`, ligne `recette_points_rouges`. Sans ça, la liste n'entre dans aucun calcul.
  - Hard techno y entre « non expliqué » (25 → 0 décrochage sans changement du moteur).
- **Les fiches des sièges** : **l'agent principal écrit à la main** `sieges/tour-6/sieges-proposes.tsv` (le tableau du § 4). `fabrique-sieges.py` ne choisit rien : il recopie ce tableau en `start.txt` et en consignes. Les promesses viennent dans cet ordre : d'abord les points « à vérifier d'abord » (promesses libres), puis celles du catalogue des chantiers corrigés. 5 au plus par siège. Consignes v2 copiées dans `sieges/tour-6/`.

## 4. Les huit sièges
| Siège | Zone | Appareil | Mode | Langue | Mission | Promesses (oui ou non, avec l'écran) |
|---|---|---|---|---|---|---|
| A a | Mon morceau en Simple | petit téléphone 130 % (`phone360`, `fresh=reel`) | Simple, choisi par le siège | hindi | simple | 1. changer de partie (bloquant A a #27) ; 2. « ▶ cette partie » et « ▶ le morceau » ; 3. pas de « Simple ou Pro ? » au 2e morceau ; 4. « Plus » montre la Recette |
| A b | Mélodie et Pad Live | téléphone 100 % (`phone`) | Simple | français | padlive | 1. mélodie gardée après Retour pendant la prise, Pad Live, puis le projet (bloquant E a #117) ; 2. « Tout couper » coupe tout, tempo compris ; 3. un glissé ne lance pas d'effet ; 4. le pad attend la mesure |
| A c | Partager et retrouver | téléphone 100 % | Simple | français | exports | 1. partager en 3 gestes ; 2. « Mes exports » atteint en Simple (grave C d #50) ; 3. le .zip listé ; 4. la longueur annoncée est celle exportée |
| B a | Remix et découpe, d'une main | téléphone 100 %, main gauche | Simple | espagnol | remix-voix | 1. gaucher (grave F d) ; 2. une seule carte conseillée ; 3. alerte « la fin sera coupée » ; 4. outils pas cachés sous la forme d'onde |
| B b | Tutos, Guide, synthé, courbes | tablette 8 pouces 130 % (`tablet8`) | Pro | allemand | tuto-pro | 1. Swing du tuto Pro (bloquant B c #23) ; 2. ☆ sur les sons (grave D b #39) ; 3. la nouvelle courbe se montre ; 4. « Voir le réglage » y mène |
| B c | **Pro, en plus** (réponse 4B) | téléphone 100 %, `fresh=reel` | Pro, choisi par le siège | arabe (droite à gauche) | libre | 1. premier lancement jusqu'au Mode Pro ; 2. Mix : régler un niveau et un effet ; 3. piano roll : poser et déplacer une note ; 4. réglages : changer la langue et revenir |
| C a | **Œil neuf, tablette** | tablette 8 pouces 130 % | Simple, puis Pro | français | libre | sa feuille à lui (ci-dessous) |
| C b | **Œil neuf, téléphone** | téléphone 100 % | Simple, puis Pro | français | libre | sa feuille à lui (ci-dessous) |

- **Réglages d'origine des deux bloquants repris** : A a #27 avait été vu en hindi, petit téléphone à 130 %, au premier lancement ; E a #117 en français, téléphone 100 %, Mode simple. Le lot de correction ne les avait pas reproduits ; les revoir ailleurs ne prouverait rien.
- **Cinq langues** : hindi, français, espagnol, allemand, arabe.
- **Avec `fresh=reel`**, le banc ne règle pas le mode d'avance : la consigne dit au siège lequel choisir.
- **« A fini » n'est pas « a réussi ».** L'oracle de la mission dit peu : `simple` = « resté en Mode simple », `padlive` = « un son au premier toucher », `exports` = « deux fichiers ». Un siège peut « réussir » avec le défaut encore là. Ce qui compte, ce sont les **promesses numérotées**, chacune avec oui ou non et le numéro d'écran.
- Les points du téléphone ne sont **pas** donnés aux testeurs comme défauts à chercher. A c passe par Mix et le note (« vu, rien à juger »).
- Les six sièges à mission finissent par les questions 1, 6 et 9 de l'œil neuf sur leur dernier écran (une image chacun).

**Les deux sièges œil neuf (réponse 1A).**
- **Un appareil chacun** : le banc ne donne qu'un appareil et une police par siège.
- **Mission `libre`** : « aucune » est inconnue du banc et compterait ratée. Son verdict de mission (il demande un son et une sauvegarde) n'est pas leur critère.
- **Budget écrit en images** : 30 images au plus, et `gestes.max=100` (une image compte un demi-geste). Le premier écran d'une route neuve a toujours droit à son image ; ensuite, une pour 5 gestes. Les textes restent lisibles (pas de `lit=non`, qui les grise). On vise **10 à 15 écrans par siège**, pas 36 (au tour 5, le meilleur siège en a vu 11 en 95 gestes).
- **Leur feuille** (`oeil-neuf.tsv`, une ligne par écran vu, sans plafond) : l'écran, le numéro d'image, une réponse à chacune des 10 questions de `AUDIT-PRISE-EN-MAIN.md` § 7, les écarts.
- **Leur règle de « grave »** : sur cet écran, quelqu'un de neuf ne sait pas quoi toucher pour avancer (questions 1 et 3), ou croit à tort que son action a raté ou réussi (questions 6 et 7), ou un mot de bouton est coupé (question 9). Le reste est moyen ou mineur. Chaque grave cite son image ; le lecteur la regarde lui-même.
- **Écrans non atteints** : listés « non vus » dans `sieges/tour-6/ecrans-oeil-neuf.txt`, face à `carte-ecrans.tsv` (36 écrans).

## 5. Ce que rend chaque testeur, et comment un défaut se prouve
- `rapport.md` au gabarit v2 (60 lignes, 10 défauts au plus ; les œil neuf ajoutent leur feuille), `mesures.json`, `journal.txt`, `images.txt`, les `cmd-N.txt` et `screen-N.txt`.
- **Les gestes sont commités à la fin de chaque lot, avant le lot suivant**, par l'agent principal : `git status --short sieges/tour-6/` doit être vide avant de lancer le lot d'après. Aucun script ne le fait ; au tour 5, ils ont été ajoutés après coup.
- Chaque fiche porte le geste `#N`, la ligne d'écran copiée, la gravité par conséquence, `revu`, la case `banc?` et le numéro de la promesse.
- **Bloquant ou grave** : refait par le testeur (`revu: oui`), puis un rejeu créé dans `rejeux-attente/` (`PS_REJEUX_DIR=…/rejeux-attente rejeu-nouveau.sh`), dont les touchers sont ensuite recopiés en étiquettes à la main, comme pour t4-26 (le script recopie des pixels). Sans rejeu qui le montre, il ne compte que « moyen ».
- **Case « banc? »** à partir de moyen : le lecteur propose ; **la ligne de `verdicts.tsv` est écrite par le lot banc `tour-6-tri`** (le fichier est réservé à l'agent banc, l. 2), avec les preuves du § 12.3 de `AUDIT-ULTRA.md`.
- Le tableau de bord (`tableau-de-bord.py`) est recalculé par le portail après la fusion des rapports.

## 6. Déroulé et nombre d'agents
| Temps | Qui | Agents en même temps |
|---|---|---|
| Lot 1 : A a, A b, A c (deux bloquants et le partage) | 3 testeurs | 3 |
| Commit des gestes, contrôle | l'agent principal | 0 |
| Lot 2 : B a, B b, B c | 3 testeurs | 3 |
| Commit des gestes, contrôle | l'agent principal | 0 |
| Lot 3 : C a, C b (œil neuf) | 2 testeurs | 2 |
| Lecture : `fuse-reports.py --maj-base`, « revient ? » tranchés dans `revient.tsv`, « banc? » proposés, grappes refaites à la main, promesses relues | 1 lecteur neuf | 1 |
| Synthèse : `tour-6-synthese.md`, tableau de bord, liste du téléphone, lots de correction proposés. **Pas de note.** | 1 rédacteur neuf | 1 |

**Annonce : 10 agents en tout, 3 au plus en même temps, en trois lots (3, 3, 2).** Aucun n'a écrit de code des étapes 2 à 4. Ne sont pas comptés : les lots de correction, le lot `tour-6-tri`, et l'agent qui refait les notes.

**Contrôle entre les lots** : si 2 sièges d'un lot sont coupés par le banc, ou si le même incident touche 2 sièges, on s'arrête. **Un seul lot de réparation du banc**, pas plus : s'il ne suffit pas, le tour s'arrête et je te le dis. Un gel de moins d'une seconde au geste 0 (la tablette à 130 % n'a jamais été jouée) est lu avant d'être compté « siège coupé ».

**Les deux notes (réponse 3A)** : refaites **après le tour**, par un agent neuf, **dans une autre discussion**, point par point : tenu, non tenu, non vérifié. Jamais l'agent principal. La grille « banc crédible » parle du juge, du faux moteur, des gardes et du selftest : elle se note sur le code et les tests, pas sur les testeurs. La grille « prise en main » cite « longueur d'export en 3 touchers » : ce contrôle n'existe pas dans `NavigationMapTest` (il y a Export, Mes fichiers, Recette) ; il reste « non vérifié » tant qu'il n'est pas écrit.

## 7. Quota et durée (estimations, non mesurées)
- **En gestes** : environ 40 % du tour 5 (8 sièges de 100 à 120 gestes, contre 24).
- **En images** : environ 60 % du tour 5 (les deux œil neuf en prennent une soixantaine à eux deux ; un siège du tour 5 en lisait 8 en moyenne).
- Lecture et synthèse : à peu près deux sièges de plus. Les notes : environ un siège, à part.
- **Le vrai coût est après** : au tour 5, 13 lots sur 29 ont été refusés à la première relecture. D'où le plafond du § 8.
- Durée : un lot en 1 h 30 à 2 h, la lecture en 1 h, la synthèse en 1 h 30. Un peu plus d'une journée. Si le quota de la semaine est en alerte : on finit le lot en cours, on commite, on pousse, on s'arrête.

## 8. Après le tour
- **Au plus 6 lots de correction**, `tour-6-tri` compris. Une branche chacun (`equipe-atelier.sh cree tour-6 <zone>`), fiches de `docs/AIDE-FUSIONS-TESTS.md` collées dans les consignes, commit « rouge: » puis « vert: », relecture neuve, `pre-vol.sh`, puis le portail.
- Ordre : `tour-6-tri` (verdicts, rejeux) d'abord, puis les bloquants, puis les graves, puis les moyens vus par 2 sièges. Ce qui ne rentre pas dans les 6 lots va dans `docs/REVIEW.md`, avec ce qu'on sait.
- **Une seule CI par paquet de fusions**, jamais relancée sur un commit déjà rouge.
- Fin : les rejeux du tour 6 tenus avec leur preuve, et les deux sièges œil neuf rejoués seuls. Pas de nouveau tour complet.
- La liste du téléphone reste à part : un lot seulement si la cause est connue ; sinon la ligne « non expliqué » est gardée pour le grand audit final (`docs/PLAN-AUDIT-FINAL.md`), que ce plan ne lance pas.
- La lenteur de la séparation Remix sous le banc : réparée ou non, non vérifié. On la regarde au lot 2 (siège B a).

## 9. Règles d'arrêt (pas de boucle)
- Un siège coupé par le banc est relancé **une fois**. Coupé une deuxième fois : incident compté, siège pas refait.
- Un défaut a **deux essais de correction** au plus. Ensuite il va dans `docs/REVIEW.md`.
- Le banc : **un lot de réparation**, pas deux.
- Sur le téléphone : pas de nouveau « Tout tester » avant la fin des corrections. Au plus un « Retester les corrections » à la fin, qui sert à la règle de sortie.
- Une étape qui prend deux fois sa durée est coupée, et c'est dit en trois lignes.
- Pas de tour 7 automatique : la règle de sortie du § 1 dit si on a fini.

## 10. Ce que Benjamin a à faire
- **Avant** : le retest v6 (« Retester les corrections », puis « Envoyer le rapport »). T3, les 8 questions à l'oreille, si tu as 10 minutes.
- **Pendant** : rien.
- **Après** : une seule demande groupée, par la page Recette, pour ce que le banc ne peut pas trancher (son, temps, gel de 789 ms en allemand à 200 %, les 4 essais de partage restés de côté). Environ 15 minutes. C'est aussi elle qui dit si ta recette est sans point rouge.

## 11. Tes réponses (2026-11-12) : 1A, 2A, 3A, 4B
- **1A** : deux sièges œil neuf, tablette 130 % et téléphone 100 % (§ 4).
- **2A** : la règle de sortie légère, écrite au § 1.
- **3A** : les notes refaites après le tour par un agent neuf, ailleurs (§ 6).
- **4B** : un siège Pro de plus pour Mix, piano roll, réglages et premier lancement (B c).
Plus de question ouverte dans ce plan. Les choix faits en le corrigeant sont dans `docs/decisions/tour-5-plan6.md`.

## 12. Points de l'audit non retenus ou nuancés
Tout a été revérifié dans les fichiers cités. Trois nuances :
- « `rejeu-nouveau.sh` écrit dans `rejeux/`, pas `rejeux-attente/` » : **en partie faux**. Il écrit dans `rejeux/` par défaut, mais son en-tête (l. 8-9) dit d'utiliser `PS_REJEUX_DIR=…/rejeux-attente` pour un défaut pas encore corrigé. Le reste est juste : il recopie les touchers en pixels.
- « Une image pour 5 gestes » : **incomplet**. Le premier écran d'une route neuve a toujours droit à son image (`human-act.py` l. 107 et 113). Le plafond gêne moins l'œil neuf que ne le dit l'audit ; mais chaque image coûte un demi-geste, donc le budget en images reste.
- « La CI ne part pas seule » : **vrai, sauf si une PR est ouverte** sur la branche (`ci.yml` l. 4). D'où « l'agent principal la lance » au § 2.
- Repris sans recompter : « 14 écrans sur 36 vus par les sièges à mission » et « 11 écrans en 95 gestes ».

## Liste de lancement (à cocher dans l'ordre, depuis `/home/user/fx-int`, sur le commit de départ)
Chaque résultat est recopié dans `sieges/tour-6/depart.txt` (gabarit prêt). Un contrôle qui ne donne pas la sortie attendue : on ne lance pas.
Prêt depuis le lot `plan6` : `sieges/tour-6/sieges-proposes.tsv` (8 sièges du § 4), `consignes-testeur.md` (v2 + partie « Round 6 » : promesses oui/non, les 10 questions œil neuf, leur règle de grave), `promesses-numerotees.tsv` (24 promesses), `oeil-neuf-gabarit.tsv`, `ecrans-oeil-neuf-carte.tsv` (36 écrans), `telephone.tsv` (6 lignes, B24 à B26 ajoutées à `defauts-caches.tsv`), `hors-banc.tsv`, `depart.txt`.

1. **mixwake** : `git log --oneline -1 --grep "Merge team mixwake of tour-5"` → `f2051def …`. Fait.
2. **rejeux6 fusionné** : `git log --oneline -1 --grep "Merge team rejeux6"` → une ligne (son sha dans `depart.txt`, clé `rejeux6`). Aujourd'hui : rien.
3. **Rejeux par étiquette** : `grep -lE "^(touche|appui-long|double-touche) [0-9]" docs/audits/ultra/rejeux-attente/*/cmd-*.txt docs/audits/ultra/rejeux/*/cmd-*.txt` → aucune ligne. Aujourd'hui : 279 fichiers par pixels dans les 8 dossiers `t5-0*`.
4. **Journal des rejeux tenus** : `PS_REJEUX_DIR=docs/audits/ultra/rejeux-attente scripts/bench/rejeu-defauts.sh t5-01-partie-simple` puis le journal du rejeu tenu est encore là, avec ses lignes `DOIGT`. Aujourd'hui : non (seuls les échecs de `rejeux/` sont gardés).
5. **Retest v6** : `ls -d docs/tests-telephone/*retest-v6*` → un dossier (clé `retest_v6`) ; ses chiffres remplacent les lignes `tout-tester-v4-v5` de `telephone.tsv`, et `recette_points_rouges` de `hors-banc.tsv` est recompté (4 aujourd'hui).
6. **Pré-vol** : `bash scripts/bench/pre-vol.sh --ci` → dernière ligne `PRÉ-VOL : PRÊT (N à voir).` ; chaque « À VOIR » recopié (clé `prevol`).
7. **CI à la main** : `gh workflow run ci.yml --ref fx-int`, puis `scripts/bench/ci-etat.sh <sha> --ecris docs/audits/ultra/sieges/tour-6/hors-banc.tsv` → `ci_build vert` **et** `ci_emulateur vert`.
8. **Fiches du lot 1** : `python3 -I scripts/bench/fabrique-sieges.py /home/user/fx-int/docs/audits/ultra/sieges/tour-6 --tour tour-6` → `8 sièges` (chemin absolu : les consignes donnent `SEAT=` aux testeurs). Puis `rm -r docs/audits/ultra/sieges/tour-6/lotB docs/audits/ultra/sieges/tour-6/lotC` : un lot sans ses traces est un écart du juge (D1) ; la même commande les refait à l'identique avant leur lot.
9. **Selftest du lot 1** : `scripts/bench/selftest.sh docs/audits/ultra/sieges/tour-6/lotA` (sans `PS_SKIP_SELFTEST`) → vert ; seuls les sauts de `selftest-sauts-connus.txt`.
10. **Juge** : `python3 -I scripts/bench/verifie-chantier.py --resume docs/audits/ultra/sieges/tour-6/lotA/selftest-resume.txt > docs/audits/ultra/sieges/tour-6/lotA/selftest-chantier.txt; tail -1 docs/audits/ultra/sieges/tour-6/lotA/selftest-chantier.txt` → `chantier : 7 écarts` (ou moins), et `grep -cE "^(2b34c216|a2a6ec58|11c48708|96a7a702|ade1a174|9b8fc4fd|411b2d51) " docs/audits/ultra/sieges/tour-6/lotA/selftest-chantier.txt` → le même nombre : aucun autre écart. Les 7 acceptés : anciens `2b34c216`, `a2a6ec58`, `11c48708`, `96a7a702`, `ade1a174` ; nouveaux `9b8fc4fd`, `411b2d51`. Aujourd'hui (2026-11-12) : 24 sur la trace mixwake, 45 sur la tête de fx-int avec ce résumé (résumé d'un autre code, gardes non vertes, fusion retest sans traces).
11. **Commit de départ** : `git rev-parse --short HEAD` (clé `sha`), `git status --short docs/audits/ultra/sieges/tour-6/` → vide après le commit de `depart.txt`, `hors-banc.tsv` et du lot A.
12. **Aucun `?` dans `depart.txt`** : `grep -cP "\t\?$" docs/audits/ultra/sieges/tour-6/depart.txt` → `0`. Alors seulement : lot 1.

À savoir : dès que `sieges/tour-6/` existe sur fx-int, le portail calcule aussi le tableau de bord du tour 6 et l'écrit dans `sieges/tour-6/tableau-de-bord.md` (le dernier tour, qui porte aussi les lignes du tour 5) ; `sieges/tour-5/tableau-de-bord.md` n'est plus mis à jour (le chiffre de 85 % du § 1 se lit alors là). Les défauts cachés « vus par de vrais sièges » passent de 9 sur 23 à 9 sur 26 (B24 à B26 sont hors banc : le taux de 85 % ne bouge pas).
