# Difficulty Scale and Puzzle Inventory Plan

Date: 2026-07-27

## Decision summary

- Replace the four selectable difficulties with an exact integer scale from 1 through 10.
- Derive the display category from the number: 1–2 Beginner, 3–4 Easy, 5–6 Moderate, 7–8 Hard, and 9–10 Challenge.
- Rate puzzles by running the same human-technique engine used by hints, rather than QQWing's much smaller technique set.
- Steer clue removal toward a requested level, but always solve and rate the finished puzzle before accepting its difficulty.
- Keep 1 as the low-water mark, 2 as the normal target, and 3 as the hard cap for each `(game type, difficulty level)` pool.
- Insert a generated puzzle only after it has been rated and only when its actual pool has room.
- Treat the `levels` table as a disposable queue during migration: recreate it empty under the new schema, then regenerate bounded pools. Saved games, daily history, and statistics must not be deleted.
- Let the user switch globally between ten numbered levels and five named
  categories. Numbered mode is the first-install default at Level 5.
- Persist main-menu changes immediately. Numbered-to-named selects the
  containing category; named-to-numbered selects that category's lower level.
- Open High Scores at the main menu's current selection without allowing
  statistics browsing to mutate that selection.

## Implementation status — 2026-07-31

The `lawrence` branch now contains the canonical model, shared human grader,
target-aware generator, version 3 queue schema, bounded inventory operations,
mode-aware main and statistics selectors, exact save/daily/stat persistence,
and compatibility paths for legacy saves and category statistics.

The deterministic 9x9 calibration test produces and re-rates exact targets
1 through 10. Other board sizes remain honestly rated and can use verified
off-target results, but their full 1–10 workload normalization remains a
separate rollout item as specified in Phase 5.

Validation completed so far:

- complete debug unit suite passes;
- debug APK assembles as `com.lawrence.ludoku` with label `ludoku`;
- the first lint pass identified only nine new minimum-SDK compatibility
  errors, all corrected with API-17-safe equivalents;
- the APK installs and launches successfully in the Pixelbook's ChromeOS
  ARCVM environment;
- the numbered selector opens at its intended Level 5 default during initial
  app setup;
- numbered Level 10 converts to named Challenge, and switching back selects
  Level 9, the category's lower exact level;
- High Scores opens at the main menu's Level 9 selection, and browsing another
  statistics level does not change the main menu;
- an empty Level 10 pool shows an explicit generating state, creates an exact
  Level 10 puzzle, and opens it automatically without a second button press;
- no fatal Android runtime or SQLite errors occurred during these checks.

## What the app does today

The active generator in
`app/src/main/java/org/secuso/privacyfriendlysudoku/controller/qqwing/QQWing.java`
forms a puzzle as follows:

1. Solve an empty randomized board to produce a complete valid grid.
2. Copy the complete grid into the puzzle.
3. Visit cells in random order and remove clues.
4. Keep each removal if the puzzle still has exactly one solution.
5. Solve the resulting puzzle and classify it.

The requested difficulty does not guide clue removal. The
`QQWing(GameType, GameDifficulty)` constructor receives the requested
difficulty, but the generation algorithm does not use it to shape the puzzle.
`GeneratorService` therefore generates a random minimal puzzle, grades it, and
retries until it happens to land in the requested category.

The old classifier in `QQWing.getDifficulty()` is coarse:

- any guess means Challenge;
- pairs, pointing candidates, or box-line reduction mean Hard;
- enough hidden singles mean Moderate;
- naked singles mean Easy.

It does not represent the richer techniques now available in
`controller/hints/HumanHintEngine.java`, and it cannot distinguish a
book-style introductory puzzle from a long, sparse singles-only puzzle.

## Cause of the “available puzzles” bug

`controller/GeneratorService.java` currently calls `dbHelper.addLevel(level)`
before comparing the generated difficulty to the requested difficulty. Every
failed attempt is therefore saved in its actual category while generation
continues. Categories that occur frequently grow without a limit, while the
rare target may remain at three.

