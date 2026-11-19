code testé : b49ce1e0096c95db1d73f3b345b8a862efaf27ec
joué par : sous-agent neuf

siege: a | profil: jonas-musicien-de-impatient | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: -
promesse: Kapitel 13 Schritt 2 verlangt „Pro Teil“ und eine 1-Takt-Loop-Kurve im Drop | tenue: non (après « Ich mach's », le mode sélectionné est « Song », pas « Pro Teil », et aucune courbe n'est visible) | ecran: 56
promesse: Kapitel 12 Schritt 3 « Noch 1 gestricheltes Kästchen hier » | tenue: oui (une seule case pointillée visible, un toucher → « Bravo, Kapitel geschafft! ») | ecran: 49
promesse: Kapitel 11 « Gib die Längen ein » (Break 12) | tenue: non vérifié (le +/− manuel avance par 8 ; j'ai pris « Empfohlene Längen setzen ») | ecran: 38
oeil-neuf q1: les boutons sont clairs (Loop / Pro Teil / Song, Zeichnen / Linie / Bereich), mais la zone de dessin de la courbe n'est pas visible : seulement les graduations « 18,0 kHz / 849 Hz / 40 Hz » et les numéros de Takt.
oeil-neuf q6: la preuve affichée est « Bravo, Kapitel geschafft! », mais aucune courbe n'est vue, et le mode est « Song » alors que l'étape demandait « Pro Teil ».
oeil-neuf q9: la ligne « Teil · B · Intro · C · Intro 2 · D · Drop » et les icônes au-dessus sont [coupé] par le panneau du tuto (y≈1030) ; « Einstellung zeigen » est [coupé] en haut.
hesitations: écran 31 (les boutons d'ajout de Teil remontent de y=246 à y=198 après le premier ajout, il faut viser à nouveau) ; écran 38 (Break 12 Takte impossible avec le +/− par 8, j'ai pris le bouton des longueurs recommandées) ; écran 55 (consigne longue « Pro Teil » + courbe, j'ai pris « Ich mach's »)
abandon: jamais
incidents-banc: outil Read sur les images grille du siège (screen-6, 41, 44, 53, 56) ; cat des deux fichiers de consigne ; grep/sed/tail pour filtrer la sortie de human-act.py. Aucun blocage du banc.

defauts:
- geste: 4 | ecran.vu: 4 | cible: « Weiter » (Kapitel 8 Schritt 1) | geste.type: touche | attendu: passer à l'étape suivante après avoir écouté l'exemple | obtenu: « Beispiel übersprungen, ohne es anzuhören. » alors que j'avais lancé « Beispiel anhören » (écran 3 « Anhören stoppen ») ; le message se répète à l'écran 5 | symptome: texte-faux | gravite: finition | revu: non | banc?: temps (l'écoute a peut-être été comptée comme interrompue) | promesse: -
- geste: 25 | ecran.vu: 25 | cible: « ⊕ Teil „Build-up“ erstellen » | geste.type: touche | attendu: même explication que l'étape (« die Stille auf dem letzten Schlag kommt mit den Übergängen ») | obtenu: « sein letzter Schlag verstummt » (dit tout de suite que c'est fait) | symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
- geste: 31 | ecran.vu: 31 | cible: boutons d'ajout de Teil | geste.type: touche | attendu: boutons à la même place | obtenu: les boutons remontent de y=246 à y=198 après le premier ajout | symptome: saut | gravite: finition | revu: non | banc?: non | promesse: -
- geste: 38 | ecran.vu: 38 | cible: longueur « Break » (+/−) | geste.type: touche | attendu: pouvoir saisir 12 Takte comme demandé | obtenu: le +/− ne propose que des répétitions ×1 de 8 Takte ; seul « Empfohlene Längen setzen » donne le total de 128 | symptome: impossible | gravite: gênant | revu: non | banc?: non | promesse: « Gib die Längen ein »
- geste: 53 | ecran.vu: 53 | cible: « Ich mach's » (Kapitel 13 Schritt 1) | geste.type: touche | attendu: fiche du Clap, étiquettes claires | obtenu: fiche « Effekte · Pad 5 (Clap) » avec Reverb-Send 30 % dans un cadre pointillé étiqueté « Akkorde ✓ · Snare: danach », et la grille derrière montre le pad Becken | symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
- geste: 56 | ecran.vu: 56 | cible: « Ich mach's » (Kapitel 13 Schritt 2) | geste.type: touche | attendu: mode « Pro Teil », une courbe de Loop d'un Takt dans le Drop, dessinée | obtenu: KURVEN « Melodie · Filter Cutoff 18,0 kHz », mode « Song » sélectionné (« Ein Wert pro Takt über den ganzen Song (128 Takte) »), aucune courbe visible (image 56, puis écran 57 après défilement), chapitre quand même « Erledigt » | symptome: promesse-non-tenue | gravite: grave | revu: oui (écran 57) | banc?: non | promesse: Kapitel 13 Schritt 2
- geste: 44 | ecran.vu: 44 | cible: croix rouges de la ligne Hi-Hat | geste.type: touche | attendu: ligne entièrement visible | obtenu: le panneau du tuto recouvre le bas de la ligne ; les croix sont en partie cachées mais restent touchables | symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
- geste: 7 | ecran.vu: 7 | cible: étiquettes de la courbe Energie | geste.type: regarde | attendu: étiquettes lisibles | obtenu: étiquettes [coupé] | symptome: coupé | gravite: finition | revu: non | banc?: non | promesse: -

mots-faux: « Beispiel übersprungen, ohne es anzuhören. » (écran 4, alors que l'exemple avait été écouté) ; « Akkorde ✓ · Snare: danach » sur la fiche du Clap (écran 53)

chapitre: 8 | compris du premier coup | ecran: 5
chapitre: 9 | compris du premier coup | ecran: 15
chapitre: 10 | compris du premier coup | ecran: 29
chapitre: 11 | hésitation | ecran: 39
chapitre: 12 | compris du premier coup | ecran: 50
chapitre: 13 | hésitation | ecran: 56

defauts-avant:
e-ch13 | encore là | ecran: 56 | Étape 1 correcte : « Ich mach's » ouvre la fiche du Clap, le Reverb-Send passe à 30 % et l'étape est cochée (écrans 53-54). Étape 2 : « Ich mach's » coche le chapitre (« Bravo, Kapitel geschafft! »), mais le mode reste « Song » au lieu de « Pro Teil » et aucune courbe n'est visible, même après défilement (écran 57). La partie « la courbe est dessinée » de la phrase d'acceptation n'est pas vue.
e-ch12 | corrigé | ecran: 43 | « Zum Teil „Build-up“ gehen » ouvre l'éditeur sur Clap, Takt 4/4, onglet « Schläge », qui correspond à l'appui long demandé. Un appui long sur le pas 12 puis « ×4 » posent un seul roll et cochent l'étape. Il faut donc un appui long plus un choix, pas un seul toucher.
compteur | autre | ecran: 49 | Aucun compteur de rolls n'est affiché pendant l'étape du roll (écran 41 : ni case pointillée ni compteur). Le compteur de l'étape 3, « Noch 1 gestricheltes Kästchen hier », correspond bien à la seule case pointillée visible.
c-croix | corrigé | ecran: 44 | Dans la vue d'un pad (Hi-Hat), trois croix rouges dans des cases en tirets sont visibles (« durchgestrichen: wegnehmen ») ; trois touchers donnent « Bravo, geschafft! ». La vue « Alle Pads » n'a pas été vérifiée.

chapitres bloqués: 0
