/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic, rotation-safe ordering with no canonical repeat inside one corpus cycle. */
public final class TrainingSession {
    private final List<TrainingPosition> positions;
    private final long seed;

    public TrainingSession(List<TrainingPosition> positions, long seed) {
        if(positions == null || positions.size() != TrainingCorpusCodec.POSITIONS_PER_TECHNIQUE) {
            throw new IllegalArgumentException("A Gym session requires exactly 100 positions.");
        }
        this.positions = new ArrayList<>(positions);
        this.seed = seed;
    }

    public TrainingPosition get(long sequence) {
        if(sequence < 0) throw new IllegalArgumentException("Sequence may not be negative.");
        int count = positions.size();
        long cycle = sequence / count;
        int offset = (int) (sequence % count);
        List<Integer> order = orderFor(cycle);
        TrainingPosition source = positions.get(order.get(offset));
        return TrainingTransformer.prepare(source, mix(seed, sequence + 0x6a09e667f3bcc909L));
    }

    private List<Integer> orderFor(long cycle) {
        List<Integer> result = new ArrayList<>();
        for(int index = 0; index < positions.size(); index++) result.add(index);
        Collections.shuffle(result, new Random(mix(seed, cycle)));
        if(cycle > 0) {
            List<Integer> previous = orderForWithoutBoundary(cycle - 1);
            int previousLast = previous.get(previous.size() - 1);
            if(result.get(0) == previousLast) Collections.swap(result, 0, 1);
        }
        return result;
    }

    private List<Integer> orderForWithoutBoundary(long cycle) {
        List<Integer> result = new ArrayList<>();
        for(int index = 0; index < positions.size(); index++) result.add(index);
        Collections.shuffle(result, new Random(mix(seed, cycle)));
        return result;
    }

    private static long mix(long left, long right) {
        long value = left ^ (right + 0x9e3779b97f4a7c15L + (left << 6) + (left >>> 2));
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
