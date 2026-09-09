# Gym quizzes and answer review

Tracking: [Gym quizzes and review, issue 5](https://github.com/lawrencemcafee/lawrence-sudoku/issues/5).

## Accepted requirements

The user requested a switch in the top bar with two choices: automatic advance
after 600 ms, or review/explanation. The user also chose ten-puzzle quizzes in
place of endless play. These requirements supersede the optional finite-set
proposal in [the preceding discussion](20260909-training-gym-iteration-discussion.md).

The answer to the user's technique-detection question is that the submitted
cell and digit cannot identify the reasoning used. Continue accepting every
consequence justified by the selected technique, including consequences with
additional proofs. No puzzles are removed from the corpus. A supporting-pattern
selection step would be needed to assess more of the reasoning; it is outside
this change. See the grading clarification in the preceding discussion.

Retain the input and statistics decisions in
[Gym input decisions](20260905-gym-input-decisions.md).

## Implementation

- The drill toolbar contains an Auto/Review switch. Auto remains the default;
  the choice persists across activities and app launches in Gym preferences.
- A skill starts a quiz using the existing shuffled corpus stream. The toolbar
  shows question progress. Starting another quiz continues the stream, preserving
  its no-repeat ordering within each corpus cycle.
- Auto advances after the requested delay. Review opens the explanation for the
  submitted target and waits for Next question. The last question leads to Results.
- The explanation uses the existing hint text and board overlays. Target-specific
  hint lookup preserves valid alternative answers. Post-answer review does not
  consume a hint, record another attempt, or change earned credit.
- Results separate first-try unassisted answers, answers solved after errors
  without hints, and assisted answers. A question with both an error and a hint
  belongs in the assisted category.
- Each result can reopen its question and explanation. These reviews return to
  the recap and do not update performance statistics. The recap offers another
  quiz or a return to the skill catalog.
- Activity saved state retains quiz outcomes, the submitted targets, current
  question, and review position/page. Active quiz results are session state;
  this change does not add a persistent historical quiz archive.

The existing full candidate states and Hint/Pencil control positions remain in
use. Mode-sensitive answer grading and lifetime statistics retain their existing
semantics. Differentiating valid off-technique moves from generic incorrect
answers remains a separate proposal.

Canonical implementation:

- [TrainingQuiz](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/training/TrainingQuiz.java)
- [GymDrillActivity](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java)
- [HumanHintEngine](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/hints/HumanHintEngine.java)
- [HumanHintDialog](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/view/HumanHintDialog.java)
- [GameController](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/GameController.java)
- [Drill layout](../app/src/main/res/layout/activity_gym_drill.xml)
- [Quiz recap layout](../app/src/main/res/layout/gym_quiz_results.xml)

## Verification

- Unit tests: 49 passed; the opt-in corpus generator was skipped. Coverage includes
  full corpus validation, transformed target-specific proofs, quiz outcome
  accounting, duplicate protection, and saved-state restoration.
- Android device tests: all 18 repository regressions passed on Pixelbook/ARC,
  Android 13. They cover both advancement modes, changing the switch during the
  delay, ten-question completion, mixed outcomes, explanation overlays, rotation,
  recap review, preference persistence, and avoiding duplicate statistics.
- An additional temporary instrumentation exercise captured portrait and
  landscape drills, explanation pages, and the recap. The final device run
  therefore reported 19 passing tests. The capture fixture was outside the repo
  and used the isolated `.uitest` package.
- Debug assembly passed. Android lint completed with zero errors, 501 warnings,
  and two informational findings; warnings include existing debt and untranslated
  new strings. `git diff --check` passed.

The full unit run is logged at `/tmp/sudoku-gym-quiz-verification.log`. Final
device/build/lint verification is logged at
`/tmp/sudoku-gym-quiz-final-verification.log`. Generated reports are under
`app/build/reports/tests/testDebugUnitTest/`,
`app/build/reports/androidTests/connected/uiTest/`, and
`app/build/reports/lint-results-debug.html`. Visual captures are local at
`/tmp/sudoku-gym-quiz-visual/screenshots/`.

Installed `app/build/outputs/apk/debug/pfa-sudoku-debug-v3.2.6.apk` with
`adb install -r`, preserving application data. The installed normal package
`com.lawrence.ludoku` and the built APK have matching SHA-256:
`35f2660f6f6e36bf6cf62d0f2e46c6bf82d35e4f85768464f77e13123226924f`.
The test packages were removed by the runner. The device disables `run-as`, so
normal application statistics were not independently compared byte for byte.

Visual inspection also identified an existing dark-theme sizing issue: the
night theme omits the daytime control-sizing attributes, causing excessive
icon padding in narrow portrait windows. The affected code was unchanged by
this feature. Follow-up: [dark-theme controls, issue 6](https://github.com/lawrencemcafee/lawrence-sudoku/issues/6).

## Sources

- [Tracking issue](https://github.com/lawrencemcafee/lawrence-sudoku/issues/5)
- [Dark-theme control sizing follow-up](https://github.com/lawrencemcafee/lawrence-sudoku/issues/6)
- Internal requirements and source files are linked above.
