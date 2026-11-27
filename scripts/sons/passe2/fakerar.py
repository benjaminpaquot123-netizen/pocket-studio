#!/usr/bin/env python3
"""Write a small RAR5 archive with stored (uncompressed) entries, for tests only.

No `rar` binary is needed: the RAR5 headers of a stored file are simple
(signature, main header, one file header per entry, end header). RARLAB
`unrar t` checks every header CRC and every file CRC, so a test that runs
`unrar t` on this output proves the archive is well formed.

Usage: fakerar.py OUT.rar SRC_DIR   (every file under SRC_DIR, paths relative to it)
"""
import os
import struct
import sys
import zlib

SIG = b"Rar!\x1a\x07\x01\x00"


def vint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def header(body):
    """body = header type and everything after it; prefix size and CRC32."""
    size = vint(len(body))
    crc = zlib.crc32(size + body) & 0xFFFFFFFF
    return struct.pack("<I", crc) + size + body


def file_entry(name, data):
    fields = vint(0x0004)            # file flags: CRC32 present
    fields += vint(len(data))        # unpacked size
    fields += vint(0o100644)         # attributes (Unix mode)
    fields += struct.pack("<I", zlib.crc32(data) & 0xFFFFFFFF)
    fields += vint(0)                # compression info: version 0, method 0 = store
    fields += vint(1)                # host OS: Unix
    raw = name.encode("utf-8")
    fields += vint(len(raw)) + raw
    body = vint(2) + vint(0x0002) + vint(len(data)) + fields  # type 2 file, data area present
    return header(body) + data


def write(out, entries):
    """entries: list of (name with '/' separators, bytes)."""
    blob = bytearray(SIG)
    blob += header(vint(1) + vint(0) + vint(0))  # main header, no flags
    for name, data in entries:
        blob += file_entry(name, data)
    blob += header(vint(5) + vint(0) + vint(0))  # end of archive
    with open(out, "wb") as fh:
        fh.write(blob)


def main(argv):
    if len(argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    out, src = argv[1], argv[2]
    entries = []
    for root, dirs, files in os.walk(src):
        dirs.sort()
        for f in sorted(files):
            p = os.path.join(root, f)
            with open(p, "rb") as fh:
                entries.append((os.path.relpath(p, src).replace(os.sep, "/"), fh.read()))
    write(out, entries)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
