#!/usr/bin/env python3
"""BenStudio brand: one geometry, every file made from it.

The mark is four drum pads laid out as a "B": two pads for the stem, two "D" pads with a round
right side for the bowls (the lower one wider, as in a real B). Writes:
  docs/brand/benstudio-logo.svg / -512.png      full icon (background + mark), the master
  docs/brand/benstudio-mark.svg                 the mark alone, transparent
  docs/brand/benstudio-monochrome.svg           one colour (themed icons, notification)
  docs/brand/benstudio-lockup.svg / .png        the mark with « BenStudio » under it (1024 x 500)
  app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml
  app/src/main/res/drawable/ic_notification.xml
  app/src/main/res/drawable/avd_splash_logo.xml the splash: pads pop in, then a double heartbeat
With --preview DIR: contact sheets (48/96/512 px, light, dark, themed, notification) and the
opening as key frames, intro.mp4 and intro.gif (the same timeline as BrandIntro.kt; a sketch: the
app's own frames, with the shock waves, sparks and lights, come from BrandTest.previewFrames).
Needs cairosvg, Pillow and fontTools (pip install cairosvg pillow fonttools); ffmpeg for the video.
The Compose intro (app/.../onboarding/BrandIntro.kt) draws the same geometry and timeline: keep
them in step.

Usage: python3 scripts/brand/render_brand.py [--preview DIR]
"""
import math
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DOCS = os.path.join(ROOT, "docs", "brand")
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
FONTS = os.path.join(ROOT, "ui-components", "src", "main", "res", "font")

# --- Geometry, on the 108-unit adaptive icon canvas (safe zone: circle of radius 33 at 54,54) -----
# x, y, w, h, radii (top-left, top-right, bottom-right, bottom-left), tone, counter (a hole: tried, read « ID », off)
RAW_PADS = [
    (28.5, 26.0, 19.0, 25.0, (5, 5, 5, 5), "stem", False),
    (28.5, 54.5, 19.0, 27.5, (5, 5, 5, 5), "stem", False),
    (51.0, 26.0, 22.0, 25.0, (5, 12.5, 12.5, 5), "bowl", False),
    (51.0, 54.5, 28.0, 27.5, (5, 13.75, 13.75, 5), "lit", False),  # the pad being hit
]
MARK_SCALE = 0.9
MARK_HEIGHT = 82.0 - 26.0
BG_STOPS = (("0", "#3A3F48"), ("0.6", "#24282E"), ("1", "#16181C"))
SPLASH_BG = "#1E2126"
# Top, middle, bottom of each pad's diagonal gradient: orange to amber, a hint of magenta below.
TONES = {
    "stem": ("#FFA54A", "#F2652C", "#E04F5F"),
    "bowl": ("#FFC46B", "#FF8C3A", "#F0604F"),
    "lit": ("#FFE09A", "#FFA347", "#FF6A6A"),
}
HALO = ("#FF8A3D", "#FF5E7A")
RING = "#FFB35C"


class Pad:
    def __init__(self, raw, s=MARK_SCALE, cx=54.0, cy=54.0):
        x, y, w, h, r, tone, counter = raw
        self.x = cx + (x - 54) * s
        self.y = cy + (y - 54) * s
        self.w, self.h = w * s, h * s
        self.r = tuple(v * s for v in r)
        self.tone, self.has_counter, self.s = tone, counter, s

    @property
    def cx(self):
        return self.x + self.w / 2

    @property
    def cy(self):
        return self.y + self.h / 2

    def counter(self):
        """The bowl's hole: a « D » inside the « D »."""
        s = self.s
        cw, ch = self.w - 14 * s, self.h - 14 * s
        return (self.x + 5 * s, self.y + 7 * s, cw, ch, (2.5 * s, ch / 2, ch / 2, 2.5 * s))

    def path(self, holes=True):
        d = rr(self.x, self.y, self.w, self.h, *self.r)
        if holes and self.has_counter:
            x, y, w, h, r = self.counter()
            d += rr(x, y, w, h, *r)
        return d

    def edge(self):
        return rr(self.x + 0.5, self.y + 0.5, self.w - 1, self.h - 1, *(max(0.0, v - 0.5) for v in self.r))


