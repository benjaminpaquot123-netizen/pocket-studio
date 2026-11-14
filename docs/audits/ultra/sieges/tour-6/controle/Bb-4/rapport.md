code testé : e4286b40
joué par : sous-agent neuf
siege: b | profil: t6-Bb producteur-allemand-tablette | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t6-Bb-1 | texte: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (7 %) | ecran: 20 | tenue: oui | pourquoi: le curseur Swing est sous la carte, le glisser valide l'étape (Bravo, Kapitel geschafft)
promesse: id=t6-Bb-2 | texte: Tippe bei einem Sound auf ☆, um ihn unter „Favoriten“ wiederzufinden. | ecran: 241 | tenue: oui | pourquoi: ☆ visible à côté de chaque son, un toucher le remplit (★)
promesse: id=t6-Bb-3 | texte: Neue Kurve: zeichne sie unten. | ecran: 207 | tenue: oui | pourquoi: les barres de la courbe apparaissent dès le premier trait
promesse: id=t6-Bb-4 | texte: Einstellung zeigen | ecran: 235 | tenue: oui | pourquoi: ouvre « Effekte · Melodie », Filter, « Eine Kurve bewegt diese Einstellung während der Wiedergabe. »
oeil-neuf: q1 | ecran: 241 | image: 241 | Setzen, ☆, ✕, ▶, onglets et ← se voient touchables ; la tuile du nom (toucher = écouter) ressemble à une simple étiquette
oeil-neuf: q6 | ecran: 241 | image: 241 | preuve = ★ rempli, sans message ; ensuite « Setzen » est évident
oeil-neuf: q9 | ecran: 241 | image: 241 | oui : noms coupés au milieu du mot « Snar e », « Hi-H at », « Cow bell », « Offe ne… », « Tief es… »
chapitre: 1 Tempo und Tonart | ecran: 20 | hésitation | Tonart introuvable à l'écran (il faut « Ich mach's ») ; Swing demandé 7 %, validé à 4 % sans dire pourquoi
chapitre: 2 Die Kick und das Grollen | ecran: 33 | compris du premier coup | cases pointillées + « Noch N … » clairs
chapitre: 3 Der Clap | ecran: 39 | hésitation | « Drück lange darauf, dann Sanft » : le 1er appui long pose un coup à 100 % sans menu, il en faut un 2e
chapitre: 4 Hi-Hats und Percussion | ecran: 76 | hésitation | « leiser unter der Kick » mais rien à régler ; « das Ride » = pad « Becken », à deviner
chapitre: 5 Der Bass | ecran: 106 | hésitation | « Unter der Piano Roll, Sound der Spur einstellen » introuvable sans « Ich mach's » ; la piste s'appelle « Plaits », pas « Analoger Bass »
chapitre: 6 Der Stab (Akkorde) | ecran: 126 | hésitation | « Offbeats (Steps 4 und 11) » : ce ne sont pas des contretemps, j'ai suivi les cases sans comprendre
chapitre: 7 Das Riff | ecran: 149 | compris du premier coup | flèches « weiter hinten/oben » mènent aux notes ; « noch +N » guide les deux réglages
chapitre: 8 Dein erster Teil: der Groove | ecran: 152 | hésitation | un bouton nomme le Teil tout seul : rien tapé, pas compris ce que j'ai fait
chapitre: 9 Die Teile verstehen | ecran: 160 | compris du premier coup | schéma clair, mais légende coupée au 1er écran
chapitre: 10 Bau deine Teile | ecran: 174 | hésitation | « das Tock » : quel pad ? (c'est Rim, vu seulement après coup)
chapitre: 11 Ordne deine Teile | ecran: 184 | compris du premier coup | « Noch: … » liste ce qui manque, « Empfohlene Längen setzen » fait le reste
chapitre: 12 Die Übergänge | ecran: 192 | hésitation | « den letzten Schlag vor der letzten Zählzeit » obscur ; étape 3/3 sautée sans prévenir
chapitre: 13 Die Effekte | ecran: 208 | compris du premier coup | « in Mix » mais « Ich mach's » ouvre les Effekte du pad : ça marche, validation visible seulement après « Schließen »
chapitre: 14 Der Mix | ecran: 216 | compris du premier coup | Akkorde surligné, faders faciles
chapitre: 15 Das Mastering | ecran: 219 | compris du premier coup | « Club » encadré, validé tout de suite
chapitre: 16 Exportiere deinen Track | ecran: 227 | compris du premier coup | Teilen en haut, export puis Drive, Bravo
bravo: oui | ecran: 227
hesitations:
  - ecran: 14 | cherchais: où régler la Tonart (aucun bouton visible) | regarde: « Ich mach's » ouvre l'Tonart-Blatt
  - ecran: 104 | cherchais: combien il manque au Cutoff pendant que le panneau du son est ouvert | regarde: carte repliée, il faut toucher « Lesen » pour voir « noch +N »
  - ecran: 175 | cherchais: le pad « Tock » | regarde: aucun pad ne s'appelle ainsi (c'est Rim)
