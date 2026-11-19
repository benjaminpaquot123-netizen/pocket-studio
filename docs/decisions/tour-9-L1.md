# Tour 9, lot L1 (tuto panel hides target: D02, D13, D14)

- The panel gets a ceiling = room under the top bar minus TUTO_SCREEN_LEAST (200 dp × font scale, 1–2). Folded and taller than that, it shows a compact one-line form (bar + folded line + unfold). Reason: big font made the folded panel ~260 dp, leaving ~23 dp to the grid. Back: drop `ceiling` in AppRoot.
- The compact form applies only when folded, so 100 % layouts are unchanged.
- A nav tab touched by hand during an open « Je le fais » step (spot non-empty) folds the panel (tablet: Mix ▶ under the panel). Manual steps keep it open (TutoKickSoundTest). Back: remove the `tutoTabTouched` effect.
- The piano roll and pad grids (D02) are covered by the same general ceiling, with no case-by-case code.
- D14: « Lire la suite » puts the open panel in a reading mode: it may grow up to the status bar, sheds its head row, kit and pinned « Pourquoi » one by one while the instruction still does not fit, and pages only if it still overflows; « Replier » ends the mode. Reason: under the ceiling, paging left the end of the instruction cut at 200 %. Back: drop `readAll` from `reading` in TutoPanel.kt.
- D13: a loudness step (check « loudness: ») reveals the « Volume visé » chips (spot `mixer_target`, AppRoot `tutoPlaceFor`) before the master card. Reason: the card, taller than the room above the panel, came top first and left its chips under the panel. Back: `tutoPlaceFor` returns its ids unchanged. The spot is one line in mixer/MasterLoudness.kt (L8's file), granted by the chief.
- Tour6GuideCoquilleLookTest audio-page captures: restored to the base. Their only difference (chip shimmer, touch ripple) changes from run to run and is red on the base too (tour 8).
