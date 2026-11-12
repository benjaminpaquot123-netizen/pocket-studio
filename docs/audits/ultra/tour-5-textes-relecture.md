# Deuxième relecture du lot « textes » (tour 5, chantier 3 : un mot par chose)
Relu : f887f4cbef87264835fa1b2c8d4f9c0fe9b7064d

Relecteur neuf (ni auteur du code, ni de la première relecture). Delta lu : `git log 9fe570ef..f887f4cb` (10 commits), le rapport « Après relecture », REVIEW, le code touché.

## Constats
- Grave 1 **corrigé**. Le bas d'un curseur dit « Muet » (`levelWords` / `faderWords`, `MixerScreen.kt`). C'est ce que montrent le Mix simple (`SimpleMix.kt`, texte sous le curseur et valeur lue par TalkBack) et la fiche de piste (`AppSpaces.kt`, texte et `valueText`). La clé « Muet » existait déjà, avec ses 13 traductions (Muted, Stumm, Mudo, ミュート, 静音, مكتوم…) : avec le français, cela fait 14 langues. La clé « Plus de son » a disparu de tous les catalogues et du code. Le mot est le même que celui du bouton coupé (`MuteSolo.muteCaption`) : même sens, cohérent.
- Grave 2 **corrigé**. Plus aucun « Pas à pas » ni « effets de scène » dans les textes ni les catalogues. Il en reste dans deux commentaires de code (`NewProjectPage.kt:302`, `PadLiveScreen.kt:543`) (mineur).
  - Carte Pad Live : « des effets live ». Étiquette : « EFFETS LIVE ». Vu sur `nav/launch.png` et `tabA11/130_nav_launch.png` (130 %), sans coupure.
- « Choisir ce style » ne ment pas, mais en dit peu (moyen). Le bouton appelle `customStyle`, qui fait `openWizard(id)` puis `chooseStyle(id)` : « Fais-moi un morceau » s'ouvre, ce style déjà choisi, à l'étape suivante. C'est exactement ce que fait le bouton « Choisir ce style » de l'assistant lui-même (`WizardScreen.kt:346`) : un seul mot pour une seule action. Ce n'est plus un deuxième nom de l'assistant.
  - Sur la carte rapide, le toucher de la carte crée déjà le morceau de ce style. « Choisir ce style » ne dit donc pas en quoi il diffère : on règle les sons et les parties avant de créer. « Régler avant de créer » serait plus clair. À voir à la recette.
- Aucune capture ne montre le nouveau bouton ni « Muet » (moyen). `projects_new*.png`, `nav/simple_mix.png` et `scale130/fr_simple_mix.png` n'ont pas changé : aucun curseur n'y est tout en bas, et la carte de style dépliée sort du cadre.
  - « Choisir ce style » (16 lettres, et « Diesen Stil wählen » en allemand) prend la place de « Pas à pas » (9 lettres) sur la même ligne que « Créer le morceau ». Ce dernier a un `weight(1f)` et des points de suspension : il peut se couper à 130 % ou en allemand.
  - `ScreenScanTest` est vert, mais n'a pas d'écran avec une famille dépliée au repos. À regarder sur une capture au prochain passage.
- Paires rouge/vert honnêtes. J'ai rejoué les trois rouges : j'ai retiré du code les trois verts (98414e3c, c93c8aeb, 791806ba, `git apply -R` sur `app/src/main` et `project-model/src/main`) en gardant les tests de la tête. Chaque test échoue sur son assertion, pas sur une erreur :
  - `LevelWordsTest.theBottomOfAFaderSaysMuet` : `expected:<[Muet]> but was:<[Plus de son]>` ;
  - `LexiconWordsTest.oneWordPerThingInFrench` : `old words: Pas à pas | EFFETS DE SCÈNE | … des effets de scène …` ;
  - `noSendOrExportForAFileLeftInFrench` : `Export du projet | … envoyer tes projets | L'export a échoué : %s | Export terminé : %s …` ;
  - les 4 autres tests de `LexiconWordsTest` restent verts. L'atelier a été remis fichier par fichier : propre, tête f887f4cb.
