# Tour 5 — lot « kick » : relecture neuve
Relu : ddc8cf11ae84623dfb959cb17ecff959058d536d

Relu : diff des 4 commits rouge/vert (bb5ad266~1..5ce27057~1), rapport, décisions. Lancés ici : les 5 tests `sk_*` du moteur (tous verts, +6,1 dB / +5,0 dB au modèle 300 / 200 Hz).

## Bloquant
Aucun.

## Grave
- **corrigé** (vert 2d8401e0 : `Engine::applySpeakerOutput`, coupé d'un coup tant qu'une prise `ResampleArm` est armée, revient au lâcher ; test `sk_no_speaker_kick_in_a_take`, relu et rejoué vert ici ; non rejoué rouge sur le parent, je n'ai lu que le diff) : `engine/src/main/cpp/core/Mixer.cpp:684` + `Engine.cpp:1760` (`captureResample`) : « Enregistrer ce que je joue » (`PlayedTake.kt:57`, `NativeEngine.resampleStart`) enregistre la sortie du moteur direct, après le master. Au haut-parleur, la prise contient donc les harmoniques et le clic du kick ; elle est rangée sur un pad / dans la grille, sauvegardée avec le projet, puis rejouée au casque ou exportée avec ce son cuit dedans. Contredit « casque, Bluetooth, export identiques » et « jamais à un export ». Aucun test ne le couvre. Piste : suspendre le traitement pendant une prise (ou l'appliquer après la capture).

## Moyen
- `Headphones.kt:52` / `NativeEngine.kt:560` : `Headphones.watch` est appelé dans `Application.onCreate` ; `setSpeakerOutput` passe par `nativePost` → `host()` (`JniBridge.cpp:25`), qui crée l'hôte audio (Engine entier) au démarrage de l'appli, alors que `nativeRetryOutput` évite justement de le créer (commentaire `JniBridge.cpp:42`). Coût de démarrage et mémoire non mesurés ; aucun test ne regarde ce chemin réel (`watch()`).
- `Mixer.cpp:684` : quand le pad 1 n'a pas de tranche, le traitement s'applique à toute la tranche Batterie (808, caisse claire, charleys : clic x2 sur 1,2-4,5 kHz, harmoniques de tout le grave). « La basse n'est pas touchée » (SpeakerKick.h, rapport) n'est vrai que pour la tranche du pad 1 ; `sk_kick_in_the_drums_strip` ne regarde que le gain du kick, pas l'effet sur le reste.
- `SpeakerKick.cpp:55-57` : harmoniques ajoutées x4 x enveloppe, en mono pré-fader, sans plafond propre ; seul le limiteur du master les tient (crête mesurée −1,2 dBFS par l'auteur, hors dépôt). Techno +1,6 LUFS au mix : le morceau devient plus fort au haut-parleur, ce que la décision (« sans changer le niveau ») ne dit pas franchement.

## Mineur
- `speakerkick_tests.inc:58` `sk_off_is_untouched` compare « jamais envoyé » à « envoyé 0 » : deux rendus sans traitement ; prouve peu (il rougit seulement si 0 déclenche le traitement). Pas de test « off après on » au niveau mixer, ni de solo.
- `SpeakerKickRouteTest` passe par `Headphones.route` + un sink : prouve le mapping et la cible 0, pas le `watch()` réel ni le rejeu de l'état après redémarrage du flux.
- Rapport : « 0 allocation en 30 000 rappels », « identique au bit près sur 4 styles », LUFS/crête : mesures faites hors dépôt, non rejouables ici (le rapport le dit). Lues par moi : code sans allocation, verrou, log ni JNI ; commande passée par la file (applyCommand) ; `DenormalGuard` actif ; `setOn` nettoie les filtres au démarrage depuis le repos.

Relecture du correctif (f2cc6f07 rouge, 2d8401e0 vert, ddc8cf11 rapport) : logique saine (arm coupe sans fondu, finishResample et arm nul rallument), les 6 tests sk_* sont verts. Le moyen « hôte créé tôt » reste noté, non corrigé.

Verdict : 0 bloquant, 0 grave
