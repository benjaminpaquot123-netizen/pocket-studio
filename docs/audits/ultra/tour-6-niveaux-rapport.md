# Tour 6, lot « niveaux » : la règle des niveaux dans les règles de l'Audit (2026-11-12)

Consigne de Benjamin : « le plan d'audit n'est pas optimisé pour tester comme de vrais utilisateurs : débutant, moyen, pro », puis « que la règle générale de l'audit soit intégrée ».
Branche : `ccr-6d09db19-wvcz09`, arbre de `ccr-80af1a1b-mn0wec` (496dbdc5). Décisions : `docs/decisions/tour-6-niveaux.md`.

tests: aucun : lot de documents seulement, aucun code, aucun script du banc, aucun écran

## Ce que le lot change
| Fichier | Changement |
|---|---|
| `docs/audits/ultra/PLAN-NIVEAUX.md` | neuf : le constat compté, l'échelle à cinq niveaux, les seize sièges, les huit missions à ajouter au banc, trois règles de consignes, le tableau par niveau, ce que le banc ne saura pas |
| `docs/audits/AUDIT.md` | principe 9 « Les niveaux » |
| `CLAUDE.md` | une ligne « Audits, règle des niveaux » |
| `docs/audits/ultra/PLAN-RESTE.md` | § 6 : le lot du banc « niveaux », après le tour 6 |

## Ce que le lot ne change pas
- Le tour 6 : plan, sièges, consignes, règle de sortie.
- Le banc : `fabrique-sieges.py`, `human-act.py`, consignes v2, selftest. Les missions neuves n'existent pas encore.

## D'où viennent les chiffres du constat
`PLAN-NIVEAUX.md` § 1 : comptés dans `docs/audits/ultra/sieges/tour-3` à `tour-6`, fichier `sieges.tsv`, colonnes 6 (`mode`) et 8 (`mission`), lignes de commentaire et d'en-tête retirées. Les tours 0 à 2 n'ont pas ce fichier. Les rapports des sièges n'ont pas été relus.

## Reste à faire, hors de ce lot
- Fusion par le portail (la discussion principale).
- Le lot du banc « niveaux » (`PLAN-RESTE.md` § 6), après le tour 6.
- Raccorder la réponse « 1A » de Benjamin (un passage « Nightly real use » à la fin de l'audit) à `PLAN-TOUR-6.md` § 2 point 7.