- LayoutScan (2493ee09) ne cache pas un vrai défaut, et sa garde tient. Une fenêtre pop-up est désormais redessinée depuis sa propre vue, au lieu de l'écran du dessous. Mutation faite : texte des entrées du menu ⋮ en `Neon.TextPrimary` à 22 %. Résultat : `ScreenScanTest[NavScreenshotTest.simpleMore]` rougit, « CONTRAST 12 > 2 », chaque entrée à 1,80:1 (« Live : jouer en direct », « Enregistrer ma voix », « Bibliothèque de sons »…). Le plafond est bien remis à 2 dans `recensement-acceptes.tsv`. Au repos, le recensement ne compte aucun CONTRAST sur cet écran.
  - Trou ancien, hors du lot : `contrast()` ignore un texte sous 1,2:1 (« rien de dessiné là »). Un texte devenu tout à fait invisible ne serait donc pas compté (mineur, à noter dans REVIEW au prochain passage du recensement).
- Tests rejoués à la tête f887f4cb, tous verts (exécution fraîche, 00:15–00:25, file partagée) : `com.pocketstudio.app.human.*`, `LevelWordsTest`, `SimpleNoDecibelTest`, `ScreenScanTest` (302 écrans) côté app, `LexiconWordsTest` et `I18nCatalogTest` côté modèle.
  - Bilan : 589 tests, 0 échec, 0 erreur, 57 ignorés. Les ignorés sont les bancs et testeurs humains à activer à la main (`Bench*`, `HumanTester*`, un par classe ; 30 dans `BenchAmeneesProfilesTest`) et 3 écrans de `ScreenScanTest`, comme d'habitude.
- dB restants en Mode simple. Ce sont bien des écrans du Mode simple :
  - la boîte d'effets, ouverte par Pad Live › Effets (`fxChannel = 3`, sans condition de mode) ;
  - la fiche de piste du Guide : le lien « Voir le réglage » d'une carte n'est pas filtré en simple (`GuidePanel.kt:408`, `AppRoot.kt:2186`), et la fiche mène aux couches (`formatDb`, « +0,0 dB ») et aux effets ;
  - l'écoute de référence : la carte `mix.reference` est `simple: true`, et `GuideListen.louderNote` dit « La référence était %s dB plus forte ».
- Je les classe en moyen, et non en grave, car ce n'est pas un défaut visible du chantier. Le chantier visait les mots de tous les jours du Mode simple : volume du Mix simple, fiche de piste, conseils, tutos Facile, valeurs du Guide en simple. Tout cela est fait et gardé par des tests.
  - Les trois restes sont à deux touches ou plus du chemin courant, ou derrière l'import d'un fichier de référence.
  - Les seuils et gains d'un compresseur n'ont pas d'équivalent en mots sans revoir la boîte d'effets elle-même : c'est un autre chantier.
  - La dette est écrite dans REVIEW, et le commentaire de `SimpleNoDecibelTest` ne promet plus que ce qu'il parcourt.
  - À faire tôt quand même, car c'est peu coûteux : la phrase de `louderNote` (« La référence était plus forte : … ») et `TrackSheet.formatDb` vers `levelWords`.
- DECISIONS et REVIEW sont d'accord avec le code (ligne « Exporter et partager » corrigée, ligne « Muet » ajoutée, dette dB et menu ⋮ notées).

## Première relecture (2a2c2a10)
### Relecture du lot « textes » (tour 5, chantier 3 : un mot par chose)
Relu alors : 2a2c2a10f104ab8399f41606e8d5814b7c2dce9e

Relecteur neuf, base fx-int 4137b066, trois commits (183e094c rouge, 7e92aaff rouge, 2a2c2a10 vert). Lus : le rapport de l'auteur, l'audit § 4 (chantier 3) et § 5 (lexique), DESIGN « Lexique », DECISIONS, le diff complet (code, catalogues, tutos, guides, tests, recensement).

