Relecture du lot « écoute » (T1b, écoute au micro)
Relu : 9fcc7ab1ff953f5fb25224e87b406b2c545e2fee

Portée : diff `origin/ccr-80af1a1b-mn0wec...HEAD` (Spectre, Guet, EcouteMicro, MicroReel, Robot, Rapport, Carnet, BenTestActivity, tests, scripts/bentest, docs). Lu en entier côté code ; Kotlin non exécuté (Gradle non lancé, voir plus bas).

## BLOQUANT
Aucun.

## GRAVE
Aucun.

## MOYEN

1. **`debut_s` toujours proche de 0 (le commentaire dit « environ une seconde »)** : `EcouteMicro.kt:103-108`. `t0` est pris APRÈS `prep.morceau(s)`, qui rend la main une fois la lecture lancée et le Guide refermé (`Robot.kt:559-565`, taps et attentes). `debut = horloge() - t0` ne mesure donc que `reglerVolume()` (quelques ms). Scénario : le micro écoute de 1 à 3 s après le début du morceau, `empreinte.py` lit le banc de 0 à 30 s (`empreinte.py:225`) : fenêtre décalée, empreinte comparée sur un autre bout du morceau (un intro ou un break de 2 s suffit à fausser une octave). Corriger : prendre `t0` avant `prep.morceau` (ou au moment de la lecture) et décompter jusqu'au début de l'écoute.

2. **Le bruit de la pièce est sommé sur TOUTES les trames, pour un balayage qui ne passe que ~4 trames dans chaque bande** : `Spectre.kt:170-174`, `Spectre.kt:210`. `noise = bruit × tramesMicro` (~75 trames pour 13 s) alors que l'énergie du balayage d'une bande tient en ~3 à 5 trames (rejoué en Python : 75 trames, bande d'un tiers d'octave ≈ 0,45 s). Conséquences : (a) « entendue à 6 dB au-dessus du bruit » (`AU_DESSUS = 4`) exige en réalité environ 19 dB de rapport signal/bruit dans la bande ; (b) `rsb` et le message du feu orange « n'est qu'à X dB au-dessus du bruit » (`Spectre.kt:189`) sont sous-estimés d'environ 13 dB, et le seuil `RSB_MIN_DB = 20` est donc en pratique ~33 dB réels. Sur un petit haut-parleur, les bandes graves (50 à 200 Hz) tombent à « — ». Ensuite `apply_curve` met 0 d'énergie pour ces bandes côté banc (`empreinte.py:80`) alors que le micro, lui, entend (critère stationnaire, correct pour un morceau) du grave : écart faux sur 63/125 Hz, risque de « rouge » basse non réelle. Les tests (`CourbeTest`) passent car le balayage fabriqué est à pleine échelle avec bruit 0,0005. Corriger : pour le balayage, comparer à `bruit × (trames où la bande est balayée)` ou au bruit par trame.

3. **Faux positifs « trou » probables sur de la vraie musique** : `Guet.kt:61-69`. Trou = 3 blocs de 20 ms à 30 dB sous la médiane des 2 s d'avant. Trap (808 coupées), techno (breaks, kick sidechain), house : un silence rythmique de 60 ms suffit. Effet : feu orange forcé (`EcouteMicro.kt:126`) et 2 s du micro écrites sur disque pour un « problème » qui est du rythme. `GuetTest` ne teste que du bruit rose stable (aucun morceau rythmé, aucun cas qui ne doit PAS déclencher). Au minimum : un test sur un signal rythmé à silences de 60 ms prévus, ou un seuil plus long (≥ 150 ms).

