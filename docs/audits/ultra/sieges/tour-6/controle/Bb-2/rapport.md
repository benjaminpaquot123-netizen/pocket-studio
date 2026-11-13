code testé : b4ac25f2
joué par : sous-agent neuf
siege: b | profil: t6-Bb producteur-allemand-tablette | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t6-Bb-1 | texte: Du bist dran: Gib ein ganz kleines bisschen Swing dazu (7 %) | ecran: 21 | tenue: oui | pourquoi: curseur Swing glissé, « Bravo, Kapitel geschafft! » aussitôt (mais validé à 4 % au lieu de 7 %, voir défaut)
promesse: id=t6-Bb-2 | texte: Tippe bei einem Sound auf ☆, um ihn unter „Favoriten“ wiederzufinden. | ecran: 225 | tenue: oui | pourquoi: ☆ visible à côté de chaque son, touché il devient ★ plein
promesse: id=t6-Bb-3 | texte: Neue Kurve: zeichne sie unten. | ecran: 196 | tenue: oui | pourquoi: en mode « Linie », deux taps et la rampe jaune apparaît tout de suite (un glissé diagonal, lui, fait défiler la page)
promesse: id=t6-Bb-4 | texte: Einstellung zeigen | ecran: 201 | tenue: oui | pourquoi: ouvre « Effekte · Melodie », Filter avec Cutoff et la phrase « Eine Kurve bewegt diese Einstellung »
oeil-neuf: q1 | ecran: 225 | image: 225 | ☆, « Setzen », les onglets Kicks/Snares/Claps, ← Techno, ▶ et ✕ ont l'air touchables et le sont ; la carte du son elle-même (pour écouter) ne se distingue pas comme touchable
oeil-neuf: q6 | ecran: 225 | image: 225 | preuve : l'étoile devient pleine et jaune ; rien ne dit où retrouver « Favoriten » depuis cette liste
oeil-neuf: q9 | ecran: 225 | image: 225 | tous les noms de sons coupés en trois lignes « Cha / rle… / Te… » : impossible de distinguer Charley 2 de Charley 14
chapitre: 1 Tempo und Tonart | ecran: 21 | hésitation | Swing demandé à 7 %, accepté à 4 % : je ne sais pas pourquoi c'est validé ; le kit chargé s'appelle « Kit: Eigenes » avec encore « Studio-Kick »
chapitre: 2 Die Kick und das Grollen | ecran: 34 | hésitation | le texte parle de « 909-Kick », le pad s'appelle « Studio-Kick » ; à l'étape Rumble le nom du pad affiché au-dessus de la grille est hors écran
chapitre: 3 Der Clap | ecran: 40 | compris du premier coup | cases pointillées + appui long → « Sanft », clair
chapitre: 4 Hi-Hats und Percussion | ecran: 64 | hésitation | « leiser unter der Kick » mais validé à 100 % ; « Ride » demandé, le pad s'appelle « Becken »
chapitre: 5 Der Bass | ecran: 91 | hésitation | piste créée « Bass Plaits » au lieu de « Analoger Bass » ; panneau du son de basse montre des puces « Bassdrum/Snare/Hi-Hat »
chapitre: 6 Der Stab (Akkorde) | ecran: 110 | hésitation | « auf den Offbeats (Steps 4 und 11) » : 4 et 11 ne sont pas des contretemps pour un producteur
chapitre: 7 Das Riff | ecran: 130 | bloqué | Cutoff +15 et Resonanz +16 (« mindestens 15 ») non validés ; il a fallu monter Cutoff à +20 ; la piste « Gefilterter Lead » est renommée « Melodie Plaits »
chapitre: 8 Dein erster Teil: der Groove | ecran: 136 | hésitation | le menu de partie « A ▾ » n'offre pas de renommer ; seul le bouton du tuto le fait
chapitre: 9 Die Teile verstehen | ecran: 144 | compris du premier coup | carte des parties et longueurs claire
chapitre: 10 Bau deine Teile | ecran: 158 | compris du premier coup | un bouton par partie, le résumé dit ce qui manque ; le mot « Tock » n'est relié à aucun pad
chapitre: 11 Ordne deine Teile | ecran: 169 | hésitation | Break 12 Takte impossible avec + (8→16) ; après « Empfohlene Längen setzen » tous les − + sont grisés
chapitre: 12 Die Übergänge | ecran: 177 | hésitation | « letzten Schlag vor der letzten Zählzeit » obscur ; annoncé « Schritt 2/3 » puis « Erledigt » sans étape 3
chapitre: 13 Die Effekte | ecran: 198 | hésitation | « Reverb-Send auf dem Kanalzug des Pads » : aucun canal par pad dans Mix, trouvé seulement via « Ich mach's »
chapitre: 14 Der Mix | ecran: 209 | compris du premier coup | deux faders à baisser, validé tout de suite
chapitre: 15 Das Mastering | ecran: 212 | hésitation | « OTT » non expliqué ; « Ziel-Lautheit » rangé sous « Details », caché par la carte du tuto
chapitre: 16 Exportiere deinen Track | ecran: 220 | compris du premier coup | Teilen → M4A → Drive, « Bravo, dein Track ist fertig! »
bravo: oui | ecran: 220
hesitations:
  - ecran: 179 | cherchais: le « Kanalzug des Pads Techno · Clap » dans Mix | regarde: Mix n'a que des pistes (Drums, Acid 303…), la fiche Drums n'a pas de Reverb-Send
  - ecran: 193 | cherchais: dessiner la courbe en glissant le doigt | regarde: le glissé diagonal fait défiler la page, la courbe reste vide ; il faut passer en « Linie »
  - ecran: 186 | cherchais: la preuve d'être dans le Drop après « Zum Teil Drop gehen » | regarde: l'en-tête reste « Intro Takt 1/128 »
