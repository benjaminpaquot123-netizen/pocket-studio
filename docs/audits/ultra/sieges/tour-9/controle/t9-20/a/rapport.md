code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: moyen | lang: de | device: phone | font: 200 | mode: simple
mission: tuto-facile (tuto disco, chapitres 5 à 13) | buts: -
chapitres:
  - ch5 Die Streicher : hésitation (3e note gestrichelt cachée hors écran, trouvée après ~20 gestes)
  - ch6 Dein erster Teil: der Refrain : compris du premier coup
  - ch7 Die Teile verstehen : compris du premier coup
  - ch8 Bau deine Teile : compris du premier coup
  - ch9 Ordne deine Teile : hésitation (boutons + sortis de l'écran après chaque ajout)
  - ch10 Die Übergänge : bloqué puis débloqué par « Zeig's mir » (il fallait un Roll ×4, pas 4 touches)
  - ch11 Die Effekte : compris du premier coup
  - ch12 Der Mix: die Lautstärken : compris du premier coup
  - ch13 Exportiere deinen Track : compris du premier coup (fin à l'écran 109, 108 gestes)
hesitations:
  - ecran: 2 | cherchais: les 3 notes gestrichelt du Takt 1 et 2 | regarde: 9 lignes de grille sous le panneau du tuto, glissé dans tous les sens
  - ecran: 57 | cherchais: le bouton « + C · Groove » après le premier ajout | regarde: la liste redescend, les + sont sous le bord, il faut glisser
  - ecran: 82 | cherchais: pourquoi l'étape ne passe pas après les 4 Kästchen à 100 % | regarde: Weiterlesen puis Zeig's mir
abandon: jamais
incidents-banc:
defauts:
  - geste: 81 | ecran.vu: 88 | cible: Du bist dran: Ende des Build-ups, Pad „Snare“: Tippe auf die 4 Kästchen des letzten Schlags. Lesen | geste.type: touche
    attendu: les 4 cases du dernier temps touchées, l'étape est validée | obtenu: rien ne valide ; Zeig's mir montre en fait un Roll ×4 sur la dernière case (appui-long > Roll > ×4)
    symptome: texte-faux | gravite: contourne-en-8-gestes | revu: oui: ecran 92 | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 22 | cible: Du bist dran: Tippe auf die 3 langen gestrichelten Noten in Takt 1 und 2. | geste.type: glisse
    attendu: voir les 3 notes à toucher | obtenu: la 3e note est hors écran à droite, aucune indication, la grille ne montre que ~9 lignes sous le panneau
    symptome: cache | gravite: contourne-en-20-gestes | revu: non | banc?: non | promesse: -
  - geste: 56 | ecran.vu: 57 | cible: Am Ende hinzufügen: | geste.type: glisse
    attendu: enchaîner les 9 ajouts « + Teil » | obtenu: après chaque ajout la vue saute, les boutons + passent sous le bord, un glissé de plus à chaque fois
    symptome: hors-pouce | gravite: contourne-en-8-gestes | revu: oui: ecran 71 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: Noch 1 gestrichelte Note zu tippen | geste.type: glisse
    attendu: l'en-tête Melodie · Takt reste en haut | obtenu: après un glissé vertical l'en-tête et les puces Takt disparaissent pour de bon, même en changeant d'onglet
    symptome: perdu | gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 2 | cible: Noch 1 gestrichelte Note zu tippen | geste.type: touche
    attendu: « Noch 3 » comme la consigne (3 Noten) | obtenu: « Noch 1 » alors que j'en avais encore 3 à toucher
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 83 | ecran.vu: 84 | cible: Weiterlesen | geste.type: touche
    attendu: lire la consigne en entier | obtenu: le texte déplié est coupé en haut, le début de la phrase manque
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 32 | ecran.vu: 33 | cible: Teil „Drop“ benennen | geste.type: touche
    attendu: le chapitre « der Refrain » nomme la partie Refrain | obtenu: la partie s'appelle « Drop », deux mots pour la même chose
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 95 | ecran.vu: 96 | cible: Beispiel übersprungen, ohne es anzuhören. | geste.type: touche
    attendu: pas de reproche, j'avais lancé l'exemple (il jouait encore) | obtenu: « übersprungen, ohne es anzuhören »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 108 | ecran.vu: 109 | cible: Kapitel 2/13 · Die Drums Bevor es losgeht: Das Disco-Kit ist geladen, ein trockenes Schlagzeuger-Kit. Du musst nichts einstellen. | geste.type: touche
    attendu: le kit est chargé comme promis | obtenu: chapitres 11 à 13 proposent encore « Kit Disco / Funk laden (2 MB) » / « Später »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 102 | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 51 | cible: Dein Ergebnis anhören | geste.type: touche
    attendu: deux boutons voisins de même taille de texte | obtenu: « Dein Ergebnis anhören » et « Beispiel anhören » côte à côte, tailles de texte différentes (idem étiquettes du Mix écran 100 : « Bass » plus gros que « Akkorde »)
    symptome: rien | gravite: finition | revu: oui: ecran 100 | banc?: non | promesse: -
mots-faux:
  - ecran: 51 | vu: Offene | attendu: offene
  - ecran: 91 | vu: Velocity | attendu: Anschlagstärke
  - ecran: 91 | vu: Roll | attendu: Wirbel
defauts-avant:
fin-tranche: non (dernier step=share, done=31 ; chapitre 13 « Schritt 1/1 » sans Bravo final)
