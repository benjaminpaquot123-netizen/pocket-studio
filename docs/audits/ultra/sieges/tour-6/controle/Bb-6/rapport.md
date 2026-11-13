code testé : a5f29494
joué par : sous-agent neuf
siege: b | profil: t6-Bb producteur-allemand-tablette-8 | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro (contrôle des mots du tuto) | buts: chap1, chap3, bravo
promesse: id=t6-Bb-1 | texte: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (7 %; 4 bis 10 % zählen) | ecran: 20 | tenue: oui | pourquoi: curseur à 4 % → « Bravo, Kapitel geschafft! » tout de suite
promesse: id=t6-Bb-2 | texte: - | ecran: - | tenue: pas vue | pourquoi: la liste des sons n'a pas été ouverte (run centré sur les mots du tuto)
promesse: id=t6-Bb-3 | texte: Neue Kurve: zeichne sie unten. | ecran: 236 | tenue: oui | pourquoi: les deux rampes (Linie) apparaissent aussitôt en barres, pic au milieu du takt
promesse: id=t6-Bb-4 | texte: Einstellung zeigen | ecran: 231 | tenue: pas vue | pourquoi: bouton vu, pas touché
oeil-neuf: q1 | ecran: 259 | image: 259 | touchables nets : Song-Datei erstellen, Teilen, Meinen Song anhören, Tutorial neu starten/schließen, barre du bas ; « Erweitert » coupé en haut se lit à peine comme un bouton
oeil-neuf: q6 | ecran: 259 | image: 259 | preuve claire : « Gespeichert in Musik › BenStudio », « ✓ Bereit zum Teilen », « Bravo, dein Track ist fertig! » ; suite proposée (anhören, neu starten, schließen)
oeil-neuf: q9 | ecran: 259 | image: 259 | rien de coupé à 130 % sauf le titre « Erweitert » rogné sous la barre du haut (scroll)
hesitations:
  - ecran: 37 | cherchais: le menu « Sanft » du ghost-clap | regarde: « Drück lange darauf » : 1er appui long sur la case vide pose juste un coup à 100 %, il faut un 2e appui long (#36 puis #37)
  - ecran: 113 | cherchais: « Sound der Spur einstellen » sous la piano roll | regarde: très loin (clavier, import, gammes, arpège…) ; scrolls bloqués sur la rangée d'accords/clavier ; trouvé via « Zeig's mir » (#113)
  - ecran: 248 | cherchais: « Club: −7,5 LUFS » | regarde: le bouton dit « Club · −8 LUFS » et l'export dit −8,0
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun ; outils hors human-act.py : Read sur mes propres PNG et lecture de journal.txt/rapport.md de mon siège seulement ; 3 appels groupés de gestes (écrans 41-43) dans un seul appel Bash
defauts:
chapitre: 1 Tempo und Tonart | ecran: 6 | compris du premier coup | tempo 132, F Phrygisch, swing 4 % validé (4–10 % annoncé = vrai)
chapitre: 2 Die Kick und das Grollen | ecran: 21 | compris du premier coup | pads „Studio-Kick“, „Rumble“ = noms des pads (« Studio-Kick FX », « Rumble FX »)
chapitre: 3 Der Clap | ecran: 34 | hésitation : « Drück lange darauf, dann „Sanft“ » | sur case vide l'appui long pose un coup sans menu ; 2e appui long ouvre le menu
chapitre: 4 Hi-Hats und Percussion | ecran: 40 | compris du premier coup | « Offbeats (zwischen zwei Zählzeiten, genau zwischen den Kicks) » dans la consigne ; Offene Hi-Hat, Hi-Hat, Rim, Becken = pads
chapitre: 5 Der Bass | ecran: 77 | hésitation : « Unter der Piano Roll, „Sound der Spur einstellen“ » | réglage très bas dans la page, trouvé avec « Zeig's mir » ; Sound „Plaits“ = piste « Bass Plaits »
chapitre: 6 Der Stab (Akkorde) | ecran: 118 | compris du premier coup | consigne « Sound „Synth“, ein kurzer Pluck » ; la piste s'appelle « Akkorde Pluck »
chapitre: 7 Das Riff | ecran: 138 | hésitation : « Dreh unter „Sound der Spur einstellen“ » | 6 glissements pour l'atteindre ; piste « Melodie Gefilterter Lead » puis « Melodie Plaits »
chapitre: 8 Dein erster Teil: der Groove, deine Grundschleife | ecran: 164 | compris du premier coup | « Groove » expliqué (« das Wort der Musiker für einen Loop… »)
chapitre: 9 Die Teile verstehen | ecran: 171 | compris du premier coup | écoutes + carte des parties
chapitre: 10 Bau deine Teile | ecran: 184 | compris du premier coup | 7 boutons « Teil … erstellen »
chapitre: 11 Ordne deine Teile | ecran: 198 | compris du premier coup | ajouts dans l'ordre + « Empfohlene Längen setzen »
chapitre: 12 Die Übergänge: von einem Teil zum nächsten | ecran: 208 | compris du premier coup | roll ×4 sur Clap ; étape 2 ne nomme pas le pad (grille Hi-Hat ouverte seule, croix visibles)
chapitre: 13 Die Effekte | ecran: 216 | compris du premier coup | Clap → Pad-Effekte → Reverb-Send trouvé sans « Ich mach's » ; « bis zur Mitte » = pic dessiné au milieu
chapitre: 14 Der Mix | ecran: 238 | compris du premier coup | « um etwa 2 dB runter » : chiffre donné, validé à −3,2 / −2,3 dB
chapitre: 15 Das Mastering | ecran: 248 | hésitation : « „Club“: −7,5 LUFS » | OTT et LUFS expliqués dans la consigne, mais le bouton dit −8 LUFS
chapitre: 16 Teile deinen Track | ecran: 253 | compris du premier coup | Solo expliqué (« Keine Spur ist auf Solo geblieben und spielt allein ») ; export M4A Club
bravo: oui | ecran: 259
defauts-avant:
  - M1: corrigé (écrans 24, 29, 34, 40, 45, 58, 63, 216) ; seule l'étape 2 du chap. 12 ne nomme aucun pad (écran 212)
  - M2: autre (« Groove » n'apparaît pas au chap. 1 ; 1re apparition au titre du chap. 8, écran 163, expliqué écran 164) ; Offbeats corrigé (écran 40) ; Solo corrigé (écran 254)
  - M3: corrigé (écran 19 : « 4 bis 10 % zählen », validé à 4 %, écran 20)
  - M4: autre (« Analoger Bass » absent : « Plaits » = « Bass Plaits », écran 80 ; mais chap. 6 « Sound „Synth“ » vs piste « Akkorde Pluck », écran 119) ; « Mitte » corrigé (écran 227 « bis zur Mitte steigt », courbe écran 236)
  - M5: corrigé pour le mix (2 dB, écran 238) et OTT/LUFS (écran 248) ; autre pour Club : « −7,5 LUFS » dans la consigne vs « Club · −8 LUFS » sur le bouton (écran 250)
  - M6: corrigé (écran 224 : Pad-Effekte du Clap → « Reverb-Send », sans « Ich mach's »)
