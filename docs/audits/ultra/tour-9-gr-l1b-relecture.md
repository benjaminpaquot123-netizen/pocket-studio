# Tour 9, lot gr-l1b : relecture neuve

Relecteur : sous-agent neuf (n'a pas écrit le code), diff b0ac8fe3...eq/gr-l1b.

relu : bb47e3992c66b629a112c7816f13b4cd01092e4c

Le lot joue le corps commun du format v2 : les sons de couche sur les pads libres, la boucle sur une piste audio, le chop vocal échantillonné, le coup de transition sur la crash et le shaker du corps. Les toms de break passent sur les pads 7 et 8, et honest() garde la pompe des accords du style. Les indices de pads (LAYER_POOL, TOM_LANES, TOM_HIGH = 8) et les copies de canaux de mixage sont cohérents avec Role.lane. Le découpage des parties garde `muted`, et le moteur C++ n'est pas touché. J'ai trouvé un plantage possible, latent pour l'instant (aucun style ne remplit encore kitSounds), dans le choix du son nommé de la bibliothèque. Le reste est mineur : un conflit entre les couches et l'extra du dernier Drop, une double compression des accords, des fichiers orphelins, une racine de chop forcée, un commentaire inexact et deux tests trop faibles.

1. Grave : `app/src/main/java/com/pocketstudio/app/library/SoundLibraryViewModel.kt:234`. `packs.first { it.id == PACK_PREFIX + named.packId }` lève NoSuchElementException quand le son nommé par `style.kitSounds[role]` vient d'un pack qui n'a aucun son du type de la ligne (`role.packKind`). En effet, `packs` (l. 225) ne garde que les manifestes où `ofKind(role.packKind)` n'est pas vide, alors que `PackRefs.resolve` trouve le fichier quel que soit son type. Exemple : Rim (perc) nommé dans un pack qui ne contient que des kicks. L'assistant plante alors dans defaultRow / withStyle. Le défaut est latent, car aucun style ne remplit encore kitSounds et `lib_sound` de recipes_to_kotlin.py ne vérifie pas le type. Il faut `firstOrNull` avec un repli.
2. Mineur : `project-model/src/main/kotlin/com/pocketstudio/model/StarterWizard.kt:1582` (withLayers) contre `:2557` (withDropLayer). L'extra G0 du dernier Drop se pose sur le pad 6 (OpenHat) ou 13 (Perc2) quand ce pad est vide. Or ces deux pads sont dans LAYER_POOL (l. 1543), et withLayers passe après et réécrit le pad de destination dans toutes les scènes. L'extra du dernier Drop est donc perdu, remplacé par la copie de la ligne doublée.
3. Mineur : `project-model/src/main/kotlin/com/pocketstudio/model/WizardMix.kt:445`. Les accords pompés gardent leur PUMP mais restent membres du MusicBus, qui les baisse aussi : ils sont donc compressés deux fois. Les copies des accords (double, padLayer) ne sont pas dans partOfTrack : elles perdent leur pompe et ne suivent plus les accords.
4. Mineur : `SoundLibraryViewModel.kt` (bodyExtras, l. 1390 à 1432). Le tempo de la boucle vient de `PackManifest.bpm` (120 par défaut), pas du son lui-même. `fit` est donc toujours vrai, et une boucle d'un autre tempo dans un pack sans bpm est étirée à tort.
5. Mineur : `SoundLibraryViewModel.kt:1427` et l. 1413 à 1422. Le chop vocal, le coup de transition et les couches sont écrits dans `files` même quand le modèle ne les utilise pas : pas de ligne vocale ou pas d'humeur, pas de crash (style avec une Ride), ligne doublée muette. Cela laisse des wav orphelins dans le projet.
6. Mineur : `SoundLibraryViewModel.kt:1427`. `root ?: 60f` fait passer une détection de hauteur ratée pour une racine connue (do 60). La branche « racine inconnue » de `SampledPatch.fitRoot` ne sert plus, et le chop peut être transposé faux.
7. Mineur : `StarterWizard.kt:866`. Le commentaire dit que la boucle se tait dans l'Intro, le Break et l'Outro, mais le code ne coupe que les parties d'Intro du début (le rapport dit bien « jusqu'à la fin »). Le clip a `scene = null` : il s'entend donc aussi quand on joue l'Intro seule dans le Live.
8. Mineur (tests) : `project-model/src/test/kotlin/com/pocketstudio/model/BodyHeardTest.kt:98`. theDrummersTomsLeaveTheSnap utilise trap, qui est de forme Rap (chanté), dont le break passe par les charlés, et qui n'a ni ligne Tom ni Snap. Le chemin des toms n'est probablement pas parcouru, et le test passerait sans doute aussi sur l'ancien code. Le test `:90` theChordsPumpIsKept ne vérifie qu'« une PUMP sur une piste d'instrument quelconque », pas sur le canal des accords.

Verdict : 0 bloquant, 1 grave

## Relecture 2 (sous-agent neuf, sur 9e812b78 : ajout 2 et grave de la liste des sons)

1. Grave : `StarterWizard.kt:2522`. La Doublure n'était vidée qu'en dehors des Drops (`spec.base != 2`), sans coupe de scène (`windowed`) ni lien avec les accords : un Post-refrain sans accords, ou le faux drop du riddim, l'auraient gardée. Corrigé dans 5948fdcc : coupée comme les accords, muette dans une scène sans accords. Le test (BodyHeardTest#theBodyKeepsTheSongsShape, tous styles × humeurs × parties) est vert avant et après : aucun style actuel n'atteint ce cas. C'est donc une protection, pas un défaut entendu.
2. Mineur : les 6 styles qui ont leur propre contre-chant le perdent quand on garde la basse seule. Choix noté dans DECISIONS.md.
3. Mineur : avec les accords seuls, le contre-chant fait encore deux tours de stabs. Le commentaire de planFor (« with neither ») ne promet que le cas sans lead ni accords. Non traité.
4. Mineur : SoundCategoriesTest#aNamedSoundOfAnotherKindDoesNotCrashTheList ne vérifie pas l'ordre des catégories. Non traité.
5. Mineur : `scripts/packs/recipes_to_kotlin.py:315` contrôle les slugs sur un pack qui est déjà dans PACKS. Hors des fichiers du lot (scripts/packs/), noté.
6. Mineur : la plage `[low, high]` des parties d'instrument n'est lue nulle part. Noté pour le lot de la forme du morceau.
7. Mineur : BodyHeardTest dure 22 s. Accepté : il couvre tous les styles.

Verdict : 0 bloquant, 1 grave (corrigé, protection).
