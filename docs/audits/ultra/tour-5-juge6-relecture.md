# Relecture du lot « juge6 » (tour 5), par un relecteur neuf
Relu : 54064c9ac3180e7b487160be8ce9f08e991516a8

Cible : `git diff fx-int...HEAD` (le juge `scripts/bench/verifie-chantier.py`, ses tests, `paires-rejouees/`, `rouges-mutes/`, `verdicts.tsv`). Aucun code de l'appli touché.

## Ce qui a été refait
- `python3 -I scripts/bench/test_ultra_bench.py` : vert (« ultra bench scripts: OK »).
- Mutations du juge, une à une, chacune rattrapée par un témoin du banc : `CRASH` retiré, `added > removed` retiré, `renamed_only` retiré, « lignes ôtées = lignes ajoutées par le vert » retiré, `REPLAYS_UNTIL` remis à 92258b19, `_patch_applies` retiré, « le test n'existe pas dans le parent » retiré. Le fichier du juge a été remis tel quel (git propre).
- Mutation rejouée : c391ca38, `mutation.patch` appliqué au vert, `gradle-file.sh :app:testDebugUnitTest --tests *PremiereInstallationTest` : muté rc=1, `aFirstInstallReachesTheFirstSongsCreation` et `aFirstInstallExportsAndShowsItsFiveScreens` en échec, `aFailedStepSaysWhatWasOnScreen` vert ; sans la mutation rc=0, les trois verts. Identique à `rouge.xml` et `vert.xml` committés.
- Paire rejouée : d7abd35b / 14132399, `*ClipHoldTest` : rouge rc=1 (les 2 cas en échec), vert rc=0 (les 2 cas verts). Identique aux XML committés.
- Juge lancé comme le portail (`--resume`). Aucun résumé du dépôt ne porte l'arbre de cette tête (« résumé d'un autre code ») : le dernier selftest est antérieur à la fusion de retest. Sans résumé : 9 lignes = les 7 écarts de paires + « f88d08bb fusion sans trace du portail » (la fusion retest, que le portail de ce paquet écrira) + « résumé absent ». Avec un résumé d'un lot précédent dont j'ai remplacé l'arbre à la main (essai local, rien committé) : les mêmes 7 écarts, plus des « garde pas verte » propres à ce vieux résumé et la ligne f88d08bb. Les 7 sont bien : 2b34c216, a2a6ec58, 11c48708, 411b2d51, 9b8fc4fd, 96a7a702, ade1a174 ; plus de T5-AUTO-OPTION / CACHE-B19.

## Contre-exemples construits (toy git, avec les assistants de `test_ultra_bench.py`)
- Faux renommage (renommé + corps changé, ou « test au vert » vers un nom que le vert ne donne pas) : refusé. Faux renommage fait de plusieurs fichiers : seul le fichier qui porte le test compte, refusé si autre chose a bougé.
- Nom entre accents graves qui cacherait deux tests : impossible, `class_methods` rend chaque nom entier (`C#a`, `C#b`, `C#a b c`), `ADDED_TEST` aussi. Rien à signaler.
- Mutation qui ôte des lignes sans rapport : voir M1.

## Constats

