# Tour 9, lot L8 : décisions (correcteur)

Base 7455749a5, branche eq/tour-9-L8.

- F133 : le nom de la piste ne répète plus un mot du son (`DefaultNames.soundBeside`) ; test toutes langues, tous styles. Retour arrière : revert du commit F133.
- F119 / F120 : feuille Tempo et « Fermer » kit/pad encadré, 12 dp, 48 dp de haut.
- Tempo et feuilles Kit/Pad à 360 dp, 200 % (vu sur capture) : sous 260 dp utiles, le haut s'empile (titre, puis « Fermer », puis la valeur ; cases de kit une par ligne). Raison : « Fermer » et « 124 BPM » étaient coupés lettre par lettre. Retour arrière : supprimer la branche « narrow ».
- F030 : signe moins en RTL dans Partager (isolation bidi).
- F123 : M4A/WAV ne cachent plus le champ nom ; disposition serrée si hauteur < 380 dp et largeur < 300 dp.
- F124, F134, F135 : non reproduits sur la base ; garde-fous (tests) posés, couleur explicite pour « Annuler ». La partie onglets de F135 est dans AppRoot (interdit).
- F126 : le rond coché porte un sens (description et légende).
- F131 (grave) : passé à L10 par le chef. Ici, seulement le test de garde (BackClosesSheetFirstTest, déjà vert sur la base : il ne reproduit pas). Cas des sièges à rejouer par L10 : t9-09 (rapport a, lignes 27-28 : geste 52, feuille des pas « Mesure 2/2 · Bord de caisse », Retour mène à l'accueil « Que veux-tu faire ? ») et t9-55 (rapport a, lignes 28-29 : geste 102, courbe « Filtr Odcięcie » ouverte depuis le Mix, Retour sort vers « Co chcesz zrobić? »).
- F127, F128, F129 : passés à L2 par le chef (AutomationCard.kt).
- F125 : Partager dit les parties distinctes (#65 tient) et, s'il y a des reprises, « (avec les reprises : 11) », le compte de l'accueil. Retour arrière : revert du commit vert F125/F122/F121.
- F122 : interrupteur « Renommer aussi le projet » (projet au nom par défaut seulement), allumé par défaut ; éteint, le projet garde son nom et le texte le dit.
- F121 : Streaming dit « −14 LUFS, le niveau de Spotify et YouTube » ; Auto dit « le volume habituel de ton style » (plus « club ») ; les détails disent « Mesuré pendant l'écoute ». Le « (−14) » seul reste dans Loudness.describe (Loudness.kt, hors liste) mais Partager le remplace.
- Fichiers i18n touchés (clés ajoutées seulement, 5 par langue) : project-model/src/main/resources/i18n/{ar,de,en,es,hi,id,it,ja,ko,nl,pl,pt-BR,tr,zh-CN}/partage.json.
- F132 : non fait. Le nom « Drum & bass · Caisse FX » vient de RhythmScreen.padName (interdit) ; changer DefaultNames.shownSample casserait le contrat du tuto (Lot6VmTutoKitNameTest « Techno · Kick ») et la lecture du kit (KitAndPadSheets.kt:85). À confier au lot qui tient RhythmScreen ou le tuto.
- Vu sur capture, hors liste : « Mes fichiers » tronqué à 200 % (export/MyExports.kt:126) : à confier au lot qui tient ce fichier.
- Lot6UiShareLevelTest : son attendu suit maintenant shareLevelWords (mêmes mots en Simple et en Pro, intention du lot 6 gardée).
- Banc (selftest rouge « share_bars_8 ») : la phrase « renomme aussi le projet » devient l'étiquette de l'interrupteur (une ligne au lieu de deux), les cases de mesure restent entières à l'écran ; F126 ne fusionne plus que le rond vert « Tout est prêt » (les ⚠ restent à part, comme le banc les écrit). Retour arrière : revert de ce commit.
