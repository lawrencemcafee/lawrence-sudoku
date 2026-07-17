/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.
 */
package org.secuso.privacyfriendlysudoku.controller.hints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Size-independent rows, columns, blocks, and peer relationships for a Sudoku board. */
public final class BoardTopology {

    public static final class Cell {
        private final int row;
        private final int col;
        private final int index;

        private Cell(int row, int col, int size) {
            this.row = row;
            this.col = col;
            this.index = row * size + col;
        }

        public int getRow() { return row; }
        public int getCol() { return col; }
        public int getIndex() { return index; }

        @Override
        public boolean equals(Object other) {
            return other instanceof Cell && ((Cell) other).index == index;
        }

        @Override
        public int hashCode() { return index; }
    }

    public static final class Unit {
        private final GameHint.UnitType type;
        private final int index;
        private final List<Cell> cells;

        private Unit(GameHint.UnitType type, int index, List<Cell> cells) {
            this.type = type;
            this.index = index;
            this.cells = Collections.unmodifiableList(cells);
        }

        public GameHint.UnitType getType() { return type; }
        public int getIndex() { return index; }
        public List<Cell> getCells() { return cells; }

        public String label() {
            return type.name().toLowerCase(Locale.ROOT) + " " + (index + 1);
        }
    }

    private final int size;
    private final int blockHeight;
    private final int blockWidth;
    private final Cell[] cells;
    private final List<Unit> units;
    private final List<Cell>[] peers;
    private final List<Unit>[] cellUnits;

    @SuppressWarnings("unchecked")
    public BoardTopology(int size, int blockHeight, int blockWidth) {
        if(size <= 0 || blockHeight <= 0 || blockWidth <= 0
                || blockHeight * blockWidth != size) {
            throw new IllegalArgumentException("Block dimensions must multiply to the board size.");
        }
        this.size = size;
        this.blockHeight = blockHeight;
        this.blockWidth = blockWidth;
        this.cells = new Cell[size * size];
        this.units = new ArrayList<>(size * 3);
        this.peers = new List[size * size];
        this.cellUnits = new List[size * size];

        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                cells[index(row, col)] = new Cell(row, col, size);
                cellUnits[index(row, col)] = new ArrayList<>(3);
            }
        }
        buildUnits();
        buildPeers();
    }

    private void buildUnits() {
        for(int row = 0; row < size; row++) {
            List<Cell> members = new ArrayList<>(size);
            for(int col = 0; col < size; col++) members.add(cell(row, col));
            addUnit(new Unit(GameHint.UnitType.ROW, row, members));
        }
        for(int col = 0; col < size; col++) {
            List<Cell> members = new ArrayList<>(size);
            for(int row = 0; row < size; row++) members.add(cell(row, col));
            addUnit(new Unit(GameHint.UnitType.COLUMN, col, members));
        }
        int blocksPerRow = size / blockWidth;
        for(int block = 0; block < size; block++) {
            int startRow = (block / blocksPerRow) * blockHeight;
            int startCol = (block % blocksPerRow) * blockWidth;
            List<Cell> members = new ArrayList<>(size);
            for(int row = startRow; row < startRow + blockHeight; row++) {
                for(int col = startCol; col < startCol + blockWidth; col++) {
                    members.add(cell(row, col));
                }
            }
            addUnit(new Unit(GameHint.UnitType.BLOCK, block, members));
        }
    }

    private void addUnit(Unit unit) {
        units.add(unit);
        for(Cell cell : unit.cells) cellUnits[cell.index].add(unit);
    }

    private void buildPeers() {
        for(Cell cell : cells) {
            Set<Cell> related = new LinkedHashSet<>();
            for(Unit unit : cellUnits[cell.index]) related.addAll(unit.cells);
            related.remove(cell);
            peers[cell.index] = Collections.unmodifiableList(new ArrayList<>(related));
            cellUnits[cell.index] = Collections.unmodifiableList(cellUnits[cell.index]);
        }
    }

    public int getSize() { return size; }
    public int getBlockHeight() { return blockHeight; }
    public int getBlockWidth() { return blockWidth; }
    public int index(int row, int col) { return row * size + col; }
    public Cell cell(int row, int col) { return cells[index(row, col)]; }
    public Cell cell(int index) { return cells[index]; }
    public List<Unit> getUnits() { return Collections.unmodifiableList(units); }
    public List<Cell> peers(Cell cell) { return peers[cell.index]; }
    public List<Unit> units(Cell cell) { return cellUnits[cell.index]; }

    public Unit row(int row) { return units.get(row); }
    public Unit column(int col) { return units.get(size + col); }
    public Unit block(int block) { return units.get(size * 2 + block); }

    public int blockIndex(int row, int col) {
        return (row / blockHeight) * (size / blockWidth) + col / blockWidth;
    }

    public boolean sees(Cell first, Cell second) {
        return first.index != second.index
                && (first.row == second.row || first.col == second.col
                || blockIndex(first.row, first.col) == blockIndex(second.row, second.col));
    }
}
