/* Licensed under the GNU General Public License, version 3 or later. */
package org.secuso.privacyfriendlysudoku.ui;

import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.secuso.privacyfriendlysudoku.R;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;
import org.secuso.privacyfriendlysudoku.controller.training.TechniqueLesson;
import org.secuso.privacyfriendlysudoku.controller.training.TrainingStatsRepository;
import org.secuso.privacyfriendlysudoku.ui.view.TechniqueDiagramView;
import org.secuso.privacyfriendlysudoku.ui.view.TechniqueHelpDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GymActivityTest {
    private Context context;
    private int originalNightMode;
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    @Before
    public void checkIsolatedApp() {
        context = ApplicationProvider.getApplicationContext();
        assertTrue(context.getPackageName().endsWith(".uitest"));
        originalNightMode = AppCompatDelegate.getDefaultNightMode();
        String theme = InstrumentationRegistry.getArguments().getString("lessonTheme", "system");
        if(!theme.equals("system")) instrumentation.runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(
                theme.equals("dark") ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));
    }

    @After
    public void restoreTheme() {
        instrumentation.runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(originalNightMode));
    }

    @Test
    public void everyHelpButtonOpensItsLessonWithoutStartingAQuizOrChangingHistory() throws Exception {
        TrainingStatsRepository stats = new TrainingStatsRepository(context);
        stats.recordAttempt(HumanTechnique.LAST_DIGIT);
        stats.recordCorrect(HumanTechnique.LAST_DIGIT);
        Map<String, ?> preferences = context.getSharedPreferences("gym", Context.MODE_PRIVATE).getAll();
        int attempts = stats.get(HumanTechnique.LAST_DIGIT).getAttempts();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(GymDrillActivity.class.getName(), null, false);
        try(ActivityScenario<GymActivity> scenario = ActivityScenario.launch(GymActivity.class)) {
            for(HumanTechnique technique : HumanTechnique.values()) {
                select(scenario, technique);
                scenario.onActivity(activity -> {
                    View row = row(activity, technique);
                    View help = row.findViewById(R.id.gymSkillHelp);
                    assertEquals(activity.getString(R.string.gym_skill_help_description, technique.getTitle()),
                            help.getContentDescription());
                    assertTrue(help.getWidth() >= 48 * activity.getResources().getDisplayMetrics().density - 1);
                });
                tap(scenario, technique, R.id.gymSkillHelp);
                instrumentation.waitForIdleSync();
                scenario.onActivity(activity -> {
                    AlertDialog dialog = dialog(activity);
                    assertTrue(dialog.isShowing());
                    TechniqueLesson lesson = TechniqueLesson.forTechnique(technique);
                    assertEquals(activity.getString(lesson.caption),
                            ((TextView) dialog.findViewById(R.id.gymHelpCaption)).getText().toString());
                    TechniqueDiagramView diagram = dialog.findViewById(R.id.gymHelpDiagram);
                    assertTrue(diagram.getWidth() > 0 && diagram.getHeight() > 0);
                    assertEquals(activity.getString(lesson.caption), diagram.getContentDescription());
                    LinearLayout steps = dialog.findViewById(R.id.gymHelpSteps);
                    assertEquals(activity.getResources().getStringArray(lesson.steps).length, steps.getChildCount());
                    assertTrue("Bullets must follow the illustration", steps.getTop() > diagram.getBottom());
                    dialog.getWindow().getDecorView().invalidate();
                });
                instrumentation.waitForIdleSync();
                capture(technique.name());
                scenario.onActivity(activity -> dialog(activity).getButton(DialogInterface.BUTTON_POSITIVE).performClick());
                instrumentation.waitForIdleSync();
            }
            assertEquals(0, monitor.getHits());
            assertEquals(preferences, context.getSharedPreferences("gym", Context.MODE_PRIVATE).getAll());
            assertEquals(attempts, new TrainingStatsRepository(context).get(HumanTechnique.LAST_DIGIT).getAttempts());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void openLessonSurvivesRecreationAndBackReturnsToTheCatalog() {
        try(ActivityScenario<GymActivity> scenario = ActivityScenario.launch(GymActivity.class)) {
            select(scenario, HumanTechnique.XYZ_WING);
            scenario.onActivity(activity -> row(activity, HumanTechnique.XYZ_WING).findViewById(R.id.gymSkillHelp).performClick());
            instrumentation.waitForIdleSync();
            scenario.recreate();
            scenario.onActivity(activity -> {
                AlertDialog dialog = dialog(activity);
                assertTrue(dialog.isShowing());
                assertEquals(activity.getString(R.string.lesson_xyz_wing_example),
                        ((TextView) dialog.findViewById(R.id.gymHelpCaption)).getText().toString());
                dialog.onBackPressed();
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> assertNull(activity.getSupportFragmentManager()
                    .findFragmentByTag(TechniqueHelpDialog.TAG)));
        }
    }

    @Test
    public void landscapeLessonScrollsToTheLastBulletWithCloseAlwaysVisible() throws Exception {
        try(ActivityScenario<GymActivity> scenario = ActivityScenario.launch(GymActivity.class)) {
            scenario.onActivity(activity -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            long deadline = SystemClock.uptimeMillis() + 5000;
            AtomicBoolean landscape = new AtomicBoolean();
            do {
                instrumentation.waitForIdleSync();
                scenario.onActivity(activity -> landscape.set(activity.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE));
                if(!landscape.get()) SystemClock.sleep(20);
            } while(!landscape.get() && SystemClock.uptimeMillis() < deadline);
            assertTrue(landscape.get());
            select(scenario, HumanTechnique.FORCING_CHAIN);
            tap(scenario, HumanTechnique.FORCING_CHAIN, R.id.gymSkillHelp);
            scenario.onActivity(activity -> {
                AlertDialog dialog = dialog(activity);
                assertTrue(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getGlobalVisibleRect(new Rect()));
                ScrollView scroll = dialog.findViewById(R.id.gymHelpScroll);
                scroll.setSmoothScrollingEnabled(false);
                scroll.fullScroll(View.FOCUS_DOWN);
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                LinearLayout steps = dialog(activity).findViewById(R.id.gymHelpSteps);
                View last = steps.getChildAt(steps.getChildCount() - 1);
                Rect visible = new Rect();
                assertTrue(last.getGlobalVisibleRect(visible));
                assertEquals("The last bullet must be fully reachable", last.getHeight(), visible.height());
            });
            capture("LANDSCAPE_STEPS");
            scenario.onActivity(activity -> {
                AlertDialog dialog = dialog(activity);
                ((ScrollView) dialog.findViewById(R.id.gymHelpScroll)).fullScroll(View.FOCUS_UP);
            });
            instrumentation.waitForIdleSync();
            capture("LANDSCAPE_DIAGRAM");
            scenario.onActivity(activity -> dialog(activity).getButton(DialogInterface.BUTTON_POSITIVE).performClick());
        }
    }

    @Test
    public void tappingTheSkillTextStillStartsTheSelectedQuiz() {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(GymDrillActivity.class.getName(), null, false);
        try(ActivityScenario<GymActivity> scenario = ActivityScenario.launch(GymActivity.class)) {
            select(scenario, HumanTechnique.NAKED_SINGLE);
            tap(scenario, HumanTechnique.NAKED_SINGLE, R.id.gymSkillTitle);
            GymDrillActivity drill = (GymDrillActivity) instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            assertNotNull("The row must still launch a quiz", drill);
            assertEquals(HumanTechnique.NAKED_SINGLE.name(), drill.getIntent().getStringExtra(GymDrillActivity.EXTRA_TECHNIQUE));
            instrumentation.runOnMainSync(drill::finish);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    private void select(ActivityScenario<GymActivity> scenario, HumanTechnique technique) {
        scenario.onActivity(activity -> ((ListView) activity.findViewById(R.id.gymSkillList)).setSelection(position(technique)));
        instrumentation.waitForIdleSync();
    }

    private static int position(HumanTechnique requested) {
        int position = 0, level = -1;
        for(HumanTechnique technique : HumanTechnique.values()) {
            if(technique.getBaseLevel() != level) { position++; level = technique.getBaseLevel(); }
            if(technique == requested) return position;
            position++;
        }
        throw new AssertionError(requested);
    }

    private static View row(GymActivity activity, HumanTechnique technique) {
        ListView list = activity.findViewById(R.id.gymSkillList);
        View row = list.getChildAt(position(technique) - list.getFirstVisiblePosition());
        assertNotNull(row);
        return row;
    }

    private static AlertDialog dialog(GymActivity activity) {
        TechniqueHelpDialog fragment = (TechniqueHelpDialog) activity.getSupportFragmentManager()
                .findFragmentByTag(TechniqueHelpDialog.TAG);
        assertNotNull(fragment);
        return (AlertDialog) fragment.requireDialog();
    }

    private void tap(ActivityScenario<GymActivity> scenario, HumanTechnique technique, int targetId) {
        AtomicReference<float[]> point = new AtomicReference<>();
        scenario.onActivity(activity -> {
            View target = row(activity, technique).findViewById(targetId);
            int[] position = new int[2];
            target.getLocationOnScreen(position);
            point.set(new float[]{position[0] + target.getWidth() / 2f,
                    position[1] + target.getHeight() / 2f});
        });
        long now = SystemClock.uptimeMillis();
        for(int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
            MotionEvent event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action,
                    point.get()[0], point.get()[1], 0);
            instrumentation.sendPointerSync(event);
            event.recycle();
        }
        instrumentation.waitForIdleSync();
    }

    /** Optional visual evidence during the same interaction check, never normal app data. */
    private void capture(String name) throws Exception {
        if(!InstrumentationRegistry.getArguments().getString("captureLessons", "false").equals("true")) return;
        SystemClock.sleep(200); // Let the dialog's entrance animation finish before taking evidence.
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File folder = new File(context.getExternalFilesDir(null), "lesson-captures");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try(FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
    }
}
