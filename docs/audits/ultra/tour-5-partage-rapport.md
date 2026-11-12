# Lot « partage » : rapport de l'auteur

Date : 2026-11-10 · branche `eq/tour-5-partage` · un seul auteur, aucun sous-agent · rien n'est poussé.
Étape 2 du plan d'action (`PLAN-ACTION-AUDITS.md`), premier lot : le chantier 1 de l'audit de prise en main (« partager et retrouver en 3 gestes »), les défauts T5-ZIP, T5-ALERTE et T5-8MES, et le test de fumée sur émulateur.

## En trois lignes

1. Dans « Prêt à partager ? », on voit et on règle la longueur de ce qui part, en Simple comme en Pro. « Mes fichiers » est juste sous le bouton Partager, et il liste aussi le .zip du projet et les vidéos.
2. Les trois défauts d'export sont corrigés, chacun avec sa garde : le .zip absent de la liste (par les deux boutons), l'alerte « le morceau a changé » à tort, les 8 mesures au lieu de 16.
3. Le test de fumée rouge sur émulateur venait du test, pas de l'appli : il lisait la mauvaise piste. Il est corrigé, mais pas rejoué ici (pas d'émulateur).

Tout ce qui touche un appareil est prouvé sous Robolectric seulement : c'est « corrigé (à confirmer sur appareil) ». Les essais à demander à Benjamin sont listés à la fin.

## 1. Chantier 1 : partager et retrouver en 3 gestes

| Quoi | Ce qui a changé | Test qui le prouve |
|---|---|---|
| La longueur dans « Prêt à partager ? » (G1) | Sans morceau, la feuille montre « Longueur de la boucle » avec les puces 4, 8, 16, 32. C'est le même réglage que dans Mix. La durée affichée suit tout de suite. En Simple, l'onglet est la même feuille. | `SharePathTest#theSheetsChipsSetTheLoopLengthAndTheDuration` : depuis l'accueil du Mode simple, 3 touchers, 0:16 → 0:32 → 0:08 |
| La longueur sortie d'« Avancé » dans Mix (G1) | « Longueur de la boucle » est sous la puce « La partie en boucle ». La phrase « la longueur se règle dans Avancé » a disparu. | `SharePiecesTest#theMixCardShowsTheLoopLengthWithoutAvance` |
| « Mes fichiers » sous le bouton Partager (G2) | L'ancien « Mes exports » s'appelle « Mes fichiers ». Il est dessiné comme une ligne qui s'ouvre (dossier, titre, sous-ligne, flèche), juste sous Partager, visible sans défiler, dans les deux modes. | `SharePathTest#myFilesIsInSightUnderTheShareButton` ; `NavigationMapTest`, chemin « Mes fichiers » (3 touchers, il en fallait 4) |
| « C'est prêt » mène au fichier (G2) | La fenêtre a « Voir mes fichiers » à côté de OK. La même fenêtre s'ouvre après le .zip : « Projet sauvegardé : Nom.pocketstudio.zip ». | `Tour5AppProofTest#myFilesListsAProjectZipSavedFromMyProjects` (ce que dit la fenêtre après le .zip) ; captures `partage/ready_song`, `partage/ready_zip` |
| Le .zip listé (G2) | Voir T5-ZIP plus bas. Les vidéos sont listées aussi. Un .zip porte le mot « projet », une vidéo le mot « vidéo ». | `Tour5AppProofTest#myExportsListsTheProjectZipJustSent`, `#myFilesListsAProjectZipSavedFromMyProjects` |
| Le message vide ne contredit plus (G2) | « Rien ici pour l'instant. Tout ce que tu partages s'ajoute ici, dans Musique › BenStudio sur cet appareil. » Le bouton de chaque fichier dit « Renvoyer ». | capture `partage/my_files_empty` ; `Round3Zone2Test#myExportsAreListed` |
| Le mot « Partager » (réponse 1A) | Onglet du Mode simple « Partager », « Fini ? Partage ton morceau », « Sauvegarder le morceau en .zip » (feuille, Mes projets › ⋯, Réglages), « Autres fichiers », la visite rapide. Textes ajoutés dans les 14 autres langues (zone `partage` des catalogues). `docs/DESIGN.md` § 10 mis à jour. | `I18nCatalogTest` (toutes les langues, aucune clé orpheline) ; `LanguageTest`, `TextsAuditTest` |

Les quatre premiers tests ont été vus rouges sur le code d'avant (commit `49e6c4d`), puis verts (`d4ab90c`).

