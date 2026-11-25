# Tour 9, lot kick-fable : le kick par famille de styles

Retour de Benjamin : « le son est pur mais ça manque de volume et le kick tape pas assez » ; version B retenue (kick 49 Hz, basse 41,2 Hz sur une autre note).

- Moteur : le kick studio et le kick dur lisent la descente (PITCH_DROP, x2,7 à x6) et sa constante (DROP_TIME, 25 à 45 ms, donc 70 à 145 ms pour arriver sur la note) ; drive de 0,8 à 4,5.
- Appli : chaque style a son kick de famille (StudioDrums.KICK_FAMILIES) : note (racine ou quinte la plus proche), descente, drive, longueur, clic. Saturation propre en house, trance, dnb ; sale en disco, hard, gabber.
- Grave tenu entre les kicks et effacement du reste : la pompe existe déjà (mix.pump des recettes) ; le régler par famille touche les recettes des lots gr-techno et gr-bass : laissé à un lot recettes (décision 8).
- Fusionné (merge) : gr-techno, gr-l12, gr-bass ; guides i18n régénérés.
- rouge moteur : bb5104b4, da95bf25 ; vert moteur : 8acd7b0c ; rouge appli : 69e960ac ; vert appli : 7cddb83b.
- host-tests : ALL PASSED. Live-bench (60 s) house, tech_house, techno, trap, dnb, dubstep : 6/6 sans alerte.
- A/B (40 s avant à 7d8ecc8a, 1 s, 40 s après) : /tmp/claude-0/-home-user-pocket-studio/64e19a33-c5c6-54c3-ab1c-d411528ad75c/scratchpad/ab/kick-<style>-avant-apres.mp3

tests: *TutoKickSoundTest *Kit808Test *Tour9TutoL5aWordsTest *Tour6TutoPanelPadQuotesTest
chemin: tuto, étape du kick, son du kick sur son pad | test: TutoKickSoundTest#theKickStepNowSaysShowsAndTicks
chemin: « Kit ▾ » › Drum & bass, kick dnb reconnu | test: TutoKickSoundTest#theDrumAndBassKitIsBuiltIn
chemin: « Kit ▾ » › Trap, 808 sur son pad | test: Kit808Test#trapPutsThe808OnItsPadInOneUndoStep