def rr(x, y, w, h, tl, tr, br, bl):
    """A rounded rectangle as SVG / Android path data."""
    f = lambda v: f"{v:.3f}".rstrip("0").rstrip(".")
    tl, tr, br, bl = (min(r, h / 2) for r in (tl, tr, br, bl))
    return (f"M{f(x + tl)},{f(y)}H{f(x + w - tr)}A{f(tr)},{f(tr)} 0 0 1 {f(x + w)},{f(y + tr)}"
            f"V{f(y + h - br)}A{f(br)},{f(br)} 0 0 1 {f(x + w - br)},{f(y + h)}H{f(x + bl)}"
            f"A{f(bl)},{f(bl)} 0 0 1 {f(x)},{f(y + h - bl)}V{f(y + tl)}A{f(tl)},{f(tl)} 0 0 1 {f(x + tl)},{f(y)}Z")


PADS = [Pad(p) for p in RAW_PADS]

# --- The opening's timeline (ms from the splash's start), as BrandIntro.kt -----------------------
# The splash (at most SPLASH_MS) pops the pads in and breathes in; the app draws the rest, in step
# with the sound (render_heartbeat.py): the double heartbeat comes after the splash has gone.
POP_STAGGER, POP_MS = 60, 260
HALO_IN = (120, 520)
INHALE = (560, 1100)  # the breath in before the first beat: the mark 3.5 % smaller, the halo 20 % dimmer
INHALE_DEPTH, INHALE_DIM, RELEASE_MS = 0.035, 0.2, 70
BEATS = ((1100, 0.15, 0.6, 0.9), (1340, 0.085, 0.38, 0.6))  # start, swell, pad flash, ring alpha
BEAT_UP, SPRING_MS, SPRING_DECAY, SPRING_END = 55, 300, 85, 700
RING_MS = 480
FLASH_STAGGER, FLASH_UP, FLASH_DOWN = 35, 40, 240
RISE = (1650, 2050)
NAME = (1750, 2850)
LINE = (2550, 3150)
FADE = (4400, 4700)
LIFT, NAME_TOP = 0.13, 0.19  # of the 288 dp box: the mark goes up, the name sits under it
SPLASH_MS = 1000


def ramp(t, a, b):
    return min(1.0, max(0.0, (t - a) / (b - a)))


def ease(x):
    return x * x * (3 - 2 * x)


def ease_out(x):
    return 1 - (1 - x) ** 3


def overshoot(x, tension=2.0):
    x -= 1
    return x * x * ((tension + 1) * x + tension) + 1


def pop(i, t):
    x = ramp(t, i * POP_STAGGER, i * POP_STAGGER + POP_MS)
    return overshoot(x) if x < 1 else 1.0


def ease_cos(x):
    """Android's accelerate_decelerate interpolator."""
    return (1 - math.cos(math.pi * x)) / 2


def inhale(t):
    if t < BEATS[0][0]:
        return ease_cos(ramp(t, *INHALE))
    return 1 - ease_out(ramp(t, BEATS[0][0], BEATS[0][0] + RELEASE_MS))


def spring(u):
    if u <= 0 or u > SPRING_END:
        return 0.0
    if u < BEAT_UP:
        return ease_out(u / BEAT_UP)
    v = u - BEAT_UP
    return math.cos(2 * math.pi * v / SPRING_MS) * math.exp(-v / SPRING_DECAY)


def heartbeat(t):
    s = 1 - INHALE_DEPTH * inhale(t)
    for start, swell, _, _ in BEATS:
        s *= 1 + swell * spring(t - start)
    return s


