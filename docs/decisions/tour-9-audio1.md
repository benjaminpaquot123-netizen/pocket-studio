# Tour 9, lot audio1 : décisions

- Plancher de 40 ms sur un chemin lent (pas LowLatency, ou rafale > 4 ms) : couvre les rafales des sorties voiture et Bluetooth (10 à 40 ms) avec au moins une rafale de marge. Le haut-parleur rapide garde le choix de l'utilisateur. Retour arrière : enlever le plancher dans AudioHost::bufferFramesLocked (et OutputBuffer.kt, son miroir testé).
- Tampon automatique : 3 coupures en 10 s = un cran (2 → 4 → 6), 10 s entre deux changements, jamais de descente automatique (évite le va-et-vient), remise à zéro par un nouveau choix de l'utilisateur. Appliqué par setBufferBursts (pas de redémarrage du flux). Retour arrière : dans NativeEngine.streamInfo(), ne plus appeler autoBuffer.update.
- Le symbole JNI devient nativeSetBufferBursts ; setBufferBursts (Kotlin) garde la même signature pour l'appli.
