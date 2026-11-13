siege: a | profil: t6-Ha-suite | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Ha-1 | texte: 1. Le premier lancement mène jusqu'au tuto Facile sans impasse | ecran: 4 | tenue: oui | pourquoi: « J'apprends » → « Simple … Avec le tuto Facile » → style House → chapitre 1/15 ouvert, en 3 gestes
promesse: id=t6-Ha-2 | texte: 2. Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 247 | tenue: oui | pourquoi: « Bravo, ton morceau est fini ! » atteint sans « Suivant » forcé, mais avec des détours (chap. 7 surtout)
promesse: id=t6-Ha-3 | texte: 3. Chaque étape du tuto se valide quand je fais le geste demandé | ecran: 239 | tenue: oui | pourquoi: chaque étape faite passe en « Réussi » ; un coup sur le mauvais pad (Cymbale) n'a pas validé, ce qui est juste
promesse: id=t6-Ha-4 | texte: 4. chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 13 | tenue: non | pourquoi: « Tonalité », « gamme mineure », « La », « 909 / 707 », « accord », « relances de fin de phrase », « Break » jamais expliqués ; « pad du tambourin » introuvable
promesse: id=t6-Ha-5 | texte: 5. tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 242 | tenue: non | pourquoi: tout est en français, mais « Tout est prêt : aucun solo oublié, aucune piste » coupé au bord, et le champ « Nom du morceau » coupé à l'écran 247
oeil-neuf: q1 | ecran: 247 | image: 247 | boutons nets (Écouter mon morceau, Recommencer le tuto, Partager, M4A/WAV) ; « Fermer le tuto » est un texte nu qu'on ne devine pas touchable
oeil-neuf: q6 | ecran: 247 | image: 247 | preuve claire (coupe + « Bravo, ton morceau est fini ! ») et suite évidente (Écouter mon morceau)
oeil-neuf: q9 | ecran: 247 | image: 247 | le champ « Nom du morceau » est coupé net par la rangée M4A / WAV qui passe dessus
chapitre: 1/15 « La vitesse et la couleur du morceau » | ecran: 4 | hésitation : « Tonalité », « Dorienne », « gamme mineure », « La » : je ne sais pas ce que je choisis
chapitre: 2/15 « Le kick » | ecran: 19 | hésitation : « kick », « 909 », « 707 », « pad » dans le texte d'accueil avant toute explication
chapitre: 3/15 « Le clap » | ecran: 33 | hésitation : « Je le fais » ouvre la mesure 2, le titre de mesure est caché en haut, je ne savais pas où j'étais
chapitre: 4/15 « Les charleys et le tambourin » | ecran: 41 | hésitation : « pad du tambourin » alors que la grille dit « Bord de caisse », et « 4 cases » alors qu'il en faut 8
chapitre: 5/15 « La basse » | ecran: 84 | hésitation : consigne longue (La grave, La aigu, Ré, Mi, Sol) ; notes à moitié cachées sous le panneau du tuto
chapitre: 6/15 « Le piano » | ecran: 109 | hésitation : « accord » jamais expliqué ; « Encore 36 notes » fait peur alors qu'il suffit de 9 touches
chapitre: 7/15 « La petite boucle » | ecran: 134 | bloqué : « Encore 12 notes en pointillé à toucher » sans aucune note visible ni flèche ; la dernière note était en mesure 3 au lieu de 2
chapitre: 8/15 « Ta première partie : le Groove » | ecran: 167 | compris du premier coup
chapitre: 9/15 « Comprendre les parties » | ecran: 174 | hésitation : « le Break et la sienne 16 + 8 » incompréhensible ; pas d'écran « Bravo » à la fin du chapitre
chapitre: 10/15 « Construis tes parties » | ecran: 187 | hésitation : « relances de fin de phrase » ; chaque bouton fait tout seul, je n'apprends pas comment
chapitre: 11/15 « Range tes parties » | ecran: 199 | hésitation : « dans « Le morceau » » alors que l'onglet s'appelle « Mon morceau »
chapitre: 12/15 « Les transitions » | ecran: 210 | hésitation : « Aller dans la partie « Drop 2 » » ouvre le pad Cymbale au lieu de Conga, sans le dire
chapitre: 13/15 « Les effets » | ecran: 230 | compris du premier coup
chapitre: 14/15 « Le mix : les volumes » | ecran: 236 | compris du premier coup
chapitre: 15/15 « Exporte ton morceau » | ecran: 241 | compris du premier coup
hesitations:
  - ecran: 3 | cherchais: quel style choisir | regarde: « 132 BPM », « 126 BPM »… : le mot BPM n'est expliqué qu'au chapitre 1
  - ecran: 17 | cherchais: comment passer au chapitre 2 après « Revenir au tuto » | regarde: panneau replié, il faut deviner de toucher « Bravo, chapitre fini ! » ⌃
  - ecran: 186 | cherchais: ce que veut dire la carte | regarde: « le Break et la sienne 16 + 8 » : « la sienne » ne renvoie à rien
