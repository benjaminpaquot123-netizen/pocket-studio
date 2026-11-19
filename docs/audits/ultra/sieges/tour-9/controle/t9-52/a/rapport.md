code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: curieux | lang: it | device: phone | font: 130 | mode: simple
mission: tuto-facile (tuto phonk, chapitres 1-4) | buts: -
chapitres: 1 « La velocità e il colore del brano » hésitation (écran 16) | 2 « La batteria » hésitation (écran 28, 50) | 3 « La 808 » hésitation (écran 62, 70) | 4 « La cowbell » compris du premier coup
promesse: id=- | texte: « Tocca a te: tocca “Copia sulle battute successive”. » | ecran: 70 | tenue: non | pourquoi: premier appui sans effet au chapitre 3, le second a marché ; au chapitre 4 un seul appui a suffi
hesitations:
  - ecran: 28 | cherchais: le texte dit « tocca “Passa a 2 battute” » mais la grille dit déjà « Ancora 9 caselle tratteggiate qui » | regarde: j'ai suivi le texte du tuto, la grille est devenue cohérente après
  - ecran: 50 | cherchais: comment aller à la battuta 2 pour la 2e case de l'Hi-hat aperto, la vue compacte n'en montre qu'une | regarde: « Lo faccio io » a ouvert la grille avec « Battuta 2 · 1 casella → »
  - ecran: 16 | cherchais: la suite du tuto après « Torna al tutorial », le panneau était replié | regarde: touché « Bravo, capitolo finito! » pour le rouvrir
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 0 | vu: l'image de l'écran 1 ne montre pas le bouton « Scarica il kit Phonk (3 MB) » que le texte décrit (image et texte décalés)
defauts:
  - geste: 70 | ecran.vu: 71 | cible: bouton ⊕ «   Copia sulle battute successive » | geste.type: touche
    attendu: la 808 copiée et le chapitre fini | obtenu: rien ne change (même écran après 3 s d'attente) ; un 2e appui (#72) a marché ; au chapitre 4 (#85) un seul appui a suffi
    symptome: rien | gravite: contourne-en-1-gestes | revu: non | banc?: temps | promesse: -
  - geste: 61 | ecran.vu: 62 | cible: icône « Tocca a te: tocca le 5 note tratteggiate della battuta 1. Leggi » ⌃ | geste.type: touche
    attendu: le même nombre de notes partout | obtenu: le tuto dit 5 notes, la grille dit « Ancora 3 note tratteggiate da toccare » (les 2 autres sont plus haut, hors de vue, sans le dire)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 18 | cible: texte « Tocca a te: sul pad “Grancassa da studio” (il pulsante che suona la cassa), tocca le 3 caselle tratteggiate. » | geste.type: touche
    attendu: le nom du pad tel qu'il est écrit dessus | obtenu: après le kit Phonk le pad s'appelle « Phonk · Grancassa 1 » ; les étapes suivantes disent bien « Phonk · Battimani »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: texte « Tonalità del brano » | geste.type: touche
    attendu: la « scheda Tono » annoncée par le tuto, et savoir quels boutons choisir | obtenu: la feuille s'appelle « Tonalità del brano » ; les deux boutons cerchiati (Frigia, Fa) ne se voient qu'au dessin, le texte ne les nomme pas
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: texte « Bravo, ce l'hai fatta! » [coupé] | geste.type: touche
    attendu: le cadre « Bravo » entier | obtenu: le cadre vert est coupé en bas, la 2e ligne du message n'est pas visible (« ⋯ » à droite)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 77 | ecran.vu: 78 | cible: bouton « Basso fantasma » [sélectionné] | geste.type: touche
    attendu: des choix qui parlent de la cowbell / mélodie | obtenu: sur la piste Melodia le choix allumé est « Basso fantasma » ; une piste « Acid 303 Linea di basso » que je n'ai pas créée est là depuis le chapitre 3
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: onglet (lignes et note) « Il mio brano » | geste.type: touche
    attendu: le nom de l'onglet lisible | obtenu: « Il mio brano » sur deux lignes, le mot « brano » déborde sous la barre
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 57 | ecran.vu: 58 | cible: texte « Ascolta la 808 con la cassa: un solo grosso colpo, poi il glide. » | geste.type: touche
    attendu: un mot expliqué pour un curieux | obtenu: « glide » n'est jamais expliqué (et « 3-3-2 » au chapitre 4 non plus)
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 58 | vu: Kick | attendu: Cassa (le tuto dit déjà « cassa »)
  - ecran: 26 | vu: clap | attendu: battimani (nom du pad)
  - ecran: 74 | vu: La cowbell | attendu: La campana (nom du pad « Campana » et du chapitre 1)
defauts-avant:
fin-tranche: oui (ROUTE ligne 472)
