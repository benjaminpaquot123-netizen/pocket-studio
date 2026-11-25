# Tour 9, lot « tests-fragiles » : rapport

Branche : eq/tour-9-tests-fragiles, partie de l'intégration d87dea73 (puis ccr-80af1a1b-mn0wec fusionnée).
Deux tests capricieux, tests seulement, aucune ligne de l'appli touchée.

tests: :app:testDebugUnitTest --tests '*TutoKickSoundTest' --tests '*Tour5BloquantsTest'
chemin: tuto, étape « écoute le kick » | test: TutoKickSoundTest#theKickStepNowSaysShowsAndTicks
chemin: Clavier, prise de mélodie gardée après Pad Live | test: Tour5BloquantsTest#aMelodyTakeLeftForPadLiveIsKept
controle: reporté (mode boost, grande vérification de fin)

## 1. TutoKickSoundTest#theKickStepNowSaysShowsAndTicks : cause trouvée
- Cause : le test ne fixait pas le catalogue des packs (`TutoTestHooks.packs` nul). L'appli allait donc lire les deux `index.json` des packs sur GitHub pendant le test. La carte « kit » arrivait selon le réseau : hors ligne, réponse lente ou rapide, l'étape changeait (10,4 s contre 1,5 s pour les variantes).
- Rouge (f597aaa2) : réseau coupé dans le test (sélecteur de proxy qui note chaque adresse demandée et l'envoie sur un port fermé). Le test échoue : il attendait [] et a reçu les deux adresses GitHub.
- Vert (c2549fdb) : le test fixe le catalogue (`catalogue(withTheStylesPacks = false)`). Plus aucune adresse demandée, 1,5 s, 3 fois vert d'affilée (`--rerun`), et 5 fois vert sous charge.

## 2. Tour5BloquantsTest#aMelodyTakeLeftForPadLiveIsKept : reproduit, cause non prouvée
- Sous charge (3 boucles de calcul sur 4 cœurs, deux classes en parallèle, `--rerun`) : rouge 2 fois sur 12 passages, puis 0 fois sur 22 autres passages avec des traces.
- Rouge vu : « Grille des notes : 0 notes », attendu 4. Au même instant : piste choisie = la mélodie (5), partie 0, lecture arrêtée, et la mélodie tient bien ses 4 notes dans la partie 0. La piste 0 a 0 note dans cette partie : la grille montrée était sans doute celle de la piste d'avant (0), pas celle de la mélodie. Autre signe : le bouton de la piste était décalé de 8 px.
- Pas de commit « rouge: » : je n'ai pas su rendre ce rouge déterministe. Aucun « vert: » non plus, donc rien n'est corrigé à l'aveugle.
- Fait (511b956a) : si le test passe rouge, son message dit maintenant la piste choisie, les notes de chaque piste, le nombre de grilles dessinées et l'écran en texte. Le prochain rouge nommera la cause.
- Note : les premiers « 5 fois vert » étaient des résultats en cache de Gradle ; toutes les mesures ci-dessus sont faites avec `--rerun`.

## Ce qui n'est pas vert
- Tour5BloquantsTest#aMelodyTakeLeftForPadLiveIsKept : rouge possible sous forte charge (environ 1 fois sur 17), cause pas prouvée. Piste : une grille encore celle de la piste d'avant quand on la lit (affichage, pas sauvegarde : les notes sont dans la chanson). Si ça revient, proposer Fable sur ce seul sujet.
