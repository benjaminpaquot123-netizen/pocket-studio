code testé : ab7fbf4e8
joué par : sous-agent neuf

siege: a | profil: moyen | lang: hi | device: phone360 | font: 200 | mode: simple
mission: tuto-facile (tuto grime chapitres 1-5) | buts: - (aucun id donné par le brief)
promesse: aucune promesse donnée par le brief
chapitres:
  - ch1 « गाने की स्पीड और रंग » : compris du premier coup
  - ch2 « सूखे ड्रम्स » : hésitation (cases cachées sous la carte, reste de la mesure 2 hors écran)
  - ch3 « स्क्वेयर बेस » : bloqué (étape 2 sautée, liste : 2/4)
  - ch4 « बर्फ़ीले स्टैब्स » : bloqué (étapes 1 et 2 sautées, liste : 0/2)
  - ch5 « चिपट्यून लीड » : bloqué (étapes 1 et 2 sautées, liste : 0/2)
hesitations:
  - ecran: 30 | cherchais: les cases restantes de la mesure 2 | regarde: « बार 2 · 3 खाने → » n'était visible qu'en faisant défiler
  - ecran: 61 | cherchais: les 5 notes en pointillé de la basse | regarde: le piano est caché sous la carte du tuto, une seule rangée visible
  - ecran: 87 | cherchais: un bouton « copier » pour « अगले बार पर कॉपी करो » | regarde: aucun bouton, le menu « और » est vide au-dessus de la carte
abandon: jamais (étapes sautées avec « अगला » ×2, puis arrivé au chapitre 6)
incidents-banc:
  - geste: 119 | attente: 0 | vu: plafond d'images atteint, fin de partie lue en texte seul (ni écran vide ni attente)
  - geste: - | attente: 0 | vu: hors human-act.py, j'ai seulement lu les fichiers du siège (screen-N.txt, journal.txt, images)
defauts:
  - geste: 79 | ecran.vu: 83 | cible: तुम्हारी बारी: बार 1 के 5 डॉटेड नोट्स दबाओ। पढ़ें | geste.type: touche
    attendu: toucher les notes en pointillé les pose et l'étape se valide | obtenu: le piano fait 70 px sous la carte, les notes sont sur plusieurs rangées, l'étape ne se valide jamais
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 118 (même étape au ch5, 2 notes posées, la 3e fait sauter la vue) | banc?: non | promesse: -
  - geste: 101 | ecran.vu: 104 | cible: तुम्हारी बारी: बार 1 और 2 में हर कॉर्ड का नीचे वाला डॉटेड नोट दबाओ: पूरा कॉर्ड लग जाता है। पढ़ें | geste.type: touche
    attendu: toucher la note pointillée pose l'accord | obtenu: rien ne se passe, puis la vue saute sur une autre rangée et les pointillés disparaissent
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 104 (2e et 3e essai) | banc?: non | promesse: -
  - geste: 87 | ecran.vu: 88 | cible: तुम्हारी बारी: अगले बार पर कॉपी करो। पढ़ें | geste.type: touche
    attendu: trouver un bouton pour copier la mesure 1 sur la mesure 2 | obtenu: aucun bouton visible ; le menu « और » ne montre que « वापस · और », son contenu est sous la carte
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 126 (même étape au ch5, menu « पार्ट: A » essayé) | banc?: non | promesse: -
  - geste: 110 | ecran.vu: 114 | cible: मुझे दिखाओ | geste.type: touche
    attendu: la démo montre où est le bouton, ou fait l'étape comme au ch3 | obtenu: démo puis « लो! अब तुम्हारी बारी।», mais « अगला » dit « यह स्टेप अभी पूरा नहीं हुआ » (au ch3 la même démo validait l'étape)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 98 | ecran.vu: 99 | cible: यह अध्याय अभी पूरा नहीं हुआ: स्टेप 2 बाकी है। उस पर लौटने के लिए “अध्याय” छुओ। | geste.type: touche
    attendu: le compte des étapes concorde | obtenu: écran 99 dit qu'il ne manque que l'étape 2, la liste (écran 133) affiche « 3 स्क्वेयर बेस 2/4 »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 133 | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 15 | cible: 2 बार पर जाओ | geste.type: touche
    attendu: voir les cases en pointillé de la grille | obtenu: les rangées de pads sont sous la carte du tuto, il faut faire glisser la grille (aussi après le passage à la mesure 2, écran 21)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 21 | banc?: non | promesse: -
  - geste: 63 | ecran.vu: 64 | cible: ⌃ | geste.type: touche
    attendu: la flèche replie la carte pour libérer le piano | obtenu: la carte s'ouvre en plein écran, il faut « मैं करूँगा » pour revenir
    symptome: incompris | gravite: contourne-en-1-gestes | revu: oui: ecran 76 | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: गाने की स्केल | geste.type: touche
    attendu: la consigne et la fiche portent le même nom | obtenu: la consigne dit « टोन शीट », la fiche s'appelle « गाने की स्केल »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 110 | ecran.vu: 77 | cible: देखो, मैं करके दिखाता हूँ… | geste.type: touche
    attendu: lire la phrase entière | obtenu: phrase coupée « … » (comme les leçons de chaque étape, « आगे पढ़ो » obligatoire, écran 2)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 2 | banc?: non | promesse: -
mots-faux:
  - ecran: 9 | vu: Key / गाने की key: कोई नहीं | attendu: mot hindi (स्केल / सुर)
  - ecran: 9 | vu: Dorian, Phrygian, Blues | attendu: noms en hindi ou expliqués
  - ecran: 85 | vu: grime (dans « तुम्हारे grime में ») | attendu: ग्राइम
defauts-avant:
  - aucun défaut visé donné par le brief
fin-tranche: oui (ROUTE ligne 704, step=groove_hear done=10)
