/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Applies Sudoku-preserving house, digit, and transpose transformations. */
public final class TrainingTransformer {
    private TrainingTransformer() {}

    public static TrainingPosition transform(TrainingPosition source, long seed) {
        Random random = new Random(seed);
        int[] rowMap = houseMap(random);
        int[] colMap = houseMap(random);
        int[] digitMap = digitMap(random);
        boolean transpose = random.nextBoolean();
        int[] values = new int[TrainingPosition.CELL_COUNT];
        int[] masks = new int[TrainingPosition.CELL_COUNT];
        int[] solution = new int[TrainingPosition.CELL_COUNT];

        int[] oldValues = source.getValues();
        int[] oldMasks = source.getCandidateMasks();
        int[] oldSolution = source.getSolution();
        for(int oldRow = 0; oldRow < TrainingPosition.SIZE; oldRow++) {
            for(int oldCol = 0; oldCol < TrainingPosition.SIZE; oldCol++) {
                int newRow = transpose ? colMap[oldCol] : rowMap[oldRow];
                int newCol = transpose ? rowMap[oldRow] : colMap[oldCol];
                int oldIndex = oldRow * TrainingPosition.SIZE + oldCol;
                int newIndex = newRow * TrainingPosition.SIZE + newCol;
                values[newIndex] = remapValue(oldValues[oldIndex], digitMap);
                solution[newIndex] = remapValue(oldSolution[oldIndex], digitMap);
                masks[newIndex] = remapMask(oldMasks[oldIndex], digitMap);
            }
        }

        List<TrainingTarget> targets = new ArrayList<>();
        for(TrainingTarget target : source.getTargets()) {
            int row = transpose ? colMap[target.getCol()] : rowMap[target.getRow()];
            int col = transpose ? rowMap[target.getRow()] : colMap[target.getCol()];
            targets.add(new TrainingTarget(row, col, digitMap[target.getValue() - 1]));
        }
        return new TrainingPosition(source.getId(), source.getTechnique(), values, masks, solution,
                source.getAction(), targets);
    }

    /** Transform every valid target along with the candidate-state snapshot. */
    public static TrainingPosition prepare(TrainingPosition source, long seed) {
        return transform(source, seed);
    }

    private static int[] houseMap(Random random) {
        List<Integer> bands = shuffled(3, random);
        int[] result = new int[TrainingPosition.SIZE];
        int destination = 0;
        for(int sourceBand : bands) {
            List<Integer> members = shuffled(3, random);
            for(int member : members) result[sourceBand * 3 + member] = destination++;
        }
        return result;
    }

    private static int[] digitMap(Random random) {
        List<Integer> digits = shuffled(TrainingPosition.SIZE, random);
        int[] result = new int[TrainingPosition.SIZE];
        for(int old = 0; old < result.length; old++) result[old] = digits.get(old) + 1;
        return result;
    }

    private static List<Integer> shuffled(int count, Random random) {
        List<Integer> result = new ArrayList<>();
        for(int index = 0; index < count; index++) result.add(index);
        Collections.shuffle(result, random);
        return result;
    }

    private static int remapValue(int value, int[] digitMap) {
        return value == 0 ? 0 : digitMap[value - 1];
    }

    private static int remapMask(int mask, int[] digitMap) {
        int result = 0;
        for(int value = 1; value <= TrainingPosition.SIZE; value++) {
            if((mask & (1 << (value - 1))) != 0) result |= 1 << (digitMap[value - 1] - 1);
        }
        return result;
    }
}