def flash(i, t):
    v = 0.0
    for start, _, peak, _ in BEATS:
        u = t - start - i * FLASH_STAGGER
        if 0 <= u < FLASH_UP:
            v = max(v, peak * u / FLASH_UP)
        elif FLASH_UP <= u < FLASH_UP + FLASH_DOWN:
            v = max(v, peak * (1 - (u - FLASH_UP) / FLASH_DOWN))
    return v


def rings(t):
    """(scale, alpha) of each beat's ring."""
    out = []
    for start, _, _, alpha in BEATS:
        x = ramp(t, start, start + RING_MS)
        if 0 < x < 1:
            out.append((0.85 + 1.05 * ease_out(x), alpha * (1 - x)))
    return out


# --- SVG ------------------------------------------------------------------------------------------
def svg_defs():
    out = ['<radialGradient id="bg" cx="54" cy="42" r="78" gradientUnits="userSpaceOnUse">'
           + "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in BG_STOPS) + '</radialGradient>',
           f'<radialGradient id="halo" cx="58" cy="58" r="44" gradientUnits="userSpaceOnUse">'
           f'<stop offset="0" stop-color="{HALO[0]}" stop-opacity="0.42"/><stop offset="0.5" stop-color="{HALO[1]}" stop-opacity="0.12"/>'
           f'<stop offset="1" stop-color="{HALO[1]}" stop-opacity="0"/></radialGradient>']
    for i, p in enumerate(PADS):
        a, b, c = TONES[p.tone]
        out.append(f'<linearGradient id="g{i}" x1="{p.x}" y1="{p.y}" x2="{p.x + p.w * 0.35}" y2="{p.y + p.h}" gradientUnits="userSpaceOnUse">'
                   f'<stop offset="0" stop-color="{a}"/><stop offset="0.65" stop-color="{b}"/><stop offset="1" stop-color="{c}"/></linearGradient>')
        out.append(f'<radialGradient id="in{i}" cx="{p.x + p.w * 0.45}" cy="{p.y + p.h * 0.25}" r="{max(p.w, p.h) * 0.7}" gradientUnits="userSpaceOnUse">'
                   f'<stop offset="0" stop-color="#FFFFFF" stop-opacity="0.28"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient>')
        out.append(f'<linearGradient id="e{i}" x1="0" y1="{p.y}" x2="0" y2="{p.y + p.h}" gradientUnits="userSpaceOnUse">'
                   f'<stop offset="0" stop-color="#FFFFFF" stop-opacity="0.7"/><stop offset="0.35" stop-color="#FFFFFF" stop-opacity="0.08"/>'
                   f'<stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></linearGradient>')
        if p.tone == "lit":
            out.append(f'<radialGradient id="lit" cx="{p.cx}" cy="{p.cy}" r="{p.w * 0.95}" gradientUnits="userSpaceOnUse">'
                       f'<stop offset="0" stop-color="#FFB347" stop-opacity="0.55"/><stop offset="1" stop-color="#FF6A6A" stop-opacity="0"/></radialGradient>')
    return "<defs>" + "".join(out) + "</defs>"


def svg_pad(i, p, scale=1.0, light=0.0):
    t = f' transform="translate({p.cx} {p.cy}) scale({scale}) translate({-p.cx} {-p.cy})"' if scale != 1.0 else ""
    parts = [f'<g{t}>']
    if p.tone == "lit":
        parts.append(f'<circle cx="{p.cx}" cy="{p.cy}" r="{p.w * 0.95}" fill="url(#lit)"/>')
    parts.append(f'<path d="{p.path()}" fill="url(#g{i})" fill-rule="evenodd"/>')
    parts.append(f'<path d="{p.path()}" fill="url(#in{i})" fill-rule="evenodd"/>')
    parts.append(f'<path d="{p.edge()}" fill="none" stroke="url(#e{i})" stroke-width="1"/>')
    if light > 0:
        parts.append(f'<path d="{p.path()}" fill="#FFFFFF" fill-opacity="{light:.3f}" fill-rule="evenodd"/>')
    parts.append("</g>")
    return "".join(parts)


