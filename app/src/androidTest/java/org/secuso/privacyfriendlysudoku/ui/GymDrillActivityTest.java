/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.hints.GameHint;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanHintEngine;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStats;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingPosition;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuButton;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuButtonType;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuSpecialButton;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuSpecialButtonLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuFieldLayout;
import org.secuso.privacyfriendlysudoku.ui.view.HumanHintDialog;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Real board/keypad input and Android saved-state recreation, in the isolated uiTest app. */
@RunWith(AndroidJUnit4.class)
public class GymDrillActivityTest {
    private Context context;

    @Before
    public void resetTestStats() {
        context = ApplicationProvider.getApplicationContext();
        assertTrue("Device tests must not touch the installed app's data",
                context.getPackageName().endsWith(".uitest"));
        new TrainingStatsRepository(context).reset();
    }

    @Test
    public void cellSelectionAndBoardSurviveRecreation() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.LAST_DIGIT)) {
            AtomicReference<GameHint> answer = new AtomicReference<>();
            AtomicReference<int[]> board = new AtomicReference<>();
            scenario.onActivity(activity -> {
                answer.set(answer(activity));
                board.set(values(controller(activity)));
                tapCell(activity, answer.get().getRow(), answer.get().getCol());
            });
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> {
                assertArrayEquals(board.get(), values(controller(activity)));
                assertEquals(answer.get().getRow(), controller(activity).getSelectedRow());
                assertEquals(answer.get().getCol(), controller(activity).getSelectedCol());
                pressNumber(activity, answer.get().getValue());
            });
            assertStats(HumanTechnique.LAST_DIGIT, 1, 1, 0, 1);
        }
    }

    @Test
    public void numberSelectionSurvivesRecreationAndStillGradesBeforeWriting() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.LAST_DIGIT)) {
            AtomicReference<GameHint> answer = new AtomicReference<>();
            scenario.onActivity(activity -> {
                answer.set(answer(activity));
                pressNumber(activity, answer.get().getValue());
            });
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals(answer.get().getValue(), controller(activity).getSelectedValue());
                tapCell(activity, answer.get().getRow(), answer.get().getCol());
            });
            assertStats(HumanTechnique.LAST_DIGIT, 1, 1, 0, 1);
        }
    }

    @Test
    public void incorrectAttemptStaysIneligibleAfterRecreation() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.LAST_DIGIT)) {
            AtomicReference<GameHint> answer = new AtomicReference<>();
            scenario.onActivity(activity -> {
                answer.set(answer(activity));
                tapCell(activity, answer.get().getRow(), answer.get().getCol());
                pressNumber(activity, answer.get().getValue() % 9 + 1);
                assertEquals(0, controller(activity).getValue(
                        answer.get().getRow(), answer.get().getCol()));
            });
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> {
                pressNumber(activity, answer.get().getValue());
                assertFalse(hintButton(activity).isEnabled());
            });
            awaitReady(scenario);
            assertStats(HumanTechnique.LAST_DIGIT, 1, 0, 0, 0);
        }
    }

    @Test
    public void hintPageSurvivesRepeatedRecreationWithoutExtraReveals() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.NAKED_PAIR)) {
            scenario.onActivity(activity -> hintButton(activity).performClick());
            scenario.onActivity(activity -> hintWindow(activity)
                    .getButton(DialogInterface.BUTTON_POSITIVE).performClick());
            for(int iteration = 0; iteration < 2; iteration++) {
                scenario.recreate();
                awaitReady(scenario);
                scenario.onActivity(activity -> {
                    assertNotNull(controller(activity).getActiveHint());
                    assertEquals(0, controller(activity).getActiveHintFrame());
                });
                assertStats(HumanTechnique.NAKED_PAIR, 1, 0, 1, 0);
            }
            scenario.onActivity(activity -> hintWindow(activity)
                    .getButton(DialogInterface.BUTTON_NEUTRAL).performClick());
            awaitReady(scenario);
            scenario.onActivity(activity -> assertNull(controller(activity).getActiveHint()));
            assertStats(HumanTechnique.NAKED_PAIR, 1, 0, 1, 0);
        }
    }

    @Test
    public void dismissedHintDoesNotReopenAfterRecreation() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.LAST_DIGIT)) {
            scenario.onActivity(activity -> hintButton(activity).performClick());
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> {
                assertNotNull(controller(activity).getActiveHint());
                assertEquals(-1, controller(activity).getActiveHintFrame());
            });
            scenario.onActivity(activity -> hintWindow(activity).cancel());
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> assertNull(controller(activity).getActiveHint()));
            assertStats(HumanTechnique.LAST_DIGIT, 1, 0, 1, 0);
        }
    }

    @Test
    public void completedDrillIsNotCountedAgainAfterRecreation() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.LAST_DIGIT)) {
            scenario.onActivity(activity -> {
                GameHint answer = answer(activity);
                tapCell(activity, answer.getRow(), answer.getCol());
                pressNumber(activity, answer.getValue());
            });
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals(-1, controller(activity).getSelectedRow());
                assertEquals(0, controller(activity).getSelectedValue());
                GameHint answer = answer(activity);
                pressNumber(activity, answer.getValue());
                tapCell(activity, answer.getRow(), answer.getCol());
            });
            assertStats(HumanTechnique.LAST_DIGIT, 2, 2, 0, 2);
        }
    }

    @Test
    public void catalogHasNoModeSelector() {
        context.getSharedPreferences("gym", Context.MODE_PRIVATE).edit()
                .putString("mode", "FOCUSED").commit();
        try(ActivityScenario<GymActivity> scenario = ActivityScenario.launch(GymActivity.class)) {
            scenario.onActivity(activity -> assertFalse(containsModeSelector(
                    activity.findViewById(R.id.main_content))));
        }
    }

    @Test
    public void nakedPairShowsAllCandidatesAndUsesTheFullGameControlLayout() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.NAKED_PAIR)) {
            scenario.onActivity(activity -> {
                TrainingPosition position = (TrainingPosition) privateField(
                        privateField(activity, "current"), "position");
                int[] expectedMasks = position.getCandidateMasks();
                for(int cell = 0; cell < 81; cell++) {
                    boolean[] notes = controller(activity).getGameCell(cell / 9, cell % 9).getNotes();
                    for(int digit = 0; digit < 9; digit++) {
                        assertEquals((expectedMasks[cell] & (1 << digit)) != 0, notes[digit]);
                    }
                }
                assertTrue(controller(activity).getNoteStatus());
                assertTrue(specialButton(activity, SudokuButtonType.NoteToggle).isSelected());
                SudokuSpecialButtonLayout controls = activity.findViewById(R.id.sudokuSpecialLayout);
                assertEquals(SudokuButtonType.getSpecialButtons().size(), controls.getChildCount());
                for(SudokuButtonType type : SudokuButtonType.getSpecialButtons()) {
                    assertEquals(type == SudokuButtonType.Hint || type == SudokuButtonType.NoteToggle
                                    ? View.VISIBLE : View.INVISIBLE,
                            controls.getButton(type).getVisibility());
                }
                int[] board = location(activity.findViewById(R.id.sudokuLayout));
                int[] keys = location(activity.findViewById(R.id.sudokuKeyboardLayout));
                int[] special = location(controls);
                if(activity.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE) {
                    assertTrue(special[0] + controls.getWidth() <= board[0]);
                    assertTrue(board[0] < keys[0]);
                } else {
                    assertTrue(board[1] < keys[1]);
                    assertTrue(keys[1] < special[1]);
                }
            });
        }
    }

    @Test
    public void nakedPairCandidateRemovalWorksWithBothInputOrders() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.NAKED_PAIR)) {
            for(boolean numberFirst : new boolean[] {false, true}) {
                scenario.onActivity(activity -> {
                    GameHint.Candidate target = answer(activity, HumanTechnique.NAKED_PAIR)
                            .getEliminations().get(0);
                    assertTrue(controller(activity).getNoteStatus());
                    if(numberFirst) {
                        pressNumber(activity, target.getValue());
                        tapCell(activity, target.getRow(), target.getCol());
                    } else {
                        tapCell(activity, target.getRow(), target.getCol());
                        pressNumber(activity, target.getValue());
                    }
                    assertFalse(controller(activity).getGameCell(target.getRow(), target.getCol())
                            .getNotes()[target.getValue() - 1]);
                    assertEquals(0, controller(activity).getValue(target.getRow(), target.getCol()));
                    assertFalse(hintButton(activity).isEnabled());
                });
                awaitReady(scenario);
            }
            assertStats(HumanTechnique.NAKED_PAIR, 2, 2, 0, 2);
        }
    }

    @Test
    public void wrongInputModeIsRejectedAndPencilStateSurvivesRecreation() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.NAKED_PAIR)) {
            scenario.onActivity(activity ->
                    specialButton(activity, SudokuButtonType.NoteToggle).performClick());
            scenario.recreate();
            awaitReady(scenario);
            scenario.onActivity(activity -> {
                assertFalse(controller(activity).getNoteStatus());
                assertFalse(specialButton(activity, SudokuButtonType.NoteToggle).isSelected());
                GameHint.Candidate target = answer(activity, HumanTechnique.NAKED_PAIR)
                        .getEliminations().get(0);
                tapCell(activity, target.getRow(), target.getCol());
                pressNumber(activity, target.getValue());
                assertEquals(0, controller(activity).getValue(target.getRow(), target.getCol()));
                assertTrue(controller(activity).getGameCell(target.getRow(), target.getCol())
                        .getNotes()[target.getValue() - 1]);
                assertTrue(hintButton(activity).isEnabled());
                specialButton(activity, SudokuButtonType.NoteToggle).performClick();
                pressNumber(activity, target.getValue());
                assertFalse(hintButton(activity).isEnabled());
            });
            awaitReady(scenario);
            scenario.onActivity(activity -> assertTrue(controller(activity).getNoteStatus()));
            assertStats(HumanTechnique.NAKED_PAIR, 1, 0, 0, 0);
        }
    }

    @Test
    public void placementStartsInEntryModeAndRejectsNotes() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.LAST_DIGIT)) {
            scenario.onActivity(activity -> {
                assertFalse(controller(activity).getNoteStatus());
                GameHint target = answer(activity);
                specialButton(activity, SudokuButtonType.NoteToggle).performClick();
                tapCell(activity, target.getRow(), target.getCol());
                pressNumber(activity, target.getValue());
                assertTrue(hintButton(activity).isEnabled());
                assertEquals(0, controller(activity).getValue(target.getRow(), target.getCol()));
                assertTrue(controller(activity).getGameCell(target.getRow(), target.getCol())
                        .getNotes()[target.getValue() - 1]);
                specialButton(activity, SudokuButtonType.NoteToggle).performClick();
                pressNumber(activity, target.getValue());
                assertEquals(target.getValue(), controller(activity)
                        .getValue(target.getRow(), target.getCol()));
            });
            assertStats(HumanTechnique.LAST_DIGIT, 1, 0, 0, 0);
        }
    }

    private static boolean containsModeSelector(View view) {
        if(view instanceof RadioGroup) return true;
        if(view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for(int index = 0; index < group.getChildCount(); index++) {
                if(containsModeSelector(group.getChildAt(index))) return true;
            }
        }
        return false;
    }

    private static int[] location(View view) {
        int[] result = new int[2];
        view.getLocationInWindow(result);
        return result;
    }

    private ActivityScenario<GymDrillActivity> launch(HumanTechnique technique) {
        Intent intent = new Intent(context, GymDrillActivity.class)
                .putExtra(GymDrillActivity.EXTRA_TECHNIQUE, technique.name());
        ActivityScenario<GymDrillActivity> scenario = ActivityScenario.launch(intent);
        awaitReady(scenario);
        return scenario;
    }

    private static void awaitReady(ActivityScenario<GymDrillActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> {
                SudokuFieldLayout board = activity.findViewById(R.id.sudokuLayout);
                ready.set(hintButton(activity) != null && hintButton(activity).isEnabled()
                        && board.gamecells != null && board.gamecells[0][0].getWidth() > 0);
            });
            if(ready.get()) return;
            SystemClock.sleep(20);
        } while(SystemClock.uptimeMillis() < deadline);
        fail("Gym did not finish preparing its drill");
    }

    private void assertStats(HumanTechnique technique, int attempts, int correct,
                             int reveals, int streak) {
        TrainingStats stats = new TrainingStatsRepository(context).get(technique);
        assertEquals(attempts, stats.getAttempts());
        assertEquals(correct, stats.getCorrect());
        assertEquals(reveals, stats.getReveals());
        assertEquals(streak, stats.getCurrentStreak());
    }

    private static SudokuSpecialButton hintButton(GymDrillActivity activity) {
        return specialButton(activity, SudokuButtonType.Hint);
    }

    private static SudokuSpecialButton specialButton(GymDrillActivity activity, SudokuButtonType type) {
        SudokuSpecialButtonLayout controls = activity.findViewById(R.id.sudokuSpecialLayout);
        return controls.getButton(type);
    }

    private static GameController controller(GymDrillActivity activity) {
        return (GameController) privateField(activity, "gameController");
    }

    private static AlertDialog hintWindow(GymDrillActivity activity) {
        HumanHintDialog hint = (HumanHintDialog) privateField(activity, "hintDialog");
        assertNotNull("Gym should own its open hint", hint);
        assertTrue(hint.isShowing());
        return (AlertDialog) privateField(hint, "dialog");
    }

    private static Object privateField(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch(ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static int[] values(GameController controller) {
        int[] values = new int[81];
        for(int cell = 0; cell < values.length; cell++) {
            values[cell] = controller.getValue(cell / 9, cell % 9);
        }
        return values;
    }

    private static GameHint answer(GymDrillActivity activity) {
        return answer(activity, HumanTechnique.LAST_DIGIT);
    }

    private static GameHint answer(GymDrillActivity activity, HumanTechnique technique) {
        GameController controller = controller(activity);
        int[] masks = new int[81];
        for(int cell = 0; cell < masks.length; cell++) {
            boolean[] notes = controller.getGameCell(cell / 9, cell % 9).getNotes();
            for(int value = 0; value < 9; value++) {
                if(notes[value]) masks[cell] |= 1 << value;
            }
        }
        GameHint hint = HumanHintEngine.findTechnique(GameType.Default_9x9, values(controller),
                masks, controller.solve(), Symbol.Default, technique);
        assertNotNull(hint);
        return hint;
    }

    private static void tapCell(GymDrillActivity activity, int row, int col) {
        SudokuFieldLayout board = activity.findViewById(R.id.sudokuLayout);
        View cell = board.gamecells[row][col];
        long now = SystemClock.uptimeMillis();
        for(int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
            MotionEvent event = MotionEvent.obtain(now, now, action,
                    cell.getLeft() + cell.getWidth() / 2f,
                    cell.getTop() + cell.getHeight() / 2f, 0);
            board.dispatchTouchEvent(event);
            event.recycle();
        }
    }

    private static void pressNumber(GymDrillActivity activity, int value) {
        SudokuButton button = findNumber(activity.findViewById(R.id.sudokuKeyboardLayout), value);
        assertNotNull(button);
        assertTrue(button.isEnabled());
        button.performClick();
    }

    private static SudokuButton findNumber(View view, int value) {
        if(view instanceof SudokuButton && ((SudokuButton) view).getValue() == value) {
            return (SudokuButton) view;
        }
        if(view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for(int index = 0; index < group.getChildCount(); index++) {
                SudokuButton button = findNumber(group.getChildAt(index), value);
                if(button != null) return button;
            }
        }
        return null;
    }
}