### Ce qui n'est pas fait dans le chantier 1

- **« C'est prêt » seulement au retour du menu d'Android** : non. La fenêtre s'ouvre dès que le menu d'Android est ouvert et attend dessous ; c'est bien elle qu'on trouve au retour. La retarder vraiment demande que le banc sache jouer la pause puis le retour de l'écran, ce qui est une modification du banc, hors de ce lot (ligne dans DECISIONS).
- **La vraie ligne-lien partagée** (`LinkRow`) : c'est le lot suivant. « Mes fichiers » imite ses voisines en attendant.
- **Le reste du lexique** : la carte « Exporter » de Mix et ses boutons, « Envoyer les bouts », « Envoyer le fichier brut (.zip) » d'un projet illisible, et le test qui interdit ces mots. C'est le chantier 3.
- **« Autres fichiers » sans changer de mode** : en Simple, la ligne dit encore « (passe en mode Pro) », parce que c'est encore vrai. C'est le chantier 5.
- **Le test `NavigationMapTest` « longueur d'export »** : la carte de navigation ouvre un projet qui a un morceau, donc sans puces de longueur. La preuve « 3 touchers en Simple » est dans `SharePathTest`, avec un projet sans morceau.

## 2. T5-ZIP : le .zip du projet dans la liste, par les deux boutons

- **Cause.** Le .zip restait dans le cache de l'appli (effacé au bout d'une heure) et la liste ne lisait que le dossier Musique.
- **Correction.** `ProjectsViewModel.exportZip` copie le .zip dans Documents › BenStudio, et « Mes fichiers » lit ce dossier. Pas dans Musique : la collection Musique d'Android ne prend que du son.
- **Un seul chemin.** Les deux boutons (« Prêt à partager ? » et Mes projets › ⋯) passent par le même objet, `ProjectZipSender`. Le bloc écrit à la main dans `AppRoot` a disparu.
- **Gardes.** Premier chemin : `Tour5AppProofTest#myExportsListsTheProjectZipJustSent`, qui était `@KnownDefect("T5-ZIP")` ; avec la correction elle a dit « no longer fails », la marque est retirée. Second chemin : `Tour5AppProofTest#myFilesListsAProjectZipSavedFromMyProjects`, vue rouge (`dbff2a5`) puis verte (`f3c283e`).
- **État : corrigé (à confirmer sur appareil).** La preuve utilise un faux MediaStore. Sur un vrai téléphone, il reste à voir que Documents accepte le .zip et que la liste le retrouve.

## 3. T5-ALERTE : plus d'alerte « le morceau a changé » juste après un partage

- **Cause.** Partager coche une carte du Guide, et le Guide entrait dans l'empreinte de l'export. Et un export MIDI ne posait pas son empreinte.
- **Correction.** Le Guide sort de l'empreinte (`ExportUi.madeHash`). L'export MIDI pose la sienne.
- **Gardes.** `Tour5AppProofTest#noChangedWarningRightAfterSharing` et `#noChangedWarningRightAfterAMixExport`, toutes deux `@KnownDefect("T5-ALERTE")` avant ; elles ont dit « no longer fails », les marques sont retirées (`2e6e828`).
- **État : corrigé.** Sur le téléphone, tel-07 était déjà ✅ le 8 novembre ; l'essai « MIDI puis retour » n'a jamais été fait.

## 4. T5-8MES : 16 mesures demandées, 16 exportées

- **Cause.** « Partager » recalculait sa propre longueur (8 mesures au moins) au lieu de prendre celle choisie. Tout est en Kotlin, rien dans le moteur : pas de test hôte à ajouter.
- **Correction.** `ShareSong.settings` prend la longueur choisie, et la feuille affiche celle que Mix affiche.
- **Garde.** `Tour5AppProofTest#shareFollowsTheLengthChosenInMix`, `@KnownDefect("T5-8MES")` avant ; « no longer fails », marque retirée (`0d049be`).
- **État : corrigé (à confirmer sur appareil).**

## 5. Le test de fumée sur émulateur

