code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: controle/t9-48/a | profil: pro | lang: pt-BR | device: phone | font: 100 | mode: pro
mission: finir les chapitres 1-7 du tuto minimal (départ chapitre 1), budget 174 gestes | buts: -
promesse: -
chapitres: ch1 tempo/tonalité : compris du premier coup | ch2 batterie : hésitation (carte qui cache la 2e rangée) | ch3 basse : compris du premier coup | ch4 stab dub : compris du premier coup | ch5 sino : hésitation (compteur de notes qui saute) | ch6 Groove : hésitation (pas trouvé où renommer la partie à la main) | ch7 parties : compris du premier coup
hesitations:
  - #84-86 : le compteur passe de « Falta 1 » à « Faltam 6 … mais acima ↑ » puis « Faltam 5 … mais adiante → » ; je ne savais plus combien de notes restaient.
  - #102-108 : « dê o nome Groove a esta cena » : ni le menu « A ▾ » ni l'appui long ni Estrutura > Partes n'offrent de renommage ; fini avec le bouton « Nomear a parte “Groove” » de la carte.
  - #40 : la carte du tuto couvre la rangée de pas à toucher (clap 5/7) ; il faut la replier.
abandon: aucun ; chapitres 1 à 7 finis (écran 124 « Capítulo 7/14 · Feito ») en 126 gestes sur 174.
incidents-banc: aucun
defauts:
  - geste: #2 | ecran.vu: 3 | cible: « Exemplo pulado sem ouvir. » | geste.type: touche
    attendu: l'exemple lancé (7 s restantes) compte comme écouté | obtenu: message « pulado sem ouvir »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #18 | ecran.vu: 18 | cible: « Já pronto: o kit de “Faça uma música pra mim” para o minimal (kick curto, rim, shaker)… » | geste.type: touche
    attendu: le kit Minimal en place | obtenu: « Kit: Estúdio », « Kick de estúdio », et « Baixar o kit Minimal (1 MB) » encore proposé jusqu'au ch7
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #40 | ecran.vu: 40 | cible: carte « A bateria » (clap passo 5/7) | geste.type: touche
    attendu: voir les pas demandés | obtenu: la carte du tuto cache la 2e rangée de pas
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: #54 | ecran.vu: 54 | cible: « Compasso 1/2 · Shaker » | geste.type: glisse
    attendu: en-tête de grille lisible | obtenu: en-tête coupé sous la barre du haut
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #60 | ecran.vu: 60 | cible: « som “Baixo dedilhado” » | geste.type: touche
    attendu: piste au nom annoncé | obtenu: piste « Baixo Baixo pluck »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #79 | ecran.vu: 79 | cible: « Faltam tocar 5 notas pontilh… » | geste.type: regarde
    attendu: consigne entière | obtenu: texte tronqué
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #84 | ecran.vu: 84 | cible: « Faltam 6 notas pontilhadas, mais acima ↑ » | geste.type: touche
    attendu: compteur qui descend (annoncé 5, puis 1) | obtenu: remonte à 6 puis 5, sens qui change
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #104 | ecran.vu: 106 | cible: « Pronto! Agora é com você. » « Parar » | geste.type: touche
    attendu: arrêter la démo « Mostre-me » | obtenu: avertissement « Esta etapa ainda não foi feita. Toque de novo em “Próximo”… » comme si j'avais tapé Próximo
    symptome: mauvais-ecran | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #102 | ecran.vu: 102 | cible: « Parte a editar » / « A » | geste.type: appui-long
    attendu: renommer la partie à la main | obtenu: aucun renommage visible (menu A, appui long, Estrutura > Partes)
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: #111 | ecran.vu: 112 | cible: « Baixar o kit Minimal (1 MB) » | geste.type: regarde
    attendu: voir le schéma des parties avec leurs durées | obtenu: la bannière du kit couvre le bas des blocs (durées coupées) ; les puces « Intro » sous le schéma ne jouent rien (#114)
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux: « Baixo dedilhado » (tuto) vs « Baixo pluck » (piste) ; « + Nova parte (cópia da · Groove) » (#108) ; « Baixo Baixo pluck » (doublon) ; clavier du piano roll sans noms de notes hors « Dó3/Dó4 »
defauts-avant: -
fin-tranche: oui (ROUTE ligne 647)
