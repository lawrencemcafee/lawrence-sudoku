/*
 This file is part of Privacy Friendly Sudoku.
 Privacy Friendly Sudoku is licensed under the GNU General Public License, version 3 or later.
 */
package org.secuso.privacyfriendlysudoku.controller.training;

import java.util.Objects;

/** One value placement or candidate elimination expected by a training position. */
public final class TrainingTarget implements Comparable<TrainingTarget> {
    private final int row;
    private final int col;
    private final int value;

    public TrainingTarget(int row, int col, int value) {
        if(row < 0 || col < 0 || value <= 0) {
            throw new IllegalArgumentException("Training target coordinates and value are invalid.");
        }
        this.row = row;
        this.col = col;
        this.value = value;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getValue() { return value; }

    @Override
    public int compareTo(TrainingTarget other) {
        int coordinate = Integer.compare(row, other.row);
        if(coordinate != 0) return coordinate;
        coordinate = Integer.compare(col, other.col);
        return coordinate != 0 ? coordinate : Integer.compare(value, other.value);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof TrainingTarget)) return false;
        TrainingTarget target = (TrainingTarget) other;
        return row == target.row && col == target.col && value == target.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col, value);
    }
}
