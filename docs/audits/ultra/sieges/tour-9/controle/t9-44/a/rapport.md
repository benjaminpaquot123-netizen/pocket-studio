code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: musicien-confirme | lang: nl | device: phone | font: 200 | mode: pro
mission: tuto-pro (liquid_dnb, chapitres 7-11, départ chapitre 7) | buts: - (brief sans ids)
promesse: id=- | texte: Verder lezen | ecran: 3 | tenue: non | pourquoi: le texte de la consigne reste coupé après « Verder lezen » (2 fois sur l'étape, idem chapitre 8)
promesse: id=- | texte: Melodie · maat 1/8 Nog 1 gestippelde noot om aan te tikken | ecran: 35 | tenue: non | pourquoi: la note pointillée est cachée sous le panneau du tuto, impossible de la toucher
chapitres:
  - ch7 De piano : hésitation (consigne illisible, premier tap sur « Kopiëren » ignoré, Chorus posé sans souci)
  - ch8 De bel die antwoordt : bloqué à l'étape 2/3 (grille de notes cachée)
  - ch9, ch10, ch11 : pas atteints (le banc a arrêté le siège au geste 35, voir incidents-banc)
hesitations:
  - ecran: 2 | cherchais: la suite de la consigne (quels accords, quelles basses) | regarde: une seule ligne « Akkoorden (‘FM-Rhodes’… », Verder lezen ne déplie rien
  - ecran: 15 | cherchais: les faders de Mix | regarde: tranches coupées sous le panneau du tuto, nom de piste « Drum s » cassé en deux
  - ecran: 31 | cherchais: la grille de notes du spoor Melodie | regarde: une bande de 10 px entre l'en-tête et le panneau du tuto
abandon: ecran 35 | parce que la grille de notes du spoor Melodie est entièrement cachée par le panneau du tuto replié (police 200 %), aucune note à toucher
incidents-banc:
  - geste: 34 | attente: 0 | vu: le banc a fermé le siège tout seul après 35 gestes (« rien ne change (5 écrans identiques) ») alors que le budget était de 167 ; je cherchais encore un moyen d'atteindre la note
defauts:
  - geste: 1 | ecran.vu: 3 | cible: icône « Verder lezen » ⌄ | geste.type: touche
    attendu: la consigne entière de l'étape s'affiche (accords Am/F, G/E, C/A) | obtenu: rien ne bouge, la consigne reste coupée à une ligne avec « … »; au ch8 (#26, #28) le cadre montre le milieu du texte, début et fin coupés
    symptome: texte-coupe
    gravite: contourne-en-4-gestes | revu: oui: ecran 29 | banc?: non | promesse: -
  - geste: 31 | ecran.vu: 35 | cible: bouton « Melodie · maat 1/8 Nog 1 gestippelde noot om aan te tikken » [coupé] | geste.type: touche
    attendu: voir la grille de notes et toucher la note pointillée | obtenu: le panneau du tuto replié couvre toute la grille, seule une bande de 10 px reste ; ni tap ni glissé (grille, page, panneau) ne la dégagent
    symptome: cache
    gravite: mission-impossible | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 10 | ecran.vu: 11 | cible: bouton ⊕ «   Kopiëren naar de volgende maten » | geste.type: touche
    attendu: les mesures suivantes se remplissent | obtenu: rien au premier tap (milieu du bouton, entre les deux lignes), ça marche au second (#11)
    symptome: rien
    gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: texte « Akkoorden » [coupé] | geste.type: retour
    attendu: les tranches de Mix lisibles avec leurs faders | obtenu: noms coupés (« Drum s », « Ak ord ») et faders cachés sous le panneau du tuto replié
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 11 | cible: texte « Het spoor ‘Akkoorden’ speelt op elektrische piano: dat is de jazzy kleur van liquid. » [coupé] | geste.type: touche
    attendu: l'explication « Waarom? » lisible | obtenu: le texte est tronqué à mi-ligne (lettres coupées horizontalement) et le « … » chevauche « speelt »
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: bouton « Voor: Pluck » [sélectionné] | geste.type: touche
    attendu: des ambiances pour une piste d'accords FM-Rhodes | obtenu: le filtre proposé est « Voor: Pluck », sans rapport avec le Rhodes
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 1 | vu: Feedbackmodus (coupé « Feedbackm… ») | attendu: mot entier visible
defauts-avant:
fin-tranche: non (dernier step=lead_do, done=22, chapters=1,2,3,4,5,6,7)
