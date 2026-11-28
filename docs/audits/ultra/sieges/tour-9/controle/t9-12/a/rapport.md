code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: curieux | lang: en | device: phone | font: 100 | mode: simple
mission: tuto amapiano chapitres 6-12 (tuto-facile) | buts: - (aucun id donné par le brief)
promesse: id=- | texte: Tap a cell to add a hit, slide your finger to paint several. | ecran: 29 | tenue: oui | pourquoi: one tap on an empty cell gave « hit, strength 100 % »
promesse: id=- | texte: Your turn: in “My song”, under “Add”, tap in order Intro, Groove, Drop, Break, Drop 2 and End. | ecran: 43 | tenue: oui | pourquoi: « Still to add » list shrank at each tap, then « Passed »
promesse: id=- | texte: Ready: My-song-20261119-074350.m4a, a copy is also in Music › BenStudio. | ecran: 61 | tenue: pas vue | pourquoi: I did not open My files
chapitres:
  - 6 Your first part: the Drop : compris du premier coup
  - 7 Understanding parts : hésitation (« Example skipped without listening » after I had listened)
  - 8 Build your parts : hésitation (step 4 asks for conga or “tock”, the app opened « Drum rim »; I guessed it was the same)
  - 9 Arrange your parts : compris du premier coup
  - 10 Effects : compris du premier coup
  - 11 The mix: the volumes : hésitation (sliders half hidden under the tutorial card until « I'll do it »)
  - 12 Export your song : compris du premier coup
hesitations:
  - ecran: 28 | cherchais: the conga / “tock” sound the step asks for | regarde: the app opened « STEPS: DRUM RIM · DROP 2 », no word says rim = tock
  - ecran: 53 | cherchais: the « Chords » slider | regarde: « curseur [coupé] » under the tutorial card, I tapped « I'll do it » to see it
  - ecran: 36 | cherchais: what « Add: + A · Drop » means next to « + New part » | regarde: the chapter text, then understood it adds to the timeline
abandon: jamais
incidents-banc:
  - geste: 63 | attente: 0 | vu: « Almost done! 5 steps left » lists steps of chapters 1-3 (and chapter list shows 1, 2, 3, 5 not ticked): these chapters were built by the bench's set-up, not played by me; not a defect
  - geste: - | attente: 0 | vu: outils hors human-act : lecture des images screen-N-grille.png de mon siège, grep/sed sur la sortie de human-act et sur journal.txt/start.txt de mon siège ; un BrokenPipe Python au geste 65 venait de mon propre « | head »
defauts:
  - geste: 13 | ecran.vu: 16 | cible: Example skipped without listening. | geste.type: touche
    attendu: after listening to the Drop example to the end, no « skipped » reproach on the « Done » card | obtenu: the « Example skipped without listening. » line from step 2 stays on step 3 and on the chapter's « Done » card
    symptome: texte-faux | gravite: finition | revu: oui: ecran 5 (same line after I had started then stopped the example, geste 3-4) | banc?: temps | promesse: -
  - geste: 49 | ecran.vu: 50 | cible: bouton ▶ «  Listen to the example » @ 453,975 | geste.type: touche
    attendu: my finger on « Next → » goes to the next step | obtenu: the example had just ended, the card rebuilt itself and « Next » became « Listen to the example », which restarted the example
    symptome: mauvais-ecran | gravite: contourne-en-1-gestes | revu: oui: ecran 16 (geste 15, same jump: my « Next » hit « Stop listening ») | banc?: temps | promesse: -
  - geste: 16 | ecran.vu: 17 | cible: texte « The timeline puts your parts in order, each with its length in bars. Add them in order. » [coupé] | geste.type: touche
    attendu: to read the explanation of the timeline | obtenu: the sentence is half hidden under the « 112 Tempo » bar, only « length in bars. Add them in order. » shows, cut
    symptome: cache | gravite: finition | revu: oui: ecran 19 | banc?: non | promesse: -
  - geste: 6 | ecran.vu: 7 | cible: texte « Intro » @ 48,597 | geste.type: touche
    attendu: the part diagram readable | obtenu: first box reads « ntro » (cut on the left), « Energy » title runs into the curve, the Break box shows only « 8 » with no name
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 13 | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: Your turn: in Drop 2, add a few conga or “tock” hits. | geste.type: touche
    attendu: to land on a sound called conga or tock | obtenu: the app opened « STEPS: DRUM RIM · DROP 2 »; the step passed, but nothing told me the rim is the “tock”
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 52 | ecran.vu: 53 | cible: curseur [coupé, état « Normal », à 80 %] @ 216,486 | geste.type: touche
    attendu: the Mix tab shows the Chords slider the step asks me to lower | obtenu: the sliders are cut by the tutorial card; I had to tap « I'll do it » to fold it
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 63 | cible: bouton ← «  Back to the step » [coupé] @ 294,1012 | geste.type: touche
    attendu: to see the way back to my step | obtenu: « Back to the step » is cut by the bottom bar, only its top edge shows
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 64 | banc?: non | promesse: -
  - geste: 63 | ecran.vu: 64 | cible: … and 1 more (list ≡) | geste.type: touche
    attendu: to see the 5th missing step | obtenu: nothing happens, it is plain text; I had to find « Chapters » myself
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -

fin-tranche: non (dernier step=share, done=29)
