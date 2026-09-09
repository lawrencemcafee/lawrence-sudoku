# Current quiz accuracy in the Gym header

Tracking: [issue 7](https://github.com/lawrencemcafee/lawrence-sudoku/issues/7).

The user reported a 58% header after correctly answering the first question of a
new quiz. The header still read persisted per-skill accuracy and streaks from
`TrainingStatsRepository`; it had not been converted when ten-question quizzes
were introduced. This report supersedes that header behavior in
[the quiz implementation](20260909-gym-quizzes-and-review.md).

`GymDrillActivity.updateQuizScore()` now derives the score from the current
`TrainingQuiz` outcomes and active-question attempt state. The header explicitly
says “This quiz,” displays percent correct and correct/attempted counts, and
shows “no answers yet” before the first attempt. One first-try success therefore
shows “This quiz: 100% correct (1/1).” Unattempted questions do not reduce accuracy.

Scoring matches the recap: first-try answers without hints earn credit. An active
question with an error or hint enters the denominator once; further guesses,
eventual completion, and explanation review cannot count it twice. Quiz state
already survives activity recreation, so no additional counters or persistence
format are needed. Starting another quiz resets the displayed score. Lifetime
statistics continue to be recorded and remain available in the skill catalog.

Canonical changes:

- [Quiz score rendering](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java)
- [Score strings](../app/src/main/res/values/strings.xml)
- [Android regression coverage](../app/src/androidTest/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivityTest.java)

## Verification

All 19 repository Android tests passed on Pixelbook/ARC Android 13. The new
regression seeds historical accuracy at 58% and verifies the new quiz at 100%
after its first success, 50% after an incorrect second question, and 33% after
an assisted third question. Repeated errors, recreation, and completed-answer
review preserve those counts. The existing ten-question test additionally
checks 80% against its 8/10 recap, then resets to an empty score and 100% after
the next quiz's first success.

Debug assembly and lint passed. Lint reported zero errors, 502 warnings, and two
informational findings. Verification command:

```sh
ANDROID_HOME=/home/lcmcafee/Android/Sdk ./gradlew :app:connectedUiTestAndroidTest :app:assembleDebug :app:lintDebug --console=plain
```

Log: `/tmp/sudoku-quiz-score-verification.log`. Reports:
`app/build/reports/androidTests/connected/uiTest/index.html` and
`app/build/reports/lint-results-debug.html`.

Installed the rebuilt normal APK using `adb install -r`, preserving app data.
The built APK and installed `com.lawrence.ludoku` APK have matching SHA-256:
`923ca9c9fbf61584bf7e8a9f5b93377725769addac9374c27e84e70389f72340`. Tests used the isolated `.uitest` package.

## Sources

- [User report and fix tracking](https://github.com/lawrencemcafee/lawrence-sudoku/issues/7)
- Internal implementation and prior decisions are linked above.
