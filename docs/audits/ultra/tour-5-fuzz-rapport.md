# Rapport du lot « fuzz » (tour 5) : CI 43229551237 rouge sur GestureFuzzTest

## Le constat
- CI 43229551237 (commit db66a3ce) : 1 test rouge sur 8537, `GestureFuzzTest#aRandomFingerNeverCrashesTheApp`, graine 11, après le geste 132 « touch acid_sounds_save » : `AppNotIdleException` (« Compose did not get idle after 128458 attempts in 60 SECONDS »), levée par `compose.waitForIdle()` (ligne 88), relayée ligne 203. Lu dans l'artefact `unit-test-reports` de ce passage.
- Les 15 derniers gestes de la CI sont sur l'écran Clavier du Mode simple, pas sur le Mix.

## Le suspect n° 1 (mixwake, `rememberChangesEvery` / `rememberWorstEvery`) : écarté, preuves
- Sonde isolée (non commitée) : un écran qui ne contient que `rememberWorstEvery(250)` (boucle `while (true) delay`) ou `rememberChangesEvery(250)` : 20 `waitForIdle()` reviennent tout de suite, l'horloge des tests ne bouge pas. Ces boucles n'empêchent pas le repos.
- Le même blocage existait avant mixwake : `docs/audits/ultra/tour-5-composants-rapport.md` (commit 37287246 du 10 novembre, contenu dans 193d17ee) note « GestureFuzzTest (AppNotIdle après « synth_sounds_save », puis après « scene_rename ») » en suite complète. `Changes.kt` n'existe que depuis le 12 novembre (3b52258d).
- Pourquoi la CI d'avant était verte : le chemin du doigt au hasard dépend des étiquettes à l'écran et de l'état laissé par les classes jouées avant lui dans la même JVM ; les lots mixwake, testapk et oreille l'ont déplacé vers un bouton « Garder ce son ». Le commit fautif n'en est pas un : c'est un défaut ancien du banc que le hasard a croisé.

## La cause, prouvée
- Reproduit ici sur db66a3ce avec une sonde du même doigt qui appuie de préférence sur `*_sounds_save` (non commitée) : graine 18 sur 30, puis graine 27 sur 60, `AppNotIdleException` après « Garder ce son ». Pas reproductible à coup sûr : il faut une petite fenêtre au mauvais moment.
- Relevé des fenêtres au blocage : `type=1002 « Pop-Up Window »` (sans champ) en haut, puis `type=2` (le dialogue « Garder ce son », avec son champ « Nom du son »), puis la feuille, puis l'écran. `textFieldInADialog()` répondait oui.
- `GestureFuzzTest.closeTextWindows()` envoyait Retour à la **première** fenêtre au-dessus de l'écran, donc à la bulle, jamais au dialogue. Le dialogue à champ restait ouvert, et sous Robolectric un dialogue à champ ne laisse jamais Compose au repos (`Zone3DialogDiagTest`, tour 3).

## La correction
- `BenchWindows.textFieldDialog()` : la fenêtre au-dessus de l'écran qui tient un champ (même lecture que `textFieldInADialog()`, qui la réutilise sans changer de réponse). `closeTextWindows()` envoie Retour à cette fenêtre-là.
- Le test n'est ni sauté ni affaibli : mêmes graines, mêmes 300 gestes, mêmes invariants (pas de plantage, sauvegarde relue à l'identique).
- Rien de l'appli ne change, aucune capture ne change.

## Preuves
- rouge 96083cfd : `GestureFuzzTest#aTextWindowUnderAPopupIsClosedWithRetour` (un dialogue à champ, une bulle par-dessus, l'attente du doigt) échoue sur « the window with the text field is still open ».
- vert b934b4c2 : `GestureFuzzTest` (3 tests), `PlayingRecomposeTest` (4), `ClipHoldTest` (2), `Zone3DialogDiagTest` verts.
- Sonde biaisée après correctif, machine chargée (2 cœurs occupés) : 60 graines, 0 `AppNotIdleException`, 1665 fenêtres à champ fermées par Retour.

## Tests ciblés et chemins
tests: *GestureFuzzTest *PlayingRecomposeTest *ClipHoldTest *Zone3DialogDiagTest
- chemin: Doigt au hasard › fenêtre « Garder ce son » sous une bulle | test: GestureFuzzTest#aTextWindowUnderAPopupIsClosedWithRetour
- chemin: Doigt au hasard dans toute l'appli (graine 11) | test: GestureFuzzTest#aRandomFingerNeverCrashesTheApp
- chemin: Doigt au hasard dans toute l'appli (graine 20261103) | test: GestureFuzzTest#anotherRandomFingerNeverCrashesTheApp
- chemin: Mode simple › Mix en lecture (gain de mixwake gardé) | test: PlayingRecomposeTest#simpleMixComposesLittleWhilePlaying
- chemin: Mix en lecture (gain de mixwake gardé) | test: PlayingRecomposeTest#mixComposesLittleWhilePlaying

## Ce dont je ne suis pas sûr
- Quelle bulle exacte était ouverte (son titre est celui par défaut d'Android) : les poignées du champ ou un menu, sans doute. La correction ne dépend pas de laquelle.
- Le passage complet de la suite n'a pas été rejoué ici (coupé deux fois : garde-fou de 20 min sans sortie, puis arrêté pour la sonde ciblée).
