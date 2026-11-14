# Tour 2 : relecture neuve

Relecteur neuf (n'a pas écrit le code), diff `ecdab74..a8578c5`. Bilan : 1 Grave corrigé, 0 Moyen, 7 Mineurs laissés.

## Ordre de la relecture
1. **Thread audio** : aucun fichier C++ du moteur touché au tour 2 (seul `engine/host-tests/masterpro_tests.inc`, qui mesure maintenant le WAV écrit : vrai test). Gain de cible d'export : côté Kotlin et `BenchEngine` (tests seulement). Rien à signaler.
2. **Sauvegarde** : voir le Grave ci-dessous. Rapport d'export vers Documents (MediaStore Files, minSdk 31, échec = `false`, jamais de plantage), numéros de prise (`AudioClip.take` absent = 0, anciens projets relus et numérotés par leur nom), `SelectAllTextField` (valeur synchrone partout) : sains.
3. **Plantages** : `placeOnPad` / Pad Live attrapent `Throwable` (fichier natif non créé = aucun handle à libérer), Toasts tous sur le fil principal, question « 174 Mo » (ACCESS_NETWORK_STATE déclaré, état survit à la rotation), `ArrivalGuard` : sains.
4. **Fuites** : `PadSamples` pas modifié ; un échec de `NativeSample.create` ne laisse aucun handle. Rien.
5. **Tests « rouges avant »** relus : `PlaceFromLibraryTest` (UnsatisfiedLinkError non attrapée avant), `KeptNameFieldTest` (« Mon son 1Pluie »), `AloneListenVmTest.turningTheSoundAloneOff…`, `TakeNumbersTest`, `ExportTargetRound2Test` : vrais. Mais `noProjectNobodyMadeInMyProjects` ne vérifiait que l'effacement, jamais qu'un projet utilisé reste.

## Grave corrigé
| Point | Scénario | Correction | Test (rouge avant, vert après) | Commit |
|---|---|---|---|---|
| G1 premier projet effacé avec une prise dedans (`CurrentProject.takeUnusedStarter`, `ProjectsViewModel` init) | On enregistre sa voix sur « Mon premier rythme », on arrête et on touche tout de suite « Nouveau morceau » : la prise est écrite dans `takes/` + `samples/` du dossier, pas encore dans `project.json` (elle y entre à la prochaine ouverture). Au lancement suivant, `project.json` est identique à sa création : le dossier part en corbeille puis **définitivement**, la prise avec. Pareil pour une prise Live coupée (journal) ou une prise encore en écriture (TakeVault non attendu). | Le projet est gardé si son dossier contient le moindre fichier autre que `project.json`, `.bak`, `project.open.json`, ou s'il a un journal de prise Live ; l'effacement attend aussi `TakeVault`. | `UnusedStarterTest` (prise en attente, journal Live ; témoin : intact = effacé) + `Zone3Round2Test.noProjectNobodyMadeInMyProjects` toujours vert | fbcdebd |

Autres cas vérifiés sans défaut : modifié, renommé (`catalog.rename` réécrit le JSON), importé / partagé (nouvel id), dupliqué (nouvel id), restauré de la sauvegarde nuage (JSON différent ou sans sons), course d'autosave (attente de `ClosingSaves`, et `id == courant` jamais effacé), deux créations rapides (`rememberStarter` lit le fichier juste après sa première écriture, avant toute édition), changement de format JSON (comparaison fausse = gardé).

## Mineurs laissés (dans docs/REVIEW.md)
- m1 Le premier projet part par `deleteForever`, sans corbeille : aucun retour si la comparaison se trompait un jour. Gardé (le contenu est celui que l'appli fabrique), à revoir si un cas apparaît.
- m2 Après le sauvetage « s'est fermé deux fois en ouvrant « Mon premier rythme » … gardé tel quel », ce projet intact est effacé au même lancement : message faux (rien de l'utilisateur perdu).
- m3 Projet intact ré-obtenu (modifié puis annulé jusqu'à l'identique, ou restauré de la corbeille) : effacé quand même ; contenu identique à celui fabriqué.
- m4 `takesInOrder` : un son sans numéro arrivé après des prises numérotées (gel, import sur la piste) peut partager « 1 » avec « Prise 1 » (même `testTag`) ; son nom propre est affiché, donc rare à l'écran.
- m5 `SelectAllTextField` : après une prise de focus sans toucher (clavier, TalkBack), le premier déplacement de curseur est avalé une fois.
- m6 `WavExporter.copyInto` : un premier essai MIME raté à mi-copie relance avec le second MIME (doublon impossible : l'entrée ratée est effacée, mais double écriture sur un téléphone plein).
- m7 `ArrivalGuard` : `asked` lit et remet à zéro un drapeau pendant la composition (effet de bord en composition, sans conséquence observée).
