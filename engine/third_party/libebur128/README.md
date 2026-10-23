Vendored copy of libebur128 (EBU R 128 / ITU-R BS.1770-4 loudness, LRA and true peak), MIT license
(`COPYING`, © 2011 Jan Kokemüller), unmodified:

- `ebur128.c`, `ebur128.h` from https://github.com/jiixyj/libebur128 tag v1.2.6
  (commit 67b33abe1558160ed76ada1322329b0e9e058b02, 2021-02-14), directory `ebur128/`
- `queue/sys/queue.h` from the same commit (`ebur128/queue/sys/queue.h`, BSD-3-Clause,
  `queue/LICENSE.txt`), for platforms without `<sys/queue.h>`

Used by the song bench (`engine/bench/ps_measure.cpp`), off the audio thread: it allocates.