### G1 (grave) : `still_runs` et `method_exists` ne ferment pas sur l'accent grave ; un test @Ignore ou sans @Test passe pour « tourne »
Les deux gardes cherchent `fun \`` + nom + `(?![A-Za-z0-9_])`. Avec un nom entier entre accents graves, « vert - x » est un préfixe de « vert - x y » (la suite est un espace, pas un caractère d'identifiant). `still_runs` lit les annotations du premier « fun » qui commence par ce nom, pas celui du nom exact.
Reproduit : un rejeu (`paires-rejouees/`) dont le test exact porte `@Ignore` (ou n'a pas `@Test`) et qui est précédé, dans le fichier, d'un frère « <nom> twice » portant `@Test` : le vert est fermé, aucun écart (témoin contraire, le frère placé après : écart). `still_runs(txt, "vert - a ... again")` rend True alors que cette fonction est sans @Test ; `method_exists` rend True pour un préfixe. C'est le trou que `test_in` dit fermer (« un test @Ignore ne tourne pas »). Le lot a justement réécrit ces deux motifs (`\b` devient `(?![A-Za-z0-9_])`) sans les arrêter sur l'accent grave fermant. Aucun témoin dans `test_ultra_bench.py` (`replay_backticks` n'essaie pas de frère à nom plus long).
À corriger : exiger l'accent grave fermant (ou `(?=`?\()`) après le nom, et un témoin « frère à nom plus long ».

### M1 (moyen) : la forme de `mutation.patch` laisse passer une mutation qui ne mute rien, ou qui plante
Le contrôle est sur le texte des lignes (strippées) comparé à l'ensemble des lignes que le vert a ajoutées au fichier, pas à leur place, et `CRASH` ne connaît que `throw`, `error(`, `TODO(`, `exitProcess`, `System.exit`. Passent, au jugement (reproduit) : ôter une ligne vide ou un commentaire ajouté par le vert (la mutation ne change rien) ; remplacer la ligne corrigée par `require(false)` ou `return (null as Int?)!!` (plante : n'importe quel test rougit) ; ôter une ligne d'une autre fonction, déjà là avant le vert, dont le texte égale celui d'une ligne ajoutée (`return 0`, `}`). Le juge ne peut donc pas distinguer cela d'une vraie mutation ; seuls `rouge.xml` (la parole du lot) et le rejeu du relecteur font foi. Pas bloquant : la fenêtre est bornée à l'histoire ancêtre de db66a3ce (aucun vert futur n'en profite) et les deux mutations réelles (c391ca38, 662c80ab) sont honnêtes : j'ai rejoué c391ca38 (le patch ôte `j.tap("welcome_later")`, ajouté par le vert). À noter dans la doc du juge : « la forme du patch ne prouve rien, le rejeu du relecteur oui ».

### I1 (info) : la preuve de 662c80ab n'a pas été rejouée ici
Seule c391ca38 l'a été (une mutation, une paire, comme demandé). Le patch de 662c80ab a la même forme (une ligne `blocVrai(...)` ôtée de `Feux.style`, aucun ajout).

### I2 (info) : les sept écarts qui restent sont bien nommés et honnêtes
Le rapport dit pourquoi pour chacun (dont 411b2d51, qui ne compile pas seul). Rien de fermé à tort dans les listes : les verts fermés le sont par rejeu ou mutation dont je n'ai trouvé que la faille G1 (hors données : aucune des sept paires ne dépend d'un @Ignore, `test_in` rend True à raison pour ClipHoldTest, relu à la main).


## Delta (relu 54064c9a, après les commits ec8cf2ad rouge et 54064c9a vert)
Le tour a été refait sur la branche reconstruite (12 commits sur fx-int ; `git merge-base --is-ancestor f88d08bb HEAD` échoue : la fusion avortée n'y est plus, le seul « fusion sans trace » restant est celle de l'intégration 57b16e8b, que le portail du paquet écrira).

- G1 : **corrigé**. `still_runs` et `method_exists` comparent le nom entier (`FUN`/`ANNOTATED_FUN` + `NAME` rendent le nom entre accents graves tel quel). Contre-exemple rejoué : test exact @Ignore (ou sans @Test) avec un frère à nom plus long avant ou après : `still_runs` False dans les deux cas ; frère qui ne diffère que par un texte de fin dans les accents graves (« . », « (2) », « again ») : False ; nom simple `foo` avec frère `foo2` : False ; test sain : True ; `method_exists` False pour un frère seul. Témoin du banc rougit quand on remplace `==` par `startswith` (mutation locale, remise).
- M1 : **corrigé**. Les cinq contournements (ligne vide, commentaire, `require(false)`, `!!` sur null, ligne antérieure de même texte) sont refusés ; toute ligne ajoutée (`+`) est refusée ; la ligne ôtée doit être, au numéro, une ligne que le vert a ajoutée, non vide et non commentée. Mutations du juge rejouées une à une (position, commentaire, refus des ajouts) : chacune est rattrapée par le banc (écarts en trop), puis le fichier remis (identique à la sauvegarde).
- Les deux vraies mutations (c391ca38, 662c80ab) restent acceptées : aucune des deux n'apparaît dans les écarts du juge.
- Juge lancé comme le portail (`--resume`, dernier résumé du dépôt, d'un autre code donc 28 écarts dont des « garde pas verte » propres à ce vieux résumé) : les 7 écarts de paires sont bien 2b34c216, a2a6ec58, 11c48708, 411b2d51, 9b8fc4fd, 96a7a702, ade1a174, plus 57b16e8b (fusion de l'intégration) ; f88d08bb n'apparaît plus.
- `python3 scripts/bench/test_ultra_bench.py` : vert (« ultra bench scripts: OK »), avant et après mes mutations. Je n'ai pas rejoué les tests Gradle de c391ca38 ici (inchangés depuis le premier tour, XML committés).

Verdict : 0 bloquant, 0 grave — fusion possible
