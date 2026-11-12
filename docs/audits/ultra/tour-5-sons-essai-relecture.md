# Tour 5, lot « sons-essai » : relecture neuve (2026-11-12)
Relu : 4a3bc0252a3b9965dccfe85b1514794b78f4eb7f

## Ce qui a été relu
- Les dix commits du lot, de 2ee8f348 à bbfed992 : quatre paires rouge / vert, `THIRD_PARTY.md`, le rapport et les décisions. La fusion 4a3bc025 n'apporte que des documents : `git diff 4a3bc025^1 4a3bc025` est vide hors de `docs/`.
- Le code : `SoundTrial.kt`, `StarterWizard.style`, `SoundTrialTest.kt`, `BenchFlac.kt` (`registerWavs`, `pcm16`), `BenchWavPackTest.kt`, `SoundTrialPacksTest.kt`, la clé `sons.essai` de `app/build.gradle.kts`, `trial_drums.py` et `test_trial_drums.py`.
- Je suis un relecteur neuf : je n'ai rien écrit de ce lot, et je ne modifie que ce fichier.

## Contrôles faits
- **Moteur** : `git diff 2d931c22..HEAD -- engine` est vide. Rien n'est ajouté dans le rappel audio : ni allocation, ni verrou, ni log, ni appel JNI.
- **Rendu par défaut** :
  - le seul changement du code livré est `?.let(SoundTrial::styleOf)` ; coupé, `styleOf` rend son argument, c'est-à-dire le même objet ;
  - la propriété n'est lue nulle part ailleurs : `grep` ne la trouve que dans `StarterWizard.style`, les deux tests et `build.gradle.kts` ;
  - les voies qui lisent `STYLES` ou `stylesOf` sans passer par `style()` ne construisent pas de morceau. Ce sont les puces de `NewProjectPage`, `StyleFamilies`, `SongGuide.seconds`, `ShareSong`, `DefaultNames`, `WizardAcid.Feel`, `TutoCourse` et `LibraryScreen` : elles lisent l'id, le nom, la famille ou la durée. Coupé, elles rendraient de toute façon le même objet ;
  - interrupteur mis, les voies qui font le morceau passent par `style(id)` : `quickCreate`, `styleDemo`, `soundCategories`, et `LiveSongRecorder` par `quickCreate(style.id)`.
- **Empreintes rejouées sur la base** :
  - `git diff --name-only 2cfb8a3a 2ee8f348` ne contient, hors de `docs/`, que `SoundTrialTest.kt`. Un worktree sur 2ee8f348 est donc le code de 2cfb8a3a plus ce seul test ;
  - `gradle-file.sh :project-model:test --tests SoundTrialTest` y donne `offEverySongIsTheSameToTheByte` et `offEveryStyleIsItsVeryRecipe` verts. Les 37 `BASE_HASHES` sont bien ceux du code de la base ;
  - les trois tests « on » échouent sur leurs assertions (voir la ligne « rouge constaté ») ;
  - le worktree a été supprimé.
