code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: pro | lang: pt-BR | device: phone360 | font: 200 | mode: pro
mission: tuto-pro (tuto detroit, chapitres 1 à 7) | buts: -
hesitations:
  - ecran: 16 | cherchais: le curseur Swing 22 % | regarde: il était caché sous le panneau du tuto, j'ai dû faire défiler la petite fente au-dessus
  - ecran: 45 | cherchais: la 2e rangée de pas (hi-hats) | regarde: la grille saute de ~18 px, mes touches tombaient sur la rangée 1
  - ecran: 151 | cherchais: la note suivante du sino | regarde: « Eu faço » me ramenait à une note déjà posée, en boucle
abandon: ecran 112 | parce que le clavier des accords (ch3) reste dans une fente de ~90 px sous le panneau et mes touches n'y font rien ; j'ai sauté le chapitre
incidents-banc:
  - geste: - | attente: 0 | vu: outils hors human-act.py : lecture des captures et de journal.txt / start.txt / rapport.md de mon siège, et des deux gabarits au départ
defauts:
  - geste: 104 | ecran.vu: 104 | cible: bouton « Acordes · compasso 1/4 Acordes pontilhados para tocar: 2 » [coupé] | geste.type: touche
    attendu: poser l'accord pointillé que le tuto demande | obtenu: rien (DOIGT rien, idem au #109), « Mostre-me » ne montre rien, le panneau ne se baisse pas (#101)
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 109 | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: texte « Swing » [coupé] | geste.type: touche
    attendu: « Eu faço » amène le réglage Swing 22 % sous mon doigt | obtenu: défile seulement jusqu'à « Contagem », le curseur Swing reste sous le panneau ; 7 gestes de défilement pour l'atteindre
    symptome: cache | gravite: contourne-en-7-gestes | revu: oui: ecran 23 | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 45 | cible: bouton « 3 » [coupé, état « vazia, pontilhada: toque »] | geste.type: touche
    attendu: allumer un hi-hat sur la 2e rangée | obtenu: la grille a sauté, j'ai éteint un pas de la rangée 1 ; ~10 gestes pour réparer
    symptome: perdu | gravite: contourne-en-10-gestes | revu: oui: ecran 50 | banc?: non | promesse: -
  - geste: 158 | ecran.vu: 159 | cible: icône « Sua vez: crie a faixa Melodia (som “Sino FM”) … Ler » ⌃ | geste.type: glisse
    attendu: tirer une note longue sur la cible pointillée large | obtenu: le rouleau défile, pas de note ; une note courte posée au bout suffit à valider
    symptome: incompris | gravite: contourne-en-4-gestes | revu: oui: ecran 165 | banc?: non | promesse: -
  - geste: 157 | ecran.vu: 151 | cible: icône « Sua vez: crie a faixa Melodia (som “Sino FM”) … Ler » ⌃ | geste.type: touche
    attendu: « Eu faço » me montre la note suivante qui manque | obtenu: il revient sur des notes déjà posées (Dó au début, deux fois), mes notes semblent retirées ; ~12 gestes en boucle
    symptome: perdu | gravite: contourne-en-12-gestes | revu: oui: ecran 156 | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 52 | cible: texte « hats com duas intensidades (um a cada dois mais suave) » | geste.type: touche
    attendu: l'étape vérifie les deux intensités | obtenu: validée avec tous les pas à 100 %
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 9 | cible: texte « Exemplo pulado sem ouvir. » | geste.type: touche
    attendu: pas de reproche, j'avais lancé l'exemple | obtenu: le message s'affiche, et revient sur l'écran de réussite
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 170 | ecran.vu: 166 | cible: texte « Ouça seu loop por alguns segundos, depois o exemplo. Cordas que cantam sobre uma bateria eletrônica que dança: isso é Detroit. » [coupé] | geste.type: touche
    attendu: un texte qui tient compte des accords sautés | obtenu: parle de « cordas que cantam » alors que ma faixa Acordes est vide
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 166 | ecran.vu: 166 | cible: bouton « Baixar o kit Techno de Detroit (4 MB) » | geste.type: touche
    attendu: la bannière disparaît après un premier refus implicite | obtenu: elle revient à chaque étape et pousse le contenu vers le bas
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
defauts-avant:
chapitres:
  - ch1 O tempo e a tonalidade : hésitation (Swing caché)
  - ch2 A bateria : hésitation (grille qui saute)
  - ch3 Os acordes : bloqué (sauté avec « Próximo » x2)
  - ch4 O baixo : hésitation (notes trouvées via « Eu faço »)
  - ch5 O sino : hésitation (boucle de « Eu faço », note longue)
  - ch6 Sua primeira parte: o Groove : compris du premier coup
  - ch7 Entender as partes : budget épuisé au chapitre 7 (étape 3/5)

fin-tranche: non (dernier step=map_drop, done=19)
