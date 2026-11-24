# Tour 9, zone fluide1 (C6, fluidité Mix et Live)

- Live counter: its figures move to `CounterFigures`, reading a derived (bar, beat) state; they compose again once a beat, not at each step. The ring reads the clock in `drawWithCache`/`onDrawBehind` behind its own `graphicsLayer()`, the frame path and its measure built once per size. Reason: the counter composed again 20 times in 2 s with its two FitTexts (70 scopes of 70 on Live). Back: revert 1f6ebbbb1.
- The counter's TalkBack description moves from the counter's Column to its figures' Text; the Column still merges it (merged tree unchanged, `live_counter` tag kept). Reason: the Column would otherwise compose again at each beat for its description. Back: same revert.
- Test ceiling: LIVE_MAX 35 (measured after: 7 scopes per beat, the Text's own) plus « no scope of ours over twice a beat ». The red commit's 25 was a guess before measuring; both thresholds fail the old code (70; 20 and 40 per scope).
- Mix: no repair. Its 77 scopes are texts already throttled to 4 changes a second (tour 5 « mixwake »): a number must stay to be read.
- Live pads: already in their own layers (clip); kept as a guard test, no code change.
