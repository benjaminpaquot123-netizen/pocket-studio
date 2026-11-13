siege: b | profil: t6-Gb musicienne-id-tablette-padlive | lang: id | device: tablet8 | font: 100 | mode: simple
mission: padlive | buts: son, stop, prise
promesse: id=t6-Gb-1 | texte: Pad Live BARU Pad siap dimainkan, suaramu, efek live. Langsung berbunyi. | ecran: 3 | tenue: oui | pourquoi: premier toucher sur « Tepuk tangan » : le pad s'allume et joue une fois tout de suite
promesse: id=t6-Gb-2 | texte: Merekam yang kamu mainkan di pad, per birama penuh (aucun texte vu sur l'attente des pads) | ecran: 17 | tenue: non | pourquoi: 5 essais (écrans 5, 6, 14, 17, 28), loop déjà en route ou musique lancée : le pad passe tout de suite [allumé], jamais d'anneau pâle d'attente
promesse: id=t6-Gb-3 | texte: Stop semua (kotak merah di bawah) menghentikan semuanya: pad, musik, dan waktu. | ecran: 18 | tenue: oui | pourquoi: écrans 18, 29, 35, 40 : tout s'arrête, le ▶ du haut revient, tempo 124 inchangé, aucun compteur ne tourne
promesse: id=t6-Gb-4 | texte: Pad Live punya 4 halaman berisi 16 pad, A sampai D | ecran: 33 | tenue: non | pourquoi: rien en français ni coupé, mais des mots anglais restent : « bar » (écrans 19-21, alors que l'aide dit « birama »), « take-mu » (écran 33)
oeil-neuf: q1 | ecran: 40 | image: 40 | tout ce qui a un cadre se touche ; les 4 « ▶ » gris à droite des rangées ont l'air touchables mais rien ne dit ce qu'ils lancent
oeil-neuf: q6 | ecran: 40 | image: 40 | la bande « Penampilan disimpan · 0:07 » prouvait la prise, mais elle a disparu après « Dengar » : plus aucune preuve à l'écran, je ne sais pas où retrouver ma prise
oeil-neuf: q9 | ecran: 40 | image: 40 | rien de coupé ni superposé à 100 % ; « Teriakan “Go go go!” » passe sur deux lignes mais reste lisible
hesitations:
  - ecran: 2 | cherchais: comment enregistrer ma performance | regarde: deux REC (rond rouge en haut et « Rekam yang aku mainkan ») ; il a fallu les « ? » pour comprendre la différence
  - ecran: 17 | cherchais: un signe que le pad attend la mesure | regarde: le pad s'allume direct, je ne sais pas s'il part tout de suite ou au temps suivant
  - ecran: 36 | cherchais: fermer le menu « Lainnya » | regarde: le bouton devient « Tutup » mais garde l'icône ⋯
abandon: jamais
incidents-banc:
  - geste: 8 | attente: 0 | vu: écran 9, le texte du dialogue d'aide « Memainkan efek live » n'apparaît pas dans la version texte (visible sur l'image)
defauts:
  - geste: 16 | ecran.vu: 17 | cible: texte « Loop bass » | geste.type: touche
    attendu: avec une boucle déjà en route, le pad touché attend la mesure suivante (anneau pâle) avant de partir | obtenu: le pad est tout de suite [allumé], jamais d'état d'attente visible (5 essais, avec et sans musique du projet)
    symptome: rien
    gravite: finition | revu: oui: ecran 28 | banc?: temps | promesse: t6-Gb-2
  - geste: 18 | ecran.vu: 19 | cible: bouton « Siap: mulai di bar berikutnya » | geste.type: touche
    attendu: « birama » comme dans l'aide de ce bouton (« per birama penuh ») | obtenu: mot anglais « bar », aussi dans « 2 bar · ketuk untuk selesai » et « Rekamanmu: 4 bar »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: t6-Gb-4
  - geste: 32 | ecran.vu: 33 | cible: texte « Pad live dari take-mu ada di trek audio “Pads live”. » | geste.type: touche
    attendu: « rekamanmu » (mot déjà utilisé ailleurs) | obtenu: « take-mu », anglais mélangé, phrase peu naturelle
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Gb-4
  - geste: 38 | ecran.vu: 39 | cible: texte « Dengar » | geste.type: touche
    attendu: la prise reste visible (bande « Penampilan disimpan ») ou l'écran dit où la retrouver | obtenu: la bande disparaît dès que j'écoute ; plus aucune trace de la prise à l'écran
    symptome: perdu
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 35 | ecran.vu: 36 | cible: bouton ⋯ « Tutup » | geste.type: touche
    attendu: une icône de fermeture (✕ ou flèche) pour « Tutup » | obtenu: l'icône ⋯ « plus » reste alors que le bouton ferme
    symptome: incompris
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: -
  - geste: 35 | ecran.vu: 36 | cible: bouton (trois points reliés) « Bagikan » | geste.type: touche
    attendu: un seul « Bagikan » à l'écran | obtenu: deux « Bagikan » (bande de la prise et menu « Lainnya ») sans dire en quoi ils diffèrent
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 19 | vu: bar | attendu: birama
  - ecran: 33 | vu: take-mu | attendu: rekamanmu