abandon: jamais
incidents-banc:
  - geste: 89 | attente: 0 | vu: écran 90 texte vide (seule la barre Feedback) alors que l'image montre le panneau de son
  - geste: 126 | attente: 0 | vu: écran 127 texte vide, même après `regarde` ; l'image montre Resonanz/Cutoff
  - geste: 210 | attente: 0 | vu: écrans 211 et 212 texte vide ; l'image montre le Master-Ausgang
  - geste: - | attente: 0 | vu: outils hors human-act : Read/Write/Bash (ls, grep) limités à mon dossier de siège, pour images, journal et rapport
defauts:
  - geste: 128 | ecran.vu: 129 | cible: Du bist dran: Dreh unter „Sound der Spur einstellen“ der Melodie „Cutoff“ und „Resonanz“ jeweils um mindestens 15 auf | geste.type: glisse
    attendu: Cutoff 60→75 et Resonanz 60→76 (≥ +15) valident l'étape | obtenu: rien ne se passe ; il faut monter Cutoff à 80 (#129)
    symptome: incompris | gravite: contourne-en-1-gestes | revu: oui: ecran 130 | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: Swing 4 % | geste.type: glisse
    attendu: étape validée à environ 7 % comme demandé | obtenu: validée à 4 %, sans dire qu'une marge est admise
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t6-Bb-1
  - geste: 223 | ecran.vu: 224 | cible: bouton (points en rond) « Charley 2 Techno » | geste.type: touche
    attendu: noms de sons lisibles en 2 colonnes à 130 % | obtenu: « Cha / rle… / Te… » sur toutes les cartes, sons indiscernables
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 225 | banc?: non | promesse: t6-Bb-2
  - geste: 178 | ecran.vu: 179 | cible: Du bist dran: Dreh in Mix den „Reverb-Send“ auf dem Kanalzug des Pads „Techno · Clap“ auf. | geste.type: touche
    attendu: un canal du pad Clap dans Mix | obtenu: Mix ne montre que des pistes ; le réglage n'existe que dans « Effekte · Pad 5 », ouvert par « Ich mach's »
    symptome: sans-aide | gravite: contourne-en-4-gestes | revu: oui: ecran 181 | banc?: non | promesse: -
  - geste: 192 | ecran.vu: 193 | cible: bouton (pinceau) « Zeichnen » [sélectionné] | geste.type: glisse
    attendu: le doigt dessine la courbe dans le cadre | obtenu: la page défile, aucune courbe ; contourné par « Linie » et deux taps
    symptome: rien | gravite: contourne-en-3-gestes | revu: oui: ecran 196 | banc?: non | promesse: t6-Bb-3
  - geste: 127 | ecran.vu: 128 | cible: bouton « Melodie Plaits » ⋮ | geste.type: glisse
    attendu: la piste garde son son « Gefilterter Lead » | obtenu: après un réglage elle s'appelle « Melodie Plaits » (idem « Bass Plaits » au lieu de « Analoger Bass »)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 215 | ecran.vu: 216 | cible: texte « Lautstärke: −14,4, wie Streaming-Musik (−14) » | geste.type: touche
    attendu: niveau cohérent avec « Club · −8 LUFS » sélectionné | obtenu: dit « wie Streaming-Musik » juste au-dessus du choix Club (le fichier final dit −8,0 LUFS)
    symptome: texte-faux | gravite: finition | revu: non | banc?: son | promesse: -
  - geste: 176 | ecran.vu: 177 | cible: texte « Die Übergänge Kapitel 12/16 · Erledigt · Pro » | geste.type: touche
    attendu: étape 3/3 affichée après 2/3 | obtenu: chapitre « Erledigt » directement, l'étape 3 n'est jamais montrée
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 185 | ecran.vu: 186 | cible: icône « Intro Takt 1/128 » ▾ | geste.type: touche
    attendu: après « Zum Teil Drop gehen », l'écran montre le Drop | obtenu: Mix inchangé, l'en-tête reste « Intro » ; seul le texte « Teil D » plus loin le confirme
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 58 | vu: Ride | attendu: Becken (nom du pad) ou pad renommé « Ride »
  - ecran: 224 | vu: Charley | attendu: Hi-Hat (« Charley » est français, pas allemand)
  - ecran: 22 | vu: 909-Kick | attendu: Studio-Kick (nom affiché sur le pad)
  - ecran: 145 | vu: „Tock“ | attendu: Rim (nom du pad)

