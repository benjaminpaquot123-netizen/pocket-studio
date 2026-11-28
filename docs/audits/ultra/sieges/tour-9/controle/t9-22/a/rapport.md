code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: curieux | lang: it | device: foldable | font: 130 | mode: simple
mission: tuto-facile (tuto dnb, chapitres 9 à 15) | buts: -
promesse: id=- | texte: « Bravo, ce l'hai fatta! » | ecran: 77 | tenue: oui | pourquoi: chapitres 9 à 15 finis, le morceau est exporté en M4A et partagé (écran 76)
chapitres:
  - ch 9 « Capire le parti » : compris du premier coup
  - ch 10 « Costruisci le tue parti » : compris du premier coup
  - ch 11 « Ordina le tue parti » : hésitation (panneau sur les boutons « Aggiungi », liste qui saute)
  - ch 12 « Le transizioni » : compris du premier coup
  - ch 13 « Gli effetti » : compris du premier coup
  - ch 14 « Il mix: i volumi » : compris du premier coup
  - ch 15 « Esporta il tuo brano » : compris du premier coup
hesitations:
  - ecran: 37 | cherchais: les boutons « Aggiungi » demandés par le tuto | regarde: le panneau du tuto les couvrait, je l'ai replié avec ⌄
  - ecran: 40 | cherchais: « + F · Drop 2 » et « + G · Fine » | regarde: coupés en bas, la liste remonte après chaque ajout, j'ai dû faire glisser
abandon: jamais
incidents-banc:
defauts:
  - geste: 37 | ecran.vu: 37 | cible: Ordina le tue parti Capitolo 11/15 | geste.type: touche
    attendu: les boutons « Aggiungi » que le tuto demande sont visibles | obtenu: le panneau du tuto les couvre, il faut le replier (⌄) ou « Lo faccio io »
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 38 | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 40 | cible: + F · Drop 2 | geste.type: touche
    attendu: après chaque ajout, le bouton suivant reste visible | obtenu: « + F · Drop 2 » et « + G · Fine » sont coupés en bas et la liste revient en arrière à chaque ajout
    symptome: texte-coupe | gravite: contourne-en-2-gestes | revu: oui: ecran 44 | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 21 | cible: Tocca a te: tocca “Crea la parte “Intro””. | geste.type: touche
    attendu: une consigne simple (« Tocca “Crea la parte Intro” ») | obtenu: guillemets imbriqués et « tocca » répété deux fois, vu aussi aux écrans 23 et 25
    symptome: texte-faux | gravite: finition | revu: oui: ecran 25 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 19 | cible: ▸ Perché? | geste.type: touche
    attendu: la réponse s'ouvre sous son bouton | obtenu: elle s'ouvre au-dessus de la carte Energia, loin du bouton, et la carte est coupée
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 2 | cible: Scarica il kit Drum & bass (2 MB) | geste.type: touche
    attendu: la carte des parties reste visible pendant le téléchargement | obtenu: « Download del kit… 0 % » prend la place de la carte
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 50 | cible: Drum & bass · Rullante | geste.type: touche
    attendu: le nom dit par le tuto est celui écrit sur le pad | obtenu: le tuto dit « Drum & bass · Rullante », le pad affiche « Drum & bass · Rullante FX »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 72 | ecran.vu: 72 | cible: Tocca a te: alza ciò che deve sentirsi, abbassa ciò che disturba. Tocca ▶ per ascoltare una traccia da sola. | geste.type: touche
    attendu: le conseil en haut du Mix se lit en entier | obtenu: le texte est coupé (police 130 %), et « Normale » / « −2 » sous les curseurs le sont aussi quand le panneau est ouvert
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 72 | banc?: non | promesse: -
  - geste: 75 | ecran.vu: 76 | cible: Livello: come la musica in streaming (−14) | geste.type: touche
    attendu: un débutant comprend le chiffre | obtenu: « (−14) » sans explication
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 26 | vu: Aggiungili in ordine | attendu: Aggiungile in ordine (le parti)
  - ecran: 51 | vu: Velocity | attendu: Intensità
  - ecran: 60 | vu: il reverb | attendu: il riverbero (ou une explication)
  - ecran: 9 | vu: reese | attendu: un mot expliqué (basso « reese » non défini)
defauts-avant:
gestes: 79 sur 167 (fin comprise)
fin-tranche: oui (Bravo final « Bravo, ce l'hai fatta! » écran 77, Capitolo 15/15, journal ligne 346 ; mais le compteur du journal reste à step=share done=35, pas done=42)
