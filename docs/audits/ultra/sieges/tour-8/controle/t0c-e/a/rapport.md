code testé : dee3519df1694386b61d84269762b368f08029e8
joué par : sous-agent neuf
siege: a | profil: t8-t0c-e | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: ch8-12-finis, ch13-commence, mots-allemands
promesse: id=t0c-e-1 | texte: « Ich mach's » | ecran: 43 | tenue: non | pourquoi: ch11 (#42, refait #46) et ch12 étape Roll (#67) : le bouton replie le tuto et fait défiler, n'ajoute rien, ne dit pas ce qui reste ; il marche en ch12 étape 2 (#73, montre « 3 Schläge zu viel hier ») et en ch13 (#81, #84)
promesse: id=t0c-e-2 | texte: « STEPS: TECHNO · CLAP · BUILD-UP » [coupé] | ecran: 67 | tenue: non | pourquoi: textes coupés (écrans 6, 16, 67, 74, 79), anglais restant (« Steps », « Vibe », « Loop »), tournures pas naturelles (voir mots-faux)
oeil-neuf: q1 | ecran: 85 | image: 85 | « Eine Einstellung automatisieren », « Loop / Pro Teil / Song », « Über den Drums · 1 Takt », « Zeichnen / Linie » et l'en-tête du tuto se touchent ; « Leeren » (texte orange) ne ressemble pas à un bouton
oeil-neuf: q6 | ecran: 85 | image: 85 | preuve : bandeau vert « Bravo, Kapitel geschafft! » et courbe « Filter Cutoff 9,4 kHz » ; la suite (« Nächstes Kapitel ») est cachée dans le bandeau replié
oeil-neuf: q9 | ecran: 85 | image: 85 | la rangée « Zeichnen · Linie · Glätten · ×1 » est coupée en bas par le panneau du tuto
hesitations:
  - ecran: 43 | cherchais: ce que « Ich mach's » avait fait | regarde: rien ajouté, panneau replié, la ligne « Noch: … » n'est plus visible
  - ecran: 49 | cherchais: les boutons « + Teil » | regarde: ils bougent après chaque ajout, j'ai ajouté « A · Groove » par erreur (annulé écran 55)
  - ecran: 82 | cherchais: le lien entre « Akkorde ✓ · Danach: Snare » et le pad « Techno · Clap » | regarde: aucun, le texte parle d'autres pistes
abandon: jamais
incidents-banc:
  - geste: 6 | attente: 0 | vu: « regarde image » refusé (quota d'images), continué en texte
  - geste: - | attente: 0 | vu: hors human-act.py : lecture (cat/sed/grep, Read) des fichiers screen-N.txt, screen-N-grille.png et journal.txt de mon dossier de siège
defauts:
  - geste: 42 | ecran.vu: 43 | cible: «   Ich mach's » | geste.type: touche
    attendu: les parts Intro 2…Ende ajoutées, ou la liste de ce qui reste | obtenu: tuto replié, défilement vers les boutons, rien d'ajouté, étape toujours 1/2
    symptome: rien | gravite: contourne-en-7-gestes | revu: oui: ecran 48 | banc?: non | promesse: t0c-e-1
  - geste: 67 | ecran.vu: 69 | cible: «   Ich mach's » | geste.type: touche
    attendu: Roll ×3/×4 posé sur le Schlag 12, ou dit ce qui reste | obtenu: saute au Takt 4/4 du Clap, le pas reste « ×1 », rien n'est dit ; posé à la main (#69)
    symptome: rien | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: t0c-e-1
  - geste: 16 | ecran.vu: 18 | cible: « Beispiel übersprungen, ohne es anzuhören. » | geste.type: touche
    attendu: pas ce message : je venais d'écouter l'exemple (#16, il a joué) | obtenu: le message dit que je l'ai sauté (aussi écran 6 après écoute au #5)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 6 | banc?: non | promesse: t0c-e-2
  - geste: 66 | ecran.vu: 67 | cible: titre « STEPS: TECHNO · CLAP · BUILD-UP » [coupé] | geste.type: touche
    attendu: titre et « Alle Pads » lisibles en entier | obtenu: titre, « ? » et « Alle Pads » coupés (aussi écrans 74, 79), « Steps » en anglais
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: t0c-e-2
  - geste: 15 | ecran.vu: 16 | cible: légende des parts (Intro, Intro 2, Drop…) sous les barres | geste.type: touche
    attendu: légende complète à 130 % | obtenu: légende coupée à droite, « Energie » chevauche (image 16)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t0c-e-2
  - geste: 5 | ecran.vu: 6 | cible: « Kit: Eigenes » [coupé] | geste.type: touche
    attendu: nom du kit et aide « Pads klingen, Kästchen behalten die Schläge… » lisibles | obtenu: les deux coupés
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t0c-e-2
  - geste: 31 | ecran.vu: 32 | cible: « Dabei: Studio-Kick, … Akkorde, Melodie. » | geste.type: touche
    attendu: le même nom que dans la consigne (« der Stab », écran 12) | obtenu: « Akkorde » dans le résultat, « Stab » dans les consignes
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t0c-e-2
mots-faux:
  - ecran: 8 | vu: « Ein Teil sind mehrere Bausteine » | attendu: « Ein Teil besteht aus mehreren Bausteinen »
  - ecran: 36 | vu: « Sein Pad „Techno · Clap“ wird Takt für Takt schneller » | attendu: « Das Pad „Techno · Clap“ … »
  - ecran: 66 | vu: « eine Salve » | attendu: « ein Wirbel » (ou « ein Roll »)
  - ecran: 69 | vu: « Ganzer Schlag auf ×2 (1/32-Salve) » | attendu: « (1/32-Wirbel) »
  - ecran: 38 | vu: « ohne Techno · Rim » (consigne : « Rimshot ») | attendu: un seul nom, « Rimshot »
  - ecran: 82 | vu: « Tippe auf einen Vibe » | attendu: « Tippe auf eine Stimmung »
  - ecran: 85 | vu: « Über den Drums · 1 Takt » | attendu: « Wie der Drum-Loop · 1 Takt »
tuto: ch8 | compris du premier coup | Kit Techno geladen, Beispiel gehört (écrans 1-7)
tuto: ch9 | compris du premier coup | carte des parts lue, message « übersprungen » faux (écrans 8-23)
tuto: ch10 | compris du premier coup | parts créées, « Zeig's mir » marche (écrans 24-40)
tuto: ch11 | hésitation | « Ich mach's » n'ajoute rien, parts ajoutées à la main, « Empfohlene Längen setzen » marche (écrans 41-65)
tuto: ch12 | hésitation | Roll ×4 posé à la main, Hi-Hat effacée, Becken posé : « Bravo, Kapitel geschafft! » (écrans 66-80)
tuto: ch13 | compris du premier coup | fini : Reverb-Send et courbe de filtre faits par « Ich mach's » (écrans 81-85)
defauts-avant:
  - de-mots | encore là | ecran: 24, 32, 16, 66, 79, 15, 26, 72, 82, 84, 85 | 1: « Tippe auf „Teil ‚Intro‘ erstellen“ » (corrigé, 24) ; 2: « der ganze Groove spielt darin. Dabei: … » (corrigé, 32) ; 3: consigne « Rimshot » (corrigé, 16 ; résultat « Techno · Rim », 38) ; 4: « Drück lange auf den letzten Schlag vor Zählzeit 4 » (corrigé, 66) ; 5: pas atteint ; 6: seul « gestrichelt » vu (« Noch 1 gestricheltes Kästchen hier », corrigé, 79) ; 7: encore là, « das Riff » (15) puis « Melodie » (26, 84) ; 8: « …, wenn du so weit bist. » (corrigé, 72) ; 9: autre, « Akkorde ✓ · Danach: Snare » sur le pad Techno · Clap, pas clair (82) ; 10: encore là, consigne « Filter (Sweep) » (84), écran « Filter Cutoff » (85)
  - rafale | corrigé | ecran: 66 | « Du bist dran: Im Teil „Build-up“, letzter Takt, Pad „Techno · Clap“: Drück lange auf den letzten Schlag vor Zählzeit 4, dann „Roll“ ×3 oder ×4: eine Salve. » : plus de « letzte Salve »
