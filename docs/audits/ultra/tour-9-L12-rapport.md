# Tour 9, lot L12 « liste des parties » (zone SessionScreen.kt) : rapport

Branche : eq/tour-9-L12, partie de l'intégration ccr-80af1a1b-mn0wec (b81c56d3).
Rôle : correcteur. SessionScreen.kt est inchangé (F101) ; F111 touche DoneBar.kt et une ligne d'AppRoot.kt (accordés par le chef).

F101 (siège t9-53, Structure) : « La liste des parties défile d'où que parte mon doigt. »
Résultat : non reproduit dans la liste. Aucune correction, un test qui garde la phrase.

Ce que montre le siège t9-53 (it, phone360, police 200 %, Mode simple, tuto « Ordina le tue parti ») :
- les glissés partis de la liste elle-même ont défilé (gestes 40, 42, 43, 49) ;
- les glissés partis de (270, 520) n'ont rien fait (gestes 45, 47, 48). À ce réglage, la liste ne se voit que de y 204 à y 510 environ : le panneau replié du tuto commence juste dessous (son titre est à y 631). Ces glissés partaient donc du panneau du tuto, pas de la liste ;
- les gestes 47 et 48 ont échoué lecture arrêtée : la lecture n'y est pour rien.

Preuves :
- au même endroit, un glissé tous les 15 px de y 220 à y 490 fait défiler la liste à chaque fois ; de y 505 à y 595 (sur le panneau), rien ne bouge ;
- le test refait l'endroit du siège et deux autres réglages (fr phone 100 %, it phone360 200 % hors tuto). Il glisse depuis le milieu d'une ligne, depuis le bouton d'une ligne, puis tous les 50 px au milieu de la liste ;
- contre-épreuve : en bloquant les glissés sur les lignes (mutation de SessionScreen.kt, retirée ensuite), les trois tests rougissent. Exemple : « a swipe from the middle of part_play_5 (162,311 → 611) did not scroll the parts (offset 2778.0 → 2778.0 of 3607.0) ».

tests: *Tour9PartsListScrollsTest* *Tour9DoneBarGoesTest* *DoneBarNextTouchTest*

chemin: Structure, tuto « Ordina le tue parti », it phone360 à 200 %, panneau replié | test: Tour9PartsListScrollsTest#italianSmallPhoneAt200InTheTutorial
chemin: Structure, Mode simple, fr phone à 100 % | test: Tour9PartsListScrollsTest#frenchPhoneAt100
chemin: Structure, Mode simple, it phone360 à 200 % | test: Tour9PartsListScrollsTest#italianSmallPhoneAt200
chemin: Structure, message « Fait : nouvelle longueur », fr phone 100 %, touche suivante | test: Tour9DoneBarGoesTest#frenchTheNextTouchLetsTheMessageGo
chemin: Structure, message « Gotowe: nowa długość », pl phone 100 %, glissé suivant | test: Tour9DoneBarGoesTest#polishTheNextSwipeLetsTheMessageGo
chemin: Structure, message « Fait : nouvelle longueur », fr phone 100 %, sans toucher | test: Tour9DoneBarGoesTest#withoutATouchItGoesAfterAFewSeconds
chemin: Structure, message « Fait : nouvelle longueur », fr phone 100 %, « Annuler » | test: Tour9DoneBarGoesTest#itsAnnulerStillUndoes

ne corrige pas: F101 : non reproduit dans la liste ; les glissés du siège partaient du panneau replié du tuto (voir la note pour les restes)

Note pour les restes (le chef garde TutoPanel.kt pour d'autres lots) : app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt : sur un petit écran à grosse police, le panneau replié du tuto couvre la moitié basse de l'écran. Un glissé vertical parti du panneau pourrait faire défiler l'écran derrière lui, ou le panneau replié pourrait prendre moins de hauteur. C'est la zone du tuto : un lot à part, avec son siège de contrôle.

F111 (sièges t9-24, t9-49, t9-55, t9-59, t9-45, Structure) : « Le message « nouvelle longueur » disparaît après quelques secondes ou au geste suivant. »
Résultat : tenté (pas encore vu par un testeur neuf).

Cause : « Fait : … · Annuler » ne partait qu'au bout de 6 s, sans toucher. Les sièges font un geste par seconde environ : le message restait sur quatre ou cinq gestes, jusqu'au chapitre suivant du tuto (t9-24, gestes 47 à 54).
Correction : la touche ou le glissé suivant sur la page laisse partir le message, au lever du doigt. « Annuler » garde son clic (il passe avant). Un message dit par ce geste-là reste. Les 6 s restent pour qui ne touche rien. Le seul DoneBar de l'appli est celui de la page (AppRoot), donc tous les messages « Fait : … » en profitent.
Preuves :
- rouge d'abord (commit « rouge: ») : en fr, une touche sur l'onglet laisse le message ; en pl, un glissé le laisse ;
- vert ensuite, et le retrait de la ligne d'AppRoot.kt remet les deux en rouge ; le retrait du test « même message » fait rougir DoneBarNextTouchTest#theMessageThisTouchSaysStays ;
- gardes vertes avant et après : le message part seul après 7 s, « Annuler » rend la longueur d'avant.
Limite : une touche dans une autre fenêtre (une feuille, un dialogue) ne compte pas. Le message reste alors jusqu'à la touche suivante sur la page, ou 6 s.
Vu en passant : en polonais, le message (une ligne au-dessus du guide) couvre le « ⋮ » de la deuxième partie ; un appui sur ce « ⋮ » tombe sur « Anuluj ». Le message part maintenant au geste suivant, ce qui réduit le risque, sans le supprimer.

corrige: F111 | gravité: mineur

Registre : docs/audits/ultra/corrections.tsv n'a pas de ligne F101 ni F111 du trieur. Un lot ne fait que passer sa ligne à « tenté », il n'en crée pas : registre non touché.

Écart du banc vu en passant (hors lot) : en français, le dialogue « 24 parties occupées » n'apparaît pas dans screenLines.
