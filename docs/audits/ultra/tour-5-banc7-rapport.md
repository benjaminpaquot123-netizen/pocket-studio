# Tour 5, lot « banc7 » : fin de l'étape 3, premier paquet (7.1 à 7.5 et critère 9)

Branche : `claude/socle-du-banc-alqcr3`, après le lot « socle ». Décisions : `docs/decisions/tour-5-banc7.md`. Quatre agents en parallèle (copies isolées), réunis ici.

| Point | Fait | Preuve |
|---|---|---|
| 7.1 | Le portail lit le rapport de zone (`tests:`, liste des chemins, verdict des rejeux ajoutés) et refuse sinon, à partir du paquet 2 ; base, rapports et tableau recalculés après chaque fusion | 6 paires rouge/vert (e658094d … 0ab00dcd) ; `test_ultra_bench.py` (`zone_report`, `portal_recomputes`), `test_pre_vol.sh` |
| 7.2 | La garde Gradle a son test | `scripts/bench/test_garde_gradle.sh` : 4 ok ; rougit garde vide (2) ou sans condition de projet (1) |
| 7.3, 7.4 | Consignes des équipes du tour 5 sans blanc : zones, ordre, propriétaires, `marque-corrige.sh`, `I18n.AREAS` | `sieges/tour-5/consignes-equipes.md` |
| 7.5 | Un rejeu raté dit le dernier écran qui diffère et le bouton absent ou déplacé | `BenchReplayReadableTest` rouge 6a1f96e6, vert 0afb909f |
| Critère 9 | 17/20 défauts vus par Benjamin montrés aujourd'hui (85 %, seuil 80 %) | `defauts-caches.py` : 0 écart, critère rempli ; `HiddenDefectsGuardTest` (CACHE-B19, CACHE-B20) |

Reste pour le second paquet (banc7b) : 7.6 essai à blanc, 7.7 temps mesuré, critère 1 (dix selftests de suite), critère 8 (tour de fumée à 4 sièges), revérification des critères 2 à 5.

tests: com.pocketstudio.app.human.BenchReplayReadableTest com.pocketstudio.app.nav.HiddenDefectsGuardTest

## Liste des chemins

- chemin: rejeu d'un parcours dont un bouton a bougé | test: BenchReplayReadableTest#aMovedButtonIsSaidInTheVerdict
- chemin: Mix › Automatiser un réglage, « Filtre (balayage) » | test: HiddenDefectsGuardTest#theFilterSweepJustAddedIsStillNamedInThePicker
- chemin: Guide › Voir le réglage, envoi réverbe | test: HiddenDefectsGuardTest#showTheSettingBringsTheReverbSendIntoView
