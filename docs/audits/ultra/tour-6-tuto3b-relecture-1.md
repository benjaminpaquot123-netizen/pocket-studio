# Tour 6 — lot « tuto3 » : relecture neuve
Relu : 600abd2921aaa5e5ed478d42e2233e1cc699c063

Relecture par un sous-agent neuf qui n'a pas écrit le lot : `git diff c384b6939..600abd29 -- app/src/main project-model/src/main` (`PianoRoll.kt`, `TutoController.kt`, `TutoPanel.kt`, `TutoChecks.kt`, 14 `i18n/*/tuto.json`), les six tests neufs et les deux tests de modèle changés, le rapport et les décisions du lot. Lus en entier autour : `check()`, `looks()`, `save()`, `base()`, `noteFor()` du contrôleur ; le toucher du rouleau (`Grid`, branche `found == null`) ; `TutoRollHint.next/way/brought` ; l'affichage des boutons dans `SynthScreen.kt` (`ParamSliderBody`, `synthUnit`) et `PlaitsPanel.kt`.

## Bloquant
Aucun.

## Grave
Aucun.
- `TutoChecks.knobPct` / `done` : le contrôle « au moins 15 » au pourcentage arrondi est strictement plus large que l'ancien (|écart arrondi − 100 × écart brut| < 1, donc tout écart brut ≥ 0,15 donne ≥ +15 arrondi) : rien qui passait ne peut plus être refusé, aucun test existant ne change de couleur par là. `knobsShort` suit la même règle (`left > 0` et `< 95`), donc la note est vide exactement quand l'étape est faite.
- `PianoRoll.tutoTapTarget` : ne joue que si `TutoHints.active` et des pointillés sont passés au rouleau ; les pointillés viennent de `TutoHints.notes` filtrés sur la piste montrée (`AppRoot.kt:3151`) ; un toucher sur une note existante est pris avant (branche `found`). Le toucher proche n'écrit donc qu'une note en pointillé pas encore écrite de l'étape, jamais une note étrangère. Hors tuto (`active` faux) : le code d'avant, inchangé. Pas de mauvaise note possible ; seul effet de bord en Mineur.
- `TutoRollHint.next` : `way()` dit « rien » dès qu'une note restante est entière en vue (son écart vaut 0, elle est donc aussi celle que `next` choisit) ; sinon la flèche part de la note la plus proche, et `brought` mène à la même. Cohérent ; propriété de `Tour6TutoFlechesTest` verte.
- Extinction d'« Accords » une fois par étape : `chordOffFor` est dans le même `rememberSaveable` que `chordMode`, et le rouleau vit sous `tabStates.SaveableStateProvider` (`AppRoot.kt:330`, `2777`) : l'état revient avec l'onglet, comme dans le test. `Tour6TutoFlechesChordsTest` (Accords éteint à l'ouverture) reste vert.
- `TutoTarget.startTempo` : même règle que `blank()` (`TutoChecks.kt:299`), nombre fixe ; la note ne bouge plus à mi-chemin.
- `onlySoftnessMissing` et le bloc `softSaid` dans `check()` avec la mécanique de la tête (un regard à la fois, réponse périmée jetée) : le bloc est après le contrôle `now.progress != u.progress || projet changé → return false`, donc il ne parle que sur une réponse qui compte ; s'il a coché autre chose (`found` non vide), `check()` rend faux et le regard refait trouve `found` vide et ouvre le panneau. Il exige `_ui.current == u.current` : pas d'ouverture sur une autre étape. Rouge rejoué (plus bas).
- `noteFor` des boutons : `base()` n'est lu que pour l'étape affichée ; à l'arrivée (dans `save()`, `_ui` encore sur l'étape d'avant) il rend null, donc pas de note fausse, et le regard d'arrivée la pose aussitôt (« Resonanz: noch +15 », écran 147 de Bb-4).
- `TutoHints.reset()` : `@VisibleForTesting`, appelé seulement depuis les tests (`@After`) ; remet chaque champ de l'objet à sa valeur d'origine.
- Phrase neuve `%1$s : encore +%2$d` : présente dans les 14 `tuto.json`, JSON valides, `%1$s` et `%2$d` gardés partout ; `I18nCatalogTest` et les autres tests `model.i18n` verts. Le nom du bouton vient de `SYNTH_LABELS` (déjà traduits).
- Ni sauvegarde (aucun changement de `save()`, du store ni du projet écrit hors du geste de l'utilisateur), ni son touchés.

## Moyen
- Moyen laissé ouvert : sur un son de synthé simple (`engine = 0`), la « Coupure » s'affiche en Hz ou kHz (`synthUnit`), pas en % ; « Coupure : encore +8 » donne alors des points qu'on ne lit nulle part à l'écran (pareil pour « Glissé entre les notes », en secondes, `jump_up`). Le cas du contrôle (techno Pro, Mélodie Plaits « Analogique + filtre », affichée en nombre) est juste. Pas bloquant (monter encore coche l'étape) ; parade : dire « monte encore un peu » pour un bouton dont l'écran n'est pas en %, ou montrer le % sous le Hz.
- Moyen laissé ouvert : T3 (« Doux » au ch. 3) pas réglé partout d'après le contrôle Da-4 (écrans 38/40) : le panneau rouvert ne suffit pas toujours ; le lot le dit lui-même et le renvoie à un prochain lot.

