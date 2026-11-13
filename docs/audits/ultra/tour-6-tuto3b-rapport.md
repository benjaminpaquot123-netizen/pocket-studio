# Tour 6 — lot « tuto3 » : rapport

Contrôles joués sur `b4ac25f21` (Testeurs 1 fr Facile, 2 ja Facile, 3 de Pro), résumés par le chef. Zone tuto.

## Corrigés
1. **GRAVE, tuto Pro ch. 7 (« lead_knobs »), Coupure / Résonance +15 refusés, +20 exigés.** Le contrôle comparait la valeur brute (0..1) avec `v ≥ avant + 0,15` ; l'écran montre un pourcentage arrondi : un +15 lu pouvait faire 0,142 brut. Le contrôle compte maintenant le pourcentage affiché (`TutoChecks.knobPct`), +15 au moins ou 95 et plus. Paire `0d5bdd0b` → `508692db`.
2. **GRAVE, tuto Facile ch. 7 (mélodie), dernière note en pointillé en liseré au bord droit, la toucher pose une note parasite.** Pendant le tuto, un toucher qui manque de peu une note en pointillé (un pas, une rangée) écrit cette note (`tutoTapTarget`) au lieu d'une note neuve à côté. Hors tuto, rien ne change. Rouge observé : la note parasite `Note(264, 48, 72)`. Paire `7957cfea` → `aac21ca5`.
3. **N° 8, ordre des tests (`Tour6TutoPanelAlreadyTest` après `Tour6TutoFlechesRollTest`)** : pas une fuite d'état mais une course dans `TutoController.check()`. Sorti seul en lot « tutoordre » (`eq/tour-6-tutoordre`, `0de98372`). Ici reste seulement `TutoHints.reset()` pour le nettoyage des tests du rouleau (`bd569947`, puis `d181f71e` qui rend le fichier du test au lot tutoordre).
4. **Moyen 7a, `TutoController.kt:438-442`, « démarre à %d BPM » faux à mi-chemin.** La note nomme le tempo de départ d'un morceau neuf du style (`TutoTarget.startTempo`, même règle que `blank()`), plus le tempo du moment (« 110 » lu entre 100 et 122). Paire `69d0d681` → `a7471381`.
5. **Moyen 7c, `PianoRoll.kt:220-221`, « Accords » réteint en revenant sur la piste.** L'extinction se fait une fois par étape (mémorisée avec l'état du rouleau). Paire `a86cf09d` → `05824c0f`, avec `07fe077c` entre les deux (voir décisions : « Encore 9 notes »).

6. **GRAVE (n° 4, passé grave au contrôle Da-3 sur 4cb88f55), flèches du rouleau** : « もっと上 ↑ » alors que les notes restantes étaient à droite ou à gauche (12 gestes). La ligne parle de la note restante la PLUS PROCHE (le plus court défilement qui la montre entière ; le long du temps seul tant qu'aucune rangée n'est en vue) et pointe de son côté ; un toucher l'amène. La propriété de `Tour6TutoFlechesTest` le vérifie sur tous les styles, Facile et Pro, à chaque note ; la condition ancienne « en travers seulement si aucune note dans le temps en vue » est retirée (elle imposait ce défaut). Au plus deux gestes (propriété existante). Paire `cbd5890c` → `b215fa95`.
7. **Moyen (contrôle Da-3, ch. 3, techno « clap_do »)** : cases toutes posées mais la dernière pas « Doux », consigne repliée, rien ne disait ce qui manque. Le panneau replié se rouvre une fois sur ses mots quand seule la douceur manque (`TutoChecks.onlySoftnessMissing`). Paire `5f485551` → `3f8ebaf7`.

8. **T1 (contrôle Bb-3, de Pro, ch. 7)** : validé à +16 lu (attendu : le compte au % affiché, corrigé 1), mais à +7 rien ne disait ce qui manque. L'étape dit maintenant, bouton par bouton, ce qui reste (« Coupure : encore +8 »), avec le nom du bouton de l'écran du son ; une phrase neuve, traduite dans les 14 langues (`i18n/*/tuto.json`). Paire `fdda200e` → `13cfc0be`.

## T8 en allemand (Bb-3, ch. 12 étape 3/3 « Erledigt » sans un mot, écran 208)
Vrai cas probable, déjà corrigé ailleurs : la course du lot « tutoordre » (un regard sur le projet commencé sur l'étape d'avant coche l'étape après l'arrivée, sans « Bereits erledigt »). 4cb88f55 ne l'avait pas ; ce lot contient maintenant la tête avec « tutoordre ». À revoir au prochain contrôle.

## T8 en français (Ha-3, écran 205)
Attente du testeur, pas un défaut de « Déjà fait » : l'étape « make_montee » demande « crée la Montée à partir du Groove : sans kick ni basse » ; la copie du Groove contient le kick, les coups barrés sont ceux à enlever. La phrase « puis tout se tait juste avant le Drop » décrit la fin de la Montée et a pu laisser croire qu'il n'y avait rien à faire : à reformuler dans le lot des textes du tuto.

