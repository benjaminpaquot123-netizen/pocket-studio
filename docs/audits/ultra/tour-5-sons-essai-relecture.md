# Tour 5, lot « sons-essai » : relecture neuve (2026-11-12)
Relu : c777d2f652f8ad5e2ea4fbd5550e48b84b497495

Deux relectures dans ce fichier.
- L'essai n° 2 : premier passage sur 69cbcae4 (relevé commité en f1810d0c), second passage sur c777d2f6 (l'écart `f1810d0c..c777d2f6`).
- L'essai n° 1 : relu sur 262236b2, constats gardés tels quels.
- La dernière ligne donne le verdict de l'essai n° 2, après son second passage.

## Essai n° 2, second passage (relu sur c777d2f6)

### Ce qui a été relu
- Les cinq commits du lot depuis f1810d0c, sur la première branche :
  - la paire fbf6f260 (rouge, `SoundTrialTest.kt` seul) / 862f5cd5 (vert, `SoundTrial.kt` seul) ;
  - 9f7ff465 : commentaires des scripts de batterie et `RETOURS.md` ;
  - e9e4db51 (décisions) et 84bf9ad6 (rapport).
- La fusion de la base c777d2f6 est propre (`git show --cc` vide). Elle apporte le lot « kick » (`SpeakerKick`, moteur), « langues » et « gaucher », mais aucun fichier du lot, ni `StarterWizard`, `WizardMix`, `RoleMix` ou `WideMix`.

### Contrôles faits
- **Rendu par défaut** : `862f5cd5` ne change que `SECOND` et `SECOND_OWN`, qui ne jouent qu'interrupteur mis. Les empreintes « off » sont vertes sur HEAD après la fusion (voir plus bas).
- **Moteur** : aucun commit du lot ne touche `engine`. Ce que la fusion apporte au moteur vient de la base (lot « kick »).
- **L'égaliseur trois bandes** : aucune règle du mixage ne le lit ni ne le modifie (`grep EffectType.EQ` : seulement des préréglages de `FxPresets`). Médium : cloche à 1 kHz, ±12 dB, donc +12 (basse) et +9 (voix de la tech house). Aigus de la voix : +6 dB.
- **Chaînes réelles sur HEAD**, par le même test temporaire hors dépôt (worktree supprimé) :
  - jump up, voie de la basse : les six effets du mixage, dont le `BASS_BODY`, puis la saturation et l'égaliseur. La voie est pleine (8 sur 8), voir m16 ;
  - tech house : excitateur → saturation → égaliseur → filtres → écrêteur ;
  - techno : coupe-bas (égaliseur paramétrique neutre, le sien) → saturation → égaliseur → excitateur → filtre → écrêteur, voir m14 ;
  - leads : la techno garde son chorus. Le lead du jump up garde aussi tous les effets de la chanson coupée (voir m13). Envois partout à 0,3 (réverbe) et 0,4 (délai) ;
  - nappes : 0,45 de réverbe. Celles de la tech house et de la techno gardent tous leurs effets. Celle du jump up (« Accords future bass ») perd l'élargisseur des accords de `RoleMix` (largeur 0,75) et prend celui de `withSpread` (0,5) : voir m13.
- **Rouge rejoué** : fbf6f260, sur son propre code, celui d'avant la correction (ligne « rouge constaté » plus bas). 4 tests sur 10 échouent sur leurs assertions :
  - « jump_up: its EQ » ;
  - « jump_up strip 2 lost Insert(type=13, [0.2594, 0.5833, …]) » : le `BASS_BODY` ;
  - « tech_house lead delay [0.2, 0.3] -> [0.25, 0.3] » ;
  - « mids and highs lifted on the voice ».
- **Ce que le rouge ne montrait pas, prouvé par une mutation** : le test des leads rougissait sur le délai, pas sur le chorus. J'ai rajouté un élargisseur sur le lead de la tech house dans `SECOND_OWN` (worktree jetable, second lancement de Gradle). `onLeadsAndPadsGetMoreSpaceAndKeepTheirEffects` échoue alors sur « tech_house strip 6 lost Insert(type=4, [0.3, 0.4, 0.4, 0.4, 1.0]) », le chorus. Les 9 autres tests de `SoundTrialTest` restent verts, dont les deux « off ».
- **Mesures du rapport**, relues contre son nouveau tableau (commit 862f5cd5) :
  - jump up : batterie bas +3,0 et médium +3,5, basse médium +3,2 ;
  - tech house : seule hausse, le médium de la batterie (+4,1). Le bas de la basse baisse de 3,1 dB ;
  - techno : seule hausse, le médium de la batterie (+4,9). Le bas de la basse baisse de 3,3 dB ;
  - mix : +1,5 dB au plus, +2,1 et +0,9 ;
  - aigus : mix −1,2 à −4,2 dB, batterie −2,8 à −4,8 dB ;
  - contreparties : sub de la basse −2,5 à −5,0 dB, bas −3,1 à −3,3 dB ; sub de la batterie −3,2 et −3,9 dB ; sub du mix du jump up −4,7 dB.
  Toutes les phrases collent au tableau. Facteur de crête du kick recalculé : 8,8-10,9 dB à l'essai n° 1, 4,7-7,3 dB maintenant, donc « presque divisé par deux » se tient. Je n'ai pas refait le banc.
- **Python** : `test_trial_drums.py` sur HEAD : « 2041 checks, 0 failed ». Le changement de 9f7ff465 dans `trial_drums.py` ne touche que des docstrings : les sons sont les mêmes.
- **Zone** :
  - 7 fichiers, tous en mode 100644, aucun lien symbolique ;
  - rien dans `app/`, `scripts/bench/`, `engine`, ni dans `SpeakerKick` ;
  - `rapport-zone.py HEAD` : code 0, 4 filtres.
