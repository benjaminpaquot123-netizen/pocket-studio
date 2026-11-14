siege: a | profil: t6-Ga | lang: tr | device: phone | font: 130 | mode: pro
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t6-Ga-1 | texte: Remix Parça ayırma: davul, bas, vokal ve geri kalanı. | ecran: 37 | tenue: oui | pourquoi: Davul/Bas/Kalan/Vokal apparaissent (écran 14), « Dinle » joue la chanson et chaque piste se coupe pendant l'écoute (écran 37)
promesse: id=t6-Ga-2 | texte: Vokali cümlelere böl | ecran: 15 | tenue: oui | pourquoi: « 9 ile 14 arası pad'lerinde 6 cümle. », les pads Vokal 1 à 6 existent et sonnent (écran 26)
promesse: id=t6-Ga-3 | texte: (aucun message « sonu kesilecek » vu) | ecran: 13 | tenue: non | pourquoi: chanson de 0:40 ajoutée à un projet d'1 mesure : aucun avertissement, le projet passe en silence à 20 mesures ; passage « 20. ölçüden, 8 ölçü » (écran 42) sans avertissement non plus
promesse: id=t6-Ga-4 | texte: 0:00 / 0:40 · başlangıç | ecran: 13 | tenue: non | pourquoi: tout est en turc, mais des textes sont coupés à 130 % (« başlangıç », « hazırlanıyor… », « Sıra sende: »)
oeil-neuf: q1 | ecran: 43 | image: 43 | le gros « + Bir şarkı seç », les onglets et le Rehber se touchent ; l'onglet coupé à droite (« Remix Ayrıştır ») ne se devine pas
oeil-neuf: q6 | ecran: 43 | image: 43 | après « İptal » je suis dans le projet Pop sans preuve de ce qui s'est passé ni de la chanson ; pas de suite évidente
oeil-neuf: q9 | ecran: 43 | image: 43 | « Sıra sende: » coupé par la barre du bas, titre du Rehber tronqué « Kick: ses, kalıp, ne… »
hesitations:
  - ecran: 20 | cherchais: mes 6 phrases de voix « sur les pads 9 à 14 » | regarde: Live > Pad'ler montre 8 pads vides « + », il fallait trouver Davul 16 pad (écran 25)
  - ecran: 13 | cherchais: si j'avais bien eu la voix seule | regarde: la carte dit « ŞARKI » et la chanson entière 0:40, la séparation n'est visible qu'en ouvrant « Kanal panosunu aç »
  - ecran: 41 | cherchais: un endroit qui dise si la fin sera coupée | regarde: rien, ni en « Şarkının tamamı » ni en « Sadece bir pasaj »
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: outil hors human-act.py : Read sur les consignes et sur les images de mon siège (screen-2/9/13/21/37/43-grille.png), Write pour ce rapport
defauts:
  - geste: 12 | ecran.vu: 13 | cible: texte « 0:00 / 0:40 · başlangıç » | geste.type: touche
    attendu: un message « şarkının sonu kesilecek » ou au moins « proje 20 ölçüye uzatıldı » | obtenu: rien, le projet d'1 mesure devient 20 mesures sans un mot
    symptome: sans-aide | gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: t6-Ga-3
  - geste: 12 | ecran.vu: 13 | cible: texte « 0:00 / 0:40 · başlangıç » | geste.type: touche
    attendu: « 0:00 / 0:40 · başlangıç » lisible | obtenu: sur l'image on lit « 0:00 / 0:40 · » puis « Efektler », le mot « başlangıç » a disparu
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ga-4
  - geste: 19 | ecran.vu: 21 | cible: texte « Çalmaya hazır pad'ler hazırlanıyor… » | geste.type: touche
    attendu: lire « … hazırlanıyor… » (en préparation) | obtenu: l'image n'affiche que « Çalmaya hazır pad'ler » (= « pads prêts ») au-dessus de 8 pads vides « + », le sens s'inverse ; ça ne change qu'en passant à 16 pads
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ga-4
  - geste: 42 | ecran.vu: 43 | cible: texte « Sıra sende: bu sesi tek başına dinle, sonra karar ver. » [coupé] | geste.type: touche
    attendu: la phrase du Rehber lisible | obtenu: « Sıra sende: » coupé net par la barre Geri al/Yinele, le reste invisible
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ga-4
  - geste: 42 | ecran.vu: 43 | cible: texte « İptal » | geste.type: touche
    attendu: « İptal » ferme la fenêtre et me laisse sur la page Remix (écran 41 venait de là) | obtenu: je me retrouve dans le projet Pop, onglet Remix, sans ma chanson
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 2 | cible: bouton « Modeli indir (174 MB) » | geste.type: touche
    attendu: un seul bouton de téléchargement | obtenu: deux « Modeli indir (174 MB) » (dans la carte et en bas), pareil ensuite avec deux « Bir şarkı seç » (écrans 5 et 37)
    symptome: incompris | gravite: finition | revu: oui: ecran 37 | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: bouton ✂ «  Şarkıyı kes » | geste.type: touche
    attendu: un seul « Şarkıyı kes » | obtenu: deux boutons « Şarkıyı kes » identiques l'un sous l'autre (533 et 901)
    symptome: incompris | gravite: finition | revu: oui: ecran 24 | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: onglet « Ma chanson préférée Şarkı » [coupé, sélectionné] | geste.type: touche
    attendu: voir l'onglet de ma chanson ou un signe qu'on peut défiler | obtenu: onglet coupé au bord droit, rien n'indique qu'il y a une suite (idem « Üret » coupé)
    symptome: cache | gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 14 | ecran.vu: 15 | cible: texte « 9 ile 14 arası pad'lerinde 6 cümle. » | geste.type: touche
    attendu: le message dit où trouver les pads 9 à 14 | obtenu: rien ne dit que c'est le canal « Davul 16 pad » ; dans Live > Pad'ler (8 pads) je ne les trouve pas
    symptome: sans-aide | gravite: contourne-en-6-gestes | revu: oui: ecran 25 | banc?: non | promesse: -
mots-faux:
  - ecran: 3 | vu: Önce ayırma modeli | attendu: Önce ayrıştırma modeli (même mot que la page Remix, « ayrıştırma »)