- **Sur HEAD** (deuxième et dernier lancement de Gradle) :
  - `SoundTrialTest` : 6 tests sur 6, à jour (mêmes entrées, déjà vertes) ;
  - `SoundTrialPacksTest` : 2 sur 2 ; `BenchWavPackTest` : 1 sur 1 ; `SoundCategoriesTest` : 2 sur 2 ;
  - `HumanLucasBugsTest` : 7 tests verts, dont `aDownloadedPacksSoundsAreRead`, et 1 sauté (`drive`, qui ne tourne qu'avec `-Phuman.dir`).
- **Interrupteur** :
  - aucun `System.setProperty` dans `app/src/main` ni dans `project-model/src/main` ;
  - sur Android, rien d'extérieur ne fixe les propriétés Java d'une appli : `adb setprop` écrit dans une autre table, et l'APK de release n'accepte pas de `wrap.sh` ;
  - la lecture se fait côté Kotlin, jamais dans le fil audio. Elle coûte une recherche dans une table, à côté du parcours des 37 styles que `style()` faisait déjà : négligeable.
- **BenchFlac** :
  - appelants : `HumanTester` (`PackStore.dir`), `LiveSongRecorder` (`bench.packs`) et `BenchWavPackTest` ;
  - les vrais packs ne contiennent que du FLAC : `generate_pack.py`, `library_pack.py` et `instrument_pack.py` écrivent des `.flac`. Les WAV de l'appli (pads, prises, pistes séparées) ne sont pas sous `files/packs/<id>/` ;
  - les clés de `known` diffèrent par le chemin, donc le FLAC et le WAV ne se mélangent pas ;
  - `BenchWavPackTest` compare chaque échantillon à 1/32768 près, sur deux canaux de sinus et d'amplitudes différents, avec la fréquence d'échantillonnage et la longueur. Une tonalité ou des canaux inversés le feraient échouer ; sans `registerWavs`, le décodage échoue et `assertNotNull` rougit.
- **trial_drums.py** :
  - `python3 -I scripts/packs/test_trial_drums.py` : « 1225 checks, 0 failed » en 6,5 s, sans `__pycache__` laissé ;
  - trois versions cassées, rejouées hors du dépôt, sont toutes refusées :
    - générateur non graine : « the two builds differ » ;
    - kick techno à 45 Hz : 4 échecs sur la fondamentale ;
    - crête à 0 dBFS : 130 échecs ;
  - les graines viennent du nom de chaque son (crc32), et le module `wave` écrit du PCM au format 1, celui qu'exige `pcm16` ;
  - le script ne lit que ce qu'il a lui-même écrit : aucun son tiers ;
  - rien n'est commité : ni son, ni lien symbolique, ni `__pycache__` (`git ls-files`, modes des fichiers). `packs.yml` se déclenche sur `scripts/packs/**`, mais ses tâches ne tournent pas, puisque la Release `sons-packs-6` existe.
- **Rapport** :
  - `rapport-zone.py HEAD` : code 0, 4 filtres ;
  - chaque test d'une ligne « chemin: » existe et fait ce que le chemin dit. `aDownloadedPacksSoundsAreRead` installe des FLAC faits par `FlacEncoder` : c'est bien la voie FLAC ;
  - `SoundTrialTest`, la preuve principale, n'est pas dans la ligne « tests: », mais la CI joue tout `:project-model:test` (`ci.yml`, l. 156) ;
  - « Aucune hausse au-delà du bruit » colle au tableau : pour les trois styles de l'essai, l'après est au plus égal à l'avant en moyenne, en p99 et au pire. Le témoin bouge de +0,6 point ;
  - « hard techno identique à l'octet » et « 0,0 LU » sont des mesures du banc, gardées hors du dépôt, que je n'ai pas pu refaire. Elles sont cohérentes avec le code : interrupteur mis, les autres styles restent le même objet (`onOnlyTheThreeStylesChangeAndTheirNotesStay`).
- **Zone** :
  - rien n'est touché dans `app/src/main`, `app/src/benTest`, `app/src/journey`, `scripts/bench/`, `SpeakerKick` ni `engine` ;
  - chaque commit rouge ne contient que des tests (voir m1 pour l'un d'eux).

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

### M1. L'air gagné par la nappe et le lead du jump up est mis sur le compte de l'OTT seul
- **Passage** : rapport, « Ce qu'on en tire » : « leads et nappes gagnent de l'air avec l'OTT, surtout la nappe du jump up (+7 dB) ».
- **Scénario** :
  - en jump up, la nappe et le lead changent de son (« Nappe » devient « Accords future bass », « Hoover » devient « Supersaw ») **et** reçoivent l'OTT : leurs +7,0 et +4,3 dB ne sont pas ceux de l'OTT ;
  - là où seul l'OTT change (nappes et leads de la tech house et de la techno), le gain va de +1,5 à +5,4 dB ;
  - Benjamin peut en conclure que l'OTT seul éclaircit une nappe de 7 dB, et le vouloir partout.
- **Correction** : « leads et nappes gagnent de l'air : +1,5 à +5,4 dB là où seul l'OTT change (tech house, techno) ; en jump up, +7 dB sur la nappe et +4,3 dB sur le lead, avec les nouveaux sons et l'OTT ensemble ».

### M2. Le recul dans la bande du téléphone est adouci
- **Passage** : rapport, « Ce qu'on en tire » : « la part 1-4 kHz recule de 1 à 2 dB sur plusieurs voix ».
- **Scénario** :
  - le tableau montre un recul sur les 9 voix sans exception, de 0,6 dB (lead et nappe du jump up) à 2,3 dB (batterie et lead de la tech house) ;
  - c'est la bande que joue un haut-parleur de téléphone : c'est la principale contrepartie pour l'écoute de Benjamin, et la phrase la présente comme partielle.
- **Correction** : « la part 1-4 kHz recule sur les 9 voix, de 0,6 à 2,3 dB ».

### M3. Les extraits ne disent pas quel « avant » ils font entendre
- **Passages** :
  - rapport, « Extraits envoyés à Benjamin » : « même passage avant / après » ;
  - décisions, ligne « Extraits ».
- **Scénario** :
  - au banc, le FLAC d'un pack n'est qu'une tonalité de remplacement, et coupé, un pack d'essai installé ne passe pas devant le kit (`soundCategories`). L'« avant » joue donc les sons intégrés à l'appli. Le rapport ne le dit qu'en creux, dans « Charge du processeur » : « Remplacer la batterie synthétisée » ;
  - sur un téléphone qui a téléchargé les packs des styles, l'avant est autre : la batterie du jump up vient du pack « dnb », le charley ouvert de la techno du pack TR-909, la tech house de son pack ;
  - Benjamin peut juger l'essai contre un « avant » qui n'est pas celui de son téléphone, ou ne pas reconnaître son morceau dans l'extrait.
- **Correction** : dans « Extraits » et dans « Ce dont je ne suis pas sûr », écrire : « avant = les sons intégrés à l'appli, sans pack (au banc, les FLAC des packs ne sont qu'une tonalité) ; sur un téléphone qui a téléchargé les packs des styles, l'avant sonne autrement ».

