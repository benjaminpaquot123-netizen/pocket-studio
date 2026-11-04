#!/usr/bin/env python3
"""The tester's finger on the « testeur humain » bench (scripts/human-tester.sh).

    human-act.py <seat folder> regarde                  the screen as it is now, as text
    human-act.py <seat folder> regarde image            its image (with a grid), counted half a gesture
    human-act.py <seat folder> touche X Y               a tap at (X, Y), the screen's pixels
    human-act.py <seat folder> appui-long X Y           a long press
    human-act.py <seat folder> glisse X1 Y1 X2 Y2       a swipe (scroll, slider)
    human-act.py <seat folder> ecris TEXTE              typing in the field that has the focus
    human-act.py <seat folder> retour                   the phone's back gesture
    human-act.py <seat folder> attends SECONDES         time goes by (an example plays, a ring fades)
    human-act.py <seat folder> fin                      the end of the test

Prints the next screen as text (screen-N.txt: what TalkBack reads, each line with its middle « @ x,y »)
and what is heard. The image is asked with « regarde image »: allowed at the first screen of a new
route, or one image for 5 gestures (Audit ultra §3.1); with lit=non (texts unreadable) always.
When the bench ends the seat (budget, nothing changes, the end), says why (fin.txt).
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


def ended(seat):
    p = os.path.join(seat, "fin.txt")
    if os.path.isfile(p):
        with open(p) as f:
            return f.read().strip()
    return None


def wait_beyond(seat, n, seconds, what):
    end = time.time() + seconds
    while turn(seat) <= n:
        why = ended(seat)
        if why is not None and turn(seat) <= n:
            print(f"Fin du siège : {why}.")
            sys.exit(0)
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


def read(path, default=""):
    try:
        with open(path) as f:
            return f.read()
    except OSError:
        return default


def options(seat):
    out = {}
    for line in read(os.path.join(seat, "start.txt")).splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def gestures(seat):
    """The gestures played so far (« attends » is not one)."""
    n = 0
    k = 1
    while os.path.isfile(os.path.join(seat, f"cmd-{k}.txt")):
        if not read(os.path.join(seat, f"cmd-{k}.txt")).strip().startswith(("attends", "fin")):
            n += 1
        k += 1
    return n


def image_allowed(seat, n):
    """One image for 5 gestures, the first screen of a new route always, lit=non always."""
    if options(seat).get("lit", "").lower() == "non":
        return True, ""
    looked = [l.strip() for l in read(os.path.join(seat, "images.txt")).splitlines() if l.strip()]
    if str(n) in looked:
        return True, ""
    if "nouvelle=1" in read(os.path.join(seat, f"meta-{n}.txt")):
        return True, ""
    if len(looked) < 1 + gestures(seat) // 5:
        return True, ""
    return False, f"Plafond d'images atteint ({len(looked)} pour {gestures(seat)} gestes : une pour 5) : lis l'écran en texte."


def show_text(seat, n):
    print(read(os.path.join(seat, f"screen-{n}.txt"), f"Écran {n} : pas de texte.").rstrip())
    print(read(os.path.join(seat, f"sound-{n}.txt")).strip())


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    seat, words = sys.argv[1], sys.argv[2:]
    n = wait_beyond(seat, 0, 900, "L'appli ne s'est pas ouverte (15 min).")
    if words[0] == "regarde":
        if len(words) > 1 and words[1] == "image":
            ok, why = image_allowed(seat, n)
            if not ok:
                print(why)
                return
            looked = read(os.path.join(seat, "images.txt")).split()
            if str(n) not in looked:
                with open(os.path.join(seat, "images.txt"), "a") as f:
                    f.write(f"{n}\n")
            out, (w, h) = gridded(seat, n)
            print(f"Écran {n} ({w}x{h}) : {out}")
            return
        show_text(seat, n)
        return
    why = ended(seat)
    if why is not None:
        print(f"Fin du siège : {why}.")
        return
    cmd = " ".join(words)
    tmp = os.path.join(seat, f"cmd-{n}.tmp")
    with open(tmp, "w") as f:
        f.write(cmd)
    os.rename(tmp, os.path.join(seat, f"cmd-{n}.txt"))
    if cmd == "fin":
        print("Fin du test.")
        return
    n = wait_beyond(seat, n, 300, "L'appli ne répond plus (5 min sans nouvel écran).")
    show_text(seat, n)


if __name__ == "__main__":
    main()
