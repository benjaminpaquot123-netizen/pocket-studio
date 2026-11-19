code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: debutant-motive | lang: es | device: foldable | font: 130 | mode: simple
mission: tuto-facile (chapitres 1 à 5 du tuto lofi) | buts: - (pas d'id dans le brief)
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: aucune promesse donnée par le brief
hesitations:
  - ecran: 11 | cherchais: quels sont « los dos botones marcados » | regarde: le contour pointillé n'est clair que sur l'image, Mayor + Fa trouvés ensuite
  - ecran: 18 | cherchais: lequel des deux boutons fait avancer | regarde: « Paso siguiente » et « Siguiente → » visibles en même temps
  - ecran: 62 | cherchais: le pad « Maracas » demandé | regarde: la grille affiche « Compás 1/2 · Aro del tambor », le pad Maracas est sous le panneau du tuto
abandon: jamais
chapitres:
  - chapitre 1 (Tempo et tonalité) : compris du premier coup
  - chapitre 2 : hésitation (bouton Copiar à toucher deux fois)
  - chapitre 3 (rythme, Swing) : hésitation (Maracas demandé, grille Aro del tambor)
  - chapitre 4 (El bajo) : hésitation (piste de basse déjà là, compteur 2/3 notes, Copiar deux fois)
  - chapitre 5 (La melodía) : compris du premier coup
incidents-banc:
  - geste: 1 | attente: 0 | vu: le texte de l'écran 1 et son image ne montraient pas le même bouton de téléchargement
defauts:
  - geste: 23 | ecran.vu: 23 | cible: bouton ⊕ «   Copiar en los compases siguientes » @ 517,850 | geste.type: touche
    attendu: la copie se fait et le tuto passe à « ¡Bravo! » | obtenu: rien ne change (même après 2 s d'attente), il faut toucher une 2e fois ; refait au chapitre 4 (#84 sans effet, #85 marche) ; au chapitre 5 (#91) un seul toucher a suffi
    symptome: rien
    gravite: contourne-en-1-gestes | revu: oui: ecran 85 | banc?: temps | promesse: -
  - geste: 78 | ecran.vu: 78 | cible: bouton ⊕ «   Crear la pista Bajo » | geste.type: touche
    attendu: le tuto tient compte de la piste « Acid 303 Línea de bajo » déjà présente | obtenu: une 2e piste de basse est créée, on se demande laquelle est « le bajo »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 79 | ecran.vu: 80 | cible: bouton « Bajo · compás 1/8 Quedan 3 notas punteadas por tocar » @ 517,451 | geste.type: touche
    attendu: le bandeau et le tuto donnent le même nombre de notes | obtenu: le tuto dit « las 3 notas punteadas », le bandeau disait « Quedan 2 » (écran 79) puis « Quedan 3 » après un toucher sur le bandeau
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 62 | cible: « Te toca: en el pad “Maracas”, toca las casillas punteadas. » | geste.type: touche
    attendu: la grille des Maracas s'ouvre ou le pad est visible | obtenu: la grille affiche « Compás 1/2 · Aro del tambor », le pad Maracas est caché par le panneau du tuto ; l'étape est validée quand même
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 35 | cible: « Lo-fi hip-hop · Caja » | geste.type: touche
    attendu: le nom du pad se lit (Caja, Hi-hat) | obtenu: « Lo-fi hip-hop ·… », le vrai nom du pad est coupé, alors que le tuto (écran 36) parle du pad « Lo-fi hip-hop · Caja »
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 35 | ecran.vu: 35 | cible: « Los pads suenan, las casillas gua… » | geste.type: touche
    attendu: la phrase d'aide se lit en entier | obtenu: phrase coupée ; « Compás 2 · 11 casillas → » coupé aussi
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 72 | ecran.vu: 72 | cible: réglage Swing (texte du tuto « bajo la cuadrícula ») | geste.type: touche
    attendu: le Swing est sous la grille comme dit le tuto | obtenu: sur le pliable, le Swing est à gauche, au-dessus ; il faut le chercher
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
defauts-avant:
  - - | pas atteint | ecran: - | aucun défaut visé donné par le brief

fin-tranche: oui (ROUTE ligne 505)
