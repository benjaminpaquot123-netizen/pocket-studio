code testé : ffbcb88654a7617f82d47a04fd87f87d9f5ec36c
joué par : sous-agent neuf

siege: a | profil: t6-Ab-controle3 (musicienne pressée) | lang: fr | device: téléphone | font: 100 | mode: simple
mission: padlive | buts: son, stop, prise
promesse: id=P2 | texte: Pad Live (ouverture) | ecran: 7 | tenue: oui | pourquoi: écrans 5 à 7 puis 31, « Son : rien ne joue » tant que je ne touche aucun pad
promesse: id=P3 | texte: « Continuer Simple Live Boucle de 1 mes. Sauvegardé à l'instant » | ecran: 30 | tenue: non | pourquoi: prises de 6 puis 2 mesures (écrans 11, 15) mises en brique, la carte d'accueil dit « Boucle de 1 mes. » ; refait avec une prise de 5 mesures, encore « 1 mes. » (écran 39) ; Briques affiche « A mesure 1/1 » (écran 24)
promesse: id=P4 | texte: bouton ▶ (haut) | ecran: 24 | tenue: non | pourquoi: page Pad Live oui (■ pendant la boucle, un toucher arrête tout, écran 38) ; vues Pads (écrans 19, 22) et Briques (écrans 18, 23) : ▶ alors que « le pad live 1 joue en boucle », et le toucher lance en plus la musique du projet (écrans 20, 24)
abandon: jamais
incidents-banc:
  - geste: 3 | attente: 0 | vu: toucher sur la carte Pad Live coupée en bas d'écran sans effet ; passé par un glissé
defauts:
  - geste: 20 | ecran.vu: 20 | cible: bouton ▶ @ 375,48 | geste.type: touche
    attendu: ■ pendant que la boucle du pad tourne, un toucher l'arrête | obtenu: ▶ affiché, le toucher lance la musique du projet par-dessus la boucle
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 24 | banc?: non | promesse: P4
  - geste: 29 | ecran.vu: 30 | cible: bouton ✓ « Continuer Simple Live Boucle de 1 mes. Sauvegardé à l'instant » → @ 294,294 | geste.type: retour
    attendu: la longueur de la prise la plus longue (6 mes.) | obtenu: « Boucle de 1 mes. »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 39 | banc?: non | promesse: P3
defauts-avant:
  - P2 | corrigé (écrans 7 et 31)
  - P3 | encore là (écrans 30 et 39 : « Boucle de 1 mes. » après des prises de 6, 2 puis 5 mesures)
  - P4 | autre : tenu sur la page Pad Live (écrans 9, 38), encore là dans Pads (écrans 19-20) et Briques (écrans 18, 24)
