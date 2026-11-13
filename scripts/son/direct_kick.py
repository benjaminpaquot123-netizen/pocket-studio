#!/usr/bin/env python3
"""Tour 6, siège son: the kick hit lift heard LIVE (ps_live, the engine as the phone runs it). For each style, LIVE_DIR
holds <style>.wav and <style>.kick.wav (speaker off) and <style>.hp.wav (command 210 on, the phone's speaker), each played by ps_live --wav (the kick alone: --solo 16). The hits are read on the kick alone and
the lift is son6_kick.py's (ps_measure --punch's « rise »), over the whole play from its 4th second, at full range and
through the two phone models.

  direct_kick.py LIVE_DIR style [style …]   → one TSV row per style and version on stdout
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import son6 as S  # noqa: E402
import son6_kick as K  # noqa: E402


def main(argv):
    d = argv[0]
    print("style\tversion\tcoups\tcoup_plein\tcoup_tel\tcoup_tel400")
    for s in argv[1:]:
        for v, name in (("", "sans haut-parleur"), (".hp", "haut-parleur")):
            mix = S.read_wav(os.path.join(d, f"{s}{v}.wav"))
            # The hits of the kick played without the speaker's processing (the same timeline): the processed kick
            # alone has hits of its own (its harmonics), not the song's.
            kick = S.read_wav(os.path.join(d, f"{s}.kick.wav"))
            start = 4 * S.RATE
            hits = [h for h in K.onsets(kick) if h - start >= 0 and h + 2 * S.RATE < len(mix)]
            row = [s, name, str(len(hits))]
            for pw in (None, lambda f: S.phone_power(f), S.phone400_power):
                y = S.shape(mix, pw) if pw else mix
                m = y.mean(axis=1)
                row.append(S.fmt(K.rise(m * m, hits)))
            print("\t".join(row), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
