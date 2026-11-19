code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: pro | lang: pt-BR | device: phone360 | font: 130 | mode: pro
mission: tuto-pro | buts: chapitres 8-14 du tuto minimal (amenee tuto:minimal:8)
promesse: id=- | texte: « uma batida só, discreta » (crash du Drop, ch 10) | ecran: 45 | tenue: non | pourquoi: le coup posé est à intensidade 100%
chapitres: 8 compris du premier coup | 9 hésitation | 10 compris du premier coup | 11 hésitation (étape 1 contournée par Mostre-me) | 12 compris du premier coup | 13 compris du premier coup | 14 compris du premier coup — fini en 100 gestes sur 153
hesitations:
  - ecran: 49 | cherchais: le canal du pad « Rim » dans Mix | regarde: barre des pistes puis fiche Bateria (seulement « Envio eco » du kit entier)
  - ecran: 21 | cherchais: les boutons « + F/G/H » | regarde: ils passent sous le panneau du tuto après chaque ajout
  - ecran: 68 | cherchais: le graphe où « desenhe-a abaixo » | regarde: il était sous le panneau, 3 glissés
abandon: jamais
incidents-banc:
  - geste: 96-97 | attente: 10 | vu: export WAV figé à 16% puis feuille de partage (banc, encodeur non représentatif)
  - geste: - | attente: 0 | vu: outils hors human-act : cat/tail sur start.txt, meta, journal et gabarits du siège ; Read de mes captures (écrans 2, 13, 38, 45, 73)
defauts:
  - geste: #49 | ecran.vu: 49 | cible: « no Mix, aumente o “Envio de eco” no canal do pad “Rim” » | geste.type: touche
    attendu: un canal Rim avec son envoi d'écho | obtenu: Mix n'a que Bateria/Acid 303/Baixo/Acordes/Melodia/Remix ; Bateria n'offre qu'un « Envio eco » global
    symptome: incompris
    gravite: contourne-en-10-gestes | revu: oui: ecran 57 | banc?: non | promesse: -
  - geste: #59 | ecran.vu: 59 | cible: « Mostre-me » | geste.type: touche
    attendu: une démo puis ma main | obtenu: la démo dit « Vou deixar como estava… » mais valide l'étape ; je ne sais pas ce qui est gardé
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #37 | ecran.vu: 38 | cible: « Dar as durações recomendadas » | geste.type: touche
    attendu: un message sur les durées recommandées | obtenu: le bandeau « Feito: nova duração para E · Break · Cancelar » reste, faux et couvre la ligne B · Intro puis la barre BPM (écran 39)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 39 | banc?: non | promesse: -
  - geste: #38 | ecran.vu: 38 | cible: icône « Intro comp. 1/120 » | geste.type: touche
    attendu: « 1/120 » lisible | obtenu: l'image montre « 1/12 », le dernier chiffre est coupé à 130 % (aussi écrans 45, 73)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 73 | banc?: non | promesse: -
  - geste: #45 | ecran.vu: 45 | cible: crash dans le Drop | geste.type: touche
    attendu: « uma batida só, discreta » | obtenu: le coup est posé à intensidade 100%
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #22 | ecran.vu: 21 | cible: « + F · ... » | geste.type: touche
    attendu: la liste reste où j'étais | obtenu: elle remonte après chaque ajout, les boutons suivants passent sous le panneau
    symptome: cache
    gravite: contourne-en-3-gestes | revu: oui: ecran 26 | banc?: non | promesse: -
  - geste: #79 | ecran.vu: 79 | cible: page Mix | geste.type: glisse
    attendu: faire défiler le Mix | obtenu: le fader Baixo part à −∞ dB ; noms des pistes cachés une fois défilé
    symptome: perdu
    gravite: contourne-en-1-gestes | revu: oui: ecran 80 | banc?: non | promesse: -
  - geste: #13 | ecran.vu: 13 | cible: carte de réussite du tuto | geste.type: touche
    attendu: carte lisible | obtenu: un « ⋯ » orange de la liste derrière passe au-dessus, texte coupé (« Continuar lendo »)
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #73 | ecran.vu: 73 | cible: graphe d'automatisation Filtro | geste.type: glisse
    attendu: axe lisible | obtenu: étiquettes coupées « 18,0 kH », « 849 H »
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #88 | ecran.vu: 88 | cible: « Volume-alvo » | geste.type: glisse
    attendu: le réglage demandé visible | obtenu: Volume-alvo et ses choix sont sous le panneau du tuto ; noms des pistes « [coupé] » en haut du Mix (écrans 84-86)
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 89 | banc?: non | promesse: -
mots-faux:
  - ecran: 1 | vu: o tic | attendu: o chimbal (hi-hat)
  - ecran: 12 | vu: o sino (aussi écran 85) | attendu: a melodia (aucune piste « sino »)
  - ecran: 63 | vu: Corte (tuto) | attendu: Filtro (varredura) / Filtro Corte, comme dans le menu
  - ecran: 47 | vu: Envio de eco (tuto) | attendu: Envio eco (appli)

fin-tranche: oui (ROUTE ligne 615)
