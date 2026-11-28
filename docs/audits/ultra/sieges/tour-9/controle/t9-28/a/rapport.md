code testé : ab7fbf4e8
joué par : sous-agent neuf
siege: a | profil: pro | lang: id | device: phone | font: 130 | mode: pro
mission: tuto-pro | buts: finir les chapitres 8 à 14 du tuto gabber (sections, arrangement, transitions, automation, mix, master, export)
promesse: ch8 Bangun bagian-bagianmu : hésitation (fini au geste ~20 ; le bouton « + Bagian baru (salinan…) » ne fait pas ce qu'il annonce)
promesse: ch9 Susun bagian-bagianmu : compris du premier coup (7 blocs, 144 mesures, 3:17)
promesse: ch10 Transisi : hésitation (roll ×4 + suppression des coups barrés OK ; en-tête de grille coupé, pad cymbale mal nommé)
promesse: ch11 Efek : bloqué à la main (Mix intouchable), validé seulement par « Aku yang buat » (geste 62)
promesse: ch12 Mix : bloqué (étape 1/3 « baisser le fader Akor » impossible, faders intouchables, même après démo et retour par la liste des chapitres) ; étapes 2 et 3 lues
promesse: ch13 Mastering : hésitation (« Klub » refusé au premier toucher, accepté juste après la démo « Tunjukkan ») ; fini geste ~87
promesse: ch14 Ekspor lagumu : compris du premier coup (WAV 48 kHz 24-bit, −8,0 LUFS, −3,2 dBTP, partage Drive)
hesitations: ch8 « + Bagian baru (salinan D · Drop) » a créé « Break (salinan Groove) » ; ch10 en-tête de grille coupé ; ch13 premier toucher sur « Klub » sans effet
abandon: aucun ; chapitre 12 étape 1 laissée non faite (liste de fin « Tinggal 5 langkah »)
incidents-banc: écran Mix : élément « image (courbe) @ 0,0 » sur tout l'écran, le banc dit « un autre élément est dessiné par-dessus » à chaque toucher sur le contenu (gestes 49-56, 60, 65, 67, 70, 80, 103, 104) alors que les glissés de défilement passent (geste 79) ; outils hors human-act.py : Read sur screen-3/28/49/62/66/69/78-grille.png du siège, cat sur start.txt, rapport.md, screen-1.txt, screen-106.txt, journal, turn du siège
defauts:
- geste: 52 | ecran.vu: 49-56, 60, 65, 67, 70, 80, 103-104 | cible: page Mix (« + Otomasi pengaturan », faders Akor/Melodi, chip « Klub ») | geste.type: touche / glisse | attendu: ajouter la courbe Cutoff sur Akor, baisser le fader Akor, choisir Klub | obtenu: rien ne réagit, seul le défilement passe ; chapitres 11 et 12 impossibles à la main (le tuto ne passe que par « Aku yang buat » / « Berikutnya ») | symptome: gel-tactile | gravite: mission-impossible | revu: oui, refait après sortie vers Suara et retour (53-54), après démo (58, 102), depuis la liste des chapitres (100-101) | banc?: possible (calque « image (courbe) @ 0,0 » plein écran) | promesse: ch11, ch12
- geste: 11 | ecran.vu: 11, 15 | cible: bouton « + Bagian baru (salinan D · Drop) » | geste.type: touche | attendu: une copie de Drop | obtenu: « Break (salinan Groove) » ; au geste 15 le même genre de bouton a fait une simple copie F | symptome: texte-faux | gravite: contourne-en-2-gestes | revu: oui (15) | banc?: non | promesse: ch8
- geste: 79 | ecran.vu: 78-79 | cible: chip « Klub · −8 LUFS » | geste.type: touche | attendu: Klub choisi | obtenu: « Aku yang buat » fait défiler la page pour que Klub passe sous la carte du tuto (coupé à 794), il faut défiler soi-même | symptome: cache | gravite: contourne-en-2-gestes | revu: oui (83) | banc?: non | promesse: ch13
- geste: 97 | ecran.vu: 97-98 | cible: liste « Tinggal 5 langkah » | geste.type: glisse | attendu: voir les 5 étapes restantes | obtenu: seules 4 sont visibles (Bab 2, 5, 7, 7) ; l'étape oubliée du ch12 n'apparaît pas | symptome: liste-tronquee | gravite: finition | revu: oui (98) | banc?: non | promesse: ch14
- geste: 5 | ecran.vu: 23 | cible: « Buat Build-up » | geste.type: touche | attendu: Build-up de 8 mesures comme dit le ch8 | obtenu: 4 mesures (le ch9 le remet à 8) | symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: ch8
- geste: 3 | ecran.vu: 3, 6, 12 | cible: carte du tuto | geste.type: touche (replier) | attendu: la carte reste repliée | obtenu: elle se rouvre à chaque étape et cache la liste des sections (« LAGUNYA » coupé) | symptome: cache | gravite: finition | revu: oui | banc?: non | promesse: ch8-ch14
- geste: 48 | ecran.vu: 48 | cible: « Pergi ke bagian “Build-up” » | geste.type: touche | attendu: aller au Build-up | obtenu: ouvre Mix, l'en-tête reste « Drop birama 33/144 » | symptome: navigation | gravite: finition | revu: non | banc?: non | promesse: ch11
- geste: 2 | ecran.vu: 2-106 | cible: barre du haut | geste.type: regarde | attendu: le nom du projet visible | obtenu: « Proyekku 1 » disparaît sur Struktur/Mix ; « Unduh kit Gabber (4 MB) » répété à chaque étape | symptome: finition | gravite: finition | revu: oui | banc?: non | promesse: ch8-ch14
mots-faux: écran 36 « 4 ketukan kelebihan di sini (dicoret) » : ce sont des coups (pukulan), pas des temps ; écran 94 « Music › BenStudio » ≠ nom de l'appli
defauts-avant: aucune cible donnée dans la fiche

fin-tranche: non (dernier step=levels, done=27)