## Laissés de côté (demandes et listes)
- **GRAVE 3, tuto Pro ch. 13, départ Reverb du Clap introuvable dans le Mix** — Demande : `app/…/ui/AppRoot.kt` (`mixerStrips`) et `app/…/mixer/MixerScreen.kt` : la console Pro n'a qu'une tranche « Batterie » (canal 0), aucune tranche par pad ; le départ réverbe du pad Clap n'existe que dans les Effets du pad, ouverts depuis la grille de batterie (`openPadEffects`). Deux voies : (A) le lot Mix ajoute les tranches des pads (la consigne marche telle quelle) ; (B) le lot tuto mène aux Effets du pad depuis la grille (`spot`, `goFor`, texte de `techno.json` « reverb », et l'étiquette réelle est « Réverbe commune », pas « Envoi réverbe »). Je recommande (B) pour le tuto, (A) pour l'appli ; à trancher par le chef.
- **N° 5, « 100 Tempo » à l'écran 4** : c'est la fiche Tempo (`app/…/ui/TempoSheet.kt`, hors zone) qui montre le tempo actuel, 100 pour un morceau neuf de House. La note du tuto le dit maintenant de façon stable (corrigé 4 ci-dessus). Demande éventuelle : la fiche Tempo nomme la cible du tuto.
- **N° 6, mots** : chaque phrase source française est la clé de ses 14 traductions, donc toute correction du texte source réécrit 14 fichiers. Relevé : « tambourin » dans `house.json` (le pad est « Rim », « Bord de caisse » en mots simples ; et « Tambourin » est un autre pad), « Groove » non expliqué, « relances de fin de phrase », « Break : la batterie se tait » avec les charleys, japonais (`手拍子たち`, `ベロシティ`, `3連符`, titre `→ スタジオのバスドラム` au ch. 12, dans `ja/tuto_autres.json` et `ja/tuto_hiphop.json`). « Break 12 Takte » : cohérent avec le français (Break 12 + Montée 4 = 16) ; s'il est refusé à la saisie, c'est la longueur des parties (hors zone). À faire en un lot de textes du tuto, langue par langue.
- **Moyen 7b, `PianoRoll.kt:720`, demande de mise au point en attente sans limite** : la consommer au changement d'étape risque de perdre le « Je le fais » qui change d'étape en même temps ; à faire avec un rejeu.
- Neufs du contrôle Bb-3, moyens, pas pris (hors du tuto ou à trier) : ☆ introuvable sur les sons de synthé (écran 253) ; noms des pistes hors écran dans le Mix au ch. 14 ; rejouer le ch. 1 après la fin montre « Bravo » sans étapes.
- Rejeux : aucun siège de contrôle n'est encore fusionné (branches des testeurs) ; rejeux à ajouter quand leurs dossiers seront dans l'intégration.

## Histoire refaite (eq/tour-6-tuto3b)
Branche `eq/tour-6-tuto3b` : les commits de `eq/tour-6-tuto3` rejoués sur la tête `c384b6939`, arbre final identique à celui de `eq/tour-6-tuto3` fusionnée avec cette tête (`git diff` vide) ; seule la paire du toucher reçoit un sujet commun (« a tap a hair off a tutorial ghost writes the ghost, not a parasite ») pour que le juge l'apparie. Les shas cités ci-dessus sont ceux de cette branche. Les sièges ont joué `e4286b40` (même code d'appli du lot ; la tête a apporté depuis tutoordre, tutoordre2 et tutocourse dans `TutoController.check()`, qui ne touchent pas les chemins des défauts du lot).

## Contrôles sur la branche (2e essai)
controle: Ha-4 | code testé: e4286b40 | bloqués: 0 | graves neufs: 0
controle: Da-4 | code testé: e4286b40 | bloqués: 0 | graves neufs: 0
controle: Bb-4 | code testé: e4286b40 | bloqués: 0 | graves neufs: 0

