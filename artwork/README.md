# Ludoku launcher artwork

[ludoku-icon.png](ludoku-icon.png) is the 1024 px preview.
[ludoku-icon.svg](ludoku-icon.svg) is the generated, self-contained vector preview.

## Official Android icon

The approved L artwork is the application's launcher icon, selected by
[`android:icon` in the manifest](../app/src/main/AndroidManifest.xml). The
[adaptive icon definition](../app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
combines the generated foreground with the existing blue background.

Keep these layers square and let Android apply the device's icon mask; the
Pixel 8 Pro displays this icon as a circle. Do not pre-crop the layers or use the
rounded preview as the adaptive foreground. Android supports different launcher
shapes using the same layers. See [Android's adaptive icon documentation](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

## Canonical inputs

| Element | Tracked source |
| --- | --- |
| Letter and grid outlines | [ic_launcher_foreground.xml](../app/src/main/res/drawable/ic_launcher_foreground.xml) |
| Long-shadow silhouette | First path of [ic_launcher_foreground_shadow.xml](../app/src/main/res/drawable/ic_launcher_foreground_shadow.xml) |
| Blue background | [ic_launcher_adaptive_back.png](../app/src/main/res/mipmap-xxxhdpi/ic_launcher_adaptive_back.png) |
| Shadow palette, composition, export sizes | [render_launcher_icon.py](../tools/render_launcher_icon.py) |
| Python dependency versions | [requirements-icons.txt](../tools/requirements-icons.txt) |

Every input is in Git. Rendering does not require an image-generation service,
API key, original design application, or files from a previous agent session.
The translucent dark shadow palette is defined in the renderer; the historical
blue gradient in the vector XML is not used for PNG exports. Only the first
path of the shadow XML is consumed; lettering comes from the foreground XML.

## Environment and regeneration

Use Python 3.11 and a system Cairo library. The approved exports were rendered
with Python 3.11.2 and Cairo 1.16.0. Python dependencies, including transitive
dependencies, are pinned in the requirements file. Different native Cairo
versions may change rasterization details, so use the recorded version when
checking byte-for-byte reproduction.

On Debian/Ubuntu, the prerequisites are `python3-venv` and `libcairo2`. Create an
isolated environment and run these commands from the repository root:

Regenerate the previews, launcher density variants, splash images, and metadata:

```sh
python3 -m venv .venv-icons
.venv-icons/bin/python -m pip install -r tools/requirements-icons.txt
.venv-icons/bin/python tools/render_launcher_icon.py
```

The renderer resolves its inputs relative to its own file, so it can also be
invoked by absolute path from another working directory.

## Editing and validation

Edit the canonical paths, then rerun the exporter; do not edit generated PNGs
or the generated preview SVG. Update a letter's outline and its shadow together.
The other paths in the historical shadow vector copies are not renderer inputs;
keep them consistent if retaining those alternate vector representations.

The renderer writes 25 files:

- `artwork/ludoku-icon.png` and `artwork/ludoku-icon.svg`.
- `app/src/main/res/mipmap-*/ic_launcher.png` (six densities).
- `app/src/main/res/mipmap-*/ic_launcher_adaptive_fore.png` (five densities).
- `app/src/main/res/mipmap-*/ic_splash.png` (five densities).
- `app/src/main/res/drawable-*/splash_icon.png` (five densities).
- `fastlane/metadata/android/{en-US,de-DE}/images/icon.png`.

The adaptive background files are existing inputs and are not regenerated.
After a no-change regeneration, `git diff --exit-code -- artwork app/src/main/res
fastlane/metadata/android` should be empty (run against a clean checkout).
After an edit, inspect the PNG preview and launcher variants, check transparency,
and run `./gradlew :app:assembleDebug` with the Android SDK configured. Commit all
changed inputs and generated outputs together.
