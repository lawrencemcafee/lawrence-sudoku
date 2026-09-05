/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stable duplicate key across digit relabeling and the eight square-grid symmetries. */
public final class TrainingCanonicalizer {
    private TrainingCanonicalizer() {}

    public static String fingerprint(TrainingPosition position) {
        String best = null;
        for(int symmetry = 0; symmetry < 8; symmetry++) {
            String candidate = fingerprint(position, symmetry);
            if(best == null || candidate.compareTo(best) < 0) best = candidate;
        }
        return best;
    }

    private static String fingerprint(TrainingPosition position, int symmetry) {
        int[] sourceValues = position.getValues();
        int[] sourceMasks = position.getCandidateMasks();
        int[] sourceSolution = position.getSolution();
        int[] values = new int[TrainingPosition.CELL_COUNT];
        int[] masks = new int[TrainingPosition.CELL_COUNT];
        int[] solution = new int[TrainingPosition.CELL_COUNT];

        for(int row = 0; row < TrainingPosition.SIZE; row++) {
            for(int col = 0; col < TrainingPosition.SIZE; col++) {
                int oldIndex = row * TrainingPosition.SIZE + col;
                int newRow = mapRow(symmetry, row, col);
                int newCol = mapCol(symmetry, row, col);
                int newIndex = newRow * TrainingPosition.SIZE + newCol;
                values[newIndex] = sourceValues[oldIndex];
                masks[newIndex] = sourceMasks[oldIndex];
                solution[newIndex] = sourceSolution[oldIndex];
            }
        }

        int[] digits = new int[TrainingPosition.SIZE + 1];
        int nextDigit = 1;
        for(int value : solution) {
            if(value > 0 && digits[value] == 0) digits[value] = nextDigit++;
        }

        StringBuilder result = new StringBuilder(TrainingPosition.CELL_COUNT * 3 + 32);
        for(int value : solution) result.append((char) digits[value]);
        for(int value : values) result.append((char) (value == 0 ? 0 : digits[value]));
        for(int mask : masks) result.append((char) remapMask(mask, digits));
        result.append((char) position.getAction().ordinal());
        result.append((char) remapMask(position.getFocusedValueMask(), digits));

        List<Integer> targets = new ArrayList<>();
        for(TrainingTarget target : position.getTargets()) {
            int row = mapRow(symmetry, target.getRow(), target.getCol());
            int col = mapCol(symmetry, target.getRow(), target.getCol());
            targets.add(row * 100 + col * 10 + digits[target.getValue()]);
        }
        Collections.sort(targets);
        for(int target : targets) result.append((char) target);
        return result.toString();
    }

    private static int remapMask(int mask, int[] digits) {
        int result = 0;
        for(int value = 1; value <= TrainingPosition.SIZE; value++) {
            if((mask & (1 << (value - 1))) != 0) {
                result |= 1 << (digits[value] - 1);
            }
        }
        return result;
    }

    private static int mapRow(int symmetry, int row, int col) {
        switch(symmetry) {
            case 1: return col;
            case 2: return 8 - row;
            case 3: return 8 - col;
            case 4: return row;
            case 5: return 8 - col;
            case 6: return 8 - row;
            case 7: return col;
            default: return row;
        }
    }

    private static int mapCol(int symmetry, int row, int col) {
        switch(symmetry) {
            case 1: return 8 - row;
            case 2: return 8 - col;
            case 3: return row;
            case 4: return 8 - col;
            case 5: return 8 - row;
            case 6: return col;
            case 7: return row;
            default: return col;
        }
    }
}
