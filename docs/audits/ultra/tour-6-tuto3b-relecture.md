# Tour 6 — lot « tuto3b » : relecture neuve (trois paires après e6d70b69)
Relu : d942171d1efc3207c9e283538ce525d42e3c5ca3

Relecture par un sous-agent neuf, qui n'a écrit ni le lot ni les relectures précédentes (`tour-6-tuto3b-relecture-1.md` sur 600abd29, puis la re-signature sur a24b32c2, 0 grave chacune). Lu : `git diff e6d70b69 fbf14ee1 -- app/src/main project-model/src/main` (`PianoRoll.kt` : ligne du rouleau et « Touche ici » ; `TutoPanel.kt` : `tutoChordsLeft`, « Déjà fait » sans « À toi » ; 14 `i18n/*/tuto.json`, deux phrases neuves), les trois tests neufs, `alreadyWords` et le calcul de `already` dans `TutoController` (l. 786, 866), le rapport (ligne `controle: Da-6`) et `sieges/tour-6/controle/Da-6/rapport.md`. La tête (d942171d) est une fusion ; `git diff fbf14ee1 HEAD -- app/src/main project-model/src/main` est vide : les fusions (0ff032ee9, puis d942171d) n'ont changé aucun code du tuto (banc, portail et documents seulement).

## Bloquant
Aucun.