`NewLevelManager.PRE_SAVES_MAX` is not enforced. Counts also materialize every
matching database row through `getLevels(...).size()` rather than issuing a
SQL `COUNT(*)`.

## Canonical 1–10 scale

The technique band sets the base level. Workload then chooses the lower or
upper level within the named category. Workload includes the number of
deductions, clue ratio, length of forced sequences, and how many elementary
moves are available at each step.

| Level | Category | Required solving profile |
|---:|---|---|
| 1 | Beginner | Last-digit and obvious naked-single placements; high move availability and a generous clue count |
| 2 | Beginner | Naked singles only, but with fewer simultaneous obvious moves or a longer solve sequence |
| 3 | Easy | Hidden singles, with a short and forgiving solve path |
| 4 | Easy | Heavier hidden-single work and/or pointing or claiming candidates |
| 5 | Moderate | Locked candidates and naked/hidden pairs |
| 6 | Moderate | Triples or quads, or sustained lower-technique workload |
| 7 | Hard | X-Wing, Skyscraper, or 2-String Kite |
| 8 | Hard | XY/XYZ/W-Wing, Simple Coloring, or Swordfish |
| 9 | Challenge | X-Chains, XY-Chains, Jellyfish, or several advanced deductions |
| 10 | Challenge | A forcing proof or controlled search after all supported logical techniques are exhausted |

These are initial deterministic boundaries, not immutable taste judgments.
Before release, generate a calibration corpus and inspect representative
puzzles at every level. Any threshold adjustment must remain centralized in
one `DifficultyScale` implementation.

### Rating data

Create a `DifficultyRating` result containing:

- exact level, 1 through 10;
- derived category;
- hardest technique used;
- count of every technique;
- total logical steps;
- initial givens and normalized clue ratio;
- minimum/average number of elementary next moves available;
- whether forcing or search was required.

The category is derived from the exact level and is never separately stored.
The hint system and grader must share a single ordered technique registry so
they cannot disagree about technique names or ordering.

## Targeted generation

Difficulty cannot be guaranteed merely by asking a random generator for
“level 4.” It can be steered during construction and must then be verified.

1. Generate a randomized complete solution.
2. Remove clues in a randomized, optionally symmetric order.
3. After each removal or small batch, confirm that the puzzle still has one
   solution.
4. Run the human grader and retain the candidate closest to the requested
   level.
5. Stop removal when the target is reached; for Beginner puzzles, stop early
   and enforce a generous clue/move-availability floor.
6. If removal overshoots, restore selected clues and rate again.
7. Accept only a uniquely solvable puzzle whose verified rating is the exact
   requested level.
8. If the attempt budget expires, retry later; never mislabel a near match.

An off-target result may be placed in its own correctly rated pool only if
that pool is below its target. Otherwise it is discarded. This preserves useful
generation work without recreating the inventory leak.

## Bounded inventory design

Use one queue per `(game type, exact difficulty level)`:

- low-water mark: 1;
- target: 2;
- hard cap: 3.

This means a named category contains at most six queued puzzles per game type,
because each category spans two exact levels. The scheduler prioritizes the
user's currently selected game type and level, then fills other pools that are
below their low-water marks.

Database operations:

- add an integer `difficulty_level` column;
- derive the category in application code;
- add a stable puzzle hash with a unique `(game_type, puzzle_hash)` index;
- count with `SELECT COUNT(*)`;
- atomically claim and delete one queued puzzle;
- insert only in a transaction that rechecks the pool count against the cap;
- provide an explicit trim-to-cap operation as a defensive repair;
- order queued puzzles consistently rather than depending on unspecified
  SQLite row order.

Generation work must have a single active scheduler or idempotent lock so two
service starts cannot independently fill the same deficit.

## Migration and compatibility

Increment the SQLite schema version and add a real migration path.

- Recreate only the disposable `levels` queue under the new schema. Old rows
  have only a coarse category, so assigning them exact scores would invent
  precision. Fresh bounded pools are safer.
