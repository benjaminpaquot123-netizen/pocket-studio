siege: c | profil: t5-Ac etudiante-anxieuse-renomme | lang: fr | device: phone | font: 100 | mode: simple
mission: libre | buts: but1, but2, but3
promesse: id=t5-Ac-1 | texte: Renommer | ecran: 8 | tenue: oui | pourquoi: champ touché, ✕ Effacer, texte tapé, OK : chaque écran revient tout de suite, nom pris (écrans 8-12, puis 24-28 et 50-54), jamais figé
promesse: id=t5-Ac-2 | texte: Projet final Léa OUVERT à l'instant · 120 BPM · Boucle de 1 mes. | ecran: 67 | tenue: oui | pourquoi: après Retour puis Accueil › Mes projets, la carte porte le nouveau nom (déjà vu avec « Devoir musique L1 » écran 12)
promesse: id=t5-Ac-3 | texte: Mélodie refrain Changer le son | ecran: 69 | tenue: oui | pourquoi: la piste « Synthé » renommée garde son nom en Pro, en Simple (Mix, Clavier, Mes pistes) et après sortie/réouverture ; Annuler sur un 2e renommage ne l'abîme pas
promesse: id=accueil-mes-projets | texte: Mes projets Ouvrir un autre morceau, le copier ou le renommer. | ecran: 13 | tenue: oui | pourquoi: Options › Renommer présent et fonctionne
hesitations:
  - ecran: 15 | cherchais: renommer ma piste depuis « MES PISTES » | regarde: appui long sur le nom (ouvre les sons), puis Réglages de la piste : pas de Renommer
  - ecran: 21 | cherchais: renommer la piste dans le Mix | regarde: appui long sur « Synthé » : rien ne se passe
  - ecran: 30 | cherchais: passé en Pro par peur que ce soit réservé aux pros | regarde: feuille de piste du Mix, onglet Plus, Aide : rien sur renommer une piste ; trouvé enfin par appui long sur l'onglet de piste après avoir glissé la rangée
abandon: jamais
incidents-banc:
defauts:
  - geste: 18 | ecran.vu: 18 | cible: bouton « Réglages de Synthé : volume, couper, supprimer » | geste.type: touche
    attendu: un « Renommer » dans les réglages de la piste, là où l'appli dit « MES PISTES » | obtenu: Volume, Écouter seule, Changer le son, Supprimer : pas de Renommer ; il est caché dans l'onglet Clavier (toucher le nom de piste en haut)
    symptome: cache
    gravite: contourne-en-3-gestes | revu: oui: ecran 59 | banc?: non | promesse: t5-Ac-3
  - geste: 39 | ecran.vu: 40 | cible: texte « Glisse le doigt pour triturer le son en direct. Deux touchers : retour au son de départ. » | geste.type: glisse
    attendu: faire défiler la feuille de la piste pour voir la suite | obtenu: le doigt est tombé sur le pad XY : « Espace » passe à 100 % et « Sans effet » n'est plus coché, sans que je le veuille
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 24 | cible: bouton « Annuler » | geste.type: touche
    attendu: un bouton Annuler écrit en toutes lettres | obtenu: renommage en haut de l'écran : deux croix ✕ presque collées (Effacer dans le champ, Annuler à côté), même dessin ; j'ai eu peur de tout perdre
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: t5-Ac-1
  - geste: 23 | ecran.vu: 24 | cible: touchable « Devoir musique L1 Sauvegardé » | geste.type: touche
    attendu: le nom de mon projet en entier dans la barre du haut | obtenu: « Devoir musiqu… » coupé alors que le nom fait 17 lettres sur 40 permises
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: bouton « Retour » | geste.type: retour
    attendu: Retour depuis Mes projets (ouvert par le nom du projet) me ramène dans mon morceau | obtenu: je tombe sur l'Accueil « Que veux-tu faire ? » ; il faut toucher Continuer
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 61 | ecran.vu: 62 | cible: champ « Nom 0/24 » contenu «  » | geste.type: ecris
    attendu: à l'ouverture de « Renommer la piste », puis ✕ Effacer, je peux taper directement | obtenu: rien ne s'écrit tant que je n'ai pas retouché le champ (le clavier n'attend pas)
    symptome: rien
    gravite: contourne-en-1-gestes | revu: non | banc?: clavier | promesse: t5-Ac-1
