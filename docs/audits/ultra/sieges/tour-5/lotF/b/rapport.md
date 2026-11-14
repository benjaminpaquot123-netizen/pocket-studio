siege: b | profil: t5-Fb producteur-allemand-presbyte-200 | lang: de | device: tablet11Land | font: 200 | mode: pro
mission: libre | buts: but1, but2, but3
promesse: id=t5-Fb-1 | texte: Lass eine Einstellung sich von selbst bewegen: Wähl sie aus und zeichne ihre Kurve (mit dem Finger oder eine Linie mit zwei Taps). | ecran: 23 | tenue: non | pourquoi: la courbe se dessine (rampe visible, « 1 Einstellung bewegt sich von selbst ») mais pendant la lecture le curseur Cutoff reste figé à 1,6 kHz (écrans 21-23)
promesse: id=t5-Fb-2 | texte: Ein Effekt kann nur in bestimmten Teilen spielen: Tippe auf ihre Buchstaben. | ecran: 41 | tenue: oui | pourquoi: Filter sur B, Sättigung sur A ; « Was spielt » par partie le confirme (« Spielt nur in: A/B ») et c'est gardé après Retour
promesse: id=t5-Fb-3 | texte: A · mit dem Master / B · ohne den Master (+ « Gleich laut ») | ecran: 52 | tenue: oui | pourquoi: bascule A/B présente et sélectionnable pendant la lecture, avec mise à niveau ; son non jugé
promesse: id=t5-Fb-4 | texte: 18,0 kHz / 849 Hz / 40 Hz (axe de la courbe) | ecran: 11 | tenue: non | pourquoi: à 200 % les unités de l'axe sont coupées (« 18,0 », « 40 H », « Sch »), et « Lautstärke −14,0 » n'a pas d'unité
hesitations:
  - ecran: 7 | cherchais: où dessiner la courbe | regarde: le couloir n'est visible qu'après deux glissés, ma première trace n'a rien montré en texte
  - ecran: 70 | cherchais: comment éditer la courbe de la partie B (« Steps von Teil A ») | regarde: aucun choix de partie à côté de « Pro Teil » ; « Spielen ab… 2 · B » ne change rien
  - ecran: 34 | cherchais: « écouter sans puis avec » sur l'effet de piste | regarde: seulement le bouton marche/arrêt ; le A/B n'existe que dans le Master, très bas
abandon: jamais
incidents-banc:
  - geste: 63 | attente: 5 | vu: compteur bloqué sur « A Takt 1/2 » pendant la lecture du song, ne passe jamais à B
defauts:
  - geste: 1 | ecran.vu: 1 | cible: Weiter Pro Mein erster Beat Loop von 1 Takt Gespeichert gerade eben | geste.type: touche
    attendu: la grande carte « Weiter » affiche le nom du projet | obtenu: carte orange vide, aucun texte visible (vu aussi écran 58)
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 58 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: Drums | geste.type: touche
    attendu: noms de pistes lisibles sur la console | obtenu: « Drum/s », « Synt/h », « Remi/x » coupés en plein mot
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 91 | banc?: non | promesse: t5-Fb-4
  - geste: 6 | ecran.vu: 11 | cible: 18,0 kHz | geste.type: glisse
    attendu: axe avec valeurs et unités lisibles | obtenu: « 18,0 », « 40 H », « Sch 1 » et chiffres des temps coupés en bas
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 91 | banc?: non | promesse: t5-Fb-4
  - geste: 13 | ecran.vu: 23 | cible: Cutoff | geste.type: touche
    attendu: « Einstellung zeigen » pendant la lecture montre le Cutoff qui suit la rampe 40 Hz → 18 kHz | obtenu: curseur figé « 1,6 kHz, à 60 % » sur trois relevés, rien n'indique qu'une courbe le pilote
    symptome: rien | gravite: finition | revu: non | banc?: moteur | promesse: t5-Fb-1
  - geste: 44 | ecran.vu: 45 | cible: 18,0 kHz | geste.type: glisse
    attendu: glisser vers le haut fait défiler la colonne | obtenu: le glissé a redessiné la courbe (un pas monté à 18 kHz) au lieu de défiler ; Rückgängig répare
    symptome: perdu | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t5-Fb-1
  - geste: 30 | ecran.vu: 31 | cible: Teil A: Kopie von Teil B. Ändere ihn für eine Variante, oder „Leeren“. | geste.type: touche
    attendu: « Teil B: Kopie von Teil A » (B vient d'être copié de A) | obtenu: le sens est inversé
    symptome: texte-faux | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: -
  - geste: 73 | ecran.vu: 74 | cible: B | geste.type: touche
    attendu: « Teil zum Bearbeiten » → B passe sur B | obtenu: reste « Aktueller Teil: A », « Teil: A » et Loop s'allume sur A ; il faut arrêter la lecture puis toucher le Baustein B
    symptome: rien | gravite: contourne-en-3-gestes | revu: oui: ecran 78 | banc?: moteur | promesse: t5-Fb-2
  - geste: 40 | ecran.vu: 41 | cible: Bewegt: Filter Cutoff | geste.type: touche
    attendu: pour la partie A, ne pas annoncer une courbe sur un filtre qui ne joue qu'en B (ou le signaler) | obtenu: « Bewegt: Filter Cutoff … Steigt von 15 % auf 84 % » listé en A
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t5-Fb-2
  - geste: 32 | ecran.vu: 34 | cible: Was sich bewegt (1) | geste.type: touche
    attendu: bouton lisible en entier à 200 % | obtenu: « Was sich bewe… »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t5-Fb-4
  - geste: 46 | ecran.vu: 47 | cible: Lautstärke −14,0 · wie Streaming-Musik (−14) | geste.type: glisse
    attendu: valeur avec unité (LUFS), comme dans les détails | obtenu: nombre sans unité (idem « TP −1,2 » sans dBTP)
    symptome: sans-nom | gravite: finition | revu: non | banc?: non | promesse: t5-Fb-4
mots-faux:
  - ecran: 50 | vu: 1. Equalizer · fehlt | attendu: 1. Equalizer · aus
  - ecran: 51 | vu: Attack: 100 % des Look-ahead | attendu: Attack: 100 % des Look-aheads
