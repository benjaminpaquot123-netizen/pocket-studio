siege: a | profil: t5-Aa debutant-indien-petit-tel | lang: hi | device: phone360 | font: 130 | mode: simple
mission: premier-morceau | buts: son, partie, partage
promesse: id=t5-Aa-1 | texte: « मेरी मेलोडी रिकॉर्ड करो » / « पार्ट: A · इंट्रो » | ecran: 17 | tenue: oui | pourquoi: la prise (« तुम्हारा टेक: 3 नोट्स ») reste dans la partie A · इंट्रो affichée, pas dans une autre partie
promesse: id=t5-Aa-2 | texte: « Allow BenStudio to send you notifications? » | ecran: 16 | tenue: oui | pourquoi: la demande n'apparaît qu'après « रोको » (fin de prise, geste #15), pas pendant
promesse: id=t5-Aa-3 | texte: « मेलोडी Marimba » / « मेरी मेलोडी FM पियानो » | ecran: 12 | tenue: non | pourquoi: j'ai choisi Marimba pour « मेलोडी », mais l'enregistrement part sur une piste « मेरी मेलोडी » en FM पियानो
promesse: id=t5-Aa-4 | texte: « गाइड · Kick (“बूम”) » | ecran: 17 | tenue: non | pourquoi: en Simple restent des mots anglais : Kick, Solo, REC, Modal, 6-op FM, Snare
hesitations:
  - ecran: 10 | cherchais: le bouton « कीबोर्ड खोलो » que le guide me demande de toucher | regarde: il n'existe pas à l'écran ; j'ai pris l'onglet « कीबोर्ड » en bas
  - ecran: 12 | cherchais: ma piste Marimba pendant l'enregistrement | regarde: une nouvelle piste « मेरी मेलोडी » est apparue et sélectionnée à sa place
  - ecran: 21 | cherchais: comment ajouter une 2e partie | regarde: le gabarit « पूरा » en a déjà plusieurs (A, C, B, E, F, G), rien à faire
abandon: jamais
incidents-banc:
  - geste: 23 | attente: 30 | vu: export M4A à 33 %, attendu 30 s puis la feuille de partage est venue (normal)
  - geste: 25 | attente: 0 | vu: niveau annoncé « स्ट्रीमिंग म्यूज़िक जैसा (−14) » avant, « तेज़, क्लब म्यूज़िक जैसा » après l'export : chiffres du banc, non jugés
defauts:
  - geste: 11 | ecran.vu: 12 | cible: touchable « मेरी मेलोडी FM पियानो » (nom « मेरी मेलोडी के ऑप्शन ») | geste.type: touche
    attendu: mes notes vont sur la piste « मेलोडी » où je viens de mettre Marimba (le guide me l'a fait choisir juste avant) | obtenu: une piste « मेरी मेलोडी » au son FM पियानो reçoit la prise ; Marimba n'est pas sur ma mélodie
    symptome: incompris
    gravite: contourne-en-4-gestes | revu: oui: ecran 29 (piste Marimba resélectionnée au #27, l'enregistrement #28 rebascule sur « मेरी मेलोडी ») | banc?: non | promesse: t5-Aa-3
  - geste: 9 | ecran.vu: 10 | cible: texte « तुम्हारी बारी: “कीबोर्ड खोलो” पर टैप करो। » | geste.type: touche
    attendu: un bouton « कीबोर्ड खोलो » à toucher | obtenu: aucun bouton de ce nom, seul l'onglet « कीबोर्ड » existe
    symptome: sans-aide
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 18 | cible: bouton « गाइड  · Kick (“बूम”) 35 % » | geste.type: touche
    attendu: un mot hindi en Simple (ex. « बूम ड्रम ») | obtenu: mot anglais « Kick » dans le titre du guide
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: t5-Aa-4
  - geste: 21 | ecran.vu: 22 | cible: texte « सब तैयार: कोई Solo ऑन नहीं छूटा, कोई ट्रैक म्यूट नहीं। » | geste.type: touche
    attendu: phrase tout en hindi | obtenu: « Solo » en anglais (et « ऑन ») dans le bilan avant partage
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Aa-4
  - geste: 8 | ecran.vu: 9 | cible: bouton « FM पियानो 6-op FM · बैंक 2 » | geste.type: touche
    attendu: sous-titres compréhensibles en Simple | obtenu: jargon anglais « 6-op FM », « Modal », « बैंक 2 » sur chaque son
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Aa-4
  - geste: 9 | ecran.vu: 10 | cible: touchable « Lo-fi hip-hop सेव हो गया » (nom « मेरे प्रोजेक्ट ») | geste.type: touche
    attendu: nom du projet lisible en haut | obtenu: affiché « L... » seulement (130 %, petit écran)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 18 | banc?: non | promesse: -
mots-faux:
  - ecran: 7 | vu: REC | attendu: रिकॉर्ड
  - ecran: 4 | vu: Snare | attendu: स्नेयर ड्रम / ताली वाला ड्रम
  - ecran: 9 | vu: Modal (पर्कशन) | attendu: मुलायम ठोक वाले साउंड
  - ecran: 25 | vu: 3 min 3 s | attendu: 3 मिनट 3 सेकंड
