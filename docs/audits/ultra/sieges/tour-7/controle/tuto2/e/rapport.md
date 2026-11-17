code testé : f1b9fdb1fdb38b1824bbf4bb43318cc6f9291277
joué par : sous-agent neuf

siege: e | profil: jonas-musicien-de-impatient | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro (Techno, fin du chapitre 8, écoutes sautées) | buts: -
promesse: id=D7 | texte: Beispiel übersprungen, ohne es anzuhören. | ecran: 2 | tenue: oui | pourquoi: après « Weiter » sans écouter, la ligne verte le dit, et l'écran 3 la répète sous « Bravo, Kapitel geschafft! »
oeil-neuf: q1 | ecran: 7 | image: 3 | tout ce qui a un contour se touche ; seule la flèche ← nue (sans mot) à côté de « Meinen Song anhören » se devine
oeil-neuf: q6 | ecran: 7 | image: 3 | preuve : « + A · Groove » dans DER SONG et « Erledigt » ; suite : un seul gros bouton « Nächstes Kapitel », clair
oeil-neuf: q9 | ecran: 7 | image: 3 | titre « SONG-EINSTELLUNGEN » coupé par la carte du tuto (écran 7) ; rien d'autre coupé
hesitations:
  - ecran: 4 | cherchais: le bon bouton pour revenir où j'étais | regarde: « Nächster Schritt » et « Weiter » font la même chose, deux boutons
abandon: jamais
incidents-banc:
  - geste: 7 | attente: 0 | vu: « regarde image » refusé (plafond 1 pour 5) ; « attends » ne compte pas comme geste, pas d'image du dernier écran
defauts:
  - geste: 3 | ecran.vu: 4 | cible: Diesen Schritt hast du schon gemacht. | geste.type: touche
    attendu: en revenant sur l'étape d'écoute sautée, rappel que l'exemple a été sauté | obtenu: « Schon erledigt! Diesen Schritt hast du schon gemacht. » alors que je ne l'ai pas écouté
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: Dein Song hatte schon, was dieser Schritt verlangt: nichts zu tun. | geste.type: touche
    attendu: « Erledigt » avec le rappel « Beispiel übersprungen » comme à l'écran 3 | obtenu: le message dit que mon song l'avait déjà (faux : je l'ai nommé moi-même) et le rappel du saut a disparu
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: D7
  - geste: 3 | ecran.vu: 4 | cible: bouton « Nächster Schritt   » → | geste.type: touche
    attendu: un seul bouton pour avancer | obtenu: « Nächster Schritt » (gros) et « Weiter → » (bas) côte à côte font la même chose
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: bouton « 8 Dein erster Teil: der Groove, deine Grundschleife » (rond coché) | geste.type: touche
    attendu: une marque discrète « exemple sauté » ou au moins pas la même coche que les chapitres faits en entier | obtenu: coche pleine, rien ne dit qu'une écoute a été sautée
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 5 | vu: Dein Song hatte schon, was dieser Schritt verlangt: nichts zu tun. | attendu: Dein Song erfüllt diesen Schritt schon – hier gibt es nichts zu tun.
defauts-avant:
  - D7 | corrigé | ecran: 3 | écran 2 : « Beispiel übersprungen, ohne es anzuhören. » après Weiter sans écouter ; écran 3 : « Bravo, Kapitel geschafft! » + même ligne, un seul bouton « Nächstes Kapitel », pas de « Weiter » à côté (mais après ← puis Weiter, écran 5, le rappel disparaît : voir defauts)
