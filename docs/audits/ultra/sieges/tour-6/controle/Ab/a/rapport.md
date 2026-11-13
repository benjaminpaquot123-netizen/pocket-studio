code testé : b3617da2
joué par : sous-agent neuf
siege: a | profil: t6-Ab musicienne-pressee | lang: fr | device: phone | font: 100 | mode: simple
mission: padlive | buts: son, stop, prise
promesse: id=t6-Ab-1 | texte: Une mélodie enregistrée est encore là après Retour pendant la prise, un passage par Pad Live, puis le retour au projet | ecran: 40 | tenue: oui | pourquoi: prise au Clavier (« Ta prise : 3 notes », écran 9), Retour en pleine prise, Pad Live, puis Mes projets > « Mon projet 1 » : piste « Ma mélodie », « Vider » actif et notes visibles dans « Notes » > « Tout voir » (image 40)
promesse: id=t6-Ab-2 | texte: « Tout couper » arrête tout, tempo et compteur compris | ecran: 28 | tenue: oui | pourquoi: 3 boucles + musique du projet pendant REC : tout s'éteint, ▶ revient, « Son : rien ne joue » (refait écran 50, la prise est alors close et gardée)
promesse: id=t6-Ab-3 | texte: Un glissé sur les effets de Pad Live ne lance aucun effet | ecran: 21 | tenue: oui | pourquoi: glissé de « Filtre » à « Roulement » : aucun bouton allumé, le son ne signale que les 3 boucles (le banc ne signale pas non plus l'appui long, écran 22 : preuve faible)
promesse: id=t6-Ab-4 | texte: Un pad touché attend la mesure suivante avant de partir | ecran: 20 | tenue: oui | pourquoi: « Boucle Acid 303 » retouchée : [anneau pâle] et « le pad live 3 attend sa ligne (le prochain temps, ou la prochaine mesure) » ; aux écrans 13, 14, 47, 48 le pad était déjà allumé au retour d'écran (temps de geste du banc)
oeil-neuf: q1 | ecran: 59 | image: 59 | les 16 pads, A-D, Fête, ★, ?, ▶, −/+, REC, les 3 effets, Tout couper, Enregistrer, Plus ont l'air touchables et le sont ; « 124 Tempo » au milieu de − et + ne se touche pas, c'est normal
oeil-neuf: q6 | ecran: 59 | image: 59 | rien ne dit que je suis dans un autre projet (« Live ») ni où sont mes prises ; le ▶ du haut reste ▶ alors que des boucles tournent (écrans 12-21), seul REC le passe en ■
oeil-neuf: q9 | ecran: 59 | image: 59 | rien de coupé ; « Claquement de mains » est écrit plus petit que les autres pads, « Tout couper » tient juste sur deux lignes
hesitations:
  - ecran: 31 | cherchais: ma mélodie en rouvrant le projet | regarde: le projet rouvre sur « Rythme », puis sur la piste « Acid 303 » au Clavier avec « Vider » grisé : on croit la prise perdue jusqu'à toucher « Ma mélodie »
  - ecran: 34 | cherchais: les notes de ma prise | regarde: la grille est sous le Guide, il faut faire défiler puis « Tout voir » [coupé] pour voir les 4 mesures
  - ecran: 13 | cherchais: si le pad attend la mesure | regarde: le pad est déjà allumé au retour d'écran, l'anneau pâle n'est vu qu'à l'écran 20
abandon: jamais
incidents-banc:
  - geste: 33 | attente: 0 | vu: j'ai écrit puis effacé un fichier texte temporaire dans mon dossier de siège pour lire une sortie trop longue (aucun outil hors human-act.py sinon que Read sur mes images et journal.txt de mon siège)
defauts:
  - geste: 41 | ecran.vu: 42 | cible: bouton (grille de pads) « Pad Live NOUVEAU Des pads prêts à jouer, tes sons, des effets live. Ça sonne tout de suite. » → | geste.type: touche
    attendu: Pad Live s'ouvre en silence, rien ne sonne tant que je ne touche pas un pad | obtenu: « le pad live 2 (« Clap ») a joué une fois » et le pad Claquement de mains s'allume sans que je le touche (revu écrans 45 et 59)
    symptome: incompris | gravite: finition | revu: oui: ecran 45 | banc?: son | promesse: -
  - geste: 49 | ecran.vu: 54 | cible: bouton « Live OUVERT à l'instant · 124 BPM · 1 mes. · 0:02 · 1 partie » | geste.type: touche
    attendu: ma première perf gardée (« Perf gardée · 0:07 », projet « Live 4 mes. · 0:08 ») reste entière après une 2e prise courte | obtenu: après une 2e prise de 0:01 le projet Live passe à « 1 mes. · 0:02 », « A mesure 1/1 » : le morceau ne joue plus qu'une mesure (2 pistes « Pads live » quand même là, écran 55)
    symptome: perdu | gravite: finition | revu: non | banc?: son | promesse: -
  - geste: 11 | ecran.vu: 20 | cible: bouton ▶ @ 231,45 | geste.type: touche
    attendu: le bouton lecture du haut montre ■ quand des boucles tournent | obtenu: il reste ▶ avec 3 boucles en route (image 20), il ne passe à ■ qu'avec REC (écran 23) : on croit que rien ne joue
    symptome: incompris | gravite: finition | revu: oui: ecran 47 | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: onglet (quatre carrés) « Rythme » [sélectionné] | geste.type: touche
    attendu: le projet rouvre là où j'enregistrais ma mélodie (Clavier, piste « Ma mélodie ») | obtenu: il rouvre sur Rythme, puis Clavier sur « Acid 303 » avec « Vider » grisé : la mélodie semble absente
    symptome: incompris | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t6-Ab-1
defauts-tour-6:   (ajouté par l'hôte du siège d'après lotA/b/rapport.md et les écrans .txt/images de ce passage)
  - pad qui part tout de suite au lieu d'attendre la mesure (lotA/b #13, promesse t6-Ab-4) | corrigé | [anneau pâle] et « attend sa ligne » vus écran 20, promesse tenue
  - « Notes » montre la mesure 1 seule, mélodie qui semble perdue (lotA/b #34) | encore là | hésitation écran 34 : il faut faire défiler puis « Tout voir » ; s'y ajoute le projet rouvert sur Rythme / « Acid 303 » (défaut #30)
  - titre « EFFETS LIVE » rogné sous les pads (lotA/b #17) | corrigé | œil neuf q9 écran 59 : rien de coupé
