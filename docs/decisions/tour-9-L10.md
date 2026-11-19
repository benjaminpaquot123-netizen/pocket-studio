# Tour 9, lot L10 : décisions

- F139 : le message trouvé à l'arrivée est mémorisé ; il ne fait plus jamais défiler, même s'il revient après un travail. Seul un message différent fait descendre. Retour arrière : revenir au drapeau `arrived` seul (6042f7e4).
- N2 : au plancher de `WholeWordsAutoSize`, on descend encore par le même pas jusqu'à 1 sp, avec pour seul critère « la hauteur tient » (mot entier ou non). Raison : un mot coupé vaut mieux qu'un texte qui sort de sa case et chevauche ses voisins. `didOverflowHeight` écarté : il refusait des tailles qui tenaient (« Schlagzeug » 5,85 → 4,85 sp). Retour arrière : 99b7e135.
- N2 : la réduction en largeur de `shrinkToFit` (`minIntrinsicWidth`) est retirée après relecture : aucun test rouge ne la prouvait (un texte est déjà coupé au bord de sa case) et elle changeait les tuiles de scène. Le test reste en garde. Retour arrière : 99b7e135 la remettait.
- F139 : les deux tests neufs vont dans `CutCurrentMessageScrollTest` ; `CutStaleMessageTest#staleMessageKeepsTheInstructionOnTop` (tour 8, decoupe2) est remis tel quel et reste vert.
- N3 : la description d'origine du défaut est introuvable dans docs/ ; pris au mot sur la phrase d'acceptation, vérifiée sur les titres de briques du Live (`live_grid`, `live_counter`, `live_row_*`, `live_stop_*`). Le rouge d'avant se prouve en remettant le `FitText.kt` de la base (le test vient après le vert de N2, commit « test: »).
- N1 : non reproduit à neuf ; garde ajoutée, cause renvoyée par une Demande (fichiers interdits au lot).
