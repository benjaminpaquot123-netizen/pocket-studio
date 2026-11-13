# Tour 6 — lot « tuto3b » : relecture neuve (re-signée après le rapport corrigé)
Relu : b3359ca3561120a801f8938bc3dc6d22640cd6df

Relecture par un sous-agent neuf, qui n'a écrit ni le lot ni les relectures précédentes (`tour-6-tuto3b-relecture-1.md` sur 600abd29 ; puis d942171d, 0 bloquant, 0 grave). Depuis d942171d, un seul commit (b3359ca3), de documents : `docs/audits/ultra/tour-6-tuto3b-rapport.md` seul (4 lignes changées). `git diff d942171d1 HEAD -- app project-model engine scripts` : vide. Lu : ce diff du rapport, la fiche du relecteur, la relecture d'avant.

## Bloquant
Aucun.

## Grave
Aucun.
- Rapport : les trois lignes `controle:` Ha-4, Da-4, Bb-4 (code testé e4286b40) deviennent des lignes d'historique. C'est juste : `git merge-base --is-ancestor e4286b40 HEAD` rend 1 (pas un ancêtre ; e4286b405 est « Lot tuto3: report after the second control », de l'ancienne histoire `eq/tour-6-tuto3`). La seule ligne `controle:` restante est `Da-6 | code testé: fbf14ee1 | bloqués: 0 | graves neufs: 0` : `fbf14ee1` est un ancêtre de HEAD (rend 0), `git diff fbf14ee1 HEAD -- app/src/main project-model/src/main` est vide (aucun code du tuto changé après lui), et `sieges/tour-6/controle/Da-6/rapport.md` commence par « code testé : fbf14ee1fc617ff2b9ca9400aeff2f569da10b73 ».
- Rapport : « sa branche n'a pas été poussée » devient « sa branche est poussée mais pas fusionnée » : juste, `origin/claude/benstudio-tour6-Da-controle3` existe. Le Moyen de la relecture d'avant sur cette phrase est **corrigé**.
- `scripts/bench/rapport-zone.py eq/tour-6-tuto3b docs/audits/ultra/tour-6-tuto3b-rapport.md` : sortie 0, aucun refus (filtres et chemins lus).

## Moyen
- Repris, ouvert : avec « Touche ici », le texte de la ligne du rouleau est coupé à 360 dp (`maxLines = 1`), en fr dès 100 % et dans neuf autres langues à 100 % ou 130 % ; pas bloquant, la pastille dit le geste.
- Repris, ouvert : une étape faite par l'utilisateur puis revue (« ← », liste des chapitres) arrive avec `already = true` et sa consigne est cachée (« Tu as déjà fait cette étape. » seul) : on ne peut plus relire comment on l'a faite.
- Repris, ouvert : sur le synthé simple (`engine = 0`), « Coupure : encore +N » compte des points de % alors que l'écran affiche des Hz/kHz.
- Repris, ouvert : T3 (« Doux » au ch. 3) pas réglé partout (Da-4, écrans 38/40), renvoyé à un prochain lot.

## Mineur
- Repris de la relecture d'avant, inchangés (code identique) : les tests LineGo et ChordWords lisent la sémantique et ne voient pas la coupure ; ChordWords tourne à 392 dp ; AlreadyQuiet pose `already = true` à la main ; Da-6 en texte seul ; les points du rapport et du code déjà listés (n° 4 « passé grave », chemin « Coupure et Résonance +15 refusés », `softSaid` / `chordOffFor`, `knobsLeft`, `Tour6TutoKnobLeftTest` en Hz).
- Le rouge de la paire AlreadyQuiet ne se rejoue pas avec le `TutoPanel.kt` du commit rouge e90e9baf4 sur HEAD : il ne compile pas (`Unresolved reference 'tutoChordsLeft'`, `PianoRoll.kt:1433`), car beeb92f3b, entre le rouge et le vert, ajoute `tutoChordsLeft` à `TutoPanel.kt`. Le rouge se rejoue avec le `TutoPanel.kt` de beeb92f3b, parent direct du vert (ci-dessous).

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --rerun --tests …Tour6TutoLineGoTest …Tour6TutoChordWordsTest …Tour6TutoAlreadyQuietTest` sur b3359ca3 : BUILD SUCCESSFUL ; LineGo 1/1, ChordWords 1/1, AlreadyQuiet 1/1, 0 échec (résultats horodatés de ce passage).
- `TutoPanel.kt` de e90e9baf4 remis seul : compilation refusée (voir Mineur), pas un rouge de test.
- rouge constaté : 547438f23 : rejoué sur beeb92f3b : `TutoPanel.kt` de beeb92f3b remis seul sur b3359ca3, `Tour6TutoAlreadyQuietTest` échoue (1 test, 1 échec), « its « À toi : … efface » is not asked any more ». Fichier remis à HEAD ensuite, arbre propre.
- `git diff d942171d1 HEAD -- app project-model engine scripts` vide ; `git merge-base --is-ancestor fbf14ee1 HEAD` vrai ; `git diff fbf14ee1 HEAD -- app/src/main project-model/src/main` vide.

Verdict : 0 bloquant, 0 grave — fusion possible
