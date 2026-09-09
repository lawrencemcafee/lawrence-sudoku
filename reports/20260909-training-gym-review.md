# Training Gym review

## Repository baseline

Reviewed branch `lawrence` at `717b5e881f3a3decf9c7fcaaa201daadc16753fc`.
The repository is `sudoku/privacy-friendly-sudoku/`; its parent `sudoku/` is
only a containing directory. A fresh `git fetch origin` confirmed that HEAD
and `origin/lawrence` match, with zero commits ahead or behind. The working
tree and backup submodule were clean before this review.

The Gym implementation, recreation fix, and input correction are all included
in this pushed branch. [CI for the reviewed commit](https://github.com/lawrencemcafee/lawrence-sudoku/actions/runs/33976572350)
completed successfully. The two preceding Gym commits also have successful CI runs.

This review adds a discussion record only. It does not change app behavior.

## Current product contract

The canonical requirements are [Gym input decisions](20260905-gym-input-decisions.md).
The final delivery record is [Gym input implementation](20260905-gym-input-implementation.md),
which supersedes the original Focused/Full behavior in
[Skill Gym implementation](20260905-skill-gym-implementation.md).

The important constraints to preserve during iteration are:

- Display the complete saved candidate state.
- Use the full game's number, hint, and pencil arrangement.
- Start placement drills in entry mode and elimination drills in note mode;
  grade the action strictly.
- Retain the existing Full statistics as the single Gym history.

The reviewed code supports continued iteration. Immutable corpus snapshots,
deterministic shuffling, Sudoku-preserving transformations, and background
prefetching are separated from the UI. The hint engine supplies the logical
rules, and the corpus stores alternative accepted consequences. Gym statistics
are separate from ordinary game statistics and live in the existing backed-up
stats directory.

Canonical code:
[training package](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/training/),
[GymActivity](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymActivity.java),
[GymDrillActivity](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java),
[backup creator](../app/src/main/java/org/secuso/privacyfriendlysudoku/backup/BackupCreator.kt).

## Main teaching limitation: correctness does not establish technique use

Clarification from the follow-up discussion: Gym already requires an answer to
be justified by the selected technique. The overlap measured below consists of
answers justified by both the selected technique and an easier technique.
Answers outside the selected technique's accepted target set are rejected today
with generic “Incorrect” feedback. The audit does not show that the selected
technique check is missing. Proposed feedback changes are described in
[the iteration discussion](20260909-training-gym-iteration-discussion.md).

The original implementation report explicitly permits easier deductions on a
drill board. The current generator checks every named technique at each solver
snapshot, before advancing with the next ordinary deduction. Consequently,
some accepted answers can also be obtained with substantially easier techniques.
One accepted consequence completes the drill, so the app cannot infer which
technique the player used.

I audited every bundled position against the first hint returned by each
strictly lower-level technique through level 5. When actions matched, I
intersected that hint's consequences with the drill's saved accepted targets.
The following are conservative lower bounds: the audit did not enumerate every
easier deduction or inspect easier techniques above level 5.

| Selected skill | Positions with an accepted easier answer, out of 100 |
|---|---:|
| Naked Pair | 21 |
| Naked Triple | 81 |
| X-Wing | 78 |
| Swordfish | 59 |
| Jellyfish | 50 |
| X-Chain | 62 |
| XY-Chain | 83 |

For example, `X_WING_011` accepts eliminating 2 at r3c9, which its Pointing
Candidates detector also justifies. `JELLYFISH_009` accepts eliminating 2 at
r5c7 via Pointing Candidates. Coordinates refer to the untransformed corpus;
the displayed drill may have different coordinates and digits.

This follows the documented corpus policy. The new finding is how often the
overlap reaches the accepted answer itself. Current accuracy and streaks measure
first-attempt, unassisted accepted consequences; they do not measure demonstrated
mastery of the selected technique.

Recommended direction: curate positions where the selected technique is needed
for the intended deduction, and add an optional step requiring the player to
identify the supporting pattern. For example, an X-Wing exercise could ask for
its four corners before the elimination. Keep complete candidates and existing
input controls. Use overlap detection as a corpus-quality check, while preserving
logically valid alternative answers.

Sources:
[generator](../app/src/test/java/org/secuso/privacyfriendlysudoku/controller/hints/TrainingCorpusGeneratorTest.java),
[target enumeration](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/hints/HumanHintEngine.java),
[bundled corpus](../app/src/main/res/raw/gym_corpus_v1.bin),
[original scope note](20260905-skill-gym-implementation.md).

## Feedback and practice progression

The next valuable UI iteration is a result review step. Today, a valid answer
disables Hint and Pencil and automatically advances after 600 ms. A player cannot
inspect why their accepted answer works after submitting it. The cached hint is
also whichever deduction the engine found first, which may differ from an
alternative answer the player submitted.

Add an optional “Why this works” / “Next” result state, with an explanation for
the actual submitted target. Preserve quick automatic advancement as a practice
preference. This needs a target-specific explanation API; showing the existing
first-match hint would sometimes explain a different deduction.

Other useful follow-ups, in order:

1. Label accuracy as first-try, unassisted accuracy. A wrong attempt followed by
   a correct answer shows “Correct” but does not increase the accuracy numerator.
   That accounting is intentional; the current label leaves its meaning implicit.
2. Offer short sets and a session recap, including missed or revealed positions.
   Current statistics are lifetime aggregates, and closing/reopening a skill
   starts a new random session. Activity recreation preserves the active session.
3. Add mixed-technique practice once targeted practice gives reliable evidence of
   recognition. The current skill title already tells the player what to look for.

Sources:
[completion and hint handling](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java),
[statistics model](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/training/TrainingStats.java),
[UI labels](../app/src/main/res/values/strings.xml).

## Verification

Fresh unit execution passed: 45 tests passed, zero failures or errors, and the
opt-in offline corpus generator was skipped. This includes validation of every
bundled position and sampled transformations. Debug assembly and lint also
completed successfully; Gradle reused their unchanged outputs. The lint report
has zero errors and retains its existing warnings.

All 14 device tests passed with zero failures or errors: the existing 13 tests
plus the temporary fixed-clue input test. The extra test confirmed that both
input orders preserve attempts, accuracy, and an established streak when a fixed
clue is tapped. `GameController.isValidCellSelected()` excludes fixed cells, so
the suspected input defect was ruled out. Existing tests also cover recreation,
hint accounting, both input orders, pencil behavior, control placement, and the
statistics migration.

No blocking implementation defect was confirmed in the reviewed paths. The
technique-overlap and result-review findings above are product limitations to
address in the next iteration. This verification does not establish usability
across every device size or prove which technique a player actually uses.

Generated evidence:
[unit report](../app/build/reports/tests/testDebugUnitTest/index.html),
[device report](../app/build/reports/androidTests/connected/uiTest/index.html),
[lint report](../app/build/reports/lint-results-debug.html).

Commands:

```sh
ANDROID_HOME=/home/lcmcafee/Android/Sdk ./gradlew \
  :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain

ANDROID_HOME=/home/lcmcafee/Android/Sdk ./gradlew \
  --init-script /tmp/sudoku-gym-review-ti2bndlm/review.init.gradle \
  :app:testDebugUnitTest :app:connectedUiTestAndroidTest --console=plain
```

The temporary init script explicitly makes the unit-test task execute again,
in addition to registering the extra device test source. Source and resource
files in the repository remain unchanged.

Local audit artifacts are under `/tmp/sudoku-gym-review-ti2bndlm/`:

- `CorpusAudit.java` and `corpus-audit.txt`: executable corpus overlap audit and results.
- `src/GymReviewProbeTest.java`: temporary device characterization test using
  real board touch dispatch and number-button clicks.
- `review.init.gradle`: adds the temporary test source without modifying the repository.
- `verification.log`: unit and device test output.

The temporary device probe used the existing isolated `com.lawrence.ludoku.uitest`
application. The test runner removed the test packages afterward; package
enumeration confirms only the normal Ludoku package remains. The review did not
install a new build into the normal app or alter its games or Gym history.

At the end of the initial review, HEAD matched `origin/lawrence`, and this review
report was the only uncommitted file.

## Sources

- [Reviewed commit](https://github.com/lawrencemcafee/lawrence-sudoku/commit/717b5e881f3a3decf9c7fcaaa201daadc16753fc)
- [CI for reviewed commit](https://github.com/lawrencemcafee/lawrence-sudoku/actions/runs/33976572350)
- [CI for recreation fix](https://github.com/lawrencemcafee/lawrence-sudoku/actions/runs/33974402653)
- [CI for initial Gym implementation](https://github.com/lawrencemcafee/lawrence-sudoku/actions/runs/33972216762)
- Internal requirements, source, and audit evidence are linked above.
