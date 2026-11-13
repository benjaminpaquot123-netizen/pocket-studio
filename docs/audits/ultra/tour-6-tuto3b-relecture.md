# Tour 6 — lot « tuto3b » : relecture neuve (re-signée sur le dernier commit)
Relu : a24b32c2677ff0fa56365c7dd14e94eb18861f68

Relecture par un sous-agent neuf, qui n'a écrit ni le lot ni la première relecture (`tour-6-tuto3b-relecture-1.md`, relu 600abd29, 0 bloquant, 0 grave). `git diff 600abd29..a24b32c2 -- app project-model engine` est vide : aucun code ni test n'a changé depuis ; seuls trois documents bougent (rapport et fiche de zone renommés « tuto3 » → « tuto3b », la première relecture ajoutée). Lus : le rapport `tour-6-tuto3b-rapport.md` en entier, ses lignes neuves contre les tests nommés, `sieges/tour-6/verdicts.tsv` (T4, N4), les rapports de contrôle Da-4, Ha-4, Bb-4 et celui de Da-3 sur sa branche `origin/claude/benstudio-tour6-Da-controle3`.

## Bloquant
Aucun.

## Grave
Aucun.
- Ligne `chemin: … la flèche de la ligne | test: Tour6TutoFlechesRollTest#technoBassEveryNoteIsReachedByFollowingTheLine` : `@Test` sans `@Ignore` de `app/src/test`, dans cette classe ; il joue le vrai rouleau (techno, `bass_do`, 360 dp) et exige, note après note, soit une note restante entière en vue au-dessus du panneau du tuto sans flèche, soit une flèche dont au plus deux touchers sur la ligne amènent une note entière au-dessus du panneau. Il garde bien ce que dit le chemin (et le cœur de T4 dans `verdicts.tsv` : notes en pointillé sous le panneau).
- Ligne `corrige: T4 | gravité: grave` : T4 est « réel, grave » dans `verdicts.tsv` (rouleau placé sans tenir compte du panneau, notes hors de vue ; N4, la flèche, « même racine que T4 ») ; Da-3 a bien joué `4cb88f55` (sa NOTE) et dit « もっと上 ↑ » trompeur, 12 gestes (écran 106) ; Da-4 dit « T4 : corrigé (écrans 80, 100, 104, 106) » ; `Tour6TutoFlechesTest#theLinePointsToTheNearestNoteLeft` existe (`project-model/src/test`, l. 179). Le dossier Da-3 n'est pas dans la tête (928cd71e1 pas ancêtre de HEAD) : « rejeu impossible » tient.
- Ligne `chemin:` retirée (`Tour6TutoKnobStepTest`, test de `project-model`) : conforme à la règle (chemins dans `app/src/test`) ; le test reste dans `tests:`.

## Moyen
- Neuf, dans le rapport : « sa branche n'a pas été poussée » est inexact : `origin/claude/benstudio-tour6-Da-controle3` existe (928cd71e1, « run 3 (lot tuto3, ja) »). Ce qui empêche le rejeu, c'est que ce dossier n'est pas fusionné dans l'intégration. À reformuler (« poussée mais pas fusionnée ») ; la conclusion ne change pas.
- Repris de la première relecture, toujours ouvert (code inchangé) : sur un son de synthé simple (`engine = 0`), « Coupure : encore +8 » compte des points de % alors que l'écran montre des Hz/kHz.
- Repris de la première relecture, toujours ouvert : T3 (« Doux » au ch. 3) pas réglé partout (Da-4, écrans 38/40), renvoyé à un prochain lot.

## Mineur
- Rapport, corrigé 6 : « n° 4, passé grave au contrôle Da-3 » alors que `verdicts.tsv` tient déjà T4 pour grave ; sans effet.
- Le chemin « Coupure et Résonance +15 refusés » (corrigé 1) n'a plus de ligne `chemin:` à lui ; il reste couvert par `Tour6TutoKnobStepTest` (dans `tests:`) et voisin de `Tour6TutoKnobLeftTest`.
- Repris de la première relecture : toucher proche d'un pointillé pendant le tuto ; `softSaid` / `chordOffFor` jamais effacés ; `knobsLeft` à l'écart brut ; `Tour6TutoKnobLeftTest` sur un son où la Coupure s'affiche en Hz.

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --tests …Tour6TutoGhostTapTest …TempoNoteTest …ChordModeTest …SoftOpenTest …KnobLeftTest …FlechesRollTest` : le premier passage est sorti `FROM-CACHE` (rien de rejoué) ; relancé avec `--rerun` sur a24b32c2 : BUILD SUCCESSFUL, tâche exécutée ; 11 tests, 0 échec (FlechesRoll 6/6, les cinq autres 1/1).
- rouge constaté : aac21ca5e : rejoué sur bd5699470 : `PianoRoll.kt` de bd5699470 remis seul, `Tour6TutoGhostTapTest` échoue (1/1), « a tap a hair past the ghost writes the ghost, not a parasite expected:<[Note(start=240, length=24, pitch=72…)]> but was:<[Note(start=264, length=48, pitch=72…)]> » (la note parasite). Fichier remis à HEAD ensuite, suite verte relancée (`--rerun`) sur l'arbre remis, arbre propre.

Verdict : 0 bloquant, 0 grave — fusion possible