- **Gradle** : deux lancements, chacun par `gradle-file.sh` dans un worktree jetable (le rejeu du rouge, puis la mutation avec le diagnostic).

### Bloquant

Aucun.

### Grave

Aucun : G1, G2 et G3 du premier passage sont **corrigés** (voir plus bas).

### Mineur (second passage)
- **m12. Le rapport compte encore 9 tests, dont un sur « la largeur ».**
  - Passage : rapport l. 213, « (9 tests verts, dont les 3 de l'essai n° 2 sur la basse, la largeur et l'espace, la voix de la tech house) ».
  - Scénario : il y en a 10 maintenant, dont 4 de l'essai n° 2, et plus aucun sur la largeur.
  - Correction : « 10 tests verts, dont les 4 de l'essai n° 2 : ordre de la basse, rien du mixage perdu, espace des leads et nappes, voix de la tech house ».
- **m13. L'exemption du jump up dans le test des leads est plus large que nécessaire, et son commentaire n'est pas exact.**
  - Passage : `SoundTrialTest.kt` l. 177-178, « a wide synth takes no widener ».
  - Scénario :
    - le lead du jump up (Supersaw) garde tous les effets de la chanson coupée, mais le test ne le vérifie pas, puisqu'il exempte toute piste dont le son change ;
    - la nappe « Accords future bass » prend bien un élargisseur, celui de `withSpread` (0,5 ; 0 ; 0,35). Elle perd seulement celui des accords de `RoleMix` (largeur 0,75, basses au centre). Ce changement vient du changement de son de l'essai n° 1 ; le rapport ne le dit pas.
  - Correction : n'exempter que la nappe du jump up, et écrire « a synth with its own width takes the spread's widener, not the chords' ».
- **m14. En techno, l'excitateur de l'assistant passe après la chaîne de l'essai.**
  - Passage : `WizardMix.withAudibleBass`, qui place l'excitateur devant le premier filtre, écrêteur, OTT ou pompe.
  - Scénario :
    - en jump up et en tech house, l'excitateur reste devant la saturation de l'essai. En techno, il arrive derrière l'égaliseur de l'essai (+12 dB à 1 kHz) ;
    - `onTheBassKeepsEveryInsertTheMixGaveIt` vérifie que les effets du mixage sont là, pas leur ordre ;
    - rien n'est perdu, mais les trois styles n'ont pas la même chaîne.
  - Correction : le dire dans le rapport, ou vérifier aussi que l'ordre relatif des effets du mixage ne change pas.
- **m15. « Mes élargisseurs ne faisaient rien » est un peu fort.**
  - Passage : rapport l. 145.
  - Scénario : ils faisaient passer la largeur de 100 à 180 %. Seul leur étalement était écrasé. Sans effet sur la suite : ils sont retirés.
  - Correction : « mes élargisseurs ne changeaient que la largeur ».
