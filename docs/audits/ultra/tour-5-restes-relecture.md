# Tour 5, lot « restes » : relecture neuve
Relu : d8a45e0fc5c23aaee25bd78252e1e922206075bd

Base fx-int dba28dab, 13 commits (b20768cf..d8a45e0f). Relu : diff complet du code, des tests, des 14 langues, du recensement, et les 4 captures.

## Paires rouge/vert
- Trois rouges rejoués sur leur parent (fichiers de test du commit rouge posés sur dba28dab, code de la base) : `ZipSavedLinesTest` échoue sur son assertion (`[Projet sauvegardé : a.pocketstudio.zip]`), `QuickStyleCardTest#theSecondButton…` sur `expected:<Régler avant de créer> but was:< Choisir ce style>`, les trois `SimpleNoDecibelTest` nouveaux sur « dB » vus (`Seuil -16 dB`, `+3,0 dB vers 120 Hz`, `−6,0 dB`, `4,2 dB plus forte`). Aucun échec de compilation ou de nœud absent : rouges honnêtes.
- Sur la tête : `SimpleNoDecibelTest` 6/6, `QuickStyleCardTest` 5/5 (130 %, 14 langues compris), `ZipSavedLinesTest`, `SimpleMix*`, `ScreenScanTest` 303 tests sans échec.
- Mineur : le correctif final des boutons (Column, 8b6fe736) n'a pas de test rouge propre ; le test « jamais coupé » passait aussi avec le FlowRow qui cachait le bouton (l'auteur le dit). Seules les captures en français gardent la visibilité du second bouton ; un `assertIsDisplayed` du bouton dans le test 130 % des 14 langues fermerait le trou.

## Recensement (2 lignes)
- Justifiées : `SimpleMixScreenshotTest.mixMuet` est un écran nouveau (nouveau test de capture), pas un plafond existant relevé. HELP 30 et CONTRAST 1, sous ceux du Mix voisin (35 et 2). Aucune ligne existante modifiée.

## « Projet sauvegardé »
- Vrai : `zipSavedLines(kept = false)` ne dit plus « Projet sauvegardé » ; le message dit que le .zip est prêt et que la copie dans Documents a échoué, avec un seul `%s`, dans les 13 traductions plus le français (14). Plus aucun autre appel de « Projet sauvegardé » dans le code (seul `SaveProjectZip.kt`).
- Mineur : en cas d'échec, la fenêtre garde le bouton « Voir mes fichiers », qui ouvre Documents où le .zip n'est justement pas. Et « garde-le depuis le menu de partage » vise le menu déjà ouvert avant la fenêtre : s'il est fermé, rien ne le rouvre d'ici. À traiter plus tard (masquer « Voir mes fichiers » si `kept` est faux, ou proposer « Partager encore »).

## Plus de dB en simple
- Correct pour les trois endroits (boîte d'effets de Pad Live, fiche de piste du Guide avec couches et chaîne, écoute de référence). Pro garde ses textes.
- Mineur : `withoutDecibels` traduit « 0 dB » de l'indicateur de réduction du compresseur par « Normal » et laisse « Seuil −40 » sans unité ; pas de test unitaire direct de `withoutDecibels` (chiffres arabes, virgule, « dBFS » non touché). `noDecibelInTheReferenceListening` teste la fonction `louderNote`, pas l'écran.

## Captures (regardées)
- `projects_new_style_card` (téléphone et 130 % fr) : « Créer le morceau » et « Régler avant de créer » entiers et visibles, sur deux lignes.
- Carte de style à 130 % en allemand, hindi et japonais, rendue à part (test jetable, non commité) : « Song erstellen » et « Vorher anpassen » entiers, second bouton visible ; idem en hindi et japonais.
- `simple_mix_muet` (téléphone et 130 %) : « Muet » sous le curseur au plus bas, rien de coupé ; à 130 % la piste Sortie déborde à droite comme sur `fr_simple_mix` de la base (défilement horizontal, déjà là).
- Hors lot, déjà sur la base : en-tête « Hip-hop » ouvert très pâle (rose sur rose), reste de texte estompé sous la barre du haut à 130 %.

## Les 12 écarts de capture signalés par l'auteur
- Rejoués dans deux worktrees jetables (base dba28dab, tête d8a45e0f) : `I18nScreenshotTest`, `TabletScreenshotTest`, `Nav*ScreenshotTest`, `ScreenshotTest`. Mêmes 5 échecs des deux côtés : `i18n/en_welcome`, `recette`, `tablet/welcome`, `tablet/nav_plus_settings`, `tablet/nav_welcome_mode`. Les images produites sont identiques octet pour octet entre base et tête.
- welcome hi et tablette paysage ne sont pas atteints (le test s'arrête à sa première image changée), des deux côtés pareil. cut_screen, cut_screen_magnifier, rhythm_scrolled_under_part : instables connues, non rejouées.
- Conclusion : ces écarts existent déjà sur la base, ils ne viennent pas de ce lot. À régler à part (réenregistrement des images de la base).

Verdict : 0 bloquant, 0 grave — fusion possible