- **Ce que disent les journaux de la CI 247** (artefact `smoke-test-report`, journal du job) : « take: 4 notes; Prise keys : 4 touches, 4 notes, 0 coups », puis « in time: [] » et l'échec `JourneyTest.kt:305`. L'appli a bien gardé les 4 notes.
- **Ce que dit le code.** Le parcours lance la prise morceau arrêté : aucune autre partie n'est entendue, ce n'est donc pas le cas de T4-26. Mais en Mode simple, la première prise au clavier crée la piste « Ma mélodie » et la sélectionne au moment où elle démarre. Le test notait la piste sélectionnée **avant** le rond rouge, puis lisait cette piste-là : vide.
- **Verdict : le test lisait la mauvaise piste.** Ce n'est pas un défaut de l'appli.
- **Correction du test.** Il lit la piste sélectionnée après la prise. Aucune attente à l'aveugle, aucun nouvel essai. S'il échoue encore, il écrit une sonde : les notes nouvelles, piste par piste.
- **Preuve sans émulateur.** `Tour4AppProofTest#aFirstMelodyTakeGoesToMaMelodieNotToTheTrackSelectedBefore` (vert) : la piste d'avant ne reçoit aucune note, « Ma mélodie » reçoit les trois.
- **Limite.** Pas d'émulateur ici : le test corrigé n'a pas été rejoué. Il compile. La prochaine CI dira s'il passe ; s'il est encore rouge, la sonde dira où sont les notes.
- T4-26 reste « non-tranché » : la question « quelle partie reçoit les notes » attend toujours l'essai n° 5 de Benjamin.

## 6. Les captures regardées

Nouvelles, dans `app/screenshots/partage/` : 18 images (6 écrans, sur téléphone à 100 %, téléphone à 130 %, tablette Tab A11 à 130 %). J'en ai regardé 10, celles du tableau ; les 8 autres sont dites dessous.

| Capture regardée | Ce que j'ai vu |
|---|---|
| `loop_simple` (téléphone 100 %) | Page du Mode simple : durée 0:16, « Longueur de la boucle » avec 4 puces dont 8 allumée, le message « ta partie part en boucle (8 mesures) », puis en bas les puces M4A / WAV, le gros bouton Partager et dessous « Mes fichiers » avec son dossier et sa flèche. Rien de coupé. |
| `130_loop_simple` (téléphone 130 %) | Mêmes éléments. Avant retouche, la légende « une partie en boucle » passait à la ligne et collait au chiffre (« 0:16boucle ») : corrigé, elle a sa colonne. « Modifier le morceau » est coupé au bord de la zone qui défile : il y a une suite, ce n'est pas un défaut. |
| `tabA11_130_loop_simple` (tablette 130 %) | Tout tient : le titre sur une ligne, les quatre puces côte à côte, « Mes fichiers » et sa sous-ligne sur une ligne sous Partager. |
| `loop_pro` (téléphone 100 %) | Feuille Pro : 0:32, la puce 16 allumée, « Volume visé » dessous, le message dit 16 mesures. |
| `130_my_files` (téléphone 130 %) | Quatre fichiers. Le nom long passe sur deux lignes puis finit par « … ». Le .zip porte « projet », la vidéo « vidéo ». « Renvoyer » reste entier. Les lignes étaient serrées : un peu d'air ajouté ensuite. |
| `tabA11_130_my_files` (tablette 130 %) | La même liste, aérée, les noms entiers sur deux lignes au plus. |
| `tabA11_130_my_files_empty` | « Rien ici pour l'instant. Tout ce que tu partages s'ajoute ici… » et « Fermer ». |
| `130_ready_song` (téléphone 130 %) | « C'est prêt », le nom du fichier, la durée et le niveau, puis « Voir mes fichiers » et « OK » sur la même ligne, entiers. |
| `130_ready_zip` (téléphone 130 %) | « Projet sauvegardé : Nuit-sur-la-ville-….pocketstudio.zip » (le nom coupé en deux lignes), la ligne sur la copie dans Documents, les deux boutons entiers. |
| `tabA11_130_ready_zip` (tablette 130 %) | La même fenêtre, le nom sur une ligne. |

Pas regardées une à une (mêmes écrans, autres tailles) : `130_loop_pro`, `tabA11_130_loop_pro`, `my_files`, `my_files_empty`, `130_my_files_empty`, `ready_song`, `tabA11_130_ready_song`, `ready_zip`.

Captures existantes changées et regardées :