- Ha-4 (fr) : Bravo en 233 gestes ; T4 corrigé (écran 98) ; T8 corrigé (écran 242).
- Da-4 (ja) : Bravo en 167 gestes, 5 hésitations ; T4 corrigé (écrans 80-106).
- Bb-4 (de, Pro) : Bravo en 226 gestes, 8 hésitations ; T1 corrigé (écrans 146-149) ; T4 corrigé (écran 89).
- **T3 (« Doux » au ch. 3) : tenté, hors lot.** Ha-4 : « au ch. 3 aucun "Doux" n'est demandé, l'étape se valide dès les cases posées » (le style joué n'a pas de « Doux » à cette étape) ; Da-4 : encore là (écrans 38/40). Le panneau rouvert ne suffit pas toujours : à reprendre dans un prochain lot.
- **T8 (« Déjà fait ») : tenté, hors lot.** Bb-4 : « Schon erledigt! » existe (écrans 193-195), mais au premier passage l'étape 3/3 du ch. 12 est sautée sans message (écran 192). À reprendre.
- Le second aller-retour n'est pas consommé (décision du chef, pour ne pas retarder). Aucun commit de code d'appli après e4286b40 dans ce lot, hors fusion de la tête.

## Après le contrôle Da-5 (ja, sur 307759b3)
9. **T4, pour le débutant qui ne sait pas toucher la ligne** : tant que la ligne du rouleau pointe ailleurs, elle montre « Touche ici » à côté de ses mots (un toucher amène la note). Paire `c3b9e2d0` → `5890c134`. Le rappel est là dès qu'une flèche l'est, donc aussi au premier défilement à la main.
10. **« Encore 9 notes » sur les accords** : « Accords » allumé, la ligne compte les accords et le dit (« Accords en pointillé à toucher : 3 », la flèche autour). Paire `fbc3cfd9` → `beeb92f3`.
11. **Ch. 12, « déjà fait » sous « À toi : … efface »** : une étape déjà tenue ne montre plus ses mots « À toi » ni leurs explications. Paire `e90e9baf` → `547438f2`.
- Deux phrases neuves (« Touche ici », « Accords en pointillé à toucher : %d »), traduites dans les 14 langues ; le compte en « %d » après deux-points évite les formes du pluriel.
- Da-5 : la règle de la note la plus proche tient (ses flèches successives suivent chaque fois la note restante la plus proche) ; ses dix glissés venaient de ne pas savoir qu'un toucher amène la note (Da-4 l'a touchée : 3 gestes).

## Corrections et rejeux
corrige: T4 | gravité: grave | rejeu impossible : le siège qui a vu le défaut (Da-3, joué sur 4cb88f55) n'est pas dans l'intégration, sa branche n'a pas été poussée ; le contrôle Da-4 l'a dit corrigé (écrans 80-106) et Tour6TutoFlechesTest#theLinePointsToTheNearestNoteLeft le garde sur tous les styles
- Zone de ce lot au portail : « tuto3b » (branche `eq/tour-6-tuto3b`, histoire refaite de `eq/tour-6-tuto3`) ; première relecture neuve gardée dans `tour-6-tuto3b-relecture-1.md` (relu 600abd29, 0 bloquant, 0 grave), re-signée dans `tour-6-tuto3b-relecture.md` après le renommage.

tests: com.pocketstudio.model.tuto.Tour6TutoKnobStepTest com.pocketstudio.app.tuto.Tour6TutoGhostTapTest com.pocketstudio.app.tuto.Tour6TutoTempoNoteTest com.pocketstudio.app.tuto.Tour6TutoChordModeTest com.pocketstudio.app.tuto.Tour6TutoFlechesChordsTest com.pocketstudio.app.tuto.Tour6TutoFlechesRollTest com.pocketstudio.model.tuto.Tour6TutoFlechesTest com.pocketstudio.app.tuto.Tour6TutoSoftOpenTest com.pocketstudio.app.tuto.Tour6TutoKnobLeftTest com.pocketstudio.app.tuto.Tour6TutoLineGoTest com.pocketstudio.app.tuto.Tour6TutoChordWordsTest com.pocketstudio.app.tuto.Tour6TutoAlreadyQuietTest

chemin: Tuto Facile › mélodie › dernière note en pointillé au bord › toucher | test: Tour6TutoGhostTapTest#aTapAHairPastADottedNoteWritesThatNoteNotAParasite
chemin: Tuto Facile › House › étape tempo › à mi-chemin | test: Tour6TutoTempoNoteTest#theTempoNoteNamesTheStartNotATempoHalfWay
chemin: Tuto › piste Mélodie › « Accords » allumé › retour sur la piste | test: Tour6TutoChordModeTest#accordsTurnedOnByTheUserStaysWhenTheRollComesBack
chemin: Tuto Facile › ch. 7 › notes restantes hors de vue › la flèche de la ligne | test: Tour6TutoFlechesRollTest#technoBassEveryNoteIsReachedByFollowingTheLine
chemin: Tuto Facile › techno › ch. 3 « clap_do » › cases posées pas douces, panneau replié | test: Tour6TutoSoftOpenTest#cellsAllHitButNotSoftOpenTheFoldedPanel
chemin: Tuto Pro › techno › ch. 7 « lead_knobs » › Coupure à +7 | test: Tour6TutoKnobLeftTest#aKnobNotUpEnoughSaysHowMuchIsLeft
chemin: Tuto Facile › ch. 7 › notes restantes hors de vue › « Touche ici » sur la ligne | test: Tour6TutoLineGoTest#aLinePointingAwaySaysATouchTakesYouThere
chemin: Tuto › Accords › « Accords » allumé › ligne des accords restants | test: Tour6TutoChordWordsTest#withAccordsOnTheLineSaysChordsNotNotes
chemin: Tuto Facile › ch. 12 › étape déjà tenue › « Déjà fait » sans « À toi » | test: Tour6TutoAlreadyQuietTest#aStepDoneAlreadyDoesNotAskToDoIt
