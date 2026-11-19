code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : aucun (siège non joué : amenée refusée par le banc, aucun sous-agent lancé)

# Siège t9-57 : reggaeton Facile, chapitres 8-15, tr, phone360, police 100, débutant motivé

Statut : bloqué au banc (amenée), pas un défaut de l'appli.
Self-test : vert (324 verts, 0 rouges, 0 en quarantaine, 102 sautés).

Lignes du banc, telles quelles :
#0 BENCH-FLAG where=amenee.wait want="les chapitres" saw="après touche tuto_chapters@398,336" waited=123445 screen=tuto_panel/tab_simple_Rythme
#0 BENCH-VERDICT fail where=amenee.wait why="amenée tuto:reggaeton:8 : les chapitres n'est jamais venu"

Note : l'amenée a touché `tuto_chapters` à (398,492), le flag rapporte (398,336) ; la liste des chapitres n'est pas apparue en 123 s, écran resté sur tuto_panel/tab_simple_Rythme.

Défauts : 0 (aucun geste joué).
fin-tranche: non (dernier step=amenée, done=-, chapters=-)
