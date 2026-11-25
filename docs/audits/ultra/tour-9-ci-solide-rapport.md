# Tour 9, lot ci-solide : rapport

Demande de Benjamin (2026-11-25) : réparer les contrôles capricieux et les téléchargements qui font rougir la CI, et écrire les astuces pour de bon.

## Ce qui rougissait, et ce qui est fait

| Rouge | Cause | Fait | Preuve |
|---|---|---|---|
| CI 455 et 458 : `ScenariosTest`, « silence » | Prouvée : une fenêtre d'écoute de 60 ms finie avec moins de 4 lectures sur une machine chargée, donc aucune seconde entière | L'écoute continue jusqu'à une seconde entière (30 s de plus au plus), sinon « non mesuré » | rouge 14fecb43, vert ffcf98cb ; 5 passages `--rerun` verts sur 5 (36 + 13 tests), dont 2 avec 8 processus qui chargent la machine |
| CI 446 : `EnduranceTest`, 5043 ms pour 5000 | Probable, pas prouvée ici : émulateur figé tout entier | Le fil principal est comparé à un fil témoin ; la limite de l'appli ne bouge pas | Compilé ici (`:app:compileDebugAndroidTestKotlin`) ; pas d'émulateur ici : preuve = « Emulator smoke test » de la CI du lot |
| Nuit 13 : trois étapes perdues | Lue dans le journal : le fichier Android de Robolectric non téléchargé | Même cache et mêmes miroirs que la CI | « Nightly real use » lancé à la main sur la branche |
| Téléchargements à un seul essai | Lus dans les workflows | Réessais partout : sous-modules (12 jobs), `python3-pil`, licence des packs, bibliothèque de séparation et son modèle | `scripts/ci/submodules.sh` essayé avec un faux git (réussite au 3e essai, échec après tous les essais) ; YAML relu par `yaml.safe_load` ; `shards.py policy` : OK |

Écrit pour de bon : `docs/AIDE-TELECHARGEMENTS.md` et une règle dans `CLAUDE.md`. Outils Android d'une discussion : `scripts/session-android.sh` (joué ici de zéro : 33 s, tout installé).

- rouge : 14fecb43. `ScenariosTest#aMachineTooSlowToReadFourTimesInTheWindowStillHearsWholeSeconds` et `ScenariosTest#theMusicOnASlowMachineIsNeverReportedAsSilence` rouges, message de la même forme que la CI (« silence à 240.00 bpm : 0 s »).
- vert : ffcf98cb. Les deux verts, plus `ToutTesterTest#fewerReadingsThanOneWholeSecondAreNotHeardRatherThanASilence`.

tests: com.pocketstudio.bentest.ScenariosTest com.pocketstudio.bentest.ToutTesterTest

chemin: robot d'écoute, scénarios joués sur une machine lente | test: ScenariosTest#theMusicOnASlowMachineIsNeverReportedAsSilence
chemin: robot d'écoute, fenêtre trop courte pour 4 lectures | test: ScenariosTest#aMachineTooSlowToReadFourTimesInTheWindowStillHearsWholeSeconds
chemin: robot d'écoute, bilan « non mesuré » plutôt que « silence » | test: ToutTesterTest#fewerReadingsThanOneWholeSecondAreNotHeardRatherThanASilence

## Pas prouvé, pas fait

- `EnduranceTest` : jamais joué ici. Si la fumée sur émulateur de la CI est rouge, lire son message (il donne maintenant le temps brut et le temps de la machine).
- Le plafond de 30 s de l'écoute n'est pas atteint par un test de scénario entier.
- Hors lot, pour le chef : réseau possible dans les tests (`PackStore.fetchCatalogue`, `UpdateChecker.check`) ; `EssaiEclair.mesurer` (même faux silence) ; fuite d'état de `Tour5BloquantsTest` ; `setup-android` sans cache ; `git clone` / `pip install` des packs à un essai ; `ScenariosTest` (environ 140 s) n'a pas de ligne dans `scripts/ci/test-durations.tsv`.
- Le correctif de la nuit n'arrive sur `main` qu'à la fusion du tour 9.
