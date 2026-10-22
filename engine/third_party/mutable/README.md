Vendored subset of Mutable Instruments' code (MIT license, Emilie Gillet), unmodified except
`stmlib/utils/random.{h,cc}` (the generator's state is a relaxed atomic, for engines on two threads):

- `plaits/dsp`, `plaits/resources.{cc,h}` from https://github.com/pichenettes/eurorack
  at commit f4cfa7697095a28d292bff758beb51deb49b5a86 (STM32F projects: MIT, see each file's header)
- `stmlib/stmlib.h`, `stmlib/dsp`, `stmlib/utils` from https://github.com/pichenettes/stmlib
  at commit d18def816c51d1da0c108236928b2bbd25c17481 (`stmlib/LICENSE`)

`shim/plaits/user_data.h` replaces the module's flash-backed user data (no user banks on the phone).
Only the files needed by the Plaits voice are copied (the full repository is 90 MB).
