/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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
import org.secuso.privacyfriendlysudoku.controller.training.TrainingMode;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStats;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuButton;
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
                assertFalse(activity.findViewById(R.id.gymHintButton).isEnabled());
            });
            awaitReady(scenario);
            assertStats(HumanTechnique.LAST_DIGIT, 1, 0, 0, 0);
        }
    }

    @Test
    public void hintPageSurvivesRepeatedRecreationWithoutExtraReveals() {
        try(ActivityScenario<GymDrillActivity> scenario = launch(HumanTechnique.NAKED_PAIR)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.gymHintButton).performClick());
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
            scenario.onActivity(activity -> activity.findViewById(R.id.gymHintButton).performClick());
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
            TrainingStats otherMode = new TrainingStatsRepository(context)
                    .get(HumanTechnique.LAST_DIGIT, TrainingMode.FOCUSED);
            assertEquals(0, otherMode.getAttempts());
        }
    }

    private ActivityScenario<GymDrillActivity> launch(HumanTechnique technique) {
        Intent intent = new Intent(context, GymDrillActivity.class)
                .putExtra(GymDrillActivity.EXTRA_TECHNIQUE, technique.name())
                .putExtra(GymDrillActivity.EXTRA_MODE, TrainingMode.FULL.name());
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
                ready.set(activity.findViewById(R.id.gymHintButton).isEnabled()
                        && board.gamecells != null && board.gamecells[0][0].getWidth() > 0);
            });
            if(ready.get()) return;
            SystemClock.sleep(20);
        } while(SystemClock.uptimeMillis() < deadline);
        fail("Gym did not finish preparing its drill");
    }

    private void assertStats(HumanTechnique technique, int attempts, int correct,
                             int reveals, int streak) {
        TrainingStats stats = new TrainingStatsRepository(context).get(technique, TrainingMode.FULL);
        assertEquals(attempts, stats.getAttempts());
        assertEquals(correct, stats.getCorrect());
        assertEquals(reveals, stats.getReveals());
        assertEquals(streak, stats.getCurrentStreak());
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
        GameController controller = controller(activity);
        int[] masks = new int[81];
        for(int cell = 0; cell < masks.length; cell++) {
            boolean[] notes = controller.getGameCell(cell / 9, cell % 9).getNotes();
            for(int value = 0; value < 9; value++) {
                if(notes[value]) masks[cell] |= 1 << value;
            }
        }
        GameHint hint = HumanHintEngine.findTechnique(GameType.Default_9x9, values(controller),
                masks, controller.solve(), Symbol.Default, HumanTechnique.LAST_DIGIT);
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
