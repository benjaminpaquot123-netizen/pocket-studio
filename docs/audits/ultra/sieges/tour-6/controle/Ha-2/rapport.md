code testé : b4ac25f2
joué par : sous-agent neuf
siege: a | profil: t6-Ha-controle | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Ha-1 | texte: Le premier lancement mène jusqu'au tuto Facile sans impasse | ecran: 4 | tenue: oui | pourquoi: J'apprends → Simple (« Avec le tuto Facile ») → House → chapitre 1/15 en 3 gestes
promesse: id=t6-Ha-2 | texte: Le tuto Facile se suit du premier chapitre au « Bravo » sans impasse | ecran: 233 | tenue: oui | pourquoi: 15 chapitres suivis sans « Suivant » forcé, « Bravo, ton morceau est fini ! » au geste 233
promesse: id=t6-Ha-3 | texte: Chaque étape du tuto se valide quand je fais le geste demandé | ecran: 214 | tenue: oui | pourquoi: chaque étape passe à « Réussi » dès le geste fait (écoute, cases, notes, parties, curseurs, export)
promesse: id=t6-Ha-4 | texte: chaque étape du tuto dit clairement quoi faire, sans mot de musicien non expliqué | ecran: 186 | tenue: non | pourquoi: « relances de fin de phrase », « Groove », « Tonique », « La m », « solo », « saturation », « (−14) » jamais expliqués ; « kick » expliqué une étape après le titre
promesse: id=t6-Ha-5 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 233 | tenue: non | pourquoi: tout est en français, mais des textes sont coupés (bouton du kit sous « Pourquoi ? », champ « Nom du morceau », « Tout est prêt… » écran 228)
oeil-neuf: q1 | ecran: 233 | image: 233 | tout ce qui a l'air touchable l'est ; « Mes fichiers » et « ? » bien lisibles ; le champ du nom à moitié caché ne se devine plus comme un champ
oeil-neuf: q6 | ecran: 233 | image: 233 | preuve claire : coupe + « Bravo, ton morceau est fini ! » ; suite proposée : Écouter / Recommencer / Fermer le tuto
oeil-neuf: q9 | ecran: 233 | image: 233 | oui : le champ « Nom du morceau (et du projet) » est coupé, son texte passe sous les boutons M4A/WAV
chapitre: 1/15 « La vitesse et la couleur du morceau » | ecran: 4 | hésitation : deux boutons pour avancer (« Étape suivante » et « Suivant ») ; « Tonique » sans explication
chapitre: 2/15 « Le kick » | ecran: 19 | hésitation : le titre dit « kick », expliqué seulement à l'étape 2/2
chapitre: 3/15 « Le clap » | ecran: 33 | hésitation : après « Je le fais », la grille ne montre pas le nom du pad (titre caché en haut)
chapitre: 4/15 « Les charleys et le tambourin » | ecran: 41 | hésitation : le pad « Bord de caisse » « joue le tambourin » ; « les 4 cases » mais il y en a 8
chapitre: 5/15 « La basse » | ecran: 84 | hésitation : consigne de 4 actions en un bloc ; 2 notes cachées au-dessus de la grille, il faut la faire défiler
chapitre: 6/15 « Le piano » | ecran: 107 | hésitation : la « note du bas » de l'accord est cachée sous le panneau du tuto ; « Encore 9 notes » alors que 12 sont en pointillé
chapitre: 7/15 « La petite boucle » | ecran: 132 | hésitation : 16 notes réparties hors de vue, 3 défilements à deviner
chapitre: 8/15 « Ta première partie : le Groove » | ecran: 157 | hésitation : « Groove » jamais expliqué
chapitre: 9/15 « Comprendre les parties » | ecran: 164 | compris du premier coup
chapitre: 10/15 « Construis tes parties » | ecran: 178 | hésitation : « relances de fin de phrase (« Montre-moi » les pose) » : je ne sais pas si toucher « Montre-moi » ou « Créer »
chapitre: 11/15 « Range tes parties » | ecran: 190 | hésitation : les boutons « Ajouter » descendent après le premier ajout (texte devient « Ajouter à la fin »)
chapitre: 12/15 « Les transitions » | ecran: 201 | hésitation : « ajoute quelques coups » sans nombre ni case guidée ; « La m » dans la bande d'accords
chapitre: 13/15 « Les effets » | ecran: 216 | compris du premier coup
chapitre: 14/15 « Le mix : les volumes » | ecran: 222 | compris du premier coup
chapitre: 15/15 « Exporte ton morceau » | ecran: 227 | hésitation : deux consignes « À toi » à l'écran (celle du Mix et celle du tuto)
hesitations:
  - ecran: 186 | cherchais: quoi toucher pour les « relances de fin de phrase » | regarde: « Créer la partie « Drop 2 » » et « Montre-moi » côte à côte, la consigne cite les deux
  - ecran: 112 | cherchais: la « note du bas » de l'accord à toucher | regarde: grille coupée par le panneau du tuto, il a fallu faire défiler
  - ecran: 67 | cherchais: le pad du tambourin | regarde: aucun pad « Tambourin », la consigne dit « Bord de caisse » (il joue le tambourin)
abandon: jamais
incidents-banc:
  - geste: 34 | attente: 0 | vu: écran en texte vide après « Je le fais » (même chose aux gestes 42, 53, 68-69, 191-198, 223, 228) ; image normale, j'ai joué sur l'image
  - geste: 0 | attente: 0 | vu: outils hors human-act.py : Read sur les consignes et les .png de « regarde image », grep/head/ls pour filtrer la sortie du banc et lister mon dossier de siège