abandon: jamais
incidents-banc:
  - geste: 102 | attente: 0 | vu: écran 103 en texte vide (seule la barre Feedback) alors que l'image montre le panneau du son ; idem écrans 143, 211, 218, 219
  - geste: 130 | attente: 0 | vu: outil hors human-act : une fois deux commandes du banc dans un même appel (geste + regarde image) ; ls/grep/Read sur mon seul dossier de siège
defauts:
  - geste: 19 | ecran.vu: 20 | cible: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (7 %) | geste.type: glisse
    attendu: validé à 7 % (ou texte « ab 3 % » si c'est la règle) | obtenu: validé à 4 %, on ne sait pas pourquoi ni si 7 % comptait
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Bb-1
  - geste: 36 | ecran.vu: 37 | cible: Du bist dran: Setz den Clap auf 2 und 4 und seinen Ghost-Schlag kurz vor den nächsten Takt: Drück lange darauf, dann „Sanft“. | geste.type: appui-long
    attendu: l'appui long sur la case pointillée ouvre le menu avec « Sanft » | obtenu: pose un coup à 100 % sans menu ; un 2e appui long ouvre le menu
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 103 | ecran.vu: 104 | cible: Cutoff | geste.type: glisse
    attendu: « Cutoff: noch +N » visible pendant le réglage | obtenu: carte repliée sous le panneau, le reste n'apparaît qu'après « Lesen » (même chose écran 144, Melodie)
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: oui: ecran 146 | banc?: non | promesse: -
  - geste: 143 | ecran.vu: 144 | cible: Melodie Plaits | geste.type: glisse
    attendu: la piste garde son son « Gefilterter Lead » après un réglage de Cutoff | obtenu: l'onglet passe à « Melodie Plaits »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 152 | ecran.vu: 153 | cible: Intro | geste.type: touche
    attendu: légende du schéma (Intro, Intro 2, Drop, Break…) lisible | obtenu: légende coupée à mi-hauteur sous « Warum? », « Energie » rogné à gauche
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 191 | ecran.vu: 192 | cible: Die Übergänge Kapitel 12/16 · Erledigt · Pro | geste.type: touche
    attendu: l'étape 3/3 (Becken im Drop) s'affiche avec « Schon erledigt! » | obtenu: saut direct de 2/3 à « Bravo, Kapitel geschafft! », l'étape 3 n'est visible qu'en revenant (écran 195)
    symptome: incompris | gravite: finition | revu: oui: ecran 195 | banc?: non | promesse: -
  - geste: 210 | ecran.vu: 211 | cible: Akkorde | geste.type: touche
    attendu: nom de la tranche « Akkorde » sur une ligne | obtenu: « Akkord / e » coupé au milieu du mot (police 130 %)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 239 | ecran.vu: 240 | cible: Weitere Kit-Sounds | geste.type: touche
    attendu: noms des sons lisibles | obtenu: « Snar e », « Hi-H at », « Offe ne… », « Tief es… », « Cow bell » coupés ; tous ont l'icône main du Clap
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 241 | banc?: non | promesse: t6-Bb-2
mots-faux:
  - ecran: 13 | vu: Tippe auf „Nächster Schritt“, wann du willst. | attendu: wenn du willst
  - ecran: 96 | vu: Noch 2 gepunktete Noten (ailleurs « gestrichelte ») | attendu: gestrichelte, partout pareil
  - ecran: 108 | vu: auf den Offbeats (Steps 4 und 11) | attendu: Steps 3 und 11 (ou ne pas dire « Offbeats »)
  - ecran: 161 | vu: das „Tock“ | attendu: der Rim (nom du pad)
defauts-avant:
  T1: corrigé (écran 146) — « Cutoff: noch +1 · Resonanz: noch +15 » puis validé pile à +15 (écran 149) ; à mi-chemin rien ne valide (écrans 104, 144) ; seul reste : carte repliée pendant le réglage
  T4: corrigé (écran 89) — « Noch 8 … weiter hinten → », un toucher mène à Takt 4 ; idem ↑/← (écrans 97, 137, 139), jamais plus d'1 geste ; écran 118 les notes visées restent à moitié sous le bord
  T8: autre (« Schon erledigt! » existe (écrans 193-195) mais au premier passage l'étape 3/3 déjà faite est sautée sans aucun message, écran 192)