## Mineur
- **m1. Un test placé dans un commit rouge y est vert.**
  - Passage : rapport, « Rouge puis vert, à chaque changement : […] WAV de pack lus au banc : 599779ab / 94c546f5 ».
  - Scénario : `SoundTrialPacksTest` est arrivé dans le rouge 599779ab, mais il ne dépend que de `SoundTrial` (06efec42) et de `soundCategories`, que 94c546f5 ne touche pas. Il est donc vert sur son « rouge », et son rouge n'a jamais été montré : sur 2ee8f348, il ne compile pas. Il vérifie pourtant quelque chose : sans le kit de l'essai, le charley ouvert de la techno donnerait `pack:tr909`. La clé `sons.essai` de Gradle est entrée dans le même vert, sans test.
  - Correction : dire dans le rapport que `SoundTrialPacksTest` est un test de garde de 06efec42, vert dès son commit. Aucun code à changer.
- **m2. Un test suppose l'interrupteur coupé sans le couper.**
  - Passage : `SoundTrialPacksTest#withoutTheSwitchTheRecipesPacksStayFirst`, qui ne coupe pas la propriété, alors que `@After` l'efface toujours.
  - Scénario : `-Psons.essai=1` vaut pour tous les tests de l'appli lancés ensemble. JUnit 4 joue `without…` en premier (ordre des hachages : −1612075058 avant −1057770999), donc le test échoue. Ensuite, le `@After` éteint l'essai pour le reste de cette JVM. Les scripts du banc filtrent leurs tests (`live-bench.sh` : `LiveSongRecorder` et `PreviewLevelBench`), donc ce cas n'arrive pas aujourd'hui.
  - Correction : garder la valeur d'avant en `@Before`, la remettre en `@After`, et couper la propriété au début de `without…`. Même schéma dans `SoundTrialTest`, que la clé Gradle n'atteint pas.
