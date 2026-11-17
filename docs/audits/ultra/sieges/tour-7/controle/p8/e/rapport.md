code testé : ede109d8ada92d293e0cfd3b777f53b38e2e3b68
joué par : sous-agent neuf

siege: e | profil: produzent-de-pro | lang: de | device: tablette-8 | font: 130 | mode: pro
mission: tuto-pro ch. 8-13 | buts: ch8-12-finis, ch13-commence, mots-allemands
promesse: id=pro-boutons | texte: „Ich mach's“ / „Zeig's mir“ | ecran: 69 | tenue: oui | pourquoi: ch. 10, 11 (Empfohlene Längen setzen, #53), 12 et 13 (Ich mach's ouvre les Effekte, #67) : chaque étape a un bouton ou Zeig's mir, et le rond coché « Geschafft » vient après
promesse: id=partie-etape | texte: Noch 1 gestricheltes Kästchen hier | ecran: 63 | tenue: oui | pourquoi: croix rouges seulement dans Build-up (58), Becken sans pointillés dans Build-up (62), case pointillée seulement dans Drop (63)
promesse: id=a-faire | texte: es ist noch leer | ecran: 63 | tenue: oui | pourquoi: le dernier coup du Clap était ×2, pas ×3/×4 (55) ; la 1re case Becken du Drop est vide et pointillée, comme le dit l'étape
promesse: id=kit-banniere | texte: Kit laden | ecran: 5 | tenue: oui | pourquoi: la bannière du kit reste sous le texte de l'étape, la carte des parties reste visible (screens 1, 4, 5, 18)
oeil-neuf: q1 | ecran: 69 | image: 69 | les pads, chips, onglets et la barre « Geschafft » ont l'air touchables ; « Füllen alles 1/2 1/4 ◀ ▶ Löschen » et le « − » des 16 Steps sont du texte orange sans cadre, on doit deviner qu'ils se touchent
oeil-neuf: q6 | ecran: 69 | image: 69 | preuve : la case 1 du Becken est pleine, et la barre verte « Geschafft » propose « Nächster Schritt » ; le Reverb-Send à 61 % (#68) ne laisse aucune trace sur cet écran
oeil-neuf: q9 | ecran: 69 | image: 69 | « Mein Proje… » et « Akkorde Pl… » coupés ; le titre « STEPS: BECKEN · DROP » est caché sous « Teil: D · Drop »
hesitations:
  - ecran: 39 | cherchais: le bouton qui fait l'étape 1 du ch. 11 | regarde: « Ich mach's » replie seulement le panneau, rien n'est ajouté
  - ecran: 44 | cherchais: « + E · Break » | regarde: les boutons « Am Ende hinzufügen » descendent après chaque ajout, mon tap a ajouté A · Groove et lancé la lecture
  - ecran: 52 | cherchais: où régler les Takte d'une partie | regarde: − désactivé à ×1, il n'y a que « Empfohlene Längen setzen »
abandon: jamais
incidents-banc:
  - geste: 4 | attente: 0 | vu: « Plafond d'images atteint » une fois ; outils hors human-act : Read sur consignes-testeur.md et sur mes images, cat sur ce rapport
defauts:
  - geste: 15 | ecran.vu: 18 | cible: Beispiel übersprungen, ohne es anzuhören. | geste.type: touche
    attendu: la légende de la carte des parties est lisible | obtenu: le message reste affiché et coupe les noms Intro…Ende à moitié
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 19 | cible: Beispiel übersprungen, ohne es anzuhören. | geste.type: touche
    attendu: l'écran « Kapitel geschafft » sans vieux message | obtenu: « Beispiel übersprungen » reste affiché à côté du « Bravo »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 55 | cible: STEPS: TECHNO · CLAP · BUILD-UP | geste.type: touche
    attendu: le titre de la grille et « Alle Pads » visibles | obtenu: le titre, le « ? » et « Alle Pads » sont cachés sous la bande « Teil: F · Build-up » (pareil aux écrans 63 et 69)
    symptome: cache | gravite: finition | revu: oui: ecran 63 | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 55 | cible: bouton « F · Build-up » ▾ | geste.type: touche
    attendu: le nom du projet et de la partie en entier | obtenu: « Mein Pr… » et « F · Build-… » coupés en haut, à 130 %
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 63 | banc?: non | promesse: -
  - geste: 43 | ecran.vu: 44 | cible: bouton « + A · Groove » | geste.type: touche
    attendu: les boutons d'ajout restent en place pendant que je remplis la liste | obtenu: ils descendent après chaque ajout ; mon tap ajoute A · Groove (pas demandé), le suivant lance la lecture
    symptome: incompris | gravite: contourne-en-3-gestes | revu: oui: ecran 48 | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 39 | cible: bouton (doigt qui touche) «   Ich mach's » | geste.type: touche
    attendu: comme au ch. 13, le bouton ouvre l'endroit où agir | obtenu: au ch. 11, il replie juste le panneau, rien ne s'ouvre
    symptome: rien | gravite: finition | revu: oui: ecran 67 | banc?: non | promesse: pro-boutons
  - geste: 67 | ecran.vu: 67 | cible: Akkorde ✓ · Snare: danach | geste.type: touche
    attendu: l'étape parle du Pad « Techno · Clap » | obtenu: l'encadré dit « Snare: danach » sur la feuille du Clap
    symptome: texte-faux | gravite: finition | revu: oui: ecran 68 | banc?: non | promesse: -
mots-faux:
  - ecran: 20 | vu: Tippe auf „Teil „Intro“ erstellen“ | attendu: Tippe auf „Teil Intro erstellen“
  - ecran: 27 | vu: der ganze Groove spielt darin. Darin: | attendu: der ganze Groove spielt darin:
  - ecran: 6 | vu: Kit: Eigenes (juste après Techno-Kit laden) | attendu: Kit: Techno
  - ecran: 25 | vu: ohne Becken, Melodie | attendu: ohne Becken und Riff
  - ecran: 54 | vu: Drück lange auf den letzten Schlag vor der letzten Zählzeit | attendu: Halte den letzten Schlag vor Zählzeit 4 gedrückt
defauts-avant:
  - e-V1 | corrigé | ecran: 69 | Ich mach's (ch. 13) ouvre les Effekte puis coche ; Empfohlene Längen setzen (ch. 11) coche aussi (53)
  - e-V2 | corrigé | ecran: 63 | roll final pas posé (×2 à mettre en ×4, 55), Becken du Drop vide sur sa 1re case
  - e-V3 | corrigé | ecran: 63 | pointillés seulement dans Drop ; pas dans Build-up (62)
  - e-V4 | corrigé | ecran: 7 | ch. 8 terminé sans accroc (écrans 1-7)
