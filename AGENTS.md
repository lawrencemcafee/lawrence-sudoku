# Ludoku project instructions

When working in the Lawrence workspace, follow the parent `AGENTS.md` and
canonical `CLAUDE.md` instructions. This file adds Ludoku-specific guidance.

## Launcher icon edits

Read [artwork/README.md](artwork/README.md) before changing the icon. It documents
the canonical sources, pinned dependencies, generation commands, and output
files. The renderer and every input required for it are tracked in this repo.

- Use the existing vector paths and `tools/render_launcher_icon.py` for edits.
  Do not recreate this icon with an image model or edit generated PNGs by hand.
- Update a changed letter and its long-shadow silhouette together. Preserve the
  other letter/grid outlines and the established shadow direction and weight
  unless the user requests a redesign.
- Regenerate all outputs using the documented environment. Inspect the preview
  and both legacy/adaptive launcher variants, including actual alpha transparency.
- Run `./gradlew :app:assembleDebug` after changing Android icon resources.
- Commit source changes, dependency changes, and regenerated assets together.
  The generated preview SVG is an output; edit the canonical source files.

Keep the detailed recipe in the artwork README as the single source of truth.
