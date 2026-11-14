# Audit ultra, préparation P0, équipe B (2026-11-04)

Atelier `fx-ub` (depuis `fx-int`), non poussé, non fusionné. Plan : `docs/audits/AUDIT-ULTRA.md` §1, §2, §5 bis, §6 ter, §10.

## Fait
1. **`TutoJourneyTest` en allemand et en arabe** : `-Ptuto.langs=de,ar` ajoute ces langues (français seul par défaut, noms des cas français inchangés). Premier passage : 74 cours en allemand et 74 en arabe, **148/148 verts** (13 min). La langue de l'interface est vérifiée au début de chaque cours.
2. **« Le panneau tient »** (`TutoPanelFitsTest`, 5 classes d'appareils) : pour chaque langue, ses 8 textes d'étape et titres de chapitre les plus longs, dans le panneau déplié, par paires avec les appareils et la police 100/130 % : **120 cas**. Trouvé : 66 cas où les mots défilaient sans rien pour le dire (étapes Pro « Déjà prêt pour toi… », jusqu'à 218 dp cachés). Corrigé : « Lire la suite ⌄ » sur un fondu en bas des mots quand il en reste (un toucher fait défiler) ; 14 traductions. 120/120 verts.
3. **« Cible visible »** dans `TutoJourneyTest` : à chaque étape de batterie, chaque case en pointillé entière dans la fenêtre et au-dessus du panneau (rectangle entier, pas seulement le centre) ; chaque « Copier sur les mesures suivantes » touché là où le panneau replié le montre, sans le déplier (défaut n° 2 de Clara). 74/74 verts en français : rien de caché aujourd'hui.
4. **Registre des écrans** (`ScreenRegistry`, réflexion sur les classes `*ScreenshotTest`) : **293 écrans**. `ScreenScanTest` rejoue chaque écran et compte « sans nom » (LABEL étendu aux curseurs, interrupteurs, appuis longs), « sans ? », cibles, contrastes. Détail dans `app/build/recensement/` (jamais commité) ; plafonds acceptés avec raison dans `app/src/test/resources/audit/recensement-acceptes.tsv` (510 lignes). Premier passage : **6 724 « sans ? »** sur 283 écrans, **295 « sans nom »** sur 27 écrans (256 dans la grille `allPads`), 736 cibles sur 55, 496 contrastes sur 145. Trois minutes : tourne à chaque passage.
5. **`NavigationMapProTest`** : 33 chemins depuis l'accueil Pro (5 espaces, pages de Plus, feuilles de la barre du haut, cartes de l'accueil) et depuis Pad Live comme racine à part ; ≤ 3 touchers (4 par « Continuer »), Retour jusqu'à l'accueil. 33/33 verts.
6. **StrictMode dans les tests** : application de test `StrictTestApplication` (robolectric.properties). Le StrictMode d'Android ne voit rien sous Robolectric (les fichiers sont ceux du JDK de la machine) : l'enregistreur du JDK (JFR) suit chaque test ; disque ou réseau sur le fil principal par le code de l'appli = test en échec, sauf ligne acceptée dans `app/src/test/resources/audit/strictmode-acceptes.tsv` (vide). ≈ 300 tests de 15 classes passés : aucune violation. `-Pstrict=off` l'éteint.
7. **Scie / Triangle / Carré** : en Mode simple « Brillant / Creux / Doux », en Pro les noms d'onde ; un petit dessin de l'onde (vecteur, couleur du thème) à côté du son dans le navigateur de sons, l'assistant et les puces « Forme d'onde » du synthé ; le mot reste pour TalkBack (le dessin est décoratif). Les 15 langues avaient déjà les six mots. `WaveWordsTest`.
8. **Mise à jour, réinstallation, premier lancement** :
   - `UpgradeFromReleasesTest` : v0.1.23, v0.9.95, v0.22.173, v0.22.175, audit-1. **Pas d'APK ancienne construite** (trop lourd ici) : chaque projet est écrit par le code du modèle de l'étiquette lui-même (`scripts/upgrade-fixtures/make.sh` compile `project-model` à l'étiquette avec le compilateur Kotlin déjà téléchargé par Gradle, puis `Gen1.kt`) ; les réglages sont écrits à la main d'après les clés de `Settings.kt` à l'étiquette. Tout s'ouvre tel quel, réglages gardés, annulation vivante, resauvé puis relu pareil.
   - `RestoredPhoneTest` : seules les copies de la sauvegarde Android sur un nouveau téléphone. Trouvé : l'appli ne disait rien. Corrigé : un message une fois, au premier lancement (« Projets retrouvés dans la sauvegarde du téléphone : 2. Leurs sons n'y étaient pas… », 14 traductions).
   - `FirstLaunchTest` : 23 langues de téléphone (les 15, plus pt-PT, zh-TW, en-IN, in-ID, fr-BE, de-AT, es-MX, sw-KE) : langue adoptée, écran d'accueil dans cette langue, rien en français.
9. **`recette.json`** : la liste téléphone ultra (24 points, chacun finit par un chiffre ou une capture à recopier ; les 3 points MiniLab marqués reportés à l'achat d'un clavier).
10. **Carte des fonctions** : 15 cases remplies (14 « Access. » par `ScreenScanTest::census`, « Tailles » des tutos par `TutoPanelFitsTest::panelFits`) : 25 vides, plafond de `CarteFonctionsTest` baissé à 25.

## Décisions (à recopier dans DECISIONS.md)
- 2026-11-04 · StrictMode des tests par l'enregistreur du JDK (JFR) plutôt que par `android.os.StrictMode`, qui ne voit rien sous Robolectric. Une lecture faite par un test lui-même n'est pas comptée ; celle de l'appli passée par Android ou Compose l'est. Retour arrière : enlever `application=` de `robolectric.properties`.
- 2026-11-04 · Recensement des écrans à chaque passage (3 min) plutôt que sous `-Paudit`, avec une marge de 2 par compteur pour l'ordre des tests (les cartes repliées sont partagées par la JVM). Retour arrière : `ORDER_MARGIN`, ou un `assume` sur `ps.audit`.
- 2026-11-04 · Les mots du panneau du tuto qui dépassent défilent avec « Lire la suite » en bas, plutôt que de raccourcir les textes. Retour arrière : `MoreWords` dans `TutoPanel.kt`.
- 2026-11-04 · Anciennes versions simulées par leur propre code du modèle (pas d'APK construite). Retour arrière : sans objet.
- 2026-11-04 · Message unique après une restauration depuis la sauvegarde Android. Retour arrière : `restoredNotice` dans `ProjectOpening.kt`.

## Restes
- « Sans ? » : 6 724 contrôles à trier une fois « évident / à expliquer » par un agent léger (§1), puis baisser les plafonds.
- « Sans nom » : 295 (visé 0 au §7) ; d'abord `allPads` (256) et `wizardMood` (5).
- `ScreenScanTest` ne lit que le dernier état d'un écran et à une seule taille (400 × 890) ; ne voit pas les `@Config` de méthode.
- « Cible visible » : les notes en pointillé du clavier (dessinées dans un canevas, sans nœud) ne sont vérifiées que par `SpotChecks` (centre de la zone).
- StrictMode : seulement ≈ 300 tests passés ici ; la suite complète (étiquette `ultra-0`) dira s'il faut des lignes acceptées.
- `FirstLaunchTest` ne joue ni la question des notifications ni l'économie de batterie ni le premier téléchargement de modèle : à faire sur l'émulateur (`audit-emulator.sh`), comme l'installation par-dessus l'APK précédente et `bmgr`.
- L'onde de l'effet LFO (« Douce / Triangle / Scie / Carrée ») n'a pas changé : c'est une forme de modulation, pas un timbre.
- « Santé du son » (XRuns, état thermique) n'existe pas encore à l'écran : la liste téléphone renvoie aux « Décrochages audio » de l'écran Recette.
