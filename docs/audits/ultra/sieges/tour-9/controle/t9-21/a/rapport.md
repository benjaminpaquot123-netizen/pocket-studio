code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: curieux | lang: it | device: phone360 | font: 100 | mode: simple
mission: tuto-facile (tuto dnb, chapitres 1 à 8) | buts: - (aucun id donné par le brief)
promesse: id=- | texte: « Bravo, capitolo finito! » | ecran: 124 | tenue: oui | pourquoi: les 8 chapitres finis en 124 gestes, sans blocage
chapitres:
  - ch1 Tempo/tonalità : compris du premier coup
  - ch2 : compris du premier coup
  - ch3 : compris du premier coup
  - ch4 : compris du premier coup
  - ch5 basso : hésitation (bouton « Copia sulle battute successive » sans effet, compteur de notes trompeur)
  - ch6 accordi : compris du premier coup (même bouton mort, contourné par « Copia » dans la grille)
  - ch7 riff : hésitation (compteur « Ancora N note » qui change sans que je touche une note)
  - ch8 groove : compris du premier coup
hesitations:
  - ecran: 89 | cherchais: comment copier la battuta 2 | regarde: le gros bouton du bas, puis le petit « Copia » dans la grille
  - ecran: 110 | cherchais: combien de notes il reste | regarde: le compteur passe de 1 à 2 puis 3 sans rien poser
  - ecran: 84 | cherchais: quelles notes toucher | regarde: le texte dit 3 notes, la case dit 2
abandon: jamais
incidents-banc:
defauts:
  - geste: #88 | ecran.vu: 89 | cible: bouton « Copia sulle battute successive » → @ 270,870 | geste.type: touche
    attendu: la battuta copiée sur les suivantes, chapitre fini | obtenu: rien ne bouge (idem #95 écran 96 dans le panneau ouvert, #114 écran 115)
    symptome: rien
    gravite: contourne-en-1-gestes | revu: oui: ecran 96 et 115 | banc?: non | promesse: -
  - geste: #110 | ecran.vu: 110 | cible: bouton « Melodia · battuta 2/8 ↓ Ancora 2 note tratteggiate da toccare Tocca qui » | geste.type: touche
    attendu: un compteur fixe des notes qui restent | obtenu: 1 (écran 109), 2 en repliant le panneau (110), 3 après « Tocca qui » (111), sans rien poser
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 105 (2 devient 3 au ch7 passo 2) | banc?: non | promesse: -
  - geste: #108 | ecran.vu: 109 | cible: texte « Tocca a te: scrivi le 3 note della battuta 2. » | geste.type: touche
    attendu: la case dit aussi 3 notes | obtenu: la case dit « Ancora 1 nota tratteggiata » (même écart au ch5, écran 84 : 3 contre 2)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 84 | banc?: non | promesse: -
  - geste: #107 | ecran.vu: 108 | cible: bouton « Melodia · battuta 1/8 Ancora 3 note tratteggiate, più avanti → Tocca qui » | geste.type: touche
    attendu: « fatto » puisque le pas est « Riuscito » | obtenu: la case me dit qu'il reste 3 notes et « Tocca qui » (même chose au ch5)
    symptome: incompris
    gravite: finition | revu: oui: ecran ch5 après le pas 2 | banc?: non | promesse: -
  - geste: #59 | ecran.vu: 59 | cible: bouton « Battuta 2 · 1 casella → » | geste.type: touche
    attendu: la puce de la battuta 2 bien visible | obtenu: pâle, presque cachée sous l'en-tête « Parte: A », le panneau tuto couvre la moitié de la grille
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #124 | ecran.vu: 124 | cible: texte « Bravo, capitolo finito! » [coupé] | geste.type: touche
    attendu: le message de fin lisible en entier | obtenu: la case verte est coupée en bas, le texte caché derrière « Continua a leggere »
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 8 | vu: « “Lo faccio io” apre la scheda Tono » | attendu: « Tonalità » (la fiche s'appelle « Tonalità del brano »)
  - ecran: 123 | vu: « tocca “Dai il nome “Groove” alla parte” » | attendu: guillemets imbriqués : « Dai il nome «Groove» alla parte »

fin-tranche: non (dernier step=groove_name, done=21 ; aucune ligne ROUTE step=brick, « Bravo, capitolo finito! » vu au geste #124 sans passer au chapitre suivant)
