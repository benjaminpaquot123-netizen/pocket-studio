code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: pro | lang: pt-BR | device: foldable | font: 100 | mode: pro
mission: tuto-pro (detroit, chapitres 8 à 14) | buts: ch8-14-finis, export-wav, master-club
promesse: id=mix-nom-faixa | texte: Deslize um fader para o volume; toque no nome de uma faixa para ver os efeitos e as curvas dela. | ecran: 51 | tenue: pas vue | pourquoi: chaque toucher dans les bandes du Mix est refusé par le banc (« Bateria couvre … »)
chapitres:
  - ch8 Construa suas partes : compris du premier coup
  - ch9 Organize suas partes : hésitation (boutons + cachés sous le panneau)
  - ch10 As transições : hésitation (« último compasso » sans sélecteur de mesure, partie Break au lieu de Drop)
  - ch11 Os efeitos : hésitation (canal du pad Caixa introuvable seul, trouvé par Mostre-me)
  - ch12 A mixagem : bloqué à l'étape 1 (faders refusés par le banc), étape sautée par « Próximo » ×2
  - ch13 A masterização : compris du premier coup
  - ch14 Exporte sua música : compris du premier coup (WAV Club exporté), message final trompeur
hesitations: (3 max)
  - ecran: 21 | cherchais: + E · Break / + F · Drop 2 / + G · Fim | regarde: liste revenue en haut, boutons sous le panneau du tuto
  - ecran: 51 | cherchais: le canal du pad Caixa dans le Mix | regarde: 4 bandes seulement (Bateria, Acid 303, Acordes, Baixo)
  - ecran: 74 | cherchais: le fader Melodia | regarde: 5e bande hors écran à droite, aucun signe de défilement
abandon: jamais (ch12 étape 1 sautée, pas abandonnée)
incidents-banc:
  - geste: 50, 51, 59, 73, 77, 78, 79, 80, 83, 85, 92, 93 | attente: 0 | vu: REFUS « doigt couvert : Bateria couvre Volume de … / Acid 303 / Baixo » sur toute la carte Volumes du Mix, même Bateria entièrement visible ; aucun fader ni nom de piste touchable
defauts: (10 max)
  - geste: 20 | ecran.vu: 21 | cible: + E · Break | geste.type: touche
    attendu: ajouter les parties à la suite | obtenu: après chaque ajout la liste remonte, les boutons suivants sont sous le panneau ; un glissé avant chaque ajout
    symptome: cache | gravite: contourne-en-3-gestes | revu: oui: ecran 23 | banc?: non | promesse: -
  - geste: 37 | ecran.vu: 37 | cible: + (G · Fim, coupé au bord du panneau) | geste.type: touche
    attendu: allonger Fim | obtenu: rien ; marche après avoir fait défiler
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 39 | banc?: non | promesse: -
  - geste: 46 | ecran.vu: 46 | cible: PASSOS: PRATO · BREAK | geste.type: touche
    attendu: l'étape 2 demande le Drop, l'écran devrait être sur le Drop | obtenu: reste sur Break, il faut passer par « E · Break ▾ » → D · Drop
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 41 | cible: Intro comp. 1/120 (en-tête) | geste.type: touche
    attendu: l'en-tête suit la partie éditée | obtenu: « Intro » pendant l'édition de Break ; plus tard « Drop comp. 33/120 » sur « Passos da parte E »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 71 | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 41 | cible: « último compasso » (consigne ch10) | geste.type: touche
    attendu: choisir la dernière mesure de la partie | obtenu: grille de 16 pas, aucun sélecteur de mesure : on ne sait pas si seule la dernière change
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 51 | cible: Sua vez: no Mix, aumente o “Envio de reverb” no canal do pad “Caixa”. | geste.type: touche
    attendu: un canal Caixa visible dans le Mix | obtenu: aucune bande pad ; seul « Mostre-me » ouvre « Efeitos · pad 3 (Caixa) »
    symptome: incompris | gravite: contourne-en-6-gestes | revu: oui: ecran 60 | banc?: oui, touchers de noms refusés « doigt couvert » | promesse: mix-nom-faixa
  - geste: 73 | ecran.vu: 74 | cible: Sua vez: no Mix, abaixe um pouco o fader da faixa “Melodia” | geste.type: glisse
    attendu: voir la bande Melodia | obtenu: 5e bande coupée hors écran à droite sur le pliable, aucun indice de défilement ; vue seulement grâce à Mostre-me
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 83 | banc?: non | promesse: -
  - geste: 68 | ecran.vu: 69 | cible: Sobre a parte inteira · 4 comp. | geste.type: touche
    attendu: la durée de la partie Break (8 comp.) | obtenu: « 4 comp. » et axe de temps 1–15
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: a bateria e as cordas, sem baixo nem sino | geste.type: touche
    attendu: Fim garde la batterie | obtenu: résultat « sem Caixa, Prato, Baixo, Melodia » ; ch12 étape 3 parle encore de « cordas » et « sino », pistes qui n'existent pas (Acordes, Melodia)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 97 | banc?: non | promesse: -
  - geste: 113 | ecran.vu: 114 | cible: Este capítulo não acabou: falta a etapa 1. | geste.type: touche
    attendu: chapitre fini après l'export (l'étape 1 n'est qu'un texte sans « Sua vez ») | obtenu: « falta a etapa 1 » et bouton « Próxima etapa » sur la dernière étape
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 10 | vu: rufo | attendu: virada
defauts-avant:

fin-tranche: non (dernier step=share, done=27)
