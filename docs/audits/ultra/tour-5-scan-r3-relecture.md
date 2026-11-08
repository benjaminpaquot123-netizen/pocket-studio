# Tour 5, scan r3 : relecture critique (LayoutScan, coupe vers le bas dans une page qui défile)
Relu : 6550777

Relecteur neuf (n'a pas écrit le code). Périmètre : `86b5dd8..6550777` (df794a9 rouge, dcc3367 vert, 6550777 rapport).

## Ce qui a été vérifié, en le faisant tourner
- **Rouge réel** : `LayoutScan.kt` de `86b5dd8` remis avec le `LayoutScanClipTest` de la branche : 1 échec sur 3
  (« « 16 pads » cut at its bottom, not found: [] »), les 2 autres verts. Avec le correctif : 3/3 verts.
- **Le motif réel (page qui défile vraiment)** : sonde jetable (non commitée) : pastilles à police 1.3 dans une
  rangée `horizontalScroll` dans une `Column(verticalScroll)` haute de 3000 dp (maxValue > 0), puis dans une
  `LazyColumn` de 200 lignes : « 16 pads » et « Micro » trouvés « down, 17 dp shown of 21 » dans les deux cas. La
  logique des fenêtres marche bien sur le vrai motif.
- **Mutant** : `cutY` remplacé par « jugé seulement si aucun défileur vertical n'a rien à défiler » (sans aucune
  comparaison aux fenêtres) : `LayoutScanClipTest` reste 3/3 vert. Les tests ne tiennent donc que le filtre
  `maxValue > 0`, pas la comparaison aux fenêtres (voir constat 1).
- **Règles `Allowed` retirées à titre d'essai** : la matrice échoue exactement sur `phone411 en acid 1.3`
  (🔒, ↑, ⌒ « 27 dp shown of 31 ») et `phone411 en tracks 1.3` (Synth, Plaits, Strings « 17 dp shown of 21 ») :
  conforme au rapport, rien d'autre de caché aujourd'hui.
- **Suites vertes sur la branche** : CaptureMatrix* (6 suites, 470 cas), TutoPanelFits* (128), TutoBigFontTest,
  BigTextLabelsTest, PadWordWholeTest, I18nScreenshotTest, LayoutScanClipTest, SonsChips* (14 cas, ~17 s de test,
  1 min 43 avec Gradle). ScreenScanTest non relancé (le rapport le donne identique ; il ne lit pas CUT).
- Toutes les expériences défaites (`git checkout`, sonde supprimée).

## Lecture du changement de LayoutScan
- Hors de tout défileur vertical : comportement inchangé (`scrollWindows` vide, même formule qu'avant).
- Sous un défileur vertical : une coupe vers le bas n'est plus excusée en bloc ; elle l'est seulement si l'encre
  dépasse la fenêtre (`boundsInRoot`, déjà rognée par les parents) d'un défileur vertical qui a quelque chose à
  défiler. Pour `verticalScroll`, `maxValue` = `ScrollState.maxValue` ; pour une liste paresseuse, `maxValue`
  vaut `value + 1` tant qu'elle peut avancer, sinon `value` : une liste en bas de course garde une fenêtre, une
  liste qui tient entière n'en a pas. Correct.
- Faux négatifs possibles (étroits, acceptables) : `.any` sur toutes les fenêtres excuse une coupe faite par la
  boîte du texte si cette boîte touche à moins de ~3 dp le bord d'un défileur ; `shown.isEmpty && axes != 0`
  saute toujours un texte entièrement rogné par un parent non défilant dans une page qui défile (déjà le cas
  avant).
- Faux positifs possibles : l'« encre » est la boîte de ligne (`lineHeight`), pas le dessin des lettres. Un petit
  texte qui hérite d'une grande hauteur de ligne dans une case serrée est désormais jugé dans toutes les pages
  qui défilent : c'est exactement le cas acid (voir constat 3). Le reste (listes à moitié défilées, défileurs
  imbriqués, feuille du bas animée : la fenêtre rognée suit) est bien excusé ou inchangé.

