# Audit ultra, tour 3 : zone 5 (tutos, aides, sons, parties, synthé)

Items 28 à 36 de la synthèse. Tests sans le banc, chacun rouge sur le code d'avant (passe « avant » : 13 tests, 13 échecs ; captures du code d'avant restaurées ensuite).

| N° | Verdict | Gravité | Correction | Test |
|---|---|---|---|---|
| 28 ×4 (3e fois) | appli | grave | Racine : la carte « Le kit … » prenait la place du panneau du tuto quand le catalogue arrivait ; la touche des sièges venait des secondes après, donc la garde d'1 s du tour 2 ne pouvait rien. Le kit est maintenant un encadré à la fin du texte de l'étape, dans le panneau : le panneau, ses mots, « Lire la suite » et « Écouter l'exemple » ne bougent jamais. `ArrivalGuard` retiré. | `TutoKitUnderFingerTest` (A c : fr, 200 %, « Lire la suite » ; E b : nl, tablette, « Luister naar het voorbeeld » joue) ; `TutoKitTest`, `TutoBigFontTest`, `TutoKickSoundTest` mis à jour |
| 29 ×3 (revient) | appli | grave | Pourquoi ça revenait : le tour 2 traduisait le nom du kit (« Kick » → « Grosse caisse 1 »), mais le pad montre le son posé dessus (« Kick studio », un son de pack). Le tuto dit le nom que montre le pad, entre guillemets de toute langue, Simple et Pro (`tutoPadNames`, `QuotedNames`, particules coréennes) ; la carte « Son du pad » dit aussi ce nom. | `TutoPadNamesTest` (fr Simple, nl Simple, fr Pro, tr Simple) |
| 30 ×2 | appli | moyen | « Bravo, chapitre fini ! » seulement si toutes ses étapes sont faites ; sinon « il reste l'étape N » ; « Écouter mon morceau » à la fin du tuto. L'étape 1 non comptée : « Suivant » touché 1 s après « Écouter » (moins de 2 s entendues) : comportement voulu. | `TutoChapterEndTest` |
| 31 ×3 | non fait | moyen | Jargon des cours Facile (Dorien, 909, kick…) : 37 cours × 15 langues à réécrire ; reste. | — |
| 32 | appli | grave | Racine du message français dans l'appli allemande : le test des catalogues ne voyait pas les appels écrits `com.pocketstudio.model.i18n.tr(…)` ; 13 textes du tuto (« Cette étape n'est pas encore faite… », « Pas dans l'ordre… », « Partie créée… ») restaient sans traduction. Test corrigé, 13 × 14 traductions (« Suivant » = le mot du bouton de chaque langue). | `I18nCatalogTest.everyTextHasItsTranslations` |
| 33 ×2 | appli | grave | La recherche de « Changer le son » ignorait le kit intégré, les accents et les écritures non latines : elle cherche maintenant le kit (noms Pro et mots de tous les jours), « Fête », les packs (familles traduites). Exemples du champ « caisse, conga, sirène », trouvables sans pack dans les 15 langues. « Autres sons du kit » dans son propre dossier : l'onglet d'une famille ne montre que sa famille. | `SoundSearchWordsTest` (exemples × 15 langues × Simple/Pro, caisse/davul/trampet/スネア…, saisie réelle et onglet « Caisses claires ») ; `ShelfBrowserTest`, `LibraryChoicesTest` mis à jour |
| 34 | appli | moyen | Aides vraies : étoile « Défi » (pas « la cible ») ; ambiance = boutons Couleur/Saleté/Mouvement/Espace (+ parfois des effets) ; Place/Hauteur se règlent par le curseur sous la grille (ramené à l'écran quand on touche un coup) ; « Pas » propre au mode Simple ; colonne Remix citée seulement quand le Live l'a. Étiquettes « ? » : « Annuler (défaire) » sur la flèche (Cofnij), « Jouer »/« Arrêter » sur la lecture, onglets de la barre étiquetés en premier (Mix et Plus n'en avaient pas). | `HelpLabelsTest.everyTabOfTheBarIsLabelledBesideIt…` (pl, Pro) ; aides : relues sur l'écran (texte seul) |
| 35 | appli | grave | « Renommer » dans la feuille d'une partie (Drop, Fin…, nomme sa brique). | `SongArrangementTest.aPartIsNamedFromItsOwnSheet` |
| 36 | non fait | moyen | Synthé sur pliable, mise à jour d'un son gardé, piste avec un favori : reste. | — |

Traductions : `ultra3z5.json` dans les 14 langues ; 7 anciennes phrases retirées des catalogues.

## Reste
- 30 : une nouvelle étape s'ouvre au milieu du texte (A c 20) : non reproduit en test (le texte ne déborde pas sous Robolectric), pas corrigé.
- 31 : jargon des cours Facile ; 32 : noms du tuto Pro ≠ écran (Acid 303/Cutoff, « Drop », « Intro 2 »), « Montre-moi » non validé, B♭/A#, kit 909 annoncé ; 34 : « ■ Tout arrêter » (verdict banc zone 1), aide « Pages et packs » qui promet « Annuler » ; 35 : liste de 27 parties d'1 mesure, « Seulement celle-ci » qui copie sans demander, champ mesures ; 36 entier.
- Sous Robolectric un dialogue avec un champ ne revient jamais au repos (verdict banc n° 16) : le test de 35 s'arrête au bouton « Renommer ».
- Échec hors zone dans `I18nCatalogTest.dutchAndEnglishWording` (es « «%s» estaba silenciado en Mezcla », zone 1).
