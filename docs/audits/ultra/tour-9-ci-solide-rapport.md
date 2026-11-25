# Tour 9, lot ci-solide : rapport

Demande de Benjamin (2026-11-25) : réparer les contrôles capricieux et les téléchargements qui font rougir la CI, et écrire les astuces pour de bon.

## Ce qui rougissait, et ce qui est fait

| Rouge | Cause | Fait | Preuve |
|---|---|---|---|
| CI 455 et 458 : `ScenariosTest`, « silence » | Prouvée : une fenêtre d'écoute de 60 ms finie avec moins de 4 lectures sur une machine chargée, donc aucune seconde entière | L'écoute continue jusqu'à une seconde entière (30 s de plus au plus), sinon « non mesuré » | rouge 14fecb43, vert ffcf98cb ; 5 passages `--rerun` verts sur 5 (36 + 13 tests), dont 2 avec 8 processus qui chargent la machine |
| CI 446 : `EnduranceTest`, 5043 ms pour 5000 | Mesurée à la CI 460 : la première manche tient le fil principal 4457 ms, dont 53 ms perdus par la machine (manches suivantes : 224, 242, 106 ms). C'est l'appli à froid, pas un émulateur figé | Le fil principal est comparé à un fil témoin, la limite ne bouge pas : la mesure est juste, mais elle ne retire presque rien. **Pas réglé** : voir plus bas | « Emulator smoke test » de la CI 460, vert à 4457 ms |
| CI 462 : `ScenariosVraisEcransTabletteTest`, « aucun autre projet visible dans « Mes projets » » | Prouvée : le robot lisait les lignes une seule fois, alors que la liste attend les dernières sauvegardes des écrans fermés avant sa première lecture (« Chargement de la liste… », 20 s au pire). Mesuré : une sauvegarde tenue 8 s, les lignes arrivent 8,5 s plus tard | Le robot attend la liste comme un utilisateur (25 s au plus) à chaque visite de « Mes projets » ; le message dit si la liste chargeait encore. **Tenté, pas réglé** : voir plus bas | rouge d71f7827, vert 1236e700 ; ensuite 14 passages forcés des deux classes d'écran : 13 verts, 1 rouge (« liste encore en chargement après 25 s ») |
| Nuit 13 : trois étapes perdues | Lue dans le journal : le fichier Android de Robolectric non téléchargé | Même cache et mêmes miroirs que la CI | « Nightly real use » lancé à la main sur la branche |
| Téléchargements à un seul essai | Lus dans les workflows | Réessais partout : sous-modules (12 jobs), `python3-pil`, licence des packs, bibliothèque de séparation et son modèle | `scripts/ci/submodules.sh` essayé avec un faux git (réussite au 3e essai, échec après tous les essais) ; YAML relu par `yaml.safe_load` ; `shards.py policy` : OK |

Écrit pour de bon : `docs/AIDE-TELECHARGEMENTS.md` et une règle dans `CLAUDE.md`. Outils Android d'une discussion : `scripts/session-android.sh` (joué ici de zéro : 33 s, tout installé).

- rouge : 14fecb43. `ScenariosTest#aMachineTooSlowToReadFourTimesInTheWindowStillHearsWholeSeconds` et `ScenariosTest#theMusicOnASlowMachineIsNeverReportedAsSilence` rouges, message de la même forme que la CI (« silence à 240.00 bpm : 0 s »).
- vert : ffcf98cb. Les deux verts, plus `ToutTesterTest#fewerReadingsThanOneWholeSecondAreNotHeardRatherThanASilence`.

- rouge : d71f7827. `ScenariosVraisEcransTest#theOtherProjectIsWaitedForWhenTheRowsComeAfterTheirScreen` rouge (et le même sur l'écran tablette), message de la CI 462 (« aucun autre projet visible dans « Mes projets » »).
- vert : 1236e700. Le même vert sur les deux écrans.

tests: com.pocketstudio.bentest.ScenariosTest com.pocketstudio.bentest.ToutTesterTest com.pocketstudio.bentest.ScenariosVraisEcransTest com.pocketstudio.bentest.ScenariosVraisEcransTabletteTest

chemin: robot d'écoute, scénarios joués sur une machine lente | test: ScenariosTest#theMusicOnASlowMachineIsNeverReportedAsSilence
chemin: robot d'écoute, fenêtre trop courte pour 4 lectures | test: ScenariosTest#aMachineTooSlowToReadFourTimesInTheWindowStillHearsWholeSeconds
chemin: robot d'écoute, bilan « non mesuré » plutôt que « silence » | test: ToutTesterTest#fewerReadingsThanOneWholeSecondAreNotHeardRatherThanASilence
chemin: robot, « Mes projets » dont les lignes arrivent après l'écran | test: ScenariosVraisEcransTest#theOtherProjectIsWaitedForWhenTheRowsComeAfterTheirScreen

## Pas prouvé, pas fait

- `EnduranceTest` n'est pas réglé. La première manche prend 4 à 5 s d'appli à froid (4457 ms ici ; 5043, 4294, 2864, 1417, 1169 ms dans les mesures du lot ci-fiab) pour une limite de 5000 : le rouge peut revenir environ une fois sur six. Aucun seuil élargi. Au chef de trancher : chercher ce qui tient le fil principal 4 s à la première manche (le vrai défaut, vu d'un utilisateur : l'appli figée 4 s à froid), ou une manche d'échauffement non mesurée.
- « Mes projets » n'est pas réglé. Un passage sur 14 a encore rougi après la réparation : la liste chargeait toujours après 25 s, plus que les 20 s que le code explique. Cause pas trouvée : 10 passages de chasse ensuite, tous verts. Posé pour la prochaine fois : un scénario rouge des deux classes d'écran écrit le journal de l'appli et les piles de ses fils dans la sortie du test (`ScenariosVraisEcransTest.jouer`), donc dans le rapport de la CI.
- Vu de l'utilisateur, hors lot (`ProjectsViewModel.kt`) : « Mes projets » peut afficher « Chargement de la liste… » jusqu'à 20 s si une sauvegarde de fermeture traîne, parce que la première lecture de la liste passe après cette attente. Piste : lire la liste d'abord, attendre ensuite.
- Le plafond de 30 s de l'écoute n'est pas atteint par un test de scénario entier.
- Hors lot, pour le chef : réseau possible dans les tests (`PackStore.fetchCatalogue`, `UpdateChecker.check`) ; `EssaiEclair.mesurer` (même faux silence) ; fuite d'état de `Tour5BloquantsTest` ; `setup-android` sans cache ; `git clone` / `pip install` des packs à un essai ; `ScenariosTest` (environ 140 s) n'a pas de ligne dans `scripts/ci/test-durations.tsv`.
- Le correctif de la nuit n'arrive sur `main` qu'à la fusion du tour 9.
