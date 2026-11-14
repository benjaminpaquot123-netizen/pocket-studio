# Audit ultra, tour 3 : zone 4 (Enregistrement)

Sièges : C a (rappeur, pt-BR), C c (débutant, hindi), E c (chanteuse-guitariste, anglais), F a (chasseur de promesses). Items 23 à 27 de la synthèse.
Tests sans banc : `audiotracks/Zone4Round3Test` (écran des pistes audio, interrupteur de son, vue-modèle et appli entière via `AppHarness`, moteur de remplacement qui avance comme celui d'un téléphone).

| N° | Verdict | Gravité | Ce qui est fait | Test |
|---|---|---|---|---|
| 23a | **banc** | — | Le ● du haut et « Enregistrer » de la carte appellent tous deux `recordAudio` : le message « moteur pas démarré » ne sort que sans moteur, jamais sur un téléphone. Le banc reconnaissait le ● en mode micro au mot « micro » lu par `ScreenText.under`, qui ne lit pas la description de l'icône : il ne gardait pas son moteur. Banc corrigé : `HumanTester.topRecordIsTheMicrophones` (nom exact du ● dans la langue du siège). | preuve par lecture des deux chemins + correction du banc |
| 23b | appli | moyen | (C a) Le ● du haut, cible « tout le morceau » alors qu'« Áudio 1 » a une prise de partie, crée une piste « Voz » (règle « jamais une autre prise remplacée ») et y enregistre hors écran : la prise semblait partir nulle part. Désormais l'état de prise est dans la carte de la piste qui enregistre et ramené à l'écran (voir 24). Prise « perdue » : banc (moteur lâché, 23a). | `theTakesStateSitsInTheTrackThatRecords` |
| 24a | appli | moyen | Décompte, niveau, « Arrêter et garder » et « Annuler » dans la carte de la piste qui enregistre (plus en haut de page), ramenés à l'écran au début de la prise (`BringIntoViewRequester`). | `theTakesStateSitsInTheTrackThatRecords` (rouge avant) |
| 24b | **banc** | — | « Décompte : 1… » pour 2 mesures : `BenchEngine` ne donne pas `playPosition` (toujours −1 → 1 temps). Le vrai moteur rend la position négative pendant le décompte (8 temps pour 2 mesures). À faire côté banc : `playPosition` négative pendant `countInMs`. | lecture de `NativeEngine.playPosition` |
| 25a | appli | moyen | Toucher une prise la fait jouer **et** fait écouter la piste seule en boucle (`hearTake`) ; bouton « ▶ Écouter seule » à côté de « Prises (n) » ; la boucle continue d'une prise à l'autre ; s'arrête en quittant l'écran (`HoldAlone`) ou en enregistrant. Rien pendant une prise. | `aTakeTouchedIsHeardAlone`, `theViewModelPlaysTheTakeAlone` |
| 25b | appli | mineur | « Prise précédente » supprimé : c'était une bascule entre deux prises ; le sélecteur numérote déjà toutes les prises. | `noPreviousTakeThatSwapsBack` (rouge avant) |
| 25c | appli | mineur | Une seule durée par prise : la puce dit « Prise 2 · 8,5 s » comme le titre sous la forme d'onde. | `oneDurationPerTake` (rouge avant), `RecordRound2Zone4Test` mis à jour |
| 26a | appli | moyen | La demande système de notifications vient **après** la première prise (plus au moment où elle commence). La langue de cette fenêtre est celle du téléphone (anglais = banc). | `theNotificationsWaitForTheTakesEnd` (rouge avant) |
| 26b | appli | moyen | Racine : depuis le tour 2 (n° 17), un nouveau son renommait « Mélodie » en « FM piano » ; « Ma mélodie » ne retrouvait plus la mélodie et prenait le son du style (FM bell). Les noms de rôle (Mélodie, Basse, Accords, Arpège, Nappe, Lead, Contre-chant, Ma mélodie, Sub, Voix, Batterie…) ne suivent plus le son (`DefaultNames.isRole`) ; « Synthé », « Piste 2 », un nom de son le suivent toujours. Même cause réglée pour le Guide (« Le son de la mélodie » cherche aussi par nom). | `myMelodyTakesTheSoundChosenForTheMelody` (rouge avant), `Zone3Round2Test` |
| 26c | **banc** | — | Touches tapées non enregistrées : sur le banc `playPosition` = −1, chaque note tombe « avant le premier pas » et est jetée. Avec un moteur qui avance, trois touches de 80 ms = trois notes. | `theKeysShortTapsAreRecorded` |
| 27 | appli (revient) | mineur | Pourquoi le tour 2 n'a pas tenu : il avait renommé l'interrupteur (« Son de X », activé quand on entend) mais le petit mot sous l'icône (« Muet ») était fusionné dans ce que lit TalkBack : « Mute, off ». Le mot est maintenant pour les yeux seulement ; TalkBack lit « Son de X, désactivé » dans toutes les langues. Le mot affiché devient un état là où c'était un ordre : en « Muted », pl « Wyciszona », es « Mudo », nl « Stil », id « Senyap », ar « مكتوم ». | `aMutedTracksSwitchReadsTrue` (rouge avant), `MuteSoloEverywhereTest` mis à jour |

## Méthode
- Tests écrits avant les corrections et lancés sans elles (voir commit) puis avec.
- Aucune nouvelle chaîne (« Écouter seule » existait) ; seules six traductions de « Muet » changent.

## Reste
- Banc : `BenchEngine.playPosition` (24b, 26c) — à l'équipe banc.
- C a 38 : toucher « Voz » dans Minha música ouvre la page de toutes les pistes audio, en haut (Áudio 1) : pas de défilement vers la piste touchée.
- Le choix « une nouvelle piste plutôt que remplacer la prise d'une autre partie » reste ; il est maintenant visible, pas expliqué en mots.
