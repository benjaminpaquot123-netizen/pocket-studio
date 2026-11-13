# Relecture neuve — lot « exports2 » (tour 6, zone Exports)

Relecteur neuf (n'a pas écrit le code), sur `app/.../export/ShareSheet.kt` et `app/.../export/MyExports.kt`
à HEAD de `eq/tour-6-exports2`.

relu : d8ef698c Code touché conforme au périmètre (seuls ces deux fichiers d'appli).

## Relancé et vu
- Tests du lot relancés verts : `Tour6ExportsNameWrapTest` 1/0, `Tour6ExportsOtherFilesClearTest` 1/0,
  `Tour6ExportsSaveLabelTest` 1/0, `Tour6ExportsSimpleTargetTest` 2/0, `Lot6UiShareLevelTest` 4/0,
  `BenchSelfTest` 12 (1 skip)/0.
- Rejeu rouge vérifié : E4 — code remis au parent du vert (`maxLines=2`/`Ellipsis`) + test rouge
  (`Tour6ExportsNameWrapTest`) → ROUGE (failures=1) sans le correctif. Arbre restauré, HEAD inchangé.

## Contrôles ciblés (tous OK)
- `SimpleTargetPicker` ne peut pas planter : `pick.choice` non-null, `quickTargets().forEach` sans index,
  `LoudnessTargets.label(c, null)` → nom seul sans LUFS, `simpleTargetHint` exhaustif (`else` couvre
  APPLE/FREE), un chip appelle `chooseTarget` via `if (c != pick.choice)`.
- `ui.target` devenu non-null en Simple : seuls les deux pickers le consomment, gardés par `if (!simple)`
  (Pro, au-dessus des contrôles) / `if (simple)` (Simple, sous les contrôles) — mutuellement exclusifs,
  tag `share_target` jamais en double.
- `Spacer(SCROLL_FADE)` : en fin de colonne défilante ; M4A/WAV, « Partager » et « Mes fichiers » sont
  épinglés hors du défilement → « Partager » jamais masqué.
- Ligne « Niveau » Simple : chiffre `%.1f` + `Loudness.describe`, pas d'unité « LUFS » ; `?.let { lufs -> }`
  → pas de NPE.
- E6 : seule la ligne `share_project` renommée « Envoyer le projet (.zip) » ; le vrai « Sauvegarder » de
  Mes projets n'est pas touché.
- Tests : assertions réelles (hauteur d'une ligne, dégagement du fondu, présence/absence de mots, chiffre).

## Graves
Aucun.

## Moyens (laissés ouverts, repris dans `docs/REVIEW.md`)
- `Spacer(SCROLL_FADE)` : sur une page « Envoyer » qui tenait pile, peut ajouter un petit défilement +
  l'indice de fondu. Cosmétique, accepté pour corriger E3.
- Seuil du test `Tour6ExportsNameWrapTest` (`long <= short*1.5f`) : distingue 1 vs 2 lignes seulement ;
  suffisant mais peu strict.
- (corrigé) Commentaire périmé de la ligne « Niveau » Simple (« mots seuls ») mis à jour pour E1.

Ce qui n'est pas vert : rien.

Verdict : 0 bloquant, 0 grave
