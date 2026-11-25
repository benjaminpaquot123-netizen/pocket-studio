relu : 85ecb362
verdict : OK

## Ce qui a été vérifié sur la fusion (41430816 + 85ecb362)
- Delta du lot = `git diff origin/eq/ci-int-styles HEAD` : 80 fichiers, seulement les ajouts L12 (moteur, modèle, panneau, préréglages, guides, i18n, tests, docs du lot). Aucune ligne de l'intégration retirée ou réécrite par la fusion.
- JSON : tous les fichiers JSON du delta (guides de style, guide.json et model.json des 14 langues) se lisent sans erreur et sans clé en double.
- Guides : `scripts/guides/make_guides.py` (fusionné) régénéré dans un dossier à part : sortie identique, octet pour octet, à `project-model/src/main/resources/guides` et `i18n` (aucun écart avec les styles de l'intégration).
- Ordinaux : l'intégration n'ajoute aucun effet. Kotlin `EffectType` : BASS_EXCITER=29, TRANSIENT=30, DRUM_BUS=31, MID_SIDE_EQ=32 ; C++ `InsertSlot::Type` (Effects.h:827-829) : mêmes numéros, kTypes=33. Cohérent.
- Moteur : les trois effets sont branchés dans prepare / resetState / setParam / process (Effects.cpp:1186-1381), aucun oubli.
- Tests hôte : aucun nom de test ni de fonction en double introduit (le seul doublon de nom, `testStemsAddUpToTheMix`, vient de l'intégration et vit dans l'espace `masterpro`, sans conflit). `l12_tests.inc` inclus et enregistré une fois (engine_tests.cpp:8182, 8197).
- i18n : toutes les nouvelles chaînes (`Attaque`, `Bus batt.`, `Mono`, `Coupé`, noms de réglages, descriptions, préréglages) ont leur clé dans model.json.

## Thread audio (ExtraEffects.cpp:656-870)
- Ni allocation, ni verrou, ni log, ni JNI : tampons de DrumBus sur la pile (9 × 64 floats, plus ceux du DriveShaperPair), anneau `postRing_` en std::array membre.
- NaN / infini en entrée : silence et remise à zéro des suiveurs (TransientShaper, MidSideEq) ou entrée lue comme 0 (DrumBus) ; sorties passées par `finiteOr0`. `ratioDb` coupe sous -120 dBFS (pas de log de 0) ; DrumBus a un plancher à 1e-3 pour la division.
- Paramètres : `setParam` borne l'index et la valeur, refuse les non-finis.

## Sauvegarde et chargement
- Effets ajoutés en fin d'énumération : les anciens projets gardent leurs effets aux mêmes numéros. Valeurs par défaut de taille 4 partout (DRUM_BUS et MID_SIDE_EQ complétées par un 0 inutilisé).

## Constats
- Bloquant : aucun.
- Grave : aucun.
- Moyen : aucun.
- Mineur (neuf) : make_guides.py:1096 (et les guides générés) « le kick claque plus, sans monter le volume » : au réglage par défaut (Attaque 0,65), les coups montent d'environ 3 dB. Mieux : « sans monter le volume moyen », comme la description de TRANSIENT (Effects.kt:382).

## Mineurs laissés (pour un lot suivant)
1. ExtraEffects.cpp:671 : slowR_ = fastR_, gain d'attaque figé pendant la retombée des sons de moins de 40 ms (hat, clap).
2. ExtraEffects.cpp:763 : sous le plancher, le bus batterie relève encore un petit signal jusqu'à +12 dB ; commentaire « silence is not lifted up » trompeur.
3. ExtraEffects.cpp:798 : « Graves mono » à 0 garde un passe-haut 10 Hz d'ordre 4 sur les côtés.
4. Latence du bus batterie non compensée (comme Saturation).
5. Effects.h:789 : DrumBus::p_ de taille 4 pour 3 paramètres.
6. EffectsPanel.kt:76 : nom court « Mono » ambigu (« Gr. mono » plus clair).
7. Pas de test Kotlin des deux nouveaux préréglages.
8. hi/model.json : « घनी, थोड़ी तेज़ ड्रम » met « ड्रम » au féminin ; mieux : « घना, थोड़ा तेज़ ड्रम ».
9. (neuf) make_guides.py:1096 : « sans monter le volume » → « sans monter le volume moyen ».

Verdict : 0 bloquant, 0 grave
