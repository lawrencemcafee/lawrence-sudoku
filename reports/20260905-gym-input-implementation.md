# Gym input implementation

Tracking: [lawrencemcafee/lawrence-sudoku#4](https://github.com/lawrencemcafee/lawrence-sudoku/issues/4).

Requirements and recovered user answers: [Gym input decisions](20260905-gym-input-decisions.md). That document is the canonical decision record; the earlier session-continuity change did not implement the requested work.

## Delivered changes

`GymActivity.java` no longer exposes a practice-mode selector. `TrainingMode.java` and the candidate-filtering API were removed. Gym always uses the complete saved candidate state. The bundled corpus is unchanged; `TrainingCorpusCodec.java` consumes and ignores its retired filter field for binary compatibility.

`activity_gym_drill.xml` now includes the full game's `content_game_view` layout, including its portrait, landscape, and density variants. `SudokuSpecialButtonLayout.java` supports selecting visible actions while preserving the standard slots. Gym exposes Hint and NoteToggle only. There is no header-only hint/pencil layout. The include's ID replaces the full-game root ID at runtime; its narrowly suppressed duplicate-ID lint warning was checked against the actual view hierarchy, which contains distinct `main_content` and `gymGameContent` roots.

`GymDrillActivity.java` initializes and restores pencil state, rejects submissions made in the wrong input mode without changing the board, and uses the existing all-valid-target grading. Valid note input removes a candidate; valid entry input places a value. Both cell-first and number-first input work. The next drill resets to its action-appropriate input mode. Existing hint and recreation behavior remains covered.

`TrainingStatsRepository.java` migrates the old mode-separated statistics to a single record per skill. Full counters and streaks are retained exactly; Focused records are intentionally discarded, not merged. The same installed file location is retained and the header advances to version 2. Discarded Focused history is not recoverable from the rewritten statistics file; recovering it would require a pre-migration backup.

The isolated test application is now visibly named **Ludoku tests**, distinguishing it from the user's app during device testing.

## Verification

Completed successfully:

```text
ANDROID_HOME=/home/lcmcafee/Android/Sdk ./gradlew \
  :app:connectedUiTestAndroidTest :app:testDebugUnitTest \
  :app:assembleDebug :app:lintDebug
git diff --check
```

- All 13 device tests passed: 11 Gym interaction tests and 2 statistics-migration tests. Coverage includes complete candidates, control placement, Naked Pair candidate removal in both input orders, wrong-mode rejection, note-state restoration, placement defaults, and exact Full-only migration/reopening.
- Unit tests: 45 passed; the opt-in corpus generator was skipped. The bundled corpus validation passed with complete candidates and unchanged target coverage.
- Debug assembly and lint passed with zero lint errors. Existing warnings and untranslated new English UI strings remain. Assembly and lint were rerun after the non-runtime include-ID lint annotation.
- On the original installed app, the catalog visibly lacks Focused/Full controls. Naked Pair shows Hint and Pencil beneath the numeric keypad in portrait. Resizing the same window to landscape moves Hint/Pencil to the left and numbers to the right, matching the shared full-game layout. Pencil toggling and its state across resizing were verified. No drill answers or hints were submitted in the original app during this corrective verification.

Canonical regression tests:

- `app/src/androidTest/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivityTest.java`
- `app/src/androidTest/java/org/secuso/privacyfriendlysudoku/controller/training/TrainingStatsRepositoryTest.java`
- `app/src/test/java/org/secuso/privacyfriendlysudoku/controller/training/TrainingCorpusTest.java`

Generated evidence: `app/build/reports/androidTests/connected/uiTest/index.html`, `app/build/reports/tests/testDebugUnitTest/index.html`, and `app/build/reports/lint-results-debug.html`. Local visual captures: `/tmp/ludoku-gym.png` and `/tmp/ludoku-gym-landscape.png`.

## Installed build

The existing `com.lawrence.ludoku` installation was updated in place using `adb install -r`, without clearing application data. Full Gym history remains; Focused history was discarded by the requested migration. The test packages were removed by the test runner, leaving only the original app package.

Artifact: `app/build/outputs/apk/debug/pfa-sudoku-debug-v3.2.6.apk`.

Final artifact/installed APK SHA-256:

```text
d3ff0b506f051c8d9d31320df76cbdd7b774219e2d9c3607324175effae59d6d
```

## Sources

- [Tracking issue](https://github.com/lawrencemcafee/lawrence-sudoku/issues/4)
- Internal requirements, source files, and generated verification evidence are linked or named above.
