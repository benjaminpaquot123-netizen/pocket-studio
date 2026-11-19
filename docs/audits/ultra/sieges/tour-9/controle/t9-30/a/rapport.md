code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: moyen | lang: hi | device: foldable | font: 100 | mode: simple
mission: tuto-facile (grime ch. 6-13) | buts: tuto-grime-ch6-13, mixage-accords-moins-2, export-partage
promesse: id=- | texte: aucune donnée | ecran: - | tenue: pas vue | pourquoi: aucune promesse dans la consigne
chapitres:
  - ch6: compris du premier coup | ch7: compris du premier coup | ch8: compris du premier coup | ch9: hésitation
  - ch10: compris du premier coup | ch11: compris du premier coup | ch12: hésitation | ch13: compris du premier coup
  - tranche 6-13 finie ; pas de « Bravo » : écran 74 « लगभग हो गया! 3 स्टेप बाकी » (ch. 1-3 non joués, siège amené au ch. 6)
hesitations:
  - ecran: 43 | cherchais: un refrain de 16 mesures comme dit au ch. 7 | regarde: « × 1 · 8 बार » avant « सुझाई गई लंबाइयाँ दो »
  - ecran: 65 | cherchais: mettre le curseur des accords à −2 | regarde: le curseur ne bouge pas puis saute de −1 à −3
abandon: jamais
incidents-banc:
  - geste: 3 | attente: 0 | vu: téléchargement du kit à 0 % puis disparu sans confirmation (écran 4→5), peut-être le banc
defauts:
  - geste: #65 | ecran.vu: 65 | cible: curseur [état « सामान्य », à 80 %] @ 462,447 | geste.type: glisse
    attendu: un petit glissé règle les accords à −2 | obtenu: zone morte puis sauts (rien, −1, −1, −3, rien, −2) : 6 glissés
    symptome: lent | gravite: contourne-en-5-gestes | revu: oui: ecran 66 (mélodie réussie en 1 glissé) | banc?: non | promesse: -
  - geste: #6 | ecran.vu: 7 | cible: « टाइमलाइन तुम्हारे पार्ट्स को क्रम में रखती है… » [coupé] | geste.type: touche
    attendu: lire toute la consigne du tuto | obtenu: phrase coupée par le panneau du tuto (aussi écran 43 « गाने के कॉर्ड्स … » [coupé])
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: #5 | ecran.vu: 6 | cible: « तुम्हारी बारी: “पार्ट का नाम “मुखड़ा” रखो” पर टैप करो। » | geste.type: touche
    attendu: des guillemets lisibles | obtenu: guillemets imbriqués “…“…”…” difficiles à lire (aussi écrans 17, 22, 28)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: -
  - geste: #20 | ecran.vu: 21 | cible: « पार्ट “इंट्रो” बन गया (“मुखड़ा” की कॉपी): Grime · बड़ा ढोल 1, … मेलोडी के बिना। » | geste.type: touche
    attendu: comprendre ce que contient l'intro | obtenu: liste d'instruments ambiguë : enlevés ou gardés ?
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #5 | ecran.vu: 6 | cible: grille de pas sous le panneau du tuto | geste.type: touche
    attendu: voir toute la grille | obtenu: grille coupée par le panneau ; « + नया खाली पार्ट » [coupé] écran 23
    symptome: cache | gravite: finition | revu: oui: ecran 23 | banc?: non | promesse: -
  - geste: #73 | ecran.vu: 74 | cible: « 3 स्टेप बाकी हैं। किसी पर जाने के लिए उसे दबाओ: » | geste.type: touche
    attendu: la liste dit « chapitres » | obtenu: dit « स्टेप » (étapes) mais liste des chapitres 1/13, 2/13, 3/13
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #68 | ecran.vu: 69 | cible: image (rond coché) @ 0,0 | geste.type: touche
    attendu: rien de collé au coin | obtenu: une image « rond coché » posée au coin haut gauche de l'onglet Partager (aussi écran 73)
    symptome: sans-nom | gravite: finition | revu: oui: ecran 73 | banc?: non | promesse: -
mots-faux:
  - ecran: 8 | vu: Outro | attendu: आउटरो
  - ecran: 12 | vu: chiptune | attendu: चिपट्यून
  - ecran: 12 | vu: लगभग नंगा | attendu: लगभग खाली
  - ecran: 74 | vu: grime / square (titre du ch. 1) | attendu: ग्राइम / स्क्वेयर
defauts-avant:
  - aucun donné par la consigne

fin-tranche: non (dernier step=share, done=30 sur 33 ; chapitres 1-3 sautés par l amenée, écran 74 « 3 स्टेप बाकी »)
