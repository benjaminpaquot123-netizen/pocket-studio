siege: b | profil: t5-Cb dj-espagnol-voix-sur-pads | lang: es | device: phone411 | font: 100 | mode: pro
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t5-Cb-1 | texte: “Voz” está en la casilla Remix del bloque A. | ecran: 33 | tenue: non | pourquoi: envoyée « En el Live », la colonne « Ma chanson préférée » est coupée d'office ; la case dit « suena » alors que toutes les parties sont coupées
promesse: id=t5-Cb-2 | texte: Cancelar | ecran: 63 | tenue: oui | pourquoi: après Cancelar (puis Atrás, Continuar), le projet n'a ni nouvelle piste ni nouveau projet ; idem à l'écran 15
promesse: id=t5-Cb-3 | texte: “Voz 3” está en el pad live 3. | ecran: 44 | tenue: non | pourquoi: le même bout renvoyé 3 fois donne Voz 1, Voz 2, Voz 3 identiques sur 3 pads, sans aucun avertissement
promesse: id=t5-Cb-4 | texte: Cortes: Frases | ecran: 36 | tenue: non | pourquoi: mode Frases (puis Transitorios) choisi, au retour sur la découpe il est revenu à « Un trozo »
hesitations:
  - ecran: 23 | cherchais: comment choisir un bout en mode Frases (glisser, toucher l'onde) | regarde: l'aide « Herramientas de corte », puis le petit numéro « 1 » au-dessus de l'onde
  - ecran: 31 | cherchais: où est passé mon 2e envoi « bloque B » | regarde: la grille Live, case B vide
  - ecran: 53 | cherchais: un bouton Cancelar dans le projet pour tester la promesse | regarde: Elegir una canción, puis la page Remix de l'accueil
abandon: jamais
incidents-banc:
  - geste: 5 | attente: 1100 | vu: séparation par paliers de 15 % (≈ 13 min en tout), écran toujours revenu ; banc?: temps
defauts:
  - geste: 32 | ecran.vu: 33 | cible: bouton « Voz · 2 » (nom « Ma chanson préférée · A · suena ») | geste.type: touche
    attendu: le bout de voix envoyé au Live s'entend dans sa colonne, ou la case ne dit pas « suena » | obtenu: colonne coupée d'office (« estaba silenciado en Mezcla », jamais coupée par moi), case « suena », le banc dit « toutes ses parties sont coupées »
    symptome: texte-faux | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: t5-Cb-1
  - geste: 32 | ecran.vu: 33 | cible: bouton « Batería · A · suena » | geste.type: touche
    attendu: une case vide reste « vacío » | obtenu: toutes les cases vides de la ligne A passent à « suena » (Batería, Acid 303, Sinte, puis Voz 1)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 69 | banc?: non | promesse: t5-Cb-1
  - geste: 30 | ecran.vu: 32 | cible: texte « “Voz” está en la casilla Remix del bloque B. » | geste.type: touche
    attendu: le 2e envoi est dans la case B comme annoncé | obtenu: case « Ma chanson préférée · B · vacío », la case A affiche « Voz · 2 »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 47 | banc?: non | promesse: -
  - geste: 43 | ecran.vu: 44 | cible: texte « “Voz 3” está en el pad live 3. » | geste.type: touche
    attendu: renvoyer le même bout prévient (« ya está en el pad 1 ») ou réutilise le pad | obtenu: 3e copie identique « Voz 3 » sur un nouveau pad, sans prévenir (pads « Voz 1/2/3 · listo » écran 48)
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: t5-Cb-3
  - geste: 35 | ecran.vu: 36 | cible: bouton « Un trozo » (nom « Cortes: Un trozo ») | geste.type: touche
    attendu: en revenant sur « Cortar la canción », le mode choisi (Frases) est gardé | obtenu: retour au mode « Un trozo », découpe en phrases à refaire
    symptome: perdu | gravite: contourne-en-2-gestes | revu: oui: ecran 41 | banc?: non | promesse: t5-Cb-4
  - geste: 28 | ecran.vu: 28 | cible: texte « Marca trozos para enviarlos. » | geste.type: touche
    attendu: le numéro de bout « 1 » au-dessus de l'onde est un bouton nommé (lecteur d'écran) | obtenu: invisible dans le texte de l'écran, le toucher est lu « rien » alors qu'il choisit le bout
    symptome: sans-nom | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 28 | cible: bouton « Frases » (nom « Cortes: Frases ») | geste.type: touche
    attendu: les boutons Frases / En el tiempo entiers et lisibles | obtenu: en mode Frases ils sont à moitié cachés sous la carte « Marca trozos para enviarlos. »
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 31 | ecran.vu: 32 | cible: texte « Ma chanson préférée » | geste.type: touche
    attendu: le nom de la colonne lisible | obtenu: « Ma chanson pr... » coupé en tête de colonne Live
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 69 | banc?: non | promesse: -
  - geste: 67 | ecran.vu: 69 | cible: interrupteur « Sonido de Ma chanson préférée » [non coché, état « desactivado »] | geste.type: touche
    attendu: colonne coupée = la chanson ne joue pas | obtenu: interrupteur coupé, mais le banc dit « avec la chanson Ma chanson préférée entière » en lançant la case
    symptome: texte-faux | gravite: finition | revu: non | banc?: son | promesse: t5-Cb-1
mots-faux:
