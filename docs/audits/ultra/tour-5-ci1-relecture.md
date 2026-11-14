# Relecture neuve du lot « ci1 » (tour 5)
Relu : ef6c1e177fa404fe605c8fa33cd728a1b3ac4e15

Relecteur neuf, qui n'a pas écrit le code. Lu : le diff entier depuis la base d'intégration (ProjectsViewModel.kt, Journey.kt, JourneyPathsTest, UnusedStarterTest, rapport, décisions) et 7 des 16 captures, avant / après côte à côte.

## Ce qui a été rejoué

- Tête du lot : `JourneyPathsTest`, `projects.*`, `nav.ModeChoiceTest` : 46 tests, 0 échec.
- Mutation 1 (la file retirée : `ProjectsViewModel.kt` remis tel qu'à la base, Mutex + une coroutine par opération) : `UnusedStarterTest#anUntouchedFirstProjectGoesBeforeTheListIsShown` rouge (« round 52: the list loaded before the first project went »).
- rouge constaté : dd89cd10147e1d81f87bcdf86d71b05ad3469ebb : rejoué sur 97b1df2c100cc979950523f42552fb5f8f0ccf99 : le fichier du parent est celui de la base, le test neuf échoue au 52e tour sur 200.
- Mutation 2 (la remise du mode par « Changer » et la vérification finale retirées de `Journey.newSongInAssistant`) : `JourneyPathsTest#aLaterStartWithAnotherLastModeChangesItInTheAssistant` rouge (« the mode the app was in, set back with « Changer » »).
- Mutation 2 bis (la vérification finale seule retirée) : `JourneyPathsTest` reste vert. Attendu : les deux tests Robolectric vérifient eux-mêmes le mode ; la ligne `check` ne protège que le parcours sur émulateur (`JourneyTest`), qui n'a pas d'autre vérification du mode à cet endroit.

## La file de « Mes projets »

- Un seul travailleur (`viewModelScope`, `Dispatchers.IO`) lit une file sans limite : `trySend` ne peut pas échouer (la file n'est jamais fermée), les opérations tournent dans l'ordre demandé.
- Blocage : aucune opération n'attend la fin d'une autre. Celle du démarrage met une suppression dans la file (`::delete`) sans l'attendre : elle passe après, comme avec le verrou. `withContext(Dispatchers.Main)` (« then », « saveFirst ») ne revient jamais vers la file. Les attentes de `ClosingSaves`/`TakeVault` (10 s au plus) retardaient déjà les suivantes avec le verrou : rien de neuf.
- Erreur dans une opération : `catch (e: Exception)` la rattrape (y compris une `TimeoutCancellationException`), le travailleur continue, `busy` redescend dans le `finally`. Une `Error` le tuerait, mais elle plantait déjà l'appli avant (même portée).
- Annulation : à `onCleared`, le travailleur s'arrête et les opérations encore dans la file ne tournent pas ; avant, celles qui attendaient le verrou étaient annulées de la même façon. Pas de perte nouvelle : l'ouverture d'un projet ne passe pas par cette file.
- Fuite : une coroutine par modèle, suspendue sur la file, annulée avec lui. Dans le test neuf, 200 modèles jamais effacés gardent chacun la leur jusqu'à la fin du test (mineur, test seul).

## Le parcours partagé

- Pas plus permissif sur le mode : si « Simple ou Pro ? » est posé, la carte du mode voulu est touchée ; sinon « Ce morceau sera en mode … » est exigé, « Changer » remet le mode voulu, et la fin vérifie le mode dans les deux cas (mutation 2 rouge).
- `firstLaunch` reste strict (l'étape doit être posée au premier lancement).

## Captures

- Regardées en entier avant / après : `brand/welcome`, `i18n/ar_welcome`, `scale130/fr_welcome`, `remix_home`, `scale130/fr_simple_mix`, `tablet/land_wizard_style_open`, plus la zone qui change dans `scale130/fr_guide_simple` (le nom de la barre du haut). Accueil : la dernière carte s'efface au-dessus de la pastille « ↓ n autres » au lieu d'être coupée dessous (voulu, be20a26c). Remix : écart minime du rendu du libellé du bouton orange. Assistant sur tablette : famille ouverte un peu plus marquée. Aucune des cinq captures interdites n'est touchée.
- Constat moyen, hors de ce lot (vient de d85cdba8, déjà fusionné) : à 130 % en Mode simple, la barre du haut ne garde que le dossier ; la coche « Sauvegardé » et « REC dans … », qui vivent sous le nom, disparaissent avec lui (`scale130/fr_simple_mix`, `fr_guide_simple`). À reprendre par un lot de la barre du haut : garder la coche ou « REC » à côté du dossier quand le nom est retiré.
- `brand/welcome` : le titre « Studio » et la phrase d'accueil peu lisibles sur fond clair, déjà ainsi avant (pas de ce lot).

## Points restants (mineurs)

| n° | Gravité | Constat | Suite |
|---|---|---|---|
| 1 | moyen | Coche « Sauvegardé » et « REC dans … » absentes à 130 % en Mode simple (d85cdba8, hors lot) | à signaler au lot de la barre du haut |
| 2 | mineur | Le `check` final de `Journey.newSongInAssistant` n'est rougi que sur émulateur (Robolectric vérifie le mode lui-même) | rien |
| 3 | mineur | 200 modèles de vue laissés vivants dans `anUntouchedFirstProjectGoesBeforeTheListIsShown` | rien |

Verdict : 0 bloquant, 0 grave — fusion possible
