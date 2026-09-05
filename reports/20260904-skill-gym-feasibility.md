# Skill Gym Feasibility

## Goal

Add a Gym mode where a player selects one of the 23 techniques in
`app/src/main/java/org/secuso/privacyfriendlysudoku/controller/hints/HumanTechnique.java`
and repeatedly practices that technique without solving an entire Sudoku.

## Decision

An effectively pause-free Gym is feasible. Do not generate a complete random Sudoku and wait
for the selected technique to occur for every repetition. Instead:

1. Mine or construct drill positions during development, not while the player waits.
2. Validate every position with the same hint engine used by normal games.
3. Bundle a modest corpus of canonical positions in the app.
4. At runtime, apply Sudoku-preserving transformations and serve the result immediately.
5. Select and transform the next position in the background while the current one is active.

The runtime operation is then only loading small arrays, shuffling, and relabeling. It does not
involve random whole-puzzle generation, uniqueness testing, or repeated difficulty grading.

## Why Existing Puzzle Generation Is Unsuitable

`TargetedPuzzleGenerator` generates or loads a complete puzzle, solves it, repeatedly rates it,
and restores clues while trying to approach a requested overall difficulty. `GeneratorService`
therefore maintains pools in the background and accepts bounded misses. This is suitable for
normal games, but it gives unpredictable latency when searching for a particular rare technique
such as Jellyfish.

Runtime rejection sampling would be especially wasteful: generate a complete puzzle, replay its
solution path, reject it if the target technique never appears, and repeat. Overall difficulty is
not a reliable proxy for the presence of one exact technique.

## Drill Production Pipeline

Use valid 9x9 puzzle states as the initial source of truth:

1. Generate or import a valid unique puzzle and its solution.
2. Replay it with the ordered human solver.
3. Before each deduction, capture the current values and candidate masks.
4. When the selected technique is the next accepted deduction, save that snapshot and its
   structured `GameHint` result.
5. Reject snapshots that have an unintended easier move, ambiguous grading target, invalid
   candidate state, or duplicate canonical form.
6. Run every saved snapshot back through the detector as a build-time validation test.

Simple techniques can later gain specialized constructive generators. Advanced fish, wings,
coloring, and chains should initially use mined or curated snapshots because a candidate pattern
can look correct while being unreachable from a legal Sudoku state or while permitting a simpler
alternative deduction.

## Runtime Variation

Create many surface variants of each canonical position with transformations that preserve Sudoku
logic:

- Relabel digits.
- Permute rows within each band and columns within each stack.
- Permute whole bands and whole stacks.
- Transpose, rotate, or reflect the grid.

These transformations make repetition look varied and preserve the deduction. They do not create
fundamentally new logical structures, so the source corpus should contain multiple canonical
examples per technique and be expanded over time.

## Data Model

Use a dedicated model rather than pretending a drill is a normal saved game:

```text
TrainingPosition
  technique
  gameType
  values
  candidateMasks
  solution
  expectedAction
  pattern/support cells and candidates
  canonicalTemplateId
```

`CandidateState` already represents values and candidate masks. `HumanHintEngine.findTechnique`
already runs one named rule, although it is currently package-visible as a test hook.
`GameHint` already represents placements, candidate eliminations, evidence marks, links, and
explanation frames. Those should remain the shared logical source of truth for Gym validation and
normal-game hints.

## Player Interaction

The completion action depends on the technique:

- Placement techniques: select the cell and value.
- Elimination techniques: select the candidate or candidates to remove.
- Recognition-focused advanced drills: optionally require selecting the pattern/support cells
  before selecting the consequence.

For the first version, grade the logical consequence returned by `GameHint`: one placement or the
required candidate elimination set. Show immediate feedback and advance automatically after a
correct answer. Pattern selection can be added as a stricter second training mode without changing
the drill corpus.

## Recommended First Version

- Support 9x9 boards only.
- List all 23 techniques from `HumanTechnique`.
- Store exactly 100 validated canonical snapshots per technique as bundled assets.
- Shuffle without immediate repeats and transform each snapshot at runtime.
- Preload the next drill while the current one is displayed.
- Include reveal/explanation using the existing `GameHint` frames.
- Keep Gym sessions separate from normal game saves and difficulty statistics.

This provides an endless session with effectively no generation pause. "Endless" means a
continuously available stream of transformed, shuffled examples; a finite corpus cannot provide an
infinite number of fundamentally distinct logical patterns.

## Corpus Sizing

Use a strict `N = 100` canonical positions for every technique. Across all 23 techniques, that is
2,300 compact training records. Runtime transformations multiply the visible variety but should
not be counted as new logical examples.

The loader should avoid repeating a canonical template during a session. Corpus validation should
also canonicalize transformed states so the stored bank does not accidentally contain many
symmetry-equivalent copies. Additional positions can be added later without changing the initial
uniform corpus requirement.
