# Tour 7 — lot « guide2 » (lot 3, le Guide et la coquille) : rapport

Base : `7948ecb9`, tirée de `ccr-80af1a1b-mn0wec`. Fichiers : `guide/GuidePanel.kt`, `ui/AppSpaces.kt`, `mixer/Console.kt`, `mixer/MixerScreen.kt`, des tests neufs sous `guide/` et `mixer/`, un rejeu neuf. Décisions : `docs/decisions/tour-7-guide2.md`.

tests: com.pocketstudio.app.guide.Tour7GuideTargetInPanelTest com.pocketstudio.app.guide.Tour7PlusTilesAboveGuideTest com.pocketstudio.app.mixer.Tour7MixEntryAboveGuideTest com.pocketstudio.app.guide.Tour7Guide2LookTest *Guide*

## Fait
- **D2 (G1, grave)** [3a #11] : en hi, phone360 à 130 %, Mode Simple, Guide ouvert à « एक साउंड बदलो » : le bouton nommé par la consigne (« ↗ मेलोडी का साउंड ») est dans le panneau du Guide, juste sous la consigne, en grand bouton orange, et le panneau défile pour le montrer. Plus besoin de fermer le Guide ni de toucher « ↓ नीचे और है ».
- **D3, page Mix** [3b #42] : en fr, phone à 100 %, Mode Pro, « Voir les courbes » et « Ce qui joue » sont entiers au-dessus de la barre du Guide (faders tassés jusqu'à 80 dp).
- **D3, page Plus** [3a #62] : en hi, phone360 à 130 %, Mode Simple, « मेरी लाइब्रेरी » et « मदद » ne sont plus coupées par la barre : une tuile est entière au-dessus, ou entièrement sous le pli.

## Laissé
- D3 Mix sur phone360 à 130 % : la console et les deux boutons ne tiennent pas ensemble au-dessus de la barre même à 80 dp ; la page défile.
- D3, consigne de la Guía en espagnol [2c #9 et #16] : dans la découpe du Remix, fichiers du lot « découpe ».
- D7 (U27) : conflit avec le Remix à une touche depuis Sons (décision du 2026-10-24, `HumanRemixJourneyTest`) et avec « Basse » visible sur 360 dp au Mixer (`KeyActionsGestureTest`) ; à trancher par le chef.

chemin: Mon morceau › Guide ouvert › « एक साउंड बदलो » (hi, phone360, 130 %, Simple) | test: Tour7GuideTargetInPanelTest#hindiSmallPhoneAt130
chemin: Mon morceau › Guide ouvert › « Changer un son » (fr, phone, 100 %, Simple) | test: Tour7GuideTargetInPanelTest#frenchPhoneAt100
chemin: Mix › Voir les courbes, Ce qui joue (fr, phone, 100 %, Pro) | test: Tour7MixEntryAboveGuideTest#phoneAt100
chemin: Mon morceau › ⋮ « और » (hi, phone360, 130 %, Simple) | test: Tour7PlusTilesAboveGuideTest#hindiSmallPhoneAt130
chemin: Mon morceau › ⋮ « Plus » (fr, phone, 100 %, Simple) | test: Tour7PlusTilesAboveGuideTest#frenchPhoneAt100

corrige: 3a-11 | gravité: grave | rejeu: t7-g1-guide-son
corrige: 3b-42 | gravité: moyen | rejeu: aucun, prouvé par Tour7MixEntryAboveGuideTest#phoneAt100
corrige: 3a-62 | gravité: moyen | rejeu: aucun, prouvé par Tour7PlusTilesAboveGuideTest#hindiSmallPhoneAt130

## Rejeux

t7-g1-guide-son : oracle tenu