| Capture | Ce que j'ai vu |
|---|---|
| `nav/simple_send` (téléphone 100 %) | L'onglet du bas dit « Partager ». « Autres fichiers », puis les puces, Partager et « Mes fichiers » avec sa sous-ligne, au-dessus de la barre du Guide. |
| `scale130/fr_nav_simple_send` (téléphone 130 %, l'appli entière) | **Défaut vu puis corrigé.** Avec « Mes fichiers » sous le bouton, la durée sortait de l'écran. Maintenant, quand la page est courte, le titre tient sur une ligne et « Mes fichiers » ne garde que son nom : on voit le nom, la durée 0:06, Écouter, les puces, Partager et « Mes fichiers ». |
| `nav/sheet_share` (Pro 100 %, l'appli entière) | La feuille avec « Autres fichiers », « Sauvegarder le morceau en .zip » (sa sous-ligne coupée au bord de la zone qui défile), puis Partager et « Mes fichiers ». Tout est lisible. |
| `tabA11/130_nav_sheet_share` (tablette 130 %) | La même feuille : « Mes fichiers » et sa sous-ligne sur une ligne sous Partager, rien de coupé hors de la zone qui défile. |
| `share_sheet` (la feuille avec ses alertes) | Solo, piste coupée, saturation avec leurs boutons, puis Partager et « Mes fichiers ». |

Les autres images changées (environ 55) ne diffèrent que par le mot de l'onglet du bas (« Envoyer » → « Partager ») ou par la ligne « Cinq onglets : … Partager » de la page Plus : vérifié par programme (la zone qui change est celle du mot), pas regardées une à une ; le mot de l'onglet se voit sur `nav/simple_send`. Les variantes en néerlandais et couchées n'ont pas été regardées non plus.

## 7. Ce que j'ai touché en plus, et pourquoi

- **`Tour5AppProofTest`** : chaque composition a son propre recomposeur. Sans cela, dès qu'une preuve avait composé un écran, les effets de la suivante ne tournaient plus (la fenêtre de partage ne prenait pas le morceau). L'ordre des tests l'avait caché ; ma nouvelle garde l'a fait apparaître.
- **`Round3Zone2Test#twoFilesLeaveTogether`** : il ne passait que s'il était le premier de sa JVM à partager un fichier. Il vide maintenant le cache de `FileProvider`, comme `BenchShareTest`.
- **`BenchShareTest`** : il touchait l'onglet par son mot « Envoyer » ; il le touche par son étiquette de test. Aucune règle du banc n'est changée.
- **`scripts/bench/mutation-l1.sh`** : sort en code 3 avec un message. Les deux défauts qu'il simulait sont corrigés, son correctif ne s'applique plus.
- **Recensement des écrans** (`recensement-acceptes.tsv`) : les nouvelles captures y entrent avec leur raison. **Un plafond a été relevé**, `NavScreenshotTest.sheetShare` CONTRAST de 7 à 12, alors que l'en-tête du fichier dit qu'un plafond ne monte jamais. Raison écrite dans la ligne : c'est le même artefact qu'avant (des textes lus à travers le voile de la feuille : « 0:06 » orange sur fond sombre lu à 1,45:1), et la feuille montre maintenant plus de lignes. La capture montre que tout est lisible. **À faire valider par le relecteur.**

## 8. Données

- `verdicts.tsv` et `base.tsv`, par `marque-corrige.sh` : T5-Cc-27 (8MES), T5-Cd-68, T5-Cc-41a et T5-Cd-83 (ZIP), T5-Cc-30 et T5-Ec-61 (ALERTE) passent « corrigé », avec leur commit. Leurs preuves sont réécrites (gardes ordinaires) et leur `garde_id` vidé. La ligne T4-26 dit ce qui a été trouvé sur le test de fumée ; son verdict ne change pas.
- `docs/DECISIONS.md` : neuf lignes ajoutées à la fin.
- `docs/DESIGN.md` § 10 : l'onglet « Partager », le mot « Partager ».
- `app/src/main/assets/changelog.txt` et `docs/PROGRESS.md` : pas touchés.

## 9. Résultats des contrôles

Passés sur la tête `711e1b0` (ce rapport est le seul commit après).

| Contrôle | Résultat |
|---|---|
| Suite unitaire complète et captures, en un seul passage : `:app:testDebugUnitTest :app:verifyRoborazziDebug` (plus `:project-model:test` et la compilation des tests sur appareil), 32 min | 8 118 tests, **4 rouges**, 4 479 sautés. Aucun des quatre ne vient de ce lot, détail dessous. Catalogues : 1 074 tests, 0 rouge. Les tests sur appareil compilent. |
| Les 4 rouges | (1) `ScreenshotTest#recette` : l'image du dépôt est en retard sur la liste de recette (elle montre « 18 essais », la page dit « les 10 essais restants »). (2) et (3) `CutScreenshotTest#cutScreen` et `#cutScreenMagnifier` : des pastilles de bouts allumées ou non selon l'ordre des tests. Ces trois images étaient déjà différentes avant mes changements et je ne les ai pas réenregistrées (hors lot). (4) `BugReportFlowTest#shareWritesTheZipAndOpensAndroidsShareMenu` : dépendait de l'ordre des tests (cache de `FileProvider`) ; **corrigé** ensuite et rejoué vert avec les autres tests de partage (77 tests, 0 rouge). |
| La suite complète n'a pas été relancée après cette dernière retouche de test | Seules les classes touchées l'ont été (77 tests verts), puis le selftest du banc. |
| `python3 -I scripts/bench/test_ultra_bench.py` | `ultra bench scripts: OK` |
| `scripts/bench/selftest.sh` (dossier `lot-partage-final`) | vert : 151 verts, 0 rouge, 0 en quarantaine, 41 sautés (les sauts connus) |
| `verifie-chantier.py --resume …/selftest-resume.txt` | **24 écarts** (22 avant). Les 2 nouveaux sont attendus : « vert sans rouge du même test avant lui » pour `0d049be` (T5-8MES) et `2e6e828` (T5-ALERTE). Leur rouge est la garde `@KnownDefect` qui existait déjà, comme le voulait la consigne ; le juge ne connaît que les paires de commits. Les 22 autres sont inchangés. Les deux paires rouge / vert de ce lot (`49e6c4d` / `d4ab90c`, `dbff2a5` / `f3c283e`) sont reconnues. |
| `git ls-tree -r HEAD \| grep -c '^120000'` | 0 |

## 10. Classes à rejouer à la fusion

`Tour5AppProofTest`, `Tour4AppProofTest`, `SharePathTest`, `SharePiecesTest`, `ShareScreenshotTest` (et ses deux variantes), `NavigationMapTest`, `Round3Zone2Test`, `ShareSheetTest`, `ShareSongTest`, `BenchShareTest`, `ScreenScanTest`, `NavScreenshotTest` (et ses variantes), `ScreenshotTest`, `LanguageTest`, `TextsAuditTest`, `ProjectsSafetyTest`, et côté catalogues `:project-model:test` (`I18nCatalogTest`, `LexiconWordsTest`). Sur la CI : le job « Emulator smoke test ».

## 11. Essais à demander à Benjamin plus tard (pas maintenant)

1. **Le .zip retrouvé (tel-03 refait).** Mode simple, onglet Partager, « Sauvegarder le morceau en .zip », choisir une appli ou fermer le menu. Au retour : la fenêtre « C'est prêt » dit-elle « Projet sauvegardé » ? « Voir mes fichiers » montre-t-il le .zip avec le mot « projet » ? Même essai depuis Mes projets › ⋯.
2. **La longueur (tel-06 refait).** Projet sans parties dans Structure, 120 BPM. Onglet Partager : toucher « 16 » sous « Longueur de la boucle », puis Partager en WAV. Le fichier dure-t-il 32 secondes ?
3. **Le MIDI (essai § 8 n° 4).** Mode Pro, Mix, exporter en WAV, changer un volume, exporter les notes en MIDI : l'alerte « le morceau a changé depuis cet export » apparaît-elle sous le fichier MIDI ?
4. **À 130 % sur le téléphone.** L'onglet Partager montre-t-il bien la durée sans défiler, et « Mes fichiers » sous le bouton ?
5. Toujours en attente, pas de ce lot : l'essai n° 5 (Clavier en Simple, enregistrer sur la partie A pendant que la partie B joue).

## 12. Ce dont je ne suis pas sûr

- **Documents › BenStudio sur un vrai téléphone.** Je n'ai pas pu vérifier qu'Android accepte le .zip dans ce dossier ni que la liste le relit après coup. C'est la raison du « à confirmer sur appareil ».
- **Le test de fumée.** Le diagnostic s'appuie sur les journaux et le code ; la correction n'a pas tourné sur un émulateur.
- **Le plafond relevé du recensement** (section 7).
- **Les traductions.** Les textes des 14 langues sont de moi, sans relecture par quelqu'un qui parle ces langues. Les tests vérifient les clés, les guillemets et les « %s », pas le sens.
- **Trois images rouges au contrôle des captures, hors lot** : `recette.png` (image en retard sur la liste de recette) et `cut_screen.png` avec sa loupe (pastilles allumées selon l'ordre des tests). `remix_stem_edit_menu.png` a aussi changé une fois sur trois enregistrements. Je ne les ai pas réenregistrées : à traiter par qui tient ces écrans.
- **La suite complète n'est pas verte à 100 %** à cause de ces trois images ; tout le reste l'est.