## Contrôle du lot « tutofleches » (ajouté par Testeur 3, hors gabarit testeur)
lot joué : controle/lotBb2/b ; selftest 176 verts, 0 rouge ; bravo oui au geste 219 (écran 220), 16/16 chapitres, 223 gestes (avant : 214)
chapitres : 5 du premier coup (3, 9, 10, 14, 16), 10 hésitation, 1 bloqué contourné (7 : Cutoff/Resonanz +15 refusés) ; avant : 4 / 11 / 1
visés par la réparation :
  - doubles-croches du ch. 4 | corrigé | plus de refus ; ch. 4 hésite sur « leiser » et « Ride »
  - flèches du rouleau contradictoires (ch. 7) | pas vu | non signalé ; ch. 7 bloqué pour une autre raison (#128)
  - flottement après « Copier sur les mesures suivantes » | pas vu | non signalé
  - note coupée au bord droit | pas vu | non signalé
  - « Akkorde » resté allumé | pas vu | non signalé
  - message de réussite du ch. 10 | corrigé | « le résumé dit ce qui manque », ch. 10 du premier coup
  - partie de montée à 1 Takt | pas vu | non signalé
  - « 8 Takte » | autre | ch. 11 : Break de 12 Takte impossible avec + (8→16), puis − + grisés
  - texte de l'étape du tempo | pas vu | non signalé (ch. 1 hésite sur le Swing)
défauts du rapport controle/Bb :
  - Swing validé à 4 % pour 7 % | encore là | #20
  - doubles-croches refusées au ch. 4 | corrigé |
  - « Kit: Eigenes », Studio-Kick au lieu de 909 | encore là | ch. 1-2, mot-faux « 909-Kick »
  - saut d'octave silencieux au ch. 7 | pas vu | non signalé
  - « Bass Plaits » au lieu de « Analoger Bass » | encore là | #127 (et « Melodie Plaits »)
  - Build-up à 1 Takt | pas vu | non signalé
  - Reverb-Send du Clap introuvable dans Mix | encore là | #178, coté grave cette fois (contourne-en-4-gestes)
  - « wie Streaming-Musik (−14) » sous Club | encore là | #215
  - noms de sons coupés « Cha rle… Te… » | encore là | #223
  - « Sound ändern » caché par la barre du tuto | pas vu | non signalé
compte : visés 2/9 corrigés constatés (1 autre, 6 non revus) ; défauts controle/Bb 1/10 corrigés (6 encore là, 3 non revus) ; neufs 2 (#128 Cutoff +15 refusé, #192 « Zeichnen » qui fait défiler) dont 0 bloquant / 0 grave ; 1 grave ancien (#178)
