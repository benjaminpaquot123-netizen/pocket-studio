code testé : ee4c2a92566bb2b6d7afeae799c0191c60c2e915
joué par : sous-agent neuf

siege: a | profil: t6-Ab musicienne-pressee | lang: fr | device: telephone | font: 100 | mode: simple
mission: padlive | buts: son, stop, prise
promesse: id=t6-Ab-1 | texte: Ma mélodie Mélodie (Notes : 3 notes en mes. 1) | ecran: 17 | tenue: oui | pourquoi: prise (3 notes) coupée par Retour (#6), passage Pad Live (#8), retour par Mes projets : les 3 notes sont dans la grille de « Ma mélodie »
promesse: id=t6-Ab-2 | texte: Tout couper | ecran: 28 | tenue: oui | pourquoi: boucle en cours, un toucher : plus rien ne joue, le haut repasse à ▶ (aussi écran 47 depuis la vue Pads)
promesse: id=t6-Ab-3 | texte: Appuie et maintiens | ecran: 27 | tenue: oui | pourquoi: deux glissés sur Filtre/Écho/Roulement (#26, #27) : aucun effet allumé, seul le kick continue
promesse: id=t6-Ab-4 | texte: Boucle de kick | ecran: 21 | tenue: non | pourquoi: les boucles touchées (#21, #22, #23) sont tout de suite [allumé], jamais d'[anneau pâle] d'attente (transport déjà lancé ; peut-être le temps du banc)
oeil-neuf: q1 | ecran: 55 | image: 55 | pistes, ▶ verts, curseurs, Annuler, Guide, onglets : tout semble touchable ; la piste Acid 303 vue dans Briques n'est pas dans « Mes pistes »
oeil-neuf: q6 | ecran: 55 | image: 55 | après deux prises, la preuve est « Audio 1 / Audio 2 », mais « Ton morceau est vide » et le projet dit 1 mesure : je doute que mes prises soient là
oeil-neuf: q9 | ecran: 55 | image: 55 | rien de coupé ni de chevauché
hesitations:
  - ecran: 10 | cherchais: revenir à « Mon projet 1 » | regarde: « Continuer » pointe maintenant sur Live, il faut passer par Mes projets
  - ecran: 34 | cherchais: où est ma prise | regarde: « Ta prise est dans une brique du Live (Plus › Briques) »
  - ecran: 44 | cherchais: la page Pad Live connue | regarde: « Pads » de Briques ouvre une autre grille, avec un ▶ et un Tout couper déplacés
abandon: jamais
incidents-banc:
defauts:
  - geste: 45 | ecran.vu: 45 | cible: bouton ▶ @ 375,48 | geste.type: touche
    attendu: boucle de kick en cours (écran 45) : le haut montre ■ et un toucher arrête | obtenu: le haut montre ▶ ; le toucher lance en plus la musique du projet (écran 46)
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: oui: ecran 41 (même chose dans Briques, #41) | banc?: non | promesse: P4
  - geste: 38 | ecran.vu: 54 | cible: bouton « Dans une brique du Live » | geste.type: touche
    attendu: prise de 6 mesures puis de 3 : le projet Live garde la longueur, la 2e vient à la suite | obtenu: projet « Live · Boucle de 1 mes. » (écrans 51, 52), lecture « A mesure 1/1 » (écran 54), les deux prises côte à côte en ligne A (Audio 1 / Audio 2, écran 41)
    symptome: perdu
    gravite: contourne-en-4-gestes | revu: oui: ecran 52 | banc?: non | promesse: P3
  - geste: 21 | ecran.vu: 21 | cible: texte « Boucle de kick » | geste.type: touche
    attendu: le pad attend la mesure suivante ([anneau pâle]) | obtenu: [allumé] tout de suite, sans attente visible (aussi #22, #23)
    symptome: rien
    gravite: finition | revu: oui: ecran 23 | banc?: temps | promesse: t6-Ab-4
  - geste: 50 | ecran.vu: 55 | cible: texte « Ton morceau est vide : ajoute tes parties dans l'ordre où elles jouent (Intro, Couplet, Refrain…). » | geste.type: touche
    attendu: le projet Live montre mes deux prises | obtenu: « Ton morceau est vide » alors que Audio 1 et Audio 2 sont listées
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - P2 | corrigé (écran 9 : Pad Live ouvert, 4 gestes d'attente, « rien ne joue » ; le Clap ne part qu'au toucher #20)
  - P3 | encore là (écran 54 : après prises de 6 et 3 mesures, le Live reste « Boucle de 1 mes. », « A mesure 1/1 »)
  - P4 | autre : corrigé sur la page Pad Live (écrans 23→24 : ■ puis un toucher arrête), encore là dans la vue Pads/Briques de Plus (écrans 41, 45 : ▶ affiché alors que la boucle tourne, le toucher lance le projet)
