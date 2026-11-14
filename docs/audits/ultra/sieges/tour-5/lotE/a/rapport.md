siege: a | profil: t5-Ea-casse-tout-impatient | lang: fr | device: phone | font: 100 | mode: simple
mission: libre | buts: but1, but2, but3
promesse: id=t5-Ea-1 | texte: « Que veux-tu faire ? » | ecran: 27 | tenue: oui | pourquoi: projet ouvert supprimé depuis Mes projets puis Retour → accueil « Que veux-tu faire ? » ; refait écran 55 (Live) et 103 (Techno, via le menu Plus)
promesse: id=t5-Ea-2 | texte: « Enregistrer ma mélodie » | ecran: 20 | tenue: oui | pourquoi: Retour pendant la prise puis Continuer : « Ta prise : 3 notes » + Garder/Refaire, gardées (grille 5 notes écran 70) ; refait écran 141
promesse: id=t5-Ea-2 | texte: « Enregistrer ma mélodie » | ecran: 125 | tenue: non | pourquoi: Retour pendant la prise, puis Pad Live, puis Mes projets > Techno : la prise a disparu, piste Mélodie vide (« Grille des notes : 0 notes ») ; refait écran 149
promesse: id=t5-Ea-3 | texte: (brief) jamais d'écran vide ni coincé | ecran: 149 | tenue: oui | pourquoi: ~40 Retour en tout genre, toujours un écran utilisable, la musique s'arrête en sortant
hesitations:
  - ecran: 31 | cherchais: mes notes après réouverture | regarde: piste « Synthé » resélectionnée, « Vider » grisé : j'ai cru ma mélodie perdue avant de toucher « Ma mélodie »
  - ecran: 47 | cherchais: mon projet après un aller-retour éclair dans Pad Live | regarde: « Continuer » pointe maintenant sur « Live » (le petit message l'annonçait, mais il disparaît vite)
  - ecran: 95 | cherchais: quitter le morceau qui joue | regarde: le 1er Retour a seulement replié le Guide, il en faut un 2e
abandon: jamais
incidents-banc:
  - geste: 2 | attente: 30 | vu: écrans coréens 900x1509 d'un autre siège : mon raccourci de commande (fichier partagé du scratchpad) avait été réécrit par le testeur du siège c ; 3 gestes (retour, attends 30, retour) sont partis dans le siège lotE/c (son journal : « #6 > attends 30 », « #13 > retour »). Corrigé avec un raccourci privé ; à exclure du siège c.
defauts:
  - geste: 117 | ecran.vu: 125 | cible: bouton « Pad Live NOUVEAU Des pads prêts à jouer, tes sons, des effets de scène. Ça sonne tout de suite. » | geste.type: touche
    attendu: ma prise de 4 notes (Retour pendant « Enregistrer ma mélodie ») m'attend encore (Garder/Refaire) quand je rouvre Techno après un passage par Pad Live
    obtenu: plus de « Ta prise », piste Mélodie vide dans les parties I, E et A (« Grille des notes : 0 notes », « Vider » grisé) : notes perdues
    symptome: perdu
    gravite: mission-impossible | revu: oui: ecran 149 | banc?: non | promesse: t5-Ea-2
  - geste: 77 | ecran.vu: 78 | cible: bouton « Continuer Pro Mon projet 1 Boucle de 4 mes. Sauvegardé à l'instant » | geste.type: retour
    attendu: la carte garde « Simple » : j'ai choisi Pro pour un nouveau morceau que j'ai abandonné, pas pour Mon projet 1
    obtenu: la carte annonce « Pro », mais le projet s'ouvre en Simple (écran 79) et la carte redevient « Simple » ensuite (écran 80)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 105 | ecran.vu: 106 | cible: bouton « Retour » | geste.type: retour
    attendu: Mes projets ouvert depuis le nom du projet dans l'éditeur, sans rien changer : Retour me ramène dans mon morceau
    obtenu: Retour mène à l'accueil « Que veux-tu faire ? » ; il faut retoucher « Continuer »
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: texte « Octave 3 » | geste.type: touche
    attendu: l'étiquette d'octave colle aux touches affichées
    obtenu: « Octave 3 » alors que les touches s'appellent « Do 4 » à « Si 5 »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 111 | banc?: non | promesse: -
mots-faux:
