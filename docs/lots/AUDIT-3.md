# Audit, étage 3 « robustesse et chaos » : note de suivi (équipe étage 3, 2026-11-03)

Branche `fx-s3` (worktree `/home/user/fx-s3`, depuis fx-int après l'étage 2). Rien de poussé, pas d'APK. Référence : `docs/audits/AUDIT.md` §4 étage 3, §5, §6. L'étage 1 est fait par l'autre équipe.

## Ce qui a été livré
| Point de l'AUDIT | Livré | Test |
|---|---|---|
| Chaos Android (Robolectric) | tuée en arrière-plan puis reconstruite (état sauvé, projet relu entier), mémoire basse en lecture, casque débranché, appel / perte du focus (une notification ne coupe pas), mode sombre, police 200 %, droite à gauche, pliable ouvert, petit écran partagé | `audit3/ChaosAndroidTest` (7) |
| Fuzzing modèle | 4 graines × 1 500 éditions au hasard (valeurs folles : NaN, infinis, négatifs, énormes) ; invariants : pas de plantage, pas de NaN écrit, relu identique, annuler/rétablir exacts, rien de perdu, annulation complète, fichier = projet | `model/audit3/EditFuzzTest` (rejouable : `PS_FUZZ_SEED`, `PS_FUZZ_STEPS`) |
| Fuzzing écran (ViewModel) | 2 graines × 1 500 appels des écrans (cases, pads, détails de coups, tempo, parties, 303, lecture, annuler, rétablir) | `audit3/ViewModelFuzzTest` |
| Gestes au hasard | toute l'appli, 2 graines × 300 gestes (touchers, appuis longs, glissés, Retour), les 15 derniers gestes dans l'échec | `audit3/GestureFuzzTest` (`PS_GESTURE_SEED`, `PS_GESTURE_STEPS`) |
| Fichiers piégés | .zip de projet : chemins « ../ », absolus, antislash, bombe (flux de 260 Mo de zéros), project.json énorme, tronqué, pas du JSON, doublon, imbrication 200 000 niveaux, valeurs folles ; .zip de pack de sons : « ../ », bombe ; WAV menteurs (fréquence 0/100 Hz/négative, 0 ou 9 canaux, données plus longues que le fichier, tronqué) ; décodeur à 0 canal | `model/audit3/TrappedProjectFilesTest`, `audit3/TrappedSoundFilesTest`, hôte `sample_resample` |
| Écriture atomique | coupure à chaque pas de la sauvegarde (temp à moitié, vide, copie de secours à moitié, avant le renommage, après) ; sauvegarde qui échoue ; export coupé (erreur du moteur, exception) ; séparation qui échoue après le modèle | `model/audit3/AtomicSaveTest`, `audit3/AtomicExportTest`, `SeparationServiceTest::aFailureAfterTheModelLeavesNoHalfSong` |
| Mutations | PIT 1.17 lancé à la main (hors Gradle, jars dans le bloc-notes) sur `ProjectStore`, `History`, `ProjectSession`, `TapTempo`, `AutoTempo` : **185/261 mutants tués (71 %) avant, 211/261 (81 %) après** ; mutations à la main sur les 3 corrections d'écriture atomique (chaque test tombe sans la correction) et sur `hitPad` (le fuzzing tombe) | `model/audit3/MutationKillersTest` (14) |
| Décision du jour : « Couches » | réglage Pro dans la fiche du pad : ajouter un son pour les frappes fortes (3 au plus), seuil réglable, retirer ; modèle `PadSample.layers` (à la fin, absent du fichier quand vide), moteur `setPadLayer` en direct et à l'export, annuler, sauvegarde, .zip, sons manquants | `audit3/PadLayersTest` (3), `AppContractsTest` (la liste d'exceptions ne contient plus `setPadLayer`), capture `screenshots/pad_sheet_layers.png` (regardée) |

## Fiches de défaut

### Grave
1. **Import d'un son au format menteur : boucle sans fin** (zone : import ; cause racine : en-tête non vérifié). Un décodeur qui annonce 0 canal faisait tourner `FoldingSink.appendFolded` à l'infini (import jamais fini, processeur à 100 %). **Corrigé** : `AudioDecoder.checkFormat` (4 000 à 768 000 Hz, 1 à 32 canaux) et garde dans `appendFolded`. Test : `TrappedSoundFilesTest::aDecoderFormatWithNoChannelOrAnImpossibleRateFailsAtOnce` (avant : délai dépassé).
2. **Fréquence d'échantillonnage menteuse : gigaoctets demandés au moteur** (zone : moteur, priorité 1 ; cause : en-tête). Un WAV de projet (.zip importé) à 100–200 Hz faisait allouer jusqu'à ~2 Go par `Sample::create` avant que libsamplerate refuse (tueur de mémoire). **Corrigé** : `Sample::create` refuse un rapport hors 1/256..256 avant toute allocation ; `FloatWav` refuse les fréquences hors plage. Tests : hôte `sample_resample`, `TrappedSoundFilesTest::lyingFloatWavsAreRefused`.

### Moyen
3. **Séparation qui échoue après le modèle : des centaines de Mo laissés** (cause : nettoyage manquant). Une erreur à l'analyse tempo/tonalité, au renommage ou au fichier méta laissait `<clé>.part` jusqu'à la séparation suivante. **Corrigé** (`SeparationService.separateInto` + nettoyage). Test : `SeparationServiceTest::aFailureAfterTheModelLeavesNoHalfSong`.
4. **Export qui lève une exception : WAV à moitié laissé** (dans le cache, effacé une heure plus tard seulement). **Corrigé** (`renderMix`). Test : `AtomicExportTest`.
5. **Sauvegarde qui échoue (téléphone plein) : `project.json.tmp` laissé**. **Corrigé** (`ProjectStore.save`). Test : `AtomicSaveTest::aSaveThatFailsLeavesNoTemporaryFile`.

