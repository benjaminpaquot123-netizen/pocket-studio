Vendored subset of HIIR 1.50 (2024-08-09) by Laurent de Soras, WTFPL v2 (`license.txt`), unmodified.
Source: https://ldesoras.fr/prod.html#src_hiir (archive `hiir-1.50.zip`,
sha256 8a13724e8d698a40f53687d40466443879f6250abd2229097f3990b76e6855a6).

Only the portable FPU half-band up/downsamplers are copied (`hiir/*Tpl*`, `*Fpu.h`, `def.h`,
`StageDataTpl.h`, `StageProc*`) with the precalculated oversampling coefficients `hiir/coef/C2x.h`
and `C4x.h` (integer phase delay up then down, see `oversampling.txt`). The SIMD variants, the
designer and the test bench are left out. Used by `core/Oversampling.h`.
