code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

```
siege: a | profil: debutant-motive | lang: es | device: tablet8 | font: 100 | mode: simple
mission: tuto-facile (deep_house, capítulos 1 a 6) | buts: -
promesse: id=- | texte: Tu primera parte: el Groove Capítulo 6/13 · Hecho · Fácil | ecran: 101 | tenue: oui | pourquoi: les 6 chapitres finis en 100 gestes, sans abandon
hesitations:
  - ecran: 9 | cherchais: le bouton pour passer au chapitre suivant | regarde: le panneau du tuto s'est replié après « Volver al tutorial », un toucher de plus pour le rouvrir (pareil écran 93)
  - ecran: 57 | cherchais: combien de notes il me restait à poser | regarde: le tuto dit 5 notes, le bandeau dit 4, puis 6, puis 1, puis 5 « más adelante » alors que c'est « Conseguido »
  - ecran: 67 | cherchais: pourquoi rien ne bouge après « Copiar en los compases siguientes » | regarde: il faut ensuite toucher le petit « Copiar » du bandeau en haut
abandon: jamais
incidents-banc:
defauts:
  - geste: 2 | ecran.vu: 3 | cible: icône « Siguiente  » → | geste.type: touche
    attendu: le tuto voit que j'ai écouté l'exemple | obtenu: « Ejemplo saltado sin escucharlo. » alors que je l'avais lancé et écouté en partie
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 6 | ecran.vu: 7 | cible: Tonalidad del tema: ninguna | geste.type: touche
    attendu: aucune gamme choisie puisque la tonalité est « ninguna » | obtenu: « Dórico » est déjà sélectionné dans la même feuille
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 12 | cible: Te toca: en el pad “Bombo de estudio”, toca las 4 casillas punteadas: un golpe en cada tiempo. | geste.type: touche
    attendu: la consigne entière lisible | obtenu: la consigne est coupée sur une ligne avec « … », il faut ouvrir « Leer »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: golpe, fuerza 100 % | geste.type: touche
    attendu: des coups forts entre les temps et doux autour, comme dit la consigne du charleston (écran 22) | obtenu: tous mes coups sont à 100 % et le tuto les accepte : rien n'est doux
    symptome: texte-faux | gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 40 | cible: golpe, fuerza 100 % | geste.type: touche
    attendu: des « golpes ligeros » pour le rim, comme dit la consigne (écran 35) | obtenu: chaque coup posé est à 100 %, rien de léger
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 42 | ecran.vu: 41 | cible: toca las 3 casillas punteadas, entre los kicks | geste.type: touche
    attendu: 3 cases entre les coups de grosse caisse | obtenu: une des cases pointées (« 3 ») tombe sur le temps 3, là où il y a déjà un kick
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 58 | ecran.vu: 59 | cible: Quedan 5 notas punteadas, más adelante → | geste.type: touche
    attendu: un seul compte des notes restantes, qui baisse | obtenu: tuto « 5 notas », bandeau « 4 », puis « 6 más arriba », puis « 1 », puis « 5 más adelante » en même temps que « Conseguido »
    symptome: incompris | gravite: finition | revu: oui: ecran 88 | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: bouton ⊕ «   Copiar en los compases siguientes » | geste.type: touche
    attendu: le motif est copié et l'étape est finie | obtenu: rien de visible ; il faut trouver et toucher le « Copiar » du bandeau en haut de la grille
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 85 | banc?: non | promesse: -
  - geste: 73 | ecran.vu: 74 | cible: Te toca: toca la nota de abajo punteada de cada acorde de los compases 1 y 2: se coloca todo el acorde. Hay 4 golpes por compás. | geste.type: touche
    attendu: le cadre jaune « touche ici » sur la note du bas | obtenu: le cadre jaune est sur la note du haut (Sol) ; la note du bas (La♭) est collée au bord du panneau du tuto
    symptome: incompris | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: -
  - geste: 87 | ecran.vu: 88 | cible: Te toca: toca las 3 notas punteadas: una larga al principio y la respuesta en el compás 4. | geste.type: touche
    attendu: un piano roll sans réglage d'une autre piste | obtenu: sur la piste Melodía, « Bajo fantasma » reste sélectionné (et écran 74 « Acordes » et « Bajo fantasma » sont tous deux sélectionnés)
    symptome: incompris | gravite: finition | revu: oui: ecran 95 | banc?: non | promesse: -
mots-faux:
  - ecran: 100 | vu: toca “Nombrar la parte “Groove”” | attendu: toca «Nombrar la parte Groove» (guillemets imbriqués)
chapitres:
  - 1 La velocidad y el color del tema : compris du premier coup
  - 2 La batería : compris du premier coup
  - 3 El bajo : hésitation (compteurs de notes, bouton Copiar)
  - 4 El piano eléctrico : hésitation (cadre sur la mauvaise note, bouton Copiar)
  - 5 La voz : compris du premier coup
  - 6 Tu primera parte: el Groove : compris du premier coup
```

fin-tranche: non (dernier step=groove_name, done=19)
