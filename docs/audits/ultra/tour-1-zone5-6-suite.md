# Audit ultra, tour 1 : zones 5 et 6, suite (équipe 4b)

Suite de `tour-1-zone5-6.md` (équipe 4). Gravités de la synthèse ; « appli » = vrai défaut corrigé.

| N° | Verdict | Gravité | Test (échoue avant) | Fait | Reste |
|---|---|---|---|---|---|
| 34 consigne « t… » (A b 50) | appli | Grave | TutoBigFontTest.theFoldedLine… (200 %, police native) : « ellipsized » | Ligne repliée haute de deux lignes de texte à 200 % (48 dp à 100 %), consigne sur deux lignes, « Lire ⌃ » au bout | Une très longue consigne reste coupée après deux lignes (le toucher déplie tout) |
| 34 1re rangée sous « Partie : A » (A b 50) | non reproduit sous Robolectric (le test passe avant et après) | Grave | même test : chaque case en pointillé sous l'en-tête + son fondu (28 dp) et au-dessus du panneau | Garde-fou : `SpotNode.reveal` demande la place, puis le contrôle seul entier sous le fondu (le haut d'abord) ; `hidden()` signale un haut coupé sous l'en-tête quand le contrôle tiendrait (TutoJourneyTest le lit, 74/74 verts) | À revoir au tour 2 sur le siège A b (200 %) |
| 34 pas de bravo en fin de chapitre (A b 28, 46) | appli | Grave | TutoBigFontTest.aChaptersLastStep… | « Bravo, chapitre fini ! », « Écouter mon morceau », « Chapitre suivant » ; repliée, la ligne déplie au lieu de sauter au chapitre suivant | — |
| 34 carte du kit par-dessus la barre du haut (A b 12) | appli | Moyen | TutoBigFontTest.theKitsCard… (carte de 18 à 430 dp, barre finie à 56) | La carte prend la place du panneau (plus d'empilement), au plus 60 % de l'écran, défile | — |
| 35 effets du son gardé (D c 38) | appli | Grave | KeptSoundTest | Le favori garde effets, envois et réglages rapides de sa piste, les ramène en une étape d'annulation (volume et place restent ceux de la piste) | Plaits : même chose ; l'Acid 303 garde son son seul |
| 35 nom (D c 31) | appli | Moyen | KeptSoundTest | « Garder ce son » demande un nom (« Mon son N » proposé) ; « Renommer « X » » sous le son gardé joué ; nom déjà pris refusé et dit | — |
| 35 doublon (D c 67) | appli | Mineur | KeptSoundTest | Le même son (et les mêmes effets) refusé : « Ce son est déjà dans tes favoris : « X » » ; bouton « Déjà dans tes favoris » | — |
| 35 valeurs sans unité (D c 17) | appli | Moyen | — (lecture des formules du moteur) | Coupure en Hz/kHz, enveloppes en ms/s, glissé en s, formules de `Synth.cpp` | Test dédié à ajouter |
| 38 « Sim… », « Char… », « Écouter l'exempl » | appli | Moyen | TutoBigFontTest (« Simple » ellipsé avant), CaptureMatrixBigFontTest[rhythm] (11 noms de pads ellipsés, « PAS : KICK DE MA V/OIX »), [session] (« × 2 · 2 mes. · joue »), TutoPanelFitsBigFontTest (titres de chapitre coupés) | Racine : au-delà de 130 %, `WholeWords`/`WholeWordsAutoSize` descendent jusqu'à leur plancher de 130 % et l'interligne suit ; titres de section en mots entiers ; boutons et titre du tuto en `WholeWordsText` (3 lignes) | Clavier à 200 % (`tracks` : puces Synthé/Plaits, « Octave 3 », « Do ») ; lettre de partie (Pro) sur une barre de 300 dp, acceptée |
| 38 « mes. 1/8 » coupé (F a 17) | appli (non reproduit sur téléphone) | Moyen | — (play_strip accepte ses coupes : capture empilée) | La position du bandeau grandit avec le texte (48 dp de toucher au moins) | « mes. » abrégé en Mode simple |
| 38 « Tout le morce » (C c 26) | — | Mineur | — | Non fait : écran Ma voix de l'équipe enregistrement | — |
| 41 accueil tablette couchée (D b 1) | appli | Mineur | — | Choix deux par deux sur un écran large et couché | « sur ton téléphone » dit sur une tablette |
| 41 « Tegenmel/odie » (D b 14) | appli | Mineur | — | « Mes pistes » : nom en `WholeWordsText`, « ▶ Écouter seule » en icône seule quand le plus long mot du nom ne tiendrait pas | — |

Textes nouveaux : `audit1z56b.json` dans les 14 catalogues.

Tests lancés (verts) : TutoBigFontTest 3/3, KeptSoundTest, TutoJourneyTest 74/74, TutoTest, TutoKitTest, TutoScreenTest, TutoLot4Test (attendu d'équipe 4 corrigé : « À toi » en FADING), TutoAppScreenshotTest, CaptureMatrixBigFontTest 6/6, TutoPanelFitsBigFontTest 8/8, TutoPanelFitsPhone360Test 49/49, SoundBrowserCurrentTest, TrackSheetVmTest, I18nCatalogTest.
Captures ré-enregistrées et regardées : `synth` (+ tablet, tablet8, land, land_en : Coupure en kHz, Attaque/Relâchement en ms), `tablet/land_welcome`, `tablet8/land_welcome` (choix deux par deux). Phone : « Mes pistes » inchangé.
ScreenScanTest : 54 recensements au-dessus du plafond, les mêmes avant mes changements (vérifié en remettant l'ancien `Spotlight.kt`) ; pas les miens.
