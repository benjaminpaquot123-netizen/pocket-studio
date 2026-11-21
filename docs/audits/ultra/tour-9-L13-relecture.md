# Tour 9, lot L13 : relecture neuve

Relu : e2605208

Périmètre : `git diff c72349d0 e2605208 -- app/` (commits 781beb26 et 4db8f4e5 sur `TutoPanel.kt` et `Tour9L13TutoFoldedActionTest.kt`), et survol de `git diff 4dd35036 e2605208 -- app/` pour reconfirmer le grave déjà corrigé. Lecture seule, aucun test lancé.

## Défauts

- **Corrigé** (c9a0bdb1) — grave : couché (sans plafond), après « Lire la suite », les mots étaient mesurés sans poids et la barre d'actions de l'étape tombait à 0 px. Reconfirmé : `WordsRoom` reçoit toujours `weight(1f)` (tenu) ou `weight(1f, fill = false)`, la colonne mesure donc la tête et `ActionBar` avant les mots ; `Tour9L13TutoReadAllTest` mesure de vrais bords visibles et exige une hauteur > 0.

## Changement 781beb26 / 4db8f4e5 (fin de démonstration repliée)

- Pendant `FADING` avec un `need`, la `NeedLine` est enveloppée d'un `Box` étiqueté `tuto_demo_caption`. Vérifié dans `TutoController` : la phase `FADING` finit seule (boucle de `fadeMs / FADE_TICK` pas, puis le `finally` remet `demo = null`), et un toucher sur la ligne appelle `stopDemo()` (annulation, même `finally`, `restored` déjà vrai donc pas de double `demoShow(null)`) puis `need()`. L'étiquette disparaît donc toujours : `TutoJourneyTest`, `TutoKickSoundTest` (« demo over ») ne restent pas bloqués, et ils ne déplient plus en plein fondu. Pas d'état coincé, pas de plantage.
- Mise en page : le `Box` n'a pas de modificateur de taille, l'enfant garde `fillMaxWidth().height(foldedLineHeight())` ; même rendu qu'avant. `tuto_need_folded` reste cliquable (semantics de l'enfant intactes, le `Box` n'en fusionne aucune).
- Le test ajouté (`node("tuto_demo_caption") == null` → échec) échoue bien sans la correction (le commit « rouge » le prouve) et passe avec : il vérifie quelque chose.

## Notes (mineurs)

- mineur : l'étiquette `tuto_demo_caption` couvre désormais, pendant le fondu, une ligne dont le texte est le bouton de l'étape et non les mots de la démonstration ; un futur test qui lirait le texte sous cette étiquette serait trompé. Sans effet pour TalkBack (une étiquette de test n'est pas lue).
- mineur (déjà noté) : couché après « Lire la suite », le test ne vérifie que le bouton « suivant », pas qu'il reste une ligne de consigne visible à 200 % sur 360 dp.
- mineur (déjà noté) : `userFolded` n'est remis à zéro que par `start()` et `toggleOpen()`.

Aucun nouveau bloquant ni grave.

Verdict : 0 bloquant, 0 grave
