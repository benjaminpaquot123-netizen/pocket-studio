siege: a | profil: t6-Fa musicien-italien-tablette | lang: it | device: tablet8 | font: 130 | mode: simple
mission: simple | buts: son, partie, jamais-pro
oeil-neuf: 15 écrans, 12 graves
promesse: id=t6-Fa-1 | texte: Un son joue dès le premier morceau | ecran: 7 | tenue: oui | pourquoi: ▶ « il brano » après 2 réglages de pas : le banc dit « la musique du projet joue en boucle »
promesse: id=t6-Fa-2 | texte: Une deuxième partie se crée et se joue | ecran: 15 | tenue: oui | pourquoi: « + Nuova parte (copia di A) » crée B (écran 9), le brano joue B puis A (écrans 15 et 30) ; mais A est sortie du brano (fiche plus bas)
promesse: id=t6-Fa-3 | texte: Je ne passe jamais en Pro sans l'avoir voulu | ecran: 83 | tenue: oui | pourquoi: toujours « Semplice » jusqu'à la fin, malgré Guida, Condividi, Impostazioni, Aiuto (je n'ai touché aucun bouton « Pro »)
promesse: id=t6-Fa-4 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 55 | tenue: non | pourquoi: « Sempli… » et « Batteri / a » coupés, mots anglais (tab, kick, timeline, Squelchy), faute « su il pad »
oeil-neuf: q1 | ecran: 83 | image: 83 | tout a l'air touchable sauf « Accordi del brano » (carte pâle) qui l'est ; la ligne de la partie B est à moitié sous la Guida
oeil-neuf: q6 | ecran: 83 | image: 83 | preuve de lecture : barre verte, « batt. 1.2 / 3 », ■ « il brano » vert ; mais « 0:00 / 0:08 » pendant que la barre avance
oeil-neuf: q9 | ecran: 83 | image: 83 | « Sempli… » coupé en haut ; « B × 2 · 2 batt. · suona » coupé par la Guida
hesitations:
  - ecran: 11 | cherchais: ma partie A dans « IL BRANO » après avoir créé B | regarde: seule « B × 1 » restait, j'ai dû retoucher « + A »
  - ecran: 25 | cherchais: une ligne de basse générée (« Genera una linea Acid 303 ») | regarde: un choix de son s'ouvre, rien n'indique qu'une ligne est créée
  - ecran: 37 | cherchais: mon projet dans « I miei progetti » | regarde: liste vide, retrouvé seulement par l'accueil « Continua »
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: aucun outil hors human-act.py (Read seulement sur les images de mon siège) ; menu de partage système fermé sans envoi (écran 66)
defauts:
  - geste: 2 | ecran.vu: 3 | cible: bouton « Semplice » | geste.type: touche
    attendu: l'étiquette de mode lisible en entier | obtenu: « Sempli… » coupé, sur tous les écrans du projet à 130 %
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 83 | banc?: non | promesse: t6-Fa-4
  - geste: 50 | ecran.vu: 55 | cible: bouton « Batteria ▸ » | geste.type: touche
    attendu: le nom de la piste sur une ligne | obtenu: « Batteri » / « a ▸ » coupé en deux lignes dans le Mix
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 55 | banc?: non | promesse: t6-Fa-4
  - geste: 74 | ecran.vu: 75 | cible: texte « Tocca un suono per ascoltarlo, “Usa” per metterlo su il pad 3 (Rullante). » | geste.type: appui-long
    attendu: « sul pad 3 » | obtenu: « su il pad 3 », faute d'italien (aussi écran 76)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 76 | banc?: non | promesse: t6-Fa-4
  - geste: 8 | ecran.vu: 11 | cible: texte « + Nuova parte (copia di A) » | geste.type: touche
    attendu: le brano = A puis B | obtenu: le brano ne contient plus que B (1 battuta) ; A rajoutée à la main par « + A », et elle passe après B
    symptome: incompris
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Fa-2
  - geste: 29 | ecran.vu: 30 | cible: bouton « Applica a tutto il brano » | geste.type: touche
    attendu: la carte affiche les nouveaux accords | obtenu: « Fatto: accordi del brano Do Sol La m Fa » mais la carte reste « B Do A Do » (encore écran 40)
    symptome: rien
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 37 | cible: bouton (dossier ouvert) « I miei progetti » | geste.type: touche
    attendu: mon « Il mio progetto 1 » dans la liste | obtenu: liste vide (aussi après 3 s) ; le projet n'apparaît qu'après l'avoir renommé (écran 72)
    symptome: perdu
    gravite: finition | revu: oui: ecran 38 | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: bouton (boîte, flèche vers le bas) › « Salva il brano in .zip Per un altro dispositivo o una copia di sicurezza » | geste.type: touche
    attendu: la ligne .zip entière | obtenu: la ligne passe sous les pastilles « M4A / WAV », seul le haut du titre dépasse
    symptome: cache
    gravite: finition | revu: oui: ecran 23 | banc?: non | promesse: -
  - geste: 23 | ecran.vu: 24 | cible: bouton « Più tardi » | geste.type: touche
    attendu: les 4 boutons de la Guida visibles | obtenu: « Più tardi », « Salta », « Scopri di più », « Non proporre più » coupés sous la barre du bas
    symptome: cache
    gravite: finition | revu: oui: ecran 26 | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: champ « Nome del brano inviato » | geste.type: retour
    attendu: rien n'est dit « envoyé » quand j'ai fermé le partage | obtenu: le champ devient « Nome del brano inviato » alors que rien n'a été envoyé
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 68 | banc?: non | promesse: -
mots-faux:
  - ecran: 59 | vu: Semplice: 5 tab | attendu: Semplice: 5 schede (comme l'écran 2)
  - ecran: 24 | vu: kick | attendu: grancassa
  - ecran: 42 | vu: timeline | attendu: sequenza (et pas de timeline en Semplice)
  - ecran: 25 | vu: Squelchy | attendu: un nom italien
  - ecran: 3 | vu: STEP: GRANCASSA 1 | attendu: PASSI: GRANCASSA 1
