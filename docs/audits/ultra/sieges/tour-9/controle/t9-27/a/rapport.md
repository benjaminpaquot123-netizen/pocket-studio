code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: pro | lang: id | device: tablet8 | font: 100 | mode: pro
mission: tuto-pro (tuto gabber ch. 1-7) | buts: -
promesse: id=- | texte: finir les chapitres 1 à 7 du tuto gabber | ecran: 124 | tenue: oui (7/7 finis en 124 gestes sur 174) | pourquoi: chaque étape se valide, « Tunjukkan » montre le chemin
chapitres: ch1 compris du premier coup | ch2 compris du premier coup (petite hésitation : jouer le pad kick ne valide pas « dengarkan kick ») | ch3 compris du premier coup | ch4 hésitation (note de la mesure 2 pas au même endroit, 6 gestes de nettoyage) | ch5 hésitation (compteur de notes trompeur) | ch6 hésitation (renommer la partie : 8 gestes pour trouver où) | ch7 compris du premier coup
hesitations:
- ecran: 98 | cherchais: renommer la partie « A » en « Groove » | regarde: menu « A ▾ » (écran 99, pas de renommage), appui-long, onglet Struktur ; trouvé dans ⋮ de la ligne du lagu (écran 106, « Ganti nama »)
- ecran: 55 | cherchais: la note pointillée de la mesure 2 de l'accord | regarde: au même temps que la mesure 1, alors qu'elle était au temps 3
- ecran: 75 | cherchais: combien de notes reste-t-il au riff | regarde: le bandeau « Masih N not », qui remonte de 1 à 3 puis 6
abandon: jamais
incidents-banc:
- geste: #47 | attente: écran du tuto après « Langkah berikutnya » | vu: écran texte presque vide, normal au « regarde » suivant
- outils hors human-act.py : Read des screen-N-grille.png du siège (regarde image), cat/ls dans le siège, lecture des deux gabarits demandés
defauts:
- geste: #5 | ecran.vu: 5 | cible: « Contoh dilewati tanpa didengarkan. » | geste.type: touche
    attendu: l'exemple écouté est compté (j'ai appuyé Berikutnya pendant qu'il jouait) | obtenu: le tuto dit que je l'ai sauté sans l'écouter (revu écran 14)
    symptome: texte-faux | gravite: finition | revu: oui | banc?: non | promesse: -
- geste: #21 | ecran.vu: 21 | cible: « Sudah siap: kick keras dari “Buatkan aku lagu”… » | geste.type: touche
    attendu: un texte qui suit mon parcours (je suis passé par le tuto, jamais par « Buatkan aku lagu ») | obtenu: phrase qui parle d'un bouton que je n'ai pas utilisé
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
- geste: #23 | ecran.vu: 23 | cible: « dengarkan kick sendirian » | geste.type: touche
    attendu: jouer le pad kick seul valide l'écoute du kick | obtenu: rien ne se valide, il faut « Dengar contohnya »
    symptome: rien | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
- geste: #56 | ecran.vu: 55 | cible: « Akor · birama 1/8 » | geste.type: touche
    attendu: la note pointillée de la mesure 2 au même temps qu'en mesure 1, ou une flèche claire | obtenu: elle est au temps 3 ; mon toucher au temps 1 a posé un accord F♯m parasite
    symptome: perdu | gravite: contourne-en-6-gestes | revu: non | banc?: non | promesse: -
- geste: #57 | ecran.vu: 57 | cible: « Pola terpasang! Sisanya disalin: » | geste.type: touche
    attendu: la phrase dit ce qui a été copié | obtenu: la phrase finit par « : » et rien derrière (revu écrans 58 et 84)
    symptome: texte-coupe | gravite: finition | revu: oui | banc?: non | promesse: -
- geste: #58 | ecran.vu: 58 | cible: « Tempel birama 6 » | geste.type: touche
    attendu: une barre d'édition qui parle de la mesure où je suis (mesure 2) | obtenu: « Tempel birama 6 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
- geste: #75 | ecran.vu: 75 | cible: « Melodi · birama 1/8 ↑ Masih 7 not bergaris putus untuk disentuh Ketuk di sini » | geste.type: touche
    attendu: un compte total des notes du riff qui ne fait que descendre | obtenu: « Masih 1 » puis « Masih 3 », « Masih 6 » ; flèche ↓ alors que les notes sont à l'écran (écran 78)
    symptome: incompris | gravite: finition | revu: oui | banc?: non | promesse: -
- geste: #99 | ecran.vu: 99 | cible: « Bagian yang diedit » | geste.type: touche
    attendu: renommer la partie depuis son menu « A ▾ » | obtenu: seulement « A » et « Bagian kosong baru » ; le renommage est caché dans ⋮ du lagu (8 gestes)
    symptome: cache | gravite: contourne-en-8-gestes | revu: non | banc?: non | promesse: -
- geste: #105 | ecran.vu: 105 | cible: « + A » | geste.type: appui-long
    attendu: un appui long ouvre les options de la partie | obtenu: il ajoute A au lagu, comme un toucher simple
    symptome: mauvais-ecran | gravite: finition | revu: non | banc?: non | promesse: -
- geste: #111 | ecran.vu: 111 | cible: « Timeline » | geste.type: touche
    attendu: onglets « Timeline / Bagian » entiers | obtenu: coupés en haut sous la barre du BPM (aussi écran 105)
    symptome: texte-coupe | gravite: finition | revu: oui | banc?: non | promesse: -
mots-faux:
- ecran: 86 | vu: « Ketebalan (detune): tambah +15 » avec « naikkan … minimal 15 » | attendu: une seule consigne (le réglage était déjà à 52 %, on ne sait pas s'il faut +15 ou au moins 15)
fin-tranche: non (dernier step=map_phrases, done=22 ; les 22 étapes de la tranche sont faites, mais l'oracle step=make_intro n'est jamais lu : le testeur a fait « fin » avant de passer au chapitre 8)
