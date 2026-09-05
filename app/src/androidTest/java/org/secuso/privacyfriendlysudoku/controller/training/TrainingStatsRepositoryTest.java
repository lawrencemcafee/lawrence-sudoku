/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TrainingStatsRepositoryTest {
    private Context context;
    private File statsFile;

    @Before
    public void prepareIsolatedStorage() {
        context = ApplicationProvider.getApplicationContext();
        assertTrue(context.getPackageName().endsWith(".uitest"));
        new TrainingStatsRepository(context).reset();
        statsFile = new File(context.getDir("stats", 0), "gym_stats_v1.bin");
    }

    @Test
    public void migrationKeepsOnlyFullHistoryAndRunsOnce() throws Exception {
        writeLegacyStats(false);
        TrainingStatsRepository repository = new TrainingStatsRepository(context);
        for(HumanTechnique technique : HumanTechnique.values()) {
            assertFullRecord(repository.get(technique));
        }
        try(DataInputStream input = new DataInputStream(new FileInputStream(statsFile))) {
            assertEquals(0x4c475953, input.readInt());
            assertEquals(2, input.readInt());
            assertEquals(HumanTechnique.values().length, input.readInt());
        }
        repository = new TrainingStatsRepository(context);
        assertFullRecord(repository.get(HumanTechnique.NAKED_PAIR));
        repository.recordAttempt(HumanTechnique.NAKED_PAIR);
        repository.recordCorrect(HumanTechnique.NAKED_PAIR);
        TrainingStats result = new TrainingStatsRepository(context).get(HumanTechnique.NAKED_PAIR);
        assertEquals(6, result.getAttempts());
        assertEquals(4, result.getCorrect());
        assertEquals(2, result.getReveals());
        assertEquals(3, result.getCurrentStreak());
        assertEquals(3, result.getBestStreak());
    }

    @Test
    public void focusedHistoryDoesNotFillAnEmptyFullHistory() throws Exception {
        writeLegacyStats(true);
        TrainingStatsRepository repository = new TrainingStatsRepository(context);
        for(HumanTechnique technique : HumanTechnique.values()) {
            assertEquals(0, repository.get(technique).getAttempts());
            assertEquals(0, repository.get(technique).getBestStreak());
        }
        repository.reset();
        assertEquals(0, new TrainingStatsRepository(context)
                .get(HumanTechnique.NAKED_PAIR).getAttempts());
    }

    private void writeLegacyStats(boolean emptyFull) throws Exception {
        try(DataOutputStream output = new DataOutputStream(new FileOutputStream(statsFile))) {
            output.writeInt(0x4c475953);
            output.writeInt(1);
            output.writeInt(HumanTechnique.values().length * 2);
            for(HumanTechnique technique : HumanTechnique.values()) {
                // Focused comes last to catch accidental overwriting of the retained Full record.
                for(String mode : new String[] {"FULL", "FOCUSED"}) {
                    output.writeUTF(technique.name());
                    output.writeUTF(mode);
                    int[] values = mode.equals("FOCUSED") ? new int[] {40, 30, 10, 5, 20}
                            : emptyFull ? new int[5] : new int[] {5, 3, 2, 2, 3};
                    for(int value : values) output.writeInt(value);
                }
            }
        }
    }

    private static void assertFullRecord(TrainingStats stats) {
        assertEquals(5, stats.getAttempts());
        assertEquals(3, stats.getCorrect());
        assertEquals(2, stats.getReveals());
        assertEquals(2, stats.getCurrentStreak());
        assertEquals(3, stats.getBestStreak());
    }
}