- **m16. Interrupteur mis, la voie de la basse du jump up est pleine.**
  - Passage : la doc de `SECOND` le dit (« Two inserts only: a full strip drops the mix's own »).
  - Scénario : 8 effets sur 8. Dans un projet fait par l'essai, on ne peut plus ajouter d'effet sur cette voie, et le prochain effet du mixage ajouté pour la bass music y serait perdu sans bruit. `onTheBassKeepsEveryInsertTheMixGaveIt` le verrait.
  - Correction : aucune pour un essai caché. À régler si l'essai devient le son par défaut.

## Essai n° 2, premier passage (relu sur 69cbcae4)

### Ce qui a été relu
- Les 16 commits du lot sur la première branche, de 732a4fad (le retour dans `RETOURS.md`) à 0f01038c (rapport et décisions) : sept paires rouge / vert. Les fusions de la base ccr-80af1a1b-mn0wec ne sont pas relues.
- La fusion 69cbcae4 est propre : `git show --cc` est vide, et elle n'apporte aucun fichier du lot.
- Le code : `SoundTrial.kt` (`SECOND`, `SECOND_OWN`), `SoundTrialTest.kt` (trois tests neufs), `trial_drums.py` et `test_trial_drums.py` (batterie n° 2, kick v3), `voix.py` et `test_voix.py` (quatre bandes).
- Ce que ce code touche dans l'assistant : `StarterWizard.withEffects`, `WizardMix.honest` et `withAudibleBass`, `RoleMix.apply`, `WideMix.withLeadEffects`, `withSpread` et `withBuses`.
- Je n'ai rien écrit de ce lot, et je ne modifie que ce fichier.

### Contrôles faits
- **Rendu par défaut** :
  - `SECOND` et `SECOND_OWN` ne s'ajoutent que dans `variant()`, que seul `styleOf` appelle, interrupteur mis ;
  - la propriété n'est lue que par `SoundTrial.on()` : `grep` la trouve aussi dans les deux tests et dans `build.gradle.kts`, nulle part ailleurs ;
  - sur HEAD (worktree jetable), `SoundTrialTest` donne 9 tests verts sur 9, dont `offEverySongIsTheSameToTheByte` (les 37 empreintes de 2cfb8a3a n'ont pas bougé) et `offEveryStyleIsItsVeryRecipe`.
- **Moteur** :
  - `git log --no-merges 41975db2..HEAD -- engine` et `git diff 41975db2 HEAD -- engine` sont vides ;
  - l'essai ne se sert que d'effets qui existent déjà (saturation, EQ paramétrique, égaliseur 3 bandes, élargisseur, envois) : rien de neuf dans le rappel audio.
- **Réglages relus dans le code** :
  - EQ paramétrique de la basse : 20 × 1000^0,535 = 805 Hz, (0,833 − 0,5) × 36 = +12 dB, Q 0,8 ;
  - égaliseur : plateaux à 100 Hz et 8 kHz, ±12 dB (`Effects.h`, l. 70), donc +5 / −9 dB ;
  - voix de la tech house : 2,52 kHz, +9 dB, Q 1,0 ;
  - saturation, dans l'ordre : force 0,95, tonalité ouverte, mélange 85 %, volume, volume constant.
- **Chaînes réelles** : un test temporaire, hors dépôt (worktree supprimé), construit les trois chansons de quatre façons :
  - interrupteur coupé ;
  - essai n° 1 ;
  - essai n° 2 ;
  - essai n° 2 sans ses deux élargisseurs.
  Il imprime les voies de la basse, du lead et de la nappe. G1 et G2 en viennent.
- **Python** (`-I`, worktrees jetables) :
  - sur HEAD, `test_trial_drums.py` : « 2041 checks, 0 failed » ; `test_voix.py` : OK ;
  - rouges rejoués (voir les lignes « rouge constaté ») : 805 échecs sur 2028 pour la batterie n° 2, 12 sur 2041 pour le kick v3 ;
  - le rouge de `voix.py` (56bf3f55, sur sa propre version de `voix.py`) échoue sur « 41.2 Hz rms None ».
- **Kick, recalculé** : packs construits hors du dépôt avec le script de 922d5778 (l'essai n° 1) et celui de HEAD, comparés à crête égale :
  - 60-250 Hz : +4,6 à +6,5 dB ;
  - clic : +6,8 à +20,9 dB ;
  - facteur de crête : 4,7 à 7,3 dB.
  Ces trois chiffres du rapport sont exacts. Pour les deux autres, voir m9.
- **Tableau rouge / vert** : les 7 paires du rapport sont celles de `git log`. Chaque rouge ne contient qu'un fichier de test, chaque vert que le code. L'ordre du tableau n'est pas celui des commits : c'est sans effet.
- **Licences** :
  - `trial_drums.py` ne lit que les WAV qu'il vient d'écrire (`read_wav` sur son dossier de sortie), et `LICENSE.txt` dit « aucun enregistrement ni échantillon tiers » ;
  - `THIRD_PARTY.md` n'a pas bougé depuis l'essai n° 1, et ses lignes NumPy et SciPy restent justes.
- **Zone** :
  - 9 fichiers, tous en mode 100644 ;
  - rien dans `app/`, `scripts/bench/`, `engine`, ni dans `SpeakerKick` (qui n'est pas dans cette branche) ;
  - ni son, ni lien symbolique, ni `__pycache__` commité. Les deux `__pycache__` du disque sont plus anciens et ignorés par `.gitignore`.
  - `rapport-zone.py HEAD` : code 0, les mêmes 4 filtres.
- **Non refait** :
  - les mesures du banc (bandes, haut-parleur de téléphone, processeur, LU, dBTP) : le banc est long, et ses résultats sont hors du dépôt ;
  - le sha mesuré n'est pas noté (M5).

### Bloquant

Aucun.

### Grave

#### G1 (grave). La chaîne de la basse n'est pas celle annoncée, en techno et en jump up — **corrigé**
- **Corrigé en 862f5cd5** : la basse ne reçoit plus que deux effets, la saturation puis l'égaliseur trois bandes (graves +5, médium +12 à 1 kHz, aigus −9), que le mixage ne touche pas. `onTheBassGetsParallelSaturationThenItsEq` exige l'ordre ; `onTheBassKeepsEveryInsertTheMixGaveIt` exige chaque effet de la chanson coupée, à l'identique. Rouge rejoué sur fbf6f260 (le `BASS_BODY` du jump up perdu) ; sur HEAD, techno : coupe-bas neutre → saturation → égaliseur. **corrigé**
- **Passages** :
  - `SoundTrial.kt` l. 62-66 : les trois inserts de basse de `SECOND` ;
  - rapport l. 124 : « Basse (sous l'interrupteur), dans cet ordre : saturation […] ; +12 dB vers 800 Hz ; égaliseur » ;
  - doc de `SoundTrial` : « Their lines, notes, form and mix are their recipe's ».
- **Scénario** : relevé sur HEAD, sur la voie de la basse de la chanson construite.
  - **Techno.**
    - `RoleMix` prend le PARAM_EQ de l'essai pour « a flat EQ of its own » : l. 124-126, il ne regarde que la coupe, pas le gain. Il y écrit donc sa coupe-bas (réglage 3 : 0,126).
    - Puis `WizardMix.withAudibleBass` (l. 513-517) range cette coupe-bas devant l'excitateur.
    - Chaîne finale : PARAM_EQ (+12 dB à 805 Hz, coupe à 30 Hz) → excitateur → SATURATION → EQ → filtre → écrêteur.
    - Le relèvement passe donc avant la saturation (force 0,95, 85 %) au lieu d'après, et la saturation l'écrase. C'est cohérent avec la basse de la techno qui ne bouge pas dans le médium (−0,1) : le rapport le constate sans l'expliquer.
  - **Jump up.**
    - La voie de la basse et sa copie (voie 8) n'ont plus aucune case libre : 8 sur 8.
    - Le `BASS_BODY` de l'assistant (+3 dB à 120 Hz, « Bass music's lows: […] the bass's body up », `WizardMix.kt` l. 269 et 378) n'y entre plus : il disparaît sans bruit.
    - Interrupteur coupé, il est là : « … FILTER | PARAM_EQ(0,259 ; 0,583) ».
    - C'est la bande « bas » (60-250 Hz). La basse du jump up y perd 4,5 dB, et garde −5,2 dB de sub malgré les +5 dB de l'égaliseur, alors que celles des deux autres styles s'arrêtent à −2,4 et −1,7. Le rapport met cette perte sur le compte de la saturation seule.
  - **Tech house** : l'ordre annoncé est tenu (excitateur, saturation, +12 dB, égaliseur, puis filtres et écrêteur).
  - `onTheBassGetsParallelSaturation` vérifie que les trois inserts sont là, pas leur ordre, ni ce qui a disparu : il est vert dans les deux cas.
- **Correction** :
  - poser la chaîne de basse de l'essai là où l'assistant ne la reprend pas. Par exemple, une étape de l'essai après `withAudibleBass`, dans les cases libres, et seulement interrupteur mis ; ou retirer d'abord ce que l'essai remplace ;
  - ajouter un test sur la chanson construite, pour les trois styles :
    - la SATURATION passe avant le PARAM_EQ de +12 dB, et lui avant l'égaliseur ;
    - le PARAM_EQ de l'essai garde ses quatre réglages ;
    - interrupteur mis, chaque insert de la voie coupée est encore là (le `BASS_BODY`, la coupe-bas) ;
  - remesurer la basse ensuite (M5).

#### G2 (grave). Élargisseur des leads et des nappes : le test ne prouve rien, l'étalement de l'essai est écrasé, le chorus du lead disparaît — **corrigé**
- **Corrigé en 862f5cd5** : plus d'élargisseur de l'essai ; lead réverbe 0,3 et délai 0,4, nappe réverbe 0,45. `onLeadsAndPadsGetMoreSpaceAndKeepTheirEffects` exige des envois plus forts que la chanson coupée, et chaque effet du mixage sur les pistes dont le son ne change pas. Prouvé par mutation : un élargisseur remis sur le lead de la tech house le fait rougir sur le chorus perdu. Le rapport dit maintenant « la plaque et le ping-pong existaient déjà, seul le dosage monte ». Reste m13. **corrigé**
- **Passages** :
  - `SoundTrial.kt` l. 67-68 ;
  - `SoundTrialTest#onLeadsAndPadsGetWidthAndSpace` (l. 141-152) ;
  - rapport l. 129 : « élargisseur avec étalement, plaque et délai en ping-pong ».
- **Scénario** :
  - **Le test ne prouve pas l'élargisseur.**
    - Sans l'essai, `WideMix.withSpread` met déjà un élargisseur sur chaque lead et chaque nappe. Interrupteur coupé, en tech house : lead WIDENER(0,5 ; 0 ; 0,75), nappe WIDENER(0,5 ; 0 ; 1,0).
    - `LEAD_SEND_V3` (0,2) met déjà la réverbe du lead à 0,2.
    - « lead widened », « pad widened » et « lead reverb » sont donc vrais sans l'élargisseur de l'essai. J'ai émulé l'essai n° 2 sans ses deux WIDENER, sur la chanson construite : les cinq vérifications restent vraies, pour les trois styles.
    - Rejoué sur 2ef1ceae, le test ne rougit que sur « jump_up lead delay [0.2, 0.15] », après avoir passé l'élargisseur et la réverbe.
  - **L'étalement de l'essai est écrasé.**
    - `withSpread` (`WideMix.kt` l. 427-429) réécrit l'étalement de l'élargisseur qu'il trouve. Les 0,6 et 0,8 de l'essai ne jouent jamais.
    - Valeurs finales, les mêmes qu'avant : lead 0,375 en jump up et 0,75 ailleurs ; nappe 0,35 en jump up et 1,0 ailleurs.
    - Ce que l'élargisseur de l'essai change vraiment : la largeur, de 0,5 à 0,9 (de 100 à 180 %), et de 0,75 à 0,9 pour la nappe du jump up ; et les « Basses au centre », sous 110 Hz.
  - **Effet de bord non dit** : quand un élargisseur est déjà là, `withLeadEffects` (`WideMix.kt` l. 331) ne met plus le chorus du lead. En tech house et en techno, le lead perd son CHORUS(0,3 …), qu'il a interrupteur coupé et dans l'essai n° 1.
  - **Envois** :
    - la réverbe du lead passe de 0,2 à 0,25 ;
    - son délai : 0,15 → 0,3 en jump up, 0,25 → 0,3 en techno, sans changement en tech house (déjà 0,3) ;
    - réverbe des nappes : 0,35 → 0,4 (jump up), 0,1 → 0,4 (tech house), 0,15 → 0,4 (techno).
    - Le bus de réverbe est déjà une plaque, et le délai déjà un ping-pong (`withBuses`, valeurs par défaut des bus). Le rapport présente comme ajouté ce qui était déjà là.
- **Correction** :
  - tester ce que l'essai seul apporte : la largeur (au moins 0,85) de l'élargisseur du lead et de la nappe, la réverbe de la nappe, le délai du lead. Chaque vérification doit rougir sans la ligne de l'essai qu'elle couvre ;
  - retirer de `SECOND` l'étalement, qui ne joue pas, ou écrire qu'il est écrasé ;
  - trancher pour le chorus : le garder, ou écrire dans le rapport et les décisions « le lead perd son chorus en tech house et en techno » ;
  - dans le rapport, écrire : « largeur 180 %, réverbe et délai un peu plus hauts ; l'étalement, la plaque et le ping-pong étaient déjà là ».

#### G3 (grave). « Critère atteint » est plus fort que les tableaux du rapport — **corrigé**
- **Corrigé en 84bf9ad6 et e9e4db51** : « Le critère, dit exactement » : jump up tenu par des hausses ; tech house et techno tenus seulement en comptant la baisse du bas de la basse (−3,1, −3,3) ; « au mix entier, aucune bande hors aigus ne monte de 3 dB ». La décision dit la même chose et ne parle plus de « critère atteint ». Chiffres relus contre le nouveau tableau (mesures sur 862f5cd5) : exacts. **corrigé**
- **Passages** :
  - rapport l. 143-146 : « Critère (au moins 3 dB sur au moins deux bandes hors aigus, sur les pistes qu'on remarque) » ;
  - décisions l. 29 : « Arrêt des réglages au critère atteint (au moins 3 dB sur deux bandes hors aigus par style, sur la batterie et la basse) ».
- **Scénario** : hausses d'au moins 3 dB hors aigus, d'après le tableau « Mesures de l'essai n° 2 » du rapport.
  - **jump up** : batterie bas +3,1 et médium +3,6, basse médium +3,5. Tenu, sur deux bandes.
  - **tech house** : une seule bande, le médium, sur deux pistes (batterie +4,4, basse +3,8). Ni le bas ni le sub ne montent nulle part (le sub de la batterie, −3,0, est une perte). Ce n'est pas « deux bandes ».
  - **techno** : la batterie seule, avec le bas pile au seuil (+3,0) et le médium à +5,4. Sa basse ne monte nulle part (−1,7, −3,0, −0,1). « sur la batterie et la basse » est faux pour la techno.
  - **Au mix**, ce qu'entend Benjamin : aucune bande hors aigus ne monte de 3 dB. Tech house +1,9 au plus, techno +0,3 au plus. Seul le sub du jump up bouge de plus de 3 dB, et il baisse (−4,9).
  - Le rapport ajoute au critère du chef d'orchestre « sur les pistes qu'on remarque ».
  - Benjamin a jugé l'essai n° 1 sur le mix. Il risque d'entendre la techno presque inchangée, hors des aigus, qui baissent de 1,8 dB.
- **Correction** :
  - écrire, style par style, ce qui est tenu : jump up, oui ; techno, sur la batterie seule, avec le bas au seuil ; tech house, une seule bande, sur deux pistes ;
  - écrire aussi ce que montre le mix ;
  - dans les décisions, remplacer « critère atteint » par ce constat. Ou bien reprendre les réglages (corriger G1 peut faire bouger la basse de la techno) et remesurer.

### Moyen

#### M4. Les contreparties ne sont pas toutes dites — **corrigé**
- **Corrigé en 84bf9ad6** : le bas de la basse, le sub du bus batterie, le sub du mix du jump up, le kick écrêté et le grain de la basse de la tech house sont dans « Contreparties mesurées », avec les chiffres du nouveau tableau.
- **Passage** : rapport l. 155-158, « Contreparties mesurées ».
- **Scénario** : d'après le tableau du rapport, au-delà de ce qu'il dit :
  - la basse perd aussi sa bande « bas » (60-250 Hz) : −4,5 dB en jump up, −2,8 en tech house, −3,0 en techno ;
  - le bus batterie perd du sub : −3,9 dB en jump up, −3,0 en tech house ;
  - le mix de la tech house perd 1,8 dB de sub.
  - Les contreparties ne disent que le sub de la basse et celui du mix du jump up. Le kick très écrêté (facteur de crête 4,7 à 7,3 dB) n'est dit que dans sa description, pas parmi les contreparties.
  - Un auditeur qui trouve le jump up « moins rond » n'en trouvera pas la cause.
- **Correction** : ajouter ces lignes aux contreparties, avec le `BASS_BODY` perdu (G1) tant qu'il n'est pas corrigé.

#### M5. Les mesures et les extraits ne disent pas sur quel commit ils ont été pris — **corrigé**
- **Corrigé en 84bf9ad6** : mesures, processeur et extraits sur 862f5cd5, batterie au commit 7764266a. Les six extraits d'avant la relecture, faits sur 2f4cefdb, sont remplacés.
- **Passages** : rapport, « Mesures de l'essai n° 2 », « Processeur », « Extraits envoyés à Benjamin ».
- **Scénario** :
  - six verts de code se suivent : basse et largeur, 700 Hz, batterie, 800 Hz, kick v3, égaliseur. Le dernier vient juste avant le rapport ;
  - aucun tableau ne donne le sha mesuré. On ne peut donc pas savoir si les extraits que Benjamin écoutera ont le kick v3 et l'égaliseur ;
  - je n'ai pas pu refaire ces mesures.
- **Correction** : écrire le sha mesuré pour chaque tableau et pour les extraits. Après G1 et G2, remesurer et refaire les extraits.

### Mineur
- **m7. Des commentaires sont restés en arrière.** **corrigé** en 862f5cd5 : la doc de `SECOND` est réécrite, et l'ancien test avec son commentaire est remplacé.
  - Passages : doc de `SECOND` (`SoundTrial.kt` l. 56), « a lift around 700 Hz » ; `SoundTrialTest.kt` l. 121.
  - Scénario : le relèvement est maintenant à 805 Hz. Dans le test, l'ancien commentaire « Driven hard enough to bite… » est resté au-dessus de celui qui le remplace.
  - Correction : « around 800 Hz », et retirer l'ancienne ligne.
- **m8. « trial n° 3 » dans les scripts, « essai n° 2 » dans le rapport.** **corrigé** en 9f7ff465 : « trial n° 2, kick v3 ». Plus aucun « trial n° 3 ».
  - Passages : `trial_drums.py` l. 20 et 219, `test_trial_drums.py` l. 19.
  - Scénario : les scripts appellent « trial n° 3 » le kick v3 que le rapport range dans l'essai n° 2. Un lecteur peut chercher un essai n° 3.
  - Correction : écrire « essai n° 2, kick v3 ».
- **m9. Deux chiffres de la batterie s'écartent du recalcul.** **corrigé** en 84bf9ad6 : « environ +11 à +19 dB », « environ +3 à +13 dB ».
  - Passage : rapport l. 121-122.
  - Scénario : recalculés à crête égale, sur la somme des canaux :
    - 250 Hz-1,5 kHz du kick : +11,2 à +19,1 dB (le rapport dit +10,8 à +19,1) ;
    - caisses claires et claps, 250 Hz-2 kHz : +3,2 à +13,3 dB (le rapport dit +4 à +12). Les caisses claires font +9,2 à +10,2, les claps +3,2 à +13,3.
  - Correction : reprendre ces deux plages, ou dire comment elles ont été mesurées.
- **m10. Le retour de Benjamin dans `RETOURS.md` ne renvoie pas à l'essai n° 2.** **corrigé** en 9f7ff465 : « → analysé : […] essai n° 2 fait […], 6 nouveaux extraits envoyés ».
  - Passage : la ligne du 2026-11-12, « → à analyser ».
  - Scénario : l'analyse est faite, c'est l'essai n° 2. Mais la ligne ne le dit pas, et n'a pas la case « [ ] » du format du fichier (ses voisines non plus).
  - Correction : « → analysé : essai n° 2 (tour-5-sons-essai-rapport.md), à réécouter ».
- **m11. Le « 300 / 200 Hz » de la basse au haut-parleur n'est pas défini.** **corrigé** en 84bf9ad6 : basses seules, passe-haut à 300 Hz (un téléphone moyen) et à 200 Hz (un bon), pondération K, rapporté à leur propre niveau.
  - Passage : rapport, « Basse au haut-parleur de téléphone (écoute en direct, 300 / 200 Hz) ».
  - Scénario : rien ne dit ce qu'est cette valeur (coupure à 300 ou à 200 Hz ? niveau rapporté à quoi ?). Les chiffres ne peuvent pas être lus.
  - Correction : une phrase qui dit ce qui est mesuré.
- **Ouverts depuis l'essai n° 1, inchangés** : m1, m2, m4 (la lecture du fichier entier) et m5, plus bas.

## Essai n° 1 (relu sur 262236b21e3de6d3136de4100432413469ad7a81, constats gardés)

### Ce qui a été relu
- Les dix commits du lot, de 2ee8f348 à bbfed992 : quatre paires rouge / vert, `THIRD_PARTY.md`, le rapport et les décisions. La fusion 4a3bc025 n'apporte que des documents : `git diff 4a3bc025^1 4a3bc025` est vide hors de `docs/`.
- Le code : `SoundTrial.kt`, `StarterWizard.style`, `SoundTrialTest.kt`, `BenchFlac.kt` (`registerWavs`, `pcm16`), `BenchWavPackTest.kt`, `SoundTrialPacksTest.kt`, la clé `sons.essai` de `app/build.gradle.kts`, `trial_drums.py` et `test_trial_drums.py`.
- Je suis un relecteur neuf : je n'ai rien écrit de ce lot, et je ne modifie que ce fichier.
- **Premier passage** sur 4a3bc025, relevé commité en 08f67747. **Second passage** sur 262236b2, seulement l'écart `08f67747..262236b2` : un commit, trois fichiers en mode 100644 (`THIRD_PARTY.md`, `BenchFlac.kt`, le rapport).

### Contrôles du second passage
- **Chiffres recalculés** à partir du tableau « Timbre » du rapport, colonne par colonne :
  - air, là où seul l'OTT change : lead tech house +5,4, nappe tech house +2,0, lead techno +2,9, nappe techno +1,5 dB, soit +1,5 à +5,4 ;
  - air en jump up : nappe +7,0, lead +4,3 dB ;
  - 1-4 kHz : les 9 voix reculent, de −0,6 (lead et nappe du jump up) à −2,3 dB (batterie et lead de la tech house).
  Les phrases nouvelles du rapport collent à ces chiffres.
- `python3 scripts/bench/rapport-zone.py HEAD docs/audits/ultra/tour-5-sons-essai-rapport.md` : code 0, les mêmes 4 filtres.
- `BenchFlac.kt` : seul le commentaire change, pas une ligne de code, donc aucun test à rejouer. Le nouveau commentaire est exact : `AudioDecoder` lit sur un téléphone le 24 bits et le flottant (`ENCODING_PCM_24BIT_PACKED`, `ENCODING_PCM_FLOAT`, l. 247-249).
- La ligne NumPy de `THIRD_PARTY.md` est juste : `library_pack.py`, `instrument_pack.py`, `trial_drums.py` et `docs/sons/outils/voix.py` importent bien NumPy, et `requirements.txt` donne 2.5.4.

### Contrôles faits
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

### Bloquant

Aucun.

### Grave

Aucun.

### Moyen

#### M1. L'air gagné par la nappe et le lead du jump up est mis sur le compte de l'OTT seul — **corrigé**
- **Corrigé en 262236b2** : le rapport sépare l'OTT seul (« là où seul l'OTT change (tech house et techno), leads et nappes gagnent de +1,5 à +5,4 dB d'air ») du jump up (« la nappe et le lead changent aussi de son : +7,0 et +4,3 dB d'air »). Chiffres recalculés : exacts.
- **Passage** : rapport, « Ce qu'on en tire » : « leads et nappes gagnent de l'air avec l'OTT, surtout la nappe du jump up (+7 dB) ».
- **Scénario** :
  - en jump up, la nappe et le lead changent de son (« Nappe » devient « Accords future bass », « Hoover » devient « Supersaw ») **et** reçoivent l'OTT : leurs +7,0 et +4,3 dB ne sont pas ceux de l'OTT ;
  - là où seul l'OTT change (nappes et leads de la tech house et de la techno), le gain va de +1,5 à +5,4 dB ;
  - Benjamin peut en conclure que l'OTT seul éclaircit une nappe de 7 dB, et le vouloir partout.
- **Correction** : « leads et nappes gagnent de l'air : +1,5 à +5,4 dB là où seul l'OTT change (tech house, techno) ; en jump up, +7 dB sur la nappe et +4,3 dB sur le lead, avec les nouveaux sons et l'OTT ensemble ».

#### M2. Le recul dans la bande du téléphone est adouci — **corrigé**
- **Corrigé en 262236b2** : « **en contrepartie, la part 1-4 kHz recule sur les 9 voix, de 0,6 à 2,3 dB** », en gras, avec la raison (la bande du haut-parleur), et repris dans « Ce dont je ne suis pas sûr ». Chiffres recalculés : exacts.
- **Passage** : rapport, « Ce qu'on en tire » : « la part 1-4 kHz recule de 1 à 2 dB sur plusieurs voix ».
- **Scénario** :
  - le tableau montre un recul sur les 9 voix sans exception, de 0,6 dB (lead et nappe du jump up) à 2,3 dB (batterie et lead de la tech house) ;
  - c'est la bande que joue un haut-parleur de téléphone : c'est la principale contrepartie pour l'écoute de Benjamin, et la phrase la présente comme partielle.
- **Correction** : « la part 1-4 kHz recule sur les 9 voix, de 0,6 à 2,3 dB ».

#### M3. Les extraits ne disent pas quel « avant » ils font entendre — **corrigé**
- **Corrigé en 262236b2** : « « Avant » = les sons intégrés, sans pack téléchargé (comme un téléphone qui n'a pas les packs des styles). Sur un téléphone qui a ces packs, l'avant sonne autrement. » La ligne « Extraits » des décisions n'a pas bougé, mais le rapport suffit.
- **Passages** :
  - rapport, « Extraits envoyés à Benjamin » : « même passage avant / après » ;
  - décisions, ligne « Extraits ».
- **Scénario** :
  - au banc, le FLAC d'un pack n'est qu'une tonalité de remplacement, et coupé, un pack d'essai installé ne passe pas devant le kit (`soundCategories`). L'« avant » joue donc les sons intégrés à l'appli. Le rapport ne le dit qu'en creux, dans « Charge du processeur » : « Remplacer la batterie synthétisée » ;
  - sur un téléphone qui a téléchargé les packs des styles, l'avant est autre : la batterie du jump up vient du pack « dnb », le charley ouvert de la techno du pack TR-909, la tech house de son pack ;
  - Benjamin peut juger l'essai contre un « avant » qui n'est pas celui de son téléphone, ou ne pas reconnaître son morceau dans l'extrait.
- **Correction** : dans « Extraits » et dans « Ce dont je ne suis pas sûr », écrire : « avant = les sons intégrés à l'appli, sans pack (au banc, les FLAC des packs ne sont qu'une tonalité) ; sur un téléphone qui a téléchargé les packs des styles, l'avant sonne autrement ».

### Mineur
- **m1. Un test placé dans un commit rouge y est vert.** Ouvert au second passage (le rapport n'a pas changé sur ce point) : il reste mineur.
  - Passage : rapport, « Rouge puis vert, à chaque changement : […] WAV de pack lus au banc : 599779ab / 94c546f5 ».
  - Scénario : `SoundTrialPacksTest` est arrivé dans le rouge 599779ab, mais il ne dépend que de `SoundTrial` (06efec42) et de `soundCategories`, que 94c546f5 ne touche pas. Il est donc vert sur son « rouge », et son rouge n'a jamais été montré : sur 2ee8f348, il ne compile pas. Il vérifie pourtant quelque chose : sans le kit de l'essai, le charley ouvert de la techno donnerait `pack:tr909`. La clé `sons.essai` de Gradle est entrée dans le même vert, sans test.
  - Correction : dire dans le rapport que `SoundTrialPacksTest` est un test de garde de 06efec42, vert dès son commit. Aucun code à changer.
- **m2. Un test suppose l'interrupteur coupé sans le couper.** Ouvert au second passage : il reste mineur.
  - Passage : `SoundTrialPacksTest#withoutTheSwitchTheRecipesPacksStayFirst`, qui ne coupe pas la propriété, alors que `@After` l'efface toujours.
  - Scénario : `-Psons.essai=1` vaut pour tous les tests de l'appli lancés ensemble. JUnit 4 joue `without…` en premier (ordre des hachages : −1612075058 avant −1057770999), donc le test échoue. Ensuite, le `@After` éteint l'essai pour le reste de cette JVM. Les scripts du banc filtrent leurs tests (`live-bench.sh` : `LiveSongRecorder` et `PreviewLevelBench`), donc ce cas n'arrive pas aujourd'hui.
  - Correction : garder la valeur d'avant en `@Before`, la remettre en `@After`, et couper la propriété au début de `without…`. Même schéma dans `SoundTrialTest`, que la clé Gradle n'atteint pas.
- **m3. Interrupteur mis sans packs d'essai, le banc joue un mélange.** **corrigé** en 262236b2, par écrit : « Ce dont je ne suis pas sûr » dit que le banc jouerait alors un mélange, et que l'essai se joue toujours avec `-Psons.essai=1` et `-Pbench.packs=<packs d'essai>` ensemble. Le banc ne le refuse pas encore : c'est accepté pour un essai.
  - Passage : `SoundTrial.variant`, `kit = drums.associateWith { pack }`, et les voix de kit retirées.
  - Scénario : si les packs `essai_*` ne sont pas installés, `soundCategories` met devant le pack du style. Le charley ouvert de la techno quitte alors le TR-909 pour le pack « techno », et la percussion de la tech house perd sa voix 10 : le banc n'entend ni le défaut, ni l'essai. Ce cas ne concerne que le banc.
  - Correction : que `LiveSongRecorder` refuse `ps.sons.essai=1` quand un pack `essai_<style>` manque, ou au moins l'écrire dans la doc de `SoundTrial`.
- **m4. `BenchFlac.registerWavs` lit tout le fichier et promet un refus non testé.** Commentaire **corrigé** en 262236b2 : « Other WAVs (24 bits, float, a broken header) stay unreadable on the bench (a phone would read the first two) ». Restent ouverts, et toujours mineurs : la lecture du fichier entier (`readBytes`) et l'absence de test du refus.
  - Passage : `pcm16` : `val b = f.readBytes()`.
  - Scénario : un WAV entier est chargé en mémoire avant le contrôle du format et avant la coupe à `MAX_SECONDS`. Un WAV d'une heure (690 Mo) déposé dans un dossier de pack irait tout entier dans la mémoire de la JVM de test. Aucun vrai pack n'a de WAV aujourd'hui.
  - Le commentaire « Other WAVs (another depth, a broken header) stay unreadable, as a FLAC that is no FLAC » n'est pas comme sur un téléphone : l'appli y lit un WAV 24 bits (« Raw WAVs can pass straight through at 8, 24 or 32 bits », `AudioDecoder`). Et aucun test ne couvre ce refus.
  - Correction : lire l'en-tête avec `RandomAccessFile`, puis seulement les `MAX_SECONDS` de données. Corriger le commentaire (« refusés au banc, lisibles sur un téléphone »), ou ajouter un test du refus.
- **m5. Les empreintes rougiront au prochain changement voulu.** Ouvert au second passage : il reste mineur.
  - Passage : `SoundTrialTest.BASE_HASHES` et le commentaire « hashes taken on 2cfb8a3a, before the trial ».
  - Scénario : après la fusion, tout changement voulu de l'une des 37 recettes, ou de `build`, rougit `offEverySongIsTheSameToTheByte`. Une fois les empreintes refaites, le commentaire devient faux.
  - Correction : dire dans le test comment refaire les empreintes, et que la garde durable est `offEveryStyleIsItsVeryRecipe` ; ou retirer les empreintes quand l'essai est tranché.
- **m6. NumPy manque à `THIRD_PARTY.md`.** **corrigé** en 262236b2 : une ligne NumPy (BSD-3-Clause, scripts et outils de mesure, pas dans l'appli).
  - Passage : la ligne SciPy, qui cite `trial_drums.py`.
  - Scénario : le script utilise aussi NumPy (BSD-3-Clause), absent du tableau. C'était déjà le cas pour `instrument_pack.py`.
  - Correction : ajouter une ligne NumPy (scripts seulement, pas dans l'appli).

rouge constaté : 862f5cd5236b015f9b8fbe02c649ff0e7727b74b : rejoué sur fbf6f260fe8d706c4fa888b355b388de4bf32072 : `gradle-file.sh :project-model:test --tests '*SoundTrialTest'` dans un worktree jetable sur fbf6f260 (le test neuf, le code d'avant) : 10 tests, 4 échecs sur leurs assertions, aucune erreur de compilation : « jump_up: its EQ », « jump_up strip 2 lost Insert(type=13, params=[0.2594, 0.5833, 0.2416, 0.0]…) » (le `BASS_BODY`), « tech_house lead delay [0.2, 0.3] -> [0.25, 0.3] », « mids and highs lifted on the voice ».
rouge constaté : b1d2245d988115755476a1738af96db22f6f1237 : rejoué sur 3888b0e611b883f63c380a47404038f2cba9100d : `test_trial_drums.py` de 3888b0e6 avec `--script` sur `git show 3888b0e6~1:scripts/packs/trial_drums.py` (identique à celui de 3888b0e6) : « 2028 checks, 805 failed » (crêtes par sorte, bandes du kick, de la caisse et du clap, aigus du métal, queues, colonnes absentes de mesures.json).
rouge constaté : 7764266a3f426d2c306784148a6958c69e4f2ddb : rejoué sur db7b3ea82c9a7b05112ca3fe4b42aee77a2b6f19 : `test_trial_drums.py` de db7b3ea8 avec `--script` sur `git show 7764266a~1:scripts/packs/trial_drums.py` : « 2041 checks, 12 failed », les 12 kicks sous le corps voulu (−21,5 à −23,9 dB FS²·s dans 60-250 Hz, au moins −18,5 voulu).
rouge constaté : e3835d455b3d5ee625877a47ada9f02a7fa4aa7f : rejoué sur 2ef1ceaef0115eafa2e2b1f460537ecc9932dc69 : `gradle-file.sh :project-model:test --tests '*SoundTrialTest'` dans un worktree jetable : 9 tests, 3 échecs sur leurs assertions, aucune erreur de compilation : « jump_up: one of them parallel, its loudness held », « a presence lift on the voice », « jump_up lead delay [0.2, 0.15] » (celui-ci après l'élargisseur et la réverbe passés : voir G2).
rouge constaté : fb505a5b0cc735c26b6cb02670e8ed6e819ce4d8 : rejoué sur 56bf3f55698bf8a965a057af1552c0b612ee4b9a : `test_voix.py` et `voix.py` de 56bf3f55, hors du dépôt : code 1, « FAIL 41.2 Hz rms None vs -16.7 ».
rouge constaté : 06efec427988f08fd8e7817f7300fc11fe5a035c : rejoué sur 2ee8f3483402c9e101bb172a1391559fdf8b3641 : 3 tests sur 6 échouent sur leurs propres assertions, aucun échec de compilation : `onTheBundledModernSoundsPlay` (l. 87, « expected Accords future bass but was Nappe »), `onPadsAndLeadsGetTheOtt` (l. 77, « jump_up pad ») et `onTheThreeStylesTakeTheirTrialDrumPacks` (l. 62, kit `essai_jump_up` attendu, `dnb` trouvé). Les deux tests « off » sont verts sur le code de la base.

Verdict : 0 bloquant, 0 grave — fusion possible
