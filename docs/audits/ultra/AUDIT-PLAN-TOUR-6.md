# Audit du plan du tour 6 (2026-11-12, avant lancement)

Plan audité : le fichier `PLAN-TOUR-6.md` envoyé par Benjamin (97 lignes), pas encore poussé sur `ccr-80af1a1b-mn0wec` (tête lue : `71e371f0`). Lecture seule, rien lancé, 3 agents (un analyste, un sceptique, le rédacteur). Chemins courts : `ultra/` = `docs/audits/ultra/`, `bench/` = `scripts/bench/`, `human/` = `app/src/test/java/com/pocketstudio/app/human/`.

## 1. Verdict : prêt après corrections
- L'ossature est bonne : petit tour, porte d'entrée, règles d'arrêt, téléphone tenu à part, testeurs qui ne jugent ni le son ni la vitesse.
- Mais trois pièces ne peuvent pas tourner comme écrit (le siège œil neuf, les rejeux « par étiquette », plusieurs contrôles d'entrée), et deux critères de réussite sont gagnés d'avance.
- Et la porte d'entrée n'est pas franchie aujourd'hui : pas de retest v6 dans le dépôt, mixwake en cours, le juge affiche 18 écarts.

## 2. Corrections

### À faire avant le lancement
1. **Refaire le siège œil neuf** (§4, §7, question 1). Tel qu'écrit, il ne peut pas tourner.
   - Un siège n'a qu'un appareil et une police (`human/SeatOptions.kt` l. 35-37) : « tablette puis téléphone » demande deux sièges, donc 7 sièges et 9 agents.
   - « Sans plafond d'images » n'existe pas : une image pour 5 gestes, sans limite seulement avec `lit=non`, qui grise les textes (`scripts/human-act.py` l. 105-116). Chaque image mange un demi-geste (`human/HumanTester.kt` l. 351-352).
   - Au tour 5, le meilleur siège a vu 11 écrans en 95 gestes. Viser 36 écrans en 120 gestes est hors de portée : compter 10 à 15 par réglage.
   - La mission « aucune » est inconnue du banc, donc comptée ratée (`bench/seat-metrics.py` l. 421-436) : écrire `libre`.
   - Coût : un siège de plus, et le budget écrit en images.
2. **Réécrire les critères de réussite** (§1). Deux sont gagnés d'avance, un se contourne.
   - « 0 grave sur l'œil neuf » : la gravité se mesure en gestes de détour dans une mission (`docs/audits/GABARIT-RAPPORT-TESTEUR.md` l. 12), et ce siège n'a pas de mission. Le gabarit plafonne aussi à 10 défauts (l. 6) pour une trentaine d'écrans. Il lui faut sa propre feuille (une ligne par écran, les 10 questions, sans plafond) et sa propre règle de « grave ».
   - « 80 % des défauts du téléphone montrés par le banc » : déjà 85 % avant le tour (`ultra/sieges/tour-5/tableau-de-bord.md` l. 103), dont 7 par des tests écrits après coup. Par de vrais sièges : 9 sur 23, soit 39 %. Donner les deux chiffres, et ajouter les essais v4 à v6 au fichier, qui s'arrête au 8 novembre.
   - « 0 bloquant » : à compter après le tri des « banc? ». Une fiche non triée ne compte ni grave ni banc (`docs/audits/AUDIT-ULTRA.md` §12.3) ; elle ferait passer le critère.
   - Interdire de comparer « graves par siège » entre 24 sièges tirés au hasard et 6 sièges ciblés.
   - Coût : 30 minutes d'écriture.
3. **Rejeux : ne pas croire un « tenu » sur parole** (§3, §5).
   - Les 8 rejeux du tour 5 visent des pixels. Quatre attendent qu'un texte soit absent (`oracle.txt` de t5-02, 04, 05, 06). Après dix chantiers, le doigt tombe ailleurs, le texte n'y est pas, et le rejeu dit « tenu » à tort. Rien n'est gardé (`bench/rejeu-defauts.sh` l. 12-13). Le plan ne protège que le « non tenu ».
   - « Rejouer par étiquette » n'est pas un outil : `bench/rejeu-nouveau.sh` recopie les pixels (l. 23-31) et écrit dans `rejeux/`, pas `rejeux-attente/` (l. 14). t4-26 a été converti à la main (`ultra/tour-5-socle-rapport.md` l. 35). Un rejeu au pixel est refusé dans `rejeux/` (`bench/test_socle.py` l. 198-217).
   - À écrire : convertir les 8 rejeux en étiquettes avant le tour, ou exiger pour chaque « tenu » la preuve que le bon bouton a été touché.
   - Coût : 2 à 3 heures d'un agent, donc pas « sans agent », et une heure de machine sans marge.
4. **Réécrire la condition d'entrée avec des contrôles qui existent** (§2).
   - « 0 écart neuf » n'est pas une sortie du juge : il dit « chantier : N écarts » (`bench/verifie-chantier.py` l. 1051). Sur le commit candidat : 18, contre 6 avant le lot testapk (`ultra/portail/2026-11-12-tour-5-testapk/selftest-chantier.txt`). Le plan d'action promettait zéro. Écrire un chiffre.
   - « pre-vol ne dit rien » : il parle toujours ; attendre « PRÉ-VOL : PRÊT » (`bench/pre-vol.sh` l. 27-37).
   - Le selftest se joue par lot, après la fabrication des fiches (`bench/selftest.sh` l. 19) : le déplacer au §3. La quarantaine est vide ; les sauts permis sont dans `bench/selftest-sauts-connus.txt` (14 lignes).
   - La CI ne part pas seule sur cette branche (`ci.yml` l. 3-7) : dire qui la lance, et si la fumée sur émulateur doit être verte (rouge sur `c55ffc1`).
   - `depart.txt` et `telephone.tsv` sont inventés par le plan : donner leurs colonnes, et relier la liste du téléphone à `defauts-caches.tsv` et `hors-banc.tsv`, sinon elle n'entre dans aucun calcul.
   - Coût : 30 minutes.
5. **Les deux notes : dire qui note** (§1, §6).
   - Les 6 points de la grille « banc crédible » parlent du juge, du faux moteur, des gardes et du selftest (`ultra/AUDIT-BANC-CREDIBLE.md` l. 23-29). Aucun ne se mesure avec des testeurs. Un rédacteur en 1 h 30 recopierait le journal de ceux qui ont réparé.
   - La grille « prise en main » cite « longueur d'export en 3 touchers » : ce contrôle n'existe pas dans `NavigationMapTest.kt` (l. 47-49, 63 : Export, Mes fichiers, Recette seulement).
   - À écrire : un agent neuf, dans une autre discussion, note point par point (tenu, non tenu, non vérifié). Jamais l'agent principal.
   - Coût : environ un siège.
6. **Dire ce que le tour ne regarde pas** (§1, §4). « Un par coin de l'appli » est trop beau.
   - Les cinq sièges à mission verront 14 écrans sur 36, 22 au mieux. Quatorze écrans ne seront vus que par l'œil neuf, s'il y arrive.
   - Sans siège : langues et lisibilité (la pire zone du tour 5, 0 promesse tenue sur 8, `ultra/tour-5-synthese.md` l. 159), piano roll, Mix et effets, voix, réglages, premier lancement, paysage, police à 200 %.
   - Deux bloquants sont revérifiés hors de leur réglage d'origine : A a #27 a été vu en petit téléphone à 130 % au premier lancement, E a #117 en Mode simple. Le lot de correction ne les a pas reproduits (`docs/PROGRESS.md` l. 25). Reprendre les réglages d'origine.
   - « Six langues » : il y en a cinq.
   - Coût : 20 minutes.
7. **« A fini » n'est pas « a réussi »** (§4, §5). Leçon du méta-audit 2 absente du plan.
   - L'oracle `simple` dit seulement « resté en Mode simple », `padlive` « un son au premier toucher », `exports` « deux fichiers » (`bench/seat-metrics.py` l. 209-214, 401-414). A a peut « réussir » avec le changement de partie encore cassé.
   - Chaque point « à vérifier d'abord » devient une promesse numérotée du brief, avec oui ou non et l'écran.
   - `bench/fabrique-sieges.py` ne choisit rien : il lit un tableau écrit à la main (l. 2-4). « D'abord les chantiers corrigés » n'est pas codé. Dire qui écrit ce tableau.
   - Coût : 30 minutes.
8. **Refaire le compte du quota et plafonner l'après** (§7, §8).
   - Le plan additionne 5 + 2 + 2 et trouve 8 : c'est 9, et 10 avec deux sièges œil neuf.
   - En gestes, environ un tiers du tour 5. En images, plutôt la moitié : 60 à 70 images, quand un siège du tour 5 en lisait 8 en moyenne.
   - Le vrai coût est après : 13 lots sur 29 refusés à la première relecture (`docs/AIDE-FUSIONS-TESTS.md` l. 7), et le §8 n'a aucun plafond. Écrire un nombre de lots.
   - « On répare le banc » (§6) : plafonner à un lot. C'est la leçon du test APK, 70 % de l'effort parti sur le robot.
   - Lenteur de la séparation Remix sous le banc : réparée ou non, (non vérifié).
9. **Deux lignes de mécanique** (§5).
   - `verdicts.tsv` : écrit par « l'agent banc seulement » (`ultra/verdicts.tsv` l. 2). Nommer le lot `tour-6-tri` équipe banc.
   - Gestes sauvés : aucun script ne les copie ; au tour 5 ils ont été ajoutés après coup (commit `076fc5cd`). Écrire qui les commite, à la fin de chaque lot, avant le lot suivant.

### Peut attendre
10. Le tableau de bord dira « non » ou « ? » sur D, E, H et I quoi qu'il arrive (`bench/tableau-de-bord.py` l. 219, 230, 241-243). Le plan ne cite que I : citer les quatre.
11. Trois petites pannes : `ci-etat.sh` écrit « vert (sha) » quand le tableau veut « vert » ; le contrôle « CI déjà rouge » du pré-vol est mort (`bench/pre-vol.sh` l. 113) ; `scripts/human-replay.sh` ne peut plus rien rejouer.
12. Un gel de moins d'une seconde au geste 0 compte « siège coupé » (`bench/tableau-de-bord.py` l. 126-128). La tablette à 130 % n'a jamais été jouée au tour 5 : ce gel peut déclencher la règle d'arrêt du §6 pour rien.
13. T3, les questions à l'oreille, devient facultatif au §10 alors que l'auto-audit le garde sur le chemin (`ultra/auto-audit-test-apk-2026-11-12.md` l. 35).
14. `docs/PROGRESS.md` l. 23 dit testapk « pas poussé » : il l'est (`d66b67ce`).

## 3. Questions pour Benjamin (réponse du type « 1A, 2A, 3A, 4A »)
1. **Le siège œil neuf ?**
   - A. (conseillé) Deux sièges, tablette puis téléphone. Conséquence : 7 sièges, 9 agents, et les écrans non atteints sont listés.
   - B. Un seul siège, sur tablette. Conséquence : 6 sièges, mais personne ne regarde le téléphone d'un œil neuf.
   - C. Des captures fabriquées pour tous les écrans. Conséquence : complet, mais l'outil ne couvre que 29 écrans sur 36 ; une demi-journée avant.
2. **Comment le grand audit s'arrête-t-il ?** Six sièges ne comptent pas pour la règle d'arrêt, et le plan ne prévoit plus de grand tour.
   - A. (conseillé) Une règle légère écrite avant le tour : aucun bloquant après le tri, rejeux tenus, ta recette sans point rouge. Conséquence : une sortie existe, moins exigeante.
   - B. Garder la règle : deux tours de 16 sièges de plus. Conséquence : environ cinq fois le quota du tour 6.
   - C. Décider après le tour. Conséquence : on fixerait la barre en voyant le résultat.
3. **Qui refait les deux notes ?**
   - A. (conseillé) Un agent neuf, dans une autre discussion. Conséquence : environ un siège de quota, note crédible.
   - B. Le rédacteur de la synthèse, note marquée « provisoire ». Conséquence : gratuit, peu fiable.
   - C. Pas de note ce tour-ci, seulement le tableau point par point.
4. **Les coins que personne ne regarde ?**
   - A. (conseillé) On l'écrit comme limite connue. Conséquence : quota tenu.
   - B. Un siège de plus : Mix, piano roll, réglages, premier lancement. Conséquence : un huitième de quota en plus.

## 4. Résumé pour Benjamin
1. Le plan est bon dans l'idée, mais il n'est pas prêt à partir.
2. Le testeur « œil neuf » ne peut pas marcher comme écrit : il faut le couper en deux, tablette et téléphone.
3. Deux objectifs étaient gagnés avant de jouer, et quatre vieux rejeux peuvent dire « c'est corrigé » à tort.
4. La porte d'entrée n'est pas franchie : il manque ton retest v6 et la fin du lot mixwake.
5. Une heure et demie de réécriture, deux à trois heures sur les rejeux, tes quatre réponses, et on peut lancer.

## 5. Réponses de Benjamin (2026-11-12) : 1A, 2A, 3A, 4B
À reporter dans le plan par la discussion qui le tient (et dans `docs/DECISIONS.md`, que cet audit ne touche pas).
- **1A** : deux sièges œil neuf, un sur tablette à 130 %, un sur téléphone à 100 %, mission `libre`, budget écrit en images. Les écrans non atteints sont listés « non vus ».
- **4B** : un siège de plus en Mode Pro pour Mix, piano roll, réglages et premier lancement (`fresh=reel`).
- **Ce que ça donne** : 8 sièges (5 à mission, 1 de plus, 2 œil neuf), un lecteur, une synthèse : **10 agents en tout, 3 au plus en même temps**, en trois lots (3, 3, 2). À annoncer ainsi avant le lancement.
- **Quota, à réécrire au §7** : environ 40 % du tour 5 en gestes et 60 % en images (estimation, non mesurée), avant les lots de correction.
- **2A** : la règle de sortie s'écrit au §1 avant le tour : aucun bloquant après le tri des « banc? », tous les rejeux tenus avec leur preuve, la recette de Benjamin sans point rouge. Le tableau de bord de l'audit ultra reste une information.
- **3A** : les deux notes sont refaites après le tour par un agent neuf, dans une autre discussion, point par point (tenu, non tenu, non vérifié). Il ne compte pas dans les 10 agents. La synthèse du tour ne donne pas de note.
- Les corrections 2 à 9 du § 2 restent à faire telles quelles ; la question 1 du plan (§11) disparaît, remplacée par 1A.
