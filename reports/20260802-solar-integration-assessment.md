# Solar integration assessment for Ludoku

Date: 2026-08-02

## Recommendation

Make Sudoku a first-class Solar game module, using the same `android/lib` plus thin standalone `android/app` structure as the other Solar games. Reuse Ludoku's puzzle model, solver, candidate engine, human-hint engine, difficulty system, generator, and tests, but replace its Activity/XML presentation layer with a Solar-themed Compose shell.

For the first integrated version, the existing custom board view can be hosted through Compose's `AndroidView`. This limits risk while the surrounding menus, keypad, dialogs, settings, statistics, and top bar are converted to Compose. A native Compose board can follow later and is not a prerequisite for visual consistency.

## Existing contracts

Solar's unified Android app is a Compose application whose game implementations are Gradle library modules. The dependency direction is `common <- game libraries <- app`.

Canonical internal references:

- Solar app module contract: `/home/lcmcafee/office/solar/app/CLAUDE.md`
- Shared UI contract: `/home/lcmcafee/office/solar/common/CLAUDE.md`
- Included game libraries: `/home/lcmcafee/office/solar/app/android/settings.gradle.kts`
- Unified navigation and game top bars: `/home/lcmcafee/office/solar/app/android/app/src/main/java/com/solar/app/ui/navigation/SolarNavHost.kt`
- Home grid: `/home/lcmcafee/office/solar/app/android/app/src/main/java/com/solar/app/ui/home/HomeScreen.kt`
- Shared top bar: `/home/lcmcafee/office/solar/common/android/src/main/java/com/solar/common/ui/SolarTopBar.kt`
- Solar palette: `/home/lcmcafee/office/solar/app/android/app/src/main/java/com/solar/app/ui/theme/Color.kt`
- Existing Sudoku UI: `/home/lcmcafee/office/lawrence/sudoku/privacy-friendly-sudoku/app/src/main/java/org/secuso/privacyfriendlysudoku/ui/`

Ludoku currently uses AppCompat Activities, XML layouts, its own toolbar and navigation drawer, custom Views, `SharedPreferences`, and `SQLiteOpenHelper`. Those screens cannot simply be embedded without producing a nested navigation shell and visibly different interaction patterns.

## Target structure

```text
solar/sudoku/
  CLAUDE.md
  icon.png
  generate_icon.py
  android/
    lib/       # reusable engine, persistence boundary, ViewModel, Compose UI
    app/       # optional standalone Solar wrapper
```

The `lib` module should expose a small integration surface such as:

- `SudokuGameContent`
- `SudokuViewModel`
- `SudokuTopBarActions`
- `SudokuSettingsDialog`

The unified Solar app owns the `Scaffold`, `SolarTopBar`, Home navigation, and app icon. Sudoku owns everything inside its game content and its game-specific dialogs/actions.

## Required work

### 1. Separate reusable game logic from Android screens

- Move the puzzle state, candidate calculation, solver, human-hint engine, generator, difficulty grading, and related tests into the game library.
- Remove direct references from that logic to `Activity`, dialogs, Views, and global/static screen state.
- Introduce a `SudokuViewModel` with one observable UI state and explicit events for cell selection, digit entry, notes, undo, hints, new games, difficulty, and size.
- Java logic can remain Java initially. A Kotlin rewrite is not needed merely to integrate with Compose.

### 2. Put persistence behind a game-owned interface

- Define repositories for current-game saves, generated-puzzle inventory, settings, and statistics.
- Prefer Room and DataStore to match Solar's current Android conventions, but retaining the existing SQLite implementation behind the interface is acceptable for the first integration.
- Replace Activity-bound background generation with lifecycle-safe work managed by the library/ViewModel, using coroutines or WorkManager as appropriate.
- Decide whether existing Ludoku users need migration. The unified app uses a different Android application ID and cannot directly read Ludoku's private app data; migration would require an explicit export/import path.

### 3. Create the Solar presentation layer

