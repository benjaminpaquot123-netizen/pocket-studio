code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: moyen | lang: de | device: tablet8 | font: 130 | mode: simple
mission: tuto-facile (disco, chapitres 1 a 4) | buts: chap1-tempo-tonalite, chap2-drums, chap3-bass (+ chap4-gitarre)
promesse: id=- | texte: aucune promesse fournie dans la consigne | ecran: - | tenue: pas vue | pourquoi: aucune promesse fournie
chapitres:
  - chap 1 « Tempo und Farbe des Tracks » : compris du premier coup (ecrans 1-19)
  - chap 2 « Die Drums » : hesitation (noms des pads du tuto != noms dans l'editeur, ecrans 47, 52)
  - chap 3 « Der Funk-Bass » : hesitation (compteur de notes contradictoire, ecrans 61-80)
  - chap 4 « Die Gitarre » : compris du premier coup (ecrans 94-112, « Bravo, Kapitel geschafft! »)
hesitations:
  - ecran: 47 | cherchais: le pad « Congas » | regarde: l'editeur affiche « Trommelrand », j'ai tape quand meme les cases pointillees
  - ecran: 67 | cherchais: combien de notes il reste dans Takt 1 | regarde: bandeau « Noch 9 gestrichelte Noten, weiter oben » apres 6 notes tapees
  - ecran: 70 | cherchais: si l'etape est finie | regarde: tuto « Geschafft » mais bandeau « Noch 7 gestrichelte Noten, weiter hinten »
abandon: jamais
incidents-banc:
  - geste: - | attente: - | vu: aucun ecran vide ; le texte du banc doublait parfois une ligne (ex. « • BPM… » deux fois) alors que l'image l'affiche une seule fois
  - geste: - | attente: - | vu: outils hors human-act.py : lecture de start.txt, meta-1.txt, journal.txt et des PNG d'ecran du siege (cat/tail/grep/Read), plus les deux gabarits demandes
defauts:
  - geste: #46 | ecran.vu: 47 | cible: « Du bist dran: Tippe beim Pad der Congas auf die 4 gestrichelten Kästchen: Schläge von Hand. » | geste.type: touche
    attendu: l'editeur montre le pad Congas | obtenu: l'editeur montre « Trommelrand », les coups vont sur ce pad
    symptome: texte-faux | gravite: finition | revu: oui: ecran 52 | banc?: non | promesse: -
  - geste: #51 | ecran.vu: 52 | cible: « Tippe beim Pad des Tamburins auf die 8 gestrichelten Kästchen » | geste.type: touche
    attendu: le pad Tamburin | obtenu: l'editeur montre « Maracas »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #55 | ecran.vu: 56 | cible: « Beispiel übersprungen, ohne es anzuhören. » | geste.type: touche
    attendu: pas de reproche, j'avais ecoute l'exemple (il jouait encore, 3 s restantes, ecran 55) | obtenu: message dit que je l'ai saute sans l'ecouter
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #66 | ecran.vu: 67 | cible: « Noch 9 gestrichelte Noten, weiter oben ↑ » | geste.type: touche
    attendu: le compteur baisse et suit le tuto (« Tippe auf die 8 Noten in Takt 1 ») | obtenu: 6 → apres 6 notes 9 → 2 → « Noch 7 … weiter hinten » alors que le tuto dit Geschafft ; idem etape 3 (« 7 Noten » vs « Noch 4 » puis « Noch 3 … weiter unten ») et ecran 80 (Bravo + « Noch 1 gestrichelte Note »)
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecrans 75, 80 | banc?: non | promesse: -
  - geste: #4 | ecran.vu: 5 | cible: « Kit: Disco / Funk » | geste.type: regarde
    attendu: bouton et son aide lisibles en entier | obtenu: bouton et texte d'aide coupes par le panneau du tuto
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #4 | ecran.vu: 5 | cible: « Disco / Funk · Große Trom... » | geste.type: regarde
    attendu: nom du pad complet (police 130 %, tablette) | obtenu: nom tronque
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #2 | ecran.vu: 3 | cible: « Später » | geste.type: regarde
    attendu: rien a choisir pendant le chargement du kit | obtenu: « Später » reste visible pendant le chargement
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 16 | vu: « B » et « H » a cote de « E♭ » / « A♭ » | attendu: notation allemande coherente (B pour si bemol ou partout bemols)
defauts-avant:
  - aucun defaut vise fourni dans la consigne
gestes: 113 / 150 (fin comprise) ; mission finie (chapitres 1 a 4 « geschafft »)

fin-tranche: non (dernier step=pad_2, done=18 ; done=18 = les 18 étapes de la tranche, mais aucune ligne ROUTE step=lead_do : le joueur a envoyé « fin » au geste suivant, sans nouvel écran)