#### Ce qui a été rejoué
- Tête 2a2c2a10 : `LexiconWordsTest` (6/6), `TutoLot4Test` (12/12), `MixTipsTest`, `I18nCatalogTest`, `SimpleNoDecibelTest` (3/3), `SimpleMixTest` (6/6), `LanguageTest`, `OnboardingUiTest`, `CutSpanJourneyTest`, `HelpCoverageTest` : tout vert.
- Rouges rejoués sur 7e92aaff (base + tests seuls) : `LexiconWordsTest` 4 échecs sur 6, `SimpleNoDecibelTest` 2 échecs sur 3 ; atelier remis sur la branche ensuite.
- `scripts/guides/make_guides.py` relancé dans un dossier à part : sa sortie est identique, octet pour octet, aux 38 `guides/*.json` et aux catalogues en/nl commités. Le changement (« Exporter le M4A » devient « Partager le M4A », titre d'étape « Partager le morceau ») est sûr.

#### Graves
- grave : « Plus de son » veut dire deux choses opposées. C'est le libellé d'un curseur tout en bas (`levelWords` et `faderWords`, `MixerScreen.kt`), visible sur le Mix simple et la fiche de piste, et lu par TalkBack. En français, « plus de son » se lit aussi « davantage de son ». La même appli l'emploie dans ce sens : « Nouvelle version : plus de sons », « Plus de sons %s : … ». Les traductions (« No sound », « Kein Ton », « 無音 ») montrent le sens voulu, le français ne le garantit pas. C'est justement le défaut que vise le chantier (« un mot, un sens »). L'audit proposait « Silence » : à reprendre, ou « Muet », que la fiche de couche emploie déjà (`formatDb` : « muet »). **corrigé**
- grave : deux restes du lexique sur des écrans d'entrée, que la règle écrite par le lot dans DESIGN « Lexique » interdit elle-même. **corrigé**
  - `NewProjectPage.kt:343` : le bouton « Pas à pas » de chaque carte de style ouvre l'assistant (`START_CUSTOM`). C'est un deuxième nom pour « Fais-moi un morceau », sur la même page que la grande carte qui porte ce nom.
  - `LaunchChoice.kt:500` : la carte Pad Live de l'accueil dit « des effets de scène ».
  - `LexiconWordsTest.oneWordPerThingInFrench` les laisse passer : ses motifs sont sensibles à la casse (« Effets de scène ») et trop étroits (« pas à pas avec », « Nouveau morceau, pas à pas »). Il faut corriger les deux textes et élargir les motifs (`(?i)effets de scène`, la clé exacte « Pas à pas »).

#### Choix de mots (point 1)
- Les choix sont conformes à l'audit § 5 : « Fais-moi un morceau », « Guide » pour la barre seule, « Visite rapide », « Aide », « Partager » / « Créer le fichier… », « Sauvegarder le morceau en .zip », « Mettre les bouts sur… », « Effets live ». « J'apprends » est gardé au lieu de « J'apprends à le faire » : le choix est noté dans DECISIONS, c'est acceptable.
- Restes de « envoyer » ou « export » au sens de sortir un fichier (moyens, à faire dans ce lot ou le suivant) :
  - « Ouvrir, créer, dupliquer, envoyer un projet » (Plus › Mes projets, `AppSpaces.kt:188`) ;
  - « 📁 … ouvrir, créer ou envoyer tes projets » et « Mix : … les courbes et l'export », dans la Visite rapide elle-même (`GuideDialog.kt:52`, `:65`) ;
  - « Un seul fichier avec tes effets, prêt à envoyer » (`PerfShare.kt:264`) et « Partager envoie ta perf » (aide de Pad Live) ;
  - les messages « Export de « %s » », « Export terminé : %s » et « L'export a échoué » ;
  - « Puis envoie-le » (aide « Un bout », reste déjà admis par l'auteur).
- DECISIONS n'est pas d'accord avec le code : la ligne « boutons de Mix » dit que « Exporter et partager » (titre de chapitre du Guide) reste. Or le diff le remplace par « Partager le morceau » (`GuideParcours.kt`, `make_guides.py`). Il faut corriger la ligne.
- Le titre de la carte d'export du Mix Pro devient « Autres fichiers ». Sur le Mix Pro, c'est la seule carte qui fabrique le fichier du morceau : « autres » que quoi ? L'audit voulait ce mot pour le lien depuis « Prêt à partager ? », pas pour cette carte (mineur, maladroit).
- Le Mix simple garde le bouton « Tout à zéro », alors que les valeurs disent maintenant « Normal » : « Tout remettre à normal » serait cohérent (mineur).
- Kits des tutos Pro : « la démo de « Fais-moi un morceau » » et « les réglages de « Fais-moi un morceau » » sont lourds, sans être faux (mineur ; la règle « l'appli dans les phrases lourdes » n'y est pas appliquée).

#### Plus de dB en Mode simple (point 2)
- `SimpleNoDecibelTest` ne parcourt pas tout le Mode simple. Il voit :
  - le premier écran des quatre onglets, plus le menu ⋮ ;
  - le Mix (303 montée, conseil, batterie ouverte, Guide ouvert) ;
  - une fiche de piste (Batterie seulement : la boucle `for` n'a qu'une clé).

  Il ne voit pas :
  - la boîte d'effets, ouverte en Mode simple depuis Pad Live › Effets (`fxChannel = 3`). Là, les valeurs restent en dB : « Seuil … dB », « Gain +3,0 dB », « Réduction 0 dB » (`Effects.kt`, `EffectsBox.kt:415`) ;
  - la fiche de piste du Guide (`TrackSheet.formatDb`) et l'écoute de référence (« La référence était %s dB plus forte », `GuideListen.kt:307`) ;
  - la page Partager déroulée, les aides « ? », le tuto Facile joué dans l'appli. Le tuto Facile est couvert côté modèle par `theFacileTutosSayNoDecibel`.

  Le commentaire du test (« No « dB » on any of its screens ») promet plus que ce que le test vérifie (moyen). Il faut soit étendre le test (Pad Live › Effets, une carte du Guide qui ouvre la fiche), soit restreindre la promesse et noter les écrans restants dans REVIEW.
- Les nouveaux mots sont justes et compris : « Normal », « +3 », « −6 », « Baisser la basse un peu ». Sur la capture, « +3 » sans unité se lit bien sous un curseur gradué +4 / 0 / −12 / −∞ ; TalkBack dit « plus 3 », c'est acceptable. Seul « Plus de son » pose problème (voir les graves).
- Phrase des tutos Facile : « Le nombre sous le curseur dit le volume ». Au repos, sous le curseur on lit le mot « Normal », pas un nombre (mineur : « Ce qui est écrit sous le curseur »).

#### Discipline (point 3)
- Le commit vert nomme `SimpleNoDecibelTest` et `oneWordPerThing`. Les trois autres tests rouges de `LexiconWordsTest` (`theAssistantAndTheTourHaveOneNameInEveryLanguage`, `theGuideHasOneWordInEveryLanguage`, `theFacileTutosSayNoDecibel`) ne sont pas nommés (mineur).
- Les rouges, rejoués :
  - `oneWordPerThingInFrench`, `theGuideHasOneWord…` et `theFacileTutosSayNoDecibel` rougissent pour la bonne raison (anciens mots, « asisten » / « المساعد », « dB » dans les tutos Facile) ;
  - `SimpleNoDecibelTest.noDecibelOnTheMix` et `noDecibelOnATracksSheet` aussi (« +0,0 dB », « Remettre à 0 dB », « Baisser la basse de 2 dB »).
- Moyen :
  - `theAssistantAndTheTourHaveOneNameInEveryLanguage` rougit sur une `NullPointerException` (clé absente, `t[name]!!`), pas sur son assertion ;
  - `noDecibelOnTheTabsAndTheMore` est vert sur la base : c'est une garde, pas un rouge ;
  - entre le rouge et le vert, ce test a été assoupli sans le dire. Il ne cherche plus la phrase exacte de la visite dans chaque texte, mais une racine en minuscules (`TOUR_STEM`, « tour » en anglais). Contrôle encore utile, plus faible : à mentionner dans le commit ou le rapport.
- `TutoLot4Test` retourné : c'est honnête. L'ancienne exigence (« dB » expliqué en Facile) contredit la règle votée (audit § 5, « Volume »). L'explication est remplacée par une exigence équivalente (« nombre sous le curseur » doit être présent), rien n'est supprimé sans contrepartie, et une ligne DECISIONS le dit.

#### Plafond du recensement (point 4)
- La cause est vérifiée. `app/build/recensement/NavScreenshotTest.simpleMore.txt` mesure « Aide » à 1,20:1, « Renommer le morceau » à 1,58:1, etc. Or la capture `nav/simple_more.png` montre ces entrées en blanc sur gris foncé, nettement au-dessus de 4,5:1. Le recensement lit donc le fond de la page sous le menu (fenêtre du dessous), pas le menu. Le relèvement de 2 à 5 n'est pas une baisse de qualité réelle, et la raison écrite dans le TSV est exacte.
- Il reste contraire à la règle de l'en-tête. La vraie correction est dans `ScreenScanTest` : ne pas mesurer le contraste des nœuds d'une fenêtre surgissante sur la capture de la fenêtre principale. À noter dans `docs/REVIEW.md` comme dette, avec retour à 2 dès que la mesure est corrigée (moyen, pas grave).

#### Langues (point 5)
- Relus : EN, NL, DE, PL, JA, KO, ZH-CN, HI, AR.
  - Le nom de l'assistant est le même partout dans chaque langue (« Make me a song », « Maak een nummer voor me », « Mach mir einen Track », « 曲を作って », « اصنع لي مقطوعة »…).
  - « Visite rapide » est rendu à chaque fois par un seul mot (Quick tour, snelle rondleiding, Kurztour, クイックツアー, 빠른 둘러보기, 快速导览, त्वरित टूर, جولة).
  - « Guide » reste distinct de la visite. En chinois, 向导 (Guide) et 导览 (visite) partagent un caractère : distincts, mais proches (mineur).
- Les kits des tutos en allemand disent bien « von „Mach mir einen Track“ ». Traductions non relues par un natif, comme l'auteur le dit.

#### Tests `human/` et autres tests retouchés (point 6)
- Les 9 fichiers `app/src/test/.../human/` ne changent que le texte touché sur la carte d'accueil, « Créer mon premier morceau » devenant « Fais-moi un morceau ». Aucune assertion retirée.
- `LanguageTest` vérifie toujours la traduction (« Make me a song », « Maak een nummer voor me », et l'absence du français). `SimpleMixTest` vérifie « Normal » au lieu de « +0,0 dB », et `MixTipsTest` « un peu » au lieu des dB. Rien d'affaibli.

#### Captures (point 7)
- Regardées moi-même : `nav/simple_mix.png`, `scale130/fr_simple_mix.png`, `welcome.png`, `projects_new.png`, `mixer_export.png` (Mix Pro, export), `tablet/simple_mix.png`, `nav/simple_more.png`.
- Rien de coupé, sauf la colonne « Sortie » rognée au bord à 130 %, comme avant le lot. « Normal » et « +3 » sont lisibles ; à 130 %, « Normal » passe sous la barre du Guide tant qu'on n'a pas défilé, comme avant.
- Les cinq captures instables (recette, cut_screen, cut_screen_magnifier, remix_stem_edit_menu, rhythm_scrolled_under_part) ne sont pas dans le diff.

Ancien verdict : 0 bloquant, 2 grave — fusion impossible

Verdict : 0 bloquant, 0 grave — fusion possible
