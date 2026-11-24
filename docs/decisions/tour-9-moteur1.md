# Tour 9, lot moteur1 (B1: the measure)

- Worst gap between blocks (C3): now the lateness against the block's due time, over the usual block, instead of the elapsed time over the incoming block. Reason: with unequal blocks the old reading raised false alarms and read the same lateness two ways. No public signature changed. Back: revert `c090b1c35`.
- Brick-by-brick timing lives in `core/Profile.h` behind `PS_PROFILE` (CMake option, off by default): the macros expand to nothing, so the app, the tests and the live bench compile none of it. Reason: no cost and no risk in the audio callback of the shipped engine. Back: remove the macros and the option.
- The brick table comes from `scripts/cpu-bench.sh --bricks` and `engine/bench/brick_report.py`, not from `scripts/bench/**` (outside this lot's files).
- The per-style budget is a ratio: the song's load over a fixed reference scene measured in the same pass by `ps_live` (before and after the song, the slower kept). Reason: milliseconds differ on every machine, a ratio does not. Back: drop the BUDGET block in `ps_live.cpp`.
- The budget is judged in the live bench, its logic and table are tested in the engine tests. Reason: the recorded songs need 320 MB of samples and a Gradle recording, too heavy for host tests.
- Margin 1.5 over the measured ratio. Reason: a second pass with four songs at a time stayed within 0.92–1.09 of the first; 1.5 leaves room for ARM and still catches a brick half as dear again. Back: `kMargin` in `StyleBudgets.h`.
- The budget does not judge a solo, a style without a budget, or a build that profiles or does not count allocations (it prints the ratio and says so). Reason: those runs do not measure the app's engine.
- Ratios were measured with three songs at a time on a 4-core x86_64 machine, 150 s each. To set them again: run the live bench and copy each `ratio=`.
- Gradle was run once outside the pre-flight, to record the 37 `.pslive` songs (none existed on the machine and both measures need them). Cost: one recording run. The fiche said « pas de Gradle sauf le pré-vol »; no app test was run with it.
- The git submodules were fetched (`git submodule update --init --depth 1`) to build the engine; nothing committed from it.
- Sound unchanged, checked: six songs rendered by `ps_live` before and after the lot are byte-identical.
