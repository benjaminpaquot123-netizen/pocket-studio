# Prompt pour Fable 5.1 (ultracode) : 2e audit de la méthode, version allégée

À coller tel quel dans la session, sur la branche `ccr-80af1a1b-mn0wec`, quand Benjamin a du quota.

---

Tu audites **la méthode d'audit**, pas l'application. Le 1er méta-audit (`docs/audits/ultra/META-AUDIT.md`) a donné la méthode v2 (`docs/audits/AUDIT-ULTRA.md` §12), appliquée puis jouée une fois : le tour 5 (24 testeurs). Ton travail : dire ce que la v2 a vraiment changé, ce qui manque encore, et les **5 changements** les plus rentables pour le tour 6.

## À lire (et seulement ça)
- `docs/audits/ultra/tour-5-synthese.md` et `docs/audits/ultra/tour-5-plus-value-fable.md` (déjà contre-vérifiés) ;
- `docs/audits/ultra/sieges/tour-5/` : `sieges.tsv`, `clusters-complet.md`, `tableau-de-bord.md`, et les `rapport.md` / `mesures.json` seulement quand une affirmation l'exige ;
- `docs/audits/AUDIT-ULTRA.md` §12, `docs/audits/ultra/verdicts.tsv`, `docs/audits/ultra/base.tsv` ;
- les scripts `scripts/bench/{fuse-reports.py,tableau-de-bord.py,selftest.sh,seat-metrics.py}` ;
- `docs/DECISIONS.md` (lignes du 2026-11-05 et du 2026-11-06).
Ne relis pas les tours 0 à 4 : le 1er méta-audit l'a fait.

## Quatre questions
1. **Mesure** : le tableau de bord et le critère d'arrêt sont-ils calculables maintenant ? Qu'est-ce qui reste « ? » (G, H, I), et comment le remplir sans effort humain ?
2. **Fusion et « revient »** : pourquoi 0 « revient » au tour 5 alors qu'une quarantaine de défauts étaient déjà au tour 4 ? Proposer comment remplir `base.tsv` à partir des tours 1 à 4, et comment relier les promesses aux 120 ids.
3. **Banc** : incidents du tour 5 (raccourcis partagés entre testeurs, test rouge dépendant de l'ordre, séparation lente, préalables P1 à P6 encore absents). Lesquels réparer avant le tour 6, dans quel ordre ?
4. **Corrections** : l'organisation v2 (une branche par équipe, rejeux, relecture avant fusion) n'a pas encore servi. Avant qu'elle serve, qu'est-ce qui risque de casser ?

## Règles (allégées)
- Un analyste par question (4), puis **2 agents sceptiques** en tout pour les affirmations chiffrées, puis un rédacteur. Pas plus de 8 agents. Pas de Gradle, lecture seule sauf ton rapport.
- Rapport : `docs/audits/ultra/META-AUDIT-2.md`, 150 lignes au plus, en français simple, chaque affirmation avec sa preuve (fichier, siège, geste).
- Termine par **les 5 changements** (fichier ou script à modifier, gain attendu, coût), puis un résumé de 8 lignes pour Benjamin, sans jargon.
- Commit par chemin uniquement (jamais `git add -A`), message en anglais, push sur `ccr-80af1a1b-mn0wec`.
