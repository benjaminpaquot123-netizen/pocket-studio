siege: a | profil: t5-Aa debutant-indien-petit-tel-130 | lang: hi | device: phone360 | font: 130 | mode: simple
mission: premier-morceau | buts: son, partie, partage
promesse: id=t5-Aa-1 | texte: मेरी मेलोडी रिकॉर्ड करो | ecran: 40 | tenue: oui | pourquoi: je regardais « पार्ट: A · इंट्रो », la grille de A montre « नोट ग्रिड: 3 नोट्स » (mais je ne peux regarder aucune autre partie, voir défaut #27)
promesse: id=t5-Aa-2 | texte: रिकॉर्डिंग रोको | ecran: 35 | tenue: oui | pourquoi: la demande « Allow BenStudio to send you notifications? » n'arrive qu'après « रोको » (fin de la prise), rien pendant la prise
promesse: id=t5-Aa-3 | texte: मेलोडी Marimba | ecran: 31 | tenue: non | pourquoi: la piste « मेलोडी » est bien en Marimba, mais « मेरी मेलोडी रिकॉर्ड करो » crée une piste « मेरी मेलोडी FM पियानो » : ma mélodie ne joue pas le Marimba choisi
promesse: id=t5-Aa-4 | texte: सिंपल शुरुआत के लिए: 5 टैब, बड़े बटन और रोज़ के शब्द। | ecran: 10 | tenue: non | pourquoi: en Simple, mots anglais en lettres latines : SNARE, Kick, groove, Hi-hat, Octave, Crash, Solo (voir mots-faux)
hesitations:
  - ecran: 28 | cherchais: mettre ma mélodie dans le refrain (मुखड़ा) | regarde: la liste des parties, je choisis C mais l'écran reste sur A
  - ecran: 39 | cherchais: voir mes notes dans « नोट्स » | regarde: la grille est sous la bande « गाइड », mon glissé a touché le guide au lieu de défiler
  - ecran: 16 | cherchais: Marimba dans la liste des sons | regarde: 4 glissés dans une très longue liste (6-op FM, Formants…) avant de le trouver
abandon: jamais
incidents-banc:
defauts:
  - geste: 27 | ecran.vu: 28 | cible: C · मुखड़ा | geste.type: touche
    attendu: l'éditeur passe sur « पार्ट: C · मुखड़ा » | obtenu: reste « पार्ट: A · इंट्रो » ; pareil avec B (#49), et avec la nouvelle partie M (#53)
    symptome: rien
    gravite: mission-impossible | revu: oui: ecran 50 | banc?: non | promesse: t5-Aa-1
  - geste: 51 | ecran.vu: 53 | cible: + नया पार्ट (A · इंट्रो की कॉपी) | geste.type: touche
    attendu: une nouvelle partie avec un nom (ex. « M · इंट्रो 2 ») | obtenu: une partie « M » sans nom, et l'éditeur reste sur A
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: मेरी मेलोडी FM पियानो | geste.type: touche
    attendu: ma mélodie enregistrée joue le Marimba choisi pour la mélodie à l'étape 9 | obtenu: nouvelle piste « मेरी मेलोडी » en FM पियानो
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t5-Aa-3
  - geste: 9 | ecran.vu: 10 | cible: SNARE | geste.type: touche
    attendu: un titre en hindi comme à l'étape d'avant « किक (बेस ड्रम) » | obtenu: titre anglais en majuscules « SNARE », et « Kick », « groove » dans le texte
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Aa-4
  - geste: 20 | ecran.vu: 21 | cible: अंतरा · आवाज़ के लिए जगह: Kick, बेस और मेलोडी, बिना कॉर्ड्स | geste.type: touche
    attendu: la liste dessous sans accords | obtenu: la liste dessous contient « Pad » (la piste des accords, « PAD / कॉर्ड्स ») : le texte se contredit
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 30 | cible: मेलोडी Marimba | geste.type: touche
    attendu: noms de pistes lisibles à 130 % | obtenu: deuxième ligne des pastilles coupée en bas (« गोल बेस », « FM Rhodes », « Marimba » à moitié visibles)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 22 | cible: Lo-fi hip-hop | geste.type: touche
    attendu: le nom de mon projet visible en haut | obtenu: seulement « L… »
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 30 | cible: कीबोर्ड | geste.type: touche
    attendu: tout le clavier visible | obtenu: le bas des touches blanches est caché par la bande « गाइड · एक साउंड बदलो »
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 40 | cible: E♭4 | geste.type: glisse
    attendu: mêmes noms de notes que le clavier (« D# 4 ») | obtenu: la grille écrit « E♭4 », le clavier « D# 4 » : deux noms pour la même note
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 43 | ecran.vu: 44 | cible: लेवल: तेज़, क्लब म्यूज़िक जैसा | geste.type: touche
    attendu: le même niveau qu'annoncé avant l'envoi (« लेवल: स्ट्रीमिंग म्यूज़िक जैसा (−14) ») | obtenu: bilan « तेज़, क्लब म्यूज़िक जैसा », plus « लक्ष्य पर (ऑटो (स्टाइल)) » aux parenthèses doublées
    symptome: texte-faux
    gravite: finition | revu: non | banc?: son | promesse: -
mots-faux:
  - ecran: 10 | vu: SNARE | attendu: स्नेयर
  - ecran: 11 | vu: CLOSED HI-HAT | attendu: बंद हाई-हैट
  - ecran: 15 | vu: Octave | attendu: ऑक्टेव (le texte au-dessus l'écrit déjà ainsi)
  - ecran: 21 | vu: Kick | attendu: किक (la même carte écrit aussi « किक »)
  - ecran: 27 | vu: Crash | attendu: क्रैश
  - ecran: 41 | vu: Solo | attendu: सोलो
