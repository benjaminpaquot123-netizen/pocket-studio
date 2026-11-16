code testé : 8a8542fe1bffe24594417d7416cac7b13662451b
joué par : sous-agent neuf
siege: d | profil: t7-1d | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
chapitres: ch8 hésitation (renommage « Groove » non validé, coché non 1/2) | ch9 compris du premier coup (coché oui) | ch10 bloqué (boutons « Teil „Intro“ erstellen » absents, 0/7) | ch11 bloqué (pas de parties, 0/2) | ch12 hésitation (Build-up absent, 0/3) | ch13 étape 1 comprise, étape 2 bloquée (1/2) | ch14 hésitation (Akkorde/Melodie absents, 2/3) | ch15 compris du premier coup (coché oui) | ch16 compris du premier coup, export fait (coché oui)
promesse: id=t7-1d-1 | texte: Bravo, Kapitel geschafft! | ecran: 30, 109, 120 | tenue: oui | pourquoi: ch9, ch15 et ch16 cochés (rond vert) dans la liste juste après leur Bravo
promesse: id=t7-1d-2 | texte: Kapitel 8/16 · Schritt 1/2 (écoute) | ecran: 2, 17 | tenue: non | pourquoi: sur une étape d'écoute, un seul « Weiter » passe à l'étape suivante sans rien dire (les étapes d'action, elles, préviennent)
promesse: id=t7-1d-3 | texte: Zieh in Mix den Fader der Spur „Akkorde“ … dann den der Spur „Melodie“ | ecran: 99, 87 | tenue: non | pourquoi: le Mixer montre Drums, Acid 303, Synth, Remix ; ni « Akkorde » ni « Melodie » ; « Cutoff » de « Melodie » absent de l'automation (Master-Ausgang, Ziel-Lautheit, Club, Reverb-Send : eux, conformes)
promesse: id=t7-1d-4 | texte: Die echten Drum-Sounds des Stils, für deinen Track und für die … | ecran: 102, 14 | tenue: non | pourquoi: tout est en allemand, mais des textes coupés à 130 % (carte « Das Kit Techno », légende de la carte d'énergie)
oeil-neuf: q1 | ecran: 120 | image: 120 | les 16 lignes, ✕, les 3 bas de liste paraissent touchables et le sont ; « Tutorial beenden » sans contour ressemble à un texte mais se touche
oeil-neuf: q6 | ecran: 120 | image: 120 | preuve claire : ch15 et ch16 cochés en vert, compteurs 1/2, 0/7… pour le reste ; la suite = toucher un chapitre non fini
oeil-neuf: q9 | ecran: 120 | image: 120 | la ligne 4 (Hi-Hats und Percussion) est coupée en haut par le titre fixe, sinon rien de coupé
hesitations:
  - ecran: 34 | cherchais: le bouton « Teil „Intro“ erstellen » | regarde: défilé tout Struktur/Teile, seulement « + Neuer Teil (Kopie von A · Groove) »
  - ecran: 87 | cherchais: la Spur « Melodie » et son « Cutoff » | regarde: la boîte « Was willst du bewegen? » ne propose que Drums, Acid 303, Synth, Remix
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: le banc écrit « code testé : 8a8542fe… » alors que le brief annonçait 98e83c21
  - geste: 3 | attente: 0 | vu: écran 4 décrit presque vide en texte (l'image montrait bien Struktur/Teile) ; idem écran 106 ; projet fourni vide (pas de parties Intro/Drop), ch10-12 peu jouables ; hors human-act : Read des images du siège seulement
defauts:
  - geste: 1 | ecran.vu: 2 | cible: Weiter | geste.type: touche
    attendu: un message « ce qu'il reste à faire » sur l'étape d'écoute | obtenu: passage direct à l'étape 2, sans message
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 17 | banc?: non | promesse: t7-1d-2
  - geste: 33 | ecran.vu: 43 | cible: Du bist dran: Tippe auf „Teil „Intro“ erstellen“ | geste.type: touche
    attendu: un bouton « Teil „Intro“ erstellen » à l'écran | obtenu: aucun bouton de ce nom ; « + Neuer Teil » crée B et ne valide pas
    symptome: incompris | gravite: mission-impossible | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 86 | ecran.vu: 87 | cible: Automatisiere den „Cutoff“ der Spur „Melodie“ | geste.type: touche
    attendu: Spur « Melodie » et réglage « Cutoff » proposés | obtenu: seulement Drums, Acid 303, Synth, Remix
    symptome: texte-faux | gravite: mission-impossible | revu: non | banc?: non | promesse: t7-1d-3
  - geste: 98 | ecran.vu: 99 | cible: Zieh in Mix den Fader der Spur „Akkorde“ um etwa 2 dB runter | geste.type: glisse
    attendu: une tranche « Akkorde » et une « Melodie » | obtenu: Drums, Acid 303, Synth, Remix ; Weiter ×2 pour passer
    symptome: texte-faux | gravite: contourne-en-2-gestes | revu: oui: ecran 101 | banc?: non | promesse: t7-1d-3
  - geste: 62 | ecran.vu: 63 | cible: Im Teil „Build-up“, letzter Takt, Pad „Clap“ | geste.type: touche
    attendu: « Ich mach's » ouvre la partie Build-up, ou dit qu'elle n'existe pas | obtenu: ouvre le Teil B sans rien dire
    symptome: mauvais-ecran | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 46 | cible: Der ganze Groove spielt. | geste.type: touche
    attendu: phrase vraie seulement si le Groove joue | obtenu: affichée alors que le Groove est vide
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 14 | cible: Ein Baustein = die Drums, Acid und Noten eines Teils | geste.type: touche
    attendu: une seule définition de « Baustein » | obtenu: le tuto dit « der Loop eines einzigen Instruments » sur le même écran
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 101 | ecran.vu: 102 | cible: Die echten Drum-Sounds des Stils, für deinen Track und für die | geste.type: touche
    attendu: texte de la carte « Das Kit Techno » entier | obtenu: coupé au milieu de la phrase (image 102), aussi écrans 103, 111
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 111 | banc?: non | promesse: t7-1d-4
  - geste: 86 | ecran.vu: 87 | cible: Synth | geste.type: touche
    attendu: mêmes noms de pistes partout | obtenu: Sounds dit « Vocals », Mixer et automation disent « Synth » ; liste dit « Stab (Akkorde) », « Riff »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 99 | banc?: non | promesse: t7-1d-3
mots-faux:
  - ecran: 63 | vu: Roll (tuto) | attendu: Rolls (bouton)
  - ecran: 63 | vu: Pad „Clap“ (tuto) | attendu: Clap FX (pad)
defauts-avant:
  - N19 | pas corrigé (écran 2) : Weiter sur l'étape d'écoute passe sans dire ce qui reste (idem écran 17) ; « Fast fertig! » vu seulement à la toute fin (écran 118, « Noch 33 Schritte », honnête)
  - T9 | corrigé (écran 30) : ch9 coché après Bravo, aussi ch15 (109) et ch16 (120)
  - N25 | pas corrigé (écran 63) : « Ich mach's » ouvre le Teil B au lieu de Build-up, sans le dire (réserve : partie non créée)
  - N28 | pas vu : aucune consigne « Takt N » rencontrée (seulement « letzter Takt »)
  - N26 | pas corrigé (écran 46) : « Der ganze Groove spielt. » alors que le Groove est vide
  - N24 | pas corrigé (écran 99) : « Akkorde »/« Melodie » absents du Mixer ; Master-Ausgang, Ziel-Lautheit, Club −8 LUFS retrouvés (écran 106)
  - U27 | pas corrigé (écran 99) : Mixer = Drums, Acid 303, Synth, Remix ; Sounds = Drums, Acid 303, Vocals, Remix
  - N21 | pas corrigé (écran 14) : « Baustein » défini deux fois autrement ; Akkorde/Stab/Synth et Melodie/Riff pour la même piste
