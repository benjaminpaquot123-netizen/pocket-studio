code testé : 2b18313c4
joué par : sous-agent neuf

siege: a | profil: curieux | lang: ar | device: foldable | font: 200 | mode: simple
mission: tuto-facile (speed_garage, chapitres 1 à 6) | buts: ch1-6 finis
chapitres:
  - ch1 (tempo + tonalité) : compris du premier coup (écran 14)
  - ch2 (batterie) : hésitation (carte qui cache la grille, « ملء » muet ; fini écran 78)
  - ch3 (البيس الذي يتكلم) : bloqué (étape 2 réussie après détour de 10 gestes, étapes 3 et 4 sautées par « التالي »)
  - ch4 (الأورغن) : bloqué (mesures 3+ introuvables, chapitre sauté par « التالي »)
  - ch5 (صرخات الـMC) : compris du premier coup (fini écran 134)
  - ch6 (جزؤك الأول: غروف) : compris du premier coup (fini écran 142)
hesitations:
  - ecran: 24 | cherchais: la dernière case pointillée | regarde: la carte du tuto couvrait la 2e rangée de la grille, j'ai dû faire défiler
  - ecran: 90 | cherchais: la note pointillée qui « reste » | regarde: j'avais touché les 4 notes visibles, le compteur disait toujours « بقيت نوتة منقّطة للمس (1) »
  - ecran: 117 | cherchais: les accords des mesures suivantes | regarde: « انسخ إلى المازورات التالية » n'a rien copié de visible
abandon: jamais (étapes sautées aux écrans 109-110 et 124-125, budget presque fini)
incidents-banc:
defauts:
  - geste: 106 | ecran.vu: 107 | cible: دورك: اضغط النوتات الـ4 في المازورة 2. | geste.type: touche
    attendu: voir les 4 notes pointillées de la mesure 2 | obtenu: une seule pointillée visible, puis plus aucune nulle part même en faisant défiler ; impossible de finir l'étape
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 102 et 107 | banc?: non | promesse: -
  - geste: 117 | ecran.vu: 122 | cible: دورك: اضغط النوتة السفلى المنقّطة لكل كورد في كل مازورة: يوضع الكورد كله. ضربتان في كل مازورة. | geste.type: glisse
    attendu: trouver les accords pointillés des mesures 3 et suivantes | obtenu: mesures 1 et 2 faites, ensuite aucune note pointillée nulle part (défilement en temps et en hauteur), étape jamais validée
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 120 et 122 | banc?: non | promesse: -
  - geste: 87 | ecran.vu: 90 | cible: باص · المازورة 1/8 بقيت نوتة منقّطة للمس (1) | geste.type: touche
    attendu: l'étape réussit après avoir touché les 4 notes pointillées | obtenu: 4 notes orange, compteur bloqué à « 1 », la note manquante n'est apparue qu'après défilement (écran 97)
    symptome: cache | gravite: contourne-en-10-gestes | revu: oui: ecran 98 | banc?: non | promesse: -
  - geste: 91 | ecran.vu: 93 | cible: أرني | geste.type: touche
    attendu: voir où toucher | obtenu: « انظر، سأفعلها… » mais le panneau couvre le piano, je ne vois rien bouger
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 115 | ecran.vu: 116 | cible: انسخ إلى المازورات التالية | geste.type: touche
    attendu: les accords copiés dans les mesures suivantes | obtenu: rien de copié (mesure 2 vide, écran 117), l'étape reste à faire
    symptome: rien | gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 47 | cible: ملء | geste.type: touche
    attendu: un remplissage de batterie | obtenu: rien de visible ni d'audible
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 24 | ecran.vu: 24 | cible: بقيت هنا خانة منقّطة واحدة (1) | geste.type: touche
    attendu: voir toute la grille demandée | obtenu: la carte du tuto cache la 2e rangée de 16 cases (police 200 %, pliable)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 25 | banc?: non | promesse: -
  - geste: 83 | ecran.vu: 84 | cible: مازورة | geste.type: touche
    attendu: lire le numéro de mesure | obtenu: le mot « مازورة » chevauche le chiffre « 1 » dans l'en-tête du piano
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 109 | ecran.vu: 110 | cible: دورك: أخيرًا اكتب المازورة 4 (على D). | geste.type: touche
    attendu: la note nommée comme sur le clavier (ري) | obtenu: « D » en lettre anglaise alors que les touches disent « ري » (même chose « G » contre « صول » au ch1, écran 10)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 40 | cible: ضربات خفيفة | geste.type: touche
    attendu: comprendre « الكيك » et « ضربات خفيفة » | obtenu: « الكيك » utilisé avant d'être expliqué (le pad s'appelle « طبل كبير »), cases posées à 100 % alors qu'on demande des coups légers
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 11 | vu: Dorian / Phrygian / Blues | attendu: traduits en arabe
  - ecran: 23 | vu: 2 مازورات | attendu: مازورتين
  - ecran: 83 | vu: البيس / باص (deux orthographes) | attendu: une seule
  - ecran: 24 | vu: Speed ga… | attendu: titre entier
  - ecran: 84 | vu: %50 ضبط / #فا | attendu: ضبط 50% / فا# (ordre bidi)
defauts-avant:
  - (aucune liste de défauts visés dans le brief) | pas atteint | ecran: - | rien à contrôler nommément
gestes: 142 / 150

fin-tranche: non (dernier step=groove_name, done=15, chapters=1,2,5,6)
BENCH-VERDICT (journal ligne 730) : #142 BENCH-VERDICT ok where=seat.fin
