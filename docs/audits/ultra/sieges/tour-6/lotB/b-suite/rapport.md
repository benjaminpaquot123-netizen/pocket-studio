siege: b | profil: t6-Bb producteur-allemand-tablette | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t6-Bb-1 | texte: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (5 %) | ecran: 23 | tenue: oui | pourquoi: curseur Swing visible au-dessus du tuto ; à 5 % exactement « Bravo, Kapitel geschafft! » (6 % refusé, il faut viser juste)
promesse: id=t6-Bb-2 | texte: Tippe bei einem Sound auf ☆, um ihn unter „Favoriten“ wiederzufinden. | ecran: 232 | tenue: oui | pourquoi: ☆ à côté de chaque son (Techno), touché → ★, puis « Favoriten 1 Sound » en tête de liste (écran 233)
promesse: id=t6-Bb-3 | texte: Neue Kurve: zeichne sie unten. | ecran: 203 | tenue: oui | pourquoi: la courbe montée-descente dessinée en deux traits s'affiche aussitôt en barres jaunes
promesse: id=t6-Bb-4 | texte: Einstellung zeigen | ecran: 205 | tenue: oui | pourquoi: ouvre « Effekte · Melodie », Filter, curseur Cutoff, avec « Eine Kurve bewegt diese Einstellung während der Wiedergabe. »
oeil-neuf: q1 | ecran: 233 | image: 233 | lignes Favoriten/Sounds avec › et boutons Laden lisibles comme touchables ; « SOUNDS ⌄ » et « SOUND-PACKS ⌄ » repliables mais ressemblent à des titres
oeil-neuf: q6 | ecran: 233 | image: 233 | preuve de l'étoile : « Favoriten 1 Sound » en haut ; suite logique : Setzen ou ✕
oeil-neuf: q9 | ecran: 233 | image: 233 | puce « Geschlossene Hi-Hats » coupée au bord droit sans signe de défilement ; écran 231 noms de sons « Te… » et « Kick / 3 » sur deux lignes
chapitre: 1 Tempo und Tonart | ecran: 23 | hésitation | Swing : 2 %, 9 %, 6 % avant d'avoir 5 % pile, seul 5 % valide
chapitre: 2 Die Kick und das Grollen | ecran: 39 | hésitation | au pas Rumble, le nom du pad est caché sous « Noch 4 gestrichelte Kästchen hier »
chapitre: 3 Der Clap | ecran: 48 | compris du premier coup | appui long sur le dernier coup → « Sanft », validé
chapitre: 4 Hi-Hats und Percussion | ecran: 75 | compris du premier coup | « durchgestrichen: wegnehmen » guide bien après un glissé trop large
chapitre: 5 Der Bass | ecran: 101 | hésitation | piste créée « Bass Plaits » alors que le texte dit « Analoger Bass » ; note du Takt 4 à moitié hors cadre
chapitre: 6 Der Stab (Akkorde) | ecran: 120 | compris du premier coup | s'ouvre sur Takt 4 au lieu de 1, le bandeau ramène
chapitre: 7 Das Riff | ecran: 140 | hésitation | « Noch 5 … zu tippen » sans flèche alors que le F aigu est hors écran ; toucher le bandeau y mène
chapitre: 8 Dein erster Teil: der Groove | ecran: 144 | compris du premier coup | un seul bouton
chapitre: 9 Die Teile verstehen | ecran: 151 | compris du premier coup | carte des parties claire
chapitre: 10 Bau deine Teile | ecran: 165 | hésitation | chaque partie dit « der ganze Groove spielt darin » alors que la consigne retire des instruments
chapitre: 11 Ordne deine Teile | ecran: 176 | compris du premier coup | « Empfohlene Längen setzen » fait tout
chapitre: 12 Die Übergänge | ecran: 189 | hésitation | « Zum Teil „Build-up“ gehen » reste sur A · Groove tant que le Song joue ; « Takt 4 » introuvable (pad d'1 Takt)
chapitre: 13 Die Effekte | ecran: 203 | compris du premier coup | courbe dessinée en 2 traits, validée
chapitre: 14 Der Mix | ecran: 213 | compris du premier coup | deux faders baissés
chapitre: 15 Das Mastering | ecran: 216 | compris du premier coup | « Club · −8 LUFS »
chapitre: 16 Exportiere deinen Track | ecran: 226 | compris du premier coup | WAV puis Drive, « Tutorial geschafft: Bravo! »
bravo: oui | ecran: 226
hesitations:
  - ecran: 35 | cherchais: quel pad je remplis pour le Rumble | regarde: image : le titre du pad est recouvert par le bandeau « Noch 4 gestrichelte Kästchen hier »
  - ecran: 128 | cherchais: où est la note restante | regarde: « Noch 5 gestrichelte Noten zu tippen » sans flèche, la note est au-dessus de l'écran
  - ecran: 181 | cherchais: « Takt 4 » du Build-up | regarde: le pad n'a que « 1 Takt », j'ai deviné le 12e pas
abandon: jamais
incidents-banc:
  - geste: 29 | attente: 0 | vu: après « Ich mach's », le texte de l'écran est vide (seule la barre Feedback) ; idem 35, 51, 100, 167, 209, 215, 216 : j'ai dû passer par l'image
  - geste: 27 | attente: 0 | vu: deux gestes envoyés dans un même appel Bash par erreur (27-28, 31-32, 71-72), un à la fois ensuite ; seul outil hors human-act : Read sur mes images de siège
defauts:
  - geste: 178 | ecran.vu: 178 | cible: bouton ⊕ «   Zum Teil „Build-up“ gehen » | geste.type: touche
    attendu: ouvrir la partie F · Build-up | obtenu: reste sur « Teil: A · Groove » tant que « der Song » joue ; il faut arrêter le Song puis retoucher
    symptome: mauvais-ecran
    gravite: contourne-en-2-gestes | revu: oui: ecran 179 | banc?: non | promesse: -
  - geste: 155 | ecran.vu: 155 | cible: texte « Teil „Intro 2“ erstellt: der ganze Groove spielt darin. » | geste.type: touche
    attendu: confirmer « alles außer dem Ride und dem Riff » comme demandé | obtenu: dit que tout le Groove joue (même message pour Intro, Break, Ende)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 159 | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 22 | cible: curseur [état « 6 % », à 6 %] | geste.type: touche
    attendu: « ein ganz kleines bisschen » (≈5 %) accepté | obtenu: 6 % non validé, il faut 5 % pile sur un curseur où 1 % ≈ 8 px
    symptome: incompris
    gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 8 | cible: bouton « Kit Techno laden (4 MB) » [coupé] | geste.type: touche
    attendu: bouton de chargement lisible | obtenu: bouton coupé et recouvert par « Weiterlesen ⌄ » à chaque pas du chapitre 1
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 19 | banc?: non | promesse: -
  - geste: 35 | ecran.vu: 35 | cible: texte « Noch 4 gestrichelte Kästchen hier » [coupé] | geste.type: touche
    attendu: voir le nom du pad à remplir (Rumble) | obtenu: le nom du pad est caché par le bandeau, on tape à l'aveugle
    symptome: cache
    gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: -
  - geste: 78 | ecran.vu: 78 | cible: bouton « Bass Plaits » ⋮ | geste.type: touche
    attendu: piste au son « Analoger Bass » comme dit le tuto | obtenu: piste nommée « Bass Plaits », réglages Bassdrum/Snare/Hi-Hat
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 189 | ecran.vu: 189 | cible: texte « Du bist dran: Tippe im Teil „Drop“ auf das allererste Kästchen des Becken-Pads … » | geste.type: touche
    attendu: un pas à faire | obtenu: pas déjà coché avec « Nächstes Kapitel » sans que je touche le Becken
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 209 | ecran.vu: 209 | cible: Akkord e (nom de tranche du mixeur) | geste.type: touche
    attendu: « Akkorde » sur une ligne | obtenu: coupé en « Akkord / e » à 130 %
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 201 | ecran.vu: 203 | cible: texte de l'axe de la courbe « 18,0 kI », « 849 H: », « Schla » | geste.type: glisse
    attendu: unités lisibles (kHz, Hz, Schlag) | obtenu: libellés de l'axe coupés à 130 %
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 201 | banc?: non | promesse: -
  - geste: 231 | ecran.vu: 231 | cible: bouton ◎ « Kick 3 Techno » | geste.type: touche
    attendu: nom du son lisible | obtenu: « Kick / 3 / Te… » sur trois lignes, nom de pack coupé
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 95 | vu: gepunktete Note | attendu: gestrichelte Note (comme partout ailleurs)
  - ecran: 191 | vu: Akkorde ✓ · Snare: danach | attendu: une indication sur le Clap (pad ouvert : Techno · Clap)
