code testé : 2b18313c4
joué par : sous-agent neuf

siege: a | profil: confirmé (niveau 3) | lang: nl | device: phone | font: 130 | mode: pro
mission: tuto-pro (uk_garage, chapitres 5 à 14) | buts: -
chapitres:
  - ch5 De stukjes stem : hésitation (notes en pointillé dures à voir sans image ; maat 4 demande 3 notes)
  - ch6 Je eerste deel: het Refrein : compris du premier coup
  - ch7 De delen begrijpen : compris du premier coup (bandeau Kit gêne)
  - ch8 Bouw je delen : compris du premier coup
  - ch9 Zet je delen op volgorde : compris du premier coup
  - ch10 De overgangen : hésitation (une touche par coup, ~34 gestes)
  - ch11 De effecten : hésitation (la fiche parle de Snare, le texte de Rim)
  - ch12 De mix : compris du premier coup
  - ch13 De mastering : hésitation légère (Doelvolume caché sous le panneau)
  - ch14 Exporteer je nummer : compris du premier coup (Tutorial klaar, écran 140)
hesitations:
  - ecran: 83 | cherchais: un geste pour vider toute la maat 4 du Pre-refrein | regarde: aucun, j'ai effacé coup par coup
  - ecran: 116 | cherchais: le réglage galm-send du pad Rim | regarde: la fiche dit « Snare: hier instellen », je suis allé dans Mix > Drums sans le trouver
  - ecran: 15 | cherchais: pourquoi maat 4 demande 3 notes alors que le texte dit G et C | regarde: image pour trouver la 3e note
abandon: jamais
incidents-banc:
defauts:
  - geste: 83 | ecran.vu: 83 | cible: texte « Maat 4/4 · Rim » | geste.type: touche
    attendu: vider la dernière maat du Pre-refrein en un ou deux gestes | obtenu: une touche par coup (kick, snare, hi-hats, rim : ~34 gestes), appui-long sur la maat 4 ne fait rien
    symptome: sans-aide
    gravite: contourne-en-30-gestes | revu: oui: ecran 113 | banc?: non | promesse: -
  - geste: 115 | ecran.vu: 116 | cible: texte « Akkoorden ✓ · Snare: hier instellen » | geste.type: touche
    attendu: le texte dit « en dan op de pad ‘Rim’ », la fiche devrait parler de Rim | obtenu: la fiche demande et valide la Snare
    symptome: texte-faux
    gravite: contourne-en-9-gestes | revu: oui: ecran 124 | banc?: non | promesse: -
  - geste: 52 | ecran.vu: 53 | cible: texte « Deel ‘Couplet’ gemaakt (kopie van ‘Refrein’): zonder Clap, Open hi-hat, Rim, Melodie. » | geste.type: touche
    attendu: le texte dit « de kick, de rim, de hi-hats » : la rim reste | obtenu: le Couplet est créé sans la Rim (la Brug perd aussi l'Open hi-hat malgré « de hi-hats blijven »)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 128 | ecran.vu: 129 | cible: texte « Nu jij: zet in ‘Hoofduitgang’ het ‘Doelvolume’ op ‘Club’. » | geste.type: touche
    attendu: voir le choix Doelvolume en arrivant au chapitre | obtenu: le panneau du tuto couvre Doelvolume et ses choix ; il faut replier puis faire défiler
    symptome: cache
    gravite: contourne-en-3-gestes | revu: oui: ecran 132 | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 38 | cible: bouton « Kit UK Garage downloaden (3 MB) » | geste.type: touche
    attendu: voir le graphe Energie de l'étape | obtenu: le bandeau Kit (reproposé à chaque étape) couvre le graphe jusqu'à « Later »
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 46 | cible: texte « Voorbeeld overgeslagen zonder te luisteren. » | geste.type: touche
    attendu: un message neutre quand je passe l'étape | obtenu: « Goed gedaan, het is gelukt! » juste au-dessus de « overgeslagen zonder te luisteren » (contradictoire)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 15 | cible: bouton « Melodie · maat 4/8 Nog 3 gestippelde noten om aan te tikken » | geste.type: touche
    attendu: le texte dit « (G, C) in 2 en 4 » : 2 notes en maat 4 | obtenu: 3 notes en pointillé demandées en maat 4
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 61 | ecran.vu: 62 | cible: texte « Achteraan toevoegen: » | geste.type: touche
    attendu: le libellé « Toevoegen » nommé par le texte du tuto | obtenu: après le premier ajout le libellé devient « Achteraan toevoegen: » ; la frise ne montre que le dernier deel ajouté
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 34 | ecran.vu: 35 | cible: bouton ⊕ «   Deel ‘Refrein’ een naam geven » | geste.type: touche
    attendu: « Nu jij: noem dit deel ‘Refrein’ » : taper le nom moi-même | obtenu: le bouton nomme le deel tout seul, je n'écris rien
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
defauts-avant:
fin-tranche: oui (ROUTE ligne 750)
