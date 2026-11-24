# Relecture tour 9 audio1
Relu : 8656f50a2

Base : `origin/ccr-80af1a1b-mn0wec`, diff `FETCH_HEAD...HEAD` hors `docs/` (8 fichiers).

## Points vérifiés, sans défaut
- Thread audio : `bufferFramesLocked` n'est appelé que dans `openOutputLocked` (tous ses appelants tiennent `lock_` : start, reopen, onErrorAfterClose, ouverture duplex) et dans `setBufferBursts` (sous `lock_`, `output_` testé avant). Jamais depuis `onAudioReady`. Aucune allocation, verrou, log ni JNI ajouté dans le callback.
- `output_` nul : dans `openOutputLocked`, l'appel vient après le `return false` d'un échec d'ouverture, donc `output_` est valide ; dans `setBufferBursts`, il est protégé par `if (output_)`.
- JNI : `private external fun nativeSetBufferBursts` correspond exactement à `Java_com_pocketstudio_engine_NativeEngine_nativeSetBufferBursts` ; plus aucune autre référence à l'ancien symbole ; pas de `RegisterNatives`. Même schéma que `nativeStreamInfo` (privé, déjà en place).
- Ordre d'initialisation de `NativeEngine` : aucun initialiseur placé avant `autoBuffer` n'appelle `setBufferBursts` ni `streamInfo` ; l'objet est entièrement initialisé avant tout appel extérieur. Pas de NPE possible sur `synchronized(autoBuffer)`.
- Boucles et oscillation : `AutoBuffer` ne descend jamais seul, au plus un cran par 10 s, s'arrête à 6 ; `setBufferSizeInFrames` ne redémarre pas le flux. Une réouverture (`restarts` change, ou `xruns` recule) ne fait que reprendre une base. Pas de boucle de redémarrage.
- Accord C++ / Kotlin : `bufferFramesLocked` et `OutputBuffer.frames` donnent le même résultat (bornage 1..8 fait côté C++ dans `setBufferBursts`, même test « rapide » `LowLatency && burst <= rate*4/1000`, même plancher 40 ms arrondi au burst supérieur, même plafond à la capacité, même 0 si burst <= 0).
- `synchronized` : `userBursts` et l'état d'`AutoBuffer` ne sont touchés que sous le moniteur ; `nativeStreamInfo` est lu hors moniteur. Pas d'inversion d'ordre de verrous possible (le C++ ne rappelle jamais Kotlin sous `lock_`).

## Mineur
1. Plancher 40 ms aussi sur le haut-parleur de certains téléphones : un burst à faible latence de plus de 4 ms (240 ou 256 trames à 48 kHz, courant hors MMAP) est classé « lent ». Le réglage « Rapide » y passe de ~10 ms à 40 ms, sans réroutage. Le téléphone de Benjamin (burst 96) n'est pas touché. Piste : ne juger « lent » que sur `!lowLatency`, ou relever le seuil à ~6 ms.
2. Les tests du plancher vérifient la copie Kotlin `OutputBuffer`, qui n'est appelée nulle part en production ; le code C++ réel n'a aucun test, et rien n'empêche les deux de diverger un jour. Aujourd'hui ils concordent (relu ligne à ligne).
3. Le câblage dans `NativeEngine` (remise à zéro sur nouveau choix, `maxOf(user, auto)`, appel natif) n'est pas testé ; seul `AutoBuffer` l'est. `AutoBufferTest.userChoiceAboveTheBoostWinsAndANewChoiceResets` appelle `reset()` à la main au lieu de passer par `setBufferBursts`.
4. Le cran automatique appelle `nativeSetBufferBursts` depuis la boucle de lecture du flux (fil principal), qui prend `lock_` en attente bloquante, alors qu'`info()` évite exprès d'attendre (`try_to_lock`). Si une réouverture tient `lock_` au même moment, le fil principal attend la fin de l'ouverture. Rare (il faut un cran pile pendant une réouverture), mais à garder en tête.
5. Sur un chemin lent déjà relevé à 40 ms, les crans 4 puis 6 bursts (burst 96 : 8 et 12 ms) restent sous le plancher : ils sont gaspillés sans effet. Sans danger.
6. Le cran automatique est invisible pour l'utilisateur (l'écran Réglages affiche toujours son choix), et rechoisir la même valeur ne l'annule pas (`reset` seulement si la valeur change).

Verdict : 0 bloquant, 0 grave
