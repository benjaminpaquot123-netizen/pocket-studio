# Audit ultra, tour 2 : synthèse des 8 sièges (lots A et B)

Sièges : scratchpad/ultra/tour-2/lot{A,B}/{a..d}/ (rapport.md, journal.txt, screen-N.txt/png). Notes banc : tour-2/notes-banc.txt (à lire d'abord).
A a senior fr 200 % tuto · A b remix es · A c enfant 6 ans tablet11Land (ne lit pas) défi · A d DJ en Pro exports
B a parent une main phone360 premier lancement · B b guitariste ko tablet8 voix · B c producteur foldable Pro synthé · B d anxieuse thème nuit Simple
×N = sièges ; « revient » = déjà au tour 1 (la correction n'a pas tenu ou pas partout).

## Zone 1 — Pads, Pad Live, défis, remix
1. ×2 Bibliothèque d'un pad → « Poser » : le pad garde son nom, rien ne sonne, Annuler grisé (A c 4-6, B d 4). SUSPECT BANC : UnsatisfiedLinkError
   NativeEngine.sampleCreate (sink null) dans placeOnPad (notes-banc). Prouver par un test sans banc ; dans tous les cas, échec de chargement = message, jamais muet.
2. revient : Pad Live s'ouvre sur 16 cases grises vides (A c 1-2 : tablet11Land, premier lancement réel) et de nouveau vide après avoir fait un morceau (A c 41).
3. Bloquant : défis introuvables sans lire (A c 39) ; le drapeau orange de l'accueil ouvre « Simple ou Pro ? » + assistant (A c 12) ; la cible rouge de Pad Live = encadré de texte (A c 42). Il faut une entrée imagée (étoile/jeu) pour les défis.
4. Pad Live depuis l'accueil crée et ouvre en silence un projet « Live » ; le projet Remix reste derrière (A b 32, 41).
5. Le bout posé « Voz 1 está en el pad 9 » ne dit pas quel son de batterie a été remplacé (A b 16).
6. Appui long sur un pad : 1re fois la bibliothèque entière, ensuite une petite fiche « Pad 1 » (B d 2) ; demande de Benjamin : appui long = « Changer le son », toujours le même ; son actuel non marqué, « Volume du pad 100 % » vs curseur 80 % (B d 29).
7. « Ma bibliothèque » introuvable sous ce nom (A b 27) ; « Cortar lo que quiero » ajoute toute la chanson au projet sans le dire (A b 14) ; TalkBack « Cortes: » / « Imán: » sans valeur (A b 14) ; glisser la rangée d'onglets sélectionne un onglet (A b 37).

## Zone 2 — Exports et master
8. Club et streaming : mêmes chiffres (−12,9 LUFS, −12,0 dBTP), seul l'écart change (A d 31, 47). « Length: 3 s » = banc (3 s de son fixe). Les LUFS : trancher par un test sans banc si le gain de cible est appliqué au rendu.
9. Club −7,5 (puce) vs −8 (aide) (A d 17) ; « Auto (style) · −7,5 » et « Club · −7,5 » indiscernables (A d 16) ; rapport club écrit « comme le streaming (−14) » (A d 31) ; « ✓ Prêt à partager » vert au-dessus d'un écart de 5,4 LU (A d 31).
10. Export : aucune progression, seulement « Annuler » 30-120 s (A d 45) ; M4A « 16 bits » au lieu d'un débit (A d 50) ; dossier d'enregistrement absent du rapport WAV et invisible pour TalkBack (A d 55) ; Partager présélectionne M4A, Mix WAV, sans cible affichée (A d 41).

## Zone 3 — Navigation, prise en main, sauvegarde, une main
11. Grave : le Guide ouvert couvre la moitié basse, cache la barre du pouce (Jouer) ; panneau « Le Guide lit ton morceau… » vide (B a 18-20).
12. ×2 Grave : premier morceau trop long : assistant 10 étapes ≈ 16 écrans avant le son (B a 4) ; 8 écrans de texte (A c 17).
13. Une main : carte « Simple » trop haut (B a 3) ; « Choisir ce style » repoussé sous la liste d'exemples (B a 6) ; « Mes projets » seulement en haut (B a 23) ; deux « Retour » sur l'écran d'étape (B a 9) ; bloc « Les vrais sons du style » répété à chaque étape (B a 8) ; étape sans titre d'instrument (B a 9).
14. Projet fantôme « Mon premier rythme » dans Mes projets (B a 25).
15. ×2 revient : sauvegarde invisible : coche minuscule, nom coupé « Mon pr… », aucun mot « Sauvegardé » en quittant (B d 1, 19 ; tour 1 B b). Retour sur l'accueil ne quitte pas l'appli (B d 19 — vérifier banc). Retour depuis Mes projets (ouvert du morceau) → accueil au lieu du morceau (B d).
16. L'accueil change après un retour (carte chapeau vert disparue) (A c 9).
17. Changer le son d'une piste / supprimer une piste : aucun message (B d 8, 14) ; « Supprimer » caché dans une fiche nommée « Volume de Synthé » (B d 12) ; ×2 nom de piste qui ne suit pas le preset (« Synthé » au lieu de « Basse sub »/« Sub House Ben », B d 8, B c 46).

## Zone 4 — Enregistrement
18. Prises : même heure « 테이크 03:37 », ordre qui change à chaque sélection → numéroter (Prise 1, 2…) et ordre stable (B b 10) ; « 빼기 » pour supprimer (B b 10) ; « s » vs « 초 » (B b 7).
19. Interrupteur « Son » affiche « éteint (꺼짐) » alors que la piste sonne (B b 14) ; enlever le solo arrête toute la lecture (B b 18).
20. Vumètre d'entrée = trait fin, sans libellé ni nom TalkBack (B b 5) ; piste micro « Audio 1 » non traduite (B b 5).
21. Rien sur l'accueil pour enregistrer voix/guitare (B b 1) ; le rond rouge de Rythme n'enregistre que les pads (B b 2, revient tour 1 n° 30).

## Zone 5 — Tutos, synthé, notes
22. revient Grave : tuto « Lire la suite » ouvre « Le kit Techno… 0 % » (A a 7, tour 1 n° 34) ; « Lire la suite » fait défiler et coupe (A a 19).
23. revient : à 200 %, « Écouter mon morceau » / « Écouter l'exemple » = deux « Éco… », « Précédent » réduit à une flèche (A a 43) ; texte « Encore 4 cases… » sous le bandeau « Partie : A » (A a 31, 38) ; « À toi : sur le pad Rumble » sans montrer le pad (A a 38).
24. Tuto : fiche Tonalité sans « Réussi » (A a 22) ; écran vide un instant à « Je le fais » (A a 30, 37) ; « mes. 1/1 » (A a 44) ; onglet « Mon morceau » coupé, « Mix » plus gros (A a 6).
25. Synthé : réglages du son introuvables depuis « Options de Synthé » (B c 6) ; valeurs sans unité (« Résonance 35 »), Bend « 0.0 » (B c 23) ; fiche : « passe-haut vers 80 Hz » alors que le filtre est un passe-bas à 364 Hz (B c 14) ; clavier flottant couvre presets et « Unisson » (B c 20).
26. ×2 revient : renommer ajoute au texte au lieu de le remplacer (B c 34 « Nom du son », tour 1 E b) — sélectionner tout à la prise du champ, partout.
27. Grave : piano roll sur pliable : grille ≈ 450 px sur 1243 (barres en double, Guide, onglets, Mode retours) (B c 66). Appui long sur une note l'efface sans message (B c 71).

## Zone 6 — Textes, langues, accessibilité
28. Jargon et anglais en Simple (Kick, Rumble, Clap, BPM, Drop… A a 17 ; 노멀라이즈, 리버스 B b 7).
29. Coréen : « 당신의 » mot à mot, « 나중에: 시작 비트 보기 » (B b 1) ; 반말 dans une consigne + espace « ▶ 를 » (B b 14) ; noms de piste anglais.
30. Contraste : « Annuler » grisé en thème nuit (B d 9) ; banc LAYOUT CONTRAST : boutons Remix « Télécharger le modèle » / « Choisir une chanson » 1,38-2,04:1 (A c journal #50), « Poser » 1,21-2,75:1 (B d journal #3) — vérifier si réel (thème) ou faux positif du banc.
31. Remix : « Choisir une chanson » lance aussi le téléchargement du modèle 174 Mo sans confirmation (A b 2).

## Banc
- Siège B d : UnsatisfiedLinkError sampleCreate (sink null) → test rouge en fin de lot B, et peut-être le n° 1 (voir notes-banc).
- Export : 3 s de son fixe (durée d'export non vérifiable sur le banc) ; à documenter dans le rapport du banc.
