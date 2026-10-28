#!/usr/bin/env python3
"""The tester's finger on the « testeur humain » bench (scripts/human-tester.sh).

    human-act.py <seat folder> regarde                  the screen as it is now
    human-act.py <seat folder> touche X Y               a tap at (X, Y), the screen's pixels
    human-act.py <seat folder> appui-long X Y           a long press
    human-act.py <seat folder> glisse X1 Y1 X2 Y2       a swipe (scroll, slider)
    human-act.py <seat folder> ecris TEXTE              typing in the field that has the focus
    human-act.py <seat folder> retour                   the phone's back gesture
    human-act.py <seat folder> attends SECONDES         time goes by (an example plays, a ring fades)
    human-act.py <seat folder> fin                      the end of the test

Prints the next screen's image (with a faint grid every 100 px, its numbers on the top and left
edges) and what is heard.
"""
import os
import sys
import time

from PIL import Image, ImageDraw


def turn(seat):
    try:
        with open(os.path.join(seat, "turn")) as f:
            return int(f.read().strip())
    except (OSError, ValueError):
        return 0


def wait_beyond(seat, n, seconds, what):
    end = time.time() + seconds
    while turn(seat) <= n:
        if time.time() > end:
            print(what)
            sys.exit(1)
        time.sleep(0.2)
    return turn(seat)


def gridded(seat, n):
    src = os.path.join(seat, f"screen-{n}.png")
    out = os.path.join(seat, f"screen-{n}-grille.png")
    img = Image.open(src).convert("RGBA")
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    w, h = img.size
    for x in range(100, w, 100):
        d.line([(x, 0), (x, h)], fill=(255, 0, 255, 38), width=1)
        d.rectangle([x - 14, 0, x + 14, 11], fill=(0, 0, 0, 170))
        d.text((x - 11, 0), str(x), fill=(255, 120, 255, 255))
    for y in range(100, h, 100):
        d.line([(0, y), (w, y)], fill=(255, 0, 255, 38), width=1)
        d.rectangle([0, y - 6, 30, 6 + y], fill=(0, 0, 0, 170))
        d.text((2, y - 6), str(y), fill=(255, 120, 255, 255))
    Image.alpha_composite(img, layer).convert("RGB").save(out)
    return out, (w, h)


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    seat, words = sys.argv[1], sys.argv[2:]
    n = wait_beyond(seat, 0, 900, "L'appli ne s'est pas ouverte (15 min).")
    if words[0] != "regarde":
        cmd = " ".join(words)
        tmp = os.path.join(seat, f"cmd-{n}.tmp")
        with open(tmp, "w") as f:
            f.write(cmd)
        os.rename(tmp, os.path.join(seat, f"cmd-{n}.txt"))
        if cmd == "fin":
            print("Fin du test.")
            return
        n = wait_beyond(seat, n, 300, "L'appli ne répond plus (5 min sans nouvel écran).")
    out, (w, h) = gridded(seat, n)
    print(f"Écran {n} ({w}x{h}) : {out}")
    with open(os.path.join(seat, f"sound-{n}.txt")) as f:
        print(f.read().strip())


if __name__ == "__main__":
    main()
