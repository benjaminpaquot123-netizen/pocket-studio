Relu : b1f1a5daf (tête eq/tour-9-L14 au moment de la relecture ; code : QuickFx.kt, TutoPanel.kt, AppRoot.kt)

# Relecture neuve, tour 9 L14 (panneau tuto, bandeau du kit, fin du tuto)

Diff relu : `git diff b72de0c9f..b1f1a5daf` (merge-base avec origin/ccr-80af1a1b-mn0wec). Moteur audio non touché.

## Tests lancés (ciblés)
- `Tour9L14TutoPanelTest` (13), `Tour9L14TutoEndTest` (2), `TutoKeySheetDoneTest` (1) : 16 verts.
- Voisins touchés par les mêmes fichiers : `Tour6GuideAndTutoTest`, `TutoKitTest`, `TutoKitUnderFingerTest`, `Tour7TutoKit*`, `TutoBigFontTest`, `Tour7TutoPanelTopWholeTest` : tous verts.
- `Tour6TutoCellsPanel100Test#theKickStepsDottedCellsAreWholeAboveThePanelAt100` : rouge une fois lancé avec les classes ci-dessus (« no tuto_listen » : le tuto n'était pas ouvert du tout), vert seul sur la tête L14 et vert seul sur la base. Instable selon l'ordre, pas lié au lot à ce stade ; à surveiller à la grande vérification.

## Bloquant
Aucun.

## Grave
Aucun.

## Mineur
- F014 (`TutoPanel.kt`, `SharedLabelSize`) : les tailles ne font que descendre tant que l'étape ne change pas. « Arrêter l'écoute » (plus long) rétrécit le groupe, et « Écouter l'exemple » revenu garde la petite taille jusqu'à l'étape suivante. Une case d'un bouton disparu garde aussi sa taille. Sans danger (pas de boucle : chaque écriture est ≤ la précédente, bornée par le plancher), juste un peu plus petit que nécessaire.
- F014 : `SecondaryLabel` écrit un état Compose pendant la mesure (`getFontSize` → `fitted`) : une ou deux recompositions de plus au premier affichage. Acceptable, à garder en tête si le panneau clignote.
- F019 : `tutoCourseFinished` refait `TutoResume.finished` (même règle Pro/écoute) avec une différence : cours inconnu = « fini » d'un côté, « pas fini » de l'autre. Deux règles pour une même idée ; à fusionner plus tard.
- F019 (`AppRoot.kt`) : `tuto.saved` relit et décode les préférences à chaque recomposition (déjà le cas de `resumable`) ; coût faible.
- F021 : le test place `QuickFxPanel` dans un `Dialog` ; la vraie fenêtre d'effets est un `ScaledAlertDialog` (même famille, détectée par `DialogWindowProvider`), et la feuille de la console un `ModalBottomSheet`, non testé. Le message n'apparaît pas si le compteur `TutoHints.ticked` est remis à 0 pendant que le tuto reste actif (il ne l'est qu'avec `active = false`, donc rien de visible).
- F018 : la place réservée de « Réussi ! » laisse un vide sous les toniques (capture `tuto_key_sheet`) ; voulu (la feuille ne saute plus). Si la tonalité du morceau change ailleurs pendant un choix à moitié fait, le choix local repart à zéro (`remember(harmony)`) ; cas rare.
- Rapport : il dit que seule `tuto_key_sheet` change parmi les captures ; `tuto_demo_caption` (Précédent/Suivant grisés pendant la démo, F013) et `tuto_kit_offer` (espace insécable) changent aussi, dans 60f8bcc3. Les deux sont cohérentes avec les corrections (écran entier regardé pour `tuto_demo_caption` et `tuto_key_sheet`).

## Vérifié sans défaut
- F012 : `cellsToTouch` faux pendant « Bravo » ; test sur les étapes de t9-40/52/68.
- F013 : Précédent/Suivant `enabled = false` pendant la démo, à leur place ; le test compte zéro appel.
- F016 : la hauteur du bandeau du kit s'ajoute à la place des mots sur une étape de carte (`layoutId` KIT_ROOM) ; espace insécable dans la taille ; test sur plusieurs tailles de texte.
- F017 : « Plus tard » absent pendant le téléchargement (bandeau épinglé et non épinglé), testé.
- F018 : rien choisi d'avance, la tonalité n'est posée qu'avec gamme et tonique ; puces `selectable` (rôle bouton radio, lu par TalkBack) ; testTags posés seulement quand visible ; test sur l'ordre tonique puis gamme et sur la position des puces.
- F019 : en-tête « Tuto fini : bravo ! », plus de « Reprendre » ni dans la ligne sous la page ni dans les outils ; test d'un tuto fini et d'un tuto non fini.
- F021 : popup non focalisable, 2,5 s, annoncée (liveRegion), seulement dans une fenêtre ; test avec et sans fenêtre.
- Textes d'interface en français, traductions présentes (en, de, pl, pt-BR) ; code et commentaires en anglais. Aucune sauvegarde touchée, aucun risque de plantage repéré (pas de `!!`, accès protégés).

Verdict : 0 bloquant, 0 grave
