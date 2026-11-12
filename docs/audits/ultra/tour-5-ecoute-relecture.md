Relecture du lot « écoute » (T1b, écoute au micro), après corrections
Relu : e2810bee34d05a01892617d2958a470e79815569

Portée : `git diff 9fcc7ab1 HEAD` sur `app/src/benTest`, `app/src/test/java/com/pocketstudio/bentest`, `scripts/bentest`, décisions et rapport. Ordre des commits vérifié : « squelette » (signatures seules), puis chaque « rouge: » (test seul) avant son « vert: » (code seul, sauf un test d'aide ajusté dans le dernier vert).

## Les 7 moyens de l'ancienne relecture

| n° | constat | état |
|----|---------|------|
| 1 | `debut_s` proche de 0 | corrigé : `HautParleurReel.pasJoues()` lit `NativeEngine.absoluteStep()` (même lecture que `AppRoot.kt:2244`, -1 si moteur absent) ; rouge `HautParleurReelTest` (32 contre -1) puis vert ; le test passe par le vrai `HautParleurReel`. |
| 2 | bruit de la pièce sommé sur toutes les trames du balayage | corrigé : `Bandes.fenetre` (meilleure suite de ⌈durée⌉+2 trames), bruit × trames de la fenêtre, rsb sur le passage propre de la bande ; rouge `aBandIsJudgedAgainstTheNoiseOfItsOwnFrames` puis vert ; `aBandUnderTheRoomsNoiseIsNotMadeUp` reste vert (bruit pur non dit entendu). Relu : seuil `AU_DESSUS - 1` appliqué à `sig` équivaut à l'ancien `micro >= 4 × bruit`, pas de régression. |
| 3 | faux « trou » sur des rythmes | corrigé : 8 blocs (160 ms), rouge `theSilencesOfARhythmAreNotHoles` (125 ms de silence à chaque temps) puis vert ; les deux autres tests de trou passent à 250 et 300 ms. Voir MINEUR 3 pour la limite. |
| 4 | début du balayage manqué | corrigé : `Micro.ecouter(ms, avant, …)` démarre l'enregistrement puis appelle `avant` (= `hp.jouer`) ; tampon 2 s (assez pour l'attente du moteur, 3 s max dans `jouer` : seul un moteur très lent déborderait) ; queue 1,5 s ; test `theMicrophoneListensBeforeTheSweepStarts` (rouge avant). |
| 5 | pas de test croisé Kotlin et Python | corrigé : `MemeAnalyseTest` et `test_the_same_numbers_as_the_phone` portent le même signal et les mêmes valeurs d'or (5 bandes à 1e-4, 8 octaves à 1e-3, trous inclus) ; verts des deux côtés au rejeu. |
| 6 | permission : câblage non prouvé, état perdu à la rotation | corrigé en partie : l'état est dans `EcouteMicro.reponseMicro` (une seule fois, testé), plus de `remember`. Le câblage de `BenTestActivity.kt:62-66` reste sans test (voir MINEUR 1). |
| 7 | « 2 secondes » au lieu de 4 extraits | corrigé : phrase « au plus 4 extraits », assertion ajoutée dans `BoutonMicroTest` (rouge avant). |

Les mineurs : écoute bornée (durée + 5 s), compteur `empty` remis à zéro, `runCatching` autour de `guet.finir()`, repli sur `MIC`, courbe vide dite côté Linux (testé), volume gardé sur disque : tous présents dans le code.

## BLOQUANT

Aucun.

## GRAVE

Aucun. Le GRAVE de la relecture précédente (`debut_s` toujours 0, `pasJoues` non câblé dans `HautParleurReel`) est **corrigé** par `79cd9f70` (`MicroReel.kt:109`), prouvé par `4f41bc29` ; le KDoc orphelin de `Spectre.kt` est aussi remis en place.

## MOYEN

Aucun nouveau.

## MINEUR

1. `BenTestActivity.kt:62-66` : le câblage lanceur → `reponseMicro()` → `Robot.start` n'est toujours exercé par aucun test (l'état seul l'est) ; un lanceur partagé avec un autre bouton de permission pendant l'attente pourrait aussi démarrer la mise en route sur la mauvaise réponse (peu probable : une seule demande à la fois).
2. (corrigé) KDoc orphelin de `Spectre.kt` déplacé sur `mesurer`.
3. `Guet.kt:123` : à 160 ms, un vrai décrochage plus court n'est plus attrapé au micro (dit dans les décisions) ; à l'inverse un trap en demi-temps (double croche > 160 ms) ou un break d'un temps peut encore sonner « trou » (orange + extrait). Test à 125 ms seulement.
4. `MicroReel.kt:111,118` : si l'appli meurt en plein son, le volume reste à 60 % jusqu'au prochain passage (aucune remise au lancement de l'appli) ; un fichier `volume.txt` périmé remettrait une valeur ancienne si Benjamin a changé son volume entre-temps.
5. `MicroReel.kt:37` : si la source choisie est déjà `MIC` et refuse, `ouvrir(MIC)` est retenté pour rien (inoffensif).
6. `Bandes.fenetre` prend le MAX des fenêtres glissantes : pour une bande de pur bruit à peu de bacs (50 Hz) l'écart est de l'ordre de +50 % sur l'énergie, très loin du seuil ×4 : sans effet, noté pour mémoire.

## Vérifié sans problème

- Calcul de `Courbe.mesurer` (fenêtre) : `k = ⌈trames balayées⌉ + 2` couvre la durée de la bande plus les bords ; `fenetre` retombe sur la somme entière quand il y a moins de trames que `k` ; `compte = min(k, trames)` ; la mémoire des trames gardées (~2 MB pour 13 s) ne contient que des chiffres. Le moteur est lancé depuis le fil du robot, jamais dans le callback audio.
- Ordre micro puis balayage : `rec` libéré dans `finally`, `until` calculé avant `startRecording`, marge 5 s ; `hp.jouer` appelle `arreter()` d'abord.
- Python : `apply_curve` rend `None`, `octaves` rend `None` pour une octave dont un tiers est `None`, `compare` et `light` savent le traiter (testé : `test_a_band_the_phone_did_not_hear_is_not_a_missing_bass`) ; courbe entièrement vide : message clair, pas de trace d'erreur.
- Delta `2646da66..HEAD` relu : 3 fichiers de code/test, rien d'autre ; `absoluteStep` est le compteur déjà lu par l'appli (hors callback audio).
- Rien dans la vraie appli : `git diff origin/ccr-80af1a1b-mn0wec...HEAD -- app/src/main app/build.gradle.kts` vide.

## Rejeu

- `python3 -I scripts/bentest/test_empreinte.py` : 12 tests, OK.
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests 'com.pocketstudio.bentest.*'` : BUILD SUCCESSFUL (4 min 47) sur 2646da66 ; le delta est rejoué par le coordinateur (94 tests, 0 échec), non relancé ici. Aucun fichier de code ou de test modifié, rien commité.

Verdict : 0 bloquant, 0 grave