def svg_mark(t=None):
    if t is None:
        return "".join(svg_pad(i, p) for i, p in enumerate(PADS))
    return "".join(svg_pad(i, p, pop(i, t), flash(i, t)) for i, p in enumerate(PADS) if pop(i, t) > 0.001)


def svg(content, view="0 0 108 108", size=None):
    wh = f' width="{size[0]}" height="{size[1]}"' if size else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}"{wh}>{content}</svg>\n'


def full_icon_svg():
    return svg(svg_defs() + '<rect width="108" height="108" fill="url(#bg)"/><circle cx="58" cy="58" r="44" fill="url(#halo)"/>' + svg_mark())


def mark_svg():
    return svg(svg_defs() + svg_mark())


def mono_pads(scale=0.8):
    return [Pad(p, MARK_SCALE * scale) for p in RAW_PADS]


def mono_svg(color="#000000"):
    return svg("".join(f'<path d="{p.path()}" fill="{color}" fill-rule="evenodd"/>' for p in mono_pads()))


# --- The wordmark, as outlines (Inter, the app's font) --------------------------------------------
def text_path(text, font_file, size, x, baseline):
    """[text] as SVG path data, [size] units to the em, starting at [x]; returns (d, advance)."""
    from fontTools.pens.svgPathPen import SVGPathPen
    from fontTools.pens.transformPen import TransformPen
    from fontTools.ttLib import TTFont
    font = TTFont(os.path.join(FONTS, font_file))
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    scale = size / font["head"].unitsPerEm
    pen = SVGPathPen(glyphs)
    cursor = 0.0
    for ch in text:
        name = cmap[ord(ch)]
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, x + cursor, baseline)))
        cursor += glyphs[name].width * scale
    return pen.getCommands(), cursor


def wordmark(cx, baseline, size):
    """« Ben » (ExtraBold, accent gradient) and « Studio » (Medium, white), centred on [cx]."""
    _, wb = text_path("Ben", "inter_extrabold.ttf", size, 0, 0)
    _, ws = text_path("Studio", "inter_medium.ttf", size, 0, 0)
    left = cx - (wb + ws) / 2
    ben, _ = text_path("Ben", "inter_extrabold.ttf", size, left, baseline)
    studio, _ = text_path("Studio", "inter_medium.ttf", size, left + wb, baseline)
    return ben, studio, left, wb + ws


def word_defs(left, w, cx, half_line):
    return (f'<defs><linearGradient id="word" x1="{left}" y1="0" x2="{left + w * 0.4}" y2="0" gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="#FFC46B"/><stop offset="1" stop-color="#FF7A3D"/></linearGradient>'
            f'<linearGradient id="line" x1="{cx - half_line}" y1="0" x2="{cx + half_line}" y2="0" gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="#FFA347"/><stop offset="1" stop-color="#FF5E7A"/></linearGradient></defs>')


def lockup_svg(width=1024, height=500):
    mark_box = 380
    mx, my = (width - mark_box) / 2, -20
    ben, studio, left, w = wordmark(width / 2, 424, 84)
    content = (svg_defs() + word_defs(left, w, width / 2, 70) +
               f'<defs><radialGradient id="wide" cx="{width / 2}" cy="{height * 0.4}" r="{width * 0.6}" gradientUnits="userSpaceOnUse">'
               + "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in BG_STOPS) + '</radialGradient></defs>'
               f'<rect width="{width}" height="{height}" fill="url(#wide)"/>'
               f'<g transform="translate({mx} {my}) scale({mark_box / 108})"><circle cx="58" cy="58" r="44" fill="url(#halo)"/>{svg_mark()}</g>'
               f'<path d="{ben}" fill="url(#word)"/><path d="{studio}" fill="#F6F7F9"/>'
               f'<rect x="{width / 2 - 70}" y="452" width="140" height="6" rx="3" fill="url(#line)"/>')
    return svg(content, f"0 0 {width} {height}", (width, height))


