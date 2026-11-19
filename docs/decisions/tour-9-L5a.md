# Décisions du lot L5a (tour 9, contenu des cours du tuto)

- F095 : le texte du mix dit « −1 », le premier cran qui coche l'étape, au lieu de « −2 ». Raison : un fader un peu plus bas cochait déjà l'étape. Retour arrière : les `text`/`textPro` des étapes `changed:volumes` des cours.
- F092 : « sous la grille » est retiré de toutes les étapes Swing, y compris boom_bap et drill que le siège n'a pas vus (sur un pliable, le curseur est en haut à gauche). Retour arrière : les textes Swing des cours.
- F083 : jungle écoute ses parties dans l'ordre du morceau (Break, Montée, Drop) ; la liste figée des ids Pro (`pro-step-ids.txt`) suit ce nouvel ordre. Retour arrière : l'ordre des étapes `map_*` de jungle et la fixture.
- F083 : les écoutes « vs: » (comparaison de deux parties) ne sont pas touchées, le test ne lit que les écoutes « part: ».
- F062 detroit : la Fin nomme la piste « Accords » telle qu'elle s'affiche, pas « les cordes ». Retour arrière : `make_fin` de detroit.
- F091 house : la valeur dite est celle que l'étape accepte ; une valeur MPC reste dite seulement comme valeur MPC, entre parenthèses ou « sur une MPC ». Retour arrière : les textes Swing de house.
- F072 industrial : le texte Facile dit ce que fait le bouton (« recopie sur les mesures suivantes ») sans le nommer (règle d'un seul moment, TutoOneMomentTest) ; le Pro le nomme. Retour arrière : `lead_*` d'industrial_techno.
- F063 : la note de variation s'écrit La# avec « # », comme les touches du clavier Acid (`NoteNames.pitchClass`), au lieu de Si♭. Traductions : A# (langues à lettres), La# (es, it, tr), Lá# (pt-BR), لا# (ar).
- F063 arabe : les 182 noms de notes latins des cours arabes passent en solfège arabe (le clavier arabe parle en solfège). Les symboles d'accords (Am7, D7…) restent latins, comme sur l'écran des accords. La carte Tonalité garde l'écriture du français (« Fa# »). Retour arrière : les 5 fichiers `i18n/ar/tuto_*.json`.
- Demandes au chef, hors de mes fichiers (aucun contournement par les données, qui coûterait 14 langues sans régler l'écran) :
  - F061 : nom du pad dans TutoPanel et KitAndPadSheets.
  - F064 : libellé de la piste 303 dans AppSpaces.kt / AcidLook.kt.
  - F065, F079 : définitions de PlainWords.kt, pas des cours.
  - F074 : `shownLabel` dans Scenes.kt.
- Vu hors lot, non touché : la Fin de hard_techno ne cite pas ses percussions.
