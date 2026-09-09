#!/usr/bin/env python3
"""Export launcher artwork from the original Android vector outlines.

Install tools/requirements-icons.txt, then run this script from any directory.
The existing adaptive background is the source for the blue tile palette.
"""

import base64
from pathlib import Path
from xml.etree import ElementTree

import cairosvg


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
ARTWORK = ROOT / "artwork"
ANDROID = "{http://schemas.android.com/apk/res/android}"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def paths(name):
    return ElementTree.parse(RES / "drawable" / name).getroot().findall("path")


GLYPHS = "".join(
    f'<path d="{path.get(ANDROID + "pathData")}" fill="white"/>'
    for path in paths("ic_launcher_foreground.xml")
)
SHADOW = paths("ic_launcher_foreground_shadow.xml")[0].get(ANDROID + "pathData")
BACKGROUND = base64.b64encode(
    (RES / "mipmap-xxxhdpi/ic_launcher_adaptive_back.png").read_bytes()
).decode("ascii")


def drawing(viewport, artwork_width, *, tile=None, shadow=True):
    """Compose vector geometry; tile is (inset, width, corner radius)."""
    scale = artwork_width / 512
    offset = (viewport - artwork_width) / 2
    definitions = '''
      <linearGradient id="shadow" gradientUnits="userSpaceOnUse"
          x1="119.85" y1="43.94" x2="518.73" y2="680.62">
        <stop offset="0" stop-color="#0c0c0c" stop-opacity="0.19"/>
        <stop offset="0.4" stop-color="#0c0c0c" stop-opacity="0.17"/>
        <stop offset="0.75" stop-color="#000000" stop-opacity="0.08"/>
        <stop offset="1" stop-color="#000000" stop-opacity="0"/>
      </linearGradient>'''
    background = ""
    if tile is not None:
        inset, width, radius = tile
        definitions += (f'<clipPath id="tile"><rect x="{inset}" y="{inset}" '
                        f'width="{width}" height="{width}" rx="{radius}"/></clipPath>')
        background = (f'<image x="{inset}" y="{inset}" width="{width}" height="{width}" '
                      f'href="data:image/png;base64,{BACKGROUND}"/>')
    artwork = f'<path d="{SHADOW}" fill="url(#shadow)"/>' if shadow else ""
    artwork += GLYPHS
    content = background + f'<g transform="translate({offset} {offset}) scale({scale})">{artwork}</g>'
    if tile is not None:
        content = f'<g clip-path="url(#tile)">{content}</g>'
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{viewport}" height="{viewport}" '
            f'viewBox="0 0 {viewport} {viewport}"><defs>{definitions}</defs>{content}</svg>')


def png(svg, path, size):
    path.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(bytestring=svg.encode(), write_to=str(path),
                    output_width=round(size), output_height=round(size))


def main():
    preview = drawing(512, 512 * .84, tile=(0, 512, 40))
    ARTWORK.mkdir(exist_ok=True)
    (ARTWORK / "ludoku-icon.svg").write_text(preview + "\n")
    png(preview, ARTWORK / "ludoku-icon.png", 1024)

    legacy = drawing(48, 38 * .84, tile=(5, 38, 3))
    foreground = drawing(108, 60)
    splash = drawing(512, 512, shadow=False)
    padded_splash = drawing(512, 512 * .8125, shadow=False)
    for density, multiplier in {"ldpi": .75, **DENSITIES}.items():
        folder = RES / f"mipmap-{density}"
        png(legacy, folder / "ic_launcher.png", 48 * multiplier)
        if density == "ldpi":
            continue
        png(foreground, folder / "ic_launcher_adaptive_fore.png", 108 * multiplier)
        png(padded_splash, folder / "ic_splash.png", 288 * multiplier)
        png(splash, RES / f"drawable-{density}/splash_icon.png", 120 * multiplier)

    metadata = drawing(166, 152 * .84, tile=(7, 152, 12))
    for locale in ("en-US", "de-DE"):
        png(metadata, ROOT / f"fastlane/metadata/android/{locale}/images/icon.png", 166)


if __name__ == "__main__":
    main()
