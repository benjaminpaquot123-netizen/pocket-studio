Relu : e255b34fe86dd330b999283b4545088545d4a0d7

Relecture neuve du diff `8801b1dc..HEAD` (branche eq/tour-9-base-rouges). Tests ciblés relancés : TutoReadabilityBudgetTest 3/3, Tour9L5bTutoWordsTest 13/13, I18nCatalogTest 13/13, ModelTextsTest 2/2, tous verts. Les 5 JSON se chargent sans erreur ; rien d'autre n'est touché (hors le rapport du lot).

## Bloquant
Aucun.

## Grave
Aucun. Aucun test affaibli : FACILE_FROZEN 409→400 et PRO_RATCHET 3725→3722 ne font que baisser ; la liste d'exceptions perd 9 lignes (les 9 « gap ja », toutes réellement rentrées, sinon everyFacileExceptionStillMisses serait rouge) et n'en gagne aucune ; l'ajout dans le test Pro n'écrit qu'un fichier de build en plus.
L'entrée « TrackBar.kt: it » est vraie : le seul `tr(it)` de TrackBar.kt (ligne 95) traduit `near`, un `it.name` de SoundLibrary.SYNTH ou SoundLibrary.PLAITS, et ModelTextsTest ajoute bien SoundLibrary.SYNTH/PLAITS/ACID.name à ses textes vérifiés dans les 13 langues.

## Mineur
- hi (3 consignes Roulement) : « बिल्ड-अप का अंतिम “Snare” » perd « dernière mesure » et « coup » ; le sens reste juste (le dernier coup de la Montée est forcément dans sa dernière mesure), et « sous Roulement » devient « dans Roulement » ; « टैप » est bien après la dernière virgule.
- de (3 consignes Roulement) : « dann tippe „×4“ unter „Roll“ » sans « auf » est familier mais compréhensible ; « tippe » est bien après la dernière virgule ; partie, dernière mesure et coup gardés.
- ja (6 consignes gap) : les noms de partie perdent leurs 「」 (ビルドアップ, プレサビ, ブレイク), un peu moins raccord avec les consignes voisines qui les gardent, et « それぞれ » (chacune) saute ; la partie, la dernière mesure et les cases barrées (取り消し線のマス) restent, aucun 赤い× (f090 respecté).
- I18nCatalogTest : la clé « TrackBar.kt: it » couvre tout futur `tr(it)` du fichier, pas seulement celui-ci (même limite que les entrées voisines du même genre).
- T9PanelTargetTest laissé rouge par le lot (dit « mise en page, pas texte ») : pas vérifié ici, à traiter par le lot qui possède cette zone.

Verdict : 0 bloquant, 0 grave
