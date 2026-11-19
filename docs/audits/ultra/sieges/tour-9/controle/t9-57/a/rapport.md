code testé : 7455749a5
joué par : sous-agent neuf
siege: a | profil: débutant motivé | lang: tr | device: phone360 | font: 100 | mode: simple
mission: tuto-facile (reggaeton, chapitres 8 à 15, Kolay) | buts: -
promesse: id=- | texte: finir le tuto reggaeton jusqu'à l'export | ecran: 87 | tenue: oui | pourquoi: « Bravo, parçan bitti! » affiché, Aşama 15/15, fichier m4a exporté et partagé
chapitres:
  - 8 Nakarat : compris du premier coup
  - 9 Bölümleri anlamak : compris du premier coup
  - 10 Bölümlerini kur : compris du premier coup
  - 11 Bölümlerini sırala : hésitation (le texte dit « Ekle », le bouton dit « Sona ekle: », caché sous la carte)
  - 12 Geçişler : hésitation (3 cases avant le dernier temps ne validaient pas ; il a fallu aussi la case « 4 », 7 gestes de plus)
  - 13 Efektler : compris du premier coup
  - 14 Mix: seviyeler : compris du premier coup
  - 15 Parçanı dışa aktar : compris du premier coup
  - fini en 87 gestes sur 167
hesitations:
  - ecran: 39 | cherchais: le bouton « Ekle » demandé | regarde: « Sona ekle: », caché sous la carte jusqu'à « Ben yaparım »
  - ecran: 59 | cherchais: pourquoi mes 3 cases ne validaient pas | regarde: carte rouverte, Göster regardé, case « 4 » ajoutée
  - ecran: 55 | cherchais: si j'étais à la bonne étape | regarde: « Adım 1/3 » inchangé alors que la consigne avait changé
abandon: jamais
incidents-banc:
  - geste: - | attente: - | vu: aucun écran vide ; outils hors human-act.py : cat, ls, tail, grep dans le dossier du siège seulement
defauts:
  - geste: 59 | ecran.vu: 59 | cible: « Sıra sende: son ölçüde, timbal pad'i: son vuruştan hemen önce art arda 3 kutuya dokun. » | geste.type: touche
    attendu: 3 cases juste avant le dernier temps valident l'étape | obtenu: rien ne valide ; il faut aussi la case « 4 » (vu dans Göster)
    symptome: incompris, contourne-en-5-gestes |
    gravite: grave | revu: non | banc?: non | promesse: -
  - geste: 56 | ecran.vu: 56 | cible: grille « Ölçü 4/4 · Davul kenarı » | geste.type: regarde
    attendu: la grille porte le nom « timbal » de la consigne | obtenu: elle s'appelle « Davul kenarı », je ne savais pas si c'était la bonne
    symptome: incompris |
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: case barrée de l'étape 2 | geste.type: touche
    attendu: la case s'efface et je reste sur la même grille | obtenu: la grille saute sur le Hi-hat avec une autre case barrée
    symptome: surprise |
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 54 | ecran.vu: 55 | cible: « Aşama 12 · Adım 1/3 » | geste.type: touche
    attendu: le compteur passe à l'étape suivante quand la consigne change | obtenu: reste « Adım 1/3 »
    symptome: perdu |
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 39 | cible: consigne « … Ekle … » et boutons « Sona ekle: » | geste.type: regarde
    attendu: le mot de la consigne est celui du bouton, bouton visible | obtenu: « Sona ekle: », boutons cachés sous la carte ; la liste ne montre que le dernier bloc
    symptome: cache, incompris |
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 25 | cible: liste des sections après « Giriş » créé | geste.type: touche
    attendu: voir la section que je viens de créer | obtenu: la carte du tuto couvre la liste
    symptome: cache |
    gravite: finition | revu: oui | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 21 | cible: légende « Giriş / Kıta / Ön nakarat / Nakarat » et pastilles du graphique | geste.type: regarde image
    attendu: noms et nombres de mesures lisibles | obtenu: légende coupée (haut des lettres seulement), pastilles coupées, la pastille du pont dit juste « 8 »
    symptome: coupé |
    gravite: finition | revu: oui | banc?: non | promesse: -
  - geste: 83 | ecran.vu: 83 | cible: onglet Paylaş | geste.type: regarde
    attendu: rien d'étrange en haut à gauche | obtenu: une « image (rond coché) » posée à 0,0
    symptome: déplacé |
    gravite: finition | revu: non | banc?: oui | promesse: -
mots-faux:
  - ecran: 6 | vu: « Sıra sende: “Bölüme “Nakarat” adını ver” düğmesine dokun. » | attendu: guillemets non doublés
  - ecran: 24 | vu: « Sıra sende: ““Giriş” bölümünü oluştur” düğmesine dokun. » | attendu: « “Giriş bölümünü oluştur” düğmesine dokun »
  - ecran: 21 | vu: « Post-nakarat » | attendu: « Nakarat sonrası »
  - ecran: 78 | vu: « Sürgünün altında “Normal”, “−2” olur. » | attendu: « Sürgünün altındaki “Normal”, “−2” olur. »
  - ecran: 86 | vu: « OK » | attendu: « Tamam »
fin-tranche: oui (ROUTE ligne 411)
