code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: moyen | lang: hi | device: phone | font: 100 | mode: simple
mission: tuto-facile (riddim ch. 1-8) | buts: - (aucun id donné par le brief)
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: le brief ne donnait aucune promesse
chapitres:
  - ch1 गाने की स्पीड और रंग : hésitation (carte « अध्याय पूरा » repliée, il faut ⌃ pour voir « अगला अध्याय », écran 12)
  - ch2 किक : compris du premier coup
  - ch3 स्नेयर और क्लैप : compris du premier coup
  - ch4 हाई-हैट्स : compris du premier coup
  - ch5 बेस : hésitation (compteurs de notes pointées contradictoires, écrans 43-49)
  - ch6 इंट्रो का पैड : compris du premier coup
  - ch7 तुम्हारा पहला पार्ट: ग्रूव : hésitation (j'ai cherché à renommer via « पार्ट: A ▾ », écran 69)
  - ch8 पार्ट्स को समझना : compris du premier coup (« शाबाश, अध्याय पूरा! » écran 92)
  - mission finie : chapitres 1 à 8 terminés en 92 gestes
hesitations:
  - ecran: 12 | cherchais: le bouton pour passer au chapitre suivant | regarde: carte repliée, seul ⌃ visible
  - ecran: 45 | cherchais: combien de notes il me reste à poser | regarde: bandeau « अभी 5 बिंदुदार नोट » alors qu'il en restait 1
  - ecran: 69 | cherchais: renommer le pattern en « ग्रूव » | regarde: menu « पार्ट: A ▾ » sans option renommer
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun écran vide ; outils hors human-act utilisés : awk/grep/tail/cat sur un fichier brouillon, lecture des PNG du siège
defauts:
  - geste: 45 | ecran.vu: 45 | cible: अभी 5 बिंदुदार नोट, और ऊपर ↑ | geste.type: touche
    attendu: le bandeau compte les notes qui restent dans l'étape (1) | obtenu: il annonce 5, le tuto disait 3 au total
    symptome: incompris | gravite: contourne-en-1-gestes | revu: oui: ecran 49 | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 48 | cible: 1 डॉटेड नोट दबाना बाकी है | geste.type: touche
    attendu: même nombre que le tuto « बार 2 के 4 नोट्स » | obtenu: bandeau dit 1 puis ↑ 3, avant il disait « 4 डॉटेड नोट्स बाकी हैं, आगे → »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 49 | banc?: non | promesse: -
  - geste: 86 | ecran.vu: 85 | cible: ब्रेक सुनो: पैड और अकेला बेस, बिना किक के | geste.type: touche
    attendu: le break suit la définition « किक और बेस चुप हो जाते हैं » | obtenu: le texte dit que la basse joue seule dans le break
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 18 | cible: हिट, ज़ोर 100 % | geste.type: touche
    attendu: le coup du milieu « बीच में हल्का » est plus faible | obtenu: les 2 coups de kick à 100 %
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: उदाहरण बिना सुने छोड़ दिया। | geste.type: touche
    attendu: pas de reproche, j'avais lancé l'exemple | obtenu: le tuto dit que je l'ai sauté sans l'écouter
    symptome: texte-faux | gravite: finition | revu: non | banc?: son | promesse: -
  - geste: 28 | ecran.vu: 29 | cible: पर्कशन पैड | geste.type: touche
    attendu: le nom du pad cité existe à l'écran | obtenu: le pad s'appelle « ढोल का किनारा »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 22 | cible: स्नेयर और क्लैप | geste.type: touche
    attendu: même mot dans le titre et sur le pad | obtenu: titre « क्लैप », pad et texte « ताली » (« “ताली” पैड (ताली) » redondant)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 11 | cible: शाबाश, अध्याय पूरा! ⌃ | geste.type: touche
    attendu: « अगला अध्याय » visible à la fin du chapitre | obtenu: carte repliée, il faut toucher ⌃
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 69 | ecran.vu: 69 | cible: पार्ट: A ▾ | geste.type: touche
    attendu: renommer le pattern depuis son menu | obtenu: seulement « A » et « + नया पार्ट (A की कॉपी) », retour et bouton du tuto
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: oui: ecran 71 | banc?: non | promesse: -
  - geste: 57 | ecran.vu: 58 | cible: + ट्रैक | geste.type: touche
    attendu: puce entière | obtenu: coupée au bord droit (aussi « पार्ट: A » coupé à gauche, écran 34)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 8 | vu: Dorian / Blues / Phrygian | attendu: noms en hindi ou translittérés
  - ecran: 8 | vu: गाने की key | attendu: गाने की स्केल (le tuto dit aussi « टोन शीट », la feuille « गाने की स्केल »)
  - ecran: 45 | vu: बिंदुदार | attendu: डॉटेड (deux mots pour la même chose)
  - ecran: 43 | vu: यहाँ टैप करें | attendu: यहाँ टैप करो (le reste de l'appli tutoie)
  - ecran: 82 | vu: क्लैक्स | attendu: क्लैप्स / तालियाँ
  - ecran: 58 | vu: पैड (son de nappe) | attendu: autre mot, « पैड » = carré à toucher dans le tuto
defauts-avant:
  - aucun défaut visé donné par le brief
fin-tranche: oui (ROUTE ligne 464)
BENCH-VERDICT: #92 BENCH-VERDICT ok where=seat.fin
