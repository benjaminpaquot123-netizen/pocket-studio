# Lot « langues » : rapport de l'auteur (tour 5, zone « langues et lisibilité »)

Branche `ccr-6f42e003-295715`, partie de `ccr-80af1a1b-mn0wec`. But : réparer la zone avant le tour 6 (tour 5 : 0 promesse tenue sur 8).
Les cinq langues du tour 6 : français, allemand, arabe, hindi, chinois (`PLAN-TOUR-6.md` n'est pas encore sur la branche : décision dans `docs/decisions/tour-5-langues.md`).

tests: *LanguagesScanTest *RtlPunctuationTest *ThumbUndoStaysTest *WizardGridDirectionTest *SoundTileNameOnceTest *PartNameFieldLanguageTest *SongGuideTest

Tests du module `project-model` (le portail ne rejoue que l'appli : à lancer à la main, `:project-model:test`) : `SimpleWordsNonLatinTest`, `GermanNoteNamesTest`, et autour `EverydayPadWordsTest`, `I18nCatalogTest`, `LexiconWordsTest`, `NoteNamesTest` (verts).

## Les 8 promesses non tenues et ce qui est fait

| Promesse | Constat du tour 5 | Fait | Preuve (rouge → vert) |
|---|---|---|---|
| t5-Aa-4 (hindi) | « SNARE », « Kick », « Octave », « Crash », « Solo » en lettres latines en Simple | glossaire par langue appliqué en Mode simple (`mots_simples.json`) : les ~50 mots du métier listés (pads, parties, sons, effets courants) | `SimpleWordsNonLatinTest` : ces mots, dans tous les textes des catalogues (3 989 textes rouges → 0) ; `LanguagesScanTest` : aucun mot latin sur les 5 écrans du Simple vus en arabe, hindi, chinois, hors « Lead » (seul nom tapé du projet de test) et noms de fonctions (Acid 303, Remix, Live, Pad Live). **Limite** : d'autres mots latins restent dans les catalogues (≈ 2 000 textes arabes, 2 260 hindi, 1 400 chinois, surtout Pro, tutos et mots rares : relu par le relecteur) ; seuls les mots listés sont garantis. |
| t5-Bd-1 (arabe) | « Sub bass », « Loop », « Pads », « Kick », « Snare », « Mix », « Bridge », « Groove », « باص music » | même glossaire (arabe, avec l'article « الـ » fondu) ; « باص تناظري تناظري » ne se répète plus | idem ; `SoundTileNameOnceTest` |
| t5-Fc-ecran-1 (japonais) | « ミックス », « フィル »… | glossaire japonais et coréen pour les restes en lettres latines (Drop, Ride, swing…) ; les mots en katakana restent (mots courants en japonais) | `SimpleWordsNonLatinTest` |
| t5-Bd-2 (arabe) | flèches → dans la phrase, liste avec « , », grille de l'assistant à l'envers | « ← » et « ، » dans les phrases arabes ; la grille de l'assistant de gauche à droite comme l'onglet Rythme | `RtlPunctuationTest`, `WizardGridDirectionTest` |
| t5-Bd-3 (arabe) | « إعادة » prend la place de « تراجع » | la place de ↷ est gardée (vide et muette) : ↶ ne bouge plus | `ThumbUndoStaysTest` (rouge : 44 dp de saut, en arabe et en français) |
| t5-Bc-3 (allemand) | notation B♭/B au lieu de B/H | H et B en allemand (C D E) | `GermanNoteNamesTest` (project-model) |
| t5-Fa-3 (130 %, pliable) | « Sauvegardé » réduit à la coche, noms de parties sur 3 lignes, Guide qui cache | **non fait ici** : la coche « Sauvegardé » est à une autre équipe ; le Guide déplié est une décision de mise en page hors langue. Restes notés dans `docs/REVIEW.md` | — |
| t5-Fb-4 (200 %, allemand, Pro) | axes de la courbe coupés, « Lautstärke −14 » sans unité, noms de pistes du Mix coupés | **non fait ici** : tout est dans le Mix (interdit à ce lot) ; noté pour l'équipe Mix | — |

Autres défauts de langue ouverts, traités :
- D c #38 (italien) : « Ritornello » touché écrivait « Refrain » dans le champ : le champ parle la langue de l'appli, la partie garde sa section (`PartNameFieldLanguageTest`). Le dialogue lui-même ne se teste pas sous Robolectric (un dialogue avec un champ n'est jamais « idle », verdict 16 du banc du tour 3) : ses champs ont été sortis tels quels (commit `refactor`), le retour à la clé « Refrain » au moment de valider est lu, pas testé.
- « 报告 Bug » (chinois, Plus) → « 报告问题 ».
- Coupes à 200 % communes à tous les écrans du Simple (barre du pouce « Annul… », « Rückg… » ; mot sous ▶ en hindi et arabe qui sort par le bas ; tempo arabe « السرعة » coupé ; compteur « المازورة 1/3 » coupé en arabe **même à 100 %** ; « Lancer à » du Live en arabe à 100 % ; carte Remix de l'accueil à 100 % en français) : réglés, prouvés par `LanguagesScanTest`.

## Le contrôle global : `LanguagesScanTest`
11 écrans (premier lancement, accueil Simple et Pro, les onglets du Simple, ⋮, Structure, Sons, Live, Plus) × 5 langues × police 100 % et 200 % = 110 cas. Le Simple sur un petit téléphone (360 dp), le Pro à 200 % sur tablette debout (comme le siège F b). Chaque cas lit ce que l'écran dessine : mots coupés, lignes perdues, « … », textes l'un sur l'autre (`LayoutScan`), et en Simple, en arabe, hindi et chinois, tout mot anglais (hors noms de fonctions de l'appli et noms tapés par l'utilisateur du projet de test).
Premier passage : 75 cas rouges sur 110 (241 coupes, 89 mots latins). Dernier : 110 verts, avec une liste `OPEN` d'acceptations écran par écran (chacune regardée, écrite dans le test et dans `docs/REVIEW.md`) :
- Simple à 200 % sur petit téléphone : la ligne unique de la barre du Guide (« · Ajoute une mélodie » se termine par « … », le texte entier est à une touche), le compteur « mesure 1/3 » trop large, « Octave 3 » du clavier (et « مسح » en « … » en arabe), l'en-tête « PAS : GROSSE CAISSE 1 · INTRO », « Händeklatschen » (allemand), « Simple » coupé sur l'accueil Simple (français) ;
- Pro à 200 % sur tablette : la puce « ⟳ Boucler », les en-têtes de la Structure, l'onglet Effets de Sons, et toute la grille du Live (un chantier à part).

## Captures regardées
`verifyRoborazziDebug` sur tous les tests de captures : 68 captures changées, regardées (images de comparaison), toutes dues au lot :
- barre du pouce du Mode simple : la place de ↷ gardée, ↶ un peu décalé (voulu, t5-Bd-3) ; ses mots entiers et plus petits au lieu de « Annul… », « Ongeda… » (130 % néerlandais, Pro) ;
- bande de lecture plus haute en arabe et en hindi (+40 %) ; flèche du Guide « ← » en arabe ;
- Live : « Lancer à » passe de « … » à « L… » dans une case très étroite (pas mieux réglé ici) ;
- Fais-moi un morceau : « Machine à cordes », « Essaim », « Nappe d'accords » sans leur ligne répétée.
Les 5 captures instables ne sont pas réenregistrées (`remix_stem_edit_menu` remise telle quelle). Images du balayage (5 langues, 100 et 200 %) : `app/build/langues-captures/` (jamais commitées), regardées pour l'arabe, le hindi, l'allemand et le chinois ; « Leeren » cassé sur trois lignes à 200 % y a été vu puis réglé (`05a0d767`).

## Chemins
chemin: Mode simple en arabe, hindi, chinois (mots du métier) et tous les écrans à 100 et 200 % | test: LanguagesScanTest#screenReadsWhole
chemin: Fais-moi un morceau › plan du morceau en arabe (← et ،) | test: RtlPunctuationTest#anArabicSentencePointsAndListsTheWayItReads
chemin: message « Effet changé : A ← B » en arabe | test: RtlPunctuationTest#theArabicCatalogsFromToTextsPointBack
chemin: Fais-moi un morceau › plan du morceau en français (→) | test: RtlPunctuationTest#frenchKeepsItsArrowAndComma
chemin: barre du pouce ↶ / ↷ en arabe | test: ThumbUndoStaysTest#undoStaysWhereItWasInArabic
chemin: barre du pouce ↶ / ↷ en français | test: ThumbUndoStaysTest#undoStaysWhereItWasInFrench
chemin: Fais-moi un morceau › batterie, petite grille en arabe | test: WizardGridDirectionTest#theAssistantsGridRunsLeftToRightInArabic
chemin: tuiles de sons (Fais-moi un morceau, sons de piste) | test: SoundTileNameOnceTest#aTileNeverSaysItsNameTwice
chemin: Structure › nommer une partie (italien) | test: PartNameFieldLanguageTest#theChosenSectionIsWrittenInItalian

Sans chemin d'appli : les noms de notes allemands (testés dans `project-model`, `GermanNoteNamesTest`) ; la ligne d'un son dans le navigateur de sons (`SoundBrowser`, même règle `repeatsName` que la tuile, lue, pas testée à l'écran).

## Doutes
- Les mots des glossaires sont écrits par l'auteur, non relus par un natif (comme le reste des 14 langues). En arabe, « Pad » devient « باد » (lisible pour un pad de batterie comme pour une nappe ; le siège attendait « الوسادات » pour les pads).
- Le Mode pro garde les mots du métier en lettres latines (choix : c'est le vocabulaire des producteurs).
- Les katakana japonais (« ミックス », « ブレイク ») ne sont pas changés : ce sont des mots courants en japonais ; la promesse t5-Fc-ecran-1 reste à rejuger par un siège.