## Grave
Aucun.
- « Touche ici » (`PianoRoll.kt:1442`) : ne s'affiche que hors copie, avec une flèche (`way != null`) et des notes restantes ; le toucher reste celui de toute la ligne (`TutoHints.focus++`), inchangé. La coupure du texte principal (Moyen) ne bloque pas : le geste que dit la pastille amène à la note en 1 toucher (Da-6, écrans 87, 109).
- `tutoChordsLeft` (`TutoPanel.kt:285`) : seulement quand « Accords » est allumé ; `left <= 0` retombe sur « Toutes les notes en pointillé sont posées » ; le compte est celui des départs distincts, comme avant. Un seul `%d`, sans forme de pluriel (« Accords … : N » se lit pour 1 comme pour 9) : rien de cassé, dans aucune des 14 langues.
- Traductions (14 `tuto.json`) : JSON valides, `%d` gardé partout, sens juste (« toucher » = taper ; « pointillé » = gestrichelt, gestippeld, tratteggiato, punteado, pontilhado, kropkowany, noktalı, bertitik, 点線, 虚线, 점선, बिंदुदार, منقّط).
- « Déjà fait » sans « À toi » (`TutoPanel.kt:551-566`) : n'agit que si `alreadyWords(ui) != null` (étape montrée cochée et `already`). Le bandeau « Déjà fait ! » et sa phrase restent, ainsi que « Pourquoi ? », la carte, le kit et la barre d'actions (« Écouter l'exemple », « Suivant ») : rien ne manque pour avancer, l'étape étant cochée. Ni sauvegarde ni son touchés. Le cas de l'étape faite par l'utilisateur et revue est en Moyen.
- Aucun test existant ne change de couleur : les six classes demandées repassent vertes (13 tests), dont `Tour6TutoPanelAlreadyTest` 3/3, voisin direct du changement.

## Moyen
- Neuf : avec « Touche ici », le texte principal de la ligne est coupé (`maxLines = 1`, ellipse ; `PianoRoll.kt:1432-1438`). Mesuré à 360 dp (rendu natif, largeur voulue contre largeur donnée) : en français, « Encore 1 note en pointillé, plus loin → » coupé dès 100 % (la flèche tombe ; sans la pastille il tenait) et à 130 % on lit « Encore 1 note en pointillé, p… » ; « Accords en pointillé à toucher : 2 → » coupé à 130 % (le nombre et la flèche tombent). Coupé aussi en de, es, it, pt-BR, pl, id, nl, ja, tr à 100 % ou 130 %. Pas bloquant (la pastille dit le geste) ; parade : deux lignes permises quand la pastille est là, ou la pastille sous le texte.
- Neuf : une étape faite par l'utilisateur lui-même puis revue (« ← », liste des chapitres) arrive avec `already = true` (`TutoController.kt:786`) : sa consigne n'est plus lisible (« Tu as déjà fait cette étape. » seul), et une étape non manuelle revue dit « Ton morceau avait déjà ce que demande cette étape : rien à toucher » sans ses mots. Qui revient en arrière pour relire comment il a fait ne le peut plus. Pas bloquant (étape cochée, rien ne manque pour avancer) ; parade : ne cacher les mots que pour l'étape trouvée faite à l'arrivée (l. 866), ou les garder repliés sous « Revoir la consigne ».
- Repris, toujours ouvert : rapport l. 59, « sa branche n'a pas été poussée » est inexact (`origin/claude/benstudio-tour6-Da-controle3` existe) ; dire « poussée mais pas fusionnée ».
- Repris, toujours ouvert (code inchangé) : sur un son de synthé simple (`engine = 0`), « Coupure : encore +8 » compte des points de % alors que l'écran montre des Hz/kHz.
- Repris, toujours ouvert : T3 (« Doux » au ch. 3) pas réglé partout (Da-4, écrans 38/40), renvoyé à un prochain lot (rapport l. 47).

## Mineur
- `Tour6TutoLineGoTest` et `Tour6TutoChordWordsTest` lisent le texte par la sémantique (texte entier même coupé) : ils ne voient pas la coupure du Moyen ; `Tour6TutoChordWordsTest` tourne à 392 dp, pas à 360 dp.
- `Tour6TutoAlreadyQuietTest` ne couvre que l'étape « gap » posée `already = true` à la main, pas le chemin du contrôleur (arrivée sur une étape cochée).
- Le contrôle Da-6 est en texte seul : la coupure de la ligne ne pouvait pas s'y voir.
- Repris : rapport, « n° 4, passé grave au contrôle Da-3 » alors que `verdicts.tsv` le tient déjà pour grave ; le chemin « Coupure et Résonance +15 refusés » sans ligne `chemin:` à lui ; toucher proche d'un pointillé pendant le tuto ; `softSaid` / `chordOffFor` jamais effacés ; `knobsLeft` à l'écart brut ; `Tour6TutoKnobLeftTest` sur un son où la Coupure s'affiche en Hz.

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --rerun --tests …Tour6TutoLineGoTest …ChordWordsTest …AlreadyQuietTest …PanelAlreadyTest …FlechesRollTest …ChordModeTest` sur d942171d : BUILD SUCCESSFUL ; 13 tests, 0 échec (LineGo 1/1, ChordWords 1/1, AlreadyQuiet 1/1, PanelAlready 3/3, FlechesRoll 6/6, ChordMode 1/1).
- rouge constaté : 5890c1346 : rejoué sur e90e9baf4 : `PianoRoll.kt` de e90e9baf4 remis seul, `Tour6TutoLineGoTest` échoue, « the line says a touch takes you there (« Touche ici ») ».
- rouge constaté : beeb92f3b : rejoué sur 5890c1346 : même passage, `PianoRoll.kt` de e90e9baf4 (sans `tutoChordsLeft`, comme celui de 5890c1346), `Tour6TutoChordWordsTest` échoue, « three chords, said as chords: « Encore 3 notes en pointillé à toucher » ».
- rouge constaté : 547438f23 : rejoué sur beeb92f3b : `TutoPanel.kt` de beeb92f3b remis seul, même passage, `Tour6TutoAlreadyQuietTest` échoue, « its « À toi : … efface » is not asked any more ». Les deux fichiers remis à HEAD ensuite, arbre propre.
- Mesure de la ligne (test temporaire, retiré, rien commité) : `PianoRoll` à 360 dp, rendu natif, 14 langues × 100 % / 130 %, une note puis deux accords hors de vue : chiffres dans le Moyen ; la pastille « Touche ici » n'est jamais coupée.
- `git diff fbf14ee1 HEAD -- app/src/main project-model/src/main` : vide.

Verdict : 0 bloquant, 0 grave — fusion possible
