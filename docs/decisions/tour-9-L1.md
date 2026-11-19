# Tour 9, lot L1 (tuto panel hides target: D02, D13, D14)

- The panel gets a ceiling = room under the top bar minus TUTO_SCREEN_LEAST (200 dp × font scale, 1–2). Folded and taller than that, it shows a compact one-line form (bar + folded line + unfold). Reason: big font made the folded panel ~260 dp, leaving ~23 dp to the grid. Back: drop `ceiling` in AppRoot.
- The compact form applies only when folded, so 100 % layouts are unchanged.
- A nav tab touched by hand during an open « Je le fais » step (spot non-empty) folds the panel (tablet: Mix ▶ under the panel). Manual steps keep it open (TutoKickSoundTest). Back: remove the `tutoTabTouched` effect.
- The piano roll and pad grids (D02) are covered by the same general ceiling, with no case-by-case code.
- D14 (« Lire la suite » should unfold the whole instruction) is not addressed by this lot: left to the chief.
- Tour6GuideCoquilleLookTest captures re-recorded: the panel is identical, only the chip shimmer and the ripple timing moved.
