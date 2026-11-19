code testé : 2b18313c4
joué par : sous-agent neuf

siege: a | profil: pro | lang: id | device: foldable | font: 130 | mode: pro
mission: tuto-pro (retro_techno, chapitres 1 à 6) | buts: - (aucun id donné par le brief)
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: le brief ne donnait aucune promesse
hesitations:
  - ecran: 44 | cherchais: la mesure 2 où poser le roll de caisse claire | regarde: les cases pointillées étaient en mesure 4, la mesure 2 n'en avait aucune
  - ecran: 59 | cherchais: la ligne « Zap » demandée par la consigne | regarde: l'en-tête de ligne disait « Birama 1/4 · Rim »
  - ecran: 102 | cherchais: quelle valeur valide « Glide antar not minimal 15 » (le curseur est en ms) | regarde: détune 36 %, glide 96 puis 235 ms, l'étape ne se valide jamais
abandon: ecran 108 | parce que l'étape 3/3 du chapitre 4 (détune + glide) ne se valide pas malgré des valeurs au-dessus du minimum ; sautée par « Berikutnya » deux fois
incidents-banc:
  - geste: - | attente: 0 | vu: outils hors human-act.py : cat/grep/sed et lecture d'images, seulement sur les fichiers du dossier du siège (+ les deux gabarits lus à la demande du chef)
  - geste: 1 | attente: 0 | vu: le texte de l'écran 1 montrait la bannière « Unduh kit Techno retro (6 MB) », absente de l'image
defauts:
  - geste: 102 | ecran.vu: 102 | cible: Giliranmu: di “Atur suara trek” Melodi, naikkan “Ketebalan (detune)” minimal 15, lalu di “Pengaturan lanjutan”, “Glide antar not” minimal 15 | geste.type: glisse
    attendu: détune 36 % puis 78 % et glide 96 puis 235 ms valident l'étape | obtenu: rien ne se valide ; « Tunjukkan » met le glide à 272 ms puis le remet à 96 ms ; seul « Berikutnya » ×2 permet de sauter
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 136 | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 44 | cible: roll snare di akhir birama ke-2 | geste.type: touche
    attendu: les cases guides du roll à la fin de la mesure 2 | obtenu: les cases pointillées sont en mesure 4 (« Birama 4/4 ») ; la mesure 2 affiche « Birama 4 · 4 kotak → »
    symptome: texte-faux | gravite: contourne-en-2-gestes | revu: oui: ecran 45 | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 21 | cible: Langkah berikutnya | geste.type: touche
    attendu: la grille de pas reste visible pour l'étape suivante | obtenu: le panneau du tuto se rouvre à chaque étape et cache la grille ; il faut le replier à chaque fois
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 22 | banc?: non | promesse: -
  - geste: 75 | ecran.vu: 75 | cible: Pola terpasang! Sisanya disalin: | geste.type: touche
    attendu: un « Salin ke birama berikutnya » copie la suite et valide | obtenu: phrase finie par « : » sans rien après, étape non validée ; deuxième Salin nécessaire (pareil au ch. 4, écrans 92-93)
    symptome: texte-coupe | gravite: contourne-en-1-gestes | revu: oui: ecran 92 | banc?: non | promesse: -
  - geste: 59 | ecran.vu: 59 | cible: Giliranmu: zap tiap 2 birama. | geste.type: touche
    attendu: la case guide est sur une ligne « Zap » | obtenu: l'en-tête de ligne dit « Birama 1/4 · Rim » ; on ne sait pas si c'est le bon son
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 42 | ecran.vu: 42 | cible: Giliranmu: hi-hat tertutup dengan dua kekuatan. | geste.type: touche
    attendu: l'étape vérifie deux forces (et au #50 des rolls 1-2-3-4) | obtenu: validée avec tous les coups à 100 % ; idem étape roll validée sans aucun roll (#50)
    symptome: rien | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 86 | ecran.vu: 86 | cible: Masih 3 not bertitik, lebih atas ↑ | geste.type: touche
    attendu: le compteur de notes restantes descend | obtenu: il saute 1 → 3 « lebih atas » → 1 → 2 « lebih jauh » (écrans 85 à 88)
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 96 | ecran.vu: 98 | cible: Melodi Hoover | geste.type: glisse
    attendu: la piste reste « Hoover » après avoir monté le détune comme demandé | obtenu: l'onglet devient « Melodi Synth »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 135 | banc?: non | promesse: -
  - geste: 3 | ecran.vu: 3 | cible: Contoh dilewati tanpa didengarkan. | geste.type: touche
    attendu: l'exemple compte comme écouté (j'avais touché « Dengar contohnya » au #2) | obtenu: message « exemple sauté sans l'écouter »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 15 | cible: Sudah siap: kick, clap, hi-hat, dan snare 909 (kit dari “Buatkan aku lagu”) | geste.type: touche
    attendu: le kit 909 est déjà là | obtenu: les pads étaient « Kick studio / Kit: Studio » jusqu'au téléchargement de 6 MB
    symptome: texte-faux | gravite: finition | revu: oui: ecran 18 | banc?: non | promesse: -
mots-faux:
  - ecran: 62 | vu: lalu 909 contohnya | attendu: lalu dengarkan contohnya
  - ecran: 82 | vu: Ghost bass (sélectionné sur la piste Melodi) | attendu: une puce propre à la mélodie
defauts-avant:
  - - | pas atteint | ecran: - | aucun défaut ciblé listé dans le brief

chapitres:
  - ch1 Tempo dan kunci nada : compris du premier coup
  - ch2 Drum 909 : hésitation (roll en mesure 4 au lieu de 2, ligne Rim au lieu de Zap)
  - ch3 Bass : hésitation (second « Salin » nécessaire)
  - ch4 Riff hoover : bloqué à l'étape 3/3 (détune + glide jamais validés), sautée
  - ch5 Bagian pertamamu: Groove : compris du premier coup
  - ch6 Memahami bagian : compris du premier coup
gestes: 136 sur 174

fin-tranche: non (dernier step=lead_knobs, done=21, chapters=1,2,3,5,6 ; ROUTE ligne 700 ; chapitre 4 manquant)
