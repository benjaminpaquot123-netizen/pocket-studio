# Tour 9, lot L8 : décisions (correcteur)

Base 7455749a5, branche eq/tour-9-L8.

- F133 : le nom de la piste ne répète plus un mot du son (`DefaultNames.soundBeside`) ; test toutes langues, tous styles. Retour arrière : revert du commit F133.
- F119 / F120 : feuille Tempo et « Fermer » kit/pad encadré, 12 dp, 48 dp de haut.
- Tempo et feuilles Kit/Pad à 360 dp, 200 % (vu sur capture) : sous 260 dp utiles, le haut s'empile (titre, puis « Fermer », puis la valeur ; cases de kit une par ligne). Raison : « Fermer » et « 124 BPM » étaient coupés lettre par lettre. Retour arrière : supprimer la branche « narrow ».
- F030 : signe moins en RTL dans Partager (isolation bidi).
- F123 : M4A/WAV ne cachent plus le champ nom ; disposition serrée si hauteur < 380 dp et largeur < 300 dp.
- F124, F134, F135 : non reproduits sur la base ; garde-fous (tests) posés, couleur explicite pour « Annuler ». La partie onglets de F135 est dans AppRoot (interdit).
- F126 : le rond coché porte un sens (description et légende).
- F131 (grave) : test de garde seulement ; la vraie correction est dans AppRoot / tuto (interdits).
- F127, F128, F129 : dans AutomationCard.kt (interdit) : non faits.
- F125 : Partager compte les parties distinctes (exigé par Tour8ShareListenAndPartsTest #65) ; l'accueil compte p.song.size dans RhythmViewModel.kt (interdit) : non fait.
- F121, F122, F132 : demandent de nouvelles clés I18n (fichiers hors liste) : non faits.
- Vu sur capture, hors liste : « Mes fichiers » tronqué à 200 % (export/MyExports.kt:126) : à confier au lot qui tient ce fichier.
