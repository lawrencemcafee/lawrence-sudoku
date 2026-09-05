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
import java.util.Map;

/** Versioned, atomically persisted Gym statistics in the already-backed-up stats directory. */
public final class TrainingStatsRepository {
    private static final String TAG = "GymStats";
    private static final int MAGIC = 0x4c475953; // LGYS
    private static final int VERSION = 1;
    private static final String FILE_NAME = "gym_stats_v1.bin";

    private final AtomicFile file;
    private final Map<HumanTechnique, EnumMap<TrainingMode, TrainingStats>> records =
            new EnumMap<>(HumanTechnique.class);

    public TrainingStatsRepository(Context context) {
        file = new AtomicFile(new File(context.getDir("stats", 0), FILE_NAME));
        initializeEmpty();
        load();
    }

    public synchronized TrainingStats get(HumanTechnique technique, TrainingMode mode) {
        TrainingStats stats = records.get(technique).get(mode);
        return new TrainingStats(stats.getAttempts(), stats.getCorrect(), stats.getReveals(),
                stats.getCurrentStreak(), stats.getBestStreak());
    }

    public synchronized void recordAttempt(HumanTechnique technique, TrainingMode mode) {
        mutable(technique, mode).recordAttempt();
        save();
    }

    public synchronized void recordCorrect(HumanTechnique technique, TrainingMode mode) {
        mutable(technique, mode).recordCorrect();
        save();
    }

    public synchronized void recordFailure(HumanTechnique technique, TrainingMode mode) {
        mutable(technique, mode).recordFailure();
        save();
    }

    public synchronized void recordReveal(HumanTechnique technique, TrainingMode mode) {
        mutable(technique, mode).recordReveal();
        save();
    }

    public synchronized void reset() {
        initializeEmpty();
        file.delete();
    }

    private TrainingStats mutable(HumanTechnique technique, TrainingMode mode) {
        return records.get(technique).get(mode);
    }

    private void initializeEmpty() {
        records.clear();
        for(HumanTechnique technique : HumanTechnique.values()) {
            EnumMap<TrainingMode, TrainingStats> modes = new EnumMap<>(TrainingMode.class);
            for(TrainingMode mode : TrainingMode.values()) modes.put(mode, new TrainingStats());
            records.put(technique, modes);
        }
    }

    private void load() {
        if(!file.getBaseFile().isFile()) return;
        try(DataInputStream input = new DataInputStream(new BufferedInputStream(
                file.openRead()))) {
            if(input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported Gym statistics file.");
            }
            int count = input.readInt();
            if(count != HumanTechnique.values().length * TrainingMode.values().length) {
                throw new IOException("Unexpected Gym statistics record count.");
            }
            for(int index = 0; index < count; index++) {
                HumanTechnique technique = HumanTechnique.valueOf(input.readUTF());
                TrainingMode mode = TrainingMode.valueOf(input.readUTF());
                int attempts = nonnegative(input.readInt());
                int correct = nonnegative(input.readInt());
                int reveals = nonnegative(input.readInt());
                int current = nonnegative(input.readInt());
                int best = nonnegative(input.readInt());
                if(correct > attempts || current > best) {
                    throw new IOException("Inconsistent Gym statistics.");
                }
                records.get(technique).put(mode,
                        new TrainingStats(attempts, correct, reveals, current, best));
            }
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
            output.writeInt(HumanTechnique.values().length * TrainingMode.values().length);
            for(HumanTechnique technique : HumanTechnique.values()) {
                for(TrainingMode mode : TrainingMode.values()) {
                    TrainingStats stats = mutable(technique, mode);
                    output.writeUTF(technique.name());
                    output.writeUTF(mode.name());
                    output.writeInt(stats.getAttempts());
                    output.writeInt(stats.getCorrect());
                    output.writeInt(stats.getReveals());
                    output.writeInt(stats.getCurrentStreak());
                    output.writeInt(stats.getBestStreak());
                }
            }
            output.flush();
            file.finishWrite(stream);
        } catch(IOException failure) {
            Log.e(TAG, "Could not write Gym statistics.", failure);
            if(stream != null) file.failWrite(stream);
        }
    }
}