- Preserve daily Sudoku records, active/saved games, and statistics.
- Add a versioned exact difficulty field to new game saves and daily records.
- Lazily regrade a legacy saved puzzle when its original givens are available.
- Retain legacy aggregate statistics as coarse-category history rather than
  fabricating an exact 1–10 split.
- Store the last selected difficulty as an integer. Map a legacy selection to
  the lower step of its corresponding new category for a gentler upgrade.

## UI changes

- Replace the four-star chooser with a ten-step slider or discrete seek bar.
- Show both values beside it, for example `Level 2 · Beginner`.
- Rename `Levels available` to `Puzzles ready` and show the count for the exact
  selected level.
- Keep the Play action available when a puzzle exists; otherwise show
  generation state and prioritize that exact pool.
- Display exact level plus category in the game header, saved-game list, daily
  Sudoku history, and new statistics.
- Update accessibility labels and every localized string touched by the new
  control.

## Implementation todo list

### Phase 1 — shared difficulty model and grader

- [x] Add `DifficultyLevel`, `DifficultyCategory`, and the single canonical
      level-to-category mapping.
- [x] Extract the hint techniques into one ordered registry shared by hints
      and grading.
- [x] Build a headless human solver that repeatedly applies deductions and
      records a `DifficultyRating`.
- [x] Add deterministic fixtures for every supported technique and initial
      workload boundaries.
- [x] Generate and inspect a deterministic 9x9 calibration corpus for levels
      1–10.
- [ ] Normalize workload metrics for 6x6, 12x12, and 16x16 before enabling the
      full scale for those board sizes.

### Phase 2 — schema and inventory repair

- [x] Add the versioned SQLite migration and recreate only the queued-level
      table.
- [x] Store exact level and puzzle hash; derive category.
- [x] Replace list-based counts with SQL counts.
- [x] Add atomic claim, bounded insert, deduplication, and trim operations.
- [x] Replace `PRE_SAVES_MIN/MAX` with low-water, target, and hard-cap
      constants.
- [ ] Add database tests proving that repeated wrong-target generation and
      concurrent replenishment can never exceed the cap.

### Phase 3 — target-aware generation

- [ ] Separate complete-grid creation, clue removal, uniqueness checking, and
      human rating into testable components.
- [ ] Stop clue removal early for introductory puzzles.
- [x] Add clue restoration when a candidate overshoots its target.
- [x] Route verified off-target results only into under-target matching pools.
- [x] Add attempt budgets and bounded, resumable background replenishment.
- [ ] Test uniqueness, exact rating, deduplication, and bounded storage across
      large generated samples.

### Phase 4 — 1–10 user experience

- [x] Add mode-aware ten-step and five-category main-menu selectors.
- [x] Persist and restore the current selection, including deterministic mode
      conversion.
- [x] Update game launch parameters, game state, daily Sudoku, saved-game
      display, and statistics.
- [x] Replace `Levels available` with exact/category-pool `Puzzles ready`.
- [ ] Add accessibility coverage, localization, and UI tests.

### Phase 5 — rollout and calibration

- [ ] Ship and validate 9x9 first, where the human technique scale is easiest
      to calibrate.
- [ ] Measure generation time and level distribution on a physical phone.
- [ ] Inspect representative puzzles, especially levels 1, 2, 9, and 10.
- [ ] Enable other board sizes only after their normalized thresholds and
      generation budgets pass the same tests.

## Acceptance criteria

- A user can select any exact level from 1 to 10 and sees its derived category.
- Levels 1 and 2 are materially more introductory than the current Easy pool.
- Every delivered puzzle has one solution and was verified at the selected
  level by the shared human grader.
- No exact-level pool can exceed three rows, even after repeated off-target
  attempts or concurrent generator starts.
- Consuming a puzzle schedules replenishment back to two.
- Upgrading clears the bloated disposable queue without deleting games,
  history, or statistics.
- The hint engine and difficulty grader use the same technique definitions.
