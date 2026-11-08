# Tour 5, scan r3 : les pastilles de Sons coupées en bas (Galaxy Tab A11)

## Le défaut vu par Benjamin
Tablette Galaxy Tab A11 debout, mode Pro, espace Sons : la rangée de pastilles du haut (« Batterie / 16 pads »,
« Acid 303 / Ligne de bas… », « Voix / Micro », « Remix / Séparer », pistes…) montre la deuxième ligne coupée en
bas, la moitié basse des lettres manque.

## La cause dans l'app (à corriger par l'équipe de correction, pas ici)
Chaque pastille a une hauteur fixe de 44 dp (`EditorChip` dans `ui/AppSpaces.kt`, `AcidChip` dans
`acid/AcidLook.kt`, les pistes dans `synth/TrackBar.kt`) avec deux lignes : labelLarge (ligne de 20 sp) +
labelSmall (ligne de 16 sp) = 36 sp. Dès que la taille de police du téléphone dépasse environ 1,22, les deux
lignes ne tiennent plus dans 44 dp : la deuxième reçoit une boîte plus courte que sa ligne et se rogne elle-même
(`maxLines = 1`, `overflow = Ellipsis` coupe le dessin au bord de sa boîte). Ce n'est pas lié à la taille de
l'écran : tout téléphone ou tablette en police ≥ 1,25 l'a.

## Ce que le scan lit maintenant sur le vrai écran Sons (tablette)
`SonsChipsTabletScanTest` (app complète, projet avec Batterie, Acid, Voix, Remix, Synthé, Basse, Lead, Orgue…,
Sons ouvert), constat brut dans `app/build/sons-chips-scan/` :

| Tablette | Police 1.0 à 1.2 | Police 1.25 | Police 1.3 | Police 1.5 |
|---|---|---|---|---|
| 600 x 1006 dp (Tab A11, tvdpi) | rien | 6 pastilles « drawn past its parent's edge (down, 18 dp shown of 20) » | 5 pastilles « (down, 17 dp shown of 21) » | « lines cut (taller than its box) » |
| 800 x 1280 dp (hdpi) | rien | 7 pastilles « (down, 18 dp shown of 21) » | 6 « (down, 17 dp shown of 21) » + « Ligne de basse » ellipsized | « lines cut » |

Constat exact à 1.3 sur la Tab A11 :
```
CUT « 16 pads » drawn past its parent's edge (down, 17 dp shown of 21)
CUT « Ligne de basse » drawn past its parent's edge (down, 17 dp shown of 21)
CUT « Micro » drawn past its parent's edge (down, 17 dp shown of 21)
CUT « Séparer » drawn past its parent's edge (down, 17 dp shown of 21)
CUT « Synthé » drawn past its parent's edge (down, 17 dp shown of 21)
```
(les pastilles hors de l'écran, rangée défilée, ne sont pas jugées.) À police 1.0 les pastilles sont entières
(20 + 16,5 dp dans 44 dp) : la tablette de Benjamin est très probablement réglée en grande police (≥ 1,25).

## Pourquoi aucun scan ne l'avait signalé
1. Sur l'écran Sons réel, LayoutScan le voyait déjà (ci-dessus) mais aucun scan ne dessine cet écran en grande
   police : la matrice de captures ne passe jamais par l'en-tête Sons, et ScreenScanTest ne lit pas CUT.
2. Là où la matrice voit ces pastilles en 1.3 (écran « tracks », phone411 en), la rangée défile sur le côté
   dans une page qui défile vers le bas : LayoutScan tenait alors les deux axes pour « défilants » et ne jugeait
   jamais une coupe verticale, même faite par la pastille elle-même. Corrigé : une coupe vers le bas n'est
   excusée que si une liste qui peut vraiment défiler la cache par son propre bord.

## Après le correctif du scan
- Test de garde : `LayoutScanClipTest` (rouge `df794a9`, vert `dcc3367`).
- Matrice de captures : 2 nouveaux constats, regardés sur les images.
  - `tracks_en_1.3` (phone411) : « Synth », « Plaits », « Strings » coupés en bas : **vrai défaut**, le même.
    Retenu dans `Allowed` par une règle marquée « REAL defect » à retirer avec la correction des pastilles.
  - `acid_en_1.3` (phone411) : 🔒 ↑ ⌒ des pas : entiers sur l'image (boîte de ligne haute de la police de
    secours), acceptés.
- ScreenScanTest : 296 cas, 0 échec, avant comme après (TARGET 707, LABEL 0, CONTRAST 493, HELP 6841 : identiques).

## Reste noté
- Un texte à la fois coupé et ellipsé (« Ligne de basse » à 1.3 sur 800 dp) ne remonte que « ellipsized » : le
  premier constat masque la coupe.
- L'axe horizontal garde l'ancienne règle (pas jugé dans une rangée défilante) : l'ouvrir donnait un faux
  constat sur une pastille en arabe (`rhythm_ar_1.0`, tablet8).
