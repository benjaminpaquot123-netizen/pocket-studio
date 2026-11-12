# Relecture neuve du lot moyens (tour 5)
Relu : 01367b9eb249913ca32f42a83bcadb4c42dfab40
delta : après la 2e passe (020026a2), seul le rapport de zone docs/audits/ultra/tour-5-moyens-rapport.md a été ajouté (aucun code) ; relu, conforme.
delta : fusion de l'intégration 9b3eeb28 (lots padlive et access) : conflits sur I18n.kt (les deux lignes de catalogues gardées) et sur scale130/fr|nl_nav_simple_keys.png (réenregistrées sur la base fusionnée et regardées : barre sur une ligne, dossier seul, clavier entier, nouveau « ? » du chantier 9) ; tests ciblés du lot verts (2 277, 0 échec) ; rejeu t4-26-prise-clavier : oracle tenu. Aucun autre changement.

## Constats

- **grave** · **corrigé** · app/screenshots/scale130/fr_nav_simple_more.png, nl_nav_simple_more.png (NavScreenshotTest.kt:115) · Première passe : les captures de la page Plus en Simple à 130 % montraient encore « M… », car elles avaient été enregistrées avant la barre de l'équipe A. Preuve vue : f0166e3f a réenregistré toute la série NavScale130, simple_more compris. Sur nl_nav_simple_more et fr_nav_simple_keys, la barre montre le dossier seul, la pastille et les boutons sur une seule ligne, ce que donne le code de TopBar.kt:189-190 à 316 dp.

- **grave** · **corrigé** · docs/audits/ultra/rejeux/t4-26-prise-clavier · Première passe : les gestes à coordonnées fixes ne visaient plus les mêmes boutons une fois la barre décalée. Preuve vue : les 32 gestes sont maintenant des étiquettes (`touche welcome_guided` … `touche key_62`, `touche record_stop`). HumanTester.kt:326-342 vise le nœud de l'étiquette, l'amène à l'écran puis le touche en son milieu. Une étiquette introuvable donne « divergé au pas n » : le rejeu ne peut plus passer au vert en ne rejouant rien. Il ne dépend plus des décalages de mise en page. En plus, avec le palier `nameShown`, la barre reste sur une seule ligne sur ce banc (392 dp, police 200 %).
- rejeu changé : t4-26-prise-clavier : changé par le socle du banc (gestes passés aux étiquettes, 1bf20cb1 et suivants), pas par ce lot. Le changement est légitime : il rend le rejeu insensible à la mise en page sans changer ce qu'il vérifie (absence de « Rien n'a été joué pendant la prise »).

- **moyen** · app/src/main/java/com/pocketstudio/app/ui/TopBar.kt:189-190 et 325 · Nouveau palier `nameShown = false` : avec le nom, toute la colonne sous le dossier disparaît, et avec elle « REC dans Intro » (`top_record_into`) et la coche « Sauvegardé ». Pendant une prise, la barre ne dit donc plus dans quelle partie on enregistre. « REC dans » n'est affiché nulle part ailleurs (seulement TopBar.kt:296 et :335). Cela touche le Mode simple à 130 % sur 411 et 430 dp, le banc « phone » à 200 %, et le Mode pro sur 360 dp hors écran de partie. « Sans nom » est acceptable (le nom reste dans « Mes projets »), mais « REC dans » et la coche devraient rester, par exemple seuls dans la place laissée libre.

- **mineur** · TopBar.kt:309-325 · Au même palier, le bouton projets (dossier plus grand vide à droite, voir fr_nav_simple_keys) ne s'annonce plus à TalkBack que par « Mes projets » : le nom du projet n'est plus dit nulle part sur l'écran. Ajouter le nom à sa description d'accessibilité suffirait. La zone vide touchable ouvre « Mes projets » : sans danger, mais rien ne le montre.

- **mineur** · docs/decisions/tour-5-moyens.md (ligne 7 et ligne 27) · La ligne 7 dit encore « sinon il prend sa propre ligne au-dessus » ; c'est la ligne 27 qui corrige. Il faudrait relier les deux pour qu'on n'ait pas à lire l'une sans l'autre.

- **moyen** · app/screenshots/nav/land_launch.png (ui-components/.../ScrollHint.kt:126-140) · Téléphone couché : la bande de la puce prend environ 75 dp sur 360. La carte « Remix » n'est plus qu'un trait violet, et « Continuer » perd aussi du bas. Rien n'est recouvert, mais en paysage la place se paie cher.

- **mineur** · app/src/test/java/com/pocketstudio/app/audit/LayoutScan.kt:380 · La règle « flottant sur un contrôle » ignore les contrôles coupés (`wholly`). Le défaut d'origine (la puce sur la carte « Techno » coupée) lui aurait échappé ; seul HomeScreensSayTheyScrollTest le garde, sur les écrans d'accueil.

- **mineur** · LayoutScan.kt:394 (`drawnAfter`) · L'ordre de dessin vient de l'ordre des enfants dans l'arbre d'accessibilité (un `zIndex` est ignoré). Les alertes vont dans le type OVERLAP et comptent dans ses plafonds.

- **mineur** · app/src/test/java/com/pocketstudio/app/ui/AppRootZipSenderTest.kt · C'est une vraie garde contre la mutation décrite (AppRoot.kt:918, 1750, 3789 au format attendu). Mais elle lit le source avec des expressions régulières : fragile à un simple reformatage, et aveugle à un .zip fait par un autre appel que `.exportZip(`.

- **mineur** · app/src/main/java/com/pocketstudio/app/ui/AppSpaces.kt:270 · Sur tablette, une page demandée mais absente de la liste affichée est remplacée sans bruit par la première. Rien ne casse aujourd'hui : seul `open` (venu de la liste) fixe la page.

- **mineur** · ScrollHint.kt:120-126 · Au premier dessin (`maxValue` encore à Int.MAX_VALUE), la bande de 64 dp apparaît pendant une image, même pour une liste qui tient. Ensuite, le calcul est stable et ne clignote pas.

- **mineur** · scale130/fr_nav_simple_more.png, nl_nav_simple_more.png · Ce qui suit existait avant ce lot. Les dernières tuiles (« Mijn bibliotheek », « Hulp ») sont coupées sous la barre du Guide, sans fondu ni puce : la page Plus n'applique pas la règle 17. Dans la barre du pouce en néerlandais à 130 %, « Ongedaan m… » est coupé.

## Points vérifiés sans défaut
- Clavier à 130 % : sur fr_nav_simple_keys, les touches noires se voient de nouveau en entier, comme avant le lot. Le Mode pro sur 360 dp hors partie ne perd plus de hauteur : c'est maintenant le palier « dossier seul » (voir plus haut).
- M2 : « ▶ cette partie » arrête pendant une prise (notes ou micro) par le même `togglePlay` que « ▶ le morceau », donc la prise est gardée. Son ■ suit aussi la prise au micro. Les tests seraient rouges sans la correction.
- ModeChoiceTest à 398 dp : c'est la largeur réelle de la barre du Mode simple sur un 360 dp, le test n'est pas affaibli.
- Tuiles Plus, volet tablette, « Voir mes fichiers », en-tête de famille opaque : les tests seraient rouges sans les corrections. Aucune capture interdite ni zone de code interdite touchée par ce lot ; les sujets des paires rouge/vert correspondent.

Verdict : 0 bloquant, 0 grave — fusion possible
