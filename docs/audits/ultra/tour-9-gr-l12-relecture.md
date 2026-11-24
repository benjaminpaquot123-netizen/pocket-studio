relu : 245a3728
verdict : OK

## Relectures précédentes (6e835a7a OK, beed4fec À CORRIGER) : résumé
- Thread audio propre : ni allocation, ni verrou, ni log, ni JNI ; tampons DrumBus sur la pile ; dénormaux couverts ; NaN en entrée = silence et remise à zéro.
- Ordinaux Kotlin/C++ cohérents (TRANSIENT=30, DRUM_BUS=31, MID_SIDE_EQ=32, kTypes=33) ; anciens projets intacts ; JSON valides ; tests host l12 verts et qui mesurent vraiment (bus batterie : pic +0,00 dB, moyenne +1,34 dB).
- Moyen (texte du préréglage « Corps ») : CORRIGÉ dans 245a3728.
  - 1er texte « le kick garde son pic » : faux (l'Attaque à 0,65 monte les coups d'environ 3 dB).
  - 2e texte « au même volume moyen » : faux (l12_tests.inc:96-113 exige une moyenne +1 à +3 dB).
  - Texte final « Des coups plus francs, une batterie plus dense et un peu plus forte. » : vrai.

## Ce delta (beed4fec -> 245a3728)
- Vérité : le préréglage met TRANSIENT (attaque 0,65 : coups plus francs, plus forts) puis DRUM_BUS à (0,5 ; 0,6 ; 0,5), ce sont exactement ses réglages par défaut (Effects.kt:392), ceux que mesure testDrumBus : moyenne +1,34 dB, dans la fourchette 1 à 3 dB. « Un peu plus forte » est juste et prudent.
- Même clé dans FxPresets.kt:90 et les 14 model.json (ar, de, en, es, hi, id, it, ja, ko, nl, pl, pt-BR, tr, zh-CN) ; l'ancienne clé a disparu partout ; 14 JSON valides.
- Traductions fidèles (plus franc, plus dense, un peu plus fort) ; coréen en 해요체 (« 드럼이에요 »), aucun 합니다.
- Rien d'autre de changé, hors le rapport L12 mis à jour (docs/audits/ultra/tour-9-gr-l12-rapport.md).

## Mineurs laissés (pour un lot suivant)
1. ExtraEffects.cpp:671 : slowR_ = fastR_, gain d'attaque figé pendant la retombée des sons de moins de 40 ms (hat, clap).
2. ExtraEffects.cpp:763 : sous le plancher, le bus batterie relève encore un petit signal jusqu'à +12 dB ; commentaire « silence is not lifted up » trompeur.
3. ExtraEffects.cpp:798 : « Graves mono » à 0 garde un passe-haut 10 Hz d'ordre 4 sur les côtés.
4. Latence du bus batterie non compensée (comme Saturation).
5. Effects.h:789 : DrumBus::p_ de taille 4 pour 3 paramètres.
6. EffectsPanel.kt:76 : nom court « Mono » ambigu (« Gr. mono » plus clair).
7. Pas de test Kotlin des deux nouveaux préréglages.
8. (neuf, cosmétique) hi/model.json : « घनी, थोड़ी तेज़ ड्रम » met « ड्रम » au féminin ; l'ancien texte disait « घना ड्रम » (masculin). Mieux : « घना, थोड़ा तेज़ ड्रम ».

Verdict : 0 bloquant, 0 grave
