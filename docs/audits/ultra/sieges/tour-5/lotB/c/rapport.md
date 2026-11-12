siege: c | profil: t5-Bc producteur-allemand | lang: de | device: tablet8 | font: 100 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t5-Bc-1 | texte: Du baust den ganzen Track selbst, Schritt für Schritt: Ich erkläre und prüfe. | ecran: 50 | tenue: oui | pourquoi: après « Zeig's mir » le tempo revient à 120 et l'étape reste 2/4 (écran 9) ; « alles » sur la Kick affiche « 12 Schläge zu viel hier (durchgestrichen) », rien n'est validé
promesse: id=t5-Bc-2 | texte: Zeig's mir | ecran: 49 | tenue: oui | pourquoi: la démo ne coche rien (« Noch 4 gestrichelte Kästchen hier ») et ma frappe en trop revient, marquée « durchgestrichen: wegnehmen »
promesse: id=t5-Bc-3 | texte: Grundton … B♭ … B | ecran: 17 | tenue: non | pourquoi: notation anglaise (B♭/B) au lieu de l'allemande (B/H) ; la 303 ne propose que « C D E » ou « Do Re Mi »
promesse: id=ecran-pruefe | texte: der Clap auf 2 und 4, mit seinem Ghost-Schlag kurz vor dem nächsten Takt (auf „Sanft“). | ecran: 67 | tenue: non | pourquoi: Ghost-Schlag laissé à « Stärke 100 % » et pourtant « Bravo, Kapitel geschafft! »
hesitations:
  - ecran: 25 | cherchais: où régler le Swing 5 % | regarde: l'anneau « Ich mach's » entoure la grille de pas, aucun réglage Swing nulle part
  - ecran: 30 | cherchais: Swing dans Struktur / Song-Einstellungen | regarde: seulement 2/4…7/4, Bausteine
  - ecran: 42 | cherchais: Swing dans Pad-Einstellungen | regarde: Sound, Lautstärke, Panorama, stumm
abandon: ecran 76 | parce que le Swing (Kapitel 1, Schritt 4/4) est introuvable : chapitre 1 resté 2/4, j'ai sauté l'étape avec « Weiter » deux fois
incidents-banc:
defauts:
  - geste: 23 | ecran.vu: 24 | cible: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (5 %): Die kleinen Hi-Hats schleppen kaum merklich, der Groove atmet, ohne zu hinken. | geste.type: touche
    attendu: « Ich mach's » ouvre ou montre le réglage Swing | obtenu: anneau sur la grille de pas de la Studio-Kick ; aucun réglage Swing dans Sounds, Struktur, Mix, Mehr, Pad-Einstellungen ; chapitre 1 impossible à finir (2/4 dans « Kapitel »)
    symptome: mauvais-ecran
    gravite: mission-impossible | revu: oui: ecran 76 | banc?: non | promesse: -
  - geste: 26 | ecran.vu: 27 | cible:  Zeig's mir | geste.type: touche
    attendu: la démo montre où se règle le Swing | obtenu: le doigt tape dans la grille de pas (« Schau, ich mach's... »), rien n'indique le Swing, l'étape reste à faire
    symptome: incompris
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: t5-Bc-2
  - geste: 16 | ecran.vu: 17 | cible: B♭ | geste.type: touche
    attendu: « B » (notation allemande) | obtenu: « B♭ », et la touche suivante « B » devrait être « H » : un Allemand choisit la mauvaise note
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 19 | banc?: non | promesse: t5-Bc-3
  - geste: 78 | ecran.vu: 79 | cible: case « C D E » | geste.type: touche
    attendu: un choix de notation allemande (C D E … A H) | obtenu: seulement « C D E » ↔ « Do Re Mi » (BASSLINE · GRUNDTON DO), pas de H
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Bc-3
  - geste: 66 | ecran.vu: 67 | cible: Du bist dran: der Clap auf 2 und 4, mit seinem Ghost-Schlag kurz vor dem nächsten Takt (auf „Sanft“). | geste.type: touche
    attendu: le Step 16 doit être « Sanft » pour valider (Ich erkläre und prüfe) | obtenu: Step 16 « Schlag, Stärke 100 % » accepté, « Bravo, Kapitel geschafft! »
    symptome: rien
    gravite: finition | revu: non | banc?: non | promesse: ecran-pruefe
  - geste: 21 | ecran.vu: 22 | cible: Später | geste.type: touche
    attendu: la carte « Das Kit Techno » se ferme | obtenu: la carte reste (elle bouge seulement), il faut retoucher « Später » une 2e fois
    symptome: rien
    gravite: contourne-en-1-gestes | revu: oui: ecran 23 | banc?: non | promesse: -
  - geste: 77 | ecran.vu: 78 | cible: BASSLINE · GRUNDTON C | geste.type: touche
    attendu: Grundton F, puisque « Tonart des Tracks: F · Phrygisch » | obtenu: la 303 reste en C
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 17 | vu: B♭ | attendu: B
  - ecran: 17 | vu: B | attendu: H
  - ecran: 16 | vu: (G♭) | attendu: (Ges) — et le clavier de Grundton dit « F♯ » pour la même touche
  - ecran: 56 | vu: die das Pumpen bei jeder Kick absenkt | attendu: der bei jeder Kick abgesenkt wird (pumpt)