# --- Android vectors ------------------------------------------------------------------------------
HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n'
        '<!-- Generated by scripts/brand/render_brand.py: edit the script, not this file. -->\n')


def argb(hex_rgb, alpha=1.0):
    return f"#{round(alpha * 255):02X}{hex_rgb.lstrip('#').upper()}"


def grad_linear(ind, x1, y1, x2, y2, stops, attr="android:fillColor"):
    items = "".join(f'{ind}        <item android:offset="{o}" android:color="{c}" />\n' for o, c in stops)
    return (f'{ind}<aapt:attr name="{attr}">\n{ind}    <gradient android:type="linear" android:startX="{x1:.3f}" android:startY="{y1:.3f}" '
            f'android:endX="{x2:.3f}" android:endY="{y2:.3f}">\n{items}{ind}    </gradient>\n{ind}</aapt:attr>\n')


def grad_radial(ind, cx, cy, r, stops, attr="android:fillColor"):
    items = "".join(f'{ind}        <item android:offset="{o}" android:color="{c}" />\n' for o, c in stops)
    return (f'{ind}<aapt:attr name="{attr}">\n{ind}    <gradient android:type="radial" android:centerX="{cx:.3f}" android:centerY="{cy:.3f}" '
            f'android:gradientRadius="{r:.3f}">\n{items}{ind}    </gradient>\n{ind}</aapt:attr>\n')


def circle(cx, cy, r):
    return f"M{cx - r:.3f},{cy:.3f}a{r:.3f},{r:.3f} 0 1,0 {2 * r:.3f},0a{r:.3f},{r:.3f} 0 1,0 {-2 * r:.3f},0z"


def vector_open(size_dp=108, viewport=108, aapt=True):
    ns = ' xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
            f'    android:width="{size_dp}dp" android:height="{size_dp}dp"\n'
            f'    android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">\n')


def vd_pad(i, p, ind, named=False):
    a, b, c = TONES[p.tone]
    out = []
    if p.tone == "lit":
        out.append(f'{ind}<path android:pathData="{circle(p.cx, p.cy, p.w * 0.95)}">\n'
                   + grad_radial(ind + "    ", p.cx, p.cy, p.w * 0.95, (("0", argb("#FFB347", 0.55)), ("1", argb("#FF6A6A", 0))))
                   + f'{ind}</path>\n')
    out.append(f'{ind}<path android:fillType="evenOdd" android:pathData="{p.path()}">\n'
               + grad_linear(ind + "    ", p.x, p.y, p.x + p.w * 0.35, p.y + p.h, (("0", argb(a)), ("0.65", argb(b)), ("1", argb(c))))
               + f'{ind}</path>\n')
    out.append(f'{ind}<path android:fillType="evenOdd" android:pathData="{p.path()}">\n'
               + grad_radial(ind + "    ", p.x + p.w * 0.45, p.y + p.h * 0.25, max(p.w, p.h) * 0.7, (("0", argb("#FFFFFF", 0.28)), ("1", argb("#FFFFFF", 0))))
               + f'{ind}</path>\n')
    out.append(f'{ind}<path android:strokeWidth="1" android:pathData="{p.edge()}">\n'
               + grad_linear(ind + "    ", 0, p.y, 0, p.y + p.h, (("0", argb("#FFFFFF", 0.7)), ("0.35", argb("#FFFFFF", 0.08)), ("1", argb("#FFFFFF", 0))), "android:strokeColor")
               + f'{ind}</path>\n')
    if named:  # the pad's flash on the beat
        out.append(f'{ind}<path android:name="flash{i}" android:fillType="evenOdd" android:fillColor="#FFFFFFFF" android:fillAlpha="0" android:pathData="{p.path()}" />\n')
    return "".join(out)


def halo_path(ind, extra=""):
    return (f'{ind}<path{extra} android:pathData="{circle(58, 58, 44)}">\n'
            + grad_radial(ind + "    ", 58, 58, 44, (("0", argb(HALO[0], 0.42)), ("0.5", argb(HALO[1], 0.12)), ("1", argb(HALO[1], 0))))
            + f'{ind}</path>\n')


