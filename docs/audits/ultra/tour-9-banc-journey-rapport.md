# Tour 9, lot banc-journey « TutoJourneyTest instable » : rapport

Branche : eq/tour-9-banc-journey. Fichier touché : app/src/test/java/com/pocketstudio/app/tuto/TutoJourneyTest.kt (rien dans app/src/main).

Mesure : 5 passages de la classe entière, seule sur la machine, tous verts (74/74) ; sous charge (6 brûleurs sur 2 cœurs, puis jump_up + techno sur 1 cœur avec 8 brûleurs) : verts aussi. L'instabilité ne venait pas toute seule.

Cause (prouvée) : le tuto calcule le bouton de l'étape (« Ajouter … » ou « Je le fais ») hors du fil principal (TutoController.check, sur Dispatchers.Default). En chargeant ces fils, le bouton arrive en retard : le test lisait l'écran trop tôt (panneau replié, « Ajouter la caisse claire » pas encore là), puis touchait « Je le fais » qui n'existait pas. Panne reproduite à chaque fois : jump_up, hard_techno, retro_techno, Facile et Pro, étape snare_do (« tuto_do » introuvable). Le défaut est dans le test, pas dans l'appli : l'appli finit toujours par afficher le bon bouton.

Ce qui change :
- rouge (4d1c5bb6) : un cas de plus, « jump_up Facile sous charge », qui garde Dispatchers.Default occupé pendant tout le parcours (arrêté dans @After). Rouge sur l'ancien test (tuto_do introuvable).
- vert (078dd61a) : pour une étape « lane: », le test attend que l'écran montre le bouton que le projet demande (même règle que l'appli, TutoPlay.button), panneau déplié, avant de toucher.

Preuve : après la correction, 5 passages d'affilée de toute la classe avec --rerun, 75/75 verts à chaque fois (dont le cas sous charge, environ 150 s).

À surveiller (relecture, mineur) : d'autres `click("tuto_do")` de perform() (velocity:hat, changed:synth, shared, song:order, triplet:) n'attendent pas encore leur bouton ; le cas sous charge ne parcourt que jump_up. Le cas sous charge ajoute environ 2 min 30 à la CI.

tests: *TutoJourneyTest

chemin: tuto jump_up Facile, étape snare_do, machine chargée, bouton « Je le fais » | test: TutoJourneyTest#theWholeCourse
