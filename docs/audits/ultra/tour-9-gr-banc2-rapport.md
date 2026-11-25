# Tour 9, grand remplacement v2, lot L4 (gr-banc) : rapport

Banc « corps » du son : ps_measure mesure les pistes qui sonnent ensemble au Drop/Refrain (--track ch:wav), la part du temps où l'aigu (> 6 kHz) est occupé, le fond entre deux coups, et chaque changement de section (saut en LU, saut au téléphone, bande qui bouge, brutal). report.py les liste (critères listés, pas bloquants) et ajoute le tableau « Changements de section ». song-bench rend chaque piste seule (--no-tracks pour s'en passer).

- rouge : ed655f82, 3e008d11, 29abdc89 ; vert : 6396e3e7, d283c01b, a4e3c49a ; song-bench d2c4d32d ; doc dc87eaa3 ; relecture : dernier commit.
- Avant (Drop − fin de Montée au téléphone, dB) : techno −0,95 / −0,52 ; house −1,52 / −0,57 ; trap et boom bap : pas de Montée.
- Corps avant : 4 pistes ensemble partout ; fond boom bap −9,7 (rouge), trap −4,0, techno −4,4, house −10,1 ; brutal : techno et house Drop→Intro (fin du morceau).
- Seuils : fond ≥ −8,5 dB (chansons) rouge sur boom bap S7, vert sur une référence hip-hop CC0 (−7,5) ; pistes ≥ 5 sans référence à pistes ; aigu sans cible (les références ne se séparent pas de nos morceaux).
- host-tests verts ; aucun audio commité ; moteur temps réel non touché (live-bench non relancé).

tests: aucun : banc hors appli, aucun code de l'appli touché (tests du lot : python3 -I scripts/bench/test_ps_measure_body.py, python3 -I scripts/bench/test_report.py, tous verts)

chemin: banc song-bench → export des styles lu par ps_measure | test: SongBenchExport#recordsTheExportOfEveryStyle

Limite : test_ps_measure_body.py n'est lancé ni par le pré-vol ni par la CI (hors liste). Statut : tenté.
