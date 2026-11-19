# Tour 9, lot L12 « liste des parties » (zone SessionScreen.kt) : rapport

Branche : eq/tour-9-L12, partie de l'intégration ccr-80af1a1b-mn0wec (b81c56d3).
Rôle : correcteur. Aucun fichier de l'appli touché : SessionScreen.kt est inchangé.

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

tests: *Tour9PartsListScrollsTest*

chemin: Structure, tuto « Ordina le tue parti », it phone360 à 200 %, panneau replié | test: Tour9PartsListScrollsTest#italianSmallPhoneAt200InTheTutorial
chemin: Structure, Mode simple, fr phone à 100 % | test: Tour9PartsListScrollsTest#frenchPhoneAt100
chemin: Structure, Mode simple, it phone360 à 200 % | test: Tour9PartsListScrollsTest#italianSmallPhoneAt200

ne corrige pas: F101 : non reproduit dans la liste ; les glissés du siège partaient du panneau replié du tuto (voir la demande)

Demande : app/src/main/java/com/pocketstudio/app/tuto/TutoPanel.kt : sur un petit écran à grosse police, le panneau replié du tuto couvre la moitié basse de l'écran. Un glissé vertical parti du panneau pourrait faire défiler l'écran derrière lui, ou le panneau replié pourrait prendre moins de hauteur. C'est la zone du tuto : un lot à part, avec son siège de contrôle.

Registre : docs/audits/ultra/corrections.tsv n'a pas de ligne F101 du trieur. Un lot ne fait que passer sa ligne à « tenté », il n'en crée pas : registre non touché.

Écart du banc vu en passant (hors lot) : en français, le dialogue « 24 parties occupées » n'apparaît pas dans screenLines.