abandon: jamais
incidents-banc:
  - geste: 33 | attente: 0 | vu: écran en texte vide (seule la pastille « Mode retours »), l'image montrait bien l'appli ; idem écrans 42, 53, 68, 200, 237, 242
  - geste: 0 | attente: 0 | vu: outils hors human-act.py : Read sur les consignes et sur les images screen-*.png de mon siège seulement
defauts:
  - geste: 6 | ecran.vu: 7 | cible: bouton « Télécharger le kit House (4 Mo) » [coupé] | geste.type: touche
    attendu: le bouton du kit visible dans la carte | obtenu: il est caché sous le fondu, il faut d'abord « Lire la suite » ; la carte revient à chaque étape
    symptome: cache | gravite: finition | revu: oui: ecran 13 | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: texte « À toi : choisis la couleur du morceau. « Je le fais » ouvre la fiche Tonalité : touche « Dorienne » (une gamme mineure avec une note claire qui donne le sourire), puis « La ». » | geste.type: touche
    attendu: des mots de tous les jours (mode Simple) | obtenu: Tonalité, gamme mineure, Dorienne, La, Tonique : je touche sans comprendre
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 33 | ecran.vu: 36 | cible: texte « Mesure 2/2 · Claquement de mains » [coupé] | geste.type: touche
    attendu: « Je le fais » ouvre la mesure 1 avec son titre visible | obtenu: il ouvre la mesure 2, titre caché en haut ; je remplis la mauvaise mesure d'abord (même chose aux chap. 4 écrans 46 et 59)
    symptome: perdu | gravite: contourne-en-2-gestes | revu: oui: ecran 59 | banc?: non | promesse: t6-Ha-4
  - geste: 66 | ecran.vu: 72 | cible: texte « À toi : sur le pad du tambourin, touche les 4 cases en pointillé, entre les temps. » | geste.type: touche
    attendu: un pad nommé tambourin, et 4 cases à toucher | obtenu: aucun pad « tambourin » (la grille dit « Bord de caisse ») et il faut 8 cases sur 2 mesures
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 92 | ecran.vu: 93 | cible: bouton « Basse · mesure 1/8 Encore 2 notes en pointillé à toucher » | geste.type: touche
    attendu: le bouton amène la note à toucher au milieu de la grille | obtenu: la note arrive au bord bas, à moitié sous le panneau du tuto (même chose écrans 97, 105, 163)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 163 | banc?: non | promesse: -
  - geste: 140 | ecran.vu: 141 | cible: bouton « Mélodie · mesure 1/8 Encore 12 notes en pointillé à toucher » | geste.type: touche
    attendu: voir où sont les notes, ou une flèche « plus haut / plus bas » | obtenu: rien de visible, pas de flèche ; il faut deviner de faire glisser la grille
    symptome: sans-aide | gravite: contourne-en-4-gestes | revu: oui: ecran 158 | banc?: non | promesse: t6-Ha-4
  - geste: 162 | ecran.vu: 163 | cible: icône « À toi : touche « Créer la piste Mélodie », puis les 8 notes en pointillé de la mesure 1, puis les 8 de la mesure 2. « Copier sur les mesures suivantes » fait le reste. Lire » ⌃ | geste.type: glisse
    attendu: 8 notes en mesure 1 et 8 en mesure 2 comme écrit | obtenu: la dernière note à poser est en mesure 3 ; « Je le fais » ne la montre pas
    symptome: texte-faux | gravite: contourne-en-5-gestes | revu: oui: ecran 161 | banc?: non | promesse: t6-Ha-4
  - geste: 220 | ecran.vu: 221 | cible: texte « Mesure 1/4 · Cymbale » | geste.type: touche
    attendu: « Aller dans la partie « Drop 2 » » ouvre le pad Conga demandé | obtenu: il ouvre le pad Cymbale ; mon coup dessus ne fait rien, sans message
    symptome: mauvais-ecran | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 241 | ecran.vu: 242 | cible: texte « Tout est prêt : aucun solo oublié, aucune piste » | geste.type: touche
    attendu: phrase entière | obtenu: phrase coupée au bord droit de l'écran (vu sur l'image 242)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ha-5
  - geste: 246 | ecran.vu: 247 | cible: champ « Nom du morceau (et du projet) » contenu « Mon morceau » [coupé] | geste.type: touche
    attendu: le champ du nom entier | obtenu: la rangée « M4A · pour partager / WAV · qualité max » passe sur le champ et coupe le nom
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ha-5
