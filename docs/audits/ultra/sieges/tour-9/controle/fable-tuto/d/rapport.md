code testé : d87dea73ff18887bc55b2ac5e0987f5e17be4c21
joué par : sous-agent neuf
siege: d | profil: debutant-motive | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=- | texte: « Ton morceau avait déjà ce que demande cette étape : rien à toucher. » | ecran: 70 | tenue: non | pourquoi: étape 3/3 (4 coups de maracas dans le Drop 2) dite « Déjà fait » alors que je n'avais mis qu'une seule case, et l'écran montre les maracas du Drop (D) toutes vides
hesitations:
  - ecran: 13 | cherchais: ce qu'il fallait faire de plus à l'étape 2 après avoir touché la 1re case de la cymbale | regarde: la case était remplie, rien ne bougeait, j'ai vidé/rempli, joué la partie, fait Précédent/Suivant
  - ecran: 32 | cherchais: les cases en pointillé de l'étape 3 (Drop 2, Maracas) | regarde: aucune case en pointillé nulle part
  - ecran: 64 | cherchais: la cymbale à toucher au 2e passage | regarde: le Drop affichait « Vider · Mesure 2 », pas de pointillé ; touché la 1re case de la mesure 1 quand même
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: j'ai filtré la sortie de human-act.py avec grep/head (lecture seulement) et lu les premières lignes de rapport.md avec head
  - geste: 49 | attente: 0 | vu: toucher à 114,1106 n'a rien touché car la feuille avait bougé (Renommer était à 114,1151) ; refait au bon endroit, pas un défaut
defauts:
  - geste: 13 | ecran.vu: 13 | cible: bouton « 1 » [état « vide, en pointillé : à toucher »] @ 70,363 | geste.type: touche
    attendu: l'étape 2/3 « dans le Drop, touche la 1re case du pad de la cymbale » est validée (Réussi) | obtenu: la case se remplit, mais l'étape n'est jamais validée ; même chose en vidant/remplissant, en jouant la partie, après Précédent/Suivant, au 2e passage (écran 65) et même avec « Je le fais » (écran 67) ; seule sortie : Suivant ×2 « pour la passer quand même »
    symptome: rien
    gravite: contourne-en-2-gestes | revu: oui: ecran 67 | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 33 | cible: texte « À toi : dans le Drop 2, pad « Maracas », touche 4 cases, une par temps de la mesure 1. » | geste.type: touche
    attendu: l'étape 3/3 se valide quand les 4 cases sont touchées | obtenu: « Réussi » après une seule case ; au 2e passage (écran 70) « Déjà fait ! » alors que les maracas affichées (partie D · Drop) sont toutes vides
    symptome: incompris
    gravite: finition | revu: oui: ecran 70 | banc?: non | promesse: -
  - geste: 32 | ecran.vu: 32 | cible: texte « À toi : dans le Drop 2, pad « Maracas », touche 4 cases, une par temps de la mesure 1. » | geste.type: touche
    attendu: des cases en pointillé dans le Drop 2 montrent où toucher, comme à l'étape 2 | obtenu: aucune case en pointillé ; au 2e passage l'étape 3 s'ouvre sur la partie D · Drop au lieu du Drop 2 (écran 70)
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 70 | banc?: non | promesse: -
  - geste: 34 | ecran.vu: 54 | cible: bouton « 12 Les transitions 2/3 » | geste.type: touche
    attendu: après la dernière étape, un écran bravo et le chapitre 12 coché | obtenu: pas de bravo, l'appli passe directement au chapitre 13 « Les effets » ; la liste des chapitres garde « 12 Les transitions 2/3 » (revu écran 73)
    symptome: mauvais-ecran
    gravite: finition | revu: oui: ecran 73 | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 12 | cible: icône (flèches en boucle) « Montée mesure 17/96 » ▾ | geste.type: touche
    attendu: l'en-tête dit la partie où je suis (Drop) | obtenu: l'en-tête dit « Montée mesure 17/96 » alors que la partie affichée est « D · Drop »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 17 | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 56 | cible: bouton « Créer la partie « Montée » » | geste.type: touche
    attendu: si une partie Montée existe déjà (H · Montée, vide), le tuto dit d'y aller ou de la remplir | obtenu: « À toi : touche « Créer la partie Montée » », comme si elle n'existait pas ; le bouton remplit en fait la H · Montée existante
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 57 | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 5 | cible: texte « À toi : dans la partie « Montée », dernière mesure, efface les coups du dernier temps (touche chaque croix rouge) : un temps de silence juste avant le Drop. » | geste.type: touche
    attendu: une consigne = un ou deux gestes | obtenu: il a fallu 5 touches sur 3 pads (Charley ×3, Charley ouvert, Bord de caisse) ; l'étape 3 demande 4 touches
    symptome: incompris
    gravite: finition | revu: oui: ecran 62 | banc?: non | promesse: -
  - geste: 63 | ecran.vu: 64 | cible: texte « Vider · Mesure 2 » | geste.type: touche
    attendu: au 2e passage de l'étape 2, la cymbale du Drop montre la case à toucher en pointillé | obtenu: toutes les cases de la cymbale sont vides, aucun pointillé, l'étape n'est pas « Déjà fait » alors que j'avais rempli la case au 1er passage
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 65 | banc?: non | promesse: -
mots-faux:
defauts-avant:
  - montee-vide | corrigé | ecran: 56 | avec une partie « H · Montée » vide (et C renommée « Pont »), l'étape 1/3 n'est pas validée (pas de « Déjà fait », pas de coche) et l'appli ne dit jamais « Il n'y a pas de partie Montée » ; elle propose seulement « Créer la partie Montée » (mot gênant, voir défauts), qui remplit la H existante
  - cible-autre-partie | autre | ecran: 12 | à l'étape 2/3 la case en pointillé est bien dans le Drop dont parle la consigne (pas dans A) ; mais à l'étape 3/3 il n'y a aucune case en pointillé (écran 32), et au 2e passage l'étape 3 s'ouvre sur les maracas de la partie D · Drop au lieu du Drop 2 (écran 70)
  - d-fin | encore là | ecran: 73 | l'étape 2/3 ne se valide jamais en faisant ce qu'elle dit (même « Je le fais », écran 67) ; l'étape 3/3 se valide (trop tôt, après 1 case sur 4) ; le chapitre 12 reste « 2/3 » dans la liste, pas de bravo
  - D4 | encore là | ecran: 5 | l'étape 1 demande 5 touches sur 3 pads ; l'étape 3 demande 4 touches (écran 32)
