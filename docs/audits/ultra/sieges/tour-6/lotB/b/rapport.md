siege: b | profil: t6-Bb | lang: de | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro | buts: chap1, chap3, bravo
promesse: id=t6-Bb-1 | texte: Du bist dran: Stell den Swing auf 25 | ecran: 62 | tenue: non | pourquoi: Swing mis à 25 % deux fois (#58, #61), l'étape reste « Schritt 4/5 », la liste dit 4 « 0/5 » (écran 85)
promesse: id=t6-Bb-2 | texte: Tippe bei einem Sound auf ☆, um ihn unter „Favoriten“ wiederzufinden. | ecran: 70 | tenue: oui | pourquoi: ☆ visible à côté de chaque son, devient ★, « Favoriten 1 Sound » apparaît (écran 71)
promesse: id=t6-Bb-3 | texte: Neue Kurve: zeichne sie unten. | ecran: 79 | tenue: oui | pourquoi: la courbe du filtre tracée au doigt s'affiche aussitôt (image 79)
promesse: id=t6-Bb-4 | texte: Einstellung zeigen | ecran: 80 | tenue: oui | pourquoi: ouvre « Effekte · Drums » sur « 4. Filter » sélectionné, avec « Eine Kurve bewegt diese Einstellung während der Wiedergabe. »
oeil-neuf: q1 | ecran: 86 | image: 86 | onglets, pads, « Erzeug mir ein Pattern », chips Länge et « C D E » se voient touchables ; « BASSLINE · GRUNDTON C ⌄ » se replie mais ressemble à un titre, et « A ▾ » en haut (la partie) se confond avec la tonalité A
oeil-neuf: q6 | ecran: 86 | image: 86 | après « Erzeug mir ein Pattern » la preuve est là (cases « C »), mais toutes en C alors que le morceau est en A Dorisch ; la suite reste floue : le tuto est bloqué sur Swing
oeil-neuf: q9 | ecran: 86 | image: 86 | la 2e rangée de pas de la basse est coupée par le panneau du tuto ; aucun mot de bouton coupé sur cet écran
hesitations:
  - ecran: 1 | cherchais: le tutoriel | regarde: rien sur l'écran d'accueil ; « ↓ 4 weitere » ne descend que d'un cran (écran 2), trouvé « Ich lerne » après un glissé (écran 3)
  - ecran: 35 | cherchais: les cases du clap en mesure 1 | regarde: la grille était restée sur « Takt 2/2 » du chapitre de la kick, il a fallu « Takt 1 · 2 Kästchen → » (écran 37)
  - ecran: 73 | cherchais: « Automatisieren » | regarde: bouton caché sous le panneau du tuto, trouvé en faisant défiler la page (écran 75)
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 900 | vu: avant le lanceur du lot, deux « regarde » ont répondu « L'appli ne s'est pas ouverte (15 min) » ; après démarrage de scripts/human-tester.sh, normal
  - geste: 34 | attente: 0 | vu: écran 35 rendu en texte vide (seulement la barre Feedback-Modus) ; l'image montrait la grille
  - geste: 42 | attente: 2 | vu: écran 43 en texte vide (de même au geste 55, écran 56) ; revenu avec « attends 2 »
  - geste: 0 | attente: 0 | vu: outils hors human-act.py : Read sur les images de mon siège, grep/cat sur journal.txt de mon siège (numéros de gestes) ; avant de jouer, rôle machine : selftest.sh et human-tester.sh sur lotB
defauts:
  - geste: 61 | ecran.vu: 66 | cible: curseur [état « 25 % », à 24 %] @ 450,955 | geste.type: touche
    attendu: Swing sur 25 → « Bravo » et étape suivante | obtenu: rien ne se valide, l'étape reste « Kapitel 4/17 · Schritt 4/5 », chapitre 4 « 0/5 »
    symptome: rien
    gravite: contourne-en-2-gestes | revu: oui: ecran 62 | banc?: non | promesse: t6-Bb-1
  - geste: 63 | ecran.vu: 65 | cible: bouton (œil) «  Zeig's mir » @ 667,1126 | geste.type: touche
    attendu: la démonstration règle le Swing sur 25 comme le texte | obtenu: le curseur montre « 33 % », puis revient à 25 %
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Bb-1
  - geste: 40 | ecran.vu: 41 | cible: bouton « 1 Tempo und Tonart 2/3 » @ 450,149 | geste.type: touche
    attendu: chapitre 1 coché, puisque l'appli a dit « Bravo, Kapitel geschafft! » (écran 18) | obtenu: « 2/3 », sans coche
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 85 | banc?: non | promesse: -
  - geste: 82 | ecran.vu: 84 | cible: titre « BASSLINE · GRUNDTON C » [état « Aufgeklappt »] @ 389,730 | geste.type: touche
    attendu: basse en A, la tonalité réglée au chapitre 1 (« A · Dorisch ») | obtenu: « GRUNDTON C », et « Erzeug mir ein Pattern » ne génère que des C
    symptome: incompris
    gravite: finition | revu: oui: ecran 84 | banc?: non | promesse: -
  - geste: 73 | ecran.vu: 74 | cible: bouton « Automatisieren » [coupé] @ 243,1029 | geste.type: touche
    attendu: ouvre les courbes | obtenu: rien, le bouton est caché sous le panneau du tuto (image 74)
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 76 | banc?: non | promesse: t6-Bb-3
  - geste: 68 | ecran.vu: 69 | cible: bouton ○ « Snare » @ 243,395 | geste.type: touche
    attendu: noms de sons lisibles | obtenu: « Snar / e », « Snar / e 2 », « Rim ✓ J… » coupés au milieu du mot (image 69, police 130 %)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t6-Bb-2
  - geste: 78 | ecran.vu: 79 | cible: texte « 18,0 kHz » @ 81,805 | geste.type: glisse
    attendu: échelle de la courbe lisible | obtenu: « 18,0 kl », « 849 H: », « Schla » coupés par la grille (image 79)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t6-Bb-3
  - geste: 31 | ecran.vu: 32 | cible: bouton [état « leer, gepunktet: antippen »] @ 706,564 | geste.type: touche
    attendu: le « leise Schlag auf Step 15 » demandé doit être doux, ou l'appli dit comment l'adoucir | obtenu: coup posé à 100 % et chapitre validé quand même
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 72 | ecran.vu: 73 | cible: texte « Synth » @ 204,379 | geste.type: touche
    attendu: les pistes « Drums, Acid 303, Vocals, Remix » de Sounds | obtenu: le mixeur montre « Drums, Synth, Remix » : Acid 303 renommée, Vocals absente
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 54 | ecran.vu: 55 | cible: texte « Du bist dran: Stell den Swing auf 25: Die geraden Sechzehntel kommen etwas später, zwischen Chicago House (55–60 %) und New York House (70–80 %). » @ 450,703 | geste.type: touche
    attendu: une seule échelle | obtenu: « auf 25 » puis « 55–60 % » et « 70–80 % » : 25 n'est pas entre les deux, le curseur affiche des %
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Bb-1