## Mineur
- Pendant le tuto, on ne peut plus poser à la main une note libre à un pas ou une rangée d'une note en pointillé pas encore écrite : le toucher écrit le pointillé. Choix noté dans les décisions ; c'est la note que le tuto attend, un retour arrière au besoin.
- `softSaid` et `chordOffFor` ne s'effacent jamais : une étape revisitée plus tard (chapitre rejoué) ne rouvre plus le panneau et ne rééteint plus « Accords ». Sans gêne connue.
- `knobsLeft` (bouton cerclé) compte encore l'écart brut (`KNOB_TURNED` 0,05) : sans effet sur la coche.
- Le test des boutons (`Tour6TutoKnobLeftTest`) prend un son `engine = 0`, où la Coupure s'affiche en Hz : il prouve le compte, pas ce que l'écran montre (voir Moyen).

## Captures
Le lot ne change ni n'ajoute aucune capture Roborazzi ; seul un texte neuf apparaît (la note de l'étape). Vu dans les écrans décrits du contrôle Bb-4 (146-149) : « Cutoff: noch +1 · Resonanz: noch +15 » sous la consigne, puis « Resonanz: noch +15 », puis « Bravo » ; les nombres lus à l'écran (74 → 76, 60 → 75) collent au compte.

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --tests …Tour6TutoGhostTapTest …Tour6TutoTempoNoteTest …Tour6TutoChordModeTest …Tour6TutoSoftOpenTest …Tour6TutoKnobLeftTest …Tour6TutoFlechesRollTest` sur `600abd29` : BUILD SUCCESSFUL ; 11 tests, 0 échec (FlechesRoll 6/6, les cinq autres 1/1).
- `scripts/bench/gradle-file.sh :project-model:test --no-configuration-cache --tests 'com.pocketstudio.model.tuto.*'` : BUILD SUCCESSFUL ; 27 classes, 108 tests, 0 échec (dont `Tour6TutoKnobStepTest`, `Tour6TutoFlechesTest`).
- Voisins : `Tour6TutoFlechesChordsTest` 2/2, `Tour6TutoPanelAlreadyTest` 3/3, `Tour6TutoOrderRaceTest` 1/1, `TutoLot5ControllerTest` 5/5 ; `com.pocketstudio.model.i18n.*` (10 classes, dont `I18nCatalogTest` 13/13) : 0 échec.
- rouge constaté : aac21ca5e : rejoué sur bd5699470 : `PianoRoll.kt` de bd5699470 remis seul, `Tour6TutoGhostTapTest#aTapAHairPastADottedNoteWritesThatNoteNotAParasite` échoue, « expected [Note(start=240, length=24, pitch=72…)] but was [Note(start=264, length=48, pitch=72…)] » (la note parasite).
- rouge constaté : 3f8ebaf79 : rejoué sur 5f4855516 : `TutoController.kt` de 5f4855516 remis seul (même run), `Tour6TutoSoftOpenTest#cellsAllHitButNotSoftOpenTheFoldedPanel` échoue, « only the softness is missing: the panel opens on its words ». Les deux fichiers remis à HEAD ensuite, arbre propre.

Verdict : 0 bloquant, 0 grave — fusion possible
