# Gym input decisions — recovered user answers

## Requested changes

- Remove Focused mode.
- Add the full game's edit/note (pencil) control to Gym, including candidate elimination input.

## Confirmed decisions

The user answered the three implementation questions at 2026-09-05 14:44:30 UTC:

1. **Statistics:** Keep the existing Full-mode statistics as the single Gym history. Discard Focused-mode history. Do not merge the histories or reset Full-mode progress.
2. **Input semantics:** Action-aware, strict grading. Elimination drills start with notes on; placement drills start with notes off. The input mode must match the expected action for a submitted target to count as correct.
3. **Control placement:** Match the full game's number, hint, and pencil layout. Gym has fewer controls, but the relevant controls must occupy the same layout as in a full game. The user explicitly rejected the proposed header-only placement.

Exact custom placement answer:

> the number, hint, and pencil button layout should be the same as in a full game. in gym mode, we have less buttons overall, but the buttons that are relevant in gym mode should be in the same layout as in full game mode

## Recovery source

Saved conversation: `/home/lcmcafee/.codex/sessions/2026/07/08/rollout-2026-07-08T09-35-35-019f41f0-ea65-74c2-b181-710ccbe5ddef.jsonl`.

The original request is at `2026-09-05T14:35:52.115Z`. The question/answer call is `call_9L7DBUpjEclqoZeRx4sWGnEE`; the answer was recorded at `2026-09-05T14:44:30.749Z`.

The subsequent session omitted this exchange from the active conversation context. The restoration work described in `20260905-gym-session-continuity.md` did not implement these requests. These recovered decisions govern the corrective implementation.
