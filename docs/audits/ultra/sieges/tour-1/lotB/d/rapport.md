siege: d | profil: testeur QA | lang: fr | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts atteints: 3 sur 3
hesitations:
  - ecran: 30 | cherchais: quelles cases du kick sont en pointillé | regarde: la liste TalkBack (toutes « vide »), puis l'image de la grille
  - ecran: 94 | cherchais: la 4e note en pointillé (Fa à l'octave) | regarde: le piano roll visible Do2 à Ré1, puis glissé vers le haut
  - ecran: 101 | cherchais: les touches « Sol♭ » et « La♭ » citées par le tuto | regarde: le clavier à gauche (seulement « Fa♯ », « Sol♯ »)
surprise: « Montre-moi » du tempo règle 174 BPM à ma place puis dit « Voilà ! À toi maintenant. » avec la valeur déjà posée
abandon: jamais
defauts:
  - ecran: 33 | cible: Pas 3 (case hors pointillé, pad Kick studio) | geste: touche
    attendu: étape « two-step » non validée tant qu'un coup en trop est posé | obtenu: « Bravo, c'est réussi ! » avec Pas 1, 3 et 11
    symptome: rien
    gravite: moyen | bloque: non
  - ecran: 101 | cible: touches « Fa♯ » / « Sol♯ » du piano roll | geste: touche
    attendu: touches nommées « Sol♭ », « La♭ » comme dans le texte du tuto | obtenu: clavier en dièses uniquement, aucun bémol affiché
    symptome: incompris
    gravite: moyen | bloque: non
  - ecran: 94 | cible: « Encore 1 note en pointillé à toucher » | geste: glisse
    attendu: note cible visible ou indication « plus haut ↑ » (comme « plus loin → ») | obtenu: Fa2 puis Do2 hors champ, rien n'indique de défiler
    symptome: perdu
    gravite: moyen | bloque: non
  - ecran: 37 | cible: « Montre-moi » (étape 2/4 kick) | geste: touche
    attendu: entendre un kick long puis le kick du kit, comme annoncé | obtenu: banc « rien ne joue » à chaque relevé, arbre TalkBack vide pendant la démo
    symptome: sans-nom
    gravite: moyen | bloque: non
  - ecran: 30 | cible: Pas 1 / Pas 11 (cases en pointillé) | geste: touche
    attendu: cases en pointillé annoncées comme cibles à TalkBack | obtenu: toutes « vide », seul le compteur « Encore 2 cases » parle
    symptome: sans-nom
    gravite: moyen | bloque: non
  - ecran: 16 | cible: « Montre-moi » (tempo 174 BPM) | geste: touche
    attendu: démonstration puis retour à 120 BPM pour que je le fasse | obtenu: 174 BPM reste posé jusqu'à « Arrêter »
    symptome: incompris
    gravite: mineur | bloque: non
  - ecran: 10 | cible: onglets « Batterie 16 pads », « Acid 303 Ligne de basse », pastille « REC Noter ⋯ » | geste: touche
    attendu: textes entiers à 130 % et bouton « ? » dégagé | obtenu: 2e ligne des onglets coupée, pastille chevauche « ? » (Étiqueter l'écran)
    symptome: coupe
    gravite: mineur | bloque: non
