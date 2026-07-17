/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import org.secuso.privacyfriendlysudoku.game.GameBoard;
import org.secuso.privacyfriendlysudoku.game.GameCell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds the next explainable Sudoku move using the techniques supported by the local hard
 * puzzle generator. The engine derives candidates from the board instead of trusting pencil
 * notes, so requesting a hint never requires a network connection or candidate preparation.
 */
public final class HumanHintEngine {

    private final GameBoard board;
    private final int[] solution;
    private final int size;
    private final int sectionHeight;
    private final int sectionWidth;
    private final int sectionsPerRow;
    private final int[][] values;
    private final boolean[][][] candidates;

    private HumanHintEngine(GameBoard board, int[] solution) {
        this.board = board;
        this.solution = solution;
        this.size = board.getSize();
        this.sectionHeight = board.getGameType().getSectionHeight();
        this.sectionWidth = board.getGameType().getSectionWidth();
        this.sectionsPerRow = size / sectionWidth;
        this.values = new int[size][size];

        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                values[row][col] = board.getCell(row, col).getValue();
            }
        }
        this.candidates = createCandidates();
    }

    /**
     * Return the next explained move, or {@code null} when the board is complete or has no
     * usable solution.
     */
    public static GameHint findHint(GameBoard board, int[] solution) {
        if(board == null || solution == null || solution.length != board.getSize() * board.getSize()) {
            return null;
        }
        for(int value : solution) {
            if(value <= 0 || value > board.getSize()) {
                return null;
            }
        }

        HumanHintEngine engine = new HumanHintEngine(board, solution);
        GameHint mistake = engine.findMistake();
        if(mistake != null) {
            return mistake;
        }
        return engine.findLogicalMove();
    }

    private GameHint findMistake() {
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                GameCell cell = board.getCell(row, col);
                int expected = solution[index(row, col)];
                if(!cell.isFixed() && cell.hasValue() && cell.getValue() != expected) {
                    int current = cell.getValue();
                    List<String> details = Arrays.asList(
                            coordinate(row, col) + " currently contains " + current + ".",
                            "Following the puzzle from its fixed clues leads to a contradiction with "
                                    + current + " in this cell.",
                            "Clear this entry, then ask again for the next logical move."
                    );
                    return new GameHint(
                            "Mistake Found",
                            "The " + current + " at " + coordinate(row, col)
                                    + " cannot be part of this puzzle's solution.",
                            details, row, col, current, GameHint.Action.CLEAR_VALUE);
                }
            }
        }
        return null;
    }

    private GameHint findLogicalMove() {
        if(isComplete()) {
            return null;
        }

        Placement placement = findPlacement();
        if(placement != null) {
            return placement.toHint();
        }

        List<ReasoningStep> reductions = new ArrayList<>();
        int maximumReductions = size * size * size;
        for(int i = 0; i < maximumReductions; i++) {
            ReasoningStep reduction = applyNextReduction();
            if(reduction == null) {
                break;
            }
            reductions.add(reduction);

            placement = findPlacement();
            if(placement != null) {
                return combine(reductions, placement);
            }
        }

        return createLookAheadHint();
    }

    private Placement findPlacement() {
        Placement placement = findLastDigit();
        if(placement != null) {
            return placement;
        }
        placement = findHiddenSingle();
        if(placement != null) {
            return placement;
        }
        return findNakedSingle();
    }

    private Placement findLastDigit() {
        for(Unit unit : allUnits()) {
            int emptyRow = -1;
            int emptyCol = -1;
            int emptyCount = 0;
            for(CellPosition position : unit.cells) {
                if(values[position.row][position.col] == 0) {
                    emptyCount++;
                    emptyRow = position.row;
                    emptyCol = position.col;
                }
            }
            if(emptyCount == 1) {
                int value = solution[index(emptyRow, emptyCol)];
                List<String> details = Arrays.asList(
                        "Every cell but one is filled in " + unit.label() + ".",
                        "The missing value is " + value + ".",
                        "Therefore " + coordinate(emptyRow, emptyCol) + " must be " + value + "."
                );
                return new Placement(
                        "Last Digit",
                        "Only one cell is empty in " + unit.label() + "; its missing value is "
                                + value + ".",
                        details, emptyRow, emptyCol, value);
            }
        }
        return null;
    }

    private Placement findHiddenSingle() {
        for(Unit unit : allUnits()) {
            for(int value = 1; value <= size; value++) {
                CellPosition onlyPosition = null;
                int count = 0;
                for(CellPosition position : unit.cells) {
                    if(candidates[position.row][position.col][value]) {
                        onlyPosition = position;
                        count++;
                    }
                }
                if(count == 1) {
                    List<String> details = Arrays.asList(
                            "Consider where " + value + " can go in " + unit.label() + ".",
                            "Every other empty cell in this " + unit.shortName()
                                    + " is ruled out by a matching value in its row, column, or block.",
                            "Only " + coordinate(onlyPosition.row, onlyPosition.col)
                                    + " remains, so place " + value + " there."
                    );
                    return new Placement(
                            "Hidden Single",
                            "Within " + unit.label() + ", only "
                                    + coordinate(onlyPosition.row, onlyPosition.col)
                                    + " can contain " + value + ".",
                            details, onlyPosition.row, onlyPosition.col, value);
                }
            }
        }
        return null;
    }

    private Placement findNakedSingle() {
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                if(values[row][col] != 0) {
                    continue;
                }
                List<Integer> possibleValues = candidateValues(row, col);
                if(possibleValues.size() == 1) {
                    int value = possibleValues.get(0);
                    List<String> details = Arrays.asList(
                            "Values already in row " + (row + 1) + " rule out "
                                    + joinedValues(valuesInRow(row)) + ".",
                            "Values in column " + (col + 1) + " and block "
                                    + (section(row, col) + 1) + " rule out the remaining alternatives.",
                            value + " is the only candidate left for " + coordinate(row, col) + "."
                    );
                    return new Placement(
                            "Naked Single",
                            "Only " + value + " can be placed at " + coordinate(row, col) + ".",
                            details, row, col, value);
                }
            }
        }
        return null;
    }

    private ReasoningStep applyNextReduction() {
        ReasoningStep step = applyNakedPair();
        if(step != null) {
            return step;
        }
        step = applyPointingCandidates();
        if(step != null) {
            return step;
        }
        step = applyClaimingCandidates();
        if(step != null) {
            return step;
        }
        return applyHiddenPair();
    }

    private ReasoningStep applyPointingCandidates() {
        for(Unit block : blockUnits()) {
            for(int value = 1; value <= size; value++) {
                List<CellPosition> positions = candidatePositions(block, value);
                if(positions.size() < 2) {
                    continue;
                }

                int row = positions.get(0).row;
                boolean sameRow = true;
                for(CellPosition position : positions) {
                    sameRow &= position.row == row;
                }
                if(sameRow) {
                    int removed = removeFromRowOutsideBlock(row, block.index, value);
                    if(removed > 0) {
                        return new ReasoningStep(
                                "Pointing Candidates",
                                "Every possible position for " + value + " in " + block.label()
                                        + " lies in row " + (row + 1) + ". Remove " + value
                                        + " from the other cells in that row (" + removed
                                        + (removed == 1 ? " candidate)." : " candidates)."));
                    }
                }

                int col = positions.get(0).col;
                boolean sameCol = true;
                for(CellPosition position : positions) {
                    sameCol &= position.col == col;
                }
                if(sameCol) {
                    int removed = removeFromColumnOutsideBlock(col, block.index, value);
                    if(removed > 0) {
                        return new ReasoningStep(
                                "Pointing Candidates",
                                "Every possible position for " + value + " in " + block.label()
                                        + " lies in column " + (col + 1) + ". Remove " + value
                                        + " from the other cells in that column (" + removed
                                        + (removed == 1 ? " candidate)." : " candidates)."));
                    }
                }
            }
        }
        return null;
    }

    private ReasoningStep applyClaimingCandidates() {
        for(Unit row : rowUnits()) {
            ReasoningStep step = claimFromLine(row);
            if(step != null) {
                return step;
            }
        }
        for(Unit column : columnUnits()) {
            ReasoningStep step = claimFromLine(column);
            if(step != null) {
                return step;
            }
        }
        return null;
    }

    private ReasoningStep claimFromLine(Unit line) {
        for(int value = 1; value <= size; value++) {
            List<CellPosition> positions = candidatePositions(line, value);
            if(positions.size() < 2) {
                continue;
            }
            int block = section(positions.get(0).row, positions.get(0).col);
            boolean sameBlock = true;
            for(CellPosition position : positions) {
                sameBlock &= section(position.row, position.col) == block;
            }
            if(!sameBlock) {
                continue;
            }

            int removed = removeFromBlockOutsideLine(block, line, value);
            if(removed > 0) {
                return new ReasoningStep(
                        "Claiming Candidates",
                        "Every possible position for " + value + " in " + line.label()
                                + " lies in block " + (block + 1) + ". Remove " + value
                                + " from the other cells in that block (" + removed
                                + (removed == 1 ? " candidate)." : " candidates)."));
            }
        }
        return null;
    }

    private ReasoningStep applyNakedPair() {
        for(Unit unit : allUnits()) {
            List<CellPosition> pairCells = new ArrayList<>();
            for(CellPosition position : unit.cells) {
                if(candidateValues(position.row, position.col).size() == 2) {
                    pairCells.add(position);
                }
            }
            for(int first = 0; first < pairCells.size(); first++) {
                CellPosition firstCell = pairCells.get(first);
                List<Integer> pair = candidateValues(firstCell.row, firstCell.col);
                for(int second = first + 1; second < pairCells.size(); second++) {
                    CellPosition secondCell = pairCells.get(second);
                    if(!pair.equals(candidateValues(secondCell.row, secondCell.col))) {
                        continue;
                    }
                    int removed = 0;
                    for(CellPosition position : unit.cells) {
                        if(position.equals(firstCell) || position.equals(secondCell)) {
                            continue;
                        }
                        for(int value : pair) {
                            if(candidates[position.row][position.col][value]) {
                                candidates[position.row][position.col][value] = false;
                                removed++;
                            }
                        }
                    }
                    if(removed > 0) {
                        return new ReasoningStep(
                                "Naked Pair",
                                coordinate(firstCell.row, firstCell.col) + " and "
                                        + coordinate(secondCell.row, secondCell.col) + " contain only "
                                        + joinedValues(pair) + " in " + unit.label()
                                        + ". Those values can be removed from the other cells (" + removed
                                        + (removed == 1 ? " candidate)." : " candidates)."));
                    }
                }
            }
        }
        return null;
    }

    private ReasoningStep applyHiddenPair() {
        for(Unit unit : allUnits()) {
            for(int firstValue = 1; firstValue <= size; firstValue++) {
                List<CellPosition> firstPositions = candidatePositions(unit, firstValue);
                if(firstPositions.size() != 2) {
                    continue;
                }
                for(int secondValue = firstValue + 1; secondValue <= size; secondValue++) {
                    List<CellPosition> secondPositions = candidatePositions(unit, secondValue);
                    if(!firstPositions.equals(secondPositions)) {
                        continue;
                    }
                    int removed = 0;
                    for(CellPosition position : firstPositions) {
                        for(int value = 1; value <= size; value++) {
                            if(value != firstValue && value != secondValue
                                    && candidates[position.row][position.col][value]) {
                                candidates[position.row][position.col][value] = false;
                                removed++;
                            }
                        }
                    }
                    if(removed > 0) {
                        return new ReasoningStep(
                                "Hidden Pair",
                                "In " + unit.label() + ", " + firstValue + " and " + secondValue
                                        + " can appear only at "
                                        + coordinate(firstPositions.get(0).row, firstPositions.get(0).col)
                                        + " and "
                                        + coordinate(firstPositions.get(1).row, firstPositions.get(1).col)
                                        + ". Remove their other candidates (" + removed
                                        + (removed == 1 ? " candidate)." : " candidates)."));
                    }
                }
            }
        }
        return null;
    }

    private GameHint combine(List<ReasoningStep> reductions, Placement placement) {
        List<String> details = new ArrayList<>();
        for(ReasoningStep step : reductions) {
            details.add(step.explanation);
        }
        details.addAll(placement.details);

        ReasoningStep first = reductions.get(0);
        String summary = first.explanation + " This reveals that "
                + coordinate(placement.row, placement.col) + " must be " + placement.value + ".";
        return new GameHint(first.title, summary, details, placement.row, placement.col,
                placement.value, GameHint.Action.PLACE_VALUE);
    }

    private GameHint createLookAheadHint() {
        CellPosition best = null;
        List<Integer> bestCandidates = null;
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                if(values[row][col] != 0) {
                    continue;
                }
                List<Integer> possible = validCandidatesFromValues(row, col);
                if(!possible.isEmpty()
                        && (bestCandidates == null || possible.size() < bestCandidates.size())) {
                    best = new CellPosition(row, col);
                    bestCandidates = possible;
                }
            }
        }
        if(best == null) {
            return null;
        }

        int value = solution[index(best.row, best.col)];
        List<String> details = Arrays.asList(
                coordinate(best.row, best.col) + " starts with candidates "
                        + joinedValues(bestCandidates) + ".",
                "Testing the alternatives against the remaining puzzle causes every value except "
                        + value + " to reach a contradiction.",
                "Keep " + value + " at " + coordinate(best.row, best.col) + "."
        );
        return new GameHint(
                "Trial and Elimination",
                "At " + coordinate(best.row, best.col) + ", only " + value
                        + " leads to a valid completion.",
                details, best.row, best.col, value, GameHint.Action.PLACE_VALUE);
    }

    private boolean[][][] createCandidates() {
        boolean[][][] result = new boolean[size][size][size + 1];
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                if(values[row][col] != 0) {
                    continue;
                }
                for(int value = 1; value <= size; value++) {
                    result[row][col][value] = isValidFromValues(row, col, value);
                }
            }
        }
        return result;
    }

    private boolean isValidFromValues(int row, int col, int value) {
        for(int i = 0; i < size; i++) {
            if(values[row][i] == value || values[i][col] == value) {
                return false;
            }
        }
        int startRow = (row / sectionHeight) * sectionHeight;
        int startCol = (col / sectionWidth) * sectionWidth;
        for(int rowOffset = 0; rowOffset < sectionHeight; rowOffset++) {
            for(int colOffset = 0; colOffset < sectionWidth; colOffset++) {
                if(values[startRow + rowOffset][startCol + colOffset] == value) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<Integer> validCandidatesFromValues(int row, int col) {
        List<Integer> result = new ArrayList<>();
        for(int value = 1; value <= size; value++) {
            if(isValidFromValues(row, col, value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<Integer> candidateValues(int row, int col) {
        List<Integer> result = new ArrayList<>();
        for(int value = 1; value <= size; value++) {
            if(candidates[row][col][value]) {
                result.add(value);
            }
        }
        return result;
    }

    private List<CellPosition> candidatePositions(Unit unit, int value) {
        List<CellPosition> result = new ArrayList<>();
        for(CellPosition position : unit.cells) {
            if(candidates[position.row][position.col][value]) {
                result.add(position);
            }
        }
        return result;
    }

    private int removeFromRowOutsideBlock(int row, int block, int value) {
        int removed = 0;
        for(int col = 0; col < size; col++) {
            if(section(row, col) != block && candidates[row][col][value]) {
                candidates[row][col][value] = false;
                removed++;
            }
        }
        return removed;
    }

    private int removeFromColumnOutsideBlock(int col, int block, int value) {
        int removed = 0;
        for(int row = 0; row < size; row++) {
            if(section(row, col) != block && candidates[row][col][value]) {
                candidates[row][col][value] = false;
                removed++;
            }
        }
        return removed;
    }

    private int removeFromBlockOutsideLine(int block, Unit line, int value) {
        int removed = 0;
        for(CellPosition position : blockUnit(block).cells) {
            boolean outsideLine = line.type == UnitType.ROW
                    ? position.row != line.index : position.col != line.index;
            if(outsideLine && candidates[position.row][position.col][value]) {
                candidates[position.row][position.col][value] = false;
                removed++;
            }
        }
        return removed;
    }

    private boolean isComplete() {
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                if(values[row][col] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<Integer> valuesInRow(int row) {
        List<Integer> result = new ArrayList<>();
        for(int value : values[row]) {
            if(value != 0 && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<Unit> allUnits() {
        List<Unit> result = new ArrayList<>();
        result.addAll(blockUnits());
        result.addAll(rowUnits());
        result.addAll(columnUnits());
        return result;
    }

    private List<Unit> blockUnits() {
        List<Unit> result = new ArrayList<>();
        for(int block = 0; block < size; block++) {
            result.add(blockUnit(block));
        }
        return result;
    }

    private Unit blockUnit(int block) {
        int startRow = (block / sectionsPerRow) * sectionHeight;
        int startCol = (block % sectionsPerRow) * sectionWidth;
        List<CellPosition> cells = new ArrayList<>();
        for(int rowOffset = 0; rowOffset < sectionHeight; rowOffset++) {
            for(int colOffset = 0; colOffset < sectionWidth; colOffset++) {
                cells.add(new CellPosition(startRow + rowOffset, startCol + colOffset));
            }
        }
        return new Unit(UnitType.BLOCK, block, cells);
    }

    private List<Unit> rowUnits() {
        List<Unit> result = new ArrayList<>();
        for(int row = 0; row < size; row++) {
            List<CellPosition> cells = new ArrayList<>();
            for(int col = 0; col < size; col++) {
                cells.add(new CellPosition(row, col));
            }
            result.add(new Unit(UnitType.ROW, row, cells));
        }
        return result;
    }

    private List<Unit> columnUnits() {
        List<Unit> result = new ArrayList<>();
        for(int col = 0; col < size; col++) {
            List<CellPosition> cells = new ArrayList<>();
            for(int row = 0; row < size; row++) {
                cells.add(new CellPosition(row, col));
            }
            result.add(new Unit(UnitType.COLUMN, col, cells));
        }
        return result;
    }

    private int section(int row, int col) {
        return (row / sectionHeight) * sectionsPerRow + col / sectionWidth;
    }

    private int index(int row, int col) {
        return row * size + col;
    }

    private static String coordinate(int row, int col) {
        return "row " + (row + 1) + ", column " + (col + 1);
    }

    private static String joinedValues(List<Integer> values) {
        if(values == null || values.isEmpty()) {
            return "none";
        }
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < values.size(); i++) {
            if(i > 0) {
                result.append(i == values.size() - 1 ? " and " : ", ");
            }
            result.append(values.get(i));
        }
        return result.toString();
    }

    private enum UnitType {
        ROW,
        COLUMN,
        BLOCK
    }

    private static final class CellPosition {
        private final int row;
        private final int col;

        private CellPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof CellPosition)) {
                return false;
            }
            CellPosition position = (CellPosition) other;
            return row == position.row && col == position.col;
        }

        @Override
        public int hashCode() {
            return 31 * row + col;
        }
    }

    private static final class Unit {
        private final UnitType type;
        private final int index;
        private final List<CellPosition> cells;

        private Unit(UnitType type, int index, List<CellPosition> cells) {
            this.type = type;
            this.index = index;
            this.cells = cells;
        }

        private String label() {
            return shortName() + " " + (index + 1);
        }

        private String shortName() {
            switch(type) {
                case ROW:
                    return "row";
                case COLUMN:
                    return "column";
                case BLOCK:
                default:
                    return "block";
            }
        }
    }

    private static final class ReasoningStep {
        private final String title;
        private final String explanation;

        private ReasoningStep(String title, String explanation) {
            this.title = title;
            this.explanation = explanation;
        }
    }

    private static final class Placement {
        private final String title;
        private final String summary;
        private final List<String> details;
        private final int row;
        private final int col;
        private final int value;

        private Placement(String title, String summary, List<String> details,
                          int row, int col, int value) {
            this.title = title;
            this.summary = summary;
            this.details = details;
            this.row = row;
            this.col = col;
            this.value = value;
        }

        private GameHint toHint() {
            return new GameHint(title, summary, details, row, col, value,
                    GameHint.Action.PLACE_VALUE);
        }
    }
}
