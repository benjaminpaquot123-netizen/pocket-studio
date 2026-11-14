siege: b | profil: t5-Ab rappeur-bresilien | lang: pt-BR | device: phone411 | font: 100 | mode: simple
mission: voix | buts: prise, ecoute, garde
promesse: id=t5-Ab-1 | texte: Toque numa parte para ouvir a música a partir dela. | ecran: 21 | tenue: oui | pourquoi: ▶ part de 0:00 et va jusqu'à comp. 7.3/7 (écrans 15-17), toucher « B » joue depuis 0:04 jusqu'à comp. 6.3 (21-22) ; Áudio 1 est « ligado » dans Mix (le banc ne dit pas si la voix sonne)
promesse: id=t5-Ab-2 | texte: A compasso 1/7 | ecran: 16 | tenue: non | pourquoi: le compteur du haut reste « A compasso 1/7 » pendant toute la lecture ; seul « comp. 3.4 / 7 » (page Minha música) avance, et sur la page Minha voz où je rappe rien n'avance
promesse: id=t5-Ab-3 | texte: Take 1 · 10,8 s | ecran: 32 | tenue: non | pourquoi: toucher une prise lance une boucle « A compasso 1/1 (em loop) », même si le morceau entier jouait ; pour comparer sur tout le morceau il faut Parar puis Tocar
hesitations:
  - ecran: 25 | cherchais: retrouver mes prises de voix | regarde: la piste « Áudio 1 Trocar o som » dit changer le son, j'ai touché quand même et c'était la bonne page
  - ecran: 30 | cherchais: la liste des takes après la 2e prise | regarde: elle est sous le pli, il faut glisser ; seul « Takes (2) » se voit
abandon: jamais
incidents-banc:
defauts:
  - geste: 31 | ecran.vu: 32 | cible: Take 1 · 10,8 s | geste.type: touche
    attendu: choisir la prise et l'écouter dans tout le morceau | obtenu: lecture en boucle « A compasso 1/1 » (em loop), le bouton Ouvir sozinha devient Parar
    symptome: incompris
    gravite: contourne-en-2-gestes | revu: oui: ecran 37 | banc?: non | promesse: t5-Ab-3
  - geste: 14 | ecran.vu: 16 | cible: A compasso 1/7 | geste.type: touche
    attendu: le compteur du haut avance avec la lecture | obtenu: reste « A compasso 1/7 » alors que la ligne du dessous dit « 0:05 / 0:13 · comp. 3.4 / 7 »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: t5-Ab-2
  - geste: 20 | ecran.vu: 22 | cible: B × 2  ·  2 comp.  ·  tocando | geste.type: touche
    attendu: l'étiquette « tocando » suit la partie qui joue | obtenu: reste sur la partie 2 (et « A × 1 · em seguida ») alors que « comp. 6.3 / 7 » est dans la 4e partie
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 41 | banc?: non | promesse: t5-Ab-1
  - geste: 14 | ecran.vu: 18 | cible: comp. 7.4 / 7 | geste.type: touche
    attendu: en fin de morceau, la lecture s'arrête ou reprend au début | obtenu: bloqué sur « 0:12 / 0:13 · comp. 7.4 / 7 » (au-delà de 7), toujours en lecture
    symptome: rien
    gravite: finition | revu: oui: ecran 13 | banc?: son | promesse: -
  - geste: 27 | ecran.vu: 31 | cible: Take 2 · 18,7 s | geste.type: touche
    attendu: l'écran dit « toca uma vez só » : la prise s'arrête à la fin du morceau (0:13) | obtenu: enregistrement continue jusqu'à 18,7 s
    symptome: rien
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 26 | ecran.vu: 27 | cible: A parte B | geste.type: touche
    attendu: après avoir choisi « 1. A », l'option dit « A parte A » | obtenu: l'option reste « A parte B »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: A música está tocando: toque na grade e esta parte fica nas suas mãos, em loop. | geste.type: touche
    attendu: une aide qui parle de la prise de voix sur la page Minha voz | obtenu: parle d'une grade absente de l'écran et d'une boucle, alors que la prise est « na música inteira »
    symptome: incompris
    gravite: finition | revu: oui: ecran 9 | banc?: non | promesse: -
mots-faux:
  - ecran: 4 | vu: A Do B Do | attendu: A Dó B Dó
  - ecran: 45 | vu: A gente tira para aumentar a vontade | attendu: Tirar para dar vontade
  - ecran: 8 | vu: seu take entra no lugar dele na música | attendu: seu take entra no lugar certo da música
