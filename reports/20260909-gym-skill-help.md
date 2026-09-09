# Illustrated Gym skill help

Tracking: [issue 10](https://github.com/lawrencemcafee/lawrence-sudoku/issues/10).

The user requested a question mark on the right of every skill row, opening a
dialog with an illustration above concise bullet steps. Each catalog technique
now has a focused candidate sketch and a short procedure. Tapping the row text
still launches its quiz; opening help does not create a quiz, advance the puzzle
stream, or update performance statistics.

## Implementation

- [GymActivity](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymActivity.java)
  binds the independent help action and a skill-specific accessibility label to
  the [row button](../app/src/main/res/layout/gym_skill_row.xml).
- [TechniqueLesson](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/training/TechniqueLesson.java)
  owns the example geometry and its underlying candidate constraints. Related
  subset/fish techniques share construction logic.
- [TechniqueDiagramView](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/view/TechniqueDiagramView.java)
  draws native, scalable diagrams with relevant candidates, highlighted units,
  labeled links, circled placements, and crossed-out removals. Captions describe
  the examples for screen readers. The sketches explicitly omit irrelevant notes;
  they are teaching examples rather than playable boards.
- [Lesson resources](../app/src/main/res/values/gym_lessons.xml) contain each
  caption and bullet list. The procedural text distinguishes the constraints
  of similar techniques, including XYZ-Wing targets seeing the pivot as well as
  both wings, and hidden subsets removing extras from the selected cells.
- [TechniqueHelpDialog](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/view/TechniqueHelpDialog.java)
  uses a `DialogFragment` with a scrollable illustration/caption/bullet layout,
  theme-aware colors, a Close action, and framework restoration on recreation.

No generated artwork, network access, or training-corpus loading is required to
open help. The project manual points to the lesson sources and validation.

## Verification

- [TechniqueLessonTest](../app/src/test/java/org/secuso/privacyfriendlysudoku/controller/training/TechniqueLessonTest.java)
  passed both tests across every catalog technique. An
  independent Sudoku search verifies that the examples are satisfiable and that
  every illustrated conclusion is forced. The named hint technique must also
  justify each conclusion. A second check verifies the advertised strong/weak
  relationships of every drawn link.
- [GymActivityTest](../app/src/androidTest/java/org/secuso/privacyfriendlysudoku/ui/GymActivityTest.java)
  passed all four tests on the Pixel 8 Pro in each of light and dark themes.
  The tests inject real taps on every help button and on the quiz-launch text,
  check the lesson content and accessibility labels, preserve Gym preferences
  and history, restore an open lesson after recreation, and scroll to the final
  bullet in landscape while keeping Close visible.
- Each row owns its quiz click action so its focusable help button can have a
  separate action. This avoids relying on ListView item clicks with focusable
  descendants.
- Inspected native screenshots of placements, subsets, locked candidates,
  fish, wings, coloring, chains, and landscape scrolling. Adjacent-cell links
  omit digit badges so the badges cannot obscure short dashed segments.
- Debug and isolated test APK builds passed. Build log:
  `/tmp/ludoku-skill-help-verified-build.log`. Final device logs:
  `/tmp/ludoku-lessons-final-{light,dark}-tests.log`. Screenshots:
  `/tmp/ludoku-lessons-final-{light,dark}/` (25 in each directory).
- `git diff --check` passed.

## Pixel installation

Updated `com.lawrence.ludoku` on the USB-connected Pixel 8 Pro with `adb install
-r`, then launched the normal app successfully. The installed APK matched the
build's SHA-256:
`d7536e350ffdffb6e2f6ce2ec608b0dfdbd9876b1db9ff46de7cc695546136b3`.
The normal app's Gym statistics file was byte-for-byte unchanged across the
installation. All device tests used the separate `.uitest` application.

The Pixel disconnected after the normal installation and hash checks, before
temporary test-package cleanup. The feature is installed and verified. Remaining
cleanup when serial `39290DLJG000YU` reconnects: uninstall only
`com.lawrence.ludoku.uitest.test` and `com.lawrence.ludoku.uitest`; keep the normal
`com.lawrence.ludoku` installation. The user was asked to reconnect briefly.

## Sources

- Internal implementation and test pointers are linked above. The existing
  [HumanHintEngine](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/hints/HumanHintEngine.java)
  defines the techniques accepted by the Gym.
- External cross-checks: [HoDoKu wings](https://hodoku.sourceforge.net/en/tech_wings.php),
  [chains](https://hodoku.sourceforge.net/en/tech_chains.php),
  [basic fish](https://hodoku.sourceforge.net/en/tech_fishb.php), and
  [single-digit patterns](https://hodoku.sourceforge.net/en/tech_sdp.php).
  The diagrams and lesson wording are original; no source images were copied.
