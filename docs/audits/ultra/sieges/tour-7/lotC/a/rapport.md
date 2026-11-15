code testé : 85497e2f97cd8319f0374d48b55913e4797b800e
joué par : sous-agent neuf
siege: a | profil: t7-3a-hindi-curieux-360 | lang: hi | device: phone360 | font: 130 | mode: simple
mission: simple (morceau + 2e partie + ▶ partie / ▶ morceau) | buts: son, partie, jamais-pro
oeil-neuf: 10 écrans, 2 graves
note: siège œil neuf resté en Simple (le but jamais-pro du brief interdit le passage en Pro) : q10 non comparée.
promesse: id=t7-3a-1 | texte: ▶ गाना / ▶ यह पार्ट | ecran: 42 | tenue: non | pourquoi: ▶ गाना repart de la partie en cours (T bar 105, B bar 25, C bar 9), pas du début (revu écrans 51, 61)
promesse: id=t7-3a-2 | texte: + नया खाली पार्ट | ecran: 31 | tenue: oui | pourquoi: les 4 coups posés sur T sont là après un passage par B
promesse: id=t7-3a-3 | texte: गाइड · ... | ecran: 9 | tenue: non | pourquoi: le Guide cache des pistes (écrans 9, 11), texte du guide coupé (9), bouton « ▶ सुनो » sans libellé visible (65)
promesse: id=t7-3a-4 | texte: bar | ecran: 28 | tenue: non | pourquoi: mots anglais « bar » (28), « Live », « Remix » (63), « Pad Live » (3), « M4A/WAV » (65)
hesitations:
  - ecran: 14 | cherchais: où ajouter une 2e partie | regarde: « + नया खाली पार्ट » seulement tout en bas, après ~6 balayages (écran 22)
  - ecran: 23 | cherchais: où est passée ma nouvelle partie T dans le morceau | regarde: rien à l'écran ne dit qu'elle est ajoutée à la fin
  - ecran: 38 | cherchais: « 8 बार · 2 बार बजा » | regarde: « बार » veut dire à la fois mesure et fois
abandon: jamais
incidents-banc:
  - geste: 27 | attente: 0 | vu: image de l'écran 28 en retard sur le texte (montre encore T, bar 6/8)
defauts-avant:
  - U28 | pas corrigé (écran 51) : B choisie, ▶ गाना joue depuis B bar 25/112, A n'est pas jouée avant
  - U29 | corrigé (écran 22) : bouton « + नया खाली पार्ट » présent, mais tout en bas de la liste
  - U1 | corrigé (écran 33) : « ▶ सिर्फ़ ये पार्ट बजाता है » dit clairement ce qui joue
  - G1 | pas corrigé (écran 11) : le Guide ouvert couvre पैड / मेलोडी et demande de toucher « मेलोडी का साउंड » caché dessous
  - R15 | pas corrigé (écran 28) : mot anglais « bar » à côté des boutons 1-4
  - R1 | pas corrigé (écran 65) : bouton contour à droite de « 27 पार्ट » sans mot lisible ; écran 9 texte du guide coupé à gauche
defauts:
  - geste: 41 | ecran.vu: 42 | cible: गाना | geste.type: touche
    attendu: le morceau joue depuis le début, A puis B... | obtenu: lecture depuis la partie en cours (bar 105/112) puis arrêt
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 51 | banc?: non | promesse: t7-3a-1
  - geste: 11 | ecran.vu: 11 | cible: तुम्हारी बारी: “मेलोडी का साउंड” पर टैप करो। | geste.type: touche
    attendu: le Guide ne cache rien de touchable | obtenu: le Guide couvre les lignes पैड / मेलोडी qu'il demande de toucher
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 9 | banc?: non | promesse: t7-3a-3
  - geste: 64 | ecran.vu: 65 | cible: 27 पार्ट | geste.type: touche
    attendu: bouton « ▶ सुनो » lisible | obtenu: contour de bouton vide, libellé coupé/invisible (image 65)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-3a-3
  - geste: 8 | ecran.vu: 9 | cible: ाइड तुम्हारा गाना पढ़ रही है… | geste.type: touche
    attendu: phrase du guide entière | obtenu: début du mot « गाइड » coupé à gauche
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-3a-3
  - geste: 27 | ecran.vu: 28 | cible: bar | geste.type: touche
    attendu: mot hindi (बार) | obtenu: « bar » en anglais
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t7-3a-4
  - geste: 62 | ecran.vu: 63 | cible: Live: लाइव बजाओ | geste.type: touche
    attendu: tout en hindi | obtenu: « Live », « Remix » en anglais dans le menu और
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t7-3a-4
  - geste: 62 | ecran.vu: 63 | cible: मेरी लाइब्रेरी | geste.type: touche
    attendu: dernière rangée du menu lisible | obtenu: la barre du Guide coupe « मेरी लाइब्रेरी » / « मदद »
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: t7-3a-3
  - geste: 23 | ecran.vu: 23 | cible: + नया खाली पार्ट | geste.type: touche
    attendu: voir que T est ajoutée au morceau | obtenu: aucune preuve à l'écran (liste hors écran)
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 14 | cible: − | geste.type: glisse
    attendu: savoir pourquoi − / + sont grisés | obtenu: presque tous [désactivé], sans explication ; « मिक्स » non expliqué
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 3 | vu: Pad Live | attendu: पैड लाइव
  - ecran: 28 | vu: फ़ॉलो | attendu: साथ चलो
  - ecran: 38 | vu: 2 बार बजा | attendu: 2 दफ़ा बजा (बार = mesure ailleurs)
  - ecran: 65 | vu: 27 पार्ट | attendu: 20 parties A-T dans la liste, nombre incohérent
