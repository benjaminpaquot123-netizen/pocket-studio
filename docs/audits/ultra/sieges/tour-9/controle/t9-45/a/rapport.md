code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : personne (amenée refusée par le banc, aucun sous-agent lancé)

# Siège t9-45 : bloqué à l'amenée (lot de banc, pas un défaut de l'appli)

Amenée demandée : `tuto:liquid_dnb:12` (nl, phone360, police 100, Pro, confirmé).
Le banc a construit les chapitres 1 à 11, touché `tuto_chapters`, puis attendu la liste des chapitres 124 s sans la voir (écran resté sur `tuto_panel/tab_Sons`). `HumanTesterA > drive FAILED` (IllegalStateException, HumanTester.kt:901).

```
#0 BENCH-FLAG where=amenee.wait want="les chapitres" saw="après touche tuto_chapters@391,336" waited=124039 screen=tuto_panel/tab_Sons
#0 BENCH-VERDICT fail where=amenee.wait why="amenée tuto:liquid_dnb:12 : les chapitres n'est jamais venu"
```

Self-test : vert (324 verts, 0 rouges, 0 en quarantaine, 102 sautés).
Défauts de l'appli : aucun relevé (siège non joué).

fin-tranche: non (dernier step=amenée, done=0, chapters=aucune ligne ROUTE)
