# Ludoku icon: S to L

Tracking: [issue 9](https://github.com/lawrencemcafee/lawrence-sudoku/issues/9).

The user requested an L in the upper-left cell, with a long shadow matching the
other letters, and a PNG preview. The final artwork uses the existing vector
outlines. The L has the same stroke width and occupies the original S's bounds.
The shared shadow silhouette now follows the L's straight stem and casts toward
the lower right. Other glyphs and grid outlines are unchanged.

Deliverables:

- [1024 px PNG preview](../artwork/ludoku-icon.png)
- [Self-contained SVG preview](../artwork/ludoku-icon.svg)
- [Source and regeneration instructions](../artwork/README.md)
- [PNG exporter](../tools/render_launcher_icon.py)

The exported launcher assets cover legacy and adaptive icons. Splash variants
and the two existing metadata icons were regenerated from the same source. The
existing adaptive blue background images are unchanged. The exporter uses the
PNG artwork's translucent dark-shadow palette rather than the historical vector
copies' blue gradient.

## Image tool decision

The built-in imagegen tool was initially tried with the existing transparent
adaptive foreground. The edit specification was to replace only S with a
monoline L, preserve the grid and other letters, and replace its shadow with a
matching lower-right long shadow on a transparent canvas. That draft changed
stroke styling and returned an RGB image with a baked-in checkerboard, so it was
discarded. No CLI/API fallback was used. The delivered PNGs are deterministic
renders of the repository's editable vector paths using CairoSVG; no generated
bitmap is included in the application.

## Verification

- Source comparison confirmed that only path index 4, the S/L outline, changed
  in the canonical foreground; all grid and other letter paths match the prior
  commit.
- All 23 replaced PNG exports retain their previous dimensions and have RGBA
  channels. The adaptive foreground retains actual transparent padding.
- The full preview, legacy launcher, and adaptive foreground were visually
  inspected for the L, its shadow direction, spacing, and other lettering.
- `git diff --check` passed.
- `ANDROID_HOME=/home/lcmcafee/Android/Sdk ./gradlew :app:assembleDebug` passed.
  Build log: `/tmp/ludoku-icon-build.log`.

The rebuilt APK is available at
`app/build/outputs/apk/debug/pfa-sudoku-debug-v3.2.6.apk`. This turn prepares the
icon assets and preview; device installation remains at the preceding build.

## Reproducibility follow-up

After approving the icon, the user requested durable instructions in
`AGENTS.md`. The renderer, all three input assets, and generated outputs were
already tracked and pushed. Added [project instructions](../CLAUDE.md), expanded
the canonical [artwork recipe](../artwork/README.md), and pinned the renderer's
transitive Python dependencies as well as CairoSVG. The recipe records the
native Cairo prerequisite and the environment used for the approved exports.

Verification used a fresh `git archive` of the committed source, the updated
requirements file, and a new isolated Python environment. Invoking the renderer
from outside that checkout reproduced all 25 generated files byte for byte;
`pip check` also passed. No artwork changed. Local SHA-256 evidence:
`/tmp/ludoku-icon-repro-4clk1hpe/verified-sha256.json`.

The user then clarified that the entry point should be directly under `sudoku/`.
Neither outer instruction file existed. The canonical manual now lives in the
tracked checkout's `CLAUDE.md`, with `AGENTS.md` and both outer workspace entry
points linked to it. The manual documents how to recreate the outer links.

## Sources

- [User request and implementation tracking](https://github.com/lawrencemcafee/lawrence-sudoku/issues/9)
- Internal source and generated-artwork pointers are linked above.
