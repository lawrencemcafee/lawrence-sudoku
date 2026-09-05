/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import org.secuso.privacyfriendlysudoku.controller.hints.HumanTechnique;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Versioned, atomically persisted Gym statistics in the already-backed-up stats directory. */
public final class TrainingStatsRepository {
    private static final String TAG = "GymStats";
    private static final int MAGIC = 0x4c475953; // LGYS
    private static final int VERSION = 2;
    // Keep the installed file location; the header controls the format version.
    private static final String FILE_NAME = "gym_stats_v1.bin";

    private final AtomicFile file;
    private final Map<HumanTechnique, TrainingStats> records =
            new EnumMap<>(HumanTechnique.class);

    public TrainingStatsRepository(Context context) {
        file = new AtomicFile(new File(context.getDir("stats", 0), FILE_NAME));
        initializeEmpty();
        load();
    }

    public synchronized TrainingStats get(HumanTechnique technique) {
        TrainingStats stats = records.get(technique);
        return new TrainingStats(stats.getAttempts(), stats.getCorrect(), stats.getReveals(),
                stats.getCurrentStreak(), stats.getBestStreak());
    }

    public synchronized void recordAttempt(HumanTechnique technique) {
        records.get(technique).recordAttempt();
        save();
    }

    public synchronized void recordCorrect(HumanTechnique technique) {
        records.get(technique).recordCorrect();
        save();
    }

    public synchronized void recordFailure(HumanTechnique technique) {
        records.get(technique).recordFailure();
        save();
    }

    public synchronized void recordReveal(HumanTechnique technique) {
        records.get(technique).recordReveal();
        save();
    }

    public synchronized void reset() {
        initializeEmpty();
        file.delete();
    }

    private void initializeEmpty() {
        records.clear();
        for(HumanTechnique technique : HumanTechnique.values()) {
            records.put(technique, new TrainingStats());
        }
    }

    private void load() {
        if(!file.getBaseFile().isFile()) return;
        try(DataInputStream input = new DataInputStream(new BufferedInputStream(
                file.openRead()))) {
            if(input.readInt() != MAGIC) {
                throw new IOException("Unsupported Gym statistics file.");
            }
            int version = input.readInt();
            if(version != 1 && version != VERSION) {
                throw new IOException("Unsupported Gym statistics version: " + version);
            }
            int count = input.readInt();
            if(count != HumanTechnique.values().length * (version == 1 ? 2 : 1)) {
                throw new IOException("Unexpected Gym statistics record count.");
            }
            Set<String> seen = new HashSet<>();
            for(int index = 0; index < count; index++) {
                HumanTechnique technique = HumanTechnique.valueOf(input.readUTF());
                String legacyMode = version == 1 ? input.readUTF() : "FULL";
                if(!legacyMode.equals("FULL") && !legacyMode.equals("FOCUSED")) {
                    throw new IOException("Unrecognized legacy Gym mode.");
                }
                if(!seen.add(technique.name() + ":" + legacyMode)) {
                    throw new IOException("Duplicate Gym statistics record.");
                }
                int attempts = nonnegative(input.readInt());
                int correct = nonnegative(input.readInt());
                int reveals = nonnegative(input.readInt());
                int current = nonnegative(input.readInt());
                int best = nonnegative(input.readInt());
                if(correct > attempts || current > best) {
                    throw new IOException("Inconsistent Gym statistics.");
                }
                // User chose to retain Full history only, including its exact streaks.
                if(legacyMode.equals("FULL")) {
                    records.put(technique,
                            new TrainingStats(attempts, correct, reveals, current, best));
                }
            }
            if(input.read() != -1) throw new IOException("Trailing Gym statistics data.");
            if(version == 1) save();
        } catch(IOException | IllegalArgumentException failure) {
            Log.e(TAG, "Could not read Gym statistics; starting with empty statistics.", failure);
            initializeEmpty();
        }
    }

    private int nonnegative(int value) throws IOException {
        if(value < 0) throw new IOException("Negative Gym statistic.");
        return value;
    }

    private void save() {
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(stream));
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(HumanTechnique.values().length);
            for(HumanTechnique technique : HumanTechnique.values()) {
                TrainingStats stats = records.get(technique);
                output.writeUTF(technique.name());
                output.writeInt(stats.getAttempts());
                output.writeInt(stats.getCorrect());
                output.writeInt(stats.getReveals());
                output.writeInt(stats.getCurrentStreak());
                output.writeInt(stats.getBestStreak());
            }
            output.flush();
            file.finishWrite(stream);
        } catch(IOException failure) {
            Log.e(TAG, "Could not write Gym statistics.", failure);
            if(stream != null) file.failWrite(stream);
        }
    }
}
