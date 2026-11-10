# Audit « banc crédible » 2 (A.17) : synthèse

Écrit le 2026-11-10 par un auditeur seul, sur `eq/tour-5-audit-banc2` (copie de fx-int à `3dfdd0b`). Les quatre dimensions laissées de côté par `AUDIT-BANC-CREDIBLE.md` : portail et scripts, réalisme du testeur, dépôt, méthode. Expériences dans une copie jetable (worktree et clone local sous le scratchpad, supprimés à la fin), aucun Gradle, aucune correction. Pas de contre-expertise par un second agent : chaque constat porte sa preuve copiée ; ce qui n'est que lu dans le code est dit « déduit » ou rangé en « piste ».

## En trois lignes (pour Benjamin)

- Le portail de fusion se laisse tromper : une branche qui débranche le contrôle du banc a été fusionnée par le script, sans aucun test, avec une relecture qui écrivait elle-même « 1 bloquant ». C'est confirmé, à corriger en premier.
- Le testeur IA lit à l'écran le nom des icônes et voit « bouton » là où toi tu as vu une simple phrase (« Mes exports ») : le banc ne peut pas trouver ce genre de défaut de prise en main.
- Aucune trace des contrôles n'est gardée dans le dépôt : les chiffres « 149 verts » ou « 22 écarts » ne se vérifient pas après coup.

## Comptes

| Dimension | Rapport | bloquant | grave | moyen | mineur |
|---|---|---|---|---|---|
| Portail et scripts | `portail-et-scripts.md` | 1 (P1) | 2 (P2, P3) | 4 (P4-P7) | 1 (P8) |
| Réalisme du testeur | `realisme-testeur.md` | 0 | 1 (R1) | 3 (R2-R4) | 1 (R5) |
| Dépôt | `depot.md` | 0 | 1 (D1) | 2 (D2, D3) | 1 (D4) |
| Méthode | `methode.md` | 0 | 1 (MT1) | 2 (MT2, MT3) | 1 (MT4) |
| **Total** | | **1** | **5** | **11** | **4** |

## Les pistes E1 à E6 de l'équipe « portail » (AUDIT-BANC-CREDIBLE § 5)

| Piste | Résultat sur `3dfdd0b` |
|---|---|
| E1 tampon forgé | confirmé → P2 (grave : demande un geste délibéré, mais ne laisse aucune trace, D1) |
| E2 juge trompé (`@Ignore`, commentaire, …) | confirmé pour l'existence des preuves → P5 (moyen : 3 preuves seulement hors selftest) ; résumé du selftest forgeable → P3 (grave) ; les cas « commit `-` / paires » non rejoués (juge réécrit, règle (c) chez un autre agent) |
| E3 fusion forgée | confirmé → P1 (bloquant) et P4 (moyen) |
| E4 selftest-verdict | « classe entièrement sautée » corrigé depuis (A.4, lu dans `selftest.sh`) ; « quarantaine couvre toute cause » confirmé → P6 |
| E5 paires rouge/vert | non rejoué (règle (b) réécrite au lot 1) |
| E6 sortie de siège | confirmé et élargi → P7 |

## Bloquant et graves, en une ligne chacun

- **P1 (bloquant)** : `fusion-equipes.sh` cherche la phrase « Verdict : 0 bloquant, 0 grave » n'importe où dans la relecture et ne lance le selftest qu'à travers `rejeux/` ; une branche qui casse `tampon.sh` et déplace le seul rejeu (t4-26) est fusionnée, exit 0, sans test. Correction : verdict sur une ligne imposée et tableau sans grave ouvert, selftest et `Bench*` toujours lancés à la fusion, `rejeux/` vide = refus.
- **P2 (grave)** : `tampon.sh ecris` sans selftest → `verifie` exit 0. Correction : le tampon exige et scelle le résumé vert et les XML du lot.
- **P3 (grave)** : 12 lignes `vert : …` tapées à la main font passer le juge de 38 à 22 écarts. Correction : le résumé porte l'empreinte du code, le juge la compare.
- **R1 (grave)** : icône seule écrite `bouton « Plus de choix »`, lien-phrase écrit `bouton « Mes exports : … »` (que Benjamin a trouvé illisible comme lien). Correction : écrire l'icône (glyphe) et non son nom, « bouton » seulement si ça se dessine comme un bouton, et le dire aux testeurs.
- **D1 (grave)** : 0 fichier de résumé, tampon ou rapport du juge commité. Correction : les garder par lot et par fusion, les chiffres cités y renvoient.
- **MT1 (grave)** : les critères de sortie ne comptent que les faux défauts, jamais les défauts que le banc cache. Correction : critère « vu par le banc ? » pour chaque ❌ de Benjamin.

## Ordre proposé (à coller dans PLAN-RESTE après A.16)

| id | Quoi | Preuve | Durée |
|---|---|---|---|
| A.18 | P1 + P4 + MT2 : fusion durcie (verdict sur une ligne, pas de grave ouvert dans le tableau, relecture suivie seulement, selftest et `Bench*` toujours, `rejeux/` vide ou rejeu retiré = refus) | `rouge:`/`vert:` sur `test_ultra_bench.py` avec les essais forge, forge2, forge3 de `audit-banc2/portail-et-scripts.md` | 2,5 h |
| A.19 | P2 + P3 + D1 : tampon scellé au résumé et aux XML, résumé daté par l'empreinte, traces commitées par lot et par fusion | `rouge:`/`vert:` : tampon sans résumé refusé, résumé d'un autre code = écart ; `git ls-files` des traces de la prochaine fusion | 3 h |
| A.20 | R1 (+ consignes de R2) : écran en texte sans noms d'icônes ni « bouton » pour un texte nu ; consignes testeur | `BenchSelfTest` rouge/vert : `top_more` écrit `bouton (icône ⋮)` ; texte de consigne | 2 h |
| A.21 | MT1 + MT3 : critère « vu par le banc ? » et état « corrigé, à confirmer » | colonne remplie pour les 8 ❌ du 8 novembre ; écart du juge sur les 4 lignes de MT3 | 1,5 h |
| A.22 | Moyens restants : P5, P6, P7, R2 (option), R3, R4, D2, D3 | une ligne par constat dans le journal | 6 h |

## Limites

- Un seul auditeur, pas de sceptique : P1, P2, P3, P4, P5, P6, P7 sont rejoués (sorties copiées) ; R1 est vu dans le code et dans les écrans commités du tour 5 ; R2, R4 et D3 sont lus dans le code ou dans git, leurs effets « déduits » sont marqués ; aucun siège n'a été lancé.
- Non regardés : `fuse-reports.py`, `seat-metrics.py`, `tableau-de-bord.py`, `fabrique-sieges.py`, `applique-corrections.py`, `equipe-atelier.sh` ; la règle (c) du juge et `Tour3StudioRenameIdleTest` (un autre agent).