def launcher_background():
    return (HEAD + vector_open() + '    <path android:pathData="M0,0h108v108h-108z">\n'
            + grad_radial("        ", 54, 42, 78, tuple((o, argb(c)) for o, c in BG_STOPS)) + '    </path>\n'
            + halo_path("    ") + '</vector>\n')


def launcher_foreground():
    return HEAD + vector_open() + "".join(vd_pad(i, p, "    ") for i, p in enumerate(PADS)) + '</vector>\n'


def launcher_monochrome():
    # Themed icons draw the layer's alpha in the wallpaper's colour; smaller, as the system's own.
    paths = "".join(f'    <path android:fillType="evenOdd" android:fillColor="#FF000000" android:pathData="{p.path()}" />\n' for p in mono_pads())
    return HEAD + vector_open(aapt=False) + paths + '</vector>\n'


def notification_pads():
    return [Pad(p, 22.0 / MARK_HEIGHT, 12, 12) for p in RAW_PADS]


def notification():
    paths = "".join(f'    <path android:fillType="evenOdd" android:fillColor="#FFFFFFFF" android:pathData="{p.path()}" />\n' for p in notification_pads())
    return HEAD.replace("-->", "Four pads as a B, one colour for the status bar. -->") + vector_open(24, 24, aapt=False) + paths + '</vector>\n'


def anim(ind, prop, start, dur, frm, to, interp="@android:interpolator/linear"):
    return (f'{ind}<objectAnimator android:propertyName="{prop}" android:valueFrom="{frm}" android:valueTo="{to}" '
            f'android:startOffset="{start}" android:duration="{dur}" android:interpolator="{interp}" />\n')


def target(name, animators):
    ind = "                "
    return (f'    <target android:name="{name}">\n        <aapt:attr name="android:animation">\n'
            f'            <set android:ordering="together">\n' + "".join(a(ind) for a in animators)
            + '            </set>\n        </aapt:attr>\n    </target>\n')


def splash_avd():
    """The splash, within 1 s: the pads pop in one after the other, the halo comes up, then the
    mark breathes in (a little smaller, the halo a little dimmer) towards the first heartbeat,
    which the app draws (BrandIntro), in step with its sound."""
    ind = "            "
    pads = halo_path(ind + "    ", ' android:name="halo" android:fillAlpha="0"')
    for i, p in enumerate(PADS):
        pads += (f'{ind}    <group android:name="pad{i}" android:pivotX="{p.cx:.3f}" android:pivotY="{p.cy:.3f}" android:scaleX="0" android:scaleY="0">\n'
                 + vd_pad(i, p, ind + "        ") + f'{ind}    </group>\n')
    body = f'{ind}<group android:name="breath" android:pivotX="54" android:pivotY="54">\n{pads}{ind}</group>\n'
    inhale_ms = INHALE[1] - INHALE[0]
    targets = target("halo", [lambda ind: anim(ind, "fillAlpha", HALO_IN[0], HALO_IN[1] - HALO_IN[0], 0, 1),
                              lambda ind: anim(ind, "fillAlpha", INHALE[0], inhale_ms, 1, round(1 - INHALE_DIM, 3), "@android:interpolator/accelerate_decelerate")])
    for i in range(len(PADS)):
        targets += target(f"pad{i}", [lambda ind, i=i, pr=pr: anim(ind, pr, i * POP_STAGGER, POP_MS, 0, 1, "@android:interpolator/overshoot")
                                      for pr in ("scaleX", "scaleY")])
    targets += target("breath", [lambda ind, pr=pr: anim(ind, pr, INHALE[0], inhale_ms, 1, round(1 - INHALE_DEPTH, 4), "@android:interpolator/accelerate_decelerate")
                                 for pr in ("scaleX", "scaleY")])
    # Nothing the splash cannot hand over without a jump: the beats come after it.
    assert min(b[0] for b in BEATS) > SPLASH_MS
    return (HEAD + '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt">\n    <aapt:attr name="android:drawable">\n'
            '        <vector android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
            + body + '        </vector>\n    </aapt:attr>\n' + targets + '</animated-vector>\n')


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def png(svg_text, out, px, py=None):
    import cairosvg
    cairosvg.svg2png(bytestring=svg_text.encode(), write_to=out, output_width=px, output_height=py or px)


