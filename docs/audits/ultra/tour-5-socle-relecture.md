# Relecture du lot « socle » (tour 5)
Relu : 5fc1ad908ad2e99cd84e5d6af1ca22b349d65d50

Relu : `git diff 0d52cebc..HEAD`, puis les corrections `git diff f82a4094..HEAD` (65287320 rouge, a48b1c12 vert, 5fc1ad90 docs). Joués ici, rien modifié : `test_socle.py` (15 verts), `test_pre_vol.sh` (tout vert, dont les 6 cas neufs du vrai portail). Les mutations de la première passe (`-z`, `-lt`, `if false`, `checkout` sans HEAD) ont été rejouées par le coordinateur ; je ne les ai pas refaites (lecture seule) et je lis les nouveaux tests : le portail complet est lancé, et c'est son message (« decisions written outside », « moves the batch ») qui est exigé, présent au paquet 2, absent au paquet 1 et pour le fichier du lot lui-même.

| n° | gravité | constat | preuve |
|---|---|---|---|
| 1 | grave | Règle « décisions écrites ailleurs » et `from_paquet` du portail non prouvées par un test qui rougit. | corrigé : `test_pre_vol.sh` (`portal_says`) exige du vrai portail la raison au paquet 2 (decnew, decother), son absence au paquet 1 (decold) et pour le fichier propre du lot (decown) ; `test_ultra_bench.py` fait écrire l'index par le portail complet de la maquette (commit des traces contenant `DECISIONS.md`, « 2 décision(s) »). |
| 2 | moyen | `merge=union` sur le journal du banc, dont les lignes changent sur place (doublon silencieux, faux « sans preuve »). | corrigé : la ligne est retirée de `.gitattributes` (reste `DECISIONS.md`) ; `test_socle.py` refuse l'union sur le journal, verdicts.tsv et PLAN-RESTE. |
| 3 | moyen | `undo_merge` ne défaisait pas la ligne de `DECISIONS.md` déjà indexée quand le commit des traces échoue. | corrigé : `git checkout -q HEAD -- docs/DECISIONS.md` ; garde dans `test_ultra_bench.py` (hook `commit-msg` qui refuse le commit des traces : code non nul, rien de fusionné, arbre propre, `DECISIONS.md` inchangé). |
| 4 | moyen | Garde « gelée par paquet » faible : un lot pouvait changer lui-même `portail-paquet.txt` ; condition inversée non vue ; clone superficiel sans effet. | corrigé pour les deux premiers points : règle « moves the batch » (portail et pré-vol, `from_paquet 2`), testée par le portail réel, `KNOWN_RULES` mis à `13 551b6bf796` (recalculé juste : pas d'avertissement) ; la condition inversée est couverte par le n° 1. Reste connu, dit dans le rapport : la garde ne lit que les lignes « not merged » / « undone » et ne dit rien sur un clone superficiel. |
| 5 | moyen | Phrases plus fortes que la preuve : « Renommage réglé » sur trois verts dont deux CI non relisibles ici ; autotest cité sur 28e64fe3, antérieur à abc05262 et f82a4094 (fiche, point 4). | La phrase « réglé » du rapport est inchangée ; l'autotest n'est pas rejoué sur le dernier commit de code (le coordinateur dit en lancer un). Point 8 de la fiche traité (rien à suivre). Ne bloque pas : à confirmer par le selftest du portail. |
| 6 | mineur | `awaitGuide` n'aboutit pas si `guide_bar` existe sans `guide_bar_step` rempli (GuidePill, ligne vide) ; le test de garde ne couvre que 2 des 5 captures. | Non traité (test seulement, téléphone). `GuidePanel.kt` 123-131, 162, 190-199. |
| 7 | mineur | Tolérance `HALO_TOLERANT` à 0,15 par pixel : une dérive de teinte plus faible passe ; mesure faite ici, pas sur la CI. | Non traité. `RemixSongScreenshotTest.kt` 45-54. |
| 8 | mineur | `reachTag` rend des coordonnées même hors écran après 8 glissés, sans « divergé » ; `test_the_tester_resolves_a_tag` n'est qu'un `assertIn`. Rejeu non rejoué ici (pas de Gradle). | Non traité. `HumanTester.kt` `reachTag`. |

Relu sans trouver de défaut : le travail « quick » est bien en tête de la CI et chaîné pour tous les autres, `--rapide` s'arrête avant le moteur, le portail garde l'ordre de ses étapes, `recette.json` réel reste couvert par `LogicTest`, les corrections ne touchent ni le code de l'appli ni les captures.

Le rejeu t4-26-prise-clavier : l'ancien et le nouveau fichier de gestes disent la même suite de 32 pas (mêmes cibles dans le même ordre, les 6 glissés de défilement deviennent « attends 1 »), l'oracle et le départ sont inchangés.

rejeu changé : t4-26-prise-clavier : les 26 touchers en pixels (cassés à chaque changement de mise en page) visent maintenant l'étiquette du même bouton, avec le même oracle et la même suite de 32 pas.

Verdict : 0 bloquant, 0 grave
