# Ludoku project instructions

When working in the Lawrence workspace, follow `~/office/lawrence/AGENTS.md`
and `~/office/lawrence/CLAUDE.md`. This file adds Ludoku-specific guidance.
Paths below are relative to this Git checkout, normally
`~/office/lawrence/sudoku/privacy-friendly-sudoku/`.

## Instruction entry points

This `CLAUDE.md` is the canonical project manual, tracked with the application.
The checkout's `AGENTS.md` is a relative symlink to it. In the surrounding
workspace, `sudoku/CLAUDE.md` points to `privacy-friendly-sudoku/CLAUDE.md`, and
`sudoku/AGENTS.md` points to `CLAUDE.md`. Edit this canonical file to update all
entry points; do not replace the symlinks with separate copies.

When setting up a new workspace with this nested checkout, create the outer
links if they are absent. Run these commands from the surrounding `sudoku/`
directory, not from the Git checkout:

```sh
ln -s privacy-friendly-sudoku/CLAUDE.md CLAUDE.md
ln -s CLAUDE.md AGENTS.md
```

The outer directory is not a Git repository; these workspace links expose the
versioned manual without moving its contents outside source control.

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