# --- Previews -------------------------------------------------------------------------------------
def intro_frame(t, width=540, height=960):
    """One frame of the opening at [t] ms, as BrandIntro draws it on a phone (1 dp = 1.5 px here)."""
    dp = 1.5
    box = 288 * dp  # the splash's icon area, the intro's too
    u = box / 108
    cx, cy = width / 2, height / 2
    lift = box * LIFT * ease(ramp(t, *RISE))
    beat = heartbeat(t)
    fade = ease(ramp(t, *FADE))
    parts = [f'<rect width="{width}" height="{height}" fill="{SPLASH_BG}"/>']
    ring_svg = "".join(f'<circle cx="54" cy="54" r="{31 * s:.3f}" fill="none" stroke="{RING}" stroke-width="1.6" stroke-opacity="{a:.3f}"/>' for s, a in rings(t))
    parts.append(f'<g transform="translate({cx} {cy - lift}) scale({u}) translate(-54 -54)">{ring_svg}</g>')
    parts.append(f'<g transform="translate({cx} {cy - lift}) scale({u * beat}) translate(-54 -54)">'
                 f'<circle cx="58" cy="58" r="44" fill="url(#halo)" fill-opacity="{ramp(t, *HALO_IN) * (1 - INHALE_DIM * inhale(t)):.3f}"/>{svg_mark(t=t)}</g>')
    name = ease(ramp(t, *NAME))
    if name > 0:
        size = 34 * dp
        base = cy + box * NAME_TOP + size * 0.95 + (1 - name) * 18 * dp
        ben, studio, left, w = wordmark(cx, base, size)
        parts.append(word_defs(left, w, cx, 32 * dp) +
                     f'<g opacity="{name:.3f}"><path d="{ben}" fill="url(#word)"/><path d="{studio}" fill="#F6F7F9"/></g>')
        line = ease(ramp(t, *LINE))
        if line > 0:
            lw = 64 * dp * line
            parts.append(f'<rect x="{cx - lw / 2}" y="{base + 12 * dp}" width="{lw}" height="{3 * dp}" rx="{1.5 * dp}" fill="url(#line)"/>')
    content = svg_defs() + f'<g opacity="{1 - fade:.3f}">' + "".join(parts) + "</g>"
    return svg(f'<rect width="{width}" height="{height}" fill="#2B2F35"/>' + content, f"0 0 {width} {height}", (width, height))


