code testé : 4cb88f552031889ab2b4854b9ddd285f2abb6250
joué par : sous-agent neuf
siege: b | profil: t6-Bb producteur-allemand-tablette | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t6-Bb-1 | texte: Gib ein ganz kleines bisschen Swing dazu (7 %) | ecran: 20 | tenue: oui | pourquoi: curseur Swing visible au-dessus du tuto, glissé, étape validée (mais déjà à 4 %)
promesse: id=t6-Bb-2 | texte: (aucune ☆ sur les cartes de sons « Runder Bass… », ni sur les pads) | ecran: 253 | tenue: non | pourquoi: aucune étoile vue sur les sons ; seul « ★ Favoriten » existe dans les Vibes d'effets
promesse: id=t6-Bb-3 | texte: Neue Kurve: zeichne sie unten. | ecran: 223 | tenue: oui | pourquoi: les barres de la courbe apparaissent dès le premier trait
promesse: id=t6-Bb-4 | texte: Einstellung zeigen | ecran: 226 | tenue: oui | pourquoi: ouvre « Effekte · Melodie », Filter avec Cutoff et « Eine Kurve bewegt diese Einstellung »
oeil-neuf: q1 | ecran: 262 | image: 262 | touchables clairs (puces, 3 gros boutons du tuto, onglets) ; « Intro Takt 1/128 ▾ » a l'air d'un menu ; rangée INSTRUMENT à moitié sous le panneau
oeil-neuf: q6 | ecran: 262 | image: 262 | après saut au chapitre 1 : en-tête « Kapitel 1/16 · Schritt 1/4 » mais corps « Bravo, dein Track ist fertig! » : aucune suite possible
oeil-neuf: q9 | ecran: 262 | image: 262 | pas de mot coupé visible ; boutons de la rangée INSTRUMENT coupés par le panneau du tuto
chapitre: 1 Tempo und Tonart | ecran: 6 | hésitation | Swing demandé 7 %, validé à 4 % : je ne sais pas pourquoi ça passe ; « Später » coupé dans la carte Kit
chapitre: 2 Die Kick und das Grollen | ecran: 21 | hésitation | étape Rumble : le titre de la grille est coupé, j'ai tapé sans voir quel pad j'éditais
chapitre: 3 Der Clap | ecran: 34 | hésitation | 1er appui long a posé le coup sans menu « Sanft », il a fallu un 2e appui long
chapitre: 4 Hi-Hats und Percussion | ecran: 40 | hésitation | « leiser unter der Kick » mais validé à 100 % ; « Ride » demandé, le pad s'appelle « Becken »
chapitre: 5 Der Bass | ecran: 77 | hésitation | « Analoger Bass » annoncé, la piste dit « Bass Plaits » ; Cutoff à 7 glissés sous la piano roll
chapitre: 6 Der Stab (Akkorde) | ecran: 114 | compris du premier coup | cases pointillées claires ; « Offbeats (Steps 4 und 11) » étrange pour un pro
chapitre: 7 Das Riff | ecran: 133 | hésitation | Cutoff +7 : aucun texte ne dit ce qui manque ; validé à +16
chapitre: 8 Dein erster Teil: der Groove | ecran: 164 | hésitation | « Nenn diese Szene » : le menu du Teil A n'offre pas de renommage, seul le bouton du tuto le fait
chapitre: 9 Die Teile verstehen | ecran: 169 | compris du premier coup | écoute + Verstanden
chapitre: 10 Bau deine Teile | ecran: 177 | compris du premier coup | boutons « Teil … erstellen » ; « Tock » ne correspond à aucun pad
chapitre: 11 Ordne deine Teile | ecran: 191 | compris du premier coup | « Noch: … » liste ce qui reste, excellent
chapitre: 12 Die Übergänge | ecran: 201 | hésitation | « letzten Schlag vor der letzten Zählzeit » obscur ; Schritt 2/3 passe direct à « Erledigt »
chapitre: 13 Die Effekte | ecran: 209 | hésitation | pas de tranche « Clap » dans Mix ; seul « Ich mach's » ouvre le Reverb-Send du pad
chapitre: 14 Der Mix | ecran: 229 | hésitation | noms des pistes sortis de l'écran au-dessus des faders : deviné Akkorde = 4e
chapitre: 15 Das Mastering | ecran: 235 | compris du premier coup | Ziel-Lautheit Club
chapitre: 16 Exportiere deinen Track | ecran: 240 | compris du premier coup | Teilen → M4A → Drive
bravo: oui | ecran: 246
hesitations:
  - ecran: 89 | cherchais: où écrire G♭ et le F aigu (hors champ) | regarde: bandeau « Noch 2 gepunktete Noten, weiter oben ↑ » m'y a mené
  - ecran: 102 | cherchais: « Sound der Spur einstellen » | regarde: 7 glissés (Tastatur, Noten importieren, Tonleiter, Instrument…) avant Cutoff
  - ecran: 210 | cherchais: le Kanalzug du pad Clap dans Mix | regarde: seules les pistes ; « Drums » ouvre les effets de piste
