# Tour 9, lot audio1 : rapport

Branche : eq/tour-9-audio1, partie de ccr-80af1a1b-mn0wec.
Défauts visés : C4 (coupures quand le son part vers Android Auto), puis le tampon automatique. Hors lot : C5, C2.

État : tenté (pas de voiture ici ; Benjamin confirmera avec le test guidé « Changement de sortie »).

Causes :
- C4 : quand la sortie change (voiture, Bluetooth), le son se rouvre sur un chemin lent ou partagé (rafales de 10 à 40 ms), mais le tampon restait à 2 rafales, réglé pour le haut-parleur rapide : il se vidait, d'où des coupures.
- Tampon automatique : rien ne réagissait quand les coupures s'accumulaient ; il fallait aller changer « Tampon » à la main.

Ce qui change :
- Sur un chemin lent (pas de basse latence, ou rafale de plus de 4 ms), le tampon a un plancher de 40 ms, arrondi à des rafales entières et plafonné à la capacité. Sur le haut-parleur rapide, rien ne change.
- Trois coupures en 10 s montent le tampon d'un cran (2, 4, 6 rafales), sans redémarrer le son ; un changement toutes les 10 s au plus ; il ne redescend jamais tout seul pendant la séance ; un nouveau choix dans les réglages le remet à zéro ; un son rouvert (changement de sortie) ne compte pas comme des coupures.
- Fichiers : engine/src/main/cpp/android/AudioHost.{h,cpp}, JniBridge.cpp, engine/src/main/java/com/pocketstudio/engine/{OutputBuffer,AutoBuffer,NativeEngine}.kt. Rien dans le cœur du moteur.

Décisions : docs/decisions/tour-9-audio1.md.

Captures : aucune (pas d'écran touché).

tests: com.pocketstudio.app.playback.OutputChangeBufferTest com.pocketstudio.app.playback.AutoBufferTest

chemin: Réglages > Sortie audio (changement de sortie vers la voiture) | test: OutputChangeBufferTest#rerouteToCarOrBluetoothGetsASafeBuffer
chemin: Réglages > Sortie audio (haut-parleur) | test: OutputChangeBufferTest#fastSpeakerPathKeepsTheChosenLowLatency
chemin: Lecture avec coupures répétées | test: AutoBufferTest#repeatedUnderrunsRaiseOneNotch
chemin: Lecture après un changement de sortie | test: AutoBufferTest#aReopenedStreamIsNotCountedAsDropouts