4. **Le début du balayage peut être manqué par le micro** : `EcouteMicro.kt:86-87`, `MicroReel.kt:73-78`. `hp.jouer` attend que le moteur joue (jusqu'à 10 ms de pas), PUIS le micro démarre (`AudioRecord.startRecording` : plusieurs dizaines de ms). Le balayage passe dans la bande 50 Hz entre 0,2 et 0,65 s : celle-ci (et un peu 63 Hz) est sous-lue alors que la référence les compte en entier. Le test (`CourbeTest.kt:34`) simule le contraire (le micro écoute 150 ms AVANT). Remède : armer le micro d'abord (thread) puis jouer, ou ignorer les bandes < 80 Hz dans le feu.

5. **Affirmation « même analyse à l'identique » (rapport ligne 9) non prouvée par un test croisé** : aucun test ne compare Kotlin et Python sur le même signal ; `test_the_bands_are_the_phones` ne vérifie que les centres. Relecture ligne à ligne : fenêtre de Hann périodique, trames (n=16384, saut 8192, trame partielle jetée), bornes `ceil`, `min(…, n/2)`, octave = 3 tiers, sélection « entendue » : identiques. Mais un écart futur passerait inaperçu. Proposer un jeu d'or (valeurs `Bandes.energies()` d'un signal fixe copiées dans `test_empreinte.py`).

6. **Le test de la permission ne prouve pas le câblage** : `EcouteMicroTest#theMicrophoneIsAskedForWhenItIsNeeded` ne teste que `EcouteMicro.demarrer` (deux lambdas). Le vrai câblage (`BenTestActivity.kt:62-70` : `microEnAttente`, résultat du lanceur, `Robot.start`) n'est exercé nulle part ; le rapport (ligne « permission au moment voulu ») y voit une preuve. De plus `microEnAttente` est un `remember` : une rotation d'écran pendant la boîte de dialogue le perd, le résultat arrive, rien ne démarre (bouton « Commencer » consommé).

7. **« 2 s seulement » : en fait jusqu'à 4 extraits (8 s) par passage** : `Guet.kt:15` (`maxExtraits = 1` par instance, une instance par style, `EcouteMicro.kt:105`). `PHRASE` (`EcouteMicro.kt:240-242`) annonce « 2 secondes de son » ; l'écran dit bien « N extraits de 2 s » après coup, mais la phrase d'avant est plus faible que la réalité.

## MINEUR

- `empreinte.py:166-170` : `filter_curve` plante (`min()` sur liste vide) si toute la courbe est `null` (liste de 26 null passe le garde de `micro_of`, `:161`).
- `EcouteMicro.kt:112` : `guet.finir()` est dans un `finally` ; une erreur d'écriture disque y masquerait l'exception d'origine et sauterait `prep.arreter()`/`remettreVolume()` (le même bloc). Entourer d'un `runCatching`.
- `MicroReel.kt:166` : pas de repli sur `MIC` si la source `UNPROCESSED` annoncée n'initialise pas ; échec net « micro indisponible ». `MicroReel.kt:176` : le compteur `empty` ne se remet jamais à zéro (200 lectures vides cumulées).
- `MicroReel.kt:121-125` : si l'appli est tuée entre `reglerVolume` et `remettreVolume`, le volume reste à 60 % et une reprise le prend pour « l'ancien » (le volume de Benjamin est perdu).
- Pas de borne de temps dans la boucle de lecture du micro (`MicroReel.kt:173`) : un `read` bloquant sans fin dépend du seul chien de garde d'étape (qui ne tue pas, il marque « trop longue » après coup).
- `Guet.kt:45-46` : l'écriture du WAV (192 Ko) se fait dans la boucle de lecture du micro ; sans conséquence avec 0,8 s de tampon, à noter.

## Vérifié sans problème

- FFT (`Spectre.kt:91-128`) : inversion de bits, table de twiddle (pas `n/size`, signe négatif), ordre des étages corrects. Trame partielle jetée comme en Python. Ondulation de la fenêtre de Hann² à 50 % sur un balayage mesurée en Python à 5 décalages de grille : ≤ 0,4 dB, négligeable.
- Bandes et octaves : milieux (63, 125, …, 8000) = bons indices de `NOMS` ; `m-1..m+1` toujours dans [0, 25] ; Kotlin et Python utilisent le même critère `heard` (bruit nul : énergie > max × 1e-9).
- `Guet` : anneau de 2 s, `from/to` bornés, rien d'écrit hors des 2 s autour de l'anomalie, un seul fichier par instance, rien sans anomalie (testé), pas de boucle sans fin.
- Ressources : `AudioRecord` arrêté et libéré en `finally` ; `NativeStems` fermé dans `arreter()` dans le même ordre que `GuideListen.releaseEngine` ; `arreter` et `remettreVolume` en `finally` pour la courbe et chaque style ; micro refusé : aucune étape ne touche au moteur ni au volume (testé).
- Vraie appli : `git diff` sur `app/src/main` et `app/build.gradle.kts` vide (RECORD_AUDIO était déjà déclaré dans le manifeste principal) ; `check-apk.sh` exige les nouvelles classes ; `-keep class com.pocketstudio.bentest.**` les garde en clair ; `HorsVraieAppliTest` lit réellement les fichiers (échouerait si le code passait dans `src/main`).
- Rapport : `Carnet.MICRO` exclu de `steps` de `mesures.json` et repris dans l'objet `micro` ; `JsonText.obj` retire les null ; l'avertissement « extraits de 2 s » est dans le bloc qui porte le bouton « Envoyer » (seul endroit où il existe).

## Rejeu

- `python3 -I scripts/bentest/test_empreinte.py` : 9 tests, OK.
- Simulation Python du balayage (12 s, 5 décalages de grille) : écart de niveau par bande ≤ 0,4 dB ; 75 trames pour 13 s (pour le constat 2).
- Gradle (`:app:testDebugUnitTest --tests 'com.pocketstudio.bentest.*'`) : non relancé ; les constats portent sur la lecture du code. Aucun fichier de code ou de test modifié, rien commité.

Verdict : 0 bloquant, 0 grave
