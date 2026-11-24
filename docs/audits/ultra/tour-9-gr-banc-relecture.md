# Tour 9, lot gr-banc (L4) : relecture neuve

Relecteur : sous-agent neuf (n'a pas écrit le code), diff b0ac8fe3..eq/gr-banc.

relu : a91152f6b

Rien de grave. ps_measure recompilé, test_ps_measure_body.py et test_report.py verts ; cas limites essayés (sans .pscmd, piste de 0,1 s sous un mix de 6 s, mix muet, section vide) : JSON strict (null, jamais nan), aucun plantage. Guillemets awk dans le sh -c de song-bench justes ; report.py lit un vieux JSON sans les nouvelles clés.

Mineurs :
1. ps_measure : une piste plus courte que le mix compte comme entendue sur la mesure qu'elle ne couvre qu'en partie (sans effet dans song-bench : même longueur de rendu).
2. song-bench : les WAV par piste d'un morceau restent sur le disque jusqu'à ps_measure, dans chaque job parallèle (~70 Mo par piste, 3 min) : plusieurs Go avec 8 jobs ; --no-tracks pour s'en passer.
3. song-bench : sur un échec de rendu, les .track*.wav restent (comme lead/bass avant).
4. ps_measure : une section de longueur nulle donne une transition à son nom, mesurée sur la suivante.
5. hf_occupancy vaut 0 (pas null) sur une section principale muette.
6. Tests : pas de cas « montée > 6 LU hors Drop » ; l'exemption Drop repose sur +6,02 LU.

Verdict : 0 bloquant, 0 grave

Complément (chef, 2026-11-24) : depuis dc87eaa3, seul le rapport de zone a été ajouté (doc, aucun code) ; relu, conforme.
