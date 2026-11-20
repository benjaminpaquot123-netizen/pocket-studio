Relu : 1686dd30

Relecture neuve du lot « textes » (tour 9), diff `origin/ccr-80af1a1b-mn0wec..HEAD` hors `docs/` (4 commits rouge/vert + rapport).

## Constats

- mineur : `GuideSheets.tabSoundName` est une copie ligne à ligne de `trackSoundName` (TrackBar.kt) : même sortie anticipée, même `moved` (seuil 1e-4), mêmes boutons Synthé (gain forcé à 0,5) et Plaits (6 boutons, même modèle), même garde « synthé neuf », même seuil de 3 boutons, même `minByOrNull`, même `tr` (`com.pocketstudio.model.i18n.tr`). Comportement identique. Le doublon peut dériver un jour ; à terme `trackSoundName` devrait simplement appeler `tabSoundName` (TrackBar.kt interdit à ce lot).
- mineur : `Tour9TextesGuideSoundNameTest` ne couvre que le moteur Synthé ; la branche Plaits (filtre par modèle, garde `PlaitsPatch()`) n'est pas vérifiée côté guide. Le commentaire « un test les garde alignées » est donc vrai pour le Synthé seulement.
- mineur : le test ne tourne qu'un bouton (résonance) ; 2 ou 3 boutons tournés (la limite de la règle) ne sont pas essayés. L'égalité guide = onglet sur toute la bibliothèque prouve tout de même la phrase d'acceptation « La fiche Son du guide nomme le son que j'ai choisi, même après un bouton tourné, comme l'onglet de la piste ».
- Pas de régression : `soundName` reste inchangé et ses autres appelants (TrackBar, WhatPlays, Layers…) n'en dépendent pas ; seul `soundOf` (pistes `inst:`) change. `soundShown` / `soundNote` de TrackSheet ne réagissent qu'à « Plaits » et à l'Acid, toujours rendus pareils. Aucun risque de plantage neuf (`p.track(...)` était déjà appelé avant).
- es : « Detener la fila %d » est correct et cohérent : plus aucun « Parar » dans les catalogues es, toutes les clés « Arrêter … » utilisent « Detener ».
- Le test `Tour9TextesStopWordTest` vérifie bien « le même geste a le même mot partout » pour les clés « Arrêter … » des 7 langues sans conjugaison ; les langues qui conjuguent (ar, hi, ja, ko, pl, id, nl) sont exclues et ne sont donc pas gardées (mineur, limite assumée et écrite dans le test).

## Écart c382dad3..1686dd30 (relecture neuve)

- Tour9TextesRowStopWordTest traduit la vraie clé « Arrêter la ligne %d » (PadLiveScreen.kt:765) et exige le mot de « Arrêter » dans 7 langues ; il ne peut passer à vide ; @After remet FR. Les lignes « chemin » citent toutes un @Test de app/src/test.
- mineur : nl, pl, id, hi, ja, ko, ar écartées sans commentaire dans ce test (même limite que Tour9TextesStopWordTest) ; la ligne « tests: » cite aussi deux tests de project-model.

Verdict : 0 bloquant, 0 grave
