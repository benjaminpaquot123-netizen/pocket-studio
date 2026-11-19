code testé : 2b18313c4
joué par : sous-agent neuf
siege: controle/t9-73/a
profil: curieux | lang: en | device: phone360 | font: 130 | mode: simple
mission: tuto-facile, chapitres 1 à 7 du tuto Trap | buts: 7/7 chapitres finis (« Done ») en 95 gestes sur 150
outils hors human-act: lecture (cat/tail/ls, Read d'images) des seuls fichiers du dossier du siège
promesse ch1 « The song's speed and colour » : compris du premier coup
promesse ch2 « The kick » : hésitation (consigne « Your turn » cachée derrière « Read on » après l'écoute)
promesse ch3 « The clap » : compris du premier coup
promesse ch4 « The hi-hats » : hésitation (grille restée sur la mesure 2, cases 13 et 15 introuvables)
promesse ch5 « The 808 » : hésitation (bouton « Copy to the next bars » du tuto sans effet, contourné par « Copy »)
promesse ch6 « The chords » : hésitation (même bouton sans effet au premier appui)
promesse ch7 « The melody » : compris du premier coup (bannière contradictoire sur le nombre de notes)
hesitations:
- #37-39 : « bar 1, hold cells 13 and 15 » alors que la grille montre Bar 2/2 et que le choix de mesure est sorti de l'écran.
- #64 et #74 : j'appuie « Copy to the next bars » dans le tuto, rien ne bouge ; je cherche un autre bouton.
- #15-16 : après l'écoute, « Example skipped without listening. » et la consigne est cachée.
abandon: aucun
incidents-banc: aucun
defauts:
1. geste #64 (revu #74) | ecran.vu 65 / 75 | cible « Copy to the next bars » | geste.type touche | attendu: le motif est copié sur les mesures suivantes et l'étape passe | obtenu: rien ne change, l'étape reste la même ; il faut passer par le bouton « Copy » de l'éditeur (ch5) ou replier le tuto et rappuyer (ch6) | symptome rien | gravite contourne-en-1-geste | revu oui (ch5 replié, ch6 déplié ; marche à #76 et #94) | banc? non | promesse ch5, ch6
2. geste #37 | ecran.vu 38 | cible « Your turn: bar 1, hold cells 13 and 15 of the “Trap · Hi-hat” » | geste.type regarde | attendu: la grille montre la mesure 1 et les cases 13 et 15 marquées | obtenu: la grille reste sur Bar 2/2, le sélecteur de mesure est hors de l'écran, les cases ne sont ni numérotées ni entourées | symptome sans-aide | gravite contourne-en-2-gestes | revu non | banc? non | promesse ch4
3. geste #15 | ecran.vu 16 | cible « Example skipped without listening. » | geste.type touche | attendu: après avoir écouté presque tout l'exemple, la consigne « Your turn » s'affiche | obtenu: message « skipped without listening » et consigne cachée derrière « Read on » | symptome texte-faux / cache | gravite contourne-en-1-geste | revu non | banc? non | promesse ch2
4. geste #28 | ecran.vu 27 (aussi 31) | cible « Tap a cell to add a hit… » | geste.type regarde | attendu: lire l'aide de la grille en entier | obtenu: l'aide est coupée par le panneau du tuto, le nom « Trap · Handclap » est coupé sous « Part: A » | symptome texte-coupe | gravite finition | revu oui | banc? non | promesse ch2, ch3
5. geste #26 | ecran.vu 27 → 32 | cible « Step 1/4 » | geste.type regarde | attendu: le compteur d'étape avance quand la consigne change | obtenu: la consigne passe de « Switch to 2 bars » à « tap the dotted cells » mais reste « Step 1/4 » (pareil ch7 « Step 2/3 » de #82 à #83) | symptome texte-faux | gravite finition | revu oui | banc? non | promesse ch4, ch7
6. geste #41 | ecran.vu 42 | cible « 3 over 2 cells » | geste.type touche | attendu: faire les cases 13 et 15 comme demandé | obtenu: « Passed » dès la case 13 seule | symptome texte-faux | gravite finition | revu non | banc? non | promesse ch4
7. geste #57 | ecran.vu 58 / 85 / 87 | cible « 4 more dotted notes, further on → Tap here » | geste.type touche | attendu: la bannière compte les notes de l'étape (3 ou 4) | obtenu: « Passed » affiché en même temps que « 4 more dotted notes, further on » ; ch7 « 5 dotted notes left, higher up » alors que la consigne dit 3 notes | symptome texte-faux | gravite finition | revu oui | banc? non | promesse ch5, ch7
8. geste #59 | ecran.vu 59 | cible « ↑ 3 more dotted n… » | geste.type regarde | attendu: lire combien de notes et où | obtenu: texte coupé, flèche ↑ alors que les notes sont visibles ; « Pattern placed! Copy it to the rest: » aussi coupé (#64) | symptome texte-coupe | gravite finition | revu oui | banc? non | promesse ch5
9. geste #54 | ecran.vu 54 / 79 | cible « Acid 303 Bass line » | geste.type regarde | attendu: voir seulement les pistes du tuto | obtenu: une piste « Acid 303 Bass line » non expliquée, et « Chords » + « Ghost bass » tous deux marqués sélectionnés | symptome incompris | gravite finition | revu oui | banc? non | promesse ch5
10. geste #72 | ecran.vu 72 / 83 | cible grille du clavier | geste.type touche | attendu: une grille assez haute pour viser les notes | obtenu: le panneau du tuto ne laisse qu'une bande d'environ 100 px, notes très fines, il faut « Tap here » pour les trouver | symptome hors-pouce | gravite finition | revu oui | banc? non | promesse ch6, ch7
mots-faux: « Example skipped without listening. » (écouté presque en entier)

fin-tranche: oui (ROUTE ligne 522)
oracle: step=groove_hear absent du journal ; fin de tranche lue à step=lead_2 done=18 chapters=1,2,3,4,5,6,7
BENCH-VERDICT: #95 BENCH-VERDICT ok where=seat.fin
