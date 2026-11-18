code testé : a0d928cdb7be2961a852750dca8721b975ee8473
joué par : sous-agent neuf
siege: a | profil: jonas-musicien-de-impatient | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: e-ch13, e-ch12, c-croix
promesse: id=P1 | texte: « Ich mach's » (bouton du tuto : l'appli fait l'étape) | ecran: 29 | tenue: non | pourquoi: ch11 étape 1 : le panneau se replie, aucun Teil ajouté, pas de coche (refait à la main #31-#37)
promesse: id=P2 | texte: « Ich mach's » | ecran: 49 | tenue: non | pourquoi: ch12 étape 2 : panneau replié, les 3 Hi-Hat du dernier temps restent ; pas de fausse coche (écran 50), supprimés à la main #51-#53
promesse: id=P3 | texte: « Ich mach's » | ecran: 59 | tenue: oui | pourquoi: ch13 étape 1 : fiche Effekte Pad 5 (Clap) ouverte, Reverb-Send à 30 %, coche après fermeture (écran 60)
promesse: id=P4 | texte: « Ich mach's » | ecran: 63 | tenue: oui | pourquoi: ch13 étape 2 : Melodie / Filter Cutoff, « Pro Teil » + « 1 Takt » choisis, courbe qui monte jusqu'au milieu puis redescend ; coche ensuite
promesse: id=P5 | texte: « Empfohlene Längen setzen » | ecran: 39 | tenue: oui | pourquoi: longueurs 16+16+32+12+4+32+16 = Takt 1/128
oeil-neuf: q1 | ecran: 63 | image: 63 | boutons Loop/Pro Teil/Song, Zeichnen/Linie, loupes, barres de la courbe se lisent comme touchables ; « Glätten » est en texte orange seul (sans contour), on doit deviner que c'est un bouton
oeil-neuf: q6 | ecran: 63 | image: 63 | preuve : la courbe dessinée et « Bravo, Kapitel geschafft! » ; on va ensuite au chapitre suivant en dépliant le panneau
oeil-neuf: q9 | ecran: 63 | image: 63 | à 130 % les étiquettes de l'axe sont coupées : « 18,0 kI », « 849 H », « Schla 1 » (bas de la courbe sous le panneau du tuto)
tuto: ch9 | compris du premier coup | carte « Energie » en partie cachée par la bannière « Kit Techno laden (4 MB) »
tuto: ch10 | compris du premier coup | 7 Teile créés avec les boutons d'étape ; petite contradiction de texte sur le Build-up
tuto: ch11 | hésitation | « Ich mach's » ne fait rien (panneau replié seulement), Teile ajoutés à la main
tuto: ch12 | hésitation | roll ×4 et Becken ok ; « Ich mach's » de l'étape 2 ne supprime pas les Hi-Hat, fait à la main
tuto: ch13 | compris du premier coup | les deux « Ich mach's » font vraiment l'étape, coche seulement après
defauts-avant:
  - e-ch13 | corrigé | ecran: 63 | étape 1 : Reverb-Send à 30 % visible (écran 59) avant la coche (60) ; étape 2 : mode « Pro Teil » + « 1 Takt » ET courbe monte-descend visibles, coche « Erledigt » (62-63)
  - e-ch12 | corrigé | ecran: 43 | un appui long + ×4 = un seul roll (« ×4 », « 4 Wiederholungen ») ; l'éditeur s'ouvre sur « Schläge » (écran 41) et sur « Alle Pads » avec Becken visible (55) ; aucune étape ne nommait un autre onglet
  - c-croix | corrigé | ecran: 47 | croix rouges et cases pointillées dessinées en vue un pad Hi-Hat (image 44) et en vue Alle Pads (image 47)
hesitations:
  - ecran: 29 | cherchais: pourquoi rien n'est ajouté après « Ich mach's » | regarde: panneau replié, liste des Teile inchangée
  - ecran: 49 | cherchais: si les Hi-Hat ont été effacés | regarde: les 3 croix rouges toujours là
  - ecran: 59 | cherchais: ce que veut dire « Akkorde ✓ · Snare: danach » sur la fiche du Clap | regarde: encadré pointillé autour du Reverb-Send
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun outil hors human-act.py ; Read seulement sur mes propres captures screen-N-grille.png du siège
defauts:
  - geste: 29 | ecran.vu: 30 | cible: bouton (doigt qui touche) «   Ich mach's » @ 450,1042 | geste.type: touche
    attendu: l'appli ajoute les Teile de l'étape et la coche | obtenu: le panneau se replie, rien n'est ajouté, pas de coche
    symptome: rien | gravite: contourne-en-7-gestes | revu: oui: ecran 49 (même chose au ch12) | banc?: non | promesse: P1
  - geste: 49 | ecran.vu: 50 | cible: bouton (doigt qui touche) «   Ich mach's » @ 450,1042 | geste.type: touche
    attendu: les 3 Hi-Hat du dernier temps sont supprimés | obtenu: panneau replié, les 3 croix restent, aucune coche
    symptome: rien | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: P2
  - geste: 6 | ecran.vu: 7 | cible: bouton « Kit Techno laden (4 MB) » | geste.type: touche
    attendu: « Schau dir die Karte an » : la carte Energie lisible | obtenu: longueurs et noms des Teile cachés sous la bannière jaune
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 63 | cible: texte « 18,0 kHz » @ 81,817 | geste.type: glisse
    attendu: étiquettes d'axe entières | obtenu: « 18,0 kI », « 849 H », « Schla » coupés à 130 %
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 58 | ecran.vu: 59 | cible: texte « Akkorde ✓ · Snare: danach » @ 226,1225 | geste.type: touche
    attendu: un repère sur le Clap, le pad de l'étape | obtenu: parle d'Akkorde et de Snare sur la fiche du Clap, incompréhensible
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 55 | cible: icône « Intro Takt 1/128 » ▾ @ 264,123 | geste.type: touche
    attendu: l'en-tête indique le Teil en cours (Build-up, puis Drop) | obtenu: reste « Intro » alors qu'on édite Build-up/Drop
    symptome: texte-faux | gravite: finition | revu: oui: ecran 63 | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 47 | cible: bouton « Alle Pads » [sélectionné] @ 770,400 | geste.type: touche
    attendu: le roll ×4 du Clap visible aussi en vue Alle Pads | obtenu: case Clap sans marque de roll
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 19 | vu: « der ganze Groove spielt darin. Darin: … » | attendu: une seule fois « darin »
  - ecran: 23 | vu: « sein letzter Schlag verstummt » (l'étape 22 disait « die Stille … kommt mit den Übergängen ») | attendu: une seule version
