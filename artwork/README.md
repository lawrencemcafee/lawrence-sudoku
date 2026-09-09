# Ludoku launcher artwork

[ludoku-icon.png](ludoku-icon.png) is the 1024 px preview.
[ludoku-icon.svg](ludoku-icon.svg) is the generated, self-contained vector preview.

The canonical letter/grid outlines are in
`app/src/main/res/drawable/ic_launcher_foreground.xml`. The shared shadow
silhouette is the first path in `ic_launcher_foreground_shadow.xml`.
The exporter reuses the existing adaptive background and matches the PNG
artwork's translucent dark shadow palette.

Regenerate the previews, launcher density variants, splash images, and metadata:

```sh
python3 -m pip install -r tools/requirements-icons.txt
python3 tools/render_launcher_icon.py
```

Edit the canonical paths, then rerun the exporter; do not edit generated PNGs.