### Mineur
6. `hitPad` hors plage (-1, 16) devenait le pad choisi ; le toucher de case suivant plantait (trouvé par le fuzzing, graine 7, pas 172 ; aucun chemin connu dans l'appli, le MIDI passe ailleurs). **Corrigé**. Test : `ViewModelFuzzTest::aPadOutOfRangeIsIgnored`.
7. Nom de projet de 5 000 caractères gardé à l'import d'un .zip fabriqué. **Coupé à 40** comme un renommage. Test : `TrappedProjectFilesTest::crazyValuesAreCleanedOnImport`.
8. Banc de test : `NativeEngine.stop`, `cpuLoad`, `clipStates` appelaient le natif sous le moteur de remplacement des tests (plantage du banc seulement). `isNative` / `cpuLoadNow` / `clipStatesNow`.

### Vu, gardé tel quel
- Force d'un coup hors 0..1 lue d'un fichier : gardée (choix existant, « any strength already saved stays ») ; le moteur la borne (`SetDrumHit`), l'export MIDI aussi.
- « ⟳ Partie » et réglages Live non annulés par « Annuler » : voulu (`keepListenChoice`).
- Fuites du banc Robolectric : AndroidX garde la première Application dans `AndroidViewModelFactory` ; les écrans d'un 2e test lisaient les fichiers du 1er. Sans effet sur le téléphone (une seule Application) ; les tests d'activité de l'étage 3 l'oublient avant chaque test (explique « le harnais ne survit pas à la reconstruction » noté à l'étage 2).

## Mutants survivants restants (50/261)
fsync du fichier, de la copie de secours et du dossier (durabilité : invisibles à un test unitaire), minutage des coroutines d'autosauvegarde (délais), bornes exactes en virgule flottante (`needsFit` 0,05, `SNAP` 0,06), comparaisons avec NaN (équivalentes), `commitQuietEdits` de la session sans test dans le lot choisi (couvert dans l'appli par `RecordWithoutFearTest`). Rejouer : voir « Commandes ».

## Commandes
- Fuzzing long : `PS_FUZZ_STEPS=20000 ./gradlew :project-model:test --tests '*EditFuzzTest'` ; une graine : `PS_FUZZ_SEED=…`.
- Gestes : `PS_GESTURE_SEED=… PS_GESTURE_STEPS=2000 ./gradlew :app:testDebugUnitTest --tests '*GestureFuzzTest'`.
- PIT : classpath de test de project-model (tâche imprimée par un script d'init) + `pitest`, `pitest-entry`, `pitest-command-line` 1.17.0, `commons-text`, `commons-lang3` ; `MutationCoverageReport --targetClasses … --targetTests …` (1 min 30 s). Pas de plugin Gradle ajouté (Gradle 9 non garanti par gradle-pitest-plugin).

## Décisions à recopier dans DECISIONS
- 2026-11-03 · « Couches » : jusqu'à 3 sons de plus par pad (`PadSample.layers`), chacun dès une force (2..127, nouvelle couche dès 100) ; le son du pad garde les coups doux ; un pad calé au tempo joue sa copie seule. Raison : décision du jour, moteur déjà prêt. Retour : retirer `layers` et `PadLayers` de la fiche.
- 2026-11-03 · Un son qui annonce une fréquence hors 4 000..768 000 Hz ou 0 canal est refusé (« format audio illisible ») ; le moteur refuse un rapport de rééchantillonnage hors 1/256..256 avant d'allouer. Raison : défauts graves 1 et 2. Retour : `AudioDecoder.checkFormat`, `Sample::create`.
- 2026-11-03 · PIT lancé à la main hors Gradle, pas de plugin dans le build. Raison : rien à maintenir dans le build, Gradle 9. Retour : sans objet.

## CI et appareil
- **Émulateur** (`audit-emulator.yml`, `scripts/audit-emulator.sh`, étape 4b ajoutée) : appel téléphonique en lecture (`adb emu gsm call`), économie de batterie, micro retiré (Android tue l'appli) puis réouverture, stockage plein (~30 Mo libres) en lecture puis retour en arrière-plan, tuée en lecture. Pas encore lancé.
- **Téléphone de Benjamin uniquement** : casque Bluetooth et USB branchés/débranchés en direct, MiniLab branché à chaud et un clavier inconnu (pas de MIDI dans Robolectric ni l'émulateur), changement de fréquence de sortie (casque BT à 44,1 kHz), mode performance soutenue et tueur d'arrière-plan MagicOS, lecture écran éteint 10 min, et à l'oreille les « Couches » (frappe douce / forte sur le MiniLab).

## Ce qui reste
- Couches : choisir la couche depuis « Tous les sons… » (aujourd'hui : la liste rapide du pad) ; pas de couches sur un pad calé au tempo.
- `commitQuietEdits` et `redo` de `ProjectSession` à faire entrer dans le lot de tests de PIT ; exports et pads côté appli non mutés (Robolectric trop lent pour PIT).
- `ZipPack` : les entrées ignorées sont quand même décompressées (une bombe de fichiers non audio coûte du temps, pas de disque) ; Mineur.
