# Training Gym iteration discussion

Continuation of [the implementation review](20260909-training-gym-review.md).
The user asked why other techniques' answers are accepted, whether differentiated
feedback would suffice, and what answer explanations and short practice sets
would entail. This document records clarification and proposals; no product
changes have been implemented or approved by this discussion.

Subsequent decisions and the delivered implementation are recorded in
[Gym quizzes and answer review](20260909-gym-quizzes-and-review.md), which
supersedes the advancement and optional-set proposals below.

## Grading clarification

Gym already grades membership in the selected technique's saved target set.
`HumanHintEngine.findTechniqueTargets()` constructs that set using the selected
rule and a required target. `GymDrillActivity.submitAnswer()` rejects answers
outside it. The overlap in the review means a consequence has both a selected-rule
proof and an easier-rule proof. It remains a valid answer to the selected skill.
The cell and digit alone cannot establish which proof the player used.

The proposed feedback distinction is useful for answers outside that set:

| Submitted move | Proposed response |
|---|---|
| Justified by the selected technique, including shared consequences | Accept it. |
| Valid move, but outside the selected technique's detected consequences | Explain that the move is valid but does not satisfy this drill; keep the position active. |
| Invalid move | Retain incorrect-answer feedback. |

For the second category, validation should establish correctness, and an
explanation naming another technique should come from a proof for that exact
move. A known solution can establish that a placement or elimination is
solution-consistent; it does not identify the player's reasoning. The wording
should describe the move and the exercise requirement, rather than claim to
know which technique the player used.

Suggested UI: a brief inline message such as “Valid elimination, but find one
justified by X-Wing,” with optional detail. A dialog is also possible, but repeated
dismissals would interrupt practice. Proposed accounting is to leave the board
and drill active and treat this category separately from a Sudoku error; its
effect on first-try scoring remains a product decision.

This is a bounded extension of feedback and validation. Rejecting shared
consequences would reject legitimate answers. Corpus curation or requiring a
supporting pattern is a larger, optional change if practice reveals that players
are routinely bypassing the intended skill. Recommendation after clarification:
start with differentiated feedback and move-specific explanations, and defer
mandatory pattern selection.

Sources:
[target enumeration](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/hints/HumanHintEngine.java),
[grading](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java),
[corpus overlap measurements](20260909-training-gym-review.md).

## Explanation after an answer

Reuse the current hint engine's step-by-step text and board highlighting to
explain the exact submitted move using the selected skill. For an X-Wing, show
the supporting digit at the four corners, why the two base rows force that digit
into the two crossing columns, and why the player's target elsewhere in one of
those columns loses that candidate.

After success, offer “Show reasoning” and “Next drill.” Reviewing an already
completed answer should preserve earned credit and should not count as a hint
reveal. Keep automatic advancement available for users who want continuous
repetitions. These are proposed controls, not a decision to replace the existing
input layout.

Most explanation machinery already exists in `HumanHintDialog` and the hint
engine's frames. The current drill caches the first proof found for the position;
another accepted answer may need a different proof. Expose a target-specific
proof and retain the original snapshot for result review. The review flow should
also avoid the current dialog's Apply action, because the answer has already
been applied.

Sources:
[existing explanation UI](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/view/HumanHintDialog.java),
[hint frames](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/hints/HumanHintEngine.java),
[current completion behavior](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java).

## Short sets and recaps

An optional finite set gives the player a stopping point and feedback about that
practice session. It uses the existing per-technique drills and retains endless
practice as an option.

Illustrative proposal: select Naked Pair, choose a set of ten drills, and see
progress such as “4 of 10.” A completed set could show:

- Seven solved correctly on the first try without help.
- Two solved after errors without help.
- One solved with a hint.

Those are hypothetical, mutually exclusive categories; a drill with both an
error and a hint belongs in the assisted category. The recap offers review or
retry of the three difficult positions, another set, or continued free practice.
No speed target or mandatory timer is proposed.

This requires session outcomes and references to the positions that caused
trouble. Current `TrainingStats` stores lifetime counters and streaks, so it
cannot reconstruct that recap on its own. Retry results should remain
distinguishable from first exposure when evaluating progress.

Source:
[current statistics model](../app/src/main/java/org/secuso/privacyfriendlysudoku/controller/training/TrainingStats.java).

## Sources

Internal implementation and the prior review are linked above. All new controls,
session sizes, and example results in this document are proposals, not deployed
behavior or observed user performance.
