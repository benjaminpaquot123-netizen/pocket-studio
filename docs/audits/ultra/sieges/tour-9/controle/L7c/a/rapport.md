code testé : 3b169e0809d63c4877b015abb2f906095ac3d72a
joué par : sous-agent neuf

siege: a | profil: curieux | lang: ar | device: foldable | font: 200 | mode: simple
mission: tuto-facile (speed garage, chapitres 1 à 6) | buts: ch1-6 finis
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: aucune promesse nommée dans la consigne
chapitres:
  - ch1 tempo et tonalité : compris du premier coup (gestes 1-15)
  - ch2 batterie : hésitation (gestes 16-80 ; 2e rangée et accès à la mesure 2 cachés)
  - ch3 basse qui parle : hésitation (gestes 81-110 ; compteur qui remonte, notes sur d'autres rangées)
  - ch4 orgue : compris du premier coup (gestes 112-124)
  - ch5 cris du MC : compris du premier coup (gestes 126-137)
  - ch6 premier groove : compris du premier coup (gestes 138-144)
piano-roll: pointillés visibles oui (mais une seule rangée à l'écran, à moitié sous le panneau du tuto) | compteur « encore N » baisse : non, pas toujours (il remonte : écran 89 1→4, 94 1→2, 102 1→3 ; il affiche « encore 2 » à côté de « réussi », écrans 91, 99, 130) ; dans la grille de batterie, il baisse bien
hesitations:
  - ecran: 27 | cherchais: comment passer à la mesure 2 de la batterie | regarde: rien en bas ; le lien « المازورة 2 · 4 خانات → » était caché sous l'en-tête, trouvé en remontant
  - ecran: 89 | cherchais: la dernière note pointillée de la basse | regarde: le compteur passe de 1 à 4 après avoir touché une note pointillée
  - ecran: 103 | cherchais: la note pointillée signalée par « المس هنا » | regarde: le compteur alterne 1 ↔ 2 à chaque appui
abandon: jamais
incidents-banc:
defauts:
  - geste: #89 | ecran.vu: 89 | cible: « بقيت نوتة منقّطة للمس (1) » | geste.type: touche
    attendu: toucher la note pointillée fait passer le compteur à 0 | obtenu: il remonte à 4 (puis 1→2 en #94, 1→3 en #102) ; « نجحت » s'affiche avec « بقيت نوتتان » (#91, #99, #130)
    symptome: incompris | gravite: contourne-en-6-gestes | revu: oui: ecran 102 | banc?: non | promesse: -
  - geste: #25 | ecran.vu: 24 | cible: rangée 2 de la grille de batterie | geste.type: glisse
    attendu: les cases pointillées sont visibles | obtenu: la 2e rangée est cachée sous le panneau du tuto, il faut faire défiler la grille
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 41 | banc?: non | promesse: -
  - geste: #28 | ecran.vu: 27 | cible: « المازورة 2 · 4 خانات → » | geste.type: glisse
    attendu: un bouton visible pour aller à la mesure 2 une fois la mesure 1 finie | obtenu: le lien a glissé sous l'en-tête ; rien à l'écran ne dit quoi faire
    symptome: sans-aide | gravite: contourne-en-3-gestes | revu: oui: ecran 42 | banc?: non | promesse: -
  - geste: #85 | ecran.vu: 85 | cible: grille du piano roll | geste.type: touche
    attendu: voir quelques rangées de notes | obtenu: une seule rangée d'environ 60 px entre la carte compteur et le panneau ; « لا♭ » et « صول♭ » coupés, « مازورة » coupé en « ازورة » ; les notes pointillées sont à moitié sous le panneau (écran 118)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 118 | banc?: non | promesse: -
  - geste: #95 | ecran.vu: 95 | cible: « ↑ أعلى » / « ↓ » | geste.type: touche
    attendu: la flèche montre où est la note | obtenu: elle dit « en haut » ou « en bas » alors que la note est sur la rangée visible (écrans 95, 114, 121)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 121 | banc?: non | promesse: -
  - geste: #18 | ecran.vu: 18 | cible: « بقيت هنا N خانات منقّطة » | geste.type: touche
    attendu: lire le compteur | obtenu: coupé en haut, sous l'ombre de l'en-tête
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: -
  - geste: #18 | ecran.vu: 18 | cible: pads « هاي هات مفتوح », « طبل كبير للاستوديو » | geste.type: touche
    attendu: lire le nom des pads | obtenu: les badges « FX », « R 30 », « L 30 », « R 45 » recouvrent les noms ; la 3e rangée de pads est coupée
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #17 | ecran.vu: 17 | cible: « تنزيل الطقم Speed garage (3 MB) » | geste.type: touche
    attendu: le texte dit que le kit est déjà chargé, donc pas de bouton de téléchargement | obtenu: le bouton revient à presque chaque étape (écrans 112, 126, 138, 143)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 143 | banc?: non | promesse: -
  - geste: #47 | ecran.vu: 47 | cible: icône « تراجع » | geste.type: touche
    attendu: une flèche d'annulation ↶ | obtenu: dessinée ↷ comme « refaire » (miroir RTL)
    symptome: incompris | gravite: finition | revu: oui: ecran 101 | banc?: non | promesse: -
mots-faux:
  - ecran: 11 | vu: Dorian, Phrygian, Blues | attendu: noms traduits en arabe
  - ecran: 23 | vu: الانتقال إلى 2 مازورات | attendu: الانتقال إلى مازورتين
  - ecran: 85 | vu: باص باص متكلم (« باص » = bus, doublé) | attendu: البيس المتكلم
  - ecran: 101 | vu: (على D) / ecran 13 : G | attendu: ري / صول, comme sur la grille
  - ecran: 9 | vu: L 30, R 45 | attendu: un mot compréhensible (يسار 30) ou rien
defauts-avant:
budget: 144 gestes sur 150 ; chapitres 1 à 6 finis
fin-tranche: oui (ROUTE ligne 735)