- Implement the start/new-game UI, game status row, keypad, candidate controls, hint flow, completion state, settings, and high-score views in Material 3 Compose.
- Use `MaterialTheme.colorScheme` and shared Solar components rather than hard-coded SECUSO blue colors or AppCompat styles.
- Remove the Sudoku toolbar and navigation drawer. Do not nest an Activity within the Solar route.
- Initially host `SudokuFieldLayout` and its cell Views inside `AndroidView`; later replace the board with Compose if maintenance or accessibility warrants it.
- Keep the current human-hint `Prev` / `Next` / `Apply` flow and candidate-fill feature.

### 4. Adopt the Solar top bar

Use `SolarTopBar` with:

- Home button plus the Sudoku icon on the left, matching other game routes.
- Title `Sudoku`.
- A new-game action and settings action on the right.
- Size, difficulty, elapsed time, and mistakes in a compact status row below the top bar; crowding all of these into the app bar would make the bar inconsistent and fragile on narrow screens.

The hint button should remain near the board/keypad because it is a primary game action, not global navigation.

### 5. Register Sudoku in the unified app

- Add `:sudoku` in `app/android/settings.gradle.kts`, pointing at `../../sudoku/android/lib`.
- Add `implementation(project(":sudoku"))` to the unified app.
- Add `Routes.SUDOKU`, a `SudokuRoute`, and the Home-screen navigation callback.
- Add a sixth `AppTile` to the currently empty bottom-right slot in the 2-by-3 Home grid.
- Change the Home reveal animation/count from five tiles to six.
- Copy the 512-pixel Sudoku master icon to the unified app's assets as `sudoku_icon.png`.

### 6. Create the icon

Follow the established generated-icon system rather than creating a one-off bitmap. Add `solar/sudoku/generate_icon.py`, importing the shared Solar sun geometry.

Proposed mark:

- Solar navy square background (`#1A1A2E`).
- A simplified 3-by-3 Sudoku block/grid in pale blue-gray.
- The Solar gold sun (`#FBBF24`) occupying one cell as a given.
- Two or three restrained Solar accent-color digits or dots, with enough negative space to remain legible at launcher size.

Generate the 512-pixel master, standalone Android launcher mipmaps, and the unified app asset from the same script so the icon cannot drift between builds.

### 7. Verify the integration

- Preserve existing solver, generator, difficulty, candidate, and hint tests.
- Add ViewModel tests for new-game generation, save/restore, mode/level selection, hints, candidates, and completion/statistics.
- Add Compose tests for Home-to-Sudoku navigation, top-bar Home/settings/new-game actions, difficulty/size selection, number entry, and the hint flow.
- Build and run both the unified Solar app and optional standalone Sudoku app.
- Check compact phones, tablets/ChromeOS, dark/light theme, process recreation, and back navigation.

## Licensing gate

The current Ludoku repository carries GPLv3, while no project-level license file was found in the Solar repositories during this assessment. Linking the existing Sudoku code into the same Solar APK is likely to create a combined work for GPL purposes. The GNU GPL FAQ says that both static and dynamic linking generally produce a combined work whose distribution must comply with the GPL.

Before implementation, choose one of these paths:

1. Release the combined Solar Android app under GPLv3-compatible terms and provide corresponding source when distributing it.
2. Reimplement the Sudoku module without copying GPL-covered implementation code if Solar must use incompatible/proprietary terms.
3. Keep Ludoku as a separate installed APK and make Solar launch it by intent. This avoids tight code integration but does not deliver a seamless shared top bar and is therefore only a short-term fallback.

This is an engineering risk assessment, not legal advice.

## Suggested delivery sequence

1. Resolve licensing and create the `solar/sudoku` module skeleton.
2. Extract engine and tests behind a UI-free API.
3. Add ViewModel and repository boundaries.
4. Build the Compose shell around the existing board View.
5. Add Solar top bar, route, Home tile, and generated icon.
6. Complete persistence/migration decisions and full integration testing.
7. Consider a native Compose board only after the integrated version is stable.

## Sources

- [GNU GPL FAQ: combining work with GPL-covered code](https://www.gnu.org/licenses/gpl-faq.html)
- Ludoku license: `/home/lcmcafee/office/lawrence/sudoku/privacy-friendly-sudoku/LICENSE.md`