- **m3. Interrupteur mis sans packs d'essai, le banc joue un mélange.**
  - Passage : `SoundTrial.variant`, `kit = drums.associateWith { pack }`, et les voix de kit retirées.
  - Scénario : si les packs `essai_*` ne sont pas installés, `soundCategories` met devant le pack du style. Le charley ouvert de la techno quitte alors le TR-909 pour le pack « techno », et la percussion de la tech house perd sa voix 10 : le banc n'entend ni le défaut, ni l'essai. Ce cas ne concerne que le banc.
  - Correction : que `LiveSongRecorder` refuse `ps.sons.essai=1` quand un pack `essai_<style>` manque, ou au moins l'écrire dans la doc de `SoundTrial`.
- **m4. `BenchFlac.registerWavs` lit tout le fichier et promet un refus non testé.**
  - Passage : `pcm16` : `val b = f.readBytes()`.
  - Scénario : un WAV entier est chargé en mémoire avant le contrôle du format et avant la coupe à `MAX_SECONDS`. Un WAV d'une heure (690 Mo) déposé dans un dossier de pack irait tout entier dans la mémoire de la JVM de test. Aucun vrai pack n'a de WAV aujourd'hui.
  - Le commentaire « Other WAVs (another depth, a broken header) stay unreadable, as a FLAC that is no FLAC » n'est pas comme sur un téléphone : l'appli y lit un WAV 24 bits (« Raw WAVs can pass straight through at 8, 24 or 32 bits », `AudioDecoder`). Et aucun test ne couvre ce refus.
  - Correction : lire l'en-tête avec `RandomAccessFile`, puis seulement les `MAX_SECONDS` de données. Corriger le commentaire (« refusés au banc, lisibles sur un téléphone »), ou ajouter un test du refus.
- **m5. Les empreintes rougiront au prochain changement voulu.**
  - Passage : `SoundTrialTest.BASE_HASHES` et le commentaire « hashes taken on 2cfb8a3a, before the trial ».
  - Scénario : après la fusion, tout changement voulu de l'une des 37 recettes, ou de `build`, rougit `offEverySongIsTheSameToTheByte`. Une fois les empreintes refaites, le commentaire devient faux.
  - Correction : dire dans le test comment refaire les empreintes, et que la garde durable est `offEveryStyleIsItsVeryRecipe` ; ou retirer les empreintes quand l'essai est tranché.
- **m6. NumPy manque à `THIRD_PARTY.md`.**
  - Passage : la ligne SciPy, qui cite `trial_drums.py`.
  - Scénario : le script utilise aussi NumPy (BSD-3-Clause), absent du tableau. C'était déjà le cas pour `instrument_pack.py`.
  - Correction : ajouter une ligne NumPy (scripts seulement, pas dans l'appli).

rouge constaté : 06efec427988f08fd8e7817f7300fc11fe5a035c : rejoué sur 2ee8f3483402c9e101bb172a1391559fdf8b3641 : 3 tests sur 6 échouent sur leurs propres assertions, aucun échec de compilation : `onTheBundledModernSoundsPlay` (l. 87, « expected Accords future bass but was Nappe »), `onPadsAndLeadsGetTheOtt` (l. 77, « jump_up pad ») et `onTheThreeStylesTakeTheirTrialDrumPacks` (l. 62, kit `essai_jump_up` attendu, `dnb` trouvé). Les deux tests « off » sont verts sur le code de la base.

Verdict : 0 bloquant, 0 grave — fusion possible
