# Tour 9, lot ci-outils : relecture neuve

relu : 6ba7bb4b (sous-agent neuf, après la fusion de l'intégration 4f3f19c2)

Lu : `git diff origin/eq/ci-int-styles 6ba7bb4b` en entier (ci-etat.sh, ci-rouge.sh, test_stable.py, fixtures, rapport, décisions).
Vérifié : `python3 -I scripts/bench/test_stable.py` vert (13 tests) ; les anciens tests de ci-etat.sh dans `test_ultra_bench.py` (`ci_state_reads_every_job`, `ci_state_without_gh`) toujours verts avec le nouveau format (lancés seuls) ; `docs/DECISIONS.md` identique à l'intégration ; aucun fichier audio ou apk, aucun fichier tour-9-gr-styles ajouté ; commits rouge 0f555938 / 74e0841e avant les verts, comme le dit le rapport ; le défaut de `pre-vol.sh:113` (espace au lieu de tabulation) est réel.

Constats (aucun bloquant, aucun grave) :
- Mineur (test) : `test_a_zip_reaching_out_of_its_folder_is_refused` vérifie « rien d'écrit » dans le dossier temporaire de Python, alors que `../../` partirait du `mktemp -d` du script (sous /tmp) : cette assertion ne prouve rien. Le refus, lui, est prouvé par le message de la garde et le code 2 (la garde passe avant `extractall`).
- Mineur (lecture) : le verdict suit la conclusion du run, ci_build les jobs ; un run rouge aux jobs verts (cas de `ci_state_without_gh`) donne « verdict rouge » et « ci_build vert ». Cohérent avec la décision, à savoir en lisant.
- Mineur (rapport) : l'en-tête cite encore l'intégration ccr-80af1a1b-mn0wec (tête d87dea73) ; la base est maintenant 4f3f19c2. « Essayé en vrai (réseau) » n'a pas de trace jointe : c'est un essai déclaré, pas une preuve rejouable.
- Mineur : le chemin curl de ci-etat.sh lit une seule page de 20 runs par commit ; sans effet en pratique.

Verdict : 0 bloquant, 0 grave
