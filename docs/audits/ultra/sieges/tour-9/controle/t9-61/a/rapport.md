code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

```
siege: a | profil: moyen | lang: hi | device: phone360 | font: 130 | mode: simple
mission: tuto-facile (tuto riddim, chapitres 9 à 14) | buts: -
promesse: id=- | texte: तुम्हारी बारी: बिल्ड-अप के अंतिम बार में “ताली” का अंतिम हिट दबाए रखो, “रोल” ×4। | ecran: 37 | tenue: non | pourquoi: roll ×4 posé sur le dernier hit (n°12) : l'étape ne passe pas ; elle attend la dernière case (n°16, vide)
promesse: id=- | texte: “M4A” दोस्तों के लिए हल्की फ़ाइल बनाता है | ecran: 64 | tenue: oui | pourquoi: le partage M4A est allé jusqu'au choix d'appli et au bilan « M4A फ़ाइल: हल्की »
hesitations:
  - ecran: 22 | cherchais: « + F · ब्रेक » pour la suite du morceau | regarde: les puces F, G, H sont sous le panneau du tuto replié ; il faut faire défiler
  - ecran: 37 | cherchais: pourquoi l'étape ne valide pas après roll ×4 sur le dernier hit | regarde: « मुझे दिखाओ » montre la case 16 (vide), pas le hit 12
  - ecran: 55 | cherchais: le curseur « कॉर्ड्स » | regarde: le panneau du tuto couvre tout le mixeur ; « मैं करूँगा » le replie
abandon: jamais
chapitres:
  - 9 « अपने पार्ट्स बनाओ » : compris du premier coup (7 étapes, un bouton chacune)
  - 10 « अपने पार्ट्स क्रम में रखो » : hésitation (puces cachées sous le panneau, la liste remonte après chaque ajout)
  - 11 « ट्रांज़िशन » : hésitation (étape 1 : « dernier hit » compris de travers, passé grâce à « मुझे दिखाओ »)
  - 12 « इफ़ेक्ट्स » : compris du premier coup
  - 13 « मिक्स: वॉल्यूम्स » : compris du premier coup
  - 14 « अपना ट्रैक एक्सपोर्ट करो » : compris du premier coup ; tuto fini (« ट्यूटोरियल पूरा: शाबाश! », écran 65), 64 gestes
incidents-banc:
defauts:
  - geste: 34 | ecran.vu: 37 | cible: bouton [état « हिट, ज़ोर 99 % »] @ 240,657 | geste.type: appui-long
    attendu: roll ×4 sur le dernier hit de la mesure 4 = étape validée, comme le dit la consigne | obtenu: roll posé (« 4 दोहराव ») mais étape 1/2 reste ; la démo le met sur la case 16 vide
    symptome: incompris | gravite: contourne-en-7-gestes | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 22 | cible: bouton « + F · ब्रेक » [coupé] @ 132,681 | geste.type: touche
    attendu: ajouter « ब्रेक » au morceau | obtenu: rien ; la puce est cachée sous le panneau du tuto replié, il faut faire défiler
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 23 | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 24 | cible: bouton « + F · ब्रेक » @ 132,429 | geste.type: touche
    attendu: la liste reste où je l'ai mise pour enchaîner G puis H | obtenu: après chaque ajout la liste remonte, F/G/H repassent sous le panneau ; un glissé par ajout
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 29 | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 57 | cible: texte « आउटपुट » @ 474,265 | geste.type: touche
    attendu: la 4e tranche du mixeur entière | obtenu: coupée par le bord droit (« आउटपु… », « ▶ दबा… ») en police 130 %
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: bouton « किट Riddim डाउनलोड करो (3 MB) » @ 223,548 | geste.type: touche
    attendu: « (3 MB) » sur une ligne | obtenu: le bouton se coupe entre « 3 » et « MB » (deux lignes)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 5 | banc?: non | promesse: -
  - geste: 59 | ecran.vu: 60 | cible: bouton « M4A · शेयर करने के लिए » [sélectionné] @ 196,227 | geste.type: touche
    attendu: les choix M4A et WAV l'un sous l'autre | obtenu: à l'arrivée sur « शेयर करो », M4A et WAV posés l'un sur l'autre (même hauteur 227) ; bon après repli du panneau
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 64 | ecran.vu: 65 | cible: texte « अपना ट्रैक एक्सपोर्ट करो अध्याय 14/14 · स्टेप 1/1 · आसान » @ 198,740 | geste.type: touche
    attendu: « हो गया » comme aux chapitres 9 à 13 | obtenu: l'en-tête reste « स्टेप 1/1 » alors que « ट्यूटोरियल पूरा: शाबाश! » est affiché dessous
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 18 | cible: texte « आख़िर में जोड़ो: » @ 124,393 | geste.type: touche
    attendu: le titre « जोड़ो: » que nomme la consigne | obtenu: le titre devient « आख़िर में जोड़ो: » dès le premier ajout
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 2 | vu: हिस्सा “इंट्रो” बन गया | attendu: पार्ट “इंट्रो” बन गया (le bouton dit « पार्ट »)
  - ecran: 12 | vu: ढोल का किनारा के बिना | attendu: ढोल के किनारे के बिना
  - ecran: 36 | vu: खाने में 4 चोट | attendu: खाने में 4 हिट
  - ecran: 51 | vu: झूठी शुरुआत | attendu: फ़ेक ड्रॉप (nom du part partout ailleurs)
  - ecran: 17 | vu: पढ़ें | attendu: पढ़ो (tutoiement partout ailleurs)
defauts-avant:
```

fin-tranche: oui (ROUTE ligne 305)
