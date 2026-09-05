# Skill Gym implementation

Date: 2026-09-05

Tracking issue: [lawrencemcafee/lawrence-sudoku#2](https://github.com/lawrencemcafee/lawrence-sudoku/issues/2)

## Delivered behavior

- Added a Gym entry to the main menu and navigation drawer.
- Added a catalog covering all 23 `HumanTechnique` skills, grouped by the existing 10 difficulty levels.
- Added Focused and Full practice modes. Focused shows only candidates relevant to the selected technique; Full shows the complete candidate state.
- Added endless per-skill drill sessions with deterministic shuffled cycles and no repeated canonical position within a 100-position cycle.
- Every independently valid consequence of the selected technique is stored. Entering any one valid placement or candidate elimination completes the drill.
- Drill input matches full games: users can select a cell and then a number, or select a number and then a cell.
- Wrong answers remain on the current drill, show red feedback, and reset the streak. Correct answers advance after 600 ms.
- Existing human-hint explanations are available from each drill. Opening a hint records a reveal and makes that attempt assisted; applying the hint completes the drill and advances.
- Added per-technique, per-mode attempts, accuracy, reveals, current streak, and best streak, plus a reset action.
- Drill state survives activity recreation. Gym drills do not create normal game saves, normal game statistics, or timers.
- Corpus loading and preparation happen off the UI thread, and the next position is prefetched so drills can advance without runtime puzzle generation.

## Corpus

The bundled binary corpus is `app/src/main/res/raw/gym_corpus_v1.bin`.

- 23 techniques
- Exactly 100 canonical positions per technique
- 2,300 positions total
- 846,063 bytes
- SHA-256: `49b42c7c5cc41d11dc6fcf64314b4025d46bb8831338d1f9df684878a549f904`

The offline generator mines uniquely solvable 9x9 QQWing puzzles and records valid candidate states where the selected `HumanHintEngine` detector returns a target. A target-aware pass then enumerates every cell/value consequence independently justified by that technique. Runtime transformations relabel digits and apply board symmetries to the board and the complete target set.

Validation checks every bundled position for count, solution validity, candidate safety, exact all-target detector output, Focused masks, and duplicate fingerprints under digit relabeling and the eight square symmetries. It also samples transformed positions, verifies multi-answer Naked Single records, and verifies no-repeat session ordering.

Scope note: a position can still contain deductions from other techniques. Gym grading accepts only consequences justified by the technique being practiced, but it now accepts every such consequence rather than an arbitrary first match.

## Verification

The following completed successfully:

```text
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:lintDebug
git diff --check
```

The debug APK was installed on the Pixelbook Android environment (`Google_Pixelbook`, `emulator-5554`). Manual checks covered the main-menu and drawer entry, full catalog scrolling, Focused/Full switching, a Last Digit drill, a Jellyfish drill, hint display/application, immediate next-position loading, persisted statistics, immediate catalog refresh, mode-separated statistics, and acceptance of a later Naked Single when an earlier valid Naked Single was also present.

APK: `app/build/outputs/apk/debug/pfa-sudoku-debug-v3.2.6.apk`

Follow-ups: [Gym session continuity](20260905-gym-session-continuity.md) and [Gym input implementation](20260905-gym-input-implementation.md). The latter supersedes the original Focused/Full mode behavior described above.
