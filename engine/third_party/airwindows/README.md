Airwindows ClipOnly2 by Chris Johnson, MIT (`LICENSE`), from https://github.com/airwindows/airwindows
at commit e68f75154ed0520fc5757fec9e7792dd1747e5ee (2026-10-20), `plugins/LinuxVST/src/ClipOnly2`.

Kept here as the reference source; it is not compiled. Its "compressed chunk" (the clip and the
softening of the samples around a clip) is ported into `core/MasterChain.cpp` (`clipOnly2`), with
the fixed clip level 0.9549925859 made a parameter and run at 4x (HIIR) as at 44.1 kHz.
