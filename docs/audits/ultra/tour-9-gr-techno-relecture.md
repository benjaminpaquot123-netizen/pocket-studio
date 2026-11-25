# Tour 9, lot gr-techno : relecture neuve

relu : d22112e23c438879e0bcf37d4033a333a1423575

Relecture par un sous-agent neuf qui n'a pas écrit le code (lecture de `git diff 3d9436bb0..d22112e2`, 42 fichiers).
Rejeu débutant neuf : `scripts/bench/tuto-sweep.sh` sur les 6 cours techno (techno, retro_techno, acid_techno, detroit,
hard_techno, industrial_techno), code testé d22112e2 : 0 chapitre bloqué, 0 défaut neuf (diff vide avec la base du tour 9).

## Points relevés

1. Tests changés : chacun suit un son voulu (303 sur 32 pas, crash au Drop, lead techno absent du Break, basse de la
   hard techno cochée par défaut, nouvelles courbes acidcore et pad techno). Aucun ne masque de régression.
2. Moyen, laissé : le tuto techno fait écrire un « Lead filtré » en riff d'octaves, alors que le style propose
   maintenant un stab métallique sur une note (lead décoché par défaut). Rien ne casse ; à aligner dans un lot tuto.
3. Mineur, laissé : AuditSonTest ne couvre plus « lead de hard techno coché → EQ présente ».
4. Mineur, laissé : exception du Mode simple pour le rumble des styles acides (borne 10..22 cartes gardée).
5. Mineur, laissé : withFilterAsked remet `level` à celui de la bibliothèque (même convention que FxPresets et GuideSheets).
6. Mineur, laissé : guides/techno.json, carte lead, simpleWhy « qu'on chante » face à « pas de mélodie au départ ».
7. Mineur, laissé : commentaire de WideMix.leadTrackId pas mis à jour pour les pistes de corps exclues.
8. Mineur, laissé : « Charlotte de Witte – Doppler » datée 2020 (house) et 2021 (techno).
9. Recettes : accents acides tous sur des pas joués ; coupe du bas du lead trance et gabber vers 150 Hz. Traductions :
   14 fichiers valides, ajouts présents partout.

Verdict : 0 bloquant, 0 grave