| Gravité | Constat | Suite |
|---|---|---|
| Moyen | **Le test de garde ne garde pas le vrai motif.** Dans `LayoutScanClipTest`, la page `verticalScroll` du cas rouge n'a rien à défiler (`maxValue == 0`) : le cas passe par le filtre `maxValue > 0`, jamais par la comparaison aux fenêtres. Un mutant qui excuse tout dès qu'un défileur vertical peut défiler (donc aveugle sur la tablette réelle, où la page défile) laisse les 3 cas verts. Et dans la matrice, le seul cas qui le verrait (`tracks`) est couvert par la règle `Allowed` « tracks » : une régression de cette logique ne ferait échouer aucun test. | Ajouter un 4e cas : pastilles à 1.3 dans une page qui défile vraiment (`Spacer` de 3000 dp ou `LazyColumn`), coupe attendue (sonde de cette relecture : elle passe avec le correctif). |
| Moyen | **Règle `Allowed` « tracks » trop large.** `CUT « [^»]+ » drawn past its parent's edge \(down` excuse toute coupe vers le bas (et vers le haut : le libellé dit « down » pour les deux) de n'importe quel texte de l'écran `tracks`, sur tous les appareils, langues et polices, sans date de fin vérifiable. Une nouvelle vraie coupe sur cet écran passerait en silence. Aucune trace non plus du défaut réel dans `docs/PROGRESS.md` ni `docs/REVIEW.md` : la règle et la correction des pastilles peuvent être oubliées. | Restreindre aux trois textes vus (`« (Synth\|Plaits\|Strings) »`) et au constat précis (`17 dp shown of 21`) ; noter le défaut des pastilles (EditorChip, AcidChip, TrackBar à 44 dp fixes) dans PROGRESS/REVIEW pour l'équipe de correction. |
| Moyen | **Aucun garde sur l'écran où Benjamin a vu le défaut.** `SonsChipsTabletScanTest` n'échoue jamais (sauf pastilles absentes) ; la matrice ne passe pas par l'en-tête Sons. Le constat écrit dans `app/build/` n'est lu par personne en CI. Coût faible (~17 s), pas de fuite (police remise à 1 dans `@After`, Robolectric réapplique la configuration à chaque test). | Le garder, mais lui faire affirmer : 0 coupe dans la rangée à 1.0 (garde contre un faux positif futur) et, en « défaut connu », au moins une coupe à 1.3 : quand l'équipe corrige les pastilles, ce test passe au rouge et rappelle de retirer la règle « tracks ». |
| Mineur | **Raison de la règle « acid » inexacte, conclusion plausible.** Les marques (🔒 ↑ ↓ ⌒) sont un `Text` de 11 sp sans `lineHeight` : il hérite du `bodyLarge` Material (24 sp, soit 31 dp à 1.3), centré. Ce n'est pas « la police de secours » : c'est la hauteur de ligne héritée. Les ~4 dp rognés sont du vide sous des signes de ~14 dp : « entiers » est crédible (images non regardées, la matrice ne les garde qu'en enregistrement). La règle n'accepte qu'un signe seul : « ↑↑ » ou « ↑⌒ » échoueraient (prudent mais fragile). | Corriger la raison de la règle ; côté app, donner `lineHeight = 14.sp` aux marques supprimerait la règle. |
| Mineur | **« Ellipsé masque coupé » (noté par l'auteur, non corrigé).** `cut()` rend « ellipsized » et s'arrête : « Ligne de basse » (800 dp, 1.3) ne remonte pas sa coupe ; avec les règles `.*` / `live_grid` / `padlive` qui acceptent « ellipsized », une coupe réelle sur un texte ellipsé est masquée deux fois. | Ne pas s'arrêter sur « ellipsized » : chercher aussi la coupe de bord et rendre la plus grave (une coupe plutôt qu'un « … »). |
| Mineur | Le libellé dit « down » aussi pour une coupe par le haut ; seuil de 2 dp : entre police ~1.22 et 1.25, une coupe de moins de 2 dp passe (le rapport la situe dès 1.25, cohérent). | Libellé « up »/« down » à séparer quand on touchera aux règles. |
| Mineur | Comportement horizontal inchangé et documenté (une rangée défilante n'est pas jugée en largeur) : choix assumé, raison donnée (faux constat arabe sur tablet8). | Rien. |

Verdict : 0 bloquant, 0 grave — fusion possible