def previews(out_dir):
    from io import BytesIO
    from PIL import Image, ImageDraw
    import cairosvg
    os.makedirs(out_dir, exist_ok=True)

    def raster(svg_text, px, py=None):
        return Image.open(BytesIO(cairosvg.svg2png(bytestring=svg_text.encode(), output_width=px, output_height=py or px))).convert("RGBA")

    def masked(img, shape):
        px = img.size[0]
        crop = img.crop((px * 18 // 108, px * 18 // 108, px * 90 // 108, px * 90 // 108))
        m = Image.new("L", crop.size, 0)
        d = ImageDraw.Draw(m)
        if shape == "circle":
            d.ellipse((0, 0, crop.size[0] - 1, crop.size[1] - 1), fill=255)
        else:
            d.rounded_rectangle((0, 0, crop.size[0] - 1, crop.size[1] - 1), radius=crop.size[0] * 0.3, fill=255)
        crop.putalpha(m)
        return crop

    full = full_icon_svg()
    for theme, wall in (("dark", (18, 18, 20, 255)), ("light", (238, 240, 244, 255))):
        sheet = Image.new("RGBA", (900, 1100), wall)
        x = 20
        for px in (48, 96, 512):
            big = raster(full, int(px * 108 / 72))
            for row, shape in enumerate(("circle", "squircle")):
                sheet.alpha_composite(masked(big, shape), (x, 20 + row * 540))
            x += max(px, 100) + 40
        sheet.save(os.path.join(out_dir, f"icon-{theme}.png"))
    themed = Image.new("RGBA", (520, 240), (238, 240, 244, 255))
    x = 20
    for px in (48, 96, 192):
        size = int(px * 108 / 72)
        tile = Image.new("RGBA", (size, size), (205, 230, 205, 255))
        tile.alpha_composite(raster(mono_svg("#1F4D27"), size))
        themed.alpha_composite(masked(tile, "circle"), (x, 20))
        x += px + 40
    themed.save(os.path.join(out_dir, "icon-themed.png"))
    note = Image.new("RGBA", (360, 120), (32, 33, 36, 255))
    nsvg = svg("".join(f'<path d="{p.path()}" fill="#FFFFFF" fill-rule="evenodd"/>' for p in notification_pads()), "0 0 24 24")
    x = 20
    for px in (24, 48, 72, 96):
        note.alpha_composite(raster(nsvg, px), (x, 12))
        x += px + 20
    note.save(os.path.join(out_dir, "notification.png"))
    # The opening: key frames, then a video and a GIF of every frame at 30 fps.
    frames = os.path.join(out_dir, "intro-frames")
    os.makedirs(frames, exist_ok=True)
    for t in (100, 300, 900, 1150, 1400, 1900, 2700):
        raster(intro_frame(t), 540, 960).save(os.path.join(out_dir, f"intro-{t:04d}ms.png"))
    n = int(FADE[1] / (1000 / 30)) + 8
    for k in range(n):
        raster(intro_frame(k * 1000 / 30), 540, 960).convert("RGB").save(os.path.join(frames, f"f{k:03d}.png"))
    try:
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", "30", "-i", os.path.join(frames, "f%03d.png"),
                        "-pix_fmt", "yuv420p", os.path.join(out_dir, "intro.mp4")], check=True)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", "30", "-i", os.path.join(frames, "f%03d.png"),
                        "-vf", "scale=270:480:flags=lanczos,split[a][b];[a]palettegen[p];[b][p]paletteuse", os.path.join(out_dir, "intro.gif")], check=True)
    except (OSError, subprocess.CalledProcessError) as e:
        print("ffmpeg:", e, file=sys.stderr)
    print("previews in", out_dir)


def main():
    write(os.path.join(DOCS, "benstudio-logo.svg"), full_icon_svg())
    write(os.path.join(DOCS, "benstudio-mark.svg"), mark_svg())
    write(os.path.join(DOCS, "benstudio-monochrome.svg"), mono_svg())
    write(os.path.join(DOCS, "benstudio-lockup.svg"), lockup_svg())
    write(os.path.join(RES, "ic_launcher_background.xml"), launcher_background())
    write(os.path.join(RES, "ic_launcher_foreground.xml"), launcher_foreground())
    write(os.path.join(RES, "ic_launcher_monochrome.xml"), launcher_monochrome())
    write(os.path.join(RES, "ic_notification.xml"), notification())
    write(os.path.join(RES, "avd_splash_logo.xml"), splash_avd())
    # The store icon is full-bleed: the middle 84 units, the mark bigger than on a launcher.
    png(full_icon_svg().replace('viewBox="0 0 108 108"', 'viewBox="12 12 84 84"'), os.path.join(DOCS, "benstudio-512.png"), 512)
    png(lockup_svg(), os.path.join(DOCS, "benstudio-lockup.png"), 1024, 500)
    print("wrote docs/brand/benstudio-512.png, benstudio-lockup.png")
    if "--preview" in sys.argv:
        previews(sys.argv[sys.argv.index("--preview") + 1])


if __name__ == "__main__":
    main()
