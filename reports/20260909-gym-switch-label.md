# Gym mode switch label spacing

Tracking: [issue 8](https://github.com/lawrencemcafee/lawrence-sudoku/issues/8).

The user reported that Auto/Review text overlapped its switch. The original
layout enabled `textOn`/`textOff` thumb text; the compact themed thumb could not
display the words legibly. The label now uses SwitchCompat's external text area,
with an 8 dp gap before the track. Internal thumb text is disabled. A minimum
control width reserves space in the toolbar.

The existing mode update method sets the external label and accessible
description together. The persistent preference and 600 ms advancement behavior
are unchanged.

Canonical changes:

- [Switch layout](../app/src/main/res/layout/activity_gym_drill.xml)
- [Mode label update](../app/src/main/java/org/secuso/privacyfriendlysudoku/ui/GymDrillActivity.java)

## Verification

The existing pending-advance/preference-persistence device regression passed,
as did a temporary screenshot exercise in the isolated `.uitest` package.
Auto and Review labels were visually checked in dark-mode portrait and landscape
and light-mode portrait. ARC clamped the requested 320 px window to 412 px;
additional captures used 420 px portrait and 1100 px landscape widths.

Some ARC screen captures omitted unchanged parts of the screen after the switch
animation. The screenshot exercise was rerun with an explicit full-window
invalidation; labels remained readable with clear separation. This capture-only
step is outside the repository and is not an application workaround. Local
captures are under `/tmp/sudoku-gym-switch-visual/`.

Debug assembly and lint passed, with zero lint errors, 502 warnings, and two
informational findings. Logs: `/tmp/sudoku-gym-switch-verification.log` and
`/tmp/sudoku-gym-switch-captures.log`. No new permanent tests were added for this
layout change.

Installed the normal app with `adb install -r`, preserving app data. The built
and installed APK SHA-256 values match:
`28282dcddf10783624d8bac3a5362f25b331ec17392b465226e7c4a5e4ada09c`.

## Sources

- [User report and fix tracking](https://github.com/lawrencemcafee/lawrence-sudoku/issues/8)
- Internal implementation is linked above.