abandon: jamais
incidents-banc:
  - geste: 235 | attente: 60 | vu: écran 236 texte vide (seule la pastille), l'image montrait le Master-Ausgang ; écran 237 aussi, revenu après « attends 60 »
  - geste: 57 | attente: 0 | vu: (outil) j'ai lancé par erreur 3 gestes dans un seul appel (#58-#60) ; outils hors human-act : Read des PNG et lecture de journal/rapport du siège
defauts:
  - geste: 19 | ecran.vu: 20 | cible: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (7 %) | geste.type: glisse
    attendu: validé autour de 7 %, ou « encore un peu » | obtenu: « Bravo, Kapitel geschafft! » à 4 %
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Bb-1
  - geste: 156 | ecran.vu: 158 | cible: Dreh unter „Sound der Spur einstellen“ der Melodie „Cutoff“ und „Resonanz“ jeweils um mindestens 15 auf | geste.type: glisse
    attendu: à Cutoff +7, une ligne « Cutoff: noch +8 » | obtenu: rien ne dit ce qui manque, même panneau ouvert
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: oui: ecran 162 | banc?: non | promesse: -
  - geste: 165 | ecran.vu: 166 | cible: Du bist dran: Nenn diese Szene „Groove“ | geste.type: touche
    attendu: renommer le Teil depuis son menu | obtenu: menu « A / Neuer leerer Teil », pas de renommage ; appui long sans effet
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: oui: ecran 167 | banc?: non | promesse: -
  - geste: 210 | ecran.vu: 211 | cible: Dreh in Mix den „Reverb-Send“ auf dem Kanalzug des Pads „Clap“ auf. | geste.type: touche
    attendu: une tranche « Clap » dans Mix | obtenu: Mix n'a que les pistes ; « Drums » ouvre ses effets ; seul « Ich mach's » ouvre « Effekte · Pad 5 (Clap) »
    symptome: perdu | gravite: contourne-en-3-gestes | revu: oui: ecran 213 | banc?: non | promesse: -
  - geste: 229 | ecran.vu: 229 | cible: Zieh in Mix den Fader der Spur „Akkorde“ etwas runter | geste.type: glisse
    attendu: le nom de chaque piste au-dessus de son fader | obtenu: noms hors écran (en haut), faders anonymes « +0,0 »
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 253 | ecran.vu: 254 | cible: Runder Bass Säge · gehalten | geste.type: appui-long
    attendu: un menu (☆ favori, écouter) | obtenu: le son remplace aussitôt celui de la piste Akkorde (« Akkorde Runder Bass »), pas d'étoile
    symptome: mauvais-ecran | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Bb-2
  - geste: 261 | ecran.vu: 262 | cible: 1 Tempo und Tonart | geste.type: touche
    attendu: revoir le chapitre 1 | obtenu: « Kapitel 1/16 · Schritt 1/4 » + « Bravo, dein Track ist fertig! », aucun bouton du chapitre
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 207 | ecran.vu: 208 | cible: Die Übergänge Kapitel 12/16 · Schritt 2/3 · Pro | geste.type: touche
    attendu: étape 3/3 ou « Bereits erledigt » | obtenu: saut direct à « Erledigt », sans dire pourquoi l'étape 3 est passée
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: Später | geste.type: touche
    attendu: lien « Später » entier | obtenu: « Später » coupé en bas de la carte Kit Techno, carte répétée à chaque étape
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 159 | banc?: non | promesse: -
  - geste: 222 | ecran.vu: 223 | cible: Steps von Teil D (jeder Teil hat seine eigenen). | geste.type: glisse
    attendu: barre du haut sur le Teil D | obtenu: « Intro Takt 1/128 ▾ » en haut pendant l'édition du Drop ; axes « 18,0 kI », « 849 H », « Schla » coupés
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 63 | vu: Ride | attendu: Becken (nom du pad)
  - ecran: 177 | vu: „Tock“ | attendu: Rim (nom du pad)
  - ecran: 96 | vu: gepunktete Noten | attendu: gestrichelte Noten (comme partout ailleurs)
  - ecran: 165 | vu: Szene | attendu: Teil
defauts-avant:
  - T1: autre (validé pile à +16 avec les chiffres, écran 162 ; à +7 rien ne dit ce qui manque, écran 158)
  - T7a: corrigé (écran 8)
  - T7c: corrigé (écran 260)
  - T8: autre (aucun « Bereits erledigt » vu ; l'étape 3/3 du chapitre 12 passe à « Erledigt » sans un mot, écran 208)
  - Clap Reverb-Send: autre (fait et validé via « Ich mach's », écran 215 ; pas de tranche « Clap » dans Mix, écran 211)