defauts:
  - geste: 186 | ecran.vu: 186 | cible: texte « À toi : crée le Drop 2 : tout ton Groove, avec la batterie de l'exemple et ses relances de fin de phrase (« Montre-moi » les pose). » | geste.type: touche
    attendu: une consigne sans mot de musicien, un seul bouton à toucher | obtenu: « relances de fin de phrase » non expliqué, et on ne sait pas s'il faut « Montre-moi » ou « Créer »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 157 | ecran.vu: 157 | cible: texte « Ta première partie : le Groove Chapitre 8/15 · Étape 1/2 · Facile » | geste.type: touche
    attendu: « Groove » expliqué comme les autres mots (« • … ») | obtenu: le mot sert de titre et de nom de partie, jamais expliqué
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 18 | ecran.vu: 19 | cible: texte « Le kick Chapitre 2/15 · Étape 1/2 · Facile » | geste.type: touche
    attendu: « kick » expliqué dès le titre du chapitre | obtenu: l'explication « • Kick : la grosse caisse » n'arrive qu'à l'étape 2/2 (écran 20)
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 67 | ecran.vu: 67 | cible: texte « À toi : sur le pad « Bord de caisse » (il joue le tambourin), touche les 4 cases en pointillé, entre les temps. » | geste.type: touche
    attendu: le pad porte le nom du son qu'il joue, et « 4 cases » = tout ce qu'il faut faire | obtenu: pad « Bord de caisse » qui joue un tambourin ; il faut 8 cases (4 de plus en mesure 2)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 112 | ecran.vu: 112 | cible: bouton « Accords » | geste.type: touche
    attendu: la note du bas de l'accord, à toucher, est visible | obtenu: elle est sous le panneau du tuto ; il faut glisser la grille pour la voir
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 113 | banc?: non | promesse: t6-Ha-4
  - geste: 87 | ecran.vu: 91 | cible: bouton « Basse · mesure 1/8 Encore 2 notes en pointillé, plus haut ↑ » | geste.type: touche
    attendu: les notes de la mesure visibles d'un coup | obtenu: 2 notes hors de vue au-dessus, 2 glissements pour les trouver (même chose chapitres 6 et 7)
    symptome: cache
    gravite: contourne-en-2-gestes | revu: oui: ecran 101 | banc?: non | promesse: -
  - geste: 119 | ecran.vu: 119 | cible: bouton « Accords · mesure 3/8 Encore 9 notes en pointillé à toucher » | geste.type: touche
    attendu: le compte égal aux notes en pointillé (12 visibles) ou à la consigne (« 4 coups ») | obtenu: « Encore 9 notes » (et « Encore 16 notes » écran 111 pour « 4 coups »)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 162 | ecran.vu: 162 | cible: bouton « Télécharger le kit House (4 Mo) » [coupé] | geste.type: touche
    attendu: le bouton du kit lisible en entier | obtenu: son texte est coupé et recouvert par « ▸ Pourquoi ? »
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 178 | banc?: non | promesse: t6-Ha-5
  - geste: 233 | ecran.vu: 233 | cible: champ « Nom du morceau (et du projet) » contenu « Mon morceau » [coupé] | geste.type: touche
    attendu: le champ du nom entier au-dessus des boutons M4A/WAV | obtenu: le champ est coupé, son texte passe sous les boutons
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-5
  - geste: 6 | ecran.vu: 6 | cible: bouton « Étape suivante   » → | geste.type: touche
    attendu: un seul bouton pour avancer | obtenu: « Étape suivante » et « Suivant » côte à côte font la même chose, j'ai hésité
    symptome: incompris
    gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: -
mots-faux:
  - ecran: 14 | vu: Tonique | attendu: note de départ (mot expliqué)
  - ecran: 201 | vu: La m | attendu: La mineur (expliqué)
  - ecran: 228 | vu: Niveau : comme la musique en streaming (−14) | attendu: une phrase sans chiffre de mesure
compte: hesitations=15
defauts-controle-Ha:   (ajouté par l'hôte du siège d'après controle/Ha/rapport.md et les écrans .txt de ce passage)
  - tempo affiché 100 alors que « House 122 BPM » est choisi | encore là | écran 4 « 100  Tempo » ; le testeur ne l'a plus noté comme défaut
  - « BPM » non expliqué au choix du style | autre | plus signalé par ce testeur
  - « Chapitre suivant » caché dans la bande repliée | corrigé | bouton « Chapitre suivant » visible (écrans 16-18)
  - bouton « Télécharger le kit » coupé, bloc répété | encore là | défaut #162 (coupé sous « Pourquoi ? »)
  - perdu 8 gestes après « Copier sur les mesures suivantes » (basse) | corrigé | plus de flottement ; reste 2 notes hors de vue à faire défiler (défaut #87, contourne en 2 gestes)
  - fausse note posée sur une note coupée au bord droit | corrigé | plus vue
  - compteur « Encore 9 notes » qui ne correspond pas aux touchers | encore là | défaut #119
  - « Accords » resté allumé sur la piste Mélodie | corrigé | plus vu ni signalé
  - « relances de fin de phrase » non expliqué, réussite muette au ch. 10 | encore là | défaut #186 (mot non expliqué, Montre-moi ou Créer ?)
  - « Tout est prêt : aucun solo oublié… » coupé à l'export | corrigé | plus signalé ; le champ « Nom du morceau » reste coupé (défaut #233)
